#!/usr/bin/env python3
"""
VALKYRIE DRILL LANCE: three versions = PREVIEW ONLY (nothing is registered in the game; no Java class, no asset, no lang key).

Renders the three candidate lances of tools/gen_valkyrie_lance.py (A Frosted Drill, B Crystal Corkscrew, C Living Water Dragon Lance) on the Valkyrie armour figure of valkyrie_v2_preview.py, and the
optional Mermaid look (tail, shell top, horn).

    python tools/gen_valkyrie_armor_v2.py ; python tools/gen_valkyrie_lance.py
    python tools/vfx_preview/valkyrie_lance_preview.py [compare|turn|close|vs|saint|mermaid ...]      (no argument: everything)

Output (docs/previews/vfx_remake/valkyrie/): valkyrie_lance_compare.png (3 versions x attached / held with arms raised / ready to throw / floating beside the wearer),
valkyrie_lance_turntable.png (3 x 8 angles), valkyrie_lance_closeup.png (tip and grip / base of each), valkyrie_lance_vs_ref.png (the frames 131 and 130 next to each version on the figure),
valkyrie_lance_saint.png (the Saint look with each lance), valkyrie_mermaid.png (the Mermaid look next to frames 132 and 133).

Same renderer and honest limits as valkyrie_v2_preview.py: software, no bloom, no world light, procedural textures, flat 2D bubbles / foam / background, never seen in a game.  The hand is placed by the pose
(the grip of the lance, a quarter of its length from the butt, on the hand point of the arm), the direction by the pose, not by an item display transform (that is the wiring-time job, see the doc).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, TOOLS)
sys.path.insert(0, HERE)
import valkyrie_v2_preview as pv                                                # noqa: E402
import gen_valkyrie_lance as gl                                                 # noqa: E402
from valkyrie_v2_preview import v3, Pose, Quads, bones, figure, armour, render, label, font, bg_plain, bg_ref, mesh_to_quads, apply, tr, ry, rx, rz, orbit   # noqa: E402

OUT = pv.OUT
BASE = pv.BASE
TEXMAP = {k: {int(a): b for a, b in v.items()} for k, v in gl.LAYER_TEX.items()}
MS = None


def meshes():
    global MS
    if MS is None:
        MS = gl.build_all()
    return MS


def frame(d, roll=0.0):
    ya = np.array(d, float)
    ya /= np.linalg.norm(ya)
    ref = np.array([0, 0, 1.0]) if abs(ya @ np.array([0, 0, 1.0])) < 0.9 else np.array([1.0, 0, 0])
    xa = np.cross(ref, ya)
    xa /= np.linalg.norm(xa)
    za = np.cross(xa, ya)
    M = np.eye(4)
    M[:3, 0], M[:3, 1], M[:3, 2] = xa, ya, za
    return M @ ry(roll)


def lance_matrix(mode, B, variant, spin=0.0, d=None):
    L = gl.LEN[variant]
    grip = gl.GRIP * L
    if mode == "attached":                                 # at the hips / legs like a tail, pointing down and forward (the Valkyrie Dress pose of the frames)
        dd = d if d is not None else (-0.3, -0.62, 0.72)
        return B["body"] @ tr(-1.0, 12.0, 4.0) @ frame(dd, spin)
    if mode == "held":                                     # the right hand, the grip on the hand point
        hand = apply(B["arm_r"], np.array([[-6.0, 12.5, 0.0]]))[0]
        dd = d if d is not None else (-0.05, 0.28, 0.96)
        return tr(*hand) @ frame(dd, spin) @ tr(0, -grip, 0)
    if mode == "float":                                    # hovering beside the wearer like Yuno's floating sword, slowly turning about its own axis
        dd = d if d is not None else (0.02, 0.05, 1.0)
        return B["root"] @ tr(-17.0, 22.0, 3.0) @ frame(dd, spin) @ tr(0, -L * 0.35, 0)
    if mode == "alone":
        return tr(0, -L / 2, 0) @ frame(d if d is not None else (0, 1, 0), spin)
    raise ValueError(mode)


def add_lance(out, variant, lod, M):
    ms = meshes()
    mesh_to_quads(out, ms[(variant, lod)], TEXMAP[variant], lambda p, uv: apply(M, p))


def scene(pose, look, variant, mode, lod, eye, tgt, size, bg, spin=0.0, d=None, fig=True, arm=True, front=None, legs=True, lance=True, ss=2):
    q = Quads()
    B = bones(pose)
    if fig:
        figure(q, B, pose, legs=legs)
    if arm:
        armour(q, B, pose, lod, look=look)
    if lance and variant:
        add_lance(q, variant, lod, lance_matrix(mode, B, variant, spin, d))
    img, cam = render(q, eye, tgt, size, bg, ss=ss, front=front)
    return img, cam, q


def count(q):
    return sum(1 for k in q.K if k not in ("skin", "suit", "hair", "dark"))


NAMES = {"A": "A  Frosted Drill", "B": "B  Crystal Corkscrew", "C": "C  Living Water Dragon Lance"}
POSE_RAISED = Pose(hover=2, arm_l=(8, 150), arm_r=(10, 152), pitch=-10, flap=3, bend=5, wave_amp=0.5, wave=1.0, tails=True, leg_l=8, leg_r=-10, leg_abd_l=6, leg_abd_r=5)
POSE_THROW = Pose(hover=1, lean=-4, yaw=-10, pitch=-6, arm_r=(112, 8), arm_l=(30, 40), leg_l=-12, leg_r=14, flap=2, bend=3, wave_amp=0.4, tails=True)
POSE_ATTACH = Pose(hover=2.5, lean=-3, yaw_body=-8, yaw=8, pitch=-14, arm_l=(14, 148), arm_r=(10, 152), leg_l=10, leg_r=-14, leg_abd_l=8, leg_abd_r=7, flap=3, bend=5, wave_amp=0.55, wave=1.0, tails=True)
POSE_131 = Pose(hover=2, lean=8, yaw_body=18, yaw=-12, pitch=-6, arm_r=(40, 62), arm_l=(20, 30), leg_l=-14, leg_r=18, leg_abd_l=5, leg_abd_r=8, flap=2, bend=3, wave_amp=0.4, tails=True)


def compare():
    cols = [("attached at the legs", POSE_ATTACH, "attached", None, orbit(-18, 11.0, 0.8, 0.55)),
            ("held, arms raised", POSE_RAISED, "held", (-0.25, 0.85, 0.45), orbit(-18, 11.0, 1.8, 2.0)),
            ("held, ready to throw", POSE_THROW, "held", (-0.08, 0.34, 0.94), orbit(-62, 10.5, 1.6, 1.5)),
            ("floating beside the wearer", POSE_ATTACH, "float", (0.03, 0.1, 1.0), orbit(-30, 11.0, 1.3, 1.45))]
    TW, TH = 360, 520
    sheet = Image.new("RGB", (TW * 4, TH * 3), (10, 14, 18))
    for r, v in enumerate("ABC"):
        for c, (nm, pose, mode, d, (eye, tgt)) in enumerate(cols):
            img, cam, q = scene(pose, "VALKYRIE", v, mode, 0, eye, tgt, (TW, TH), bg_plain((TW, TH)), spin=0.7 * (r + 1), d=d)
            label(img, "%s  /  %s" % (NAMES[v], nm), sz=12)
            sheet.paste(img, (c * TW, r * TH))
    p = os.path.join(OUT, "valkyrie_lance_compare.png")
    sheet.save(p)
    return p


def turn():
    TW, TH = 210, 380
    sheet = Image.new("RGB", (TW * 8, TH * 3), (10, 14, 18))
    for r, v in enumerate("ABC"):
        for c in range(8):
            a = c * 45
            eye = v3(math.sin(math.radians(a)) * 6.6, 0.2, math.cos(math.radians(a)) * 6.6)
            q = Quads()
            add_lance(q, v, 0, lance_matrix("alone", None, v, 0.0, (0.0, 1.0, 0.0)))
            img, cam = render(q, eye, v3(0, 0.0, 0), (TW, TH), bg_plain((TW, TH)), ss=2, fov=32)
            label(img, "%s / %d" % (v, a), sz=12)
            sheet.paste(img, (c * TW, r * TH))
    p = os.path.join(OUT, "valkyrie_lance_turntable.png")
    sheet.save(p)
    return p


def close():
    TW, TH = 520, 520
    sheet = Image.new("RGB", (TW * 3, TH * 2), (10, 14, 18))
    for c, v in enumerate("ABC"):
        L = gl.LEN[v]
        for r, (nm, yc, ext) in enumerate((("tip and the upper cone", L * 0.78, 22.0), ("grip, guard and the base", 12.0, 26.0))):
            q = Quads()
            add_lance(q, v, 0, tr(0, 0, 0) @ frame((0, 1, 0)))
            tgt = v3(0, yc / 16, 0)
            eye = v3(0.15, (yc + 2) / 16, ext / 16 * 1.9 + 0.1)
            img, cam = render(q, eye, tgt, (TW, TH), bg_plain((TW, TH)), ss=2, fov=34)
            label(img, "%s  /  %s" % (NAMES[v], nm), sz=13)
            sheet.paste(img, (c * TW, r * TH))
    p = os.path.join(OUT, "valkyrie_lance_closeup.png")
    sheet.save(p)
    return p


def vs_ref():
    size = (600, 400)
    r131 = Image.open(os.path.join(BASE, "ref", "valkyrie_ref_131_drill.png")).convert("RGB")
    r130 = Image.open(os.path.join(BASE, "ref", "valkyrie_ref_130.png")).convert("RGB")
    sheet = Image.new("RGB", (size[0] * 4 + 18, size[1] * 2 + 48), (12, 12, 14))
    rows = [("held out behind (frame 131)", r131, POSE_131, "held", (-0.9, 0.38, -0.2), orbit(-22, 8.6, 1.2, 1.55), lambda: bg_plain(size, (6, 14, 30), (16, 40, 70))),
            ("attached at the legs (frame 130 pose)", r130, POSE_ATTACH, "attached", None, orbit(-22, 8.4, 0.7, 1.0), None)]
    for ri, (nm, ref, pose, mode, d, (eye, tgt), bgf) in enumerate(rows):
        y = 24 + ri * (size[1] + 24)
        sheet.paste(ref.resize(size, Image.LANCZOS), (0, y))
        label(sheet, "REFERENCE  " + nm, (6, y - 18), 13)
        for ci, v in enumerate("ABC"):
            bg = bgf() if bgf else bg_ref(size, 4)
            img, cam, q = scene(pose, "VALKYRIE", v, mode, 0, eye, tgt, size, bg, spin=0.9, d=d)
            sheet.paste(img, ((ci + 1) * (size[0] + 6), y))
            label(sheet, "%s  %d quads incl. armour" % (NAMES[v], count(q)), ((ci + 1) * (size[0] + 6) + 6, y - 18), 13)
    p = os.path.join(OUT, "valkyrie_lance_vs_ref.png")
    sheet.save(p)
    return p


def saint():
    TW, TH = 480, 520
    sheet = Image.new("RGB", (TW * 3, TH), (10, 14, 18))
    for c, v in enumerate("ABC"):
        pose = Pose(hover=2, arm_r=(40, 20), arm_l=(10, 40), pitch=-4, leg_l=-6, leg_r=8)
        eye, tgt = orbit(-28, 12.5, 1.6, 1.75)
        img, cam, q = scene(pose, "SAINT", v, "float", 0, eye, tgt, (TW, TH), bg_plain((TW, TH), (22, 40, 70), (150, 190, 215)), spin=0.6 * c, d=(0.05, 0.12, 1.0))
        label(img, "SAINT look + %s" % NAMES[v], sz=13)
        sheet.paste(img, (c * TW, 0))
    p = os.path.join(OUT, "valkyrie_lance_saint.png")
    sheet.save(p)
    return p


def mermaid():
    size = (560, 760)
    r132 = Image.open(os.path.join(BASE, "ref", "valkyrie_ref_132_mermaid.png")).convert("RGB")
    sheet = Image.new("RGB", (size[0] * 3 + 12, size[1] + 24), (12, 12, 14))
    rw = int(r132.width * size[1] / r132.height)
    sheet.paste(r132.resize((rw, size[1]), Image.LANCZOS), (0, 24))
    label(sheet, "REFERENCE frame 132 (Mermaid Form)", (6, 4), 13)
    pose = Pose(hover=3, lean=-10, yaw=10, pitch=-4, arm_r=(40, 62), arm_l=(30, 40), tails=True)
    eye, tgt = orbit(-28, 10.5, 1.0, 0.95)
    img, cam, q = scene(pose, "MERMAID", "C", "held", 0, eye, tgt, size, bg_plain(size, (120, 150, 120), (190, 210, 170)), spin=0.5, d=(-0.55, -0.82, 0.25), legs=False)
    sheet.paste(img, (size[0] + 6, 24))
    label(sheet, "NEW Mermaid look + lance C held low: scaled tail, shell top, frills, horn  (%d quads)" % count(q), (size[0] + 12, 4), 13)
    eye, tgt = orbit(70, 10.5, 1.0, 0.95)
    img2, cam, q2 = scene(pose, "MERMAID", "A", "float", 0, eye, tgt, size, bg_plain(size, (20, 120, 200), (120, 190, 235)), d=(0.0, 0.1, 1.0), legs=False, spin=0.4)
    sheet.paste(img2, (size[0] * 2 + 12, 24))
    label(sheet, "NEW Mermaid look from the side + lance A floating", (size[0] * 2 + 18, 4), 13)
    p = os.path.join(OUT, "valkyrie_mermaid.png")
    sheet.save(p)
    return p


def main():
    os.makedirs(OUT, exist_ok=True)
    todo = sys.argv[1:] or ["compare", "turn", "close", "vs", "saint", "mermaid"]
    fn = {"compare": compare, "turn": turn, "close": close, "vs": vs_ref, "saint": saint, "mermaid": mermaid}
    for t in todo:
        print("wrote", fn[t]())


if __name__ == "__main__":
    main()
