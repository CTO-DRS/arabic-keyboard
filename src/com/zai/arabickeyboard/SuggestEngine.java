package com.zai.arabickeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * محرك التنبؤ الذكي — DRS Smart v2.1
 * - قاموس عربي موسّع وإنجليزي مضمّن (الكلمات الأكثر شيوعاً بتردداتها)
 * - تطبيع عربي: الهمزات والتاء المربوطة والألف المقصورة وحدة للمطابقة
 *   (كتابة "انشاءالله" أو "إن شاء" أو "شاءالله" تقترح الصواب)
 * - اقتراحات بإكمال البادئة مرتبة بالتردد، وتعلّم كلمات المستخدم
 * - تصحيح تلقائي لمسافة تعديل 1 (عربي بعد التطبيع + إنجليزي)
 * - تنبؤ بالكلمة التالية عبر ثنائيات (مضمّنة + متعلمة)
 * - تراجع عن التصحيح التلقائي: يرجع المحرك الكلمة الأصلية عند الحذف
 */
public class SuggestEngine {

    private static final int MAX_LEARNED = 300;
    private static final int MAX_BIGRAMS = 250;
    private static final int MAX_SUGGESTIONS = 3;
    /** أقل تردد يُقبل به مرشّح تصحيح عربي (يمنع التصحيح نحو كلمات نادرة) */
    private static final int MIN_AR_CORRECT_FREQ = 60;

    private final Map<String, Integer> arWords = new HashMap<>();
    /** مفتاح مُطبَّع ← الكلمة الأصلية الأعلى تردداً */
    private final Map<String, String> arNormKey = new HashMap<>();
    private final Map<String, Integer> enWords = new HashMap<>();
    private final Map<String, Integer> learned = new HashMap<>();
    /** ثنائيات مضمّنة: كلمة ← قائمة كلمات تالية */
    private final Map<String, List<String>> biStatic = new HashMap<>();
    /** ثنائيات متعلمة من المستخدم: "أ|ب" ← تكرار */
    private final Map<String, Integer> biLearned = new HashMap<>();
    private final SharedPreferences sp;
    /** اختصارات نصية: {اختصار، توسيع} مرتبة بإضافة المستخدم */
    private final ArrayList<String[]> shortcuts = new ArrayList<>();

