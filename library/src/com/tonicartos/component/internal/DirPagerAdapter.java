
package com.tonicartos.component.internal;

import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.component.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

public class DirPagerAdapter extends PagerAdapter {
    private static final String ARG_NUM_FRAGMENTS = "arg_num_fragments";
    private static final String ARG_DIR_DATA = "arg_dir_data";
    private static final String ARG_NUM_COLUMNS = "arg_num_columns";
    private static final String ARG_COLUMN_WIDTH = "arg_column_width";

    protected static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    private DirNode mCurrentNode;
    private DirNode mRootNode;

    private Fragment mCurrentPrimaryItem;
    private FragmentTransaction mCurTransaction = null;

    private boolean mDragDropEnabled;
    private boolean mSelectEnabled;

    private FragmentManager mFragmentManager;

    private int mMaxFragmentsSeen;

    private int mColumnWidth;
    private int mNumColumns;

    protected FilePickerFragment mController;
    private String mRootTabName = "Disk";

    public DirPagerAdapter(FilePickerFragment fragment, FragmentManager fm, String rootTabName) {
        mFragmentManager = fm;
        mController = fragment;
        mRootTabName = rootTabName;
    }

    /**
     * Add a new directory to the pager. It will be inserted in the appropriate
     * part of the directory hierarchy.
     * 
     * @param dir Directory to be added.
     */
    public void addDir(File dir) {
        if (mRootNode == null) {
            mRootNode = new DirNode(dir, this, mFragmentManager);
            notifyDataSetChanged();
            mCurrentNode = mRootNode;
        } else {
            Pair<Boolean, DirNode> result = mRootNode.addPath(dir);
            if (result.first) {
                notifyDataSetChanged();
            }

            if (mCurrentNode == result.second) {
                // Don't double add node to history. Required because view
                // paging triggers a double call here.
                return;
            }

            mCurrentNode = result.second;
        }
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        mCurTransaction.detach((Fragment)object);
    }

    @Override
    public void finishUpdate(View container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    public int getColumnWidth() {
        return mColumnWidth;
    }

    @Override
    public int getCount() {
        if (mRootNode == null) {
            return 0;
        }

        return mRootNode.mNumDescendants + 1;
    }

    public DirFragment getItem(int i) {
        if (mRootNode == null) {
            return null;
        }

        return mRootNode.getNodeAtDepth(i).getFragment();
    }

    public DirNode getNode(int position) {
        return mRootNode.getNodeAtDepth(position);
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return mRootTabName;
        }
        return getItem(position).getTitle();
    }

    public Bundle getState() {
        Bundle out = new Bundle();
        out.putParcelable(ARG_DIR_DATA, mRootNode);
        out.putInt(ARG_NUM_FRAGMENTS, mMaxFragmentsSeen);
        out.putInt(ARG_NUM_COLUMNS, mNumColumns);
        out.putInt(ARG_COLUMN_WIDTH, mColumnWidth);
        return out;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mMaxFragmentsSeen < position + 1) {
            mMaxFragmentsSeen = position + 1;
        }
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        // Do we already have this fragment?
        String name = makeFragmentName(container.getId(), position);
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            mCurTransaction.attach(fragment);
        } else {
            fragment = getItem(position);
            mCurTransaction.add(container.getId(), fragment,
                    makeFragmentName(container.getId(), position));
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
        }

        return fragment;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    public void loadState(Bundle in) {
        mRootNode = in.getParcelable(ARG_DIR_DATA);
        mRootNode.setPagerAdapter(this);
        mRootNode.setFragmentManager(mFragmentManager);

        mMaxFragmentsSeen = in.getInt(ARG_NUM_FRAGMENTS);

        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.pager, i));
            f.addController(mController);
        }

        mNumColumns = in.getInt(ARG_NUM_COLUMNS);
        mColumnWidth = in.getInt(ARG_COLUMN_WIDTH);

        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mController.mIndicator.notifyDataSetChanged();
    }

    /**
     * Set the width of the columns displayed in the file picker.
     * 
     * @param width New width of the columns.
     */
    public void setColumnWidth(int width) {
        mColumnWidth = width;

        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.pager, i));
            f.setColumnWidth(width);
        }
    }

    /**
     * Set whether the file picker allows files to be dragged from the file
     * picker fragment and dropped elsewhere.
     * 
     * @param enabled True if enabled.
     */
    public void setDragDropEnabled(boolean enabled) {
        mDragDropEnabled = enabled;
    }

    /**
     * set whether the file picker allows multiple files to be selected and
     * picked.
     * 
     * @param enabled True if enabled.
     */
    public void setMultiSelectEnabled(boolean enabled) {
        mSelectEnabled = enabled;
    }

    /**
     * Set the number of columns displayed in file picker.
     * 
     * @param numColumns New number of columns to display.
     */
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;

        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.pager, i));
            f.setNumColumns(numColumns);
        }
    }

    @Override
    public void setPrimaryItem(View container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    /**
     * Set the directory to start file picking from.
     * 
     * @param file Directory to start from.
     */
    public void setRootDir(File file) {
        mRootNode = new DirNode(file, this, mFragmentManager);
        notifyDataSetChanged();
        mCurrentNode = mRootNode;
    }

    public void setRootTabName(String rootTabName) {
        mRootTabName = rootTabName;
    }
}
