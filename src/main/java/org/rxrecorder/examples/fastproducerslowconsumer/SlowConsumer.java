package org.rxrecorder.examples.fastproducerslowconsumer;

import org.rxrecorder.util.DSUtil;
import rx.Subscriber;

/**
 * Created by daniel on 10/04/17.
 */
public class SlowConsumer extends Subscriber<MarketData> {
    private String id;
    private int delayMS;
    private int count;

    public SlowConsumer(String id, int delayMS) {
        this.id = id;
        this.delayMS = delayMS;
    }

    @Override
    public void onCompleted() {
        System.out.println(id + ": SlowConsumer completes");
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onNext(MarketData marketData) {
        DSUtil.sleep(delayMS);
        System.out.println(++count + ":" + id + ": SlowConsumer consumed " + marketData);
    }
}
