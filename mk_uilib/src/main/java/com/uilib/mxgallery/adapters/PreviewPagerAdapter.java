/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uilib.mxgallery.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.mikiller.mkglidelib.imageloader.GlideImageLoader;
import com.uilib.mxgallery.models.ItemModel;
import com.uilib.R;
import com.uilib.mxgallery.models.MimeType;
import com.uilib.utils.DisplayUtil;
import com.uilib.zoomimageview.PinchImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PreviewPagerAdapter extends PagerAdapter {
    private Context mContext;
    private ArrayList<ItemModel> mItems = new ArrayList<>();

    private ViewGroup mContainer;
    private PageHolder lastHolder;
    private Timer timer;

    public PreviewPagerAdapter(Context context, List<ItemModel> items) {
        mContext = context;
        if (items != null)
            mItems.addAll(items);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View page = LayoutInflater.from(mContext).inflate(R.layout.layout_preview_page, null, false);
        ItemModel item = mItems.get(position);
        PageHolder holder = new PageHolder(page);
        holder.setPreview(MimeType.isPic(item.mimeType), item);
        page.setTag(holder);
        page.setId(position);
        container.addView(page);
        mContainer = container;

        return page;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((PageHolder) ((View) object).getTag()).stopVideo();
        container.removeView((View) object);
    }

    public void release() {
        mItems.clear();
        mItems = null;
        mContext = null;
        mContainer = null;
        lastHolder = null;
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        System.gc();
    }

//    public ItemModel getMediaItem(int position) {
//        return mItems.get(position);
//    }
//
//    public void addAll(List<ItemModel> items) {
//        mItems.addAll(items);
//    }

    public void onPageSelected(int pos) {
        if (lastHolder != null && !lastHolder.isPic) {
            lastHolder.stopVideo();
        }
        ((PageHolder) mContainer.findViewById(pos).getTag()).viewVideo.resume();
    }

    private class PageHolder {
        private PinchImageView iv_preview;
        private RelativeLayout rl_vieoPreview;
        private VideoView viewVideo;
        private CheckBox btn_start;
        private TextView tv_time;
        private SeekBar pgs;
        private VidoeState state = VidoeState.STOP;
        private boolean isPic = false;

        public PageHolder(View page) {
            iv_preview = (PinchImageView) page.findViewById(R.id.iv_preview);
            rl_vieoPreview = (RelativeLayout) page.findViewById(R.id.rl_vieoPreview);
            viewVideo = (VideoView) page.findViewById(R.id.viewVideo);
            btn_start = (CheckBox) page.findViewById(R.id.btn_start);
            tv_time = (TextView) page.findViewById(R.id.tv_time);
            pgs = (SeekBar) page.findViewById(R.id.pgs);
        }

        public void setPreview(boolean isPic, final ItemModel item) {
            this.isPic = isPic;
            iv_preview.setVisibility(isPic ? View.VISIBLE : View.GONE);
            rl_vieoPreview.setVisibility(isPic ? View.GONE : View.VISIBLE);
            if (isPic) {
                setImgPreview(item.getContentUri());
            } else {
                setVideoPreview(item);
            }
        }

        public void setImgPreview(Uri uri) {
            GlideImageLoader.getInstance().loadLocalImage(mContext, uri, R.mipmap.placeholder, iv_preview);
        }

        public void setVideoPreview(final ItemModel item) {

            //viewVideo.resume();
            viewVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopVideo();
                    viewVideo.resume();
                }
            });

            viewVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    prepareVideo();
                }
            });
            viewVideo.setVideoPath(item.getPath());
            btn_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (state == VidoeState.STOP) {
                        //停止状态
                        //重新播放
                        startVideo(viewVideo.getCurrentPosition());
                    } else if (state == VidoeState.START) {
                        //播放状态
                        //暂停播放
                        viewVideo.pause();
                        state = VidoeState.PAUSE;
                    } else if (state == VidoeState.PAUSE) {
                        //暂停状态
                        //继续播放
                        viewVideo.start();
                        state = VidoeState.START;
                    }

                }
            });

            pgs.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        viewVideo.seekTo(progress);
                        tv_time.setText(DateUtils.formatElapsedTime(progress/1000));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        public void prepareVideo() {
            viewVideo.setVisibility(View.VISIBLE);
            viewVideo.seekTo(viewVideo.getCurrentPosition() == 0 ? 1 : viewVideo.getCurrentPosition());
            pgs.setMax(viewVideo.getDuration());
            tv_time.setText(DateUtils.formatElapsedTime(viewVideo.getDuration() / 1000));
        }

        public void startVideo(int startPos) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (state == VidoeState.START && viewVideo.isPlaying())
                        updateTime();

                }
            }, 0, 10);
            viewVideo.seekTo(startPos);
            viewVideo.start();
            state = VidoeState.START;
            lastHolder = this;
        }

        private void updateTime() {
            if (tv_time == null)
                return;
            tv_time.post(new Runnable() {
                @Override
                public void run() {
                    tv_time.setText(DateUtils.formatElapsedTime(viewVideo.getCurrentPosition()/1000));
                    pgs.setProgress(viewVideo.getCurrentPosition());
                }
            });
        }

        public void stopVideo() {
            if (timer != null) {
                timer.cancel();
                timer = null;

            }
            viewVideo.stopPlayback();
//            viewVideo.suspend();
            pgs.setProgress(0);
            btn_start.setChecked(false);

            state = VidoeState.STOP;
        }
    }

    enum VidoeState{
        STOP, START, PAUSE;
    }

}
