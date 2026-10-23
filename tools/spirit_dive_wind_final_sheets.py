"""Composes the preview sheets of the final Wind A into docs/previews/spirit_dive_wind_final/ from the scene renders (scenes.d/spirit_dive_wind_final.json).

    python3 -B tools/spirit_dive_wind_final_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py <every scene name of the json except sdw4_yuno_cast>
    SDV_CAST=1 RENDER_PREVIEW_BUILD=<dir2> python3 -B tools/render_preview/preview.py sdw4_yuno_cast
    RENDER_PREVIEW_BUILD=<dir> RENDER_PREVIEW_CAST=<dir2> python3 -B tools/spirit_dive_wind_final_sheets.py

BEFORE pictures are the 3.5 version A (scenes sdw3_a_* of scenes.d/spirit_dive_wind3.json, same camera), kept in docs/previews/spirit_dive_wind_final/before_3_5/.
Every picture is a render of the repo's software harness, NOT a game screenshot (no bloom, additive passes approximated, no animation: the arms hang).
"""
import json
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
CAST = os.path.abspath(os.environ.get("RENDER_PREVIEW_CAST") or BUILD)
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind_final")
BEFORE = os.path.join(OUT, "before_3_5")
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind_final.json")))
MARGIN, HEAD = 6, 30
BEFORE_SCENES = {"sdw3_a_yuno_full": ((400, 580), 4, 1), "sdw3_a_std_full": ((400, 580), 4, 1), "sdw3_a_yuno_entry": ((260, 440), 1, 13)}


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def tiles_of(path, tile, rows, cols):
    im = Image.open(path).convert("RGB")
    w, h = tile
    return {(r, c): im.crop((MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN), MARGIN + c * (w + MARGIN) + w, HEAD + MARGIN + r * (h + MARGIN) + h)) for r in range(rows) for c in range(cols)}


def tiles(scene, root=None):
    sc = SCENES[scene]
    return tiles_of(os.path.join(root or BUILD, scene + ".png"), sc["tile"], len(sc["views"]), len(sc["ages"]))


def before(scene):
    tile, rows, cols = BEFORE_SCENES[scene]
    return tiles_of(os.path.join(BEFORE, scene + ".png"), tile, rows, cols)


def fit(im, h):
    return im.resize((max(1, round(im.width * h / im.height)), h), Image.LANCZOS)


def label(im, text, xy=(6, 5), size=13):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


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


def titled(sheet, title):
    head = Image.new("RGB", (sheet.width, 28), (20, 21, 28))
    ImageDraw.Draw(head).text((8, 6), title, font=font(14), fill=(235, 240, 235))
    return grid([[head], [sheet]], gap=0)


def main():
    os.makedirs(OUT, exist_ok=True)
    names = ("front", "front 3/4", "back 3/4", "side")
    for g, nm in (("yuno", "Yuno"), ("std", "standard")):
        # BEFORE (3.5 A) vs AFTER (final A), same camera
        b, a = before("sdw3_a_%s_full" % g), tiles("sdw4_%s_full" % g)
        rows = []
        for src, tag in ((b, "BEFORE (3.5 A)"), (a, "AFTER (final A, calm)")):
            r = []
            for i in range(4):
                im = fit(src[(i, 0)], 430)
                label(im, tag + " / " + names[i] if i == 0 else names[i])
                r.append(im)
            rows.append(r)
        titled(grid(rows), "WIND Spirit Dive, %s tier, steady state (t = 50): BEFORE vs AFTER (software render, not a game screenshot)" % nm).save(os.path.join(OUT, "before_after_%s.png" % g))
        # the crown from four sides
        t = tiles("sdw4_%s_crown" % g)
        r = []
        for i in range(4):
            im = fit(t[(i, 0)], 380)
            label(im, SCENES["sdw4_%s_crown" % g]["views"][i]["name"])
            r.append(im)
        titled(grid([r]), "3D crown layer, %s tier, from four angles (software render)" % nm).save(os.path.join(OUT, "crown_%s.png" % g))
        # entry -> steady state
        t = tiles("sdw4_%s_entry" % g)
        ages = SCENES["sdw4_%s_entry" % g]["ages"]
        r = []
        for i, age in enumerate(ages):
            im = fit(t[(0, i)], 300)
            label(im, "t=%d" % age + (" entry" if age < 22 else " settling" if age < 36 else " quiet"))
            r.append(im)
        titled(grid([r[:5], r[5:]]), "Entry (t < 22) and the settling into the quiet steady state, %s tier" % nm).save(os.path.join(OUT, "entry_to_steady_%s.png" % g))
        # calm vs loud (spiritDive.calm = false) vs just after a cast
        cells = [(tiles("sdw4_%s_full" % g)[(1, 0)], "calm (default), t=50"), (tiles("sdw4_%s_loud" % g)[(0, 0)], "spiritDive.calm = false (3.5 density), t=50")]
        if g == "yuno":
            cells.append((tiles("sdw4_yuno_cast", CAST)[(0, 0)], "2 s after a cast (loud again)"))
        r = []
        for im, txt in cells:
            im = fit(im, 430)
            label(im, txt)
            r.append(im)
        titled(grid([r]), "Calm vs the previous density%s, %s tier" % (" vs just after a cast" if g == "yuno" else "", nm)).save(os.path.join(OUT, "calm_vs_loud_%s.png" % g))
        # first dive (half), exit
        r = []
        for scene, txt in (("sdw4_%s_half" % g, "first dive (left half)"),):
            im = fit(tiles(scene)[(0, 0)], 400)
            label(im, txt)
            r.append(im)
        t = tiles("sdw4_%s_exit" % g)
        for i, age in enumerate(SCENES["sdw4_%s_exit" % g]["ages"]):
            im = fit(t[(0, i)], 400)
            label(im, "exit t=%d" % age)
            r.append(im)
        titled(grid([r]), "First dive and the exit, %s tier" % nm).save(os.path.join(OUT, "half_and_exit_%s.png" % g))
    # the entry before (3.5 A), for the same ages as far as they exist
    bt = before("sdw3_a_yuno_entry")
    r = []
    for i, age in enumerate((0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24)):
        if age in (4, 8, 12, 16, 20, 22):
            im = fit(bt[(0, i)], 300)
            label(im, "BEFORE t=%d" % age)
            r.append(im)
    t = tiles("sdw4_yuno_entry")
    a = []
    for i, age in enumerate(SCENES["sdw4_yuno_entry"]["ages"]):
        if age in (4, 8, 12, 16, 20, 22):
            im = fit(t[(0, i)], 300)
            label(im, "AFTER t=%d" % age)
            a.append(im)
    titled(grid([r, a]), "Yuno entry, BEFORE (3.5 A) vs AFTER (3D crown ignites; the entry effects are unchanged)").save(os.path.join(OUT, "entry_before_after_yuno.png"))
    t = tiles("sdw4_yuno_upgrade")
    r = []
    for i, age in enumerate(SCENES["sdw4_yuno_upgrade"]["ages"]):
        im = fit(t[(0, i)], 400)
        label(im, "upgrade t=%d" % age)
        r.append(im)
    titled(grid([r]), "Upgrade (second dive of Wind): the right half of the crown ignites").save(os.path.join(OUT, "upgrade_yuno.png"))
    print("sheets written to", OUT)


if __name__ == "__main__":
    main()
