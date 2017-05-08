package org.rxrecorder.examples.fastproducerslowconsumer;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;
import rx.observables.ConnectableObservable;

import java.io.IOException;

/**
 * Created by daniel on 05/05/17.
 */
public class FastProducerSlowConsumer_Playback {
    public static void main(String[] args) throws IOException {
        DSUtil.exitAfter(10_000);
        
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init("src/main/java/org/rxrecorder/examples/fastproducerslowconsumer/resources", false);
        rxRecorder.writeToFile("/tmp/fcsp.txt", true);

        //Get the input from the recorder
        ReplayOptions options = new ReplayOptions()
                .filter("MKT1")
                .completeAtEndOfFile(true);
        ConnectableObservable<MarketData> observableInput = rxRecorder.play(options).publish();

        SlowConsumer slowMarketDataConsumer = new SlowConsumer("MKT1", 500);
        observableInput.subscribe(slowMarketDataConsumer);

        observableInput.connect();
    }

}
