package org.rxrecorder.examples.trading;

import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.io.IOException;

/**
 * Created by daniel on 07/12/16.
 */
public class FastProducerSlowConsumer {

    public static void main(String[] args) throws IOException {
        SlowMarketDataConsumer slowMarketDataConsumer = new SlowMarketDataConsumer("MKT1");


        //Subject<MarketData, MarketData> marketDataSubject = ReplaySubject.create();
        Subject<MarketData, MarketData> marketDataSubject = PublishSubject.create();
        MarketDataFastProducer marketDataFastProducer = new MarketDataFastProducer("MKT1", marketDataSubject);
        marketDataFastProducer.startPublishing(Integer.MIN_VALUE);


        //Slows down the market data publisher which is not acceptable
        //marketDataObservable.subscribe(slowMarketDataConsumer);

        //Back pressure causes the program to fail if PublisherSubject
        marketDataSubject.observeOn(Schedulers.io()).subscribe(slowMarketDataConsumer);
    }
}
