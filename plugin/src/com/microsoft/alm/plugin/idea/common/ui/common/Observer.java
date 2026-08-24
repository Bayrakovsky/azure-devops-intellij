// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

/**
 * A class can implement the {@code Observer} interface when it wants to be informed of changes in
 * {@link Observable} objects.
 * <p>
 * This is a drop-in replacement for the deprecated {@code java.util.Observer} with identical semantics.
 */
public interface Observer {
    /**
     * This method is called whenever the observed object is changed.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the {@link Observable#notifyObservers(Object)} method.
     */
    void update(Observable o, Object arg);
}
