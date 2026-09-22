package com.zai.arabickeyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * تخطيطات لوحة المفاتيح — DRS Smart v2.0:
 * ست لغات (عربي، إنجليزي، فرنسي، ألماني، إسباني، تركي) + أرقام + رموز + لوحة تحرير
 */
public final class Layouts {

    private Layouts() {}

    // فهارس اللغات
    public static final int LANG_AR = 0;
    public static final int LANG_EN = 1;
    public static final int LANG_FR = 2;
    public static final int LANG_DE = 3;
    public static final int LANG_ES = 4;
    public static final int LANG_TR = 5;
    public static final int LANG_COUNT = 6;

    public static final String[] LANG_NAMES = {
            "العربية", "English", "Français", "Deutsch", "Español", "Türkçe"
    };
    /** ملصق مختصر يظهر على زر التبديل (اللغة التالية) */
    public static final String[] LANG_SHORT = {"EN", "ع", "FR", "DE", "ES", "TR"};

    private static Key c(String l, String t, float w) { return Key.charKey(l, t, w); }
    private static Key f(String label, int code, float w) { return Key.func(label, code, w); }
    private static Key back(float w) { return Key.func("", Key.CODE_BACKSPACE, w); }
    private static Key shift(float w) { return Key.func("", Key.CODE_SHIFT, w); }
    private static Key enter(float w) { return Key.func("", Key.CODE_ENTER, w); }
    private static Key emoji(float w) { return Key.func("", Key.CODE_EMOJI, w); }

    private static Key space(String hint, float w) {
        Key k = Key.charKey(hint, " ", w);
        k.type = Key.SPACE;
        return k;
    }

    private static Key up(Key k) {
        if (k.text != null && k.text.length() == 1
                && Character.isLowerCase(k.text.charAt(0))) {
            String u = k.text.toUpperCase(java.util.Locale.ENGLISH);
            k.shiftLabel = u;
            k.shiftText = u;
        }
        return k;
    }

    private static Row row(Key... ks) {
        Row r = new Row();
        for (Key k : ks) r.keys.add(k);
        return r;
    }

    /** زر تبديل اللغة: يعرض اللغة التالية في الدورة */
    private static Key langKey(int lang, float w) {
        return f(LANG_SHORT[(lang + 1) % LANG_COUNT], Key.CODE_LANG, w);
    }

    public static List<Row> get(int mode, int lang, boolean numRow, boolean voice, boolean arabicDigits) {
        switch (mode) {
            case ImeService.MODE_EN: case ImeService.MODE_FR:
            case ImeService.MODE_DE: case ImeService.MODE_ES:
            case ImeService.MODE_TR:
                return latin(lang, numRow, voice);
            case ImeService.MODE_SYM1: return sym1(lang);
            case ImeService.MODE_SYM2: return sym2(lang);
            case ImeService.MODE_EDIT: return edit(lang);
            case ImeService.MODE_NUMPAD: return numpad(lang, arabicDigits);
            case ImeService.MODE_EMOJI: return emojiNav(lang);
            default: return arabic(numRow, voice);
        }
    }

    private static Row digitsRowAr() {
        return row(
                c("١", "١", 1).alt("1"), c("٢", "٢", 1).alt("2"),
                c("٣", "٣", 1).alt("3"), c("٤", "٤", 1).alt("4"),
                c("٥", "٥", 1).alt("5"), c("٦", "٦", 1).alt("6"),
                c("٧", "٧", 1).alt("7"), c("٨", "٨", 1).alt("8"),
                c("٩", "٩", 1).alt("9"), c("٠", "٠", 1).alt("0")
        );
    }

    private static Row digitsRowEn() {
        return row(
                c("1", "1", 1).alt("!"), c("2", "2", 1).alt("@"),
                c("3", "3", 1).alt("#"), c("4", "4", 1).alt("$"),
                c("5", "5", 1).alt("%"), c("6", "6", 1).alt("^"),
                c("7", "7", 1).alt("&"), c("8", "8", 1).alt("*"),
                c("9", "9", 1).alt("("), c("0", "0", 1).alt(")")
        );
    }

    // ==================== العربية ====================

