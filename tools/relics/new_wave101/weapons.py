"""The three Genesis weapons of wave 101 (release 1.111.0): the Millennium Rod (a golden rod with a winged Eye of Wdjat, class staff), Ea, Sword of Rupture (three turning cylinders of black, red
and gold, class longsword) and the Requiem Stand Arrow (a golden arrow with a beetle's head, class dagger). Built with the kit of the 35 weapons of 1.97.0 (tools/relics/new197_*.py): real
cross-sections, a painted skin, a second additive glow skin, the dimensional void of the Genesis Demon-Slayer where it fits, the 16 x 16 sprite rendered from the mesh, both item models.

    python3 -B tools/relics/new_wave101/weapons.py [id ...]
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
RELICS = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, RELICS)
from new197_kit2 import *            # noqa: E402,F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, grip, collar_ring, extrude, ring_blade, catmull, sweep_blade, guard_disc, guard_wings,
                         pommel, spear_head, void_poly, void_strip)  # noqa: E402
from new197_lib import paint_glow, new_mesh, new_atlas, lay_and_paint, finish  # noqa: E402
from new197_legend import glow_parts  # noqa: E402

GOLD, GOLD_HI, LAPIS, LAPIS_DK = (226, 178, 66), (250, 212, 106), (28, 62, 170), (14, 30, 110)


def helix(r, y0, y1, turns, n, phase=0.0):
    return [(r * math.cos(phase + 2 * math.pi * turns * i / n), y0 + (y1 - y0) * i / n, r * math.sin(phase + 2 * math.pi * turns * i / n)) for i in range(n + 1)]


def lid(cx, cy, half, h, z, sign, n=14, drop=0.0):
    """One lid of an almond (the outline of an eye) in the plane z: from the left corner to the right corner."""
    return [(cx - half + 2 * half * t, cy + sign * h * math.sin(math.pi * t) ** 0.9 - drop * t * (1 - t), z) for t in np.linspace(0, 1, n + 1)]


# ====================================================================== MILLENNIUM ROD
def build_millennium_rod():
    """Millennium Rod: a slender golden rod in lapis bands and a lapis-corded grip, mind threads winding up it, and at the head three gold falcon feathers a side round a lapis disc in a gold ring
    that carries the Eye of Wdjat on both faces (a lit pupil, a rim, the brow, the falcon's tear and its curl) over a void, a gold spire and a halo of the mind above."""
    m = new_mesh("millennium_rod")
    Y1 = 178.0
    rod(m, "shaft", "gold", 0.0, Y1, 1.5, 1.5, sides=10, base=GOLD, smooth=55)
    for y in (32.0, 72.0, 114.0, 152.0, Y1 - 3.0):
        collar_ring(m, y, 2.6, 2.4, "stone", LAPIS, name="band_%d" % int(y))
    wrap(m, "grip_cord", 12.0, 64.0, 1.5, 14, "cloth", (30, 66, 180))
    lathe_y(m, "butt", "gold", -9.0, [(0.0, 0.0), (1.4, 1.8), (9.0, 2.7)], sides=10, base=GOLD, caps=(False, True))
    lathe_y(m, "socket", "gold", Y1 - 2.0, [(0.0, 2.4), (2.4, 3.8), (7.0, 3.2), (10.5, 2.3)], sides=12, base=GOLD_HI)
    # three feathers a side, sweeping out and up from the socket
    spans = [[(2.6, Y1 + 8), (9.0, Y1 + 17), (16.0, Y1 + 29), (20.0, Y1 + 44)], [(2.6, Y1 + 6), (10.0, Y1 + 12), (18.0, Y1 + 21), (24.5, Y1 + 33)],
             [(2.6, Y1 + 4), (11.0, Y1 + 7.5), (20.0, Y1 + 13.5), (28.0, Y1 + 22)]]
    for sx in (-1, 1):
        for k, ctrl in enumerate(spans):
            sweep_blade(m, "wing_%s%d" % ("l" if sx < 0 else "r", k), "gold", [(sx * x, y) for (x, y) in ctrl], lambda t: 3.2 * (1 - 0.75 * t) + 0.3, lambda t: 0.7 * (1 - 0.5 * t), n=6,
                        base=GOLD_HI if k != 1 else GOLD, smooth=40)
    CY = Y1 + 31.0
    ring_blade(m, "eye_ring", "gold", 0.0, CY, 10.0, 1.5, 1.7, n=30, base=GOLD_HI)
    extrude(m, "eye_plate", "stone", [(8.2 * math.cos(a), CY + 8.2 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)[:-1]], 1.0, edge=0.9, base=LAPIS_DK)
    void_poly(m, "void_eye", [(5.4 * math.cos(a), CY + 5.4 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)[:-1]], 1.04)
    for sg, tag in ((1, "f"), (-1, "b")):
        z = sg * 1.45
        rim = lid(0.0, CY, 6.4, 2.9, z, 1.0) + lid(0.0, CY, 6.4, 2.3, z, -1.0)[::-1]
        tube(m, "eye_rim_" + tag, "gold", rim, 0.5, 0.5, sides=5, base=GOLD_HI, glow=True)
        orb(m, "iris_" + tag, "stone", (0.0, CY, sg * 1.5), 2.3, base=LAPIS_DK, glow=False, rz=0.8)
        orb(m, "pupil_" + tag, "crystal", (0.0, CY, sg * 2.2), 1.0, base=(130, 255, 240), glow=True, rz=0.55)
        tube(m, "brow_" + tag, "gold", [(-7.0 + 14.0 * t, CY + 6.2 + 1.6 * math.sin(math.pi * t), z) for t in np.linspace(0, 1, 10)], 0.45, 0.45, sides=5, base=GOLD_HI)
        tear = [(-0.8, CY - 2.8, z), (-0.8, CY - 5.6, z), (-0.8, CY - 7.2, z)] + [(3.6 + 2.4 * math.cos(a) * (1 - a / 14.0), CY - 7.4 + 2.4 * math.sin(a) * (1 - a / 14.0), z) for a in np.linspace(0, 8.0, 18)]
        tube(m, "tear_" + tag, "gold", tear, 0.4, 0.4, sides=5, base=GOLD_HI)
    spear_head(m, CY + 9.2, 16.0, 2.6, t=0.9, base=GOLD_HI, material="gold", name="spire")
    halo = [(6.4 * math.cos(a), CY + 14.0, 6.4 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)]
    tube(m, "mind_halo", "gold", halo, 0.55, 0.55, sides=6, base=GOLD_HI, glow=True)
    for k in range(3):
        tube(m, "mind_thread_%d" % k, "crystal", helix(3.5, 76.0 + 4 * k, 168.0, 3.0, 48, 2 * math.pi * k / 3), 0.5, 0.5, sides=5, base=(120, 250, 240), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["pupil_f", "pupil_b"], (140, 255, 244), 1.0, 0.6)
    glow_parts(atlas, m, ["mind_thread_0", "mind_thread_1", "mind_thread_2"], (110, 245, 235), 0.8, 0.4)
    glow_parts(atlas, m, ["mind_halo", "eye_rim_f", "eye_rim_b"], (255, 226, 140), 0.9, 0.5)
    finish("millennium_rod", m, atlas, "staff", glow=("FFFFFF", 0.15, 0.45, 1.0))


# ====================================================================== EA, SWORD OF RUPTURE
def build_ea_sword_of_rupture():
    """Ea: a black core with three cylinders of gold, red and black turning round it in a drill of rupture, red light running between them, gold bands, a gold disc guard with swept wings, a red-wrapped grip
    and a ruby pommel."""
    m = new_mesh("ea_sword_of_rupture")
    YG, YT = 30.0, 128.0
    rod(m, "core", "obsidian", YG, YT - 6.0, 2.4, 1.6, sides=10, base=(18, 16, 20), smooth=55)
    cyl = [("cyl_gold", "gold", (232, 184, 70)), ("cyl_red", "bronze", (196, 44, 34)), ("cyl_black", "obsidian", (28, 24, 30))]
    for k, (nm, mat, base) in enumerate(cyl):
        pts = helix(3.7, YG + 4.0, YT - 14.0, 2.4, 40, 2 * math.pi * k / 3)
        tube(m, nm, mat, pts, 2.1, 1.0, sides=8, base=base, smooth=55, tip=True)
        tube(m, nm + "_glow", "crystal", helix(3.7, YG + 6.0, YT - 18.0, 2.4, 40, 2 * math.pi * k / 3 + math.pi / 3), 0.55, 0.3, sides=5, base=(255, 80, 44), glow=True, tip=True)
    for y in (YG + 16.0, YG + 46.0, YG + 76.0):
        collar_ring(m, y, 5.7, 2.2, "gold", GOLD_HI, name="band_%d" % int(y))
    lathe_y(m, "tip", "gold", YT - 14.0, [(0.0, 3.0), (6.0, 2.2), (14.0, 0.0)], sides=10, base=(236, 190, 80), caps=(False, False))
    guard_disc(m, YG, r=6.4, thick=1.0, material="gold", base=GOLD_HI, name="guard_disc", notches=8)
    guard_wings(m, YG + 1.0, span=10.0, feathers=3, rise=6.0, t=0.9, material="gold", base=GOLD, name="guard_wings")
    grip(m, 5.0, YG - 1.4, r=1.8, base=(120, 22, 24), wrap_base=(232, 184, 70), n_wraps=8)
    lathe_y(m, "pommel", "gold", -2.5, [(0.0, 1.4), (1.8, 3.1), (4.6, 3.6), (6.4, 2.6), (7.5, 1.0)], sides=12, base=GOLD_HI)
    orb(m, "ruby", "crystal", (0.0, 1.0, 0.0), 1.7, base=(255, 60, 60), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["cyl_gold_glow", "cyl_red_glow", "cyl_black_glow"], (255, 90, 52), 1.0, 0.55)
    glow_parts(atlas, m, ["ruby"], (255, 70, 70), 1.0, 0.6)
    glow_parts(atlas, m, ["band_%d" % int(y) for y in (YG + 16.0, YG + 46.0, YG + 76.0)], (255, 200, 110), 0.4, 0.3)
    finish("ea_sword_of_rupture", m, atlas, "longsword", glow=("FFD0A0", 0.17, 0.5, 1.0))


# ====================================================================== REQUIEM STAND ARROW
def build_requiem_stand_arrow():
    """Requiem Stand Arrow: a golden arrow with a leaf head cut by a void and a beetle (a gold body with green wing cases and a lit eye) at its throat, a shaft wound with a gold-green aura, four vanes
    of fletching, a gold nock; golden light spirals round it."""
    m = new_mesh("requiem_stand_arrow")
    rod(m, "shaft", "gold", 0.0, 46.0, 0.75, 0.75, sides=8, base=(232, 196, 84), smooth=55)
    spear_head(m, 44.0, 19.0, 3.4, t=0.8, base=GOLD_HI, material="gold", name="head", leaf=0.32)
    void_strip(m, "void_head", 48.0, 60.0, 1.0, 0.9, cx=0.0, jag=0.35, n=6)
    # the beetle at the throat of the arrow
    orb(m, "beetle_body", "gold", (0.0, 44.0, 0.0), 2.6, base=(236, 190, 70), rx=2.8, ry=2.0, rz=3.0, glow=False, sides=10, stacks=6)
    for sx in (-1, 1):
        orb(m, "wing_case_%s" % ("l" if sx < 0 else "r"), "crystal", (sx * 1.4, 43.4, 1.0), 1.4, base=(60, 180, 96), rx=1.5, ry=1.0, rz=2.4, glow=True, sides=8, stacks=5)
    orb(m, "beetle_eye", "crystal", (0.0, 45.6, -2.8), 0.6, base=(255, 226, 120), glow=True)
    legs = P(m, "beetle_legs", "gold", base=GOLD, smooth=40)
    for sx in (-1, 1):
        for k, yy in enumerate((41.6, 43.6, 45.4)):
            spike(legs, (sx * 2.3, yy, 0.8 + 0.8 * k), (sx * 5.2, yy - 1.4, 1.8 + 1.2 * k), 0.28, 4)
    # the fletching: four vanes, two white and two green-gold, and a gold nock
    for i, (cx, cz, tx, tz, rx, rz, base) in enumerate(((1.6, 0.0, 0, 1, 0.18, 2.8, (250, 246, 232)), (-1.6, 0.0, 0, 1, 0.18, 2.8, (250, 246, 232)),
                                                       (0.0, 1.6, 1, 0, 2.8, 0.18, (120, 200, 120)), (0.0, -1.6, 1, 0, 2.8, 0.18, (120, 200, 120)))):
        orb(m, "vane_%d" % i, "cloth", (cx, 8.0, cz), 3.0, base=base, rx=rx, ry=7.5, rz=rz, glow=False, sides=8, stacks=6)
    lathe_y(m, "nock", "gold", -3.4, [(0.0, 0.5), (1.4, 1.3), (3.4, 1.0)], sides=8, base=GOLD_HI, caps=(False, True))
    # a golden aura spiralling round the shaft and a ring of light about the head
    for k in range(2):
        tube(m, "aura_%d" % k, "crystal", helix(2.2, 12.0, 41.0, 3.0, 40, math.pi * k), 0.34, 0.2, sides=5, base=(255, 224, 120), glow=True, tip=True)
    tube(m, "head_ring", "gold", [(5.2 * math.cos(a), 52.0, 5.2 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 21)], 0.4, 0.4, sides=5, base=GOLD_HI, glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["aura_0", "aura_1", "head_ring", "beetle_eye"], (255, 226, 130), 1.0, 0.55)
    glow_parts(atlas, m, ["wing_case_l", "wing_case_r"], (110, 255, 150), 0.8, 0.5)
    finish("requiem_stand_arrow", m, atlas, "dagger", glow=("FFF0B0", 0.17, 0.5, 1.0))


BUILDERS = {"millennium_rod": build_millennium_rod, "ea_sword_of_rupture": build_ea_sword_of_rupture, "requiem_stand_arrow": build_requiem_stand_arrow}
CLASSES = {"millennium_rod": "staff", "ea_sword_of_rupture": "longsword", "requiem_stand_arrow": "dagger"}


def write_classes():
    """The class row of each weapon in assets/nusmp/weapon_classes.json and tools/relics/weapon_classes.json (appended; the other rows stay)."""
    import json
    from new197_lib import ROOT
    pa = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "weapon_classes.json")
    pt = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")
    a = json.load(open(pa))
    t = json.load(open(pt))
    for rid, cls in CLASSES.items():
        a["weapons"].setdefault(rid, cls)
        t["weapons"].setdefault(rid, {"class": cls})
    with open(pa, "w", newline="\n") as f:
        f.write(json.dumps(a, indent=1, ensure_ascii=True) + "\n")
    with open(pt, "w", newline="\n") as f:
        f.write(json.dumps(t, indent=2, ensure_ascii=True) + "\n")


