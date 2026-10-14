"""Weapon PAIRS as 3D models (1.29.0): one mesh per hand for Twin Hook Swords, Whirling Blades and the Twin Swords of Deflection.

    python3 -B tools/relics/pair_models.py [base_id ...]          (no ids = all three pairs; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/pair_models.py twin_hook_swords     (also renders the previews beside the cards)

The old meshes held both swords of a pair in one item. A pair is now two items (docs/relic_pairs.md): the RIGHT half keeps the card's id
(<id>.txt, so old worlds load) and the LEFT half is <id>_left. Each mesh is ONE blade built on its own axis (grip below, point above, centred on the
grip) with the owner's model method (tools/relics/mesh_lib.py): measured constants, lofted/swept forms, 8 to 12 named parts, smoothed normals, a painted
skin and a glow skin. The left half is the same build mirrored across x BEFORE the skin is laid out, so its curves, hooks and sigils face the other way,
and it is placed on the same inventory sprite as the right one (the sprite stays the inventory icon of both: it shows the pair). Vanilla mirrors the
hand pose of the off hand, so a mirrored mesh in the off hand is the true mirror image of the main-hand one.

Run the wave 9 and wave 12 icon generators first: the meshes are placed on the existing 16 x 16 sprites. Per half it writes
assets/nusmp/weapon_meshes/<id>.txt, textures/entity/weapon/<id>.png and <id>_glow.png, and (left only) textures/item/<id>_left.png (a copy of the
sprite), models/item/<id>_left.json and <id>_left_icon.json (copies of the right half's, which carry the display transforms).
"""
import json
import math
import os
import shutil
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                           # noqa: E402
import wave1_models as w                                        # noqa: E402
import wave12_models as W                                       # noqa: E402
import wave9_models as w9                                       # noqa: E402
import wave9_m_swords as sw9                                    # noqa: E402

ASSETS = ml.ASSETS
MODEL_DIR = os.path.join(ASSETS, "models", "item")
ICON_DIR = os.path.join(ASSETS, "textures", "item")


def mirror_mesh(m):
    """Mirrors every part across x (faces re-wound): the left half of a pair."""
    for p in m.parts:
        q = p.mirrored("x", name=p.name)
        p.V, p.F, p.tags = q.V, q.F, q.tags
    return m


def write_half(item_id, base_id, m, atlas, real_m, glow, ref=None):
    """Places the mesh on the pair's sprite and writes the mesh and the skins. The left half uses the right half's sprite for its placement."""
    n = w.finish(item_id, m, atlas, real_m=real_m, glow=glow, ref=ref, icon_id=base_id)
    if n > W.MAX_QUADS:
        raise SystemExit(f"{item_id}: {n} quads is over the budget of {W.MAX_QUADS}")
    return n


