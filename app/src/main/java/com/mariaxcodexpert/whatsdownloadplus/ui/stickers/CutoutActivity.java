package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.mariaxcodexpert.whatsdownloadplus.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CutoutActivity extends AppCompatActivity {
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cutout);

        drawingView = findViewById(R.id.drawing_view);
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString == null) return;

        Uri uri = Uri.parse(uriString);

        try {
            Bitmap bm = loadBitmapFromUri(uri);
            drawingView.setBitmap(bm);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image!", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.btn_done).setOnClickListener(v -> {
            Bitmap result = createCutout(drawingView.getBitmap(), drawingView.getPath());
            if (result != null) {
                try {
                    File tempFile = File.createTempFile("cutout", ".png", getCacheDir());
                    FileOutputStream out = new FileOutputStream(tempFile);
                    result.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();

                    Intent intent = new Intent();
                    intent.putExtra("cutout_uri", Uri.fromFile(tempFile).toString());
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, "Error returning image", Toast.LENGTH_SHORT).show();
                }
            } else {
                finish();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, s) -> decoder.setMutableRequired(true));
        } else {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri).copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    private Bitmap createCutout(Bitmap source, Path path) {
        if (source == null) return null;
        if (path.isEmpty()) return source;

        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        canvas.drawPath(path, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);

        return result;
    }

    private void saveAndFinish(Bitmap finalBitmap) {
        try {
            File stickerDir = new File(getFilesDir(), "my_stickers");
            if (!stickerDir.exists()) stickerDir.mkdirs();

            File file = new File(stickerDir, "sticker_" + System.currentTimeMillis() + ".webp");
            FileOutputStream out = new FileOutputStream(file);

            finalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out);
            out.flush();
            out.close();

            Toast.makeText(this, "Sticker Saved!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}