package io.walther.virtualtouch;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.util.LinkedList;
import java.util.List;

import io.walther.virtualtouch.model.ReactionEvent;

public class RecordActivity extends Activity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlaybackEventListener {

    // DON'T COMMIT API KEYS
    private static final String API_KEY = "";
    private String videoId;
    private boolean playing;
    private long playStartTime;
    List<ReactionEvent> reactionEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        reactionEvents = new LinkedList<>();

        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);

        this.videoId = getIntent().getExtras().getString("videoId");
        this.playing = false;

        YouTubePlayerFragment mYoutubePlayerFragment = new YouTubePlayerFragment();
        mYoutubePlayerFragment.initialize(API_KEY, this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_youtube_player, mYoutubePlayerFragment);
        fragmentTransaction.commit();

        Button reactionButton = (Button) findViewById(R.id.reactionButton);
        reactionButton.setOnTouchListener(new View.OnTouchListener() {

            private long startTime;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("BRENTBRENT", "Started action at: " + ((event.getEventTime() - playStartTime) / 1000.0));
                        startTime = event.getEventTime() - playStartTime;
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d("BRENTBRENT", "Ended action at: " + ((event.getEventTime() - playStartTime) / 1000.0));
                        long endTime = event.getEventTime() - playStartTime;
                        reactionEvents.add(new ReactionEvent(startTime, endTime));
                        break;
                }
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
        Toast.makeText(this, "Youtube player failed to initialize!", Toast.LENGTH_LONG).show();
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
        if (this.playing == false) {
            return;
        }
        this.playing = false;
        Intent intent = new Intent(this, PlaybackActivity.class);
        intent.putExtra("videoId", videoId);
        intent.putExtra("reactions", serializeReactionEvents());
        startActivity(intent);
    }

    @Override
    public void onBuffering(boolean b) {

    }

    @Override
    public void onSeekTo(int i) {

    }

    private long[] serializeReactionEvents() {
        long[] reactions = new long[reactionEvents.size() * 2];
        for(int i = 0; i < reactionEvents.size(); i++) {
            ReactionEvent e = reactionEvents.get(i);
            reactions[i*2] = e.getStartTime();
            reactions[i*2+1] = e.getEndTime();
        }
        return reactions;
    }
}
