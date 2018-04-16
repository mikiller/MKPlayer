package com.netlib.mkokhttp.builder;

import com.netlib.mkokhttp.utils.OkHttpUtils;
import com.netlib.mkokhttp.request.OtherRequest;
import com.netlib.mkokhttp.request.RequestCall;

public class HeadBuilder extends GetBuilder
{
    @Override
    public RequestCall build()
    {
        return new OtherRequest(null, null, OkHttpUtils.METHOD.HEAD, url, tag, params, headers,id).build();
    }
}
