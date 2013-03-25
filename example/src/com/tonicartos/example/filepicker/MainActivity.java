
package com.tonicartos.example.filepicker;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.component.FilePickerFragment.HeaderMapper;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends SherlockFragmentActivity implements FilePickerFragment.Callbacks {
    private Toast mCurrentToast;
    private FilePickerFragment mFilePickerFragment;

    @Override
    public FilenameFilter getfilter() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);

        mFilePickerFragment = (FilePickerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        if (savedInstanceState == null) {
            mFilePickerFragment.setRootDir(Environment.getExternalStorageDirectory());
            mFilePickerFragment.setColumnWidth((int)TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics()));
            mFilePickerFragment.setNumColumns(StickyGridHeadersGridView.AUTO_FIT);
            mFilePickerFragment.setMultiSelectEnabled(true);
        }
    }

    @Override
    public HeaderMapper getHeaderMapper() {
        return new HeaderMapper() {
            private Pattern mDocumentPattern = Pattern.compile("text/.*|application/.*");
            private Pattern mAudioPattern = Pattern.compile("audio/.*|application/ogg");
            private Pattern mVideoPattern = Pattern.compile("video/.*");
            private Pattern mImagePattern = Pattern.compile("image/.*");

            @Override
            public String getHeaderFor(String mimeType, File file) {
                Matcher matcher = mAudioPattern.matcher(mimeType);
                if (matcher.matches()) {
                    return getResources().getString(R.string.header_audio);
                }
                matcher = mDocumentPattern.matcher(mimeType);
                if (matcher.matches() || mimeType.endsWith("Unknown")) {
                    return getResources().getString(R.string.header_documents);
                }
                matcher = mVideoPattern.matcher(mimeType);
                if (matcher.matches()) {
                    return getResources().getString(R.string.header_video);
                }
                matcher = mImagePattern.matcher(mimeType);
                if (matcher.matches()) {
                    return getResources().getString(R.string.header_image);
                }
                return mimeType;
            }
        };
    }
    
    @Override
    public void onBackPressed() {
        if (!mFilePickerFragment.goBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onFilesPicked(File... files) {
        String fileNames = null;
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (fileNames == null) {
                fileNames = name;
            } else {
                fileNames += ", " + name;
            }
        }
        int stringId;
        if (files.length > 1) {
            stringId = R.string.fmt_files_picked;
        } else {
            stringId = R.string.fmt_file_picked;
        }

        if (mCurrentToast == null) {
            mCurrentToast = Toast.makeText(this, getResources().getString(stringId, fileNames),
                    Toast.LENGTH_SHORT);
        } else {
            mCurrentToast.setText(getResources().getString(stringId, fileNames));
        }
        mCurrentToast.show();
    }
}
