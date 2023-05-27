package com.marlerino.rainbowgoldstation.viewmodel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marlerino.rainbowgoldstation.R;

import java.util.List;

public class DeskColumnAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Integer> itemList;

    public DeskColumnAdapter(List<Integer> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        populateItemRows((ItemViewHolder) holder, position);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.image_view);
        }
    }

    private void populateItemRows(ItemViewHolder viewHolder, int position) {
        int item = itemList.get(position);
        viewHolder.imageView.setImageResource(item);
    }
}