"""1.3.0: the 3D in-hand models of the Brooch Pin and the Hand of Providence.

This file is not run on its own: tools/gen_weapon_models.py executes it in its own namespace (one line before `def mirror_x`), so np, math,
Image, Mesh, Atlas, Blade, box, plate, ascii_mask, col, shade, mix, nz, fz, hsh, wrap_paint, metal_paint, lerp, SPECS, REAL, GRIP and
sprite_axis / sprite_box all exist here.

    python3 -B tools/gen_pin_providence_icons.py                                (the inventory icons and item models, first)
    python3 -B tools/gen_weapon_models.py brooch_pin hand_of_providence          (then: python3 -B tools/item_preview/preview_weapons.py brooch_pin hand_of_providence)

Model units are pixels: y runs along the weapon, x across it, z through it.
  brooch_pin           a long steel pin (a needle blade) through a gold filigree brooch with a ruby cabochon in a collar of ivory teeth (the guard),
                       a wound crimson grip and a ruby pommel. The ruby glows.
  hand_of_providence   a golden gauntlet closed in a fist, the fist at the top of the axis and the vambrace running down it: a flared cuff, a dark
                       wrist band, the back of the hand with a raised blue cross that glows, four finger plates curled over, the thumb, and a
                       halo ring floating round the fist.
"""


# ================================================================ helpers
def _pp_mask_ellipse(w, h, cx, cy, rx, ry, tilt=0.0):
    """A boolean h x w mask (row 0 on top), an ellipse centred on (cx, cy) in pixels (y down), turned by 'tilt' radians."""
    yy, xx = np.mgrid[0:h, 0:w] + 0.5
    dx, dy = xx - cx, yy - cy
    ca, sa = math.cos(tilt), math.sin(tilt)
    u, v = dx * ca + dy * sa, -dx * sa + dy * ca
    return (u / rx) ** 2 + (v / ry) ** 2 <= 1.0


def _pp_edge(mask):
    """The pixels of a mask that touch an empty one (or the border) by a side: (up, down, left, right) flags."""
    m = np.pad(mask, 1, constant_values=False)
    up = mask & ~m[:-2, 1:-1]
    down = mask & ~m[2:, 1:-1]
    left = mask & ~m[1:-1, :-2]
    right = mask & ~m[1:-1, 2:]
    return up, down, left, right


def _pp_dome(m, a, c, rx, ry, rz, tilt, sides, rings, paint, sign=1):
    """A faceted half ellipsoid (a cabochon): its flat base on the plane z = c[2], its pole at c[2] + sign * rz, the base an ellipse rx x ry turned by
    'tilt'. One flat texel per facet: paint(k, r, n) -> (rgba, glow), n the facet's outward unit normal (x, y, z)."""
    u0, v0 = a.alloc(sides, rings)
    ca, sa = math.cos(tilt), math.sin(tilt)

    def pt(k, r):
        phi = (math.pi / 2) * r / rings
        th = 2 * math.pi * (k % sides) / sides
        rr = math.cos(phi)
        x, y, z = rx * rr * math.cos(th), ry * rr * math.sin(th), rz * math.sin(phi) * sign
        return (c[0] + x * ca - y * sa, c[1] + x * sa + y * ca, c[2] + z)

    facets = {}
    for r in range(rings):
        for k in range(sides):
            quad = (pt(k, r), pt(k + 1, r), pt(k + 1, r + 1), pt(k, r + 1))
            ctr = np.mean([np.array(p) for p in quad], axis=0)
            out = ctr - np.array(c)
            n = out / max(1e-9, np.linalg.norm(out))
            facets[(k, r)] = (quad, tuple(out), n)
    for (k, r), (quad, out, n) in facets.items():
        uv = (u0 + k + 0.5, v0 + r + 0.5)
        m.quad(0, [tuple(p) + uv for p in quad], out)

    def f(lx, ly):
        k, r = int(lx), int(ly)
        return paint(k, r, facets[(k, r)][2])
    a.paint(u0, v0, sides, rings, f)


