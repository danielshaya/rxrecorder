package org.rxrecorder.examples.fastproducerslowconsumer;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumerWithRxRecorder {

    private static String file = "/tmp/MarketData";

    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);
        System.setProperty("chronicle.queueBlockSize", "1");

        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, true);

        SlowConsumer slowMarketDataConsumer = new SlowConsumer("MKT1", 1000);

        FastProducer marketDataFastProducer = new FastProducer("MKT1", PublishSubject.create());
        marketDataFastProducer.startPublishing(1);
        Observable<MarketData> marketDataObservable = marketDataFastProducer.getObservable();
        rxRecorder.recordAsync(marketDataObservable,"MKT1");

        ReplayOptions options = new ReplayOptions().filter("MKT1");
        rxRecorder.play(options).subscribe(slowMarketDataConsumer);
    }
}
