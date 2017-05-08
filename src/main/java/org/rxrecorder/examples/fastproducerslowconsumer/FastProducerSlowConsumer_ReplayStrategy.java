package org.rxrecorder.examples.fastproducerslowconsumer;

import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumer_ReplayStrategy {

    public static void main(String[] args) throws IOException {
        SlowConsumer slowMarketDataConsumer = new SlowConsumer("MKT1", 1000);

        Subject<MarketData, MarketData> marketDataSubject = ReplaySubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(Integer.MIN_VALUE);

        //Back pressure causes the program to fail if PublisherSubject
        marketDataSubject.observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
    }
}
