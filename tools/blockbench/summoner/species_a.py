"""Summon models, part 1 (second pass, from the owner's reference pack): Hayate, Cerorin, Rocanel, Mira.

Hayate: a white, lion-maned wolf with a diamond gem on the forehead, black ear backs and black "socks" with claws. Cerorin: a black three-headed hound, each head snarling, with a spiked mohawk.
Rocanel: a horned stone knight with a studded ring shield; Mira: the big cracked stone knight with a great slab sword. A script fallback (rule 15), not made in an app.
"""
import math

from summon_lib import C, Cx, Clips, bone, pair, reset, decal, box, line, np, rgb


# ---------------------------------------------------------------- decals
@decal("wolf_face")
def _wolf_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    eye = pal.get("c", {}).get("eye", "#e8a020")
    for lo, hi, mir in ((0.12, 0.40, False), (0.60, 0.88, True)):
        box(img, fw, fh, lo, 0.42, hi, 0.54, "#17141a")
        a, b = (0.20, 0.34) if not mir else (0.66, 0.80)
        box(img, fw, fh, a, 0.44, b, 0.52, eye, g, 255)
        box(img, fw, fh, a + 0.04, 0.45, a + 0.07, 0.49, "#101010")
    line(img, fw, fh, 0.10, 0.28, 0.40, 0.40, "#17141a", 2)
    line(img, fw, fh, 0.90, 0.28, 0.60, 0.40, "#17141a", 2)
    box(img, fw, fh, 0.44, 0.12, 0.56, 0.30, "#d7c9a8", blend=0.5)


@decal("wolf_muzzle")
def _wolf_muzzle(img, g, face, fw, fh, pal):
    if face == "north":
        line(img, fw, fh, 0.50, 0.55, 0.50, 0.72, "#2a2226", 1)
        line(img, fw, fh, 0.50, 0.72, 0.18, 0.80, "#2a2226", 1)
        line(img, fw, fh, 0.50, 0.72, 0.82, 0.80, "#2a2226", 1)


@decal("hound_face")
def _hound_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    eye = pal.get("c", {}).get("eye", "#ff3030")
    for lo, hi in ((0.10, 0.42), (0.58, 0.90)):
        box(img, fw, fh, lo, 0.38, hi, 0.55, "#0c0b0e")
        box(img, fw, fh, lo + 0.05, 0.42, hi - 0.05, 0.52, eye, g, 255)
    line(img, fw, fh, 0.06, 0.22, 0.42, 0.42, "#0c0b0e", 2)
    line(img, fw, fh, 0.94, 0.22, 0.58, 0.42, "#0c0b0e", 2)
    for u in (0.46, 0.50, 0.54):
        line(img, fw, fh, u, 0.10, u, 0.28, "#3a3a46", 1)


