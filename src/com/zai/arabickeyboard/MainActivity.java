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
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
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
 * التطبيق الرئيسي — DRS Smart Keyboard v2.7
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
    private static final int[] TAB_ICONS = {
            Icon.HOUSE, Icon.SWATCHES, Icon.SLIDERS, Icon.SHIELD, Icon.INFO
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
        // الطبقة الصفرية: خلفية Aurora زجاجية (تظهر شفافية البطاقات عبرها)
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.addView(new AuroraBg(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        rootFrame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ===== الشريط العلوي — بطاقة زجاجية بحد مضيء وشعار متوهج =====
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackground(UiKit.glass(this, 28));
        header.setPadding(dp(14), dp(12), dp(14), dp(12));

        // شعار متوهج: هالة شعاعية + مربع متدرج + أيقونة لوحة مفاتيح مرسومة
        FrameLayout logoWrap = new FrameLayout(this);
        View glow = new View(this);
        GradientDrawable gg = new GradientDrawable();
        gg.setShape(GradientDrawable.OVAL);
        gg.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gg.setColors(new int[]{0x808A63FF, 0x208A63FF, 0x00000000});
        gg.setGradientRadius(dp(34));
        glow.setBackground(gg);
        logoWrap.addView(glow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout squircle = UiKit.iconBadge(this, Icon.KEYBOARD, 46, 25);
        logoWrap.addView(squircle, new FrameLayout.LayoutParams(
                dp(46), dp(46), Gravity.CENTER));
        header.addView(logoWrap, new LinearLayout.LayoutParams(dp(50), dp(50)));

        LinearLayout hCol = UiKit.vstack(this);
        TextView hTitle = new TextView(this);
        hTitle.setText("DRS Smart Keyboard");
        hTitle.setTextSize(16.5f);
        hTitle.setTypeface(UiKit.bold());
        hTitle.setTextColor(UiKit.TEXT_MAIN);
        hCol.addView(hTitle);
        TextView hSub = new TextView(this);
        hSub.setText("لوحة المفاتيح العربية الذكية");
        hSub.setTextSize(10.5f);
        hSub.setTextColor(UiKit.TEXT_SUB);
        hCol.addView(hSub);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        htLp.setMargins(dp(12), 0, dp(8), 0);
        header.addView(hCol, htLp);

        TextView ver = UiKit.chip(this, "v" + BuildInfo.VERSION_NAME,
                0x227C5CFF, 0x557C5CFF, UiKit.ACCENT_SOFT);
        header.addView(ver);

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(dp(10), dp(10), dp(10), dp(2));
        root.addView(header, hLp);
        // ===== محتوى الشاشات =====
        content = new FrameLayout(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(content, cLp);

        // ===== شريط التنقل السفلي — قرص زجاجي عائم بحد مضيء =====
        FrameLayout navWrap = new FrameLayout(this);
        navWrap.setPadding(dp(10), dp(6), dp(10), dp(10));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable nbg = new GradientDrawable();
        nbg.setOrientation(GradientDrawable.Orientation.TL_BR);
        nbg.setColors(new int[]{0xD9182042, 0xA80D1329});
        nbg.setCornerRadius(dp(28));
        nbg.setStroke(dp(1), 0x3CFFFFFF);
        nav.setBackground(nbg);
        nav.setPadding(dp(6), dp(7), dp(6), dp(7));
        for (int i = 0; i < TAB_COUNT; i++) {
            navItems[i] = navItem(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nav.addView(navItems[i], lp);
        }
        navWrap.addView(nav, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(navWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(rootFrame);
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
        item.setPadding(0, dp(6), 0, dp(6));

        Icon icon = new Icon(this, TAB_ICONS[index], UiKit.TEXT_FAINT);
        item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
        UiKit.pressScale(item, 0.92f);

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
            // كبسولة زجاجية متوهجة للتبويب النشط
            GradientDrawable ib = new GradientDrawable();
            ib.setOrientation(GradientDrawable.Orientation.TL_BR);
            if (on) {
                ib.setColors(new int[]{0x527C5CFF, 0x2E5B3FE0});
                ib.setStroke(dp(1), 0x669D7BFF);
            } else {
                ib.setColors(new int[]{0x00000000, 0x00000000});
            }
            ib.setCornerRadius(dp(21));
            item.setBackground(ib);
            TextView label = (TextView) item.getTag();
            label.setTextColor(on ? 0xFFD4C6FF : UiKit.TEXT_FAINT);
            ((Icon) item.getChildAt(0)).setActive(on, 0xFFD4C6FF, UiKit.TEXT_FAINT);
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
        // انتقال ناعم: تلاشٍ + انزلاق خفيف للأعلى
        AnimationSet st = new AnimationSet(true);
        AlphaAnimation a = new AlphaAnimation(0.35f, 1f);
        TranslateAnimation tr = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.018f, Animation.RELATIVE_TO_SELF, 0f);
        st.addAnimation(a);
        st.addAnimation(tr);
        st.setDuration(230);
        st.setInterpolator(new DecelerateInterpolator(1.5f));
        screens[index].startAnimation(st);
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

    /** بناء شاشة رقم i — شفافة لتظهر خلفية Aurora عبر البطاقات الزجاجية */
    private ScrollView buildScreen(int index) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(0x00000000);
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

        LinearLayout t1 = UiKit.tile(this, Icon.PALETTE, "الثيمات", "١٦ ثيماً و٨ ألوان", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_THEMES); }
        });
        LinearLayout t2 = UiKit.tile(this, Icon.SHIELD, "الفحص الذكي", "١٤ فحصاً شاملاً", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_DIAG); }
        });
        LinearLayout t3 = UiKit.tile(this, Icon.GEAR, "الإعدادات", "تحكم كامل", new View.OnClickListener() {
            @Override public void onClick(View v) { showTab(TAB_SETTINGS); }
        });
        LinearLayout t4 = UiKit.tile(this, Icon.INFO, "حول التطبيق", "دليل الاستخدام", new View.OnClickListener() {
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
        t.setBackground(UiKit.glass(this, 22));
        t.setPadding(dp(6), dp(15), dp(6), dp(15));
        TextView v = UiKit.text(this, value, 23, 0xFFD4C6FF, true);
        v.setGravity(Gravity.CENTER);
        UiKit.glowText(v, 0x998A63FF, 7, this); // توهج بنفسجي ناعم حول الرقم
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
        LinearLayout runRow = UiKit.hstack(this);
        Icon runIc = new Icon(this, Icon.HOURGLASS, UiKit.AMBER);
        runRow.addView(runIc, new LinearLayout.LayoutParams(dp(16), dp(16)));
        TextView runTx = UiKit.text(this, "جارٍ فحص كل المكوّنات...", 14, UiKit.AMBER, true);
        LinearLayout.LayoutParams runLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        runLp.setMargins(dp(8), 0, 0, 0);
        runRow.addView(runTx, runLp);
        diagResults.addView(runRow);

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
            LinearLayout healRow = UiKit.hstack(this);
            Icon healIc = new Icon(this, Icon.WRENCH, UiKit.GREEN);
            healRow.addView(healIc, new LinearLayout.LayoutParams(dp(15), dp(15)));
            TextView healTx = UiKit.text(this, rep.healNote, 12.5f, UiKit.GREEN, false);
            LinearLayout.LayoutParams healLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            healLp.setMargins(dp(8), 0, 0, 0);
            healRow.addView(healTx, healLp);
            diagResults.addView(healRow);
        }

        for (final SelfTest.Result x : rep.results) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);

            int markIcon; int markColor;
            switch (x.status) {
                case SelfTest.PASS: markIcon = Icon.CHECK_CIRCLE; markColor = UiKit.GREEN; break;
                case SelfTest.WARN: markIcon = Icon.WARN_TRIANGLE; markColor = UiKit.AMBER; break;
                case SelfTest.FAIL: markIcon = Icon.CROSS_CIRCLE; markColor = UiKit.RED; break;
                default: markIcon = Icon.INFO; markColor = 0xFF8A93C4; break;
            }
            Icon mark = new Icon(this, markIcon, markColor);
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(dp(17), dp(17));
            mLp.setMargins(0, dp(2), dp(10), 0);
            row.addView(mark, mLp);

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
            if (sel) {
                GradientDrawable selBg = new GradientDrawable();
                selBg.setOrientation(GradientDrawable.Orientation.TL_BR);
                selBg.setColors(new int[]{0x477C5CFF, 0x2E5B3FE0});
                selBg.setCornerRadius(dp(20));
                selBg.setStroke(dp(1.4f), 0x8A9D7BFF);
                cell.setBackground(selBg);
            } else {
                cell.setBackground(UiKit.glass(this, 20));
            }
            UiKit.pressScale(cell, 0.96f);
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
        pageSection(page, Icon.KEYBOARD, "الكتابة الذكية");
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

        pageSection(page, Icon.RULER, "التنسيق والمظهر");
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

        pageSection(page, Icon.SPEAKER, "الصوت واللمس");
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

        pageSection(page, Icon.EYE_OFF, "الخصوصية");
        LinearLayout privCard = UiKit.card(this);
        privCard.addView(toggle("الوضع التخفي", "إيقاف التعلّم والتقاط الحافظة وسجل الإيموجي مؤقتاً — درع في شريط اللوحة لتفعيله أثناء الكتابة", prefs.incognito, new UiKit.BoolListener() {
            @Override public void on(boolean b) { prefs.setIncognito(b); }
        }));
        privCard.addView(UiKit.notice(this,
                "بياناتك لا تغادر جهازك أبداً: القواميس مضمّنة داخل التطبيق والتعلّم محلي بالكامل.",
                UiKit.FIELD_BG, UiKit.FIELD_STROKE, UiKit.TEXT_SUB));
        addCard(page, privCard, 8);

        pageSection(page, Icon.BOOK, "القاموس الذكي");
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

        pageSection(page, Icon.BOLT, "الاختصارات النصية");
        LinearLayout scCard = UiKit.card(this);
        scCard.addView(UiKit.body(this,
                "اكتب الاختصار ثم مسافة فيتوسّع تلقائياً إلى النص الكامل."));
        shortcutsBox = UiKit.vstack(this);
        scCard.addView(shortcutsBox);
        fillShortcutsBox();
        addCard(page, scCard, 8);

        pageSection(page, Icon.SAVE, "النسخ الاحتياطي والاستعادة");
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

    /** ترويسة قسم داخل شاشة الإعدادات — أيقونة داخل شارة زجاجية متوهجة */
    private void pageSection(LinearLayout page, int iconType, String title) {
        LinearLayout head = UiKit.hstack(this);
        FrameLayout badge = UiKit.iconBadge(this, iconType, 30, 16);
        head.addView(badge, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView t = UiKit.text(this, title, 16, UiKit.TEXT_MAIN, true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.setMargins(dp(10), 0, 0, 0);
        head.addView(t, tLp);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(dp(14), dp(14), dp(14), 0);
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
            GradientDrawable rowG = new GradientDrawable();
            rowG.setOrientation(GradientDrawable.Orientation.TL_BR);
            rowG.setColors(new int[]{0x16FFFFFF, 0x0BFFFFFF});
            rowG.setCornerRadius(dp(14));
            rowG.setStroke(dp(1), 0x2EFFFFFF);
            row.setBackground(rowG);
            row.setPadding(dp(12), dp(9), dp(8), dp(9));

            TextView tv = new TextView(this);
            tv.setText(s[0] + "  ⤳  " + s[1]);
            tv.setTextSize(13);
            tv.setTextColor(UiKit.TEXT_MAIN);
            tv.setMaxLines(2);
            row.addView(tv, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Icon del = new Icon(this, Icon.CROSS, UiKit.RED);
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
                    dp(26), dp(20)));

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
        guide.addView(guideRow(Icon.KEYBOARD, "ست لغات كاملة: العربية وEnglish وFrançais وDeutsch وEspañol وTürkçe — زر التبديل في الصف الثالث"));
        guide.addView(guideRow(Icon.TAP, "اضغط مطولاً على أي حرف لبدائله وحركات التشكيل"));
        glideGuide(guide);
        guide.addView(guideRow(Icon.CLIPBOARD, "زر «تحرير» يفتح لوحة المؤشر والحافظة والنسخ واللصق"));
        guide.addView(guideRow(Icon.PALETTE, "غيّر الثيم ولون التمييز من تبويب الثيمات — يطبّق فوراً"));
        guide.addView(guideRow(Icon.SHIELD, "الفحص الذكي يفحص ١٤ مكوناً ويصلح الإعدادات التالفة ذاتياً"));
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
        guide.addView(guideRow(Icon.PEN, "الكتابة بالسحب: مرّر إصبعك فوق حروف الكلمة دفعة واحدة"));
    }

    private LinearLayout guideRow(int iconType, String txt) {
        LinearLayout row = UiKit.hstack(this);
        row.setGravity(Gravity.TOP);
        Icon g = new Icon(this, iconType, UiKit.ACCENT_SOFT);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(dp(15), dp(15));
        gLp.setMargins(0, dp(2), 0, 0);
        row.addView(g, gLp);
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

}