"""Software previews of the Black Clover boss player models (docs/bc_playermodels_previews/): a turnaround sheet (front, three-quarter, side, back) per look and an overview sheet.

    python3 -B tools/bc_playermodels/preview_bc.py [id-part ...] [--out DIR] [--old]

SOFTWARE RENDERS: pmrender/pmgeo's orthographic numpy renderer (not the game): colours and lighting differ from the game's, the cutout cards have no mipmaps or fog, the extras are shown in
their idle clip at t = 0.4 s (they sway in the game), the additive glow is drawn at a fixed level, and the kit's own hair/hat layer logic is not applied (the looks have no kit hair). --old adds the
look the new one replaces on the left of each sheet.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "playermodel"))
sys.dont_write_bytecode = True

from PIL import Image, ImageDraw  # noqa: E402

import bclib as L  # noqa: E402
import preview as PV  # noqa: E402

_geo_faces = PV.G.geo_faces


def _double_sided(*a, **k):
    """The game draws the extras with the no-cull cutout type: a single-face card shows from both sides. pmrender culls back faces, so every extras face is added again turned round."""
    fs = _geo_faces(*a, **k)
    out = list(fs)
    for f in fs:
        out.append(PV.R.Face(list(reversed(f.pts)), list(reversed(f.uv)), -f.normal, f.tex, f.tint, f.emissive, f.mode))
    return out


PV.G.geo_faces = _double_sided
LOOKS_BC = os.path.join(L.ASSETS, "playermodel", "looks_bc.json")
ROOT = L.ROOT


BG = (0.78, 0.82, 0.86)


def _bbox(im):
    import numpy as np
    a = np.asarray(im, np.float32) / 255.0
    d = np.abs(a - np.array(BG, np.float32)).sum(-1) > 0.12
    d[-int(0.05 * a.shape[0]):, :] = False                      # the floor shadow
    ys = np.where(d.any(1))[0]
    xs = np.where(d.any(0))[0]
    return (ys.min(), ys.max(), xs.min(), xs.max()) if len(ys) else (0, a.shape[0] - 1, 0, a.shape[1] - 1)


def render(look, yaw, pitch=7, size=(340, 600), zoom=1.0, clip="idle", t=0.4, cy=7.5):
    s = look.get("scale", 1.0)
    sc = PV.scene_of(look, clip=clip, t=t)
    px = (7.9 / max(1.0, s * 0.92)) * zoom
    return PV.R.render(sc, size=size, yaw=yaw, pitch=pitch, center=(0, cy, 0), scale=px, floor=True)


def fit(look):
    """(zoom, centre y) that frame the figure of this look in a tile: a probe render, then the bounding box decides."""
    H = 600
    probe = render(look, 32, zoom=1.0, size=(340, H))
    y0, y1, _, _ = _bbox(probe)
    zoom = max(0.7, min(1.7, 0.86 * H / max(40.0, (y1 - y0))))
    s = look.get("scale", 1.0)
    px = (7.9 / max(1.0, s * 0.92)) * zoom
    c0 = 0.5 * (y0 + y1)
    cy = 7.5 + (c0 - H / 2.0) / (7.9 / max(1.0, s * 0.92)) * 0.0
    probe2 = render(look, 32, zoom=zoom, size=(340, H), cy=cy)
    y0, y1, _, _ = _bbox(probe2)
    cy += ((0.5 * (y0 + y1)) - H / 2.0) / px
    return zoom, cy


def sheet(name, look, old=None):
    views = [(0, 7), (32, 7), (90, 7), (180, 7)]
    zoom, cy = fit(look)
    tiles = [render(look, y, p, zoom=zoom, cy=cy) for y, p in views]
    W, H = tiles[0].size
    n = len(tiles) + (1 if old else 0)
    out = Image.new("RGB", (W * n, H + 22), (232, 236, 242))
    x = 0
    if old:
        out.paste(render(old, 32, 7, zoom=zoom, cy=cy), (0, 22))
        ImageDraw.Draw(out).text((6, 5), "before (3/4)", fill=(60, 60, 60))
        x = W
    for i, t in enumerate(tiles):
        out.paste(t, (x + i * W, 22))
    d = ImageDraw.Draw(out)
    d.text((x + 6, 5), name + "   front / three-quarter / side / back   (software render, idle pose)", fill=(20, 20, 20))
    return out


def overview(looks, title="Black Clover bosses on the player model, three-quarter view   (software renders, idle pose; not the game)"):
    """One image: every look in its three-quarter view, in rows of seven."""
    ids = list(looks)
    cols = 7
    tw, th = 200, 330
    rows = (len(ids) + cols - 1) // cols
    out = Image.new("RGB", (tw * cols, (th + 16) * rows + 22), (232, 236, 242))
    d = ImageDraw.Draw(out)
    d.text((6, 5), title, fill=(20, 20, 20))
    for i, lid in enumerate(ids):
        look = looks[lid]
        zoom, cy = fit(look)
        t = render(look, 32, 7, zoom=zoom, cy=cy).resize((tw, int(tw * 600 / 340)))
        x, y = (i % cols) * tw, 22 + (i // cols) * (th + 16)
        out.paste(t.crop((0, 0, tw, th)), (x, y + 16))
        d.text((x + 4, y + 2), lid[3:], fill=(20, 20, 20))
    return out


def main(argv):
    out_dir = os.path.join(ROOT, "docs", "bc_playermodels_previews")
    flt = []
    use_old = False
    wave2 = False
    i = 0
    while i < len(argv):
        if argv[i] == "--out":
            out_dir = argv[i + 1]
            i += 2
            continue
        if argv[i] == "--wave2":                                  # wave 2 (docs/bosses_bc_wave2.md): only its looks, into docs/bosses_bc_wave2_previews
            wave2 = True
            out_dir = os.path.join(ROOT, "docs", "bosses_bc_wave2_previews")
        elif argv[i] == "--old":
            use_old = True
        else:
            flt.append(argv[i])
        i += 1
    looks = json.load(open(LOOKS_BC))["looks"]
    if wave2:
        import bcchars2
        mine = {"bc_" + c["id"] for c in bcchars2.CHARS}
        looks = {k: v for k, v in looks.items() if k in mine}
    olds = PV.load_looks()
    os.makedirs(out_dir, exist_ok=True)
    made = []
    for lid, look in looks.items():
        if flt and not any(f in lid for f in flt):
            continue
        old = olds.get(lid[3:]) if use_old else None
        im = sheet(lid, look, old)
        p = os.path.join(out_dir, lid + ".png")
        im.save(p)
        made.append((lid, p))
        print(p)
    if not flt:
        p = os.path.join(out_dir, "overview.png")
        overview(looks, "Black Clover bosses, wave 2, three-quarter view   (software renders, idle pose; not the game)" if wave2 else None or "Black Clover bosses on the player model, three-quarter view   (software renders, idle pose; not the game)").save(p)
        print(p)
    return made


if __name__ == "__main__":
    main(sys.argv[1:])
