package com.netlib.mkokhttp.builder;

import com.netlib.mkokhttp.request.PostFormRequest;
import com.netlib.mkokhttp.request.RequestCall;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostFormBuilder extends OkHttpRequestBuilder<PostFormBuilder> implements HasParamsable
{
    private List<FileInput> files = new ArrayList<>();

    @Override
    public RequestCall build()
    {
        return new PostFormRequest(url, tag, params, headers, files,id).build();
    }

    public PostFormBuilder files(Map<String, File> files)
    {
        for (String key : files.keySet())
        {
            this.files.add(new FileInput(key, files.get(key).getName(), files.get(key)));
        }
        return this;
    }

    public PostFormBuilder addFile(String key, String filename, File file)
    {
        files.add(new FileInput(key, filename, file));
        return this;
    }

    public static class FileInput
    {
        public String key;
        public String filename;
        public File file;

        public FileInput(String key, String filename, File file)
        {
            this.key = key;
            this.filename = filename;
            this.file = file;
        }

        @Override
        public String toString()
        {
            return "FileInput{" +
                    "key='" + key + '\'' +
                    ", filename='" + filename + '\'' +
                    ", file=" + file +
                    '}';
        }
    }



    @Override
    public PostFormBuilder params(Map<String, String> params)
    {
        this.params = params;
        return this;
    }

    @Override
    public PostFormBuilder addParams(String key, String val)
    {
        if (this.params == null)
        {
            params = new LinkedHashMap<>();
        }
        params.put(key, val);
        return this;
    }




}