# ================================================================ the Brooch Pin
_PP_RUBY = (206, 22, 48)
_PP_RUBY_D = (104, 6, 28)
_PP_RUBY_L = (255, 138, 150)
_PP_GOLD = (214, 160, 56)
_PP_LIGHT = np.array((-0.45, 0.62, 0.65))
_PP_LIGHT = _PP_LIGHT / np.linalg.norm(_PP_LIGHT)
_PP_TILT = math.radians(22)                                                          # the stone leans to the upper right (image rows run down)


def _pp_gem_paint(base=_PP_RUBY, dark=_PP_RUBY_D, light=_PP_RUBY_L, glow_rgb=(255, 52, 84), glow_a=0.5):
    """A cut gem: every facet flat, lit from the upper left, a hard specular on the facets that face the light."""
    def p(k, r, n):
        lit = float(np.clip(np.dot(n, _PP_LIGHT), 0.0, 1.0))
        c = np.array(dark, np.float32) * (1 - lit) + np.array(base, np.float32) * lit
        spec = max(0.0, lit - 0.82) / 0.18
        c = c + (np.array(light, np.float32) - c) * (spec * 0.9)
        c = c + (hsh(k, r, 11) - 0.5) * 22                                           # each facet a little off its neighbours
        return col(np.clip(c, 0, 255)), col(glow_rgb, min(1.0, glow_a * (0.55 + 0.7 * lit)))
    return p


def _pp_gold_box(base=_PP_GOLD, rivets=False, salt=0):
    def p(face, i, j, fw, fh):
        c = np.array(base, np.float32) * (1.16 if face == 0 else 0.78 if face == 1 else 1.0) + fz(i * 1.9, j * 1.3, face + salt) * 24
        if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
            c = c * 1.18
        if rivets and face >= 2 and fw >= 3 and fh >= 3 and i == fw // 2 and j == fh // 2:
            c = c * 1.4
        return col(np.clip(c, 0, 255)), None
    return p


def _pp_brooch_frame(m, a):
    """The filigree frame: a rounded square whose rim swells into lobes, a teardrop hole for the ruby; its gold is chased with a ring of beads, a
    swirl of scrolls and a collar of teeth round the stone."""
    W, H = 21, 20
    cx, cy = W / 2, H / 2
    yy, xx = np.mgrid[0:H, 0:W] + 0.5
    dx, dy = (xx - cx) / 9.8, (yy - cy) / 9.2
    ang = np.arctan2(yy - cy, xx - cx)
    wob = 1.0 + 0.045 * np.sin(5 * ang + 1.0) + 0.035 * np.sin(9 * ang + 0.3)        # an organic, hand-worked rim
    outer = (np.abs(dx) ** 3.0 + np.abs(dy) ** 3.0) <= wob ** 3
    for k in range(8):                                                               # a lobe at each corner and edge middle
        a2 = k * math.pi / 4 + math.pi / 4
        rr = 1.0 if k % 2 == 0 else 0.93
        outer |= np.hypot(xx - (cx + math.cos(a2) * 9.4 * rr), yy - (cy + math.sin(a2) * 8.8 * rr)) <= (2.3 if k % 2 == 0 else 1.7)
    hole = _pp_mask_ellipse(W, H, cx, cy, 5.5, 6.6, _PP_TILT)
    ring = outer & ~hole
    up, down, left, right = _pp_edge(ring)
    collar = ring & _pp_mask_ellipse(W, H, cx, cy, 6.7, 7.8, _PP_TILT)
    ca, sa = math.cos(_PP_TILT), math.sin(_PP_TILT)

    def paint(i, j, front):
        x, y = i + 0.5 - cx, j + 0.5 - cy
        u, v = (x * ca + y * sa) / 5.5, (-x * sa + y * ca) / 6.6                     # the hole's own frame: rho 1 = the stone's edge
        rho = math.hypot(u, v)
        th = math.atan2(v, u)
        c = np.array(_PP_GOLD, np.float32) + fz(x * 2, y * 2, 32) * 16 + nz(x, y, 31) * 14
        g = None
        if collar[j, i]:
            c = np.array((250, 240, 212), np.float32) if (i + j) % 2 == 0 else np.array((104, 66, 24), np.float32)
        else:
            sw = abs(math.sin(th * 4.0 + rho * 5.5))
            if sw < 0.16 and 1.55 < rho < 2.4:
                c = np.array((96, 60, 22), np.float32)                                # a swirl of scroll grooves
            elif sw > 0.93 and 1.55 < rho < 2.4:
                c = np.array((255, 224, 130), np.float32)                             # and the raised ridge beside each
            if abs(rho - 1.5) < 0.12:
                c = np.array((255, 232, 150), np.float32)                             # a ring of beads
        if up[j, i] or left[j, i]:
            c = c * 1.2
        if down[j, i] or right[j, i]:
            c = c * 0.66
        return col(np.clip(c, 0, 255)), g
    # the frame's own plate: x from -10.5, y from -18.5 (the plate's row 0 is its top)
    plate(m, a, ring, -10.5, -18.5, 1.0, paint, "xy", 0.0)
    return cx, cy


