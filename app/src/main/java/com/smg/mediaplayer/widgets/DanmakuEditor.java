package com.smg.mediaplayer.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.smg.mediaplayer.R;

/**
 * Created by Mikiller on 2018/5/14.
 */

public class DanmakuEditor extends LinearLayout {
    private TextInputEditText edt_danmaku;
    private Button btn_send;
    private RadioGroup rdg_dmkStyle;
    private OnSendDanmakuListener sendListener;

    public void setSendListener(OnSendDanmakuListener sendListener) {
        this.sendListener = sendListener;
    }

    public DanmakuEditor(Context context) {
        this(context, null, 0);
    }

    public DanmakuEditor(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DanmakuEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, @Nullable AttributeSet attrs, int defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.layout_danmaku_editor, this);
        edt_danmaku = findViewById(R.id.edt_danmaku);
        btn_send = findViewById(R.id.btn_send);
        rdg_dmkStyle = findViewById(R.id.rdg_dmkStyle);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = edt_danmaku.getText().toString();
                if(sendListener != null){
                    sendListener.onSend(txt, (String) rdg_dmkStyle.findViewById(rdg_dmkStyle.getCheckedRadioButtonId()).getTag());
                }
                edt_danmaku.setText("");
                edt_danmaku.clearFocus();
            }
        });
    }

    public void toggleFullScreen(boolean isFullScreen){
        this.setVisibility(isFullScreen ? GONE : VISIBLE);
        rdg_dmkStyle.setVisibility(isFullScreen ? VISIBLE : GONE);
    }

    public interface OnSendDanmakuListener{
        void onSend(String txt, String style);
    }
}
