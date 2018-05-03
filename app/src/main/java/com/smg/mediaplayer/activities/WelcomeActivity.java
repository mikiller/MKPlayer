package com.smg.mediaplayer.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.mikiller.mkplayerlib.MKPlayer;
import com.mikiller.mkplayerlib.MKVideoView;
import com.smg.mediaplayer.R;
import com.smg.mediaplayer.base.BaseActivity;
import com.smg.mediaplayer.logic.SafeLogic;
import com.smg.mediaplayer.utils.SignatureUtils;
import com.smg.mediaplayer.widgets.AndroidMediaController;
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
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

public class WelcomeActivity extends BaseActivity {
    /** 产品密钥ID，产品标识 */
    private final static String SECRETID = "608bb66316e949abc4b2f3dcb8c0db1c";
    /** 产品私有密钥，服务端生成签名信息使用，请严格保管，避免泄露 */
    private final static String SECRETKEY = "36b892904788c1d39d39c7780b015937";
    /** 业务ID，易盾根据产品业务特点分配 */
    private final static String BUSINESSID = "97e91f0a5954d53599c3e57658b6e8b6";
    /** 易盾反垃圾云服务文本在线检测接口地址 */
    private final static String API_URL = "https://as.dun.163yun.com/v3/text/check";
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
                }
            }
        });
        video.setVideoUri(MKPlayer.SD, Uri.parse("http://flv2.bn.netease.com/videolib3/1611/28/GbgsL3639/SD/movie_index.m3u8"));
        video.setVideoUri(MKPlayer.HD, Uri.parse("http://flv2.bn.netease.com/videolib3/1611/28/GbgsL3639/HD/movie_index.m3u8"));
        video.preparedVideo();
        mediaController.setDefinitions(MKPlayer.SD, MKPlayer.HD);
        mediaController.setDefaultDefinition(video.getDefaultDefinition());
        video.start();

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

    @Override
    protected void onResume() {
        super.onResume();
        //video.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        video.stopPlayback();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        video.stopPlayback();
    }
}
