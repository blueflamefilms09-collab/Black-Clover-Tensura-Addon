"""1.3.0: the 3D in-hand models of the Brooch Pin and the Hand of Providence.

This file is not run on its own: tools/gen_weapon_models.py executes it in its own namespace (one line before `def mirror_x`), so np, math,
Image, Mesh, Atlas, Blade, box, plate, ascii_mask, col, shade, mix, nz, fz, hsh, wrap_paint, metal_paint, lerp, SPECS, SIZE, GRIP and
sprite_axis / sprite_box all exist here.

    python3 -B tools/gen_weapon_models.py brooch_pin hand_of_providence      (then: python3 -B tools/item_preview/preview_weapons.py brooch_pin hand_of_providence)
    python3 -B tools/gen_pin_providence_icons.py                              (the inventory icons and item models, first)

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
_PP_GOLD = (210, 154, 52)
_PP_LIGHT = np.array((-0.45, 0.62, 0.65))
_PP_LIGHT = _PP_LIGHT / np.linalg.norm(_PP_LIGHT)


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
    """The filigree frame: an irregular rounded square with scalloped edges, a teardrop hole for the ruby, scrolls cut into its gold."""
    W, H = 19, 18
    cx, cy = W / 2, H / 2
    yy, xx = np.mgrid[0:H, 0:W] + 0.5
    dx, dy = (xx - cx) / 9.0, (yy - cy) / 8.5
    outer = np.abs(dx) ** 3.0 + np.abs(dy) ** 3.0 <= 1.0
    for k in range(12):                                                              # the scallops: a ring of small lobes round the rim
        ang = 2 * math.pi * k / 12 + 0.26
        px_, py_ = cx + math.cos(ang) * 9.0 * 0.96, cy + math.sin(ang) * 8.5 * 0.96
        outer |= np.hypot(xx - px_, yy - py_) <= 1.55
    hole = _pp_mask_ellipse(W, H, cx, cy, 5.5, 6.6, math.radians(-22))
    ring = outer & ~hole
    up, down, left, right = _pp_edge(ring)
    # the collar: frame pixels beside the hole are the sockets of the teeth
    hole_big = _pp_mask_ellipse(W, H, cx, cy, 6.6, 7.7, math.radians(-22))
    collar = ring & hole_big

    def paint(i, j, front):
        x, y = i + 0.5, j + 0.5
        scroll = math.sin(x * 0.95 + math.sin(y * 0.8) * 1.6) + math.sin(y * 1.1 - x * 0.45)
        c = np.array(_PP_GOLD, np.float32) + nz(x, y, 31) * 40 + fz(x, y, 32) * 18
        if abs(scroll) < 0.34:
            c = np.array((88, 56, 20), np.float32) + fz(x, y, 33) * 10                # the groove of a scroll
        elif scroll > 1.25:
            c = np.array((252, 214, 116), np.float32)                                 # its raised ridge
        if up[j, i] or left[j, i]:
            c = c * 1.22
        if down[j, i] or right[j, i]:
            c = c * 0.7
        g = None
        if collar[j, i]:
            if (i + j) % 2 == 0:
                c = np.array((250, 240, 212), np.float32) + fz(x, y, 34) * 10          # an ivory tooth
            else:
                c = np.array((96, 60, 22), np.float32)
        elif scroll > 1.45:
            g = col((255, 190, 90), 0.25)
        return col(np.clip(c, 0, 255)), g
    # the frame's own plate: x from -9.5, y from -17.5 (the plate's row 0 is its top)
    plate(m, a, ring, -9.5, -17.5, 1.0, paint, "xy", 0.0)
    return cx, cy


def brooch_pin(m, a):
    """Brooch Pin: a long steel pin run through a ruby brooch of gold filigree, a collar of ivory teeth round the stone, a wound grip below."""
    # ---- the pin: a slim steel needle blade, brushed, a bright ridge, the red of the stone spilling up its foot
    L = 36
    st = [(-3.0, -1.55, 0.0, 1.55), (6.0, -1.55, 0.0, 1.55), (18.0, -1.3, 0.0, 1.3), (28.0, -0.9, 0.0, 0.9), (L, 0.0, 0.0, 0.0)]

    def pin_paint(x, y, front, e, rd):
        if x is None:
            return col((196, 204, 220)), None
        c = np.array((178, 188, 206), np.float32) + fz(int(x * 2), int(y * 0.7), 21) * 26
        if abs(x) < 0.4:
            c = np.array((238, 244, 253), np.float32)                                   # the bright ridge
        elif abs(x) > 1.0 and y < 14:
            c = c * 0.82
        g = None
        if y < 9:
            g = col((255, 52, 84), 0.5 * (1.0 - max(0.0, y) / 9.0))                    # the ruby's light on the foot of the pin
        if y > 26 and abs(x) < 0.3:
            g = col((255, 210, 220), 0.45)                                              # a point of light at the tip
        if e < 0.7:
            c = np.array((250, 252, 255), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.5, 1.0, pin_paint).build(m, a)
    # ---- the collar the pin rises from, a gold boss with a ring above the frame
    box(m, a, -2.4, -1.0, -1.7, 2.4, 2.2, 1.7, _pp_gold_box(rivets=True, salt=3))
    box(m, a, -1.7, 2.2, -1.2, 1.7, 3.4, 1.2, _pp_gold_box((240, 196, 96), salt=4))
    # ---- the brooch
    cx, cy = _pp_brooch_frame(m, a)
    ox, oy = -9.5 + cx, -17.5 + (18 - cy)                                            # the centre of the frame in model units
    tilt = math.radians(-22)
    # the ruby: a faceted cabochon domed out of both faces of the frame, glowing
    _pp_dome(m, a, (ox, oy, 0.9), 5.9, 7.0, 3.5, -tilt, 12, 4, _pp_gem_paint(glow_a=0.6), sign=1)
    _pp_dome(m, a, (ox, oy, -0.9), 5.9, 7.0, 2.6, -tilt, 12, 3, _pp_gem_paint(glow_a=0.45), sign=-1)
    # the teeth of the collar standing up out of the frame's face, round the stone
    for k in range(16):
        ang = 2 * math.pi * k / 16
        ex, ey = 6.5 * math.cos(ang), 7.6 * math.sin(ang)
        px_, py_ = ex * math.cos(-tilt) - ey * math.sin(-tilt), ex * math.sin(-tilt) + ey * math.cos(-tilt)
        for s in (1, -1):
            box(m, a, ox + px_ - 0.45, oy + py_ - 0.45, 0.9 * s - (0.0 if s > 0 else 0.9), ox + px_ + 0.45, oy + py_ + 0.45, 0.9 * s + (0.9 if s > 0 else 0.0),
                _pp_gold_box((250, 240, 214), salt=k))
    # the hinge barrel at the lower left and the catch at the lower right
    box(m, a, -10.8, -17.2, -1.0, -8.0, -14.2, 1.0, _pp_gold_box((224, 170, 64), rivets=True, salt=7))
    box(m, a, 7.6, -17.0, -0.9, 10.2, -14.6, 0.9, _pp_gold_box((196, 142, 46), salt=8))
    # ---- below the brooch: a gold ferrule, the wound grip, a ruby pommel in a gold cup
    box(m, a, -2.5, -20.2, -1.7, 2.5, -17.4, 1.7, _pp_gold_box(rivets=True, salt=9))
    box(m, a, -1.4, -28.6, -1.4, 1.4, -20.2, 1.4, wrap_paint((98, 24, 36), (226, 172, 62), diamonds=True))
    box(m, a, -2.6, -31.0, -2.1, 2.6, -28.6, 2.1, _pp_gold_box((230, 178, 66), salt=10))
    _pp_dome(m, a, (0.0, -31.0, 0.0), 2.0, 2.0, 2.0, 0.0, 8, 3, _pp_gem_paint(glow_a=0.7), sign=1)
    return dict(glow=("FF3358", 0.1, 0.4, 1.0))


# ================================================================ the Hand of Providence
_PP_BLUE = (62, 148, 232)


def _pp_gold_plate(base=(222, 168, 60), grooves=(), salt=0):
    """A painter for a gold plate: vertical streaks like the painted gold of the sheet, bevelled edges, optional dark groove rows."""
    def p(i, j, front, mask=None):
        x, y = i + 0.5, j + 0.5
        c = np.array(base, np.float32) + fz(x * 3.1, y * 0.3, salt + 5) * 38 + nz(x, y, salt + 6) * 14
        if j in grooves:
            c = c * 0.62
        return col(np.clip(c, 0, 255)), None
    return p


def _pp_flare_mask(w, h, hw_top, hw_bot, round_bot=0):
    """A flared silhouette: row 0 (top) is hw_top wide each side of the middle, the last row hw_bot."""
    m = np.zeros((h, w), bool)
    for j in range(h):
        hw = lerp(hw_top, hw_bot, j / max(1, h - 1))
        for i in range(w):
            if abs(i + 0.5 - w / 2) <= hw:
                m[j, i] = True
    return m


def _pp_rect_round(w, h, r):
    m = np.ones((h, w), bool)
    for (ci, cj) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        for j in range(h):
            for i in range(w):
                if abs(i - ci) < r and abs(j - cj) < r and math.hypot(max(0, r - 1 - abs(i - ci)) if False else 0, 0) == 0:
                    pass
    for j in range(h):
        for i in range(w):
            di = min(i, w - 1 - i)
            dj = min(j, h - 1 - j)
            if di < r and dj < r and math.hypot(r - 0.5 - di, r - 0.5 - dj) > r:
                m[j, i] = False
    return m


def hand_of_providence(m, a):
    """Hand of Providence: a golden gauntlet closed in a fist. y runs up the arm: the flared vambrace below, a dark wrist band, the back of the hand
    with its raised blue cross, four curled finger plates and the thumb; a halo ring floats round the fist. The cross, the cuff's trim and the ring glow."""
    gold = (222, 168, 60)
    goldl = (250, 204, 104)
    goldd = (150, 100, 32)
    # ---- the vambrace: a flared plate (wider at the rim), grooved, with a rim ring and two lines of blue trim
    vw, vh = 17, 18                                                                  # x -8.5 .. 8.5, y -24 .. -6
    vmask = _pp_flare_mask(vw, vh, 5.7, 7.5)

    def vamb_paint(i, j, front):
        x, y = i + 0.5, j + 0.5
        c = np.array(gold, np.float32) + fz(x * 3.1, y * 0.25, 40) * 40 + nz(x, y, 41) * 12
        g = None
        if j in (4, 11):
            c = c * 0.6                                                                # two grooves round the arm
        if j in (5, 12):
            c = c * 1.15
        if j == 8 and 2 <= i <= vw - 3:
            c = np.array(_PP_BLUE, np.float32)                                         # a line of blue trim between them
            g = col((90, 190, 255), 0.85)
        if i in (3, vw - 4) and j >= 13 and front:
            c = np.array(_PP_BLUE, np.float32)
            g = col((90, 190, 255), 0.7)
        lo = i < 2 or i > vw - 3
        if lo:
            c = c * 0.72
        return col(np.clip(c, 0, 255)), g
    plate(m, a, vmask, -8.5, -24.0, 4.5, vamb_paint, "xy", 0.0)
    rim = np.ones((3, 17), bool)
    plate(m, a, rim, -8.5, -24.8, 5.4, lambda i, j, f: (col(np.clip(np.array(goldl, np.float32) * (1.1 if j == 0 else 0.84 if j == 2 else 1.0) + fz(i * 2, j * 3, 42) * 18, 0, 255)),
                                                        col((255, 220, 140), 0.25) if j == 0 and i % 4 == 1 else None), "xy", 0.0)
    # ---- the wrist: dark leather between the vambrace and the hand
    def leather(face, i, j, fw, fh):
        c = np.array((78, 42, 34), np.float32) + fz(i * 2.3, j * 1.7, 43) * 16
        if face >= 2 and j % 3 == 1 and i % 2 == 0:
            c = c * 1.35                                                               # stitching
        return col(np.clip(c, 0, 255)), None
    box(m, a, -5.2, -6.0, -3.6, 5.2, -1.6, 3.6, leather)
    # ---- the hand: a core plate with the thumb on its right, and the back plate over it
    core = np.zeros((15, 20), bool)                                                  # x -7.8 .. 12.2, y -1.6 .. 13.4 (row 0 on top)
    core[0:15, 0:16] = True
    core[0:2, 0:1] = core[0:2, 15:16] = False
    core[13:15, 0:1] = False
    core[2:12, 16:19] = True                                                         # the thumb's base, a bump on the right
    core[3:11, 19:20] = True

    def core_paint(i, j, front):
        c = np.array(gold, np.float32) * (0.9 if i > 14 else 1.0) + fz(i * 2.7, j * 0.4, 44) * 30
        if i > 15 and (j in (3, 10)):
            c = c * 0.62
        return col(np.clip(c, 0, 255)), None
    plate(m, a, core, -7.8, -1.6, 3.4, core_paint, "xy", 0.0)
    back = _pp_rect_round(15, 12, 2)

    def back_paint(i, j, front):
        x, y = i + 0.5, j + 0.5
        c = np.array(goldl, np.float32) + fz(x * 2.7, y * 0.5, 45) * 26 + nz(x, y, 46) * 8
        if i == 0 or j == 0:
            c = c * 1.14
        if i == 14 or j == 11 or (i == 13 and j == 10):
            c = c * 0.66
        if (i, j) in ((2, 2), (12, 2), (2, 9), (12, 9)):
            c = np.array((255, 236, 170), np.float32)                                    # rivets
        return col(np.clip(c, 0, 255)), None
    plate(m, a, back, -7.5, -1.2, 0.5, back_paint, "xy", 3.9)
    cross = ascii_mask(["..###..", "...#...", "#..#..#", "#######", "#..#..#", "...#...", "...#...", "...#...", "..###.."])

    def cross_paint(i, j, front):
        edge = i in (0, 6) or j in (0, 8)
        c = np.array((176, 226, 255) if edge else _PP_BLUE, np.float32)
        return col(c), col((120, 205, 255), 1.0)
    plate(m, a, cross, -3.5, 0.9, 0.3, cross_paint, "xy", 4.6)
    # ---- the fingers: four curled plates, each its own block, a knuckle bump below each
    fx = (-5.55, -1.85, 1.85, 5.55)
    for n, x in enumerate(fx):
        def finger(face, i, j, fw, fh, n=n):
            c = np.array(goldl if face in (0, 5) else gold, np.float32) + fz(i * 2.1 + n * 7, j * 1.5, 47) * 28
            if face == 5 and j in (2, 3):
                c = c * 0.7                                                              # the joint of the finger on its back
            if face == 3 and j in (1, 2):
                c = c * 0.6                                                              # the fingertip tucked against the palm
            if face == 0 and (i == 0 or i == fw - 1):
                c = c * 0.72
            if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
                c = c * 1.1
            return col(np.clip(c, 0, 255)), None
        box(m, a, x - 1.65, 12.4, -3.4, x + 1.65, 18.4, 4.9, finger)
        box(m, a, x - 1.65, 11.0, 3.4, x + 1.65, 12.8, 5.5, _pp_gold_box((246, 200, 100), salt=n + 20))
    # ---- the thumb: tucked across the front of the fingers, its tip beside the first finger
    box(m, a, 4.0, 8.8, -4.6, 10.4, 13.0, -0.6, _pp_gold_box((214, 158, 54), salt=30))
    box(m, a, 4.0, 13.0, -4.6, 8.6, 15.0, -1.4, _pp_gold_box((234, 184, 84), salt=31))
    # ---- the halo: a thin ring of light round the fist, with eight gold ticks
    R = 13
    ring_mask = np.zeros((2 * R + 2, 2 * R + 2), bool)
    yy, xx = np.mgrid[0:2 * R + 2, 0:2 * R + 2] + 0.5
    d = np.hypot(xx - (R + 1), yy - (R + 1))
    ring_mask |= (d >= 11.4) & (d <= 12.6)
    for k in range(8):
        ang = k * math.pi / 4
        ring_mask |= (np.abs((xx - (R + 1)) * math.cos(ang) + (yy - (R + 1)) * math.sin(ang) - 13.0) <= 1.0) & (np.abs(-(xx - (R + 1)) * math.sin(ang) + (yy - (R + 1)) * math.cos(ang)) <= 0.9)

    def ring_paint(i, j, front):
        x, y = i + 0.5 - (R + 1), j + 0.5 - (R + 1)
        if math.hypot(x, y) > 12.8:
            return col((255, 226, 140)), col((255, 226, 140), 0.9)
        return col((196, 236, 255)), col((150, 220, 255), 0.85)
    plate(m, a, ring_mask, -(R + 1.0), -(R + 1.0) + 4.0, 0.35, ring_paint, "xz", 4.0)
    return dict(glow=("8CD2FF", 0.08, 0.45, 1.0))


SPECS += [("brooch_pin", brooch_pin), ("hand_of_providence", hand_of_providence)]
SIZE.update({"brooch_pin": 0.88})
GRIP.update({"brooch_pin": 0.2})

_pp_sprite_axis = sprite_axis


def sprite_axis(item_id):                                                            # noqa: F811
    """The gauntlet's sprite is upright (the cuff at the bottom, the fist on top): its axis is the middle of the icon's box, pointing up."""
    if item_id == "hand_of_providence":
        x0, y0, x1, y1 = sprite_box(item_id)
        return np.array([(x0 + x1) / 2, y0]), 90.0, y1 - y0
    return _pp_sprite_axis(item_id)
