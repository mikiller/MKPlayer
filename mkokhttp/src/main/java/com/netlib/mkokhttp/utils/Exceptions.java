package com.netlib.mkokhttp.utils;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.ConnectException;

public class Exceptions
{
    public static void illegalArgument(String msg, Object... params)
    {
        throw new IllegalArgumentException(String.format(msg, params));
    }

    public static ConnectException connectError(Object... params){
        return new ConnectException(String.format("%1$s:%2$s", params));
    }

    public static IOException io(Object... params) {
         return new IOException(String.format("%1$s:%2$s", params));
    }

    public static void jsonParseError(Object... params){
        throw new JsonParseException(String.format("%1$s:%2$s", params));
    }

    public static NoSuchFieldException wrongParam(Object... params){
        return new NoSuchFieldException(String.format("%1$s:%2$s", params));
    }
}
