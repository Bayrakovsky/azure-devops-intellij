// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.microsoft.alm.plugin.context.RepositoryContext;
import com.microsoft.alm.plugin.events.ServerEvent;
import com.microsoft.alm.plugin.events.ServerEventListener;
import com.microsoft.alm.plugin.events.ServerEventManager;
import com.microsoft.alm.plugin.idea.common.utils.EventContextHelper;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import com.microsoft.alm.plugin.operations.BuildStatusLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.OperationFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class StatusBarManager {
    private static final Logger logger = LoggerFactory.getLogger(StatusBarManager.class);
    private static ServerEventListener serverEventListener;

    public static void setupStatusBar() {
        if (serverEventListener == null) {
            serverEventListener = new ServerEventListener() {
                @Override
                public void serverChanged(final ServerEvent event, final Map<String, Object> contextMap) {
                    // When we receive an event that builds have changed, update the status bar (ON UI THREAD)
                    if (event == ServerEvent.BUILDS_CHANGED) {
                        IdeaHelper.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                // Avoid letting exceptions bubble out here and cause error notifications
                                try {
                                    // Check the context object to see if these change events were triggered by IntelliJ
                                    if (EventContextHelper.isProjectOpened(contextMap)
                                            || EventContextHelper.isRepositoryChanged(contextMap)) {
                                        // On project opened or repo changed we use the project context to update the status bar
                                        updateStatusBar(EventContextHelper.getProject(contextMap), false);
                                    } else if (EventContextHelper.isProjectClosing(contextMap)) {
                                        // Widget lifecycle is managed by StatusBarWidgetFactory on project close
                                        refreshBuildWidgetAvailability(EventContextHelper.getProject(contextMap));
                                    } else {
                                        // If there isn't any context, then we were called by the polling timer
                                        // Just update all the status bars for all the projects
                                        updateStatusBar();
                                    }
                                } catch (final Throwable t) {
                                    logger.warn("Unable to update the status bar.", t);
                                }
                            }
                        });
                    }
                }
            };
            // Add the listener to the server event manager
            ServerEventManager.getInstance().addListener(serverEventListener);
        }
    }

    private static void updateStatusBar() {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (final Project p : openProjects) {
            updateStatusBar(p, false);
        }
    }

    public static void updateStatusBar(final Project project, final boolean allowPrompt) {
        refreshBuildWidgetAvailability(project);

        if (IdeaHelper.isRider() && !VcsHelper.isVstsRepo(project)) {
            return;
        }

        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            updateWidgets(statusBar, project, allowPrompt);
        }
    }

    private static void updateWidgets(final StatusBar statusBar, final Project project, final boolean allowPrompt) {
        BuildWidget buildWidget = (BuildWidget) statusBar.getWidget(BuildWidget.getID());
        if (buildWidget == null) {
            return;
        }

        // Attempt to get the current repository context (if none, then the status stays as it was)
        final RepositoryContext repositoryContext = VcsHelper.getRepositoryContext(project);
        if (repositoryContext != null) {
            final BuildWidget widget = buildWidget;

            // Create the operation and start the background work to get the latest build information
            final BuildStatusLookupOperation op = OperationFactory.createBuildStatusLookupOperation(repositoryContext, allowPrompt);
            op.addListener(new Operation.Listener() {
                @Override
                public void notifyLookupStarted() { /* do nothing */ }

                @Override
                public void notifyLookupCompleted() { /* do nothing */ }

                @Override
                public void notifyLookupResults(final Operation.Results results) {
                    updateBuildWidget(project, statusBar, widget, (BuildStatusLookupOperation.BuildStatusResults) results);
                }
            });
            op.doWorkAsync(null);

        } else {
            // The repository hasn't been opened yet, we should get an event when it is opened
        }
    }

    private static void updateBuildWidget(final Project project, final StatusBar statusBar, final BuildWidget widget, final BuildStatusLookupOperation.BuildStatusResults results) {
        final BuildStatusLookupOperation.BuildStatusResults r = results;
        final BuildStatusModel model = BuildStatusModel.create(project, results);
        widget.update(model);

        // Tell the UI to update and restart the timer
        // (This should be done on the UI thread)
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                statusBar.updateWidget(BuildWidget.getID());
            }
        });
    }

    private static void refreshBuildWidgetAvailability(@Nullable final Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        final StatusBarWidgetFactory factory = findBuildWidgetFactory();
        if (factory != null) {
            project.getService(StatusBarWidgetsManager.class).updateWidget(factory);
        }
    }

    @Nullable
    private static StatusBarWidgetFactory findBuildWidgetFactory() {
        for (final StatusBarWidgetFactory factory : StatusBarWidgetFactory.EP_NAME.getExtensions()) {
            if (BuildWidget.getID().equals(factory.getId())) {
                return factory;
            }
        }
        return null;
    }
}
