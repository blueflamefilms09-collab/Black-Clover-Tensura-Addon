"""Why: a dungeon is a place where things get worn. The skin painter of the player-model kit (tools/playermodel/skin_lib.py) paints a clean character from a sheet; these are the passes that run on
the finished 64x64 canvas and make it LIVE in the dungeon: grime that gathers towards the feet and the hems, rot and stains in low blotches, scars, tears in the cloth of the second layer (the
jacket / sleeves / pants / hat layer shows the skin under it where it is torn), stitches, rivets on plates, a rib cage and joints on bone, hair-thin glowing cracks and veins (they also write the
glow map), frost, moss, blood, fur flecks, warts and runes. Every pass is deterministic (a hash of the texel and a seed, never a clock), works in the part's own cell coordinates (lx from the entity's
right, ly from the top, lz from the front: `skin_lib.cell`), and leaves a texel alone when it is empty, so a pass over the second layer never fills the transparent parts of a plate or a cloak.
`apply(cv, glow, passes, seed)` runs a recipe: a list of (pass name, keyword arguments). Run python as `python3 -B`.
"""
import sys

import numpy as np  # noqa: F401  (the canvas is a numpy array)

import common  # noqa: F401  (sets up sys.path)
import skin_lib as K

PARTS = ("head", "body", "arm_r", "arm_l", "leg_r", "leg_l")
FACE_ID = {"front": 1, "back": 2, "west": 3, "east": 4, "top": 5, "bottom": 6}


def texels(cv, part, overlay=False):
    """(x, y, face, lx, ly, lz, w, h, d) for every texel of a layer of a part."""
    w, h, d = cv.size(part)
    u, v = cv.uv(part, overlay)
    for face, (x0, y0, rw, rh) in K.rects(u, v, w, h, d).items():
        for yy in range(rh):
            for xx in range(rw):
                lx, ly, lz = K.cell(face, xx, yy, w, h, d)
                yield x0 + xx, y0 + yy, face, lx, ly, lz, w, h, d


def rnd(face, lx, ly, lz, seed, salt=0):
    """0..1, a hash of the texel."""
    return K.hsh(lx * 7 + FACE_ID[face], ly * 3 + salt, lz * 5 + seed)


def blob(face, lx, ly, lz, seed, cell=2, salt=0):
    """0..1 constant over a cell x cell block of a face: the low-frequency patches of stains and rot."""
    return K.hsh(lx // cell + FACE_ID[face] * 11, ly // cell + salt, lz // cell + seed * 3)


def mix(a, b, t):
    a0, a1, a2, b0, b1, b2 = int(a[0]), int(a[1]), int(a[2]), int(b[0]), int(b[1]), int(b[2])        # numpy uint8 scalars would wrap on a subtraction
    return (int(a0 + (b0 - a0) * t), int(a1 + (b1 - a1) * t), int(a2 + (b2 - a2) * t))


def _layers(cv, layers, parts):
    for part in parts:
        for ov in layers:
            yield part, ov


def _set(cv, x, y, rgb, a=255):
    cv.px[y, x] = (max(0, min(255, rgb[0])), max(0, min(255, rgb[1])), max(0, min(255, rgb[2])), a)


def _glow(glow, x, y, rgb, a=255):
    glow[y, x] = (rgb[0], rgb[1], rgb[2], a)


def _rgb(c):
    if isinstance(c, str):
        import palette as PAL
        h = PAL.hexof(c).lstrip("#")
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))
    return tuple(c)


# ================================================================================================ the passes
def grime(cv, glow, seed, k=0.35, tint="soot", layers=(False, True), parts=PARTS, top=0.15):
    """Dirt that gathers towards the bottom of every part (feet, hems, cuffs, the fall of a coat), broken up by a per-texel hash."""
    t = _rgb(tint)
    for part, ov in _layers(cv, layers, parts):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0 or face == "top":
                continue
            f = ly / float(max(1, h - 1))
            s = k * (top + (1.0 - top) * f * f) * (0.55 + 0.9 * rnd(face, lx, ly, lz, seed, 1))
            if s > 0.03:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), t, min(0.85, s)), int(cv.px[y, x, 3]))


