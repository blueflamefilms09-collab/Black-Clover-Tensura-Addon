"""The 3D models of the two Wind Spirit blades (creative display items): Spirit of Zephyr (sword) and Spirit of Boreas (bow), built with the owner's model method
(tools/relics/mesh_lib.py, the kit of tools/relics/new197_*.py), to the Genesis Demon-Slayer's standard: real cross-sections, named parts, a painted skin, an additive glow skin.

    python3 -B tools/gen_weapon_models.py spirit_blades        (the hook of the weapon generator; this file can also be run directly)
    python3 -B tools/gen_spirit_blades_models.py               deterministic, idempotent
    N197_PREVIEW=<dir> python3 -B tools/gen_spirit_blades_models.py   also renders the kit's mesh preview

Looks (the Wind Spirit Dive pack, docs/magic/spirit_dives_wind.md):
  Spirit of Zephyr   a long glowing mint-green blade (flat blade, bright edge bevel, deep glowing fuller on both faces), a cross-guard whose quillons curve UP and end in leaf points,
                     two crossed rings (a big one facing the viewer, a smaller one across it) turning round the hilt, a wrapped green grip and a glowing gem pommel with a cross.
  Spirit of Boreas   a bow of two LEAF limbs (a leaf section: widest in the middle), a cross ornament on each limb, a slim riser, a thin string of light and an arrow of light.
It writes (through new197_lib.finish / new197_ranged.finish_bow): assets/nusmp/weapon_meshes/<id>.txt (+ the bow's _pull1..3 states), textures/entity/weapon/<id>.png and _glow.png,
textures/item/<id>.png (the 16 x 16 inventory sprite rendered from the finished mesh, kept as the inventory icon), models/item/<id>.json and <id>_icon.json, then the class rows (both
copies of weapon_classes.json) and the display presets. The json files with other weapons' rows are changed by text insertion only: every other row stays byte for byte.
Also writes the small leaf sprite of the hand effect (textures/entity/weapon/spirit_blades_leaf.png).
"""
import json
import math
import os
import re
import sys

import numpy as np

_TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(_TOOLS, "relics"))

from new197_kit import *                                                   # noqa: F401,F403,E402
from new197_kit import (P, rod, lathe_y, orb, crystal, bar, tube, wrap, ml, w, paint, put_glow, collar_ring, new_mesh, new_atlas, lay_and_paint, finish,   # noqa: E402
                        straight_sword, grip)
from new197_god import helix_pts                                           # noqa: E402
from new197_legend import glow_parts, fuller_pair                          # noqa: E402

REPO = os.path.abspath(os.path.join(_TOOLS, ".."))
GAME_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
SWORD_ID = "spirit_of_zephyr"
BOW_ID = "spirit_of_boreas"

MINT = (150, 238, 204)
PALE = (232, 255, 244)
TEAL = (84, 214, 170)
DEEP = (40, 150, 120)
GLOW_RGB = (170, 255, 214)


def ring_pts(c, ex, ey, R, n=28):
    """A closed polyline: a circle of radius R round c in the plane spanned by ex and ey."""
    return [tuple(c[k] + R * (math.cos(a) * ex[k] + math.sin(a) * ey[k]) for k in range(3)) for a in np.linspace(0, 2 * math.pi, n + 1)]


