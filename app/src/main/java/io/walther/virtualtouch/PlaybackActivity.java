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

    public interface Reactor {
        public void react(long time);
    }

    private class VibratorReactor implements Reactor {

        private Vibrator vibrator;

        public VibratorReactor(Vibrator vibrator) {
            this.vibrator = vibrator;
        }

        public void react(long time) {
            vibrator.vibrate(time);
        }
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
                    try {
                        reactor.react(reactions[i + 1] - reactions[i]);
                    } catch (Exception e) {
                        Log.d("BRENTBRENT", e.toString());
                    }
                    i += 2;
                }
            }
        }

        public void stopRunning() {
            stillRunning = false;
        }
    }
}
