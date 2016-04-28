package io.walther.virtualtouch;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadActivity extends AppCompatActivity
        implements LoadResultFragment.OnLoadResultListInteractionListener {

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        Toolbar toolbar = (Toolbar) findViewById(R.id.loadtoolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new LoadYoutubeTask(this).execute();
/*
        findViewById(R.id.loadContainer).findViewById(R.id.doLoadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pass the buttons parent (loadContainer) to the doLoad function
                doLoad(v.getRootView());
            }
        });*/
    }

    /*
    public void doLoad(View view) {
        EditText editText = (EditText) view.findViewById(R.id.loadQuery);
        if (editText != null && editText.getText() != null) {
            new LoadYoutubeTask(this).execute(editText.getText().toString());
        }
    }
    */

    private Fragment getResultListFragment() {
        return getFragmentManager().findFragmentById(R.id.loadresultsList);
    }

    @Override
    public void onLoadResultSelected(Video item) {
        Intent intent = new Intent(this, PlaybackActivity.class);
        String selected_video = item.getId();
        intent.putExtra("videoId", selected_video);
        intent.putExtra("reactions", getReaction(selected_video));
        startActivity(intent);
    }

    private class LoadYoutubeTask extends AsyncTask<String, Integer, List<Video>> {

        private final LoadActivity activity;

        private final String LOG_TAG = LoadYoutubeTask.class.toString();

        public LoadYoutubeTask(LoadActivity activity) {
            this.activity = activity;
        }

        protected List<Video> doInBackground(String... strings) {
            YouTube youtube = null;
            VideoListResponse loadResponse = null;
            try {
                // This object is used to make YouTube Data API requests. The last
                // argument is required, but since we don't need anything
                // initialized when the HttpRequest is initialized, we override
                // the interface and provide a no-op function.
                youtube = new YouTube.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) throws IOException {
                    }
                }).setApplicationName("virtual-touch-android").build();

                // Prompt the user to enter a query term.
                //String queryTerm = strings[0];

                /*
                // Define the API request for retrieving load results.
                YouTube.Search.List load = youtube.search().list("id,snippet");

                load.setKey(getString(R.string.YOUTUBE_API_KEY));
                load.setQ(queryTerm);

                // Restrict the load results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/load/list#type
                load.setType("video");

                // To increase efficiency, only retrieve the fields that the
                // application uses.
                load.setFields("items(id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/channelId,snippet/channelTitle)");
                load.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
                */

                String videoId = getIds();
                YouTube.Videos.List listVideosRequest = youtube.videos().list("snippet, recordingDetails");
                listVideosRequest.setId(videoId);
                listVideosRequest.setKey(getString(R.string.YOUTUBE_API_KEY));
                listVideosRequest.setFields("items(id,snippet/title,snippet/thumbnails/default/url,snippet/channelId,snippet/channelTitle)");

                // Call the API and print results.
                loadResponse = listVideosRequest.execute();
            } catch (GoogleJsonResponseException e) {
                Log.e(LOG_TAG, "There was a service error: " + e.getDetails().getCode() + " : "
                        + e.getDetails().getMessage());
            } catch (IOException e) {
                Log.e(LOG_TAG, "There was an IO error: " + e.getCause() + " : " + e.getMessage());
            } catch (Throwable t) {
                Log.e(LOG_TAG, Log.getStackTraceString(t));
            }

            if (loadResponse == null) {
                return new ArrayList<>();
            }
            return loadResponse.getItems();
        }

        protected void onPostExecute(List<Video> loadResultList) {
            if (loadResultList != null) {
                MyLoadResultRecyclerViewAdapter adapter =
                        new MyLoadResultRecyclerViewAdapter(loadResultList, activity);
                RecyclerView view = ((RecyclerView) activity.getResultListFragment().getView());
                view.swapAdapter(adapter, true);
            }
        }
    }

    private String getIds(){
        String identifiers = "";
        File directory = getFilesDir();
        String[] filenames = directory.list();
        for (int i = 0; i < filenames.length - 1; i++){
            identifiers += filenames[i] + ",";
        }
        identifiers += filenames[filenames.length - 1];

        return identifiers;
    }

    public long[] getReaction(String video_id){
        FileInputStream fis = null;
        try {
            fis = openFileInput(video_id);
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
        StringBuffer buff = new StringBuffer("REACTIONS: ");
        for(String l : stringReactionArray) {
            buff.append(" " + l + ",");
        }
        Log.d("BRENTBRENT", buff.toString());
        // The first value is the saved name of the reaction,
        // so we account for it by altering our computations by 1
        long reactionArray[] = new long[stringReactionArray.length - 1];
        for(int i = 1; i < stringReactionArray.length; i++){
            reactionArray[i - 1] = Long.valueOf(stringReactionArray[i]);
        }

        return reactionArray;
    }
}
