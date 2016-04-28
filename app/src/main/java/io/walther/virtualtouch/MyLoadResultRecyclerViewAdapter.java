package io.walther.virtualtouch;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.services.youtube.model.SearchResult;

import java.util.List;

import io.walther.virtualtouch.LoadResultFragment.OnLoadResultListInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link SearchResult} and makes a call to the
 * specified {@link OnLoadResultListInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyLoadResultRecyclerViewAdapter extends RecyclerView.Adapter<MyLoadResultRecyclerViewAdapter.ViewHolder> {

    private final List<SearchResult> mValues;
    private final OnLoadResultListInteractionListener mListener;

    public MyLoadResultRecyclerViewAdapter(List<SearchResult> items,
                                           OnLoadResultListInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_loadresult, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.setItem(mValues.get(position));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onLoadResultSelected(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public final TextView mChannelView;
        public SearchResult mItem;
        private YouTubeThumbnailLoader loader;

        public ViewHolder(View view) {
            super(view);
            mView = view;

            YouTubeThumbnailView youTubeThumbnailView = (YouTubeThumbnailView)view.findViewById(R.id.youtubethumbnailview);
            YouTubeThumbnailView.OnInitializedListener listener =
                    new YouTubeThumbnailView.OnInitializedListener() {
                        @Override
                        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView,
                                                            YouTubeThumbnailLoader youTubeThumbnailLoader) {
                            // hopefully they will all succeed. The video gets set later.
                            loader = youTubeThumbnailLoader;
                            if (mItem != null) {
                                loader.setVideo(mItem.getId().getVideoId());
                            }
                            loader.release();
                        }

                        @Override
                        public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView,
                                                            YouTubeInitializationResult youTubeInitializationResult) {
                            // do nothing on failure (for now).
                        }
                    };
            youTubeThumbnailView.initialize(mView.getContext().getString(R.string.YOUTUBE_API_KEY), listener);

            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
            mChannelView = (TextView) view.findViewById(R.id.channel);
        }


        public void setItem(SearchResult mItem) {
            this.mItem = mItem;
            mContentView.setText(mItem.getSnippet().getTitle());
            mChannelView.setText(mItem.getSnippet().getChannelTitle());


            if (loader != null) {
                loader.setVideo(mItem.getId().getVideoId());
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
