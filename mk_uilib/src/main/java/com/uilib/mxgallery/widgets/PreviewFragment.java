package com.uilib.mxgallery.widgets;

import android.content.ClipData;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.uilib.mxgallery.models.ItemModel;
import com.uilib.R;
import com.uilib.mxgallery.adapters.PreviewPagerAdapter;
import com.uilib.mxgallery.listeners.OnBottomBtnClickListener;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Mikiller on 2017/5/16.
 */

public class PreviewFragment extends Fragment {
    private final static String MODELS = "models";
    private final static String MODEL = "model";
    private final static String POS = "currentPos";
    private final static String ISSELECTED = "isSelected";
    private final static String MIMETYPE = "mimeType";
    private ViewPager vp_preview;
    private BottomBar bottomBar;
    private PreviewPagerAdapter adapter;
    private boolean isSelected;
    private OnSureListener listener;

    /**创建预览页
     * 预览页分为预览被选中的素材和预览未选中的素材
     * 预览选中素材时，预览整个集合，并跳转至点击的素材
     * 预览未选中的素材时，只预览单个素材，并且操作栏提供“选择”功能，将该素材添加进集合
     * @param collection 选中的素材集合
     * @param model 点击的某个素材，仅item响应单独点击事件时生效
     * @param isSelected 该素材是否被选中
     * */
    public static PreviewFragment newInstance(MediaCollection collection, ItemModel model, boolean isSelected){
        PreviewFragment fragment = new PreviewFragment();
        Bundle bundle = new Bundle();
        if(isSelected){
            bundle.putParcelableArrayList(MODELS, new ArrayList<ItemModel>(collection.modelSet));
            bundle.putInt(POS, collection.getMediaPos(model));
        }else{
            bundle.putParcelable(MODEL, model);
            bundle.putInt(POS, 0);
        }
        bundle.putBoolean(ISSELECTED, isSelected);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setOnSureListener(OnSureListener listener){
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vp_preview = (ViewPager) view.findViewById(R.id.vp_preview);
        bottomBar = (BottomBar) view.findViewById(R.id.bottomBar);
        isSelected = getArguments().getBoolean(ISSELECTED);
        final ArrayList<ItemModel> models = new ArrayList<>();

        if(!isSelected) {
            //预览未选中素材，单个预览
            models.add((ItemModel) getArguments().getParcelable(MODEL));
            bottomBar.setRightText("选择");
        }else{
            //预览选种素材，预览整个集合
            models.addAll((ArrayList)(getArguments().getParcelableArrayList(MODELS)));
            bottomBar.updateNum(models.size());
        }
        bottomBar.updateBtnState(true);
        bottomBar.needNum(isSelected);
        bottomBar.setBtnListener(new OnBottomBtnClickListener() {
            @Override
            public void onLeftClick() {
                getActivity().getSupportFragmentManager().beginTransaction().remove(PreviewFragment.this).commit();
                if(listener != null)
                    listener.onBack();
            }

            @Override
            public void onRightClick() {
                if(listener != null){
                    if (!isSelected){
                        listener.addNewMedia(models.get(0));
                    }else{
                        listener.confirmSelectedMedia();
                    }
                }
                getActivity().getSupportFragmentManager().beginTransaction().remove(PreviewFragment.this).commit();
            }
        });
        adapter = new PreviewPagerAdapter(getContext(), models);
        vp_preview.setAdapter(adapter);

        vp_preview.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                adapter.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        vp_preview.setCurrentItem(getArguments().getInt(POS));
    }

    @Override
    public void onDestroyView() {
        adapter.release();
        super.onDestroyView();
    }

    public interface OnSureListener{
        void onBack();
        void addNewMedia(ItemModel model);
        void confirmSelectedMedia();
    }
}
