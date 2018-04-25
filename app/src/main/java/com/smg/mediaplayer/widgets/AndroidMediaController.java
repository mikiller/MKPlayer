/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
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

package com.smg.mediaplayer.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.mikiller.mkplayerlib.MKMediaController;
import com.smg.mediaplayer.R;

import java.util.ArrayList;

import tv.danmaku.ijk.media.viewlib.widget.media.IMediaController;


public class AndroidMediaController extends MKMediaController implements IMediaController {
    private LinearLayout ll_controllerTop;
    private ImageButton btn_return;
    private ImageButton btn_setting;

    public AndroidMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public AndroidMediaController(Context context, boolean useFastForward) {
        super(context, useFastForward);
        initView(context);
    }

    public AndroidMediaController(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
    }

    @Override
    protected int getControllerRes() {
        return R.layout.custom_media_controller;
    }

    @Override
    protected void initControllerView(View v) {
        super.initControllerView(v);
        ll_controllerTop = v.findViewById(R.id.ll_controllerTop);
        btn_return = v.findViewById(R.id.btn_return);
        btn_setting = v.findViewById(R.id.btn_setting);
    }

    @Override
    public void onFullScreen(boolean isFull) {
        super.onFullScreen(isFull);
        if(ll_controllerTop != null)
            ll_controllerTop.setVisibility(isFull ? VISIBLE : GONE);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
        for (View view : mShowOnceArray)
            view.setVisibility(View.GONE);
        mShowOnceArray.clear();
    }

    //----------
    // Extends
    //----------
    private ArrayList<View> mShowOnceArray = new ArrayList<View>();

    public void showOnce(@NonNull View view) {
        mShowOnceArray.add(view);
        view.setVisibility(View.VISIBLE);
        show();
    }
}