def brooch_pin(m, a):
    """Brooch Pin: a long steel pin run through a ruby brooch of gold filigree, a collar of ivory teeth round the stone, a wound grip below."""
    # ---- the pin: a slim steel dagger blade, brushed, a bright ridge, the red of the stone spilling up its foot
    L = 38
    st = [(-3.0, -2.2, 0.0, 2.2), (7.0, -2.2, 0.0, 2.2), (18.0, -1.8, 0.0, 1.8), (29.0, -1.1, 0.0, 1.1), (L, 0.0, 0.0, 0.0)]

    def pin_paint(x, y, front, e, rd):
        if x is None:
            return col((150, 160, 180)), None
        c = np.array((132, 144, 166), np.float32) + fz(int(x * 2), int(y * 0.6), 21) * 30      # brushed steel
        if abs(x) < 0.45:
            c = np.array((226, 233, 246), np.float32)                                   # the bright ridge
        elif abs(x) > 1.25 and y < 22:
            c = c * 0.7                                                                 # a shadowed bevel either side of it
        elif abs(x) < 1.0:
            c = c * 1.18
        g = None
        if y < 7:
            g = col((255, 52, 84), 0.3 * (1.0 - max(0.0, y) / 7.0))                    # the ruby's light on the foot of the pin
        if y > 28 and abs(x) < 0.3:
            g = col((255, 210, 220), 0.4)                                               # a point of light at the tip
        if e < 0.8:
            c = np.array((246, 249, 255), np.float32)                                   # the honed edge
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.5, 1.15, pin_paint).build(m, a)
    # ---- the collar the pin rises from, a gold boss with a ring above the frame
    box(m, a, -2.8, -1.0, -1.7, 2.8, 2.4, 1.7, _pp_gold_box(rivets=True, salt=3))
    box(m, a, -2.0, 2.4, -1.3, 2.0, 3.6, 1.3, _pp_gold_box((240, 196, 96), salt=4))
    # ---- the brooch
    cx, cy = _pp_brooch_frame(m, a)
    ox, oy = -10.5 + cx, -18.5 + (20 - cy)                                           # the centre of the frame in model units
    tilt = -_PP_TILT                                                                 # the same lean in model space (y up)
    # the ruby: a faceted cabochon domed out of both faces of the frame, glowing
    _pp_dome(m, a, (ox, oy, 0.9), 5.9, 7.0, 3.6, tilt, 12, 4, _pp_gem_paint(glow_a=0.6), sign=1)
    _pp_dome(m, a, (ox, oy, -0.9), 5.9, 7.0, 2.7, tilt, 12, 3, _pp_gem_paint(glow_a=0.45), sign=-1)
    # the teeth of the collar standing up out of the frame's face, round the stone
    for k in range(16):
        ang = 2 * math.pi * k / 16
        ex, ey = 6.2 * math.cos(ang), 7.3 * math.sin(ang)
        px_, py_ = ex * math.cos(tilt) - ey * math.sin(tilt), ex * math.sin(tilt) + ey * math.cos(tilt)
        for (z0, z1) in ((0.9, 2.2), (-2.2, -0.9)):
            box(m, a, ox + px_ - 0.55, oy + py_ - 0.55, z0, ox + px_ + 0.55, oy + py_ + 0.55, z1, _pp_gold_box((250, 240, 214), salt=k))
    # ---- below the brooch: a gold ferrule, the wound grip, a ruby pommel in a gold cup
    box(m, a, -2.8, -21.8, -1.8, 2.8, -18.4, 1.8, _pp_gold_box(rivets=True, salt=9))
    box(m, a, -1.5, -30.6, -1.5, 1.5, -21.8, 1.5, wrap_paint((98, 24, 36), (226, 172, 62), diamonds=True))
    box(m, a, -2.8, -33.0, -2.3, 2.8, -30.6, 2.3, _pp_gold_box((230, 178, 66), salt=10))
    _pp_dome(m, a, (0.0, -31.8, 2.2), 2.0, 2.0, 1.7, 0.0, 8, 3, _pp_gem_paint(glow_a=0.7), sign=1)
    _pp_dome(m, a, (0.0, -31.8, -2.2), 2.0, 2.0, 1.7, 0.0, 8, 3, _pp_gem_paint(glow_a=0.7), sign=-1)
    return dict(glow=("FF3358", 0.1, 0.4, 1.0))


