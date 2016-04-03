package io.walther.virtualtouch;

import android.app.Fragment;
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
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity
        implements SearchResultFragment.OnSearchResultListInteractionListener {

    private static final String API_KEY = "AIzaSyBPJHkiABDrtD8TuGvLtwK3gg5hb8SMHpE";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.searchContainer).findViewById(R.id.doSearchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pass the buttons parent (searchContainer) to the doSearch function
                doSearch(v.getRootView());
            }
        });
    }

    public void doSearch(View view) {
        EditText editText = (EditText) view.findViewById(R.id.searchQuery);
        if (editText != null && editText.getText() != null) {
            new SearchYoutubeTask(this).execute(editText.getText().toString());
        }
    }

    private Fragment getResultListFragment() {
        return getFragmentManager().findFragmentById(R.id.resultsList);
    }

    @Override
    public void onSearchResultSelected(SearchResult item) {
        //TODO: Launch new activity that shows the video from this SearchResult
        return;
    }

    private class SearchYoutubeTask extends AsyncTask<String, Integer, List<SearchResult>> {

        private final SearchActivity activity;

        private final String LOG_TAG = SearchYoutubeTask.class.toString();

        public SearchYoutubeTask(SearchActivity activity) {
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
                return new ArrayList<>();
            }
            return searchResponse.getItems();
        }

        protected void onPostExecute(List<SearchResult> searchResultList) {
            if (searchResultList != null) {
                MySearchResultRecyclerViewAdapter adapter =
                        new MySearchResultRecyclerViewAdapter(searchResultList, activity);
                RecyclerView view = ((RecyclerView) activity.getResultListFragment().getView());
                view.swapAdapter(adapter, true);
            }
        }
    }
}
