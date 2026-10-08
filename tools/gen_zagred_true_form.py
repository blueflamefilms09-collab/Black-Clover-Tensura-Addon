"""0.48 Zagred true form: ONE part list -> the 128x128 skin + glow textures AND the Java geometry of client/ZagredModel.java.

    python tools/gen_zagred_true_form.py

A tall black devil in Minecraft cuboids: pale face with red eyes under black hair, pointed ears, segmented swept horns, a pale
ribcage over an ink-black body with violet rune lines, long arms ending in long claws, clawed feet, huge tattered bat wings in two
segments, and a long whip tail with a spade. The geometry block between the "generated" markers in ZagredModel.java is rewritten
from PARTS below, so texture and generated model always agree. Additional sculpted detail layers live outside that block and
survive regeneration. Old 0.47 textures (entity/zagred*.png) are left in place.
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

# (name, parent, pivot xyz, rotation xyz (radians), [boxes]); box = (x, y, z, w, h, d, mirror, material). Texture offsets are
# packed automatically (pack()), boxes of the same size and material share their spot (left and right mirror each other).
PARTS = []


def part(name, parent, pivot, rot, *boxes):
    PARTS.append((name, parent, pivot, rot, list(boxes)))


# 0.49: after the owner's two reference images: a very tall, thin black devil. Pale long face with red eyes and dark streaks,
# messy black hair with two upswept horns, long pointed ears, pale neck and spine, a pale ribcage in a V of black, black smoke
# clumps on the shoulders, very long thin arms with long curved claws, very long legs on clawed feet, huge plain dark wings and a
# long whip tail curling round to the front.
part("root", None, (0, 0, 0), (0, 0, 0))
part("body", "root", (0, 0, 0), (0, 0, 0),
     (-1.2, -2, -1.2, 2.4, 3, 2.4, False, "neck"),
     (-4, 0, -2, 8, 11, 4, False, "chest"),
     (-3, 1.5, -2.35, 6, 8.5, 0.8, False, "ribs"),
     (-2.2, 10, -1.7, 4.4, 4, 3.4, False, "black"),
     (-3, 14, -1.8, 6, 4, 3.6, False, "black"),
     (-5, 0, -1.5, 3, 3, 3, False, "smoke"), (2, 0, -1.5, 3, 3, 3, True, "smoke"),
     (-4.5, 1, 0.8, 2, 2, 2, False, "smoke"), (2.5, 1, 0.8, 2, 2, 2, True, "smoke"),
     (-4.3, 5, -1.8, 2, 2, 2, False, "smoke"), (2.3, 5, -1.8, 2, 2, 2, True, "smoke"))
part("head", "root", (0, 0, 0), (0, 0, 0),
     (-3.5, -8, -3.3, 7, 8, 6.6, False, "face"),
     (-4, -9, -3.7, 8, 3, 7.4, False, "hair"),
     (-3.5, -6.5, 2.8, 7, 6.5, 1.4, False, "hair"))
for k, (x, y, z, rx, rz) in enumerate([(-2.5, -8.5, -2, -0.4, 0.5), (1.5, -9, -1, 0.2, -0.4), (-0.5, -9.5, 1, 0.5, 0.1), (2.5, -7.5, 1.5, 0.6, -0.8),
                                       (-3.5, -7, 1, 0.3, 0.9), (0.5, -9, -2.5, -0.7, -0.2)]):
    part("hair_spike%d" % k, "head", (x, y, z), (rx, 0, rz), (-0.65, -2, -0.65, 1.3, 2, 1.3, False, "hair"))
for side, sx in (("r", -1), ("l", 1)):
    m = side == "l"
    part("ear_" + side, "head", (3.2 * sx, -4.2, 0), (0, 0, 0.15 * sx), (0 if m else -3, -0.5, -0.5, 3, 1, 1, m, "pale"))
    part("horn_" + side, "head", (1.8 * sx, -6.5, -0.5), (-0.15, 0, 0.35 * sx), (-0.7, -2.5, -0.7, 1.4, 2.5, 1.4, m, "horn"))
    part("horn_" + side + "_mid", "horn_" + side, (0, -2.2, 0), (0.1, 0, -0.35 * sx), (-0.5, -1.8, -0.5, 1, 1.8, 1, m, "horn"))
    part("horn_" + side + "_tip", "horn_" + side + "_mid", (0, -1.6, 0), (0.15, 0, -0.45 * sx), (-0.4, -1.2, -0.4, 0.8, 1.2, 0.8, m, "horn"))
    part("arm_" + side, "root", (5 * sx, 1.5, 0), (0, 0, -0.12 * sx), (-1.5, -1, -1.2, 3, 6, 2.4, m, "black"))
    part("forearm_" + side, "arm_" + side, (0, 5, 0), (-0.1, 0, 0), (-1.2, 0, -1, 2.4, 7, 2, m, "black"))
    part("hand_" + side, "forearm_" + side, (0, 6.5, 0), (0, 0, 0), (-1.2, 0, -1.2, 2.4, 2.4, 2.4, m, "black"))
    for k, cx in enumerate((-0.9, -0.3, 0.3, 0.9)):
        part("claw_%s%d" % (side, k), "hand_" + side, (cx, 1.8, -0.5), (-0.35, 0, 0.18 * (k - 1.5)), (-0.4, 0, -0.4, 0.8, 2.8, 0.8, m, "claw"))
        part("claw_%s%d_tip" % (side, k), "claw_%s%d" % (side, k), (0, 2.6, 0), (-0.75, 0, 0), (-0.35, 0, -0.35, 0.7, 1.8, 0.7, m, "clawtip"))
    part("leg_" + side, "root", (1.9 * sx, 12, 0), (0, 0, 0), (-1.5, 0, -1.3, 3, 6, 2.6, m, "black"))
    part("shin_" + side, "leg_" + side, (0, 6, 0), (0, 0, 0), (-1.25, 0, -1.1, 2.5, 6, 2.2, m, "black"))
    part("foot_" + side, "shin_" + side, (0, 6, 0), (0, 0, 0), (-1.25, 0, -2, 2.5, 1.5, 3, m, "black"))
    for k, cx in enumerate((-0.7, 0, 0.7)):
        part("toe_%s%d" % (side, k), "foot_" + side, (cx, 0.8, -2), (0.25, 0.25 * (k - 1), 0), (-0.4, -0.4, -2, 0.8, 0.8, 2, m, "claw"))
    part("wing_" + side, "body", (3.2 * sx, 2, 1.6), (0, -0.25 * sx, -0.25 * sx), (0 if m else -13, -0.8, -0.8, 13, 1.6, 1.6, m, "wingbone"))
    part("membrane_" + side, "wing_" + side, (0, 0.4, 0), (0, 0, 0), (0 if m else -13, 0, 0, 13, 12, 0, m, "membrane"))
    part("wing2_" + side, "wing_" + side, (13 * sx, 0, 0), (0, 0, -0.15 * sx), (0 if m else -10, -0.7, -0.7, 10, 1.4, 1.4, m, "wingbone"))
    part("membrane2_" + side, "wing2_" + side, (0, 0.4, 0), (0, 0, 0), (0 if m else -10, 0, 0, 10, 14, 0, m, "membrane"))
TAIL = 8
part("tail1", "body", (0, 15.5, 1.5), (-1.0, 0, 0), (-0.6, -0.6, 0, 1.2, 1.2, 4, False, "black"))
for k in range(2, TAIL + 1):
    part("tail%d" % k, "tail%d" % (k - 1), (0, 0, 3.8), (0.18 if k < 4 else 0.05, 0.42, 0), (-0.4, -0.4, 0, 0.8, 0.8, 4, False, "black"))


def pack():
    """Shelf-packs every distinct (size, material) box into the 128x128 texture; returns {(W, H, D, mat): (u, v)}."""
    import math as _m
    keys = []
    for (_, _, _, _, boxes) in PARTS:
        for (x, y, z, w, h, d, mirror, mat) in boxes:
            k = (int(_m.ceil(w)), int(_m.ceil(h)), int(_m.ceil(d)), mat)
            if k not in keys: keys.append(k)
    keys.sort(key=lambda k: -(k[2] + k[1]))
    spots, u, v, row = {}, 0, 0, 0
    for k in keys:
        W, H, D, _ = k
        bw, bh = 2 * (D + W), D + H
        if u + bw > S: u, v, row = 0, v + row, 0
        spots[k] = (u, v)
        u += bw
        row = max(row, bh)
    assert v + row <= S, "texture full"
    return spots


SPOTS = pack()


def uv_of(box):
    import math as _m
    x, y, z, w, h, d, mirror, mat = box
    return SPOTS[(int(_m.ceil(w)), int(_m.ceil(h)), int(_m.ceil(d)), mat)]


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
    done = set()

    def px(img, x, y, rgb, a=255):
        if 0 <= x < S and 0 <= y < S:
            img[y, x, :3] = np.clip(rgb, 0, 255)
            img[y, x, 3] = a

    for (_, _, _, _, boxes) in PARTS:
        for box in boxes:
            x, y, z, w, h, d, mirror, mat = box
            u, v = uv_of(box)
            W, H, D = int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))
            if (u, v) in done:
                continue
            done.add((u, v))
            for fname, (fu, fv, fw, fh) in faces(u, v, W, H, D).items():
                for j in range(fh):
                    for i in range(fw):
                        X, Y = fu + i, fv + j
                        nn = (n1[Y % S, X % S] - 0.5) * 22 + (n2[Y % S, X % S] - 0.5) * 12
                        lit = 1.15 if fname == "top" else 0.8 if fname == "bottom" else 1.0
                        g = None
                        if mat in ("black", "chest", "smoke"):
                            c = np.array([13, 12, 15]) + nn * 0.45
                            if mat == "smoke" and rng.random() < 0.3: c = np.array([30, 28, 32])
                            if mat == "chest" and fname == "front":                     # the pale V opening down the chest
                                half = (fw - 1) / 2
                                if abs(i - half) <= (fh - j) * 0.45:
                                    c = np.array([200, 194, 186]) + nn * 0.4 if (j % 2 == 0 or abs(i - half) < 0.6) else np.array([120, 112, 108])
                        elif mat == "neck":
                            c = np.array([196, 190, 184]) + nn * 0.4
                            if fname in ("front", "back") and i == fw // 2 and j % 2 == 0: c = np.array([150, 142, 138])   # vertebrae
                        elif mat == "ribs":
                            half = (fw - 1) / 2
                            c = np.array([214, 206, 196]) + nn * 0.4 if (j % 2 == 0 or abs(i - half) < 0.6) else np.array([92, 84, 82])
                        elif mat in ("face", "pale"):
                            c = np.array([206, 200, 194]) + nn * 0.35
                            if mat == "face" and fname == "front":
                                if j == 4 and i in (1, 4):
                                    c = np.array([255, 36, 30]); g = (255, 40, 30)              # red eyes
                                elif j == 4 and i in (2, 3):
                                    c = np.array([150, 140, 136])
                                elif j in (5, 6) and i in (1, 4):
                                    c = np.array([40, 22, 24]); g = (90, 6, 8)                    # dark streaks under the eyes
                                elif j == 3 and i in (0, 1, 4, 5):
                                    c = np.array([40, 22, 24])                                    # streaks above
                                elif j == 6 and 2 <= i <= 3:
                                    c = np.array([60, 50, 54])                                    # thin mouth
                                elif j == 7 and 1 <= i <= 4:
                                    c = np.array([170, 162, 158])
                        elif mat == "hair":
                            c = np.array([8, 7, 9]) + nn * 0.3
                            if rng.random() < 0.12: c = np.array([34, 32, 38])
                        elif mat == "horn":
                            c = np.array([16, 14, 18]) + nn * 0.3
                            if fname == "east" or fname == "west": c = c * 1.4
                        elif mat in ("claw", "clawtip"):
                            c = np.array([18, 16, 20]) + nn * 0.3
                            if fname == "front": c = np.array([60, 58, 64])                     # glossy edge
                        elif mat == "wingbone":
                            c = np.array([24, 24, 28]) + nn * 0.4
                        elif mat == "membrane":
                            c = np.array([26, 26, 30]) + nn * 0.5
                            if i % 6 == 0: c = np.array([16, 16, 19])                            # finger lines
                            edge = fh - j
                            if edge <= 3 and ((i * 7 + edge * 3) % 6 < edge - 1 or (i % 6 == 3 and edge <= 2)):
                                px(base, X, Y, (0, 0, 0), 0); continue                          # tattered trailing edge
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
        for box in boxes:
            x, y, z, w, h, d, mirror, mat = box
            u, v = uv_of(box)
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
        for box in boxes:
            x, y, z, w, h, d, mirror, mat = box
            u, v = uv_of(box)
            x0, y0, z0, x1, y1, z1 = x, y, z, x + w, y + h, z + d
            fl = faces(u, v, int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d)))
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
                X, Y, Z = p[0] * m.cos(a) + p[2] * m.sin(a), p[1] + 25.5, -p[0] * m.sin(a) + p[2] * m.cos(a)
                S2.append((Wd / 2 + X * 3.6, 20 + (Y + 36) * 3.6, Z))
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