def blotch(cv, glow, seed, color="swamp", k=0.55, cell=2, coverage=0.28, layers=(False,), parts=PARTS, salt=3):
    """Low patches of a colour (rot, a stain, moss, mildew): a block of cells is coloured when its hash is under the coverage."""
    c = _rgb(color)
    for part, ov in _layers(cv, layers, parts):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0:
                continue
            if blob(face, lx, ly, lz, seed, cell, salt) < coverage:
                shade = 0.85 + 0.3 * rnd(face, lx, ly, lz, seed, 9)
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), tuple(int(v * shade) for v in c), k), int(cv.px[y, x, 3]))


def spots(cv, glow, seed, color="moss", density=0.07, layers=(False,), parts=("head", "arm_r", "arm_l", "body"), dark=False):
    """Single texels of a colour: warts, freckles, leopard spots, scale speckles."""
    c = _rgb(color)
    for part, ov in _layers(cv, layers, parts):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0 or face == "bottom":
                continue
            if rnd(face, lx, ly, lz, seed, 17) < density:
                base = tuple(cv.px[y, x][:3])
                _set(cv, x, y, mix(base, (int(c[0] * 0.5), int(c[1] * 0.5), int(c[2] * 0.5)) if dark else c, 0.7), int(cv.px[y, x, 3]))


def scars(cv, glow, seed, count=2, color=(110, 60, 60), parts=("arm_r", "arm_l", "body", "leg_r", "leg_l")):
    """Short diagonal cuts across the front and back of a part."""
    c = _rgb(color)
    n = 0
    for i, part in enumerate(parts * 3):
        if n >= count:
            break
        w, h, d = cv.size(part)
        u, v = cv.uv(part, False)
        face = "front" if (i + seed) % 2 == 0 else "back"
        fx, fy, rw, rh = K.rects(u, v, w, h, d)[face]
        sx = int(K.hsh(i, seed, 41) * max(1, rw - 3))
        sy = 1 + int(K.hsh(i, seed, 43) * max(1, rh - 6))
        n += 1
        for j in range(4):
            xx, yy = fx + min(rw - 1, sx + j), fy + min(rh - 1, sy + j)
            if cv.px[yy, xx, 3]:
                _set(cv, xx, yy, mix(tuple(cv.px[yy, xx][:3]), c, 0.8))
            if j in (1, 2):                      # the lighter edge of the wound
                x2 = min(fx + rw - 1, xx + 1)
                if cv.px[yy, x2, 3]:
                    _set(cv, x2, yy, mix(tuple(cv.px[yy, x2][:3]), (220, 190, 180), 0.35))


def tears(cv, glow, seed, coverage=0.2, parts=("body", "arm_r", "arm_l", "leg_r", "leg_l"), rows=4):
    """Rips along the lower edge of the second layer (a torn jacket, ragged sleeves and trouser legs): those texels become empty, so the layer under shows."""
    for part in parts:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, True):
            if cv.px[y, x, 3] == 0 or face in ("top", "bottom"):
                continue
            fall = (ly - (h - rows)) / float(rows)
            if fall > 0 and rnd(face, lx, ly, lz, seed, 23) < coverage * (0.4 + 1.6 * fall):
                cv.px[y, x, 3] = 0
        # and a hole or two higher up
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, True):
            if cv.px[y, x, 3] and face in ("front", "back") and 2 < ly < h - rows and rnd(face, lx, ly, lz, seed, 29) < coverage * 0.12:
                cv.px[y, x, 3] = 0


