package com.zai.arabickeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * لوحة مفاتيح مرسومة بالكامل عبر Canvas — تصميم عصري بأزرار دائرية الحواف،
 * مع معاينة ضغط، بدائل الضغط المطوّل، وتكرار المسح تلقائياً.
 */
public class KeyboardView extends View {

    public interface Listener {
        void onKey(Key k);
        void onText(String t);
        void onKeyLongPress(Key k);
        void onCursorMove(int deltaSteps);
    }

    /** مستمع الكتابة بالسحب: يُستدعى عند رفع الإصبع بعد تمرير بين عدة حروف */
    public interface GlideListener {
        void onGlide(List<float[]> tracePoints, String letters);
    }

    private static class LaidKey {
        Key key;
        final RectF r = new RectF();
        boolean pressed;
    }

    private List<Row> rows = new ArrayList<>();
    private final List<List<LaidKey>> laid = new ArrayList<>();
    private Listener listener;
    private ThemeSet theme;
    public boolean hapticsEnabled = true;
    private boolean shifted;
    private boolean shiftLocked;
    private String enterLabel;

    // شريط الاقتراحات
    public interface StripListener {
        void onStripAction(int action, int index);
    }
    public static final int STRIP_CLIP = 0;
    public static final int STRIP_EDIT = 1;
    public static final int STRIP_WORD = 2;
    public static final int STRIP_SHIELD = 3;

    private StripListener stripListener;
    private final List<String> suggestions = new ArrayList<>();
    private boolean stripVisible;
    private int stripH;
    private int pressedSlot = -1;

    // الوضع بيد واحدة: 0 وسط، 1 يمين، 2 يسار
    private int oneHanded;
    private float kbOffsetX, kbWidth;

    // أبعاد
    private int baseKeyH;
    private int keyH, rowGap, keyGap, sidePad, topPad, bottomPad, radius;
    private float keyHeightScale = 1f;

    // سحب المسافة لتحريك المؤشر
    private boolean spaceCursorMode;
    private float spaceStartX;
    private int cursorStepsEmitted;

    // الكتابة بالسحب (Glide)
    public boolean glideEnabled = false;
    private boolean glideMode;
    private final List<float[]> glidePts = new ArrayList<>();
    private final StringBuilder glideLetters = new StringBuilder();
    private float glideLastX, glideLastY;
    private float glideStartX, glideStartY;
    private GlideListener glideListener;
    private int longPressDelay = 380;
    public boolean incognitoOn = false;

    // أدوات الرسم
    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path iconPath = new Path();
    private final RectF rectF = new RectF();
    private final Rect fmRect = new Rect();

    // اللمس
    private LaidKey touched;
    private int pointerId = -1;
    private final Handler handler = new Handler();
    private Runnable repeatRunnable;
    private int repeatCount;
    private boolean longPressedFired;
    private boolean stripTouched;
    private int stripDownSlot = -1;

    // نوافذ منبثقة
    private PopupWindow previewPopup;
    private TextView previewText;
    private PopupWindow altPopup;
    private LinearLayout altContent;
    private List<String> altList;
    private int altSelected = -1;
    private LaidKey altSourceKey;

    public KeyboardView(Context c) { super(c); init(); }
    public KeyboardView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        baseKeyH = res(R.dimen.kb_key_height);
        keyH = baseKeyH;
        rowGap = res(R.dimen.kb_row_gap);
        keyGap = res(R.dimen.kb_key_gap);
        sidePad = res(R.dimen.kb_side_pad);
        topPad = res(R.dimen.kb_top_pad);
        bottomPad = res(R.dimen.kb_bottom_pad);
        radius = dp(9);
        stripH = dp(40);

        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hintPaint.setTypeface(Typeface.DEFAULT);
        hintPaint.setTextAlign(Paint.Align.RIGHT);

