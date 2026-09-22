package com.zai.arabickeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** الشاشة الرئيسية: التفعيل، الثيمات، الإعدادات بالأقسام، التجربة — DRS Smart v2.0 */
public class MainActivity extends Activity {

    private Prefs prefs;
    private SuggestEngine engine;
    private TextView tvStatus;
    private TextView tvDictCount;
    private LinearLayout stResults;
    private InputMethodManager imm;
    private LinearLayout sections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashGuard.install(this); // نظام الحماية الذكي من الأعطال
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

        // ===== بطاقة الفحص الذكي المتكامل =====
        addCard(buildSelfTestCard());

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
        addSwitch(typingCard, R.string.sw_next_word, prefs.nextWord, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setNextWord(b); }
        });
        addSwitch(typingCard, R.string.sw_glide, prefs.glide, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setGlide(b); }
        });
        addSwitch(typingCard, R.string.sw_incognito, prefs.incognito, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setIncognito(b); }
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
        soundCard.addView(label(R.string.label_sound_style, 14, R.color.text_sub, false));
        soundCard.addView(radioRow(new String[]{
                getString(R.string.snd_classic), getString(R.string.snd_digital), getString(R.string.snd_soft)
        }, prefs.soundStyle, new IntListener() {
            @Override public void on(int i) { prefs.setSoundStyle(i); }
        }));
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
        layoutCard.addView(label(R.string.label_longpress, 14, R.color.text_sub, false));
        layoutCard.addView(radioRow(new String[]{
                getString(R.string.lp_fast), getString(R.string.lp_normal), getString(R.string.lp_slow)
        }, prefs.longPressIdx, new IntListener() {
            @Override public void on(int i) { prefs.setLongPressIdx(i); }
        }));
        addSwitch(layoutCard, R.string.sw_voice, prefs.voice, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setVoice(b); }
        });
        addSwitch(layoutCard, R.string.sw_arabic_digits, prefs.arabicDigits, new SwitchListener() {
            @Override public void on(boolean b) { prefs.setArabicDigits(b); }
        });

        // ===== منتقي لون التمييز =====
        layoutCard.addView(label(R.string.label_accent_color, 14, R.color.text_sub, false));
        LinearLayout accentRow = new LinearLayout(this);
        accentRow.setOrientation(LinearLayout.HORIZONTAL);
        accentRow.setGravity(Gravity.CENTER_VERTICAL);

        // خيار "افتراضي الثيم"
        TextView auto = new TextView(this);
        auto.setText(R.string.accent_auto);
        auto.setTextSize(12);
        auto.setPadding(dp(12), dp(7), dp(12), dp(7));
        auto.setClickable(true);
        GradientDrawable autoBg = new GradientDrawable();
        autoBg.setCornerRadius(dp(16));
        autoBg.setColor(prefs.accent == 0 ? 0xFF243055 : 0x00000000);
        autoBg.setStroke(dp(1), prefs.accent == 0 ? 0xFF7C4DFF : 0xFF3A4470);
        auto.setBackground(autoBg);
        auto.setTextColor(prefs.accent == 0 ? 0xFFB388FF : 0xFF9AA3D0);
        auto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.setAccent(0);
                buildSections();
            }
        });
        accentRow.addView(auto);

        // دوائر الألوان الثمانية
        for (int i = 1; i <= ThemeSet.ACCENT_COLORS.length; i++) {
            final int idx = i;
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(ThemeSet.ACCENT_COLORS[i - 1]);
            g.setStroke(prefs.accent == idx ? dp(2) : dp(1),
                    prefs.accent == idx ? 0xFFFFFFFF : 0x33000000);
            dot.setBackground(g);
            dot.setClickable(true);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.setAccent(idx);
                    buildSections();
                }
            });
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(26), dp(26));
            dlp.setMargins(dp(8), dp(8), dp(4), dp(4));
            accentRow.addView(dot, dlp);
        }
        layoutCard.addView(accentRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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

        // ===== الاختصارات النصية =====
        LinearLayout scCard = card();
        scCard.addView(label(R.string.section_shortcuts, 17, R.color.text_main, true));
        scCard.addView(label(R.string.shortcuts_info, 13, R.color.text_sub, false));
        buildShortcutsList(scCard);
        addCard(scCard);

        // ===== النسخ الاحتياطي والاستعادة =====
        LinearLayout bkCard = card();
        bkCard.addView(label(R.string.section_backup, 17, R.color.text_main, true));
        bkCard.addView(label(R.string.backup_info, 13, R.color.text_sub, false));
        bkCard.addView(secondaryButton(R.string.btn_backup_export, new View.OnClickListener() {
            @Override public void onClick(View v) { exportBackup(); }
        }));
        bkCard.addView(secondaryButton(R.string.btn_backup_import, new View.OnClickListener() {
            @Override public void onClick(View v) { importBackupDialog(); }
        }));
        addCard(bkCard);

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

    // ==================== الاختصارات النصية ====================

    private void buildShortcutsList(LinearLayout parent) {
        ArrayList<String[]> list = engine.getShortcuts();
        if (list.isEmpty()) {
            parent.addView(label(R.string.shortcuts_none, 13, R.color.text_sub, false));
        }
        for (final String[] s : list) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(9));
            bg.setColor(0xFF0E1428);
            bg.setStroke(dp(1), 0xFF2A3562);
            row.setBackground(bg);
            row.setPadding(dp(12), dp(9), dp(8), dp(9));

            TextView tv = new TextView(this);
            tv.setText(s[0] + "  ⤳  " + s[1]);
            tv.setTextSize(13);
            tv.setTextColor(0xFFEDF0FF);
            tv.setMaxLines(2);
            row.addView(tv, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(this);
            del.setText("✕");
            del.setTextSize(14);
            del.setTextColor(0xFFFF7B93);
            del.setPadding(dp(12), dp(4), dp(6), dp(4));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    engine.removeShortcut(s[0]);
                    Toast.makeText(MainActivity.this, R.string.shortcut_deleted, Toast.LENGTH_SHORT).show();
                    buildSections();
                }
            });
            row.addView(del, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, 0);
            parent.addView(row, lp);
        }

        // نموذج إضافة اختصار جديد
        final EditText abIn = new EditText(this);
        abIn.setHint(R.string.shortcut_abbr_hint);
        final EditText expIn = new EditText(this);
        expIn.setHint(R.string.shortcut_exp_hint);
        for (EditText et : new EditText[]{abIn, expIn}) {
            et.setTextColor(0xFFEDF0FF);
            et.setHintTextColor(0xFF6B7398);
            et.setTextSize(14);
            GradientDrawable etBg = new GradientDrawable();
            etBg.setCornerRadius(dp(10));
            etBg.setColor(0xFF0E1428);
            etBg.setStroke(dp(1), 0xFF2A3562);
            et.setBackground(etBg);
            et.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, 0);
            parent.addView(et, lp);
        }
        parent.addView(primaryButton(R.string.btn_shortcut_add, new View.OnClickListener() {
            @Override public void onClick(View v) {
                String ab = abIn.getText().toString().trim();
                String ex = expIn.getText().toString().trim();
                if (ab.isEmpty() || ex.isEmpty()) return;
                engine.addShortcut(ab, ex);
                Toast.makeText(MainActivity.this, R.string.shortcut_added, Toast.LENGTH_SHORT).show();
                buildSections();
            }
        }));
    }

    // ==================== الفحص الذكي المتكامل ====================

    /** بطاقة الفحص الذكي: ملخص سريع فوري + زر الفحص الشامل + صندوق النتائج */
    private LinearLayout buildSelfTestCard() {
        LinearLayout stCard = card();
        stCard.addView(label(R.string.selftest_title, 17, R.color.text_main, true));
        stCard.addView(label(R.string.selftest_info, 13, R.color.text_sub, false));

        // فحص سريع فوري عند فتح الشاشة
        TextView quick = new TextView(this);
        quick.setTextSize(13);
        quick.setLineSpacing(dp3(), 1f);
        boolean crash = CrashGuard.hasReport();
        if (crash) {
            String s = CrashGuard.lastReportSummary();
            quick.setText(getString(R.string.selftest_quick_crash)
                    + (s == null ? "" : "\n" + s));
            quick.setTextColor(0xFFFF7B6B);
        } else if (isEnabledBySystem() && isSelected()) {
            quick.setText(R.string.selftest_quick_ok);
            quick.setTextColor(0xFF66D99A);
        } else {
            quick.setText(R.string.selftest_quick_pending);
            quick.setTextColor(0xFFFFC93A);
        }
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qLp.setMargins(0, dp(10), 0, 0);
        stCard.addView(quick, qLp);

        stResults = new LinearLayout(this);
        stResults.setOrientation(LinearLayout.VERTICAL);
        stCard.addView(stResults, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        stCard.addView(primaryButton(R.string.selftest_run, new View.OnClickListener() {
            @Override public void onClick(View v) { runFullSelfTest(); }
        }));
        return stCard;
    }

    /** تشغيل الفحص الشامل في خيط خلفي ثم عرض النتائج */
    private void runFullSelfTest() {
        if (stResults == null) return;
        stResults.removeAllViews();
        TextView running = new TextView(this);
        running.setText(R.string.selftest_running);
        running.setTextSize(14);
        running.setTextColor(0xFFFFC93A);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, dp(12), 0, 0);
        stResults.addView(running, rLp);

        SelfTest.Report r0 = null;
        try {
            r0 = SelfTest.runAll(this);
        } catch (Throwable t) {
            CrashGuard.log(t);
        }
        final SelfTest.Report rep = r0;
        stResults.removeAllViews();
        if (rep == null) {
            TextView fail = new TextView(this);
            fail.setText(R.string.selftest_failed_run);
            fail.setTextSize(14);
            fail.setTextColor(0xFFFF7B6B);
            stResults.addView(fail, rLp);
            return;
        }
        showSelfTestResults(rep);
    }

    /** عرض نتائج الفحص: خلاصة ملونة + صف لكل فحص + سجل الأعطال + أزرار النسخ والمسح */
    private void showSelfTestResults(final SelfTest.Report rep) {
        // الخلاصة العامة
        TextView sum = new TextView(this);
        sum.setText(rep.summary);
        sum.setTextSize(15);
        sum.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sum.setTextColor(rep.failed == 0 ? 0xFF66D99A : (rep.score >= 60 ? 0xFFFFC93A : 0xFFFF7B6B));
        sum.setLineSpacing(dp3(), 1f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(12), 0, dp(4));
        stResults.addView(sum, sLp);

        // ملاحظة الإصلاح الذاتي إن حدث
        if (rep.healed) {
            TextView healed = new TextView(this);
            healed.setText(R.string.selftest_healed);
            healed.setTextSize(13);
            healed.setTextColor(0xFF66D99A);
            stResults.addView(healed, sLp);
        }

        // صفوف النتائج
        for (final SelfTest.Result x : rep.results) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);

            TextView mark = new TextView(this);
            switch (x.status) {
                case SelfTest.PASS: mark.setText("✔"); mark.setTextColor(0xFF66D99A); break;
                case SelfTest.WARN: mark.setText("⚠"); mark.setTextColor(0xFFFFC93A); break;
                case SelfTest.FAIL: mark.setText("✖"); mark.setTextColor(0xFFFF7B6B); break;
                default: mark.setText("ℹ"); mark.setTextColor(0xFF8A93C4); break;
            }
            mark.setTextSize(15);
            mark.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            mark.setPadding(0, dp(2), dp(10), 0);
            row.addView(mark, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(this);
            name.setText(x.name);
            name.setTextSize(14);
            name.setTextColor(0xFFEDF0FF);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            TextView det = new TextView(this);
            det.setText(x.detail);
            det.setTextSize(12);
            det.setTextColor(0xFF9AA3D0);
            det.setLineSpacing(dp3(), 1f);
            col.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            col.addView(det, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, dp(10), 0, 0);
            stResults.addView(row, rowLp);
        }

        // صندوق آخر عطل مسجل + زر المسح
        if (CrashGuard.hasReport()) {
            TextView cr = new TextView(this);
            String s = CrashGuard.lastReportSummary();
            cr.setText(getString(R.string.selftest_last_crash) + "\n" + (s == null ? "—" : s));
            cr.setTextSize(12);
            cr.setTextColor(0xFFFF7B6B);
            GradientDrawable crBg = new GradientDrawable();
            crBg.setCornerRadius(dp(9));
            crBg.setColor(0xFF1A0E12);
            crBg.setStroke(dp(1), 0xFF5A2430);
            cr.setBackground(crBg);
            cr.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0, dp(14), 0, 0);
            stResults.addView(cr, cLp);

            stResults.addView(secondaryButton(R.string.selftest_clear, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    CrashGuard.clearReport();
                    Toast.makeText(MainActivity.this, R.string.selftest_cleared, Toast.LENGTH_SHORT).show();
                    buildSections();
                }
            }));
        }

        // زر نسخ التقرير
        stResults.addView(secondaryButton(R.string.selftest_copy, new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    ClipboardManager cm = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("DRS-SelfTest",
                                SelfTest.buildReport(rep)));
                    }
                    Toast.makeText(MainActivity.this, R.string.selftest_copied, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            }
        }));
    }

    // ==================== النسخ الاحتياطي ====================

    /** تصدير كل البيانات (إعدادات + قاموس + اختصارات + حافظة) كنص JSON */
    private void exportBackup() {
        try {
            android.content.SharedPreferences panelSp =
                    getSharedPreferences("kb_panel", Context.MODE_PRIVATE);
            JSONObject o = new JSONObject();
            o.put("app", "DRS-Smart-Keyboard");
            o.put("v", 22);
            o.put("theme", prefs.themePreset);
            o.put("sound", prefs.sound);
            o.put("haptics", prefs.haptics);
            o.put("numRow", prefs.numRow);
            o.put("autoCap", prefs.autoCap);
            o.put("doubleSpace", prefs.doubleSpace);
            o.put("suggest", prefs.suggest);
            o.put("autoCorrect", prefs.autoCorrect);
            o.put("keyHeight", prefs.keyHeight);
            o.put("oneHanded", prefs.oneHanded);
            o.put("voice", prefs.voice);
            o.put("arabicDigits", prefs.arabicDigits);
            o.put("accent", prefs.accent);
            o.put("nextWord", prefs.nextWord);
            o.put("glide", prefs.glide);
            o.put("incognito", prefs.incognito);
            o.put("soundStyle", prefs.soundStyle);
            o.put("longPress", prefs.longPressIdx);
            o.put("learned", engine.exportLearned());
            o.put("bigrams", engine.exportBigrams());
            o.put("shortcuts", engine.exportShortcuts());
            o.put("clips", panelSp.getString("clips", ""));
            o.put("pins", panelSp.getString("pins", ""));
            o.put("emoji", panelSp.getString("emoji_recents", ""));
            String json = o.toString();
            // نسخ إلى الحافظة
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("DRS-Backup", json));
            }
            // مشاركة مباشرة أيضاً
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, json);
            try {
                startActivity(Intent.createChooser(send, getString(R.string.btn_backup_export)));
            } catch (Exception ignored) {}
            Toast.makeText(this, R.string.backup_exported, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.backup_import_bad, Toast.LENGTH_SHORT).show();
        }
    }

    /** نافذة استيراد: لصق النص JSON واستعادته */
    private void importBackupDialog() {
        final EditText in = new EditText(this);
        in.setHint(R.string.backup_import_hint);
        in.setTextColor(0xFFEDF0FF);
        in.setHintTextColor(0xFF6B7398);
        in.setTextSize(12);
        in.setMinLines(4);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0xFF0E1428);
        bg.setStroke(dp(1), 0xFF2A3562);
        in.setBackground(bg);
        in.setPadding(dp(12), dp(10), dp(12), dp(10));
        // لصق تلقائي من الحافظة إن كانت تبدأ كنسخة احتياطية
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (cs != null && cs.toString().contains("\"DRS-Smart-Keyboard\"")) {
                    in.setText(cs.toString());
                }
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_import_title)
                .setView(in)
                .setPositiveButton(R.string.btn_backup_import, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        applyBackup(in.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyBackup(String text) {
        try {
            JSONObject o = new JSONObject(text);
            if (!"DRS-Smart-Keyboard".equals(o.optString("app"))) throw new Exception("bad");
            if (o.has("theme")) prefs.setThemePreset(o.getInt("theme"));
            if (o.has("sound")) prefs.setSound(o.getBoolean("sound"));
            if (o.has("haptics")) prefs.setHaptics(o.getBoolean("haptics"));
            if (o.has("numRow")) prefs.setNumRow(o.getBoolean("numRow"));
            if (o.has("autoCap")) prefs.setAutoCap(o.getBoolean("autoCap"));
            if (o.has("doubleSpace")) prefs.setDoubleSpace(o.getBoolean("doubleSpace"));
            if (o.has("suggest")) prefs.setSuggest(o.getBoolean("suggest"));
            if (o.has("autoCorrect")) prefs.setAutoCorrect(o.getBoolean("autoCorrect"));
            if (o.has("keyHeight")) prefs.setKeyHeight(o.getInt("keyHeight"));
            if (o.has("oneHanded")) prefs.setOneHanded(o.getInt("oneHanded"));
            if (o.has("voice")) prefs.setVoice(o.getBoolean("voice"));
            if (o.has("arabicDigits")) prefs.setArabicDigits(o.getBoolean("arabicDigits"));
            if (o.has("accent")) prefs.setAccent(o.getInt("accent"));
            if (o.has("nextWord")) prefs.setNextWord(o.getBoolean("nextWord"));
            if (o.has("glide")) prefs.setGlide(o.getBoolean("glide"));
            if (o.has("incognito")) prefs.setIncognito(o.getBoolean("incognito"));
            if (o.has("soundStyle")) prefs.setSoundStyle(o.getInt("soundStyle"));
            if (o.has("longPress")) prefs.setLongPressIdx(o.getInt("longPress"));
            engine.importLearned(o.optString("learned", ""));
            engine.importBigrams(o.optString("bigrams", ""));
            engine.importShortcuts(o.optString("shortcuts", ""));
            android.content.SharedPreferences.Editor pe =
                    getSharedPreferences("kb_panel", Context.MODE_PRIVATE).edit();
            pe.putString("clips", o.optString("clips", ""));
            pe.putString("pins", o.optString("pins", ""));
            pe.putString("emoji_recents", o.optString("emoji", ""));
            pe.apply();
            Toast.makeText(this, R.string.backup_import_ok, Toast.LENGTH_LONG).show();
            buildSections();
        } catch (Exception e) {
            Toast.makeText(this, R.string.backup_import_bad, Toast.LENGTH_SHORT).show();
        }
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