    /** إضافة حركات الضغط المطوّل للأحرف التي لا تملك بدائل (فتحة ضمة كسرة شدة سكون) */
    private static Key tash(Key k) {
        if (k.alts == null) k.alt("َ", "ُ", "ِ", "ّ", "ْ");
        return k;
    }

    public static List<Row> arabic(boolean numRow, boolean voice) {
        List<Row> rows = new ArrayList<>();
        if (numRow) rows.add(digitsRowAr());
        rows.add(row(
                tash(c("ض", "ض", 1)).shift("َ", "َ"),
                tash(c("ص", "ص", 1)).shift("ً", "ً"),
                tash(c("ث", "ث", 1)).shift("ُ", "ُ"),
                tash(c("ق", "ق", 1)).shift("ٌ", "ٌ"),
                tash(c("ف", "ف", 1)).shift("لإ", "لإ"),
                tash(c("غ", "غ", 1)).shift("إ", "إ"),
                tash(c("ع", "ع", 1)).shift("ّ", "ّ"),
                c("ه", "ه", 1).shift("÷", "÷").alt("ة"),
                tash(c("خ", "خ", 1)).shift("×", "×"),
                tash(c("ح", "ح", 1)).shift("؛", "؛"),
                tash(c("ج", "ج", 1)).shift("<", "<"),
                c("د", "د", 1).shift(">", ">").alt("ذ"),
                back(1.4f)
        ));
        rows.add(row(
                tash(c("ش", "ش", 1)).shift("ِ", "ِ"),
                tash(c("س", "س", 1)).shift("~", "~"),
                c("ي", "ي", 1).shift("]", "]").alt("ئ"),
                tash(c("ب", "ب", 1)).shift("[", "["),
                c("ل", "ل", 1).shift("لأ", "لأ").alt("لآ"),
                c("ا", "ا", 1).shift("لآ", "لآ").alt("أ", "إ", "آ"),
                tash(c("ت", "ت", 1)).shift("ـ", "ـ"),
                tash(c("ن", "ن", 1)).shift("/", "/"),
                tash(c("م", "م", 1)).shift(":", ":"),
                tash(c("ك", "ك", 1)).shift("\"", "\""),
                tash(c("ط", "ط", 1)).shift("'", "'")
        ));
        rows.add(row(
                shift(1.4f),
                tash(c("ذ", "ذ", 1)).shift("ٍ", "ٍ"),
                tash(c("ئ", "ئ", 1)),
                c("ء", "ء", 1).alt("ؤ", "ئ", "أ"),
                tash(c("ؤ", "ؤ", 1)),
                tash(c("ر", "ر", 1)),
                c("لا", "لا", 1).shift("لآ", "لآ"),
                c("ى", "ى", 1).shift("آ", "آ").alt("آ"),
                c("ة", "ة", 1).alt("ه"),
                c("و", "و", 1).shift("ؤ", "ؤ").alt("ؤ"),
                tash(c("ز", "ز", 1)),
                tash(c("ظ", "ظ", 1)),
                langKey(LANG_AR, 1.2f)
        ));
        rows.add(arBottom(LANG_AR, voice));
        return rows;
    }

    // ==================== اللغات اللاتينية ====================

    private static final String[][] LATIN_R1 = {
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","z","e","r","t","y","u","i","o","p"},              // FR
            {"q","w","e","r","t","z","u","i","o","p"},              // DE
            {"q","w","e","r","t","y","u","i","o","p"},              // ES
            {"q","w","e","r","t","y","u","ı","o","p"}               // TR
    };
    private static final String[][] LATIN_R2 = {
            {"a","s","d","f","g","h","j","k","l"},
            {"q","s","d","f","g","h","j","k","l","m"},              // FR
            {"a","s","d","f","g","h","j","k","l","ö","ä"},          // DE
            {"a","s","d","f","g","h","j","k","l","ñ"},              // ES
            {"a","s","d","f","g","h","j","k","l","ş","i"}           // TR
    };
    private static final String[][] LATIN_R3 = {
            {"z","x","c","v","b","n","m"},
            {"w","x","c","v","b","n","é","è","ç"},                  // FR
            {"y","x","c","v","b","n","m","ü","ß"},                  // DE
            {"z","x","c","v","b","n","m","á","é","í"},              // ES
            {"z","x","c","v","b","n","m","ö","ç"}                   // TR
    };