def left_assets(base_id):
    """The left half's item models and inventory sprite: copies of the right half's (same display transforms, same sprite)."""
    left = base_id + "_left"
    shutil.copyfile(os.path.join(ICON_DIR, base_id + ".png"), os.path.join(ICON_DIR, left + ".png"))
    for suffix in ("", "_icon"):
        src = os.path.join(MODEL_DIR, base_id + suffix + ".json")
        with open(src, "r", encoding="utf-8") as f:
            text = f.read()
        text = text.replace("nusmp:item/" + base_id + '"', "nusmp:item/" + left + '"')
        data = json.loads(text)
        with open(os.path.join(MODEL_DIR, left + suffix + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2)
            f.write("\n")


# ================================================================ Twin Hook Swords
def hook_half(left):
    """One hook sword: the spear-like butt leaf, the teal-wrapped grip with its cord, the crescent guard, the straight blade and the hook that curls
    toward the other sword. Constants as in wave12_mb.twin_hook_swords (card 736 x 752, the sword measured from its point to the end of its hook)."""
    TOTAL_M = 1.05                                       # a hook sword a little over a metre with its hook (the card shows them long)
    S_TOT = 480.0                                        # card px from the butt leaf to the end of the hook
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOT)               # cm per card px
    y = lambda s: s * K
    S_LEAF, S_GRIP, S_BLADE = (0.0, 112.0), (112.0, 198.0), (198.0, 462.0)
    LEAF_W = w.lerp_table([(0.0, 0.0), (30.0, 18.0), (78.0, 24.0), (112.0, 10.0)])    # the spear-like butt is 22 px wide at its fullest
    R_GRIP = 15.0 * K                                    # the teal wrap, about 30 px across
    BLADE_W, BLADE_T = 11.0 * K, 7.0 * K                 # the straight blade: 11 px wide
    S_CRES = (96.0, 214.0)                               # the crescent guard spans 118 px along the sword
    CRES_REACH, CRES_W = 62.0 * K, 17.0 * K              # it stands 62 px off the blade line and is 17 px thick at the belly
    R_HOOK = 14.0 * K                                    # the hook at the far end: 28 px across
    side = 1
    teal = (30, 128, 108)
    m = ml.Mesh("twin_hook_swords_left" if left else "twin_hook_swords")

    lf = w.add(m, "leaf", "steel", base=(196, 204, 214), smooth=45, uv_mode="loft")
    ml.loft(lf, "y", (0, 0, 0), [(0.0, 0.0, 0.0), (y(30), LEAF_W(30) * K, 1.0), (y(78), LEAF_W(78) * K, 1.1), (y(S_LEAF[1]) + 0.2, LEAF_W(112) * K, 0.9)], ml.lozenge(1.0, 1.0, 0.25))
    lr = w.add(m, "leaf_ridge", "steel", base=(224, 230, 238), wear=0.8)
    ml.ridge_pair(lr, (0, y(14), BLADE_T * 0.6), (0, y(100), BLADE_T * 0.55), (0, 0, 1), 0.3, 0.1, 0.05, 0.5)
    ml.ridge_pair(lr, (0, y(14), -BLADE_T * 0.6), (0, y(100), -BLADE_T * 0.55), (0, 0, -1), 0.3, 0.1, 0.05, 0.5)
    gp = w.add(m, "grip", "leather", base=teal, uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(150), 1.05, 1.05), (y(S_GRIP[1]), 0.92, 0.92)], ml.circle(R_GRIP, 10))
    cd = w.add(m, "cord", "cloth", base=(20, 92, 78), wear=0.2)
    W.wrap_rings(cd, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, R_GRIP * 1.08, 9, h=0.55, sides=10)
    ft = w.add(m, "fittings", "steel", base=(176, 184, 196), smooth=45)
    ml.collar(ft, y(S_GRIP[0]) - 0.7, R_GRIP * 1.2, 0.9, lip=0.2, sides=10)
    ml.collar(ft, y(S_GRIP[1]) - 0.2, R_GRIP * 1.2, 0.9, lip=0.2, sides=10)
    for sp in (130.0, 182.0):                            # the two posts that join the guard to the grip
        ml.lathe(ft, "x", (0, y(sp), 0), [(0.0, 0.55), (side * (CRES_REACH * 0.34 + R_GRIP), 0.55)], sides=8)
    cr = w.add(m, "crescent", "steel", base=(204, 210, 222), smooth=50)
    pts = []
    for i in range(0, 21):                               # the crescent: a bowl whose belly touches the posts and whose tips rise away from the grip
        t = i / 20.0
        pts.append((side * (R_GRIP + 1.4 + CRES_REACH * (1.0 - math.sin(math.pi * t)) * 0.9), y(S_CRES[0]) + t * (y(S_CRES[1]) - y(S_CRES[0])), 0.0))
    ml.sweep(cr, pts, ml.lozenge(1.0, 1.0, 0.35), scales=lambda t: (CRES_W * (0.12 + 0.88 * math.sin(math.pi * t) ** 0.8), 1.5 * (0.35 + 0.65 * math.sin(math.pi * t) ** 0.5)), up=(0, 0, 1))
    cs = w.add(m, "crescent_studs", "gold", base=(226, 190, 96), smooth=70)
    for t in (0.18, 0.5, 0.82):                          # three rivets along the crescent's face
        i = int(round(t * 20))
        ml.stud(cs, (pts[i][0], pts[i][1], 0.8), 0.34, 0.3, (0, 0, 1), sides=8)
        ml.stud(cs, (pts[i][0], pts[i][1], -0.8), 0.34, 0.3, (0, 0, -1), sides=8)
    bd = w.add(m, "blade", "steel", base=(184, 192, 204), smooth=45, uv_mode="loft")
    ml.loft(bd, "y", (0, 0, 0), [(y(S_BLADE[0]) - 0.4, BLADE_W * 1.3, BLADE_T * 1.2), (y(260), BLADE_W * 1.05, BLADE_T * 1.05), (y(400), BLADE_W * 0.9, BLADE_T * 0.95),
                                  (y(S_BLADE[1]), BLADE_W * 0.8, BLADE_T * 0.9)], ml.octagon(1.0, 1.0, 0.28))
    hp = []
    for i in range(0, 12):                               # the hook: the blade's end curls round toward the other sword
        a = math.radians(i * 18.5)
        hp.append((-side * R_HOOK * (1 - math.cos(a)), y(S_BLADE[1]) + R_HOOK * math.sin(a), 0.0))
    ml.sweep(bd, hp, ml.octagon(1.0, 1.0, 0.28), scales=lambda t: (BLADE_W * (0.8 - 0.55 * t), BLADE_T * (0.9 - 0.5 * t)))
    fu = w.add(m, "blade_fuller", "steel", base=(226, 232, 240), wear=0.8)
    for face in (1, -1):                                 # an engraved line down each flat
        ml.ridge_pair(fu, (0, y(S_BLADE[0] + 12), face * BLADE_T * 0.5), (0, y(S_BLADE[1] - 20), face * BLADE_T * 0.42), (0, 0, face), 0.3, 0.1, 0.05, 0.8)
    tb = w.add(m, "hook_bead", "gold", base=(236, 200, 110), smooth=60)
    w.ellipsoid(tb, hp[-1], 0.55, 0.55, 0.55, sides=8, stacks=6)
    ts = w.add(m, "tassel", "cloth", base=(26, 110, 92), wear=0.3)
    ml.lathe(ts, "y", (side * (R_GRIP * 1.1 + 0.2), y(S_GRIP[0]) + 0.2, 0), [(0.0, 0.25), (-1.2, 0.45), (-3.2, 0.6), (-5.0, 0.15)], sides=6)   # a short tassel hanging from the lower fitting

    if left:
        mirror_mesh(m)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    W.glow_part(atlas, m.part("blade"), (210, 235, 235), 0.12, core=0.1)
    W.glow_part(atlas, m.part("blade_fuller"), (150, 255, 230), 0.55, core=0.2)
    W.glow_part(atlas, m.part("hook_bead"), (255, 230, 150), 0.5, core=0.3)
    ref = w.reference(W.P3 + "faca332834adf1adfeccbb405016e64a.jpg", (80, 120, 640, 340), "twin_hook_swords")
    return m, atlas, TOTAL_M, ("FFFFFF", 0.14, 0.45, 0.8), ref


