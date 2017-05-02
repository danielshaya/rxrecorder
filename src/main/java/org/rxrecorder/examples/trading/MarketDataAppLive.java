package org.rxrecorder.examples.trading;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07/12/16.
 */
public class MarketDataAppLive {

    public static void main(String[] args) throws IOException {
        MarketDataEngine marketDataEngine = new MarketDataEngine("MKT1");

        MarketDataPublisher marketDataPublisher = new MarketDataPublisher("MKT1");
        marketDataPublisher.startPublishing();
        Observable<MarketData> marketDataObservable = marketDataPublisher.getObservable();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(() -> {
            marketDataPublisher.stopPublishing();
        }, 5, TimeUnit.SECONDS);

        executorService.shutdown();

        marketDataObservable.observeOn(Schedulers.io()).subscribe(marketDataEngine);
    }
}
