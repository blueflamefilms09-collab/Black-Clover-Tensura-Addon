"""Composes the preview sheets of merge 2 from the scene renders (tools/render_preview, scenes.d/spirit_dive_merge2.json) into docs/previews/spirit_dive_merge2/.

    python3 -B tools/spirit_dive_merge2_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py <scene names>          # the scene names of the json
    python3 -B tools/spirit_dive_merge2_sheets.py <dir> [<dir> ...]                              # the build folders that hold the renders

Sheets (every picture is a render of the repo's software harness, NOT a game screenshot: no bloom, additive passes approximated):
  merge_<element>.png   per tier: the merge alone, SLIM spirit (top) against the BOXY one of the first merge (bottom), ticks 2 .. 18
  entry_<element>.png   per tier: the full look entry, merge 2 (top: slim spirit, calm entry) against everything switched back (bottom), ticks 0 .. 24
  fp_<element>.png      the first person view of the merge, per tier, ticks 2 .. 18
  crowns_<element>_<tier>.png   the crown from the front, three quarters and the side: BEFORE (left) and AFTER (right), on a dark and on a bright background
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_merge2")
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_merge2.json")))
DIRS = sys.argv[1:]
MARGIN, HEAD = 6, 30
CHAR = {"wind": "yuno", "fire": "fuego", "water": "noelle"}
TIER = {"std": "standard", "yuno": "Yuno (Sylph)", "fuego": "Fuegoleon (Salamander)", "noelle": "Noelle (Undine)"}


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def find(scene):
    for d in DIRS:
        p = os.path.join(d, scene + ".png")
        if os.path.exists(p):
            return p
    raise FileNotFoundError(scene)


def tiles(scene):
    sc = SCENES[scene]
    im = Image.open(find(scene)).convert("RGB")
    w, h = sc["tile"]
    return [[im.crop((MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN), MARGIN + c * (w + MARGIN) + w, HEAD + MARGIN + r * (h + MARGIN) + h)) for c in range(len(sc["ages"]))] for r in range(len(sc["views"]))]


def label(im, text, xy=(6, 5), size=13):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


def strip(scene, row=0, title=None, height=300):
    sc = SCENES[scene]
    out = []
    for i, im in enumerate(tiles(scene)[row]):
        im = im.resize((round(im.width * height / im.height), height), Image.LANCZOS)
        label(im, "tick %d" % sc["ages"][i])
        out.append(im)
    if title:
        label(out[0], title, xy=(6, height - 24))
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


def tiers(el):
    return ["std"] + ([CHAR[el]] if el in CHAR else [])


def main():
    os.makedirs(OUT, exist_ok=True)
    for el in ("wind", "water", "fire", "earth"):
        rows_m, rows_e, rows_f = [], [], []
        for g in tiers(el):
            rows_m += [strip("sdm2_%s_%s_solo_slim" % (el, g), title="%s: SLIM (merge 2)" % TIER[g]), strip("sdm2_%s_%s_solo_boxy" % (el, g), title="%s: BOXY (first merge)" % TIER[g])]
            rows_e += [strip("sdm2_%s_%s_entry_new" % (el, g), height=380, title="%s: merge 2" % TIER[g]), strip("sdm2_%s_%s_entry_old" % (el, g), height=380, title="%s: before (loud entry, boxy)" % TIER[g])]
            rows_f += [strip("sdm2_%s_%s_fp" % (el, g), height=190, title="%s: first person" % TIER[g])]
        grid(rows_m).save(os.path.join(OUT, "merge_%s.png" % el))
        grid(rows_e).save(os.path.join(OUT, "entry_%s.png" % el))
        grid(rows_f).save(os.path.join(OUT, "fp_%s.png" % el))
        for g in tiers(el):
            rows = []
            for bg in ("dark", "bright"):
                old, new = tiles("sdm2_%s_%s_crown_old_%s" % (el, g, bg))[0], tiles("sdm2_%s_%s_crown_new_%s" % (el, g, bg))[0]
                for i, im in enumerate(old):
                    label(im, "BEFORE" if i == 0 else ["", "3/4", "side"][i])
                for i, im in enumerate(new):
                    label(im, "AFTER" if i == 0 else ["", "3/4", "side"][i])
                rows.append(old + new)
            grid(rows).save(os.path.join(OUT, "crowns_%s_%s.png" % (el, g)))
    print("sheets in", OUT)


if __name__ == "__main__":
    main()
