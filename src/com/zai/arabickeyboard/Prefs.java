package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** إعدادات محفوظة دائمة */
public class Prefs {
    public int theme = -1;          // -1 تلقائي، 0 نهاري، 1 ليلي، 2 AMOLED
    public int accent = 0;          // فهرس لون التمييز
    public boolean sound = false;
    public boolean haptics = true;
    public boolean numRow = false;      // صف أرقام دائم فوق الحروف
    public boolean autoCap = true;      // أحرف كبيرة تلقائية (إنجليزي)
    public boolean doubleSpace = true;  // نقطة بضغطتين على المسافة
    public int keyHeight = 1;           // 0 صغير، 1 متوسط، 2 كبير

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE);
        reload();
    }

    public void reload() {
        theme = sp.getInt("theme", -1);
        accent = sp.getInt("accent", 0);
        sound = sp.getBoolean("sound", false);
        haptics = sp.getBoolean("haptics", true);
        numRow = sp.getBoolean("num_row", false);
        autoCap = sp.getBoolean("auto_cap", true);
        doubleSpace = sp.getBoolean("double_space", true);
        keyHeight = sp.getInt("key_height", 1);
    }

    public void setTheme(int t) {
        theme = t;
        sp.edit().putInt("theme", t).apply();
    }

    public void setAccent(int a) {
        accent = a;
        sp.edit().putInt("accent", a).apply();
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

    public void setKeyHeight(int h) {
        keyHeight = h;
        sp.edit().putInt("key_height", h).apply();
    }
}
