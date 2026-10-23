"""Composes the preview sheets of the Spirit Dive MERGE from the scene renders (tools/render_preview, scenes.d/spirit_dive_merge.json) into docs/previews/spirit_dive_merge/.

    python3 -B tools/spirit_dive_merge_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdm_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_merge_sheets.py [<element> ...]

One sheet per element (`merge_<element>.png`): per tier a strip of the entry (ticks 0, 4, 8, 12, 14, 16, 20, 24 and the steady state 60) and a strip of the exit (ticks 100 to 119, the same clock
backwards), then the steady state WITH the merge next to the same dive WITHOUT it (`spiritDive.merge=false`), front and back. Every picture is a render of the repo's software harness, NOT a game screenshot
(no bloom, additive passes approximated).
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_merge")
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_merge.json")))
MARGIN, HEAD = 6, 30
CHAR = {"wind": "yuno", "fire": "fuego", "water": "noelle"}
TIER = {"std": "standard", "yuno": "Yuno (Sylph)", "fuego": "Fuegoleon (Salamander)", "noelle": "Noelle (Undine)"}
SPIRIT = {"wind": "Sylph", "water": "Undine", "fire": "Salamander", "earth": "Gnome"}


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def tiles(scene):
    sc = SCENES[scene]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    w, h = sc["tile"]
    out = []
    for r in range(len(sc["views"])):
        row = []
        for c in range(len(sc["ages"])):
            ox, oy = MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN)
            row.append(im.crop((ox, oy, ox + w, oy + h)))
        out.append(row)
    return out


def label(im, text, xy=(6, 5), size=14):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


def strip(scene, row=0, title=None, height=330):
    sc = SCENES[scene]
    ims = tiles(scene)[row]
    out = []
    for i, im in enumerate(ims):
        im = im.resize((round(im.width * height / im.height), height), Image.LANCZOS)
        label(im, "tick %d" % sc["ages"][i] if sc["ages"][i] < 100 else "exit %d" % (sc["ages"][i] - 100), size=13)
        out.append(im)
    if title:
        label(out[0], title, xy=(6, height - 26), size=13)
    return out


def grid(rows, bg=(20, 21, 28), gap=4):
    w = max(sum(i.width for i in r) + gap * (len(r) - 1) for r in rows)
    h = sum(max(i.height for i in r) for r in rows) + gap * (len(rows) - 1)
    sheet = Image.new("RGB", (w, h), bg)
    y = 0
    for r in rows:
        x = 0
        for i in r:
            sheet.paste(i, (x, y))
            x += i.width + gap
        y += max(i.height for i in r) + gap
    return sheet


def element_sheet(el):
    rows = []
    for g in ["std"] + ([CHAR[el]] if el in CHAR else []):
        rows.append(strip("sdm_%s_%s_entry" % (el, g), title="%s entry, %s tier" % (SPIRIT[el], TIER[g])))
        rows.append(strip("sdm_%s_%s_exit" % (el, g), title="exit (the clock backwards), %s tier" % TIER[g]))
    for g in ["std"] + ([CHAR[el]] if el in CHAR else []):
        on = tiles("sdm_%s_%s_steady" % (el, g))
        off = tiles("sdm_%s_%s_off" % (el, g))
        row = []
        for im, t in ((on[0][0], "steady, merge ON, %s" % TIER[g]), (off[0][0], "steady, merge OFF"), (on[1][0], "steady back, merge ON")):
            im = im.resize((round(im.width * 420 / im.height), 420), Image.LANCZOS)
            label(im, t, size=13)
            row.append(im)
        rows.append(row)
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "merge_%s.png" % el)
    grid(rows).save(path)
    print(path)
    srows = []
    for g in ["std"] + ([CHAR[el]] if el in CHAR else []):                                     # the merge alone: no look under it
        srows.append(strip("sdm_%s_%s_soloA" % (el, g), title="%s merge alone, %s tier: appears, begins to flow" % (SPIRIT[el], TIER[g]), height=380))
        srows.append(strip("sdm_%s_%s_soloB" % (el, g), title="contact (tick 14.5), the glow, the silhouette within", height=380))
        srows.append(strip("sdm_%s_%s_soloX" % (el, g), title="exit: the spirit leaves the chest and fades", height=380))
    path = os.path.join(OUT, "solo_%s.png" % el)
    grid(srows).save(path)
    print(path)


def main():
    els = sys.argv[1:] or ["wind", "water", "fire", "earth"]
    for el in els:
        element_sheet(el)


if __name__ == "__main__":
    main()
