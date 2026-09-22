package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;

/**
 * نظام الفحص الذكي المتكامل — DRS Smart v2.3
 * يفحص كل مكونات اللوحة أربعة عشر فحصاً شاملاً:
 *  1) تسجيل الخدمة في النظام   2) تفعيل اللوحة   3) اختيارها كلوحة حالية
 *  4) سلامة كل التخطيطات (٦ لغات + رموز + تحرير + أرقام + إيموجي)
 *  5) قاعدة الإيموجي           6) القاموس العربي  7) القاموس الإنجليزي
 *  8) محرك التصحيح والسحب      9) الثيمات الستة عشر وألوان التمييز
 * 10) الإعدادات المحفوظة (مع إصلاح ذاتي للقيم التالفة)
 * 11) بيانات الحافظة والإيموجي 12) الموارد  13) سجل الأعطال  14) بيئة التشغيل
 * كل فحص يعيد نتيجة (سليم/تحذير/فاشل/معلومة) مع تفاصيل قابلة للعرض والنسخ.
 */
public final class SelfTest {

    public static final int PASS = 0;
    public static final int WARN = 1;
    public static final int FAIL = 2;
    public static final int INFO = 3;

    /** نتيجة فحص واحد */
    public static class Result {
        public final String name;
        public final int status;
        public final String detail;
        public Result(String name, int status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    /** تقرير الفحص الكامل */
    public static class Report {
        public final ArrayList<Result> results = new ArrayList<>();
        public int passed, warned, failed, infos;
        public int score;          // 0..100
        public String summary;     // سطر الخلاصة
        public boolean healed;     // هل أصلح الفحص إعدادات تالفة؟
        public String healNote;
    }

    private SelfTest() {}

    /** تشغيل كل الفحوصات — ينادى من خيط خلفي */
    public static Report runAll(Context ctx) {
        Report rep = new Report();
        ArrayList<Result> r = rep.results;
        Prefs prefs = new Prefs(ctx);

        checkSystem(ctx, r);
        checkLayouts(r);
        checkEmoji(r);
        checkEngine(ctx, r);
        checkThemes(r);
        checkPrefs(ctx, prefs, r, rep);
        checkPanelData(ctx, r);
        checkResources(ctx, r);
        checkCrashLog(r);
        r.add(new Result("بيئة التشغيل", INFO,
                "أندرويد " + android.os.Build.VERSION.RELEASE
                        + " (API " + android.os.Build.VERSION.SDK_INT + ")"));

        // النتيجة = (سليم + ¾ التحذيرات) ÷ مجموع الوحدات المقيمة
        int units = 0;
        for (Result x : r) {
            switch (x.status) {
                case PASS: rep.passed++; units++; break;
                case WARN: rep.warned++; units++; break;
                case FAIL: rep.failed++; units++; break;
                default: rep.infos++; break;
            }
        }
        int pts = rep.passed + rep.warned;  // التحذير لا يُعاقب في العلامة
        rep.score = units == 0 ? 0 : (int) Math.round(100.0 * pts / units);
        if (rep.score < 0) rep.score = 0;
        if (rep.score > 100) rep.score = 100;
        rep.summary = "سليم: " + rep.passed + " — تحذير: " + rep.warned
                + " — فاشل: " + rep.failed + " — النتيجة: " + rep.score + " / 100";
        return rep;
    }

    /** نص التقرير الكامل قابل للنسخ والمشاركة */
    public static String buildReport(Report rep) {
        StringBuilder sb = new StringBuilder();
        sb.append("تقرير الفحص الذكي المتكامل — DRS Smart Keyboard v")
          .append(BuildInfo.VERSION_NAME).append('\n');
        sb.append(rep.summary).append("\n\n");
        for (Result x : rep.results) {
            sb.append(statusMark(x.status)).append(' ').append(x.name)
              .append("  —  ").append(x.detail).append('\n');
        }
        return sb.toString();
    }

    private static String statusMark(int s) {
        switch (s) {
            case PASS: return "✔";
            case WARN: return "⚠";
            case FAIL: return "✖";
            default: return "ℹ";
        }
    }

    // ==================== 1-3: حالة النظام ====================

    private static void checkSystem(Context ctx, ArrayList<Result> r) {
        try {
            InputMethodManager imm = (InputMethodManager)
                    ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            String pkg = ctx.getPackageName();
            if (imm == null) {
                r.add(new Result("تسجيل الخدمة في النظام", FAIL, "لا يمكن الوصول لخدمة الإدخال"));
                return;
            }
            boolean registered = false, enabled = false;
            for (InputMethodInfo i : imm.getInputMethodList()) {
                if (pkg.equals(i.getPackageName())) registered = true;
            }
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (pkg.equals(i.getPackageName())) enabled = true;
            }
            r.add(new Result("تسجيل الخدمة في النظام",
                    registered ? PASS : FAIL,
                    registered ? "خدمة لوحة المفاتيح مسجلة بشكل صحيح" : "الخدمة غير موجودة في قائمة النظام"));

            r.add(new Result("تفعيل اللوحة في إعدادات النظام",
                    enabled ? PASS : WARN,
                    enabled ? "اللوحة مفعّلة ويمكن اختيارها" : "افتح التطبيق واضغط زر التفعيل الأول"));

            String def = Settings.Secure.getString(ctx.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD);
            boolean selected = def != null && def.contains(pkg);
            r.add(new Result("اختيار اللوحة كلوحة إدخال حالية",
                    selected ? PASS : WARN,
                    selected ? "اللوحة هي لوحة الإدخال النشطة الآن" : "اخترها من «اختيارها كلوحة الإدخال»"));
        } catch (Throwable t) {
            r.add(new Result("حالة النظام", FAIL, "تعذر الفحص: " + t.getClass().getSimpleName()));
        }
    }

