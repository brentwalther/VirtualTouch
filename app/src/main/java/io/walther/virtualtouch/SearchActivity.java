package io.walther.virtualtouch;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String API_KEY = "AIzaSyBPJHkiABDrtD8TuGvLtwK3gg5hb8SMHpE";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;
    private List<YouTubeThumbnailLoader> loaders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        loaders = new ArrayList<>();
        YouTubeThumbnailView.OnInitializedListener listener =
                new YouTubeThumbnailView.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView,
                                                        YouTubeThumbnailLoader youTubeThumbnailLoader) {
                        // hopefully they will all succeed. The video gets set later.
                        loaders.add(youTubeThumbnailLoader);
                    }

                    @Override
                    public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView,
                                                        YouTubeInitializationResult youTubeInitializationResult) {
                        // do nothing on failure (for now).
                    }
                };


        for (int i = 0; i < NUMBER_OF_VIDEOS_RETURNED; i++) {
            LinearLayout listLayout = (LinearLayout) findViewById(R.id.resultListLayout);
            YouTubeThumbnailView youTubeThumbnailView;

            youTubeThumbnailView = new YouTubeThumbnailView(this);
            youTubeThumbnailView.initialize(API_KEY, listener);
            listLayout.addView(youTubeThumbnailView);
        }
    }

    public void doSearch(View view) {
        String[] choices = {
                "rc car race",
                "trololo",
                "animal wildlife",
                "turnpike troubadours"
        };

        new SearchYoutubeTask(this).execute(choices[Math.round((float) Math.floor(Math.random() * 4))]);
    }

    private Fragment getResultListFragment() {
        return null;
    }

    private class SearchYoutubeTask extends AsyncTask<String, Integer, List<SearchResult>> {

        private final Activity activity;

        private final String LOG_TAG = SearchYoutubeTask.class.toString();

        public SearchYoutubeTask(Activity activity) {
            this.activity = activity;
        }

        protected List<SearchResult> doInBackground(String... strings) {
            YouTube youtube = null;
            SearchListResponse searchResponse = null;
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
                String queryTerm = strings[0];

                // Define the API request for retrieving search results.
                YouTube.Search.List search = youtube.search().list("id,snippet");

                search.setKey(API_KEY);
                search.setQ(queryTerm);

                // Restrict the search results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/search/list#type
                search.setType("video");

                // To increase efficiency, only retrieve the fields that the
                // application uses.
                search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
                search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

                // Call the API and print results.
                searchResponse = search.execute();
            } catch (GoogleJsonResponseException e) {
                Log.e(LOG_TAG, "There was a service error: " + e.getDetails().getCode() + " : "
                        + e.getDetails().getMessage());
            } catch (IOException e) {
                Log.e(LOG_TAG, "There was an IO error: " + e.getCause() + " : " + e.getMessage());
            } catch (Throwable t) {
                Log.e(LOG_TAG, Log.getStackTraceString(t));
            }

            if (searchResponse == null) {
                return new ArrayList<SearchResult>();
            }
            return searchResponse.getItems();
        }

        protected void onPostExecute(List<SearchResult> searchResultList) {
            if (searchResultList != null) {
                Iterator<SearchResult> results = searchResultList.iterator();
                Iterator<YouTubeThumbnailLoader> thumbnailLoaders = loaders.iterator();
                while (results.hasNext() && thumbnailLoaders.hasNext()) {
                    SearchResult searchResult = results.next();
                    searchResult.getSnippet().getTitle();
                    thumbnailLoaders.next().setVideo(searchResult.getId().getVideoId());
                }
            }
        }
    }
}
