// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Feedback entry point. Opens the community fork's own channels in the browser instead of the
 * unmaintained Microsoft "Send a Smile / Send a Frown" flow.
 */
public class FeedbackAction extends AbstractAction {
    private static final String URL_MARKETPLACE_REVIEWS =
            "https://plugins.jetbrains.com/plugin/32811-azure-devops-community/reviews";
    private static final String URL_GITHUB_ISSUES =
            "https://github.com/Bayrakovsky/azure-devops-intellij/issues";
    private static final String URL_GITHUB_DISCUSSIONS =
            "https://github.com/Bayrakovsky/azure-devops-intellij/discussions";

    public FeedbackAction() {
        super(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE));
    }

    public JMenu getSubMenu() {
        final JMenu menu = new JMenu(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE));
        for (final JBMenuItem item : createItems()) {
            menu.add(item);
        }
        return menu;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        assert e != null;

        if (e.getSource() instanceof Component) {
            final Component buttonSource = (Component) e.getSource();
            final JBPopupMenu popupMenu = new JBPopupMenu();
            for (final JBMenuItem item : createItems()) {
                popupMenu.add(item);
            }
            popupMenu.show(buttonSource, 0, buttonSource.getHeight());
        }
    }

    private JBMenuItem[] createItems() {
        return new JBMenuItem[] {
                createMenuItem(TfPluginBundle.KEY_FEEDBACK_MENU_RATE, AllIcons.Nodes.Favorite, URL_MARKETPLACE_REVIEWS),
                createMenuItem(TfPluginBundle.KEY_FEEDBACK_MENU_REPORT_ISSUE, AllIcons.Vcs.Vendors.Github, URL_GITHUB_ISSUES),
                createMenuItem(TfPluginBundle.KEY_FEEDBACK_MENU_DISCUSSIONS, AllIcons.General.Balloon, URL_GITHUB_DISCUSSIONS)
        };
    }

    private JBMenuItem createMenuItem(final String resourceKey, final Icon icon, final String url) {
        final JBMenuItem menuItem = new JBMenuItem(TfPluginBundle.message(resourceKey), icon);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                BrowserUtil.browse(url);
            }
        });
        return menuItem;
    }
}
