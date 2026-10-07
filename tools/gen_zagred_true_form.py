"""0.48 Zagred true form: ONE part list -> the 128x128 skin + glow textures AND the Java geometry of client/ZagredModel.java.

    python tools/gen_zagred_true_form.py

A tall black devil in Minecraft cuboids: pale face with red eyes under black hair, pointed ears, segmented swept horns, a pale
ribcage over an ink-black body with violet rune lines, long arms ending in long claws, clawed feet, huge tattered bat wings in two
segments, and a long whip tail with a spade. The geometry block between the "generated" markers in ZagredModel.java is rewritten
from PARTS below, so texture and model always agree. Old 0.47 textures (entity/zagred*.png) are left in place.
"""
import math
import os
import re
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "entity")
MODEL = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "ZagredModel.java")
S = 128

# (name, parent, pivot xyz, rotation xyz (radians), [boxes]); box = (u, v, x, y, z, w, h, d, mirror, material)
PARTS = []


def part(name, parent, pivot, rot, *boxes):
    PARTS.append((name, parent, pivot, rot, list(boxes)))


part("root", None, (0, -9, 0), (0, 0, 0))
part("body", "root", (0, 0, 0), (0, 0, 0),
     (0, 0, -4.5, 0, -2.5, 9, 13, 5, False, "torso"), (30, 0, -3.5, 1.5, -3.3, 7, 7, 1, False, "ribs"), (48, 0, -4, 13, -2.5, 8, 3, 5, False, "cloth"))
part("head", "root", (0, 0, 0), (0, 0, 0),
     (0, 20, -4, -8, -4, 8, 8, 8, False, "face"), (32, 20, -4.5, -8.6, -4.5, 9, 4, 9, False, "hair"), (68, 20, -4, -6, 3.5, 8, 11, 2, False, "hairback"))
for side, sx in (("r", -1), ("l", 1)):
    m = side == "l"
    part("ear_" + side, "head", (4 * sx, -4.5, 0), (0, 0, -0.35 * sx), (88, 20, 0 if m else -3, -0.5, -0.5, 3, 1, 1, m, "pale"))
    part("horn_" + side, "head", (2.6 * sx, -7.6, -1.5), (-0.35, 0, 0.5 * sx), (96, 20, -1, -3, -1, 2, 3, 2, m, "horn"))
    part("horn_" + side + "_mid", "horn_" + side, (0, -2.8, 0), (-0.45, 0, -0.25 * sx), (104, 20, -0.5, -4, -0.5, 1, 4, 1, m, "horn"))
    part("horn_" + side + "_tip", "horn_" + side + "_mid", (0, -3.8, 0), (-0.5, 0, -0.2 * sx), (108, 20, -0.5, -3, -0.5, 1, 3, 1, m, "horntip"))
    part("arm_" + side, "root", (6 * sx, 1.5, 0), (0, 0, -0.18 * sx), (0, 38, -1.5, -1, -1.5, 3, 9, 3, m, "skin"))
    part("forearm_" + side, "arm_" + side, (0, 8, 0), (-0.2, 0, 0), (12, 38, -1.5, 0, -1.5, 3, 9, 3, m, "skin"))
    part("hand_" + side, "forearm_" + side, (0, 9, 0), (0, 0, 0), (24, 38, -1.5, 0, -1.5, 3, 2, 3, m, "skin"))
    for k, cx in enumerate((-1, 0, 1)):
        part("claw_%s%d" % (side, k), "hand_" + side, (cx, 1.8, -1), (-0.35, 0, 0.12 * cx), (36, 38, -0.5, 0, -0.5, 1, 5, 1, m, "claw"))
    part("leg_" + side, "root", (2.4 * sx, 16, 0), (0, 0, 0), (0, 52, -2, 0, -2, 4, 7, 4, m, "skin"))
    part("shin_" + side, "leg_" + side, (0, 7, 0), (0, 0, 0), (16, 52, -1.5, 0, -1.5, 3, 8, 3, m, "skin"))
    part("foot_" + side, "shin_" + side, (0, 8, 0), (0, 0, 0), (28, 52, -1.5, 0, -3.5, 3, 2, 5, m, "skin"))
    for k, cx in enumerate((-1, 0, 1)):
        part("toe_%s%d" % (side, k), "foot_" + side, (cx, 1, -3.5), (0.3, 0, 0), (44, 52, -0.5, 0, -3, 1, 1, 3, m, "claw"))
    part("wing_" + side, "body", (2 * sx, 2, 2.6), (0, -0.55 * sx, 0.3 * sx), (0, 66, 0 if m else -14, -1, -1, 14, 2, 2, m, "wingbone"))
    part("membrane_" + side, "wing_" + side, (0, 1, 0), (0, 0, 0), (0, 72, 0 if m else -14, 0, 0, 14, 18, 0, m, "membrane"))
    part("wing2_" + side, "wing_" + side, (14 * sx, 0, 0), (0, 0, -0.65 * sx), (32, 66, 0 if m else -14, -1, -1, 14, 2, 2, m, "wingbone"))
    part("membrane2_" + side, "wing2_" + side, (0, 1, 0), (0, 0, 0), (28, 72, 0 if m else -14, 0, 0, 14, 22, 0, m, "membrane"))
