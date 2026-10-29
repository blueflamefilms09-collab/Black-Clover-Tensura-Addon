"""Celestial Spirit Magic: the 3D in-hand models (weapon meshes) of the 12 Gold Zodiac keys, the 4 silver keys, the gate key, the skill book and the sceptre of the Spirit
King, built with the model method of tools/relics/mesh_lib.py and placed on their 16 x 16 icons (tools/celestial/gen_celestial_icons.py, run it first).

    python3 -B tools/celestial/gen_celestial_keys.py [id ...]          (no ids = all 19; idempotent, deterministic)

Per item this writes assets/nusmp/weapon_meshes/<id>.txt, textures/entity/weapon/<id>.png (512 px skin) and <id>_glow.png (the additive layer: the gem, the sign, the star).
The meshes carry `orient manual` (hand placed on their icon like the bows; the runtime normaliser leaves them alone).
A key: a round bow (a swept torus) with the gem of its spirit in the middle and the sign of the spirit round it (the same pixels as the icon, as little raised blocks), a round
shaft with two collars, and a bit of three teeth. The book: two covers, a spine, the pages, a gold star clasp, corner caps and a ribbon. The sceptre: a long gold staff with a
ferrule, a cage of four bars round a glowing orb, and a crown of eight rays.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
import mesh_lib as ml            # noqa: E402
import wave1_models as w         # noqa: E402
from celestial_data import KEYS, rgb  # noqa: E402
from gen_celestial_icons import EXTRAS  # noqa: E402

BASE = {"gold": (212, 168, 66), "silver": (196, 202, 216), "gate": (244, 228, 164)}
R_BOW, TUBE, YB = 2.3, 0.48, 11.2          # bow radius, its tube radius, the height of its centre (cm)
PX = 1.1                                   # one icon pixel in cm


def ring_path(cx, cy, r, n=28, z=0.0):
    return [(cx + r * math.cos(2 * math.pi * i / n), cy + r * math.sin(2 * math.pi * i / n), z) for i in range(n + 1)]


def build_key(kid, rank, colour):
    item_id = kid + "_key"
    m = ml.Mesh(item_id)
    base = BASE[rank]
    metal = "silver" if rank == "silver" else "gold"
    gem_rgb = rgb(colour)
    ring = w.add(m, "bow_ring", metal, base=base, smooth=50)
    ml.sweep(ring, ring_path(0, YB, R_BOW), ml.circle(TUBE, 8), caps=(False, False))
    sign = w.add(m, "bow_sign", metal, base=tuple(min(255, int(c * 1.05)) for c in base), smooth=20)
    for (dx, dy) in EXTRAS[kid]:
        x, y = (dx - 3) * PX, YB - (dy - 3) * PX
        ml.box(sign, (x - PX * 0.42, y - PX * 0.42, -0.42), (x + PX * 0.42, y + PX * 0.42, 0.42), chamfer=0.1)
    gem = w.add(m, "gem", "crystal", base=tuple(gem_rgb), smooth=0)
    ml.gem(gem, (0, YB, 0), 1.05, long=0.55, sides=6, axis="z")
    shaft = w.add(m, "shaft", metal, base=tuple(int(c * 0.96) for c in base), smooth=45)
    ml.loft(shaft, "y", (0, 0, 0), [(0.6, 1.0, 1.0), (YB - R_BOW - TUBE + 0.2, 1.0, 1.0)], ml.circle(0.4, 10))
    cols = w.add(m, "collars", metal, base=tuple(min(255, int(c * 1.08)) for c in base), smooth=40)
    for s, r in ((YB - R_BOW - 1.0, 0.72), (5.6, 0.62), (4.7, 0.55)):
        ml.collar(cols, s, r, 0.5, lip=0.12, sides=12)
    bit = w.add(m, "bit", metal, base=tuple(int(c * 0.92) for c in base), smooth=0)
    seed = sum(ord(ch) for ch in kid)
    for i in range(3):                                                         # three teeth, their lengths from the name of the spirit
        ln = 1.2 + ((seed >> i) % 3) * 0.55
        y0 = 0.4 + i * 1.15
        ml.box(bit, (0.3, y0, -0.34), (0.3 + ln, y0 + 0.85, 0.34), chamfer=0.07)
    ml.box(bit, (0.0, 0.2, -0.34), (0.5, 3.8, 0.34), chamfer=0.07)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("gem"), lambda c: w.set_glow(c, gem_rgb, 0.95, core=0.5))
    w.paint_part(atlas, m.part("bow_sign"), lambda c: w.set_glow(c, gem_rgb, 0.35, core=0.2))
    w.paint_part(atlas, m.part("bow_ring"), lambda c: w.set_glow(c, gem_rgb, 0.12, core=0.1))
    m.orient_cfg = "manual"
    return w.finish(item_id, m, atlas, real_m=0.30, glow=("FFFFFF", 0.10, 0.5, 1.0))


def build_sceptre():
    item_id = "spirit_king_sceptre"
    m = ml.Mesh(item_id)
    L = 34.0
    st = w.add(m, "staff", "gold", base=(214, 172, 70), smooth=45)
    ml.loft(st, "y", (0, 0, 0), [(0.5, 1.0, 1.0), (L, 1.0, 1.0)], ml.circle(0.55, 10))
    fe = w.add(m, "ferrule", "gold", base=(238, 202, 104), smooth=40)
    ml.lathe(fe, "y", (0, 0, 0), [(0.0, 0.2), (0.5, 0.85), (1.6, 0.7), (2.0, 0.55)], sides=10)
    ml.collar(fe, 10.0, 0.9, 0.7, lip=0.2, sides=12)
    ml.collar(fe, 20.0, 0.9, 0.7, lip=0.2, sides=12)
    cage = w.add(m, "cage", "gold", base=(236, 196, 96), smooth=45)
    cy = L + 3.2
    for i in range(4):                                                         # four bars curving round the orb
        a = i * math.pi / 2 + math.pi / 4
        path = [(math.cos(a) * r, y, math.sin(a) * r) for r, y in ((0.7, L - 0.5), (2.7, L + 0.9), (3.4, cy), (2.7, L + 5.5), (0.7, L + 6.9))]
        ml.sweep(cage, path, ml.circle(0.26, 6))
    ml.collar(cage, L - 0.9, 1.1, 0.8, lip=0.2, sides=12)
    ml.collar(cage, L + 6.6, 1.0, 0.7, lip=0.2, sides=12)
    orb = w.add(m, "orb", "crystal", base=(255, 246, 214), smooth=70, uv_mode="loft")
    w.ellipsoid(orb, (0, cy, 0), 2.2, 2.2, 2.2, sides=14, stacks=10)
    rays = w.add(m, "rays", "gold", base=(255, 224, 130), smooth=0)
    for i in range(8):
        a = i * math.pi / 4
        d = np.array([math.cos(a), 0.55 if i % 2 == 0 else 0.25, math.sin(a)])
        d /= np.linalg.norm(d)
        base = (math.cos(a) * 0.6, L + 7.0, math.sin(a) * 0.6)
        ml.loft(rays, tuple(d), base, [(0.0, 0.8, 0.8), (1.0, 0.5, 0.5), (3.0 if i % 2 == 0 else 2.0, 0.0, 0.0)], ml.circle(0.45, 4))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("orb"), lambda c: w.set_glow(c, (255, 244, 200), 1.0, core=0.6))
    w.paint_part(atlas, m.part("rays"), lambda c: w.set_glow(c, (255, 226, 130), 0.7, core=0.3))
    w.paint_part(atlas, m.part("cage"), lambda c: w.set_glow(c, (255, 214, 110), 0.25, core=0.2))
    m.orient_cfg = "manual"
    return w.finish(item_id, m, atlas, real_m=0.95, glow=("FFFFFF", 0.10, 0.5, 1.0))


def main(only=None):
    for kid, _name, rank, colour in KEYS:
        if only and kid + "_key" not in only:
            continue
        build_key(kid, rank, colour)
    if not only or "spirit_king_sceptre" in only:
        build_sceptre()


if __name__ == "__main__":
    main(sys.argv[1:])
