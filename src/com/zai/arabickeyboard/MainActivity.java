package com.zai.arabickeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * التطبيق الرئيسي — DRS Smart Keyboard v2.5
 * تطبيق متعدد الشاشات بتصميم عصري موحد وخمسة أقسام:
 *   الرئيسية (حالة + اختصارات سريعة + تجربة) — الثيمات — الإعدادات — الفحص الذكي — حول
 * تنقّل سفلي بأيقونات مرسومة، انتقالات ناعمة، وهوية بصرية متكاملة.
 */
public class MainActivity extends Activity {

    private static final int TAB_HOME = 0;
    private static final int TAB_THEMES = 1;
    private static final int TAB_SETTINGS = 2;
    private static final int TAB_DIAG = 3;
    private static final int TAB_ABOUT = 4;
    private static final int TAB_COUNT = 5;
    private static final String[] TAB_LABELS = {
            "الرئيسية", "الثيمات", "الإعدادات", "الفحص الذكي", "حول"
    };

    private Prefs prefs;
    private SuggestEngine engine;
    private InputMethodManager imm;

    private FrameLayout content;
    private final ScrollView[] screens = new ScrollView[TAB_COUNT];
    private final LinearLayout[] navItems = new LinearLayout[TAB_COUNT];
    private int currentTab = TAB_HOME;
    private final boolean[] stale = new boolean[TAB_COUNT];

