
package com.tonicartos.component.internal;

import com.actionbarsherlock.app.SherlockFragment;
import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.component.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FilenameFilter;

public class DirFragment extends SherlockFragment {
    private static final String ARG_COL_WIDTH = "arg_col_width";
    private static final String ARG_NUM_COLS = "arg_num_cols";
    private static final String ARG_PATH = "arg_id";

    public static DirFragment newInstance(File file) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PATH, file);
        DirFragment f = new DirFragment();
        f.setArguments(args);
        return f;
    }

    public static DirFragment newInstance(File file, int numColumns, int columnWidth) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PATH, file);
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
    private FileSystemAdapter mFsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (!args.containsKey(ARG_PATH)) {
            throw new IllegalStateException("Path required.");
        }

        mFile = (File)args.getSerializable(ARG_PATH);
        mTitle = mFile.getName();
        FilenameFilter filter = ((FilePickerFragment.Callbacks)getActivity()).getfilter();
        if (filter == null) {
            mFsAdapter = new FileSystemAdapter(getActivity(), mFile);
        } else {
            mFsAdapter = new FileSystemAdapter(getActivity(), mFile, filter);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dir, container, false);
        mFilesGrid = (StickyGridHeadersGridView)v.findViewById(R.id.list_files);
        mFilesGrid.setAdapter(mFsAdapter);

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
}