def build_sword():
    m = new_mesh(SWORD_ID)
    L0, L1 = 26.0, 128.0                                                   # the guard line and the point (cm)
    # the blade: a flat, long leaf-straight plate with a bright bevel, widest near the guard, a long taper to the point
    st = [(L0, -2.9, 2.9, 0.62), (L0 + 6, -3.2, 3.0, 0.6), (62, -3.1, 2.9, 0.54), (96, -2.6, 2.5, 0.44), (114, -1.7, 1.6, 0.34), (123, -0.8, 0.8, 0.22), (L1, 0.0, 0.0, 0.0)]
    straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=10.5, t=1.25, h=1.2, bulb=1.1, curve=4.2), grip_len=22.0, pommel_style="orb", pommel_kw=dict(r=2.2),
                   blade_material="steel", blade_base=MINT, guard_material="silver", guard_base=PALE, grip_base=DEEP, wrap_base=TEAL, bevel=0.6, glow_blade=False)
    fuller_pair(m, "fuller", L0 + 7, 112.0, 1.15, base=(200, 255, 232), material="silver")
    # leaf-pointed quillon tips (the guard curves up; a small leaf on each end)
    for k, sx in enumerate((-1, 1)):
        bar(m, "quillon_leaf_%d" % k, "silver", (sx * 10.2, L0 + 4.4, 0.0), (sx * 12.4, L0 + 10.5, 0.0), 1.2, 0.55, up=(0, 0, 1), taper=3.0, base=PALE, end_scale=0.05)
    # the gem and the cross on the pommel
    orb(m, "pommel_gem", "crystal", (0.0, L0 - 22.0 - 3.2, 0.0), 1.9, base=(190, 255, 225), glow=True)
    bar(m, "pommel_cross_v", "silver", (0.0, L0 - 22.0 - 7.0, 0.0), (0.0, L0 - 22.0 - 0.6, 0.0), 0.35, 0.5, up=(0, 0, 1), taper=0.0, base=PALE)
    # the two crossed rings round the hilt (glowing): one facing the viewer (xy plane), one across it (yz plane)
    c = (0.0, L0 - 0.5, 0.0)
    tube(m, "ring_a", "silver", ring_pts(c, (1, 0, 0), (0, 1, 0), 10.5), 0.5, 0.5, sides=6, base=(210, 255, 236), glow=True)
    tube(m, "ring_b", "silver", ring_pts(c, (0, 0, 1), (0, 1, 0), 8.2), 0.45, 0.45, sides=6, base=(200, 255, 230), glow=True)
    tube(m, "ring_c", "silver", ring_pts(c, (1, 0, 0), (0, 0, 1), 5.2), 0.4, 0.4, sides=6, base=(200, 255, 230), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["fuller_l", "fuller_r"] if _has(m, "fuller_l") else [p.name for p in m.parts if p.name.startswith("fuller")], GLOW_RGB, 1.0, 0.6)
    glow_parts(atlas, m, ["ring_a", "ring_b", "ring_c", "pommel_gem"], GLOW_RGB, 1.0, 0.6)
    glow_parts(atlas, m, ["quillon_leaf_0", "quillon_leaf_1"], (200, 255, 232), 0.7, 0.4)
    try:
        glow_parts(atlas, m, ["blade"], (150, 255, 205), 0.35, 0.25)       # a faint green light in the whole blade
    except Exception:
        pass
    finish(SWORD_ID, m, atlas, "longsword", glow=("A8FFD2", 0.18, 0.45, 1.0))
    add_gripxyz(SWORD_ID, 0.0, L0 - 11.5, 0.0)


def _has(m, name):
    try:
        m.part(name)
        return True
    except Exception:
        return False