@decal("teeth")
def _teeth(img, g, face, fw, fh, pal):
    if face in ("north", "up"):
        n = max(3, fw // 3)
        for i in range(n):
            u = (i + 0.5) / n
            box(img, fw, fh, u - 0.05, 0.0, u + 0.05, 0.55, "#ecebe6")
    box(img, fw, fh, 0.0, 0.0, 1.0, 1.0, "#5a1418", blend=0.0)


@decal("jaw_teeth")
def _jaw_teeth(img, g, face, fw, fh, pal):
    if face == "up":
        box(img, fw, fh, 0.05, 0.05, 0.95, 0.95, "#5a1418")
        n = max(3, fw // 3)
        for i in range(n):
            u = (i + 0.5) / n
            box(img, fw, fh, u - 0.06, 0.05, u + 0.06, 0.45, "#ecebe6")
    elif face == "north":
        n = max(3, fw // 3)
        for i in range(n):
            u = (i + 0.5) / n
            box(img, fw, fh, u - 0.06, 0.0, u + 0.06, 0.4, "#ecebe6")


@decal("helm_rocanel")
def _helm_rocanel(img, g, face, fw, fh, pal):
    if face == "north":
        box(img, fw, fh, 0.22, 0.38, 0.38, 0.52, "#0a0a0c")
        box(img, fw, fh, 0.62, 0.38, 0.78, 0.52, "#0a0a0c")
        box(img, fw, fh, 0.27, 0.42, 0.33, 0.48, "#9fd8ff", g, 255)
        box(img, fw, fh, 0.67, 0.42, 0.73, 0.48, "#9fd8ff", g, 255)
        line(img, fw, fh, 0.50, 0.15, 0.50, 0.92, "#3a3836", 1)
        for v in (0.64, 0.72, 0.80):
            line(img, fw, fh, 0.30, v, 0.70, v, "#3a3836", 1)


@decal("helm_mira")
def _helm_mira(img, g, face, fw, fh, pal):
    if face == "north":
        box(img, fw, fh, 0.14, 0.34, 0.86, 0.62, "#0a0a0c")
        for u in (0.26, 0.38, 0.50, 0.62, 0.74):
            box(img, fw, fh, u - 0.02, 0.34, u + 0.02, 0.62, "#4a4846")
        box(img, fw, fh, 0.20, 0.44, 0.40, 0.52, "#bfe6ff", g, 255)
        box(img, fw, fh, 0.60, 0.44, 0.80, 0.52, "#bfe6ff", g, 255)
        for v in (0.72, 0.82):
            line(img, fw, fh, 0.18, v, 0.82, v, "#3a3836", 1)


@decal("chest_plate")
def _chest_plate(img, g, face, fw, fh, pal):
    if face == "north":
        line(img, fw, fh, 0.50, 0.04, 0.50, 0.96, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.25, 0.08, 0.10, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.25, 0.92, 0.10, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.55, 0.10, 0.40, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.55, 0.90, 0.40, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.85, 0.14, 0.70, "#2c2a2a", 1)
        line(img, fw, fh, 0.50, 0.85, 0.86, 0.70, "#2c2a2a", 1)


@decal("ring_shield")
def _ring_shield(img, g, face, fw, fh, pal):
    if face != "north":
        return
    cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
    rad = min(fw, fh) / 2.0
    dark, light = rgb("#2e2c2c"), rgb("#bfbab0")
    for y in range(fh):
        for x in range(fw):
            r = math.hypot(x - cx, y - cy) / rad
            a = math.atan2(y - cy, x - cx)
            if r > 1.0:
                continue
            if 0.9 < r <= 1.0 or 0.62 < r <= 0.68 or 0.30 < r <= 0.36:
                img[y, x] = dark
            elif r <= 0.18:
                img[y, x] = light
            elif 0.72 < r <= 0.86 and abs(((a / (2 * math.pi)) * 10) % 1.0 - 0.5) < 0.2:
                img[y, x] = light                      # the studs between the rims
            elif 0.40 < r <= 0.56 and abs(((a / (2 * math.pi)) * 6 + 0.5) % 1.0 - 0.5) < 0.16:
                img[y, x] = dark


# ---------------------------------------------------------------- shared quadruped parts
def quad_clips(legs, necks=(), tails=(), jaws=(), extra=None, bob=0.8):
    """legs: (front pair names, back pair names). Everything else is optional lists of bone names."""
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.45 * s(t), 0))
    for i, n in enumerate(necks):
        c.rot("idle", n, lambda t, i=i: (2.5 * s(t, 0.6 + i), 4 * s(t, i * 1.7), 0))
    for i, n in enumerate(tails):
        c.rot("idle", n, lambda t, i=i: (0, 9 * (i + 1) * s(t, -i * 0.8), 0))
    for n in jaws:
        c.rot("idle", n, lambda t: (3 + 3 * max(0, s(t, 0.5)), 0, 0))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, bob * abs(w(t)), 0))
    c.rot("walk", "body", lambda t: (0, 2.5 * w(t), 0))
    f, b = legs
    for n, ph in ((f[0], 0.0), (f[1], math.pi), (b[0], math.pi), (b[1], 0.0)):
        c.rot("walk", n, lambda t, ph=ph: (30 * w(t, ph), 0, 0))
    for i, n in enumerate(tails):
        c.rot("walk", n, lambda t, i=i: (-6, 12 * (i + 1) * w(t, 0.8 - i * 0.6), 0))
    for i, n in enumerate(necks):
        c.rot("walk", n, lambda t, i=i: (3 * w(t, 1.0), 0, 0))
    a = [(0, (0, 0, 0)), (0.15, (-14, 0, 0)), (0.32, (22, 0, 0)), (0.6, (0, 0, 0))]
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-8, 0, 0)), (0.32, (10, 0, 0)), (0.6, (0, 0, 0))])
    for n in necks:
        c.rot("attack", n, rows=a)
    for n in jaws:
        c.rot("attack", n, rows=[(0, (0, 0, 0)), (0.15, (38, 0, 0)), (0.32, (4, 0, 0)), (0.6, (0, 0, 0))])
    for n in f:
        c.rot("attack", n, rows=[(0, (0, 0, 0)), (0.15, (-40, 0, 0)), (0.32, (30, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.6, (-16, 0, 0)), (1.2, (-16, 0, 0)), (1.6, (0, 0, 0))])
    for n in necks:
        c.rot("ability", n, rows=[(0, (0, 0, 0)), (0.6, (-34, 0, 0)), (1.2, (-34, 0, 0)), (1.6, (0, 0, 0))])
    for n in jaws:
        c.rot("ability", n, rows=[(0, (0, 0, 0)), (0.6, (40, 0, 0)), (1.2, (40, 0, 0)), (1.6, (0, 0, 0))])
    if extra:
        extra(c)
    return c


def paw_set(prefix, sd, n, x, z, high, thick, mat_up, mat_lo, claws=3, claw_len=1.6, hind=False):
    """One leg: bone, upper leg, lower leg, paw and claws. x is the leg's distance from the middle line, z its front-back place."""
    bone("%s_%s" % (prefix, n), "body", (-x, high, z), side=sd)
    up = high * 0.52
    Cx("%s_%s" % (prefix, n), "up_%s_%s" % (prefix, n), -x - thick / 2.0 - (0.5 if hind else 0.2), high - up, z - thick / 2.0 - (0.4 if hind else 0), thick + (1.0 if hind else 0.4), up, thick + (1.2 if hind else 0.4), mat_up, side=sd)
    lo = high - up
    Cx("%s_%s" % (prefix, n), "lo_%s_%s" % (prefix, n), -x - thick / 2.0 + 0.3, 1.6, z - thick / 2.0 + (0.5 if hind else 0.1), thick - 0.6, lo - 1.4, thick - 0.6, mat_lo, side=sd)
    Cx("%s_%s" % (prefix, n), "paw_%s_%s" % (prefix, n), -x - thick / 2.0 - 0.3, 0, z - thick / 2.0 - 1.6, thick + 0.6, 1.9, thick + 2.2, mat_lo, side=sd)
    step = (thick + 0.6 - 0.9) / max(1, claws - 1)
    for i in range(claws):
        Cx("%s_%s" % (prefix, n), "claw%d_%s_%s" % (i, prefix, n), -x - thick / 2.0 - 0.3 + i * step, 0, z - thick / 2.0 - 1.6 - claw_len, 0.9, 1.1, claw_len + 0.4, "claw", side=sd)


# ---------------------------------------------------------------- Hayate
def build_hayate():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 14, 0))
    C("body", "torso", 0, 11, -8.5, 10.4, 9.6, 17.5, "fur")
    C("body", "belly", 0, 10.6, -6.5, 8.6, 1.4, 12, "fur2")
    C("body", "hips", 0, 11.2, 5.2, 10.8, 9.2, 8.8, "fur")
    # the lion mane: a heavy chest ruff, layered tufts down the back
    C("body", "ruff_a", 0, 7.6, -14.2, 13, 12.4, 7.4, "fur2")
    C("body", "ruff_b", 0, 5.4, -13.4, 9.8, 5.8, 6.4, "fur")
    C("body", "ruff_tuft_r", -4.2, 4.2, -13.8, 3.6, 5, 4, "fur2", rot=(0, 0, 14))
    C("body", "ruff_tuft_l", 4.2, 4.2, -13.8, 3.6, 5, 4, "fur2", rot=(0, 0, -14))
    C("body", "ruff_tuft_m", 0, 3.4, -14.2, 3.4, 5, 4, "fur", rot=(0, 0, 0))
    C("body", "shoulder_r", -6.2, 11.8, -11.2, 3.4, 8.8, 7, "fur2", rot=(0, 0, 8))
    C("body", "shoulder_l", 6.2, 11.8, -11.2, 3.4, 8.8, 7, "fur2", rot=(0, 0, -8))
    for i, (z, h, w) in enumerate(((-10, 4.0, 9.2), (-4.5, 3.2, 8.2), (1.5, 2.6, 7.2))):
        C("body", "mane_%d" % i, 0, 19.6, z, w, h, 6.6, "fur2", rot=(-6 - i * 3, 0, 0))
    bone("head", "body", (0, 19, -12))
    C("head", "neck", 0, 13.6, -15.4, 9.0, 10.4, 6.4, "fur")
    C("head", "skull", 0, 15.4, -21.6, 10.4, 9.8, 9.2, "fur", paint="wolf_face")
    C("head", "muzzle", 0, 15.2, -29.2, 5.8, 5.2, 8.2, "fur", paint="wolf_muzzle")
    C("head", "nose", 0, 19.0, -30.0, 3.2, 2.2, 1.4, "dark")
    for sx in (-1, 1):
        C("head", "ear_%d" % sx, sx * 4.2, 23.6, -16.6, 3.4, 6.8, 2.2, "dark", rot=(8, 0, -sx * 14))
        C("head", "ear_tuft_%d" % sx, sx * 4.0, 23.2, -18.2, 2.6, 4.6, 1.4, "fur", rot=(8, 0, -sx * 14))
        C("head", "cheek_%d" % sx, sx * 6.8, 12.8, -23, 4.2, 7.6, 6.4, "fur2", rot=(0, sx * -14, sx * 16))
        C("head", "cheek_tuft_%d" % sx, sx * 6.0, 10.6, -19.8, 3.4, 5.4, 4.2, "fur", rot=(0, 0, sx * 22))
        C("head", "fang_u_%d" % sx, sx * 1.9, 14.2, -28.8, 0.9, 1.8, 0.9, "claw")
        C("head", "brow_%d" % sx, sx * 3.4, 22.2, -22.0, 3.6, 1.2, 0.9, "dark", rot=(0, 0, sx * -18))
    C("head", "gem", 0, 21.4, -22.2, 2.8, 2.8, 1.0, "gem", rot=(0, 0, 45))
    C("head", "gem_ring", 0, 21.0, -22.0, 4.0, 4.0, 0.5, "fur2", rot=(0, 0, 45))
    # the mane: tufts radiating around the head like a lion's, on the head so they move with it
    for i in range(11):
        a = math.radians(i * 360.0 / 11)
        cx, cy = 7.4 * math.cos(a), 20.6 + 6.4 * math.sin(a)
        C("head", "mane_t%d" % i, cx, cy - 2.6, -15.4 - (i % 2) * 1.6, 4.4 + (i % 2), 5.4 + (i % 3) * 0.8, 3.6, "fur2" if i % 2 else "fur", rot=(0, 0, math.degrees(a) - 90))
    bone("jaw", "head", (0, 16.8, -22))
    C("jaw", "jaw", 0, 13.6, -29.0, 5.2, 2.2, 7.2, "fur")
    for sx in (-1, 1):
        C("jaw", "fang_l_%d" % sx, sx * 1.8, 15.6, -28.6, 0.9, 1.7, 0.9, "claw")
    pair(lambda sd, n: paw_set("lf", sd, n, 3.7, -6.8, 12.4, 5.0, "fur", "dark", claws=3, claw_len=0.9))
    pair(lambda sd, n: paw_set("lb", sd, n, 3.9, 7.0, 12.0, 5.2, "fur", "fur", claws=3, claw_len=0.9, hind=True))
    bone("tail", "body", (0, 19, 11))
    C("tail", "tail_a", 0, 17.4, 10.6, 4.6, 4.6, 6.4, "fur", rot=(-28, 0, 0))
    C("tail", "tail_b", 0, 19.6, 15.2, 6.2, 6.2, 7.4, "fur2", rot=(-40, 0, 0))
    bone("tail2", "tail", (0, 22, 19))
    C("tail2", "tail_c", 0, 22.2, 19.4, 5.0, 5.0, 6.4, "fur", rot=(-52, 0, 0))
    pal = dict(m=dict(fur=("#f3f1ec", "fur"), fur2=("#e2dfd8", "fur"), dark=("#1b181e", "fur"), claw=("#d9d4c6", "plate"), gem=("#cfeeff", "glow")), eye="#e8a020", glow="#bfefff", c=dict(eye="#e8a020"))
    c = quad_clips((("lf_r", "lf_l"), ("lb_r", "lb_l")), necks=("head",), tails=("tail", "tail2"), jaws=("jaw",))
    return pal, c, 31.0, 3.0


