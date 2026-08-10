// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.checkout;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A "Get from Version Control" dialog component that opens the plugin's modal checkout dialog when the primary
 * dialog button is pressed. This replaces the platform's deprecated {@code VcsCloneComponentStub} +
 * {@code CheckoutProvider#doCheckout} pair while keeping the user experience unchanged.
 */
public class CheckoutCloneComponent implements VcsCloneComponent {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutCloneComponent.class);

    private final Project project;
    private final VcsSpecificCheckoutModel specificCheckoutModel;
    private final Predicate<Project> environmentCheck;

    /**
     * @param project               current (or default) project the clone dialog was opened for.
     * @param specificCheckoutModel VCS-specific (Git or TFVC) checkout logic.
     * @param environmentCheck      precondition verified before the checkout dialog is opened (e.g. that the git or
     *                              tf executable is configured); when it fails, it is responsible for notifying the
     *                              user, and the dialog is not shown.
     */
    public CheckoutCloneComponent(
            @NotNull final Project project,
            @NotNull final VcsSpecificCheckoutModel specificCheckoutModel,
            @NotNull final Predicate<Project> environmentCheck) {
        this.project = project;
        this.specificCheckoutModel = specificCheckoutModel;
        this.environmentCheck = environmentCheck;
    }

    @NotNull
    @Override
    public JComponent getView() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.emptyLeft(UIUtil.PANEL_REGULAR_INSETS.left));
        panel.add(new JLabel(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_CLONE_HINT, getOkButtonText())),
                BorderLayout.NORTH);
        return panel;
    }

    @Override
    public void doClone(@NotNull final CheckoutProvider.Listener listener) {
        FileDocumentManager.getInstance().saveAllDocuments();

        if (!environmentCheck.test(project)) {
            return;
        }

        try {
            final CheckoutController controller = new CheckoutController(project, listener, specificCheckoutModel);
            controller.showModalDialog();
        } catch (Throwable t) {
            logger.warn("doClone failed unexpectedly", t);
            VcsNotifier.getInstance(project).notifyError(null,
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                    TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, t.getMessage()));
        }
    }

    @Override
    public boolean isOkEnabled() {
        return true;
    }

    @NotNull
    @Override
    public List<ValidationInfo> doValidateAll() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public void dispose() {
    }
}
