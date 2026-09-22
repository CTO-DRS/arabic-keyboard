package com.zai.arabickeyboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * الإدخال الصوتي — DRS Smart v2.1
 * نافذة شفافة فوق الحقل: تستمع عبر محرك التعرف على الجهاز،
 * ثم تُدخل النص مباشرة في المؤشر عبر خدمة لوحة المفاتيح.
 * إن لم تكن الخدمة متاحة تُنسخ النتيجة إلى الحافظة مع تنبيه.
 */
public class VoiceInputActivity extends Activity implements RecognitionListener {

    /** يضبطها ImeService عند إنشائه — نفس العملية والخيط الرئيسي */
    public static ImeService delegate;

    private SpeechRecognizer sr;
    private TextView tvStatus;
    private TextView tvHint;
    private boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(0xCC0D1228);
        root.setClickable(true);
        // أي لمسة تُلغي
        root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(22));
        bg.setColor(0xFF1A2142);
        card.setBackground(bg);
        card.setPadding(dp(24), dp(26), dp(24), dp(26));

        TextView tvMic = new TextView(this);
        tvMic.setText("🎤");
        tvMic.setTextSize(42);
        tvMic.setGravity(Gravity.CENTER);
        card.addView(tvMic);

        tvStatus = new TextView(this);
        tvStatus.setText(R.string.voice_listening);
        tvStatus.setTextSize(16);
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, dp(14), 0, dp(4));
        card.addView(tvStatus);

        tvHint = new TextView(this);
        tvHint.setText(R.string.voice_cancel_hint);
        tvHint.setTextSize(12);
        tvHint.setTextColor(0xFF8A93C4);
        tvHint.setGravity(Gravity.CENTER);
        tvHint.setPadding(0, dp(6), 0, 0);
        card.addView(tvHint);

        root.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_SHORT).show();
            finishSafe();
            return;
        }

        // رفع صوت الميكروفون أثناء التسجيل
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) am.setStreamVolume(AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 0);

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        String sysLang = getResources().getConfiguration().locale.getLanguage();
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                sysLang != null && sysLang.startsWith("ar") ? "ar-SA" : sysLang);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        sr.startListening(intent);
    }

    private void deliver(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String t = text.trim();
        if (delegate != null) {
            delegate.commitVoiceResult(t);
            Toast.makeText(this, R.string.voice_done, Toast.LENGTH_SHORT).show();
        } else {
            // الخدمة غير متاحة — النسخ احتياطياً
            try {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(
                        android.content.ClipData.newPlainText("voice", t));
                Toast.makeText(this, R.string.voice_copied, Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {}
        }
        finishSafe();
    }

    private void fail(String message) {
        if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finishSafe();
    }

    private void finishSafe() {
        if (finished) return;
        finished = true;
        finish();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ==================== RecognitionListener ====================

    @Override public void onReadyForSpeech(Bundle params) {
        tvStatus.setText(R.string.voice_listening);
    }

    @Override public void onBeginningOfSpeech() {
        tvStatus.setText(R.string.voice_speaking);
    }

    @Override public void onRmsChanged(float rmsdB) {}

    @Override public void onBufferReceived(byte[] buffer) {}

    @Override public void onEndOfSpeech() {
        tvStatus.setText(R.string.voice_processing);
    }

    @Override public void onError(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                fail(getString(R.string.voice_no_match));
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                fail(getString(R.string.voice_no_permission));
                break;
            default:
                fail(getString(R.string.voice_error));
        }
    }

    @Override public void onResults(Bundle results) {
        ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null && !list.isEmpty()) deliver(list.get(0));
        else fail(getString(R.string.voice_no_match));
    }

    @Override public void onPartialResults(Bundle partialResults) {}

    @Override public void onEvent(int eventType, Bundle params) {}

    @Override
    protected void onDestroy() {
        if (sr != null) {
            try { sr.destroy(); } catch (Exception ignored) {}
            sr = null;
        }
        super.onDestroy();
    }
}