# ---------------------------------------------------------------- Cerorin
def hound_head(tag, cx, cy, cz, yaw, roll, scale=1.0):
    """One snarling head on its own bone: skull, snout, open jaw with teeth, ears, a spiked mohawk."""
    bone("head_" + tag, "body", (cx, cy, cz))
    h = "head_" + tag
    k = scale
    C(h, "skull_" + tag, cx, cy, cz - 8.2 * k, 8.6 * k, 8.0 * k, 8.0 * k, "fur", paint="hound_face", rot=(0, yaw, roll), pv=(cx, cy, cz))
    C(h, "snout_" + tag, cx, cy - 0.4, cz - 14.4 * k, 5.2 * k, 4.4 * k, 6.6 * k, "fur", rot=(0, yaw, roll), pv=(cx, cy, cz))
    C(h, "nose_" + tag, cx, cy + 3.0, cz - 15.2 * k, 3.0 * k, 2.0 * k, 1.4, "dark", rot=(0, yaw, roll), pv=(cx, cy, cz))
    C(h, "uteeth_" + tag, cx, cy - 1.8, cz - 14.0 * k, 4.8 * k, 1.8, 5.8 * k, "teeth_u", paint="jaw_teeth", rot=(0, yaw, roll), pv=(cx, cy, cz))
    bone("jaw_" + tag, h, (cx, cy + 0.6, cz - 7))
    C("jaw_" + tag, "jaw_" + tag, cx, cy - 3.8, cz - 14.0 * k, 4.8 * k, 1.8, 6.4 * k, "fur", paint="jaw_teeth", rot=(0, yaw, roll), pv=(cx, cy, cz))
    for sx in (-1, 1):
        C(h, "ear_%s_%d" % (tag, sx), cx + sx * 3.4 * k, cy + 7.2 * k, cz - 5.4 * k, 2.6 * k, 4.8 * k, 1.8, "dark", rot=(8, yaw, roll - sx * 10), pv=(cx, cy, cz))
    for i in range(5):
        C(h, "mohawk_%s_%d" % (tag, i), cx, cy + 7.0 * k, cz - 7.0 * k + i * 1.9, 1.8, (5.6 - 0.5 * i) * k, 1.6, "spike", rot=(-22 - 6 * i, yaw, roll), pv=(cx, cy, cz))


