package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditorActivity extends AppCompatActivity {

    private ImageView imgEditor;
    private ImageView btnCancel, btnDone;
    private TextView btnClip, btnText, btnCutout;
    private StickerTextView stickerTextView;
    private DrawingView drawingView;
    private Uri currentImageUri;
    private String targetPack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);


        targetPack = getIntent().getStringExtra("target_pack");
        if (targetPack == null || targetPack.isEmpty()) {
            Toast.makeText(this, "Error: No pack selected!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        imgEditor = findViewById(R.id.img_editor);
        drawingView = findViewById(R.id.drawing_view);
        stickerTextView = findViewById(R.id.sticker_text_view);
        btnCancel = findViewById(R.id.btn_cancel);
        btnDone = findViewById(R.id.btn_done);
        btnClip = findViewById(R.id.btn_clip);
        btnText = findViewById(R.id.btn_text);
        btnCutout = findViewById(R.id.btn_cutout);

        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            if (uriString.startsWith("/")) {
                currentImageUri = Uri.fromFile(new File(uriString));
            } else {
                currentImageUri = Uri.parse(uriString);
            }
            Glide.with(this).load(currentImageUri).into(imgEditor);
        }

        btnCancel.setOnClickListener(v -> finish());

        btnCutout.setOnClickListener(v -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(currentImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                drawingView.setBitmap(bitmap);
                drawingView.setVisibility(View.VISIBLE);
                imgEditor.setVisibility(View.GONE);
                Toast.makeText(this, "Draw to Cutout", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image for cutout", Toast.LENGTH_SHORT).show();
            }
        });

        btnText.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Text");
            final EditText input = new EditText(this);
            builder.setView(input);
            builder.setPositiveButton("Add", (dialog, which) -> {
                stickerTextView.setText(input.getText().toString());
                stickerTextView.setVisibility(View.VISIBLE);
            });
            builder.show();
        });

        btnClip.setOnClickListener(v -> {
            Intent intent = new Intent(this, CropActivity.class);
            intent.putExtra("image_uri", currentImageUri.toString());
            intent.putExtra("target_pack", targetPack);
            startActivityForResult(intent, 101);
        });
        btnDone.setOnClickListener(v -> {
            if (currentImageUri == null) return;
            try {
                File packDir = new File(new File(getFilesDir(), "my_stickers"), targetPack);
                if (!packDir.exists()) {
                    boolean created = packDir.mkdirs();
                    Log.e("DEBUG_STICKER", "Directory created: " + created + " at: " + packDir.getAbsolutePath());
                }

                if (drawingView.getVisibility() == View.VISIBLE || stickerTextView.getVisibility() == View.VISIBLE) {
                    Bitmap workingBitmap;
                    if (drawingView.getVisibility() == View.VISIBLE) {
                        workingBitmap = trimBitmap(drawingView.getCutoutBitmap());
                        drawingView.setVisibility(View.GONE);
                        drawingView.clear();
                    } else {
                        try (InputStream is = getContentResolver().openInputStream(currentImageUri)) {
                            workingBitmap = BitmapFactory.decodeStream(is).copy(Bitmap.Config.ARGB_8888, true);
                        }
                    }

                    if (stickerTextView.getVisibility() == View.VISIBLE) {
                        Canvas canvas = new Canvas(workingBitmap);
                        float scaleX = (float) workingBitmap.getWidth() / stickerTextView.getWidth();
                        float scaleY = (float) workingBitmap.getHeight() / stickerTextView.getHeight();
                        canvas.save();
                        canvas.scale(scaleX, scaleY);
                        stickerTextView.draw(canvas);
                        canvas.restore();
                        stickerTextView.setVisibility(View.GONE);
                    }

                    imgEditor.setImageBitmap(workingBitmap);
                    File previewFile = new File(packDir, "preview_temp.png");
                    try (FileOutputStream fos = new FileOutputStream(previewFile)) {
                        workingBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                    currentImageUri = Uri.fromFile(previewFile);
                    Toast.makeText(this, "Applied! Click Done again to Save.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fileName = "sticker_" + System.currentTimeMillis() + ".webp";
                File stickerFile = new File(packDir, fileName);
                Log.e("DEBUG_STICKER", "Attempting to save .webp at: " + stickerFile.getAbsolutePath());

                try (InputStream inputStream = getContentResolver().openInputStream(currentImageUri)) {
                    Bitmap finalBitmap = BitmapFactory.decodeStream(inputStream);

                    if (finalBitmap == null) {
                        Log.e("DEBUG_STICKER", "Bitmap is NULL! URI: " + currentImageUri);
                        Toast.makeText(this, "Error: Bitmap is null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try (FileOutputStream out = new FileOutputStream(stickerFile)) {
                        finalBitmap.compress(Bitmap.CompressFormat.WEBP, 100, out);
                        out.flush();
                    }
                    Log.e("DEBUG_STICKER", "SUCCESS: File saved at " + stickerFile.getAbsolutePath());
                }

                File previewFile = new File(packDir, "preview_temp.png");
                if (previewFile.exists()) previewFile.delete();

                File trayFile = new File(packDir, "tray.webp");
                if (!trayFile.exists()) {
                    try (FileOutputStream trayOut = new FileOutputStream(trayFile);
                         InputStream is = getContentResolver().openInputStream(Uri.fromFile(stickerFile))) {
                        Bitmap trayBitmap = BitmapFactory.decodeStream(is);
                        Bitmap.createScaledBitmap(trayBitmap, 96, 96, true).compress(Bitmap.CompressFormat.WEBP, 100, trayOut);
                        Log.e("DEBUG_STICKER", "Tray file created successfully.");
                    }
                }

                Toast.makeText(this, "Saved at: " + fileName, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } catch (Exception e) {
                Log.e("DEBUG_STICKER", "CRITICAL ERROR: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            String croppedUri = data.getStringExtra("cropped_uri");
            this.targetPack = data.getStringExtra("target_pack"); // Update pack name if changed
            currentImageUri = Uri.parse(croppedUri);
            Glide.with(this).load(currentImageUri).into(imgEditor);
        }
    }
    private Bitmap trimBitmap(Bitmap source) {
        int firstX = 0, firstY = 0;
        int lastX = source.getWidth();
        int lastY = source.getHeight();

        int[] pixels = new int[source.getWidth() * source.getHeight()];
        source.getPixels(pixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());

        loop: for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (pixels[y * source.getWidth() + x] != 0) {
                    firstY = y;
                    break loop;
                }
            }
        }
        return Bitmap.createBitmap(source, 0, firstY, source.getWidth(), source.getHeight() - firstY);
    }
}