package com.smg.mediaplayer.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

import com.smg.mediaplayer.R;
import com.uilib.swipetoloadlayout.OnLoadMoreListener;
import com.uilib.swipetoloadlayout.OnRefreshListener;
import com.uilib.swipetoloadlayout.SwipeToLoadLayout;
import com.uilib.titlebar.TitleBar;
import com.uilib.utils.DisplayUtil;

import butterknife.BindView;

/**
 * Created by Mikiller on 2018/3/31.
 */

public abstract class BaseListActivity extends BaseActivity {
    @BindView(R.id.titleBar)
    protected TitleBar titleBar;
    @BindView(R.id.swipeLayout)
    protected SwipeToLoadLayout swipeLayout;
    protected RecyclerView swipe_target;

    protected int page = 0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
    }

    @Override
    protected void initView() {
        titleBar.setTitleListener(new TitleBar.TitleListener() {
            @Override
            protected void onBackClicked() {
                back();
            }
        });
        setTitle();

        swipe_target = (RecyclerView) swipeLayout.getTargetView();
        swipe_target.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        setAdapter();
        swipeLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRcvRefresh();
            }
        });
        swipeLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                onRcvLoadMore();
            }
        });
        ((LinearLayout.LayoutParams)titleBar.getLayoutParams()).setMargins(0, 0, 0, getTopMargin());
    }

    protected int getTopMargin(){
        return DisplayUtil.dip2px(this, 10);
    }

    protected abstract void setTitle();
    protected abstract void setAdapter();
    protected abstract void onRcvRefresh();
    protected abstract void onRcvLoadMore();

    @Override
    protected void initData() {
        swipeLayout.setRefreshing(true);
    }
}
