package org.rxrecorder.examples.helloworld;

import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;

/**
 * Class to run BytesToWords
 */
public class HelloWorldApp2 {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldApp2.class.getName());

    public final static String FILE_NAME = System.getProperty("filename", "/tmp/Demo");
    public final static int INTERVAL_MS = 10;
    public final static String INPUT_FILTER = "input";
    public final static String OUTPUT_FILTER = "output";

    public static void main(String[] args) throws IOException {
        Observable observableInput =
                Observable.from(new Byte[]{72,101,108,108,111,32,87,111,114,108,100,32}).map(
                        i->{
                            DSUtil.sleep(INTERVAL_MS);
                            return i;
                        });

        //Create the rxRecorder and delete any previous content by clearing the cache
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(FILE_NAME, true);

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        rxRecorder.recordAsync(observableInput, INPUT_FILTER);

        BytesToWords bytesToWords = new BytesToWords();

        //Retrieve a stream of
        ReplayOptions options = new ReplayOptions().filter(INPUT_FILTER);
        ConnectableObservable recordedObservable = rxRecorder.play(options).publish();
        //Pass the input Byte stream into the BytesToWords class which subscribes to the stream and returns
        //a stream of words.
        Observable<String> observableOutput = bytesToWords.init(recordedObservable);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        rxRecorder.record(observableOutput, OUTPUT_FILTER);
        observableOutput.subscribe(s -> LOG.info("HelloWorld2->" + s),
                throwable -> LOG.error("", throwable),
                ()->LOG.info("HelloWorld2 Complete"));
        recordedObservable.connect();
        //Sometimes useful to see the recording written to a file
        rxRecorder.writeToFile("/tmp/Demo/demo.txt", true);
    }
}
