// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Replaces the old AbstractTreeBuilder-based implementation (the class was removed from the platform) with
 * StructureTreeModel + AsyncTreeModel. Children are always computed on the model's background invoker thread,
 * so the server calls made by {@link TfsTreeNode#getChildren()} do not block the EDT.
 */
public class TfsTreeBuilder implements Disposable {
    private static final Logger LOG = Logger.getInstance(TfsTreeBuilder.class.getName());

    private static final Comparator<NodeDescriptor<?>> COMPARATOR = new Comparator<NodeDescriptor<?>>() {
        public int compare(NodeDescriptor<?> o1, NodeDescriptor<?> o2) {
            if (o1 instanceof TfsErrorTreeNode) {
                return o2 instanceof TfsErrorTreeNode ? ((TfsErrorTreeNode) o1).getMessage().compareTo(((TfsErrorTreeNode) o2).getMessage()) : -1;
            } else if (o2 instanceof TfsErrorTreeNode) {
                return 1;
            }

            final TfsTreeNode n1 = (TfsTreeNode) o1;
            final TfsTreeNode n2 = (TfsTreeNode) o2;
            if (n1.isDirectory() && !n2.isDirectory()) {
                return -1;
            } else if (!n1.isDirectory() && n2.isDirectory()) {
                return 1;
            }

            return n1.getFileName().compareToIgnoreCase(n2.getFileName());
        }
    };

    private final JTree tree;
    private final StructureTreeModel<SimpleTreeStructure> structureModel;

    public static TfsTreeBuilder createInstance(@NotNull final TfsTreeNode root, @NotNull final JTree tree) {
        final SimpleTreeStructure treeStructure = new SimpleTreeStructure.Impl(root) {
            @Override
            public boolean isToBuildChildrenInBackground(@NotNull Object element) {
                return true;
            }

            @Override
            public boolean isAlwaysLeaf(@NotNull Object element) {
                if (element instanceof TfsTreeNode) {
                    return !((TfsTreeNode) element).isDirectory();
                } else {
                    LOG.assertTrue(element instanceof TfsErrorTreeNode);
                    return true;
                }
            }
        };
        return new TfsTreeBuilder(tree, treeStructure);
    }

    private TfsTreeBuilder(final JTree tree, final SimpleTreeStructure treeStructure) {
        this.tree = tree;
        structureModel = new StructureTreeModel<SimpleTreeStructure>(treeStructure, COMPARATOR, this);
        tree.setModel(new AsyncTreeModel(structureModel, this));
    }

    @NotNull
    public Set<Object> getSelectedElements() {
        final TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return Collections.emptySet();
        }

        final Set<Object> result = new HashSet<Object>();
        for (final TreePath path : paths) {
            final Object node = path.getLastPathComponent();
            if (node instanceof DefaultMutableTreeNode) {
                result.add(((DefaultMutableTreeNode) node).getUserObject());
            }
        }
        return result;
    }

    public void select(@NotNull final TfsTreeNode node) {
        structureModel.select(node, tree, path -> {
        });
    }

    @NotNull
    public Promise<TreePath> queueUpdateFrom(@NotNull final TfsTreeNode node, final boolean structure) {
        return structureModel.invalidate(node, structure);
    }

    @Override
    public void dispose() {
    }
}
