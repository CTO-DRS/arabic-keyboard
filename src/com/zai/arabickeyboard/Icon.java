package com.zai.arabickeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * مكتبة الأيقونات المرسومة — DRS Smart v2.6
 * كل أيقونة تُرسم هندسياً عبر Canvas (لا إيموجي ولا خطوط خارجية)
 * لضمان مظهر موحد حاد الأنيق على كل الأجهزة والإصدارات.
 * الاستخدام: new Icon(ctx, Icon.PALETTE, UiKit.ACCENT_SOFT)
 */
public class Icon extends View {

    // ==================== أنواع الأيقونات ====================
    public static final int HOUSE         = 0;   // الرئيسية
    public static final int SWATCHES      = 1;   // الثيمات (شبكة ٢×٢)
    public static final int SLIDERS       = 2;   // الإعدادات (منزلقات)
    public static final int SHIELD        = 3;   // الفحص/الحماية
    public static final int INFO          = 4;   // معلومات
    public static final int PALETTE       = 5;   // لوحة الألوان
    public static final int GEAR          = 6;   // ترس
    public static final int KEYBOARD      = 7;   // لوحة مفاتيح
    public static final int RULER         = 8;   // تنسيق ومقاسات
    public static final int SPEAKER       = 9;   // الصوت
    public static final int EYE_OFF       = 10;  // الخصوصية
    public static final int BOOK          = 11;  // القاموس
    public static final int BOLT          = 12;  // الاختصارات السريعة
    public static final int SAVE          = 13;  // النسخ الاحتياطي
    public static final int TAP           = 14;  // لمسة/ضغط
    public static final int PEN           = 15;  // كتابة/سحب
    public static final int CLIPBOARD     = 16;  // الحافظة
    public static final int HOURGLASS     = 17;  // جارٍ العمل
    public static final int WRENCH        = 18;  // إصلاح
    public static final int CHECK_CIRCLE  = 19;  // نجاح
    public static final int CROSS_CIRCLE  = 20;  // فشل
    public static final int WARN_TRIANGLE = 21;  // تحذير
    public static final int PIN           = 22;  // تثبيت
    public static final int CHECK         = 23;  // علامة صح
    public static final int CROSS         = 24;  // علامة حذف

    private int type;
    private int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();

    public Icon(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setIconColor(int c) { color = c; invalidate(); }
    public void setIconType(int t)  { type = t; invalidate(); }

    /** اختصار لتبديل لون التبويب النشط */
    public void setActive(boolean on, int activeColor, int idleColor) {
        int target = on ? activeColor : idleColor;
        if (target != color) { color = target; invalidate(); }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cx = w / 2f, cy = h / 2f;
        float s = Math.min(w, h);          // وحدة القياس
        float u = s * 0.5f;                // نصف الحجم

        p.setColor(color);
        p.setStrokeWidth(s * 0.085f);
        p.setStyle(Paint.Style.STROKE);
        path.reset();

        // اختصارات إحداثيات: X(n) = cx + (n-0.5)*s
        switch (type) {

            case HOUSE: {
                // سقف
                path.moveTo(cx - u * 0.78f, cy + u * 0.02f);
                path.lineTo(cx, cy - u * 0.72f);
                path.lineTo(cx + u * 0.78f, cy + u * 0.02f);
                canvas.drawPath(path, p);
                path.reset();
                // جسم
                path.moveTo(cx - u * 0.56f, cy - u * 0.08f);
                path.lineTo(cx - u * 0.56f, cy + u * 0.74f);
                path.lineTo(cx + u * 0.56f, cy + u * 0.74f);
                path.lineTo(cx + u * 0.56f, cy - u * 0.08f);
                canvas.drawPath(path, p);
                // باب
                path.reset();
                path.moveTo(cx - u * 0.14f, cy + u * 0.74f);
                path.lineTo(cx - u * 0.14f, cy + u * 0.28f);
                path.lineTo(cx + u * 0.14f, cy + u * 0.28f);
                path.lineTo(cx + u * 0.14f, cy + u * 0.74f);
                canvas.drawPath(path, p);
                break;
            }

            case SWATCHES: {
                // شبكة ٢×٢ — واحدة مملوءة
                float r = u * 0.5f, g = u * 0.16f;
                canvas.drawRoundRect(cx - r - g, cy - r - g, cx - g, cy - g, u * 0.16f, u * 0.16f, p);
                canvas.drawRoundRect(cx + g, cy - r - g, cx + g + r, cy - g, u * 0.16f, u * 0.16f, p);
                canvas.drawRoundRect(cx - r - g, cy + g, cx - g, cy + g + r, u * 0.16f, u * 0.16f, p);
                p.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(cx + g, cy + g, cx + g + r, cy + g + r, u * 0.16f, u * 0.16f, p);
                p.setStyle(Paint.Style.STROKE);
                break;
            }

            case SLIDERS: {
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * u * 0.52f;
                    canvas.drawLine(cx - u * 0.7f, y, cx + u * 0.7f, y, p);
                    p.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx + i * u * 0.34f, y, u * 0.14f, p);
                    p.setStyle(Paint.Style.STROKE);
                }
                break;
            }

            case SHIELD: {
                path.moveTo(cx, cy - u * 0.8f);
                path.lineTo(cx + u * 0.64f, cy - u * 0.52f);
                path.lineTo(cx + u * 0.6f, cy + u * 0.06f);
                path.quadTo(cx + u * 0.52f, cy + u * 0.52f, cx, cy + u * 0.8f);
                path.quadTo(cx - u * 0.52f, cy + u * 0.52f, cx - u * 0.6f, cy + u * 0.06f);
                path.lineTo(cx - u * 0.64f, cy - u * 0.52f);
                path.close();
                canvas.drawPath(path, p);
                break;
            }

            case INFO: {
                canvas.drawCircle(cx, cy, u * 0.72f, p);
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy - u * 0.36f, u * 0.08f, p);
                canvas.drawRoundRect(cx - u * 0.06f, cy - u * 0.08f,
                        cx + u * 0.06f, cy + u * 0.44f, u * 0.06f, u * 0.06f, p);
                p.setStyle(Paint.Style.STROKE);
                break;
            }