def presets():
    """The oversized-weapon display rule (tools/relics/display_presets.py) for these three weapons only; their rows are added to display_presets_manifest.json."""
    import json
    import display_presets as dp
    ids = list(BUILDERS)
    entries = []
    for item_id in ids:
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
            text = json.dumps(dp.apply_plan(model, plan), indent=2)
            if text != raw:
                open(mjson, "w", encoding="utf-8", newline="\n").write(text)
        entries.append(entry)
        print("  display %-26s %s L=%.2f" % (item_id, plan["action"], mesh.length))
    man = json.load(open(dp.MANIFEST, encoding="utf-8"))
    mine = set(ids)
    rows = [r for r in man["weapons"] if r["id"] not in mine] + entries
    man["weapons"] = sorted(rows, key=lambda r: r["id"])
    open(dp.MANIFEST, "w", encoding="utf-8", newline="\n").write(json.dumps(man, indent=2) + "\n")


def main(argv):
    only = set(a for a in argv if not a.startswith("--"))
    for rid, fn in BUILDERS.items():
        if only and rid not in only:
            continue
        fn()
    write_classes()
    presets()
    import subprocess                                    # the dagger gets the thickened copy of its mesh (tools/relics/weapon_sizes.py: daggers and katanas only)
    subprocess.run([sys.executable, "-B", os.path.join(RELICS, "weapon_sizes.py"), "--apply", "--ids", "requiem_stand_arrow"], check=True)


if __name__ == "__main__":
    main(sys.argv[1:])
