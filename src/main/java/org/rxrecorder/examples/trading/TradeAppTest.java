package org.rxrecorder.examples.trading;

import org.junit.Assert;
import org.junit.Test;
import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.impl.ValidationResult;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07/12/16.
 */
public class TradeAppTest {
    private enum Mode{LIVE, REPLAY}

    private static String file = "/tmp/Trade";

    @Test
    public void test() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, false);


        ConnectableObservable<Trade> tradeObservable1 = rxRecorder.play(new ReplayOptions().filter("HT1")).publish();
        ConnectableObservable<Trade> tradeObservable2 = rxRecorder.play(new ReplayOptions().filter("HT2")).publish();


        TradeEngine engine1 = new TradeEngine("TE1");
        Observable<Order> orders1 = engine1.init(tradeObservable1);


        TradeEngine engine2 = new TradeEngine("TE2");
        engine2.init(tradeObservable2);


        Observable<ValidationResult> results = rxRecorder.validate(orders1, "output");
        results.subscribe(vi ->{
                    //System.out.println("****" + vi.getResult());
                },
                e->{},
        ()->{
            System.out.println("**** Summary " + rxRecorder.getValidationResult().summaryResult()
            + " items compared " + rxRecorder.getValidationResult().summaryItemsCompared()
            + " items valid " + rxRecorder.getValidationResult().summaryItemsValid());
            latch.countDown();
        });

        Executors.newSingleThreadExecutor().submit(()->tradeObservable1.connect());
        Executors.newSingleThreadExecutor().submit(()->tradeObservable2.connect());

        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxRecorder.getValidationResult().getResult());
    }
}
