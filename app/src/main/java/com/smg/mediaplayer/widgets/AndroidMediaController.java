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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikiller.mkplayerlib.MKMediaController;
import com.smg.mediaplayer.R;
import com.uilib.titlebar.TitleBar;
import com.uilib.utils.AnimUtils;

import java.util.ArrayList;

import tv.danmaku.ijk.media.viewlib.widget.media.IMediaController;


public class AndroidMediaController extends MKMediaController implements IMediaController {
    private RelativeLayout rl_controllerTop;
    private ImageButton btn_return;
    private TextView btn_definition;
    private RadioGroup rdg_definition;
    private TitleBar titleBar;

    public AndroidMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public AndroidMediaController(Context context, boolean useFastForward, TitleBar titleBar) {
        super(context, useFastForward);
        initView(context);
        this.titleBar = titleBar;
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
    protected boolean needFullScreen() {
        return true;
    }

    @Override
    protected void initControllerView(View v) {
        super.initControllerView(v);
        rl_controllerTop = v.findViewById(R.id.rl_controllerTop);
        btn_return = v.findViewById(R.id.btn_return);
        btn_definition = v.findViewById(R.id.btn_definition);
        rdg_definition = v.findViewById(R.id.rdg_definition);

        btn_return.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_fullScreen.performClick();
            }
        });
        btn_definition.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rdg_definition.getVisibility() == VISIBLE) {
                    AnimUtils.startAlphaAnim(rdg_definition, 1f,  0f, 150);
                    postDelayed(mFadeOut, sDefaultTimeout);
                }else{
                    rdg_definition.setVisibility(VISIBLE);
                    removeCallbacks(mFadeOut);
                }
            }
        });

        rdg_definition.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rdb = group.findViewById(checkedId);
                if(rdb.isChecked()) {
                    btn_definition.setText(rdb.getText().toString());
                    AnimUtils.startAlphaAnim(rdg_definition, 1f, 0f, 150);
                    if(customListener != null)
                        customListener.onClick(rdb);
                }
            }
        });
    }

    public void setDefinitions(String... definitions){
        if(definitions != null){
            for(String def : definitions){
                rdg_definition.findViewWithTag(def).setVisibility(VISIBLE);
            }
        }
    }

    public void setDefaultDefinition(String definition){
        btn_definition.setText(((RadioButton)rdg_definition.findViewWithTag(definition)).getText().toString());
    }

    @Override
    public void onFullScreen(boolean isFull) {
        super.onFullScreen(isFull);
        if(rl_controllerTop != null)
            rl_controllerTop.setVisibility(isFull ? VISIBLE : GONE);
        if(titleBar != null)
            titleBar.setVisibility(isFull ? GONE : VISIBLE);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
        if(rdg_definition != null && rdg_definition.getVisibility() == VISIBLE)
            rdg_definition.setVisibility(GONE);
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
