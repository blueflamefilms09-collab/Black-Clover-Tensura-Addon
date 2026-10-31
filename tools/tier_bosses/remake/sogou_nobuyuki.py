"""Sogou (Sugou) Nobuyuki as Oberon, the fairy king of Alfheim in Sword Art Online: long wavy golden hair under a gold crown with a red jewel, a smirking face with violet eyes, a deep-violet long
coat with gold piping, a tall gold collar and a white jabot with a gem, gold pauldrons and gold gauntlets over white gloves, a black belt with a gold buckle, black trousers, black boots with gold cuffs,
and the translucent pale-blue dragonfly wings drawn on the back (glowing). Look from memory (no wiki could be fetched)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_ga as L  # noqa: E402

hx = L.hx
FR = ("front", "back", "west", "east")


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    S = L.skin_ramp("#f6d8c2")
    V = [hx(c) for c in ("#1e0c40", "#35176a", "#4d2594", "#6a38b8", "#8a56d6", "#b08af0")]      # the violet coat
    G = [hx(c) for c in ("#6a4a08", "#9a7010", "#c89a20", "#e8b830", "#ffd860", "#fff4b0")]        # gold
    WH = [hx(c) for c in ("#a8a0c0", "#c4bedb", "#dcd8ec", "#eeebf8", "#fbfaff", "#ffffff")]       # white shirt, gloves
    K = [hx(c) for c in ("#06040c", "#100a1c", "#1c1230", "#2a1c46", "#3c2a60", "#523e80")]         # black-violet trousers, boots, belt
    JW = [hx("#7a0a2e"), hx("#c01444"), hx("#ee3868"), hx("#ff8aa8")]                              # the red gem
    WG = [hx(c) for c in ("#5aa4cc", "#86c8ea", "#b8e6fa", "#e0f6ff")]                              # wing: vein, mid, light, shine
    L.paint_base_skin(sk, S)
    hair = hx("#ffd650")
    HT = L.ramp(hair, 5)
    lg = {"G": G[5], "g": G[4], "h": G[3], "o": G[2], "O": G[1], "q": G[0],
          "V": V[5], "v": V[4], "u": V[3], "t": V[2], "s": V[1], "r": V[0],
          "W": WH[5], "w": WH[4], "d": WH[3], "D": WH[2], "z": WH[1],
          "k": K[3], "K": K[2], "j": K[4], "i": K[5], "x": K[1], "X": K[0],
          "J": JW[3], "e": JW[2], "E": JW[1], "a": WG[3], "b": WG[2], "c": WG[1], "l": WG[0], "A": WG[3]}

    # ------------------------------------------------------------ head: long golden hair (3D), a crown, a sneering face
    L.scalp(sk, HT, front_rows=2, side_rows=5, back_rows=8)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else None)
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    brow = L.ramp(hair, 5)[1]
    for outer, inner in ((1, 2), (6, 5)):                      # thin, angled brows
        sk.put("head", "front", False, outer, 3, brow); sk.put("head", "front", False, inner, 3, brow)
    L.eyes(sk, 4, (hx("#4a1c8a"), hx("#8a46d0"), hx("#c89cf4")), hx("#3a2410"), "sharp", glow=True)
    for x in (0, 7):
        sk.put("head", "front", False, x, 6, L.mix(S[3], hx("#ee98a0"), 0.35))
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])
    for x, c in ((2, S[2]), (3, hx("#b0585c")), (4, hx("#c0686a")), (5, hx("#9c4a52")), (6, S[2])):       # a thin cruel smile, one corner raised
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 6, 6, hx("#c8847c"))
    # the crown on the hat layer: a band on row 2, points on row 1, the red gem in front
    for face_ in FR:
        for x in range(8):
            if face_ == "front" and x in (3, 4):
                sk.put("head", face_, True, x, 2, JW[3] if x == 3 else JW[2], glow=True)
                sk.put("head", face_, True, x, 1, G[5])
            else:
                sk.put("head", face_, True, x, 2, G[4] if x % 2 == 0 else G[3])
                if x % 2 == 1 and face_ == "front":
                    sk.put("head", face_, True, x, 1, G[5])

    # ------------------------------------------------------------ body: violet coat, gold stand collar, white jabot, belt
    for ov in (False, True):
        sk.cloth("body", ov, V, folds=(1, 6))
    front = [
        "gGGGGGGg",
        "GuWwwWuG",
        "vodweEov",      # jabot + gem
        "vuWwwduv",
        "tudwWdut",
        "tuDwwDut",
        "uuWdDduu",
        "tuuDDuut",
        "XkKGGKkX",      # belt with a gold buckle
        "XkKhhKkX",
        "uvOxxOvu",
        "tuGxxGut",
    ]
    front[10] = "uvOxxOvu"
    front[11] = "tuGxxGut"
    for ov in (True, False):
        sk.art("body", "front", ov, front, lg, glow_chars="e")
    # the coat skirt opens over the black trousers at the bottom: the centre rows are dark
    sk.art("body", "front", True, ["uvOxxOvu", "tuGxxGut"], lg, oy=10)
    back = [
        "gGGGGGGg",
        "vabGGbav",
        "abcGGcba",
        "abbGGbba",
        "abcGGcba",
        "vabGGbav",
        "vvaGGavv",
        "vabGGbav",
        "vacGGcav",
        "vvaGGavv",
        "uvvGGvvu",
        "GgGgGgGg",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg, glow_chars="abc")
    # the dragonfly wings keep their shine: a few bright texels along the leading edge of each wing
    for ov in (True,):
        for (x, y) in ((1, 2), (6, 2), (0, 3), (7, 3), (1, 7), (6, 7)):
            sk.put("body", "back", ov, x, y, WG[3], glow=True)
    side = ["gGGg", "GvvG", "ugGu", "uOgu", "uOgu", "uOgu", "uOgu", "uOgu", "XkKk", "XkKk", "uvOu", "tuGu"]
    for face_ in ("west", "east"):
        for ov in (True, False):
            sk.art("body", face_, ov, side, lg)
    sk.fn("body", "top", True, lambda x, y, w, h: G[5] if (x in (0, w - 1) or y == 0) else G[4])

    # ------------------------------------------------------------ arms: gold pauldrons, violet sleeves, gold gauntlets, white gloves
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            sk.cloth(part, ov, V, y0=0, y1=11, folds=(1, 2), light=1)
        pauld = [
            "GggG",
            "ghhg",
            "gohg",
            "hOoh",
        ]
        gaunt = [
            "ggGg",      # row 8: the rim
            "ghGo",
            "hoGO",
            "hOoq",
            "WwdW",      # the glove
        ]
        for face_ in FR:
            sk.art(part, face_, True, pauld, lg)
            sk.art(part, face_, True, gaunt, lg, oy=7)
        sk.fn(part, "top", True, lambda x, y, w, h: G[5] if (x + y) % 2 == 0 else G[4])
        sk.put(part, "front", True, 1, 2, JW[3], glow=True); sk.put(part, "front", True, 2, 2, JW[2])
        for face_ in FR:                                                       # the white glove on the base layer too
            for x in range(4):
                sk.put(part, face_, False, x, 11, WH[4] if face_ == "front" else WH[3])
                sk.put(part, face_, True, x, 11, WH[4] if face_ == "front" else WH[3])

    # ------------------------------------------------------------ legs: black trousers, gold side stripe, tall black boots with gold cuffs
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            sk.cloth(part, ov, K, folds=(1,), light=1)
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, True, x, 6, G[5] if x % 2 == 0 else G[4])           # gold cuff of the boot
                sk.put(part, face_, True, x, 7, G[4] if x % 2 == 0 else G[3])
                sk.put(part, face_, True, x, 8, G[2])
                sk.put(part, face_, True, x, 9, K[4] if x in (1, 2) else K[3])
                sk.put(part, face_, True, x, 10, K[3] if x in (1, 2) else K[2])
                sk.put(part, face_, True, x, 11, K[2] if x in (1, 2) else K[1])
        sk.put(part, "front", True, 1, 9, K[5]); sk.put(part, "front", True, 1, 10, K[5]); sk.put(part, "front", True, 2, 11, K[5])
        stripe = "west" if part == "leg_r" else "east"
        for y in range(0, 6):
            sk.put(part, stripe, True, 1, y, G[3]); sk.put(part, stripe, True, 2, y, G[2])

    img, glow = sk.finish()
    return img, glow, "anime_swept", hair, False