def build_cerorin():
    reset()
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 15, 0))
    C("body", "torso", 0, 11, -9, 13, 12, 19, "fur")
    C("body", "hips", 0, 11.2, 6, 12.4, 11, 9.6, "fur")
    C("body", "chest", 0, 9.4, -12.4, 14.6, 13.6, 7.2, "fur2")
    C("body", "chest_tuft_r", -5.2, 6.6, -12.2, 4, 5.4, 4.4, "fur", rot=(0, 0, 12))
    C("body", "chest_tuft_l", 5.2, 6.6, -12.2, 4, 5.4, 4.4, "fur", rot=(0, 0, -12))
    for i in range(6):
        C("body", "back_spike_%d" % i, 0, 22.0, -9 + i * 4.6, 2.2, 5.4 - 0.5 * i, 2.2, "spike", rot=(-18, 0, 0))
    C("body", "mane_r", -5.6, 20.0, -12, 3.2, 5.2, 8, "fur2", rot=(0, 0, 14))
    C("body", "mane_l", 5.6, 20.0, -12, 3.2, 5.2, 8, "fur2", rot=(0, 0, -14))
    # three necks fanning out of the shoulders
    C("body", "neck_c", 0, 19.0, -13.2, 6.4, 8.2, 6.0, "fur", rot=(-18, 0, 0))
    C("body", "neck_r", -6.6, 18.2, -12.4, 5.6, 7.6, 5.6, "fur", rot=(-14, 0, 22))
    C("body", "neck_l", 6.6, 18.2, -12.4, 5.6, 7.6, 5.6, "fur", rot=(-14, 0, -22))
    hound_head("c", 0, 25.6, -16, 0, 0, 1.12)
    hound_head("r", -10.6, 22.6, -14.4, 18, 12, 1.0)
    hound_head("l", 10.6, 22.6, -14.4, -18, -12, 1.0)
    pair(lambda sd, n: paw_set("lf", sd, n, 4.6, -7.6, 13.4, 6.0, "fur", "fur2", claws=3, claw_len=2.4))
    pair(lambda sd, n: paw_set("lb", sd, n, 4.8, 8.4, 13.0, 6.2, "fur", "fur2", claws=3, claw_len=2.0, hind=True))
    bone("tail", "body", (0, 19, 14.5))
    C("tail", "tail_a", 0, 17.6, 14.2, 3.6, 3.6, 8, "fur", rot=(-34, 0, 0))
    bone("tail2", "tail", (0, 22, 20))
    C("tail2", "tail_b", 0, 22.0, 19.8, 3.0, 3.0, 7, "fur2", rot=(-50, 0, 0))
    pal = dict(m=dict(fur=("#2b2b33", "fur"), fur2=("#3d3d49", "fur"), dark=("#0d0c10", "fur"), spike=("#15141a", "plate"), claw=("#e4e0d4", "plate"), teeth_u=("#5a1418", "flat")),
               eye="#ff3030", glow="#ff4040", c=dict(eye="#ff3030"))
    c = quad_clips((("lf_r", "lf_l"), ("lb_r", "lb_l")), necks=("head_c", "head_r", "head_l"), tails=("tail", "tail2"), jaws=("jaw_c", "jaw_r", "jaw_l"), bob=0.9)
    return pal, c, 36.0, 3.4


