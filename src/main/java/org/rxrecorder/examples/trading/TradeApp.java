package org.rxrecorder.examples.trading;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 07/12/16.
 */
public class TradeApp {
    private enum Mode{LIVE, REPLAY}
    private static Mode mode = Mode.valueOf(System.getProperty("mode", "LIVE"));
    private static boolean clearCache = Boolean.valueOf(System.getProperty("clearCache", "false"));

    private static String file = "/tmp/Trade";

    public static void main(String[] args) throws IOException {

        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(file, clearCache);

        Observable<Trade> tradeObservable1 = null;
        Observable<Trade> tradeObservable2 = null;

        if(mode == Mode.LIVE) {
            TradePublisher tradePublisher1 = new TradePublisher("HT1");
            tradeObservable1 = tradePublisher1.getEventObservable();
            rxRecorder.record(tradeObservable1, "HT1");

            TradePublisher tradePublisher2 = new TradePublisher("HT2");
            tradeObservable2 = tradePublisher2.getEventObservable();
            rxRecorder.record(tradeObservable2, "HT2");

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.schedule(()->{
                tradePublisher1.shutDown();
                tradePublisher2.shutDown();
            }, 5, TimeUnit.SECONDS);

            executorService.shutdown();
        }

        if(mode == Mode.REPLAY) {
            tradeObservable1 = rxRecorder.replay(new ReplayOptions().filter("HT1"));
            tradeObservable2 = rxRecorder.replay(new ReplayOptions().filter("HT2"));
        }

        TradeEngine engine1 = new TradeEngine("TE1");
        Observable<Order> orders1 = engine1.init(tradeObservable1);
        if(tradeObservable1 instanceof ConnectableObservable) {
            ((ConnectableObservable)tradeObservable1).connect();
        }

        TradeEngine engine2 = new TradeEngine("TE2");
        engine2.init(tradeObservable2);
        if(tradeObservable1 instanceof ConnectableObservable) {
            ((ConnectableObservable)tradeObservable2).connect();
        }

        if(mode== Mode.LIVE){
            rxRecorder.record(orders1, "output");
        }

        if(mode== Mode.REPLAY){
            rxRecorder.validate(orders1, "output");
        }

        DSUtil.sleep(5000);
    }
}
