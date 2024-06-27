package com.example.ble;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.emoji.text.EmojiCompat;

public class EmojiDrawableHelper {
    public static Drawable getEmojiDrawable(overallChartFragment context, String emoji) {
        String processed = (String) EmojiCompat.get().process(emoji);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(50); // Set the desired font size
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);

        float textWidth = paint.measureText(processed.toString());
        float textHeight = paint.descent() - paint.ascent();

        Bitmap bitmap = Bitmap.createBitmap((int) textWidth, (int) textHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(processed, 0, -paint.ascent(), paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
