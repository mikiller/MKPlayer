package com.mikiller.mkplayerlib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Mikiller on 2018/5/10.
 */

public class NetWorkHint extends RelativeLayout {
    private TextView tv_hint, tv_start, tv_stop;
    private onBtnClickListener listener;
    public NetWorkHint(Context context) {
        this(context, null, 0);
    }

    public NetWorkHint(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetWorkHint(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context){
        LayoutInflater.from(context).inflate(R.layout.layout_network_hint, this);
        tv_hint = findViewById(R.id.tv_networkHint);
        tv_start = findViewById(R.id.tv_start);
        tv_stop = findViewById(R.id.tv_stop);

        tv_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null)
                    listener.onStart();
                NetWorkHint.this.setVisibility(GONE);
            }
        });

        tv_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null)
                    listener.onStop();
                NetWorkHint.this.setVisibility(GONE);
            }
        });
    }

    public void setHint(String txt){
        tv_hint.setText(txt);
    }

    public void setHint(int resId){
        tv_hint.setText(resId);
    }

    public void setStartTxt(String txt){
        tv_start.setText(txt);
    }

    public void setStartTxt(int resId){
        tv_start.setText(resId);
    }

    public void setStopTxt(String txt){
        tv_stop.setText(txt);
    }

    public void setStopTxt(int resId){
        tv_stop.setText(resId);
    }

    public void setBtnClickListener(onBtnClickListener listener){
        this.listener = listener;
    }

    public interface onBtnClickListener{
        void onStart();
        void onStop();
    }
}
