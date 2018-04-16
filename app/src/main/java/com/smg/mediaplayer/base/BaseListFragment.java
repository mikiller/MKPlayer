package com.smg.mediaplayer.base;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.smg.mediaplayer.R;
import com.uilib.swipetoloadlayout.OnLoadMoreListener;
import com.uilib.swipetoloadlayout.OnRefreshListener;
import com.uilib.swipetoloadlayout.SwipeToLoadLayout;

import butterknife.BindView;

/**
 * Created by Mikiller on 2018/4/1.
 */

public abstract class BaseListFragment extends BaseFragment {
    @BindView(R.id.swipeLayout)
    protected SwipeToLoadLayout swipeLayout;
    protected RecyclerView swipe_target;

    @Override
    protected void setLayoutRes() {
        layoutRes = R.layout.fragment_list;
    }

    @Override
    protected void initView() {
        swipe_target = (RecyclerView) swipeLayout.getTargetView();
        swipe_target.setLayoutManager(getRcvLayoutMgr());
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
    }

    protected RecyclerView.LayoutManager getRcvLayoutMgr(){
        return new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    }

    protected abstract void setAdapter();
    protected abstract void onRcvRefresh();
    protected abstract void onRcvLoadMore();

    @Override
    protected void initData() {
        swipeLayout.setRefreshing(true);
    }
}
