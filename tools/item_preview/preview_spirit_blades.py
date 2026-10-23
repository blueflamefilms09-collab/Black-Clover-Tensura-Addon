"""
Software-render previews of the two Wind Spirit blades (Spirit of Zephyr sword, Spirit of Boreas bow) with the weapon preview rasteriser (no game, no GPU).

    python tools/item_preview/preview_spirit_blades.py

Writes docs/previews/spirit_blades/:
  turntable_zephyr.png / turntable_boreas.png   eight yaw steps round the weapon (0, 45 ... 315 degrees), the skin with the additive glow
  bow_draw_stages.png                           the bow at rest, a third, two thirds and a full draw (the arrow of light rides the nock)
  inventory_icons.png                           the two 16 x 16 inventory sprites at 8x on the creative-tab grey, next to a vanilla-sized slot
  hand_poses.png                                the weapon on the hand frame of the held display (third person right / left, first person): see tools/weapon_grip/preview_grip.py
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import preview_weapons as pw   # noqa: E402

OUT = os.path.join(ROOT, "docs", "previews", "spirit_blades")
TILE = 300


def shot(item_id, yaw, pitch, dist=1.9):
    quads, glow = pw.load(item_id)
    d = os.path.join(pw.ASSETS, "textures", "entity", "weapon")
    skin, glowtex = pw.tex(os.path.join(d, item_id.split("_pull")[0] + ".png")), pw.tex(os.path.join(d, item_id.split("_pull")[0] + "_glow.png"))
    k = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * k], axis=1), n) for (ly, pts, n) in quads]
    return pw.render(quads, skin, glowtex, glow, pw.view(yaw, pitch, dist))


def label(img, text):
    im = Image.fromarray((img * 255).astype(np.uint8))
    ImageDraw.Draw(im).text((6, 4), text, fill=(235, 245, 240))
    return np.asarray(im, np.float32) / 255


def main():
    pw.W = pw.H = TILE
    os.makedirs(OUT, exist_ok=True)
    for name, item, dist in (("zephyr", "spirit_of_zephyr", 2.3), ("boreas", "spirit_of_boreas", 1.55)):
        tiles = [label(shot(item, yw, 8, dist), "%s yaw %d" % (name, yw)) for yw in range(0, 360, 45)]
        sheet = np.concatenate([np.concatenate(tiles[:4], 1), np.concatenate(tiles[4:], 1)], 0)
        Image.fromarray((sheet * 255).astype(np.uint8)).save(os.path.join(OUT, "turntable_%s.png" % name))
    tiles = [label(shot(i, 30, 10, 1.55), t) for i, t in (("spirit_of_boreas", "rest"), ("spirit_of_boreas_pull1", "draw 1/3"), ("spirit_of_boreas_pull2", "draw 2/3"), ("spirit_of_boreas_pull3", "full draw"))]
    Image.fromarray((np.concatenate(tiles, 1) * 255).astype(np.uint8)).save(os.path.join(OUT, "bow_draw_stages.png"))
    # the inventory sprites
    icons = []
    for item in ("spirit_of_zephyr", "spirit_of_boreas"):
        im = Image.open(os.path.join(pw.ASSETS, "textures", "item", item + ".png")).convert("RGBA").resize((160, 160), Image.NEAREST)
        bg = Image.new("RGBA", (176, 176), (139, 139, 139, 255))
        bg.alpha_composite(im, (8, 8))
        icons.append(np.asarray(bg.convert("RGB"), np.float32) / 255)
    Image.fromarray((np.concatenate(icons, 1) * 255).astype(np.uint8)).save(os.path.join(OUT, "inventory_icons.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
