"""Spirit Dive, WIND (PACK look): builds the preview sheets of docs/previews/spirit_dive_wind/ from the renders of the repo's own preview harness (tools/render_preview), next to crops of the owner's
reference pack (docs/previews/spirit_dive_wind/references/) and the REEL Wind of 3.3 (the 'before').

    python3 -B tools/spirit_dive_wind_scenes.py                                   # (once) the scenes
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py "sdw_*"
    SDV_CAST=1 RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py sdw_yuno_bow
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/spirit_dive_wind_previews.py [standard|yuno|bow|entry|close|half|all]

Nothing here is seen in a game: the harness is a software rasteriser (translucent and additive passes approximated, no bloom, no post effects), so judge the silhouette, the colours and the
parts, not the exact brightness. A harness PNG is a header of 36 px, then one row per view of (tile height + 6) px, each tile starting 6 px from the left, one tile per age across.
"""
import json
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.environ.get("SDW_OUT") or os.path.join(ROOT, "docs", "previews", "spirit_dive_wind")
REF = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind", "references")
with open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind.json"), encoding="utf-8") as fh:
    SCENES = {k: v for k, v in json.load(fh).items() if isinstance(v, dict)}
FONT = ImageFont.load_default()


def tile(scene, row=0, col=0):
    tw, th = SCENES[scene]["tile"]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    x = 6 + col * (tw + 6)
    y = 36 + row * (th + 6)
    return im.crop((x, y, x + tw, y + th))


def label(t, text):
    d = ImageDraw.Draw(t)
    w = d.textlength(text, font=FONT)
    d.rectangle([0, t.size[1] - 17, w + 10, t.size[1]], fill=(0, 0, 0))
    d.text((5, t.size[1] - 14), text, fill=(255, 235, 160), font=FONT)
    return t


def ref_tile(name, text, h):
    im = Image.open(os.path.join(REF, name)).convert("RGB")
    im = im.resize((max(1, int(im.size[0] * h / im.size[1])), h), Image.LANCZOS)
    return label(im, text)


def save(name, tiles, note, max_width=2300):
    rows, cur, w = [], [], 0
    for t in tiles:
        if cur and w + t.size[0] > max_width:
            rows.append(cur)
            cur, w = [], 0
        cur.append(t)
        w += t.size[0] + 4
    rows.append(cur)
    top = 18
    W = max(sum(t.size[0] for t in r) + 4 * (len(r) - 1) for r in rows)
    H = sum(max(t.size[1] for t in r) for r in rows) + 4 * (len(rows) - 1)
    out = Image.new("RGB", (W, H + top), (20, 20, 26))
    ImageDraw.Draw(out).text((6, 3), note, fill=(200, 200, 210), font=FONT)
    y = top
    for r in rows:
        x = 0
        for t in r:
            out.paste(t, (x, y))
            x += t.size[0] + 4
        y += max(t.size[1] for t in r) + 4
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".png")
    out.quantize(colors=224, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
    print(path, out.size, os.path.getsize(path) // 1024, "KB")


VIEWS = ["front", "front 3/4", "back 3/4", "side"]


def tier(g, who, refs):
    sc = "sdw_%s_full" % g
    h = SCENES[sc]["tile"][1]
    tiles = [ref_tile(f, t, h) for f, t in refs]
    tiles += [label(tile(sc, r), "%s, %s" % (who, VIEWS[r])) for r in range(4)]
    tiles.append(label(tile("sdw_reel_%s_full" % g, 0), "BEFORE (REEL Wind of 3.3), %s" % who))
    save("wind_" + g, tiles, "WIND - Spirit Dive, PACK look, %s, full stage (preview harness render, not a game screenshot)" % who)


def bow():
    sc = "sdw_yuno_bow"
    h = SCENES[sc]["tile"][1]
    tiles = [ref_tile("boreas_bow.jpg", "pack: Yuno with the bow (Boreas)", h), ref_tile("zephyr.jpg", "pack: Yuno with the sword (Zephyr)", h)]
    tiles += [label(tile(sc, r), "Yuno, bow, " + n) for r, n in enumerate(("front", "front 3/4", "side"))]
    tiles.append(label(tile("sdw_yuno_full", 1), "Yuno, sword, front 3/4"))
    tiles.append(label(tile("sdw_yuno_hand", 0), "Yuno, the hand (the arm hangs in the harness)"))
    save("yuno_weapons", tiles, "WIND - Yuno's weapons: Zephyr's sword at rest, Boreas' bow with an arrow of light while a page is cast (preview harness)")


def close():
    tiles = [ref_tile("spirit_dive_crown.jpg", "pack: the crown", 440)]
    for g, who in (("std", "standard"), ("yuno", "Yuno")):
        sc = "sdw_%s_close" % g
        tiles += [label(tile(sc, 0), "%s: head and chest" % who), label(tile(sc, 1), "%s: head, back" % who)]
    save("close", tiles, "WIND - close-ups: the crown, the cross on the leaf, the circle behind the back (preview harness)")


def entry():
    for g, who in (("std", "standard"), ("yuno", "Yuno")):
        sc = "sdw_%s_entry" % g
        n = len(SCENES[sc]["ages"])
        tiles = [label(tile(sc, 0, c), "tick %d" % SCENES[sc]["ages"][c]) for c in range(n)]
        sx = "sdw_%s_exit" % g
        tiles += [label(tile(sx, 0, c), "exit, tick %d" % SCENES[sx]["ages"][c]) for c in range(len(SCENES[sx]["ages"]))]
        save("entry_" + g, tiles, "WIND %s tier: the entry (22 ticks) and the exit (preview harness)" % who)


def half():
    tiles = [label(tile("sdw_std_half", 0), "first dive (left half), standard"), label(tile("sdw_yuno_half", 0), "first dive (left half), Yuno")]
    for c, t in enumerate(SCENES["sdw_yuno_upgrade"]["ages"]):
        tiles.append(label(tile("sdw_yuno_upgrade", 0, c), "upgrade, tick %d" % t))
    save("half_dives", tiles, "WIND - the first dive (left half) and an upgrade (preview harness)")


def main(argv):
    what = argv[1] if len(argv) > 1 else "all"
    if what in ("standard", "all"):
        tier("std", "standard tier", [("spirit_dive.jpg", "pack: Spirit Dive (Yuno)"), ("minecraft_hint.jpg", "pack: another mod (scale hint)")])
    if what in ("yuno", "all"):
        tier("yuno", "Yuno tier", [("zephyr.jpg", "pack: Spirit of Zephyr"), ("spirit_dive.jpg", "pack: Spirit Dive")])
    if what in ("bow", "all"):
        bow()
    if what in ("close", "all"):
        close()
    if what in ("entry", "all"):
        entry()
    if what in ("half", "all"):
        half()


if __name__ == "__main__":
    main(sys.argv)
