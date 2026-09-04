package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.util.List;

public class PackAdapter extends RecyclerView.Adapter<PackAdapter.PackViewHolder> {

    private Context context;
    private List<StickerPack> packList;
    private OnPackClickListener listener;

    private static final String TAG = "PACK_ADAPTER";

    public interface OnPackClickListener {
        void onPackClick(StickerPack pack);

        void onAddClick(String packName);
    }

    public PackAdapter(Context context, List<StickerPack> packList, OnPackClickListener listener) {
        this.context = context;
        this.packList = packList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_sticker_pack, parent, false);
        return new PackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackViewHolder holder, int position) {

        StickerPack pack = packList.get(position);

        holder.tvPackName.setText(pack.packName);

        File packDir = new File(
                context.getFilesDir(),
                "my_stickers/" + pack.folderPath
        );

        File[] files = packDir.listFiles((dir, name) ->
                name.endsWith(".webp") &&
                        !name.equals("tray.webp")
        );

        if (files != null && files.length > 0) {
            Glide.with(context)
                    .load(files[0])
                    .into(holder.imgSticker);
        } else {
            holder.imgSticker.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPackClick(pack);
            }
        });

        holder.btnAddToWhatsApp.setOnClickListener(v -> {

            File dir = new File(
                    context.getFilesDir(),
                    "my_stickers/" + pack.folderPath
            );

            File[] stickerFiles = dir.listFiles((d, name) ->
                    name.endsWith(".webp") &&
                            !name.equals("tray.webp")
            );

            if (stickerFiles == null || stickerFiles.length < 3) {
                Toast.makeText(
                        context,
                        "At least 3 stickers required!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Log.e(TAG, "Opening WhatsApp pack: " + pack.folderPath);

            Intent intent =
                    new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");

            intent.setPackage("com.whatsapp");

            intent.putExtra("sticker_pack_id", pack.folderPath);
            intent.putExtra("sticker_pack_name", pack.packName);
            intent.putExtra("sticker_pack_publisher", "OmarSamy Creations");
            intent.putExtra(
                    "sticker_pack_authority",
                    StickerContentProvider.AUTHORITY
            );

            try {
                context.startActivity(intent);
            } catch (Exception e) {

                Log.e(TAG, "WhatsApp error: " + e.getMessage());

                Toast.makeText(
                        context,
                        "WhatsApp not installed",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return packList.size();
    }

    static class PackViewHolder extends RecyclerView.ViewHolder {

        TextView tvPackName;
        Button btnAddToWhatsApp;
        ImageView imgSticker;

        PackViewHolder(View v) {
            super(v);

            tvPackName = v.findViewById(R.id.tv_pack_name);
            btnAddToWhatsApp = v.findViewById(R.id.btn_add_to_whatsapp);
            imgSticker = v.findViewById(R.id.img_sticker_1);
        }
    }
}