def stitches(cv, glow, seed, color=(36, 28, 30), parts=("arm_r", "leg_l", "body", "head")):
    """Coarse stitches: a dark seam with cross ticks round a limb, a vertical seam down the chest, a stitched brow."""
    c = _rgb(color)
    for part in parts:
        w, h, d = cv.size(part)
        for x, y, face, lx, ly, lz, w_, h_, d_ in texels(cv, part, False):
            if cv.px[y, x, 3] == 0:
                continue
            on = False
            if part in ("arm_r", "leg_l"):
                row = h // 2 + (1 if part == "leg_l" else 0)
                on = (ly == row and face not in ("top", "bottom")) or (ly in (row - 1, row + 1) and face not in ("top", "bottom") and (lx + lz) % 2 == 0)
            elif part == "body":
                on = face == "front" and lx in (3, 4) and 1 <= ly <= 9 and (ly % 2 == 0 or lx == 3)
            elif part == "head":
                on = face == "back" and ly == 3 and lx % 2 == 0
            if on:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, 0.9), int(cv.px[y, x, 3]))


def rivets(cv, glow, seed, color=(206, 208, 214)):
    """Bright rivets on plates: a texel of the second layer on a 4-grid whose neighbours are plate too."""
    c = _rgb(color)
    for part in PARTS:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, True):
            if cv.px[y, x, 3] == 0 or face in ("top", "bottom"):
                continue
            if (lx % 4 == 1 and ly % 4 == 1) and 0 < x < 63 and 0 < y < 63 and cv.px[y, x - 1, 3] and cv.px[y, x + 1, 3] and cv.px[y - 1, x, 3] and cv.px[y + 1, x, 3]:
                lum = sum(int(v) for v in cv.px[y, x][:3]) / 3.0
                if 70 < lum < 235:
                    _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, 0.75))


def edge_light(cv, glow, seed, amount=0.22):
    """A lighter line on the top row of every plate and cloth of the second layer, a darker one under its bottom row: plates get thickness."""
    for part in PARTS:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, True):
            if cv.px[y, x, 3] == 0 or face in ("top", "bottom"):
                continue
            above = cv.px[y - 1, x, 3] if y > 0 else 0
            below = cv.px[y + 1, x, 3] if y < 63 else 0
            p = tuple(int(v) for v in cv.px[y, x][:3])
            if not above and ly > 0:
                _set(cv, x, y, mix(p, (255, 255, 255), amount), int(cv.px[y, x, 3]))
            elif not below and ly < h - 1:
                _set(cv, x, y, mix(p, (0, 0, 0), amount * 1.3), int(cv.px[y, x, 3]))


def is_skin(cv, x, y):
    """Is this texel still bare skin (within a hair of one of the four tones painted first)? A pass that draws a body (ribs) must not paint over clothes."""
    t = getattr(cv, "tones", None)
    if not t or cv.px[y, x, 3] == 0:
        return cv.px[y, x, 3] != 0
    p = [int(v) for v in cv.px[y, x][:3]]
    return any(abs(p[0] - c[0]) + abs(p[1] - c[1]) + abs(p[2] - c[2]) < 30 for c in t)


def ribs(cv, glow, seed, bone=(214, 204, 176), gap=(40, 34, 32), spine=True):
    """A bare skeleton: ribs across the chest, a spine, a pelvis, joints at the elbows and knees. Only bare skin turns to bone: a garment is left as it is."""
    b = _rgb(bone)
    g = _rgb(gap)
    for x, y, face, lx, ly, lz, w, h, d in texels(cv, "body", False):
        if not is_skin(cv, x, y):
            continue
        if face == "front" and 1 <= ly <= 8:
            rib = ly % 2 == 1 and lx not in (3, 4)
            _set(cv, x, y, mix(b, (255, 255, 255), 0.1) if (rib or lx in (3, 4)) else g)
        elif face == "back" and ly <= 10:
            _set(cv, x, y, b if (spine and lx in (3, 4)) else (g if ly % 3 else mix(b, g, 0.5)))
        elif face in ("west", "east") and ly <= 9:
            _set(cv, x, y, b if ly % 2 == 1 else g)
        elif ly >= 10 and face in ("front", "back"):
            _set(cv, x, y, b if (lx in (1, 2, 5, 6) and ly == 10) or lx in (3, 4) and ly == 11 else g)
    for part in ("arm_r", "arm_l", "leg_r", "leg_l"):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, False):
            if not is_skin(cv, x, y):
                continue
            joint = ly in (5, 6)
            shade = 0.92 if face in ("front", "back") else 0.78
            c = tuple(int(v * shade) for v in b)
            if joint:
                c = mix(g, b, 0.45)
            if ly == 0 and part.startswith("arm"):
                c = mix(b, (255, 255, 255), 0.15)
            _set(cv, x, y, c)