def build_bow():
    import wave23_models as wm
    from new197_ranged import finish_bow
    HALF, BELLY = 62.0, 20.0
    m = new_mesh(BOW_ID)
    up = wm.limb_points(1, HALF, BELLY, n=22, y0=3.0, power=1.7, tip_curl=6.0)
    lo = wm.limb_points(-1, HALF, BELLY, n=22, y0=3.0, power=1.7, tip_curl=6.0)
    for nm, pts in (("limb_upper", up), ("limb_lower", lo)):
        p = P(m, nm, "silver", base=MINT, smooth=55, uv_mode="loft")

        def sc(t):
            if t > 0.985:
                return (0.0, 0.0)
            leaf = math.sin(math.pi * min(1.0, t * 0.92 + 0.08)) ** 0.8          # a leaf: narrow at the grip, widest in the middle, a point at the tip
            return (0.7 + 3.4 * leaf, 0.62 + 0.5 * leaf)
        ml.sweep(p, pts, ml.ellipse(1.0, 1.0, 8), scales=sc, up=(0, 0, 1))
    # a bright vein down the middle of each leaf limb (glow) and a cross ornament on each limb
    vein = P(m, "limb_vein", "crystal", base=(190, 255, 226), glow=True, smooth=30)
    V = np.array
    for pts in (up, lo):
        n = len(pts) - 1
        ml.bar(vein, V(pts[3]) + V((0, 0, 1.05)), V(pts[n - 3]) + V((0, 0, 0.55)), 0.42, 0.14, up=(0, 0, 1), taper=3.0)
        ml.bar(vein, V(pts[3]) + V((0, 0, -1.05)), V(pts[n - 3]) + V((0, 0, -0.55)), 0.42, 0.14, up=(0, 0, -1), taper=3.0)
    cross = P(m, "limb_cross", "silver", base=PALE, glow=True, smooth=30)
    for pts in (up, lo):
        i = 11
        cx, cy, _ = pts[i]
        sgn = 1 if cy > 0 else -1
        ml.bar(cross, (cx + 0.0, cy - 3.6 * sgn, 1.35), (cx + 0.0, cy + 3.6 * sgn, 1.35), 0.4, 0.3, up=(0, 0, 1), taper=0.0)
        ml.bar(cross, (cx - 2.5, cy + 0.8 * sgn, 1.35), (cx + 2.5, cy + 0.8 * sgn, 1.35), 0.4, 0.3, up=(0, 0, 1), taper=0.0)
    # a slim riser, wrapped, with a small crossed ring at the belly
    gp = P(m, "grip", "leather", base=DEEP, smooth=50, uv_mode="loft")
    ml.loft(gp, "y", (BELLY, 0, 0), [(-6.0, 1.0, 1.0), (-4.0, 1.7, 1.7), (4.0, 1.7, 1.7), (6.0, 1.0, 1.0)], ml.circle(1.0, 10))
    tube(m, "grip_ring", "silver", ring_pts((BELLY + 1.6, 0.0, 0.0), (0, 1, 0), (0, 0, 1), 3.3, 20), 0.34, 0.34, sides=5, base=(210, 255, 236), glow=True)
    orb(m, "grip_gem", "crystal", (BELLY + 1.0, 0.0, 1.8), 1.3, base=(190, 255, 225), glow=True)
    wm.string_part(m, HALF, 0.17, base=(226, 255, 240))
    wm.arrow_parts(m, length=66.0, z=2.2, head="crystal", head_base=(210, 255, 232), fletch=(190, 255, 220), glow_head=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["limb_vein", "limb_cross", "grip_ring", "grip_gem", "arrow_head"], GLOW_RGB, 1.0, 0.6)
    glow_parts(atlas, m, ["string"], (220, 255, 240), 0.7, 0.5)
    glow_parts(atlas, m, ["arrow_shaft"], (170, 255, 214), 0.8, 0.5)
    finish_bow(BOW_ID, m, atlas, HALF, BELLY, flex=8.0, max_draw=24.0, glow=("A8FFD2", 0.16, 0.45, 1.0))


def add_gripxyz(item_id, x, y, z):
    """A `gripxyz x y z` line in the mesh (docs/weapon_grip.md): the hand holds the middle of the wrapped grip, not a class fraction."""
    path = os.path.join(GAME_ASSETS, "weapon_meshes", item_id + ".txt")
    lines = open(path, encoding="utf-8").read().split("\n")
    lines = [ln for ln in lines if not ln.startswith("gripxyz")]
    at = 0
    for i, ln in enumerate(lines):
        if ln.startswith("place") or ln.startswith("glow") or ln.startswith("grip "):
            at = i + 1
    lines.insert(at, "gripxyz %.3f %.3f %.3f" % (x, y, z))
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


