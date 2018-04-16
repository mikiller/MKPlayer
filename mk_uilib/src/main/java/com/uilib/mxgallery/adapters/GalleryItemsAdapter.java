package com.uilib.mxgallery.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikiller.mkglidelib.imageloader.GlideImageLoader;
import com.uilib.customdialog.CustomDialog;
import com.uilib.mxgallery.models.ItemModel;
import com.uilib.mxgallery.models.MimeType;
import com.uilib.mxgallery.utils.GalleryMediaUtils;
import com.uilib.mxgallery.widgets.MediaCollection;
import com.uilib.R;
import com.uilib.mxgallery.listeners.OnMediaItemClickListener;

/**
 * Created by Mikiller on 2017/5/11.
 */

public class GalleryItemsAdapter extends RecyclerViewCursorAdapter<GalleryItemsAdapter.MediaViewHolder> {

    private Context mContext;
    private int itemSize;
    private OnMediaItemClickListener listnener;
    private MediaCollection mediaCollection;
    private boolean needCkb = true;
    private int mimeType;

    public GalleryItemsAdapter(Context context, MediaCollection mediaCollection, int columnCount, float margin) {
        super(null);
        mContext = context;
        this.mediaCollection = mediaCollection;
        itemSize = (int) ((context.getResources().getDisplayMetrics().widthPixels - (columnCount+1) * margin) / columnCount);
    }

    public void setNeedCkb(boolean isNeed){
        needCkb = isNeed;
    }

    public void setMimeType(int mimeType){
        this.mimeType = mimeType;
    }

    public void setItemClickeListener(OnMediaItemClickListener listnener){
        this.listnener = listnener;
    }

    @Override
    public GalleryItemsAdapter.MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MediaViewHolder viewHolder = new MediaViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_gallery_item, parent, false));
        return viewHolder;
    }

    @Override
    protected void onBindViewHolder(MediaViewHolder holder, final Cursor cursor) {
        resetItemSize(holder.cv_img);
        if(needFirstItem && holder.getAdapterPosition() == 0){
            //相机按钮
            holder.setItemType(true);
            holder.ll_time.setVisibility(View.GONE);
            holder.cv_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(MimeType.isPic(mimeType))
                        GalleryMediaUtils.getInstance(mContext).openSysCamera("tmp_");
                    else if(MimeType.isVideo(mimeType))
                        GalleryMediaUtils.getInstance(mContext).openSysVideo("tmp_");
                    else{
                        showSelectDlg();
                    }
                }
            });

        }else {
            holder.setItemType(false);
            ItemModel model = ItemModel.valueOf(cursor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GlideImageLoader.getInstance().loadLocalImage(mContext, model.getContentUri(), R.mipmap.placeholder, new Size(itemSize, itemSize), holder.iv_img);
            } else {
                GlideImageLoader.getInstance().loadLocalImage(mContext, model.getContentUri(), new int[]{itemSize, itemSize}, R.mipmap.placeholder, holder.iv_img);
            }

            holder.tv_time.setText(DateUtils.formatElapsedTime(model.duration / 1000));
            holder.setMediaType(MimeType.isPic(model.mimeType));
            setItemStatus(holder, model);
        }
    }

    private void showSelectDlg(){
        final CustomDialog dlg = new CustomDialog(mContext);
        dlg.setLayoutRes(R.layout.layout_selection_dlg).setOnCustomBtnClickListener(new CustomDialog.onCustomBtnsClickListener() {
            @Override
            public void onBtnClick(int id) {
                if(id == R.id.btn_1)
                    GalleryMediaUtils.getInstance(mContext).openSysCamera("tmp_");
                else if(id == R.id.btn_2)
                    GalleryMediaUtils.getInstance(mContext).openSysVideo("tmp_");
                dlg.dismiss();
            }
        }, R.id.btn_1, R.id.btn_2, R.id.btn_3).setCustomBtnText("照片", "视频", "取消").setTitle("选择要拍摄的格式").show();
    }

    private void resetItemSize(View view){
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        lp.width = itemSize;
        view.setLayoutParams(lp);
    }

    private void setItemStatus(final GalleryItemsAdapter.MediaViewHolder holder, final ItemModel model){
        if(needCkb) {
            holder.ckb_isCheck.setEnabled(mediaCollection == null ? true : mediaCollection.canSelectModel(model));
            holder.ckb_isCheck.setOnCheckedChangeListener(null);
            holder.ckb_isCheck.setChecked(mediaCollection == null ? false : mediaCollection.isContainMedia(model));
        }else
            holder.ckb_isCheck.setVisibility(View.GONE);
        if(listnener != null){
            holder.ckb_isCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listnener.onItemChecked(model, isChecked);
                    notifyDataSetChanged();
                }
            });
            holder.iv_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //listnener.onItemClicked(model, holder.ckb_isCheck.isChecked());
                    //点击图片等同于点击选择框，也可以单独处理图片点击
                    holder.ckb_isCheck.setChecked(!holder.ckb_isCheck.isChecked());
                }
            });
        }

    }

    @Override
    protected int getItemViewType(int position, Cursor cursor) {
        if(!isDataValid(cursor))
            return 0;
        ItemModel item = ItemModel.valueOf(cursor);
        return (int) MimeType.getMimeTypeWithTypeName(item.mimeType).getMimeTypeId();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder{
        private CardView cv_img;
        private ImageView iv_camera, iv_img;
        private CheckBox ckb_isCheck;
        private LinearLayout ll_time;
        private TextView tv_time;

        public MediaViewHolder(View itemView) {
            super(itemView);
            cv_img = (CardView) itemView.findViewById(R.id.cv_img);
            iv_camera = (ImageView) itemView.findViewById(R.id.iv_camera);
            iv_img = (ImageView) itemView.findViewById(R.id.iv_img);
            iv_img.setAdjustViewBounds(true);
            ckb_isCheck = (CheckBox) itemView.findViewById(R.id.ckb_isCheck);
            ll_time = (LinearLayout) itemView.findViewById(R.id.ll_time);
            tv_time = (TextView) itemView.findViewById(R.id.tv_time);
        }

        public void setItemType(boolean isCamera){
            iv_camera.setVisibility(isCamera ? View.VISIBLE : View.GONE);
            iv_img.setVisibility(isCamera ? View.GONE : View.VISIBLE);
            ckb_isCheck.setVisibility(isCamera ? View.GONE : View.VISIBLE);
        }

        public void setMediaType(boolean isPic){
            ll_time.setVisibility(isPic ? View.GONE : View.VISIBLE);
        }
    }
}
