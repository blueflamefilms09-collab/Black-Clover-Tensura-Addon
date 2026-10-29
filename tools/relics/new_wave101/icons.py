"""The sixteen-by-sixteen inventory sprites of the twelve upgraded relics of wave 101 and the item models that hand their 3D form to the renderer:

    python3 -B tools/relics/new_wave101/icons.py

Each sprite is drawn from the relic's own model (skin and glow passes only, a three-quarter view, no additive layers): rendered large, cut out by the difference of two renders on a light and
on a dark background, fitted into 14 x 14 with a one pixel dark outline and a light edge, then reduced to the 16 x 16 pixel-art size of the other relic sprites. Deterministic. The item json of
every relic is the `builtin/entity` model with no display block and the `_icon` json that keeps the sprite for the inventory (the same two files tools/relics/gen_hand_item_models.py writes).
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import common as C  # noqa: E402
import metrics as MT  # noqa: E402
import vupreview as VP  # noqa: E402

ICONS = os.path.join(MT.ASSETS, "textures", "item")
MODELS = os.path.join(MT.ASSETS, "models", "item")
SOLID = ~(VP.STAR | VP.HALO)

# the view of each sprite and, where the whole model is not the iconic part, the window on it (model pixels, authoring frame: x right, y up)
VIEWS = {
    "millennium_puzzle": ((0, 0), (-4.7, 4.7, -9.4, -0.2)),
    "millennium_eye": ((0, 0), (-3.2, 3.2, 3.5, 9.9)),
    "millennium_ring": ((12, 0), (-5.6, 3.6, -12.4, -3.0)),
    "millennium_key": ((0, 0), (-4.2, 4.2, -24.2, -11.0)),
    "millennium_necklace": ((0, 12), (-5.8, 5.8, -6.0, 0.8)),
    "millennium_scale": ((0, 16), (-11.5, 11.5, -9.0, 17.0)),
    "straw_hat_of_joy_boy": ((0, 12), (-11.0, 11.0, 5.5, 14.5)),
    "law_of_cycles_gown": ((0, 0), (-9.0, 9.0, -19.0, 1.6)),
    "seven_dragon_balls_of_shenron": ((20, 52), None),
    "truth_seeking_orbs_of_the_sage": ((180, 0), None),
}


def _projected_box(path, anchor, yaw, pitch):
    a, head, groups, quads = VP.parse_model(path)
    off = np.array(VP.PW.ANCHORS[anchor or a], float)
    R = VP.PW.rot(yaw, pitch)
    lo, hi = np.array([1e9, 1e9]), np.array([-1e9, -1e9])
    for (g, slot, flags, P, UV, N) in quads:
        if flags & (VP.STAR | VP.HALO):
            continue
        Q = (P + off) @ R.T
        lo = np.minimum(lo, Q[:, :2].min(axis=0))
        hi = np.maximum(hi, Q[:, :2].max(axis=0))
    return lo, hi


def sprite(rid, anchor, big=384):
    path = os.path.join(MT.VU_MODELS, rid + ".txt")
    pal = C.R.BY_ID[rid][4]
    view, window = VIEWS.get(rid, ((28, 10), None))
    if window is None:
        lo, hi = _projected_box(path, anchor, *view)
        pad = 0.04 * max(hi - lo)
        crop = (lo[0] - pad, hi[0] + pad, lo[1] - pad, hi[1] + pad)
    else:
        a0, a1, b0, b1 = window
        off = VP.PW.ANCHORS[anchor]
        crop = (a0 + off[0], a1 + off[0], -b1 + off[1], -b0 + off[1])
    w, h = crop[1] - crop[0], crop[3] - crop[2]
    side = max(w, h)
    cx, cy = (crop[0] + crop[1]) / 2, (crop[2] + crop[3]) / 2
    crop = (cx - side / 2, cx + side / 2, cy - side / 2, cy + side / 2)
    scale = (big - 2) / side
    pieces = [(path, anchor, False)]
    a = VP.render(pieces, pal, view[0], view[1], crop, scale, (big, big), 0.0, False, ("skin", "glow"), bg=0.0)
    b = VP.render(pieces, pal, view[0], view[1], crop, scale, (big, big), 0.0, False, ("skin", "glow"), bg=1.0)
    mask = (np.abs(a - b).max(axis=2) < 0.02)
    rgba = np.concatenate([a, mask[..., None].astype(np.float32)], axis=2)
    im = Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")
    # fit into the 14 x 14 middle of the sprite, keeping the shape
    bbox = im.getbbox()
    im = im.crop(bbox)
    k = 14.0 / max(im.size)
    im = im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)
    arr = np.asarray(im).astype(np.float32)
    alpha = (arr[..., 3] > 110)
    out = np.zeros((16, 16, 4), np.uint8)
    ox, oy = (16 - im.width) // 2, (16 - im.height) // 2
    col = arr[..., :3]
    col = np.clip((col - 128.0) * 1.25 + 128.0 + 10.0, 0, 255)                           # a little contrast, so the sprite does not read as mud
    for y in range(im.height):
        for x in range(im.width):
            if alpha[y, x]:
                out[oy + y, ox + x] = (*[int(v) for v in col[y, x]], 255)
    # a one pixel dark outline round the shape
    mask2 = out[..., 3] > 0
    for y in range(16):
        for x in range(16):
            if mask2[y, x]:
                continue
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < 16 and 0 <= xx < 16 and mask2[yy, xx]:
                    out[y, x] = (14, 12, 24, 255)
                    break
    return Image.fromarray(out, "RGBA")


def write_models(rid):
    os.makedirs(MODELS, exist_ok=True)
    icon = {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}
    main = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + rid}}
    for name, obj in ((rid + "_icon.json", icon), (rid + ".json", main)):
        with open(os.path.join(MODELS, name), "w", newline="\n") as fh:
            json.dump(obj, fh, indent=2)
            fh.write("\n")


def main():
    os.makedirs(ICONS, exist_ok=True)
    import bake_all
    for it in bake_all.all_items():
        anchor = it.anchor if it.worn == "pieces" else "body"
        sprite(it.rid, anchor).save(os.path.join(ICONS, it.rid + ".png"))
        write_models(it.rid)
        print("  sprite", it.rid)


if __name__ == "__main__":
    main()
