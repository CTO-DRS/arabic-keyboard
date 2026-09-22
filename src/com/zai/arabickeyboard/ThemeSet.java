package com.zai.arabickeyboard;

/**
 * نظام الثيمات الكامل — 12 ثيماً مسمّى بهوية DRS Smart:
 * كحلي داكن بتوهج بنفسجي، سايبر، نيون، منتصف الليل، ذهبي ملكي، وردة ذهبية...
 * كل ثيم يحدد كل الألوان + لون التمييز الخاص به.
 */
public class ThemeSet {
    public int kbBg;             // خلفية اللوحة
    public int kbBgTop;          // أعلى التدرج (يساوي kbBg إن لا تدرج)
    public int keyBg;            // خلفية زر عادي
    public int keyBgFunc;        // خلفية زر وظيفي
    public int keyBgAction;      // خلفية زر التنفيذ (لون التمييز)
    public int keyBgPressed;     // زر عادي عند الضغط
    public int keyBgFuncPressed;
    public int keyBgActionPressed;
    public int keyText;          // لون نص الأزرار العادية
    public int keyTextFunc;      // لون نص الأزرار الوظيفية
    public int keyTextAction;    // لون نص زر التنفيذ
    public int hintText;         // نصوص ثانوية
    public int popupBg;          // خلفية النوافذ المنبثقة
    public int stripWord;        // لون كلمة الاقتراح الأولى (المرشّحة)
    public boolean dark;
    public boolean gradient;     // تفعيل تدرج الخلفية

    /** أسماء الثيمات (تظهر في الإعدادات) */
    public static final String[] NAMES = {
            "تلقائي", "نهاري", "ليلي", "AMOLED",
            "DRS ذكي", "سايبر", "نيون", "منتصف الليل",
            "وردة ذهبية", "غروب", "زمرد", "ملكي",
            "بحري", "رملي", "شرقي", "فضائي"
    };

    /** ألوان معاينة صغيرة لكل ثيم (خلفية، مفاتيح، تمييز) */
    public static final int[][] PREVIEW = {
            {0xFFE9EBEF, 0xFFFFFFFF, 0xFF4285F4},   // 0 تلقائي
            {0xFFE9EBEF, 0xFFFFFFFF, 0xFF1A73E8},   // 1 نهاري
            {0xFF1B1D21, 0xFF35383D, 0xFF7C4DFF},   // 2 ليلي
            {0xFF000000, 0xFF1A1C1E, 0xFF26A69A},   // 3 AMOLED
            {0xFF0D1228, 0xFF1A2142, 0xFF7C4DFF},   // 4 DRS ذكي
            {0xFF0A0F1E, 0xFF13204A, 0xFF00E5FF},   // 5 سايبر
            {0xFF12081F, 0xFF241040, 0xFFE040FB},   // 6 نيون
            {0xFF101418, 0xFF232A31, 0xFF4FC3F7},   // 7 منتصف الليل
            {0xFFF9EEF2, 0xFFFFFFFF, 0xFFD81B60},   // 8 وردة ذهبية
            {0xFF2A1220, 0xFF471E2E, 0xFFFF7043},   // 9 غروب
            {0xFF0E1F18, 0xFF1B3A2C, 0xFF34D399},   // 10 زمرد
            {0xFF1A1608, 0xFF2E280F, 0xFFFFC93A},   // 11 ملكي
            {0xFFE1F0FA, 0xFFFFFFFF, 0xFF0277BD},   // 12 بحري
            {0xFFF6EFE3, 0xFFFFFFFF, 0xFFB26A00},   // 13 رملي
            {0xFF230915, 0xFF3D1128, 0xFFFF5252},   // 14 شرقي
            {0xFF0C0622, 0xFF1D1040, 0xFF651FFF}    // 15 فضائي
    };

    /** فهرس الثيم الافتراضي (هوية DRS) */
    public static final int DEFAULT_PRESET = 4;

    /** ألوان تمييز مخصصة يختارها المستخدم (تُطبّق على أي ثيم) */
    public static final int[] ACCENT_COLORS = {
            0xFF1A73E8,   // 1 أزرق
            0xFF7C4DFF,   // 2 بنفسجي
            0xFF00BCD4,   // 3 سماوي
            0xFF34D399,   // 4 أخضر
            0xFFFFC93A,   // 5 ذهبي
            0xFFFF7043,   // 6 برتقالي
            0xFFD81B60,   // 7 وردي
            0xFFE53935    // 8 أحمر
    };
    public static final String[] ACCENT_NAMES = {
            "أزرق", "بنفسجي", "سماوي", "أخضر", "ذهبي", "برتقالي", "وردي", "أحمر"
    };

