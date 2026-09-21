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
import java.util.List;

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

    // أبعاد
    private int baseKeyH;
    private int keyH, rowGap, keyGap, sidePad, topPad, bottomPad, radius;
    private float keyHeightScale = 1f;

    // سحب المسافة لتحريك المؤشر
    private boolean spaceCursorMode;
    private float spaceStartX;
    private int cursorStepsEmitted;

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

        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hintPaint.setTypeface(Typeface.DEFAULT);
        hintPaint.setTextAlign(Paint.Align.RIGHT);

        if (theme == null) theme = ThemeSet.light(ThemeSet.ACCENTS[0]);
    }

    private int res(int id) { return getResources().getDimensionPixelSize(id); }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    // ==================== ضبط الحالة من الخارج ====================

    public void setListener(Listener l) { listener = l; }

    public void setTheme(ThemeSet t) {
        theme = t;
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

    public void cancelAll() {
        stopRepeat();
        handler.removeCallbacksAndMessages(null);
        hidePreview();
        dismissAltPopup();
        if (touched != null) { touched.pressed = false; touched = null; }
    }

    public int getDesiredHeight() {
        int n = rows.size();
        if (n == 0) return 0;
        return topPad + bottomPad + n * keyH + (n - 1) * rowGap;
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
        float y = topPad;
        for (Row row : rows) {
            int n = row.keys.size();
            float total = 0;
            for (Key k : row.keys) total += k.weight;
            float avail = w - 2 * sidePad - (n - 1) * (float) keyGap;
            float unit = avail / total;
            float x = sidePad;
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
        canvas.drawColor(theme.kbBg);
        for (List<LaidKey> lr : laid) {
            for (LaidKey lk : lr) {
                drawKey(canvas, lk);
            }
        }
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
                onUp(ev.getActionMasked() == MotionEvent.ACTION_CANCEL);
                return true;
        }
        return super.onTouchEvent(ev);
    }

    private void onDown(MotionEvent ev) {
        pointerId = ev.getPointerId(0);
        cancelAll();
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
        if (k.key.code == Key.CODE_BACKSPACE) {
            startRepeat();
        } else if (k.key.type == Key.CHAR && k.key.alts != null && !k.key.alts.isEmpty()) {
            scheduleLongPress(k, false);
        } else if (k.key.code == Key.CODE_SHIFT || k.key.code == Key.CODE_LANG) {
            scheduleLongPress(k, true);
        }
    }

    private void onMove(MotionEvent ev) {
        if (touched == null) return;
        int idx = ev.findPointerIndex(pointerId);
        if (idx < 0) return;
        float x = ev.getX(idx), y = ev.getY(idx);

        if (altPopup != null) {
            updateAltSelection(x, y);
            return;
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

    private void onUp(boolean cancelled) {
        stopRepeat();
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
        handler.postDelayed(repeatRunnable, 380);
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
                if (touched != k || longPressedFired) return;
                longPressedFired = true;
                if (notify) {
                    if (listener != null) listener.onKeyLongPress(k.key);
                } else {
                    showAltPopup(k);
                }
            }
        }, 380);
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
