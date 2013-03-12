
package com.tonicartos.component.internal;

import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.component.R;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

public class DirPagerAdapter extends PagerAdapter {
    protected static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    private int mColumnWidth;
    private Fragment mCurrentPrimaryItem;
    private FragmentTransaction mCurTransaction = null;
    // private List<DirFragment> mDirFragments;
    private boolean mDragDropEnabled;
    private FragmentManager mFragmentManager;
    private int mMaxFragmentsSeen;
    private int mNumColumns;
    private DirNode mRootNode;

    private boolean mSelectEnabled;
    protected FilePickerFragment mController;

    public DirPagerAdapter(FilePickerFragment fragment, FragmentManager fm) {
        mFragmentManager = fm;
        mController = fragment;
//        mDirFragments = new ArrayList<DirFragment>();
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
        } else if (mRootNode.addPath(dir)) {
            notifyDataSetChanged();
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

    //
    // public List<DirFragment> getFragments() {
    // return mDirFragments;
    // }

    public DirFragment getItem(int i) {
        if (mRootNode == null) {
            return null;
        }

        return mRootNode.getNodeAtDepth(i).getFragment();
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public DirNode getParcelableData() {
        return mRootNode;
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

    public void loadSavedData(DirNode data, int numFragments) {
        mRootNode = data;
        mRootNode.setPagerAdapter(this);
        mRootNode.setFragmentManager(mFragmentManager);
        mMaxFragmentsSeen = numFragments;

        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.container_directory, i));
            f.addController(mController);
            // Log.d("asdf", "rebuild item " + i + " " + mDirFragments.size());
            // mDirFragments.add(f);
        }

        notifyDataSetChanged();
    }

    /**
     * Set the width of the columns displayed in the file picker.
     * 
     * @param width New width of the columns.
     */
    public void setColumnWidth(int width) {
        mColumnWidth = width;
        // for (DirFragment fragment : getFragments()) {
        // fragment.setColumnWidth(width);
        // }

        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.container_directory, i));
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

    // public void setFragments(List<DirFragment> dirFragments) {
    // mDirFragments = dirFragments;
    // }

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
        // for (DirFragment fragment : getFragments()) {
        // fragment.setNumColumns(numColumns);
        // }
        for (int i = 0; i < mMaxFragmentsSeen; i++) {
            DirFragment f = (DirFragment)mFragmentManager.findFragmentByTag(makeFragmentName(
                    R.id.container_directory, i));
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
    }

    @Override
    public void startUpdate(View container) {
    }

    public int getMaxFragmentsSeen() {
        return mMaxFragmentsSeen;
    }
}
