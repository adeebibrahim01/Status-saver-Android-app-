package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.io.File;
import java.util.List;

public class StickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<String> stickerPaths;
    private Context context;
    private OnStickerClickListener listener;
    private String packName;

    public interface OnStickerClickListener {
        void onAddStickerClick();
        void onStickerClick(String path);
    }

    public StickerAdapter(Context context, List<String> paths, OnStickerClickListener listener, String packName) {
        this.context = context;
        this.stickerPaths = paths;
        this.listener = listener;
        this.packName = packName;
    }

    public void updateList(List<String> newPaths) {
        this.stickerPaths = newPaths;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StickerViewHolder vh = (StickerViewHolder) holder;

        // Position 0 = Add Button, Baaki = Stickers
        if (position == 0) {
            vh.imgSticker.setImageResource(R.drawable.ic_add_sticker);
            vh.imgSticker.setOnClickListener(v -> listener.onAddStickerClick());
        } else {
            String path = stickerPaths.get(position - 1);
            Glide.with(context).load(new File(path)).into(vh.imgSticker);
            vh.imgSticker.setOnClickListener(v -> listener.onStickerClick(path));
        }
    }

    @Override
    public int getItemCount() {
        return stickerPaths.size() + 1;
    }

    // Bottom Sheet Method
    public void showStickerOptions(String path) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_sticker_options, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageView imgPreview = sheetView.findViewById(R.id.img_preview);
        View btnEdit = sheetView.findViewById(R.id.btn_edit);

        Glide.with(context).load(new File(path)).into(imgPreview);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditorActivity.class);
            intent.putExtra("image_uri", path);
            intent.putExtra("target_pack", packName);
            context.startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;
        StickerViewHolder(View v) {
            super(v);
            imgSticker = v.findViewById(R.id.img_sticker);
        }
    }
}