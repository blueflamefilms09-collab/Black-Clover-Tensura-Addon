"""Spirit Dive, Djinn-equip style: builds the preview sheets of docs/previews/spirit_dive_djinn/ from the renders of the repo's own preview harness (tools/render_preview), next to the Djinn Full Equip.

    python3 -B tools/spirit_dive_djinn_scenes.py                                   # (once) the scenes
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py "sdj_*"
    RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/spirit_dive_djinn_previews.py

Nothing here is seen in a game: the harness is a software rasteriser (translucent and additive passes approximated, no bloom). The Djinn tiles are cut from docs/previews/djinn36/<djinn>_sheet.png (the
mesh_lib rasteriser of the Djinn Full Equip: judge the level of detail and the silhouette, not the shading).
A harness PNG is a header of 36 px, then one row per view of (tile height + 6) px, each tile starting 6 px from the left, one tile per age across.
"""
import json
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_djinn")
with open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_djinn.json"), encoding="utf-8") as fh:
    SCENES = {k: v for k, v in json.load(fh).items() if isinstance(v, dict)}
FONT = ImageFont.load_default()
# the Djinn tile of each element: full equip, light, three-quarter and back (x 300 to 900 of the 2000 x 480 sheet)
DJINN = {"wind": ("seere", "Djinn Seere"), "fire": ("alloces", "Djinn Alloces"), "water": ("gusion", "Djinn Gusion"), "earth": ("haagenti", "Djinn Haagenti")}


def tile(scene, row=0, col=0):
    tw, th = SCENES[scene]["tile"]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    ncols = len(SCENES[scene]["ages"])
    x = 6 + col * (tw + 6)
    y = 36 + row * (th + 6)
    return im.crop((x, y, x + tw, y + th))


def label(t, text):
    d = ImageDraw.Draw(t)
    w = d.textlength(text, font=FONT)
    d.rectangle([0, t.size[1] - 17, w + 10, t.size[1]], fill=(0, 0, 0))
    d.text((5, t.size[1] - 14), text, fill=(255, 235, 160), font=FONT)
    return t


def djinn_tile(el, h):
    name, text = DJINN[el]
    im = Image.open(os.path.join(ROOT, "docs", "previews", "djinn36", name + "_sheet.png")).convert("RGB").crop((300, 0, 900, 480))
    im = im.resize((int(600 * h / 480), h), Image.LANCZOS)
    return label(im, text + " (the Full Equip the style comes from)")


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


def strip(name, scene, note, cols=None, row=0):
    n = len(SCENES[scene]["ages"])
    tiles = [label(tile(scene, row, c), "tick %d" % SCENES[scene]["ages"][c]) for c in (cols or range(n))]
    save(name, tiles, note)


def main():
    chars = {"wind": ("yuno", "Yuno"), "fire": ("fuego", "Fuegoleon"), "water": ("noelle", "Noelle")}
    for el in ("wind", "fire", "water"):
        g, who = chars[el]
        tiles = [djinn_tile(el, 560),
                 label(tile("sdj_%s_std_full" % el, 0), "standard tier, front"), label(tile("sdj_%s_std_full" % el, 1), "standard tier, back"),
                 label(tile("sdj_%s_%s_full" % (el, g), 0), who + " tier, front"), label(tile("sdj_%s_%s_full" % (el, g), 1), who + " tier, back"),
                 label(tile("sdj_%s_%s_full" % (el, g), 2), who + " tier, side")]
        save(el, tiles, "%s - Spirit Dive in the style of the Djinn Full Equip, full stage (preview harness render, not a game screenshot)" % el.upper())
        save(el + "_close", [label(tile("sdj_%s_%s_close" % (el, g), r), n) for r, n in enumerate(("head and chest", "legs", "back, close"))],
             "%s - %s tier, close up" % (el.upper(), who))
        strip(el + "_forge", "sdj_%s_%s_forge" % (el, g), "%s - %s tier: the forge-in, piece by piece (EquipAnimation, the Djinn's own)" % (el.upper(), who))
    save("earth", [djinn_tile("earth", 560), label(tile("sdj_earth_std_full", 0), "standard tier, front"), label(tile("sdj_earth_std_full", 1), "standard tier, back"),
                   label(tile("sdj_earth_std_full", 2), "standard tier, side")], "EARTH - Spirit Dive (no canon character: always the standard tier)")
    save("earth_close", [label(tile("sdj_earth_std_close", r), n) for r, n in enumerate(("head and chest", "legs", "back, close"))], "EARTH - close up")
    strip("earth_forge", "sdj_earth_std_forge", "EARTH - the forge-in")
    halves = []
    for el in ("wind", "fire", "water", "earth"):
        halves.append(label(tile("sdj_%s_std_half" % el, 0), "%s, standard" % el))
        if el in chars:
            halves.append(label(tile("sdj_%s_%s_half" % (el, chars[el][0]), 0), "%s, %s" % (el, chars[el][1])))
    save("half_dives", halves, "the first (half) dives: the crest and the arms (Wind: the left half of the body), the Djinn's Half Equip")
    strip("upgrade_wind_yuno", "sdj_wind_yuno_upgrade", "Yuno's half dive becoming full: the left half stays on, the right half forges in")
    strip("shatter_wind_yuno", "sdj_wind_yuno_shatter", "the end of the dive: the pieces shatter out (the last 18 ticks)")


if __name__ == "__main__":
    main()
