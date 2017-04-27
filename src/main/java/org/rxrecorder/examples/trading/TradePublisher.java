package org.rxrecorder.examples.trading;

import rx.Observable;
import rx.Subscriber;
import rx.observables.ConnectableObservable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to publish trades
 * Created by daniel on 23/11/16.
 */
public class TradePublisher {
    private final String id;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Observable<Trade> observable;
    private final AtomicInteger counter = new AtomicInteger(0);
    private Subscriber subscriber = null;

    public TradePublisher(String id) {
        this.id = id;
    }

    public ConnectableObservable<Trade> getEventObservable() {
        observable = Observable.create(s -> {
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                subscriber = s;
                Trade t = new Trade(id, counter.incrementAndGet(), counter.incrementAndGet());
                System.out.println(id + "Publishing " + t);
                s.onNext(t);
            }, 0, 1, TimeUnit.SECONDS);
        });
        return observable.publish();
    }

    public void shutDown() {
        scheduledExecutorService.shutdown();
        subscriber.onCompleted();
    }
}
