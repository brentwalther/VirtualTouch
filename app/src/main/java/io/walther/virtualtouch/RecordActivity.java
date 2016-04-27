package io.walther.virtualtouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import io.walther.virtualtouch.model.HardwareManager;
import io.walther.virtualtouch.model.ReactionEvent;
import io.walther.virtualtouch.util.BasicDevice;
import io.walther.virtualtouch.util.ReactionRecorder;

public class RecordActivity extends Activity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlaybackEventListener {

    private String videoId;
    private String videoTitle;
    private String videoChannel;
    public TextView mTitleView;
    public TextView mChannelView;
    private boolean playing;
    final BasicDevice basicDevice = new BasicDevice();
    private ReactionRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        ViewGroup view = (ViewGroup) findViewById(android.R.id.content);

        this.videoId = getIntent().getExtras().getString("videoId");
        this.videoTitle = getIntent().getExtras().getString("videoTitle");
        this.videoChannel = getIntent().getExtras().getString("videoChannel");
        this.playing = false;

        mTitleView = (TextView) view.findViewById(R.id.title);
        mChannelView = (TextView) view.findViewById(R.id.channel);

        mTitleView.setText(videoTitle);
        mChannelView.setText(videoChannel);

        YouTubePlayerFragment mYoutubePlayerFragment = new YouTubePlayerFragment();
        mYoutubePlayerFragment.initialize(getString(R.string.YOUTUBE_API_KEY), this);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_youtube_player, mYoutubePlayerFragment);
        fragmentTransaction.commit();

        final LinearLayout reactionButtonWrapper = (LinearLayout) findViewById(R.id.reactionButtonWrapper);
        final Button reactionButton = (Button) findViewById(R.id.reactionButton);
        final Button inputDeviceButton = (Button) findViewById(R.id.inputDeviceButton);
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

        if (HardwareManager.getInstance().getInputDevice() != null) {
            recorder = new ReactionRecorder(HardwareManager.getInstance().getInputDevice());
            final Activity activity = this;
            recorder.addReactionCallback(new ReactionRecorder.ReactionCallback() {
                @Override
                public void isReacting(final boolean isReacting) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            inputDeviceButton.setBackgroundColor(isReacting ? getResources().getColor(R.color.softGreen) : 0);
                        }
                    });
                }
            });
            reactionButtonWrapper.setVisibility(View.GONE);
            inputDeviceButton.setVisibility(View.VISIBLE);
        } else {
            recorder = new ReactionRecorder(basicDevice);
        }
        new Thread(recorder).start();
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        youTubePlayer.loadVideo(videoId);
        youTubePlayer.setPlaybackEventListener(this);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(this, "Youtube player failed to initialize!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlaying() {
        playing = true;
        recorder.setEpoch(SystemClock.uptimeMillis());
    }

    @Override
    public void onPaused() {
        playing = false;
        recorder.pause();
    }

    @Override
    public void onStopped() {
        if (!playing) {
            return;
        }
        playing = false;
        recorder.stopRecording();
        askToSave();
    }

    @Override
    public void onBuffering(boolean b) {

    }

    @Override
    public void onSeekTo(int i) {

    }

    public void askToSave(){
        new AlertDialog.Builder(this)
                .setTitle("Save Reaction")
                .setMessage("Would you like to save the recording?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //save recording
                        saveReaction();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //continue with playback
                        createNewPlaybackActivity();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //Experimental, don't know if it works. Not fully tested yet
    //TODO: Have to create function to ask user if they want to save file
    //      If they save, ask user to name file
    public void saveReaction(){
        String reactionFileName = videoId;
        String[] reactions= reactionsToStrings(serializeReactionEvents());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name your file");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String filename = input.getText().toString();
                createNewPlaybackActivity();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(reactionFileName, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < reactions.length; i++){
            String reaction = reactions[i] + ",";
            try {
                fos.write(reaction.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createNewPlaybackActivity(){
        final Activity activity = this;
        Intent intent = new Intent(activity, PlaybackActivity.class);
        intent.putExtra("videoId", videoId);
        intent.putExtra("reactions", serializeReactionEvents());
        startActivity(intent);
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

    private String[] reactionsToStrings(long[] reactions) {
        int length = reactions.length;
        String reactionsArray[] = new String[length];
        for (int i = 0; i < reactions.length; i++) {
            reactionsArray[i] = String.valueOf(reactions[i]);
        }
        return reactionsArray;
    }

}
