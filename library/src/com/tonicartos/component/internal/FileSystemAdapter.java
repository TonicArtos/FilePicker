
package com.tonicartos.component.internal;

import com.tonicartos.component.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;

import android.annotation.SuppressLint;
import android.os.FileObserver;
import android.support.v4.app.FragmentActivity;
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

public class FileSystemAdapter extends ArrayAdapter<String> implements StickyGridHeadersBaseAdapter {
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
    private DirObserver mFileObserverExtension;
    private FilenameFilter mFilter;
    private Map<String, HeaderData> mHeaderMap;
    private List<HeaderData> mHeaders;
    private LayoutInflater mInflater;

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
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1,
                new ArrayList<String>(Arrays.asList(file.list(filter))));

        mDir = file;
        mFilter = filter;

        mInflater = LayoutInflater.from(context);
        mContext = context;
        mCaseInsensitive = caseInsensitive;
        mHeaders = new ArrayList<HeaderData>();

        mFileObserverExtension = new DirObserver(file.getPath());
        mFileObserverExtension.startWatching();

        sort(mFileComparator);
        buildHeaders();
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
        if (hd == null) {
            return;
        }
        hd.count--;
        if (hd.count == 0) {
            mHeaders.remove(hd);
            mHeaderMap.remove(ext);
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
        String header = ExtensionMapper.getHeader(file);
        if (header == null) {
            header = "Unknown";
        }

        return header;
    }

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
            boolean added = false;
            for (int i = 0; i < mHeaders.size(); i++) {
                if (compareCis(ext, mHeaders.get(i).header) < 0) {
                    mHeaders.add(i, hd);
                    added = true;
                    break;
                }
            }
            if (!added) {
                mHeaders.add(hd);
            }
        }
        hd.count++;

    }

    /**
     * File observer to watch a directory and update adapter accordingly.
     */
    private final class DirObserver extends FileObserver {
        private DirObserver(String path) {
            super(path, CREATE | DELETE);
        }

        @Override
        public void onEvent(final int event, final String path) {
            if (!mFilter.accept(mDir, path)) {
                return;
            }
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (event & ALL_EVENTS) {
                        case CREATE:
                            incrementHeader(getFileSectionHeader(new File(mDir, path)));
                            add(path); // Notifies data set changed.
                            sort(mFileComparator);

                            break;
                        case DELETE:
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
}