def skull(cv, glow, seed, bone=(222, 214, 188), socket=(18, 14, 20), eye=None):
    """A skull face on the head: sockets, a nose hole, teeth. The other sides of the head are bone."""
    b = _rgb(bone)
    s = _rgb(socket)
    for x, y, face, lx, ly, lz, w, h, d in texels(cv, "head", False):
        shade = 1.0 if face in ("front", "back", "top") else 0.84
        c = tuple(int(v * shade) for v in b)
        if face == "front":
            if ly in (3, 4) and lx in (1, 2, 5, 6):
                c = s
                if eye is not None and ly == 4 and lx in (2, 5):
                    c = _rgb(eye)
                    _glow(glow, x, y, c)
            elif ly == 5 and lx in (3, 4):
                c = s
            elif ly == 7 and lx in (1, 2, 3, 4, 5, 6):
                c = s if lx % 2 == 0 else mix(b, (255, 255, 255), 0.1)
        if face != "bottom":
            _set(cv, x, y, c)


def cracks(cv, glow, seed, color="hellfire", count=7, parts=("body", "arm_r", "arm_l", "leg_r", "leg_l", "head"), emit=True):
    """Hair-thin glowing cracks: short random walks across the front, back and sides; they also write the glow map, so the skin shines through them."""
    c = _rgb(color)
    dark = mix(c, (0, 0, 0), 0.55)
    for i in range(count):
        part = parts[(i * 5 + seed) % len(parts)]
        w, h, d = cv.size(part)
        u, v = cv.uv(part, False)
        face = ("front", "back", "west", "east")[(i + seed) % 4]
        fx, fy, rw, rh = K.rects(u, v, w, h, d)[face]
        x = int(K.hsh(i, seed, 61) * rw)
        y = int(K.hsh(i, seed, 63) * max(1, rh - 3))
        for j in range(5):
            if 0 <= x < rw and 0 <= y < rh and cv.px[fy + y, fx + x, 3]:
                _set(cv, fx + x, fy + y, c)
                if emit:
                    _glow(glow, fx + x, fy + y, c)
                if x + 1 < rw and cv.px[fy + y, fx + x + 1, 3] and j % 2 == 0:
                    _set(cv, fx + x + 1, fy + y, dark)
            r = K.hsh(i, j, seed + 67)
            x += 1 if r > 0.66 else (-1 if r < 0.2 else 0)
            y += 1


