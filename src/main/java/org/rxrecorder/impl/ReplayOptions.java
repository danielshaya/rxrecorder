package org.rxrecorder.impl;

/**
 * Created by daniel on 26/04/17.
 */
public class ReplayOptions {
    private String filter = "";
    private RxRecorder.Replay replayStrategy = RxRecorder.Replay.REAL_TIME;
    private boolean endOfStreamToken = true;
    private Object using = null;
    private boolean playFromNow = false;
    private long playFrom = Long.MAX_VALUE;
    private long playUntil = Long.MIN_VALUE;

    public String filter() {
        return filter;
    }

    public ReplayOptions filter(String filter) {
        this.filter = filter;
        return this;
    }

    public RxRecorder.Replay replayStrategy() {
        return replayStrategy;
    }

    public ReplayOptions replayStrategy(RxRecorder.Replay replayStrategy) {
        this.replayStrategy = replayStrategy;
        return this;
    }

    public boolean endOfStreamToken() {
        return endOfStreamToken;
    }

    public ReplayOptions endOfStreamToken(boolean endOfStreamToken) {
        this.endOfStreamToken = endOfStreamToken;
        return this;
    }

    public Object using() {
        return using;
    }

    public ReplayOptions using(Object using) {
        this.using = using;
        return this;
    }

    public boolean playFromNow() {
        return playFromNow;
    }

    public ReplayOptions playFromNow(boolean playFromNow) {
        this.playFromNow = playFromNow;
        return this;
    }
    public long playFrom() {
        return playFrom;
    }

    public ReplayOptions playFrom(long playFrom) {
        this.playFrom = playFrom;
        return this;
    }

    public long playUntil() {
        return playUntil;
    }

    public ReplayOptions playUntil(long playUntil) {
        this.playUntil = playUntil;
        return this;
    }
}
