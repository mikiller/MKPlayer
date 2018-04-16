package com.netlib.mkokhttp.callback;

public interface IGenericsSerializator {
    <T> T transform(String response, Class<T> classOfT);
}
