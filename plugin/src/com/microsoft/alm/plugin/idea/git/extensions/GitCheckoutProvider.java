// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.checkout.CheckoutCloneComponent;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.git.ui.checkout.GitCheckoutModel;
import org.jetbrains.annotations.NotNull;

public class GitCheckoutProvider implements CheckoutProvider {

    @Override
    public String getVcsName() {
        return TfPluginBundle.message(TfPluginBundle.KEY_TF_GIT);
    }

    @NotNull
    @Override
    public VcsCloneComponent buildVcsCloneComponent(
            @NotNull final Project project,
            @NotNull final ModalityState modalityState,
            @NotNull final VcsCloneDialogComponentStateListener dialogStateListener) {
        return new CheckoutCloneComponent(project, new GitCheckoutModel(), IdeaHelper::isGitExeConfigured);
    }
}
