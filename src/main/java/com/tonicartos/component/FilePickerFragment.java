
package com.tonicartos.component;

import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.tonicartos.component.internal.DirPagerAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FilenameFilter;

public class FilePickerFragment extends Fragment implements OnPageChangeListener {
    public static final String ARG_DRAGDROP = "arg_dragdrop";
    public static final String ARG_MULTI_SELECT = "arg_multiselect";
    public static final String ARG_COLUMN_WIDTH = "arg_column_width";
    public static final String ARG_NUM_COLUMNS = "arg_num_columns";
    public static final String ARG_ROOT_PATH = "arg_root_path";
    public static final String ARG_ROOT_DIR = "arg_root_dir";
    
    private static final String ARG_CURRENT_PAGE = "arg_current_page";
    private static final String ARG_ROOT_TAB_NAME = "arg_root_tab_name";
    private static final String ARG_DIR_ADAPTER_STATE = "arg_dir_adapter_state";

    private ViewPager mDirPager;
    private DirPagerAdapter mDirPagerAdapter;
    public PagerSlidingTabStrip mIndicator;

    public void addDir(File file) {
        mDirPagerAdapter.addDir(file);
        mDirPager.setCurrentItem(mDirPager.getCurrentItem() + 1);
    }

    /**
     * Go back in the file picker history.
     * 
     * @return True if the back action was consumed. If False the caller should
     *         handle the back action.
     */
    public boolean goBack() {
        int position = mDirPager.getCurrentItem();
        if (position == 0) {
            return false;
        }
        mDirPager.setCurrentItem(position - 1);
        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement callbacks.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mDirPagerAdapter = new DirPagerAdapter(this, getChildFragmentManager(),
                getActivity().getString(R.string.root_tab_name));
        if (args == null) {
            return;
        }

        mDirPagerAdapter.setRootTabName(args.getString(ARG_ROOT_TAB_NAME));

        setDragDropEnabled(args.getBoolean(ARG_DRAGDROP, false));
        setMultiSelectEnabled(args.getBoolean(ARG_MULTI_SELECT, false));
        setNumColumns(args.getInt(ARG_NUM_COLUMNS, -1));
        if (args.containsKey(ARG_COLUMN_WIDTH)) {
            setColumnWidth(args.getInt(ARG_COLUMN_WIDTH));
        }

        if (savedInstanceState == null) {
            if (args.containsKey(ARG_ROOT_DIR)) {
                setRootDir((File)args.getSerializable(ARG_ROOT_DIR));
            }
            if (args.containsKey(ARG_ROOT_PATH)) {
                setRootPath(args.getString(ARG_ROOT_PATH));
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_filepager, container, false);
        mDirPager = (ViewPager)v.findViewById(R.id.pager);
        mDirPager.setAdapter(mDirPagerAdapter);

        mIndicator = (PagerSlidingTabStrip)v.findViewById(R.id.indicator);
        mIndicator.setViewPager(mDirPager);
        mIndicator.setOnPageChangeListener(this);

        return v;
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageSelected(int position) {
        mDirPagerAdapter.addDir(mDirPagerAdapter.getNode(position).getFile());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(ARG_DIR_ADAPTER_STATE, mDirPagerAdapter.getState());
        outState.putInt(ARG_CURRENT_PAGE, mDirPager.getCurrentItem());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mDirPagerAdapter.loadState(savedInstanceState.getBundle(ARG_DIR_ADAPTER_STATE));
            mDirPager.setCurrentItem(savedInstanceState.getInt(ARG_CURRENT_PAGE));
        }
    }

    public void setColumnWidth(int width) {
        mDirPagerAdapter.setColumnWidth(width);
    }

    public void setDragDropEnabled(boolean enabled) {
        mDirPagerAdapter.setDragDropEnabled(enabled);
    }

    public void setMultiSelectEnabled(boolean enabled) {
        mDirPagerAdapter.setMultiSelectEnabled(enabled);
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
        /**
         * Get a filename filter for the filepicker. If returned filter is null
         * then a default filter will be used.
         * 
         * @return FilenameFilter.
         */
        FilenameFilter getfilter();

        /**
         * Get a mapper that converts mime type strings into headers for the
         * file sections. If return mapper is null then the detected mime types
         * will be display.
         * 
         * @return A header mapper.
         */
        HeaderMapper getHeaderMapper();

        void onFilesPicked(File... file);
    }

    public interface HeaderMapper {
        String getHeaderFor(String mimeType, File file);
    }
}
