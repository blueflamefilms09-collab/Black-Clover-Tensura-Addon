"""Kurozumi Orochi (One Piece, the Shogun of Wano): a pale, heavy-browed face with slit eyes, a thin drooping moustache and a goatee, a black topknot, a deep-purple kimono patterned with lavender
snake scales (uroko), a crossed white collar, winged indigo shoulders (kataginu) with a lavender edge, a wide lavender obi with a gold cord, wide hanging sleeves with lilac linings, a gold-and-white
eight-headed-snake crest on the back, indigo pleated hakama, white tabi and wooden geta. Look from memory (no wiki could be fetched)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_ga as L  # noqa: E402

hx = L.hx
FR = ("front", "back", "west", "east")


def scales(sk, part, face, overlay, y0, y1, c_hi, c_lo, off=0):
    """The snake-scale (uroko) pattern: rows of small triangles, a bright tip over a dark base, staggered every other row."""
    x0, y0r, w, h = sk.rect(part, face, overlay)
    for y in range(y0, min(y1, h)):
        for x in range(w):
            k = (x + (2 if (y // 2) % 2 else 0) + off) % 4
            if y % 2 == 0 and k == 1:
                sk.put(part, face, overlay, x, y, c_hi)
            elif y % 2 == 1 and k in (0, 2):
                sk.put(part, face, overlay, x, y, c_lo) if k == 0 else None


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    S = L.skin_ramp("#f2d4be")
    P = [hx(c) for c in ("#1c0a36", "#321258", "#4a1c80", "#6428a8", "#8240c8", "#a866e6")]       # kimono purple
    I = [hx(c) for c in ("#07051a", "#0f0a2a", "#1a1244", "#281c60", "#3a2a82", "#5440a8")]        # indigo (shoulders, hakama)
    LV = [hx(c) for c in ("#8a64b8", "#a884d4", "#c4a4ea", "#dcc4fa", "#eedcff", "#fbf2ff")]        # lavender lining, obi
    W = [hx(c) for c in ("#a8a4c0", "#c4c0d8", "#dcd8ec", "#eeeaf8", "#faf8ff", "#ffffff")]         # white collar, tabi
    G = [hx(c) for c in ("#6a4a08", "#9a7010", "#c89a20", "#e8b830", "#ffd860", "#fff4b0")]
    WD = [hx(c) for c in ("#2e1c10", "#4a2e1a", "#6a4428", "#8a5a34")]                              # wood
    L.paint_base_skin(sk, S)
    hair = hx("#30304a")
    HT = L.ramp(hair, 5)
    lg = {"p": P[3], "P": P[4], "q": P[2], "r": P[1], "s": P[5], "u": I[2], "U": I[3], "v": I[4], "V": I[5], "t": I[1], "T": I[0],
          "l": LV[2], "L": LV[3], "m": LV[4], "n": LV[1], "o": LV[0], "W": W[5], "w": W[4], "d": W[3], "D": W[2], "z": W[1],
          "G": G[5], "g": G[4], "h": G[3], "i": G[2], "j": G[1], "b": WD[3], "B": WD[2], "c": WD[1], "C": WD[0]}

    # ------------------------------------------------------------ head
    L.scalp(sk, HT)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else None)
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    ink = hx("#1a1424")
    for outer, inner in ((1, 2), (6, 5)):                      # thick, heavy brows
        sk.put("head", "front", False, outer, 3, ink); sk.put("head", "front", False, inner, 3, ink)
    sclera = hx("#f6eee8")
    for outer, inner in ((1, 2), (6, 5)):                      # narrow slit eyes: a dark lid line, a pale white, a dark red-brown iris on the outer side
        sk.put("head", "front", False, outer, 4, hx("#1a1018")); sk.put("head", "front", False, inner, 4, hx("#2a1c24"))
        sk.put("head", "front", False, outer, 5, hx("#6a2a30")); sk.put("head", "front", False, inner, 5, sclera)
    for x in (0, 7):
        sk.put("head", "front", False, x, 6, S[2])
    sk.put("head", "front", False, 3, 5, S[2]); sk.put("head", "front", False, 4, 5, S[2])        # the nose
    sk.put("head", "front", False, 2, 6, ink); sk.put("head", "front", False, 5, 6, ink)            # the moustache droops at the ends
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])
    sk.put("head", "front", False, 3, 7, ink); sk.put("head", "front", False, 4, 7, ink)            # the goatee
    sk.put("head", "front", False, 2, 7, hx("#b06468")); sk.put("head", "front", False, 5, 7, hx("#b06468"))

    # ------------------------------------------------------------ body: purple kimono with scales, winged indigo shoulders, crossed white collar, obi
    for ov in (False, True):
        sk.cloth("body", ov, P, folds=(2, 5), light=1)
    front = [
        "uWwddwWu",
        "uvDWWDvu",
        "uvqDWqvu",
        "uvqqDqvu",
        "tuqqqqut",
        "tuqqqqut",
        "tuqpqqut",
        "tuqqpqut",
        "oLLLLLLo",      # the obi
        "nlgGGglm"[:8],
        "pppqqppp",
        "qpqpqpqp",
    ]
    front[9] = "ollGGllo"
    for ov in (True, False):
        sk.art("body", "front", ov, front, lg)
    # the crossed collar: a white diagonal from each shoulder to the centre, the top lapel edged in lavender
    diag = [(1, 1), (2, 2), (3, 3), (4, 4), (4, 5), (6, 1), (5, 2), (4, 3)]
    for ov in (True, False):
        for (x, y) in diag:
            sk.put("body", "front", ov, x, y, W[5] if y < 3 else LV[3])
        sk.put("body", "front", ov, 4, 5, LV[2]); sk.put("body", "front", ov, 3, 4, W[4]); sk.put("body", "front", ov, 5, 3, W[4])
        scales(sk, "body", "front", ov, 5, 8, P[4], P[2], off=1)
        scales(sk, "body", "front", ov, 10, 12, P[4], P[2])
    sk.art("body", "front", True, ["....gg..", "....hg.."], lg, oy=8)         # the gold cord on the obi
    back = [
        "uWwddwWu",
        "uvDWWDvu",
        "uvqpGGqu".replace("G", "g")[:8],
        "uvpgGGgu".replace("G", "G")[:8],
        "tuqGWWGu"[:8],
        "tuqgGGgu"[:8],
        "tuqpggqu"[:8],
        "tuLLLLLu"[:8],
        "oLmmmmLo",
        "olLmmLlo",
        "pppLLppp",
        "qpqpqpqp",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg)
    # the snake crest on the back: a ring of eight gold heads round a white centre (rows 2-6, x1-6)
    ring = [
        ".gGGgG..",
        "gG.hh.G.",
        "G.hWWh.g",
        "Gh.WW.hG",
        "g.hWWh.G",
        "gG.hh.Gg",
        ".gGGGg..",
    ]
    ring = [".gGGGg..", "gG.hh.Gg", "G.hWWh.G", "GhWWWWhG", "G.hWWh.G", "gG.hh.Gg", ".gGGGg.."]
    for ov in (True, False):
        for y, row in enumerate(ring):
            for x, ch in enumerate(row):
                if ch != "." and x < 8:
                    sk.put("body", "back", ov, x, y + 1, lg[ch])
    for ov in (True, False):
        for y in range(8, 10):                                   # the big obi knot (musubi) fills the small of the back
            for x in range(1, 7):
                sk.put("body", "back", ov, x, y, LV[4] if y == 8 else LV[2])
        sk.put("body", "back", ov, 3, 8, G[4]); sk.put("body", "back", ov, 4, 8, G[4]); sk.put("body", "back", ov, 3, 9, G[3]); sk.put("body", "back", ov, 4, 9, G[3])
    side = ["uWwu", "uvDv", "tuqp", "tuqp", "tuqp", "tuqp", "tuqp", "tuqp", "oLLo", "olLo", "pqpq", "qpqp"]
    for face_ in ("west", "east"):
        for ov in (True, False):
            sk.art("body", face_, ov, side, lg)
    sk.fn("body", "top", True, lambda x, y, w, h: W[5] if (x in (0, w - 1) or y in (0, h - 1)) else W[4])

    # ------------------------------------------------------------ arms: indigo shoulder wings, wide purple sleeves, lilac lining cuffs
    shoulder = [
        "VvUv",
        "vUuU",
        "uUuu",
        "nlnl",         # lavender edge of the shoulder wing
    ]
    cuff = [
        "nlLl",
        "mLlL",
        "ozoz",
        "TtTt",
    ]
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            sk.cloth(part, ov, P, y0=0, y1=11, folds=(1,), light=1)
        for face_ in FR:
            sk.art(part, face_, True, shoulder, lg)
            sk.art(part, face_, True, cuff, lg, oy=8)
            scales(sk, part, face_, True, 4, 8, P[4], P[2])
        sk.fn(part, "top", True, lambda x, y, w, h: I[5] if (x + y) % 2 == 0 else I[4])
        for face_ in FR:                                           # the hand shows below the wide cuff
            for x in range(4):
                sk.cv.put(sk.rect(part, face_, True)[0] + x, sk.rect(part, face_, True)[1] + 11, (0, 0, 0, 0))
                sk.put(part, face_, False, x, 11, S[3] if face_ == "front" else S[2])
        sk.put(part, "front", False, 1, 11, S[4])
        sk.put(part, "front", True, 0, 3, G[4]); sk.put(part, "front", True, 3, 3, G[4])

    # ------------------------------------------------------------ legs: indigo pleated hakama, lilac hem line, white tabi, wooden geta
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            sk.cloth(part, ov, I, y0=0, y1=10, folds=(1, 2), light=1)
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, True, x, 9, LV[2] if x % 2 == 0 else LV[1])
                sk.put(part, face_, True, x, 10, I[3] if x % 2 == 0 else I[2])
                sk.put(part, face_, True, x, 11, W[4] if face_ == "front" else W[3])
                sk.put(part, face_, False, x, 11, W[4] if face_ == "front" else W[3])
        for y in range(0, 9, 2):
            sk.put(part, "front", True, 1, y, I[4]); sk.put(part, "front", True, 2, y + 1, I[1])
        sk.put(part, "front", True, 0, 11, WD[3]); sk.put(part, "front", True, 3, 11, WD[3])
        for x in range(4):
            sk.put(part, "bottom", True, x, 0, WD[2]); sk.put(part, "bottom", True, x, 3, WD[1])

    img, glow = sk.finish()
    # the glow map: the lilac trim and the gold cord and crest shine softly (the sheet's trim glow)
    for part, face_, ov, pts in (("body", "front", True, [(x, 8) for x in range(8)]),):
        for (x, y) in pts:
            c = sk.get(part, face_, ov, x, y)
            sk.glow[sk.rect(part, face_, ov)[1] + y, sk.rect(part, face_, ov)[0] + x] = c[:3] + (255,)
            sk.any_glow = True
    img, glow = sk.finish()
    return img, glow, "anime_topknot", hair, False
