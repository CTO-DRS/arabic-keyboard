package com.zai.arabickeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * مكتبة مكونات الواجهة العصرية — DRS Smart v2.6
 * بطاقات دائرية، أزرار متدرجة، مفاتيح تبديل ملونة، أزرار مقسمة، حقول أنيقة.
 * نظام ألوان موحد داكن أنيق في كل شاشات التطبيق.
 */
public final class UiKit {

    // ==================== نظام الألوان الموحد ====================
    public static final int PAGE_BG      = 0xFF070B18;
    public static final int CARD_BG      = 0xFF101731;
    public static final int CARD_STROKE  = 0xFF1F2A4D;
    public static final int HERO_START   = 0xFF2B1B67;
    public static final int HERO_MID     = 0xFF1A1F4E;
    public static final int HERO_END     = 0xFF101731;
    public static final int ACCENT       = 0xFF7C5CFF;
    public static final int ACCENT_DARK  = 0xFF5B3FE0;
    public static final int ACCENT_SOFT  = 0xFFB388FF;
    public static final int TEXT_MAIN    = 0xFFEEF1FF;
    public static final int TEXT_SUB     = 0xFF8E97C4;
    public static final int TEXT_FAINT   = 0xFF667099;
    public static final int GREEN        = 0xFF66D99A;
    public static final int AMBER        = 0xFFFFC93A;
    public static final int RED          = 0xFFFF7B6B;
    public static final int FIELD_BG     = 0xFF0B1226;
    public static final int FIELD_STROKE = 0xFF26305A;
    public static final int NAV_BG       = 0xFF0C1226;
    public static final int TILE_BG      = 0xFF141B3A;

    private UiKit() {}

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Typeface bold()   { return Typeface.create("sans-serif", Typeface.BOLD); }
    public static Typeface medium() { return Typeface.create("sans-serif-medium", Typeface.NORMAL); }

    // ==================== الخلفيات ====================

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable outlined(int color, float radiusDp, int strokeColor,
                                            float strokeDp, Context c) {
        GradientDrawable g = rounded(color, radiusDp, c);
        g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return g;
    }

