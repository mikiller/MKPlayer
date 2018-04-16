package com.smg.mediaplayer.base;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import com.smg.mediaplayer.utils.PermissionUtils;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Mikiller on 2017/3/13.
 */

public abstract class BaseActivity extends AppCompatActivity {
    protected final String TAG = this.getClass().getSimpleName();
    Unbinder unbinder;
    protected int statusBarColor = Color.TRANSPARENT;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            setStatusBarColor();
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void setStatusBarColor(){
        getWindow().setStatusBarColor(statusBarColor);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(PermissionUtils.isNeedCheck){
            PermissionUtils.checkPermissions(this);
        }
        initData();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        unbinder = ButterKnife.bind(this);
        initView();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        unbinder = ButterKnife.bind(this);
        initView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        unbinder = ButterKnife.bind(this);
        initView();
    }

    abstract protected void initView();
    abstract protected void initData();

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "on resume");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    protected void back(){
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
