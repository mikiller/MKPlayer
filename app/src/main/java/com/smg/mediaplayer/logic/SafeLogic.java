package com.smg.mediaplayer.logic;

import android.content.Context;

import com.google.gson.reflect.TypeToken;
import com.netlib.mkokhttp.OkHttpManager;
import com.smg.mediaplayer.base.BaseLogic;
import com.smg.mediaplayer.base.BaseResponse;

import java.util.Map;

/**
 * Created by Mikiller on 2018/4/26.
 */

public class SafeLogic extends BaseLogic<SafeResp> {
    public SafeLogic(Context context, Map<String, String> params) {
        super(context, params);
    }

    @Override
    protected void setResponseType() {
        responseType = new TypeToken<BaseResponse<SafeResp>>(){}.getType();
    }

    @Override
    protected void setUrl() {
        url = "text/check";
    }

    @Override
    protected boolean isNeedDlg() {
        return false;
    }

    @Override
    public void sendRequest() {
        super.sendRequest(OkHttpManager.RequestType.POST);
    }

    @Override
    public void onSuccess(SafeResp response) {

    }

    @Override
    public void onFailed(String code, String msg, SafeResp localData) {

    }
}