    public static List<Row> latin(int lang, boolean numRow, boolean voice) {
        int idx = lang - LANG_EN; // 0..4
        if (idx < 0 || idx >= LATIN_R1.length) idx = 0;
        List<Row> rows = new ArrayList<>();
        if (numRow) rows.add(digitsRowEn());
        String[] r1 = LATIN_R1[idx], r2 = LATIN_R2[idx], r3 = LATIN_R3[idx];
        int n1 = r1.length;
        Key[] k1 = new Key[n1 + 1];
        for (int i = 0; i < n1; i++) k1[i] = up(c(r1[i], r1[i], 1));
        k1[n1] = back(1.4f);
        rows.add(row(k1));
        Key[] k2 = new Key[r2.length];
        for (int i = 0; i < r2.length; i++) k2[i] = up(c(r2[i], r2[i], 1));
        rows.add(row(k2));
        Key[] k3 = new Key[r3.length + 2];
        k3[0] = shift(1.4f);
        for (int i = 0; i < r3.length; i++) k3[i + 1] = up(c(r3[i], r3[i], 1));
        k3[r3.length + 1] = langKey(lang, 1.2f);
        rows.add(row(k3));
        rows.add(arBottom(lang, voice));
        return rows;
    }

    // ==================== الصف السفلي المشترك ====================

    private static Row arBottom(int lang, boolean voice) {
        String comma = lang == LANG_AR ? "،" : ",";
        if (voice) {
            return row(
                    emoji(1.0f),
                    voiceKey(),
                    f(lang == LANG_AR ? "؟١٢٣" : "?123", Key.CODE_MODE_NUM, 1.3f),
                    c(comma, comma, 0.9f).alt(lang == LANG_AR ? new String[]{"؛", "؟", "!"} : new String[]{"'", "\""}),
                    space(LANG_NAMES[lang], 3.1f),
                    c(".", ".", 0.9f).alt(lang == LANG_AR ? new String[]{"؟", "!", "؛", ":"} : new String[]{"!", "?", ";", ":"}),
                    enter(1.5f)
            );
        }
        return row(
                emoji(1.0f),
                f(lang == LANG_AR ? "؟١٢٣" : "?123", Key.CODE_MODE_NUM, 1.3f),
                c(comma, comma, 0.9f).alt(lang == LANG_AR ? new String[]{"؛", "؟", "!"} : new String[]{"'", "\""}),
                space(LANG_NAMES[lang], 3.6f),
                c(".", ".", 0.9f).alt(lang == LANG_AR ? new String[]{"؟", "!", "؛", ":"} : new String[]{"!", "?", ";", ":"}),
                enter(1.5f)
        );
    }

    private static Key voiceKey() { return f("", Key.CODE_VOICE, 0.8f); }

    // ==================== الأرقام والرموز ====================

    private static Row symBottom(int lang) {
        String backLabel = lang == LANG_AR ? "عربي" : "ABC";
        return row(
                f(backLabel, Key.CODE_BACK_TO_ABC, 1.6f),
                f("تحرير", Key.CODE_EDIT, 1.4f),
                space("١٢٣", 3.4f),
                c(".", ".", 0.9f).alt("؟", "!", "؛", ":"),
                enter(1.5f)
        );
    }

    public static List<Row> sym1(int lang) {
        List<Row> rows = new ArrayList<>();
        rows.add(row(
                c("1", "1", 1).alt("١"), c("2", "2", 1).alt("٢"),
                c("3", "3", 1).alt("٣"), c("4", "4", 1).alt("٤"),
                c("5", "5", 1).alt("٥"), c("6", "6", 1).alt("٦"),
                c("7", "7", 1).alt("٧"), c("8", "8", 1).alt("٨"),
                c("9", "9", 1).alt("٩"), c("0", "0", 1).alt("٠"),
                back(1.4f)
        ));
        rows.add(row(
                c("@", "@", 1), c("#", "#", 1), c("$", "$", 1),
                c("%", "%", 1), c("&", "&", 1), c("-", "-", 1),
                c("+", "+", 1), c("(", "(", 1), c(")", ")", 1),
                c("/", "/", 1)
        ));
        rows.add(row(
                f("=\\<", Key.CODE_PAGE2, 1.4f),
                c("*", "*", 1), c("\"", "\"", 1), c("'", "'", 1),
                c(":", ":", 1), c(";", ";", 1), c("!", "!", 1),
                c("؟", "؟", 1)
        ));
        rows.add(symBottom(lang));
        return rows;
    }

