"""Charlos (Saint Charlos, a Celestial Dragon of One Piece). Hand-painted: cream-white suit with a huge ruffled collar, gold epaulettes with fringe and puffed ruffle cuffs, a crimson waistcoat, sash and
cape with a gold sun crest, white trousers with a gold stripe, crimson boots with gold cuffs, a gold circlet with a ruby over curly golden hair. Look from memory (no wiki could be fetched)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_ga as L  # noqa: E402

hx = L.hx
FR = ("front", "back", "west", "east")


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    W = [hx(c) for c in ("#a89a80", "#c4b9a0", "#d9d0ba", "#e8e1cd", "#f6f1e2", "#fffef4")]
    G, R = L.ramp("#e0b030"), L.ramp("#b82a3c")
    S = L.skin_ramp("#f4cdae")
    L.paint_base_skin(sk, S)
    hair = hx("#ffe040")
    HT = L.ramp(hair, 5)
    lg = {"W": W[5], "w": W[4], "s": W[3], "d": W[2], "D": W[1], "G": G[5], "g": G[4], "h": G[3], "o": G[2], "O": G[1],
          "R": R[4], "r": R[3], "q": R[2], "Q": R[1], "S": S[3], "n": S[1], "Y": G[5], "x": R[5]}

    # ------------------------------------------------------------ head
    L.scalp(sk, HT)
    eye = (hx("#7a3e1a"), hx("#b5702c"), hx("#e8a850"))
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else None)
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    bro = L.ramp(hair, 5)[1]
    L.brows(sk, 3, bro, "flat")
    sk.put("head", "front", False, 6, 2, bro)     # the right brow lifted: a smug look
    L.eyes(sk, 4, eye, hx("#4a2616"), "round")
    for x in (0, 7):
        sk.put("head", "front", False, x, 6, L.mix(S[3], hx("#ee8c88"), 0.5))
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])          # the nose
    for x, c in ((2, S[2]), (3, hx("#a84c4a")), (4, hx("#a84c4a")), (5, hx("#d8847a")), (6, hx("#b8605a"))):  # a smug, lopsided smile
        sk.put("head", "front", False, x, 7, c)
    # circlet on the hat layer (row 2 round the head, a ruby in front); the top of the hat layer stays empty
    for face_ in FR:
        for x in range(8):
            if face_ == "front" and x in (3, 4):
                sk.put("head", face_, True, x, 2, hx("#ff6a78") if x == 3 else hx("#c81c40"), glow=True)
            else:
                sk.put("head", face_, True, x, 2, G[5] if (x % 2 == 0) else G[4])

    # the neck ring of the bubble helmet: a white-gold rim round the chin on the hat layer (the 3D hair stays free above it)
    for face_ in FR:
        for x in range(8):
            sk.put("head", face_, True, x, 7, W[5] if x % 2 == 0 else G[5])
    sk.put("head", "front", True, 3, 7, G[3]); sk.put("head", "front", True, 4, 7, G[3])

    # ------------------------------------------------------------ body: ruffled collar, waistcoat, sash, cape
    for ov in (False, True):
        sk.cloth("body", ov, W, folds=(2, 5))
    front = [
        "WWWWWWWW",
        "wWwWWwWw",
        "dsdYYdsd",
        "wgRRRRgw",
        "wgRYRRgw",
        "wgRRRRgw",
        "wgRYRRgw",
        "wgrRRrgw",
        "rRRxRRRr",
        "RrgYYgrR",
        "wWwsswWw",
        "gGgGgGgG",
    ]
    for ov in (True, False):
        sk.art("body", "front", ov, front, lg, glow_chars="Y")
    back = [
        "WWWWWWWW",
        "wWwWWwWw",
        "dsdsdsds",
        "gRRRRRRg",
        "gRRxxRRg",
        "gRxYYxRg",
        "gRYggYRg",
        "gRYggYRg",
        "gRxYYxRg",
        "gRRrrRRg",
        "gQRrrRQg",
        "GgGgGgGg",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg, glow_chars="Y")
    side = ["WwWw", "wWwW", "dsds", "wWwW", "wWwW", "wWwW", "wWwW", "wWwW", "rRRr", "rRRr", "wWws", "gGgG"]
    for face_ in ("west", "east"):
        for ov in (True, False):
            sk.art("body", face_, ov, side, lg)
    sk.fn("body", "top", True, lambda x, y, w, h: W[5] if (x + y) % 2 == 0 else W[4])

    # ------------------------------------------------------------ arms: gold epaulettes with fringe, puffed white sleeves, ruffle cuffs
    sleeve = [
        "GYYG",
        "gGGg",
        "ghgh",
        "hOoO",
        "OwOd",     # fringe of the epaulette
        "WwwW",
        "wWsw",
        "wWWs",
        "wsds",
        "gGGg",     # gold cuff band
        "WwWw",     # ruffle
        "dsWd",
    ]
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            sk.cloth(part, ov, W, y0=0, y1=11)
        for face_ in FR:
            sk.art(part, face_, True, sleeve, lg, glow_chars="Y")
        sk.fn(part, "top", True, lambda x, y, w, h: G[5] if (x + y) % 2 == 0 else G[4])
        for face_ in FR:                                                      # the hand shows under the ruffle
            for x in range(4):
                sk.cv.put(sk.rect(part, face_, True)[0] + x, sk.rect(part, face_, True)[1] + 11, (0, 0, 0, 0))
                sk.put(part, face_, False, x, 11, S[3] if face_ == "front" else S[2])
        sk.put(part, "front", False, 1, 11, S[4])

    # ------------------------------------------------------------ legs: white trousers, gold stripe, crimson boots, gold cuffs
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            sk.cloth(part, ov, W, y0=0, y1=8, folds=(1,), light=1)
            sk.cloth(part, ov, R, y0=8, y1=12, faces=("front", "back", "west", "east", "bottom"))
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, True, x, 7, G[5] if x % 2 == 0 else G[4])
                sk.put(part, face_, True, x, 8, G[4] if x % 2 == 0 else G[3])
                sk.put(part, face_, True, x, 9, R[4] if x not in (0, 3) else R[3])
                sk.put(part, face_, True, x, 10, R[3] if x not in (0, 3) else R[2])
                sk.put(part, face_, True, x, 11, R[2])
        sk.put(part, "front", True, 1, 10, R[5]); sk.put(part, "front", True, 1, 9, R[5])
        stripe = "west" if part == "leg_r" else "east"
        for y in range(0, 7):
            sk.put(part, stripe, True, 1, y, G[4]); sk.put(part, stripe, True, 2, y, G[3])

    img, glow = sk.finish()
    return img, glow, "anime_short_open", hair, False
