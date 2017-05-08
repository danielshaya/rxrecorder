package org.rxrecorder.examples.helloworld;

import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;

/**
 * An example class that demonstrates how use rxRecorder in with a cold Observable.
 * i.e. The Observable will only produce events in response to a subscribe.
 */
public class HelloWorldAppCold {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldAppCold.class.getName());

    public final static String FILE_NAME = System.getProperty("filename", "/tmp/Demo");
    public final static int INTERVAL_MS = 1000;
    public final static String INPUT_FILTER = "input";
    public final static String OUTPUT_FILTER = "output";

    public static void main(String[] args) throws IOException {
        ConnectableObservable observableInput =
                Observable.from(new Byte[]{72,101,108,108,111,32,87,111,114,108,100,32}).map(
                        i->{
                            DSUtil.sleep(INTERVAL_MS);
                            return i;
                        }).publish();

        //Create the rxRecorder and delete any previous content by clearing the cache
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(FILE_NAME, true);

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated until 'connect' is called on the input stream.
        rxRecorder.record(observableInput, INPUT_FILTER);

        BytesToWordsProcessor bytesToWords = new BytesToWordsProcessor();
        //Pass the input Byte stream into the BytesToWordsProcessor class which subscribes to the stream and returns
        //a stream of words.
        //The subscription will not be activated until 'connect' is called on the input stream.
        Observable<String> observableOutput = bytesToWords.process(observableInput);

        //Pass the output stream (of words) into the rxRecorder which will subscribe to it and record all events.
        observableOutput.subscribe(LOG::info);
        rxRecorder.record(observableOutput, OUTPUT_FILTER);

        //Activate the subscriptions
        observableInput.connect();

        //Sometimes useful to see the recording written to a file
        rxRecorder.writeToFile("/tmp/Demo/demo.txt",true);
    }
}
