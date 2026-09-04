package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

public class DrawingView extends View {
    private Path path = new Path();
    private Paint paint = new Paint();
    private Bitmap sourceBitmap;
    private RectF imageRect = new RectF();

    public DrawingView(Context c, AttributeSet a) {
        super(c, a);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.WHITE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setBitmap(Bitmap b) {
        this.sourceBitmap = b;
        invalidate();
    }

    public Bitmap getBitmap() {
        return this.sourceBitmap;
    }

    public Path getPath() {
        return this.path;
    }
    public Bitmap getCutoutBitmap() {
        if (sourceBitmap == null) return null;

        android.graphics.RectF bounds = new android.graphics.RectF();
        path.computeBounds(bounds, true);
        Matrix shiftMatrix = new Matrix();
        shiftMatrix.postTranslate(-imageRect.left, -imageRect.top);

        float scaleX = (float) sourceBitmap.getWidth() / imageRect.width();
        float scaleY = (float) sourceBitmap.getHeight() / imageRect.height();

        shiftMatrix.postScale(scaleX, scaleY);

        Path finalPath = new Path();
        path.transform(shiftMatrix, finalPath);

        android.graphics.RectF finalBounds = new android.graphics.RectF();
        finalPath.computeBounds(finalBounds, true);
        int padding = 20;
        int left = Math.max(0, (int) (finalBounds.left - padding));
        int top = Math.max(0, (int) (finalBounds.top - padding));
        int width = Math.min((int) (finalBounds.width() + (padding * 2)), sourceBitmap.getWidth() - left);
        int height = Math.min((int) (finalBounds.height() + (padding * 2)), sourceBitmap.getHeight() - top);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Matrix finalMatrix = new Matrix();
        finalMatrix.postTranslate(-left, -top);
        finalPath.transform(finalMatrix);

        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(Color.BLACK);
        canvas.drawPath(finalPath, maskPaint);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect srcRect = new Rect(left, top, left + width, top + height);
        Rect destRect = new Rect(0, 0, width, height);
        canvas.drawBitmap(sourceBitmap, srcRect, destRect, maskPaint);

        return result;
    }
    public void clear() {
        path.reset();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (sourceBitmap != null) {
            float viewRatio = (float) w / h;
            float imgRatio = (float) sourceBitmap.getWidth() / sourceBitmap.getHeight();
            float scale = (imgRatio > viewRatio) ? (float) w / sourceBitmap.getWidth() : (float) h / sourceBitmap.getHeight();
            float width = scale * sourceBitmap.getWidth();
            float height = scale * sourceBitmap.getHeight();
            imageRect.set((w - width) / 2f, (h - height) / 2f, (w + width) / 2f, (h + height) / 2f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (sourceBitmap == null) return;
        canvas.drawBitmap(sourceBitmap, null, imageRect, null);
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(0x80000000);
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                path.close();
                break;
        }
        invalidate();
        return true;
    }
}