package com.mikiller.mkplayerlib;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.Locale;

import tv.danmaku.ijk.media.viewlib.widget.media.IMediaController;

/**
 * Created by Mikiller on 2018/4/24.
 */

public class MKMediaController extends FrameLayout implements IMediaController, View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();
    private MediaController.MediaPlayerControl mPlayer;
    private final Context mContext;
    private View mAnchor;
    private View mRoot;
    private WindowManager mWindowManager;
    private Window mWindow;
    private View mDecor;
    private WindowManager.LayoutParams mDecorLayoutParams;
    private SeekBar mProgress;
    private TextView tv_duration, tv_playTime;
    private boolean mShowing;
    private boolean mDragging;
    private boolean isFullScreen;
    protected static final int sDefaultTimeout = 3000;
    private final boolean mUseFastForward;
    //    private boolean mFromXml;
    private boolean mListenersSet;

    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    protected ImageButton btn_fullScreen;
    protected ImageButton btn_play;
    protected ImageButton btn_fwd;
    protected ImageButton btn_rew;
    protected ImageButton mNextButton;
    protected ImageButton mPrevButton;
    //    private CharSequence mPlayDescription;
//    private CharSequence mPauseDescription;
    private View.OnClickListener mNextListener, mPrevListener;//, mFullScreenListener;
    protected OnClickListener customListener;

    private final OnLayoutChangeListener mLayoutChangeListener =
            new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight,
                                           int oldBottom) {
                    updateFloatingWindowLayout();
                    if (mShowing) {
                        mWindowManager.updateViewLayout(mDecor, mDecorLayoutParams);
                    }
                }
            };

    protected final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            if (!mDragging && mShowing && mPlayer.isPlaying()) {
                postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        private long targetPos = 0;
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;
            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            removeCallbacks(mShowProgress);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            targetPos = newposition;
            if (tv_playTime != null)
                tv_playTime.setText(stringForTime((int) newposition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            mPlayer.seekTo((int) targetPos);
            setProgress();
//            updatePausePlay();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            post(mShowProgress);
        }
    };

    public MKMediaController(@NonNull Context context, AttributeSet attrs) {
        super(context);
        mRoot = this;
        mContext = context;
        mUseFastForward = true;
//        mFromXml = true;
    }

    public MKMediaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        initFloatingWindowLayout();
        initFloatingWindow();
    }

    public MKMediaController(Context context) {
        this(context, true);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (mRoot != null)
            initControllerView(mRoot);
    }

    private void initFloatingWindow() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Class clazz = null;
        try {
            try {
                clazz = Class.forName("com.android.internal.policy.PolicyManager");
                Method method = clazz.getDeclaredMethod("makeNewWindow", Context.class);
                mWindow = (Window) method.invoke(null, getContext());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                try {
                    clazz = Class.forName("com.android.internal.policy.PhoneWindow");
                    mWindow = (Window) clazz.getDeclaredConstructor(Context.class).newInstance(mContext);
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }

            }
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InstantiationException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }

        if(mWindow == null)
            return;
//        mWindow = new PhoneWindow(mContext);
        mWindow.setWindowManager(mWindowManager, null, null);
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);

        mDecor = mWindow.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWindow.setStatusBarColor(Color.TRANSPARENT);
        }
