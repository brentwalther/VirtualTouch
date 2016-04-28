package io.walther.virtualtouch;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.ContextWrapper;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.services.youtube.model.Video;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import io.walther.virtualtouch.LoadResultFragment.OnLoadResultListInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Video} and makes a call to the
 * specified {@link OnLoadResultListInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyLoadResultRecyclerViewAdapter extends RecyclerView.Adapter<MyLoadResultRecyclerViewAdapter.ViewHolder> {

    private final List<Video> mValues;
    private final OnLoadResultListInteractionListener mListener;

    public MyLoadResultRecyclerViewAdapter(List<Video> items,
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
        public final TextView mNameView;
        public final TextView mContentView;
        public final TextView mChannelView;
        public Video mItem;
        private YouTubeThumbnailLoader loader;

        public ViewHolder(View view) {
            super(view);
            mView = view;

            YouTubeThumbnailView youTubeThumbnailView = (YouTubeThumbnailView)view.findViewById(R.id.loadyoutubethumbnailview);
            YouTubeThumbnailView.OnInitializedListener listener =
                    new YouTubeThumbnailView.OnInitializedListener() {
                        @Override
                        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView,
                                                            YouTubeThumbnailLoader youTubeThumbnailLoader) {
                            // hopefully they will all succeed. The video gets set later.
                            loader = youTubeThumbnailLoader;
                            if (mItem != null) {
                                loader.setVideo(mItem.getId());
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

            mIdView = (TextView) view.findViewById(R.id.loadid);
            mNameView = (TextView) view.findViewById(R.id.loadname);
            mContentView = (TextView) view.findViewById(R.id.loadcontent);
            mChannelView = (TextView) view.findViewById(R.id.loadchannel);
        }


        public void setItem(Video mItem/*, String name*/) {
            this.mItem = mItem;
            // mNameView.setText(getName(mItem.getId()));
            mContentView.setText(mItem.getSnippet().getTitle());
            mChannelView.setText(mItem.getSnippet().getChannelTitle());


            if (loader != null) {
                loader.setVideo(mItem.getId());
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
/*
        public String getName(String video_id){
            FileInputStream fis = null;
            File dir =
            File file = new File();
            try {
                fis = ContextWrapper.openFileInput(video_id);
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
            // The first value is the saved name of the reaction,
            // so we account for it by altering our computations by 1
            String name = stringReactionArray[0];

            return name;
        }*/
    }
}
