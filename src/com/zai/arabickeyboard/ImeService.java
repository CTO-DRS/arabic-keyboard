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
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * خدمة لوحة المفاتيح الذكية — DRS Smart v2.0
 * ست لغات، تنبؤ ذكي بالكلمات، تصحيح تلقائي، حافظة، لوحة تحرير، إيموجي مصنف.
 */
public class ImeService extends InputMethodService
        implements KeyboardView.Listener, KeyboardView.StripListener {

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

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        engine = new SuggestEngine(this);
        panelPrefs = getSharedPreferences("kb_panel", Context.MODE_PRIVATE);
        loadClips();
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
        prefs.reload();
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        kv = new KeyboardView(this);
        kv.setListener(this);
        kv.setStripListener(this);
        kv.hapticsEnabled = prefs.haptics;
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

        theme = ThemeSet.resolve(prefs.themePreset, systemNight());
        kv.setTheme(theme);
        applyPanelTheme();
        mode = lang;
        refreshLayout();
        return container;
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
        if (clipItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.clip_empty);
            empty.setTextSize(14);
            empty.setPadding(dp(8), dp(14), dp(8), dp(14));
            clipList.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        for (int i = 0; i < clipItems.size(); i++) {
            final int idx = i;
            final String item = clipItems.get(i);
            LinearLayout rowBg = new LinearLayout(this);
            rowBg.setOrientation(LinearLayout.HORIZONTAL);
            rowBg.setGravity(Gravity.CENTER_VERTICAL);
            rowBg.setPadding(dp(12), dp(8), dp(8), dp(8));
            GradientDrawable rowDrawable = new GradientDrawable();
            rowDrawable.setCornerRadius(dp(8));
            rowDrawable.setColor(theme.keyBg);
            rowBg.setBackground(rowDrawable);

            TextView text = new TextView(this);
            text.setText(item);
            text.setTextSize(14);
            text.setMaxLines(2);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setClickable(true);
            text.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) ic.commitText(item, 1);
                    closePanel();
                }
            });
            rowBg.addView(text, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = new TextView(this);
            del.setText("✕");
            del.setTextSize(14);
            del.setPadding(dp(10), dp(4), dp(6), dp(4));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (idx < clipItems.size()) {
                        clipItems.remove(idx);
                        saveClips();
                        rebuildClipList();
                    }
                }
            });
            rowBg.addView(del, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout wrap = new LinearLayout(this);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.setPadding(0, dp(3), 0, dp(3));
            wrap.addView(rowBg, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            clipList.addView(wrap, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
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

    /** التقاط النص المنسوخ حديثاً عند فتح الحقل */
    private void captureClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return;
            ClipData cd = cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) return;
            CharSequence cs = cd.getItemAt(0).coerceToText(this);
            if (cs == null) return;
            String text = cs.toString().trim();
            if (text.isEmpty() || text.length() > 2000) return;
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
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        prefs.reload();
        ThemeSet t = ThemeSet.resolve(prefs.themePreset, systemNight());
        if (kv != null) {
            kv.hapticsEnabled = prefs.haptics;
            kv.setTheme(t);
            kv.setKeyHeightScale(heightScale(prefs.keyHeight));
            kv.setOneHanded(prefs.oneHanded);
        }
        theme = t;
        applyPanelTheme();
        panel = PANEL_NONE;
        mode = lang;
        shiftOn = false;
        shiftLock = false;
        lastWasSpace = false;
        refreshLayout();
        updateEnterLabel();
        maybeAutoCapAtStart(info);
        captureClipboard();
        updateSuggestions();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (kv != null) kv.cancelAll();
        super.onFinishInputView(finishingInput);
    }

    private void refreshLayout() {
        if (kv == null) return;
        boolean panelOpen = panel != PANEL_NONE;
        if (panelOpen) {
            kv.setKeyboard(Layouts.emojiNav(lang));
        } else {
            kv.setKeyboard(Layouts.get(mode, lang, prefs.numRow));
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
    }

    private void applyPanelTheme() {
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

    private void updateSuggestions() {
        if (kv == null) return;
        if (!prefs.suggest || !isLetterMode() || panel != PANEL_NONE) {
            kv.setSuggestions(null);
            return;
        }
        String word = currentWord();
        if (word.isEmpty()) {
            kv.setSuggestions(null);
        } else {
            kv.setSuggestions(engine.suggest(word, engineLang()));
        }
    }

    @Override
    public void onStripAction(int action, int index) {
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
        if (action == KeyboardView.STRIP_WORD) {
            applySuggestion(index);
        }
    }

    private void applySuggestion(int index) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String word = currentWord();
        List<String> sugg = engine.suggest(word, engineLang());
        if (index >= sugg.size()) return;
        String picked = sugg.get(index);
        if (!word.isEmpty()) ic.deleteSurroundingText(word.length(), 0);
        ic.commitText(picked + " ", 1);
        engine.learn(picked, engineLang());
        lastWasSpace = false;
        consumeShift();
        updateSuggestions();
    }

    // ==================== التعامل مع المفاتيح ====================

    @Override
    public void onKey(Key k) {
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

        // التصحيح التلقائي عند الضغط على المسافة
        String word = currentWord();
        if (isLetterMode() && prefs.autoCorrect && word.length() >= 4
                && !engine.known(word, engineLang())) {
            String fix = engine.bestCorrection(word, engineLang());
            if (fix != null && !fix.equals(word)) {
                ic.deleteSurroundingText(word.length(), 0);
                ic.commitText(fix + " ", 1);
                engine.learn(fix, engineLang());
                lastWasSpace = true;
                lastSpaceAt = now;
                updateSuggestions();
                return;
            }
        }
        // تعلم الكلمة المكتوبة
        if (isLetterMode() && word.length() >= 2 && engine.known(word, engineLang())) {
            engine.learn(word, engineLang());
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
    public void onText(String t) {
        playSound();
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
    public void onKeyLongPress(Key k) {
        if (k.code == Key.CODE_SHIFT) {
            shiftLock = true; shiftOn = true;
            kv.setShifted(true);
            kv.setShiftLock(true);
        } else if (k.code == Key.CODE_LANG) {
            showLanguagePicker();
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
        autoCapAfterSentence();
        updateSuggestions();
    }

    private void playSound() {
        if (!prefs.sound) return;
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null && am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.playSoundEffect(AudioManager.FX_KEY_CLICK);
        }
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
