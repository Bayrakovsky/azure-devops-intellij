// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.statusBar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.idea.common.utils.VcsHelper;
import org.jetbrains.annotations.NotNull;

public class BuildStatusBarWidgetFactory implements StatusBarWidgetFactory {

    @NotNull
    @Override
    public String getId() {
        return BuildWidget.getID();
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Azure DevOps Build";
    }

    @Override
    public boolean isAvailable(@NotNull final Project project) {
        return !IdeaHelper.isRider() || VcsHelper.isVstsRepo(project);
    }

    @NotNull
    @Override
    public StatusBarWidget createWidget(@NotNull final Project project) {
        return new BuildWidget();
    }
}
