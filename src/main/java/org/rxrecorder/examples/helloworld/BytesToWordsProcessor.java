package org.rxrecorder.examples.helloworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * A simple BytesToWordsProcessor program. Receives a stream of Bytes and converts to
 * a stream of words. (This is intended for demonstration purposes only.)
 */
public class BytesToWordsProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BytesToWordsProcessor.class.getName());
    public Observable<String> process(Observable<Byte> observableInput){

        Subject<String, String> observableOutput = PublishSubject.create();

        StringBuilder sb = new StringBuilder();
        observableInput.subscribe(b->{
                if(b==32){ //send out a new word on a space
                    observableOutput.onNext(sb.toString());
                    sb.setLength(0);
                }else{
                    sb.append((char)b.byteValue());
                }
            },
            e->LOG.error("Error in BytesToWordsProcessor [{}]", e),
            observableOutput::onCompleted
        );

        return observableOutput;
    }
}
