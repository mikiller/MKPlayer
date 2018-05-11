package com.mikiller.mkplayerlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.mikiller.utils.NetWorkUtils;

/**
 * Created by Mikiller on 2018/5/11.
 */

public class NetWorkReceiver extends BroadcastReceiver {
    private OnNetWorkChangedListener listener;

    public NetWorkReceiver(OnNetWorkChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(NetWorkUtils.isWifiConnected(context)){
            if(listener != null)
                listener.isWifiConnected();
        }else if(NetWorkUtils.isMobileConnected(context)){
            if(listener != null)
                listener.isMobileConnected();
        }
    }

    public interface OnNetWorkChangedListener{
        void isMobileConnected();
        void isWifiConnected();
    }
}
