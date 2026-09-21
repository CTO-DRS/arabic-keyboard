#!/usr/bin/env python3
"""توليد صور معاينة للوحة المفاتيح (داكنة عربية + فاتحة إنجليزية) للتوثيق."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT_DIR = "/home/z/my-project/arabic-keyboard/docs"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

W = 1080
KEY_H = 104
GAP = 10
SIDE = 14
TOP = 18
BOTTOM = 22
RADIUS = 20

DARK = dict(bg="#1B1D21", key="#35383D", func="#26282C", action="#4285F4",
            text="#E8EAED", tfunc="#DADCE0", taction="#FFFFFF", hint="#9AA0A6")
LIGHT = dict(bg="#E9EBEF", key="#FFFFFF", func="#D3D7DE", action="#1A73E8",
             text="#1F2430", tfunc="#3C4043", taction="#FFFFFF", hint="#80868B")

# ---------- بيانات التخطيط: (label, kind, weight) ----------
AR_R1 = [("ض","c",1),("ص","c",1),("ث","c",1),("ق","c",1),("ف","c",1),("غ","c",1),
         ("ع","c",1),("ه","c",1),("خ","c",1),("ح","c",1),("ج","c",1),("د","c",1),
         ("⌫","back",1.4)]
AR_R2 = [("ش","c",1),("س","c",1),("ي","c",1),("ب","c",1),("ل","c",1),("ا","c",1),
         ("ت","c",1),("ن","c",1),("م","c",1),("ك","c",1),("ط","c",1)]
AR_R3 = [("⇧","shift",1.4),("ذ","c",1),("ئ","c",1),("ء","c",1),("ؤ","c",1),("ر","c",1),
         ("لا","c",1),("ى","c",1),("ة","c",1),("و","c",1),("ز","c",1),("ظ","c",1),
         ("EN","lang",1.2)]
AR_R4 = [("😊","emoji",1.0),("؟١٢٣","mode",1.3),("،","c",0.9),("العربية","space",3.6),
         (".","c",0.9),("⏎","enter",1.5)]

EN_R1 = [("q","c",1),("w","c",1),("e","c",1),("r","c",1),("t","c",1),("y","c",1),
         ("u","c",1),("i","c",1),("o","c",1),("p","c",1),("⌫","back",1.4)]
EN_R2 = [("a","c",1),("s","c",1),("d","c",1),("f","c",1),("g","c",1),("h","c",1),
         ("j","c",1),("k","c",1),("l","c",1)]
EN_R3 = [("⇧","shift",1.4),("z","c",1),("x","c",1),("c","c",1),("v","c",1),
         ("b","c",1),("n","c",1),("m","c",1),("ع","lang",1.4)]
EN_R4 = [("😊","emoji",1.0),("?123","mode",1.3),(",","c",0.9),("English","space",3.6),
         (".","c",0.9),("⏎","enter",1.5)]


def draw_kb(rows, th, path, label_font_size=46, small_font_size=30):
    n_rows = len(rows)
    H = TOP + BOTTOM + n_rows * KEY_H + (n_rows - 1) * GAP
    img = Image.new("RGBA", (W, H), th["bg"])
    d = ImageDraw.Draw(img)
    f_big = ImageFont.truetype(FONT_BOLD, label_font_size)
    f_small = ImageFont.truetype(FONT_BOLD, small_font_size)

    y = TOP
    for row in rows:
        total = sum(k[2] for k in row)
        avail = W - 2 * SIDE - (len(row) - 1) * GAP
        unit = avail / total
        x = SIDE
        for (label, kind, wgt) in row:
            kw = unit * wgt
            if kind in ("back", "shift", "mode", "lang"):
                fill = th["func"]
            elif kind in ("enter",):
                fill = th["action"]
            elif kind == "emoji":
                fill = th["func"]
            else:
                fill = th["key"]
            d.rounded_rectangle([x, y, x + kw, y + KEY_H], radius=RADIUS, fill=fill)

            if kind == "textcolor_free":
                pass

            # النص
            if kind == "back":
                draw_backspace(d, x, y, kw, KEY_H, th["tfunc"])
            elif kind == "shift":
                draw_shift(d, x, y, kw, KEY_H, th["tfunc"])
            elif kind == "enter":
                d.text((x + kw / 2, y + KEY_H / 2), "بحث", font=f_small,
                       fill=th["taction"], anchor="mm")
            elif kind == "emoji":
                d.text((x + kw / 2, y + KEY_H / 2), label, font=f_big,
                       fill=th["tfunc"], anchor="mm")
            elif kind == "lang":
                d.text((x + kw / 2, y + KEY_H / 2), label, font=f_small,
                       fill=th["tfunc"], anchor="mm")
            elif kind == "mode":
                d.text((x + kw / 2, y + KEY_H / 2), label, font=f_small,
                       fill=th["tfunc"], anchor="mm")
            elif kind == "space":
                d.text((x + kw / 2, y + KEY_H / 2), label,
                       font=ImageFont.truetype(FONT, 26), fill=th["hint"], anchor="mm")
            else:
                d.text((x + kw / 2, y + KEY_H / 2), label, font=f_big,
                       fill=th["text"], anchor="mm")
            x += kw + GAP
        y += KEY_H + GAP
    img.save(path)
    print("تم:", path)


def draw_backspace(d, x, y, kw, kh, color):
    cx, cy = x + kw / 2, y + kh / 2
    w2, h2 = kw * 0.17, kh * 0.15
    pts = [(cx - w2, cy), (cx - w2 * 0.55, cy - h2), (cx + w2, cy - h2),
           (cx + w2, cy + h2), (cx - w2 * 0.55, cy + h2)]
    d.polygon(pts, fill=color)
    d.line([(cx - w2 * 0.05, cy - h2 * 0.4), (cx + w2 * 0.5, cy + h2 * 0.4)],
           fill=th_bg_for_backspace(color), width=5)
    d.line([(cx - w2 * 0.05, cy + h2 * 0.4), (cx + w2 * 0.5, cy - h2 * 0.4)],
           fill=th_bg_for_backspace(color), width=5)


def th_bg_for_backspace(color):
    # لون X داخل أيقونة المسح = خلفية الزر تقريباً
    return "#26282C"


def draw_shift(d, x, y, kw, kh, color):
    cx, cy = x + kw / 2, y + kh / 2
    s = kh * 0.16
    pts = [(cx, cy - s * 1.15), (cx + s, cy), (cx + s * 0.45, cy),
           (cx + s * 0.45, cy + s * 0.8), (cx - s * 0.45, cy + s * 0.8),
           (cx - s * 0.45, cy), (cx - s, cy)]
    d.polygon(pts, outline=color, width=5)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    draw_kb([AR_R1, AR_R2, AR_R3, AR_R4], DARK,
            os.path.join(OUT_DIR, "preview-arabic-dark.png"))
    draw_kb([EN_R1, EN_R2, EN_R3, EN_R4], LIGHT,
            os.path.join(OUT_DIR, "preview-english-light.png"))


if __name__ == "__main__":
    main()
