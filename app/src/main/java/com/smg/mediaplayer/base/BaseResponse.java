package com.smg.mediaplayer.base;

import java.io.Serializable;

/**
 * @author Mikiller
 */
public class BaseResponse<T> implements Serializable{
    private static final long serialVersionUID = 9212683573784775316L;
    private String code;

    private String message;

    private T data;

    public BaseResponse(String code, String message, T rst) {
        this.code = code;
        this.message = message;
        this.data = rst;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T result) {
        this.data = result;
    }
}
