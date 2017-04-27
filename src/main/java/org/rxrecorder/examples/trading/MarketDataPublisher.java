package org.rxrecorder.examples.trading;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to publish trades
 * Created by daniel on 23/11/16.
 */
public class MarketDataPublisher {
    private AtomicInteger counter = new AtomicInteger(0);
    private String id;
    private ScheduledExecutorService scheduledExecutorService;
    private Subject<MarketData, MarketData> subject = PublishSubject.create();


    public MarketDataPublisher(String id) {
        this.id = id;
    }

    public void startPublishing() {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                int count = counter.incrementAndGet();
                MarketData marketData = new MarketData(id, count, count - 0.5, count + .5);
                System.out.println(id + "Publishing " + marketData);
                subject.onNext(marketData);
            }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopPublishing(){
        scheduledExecutorService.shutdown();
        subject.onCompleted();
    }

    public static void main(String[] args) {
        MarketDataPublisher marketDataPublisher = new MarketDataPublisher("test");
        marketDataPublisher.startPublishing();
    }

    public Observable<MarketData> getObservable(){
        return subject;
    }
}
