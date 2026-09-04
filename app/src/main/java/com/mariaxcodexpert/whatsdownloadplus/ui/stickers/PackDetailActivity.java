package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.mariaxcodexpert.whatsdownloadplus.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PackDetailActivity extends AppCompatActivity
        implements StickerAdapter.OnStickerClickListener {

    private String packName;

    private RecyclerView rvPackStickers;
    private StickerAdapter adapter;
    private List<String> stickerList = new ArrayList<>();

    private ActivityResultLauncher<Intent> galleryLauncher;

    private TextView tvPackName, tvAuthor, tvValidation;
    private ImageView imgPackThumb;
    private MaterialButton btnAddStickerToWhatsapp;

    private static final String TAG = "DEBUG_STICKER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pack_detail);

        packName = getIntent().getStringExtra("pack_name");

        if (packName == null || packName.isEmpty()) {
            Toast.makeText(this, "Invalid Pack", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvPackStickers = findViewById(R.id.rv_stickers);
        tvPackName = findViewById(R.id.tv_pack_name);
        tvAuthor = findViewById(R.id.tv_author);
        tvValidation = findViewById(R.id.tv_validation);
        imgPackThumb = findViewById(R.id.img_pack_thumb);
        btnAddStickerToWhatsapp = findViewById(R.id.btn_add_to_whatsapp);

        tvPackName.setText(packName);
        tvAuthor.setText("OmarSamy Creations");

        rvPackStickers.setLayoutManager(new GridLayoutManager(this, 4));

        adapter = new StickerAdapter(this, stickerList, this, packName);
        rvPackStickers.setAdapter(adapter);

        btnAddStickerToWhatsapp.setEnabled(true);
        btnAddStickerToWhatsapp.setAlpha(1f);

        loadStickersForPack();

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        openEditor(result.getData().getData());
                    }
                }
        );

        btnAddStickerToWhatsapp.setOnClickListener(v -> {

            loadStickersForPack();

            if (stickerList.size() < 3) {
                onAddStickerClick();
            } else {
                addPackToWhatsApp();
            }
        });
    }

    private void updateValidationUI(int count) {

        btnAddStickerToWhatsapp.setEnabled(true);

        if (count < 3) {
            int remaining = 3 - count;
            tvValidation.setVisibility(View.VISIBLE);
            tvValidation.setText("Add " + remaining + " more sticker(s)");
        } else {
            tvValidation.setVisibility(View.GONE);
        }
    }
    private void addPackToWhatsApp() {
        Log.e(TAG, "--- Starting addPackToWhatsApp ---");
        Log.e(TAG, "PackID: " + packName + " | Authority: com.mariaxcodexpert.whatsdownloadplus.stickercontentprovider");

        // JSON Creation
        createContentsJson(stickerList);
        Log.e(TAG, "JSON content generated successfully.");

        Intent intent = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra("sticker_pack_id", packName);
        intent.putExtra("sticker_pack_authority", "com.mariaxcodexpert.whatsdownloadplus.stickercontentprovider");
        intent.putExtra("sticker_pack_name", packName);
        intent.putExtra("sticker_pack_publisher", "OmarSamy Creations");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.e(TAG, "Intent extras populated and flags set.");

        try {
            Log.e(TAG, "Attempting to start: com.whatsapp");
            intent.setPackage("com.whatsapp");
            startActivity(intent);
            Log.e(TAG, "WhatsApp (Normal) startActivity triggered successfully.");
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp (Normal) failed: " + e.getMessage());

            try {
                Log.e(TAG, "Attempting to start: com.whatsapp.w4b");
                intent.setPackage("com.whatsapp.w4b");
                startActivity(intent);
                Log.e(TAG, "WhatsApp Business startActivity triggered successfully.");
            } catch (Exception e2) {
                Log.e(TAG, "WhatsApp Business failed: " + e2.getMessage());
                Toast.makeText(this, "WhatsApp not found!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Both WhatsApp attempts failed.");
            }
        }
    }
    @Override
    public void onAddStickerClick() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onStickerClick(String path) {
        adapter.showStickerOptions(path);
    }

    private void openEditor(Uri uri) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("image_uri", uri.toString());
        intent.putExtra("target_pack", packName);
        startActivity(intent);
    }

    private void loadStickersForPack() {

        File packDir = new File(getFilesDir(),
                "my_stickers/" + packName);

        stickerList.clear();

        if (packDir.exists()) {

            File[] files = packDir.listFiles((dir, name) ->
                    name.endsWith(".webp") &&
                            !name.equals("tray.webp")
            );

            if (files != null) {

                for (File f : files) {
                    stickerList.add(f.getAbsolutePath());
                }

                if (!stickerList.isEmpty()) {
                    Glide.with(this)
                            .load(stickerList.get(0))
                            .into(imgPackThumb);
                }
            }
        }

        adapter.updateList(stickerList);
        updateValidationUI(stickerList.size());
    }

    private void createContentsJson(List<String> stickers) {
        if (stickers == null || stickers.size() < 3) {
            Toast.makeText(this, "Need at least 3 stickers!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File packDir = new File(getFilesDir(), "my_stickers/" + packName);
            File jsonFile = new File(packDir, "contents.json");

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("\"android_play_store_link\": null,\n");
            sb.append("\"ios_app_store_link\": null,\n");
            sb.append("\"sticker_pack_identifier\": \"").append(packName).append("\",\n");
            sb.append("\"sticker_pack_name\": \"").append(packName).append("\",\n");
            sb.append("\"sticker_pack_publisher\": \"OmarSamy Creations\",\n");
            sb.append("\"tray_image_file\": \"tray.webp\",\n");
            sb.append("\"stickers\": [\n");

            for (int i = 0; i < stickers.size(); i++) {
                File f = new File(stickers.get(i));
                sb.append("{\"image_file\": \"").append(f.getName()).append("\", \"emojis\": [\"😀\"]}");
                if (i < stickers.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]\n}");

            FileOutputStream fos = new FileOutputStream(jsonFile);
            fos.write(sb.toString().getBytes());
            fos.flush();
            fos.close();
            Log.e(TAG, "JSON saved successfully!");
        } catch (Exception e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
        }
    }
}