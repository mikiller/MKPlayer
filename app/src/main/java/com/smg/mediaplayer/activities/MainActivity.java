package com.smg.mediaplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import com.smg.mediaplayer.R;
import com.smg.mediaplayer.adapters.MainFragmentAdapter;
import com.smg.mediaplayer.base.BaseActivity;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.vp_main)
    ViewPager vp_main;
    @BindView(R.id.mainTab)
    TabLayout mainTab;

    MainFragmentAdapter mainAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void initView() {
        mainAdapter = new MainFragmentAdapter(getSupportFragmentManager());
        vp_main.setAdapter(mainAdapter);
        mainTab.setupWithViewPager(vp_main);
//        mainTab.getTabAt(0).setIcon(R.mipmap.ic_launcher);
//        mainTab.getTabAt(1).setIcon(R.mipmap.ic_launcher);
//        mainTab.getTabAt(2).setIcon(R.mipmap.ic_launcher);

    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK)
            return;
        switch(requestCode){
//            case 1:
//                File tmpFile = (File) data.getSerializableExtra(GalleryMediaUtils.TMP_FILE);
//                List<File> fileList = (List<File>) data.getSerializableExtra(GalleryMediaUtils.THUMB_LIST);
//                if(tmpFile != null){
//                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(tmpFile),R.mipmap.placeholder, iv_preview);
//                }else if(fileList != null && fileList.size() > 0)
//                    GlideImageLoader.getInstance().loadLocalImage(this, GalleryMediaUtils.getInstance().getFileUri(fileList.get(0)),R.mipmap.placeholder, iv_preview);
//                break;
        }
    }
}