# ================================================================ the Hand of Providence
_PP_BLUE = (52, 138, 226)


def _pp_gold_shade(base, i, j, w, h, salt=0, streak=34):
    """Painted gold as on the sheet: vertical streaks, light from the upper left, a soft darkening to the lower right."""
    light = 1.0 + 0.22 * (0.5 - i / max(1, w - 1)) + 0.22 * (0.5 - j / max(1, h - 1))
    c = np.array(base, np.float32) * light + fz((i + 0.5) * 3.1, (j + 0.5) * 0.3, salt + 5) * streak + nz(i + 0.5, j + 0.5, salt + 6) * 12
    return c


def _pp_flare_mask(w, h, hw_top, hw_bot):
    """A flared silhouette: row 0 (top) is hw_top wide each side of the middle, the last row hw_bot."""
    m = np.zeros((h, w), bool)
    for j in range(h):
        hw = lerp(hw_top, hw_bot, (j / max(1, h - 1)) ** 1.6)
        for i in range(w):
            if abs(i + 0.5 - w / 2) <= hw:
                m[j, i] = True
    return m


def _pp_rect_round(w, h, r):
    """A w x h rectangle with its corners rounded to radius r."""
    m = np.ones((h, w), bool)
    for j in range(h):
        for i in range(w):
            di, dj = min(i, w - 1 - i), min(j, h - 1 - j)
            if di < r and dj < r and math.hypot(r - 0.5 - di, r - 0.5 - dj) > r:
                m[j, i] = False
    return m


