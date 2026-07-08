// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.starters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create a new commandline argument to do VSTS commands. This will allow for a protocol handler to pass IntelliJ
 * the needed arguments to start a VSTS specific workflow.
 */
public abstract class ApplicationStarterBase implements ApplicationStarter {
    private final Logger logger = LoggerFactory.getLogger(ApplicationStarterBase.class);
    public static final String VSTS_COMMAND = "vsts";
    public final String URI_PREFIX = "vsoi://";
    private static final String ACTION_NAME = "ProtocolHandler";

    public abstract String getUsageMessage();

    /**
     * Take the given command-line arguments and process them to initiate the given workflow they are calling
     *
     * @param args
     * @throws RuntimeException
     */
    protected abstract void processCommand(final List<String> args) throws RuntimeException;

    /**
     * Take the given URI and process the arguments passed inside of it to initiate the given workflow they are calling
     *
     * @param uri
     * @throws RuntimeException
     * @throws UnsupportedEncodingException
     */
    protected abstract void processUri(final String uri) throws RuntimeException, UnsupportedEncodingException;

    /**
     * The command name is no longer part of the {@link ApplicationStarter} interface; since 2026.1 it is defined by
     * the {@code id} attribute of the {@code appStarter} extension in plugin.xml and has to match this value.
     */
    public String getCommandName() {
        return VSTS_COMMAND;
    }

    @Override
    public boolean isHeadless() {
        return false;
    }

    private static void saveAll() {
        FileDocumentManager.getInstance().saveAllDocuments();
        ApplicationManager.getApplication().saveSettings();
    }

    /**
     * Checking arguments passed. They should follow the forms:
     * "vsts <command> <args>"
     * "vsts <uri>
     *
     * @param args the command line args
     * @return whether the arguments given meet the requirements
     */
    protected boolean checkArguments(List<String> args) {
        if (args.size() < 2) {
            logger.error("Azure DevOps failed due to lack of commands. Please specify the command that you want Azure DevOps to execute");
            return false;
        } else if (!StringUtils.equalsIgnoreCase(VSTS_COMMAND, args.get(0))) {
            logger.error("Azure DevOps checkout failed due to the incorrect command being used. Expected \"vsts\" but found \"{}\".", args.get(0));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void premain(List<String> args) {
        if (!checkArguments(args)) {
            System.err.println(getUsageMessage());
            // exit code IntelliJ uses checkArgs failure
            System.exit(1);
        }
    }

    @Override
    public void main(List<String> args) {
        logger.debug("Args passed to Azure DevOps to process: {}", args);
        try {
            if (StringUtils.startsWithIgnoreCase(args.get(1), URI_PREFIX)) {
                // pass the uri but after removing it's prefix since it isn't needed anymore
                processUri(args.get(1).replaceFirst(URI_PREFIX, StringUtils.EMPTY));
            } else {
                List<String> argsList = new ArrayList<String>(args);
                // remove first arg which is just the generic command "vsts" that got us to this point
                argsList.remove(0);
                processCommand(argsList);
            }
        } catch (Exception e) {
            logger.error(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, e.getMessage()));
            saveAll();

            // exit code IntelliJ uses for exceptions
            System.exit(1);
        } catch (Throwable t) {
            logger.error(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_UNEXPECTED, t.getMessage()));
            saveAll();

            // exit code IntelliJ uses for throwables
            System.exit(2);
        }

        saveAll();
    }

    @Override
    public boolean canProcessExternalCommandLine() {
        return true;
    }
}
