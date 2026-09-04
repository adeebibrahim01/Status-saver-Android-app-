package com.mariaxcodexpert.whatsdownloadplus.ui.stickers;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class StickerTextView extends View {
    private String text = "Sample Text";
    private float posX = 200, posY = 200;
    private float scaleFactor = 1f;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ScaleGestureDetector scaleDetector;

    public StickerTextView(Context c, AttributeSet a) {
        super(c, a);
        paint.setColor(Color.WHITE);
        paint.setTextSize(100f);
        paint.setTextAlign(Paint.Align.CENTER);
        scaleDetector = new ScaleGestureDetector(c, new ScaleListener());
    }

    public void setText(String t) { this.text = t; invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(posX, posY);
        canvas.scale(scaleFactor, scaleFactor);
        canvas.drawText(text, 0, 0, paint);
        canvas.restore();
    }
    // StickerTextView.java mein ye add karein
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.getVisibility() == View.VISIBLE) {
            canvas.save();
            canvas.translate(posX, posY);
            canvas.scale(scaleFactor, scaleFactor);
            canvas.drawText(text, 0, 0, paint);
            canvas.restore();
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
  if (event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    posX = event.getX();
                    posY = event.getY();
                    invalidate();
                    break;
            }
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }
}