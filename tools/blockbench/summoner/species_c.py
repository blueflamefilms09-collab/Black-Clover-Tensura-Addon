"""Summon models, part 3 (second pass, from the owner's reference pack): Ellie and Okiyosan (the Spirit dolls), Dradra (the Dragon) and Merus (the Angel).

Ellie: the porcelain girl with long wavy golden hair, teal eyes, black bows, a frilled white bib with a black bow on a navy puff-sleeve dress. Okiyosan: the white-kimono doll with the candle headband,
long black hair and a fanged half-lidded smile. Dradra: a bulky four-legged horned dragon with a fanged wedge head, black spine blades and black claws. Merus: the curly-haired angel with a halo,
brow gem, gold-and-black bracers, a white wrap and big feathered wings. A script fallback (rule 15), not made in an app.
"""
import math

from summon_lib import C, Cx, Clips, bone, pair, reset, decal, box, line, np, rgb


def humanoid_clips(skirt=False, wings=False, hover=True):
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, (1.4 if hover else 0.3) * s(t), 0))
    c.rot("idle", "head", lambda t: (2 * s(t, 1), 5 * s(t), 1.5 * s(t, 2)))
    c.rot("idle", "arm_r", lambda t: (3 * s(t, 0.6), 0, -5 + 2 * s(t)))
    c.rot("idle", "arm_l", lambda t: (3 * s(t, 2.2), 0, 5 - 2 * s(t)))
    c.rot("idle", "skirt", lambda t: (3 * s(t, 0.3), 0, 2 * s(t, 1)))
    c.rot("idle", "hair", lambda t: (4 + 3 * s(t, 0.8), 0, 2 * s(t)))
    for n in ("a", "b"):
        c.rot("idle", "wing_r" if n == "a" else "wing_l", lambda t, n=n: (0, 6 * s(t, 0.4) * (1 if n == "a" else -1), (-10 if n == "a" else 10) * s(t, 0.3)))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.pos("walk", "body", lambda t: (0, (1.2 if hover else 0.4) * abs(w(t)), 0))
    c.rot("walk", "body", lambda t: (8, 0, 0))
    c.rot("walk", "leg_r", lambda t: (28 * w(t), 0, 0))
    c.rot("walk", "leg_l", lambda t: (28 * w(t, math.pi), 0, 0))
    c.rot("walk", "arm_r", lambda t: (-22 * w(t), 0, -4))
    c.rot("walk", "arm_l", lambda t: (-22 * w(t, math.pi), 0, 4))
    c.rot("walk", "skirt", lambda t: (8 * w(t, 1), 0, 0))
    c.rot("walk", "hair", lambda t: (16 + 6 * w(t, 0.6), 0, 0))
    c.rot("walk", "wing_r", lambda t: (0, 8 * w(t, 0.5), -16 * w(t)))
    c.rot("walk", "wing_l", lambda t: (0, -8 * w(t, 0.5), 16 * w(t)))
    c.rot("attack", "body", rows=[(0, (0, 0, 0)), (0.15, (-8, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "arm_r", rows=[(0, (0, 0, -5)), (0.15, (-130, 0, -10)), (0.3, (-30, 0, 4)), (0.6, (0, 0, -5))])
    c.rot("attack", "arm_l", rows=[(0, (0, 0, 5)), (0.15, (-130, 0, 10)), (0.3, (-30, 0, -4)), (0.6, (0, 0, 5))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.15, (-10, 0, 0)), (0.3, (8, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "arm_r", rows=[(0, (0, 0, -5)), (0.5, (-150, 0, -30)), (1.1, (-150, 0, -30)), (1.6, (0, 0, -5))])
    c.rot("ability", "arm_l", rows=[(0, (0, 0, 5)), (0.5, (-150, 0, 30)), (1.1, (-150, 0, 30)), (1.6, (0, 0, 5))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.1, (-20, 0, 0)), (1.6, (0, 0, 0))])
    c.pos("ability", "body", rows=[(0, (0, 0, 0)), (0.6, (0, 4, 0)), (1.1, (0, 4, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", rows=[(0, (0, 0, 0)), (0.5, (0, 18, -40)), (1.1, (0, 18, -40)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_l", rows=[(0, (0, 0, 0)), (0.5, (0, -18, 40)), (1.1, (0, -18, 40)), (1.6, (0, 0, 0))])
    return c




# ---------------------------------------------------------------- decals: the dolls
def _eye_pair(img, g, fw, fh, iris, lid="#1a1418", lash=True, tilt=0.0, half=False, glow_g=60):
    for lo, hi, mir in ((0.14, 0.44, False), (0.56, 0.86, True)):
        v0, v1 = (0.40, 0.62) if not half else (0.44, 0.58)
        box(img, fw, fh, lo, v0, hi, v1, "#f4f6f8")
        ia, ib = (lo + 0.07, hi - 0.07)
        box(img, fw, fh, ia, v0, ib, v1, iris, g, glow_g)
        box(img, fw, fh, (ia + ib) / 2 - 0.035, v0 + 0.03, (ia + ib) / 2 + 0.035, v1 - 0.03, "#10121a")
        box(img, fw, fh, ia + 0.02, v0 + 0.02, ia + 0.06, v0 + 0.07, "#ffffff")
        line(img, fw, fh, lo - 0.02, v0 - 0.01 + (tilt if not mir else -tilt), hi + 0.02, v0 - 0.01 - (tilt if not mir else -tilt), lid, 2)
        if lash:
            o = lo - 0.03 if not mir else hi + 0.03
            line(img, fw, fh, o, v0 - 0.02, o + (-0.04 if not mir else 0.04), v0 + 0.06, lid, 1)
        if half:
            box(img, fw, fh, lo, v0, hi, v0 + 0.05, "#2a2024")


@decal("ellie_face")
def _ellie_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    top, bot = rgb("#1f7e92"), rgb("#9cf4ea")
    for lo, hi, mir in ((0.17, 0.45, False), (0.55, 0.83, True)):
        v0, v1 = 0.42, 0.68
        box(img, fw, fh, lo, v0, hi, v1, "#f6f8fa")
        ia, ib = lo + 0.05, hi - 0.05
        x0, x1, y0, y1 = int(ia * fw), max(int(ia * fw) + 2, int(ib * fw)), int(v0 * fh), max(int(v0 * fh) + 3, int(v1 * fh))
        for y in range(y0, min(fh, y1)):
            t = (y - y0) / max(1.0, y1 - y0 - 1.0)
            img[y, x0:x1] = top * (1 - t) + bot * t
            if g is not None:
                g[y, x0:x1] = 45 + 40 * t
        cx = (x0 + x1) // 2
        img[y0 + 1:y1 - 1, max(x0, cx - 1):cx + 2] = (10, 30, 44)
        img[y0 + 1:y0 + 3, x0 + 1:x0 + 3] = (255, 255, 255)
        img[y1 - 2:y1 - 1, x1 - 3:x1 - 1] = (255, 255, 255)
        # the heavy upper lid, a lash flick on the outer corner, a fine fold above the eye
        line(img, fw, fh, lo - 0.01, v0 - 0.01, hi + 0.01, v0 - 0.01, "#2c2230", 2)
        o = lo - 0.03 if not mir else hi + 0.03
        line(img, fw, fh, o, v0 - 0.02, o + (-0.03 if not mir else 0.03), v0 + 0.05, "#2c2230", 1)
        line(img, fw, fh, lo + 0.02, v0 - 0.08, hi - 0.02, v0 - 0.10 + (0.02 if mir else 0), "#caa890", 1)
        line(img, fw, fh, lo + 0.04, v1 + 0.01, hi - 0.04, v1 + 0.01, "#e4cfc6", 1)
    line(img, fw, fh, 0.14, 0.27, 0.44, 0.25, "#a88a48", 1)
    line(img, fw, fh, 0.56, 0.25, 0.86, 0.27, "#a88a48", 1)
    line(img, fw, fh, 0.50, 0.52, 0.49, 0.70, "#dcc2b8", 1)
    line(img, fw, fh, 0.43, 0.84, 0.57, 0.84, "#b0746c", 1)
    box(img, fw, fh, 0.12, 0.68, 0.24, 0.76, "#f4c4bc", blend=0.28)
    box(img, fw, fh, 0.76, 0.68, 0.88, 0.76, "#f4c4bc", blend=0.28)


@decal("okiyosan_face")
def _oki_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    _eye_pair(img, g, fw, fh, "#4a3a3a", tilt=0.03, half=True, glow_g=0)
    line(img, fw, fh, 0.14, 0.34, 0.42, 0.30, "#2a2024", 2)
    line(img, fw, fh, 0.58, 0.30, 0.86, 0.34, "#2a2024", 2)
    box(img, fw, fh, 0.28, 0.70, 0.72, 0.84, "#3a1418")
    n = 7
    for i in range(n):
        u = 0.28 + 0.44 * (i + 0.5) / n
        line(img, fw, fh, u - 0.03, 0.70, u, 0.78, "#f4f0ea", 1)
        line(img, fw, fh, u + 0.03, 0.70, u, 0.78, "#f4f0ea", 1)
    line(img, fw, fh, 0.22, 0.72, 0.28, 0.70, "#2a2024", 1)
    line(img, fw, fh, 0.72, 0.70, 0.78, 0.72, "#2a2024", 1)
    line(img, fw, fh, 0.50, 0.52, 0.48, 0.62, "#c8b0a8", 1)


@decal("bib")
def _bib(img, g, face, fw, fh, pal):
    if face != "north":
        return
    navy = rgb(pal["m"]["dress"][0])
    for y in range(fh):
        for x in range(fw):
            u, v = (x + 0.5) / fw, (y + 0.5) / fh
            edge = 0.78 - abs(u - 0.5) * 1.5
            if v > edge:
                img[y, x] = navy
    # the scalloped frill along the V, a few fine strokes of it
    for i in range(7):
        u = 0.08 + i * 0.14
        v = 0.78 - abs(u - 0.5) * 1.5
        box(img, fw, fh, u - 0.05, v - 0.07, u + 0.05, v + 0.02, "#e8e6ee")
        line(img, fw, fh, u, v - 0.04, u + (u - 0.5) * 0.1, v - 0.2, "#c4c2cc", 1)


@decal("hair_wave")
def _hair_wave(img, g, face, fw, fh, pal):
    if face not in ("north", "south"):
        return
    hi = pal.get("hair_hi", "#f0e090")
    lo = pal.get("hair_lo", "#8a7430")
    n = max(3, fw // 3)
    for i in range(n):
        u = (i + 0.5) / n
        a = 0.06 * (1 if i % 2 else -1)
        line(img, fw, fh, u, 0.0, u + a, 0.5, lo, 1)
        line(img, fw, fh, u + a, 0.5, u - a, 1.0, hi if i % 3 == 0 else lo, 1)


@decal("shoe")
def _shoe(img, g, face, fw, fh, pal):
    if face == "up":
        box(img, fw, fh, 0.2, 0.0, 0.8, 0.5, "#f4f0f4", blend=0.8)   # the white sock cuff


@decal("doll_hand")
def _doll_hand(img, g, face, fw, fh, pal):
    if face in ("north", "south"):
        for u in (0.3, 0.5, 0.7):
            line(img, fw, fh, u, 0.5, u, 1.0, "#c8b0a6", 1)


@decal("kimono_front")
def _kimono_front(img, g, face, fw, fh, pal):
    if face != "north":
        return
    c = "#b4aea6"
    line(img, fw, fh, 0.12, 0.0, 0.62, 1.0, c, 1)
    line(img, fw, fh, 0.88, 0.0, 0.38, 1.0, c, 1)
    line(img, fw, fh, 0.16, 0.0, 0.64, 1.0, "#f8f6f0", 1)
    line(img, fw, fh, 0.84, 0.0, 0.36, 1.0, "#e0dcd2", 1)


@decal("headband")
def _headband(img, g, face, fw, fh, pal):
    if face == "north":
        line(img, fw, fh, 0.62, 0.0, 0.52, 0.45, "#6a6460", 1)     # the crack across the band
        line(img, fw, fh, 0.52, 0.45, 0.60, 0.7, "#6a6460", 1)
        line(img, fw, fh, 0.60, 0.7, 0.50, 1.0, "#6a6460", 1)


@decal("flame")
def _flame(img, g, face, fw, fh, pal):
    box(img, fw, fh, 0.0, 0.0, 1.0, 1.0, "#ff9a30", g, 255)
    box(img, fw, fh, 0.25, 0.3, 0.75, 1.0, "#ffe070", g, 255)


def doll_frame(spec):
    """Shared skeleton of the two spirit dolls: legs, skirt, torso, arms, a big porcelain head, long hair on its own bone."""
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 6.6, 0))
    bone("skirt", "body", (0, 13.4, 0))
    st = spec["skirt"]
    for i, (y, h, w, d, mat) in enumerate(st):
        C("skirt", "skirt_%d" % i, 0, y, -d / 2.0, w, h, d, mat)

    def legs(sd, n):
        bone("leg_" + n, "body", (-1.9, 6.4, 0), side=sd)
        Cx("leg_" + n, "leg_" + n, -1.9 - 1.2, 1.6, -1.2, 2.4, 5.0, 2.4, "porcelain", side=sd)
        Cx("leg_" + n, "shoe_" + n, -1.9 - 1.6, 0, -2.6, 3.2, 1.8, 4.8, spec["shoe"], paint="shoe", side=sd)
    pair(legs)
    tw = spec["torso_w"]
    C("body", "torso", 0, 13.6, -2.3, tw, 5.8, 4.6, spec["bodice"], paint=spec.get("bodice_paint"))
    C("body", "neck", 0, 19.0, -1.2, 2.4, 1.2, 2.4, "porcelain")
    bone("head", "body", (0, 19.4, 0))
    hw, hh, hd = spec.get("head", (9.2, 8.8, 8.8))
    C("head", "head", 0, 19.6, -hd / 2.0, hw, hh, hd, "porcelain", paint=spec["face"])

    def arms(sd, n):
        bone("arm_" + n, "body", (-tw / 2.0 - 0.6, 18.6, 0), side=sd)
        sw, sh = spec["sleeve"]
        Cx("arm_" + n, "sleeve_" + n, -tw / 2.0 - 0.6 - sw / 2.0 - 0.4, 18.8 - sh, -sw / 2.0, sw, sh, sw, spec["sleeve_mat"], side=sd)
        Cx("arm_" + n, "forearm_" + n, -tw / 2.0 - 0.6 - 1.4 - 0.2, 10.6, -1.1, 2.3, 18.8 - sh - 10.6 + 0.6, 2.3, "porcelain", side=sd)
        Cx("arm_" + n, "wrist_" + n, -tw / 2.0 - 0.6 - 1.55, 10.2, -1.25, 2.6, 0.7, 2.6, "joint", side=sd)
        Cx("arm_" + n, "hand_" + n, -tw / 2.0 - 0.6 - 1.4 - 0.2, 7.8, -1.2, 2.4, 2.5, 2.4, "porcelain", paint="doll_hand", side=sd)
    pair(arms)


# ---------------------------------------------------------------- Ellie (third pass): a slim, pure porcelain girl: long wavy olive-gold hair parted in the middle and swept round her face, big teal eyes, black bows with white ear ribbons,
# a black choker bow on a frilled V bib, a navy dress with half puff sleeves
def build_ellie():
    reset()
    spec = dict(skirt=[(10.6, 3.4, 7.0, 6.0, "dress"), (7.8, 3.2, 8.4, 7.2, "dress"), (5.4, 2.6, 9.6, 8.2, "dress"), (5.0, 0.5, 9.8, 8.4, "trim")],
                shoe="shoe", torso_w=5.6, bodice="dress", sleeve=(4.2, 3.8), sleeve_mat="dress", face="ellie_face", head=(8.2, 8.6, 8.0))
    doll_frame(spec)
    C("body", "bib", 0, 13.6, -2.9, 5.6, 5.6, 0.7, "trim", paint="bib")
    for sx in (-1, 1):
        C("body", "frill%d" % sx, sx * 2.9, 17.2, -2.7, 2.6, 2.2, 1.1, "trim", rot=(0, 0, sx * -18))
        C("body", "frill_b%d" % sx, sx * 2.9, 17.0, 1.6, 2.6, 2.0, 1.1, "trim", rot=(0, 0, sx * -18))
        C("body", "frill_v%d" % sx, sx * 1.6, 13.0, -3.0, 3.6, 0.9, 1.1, "trim", rot=(0, 0, sx * -24))
    C("body", "choker", 0, 18.7, -2.0, 3.4, 0.7, 4.0, "ribbon")
    C("body", "bow_knot", 0, 17.5, -3.5, 1.4, 1.4, 1.0, "ribbon")
    C("body", "bow_a_r", -1.9, 16.9, -3.5, 2.6, 2.2, 0.9, "ribbon", rot=(0, 0, 16))
    C("body", "bow_a_l", 1.9, 16.9, -3.5, 2.6, 2.2, 0.9, "ribbon", rot=(0, 0, -16))
    C("body", "bow_t_r", -1.0, 13.8, -3.6, 1.5, 3.8, 0.8, "ribbon", rot=(0, 0, 8))
    C("body", "bow_t_l", 1.0, 13.8, -3.6, 1.5, 3.8, 0.8, "ribbon", rot=(0, 0, -8))
    C("body", "waist_line", 0, 13.5, -2.45, 6.0, 0.6, 4.9, "dress_dark")
    # the hair. Crown, then two swept fringe locks leaving the middle part open, then long waves falling in front of and behind the shoulders
    C("head", "hair_cap", 0, 27.4, -4.4, 8.9, 2.0, 8.7, "hair")
    C("head", "hair_part_r", -1.2, 26.2, -4.6, 1.2, 2.6, 0.9, "hair", rot=(0, 0, 14), pv=(-0.6, 28.6, -4.2))
    C("head", "hair_part_l", 1.2, 26.2, -4.6, 1.2, 2.6, 0.9, "hair", rot=(0, 0, -14), pv=(0.6, 28.6, -4.2))
    for sx in (-1, 1):
        C("head", "fringe_a%d" % sx, sx * 2.6, 25.4, -4.6, 3.2, 2.8, 1.0, "hair", rot=(0, 0, sx * 24), pv=(sx * 1.0, 28.2, -4.1))
        C("head", "fringe_b%d" % sx, sx * 4.15, 19.8, -4.3, 1.3, 7.0, 0.8, "hair", rot=(0, 0, sx * -3), pv=(sx * 4.0, 26.0, -4.0))
        C("head", "temple%d" % sx, sx * 4.5, 18.0, -3.6, 1.2, 10.4, 6.8, "hair")
        # long front waves, alternating a little side to side like the strands in the picture
        C("head", "wave_a%d" % sx, sx * 7.0, 11.6, -2.6, 2.6, 8.4, 4.6, "hair", rot=(0, 0, sx * -3), pv=(sx * 6.6, 20, 0))
        C("head", "wave_a0%d" % sx, sx * 6.0, 17.6, -2.8, 2.6, 4.6, 4.2, "hair", rot=(0, 0, sx * 12), pv=(sx * 5.2, 22, 0))
        C("head", "wave_b%d" % sx, sx * 7.6, 6.0, -2.6, 2.8, 6.2, 4.6, "hair", rot=(0, 0, sx * 6), pv=(sx * 5.4, 12, 0))
        C("head", "wave_c%d" % sx, sx * 7.0, 1.2, -2.6, 2.8, 5.6, 4.6, "hair", rot=(0, 0, sx * -7), pv=(sx * 5.8, 7, 0))
        C("head", "curl%d" % sx, sx * 7.4, -0.6, -2.4, 3.2, 2.6, 4.2, "hair", rot=(0, 0, sx * 16))
        C("head", "ear_ribbon%d" % sx, sx * 4.6, 20.8, -1.6, 0.9, 3.6, 1.8, "ribbon_w", rot=(0, 0, sx * 6))
        C("head", "ear_ribbon_t%d" % sx, sx * 4.9, 23.8, -1.6, 0.9, 1.6, 2.2, "ribbon_w", rot=(0, 0, sx * -12))
        C("head", "bow_k%d" % sx, sx * 4.6, 27.2, -1.4, 1.4, 1.6, 1.4, "ribbon")
        C("head", "bow_u%d" % sx, sx * 5.0, 28.2, -1.0, 2.8, 2.6, 1.2, "ribbon", rot=(0, 0, sx * -30))
        C("head", "bow_d%d" % sx, sx * 5.2, 25.4, -1.0, 2.8, 2.6, 1.2, "ribbon", rot=(0, 0, sx * 30))
        C("head", "bow_t%d" % sx, sx * 5.4, 23.4, -0.6, 1.2, 3.4, 1.0, "ribbon", rot=(0, 0, sx * 8))
    bone("hair", "head", (0, 27, 4.0))
    C("hair", "back_a", 0, 15.0, 3.4, 9.6, 12.6, 2.6, "hair", paint="hair_wave")
    C("hair", "back_s_r", -6.8, 10.0, 2.0, 2.6, 16.0, 3.6, "hair", paint="hair_wave", rot=(0, 0, 4), pv=(-5.4, 26, 4))
    C("hair", "back_s_l", 6.8, 10.0, 2.0, 2.6, 16.0, 3.6, "hair", paint="hair_wave", rot=(0, 0, -4), pv=(5.4, 26, 4))
    C("hair", "back_b", -0.8, 8.4, 3.2, 11.6, 6.8, 2.8, "hair", paint="hair_wave", rot=(0, 0, 3), pv=(0, 15, 4))
    C("hair", "back_c", 1.0, 3.2, 3.0, 12.4, 5.6, 2.8, "hair", paint="hair_wave", rot=(0, 0, -4), pv=(0, 9, 4))
    C("hair", "back_d", -1.2, 0.0, 2.8, 8.8, 3.6, 2.6, "hair", paint="hair_wave", rot=(0, 0, 6), pv=(0, 4, 4))
    top = 31.0
    pal = dict(m=dict(porcelain=("#fbf3ee", "porcelain"), joint=("#c4b4b0", "plate"), hair=("#c6b45a", "hair"), dress=("#242b52", "cloth"), dress_dark=("#171c3c", "cloth"), trim=("#f4f2f8", "cloth"),
                      ribbon=("#181a2c", "cloth"), ribbon_w=("#eceef4", "cloth"), shoe=("#14141c", "plate")), eye="#7fd8e0", glow="#8fe0ff", hair_hi="#e8da8c", hair_lo="#85763a")
    c = humanoid_clips()
    return pal, c, top, 3.2


# ---------------------------------------------------------------- Okiyosan: white kimono with crossed lapels, a grey obi with a big knot, the candle headband, long black hair, a fanged half-lidded smile
def build_okiyosan():
    reset()
    spec = dict(skirt=[(10.6, 3.2, 7.2, 6.0, "kimono"), (7.6, 3.2, 7.8, 6.4, "kimono"), (4.4, 3.4, 8.2, 6.8, "kimono"), (2.2, 2.2, 8.6, 7.0, "kimono_s"), (1.6, 0.6, 8.8, 7.2, "hem")],
                shoe="sandal", torso_w=6.0, bodice="kimono", bodice_paint="kimono_front", sleeve=(4.0, 7.4), sleeve_mat="kimono", face="okiyosan_face")
    doll_frame(spec)
    C("body", "lapel_r", -1.6, 14.6, -2.55, 2.8, 5.0, 0.5, "kimono_s", rot=(0, 0, -22))
    C("body", "lapel_l", 1.6, 14.6, -2.65, 2.8, 5.0, 0.5, "kimono", rot=(0, 0, 22))
    C("body", "obi", 0, 12.8, -2.9, 6.8, 3.0, 5.6, "obi")
    C("body", "obi_knot", 0, 13.0, -4.0, 3.2, 2.6, 1.4, "obi_d")
    C("body", "obi_tail_r", -1.0, 6.8, -4.1, 1.8, 6.6, 0.8, "obi", rot=(8, 0, 6), pv=(-1, 13, -4))
    C("body", "obi_tail_l", 1.2, 7.4, -4.1, 1.8, 6.0, 0.8, "obi", rot=(6, 0, -9), pv=(1, 13, -4))
    C("body", "obi_back", 0, 12.4, 2.3, 6.2, 3.4, 1.8, "obi_d")
    # the headband with a crack, two candles with real flames on top, hair: straight bangs, long locks, a long black back
    C("head", "band", 0, 25.0, -4.9, 9.8, 2.0, 9.8, "band", paint="headband")
    C("head", "hair_cap", 0, 27.0, -4.8, 9.8, 2.4, 9.6, "hair")
    for sx in (-1, 1):
        C("head", "candle%d" % sx, sx * 3.0, 27.0, -0.8, 1.5, 3.6, 1.5, "candle")
        C("head", "wax%d" % sx, sx * 3.0, 26.6, -0.9, 1.9, 0.9, 1.7, "candle")
        C("head", "flame_a%d" % sx, sx * 3.0, 30.6, -1.0, 2.0, 1.8, 2.0, "flame", paint="flame")
        C("head", "flame_b%d" % sx, sx * 3.0, 32.2, -0.8, 1.2, 1.8, 1.6, "flame", paint="flame")
        C("head", "lock_a%d" % sx, sx * 5.2, 13.0, -3.2, 2.2, 13.0, 3.6, "hair")
        C("head", "lock_b%d" % sx, sx * 5.0, 8.6, -3.0, 2.0, 5.0, 3.2, "hair")
    bone("hair", "head", (0, 26, 4.4))
    C("hair", "back_a", 0, 14.6, 3.6, 10.0, 13.0, 2.4, "hair", paint="hair_wave")
    C("hair", "back_b", 0, 5.0, 3.4, 9.6, 10.2, 2.2, "hair", paint="hair_wave")
    C("hair", "back_c", 0, 1.0, 3.2, 8.2, 4.4, 2.0, "hair", paint="hair_wave")
    # the curse nail in the right hand
    C("arm_r", "nail_shaft", -6.6, 3.0, -0.5, 0.9, 6.6, 0.9, "nail")
    C("arm_r", "nail_head", -6.6, 9.4, -0.9, 2.2, 0.8, 1.8, "nail")
    top = 34.0
    pal = dict(m=dict(porcelain=("#f2eae4", "porcelain"), joint=("#c4b8b0", "plate"), hair=("#16141c", "hair"), kimono=("#f2eee8", "cloth"), kimono_s=("#d6d0c6", "cloth"), hem=("#b8b0a6", "cloth"),
                      kimono_front=("#f2eee8", "cloth"), obi=("#6c6770", "cloth"), obi_d=("#4a4650", "cloth"), band=("#f6f2ec", "cloth"), candle=("#efe6c8", "plate"), flame=("#ff9a30", "glow"),
                      nail=("#c9c4bc", "plate"), sandal=("#2c262a", "plate")), eye="#e0b8a0", glow="#ffa030", hair_hi="#4a4a66", hair_lo="#08080c")
    pal["m"]["kimono_front"] = ("#f2eee8", "cloth")
    c = humanoid_clips()
    return pal, c, top, 3.2


# ---------------------------------------------------------------- decals: Dradra and Merus
@decal("dra_skull")
def _dra_skull(img, g, face, fw, fh, pal):
    if face in ("east", "west", "north"):
        # a narrow, angry yellow eye under a heavy black brow, a few scale ridges
        if face == "north":
            for lo, hi in ((0.08, 0.40), (0.60, 0.92)):
                box(img, fw, fh, lo, 0.40, hi, 0.55, "#0c0b10")
                box(img, fw, fh, lo + 0.05, 0.44, hi - 0.05, 0.53, "#ffc040", g, 255)
                box(img, fw, fh, (lo + hi) / 2 - 0.02, 0.44, (lo + hi) / 2 + 0.02, 0.53, "#101010")
            line(img, fw, fh, 0.04, 0.30, 0.42, 0.44, "#0c0b10", 2)
            line(img, fw, fh, 0.96, 0.30, 0.58, 0.44, "#0c0b10", 2)
        else:
            box(img, fw, fh, 0.52, 0.34, 0.86, 0.50, "#0c0b10")
            box(img, fw, fh, 0.58, 0.38, 0.82, 0.48, "#ffc040", g, 255)
            box(img, fw, fh, 0.66, 0.38, 0.72, 0.48, "#101010")
            line(img, fw, fh, 0.46, 0.26, 0.92, 0.38, "#0c0b10", 2)
        for v in (0.62, 0.74, 0.86):
            line(img, fw, fh, 0.1, v, 0.9, v - 0.04, "#26282e", 1)


@decal("dra_belly")
def _dra_belly(img, g, face, fw, fh, pal):
    if face in ("down", "north", "south", "east", "west"):
        n = max(3, int(fh / 3))
        for i in range(n):
            v = (i + 0.5) / n
            line(img, fw, fh, 0.0, v, 1.0, v, "#8e8878", 1)


@decal("dra_jaw")
def _dra_jaw(img, g, face, fw, fh, pal):
    if face == "up":
        box(img, fw, fh, 0.1, 0.05, 0.9, 0.95, "#8a1a14", g, 140)
        box(img, fw, fh, 0.3, 0.1, 0.7, 0.9, "#c8452a", g, 200)
    for u in (0.12, 0.32, 0.68, 0.88):
        if face in ("east", "west", "north"):
            box(img, fw, fh, u - 0.04, 0.0, u + 0.04, 0.5, "#efeadc")


@decal("dra_roof")
def _dra_roof(img, g, face, fw, fh, pal):
    if face == "down":
        box(img, fw, fh, 0.1, 0.05, 0.9, 0.95, "#8a1a14", g, 140)
        box(img, fw, fh, 0.3, 0.1, 0.7, 0.9, "#c8452a", g, 200)


@decal("scale_pat")
def _scale_pat(img, g, face, fw, fh, pal):
    # overlapping scale arcs
    step = max(3, fw // 4)
    for yy in range(1, fh, step):
        off = ((yy // step) % 2) * (step // 2)
        for xx in range(off, fw, step):
            if xx + 1 < fw and yy + 1 < fh:
                img[yy, max(0, xx - 1):xx + 2] *= 0.72
                img[yy:yy + 2, xx] *= 0.8


@decal("merus_face")
def _merus_face(img, g, face, fw, fh, pal):
    if face != "north":
        return
    _eye_pair(img, g, fw, fh, "#82b4d8", lid="#4a3a2c", lash=False, glow_g=40)
    line(img, fw, fh, 0.12, 0.31, 0.44, 0.30, "#8a6a40", 1)
    line(img, fw, fh, 0.56, 0.30, 0.88, 0.31, "#8a6a40", 1)
    line(img, fw, fh, 0.50, 0.50, 0.48, 0.62, "#d2b09a", 1)
    line(img, fw, fh, 0.42, 0.78, 0.58, 0.78, "#b07468", 1)
    box(img, fw, fh, 0.42, 0.06, 0.58, 0.20, "#fff0a0", g, 255)       # the brow gem


@decal("merus_chest")
def _merus_chest(img, g, face, fw, fh, pal):
    if face != "north":
        return
    sh = "#b8926e"
    line(img, fw, fh, 0.50, 0.30, 0.50, 1.0, sh, 1)
    for v in (0.56, 0.74, 0.92):
        line(img, fw, fh, 0.12, v, 0.88, v, sh, 1)
    line(img, fw, fh, 0.10, 0.28, 0.46, 0.34, sh, 1)
    line(img, fw, fh, 0.90, 0.28, 0.54, 0.34, sh, 1)
    box(img, fw, fh, 0.16, 0.20, 0.24, 0.26, "#d9867a")
    box(img, fw, fh, 0.76, 0.20, 0.84, 0.26, "#d9867a")


@decal("merus_collar")
def _merus_collar(img, g, face, fw, fh, pal):
    if face == "north":
        box(img, fw, fh, 0.42, 0.1, 0.58, 0.9, "#14141a")
        box(img, fw, fh, 0.45, 0.25, 0.55, 0.75, "#fff0a0", g, 255)


@decal("bracer")
def _bracer(img, g, face, fw, fh, pal):
    for v in (0.0, 0.5):
        box(img, fw, fh, 0.0, v, 1.0, v + 0.18, "#16161c")
    box(img, fw, fh, 0.0, 0.34, 1.0, 0.4, "#fff0a0", blend=0.5)


@decal("feather")
def _feather(img, g, face, fw, fh, pal):
    if face in ("north", "south"):
        line(img, fw, fh, 0.02, 0.5, 0.98, 0.5, "#d8d2c4", 1)
        n = max(4, fw // 3)
        for i in range(n):
            u = (i + 0.5) / n
            line(img, fw, fh, u, 0.5, min(1.0, u + 0.12), 0.0, "#e2dccf", 1)
            line(img, fw, fh, u, 0.5, min(1.0, u + 0.12), 1.0, "#e2dccf", 1)


@decal("curl")
def _curl(img, g, face, fw, fh, pal):
    n = max(2, fw // 3)
    for i in range(n):
        u = (i + 0.5) / n
        line(img, fw, fh, u - 0.1, 0.1, u + 0.1, 0.5, "#9a7a3c", 1)
        line(img, fw, fh, u + 0.1, 0.5, u - 0.1, 0.95, "#f0d890", 1)


@decal("wrap")
def _wrap(img, g, face, fw, fh, pal):
    if face in ("north", "south"):
        line(img, fw, fh, 0.0, 0.2, 1.0, 0.55, "#c8c2b8", 1)
        for u in (0.2, 0.4, 0.6, 0.8):
            line(img, fw, fh, u, 0.3, u + 0.05, 1.0, "#dcd6cc", 1)


# ---------------------------------------------------------------- Dradra: the bulky four-legged horned dragon
def claws(b, prefix, x0, z, w, n=3, big=1.0):
    """n toes with long black claws on the front of a foot (x0 the low x corner of the foot of width w)."""
    for i in range(n):
        xx = x0 + (w / n) * i + 0.2
        tw = w / n - 0.4
        Cx(b, "%s_toe%d" % (prefix, i), xx, 0.0, z - 3.0, tw, 2.2, 3.4, "toe")
        Cx(b, "%s_claw%d" % (prefix, i), xx + tw * 0.1, 0.0, z - 3.0 - 4.4 * big, tw * 0.8, 1.9 * big, 4.8 * big, "claw", rot=(-14, 0, 0), pv=(xx + tw / 2, 1.0, z - 3.0))


def build_dradra():
    reset()
    L, bh, bw, bl = 15.0, 15.0, 15.0, 28.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, L + bh / 2, 0))
    C("body", "torso", 0, L, -bl / 2 + 8, bw, bh, bl - 8, "scale", paint="scale_pat")
    C("body", "chest", 0, L - 0.5, -bl / 2 - 1, bw + 2.4, bh + 1.6, 11, "scale", paint="scale_pat")
    C("body", "belly", 0, L - 1.0, -bl / 2 - 0.5, bw - 3.5, 1.6, bl + 1, "belly", paint="dra_belly")
    C("body", "chest_plate", 0, L + 1.0, -bl / 2 - 1.4, bw - 5, bh - 5, 0.8, "belly", paint="dra_belly")
    for i in range(9):
        h = 6.2 - 0.42 * i
        C("body", "spine_%d" % i, 0, L + bh - 0.4, -bl / 2 + 1 + i * 3.1, 1.8, h, 3.4, "spike", rot=(-28, 0, 0))
    # neck: three steps rising forward, spiked, to a low aggressive head
    bone("neck", "body", (0, L + bh - 3, -bl / 2 - 2))
    C("neck", "neck_a", 0, L + bh - 4, -bl / 2 - 10, 11, 10, 9, "scale", paint="scale_pat")
    C("neck", "neck_b", 0, L + bh - 1.5, -bl / 2 - 17, 10, 10, 8, "scale", paint="scale_pat")
    C("neck", "neck_belly", 0, L + bh - 4.4, -bl / 2 - 17, 7, 1.4, 16, "belly", paint="dra_belly")
    for i, (z, y) in enumerate(((-9.5, 8.0), (-14.5, 9.6), (-19.0, 11.4))):
        C("neck", "nspike_%d" % i, 0, L + bh + y - 1, -bl / 2 + z, 1.6, 5.6 - i * 0.6, 3.0, "spike", rot=(-26, 0, 0))
    bone("head", "neck", (0, L + bh + 2, -bl / 2 - 17))
    hz = -bl / 2 - 17
    hy = L + bh - 1.0
    C("head", "skull", 0, hy, hz - 10, 10.6, 9, 10, "scale", paint="dra_skull")
    C("head", "brow", 0, hy + 8.0, hz - 9.8, 11.6, 1.8, 7.6, "spike")
    C("head", "snout", 0, hy + 0.4, hz - 20, 7.6, 5.8, 10.4, "scale", paint="dra_roof")
    C("head", "nose", 0, hy + 1.4, hz - 23.4, 5.6, 4.4, 3.8, "scale")
    C("head", "nostril_r", -1.9, hy + 4.2, hz - 23.7, 1.4, 1.4, 0.9, "gem")
    C("head", "nostril_l", 1.9, hy + 4.2, hz - 23.7, 1.4, 1.4, 0.9, "gem")
    for sx in (-1, 1):
        C("head", "horn_a%d" % sx, sx * 3.6, hy + 8.4, hz - 5.6, 2.6, 7.6, 2.6, "horn", rot=(-34, 0, sx * -9))
        C("head", "horn_b%d" % sx, sx * 4.9, hy + 14.0, hz - 1.4, 1.8, 6.0, 1.8, "horn", rot=(-46, 0, sx * -8))
        C("head", "horn_c%d" % sx, sx * 2.0, hy + 9.0, hz - 4.6, 1.6, 4.4, 1.6, "horn", rot=(-18, 0, sx * 4))
        C("head", "cheek_a%d" % sx, sx * 5.9, hy + 0.6, hz - 9.0, 2.4, 2.4, 5.6, "spike", rot=(0, sx * 26, 0))
        C("head", "cheek_b%d" % sx, sx * 5.0, hy - 1.8, hz - 12.0, 2.0, 2.0, 4.6, "spike", rot=(0, sx * 22, 0))
        for k, zz in enumerate((-15.0, -18.6, -22.0)):
            C("head", "tooth_u%d_%d" % (sx, k), sx * 3.5, hy - 1.4 + 0.2, hz + zz, 1.2, 2.6 - 0.2 * k, 1.2, "fang")
    bone("jaw", "head", (0, hy + 1.2, hz - 8))
    C("jaw", "jaw", 0, hy - 2.6, hz - 21.6, 6.6, 2.6, 13.4, "scale", paint="dra_jaw")
    C("jaw", "chin", 0, hy - 3.6, hz - 20, 4.6, 1.2, 8, "belly", paint="dra_belly")
    for sx in (-1, 1):
        for k, zz in enumerate((-14.2, -17.6, -21.0)):
            C("jaw", "tooth_l%d_%d" % (sx, k), sx * 2.9, hy - 0.2, hz + zz, 1.1, 2.4 - 0.2 * k, 1.1, "fang")

    def wings(sd, n):
        bone("wing_" + n, "body", (-6.5, L + bh - 1, -2), side=sd)
        Cx("wing_" + n, "wing_arm_" + n, -6.5 - 15, L + bh - 2.4, -3.2, 15, 2.6, 2.6, "scale", side=sd)
        Cx("wing_" + n, "wing_mem_a_" + n, -6.5 - 15, L + bh - 3.2, -0.6, 15, 0.8, 15, "membrane", side=sd)
        bone("wing_%s_tip" % n, "wing_" + n, (-21.5, L + bh - 1, -2), side=sd)
        Cx("wing_%s_tip" % n, "wing_fin_a_" + n, -21.5 - 16, L + bh - 2.2, -3.0, 16, 2.0, 2.0, "horn", side=sd)
        Cx("wing_%s_tip" % n, "wing_mem_b_" + n, -21.5 - 18, L + bh - 3.0, -1.0, 18, 0.8, 18, "membrane", side=sd)
        Cx("wing_%s_tip" % n, "wing_claw_" + n, -21.5 - 18.5, L + bh - 2.4, -3.4, 2.4, 2.4, 3.4, "claw", side=sd)
    pair(wings)

    def legs(sd, n, prefix, z, thick, big):
        bone("%s_%s" % (prefix, n), "body", (-bw / 2 - 1, L, z), side=sd)
        Cx("%s_%s" % (prefix, n), "%s_up_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 1, L * 0.38, z - thick / 2, thick + 1.6, L * 0.62, thick + 0.6, "scale", paint="scale_pat", side=sd)
        Cx("%s_%s" % (prefix, n), "%s_lo_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 0.4, 2.2, z - thick / 2 + 0.4, thick, L * 0.38 - 2.2, thick - 0.4, "scale", paint="scale_pat", side=sd)
        fw = thick + 2.0
        Cx("%s_%s" % (prefix, n), "%s_foot_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 1.0, 0, z - thick / 2 - 0.4, fw, 2.6, thick + 1.4, "belly", side=sd)
        claws("%s_%s" % (prefix, n), "%s_%s" % (prefix, n), -bw / 2 - 1 - thick / 2 - 1.0, z - thick / 2 - 0.4, fw, 3, big)
    pair(lambda sd, n: legs(sd, n, "lf", -bl / 2 + 3, 5.0, 1.0))
    pair(lambda sd, n: legs(sd, n, "lb", bl / 2 - 5, 6.0, 0.9))
    ty = L + bh * 0.42
    bone("tail1", "body", (0, ty, bl / 2 - 1))
    C("tail1", "tail_a", 0, ty - 5.4, bl / 2 - 1, 10, 10, 9, "scale", paint="scale_pat")
    bone("tail2", "tail1", (0, ty - 1, bl / 2 + 8))
    C("tail2", "tail_b", 0, ty - 5.0, bl / 2 + 8, 7.6, 7.6, 9, "scale", paint="scale_pat")
    bone("tail3", "tail2", (0, ty - 1.5, bl / 2 + 17))
    C("tail3", "tail_c", 0, ty - 4.4, bl / 2 + 17, 5.4, 5.4, 9, "scale")
    C("tail3", "tail_spike", 0, ty - 6.6, bl / 2 + 24.4, 3.4, 9.0, 3.4, "spike", rot=(80, 0, 0))
    for i, (zz, y) in enumerate(((2, 4.6), (10, 3.8), (18, 3.2))):
        C("tail1" if i == 0 else "tail2" if i == 1 else "tail3", "tspike_%d" % i, 0, ty - y + 5.2 - 0.5, bl / 2 + zz, 1.4, 4.0 - 0.4 * i, 3.0, "spike", rot=(-24, 0, 0))
    top = L + bh + 26
    pal = dict(m=dict(scale=("#3b3e48", "scale"), belly=("#cdc7b2", "plate"), spike=("#17181e", "plate"), horn=("#b9b19a", "plate"), claw=("#101014", "plate"), toe=("#8c8678", "plate"),
                      fang=("#efeadc", "plate"), membrane=("#5b4a54", "scale"), gem=("#ff7a2a", "glow")), eye="#ffc040", glow="#ff7a2a")
    c = Clips()
    P = 3.2
    s = lambda t, ph=0.0: math.sin(2 * math.pi * t / P + ph)
    c.pos("idle", "body", lambda t: (0, 0.5 * s(t), 0))
    c.rot("idle", "neck", lambda t: (3 * s(t, 0.6), 4 * s(t), 0))
    c.rot("idle", "head", lambda t: (2 * s(t, 1), 3 * s(t, 0.4), 0))
    c.rot("idle", "jaw", lambda t: (3 + 3 * max(0, s(t, 0.5)), 0, 0))
    c.rot("idle", "wing_r", lambda t: (0, 0, -9 * s(t, 0.3)))
    c.rot("idle", "wing_l", lambda t: (0, 0, 9 * s(t, 0.3)))
    c.rot("idle", "wing_r_tip", lambda t: (0, 0, -7 * s(t, -0.4)))
    c.rot("idle", "wing_l_tip", lambda t: (0, 0, 7 * s(t, -0.4)))
    for i, tn in enumerate(("tail1", "tail2", "tail3")):
        c.rot("idle", tn, lambda t, i=i: (0, 7 * (i + 1) * s(t, -i * 0.8), 0))
    w = lambda t, ph=0.0: math.sin(2 * math.pi * t / 1.0 + ph)
    c.rot("walk", "wing_r", lambda t: (0, 0, -34 * w(t)))
    c.rot("walk", "wing_l", lambda t: (0, 0, 34 * w(t)))
    c.rot("walk", "wing_r_tip", lambda t: (0, 0, -24 * w(t, -0.9)))
    c.rot("walk", "wing_l_tip", lambda t: (0, 0, 24 * w(t, -0.9)))
    c.pos("walk", "body", lambda t: (0, 0.9 * w(t, 1.2), 0))
    for pre, ph in (("lf", 0.0), ("lb", math.pi)):
        c.rot("walk", pre + "_r", lambda t, ph=ph: (22 * w(t, ph), 0, 0))
        c.rot("walk", pre + "_l", lambda t, ph=ph: (22 * w(t, ph + math.pi), 0, 0))
    for i, tn in enumerate(("tail1", "tail2", "tail3")):
        c.rot("walk", tn, lambda t, i=i: (-4, 9 * (i + 1) * w(t, 0.8 - i * 0.6), 0))
    c.rot("attack", "neck", rows=[(0, (0, 0, 0)), (0.15, (-16, 0, 0)), (0.32, (24, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "head", rows=[(0, (0, 0, 0)), (0.15, (-14, 0, 0)), (0.32, (18, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("attack", "jaw", rows=[(0, (0, 0, 0)), (0.15, (36, 0, 0)), (0.32, (4, 0, 0)), (0.6, (0, 0, 0))])
    c.rot("ability", "neck", rows=[(0, (0, 0, 0)), (0.5, (-24, 0, 0)), (1.2, (6, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "head", rows=[(0, (0, 0, 0)), (0.5, (-30, 0, 0)), (1.2, (10, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "jaw", rows=[(0, (0, 0, 0)), (0.5, (38, 0, 0)), (1.2, (34, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "body", rows=[(0, (0, 0, 0)), (0.5, (-14, 0, 0)), (1.2, (-4, 0, 0)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_r", rows=[(0, (0, 0, 0)), (0.5, (0, 0, -40)), (1.1, (0, 0, -40)), (1.6, (0, 0, 0))])
    c.rot("ability", "wing_l", rows=[(0, (0, 0, 0)), (0.5, (0, 0, 40)), (1.1, (0, 0, 40)), (1.6, (0, 0, 0))])
    return pal, c, top, 5.2


# ---------------------------------------------------------------- Merus: the curly-haired angel, halo, brow gem, gold and black bracers, white wrap, large feathered wings
def build_merus():
    reset()
    lt, lx, hip = 4.6, 2.7, 15.0
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, hip, 0))
    bone("chest", "body", (0, hip + 5, 0))
    bone("head", "chest", (0, hip + 16.4, 0))

    def legs(sd, n):
        bone("leg_" + n, "root", (-lx, hip, 0), side=sd)
        Cx("leg_" + n, "thigh_" + n, -lx - lt / 2, hip * 0.5, -lt / 2, lt, hip * 0.5, lt, "robe", side=sd)
        Cx("leg_" + n, "shin_" + n, -lx - lt / 2 + 0.2, 1.8, -lt / 2 + 0.2, lt - 0.4, hip * 0.5 - 1.8, lt - 0.4, "skin", side=sd)
        Cx("leg_" + n, "sandal_" + n, -lx - lt / 2 - 0.4, 0, -lt / 2 - 1.8, lt + 0.8, 1.8, lt + 3.0, "gold", side=sd)
        Cx("leg_" + n, "strap_" + n, -lx - lt / 2 + 0.1, 1.8, -lt / 2 - 0.1, lt - 0.2, 0.8, lt + 0.2, "gold", side=sd)
    pair(legs)
    C("body", "wrap", 0, hip - 3.0, -3.6, 10.6, 8.5, 7.2, "robe", paint="wrap")
    C("body", "wrap_low", 0, hip - 9.0, -3.9, 9.6, 6.2, 7.0, "robe")
    C("body", "sash", 0, hip + 3.0, -3.8, 11.0, 1.8, 7.6, "gold")
    C("body", "cloth_f", 0, 3.4, -4.4, 6.2, 11.6, 1.2, "robe2", paint="wrap")
    C("body", "cloth_b", 0, 3.4, 3.0, 6.2, 11.6, 1.0, "robe2")
    C("chest", "torso", 0, hip + 5, -2.8, 10.8, 10.4, 5.8, "skin", paint="merus_chest")
    C("chest", "collar", 0, hip + 14.6, -3.4, 8.8, 2.0, 6.8, "gold", paint="merus_collar")
    C("chest", "collar_t", 0, hip + 14.0, -4.0, 2.0, 3.6, 0.8, "dark")
    for sx in (-1, 1):
        C("chest", "pauld%d" % sx, sx * 6.2, hip + 12.6, -3.0, 3.4, 3.4, 6.0, "dark")
        C("chest", "pauld_g%d" % sx, sx * 6.2, hip + 15.6, -3.0, 3.2, 0.7, 6.0, "gold")
    C("head", "head", 0, hip + 16.8, -4.2, 8.4, 8.4, 8.4, "skin", paint="merus_face")
    # curly hair: a cap and ten curls around it
    C("head", "hair_cap", 0, hip + 24.0, -4.6, 9.4, 2.6, 9.2, "hair", paint="curl")
    C("head", "hair_back", 0, hip + 18.4, 3.4, 9.2, 7.0, 2.4, "hair", paint="curl")
    curls = ((-4.8, 22.8, -3.2, 3.2), (4.8, 22.8, -3.2, 3.2), (-2.2, 25.0, -4.4, 3.4), (2.4, 25.4, -4.2, 3.6), (0, 25.4, -2.6, 3.2), (-5.0, 19.8, 0.2, 3.0), (5.0, 19.8, 0.2, 3.0),
             (-3.2, 22.0, 3.0, 3.2), (3.4, 22.2, 3.0, 3.2), (-5.2, 22.0, -0.4, 3.0), (5.2, 22.0, -0.4, 3.0))
    for i, (cx, cy, cz, cs) in enumerate(curls):
        C("head", "curl_%d" % i, cx, hip + cy, cz - cs / 2.0, cs, cs, cs, "hair", paint="curl", rot=(i * 11 % 25 - 12, i * 17 % 40 - 20, i * 7 % 20 - 10))
    C("head", "gem_band", 0, hip + 21.4, -4.9, 8.8, 0.7, 0.7, "gold")
    bone("halo", "head", (0, hip + 30.5, 0))
    for i in range(10):
        a = i * math.pi / 5
        C("halo", "halo_%d" % i, 7.4 * math.cos(a), hip + 30.0, 7.4 * math.sin(a) - 0.6, 4.7, 0.9, 1.2, "gem", rot=(0, -math.degrees(a) + 90, 0))

    def arms(sd, n):
        bone("arm_" + n, "chest", (-6.6, hip + 13, 0), side=sd)
        Cx("arm_" + n, "upper_" + n, -6.6 - 2.2, hip + 6.6, -2.2, 4.4, 6.6, 4.4, "skin", side=sd)
        Cx("arm_" + n, "bracer_a_" + n, -6.6 - 2.6, hip + 9.0, -2.6, 5.2, 2.2, 5.2, "gold", paint="bracer", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-6.6, hip + 6.6, 0), side=sd)
        Cx("arm_%s_low" % n, "fore_" + n, -6.6 - 2.0, hip - 1.0, -2.0, 4.0, 7.6, 4.0, "skin", side=sd)
        Cx("arm_%s_low" % n, "bracer_b_" + n, -6.6 - 2.5, hip + 0.8, -2.5, 5.0, 4.4, 5.0, "gold", paint="bracer", side=sd)
        Cx("arm_%s_low" % n, "hand_" + n, -6.6 - 1.6, hip - 3.8, -1.8, 3.2, 3.0, 3.6, "skin", side=sd)
    pair(arms)

    def wings(sd, n):
        bone("wing_" + n, "chest", (-3, hip + 13, 3.4), side=sd)
        Cx("wing_" + n, "wing_bone_" + n, -3 - 11, hip + 15.6, 3.0, 11, 2.2, 1.6, "robe", side=sd)
        rows = ((24, hip + 12.6, -4, 0.0), (26, hip + 9.6, -12, 0.35), (24, hip + 6.6, -22, 0.7), (20, hip + 3.8, -32, 1.05), (14, hip + 1.4, -44, 1.4))
        for i, (ln, yy, rz, zz) in enumerate(rows):
            Cx("wing_" + n, "feather_%d_%s" % (i, n), -3 - ln, yy, 3.4 + zz, ln, 3.4, 0.9, "feather", paint="feather", rot=(0, 0, rz), pv=(-3, yy + 1.7, 3.8 + zz), side=sd)
        covs = ((12, hip + 15.2, -2, 0.2), (14, hip + 12.2, -8, 0.45), (13, hip + 9.4, -16, 0.8))
        for i, (ln, yy, rz, zz) in enumerate(covs):
            Cx("wing_" + n, "covert_%d_%s" % (i, n), -3 - ln, yy, 2.5 + zz, ln, 3.0, 0.8, "feather2", paint="feather", rot=(0, 0, rz), pv=(-3, yy + 1.5, 2.9 + zz), side=sd)
    pair(wings)
    top = hip + 33
    pal = dict(m=dict(robe=("#f2eee8", "cloth"), robe2=("#e8e2d8", "cloth"), gold=("#e6bd4e", "plate"), dark=("#17171d", "plate"), skin=("#f0d4b8", "porcelain"), hair=("#dcbd78", "hair"),
                      feather=("#f8f4ec", "feather"), feather2=("#ece6da", "feather"), gem=("#fff0a0", "glow")), eye="#82b4d8", glow="#fff0a0", hair_hi="#f4e0a0", hair_lo="#9a7a3c")
    c = humanoid_clips(wings=True, hover=True)
    c.rot("idle", "halo", lambda t: (0, 20 * t, 0))
    c.rot("walk", "halo", lambda t: (0, 90 * t, 0))
    c.rot("ability", "halo", lambda t: (0, 180 * t, 0))
    return pal, c, top, 5.0