part("tail1", "body", (0, 14, 2.5), (-0.75, 0, 0), (56, 38, -1.5, -1.5, 0, 3, 3, 7, False, "skin"))
part("tail2", "tail1", (0, 0, 6.5), (-0.2, 0, 0), (76, 38, -1, -1, 0, 2, 2, 7, False, "skin"))
part("tail3", "tail2", (0, 0, 6.5), (0.25, 0, 0), (94, 38, -1, -1, 0, 2, 2, 7, False, "skin"))
part("tail4", "tail3", (0, 0, 6.5), (0.35, 0, 0), (56, 50, -0.5, -0.5, 0, 1, 1, 7, False, "skin"))
part("spade", "tail4", (0, 0, 6.5), (0, 0, 0), (72, 50, -2.5, -0.5, 0, 5, 1, 5, False, "spade"))


# ---------------------------------------------------------------- painting
def faces(u, v, w, h, d):
    """Vanilla cube UV layout: name -> (u0, v0, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "east": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint():
    base = np.zeros((S, S, 4), np.float32)
    glow = np.zeros((S, S, 4), np.float32)
    n1, n2 = fbm(S, S, 16, 4870), fbm(S, S, 4, 4871)
    rng = np.random.default_rng(487)

    def px(img, x, y, rgb, a=255):
        if 0 <= x < S and 0 <= y < S:
            img[y, x, :3] = np.clip(rgb, 0, 255)
            img[y, x, 3] = a

    for (_, _, _, _, boxes) in PARTS:
        for (u, v, x, y, z, w, h, d, mirror, mat) in boxes:
            W, H, D = int(round(w)), int(round(h)), int(round(d))
            for fname, (fu, fv, fw, fh) in faces(int(u), int(v), W, H, D).items():
                for j in range(fh):
                    for i in range(fw):
                        X, Y = fu + i, fv + j
                        nn = (n1[Y % S, X % S] - 0.5) * 30 + (n2[Y % S, X % S] - 0.5) * 16
                        lit = 1.12 if fname == "top" else 0.82 if fname == "bottom" else 1.0
                        g = None
                        if mat in ("skin", "torso"):
                            c = np.array([20, 13, 25]) + nn
                            if (i + j * 2) % 11 == 0 and fname in ("front", "back") and rng.random() < 0.3:
                                c = np.array([48, 22, 72]); g = (90, 30, 170)                 # violet rune lines
                        elif mat == "ribs":
                            c = np.array([214, 204, 190]) + nn * 0.5 if (j % 2 == 0 or i in (0, fw // 2, fw - 1)) else np.array([40, 22, 34])
                            if fname == "front" and j % 2 == 1 and i not in (0, fw // 2, fw - 1): g = (120, 20, 50)
                        elif mat == "cloth":
                            c = np.array([30, 20, 32]) + nn * 0.6
                            if j == 0: c = np.array([60, 44, 30])                                 # a belt
                        elif mat in ("face", "pale"):
                            c = np.array([202, 194, 198]) + nn * 0.4
                            if mat == "face" and fname == "front":
                                if j == 4 and i in (1, 2, 5, 6):
                                    c = np.array([255, 40, 50]); g = (255, 30, 40)            # red eyes
                                elif j == 3 and i in (1, 2, 5, 6):
                                    c = np.array([40, 30, 40])                                 # brows
                                elif j == 6 and 2 <= i <= 5:
                                    c = np.array([50, 20, 30])                                 # mouth
                                elif j == 5 and i in (1, 6):
                                    c = np.array([150, 140, 150])
                        elif mat in ("hair", "hairback"):
                            c = np.array([10, 8, 13]) + nn * 0.35
                            if (i * 3 + j) % 5 == 0: c = np.array([42, 32, 54])
                            if mat == "hairback" and j >= fh - 2 and fname != "top":
                                c = np.array([60, 20, 90]); g = (130, 40, 210)                # hair tips
                            if mat == "hair" and fname == "front" and j >= fh - 1 and i % 2 == 0:
                                c = np.array([10, 8, 13])
                        elif mat == "horn":
                            c = np.array([30, 22, 28]) + nn * 0.5
                        elif mat == "horntip":
                            c = np.array([120, 70, 160]); g = (180, 60, 255)
                        elif mat == "claw":
                            t = j / max(1, fh - 1)
                            c = np.array([224, 214, 196]) * (1 - t) + np.array([40, 30, 34]) * t
                        elif mat == "wingbone":
                            c = np.array([26, 18, 30]) + nn * 0.5
                        elif mat == "membrane":
                            c = np.array([40, 14, 48]) + nn * 0.5
                            if (i % 5 == 0 and fname in ("front", "back")):
                                c = np.array([56, 18, 52]); g = (70, 6, 22)                    # veins
                            edge = fh - j
                            if edge <= 3 and ((i * 7 + edge * 3) % 5 < edge - 1 or (i % 4 == 2 and edge <= 2)):
                                px(base, X, Y, (0, 0, 0), 0); continue                         # tattered rim
                        elif mat == "spade":
                            c = np.array([26, 16, 30]); g = (110, 30, 180) if fname in ("top", "bottom") and i == fw // 2 else None
                        else:
                            c = np.array([255, 0, 255])
                        px(base, X, Y, np.array(c, np.float32) * lit)
                        if g is not None: px(glow, X, Y, g)
    for name, img in (("zagred_true", base), ("zagred_true_glow", glow)):
        Image.fromarray(img.astype(np.uint8), "RGBA").save(os.path.join(TEX, name + ".png"), optimize=True)
        print("wrote entity/" + name)


# ---------------------------------------------------------------- Java geometry
def f(x):
    s = ("%.3f" % x).rstrip("0").rstrip(".")
    return (s if s not in ("-0", "") else "0") + "f"


def java():
    lines = []
    var = {None: "mesh.getRoot()"}
    for (name, parent, piv, rot, boxes) in PARTS:
        cubes = "CubeListBuilder.create()"
        for (u, v, x, y, z, w, h, d, mirror, mat) in boxes:
            cubes += ".texOffs(%d, %d)%s.addBox(%s, %s, %s, %s, %s, %s)" % (u, v, ".mirror()" if mirror else "", f(x), f(y), f(z), f(w), f(h), f(d))
        pose = "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (f(piv[0]), f(piv[1]), f(piv[2]), f(rot[0]), f(rot[1]), f(rot[2]))
        v = "p_" + name
        lines.append("        PartDefinition %s = %s.addOrReplaceChild(\"%s\", %s, %s);" % (v, var[parent], name, cubes, pose))
        var[name] = v
    code = "\n".join(lines)
    src = open(MODEL, encoding="utf-8").read()
    new = re.sub(r"(        // <generated by tools/gen_zagred_true_form.py>\n).*?(        // </generated>)", lambda m: m.group(1) + code + "\n" + m.group(2), src, flags=re.S)
    assert new != src or code in src
    open(MODEL, "w", encoding="utf-8").write(new)
    print("wrote", len(PARTS), "parts into ZagredModel.java")


if __name__ == "__main__" and "--preview" not in sys.argv:
    paint()
    java()


# ---------------------------------------------------------------- preview (rest pose, no animation): --preview
def preview(path):
    import math as m
    tex = np.asarray(Image.open(os.path.join(TEX, "zagred_true.png")).convert("RGBA"), np.float32) / 255
    glo = np.asarray(Image.open(os.path.join(TEX, "zagred_true_glow.png")).convert("RGBA"), np.float32) / 255
    byname = {p[0]: p for p in PARTS}

    def rot(rx, ry, rz):
        cx, sx, cy, sy, cz, sz = m.cos(rx), m.sin(rx), m.cos(ry), m.sin(ry), m.cos(rz), m.sin(rz)
        Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]); Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
        return Rz @ Ry @ Rx

    def world(name):
        M, T = np.eye(3), np.zeros(3)
        chain = []
        while name:
            chain.append(byname[name]); name = byname[name][1]
        for (_, _, piv, r, _) in reversed(chain):
            T = T + M @ np.array(piv, float)
            M = M @ rot(*r)
        return M, T

    quads = []
    for (name, _, _, _, boxes) in PARTS:
        M, T = world(name)
        for (u, v, x, y, z, w, h, d, mirror, mat) in boxes:
            x0, y0, z0, x1, y1, z1 = x, y, z, x + w, y + h, z + d
            W, H, D = w, h, d
            fl = faces(u, v, W, H, D)
            corners = {  # face -> 4 corners (model space, y down) and its uv rect
                "front": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], "back": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
                "top": [(x0, y0, z0), (x0, y0, z1), (x1, y0, z1), (x1, y0, z0)], "bottom": [(x0, y1, z1), (x0, y1, z0), (x1, y1, z0), (x1, y1, z1)]}
            for fname, cs in corners.items():
                fu, fv, fw, fh = fl[fname]
                uv = [(fu + fw, fv + fh), (fu, fv + fh), (fu, fv), (fu + fw, fv)]
                if mirror: uv = [uv[1], uv[0], uv[3], uv[2]]
                P = [M @ np.array(c, float) + T for c in cs]
                quads.append((P, uv))
    Wd, Hd = 360, 420
    out = []
    for yaw in (180, 215, 0):                        # front (the model's face looks toward -z), three-quarter, back
        img = np.zeros((Hd, Wd, 3), np.float32) + np.array([0.30, 0.32, 0.38]); zb = np.full((Hd, Wd), np.inf)
        a = m.radians(yaw)
        for (P, uv) in quads:
            S2 = []
            for p in P:
                X, Y, Z = p[0] * m.cos(a) + p[2] * m.sin(a), p[1] + 9, -p[0] * m.sin(a) + p[2] * m.cos(a)
                S2.append((Wd / 2 + X * 6.5, 40 + (Y + 18) * 6.5, Z))
            for tri in ((0, 1, 2), (0, 2, 3)):
                (ax, ay, az), (bx, by, bz), (cx, cy, cz) = [S2[i] for i in tri]
                U = [uv[i] for i in tri]
                den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
                if abs(den) < 1e-9: continue
                x0_, x1_ = int(max(0, min(ax, bx, cx))), int(min(Wd - 1, max(ax, bx, cx)) + 1)
                y0_, y1_ = int(max(0, min(ay, by, cy))), int(min(Hd - 1, max(ay, by, cy)) + 1)
                if x0_ >= x1_ or y0_ >= y1_: continue
                yy, xx = np.mgrid[y0_:y1_, x0_:x1_] + 0.5
                w0 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
                w1 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
                w2 = 1 - w0 - w1
                ins = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
                z = w0 * az + w1 * bz + w2 * cz
                tu = np.clip((w0 * U[0][0] + w1 * U[1][0] + w2 * U[2][0]).astype(int), 0, S - 1)
                tv = np.clip((w0 * U[0][1] + w1 * U[1][1] + w2 * U[2][1]).astype(int), 0, S - 1)
                s, g = tex[tv, tu], glo[tv, tu]
                sub, reg = zb[y0_:y1_, x0_:x1_], img[y0_:y1_, x0_:x1_]
                mk = ins & (s[..., 3] > 0.1) & (z < sub)
                reg[mk] = np.clip(s[..., :3] * 1.2 + g[..., :3] * g[..., 3:4], 0, 1)[mk]
                sub[mk] = z[mk]
        out.append(Image.fromarray((img * 255).astype(np.uint8)))
    sheet = Image.new("RGB", (Wd * 3, Hd))
    for i, im in enumerate(out): sheet.paste(im, (i * Wd, 0))
    sheet.save(path)
    print("wrote", path)


if __name__ == "__main__" and "--preview" in sys.argv:
    preview(os.path.join(ROOT, "build", "zagred_true_preview.png"))
