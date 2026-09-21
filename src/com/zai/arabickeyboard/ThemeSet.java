package com.zai.arabickeyboard;

/** لوحة ألوان لوحة المفاتيح مع دعم ألوان التمييز */
public class ThemeSet {
    public int kbBg;             // خلفية اللوحة
    public int keyBg;            // خلفية زر عادي
    public int keyBgFunc;        // خلفية زر وظيفي
    public int keyBgAction;      // خلفية زر التنفيذ (لون التمييز)
    public int keyBgPressed;     // زر عادي عند الضغط
    public int keyBgFuncPressed;
    public int keyBgActionPressed;
    public int keyText;          // لون نص الأزرار العادية
    public int keyTextFunc;      // لون نص الأزرار الوظيفية
    public int keyTextAction;    // لون نص زر التنفيذ
    public int hintText;         // نصوص ثانوية (تلميحات المسافة/البدائل)
    public int popupBg;          // خلفية النوافذ المنبثقة
    public boolean dark;

    /** ألوان التمييز المتاحة */
    public static final int[] ACCENTS = {
            0xFF4285F4, // أزرق
            0xFF26A69A, // فيروزي
            0xFFAB47BC, // بنفسجي
            0xFFFFB300, // ذهبي
            0xFFEF5350  // أحمر
    };

    private static int shade(int c, float f) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r = Math.max(0, Math.min(255, (int) (r * f)));
        g = Math.max(0, Math.min(255, (int) (g * f)));
        b = Math.max(0, Math.min(255, (int) (b * f)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** pref: -1 تلقائي، 0 نهاري، 1 ليلي، 2 AMOLED — accentIdx: فهرس ACCENTS */
    public static ThemeSet resolve(int pref, int accentIdx, boolean systemNight) {
        int t = pref;
        if (t < 0) t = systemNight ? 1 : 0;
        int accent = ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, accentIdx))];
        if (t == 0) return light(accent);
        if (t == 2) return amoled(accent);
        return dark(accent);
    }

    private static int accentText(int accent) {
        // الذهبي يحتاج نصاً داكناً لسهولة القراءة
        if (accent == ACCENTS[3]) return 0xFF332600;
        return 0xFFFFFFFF;
    }

    public static ThemeSet light(int accent) {
        ThemeSet t = new ThemeSet();
        t.dark = false;
        t.kbBg = 0xFFE9EBEF;
        t.keyBg = 0xFFFFFFFF;
        t.keyBgFunc = 0xFFD3D7DE;
        t.keyBgAction = accent;
        t.keyBgPressed = shade(t.keyBg, 0.86f);
        t.keyBgFuncPressed = shade(t.keyBgFunc, 0.86f);
        t.keyBgActionPressed = shade(t.keyBgAction, 0.85f);
        t.keyText = 0xFF1F2430;
        t.keyTextFunc = 0xFF3C4043;
        t.keyTextAction = accentText(accent);
        t.hintText = 0xFF80868B;
        t.popupBg = 0xFFFFFFFF;
        return t;
    }

    public static ThemeSet dark(int accent) {
        ThemeSet t = new ThemeSet();
        t.dark = true;
        t.kbBg = 0xFF1B1D21;
        t.keyBg = 0xFF35383D;
        t.keyBgFunc = 0xFF26282C;
        t.keyBgAction = accent;
        t.keyBgPressed = shade(t.keyBg, 1.35f);
        t.keyBgFuncPressed = shade(t.keyBgFunc, 1.6f);
        t.keyBgActionPressed = shade(t.keyBgAction, 0.8f);
        t.keyText = 0xFFE8EAED;
        t.keyTextFunc = 0xFFDADCE0;
        t.keyTextAction = accentText(accent);
        t.hintText = 0xFF9AA0A6;
        t.popupBg = 0xFF3C4043;
        return t;
    }

    public static ThemeSet amoled(int accent) {
        ThemeSet t = new ThemeSet();
        t.dark = true;
        t.kbBg = 0xFF000000;
        t.keyBg = 0xFF1A1C1E;
        t.keyBgFunc = 0xFF0F1113;
        t.keyBgAction = accent;
        t.keyBgPressed = shade(t.keyBg, 2.1f);
        t.keyBgFuncPressed = shade(t.keyBgFunc, 2.6f);
        t.keyBgActionPressed = shade(t.keyBgAction, 0.8f);
        t.keyText = 0xFFE8EAED;
        t.keyTextFunc = 0xFFDADCE0;
        t.keyTextAction = accentText(accent);
        t.hintText = 0xFF8A8F94;
        t.popupBg = 0xFF2B2D30;
        return t;
    }
}
