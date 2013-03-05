
package com.tonicartos.component.internal;

import com.actionbarsherlock.app.SherlockFragment;
import com.tonicartos.component.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

public class DirFragment extends SherlockFragment implements LoaderCallbacks<Cursor> {
    private static final String ARG_COL_WIDTH = "arg_col_width";
    private static final String ARG_NUM_COLS = "arg_num_cols";
    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final int LOADER_DIR_LISTING = 0x01;

    public static DirFragment newInstance(int id, String name) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID, id);
        args.putString(ARG_NAME, name);
        DirFragment f = new DirFragment();
        f.setArguments(args);
        return f;
    }

    public static DirFragment newInstance(int id, String name, int numColumns, int columnWidth) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID, id);
        args.putString(ARG_NAME, name);
        if (numColumns != 0) {
            args.putInt(ARG_NUM_COLS, numColumns);
        }
        args.putInt(ARG_COL_WIDTH, columnWidth);
        DirFragment f = new DirFragment();
        f.setArguments(args);
        return f;
    }

    private File mFile;
    private StickyGridHeadersGridView mFilesGrid;
    private String mTitle;
    private SimpleCursorAdapter mDirListingAdapter;
    private int mId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (!args.containsKey(ARG_ID) || !args.containsKey(ARG_NAME)) {
            throw new IllegalStateException("Dir ID required.");
        }

        mId = args.getInt(ARG_ID);
        mTitle = args.getString(ARG_NAME);
        mDirListingAdapter = new MediaStoreFilesAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null, new String[] {
                    MediaStore.Files.FileColumns.TITLE
                }, new int[] {
                    android.R.id.text1
                }, 0);

        getLoaderManager().initLoader(LOADER_DIR_LISTING, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dir, container, false);
        mFilesGrid = (StickyGridHeadersGridView)v.findViewById(R.id.list_files);
        mFilesGrid.setAdapter(mDirListingAdapter);

        Bundle args = getArguments();
        if (args.containsKey(ARG_COL_WIDTH)) {
            mFilesGrid.setColumnWidth(args.getInt(ARG_COL_WIDTH));
        }
        if (args.containsKey(ARG_NUM_COLS)) {
            mFilesGrid.setNumColumns(args.getInt(ARG_NUM_COLS));
        }
        return v;
    }

    public void setColumnWidth(int width) {
        if (mFilesGrid != null) {
            mFilesGrid.setColumnWidth(width);
        }
        getArguments().putInt(ARG_COL_WIDTH, width);
    }

    public void setNumColumns(int numColumns) {
        if (mFilesGrid != null) {
            mFilesGrid.setNumColumns(numColumns);
        }
        getArguments().putInt(ARG_NUM_COLS, numColumns);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case LOADER_DIR_LISTING:
                return new CursorLoader(getActivity(), MediaStore.Files.getContentUri("external"),
                        new String[] {MediaStore.Files.FileColumns._COUNT}, MediaStore.Files.FileColumns.PARENT + "=" + mId, null, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> l, Cursor c) {
        DatabaseUtils.dumpCursor(c);
        switch (l.getId()) {
            case LOADER_DIR_LISTING:
                mDirListingAdapter.swapCursor(c);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> l) {
        switch (l.getId()) {
            case LOADER_DIR_LISTING:
                break;
        }
        mDirListingAdapter.swapCursor(null);
    }
}
