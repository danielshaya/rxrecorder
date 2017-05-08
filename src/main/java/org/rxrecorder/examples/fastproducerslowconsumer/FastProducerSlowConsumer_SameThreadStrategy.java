package org.rxrecorder.examples.fastproducerslowconsumer;

import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumer_SameThreadStrategy {

    public static void main(String[] args) throws IOException {
        SlowConsumer slowMarketDataConsumer = new SlowConsumer("MKT1", 1000);

        Subject<MarketData, MarketData> marketDataSubject = PublishSubject.create();
        FastProducer marketDataFastProducer = new FastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(1, true);

        //Back pressure causes the program to fail if PublisherSubject
        marketDataSubject.subscribe(slowMarketDataConsumer);
    }
}