# ---------------------------------------------------------------- the stone knights
def knight_clips(shield=True):
    c = Clips()
    P = 3.6
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.4 * s(t), 0))
    c.rot("idle", "head", lambda t: (1.5 * s(t, 1), 3 * s(t), 0))
    c.rot("idle", "arm_r", lambda t: (2 * s(t, 0.4), 0, -3))
    c.rot("idle", "arm_l", lambda t: (2 * s(t, 2.0), 0, 3))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, 0.7 * abs(w(t)), 0))
    c.rot("walk", "body", lambda t: (3, 2 * w(t), 0))
    c.rot("walk", "leg_r", lambda t: (26 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (26 * w(t, math.pi), 0, 0))
    c.rot("walk", "arm_r", lambda t: (-18 * w(t), 0, -3))
    c.rot("walk", "arm_l", lambda t: (-18 * w(t, math.pi), 0, 3))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.18, (-6, -16, 0)), (0.34, (12, 18, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, -3)), (0.18, (-120, 0, -10)), (0.34, (-20, 0, 0)), (0.6, (0, 0, -3))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 3)), (0.18, (-30, 0, 8)), (0.34, (-70, 0, 0)), (0.6, (0, 0, 3))])
    c.rot("ability", "arm_r", rows=[(0, (0, 0, -3)), (0.5, (-80, 0, -50)), (1.2, (-80, 0, -50)), (1.6, (0, 0, -3))])
    c.rot("ability", "arm_l", rows=[(0, (0, 0, 3)), (0.5, (-80, 0, 50)), (1.2, (-80, 0, 50)), (1.6, (0, 0, 3))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-14, 0, 0)), (1.2, (-14, 0, 0)), (1.6, (0, 0, 0))])
    return c


