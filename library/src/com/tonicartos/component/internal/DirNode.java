
package com.tonicartos.component.internal;

import com.tonicartos.component.R;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirNode implements Parcelable {
    public static final Parcelable.Creator<DirNode> CREATOR = new Creator<DirNode>() {
        @Override
        public DirNode createFromParcel(Parcel source) {
            return new DirNode(source);
        }

        @Override
        public DirNode[] newArray(int size) {
            return new DirNode[size];
        }
    };

    public static final boolean DATA_SET_CHANGED = true;
    public static final boolean DATA_SET_UNCHANGED = false;
    /**
     * Item at position 0 is considered 'selected'.
     */
    private List<DirNode> mChildren;
    /**
     * Path that this node represents.
     */
    private File mFile;
    /**
     * File path depth.
     */
    private int mFileDepth;
    /**
     * Position in the history.
     */
    private int mNodeDepth;
    private DirPagerAdapter mPagerAdapter;
    /**
     * This nodes parent.
     */
    private DirNode mParent;
    /**
     * The title of this directory.
     */
    private String mTitle;

    /**
     * Count of how many descendants are in the selected branch.
     */
    int mNumDescendants;

    private FragmentManager mFragmentManager;

    /**
     * Create a new DirNode with a parent.
     * 
     * @param parent Parent of new node.
     * @param dirPagerAdapter TODO
     */
    public DirNode(DirNode parent, File file, DirPagerAdapter adapter, FragmentManager fm) {
        this(file, adapter, fm);
        mNodeDepth = parent.getDepth() + 1;
        mParent = parent;
    }

    /**
     * Create a dir node from parcel data. The node will need to be initialised
     * with the pager adapter if created in this manner.
     * 
     * @param source Parcel data to create this node from.
     * @param parent A node that is this one's parent.
     */
    public DirNode(DirNode parent, Parcel source) {
        this(source);
        mParent = parent;
    }

    /**
     * Create a new root DirNode.
     */
    public DirNode(File file, DirPagerAdapter adapter, FragmentManager fm) {
        mTitle = file.getName();
        mFile = file;
        mFragmentManager = fm;
        mChildren = new ArrayList<DirNode>();
        mFileDepth = file.getPath().split(File.separator).length;
        mPagerAdapter = adapter;
    }

    /**
     * Create a dir node from parcel data. The node will need to be initialised
     * with the pager adapter if created in this manner.
     * 
     * @param source Parcel data to create this node from.
     */
    public DirNode(Parcel s) {
        mFile = new File(s.readString());
        mFileDepth = s.readInt();
        mNodeDepth = s.readInt();
        mNumDescendants = s.readInt();
        mTitle = mFile.getName();

        int numChildren = s.readInt();
        mChildren = new ArrayList<DirNode>();
        for (int i = 0; i < numChildren; i++) {
            mChildren.add(new DirNode(this, s));
        }
    }

    // }

    /**
     * Get the fragment this node holds.
     * 
     * @return Fragment held by this node.
     */
    public DirFragment getFragment() {
        String name = DirPagerAdapter.makeFragmentName(R.id.pager, mNodeDepth);
        DirFragment fragment = (DirFragment)mFragmentManager.findFragmentByTag(name);

        if (fragment == null) {
            // Add a fragment to the list of fragments if we need a new one.
            fragment = DirFragment.newInstance(mFile, mPagerAdapter.getNumColumns(),
                    mPagerAdapter.getColumnWidth(), mNodeDepth);
            fragment.addController(mPagerAdapter.mController);
            // mPagerAdapter.getFragments().add(fragment);
        } else {
            fragment.update(mFile);
        }
        return fragment;
    }

    /**
     * Get the title for the node.
     * 
     * @return Title.
     */
    public String getTitle() {
        return mTitle;
    }

    public void setPagerAdapter(DirPagerAdapter adapter) {
        mPagerAdapter = adapter;
        for (DirNode dn : mChildren) {
            dn.setPagerAdapter(adapter);
        }
    }

    @Override
    public String toString() {
        return mFile.getPath() + " @node depth: " + mNodeDepth + " open children: "
                + mChildren.size();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFile.getPath());
        dest.writeInt(mFileDepth);
        dest.writeInt(mNodeDepth);
        dest.writeInt(mNumDescendants);
        dest.writeInt(mChildren.size());
        for (DirNode dn : mChildren) {
            dn.writeToParcel(dest, flags);
        }
    }

    /**
     * Add to the descendants of this node.
     * 
     * @param newChild Node to add.
     */
    private void addChild(DirNode newChild) {
        mChildren.add(0, newChild);
        setNumDescendants(newChild.getNumDescendants() + 1);
    }

    private void addOrUpdateFragments() {
        // Get fragment implements the desired behaviour to update this
        // fragment.
        getFragment();
        if (mChildren.size() > 0) {
            // Propagate updates down descendants.
            mChildren.get(0).addOrUpdateFragments();
        }
    }

    /**
     * Get the node depth. This is analogous to position in a list where the set
     * of nodes are only the members of the active history.
     * 
     * @return Node depth.
     */
    protected int getDepth() {
        return mNodeDepth;
    }

    /**
     * Set the number of descendants in the active history this node has.
     * 
     * @param descendents The number of descendants in the active path history.
     */
    private void setNumDescendants(int descendents) {
        mNumDescendants = descendents;
        if (mParent != null) {
            mParent.setNumDescendants(descendents + 1);
        }
    }

    /**
     * Add a file to the history and/or switch to the history for the file.
     * 
     * @param newPath Path to add/switch to.
     * @return True if the active data set has changed. False if no change (path
     *         was already in the selected history).
     */
    public Pair<Boolean, DirNode> addPath(File file) {
        int depthDelta = file.getPath().split(File.separator).length - mFileDepth;
        if (depthDelta == 1) {
            for (int i = 0; i < mChildren.size(); i++) {
                if (file.compareTo(mChildren.get(i).mFile) == 0) {
                    if (i == 0) {
                        // Already in the currently selected history and we
                        // don't need to changes the data set.
                        return new Pair<Boolean, DirNode>(DATA_SET_UNCHANGED, mChildren.get(i));
                    }

                    DirNode selected = mChildren.remove(i);
                    addChild(selected);
                    selected.addOrUpdateFragments();
                    return new Pair<Boolean, DirNode>(DATA_SET_CHANGED, selected);
                }
            }
            DirNode newChild = new DirNode(this, file, mPagerAdapter, mFragmentManager);
            addChild(newChild);
            newChild.addOrUpdateFragments();
            return new Pair<Boolean, DirNode>(DATA_SET_CHANGED, newChild);
        } else if (depthDelta > 1) {
            // Assume that only the active history will have paths added so
            // pass on down to the zeroth child.
            return mChildren.get(0).addPath(file);
        } else if (TextUtils.equals(mFile.getPath(), file.getPath())) {
            return new Pair<Boolean, DirNode>(DATA_SET_UNCHANGED, this);
        } else if (mParent == null) {
            throw new RuntimeException("Illegal attempt to add " + file.getPath()
                    + " to parent node where no parent exists.");
        } else {
            return mParent.addPath(file);
        }
    }

    /**
     * Fetch the node at the given depth in the active history for this node.
     * 
     * @param i Depth to select node at.
     * @return Node at requested depth.
     */
    public DirNode getNodeAtDepth(int i) {
        if (i == mNodeDepth) {
            return this;
        } else if (i < mNodeDepth) {
            return mParent.getNodeAtDepth(i);
        }
        return mChildren.get(0).getNodeAtDepth(i);
    }

    /**
     * Get the number of descendants in the currently selected history for this
     * node.
     * 
     * @return Number of descendants.
     */
    protected int getNumDescendants() {
        return mNumDescendants;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
        for (DirNode dn : mChildren) {
            dn.setFragmentManager(fragmentManager);
        }
    }

    public File getFile() {
        return mFile;
    }
}
