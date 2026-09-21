package com.zai.arabickeyboard;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.inputmethodservice.InputMethodService;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * خدمة لوحة المفاتيح — عربي/إنجليزي/أرقام/رموز/إيموجي.
 */
public class ImeService extends InputMethodService implements KeyboardView.Listener {

    public static final int MODE_AR = 0;
    public static final int MODE_EN = 1;
    public static final int MODE_SYM1 = 2;
    public static final int MODE_SYM2 = 3;
    public static final int MODE_EMOJI = 4;

    private int lang = MODE_AR;   // اللغة الأساسية
    private int mode = MODE_AR;   // الوضع الحالي
    private boolean shiftOn, shiftLock;
    private long lastSpaceAt;
    private boolean lastWasSpace;

    private LinearLayout container;
    private KeyboardView kv;
    private ScrollView emojiScroll;
    private GridLayout emojiGrid;
    private Prefs prefs;
    private ThemeSet theme;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false; // منع وضع ملء الشاشة المزعج في الوضع الأفقي
    }

    private boolean systemNight() {
        int m = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public View onCreateInputView() {
        prefs.reload();
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        kv = new KeyboardView(this);
        kv.setListener(this);
        kv.hapticsEnabled = prefs.haptics;
        kv.setKeyHeightScale(heightScale(prefs.keyHeight));
        container.addView(kv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        emojiScroll = buildEmojiPanel();
        emojiScroll.setVisibility(View.GONE);
        int panelH = getResources().getDimensionPixelSize(R.dimen.emoji_panel_height);
        container.addView(emojiScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        theme = ThemeSet.resolve(prefs.theme, prefs.accent, systemNight());
        kv.setTheme(theme);
        applyEmojiTheme();
        mode = lang;
        refreshLayout();
        return container;
    }

    private ScrollView buildEmojiPanel() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        emojiGrid = new GridLayout(this);
        emojiGrid.setColumnCount(6);
        String[] list = Layouts.emojis();
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
                    if (kv.hapticsEnabled) kv.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    onText(e);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            emojiGrid.addView(tv, lp);
        }
        sv.addView(emojiScrollChild());
        return sv;
    }

    private View emojiScrollChild() {
        FrameLayout fl = new FrameLayout(this);
        fl.addView(emojiGrid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL));
        return fl;
    }

    private void applyEmojiTheme() {
        if (emojiGrid != null) emojiGrid.setBackgroundColor(theme.kbBg);
        if (emojiScroll != null) emojiScroll.setBackgroundColor(theme.kbBg);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        prefs.reload();
        ThemeSet t = ThemeSet.resolve(prefs.theme, prefs.accent, systemNight());
        if (kv != null) {
            kv.hapticsEnabled = prefs.haptics;
            kv.setTheme(t);
            kv.setKeyHeightScale(heightScale(prefs.keyHeight));
        }
        theme = t;
        applyEmojiTheme();
        mode = lang;
        shiftOn = false;
        shiftLock = false;
        lastWasSpace = false;
        refreshLayout();
        updateEnterLabel();
        maybeAutoCapAtStart(info);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (kv != null) kv.cancelAll();
        super.onFinishInputView(finishingInput);
    }

    private void refreshLayout() {
        if (kv == null) return;
        kv.setKeyboard(Layouts.get(mode, lang, prefs.numRow));
        kv.setShifted(isShifted());
        kv.setShiftLock(shiftLock);
        if (mode == MODE_EMOJI) {
            kv.setVisibility(View.VISIBLE);
            emojiScroll.setVisibility(View.VISIBLE);
        } else {
            emojiScroll.setVisibility(View.GONE);
            kv.setVisibility(View.VISIBLE);
        }
    }

    private boolean isShifted() {
        return (shiftOn || shiftLock) && (mode == MODE_AR || mode == MODE_EN);
    }

    private void updateEnterLabel() {
        String label = null;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && (mode == MODE_AR || mode == MODE_EN
                || mode == MODE_SYM1 || mode == MODE_SYM2)) {
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
                lang = (lang == MODE_AR) ? MODE_EN : MODE_AR;
                mode = lang;
                shiftOn = false; shiftLock = false;
                refreshLayout();
                updateEnterLabel();
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
                mode = lang; shiftOn = false; shiftLock = false;
                refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_EMOJI:
                mode = MODE_EMOJI; refreshLayout(); updateEnterLabel();
                return;
            case Key.CODE_BACKSPACE:
                handleBackspace(ic);
                return;
            case Key.CODE_ENTER:
                handleEnter(ic);
                return;
            case Key.CODE_SPACE: {
                InputConnection ic2 = getCurrentInputConnection();
                if (ic2 == null) return;
                long now = System.currentTimeMillis();
                // نقطة تلقائية بضغطتين متتاليتين على المسافة
                if (prefs.doubleSpace && lastWasSpace && (now - lastSpaceAt) < 400) {
                    CharSequence before = ic2.getTextBeforeCursor(1, 0);
                    if (before != null && before.length() == 1 && before.charAt(0) == ' ') {
                        ic2.deleteSurroundingText(1, 0);
                        ic2.commitText(". ", 1);
                        lastWasSpace = false;
                        autoCapAfterSentence();
                        return;
                    }
                }
                ic2.commitText(" ", 1);
                lastWasSpace = true;
                lastSpaceAt = now;
                return;
            }
            default: {
                String t = currentText(k);
                lastWasSpace = false;
                if (t == null || t.length() == 0) return;
                if (ic != null) ic.commitText(t, 1);
                consumeShift();
            }
        }
    }

    @Override
    public void onText(String t) {
        playSound();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && t != null && t.length() > 0) ic.commitText(t, 1);
        lastWasSpace = false;
        consumeShift();
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

    /** تفعيل Shift تلقائياً لما بعد نهاية الجملة (إنجليزي) */
    private void autoCapAfterSentence() {
        if (prefs.autoCap && lang == MODE_EN) {
            shiftOn = true;
            if (kv != null) kv.setShifted(true);
        }
    }

    /** Shift تلقائي عند بداية الحقل النصي */
    private void maybeAutoCapAtStart(EditorInfo info) {
        if (!prefs.autoCap || lang != MODE_EN || kv == null || info == null) return;
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
            // فتح شاشة إعدادات التطبيق
            try {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception ignored) {}
        }
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
