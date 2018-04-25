package com.smg.mediaplayer.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.mikiller.mkplayerlib.MKPlayer;
import com.mikiller.mkplayerlib.MKVideoView;
import com.smg.mediaplayer.R;
import com.smg.mediaplayer.base.BaseActivity;
import com.smg.mediaplayer.widgets.AndroidMediaController;
import com.uilib.mxgallery.utils.GalleryMediaUtils;
import com.uilib.mxgallery.widgets.MXGallery;
import com.uilib.utils.DisplayUtil;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class WelcomeActivity extends BaseActivity {

//    private ImageView iv_preview;
    private Button btn_gallery;
    @BindView(R.id.videoPlayer)
    MKPlayer video;
//    @BindView(R.id.table)
//    TableLayout tab;
    private AndroidMediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

    }

    @Override
    protected void initView() {
//        mediaController = new AndroidMediaController(this, true);
//        mediaController.setFullScreenListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleFullScreen();
//                video.toggleMediaControlsVisiblity();
//            }
//        });
//        IjkMediaPlayer.loadLibrariesOnce(null);
//        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        video.setMediaController(new AndroidMediaController(this, true));
//        video.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                video.toggleMediaControlsVisiblity();
//            }
//        });
        //video.setHudView(tab);


//        iv_preview = (ImageView) findViewById(R.id.iv_preview);
        btn_gallery = (Button) findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, GalleryActivity.class);
                intent.putExtra(MXGallery.MEDIA_TYPE, 2);
                startActivityForResult(intent, 1);
//                Intent intent = new Intent();
//                intent.setClassName("com.buyantech.interview","com.buyantech.interview.activity.AppStart");
//                startActivity(intent);
            }
        });
    }

    /**
     * 全屏切换，点击全屏按钮
     */
    private void toggleFullScreen() {
        if (DisplayUtil.getScreenOrientation(this) != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        video.configurationChanged(newConfig);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK)
            return;
        switch(requestCode){
            case 1:
                File tmpFile = (File) data.getSerializableExtra(GalleryMediaUtils.TMP_FILE);
                List<File> fileList = (List<File>) data.getSerializableExtra(GalleryMediaUtils.THUMB_LIST);
                if(tmpFile != null){
                    //GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(tmpFile),R.mipmap.placeholder, iv_preview);
                }else if(fileList != null && fileList.size() > 0){
                    //video.stopPlayback();
                    video.setVideoUri(GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)));
                    //video.start();
                }
//                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)),R.mipmap.placeholder, iv_preview);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        video.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        video.stopPlayback();

    }
}