        if (theme == null) theme = ThemeSet.resolve(ThemeSet.DEFAULT_PRESET, false);
    }

    private int res(int id) { return getResources().getDimensionPixelSize(id); }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    // ==================== ضبط الحالة من الخارج ====================

    public void setListener(Listener l) { listener = l; }

    public void setStripListener(StripListener l) { stripListener = l; }

    public void setGlideListener(GlideListener l) { glideListener = l; }

    /** مدة الضغط المطوّل بالميلي ثانية */
    public void setLongPressDelay(int ms) {
        longPressDelay = Math.max(150, ms);
    }

    /** خريطة حروف اللوحة الحالية ← مركز الزر {x, y} (للمطابقة الهندسية للسحب) */
    public Map<String, float[]> charCenters() {
        Map<String, float[]> m = new HashMap<>();
        for (List<LaidKey> lr : laid) {
            for (LaidKey lk : lr) {
                Key k = lk.key;
                if (k.type == Key.CHAR && k.text != null && k.text.length() == 1
                        && !m.containsKey(k.text)) {
                    m.put(k.text, new float[]{lk.r.centerX(), lk.r.centerY()});
                }
            }
        }
        return m;
    }

    public int getKeyHeightPx() { return keyH; }

    public void setTheme(ThemeSet t) {
        theme = t;
        bgPaint = null; // إعادة بناء تدرج الخلفية للثيم الجديد
        invalidate();
    }

    public void setKeyboard(List<Row> newRows) {
        cancelAll();
        rows = newRows;
        touched = null;
        // إعادة الحساب فوراً حتى لو لم يتغير العرض (تبديل العربية/الإنجليزية)
        if (getWidth() > 0) computeLayout(getWidth());
        requestLayout();
        invalidate();
    }

    public void setShifted(boolean s) {
        if (shifted != s) { shifted = s; invalidate(); }
    }

    public void setShiftLock(boolean l) {
        if (shiftLocked != l) { shiftLocked = l; invalidate(); }
    }

    public void setEnterLabel(String label) {
        enterLabel = label;
        invalidate();
    }

    /** ضبط حجم المفاتيح (0.85 - 1.2) */
    public void setKeyHeightScale(float s) {
        if (Math.abs(keyHeightScale - s) < 0.01f) return;
        keyHeightScale = s;
        keyH = Math.max(dp(30), (int) (baseKeyH * s + 0.5f));
        if (getWidth() > 0) computeLayout(getWidth());
        requestLayout();
        invalidate();
    }

    /** الوضع بيد واحدة: 0 وسط، 1 يمين، 2 يسار */
    public void setOneHanded(int mode) {
        if (oneHanded == mode) return;
        oneHanded = mode;
        if (getWidth() > 0) computeLayout(getWidth());
        invalidate();
    }

    /** إظهار/إخفاء شريط الاقتراحات */
    public void setStripVisible(boolean v) {
        if (stripVisible == v) return;
        stripVisible = v;
        if (getWidth() > 0) computeLayout(getWidth());
        requestLayout();
        invalidate();
    }

    /** تحديث كلمات الاقتراح (حتى 3) */
    public void setSuggestions(List<String> s) {
        suggestions.clear();
        if (s != null) suggestions.addAll(s);
        if (stripVisible) invalidate();
    }

    public void cancelAll() {
        stopRepeat();
        handler.removeCallbacksAndMessages(null);
        hidePreview();
        dismissAltPopup();
        clearGlide();
        if (touched != null) { touched.pressed = false; touched = null; }
    }

    /** مسح حالة السحب (Glide) بالكامل */
    private void clearGlide() {
        glidePts.clear();
        glideLetters.setLength(0);
        glideMode = false;
    }

    /** إلغاء مؤقّت الضغط المطوّل المعلّق (يستخدم عند تفعيل وضع السحب) */
    private void cancelLongPressTimer() {
        longPressedFired = true;
    }

    public int getDesiredHeight() {
        int n = rows.size();
        if (n == 0) return 0;
        return (stripVisible ? stripH : 0) + topPad + bottomPad + n * keyH + (n - 1) * rowGap;
    }

    // ==================== القياس والتخطيط ====================

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = getDesiredHeight();
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(hSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        computeLayout(w);
    }

    private void computeLayout(int w) {
        laid.clear();
        if (rows.isEmpty() || w <= 0) return;
        // حساب منطقة المفاتيح حسب الوضع بيد واحدة
        if (oneHanded == 0) {
            kbOffsetX = 0;
            kbWidth = w;
        } else {
            kbWidth = w * 0.86f;
            kbOffsetX = (oneHanded == 1) ? w - kbWidth : 0;
        }
        float y = topPad + (stripVisible ? stripH : 0);
        for (Row row : rows) {
            int n = row.keys.size();
            float total = 0;
            for (Key k : row.keys) total += k.weight;
            float avail = kbWidth - 2 * sidePad - (n - 1) * (float) keyGap;
            float unit = avail / total;
            float x = kbOffsetX + sidePad;
            List<LaidKey> lr = new ArrayList<>(n);
            for (Key k : row.keys) {
                LaidKey lk = new LaidKey();
                lk.key = k;
                lk.r.set(x, y, x + unit * k.weight, y + keyH);
                x += unit * k.weight + keyGap;
                lr.add(lk);
            }
            laid.add(lr);
            y += keyH + rowGap;
        }
    }

    private LaidKey findKey(float x, float y) {
        for (List<LaidKey> lr : laid)
            for (LaidKey lk : lr)
                if (lk.r.contains(x, y)) return lk;
        return null;
    }

    // ==================== الرسم ====================

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        if (stripVisible) drawStrip(canvas);
        for (List<LaidKey> lr : laid) {
            for (LaidKey lk : lr) {
                drawKey(canvas, lk);
            }
        }
        // أثر السحب (Glide) فوق كل شيء
        drawGlideTrace(canvas);
    }

    /** رسم مسار السحب: خط عريض شفاف بتوهج + قلب بلون التمييز */
    private final Paint glidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path glidePath = new Path();
    private int glideGlowColor = 0;

    private void drawGlideTrace(Canvas canvas) {
        if (glidePts.size() < 2) return;
        glidePath.reset();
        glidePath.moveTo(glidePts.get(0)[0], glidePts.get(0)[1]);
        for (int i = 1; i < glidePts.size(); i++) {
            glidePath.lineTo(glidePts.get(i)[0], glidePts.get(i)[1]);
        }
        int glow = theme.keyBgAction;
        glidePaint.setStyle(Paint.Style.STROKE);
        glidePaint.setStrokeCap(Paint.Cap.ROUND);
        glidePaint.setStrokeJoin(Paint.Join.ROUND);
        glidePaint.setColor(((theme.dark ? 0x50 : 0x38) << 24) | (glow & 0xFFFFFF));
        glidePaint.setStrokeWidth(dp(14));
        canvas.drawPath(glidePath, glidePaint);
        glidePaint.setColor(((theme.dark ? 0xC8 : 0xB4) << 24) | (glow & 0xFFFFFF));
        glidePaint.setStrokeWidth(dp(4.5f));
        canvas.drawPath(glidePath, glidePaint);
        glideGlowColor = glow; // يُحفظ للاستخدام المستقبلي
    }

    private Paint bgPaint;
    private int bgPaintHeight = -1;

    private void drawBackground(Canvas canvas) {
        if (theme.gradient) {
            if (bgPaint == null || bgPaintHeight != getHeight()) {
                bgPaintHeight = getHeight();
                bgPaint = new Paint();
                android.graphics.LinearGradient lg = new android.graphics.LinearGradient(
                        0, 0, 0, getHeight(),
                        theme.kbBgTop, theme.kbBg,
                        android.graphics.Shader.TileMode.CLAMP);
                bgPaint.setShader(lg);
            }
            canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        } else {
            canvas.drawColor(theme.kbBg);
        }
    }

    // ==================== شريط الاقتراحات ====================

    private void drawStrip(Canvas canvas) {
        int w = getWidth();
        // خط فاصل سفلي خفيف
        keyPaint.setColor(theme.dark ? 0x22FFFFFF : 0x22000000);
        canvas.drawRect(sidePad, stripH - dp(1), w - sidePad, stripH, keyPaint);

        int iconW = dp(40);
        int gap = dp(5);
        int wordsStart = sidePad + iconW + gap;
        int wordsEnd = w - sidePad - 2 * iconW - 2 * gap;
        int slotW = (wordsEnd - wordsStart) / 3;

        // أيقونة الحافظة (يسار)
        drawClipboardIcon(canvas, sidePad + iconW / 2f, stripH / 2f, pressedSlot == 0);
        // أيقونة الدرع (الوضع التخفي) قرب اليمين
        drawShieldIcon(canvas, w - sidePad - iconW - gap - iconW / 2f, stripH / 2f, pressedSlot == 4);
        // أيقونة التحرير (يمين)
        drawEditIcon(canvas, w - sidePad - iconW / 2f, stripH / 2f, pressedSlot == 5);

        // الكلمات
        for (int i = 0; i < 3; i++) {
            float sx = wordsStart + i * (float) slotW;
            float ex = sx + slotW;
            if (i == pressedSlot - 1) {
                rectF.set(sx + dp(3), dp(5), ex - dp(3), stripH - dp(5));
                keyPaint.setColor(theme.dark ? 0x30FFFFFF : 0x1A000000);
                canvas.drawRoundRect(rectF, dp(8), dp(8), keyPaint);
            }
            if (i >= suggestions.size()) continue;
            String word = suggestions.get(i);
            if (word == null || word.isEmpty()) continue;
            int color = (i == 0) ? (theme.stripWord != 0 ? theme.stripWord : theme.keyBgAction)
                                 : theme.keyText;
            textPaint.setColor(color);
            float size = stripH * 0.40f;
            textPaint.setTextSize(size);
            float tw = textPaint.measureText(word);
            float maxW = slotW - dp(8);
            if (tw > maxW) {
                // قص الكلمة الطويلة
                while (word.length() > 1 && textPaint.measureText(word + "…") > maxW) {
                    word = word.substring(0, word.length() - 1);
                }
                word = word + "…";
                textPaint.setTextSize(size);
            }
            float cx = sx + slotW / 2f;
            float baseline = stripH / 2f - (textPaint.ascent() + textPaint.descent()) / 2f;
            canvas.drawText(word, cx, baseline, textPaint);
            if (i == 0) {
                // خط صغير أسفل الكلمة المرشحة
                float uw = Math.min(textPaint.measureText(word), maxW);
                keyPaint.setColor(color);
                canvas.drawRect(cx - uw / 2f, stripH - dp(6), cx + uw / 2f, stripH - dp(4.5f), keyPaint);
            }
        }
    }

    /** أيقونة الدرع: الوضع التخفي (مفعّل = مملوء بلون التمييز) */
    private void drawShieldIcon(Canvas canvas, float cx, float cy, boolean pressed) {
        float w2 = dp(7.2f), h2 = dp(9.2f);
        boolean on = incognitoOn;
        iconPath.reset();
        iconPath.moveTo(cx, cy - h2);
        iconPath.lineTo(cx + w2, cy - h2 * 0.5f);
        iconPath.lineTo(cx + w2 * 0.94f, cy + h2 * 0.22f);
        iconPath.quadTo(cx + w2 * 0.62f, cy + h2 * 0.92f, cx, cy + h2);
        iconPath.quadTo(cx - w2 * 0.62f, cy + h2 * 0.92f, cx - w2 * 0.94f, cy + h2 * 0.22f);
        iconPath.lineTo(cx - w2, cy - h2 * 0.5f);
        iconPath.close();
        iconPaint.setColor(on || pressed ? theme.keyBgAction : theme.keyTextFunc);
        iconPaint.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.6f));
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(iconPath, iconPaint);
        if (on) {
            // شرطة داخل الدرع للدلالة على التفعيل
            iconPaint.setColor(theme.keyTextAction);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(cx - w2 * 0.45f, cy, cx + w2 * 0.45f, cy, iconPaint);
        }
    }

    private void drawClipboardIcon(Canvas canvas, float cx, float cy, boolean pressed) {
        float w2 = dp(9), h2 = dp(11);
        iconPaint.setColor(pressed ? theme.keyBgAction : theme.keyTextFunc);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.7f));
        rectF.set(cx - w2, cy - h2 * 0.8f, cx + w2, cy + h2);
        canvas.drawRoundRect(rectF, dp(2.5f), dp(2.5f), iconPaint);
        // مشبك الحافظة العلوي
        rectF.set(cx - dp(4), cy - h2 - dp(1.5f), cx + dp(4), cy - h2 + dp(3.5f));
        iconPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rectF, dp(1.5f), dp(1.5f), iconPaint);
    }

    private void drawEditIcon(Canvas canvas, float cx, float cy, boolean pressed) {
        // قلم مائل
        float s = dp(6.5f);
        iconPaint.setColor(pressed ? theme.keyBgAction : theme.keyTextFunc);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.9f));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        iconPath.reset();
        iconPath.moveTo(cx - s * 0.7f, cy + s);
        iconPath.lineTo(cx - s * 0.9f, cy + s * 1.2f);
        iconPath.lineTo(cx - s * 0.95f, cy + s * 0.75f);
        iconPath.lineTo(cx + s * 0.75f, cy - s * 0.9f);
        iconPath.lineTo(cx + s * 0.05f, cy - s * 1.55f);
        iconPath.lineTo(cx - s * 0.7f, cy + s);
        iconPath.close();
        canvas.drawPath(iconPath, iconPaint);
        iconPath.reset();
        iconPath.moveTo(cx - s * 0.35f, cy + s * 0.05f);
        iconPath.lineTo(cx + s * 0.4f, cy - s * 0.7f);
        canvas.drawPath(iconPath, iconPaint);
    }

    private int bgColorFor(LaidKey lk) {
        Key k = lk.key;
        if (k.type == Key.ACTION)
            return lk.pressed ? theme.keyBgActionPressed : theme.keyBgAction;
        if (k.type == Key.FUNC)
            return lk.pressed ? theme.keyBgFuncPressed : theme.keyBgFunc;
        return lk.pressed ? theme.keyBgPressed : theme.keyBg;
    }

    private int textColorFor(Key k) {
        if (k.type == Key.ACTION) return theme.keyTextAction;
        if (k.type == Key.FUNC) return theme.keyTextFunc;
        return theme.keyText;
    }

    private void drawKey(Canvas canvas, LaidKey lk) {
        Key k = lk.key;
        rectF.set(lk.r);
        keyPaint.setColor(bgColorFor(lk));
        keyPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rectF, radius, radius, keyPaint);

        float cx = lk.r.centerX(), cy = lk.r.centerY();

        switch (k.code) {
            case Key.CODE_SHIFT:
                drawShiftIcon(canvas, cx, cy, lk.r.width(), lk.r.height(), shifted);
                if (shiftLocked) {
                    iconPaint.setColor(textColorFor(k));
                    float bw = lk.r.width() * 0.16f;
                    rectF.set(cx - bw / 2, lk.r.bottom - dp(7), cx + bw / 2, lk.r.bottom - dp(5));
                    canvas.drawRoundRect(rectF, dp(1), dp(1), iconPaint);
                }
                return;
            case Key.CODE_BACKSPACE:
                drawBackspaceIcon(canvas, cx, cy, lk.r.width(), lk.r.height());
                return;
            case Key.CODE_ENTER:
                if (enterLabel != null) {
                    drawTextFitted(canvas, enterLabel, cx, cy, lk.r.width() * 0.8f, keyH * 0.24f, textColorFor(k));
                } else {
                    drawEnterIcon(canvas, cx, cy, lk.r.width(), lk.r.height());
                }
                return;
            case Key.CODE_EMOJI:
                drawEmojiIcon(canvas, cx, cy, lk.r.width(), lk.r.height());
                return;
            case Key.CODE_VOICE:
                drawMicIcon(canvas, cx, cy, lk.r.width(), lk.r.height());
                return;
            case Key.CODE_LANG:
                drawTextFitted(canvas, k.label, cx, cy, lk.r.width() * 0.8f, keyH * 0.26f, textColorFor(k));
                return;
        }

        if (k.type == Key.SPACE) {
            if (k.label != null && k.label.length() > 0) {
                hintPaint.setColor(theme.hintText);
                hintPaint.setTextSize(keyH * 0.20f);
                hintPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(k.label, cx, cy - (hintPaint.ascent() + hintPaint.descent()) / 2f, hintPaint);
                hintPaint.setTextAlign(Paint.Align.RIGHT);
            }
            return;
        }

        // أزرار الحروف والرموز
        String label;
        boolean showShift;
        if (shifted && k.shiftLabel != null) {
            label = k.shiftLabel;
            showShift = true;
        } else {
            label = k.label;
            showShift = false;
        }
        if (label == null) return;

        drawTextFitted(canvas, label, cx, cy, lk.r.width() * 0.82f, keyH * 0.36f, textColorFor(k));

        // تلميح صغير أعلى اليمين عند وجود Shift مختلف أو بدائل
        String hint = null;
        if (!showShift && k.shiftLabel != null && !k.shiftLabel.equals(k.label)) hint = k.shiftLabel;
        else if (!showShift && k.alts != null && !k.alts.isEmpty()) hint = k.alts.get(0);
        if (hint != null && hint.length() == 1) {
            hintPaint.setColor(theme.hintText);
            hintPaint.setTextSize(keyH * 0.17f);
            canvas.drawText(hint, lk.r.right - dp(5), lk.r.top + dp(12), hintPaint);
        }
    }

    private void drawTextFitted(Canvas canvas, String t, float cx, float cy,
                                float maxW, float baseSize, int color) {
        textPaint.setColor(color);
        float size = baseSize;
        textPaint.setTextSize(size);
        float tw = textPaint.measureText(t);
        if (tw > maxW) {
            size = Math.max(dp(8), size * maxW / tw);
            textPaint.setTextSize(size);
        }
        textPaint.getTextBounds(t, 0, t.length(), fmRect);
        float baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(t, cx, baseline, textPaint);
    }

    private void drawShiftIcon(Canvas canvas, float cx, float cy, float kw, float kh, boolean active) {
        float s = Math.min(kw, kh) * 0.28f;
        iconPath.reset();
        iconPath.moveTo(cx, cy - s * 1.15f);
        iconPath.lineTo(cx + s, cy);
        iconPath.lineTo(cx + s * 0.45f, cy);
        iconPath.lineTo(cx + s * 0.45f, cy + s * 0.8f);
        iconPath.lineTo(cx - s * 0.45f, cy + s * 0.8f);
        iconPath.lineTo(cx - s * 0.45f, cy);
        iconPath.lineTo(cx - s, cy);
        iconPath.close();
        iconPaint.setColor(theme.keyTextFunc);
        iconPaint.setStyle(active ? Paint.Style.FILL : Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.6f));
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(iconPath, iconPaint);
    }

    private void drawBackspaceIcon(Canvas canvas, float cx, float cy, float kw, float kh) {
        float w = kw * 0.30f, h = kh * 0.24f;
        float left = cx - w * 0.62f, right = cx + w;
        iconPath.reset();
        iconPath.moveTo(left, cy);
        iconPath.lineTo(left + w * 0.38f, cy - h);
        iconPath.lineTo(right, cy - h);
        iconPath.lineTo(right, cy + h);
        iconPath.lineTo(left + w * 0.38f, cy + h);
        iconPath.close();
        iconPaint.setColor(theme.keyTextFunc);
        iconPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(iconPath, iconPaint);
        // علامة X
        iconPaint.setColor(theme.kbBg);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.8f));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        float xs = w * 0.52f, ys = h * 0.38f;
        canvas.drawLine(cx - xs * 0.55f, cy - ys, cx + xs * 0.55f, cy + ys, iconPaint);
        canvas.drawLine(cx - xs * 0.55f, cy + ys, cx + xs * 0.55f, cy - ys, iconPaint);
    }

    private void drawEnterIcon(Canvas canvas, float cx, float cy, float kw, float kh) {
        float w = kw * 0.22f, h = kh * 0.18f;
        iconPaint.setColor(theme.keyTextAction);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(2f));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        iconPath.reset();
        iconPath.moveTo(cx + w, cy - h);
        iconPath.lineTo(cx + w, cy);
        iconPath.lineTo(cx - w, cy);
        canvas.drawPath(iconPath, iconPaint);
        // رأس السهم
        iconPath.reset();
        iconPath.moveTo(cx - w + dp(4.5f), cy - dp(4.5f));
        iconPath.lineTo(cx - w, cy);
        iconPath.lineTo(cx - w + dp(4.5f), cy + dp(4.5f));
        canvas.drawPath(iconPath, iconPaint);
    }

    private void drawEmojiIcon(Canvas canvas, float cx, float cy, float kw, float kh) {
        float r = Math.min(kw, kh) * 0.17f;
        iconPaint.setColor(theme.keyTextFunc);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.8f));
        canvas.drawCircle(cx, cy, r, iconPaint);
        iconPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.3f, dp(1.4f), iconPaint);
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.3f, dp(1.4f), iconPaint);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPath.reset();
        iconPath.arcTo(new RectF(cx - r * 0.5f, cy - r * 0.1f, cx + r * 0.5f, cy + r * 0.9f), 20, 140);
        canvas.drawPath(iconPath, iconPaint);
    }

    private void drawMicIcon(Canvas canvas, float cx, float cy, float kw, float kh) {
        float s = Math.min(kw, kh) * 0.20f;
        iconPaint.setColor(theme.keyTextFunc);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(dp(1.8f));
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        // جسم الماييكروفون (كبسولة)
        rectF.set(cx - s * 0.5f, cy - s * 1.3f, cx + s * 0.5f, cy + s * 0.3f);
        canvas.drawRoundRect(rectF, s * 0.5f, s * 0.5f, iconPaint);
        // القوس السفلي
        iconPath.reset();
        iconPath.moveTo(cx - s, cy - s * 0.1f);
        iconPath.lineTo(cx - s, cy + s * 0.25f);
        iconPath.quadTo(cx, cy + s * 1.5f, cx + s, cy + s * 0.25f);
        iconPath.lineTo(cx + s, cy - s * 0.1f);
        canvas.drawPath(iconPath, iconPaint);
        // الساق
        canvas.drawLine(cx, cy + s * 1.15f, cx, cy + s * 1.55f, iconPaint);
        canvas.drawLine(cx - s * 0.55f, cy + s * 1.55f, cx + s * 0.55f, cy + s * 1.55f, iconPaint);
    }

    // ==================== اللمس ====================

    private void pressFeedback() {
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private String displayLabel(Key k) {
        if (shifted && k.shiftLabel != null) return k.shiftLabel;
        return k.label;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onDown(ev);
                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(ev);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onUp(ev.getActionMasked() == MotionEvent.ACTION_CANCEL, ev);
                return true;
        }
        return super.onTouchEvent(ev);
    }

    /** فهرس خانة الشريط عند نقطة: 0 حافظة، 1-3 كلمات، 4 درع، 5 تحرير، -1 لا شيء */
    private int stripSlotAt(float x, float y) {
        if (!stripVisible || y > stripH) return -1;
        int w = getWidth();
        int iconW = dp(40);
        int gap = dp(5);
        int wordsStart = sidePad + iconW + gap;
        int wordsEnd = w - sidePad - 2 * iconW - 2 * gap;
        if (x >= sidePad && x <= sidePad + iconW) return 0;
        if (x >= w - sidePad - iconW && x <= w - sidePad) return 5;
        if (x >= w - sidePad - 2 * iconW - gap && x < w - sidePad - iconW) return 4;
        if (x >= wordsStart && x < wordsEnd) {
            int slotW = (wordsEnd - wordsStart) / 3;
            int s = 1 + (int) ((x - wordsStart) / slotW);
            return Math.max(1, Math.min(3, s));
        }
        return -1;
    }

    private void onDown(MotionEvent ev) {
        pointerId = ev.getPointerId(0);
        cancelAll();
        // منطقة شريط الاقتراحات
        if (stripVisible && ev.getY() <= stripH) {
            int slot = stripSlotAt(ev.getX(), ev.getY());
            if (slot >= 0) {
                stripTouched = true;
                stripDownSlot = slot;
                pressedSlot = slot;
                invalidate();
                pressFeedback();
                return;
            }
        }
        stripTouched = false;
        LaidKey k = findKey(ev.getX(), ev.getY());
        pointerId = ev.getPointerId(0);
        if (k == null) return;
        touched = k;
        k.pressed = true;
        longPressedFired = false;
        repeatCount = 0;
        spaceCursorMode = false;
        cursorStepsEmitted = 0;
        if (k.key.type == Key.SPACE) spaceStartX = ev.getX();
        invalidate();
        pressFeedback();
        showPreview(k);
        // بدء تتبّع السحب (Glide): على حرف فقط عند تفعيل الميزة
        glideMode = false;
        glidePts.clear();
        glideLetters.setLength(0);
        if (glideEnabled && k.key.type == Key.CHAR && k.key.text != null
                && k.key.text.length() == 1) {
            glideStartX = ev.getX();
            glideStartY = ev.getY();
            glideLastX = ev.getX();
            glideLastY = ev.getY();
            glidePts.add(new float[]{ev.getX(), ev.getY()});
            glideLetters.append(k.key.text);
        }
        if (k.key.code == Key.CODE_BACKSPACE) {
            startRepeat();
        } else if (k.key.type == Key.CHAR && k.key.alts != null && !k.key.alts.isEmpty()) {
            scheduleLongPress(k, false);
        } else if (k.key.code == Key.CODE_SHIFT || k.key.code == Key.CODE_LANG
                || k.key.code == Key.CODE_MODE_NUM) {
            scheduleLongPress(k, true);
        }
    }

    private void onMove(MotionEvent ev) {
        if (stripTouched) {
            int idx = ev.findPointerIndex(pointerId);
            if (idx < 0) return;
            int slot = stripSlotAt(ev.getX(idx), ev.getY(idx));
            if (slot != pressedSlot) {
                pressedSlot = slot;
                invalidate();
            }
            return;
        }
        if (touched == null) return;
        int idx = ev.findPointerIndex(pointerId);
        if (idx < 0) return;
        float x = ev.getX(idx), y = ev.getY(idx);

        if (altPopup != null) {
            updateAltSelection(x, y);
            return;
        }

        // تتبّع السحب (Glide): جمع نقاط المسار وتفعيل الوضع بعد مسافة كافية
        if (glidePts != null && !glidePts.isEmpty()) {
            float dx = x - glideLastX, dy = y - glideLastY;
            if (dx * dx + dy * dy > dp(5) * dp(5)) {
                glidePts.add(new float[]{x, y});
                glideLastX = x;
                glideLastY = y;
                LaidKey gk = findKey(x, y);
                if (gk != null && gk.key.type == Key.CHAR && gk.key.text != null
                        && gk.key.text.length() == 1) {
                    String ch = gk.key.text;
                    if (glideLetters.charAt(glideLetters.length() - 1) != ch.charAt(0)) {
                        glideLetters.append(ch);
                    }
                }
                invalidate();
            }
            if (!glideMode) {
                float sdx = x - glideStartX, sdy = y - glideStartY;
                if (sdx * sdx + sdy * sdy > dp(26) * dp(26)) {
                    // تفعيل وضع السحب: إلغاء الضغط المطوّل والمعاينة والتكرار
                    glideMode = true;
                    cancelLongPressTimer();
                    stopRepeat();
                    hidePreview();
                    if (touched != null) touched.pressed = false;
                    invalidate();
                }
            }
            if (glideMode) return; // لا انتقاء مفاتيح أثناء السحب
        }

        // سحب على المسافة ← تحريك المؤشر
        if (touched.key.type == Key.SPACE) {
            float dx = x - spaceStartX;
            if (!spaceCursorMode && Math.abs(dx) > dp(20)) {
                spaceCursorMode = true;
                hidePreview();
            }
            if (spaceCursorMode) {
                int steps = (int) (dx / dp(16));
                int delta = steps - cursorStepsEmitted;
                if (delta != 0) {
                    cursorStepsEmitted = steps;
                    if (listener != null) listener.onCursorMove(delta);
                }
            }
            return;
        }

        LaidKey k = findKey(x, y);
        if (k != touched) {
            if (touched != null) touched.pressed = false;
            stopRepeat();
            touched = k;
            if (k != null) {
                k.pressed = true;
                pressFeedback();
                showPreview(k);
                if (k.key.code == Key.CODE_BACKSPACE) startRepeat();
            } else {
                hidePreview();
            }
            invalidate();
        }
    }

    private void onUp(boolean cancelled, MotionEvent ev) {
        stopRepeat();
        if (stripTouched) {
            stripTouched = false;
            int slot = stripDownSlot;
            pressedSlot = -1;
            stripDownSlot = -1;
            invalidate();
            if (!cancelled && slot >= 0 && stripListener != null
                    && stripSlotAt(ev.getX(), ev.getY()) == slot) {
                if (slot == 0) stripListener.onStripAction(STRIP_CLIP, -1);
                else if (slot == 5) stripListener.onStripAction(STRIP_EDIT, -1);
                else if (slot == 4) stripListener.onStripAction(STRIP_SHIELD, -1);
                else {
                    int wi = slot - 1;
                    if (wi < suggestions.size()) stripListener.onStripAction(STRIP_WORD, wi);
                }
            }
            return;
        }
        // إنهاء الكتابة بالسحب
        if (glideMode) {
            glideMode = false;
            List<float[]> pts = new ArrayList<>(glidePts);
            String letters = glideLetters.toString();
            clearGlide();
            if (touched != null) { touched.pressed = false; touched = null; }
            invalidate();
            hidePreview();
            if (!cancelled && letters.length() >= 2 && glideListener != null) {
                pressFeedback();
                glideListener.onGlide(pts, letters);
            }
            return;
        }
        if (altPopup != null) {
            if (!cancelled && altSelected >= 0 && altSelected < altList.size() && listener != null) {
                listener.onText(altList.get(altSelected));
            }
            dismissAltPopup();
            hidePreview();
            if (touched != null) { touched.pressed = false; touched = null; invalidate(); }
            return;
        }
        if (touched != null) {
            touched.pressed = false;
            Key k = touched.key;
            boolean fire = !cancelled && !longPressedFired && !spaceCursorMode
                    && (k.code != Key.CODE_BACKSPACE || repeatCount == 0);
            touched = null;
            spaceCursorMode = false;
            invalidate();
            if (fire && listener != null) listener.onKey(k);
        }
        hidePreview();
    }

    // ---------- التكرار والمسح المطوّل ----------

    private void startRepeat() {
        stopRepeat();
        repeatRunnable = new Runnable() {
            @Override public void run() {
                if (touched == null || touched.key.code != Key.CODE_BACKSPACE) return;
                repeatCount++;
                if (listener != null) listener.onKey(touched.key);
                if (repeatCount % 8 == 0) pressFeedback();
                // تسريع تدريجي: 55ms ثم 35ms ثم 20ms
                long delay = repeatCount < 8 ? 55 : (repeatCount < 22 ? 35 : 20);
                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(repeatRunnable, Math.max(320, longPressDelay));
    }

    private void stopRepeat() {
        if (repeatRunnable != null) {
            handler.removeCallbacks(repeatRunnable);
            repeatRunnable = null;
        }
    }

    private void scheduleLongPress(final LaidKey k, final boolean notify) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (touched != k || longPressedFired || glideMode) return;
                longPressedFired = true;
                if (notify) {
                    if (listener != null) listener.onKeyLongPress(k.key);
                } else {
                    showAltPopup(k);
                }
            }
        }, longPressDelay);
    }

    // ---------- نافذة المعاينة ----------

    private void showPreview(LaidKey lk) {
        if (lk.key.type != Key.CHAR) { hidePreview(); return; }
        String label = displayLabel(lk.key);
        if (label == null || label.length() == 0) { hidePreview(); return; }

        if (previewPopup == null) {
            previewText = new TextView(getContext());
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(theme.popupBg);
            previewText.setBackground(bg);
            previewText.setPadding(dp(16), dp(8), dp(16), dp(8));
            previewText.setTextSize(28);
            previewText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            previewPopup = new PopupWindow(previewText,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, false);
            previewPopup.setContentView(previewText);
        }
        previewText.setText(label);
        previewText.setTextColor(theme.keyText);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(theme.popupBg);
        previewText.setBackground(bg);

        previewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int pw = previewText.getMeasuredWidth(), ph = previewText.getMeasuredHeight();
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        int x = loc[0] + (int) lk.r.centerX() - pw / 2;
        x = Math.max(loc[0], Math.min(x, loc[0] + getWidth() - pw));
        int y = loc[1] + (int) lk.r.top - ph - dp(4);
        try {
            if (previewPopup.isShowing()) previewPopup.update(x, y, -1, -1);
            else previewPopup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        } catch (Exception ignored) {}
    }

    private void hidePreview() {
        if (previewPopup != null && previewPopup.isShowing()) {
            try { previewPopup.dismiss(); } catch (Exception ignored) {}
        }
    }

    // ---------- نافذة البدائل (الضغط المطوّل) ----------

    private void showAltPopup(LaidKey lk) {
        dismissAltPopup();
        hidePreview();
        altSourceKey = lk;
        altList = lk.key.alts;
        if (altList == null || altList.isEmpty()) return;

        altContent = new LinearLayout(getContext());
        altContent.setOrientation(LinearLayout.HORIZONTAL);
        altContent.setPadding(dp(8), dp(6), dp(8), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(theme.popupBg);
        altContent.setBackground(bg);

        for (String a : altList) {
            TextView tv = new TextView(getContext());
            tv.setText(a);
            tv.setTextSize(24);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setTextColor(theme.keyText);
            tv.setPadding(dp(12), dp(6), dp(12), dp(6));
            tv.setGravity(Gravity.CENTER);
            altContent.addView(tv);
        }

        altContent.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int pw = Math.min(altContent.getMeasuredWidth(), getWidth() - dp(8));
        int ph = altContent.getMeasuredHeight();

        int[] loc = new int[2];
        getLocationOnScreen(loc);
        int x = loc[0] + (int) lk.r.centerX() - pw / 2;
        x = Math.max(dp(4), Math.min(x, loc[0] + getWidth() - pw - dp(4)));
        int y = loc[1] + (int) lk.r.top - ph - dp(6);

        altPopup = new PopupWindow(altContent, pw, ph, false);
        altPopup.setContentView(altContent);
        altSelected = -1;
        try {
            altPopup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        } catch (Exception e) {
            altPopup = null;
        }
    }

    private void updateAltSelection(float viewX, float viewY) {
        if (altContent == null) return;
        int[] vloc = new int[2];
        getLocationOnScreen(vloc);
        int sx = vloc[0] + (int) viewX, sy = vloc[1] + (int) viewY;
        int newSel = -1;
        for (int i = 0; i < altContent.getChildCount(); i++) {
            TextView tv = (TextView) altContent.getChildAt(i);
            int[] cl = new int[2];
            tv.getLocationOnScreen(cl);
            if (sx >= cl[0] && sx <= cl[0] + tv.getWidth()
                    && sy >= cl[1] - dp(14) && sy <= cl[1] + tv.getHeight() + dp(14)) {
                newSel = i;
                break;
            }
        }
        if (newSel != altSelected) {
            altSelected = newSel;
            for (int i = 0; i < altContent.getChildCount(); i++) {
                TextView tv = (TextView) altContent.getChildAt(i);
                if (i == altSelected) {
                    GradientDrawable g = new GradientDrawable();
                    g.setCornerRadius(dp(8));
                    g.setColor(theme.keyBgAction);
                    tv.setBackground(g);
                    tv.setTextColor(theme.keyTextAction);
                } else {
                    tv.setBackground(null);
                    tv.setTextColor(theme.keyText);
                }
            }
            if (altSelected >= 0) pressFeedback();
        }
    }

    private void dismissAltPopup() {
        if (altPopup != null && altPopup.isShowing()) {
            try { altPopup.dismiss(); } catch (Exception ignored) {}
        }
        altPopup = null;
        altContent = null;
        altList = null;
        altSelected = -1;
        altSourceKey = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAll();
        super.onDetachedFromWindow();
    }
}