    // مراجع التحديث الحي
    private LinearLayout statusBox;        // بطاقة الحالة في الرئيسية
    private TextView dictCountTv;          // عداد القاموس في الإعدادات
    private LinearLayout shortcutsBox;     // حاوية الاختصارات في الإعدادات
    private ScoreRing scoreRing;           // حلقة النتيجة
    private LinearLayout diagResults;      // نتائج الفحص
    private boolean diagEverRun = false;
    private SelfTest.Report lastReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashGuard.install(this); // نظام الحماية الذكي من الأعطال
        prefs = new Prefs(this);
        engine = new SuggestEngine(this);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        buildRoot();
    }

    // ==================== الهيكل العام ====================

    private void buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiKit.PAGE_BG);

        // ===== الشريط العلوي =====
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackground(UiKit.rounded(0x00000000, 0, this));
        header.setPadding(dp(20), dp(18), dp(20), dp(14));

        View logo = new View(this);
        GradientDrawable lg = UiKit.accentGradient(this, 12);
        logo.setBackground(lg);
        header.addView(logo, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView hTitle = new TextView(this);
        hTitle.setText("DRS Smart Keyboard");
        hTitle.setTextSize(18);
        hTitle.setTypeface(UiKit.bold());
        hTitle.setTextColor(UiKit.TEXT_MAIN);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htLp.setMargins(dp(12), 0, dp(8), 0);
        header.addView(hTitle, htLp);

        TextView ver = UiKit.chip(this, "v" + BuildInfo.VERSION_NAME,
                0x227C5CFF, 0x557C5CFF, UiKit.ACCENT_SOFT);
        header.addView(ver);

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(dp(8), dp(4), dp(8), 0);
        root.addView(header, hLp);

        // ===== محتوى الشاشات =====
        content = new FrameLayout(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(content, cLp);

        // ===== شريط التنقل السفلي =====
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackground(UiKit.outlined(UiKit.NAV_BG, 0, UiKit.CARD_STROKE, 1, this));
        nav.setPadding(dp(6), dp(8), dp(6), dp(10));
        for (int i = 0; i < TAB_COUNT; i++) {
            navItems[i] = navItem(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nav.addView(navItems[i], lp);
        }
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(nav, nLp);

        setContentView(root);
        for (int i = 0; i < TAB_COUNT; i++) {
            screens[i] = buildScreen(i);
            content.addView(screens[i], new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        highlightNav(TAB_HOME);
    }

    /** عنصر واحد في شريط التنقل: أيقونة مرسومة + عنوان */
    private LinearLayout navItem(final int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setBackground(UiKit.rounded(0x00000000, 14, this));
        item.setPadding(0, dp(5), 0, dp(5));

        TabIcon icon = new TabIcon(this, index);
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView label = new TextView(this);
        label.setText(TAB_LABELS[index]);
        label.setTextSize(9.5f);
        label.setTypeface(UiKit.medium());
        label.setTextColor(UiKit.TEXT_FAINT);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lLp.setMargins(0, dp(3), 0, 0);
        item.addView(label, lLp);
        item.setTag(label);

        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(index); }
        });
        return item;
    }

    private void highlightNav(int active) {
        for (int i = 0; i < TAB_COUNT; i++) {
            LinearLayout item = navItems[i];
            boolean on = i == active;
            item.setBackground(on ? UiKit.rounded(0x1F7C5CFF, 14, this)
                                  : UiKit.rounded(0x00000000, 14, this));
            TextView label = (TextView) item.getTag();
            label.setTextColor(on ? UiKit.ACCENT_SOFT : UiKit.TEXT_FAINT);
            ((TabIcon) item.getChildAt(0)).setActive(on);
        }
    }

    /** تبديل الشاشة مع انتقال ناعم وإعادة بناء إن لزم */
    private void showTab(int index) {
        if (index == currentTab && screens[index].getVisibility() == View.VISIBLE) return;
        currentTab = index;
        highlightNav(index);
        if (stale[index]) { rebuildScreen(index); stale[index] = false; }
        for (int i = 0; i < TAB_COUNT; i++) {
            screens[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
        Animation fade = new AlphaAnimation(0.55f, 1f);
        fade.setDuration(180);
        screens[index].startAnimation(fade);
        if (index == TAB_DIAG && !diagEverRun) runDiag();
    }

    private void rebuildScreen(int index) {
        content.removeView(screens[index]);
        screens[index] = buildScreen(index);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(dp(8), dp(4), dp(8), dp(8));
        content.addView(screens[index], lp);
        screens[index].setVisibility(index == currentTab ? View.VISIBLE : View.GONE);
        stale[index] = false;
    }

    /** بناء شاشة رقم i */
    private ScrollView buildScreen(int index) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(UiKit.PAGE_BG);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout page = UiKit.vstack(this);
        int padTop = index == TAB_HOME ? dp(2) : dp(6);
        page.setPadding(dp(6), padTop, dp(6), dp(10));
        sv.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        switch (index) {
            case TAB_HOME:     buildHome(page); break;
            case TAB_THEMES:   buildThemes(page); break;
            case TAB_SETTINGS: buildSettings(page); break;
            case TAB_DIAG:     buildDiag(page); break;
            default:           buildAbout(page); break;
        }
        return sv;
    }

    private void markStale(int index) { stale[index] = true; }

        // ==================== الشاشة ١: الرئيسية ====================

    private void buildHome(LinearLayout page) {
        // ===== بطاقة الترحيب المتدرجة =====
        LinearLayout hero = UiKit.vstack(this);
        hero.setBackground(UiKit.heroGradient(this));
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));

        TextView heroTitle = UiKit.text(this, "لوحة المفاتيح العربية الذكية", 21, 0xFFFFFFFF, true);
        hero.addView(heroTitle);
        hero.addView(UiKit.text(this,
                "ست لغات · تنبؤ وتصحيح · كتابة بالسحب · ١٦ ثيماً · فحص ذكي متكامل",
                12.5f, 0xFFC6CDF2, false));

        LinearLayout heroChips = UiKit.hstack(this);
        heroChips.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hcLp0 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hcLp0.setMargins(0, dp(12), dp(6), 0);
        heroChips.addView(UiKit.chip(this, "✓ مفتوح المصدر", 0x22FFFFFF, 0x44FFFFFF, 0xFFE8ECFF), hcLp0);
        LinearLayout.LayoutParams hcLp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hcLp1.setMargins(0, dp(12), dp(6), 0);
        heroChips.addView(UiKit.chip(this, "بدون إعلانات", 0x22FFFFFF, 0x44FFFFFF, 0xFFE8ECFF), hcLp1);
        LinearLayout.LayoutParams hcLp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hcLp2.setMargins(0, dp(12), 0, 0);
        heroChips.addView(UiKit.chip(this, "خصوصية كاملة", 0x22FFFFFF, 0x44FFFFFF, 0xFFE8ECFF), hcLp2);
        hero.addView(heroChips);
        addCard(page, hero, 14);

        // ===== بطاقة الحالة =====
        statusBox = UiKit.vstack(this);
        addCard(page, wrapCard(statusBox), 8);
        fillStatusBox();

        // ===== أرقام سريعة =====
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statTile(arabicNum(engine.learnedCount()), "كلمة تعلّمتها"));
        stats.addView(statTile(arabicNum(engine.getShortcuts().size()), "اختصار نصي"));
        stats.addView(statTile(arabicNum(countEmojis()), "إيموجي جاهز"));
        addCard(page, stats, 8);

        // ===== أزرار وصول سريع =====
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout t1 = UiKit.tile(this, "🎨", "الثيمات", "١٦ ثيماً و٨ ألوان", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_THEMES); }
        });
        LinearLayout t2 = UiKit.tile(this, "🛡", "الفحص الذكي", "١٤ فحصاً شاملاً", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_DIAG); }
        });
        LinearLayout t3 = UiKit.tile(this, "⚙", "الإعدادات", "تحكم كامل", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_SETTINGS); }
        });
        LinearLayout t4 = UiKit.tile(this, "ℹ", "حول التطبيق", "دليل الاستخدام", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_ABOUT); }
        });
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(96), 1f);
        half.setMargins(dp(4), dp(4), dp(4), dp(4));
        row1.addView(t1, half);
        row1.addView(t2, half);
        row2.addView(t3, half);
        row2.addView(t4, half);
        tiles.addView(row1);
        tiles.addView(row2);
        addCard(page, tiles, 8);

        // ===== بطاقة التجربة =====
        LinearLayout tryCard = UiKit.card(this);
        tryCard.addView(UiKit.title(this, "جرّب اللوحة الآن"));
        tryCard.addView(UiKit.body(this, "اكتب هنا بالعربية أو الإنجليزية لتجربة التنبؤ والتصحيح والسحب مباشرة."));
        EditText et = UiKit.field(this, "اكتب هنا بالعربية أو الإنجليزية...", 2);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, dp(10), 0, 0);
        tryCard.addView(et, etLp);
        addCard(page, tryCard, 8);
    }

    /** تعبئة بطاقة الحالة (تُستدعى عند البناء وعند كل استئناف) */
    private void fillStatusBox() {
        if (statusBox == null) return;
        statusBox.removeAllViews();

        boolean enabled = isEnabledBySystem();
        boolean selected = isSelected();
        int color = selected ? UiKit.GREEN : (enabled ? UiKit.AMBER : UiKit.TEXT_SUB);
        String title = selected ? "اللوحة جاهزة للكتابة"
                : (enabled ? "مفعّلة — بقي خطوة واحدة" : "اللوحة غير مفعّلة بعد");
        String desc = selected
                ? "يمكنك استخدامها في أي تطبيق الآن — جرّبها في صندوق التجربة بالأسفل."
                : (enabled ? "اخترها كلوحة إدخال حالية من الزر الثاني بالأسفل."
                : "فعّل اللوحة من إعدادات النظام بخطوة واحدة ثم اخترها كلوحة حالية.");

        LinearLayout head = UiKit.hstack(this);
        View dot = new View(this);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(color);
        dot.setBackground(dg);
        head.addView(dot, new LinearLayout.LayoutParams(dp(12), dp(12)));
        TextView st = UiKit.text(this, title, 16, color, true);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stLp.setMargins(dp(10), 0, 0, 0);
        head.addView(st, stLp);
        statusBox.addView(head);

        statusBox.addView(UiKit.text(this, desc, 13, UiKit.TEXT_SUB, false));

        if (!selected) {
            if (!enabled) {
                statusBox.addView(UiKit.primaryButton(this, "① تفعيل اللوحة في إعدادات النظام",
                        new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                try {
                                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "افتح: الإعدادات ← اللغة والإدخال", Toast.LENGTH_LONG).show();
                                }
                            }
                        }));
            }
            statusBox.addView(UiKit.secondaryButton(this, enabled
                            ? "② اختيارها كلوحة الإدخال الحالية" : "② اختيارها كلوحة الإدخال",
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            if (imm != null) imm.showInputMethodPicker();
                        }
                    }));
        } else {
            statusBox.addView(UiKit.secondaryButton(this, "تغيير لوحة الإدخال الحالية",
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            if (imm != null) imm.showInputMethodPicker();
                        }
                    }));
        }
    }

    private LinearLayout statTile(String value, String label) {
        LinearLayout t = UiKit.vstack(this);
        t.setGravity(Gravity.CENTER);
        t.setBackground(UiKit.outlined(UiKit.TILE_BG, 16, UiKit.CARD_STROKE, 1, this));
        t.setPadding(dp(6), dp(14), dp(6), dp(14));
        TextView v = UiKit.text(this, value, 20, UiKit.ACCENT_SOFT, true);
        v.setGravity(Gravity.CENTER);
        t.addView(v);
        TextView l = UiKit.text(this, label, 11, UiKit.TEXT_SUB, false);
        l.setGravity(Gravity.CENTER);
        t.addView(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(5), 0, dp(5), 0);
        t.setLayoutParams(lp);
        return t;
    }

    // ==================== الشاشة ٤: الفحص الذكي ====================

    private void buildDiag(LinearLayout page) {
        LinearLayout ringCard = UiKit.card(this);
        ringCard.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreRing = new ScoreRing(this);
        if (lastReport != null) scoreRing.setScore(lastReport.score);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(150), dp(150));
        ringLp.gravity = Gravity.CENTER_HORIZONTAL;
        ringLp.setMargins(0, dp(6), 0, dp(6));
        ringCard.addView(scoreRing, ringLp);

        TextView ringSub = UiKit.text(this,
                "الفحص الذكي المتكامل — ١٤ فحصاً تغطي النظام والتخطيطات والقواميس والثيمات والموارد",
                12.5f, UiKit.TEXT_SUB, false);
        ringSub.setGravity(Gravity.CENTER);
        ringCard.addView(ringSub);

        ringCard.addView(UiKit.primaryButton(this, "تشغيل الفحص الشامل الآن", new View.OnClickListener() {
            @Override public void onClick(View v) { runDiag(); }
        }));
        addCard(page, ringCard, 12);

        diagResults = UiKit.vstack(this);
        addCard(page, wrapCard(diagResults), 8);
        if (lastReport != null) renderDiagResults();
        else diagResults.addView(UiKit.body(this,
                "اضغط «تشغيل الفحص الشامل» لعرض تقرير مفصل لكل مكوّنات اللوحة، مع إصلاح ذاتي للإعدادات التالفة."));
    }

    /** تشغيل الفحص الشامل ثم عرض النتائج */
    private void runDiag() {
        if (diagResults == null) { diagEverRun = false; return; }
        diagEverRun = true;
        diagResults.removeAllViews();
        TextView running = UiKit.text(this, "⏳ جارٍ فحص كل المكوّنات...", 14, UiKit.AMBER, true);
        diagResults.addView(running);

        SelfTest.Report rep = null;
        try {
            rep = SelfTest.runAll(this);
        } catch (Throwable t) {
            CrashGuard.log(t);
        }
        lastReport = rep;
        diagResults.removeAllViews();
        if (rep == null) {
            diagResults.addView(UiKit.text(this, "تعذّر إتمام الفحص — جرّب مرة أخرى.", 14, UiKit.RED, true));
            return;
        }
        if (scoreRing != null) scoreRing.setScore(rep.score);
        renderDiagResults();
    }

    /** عرض نتائج الفحص داخل بطاقة النتائج */
    private void renderDiagResults() {
        if (diagResults == null || lastReport == null) return;
        SelfTest.Report rep = lastReport;

        TextView sum = UiKit.text(this, rep.summary, 15,
                rep.failed == 0 ? UiKit.GREEN : (rep.score >= 60 ? UiKit.AMBER : UiKit.RED), true);
        diagResults.addView(sum);

        if (rep.healed) {
            diagResults.addView(UiKit.text(this, "🔧 " + rep.healNote, 12.5f, UiKit.GREEN, false));
        }

        for (final SelfTest.Result x : rep.results) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);

            TextView mark = new TextView(this);
            switch (x.status) {
                case SelfTest.PASS: mark.setText("✔"); mark.setTextColor(UiKit.GREEN); break;
                case SelfTest.WARN: mark.setText("⚠"); mark.setTextColor(UiKit.AMBER); break;
                case SelfTest.FAIL: mark.setText("✖"); mark.setTextColor(UiKit.RED); break;
                default: mark.setText("ℹ"); mark.setTextColor(0xFF8A93C4); break;
            }
            mark.setTextSize(15);
            mark.setTypeface(UiKit.bold());
            mark.setPadding(0, dp(2), dp(10), 0);
            row.addView(mark, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout col = UiKit.vstack(this);
            col.addView(UiKit.text(this, x.name, 14, UiKit.TEXT_MAIN, true));
            col.addView(UiKit.text(this, x.detail, 12, UiKit.TEXT_SUB, false));
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, dp(10), 0, 0);
            diagResults.addView(row, rowLp);
        }

        if (CrashGuard.hasReport()) {
            String s = CrashGuard.lastReportSummary();
            diagResults.addView(UiKit.notice(this,
                    "آخر عطل مسجل:\n" + (s == null ? "—" : s),
                    0xFF1A0E12, 0xFF5A2430, UiKit.RED));
            diagResults.addView(UiKit.secondaryButton(this, "مسح سجل الأعطال", new View.OnClickListener() {
                @Override public void onClick(View v) {
                    CrashGuard.clearReport();
                    Toast.makeText(MainActivity.this, "تم مسح السجل", Toast.LENGTH_SHORT).show();
                    rebuildScreen(TAB_DIAG);
                }
            }));
        }

        diagResults.addView(UiKit.secondaryButton(this, "نسخ التقرير كاملاً", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (lastReport == null) return;
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("DRS-SelfTest",
                            SelfTest.buildReport(lastReport)));
                    Toast.makeText(MainActivity.this, "نُسخ التقرير إلى الحافظة", Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            }
        }));
    }

    // ==================== الشاشة ٢: الثيمات ====================

    private void buildThemes(LinearLayout page) {
        LinearLayout head = UiKit.card(this);
        head.addView(UiKit.title(this, "معرض الثيمات"));
        head.addView(UiKit.body(this,
                "١٦ ثيماً مصممة بعناية — تتبدل تلقائياً بين النهاري والليلي حسب نظام جهازك، مع ٨ ألوان تمييز تطبّق فوراً على اللوحة."));
        addCard(page, head, 12);

        // شبكة الثيمات — عمودان بمعاينة كبيرة
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        for (int i = 0; i < ThemeSet.NAMES.length; i++) {
            final int idx = i;
            boolean sel = idx == prefs.themePreset;
            int[] pv = ThemeSet.PREVIEW[idx];

            LinearLayout cell = UiKit.vstack(this);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(10), dp(12), dp(10), dp(12));
            cell.setClickable(true);
            cell.setBackground(UiKit.outlined(sel ? 0x337C5CFF : UiKit.TILE_BG,
                    18, sel ? UiKit.ACCENT : UiKit.CARD_STROKE, sel ? 1.6f : 1, this));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.setThemePreset(idx);
                    markStale(TAB_THEMES);
                    rebuildScreen(TAB_THEMES);
                    Toast.makeText(MainActivity.this, "ثيم: " + ThemeSet.NAMES[idx], Toast.LENGTH_SHORT).show();
                }
            });

            // معاينة مصغّرة للوحة
            FrameLayout previewWrap = new FrameLayout(this);
            View bg = new View(this);
            GradientDrawable bgg = UiKit.rounded(pv[0], 12, this);
            bgg.setStroke(dp(1), 0x33000000);
            bg.setBackground(bgg);
            previewWrap.addView(bg, new FrameLayout.LayoutParams(dp(120), dp(76)));

            LinearLayout keysCol = UiKit.vstack(this);
            keysCol.setGravity(Gravity.CENTER);
            int[] rowsK = {5, 5, 4};
            for (int rr = 0; rr < 3; rr++) {
                LinearLayout kr = new LinearLayout(this);
                kr.setGravity(Gravity.CENTER);
                for (int k = 0; k < rowsK[rr]; k++) {
                    View kv = new View(this);
                    kv.setBackground(UiKit.rounded(pv[1], 3, this));
                    LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(dp(13), dp(10));
                    klp.setMargins(dp(2), dp(1), dp(2), dp(1));
                    kr.addView(kv, klp);
                }
                keysCol.addView(kr);
            }
            // شريط التمييز أسفل المعاينة
            View bar = new View(this);
            bar.setBackground(UiKit.rounded(pv[2], 2, this));
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    dp(70), dp(5), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barLp.bottomMargin = dp(8);
            previewWrap.addView(keysCol, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            previewWrap.addView(bar, barLp);
            cell.addView(previewWrap);

            LinearLayout nameRow = UiKit.hstack(this);
            nameRow.setGravity(Gravity.CENTER);
            TextView name = new TextView(this);
            name.setText(ThemeSet.NAMES[idx]);
            name.setTextSize(13);
            name.setTypeface(sel ? UiKit.bold() : UiKit.medium());
            name.setTextColor(sel ? UiKit.ACCENT_SOFT : UiKit.TEXT_SUB);
            nameRow.addView(name);
            if (sel) {
                TextView chk = new TextView(this);
                chk.setText(" ✓");
                chk.setTextSize(13);
                chk.setTypeface(UiKit.bold());
                chk.setTextColor(UiKit.ACCENT_SOFT);
                nameRow.addView(chk);
            }
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nLp.setMargins(0, dp(8), 0, 0);
            cell.addView(nameRow, nLp);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(cell, glp);
        }
        addCard(page, grid, 8);

        // ===== ألوان التمييز =====
        LinearLayout accentCard = UiKit.card(this);
        accentCard.addView(UiKit.title(this, "لون التمييز"));
        accentCard.addView(UiKit.body(this, "يلوّن أزرار الوظائف وشريط الاقتراحات في اللوحة."));

        TextView auto = new TextView(this);
        auto.setText("افتراضي الثيم");
        auto.setTextSize(12.5f);
        auto.setTypeface(prefs.accent == 0 ? UiKit.bold() : UiKit.medium());
        auto.setPadding(dp(14), dp(8), dp(14), dp(8));
        auto.setClickable(true);
        auto.setBackground(UiKit.outlined(prefs.accent == 0 ? 0x337C5CFF : 0x00000000, 16,
                prefs.accent == 0 ? UiKit.ACCENT : UiKit.FIELD_STROKE, 1.2f, this));
        auto.setTextColor(prefs.accent == 0 ? UiKit.ACCENT_SOFT : UiKit.TEXT_SUB);
        auto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.setAccent(0);
                markStale(TAB_THEMES);
                rebuildScreen(TAB_THEMES);
            }
        });
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.setMargins(0, dp(10), 0, 0);
        accentCard.addView(auto, aLp);

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 1; i <= ThemeSet.ACCENT_COLORS.length; i++) {
            final int idx = i;
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(ThemeSet.ACCENT_COLORS[i - 1]);
            g.setStroke(prefs.accent == idx ? dp(3) : dp(1),
                    prefs.accent == idx ? 0xFFFFFFFF : 0x33000000);
            dot.setBackground(g);
            dot.setClickable(true);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.setAccent(idx);
                    markStale(TAB_THEMES);
                    rebuildScreen(TAB_THEMES);
                }
            });
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(dp(34), dp(34));
            dLp.setMargins(dp(2), dp(12), dp(10), dp(4));
            dots.addView(dot, dLp);
        }
        accentCard.addView(dots);
        addCard(page, accentCard, 8);
    }

    // ==================== الشاشة ٣: الإعدادات ====================

    private void buildSettings(LinearLayout page) {
        pageSection(page, "⌨", "الكتابة الذكية");
        LinearLayout typing = UiKit.card(this);
        typing.addView(toggle("الاقتراحات الذكية", "شريط يعرض ٣ كلمات متوقعة أثناء الكتابة", prefs.suggest, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setSuggest(b); }
        }));
        typing.addView(toggle("التصحيح التلقائي", "تصحيح الكلمة عند الضغط على المسافة مع إمكانية التراجع", prefs.autoCorrect, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setAutoCorrect(b); }
        }));
        typing.addView(toggle("التنبؤ بالكلمة التالية", "يقترح الكلمة المرجحة بعد الكلمة الحالية", prefs.nextWord, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setNextWord(b); }
        }));
        typing.addView(toggle("الكتابة بالسحب", "مرّر إصبعك على الحروف ليكتب الكلمة كاملة", prefs.glide, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setGlide(b); }
        }));
        typing.addView(toggle("صف الأرقام", "صف أرقام ثابت فوق الحروف", prefs.numRow, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setNumRow(b); }
        }));
        typing.addView(toggle("الحرف الكبير تلقائياً", "أول حرف بعد نقطة أو سطر جديد يُكتب كبيراً", prefs.autoCap, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setAutoCap(b); }
        }));
        typing.addView(toggle("النقطة بضغطتين", "ضغطتان على المسافة تُدخلان نقطة", prefs.doubleSpace, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setDoubleSpace(b); }
        }));
        addCard(page, typing, 8);

        pageSection(page, "📐", "التنسيق والمظهر");
        LinearLayout layoutCard = UiKit.card(this);
        layoutCard.addView(sectionLabel("حجم المفاتيح"));
        layoutCard.addView(UiKit.segmented(this,
                new String[]{"صغير", "متوسط", "كبير"}, prefs.keyHeight, new UiKit.IntListener() {
                    @Override public void on(int i) { prefs.setKeyHeight(i); }
                }));
        layoutCard.addView(sectionLabel("وضع اليد الواحدة"));
        layoutCard.addView(UiKit.segmented(this,
                new String[]{"الوسط", "اليمين", "اليسار"}, prefs.oneHanded, new UiKit.IntListener() {
                    @Override public void on(int i) { prefs.setOneHanded(i); }
                }));
        layoutCard.addView(sectionLabel("سرعة الضغط المطوّل"));
        layoutCard.addView(UiKit.segmented(this,
                new String[]{"سريع", "عادي", "بطيء"}, prefs.longPressIdx, new UiKit.IntListener() {
                    @Override public void on(int i) { prefs.setLongPressIdx(i); }
                }));
        layoutCard.addView(toggle("الأرقام العربية في اللوحة الرقمية", "١٢٣ بدلاً من 123 في لوحة الأرقام", prefs.arabicDigits, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setArabicDigits(b); }
        }));
        layoutCard.addView(toggle("إدخال صوتي", "إملاء النص بالصوت بدل الكتابة", prefs.voice, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setVoice(b); }
        }));
        addCard(page, layoutCard, 8);

        pageSection(page, "🔊", "الصوت واللمس");
        LinearLayout soundCard = UiKit.card(this);
        soundCard.addView(toggle("صوت النقر", "نقرات خفيفة عند الضغط على الأزرار", prefs.sound, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setSound(b); }
        }));
        soundCard.addView(toggle("الاهتزاز اللمسي", "استجابة لمسية عند كل ضغطة", prefs.haptics, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setHaptics(b); }
        }));
        soundCard.addView(sectionLabel("نمط الصوت"));
        soundCard.addView(UiKit.segmented(this,
                new String[]{"كلاسيكي", "رقمي", "ناعم"}, prefs.soundStyle, new UiKit.IntListener() {
                    @Override public void on(int i) { prefs.setSoundStyle(i); }
                }));
        addCard(page, soundCard, 8);

        pageSection(page, "🕶", "الخصوصية");
        LinearLayout privCard = UiKit.card(this);
        privCard.addView(toggle("الوضع التخفي", "إيقاف التعلّم والتقاط الحافظة وسجل الإيموجي مؤقتاً — درع في شريط اللوحة لتفعيله أثناء الكتابة", prefs.incognito, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setIncognito(b); }
        }));
        privCard.addView(UiKit.notice(this,
                "بياناتك لا تغادر جهازك أبداً: القواميس مضمّنة داخل التطبيق والتعلّم محلي بالكامل.",
                UiKit.FIELD_BG, UiKit.FIELD_STROKE, UiKit.TEXT_SUB));
        addCard(page, privCard, 8);

        pageSection(page, "📚", "القاموس الذكي");
        LinearLayout dictCard = UiKit.card(this);
        dictCard.addView(UiKit.body(this,
                "اللوحة تتعلّم كلماتك محلياً وتقترحها أولاً في المرة القادمة."));
        dictCountTv = UiKit.text(this, "كلمات تعلّمتها اللوحة: " + arabicNum(engine.learnedCount()),
                14, UiKit.ACCENT_SOFT, true);
        LinearLayout.LayoutParams dcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dcLp.setMargins(0, dp(8), 0, 0);
        dictCard.addView(dictCountTv, dcLp);
        dictCard.addView(UiKit.secondaryButton(this, "تصفير القاموس المتعلّم", new View.OnClickListener() {
            @Override public void onClick(View v) {
                engine.resetLearned();
                if (dictCountTv != null)
                    dictCountTv.setText("كلمات تعلّمتها اللوحة: " + arabicNum(0));
                Toast.makeText(MainActivity.this, "تم تصفير القاموس", Toast.LENGTH_SHORT).show();
            }
        }));
        addCard(page, dictCard, 8);

        pageSection(page, "⚡", "الاختصارات النصية");
        LinearLayout scCard = UiKit.card(this);
        scCard.addView(UiKit.body(this,
                "اكتب الاختصار ثم مسافة فيتوسّع تلقائياً إلى النص الكامل."));
        shortcutsBox = UiKit.vstack(this);
        scCard.addView(shortcutsBox);
        fillShortcutsBox();
        addCard(page, scCard, 8);

        pageSection(page, "💾", "النسخ الاحتياطي والاستعادة");
        LinearLayout bkCard = UiKit.card(this);
        bkCard.addView(UiKit.body(this,
                "صدّر كل إعداداتك وقاموسك واختصاراتك وحافظتك كنص واحد، واستعدها على أي جهاز."));
        bkCard.addView(UiKit.primaryButton(this, "تصدير نسخة احتياطية", new View.OnClickListener() {
            @Override public void onClick(View v) { exportBackup(); }
        }));
        bkCard.addView(UiKit.secondaryButton(this, "استعادة من نسخة", new View.OnClickListener() {
            @Override public void onClick(View v) { importBackupDialog(); }
        }));
        addCard(page, bkCard, 8);
    }

    /** ترويسة قسم داخل شاشة الإعدادات */
    private void pageSection(LinearLayout page, String glyph, String title) {
        LinearLayout head = UiKit.hstack(this);
        TextView g = UiKit.text(this, glyph, 15, UiKit.ACCENT_SOFT, true);
        head.addView(g);
        TextView t = UiKit.text(this, title, 16, UiKit.TEXT_MAIN, true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.setMargins(dp(10), 0, 0, 0);
        head.addView(t, tLp);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(dp(16), dp(14), dp(16), 0);
        page.addView(head, hLp);
    }

    private TextView sectionLabel(String s) {
        return UiKit.text(this, s, 13, UiKit.TEXT_SUB, false);
    }

    /** صف مفتاح تبديل داخل بطاقة */
    private LinearLayout toggle(String label, String sub, boolean checked, UiKit.BoolListener l) {
        return UiKit.toggleRow(this, label, sub, checked, l);
    }

    /** تعبئة صندوق الاختصارات النصية */
    private void fillShortcutsBox() {
        if (shortcutsBox == null) return;
        shortcutsBox.removeAllViews();
        ArrayList<String[]> list = engine.getShortcuts();
        if (list.isEmpty()) {
            shortcutsBox.addView(UiKit.text(this, "لا توجد اختصارات بعد — أضف أول اختصار بالأسفل.", 12.5f, UiKit.TEXT_FAINT, false));
        }
        for (final String[] s : list) {
            LinearLayout row = UiKit.hstack(this);
            row.setBackground(UiKit.outlined(UiKit.FIELD_BG, 12, UiKit.FIELD_STROKE, 1, this));
            row.setPadding(dp(12), dp(9), dp(8), dp(9));

            TextView tv = new TextView(this);
            tv.setText(s[0] + "  ⤳  " + s[1]);
            tv.setTextSize(13);
            tv.setTextColor(UiKit.TEXT_MAIN);
            tv.setMaxLines(2);
            row.addView(tv, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(this);
            del.setText("✕");
            del.setTextSize(14);
            del.setTypeface(UiKit.bold());
            del.setTextColor(UiKit.RED);
            del.setPadding(dp(12), dp(4), dp(6), dp(4));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    engine.removeShortcut(s[0]);
                    Toast.makeText(MainActivity.this, "حُذف الاختصار", Toast.LENGTH_SHORT).show();
                    fillShortcutsBox();
                }
            });
            row.addView(del, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, 0);
            shortcutsBox.addView(row, lp);
        }

        // نموذج إضافة اختصار جديد
        final EditText abIn = UiKit.field(this, "الاختصار (مثال: سلام)", 1);
        final EditText expIn = UiKit.field(this, "التوسعة الكاملة (مثال: السلام عليكم ورحمة الله)", 1);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.setMargins(0, dp(8), 0, 0);
        shortcutsBox.addView(abIn, fLp);
        shortcutsBox.addView(expIn, fLp);
        shortcutsBox.addView(UiKit.primaryButton(this, "إضافة الاختصار", new View.OnClickListener() {
            @Override public void onClick(View v) {
                String ab = abIn.getText().toString().trim();
                String ex = expIn.getText().toString().trim();
                if (ab.isEmpty() || ex.isEmpty()) {
                    Toast.makeText(MainActivity.this, "املأ الحقلين أولاً", Toast.LENGTH_SHORT).show();
                    return;
                }
                engine.addShortcut(ab, ex);
                Toast.makeText(MainActivity.this, "أُضيف الاختصار", Toast.LENGTH_SHORT).show();
                fillShortcutsBox();
            }
        }));
    }

    // ==================== الشاشة ٥: حول ====================

    private void buildAbout(LinearLayout page) {
        LinearLayout hero = UiKit.vstack(this);
        hero.setGravity(Gravity.CENTER);
        hero.setBackground(UiKit.heroGradient(this));
        hero.setPadding(dp(20), dp(28), dp(20), dp(24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams lgLp = new LinearLayout.LayoutParams(dp(76), dp(76));
        lgLp.gravity = Gravity.CENTER_HORIZONTAL;
        hero.addView(logo, lgLp);

        TextView name = UiKit.text(this, "DRS Smart Keyboard", 21, 0xFFFFFFFF, true);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nLp.setMargins(0, dp(12), 0, 0);
        hero.addView(name, nLp);

        TextView vers = UiKit.text(this,
                "الإصدار " + BuildInfo.VERSION_NAME + " (" + BuildInfo.VERSION_CODE + ") — لوحة مفاتيح عربية متكاملة",
                13, 0xFFC6CDF2, false);
        vers.setGravity(Gravity.CENTER);
        hero.addView(vers);
        addCard(page, hero, 14);

        LinearLayout guide = UiKit.card(this);
        guide.addView(UiKit.title(this, "دليل سريع"));
        guide.addView(guideRow("⌨", "ست لغات كاملة: العربية وEnglish وFrançais وDeutsch وEspañol وTürkçe — زر التبديل في الصف الثالث"));
        guide.addView(guideRow("👆", "اضغط مطولاً على أي حرف لبدائله وحركات التشكيل"));
        glideGuide(guide);
        guide.addView(guideRow("📋", "زر «تحرير» يفتح لوحة المؤشر والحافظة والنسخ واللصق"));
        guide.addView(guideRow("🎨", "غيّر الثيم ولون التمييز من تبويب الثيمات — يطبّق فوراً"));
        guide.addView(guideRow("🛡", "الفحص الذكي يفحص ١٤ مكوناً ويصلح الإعدادات التالفة ذاتياً"));
        addCard(page, guide, 8);

        LinearLayout info = UiKit.card(this);
        info.addView(UiKit.title(this, "معلومات"));
        info.addView(UiKit.body(this,
        "تطبيق مفتوح المصدر بلا إعلانات ولا أذونات شبكة: كل شيء يعمل محلياً على جهازك. " +
        "تشمل المميزات: تنبؤ وتصحيح ذكي بقاموسين مضمّنين، كتابة بالسحب، اختصارات نصية، مدير حافظة، " +
        "وضع عائم، وضع تخفي، إدخال صوتي، ١٦ ثيماً نهارية وليلية، ونسخ احتياطي كامل."));
        info.addView(UiKit.notice(this,
        "المطوّر: DRS — مبني بأدوات مفتوحة، بدون Gradle، برخصة مفتوحة.",
        UiKit.FIELD_BG, UiKit.FIELD_STROKE, UiKit.TEXT_SUB));
        addCard(page, info, 8);

        LinearLayout tips = UiKit.card(this);
        tips.addView(UiKit.caption(this,
        "نصيحة: إذا غيّرت أي إعداد ولم يظهر أثره فأعد فتح أي حقل كتابة لإعادة بناء اللوحة. " +
        "لأي مشكلة شغّل الفحص الذكي — يخبرك بالضبط ما الخلل وكيف يُصلح."));
        addCard(page, tips, 8);
    }

    private void glideGuide(LinearLayout guide) {
        guide.addView(guideRow("✍", "الكتابة بالسحب: مرّر إصبعك فوق حروف الكلمة دفعة واحدة"));
    }

    private LinearLayout guideRow(String glyph, String txt) {
        LinearLayout row = UiKit.hstack(this);
        row.setGravity(Gravity.TOP);
        TextView g = UiKit.text(this, glyph, 13, UiKit.ACCENT_SOFT, true);
        row.addView(g);
        TextView t = UiKit.text(this, txt, 13, UiKit.TEXT_SUB, false);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.setMargins(dp(10), 0, 0, 0);
        row.addView(t, tLp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    // ==================== النسخ الاحتياطي ====================

    /** تصدير كل البيانات كنص JSON للحافظة والمشاركة */
    private void exportBackup() {
        try {
            android.content.SharedPreferences panelSp =
                    getSharedPreferences("kb_panel", Context.MODE_PRIVATE);
            JSONObject o = new JSONObject();
            o.put("app", "DRS-Smart-Keyboard");
            o.put("v", 25);
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
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("DRS-Backup", json));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, json);
            try {
                startActivity(Intent.createChooser(send, "تصدير النسخة الاحتياطية"));
            } catch (Exception ignored) {}
            Toast.makeText(this, "صُدّرت النسخة الاحتياطية إلى الحافظة والمشاركة", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر التصدير", Toast.LENGTH_SHORT).show();
        }
    }

    /** نافذة استيراد النسخة الاحتياطية */
    private void importBackupDialog() {
        final EditText in = UiKit.field(this, "الصق نص النسخة الاحتياطية هنا", 4);
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
                .setTitle("استعادة نسخة احتياطية")
                .setView(in)
                .setPositiveButton("استعادة", new DialogInterface.OnClickListener() {
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
            Toast.makeText(this, "استُعدت كل البيانات بنجاح", Toast.LENGTH_LONG).show();
            for (int i = 0; i < TAB_COUNT; i++) markStale(i);
            rebuildScreen(currentTab);
            stale[currentTab] = false;
        } catch (Exception e) {
            Toast.makeText(this, "نص النسخة الاحتياطية غير صالح", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== أدوات مساعدة ====================

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** يضيف بطاقة (أو أي عرض) إلى الصفحة بهوامش موحدة */
    private void addCard(LinearLayout page, View card, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), dp(topMarginDp), dp(8), dp(4));
        page.addView(card, lp);
    }

    /** يلفّ حاوية داخلية ببطاقة جاهزة */
    private LinearLayout wrapCard(LinearLayout inner) {
        LinearLayout card = UiKit.card(this);
        card.addView(inner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    /** تحويل رقم إلى أرقام عربية للعرض */
    private String arabicNum(int n) {
        String s = String.valueOf(n);
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') b.append((char) ('٠' + (ch - '0')));
            else b.append(ch);
        }
        return b.toString();
    }

    private int countEmojis() {
        int total = 0;
        for (int g = 1; g < Layouts.EMOJI_GROUPS.length; g++)
            total += Layouts.emojisOf(g).length;
        return total;
    }

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
        fillStatusBox();
    }

    // ==================== أيقونات شريط التنقل ====================

    /** أيقونة مرسومة بالكامل عبر Canvas لكل تبويب */
    private class TabIcon extends View {
        private final int type;
        private boolean active;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        TabIcon(Context c, int type) {
            super(c);
            this.type = type;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.8f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            setActive(type == TAB_HOME);
        }

        void setActive(boolean on) {
            active = on;
            p.setColor(on ? UiKit.ACCENT_SOFT : UiKit.TEXT_FAINT);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f;
            path.reset();
            switch (type) {
                case TAB_HOME: {
                    // بيت: سقف + جسم
                    path.moveTo(cx - w * 0.32f, cy + h * 0.05f);
                    path.lineTo(cx, cy - h * 0.34f);
                    path.lineTo(cx + w * 0.32f, cy + h * 0.05f);
                    canvas.drawPath(path, p);
                    path.reset();
                    path.moveTo(cx - w * 0.22f, cy);
                    path.lineTo(cx - w * 0.22f, cy + h * 0.34f);
                    path.lineTo(cx + w * 0.22f, cy + h * 0.34f);
                    path.lineTo(cx + w * 0.22f, cy);
                    canvas.drawPath(path, p);
                    break;
                }
                case TAB_THEMES: {
                    // لوحة ألوان: مربعات ٢×٢
                    float s = w * 0.26f, gap = w * 0.10f;
                    p.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(cx - s - gap, cy - s - gap, cx - gap, cy - gap, dp(3), dp(3), p);
                    canvas.drawRoundRect(cx + gap, cy - s - gap, cx + gap + s, cy - gap, dp(3), dp(3), p);
                    p.setStyle(Paint.Style.STROKE);
                    canvas.drawRoundRect(cx - s - gap, cy + gap, cx - gap, cy + gap + s, dp(3), dp(3), p);
                    canvas.drawRoundRect(cx + gap, cy + gap, cx + gap + s, cy + gap + s, dp(3), dp(3), p);
                    break;
                }
                case TAB_SETTINGS: {
                    // منزلقات: ثلاثة خطوط بمقابض
                    for (int i = -1; i <= 1; i++) {
                        float y = cy + i * h * 0.28f;
                        canvas.drawLine(cx - w * 0.34f, y, cx + w * 0.34f, y, p);
                        p.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(cx + i * w * 0.18f, y, dp(2.6f), p);
                        p.setStyle(Paint.Style.STROKE);
                    }
                    break;
                }
                case TAB_DIAG: {
                    // درع
                    path.moveTo(cx, cy - h * 0.38f);
                    path.lineTo(cx + w * 0.32f, cy - h * 0.18f);
                    path.lineTo(cx + w * 0.30f, cy + h * 0.12f);
                    path.quadTo(cx + w * 0.20f, cy + h * 0.38f, cx, cy + h * 0.44f);
                    path.quadTo(cx - w * 0.20f, cy + h * 0.38f, cx - w * 0.30f, cy + h * 0.12f);
                    path.lineTo(cx - w * 0.32f, cy - h * 0.18f);
                    path.close();
                    canvas.drawPath(path, p);
                    break;
                }
                default: {
                    // معلومات: دائرة + i
                    canvas.drawCircle(cx, cy, w * 0.36f, p);
                    p.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx, cy - h * 0.14f, dp(1.6f), p);
                    canvas.drawRect(cx - dp(1.2f), cy - dp(1), cx + dp(1.2f), cy + h * 0.20f, p);
                    p.setStyle(Paint.Style.STROKE);
                    break;
                }
            }
        }
    }
}