    public static List<Row> sym2(int lang) {
        List<Row> rows = new ArrayList<>();
        rows.add(row(
                c("~", "~", 1), c("`", "`", 1), c("|", "|", 1),
                c("•", "•", 1), c("√", "√", 1), c("π", "π", 1),
                c("÷", "÷", 1), c("×", "×", 1), c("¶", "¶", 1),
                c("∆", "∆", 1),
                back(1.4f)
        ));
        rows.add(row(
                c("£", "£", 1), c("€", "€", 1), c("¥", "¥", 1),
                c("¢", "¢", 1), c("^", "^", 1), c("°", "°", 1),
                c("=", "=", 1), c("{", "{", 1), c("}", "}", 1),
                c("\\", "\\", 1)
        ));
        rows.add(row(
                f("123", Key.CODE_PAGE1, 1.4f),
                c("%", "%", 1), c("©", "©", 1), c("®", "®", 1),
                c("™", "™", 1), c("✓", "✓", 1),
                c("[", "[", 1), c("]", "]", 1),
                c("<", "<", 1), c(">", ">", 1)
        ));
        rows.add(symBottom(lang));
        return rows;
    }

    // ==================== لوحة الأرقام الكاملة ====================

    public static List<Row> numpad(int lang, boolean arabicDigits) {
        String[] d = arabicDigits
                ? new String[]{"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"}
                : new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String backLabel = lang == LANG_AR ? "عربي" : "ABC";
        List<Row> rows = new ArrayList<>();
        rows.add(row(
                c(d[7], d[7], 1), c(d[8], d[8], 1), c(d[9], d[9], 1),
                c("÷", "÷", 1).alt("×", "−", "+", "="),
                back(1f)
        ));
        rows.add(row(
                c(d[4], d[4], 1), c(d[5], d[5], 1), c(d[6], d[6], 1),
                c("×", "×", 1), c("−", "−", 1)
        ));
        rows.add(row(
                c(d[1], d[1], 1), c(d[2], d[2], 1), c(d[3], d[3], 1),
                c("+", "+", 1), c("=", "=", 1)
        ));
        rows.add(row(
                f(backLabel, Key.CODE_BACK_TO_ABC, 1.4f),
                c(d[0], d[0], 1.2f),
                c(".", ".", 0.8f).alt(","),
                c("%", "%", 0.8f),
                enter(1.2f)
        ));
        return rows;
    }

    // ==================== لوحة تحرير النص ====================

    public static List<Row> edit(int lang) {
        List<Row> rows = new ArrayList<>();
        rows.add(row(
                f("تحديد الكل", Key.CODE_SEL_ALL, 2f),
                f("نسخ", Key.CODE_COPY, 1.2f),
                f("قص", Key.CODE_CUT, 1.2f),
                f("لصق", Key.CODE_PASTE, 1.2f)
        ));
        rows.add(row(
                f("⇤ بداية", Key.CODE_HOME, 1.5f),
                f("نهاية ⇥", Key.CODE_END, 1.5f),
                f("حذف كلمة", Key.CODE_DEL_WORD, 1.6f),
                back(1.2f)
        ));
        rows.add(row(
                f("↑", Key.CODE_ARR_U, 1f),
                f("↓", Key.CODE_ARR_D, 1f),
                f("عائم", Key.CODE_FLOAT, 1.3f),
                f("تم", Key.CODE_BACK_TO_ABC, 1.3f)
        ));
        rows.add(row(
                f("←", Key.CODE_ARR_L, 1.1f),
                space("اسحب المؤشر", 3.4f),
                f("→", Key.CODE_ARR_R, 1.1f),
                enter(1.4f)
        ));
        return rows;
    }

    // ==================== شريط الإيموجي ====================

