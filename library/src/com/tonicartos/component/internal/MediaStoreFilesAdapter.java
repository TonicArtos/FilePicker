
package com.tonicartos.component.internal;

import com.tonicartos.component.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaStoreFilesAdapter extends SimpleCursorAdapter implements
        StickyGridHeadersBaseAdapter {

    private Map<String, HeaderData> mHeaderMap;
    private List<HeaderData> mHeaders;
    private boolean mCaseInsensitive;
    private LayoutInflater mInflater;

    public MediaStoreFilesAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        this(context, layout, c, from, to, flags, false);
    }

    public MediaStoreFilesAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags, boolean caseInsensitive) {
        super(context, layout, c, from, to, flags);
        mCaseInsensitive = caseInsensitive;
        mHeaders = new ArrayList<HeaderData>();
        mInflater = LayoutInflater.from(context);
        if (c != null) {
            buildHeaders(c);
        }
    }

    /**
     * Build header data from initial load of files.
     */
    private void buildHeaders(Cursor c) {
        mHeaderMap = new HashMap<String, HeaderData>();

        if (!c.moveToFirst()) {
            return;
        }

        while (c.moveToNext()) {
            String header = makeHumanHeaders(c.getString(c
                    .getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)));
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

    private String makeHumanHeaders(String header) {
        if (header == null) {
            header = "folder";
        }

        // TODO: Tidy up MIME types into nice headers.
        return header;
    }

    @Override
    public int getCountForHeader(int header) {
        return mHeaders.get(header).count;
    }

    @Override
    public int getNumHeaders() {
        return mHeaders.size();
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

    /**
     * Increment the count for the file extension. Creates and adds new data for
     * a new MimeType maintaining the sorted order of the headers list.
     * 
     * @param ext File extension to update header data for.
     */
    protected void incrementHeader(String mimeType) {
        HeaderData hd = mHeaderMap.get(mimeType);
        if (hd == null) {
            hd = new HeaderData();
            hd.header = mimeType;
            boolean added = false;
            for (int i = 0; i < mHeaders.size(); i++) {
                if (compareCis(mimeType, mHeaders.get(i).header) < 0) {
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

    @Override
    public Cursor swapCursor(Cursor c) {
        if (c != null) {
            buildHeaders(c);
        }
        return super.swapCursor(c);
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
