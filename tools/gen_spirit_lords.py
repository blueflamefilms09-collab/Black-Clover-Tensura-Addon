"""0.102: the Black Clover Spirit Lords, built from the reference art (assets/nusmp/geo/entity/spirit_<kind>.geo.json + animations + textures).

    python3 -B tools/gen_spirit_lords.py [salamander undine sylph gnome]

  salamander  a winged fire dragon, red-orange scales, flames down the spine and along the tail          (spirit_salamander)
  undine      a woman of water: foam hair and a wave dress that spill into waves, fin ears               (spirit_undine, spirit_undine_water)
  sylph       a green fairy with dragonfly wings and drifting bubbles                                    (spirit_sylph, spirit_sylph_wings)
  gnome       a hunched stone giant, cracked plates, dark hollow face with glowing eyes                  (spirit_gnome)

Each kind has the clips idle, move (walk / fly), cast and hit. Files named <model>_glow.png are the emissive layers.
Signs of animation rotations follow client/geo/GeoDraw; check with python3 -B tools/render_preview/preview.py spirit_<kind>_idle.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim  # noqa: E402
from spirit_geo import Sheet, nz, fz, hsh, rgba, mix, shade, voronoi  # noqa: E402

S = math.sin
TAU = 2 * math.pi


def sway(a, bone, clip, base_rot, amp, period, phase=0.0, axis=0, steps=8, length=None):
    length = length or period
    frames = {}
    for k in range(steps + 1):
        t = length * k / steps
        r = list(base_rot)
        r[axis] += amp * S(TAU * t / period + phase)
        frames[t] = tuple(r)
    a.rot(clip, bone, frames)


def finish(m, sheet, anim, glow_name=None):
    img, glow = sheet.images()
    m.save(anim)
    m.save_texture(img, glow)
    print(m.id, "cubes", m.cube_count(), "texture rows used", sheet.used(), "of", sheet.H)


# =====================================================================================================================
# SALAMANDER
# =====================================================================================================================
def scales(base, dark, belly=None, size=3.0, salt=0, top=None, glow_edge=None):
    def p(face, i, j, fw, fh):
        d, cid = voronoi(i + 0.5 + salt * 13, j + 0.5, size, salt)
        c = np.array(base, np.float32) * (0.82 + 0.36 * cid)
        if face == "top" and top is not None:
            c = np.array(top, np.float32) * (0.85 + 0.3 * cid)
        if face == "bottom" and belly is not None:
            return rgba(np.array(belly, np.float32) * (0.9 + 0.2 * ((j // 2) % 2))), None
        if d < 0.4:
            c = np.array(dark, np.float32)
        elif d < 0.9:
            c = c * 0.9
        elif d > 1.8:
            c = c * 1.08 + 8                                         # the plated middle of each scale catches the light
        c = c + fz(i, j, salt + 3) * 10
        g = None
        if glow_edge is not None and d < 0.4 and hsh(i, j, salt) > 0.85:
            g = rgba(glow_edge, 120)
        return rgba(c), g
    return p


def salamander():
    m = Model("spirit", "salamander", 256, 256)
    sh = Sheet(256, 256)
    A = Anim()
    RED, DARK, BELLY, TOP = (226, 78, 34), (138, 34, 22), (246, 186, 96), (190, 52, 28)
    HORN = (244, 218, 160)
    FLAME = (255, 150, 30)
    sc = scales(RED, DARK, BELLY, 3.0, 1, TOP, FLAME)
    sc2 = scales((212, 66, 30), DARK, BELLY, 2.5, 2, TOP, FLAME)

    def flame(face, i, j, fw, fh):
        t = (j / max(1, fh - 1))
        c = mix((255, 244, 150), (255, 110, 20), t)
        return rgba(c), rgba(c, 255)

    def horn(face, i, j, fw, fh):
        return rgba(shade(HORN, 0.8 + 0.3 * ((j // 2) % 2))), None

    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 15, 0))
    sh.box(m, "body", (-5, 9, -8), (10, 12, 16), sc)
    sh.box(m, "body", (-5.5, 9, -11), (11, 13, 4), sc2)                                     # the chest
    for k, z in enumerate((-7, -3, 1, 5)):                                                  # flames down the spine
        sh.box(m, "body", (-1, 21, z), (2, 3, 2), flame)
    # neck and head
    m.bone("neck1", "body", pivot=(0, 19, -10))
    sh.box(m, "neck1", (-2.5, 18, -15), (5, 6, 6), sc2)
    m.bone("neck2", "neck1", pivot=(0, 22, -14))
    sh.box(m, "neck2", (-2.25, 21, -19), (4.5, 5.5, 6), sc2)
    m.bone("head", "neck2", pivot=(0, 24, -18))

    def head_paint(face, i, j, fw, fh):
        c, g = sc(face, i, j, fw, fh)
        if face in ("left", "right") and 2 <= j <= 3 and ((face == "right" and fw - 4 <= i <= fw - 2) or (face == "left" and 1 <= i <= 3)):
            return rgba((255, 224, 90)), rgba((255, 210, 60))                               # the slit eye
        if face == "front" and j >= fh - 2:
            return rgba((60, 12, 10)), None
        return c, g
    sh.box(m, "head", (-4, 21.5, -27), (8, 7, 9), head_paint)
    sh.box(m, "head", (-3, 22, -33), (6, 4, 6), sc2)                                    # the snout
    for sx in (-1, 1):
        x0 = 1.8 if sx > 0 else -3.4
        sh.box(m, "head", (x0, 28, -23), (1.8, 1.8, 9), horn, rotation=(28, 0, 0), pivot=(x0 + 0.9, 28.5, -22))
        sh.box(m, "head", (x0 + sx * 0.8, 24.5, -25), (1.6, 1.6, 5), horn, rotation=(0, sx * -25, 0), pivot=(x0 + 0.8, 25, -25))   # swept-back cheek spikes
    m.bone("jaw", "head", pivot=(0, 23.5, -22))
    sh.box(m, "jaw", (-2.5, 21, -31), (5, 1.8, 8.5), scales(DARK, (60, 14, 12), BELLY, 2.5, 4))
    # tail
    prev, tz = "body", 8
    for n, (w, h, d, y) in enumerate(((7, 7, 7, 10.5), (5.5, 6, 7, 11), (4, 4.5, 7, 11.5), (3, 3.5, 6, 11.5))):
        name = "tail%d" % (n + 1)
        m.bone(name, prev, pivot=(0, 14.5, tz))
        sh.box(m, name, (-w / 2, y, tz), (w, h, d), sc2 if n % 2 else sc)
        prev, tz = name, tz + d - 0.5
    m.bone("tailflame", prev, pivot=(0, 13, tz))
    sh.box(m, "tailflame", (-2.5, 10.5, tz), (5, 7, 5), flame)
    sh.box(m, "tailflame", (-1.5, 17, tz + 0.5), (3, 4, 3), flame)
    # legs
    for name, x, z, big in (("leg_fl", 5, -6, False), ("leg_fr", -5, -6, False), ("leg_bl", 5, 6, True), ("leg_br", -5, 6, True)):
        w = 5 if big else 4
        d = 6 if big else 4
        x0 = x - w / 2 + (1.2 if x > 0 else -1.2)
        m.bone(name, "body", pivot=(x0 + w / 2, 13, z))
        sh.box(m, name, (x0, 5, z - d / 2), (w, 8.5, d), sc)
        sh.box(m, name, (x0 + 0.5, 1.5, z - d / 2 + 0.8), (w - 1, 4.5, d - 1.6), sc2)
        sh.box(m, name, (x0 - 0.3, 0, z - d / 2 - 2), (w + 0.6, 2, d + 1.5), scales(DARK, (60, 14, 12), None, 2, 6))
        for c in range(3):
            sh.box(m, name, (x0 + 0.2 + c * (w - 0.8) / 2.4, 0, z - d / 2 - 3.2), (1, 1.4, 1.5), horn)   # claws

    # wings: an arm, a forearm and a scalloped membrane that spreads sideways
    def membrane(sx):
        def p(face, i, j, fw, fh):
            if face not in ("top", "bottom"):
                return None, None
            fx = i / max(1, fw - 1) if sx > 0 else 1 - i / max(1, fw - 1)       # 0 at the body, 1 at the tip
            depth = fh
            edge = depth * (0.55 + 0.45 * (1 - fx) ** 0.7) - 0.0
            scallop = 2.4 * abs(S(fx * TAU * 2.6)) * (0.4 + fx)
            if j > edge - scallop:
                return None, None
            c = mix((236, 120, 36), (176, 52, 24), min(1, j / max(1, edge)))
            vein = (abs(S((j + 1) * 0.0 + fx * 14)) < 0.14)
            if vein:
                c = (110, 28, 18)
            g = rgba((255, 150, 40), 90) if j > edge - scallop - 1.6 else None
            return rgba(c), g
        return p

    for sx, name in ((1, "wing_l"), (-1, "wing_r")):
        m.bone(name, "body", pivot=(sx * 4, 20, -3))
        sh.box(m, name, (4 if sx > 0 else -18, 19, -4), (14, 2.6, 2.6), sc)
        sh.box(m, name, (4 if sx > 0 else -18, 19.5, -2), (14, 0.5, 18), membrane(sx))
        m.bone(name + "2", name, pivot=(sx * 18, 20, -3))
        sh.box(m, name + "2", (18 if sx > 0 else -32, 19.3, -3.8), (14, 1.8, 1.8), sc2)
        sh.box(m, name + "2", (18 if sx > 0 else -32, 19.7, -2), (14, 0.5, 17), membrane(sx))
        for f, fl in enumerate((8, 10, 9)):
            sh.box(m, name + "2", ((26 + f * 2) if sx > 0 else -(26 + f * 2 + 1), 19.8, -2 + f * 3), (1, 0.8, fl * 0.5), horn)
    # ---------------------------------------------------------------- animations
    A.clip("idle", 3.0, True)
    sway(A, "body", "idle", (0, 0, 0), 1.5, 3.0, axis=0)
    sway(A, "neck1", "idle", (0, 0, 0), 3, 3.0, 0.8, axis=0)
    sway(A, "neck2", "idle", (0, 0, 0), 4, 3.0, 1.6, axis=1)
    sway(A, "head", "idle", (0, 0, 0), 3, 3.0, 2.4, axis=0)
    for k in range(1, 5):
        sway(A, "tail%d" % k, "idle", (0, 0, 0), 7, 3.0, -0.7 * k, axis=1)
    sway(A, "tailflame", "idle", (0, 0, 0), 9, 1.5, 0.5, axis=1, length=3.0)
    sway(A, "wing_l", "idle", (0, 0, 22), 7, 3.0, 0.2, axis=2)
    sway(A, "wing_r", "idle", (0, 0, -22), -7, 3.0, 0.2, axis=2)
    sway(A, "wing_l2", "idle", (0, 0, 20), 9, 3.0, 0.9, axis=2)
    sway(A, "wing_r2", "idle", (0, 0, -20), -9, 3.0, 0.9, axis=2)
    A.clip("move", 0.9, True)
    sway(A, "body", "move", (0, 0, 0), 3, 0.9, axis=0)
    for k in range(1, 5):
        sway(A, "tail%d" % k, "move", (0, 0, 0), 12, 0.9, -0.9 * k, axis=1)
    sway(A, "tailflame", "move", (0, 0, 0), 14, 0.9, 0.0, axis=1)
    sway(A, "neck1", "move", (-6, 0, 0), 3, 0.9, 0, axis=0)
    sway(A, "wing_l", "move", (0, 0, 0), 38, 0.9, 0, axis=2)
    sway(A, "wing_r", "move", (0, 0, 0), -38, 0.9, 0, axis=2)
    sway(A, "wing_l2", "move", (0, 0, 8), 28, 0.9, 0.7, axis=2)
    sway(A, "wing_r2", "move", (0, 0, -8), -28, 0.9, 0.7, axis=2)
    for n, ph in (("leg_fl", 0), ("leg_br", 0), ("leg_fr", math.pi), ("leg_bl", math.pi)):
        sway(A, n, "move", (0, 0, 0), 12, 0.9, ph, axis=0)
    A.clip("cast", 0.9, False)
    A.rot("cast", "neck1", {0.0: (0, 0, 0), 0.25: (-20, 0, 0), 0.6: (14, 0, 0), 0.9: (0, 0, 0)})
    A.rot("cast", "neck2", {0.0: (0, 0, 0), 0.25: (-18, 0, 0), 0.6: (10, 0, 0), 0.9: (0, 0, 0)})
    A.rot("cast", "head", {0.0: (0, 0, 0), 0.25: (-14, 0, 0), 0.6: (12, 0, 0), 0.9: (0, 0, 0)})
    A.rot("cast", "jaw", {0.0: (0, 0, 0), 0.25: (0, 0, 0), 0.35: (34, 0, 0), 0.75: (34, 0, 0), 0.9: (0, 0, 0)})
    A.rot("cast", "wing_l", {0.0: (0, 0, 8), 0.3: (0, 0, 34), 0.9: (0, 0, 8)})
    A.rot("cast", "wing_r", {0.0: (0, 0, -8), 0.3: (0, 0, -34), 0.9: (0, 0, -8)})
    A.clip("hit", 0.35, False)
    A.rot("hit", "body", {0.0: (0, 0, 0), 0.12: (-8, 0, 0), 0.35: (0, 0, 0)})
    A.rot("hit", "head", {0.0: (0, 0, 0), 0.12: (14, 0, 0), 0.35: (0, 0, 0)})
    finish(m, sh, A)


# =====================================================================================================================
# UNDINE
# =====================================================================================================================
def undine():
    SKIN, SKIN_D = (236, 242, 252), (196, 214, 240)
    HAIR, HAIR_D, HAIR_L = (176, 212, 252), (98, 152, 230), (222, 240, 255)
    BLUE, BLUE_D, BLUE_L, FOAM = (46, 124, 232), (22, 80, 196), (116, 196, 255), (248, 252, 255)
    m = Model("spirit", "undine", 256, 256)
    sh = Sheet(256, 256)
    A = Anim()

    def skin(face, i, j, fw, fh):
        c = mix(SKIN, SKIN_D, 0.2 + 0.2 * fz(i, j, 2))
        return rgba(c), rgba(c, 70)

    def bodice(face, i, j, fw, fh):                           # skin above, a wave-edged blue bodice below
        edge = fh * 0.38 + 1.1 * S(i * 1.7 + 0.5)
        if face in ("top", "bottom") or j > edge:
            t = (j - edge) / max(1, fh - edge)
            c = mix(BLUE_L, BLUE, min(1, t * 1.3)) if face in ("front", "back", "left", "right") else BLUE
            if abs(j - edge) < 1.0 and face not in ("top", "bottom"):
                c = FOAM
            return rgba(c), rgba(BLUE_L, 70) if abs(j - edge) < 1.0 else None
        return skin(face, i, j, fw, fh)

    def skirt(top_foam=True, salt=0):
        def p(face, i, j, fw, fh):
            t = j / max(1, fh - 1)
            c = mix(BLUE_L, BLUE_D, 0.25 + 0.55 * t) if face != "top" else FOAM
            c = np.array(c, np.float32) + fz(i, j, salt) * 14
            wave = 0.5 + 0.5 * S(i * 1.3 + j * 0.4 + salt)
            g = None
            if face in ("front", "back", "left", "right"):
                if j <= 1 or (wave > 0.86 and j < fh - 1):
                    c = np.array(FOAM, np.float32) - fz(i, j, salt + 1) * 18
                    g = rgba(BLUE_L, 80)
                elif wave < 0.18:
                    c = c * 0.86
            return rgba(c), g
        return p

    def hair(face, i, j, fw, fh):
        t = j / max(1, fh - 1)
        c = np.array(mix(HAIR_L, HAIR, 0.35 + 0.45 * t), np.float32) + fz(i, j, 6) * 14
        if int(i + 3 * S(j * 0.7)) % 5 == 0:
            c = c * 0.9
        return rgba(c), rgba(c, 60)

    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 16, 0))
    sh.box(m, "body", (-3.5, 15, -2), (7, 8, 4), bodice)
    n = 0
    y = 12.0
    while y > 0:
        w = 6.2 + (12 - y) * 0.82
        d = 4.0 + (12 - y) * 0.68
        h = min(2.6, y) + (0.4 if y > 1.5 else 0)
        sh.box(m, "body", (-w / 2, max(0, y - 2.6), -d / 2), (w, h, d), skirt(salt=n + 1))
        y -= 2.2
        n += 1
    # head
    m.bone("head", "body", pivot=(0, 23, 0))

    def face_paint(face, i, j, fw, fh):
        if face == "front":
            c, g = skin(face, i, j, fw, fh)
            eye = i in (1, 2, 4, 5)
            if j == 2 and eye:
                return rgba((60, 84, 140)), None                                   # brows
            if j == 3 and eye:
                return rgba((255, 255, 255)), None
            if j == 4 and eye:
                return rgba((110, 190, 248)), rgba((120, 200, 255), 230)           # pale blue eyes
            if j == 5 and eye and i in (1, 5):
                return rgba((40, 60, 110)), None
            if j == 6 and i == 3:
                return rgba((196, 120, 140)), None
            return c, g
        if face == "top":
            return hair(face, i, j, fw, fh)
        return skin(face, i, j, fw, fh)
    sh.box(m, "head", (-3.5, 23, -3.5), (7, 7, 7), face_paint)
    sh.box(m, "head", (-4, 29.5, -4), (8, 2.5, 8), hair, inflate=0.1)               # hair cap
    sh.box(m, "head", (-4, 23.5, 3.2), (8, 7.5, 1.5), hair)                           # hair behind the head
    for sx, x in ((1, 3.6), (-1, -5.1)):
        sh.box(m, "head", (x, 23.5, -4), (1.5, 6.5, 3.5), hair)                      # the side bangs
    # fin ears (side plates, point out and back)
    def fin(i, j):
        k = 1 - j / 6.0
        if i > 5 - 5 * k or j > 6:
            return None, None
        return rgba(mix(BLUE_L, FOAM, 0.4 + 0.4 * (i / 5.0))), rgba(BLUE_L, 90)
    for sx, x in ((1, 3.5), (-1, -4.5)):
        sh.side_plate(m, "head", (x, 26, -1.5), (6, 7), fin, rotation=(0, sx * 25, 0), pivot=(x, 26, -1.5))
    # hair: scalloped flowing plates behind and beside
    m.bone("hair", "body", pivot=(0, 28, 3))

    def hairwave(w, h, salt, light):
        def sp(i, j, front):
            bottom = h - 5 - 3.5 * abs(S(i * 0.75 + salt)) - 2.5 * abs(S(i * 0.31 + salt * 2))
            side = abs(i - w / 2) / (w / 2)
            top = 2 + 6 * side ** 3
            if j > bottom or j < top:
                return None, None
            t = j / h
            c = np.array(mix(HAIR_L if light else HAIR, HAIR_D, t * 0.8), np.float32) + fz(i, j, salt) * 12
            if int(i + 4 * S(j * 0.5 + salt)) % 6 == 0:
                c = c * 0.88
            if j > bottom - 1.6:
                c = np.array(FOAM, np.float32)
            return rgba(c), (rgba(BLUE_L, 70) if j > bottom - 1.6 else None)
        return sp
    sh.plate(m, "hair", (-9, 3, 3.5), (18, 28), hairwave(18, 28, 1.0, False))
    sh.plate(m, "hair", (-7, 5, 4.7), (14, 24), hairwave(14, 24, 2.3, True))
    sh.plate(m, "hair", (-11, 8, 2.8), (22, 22), hairwave(22, 22, 4.1, False), rotation=(0, 18, 0), pivot=(0, 8, 3))
    sh.plate(m, "hair", (-11, 8, 2.9), (22, 22), hairwave(22, 22, 5.9, True), rotation=(0, -18, 0), pivot=(0, 8, 3))
    sh.side_plate(m, "head", (4.6, 12, -2.5), (6, 16), lambda i, j: hairwave(6, 16, 2.0, False)(i, j, True))
    sh.side_plate(m, "head", (-5.6, 12, -2.5), (6, 16), lambda i, j: hairwave(6, 16, 3.0, False)(i, j, True))
    # arms
    for sx, name in ((1, "arm_l"), (-1, "arm_r")):
        m.bone(name, "body", pivot=(sx * 4.6, 22.5, 0))
        x0 = 3.5 if sx > 0 else -6.0
        sh.box(m, name, (x0, 16.5, -1.25), (2.5, 6.5, 2.5), skin)
        m.bone(name + "2", name, pivot=(sx * 4.8, 16.5, 0))
        sh.box(m, name + "2", (x0 + 0.1, 10.5, -1.1), (2.3, 6, 2.2), skin)
        sh.box(m, name + "2", (x0, 8.5, -1.3), (2.5, 2.5, 2.6), skin)
    # ---------------------------------------------------------------- animations
    A.clip("idle", 3.2, True)
    sway(A, "root", "idle", (0, 0, 0), 0, 3.2, axis=0)
    A.pos("idle", "root", {0.0: (0, 0, 0), 0.8: (0, 1.4, 0), 1.6: (0, 0, 0), 2.4: (0, 1.4, 0), 3.2: (0, 0, 0)})
    sway(A, "hair", "idle", (0, 0, 0), 4, 3.2, axis=0)
    sway(A, "head", "idle", (0, 0, 0), 3, 3.2, 1.0, axis=1)
    sway(A, "arm_l", "idle", (0, 0, 12), 5, 3.2, axis=2)
    sway(A, "arm_r", "idle", (0, 0, -12), -5, 3.2, axis=2)
    sway(A, "arm_l2", "idle", (-14, 0, 0), 5, 3.2, 0.5, axis=0)
    sway(A, "arm_r2", "idle", (-14, 0, 0), 5, 3.2, 0.5, axis=0)
    A.clip("move", 1.6, True)
    A.pos("move", "root", {0.0: (0, 0, 0), 0.4: (0, 2.2, 0), 0.8: (0, 0, 0), 1.2: (0, 2.2, 0), 1.6: (0, 0, 0)})
    sway(A, "body", "move", (-8, 0, 0), 2, 1.6, axis=0)
    sway(A, "hair", "move", (14, 0, 0), 6, 1.6, axis=0)
    sway(A, "arm_l", "move", (0, 0, 24), 7, 1.6, axis=2)
    sway(A, "arm_r", "move", (0, 0, -24), -7, 1.6, axis=2)
    A.clip("cast", 0.9, False)
    A.rot("cast", "arm_l", {0.0: (0, 0, 12), 0.25: (-95, 0, 8), 0.7: (-95, 0, 8), 0.9: (0, 0, 12)})
    A.rot("cast", "arm_r", {0.0: (0, 0, -12), 0.25: (-95, 0, -8), 0.7: (-95, 0, -8), 0.9: (0, 0, -12)})
    A.rot("cast", "head", {0.0: (0, 0, 0), 0.3: (-8, 0, 0), 0.9: (0, 0, 0)})
    A.rot("cast", "hair", {0.0: (0, 0, 0), 0.3: (22, 0, 0), 0.9: (0, 0, 0)})
    A.clip("hit", 0.35, False)
    A.rot("hit", "body", {0.0: (0, 0, 0), 0.12: (-10, 0, 0), 0.35: (0, 0, 0)})
    finish(m, sh, A)

    # the water that follows her: hem waves, a swirling pool and drops that circle her (translucent pass)
    w = Model("spirit", "undine_water", 128, 128)
    sw = Sheet(128, 128)
    B = Anim()

    def wave_plate(width, height, salt):
        def sp(i, j, front):
            crest = 3.0 + 3.0 * abs(S(i * 0.42 + salt)) + 1.0 * S(i * 0.9 + salt * 2)
            if j < crest:
                return None, None
            t = j / height
            c = mix(FOAM, BLUE, min(1, t * 1.15))
            if j < crest + 1.5:
                return rgba(FOAM, 230), rgba(BLUE_L, 140)
            return rgba(c, 190 - 50 * t), rgba(BLUE_L, 50) if hsh(i, j, salt) > 0.9 else None
        return sp
    w.bone("root", pivot=(0, 0, 0))
    w.bone("hem", "root", pivot=(0, 4, 0))
    sw.plate(w, "hem", (-14, 0, -0.5), (28, 13), wave_plate(28, 13, 0.0))
    sw.side_plate(w, "hem", (-0.5, 0, -14), (28, 13), lambda i, j: wave_plate(28, 13, 1.7)(i, j, True))
    sw.plate(w, "hem", (-12, 0, -0.5), (24, 11), wave_plate(24, 11, 3.1), rotation=(0, 45, 0), pivot=(0, 0, 0))
    sw.plate(w, "hem", (-12, 0, -0.5), (24, 11), wave_plate(24, 11, 4.3), rotation=(0, -45, 0), pivot=(0, 0, 0))

    def spiral(i, j, top):
        cx = cy = 14
        dx, dy = i - cx, j - cy
        r = math.hypot(dx, dy)
        if r > 14:
            return None, None
        a = math.atan2(dy, dx)
        v = (r * 0.55 - a / TAU * 3 + 40) % 1.0
        if v < 0.22 and r > 3:
            return rgba(FOAM, 200), rgba(BLUE_L, 110)
        return rgba(BLUE, 90 - 50 * r / 14), None
    w.bone("pool", "root", pivot=(0, 0.5, 0))
    sw.flat_plate(w, "pool", (-14, 0, -14), (28, 28), spiral, thick=0.5)
    for n in range(4):
        w.bone("orbit%d" % n, "root", pivot=(0, 0, 0))

        def drop(face, i, j, fw, fh):
            return rgba(mix(FOAM, BLUE_L, 0.4), 215), rgba(BLUE_L, 110)
        sw.box(w, "orbit%d" % n, (11 - n, 6 + n * 5, -1), (2.5, 3.5, 2.5), drop)
    B.clip("idle", 3.2, True)
    B.pos("idle", "root", {0.0: (0, 0, 0), 0.8: (0, 1.4, 0), 1.6: (0, 0, 0), 2.4: (0, 1.4, 0), 3.2: (0, 0, 0)})
    A_rot = {0.0: (0, 0, 0), 1.6: (0, 180, 0), 3.2: (0, 360, 0)}
    B.rot("idle", "hem", A_rot)
    B.rot("idle", "pool", {0.0: (0, 0, 0), 1.6: (0, -180, 0), 3.2: (0, -360, 0)})
    for n in range(4):
        B.rot("idle", "orbit%d" % n, {0.0: (0, n * 90, 0), 1.6: (0, n * 90 + 180, 0), 3.2: (0, n * 90 + 360, 0)})
    B.clip("move", 1.6, True)
    B.pos("move", "root", {0.0: (0, 0, 0), 0.4: (0, 2.2, 0), 0.8: (0, 0, 0), 1.2: (0, 2.2, 0), 1.6: (0, 0, 0)})
    B.rot("move", "hem", {0.0: (0, 0, 0), 0.8: (0, 180, 0), 1.6: (0, 360, 0)})
    B.rot("move", "pool", {0.0: (0, 0, 0), 0.8: (0, -180, 0), 1.6: (0, -360, 0)})
    for n in range(4):
        B.rot("move", "orbit%d" % n, {0.0: (0, n * 90, 0), 0.8: (0, n * 90 + 180, 0), 1.6: (0, n * 90 + 360, 0)})
    B.clip("cast", 0.9, False)
    B.rot("cast", "hem", {0.0: (0, 0, 0), 0.9: (0, 360, 0)})
    B.rot("cast", "pool", {0.0: (0, 0, 0), 0.9: (0, -360, 0)})
    for n in range(4):
        B.rot("cast", "orbit%d" % n, {0.0: (0, n * 90, 0), 0.9: (0, n * 90 + 720, 0)})
    B.clip("hit", 0.35, False)
    B.rot("hit", "hem", {0.0: (0, 0, 0), 0.35: (0, 40, 0)})
    img, glow = sw.images()
    w.save(B)
    w.save_texture(img, glow)
    print(w.id, "cubes", w.cube_count())


# =====================================================================================================================
# SYLPH
# =====================================================================================================================
def sylph():
    SKIN, SKIN_D = (250, 244, 214), (222, 224, 160)
    HAIR, HAIR_D, HAIR_L = (168, 214, 74), (104, 160, 40), (222, 244, 120)
    LEAF, LEAF_D, LEAF_L = (74, 188, 96), (36, 128, 70), (150, 236, 150)
    m = Model("spirit", "sylph", 128, 128)
    sh = Sheet(128, 128)
    A = Anim()

    def skin(face, i, j, fw, fh):
        c = mix(SKIN, SKIN_D, 0.2 + 0.25 * fz(i, j, 12))
        return rgba(c), rgba(c, 70)

    def leafy(salt, scallop=True):
        def p(face, i, j, fw, fh):
            c = np.array(mix(LEAF_L, LEAF, 0.35 + 0.5 * j / max(1, fh)), np.float32) + fz(i, j, salt) * 14
            if (i + j * 2 + salt) % 5 == 0:
                c = c * 0.82
            if j >= fh - 1 and face in ("front", "back", "left", "right") and scallop:
                c = np.array(LEAF_D, np.float32)
            return rgba(c), rgba(LEAF_L, 60) if hsh(i, j, salt) > 0.8 else None
        return p

    def hair(face, i, j, fw, fh):
        c = np.array(mix(HAIR_L, HAIR, 0.25 + 0.5 * (j / max(1, fh))), np.float32) + fz(i, j, 9) * 18
        if (i * 3 + j) % 4 == 0:
            c = c * 0.86
        return rgba(c), rgba(HAIR_L, 70)

    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 14, 0))
    sh.box(m, "body", (-2, 10, -1.5), (4, 6, 3), leafy(1, False))
    sh.box(m, "body", (-3, 7, -2.5), (6, 3.5, 5), leafy(2))
    sh.box(m, "body", (-3.8, 5.6, -3.3), (7.6, 2.2, 6.6), leafy(3))
    m.bone("head", "body", pivot=(0, 16, 0))

    def face_paint(face, i, j, fw, fh):
        if face == "front":
            c, g = skin(face, i, j, fw, fh)
            eye = i in (1, 2, 3, 4) if False else (i in (1, 2) or i in (fw - 3, fw - 2))
            if j == 2 and eye:
                return rgba((70, 100, 40)), None
            if j in (3, 4) and eye:
                return (rgba((255, 255, 255)) if j == 3 else rgba((60, 190, 90))), (None if j == 3 else rgba((90, 240, 120), 230))
            if j == 5 and fw // 2 - 1 <= i <= fw // 2:
                return rgba((220, 120, 120)), None
            return c, g
        if face == "top":
            return hair(face, i, j, fw, fh)
        return skin(face, i, j, fw, fh)
    sh.box(m, "head", (-3, 16, -3), (6, 6, 6), face_paint)
    sh.box(m, "head", (-3.5, 21.2, -3.5), (7, 2.2, 7), hair, inflate=0.1)
    sh.box(m, "head", (-3.5, 15.6, 2.4), (7, 6.5, 1.5), hair)
    sh.box(m, "head", (-3.6, 17, -3.4), (1.2, 4.6, 2), hair)
    sh.box(m, "head", (2.4, 17, -3.4), (1.2, 4.6, 2), hair)
    for k, (x, z, rx, rz) in enumerate(((-2.5, -1, -20, 22), (0.5, 1, 18, 0), (2.2, -2, -14, -24), (-1, 3, 30, 10))):       # wild tufts
        sh.box(m, "head", (x, 22.4, z), (1.5, 4, 1.5), hair, rotation=(rx, 0, rz), pivot=(x + 0.7, 22.4, z + 0.7))
    for x, sgn in ((3, 1), (-3.8, -1)):                                                       # pointed ears
        sh.side_plate(m, "head", (x + (0.6 if sgn > 0 else 0), 18.5, -1.0), (3, 3), lambda i, j: (rgba(SKIN), rgba(SKIN, 60)) if i + j < 4 else (None, None))
    for sx, name in ((1, "arm_l"), (-1, "arm_r")):
        m.bone(name, "body", pivot=(sx * 2.2, 15.2, 0))
        x0 = 1.0 if sx > 0 else -2.8
        sh.box(m, name, (x0, 8.8, -0.9), (1.8, 6.8, 1.8), skin)
        sh.box(m, name, (x0 - 0.1, 7.2, -1.0), (2, 2, 2), skin)
    for sx, name in ((1, "leg_l"), (-1, "leg_r")):
        m.bone(name, "body", pivot=(sx * 1.2, 7, 0))
        x0 = 0.3 if sx > 0 else -2.1
        sh.box(m, name, (x0, 0.5, -0.9), (1.8, 6.6, 1.8), skin)
        sh.box(m, name, (x0 - 0.1, 0, -1.6), (2, 1.2, 3), leafy(7 + sx))
    A.clip("idle", 2.4, True)
    A.pos("idle", "root", {0.0: (0, 0, 0), 0.6: (0, 1.6, 0), 1.2: (0, 0, 0), 1.8: (0, 1.6, 0), 2.4: (0, 0, 0)})
    sway(A, "body", "idle", (0, 0, 0), 4, 2.4, axis=0)
    sway(A, "head", "idle", (0, 0, 0), 5, 2.4, 0.6, axis=1)
    sway(A, "arm_l", "idle", (0, 0, 50), 8, 2.4, axis=2)
    sway(A, "arm_r", "idle", (0, 0, -50), -8, 2.4, axis=2)
    sway(A, "leg_l", "idle", (0, 0, 0), 10, 2.4, 0.8, axis=0)
    sway(A, "leg_r", "idle", (0, 0, 0), 10, 2.4, 3.9, axis=0)
    A.clip("move", 0.8, True)
    A.pos("move", "root", {0.0: (0, 0, 0), 0.2: (0, 1.8, 0), 0.4: (0, 0, 0), 0.6: (0, 1.8, 0), 0.8: (0, 0, 0)})
    sway(A, "body", "move", (-18, 0, 0), 3, 0.8, axis=0)
    sway(A, "arm_l", "move", (0, 0, 62), 6, 0.8, axis=2)
    sway(A, "arm_r", "move", (0, 0, -62), -6, 0.8, axis=2)
    sway(A, "leg_l", "move", (14, 0, 0), 12, 0.8, axis=0)
    sway(A, "leg_r", "move", (10, 0, 0), 12, 0.8, 3.1, axis=0)
    A.clip("cast", 0.8, False)
    A.rot("cast", "arm_l", {0.0: (0, 0, 50), 0.2: (-100, 0, 20), 0.6: (-100, 0, 20), 0.8: (0, 0, 50)})
    A.rot("cast", "arm_r", {0.0: (0, 0, -50), 0.2: (-100, 0, -20), 0.6: (-100, 0, -20), 0.8: (0, 0, -50)})
    A.rot("cast", "body", {0.0: (0, 0, 0), 0.25: (-14, 0, 0), 0.8: (0, 0, 0)})
    A.clip("hit", 0.35, False)
    A.rot("hit", "body", {0.0: (0, 0, 0), 0.12: (-14, 0, 0), 0.35: (0, 0, 0)})
    finish(m, sh, A)

    # dragonfly wings and the bubbles that drift around her (translucent pass)
    w = Model("spirit", "sylph_wings", 128, 128)
    sw = Sheet(128, 128)
    B = Anim()

    def wing_shape(width, height, salt, mirror):
        def sp(i, j, front):
            u = (width - 1 - i if mirror else i) / max(1, width - 1)       # 0 at the hinge, 1 at the tip
            v = j / max(1, height - 1)
            cx = 0.5 + 0.1 * u
            ell = ((v - cx) / (0.5 * (1 - 0.55 * u ** 2))) ** 2 + (u / 1.0) ** 2 * 0.0
            if abs(v - cx) > 0.5 * (1 - 0.7 * u * u) * (1.0 + 0.3 * (1 - u)) or u > 0.985:
                return None, None
            vein = abs(((v - cx) * 7 + u * 2.2) % 1.0 - 0.5) < 0.1 or abs((u * 5) % 1.0 - 0.5) < 0.07 and abs(v - cx) < 0.3
            if vein or abs(v - cx) > 0.46 * (1 - 0.7 * u * u):
                return rgba((196, 255, 190), 235), rgba((170, 255, 170), 160)
            return rgba(mix((150, 240, 170), (220, 255, 210), u), 120), rgba((150, 255, 170), 45)
        return sp
    w.bone("root", pivot=(0, 0, 0))
    w.bone("wings", "root", pivot=(0, 15, 2))
    for sx, tag in ((1, "l"), (-1, "r")):
        up, lo = "wing_u" + tag, "wing_d" + tag
        w.bone(up, "wings", pivot=(sx * 1.2, 16, 2.2))
        w.bone(lo, "wings", pivot=(sx * 1.2, 13, 2.4))
        mirror = sx < 0
        x0u = 1.2 if sx > 0 else -17.2
        x0l = 1.2 if sx > 0 else -12.2
        sw.plate(w, up, (x0u + (0 if sx > 0 else 4), 15, 2), (12, 17), wing_shape(12, 17, 1.0, mirror), rotation=(0, sx * -18, 0), pivot=(sx * 1.2, 16, 2.2))
        sw.plate(w, lo, (x0l + (0 if sx > 0 else 3), 6, 2.4), (9, 11), wing_shape(9, 11, 2.0, mirror), rotation=(0, sx * -26, 0), pivot=(sx * 1.2, 13, 2.4))
    for n in range(6):
        w.bone("bub%d" % n, "root", pivot=(0, 0, 0))
        size = 2.0 + (n % 3) * 0.9

        def bubble(face, i, j, fw, fh, n=n):
            cx, cy = (fw - 1) / 2, (fh - 1) / 2
            r = math.hypot(i - cx, j - cy) / max(1.0, fw / 2)
            if face == "front" or face == "back":
                if r > 1.05:
                    return None, None
                if i <= 1 and j <= 1:
                    return rgba((255, 255, 255), 240), rgba((255, 255, 255), 200)
                return rgba(mix((150, 240, 130), (210, 255, 190), 1 - r), 90 + 120 * r ** 3), rgba((140, 255, 130), 60 + 80 * r ** 2)
            return rgba((170, 245, 150), 150), rgba((140, 255, 130), 70)
        sw.box(w, "bub%d" % n, (9 + (n % 3) * 2.5, 3 + n * 3.4, -size / 2), (size, size, size), bubble)
    B.clip("idle", 2.4, True)
    B.pos("idle", "root", {0.0: (0, 0, 0), 0.6: (0, 1.6, 0), 1.2: (0, 0, 0), 1.8: (0, 1.6, 0), 2.4: (0, 0, 0)})
    for sx, tag in ((1, "l"), (-1, "r")):
        B.rot("idle", "wing_u" + tag, {0.0: (0, 0, 0), 0.075: (0, sx * 22, 0), 0.15: (0, 0, 0), 2.4: (0, 0, 0)}) if False else None
    def flap(clip, period, amp, length):
        n = max(1, int(round(length / period)))
        for sx, tag in ((1, "l"), (-1, "r")):
            for nm, k in (("wing_u", 1.0), ("wing_d", 0.8)):
                frames = {}
                for q in range(n * 4 + 1):
                    t = q * period / 4
                    frames[min(t, length)] = (0, sx * amp * k * S(TAU * t / period), 0)
                B.rot(clip, nm + tag, frames)
    flap("idle", 0.3, 24, 2.4)
    for n in range(6):
        B.rot("idle", "bub%d" % n, {0.0: (0, n * 60, 0), 1.2: (0, n * 60 + 180, 0), 2.4: (0, n * 60 + 360, 0)})
    B.clip("move", 0.8, True)
    B.pos("move", "root", {0.0: (0, 0, 0), 0.2: (0, 1.8, 0), 0.4: (0, 0, 0), 0.6: (0, 1.8, 0), 0.8: (0, 0, 0)})
    flap("move", 0.16, 30, 0.8)
    for n in range(6):
        B.rot("move", "bub%d" % n, {0.0: (0, n * 60, 0), 0.4: (0, n * 60 + 180, 0), 0.8: (0, n * 60 + 360, 0)})
    B.clip("cast", 0.8, False)
    flap("cast", 0.2, 34, 0.8)
    for n in range(6):
        B.rot("cast", "bub%d" % n, {0.0: (0, n * 60, 0), 0.8: (0, n * 60 + 720, 0)})
    B.clip("hit", 0.35, False)
    B.rot("hit", "wings", {0.0: (0, 0, 0), 0.12: (-12, 0, 0), 0.35: (0, 0, 0)})
    img, glow = sw.images()
    w.save(B)
    w.save_texture(img, glow)
    print(w.id, "cubes", w.cube_count())


# =====================================================================================================================
# GNOME
# =====================================================================================================================
def gnome():
    STONE, STONE_D, STONE_L = (176, 150, 112), (70, 54, 42), (208, 184, 146)
    DARK = (28, 20, 18)
    m = Model("spirit", "gnome", 256, 256)
    sh = Sheet(256, 256)
    A = Anim()

    def rock(salt, size=5.0, tint=1.0):
        def p(face, i, j, fw, fh):
            d, cid = voronoi(i + 0.5 + salt * 31, j + 0.5 + (11 if face in ("top", "bottom") else 0), size, salt)
            c = np.array(STONE, np.float32) * (0.78 + 0.4 * cid) * tint
            if face == "top":
                c = c * 1.12
            elif face == "bottom":
                c = c * 0.7
            if d < 0.32:
                c = np.array(STONE_D, np.float32)
                return rgba(c), rgba((255, 150, 40), 55) if hsh(i, j, salt) > 0.93 else None
            if d < 0.9:
                c = c * 0.88
            elif d > 2.2:
                c = c * 1.1 + 8
            c = c + fz(i, j, salt + 2) * 8 + nz(i, j, salt) * 12
            return rgba(c), None
        return p

    m.bone("root", pivot=(0, 0, 0))
    m.bone("hips", "root", pivot=(0, 21, 0))
    sh.box(m, "hips", (-7, 16, -4.5), (14, 8, 9), rock(1))
    m.bone("torso", "hips", pivot=(0, 24, 0), rotation=(-10, 0, 0))
    sh.box(m, "torso", (-11, 23, -7), (22, 16, 13), rock(2, 6))
    sh.box(m, "torso", (-8.5, 32, 0), (17, 9, 8), rock(3, 5), rotation=(0, 0, 4), pivot=(0, 36, 3))           # the hump on his back
    sh.box(m, "torso", (-6, 22, -8.5), (12, 6, 3), rock(4, 3.2))                                              # belly plates
    for k, (x, y, z, sz) in enumerate(((-5, 40, -2, 5), (3, 41, 1, 4), (-1, 38, 4, 5))):
        sh.box(m, "torso", (x, y, z), (sz, sz, sz), rock(5 + k, 3), rotation=(15 * k, 20 * k, 10), pivot=(x + sz / 2, y, z + sz / 2))
    m.bone("head", "torso", pivot=(0, 38, -5))

    def head_paint(face, i, j, fw, fh):
        if face == "front":
            if 2 <= j <= 3 and (1 <= i <= 2 or fw - 3 <= i <= fw - 2):
                return rgba((255, 224, 70)), rgba((255, 210, 40))                                           # glowing eyes
            if 5 <= j <= fh - 2 and 2 <= i <= fw - 3:
                return rgba((10, 6, 6)), rgba((255, 120, 30), 60 if j % 2 else 25)                          # the open mouth
            return rgba(np.array(DARK, np.float32) + fz(i, j, 4) * 10), None
        if face == "top":
            return rock(9, 3)(face, i, j, fw, fh)
        return rgba(np.array(DARK, np.float32) + 8 + fz(i, j, 6) * 12), None
    sh.box(m, "head", (-4.5, 36, -10), (9, 8.5, 9), head_paint)
    sh.box(m, "head", (-5.5, 43, -11), (11, 2.5, 4), rock(10, 3))                                            # brow ridge
    sh.box(m, "head", (-5, 41, -3), (10, 4, 6), rock(11, 3))                                                 # skull plate
    for sx, name in ((1, "arm_l"), (-1, "arm_r")):
        m.bone(name, "torso", pivot=(sx * 13, 38, -2))
        x0 = 9.5 if sx > 0 else -17.5
        sh.box(m, name, (x0 - 1, 37, -7), (10, 8, 10), rock(12 + sx, 4), rotation=(0, 0, sx * 8), pivot=(sx * 13, 39, -2))    # the shoulder boulder
        sh.box(m, name, (x0 + 0.5, 22, -5.5), (7, 16, 7), rock(14 + sx, 4))
        m.bone(name + "2", name, pivot=(sx * 13, 23, -2))
        sh.box(m, name + "2", (x0, 8, -6), (8, 15, 8), rock(16 + sx, 4))
        sh.box(m, name + "2", (x0 - 1.5, 0.5, -7.5), (11, 9, 11), rock(18 + sx, 3.5))                       # the fist
    for sx, name in ((1, "leg_l"), (-1, "leg_r")):
        m.bone(name, "hips", pivot=(sx * 4.5, 18, 0))
        x0 = 1 if sx > 0 else -8
        sh.box(m, name, (x0, 9, -3.5), (7, 10, 7), rock(20 + sx, 4))
        sh.box(m, name, (x0 + 0.5, 3, -3), (6, 7, 6), rock(22 + sx, 4))
        sh.box(m, name, (x0 - 0.8, 0, -6), (8.6, 3.2, 10), rock(24 + sx, 3))
    A.clip("idle", 3.6, True)
    sway(A, "torso", "idle", (-10, 0, 0), 2.5, 3.6, axis=0)
    sway(A, "head", "idle", (0, 0, 0), 4, 3.6, 0.8, axis=1)
    sway(A, "arm_l", "idle", (0, 0, 4), 2, 3.6, 0.4, axis=2)
    sway(A, "arm_r", "idle", (0, 0, -4), -2, 3.6, 0.4, axis=2)
    A.clip("move", 1.2, True)
    sway(A, "torso", "move", (-14, 0, 0), 3, 1.2, axis=2) if False else None
    sway(A, "hips", "move", (0, 0, 0), 5, 1.2, axis=2)
    sway(A, "torso", "move", (-12, 0, 0), 4, 1.2, 1.5, axis=1)
    sway(A, "leg_l", "move", (0, 0, 0), 24, 1.2, axis=0)
    sway(A, "leg_r", "move", (0, 0, 0), 24, 1.2, math.pi, axis=0)
    sway(A, "arm_l", "move", (0, 0, 4), 22, 1.2, math.pi, axis=0)
    sway(A, "arm_r", "move", (0, 0, -4), 22, 1.2, 0, axis=0)
    A.pos("move", "hips", {0.0: (0, 0, 0), 0.3: (0, -1.2, 0), 0.6: (0, 0, 0), 0.9: (0, -1.2, 0), 1.2: (0, 0, 0)})
    A.clip("cast", 1.0, False)
    A.rot("cast", "arm_l", {0.0: (0, 0, 4), 0.35: (-150, 0, 10), 0.55: (-150, 0, 10), 0.7: (20, 0, 4), 1.0: (0, 0, 4)})
    A.rot("cast", "arm_r", {0.0: (0, 0, -4), 0.35: (-150, 0, -10), 0.55: (-150, 0, -10), 0.7: (20, 0, -4), 1.0: (0, 0, -4)})
    A.rot("cast", "torso", {0.0: (-10, 0, 0), 0.35: (6, 0, 0), 0.7: (-26, 0, 0), 1.0: (-10, 0, 0)})
    A.clip("hit", 0.35, False)
    A.rot("hit", "torso", {0.0: (-10, 0, 0), 0.12: (0, 0, 0), 0.35: (-10, 0, 0)})
    A.rot("hit", "head", {0.0: (0, 0, 0), 0.12: (12, 0, 0), 0.35: (0, 0, 0)})
    finish(m, sh, A)


KINDS = {"salamander": salamander, "undine": undine, "sylph": sylph, "gnome": gnome}

if __name__ == "__main__":
    want = sys.argv[1:] or list(KINDS)
    for k in want:
        KINDS[k]()
