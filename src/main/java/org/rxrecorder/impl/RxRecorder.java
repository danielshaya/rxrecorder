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
import rx.observables.ConnectableObservable;
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

    public ConnectableObservable replay(ReplayOptions options) {
        Observable observable = Observable.create(s -> {
            try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build()) {
                ExcerptTailer tailer = queue.createTailer();
                long[] lastTime = new long[]{Long.MIN_VALUE};
                while (
                    tailer.readDocument(w -> {
                        ValueIn in = w.getValueIn();
                        long time = in.int64();
                        String storedFilter = in.text();
                        if(options.playUntil() > time){
                            s.onCompleted();
                            return;
                        }
                        if(options.playFrom() > time) {
                            if (options.replayStrategy() == Replay.REAL_TIME && lastTime[0] != Long.MIN_VALUE) {
                                DSUtil.sleep((int) (time - lastTime[0]));
                            }
                            if (options.using() != null) {
                                in.object(options.using(), options.using().getClass());
                                if (options.filter().equals(storedFilter)) {
                                    s.onNext(options.using());
                                }
                            } else {
                                if (options.filter().equals(storedFilter)) {
                                    Object obj = in.object();
                                    s.onNext(obj);
                                }
                            }
                            lastTime[0] = time;
                        }
                    }));
                }
            s.onCompleted();

        });
        return observable.publish();
    }

    public ConnectableObservable remoteObservable(ReplayOptions options) {
        long fromTime = System.currentTimeMillis();

        Observable observable = Observable.create(s -> {
            try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build()) {
                ExcerptTailer tailer = queue.createTailer();
                while (true){
                        tailer.readDocument(w -> {
                            ValueIn in = w.getValueIn();
                            long time = in.int64();
                            String storedFilter = in.text();
                            if(!options.playFromNow() || fromTime < time) {
                                if (options.using() != null) {
                                    if (options.filter().equals(storedFilter)) {
                                        //todo doesn't seem to work for Bytes
                                        in.object(options.using(), options.using().getClass());
                                        System.out.println(options.using());
                                        s.onNext(options.using());
                                    }
                                } else {
                                    if (options.filter().equals(storedFilter)) {
                                        Object obj = in.object();
                                        s.onNext(obj);
                                    } else if (storedFilter.equals(END_OF_STREAM)) {
                                        s.onCompleted();
                                    }
                                }
                                //todo make this a pluggle pause strategy
                                DSUtil.sleep(10);
                            }
                        });
                }
            }

        });
        return observable.publish();
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

    public void record(Observable<?> observable){
        record(observable, "");
    }

    public void record(Observable<?> observable, String filter) {
        ExcerptAppender appender;

        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(fileName).build()) {
            appender = queue.acquireAppender();
        }

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

    public void writeToFile(String fileName){
        writeToFile(fileName, false);
    }

    public void writeToFile(String fileName, boolean toStdout){
        LOG.info("Writing recording to fileName [" + fileName + "]");
        try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(this.fileName).build()) {
            ExcerptTailer tailer = queue.createTailer();
            try {
                QueueUtils.writeQueueToFile(tailer, fileName, toStdout);
            } catch (IOException e) {
                //todo log this
                e.printStackTrace();
            }
        }
        LOG.info("Writing to fileName complete");
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
