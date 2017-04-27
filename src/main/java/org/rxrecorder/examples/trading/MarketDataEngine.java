package org.rxrecorder.examples.trading;

import rx.Subscriber;

/**
 * Created by daniel on 10/04/17.
 */
public class MarketDataEngine extends Subscriber<MarketData> {
    private String id;

    public MarketDataEngine(String id) {
        this.id = id;
    }

    @Override
    public void onCompleted() {
        System.out.println(id + ": MarketDataEngine completes");
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onNext(MarketData marketData) {
        System.out.println(id + ": MaketDataEngine consumed " + marketData);
    }
}
