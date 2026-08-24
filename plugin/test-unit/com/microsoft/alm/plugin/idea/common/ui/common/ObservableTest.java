// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies that {@link Observable} is a behavioral drop-in replacement for the deprecated
 * {@code java.util.Observable}.
 */
public class ObservableTest {

    /**
     * Exposes the protected mutators the same way model classes use them.
     */
    private static class TestObservable extends Observable {
        void change() {
            setChanged();
        }

        void clear() {
            clearChanged();
        }
    }

    private static class RecordingObserver implements Observer {
        final List<Object> receivedArgs = new ArrayList<Object>();
        final List<Observable> receivedSources = new ArrayList<Observable>();

        @Override
        public void update(final Observable o, final Object arg) {
            receivedSources.add(o);
            receivedArgs.add(arg);
        }
    }

    @Test
    public void notifyObservers_doesNothingWithoutSetChanged() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);

        observable.notifyObservers("arg");

        Assert.assertTrue(observer.receivedArgs.isEmpty());
    }

    @Test
    public void notifyObservers_notifiesAfterSetChangedAndClearsFlag() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);

        observable.change();
        Assert.assertTrue(observable.hasChanged());

        observable.notifyObservers("propertyName");

        Assert.assertEquals(1, observer.receivedArgs.size());
        Assert.assertEquals("propertyName", observer.receivedArgs.get(0));
        Assert.assertSame(observable, observer.receivedSources.get(0));
        Assert.assertFalse(observable.hasChanged());

        // a second notification without setChanged must not be delivered
        observable.notifyObservers("secondCall");
        Assert.assertEquals(1, observer.receivedArgs.size());
    }

    @Test
    public void notifyObservers_withoutArgumentPassesNull() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);

        observable.change();
        observable.notifyObservers();

        Assert.assertEquals(1, observer.receivedArgs.size());
        Assert.assertNull(observer.receivedArgs.get(0));
    }

    @Test
    public void addObserver_isIdempotentForSameInstance() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);
        observable.addObserver(observer);

        Assert.assertEquals(1, observable.countObservers());

        observable.change();
        observable.notifyObservers("arg");

        Assert.assertEquals(1, observer.receivedArgs.size());
    }

    @Test(expected = NullPointerException.class)
    public void addObserver_nullThrows() {
        new TestObservable().addObserver(null);
    }

    @Test
    public void deleteObserver_stopsNotifications() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);
        observable.deleteObserver(observer);

        observable.change();
        observable.notifyObservers("arg");

        Assert.assertTrue(observer.receivedArgs.isEmpty());
        Assert.assertEquals(0, observable.countObservers());
    }

    @Test
    public void deleteObserver_unknownOrNullObserverIsIgnored() {
        final TestObservable observable = new TestObservable();
        observable.addObserver(new RecordingObserver());

        observable.deleteObserver(new RecordingObserver());
        observable.deleteObserver(null);

        Assert.assertEquals(1, observable.countObservers());
    }

    @Test
    public void deleteObservers_removesEveryObserver() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver first = new RecordingObserver();
        final RecordingObserver second = new RecordingObserver();
        observable.addObserver(first);
        observable.addObserver(second);

        observable.deleteObservers();
        observable.change();
        observable.notifyObservers("arg");

        Assert.assertEquals(0, observable.countObservers());
        Assert.assertTrue(first.receivedArgs.isEmpty());
        Assert.assertTrue(second.receivedArgs.isEmpty());
    }

    @Test
    public void notifyObservers_notifiesInReverseRegistrationOrder() {
        final TestObservable observable = new TestObservable();
        final List<String> callOrder = new ArrayList<String>();
        observable.addObserver(new Observer() {
            @Override
            public void update(final Observable o, final Object arg) {
                callOrder.add("first");
            }
        });
        observable.addObserver(new Observer() {
            @Override
            public void update(final Observable o, final Object arg) {
                callOrder.add("second");
            }
        });

        observable.change();
        observable.notifyObservers("arg");

        // java.util.Observable notified observers in reverse registration order
        Assert.assertEquals("second", callOrder.get(0));
        Assert.assertEquals("first", callOrder.get(1));
    }

    @Test
    public void notifyObservers_observerAddedDuringNotificationIsNotCalledInSameRound() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver lateObserver = new RecordingObserver();
        observable.addObserver(new Observer() {
            @Override
            public void update(final Observable o, final Object arg) {
                observable.addObserver(lateObserver);
            }
        });

        observable.change();
        observable.notifyObservers("arg");

        Assert.assertTrue(lateObserver.receivedArgs.isEmpty());
        Assert.assertEquals(2, observable.countObservers());
    }

    @Test
    public void clearChanged_preventsNotification() {
        final TestObservable observable = new TestObservable();
        final RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);

        observable.change();
        observable.clear();
        observable.notifyObservers("arg");

        Assert.assertTrue(observer.receivedArgs.isEmpty());
        Assert.assertFalse(observable.hasChanged());
    }

    @Test
    public void concurrentAddAndNotify_doesNotThrow() throws InterruptedException {
        final TestObservable observable = new TestObservable();
        final AtomicInteger notifications = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final int threadCount = 8;
        final int iterations = 500;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                        for (int i = 0; i < iterations; i++) {
                            if (threadIndex % 2 == 0) {
                                final Observer observer = new Observer() {
                                    @Override
                                    public void update(final Observable o, final Object arg) {
                                        notifications.incrementAndGet();
                                    }
                                };
                                observable.addObserver(observer);
                                observable.deleteObserver(observer);
                            } else {
                                observable.change();
                                observable.notifyObservers("arg");
                            }
                        }
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                    } finally {
                        doneLatch.countDown();
                    }
                }
            });
        }

        startLatch.countDown();
        Assert.assertTrue("timed out waiting for concurrent test to finish",
                doneLatch.await(60, TimeUnit.SECONDS));
        executor.shutdownNow();

        Assert.assertNull("concurrent access must not throw", failure.get());
    }
}
