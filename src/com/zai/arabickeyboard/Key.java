package com.zai.arabickeyboard;

import java.util.ArrayList;
import java.util.List;

/** نموذج زر واحد في لوحة المفاتيح */
public class Key {
    // أنواع الأزرار
    public static final int CHAR = 0;    // حرف/رمز يُكتب
    public static final int FUNC = 1;    // زر وظيفي (shift, مسح, ...)
    public static final int ACTION = 2;  // زر الإدخال/تنفيذ
    public static final int SPACE = 3;   // المسافة

    // أكواد الأزرار الخاصة
    public static final int CODE_SHIFT = -1;
    public static final int CODE_MODE_NUM = -2;   // ؟١٢٣
    public static final int CODE_LANG = -3;       // تبديل اللغة
    public static final int CODE_EMOJI = -4;      // لوحة الإيموجي
    public static final int CODE_ENTER = -5;
    public static final int CODE_BACKSPACE = -6;
    public static final int CODE_PAGE2 = -7;      // =\<
    public static final int CODE_BACK_TO_ABC = -8;// عربي/ABC
    public static final int CODE_PAGE1 = -9;      // 123
    public static final int CODE_SPACE = -10;     // المسافة

    public String label;        // النص المعروض عادي
    public String text;         // النص المُدخل عادي
    public String shiftLabel;   // النص المعروض مع Shift
    public String shiftText;    // النص المُدخل مع Shift
    public List<String> alts;   // خيارات الضغط المطوّل
    public float weight = 1f;   // العرض النسبي
    public int type = CHAR;
    public int code = 0;

    public Key() {}

    public static Key charKey(String label, String text, float w) {
        Key k = new Key();
        k.label = label;
        k.text = text;
        k.weight = w;
        k.type = CHAR;
        return k;
    }

    public static Key func(String label, int code, float w) {
        Key k = new Key();
        k.label = label;
        k.code = code;
        k.type = FUNC;
        k.weight = w;
        return k;
    }

    public Key shift(String l, String t) {
        this.shiftLabel = l;
        this.shiftText = t;
        return this;
    }

    public Key alt(String... a) {
        alts = new ArrayList<>();
        for (String s : a) alts.add(s);
        return this;
    }
}
