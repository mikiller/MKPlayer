package com.netlib.mkokhttp;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.netlib.mkokhttp.builder.OkHttpRequestBuilder;
import com.netlib.mkokhttp.builder.PostFormBuilder;
import com.netlib.mkokhttp.callback.Callback;
import com.netlib.mkokhttp.https.HttpsUtils;
import com.netlib.mkokhttp.log.LoggerInterceptor;
import com.netlib.mkokhttp.utils.Exceptions;
import com.netlib.mkokhttp.utils.OkHttpUtils;
import com.netlib.mkokhttp.utils.ReflectUtils;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.OkHttpClient;

public class OkHttpManager {

    private OkHttpUtils httpUtils = OkHttpUtils.getInstance();

    public enum RequestType{
        GET, POST, JSONPOST
    }

    private Gson gson = null;

    private OkHttpManager() {
        gson = new Gson();
    }

    private static class OkHttpManagerFactory{
        private static OkHttpManager instance = new OkHttpManager();
    }

    public static OkHttpManager getInstance(){
        return OkHttpManagerFactory.instance;
    }

    public void init(){
        initClient();
    }

    public void initClient(){
        OkHttpUtils.initClient(getDefaultClient());
    }

    private OkHttpClient getDefaultClient(){
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(null, null, null);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                .addInterceptor(new LoggerInterceptor("TAG"))
//                .cookieJar(cookieJar1)
                .hostnameVerifier(new HostnameVerifier()
                {
                    @Override
                    public boolean verify(String hostname, SSLSession session)
                    {
                        return true;
                    }
                })
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .build();
        return okHttpClient;
    }

    public Gson getGson(){
        return gson;
    }

    public void sendRequest(String url, RequestType requestType, Object paramObj, Map<String, File> files, Callback callback){
        if(requestType == RequestType.JSONPOST){
            executeRequest(requestJsonPost(gson.toJson(paramObj)), url, callback);
        }else{
            sendRequest(url, requestType, ReflectUtils.getInstance().toMap(paramObj), files, callback);
        }
    }

    public void sendRequest(String url, RequestType requestType, Map<String, String> params, Map<String, File> files, Callback callback){
        if(requestType == RequestType.GET) {
            executeRequest(requestGet(params), url, callback);
        }else if(requestType == RequestType.POST){
            executeRequest(requestPost(params, files), url, callback);
        }
        else{
            callback.onError(null, Exceptions.wrongParam("%1$s:%2$s", "-997", "requestType is error"), -1);
        }
    }

    private OkHttpRequestBuilder requestGet(Map<String, String> params){
        return OkHttpUtils.get().params(params);
    }

    private OkHttpRequestBuilder requestJsonPost(String json){
        return OkHttpUtils.postJsonString().content(json);
    }

    private OkHttpRequestBuilder requestPost(Map<String, String> params, Map<String, File> files){
        PostFormBuilder builder = OkHttpUtils.post().params(params);
        if(files.size() > 0)
            builder.files(files);
        return builder;
    }

    private void executeRequest(OkHttpRequestBuilder okHttpRequest, String url, Callback callback){
        okHttpRequest.url(url).build().execute(callback);
    }

    public void uploadFile(String url, File file, Callback callback){
        if(!file.exists())
            callback.onError(null, new NoSuchFieldException("File not found!"), -1);
        OkHttpUtils.postFile().url(url).file(file).build().execute(callback);
    }
}
