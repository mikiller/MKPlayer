/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uilib.mxgallery.adapters;

import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;

public abstract class RecyclerViewCursorAdapter<VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> {

    private Cursor mCursor;
    private int mRowIDColumn;
    protected boolean needFirstItem = false;

    RecyclerViewCursorAdapter(Cursor c) {
        setHasStableIds(true);
        swapCursor(c);
    }

    public void setNeedFirstItem(boolean isNeed){
        needFirstItem = isNeed;
    }

    protected abstract void onBindViewHolder(VH holder, Cursor cursor);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if(!needFirstItem)
            checkCursor(holder.getAdapterPosition());
//        if (!isDataValid(mCursor)) {
//            throw new IllegalStateException("Cannot bind view holder when cursor is in invalid state.");
//        }
//        if (!mCursor.moveToPosition(position)) {
//            throw new IllegalStateException("Could not move cursor to position " + position
//                    + " when trying to bind view holder");
//        }

        onBindViewHolder(holder, mCursor);
    }

    @Override
    public int getItemViewType(int position) {
//        if (!mCursor.moveToPosition(position)) {
//            throw new IllegalStateException("Could not move cursor to position " + position
//                    + " when trying to get item view type.");
//        }
        if(!needFirstItem || position != 0) {
            checkCursor(position);
            return getItemViewType(position, mCursor);
        }else
            return getItemViewType(position, null);
    }

    protected abstract int getItemViewType(int position, Cursor cursor);

    @Override
    public int getItemCount() {
        int count = needFirstItem ? 1 : 0;
        if (isDataValid(mCursor)) {
            return mCursor.getCount() + count;
        } else {
            return count;
        }
    }

    @Override
    public long getItemId(int position) {
        if(needFirstItem && position == 0)
            return -1;
        checkCursor(position);

        return mCursor.getLong(mRowIDColumn);
    }

    private void checkCursor(int position){
        int pos = needFirstItem ? 1 : 0;
        if (!isDataValid(mCursor)) {
            throw new IllegalStateException("Cannot lookup item id when cursor is in invalid state.");
        }
        if (!mCursor.moveToPosition(position - pos)) {
            throw new IllegalStateException("Could not move cursor to position " + position
                    + " when trying to get an item id");
        }
    }

    public void swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return;
        }

        if (newCursor != null) {
            mCursor = newCursor;
            mRowIDColumn = mCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            // notify the observers about the new cursor
            notifyDataSetChanged();
        } else {
            notifyItemRangeRemoved(0, getItemCount());
            mCursor = null;
            mRowIDColumn = -1;
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    protected boolean isDataValid(Cursor cursor) {
        return cursor != null && !cursor.isClosed();
    }
}
