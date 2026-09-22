package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * محرك التنبؤ الذكي — DRS Smart
 * - قاموس عربي وإنجليزي مضمّن (الكلمات الأكثر شيوعاً بتردداتها)
 * - اقتراحات بإكمال البادئة مرتبة بالتردد
 * - تعلّم كلمات المستخدم (تُحفظ دائماً وتظهر أولاً)
 * - تصحيح تلقائي لمسافة تعديل 1 عند الضغط على المسافة
 */
public class SuggestEngine {

    private static final int MAX_LEARNED = 300;
    private static final int MAX_SUGGESTIONS = 3;

    private final Map<String, Integer> arWords = new HashMap<>();
    private final Map<String, Integer> enWords = new HashMap<>();
    private final Map<String, Integer> learned = new HashMap<>();
    private final SharedPreferences sp;

    public SuggestEngine(Context c) {
        sp = c.getSharedPreferences("kb_suggest", Context.MODE_PRIVATE);
        loadDict(Words.ARABIC, arWords);
        loadDict(Words.ENGLISH, enWords);
        loadLearned();
    }

    private void loadDict(String raw, Map<String, Integer> out) {
        out.clear();
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int spIdx = line.lastIndexOf(' ');
            if (spIdx <= 0) continue;
            try {
                String w = line.substring(0, spIdx).trim();
                int freq = Integer.parseInt(line.substring(spIdx + 1).trim());
                if (!w.isEmpty() && freq > 0) out.put(w, freq);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void loadLearned() {
        learned.clear();
        String blob = sp.getString("learned", "");
        for (String entry : blob.split("\u0001")) {
            int p = entry.lastIndexOf('|');
            if (p > 0) {
                try {
                    learned.put(entry.substring(0, p), Integer.parseInt(entry.substring(p + 1)));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void saveLearned() {
        if (learned.size() > MAX_LEARNED) {
            // إبقاء الأعلى تكراراً
            List<Map.Entry<String, Integer>> es = new ArrayList<>(learned.entrySet());
            java.util.Collections.sort(es, (a, b) -> b.getValue() - a.getValue());
            learned.clear();
            for (int i = 0; i < MAX_LEARNED; i++) learned.put(es.get(i).getKey(), es.get(i).getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : learned.entrySet()) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(e.getKey()).append('|').append(e.getValue());
        }
        sp.edit().putString("learned", sb.toString()).apply();
    }

    /** عدد الكلمات التي تعلّمها المحرك من المستخدم */
    public int learnedCount() { return learned.size(); }

    /** حذف كل الكلمات المتعلّمة */
    public void resetLearned() {
        learned.clear();
        sp.edit().remove("learned").apply();
    }

    /** تعلّم كلمة كتبها المستخدم فعلاً */
    public void learn(String word, int lang) {
        if (word == null) return;
        word = word.trim();
        if (word.length() < 2 || word.length() > 30) return;
        if (lang == 1) word = word.toLowerCase(Locale.ENGLISH);
        if (isLetterWord(word, lang)) {
            learned.put(word, learned.containsKey(word) ? learned.get(word) + 1 : 3);
            saveLearned();
        }
    }

    private static boolean isLetterWord(String w, int lang) {
        for (int i = 0; i < w.length(); i++) {
            char ch = w.charAt(i);
            if (lang == 1) {
                if (!Character.isLowerCase(ch)) return false;
            } else {
                if (!(ch >= 0x0600 && ch <= 0x06FF) && ch != 'ة' && ch != 'أ' && ch != 'إ' && ch != 'آ') return false;
            }
        }
        return true;
    }

    /** هل الكلمة موجودة في القاموس (للتصحيح التلقائي) */
    public boolean known(String word, int lang) {
        if (word == null || word.isEmpty()) return true;
        Map<String, Integer> dict = lang == 1 ? enWords : arWords;
        if (lang == 1) word = word.toLowerCase(Locale.ENGLISH);
        return dict.containsKey(word) || learned.containsKey(word);
    }

    /**
     * اقتراحات للكلمة الحالية.
     * الأولوية: الكلمات المتعلمة المطابقة للبادئة ← قاموس اللغة بالبادئة والتردد.
     */
    public List<String> suggest(String prefix, int lang) {
        List<String> out = new ArrayList<>();
        if (prefix == null || prefix.length() < 1) return out;
        boolean en = lang == 1;
        if (en) prefix = prefix.toLowerCase(Locale.ENGLISH);
        if (prefix.length() > 24) return out;
        Map<String, Integer> dict = en ? enWords : arWords;

        // 1) كلمات متعلمة تبدأ بنفس البادئة (مرتبة بالتكرار)
        List<Map.Entry<String, Integer>> learnedHits = new ArrayList<>();
        for (Map.Entry<String, Integer> e : learned.entrySet()) {
            String w = e.getKey();
            if (w.startsWith(prefix) && !w.equals(prefix)) learnedHits.add(e);
        }
        learnedHits.sort((a, b) -> b.getValue() - a.getValue());
        for (int i = 0; i < learnedHits.size() && out.size() < MAX_SUGGESTIONS; i++)
            out.add(cap(learnedHits.get(i).getKey(), prefix, en));

        // 2) القاموس: أقصر تطابق للبادئة ثم أعلى تردد (دون تكرار الكلمة نفسها)
        if (out.size() < MAX_SUGGESTIONS) {
            List<Map.Entry<String, Integer>> pref = new ArrayList<>();
            for (Map.Entry<String, Integer> e : dict.entrySet()) {
                String w = e.getKey();
                if (w.equals(prefix)) continue;
                if (w.startsWith(prefix)) pref.add(e);
            }
            pref.sort((a, b) -> {
                int la = a.getKey().length(), lb = b.getKey().length();
                if (la != lb) return la - lb;
                return b.getValue() - a.getValue();
            });
            for (Map.Entry<String, Integer> e : pref) {
                if (out.size() >= MAX_SUGGESTIONS) break;
                String w = cap(e.getKey(), prefix, en);
                if (!out.contains(w)) out.add(w);
            }
        }
        return out;
    }

    private static String cap(String w, String prefix, boolean en) {
        // مطابقة حالة الحرف الأول للإنجليزية (يدوف يدوياً)
        if (en && !prefix.isEmpty() && !w.isEmpty()
                && Character.isUpperCase(prefix.charAt(0)) && w.length() > 1) {
            return Character.toUpperCase(w.charAt(0)) + w.substring(1);
        }
        return w;
    }

    /** أفضل كلمة لمسافة تعديل 1 (للتصحيح التلقائي) — إنجليزي فقط (العربية غنية صرفياً والقاموس صغير) */
    public String bestCorrection(String word, int lang) {
        if (lang != 1) return null;
        if (word == null || word.length() < 4 || word.length() > 18) return null;
        Map<String, Integer> dict = enWords;
        word = word.toLowerCase(Locale.ENGLISH);
        String best = null;
        int bestFreq = 0;
        for (Map.Entry<String, Integer> e : dict.entrySet()) {
            String w = e.getKey();
            if (Math.abs(w.length() - word.length()) > 1) continue;
            if (editDistance1(word, w) && e.getValue() > bestFreq) {
                best = w;
                bestFreq = e.getValue();
            }
        }
        return best;
    }

    /** مسافة تعديل 1: حذف/إضافة/استبدال حرف واحد فقط */
    private static boolean editDistance1(String a, String b) {
        int la = a.length(), lb = b.length();
        if (la == lb) {
            int diff = 0;
            for (int i = 0; i < la; i++) if (a.charAt(i) != b.charAt(i) && ++diff > 1) return false;
            return diff == 1;
        }
        String s = la > lb ? b : a, l = la > lb ? a : b;
        if (l.length() - s.length() != 1) return false;
        int i = 0, j = 0, skipped = 0;
        while (i < s.length() && j < l.length()) {
            if (s.charAt(i) == l.charAt(j)) { i++; j++; }
            else { j++; if (++skipped > 1) return false; }
        }
        return true;
    }
}
