"""Fuegoleon's flame arm (3.1): builds the preview sheets of docs/previews/fuegoleon_arm_djinn/ from the renders of the repo's preview harness (tools/render_preview). Nothing here is seen in a game.

    # 1. render the scenes of tools/render_preview/scenes.d/fuegoleon_arm_djinn.json into one build folder (the flare scene while the wearer casts):
    #      RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py fad_quiet fad_out fad_forge fad_slim fad_v3 fad_fp
    #      SDV_CAST=1 RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py fad_flare
    # 2. python3 -B tools/fad_preview_sheets.py        # reads $RENDER_PREVIEW_BUILD (default build/render_preview), writes docs/previews/fuegoleon_arm_djinn/*.png

reference_arm.png (the owner's reference art, cropped to the arm) and djinn_gauntlet.png (a crop of docs/previews/djinn36/bael_sheet.png, the Djinn armour on the same kind of figure) are
inputs kept next to the outputs. A harness PNG is a header of 30 px, then rows of (tile height + 6) px, each tile starting 6 px from the left of its column.
"""
import json
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "fuegoleon_arm_djinn")
with open(os.path.join(HERE, "render_preview", "scenes.d", "fuegoleon_arm_djinn.json"), encoding="utf-8") as fh:
    SCENES = json.load(fh)


def tile(scene, row=0, col=0):
    tw, th = SCENES[scene]["tile"]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    x, y = 6 + col * (tw + 6), 36 + row * (th + 6)
    return im.crop((x, y, x + tw, y + th))


def labelled(im, text):
    d = ImageDraw.Draw(im)
    font = ImageFont.load_default()
    w = d.textlength(text, font=font)
    d.rectangle([0, im.size[1] - 17, w + 10, im.size[1]], fill=(0, 0, 0))
    d.text((5, im.size[1] - 14), text, fill=(255, 235, 160), font=font)
    return im


def sheet(name, items, note=None, height=None):
    """items = [(label, PIL image)]; every image is scaled to the same height."""
    font = ImageFont.load_default()
    height = height or min(im.size[1] for _, im in items)
    tiles = []
    for text, im in items:
        k = height / im.size[1]
        t = im.resize((max(1, int(im.size[0] * k)), height), Image.LANCZOS)
        tiles.append(labelled(t, text))
    top = 18 if note else 0
    out = Image.new("RGB", (sum(t.size[0] for t in tiles) + 4 * (len(tiles) - 1), height + top), (20, 20, 26))
    if note:
        ImageDraw.Draw(out).text((6, 3), note, fill=(200, 200, 210), font=font)
    x = 0
    for t in tiles:
        out.paste(t, (x, top))
        x += t.size[0] + 4
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".png")
    out.quantize(colors=224, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
    print(path, out.size, os.path.getsize(path) // 1024, "KB")


def main():
    ref = Image.open(os.path.join(OUT, "reference_arm.png")).convert("RGB")
    dj = Image.open(os.path.join(OUT, "djinn_gauntlet.png")).convert("RGB")
    sheet("compare_reference", [("REFERENCE (owner's art)", ref), ("quiet (contract held)", tile("fad_quiet", 3)), ("Salamander out", tile("fad_out", 3)),
                                ("flare (casting / Fire dive)", tile("fad_flare", 3)), ("Djinn armour, same harness idea", dj)],
          "Fuegoleon's flame arm, Djinn style, next to the reference and the Djinn armour (preview harness render, not a game screenshot)", 560)
    rows = [("front 3/4", 0), ("right side", 1), ("back 3/4", 2)]
    sheet("states_third_person", [("%s: %s" % (s, v), tile(sc, r)) for sc, s in (("fad_quiet", "quiet"), ("fad_out", "out"), ("fad_flare", "flare")) for v, r in rows],
          "third person: quiet / Salamander out / flare, three sides (preview harness render)", 420)
    sheet("forge_in", [("t=%g ticks" % a, tile("fad_forge", 0, c)) for c, a in enumerate(SCENES["fad_forge"]["ages"])],
          "forge-in: the flame kindles from the shoulder to the fist (ticks since the arm was lit; EquipAnimation stagger 0,3,6,9,12)", 380)
    sheet("forge_in_close", [("t=%g" % a, tile("fad_forge", 1, c)) for c, a in enumerate(SCENES["fad_forge"]["ages"])], None, 380)
    sheet("first_person_mock", [("MOCK: from the eye (approximation)", tile("fad_fp", 0))],
          "MOCK / APPROXIMATION: the real first person hand (FireArmFirstPerson) needs the game's hand pass; this is a camera behind the shoulder looking down the arm")
    sheet("old_looks_and_slim", [("3.0.0 look (V3, kept as option)", tile("fad_v3", 1)), ("new look (DJINN)", tile("fad_out", 3)), ("new look, slim (Alex) arm", tile("fad_slim", 1))],
          "the kept 3.0.0 look next to the new one, and the slim arm", 520)


if __name__ == "__main__":
    main()