//        mDecor.setOnTouchListener(new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    if (mShowing) {
//                        hide();
//                    }
//                }
//                return false;
//            }
//        });
        mWindow.setContentView(this);
        mWindow.setBackgroundDrawableResource(android.R.color.transparent);

        // While the media controller is up, the volume control keys should
        // affect the media stream type
        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    private void initFloatingWindowLayout() {
        mDecorLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.gravity = Gravity.TOP | Gravity.LEFT;
        //p.height = LayoutParams.WRAP_CONTENT;
        p.x = 0;
        p.format = PixelFormat.TRANSLUCENT;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
        p.token = null;
        p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
    }

    private void updateFloatingWindowLayout() {
        int[] anchorPos = new int[2];
        mAnchor.getLocationOnScreen(anchorPos);

        // we need to know the size of the controller so we can properly position it
        // within its space
        mDecor.measure(MeasureSpec.makeMeasureSpec(mAnchor.getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(mAnchor.getHeight(), MeasureSpec.AT_MOST));
        mRoot.getLayoutParams().height = mAnchor.getHeight();
        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.width = mAnchor.getWidth();
        p.height = mAnchor.getHeight();
        p.x = anchorPos[0] + (mAnchor.getWidth() - p.width) / 2;
        p.y = anchorPos[1] + mAnchor.getHeight() - mDecor.getMeasuredHeight();
    }

    @Override
    public void hide() {
        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
                removeCallbacks(mFadeOut);
                removeCallbacks(mShowProgress);
                mWindowManager.removeView(mDecor);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    @Override
    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void setAnchorView(View view) {
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }

        removeAllViews();
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(getControllerRes(), null);
        initControllerView(mRoot);
        addView(mRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    protected int getControllerRes() {
        return R.layout.media_controller;
    }

    protected boolean needFullScreen(){
        return false;
    }

    protected void initControllerView(View v) {
//        Resources res = mContext.getResources();
//        mPlayDescription = res
//                .getText(com.android.internal.R.string.lockscreen_transport_play_description);
//        mPauseDescription = res
//                .getText(com.android.internal.R.string.lockscreen_transport_pause_description);

        btn_play = v.findViewById(R.id.btn_play);
        if (btn_play != null) {
            btn_play.requestFocus();
            btn_play.setOnClickListener(this);
        }

        btn_fwd = v.findViewById(R.id.btn_fwd);
        if (btn_fwd != null) {
            btn_fwd.setOnClickListener(this);
//            if (!mFromXml) {
            btn_fwd.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
//            }
        }

        btn_rew = v.findViewById(R.id.btn_rew);
        if (btn_rew != null) {
            btn_rew.setOnClickListener(this);
//            if (!mFromXml) {
            btn_rew.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
//            }
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        //mNextButton = v.findViewById(com.android.internal.R.id.next);
        if (mNextButton != null && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        //mPrevButton = v.findViewById(com.android.internal.R.id.prev);
        if (mPrevButton != null && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        btn_fullScreen = v.findViewById(R.id.btn_fullScreen);
        if (btn_fullScreen != null) {
            btn_fullScreen.setVisibility(needFullScreen() ? VISIBLE : GONE);
            btn_fullScreen.setOnClickListener(customListener);
        }

        mProgress = v.findViewById(R.id.progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        tv_duration = v.findViewById(R.id.tv_duration);
        tv_playTime = v.findViewById(R.id.tv_playTime);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    @Override
    public void setMediaPlayer(MediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay(mPlayer.isPlaying());
    }

    @Override
    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (btn_play != null) {
                btn_play.requestFocus();
            }
            disableUnsupportedButtons();
            updateFloatingWindowLayout();
            mWindowManager.addView(mDecor, mDecorLayoutParams);
            mShowing = true;
        }
        updatePausePlay(mPlayer.isPlaying());
        onFullScreen(isFullScreen);
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        post(mShowProgress);

        if (timeout != 0) {
            removeCallbacks(mFadeOut);
            postDelayed(mFadeOut, timeout);
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (tv_duration != null)
            tv_duration.setText(stringForTime(duration));
        if (tv_playTime != null)
            tv_playTime.setText(stringForTime(position));

        return position;
    }

    private void disableUnsupportedButtons() {
        try {
            if (!mPlayer.canPause()) {
                if (btn_play != null)
                    btn_play.setEnabled(false);
//                if (btn_bigPlay != null)
//                    btn_bigPlay.setEnabled(false);
            }
            if (btn_rew != null && !mPlayer.canSeekBackward()) {
                btn_rew.setEnabled(false);
            }
            if (btn_fwd != null && !mPlayer.canSeekForward()) {
                btn_fwd.setEnabled(false);
            }
            // TODO What we really should do is add a canSeek to the MediaPlayerControl interface;
            // this scheme can break the case when applications want to allow seek through the
            // progress bar but disable forward/backward buttons.
            //
            // However, currently the flags SEEK_BACKWARD_AVAILABLE, SEEK_FORWARD_AVAILABLE,
            // and SEEK_AVAILABLE are all (un)set together; as such the aforementioned issue
            // shouldn't arise in existing applications.
            if (mProgress != null && !mPlayer.canSeekBackward() && !mPlayer.canSeekForward()) {
                mProgress.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    private void updatePausePlay(boolean isPlay) {
        if (mRoot == null || btn_play == null)
            return;
        btn_play.setSelected(isPlay);
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            pause();
        } else {
            start();
        }

    }

    public void pause(){
        mPlayer.pause();
        updatePausePlay(false);
    }

    public void start(){
        mPlayer.start();
        updatePausePlay(true);
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        if (mRoot != null) {
            installPrevNextListeners();

            if (mNextButton != null) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null) {
                mPrevButton.setVisibility(View.VISIBLE);
            }
        }
    }

//    public void setFullScreenListener() {
//        if (mRoot != null) {
//            btn_fullScreen.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if(customWidgetListener != null)
//                }
//            });
//        }
//    }

    public void setCustomListener(OnClickListener listener){
        customListener = listener;
        if (mRoot != null && customListener != null) {
            btn_fullScreen.setOnClickListener(customListener);
        }
    }

    @Override
    public void toggleFullScreen() {
        if(btn_fullScreen != null)
            btn_fullScreen.performClick();
    }

    @Override
    public void onFullScreen(boolean isFull) {
        isFullScreen = isFull;
        if (btn_fullScreen != null)
            btn_fullScreen.setSelected(isFull);
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public void showOnce(View view) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        if (btn_play != null) {
            btn_play.setEnabled(enabled);
        }
        if (btn_fwd != null) {
            btn_fwd.setEnabled(enabled);
        }
        if (btn_rew != null) {
            btn_rew.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        if (btn_fullScreen != null)
            btn_fullScreen.setEnabled(enabled);
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_play) {
            show(sDefaultTimeout);
            doPauseResume();
        } else if (v.getId() == R.id.btn_rew) {
            int pos = mPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        } else if (v.getId() == R.id.btn_fwd) {
            int pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                show(sDefaultTimeout);
                doPauseResume();
                if (btn_play != null) {
                    btn_play.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay(true);
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay(false);
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MKMediaController.class.getName();
    }
}
