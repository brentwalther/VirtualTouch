package io.walther.virtualtouch;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;


import java.util.List;

import io.walther.virtualtouch.model.HardwareManager;
import io.walther.virtualtouch.model.ReactionEvent;
import io.walther.virtualtouch.util.BasicDevice;
import io.walther.virtualtouch.util.ReactionRecorder;

public class RecordActivity extends Activity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlaybackEventListener {

    private String videoId;
    private boolean playing;
    final BasicDevice basicDevice = new BasicDevice();
    private ReactionRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);

        this.videoId = getIntent().getExtras().getString("videoId");
        this.playing = false;

        YouTubePlayerFragment mYoutubePlayerFragment = new YouTubePlayerFragment();
        mYoutubePlayerFragment.initialize(getString(R.string.YOUTUBE_API_KEY), this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_youtube_player, mYoutubePlayerFragment);
        fragmentTransaction.commit();

        Button reactionButton = (Button) findViewById(R.id.reactionButton);
        reactionButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        basicDevice.setIsReacting(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        basicDevice.setIsReacting(false);
                        break;
                }
                return false;
            }
        });

        recorder = new ReactionRecorder(basicDevice);
        if (HardwareManager.getInstance().getInputDevice() != null) {
            recorder = new ReactionRecorder(HardwareManager.getInstance().getInputDevice());
        }
        new Thread(recorder).start();
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
        recorder.setEpoch(SystemClock.uptimeMillis());
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
        recorder.stopRecording();
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
        List<ReactionEvent> reactionEvents = recorder.getReactionEvents();
        long[] reactions = new long[reactionEvents.size() * 2];
        for (int i = 0; i < reactionEvents.size(); i++) {
            ReactionEvent e = reactionEvents.get(i);
            reactions[i * 2] = e.getStartTime();
            reactions[i * 2 + 1] = e.getEndTime();
        }
        return reactions;
    }
}
