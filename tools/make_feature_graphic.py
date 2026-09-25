#!/usr/bin/env python3
"""Aktualisiert die Tagline auf dem Play-Store-Feature-Graphic.

Der alte Text ("No ads / No tracking / No permissions") stimmt seit dem
Werbe-Update nicht mehr. Das Icon und der Titel ("Display Torch") bleiben
unveraendert -- nur die Tagline-Zeile wird ausgeschnitten und neu gezeichnet,
mit Schriftgroesse passend zur jeweiligen Sprache.

Quelle/Ziel: fastlane/metadata/android/<locale>/images/featureGraphic.jpg
(1024x500, wird in place ueberschrieben).

Aufruf: python3 tools/make_feature_graphic.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parent.parent
BASE = REPO / "fastlane/metadata/android"

BRAND = (0x8A, 0x6B, 0xE2)
WHITE = (255, 255, 255)

FONT_PATH = "/System/Library/Fonts/HelveticaNeue.ttc"
FONT_INDEX = 1  # Bold

# Tagline-Flaeche: rechts neben dem Icon, unterhalb des Titels "Display Torch".
BOX = (450, 300, 980, 410)
TEXT_LEFT = 472
TEXT_TOP = 312

TAGLINES = {
    "de-DE": "Kostenlos · Werbefinanziert\nWerbung per Kauf entfernbar",
    "en-US": "Free · Ad-supported\nRemove ads with a purchase",
}


def fit_font(lines, max_w, start=34):
    size = start
    while size > 16:
        font = ImageFont.truetype(FONT_PATH, size, index=FONT_INDEX)
        widest = max(font.getbbox(line)[2] - font.getbbox(line)[0] for line in lines)
        if widest <= max_w:
            return font, int(size * 1.28)
        size -= 1
    return ImageFont.truetype(FONT_PATH, 16, index=FONT_INDEX), 20


def build(locale, tagline):
    path = BASE / locale / "images/featureGraphic.jpg"
    img = Image.open(path).convert("RGB")
    draw = ImageDraw.Draw(img)

    draw.rectangle(BOX, fill=BRAND)

    lines = tagline.split("\n")
    font, line_h = fit_font(lines, BOX[2] - TEXT_LEFT)
    y = TEXT_TOP
    for line in lines:
        draw.text((TEXT_LEFT, y), line, font=font, fill=WHITE)
        y += line_h

    img.save(path, quality=92)
    print(f"{locale}: '{tagline}'")


if __name__ == "__main__":
    for locale, tagline in TAGLINES.items():
        build(locale, tagline)
