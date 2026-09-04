package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.canhub.cropper.CropImageView;
import com.mariaxcodexpert.whatsdownloadplus.R;

public class CropActivity extends AppCompatActivity {

    private CropImageView cropImageView;
    private ImageView btnDone;
    private String targetPack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        cropImageView = findViewById(R.id.cropImageView);
        btnDone = findViewById(R.id.btn_done);

        String uriString = getIntent().getStringExtra("image_uri");
        targetPack = getIntent().getStringExtra("target_pack");

        if (uriString != null) {
            cropImageView.setImageUriAsync(Uri.parse(uriString));
        }

        cropImageView.setFixedAspectRatio(false);
        cropImageView.setGuidelines(CropImageView.Guidelines.ON);

        btnDone.setOnClickListener(v -> {
            cropImageView.croppedImageAsync(
                    android.graphics.Bitmap.CompressFormat.WEBP,
                    100,
                    0,
                    0,
                    com.canhub.cropper.CropImageView.RequestSizeOptions.RESIZE_INSIDE,
                    null
            );
        });

        cropImageView.setOnCropImageCompleteListener((view, result) -> {
            if (result.isSuccessful()) {
                Uri croppedUri = result.getUriContent();
                Intent intent = new Intent();
                intent.putExtra("cropped_uri", croppedUri.toString());
                intent.putExtra("target_pack", targetPack);

                setResult(RESULT_OK, intent);
                finish();
            } else {
                Exception e = result.getError();
                if (e != null) e.printStackTrace();
            }
        });
    }
}