            case PALETTE: {
                canvas.drawCircle(cx, cy, u * 0.72f, p);
                p.setStyle(Paint.Style.FILL);
                float d = u * 0.11f;
                canvas.drawCircle(cx - u * 0.3f, cy - u * 0.32f, d, p);
                canvas.drawCircle(cx + u * 0.04f, cy - u * 0.44f, d, p);
                canvas.drawCircle(cx + u * 0.36f, cy - u * 0.24f, d, p);
                canvas.drawCircle(cx - u * 0.42f, cy + u * 0.04f, d, p);
                p.setStyle(Paint.Style.STROKE);
                // ثقب الإبهام
                canvas.drawCircle(cx + u * 0.06f, cy + u * 0.32f, u * 0.17f, p);
                break;
            }

            case GEAR: {
                canvas.drawCircle(cx, cy, u * 0.4f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4.0 * i;
                    float x1 = cx + (float) Math.cos(a) * u * 0.55f;
                    float y1 = cy + (float) Math.sin(a) * u * 0.55f;
                    float x2 = cx + (float) Math.cos(a) * u * 0.78f;
                    float y2 = cy + (float) Math.sin(a) * u * 0.78f;
                    canvas.drawLine(x1, y1, x2, y2, p);
                }
                break;
            }

            case KEYBOARD: {
                rf.set(cx - u * 0.88f, cy - u * 0.52f, cx + u * 0.88f, cy + u * 0.52f);
                canvas.drawRoundRect(rf, u * 0.18f, u * 0.18f, p);
                p.setStyle(Paint.Style.FILL);
                float k = u * 0.055f;
                for (int i = -2; i <= 2; i++)
                    canvas.drawCircle(cx + i * u * 0.32f, cy - u * 0.18f, k, p);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx - u * 0.44f, cy + u * 0.2f, cx + u * 0.44f, cy + u * 0.2f, p);
                break;
            }

            case RULER: {
                rf.set(cx - u * 0.85f, cy - u * 0.36f, cx + u * 0.85f, cy + u * 0.36f);
                canvas.drawRoundRect(rf, u * 0.14f, u * 0.14f, p);
                for (int i = -2; i <= 2; i++) {
                    float x = cx + i * u * 0.32f;
                    canvas.drawLine(x, cy - u * 0.36f, x, cy - u * 0.02f, p);
                }
                break;
            }

            case SPEAKER: {
                path.moveTo(cx - u * 0.78f, cy - u * 0.2f);
                path.lineTo(cx - u * 0.42f, cy - u * 0.2f);
                path.lineTo(cx - u * 0.06f, cy - u * 0.56f);
                path.lineTo(cx - u * 0.06f, cy + u * 0.56f);
                path.lineTo(cx - u * 0.42f, cy + u * 0.2f);
                path.lineTo(cx - u * 0.78f, cy + u * 0.2f);
                path.close();
                canvas.drawPath(path, p);
                rf.set(cx + u * 0.08f, cy - u * 0.34f, cx + u * 0.76f, cy + u * 0.34f);
                canvas.drawArc(rf, -52, 104, false, p);
                rf.set(cx + u * 0.08f, cy - u * 0.66f, cx + u * 1.1f, cy + u * 0.66f);
                canvas.drawArc(rf, -52, 104, false, p);
                break;
            }

            case EYE_OFF: {
                // عين
                path.moveTo(cx - u * 0.78f, cy);
                path.quadTo(cx, cy - u * 0.74f, cx + u * 0.78f, cy);
                canvas.drawPath(path, p);
                path.reset();
                path.moveTo(cx - u * 0.78f, cy);
                path.quadTo(cx, cy + u * 0.74f, cx + u * 0.78f, cy);
                canvas.drawPath(path, p);
                canvas.drawCircle(cx, cy, u * 0.2f, p);
                // شطبة الإخفاء
                canvas.drawLine(cx - u * 0.62f, cy + u * 0.66f, cx + u * 0.62f, cy - u * 0.66f, p);
                break;
            }

            case BOOK: {
                // كتاب مفتوح
                path.moveTo(cx, cy - u * 0.52f);
                path.quadTo(cx - u * 0.36f, cy - u * 0.7f, cx - u * 0.76f, cy - u * 0.58f);
                path.lineTo(cx - u * 0.76f, cy + u * 0.5f);
                path.quadTo(cx - u * 0.36f, cy + u * 0.38f, cx, cy + u * 0.56f);
                path.quadTo(cx + u * 0.36f, cy + u * 0.38f, cx + u * 0.76f, cy + u * 0.5f);
                path.lineTo(cx + u * 0.76f, cy - u * 0.58f);
                path.quadTo(cx + u * 0.36f, cy - u * 0.7f, cx, cy - u * 0.52f);
                canvas.drawPath(path, p);
                break;
            }

            case BOLT: {
                p.setStyle(Paint.Style.FILL);
                path.moveTo(cx + u * 0.14f, cy - u * 0.84f);
                path.lineTo(cx - u * 0.5f, cy + u * 0.1f);
                path.lineTo(cx - u * 0.08f, cy + u * 0.1f);
                path.lineTo(cx - u * 0.2f, cy + u * 0.84f);
                path.lineTo(cx + u * 0.5f, cy - u * 0.12f);
                path.lineTo(cx + u * 0.06f, cy - u * 0.12f);
                path.close();
                canvas.drawPath(path, p);
                p.setStyle(Paint.Style.STROKE);
                break;
            }

            case SAVE: {
                // صينية + سهم للأسفل (تصدير/نسخ احتياطي)
                path.moveTo(cx - u * 0.76f, cy + u * 0.12f);
                path.lineTo(cx - u * 0.76f, cy + u * 0.72f);
                path.lineTo(cx + u * 0.76f, cy + u * 0.72f);
                path.lineTo(cx + u * 0.76f, cy + u * 0.12f);
                canvas.drawPath(path, p);
                path.reset();
                path.moveTo(cx, cy - u * 0.8f);
                path.lineTo(cx, cy + u * 0.28f);
                canvas.drawPath(path, p);
                path.reset();
                path.moveTo(cx - u * 0.36f, cy - u * 0.1f);
                path.lineTo(cx, cy + u * 0.3f);
                path.lineTo(cx + u * 0.36f, cy - u * 0.1f);
                canvas.drawPath(path, p);
                break;
            }

            case TAP: {
                // بصمة لمسة: نقطة + أقواس مشعة
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy + u * 0.4f, u * 0.16f, p);
                p.setStyle(Paint.Style.STROKE);
                rf.set(cx - u * 0.42f, cy - u * 0.02f, cx + u * 0.42f, cy + u * 0.82f);
                canvas.drawArc(rf, 205, 130, false, p);
                rf.set(cx - u * 0.68f, cy - u * 0.28f, cx + u * 0.68f, cy + u * 1.08f);
                canvas.drawArc(rf, 212, 116, false, p);
                break;
            }

            case PEN: {
                path.moveTo(cx - u * 0.6f, cy + u * 0.64f);
                path.lineTo(cx - u * 0.48f, cy + u * 0.2f);
                path.lineTo(cx + u * 0.32f, cy - u * 0.64f);
                path.quadTo(cx + u * 0.46f, cy - u * 0.78f, cx + u * 0.6f, cy - u * 0.64f);
                path.quadTo(cx + u * 0.74f, cy - u * 0.5f, cx + u * 0.6f, cy - u * 0.36f);
                path.lineTo(cx - u * 0.2f, cy + u * 0.48f);
                path.close();
                canvas.drawPath(path, p);
                canvas.drawLine(cx - u * 0.48f, cy + u * 0.2f, cx - u * 0.2f, cy + u * 0.48f, p);
                canvas.drawLine(cx - u * 0.6f, cy + u * 0.64f, cx - u * 0.82f, cy + u * 0.86f, p);
                break;
            }

            case CLIPBOARD: {
                rf.set(cx - u * 0.58f, cy - u * 0.72f, cx + u * 0.58f, cy + u * 0.8f);
                canvas.drawRoundRect(rf, u * 0.16f, u * 0.16f, p);
                rf.set(cx - u * 0.2f, cy - u * 0.88f, cx + u * 0.2f, cy - u * 0.56f);
                canvas.drawRoundRect(rf, u * 0.1f, u * 0.1f, p);
                canvas.drawLine(cx - u * 0.3f, cy + u * 0.02f, cx + u * 0.3f, cy + u * 0.02f, p);
                canvas.drawLine(cx - u * 0.3f, cy + u * 0.32f, cx + u * 0.3f, cy + u * 0.32f, p);
                canvas.drawLine(cx - u * 0.3f, cy - u * 0.28f, cx + u * 0.3f, cy - u * 0.28f, p);
                break;
            }

            case HOURGLASS: {
                canvas.drawLine(cx - u * 0.5f, cy - u * 0.8f, cx + u * 0.5f, cy - u * 0.8f, p);
                canvas.drawLine(cx - u * 0.5f, cy + u * 0.8f, cx + u * 0.5f, cy + u * 0.8f, p);
                path.moveTo(cx - u * 0.38f, cy - u * 0.78f);
                path.quadTo(cx - u * 0.38f, cy, cx + u * 0.38f, cy + u * 0.78f);
                path.moveTo(cx + u * 0.38f, cy - u * 0.78f);
                path.quadTo(cx + u * 0.38f, cy, cx - u * 0.38f, cy + u * 0.78f);
                canvas.drawPath(path, p);
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy + u * 0.52f, u * 0.07f, p);
                p.setStyle(Paint.Style.STROKE);
                break;
            }

            case WRENCH: {
                // رأس مفتوح + مقبض سميك
                rf.set(cx - u * 0.14f, cy - u * 0.94f, cx + u * 0.62f, cy - u * 0.18f);
                canvas.drawArc(rf, 160, 300, false, p);
                float old = p.getStrokeWidth();
                p.setStrokeWidth(s * 0.13f);
                canvas.drawLine(cx + u * 0.2f, cy - u * 0.4f, cx - u * 0.6f, cy + u * 0.72f, p);
                p.setStrokeWidth(old);
                break;
            }

            case CHECK_CIRCLE: {
                canvas.drawCircle(cx, cy, u * 0.74f, p);
                path.moveTo(cx - u * 0.36f, cy + u * 0.02f);
                path.lineTo(cx - u * 0.1f, cy + u * 0.3f);
                path.lineTo(cx + u * 0.4f, cy - u * 0.26f);
                canvas.drawPath(path, p);
                break;
            }

            case CROSS_CIRCLE: {
                canvas.drawCircle(cx, cy, u * 0.74f, p);
                canvas.drawLine(cx - u * 0.28f, cy - u * 0.28f, cx + u * 0.28f, cy + u * 0.28f, p);
                canvas.drawLine(cx + u * 0.28f, cy - u * 0.28f, cx - u * 0.28f, cy + u * 0.28f, p);
                break;
            }

            case WARN_TRIANGLE: {
                path.moveTo(cx, cy - u * 0.76f);
                path.lineTo(cx + u * 0.8f, cy + u * 0.68f);
                path.lineTo(cx - u * 0.8f, cy + u * 0.68f);
                path.close();
                canvas.drawPath(path, p);
                canvas.drawLine(cx, cy - u * 0.3f, cx, cy + u * 0.18f, p);
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy + u * 0.42f, u * 0.07f, p);
                p.setStyle(Paint.Style.STROKE);
                break;
            }

            case PIN: {
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy - u * 0.42f, u * 0.3f, p);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx - u * 0.34f, cy + u * 0.12f, cx + u * 0.34f, cy + u * 0.12f, p);
                canvas.drawLine(cx, cy + u * 0.12f, cx, cy + u * 0.62f, p);
                break;
            }

            case CHECK: {
                float old = p.getStrokeWidth();
                p.setStrokeWidth(s * 0.11f);
                path.moveTo(cx - u * 0.6f, cy + u * 0.04f);
                path.lineTo(cx - u * 0.14f, cy + u * 0.5f);
                path.lineTo(cx + u * 0.62f, cy - u * 0.46f);
                canvas.drawPath(path, p);
                p.setStrokeWidth(old);
                break;
            }

            case CROSS: {
                float old = p.getStrokeWidth();
                p.setStrokeWidth(s * 0.11f);
                canvas.drawLine(cx - u * 0.5f, cy - u * 0.5f, cx + u * 0.5f, cy + u * 0.5f, p);
                canvas.drawLine(cx + u * 0.5f, cy - u * 0.5f, cx - u * 0.5f, cy + u * 0.5f, p);
                p.setStrokeWidth(old);
                break;
            }

            default:
                canvas.drawCircle(cx, cy, u * 0.7f, p);
                break;
        }
    }
}
