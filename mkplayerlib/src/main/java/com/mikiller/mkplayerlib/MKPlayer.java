package com.mikiller.mkplayerlib;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    private TextView tv_slideHint;
    private MKMediaController mediaController;
    private Map<String, Uri> urlMap = new HashMap<>();

    private boolean isFullScreen = false;
    private int mScreenUiVisibility;
    private int fullHeight;
    private ViewGroup.LayoutParams originLp;
    private int mInterruptPosition = 0;
    private GestureDetector gestureDetector;
    private GestureDetector.OnGestureListener gestureListener;
    private OnTouchListener touchListener;
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

    private void initView(@NonNull Context context, @Nullable AttributeSet attrs, final int defStyleAttr){
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        LayoutInflater.from(context).inflate(R.layout.layout_mkplayer, this);
        videoView = findViewById(R.id.videoView);
        iv_thumb = findViewById(R.id.iv_thumb);
        pgs_load = findViewById(R.id.pgs_loading);
        tv_slideHint = findViewById(R.id.tv_slideHint);
        setMediaController(new MKMediaController(context, false));
//        videoView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleMediaControlsVisiblity();
//            }
//        });
        setPlayerStateListener();

        gestureListener = new GestureDetector.SimpleOnGestureListener(){
            // 是否声音控制,默认为亮度控制，true为声音控制，false为亮度控制
            private boolean isVolume;
            // 是否横向滑动，默认为纵向滑动，true为横向滑动，false为纵向滑动
            private boolean isLandscape;
            private boolean isClick;
            private long mSeekPosition, mVolume = -1;
            private float mBright = 0.0f;
            private AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

            private Runnable hideHint = new Runnable() {
                @Override
                public void run() {
                    if(tv_slideHint.getVisibility() == VISIBLE) {
                        tv_slideHint.setVisibility(GONE);
                    }
                }
            };

            @Override
            public boolean onDown(MotionEvent e) {
                isClick = true;
                Log.e(TAG, "reset isclick");
                return super.onDown(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float mOldX = e1.getX(), mOldY = e1.getY();
                float deltaY = mOldY - e2.getY();
                float deltaX = mOldX - e2.getX();
                if(isClick) {
                    isClick = false;
                    tv_slideHint.setVisibility(VISIBLE);
                    // 判断左右或上下滑动
                    isLandscape = Math.abs(distanceX) >= Math.abs(distanceY);
                    // 判断是声音或亮度控制
                    isVolume = mOldX > getResources().getDisplayMetrics().widthPixels * 0.5f;
                }
                if(isLandscape){
                    seekProgress(-deltaX / videoView.getWidth());
                }else if(isVolume){
                    setVolume(deltaY / videoView.getHeight());
                }else{
                    setLight(deltaY / videoView.getHeight());
                }

                removeCallbacks(hideHint);
                postDelayed(hideHint, 1000);
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if(isLandscape)
                    videoView.seekTo((int) mSeekPosition);
                mVolume = -1;
                mBright = 0.0f;
                tv_slideHint.setText("");
                tv_slideHint.setVisibility(GONE);
                removeCallbacks(hideHint);
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleMediaControlsVisiblity();
                return super.onSingleTapUp(e);
            }

            private void seekProgress(float percent){
                int position = videoView.getCurrentPosition();
                long duration = videoView.getDuration();
                // 单次拖拽最大时间差为100秒或播放时长的1/2
                long deltaMax = Math.min(100 * 1000, duration / 2);
                // 计算滑动时间
                long delta = (long) (deltaMax * percent);
                // 目标位置
                mSeekPosition = delta + position;
                if (mSeekPosition > duration) {
                    mSeekPosition = duration;
                } else if (mSeekPosition <= 0) {
                    mSeekPosition = 0;
                }
                tv_slideHint.setText(DateUtils.formatElapsedTime(mSeekPosition / 1000));
            }

            private void setVolume(float percent){
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                if(mVolume == -1){
                    mVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if(mVolume < 0)
                        mVolume = 0;
                }
                int delta = (int) Math.max(0, Math.min(maxVolume, (percent * maxVolume + mVolume)));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) delta, 0);
                tv_slideHint.setText(String.format(getContext().getString(R.string.fmt_volume), delta*100/maxVolume));
            }

            private void setLight(float percent){
                WindowManager.LayoutParams attr = ((Activity)getContext()).getWindow().getAttributes();
                if(mBright == 0.0f){
                    mBright = Math.max(0.01f, attr.screenBrightness);
                }
                attr.screenBrightness = Math.max(0.01f, Math.min(1.0f, mBright + percent));
                ((Activity)getContext()).getWindow().setAttributes(attr);
                tv_slideHint.setText(String.format(getContext().getString(R.string.fmt_light), (int)(attr.screenBrightness * 100)));
            }
        };
        gestureDetector = new GestureDetector(getContext(), gestureListener);
        videoView.setOnTouchListener(touchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
        videoView.setClickable(true);
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

        mediaController.setOnTouchListener(touchListener);
        mediaController.setClickable(true);
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
