package com.smg.mediaplayer.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.netlib.mkokhttp.OkHttpManager;
import com.netlib.mkokhttp.callback.Callback;
import com.netlib.mkokhttp.utils.Exceptions;
import com.smg.mediaplayer.R;

import java.io.File;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Mikiller on 2016/7/21.
 */
public abstract class BaseLogic<T> extends Callback<T> {
    private final String TAG = this.getClass().getSimpleName();
    /**
     * 接口地址统一ip
     */
    private String hostIp = "https://as.dun.163yun.com/v3/";
    /**
     * 具体接口名称<br>
     * 由子类实现{@link #setUrl()}设置
     */
    protected String url;
    protected Context context;
    OkHttpManager httpMgr;
    protected ProgressDialog networkDlg;
    /**
     * 接口返回类型<br>
     * 由子类实现{@link #setResponseType()}设置
     */
    protected Type responseType;
    /**
     * 是否需要加载对话框<br>
     * 由子类实现{@link #setIsNeedDlg(boolean)}设置
     */
    protected boolean needDlg = false;
    /**
     * 对象类型参数
     */
    protected BaseModel model;
    /**
     * key-value键值对类型参数
     */
    protected Map<String, String> params;
    /**
     * 上传文件用到的文件列表
     */
    protected Map<String, File> files;

    protected LogicCallback callback;

    private String networkError, networkErrorNeedLogin, parseError;

    public BaseLogic(Context context) {
        httpMgr = OkHttpManager.getInstance();
        httpMgr.init();
        if (context != null) {
            this.context = context;
            networkDlg = new ProgressDialog(context,
                    ProgressDialog.THEME_HOLO_LIGHT);
            networkDlg.setMessage("正在网络交互，请稍后...");
            networkDlg.setCanceledOnTouchOutside(false);
            networkDlg.setCancelable(true);

            networkError = context.getString(R.string.network_error);
            networkErrorNeedLogin = context.getString(R.string.network_error_needlogin);
            parseError = context.getString(R.string.parse_date_error);
        }
        setUrl();
        setResponseType();
        setIsNeedDlg(isNeedDlg());
        this.files = new LinkedHashMap<>();


    }

    public BaseLogic(Context context, BaseModel model) {
        this(context);
        this.model = model;
    }

    public BaseLogic(Context context, Map<String, String> params) {
        this(context);
        this.params = params;
    }

    public void setParams(String key, String value) {
        if (params == null)
            params = new LinkedHashMap<>();
        params.put(key, value);
    }

    /**
     * 子类实现对{@code respoinseType}的赋值<p>
     * 通常{@code responseType}通过 {@code new com.google.gson.reflect.TypeToken<BaseResponse<T>>{}.getType()}的方式设置<p>
     *
     * @see TypeToken#getType()
     * @see BaseResponse
     */
    protected abstract void setResponseType();

    /**
     * 子类实现对{@code url}的赋值
     */
    protected abstract void setUrl();

    protected abstract boolean isNeedDlg();

    private void setIsNeedDlg(boolean isNeedDlg) {
        needDlg = isNeedDlg;
    }

    /**
     * 由子类通过调用父类{@link #sendRequest(OkHttpManager.RequestType)}实现
     */
    public abstract void sendRequest();

    /**
     * 根据请求参数类型选择调用{@code OkHttpManager.sendRequest}方法
     *
     * @see OkHttpManager#sendRequest(String, OkHttpManager.RequestType, Object, Map, Callback)
     * @see OkHttpManager#sendRequest(String, OkHttpManager.RequestType, Map, Map, Callback)
     */
    protected void sendRequest(OkHttpManager.RequestType requestType) {
        if (needDlg)
            showProgressDialog();
        if (model != null)
            httpMgr.sendRequest(hostIp.concat(url), requestType, model, files, this);
        else if (params != null) {
            httpMgr.sendRequest(hostIp.concat(url), requestType, params, files, this);
        }
    }

    public void setFiles(Map<String, File> files) {
        this.files = files;
    }

    public void setFile(String name, File file) {
        files.put(name, file);
    }

    public LogicCallback getCallback() {
        return callback;
    }

