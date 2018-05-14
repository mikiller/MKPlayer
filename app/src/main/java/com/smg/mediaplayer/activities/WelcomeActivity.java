package com.smg.mediaplayer.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.mikiller.mkplayerlib.MKPlayer;
import com.mikiller.mkplayerlib.MKVideoView;
import com.mikiller.mkplayerlib.NetWorkReceiver;
import com.smg.mediaplayer.R;
import com.smg.mediaplayer.base.BaseActivity;
import com.smg.mediaplayer.logic.SafeLogic;
import com.smg.mediaplayer.utils.SignatureUtils;
import com.smg.mediaplayer.widgets.AndroidMediaController;
import com.smg.mediaplayer.widgets.DanmakuEditor;
import com.uilib.mxgallery.utils.GalleryMediaUtils;
import com.uilib.mxgallery.widgets.MXGallery;
import com.uilib.utils.DisplayUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.BindViews;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

public class WelcomeActivity extends BaseActivity {
//    private ImageView iv_preview;
    private Button btn_gallery;
    @BindView(R.id.videoPlayer)
    MKPlayer video;
    @BindView(R.id.edt_danmaku)
    TextInputEditText edt_danmaku;
    @BindView(R.id.edt_dmk)
    DanmakuEditor edt_dmk;

    RelativeLayout.LayoutParams dmkLp;
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
        video.setMediaController(mediaController = new AndroidMediaController(this, true, null));
        video.setCustomWidgetListener(new MKPlayer.CustomWidgetListener() {
            @Override
            public void onCustomClicked(int viewId, String extra) {
                switch (viewId){
                    case R.id.rdb_fd:
                    case R.id.rdb_sd:
                    case R.id.rdb_hd:
                    case R.id.rdb_4k:
                        video.toggleMediaControlsVisiblity();
                        switchDefinition(extra);
                        video.start();
                        break;
                    case R.id.btn_danmaku:
//                        dmkLp.addRule(RelativeLayout.BELOW, R.id.videoPlayer);
                        video.pause();
                        video.toggleMediaControlsVisiblity();
                        edt_dmk.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
        video.setVideoUri(MKPlayer.SD, Uri.parse("http://flv2.bn.netease.com/videolib3/1611/28/GbgsL3639/SD/movie_index.m3u8"));
        video.setVideoUri(MKPlayer.HD, Uri.parse("http://flv2.bn.netease.com/videolib3/1611/28/GbgsL3639/HD/movie_index.m3u8"));
        video.preparedVideo();
        video.setNeedDanmaku(true);
        mediaController.setDefinitions(MKPlayer.SD, MKPlayer.HD);
        mediaController.setDefaultDefinition(video.getDefaultDefinition());
        mediaController.setNeedDanmaku(true);
        video.start();

        dmkLp = (RelativeLayout.LayoutParams) edt_dmk.getLayoutParams();
        edt_dmk.setSendListener(new DanmakuEditor.OnSendDanmakuListener() {
            @Override
            public void onSend(String txt, String style) {
                video.addDanmaku(txt, 25f, style);
                if(video.isFullScreen())
                    edt_dmk.setVisibility(View.GONE);
                video.start();
            }
        });


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

    private void switchDefinition(String definition){
        int currentPos = 0;
        if(video.isPlaying()){
            currentPos = video.getCurrentPosition();
            video.pausePlayback();
        }
        video.toggleVideoUri(definition, currentPos);
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
        if(!video.isFullScreen()){
            dmkLp.addRule(RelativeLayout.BELOW, R.id.videoPlayer);
            edt_dmk.setLayoutParams(dmkLp);
        }else{
            dmkLp.addRule(RelativeLayout.BELOW, View.NO_ID);
            edt_dmk.setLayoutParams(dmkLp);
        }
        edt_dmk.toggleFullScreen(video.isFullScreen());
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
                    video.setVideoUri(MKPlayer.HD, GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)));
                    video.setThumb("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1525333972&di=2b78133b5e99f44caafcbf89b7fb1229&imgtype=jpg&er=1&src=http%3A%2F%2Fwww.chuguo.cn%2Fimage%2Finfo%2FImage%2F2015%2F201501072.png");
                    //video.start();
                }
//                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)),R.mipmap.placeholder, iv_preview);
                break;
        }
    }

    NetWorkReceiver receiver;
    @Override
    protected void onResume() {
        super.onResume();
        if(receiver == null)
            receiver = new NetWorkReceiver(new NetWorkReceiver.OnNetWorkChangedListener() {
                @Override
                public void isMobileConnected() {
                    if(video.isPlaying()) {
                        video.showNetWorkDlg(video.getCurrentPosition());
                        video.pausePlayback();
                    }
                }

                @Override
                public void isWifiConnected() {
                    if(!video.isPlaying()) {
                        video.resume(video.getInterruptPosition());
                    }
                }
            });
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(receiver != null)
            unregisterReceiver(receiver);

    }

    @Override
    protected void onDestroy() {
        video.stopPlayback();
        super.onDestroy();
    }
}
