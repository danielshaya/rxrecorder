package org.rxrecorder.examples.trading;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07/12/16.
 */
public class MarketDataAppWithRxRecorder {

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, true);

        MarketDataEngine marketDataEngine = new MarketDataEngine("MKT1");

        MarketDataPublisher marketDataPublisher = new MarketDataPublisher("MKT1");
        marketDataPublisher.startPublishing();
        Observable<MarketData> marketDataObservable = marketDataPublisher.getObservable();
        rxRecorder.recordAsync(marketDataObservable,"MKT1");

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.schedule(() -> {
            marketDataPublisher.stopPublishing();
            System.exit(0);
        }, 5, TimeUnit.SECONDS);

        executorService.shutdown();

        ReplayOptions options = new ReplayOptions().filter("MKT1");
        //rxRecorder.play(options).observeOn(Schedulers.io()).subscribe(marketDataEngine);
        rxRecorder.play(options).subscribe(marketDataEngine);
    }
}
