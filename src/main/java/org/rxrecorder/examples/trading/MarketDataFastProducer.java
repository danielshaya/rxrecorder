package org.rxrecorder.examples.trading;

import rx.Observable;
import rx.subjects.PublishSubject;
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
public class MarketDataFastProducer {
    private AtomicInteger counter = new AtomicInteger(0);
    private String id;
    private ExecutorService scheduledExecutorService;
    private Subject<MarketData, MarketData> subject;


    public MarketDataFastProducer(String id, Subject<MarketData, MarketData> subject) {
        this.id = id;
        this.subject = subject;
    }

    public void startPublishing(int delayMS) {

            if(delayMS == Integer.MIN_VALUE){
                scheduledExecutorService = Executors.newSingleThreadExecutor();
                scheduledExecutorService.submit(() -> {
                    while(true) {
                        int count = counter.incrementAndGet();
                        MarketData marketData = new MarketData(id, count, count - 0.5, count + .5);
                        //System.out.println("Published " + marketData);
                        subject.onNext(marketData);
                    }
                });
            }else {
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
                ((ScheduledExecutorService)scheduledExecutorService).scheduleAtFixedRate(() -> {
                    int count = counter.incrementAndGet();
                    MarketData marketData = new MarketData(id, count, count - 0.5, count + .5);
                    //System.out.println("Published " + marketData);
                    subject.onNext(marketData);
                }, 1, delayMS, TimeUnit.MILLISECONDS);
            }
    }

    public void stopPublishing(){
        scheduledExecutorService.shutdown();
        subject.onCompleted();
    }

    public Observable<MarketData> getObservable(){
        return subject;
    }
}
