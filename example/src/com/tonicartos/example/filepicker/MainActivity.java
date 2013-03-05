
package com.tonicartos.example.filepicker;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.tonicartos.component.FilePickerFragment;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;

import java.io.FilenameFilter;

public class MainActivity extends SherlockFragmentActivity implements FilePickerFragment.Callbacks {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        FilePickerFragment filePickerFragment = (FilePickerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        filePickerFragment.setRootDir(Environment.getExternalStorageDirectory());
        filePickerFragment.setColumnWidth((int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics()));
        filePickerFragment.setNumColumns(StickyGridHeadersGridView.AUTO_FIT);
    }

    @Override
    public FilenameFilter getfilter() {
        return null;
    }
}
