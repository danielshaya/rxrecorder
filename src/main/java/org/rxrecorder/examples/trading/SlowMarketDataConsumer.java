package org.rxrecorder.examples.trading;

import org.rxrecorder.util.DSUtil;
import rx.Subscriber;

/**
 * Created by daniel on 10/04/17.
 */
public class SlowMarketDataConsumer extends Subscriber<MarketData> {
    private String id;

    public SlowMarketDataConsumer(String id) {
        this.id = id;
    }

    @Override
    public void onCompleted() {
        System.out.println(id + ": SlowMarketDataConsumer completes");
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
        System.exit(0);
    }

    @Override
    public void onNext(MarketData marketData) {
        DSUtil.sleep(1000);
        System.out.println(id + ": SlowMarketDataConsumer consumed " + marketData);
    }
}
