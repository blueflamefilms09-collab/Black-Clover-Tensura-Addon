"""Spirit Dive remake: the new pieces of the remade dive and the character tier, one GeoLite model per piece (key `spiritdive`, files spiritdive_arm_<name>), plus the sprites and body overlays.

    python3 -B tools/gen_spirit_dive_v2.py                 # everything
    python3 -B tools/gen_spirit_dive_v2.py wind fire       # only some parts: wind fire water earth sprites veins

What is made (the old pieces of tools/gen_spirit_dive_geo.py stay as they are; these are appended):

  WIND  (Yuno's character tier)   wind_halo (the crown halo: a turning ring with leaf prongs), wind_blades_l / _r (the wing-like wind blades, four swept blades a side), wind_hair (hair streaming in
                                  the wind).
  FIRE  (Fuegoleon's character tier) fire_mane (the flame mane round the head), fire_cape (a cape of flame from the shoulders), flame_arm_r (the molten right arm of the Salamander contract: also the
                                  persistent arm of Fuegoleon).
  WATER (Noelle's character tier) water_ribbon_l / _r (long water ribbons from the shoulders), water_rings (a gyroscope halo of three water rings), water_train (the flowing train of the dress).
  EARTH (every earth dive)        earth_crown (a crown of stone prongs and floating amber crystals).
  sprites                         textures/aura/sdv_*.png: the light column, the entry pillar and shockwave, the ground sigils, the rune ring, the flame sheet, the wisp ribbon.
  veins                           textures/aura/spirit2_<element>_veins.png: an additive vein layer over the body (stream lines, lava, currents, crystal veins), 64 x 64 player layout.

The painter is client/aura/SpiritDiveAura.java with client/aura/SpiritDiveFx.java; the piece lists are book/SpiritDivePieces. Frame of the models: model pixels = 1/16 block, y UP, front on -z, +x the
model's left (the player's right arm is at x = -5).
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import (FnMat, Piece, Anim, Mat, mbox, stepped_spike, sine_rot, loop_rot, spin_y, rgb, TAU)  # noqa: E402
from sdv_lib import (mask_from_polys, blur, rim_of, fbm2, seed_of, blade_polys, tongue_polys, ribbon_polys, leaf_polys, tip_right, tip_left, tip_down, rect_poly, place, clamp01, smooth, mix,  # noqa: E402
                     save_rgba, premult, radial)
import sdv_sprites  # noqa: E402

KEY = "spiritdive"


# ====================================================================================================================== the energy material
def energy_face(w, h, polys, pal, seed, *, flip=False, along="x", streaks=5.0, body_alpha=0.55, rim_r=1.4, glow_k=1.0, core=0.0, rim_alpha=0.96):
    """One face of an energy piece (blade, ribbon, tongue): the shape in `polys` (0..1 face coordinates), a body of streaks, a bright rim, an emissive map.

    pal = (dark, mid, light, glow) hex colours. `along` is the direction the streaks run ('x' along the face, 'y' down it). `core` lights the middle of the shape (flames: 0.7).
    """
    rng = np.random.default_rng(seed)
    a = mask_from_polys(w, h, polys)
    if flip:
        a = a[:, ::-1]
    dark, mid, light, glow = (rgb(c) for c in pal)
    inner = blur(a, max(1.0, rim_r * 1.6))
    rim = clamp01((a - inner) * 2.4) * a
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (x + 0.5) / w, (y + 0.5) / h
    n = fbm2(rng, w, h, 3, 3)
    if along == "x":
        s = 0.5 + 0.5 * np.sin(v * math.pi * streaks * 2.0 + n * 5.0 + u * 2.0)
        s2 = 0.5 + 0.5 * np.sin(v * math.pi * streaks * 5.3 + n * 9.0 - u * 3.0)
    else:
        s = 0.5 + 0.5 * np.sin(u * math.pi * streaks * 2.0 + n * 5.0 + v * 2.0)
        s2 = 0.5 + 0.5 * np.sin(u * math.pi * streaks * 5.3 + n * 9.0 - v * 3.0)
    t = clamp01(0.30 + 0.38 * s + 0.20 * s2 + (n - 0.5) * 0.3 + core * inner)
    col = mix(dark, mid, clamp01(t * 1.6))
    col = np.where((t > 0.62)[..., None], mix(mid, light, clamp01((t - 0.62) * 2.6)), col)
    col = col * (1 - rim[..., None]) + light[None, None, :] * rim[..., None]
    img = np.zeros((h, w, 4), np.float32)
    img[..., :3] = col
    img[..., 3] = a * (body_alpha + (1.0 - body_alpha) * np.maximum(rim * rim_alpha, core * inner)) * 255.0
    g = np.zeros((h, w, 4), np.float32)
    ga = clamp01((rim * 1.15 + s2 * 0.18 * a + core * inner * 0.6) * glow_k) * a
    g[..., :3] = glow[None, None, :] * ga[..., None]
    g[..., 3] = np.where(ga > 0.04, 255.0, 0.0)
    return img, g


def shape_mat(polys_fn, pal, name, *, flip_south=True, faces=("north", "south"), **kw):
    """A material that draws one shape on two opposite faces (north and south by default; mirrored on the back one so it reads the same from behind) and is clear on every other face."""
    def fn(face, w, h, tag):
        if face not in faces:
            return np.zeros((h, w, 4), np.float32), np.zeros((h, w, 4), np.float32)
        polys = polys_fn(tag)
        return energy_face(w, h, polys, pal, seed_of(name, tag, face), flip=(face == faces[1] and flip_south), **kw)
    return FnMat(fn, name)


def disc_face(w, h, pal, seed, *, rings=(), spikes=0, spike_len=0.0, spin=0.0, alpha=0.9, glow_k=1.0):
    """A flat top / bottom face of a halo plate (a w x h square seen from above): concentric rings (radius 0..1 of the half size, width) and optional crown spikes."""
    rng = np.random.default_rng(seed)
    dark, mid, light, glow = (rgb(c) for c in pal)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    dx, dy = (x - cx) / (w / 2.0), (y - cy) / (h / 2.0)
    r = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    cover = np.zeros((h, w), np.float32)
    bright = np.zeros((h, w), np.float32)
    for (r0, wd, k) in rings:
        d = np.abs(r - r0) / max(1e-4, wd)
        c = clamp01(1.4 - d)
        cover = np.maximum(cover, c * k)
        bright = np.maximum(bright, clamp01(1.0 - d * 1.4) * k)
    if spikes:
        s = np.abs(((ang * spikes / TAU + spin) % 1.0) - 0.5) * 2.0                        # 0 at a spike axis, 1 between
        prof = clamp01(1.0 - s / np.maximum(0.05, (1.0 - (r - rings[0][0]) / max(1e-3, spike_len)) * 0.9 + 0.05))
        sp = prof * (r > rings[0][0]) * (r < rings[0][0] + spike_len)
        cover = np.maximum(cover, sp)
        bright = np.maximum(bright, sp * 0.7)
    n = fbm2(rng, w, h, 4, 4)
    img = np.zeros((h, w, 4), np.float32)
    col = mix(dark, mid, clamp01(0.4 + 0.6 * n))
    col = col * (1 - bright[..., None]) + light[None, None, :] * bright[..., None]
    img[..., :3] = col
    img[..., 3] = cover * alpha * 255.0
    g = np.zeros((h, w, 4), np.float32)
    ga = clamp01(bright * 1.1 * glow_k) * cover
    g[..., :3] = glow[None, None, :] * ga[..., None]
    g[..., 3] = np.where(ga > 0.04, 255.0, 0.0)
    return img, g


def plate_mat(pal, name, **kw):
    """A flat plate (an 'up' / 'down' decal): the disc / ring drawing on the top and bottom faces, clear on the sides."""
    def fn(face, w, h, tag):
        if face not in ("up", "down"):
            return np.zeros((h, w, 4), np.float32), np.zeros((h, w, 4), np.float32)
        return disc_face(w, h, pal, seed_of(name, tag, face), **kw)
    return FnMat(fn, name)


# ====================================================================================================================== palettes (dark, mid, light, glow)
PAL_WIND = ("3AB88A", "8CF2C4", "F2FFF8", "9CFFD6")                 # the standard wind dive
PAL_YUNO = ("2AA878", "7CF8B8", "FFFFE8", "B8FFD8")                 # Yuno: green with a gold-white heart
PAL_YUNO_GOLD = ("A08A30", "F0DC78", "FFFFE0", "FFF0A0")
PAL_FIRE = ("A01408", "FF6A1C", "FFF0A0", "FF9A30")                 # the standard fire dive
PAL_FUEGO = ("8A0C10", "F03A18", "FFE070", "FF7A2A")                # Fuegoleon: crimson with a gold-white heart
PAL_FUEGO_GOLD = ("A8741A", "FFC040", "FFF4C0", "FFD060")
PAL_WATER = ("2C78D0", "6EBEF4", "F2FCFF", "A8F4FF")
PAL_NOELLE = ("2A62C8", "62B8FF", "F4F8FF", "B8E8FF")              # Noelle: blue with a silver-white heart
PAL_NOELLE_SILVER = ("8A98B8", "D4E0F4", "FFFFFF", "E8F4FF")


def lr(side):
    return "l" if side else "r"


def made(m, out):
    out.append(m)
    return m


# ====================================================================================================================== WIND (Yuno)
def blades(side, out):
    """The wing-like wind blades: four swept blades a side fanned from the upper back, long, translucent, bright rims and a gold-white heart."""
    n = "wind_blades_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=4)
    a = Anim()
    a.clip("idle", length=3.2, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 2.2, 22.5, 2.8)
    p.bone("w", P, parent="root")
    sine_rot(a, "idle", "w", 3.2, 2, -7.0 * sg, 0.0)
    sine_rot(a, "idle", "w", 3.2, 1, 5.0 * sg, 0.18)
    defs = [(-80, 38.0, 9.5, 24, 0.24), (-54, 33.0, 8.8, 28, 0.18), (-28, 27.0, 8.0, 32, 0.12)]
    for i, (rz, L, wd, yaw, curl) in enumerate(defs):
        b = "b%d" % i
        p.bone(b, P, parent="w", rotation=(0, -sg * (yaw + i * 3), -sg * rz))
        pal = PAL_YUNO if i % 2 == 0 else PAL_WIND
        mat = shape_mat(lambda tag, c=curl: blade_polys(curl=c, belly=0.55), pal, "windblade%d" % i, streaks=4.0 + i, body_alpha=0.40, core=0.5 if i == 0 else 0.3, glow_k=0.7)
        o = (P[0] - L, P[1] - wd / 2, P[2]) if side == 0 else (P[0], P[1] - wd / 2, P[2])
        p.box(b, o, (L, wd, 0.4), mat)
        sine_rot(a, "idle", b, 3.2, 2, 5.5 * sg, 0.2 + i * 0.07)
    p.save(a)
    return made(p, out)


def wind_halo(out):
    """The crown halo: a ring plate with crown rays and a turning ring of leaf prongs standing on it, above the head."""
    p = Piece(KEY, "arm_wind_halo", density=4)
    a = Anim()
    a.clip("idle", length=12.0, loop=True)
    Y = 38.2
    p.bone("spin", (0, Y, 0), parent="root")
    ring = plate_mat(PAL_YUNO, "windhalo_ring", rings=[(0.78, 0.07, 1.0), (0.64, 0.035, 0.7), (0.9, 0.03, 0.6)], spikes=18, spike_len=0.1, alpha=0.95, glow_k=1.2)
    p.box("spin", (-10.0, Y, -10.0), (20.0, 0.5, 20.0), ring, d=4)
    n = 9
    for i in range(n):
        ang = i * 360.0 / n
        rad = 7.6
        cx, cz = rad * math.sin(math.radians(ang)), rad * math.cos(math.radians(ang))
        hgt = 9.0 if i % 3 == 0 else 6.2
        leaf = shape_mat(lambda tag: leaf_polys(0.95), PAL_YUNO if i % 3 == 0 else PAL_YUNO_GOLD, "haloprong%d" % (i % 3), along="y", streaks=3.0, body_alpha=0.5, core=0.6)
        p.box("spin", (cx - 1.5, Y + 0.3, cz - 0.25), (3.0, hgt, 0.5), leaf, rot=(0, ang, 0), pivot=(cx, Y + 0.3, cz))
    a.rot("idle", "spin", {12.0 * i / 8: (0, 360.0 * i / 8, 0) for i in range(9)})
    a.pos("idle", "spin", {0.0: (0, 0, 0), 3.0: (0, 0.8, 0), 6.0: (0, 0, 0), 9.0: (0, 0.8, 0), 12.0: (0, 0, 0)})
    p.save(a)
    return made(p, out)


def wind_hair(out):
    """Hair streaming back in the wind: seven glowing locks hanging from the back of the head, each waving with its own phase."""
    p = Piece(KEY, "arm_wind_hair", density=4)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    p.bone("hair", (0, 31.0, 4.0), parent="root")
    xs = [-3.6, -2.4, -1.2, 0.0, 1.2, 2.4, 3.6]
    for i, x in enumerate(xs):
        b = "k%d" % i
        L = 17.0 - abs(x) * 1.2 - (i % 2) * 2.0
        p.bone(b, (x, 31.5, 4.4), parent="hair", rotation=(14 + abs(x) * 2.5, x * 3.0, 0))
        mat = shape_mat(lambda tag, ph=i * 0.9: ribbon_polys(wave=0.18, phase=ph, width=0.9, taper=0.7), PAL_YUNO if i % 2 == 0 else PAL_WIND, "windhair%d" % (i % 2), along="y", streaks=2.0, body_alpha=0.5, core=0.4)
        p.box(b, (x - 0.9, 31.5 - L, 4.4), (1.8, L, 0.3), mat)
        sine_rot(a, "idle", b, 2.6, 0, 9.0, 0.11 * i)
        sine_rot(a, "idle", b, 2.6, 2, 5.0, 0.07 * i + 0.3)
    p.save(a)
    return made(p, out)


def wind(out):
    for s in (0, 1):
        blades(s, out)
    wind_halo(out)
    wind_hair(out)


# ====================================================================================================================== FIRE (Fuegoleon)
def molten_mat(heat, name, crust=0.5):
    """A living flame body: a bright orange-yellow skin (hotter toward the fist, `heat` 0..1) cut by thin dark red cracks and swirled with lighter currents, glowing everywhere. Not rock: the arm of
    the series is fire given the shape of an arm."""
    def fn(face, w, h, tag):
        rng = np.random.default_rng(seed_of(name, tag, face))
        n = fbm2(rng, w, h, max(2, w // 6), max(2, h // 6), 3)
        m = fbm2(rng, w, h, max(2, w // 4), max(2, h // 9), 2)
        crack = np.maximum(clamp01(1.0 - np.abs(n - 0.5) / 0.045), clamp01(1.0 - np.abs(m - 0.5) / 0.035) * 0.8) * crust
        flow = clamp01(0.30 + 0.9 * m + heat * 0.35)
        body = mix(rgb("C82008"), rgb("FF8A1C"), flow)
        body = body + (rgb("FFF0A0") - body) * clamp01((flow - 0.78) * 3.0)[..., None]
        dark = rgb("5A1006")
        k = clamp01(crack * 1.2)
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = body * (1 - k[..., None]) + dark[None, None, :] * k[..., None]
        img[..., 3] = 255.0
        g = np.zeros((h, w, 4), np.float32)
        ga = clamp01((0.55 + 0.35 * heat) * (1.0 - k * 0.85))
        g[..., :3] = mix(rgb("FF5A14"), rgb("FFC050"), clamp01(flow))[..., :] * ga[..., None]
        g[..., 3] = 255.0
        return img, g
    return FnMat(fn, name)


def flame_arm(out):
    """The molten right arm: a shoulder flare, an upper arm, an elbow ring, a forearm and a fist of lava with cracks, and flame tongues rising and trailing from the shoulder, the elbow and the fist."""
    p = Piece(KEY, "arm_flame_arm_r", density=4)
    a = Anim()
    a.clip("idle", length=1.6, loop=True)
    M0, M1, M2, M3 = molten_mat(0.15, "arm0"), molten_mat(0.35, "arm1"), molten_mat(0.6, "arm2"), molten_mat(0.9, "arm3")
    p.box("root", (-11.0, 20.6, -3.9), (8.0, 4.6, 7.8), M0)                                  # the shoulder flare
    p.box("root", (-9.7, 15.6, -3.3), (6.6, 5.4, 6.6), M1)                                    # the upper arm
    p.box("root", (-10.0, 14.2, -3.6), (7.2, 2.0, 7.2), M0)                                   # the elbow ring (dark crust)
    p.box("root", (-9.5, 9.6, -3.2), (6.4, 5.0, 6.4), M2)                                     # the forearm
    p.box("root", (-9.9, 6.9, -3.6), (7.2, 3.4, 7.2), M3)                                     # the fist
    for k in range(4):                                                                         # the knuckles
        p.box("root", (-9.6 + k * 1.75, 6.4, -4.4), (1.7, 1.6, 1.3), M3, d=3)
    tg = PAL_FUEGO

    def tongue(name, pivot, origin, size, rot, phase, amp=9.0, faces=("north", "south"), down=False, seed=0):
        p.bone(name, pivot, parent="root", rotation=rot)
        mat = shape_mat(lambda tag, sd=seed, dn=down: (tip_down if dn else (lambda q: q))(tongue_polys(sway=0.2, phase=sd * 1.9, base=0.9, wob=0.08)), tg, "armtongue%d" % (seed % 3),
                        along="y", streaks=2.5, body_alpha=0.55, core=0.75, faces=faces)
        p.box(name, origin, size, mat)
        sine_rot(a, "idle", name, 1.6, 2, amp, phase)
        sine_rot(a, "idle", name, 1.6, 0, amp * 0.6, phase + 0.25)
        return name

    tongue("s0", (-9.0, 24.0, 0.0), (-12.2, 24.0, -0.2), (7.0, 13.0, 0.4), (0, 0, 14), 0.00, 8.0, seed=1)            # up from the shoulder
    tongue("s1", (-7.0, 24.0, 2.0), (-9.4, 24.0, 1.8), (6.0, 11.0, 0.4), (-26, 0, -8), 0.18, 8.0, seed=2)            # up and back
    tongue("e0", (-10.0, 18.0, 0.0), (-13.6, 15.5, -0.2), (6.0, 9.5, 0.4), (0, 0, 55), 0.33, 10.0, seed=3)            # from the elbow, out to the side
    tongue("e1", (-6.0, 16.0, 3.4), (-6.2, 12.0, 3.2), (0.4, 9.0, 6.0), (-28, 0, 0), 0.5, 8.0, faces=("west", "east"), seed=4)   # trailing back from the elbow
    tongue("w0", (-9.0, 12.0, 0.0), (-12.4, 10.0, -0.2), (6.0, 9.0, 0.4), (0, 0, 40), 0.66, 10.0, seed=5)             # from the wrist
    tongue("f0", (-6.0, 7.5, 0.0), (-9.0, 1.2, -0.2), (6.0, 8.0, 0.4), (0, 0, 0), 0.8, 9.0, down=True, seed=6)         # falling from the fist
    tongue("f1", (-6.0, 7.5, 0.0), (-6.2, 1.0, -3.0), (0.4, 8.0, 6.0), (0, 0, 0), 0.9, 9.0, faces=("west", "east"), down=True, seed=7)
    p.save(a)
    return made(p, out)


def fire_mane(out):
    """The flame mane: two fans of flame tongues (a left and a right one) and three upright ones, from behind the head, swaying in the heat."""
    p = Piece(KEY, "arm_fire_mane", density=4)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    P = (0.0, 30.5, 3.6)
    p.bone("m", P, parent="root")
    sine_rot(a, "idle", "m", 2.0, 0, 3.0, 0.0)
    spec = [(-60, 11.0, 4.6), (-38, 13.0, 5.0), (-16, 14.0, 5.2), (6, 13.0, 5.0), (28, 11.0, 4.6)]
    for side in (0, 1):
        sg = 1.0 if side else -1.0
        for i, (rz, L, wd) in enumerate(spec):
            b = "t%d_%d" % (side, i)
            p.bone(b, P, parent="m", rotation=(0, -sg * (58 + i * 3), -sg * rz))
            shape = tip_right if side else tip_left
            mat = shape_mat(lambda tag, sh=shape, ph=i * 1.3 + side: sh(tongue_polys(sway=0.22, phase=ph, base=0.95, wob=0.09)), PAL_FUEGO if (i + side) % 2 == 0 else PAL_FIRE, "manetongue%d" % ((i + side) % 2),
                            streaks=2.4, body_alpha=0.52, core=0.55, glow_k=0.55, flip_south=False)
            o = (P[0], P[1] - wd / 2, P[2]) if side else (P[0] - L, P[1] - wd / 2, P[2])
            p.box(b, o, (L, wd, 0.4), mat)
            sine_rot(a, "idle", b, 2.0, 2, 7.0 * sg, 0.1 * i + 0.3 * side)
            sine_rot(a, "idle", b, 2.0, 1, 5.0 * sg, 0.16 * i)
    p.save(a)
    return made(p, out)


def fire_cape(out):
    """A cape of flame: three panels hanging from the shoulders behind the back, a crimson cloak with a gold trim that burns into tongues at the hem."""
    p = Piece(KEY, "arm_fire_cape", density=4)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    TOP = 24.0
    defs = [(0.0, 0.0, 10.0, 25.0), (-5.6, 22, 8.0, 21.0), (5.6, -22, 8.0, 21.0)]
    for i, (x, yaw, wd, L) in enumerate(defs):
        b = "c%d" % i
        p.bone(b, (x, TOP, 3.0), parent="root", rotation=(6, yaw, 0))

        def polys(tag, wd=wd):
            ps = [rect_poly(0.0, 0.0, 1.0, 0.30)]
            for k in range(4):
                ps += place(tip_down(tongue_polys(sway=0.2, phase=k * 2.3 + i, base=0.95, wob=0.07)), k * 0.25 + 0.01, 0.26, k * 0.25 + 0.24, 1.0)
            return ps
        mat = shape_mat(polys, PAL_FUEGO, "fcape%d" % i, along="y", streaks=3.0, body_alpha=0.6, core=0.45)
        p.box(b, (x - wd / 2, TOP - L, 3.0), (wd, L, 0.4), mat)
        sine_rot(a, "idle", b, 2.4, 0, 7.0, 0.12 * i)
        sine_rot(a, "idle", b, 2.4, 2, 4.0, 0.2 * i + 0.3)
    p.save(a)
    return made(p, out)


def fire(out):
    flame_arm(out)
    fire_mane(out)
    fire_cape(out)


# ====================================================================================================================== WATER (Noelle)
def water_ribbon(side, out):
    n = "water_ribbon_" + lr(side)
    p = Piece(KEY, "arm_" + n, density=4)
    a = Anim()
    a.clip("idle", length=2.8, loop=True)
    sg = 1.0 if side else -1.0
    for r, (x0, z0, W) in enumerate(((sg * 3.0, 3.0, 4.6), (sg * 5.2, 2.4, 3.6))):
        parent = "root"
        y = 22.0 - r * 1.2
        for k in range(4):
            b = "r%d_%d" % (r, k)
            p.bone(b, (x0, y, z0), parent=parent, rotation=(8, sg * 6, sg * 5) if k == 0 else (0, 0, 0))
            L = 7.0
            wd = W * (1.0 - 0.17 * k)
            mat = shape_mat(lambda tag, ph=r * 1.7 + k * 0.9, kk=k: ribbon_polys(wave=0.10, phase=ph, width=0.92, taper=0.35 if kk == 3 else 0.12), PAL_NOELLE if (r + k) % 2 == 0 else PAL_WATER,
                            "wribbon%d" % ((r + k) % 2), along="y", streaks=2.2, body_alpha=0.55, core=0.5)
            p.box(b, (x0 - wd / 2, y - L, z0), (wd, L, 0.3), mat)
            sine_rot(a, "idle", b, 2.8, 0, 13.0, -0.11 * k + 0.2 * r)
            sine_rot(a, "idle", b, 2.8, 2, 8.0 * sg, -0.09 * k + 0.4 * r)
            parent = b
            y -= L - 0.3
    p.save(a)
    return made(p, out)


def water_rings(out):
    """A gyroscope halo above the head: three tilted rings of water that turn."""
    p = Piece(KEY, "arm_water_rings", density=4)
    a = Anim()
    a.clip("idle", length=9.0, loop=True)
    Y = 37.0
    ring = plate_mat(PAL_NOELLE, "wring", rings=[(0.80, 0.065, 1.0), (0.66, 0.03, 0.65), (0.92, 0.03, 0.55)], alpha=0.92, glow_k=1.2)
    for i in range(3):
        b = "g%d" % i
        p.bone(b, (0, Y, 0), parent="root", rotation=(0, i * 60.0, 62.0))
        p.box(b, (-9.0, Y, -9.0), (18.0, 0.4, 18.0), ring, d=4)
        a.rot("idle", b, {9.0 * j / 8: (0, i * 60.0 + (360.0 * j / 8) * (1 if i % 2 == 0 else -1), 62.0) for j in range(9)})
    p.save(a)
    return made(p, out)


def water_train(out):
    """The train of the dress: three sheets of water falling from the waist behind the legs, with a wavy hem and drops."""
    p = Piece(KEY, "arm_water_train", density=4)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    defs = [(0.0, 0.0, 11.0, 22.0), (-5.0, 24, 8.0, 18.0), (5.0, -24, 8.0, 18.0)]
    for i, (x, yaw, wd, L) in enumerate(defs):
        b = "t%d" % i
        p.bone(b, (x, 13.0, 3.4), parent="root", rotation=(10, yaw, 0))
        mat = shape_mat(lambda tag, ph=i * 1.3: ribbon_polys(wave=0.07, phase=ph, width=0.98, taper=0.1), PAL_NOELLE if i == 0 else PAL_WATER, "wtrain%d" % (i % 2), along="y", streaks=2.5,
                        body_alpha=0.55, core=0.45)
        p.box(b, (x - wd / 2, 13.0 - L, 3.4), (wd, L, 0.3), mat)
        sine_rot(a, "idle", b, 2.6, 0, 8.0, 0.13 * i)
        sine_rot(a, "idle", b, 2.6, 2, 4.0, 0.3 * i)
    p.save(a)
    return made(p, out)


def water(out):
    for s in (0, 1):
        water_ribbon(s, out)
    water_rings(out)
    water_train(out)


# ====================================================================================================================== EARTH
def earth_crown(out):
    """A crown of stone prongs round the brow with five amber crystals floating and turning above it."""
    from gen_spirit_dive_geo import stone, crystal_amber
    p = Piece(KEY, "arm_earth_crown", density=3)
    a = Anim()
    a.clip("idle", length=10.0, loop=True)
    p.box("root", (-5.2, 31.0, -5.2), (10.4, 1.8, 10.4), stone(80, moss=0.4))                             # the band
    for i in range(8):
        ang = i * TAU / 8
        cx, cz = 4.6 * math.cos(ang), 4.6 * math.sin(ang)
        hh = 5.0 if i % 2 == 0 else 3.4
        stepped_spike(p, "root", (cx, 32.4, cz), "+y", hh, 2.4 if i % 2 == 0 else 1.9, stone(81 + i, moss=0.5), steps=3, thick=1.8)
    p.bone("orb", (0, 37.0, 0), parent="root")
    for i in range(5):
        ang = i * TAU / 5
        r = 6.4
        cx, cz = r * math.cos(ang), r * math.sin(ang)
        s = 2.2 + (i % 2) * 0.8
        p.box("orb", (cx - s / 2, 37.0 + 1.2 * (i % 2) - s / 2, cz - s / 2), (s, s * 1.5, s), crystal_amber(), rot=(0, 30 * i, 20), pivot=(cx, 37.0, cz))
    spin_y(a, "idle", "orb", 10.0, turns=1, steps=8)
    a.pos("idle", "orb", {0.0: (0, 0, 0), 2.5: (0, 0.9, 0), 5.0: (0, 0, 0), 7.5: (0, 0.9, 0), 10.0: (0, 0, 0)})
    p.save(a)
    return made(p, out)


def earth(out):
    earth_crown(out)


def sprites(out):
    sdv_sprites.sprites()
    print("sprites written")


def veins(out):
    sdv_sprites.veins()
    print("veins written")


# ====================================================================================================================== main
PARTS = {"wind": wind, "fire": fire, "water": water, "earth": earth, "sprites": sprites, "veins": veins}


def main(argv):
    out = []
    want = argv or list(PARTS)
    for k in want:
        if k not in PARTS:
            sys.exit("unknown part %r (one of %s)" % (k, ", ".join(PARTS)))
        PARTS[k](out)
    total = 0
    for m in out:
        print("%-36s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("models", len(out), "total cubes", total)


if __name__ == "__main__":
    main(sys.argv[1:])
