#!/usr/bin/env python3
"""Baut die Play-Store-Screenshots aus den Rohaufnahmen.

Quelle:    tools/screenshots-raw/<locale>/1..5.jpg   (unveraenderte Geraeteaufnahmen, 1344x2688)
Ergebnis:  fastlane/metadata/android/<locale>/images/phoneScreenshots/1..5.jpg

Jede Tafel besteht aus der Markenflaeche, einer Aussage und der Rohaufnahme
in einem Geraeterahmen. Farben und Schrift folgen dem Feature Graphic.

Neue Aufnahmen: Release-Build auf dem Geraet installieren, App-Sprache per
`adb shell cmd locale set-app-locales <pkg> --locales <tag>` setzen, mit
`adb exec-out screencap -p` aufnehmen und auf 1344x2688 beschneiden
(200 px Versatz von oben entfernt Status- und Navigationsleiste).

Aufruf: python3 tools/make_tiles.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parent.parent
RAW = REPO / "tools/screenshots-raw"
OUT = REPO / "fastlane/metadata/android"

W, H = 1344, 2688
BRAND = (0x8A, 0x6B, 0xE2)  # Lila des Feature Graphic
INK = (0x0E, 0x1F, 0x1B)

CAP_TOP, CAP_BOTTOM = 200, 700
FRAME_W, BORDER, RADIUS = 908, 16, 76

FONT_PATH = "/System/Library/Fonts/HelveticaNeue.ttc"
FONT_INDEX = 1  # Bold

# (Rohaufnahme, Aussage de-DE, Aussage en-US)
TILES = [
    ("2.jpg", "Dein Bildschirm\nwird zur Lampe", "Your screen\nbecomes the light"),
    ("1.jpg", "Fünf Helligkeits-\nstufen — antippen", "Five brightness\nsteps — just tap"),
    ("5.jpg", "Rotlicht schont\ndie Nachtsicht", "Red light preserves\nnight vision"),
    ("3.jpg", "Jede Stufe\nfein justierbar", "Fine-tune\nevery step"),
    ("4.jpg", "Keine Berechtigungen.\nKeine Werbung.\nKein Tracking.",
              "No permissions.\nNo ads.\nNo tracking."),
]

LOCALES = {"de-DE": 1, "en-US": 2}  # Locale -> Index der Aussage in TILES


def fit_font(lines, max_w, max_h, start=120):
    """Groesste Schriftgroesse, bei der der Textblock in die Caption-Flaeche passt."""
    size = start
    while size > 30:
        font = ImageFont.truetype(FONT_PATH, size, index=FONT_INDEX)
        line_h = int(size * 1.16)
        widest = max(font.getbbox(line)[2] - font.getbbox(line)[0] for line in lines)
        if widest <= max_w and line_h * len(lines) <= max_h:
            return font, line_h
        size -= 2
    return ImageFont.truetype(FONT_PATH, 30, index=FONT_INDEX), 36


def rounded_shot(path, w, h, radius):
    """Rohaufnahme auf Zielgroesse bringen und Ecken abrunden."""
    img = Image.open(path).convert("RGB").resize((w, h), Image.LANCZOS)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=255)
    out = Image.new("RGBA", (w, h))
    out.paste(img, (0, 0), mask)
    return out


def build(locale, caption_index):
    dst = OUT / locale / "images/phoneScreenshots"
    dst.mkdir(parents=True, exist_ok=True)

    for n, tile in enumerate(TILES, start=1):
        canvas = Image.new("RGB", (W, H), BRAND)
        draw = ImageDraw.Draw(canvas)

        lines = tile[caption_index].split("\n")
        font, line_h = fit_font(lines, W - 200, CAP_BOTTOM - CAP_TOP)

        y = CAP_TOP + (CAP_BOTTOM - CAP_TOP - line_h * len(lines)) // 2
        for line in lines:
            bbox = font.getbbox(line)
            draw.text(((W - (bbox[2] - bbox[0])) // 2 - bbox[0], y), line, font=font, fill=INK)
            y += line_h

        inner_w = FRAME_W - 2 * BORDER
        inner_h = inner_w * 2  # Rohaufnahmen sind exakt 1:2
        fx, fy = (W - FRAME_W) // 2, CAP_BOTTOM + 90
        draw.rounded_rectangle(
            [fx, fy, fx + FRAME_W, fy + inner_h + 2 * BORDER], radius=RADIUS, fill=INK
        )
        shot = rounded_shot(RAW / locale / tile[0], inner_w, inner_h, RADIUS - BORDER)
        canvas.paste(shot, (fx + BORDER, fy + BORDER), shot)

        canvas.save(dst / f"{n}.jpg", quality=92)
    print(f"{locale}: {len(TILES)} Tafeln")


if __name__ == "__main__":
    for locale, caption_index in LOCALES.items():
        build(locale, caption_index)