def leaf_sprite():
    """The drifting leaf of the hand effect: a 32 x 32 pointed leaf with a bright vein, mint, black under the transparent texels (it is drawn additively)."""
    from PIL import Image
    n = 32
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    u = (xx + 0.5) / n * 2 - 1
    v = (yy + 0.5) / n * 2 - 1
    # a leaf along the diagonal
    a = (u + v) / math.sqrt(2)
    b = (u - v) / math.sqrt(2)
    half = 0.78 * np.clip(1.0 - (np.abs(a) / 0.95) ** 1.6, 0, 1) ** 0.8
    inside = np.abs(b) < half
    vein = np.abs(b) < 0.05
    alpha = np.where(inside, 0.85, 0.0)
    alpha = np.where(inside & vein, 1.0, alpha)
    edge = np.clip((half - np.abs(b)) / 0.12, 0, 1)
    alpha = alpha * edge
    img = np.zeros((n, n, 4), np.float32)
    k = np.where(vein, 1.0, 0.72)
    img[..., 0] = alpha * 0.66 * k
    img[..., 1] = alpha * 1.0 * k
    img[..., 2] = alpha * 0.84 * k
    img[..., 3] = alpha
    path = os.path.join(GAME_ASSETS, "textures", "entity", "weapon", "spirit_blades_leaf.png")
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA").save(path)


def class_rows():
    """The rows of the two weapons in both copies of weapon_classes.json, by text insertion (the files keep their formatting and every other row)."""
    rows = ((SWORD_ID, "longsword", "vorpal_sword"), (BOW_ID, "bow", "wisp_staff"))
    game = os.path.join(GAME_ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8", newline="").read()
    for wid, cls, _ in rows:
        row = '    "%s": "%s",' % (wid, cls)
        if re.search(r'^    "%s": "[^"]*",' % wid, text, re.M):
            text = re.sub(r'^    "%s": "[^"]*",' % wid, row, text, flags=re.M)
        else:
            text = re.sub(r'(^    "wisp_staff": "staff",)', lambda mt: row + "\n" + mt.group(1), text, count=1, flags=re.M)
    open(game, "w", encoding="utf-8", newline="").write(text)
    tools = os.path.join(REPO, "tools", "relics", "weapon_classes.json")
    text = open(tools, encoding="utf-8", newline="").read()
    for wid, cls, _ in rows:
        block = '    "%s": {\n      "class": "%s"\n    },' % (wid, cls)
        if '"%s": {' % wid in text:
            text = re.sub(r'    "%s": \{\s*"class": "[^"]*"\s*\},' % wid, lambda mt: block, text)
        else:
            text = re.sub(r'(    "wisp_staff": \{)', lambda mt: block + "\n" + mt.group(1), text, count=1)
    open(tools, "w", encoding="utf-8", newline="").write(text)


def presets(item_id):
    """The oversized-weapon display rule for one weapon (the rows of every other weapon stay as they are, byte for byte): see tools/relics/display_presets.py."""
    import display_presets as dp
    mesh = dp.read_mesh(os.path.join(dp.MESHES, item_id + ".txt"))
    mjson = os.path.join(dp.MODELS, item_id + ".json")
    raw = open(mjson, encoding="utf-8").read()
    model = json.loads(raw)
    plan = dp.plan_for(item_id, model, mesh)
    entry = {"id": item_id, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
    if plan["action"] == "preset":
        entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                     ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
    if plan["action"] in ("preset", "restore"):
        new = dp.apply_plan(model, plan)
        text = json.dumps(new, indent=2)
        if text != raw:
            open(mjson, "w", encoding="utf-8", newline="\n").write(text)
    text = open(dp.MANIFEST, encoding="utf-8", newline="").read()
    frag = "\n".join("    " + ln for ln in json.dumps(entry, indent=2).split("\n"))
    existing = re.search(r'    \{\n      "id": "%s",.*?\n    \}' % item_id, text, re.S)
    if existing:
        text = text[:existing.start()] + frag + text[existing.end():]
    else:
        at = None
        for mt in re.finditer(r'    \{\n      "id": "([^"]+)",', text):
            if mt.group(1) > item_id:
                at = mt.start()
                break
        if at is None:
            raise SystemExit("no place for the manifest row")
        text = text[:at] + frag + ",\n" + text[at:]
    open(dp.MANIFEST, "w", encoding="utf-8", newline="").write(text)
    return entry


def main(args=None):
    sys.path.insert(0, _TOOLS)
    build_sword()
    build_bow()
    leaf_sprite()
    class_rows()
    for wid in (SWORD_ID, BOW_ID):
        e = presets(wid)
        print("  %s L=%.2f %s" % (wid, e["length_blocks"], e["action"]))


if __name__ == "__main__":
    main(sys.argv[1:])