# ================================================================ Whirling Blades
def whirling_half(left):
    """One curved sword: the LEFT half is the cold cyan one (the blade curves toward -x), the RIGHT half the hot red one (mirrored). Constants as in
    wave12_ma.whirling_blades (card 736 x 920: the cyan sword from its point to the end of its spiked grip is 460 px)."""
    TOTAL_M = 1.0
    S_TOTAL = 460.0
    K = w.px_to_cm(TOTAL_M * 100.0, S_TOTAL)
    S_POM, S_GRIP, S_GUARD, S_BLADE = (0.0, 18.0), (18.0, 128.0), (122.0, 140.0), (138.0, 460.0)
    R_GRIP = 11.0 * K                                    # the twisted grip is about 22 px across
    BLADE_W = w.lerp_table([(138.0, 24.0), (190.0, 32.0), (260.0, 34.0), (340.0, 27.0), (410.0, 15.0), (460.0, 0.0)])   # leaf-shaped: widest in the lower half
    CURVE = 11.0                                         # how far the point lies off the axis, cm (the blades curve outward like scimitars)
    y = lambda s: s * K
    col = (150, 230, 224) if left else (236, 150, 118)
    tag = "cold" if left else "hot"
    m = ml.Mesh("whirling_blades_left" if left else "whirling_blades")

    bl = w.add(m, "blade", "steel", base=col, smooth=40, uv_mode="loft")
    path = [(-CURVE * (max(0.0, s - S_BLADE[0]) / (S_BLADE[1] - S_BLADE[0])) ** 2.2, y(s), 0.0) for s in np.linspace(S_BLADE[0] - 4, S_BLADE[1], 20)]
    ml.sweep(bl, path, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: (max(BLADE_W(S_BLADE[0] - 4 + t * (S_BLADE[1] - S_BLADE[0] + 4)) * K, 0.0), 0.85 * (1.0 - 0.55 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    fu = w.add(m, "blade_edge", "steel", base=(232, 238, 244), wear=0.9)
    for face in (1, -1):                                 # a bright ground line along the blade's belly (one thin swept ridge per flat)
        ml.sweep(fu, [(p[0], p[1], face * 0.36) for p in path[2:-3]], ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (0.55 * (1.0 - 0.7 * t), 0.14))
    sb = w.add(m, "sigil", "bone", base=(236, 240, 244), wear=0.1)
    W.extrude(sb, [(1.9, -1.1), (1.9, 1.1), (-1.9, 1.1), (-1.9, -1.1)], 0.3, 0.55, 0.05, at=(0.0, y(S_BLADE[0] + 36)))
    for a in (0, 90, 45, 135):                           # a sigil of crossed lines on the band
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        ml.ridge_pair(sb, (-ca * 1.5, y(S_BLADE[0] + 36) - sa * 0.9, 0.55), (ca * 1.5, y(S_BLADE[0] + 36) + sa * 0.9, 0.55), (0, 0, 1), 0.22, 0.07, 0.04, 0.4)
    gc = w.add(m, "grip", "iron", base=(52, 50, 58), uv_mode="loft", smooth=45)
    ml.loft(gc, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.8, 0.8), (y(60), 1.0, 1.0), (y(100), 1.05, 1.05), (y(S_GRIP[1]), 0.9, 0.9)], ml.circle(R_GRIP, 10))
    gs = w.add(m, "twist", "iron", base=(110, 106, 118), smooth=60, wear=0.8)
    for ph in (0.0, math.pi):                            # the twisted ridges of the grip
        hp = w.helix(lambda t: R_GRIP * 1.0, y(S_GRIP[0]) + 0.5, y(S_GRIP[1]) - 0.5, 4.0, 40, phase=ph)
        ml.sweep(gs, hp, ml.circle(0.34, 5), scales=lambda t: (1.0, 1.0))
    sk = w.add(m, "thorns", "iron", base=(60, 56, 66), smooth=70)
    for i in range(6):                                   # the thorns at the guard collar
        a = i * math.pi / 3
        W.spike(sk, (R_GRIP * 1.1 * math.cos(a), y(S_GUARD[0] + 8), R_GRIP * 1.1 * math.sin(a)), (math.cos(a) * 0.8, 0.5, math.sin(a) * 0.8), 2.6, 0.5, sides=5)
    pk = w.add(m, "pommel_spike", "iron", base=(66, 62, 74), smooth=70)
    W.spike(pk, (0, y(S_POM[0]) + 0.5, 0), (0, -1, 0), 3.8, 1.0, sides=6)
    gcol = w.add(m, "guard_collar", "iron", base=(84, 80, 92), smooth=50)
    W.spike(gcol, (0, y(S_GUARD[1]) - 0.6, 0), (0, 1, 0), 1.2, R_GRIP * 1.35, sides=10)
    rg = w.add(m, "guard_ring", "gold" if not left else "silver", base=(224, 188, 96) if not left else (206, 224, 232), smooth=45)
    W.torus(rg, (0, y(S_GUARD[0] + 4), 0), R_GRIP * 1.7, 0.28, plane="y", n=14, sides=5)       # a thin ring that guards the hand, in the blade's colour

    if not left:
        mirror_mesh(m)                                   # the hot sword is the cold one mirrored: it curves the other way
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow = (90, 255, 235) if left else (255, 110, 60)
    W.glow_part(atlas, m.part("blade"), glow, 0.55, core=0.15)
    W.glow_part(atlas, m.part("sigil"), (150, 255, 245) if left else (255, 150, 90), 0.9)
    W.glow_part(atlas, m.part("blade_edge"), glow, 0.35, core=0.3)
    W.tint_part(atlas, m.part("blade"), (120, 240, 230) if left else (240, 120, 84), 0.55)
    ref = w.reference(W.P3 + "ee57452c45139177e31fe4550eff205a.jpg", (60, 90, 700, 540), "whirling_blades")
    return m, atlas, TOTAL_M, ("FFFFFF", 0.22, 0.5, 1.0), ref


# ================================================================ Twin Swords of Deflection
def twin_half(left):
    """One hooked sword of the S-shaped pair (wave9_m_swords._t_sword: the lavender-inlaid blade with scalloped edge, the swept guard, the navy grip and
    the scalloped shell pommel), with a guard gem and a wrapped grip added. The LEFT half is the one as built (its hook flares toward +x), the RIGHT
    half is mirrored."""
    m = ml.Mesh("twin_swords_of_deflection_left" if left else "twin_swords_of_deflection")
    for part in sw9._t_sword(""):
        m.add(part)
    gm = w.add(m, "guard_gem", "crystal", base=(160, 140, 245), smooth=60)
    ml.gem(gm, (0.0, sw9.T_GUARD_Y, 0.95), 0.75, long=1.2, sides=6, axis="z")
    gw = w.add(m, "grip_wrap", "silver", base=(206, 208, 226), wear=0.3)
    W.wrap_rings(gw, sw9.T_GRIP[0] + 1.0, sw9.T_GRIP[1] - 1.0, 1.28, 6, h=0.4, sides=10)
    pc = w.add(m, "pommel_cap", "silver", base=(210, 214, 230), smooth=50)
    ml.collar(pc, sw9.T_POMMEL[1] - 0.2, 1.35, 0.7, lip=0.2, sides=10)
    if not left:
        mirror_mesh(m)
    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def lav(c):                                                     # the inlay: a scalloped lace in lighter lavender on the band, glowing softly
        n = w.noise2(c.img.shape[:2], 5, 3 + c.g.rect[0])
        c.img[..., :3] = c.img[..., :3] * (0.8 + 0.5 * n[..., None])
        w.set_glow(c, (170, 150, 255), 0.35, mask=(n > 0.5), core=0.2)
    w.paint_part(atlas, m.part("inlay"), lav)

    def blade(c):                                                   # white steel with a bright edge
        edge = (np.abs(c.S - 0.5) > 0.44)
        c.img[..., :3] = np.where(edge[..., None], np.minimum(255, c.img[..., :3] * 1.15), c.img[..., :3])
        w.set_glow(c, (190, 180, 255), 0.18, mask=edge, core=0.3)
    w.paint_part(atlas, m.part("blade"), blade)
    W.glow_part(atlas, m.part("guard_gem"), (190, 170, 255), 0.7, core=0.3)
    r = w9.ref("p2/Relics of the world part 2/6fde728ebde26f0db7f226ac7424e2f6.jpg", (20, 190, 720, 560), "twin_swords_of_deflection")
    return m, atlas, 1.0, ("FFFFFF", 0.14, 0.5, 1.0), r


PAIRS = {"twin_hook_swords": hook_half, "whirling_blades": whirling_half, "twin_swords_of_deflection": twin_half}


def build(base_id):
    for left in (False, True):
        item_id = base_id + ("_left" if left else "")
        m, atlas, real_m, glow, ref = PAIRS[base_id](left)
        write_half(item_id, base_id, m, atlas, real_m, glow, ref=ref if not left else None)
    left_assets(base_id)


def main(only=None):
    for base_id in PAIRS:
        if only and base_id not in only:
            continue
        build(base_id)


if __name__ == "__main__":
    main(sys.argv[1:])
