// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.services;

import com.intellij.openapi.application.ModalityState;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.CredentialsPrompt;

/**
 * Credentials prompt implementation for the IntelliJ plugin.
 * Shows a simple user name / password dialog parented to the currently active window.
 */
public class CredentialsPromptImpl implements CredentialsPrompt {
    private String userName;
    private String password;
    private boolean promptSuccess;

    @Override
    public boolean prompt(final String serverUrl, final String defaultUserName) {
        promptSuccess = false;
        // prompt() is called from a background authentication thread. Use ModalityState.any() so the
        // dialog shows immediately even when a modal dialog (e.g. Settings) is already open, instead of
        // being deferred by the default NON_MODAL state until that dialog closes.
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                promptSuccess = promptInternal(serverUrl, defaultUserName);
            }
        }, true, ModalityState.any());

        return promptSuccess;
    }

    private boolean promptInternal(final String serverUrl, final String defaultUserName) {
        final TfsCredentialsDialog authDialog = new TfsCredentialsDialog(
                TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_PROMPT_CREDENTIALS_MESSAGE, serverUrl),
                defaultUserName);

        if (authDialog.showAndGet()) {
            userName = authDialog.getUsername();
            password = authDialog.getPassword();
            return true;
        }

        return false;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String validateCredentials(final String serverUrl, final AuthenticationInfo authenticationInfo) {
        final ServerContext context =
                new ServerContextBuilder().type(ServerContext.Type.TFS)
                        .uri(serverUrl).authentication(authenticationInfo).build();
        ServerContextManager.getInstance().validateServerConnection(context);

        // validation succeeded, return the authenticated url that worked
        return serverUrl;
    }
}
