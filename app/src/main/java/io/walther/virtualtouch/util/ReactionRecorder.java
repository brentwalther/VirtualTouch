package io.walther.virtualtouch.util;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

import io.walther.virtualtouch.PlaybackActivity;
import io.walther.virtualtouch.model.ReactionEvent;

/**
 * Created by brentwalther on 4/10/2016.
 */
public class ReactionRecorder implements Runnable {

    private boolean paused;
    private boolean isRecording;
    private long startTime;
    private long epochTime;

    List<ReactionEvent> reactionEvents;
    List<ReactionCallback> reactionCallbacks;
    private long pauseTime;

    public interface ReactionDevice {
        public boolean isReacting();
    }

    public interface ReactionCallback {
        public void isReacting(boolean isReacting);
    }

    private final ReactionDevice device;

    public ReactionRecorder(ReactionDevice device) {
        this.device = device;
        this.reactionEvents = new LinkedList<>();
        this.reactionCallbacks = new LinkedList<>();
        this.isRecording = false;
        this.paused = true;
        this.pauseTime = -1;
    }

    public void addReactionCallback(ReactionCallback reactionCallback) {
        reactionCallbacks.add(reactionCallback);
    }

    public void pause() {
        this.paused = true;
        this.pauseTime = SystemClock.uptimeMillis();
    }

    public void setEpoch(long epochTime) {
        if (pauseTime != -1) {
            long timeToAdd = epochTime - pauseTime;
            this.epochTime += timeToAdd;
        } else {
            this.epochTime = epochTime;
        }
        this.paused = false;
    }

    @Override
    public void run() {
        isRecording = true;
        boolean isReacting = false;
        while(isRecording) {
            while(isRecording && paused) { /* noop; wait to be unpaused */ }
            long eventTime = SystemClock.uptimeMillis();
            if (device.isReacting() && !isReacting) {
                Log.d("BRENTBRENT", "Started action at: " + ((eventTime - epochTime) / 1000.0));
                startTime = eventTime - epochTime;
                isReacting = true;
            } else if (!device.isReacting() && isReacting) {
                Log.d("BRENTBRENT", "Ended action at: " + ((eventTime - epochTime) / 1000.0));
                long endTime = eventTime - epochTime;
                reactionEvents.add(new ReactionEvent(startTime, endTime));
                isReacting = false;
            }

            for (ReactionCallback callback : reactionCallbacks) {
                callback.isReacting(isReacting);
            }
        }
    }

    public void stopRecording() {
        this.isRecording = false;
    }

    public List<ReactionEvent> getReactionEvents() {
        return reactionEvents;
    }
}
