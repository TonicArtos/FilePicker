
package com.tonicartos.component;

import com.actionbarsherlock.app.SherlockFragment;
import com.tonicartos.component.internal.DirPagerAdapter;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.FilenameFilter;

public class FilePickerFragment extends SherlockFragment {
    private ViewPager mDirPager;
    private DirPagerAdapter mDirPagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_filepager, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mDirPager = (ViewPager)view.findViewById(R.id.container_directory);
        mDirPagerAdapter = new DirPagerAdapter(getChildFragmentManager());
        mDirPager.setAdapter(mDirPagerAdapter);
    }

    public void setRootDir(String path) {
        Log.d("asdf", path);
        // TODO: On background thread.
        int rootPathLength = path.length() + 2; // Include / on end
        Cursor c = getActivity().getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                null,
                "length(replace(substr(" + MediaStore.Files.FileColumns.DATA + "," + rootPathLength
                        + "), \"/\", \"\")) = length(substr(" + MediaStore.Files.FileColumns.DATA
                        + "," + rootPathLength + "))", null, null);
        DatabaseUtils.dumpCursor(c);
        if (!c.moveToFirst()) {
            throw new IllegalStateException(
                    "Root location for file browsing cannot be located in MediaStore.");
        }
        mDirPagerAdapter.setRootDir(c.getInt(c.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                c.getString(c.getColumnIndex(MediaStore.Files.FileColumns.TITLE)),
                c.getString(c.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
    }

    public void setColumnWidth(int width) {
        mDirPagerAdapter.setColumnWidth(width);

    }

    public void setNumColumns(int numCols) {
        mDirPagerAdapter.setNumColumns(numCols);
    }

    public interface Callbacks {
        FilenameFilter getfilter();
    }
}
