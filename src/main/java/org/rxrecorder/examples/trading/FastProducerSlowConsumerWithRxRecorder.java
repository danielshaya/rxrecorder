package org.rxrecorder.examples.trading;

import org.rxrecorder.impl.ReplayOptions;
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
public class FastProducerSlowConsumerWithRxRecorder {

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, true);

        SlowMarketDataConsumer slowMarketDataConsumer = new SlowMarketDataConsumer("MKT1");

        MarketDataFastProducer marketDataFastProducer = new MarketDataFastProducer("MKT1", PublishSubject.create());
        marketDataFastProducer.startPublishing(1);
        Observable<MarketData> marketDataObservable = marketDataFastProducer.getObservable();
        rxRecorder.recordAsync(marketDataObservable,"MKT1");

//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//        executorService.schedule(() -> {
//            marketDataFastProducer.stopPublishing();
//            System.exit(0);
//        }, 5, TimeUnit.SECONDS);
//
//        executorService.shutdown();

        ReplayOptions options = new ReplayOptions().filter("MKT1");
        //rxRecorder.play(options).observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
        rxRecorder.play(options).subscribe(slowMarketDataConsumer);
    }
}
