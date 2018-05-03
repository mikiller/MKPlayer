package com.mikiller.mkplayerlib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.mikiller.mkglidelib.imageloader.GlideImageLoader;
import com.mikiller.utils.NetWorkUtils;
import com.uilib.utils.DisplayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;

/**
 * Created by Mikiller on 2018/4/25.
 */

public class MKPlayer extends FrameLayout {
    private final String TAG = this.getClass().getSimpleName();
    public final static String FD = "FD", SD = "SD", HD = "HD", _4K = "4K";
    // 达到文件时长的允许误差值，用来判断是否播放完成
    private static final int INTERVAL_TIME = 1000;
    private MKVideoView videoView;
    private ImageView iv_thumb;
    private ProgressBar pgs_load;
    private MKMediaController mediaController;
    private Map<String, Uri> urlMap = new HashMap<>();

    private boolean isFullScreen = false;
    private int mScreenUiVisibility;
    private int fullHeight;
    private ViewGroup.LayoutParams originLp;
    private int mInterruptPosition = 0;
    protected CustomWidgetListener customWidgetListener;

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
        pgs_load = findViewById(R.id.pgs_loading);
        setMediaController(new MKMediaController(context, false));
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
            fullHeight = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getWidth();
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
        isFullScreen = isFull;
        changeViewHeight(isFull);
        mediaController.onFullScreen(isFull);
    }

    private void changeViewHeight(boolean isFull){
        if(isFull) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullHeight);
            setLayoutParams(lp);
        }else
            setLayoutParams(originLp);
    }

    public void setCustomWidgetListener(CustomWidgetListener listener){
        customWidgetListener = listener;
    }

    public void toggleFullScreen(){
        mediaController.toggleFullScreen();
    }

    public void setMediaController(MKMediaController controller){
        mediaController = controller;
//        mediaController.setCustomWidgetListener(new MKMediaController.CustomWidgetListener(){
//            @Override
//            public void onCustomClicked(int viewId, String extra) {
//                if(viewId == R.id.btn_fullScreen){
//                    changeScreenOrientation();
//                    toggleMediaControlsVisiblity();
//                }
//            }
//        });
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
    }

    /**
     * 全屏切换，点击全屏按钮
     */
    private void changeScreenOrientation() {
        if (DisplayUtil.getScreenOrientation((Activity) getContext()) != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    public void toggleMediaControlsVisiblity() {
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
            mediaController.setCustomListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(v.getId() == R.id.btn_fullScreen){
                        changeScreenOrientation();
                        toggleMediaControlsVisiblity();
                    }else if(customWidgetListener != null){
                        customWidgetListener.onCustomClicked(v.getId(), (String) v.getTag());
                    }
                }
            });
        }
    }

    public boolean isFullScreen(){
        return isFullScreen;
    }

    public boolean isPlaying(){
        return videoView.isPlaying();
    }

    public void setThumb(String url){
        GlideImageLoader.getInstance().loadImage(getContext(), url, R.mipmap.placeholder, iv_thumb, 0);
    }

    public void setVideoUri(String definition, Uri uri){
        urlMap.put(definition, uri);
    }

    public void toggleVideoUri(String definition, int pos){
        videoView.setCurrentDefinition(definition);
        videoView.setVideoURI(urlMap.get(definition));
        videoView.prepareVideo();
        videoView.seekTo(pos);
    }

    public void preparedVideo(){
        if(urlMap.size() == 0){
            Toast.makeText(getContext(), getContext().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
            return;
        }
        String rst = "";
        for(String key: urlMap.keySet()){
            rst = key;
            if(key.equals(videoView.getCurrentDefinition()))
                break;
        }

        toggleVideoUri(rst, 0);
    }

    public String getDefaultDefinition(){
        return videoView.getCurrentDefinition();
    }

    public int getCurrentPosition() {
        if (videoView != null) {
            return (int) videoView.getCurrentPosition();
        }
        return 0;
    }

    public void start(){
        mediaController.start();
    }

    public void stopPlayback(){
        videoView.stopPlayback();
    }

    public void pausePlayback(){
        videoView.release(false);
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
                pgs_load.setVisibility(GONE);
                iv_thumb.setVisibility(VISIBLE);
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
                if (videoView.getDuration() == -1 ||
                        (videoView.getInterruptPosition() + INTERVAL_TIME < videoView.getDuration())) {
                    mInterruptPosition = Math.max(videoView.getInterruptPosition(), mInterruptPosition);
                    pgs_load.setVisibility(VISIBLE);
                    Toast.makeText(getContext(), "网络异常", Toast.LENGTH_SHORT).show();

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(NetWorkUtils.isNetworkAvailable(getContext())){
                                videoView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        videoView.prepareVideo();
                                        videoView.seekTo(mInterruptPosition);
                                        videoView.start();
                                    }
                                });
                                cancel();
                            }
                        }
                    }, 1000, 1000);
                }
            }

            @Override
            public void onError(IMediaPlayer mp, int framework_err, int impl_err) {
                if(mediaController != null)
                    mediaController.hide();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        IjkMediaPlayer.native_profileEnd();
    }

    public interface CustomWidgetListener{
        void onCustomClicked(int viewId, String extra);
    }
}
