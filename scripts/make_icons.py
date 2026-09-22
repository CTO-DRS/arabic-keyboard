#!/usr/bin/env python3
"""توليد أيقونات تطبيق لوحة المفاتيح العربية (شبكة مفاتيح على خلفية متدرجة)."""
from PIL import Image, ImageDraw
import os

RES = "/home/z/my-project/arabic-keyboard/res"

SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def lerp(a, b, t):
    return int(a + (b - a) * t)


def draw_icon(size, safe_inset_ratio=0.0, rounded=True):
    """يرسم أيقونة: خلفية متدرجة + شبكة مفاتيح بيضاء + مفتاح مميز."""
    S = size * 2  # نرسم بدقة مضاعفة ثم نُصغّر للنعومة
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # تدرج أزرق -> بنفسجي
    top = (41, 98, 255)
    bottom = (108, 77, 246)
    for y in range(S):
        t = y / S
        d.line([(0, y), (S, y)],
               fill=(lerp(top[0], bottom[0], t),
                     lerp(top[1], bottom[1], t),
                     lerp(top[2], bottom[2], t), 255))

    if rounded:
        mask = Image.new("L", (S, S), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([0, 0, S, S], radius=int(S * 0.22), fill=255)
        img.putalpha(mask)

    # شبكة المفاتيح داخل المنطقة الآمنة
    inset = int(S * safe_inset_ratio)
    area = S - 2 * inset
    pad = area * 0.14
    grid = area - 2 * pad
    kr = grid * 0.045  # نصف قطر أركان المفاتيح

    rows = 4
    cols = 4
    gap = grid * 0.06
    kw = (grid - gap * (cols - 1)) / cols
    kh = (grid - gap * (rows - 1)) / rows

    y0 = inset + pad
    for r in range(rows):
        for c in range(cols):
            x0 = inset + pad + c * (kw + gap)
            yy = y0 + r * (kh + gap)
            # الصف الأخير: زر مسافة عريض في المنتصف
            if r == rows - 1:
                if c == 1 or c == 2:
                    continue
                if c == 0:
                    d.rounded_rectangle([x0, yy, x0 + kw * 2 + gap, yy + kh],
                                        radius=kr, fill=(255, 255, 255, 235))
                elif c == 3:
                    d.rounded_rectangle([x0, yy, x0 + kw, yy + kh],
                                        radius=kr, fill=(255, 213, 79, 255))
                continue
            color = (255, 255, 255, 235)
            if r == 0 and c == 3:
                color = (255, 213, 79, 255)
            d.rounded_rectangle([x0, yy, x0 + kw, yy + kh],
                                radius=kr, fill=color)
    return img.resize((size, size), Image.LANCZOS)


def main():
    # أيقونات الإطلاق العادية
    for dpi, size in SIZES.items():
        folder = os.path.join(RES, f"mipmap-{dpi}")
        os.makedirs(folder, exist_ok=True)
        draw_icon(size, rounded=True).save(os.path.join(folder, "ic_launcher.png"))
        print(f"ic_launcher.png ({dpi}) = {size}px")

    # مقدمة الأيقونة التكيفية (432px مع منطقة أمان)
    fg_folder = os.path.join(RES, "mipmap-xxxhdpi")
    os.makedirs(fg_folder, exist_ok=True)
    draw_icon(432, safe_inset_ratio=0.18, rounded=False).save(
        os.path.join(fg_folder, "ic_launcher_fg.png"))
    print("ic_launcher_fg.png (adaptive) = 432px")


if __name__ == "__main__":
    main()
