package io.walther.virtualtouch.model;

/**
 * Created by brentwalther on 4/3/2016.
 */
public class ReactionEvent {
    private long startTime;
    private long endTime;

    public ReactionEvent(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