def knight(sc, helm, big=False, sword=False):
    """A stone knight. sc scales the height (1.0 = 29 px). helm: 'rocanel' (two horns) or 'mira' (a barred bucket helm)."""
    u = lambda v: v * sc
    bone("root", None, (0, 0, 0))
    hip = u(13.0)
    bone("body", "root", (0, hip, 0))
    # legs: boots with spikes, shin armour, knee guards, thigh plates
    def legs(sd, n):
        x = u(3.0)
        bone("leg_" + n, "body", (-x, hip, 0), side=sd)
        Cx("leg_" + n, "thigh_" + n, -x - u(2.7), u(7.0), -u(2.7), u(5.4), u(6.2), u(5.4), "stone", side=sd)
        Cx("leg_" + n, "thigh_plate_" + n, -x - u(3.0), u(7.4), -u(3.2), u(6.0), u(4.6), u(1.2), "armor", side=sd)
        Cx("leg_" + n, "knee_" + n, -x - u(2.9), u(6.2), -u(3.4), u(5.8), u(2.2), u(2.2), "armor", side=sd)
        Cx("leg_" + n, "shin_" + n, -x - u(2.4), u(1.8), -u(2.4), u(4.8), u(4.6), u(4.8), "suit", side=sd)
        Cx("leg_" + n, "shin_plate_" + n, -x - u(2.7), u(1.8), -u(3.0), u(5.4), u(4.4), u(1.4), "armor", rot=(8, 0, 0), side=sd)
        Cx("leg_" + n, "boot_" + n, -x - u(2.9), 0, -u(4.0), u(5.8), u(2.4), u(7.4), "armor", side=sd)
        Cx("leg_" + n, "boot_spike_" + n, -x - u(0.7), u(0.4), -u(6.2), u(1.4), u(1.4), u(2.4), "armor", side=sd)
    pair(legs)
    C("body", "waist", 0, hip - u(1.0), -u(3.2), u(8.4), u(3.4), u(6.4), "suit")
    C("body", "belt", 0, hip + u(1.2), -u(3.8), u(9.6), u(2.0), u(7.4), "armor")
    C("body", "belt_v", 0, hip - u(1.4), -u(4.2), u(3.8), u(3.8), u(1.0), "armor", rot=(0, 0, 45))
    for sx in (-1, 1):
        C("body", "tasset_%d" % sx, sx * u(3.8), hip - u(3.4), -u(3.8), u(2.6), u(4.6), u(1.2), "armor", rot=(0, 0, sx * -12))
    bone("chest", "body", (0, hip + u(3), 0))
    C("chest", "torso", 0, hip + u(3), -u(3.0), u(9.4), u(8.6), u(6.0), "suit")
    C("chest", "plate", 0, hip + u(3.4), -u(3.9), u(10.6), u(8.4), u(1.8), "armor", paint="chest_plate")
    C("chest", "plate_back", 0, hip + u(3.4), u(2.1), u(10.2), u(8.4), u(1.6), "armor")
    C("chest", "collar", 0, hip + u(11.2), -u(3.2), u(8.4), u(1.8), u(6.6), "armor")
    for sx in (-1, 1):
        C("chest", "pauldron_%d" % sx, sx * u(6.6), hip + u(9.6), -u(3.4), u(4.8), u(3.6), u(6.8), "armor", rot=(0, 0, sx * -14))
        C("chest", "pauldron2_%d" % sx, sx * u(7.4), hip + u(8.2), -u(3.0), u(3.6), u(3.0), u(6.0), "armor", rot=(0, 0, sx * -22))
    bone("head", "chest", (0, hip + u(11.6), 0))
    C("head", "helm", 0, hip + u(11.6), -u(4.2), u(8.0), u(8.2), u(8.2), "armor", paint="helm_" + helm)
    C("head", "helm_crest", 0, hip + u(19.6), -u(1.2), u(2.2), u(2.2), u(5.2), "armor")
    if helm == "rocanel":
        for sx in (-1, 1):
            C("head", "horn_%d" % sx, sx * u(4.2), hip + u(17.6), -u(1.4), u(2.2), u(4.6), u(2.0), "armor", rot=(0, 0, sx * -26))
            C("head", "horn_tip_%d" % sx, sx * u(6.4), hip + u(20.6), -u(1.2), u(1.4), u(3.8), u(1.4), "armor", rot=(0, 0, sx * -44))
    else:
        C("head", "helm_top", 0, hip + u(19.4), -u(3.6), u(6.8), u(1.6), u(7.0), "armor")
        for sx in (-1, 1):
            C("head", "cheek_%d" % sx, sx * u(4.3), hip + u(12.0), -u(3.6), u(1.4), u(4.6), u(7.0), "armor")
    # arms
    def arms(sd, n):
        x = u(7.8)
        bone("arm_" + n, "chest", (-x, hip + u(9.4), 0), side=sd)
        Cx("arm_" + n, "upper_" + n, -x - u(2.0), hip + u(4.6), -u(2.2), u(4.2), u(5.6), u(4.4), "suit", side=sd)
        Cx("arm_" + n, "upper_plate_" + n, -x - u(2.5), hip + u(5.2), -u(2.7), u(5.2), u(3.4), u(5.4), "armor", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-x, hip + u(4.4), 0), side=sd)
        Cx("arm_%s_low" % n, "fore_" + n, -x - u(1.9), hip - u(1.6), -u(2.0), u(3.8), u(6.0), u(4.0), "armor", side=sd)
        Cx("arm_%s_low" % n, "fist_" + n, -x - u(2.2), hip - u(3.6), -u(2.3), u(4.4), u(2.6), u(4.6), "stone", side=sd)
    pair(arms)
    # the ring shield on the left forearm: an octagon of two squares, rimmed and studded
    d = u(13.0 if not big else 16.0)
    bone("shield", "arm_l_low", (u(8.4), hip + u(1.6), -u(3.0)))
    C("shield", "shield_a", u(9.4), hip + u(1.6) - d / 2.0 + u(1.4), -u(5.6), d, d, u(1.6), "ring", paint="ring_shield")
    C("shield", "shield_b", u(9.4), hip + u(1.6) - d / 2.0 + u(1.4), -u(5.6), d, d, u(1.2), "ring", rot=(0, 0, 45), paint="ring_shield")
    C("shield", "shield_boss", u(9.4), hip + u(0.4), -u(7.0), u(3.2), u(3.2), u(1.6), "armor")
    if sword:
        bone("sword", "arm_r_low", (-u(8.4), hip - u(3.0), -u(2.0)))
        C("sword", "grip", -u(8.4), hip - u(4.0), -u(3.0), u(1.8), u(3.6), u(1.8), "suit")
        C("sword", "guard", -u(8.4), hip - u(1.0), -u(4.4), u(7.4), u(1.6), u(4.2), "armor")
        C("sword", "blade", -u(8.4), hip + u(0.6), -u(3.9), u(4.6), u(19.0), u(2.6), "stone", paint="blade_stone")
        C("sword", "blade_tip", -u(8.4), hip + u(19.6), -u(3.6), u(3.2), u(3.0), u(2.0), "stone")
    return hip + u(26.0)


@decal("blade_stone")
def _blade_stone(img, g, face, fw, fh, pal):
    if face in ("north", "south"):
        box(img, fw, fh, 0.45, 0.04, 0.55, 0.96, "#4a4846")
        for v in (0.2, 0.4, 0.6, 0.8):
            line(img, fw, fh, 0.2, v, 0.45, v + 0.05, "#44423f", 1)


def build_rocanel():
    reset()
    top = knight(1.0, "rocanel")
    pal = dict(m=dict(stone=("#8b8780", "stone"), armor=("#a19c92", "stone"), suit=("#2b2a2e", "cloth"), ring=("#8b8780", "stone")), eye="#9fd8ff", glow="#9fd8ff")
    return pal, knight_clips(), top, 3.0


def build_mira():
    reset()
    top = knight(1.28, "mira", big=True, sword=True)
    pal = dict(m=dict(stone=("#76767c", "stone"), armor=("#8c8c94", "stone"), suit=("#1f1f24", "cloth"), ring=("#76767c", "stone")), eye="#bfe6ff", glow="#bfe6ff")
    return pal, knight_clips(), top, 3.6
