
package com.tonicartos.component.internal;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirPagerAdapter extends FragmentPagerAdapter {
    private int mColumnWidth;
    private int mNumColumns;
    private DirNode mRootNode;

    public DirPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addDir(int id, String name, String path) {
        if (mRootNode == null) {
            mRootNode = new DirNode(id, name, path.split(File.separator).length);
            notifyDataSetChanged();
        } else if (mRootNode.addPath(id, name, path.split(File.separator).length)) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        if (mRootNode == null) {
            return 0;
        }

        return mRootNode.mNumDescendants + 1;
    }

    @Override
    public DirFragment getItem(int i) {
        if (mRootNode == null) {
            return null;
        }

        return mRootNode.getNodeAtDepth(i).getFragment();
    }

    public void setColumnWidth(int width) {
        mColumnWidth = width;
        if (mRootNode != null) {
            mRootNode.updateColumnWidth(width);
        }
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
        if (mRootNode != null) {
            mRootNode.updateNumColumns(numColumns);
        }
    }

    public void setRootDir(int id, String name, String path) {
        Log.d("asdf", id + " " + name + " " + path);
        mRootNode = new DirNode(id, name, path.split(File.separator).length);
        notifyDataSetChanged();
    }

    protected class DirNode {
        public static final boolean DATA_SET_CHANGED = true;
        public static final boolean DATA_SET_UNCHANGED = false;
        /**
         * Item at position 0 is considered 'selected'.
         */
        private List<DirNode> mChildren;
        /**
         * File path depth.
         */
        private int mPathDepth;
        /**
         * Fragment this node represents.
         */
        private DirFragment mFragment;
        /**
         * Position in the history.
         */
        private int mNodeDepth;
        /**
         * Count of how many descendants are in the selected branch.
         */
        private int mNumDescendants;
        /**
         * This nodes parent.
         */
        private DirNode mParent;
        /**
         * The title of this directory.
         */
        private String mTitle;
        /**
         * Directory (MediaStore ID) this node represents.
         */
        private int mId;

        /**
         * Create a new DirNode with a parent.
         * 
         * @param parent Parent of new node.
         */
        public DirNode(DirNode parent, int id, String name) {
            this(id, name, parent.mPathDepth + 1);
            mNodeDepth = parent.getDepth() + 1;
            mParent = parent;
        }

        /**
         * Create a new root DirNode.
         */
        public DirNode(int id, String name, int pathDepth) {
            mTitle = name;
            mId = id;
            mFragment = DirFragment.newInstance(id, name, mNumColumns, mColumnWidth);
            mChildren = new ArrayList<DirNode>();
            mPathDepth = pathDepth;
        }

        /**
         * Get the fragment this node holds.
         * 
         * @return Fragment held by this node.
         */
        public DirFragment getFragment() {
            return mFragment;
        }

        public String getTitle() {
            return mTitle;
        }

        public void updateColumnWidth(int width) {
            mFragment.setColumnWidth(width);
            for (DirNode dn : mChildren) {
                dn.updateColumnWidth(width);
            }
        }

        public void updateNumColumns(int numColumns) {
            mFragment.setNumColumns(numColumns);
            for (DirNode dn : mChildren) {
                dn.updateNumColumns(numColumns);
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

        /**
         * Get the node depth. This is analogous to position in a list where the
         * set of nodes are only the members of the active history.
         * 
         * @return Node depth.
         */
        private int getDepth() {
            return mNodeDepth;
        }

        /**
         * Set the number of descendants in the active history this node has.
         * 
         * @param descendents The number of descendants in the active path
         *            history.
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
         * @param name
         * @param newPath Path to add/switch to.
         * @return True if the active data set has changed. False if no change
         *         (path was already in the selected history).
         */
        protected boolean addPath(int id, String name, int pathDepth) {
            int depthDelta = pathDepth - mPathDepth;
            if (depthDelta == 1) {
                for (int i = 0; i < mChildren.size(); i++) {
                    if (id == mChildren.get(i).mId) {
                        if (i == 0) {
                            // Already in the currently selected history and we
                            // don't need to changes the data set.
                            return DATA_SET_UNCHANGED;
                        }

                        DirNode selected = mChildren.remove(i);
                        addChild(selected);
                        return DATA_SET_CHANGED;
                    }
                }
                addChild(new DirNode(this, id, name));
                return DATA_SET_CHANGED;
            } else if (depthDelta > 1) {
                // Assume that only the active history will have paths added so
                // pass on down to the zeroth child.
                return mChildren.get(0).addPath(id, name, pathDepth);
            } else if (mParent == null) {
                throw new RuntimeException("Illegal attempt to add " + id + ": " + name
                        + " to parent node where no parent exists.");
            } else {
                return mParent.addPath(id, name, pathDepth);
            }
        }

        /**
         * Fetch the node at the given depth in the active history for this
         * node. This lets the pager page up and down the active path.
         * 
         * @param i Depth to select node at.
         * @return Node at requested depth.
         */
        protected DirNode getNodeAtDepth(int i) {
            if (i == mNodeDepth) {
                return this;
            } else if (i < mNodeDepth) {
                return mParent.getNodeAtDepth(i);
            }
            return mChildren.get(0).getNodeAtDepth(i);
        }

        /**
         * Get the number of descendants in the currently selected history for
         * this node.
         * 
         * @return Number of descendants.
         */
        protected int getNumDescendants() {
            return mNumDescendants;
        }
    }
}
