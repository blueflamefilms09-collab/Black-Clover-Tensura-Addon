"""Spirit Dive remake: builds the preview sheets of docs/previews/spirit_dives/ from the renders of the repo's own preview harness (tools/render_preview). Nothing here is seen in a game.

    # 1. render the scenes (tools/render_preview/scenes.d/spirit_dive_v2.json and great_spirits_dive.json) into one build folder:
    #      RENDER_PREVIEW_BUILD=build/render_preview python3 -B tools/render_preview/preview.py dive_wind_full dive_fire_full dive_water_saint dive_earth_full "sdv_*"-names...
    #    (SDV_CAST=1 renders the same arm scenes while the wearer casts; copy those PNGs to <scene>_cast.png first)
    # 2. python3 -B tools/spirit_dive_previews.py            # reads $RENDER_PREVIEW_BUILD (default build/render_preview), writes docs/previews/spirit_dives/*.png

A harness PNG is a header of 36 px, then one row per view of (tile height + 6) px, each tile starting 6 px from the left. A sheet is a row of tiles cut from several PNGs, each with its label.
"""
import json
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dives")
SCENES = {}
for f in ("great_spirits_dive.json", "spirit_dive_v2.json"):
    with open(os.path.join(HERE, "render_preview", "scenes.d", f), encoding="utf-8") as fh:
        SCENES.update({k: v for k, v in json.load(fh).items() if isinstance(v, dict)})


def tile(scene, row, png=None):
    """The tile of one view row of a scene's render (png = another file name of the same scene, e.g. the cast render)."""
    tw, th = SCENES[scene]["tile"]
    im = Image.open(os.path.join(BUILD, (png or scene) + ".png")).convert("RGB")
    y = 36 + row * (th + 6)
    return im.crop((6, y, 6 + tw, y + th))


def sheet(name, items, note=None):
    """items = [(label, scene, row)] or [(label, scene, row, png)]."""
    font = ImageFont.load_default()
    tiles = []
    for it in items:
        label, scene, row = it[0], it[1], it[2]
        t = tile(scene, row, it[3] if len(it) > 3 else None)
        d = ImageDraw.Draw(t)
        w = d.textlength(label, font=font)
        d.rectangle([0, t.size[1] - 17, w + 10, t.size[1]], fill=(0, 0, 0))
        d.text((5, t.size[1] - 14), label, fill=(255, 235, 160), font=font)
        tiles.append(t)
    h = max(t.size[1] for t in tiles)
    top = 18 if note else 0
    out = Image.new("RGB", (sum(t.size[0] for t in tiles) + 4 * (len(tiles) - 1), h + top), (20, 20, 26))
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


def copy_whole(name, scene, note=None):
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    if note:
        out = Image.new("RGB", (im.size[0], im.size[1] + 18), (20, 20, 26))
        ImageDraw.Draw(out).text((6, 3), note, fill=(200, 200, 210), font=ImageFont.load_default())
        out.paste(im, (0, 18))
        im = out
    path = os.path.join(OUT, name + ".png")
    os.makedirs(OUT, exist_ok=True)
    im.quantize(colors=224, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
    print(path, im.size, os.path.getsize(path) // 1024, "KB")


def main():
    sheet("wind_yuno", [("1.99 classic look", "dive_wind_full", 0), ("standard tier (every mage)", "sdv_wind_std_full", 0), ("Yuno tier, front", "sdv_wind_yuno_full", 0), ("Yuno tier, back", "sdv_wind_yuno_full", 1)],
          "WIND - Spirit Dive, full stage (preview harness render, not a game screenshot)")
    sheet("fire_fuegoleon", [("1.99 classic look", "dive_fire_full", 0), ("standard tier (every mage)", "sdv_fire_std_full", 0), ("Fuegoleon tier, front", "sdv_fire_fuego_full", 0), ("Fuegoleon tier, back", "sdv_fire_fuego_full", 1)],
          "FIRE - Spirit Dive, full stage (preview harness render, not a game screenshot)")
    sheet("water_noelle", [("1.99 classic look (Saint Valkyrie Dress)", "dive_water_saint", 0), ("standard tier (every mage)", "sdv_water_std_full", 0), ("Noelle tier, front", "sdv_water_noelle_full", 0),
                           ("Noelle tier, back", "sdv_water_noelle_full", 1)], "WATER - Spirit Dive, full stage (preview harness render, not a game screenshot)")
    sheet("earth", [("1.99 classic look", "dive_earth_full", 0), ("standard tier, full, front", "sdv_earth_std_full", 0), ("standard tier, full, back", "sdv_earth_std_full", 1), ("standard tier, half", "sdv_earth_std_half", 0)],
          "EARTH - Spirit Dive (no canon character: always the standard tier)")
    sheet("half_dives", [("Wind, standard", "sdv_wind_std_half", 0), ("Wind, Yuno (left half only)", "sdv_wind_yuno_half", 0), ("Fire, standard", "sdv_fire_std_half", 0), ("Fire, Fuegoleon", "sdv_fire_fuego_half", 0),
                         ("Water, standard", "sdv_water_std_half", 0), ("Water, Noelle", "sdv_water_noelle_half", 0)], "the first dives (half stage)")
    copy_whole("entry_yuno", "sdv_entry_yuno", "Yuno tier: the entry at ticks 3, 8, 14 and 22 (shockwave, pillar of light, flash)")
    copy_whole("entry_fuegoleon", "sdv_entry_fuego", "Fuegoleon tier: the entry at ticks 3, 8, 14 and 22")
    copy_whole("entry_noelle", "sdv_entry_noelle", "Noelle tier: the entry at ticks 3, 8, 14 and 22")
    sheet("fuegoleon_arm", [("front 3/4", "sdv_arm_rest", 0), ("right side", "sdv_arm_rest", 1), ("back 3/4", "sdv_arm_rest", 2)], "Fuegoleon's flaming right arm at rest (the contract is held); third person, preview harness render")
    items = [("at rest (quiet flame)", "sdv_arm_rest", 0), ("Salamander out (burns higher)", "sdv_arm_linked", 0)]
    if os.path.isfile(os.path.join(BUILD, "sdv_arm_linked_cast.png")):
        items.append(("while casting (flare)", "sdv_arm_linked", 0, "sdv_arm_linked_cast"))
    sheet("fuegoleon_arm_states", items, "the arm in its three states (third person)")
    if os.path.isfile(os.path.join(BUILD, "sdv_fire_fuego_full_cast.png")):
        sheet("fuegoleon_dive_cast", [("Fuegoleon dive", "sdv_fire_fuego_full", 0), ("the same, casting (flare)", "sdv_fire_fuego_full", 0, "sdv_fire_fuego_full_cast")], "Fire Spirit Dive of the Fuegoleon tier with the arm painter, and during a cast")
    sheet("fuegoleon_arm_first_person_approx", [("rest", "sdv_arm_fp_rest", 0), ("Salamander out", "sdv_arm_fp_linked", 0)],
          "MOCK / APPROXIMATION of the first person view: a camera behind the shoulder, the arm swung forward. The real hand view (FireArmFirstPerson) was not rendered here.")


if __name__ == "__main__":
    main()
