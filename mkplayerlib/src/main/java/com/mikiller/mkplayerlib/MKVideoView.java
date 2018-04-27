package com.mikiller.mkplayerlib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.viewlib.Settings;
import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;
import tv.danmaku.ijk.media.viewlib.widget.media.FileMediaDataSource;
import tv.danmaku.ijk.media.viewlib.widget.media.IMediaController;
import tv.danmaku.ijk.media.viewlib.widget.media.IRenderView;
import tv.danmaku.ijk.media.viewlib.widget.media.SurfaceRenderView;
import tv.danmaku.ijk.media.viewlib.widget.media.TextureRenderView;

/**
 * Created by Mikiller on 2018/4/11.
 */

public class MKVideoView extends FrameLayout implements MediaController.MediaPlayerControl {
    private final String TAG = this.getClass().getSimpleName();
    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    //-------------------------
    // Extend: Render
    //-------------------------
    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;

    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            IRenderView.AR_MATCH_PARENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};
    private int mCurrentAspectRatio = s_allAspectRatio[0];

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private Uri mUri;
    private Map<String, String> mHeaders;

    private Context mAppContext;
    private Settings mSettings;
    private IRenderView mRenderView;
    private IRenderView.ISurfaceHolder mSurfaceHolder = null;
    private IMediaPlayer mMediaPlayer = null;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private VideoViewStateListener stateListener;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoRotationDegree;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mCurrentBufferPercentage;
    private int mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean mCanPause = true;
    private boolean mCanSeekBack = true;
    private boolean mCanSeekForward = true;
    private int mCurrentRender = RENDER_SURFACE_VIEW;

    public MKVideoView(@NonNull Context context) {
        this(context, null, 0);
    }

    public MKVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MKVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr){
        mAppContext = context.getApplicationContext();
        mSettings = new Settings(mAppContext);

        initRander();

        mVideoWidth = 0;
        mVideoHeight = 0;
        // REMOVED: getHolder().addCallback(mSHCallback);
        // REMOVED: getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        // REMOVED: mPendingSubtitleTracks = new Vector<Pair<InputStream, MediaFormat>>();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    private void initRander(){
        //mSettings.getEnableSurfaceView()
        setRender(mCurrentRender);
    }

    public void setRender(int render) {
        switch (render) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_TEXTURE_VIEW: {
                TextureRenderView renderView = new TextureRenderView(getContext());
                if (mMediaPlayer != null) {
                    renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
                    renderView.setVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    renderView.setVideoSampleAspectRatio(mMediaPlayer.getVideoSarNum(), mMediaPlayer.getVideoSarDen());
                    renderView.setAspectRatio(mCurrentAspectRatio);
                }
                setRenderView(renderView);
                break;
            }
            case RENDER_SURFACE_VIEW: {
                SurfaceRenderView renderView = new SurfaceRenderView(getContext());
                setRenderView(renderView);
                break;
            }
            default:
                Log.e(TAG, String.format(Locale.getDefault(), "invalid render %d\n", render));
                break;
        }
    }

    public void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {
            if (mMediaPlayer != null)
                mMediaPlayer.setDisplay(null);

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }

        if (renderView == null)
            return;

        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

        View renderUIView = mRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);

        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                Log.e(TAG, "start in surface callback");
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }

            mSurfaceHolder = holder;
            if (mMediaPlayer != null)
                bindSurfaceHolder(mMediaPlayer, holder);
            //else
                openVideo();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }

            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            // REMOVED: if (mMediaController != null) mMediaController.hide();
            // REMOVED: release(true);
            releaseWithoutStop();
        }
    };

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        Log.e(TAG, "reset before open video");
        reset();

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        if(am != null)
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            mMediaPlayer = createPlayer(mSettings.getPlayer());

            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            // REMOVED: SubtitleController

            // REMOVED: mAudioSession
            initMediaPlayerListener();
            mCurrentBufferPercentage = 0;
            String scheme = mUri.getScheme();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    mSettings.getUsingMediaDataSource() &&
                    (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
                IMediaDataSource dataSource = new FileMediaDataSource(new File(mUri.toString()));
                mMediaPlayer.setDataSource(dataSource);
            }  else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            // REMOVED: mPendingSubtitleTracks

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            if(stateListener != null)
                stateListener.onOpenVideo();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    public IMediaPlayer createPlayer(int playerType) {
        IMediaPlayer mediaPlayer = null;

        switch (playerType) {
            case Settings.PV_PLAYER__IjkExoMediaPlayer: {
                IjkExoMediaPlayer IjkExoMediaPlayer = new IjkExoMediaPlayer(mAppContext);
                mediaPlayer = IjkExoMediaPlayer;
            }
            break;
            case Settings.PV_PLAYER__AndroidMediaPlayer: {
                AndroidMediaPlayer androidMediaPlayer = new AndroidMediaPlayer();
                mediaPlayer = androidMediaPlayer;
            }
            break;
            case Settings.PV_PLAYER__IjkMediaPlayer:
            default: {
                IjkMediaPlayer ijkMediaPlayer = null;
                if (mUri != null) {
                    ijkMediaPlayer = new IjkMediaPlayer();
                    ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

                    if (mSettings.getUsingMediaCodec()) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                        if (mSettings.getUsingMediaCodecAutoRotate()) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
                        }
                        if (mSettings.getMediaCodecHandleResolutionChange()) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
                        }
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                    }

                    if (mSettings.getUsingOpenSLES()) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
                    }

                    String pixelFormat = mSettings.getPixelFormat();
                    if (TextUtils.isEmpty(pixelFormat)) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
                    }
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                }
                mediaPlayer = ijkMediaPlayer;
            }
            break;
        }

        if (mSettings.getEnableDetachedSurfaceTextureView()) {
            mediaPlayer = new TextureMediaPlayer(mediaPlayer);
        }

        return mediaPlayer;
    }

    private void initMediaPlayerListener(){
        mMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mCurrentState = STATE_PREPARED;
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
                if (seekToPosition != 0) {
                    seekTo(seekToPosition);
                }
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                    // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    if (mRenderView != null) {
                        mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                        mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                            // We didn't actually change the size (it was already at the size
                            // we need), so we won't get a "surface changed" callback, so
                            // start the video here instead of in the callback.
                            if (mTargetState == STATE_PLAYING) {
                                if(stateListener != null)
                                    stateListener.onPrapered(true);
                                start();
                            } else if (!isPlaying() &&
                                    (seekToPosition != 0 || getCurrentPosition() > 0)) {
                                if(stateListener != null)
                                    stateListener.onPrapered(true);
                            }
                        }
                    }
                    if(stateListener != null)
                        stateListener.onPrapered(false);
                } else {
                    // We don't know the video size yet, but should start anyway.
                    // The video size might be reported to us later.
                    if (mTargetState == STATE_PLAYING) {
                        if(stateListener != null)
                            stateListener.onPrapered(false);
                        start();
                    }
                }
            }
        });
        mMediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                mVideoSarNum = mp.getVideoSarNum();
                mVideoSarDen = mp.getVideoSarDen();
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    if (mRenderView != null) {
                        Log.e(TAG, "set size in sizechangedlistener");
                        mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                        mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    }
                    // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    requestLayout();
                }
                if(stateListener!= null)
                    stateListener.onVideoSizeChanged(width, height, sar_num, sar_den);
            }
        });
        mMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                mCurrentState = STATE_PLAYBACK_COMPLETED;
                mTargetState = STATE_PLAYBACK_COMPLETED;
                if(stateListener != null)
                    stateListener.onCompleted();
            }
        });
        mMediaPlayer.setOnErrorListener(mOnErrorListener = new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                Log.e(TAG, "Error: " + framework_err + "," + impl_err);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;
                if(stateListener != null)
                    stateListener.onError(mp, framework_err, impl_err);

                    /* Otherwise, pop up an error dialog so the user knows that
                     * something bad has happened. Only try and pop up the dialog
                     * if we're attached to a window. When we're going away and no
                     * longer have a window, don't bother showing the user an error.
                     */
                if (getWindowToken() != null) {
                    Resources r = mAppContext.getResources();
                    int messageId;

                    if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                        messageId = R.string.VideoView_error_text_invalid_progressive_playback;
                    } else {
                        messageId = R.string.VideoView_error_text_unknown;
                    }

                    new AlertDialog.Builder(getContext())
                            .setMessage(messageId)
                            .setPositiveButton(R.string.VideoView_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            /* If we get here, there is no onError listener, so
                                             * at least inform them that the video is over.
                                             */
                                            if (stateListener != null) {
                                                stateListener.onCompleted();
                                            }
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                }
                return true;
            }
        });
        mMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                switch (what) {
                    case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                        Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + extra);
                        break;
                    case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                        Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                        Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                        Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                        Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                        Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                        mVideoRotationDegree = extra;
                        Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + extra);
                        if (mRenderView != null)
                            mRenderView.setVideoRotation(extra);
                        break;
                    case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                        Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                        break;
                }
                if(stateListener != null)
                    stateListener.onGetInfo(what, extra);
                return true;
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                mCurrentBufferPercentage = percent;
                if(stateListener != null)
                    stateListener.onBufferUpdated(percent);
            }
        });
        mMediaPlayer.setOnTimedTextListener(new IMediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
                if(stateListener != null)
                    stateListener.onTimedText(text);
            }
        });
    }

    public void setVideoStateListener(VideoViewStateListener listener){
        stateListener = listener;
    }

    public boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    public void releaseWithoutStop() {
        if (mMediaPlayer != null)
            mMediaPlayer.setDisplay(null);
    }

    public int setAspectRatio(int ratioType) {
        mCurrentAspectRatio = s_allAspectRatio[ratioType];
        if (mRenderView != null)
            mRenderView.setAspectRatio(mCurrentAspectRatio);
        return mCurrentAspectRatio;
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        requestLayout();
        invalidate();
    }

    // REMOVED: addSubtitleSource
    // REMOVED: mPendingSubtitleTracks

    public void reset(){
        if(mMediaPlayer != null){
            mMediaPlayer.reset();
            release(false);
        }
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            release(true);
        }
    }

    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            // REMOVED: mPendingSubtitleTracks.clear();
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }



    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            if(stateListener != null)
                stateListener.onStart();
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                if(stateListener != null)
                    stateListener.onPause();
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = pos;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public static class VideoViewStateListener{
        public void onPrapered(boolean needShow){}
        public void onOpenVideo(){}
        public void onStart(){}
        public void onPause(){}
        public void onCompleted(){}
        public void onError(IMediaPlayer mp, int framework_err, int impl_err){}
        public void onVideoSizeChanged(int width, int height, int sar_num, int sar_den){}
        public void onGetInfo(int what, int extra){}
        public void onBufferUpdated(int progress){}
        public void onTimedText(IjkTimedText text){}

    }
}
