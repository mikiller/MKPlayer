package com.smg.mediaplayer.utils;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mikiller on 2018/3/27.
 */

public class StringUtils {
    public static boolean checkPhone(String phone){
        return !TextUtils.isEmpty(phone) && phone.matches("^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(17[0,6,7])|(18[0,5-9]))\\d{8}$");
    }

    public static boolean checkPwd(String pwd){
        return !TextUtils.isEmpty(pwd) && pwd.toString().matches("^((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[~!@#$%^&*_=+/.?,<>:\"\']).{6,16})$");
    }

    public static String getFormatTimeStr(long timeStamp){
        long min = 1000 * 60l, hour = min * 60l, day = hour * 24l, sevenDay = day * 7l;
        long deltaT = System.currentTimeMillis() - timeStamp;
        if(deltaT <= min * 10l){
            return "刚刚";
        }else if(deltaT <= hour){
            return deltaT / min + "分钟前";
        }else if(deltaT <= day){
            return deltaT / hour + "小时前";
        }else if(deltaT <= sevenDay){
            return deltaT / day + "天前";
        }else{
            return new SimpleDateFormat("yyyy年MM月dd日").format(new Date(timeStamp));
        }
    }

    public static String getDateStr(String format, long timeStamp){
        return new SimpleDateFormat(format).format(new Date(timeStamp));
    }

    public static String getDateStr(long timeStamp){
        return getDateStr("MM月dd日 HH:mm", timeStamp);
    }
}
