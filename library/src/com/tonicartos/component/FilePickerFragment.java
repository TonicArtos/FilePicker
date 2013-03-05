
package com.tonicartos.component;

import com.actionbarsherlock.app.SherlockFragment;
import com.tonicartos.component.internal.DirPagerAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
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

    public void setColumnWidth(int width) {
        mDirPagerAdapter.setColumnWidth(width);

    }

    public void setNumColumns(int numCols) {
        mDirPagerAdapter.setNumColumns(numCols);
    }

    public void setRootDir(File file) {
        mDirPagerAdapter.setRootDir(file);
    }

    public void setRootPath(String path) {
        setRootDir(new File(path));
    }

    public interface Callbacks {
        FilenameFilter getfilter();
    }
}
