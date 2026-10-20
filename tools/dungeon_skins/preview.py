"""Why: nothing here can be run in a game, so this renders what the kit would draw for the dungeon's looks (the player model with the skin, the hair at its colour, the extras on their anchors, the
arm raised for the weapon) with the software renderer of tools/playermodel (pmrender / pmgeo) straight from assets/nusmp/playermodel/looks_dungeon.json, into docs/previews/humanoids/:

    python3 -B tools/dungeon_skins/preview.py sheets                 one three-view sheet per family: archetypes, the dungeon's own bodies, the puppets' four stages
    python3 -B tools/dungeon_skins/preview.py contact [prefix]       contact sheets (pages of 40) of every look whose id starts with the prefix (default: foundry_)
    python3 -B tools/dungeon_skins/preview.py look <id> [<id> ...]   three views of single looks
It is a re-implementation for review: light and colours differ from the game's, and the extras' clips are shown at rest.
"""
import json
import os
import sys

import common as CM
import preview as PV
from PIL import Image, ImageDraw
import pmrender as R


def scene(look):
    """PV.scene_of with the tint of every extra applied to its texture (the game multiplies it in; the software renderer has no tint)."""
    tints = {}
    for ex in look.get("extras", []):
        t = ex.get("tint")
        if t:
            h = t.lstrip("#")
            h = h[-6:]
            tints[os.path.join(CM.ASSETS, "textures", "entity", ex.get("texture", ex["model"]) + ".png")] = tuple(int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4))
    orig = R.load_tex

    def load(path):
        a = orig(path)
        c = tints.get(path)
        if c is not None:
            a = a.copy()
            for k in range(3):
                a[..., k] = a[..., k] * c[k]
        return a

    R.load_tex = load
    try:
        return PV.scene_of(look)
    finally:
        R.load_tex = orig


def table():
    with open(CM.LOOKS_DUNGEON) as f:
        return json.load(f)["looks"]


def views(look, size=(200, 300), yaws=((0, 6), (35, 6), (180, 6))):
    s = look.get("scale", 1.0)
    px = 5.0 / max(1.0, s * 0.9)
    return [R.render(scene(look), size=size, yaw=y, pitch=p, center=(0, 12, 0), scale=px, floor=True) for y, p in yaws]


def strip(ids, looks, out, name, cols=2, size=(200, 300)):
    cells = []
    for i in ids:
        v = views(looks[i], size)
        cell = Image.new("RGB", (size[0] * len(v), size[1]), (255, 255, 255))
        for k, t in enumerate(v):
            cell.paste(t, (k * size[0], 0))
        ImageDraw.Draw(cell).text((4, 4), i, fill=(0, 0, 0))
        cells.append(cell)
    rows = (len(cells) + cols - 1) // cols
    W = cells[0].size[0]
    page = Image.new("RGB", (W * cols, size[1] * rows), (255, 255, 255))
    for k, c in enumerate(cells):
        page.paste(c, ((k % cols) * W, (k // cols) * size[1]))
    os.makedirs(out, exist_ok=True)
    path = os.path.join(out, name + ".png")
    page.save(path)
    return path


def contact(prefix, looks, out, per_page=40, cols=8, size=(120, 190)):
    ids = [k for k in looks if k.startswith(prefix)]
    paths = []
    for p in range(0, len(ids), per_page):
        chunk = ids[p:p + per_page]
        rows = (len(chunk) + cols - 1) // cols
        sheet = Image.new("RGB", (size[0] * cols, size[1] * rows), (255, 255, 255))
        for k, i in enumerate(chunk):
            look = looks[i]
            s = look.get("scale", 1.0)
            im = R.render(scene(look), size=size, yaw=25, pitch=6, center=(0, 12, 0), scale=2.6 / max(1.0, s * 0.8), floor=False)
            ImageDraw.Draw(im).text((2, 2), i.replace(prefix, "")[:22], fill=(0, 0, 0))
            sheet.paste(im, ((k % cols) * size[0], (k // cols) * size[1]))
        os.makedirs(out, exist_ok=True)
        path = os.path.join(out, "%s%d.png" % (prefix.rstrip("_") or "all", p // per_page))
        sheet.save(path)
        paths.append(path)
    return paths


def main(argv):
    looks = table()
    out = CM.DOCS_PREVIEW
    if not argv or argv[0] == "sheets":
        dg = [k for k in looks if k.startswith("dg_")]
        for n, p in enumerate(range(0, len(dg), 8)):
            print(strip(dg[p:p + 8], looks, out, "archetypes_%d" % n))
        print(strip([k for k in ("living_armor", "floor_warden", "goliath", "goliath_form2") if k in looks], looks, out, "dungeon_bodies"))
        for who in ("jimmy", "alfredo", "carl"):
            print(strip(["corpse_%s_s%d" % (who, s) for s in range(4)], looks, out, "corpse_" + who))
    elif argv[0] == "contact":
        for p in contact(argv[1] if len(argv) > 1 else "foundry_", looks, out):
            print(p)
    elif argv[0] == "look":
        for i in argv[1:]:
            print(strip([i], looks, out, i, cols=1))


if __name__ == "__main__":
    main(sys.argv[1:])
