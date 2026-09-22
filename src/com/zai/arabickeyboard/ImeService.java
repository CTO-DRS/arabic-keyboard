package com.zai.arabickeyboard;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.inputmethodservice.InputMethodService;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * خدمة لوحة المفاتيح الذكية — DRS Smart v2.0
 * ست لغات، تنبؤ ذكي بالكلمات، تصحيح تلقائي، حافظة، لوحة تحرير، إيموجي مصنف.
 */
public class ImeService extends InputMethodService
        implements KeyboardView.Listener, KeyboardView.StripListener, KeyboardView.GlideListener {

    public static final int MODE_AR = 0;
    public static final int MODE_EN = 1;
    public static final int MODE_FR = 2;
    public static final int MODE_DE = 3;
    public static final int MODE_ES = 4;
    public static final int MODE_TR = 5;
    public static final int MODE_SYM1 = 6;
    public static final int MODE_SYM2 = 7;
    public static final int MODE_EMOJI = 8;
    public static final int MODE_EDIT = 9;
    public static final int MODE_NUMPAD = 10;

    private static final int PANEL_NONE = 0;
    private static final int PANEL_EMOJI = 1;
    private static final int PANEL_CLIP = 2;

    private int lang = Layouts.LANG_AR;
    private int mode = MODE_AR;
    private int panel = PANEL_NONE;
    private int emojiGroup = 1;
    private boolean shiftOn, shiftLock;
    private long lastSpaceAt;
    private boolean lastWasSpace;

    private LinearLayout container;
    private KeyboardView kv;
    private View emojiPanel;
    private View clipPanel;
    private GridLayout emojiGrid;
    private LinearLayout emojiTabs;
    private LinearLayout clipList;
    private Prefs prefs;
    private ThemeSet theme;
    private SuggestEngine engine;
    private SharedPreferences panelPrefs;

    private final List<String> clipItems = new ArrayList<>();
    private final List<String> pinItems = new ArrayList<>();
    private List<String> liveSugg;
    private String undoCorrected;   // آخر كلمة صححها المحرك تلقائياً
    private String undoOriginal;    // الكلمة الأصلية قبل التصحيح
    private String glideCommitted;  // كلمة أُدخلت بالسحب (للتبديل من الشريط)

    // الوضع العائم
    private PopupWindow floatPopup;
    private LinearLayout floatRoot;
    private View dockBar;
    private boolean floating;
    private int floatW, floatPosX, floatPosY;
    private int floatGripH;
    private float gripGrabDX, gripGrabDY;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashGuard.install(this); // نظام الحماية الذكي من الأعطال
        prefs = new Prefs(this);
        engine = new SuggestEngine(this);
        panelPrefs = getSharedPreferences("kb_panel", Context.MODE_PRIVATE);
        loadClips();
        loadPins();
        VoiceInputActivity.delegate = this;
    }

    /** غلاف تنفيذ آمن: أي استثناء يُسجّل في سجل الأعطال دون إسقاط الخدمة */
    private void safeRun(String op, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            CrashGuard.log(t);
        }
    }

    /** ضمان وجود ثيم صالح قبل أي استخدام — يمنع أي NPE في ترتيب البناء */
    private void ensureTheme() {
        if (theme == null) {
            theme = ThemeSet.resolve(prefs == null ? ThemeSet.DEFAULT_PRESET : prefs.themePreset,
                    systemNight(), prefs == null ? 0 : prefs.accent);
        }
    }

    @Override
    public void onDestroy() {
        if (VoiceInputActivity.delegate == this) VoiceInputActivity.delegate = null;
        exitFloating();
        super.onDestroy();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false; // منع وضع ملء الشاشة المزعج في الوضع الأفقي
    }

    private boolean systemNight() {
        int m = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isLetterMode() {
        return mode >= MODE_AR && mode <= MODE_TR;
    }

    // ==================== إنشاء الواجهة ====================

    @Override
    public View onCreateInputView() {
        // حماية كاملة: أي عطل غير متوقع في البناء لا يُغلق التطبيق —
        // تُستعاد اللوحة في وضع آمن مباشرة
        try {
            return buildInputView();
        } catch (Throwable t) {
            CrashGuard.log(t);
            return buildSafeRecoveryView();
        }
    }

    /** البناء الكامل للوحة — الثيم يُحل أولاً قبل أي مكوّن يستخدمه */
    private View buildInputView() {
        prefs.reload();
        // الإصلاح الجذري لعطل الإغلاق الفوري: الثيم يُحدد قبل بناء الألواح
        theme = ThemeSet.resolve(prefs.themePreset, systemNight(), prefs.accent);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        kv = new KeyboardView(this);
        kv.setListener(this);
        kv.setStripListener(this);
        kv.setGlideListener(this);
        kv.hapticsEnabled = prefs.haptics;
        kv.glideEnabled = prefs.glide && !prefs.incognito;
        kv.setLongPressDelay(prefs.longPressMs());
        kv.incognitoOn = prefs.incognito;
        kv.setKeyHeightScale(heightScale(prefs.keyHeight));
        kv.setOneHanded(prefs.oneHanded);
        container.addView(kv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        emojiPanel = buildEmojiPanel();
        emojiPanel.setVisibility(View.GONE);
        int panelH = getResources().getDimensionPixelSize(R.dimen.emoji_panel_height);
        container.addView(emojiPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        clipPanel = buildClipPanel();
        clipPanel.setVisibility(View.GONE);
        container.addView(clipPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        kv.setTheme(theme);
        applyPanelTheme();
        mode = lang;
        refreshLayout();
        return container;
    }

    /** وضع الاستعادة الآمن: لوحة مبسطة تعمل دائماً حتى لو فشل البناء الكامل */
    private View buildSafeRecoveryView() {
        try {
            container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            kv = new KeyboardView(this);
            kv.setListener(this);
            kv.setStripListener(this);
            kv.setGlideListener(this);
            kv.setTheme(ThemeSet.resolve(ThemeSet.DEFAULT_PRESET, systemNight(), 0));
            kv.setKeyboard(Layouts.arabic(false, false));
            container.addView(kv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView msg = new TextView(this);
            msg.setText("تم استعادة اللوحة في الوضع الآمن — أعد فتح الحقل لاستعادة كل الميزات");
            msg.setTextSize(13);
            msg.setTextColor(0xFFFFC93A);
            msg.setGravity(Gravity.CENTER);
            msg.setPadding(0, dp(6), 0, dp(6));
            container.addView(msg, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mode = MODE_AR;
            return container;
        } catch (Throwable t2) {
            // آخر ملجأ: رسالة نصية فقط — لا انهيار مهما حدث
            try {
                TextView tv = new TextView(this);
                tv.setText("تعذر عرض لوحة المفاتيح — أعد فتح الحقل");
                tv.setTextSize(15);
                tv.setTextColor(0xFFFFFFFF);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, dp(48), 0, dp(48));
                return tv;
            } catch (Throwable t3) {
                return new View(this);
            }
        }
    }

    // ==================== لوحة الإيموجي ====================

    private View buildEmojiPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        emojiTabs = new LinearLayout(this);
        emojiTabs.setHorizontalScrollBarEnabled(false);
        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        tabsScroll.addView(emojiTabs, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
        root.addView(tabsScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        emojiGrid = new GridLayout(this);
        emojiGrid.setColumnCount(6);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout fl = new FrameLayout(this);
        fl.addView(emojiGrid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL));
        scroll.addView(fl);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        buildTabs();
        fillEmojiGrid(emojiGroup);
        return root;
    }

    private void buildTabs() {
        ensureTheme(); // حماية: الثيم يجب أن يكون جاهزاً قبل أي تنسيق
        emojiTabs.removeAllViews();
        for (int i = 0; i < Layouts.EMOJI_GROUPS.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText(Layouts.EMOJI_GROUPS[i][0]);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER);
            tv.setClickable(true);
            // تنسيق مباشر حسب الثيم الحالي والتصنيف المختار
            if (idx == emojiGroup) {
                GradientDrawable sel = new GradientDrawable();
                sel.setCornerRadius(dp(10));
                sel.setColor(theme.keyBgAction);
                tv.setBackground(sel);
                tv.setTextColor(theme.keyTextAction);
                tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            } else {
                tv.setBackground(null);
                tv.setTextColor(theme.keyTextFunc);
                tv.setTypeface(Typeface.DEFAULT);
            }
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    emojiGroup = idx;
                    fillEmojiGrid(idx);
                    buildTabs();
                }
            });
            emojiTabs.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
        }
    }

    private void fillEmojiGrid(int group) {
        emojiGrid.removeAllViews();
        String[] list = (group == 0) ? emojiRecents() : Layouts.emojisOf(group);
        int cell = dp(46);
        for (final String e : list) {
            TextView tv = new TextView(this);
            tv.setText(e);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 25);
            int pad = dp(5);
            tv.setPadding(pad, pad, pad, pad);
            tv.setGravity(Gravity.CENTER);
            tv.setClickable(true);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (kv.hapticsEnabled) kv.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    addEmojiRecent(e);
                    onText(e);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            emojiGrid.addView(tv, lp);
        }
        TextView empty = null;
        if (list.length == 0) {
            empty = new TextView(this);
            empty.setText(R.string.emoji_no_recent);
            empty.setTextSize(14);
        }
        if (empty != null) {
            emojiGrid.addView(empty, new GridLayout.LayoutParams());
        }
    }

    private String[] emojiRecents() {
        String blob = panelPrefs.getString("emoji_recents", "");
        if (blob.isEmpty()) return new String[0];
        return blob.split(" ");
    }

    private void addEmojiRecent(String e) {
        if (prefs.incognito) return; // التخفي: بلا سجل إيموجي
        List<String> items = new ArrayList<>();
        items.add(e);
        for (String s : emojiRecents()) if (!s.equals(e) && !s.isEmpty()) items.add(s);
        while (items.size() > 24) items.remove(items.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(s);
        }
        panelPrefs.edit().putString("emoji_recents", sb.toString()).apply();
        if (emojiGroup == 0 && emojiGrid != null) fillEmojiGrid(0);
    }

    // ==================== لوحة الحافظة ====================

    private View buildClipPanel() {
        ensureTheme();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(14), 0);

        TextView title = new TextView(this);
        title.setText(R.string.clip_title);
        title.setTextSize(15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = new TextView(this);
        clear.setText(R.string.clip_clear);
        clear.setTextSize(13);
        clear.setPadding(dp(10), dp(6), 0, dp(6));
        clear.setClickable(true);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // مسح السجل فقط — العناصر المثبتة تبقى
                clipItems.clear();
                saveClips();
                rebuildClipList();
            }
        });
        header.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        View divider = new View(this);
        root.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        clipList = new LinearLayout(this);
        clipList.setOrientation(LinearLayout.VERTICAL);
        clipList.setPadding(dp(8), dp(6), dp(8), dp(10));
        scroll.addView(clipList);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        rebuildClipList();
        return root;
    }

    private void rebuildClipList() {
        if (clipList == null) return;
        clipList.removeAllViews();
        if (clipItems.isEmpty() && pinItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.clip_empty);
            empty.setTextSize(14);
            empty.setPadding(dp(8), dp(14), dp(8), dp(14));
            clipList.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        // العناصر المثبتة أولاً (📌 ثم نقرة للصق، ضغط مطول لإلغاء التثبيت)
        for (int i = 0; i < pinItems.size(); i++) {
            clipList.addView(clipRow(pinItems.get(i), true));
        }
        // سجل الحافظة العادي (ضغط مطول للتثبيت)
        for (int i = 0; i < clipItems.size(); i++) {
            clipList.addView(clipRow(clipItems.get(i), false));
        }
    }

    /** صف عنصر حافظة: نقرة = لصق، ضغط مطول = تثبيت/إلغاء، ✕ = حذف */
    private View clipRow(final String item, final boolean pinned) {
        LinearLayout rowBg = new LinearLayout(this);
        rowBg.setOrientation(LinearLayout.HORIZONTAL);
        rowBg.setGravity(Gravity.CENTER_VERTICAL);
        rowBg.setPadding(dp(12), dp(8), dp(8), dp(8));
        GradientDrawable rowDrawable = new GradientDrawable();
        rowDrawable.setCornerRadius(dp(8));
        rowDrawable.setColor(theme.keyBg);
        if (pinned) rowDrawable.setStroke(dp(1), theme.keyBgAction);
        rowBg.setBackground(rowDrawable);

        if (pinned) {
            Icon pin = new Icon(this, Icon.PIN, theme.keyBgAction);
            LinearLayout.LayoutParams pinLp = new LinearLayout.LayoutParams(dp(13), dp(13));
            pinLp.setMargins(0, 0, dp(7), 0);
            rowBg.addView(pin, pinLp);
        }
        TextView text = new TextView(this);
        text.setText(item);
        text.setTextSize(14);
        text.setMaxLines(2);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setClickable(true);
        text.setLongClickable(true);
        text.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(item, 1);
                closePanel();
            }
        });
        text.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (pinned) {
                    pinItems.remove(item);
                    savePins();
                } else {
                    clipItems.remove(item);
                    if (pinItems.size() >= 5) pinItems.remove(pinItems.size() - 1);
                    pinItems.add(0, item);
                    saveClips();
                    savePins();
                }
                rebuildClipList();
                return true;
            }
        });
        rowBg.addView(text, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Icon del = new Icon(this, Icon.CROSS, theme.keyTextFunc);
        del.setPadding(dp(10), dp(4), dp(6), dp(4));
        del.setClickable(true);
        del.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pinned) pinItems.remove(item); else clipItems.remove(item);
                if (pinned) savePins(); else saveClips();
                rebuildClipList();
            }
        });
        rowBg.addView(del, new LinearLayout.LayoutParams(
                dp(24), dp(20)));

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(3), 0, dp(3));
        wrap.addView(rowBg, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private void loadClips() {
        clipItems.clear();
        String blob = panelPrefs.getString("clips", "");
        for (String s : blob.split("\u0001")) {
            if (!s.isEmpty()) clipItems.add(s);
        }
    }

    private void saveClips() {
        StringBuilder sb = new StringBuilder();
        for (String s : clipItems) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(s);
        }
        panelPrefs.edit().putString("clips", sb.toString()).apply();
    }

    private void loadPins() {
        pinItems.clear();
        String blob = panelPrefs.getString("pins", "");
        for (String s : blob.split("\u0001")) {
            if (!s.isEmpty()) pinItems.add(s);
        }
    }

    private void savePins() {
        StringBuilder sb = new StringBuilder();
        for (String s : pinItems) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(s);
        }
        panelPrefs.edit().putString("pins", sb.toString()).apply();
    }

    /** التقاط النص المنسوخ حديثاً عند فتح الحقل */
    private void captureClipboard() {
        if (prefs.incognito) return; // التخفي: لا التقاط حافظة
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return;
            ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) return;
            CharSequence cs = cd.getItemAt(0).coerceToText(this);
            if (cs == null) return;
            String text = cs.toString().trim();
            if (text.isEmpty() || text.length() > 2000) return;
            if (pinItems.contains(text)) return; // مثبّت أصلاً — لا داعي للتكرار في السجل
            if (!clipItems.isEmpty() && clipItems.get(0).equals(text)) return;
            clipItems.remove(text);
            clipItems.add(0, text);
            while (clipItems.size() > 10) clipItems.remove(clipItems.size() - 1);
            saveClips();
            rebuildClipList();
        } catch (Exception ignored) {}
    }

    // ==================== دورة الحياة ====================

    @Override
    public void onStartInputView(final EditorInfo info, final boolean restarting) {
        super.onStartInputView(info, restarting);
        safeRun("onStartInputView", new Runnable() {
            @Override public void run() { afterStart(info); }
        });
    }

    private void afterStart(EditorInfo info) {
        prefs.reload();
        theme = ThemeSet.resolve(prefs.themePreset, systemNight(), prefs.accent);
        if (kv != null) {
            kv.hapticsEnabled = prefs.haptics;
            kv.setTheme(theme);
            kv.setKeyHeightScale(heightScale(prefs.keyHeight));
            kv.setOneHanded(prefs.oneHanded);
            kv.glideEnabled = prefs.glide && !prefs.incognito;
            kv.incognitoOn = prefs.incognito;
            kv.setLongPressDelay(prefs.longPressMs());
        }
        applyPanelTheme();
        panel = PANEL_NONE;
        mode = lang;
        shiftOn = false;
        shiftLock = false;
        lastWasSpace = false;
        undoCorrected = null;
        undoOriginal = null;
        glideCommitted = null;
        refreshLayout();
        updateEnterLabel();
        maybeAutoCapAtStart(info);
        captureClipboard();
        updateSuggestions();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (kv != null) kv.cancelAll();
        exitFloating();
        super.onFinishInputView(finishingInput);
    }

    private void refreshLayout() {
        if (kv == null) return;
        boolean panelOpen = panel != PANEL_NONE;
        if (panelOpen) {
            kv.setKeyboard(Layouts.emojiNav(lang));
        } else {
            kv.setKeyboard(Layouts.get(mode, lang, prefs.numRow, prefs.voice, prefs.arabicDigits));
        }
        kv.setShifted(isShifted());
        kv.setShiftLock(shiftLock);
        kv.setStripVisible(prefs.suggest && isLetterMode() && panel == PANEL_NONE);
        if (emojiPanel != null) {
            emojiPanel.setVisibility(panel == PANEL_EMOJI ? View.VISIBLE : View.GONE);
        }
        if (clipPanel != null) {
            clipPanel.setVisibility(panel == PANEL_CLIP ? View.VISIBLE : View.GONE);
        }
        // إعادة قياس النافذة العائمة بعد أي تغيير في اللوحة
        if (floating && floatPopup != null && floatPopup.isShowing()) {
            try {
                kv.measure(View.MeasureSpec.makeMeasureSpec(floatW, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                floatPopup.update(-1, -1, floatW, kv.getMeasuredHeight() + floatGripH);
            } catch (Exception ignored) {}
        }
    }

    private void applyPanelTheme() {
        ensureTheme();
        if (clipList != null) rebuildClipList(); // يعيد البناء بخلفيات الثيم الجديد
        if (emojiPanel != null) emojiPanel.setBackgroundColor(theme.kbBg);
        buildTabs(); // يعيد تنسيق التبويبات بألوان الثيم
        if (emojiGrid != null) emojiGrid.setBackgroundColor(Color.TRANSPARENT);
    }

    private void closePanel() {
        panel = PANEL_NONE;
        refreshLayout();
        updateEnterLabel();
    }

    private void togglePanel(int which) {
        panel = (panel == which) ? PANEL_NONE : which;
        if (panel == PANEL_EMOJI) fillEmojiGrid(emojiGroup);
        if (panel == PANEL_CLIP) rebuildClipList();
        refreshLayout();
        updateEnterLabel();
    }

    private boolean isShifted() {
        return (shiftOn || shiftLock) && isLetterMode();
    }

    private void updateEnterLabel() {
        String label = null;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && panel == PANEL_NONE && mode != MODE_EDIT) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            boolean noEnterAction = (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
            if (!noEnterAction) {
                switch (action) {
                    case EditorInfo.IME_ACTION_SEARCH: label = getString(R.string.act_search); break;
                    case EditorInfo.IME_ACTION_SEND: label = getString(R.string.act_send); break;
                    case EditorInfo.IME_ACTION_GO: label = getString(R.string.act_go); break;
                    case EditorInfo.IME_ACTION_NEXT: label = getString(R.string.act_next); break;
                    case EditorInfo.IME_ACTION_DONE: label = getString(R.string.act_done); break;
                }
            }
        }
        if (kv != null) kv.setEnterLabel(label);
    }

    private float heightScale(int kh) {
        if (kh == 0) return 0.88f;
        if (kh == 2) return 1.12f;
        return 1f;
    }

    // ==================== التنبؤ الذكي ====================

    private int engineLang() {
        return lang == Layouts.LANG_EN ? 1 : 0;
    }

    /** استخراج الكلمة الحالية قبل المؤشر */
    private String currentWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence before = ic.getTextBeforeCursor(48, 0);
        if (before == null || before.length() == 0) return "";
        int i = before.length() - 1;
        while (i >= 0) {
            char ch = before.charAt(i);
            boolean letter = isWordChar(ch);
            if (!letter) break;
            i--;
        }
        String word = before.subSequence(i + 1, before.length()).toString();
        return word;
    }

    private boolean isWordChar(char ch) {
        if (lang == Layouts.LANG_AR) {
            return (ch >= 0x0621 && ch <= 0x064A) || ch == 0x0640;
        }
        return Character.isLetter(ch);
    }

    /** الكلمة الواقعة قبل الكلمة الحالية مباشرة (للتنبؤ بالكلمة التالية والثنائيات) */
    private String wordBefore(String current) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return null;
        CharSequence before = ic.getTextBeforeCursor(96, 0);
        if (before == null) return null;
        int end = before.length() - (current == null ? 0 : current.length());
        while (end > 0 && Character.isWhitespace(before.charAt(end - 1))) end--;
        int start = end;
        while (start > 0 && isWordChar(before.charAt(start - 1))) start--;
        if (start >= end) return null;
        return before.subSequence(start, end).toString();
    }

    private void updateSuggestions() {
        if (kv == null) return;
        if (!prefs.suggest || !isLetterMode() || panel != PANEL_NONE) {
            liveSugg = null;
            kv.setSuggestions(null);
            return;
        }
        String word = currentWord();
        List<String> s;
        if (word.isEmpty()) {
            // لا كلمة تحت الكتابة: تنبؤ بالكلمة التالية من الكلمة السابقة
            String prev = prefs.nextWord ? wordBefore("") : null;
            s = (prev != null) ? engine.nextWords(prev, engineLang()) : null;
            if (s != null && s.isEmpty()) s = null;
        } else {
            s = engine.suggest(word, engineLang());
        }
        liveSugg = s;
        kv.setSuggestions(s);
    }

    @Override
    public void onStripAction(final int action, final int index) {
        safeRun("onStripAction", new Runnable() {
            @Override public void run() { handleStripAction(action, index); }
        });
    }

    private void handleStripAction(int action, int index) {
        if (action == KeyboardView.STRIP_CLIP) {
            togglePanel(PANEL_CLIP);
            return;
        }
        if (action == KeyboardView.STRIP_EDIT) {
            mode = MODE_EDIT;
            shiftOn = false; shiftLock = false;
            closePanel();
            refreshLayout();
            updateEnterLabel();
            return;
        }
        if (action == KeyboardView.STRIP_SHIELD) {
            toggleIncognito();
            return;
        }
        if (action == KeyboardView.STRIP_WORD) {
            applySuggestion(index);
        }
    }

    /** تبديل الوضع التخفي: بلا تعلّم وبلا التقاط حافظة وبلا سجل إيموجي */
    private void toggleIncognito() {
        prefs.setIncognito(!prefs.incognito);
        if (kv != null) {
            kv.incognitoOn = prefs.incognito;
            kv.glideEnabled = prefs.glide && !prefs.incognito;
            kv.invalidate();
        }
        Toast.makeText(this, prefs.incognito
                        ? R.string.incognito_on : R.string.incognito_off,
                Toast.LENGTH_SHORT).show();
    }

    private void applySuggestion(int index) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || liveSugg == null || index >= liveSugg.size()) return;
        String picked = liveSugg.get(index);
        String word = currentWord();
        String prevWord = wordBefore(word);
        // بعد إدخال كلمة بالسحب: النقر على بديل يستبدلها مباشرة
        if (word.isEmpty() && glideCommitted != null
                && picked.equals(glideCommitted)) {
            glideCommitted = null;
            return;
        }
        if (word.isEmpty() && glideCommitted != null) {
            CharSequence before = ic.getTextBeforeCursor(glideCommitted.length() + 1, 0);
            if (before != null && before.length() == glideCommitted.length() + 1) {
                ic.deleteSurroundingText(glideCommitted.length() + 1, 0);
                ic.commitText(picked + " ", 1);
                if (!prefs.incognito) engine.learn(picked, engineLang());
                if (prevWord != null && !prefs.incognito) engine.learnBigram(prevWord, picked, engineLang());
                glideCommitted = picked;
                updateSuggestions();
                return;
            }
            glideCommitted = null;
        }
        if (!word.isEmpty()) ic.deleteSurroundingText(word.length(), 0);
        ic.commitText(picked + " ", 1);
        if (!prefs.incognito) engine.learn(picked, engineLang());
        if (prevWord != null && !prefs.incognito) engine.learnBigram(prevWord, picked, engineLang());
        lastWasSpace = true;
        lastSpaceAt = System.currentTimeMillis();
        consumeShift();
        updateSuggestions();
    }

    // ==================== الكتابة بالسحب (Glide) ====================

    @Override
    public void onGlide(final List<float[]> tracePoints, final String letters) {
        safeRun("onGlide", new Runnable() {
            @Override public void run() { handleGlide(tracePoints, letters); }
        });
    }

    private void handleGlide(List<float[]> tracePoints, String letters) {
        if (!isLetterMode()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || tracePoints == null || tracePoints.size() < 2) return;

        // مرشّحات المحرك حسب تسلسل الحروف المرصود
        List<Map.Entry<String, Integer>> cands = engine.glideCandidates(letters, engineLang());
        if (cands.isEmpty()) {
            // لا مطابقة: إدخال الحروف المرصودة كما هي (كأنها كُتبت واحداً تلو الآخر)
            ic.commitText(letters, 1);
            lastWasSpace = false;
            updateSuggestions();
            return;
        }

        // مطابقة هندسية: مقارنة مسار الإصبع بالمسار المثالي لكل كلمة
        Map<String, float[]> centers = kv.charCenters();
        float keyH = kv.getKeyHeightPx();
        final int K = 28;
        float[][] userPath = resample(tracePoints, K);

        String best = null;
        float bestScore = Float.MAX_VALUE;
        List<String> ranked = new ArrayList<>();
        List<Map.Entry<String, Float>> scored = new ArrayList<>();
        for (Map.Entry<String, Integer> c : cands) {
            List<float[]> ideal = idealPath(c.getKey(), centers, engineLang());
            if (ideal == null) continue;
            float[][] wPath = resample(ideal, K);
            float d = 0;
            for (int i = 0; i < K; i++) {
                float dx = userPath[i][0] - wPath[i][0];
                float dy = userPath[i][1] - wPath[i][1];
                d += (float) Math.sqrt(dx * dx + dy * dy);
            }
            d /= K;
            if (d < keyH * 0.62f) { // حد القبول الهندسي
                scored.add(new HashMap.SimpleEntry<>(c.getKey(), d));
                if (d < bestScore) { bestScore = d; best = c.getKey(); }
            }
        }
        scored.sort((a, b) -> Float.compare(a.getValue(), b.getValue()));
        for (int i = 0; i < scored.size() && i < 3; i++) ranked.add(scored.get(i).getKey());

        if (best != null && !best.isEmpty()) {
            ic.commitText(best + " ", 1);
            if (!prefs.incognito) {
                String prevWord = wordBefore(best);
                engine.learn(best, engineLang());
                if (prevWord != null) engine.learnBigram(prevWord, best, engineLang());
            }
            glideCommitted = best;
            lastWasSpace = true;
            lastSpaceAt = System.currentTimeMillis();
            consumeShift();
            // البدائل في الشريط للتصحيح بنقرة
            List<String> alts = new ArrayList<>();
            for (String r : ranked) if (!r.equals(best)) alts.add(r);
            liveSugg = alts.isEmpty() ? null : alts;
            kv.setSuggestions(liveSugg);
        } else {
            ic.commitText(letters, 1);
            lastWasSpace = false;
            glideCommitted = null;
            updateSuggestions();
        }
    }

    /** إعادة معايرة نقاط المسار بالتوزيع المكاني إلى K نقطة ثابتة */
    private static float[][] resample(List<float[]> pts, int k) {
        float[][] out = new float[k][2];
        float total = 0;
        for (int i = 1; i < pts.size(); i++) {
            float dx = pts.get(i)[0] - pts.get(i - 1)[0];
            float dy = pts.get(i)[1] - pts.get(i - 1)[1];
            total += (float) Math.sqrt(dx * dx + dy * dy);
        }
        if (total <= 0) {
            for (int i = 0; i < k; i++) {
                out[i][0] = pts.get(0)[0];
                out[i][1] = pts.get(0)[1];
            }
            return out;
        }
        float step = total / (k - 1);
        float acc = 0;
        int seg = 1;
        out[0][0] = pts.get(0)[0];
        out[0][1] = pts.get(0)[1];
        for (int i = 1; i < k; i++) {
            float target = step * i;
            while (seg < pts.size()) {
                float dx = pts.get(seg)[0] - pts.get(seg - 1)[0];
                float dy = pts.get(seg)[1] - pts.get(seg - 1)[1];
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (acc + len >= target || seg == pts.size() - 1) {
                    float t = (len <= 0) ? 0 : (target - acc) / len;
                    if (t > 1) t = 1;
                    out[i][0] = pts.get(seg - 1)[0] + dx * t;
                    out[i][1] = pts.get(seg - 1)[1] + dy * t;
                    break;
                }
                acc += len;
                seg++;
            }
        }
        return out;
    }

    /** المسار المثالي لكلمة: مراكز مفاتيح حروفها (null إن نقص حرف من اللوحة) */
    private List<float[]> idealPath(String word, Map<String, float[]> centers, int lang) {
        List<float[]> pts = new ArrayList<>();
        String w = (lang == 1) ? word.toLowerCase(java.util.Locale.ENGLISH)
                : SuggestEngine.normAr(word);
        for (int i = 0; i < w.length(); i++) {
            float[] c = centers.get(String.valueOf(w.charAt(i)));
            if (c == null) return null;
            pts.add(new float[]{c[0], c[1]});
        }
        return pts;
    }

    // ==================== التعامل مع المفاتيح ====================

    @Override
    public void onKey(final Key k) {
        safeRun("onKey", new Runnable() {
            @Override public void run() { handleKey(k); }
        });
    }

    private void handleKey(Key k) {
        playSound();
        InputConnection ic = getCurrentInputConnection();
        switch (k.code) {
            case Key.CODE_SHIFT:
                if (shiftLock) { shiftLock = false; shiftOn = false; }
                else if (shiftOn) { shiftLock = true; shiftOn = true; }
                else shiftOn = true;
                kv.setShifted(isShifted());
                kv.setShiftLock(shiftLock);
                return;
            case Key.CODE_LANG:
                cycleLanguage();
                return;
            case Key.CODE_MODE_NUM:
                mode = MODE_SYM1; shiftOn = false; shiftLock = false;
                refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_PAGE2:
                mode = MODE_SYM2; refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_PAGE1:
                mode = MODE_SYM1; refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_BACK_TO_ABC:
                if (panel != PANEL_NONE) { closePanel(); return; }
                mode = lang; shiftOn = false; shiftLock = false;
                refreshLayout(); updateEnterLabel(); updateSuggestions();
                return;
            case Key.CODE_EMOJI:
                togglePanel(PANEL_EMOJI);
                return;
            case Key.CODE_EDIT:
                mode = MODE_EDIT; shiftOn = false; shiftLock = false;
                closePanel();
                refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_NUMPAD:
                mode = MODE_NUMPAD; shiftOn = false; shiftLock = false;
                closePanel();
                refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_FLOAT:
                if (floating) exitFloating(); else enterFloating();
                return;
            case Key.CODE_VOICE:
                handleVoice();
                return;
            case Key.CODE_BACKSPACE:
                handleBackspace(ic);
                return;
            case Key.CODE_ENTER:
                handleEnter(ic);
                return;
            case Key.CODE_SPACE: {
                handleSpace();
                return;
            }
            case Key.CODE_SEL_ALL: doSelectAll(); return;
            case Key.CODE_COPY: doContextAction(android.R.id.copy); return;
            case Key.CODE_CUT: doContextAction(android.R.id.cut); return;
            case Key.CODE_PASTE: doPaste(); return;
            case Key.CODE_HOME:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_HOME); return;
            case Key.CODE_END:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_END); return;
            case Key.CODE_DEL_WORD: deleteWord(ic); return;
            case Key.CODE_ARR_L:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_LEFT); return;
            case Key.CODE_ARR_R:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_RIGHT); return;
            case Key.CODE_ARR_U:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_UP); return;
            case Key.CODE_ARR_D:
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_DOWN); return;
            default: {
                String t = currentText(k);
                lastWasSpace = false;
                undoCorrected = null; undoOriginal = null;
                if (t == null || t.length() == 0) return;
                if (ic != null) ic.commitText(t, 1);
                consumeShift();
                updateSuggestions();
            }
        }
    }

    private void cycleLanguage() {
        lang = (lang + 1) % Layouts.LANG_COUNT;
        mode = lang;
        shiftOn = false; shiftLock = false;
        refreshLayout();
        updateEnterLabel();
        updateSuggestions();
    }

    private void handleSpace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = System.currentTimeMillis();
        undoCorrected = null; undoOriginal = null;
        glideCommitted = null;

        String word = currentWord();
        String prevWord = wordBefore(word);

        // توسيع الاختصارات النصية (أولوية قصوى)
        if (isLetterMode() && !word.isEmpty()) {
            String exp = engine.shortcutExpansion(word, engineLang());
            if (exp != null) {
                ic.deleteSurroundingText(word.length(), 0);
                ic.commitText(exp + " ", 1);
                if (!prefs.incognito) {
                    engine.learn(word, engineLang());
                    if (prevWord != null) engine.learnBigram(prevWord, word, engineLang());
                }
                lastWasSpace = true;
                lastSpaceAt = now;
                updateSuggestions();
                return;
            }
        }

        // التصحيح التلقائي عند الضغط على المسافة (إنجليزي مباشرة + عربي بعد التطبيع)
        if (isLetterMode() && prefs.autoCorrect && word.length() >= 4
                && !engine.known(word, engineLang())) {
            String fix = engine.bestCorrection(word, engineLang());
            if (fix != null && !fix.equals(word)) {
                ic.deleteSurroundingText(word.length(), 0);
                ic.commitText(fix + " ", 1);
                if (!prefs.incognito) {
                    engine.learn(fix, engineLang());
                    if (prevWord != null) engine.learnBigram(prevWord, fix, engineLang());
                }
                // تسجيل التراجع: الحذف بعد التصحيح يعيد الكلمة الأصلية
                undoCorrected = fix;
                undoOriginal = word;
                lastWasSpace = true;
                lastSpaceAt = now;
                updateSuggestions();
                return;
            }
        }
        // تعلم الكلمة المكتوبة + ثنائيتها
        if (isLetterMode() && word.length() >= 2 && engine.known(word, engineLang())
                && !prefs.incognito) {
            engine.learn(word, engineLang());
            if (prevWord != null) engine.learnBigram(prevWord, word, engineLang());
        }

        // نقطة تلقائية بضغطتين متتاليتين على المسافة
        if (prefs.doubleSpace && lastWasSpace && (now - lastSpaceAt) < 400) {
            CharSequence before = ic.getTextBeforeCursor(1, 0);
            if (before != null && before.length() == 1 && before.charAt(0) == ' ') {
                ic.deleteSurroundingText(1, 0);
                ic.commitText(". ", 1);
                lastWasSpace = false;
                autoCapAfterSentence();
                updateSuggestions();
                return;
            }
        }
        ic.commitText(" ", 1);
        lastWasSpace = true;
        lastSpaceAt = now;
        updateSuggestions();
    }

    @Override
    public void onText(final String t) {
        safeRun("onText", new Runnable() {
            @Override public void run() { handleText(t); }
        });
    }

    private void handleText(String t) {
        playSound();
        undoCorrected = null; undoOriginal = null;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && t != null && t.length() > 0) ic.commitText(t, 1);
        lastWasSpace = false;
        consumeShift();
        updateSuggestions();
    }

    @Override
    public void onCursorMove(int deltaSteps) {
        int code = deltaSteps > 0
                ? android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                : android.view.KeyEvent.KEYCODE_DPAD_LEFT;
        for (int i = 0; i < Math.abs(deltaSteps); i++) {
            sendDownUpKeyEvents(code);
        }
    }

    // ==================== أوامر التحرير ====================

    private void doSelectAll() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (ic.performContextMenuAction(android.R.id.selectAll)) return;
        try {
            android.view.inputmethod.ExtractedTextRequest req =
                    new android.view.inputmethod.ExtractedTextRequest();
            android.view.inputmethod.ExtractedText et = ic.getExtractedText(req, 0);
            if (et != null && et.text != null) ic.setSelection(0, et.text.length());
        } catch (Exception ignored) {}
    }

    private void doContextAction(int id) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.performContextMenuAction(id);
    }

    private void doPaste() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (ic.performContextMenuAction(android.R.id.paste)) return;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (cs != null) ic.commitText(cs.toString(), 1);
            }
        } catch (Exception ignored) {}
    }

    private void deleteWord(InputConnection ic) {
        if (ic == null) return;
        String word = currentWord();
        ic.deleteSurroundingText(word.length(), 0);
        updateSuggestions();
    }

    /** تفعيل Shift تلقائياً لما بعد نهاية الجملة */
    private void autoCapAfterSentence() {
        if (prefs.autoCap && isLetterMode() && lang != Layouts.LANG_AR) {
            shiftOn = true;
            if (kv != null) kv.setShifted(true);
        }
    }

    /** Shift تلقائي عند بداية الحقل النصي */
    private void maybeAutoCapAtStart(EditorInfo info) {
        if (!prefs.autoCap || lang == Layouts.LANG_AR || kv == null || info == null) return;
        if ((info.inputType & android.text.InputType.TYPE_CLASS_TEXT) == 0) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before == null || before.length() == 0) {
            shiftOn = true;
            kv.setShifted(true);
        }
    }

    @Override
    public void onKeyLongPress(final Key k) {
        safeRun("onKeyLongPress", new Runnable() {
            @Override public void run() { handleKeyLongPress(k); }
        });
    }

    private void handleKeyLongPress(Key k) {
        if (k.code == Key.CODE_SHIFT) {
            shiftLock = true; shiftOn = true;
            kv.setShifted(true);
            kv.setShiftLock(true);
        } else if (k.code == Key.CODE_LANG) {
            showLanguagePicker();
        } else if (k.code == Key.CODE_MODE_NUM) {
            // ضغط مطول على ؟١٢٣ يفتح لوحة الأرقام الكاملة (لوحة أرقام الهاتف)
            mode = MODE_NUMPAD; shiftOn = false; shiftLock = false;
            refreshLayout(); updateEnterLabel();
        }
    }

    /** نافذة اختيار اللغة */
    private void showLanguagePicker() {
        try {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.pick_language)
                    .setItems(Layouts.LANG_NAMES, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int which) {
                            lang = which;
                            mode = lang;
                            shiftOn = false; shiftLock = false;
                            refreshLayout();
                            updateEnterLabel();
                            updateSuggestions();
                        }
                    })
                    .show();
        } catch (Exception ignored) {}
    }

    private String currentText(Key k) {
        if (isShifted() && k.shiftText != null) return k.shiftText;
        return k.text;
    }

    private void consumeShift() {
        if (shiftOn && !shiftLock) {
            shiftOn = false;
            if (kv != null) kv.setShifted(false);
        }
    }

    private void handleBackspace(InputConnection ic) {
        if (ic == null) return;

        // تراجع عن التصحيح التلقائي: حذف فوراً بعد التصحيح يعيد الكلمة الأصلية
        if (undoCorrected != null) {
            int total = undoCorrected.length() + 1; // كلمة + مسافة
            CharSequence before = ic.getTextBeforeCursor(total, 0);
            if (before != null && before.length() == total && before.charAt(0) == ' '
                    && before.toString().substring(1).equals(undoCorrected)) {
                ic.deleteSurroundingText(total, 0);
                ic.commitText(undoOriginal, 1);
                undoCorrected = null; undoOriginal = null;
                updateSuggestions();
                return;
            }
            undoCorrected = null; undoOriginal = null;
        }

        // دعم حذف الإيموجي (أزواج البدائل) بشكل صحيح
        CharSequence before = ic.getTextBeforeCursor(2, 0);
        int n = 1;
        if (before != null && before.length() == 2
                && Character.isHighSurrogate(before.charAt(0))
                && Character.isLowSurrogate(before.charAt(1))) {
            n = 2;
        }
        ic.deleteSurroundingText(n, 0);
        updateSuggestions();
    }

    private void handleEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        boolean done = false;
        if (ei != null) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            boolean noEnterAction = (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
            if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE) {
                if (getCurrentInputConnection() != null) {
                    getCurrentInputConnection().performEditorAction(action);
                    done = true;
                }
            }
        }
        if (!done) sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_ENTER);
        lastWasSpace = false;
        undoCorrected = null; undoOriginal = null;
        autoCapAfterSentence();
        updateSuggestions();
    }

    /** إدخال نتيجة التعرف الصوتي في المؤشر (تستدعيه VoiceInputActivity — نفس العملية والخيط) */
    public void commitVoiceResult(String text) {
        if (text == null || text.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            lastWasSpace = false;
            updateSuggestions();
        }
    }

    /** فتح نافذة الإدخال الصوتي فوق الحقل */
    private void handleVoice() {
        try {
            Intent intent = new Intent(this, VoiceInputActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void playSound() {
        if (!prefs.sound) return;
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null || am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return;
        int effect;
        switch (prefs.soundStyle) {
            case 1: effect = AudioManager.FX_KEYPRESS_STANDARD; break;
            case 2: effect = AudioManager.FX_KEYPRESS_SPACEBAR; break;
            default: effect = AudioManager.FX_KEY_CLICK; break;
        }
        am.playSoundEffect(effect);
    }

    // ==================== الوضع العائم ====================

    /** تحويل اللوحة إلى نافذة عائمة قابلة للسحب في أي مكان بالشاشة */
    private void enterFloating() {
        if (floating || kv == null || container == null) return;
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int sw = dm.widthPixels;
            int sh = dm.heightPixels;
            floatW = Math.min((int) (sw * 0.72f), dp(430));
            int gripH = dp(30);
            floatGripH = gripH;

            // شريط القبض: سحب لتحريك اللوحة + زر العودة للرسو
            LinearLayout grip = new LinearLayout(this);
            grip.setOrientation(LinearLayout.HORIZONTAL);
            grip.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable gripBg = new GradientDrawable();
            gripBg.setColor(theme != null ? theme.keyBgFunc : 0xFF141A36);
            grip.setBackground(gripBg);
            grip.setPadding(dp(14), 0, dp(10), 0);

            TextView handle = new TextView(this);
            handle.setText("☰");
            handle.setTextSize(15);
            handle.setTextColor(theme != null ? theme.keyTextFunc : 0xFFB9C2F0);
            grip.addView(handle, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView dockBtn = new TextView(this);
            dockBtn.setText(R.string.float_dock);
            dockBtn.setTextSize(13);
            dockBtn.setTextColor(theme != null ? theme.stripWord : 0xFFB388FF);
            dockBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
            dockBtn.setClickable(true);
            dockBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { exitFloating(); }
            });
            LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dbLp.leftMargin = dp(8);
            grip.addView(dockBtn, dbLp);

            TextView title = new TextView(this);
            title.setText(R.string.float_title);
            title.setTextSize(12);
            title.setTextColor(theme != null ? theme.hintText : 0xFF8A93C4);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tLp.leftMargin = dp(10);
            grip.addView(title, tLp);

            // نقل لوحة المفاتيح إلى جذر النافذة العائمة
            floatRoot = new LinearLayout(this);
            floatRoot.setOrientation(LinearLayout.VERTICAL);
            container.removeView(kv);
            floatRoot.addView(grip, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, gripH));
            floatRoot.addView(kv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // شريط الرسو في منطقة الإدخال الصغيرة
            dockBar = new TextView(this);
            ((TextView) dockBar).setText(R.string.float_return);
            ((TextView) dockBar).setTextSize(13);
            ((TextView) dockBar).setGravity(Gravity.CENTER);
            ((TextView) dockBar).setTextColor(theme != null ? theme.stripWord : 0xFFB388FF);
            GradientDrawable dbBg = new GradientDrawable();
            dbBg.setColor(theme != null ? theme.kbBg : 0xFF0D1228);
            dockBar.setBackground(dbBg);
            dockBar.setClickable(true);
            dockBar.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { exitFloating(); }
            });
            container.addView(dockBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

            // إظهار النافذة العائمة
            floatPosX = sw - floatW - dp(10);
            floatPosY = (int) (sh * 0.22f);
            floatPopup = new PopupWindow(floatRoot, floatW,
                    ViewGroup.LayoutParams.WRAP_CONTENT, false);
            floatPopup.showAtLocation(container, Gravity.TOP | Gravity.START,
                    floatPosX, floatPosY);

            // السحب: قبض على شريط القبض وتحريك النافذة
            grip.setOnTouchListener(new View.OnTouchListener() {
                float downRawX, downRawY;
                @Override public boolean onTouch(View v, MotionEvent ev) {
                    switch (ev.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downRawX = ev.getRawX();
                            downRawY = ev.getRawY();
                            gripGrabDX = downRawX - floatPosX;
                            gripGrabDY = downRawY - floatPosY;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float nx = ev.getRawX() - gripGrabDX;
                            float ny = ev.getRawY() - gripGrabDY;
                            floatPosX = (int) nx;
                            floatPosY = (int) ny;
                            if (floatPopup != null) {
                                try { floatPopup.update((int) nx, (int) ny, -1, -1); }
                                catch (Exception ignored) {}
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });

            floating = true;
            Toast.makeText(this, R.string.float_on, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // فشل النافذة العائمة: إعادة اللوحة لوضعها الراسخ
            exitFloating();
        }
    }

    /** إعادة اللوحة إلى وضعها الراسخ أسفل الشاشة */
    private void exitFloating() {
        if (!floating && floatPopup == null && floatRoot == null && dockBar == null) return;
        floating = false;
        if (floatPopup != null) {
            try { floatPopup.dismiss(); } catch (Exception ignored) {}
            floatPopup = null;
        }
        if (floatRoot != null && kv != null && kv.getParent() == floatRoot) {
            floatRoot.removeView(kv);
        }
        floatRoot = null;
        if (container != null && kv != null && kv.getParent() == null) {
            container.addView(kv, 0, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (container != null && dockBar != null && dockBar.getParent() == container) {
            container.removeView(dockBar);
        }
        dockBar = null;
        refreshLayout();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
