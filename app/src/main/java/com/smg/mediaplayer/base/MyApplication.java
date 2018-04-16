package com.smg.mediaplayer.base;

import android.app.Application;

import com.tendcloud.tenddata.TCAgent;


/**
 * Created by Mikiller on 2017/6/6.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TCAgent.LOG_ON = true;
        TCAgent.init(this, "B6311ABDC2C04A289DAF76D5B3E42F7E", "test");
        TCAgent.setReportUncaughtExceptions(true);

        //UMConfigure.setLogEnabled(false);
//        UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, "");
//        PlatformConfig.setWeixin("", "");
//        PlatformConfig.setQQZone("", "");
    }


}
