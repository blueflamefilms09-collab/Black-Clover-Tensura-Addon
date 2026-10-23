"""Composes the preview sheets of the Wind remake from the scene renders (tools/render_preview, scenes.d/spirit_dive_wind3.json) into docs/previews/spirit_dive_wind3/.

    python3 -B tools/spirit_dive_wind3_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py <every scene name of the json>
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_wind3_sheets.py [<reference dir with the pack and the clips>]

Every picture is a render of the repo's software harness, NOT a game screenshot (no bloom, additive passes approximated).
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind3")
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind3.json")))
REFS = sys.argv[1] if len(sys.argv) > 1 else None
NAMES = {"a": "A  Anime-true", "b": "B  Clip-style", "c": "C  Saint Spirit"}
MARGIN, HEAD = 6, 30


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def tiles(scene):
    """The tiles of a scene render as {(row, col): image}."""
    sc = SCENES[scene]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    w, h = sc["tile"]
    out = {}
    for r in range(len(sc["views"])):
        for c in range(len(sc["ages"])):
            ox, oy = MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN)
            out[(r, c)] = im.crop((ox, oy, ox + w, oy + h))
    return out


def fit(im, h):
    return im.resize((max(1, round(im.width * h / im.height)), h), Image.LANCZOS)


def label(im, text, xy=(6, 5), size=14):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


def grid(rows, bg=(20, 21, 28), gap=4):
    """rows: list of lists of images (rows are centred on the widest)."""
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


def versions(grade, title):
    cols = []
    for v in "abc":
        t = tiles("sdw3_%s_%s_full" % (v, grade))
        col = []
        for r, nm in enumerate(("front", "front 3/4", "back 3/4")):
            im = fit(t[(r, 0)], 430)
            label(im, NAMES[v] + "  /  " + nm if r == 0 else nm)
            col.append(im)
        cols.append(col)
    rows = [[cols[c][r] for c in range(3)] for r in range(3)]
    sheet = grid(rows)
    head = Image.new("RGB", (sheet.width, 30), (20, 21, 28))
    ImageDraw.Draw(head).text((8, 7), title, font=font(14), fill=(235, 240, 235))
    out = grid([[head], [sheet]], gap=0)
    out.save(os.path.join(OUT, "versions_%s.png" % grade))


def strip(grade, kind, picks, h=300):
    rows = []
    for v in "abc":
        t = tiles("sdw3_%s_%s_%s" % (v, grade, kind))
        row = []
        for i, c in enumerate(picks):
            im = fit(t[(0, c)], h)
            if i == 0:
                label(im, NAMES[v])
            row.append(im)
        rows.append(row)
    return grid(rows)


def reference(v, ours, refs, name):
    h = 520
    row = [fit(ours, h)]
    for p, cap in refs:
        if p and os.path.isfile(p):
            im = fit(Image.open(p).convert("RGB"), h)
            label(im, cap, size=12)
            row.append(im)
    label(row[0], "ours: " + NAMES[v], size=12)
    grid([row]).save(os.path.join(OUT, name))


def main():
    os.makedirs(OUT, exist_ok=True)
    versions("yuno", "WIND Spirit Dive remake, Yuno tier, full stage: A | B | C (software render of tools/render_preview, not a game screenshot)")
    versions("std", "WIND Spirit Dive remake, standard Wind user, full stage: A | B | C (software render, not a game screenshot)")
    # the weapons in place
    rows = []
    for v in "abc":
        t = tiles("sdw3_%s_yuno_weapon" % v)
        r = []
        for c, nm in enumerate(("weapon, 3/4", "weapon, front")):
            im = fit(t[(c, 0)], 360)
            label(im, NAMES[v] + "  " + nm, size=12)
            r.append(im)
        rows.append(r)
    grid([[rows[0][0], rows[1][0], rows[2][0]], [rows[0][1], rows[1][1], rows[2][1]]]).save(os.path.join(OUT, "weapons_in_place.png"))
    # the entry, the cast, the first dive
    strip("yuno", "entry", [0, 2, 4, 6, 8, 10, 12]).save(os.path.join(OUT, "entry_yuno.png"))
    strip("std", "entry", [0, 2, 4, 6, 8, 10, 12]).save(os.path.join(OUT, "entry_std.png"))
    rows = []
    for v in "abc":
        t = tiles("sdw3_%s_yuno_cast" % v)
        r = []
        for c in range(3):
            im = fit(t[(c, 0)], 380)
            if c == 0:
                label(im, NAMES[v] + " (a page is cast)", size=12)
            r.append(im)
        rows.append(r)
    grid(rows).save(os.path.join(OUT, "cast_yuno.png"))
    rows = []
    for g in ("std", "yuno"):
        r = []
        for v in "abc":
            im = fit(tiles("sdw3_%s_%s_half" % (v, g))[(0, 0)], 400)
            label(im, NAMES[v] + " " + g + " first dive", size=12)
            r.append(im)
        rows.append(r)
    grid(rows).save(os.path.join(OUT, "half_dives.png"))
    # each version next to its reference
    if REFS:
        w = os.path.join(REFS, "windzip", "png")
        vr = os.path.join(REFS, "vref")
        reference("a", tiles("sdw3_a_yuno_full")[(1, 0)], [(os.path.join(w, "Spirit_Dive.png"), "pack: Spirit Dive"), (os.path.join(w, "Spirit_of_Zephyr.png"), "pack: Spirit of Zephyr")], "ref_a_anime.png")
        reference("b", tiles("sdw3_b_yuno_full")[(1, 0)], [(os.path.join(vr, "B_10.png"), "clip: Yuno's Spirit Dive"), (os.path.join(vr, "B_11.png"), "clip: the second blade")], "ref_b_clip.png")
        reference("c", tiles("sdw3_c_yuno_full")[(1, 0)], [(os.path.join(w, "Saint_Spirit_of_Zephyr.png"), "pack: Saint Spirit of Zephyr"), (os.path.join(w, "images1.jpg"), "pack: Boreas bow")], "ref_c_saint.png")
    print("sheets written to", OUT)


if __name__ == "__main__":
    main()
