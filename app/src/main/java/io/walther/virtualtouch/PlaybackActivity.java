package io.walther.virtualtouch;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import io.walther.virtualtouch.model.HardwareManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PlaybackActivity extends Activity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlaybackEventListener {

    private String videoId;
    private long[] reactions;
    private PlaybackTask playbackTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        this.videoId = getIntent().getExtras().getString("videoId");
        this.reactions = getIntent().getExtras().getLongArray("reactions");

        YouTubePlayerFragment mYoutubePlayerFragment = new YouTubePlayerFragment();
        mYoutubePlayerFragment.initialize(getString(R.string.YOUTUBE_API_KEY), this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_youtube_player, mYoutubePlayerFragment);
        fragmentTransaction.commit();

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        youTubePlayer.loadVideo(videoId);
        youTubePlayer.setPlaybackEventListener(this);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(this, "Youtube player failed to initialize!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlaying() {
        if (this.playbackTask != null) {
            this.playbackTask.stopRunning();
        }
        this.playbackTask = new PlaybackTask(getApplicationContext(), reactions);
        new Thread(this.playbackTask).start();
    }

    @Override
    public void onPaused() {

    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onBuffering(boolean b) {

    }

    @Override
    public void onSeekTo(int i) {

    }

    //The way files are loaded is that the filename is their video id
    //have to change by allowing user to select a file(reaction)
    //FUNCTION CURRENTLY NOT USED
    public void loadSavedReaction(){
        FileInputStream fis = null;
        try {
            fis = openFileInput(videoId);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuffer fileContent = new StringBuffer("");
        String reactionString;
        byte[] buffer = new byte[1024];

        try {
            int n;
            while ((n = fis.read(buffer)) != -1) {
                fileContent.append(new String(buffer, 0, n));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        reactionString = String.valueOf(fileContent);
        String[] stringReactionArray = reactionString.split(",");
        long reactionArray[] = new long[stringReactionArray.length];
        for(int i = 0; i < stringReactionArray.length; i++){
            reactionArray[i] = Long.valueOf(stringReactionArray[i]);
        }
        this.reactions = reactionArray;
    }

    public interface Reactor {
        public void react(long time);
        public void reset();
    }

    private class VibratorReactor implements Reactor {

        private Vibrator vibrator;

        public VibratorReactor(Vibrator vibrator) {
            this.vibrator = vibrator;
        }

        public void react(long time) {
            if (vibrator != null) {
                vibrator.vibrate(time);
            }
        }
        public void reset() { /* do nothing */ }
    }

    private class PlaybackTask implements Runnable {

        private final long[] reactions;
        private final Reactor reactor;
        private boolean stillRunning;

        public PlaybackTask(Context context, long[] reactions) {
            if(HardwareManager.getInstance().getOutputDevice()!=null){
                this.reactor = HardwareManager.getInstance().getOutputDevice();
            } else {
                this.reactor = new VibratorReactor((Vibrator) context.getSystemService(context.VIBRATOR_SERVICE));
            }
            this.reactions = reactions;
            this.stillRunning = true;
        }

        @Override
        public void run() {
            long startTime = SystemClock.uptimeMillis();
            int i = 0;
            while(stillRunning && i < reactions.length) {
                long timeNow = SystemClock.uptimeMillis();
                long elapsedTime = timeNow - startTime;
                if (elapsedTime > reactions[i]) {
                    long time = reactions[i + 1] - reactions[i];
                    reactor.react(time);
                    i += 2;
                }
            }
            reactor.reset();
        }

        public void stopRunning() {
            stillRunning = false;
        }
    }
}
