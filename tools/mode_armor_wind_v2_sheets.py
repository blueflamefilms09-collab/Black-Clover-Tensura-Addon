"""Composes preview sheets of the Wind Spirit Dive overlay v2 from the harness renders (scenes.d/mode_armor_wind_v2.json).

    python3 -B tools/mode_armor_wind_v2_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "mwv2_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/mode_armor_wind_v2_sheets.py [out-dir]           # default docs/previews/mode_armor_wind_v2

Every picture is a render of the repo's software harness (the real ModeArmorLayer and Spirit Dive painters), NOT a game screenshot: no bloom, additive passes approximated, the arms hang.
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "mode_armor_wind_v2.json")))
MARGIN, HEAD = 6, 30


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def tiles(scene):
    sc = SCENES[scene]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    w, h = sc["tile"]
    rows, cols = len(sc["views"]), len(sc["ages"])
    return {(r, c): im.crop((MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN), MARGIN + c * (w + MARGIN) + w, HEAD + MARGIN + r * (h + MARGIN) + h)) for r in range(rows) for c in range(cols)}


def label(im, text, xy=(6, 5), size=14):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


def row_of(scene, col=0, views=None):
    t = tiles(scene)
    sc = SCENES[scene]
    ims = []
    for r, v in enumerate(sc["views"]):
        if views and v["name"] not in views:
            continue
        im = t[(r, col)].copy()
        label(im, v["name"])
        ims.append(im)
    return ims


def hstack(ims, gap=4):
    W = sum(i.width for i in ims) + gap * (len(ims) - 1)
    out = Image.new("RGB", (W, max(i.height for i in ims)), (14, 14, 18))
    x = 0
    for i in ims:
        out.paste(i, (x, 0))
        x += i.width + gap
    return out


def vstack(ims, gap=6, title=None):
    W = max(i.width for i in ims)
    H = sum(i.height for i in ims) + gap * (len(ims) - 1) + (34 if title else 0)
    out = Image.new("RGB", (W, H), (14, 14, 18))
    y = 0
    if title:
        d = ImageDraw.Draw(out)
        d.text((8, 8), title, font=font(18), fill=(235, 255, 235))
        y = 34
    for i in ims:
        out.paste(i, (0, y))
        y += i.height + gap
    return out


def before_after(name, scene_after, scene_before, views=None, title=None):
    a, b = row_of(scene_after, views=views), row_of(scene_before, views=views)
    for im in b:
        label(im, "BEFORE", (6, 24))
    for im in a:
        label(im, "AFTER", (6, 24))
    return vstack([hstack(b), hstack(a)], title=title or name)


def entry(scene, title):
    t = tiles(scene)
    sc = SCENES[scene]
    ims = []
    for c, age in enumerate(sc["ages"]):
        im = t[(0, c)].copy()
        label(im, "t=%s" % age)
        ims.append(im)
    return vstack([hstack(ims)], title=title)


def main():
    out = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "previews", "mode_armor_wind_v2")
    os.makedirs(out, exist_ok=True)
    jobs = {
        "before_after_full.png": lambda: before_after("Wind Spirit Dive overlay, Yuno: classic vs v2", "mwv2_after_full", "mwv2_before_full"),
        "collar_closeup.png": lambda: before_after("Collar and shoulders", "mwv2_after_collar", "mwv2_before_collar"),
        "crown_closeup.png": lambda: before_after("Crown", "mwv2_after_crown", "mwv2_before_crown"),
        "wings_closeup.png": lambda: before_after("Wings", "mwv2_after_wings", "mwv2_before_wings"),
        "running_coat_tails.png": lambda: before_after("Running: the coat tails flare", "mwv2_after_run", "mwv2_before_run"),
        "entry_overlay.png": lambda: vstack([entry("mwv2_before_entry", "classic: entry"), entry("mwv2_after_entry", "v2: entry (wings unfold, crown ignites on the dive clock)")]),
        "yuno_dive_combined.png": lambda: before_after("Yuno's Spirit Dive: aura + overlay (sword, wrist circle and merge layer stay)", "mwv2_after_dive", "mwv2_before_dive"),
        "yuno_dive_big.png": lambda: before_after("Yuno's Spirit Dive, large", "mwv2_after_dive_big", "mwv2_before_dive_big"),
        "yuno_dive_entry.png": lambda: vstack([entry("mwv2_before_dive_entry", "before: aura + classic overlay"), entry("mwv2_after_dive_entry", "after: aura + overlay v2")]),
    }
    only = sys.argv[2:]
    for name, fn in jobs.items():
        if only and name not in only:
            continue
        try:
            im = fn()
        except FileNotFoundError as e:
            print("skip", name, e)
            continue
        im.save(os.path.join(out, name))
        print("wrote", os.path.join(out, name), im.size)


if __name__ == "__main__":
    main()
