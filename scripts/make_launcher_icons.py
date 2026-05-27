"""
make_launcher_icons.py — generate adaptive-icon-compatible PNG mipmaps from
the bundled Hanuman illustration.

For API 26+ devices the adaptive icon (drawable/ic_launcher_background.xml
+ drawable/ic_launcher_foreground.xml referenced from
mipmap-anydpi-v26/ic_launcher.xml) is used directly. For pre-API-26 devices
(minSdk 24/25), the legacy mipmap-DENSITY/ic_launcher.png is the only thing
the launcher sees, so we render a flat fallback here.

Output for each density:
    - ic_launcher.png          square saffron tile with the hero figure inset
    - ic_launcher_round.png    same image, circular-cropped

Densities (Android baseline):
    mdpi    1.0x    48x48
    hdpi    1.5x    72x72
    xhdpi   2.0x    96x96
    xxhdpi  3.0x    144x144
    xxxhdpi 4.0x    192x192
"""

from __future__ import annotations

import os
from pathlib import Path
from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parents[1]
HERO = REPO / "app/src/main/res/drawable/hero_hanuman.png"
RES = REPO / "app/src/main/res"

# Saffron gradient stops (top-left, bottom-right).
SAFFRON_TOP = (232, 119, 34)   # #E87722
SAFFRON_BOT = (200, 65, 42)    # #C8412A

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Foreground safe-zone inset (matches the 20% inset in the adaptive
# icon's foreground drawable).
INSET = 0.20


def gradient_tile(size: int) -> Image.Image:
    """Saffron diagonal gradient background, square."""
    bg = Image.new("RGB", (size, size), SAFFRON_TOP)
    px = bg.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size - 2)
            r = int(SAFFRON_TOP[0] * (1 - t) + SAFFRON_BOT[0] * t)
            g = int(SAFFRON_TOP[1] * (1 - t) + SAFFRON_BOT[1] * t)
            b = int(SAFFRON_TOP[2] * (1 - t) + SAFFRON_BOT[2] * t)
            px[x, y] = (r, g, b)
    return bg


def composite_icon(hero: Image.Image, size: int) -> Image.Image:
    """Saffron tile + inset hero, returns RGBA."""
    bg = gradient_tile(size).convert("RGBA")
    inset_px = int(size * INSET)
    inner = size - 2 * inset_px
    fg = hero.copy()
    fg.thumbnail((inner, inner), Image.LANCZOS)
    # Center-align the resized hero in the icon.
    offset = ((size - fg.size[0]) // 2, (size - fg.size[1]) // 2)
    bg.alpha_composite(fg, dest=offset)
    return bg


def circular_crop(img: Image.Image) -> Image.Image:
    """Crop a circular mask matching the launcher's round-icon shape."""
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, img.size[0] - 1, img.size[1] - 1), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main() -> None:
    if not HERO.exists():
        raise SystemExit(f"hero source missing: {HERO}")
    hero = Image.open(HERO).convert("RGBA")
    for name, size in DENSITIES.items():
        out_dir = RES / f"mipmap-{name}"
        out_dir.mkdir(exist_ok=True)
        icon = composite_icon(hero, size)
        icon.save(out_dir / "ic_launcher.png", "PNG", optimize=True)
        circular_crop(icon).save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)
        print(f"wrote {name}: {size}x{size}")

    # Adaptive-icon foreground: 432x432 (4x baseline of 108dp) so the
    # framework can downscale crisply on every density. The hero is
    # inset by 20% to stay within the safe zone of round / squircle masks.
    fg_size = 432
    fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    inset_px = int(fg_size * INSET)
    inner = fg_size - 2 * inset_px
    h = hero.copy()
    h.thumbnail((inner, inner), Image.LANCZOS)
    offset = ((fg_size - h.size[0]) // 2, (fg_size - h.size[1]) // 2)
    fg.alpha_composite(h, dest=offset)
    fg_path = RES / "drawable" / "ic_launcher_foreground.png"
    fg.save(fg_path, "PNG", optimize=True)
    print(f"wrote foreground: {fg_path.name} ({fg_size}x{fg_size})")


if __name__ == "__main__":
    main()
