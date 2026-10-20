"""tdpaint: the painters of the painted boxes of a Tiny Djinn (head, torso, arms, hands, the four tail segments, the cape). Each painter is called once for the
light palette and once for the dark one, with the same layout, so the geo stays the same. A painter returns a function fn(face, i, j, fw, fh) for tdlib.fill_box.
Colours are 0..255 triples; an entry is (rgb, alpha, glow rgb or None, glow strength).
"""
from tdlib import hsh, mix, mul, fill_box


def _n(i, j, salt, amt=0.05):
    return 1.0 + (hsh(i, j, salt) - 0.5) * 2 * amt


# ================================================================ the face
def _eyes(spec, P, face_px):
    """Draws the eyes, brows, mouth and marks on a 10 x 9 front face by calling face_px(i, j, rgb, alpha, glow, gk). Rows 3..5 are the eyes (the lids cover them)."""
    style = spec.get("eyes", "round")
    ec = P["eye"]
    dark = (22, 18, 30)
    hair = P["hair"]
    skin = P["skin"]
    for (x0, mirror) in ((1, False), (6, True)):
        def at(dx, dy, rgb, g=None, gk=1.0, a=255):
            face_px(x0 + dx, 3 + dy, rgb, a, g, gk)

        if style in ("round", "wide"):
            for dx in range(3):
                at(dx, 0, mul(ec, 0.55), mul(ec, 0.4), 0.7)
                at(dx, 1, ec, ec, 1.0)
                at(dx, 2, mul(ec, 1.15), ec, 1.0)
            at(1, 1, dark)                                     # the pupil
            at(2 if not mirror else 0, 0, (255, 255, 255), (255, 255, 255), 1.0)   # the highlight
            if style == "wide":
                at(0 if not mirror else 2, 2, (250, 250, 255))
        elif style == "slit":
            for dx in range(3):
                for dy in range(3):
                    at(dx, dy, mul(ec, 0.8 + 0.2 * dy), ec, 1.0)
            for dy in range(3):
                at(1, dy, dark, None)                          # the cat's slit
            at(0 if not mirror else 2, 0, (255, 250, 220), (255, 250, 220), 1.0)
        elif style == "sharp":
            for dx in range(3):
                at(dx, 1, ec, ec, 1.0)
                at(dx, 2, mul(ec, 0.6), ec, 0.55)
            at(0 if not mirror else 2, 0, dark)                # the upswept lash over the outer corner
            at(1, 0, dark)
            at(1 if not mirror else 1, 1, (255, 255, 255), (255, 255, 255), 1.0)
        elif style == "calm":
            for dx in range(3):
                at(dx, 1, dark)
                at(dx, 2, mul(ec, 0.9), ec, 0.9)
        elif style == "hooded":
            for dx in range(3):
                for dy in range(3):
                    at(dx, dy, mul(skin, 0.28))
            for dx in (1, 2) if not mirror else (0, 1):
                at(dx, 1, ec, ec, 1.0)
        elif style == "three":
            for dx in range(3):
                at(dx, 1, ec, ec, 1.0)
                at(dx, 2, mul(ec, 1.1), ec, 1.0)
                at(dx, 0, mul(ec, 0.5), None)
            at(1, 1, dark)
        elif style == "visor":
            for dx in range(3):
                at(dx, 0, mul(P["metal"], 0.5))
                at(dx, 1, mul(ec, 1.1), ec, 1.0)
                at(dx, 2, mul(P["metal"], 0.6))
        # the brows (row 2)
        bro = mul(hair, 0.7)
        if style not in ("hooded", "visor"):
            for dx in range(3):
                face_px(x0 + dx, 2, bro, 255, None, 0)
            if style == "sharp":
                face_px(x0 + (2 if not mirror else 0), 1, bro, 255, None, 0)
    # the mouth (row 7) and cheeks (row 6)
    mouth = spec.get("mouth", "smile")
    mc = (150, 56, 66)
    if mouth == "smile":
        face_px(4, 7, mc, 255, None, 0)
        face_px(5, 7, mc, 255, None, 0)
        face_px(3, 6, mul(skin, 0.7), 255, None, 0)
        face_px(6, 6, mul(skin, 0.7), 255, None, 0)
    elif mouth == "fang":
        face_px(4, 7, mc, 255, None, 0)
        face_px(5, 7, mc, 255, None, 0)
        face_px(4, 8, (250, 250, 250), 255, None, 0)
        face_px(5, 8, (250, 250, 250), 255, None, 0)
    elif mouth == "flat":
        face_px(4, 7, mul(skin, 0.5), 255, None, 0)
        face_px(5, 7, mul(skin, 0.5), 255, None, 0)
    elif mouth == "open":
        face_px(4, 7, (110, 30, 44), 255, None, 0)
        face_px(5, 7, (110, 30, 44), 255, None, 0)
        face_px(4, 8, (200, 70, 80), 255, None, 0)
        face_px(5, 8, (200, 70, 80), 255, None, 0)
    elif mouth == "veil":
        for i in range(2, 8):
            face_px(i, 7, P["cloth2"], 255, None, 0)
            face_px(i, 8, mul(P["cloth2"], 0.85), 255, None, 0)
    blush = mix(skin, (255, 120, 130), 0.4)
    if style not in ("hooded", "visor"):
        for i in (0, 1, 8, 9):
            face_px(i, 6, blush, 255, None, 0)
    mark = spec.get("mark", "none")
    if mark == "whisker":
        for i in (0, 1, 8, 9):
            face_px(i, 7, mul(skin, 0.62), 255, None, 0)
        for i in (0, 9):
            face_px(i, 8, mul(skin, 0.62), 255, None, 0)
    elif mark == "paint":
        for i in range(0, 4):
            face_px(i, 6, mix(P["accent"], skin, 0.2), 255, P["accent"], 0.5)
        for i in range(6, 10):
            face_px(i, 6, mix(P["accent"], skin, 0.2), 255, P["accent"], 0.5)
    elif mark == "runes":
        for (i, j) in ((4, 1), (5, 1), (4, 0), (5, 0)):
            face_px(i, j, P["glow"], 255, P["glow"], 1.0)
    elif mark == "scar":
        for k in range(4):
            face_px(7 + (k // 2), 3 + k, mul(skin, 0.55), 255, None, 0)
    elif mark == "tears":
        for j in (6, 7, 8):
            face_px(1, j, P["glow"], 255, P["glow"], 0.6)
            face_px(8, j, P["glow"], 255, P["glow"], 0.6)
    elif mark == "star":
        face_px(4, 1, P["glow"], 255, P["glow"], 1.0)
        face_px(5, 1, P["glow"], 255, P["glow"], 1.0)
        face_px(4, 0, P["glow"], 255, P["glow"], 0.6)
        face_px(5, 0, P["glow"], 255, P["glow"], 0.6)
    beard = spec.get("beard")
    if beard:
        bc = hexrgb_safe(beard)
        for j in range(7, 9):
            for i in range(2, 8):
                if j == 7 and i in (4, 5):
                    continue
                face_px(i, j, mul(bc, 0.95 + 0.06 * hsh(i, j, 3)), 255, None, 0)


def hexrgb_safe(h):
    from tdlib import hexrgb
    return hexrgb(h)


def head_painter(spec):
    cover = spec.get("cover", "hair")
    hair_fringe = spec.get("fringe", "bangs")

    def painter(T, P, rects, _spec=None):
        skin, sk2 = P["skin"], P["skin2"]
        hair, h2 = P["hair"], P["hair2"]
        cloth, metal = P["cloth"], P["metal"]

        def cover_px(face, i, j, fw, fh):
            """What covers the head at (face, i, j) or None for bare skin. Returns (rgb, glow)."""
            top_rows = 4 if face in ("right", "left") else fh
            if cover == "bald":
                return None
            if cover == "hood":
                if face == "front":
                    edge = j < 2 or i < 1 or i > fw - 2
                    return (mul(cloth, 0.8 + 0.3 * hsh(i, j, 5)), None) if edge else None
                if face in ("right", "left"):
                    return (mul(cloth, 0.82 + 0.25 * hsh(i, j, 6)), None) if (i < 7 or j < 5) else None
                return (mul(cloth, 0.78 + 0.3 * hsh(i, j, 7)), None)
            if cover == "helm":
                if face == "front":
                    edge = j < 2 or (j < 5 and (i < 1 or i > fw - 2))
                    if i in (4, 5) and 2 <= j <= 5:
                        return (mul(metal, 1.25), None)           # the nasal bar
                    return (mul(metal, 1.1 + 0.3 * (1 - j / 3.0) * hsh(i, j, 8)), None) if edge else None
                if face in ("right", "left"):
                    return (mul(metal, 0.9 + 0.35 * hsh(i, j, 9)), None) if j < 6 else None
                return (mul(metal, 0.85 + 0.3 * hsh(i, j, 10)), None)
            # hair
            if face == "front":
                if j == 0:
                    return (mul(hair, 0.95 + 0.2 * hsh(i, j, 1)), None)
                if j == 1:
                    return (mul(h2 if hsh(i, 1, 2) > 0.7 else hair, 1.0), None) if (hair_fringe == "bangs" and hsh(i, j, 4) > 0.35) or hair_fringe == "full" else None
                if hair_fringe == "full" and j == 2 and hsh(i, j, 4) > 0.6:
                    return (hair, None)
                return None
            if face in ("right", "left"):
                if j < top_rows - (1 if i > 5 else 0):
                    return (mul(hair if (i + j) % 3 else h2, 0.92 + 0.2 * hsh(i, j, 11)), None)
                return (mul(hair, 0.8), None) if i > 6 and j < 7 else None
            return (mul(hair if (i * 2 + j) % 4 else h2, 0.9 + 0.22 * hsh(i, j, 12)), None)

        def fn(face, i, j, fw, fh):
            c = cover_px(face, i, j, fw, fh)
            if c is not None:
                return (c[0], 255, c[1], 1.0) if c[1] else (c[0], 255)
            base = mix(skin, sk2, (j / fh) * 0.5)
            base = mul(base, edge_ok(i, j, fw, fh) * (0.97 + 0.06 * hsh(i, j, 13)))
            if face == "bottom":
                base = mul(sk2, 0.8)
            return (base, 255)

        def edge_ok(i, j, fw, fh):
            return 0.94 if (i == 0 or j == fh - 1) and min(fw, fh) > 4 else 1.0

        fill_box(T, rects, fn)
        # the face is drawn over the skin of the front
        x0, y0, fw, fh = rects["front"]
        face_k = 1.0

        def face_px(i, j, rgb, a=255, g=None, gk=1.0):
            if 0 <= i < fw and 0 <= j < fh:
                T.px(x0 + i, y0 + j, mul(rgb, face_k), a, g, gk)

        _eyes(spec, P, face_px)

    return painter


# ================================================================ the body
def torso_painter(spec):
    chest = spec.get("chest", "robe")

    def painter(T, P, rects, _spec=None):
        cloth, cloth2, trim, metal, skin = P["cloth"], P["cloth2"], P["trim"], P["metal"], P["skin"]

        def fn(face, i, j, fw, fh):
            n = _n(i, j, 21, 0.05)
            belt = j >= fh - 1 and face not in ("top", "bottom")
            if face in ("top", "bottom"):
                return (mul(cloth2, n), 255)
            if belt:
                if face == "front" and i in (2, 3):
                    return (P["accent"], 255, P["accent"], 1.0)
                return (mul(trim, 0.95 * n), 255)
            if face == "back":
                base = mix(cloth2, cloth, 0.3)
                if i in (2, 3) and 1 <= j <= 3:
                    return (mul(trim, 0.9), 255, P["glow"], 0.55) if spec.get("backrune", True) else (mul(base, n), 255)
                return (mul(base, n * (1 - 0.04 * j)), 255)
            if face in ("right", "left"):
                return (mul(cloth, n * (1 - 0.05 * j)), 255)
            # the front
            if chest == "plate":
                if j == 0:
                    return (mul(trim, 1.05 * n), 255)
                if i in (0, 5):
                    return (mul(trim, 0.85 * n), 255)
                if i in (2, 3) and j in (2, 3):
                    return (P["glow"], 255, P["glow"], 1.0)
                return (mul(metal, (1.35 - 0.09 * j) * n), 255)
            if chest == "robe":
                if j <= 1 and i in (2, 3):
                    return (skin, 255)
                if i in (1, 4) and j <= 3:
                    return (mul(trim, 0.95 * n), 255)
                return (mul(cloth, n * (1 - 0.05 * j)), 255)
            if chest == "vest":
                if i in (2, 3) and j <= 3:
                    return (mul(skin, 0.95 - 0.04 * j), 255)
                return (mul(cloth, n * (1 - 0.05 * j)), 255)
            if chest == "harness":
                if (i == j) or (i == 5 - j):
                    return (mul(trim, 0.95 * n), 255)
                return (mul(cloth, n * (1 - 0.05 * j)), 255)
            return (mul(cloth, n), 255)

        fill_box(T, rects, fn)

    return painter


def arm_painter(spec):
    glove = spec.get("glove", False)

    def painter(T, P, rects, _spec=None):
        def fn(face, i, j, fw, fh):
            n = _n(i, j, 31, 0.05)
            if face in ("top", "bottom"):
                return (mul(P["cloth"], n), 255)
            if j == fh - 1:
                return (mul(P["trim"], 0.95 * n), 255)
            return (mul(P["cloth"], n * (1 - 0.06 * j)), 255)
        fill_box(T, rects, fn)

    return painter


def hand_painter(spec):
    glove = spec.get("glove", False)

    def painter(T, P, rects, _spec=None):
        def fn(face, i, j, fw, fh):
            n = _n(i, j, 41, 0.04)
            if glove:
                return (mul(P["metal"], 1.1 * n), 255)
            return (mul(P["skin"], n), 255)
        fill_box(T, rects, fn)

    return painter


# ================================================================ the wisp tail
def tail_painter(spec, seg):
    style = spec.get("tail", "smoke")
    alpha_k = (1.0, 0.93, 0.78, 0.55)[seg] if style in ("smoke", "water", "wind", "flame", "tendril") else (1.0, 1.0, 0.97, 0.92)[seg]

    def painter(T, P, rects, _spec=None):
        cloth, cloth2, glow, acc = P["cloth"], P["cloth2"], P["glow"], P["accent"]
        t0 = seg / 3.0

        def fn(face, i, j, fw, fh):
            g = None
            gk = 0.0
            n = _n(i, j, 51 + seg, 0.06)
            k = (seg + j / max(1.0, fh)) / 4.0                      # 0 at the waist .. 1 at the tip
            if style == "flame":
                c = mix((255, 150, 24), (255, 238, 140), k) if not P["dark"] else mix((150, 40, 60), (255, 120, 90), k)
                c = mix(c, (255, 80, 20), 0.3 * (hsh(i, j, seg) > 0.7))
                g, gk = c, 0.85
            elif style == "water":
                c = mix((46, 128, 214), (196, 244, 255), k)
                if (i + j * 2 + seg * 3) % 5 == 0:
                    c = mix(c, (255, 255, 255), 0.6)
                if P["dark"]:
                    c = mix(c, (30, 30, 70), 0.55)
                g, gk = c, 0.25
            elif style == "wind":
                c = mix((190, 238, 222), (250, 255, 252), k)
                if (i + j + seg * 2) % 4 < 1:
                    c = mix(c, (110, 200, 180), 0.55)
                if P["dark"]:
                    c = mix(c, (50, 40, 70), 0.6)
            elif style == "bolt":
                c = mix(mix(cloth, (18, 16, 56), 0.5), mix(cloth2, (120, 150, 255), 0.5), k)
                if (i + 2 * j + seg * 3) % 6 == 0 or (hsh(i, j, seg + 9) > 0.93):
                    c = (255, 255, 214)
                    g, gk = c, 1.0
            elif style == "ice":
                c = mix((150, 210, 244), (244, 252, 255), k)
                if (i * 2 + j) % 6 == 0:
                    c = mix(c, (255, 255, 255), 0.7)
                if P["dark"]:
                    c = mix(c, (40, 40, 80), 0.55)
                g, gk = c, 0.2
            elif style == "blade":
                c = mix(mul(P["metal"], 1.7), (236, 240, 250), k)
                if i in (fw // 2, fw // 2 - 1) and seg < 3:
                    c = mix(c, (255, 255, 255), 0.5)
                g, gk = glow, 0.0
            elif style == "vine":
                c = mix((70, 150, 70), (140, 210, 90), k)
                if (i + j) % 4 == 0:
                    c = mul(c, 0.7)
                if P["dark"]:
                    c = mix(c, (30, 30, 50), 0.6)
            elif style == "tendril":
                c = mix(mul(cloth, 0.8), mul(acc, 0.9), k)
                if j % 3 == 0:
                    c = mul(c, 0.82)
                g, gk = acc, 0.3 * k
            elif style == "feather":
                c = mix(mix(cloth2, cloth, 0.5), P["trim"], k * 0.6)
                if (i + j) % 3 == 0:
                    c = mul(c, 1.2)
            else:  # smoke
                c = mix(mix(cloth, cloth2, 0.4), mix(cloth2, (232, 232, 246), 0.45), k)
                if hsh(i, j, seg + 5) > 0.82:
                    c = mul(c, 0.8)
                g, gk = glow, 0.0
            a = int(255 * alpha_k * (0.95 if face == "bottom" else 1.0))
            if style in ("flame", "water", "wind", "smoke", "tendril") and k > 0.5 and hsh(i, j, 77 + seg) > 1.15 - k * 0.9:
                a = int(a * 0.5)
            return (mul(c, n), max(30, a), g, gk) if g else (mul(c, n), max(30, a))

        fill_box(T, rects, fn)

    return painter


# ================================================================ the cape
def cape_painter(spec):
    def painter(T, P, rects, _spec=None):
        style = spec.get("cape", "cape")

        def fn(face, i, j, fw, fh):
            n = _n(i, j, 61, 0.05)
            if face in ("top", "bottom", "right", "left"):
                return (mul(P["cloth2"], 0.9 * n), 255)
            c = mix(P["cloth2"], P["cloth"], 0.35 + 0.4 * (j / max(1, fh - 1)))
            if j >= fh - 2:
                return (mul(P["trim"], 0.95 * n), 255)
            if face == "back" and i in (3, 4) and j in (2, 3, 4):
                return (P["glow"], 255, P["glow"], 0.8)
            if style in ("tattered", "icecloak") and j > fh - 4 and hsh(i, j, 62) > 0.55 + 0.1 * (fh - j):
                return (c, 0)
            return (mul(c, n), 255)
        fill_box(T, rects, fn)

    return painter
