package org.rxrecorder.examples.trading;

import org.rxrecorder.examples.fastproducerslowconsumer.MarketData;
import org.rxrecorder.examples.fastproducerslowconsumer.FastProducer;
import org.rxrecorder.impl.RxRecorder;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07/12/16.
 */
public class MarketDataValidator {
    private enum Mode{LIVE, REPLAY}
    private static Mode mode = Mode.valueOf(System.getProperty("mode", "LIVE"));
    private static boolean clearCache = Boolean.valueOf(System.getProperty("clearCache", "false"));

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        validate();
    }



    private static void validate() throws IOException {
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, clearCache);

        Observable<MarketData> marketDataObservable = null;

        FastProducer marketDataFastProducer = new FastProducer("MKT1", PublishSubject.create());
        marketDataFastProducer.startPublishing(1);
        marketDataObservable = marketDataFastProducer.getObservable();
        rxRecorder.validate(marketDataObservable, "");

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(()->{
            marketDataFastProducer.stopPublishing();
        }, 5, TimeUnit.SECONDS);

        executor.shutdown();
    }

}
