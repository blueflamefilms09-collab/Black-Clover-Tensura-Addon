"""Relics of Many Worlds, second card set, wave 9: the ten hand-held weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave9_models.py [id ...]          (no ids = all ten; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave9_models.py caesars_gladius      (also renders the preview beside the card)

Run tools/relics/wave9_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png; the crossbow also
writes <id>_pull1..3 and <id>_charged (the draw and loaded states), sharing its skin.

Budget (the game's first wave was flagged for 5000 to 7000 quads per mesh): 8 to 14 named parts per weapon, at most 2800 quads, one 512 px skin and glow.

The method, in order, for every weapon (each wave9_m_*.py module follows it):
  0 target decided: the card's art and the printed text.
  1 measure the reference: pixels along one known length -> centimetres; every proportion is a NAMED CONSTANT with a comment saying where it came from.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form.
  3 the small raised parts are the detail: each is its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material plus a second additive glow skin.
  6 rendered beside the reference (W1_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.
"""
import importlib
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                           # noqa: E402
import wave1_models as w                                        # noqa: E402

ZIP2 = os.environ.get("RELICS2_DIR", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics2")
ATLAS_PX = 512
MAX_QUADS = 2800


def ref(file, box, out_name):
    """Crops the card art (relative to scratchpad/relics2) for the preview; None when the images are not around or no preview is asked for."""
    src = os.path.join(ZIP2, file)
    prev = os.environ.get("W1_PREVIEW")
    if not prev or not os.path.exists(src):
        return None
    os.makedirs(prev, exist_ok=True)
    out = os.path.join(prev, out_name + "_ref.png")
    Image.open(src).convert("RGBA").crop(box).save(out)
    return out


def finish(item_id, m, atlas, real_m, glow=("FFFFFF", 0.12, 0.5, 1.0), ref_png=None, fit=False, **kw):
    """Places the mesh on the sprite, writes the .txt and the skins (wave1_models.finish), and checks the quad budget."""
    n = w.finish(item_id, m, atlas, real_m=real_m, fit=fit, glow=glow, ref=ref_png, **kw)
    if n > MAX_QUADS:
        raise SystemExit(f"{item_id}: {n} quads, over the budget of {MAX_QUADS}")
    return n


# ---------------------------------------------------------------- shared shapes
def spike(part, base, direction, length, r, sides=6, belly=0.6):
    """A cone / thorn from `base` along `direction`: radius r at the root, thinning to a point (belly < 1 keeps it fat near the root)."""
    return ml.loft(part, direction, base, [(-length * 0.1, r), (0.0, r), (length * 0.45, r * belly), (length, 0.0)], ml.circle(1.0, sides))


def leaf(part, base, direction, length, width, thick, up=(0, 0, 1), sections=6):
    """A flat leaf / feather plate from `base` along `direction`, swelling then closing to a tip; `width` across, `thick` through (along `up`)."""
    sec = ml.ellipse(0.5, 0.5, sections)
    st = [(0.0, width * 0.35, thick * 0.6), (length * 0.18, width * 0.8, thick), (length * 0.55, width, thick), (length * 0.85, width * 0.6, thick * 0.7), (length, 0.0, 0.0)]
    return ml.loft(part, direction, base, st, sec, up=up)


def wedge_x(part, profile, section=None, caps=(True, True)):
    """A plate lofted along +x from a profile [(x, y_top, y_bottom, thickness)]: the head of an axe (the edge is the last station), a fin, a wing.
    Sections are the flat-faced octagon by default, so the cheeks stay flat and only the thickness tapers toward the edge."""
    st = [(x, max(1e-3, yt - yb), max(0.06, t), (yt + yb) / 2.0, 0.0) for (x, yt, yb, t) in profile]
    return ml.loft(part, "x", (0, 0, 0), st, section or ml.octagon(1.0, 1.0, 0.22), caps=caps)


def arc_points(cx, cy, r, a0, a1, n, z=0.0):
    """Points of a circular arc in the xy plane (angles in degrees, 0 = +x)."""
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n)), z) for i in range(n + 1)]


def bez3(p0, p1, p2, p3, n=16):
    """Points of a cubic bezier through 3D control points."""
    out = []
    for i in range(n + 1):
        t = i / n
        a, b, c, d = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t * t, t ** 3
        out.append(tuple(a * p0 + b * p1 + c * p2 + d * p3 for p0, p1, p2, p3 in zip(p0, p1, p2, p3)))
    return out


def stars(img, glow, seed, count, glow_rgb=(210, 220, 255), p_glow=0.4):
    """Star speckles on a painted patch (img / glow are float RGBA views)."""
    rng = np.random.default_rng(seed)
    h, wd = img.shape[:2]
    for _ in range(count):
        y, x = int(rng.integers(0, h)), int(rng.integers(0, wd))
        img[y, x, :3] = (236, 240, 255)
        if rng.random() < p_glow:
            glow[y, x] = (glow_rgb[0], glow_rgb[1], glow_rgb[2], 200)


MODELS = ["hyperborean_crossbow", "druidic_blade", "axe_of_the_exile", "astralaria", "null_blade", "viper_whip", "caesars_gladius",
          "twin_swords_of_deflection", "hunger", "thornblight"]
MODULES = {"hyperborean_crossbow": "wave9_m_ranged", "viper_whip": "wave9_m_ranged",
           "druidic_blade": "wave9_m_swords", "caesars_gladius": "wave9_m_swords", "twin_swords_of_deflection": "wave9_m_swords", "null_blade": "wave9_m_swords",
           "axe_of_the_exile": "wave9_m_axes", "astralaria": "wave9_m_axes", "hunger": "wave9_m_axes", "thornblight": "wave9_m_axes"}


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        getattr(importlib.import_module(MODULES[item_id]), "build_" + item_id)()


if __name__ == "__main__":
    main(sys.argv[1:])