def hand_of_providence(m, a):
    """Hand of Providence: a golden gauntlet closed in a fist. y runs up the arm: the flared vambrace below, a dark wrist band, the back of the hand
    with its raised blue cross, four curled finger plates and the thumb; a halo ring floats round the fist. The cross, the cuff's trim and the ring glow."""
    gold = (226, 172, 62)
    goldl = (252, 212, 112)
    # ---- the vambrace: a flared plate (wider at the rim), grooved, with a raised panel and lines of blue trim
    vw, vh = 17, 18                                                                  # x -8.5 .. 8.5, y -24 .. -6
    vmask = _pp_flare_mask(vw, vh, 5.8, 7.6)

    def vamb_paint(i, j, front):
        c = _pp_gold_shade(gold, i, j, vw, vh, 40)
        g = None
        if j in (4, 12):
            c = c * 0.58                                                               # two grooves round the arm
        if j in (5, 13):
            c = c * 1.18
        if j == 8 and 2 <= i <= vw - 3:
            c = np.array(_PP_BLUE, np.float32)                                         # a line of blue trim between them
            g = col((90, 190, 255), 0.85)
        if i < 2 or i > vw - 3:
            c = c * 0.7
        return col(np.clip(c, 0, 255)), g
    plate(m, a, vmask, -8.5, -24.0, 4.5, vamb_paint, "xy", 0.0)
    panel = _pp_rect_round(9, 12, 2)                                                 # a raised panel down the middle of the cuff, edged in blue

    def panel_paint(i, j, front):
        edge = (i in (0, 8) or j in (0, 11)) and not (i in (0, 8) and j in (0, 11))
        c = _pp_gold_shade(goldl, i, j, 9, 12, 48) * (0.62 if edge else 1.0)
        return col(np.clip(c, 0, 255)), (col((90, 190, 255), 0.6) if edge else None)
    plate(m, a, panel, -4.5, -22.5, 0.5, panel_paint, "xy", 5.0)
    rim = np.ones((2, 17), bool)
    plate(m, a, rim, -8.5, -24.8, 5.0, lambda i, j, f: (col(np.clip(_pp_gold_shade(goldl, i, j, 17, 2, 42, 20) * (0.82 if j == 1 else 1.1), 0, 255)),
                                                        col((255, 220, 140), 0.25) if j == 0 and i % 4 == 1 else None), "xy", 0.0)
    # ---- the wrist: dark leather between the vambrace and the hand
    def leather(face, i, j, fw, fh):
        c = np.array((78, 42, 34), np.float32) + fz(i * 2.3, j * 1.7, 43) * 16
        if face >= 2 and j % 3 == 1 and i % 2 == 0:
            c = c * 1.35                                                               # stitching
        return col(np.clip(c, 0, 255)), None
    box(m, a, -5.4, -6.0, -3.7, 5.4, -1.6, 3.7, leather)
    # ---- the hand: a core plate with the thumb on its right, and the back plate over it
    core = np.zeros((15, 18), bool)                                                  # x -7.8 .. 10.2, y -1.6 .. 13.4 (row 0 on top)
    core[0:15, 0:16] = True
    core[0:2, 0:1] = core[0:2, 15:16] = False
    core[13:15, 0:1] = False
    core[2:12, 16:18] = True                                                         # the thumb's base, a bump on the right

    def core_paint(i, j, front):
        c = _pp_gold_shade(gold, i, j, 18, 15, 44) * (0.88 if i > 15 else 1.0)
        if i > 15 and j in (3, 10):
            c = c * 0.62
        return col(np.clip(c, 0, 255)), None
    plate(m, a, core, -7.8, -1.6, 3.4, core_paint, "xy", 0.0)
    back = _pp_rect_round(15, 12, 2)

    def back_paint(i, j, front):
        c = _pp_gold_shade(goldl, i, j, 15, 12, 45, 24)
        if i == 0 or j == 0:
            c = c * 1.16
        if i == 14 or j == 11 or (i == 13 and j == 10):
            c = c * 0.66
        if (i, j) in ((2, 2), (12, 2), (2, 9), (12, 9)):
            c = np.array((255, 238, 176), np.float32)                                    # rivets
        return col(np.clip(c, 0, 255)), None
    plate(m, a, back, -7.5, -1.2, 0.5, back_paint, "xy", 3.9)
    cross = ascii_mask(["..###..", "...#...", "#..#..#", "#######", "#..#..#", "...#...", "...#...", "...#...", "..###.."])

    def cross_paint(i, j, front):
        edge = i in (0, 6) or j in (0, 8)
        c = np.array((150, 214, 255) if edge else _PP_BLUE, np.float32)
        return col(c), col((110, 200, 255), 1.0)
    plate(m, a, cross, -3.5, 0.9, 0.3, cross_paint, "xy", 4.6)
    # ---- the fingers: four curled plates, each its own block, a knuckle bump below each, a rounded cap on top
    fx = (-5.55, -1.85, 1.85, 5.55)
    for n, x in enumerate(fx):
        def finger(face, i, j, fw, fh, n=n):
            c = _pp_gold_shade(goldl if face in (0, 5) else gold, i, j, fw, fh, 47 + n)
            if face == 5 and j in (2, 3):
                c = c * 0.7                                                              # the joint of the finger on its back
            if face == 3 and j in (1, 2):
                c = c * 0.6                                                              # the fingertip tucked against the palm
            if face == 0 and (i == 0 or i == fw - 1):
                c = c * 0.72
            if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
                c = c * 1.1
            return col(np.clip(c, 0, 255)), None
        box(m, a, x - 1.65, 12.4, -3.4, x + 1.65, 18.0, 4.9, finger)
        box(m, a, x - 1.45, 18.0, -2.4, x + 1.45, 19.0, 4.0, _pp_gold_box((250, 208, 108), salt=n + 50))
        box(m, a, x - 1.65, 11.0, 3.4, x + 1.65, 12.8, 5.5, _pp_gold_box((246, 200, 100), salt=n + 20))
    # ---- the thumb: tucked across the front of the fingers, its tip beside the first finger
    box(m, a, 4.0, 8.8, -4.6, 10.4, 13.0, -0.6, _pp_gold_box((222, 166, 58), salt=30))
    box(m, a, 4.0, 13.0, -4.6, 8.6, 15.0, -1.4, _pp_gold_box((240, 190, 90), salt=31))
    # ---- the halo: a thin ring of light round the fist, with eight gold ticks
    R = 13
    ring_mask = np.zeros((2 * R + 2, 2 * R + 2), bool)
    yy, xx = np.mgrid[0:2 * R + 2, 0:2 * R + 2] + 0.5
    d = np.hypot(xx - (R + 1), yy - (R + 1))
    ring_mask |= (d >= 11.2) & (d <= 12.7)
    for k in range(8):
        ang = k * math.pi / 4
        ring_mask |= (np.abs((xx - (R + 1)) * math.cos(ang) + (yy - (R + 1)) * math.sin(ang) - 13.0) <= 1.0) & (np.abs(-(xx - (R + 1)) * math.sin(ang) + (yy - (R + 1)) * math.cos(ang)) <= 0.9)

    def ring_paint(i, j, front):
        x, y = i + 0.5 - (R + 1), j + 0.5 - (R + 1)
        if math.hypot(x, y) > 12.9:
            return col((255, 226, 140)), col((255, 226, 140), 0.9)
        return col((196, 236, 255)), col((150, 220, 255), 0.85)
    plate(m, a, ring_mask, -(R + 1.0), -(R + 1.0) + 0.75, 0.35, ring_paint, "xz", 4.0)
    return dict(glow=("8CD2FF", 0.08, 0.45, 1.0))


SPECS += [("brooch_pin", brooch_pin), ("hand_of_providence", hand_of_providence)]
REAL.update({"brooch_pin": 0.55, "hand_of_providence": 0.5})                           # metres: a dagger, and a gauntlet from the elbow to the fist
GRIP.update({"brooch_pin": 0.9, "hand_of_providence": 0.47})                         # held by the wound grip / the wrist band

_pp_sprite_axis = sprite_axis


def sprite_axis(item_id):                                                            # noqa: F811
    """The gauntlet's sprite is upright (the cuff at the bottom, the fist on top): its axis is the middle of the icon's box, pointing up."""
    if item_id == "hand_of_providence":
        x0, y0, x1, y1 = sprite_box(item_id)
        return np.array([(x0 + x1) / 2, y0]), 90.0, y1 - y0
    return _pp_sprite_axis(item_id)
