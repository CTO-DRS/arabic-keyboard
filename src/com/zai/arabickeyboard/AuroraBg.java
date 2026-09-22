package com.zai.arabickeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * الخلفية الشفقية الزجاجية — DRS Smart v2.7
 * قاعدة كحلية داكنة مع هالات بنفسجية ناعمة (Aurora Blobs)
 * تمنح بطاقات الزجاج شفافية حقيقية مرئية خلفها.
 * على Android 12+ تُمرَّر البقع عبر Blur حقيقي (RenderEffect).
 */
public class AuroraBg extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean blurApplied;

    public AuroraBg(Context c) {
        super(c);
        applyBlur();
    }

    private void applyBlur() {
        if (blurApplied || Build.VERSION.SDK_INT < 31) return;
        try {
            setRenderEffect(RenderEffect.createBlurEffect(48f, 48f,
                    Shader.TileMode.CLAMP));
            blurApplied = true;
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        applyBlur();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // ===== القاعدة الكحلية المتدرجة =====
        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF0A0E24, 0xFF0B1128, 0xFF070B18},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);

        // ===== الهالات البنفسجية (Aurora) =====
        blob(canvas, w * 0.18f, h * 0.02f, w * 0.55f, 0x5A7C5CFF); // توهج علوي أيسر
        blob(canvas, w * 0.95f, -h * 0.04f, w * 0.42f, 0x3DB388FF); // توهج علوي أيمن
        blob(canvas, w * 0.50f, h * 0.30f, w * 0.62f, 0x2E2B1B67); // غاز منتصف
        blob(canvas, w * 0.88f, h * 0.86f, w * 0.45f, 0x265B3FE0); // هالة سفلية
        blob(canvas, w * 0.06f, h * 0.72f, w * 0.38f, 0x1E6C3BFF); // هالة سفلية يسار
        p.setShader(null);
    }

    /** هالة دائرية شعاعية تتلاشى للشفافية */
    private void blob(Canvas canvas, float cx, float cy, float r, int color) {
        p.setShader(new RadialGradient(cx, cy, r,
                new int[]{color, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, p);
    }
}