def veins(cv, glow, seed, color="voidlight", parts=("arm_r", "arm_l", "leg_r", "leg_l"), emit=True):
    """Glowing veins that wander down the limbs (a spirit, a cursed thing, an arcane sleeper): a thin line that shifts a texel every few rows and breaks now and then."""
    c = _rgb(color)
    for part in parts:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, False):
            if cv.px[y, x, 3] == 0 or face in ("top", "bottom") or ly < 1:
                continue
            side = 1 if face in ("front", "back") else 0
            col = (1 + ((ly // 3 + seed + (3 if part.endswith("r") else 0)) % 3 == 0)) if side else (1 + (ly // 4 + seed) % 2)
            if lx == col and rnd(face, lx, ly, lz, seed, 71) > 0.28:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, 0.75))
                if emit:
                    _glow(glow, x, y, c)


def runes(cv, glow, seed, color="lumen", where=("body", "arm_r", "arm_l"), emit=True):
    """Small glowing glyphs (a cross, a bar, a hook) on the chest, the back and the sleeves: holy script, cult sigils, the sign of a guild."""
    c = _rgb(color)
    glyphs = (((0, 0), (0, 1), (0, 2), (1, 1)), ((0, 0), (1, 0), (1, 1), (1, 2)), ((0, 1), (1, 0), (1, 1), (1, 2), (2, 1)), ((0, 0), (0, 1), (0, 2), (1, 0), (1, 2)))
    for part in where:
        w, h, d = cv.size(part)
        for ov in (True, False):
            u, v = cv.uv(part, ov)
            for face in ("front", "back"):
                fx, fy, rw, rh = K.rects(u, v, w, h, d)[face]
                for gi in range(2 if part == "body" else 1):
                    gx = 1 + int(K.hsh(gi, seed, 71 + (1 if face == "back" else 0)) * max(1, rw - 4))
                    gy = 2 + int(K.hsh(gi, seed, 73) * max(1, rh - 6))
                    g = glyphs[int(K.hsh(gi, seed, 79) * len(glyphs))]
                    for (dx, dy) in g:
                        xx, yy = fx + gx + dx, fy + gy + dy
                        if xx < fx + rw and yy < fy + rh and cv.px[yy, xx, 3]:
                            _set(cv, xx, yy, c)
                            if emit and not ov:
                                _glow(glow, xx, yy, c)


def frost(cv, glow, seed, color=(190, 226, 244), k=0.5, layers=(False, True)):
    """Rime: speckles over everything, a white crust on the shoulders and the hems."""
    c = _rgb(color)
    for part, ov in _layers(cv, layers, PARTS):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0:
                continue
            crust = (face == "top" or ly == 0 or ly >= h - 2) and rnd(face, lx, ly, lz, seed, 31) < 0.7
            if crust or rnd(face, lx, ly, lz, seed, 37) < 0.06:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, k if crust else 0.8), int(cv.px[y, x, 3]))


def moss(cv, glow, seed, color=(78, 112, 54), layers=(False, True)):
    """Moss on the tops of the shoulders, the head and the chest, in the creases, and a few damp patches lower down."""
    c = _rgb(color)
    for part, ov in _layers(cv, layers, PARTS):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0:
                continue
            top = face == "top" or (ly <= 1 and part in ("arm_r", "arm_l", "head"))
            if (top and rnd(face, lx, ly, lz, seed, 41) < 0.75) or (blob(face, lx, ly, lz, seed, 3, 5) < 0.12 and rnd(face, lx, ly, lz, seed, 43) < 0.8):
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), tuple(int(v * (0.8 + 0.4 * rnd(face, lx, ly, lz, seed, 47))) for v in c), 0.78), int(cv.px[y, x, 3]))


def blood(cv, glow, seed, color=(104, 18, 24), coverage=0.14):
    """Dried blood: a spray over the chest and the weapon arm, drips under it."""
    c = _rgb(color)
    for part in ("body", "arm_r", "head"):
        for ov in (False, True):
            for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
                if cv.px[y, x, 3] == 0 or face in ("top", "bottom"):
                    continue
                f = 0.5 if part != "body" else 1.0
                if blob(face, lx, ly, lz, seed, 2, 7) < coverage * f and rnd(face, lx, ly, lz, seed, 53) < 0.8:
                    _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, 0.8), int(cv.px[y, x, 3]))


def fur(cv, glow, seed, color=None, amount=0.12):
    """Fur flecks: light and dark single texels over the skin, thicker at the edges of every face."""
    for part in PARTS:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, False):
            if cv.px[y, x, 3] == 0:
                continue
            r = rnd(face, lx, ly, lz, seed, 59)
            if r < amount:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), (30, 24, 20), 0.28))
            elif r > 1.0 - amount:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), (240, 228, 210), 0.22))


def scorch(cv, glow, seed, color=(22, 16, 14), k=0.55, ember="ember"):
    """Soot on the shoulders and the hems with a few glowing embers in it (a fire demon, a burnt knight)."""
    c = _rgb(color)
    e = _rgb(ember)
    for part, ov in _layers(cv, (False, True), PARTS):
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
            if cv.px[y, x, 3] == 0:
                continue
            edge = ly <= 1 or ly >= h - 2 or lx in (0, w - 1)
            if edge and rnd(face, lx, ly, lz, seed, 61) < 0.55:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, k), int(cv.px[y, x, 3]))
            elif not ov and rnd(face, lx, ly, lz, seed, 67) < 0.012:
                _set(cv, x, y, e)
                _glow(glow, x, y, e)


