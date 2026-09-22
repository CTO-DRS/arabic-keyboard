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
}
