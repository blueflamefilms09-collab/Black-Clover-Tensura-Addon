"""Spirit Dive, ANIME look: builds the preview sheets of docs/previews/spirit_dive_anime/ from the renders of the repo's own preview harness (tools/render_preview), next to crops of the owner's
reference stills (docs/previews/spirit_dive_anime/references/).

    python3 -B tools/spirit_dive_anime_scenes.py                                   # (once) the scenes
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py "sda_*"
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/spirit_dive_anime_previews.py [wind|fire|water|earth|entry|half|close|all]

Nothing here is seen in a game: the harness is a software rasteriser (translucent and additive passes approximated, no bloom, no post effects), so judge the silhouette, the colours and the
bands, not the exact brightness. A harness PNG is a header of 36 px, then one row per view of (tile height + 6) px, each tile starting 6 px from the left, one tile per age across.
"""
import json
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.environ.get("SDA_OUT") or os.path.join(ROOT, "docs", "previews", "spirit_dive_anime")
REF = os.path.join(ROOT, "docs", "previews", "spirit_dive_anime", "references")
with open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_anime.json"), encoding="utf-8") as fh:
    SCENES = {k: v for k, v in json.load(fh).items() if isinstance(v, dict)}
FONT = ImageFont.load_default()
CHARS = {"wind": ("yuno", "Yuno"), "fire": ("fuego", "Fuegoleon"), "water": ("noelle", "Noelle")}
REFS = {"wind": [("yuno_spirit_dive.png", "reference: Yuno, Spirit Dive"), ("yuno_sylphs_breath.png", "reference: Sylph's Breath")],
        "fire": [("fuegoleon.png", "reference: Fuegoleon (owner's art)")],
        "water": [("noelle_valkyrie.png", "reference: Saint Valkyrie Dress"), ("noelle_mermaid.png", "reference: Mermaid Form")],
        "earth": []}


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


def save(name, tiles, note):
    h = max(t.size[1] for t in tiles)
    top = 18
    out = Image.new("RGB", (sum(t.size[0] for t in tiles) + 4 * (len(tiles) - 1), h + top), (20, 20, 26))
    ImageDraw.Draw(out).text((6, 3), note, fill=(200, 200, 210), font=FONT)
    x = 0
    for t in tiles:
        out.paste(t, (x, top))
        x += t.size[0] + 4
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".png")
    out.quantize(colors=224, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
    print(path, out.size, os.path.getsize(path) // 1024, "KB")


def element(el):
    g, who = CHARS.get(el, (None, None))
    h = SCENES["sda_%s_std_full" % el]["tile"][1]
    tiles = [ref_tile(f, t, h) for f, t in REFS[el]]
    tiles += [label(tile("sda_%s_std_full" % el, 0), "standard tier, front 3/4"), label(tile("sda_%s_std_full" % el, 1), "standard tier, back")]
    if g:
        tiles += [label(tile("sda_%s_%s_full" % (el, g), 0), "%s tier, front 3/4" % who), label(tile("sda_%s_%s_full" % (el, g), 1), "%s tier, back" % who),
                  label(tile("sda_%s_%s_full" % (el, g), 2), "%s tier, side" % who)]
    else:
        tiles += [label(tile("sda_%s_std_full" % el, 2), "standard tier, side")]
    save(el, tiles, "%s - Spirit Dive, ANIME look, full stage (preview harness render, not a game screenshot)" % el.upper())


def close(el):
    g, who = CHARS.get(el, (None, "standard"))
    sc = "sda_%s_%s_close" % (el, g or "std")
    save("close_" + el, [label(tile(sc, 0), "%s: head and chest" % who), label(tile(sc, 1), "%s: head, back" % who),
                         label(tile("sda_%s_std_close" % el, 0), "standard: head and chest")], "%s - close-ups: the cap of hair, the eyes, the halo, the shell (preview harness)" % el.upper())


def entry(el):
    g, who = CHARS.get(el, (None, None))
    for tier, tn in ([("std", "standard")] + ([(g, who)] if g else [])):
        sc = "sda_%s_%s_entry" % (el, tier)
        n = len(SCENES[sc]["ages"])
        tiles = [label(tile(sc, 0, c), "tick %d" % SCENES[sc]["ages"][c]) for c in range(n)]
        sx = "sda_%s_%s_exit" % (el, tier)
        tiles += [label(tile(sx, 0, c), "exit, tick %d" % SCENES[sx]["ages"][c]) for c in range(len(SCENES[sx]["ages"]))]
        save("entry_%s_%s" % (el, tier), tiles, "%s %s tier: the entry (18 ticks) and the exit (preview harness)" % (el.upper(), tn))


def half():
    tiles = []
    for sc, text in (("sda_wind_std_half", "Wind, first dive (left half)"), ("sda_fire_std_half", "Fire, first dive"), ("sda_water_std_half", "Water, first dive"), ("sda_earth_std_half", "Earth, first dive")):
        tiles.append(label(tile(sc, 0), text))
    tiles.append(label(tile("sda_wind_yuno_upgrade", 0, 2), "Wind upgrade, tick 10"))
    save("half_dives", tiles, "the half dives and an upgrade (preview harness)")


def main(argv):
    what = argv[1] if len(argv) > 1 else "all"
    for el in ("wind", "fire", "water", "earth"):
        if what in (el, "all"):
            element(el)
            close(el)
    if what in ("entry", "all"):
        for el in ("wind", "fire", "water", "earth"):
            entry(el)
    if what in ("half", "all"):
        half()


if __name__ == "__main__":
    main(sys.argv)