def sheen(cv, glow, seed, k=0.22):
    """Wet or polished skin: a lighter streak down the front edge of every limb and the top of the head."""
    for part in PARTS:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, False):
            if cv.px[y, x, 3] == 0:
                continue
            if (face == "front" and lx in (1, 2) and ly % 4 != 3) or (face == "top" and lx in (2, 3)):
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), (255, 255, 255), k))


def gild(cv, glow, seed, color="gold"):
    """Gold leaf on the edges of the plates: the rim of every plate pixel that has an empty neighbour on the left or right."""
    c = _rgb(color)
    for part in PARTS:
        for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, True):
            if cv.px[y, x, 3] == 0 or face in ("top", "bottom"):
                continue
            left = cv.px[y, x - 1, 3] if x > 0 else 0
            right = cv.px[y, x + 1, 3] if x < 63 else 0
            if not left or not right:
                _set(cv, x, y, mix(tuple(cv.px[y, x][:3]), c, 0.6), int(cv.px[y, x, 3]))


def bandages(cv, glow, seed, color=None, parts=None):
    """Wrapped bandages: slanted strips of cloth with dark gaps over the chest, the arms and the legs the sheet names (outfit.bandages); the second layer gets every third strip as thickness."""
    spec = getattr(cv, "spec", None)
    b = ((spec or {}).get("outfit") or {}).get("bandages")
    if not b and parts is None:
        return
    names = parts if parts is not None else b.get("parts", [])
    col = _rgb(color if color is not None else (b or {}).get("color") or "#d8d0b4")
    gap = tuple(int(v * 0.42) for v in col)
    targets = {}
    for n in names:
        if n == "chest":
            targets["body"] = 0
        elif n.startswith("arm_"):
            targets[n] = 0
        elif n.startswith("leg_"):
            targets[n] = 0
        elif n.startswith("forearm_"):
            targets["arm_" + n[-1]] = min(targets.get("arm_" + n[-1], 99), 5)
        elif n.startswith("shin_"):
            targets["leg_" + n[-1]] = min(targets.get("leg_" + n[-1], 99), 5)
    for part, from_row in targets.items():
        for ov in (False, True):
            for x, y, face, lx, ly, lz, w, h, d in texels(cv, part, ov):
                if face in ("top", "bottom") or ly < from_row:
                    continue
                if not ov and cv.px[y, x, 3] == 0:
                    continue
                k = (ly + lx + (2 if face in ("back", "east") else 0)) % 4
                if ov:
                    if k == 0 and (ly + seed) % 3:
                        _set(cv, x, y, tuple(int(v * 0.96) for v in col))
                    continue
                if k == 3:
                    _set(cv, x, y, gap)
                else:
                    _set(cv, x, y, tuple(int(v * (0.9 + 0.1 * ((ly + lx) % 2) + 0.06 * rnd(face, lx, ly, lz, seed, 83))) for v in col))


PASSES = {"bandages": bandages, "grime": grime, "blotch": blotch, "spots": spots, "scars": scars, "tears": tears, "stitches": stitches, "rivets": rivets, "edge_light": edge_light, "ribs": ribs,
          "skull": skull, "cracks": cracks, "veins": veins, "runes": runes, "frost": frost, "moss": moss, "blood": blood, "fur": fur, "scorch": scorch, "sheen": sheen, "gild": gild}


def apply(cv, glow, recipe, seed):
    """Runs a recipe, a list of (pass name, kwargs); returns True when any glow texel was written. Passes run in order, each with its own seed offset."""
    before = int(glow[:, :, 3].astype(int).sum())
    for i, (name, kw) in enumerate(recipe):
        PASSES[name](cv, glow, seed + 13 * i, **kw)
    return int(glow[:, :, 3].astype(int).sum()) != before
