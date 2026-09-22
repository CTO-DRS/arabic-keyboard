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
 * مكتبة مكونات الواجهة الزجاجية — DRS Smart v2.7
 * Glassmorphism حقيقي: بطاقات شبه شفافة بحدود مضيئة، أزرار متدرجة بنفسجية،
 * Ripple وضغط مرن عند اللمس، شارات أيقونات متوهجة — فوق خلفية AuroraBg.
 */
public final class UiKit {

    // ==================== نظام الألوان الزجاجي الموحد ====================
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
    public static final int TEXT_SUB     = 0xFFA9B2DC;
    public static final int TEXT_FAINT   = 0xFF7A84B2;
    public static final int GREEN        = 0xFF66D99A;
    public static final int AMBER        = 0xFFFFC93A;
    public static final int RED          = 0xFFFF7B6B;
    public static final int FIELD_BG     = 0xFF0B1226;
    public static final int FIELD_STROKE = 0xFF26305A;
    public static final int NAV_BG       = 0xFF0C1226;
    public static final int TILE_BG      = 0xFF141B3A;

    // ألوان الزجاج
    public static final int GLASS_TOP    = 0x1FFFFFFF;  // تعبئة علوية
    public static final int GLASS_BOTTOM = 0x0BFFFFFF;  // تعبئة سفلية
    public static final int GLASS_STROKE = 0x30FFFFFF;  // حد مضيء رقيق
    public static final int RIPPLE_WHITE = 0x33FFFFFF;

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
        // تدرج بنفسجي شبه شفاف — زجاج مصبغ بلمسة توهج
        g.setColors(new int[]{0x54301E6E, 0x381A1F4E, 0x2A101731});
        g.setCornerRadius(dp(c, 26));
        g.setStroke(Math.max(1, dp(c, 1)), 0x3C9D7BFF);
        return g;
    }

    public static GradientDrawable accentGradient(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{ACCENT_DARK, ACCENT, 0xFF9D7BFF});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    // ==================== الزجاج (Glassmorphism) ====================

    /** بطاقة زجاجية: تعبئة شبه شفافة متدرجة + حد مضيء رقيق */
    public static GradientDrawable glass(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{GLASS_TOP, GLASS_BOTTOM});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), GLASS_STROKE);
        return g;
    }

    /** حالة الضغط: زجاج أسطع بحد أوضح */
    public static GradientDrawable glassPressed(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{0x30FFFFFF, 0x1AFFFFFF});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x55FFFFFF);
        return g;
    }

    /** يضيف Ripple + توهج ضغط حول أي عنصر بخلفية جاهزة */
    public static void ripple(View v, android.graphics.drawable.Drawable bg,
                              int rippleColor, float radiusDp) {
        try {
            android.graphics.drawable.RippleDrawable rd =
                    new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(rippleColor),
                            bg, rounded(0xFFFFFFFF, radiusDp, v.getContext()));
            v.setBackground(rd);
        } catch (Throwable t) {
            v.setBackground(bg);
        }
    }

    /** انكماش مرن عند اللمس (Scale) دون التأثير على النقر */
    public static void pressScale(View v, float scale) {
        v.setOnTouchListener((v2, ev) -> {
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v2.animate().scaleX(scale).scaleY(scale).setDuration(90)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v2.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    break;
            }
            return false;
        });
    }

    /** توهج ناعم حول نص (أرقام الإحصاءات) */
    public static void glowText(TextView tv, int glowColor, float radiusDp, Context c) {
        tv.setShadowLayer(dp(c, radiusDp), 0, 0, glowColor);
    }

    /** شارة أيقونة زجاجية متوهجة: حاوية متدرجة + أيقونة مرسومة */
    public static android.widget.FrameLayout iconBadge(Context c, int iconType,
                                                       float badgeDp, float iconDp) {
        android.widget.FrameLayout b = new android.widget.FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setColors(new int[]{0x477C5CFF, 0x265B3FE0});
        bg.setCornerRadius(dp(c, badgeDp * 0.34f));
        bg.setStroke(Math.max(1, dp(c, 1)), 0x599D7BFF);
        b.setBackground(bg);
        Icon ic = new Icon(c, iconType, 0xFFD4C6FF);
        b.addView(ic, new android.widget.FrameLayout.LayoutParams(
                dp(c, iconDp), dp(c, iconDp), Gravity.CENTER));
        return b;
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
        card.setBackground(glass(c, 24));
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

    /** شارة صغيرة دائرية الحواف — زجاجية */
    public static TextView chip(Context c, String s, int bg, int stroke, int fg) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(11.5f);
        tv.setTypeface(medium());
        tv.setTextColor(fg);
        tv.setPadding(dp(c, 11), dp(c, 5), dp(c, 11), dp(c, 5));
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{0x33FFFFFF, 0x1AFFFFFF});
        g.setCornerRadius(dp(c, 16));
        g.setStroke(Math.max(1, dp(c, 1)), stroke);
        tv.setBackground(g);
        return tv;
    }

    // ==================== الأزرار ====================

    public static Button primaryButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        ripple(b, accentGradient(c, 18), 0x40FFFFFF, 18);
        b.setTextColor(0xFFFFFFFF);
        return b;
    }

    public static Button secondaryButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        // زجاج شفاف بحد بنفسجي مضيء
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{0x1FFFFFFF, 0x0FFFFFFF});
        g.setCornerRadius(dp(c, 18));
        g.setStroke(Math.max(1, dp(c, 1.2f)), 0x7A7C5CFF);
        ripple(b, g, 0x307C5CFF, 18);
        b.setTextColor(ACCENT_SOFT);
        return b;
    }

    public static Button dangerButton(Context c, String s, View.OnClickListener l) {
        Button b = baseButton(c, s, l);
        ripple(b, outlined(0x1AFF7B6B, 18, RED, 1.2f, c), 0x30FF7B6B, 18);
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
        // وعاء زجاجي شفاف
        GradientDrawable wg = new GradientDrawable();
        wg.setOrientation(GradientDrawable.Orientation.TL_BR);
        wg.setColors(new int[]{0x14FFFFFF, 0x09FFFFFF});
        wg.setCornerRadius(dp(c, 14));
        wg.setStroke(Math.max(1, dp(c, 1)), 0x2EFFFFFF);
        wrap.setBackground(wg);
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
        // حقل زجاجي: تعبئة شبه شفافة + حد مضيء
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{0x14FFFFFF, 0x09FFFFFF});
        g.setCornerRadius(dp(c, 14));
        g.setStroke(Math.max(1, dp(c, 1)), 0x2EFFFFFF);
        et.setBackground(g);
        et.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return et;
    }

    // ==================== مربع تحذير/ملاحظة ====================

    public static TextView notice(Context c, String s, int bg, int stroke, int fg) {
        TextView tv = text(c, s, 12.5f, fg, false);
        GradientDrawable g = new GradientDrawable();
        g.setOrientation(GradientDrawable.Orientation.TL_BR);
        g.setColors(new int[]{(bg & 0x00FFFFFF) | 0x23000000, (bg & 0x00FFFFFF) | 0x16000000});
        g.setCornerRadius(dp(c, 14));
        g.setStroke(Math.max(1, dp(c, 1)), (stroke & 0x00FFFFFF) | 0x48000000);
        tv.setBackground(g);
        tv.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        return tv;
    }

    /** صف عنصر شبكي قابل للنقر — بطاقة زجاجية بأيقونة داخل شارة متوهجة وضغط مرن */
    public static LinearLayout tile(Context c, int iconType, String titleStr,
                                    String subStr, View.OnClickListener l) {
        LinearLayout t = vstack(c);
        t.setGravity(Gravity.CENTER);
        t.setClickable(true);
        t.setOnClickListener(l);
        ripple(t, glass(c, 22), RIPPLE_WHITE, 22);
        pressScale(t, 0.96f);
        t.setPadding(dp(c, 12), dp(c, 14), dp(c, 12), dp(c, 14));

        // شارة الأيقونة الزجاجية المتوهجة
        android.widget.FrameLayout badge = iconBadge(c, iconType, 52, 26);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(c, 52), dp(c, 52));
        bLp.setMargins(0, 0, 0, dp(c, 10));
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
