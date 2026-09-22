package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * نظام الحماية الذكي من الأعطال — DRS Smart v2.3
 * - يثبّت معالج أعطال عاماً يلتقط أي انهيار غير متوقع ويحفظ تقريراً كاملاً به
 *   في ملف داخلي (يقرأه الفحص الذكي لاحقاً ليعرضه للمستخدم).
 * - يوفر غلاف تنفيذ آمن guard(): أي كود قد يرمي استثناءً يُنفَّذ داخله
 *   فلا تسقط خدمة لوحة المفاتيح ولا التطبيق، ويُسجَّل العطل للفحص.
 */
public final class CrashGuard {

    private static final String FILE = "crash_report.txt";
    private static final String FLAG = "has_crash";

    private static SharedPreferences sp;
    private static File reportFile;
    private static boolean installed;

    private CrashGuard() {}

    /** تثبيت معالج الأعطال العام — يُستدعى مرة واحدة من onCreate للنشاط والخدمة */
    public static void install(Context c) {
        if (installed) return;
        installed = true;
        sp = c.getSharedPreferences("kb_guard", Context.MODE_PRIVATE);
        reportFile = new File(c.getFilesDir(), FILE);
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try { save(e); } catch (Exception ignored) {}
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
    }

    private static void save(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String text = "----- عطل مسجل -----\n"
                + "الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                + "\nالخيط: " + Thread.currentThread().getName()
                + "\nالإصدار: " + BuildInfo.VERSION_NAME
                + "\n\n" + sw.toString();
        if (reportFile != null) {
            OutputStreamWriter w = null;
            try {
                w = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");
                w.write(text);
            } catch (Exception ignored) {
            } finally {
                try { if (w != null) w.close(); } catch (Exception ignored) {}
            }
        }
        if (sp != null) sp.edit().putBoolean(FLAG, true).apply();
    }

    /** تسجيل استثناء دون إسقاط العملية (للاستدعاء من أغلفة catch) */
    public static void log(Throwable t) {
        try { save(t); } catch (Exception ignored) {}
        Log.e("DRS-CrashGuard", "logged", t);
    }

    /** تنفيذ آمن: يلتقط أي استثناء ويسجله — لا يُلقي شيئاً أبداً */
    public static void guard(String op, Runnable r) {
        try {
            if (r != null) r.run();
        } catch (Throwable t) {
            Log.e("DRS-CrashGuard", "guard[" + op + "]", t);
            log(t);
        }
    }

    /** هل يوجد عطل مسجل سابقاً؟ */
    public static boolean hasReport() {
        return sp != null && sp.getBoolean(FLAG, false);
    }

    /** نص آخر تقرير عطل كاملاً (أو null إن لا يوجد) */
    public static String lastReport() {
        if (reportFile == null || !reportFile.exists()) return null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(reportFile);
            byte[] b = new byte[(int) Math.min(reportFile.length(), 20000)];
            int n = in.read(b);
            return n > 0 ? new String(b, 0, n, "UTF-8") : null;
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    /** ملخص سطر واحد لآخر عطل: نوع الاستثناء + أول إطار من المكدس */
    public static String lastReportSummary() {
        String r = lastReport();
        if (r == null) return null;
        String[] lines = r.split("\n");
        String ex = null, at = null;
        for (String l : lines) {
            l = l.trim();
            if (ex == null && (l.contains("Exception") || l.contains("Error"))
                    && !l.startsWith("-----")) ex = l;
            if (at == null && l.startsWith("at ") && l.contains("arabickeyboard")) at = l;
        }
        if (at == null) {
            for (String l : lines) {
                l = l.trim();
                if (l.startsWith("at ")) { at = l; break; }
            }
        }
        StringBuilder sb = new StringBuilder();
        if (ex != null) sb.append(ex);
        if (at != null) {
            if (sb.length() > 0) sb.append("  |  ");
            sb.append(at);
        }
        if (sb.length() == 0) {
            return r.substring(0, Math.min(140, r.length()));
        }
        return sb.toString();
    }

    /** مسح سجل الأعطال (بعد إصلاح المشكلة أو تجاهلها من المستخدم) */
    public static void clearReport() {
        if (sp != null) sp.edit().putBoolean(FLAG, false).apply();
        if (reportFile != null && reportFile.exists()) reportFile.delete();
    }
}
