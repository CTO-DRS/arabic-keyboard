package com.zai.arabickeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.View;

/** حلقة النتيجة الدائرية لشاشة الفحص الذكي — DRS Smart v2.5 */
public class ScoreRing extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private int score = -1;          // -1 = لم يُشغّل الفحص بعد
    private int color = UiKit.ACCENT;

    public ScoreRing(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(0xFF1A2242);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        sub.setTextAlign(Paint.Align.CENTER);
    }

    public void setScore(int s) {
        score = s;
        color = s >= 90 ? UiKit.GREEN : (s >= 60 ? UiKit.AMBER : UiKit.RED);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        float pad = UiKit.dp(getContext(), 10);
        box.set(pad, pad, w - pad, h - pad);
        float stroke = UiKit.dp(getContext(), 9);
        track.setStrokeWidth(stroke);
        ring.setStrokeWidth(stroke);
        ring.setShader(null);
        text.setTypeface(UiKit.bold());
        sub.setTypeface(UiKit.medium());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ring.getShader() == null) {
            ring.setShader(new SweepGradient(getWidth() / 2f, getHeight() / 2f,
                    new int[]{color, UiKit.ACCENT_SOFT, color}, null));
        }
        float inset = UiKit.dp(getContext(), 9);
        RectF r = new RectF(box.left + inset, box.top + inset,
                box.right - inset, box.bottom - inset);
        canvas.drawArc(r, 0, 360, false, track);
        if (score >= 0) {
            canvas.drawArc(r, -90, -360f * score / 100f, false, ring);
            text.setColor(color);
            text.setTextSize(getWidth() * 0.20f);
            float cy = getHeight() / 2f;
            float off = (text.ascent() + text.descent()) / 2f;
            canvas.drawText(String.valueOf(score), getWidth() / 2f, cy + off - UiKit.dp(getContext(), 5), text);
            sub.setColor(UiKit.TEXT_SUB);
            sub.setTextSize(getWidth() * 0.075f);
            canvas.drawText("/ 100", getWidth() / 2f, cy + sub.getTextSize() * 1.6f + UiKit.dp(getContext(), 8), sub);
        } else {
            text.setColor(UiKit.TEXT_FAINT);
            text.setTextSize(getWidth() * 0.13f);
            canvas.drawText("—", getWidth() / 2f,
                    getHeight() / 2f - (text.ascent() + text.descent()) / 2f, text);
        }
    }
}
