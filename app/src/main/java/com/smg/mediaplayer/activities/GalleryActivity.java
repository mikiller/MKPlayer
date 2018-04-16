package com.smg.mediaplayer.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.smg.mediaplayer.R;
import com.smg.mediaplayer.base.BaseActivity;
import com.uilib.titlebar.TitleBar;
import com.uilib.mxgallery.adapters.DirRcvAdapter;
import com.uilib.mxgallery.defaultloaders.AlbumLoader;
import com.uilib.mxgallery.defaultloaders.MediaLoader;
import com.uilib.mxgallery.listeners.OnBottomBarListener;
import com.uilib.mxgallery.models.MimeType;
import com.uilib.mxgallery.models.ReportResModel;
import com.uilib.mxgallery.utils.GalleryMediaUtils;
import com.uilib.mxgallery.utils.GalleryLoaderUtils;
import com.uilib.mxgallery.widgets.MXGallery;
import com.uilib.utils.AnimUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Mikiller on 2017/5/18.
 */

public class GalleryActivity extends BaseActivity {
    @BindView(R.id.titleBar)
    TitleBar titleBar;
    @BindView(R.id.gallery)
    MXGallery gallery;
    @BindView(R.id.fl_pop)
    FrameLayout fl_pop;
    @BindView(R.id.rcv_ablumList)
    RecyclerView rcv_dirList;

    private boolean isMultiple;
    private int maxSelect, mediaType;
    private Bundle savedBundle;
    private List<String> selectedPath = new ArrayList<>();
    private DirRcvAdapter adapter;
    private GalleryMediaUtils gmUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        isMultiple = getIntent().getBooleanExtra(MXGallery.ISMULTIPLE, true);
        maxSelect = getIntent().getIntExtra(MXGallery.MAX_SELECT, 9);
        mediaType = getIntent().getIntExtra(MXGallery.MEDIA_TYPE, 0);
        savedBundle = savedInstanceState;
        super.onCreate(savedInstanceState);
        gmUtils = GalleryMediaUtils.getInstance(this);
        setContentView(R.layout.activity_gallery);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        gallery.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void initView() {
        titleBar.setTitleListener(new TitleBar.TitleListener() {
            @Override
            protected void onBackClicked() {
                back();
            }

            @Override
            public void onMenuChecked(boolean isChecked) {
                if (isChecked)
                    showDirList();
                else
                    hideDirList();
            }
        });
        titleBar.setMenu(getString(R.string.tab_all));
        gallery.setIsMultiple(isMultiple);
        gallery.setMimeType(mediaType == 0 ? MimeType.ofAll() : (mediaType == 1 ? MimeType.ofImage() : MimeType.ofVideo()));
        gallery.setMaxSelectionCount(maxSelect);
        gallery.onCreate(savedBundle);
        gallery.setSelectListener(new OnBottomBarListener() {
            @Override
            public void onPreView(boolean isPreview) {
            }

            @Override
            public void onConfirm(List<File> fileList) {
                onGetImgFiles(GalleryMediaUtils.THUMB_LIST, fileList);
            }
        });


        //gallery.setSelectedPaths(getSelectPath(true));
        rcv_dirList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new DirRcvAdapter(this);
        adapter.setListener(new DirRcvAdapter.onItemClickListener() {
            @Override
            public void onItemClicked(String bucketId, String albumName) {
                titleBar.callMenuCheck();
                titleBar.setMenu(albumName);
                gallery.setBucketId(bucketId);

            }
        });
        rcv_dirList.setAdapter(adapter);
    }

    private void showDirList() {
        GalleryLoaderUtils.initLoaderManager(this, AlbumLoader.LOADER_ID, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new AlbumLoader(GalleryActivity.this, gallery.getMimeType());
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                adapter.swapCursor(data);
                fl_pop.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fl_pop.setVisibility(View.VISIBLE);
                        AnimUtils.startObjectAnim(fl_pop, "translationY", -fl_pop.getMeasuredHeight(), 0, 300);
                    }
                }, 100);

            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                adapter.swapCursor(null);
            }
        });
    }

    private void hideDirList() {
        AnimUtils.startObjectAnim(fl_pop, "translationY", 0, -fl_pop.getMeasuredHeight(), 300, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (((float) animation.getAnimatedValue()) == -fl_pop.getMeasuredHeight()) {
                    adapter.swapCursor(null);
                    fl_pop.setVisibility(View.GONE);
                }
            }
        });
    }

    private void onGetImgFiles(String key, Object value) {
        //TODO:do something after get img files
        Intent intent = new Intent();
        intent.putExtra(key, (Serializable) value);
        setResult(RESULT_OK, intent);
        back();
    }

    private List<String> getSelectPath(boolean isPic) {
        for (ReportResModel file : gmUtils.getThumbList()) {
            if (selectedPath.contains(file.getResFile().getAbsolutePath()))
                continue;
            if (isPic) {
                if (MimeType.isPic(gmUtils.getFileMimeType(file.getResFile().getPath())))
                    selectedPath.add(file.getResFile().getAbsolutePath());
            } else {
                if (MimeType.isVideo(gmUtils.getFileMimeType(file.getResFile().getPath())))
                    selectedPath.add(file.getResFile().getAbsolutePath());
            }
        }
        return selectedPath;
    }

    @Override
    protected void initData() {
//        gallery.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                gallery.checkTab(0);
//            }
//        }, 100l);
    }

    /**
     * 来自于系统相机的返回
     * 直接将临时文件返回给调用相册的activity
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        onGetImgFiles(GalleryMediaUtils.TMP_FILE, gmUtils.tmpFile.getResFile());
    }

    @Override
    protected void onDestroy() {
        GalleryLoaderUtils.destoryLoaderManager(MediaLoader.LOADER_ID, AlbumLoader.LOADER_ID);
        super.onDestroy();
    }
}