    public static List<Row> emojiNav(int lang) {
        List<Row> rows = new ArrayList<>();
        String backLabel = lang == LANG_AR ? "عربي" : "ABC";
        rows.add(row(
                f(backLabel, Key.CODE_BACK_TO_ABC, 4f),
                back(1.3f)
        ));
        return rows;
    }

    // ==================== مجموعات الإيموجي ====================

    /** تصنيفات الإيموجي — الاسم ثم السلسلة */
    public static final String[][] EMOJI_GROUPS = {
        {"الأحدث", ""},
        {"الوجوه",
            "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 " +
            "😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 " +
            "🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 💀 🤡 " +
            "👻 👽 🤖 💩"},
        {"الإيماءات",
            "✋ 🤚 🖐 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 " +
            "🦾 🖕 ✍️ 💅 🤳 👋 🫶 👀 🧠 🦷 👅 👄 💋 🩸"},
        {"القلوب",
            "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 💌 💋 👩‍❤️‍👨 🌹 💐 " +
            "💍 👰 🤵 💒"},
        {"الحيوانات",
            "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 " +
            "🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🕷 🦂 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 " +
            "🐊 🐅 🐆 🦓 🦍 🐘 🦛 🐪 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🦙 🐐 🦌 🐕 🐩 🐈 🐓 🦃 🦚"},
        {"الطعام",
            "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🥦 🥕 🌽 🌶 🥒 🍞 🥐 " +
            "🥖 🥨 🧀 🥚 🍳 🧇 🥓 🍔 🍟 🍕 🌭 🥪 🌮 🌯 🍜 🍝 🍣 🍱 🍤 🍚 🍛 🍢 🍡 🍧 🍨 🍦 🥧 " +
            "🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 ☕ 🍵 🧃 🥤 🍺"},
        {"السفر",
            "🚗 🚕 🚙 🚌 🚎 🏎 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🛵 🏍 🚲 🛴 🚨 🚔 🚍 🚘 🚖 ✈️ 🛫 🛬 " +
            "🚀 🛸 🚁 ⛵ 🚤 🛳 ⛴ 🚢 ⚓ 🗺 🗿 🗽 🗼 🏰 🏯 🏟 🎡 🎢 🎠 ⛲ ⛱ 🏖 🏝 🏜 🌋 ⛰ 🏔 " +
            "🗻 🏕 ⛺ 🌅 🌄 🌇 🌆 🏙 🌃 🌌 🌉"},
        {"الأنشطة",
            "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🪀 🏓 🏸 🏒 🏑 🥍 🏏 🥊 🥋 🎽 🛹 🛼 🏆 🥇 🥈 🥉 " +
            "🏅 🎖 🎗 🎫 🎪 🤹 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🪕 🎻 🎲 ♟ 🎯 🎳 🎮 🎰 🧩"},
        {"الرموز",
            "🔥 ⭐ 🌟 ✨ 💥 💯 ✅ ❌ ❗ ❓ 💤 ⚡ ☀️ 🌙 ☁️ 🌈 ❄️ ☂️ 💧 🌊 ⏰ ⏳ 🕐 🔔 🎵 🎶 " +
            "📌 📎 🔒 🔓 🔑 🔫 💣 🛡 ⚔️ 🧭 ⚖️ 🔗 ⚙️ 🔍 💡 🔦 📱 💻 🖥 ⌨️ 🖱 📷 🎥 📞 ☎️ " +
            "🔋 💾 💿 📀 📼 📡"},
        {"الأعلام",
            "🇸🇦 🇦🇪 🇪🇬 🇶🇦 🇰🇼 🇯🇴 🇲🇦 🇩🇿 🇹🇳 🇱🇾 🇮🇶 🇸🇾 🇱🇧 🇵🇸 🇾🇪 🇴🇲 🇧🇭 🇸🇩 🇵🇰 🇹🇷 " +
            "🇺🇸 🇬🇧 🇫🇷 🇩🇪 🇪🇸 🇮🇹 🇧🇷 🇷🇺 🇨🇳 🇯🇵 🇰🇷 🇮🇳"}
    };

    public static String[] emojisOf(int group) {
        if (group <= 0 || group >= EMOJI_GROUPS.length) return new String[0];
        return EMOJI_GROUPS[group][1].trim().split("\\s+");
    }
}
