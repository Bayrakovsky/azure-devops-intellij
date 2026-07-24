// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * A simple user name / password prompt. Replaces the platform's {@code com.intellij.vcsUtil.AuthDialog},
 * which is scheduled for removal.
 */
public class TfsCredentialsDialog extends DialogWrapper {
    private final String message;
    private final String defaultUserName;
    private final JBTextField userNameField = new JBTextField();
    private final JBPasswordField passwordField = new JBPasswordField();

    public TfsCredentialsDialog(
            final String title,
            final String message,
            final String defaultUserName) {
        // Parent to the currently active window (e.g. the Settings dialog) so the prompt stacks on
        // top of it and is modal to it, instead of being tied to a project frame.
        super(false);
        this.message = message;
        this.defaultUserName = defaultUserName;
        userNameField.setText(defaultUserName);
        setTitle(title);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel(message))
                .addLabeledComponent(
                        TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_USERNAME), userNameField)
                .addLabeledComponent(
                        TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_PASSWORD), passwordField)
                .getPanel();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return StringUtils.isNotEmpty(defaultUserName) ? passwordField : userNameField;
    }

    public String getUsername() {
        return userNameField.getText();
    }

    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }
}
