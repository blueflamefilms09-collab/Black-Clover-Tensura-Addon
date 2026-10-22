"""The 3D model of the Stormcaller Staff (Storm Magic, Fairy Tail), built with the owner's model method (tools/relics/mesh_lib.py, the kit of tools/relics/new197_*.py).

    python3 -B tools/gen_weapon_models.py storm_staff           (the hook of the weapon generator; this file can also be run directly)
    python3 -B tools/gen_storm_staff_model.py                   deterministic, idempotent
    N197_PREVIEW=<dir> python3 -B tools/gen_storm_staff_model.py   also renders the kit's mesh preview

The staff follows the Genesis Demon-Slayer's method: real cross-sections (a lathed crown, lofted shafts, swept horns), named parts, a painted skin, an additive glow skin. Look: a tall
lacquered slate-blue staff wound with a twisted silver wire of gust; a silver crown cup that holds a THUNDERCLOUD (a dark belly under a rolling grey-white top of lobes, flickering blue light
inside them) round a glowing storm core, four silver horns of wind curling up round it, a long silver LIGHTNING ROD rising out of the cloud with fins and a glowing tip, six drops of
glowing rain crystal hung under the rim, and a bolt of glow inlay zig-zagging down the shaft. It writes (through new197_lib.finish): assets/nusmp/weapon_meshes/stormcaller_staff.txt
(+ a `gripxyz` line: the hand holds the wrapped grip), textures/entity/weapon/stormcaller_staff.png and _glow.png, textures/item/stormcaller_staff.png (the 16 x 16 inventory sprite rendered
from the finished mesh, kept as the inventory icon), models/item/stormcaller_staff.json and _icon.json, then the class row ("staff", both copies of weapon_classes.json) and the display
preset of the long weapon (the oversize rule). The two json files with other weapons' rows are changed by text insertion only: every other row stays byte for byte.
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
from new197_kit import (P, rod, lathe_y, orb, crystal, bar, tube, wrap, ml, w, paint, put_glow, collar_ring, new_mesh, new_atlas, lay_and_paint, finish)   # noqa: E402
from new197_god import helix_pts                                           # noqa: E402
from new197_legend import glow_parts                                       # noqa: E402

REPO = os.path.abspath(os.path.join(_TOOLS, ".."))
GAME_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
ITEM_ID = "stormcaller_staff"
GRIP_Y = 68.0                                                              # the middle of the wrapped grip (cm, mesh space): the hand holds the staff here


def build():
    m = new_mesh(ITEM_ID)
    YT = 176.0                                                             # the top of the shaft
    # the shaft: dark slate-blue lacquer, a twisted silver wire of gust round its upper half, a wrapped grip with silver bands
    rod(m, "shaft", "wood", -4.0, YT, 1.95, 1.7, sides=10, base=(40, 52, 84), smooth=55)
    tube(m, "gust_wire", "silver", helix_pts(2.5, 100.0, YT - 14.0, 5.0, 72, 0.4), 0.5, 0.5, sides=5, base=(196, 210, 232), smooth=60)
    wrap(m, "grip_wraps", 44.0, 92.0, 1.95, 11, "leather", (54, 68, 104))
    for y in (40.0, 96.0):
        collar_ring(m, y, 2.9, 2.2, "silver", (190, 202, 224), name="band_%d" % int(y))
    lathe_y(m, "butt", "iron", -16.0, [(0.0, 0.0), (2.5, 2.3), (12.0, 3.0)], sides=10, base=(112, 122, 146), caps=(False, True))

    # the crown cup that holds the cloud, and the six drops of rain crystal under its rim
    lathe_y(m, "crown", "silver", YT - 14.0, [(0.0, 2.4), (4.0, 4.4), (9.0, 6.4), (13.0, 6.2), (15.0, 4.4)], sides=16, base=(184, 198, 224), caps=(True, False))
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        crystal(m, "rain_drop_%d" % k, "crystal", (6.5 * math.cos(a), YT - 8.0, 6.5 * math.sin(a)), 1.3, 5.2, base=(130, 192, 255), glow=True)

    # the thundercloud: a wide dark belly under a rolling grey-white top of lobes of different sizes, round a glowing storm core
    cy = YT + 3.0
    names = []
    belly = [(-7.0, 0.5, 3.6, 4.2), (6.6, 0.8, -4.0, 4.4), (3.4, 0.3, 7.0, 4.0), (-3.6, 0.6, -7.2, 4.0), (0.0, -0.6, 0.0, 5.0)]
    for i, (x, y, z, r) in enumerate(belly):
        nm = "cloud_belly_%d" % i
        orb(m, nm, "stone", (x, cy + y, z), r, base=(116, 128, 158), glow=False, rx=r * 1.25, ry=r * 0.72, rz=r * 1.25, sides=12, stacks=8)
        names.append(nm)
    lobes = [(-8.2, 4.8, 2.4, 6.4), (8.0, 5.2, -2.6, 6.8), (2.6, 4.4, 8.4, 6.0), (-2.8, 4.6, -8.4, 6.2), (-4.6, 11.2, -2.2, 5.6), (4.4, 10.8, 2.6, 5.4),
             (-3.6, 10.4, 5.2, 4.6), (4.8, 10.0, -5.0, 4.8), (0.6, 13.6, 0.4, 4.2), (-9.4, 2.6, -4.0, 3.4), (9.0, 3.0, 4.6, 3.6)]
    for i, (x, y, z, r) in enumerate(lobes):
        nm = "cloud_%d" % i
        orb(m, nm, "stone", (x, cy + y, z), r, base=(208, 216, 236), glow=False, rx=r * 1.2, ry=r * 0.84, rz=r * 1.2, sides=12, stacks=8)
        names.append(nm)
    orb(m, "storm_core", "crystal", (0.0, cy + 6.0, 0.0), 3.6, base=(150, 204, 255), glow=True, sides=10, stacks=7)

    # veins of lightning crawling over the cloud
    veins = [[(-9.0, 5.0, 8.4), (-4.2, 9.2, 11.0), (0.6, 6.6, 12.2), (5.4, 10.0, 9.4), (9.0, 6.0, 6.6)],
             [(9.4, 4.0, -7.6), (5.0, 8.8, -11.0), (-0.8, 6.0, -12.0), (-5.6, 9.6, -9.0), (-9.2, 5.2, -5.4)]]
    for k, pts in enumerate(veins):
        tube(m, "cloud_vein_%d" % k, "crystal", [(x, cy + y, z) for x, y, z in pts], 0.42, 0.42, sides=5, base=(170, 214, 255), glow=True)

    # four silver horns of wind curling up round the cloud
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        c, s = math.cos(a), math.sin(a)
        pts = [(5.6 * c, YT - 2.0, 5.6 * s), (11.6 * c, cy + 6.0, 11.6 * s), (11.0 * c, cy + 17.0, 11.0 * s), (7.0 * c, cy + 26.0, 7.0 * s), (2.4 * c, cy + 30.0, 2.4 * s)]
        tube(m, "horn_%d" % k, "silver", pts, 1.3, 0.25, sides=6, tip=True, base=(188, 202, 228))

    # the lightning rod: a slender silver rod out of the cloud with two fins and a glowing crystal tip
    ry = cy + 14.0
    rod(m, "rod", "silver", ry, ry + 52.0, 1.0, 0.36, sides=8, base=(216, 228, 248), smooth=55)
    bar(m, "rod_fin_a", "silver", (-4.6, ry + 30.0, 0.0), (4.6, ry + 30.0, 0.0), 0.7, 0.44, up=(0, 0, 1), base=(210, 222, 244))
    bar(m, "rod_fin_b", "silver", (0.0, ry + 30.0, -4.6), (0.0, ry + 30.0, 4.6), 0.7, 0.44, up=(1, 0, 0), base=(210, 222, 244))
    lathe_y(m, "rod_ring", "silver", ry + 18.0, [(0.0, 1.9), (1.2, 2.5), (2.4, 1.9)], sides=10, base=(200, 212, 236))
    crystal(m, "rod_tip", "crystal", (0.0, ry + 56.0, 0.0), 1.7, 6.2, base=(196, 228, 255), glow=True)

    # a bolt of glow inlay zig-zagging down the front and the back of the shaft
    inlay = P(m, "bolt_inlay", "crystal", base=(160, 208, 255), glow=True, smooth=40)
    zig = [(0.0, 104.0), (-1.1, 116.0), (1.0, 123.0), (-1.1, 137.0), (1.0, 144.0), (-0.9, 156.0), (0.8, 162.0), (-0.4, 170.0)]
    for sg in (1, -1):
        for a, b in zip(zig[:-1], zig[1:]):
            ml.bar(inlay, (a[0], a[1], sg * 1.75), (b[0], b[1], sg * 1.75), 0.66, 0.2, up=(0, 0, sg), taper=0.0)

    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def storm(pc):                                                         # flickering blue light inside the cloud lobes
        n = w.noise2(pc.A.shape, 6, 3)
        pc.img[..., :3] = pc.img[..., :3] * (0.86 + 0.28 * n[..., None]) + np.array([14, 22, 44], np.float32) * 0.2
        put_glow(pc, np.clip(n - 0.66, 0, 1) * 0.55, (130, 190, 255), 0.55, 0.5)
    for nm in names:
        paint(atlas, m.part(nm), storm)
    glow_parts(atlas, m, ["storm_core", "rod_tip"], (170, 214, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["bolt_inlay", "cloud_vein_0", "cloud_vein_1"], (150, 204, 255), 0.95, 0.55)
    glow_parts(atlas, m, ["rain_drop_%d" % k for k in range(6)], (130, 190, 255), 0.85, 0.5)
    glow_parts(atlas, m, ["gust_wire"], (170, 210, 255), 0.22, 0.3)
    finish(ITEM_ID, m, atlas, "staff", glow=("A8D6FF", 0.16, 0.4, 1.0))
    add_gripxyz()


def add_gripxyz():
    """Puts a `gripxyz x y z` line in the mesh (the new grip system of docs/weapon_grip.md): the hand holds the middle of the wrapped grip, not a class fraction."""
    path = os.path.join(GAME_ASSETS, "weapon_meshes", ITEM_ID + ".txt")
    lines = open(path, encoding="utf-8").read().split("\n")
    lines = [ln for ln in lines if not ln.startswith("gripxyz")]
    at = 0
    for i, ln in enumerate(lines):
        if ln.startswith("place") or ln.startswith("glow") or ln.startswith("grip "):
            at = i + 1
    lines.insert(at, "gripxyz 0.000 %.3f 0.000" % GRIP_Y)
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


def class_rows():
    """The row of this weapon (type staff) in both copies of weapon_classes.json, by text insertion (the files keep their formatting and every other row)."""
    game = os.path.join(GAME_ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8", newline="").read()
    row = '    "%s": "staff",' % ITEM_ID
    if re.search(r'^    "%s": "[^"]*",' % ITEM_ID, text, re.M):
        text = re.sub(r'^    "%s": "[^"]*",' % ITEM_ID, row, text, flags=re.M)
    else:
        text = re.sub(r'(^    "wisp_staff": "staff",)', lambda mt: row + "\n" + mt.group(1), text, count=1, flags=re.M)
    open(game, "w", encoding="utf-8", newline="").write(text)
    tools = os.path.join(REPO, "tools", "relics", "weapon_classes.json")
    text = open(tools, encoding="utf-8", newline="").read()
    block = '    "%s": {\n      "class": "staff"\n    },' % ITEM_ID
    if '"%s": {' % ITEM_ID in text:
        text = re.sub(r'    "%s": \{\s*"class": "[^"]*"\s*\},' % ITEM_ID, lambda mt: block, text)
    else:
        text = re.sub(r'(    "wisp_staff": \{)', lambda mt: block + "\n" + mt.group(1), text, count=1)
    open(tools, "w", encoding="utf-8", newline="").write(text)


def presets():
    """The oversized-weapon display rule for this one weapon (the rows of every other weapon stay as they are, byte for byte): see tools/relics/display_presets.py."""
    import display_presets as dp
    mesh = dp.read_mesh(os.path.join(dp.MESHES, ITEM_ID + ".txt"))
    mjson = os.path.join(dp.MODELS, ITEM_ID + ".json")
    raw = open(mjson, encoding="utf-8").read()
    model = json.loads(raw)
    plan = dp.plan_for(ITEM_ID, model, mesh)
    entry = {"id": ITEM_ID, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
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
    existing = re.search(r'    \{\n      "id": "%s",.*?\n    \}' % ITEM_ID, text, re.S)
    if existing:
        text = text[:existing.start()] + frag + text[existing.end():]
    else:
        at = None
        for mt in re.finditer(r'    \{\n      "id": "([^"]+)",', text):
            if mt.group(1) > ITEM_ID:
                at = mt.start()
                break
        if at is None:
            raise SystemExit("no place for the manifest row")
        text = text[:at] + frag + ",\n" + text[at:]
    open(dp.MANIFEST, "w", encoding="utf-8", newline="").write(text)
    return entry


def main(args=None):
    sys.path.insert(0, _TOOLS)
    build()
    class_rows()
    e = presets()
    print("  %s L=%.2f %s" % (ITEM_ID, e["length_blocks"], e["action"]))


if __name__ == "__main__":
    main(sys.argv[1:])
