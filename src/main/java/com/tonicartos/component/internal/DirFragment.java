
package com.tonicartos.component.internal;

import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.component.R;
import com.tonicartos.component.FilePickerFragment.Callbacks;
import com.tonicartos.component.FilePickerFragment.HeaderMapper;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.io.FilenameFilter;

public class DirFragment extends Fragment implements OnItemClickListener {
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

    public static DirFragment newInstance(File file, int numColumns, int columnWidth, int tag) {
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

    private FilePickerFragment mController;
    private File mFile;
    private StickyGridHeadersGridView mFilesGrid;
    private FileSystemAdapter mFsAdapter;
    private String mTitle;
    private View mRootView;

    public void addController(FilePickerFragment controller) {
        mController = controller;
    }

    public String getTitle() {
        return mTitle;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (!args.containsKey(ARG_PATH)) {
            throw new IllegalStateException("Path required.");
        }

        if (savedInstanceState == null) {
            mFile = (File)args.getSerializable(ARG_PATH);
        } else {
            mFile = (File)savedInstanceState.getSerializable(ARG_PATH);
        }
        mTitle = mFile.getName();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_dir, container, false);
        mFilesGrid = (StickyGridHeadersGridView)mRootView.findViewById(R.id.list_files);
        initAdapter();
        mFilesGrid.setAdapter(mFsAdapter);
//        mFilesGrid.setEmptyView(v.findViewById(android.R.id.empty));

        Bundle args = getArguments();
        if (args.containsKey(ARG_COL_WIDTH)) {
            mFilesGrid.setColumnWidth(args.getInt(ARG_COL_WIDTH));
        }
        if (args.containsKey(ARG_NUM_COLS)) {
            mFilesGrid.setNumColumns(args.getInt(ARG_NUM_COLS));
        }

        mFilesGrid.setOnItemClickListener(this);
        return mRootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File file = mFsAdapter.getFile(position);
        if (file.isDirectory()) {
            mController.addDir(file);
            return;
        }

        ((Callbacks)getActivity()).onFilesPicked(file);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_PATH, mFile);
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

    public boolean update(File file) {
        if (mFile == null || mFilesGrid == null || mRootView == null || TextUtils.equals(file.getPath(), mFile.getPath())) {
            return false;
        }

        mFile = file;
        mTitle = mFile.getName();
        
        mRootView.findViewById(android.R.id.empty).setVisibility(View.GONE);

        initAdapter();
        mFilesGrid.setAdapter(mFsAdapter);
        return true;
    }

    private void initAdapter() {
        FilenameFilter filter = ((FilePickerFragment.Callbacks)getActivity()).getfilter();
        HeaderMapper headerMapper = ((FilePickerFragment.Callbacks)getActivity()).getHeaderMapper();

        if (mFsAdapter != null) {
            mFsAdapter.stopWatching();
        }
        
        if (filter == null) {
            mFsAdapter = new FileSystemAdapter(getActivity(), mFile, headerMapper, mRootView);
        } else {
            mFsAdapter = new FileSystemAdapter(getActivity(), mFile, headerMapper, mRootView, filter);
        }
    }
}
