package io.walther.virtualtouch;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

public class RecordActivity extends Activity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlaybackEventListener {

    // DON'T COMMIT API KEYS
    private static final String API_KEY = "";
    private String videoId;
    private boolean playing;
    private long playStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);

        this.videoId = getIntent().getExtras().getString("videoId");

        YouTubePlayerFragment mYoutubePlayerFragment = new YouTubePlayerFragment();
        mYoutubePlayerFragment.initialize(API_KEY, this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_youtube_player, mYoutubePlayerFragment);
        fragmentTransaction.commit();

        Button reactionButton = (Button) findViewById(R.id.reactionButton);
        reactionButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("BRENTBRENT", "Seconds in to the video: " + ((event.getEventTime() - playStartTime) / 1000.0));
                return false;
            }
        });
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        youTubePlayer.loadVideo(videoId);
        youTubePlayer.setPlaybackEventListener(this);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

    }

    @Override
    public void onPlaying() {
        this.playing = true;
        this.playStartTime = SystemClock.uptimeMillis();
    }

    @Override
    public void onPaused() {
        this.playing = false;
    }

    @Override
    public void onStopped() {
        this.playing = false;
    }

    @Override
    public void onBuffering(boolean b) {

    }

    @Override
    public void onSeekTo(int i) {

    }

}