    public static GradientDrawable heroGradient(Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{HERO_START, HERO_MID, HERO_END});
        g.setCornerRadius(dp(c, 24));
        return g;
    }

    public static GradientDrawable accentGradient(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        g.setColors(new int[]{ACCENT_DARK, ACCENT});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    // ==================== الحاويات ====================

    public static LinearLayout vstack(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hstack(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = vstack(c);
        card.setBackground(outlined(CARD_BG, 20, CARD_STROKE, 1, c));
        card.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        return card;
    }

    public static LinearLayout card(Context c, int bgColor, int strokeColor) {
        LinearLayout card = vstack(c);
        card.setBackground(outlined(bgColor, 20, strokeColor, 1, c));
        card.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        return card;
    }

    // ==================== النصوص ====================

    public static TextView text(Context c, String s, float sp, int color, boolean isBold) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(isBold ? bold() : Typeface.DEFAULT);
        tv.setLineSpacing(dp(c, 3), 1f);
        return tv;
    }

    public static TextView title(Context c, String s) {
        return text(c, s, 18, TEXT_MAIN, true);
    }

    public static TextView body(Context c, String s) {
        return text(c, s, 13.5f, TEXT_SUB, false);
    }

    public static TextView caption(Context c, String s) {
        return text(c, s, 12, TEXT_FAINT, false);
    }

    /** شارة صغيرة دائرية الحواف */
    public static TextView chip(Context c, String s, int bg, int stroke, int fg) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(11.5f);
        tv.setTypeface(medium());
        tv.setTextColor(fg);
        tv.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        tv.setBackground(outlined(bg, 14, stroke, 1, c));
        return tv;
    }

    // ==================== الأزرار ====================

    public static Button primaryButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        b.setBackground(accentGradient(c, 14));
        b.setTextColor(0xFFFFFFFF);
        return b;
    }

    public static Button secondaryButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        b.setBackground(outlined(TILE_BG, 14, ACCENT, 1.2f, c));
        b.setTextColor(ACCENT_SOFT);
        return b;
    }

    public static Button dangerButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        b.setBackground(outlined(0xFF1A0E12, 14, RED, 1.2f, c));
        b.setTextColor(RED);
        return b;
    }

    private static Button baseButton(Context c, String s, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(15);
        b.setTypeface(bold());
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setOnClickListener(l);
        b.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 50));
        lp.setMargins(0, dp(c, 14), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ==================== صف التبديل (مفتاح) ====================

    public interface BoolListener { void on(boolean b); }

    public static LinearLayout toggleRow(Context c, String label, String subtitle,
                                         boolean checked, final BoolListener listener) {
        LinearLayout row = hstack(c);
        LinearLayout col = vstack(c);
        col.addView(text(c, label, 14.5f, TEXT_MAIN, false));
        if (subtitle != null) col.addView(text(c, subtitle, 11.5f, TEXT_FAINT, false));
        row.addView(col, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(c);
        sw.setChecked(checked);
        try {
            sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                    checked ? ACCENT : 0xFF8E97C4));
            sw.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    checked ? 0x557C5CFF : 0xFF26305A));
        } catch (Throwable ignored) {}
        sw.setOnCheckedChangeListener((b, isChecked) -> {
            try {
                sw.setThumbTintList(android.content.res.ColorStateList.valueOf(
                        isChecked ? ACCENT : 0xFF8E97C4));
                sw.setTrackTintList(android.content.res.ColorStateList.valueOf(
                        isChecked ? 0x557C5CFF : 0xFF26305A));
            } catch (Throwable ignored) {}
            listener.on(isChecked);
        });
        row.addView(sw, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 10), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    // ==================== الأزرار المقسمة (Segmented) ====================

    public interface IntListener { void on(int i); }

    public static LinearLayout segmented(Context c, String[] labels, int selected,
                                         final IntListener listener) {
        LinearLayout wrap = vstack(c);
        wrap.setBackground(rounded(FIELD_BG, 12, c));
        wrap.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));

        LinearLayout row = hstack(c);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView seg = new TextView(c);
            seg.setText(labels[i]);
            seg.setTextSize(12.5f);
            seg.setTypeface(i == selected ? bold() : Typeface.DEFAULT);
            seg.setTextColor(i == selected ? 0xFFFFFFFF : TEXT_SUB);
            seg.setGravity(Gravity.CENTER);
            seg.setClickable(true);
            seg.setBackground(i == selected
                    ? accentGradient(c, 10)
                    : rounded(0x00000000, 10, c));
            seg.setOnClickListener(v -> {
                listener.on(idx);
                // إبراز الاختيار دون إعادة بناء كاملة
                for (int j = 0; j < row.getChildCount(); j++) {
                    TextView s = (TextView) row.getChildAt(j);
                    boolean on = j == idx;
                    s.setTypeface(on ? bold() : Typeface.DEFAULT);
                    s.setTextColor(on ? 0xFFFFFFFF : TEXT_SUB);
                    s.setBackground(on ? accentGradient(c, 10) : rounded(0x00000000, 10, c));
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(c, 36), 1f);
            lp.setMargins(dp(c, 2), 0, dp(c, 2), 0);
            row.addView(seg, lp);
        }
        wrap.addView(row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 8), 0, 0);
        wrap.setLayoutParams(lp);
        return wrap;
    }

    // ==================== الحقول ====================

    public static EditText field(Context c, String hint, int minLines) {
        EditText et = new EditText(c);
        et.setHint(hint);
        et.setHintTextColor(TEXT_FAINT);
        et.setTextColor(TEXT_MAIN);
        et.setTextSize(14);
        et.setMinLines(minLines);
        et.setBackground(outlined(FIELD_BG, 12, FIELD_STROKE, 1, c));
        et.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return et;
    }

    // ==================== مربع تحذير/ملاحظة ====================

    public static TextView notice(Context c, String s, int bg, int stroke, int fg) {
        TextView tv = text(c, s, 12.5f, fg, false);
        tv.setBackground(outlined(bg, 12, stroke, 1, c));
        tv.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        return tv;
    }

    /** صف عنصر شبكي قابل للنقر مع أيقونة مرسومة داخل شارة وعنوان ووصف */
    public static LinearLayout tile(Context c, int iconType, String titleStr,
                                    String subStr, View.OnClickListener l) {
        LinearLayout t = vstack(c);
        t.setGravity(Gravity.CENTER);
        t.setBackground(outlined(TILE_BG, 18, CARD_STROKE, 1, c));
        t.setClickable(true);
        t.setOnClickListener(l);

        // شارة الأيقونة: مربع دائري الحواف بلمسة لون التمييز
        android.widget.FrameLayout badge = new android.widget.FrameLayout(c);
        badge.setBackground(rounded(0x2E7C5CFF, 13, c));
        Icon ic = new Icon(c, iconType, ACCENT_SOFT);
        badge.addView(ic, new android.widget.FrameLayout.LayoutParams(
                dp(c, 22), dp(c, 22), Gravity.CENTER));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(c, 42), dp(c, 42));
        bLp.setMargins(0, 0, 0, dp(c, 8));
        t.addView(badge, bLp);

        TextView tt = text(c, titleStr, 13.5f, TEXT_MAIN, true);
        tt.setGravity(Gravity.CENTER);
        t.addView(tt);

        if (subStr != null) {
            TextView st = text(c, subStr, 10.5f, TEXT_FAINT, false);
            st.setGravity(Gravity.CENTER);
            t.addView(st);
        }
        return t;
    }
}
