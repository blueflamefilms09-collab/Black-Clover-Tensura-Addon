"""Relics of Many Worlds, second set, wave 12: the ten hand-held weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave12_models.py [id ...]          (no ids = all ten; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave12_models.py warmages_longsword     (also renders the preview beside the card)

Run tools/relics/wave12_icons.py first: every mesh is placed on its 16 x 16 sprite (which stays the inventory icon). Per id it writes
assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png (512 px skins).

The method, in order, for every weapon: 0 target (the card's art and text), 1 measure the card (every proportion is a NAMED CONSTANT with a comment
saying where on the card it came from), 2 lofted geometry from real cross-sections (never a box or a four-sided frustum for a long form), 3 the
small raised parts as their own named Parts (8 to 14 per weapon), 4 smoothed normals and projected UVs, 5 a painted skin plus a glow skin,
6 a preview beside the card. Budget (addendum of the second set): 8 to 14 parts and at most 2800 quads per mesh.
The builders live in wave12_ma.py (jawblade, lumberjack, whip, blades, warmage) and wave12_mb.py (crystal, stabshroom, hooks, bottle, dagger).
"""
import importlib
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault("RELICS_ZIP", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics2")
import mesh_lib as ml                                           # noqa: E402
import wave1_models as w                                        # noqa: E402

P2 = "p2/Relics of the world part 2/"
P3 = "p3/Relics of the world part 3/"
MAX_QUADS = 2800

MODELS = ["giant_jawblade", "the_lumberjack", "white_whip_of_welting", "whirling_blades", "warmages_longsword",
          "sword_crystal", "the_stabshroom", "twin_hook_swords", "bottle_blade", "disintegration_dagger"]
MODULES = {i: ("wave12_ma" if n < 5 else "wave12_mb") for n, i in enumerate(MODELS)}


# ================================================================ shared helpers
def spike(part, origin, direction, length, r, sides=6, belly=0.0):
    """A cone / thorn from `origin` along `direction` (a tooth, a claw, a studded point); belly > 0 swells the middle."""
    st = [(0.0, r, r), (length * 0.35, r * (1.0 + belly), r * (1.0 + belly)), (length, 0.0, 0.0)]
    return ml.loft(part, np.asarray(direction, float), origin, st, ml.circle(1.0, sides))


def torus(part, centre, radius, tube, plane="z", n=18, sides=6):
    """A ring (a guard ring, a buckle) lying in the xy plane ('z': facing the viewer) or the xz plane ('y': a washer)."""
    cx, cy, cz = centre
    path = []
    for i in range(n + 1):
        a = 2 * math.pi * (i % n) / n
        path.append((cx + radius * math.cos(a), cy + radius * math.sin(a), cz) if plane == "z" else (cx + radius * math.cos(a), cy, cz + radius * math.sin(a)))
    return ml.sweep(part, path, ml.circle(tube, sides), caps=(False, False))


def wrap_rings(part, y0, y1, r, n, h=0.45, sides=12, oval=1.0, x=0.0, z=0.0):
    """n thin raised rings between y0 and y1 (a cord or leather binding)."""
    for i in range(n):
        ml.collar(part, y0 + (y1 - y0 - h) * i / max(1, n - 1), r, h, lip=h * 0.3, sides=sides, oval=oval, origin=(x, 0, z))


def curve(f, y0, y1, n, z=0.0):
    """Path points (f(y), y, z) from y0 to y1."""
    return [(f(y0 + (y1 - y0) * i / n), y0 + (y1 - y0) * i / n, z) for i in range(n + 1)]


def glow_part(atlas, part, rgb, strength=1.0, core=0.3):
    """The whole part also shines on the additive glow skin."""
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))


def tint_part(atlas, part, rgb, k=0.7):
    w.paint_part(atlas, part, lambda c: w.tint(c.img, rgb, k))


def extrude(part, outline, z0, z1, bevel=0.3, at=(0.0, 0.0)):
    """A flat plate: the (x, y) outline (counter-clockwise) extruded from z0 to z1 with softly chamfered faces (a head, a guard, a crescent)."""
    zc, h = (z0 + z1) / 2, (z1 - z0) / 2
    st = [(-h, 0.94, 0.94), (-h + bevel, 1.0, 1.0), (h - bevel, 1.0, 1.0), (h, 0.94, 0.94)]
    return ml.loft(part, "z", (at[0], at[1], zc), st, list(outline))


def planar(part, path, width, thick, taper=(1.0, 0.0), up=(0, 0, 1)):
    """A flat strip along a path (x, y, z), `width` wide, `thick` through, tapering from taper[0] to taper[1] of its width (a curved blade, a ribbon)."""
    n = len(path)
    return ml.sweep(part, path, ml.lozenge(1.0, 1.0, 0.3), scales=lambda t: (width * (taper[0] + (taper[1] - taper[0]) * t) + 1e-4, thick), up=up)


def finish(item_id, m, atlas, real_m, glow=("FFFFFF", 0.2, 0.55, 1.0), ref=None, **kw):
    """Writes the mesh and the skins through the shared wave 1 finisher and refuses a mesh over the quad budget."""
    n = w.finish(item_id, m, atlas, real_m=real_m, glow=glow, ref=ref, **kw)
    if n > MAX_QUADS:
        raise SystemExit(f"{item_id}: {n} quads is over the budget of {MAX_QUADS}")
    return n


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        importlib.import_module(MODULES[item_id]).build(item_id)


if __name__ == "__main__":
    main(sys.argv[1:])
