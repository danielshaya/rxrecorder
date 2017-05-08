package org.rxrecorder.examples.fastproducerslowconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.Subject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to publish trades
 * Created by daniel on 23/11/16.
 */
public class FastProducer {
    private static final Logger LOG = LoggerFactory.getLogger(FastProducer.class.getName());
    private AtomicInteger counter = new AtomicInteger(0);
    private String id;
    private ExecutorService scheduledExecutorService;
    private Subject<MarketData, MarketData> subject;


    public FastProducer(String id, Subject<MarketData, MarketData> subject) {
        this.id = id;
        this.subject = subject;
    }

    public void startPublishing(int delayMS) {
        startPublishing(delayMS, false);
    }

    public void startPublishing(int delayMS, boolean logEveryItem) {
        if (delayMS == Integer.MIN_VALUE) {
            scheduledExecutorService = Executors.newSingleThreadExecutor();
            scheduledExecutorService.submit(() -> {
                while (true) {
                    process(logEveryItem);
                }
            });
        } else {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            ((ScheduledExecutorService) scheduledExecutorService).scheduleAtFixedRate(() -> {
                process(logEveryItem);
            }, 1, delayMS, TimeUnit.MILLISECONDS);
        }
    }

    private void process(boolean logEveryItem) {
        int count = counter.incrementAndGet();
        MarketData marketData = new MarketData(id, count, count - 0.5, count + .5);
        if(count % 1000 == 0 || logEveryItem){
            System.out.println("Published " + marketData);
        }
        subject.onNext(marketData);
    }


    public void stopPublishing(){
        System.out.println("Fast producer completing");
        scheduledExecutorService.shutdown();
        subject.onCompleted();
    }

    public Observable<MarketData> getObservable(){
        return subject;
    }
}
