# -*- coding: utf-8 -*-
"""生成应用启动图标：蓝色背景 + 白色锁形（PNG 全密度）。"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

BLUE = (37, 99, 235, 255)
WHITE = (255, 255, 255, 255)

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_icon(size):
    scale = 8  # 超采样
    s = size * scale
    img = Image.new("RGBA", (s, s), BLUE)
    d = ImageDraw.Draw(img)

    def P(v):
        return int(round(v * s))

    # 锁身（圆角矩形）
    body = [P(0.32), P(0.50), P(0.68), P(0.80)]
    d.rounded_rectangle(body, radius=P(0.06), fill=WHITE)

    # 锁梁（圆弧 + 两端竖线）
    lw = max(1, P(0.055))
    cx, cy, r = P(0.50), P(0.50), P(0.14)
    # 顶部半圆（180 -> 360 度，PIL 角度顺时针，0 在 3 点钟方向）
    d.arc([cx - r, cy - r, cx + r, cy + r], start=180, end=360, fill=WHITE, width=lw)
    # 两端竖线
    d.line([cx - r, cy, cx - r, P(0.50)], fill=WHITE, width=lw)
    d.line([cx + r, cy, cx + r, P(0.50)], fill=WHITE, width=lw)

    # 锁孔
    kx, ky, kr = P(0.50), P(0.66), P(0.045)
    d.ellipse([kx - kr, ky - kr, kx + kr, ky + kr], fill=BLUE)
    d.rectangle([kx - P(0.018), ky, kx + P(0.018), P(0.74)], fill=BLUE)

    img = img.resize((size, size), Image.LANCZOS)
    return img


def main():
    for folder, size in SIZES.items():
        out_dir = os.path.join(RES, folder)
        os.makedirs(out_dir, exist_ok=True)
        icon = draw_icon(size)
        icon.save(os.path.join(out_dir, "ic_launcher.png"))
        icon.save(os.path.join(out_dir, "ic_launcher_round.png"))
        print("wrote", folder, size)


if __name__ == "__main__":
    main()