    private static int shade(int c, float f) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r = Math.max(0, Math.min(255, (int) (r * f)));
        g = Math.max(0, Math.min(255, (int) (g * f)));
        b = Math.max(0, Math.min(255, (int) (b * f)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** ترجمة اختيار المستخدم إلى ثيم فعلي (مع لون تمييز مخصص اختياري) */
    public static ThemeSet resolve(int preset, boolean systemNight, int accent) {
        ThemeSet t = resolve(preset, systemNight);
        if (accent >= 1 && accent <= ACCENT_COLORS.length) {
            applyAccent(t, ACCENT_COLORS[accent - 1]);
        }
        return t;
    }

    /** تطبيق لون تمييز مخصص على ثيم جاهز */
    private static void applyAccent(ThemeSet t, int action) {
        t.keyBgAction = action;
        t.keyBgActionPressed = shade(action, t.dark ? 0.8f : 0.85f);
        t.keyTextAction = isGold(action) ? 0xFF332600 : 0xFFFFFFFF;
        t.stripWord = t.dark ? shade(action, 1.25f) : action;
    }
    public static ThemeSet resolve(int preset, boolean systemNight) {
        if (preset < 0 || preset >= NAMES.length) preset = DEFAULT_PRESET;
        if (preset == 0) return systemNight ? dark(0xFF1B1D21, 0xFF35383D, 0xFF7C4DFF, false)
                                            : light(0xFFE9EBEF, 0xFFFFFFFF, 0xFF1A73E8, false);
        switch (preset) {
            case 1: return light(0xFFE9EBEF, 0xFFFFFFFF, 0xFF1A73E8, false);
            case 2: return dark(0xFF1B1D21, 0xFF35383D, 0xFF7C4DFF, false);
            case 3: return dark(0xFF000000, 0xFF1A1C1E, 0xFF26A69A, false);
            case 4: return drsSmart();
            case 5: return dark(0xFF0A0F1E, 0xFF13204A, 0xFF00E5FF, true);
            case 6: return dark(0xFF12081F, 0xFF241040, 0xFFE040FB, true);
            case 7: return dark(0xFF101418, 0xFF232A31, 0xFF4FC3F7, true);
            case 8: return light(0xFFF9EEF2, 0xFFFFFFFF, 0xFFD81B60, false);
            case 9: return dark(0xFF2A1220, 0xFF471E2E, 0xFFFF7043, true);
            case 10: return dark(0xFF0E1F18, 0xFF1B3A2C, 0xFF34D399, true);
            case 11: return dark(0xFF1A1608, 0xFF2E280F, 0xFFFFC93A, true);
            case 12: return light(0xFFE1F0FA, 0xFFFFFFFF, 0xFF0277BD, false);
            case 13: return light(0xFFF6EFE3, 0xFFFFFFFF, 0xFFB26A00, false);
            case 14: return dark(0xFF230915, 0xFF3D1128, 0xFFFF5252, true);
            case 15: return dark(0xFF0C0622, 0xFF1D1040, 0xFF651FFF, true);
            default: return drsSmart();
        }
    }

    /** الثيم الرئيسي: كحلي عميق بتوهج بنفسجي — هوية DRS Smart */
    private static ThemeSet drsSmart() {
        ThemeSet t = new ThemeSet();
        t.dark = true;
        t.gradient = true;
        t.kbBg = 0xFF0D1228;
        t.kbBgTop = 0xFF131A38;
        t.keyBg = 0xFF1A2142;
        t.keyBgFunc = 0xFF141A36;
        t.keyBgAction = 0xFF7C4DFF;
        t.keyBgPressed = 0xFF253060;
        t.keyBgFuncPressed = 0xFF232C55;
        t.keyBgActionPressed = 0xFF6838E8;
        t.keyText = 0xFFEDF0FF;
        t.keyTextFunc = 0xFFB9C2F0;
        t.keyTextAction = 0xFFFFFFFF;
        t.hintText = 0xFF8A93C4;
        t.popupBg = 0xFF222B52;
        t.stripWord = 0xFFB388FF;
        return t;
    }

    private static ThemeSet light(int bg, int key, int action, boolean grad) {
        ThemeSet t = new ThemeSet();
        t.dark = false;
        t.gradient = grad;
        t.kbBg = bg;
        t.kbBgTop = shade(bg, 0.96f);
        t.keyBg = key;
        t.keyBgFunc = shade(key, 0.82f);
        t.keyBgAction = action;
        t.keyBgPressed = shade(key, 0.86f);
        t.keyBgFuncPressed = shade(t.keyBgFunc, 0.86f);
        t.keyBgActionPressed = shade(action, 0.85f);
        t.keyText = 0xFF1F2430;
        t.keyTextFunc = 0xFF3C4043;
        t.keyTextAction = isGold(action) ? 0xFF332600 : 0xFFFFFFFF;
        t.hintText = 0xFF80868B;
        t.popupBg = key;
        t.stripWord = action;
        return t;
    }

    private static ThemeSet dark(int bg, int key, int action, boolean grad) {
        ThemeSet t = new ThemeSet();
        t.dark = true;
        t.gradient = grad;
        t.kbBg = bg;
        t.kbBgTop = shade(bg, 1.45f);
        t.keyBg = key;
        t.keyBgFunc = shade(bg, 1.25f);
        t.keyBgAction = action;
        t.keyBgPressed = shade(key, 1.35f);
        t.keyBgFuncPressed = shade(t.keyBgFunc, 1.6f);
        t.keyBgActionPressed = shade(action, 0.8f);
        t.keyText = 0xFFE8EAED;
        t.keyTextFunc = 0xFFDADCE0;
        t.keyTextAction = isGold(action) ? 0xFF332600 : 0xFFFFFFFF;
        t.hintText = shade(0xFF9AA0A6, 1f);
        t.popupBg = shade(key, 1.15f);
        t.stripWord = action;
        return t;
    }

    private static boolean isGold(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return r > 200 && g > 150 && b < 120;
    }
}
