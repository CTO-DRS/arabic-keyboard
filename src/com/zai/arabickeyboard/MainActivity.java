package com.zai.arabickeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** الشاشة الرئيسية: التفعيل، الثيمات، الإعدادات بالأقسام، التجربة — DRS Smart v2.0 */
public class MainActivity extends Activity {

    private Prefs prefs;
    private SuggestEngine engine;
    private TextView tvStatus;
    private TextView tvDictCount;
    private InputMethodManager imm;
    private LinearLayout sections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);
        engine = new SuggestEngine(this);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        sections = (LinearLayout) findViewById(R.id.sections);

        tvStatus = new TextView(this);
        buildSections();
    }

    private void buildSections() {
        sections.removeAllViews();

        // ===== بطاقة الحالة =====
        LinearLayout statusCard = card();
        statusCard.addView(label(R.string.card_status_title, 17, R.color.text_main, true));
        tvStatus.setTextSize(14);
        tvStatus.setLineSpacing(dp3(), 1f);
        tvStatus.setTextColor(0xFF9AA3D0);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stLp.setMargins(0, dp(10), 0, 0);
        statusCard.addView(tvStatus, stLp);

        statusCard.addView(primaryButton(R.string.btn_enable, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        }));
        statusCard.addView(secondaryButton(R.string.btn_choose, new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (imm != null) imm.showInputMethodPicker();
            }
        }));
        addCard(statusCard);

        // ===== بطاقة الثيمات =====
        LinearLayout themeCard = card();
        themeCard.addView(label(R.string.label_theme, 17, R.color.text_main, true));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        for (int i = 0; i < ThemeSet.NAMES.length; i++) {
            final int idx = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setClickable(true);
            cell.setPadding(dp(6), dp(8), dp(6), dp(8));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.setThemePreset(idx);
                    buildSections();
                }
            });

            FrameLayout previewWrap = new FrameLayout(this);
            int[] pv = ThemeSet.PREVIEW[idx];
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(pv[0]);
            bg.setStroke(idx == prefs.themePreset ? dp(2) : dp(1),
                    idx == prefs.themePreset ? pv[2] : 0xFF2A3562);
            View pv1 = new View(this);
            pv1.setBackground(bg);
            previewWrap.addView(pv1, new FrameLayout.LayoutParams(dp(56), dp(40)));

            // شريط لون التمييز أسفل المعاينة
            GradientDrawable bar = new GradientDrawable();
            bar.setCornerRadius(dp(2));
            bar.setColor(pv[2]);
            View barV = new View(this);
            barV.setBackground(bar);
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    dp(38), dp(4), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barLp.bottomMargin = dp(4);
            previewWrap.addView(barV, barLp);

            // مفاتيح وهمية داخل المعاينة
            LinearLayout keysRow = new LinearLayout(this);
            keysRow.setGravity(Gravity.CENTER);
            for (int k = 0; k < 4; k++) {
                GradientDrawable kb = new GradientDrawable();
                kb.setCornerRadius(dp(2.5f));
                kb.setColor(pv[1]);
                View kv1 = new View(this);
                kv1.setBackground(kb);
                LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(dp(8), dp(6));
                klp.setMargins(dp(1), 0, dp(1), 0);
                keysRow.addView(kv1, klp);
            }
            FrameLayout.LayoutParams keysLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            keysLp.bottomMargin = dp(8);
            previewWrap.addView(keysRow, keysLp);

            TextView name = new TextView(this);
            name.setText(ThemeSet.NAMES[idx]);
            name.setTextSize(11);
            name.setTextColor(idx == prefs.themePreset ? pv[2] : 0xFF9AA3D0);
            name.setPadding(0, dp(4), 0, 0);

            cell.addView(previewWrap, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            grid.addView(cell, glp);
        }
        themeCard.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        themeCard.addView(label(R.string.label_accent, 12, R.color.text_sub, false));
        addCard(themeCard);

        // ===== الكتابة الذكية =====
        LinearLayout typingCard = card();
        typingCard.addView(label(R.string.section_typing, 17, R.color.text_main, true));
        addSwitch(typingCard, R.string.sw_suggest, prefs.suggest, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setSuggest(b); }
        });
        addSwitch(typingCard, R.string.sw_autocorrect, prefs.autoCorrect, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setAutoCorrect(b); }
        });
        addSwitch(typingCard, R.string.sw_numrow, prefs.numRow, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setNumRow(b); }
        });
        addSwitch(typingCard, R.string.sw_autocap, prefs.autoCap, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setAutoCap(b); }
        });
        addSwitch(typingCard, R.string.sw_double_space, prefs.doubleSpace, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setDoubleSpace(b); }
        });
        addCard(typingCard);

        // ===== الصوت والاهتزاز =====
        LinearLayout soundCard = card();
        soundCard.addView(label(R.string.section_sound, 17, R.color.text_main, true));
        addSwitch(soundCard, R.string.sw_sound, prefs.sound, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setSound(b); }
        });
        addSwitch(soundCard, R.string.sw_haptic, prefs.haptics, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setHaptics(b); }
        });
        addCard(soundCard);

        // ===== التنسيق =====
        LinearLayout layoutCard = card();
        layoutCard.addView(label(R.string.section_layout, 17, R.color.text_main, true));
        layoutCard.addView(label(R.string.label_height, 14, R.color.text_sub, false));
        layoutCard.addView(radioRow(new String[]{
                getString(R.string.h_small), getString(R.string.h_medium), getString(R.string.h_large)
        }, prefs.keyHeight, new IntListener() {
            @Override public void on(int i) { prefs.setKeyHeight(i); }
        }));
        layoutCard.addView(label(R.string.label_onehand, 14, R.color.text_sub, false));
        layoutCard.addView(radioRow(new String[]{
                getString(R.string.oh_center), getString(R.string.oh_right), getString(R.string.oh_left)
        }, prefs.oneHanded, new IntListener() {
            @Override public void on(int i) { prefs.setOneHanded(i); }
        }));
        addCard(layoutCard);

        // ===== القاموس الذكي =====
        LinearLayout dictCard = card();
        dictCard.addView(label(R.string.section_dict, 17, R.color.text_main, true));
        dictCard.addView(label(R.string.dict_info, 13, R.color.text_sub, false));
        tvDictCount = new TextView(this);
        tvDictCount.setTextSize(14);
        tvDictCount.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvDictCount.setTextColor(0xFFB388FF);
        tvDictCount.setText(getString(R.string.dict_count, engine.learnedCount()));
        LinearLayout.LayoutParams dcLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dcLp.setMargins(0, dp(8), 0, 0);
        dictCard.addView(tvDictCount, dcLp);
        dictCard.addView(secondaryButton(R.string.btn_dict_reset, new View.OnClickListener() {
            @Override public void onClick(View v) {
                engine.resetLearned();
                tvDictCount.setText(getString(R.string.dict_count, 0));
                Toast.makeText(MainActivity.this, R.string.dict_reset_done, Toast.LENGTH_SHORT).show();
            }
        }));
        addCard(dictCard);

        // ===== التجربة =====
        LinearLayout tryCard = card();
        tryCard.addView(label(R.string.card_try_title, 17, R.color.text_main, true));
        EditText et = new EditText(this);
        et.setHint(R.string.try_hint);
        et.setTextColor(0xFFEDF0FF);
        et.setHintTextColor(0xFF6B7398);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setCornerRadius(dp(10));
        etBg.setColor(0xFF0E1428);
        etBg.setStroke(dp(1), 0xFF2A3562);
        et.setBackground(etBg);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setMinLines(2);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, dp(10), 0, 0);
        tryCard.addView(et, etLp);
        addCard(tryCard);

        // ===== حول =====
        LinearLayout aboutCard = card();
        aboutCard.addView(label(R.string.about_title, 17, R.color.text_main, true));
        aboutCard.addView(label(R.string.about_version, 14, 0xFFB388FF, true));
        aboutCard.addView(label(R.string.about_body, 13, R.color.text_sub, false));
        addCard(aboutCard);

        // ===== نصائح =====
        LinearLayout tips = new LinearLayout(this);
        tips.setOrientation(LinearLayout.VERTICAL);
        tips.setPadding(dp(22), dp(14), dp(22), dp(10));
        tips.addView(label(R.string.footer_tips, 12, 0xFF6B7398, false));
        addCard(tips);
    }

    // ==================== أدوات بناء الواجهة ====================

    private interface SwitchListener { void on(boolean b); }
    private interface IntListener { void on(int i); }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackgroundResource(R.drawable.bg_card);
        return c;
    }

    private void addCard(LinearLayout card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(8), dp(16), dp(8));
        sections.addView(card, lp);
    }

    private TextView label(int textRes, int sp, int colorRes, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(textRes);
        tv.setTextSize(sp);
        int color = colorRes;
        try {
            color = getResources().getColor(colorRes);
        } catch (Exception ignored) {
            // لون صريح ممرر مباشرة (0xFF...)
        }
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setLineSpacing(dp3(), 1f);
        return tv;
    }

    private void addSwitch(LinearLayout parent, int labelRes, boolean checked,
                           final SwitchListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = label(labelRes, 14, R.color.text_main, false);
        row.addView(tv, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((b, isChecked) -> listener.on(isChecked));
        row.addView(sw, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        parent.addView(row, lp);
    }

    private View radioRow(String[] labels, int checkedIdx, final IntListener listener) {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(labels.length == 3 ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]);
            rb.setTextSize(14);
            rb.setTextColor(0xFFEDF0FF);
            rb.setId(i + 1);
            rg.addView(rb);
            if (i == checkedIdx) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener((group, checkedId) -> listener.on(checkedId - 1));
        return rg;
    }

    private Button primaryButton(int textRes, View.OnClickListener listener) {
        Button b = baseButton(textRes, listener);
        b.setBackgroundResource(R.drawable.btn_primary);
        b.setTextColor(0xFFFFFFFF);
        return b;
    }

    private Button secondaryButton(int textRes, View.OnClickListener listener) {
        Button b = baseButton(textRes, listener);
        b.setBackgroundResource(R.drawable.btn_secondary);
        b.setTextColor(0xFFB388FF);
        return b;
    }

    private Button baseButton(int textRes, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(textRes);
        b.setTextSize(15);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        lp.setMargins(0, dp(14), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float dp3() { return dp(3); }

    // ==================== الحالة ====================

    private boolean isEnabledBySystem() {
        if (imm == null) return false;
        List<InputMethodInfo> list = imm.getEnabledInputMethodList();
        for (InputMethodInfo i : list)
            if (i.getPackageName().equals(getPackageName())) return true;
        return false;
    }

    private boolean isSelected() {
        String def = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        return def != null && def.contains(getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) prefs.reload();
        boolean enabled = isEnabledBySystem();
        boolean selected = isSelected();
        if (selected) {
            tvStatus.setText(R.string.status_ready);
            tvStatus.setTextColor(0xFF66D99A);
        } else if (enabled) {
            tvStatus.setText(R.string.status_enabled_not_selected);
            tvStatus.setTextColor(0xFFFFC93A);
        } else {
            tvStatus.setText(R.string.status_disabled);
            tvStatus.setTextColor(0xFF9AA3D0);
        }
    }
}
