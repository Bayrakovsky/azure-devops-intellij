// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an observable object, or "data" in the model-view paradigm.
 * <p>
 * This is a drop-in replacement for the deprecated {@code java.util.Observable} with identical semantics,
 * including notifying observers in reverse registration order and only when {@link #setChanged()} has been
 * called before {@link #notifyObservers(Object)}.
 */
public class Observable {
    private boolean changed = false;
    private final List<Observer> observers = new ArrayList<>();

    /**
     * Adds an observer to the set of observers for this object, provided that it is not the same as some
     * observer already in the set.
     *
     * @param o an observer to be added.
     */
    public synchronized void addObserver(final Observer o) {
        if (o == null) {
            throw new NullPointerException();
        }
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    /**
     * Deletes an observer from the set of observers of this object. Passing {@code null} has no effect.
     *
     * @param o the observer to be deleted.
     */
    public synchronized void deleteObserver(final Observer o) {
        observers.remove(o);
    }

    /**
     * Notifies all observers if this object has changed, as indicated by the {@link #hasChanged()} method,
     * with {@code null} as the argument.
     */
    public void notifyObservers() {
        notifyObservers(null);
    }

    /**
     * If this object has changed, as indicated by the {@link #hasChanged()} method, then notify all of its
     * observers, clear the changed flag and then pass the given argument to their {@code update} method.
     *
     * @param arg any object passed to the observers.
     */
    public void notifyObservers(final Object arg) {
        final Observer[] snapshot;

        synchronized (this) {
            if (!changed) {
                return;
            }
            snapshot = observers.toArray(new Observer[0]);
            clearChanged();
        }

        // java.util.Observable notified observers in reverse registration order; keep that behavior
        for (int i = snapshot.length - 1; i >= 0; i--) {
            snapshot[i].update(this, arg);
        }
    }

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    public synchronized void deleteObservers() {
        observers.clear();
    }

    /**
     * Marks this {@code Observable} object as having been changed; the {@link #hasChanged()} method will now
     * return {@code true}.
     */
    protected synchronized void setChanged() {
        changed = true;
    }

    /**
     * Indicates that this object has no longer changed, or that it has already notified all of its observers
     * of its most recent change.
     */
    protected synchronized void clearChanged() {
        changed = false;
    }

    /**
     * Tests if this object has changed.
     */
    public synchronized boolean hasChanged() {
        return changed;
    }

    /**
     * Returns the number of observers of this {@code Observable} object.
     */
    public synchronized int countObservers() {
        return observers.size();
    }
}
