// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoft.alm.plugin.idea.tfvc.ui.checkout;

import com.intellij.dvcs.DvcsUtil;import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.CreateWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.DeleteWorkspaceCommand;
import com.microsoft.alm.plugin.external.commands.UpdateWorkspaceMappingCommand;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceAlreadyExistsException;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.CommandUtils;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VcsSpecificCheckoutModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.ui.settings.EULADialog;
import com.microsoft.alm.plugin.idea.tfvc.ui.workspace.WorkspaceController;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TfvcCheckoutModel implements VcsSpecificCheckoutModel {
    private static final Logger logger = LoggerFactory.getLogger(TfvcCheckoutModel.class);


    @Override
    public void doCheckout(
            Project project,
            CheckoutProvider.Listener listener,
            ServerContext context,
            VirtualFile destinationParent,
            String directoryName,
            String parentDirectory,
            boolean isAdvancedChecked,
            boolean isTfvcServerCheckout) {
        final String workspaceName = directoryName;
        final String teamProjectName = getRepositoryName(context);
        final String localPath = Path.combine(parentDirectory, directoryName);
        Workspace.Location workspaceKind = isTfvcServerCheckout ? Workspace.Location.SERVER : Workspace.Location.LOCAL;
        final AtomicBoolean checkoutResult = new AtomicBoolean();
        (new Task.Backgroundable(project,
                TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_CREATING_WORKSPACE),
                true) {
            public void run(@NotNull final ProgressIndicator indicator) {
                IdeaHelper.setProgress(indicator, 0.10, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_PROGRESS_CREATING));

                // Create the workspace with default values
                final CreateWorkspaceCommand command = new CreateWorkspaceCommand(
                        context,
                        workspaceName,
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_WORKSPACE_COMMENT),
                        null,
                        null,
                        workspaceKind);
                try {
                    EULADialog.executeWithGuard(project, command::runSynchronously);
                } catch (final WorkspaceAlreadyExistsException e) {
                    logger.warn("Error creating workspace: " + LocalizationServiceImpl.getInstance().getExceptionMessage(e));
                    // A workspace with this name is already registered on the server (its name is derived from the
                    // folder name). Deleting the local folder does not remove it. Offer to delete it on the server and
                    // retry; if the user declines, abort without touching the server workspace.
                    if (!confirmFixExistingWorkspace(project, workspaceName)) {
                        return;
                    }
                    try {
                        logger.info("Deleting existing workspace '" + workspaceName + "' at the user's request and retrying");
                        new DeleteWorkspaceCommand(context, workspaceName).runSynchronously();
                        EULADialog.executeWithGuard(project, command::runSynchronously);
                    } catch (final RuntimeException retryError) {
                        logger.warn("Error recreating workspace after removing the existing one", retryError);
                        showCheckoutError(project, LocalizationServiceImpl.getInstance().getExceptionMessage(retryError));
                        return;
                    }
                    // Recreation succeeded; fall through and continue the checkout as usual.
                } catch (final RuntimeException e) {
                    // Any other failure while creating the workspace (unrecognized tf error, auth, etc.). Surface it
                    // instead of letting the background task swallow it silently, which would just make the progress
                    // bar vanish with no feedback.
                    logger.warn("Error creating workspace", e);
                    showCheckoutError(project, LocalizationServiceImpl.getInstance().getExceptionMessage(e));
                    return;
                }

                IdeaHelper.setProgress(indicator, 0.20, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_PROGRESS_ADD_ROOT));

                // Map the project root to the local folder
                final String serverPath = VcsHelper.TFVC_ROOT + teamProjectName;
                try {
                    final UpdateWorkspaceMappingCommand mappingCommand = new UpdateWorkspaceMappingCommand(context, workspaceName,
                            new Workspace.Mapping(serverPath, localPath, false), false);
                    mappingCommand.runSynchronously();
                } catch (final RuntimeException e) {
                    logger.warn("Error while mapping workspace during creation", e);
                    showCheckoutError(project, LocalizationServiceImpl.getInstance().getExceptionMessage(e));

                    // mapping failed so delete the workspace to clean up and return
                    final DeleteWorkspaceCommand deleteWorkspaceCommand = new DeleteWorkspaceCommand(context, workspaceName);
                    deleteWorkspaceCommand.runSynchronously();
                    return;
                }

                IdeaHelper.setProgress(indicator, 0.30, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_PROGRESS_CREATE_FOLDER));

                // Ensure that the local folder exists
                final File file = new File(localPath);
                if (!file.mkdirs()) {
                    //TODO should we throw here?
                }

                // if advanced is set, then sync just some of the files (those that we need for IntelliJ)
                // Otherwise, sync all the files for the team project
                if (!isAdvancedChecked) {
                    IdeaHelper.setProgress(indicator, 0.50, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_PROGRESS_SYNC));
                    // Sync all files recursively
                    CommandUtils.syncWorkspace(context, localPath);
                }

                IdeaHelper.setProgress(indicator, 1.00, "", true);

                // No exception means that it was successful
                checkoutResult.set(true);
            }

            public void onSuccess() {
                if (checkoutResult.get()) {
                    // Check the isAdvanced flag
                    if (isAdvancedChecked) {
                        // The user wants to edit the workspace before syncing...
                        final RepositoryContext repositoryContext = RepositoryContext.createTfvcContext(localPath, workspaceName, teamProjectName, context.getServerUri());
                        final WorkspaceController controller = new WorkspaceController(project, repositoryContext, context, workspaceName);
                        if (controller.showModalDialog(false)) {
                            // Save and Sync the workspace (this will be backgrounded)
                            controller.saveWorkspace(localPath, true, new Runnable() {
                                @Override
                                public void run() {
                                    // Files are all synchronized, so trigger the VCS update
                                    UpdateVersionControlSystem(project, parentDirectory, directoryName, destinationParent, listener);
                                }
                            });
                        } else {
                            // The user cancelled the edit dialog. The workspace was already created on the server
                            // above, so remove it; otherwise it would linger and block a later checkout into the
                            // same folder name. The local folder is left untouched.
                            deleteWorkspaceInBackground(project, context, workspaceName);
                        }
                    } else {
                        // We don't have to wait for the workspace to be updated, so just trigger the VCS update
                        UpdateVersionControlSystem(project, parentDirectory, directoryName, destinationParent, listener);
                    }
                }
            }
        }).queue();
    }

    /**
     * Deletes a just-created workspace from the server on a background thread. Used to clean up when the user
     * cancels the "edit workspace before getting sources" dialog. Deletion is best-effort: {@link DeleteWorkspaceCommand}
     * already ignores a missing workspace, and any other failure is only logged, since the user can still remove the
     * workspace manually through Manage Workspaces.
     */
    private void deleteWorkspaceInBackground(final Project project, final ServerContext context, final String workspaceName) {
        (new Task.Backgroundable(project,
                TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_DELETING_WORKSPACE),
                false) {
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    final DeleteWorkspaceCommand deleteWorkspaceCommand = new DeleteWorkspaceCommand(context, workspaceName);
                    deleteWorkspaceCommand.runSynchronously();
                } catch (final RuntimeException e) {
                    logger.warn("Failed to delete workspace '" + workspaceName + "' after the edit dialog was cancelled", e);
                }
            }
        }).queue();
    }

    /**
     * Shows the "workspace already exists" dialog with an OK button (dismiss) and a "Fix Workspace on Server"
     * button, and returns {@code true} only if the user chose to remove the existing workspace and retry.
     * <p>
     * OK is the default button because deleting the workspace is destructive (it discards any pending changes that
     * workspace still holds on the server). Shown with {@code wait=true} (invokeAndWait) so the modal dialog is
     * actually displayed; {@code any()} modality is deliberate so it can never deadlock against a modal that is
     * still closing.
     */
    private boolean confirmFixExistingWorkspace(final Project project, final String workspaceName) {
        final AtomicInteger choice = new AtomicInteger(-1);
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                choice.set(Messages.showDialog(
                        project,
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_WORKSPACE_EXISTS, workspaceName),
                        TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_FAILED_TITLE),
                        new String[]{
                                Messages.getOkButton(),
                                TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_WORKSPACE_EXISTS_FIX)},
                        0,
                        Messages.getWarningIcon()));
            }
        }, true);
        // index 1 is the "Fix Workspace on Server" button; OK (0) or a closed dialog (-1) means "do nothing".
        return choice.get() == 1;
    }

    /**
     * Shows a modal checkout-failure dialog. Uses {@code wait=true} (invokeAndWait) so the dialog is actually
     * displayed instead of being created outside any modal context and disposed immediately.
     */
    private void showCheckoutError(final Project project, final String message) {
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Messages.showErrorDialog(project, message, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_TFVC_FAILED_TITLE));
            }
        }, true);
    }

    private void UpdateVersionControlSystem(final Project project, String parentDirectory, String directoryName, final VirtualFile destinationParent, CheckoutProvider.Listener listener) {
        // Add our new directory to IntelliJ's project
        DvcsUtil.addMappingIfSubRoot(project, FileUtil.join(new String[]{parentDirectory, directoryName}), TFSVcs.TFVC_NAME);

        // Check the folder for any dirty files
        destinationParent.refresh(true, true, new Runnable() {
            public void run() {
                if (project.isOpen() && !project.isDisposed() && !project.isDefault()) {
                    VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
                    mgr.fileDirty(destinationParent);
                }

            }
        });

        // Trigger our listener events
        listener.directoryCheckedOut(new File(parentDirectory, directoryName), TFSVcs.getKey());
        listener.checkoutCompleted();
    }

    @Override
    public String getButtonText() {
        return TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CREATE_WORKSPACE_BUTTON);
    }

    @Override
    public String getRepositoryName(final ServerContext context) {
        return (context != null && context.getTeamProjectReference() != null)
                ? context.getTeamProjectReference().getName() : StringUtils.EMPTY;
    }

    @Override
    public RepositoryContext.Type getRepositoryType() {
        return RepositoryContext.Type.TFVC;
    }
}
