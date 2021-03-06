package org.rxrecorder.examples.helloworld;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;

/**
 *
 */
public class HelloWorldRemote {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldRemote.class.getName());

    public static void main(String... args) throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(HelloWorldAppCold.FILE_NAME, false);
        //Get the input from the remote process
        ReplayOptions options = new ReplayOptions().filter(HelloWorldAppCold.INPUT_FILTER).playFromNow(true);
        ConnectableObservable<Byte> remoteInput = rxRecorder.play(options).publish();

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        Observable<String> observableOutput = bytesToWords.process(remoteInput);


        observableOutput.subscribe(
                s->LOG.info("Remote input [{}]", s),
                e-> LOG.error("Problem in remote [{}]", e),
                ()->{
                    LOG.info("Remote input ended");
                    System.exit(0);
                });

        remoteInput.connect();
    }
}