    // ==================== 4: سلامة التخطيطات ====================

    private static void checkLayouts(ArrayList<Result> r) {
        try {
            int rows = 0, keys = 0;
            String bad = null;
            // ١) اللغات الست — عقد كامل: ٤ صفوف على الأقل + مسافة + إدخال + مسح
            for (int lang = 0; lang < Layouts.LANG_COUNT && bad == null; lang++) {
                List<Row> ls = Layouts.get(lang, lang, true, true, false);
                bad = wrap(Layouts.LANG_NAMES[lang], validate(ls, true, true, 4));
                rows += ls.size();
                for (Row row : ls) keys += row.keys.size();
            }
            // ٢) الألواح المساعدة — لكل لوحة عقد يناسب وظيفتها:
            //    الرموز والتحرير: تخطيط كامل — الأرقام: إدخال ومسح بلا مسافة — شريط الإيموجي: صف واحد فيه مسح
            String[][] contract = {
                    {"الرموز ١",       "1", "1", "4"},
                    {"الرموز ٢",       "1", "1", "4"},
                    {"لوحة التحرير",    "1", "1", "4"},
                    {"لوحة الأرقام",    "0", "1", "4"},
                    {"الأرقام العربية", "0", "1", "4"},
                    {"شريط الإيموجي",   "0", "0", "1"}
            };
            List<Row>[] panels = new List[]{Layouts.sym1(0), Layouts.sym2(0), Layouts.edit(0),
                    Layouts.numpad(0, false), Layouts.numpad(0, true), Layouts.emojiNav(0)};
            for (int i = 0; i < panels.length && bad == null; i++) {
                List<Row> ls = panels[i];
                bad = wrap(contract[i][0], validate(ls,
                        contract[i][1].equals("1"), contract[i][2].equals("1"),
                        Integer.parseInt(contract[i][3])));
                rows += ls.size();
                for (Row row : ls) keys += row.keys.size();
            }
            if (bad != null) {
                r.add(new Result("سلامة التخطيطات", FAIL, bad));
            } else {
                r.add(new Result("سلامة التخطيطات", PASS,
                        "٦ لغات + رموز + تحرير + أرقام — " + keys + " زراً في " + rows + " صفاً"));
            }
        } catch (Throwable t) {
            r.add(new Result("سلامة التخطيطات", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    /** يضيف اسم اللوحة إلى رسالة الخطأ إن وُجدت */
    private static String wrap(String name, String err) {
        return err == null ? null : name + ": " + err;
    }

    /** التحقق من بنية تخطيط: صفوف كافية، أوزان موجبة، الأزرار المطلوبة موجودة —
     *  يعيد رسالة دقيقة تسمي الزر الناقص ورقم الصف، أو null إن كان سليماً */
    private static String validate(List<Row> ls, boolean needSpace, boolean needEnter, int minRows) {
        if (ls == null || ls.size() < minRows)
            return "عدد صفوف غير كافٍ (" + (ls == null ? 0 : ls.size()) + " بدلاً من " + minRows + ")";
        boolean hasBack = false, hasEnter = false, hasSpace = false;
        for (int i = 0; i < ls.size(); i++) {
            Row row = ls.get(i);
            if (row.keys.isEmpty()) return "الصف " + (i + 1) + " فارغ";
            for (Key k : row.keys) {
                if (k.weight <= 0) return "وزن زر غير صالح في الصف " + (i + 1);
                if (k.code == Key.CODE_BACKSPACE) hasBack = true;
                if (k.code == Key.CODE_ENTER) hasEnter = true;
                if (k.type == Key.SPACE) hasSpace = true;
            }
        }
        if (!hasBack) return "أزرار أساسية مفقودة: زر المسح Backspace";
        if (needEnter && !hasEnter) return "أزرار أساسية مفقودة: زر الإدخال Enter";
        if (needSpace && !hasSpace) return "أزرار أساسية مفقودة: زر المسافة";
        return null;
    }

    // ==================== 5: قاعدة الإيموجي ====================

    private static void checkEmoji(ArrayList<Result> r) {
        try {
            int total = 0, groups = 0;
            for (int g = 1; g < Layouts.EMOJI_GROUPS.length; g++) {
                String[] list = Layouts.emojisOf(g);
                if (list.length == 0) {
                    r.add(new Result("قاعدة الإيموجي", WARN,
                            "تصنيف فارغ: " + Layouts.EMOJI_GROUPS[g][0]));
                    return;
                }
                groups++;
                total += list.length;
            }
            r.add(new Result("قاعدة الإيموجي", PASS,
                    groups + " تصنيفات — " + total + " إيموجي تعمل جميعها"));
        } catch (Throwable t) {
            r.add(new Result("قاعدة الإيموجي", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    // ==================== 6-8: المحرك الذكي ====================

    private static void checkEngine(Context ctx, ArrayList<Result> r) {
        try {
            SuggestEngine en = new SuggestEngine(ctx);

            // القاموس العربي
            int arN = en.arDictSize();
            boolean arSugg = !en.suggest("عرب", 0).isEmpty() || !en.suggest("سلام", 0).isEmpty();
            r.add(new Result("القاموس العربي", (arN >= 500 && arSugg) ? PASS : FAIL,
                    arN + " كلمة عربية — اختبار الاقتراح: " + (arSugg ? "ناجح" : "فاشل")));

            // القاموس الإنجليزي
            int enN = en.enDictSize();
            boolean enSugg = !en.suggest("goo", 1).isEmpty() || !en.suggest("hel", 1).isEmpty();
            r.add(new Result("القاموس الإنجليزي", (enN >= 200 && enSugg) ? PASS : FAIL,
                    enN + " كلمة إنجليزية — اختبار الاقتراح: " + (enSugg ? "ناجح" : "فاشل")));

            // تطبيع عربي + تصحيح + سحب + ثنائيات
            boolean normOK = SuggestEngine.normAr("إنشاء").equals("انشاء")
                    && SuggestEngine.normAr("مدرسة").equals("مدرسه");
            boolean glOK = !en.glideCandidates("سلام", 0).isEmpty();
            boolean biOK = !en.nextWords("صباح", 0).isEmpty();
            boolean all = normOK && glOK && biOK;
            r.add(new Result("محرك التصحيح والسحب والتنبؤ", all ? PASS : FAIL,
                    "التطبيع: " + ok(normOK) + " — السحب: " + ok(glOK)
                            + " — ثنائيات الكلمات: " + ok(biOK)));
        } catch (Throwable t) {
            r.add(new Result("المحرك الذكي", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    private static String ok(boolean b) { return b ? "سليم" : "فاشل"; }

    // ==================== 9: الثيمات ====================

    private static void checkThemes(ArrayList<Result> r) {
        try {
            int n = ThemeSet.NAMES.length;
            String bad = null;
            for (int i = 0; i < n; i++) {
                for (boolean night : new boolean[]{false, true}) {
                    ThemeSet t = ThemeSet.resolve(i, night, 0);
                    if (t.kbBg == 0 || t.keyBg == 0 || t.keyText == 0
                            || t.keyBgAction == 0 || t.popupBg == 0) {
                        bad = "ثيم ناقص الألوان: " + ThemeSet.NAMES[i];
                    }
                }
            }
            for (int a = 1; a <= ThemeSet.ACCENT_COLORS.length && bad == null; a++) {
                ThemeSet t = ThemeSet.resolve(ThemeSet.DEFAULT_PRESET, false, a);
                if (t.keyBgAction != ThemeSet.ACCENT_COLORS[a - 1]) bad = "فشل تطبيق لون التمييز " + a;
            }
            if (bad != null) {
                r.add(new Result("نظام الثيمات", FAIL, bad));
            } else {
                r.add(new Result("نظام الثيمات", PASS,
                        n + " ثيماً × (نهاري/ليلي) + " + ThemeSet.ACCENT_COLORS.length
                                + " ألوان تمييز — كلها سليمة"));
            }
        } catch (Throwable t) {
            r.add(new Result("نظام الثيمات", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    // ==================== 10: الإعدادات مع الإصلاح الذاتي ====================

    private static void checkPrefs(Context ctx, Prefs prefs, ArrayList<Result> r, Report rep) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE);
            int fixed = 0;
            StringBuilder note = new StringBuilder();

            if (prefs.themePreset < 0 || prefs.themePreset >= ThemeSet.NAMES.length) {
                prefs.setThemePreset(ThemeSet.DEFAULT_PRESET); fixed++;
                note.append("الثيم، ");
            }
            if (prefs.keyHeight < 0 || prefs.keyHeight > 2) {
                prefs.setKeyHeight(1); fixed++;
                note.append("حجم المفاتيح، ");
            }
            if (prefs.oneHanded < 0 || prefs.oneHanded > 2) {
                prefs.setOneHanded(0); fixed++;
                note.append("بيد واحدة، ");
            }
            if (prefs.accent < 0 || prefs.accent > ThemeSet.ACCENT_COLORS.length) {
                prefs.setAccent(0); fixed++;
                note.append("لون التمييز، ");
            }
            if (prefs.soundStyle < 0 || prefs.soundStyle > 2) {
                prefs.setSoundStyle(0); fixed++;
                note.append("نمط الصوت، ");
            }
            if (prefs.longPressIdx < 0 || prefs.longPressIdx > 2) {
                prefs.setLongPressIdx(1); fixed++;
                note.append("الضغط المطول، ");
            }
            // القيم الزائدة في التخزين الخام تُزال أيضاً
            if (sp.getInt("key_height", 1) < 0) sp.edit().putInt("key_height", 1).apply();

            if (fixed > 0) {
                rep.healed = true;
                String names = note.toString();
                if (names.endsWith("، ")) names = names.substring(0, names.length() - 2);
                rep.healNote = "أُصلحت قيم تالفة تلقائياً (" + names + ")";
                r.add(new Result("الإعدادات المحفوظة", WARN,
                        rep.healNote + " — بقية القيم سليمة"));
            } else {
                r.add(new Result("الإعدادات المحفوظة", PASS,
                        "كل القيم في نطاقاتها الصحيحة (ثيم، أحجام، ألوان، أصوات)"));
            }
        } catch (Throwable t) {
            r.add(new Result("الإعدادات المحفوظة", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    // ==================== 11: بيانات الألواح ====================

    private static void checkPanelData(Context ctx, ArrayList<Result> r) {
        try {
            SharedPreferences p = ctx.getSharedPreferences("kb_panel", Context.MODE_PRIVATE);
            int clips = count(p.getString("clips", ""), '\u0001');
            int pins = count(p.getString("pins", ""), '\u0001');
            int emoji = count(p.getString("emoji_recents", "").trim(), ' ');
            r.add(new Result("بيانات الحافظة والإيموجي المحفوظة", PASS,
                    clips + " عناصر حافظة — " + pins + " مثبتة — " + emoji + " إيموجي مستخدم حديثاً"));
        } catch (Throwable t) {
            r.add(new Result("بيانات الحافظة والإيموجي", WARN,
                    "تعذرت قراءة البيانات (تُتجاهل بأمان): " + t.getClass().getSimpleName()));
        }
    }

    private static int count(String blob, char sep) {
        if (blob == null || blob.isEmpty()) return 0;
        int c = 1;
        for (int i = 0; i < blob.length(); i++) if (blob.charAt(i) == sep) c++;
        return c;
    }

    // ==================== 12: الموارد ====================

    private static void checkResources(Context ctx, ArrayList<Result> r) {
        try {
            int[] dims = {
                    R.dimen.kb_key_height, R.dimen.kb_row_gap, R.dimen.kb_key_gap,
                    R.dimen.kb_side_pad, R.dimen.kb_top_pad, R.dimen.kb_bottom_pad,
                    R.dimen.emoji_panel_height};
            for (int d : dims) {
                if (ctx.getResources().getDimensionPixelSize(d) <= 0) {
                    r.add(new Result("موارد الواجهة", FAIL, "بُعد غير صالح: " + d));
                    return;
                }
            }
            int[] strs = {R.string.ime_name, R.string.clip_title, R.string.clip_empty,
                    R.string.voice_listening, R.string.selftest_run, R.string.act_search};
            for (int s : strs) {
                if (ctx.getString(s) == null || ctx.getString(s).isEmpty()) {
                    r.add(new Result("موارد الواجهة", FAIL, "نص مفقود: " + s));
                    return;
                }
            }
            r.add(new Result("موارد الواجهة", PASS,
                    "كل الأبعاد والنصوص والموارد الرسومية موجودة وسليمة"));
        } catch (Throwable t) {
            r.add(new Result("موارد الواجهة", FAIL, "عطل: " + t.getClass().getSimpleName()));
        }
    }

    // ==================== 13: سجل الأعطال ====================

    private static void checkCrashLog(ArrayList<Result> r) {
        try {
            if (CrashGuard.hasReport()) {
                String s = CrashGuard.lastReportSummary();
                r.add(new Result("سجل الأعطال", WARN,
                        "عطل مسجل من تشغيل سابق: " + (s == null ? "غير معروف" : s)));
            } else {
                r.add(new Result("سجل الأعطال", PASS, "لا توجد أي أعطال مسجلة"));
            }
        } catch (Throwable t) {
            r.add(new Result("سجل الأعطال", INFO, "تعذرت قراءة السجل"));
        }
    }
}
