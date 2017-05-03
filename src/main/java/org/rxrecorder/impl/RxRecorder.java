package org.rxrecorder.impl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueIn;
import org.rxrecorder.util.QueueUtils;
import org.rxrecorder.util.DSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Class to record input from Observables and playback and validate recordings.
 */
public class RxRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(QueueUtils.class.getName());
    private String fileName;
    private ValidationResult validationResult;
    private String END_OF_STREAM = "endOfStream";

    public enum Replay {REAL_TIME, FAST}

    public Observable play(ReplayOptions options) {
        long fromTime = System.currentTimeMillis();

        Observable observable = Observable.create(subscriber -> {
            try (ChronicleQueue queue = createQueue()) {
                ExcerptTailer tailer = queue.createTailer();
                long[] lastTime = new long[]{Long.MIN_VALUE};
                boolean[] stop = new boolean[]{false};
                while (true) {

                    boolean foundItem = tailer.readDocument(w -> {
                        ValueIn in = w.getValueIn();
                        long recordedAtTime = in.int64();
                        String storedWithFilter = in.text();

                        if (testEndOfStream(subscriber, storedWithFilter)) {
                            stop[0] = true;
                            return;
                        }

                        if (testPastPlayUntil(options, subscriber, recordedAtTime)){
                            stop[0] = true;
                            return;
                        }

                        if (options.playFrom() > recordedAtTime
                                && (!options.playFromNow() || fromTime < recordedAtTime)) {
                            pause(options, lastTime, recordedAtTime);

                            if (options.filter().equals(storedWithFilter)) {
                                subscriber.onNext(getStoredObject(options, in));
                            }
                            lastTime[0] = recordedAtTime;
                        }
                    });
                    if (!foundItem && !options.waitForMoreItems() || stop[0]) {
                        subscriber.onCompleted();
                        return;
                    }
                }
            }

        });
        return observable;
    }

    private boolean testPastPlayUntil(ReplayOptions options, Subscriber<? super Object> s, long recordedAtTime) {
        if(options.playUntil() > recordedAtTime){
            s.onCompleted();
            return true;
        }
        return false;
    }

    private boolean testEndOfStream(Subscriber<? super Object> s, String storedWithFilter) {
        if (storedWithFilter.equals(END_OF_STREAM)) {
            s.onCompleted();
            return true;
        }
        return false;
    }

    private Object getStoredObject(ReplayOptions options, ValueIn in) {
        Object storedObject;
        if (options.using() != null) {
            storedObject = in.object(options.using(), options.using().getClass());
        } else {
            storedObject = in.object();
        }
        return storedObject;
    }

    private void pause(ReplayOptions options, long[] lastTime, long recordedAtTime) {
        if (options.replayStrategy() == Replay.REAL_TIME && lastTime[0] != Long.MIN_VALUE) {
            DSUtil.sleep((int) (recordedAtTime - lastTime[0]));
        }
        //todo add configurable pause strategy
    }

    public Observable<ValidationResult> validate(Observable observable, String filter) {
        Subject<ValidationResult, ValidationResult>  validatorPublisher = PublishSubject.create();
        ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build();
        ExcerptTailer tailer = queue.createTailer();
        validationResult = new ValidationResult();

        observable.subscribe(generatedResult -> {
                Object onQueue = getNextMatchingFilter(tailer, filter);
                if (onQueue.equals(generatedResult)) {
                    validationResult.setResult(ValidationResult.Result.OK);
                } else {
                    validationResult.setResult(ValidationResult.Result.BAD);
                }
                validationResult.setFromQueue(onQueue);
                validationResult.setGenerated(generatedResult);
                validatorPublisher.onNext(validationResult);
            },
            error -> {
                LOG.error("error in validate [{}]", error);
            },
            () -> {
                validatorPublisher.onCompleted();
                queue.close();
            });

        return validatorPublisher;
    }

    public ValidationResult getValidationResult(){
        return validationResult;
    }

    private Object getNextMatchingFilter(ExcerptTailer tailer, String filter){
        long index = tailer.index();
        DocumentContext dc = tailer.readingDocument();

        if(!dc.isPresent()){
            throw new IllegalStateException("No value left on queue");
        }

        ValueIn in = dc.wire().getValueIn();
        long time = in.int64();
        String storedFilter = in.text();
        Object valueFromQueue = in.object();

        if(storedFilter.equals(filter)){
            return valueFromQueue;
        }else{
            tailer.moveToIndex(++index);
            return getNextMatchingFilter(tailer, filter);
        }

    }

    public void recordAsync(Observable<?> observable, String filter){
        new Thread(()->record(observable,filter)).start();
    }

    public void record(Observable<?> observable){
        record(observable, "");
    }

    public void record(Observable<?> observable, String filter) {
        ChronicleQueue queue = createQueue();

        ExcerptAppender appender = queue.acquireAppender();

        Consumer consumer = t -> appender.writeDocument(w -> {
            w.getValueOut().int64(System.currentTimeMillis());
            w.getValueOut().text(filter);
            w.getValueOut().object(t);
        });

        observable.subscribe(
            t-> {
                consumer.accept(t);
            },
            e->LOG.error("Error whilst recording [{}]", e),
            ()-> {
                LOG.info("Adding end of stream token");
                appender.writeDocument(w -> {
                    w.getValueOut().int64(System.currentTimeMillis());
                    w.getValueOut().text(END_OF_STREAM);
                    w.getValueOut().object(new EndOfStream());
                });
            });
    }

    public void writeToFile(String fileOutput){
        writeToFile(fileOutput, false);
    }

    public void writeToFile(String fileOutput, boolean toStdout){
        LOG.info("Writing recording to fileName [" + fileOutput + "]");
        try (ChronicleQueue queue = createQueue()) {
            ExcerptTailer tailer = queue.createTailer();
            try {
                QueueUtils.writeQueueToFile(tailer, fileOutput, toStdout);
            } catch (IOException e) {
                //todo log this
                e.printStackTrace();
            }
        }
        LOG.info("Writing to fileName complete");
    }

    private ChronicleQueue createQueue(){
        int blockSize = Integer.getInteger("chronicle.queueBlockSize", -1);
        ChronicleQueue queue = null;
        if(blockSize==-1) {
            queue = SingleChronicleQueueBuilder.binary(fileName).build();
        }else {
            queue = SingleChronicleQueueBuilder.binary(fileName).blockSize(blockSize).build();
        }
        return queue;
    }

    public void init(String file, boolean clearCache) throws IOException {
        LOG.info("Initialising RxRecorder on fileName [{}]", file);
        if(clearCache) {
            LOG.info("Deleting existing recording [{}]", file);
            if(Files.exists(Paths.get(file))) {
                Files.walk(Paths.get(file))
                        .map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2))
                        .forEach(File::delete);
                Files.deleteIfExists(Paths.get(file));
            }
        }
        this.fileName = file;
    }
}
