package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** إعدادات محفوظة دائمة — DRS Smart v2.0 */
public class Prefs {
    public int themePreset;         // فهرس ثيم ThemeSet.NAMES (0..11)
    public boolean sound = false;
    public boolean haptics = true;
    public boolean numRow = false;      // صف أرقام دائم فوق الحروف
    public boolean autoCap = true;      // أحرف كبيرة تلقائية
    public boolean doubleSpace = true;  // نقطة بضغطتين على المسافة
    public boolean suggest = true;      // شريط التنبؤ الذكي
    public boolean autoCorrect = true;  // تصحيح تلقائي عند الضغط على المسافة
    public int keyHeight = 1;           // 0 صغير، 1 متوسط، 2 كبير
    public int oneHanded = 0;           // 0 وسط، 1 يمين، 2 يسار
    public boolean voice = true;        // زر الإدخال الصوتي
    public boolean arabicDigits = false;// أرقام عربية-هندية في لوحة الأرقام
    public int accent = 0;              // لون التمييز: 0 افتراضي الثيم، 1..8
    public boolean nextWord = true;     // التنبؤ بالكلمة التالية
    public boolean glide = true;        // الكتابة بالسحب
    public boolean incognito = false;   // الوضع التخفي: بلا تعلم أو التقاط حافظة
    public int soundStyle = 0;          // 0 كلاسيكية، 1 رقمية، 2 ناعمة
    public int longPressIdx = 1;        // 0 سريع 250ms، 1 عادي 400ms، 2 بطيء 650ms

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE);
        reload();
    }

    public void reload() {
        // ترحيل من النسخ القديمة (-1 تلقائي/0 نهاري/1 ليلي/2 AMOLED)
        int legacy = sp.getInt("theme", Integer.MIN_VALUE);
        if (legacy != Integer.MIN_VALUE && !sp.contains("theme_preset")) {
            int mapped;
            switch (legacy) {
                case 0: mapped = 1; break;      // نهاري
                case 1: mapped = 2; break;      // ليلي
                case 2: mapped = 3; break;      // AMOLED
                default: mapped = 0; break;     // تلقائي
            }
            sp.edit().putInt("theme_preset", mapped).apply();
        }
        themePreset = sp.getInt("theme_preset", ThemeSet.DEFAULT_PRESET);
        sound = sp.getBoolean("sound", false);
        haptics = sp.getBoolean("haptics", true);
        numRow = sp.getBoolean("num_row", false);
        autoCap = sp.getBoolean("auto_cap", true);
        doubleSpace = sp.getBoolean("double_space", true);
        suggest = sp.getBoolean("suggest", true);
        autoCorrect = sp.getBoolean("auto_correct", true);
        keyHeight = sp.getInt("key_height", 1);
        oneHanded = sp.getInt("one_handed", 0);
        voice = sp.getBoolean("voice", true);
        arabicDigits = sp.getBoolean("arabic_digits", false);
        accent = sp.getInt("accent", 0);
        nextWord = sp.getBoolean("next_word", true);
        glide = sp.getBoolean("glide", true);
        incognito = sp.getBoolean("incognito", false);
        soundStyle = sp.getInt("sound_style", 0);
        longPressIdx = sp.getInt("long_press", 1);
    }

    public void setThemePreset(int t) {
        themePreset = t;
        sp.edit().putInt("theme_preset", t).apply();
    }

    public void setSound(boolean b) {
        sound = b;
        sp.edit().putBoolean("sound", b).apply();
    }

    public void setHaptics(boolean b) {
        haptics = b;
        sp.edit().putBoolean("haptics", b).apply();
    }

    public void setNumRow(boolean b) {
        numRow = b;
        sp.edit().putBoolean("num_row", b).apply();
    }

    public void setAutoCap(boolean b) {
        autoCap = b;
        sp.edit().putBoolean("auto_cap", b).apply();
    }

    public void setDoubleSpace(boolean b) {
        doubleSpace = b;
        sp.edit().putBoolean("double_space", b).apply();
    }

    public void setSuggest(boolean b) {
        suggest = b;
        sp.edit().putBoolean("suggest", b).apply();
    }

    public void setAutoCorrect(boolean b) {
        autoCorrect = b;
        sp.edit().putBoolean("auto_correct", b).apply();
    }

    public void setKeyHeight(int h) {
        keyHeight = h;
        sp.edit().putInt("key_height", h).apply();
    }

    public void setOneHanded(int m) {
        oneHanded = m;
        sp.edit().putInt("one_handed", m).apply();
    }

    public void setVoice(boolean b) {
        voice = b;
        sp.edit().putBoolean("voice", b).apply();
    }

    public void setArabicDigits(boolean b) {
        arabicDigits = b;
        sp.edit().putBoolean("arabic_digits", b).apply();
    }

    public void setAccent(int a) {
        accent = a;
        sp.edit().putInt("accent", a).apply();
    }

    public void setNextWord(boolean b) {
        nextWord = b;
        sp.edit().putBoolean("next_word", b).apply();
    }

    public void setGlide(boolean b) {
        glide = b;
        sp.edit().putBoolean("glide", b).apply();
    }

    public void setIncognito(boolean b) {
        incognito = b;
        sp.edit().putBoolean("incognito", b).apply();
    }

    public void setSoundStyle(int s) {
        soundStyle = s;
        sp.edit().putInt("sound_style", s).apply();
    }

    public void setLongPressIdx(int i) {
        longPressIdx = i;
        sp.edit().putInt("long_press", i).apply();
    }

    /** مدة الضغط المطوّل بالميلي ثانية حسب الإعداد */
    public int longPressMs() {
        if (longPressIdx == 0) return 250;
        if (longPressIdx == 2) return 650;
        return 400;
    }
}
