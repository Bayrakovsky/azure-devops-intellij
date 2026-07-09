// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.ServiceManager;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TeamServicesSecrets {
    private static final Logger logger = LoggerFactory.getLogger(TeamServicesSecrets.class);

    public static TeamServicesSecrets getInstance() {
        return ServiceManager.getService(TeamServicesSecrets.class);
    }

    public static void forget(final String key) {
        forgetPassword(key);
    }

    public AuthenticationInfo load(final String key) throws IOException {
        final String authInfoSerialized = readPassword(key);

        AuthenticationInfo info = null;
        if (StringUtils.isNotEmpty(authInfoSerialized)) {
            info = JsonHelper.read(authInfoSerialized, AuthenticationInfo.class);
        }

        if (info == null) {
            forget(key);
            logger.warn("getServerContextSecrets: info was null for key: ", key);
            return null;
        }
        return info;
    }

    public void save(final ServerContext context) {
        if (context == null) {
            return;
        }

        final String key = context.getKey();
        final AuthenticationInfo authenticationInfo = context.getAuthenticationInfo();
        final String stringValue = JsonHelper.write(authenticationInfo);

        writePassword(key, stringValue);
    }

    /**
     * Forget password by overwriting existing password with null
     *
     * @param key
     */
    private static void forgetPassword(final String key) {
        writePassword(key, null);
    }

    /**
     * Writing password to the IDEA credential store
     *
     * @param key
     * @param value
     */
    private static void writePassword(final String key, final String value) {
        // use key as the serviceName (not username) for CredentialAttributes because that is the the unique identifier used to save it in the store
        final CredentialAttributes attributes = new CredentialAttributes(key);
        final Credentials credentials = new Credentials(key, value);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    /**
     * Reading password from the IDEA credential store
     *
     * @param key
     * @return unencrypted password or an empty string if no password is found
     */
    private static String readPassword(final String key) {
        final CredentialAttributes attributes = new CredentialAttributes(key);
        final Credentials credentials = PasswordSafe.getInstance().get(attributes);
        final String password = credentials != null ? credentials.getPasswordAsString() : null;
        return password != null ? password : StringUtils.EMPTY;
    }
}
