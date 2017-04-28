package org.rxrecorder.examples.helloworld;

import org.junit.Assert;
import org.junit.Test;
import org.rxrecorder.impl.ReplayOptions;
import org.rxrecorder.impl.RxRecorder;
import org.rxrecorder.impl.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rxrecorder.impl.RxRecorder.Replay;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A Junit test class to test BytesToWords
 */
public class HelloWorldTest {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldTest.class.getName());
    private static final Replay REPLAY_STRATEGY = Replay.FAST;

    @Test
    public void testHelloWorld() throws IOException, InterruptedException {
        //Create the rxRecorder but don't delete the cache that has been created.
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(HelloWorldApp.FILE_NAME, false);

        //Get the input from the recorder
        ReplayOptions options= new ReplayOptions().filter(HelloWorldApp.INPUT_FILTER).replayStrategy(REPLAY_STRATEGY);
        ConnectableObservable<Byte> observableInput = rxRecorder.play(options);

        BytesToWords bytesToWords = new BytesToWords();
        Observable<String> observableOutput = bytesToWords.init(observableInput);

        //Send the output stream to the recorder to be validated against the recorded output
        Observable<ValidationResult> results = rxRecorder.validate(observableOutput, HelloWorldApp.OUTPUT_FILTER);

        CountDownLatch latch = new CountDownLatch(1);
        results.subscribe(
                s->LOG.info(s.toString()),
                e-> LOG.error("Problem in init test [{}]", e),
                ()->{
                    LOG.info("Summary[" + rxRecorder.getValidationResult().summaryResult()
                            + "] items compared[" + rxRecorder.getValidationResult().summaryItemsCompared()
                            + "] items valid[" + rxRecorder.getValidationResult().summaryItemsValid() +"]");
                    latch.countDown();
                });

        observableInput.connect();
        boolean completedWithoutTimeout = latch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(ValidationResult.Result.OK, rxRecorder.getValidationResult().getResult());
        Assert.assertTrue(completedWithoutTimeout);
    }
}
