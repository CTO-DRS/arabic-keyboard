package com.zai.arabickeyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * تخطيطات لوحة المفاتيح:
 * عربي (الترتيب القياسي 101) — إنجليزي QWERTY — أرقام — رموز — إيموجي
 */
public final class Layouts {

    private Layouts() {}

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

    /** يحوّل حرفاً لاتينياً إلى زر Shift بأحرف كبيرة */
    private static Key up(Key k) {
        if (k.text != null && k.text.length() == 1
                && Character.isLowerCase(k.text.charAt(0))) {
            String u = k.text.toUpperCase(Locale.ENGLISH);
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

    public static List<Row> get(int mode, int lang, boolean numRow) {
        switch (mode) {
            case ImeService.MODE_EN: return english(numRow);
            case ImeService.MODE_SYM1: return sym1(lang);
            case ImeService.MODE_SYM2: return sym2(lang);
            case ImeService.MODE_EMOJI: return emojiNav(lang);
            default: return arabic(numRow);
        }
    }

    /** صف الأرقام العربي (يعرض ١-٠ والضغط المطوّل يعطي 1-0) */
    private static Row digitsRowAr() {
        return row(
                c("١", "١", 1).alt("1"),
                c("٢", "٢", 1).alt("2"),
                c("٣", "٣", 1).alt("3"),
                c("٤", "٤", 1).alt("4"),
                c("٥", "٥", 1).alt("5"),
                c("٦", "٦", 1).alt("6"),
                c("٧", "٧", 1).alt("7"),
                c("٨", "٨", 1).alt("8"),
                c("٩", "٩", 1).alt("9"),
                c("٠", "٠", 1).alt("0")
        );
    }

    /** صف الأرقام الإنجليزي (الضغط المطوّل يعطي الرموز) */
    private static Row digitsRowEn() {
        return row(
                c("1", "1", 1).alt("!"),
                c("2", "2", 1).alt("@"),
                c("3", "3", 1).alt("#"),
                c("4", "4", 1).alt("$"),
                c("5", "5", 1).alt("%"),
                c("6", "6", 1).alt("^"),
                c("7", "7", 1).alt("&"),
                c("8", "8", 1).alt("*"),
                c("9", "9", 1).alt("("),
                c("0", "0", 1).alt(")")
        );
    }

    // ==================== العربية ====================

    public static List<Row> arabic(boolean numRow) {
        List<Row> rows = new ArrayList<>();
        if (numRow) rows.add(digitsRowAr());
        // الصف الأول + مسح
        rows.add(row(
                c("ض", "ض", 1).shift("َ", "َ"),
                c("ص", "ص", 1).shift("ً", "ً"),
                c("ث", "ث", 1).shift("ُ", "ُ"),
                c("ق", "ق", 1).shift("ٌ", "ٌ"),
                c("ف", "ف", 1).shift("لإ", "لإ"),
                c("غ", "غ", 1).shift("إ", "إ"),
                c("ع", "ع", 1).shift("ّ", "ّ"),
                c("ه", "ه", 1).shift("÷", "÷").alt("ة"),
                c("خ", "خ", 1).shift("×", "×"),
                c("ح", "ح", 1).shift("؛", "؛"),
                c("ج", "ج", 1).shift("<", "<"),
                c("د", "د", 1).shift(">", ">").alt("ذ"),
                back(1.4f)
        ));
        // الصف الثاني
        rows.add(row(
                c("ش", "ش", 1).shift("ِ", "ِ"),
                c("س", "س", 1).shift("~", "~"),
                c("ي", "ي", 1).shift("]", "]").alt("ئ"),
                c("ب", "ب", 1).shift("[", "["),
                c("ل", "ل", 1).shift("لأ", "لأ").alt("لآ"),
                c("ا", "ا", 1).shift("لآ", "لآ").alt("أ", "إ", "آ"),
                c("ت", "ت", 1).shift("ـ", "ـ"),
                c("ن", "ن", 1).shift("/", "/"),
                c("م", "م", 1).shift(":", ":"),
                c("ك", "ك", 1).shift("\"", "\""),
                c("ط", "ط", 1).shift("'", "'")
        ));
        // الصف الثالث + Shift + زر اللغة
        rows.add(row(
                shift(1.4f),
                c("ذ", "ذ", 1).shift("ٍ", "ٍ"),
                c("ئ", "ئ", 1),
                c("ء", "ء", 1).alt("ؤ", "ئ", "أ"),
                c("ؤ", "ؤ", 1),
                c("ر", "ر", 1),
                c("لا", "لا", 1).shift("لآ", "لآ"),
                c("ى", "ى", 1).shift("آ", "آ").alt("آ"),
                c("ة", "ة", 1).alt("ه"),
                c("و", "و", 1).shift("ؤ", "ؤ").alt("ؤ"),
                c("ز", "ز", 1),
                c("ظ", "ظ", 1),
                f("EN", Key.CODE_LANG, 1.2f)
        ));
        // الصف الرابع
        rows.add(row(
                emoji(1.0f),
                f("؟١٢٣", Key.CODE_MODE_NUM, 1.3f),
                c("،", "،", 0.9f).alt("؛", "؟", "!"),
                space("العربية", 3.6f),
                c(".", ".", 0.9f).alt("؟", "!", "؛", ":", "-"),
                enter(1.5f)
        ));
        return rows;
    }

    // ==================== الإنجليزية ====================

    public static List<Row> english(boolean numRow) {
        List<Row> rows = new ArrayList<>();
        if (numRow) rows.add(digitsRowEn());
        rows.add(row(
                up(c("q", "q", 1).alt("1")),
                up(c("w", "w", 1).alt("2")),
                up(c("e", "e", 1).alt("3")),
                up(c("r", "r", 1).alt("4")),
                up(c("t", "t", 1).alt("5")),
                up(c("y", "y", 1).alt("6")),
                up(c("u", "u", 1).alt("7")),
                up(c("i", "i", 1).alt("8")),
                up(c("o", "o", 1).alt("9")),
                up(c("p", "p", 1).alt("0")),
                back(1.4f)
        ));
        rows.add(row(
                up(c("a", "a", 1).alt("@")),
                up(c("s", "s", 1).alt("#")),
                up(c("d", "d", 1).alt("$")),
                up(c("f", "f", 1).alt("%")),
                up(c("g", "g", 1).alt("&")),
                up(c("h", "h", 1).alt("-")),
                up(c("j", "j", 1).alt("+")),
                up(c("k", "k", 1).alt("(")),
                up(c("l", "l", 1).alt(")"))
        ));
        rows.add(row(
                shift(1.4f),
                up(c("z", "z", 1).alt("*")),
                up(c("x", "x", 1).alt("\"")),
                up(c("c", "c", 1).alt("'")),
                up(c("v", "v", 1).alt(":")),
                up(c("b", "b", 1).alt(";")),
                up(c("n", "n", 1).alt("!")),
                up(c("m", "m", 1).alt("?")),
                f("ع", Key.CODE_LANG, 1.4f)
        ));
        rows.add(row(
                emoji(1.0f),
                f("?123", Key.CODE_MODE_NUM, 1.3f),
                c(",", ",", 0.9f).alt("'", "\""),
                space("English", 3.6f),
                c(".", ".", 0.9f).alt("!", "?", ";", ":"),
                enter(1.5f)
        ));
        return rows;
    }

    // ==================== الأرقام والرموز ====================

    private static Row symBottom(int lang) {
        String backLabel = (lang == ImeService.MODE_AR) ? "عربي" : "ABC";
        return row(
                f(backLabel, Key.CODE_BACK_TO_ABC, 1.6f),
                c("،", "،", 0.9f).alt("؛", "؟", "!"),
                space("١٢٣", 3.8f),
                c(".", ".", 0.9f).alt("؟", "!", "؛", ":"),
                enter(1.5f)
        );
    }

    public static List<Row> sym1(int lang) {
        List<Row> rows = new ArrayList<>();
        rows.add(row(
                c("1", "1", 1).alt("١"),
                c("2", "2", 1).alt("٢"),
                c("3", "3", 1).alt("٣"),
                c("4", "4", 1).alt("٤"),
                c("5", "5", 1).alt("٥"),
                c("6", "6", 1).alt("٦"),
                c("7", "7", 1).alt("٧"),
                c("8", "8", 1).alt("٨"),
                c("9", "9", 1).alt("٩"),
                c("0", "0", 1).alt("٠"),
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

    // ==================== شريط الإيموجي ====================

    public static List<Row> emojiNav(int lang) {
        List<Row> rows = new ArrayList<>();
        String backLabel = (lang == ImeService.MODE_AR) ? "عربي" : "ABC";
        rows.add(row(
                f(backLabel, Key.CODE_BACK_TO_ABC, 4f),
                back(1.3f)
        ));
        return rows;
    }

    // ==================== قائمة الإيموجي ====================

    private static final String EMOJI_STR =
        "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 " +
        "😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 " +
        "🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 💀 🤡 " +
        "👻 👽 🤖 💩 ✋ 🤚 🖐 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 " +
        "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 🔥 ⭐ 🌟 ✨ 💥 💯 ✅ ❌ ❗ ❓ 💤 " +
        "🎉 🎊 🎁 🏆 ⚽ 🏀 🌹 🌷 🌻 🌴 ☕ 🍰 🍕 🍫 🍎 🍉 🕌 🕋 📿 🌙 ☪️ " +
        "🇸🇦 🇦🇪 🇪🇬 🇶🇦 🇰🇼 🇯🇴 🇲🇦 🇩🇿 🇹🇳 🇱🇾 🇮🇶 🇸🇾 🇱🇧 🇵🇸 🇾🇪 🇴🇲 🇧🇭 🇸🇩 🇵🇰 🇹🇷";

    public static String[] emojis() {
        return EMOJI_STR.split("\\s+");
    }
}
