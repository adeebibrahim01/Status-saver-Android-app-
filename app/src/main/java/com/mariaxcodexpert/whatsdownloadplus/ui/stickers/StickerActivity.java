package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StickerActivity extends AppCompatActivity implements PackAdapter.OnPackClickListener {

    private RecyclerView rvStickerPacks;
    private MaterialButton btnCreatePack;
    private PackAdapter packAdapter;
    private List<StickerPack> packList = new ArrayList<>();
    private static final String TAG = "DEBUG_STICKER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stickers);

        rvStickerPacks = findViewById(R.id.rv_sticker_packs);
        btnCreatePack = findViewById(R.id.btn_create_sticker);

        rvStickerPacks.setLayoutManager(new LinearLayoutManager(this));
        packAdapter = new PackAdapter(this, packList, this);
        rvStickerPacks.setAdapter(packAdapter);

        btnCreatePack.setOnClickListener(v -> showCreatePackDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPacks();
    }

    private void loadPacks() {
        File rootDir = new File(getFilesDir(), "my_stickers");
        if (!rootDir.exists()) rootDir.mkdirs();

        packList.clear();
        File[] folders = rootDir.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                if (!folder.getName().equals("default_pack")) {
                    packList.add(new StickerPack(folder.getName(), folder.getName()));
                }
            }
        }
        packAdapter.notifyDataSetChanged();
    }

    private void showCreatePackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Pack");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                File dir = new File(new File(getFilesDir(), "my_stickers"), name);
                if (dir.mkdirs()) {
                    Toast.makeText(this, "Pack Created", Toast.LENGTH_SHORT).show();
                    loadPacks();
                } else Toast.makeText(this, "Already exists!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onPackClick(StickerPack pack) {
        Intent intent = new Intent(this, PackDetailActivity.class);
        intent.putExtra("pack_name", pack.packName);
        startActivity(intent);
    }

    @Override
    public void onAddClick(String packName) {
        Log.e(TAG, "Add button clicked for: " + packName);
        Intent intent = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.setPackage("com.whatsapp");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("sticker_pack_id", packName);
        intent.putExtra("sticker_pack_authority", StickerContentProvider.AUTHORITY);
        intent.putExtra("sticker_pack_name", packName);
        intent.putExtra("sticker_pack_publisher", "OmarSamy Creations");

        try {
            startActivity(intent);
        } catch (Exception e) {
            try {
                intent.setPackage("com.whatsapp.w4b");
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}