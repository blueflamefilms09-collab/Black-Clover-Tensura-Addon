"""The 3D model of Magna's Soul Bat (Magna Swing, docs/magic/fire_casting.md), built with the owner's model method (tools/relics/mesh_lib.py, the kit of tools/relics/new197_*.py).

    python3 -B tools/gen_weapon_models.py magna_bat            (the hook of the weapon generator; this file can also be run directly)
    python3 -B tools/gen_magna_bat_model.py                    deterministic, idempotent
    N197_PREVIEW=<dir> python3 -B tools/gen_magna_bat_model.py   also renders the kit's mesh preview

Look (after Magna Swing's baseball bat of the anime): a long baseball bat of dark, charred wood with a red grain, a black leather grip bound with a glowing red cord and a knob holding a glowing ember
soul in a gold ring; three iron bands round the barrel studded with rows of iron studs, six tongues of flame inlaid along the barrel in glowing orange, a crown of six flame spikes round the hitting end,
and cracks of magma spiralling round the neck. It writes (through new197_lib.finish): assets/nusmp/weapon_meshes/magnas_soul_bat.txt (+ a `gripxyz` line: the hand holds the middle of the wrapped
grip), textures/entity/weapon/magnas_soul_bat.png and _glow.png, textures/item/magnas_soul_bat.png (the 16 x 16 inventory sprite rendered from the finished mesh, kept as the inventory icon),
models/item/magnas_soul_bat.json and _icon.json, then the class row ("club", both copies of weapon_classes.json) and the display preset of the long weapon (the oversize rule). The json files with other
weapons' rows are changed by text insertion only: every other row stays byte for byte.
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
from new197_kit import (P, rod, lathe_y, orb, bar, tube, wrap, ml, paint, put_glow, collar_ring, new_mesh, new_atlas, lay_and_paint, finish)   # noqa: E402
from new197_god import helix_pts                                           # noqa: E402
from new197_legend import glow_parts                                       # noqa: E402

REPO = os.path.abspath(os.path.join(_TOOLS, ".."))
GAME_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
ITEM_ID = "magnas_soul_bat"
GRIP_Y = 20.0                                                              # the middle of the wrapped grip (cm, mesh space): the hand holds the bat here

PROFILE = [(0.0, 1.0), (1.4, 2.5), (3.2, 2.75), (4.6, 2.0), (7.0, 1.55), (30.0, 1.62), (50.0, 2.1), (70.0, 2.65), (90.0, 3.2), (106.0, 3.55), (113.0, 3.35), (118.0, 2.6), (121.0, 1.2), (122.0, 0.0)]


def r_at(y):
    """The radius of the bat at height y (the lathe profile, linear between its points)."""
    for (y0, r0), (y1, r1) in zip(PROFILE[:-1], PROFILE[1:]):
        if y0 <= y <= y1:
            return r0 + (r1 - r0) * (y - y0) / max(1e-6, y1 - y0)
    return 0.0


def build():
    m = new_mesh(ITEM_ID)
    # the body: one lathed baseball bat, dark charred wood with a red grain
    lathe_y(m, "bat_body", "wood", 0.0, PROFILE, sides=18, base=(48, 30, 26), smooth=55)
    # the grip: a black leather binding and a red cord twisted round it
    wrap(m, "grip_wraps", 9.0, 34.0, 1.62, 9, "leather", (28, 22, 26))
    tube(m, "grip_cord", "leather", helix_pts(1.74, 9.5, 33.5, 6.0, 72), 0.2, 0.2, sides=5, base=(170, 44, 30), smooth=60)
    # the knob: a gold ring and the glowing ember soul in a cradle under it
    collar_ring(m, 4.0, 2.55, 1.2, "gold", (206, 156, 62), name="knob_ring")
    orb(m, "soul_orb", "crystal", (0.0, -1.2, 0.0), 1.7, base=(255, 122, 40), glow=True, sides=12, stacks=8)
    for k in range(4):
        a = math.pi / 2 * k
        tube(m, "knob_claw_%d" % k, "gold", [(0.9 * math.cos(a), 1.2, 0.9 * math.sin(a)), (2.2 * math.cos(a), -0.6, 2.2 * math.sin(a)), (1.4 * math.cos(a), -2.6, 1.4 * math.sin(a))], 0.32, 0.18, sides=5, base=(206, 156, 62), smooth=60)
    # three iron bands round the barrel and rows of iron studs between them
    for y in (58.0, 82.0, 104.0):
        collar_ring(m, y, r_at(y) + 0.22, 2.6, "iron", (92, 90, 100), name="band_%d" % int(y), sides=18)
    studs = P(m, "studs", "iron", base=(86, 84, 92), smooth=50)
    for y in (70.0, 94.0):
        for k in range(9):
            a = 2 * math.pi * k / 9 + (0.35 if y > 80 else 0.0)
            r = r_at(y)
            ml.stud(studs, (r * math.cos(a), y, r * math.sin(a)), 0.62, 0.85, (math.cos(a), 0.0, math.sin(a)), 8, 0.55)
    # six tongues of flame inlaid along the barrel, glowing orange (the flame style of Magna Swing)
    for k in range(6):
        a0 = 2 * math.pi * k / 6 + 0.2
        pts = []
        for i in range(0, 25):
            y = 38.0 + 3.0 * i
            a = a0 + 0.55 * math.sin((y - 38.0) * 0.11 + k)
            r = r_at(y) + 0.04
            pts.append((r * math.cos(a), y, r * math.sin(a)))
        tube(m, "flame_inlay_%d" % k, "crystal", pts, 0.46, 0.46, sides=5, tip=True, base=(255, 140, 40), glow=True, smooth=40)
    # cracks of magma spiralling round the neck
    for k, ph in enumerate((0.0, math.pi)):
        pts = []
        for i in range(0, 31):
            y = 35.0 + 0.55 * i
            r = r_at(y) + 0.05
            a = ph + 2 * math.pi * 2.2 * i / 30
            pts.append((r * math.cos(a), y, r * math.sin(a)))
        tube(m, "cracks_%d" % k, "crystal", pts, 0.16, 0.16, sides=4, base=(255, 90, 30), glow=True, smooth=40)
    # the crown of flame spikes round the hitting end
    crown = P(m, "flame_crown", "crystal", base=(255, 110, 30), glow=True, smooth=40)
    for k in range(6):
        a = 2 * math.pi * k / 6
        ml_pt = lambda r, y: np.array([r * math.cos(a), y, r * math.sin(a)])                   # noqa: E731
        spike(crown, ml_pt(3.3, 111.0), ml_pt(4.6, 124.0), 0.95, sides=5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def grain(pc):                                                         # the red grain of charred wood
        n = w.noise2(pc.A.shape, 18, 3)
        pc.img[..., :3] = pc.img[..., :3] * (0.86 + 0.30 * n[..., None]) + np.array([34, 8, 4], np.float32) * (0.4 * np.clip(n - 0.55, 0, 1))[..., None]
    paint(atlas, m.part("bat_body"), grain)
    glow_parts(atlas, m, ["soul_orb", "flame_crown"], (255, 150, 60), 1.0, 0.6)
    glow_parts(atlas, m, ["flame_inlay_%d" % k for k in range(6)], (255, 140, 40), 0.95, 0.55)
    glow_parts(atlas, m, ["cracks_0", "cracks_1", "grip_cord"], (255, 100, 40), 0.9, 0.5)
    finish(ITEM_ID, m, atlas, "club", glow=("FF8A2A", 0.16, 0.4, 1.0))
    add_gripxyz()


def add_gripxyz():
    """Puts a `gripxyz x y z` line in the mesh (the grip system of docs/weapon_grip.md): the hand holds the middle of the wrapped grip, not a class fraction."""
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
    """The row of this weapon (class club) in both copies of weapon_classes.json, by text insertion (the files keep their formatting and every other row)."""
    game = os.path.join(GAME_ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8", newline="").read()
    row = '    "%s": "club",' % ITEM_ID
    if re.search(r'^    "%s": "[^"]*",' % ITEM_ID, text, re.M):
        text = re.sub(r'^    "%s": "[^"]*",' % ITEM_ID, row, text, flags=re.M)
    else:
        text = re.sub(r'(^    "club_of_rot": "club",)', lambda mt: row + "\n" + mt.group(1), text, count=1, flags=re.M)
    open(game, "w", encoding="utf-8", newline="").write(text)
    tools = os.path.join(REPO, "tools", "relics", "weapon_classes.json")
    text = open(tools, encoding="utf-8", newline="").read()
    block = '    "%s": {\n      "class": "club"\n    },' % ITEM_ID
    if '"%s": {' % ITEM_ID in text:
        text = re.sub(r'    "%s": \{\s*"class": "[^"]*"\s*\},' % ITEM_ID, lambda mt: block, text)
    else:
        text = re.sub(r'(    "club_of_rot": \{)', lambda mt: block + "\n" + mt.group(1), text, count=1)
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
