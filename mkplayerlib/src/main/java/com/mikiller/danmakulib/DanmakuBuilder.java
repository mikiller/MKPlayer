package com.mikiller.danmakulib;

import android.graphics.Color;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Danmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;

/**
 * Created by Mikiller on 2018/5/14.
 */

public class DanmakuBuilder {
    public final static String STYLE_RED = "dmk_red", STYLE_WHITE = "dmk_white", STYLE_GREEN = "dmk_green", STYLE_ORANGE = "dmk_orange";

    private static BaseDanmaku danmaku;
    public DanmakuBuilder createDanmaku(DanmakuContext dmkContext, int dmkType){
        danmaku = dmkContext.mDanmakuFactory.createDanmaku(dmkType);
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = true;
        danmaku.textShadowColor = Color.WHITE;
        return this;
    }

    public DanmakuBuilder setTime(long time){
        if(danmaku != null)
            danmaku.setTime(time);
        return this;
    }

    public DanmakuBuilder setText(String txt, float size){
        if(danmaku != null){
            danmaku.text = txt;
            danmaku.textSize = size;
        }
        return this;
    }

    public DanmakuBuilder setTextColor(int color){
        if(danmaku != null)
            danmaku.textColor = color;
        return this;
    }

    public DanmakuBuilder setBorderColor(int color){
        if(danmaku != null)
            danmaku.borderColor = color;
        return this;
    }

    public DanmakuBuilder setStyle(String style){
        switch (style){
            case STYLE_RED:
                danmaku.textColor = Color.parseColor("#ffff4444");
                break;
            case STYLE_WHITE:
                danmaku.textColor = Color.WHITE;
                danmaku.borderColor = Color.parseColor("#6633b5e5");
                break;
            case STYLE_GREEN:
                danmaku.borderColor = Color.parseColor("#ff99cc00");
                break;
            case STYLE_ORANGE:
                danmaku.borderColor = Color.WHITE;
                danmaku.textColor = Color.parseColor("#ffffbb33");
                break;
        }
        return this;
    }

    public BaseDanmaku build(){
        return danmaku;
    }
}