    public BaseLogic<T> setCallback(LogicCallback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public T parseNetworkResponse(Response response, int id) throws Exception {
        T result = null;
        String respStr = response.body().string();
        Log.d(TAG, respStr);
        if (respStr.contains("<!DOCTYPE html")) {
            //返回内容为某些需要登录的wifi热点弹出页
            //提示需要登录wifi
            throw Exceptions.connectError("-999", networkErrorNeedLogin);
        }

        BaseResponse<T> logicResp = parse(respStr);
        if (!logicResp.getCode().equals("")) {
            //自行根据接口协议，设定code的失败返回值
            throw Exceptions.io(logicResp.getCode(), logicResp.getMessage());
        } else if (logicResp.getData() == null) {
            //自行根据接口协议，判断data是否可为空
            Exceptions.jsonParseError("-998", parseError);
        } else {
            result = logicResp.getData();
            //save response to local for offline work
            saveLocalData(respStr);
        }

        return result;

    }

    @Override
    public void onError(Call call, Exception e, int id) {
        if (networkDlg != null && networkDlg.isShowing())
            networkDlg.dismiss();
        String err[] = e.getMessage().split(":");
        String code = err[0], msg = err[1];
        BaseResponse<T> localData = new BaseResponse<>(code, msg, getLocalData());
        onFailed(code, msg, localData.getData());
        if (callback != null)
            callback.onFailed(code, msg, localData.getData());
    }

    @Override
    public void onResponse(T response, int id) {
        if (networkDlg != null && networkDlg.isShowing())
            networkDlg.dismiss();
        onSuccess(response);
        if (callback != null)
            callback.onSuccess(response);
    }

    @Override
    public void onCancel(Call call, int id) {
        if (networkDlg != null && networkDlg.isShowing())
            networkDlg.dismiss();
        BaseResponse<T> localData = new BaseResponse<>("-1", "用户取消", getLocalData());
        onFailed(localData.getCode(), localData.getMessage(), localData.getData());
        if (callback != null)
            callback.onFailed(localData.getCode(), localData.getMessage(), localData.getData());
    }

    /**
     * 通过调用{@link com.google.gson.Gson#fromJson(String, Type)}解析接口返回的json数据
     *
     * @param json 接口返回的json字符串
     * @throws JsonSyntaxException
     * @see #responseType
     */
    protected BaseResponse<T> parse(String json) throws JsonSyntaxException {
        return httpMgr.getGson().fromJson(json, responseType);
    }

    /**
     * 保存本地数据
     * 根据需求实现
     * */
    protected void saveLocalData(String resultStr){
        //TODO: SharePreferenceUtil.getInstance().saveData(url, resultStr);
    }

    /**
     * 获取本地离线数据
     * 根据需求实现
     */
    protected T getLocalData() {
        return null;
    }

    /**
     * 接口内部实现的请求成功回调<br>
     * 通常处理一些与UI无关的逻辑或者对数据装箱后返回给UI层回调
     * 在{@link LogicCallback#onSuccess(Object)}之前被调用
     *
     * @param response 解析接口返回的json后得到的对象
     * @see #onResponse(Object, int)
     */
    public abstract void onSuccess(T response);

    /**
     * 接口内部实现的请求失败回调<br>
     * 通常处理一些与UI无关的逻辑或者对离线数据装箱后返回给UI层回调
     * 在{@link LogicCallback#onFailed(String, String, Object)} 之前被调用
     *
     * @param code      接口错误码
     * @param msg       接口调用或者返回的失败信息
     * @param localData 上一次成功返回后保存的离线对象。可为空
     * @see #onError(Call, Exception, int)
     * @see #onCancel(Call, int)
     */
    public abstract void onFailed(String code, String msg, T localData);

    @SuppressLint("NewApi")
    private void showProgressDialog() {
        try {
            if (networkDlg != null
                    && !networkDlg.isShowing()
//                    && !ViewManager.INSTANCE.getCurActivity().isFinishing()
//                    && !ViewManager.INSTANCE.getCurActivity().isDestroyed()
                    && networkDlg.getOwnerActivity() == null) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        networkDlg.show();
                        Window window = networkDlg.getWindow();
                        WindowManager.LayoutParams lp = window.getAttributes();
                        // lp.alpha = 0.0f;// 透明度
                        lp.dimAmount = 0.0f;// 黑暗度
                        window.setAttributes(lp);
                    }
                });
            }
        } catch (Exception e) {
        }

    }

    public interface LogicCallback<T> {
        void onSuccess(T response);

        void onFailed(String code, String msg, T localData);
    }
}
