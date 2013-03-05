
package com.tonicartos.component.internal;

import com.tonicartos.component.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystemAdapter extends ArrayAdapter<String> implements
        StickyGridHeadersBaseAdapter, LoaderCallbacks<Cursor> {
    private static final int LOADER_DIR = 0x01;
    private static final int LOADER_DIR_CONTENTS = 0x02;
    private static final String ARG_DIRPATH = "arg_filepath";
    private static final String ARG_DIR_ID = "arg_dir_id";

    private static FilenameFilter sDefaultFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir, filename);
            if (file.isHidden()) {
                return false;
            }
            if (file.isDirectory()) {
                return true;
            }
            return true;
        }
    };

    private boolean mCaseInsensitive;
    /**
     * So the file observer events can update on the UI thread.
     */
    private FragmentActivity mContext;
    private File mDir;
    private FileComparator mFileComparator = new FileComparator();
    private Cursor mFileMetadata;
    private DirObserver mFileObserverExtension;
    private FilenameFilter mFilter;
    private Map<String, HeaderData> mHeaderMap;
    private List<HeaderData> mHeaders;

    private LayoutInflater mInflater;
    private long mDirId;

    public FileSystemAdapter(FragmentActivity context, File file) {
        this(context, file, sDefaultFilter, false);
    }

    public FileSystemAdapter(FragmentActivity context, File file, boolean caseInsensitive) {
        this(context, file, sDefaultFilter, caseInsensitive);
    }

    public FileSystemAdapter(FragmentActivity context, File file, FilenameFilter filter) {
        this(context, file, filter, false);
    }

    public FileSystemAdapter(FragmentActivity context, File file, FilenameFilter filter,
            boolean caseInsensitive) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1);

        mDir = file;

        mFilter = filter;
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mCaseInsensitive = caseInsensitive;
        mHeaders = new ArrayList<HeaderData>();

        mFileObserverExtension = new DirObserver(file.getPath());
        mFileObserverExtension.startWatching();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_DIRPATH, file.getPath());
        mContext.getSupportLoaderManager().initLoader(LOADER_DIR, bundle, this);
    }

    @Override
    public int getCountForHeader(int header) {
        return mHeaders.get(header).count;
    }

    @Override
    @SuppressLint("DefaultLocale")
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.header, parent, false);
            holder = new ViewHolder();
            holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.textView.setText(mHeaders.get(position).header.toUpperCase());

        return convertView;
    }

    @Override
    public int getNumHeaders() {
        return mHeaders.size();
    }

    /**
     * Build header data from initial load of files.
     */
    private void buildHeaders() {
        mHeaderMap = new HashMap<String, HeaderData>();
        if (mFileMetadata == null) {
            return;
        }
        for (int i = 0; i < getCount(); i++) {
            File file = new File(mDir, getItem(i));
            String header = getFileSectionHeader(file);
            HeaderData hd = mHeaderMap.get(header);
            if (hd == null) {
                hd = new HeaderData();
                hd.header = header;
                hd.count = 0;

                mHeaders.add(hd);
            }
            hd.count += 1;
            mHeaderMap.put(header, hd);
        }
    }

    /**
     * Compare two strings. Checks adapter settings to select a case sensitive
     * or insensitive comparison.
     * 
     * @param lhs Left hand side.
     * @param rhs Right hand side.
     * @return -ve if lhs before rhs, 0 if the same. +ve if lhs after rhs.
     */
    protected int compareCis(String lhs, String rhs) {
        if (mCaseInsensitive) {
            return lhs.compareToIgnoreCase(rhs);
        }
        return lhs.compareTo(rhs);
    }

    /**
     * Decrement the count for the file extension. Removes data for an extension
     * should the count reach 0.
     * 
     * @param ext File extension to update header data for.
     */
    protected void decrementHeader(String ext) {
        HeaderData hd = mHeaderMap.get(ext);
        hd.count--;
        if (hd.count == 0) {
            mHeaders.remove(hd);
        }
    }

    /**
     * Get file extension for given file.
     * 
     * @param file File to get extension for.
     * @return File extension.
     */
    protected String getFileSectionHeader(File file) {
        if (file.isDirectory()) {
            return "Folder";
        }
        if (!mFileMetadata.moveToFirst()) {
            return "Error";
        }
        String header = null;
        if (TextUtils.equals(mFileMetadata.getString(mFileMetadata
                .getColumnIndex(MediaStore.Files.FileColumns.TITLE)), file.getName())) {
            header = mFileMetadata.getString(mFileMetadata
                    .getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
        } else {
            while (mFileMetadata.moveToNext()) {
                if (TextUtils.equals(mFileMetadata.getString(mFileMetadata
                        .getColumnIndex(MediaStore.Files.FileColumns.TITLE)), file.getName())) {
                    header = mFileMetadata.getString(mFileMetadata
                            .getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
                }
            }
        }

        if (header == null) {
            header = "Unknown";
        }

        return header;
    }

    // private final class InitialFilterWrapper implements FilenameFilter {
    // private FilenameFilter mFilter;
    //
    // public InitialFilterWrapper(FilenameFilter filter) {
    // mFilter = filter;
    // }
    //
    // @Override
    // public boolean accept(File dir, String filename) {
    // File f = new File(dir, filename);
    // if (!f.isDirectory()) {
    // return false;
    // }
    // return mFilter.accept(dir, filename);
    // }
    // }

    /**
     * Increment the count for the file extension. Creates and adds new data for
     * a new extension maintaining the sorted order of the headers list.
     * 
     * @param ext File extension to update header data for.
     */
    protected void incrementHeader(String ext) {
        HeaderData hd = mHeaderMap.get(ext);
        if (hd == null) {
            hd = new HeaderData();
            hd.header = ext;
            mHeaders.add(hd);
            for (int i = 0; i < mHeaders.size(); i++) {
                if (compareCis(ext, mHeaders.get(i).header) < 0) {
                    mHeaders.add(i, hd);
                    break;
                }
            }
        }
        hd.count++;

    }

    /**
     * File observer to watch a directory and update adapter accordingly.
     */
    private final class DirObserver extends FileObserver {
        private DirObserver(String path) {
            super(path, ALL_EVENTS);
        }

        @Override
        public void onEvent(final int event, final String path) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (event & ALL_EVENTS) {
                        case FileObserver.CREATE:
                        case FileObserver.MOVED_TO:
                            incrementHeader(getFileSectionHeader(new File(mDir, path)));
                            add(path); // Notifies data set changed.
                            sort(mFileComparator);
                            break;
                        case FileObserver.DELETE:
                        case FileObserver.MOVED_FROM:
                            decrementHeader(getFileSectionHeader(new File(mDir, path)));
                            remove(path); // Notifies data set changed.
                            sort(mFileComparator);
                            break;

                        default:
                            break;
                    }

                }
            });
        }
    }

    /**
     * Compare two files sorting directories first, then by file extension, and
     * finally by name alphabetically.
     */
    private final class FileComparator implements Comparator<String> {
        @Override
        public int compare(String lhsPath, String rhsPath) {
            File lhs = new File(mDir, lhsPath), rhs = new File(mDir, rhsPath);
            if (lhs.isDirectory() && rhs.isDirectory()) {
                return compareCis(lhs.getName(), rhs.getName());
            }

            if (lhs.isDirectory()) {
                return -1;
            }

            if (rhs.isDirectory()) {
                return 1;
            }
            String lhsExtension = getFileSectionHeader(lhs);
            String rhsExtension = getFileSectionHeader(rhs);

            // Compare extensions, if they are the same then compare by name.
            int extResult = compareCis(lhsExtension, rhsExtension);
            return extResult == 0 ? compareCis(lhs.getName(), rhs.getName()) : extResult;
        }

    }

    /**
     * Data for a section header.
     */
    private final class HeaderData {
        public int count;
        public String header;
    }

    /**
     * View holder for header views.
     */
    private final class ViewHolder {
        public TextView textView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case LOADER_DIR:
                return new CursorLoader(mContext, MediaStore.Files.getContentUri("external"), null,
                        MediaStore.Files.FileColumns.DATA + "=" + bundle.getString(ARG_DIRPATH),
                        null, null);
            case LOADER_DIR_CONTENTS:
            default:
                return new CursorLoader(mContext, MediaStore.Files.getContentUri("external"), null,
                        MediaStore.Files.FileColumns.PARENT + "=" + bundle.getInt(ARG_DIR_ID),
                        null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> l, Cursor c) {
        switch (l.getId()) {
            case LOADER_DIR:
                mDirId = getDirMediaStoreId(c); 
                break;
            case LOADER_DIR_CONTENTS:
                mFileMetadata = c;
                // Got MediaStore metadata so load the actual directory listing.
                addAll(new ArrayList<String>(Arrays.asList(mDir.list(mFilter))));
                sort(mFileComparator);
                buildHeaders();
                break;
        }
    }

    private long getDirMediaStoreId(Cursor c) {
        if (!c.moveToFirst()) {
            return -1;
        }
        
        return c.getLong(c.getColumnIndex(MediaStore.Files.FileColumns._ID));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mFileMetadata = null;
        clear();
        buildHeaders();
    }
}