    public SuggestEngine(Context c) {
        sp = c.getSharedPreferences("kb_suggest", Context.MODE_PRIVATE);
        loadDict(Words.ARABIC, arWords);
        loadDict(Words.ENGLISH, enWords);
        buildNormIndex();
        loadBigrams();
        loadBiLearned();
        loadLearned();
        loadShortcuts();
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
                // تجاهل العبارات المركبة (تحتوي فراغاً داخلياً) — القاموس كلمات مفردة
                if (!w.isEmpty() && !w.contains(" ") && freq > 0) out.put(w, freq);
            } catch (NumberFormatException ignored) {}
        }
    }

    /** فهرس تطبيع عربي: normAr(كلمة) ← أفضل كلمة أصلية */
    private void buildNormIndex() {
        arNormKey.clear();
        for (Map.Entry<String, Integer> e : arWords.entrySet()) {
            String nk = normAr(e.getKey());
            String cur = arNormKey.get(nk);
            if (cur == null || e.getValue() > arWords.get(cur)) {
                arNormKey.put(nk, e.getKey());
            }
        }
    }

    /**
     * تطبيع عربي للمطابقة: توحيد الهمزات (أإآٱ→ا)، التاء المربوطة→هاء،
     * الألف المقصورة→ياء، حذف التطويل والحركات.
     */
    public static String normAr(String w) {
        if (w == null) return "";
        StringBuilder sb = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char ch = w.charAt(i);
            if (ch == '\u0640' /* ـ تطويل */) continue;
            if (ch >= '\u064B' && ch <= '\u0652') continue; // حركات
            switch (ch) {
                case 'أ': case 'إ': case 'آ': case 'ٱ': ch = 'ا'; break;
                case 'ة': ch = 'ه'; break;
                case 'ى': ch = 'ي'; break;
            }
            sb.append(ch);
        }
        return sb.toString();
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

    /** حجم القاموس العربي المضمّن (للفحص الذكي) */
    public int arDictSize() { return arWords.size(); }

    /** حجم القاموس الإنجليزي المضمّن (للفحص الذكي) */
    public int enDictSize() { return enWords.size(); }

    // ==================== الاختصارات النصية ====================

    private void loadShortcuts() {
        shortcuts.clear();
        String blob = sp.getString("shortcuts", null);
        if (blob == null) {
            // بذرة أولى مفيدة عند أول تشغيل
            shortcuts.add(new String[]{"سلام", "السلام عليكم ورحمة الله وبركاته"});
            shortcuts.add(new String[]{"صباح", "صباح الخير يا صديقي 🌞"});
            saveShortcuts();
            return;
        }
        for (String entry : blob.split("\u0001")) {
            int p = entry.indexOf("\u0003");
            if (p > 0) {
                String ab = entry.substring(0, p).trim();
                String ex = entry.substring(p + 1);
                if (!ab.isEmpty() && !ex.isEmpty()) shortcuts.add(new String[]{ab, ex});
            }
        }
    }

    private void saveShortcuts() {
        StringBuilder sb = new StringBuilder();
        for (String[] s : shortcuts) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(s[0]).append('\u0003').append(s[1]);
        }
        sp.edit().putString("shortcuts", sb.toString()).apply();
    }

    public ArrayList<String[]> getShortcuts() { return new ArrayList<>(shortcuts); }

    public void addShortcut(String abbr, String expansion) {
        if (abbr == null || expansion == null) return;
        abbr = abbr.trim();
        expansion = expansion.trim();
        if (abbr.isEmpty() || expansion.isEmpty() || abbr.length() > 24) return;
        removeShortcut(abbr); // استبدال أي تعريف سابق لنفس الاختصار
        shortcuts.add(new String[]{abbr, expansion});
        saveShortcuts();
    }

    public void removeShortcut(String abbr) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            if (shortcuts.get(i)[0].equals(abbr)) shortcuts.remove(i);
        }
        saveShortcuts();
    }

    public void clearShortcuts() {
        shortcuts.clear();
        saveShortcuts();
    }

    /** توسيع الاختصار إن وُجد (الإنجليزية غير حساسة لحالة الأحرف) */
    public String shortcutExpansion(String word, int lang) {
        if (word == null || word.isEmpty()) return null;
        for (String[] s : shortcuts) {
            if (lang == 1) {
                if (s[0].equalsIgnoreCase(word)) return s[1];
            } else if (s[0].equals(word)) {
                return s[1];
            }
        }
        return null;
    }

    // ==================== الكتابة بالسحب (Glide) ====================

    /** هل الكلمة سلسلة فرعية (بترتيب) من الحروف المرصودة؟ */
    private static boolean isSubsequence(String word, String letters) {
        int i = 0;
        for (int j = 0; j < letters.length() && i < word.length(); j++) {
            if (word.charAt(i) == letters.charAt(j)) i++;
        }
        return i == word.length();
    }

    /**
     * كلمات مرشّحة للسحب: كلمات يمكن رسمها بتمرير الإصبع على نفس الحروف
     * بالترتيب (كلمة ⊆ حروف المسار)، مرتبة بالتردد.
     */
    public List<Map.Entry<String, Integer>> glideCandidates(String letters, int lang) {
        List<Map.Entry<String, Integer>> out = new ArrayList<>();
        if (letters == null || letters.length() < 2) return out;
        if (lang == 1) letters = letters.toLowerCase(Locale.ENGLISH);
        else letters = normAr(letters);
        Map<String, Integer> dict = (lang == 1) ? enWords : arWords;
        for (Map.Entry<String, Integer> e : dict.entrySet()) {
            String w = lang == 1 ? e.getKey() : normAr(e.getKey());
            if (w.length() < 2 || w.length() > letters.length()) continue;
            if (isSubsequence(w, letters)) out.add(new HashMap.SimpleEntry<>(e.getKey(), e.getValue()));
            if (out.size() >= 60) break;
        }
        // الكلمات المتعلمة (بأولوية أعلى)
        for (Map.Entry<String, Integer> e : learned.entrySet()) {
            if (out.size() >= 80) break;
            String w = lang == 1 ? e.getKey() : normAr(e.getKey());
            if (w.length() < 2 || w.length() > letters.length()) continue;
            if (isSubsequence(w, letters)) out.add(new HashMap.SimpleEntry<>(e.getKey(), e.getValue() + 50));
        }
        out.sort((a, b) -> b.getValue() - a.getValue());
        return out;
    }

    // ==================== تصدير/استيراد بيانات المحرك ====================

    /** سلسلة الكلمات المتعلمة الخام (للنسخ الاحتياطي) */
    public String exportLearned() { return sp.getString("learned", ""); }

    /** سلسلة الثنائيات المتعلمة الخام (للنسخ الاحتياطي) */
    public String exportBigrams() { return sp.getString("bigrams", ""); }

    /** سلسلة الاختصارات الخام (للنسخ الاحتياطي) */
    public String exportShortcuts() { return sp.getString("shortcuts", ""); }

    /** استيراد الكلمات المتعلمة من نسخة احتياطية (يدمج مع الموجود) */
    public void importLearned(String blob) {
        if (blob == null) return;
        for (String entry : blob.split("\u0001")) {
            int p = entry.lastIndexOf('|');
            if (p > 0) {
                try {
                    String w = entry.substring(0, p);
                    int f = Integer.parseInt(entry.substring(p + 1));
                    Integer cur = learned.get(w);
                    learned.put(w, Math.max(f, cur == null ? 0 : cur));
                } catch (NumberFormatException ignored) {}
            }
        }
        saveLearned();
    }

    /** استيراد الثنائيات من نسخة احتياطية (يدمج مع الموجود) */
    public void importBigrams(String blob) {
        if (blob == null) return;
        for (String entry : blob.split("\u0001")) {
            int p = entry.lastIndexOf('|');
            if (p > 0) {
                try {
                    String k = entry.substring(0, p);
                    int f = Integer.parseInt(entry.substring(p + 1));
                    Integer cur = biLearned.get(k);
                    biLearned.put(k, Math.max(f, cur == null ? 0 : cur));
                } catch (NumberFormatException ignored) {}
            }
        }
        // حفظ عبر نفس مسار learnBigram
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : biLearned.entrySet()) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(e.getKey()).append('|').append(e.getValue());
        }
        sp.edit().putString("bigrams", sb.toString()).apply();
    }

    /** استيراد الاختصارات من نسخة احتياطية (يدمج مع الموجود) */
    public void importShortcuts(String blob) {
        if (blob == null || blob.isEmpty()) return;
        for (String entry : blob.split("\u0001")) {
            int p = entry.indexOf("\u0003");
            if (p > 0) addShortcut(entry.substring(0, p), entry.substring(p + 1));
        }
    }

    /** حذف كل الكلمات المتعلّمة */
    public void resetLearned() {
        learned.clear();
        biLearned.clear();
        sp.edit().remove("learned").remove("bigrams").apply();
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

    // ==================== ثنائيات الكلمات (تنبؤ تالٍ) ====================

    private void loadBigrams() {
        biStatic.clear();
        // ثنائيات عربية شائعة (مضمّنة)
        putBi(biStatic, "الحمد", "لله");
        putBi(biStatic, "ان", "شاء");
        putBi(biStatic, "إن", "شاء");
        putBi(biStatic, "شاء", "الله");
        putBi(biStatic, "ما", "شاء");
        putBi(biStatic, "صباح", "الخير");
        putBi(biStatic, "مساء", "الخير");
        putBi(biStatic, "كيف", "حالك");
        putBi(biStatic, "كيف", "الحال");
        putBi(biStatic, "ان", "بخير");
        putBi(biStatic, "جزاك", "الله");
        putBi(biStatic, "بارك", "الله");
        putBi(biStatic, "يا", "اخي");
        putBi(biStatic, "انا", "بخير");
        putBi(biStatic, "على", "الرحب");
        putBi(biStatic, "مع", "السلامة");
        putBi(biStatic, "تصبح", "على");
        putBi(biStatic, "من", "فضلك");
        putBi(biStatic, "لا", "شكرا");
        putBi(biStatic, "ما", "عليك");
        putBi(biStatic, "ان", "شاء");
        putBi(biStatic, "عن", "ابد");
        putBi(biStatic, "كل", "عام");
        putBi(biStatic, "عيد", "سعيد");
        putBi(biStatic, "رمضان", "كريم");
        putBi(biStatic, "نهارك", "سعيد");
        putBi(biStatic, "يومك", "سعيد");
        putBi(biStatic, "احبك", "كثيرا");
        putBi(biStatic, "شكرا", "جزيلا");
        putBi(biStatic, "بالتوفيق", "ان");
        putBi(biStatic, "في", "البيت");
        putBi(biStatic, "في", "العمل");
        putBi(biStatic, "الى", "البيت");
        putBi(biStatic, "بعد", "ذلك");
        putBi(biStatic, "في", "النهاية");
        putBi(biStatic, "من", "الجديد");
        putBi(biStatic, "ماذا", "تفعل");
        putBi(biStatic, "اين", "انت");
        putBi(biStatic, "متى", "ستاتي");
        putBi(biStatic, "اريد", "ان");
        putBi(biStatic, "احتاج", "الى");
        putBi(biStatic, "استطيع", "ان");
        putBi(biStatic, "لا", "ايمكن");
        putBi(biStatic, "هل", "يمكن");
        putBi(biStatic, "هل", "تعرف");
        putBi(biStatic, "لا", "اعرف");
        putBi(biStatic, "لا", "مشكلة");
        putBi(biStatic, "بكل", "تأكيد");
        putBi(biStatic, "بحمد", "الله");
        putBi(biStatic, "استغفر", "الله");
        putBi(biStatic, "سبحان", "الله");
        // ثنائيات إنجليزية شائعة
        putBi(biStatic, "thank", "you");
        putBi(biStatic, "good", "morning");
        putBi(biStatic, "good", "night");
        putBi(biStatic, "good", "luck");
        putBi(biStatic, "how", "are");
        putBi(biStatic, "are", "you");
        putBi(biStatic, "nice", "to");
        putBi(biStatic, "see", "you");
        putBi(biStatic, "of", "course");
        putBi(biStatic, "no", "problem");
        putBi(biStatic, "i'm", "fine");
        putBi(biStatic, "let's", "go");
        putBi(biStatic, "i", "love");
        putBi(biStatic, "i", "want");
        putBi(biStatic, "i", "need");
        putBi(biStatic, "i", "think");
        putBi(biStatic, "i", "know");
        putBi(biStatic, "do", "you");
        putBi(biStatic, "can", "you");
        putBi(biStatic, "what", "about");
        putBi(biStatic, "right", "now");
        putBi(biStatic, "very", "good");
        putBi(biStatic, "a", "lot");
        putBi(biStatic, "as", "well");
        putBi(biStatic, "so", "much");
        putBi(biStatic, "take", "care");
    }

    private static void putBi(Map<String, List<String>> m, String a, String b) {
        List<String> l = m.get(a);
        if (l == null) { l = new ArrayList<>(); m.put(a, l); }
        if (!l.contains(b)) l.add(b);
    }

    /** تعلّم ثنائية كتبها المستخدم فعلاً */
    public void learnBigram(String prev, String word, int lang) {
        if (prev == null || word == null) return;
        prev = prev.trim(); word = word.trim();
        if (prev.isEmpty() || word.isEmpty()) return;
        if (lang == 1) {
            prev = prev.toLowerCase(Locale.ENGLISH);
            word = word.toLowerCase(Locale.ENGLISH);
        }
        if (!isLetterWord(prev, lang) || !isLetterWord(word, lang)) return;
        String key = prev + "\u0002" + word;
        biLearned.put(key, biLearned.containsKey(key) ? biLearned.get(key) + 1 : 2);
        if (biLearned.size() > MAX_BIGRAMS) {
            // إبقاء الأكثر تكراراً
            List<Map.Entry<String, Integer>> es = new ArrayList<>(biLearned.entrySet());
            java.util.Collections.sort(es, (a, b) -> b.getValue() - a.getValue());
            biLearned.clear();
            for (int i = 0; i < MAX_BIGRAMS; i++) biLearned.put(es.get(i).getKey(), es.get(i).getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : biLearned.entrySet()) {
            if (sb.length() > 0) sb.append('\u0001');
            sb.append(e.getKey()).append('|').append(e.getValue());
        }
        sp.edit().putString("bigrams", sb.toString()).apply();
    }

    private void loadBiLearned() {
        String blob = sp.getString("bigrams", "");
        for (String entry : blob.split("\u0001")) {
            int p = entry.lastIndexOf('|');
            if (p > 0) {
                try {
                    biLearned.put(entry.substring(0, p), Integer.parseInt(entry.substring(p + 1)));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /** اقتراحات الكلمة التالية بعد كلمة سابقة (ثنائيات متعلمة ثم مضمّنة) */
    public List<String> nextWords(String prev, int lang) {
        List<String> out = new ArrayList<>();
        if (prev == null) return out;
        String p = prev.trim();
        if (p.isEmpty() || p.length() > 24) return out;
        if (lang == 1) p = p.toLowerCase(Locale.ENGLISH);

        // 1) ثنائيات تعلمها المستخدم (الأعلى تكراراً أولاً)
        List<Map.Entry<String, Integer>> hits = new ArrayList<>();
        for (Map.Entry<String, Integer> e : biLearned.entrySet()) {
            int sep = e.getKey().indexOf('\u0002');
            if (sep <= 0) continue;
            if (e.getKey().substring(0, sep).equals(p)) {
                hits.add(new HashMap.SimpleEntry<>(e.getKey().substring(sep + 1), e.getValue()));
            }
        }
        hits.sort((a, b) -> b.getValue() - a.getValue());
        for (Map.Entry<String, Integer> h : hits) {
            if (out.size() >= MAX_SUGGESTIONS) break;
            if (!out.contains(h.getKey())) out.add(h.getKey());
        }

        // 2) ثنائيات مضمّنة
        List<String> st = biStatic.get(p);
        if (st != null) {
            for (String w : st) {
                if (out.size() >= MAX_SUGGESTIONS) break;
                if (!out.contains(w)) out.add(w);
            }
        }
        return out;
    }

    /** هل الكلمة موجودة في القاموس (للتصحيح التلقائي) */
    public boolean known(String word, int lang) {
        if (word == null || word.isEmpty()) return true;
        if (lang == 1) {
            return enWords.containsKey(word.toLowerCase(Locale.ENGLISH)) || learned.containsKey(word);
        }
        return arWords.containsKey(word) || learned.containsKey(word)
                || arNormKey.containsKey(normAr(word));
    }

    /**
     * اقتراحات للكلمة الحالية.
     * الأولوية: الكلمات المتعلمة المطابقة للبادئة ← القاموس بالتطبيع والتردد.
     */
    public List<String> suggest(String prefix, int lang) {
        List<String> out = new ArrayList<>();
        if (prefix == null || prefix.length() < 1) return out;
        boolean en = lang == 1;
        if (en) prefix = prefix.toLowerCase(Locale.ENGLISH);
        if (prefix.length() > 24) return out;

        String norm = null;
        if (!en) norm = normAr(prefix);

        // 0) توسيع الاختصارات المطابقة للبادئة (أولوية قصوى)
        for (String[] s : shortcuts) {
            if (out.size() >= MAX_SUGGESTIONS) break;
            String ab = en ? s[0].toLowerCase(Locale.ENGLISH) : s[0];
            boolean hit = en ? ab.startsWith(prefix) : normAr(ab).startsWith(norm);
            if (hit && !out.contains(s[1])) out.add(s[1]);
        }

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
            if (en) {
                for (Map.Entry<String, Integer> e : enWords.entrySet()) {
                    String w = e.getKey();
                    if (w.equals(prefix)) continue;
                    if (w.startsWith(prefix)) pref.add(e);
                }
            } else {
                // عربي: مطابقة عبر الفهرس المُطبَّع (تجاهل الهمزات والتاء) — norm محسوبة مسبقاً
                for (Map.Entry<String, String> e : arNormKey.entrySet()) {
                    if (e.getKey().equals(norm)) continue;
                    if (e.getKey().startsWith(norm)) {
                        Integer f = arWords.get(e.getValue());
                        pref.add(new HashMap.SimpleEntry<>(e.getValue(), f == null ? 1 : f));
                    }
                }
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

    /**
     * أفضل كلمة لمسافة تعديل 1 (للتصحيح التلقائي).
     * الإنجليزية: على النص الخام. العربية: على الصيغة المُطبَّعة
     * (يغطي أخطاء الهمزات والتاء والألف المقصورة وأخطاء حرف واحد).
     */
    public String bestCorrection(String word, int lang) {
        if (word == null) return null;
        if (lang == 1) {
            if (word.length() < 4 || word.length() > 18) return null;
            word = word.toLowerCase(Locale.ENGLISH);
            return bestDist1(word, enWords, 0);
        }
        // عربي: طول ≥ 4 بعد التطبيع
        String norm = normAr(word);
        if (norm.length() < 4 || norm.length() > 18) return null;
        // إذا كانت الكلمة صحيحة بعد التطبيع فلا تصحيح (فرق الهمزة/التاء وحده لا يستدعي تدخلاً)
        if (arNormKey.containsKey(norm)) return null;
        Map<String, Integer> cand = new HashMap<>();
        for (Map.Entry<String, String> e : arNormKey.entrySet()) {
            Integer f = arWords.get(e.getValue());
            if (f != null) cand.put(e.getValue(), f);
        }
        String best = bestDist1(norm, cand, MIN_AR_CORRECT_FREQ);
        if (best == null) return null;
        // لا ترجع كلمة تساوي المدخل بعد التطبيع (مثلاً فرق همزة فقط — عادةً مقبول لكن نتجنب إن كانت الكلمة معروفة أصلاً)
        return best;
    }

    private String bestDist1(String word, Map<String, Integer> dict, int minFreq) {
        String best = null;
        int bestFreq = 0;
        for (Map.Entry<String, Integer> e : dict.entrySet()) {
            String w = e.getKey();
            if (Math.abs(w.length() - word.length()) > 1) continue;
            if (e.getValue() < minFreq) continue;
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
