// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.setup;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.AuthTypes;
import com.microsoft.alm.plugin.events.ServerPollingManager;
import com.microsoft.alm.plugin.idea.common.services.CredentialsPromptImpl;
import com.microsoft.alm.plugin.idea.common.services.DeviceFlowResponsePromptImpl;
import com.microsoft.alm.plugin.idea.common.services.HttpProxyServiceImpl;
import com.microsoft.alm.plugin.idea.common.services.IdeaAsyncService;
import com.microsoft.alm.plugin.idea.common.services.IdeaCertificateService;
import com.microsoft.alm.plugin.idea.common.services.LocalizationServiceImpl;
import com.microsoft.alm.plugin.idea.common.services.PropertyServiceImpl;
import com.microsoft.alm.plugin.idea.common.services.ServerContextStoreImpl;
import com.microsoft.alm.plugin.idea.common.statusBar.StatusBarManager;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes and configures plugin at startup.
 * <p>
 * Registered as an {@link AppLifecycleListener} in plugin.xml (the old application component model is deprecated).
 */
public class ApplicationStartup implements AppLifecycleListener {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartup.class);
    private static final String USER_HOME_DIR = System.getProperty("user.home");
    private static final String VSTS_DIR = ".vsts";
    private static final String LOCATION_FILE = "locations.csv";
    private static final String CSV_COMMA = ",";

    public ApplicationStartup() {
    }

    @Override
    public void appFrameCreated(final List<String> commandLineArgs) {
        initComponent();
    }

    public void initComponent() {
        // Setup the services that the core plugin components need
        PluginServiceProvider.getInstance().initialize(
                new ServerContextStoreImpl(),
                new CredentialsPromptImpl(),
                new DeviceFlowResponsePromptImpl(),
                PropertyServiceImpl.getInstance(),
                LocalizationServiceImpl.getInstance(),
                new HttpProxyServiceImpl(),
                new IdeaAsyncService(),
                new IdeaCertificateService(),
                true);

        final File vstsDirectory = setupPreferenceDir(USER_HOME_DIR);
        final String ideLocation = getIdeLocation();
        doOsSetup(vstsDirectory, ideLocation);

        // Setup status bar
        StatusBarManager.setupStatusBar();

        // Project and repository events are delivered via ProjectRepoEventManager's
        // postStartupActivity and ProjectCloseListener registrations in plugin.xml.

        // Start polling for server events
        ServerPollingManager.getInstance().startPolling();

        // Check for auth type settings
        configureAuthType();
    }

    /**
     * Create .vsts directory in user's home directory if not already there
     *
     * @param parentDirectory
     * @return the vsts directory
     */
    protected File setupPreferenceDir(final String parentDirectory) {
        final File parent = new File(parentDirectory);
        final File vstsDirectory = new File(parent, VSTS_DIR);
        if (!vstsDirectory.exists()) {
            vstsDirectory.mkdir();
        }
        return vstsDirectory;
    }

    /**
     * Returns the IDE executable directory (bin on Linux/Windows, MacOS on macOS).
     */
    protected String getIdeLocation() {
        return PathManager.getBinPath();
    }

    /**
     * Create the locations.csv file if it doesn't exist or check it for the IDE location.
     * If no location is found or they mismatch, insert the new location into the file.
     *
     * @param vstsDirectory
     * @param currentLocation
     */
    protected void cacheIdeLocation(final File vstsDirectory, final String currentLocation) {
        final Map<String, String> locationEntries = new HashMap<String, String>();
        final File locationsFile = new File(vstsDirectory, LOCATION_FILE);
        final String ideName = ApplicationNamesInfo.getInstance().getProductName().toLowerCase();
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        String currentEntry = StringUtils.EMPTY;

        try {
            // if file doesn't exist create it else read the entries in it
            if (!locationsFile.exists()) {
                locationsFile.createNewFile();
            } else {
                String line;
                bufferedReader = new BufferedReader(new FileReader(locationsFile));
                while ((line = bufferedReader.readLine()) != null) {
                    final String[] entry = line.split(CSV_COMMA);
                    if (entry.length == 2) {
                        // find existing IDE entry if there is one
                        if (ideName.equals(entry[0])) {
                            currentEntry = entry[1];
                        }
                        locationEntries.put(entry[0], entry[1]);
                    }
                }
                bufferedReader.close();
            }

            if (!currentEntry.equals(currentLocation) && !currentLocation.isEmpty()) {
                // delete current entry if it exists
                if (!currentEntry.isEmpty()) {
                    locationEntries.remove(ideName);
                }

                // add current entry
                locationEntries.put(ideName, currentLocation);

                // rewrite file with new entry
                bufferedWriter = new BufferedWriter(new FileWriter(locationsFile.getPath()));
                for (String key : locationEntries.keySet()) {
                    bufferedWriter.write(key + CSV_COMMA + locationEntries.get(key) + "\n");
                }
                bufferedWriter.close();
            }
        } catch (FileNotFoundException e) {
            logger.warn("A FileNotFoundException was caught while trying to cache the IDE location", e);
        } catch (IOException e) {
            logger.warn("An IOException was caught while trying to cache the IDE location", e);
        } catch (Exception e) {
            logger.warn("An Exception was caught while trying to cache the IDE location", e);
        } finally {
            // try closing the buffered reader/writer in case it was missed
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                logger.warn("An IOException was caught while trying to close the buffered reader/writer", e);
            }
        }
    }

    /**
     * Finds the OS type the plugin is running on and calls the setup for it
     */
    protected void doOsSetup(final File vstsDirectory, final String ideLocation) {
        if (StringUtils.isNotEmpty(ideLocation)) {
            cacheIdeLocation(vstsDirectory, ideLocation);
        }
    }

    /**
     * Check if the auth type is set in the settings file or by the VM options
     */
    @VisibleForTesting
    protected void configureAuthType() {
        // if settings file has the auth type saved then set it for the session
        if (AuthHelper.isAuthTypeFromSettingsFileSet()) {
            AuthHelper.setDeviceFlowEnvFromSettingsFile();
        } else if (AuthHelper.isDeviceFlowEnvSetTrue()) {
            // this is the case where users are using the VM option to set device flow (legacy way)
            // update the settings file to do device flow and then we will just look at the settings value going further
            AuthHelper.setAuthTypeInSettingsFile(AuthTypes.DEVICE_FLOW);
        }
    }
}