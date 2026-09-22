package com.zai.arabickeyboard;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * حافة زجاجية مضيئة — DRS Smart v2.8
 * حواف الشريط العلوي والسفلي بمعالجة زجاجية كاملة بدل الحد الأحادي المسطح:
 *
 *  ١) توهج خارجي ناعم: ثلاث طبقات متراكبة تتسع وتخفت حول الحافة (Glow Halo)
 *  ٢) تعبئة زجاجية متدرجة عمودياً (أثقل أعلى، أخف أسفل)
 *  ٣) حد متدرج الإضاءة: ساطع عند الحافة العلوية يتلاشى نحو السفلية —
 *     كأن ضوءاً يسقط على الزجاج من الأعلى (Lit Edge)
 *  ٤) خط انعكاس داخلي رفيع يلمع أسفل الحافة العلوية ثم يتلاشى (Inner Shine)
 *
 * كل الرسم Canvas خالص — يعمل من API 21 بدون مكتبات خارجية،
 * ويُعاد بناء المسارات والظلال تلقائياً عند تغيّر الحدود.
 */
public final class GlassEdge extends Drawable {

    private final float radius;        // نصف قطر الزوايا الموحد px
    private final float strokeW;       // سماكة الحد الأساسية px
    private final int fillTop, fillBottom;          // تعبئة الزجاج
    private final int edgeTop, edgeMid, edgeBottom; // الحد المتدرج
    private final int glowColor;                    // لون التوهج الخارجي
    private final float glowStrength;               // 0..1 شدة التوهج

    private final Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shineP = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path glowP1 = new Path();
    private final Path glowP2 = new Path();
    private final Path glowP3 = new Path();
    private final Path fillPath = new Path();
    private final Path edgePath = new Path();
    private final Path shinePath = new Path();
    private final RectF rf = new RectF();
    private boolean built;

    public GlassEdge(float radiusPx,
                     int fillTopColor, int fillBottomColor,
                     int edgeTopColor, int edgeMidColor, int edgeBottomColor,
                     int glowColorInt, float glowStrength01, float strokeWidthPx) {
        radius = radiusPx;
        strokeW = Math.max(1f, strokeWidthPx);
        fillTop = fillTopColor;
        fillBottom = fillBottomColor;
        edgeTop = edgeTopColor;
        edgeMid = edgeMidColor;
        edgeBottom = edgeBottomColor;
        glowColor = glowColorInt;
        glowStrength = Math.max(0f, Math.min(1f, glowStrength01));

        glowP.setStyle(Paint.Style.STROKE);
        fillP.setStyle(Paint.Style.FILL);
        edgeP.setStyle(Paint.Style.STROKE);
        edgeP.setStrokeWidth(strokeW);
        shineP.setStyle(Paint.Style.STROKE);
        shineP.setStrokeWidth(Math.max(1f, strokeW * 0.7f));
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        built = false;
    }

    /** بناء المسارات والظلال وفق الحدود الفعلية */
    private void build() {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) return;

        // مسار الحد والتعبئة — منسحب نصف سماكة الحد ليبقى داخل الحدود
        float half = strokeW * 0.5f;
        rf.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
        fillPath.reset();
        fillPath.addRoundRect(rf, radius, radius, Path.Direction.CW);
        edgePath.set(fillPath);

        // طبقات التوهج: كل طبقة حلقة أوسع وأخفت من سابقتها
        ring(glowP3, strokeW * 7.0f);
        ring(glowP2, strokeW * 4.2f);
        ring(glowP1, strokeW * 2.0f);

        // خط الانعكاس الداخلي — أسفل الحافة العلوية مباشرة
        float in = strokeW * 1.7f;
        rf.set(b.left + in, b.top + in, b.right - in, b.bottom - in);
        shinePath.reset();
        shinePath.addRoundRect(rf, radius, radius, Path.Direction.CW);

        // ظلال الرسم
        fillP.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{fillTop, fillBottom}, new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
        edgeP.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{edgeTop, edgeMid, edgeBottom},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        shineP.setShader(new LinearGradient(0, b.top, 0,
                b.top + b.height() * 0.42f,
                new int[]{0x5CFFFFFF, 0x00FFFFFF}, new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
        built = true;
    }

    /** حلقة توهج متمركزة خارج الحافة بمقدار halfWidth */
    private void ring(Path path, float width) {
        Rect b = getBounds();
        float half = width * 0.5f;
        rf.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
        path.reset();
        path.addRoundRect(rf, radius, radius, Path.Direction.CW);
    }

    /** ضبط شفافية لون (لطبقات التوهج) */
    private static int alpha(int color, float factor) {
        int a = (int) ((color >>> 24) * factor) & 0xFF;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!built) build();
        if (!built || getBounds().isEmpty()) return;

        // ١) الهالة الخارجية: أوسع وأخفت ← أضيق وأسطع
        glowP.setStrokeWidth(strokeW * 7.0f);
        glowP.setColor(alpha(glowColor, 0x0D * glowStrength));
        canvas.drawPath(glowP3, glowP);
        glowP.setStrokeWidth(strokeW * 4.2f);
        glowP.setColor(alpha(glowColor, 0x1C * glowStrength));
        canvas.drawPath(glowP2, glowP);
        glowP.setStrokeWidth(strokeW * 2.0f);
        glowP.setColor(alpha(glowColor, 0x30 * glowStrength));
        canvas.drawPath(glowP1, glowP);

        // ٢) الزجاج
        canvas.drawPath(fillPath, fillP);

        // ٣) الحد المتدرج الإضاءة
        canvas.drawPath(edgePath, edgeP);

        // ٤) الانعكاس الداخلي العلوي
        canvas.drawPath(shinePath, shineP);
    }

    @Override
    public void setAlpha(int alpha) {
        // الشفافية مضبوطة داخل الألوان نفسها — لا حاجة لتعديل
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // بلا فلترة
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
