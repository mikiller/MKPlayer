package com.mikiller.mkplayerlib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.module.GlideModule;
import com.mikiller.mkglidelib.imageloader.GlideImageLoader;
import com.uilib.utils.DisplayUtil;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.viewlib.widget.media.IMediaController;

/**
 * Created by Mikiller on 2018/4/25.
 */

public class MKPlayer extends FrameLayout {
    private final String TAG = this.getClass().getSimpleName();
    private MKVideoView videoView;
    private ImageView iv_thumb;
    private MKMediaController mediaController;

    private int mScreenUiVisibility;
    private int fullHeight;
    private ViewGroup.LayoutParams originLp;

    public MKPlayer(@NonNull Context context) {
        this(context, null, 0);
    }

    public MKPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MKPlayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr){
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        LayoutInflater.from(context).inflate(R.layout.layout_mkplayer, this);
        videoView = findViewById(R.id.videoView);
        iv_thumb = findViewById(R.id.iv_thumb);

        videoView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMediaControlsVisiblity();
            }
        });
        setPlayerStateListener();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(fullHeight == 0){
            originLp = getLayoutParams();
            fullHeight = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getHeight();
        }
    }

    public void configurationChanged(Configuration newConfig){
        if (Build.VERSION.SDK_INT >= 14) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 获取关联 Activity 的 DecorView
                View decorView = ((Activity)getContext()).getWindow().getDecorView();
                // 保存旧的配置
                mScreenUiVisibility = decorView.getSystemUiVisibility();
                // 沉浸式使用这些Flag
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                ((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setFullScreen(true);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                View decorView = ((Activity)getContext()).getWindow().getDecorView();
                // 还原
                decorView.setSystemUiVisibility(mScreenUiVisibility);
                ((Activity)getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setFullScreen(false);
            }
        }
    }

    private void setFullScreen(boolean isFull){
        changeViewHeight(isFull);
        mediaController.onFullScreen(isFull);
    }

    private void changeViewHeight(boolean isFull){
        if(isFull) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            setLayoutParams(lp);
        }else
            setLayoutParams(originLp);
    }

    public void setThumb(String url){
        GlideImageLoader.getInstance().loadImage(getContext(), url, R.mipmap.placeholder, iv_thumb, 0);
    }

    public void setMediaController(MKMediaController controller){
        mediaController = controller;
        mediaController.setFullScreenListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullScreen();
                toggleMediaControlsVisiblity();
            }
        });
        mediaController.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController.isShowing())
                    mediaController.hide();
            }
        });
        if (mediaController != null) {
            mediaController.hide();
        }
        attachMediaController();
        //videoView.setMediaController(mediaController);
    }

    /**
     * 全屏切换，点击全屏按钮
     */
    private void toggleFullScreen() {
        if (DisplayUtil.getScreenOrientation((Activity) getContext()) != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void toggleMediaControlsVisiblity() {
        if(!videoView.isInPlaybackState() || mediaController == null)
            return;
        if (mediaController.isShowing()) {
            mediaController.hide();
        } else {
            mediaController.show();
        }
    }

    private void attachMediaController() {
        if (mediaController != null) {
            mediaController.setMediaPlayer(videoView);
            mediaController.setAnchorView(videoView);
            mediaController.setEnabled(videoView.isInPlaybackState());
        }
    }

    public void setVideoUri(Uri uri){
        videoView.setVideoURI(uri);
        videoView.openVideo();
    }

    public void start(){
//        videoView.start();
        mediaController.start();
    }

    public void stopPlayback(){
        videoView.stopPlayback();
    }

    private void setPlayerStateListener(){
        videoView.setVideoStateListener(new MKVideoView.VideoViewStateListener(){
            @Override
            public void onPrapered(boolean needShow) {
                super.onPrapered(needShow);
                if(mediaController != null) {
                    mediaController.setEnabled(true);
                    if(needShow)
                        mediaController.show();
                }
            }

            @Override
            public void onOpenVideo() {
                //attachMediaController();
            }

            @Override
            public void onStart() {
                iv_thumb.setVisibility(GONE);
            }

            @Override
            public void onCompleted() {
                super.onCompleted();
                if(mediaController != null)
                    mediaController.show();
            }

            @Override
            public void onError(IMediaPlayer mp, int framework_err, int impl_err) {
                super.onError(mp, framework_err, impl_err);
                if(mediaController != null)
                    mediaController.hide();
            }

            @Override
            public void onTimedText(IjkTimedText text) {
                Log.e(TAG, text.getText());
                super.onTimedText(text);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        IjkMediaPlayer.native_profileEnd();
    }
}
