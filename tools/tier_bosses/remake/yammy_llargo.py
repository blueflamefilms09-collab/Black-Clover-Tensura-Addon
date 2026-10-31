"""Yammy Llargo (Bleach, the Espada's Cero Espada 0 / 10th): a huge tanned brute with spiky black hair, a bone mask fragment on the side of the head, the white Espada jacket worn open over a bare
muscular chest with the numeral 10 on the belly (glowing orange), black wrist guards, a black sash over wide white hakama and sandals. Look from memory (no wiki could be fetched)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_ga as L  # noqa: E402

hx = L.hx
FR = ("front", "back", "west", "east")


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    T = [hx(c) for c in ("#73432a", "#945a36", "#b2754a", "#cc9058", "#e2ac70")]          # tanned skin dark..light
    W = [hx(c) for c in ("#a9a8b4", "#c4c3cd", "#dcdbe2", "#eeeef2", "#fbfbfd", "#ffffff")]   # white jacket / hakama, cool
    K = [hx(c) for c in ("#0a0a10", "#15151e", "#22222e", "#323244", "#46465c", "#5e5e78")]     # black
    O = [hx(c) for c in ("#8a2a10", "#b5401a", "#e0602a", "#ff7a3c", "#ff9a5c", "#ffc890")]
    B = [hx(c) for c in ("#6e6858", "#968f7a", "#bdb69e", "#dcd6c0", "#f2eedc")]          # bone
    L.paint_base_skin(sk, T)
    hair = hx("#22222e")
    HT = L.ramp(hair, 5)

    # ------------------------------------------------------------ head
    L.scalp(sk, HT)
    sk.fn("head", "front", False, lambda x, y, w, h: T[3] if y >= 2 else None)
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, T[2]); sk.put("head", "front", False, 7, y, T[2])
    brow = hx("#14101a")
    for outer, inner in ((1, 2), (6, 5)):
        sk.put("head", "front", False, outer, 3, brow); sk.put("head", "front", False, inner, 3, brow); sk.put("head", "front", False, inner, 4, brow)
    for outer, inner in ((1, 2), (6, 5)):                       # small fierce eyes, a white of the eye and an orange glowing iris
        sk.put("head", "front", False, outer, 5, hx("#1c1218")); sk.put("head", "front", False, inner, 5, hx("#1c1218"))
        sk.put("head", "front", False, outer, 6, hx("#f2eee8")); sk.put("head", "front", False, inner, 6, O[3], True)
    sk.put("head", "front", False, 1, 6, O[3], True); sk.put("head", "front", False, 6, 6, hx("#f2eee8")); sk.put("head", "front", False, 5, 6, O[3], True)
    sk.put("head", "front", False, 2, 6, hx("#f2eee8"))
    for x in (3, 4):
        sk.put("head", "front", False, x, 5, T[2]); sk.put("head", "front", False, x, 6, T[1])      # a broad nose
    for x, c in ((1, T[1]), (2, hx("#4a1c1c")), (3, hx("#3a0e10")), (4, hx("#3a0e10")), (5, hx("#4a1c1c")), (6, T[1])):     # a heavy frown
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 0, 7, T[1]); sk.put("head", "front", False, 7, 7, T[1])
    sk.put("head", "front", False, 6, 4, T[1]); sk.put("head", "front", False, 6, 5, T[1])        # a crack scar on the cheek
    # the hollow's bone fragment on the right of the face (viewer's right), on the hat layer: front corner + the side
    bone_f = [
        "....BBBB",
        "....BbBB",
        ".....bbk",
        "......kk",
    ]
    lgb = {"B": B[4], "b": B[2], "k": B[0]}
    sk.art("head", "front", True, [r for r in bone_f], lgb, oy=1)
    sk.art("head", "east", True, ["bBBb", "BBbk", "bBkk", ".bk."], lgb, oy=1)
    sk.art("head", "east", True, ["k..."], lgb, oy=5)

    # ------------------------------------------------------------ body: bare chest, open white jacket, sash with the numeral 10
    lg = {"T": T[3], "t": T[2], "u": T[1], "v": T[4], "W": W[5], "w": W[4], "s": W[3], "d": W[2], "D": W[1], "k": K[2], "K": K[1], "j": K[3], "i": K[4],
          "o": O[3], "O": O[4], "p": O[2], "q": K[0], "z": W[0]}
    for ov in (False, True):
        sk.cloth("body", ov, T, faces=("front",))
    chest = [
        "TTvvvvTT",
        "TvvTTvvT",
        "TTTuuTTT",
        "tTuTTuTt",
        "TTTuuTTT",
        "TToToooT",     # the numeral 10 on the belly: a 1 (x2) and a 0 (x4-6)
        "TToTotoT",
        "TToToooT",
        "kjiiiijk",     # the black sash
        "KjiiiijK",
        "dwsWWwsd",
        "dzdddddz",
    ]
    jacket = [
        "WKKKKKKW",
        "Ww....wW",
        "Ws....sW",
        "Ws....sW",
        "ds....sd",
        "ds....sd",
        "ds....sd",
        "ds....sd",
        "kjiiiijk",
        "KjiiiijK",
        "dwsWWwsd",
        "dzdddddz",
    ]
    sk.art("body", "front", False, chest, lg, glow_chars="o")
    sk.art("body", "front", True, chest, lg, glow_chars="o")          # the overlay is filled too, so the chest art must be on both layers
    sk.art("body", "front", True, jacket, lg)
    # the two sides of the open jacket: bright inner edge so the lapel has body
    # collar: the tall white Espada collar with a black rim, round the neck
    col = ["KKKKKKKK", "WwWwwWwW", "dsdssdsd"]
    sk.art("body", "back", True, col, lg)
    for face_ in ("west", "east"):
        sk.art("body", face_, True, ["KKKK", "WwwW", "dsds"], lg)
    sk.art("body", "front", True, ["KK....KK", "Ww....wW", "ds....sd"], lg)
    sk.fn("body", "top", True, lambda x, y, w, h: W[5] if (x in (0, w - 1) or y in (0, h - 1)) else W[4])
    sk.cloth("body", True, W, faces=("back", "west", "east"), y0=3)
    sk.art("body", "back", True, col, lg)
    # the black sash round the waist, back + sides, and the long sash tails hanging at the hip (viewer's right)
    for face_, wd in (("back", 8), ("west", 4), ("east", 4)):
        for x in range(wd):
            for ov in (True, False):
                sk.put("body", face_, ov, x, 8, K[3] if x % 2 else K[4]); sk.put("body", face_, ov, x, 9, K[1] if x % 2 else K[2])
    sk.art("body", "back", True, ["dsdsdsds", "dwswswsd"], lg, oy=10)
    # the back: the jacket's white back with a long vent and the dark sash
    for y in range(3, 8):
        sk.put("body", "back", True, 3, y, W[2]); sk.put("body", "back", True, 4, y, W[2])         # centre seam

    # ------------------------------------------------------------ arms: white shoulders, bare forearms, black wrist guards
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            sk.cloth(part, ov, T, y0=0, y1=12, faces=("front", "back", "west", "east", "bottom"))
        for face_ in FR:
            for x in range(4):
                for y in range(0, 5):                                # the broad white sleeve of the jacket: puffed, with a black trim at the opening
                    c = W[5] if y == 0 else (W[4] if y in (1, 2) else (W[3] if y == 3 else K[3]))
                    if face_ != "front":
                        c = W[4] if y == 0 else (W[3] if y in (1, 2) else (W[2] if y == 3 else K[2]))
                    if x in (0, 3) and y < 4 and face_ in ("front", "back"):
                        c = W[3] if y else W[4]
                    sk.put(part, face_, True, x, y, c)
        sk.fn(part, "top", True, lambda x, y, w, h: W[5] if (x + y) % 2 == 0 else W[4])
        for face_ in FR:                                             # muscular forearm: highlight column
            for y in range(5, 8):
                sk.put(part, face_, True, 1, y, T[4] if face_ == "front" else T[3]); sk.put(part, face_, True, 2, y, T[3] if face_ == "front" else T[2])
                sk.put(part, face_, True, 0, y, T[2]); sk.put(part, face_, True, 3, y, T[1] if face_ != "front" else T[2])
            # wrist guard: black leather with a white tape band
            for x in range(4):
                sk.put(part, face_, True, x, 8, W[4] if x % 2 == 0 else W[3])
                sk.put(part, face_, True, x, 9, K[4] if x in (1, 2) else K[3])
                sk.put(part, face_, True, x, 10, K[3] if x in (1, 2) else K[2])
            sk.cv.put(sk.rect(part, face_, True)[0] + 0, sk.rect(part, face_, True)[1] + 11, (0, 0, 0, 0))
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, False, x, 11, T[3] if face_ == "front" else T[2])
                sk.cv.put(sk.rect(part, face_, True)[0] + x, sk.rect(part, face_, True)[1] + 11, (0, 0, 0, 0))
        sk.put(part, "front", False, 1, 11, T[4]); sk.put(part, "front", False, 2, 11, T[4])

    # ------------------------------------------------------------ legs: wide white hakama with pleats, black tabi + wooden sandals
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            sk.cloth(part, ov, W, y0=0, y1=10, folds=(1, 2), light=1)
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, True, x, 10, W[3] if x % 2 else W[4])                 # hem
                sk.put(part, face_, True, x, 9, W[2] if x in (1, 2) else W[3])
                sk.put(part, face_, True, x, 11, K[3] if face_ == "front" else K[2])     # black tabi
                sk.put(part, face_, False, x, 10, W[3]); sk.put(part, face_, False, x, 11, K[2])
        for x in range(4):
            sk.put(part, "bottom", True, x, 0, hx("#6a4428")); sk.put(part, "bottom", True, x, 3, hx("#6a4428"))
        for y in range(1, 9, 2):
            sk.put(part, "front", True, 1, y, W[2]); sk.put(part, "front", True, 2, y + 1, W[3])
        sk.put(part, "front", True, 0, 11, hx("#8a5a34")); sk.put(part, "front", True, 3, 11, hx("#8a5a34"))       # the sandal straps

    img, glow = sk.finish()
    return img, glow, "anime_spiky", hair, False
