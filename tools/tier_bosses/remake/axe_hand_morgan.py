"""Axe-Hand Morgan (One Piece, the Marine captain of Shells Town): a huge man with a flat-topped crop, a square slab of a jaw, heavy angry brows and stubble, a steel-blue Marine captain's coat with
a navy standing collar, gold buttons and gold piping, enormous steel epaulettes with gold fringe, big navy cuffs, a black belt with a gold buckle, navy trousers, black boots with steel cuffs, and
his right hand replaced by a great steel axe (a bladed gauntlet with a bright cutting edge). Look from memory (no wiki could be fetched in the session)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_d as L  # noqa: E402

hx = L.hx
FR = L.FR


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    S = L.skin_ramp("#f0c8a4")
    hair = hx("#4a3a2c")
    HT = L.ramp(hair, 5)
    C = L.ramp("#2c4272", 6)           # the Marine captain coat, deep navy
    N = L.lin("#7a86a8", "#fbf8ee", 6, 0.9)   # cream-white facings: collar, lapels, cuffs
    P = L.lin("#56647e", "#d8dfea", 6, 0.9)   # light grey-blue trousers
    G = L.ramp("#dcae3c", 6)           # gold: buttons, piping, fringe
    W = L.ramp("#eef0f2", 6, 0.5)      # white shirt, cravat
    M = L.lin("#3e4a62", "#e4edf8", 6, 0.8)   # steel: epaulettes, axe, boot cuffs
    K = L.ramp("#1c1a22", 6)           # black leather: belt, boots
    lg = L.lg_from([("abcdef", C), ("ghijkl", N), ("mnopqr", G), ("stuvwx", W), ("ABCDEF", M), ("GHIJKL", K),
                    ("01234", S)])
    L.paint_base_skin(sk, S)

    # ------------------------------------------------------------ head: a flat crop (3D), a square face, heavy brows, stubble
    L.head_sides(sk, S, HT, hair_side_rows=3, hair_back_rows=6, ear=True, sideburn=1)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else HT[2])
    brow = L.mix(HT[0], hx("#000000"), 0.25)
    iris = (hx("#1c2438"), hx("#4a6288"), hx("#9fb6d6"))          # hard steel-blue eyes
    lash = hx("#241a18")
    for ox, ix in ((1, 2), (6, 5)):
        sk.put("head", "front", False, ox, 3, brow)                    # heavy brows, the inner end one step lower than the outer is not possible at this size: the outer end is darker
        sk.put("head", "front", False, ix, 3, L.mix(brow, S[2], 0.35))
        sk.put("head", "front", False, ox, 4, lash); sk.put("head", "front", False, ix, 4, lash)        # the upper lids (dark, flat: a glare)
        sk.put("head", "front", False, ox, 5, iris[0]); sk.put("head", "front", False, ix, 5, iris[1])
    sk.put("head", "front", False, 2, 5, iris[2]); sk.put("head", "front", False, 5, 5, iris[1])
    sk.put("head", "front", False, 1, 5, iris[1]); sk.put("head", "front", False, 6, 5, iris[0])
    sk.put("head", "front", False, 2, 5, hx("#e8eef8"))                  # one catch-light per eye
    sk.put("head", "front", False, 5, 5, hx("#e8eef8")); sk.put("head", "front", False, 6, 5, iris[2])
    sk.put("head", "front", False, 1, 5, iris[0])
    # the slab face: the cheek planes step darker toward the jaw, a hard nose shadow, wide hard mouth, stubble
    for y in (3, 4, 5):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    for y in (6, 7):
        sk.put("head", "front", False, 0, y, S[1]); sk.put("head", "front", False, 7, y, S[1])
    sk.put("head", "front", False, 3, 5, S[4]); sk.put("head", "front", False, 4, 5, S[3])      # the nose bridge catches the light
    sk.put("head", "front", False, 3, 6, S[1]); sk.put("head", "front", False, 4, 6, S[1])      # the nose shadow
    mouth = hx("#7a3a38")
    for x, c in ((2, S[2]), (3, mouth), (4, mouth), (5, S[2])):
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 2, 7, L.mix(mouth, S[2], 0.5)); sk.put("head", "front", False, 5, 7, L.mix(mouth, S[2], 0.5))
    stub = L.mix(S[1], hair, 0.5)
    L.stubble(sk, S, [(1, 6), (2, 6), (5, 6), (6, 6), (1, 7), (6, 7), (0, 7), (7, 7), (0, 6), (7, 6), (3, 7), (4, 7)], col=stub)
    for x in (3, 4):
        sk.put("head", "front", False, x, 7, mouth)
    # the cap band: a navy band under the hair with a gold badge (hat layer; the top face stays empty)
    for f in ("front", "west", "east", "back"):
        for x in range(8):
            sk.put("head", f, True, x, 1, C[3] if x % 2 == 0 else C[2])
    for x in (3, 4):
        sk.put("head", "front", True, x, 1, G[4] if x == 3 else G[3])

    # ------------------------------------------------------------ body: a steel-blue coat, navy collar and lapels, gold buttons and piping, belt, coat skirt
    XB = [1, 3, 3, 3, 3, 2, 2, 1]              # light from the upper left
    for ov in (False, True):
        L.panel(sk, "body", "front", ov, C, XB, 0, 12)
        L.panel(sk, "body", "back", ov, C, [1, 2, 3, 3, 3, 3, 2, 1], 0, 12)
        for f in ("west", "east"):
            L.panel(sk, "body", f, ov, C, [2, 3, 3, 1] if f == "west" else [1, 3, 3, 2], 0, 12)
        sk.fn("body", "top", ov, lambda x, y, w, h: N[4])
    front = [
        "hjkwwkjh",     # 0 navy collar, white cravat
        "iknxwnki",     # 1 gold collar edge
        "bhgwwghb",     # 2 navy lapels frame the shirt
        "cdhgwghd",     # 3
        "cdchmhdc",     # 4 button
        "ceddmddb",     # 5
        "cdcdhdcb",     # 6
        "cdddmddb",     # 7 button
        "KHGGGGHK",     # 8 belt
        "KIGqqGIK",     # 9 belt with the gold buckle
        "bdcchccb",     # 10 coat skirt
        "bppppppb"[:8],  # 11 gold hem
    ]
    for ov in (True, False):
        sk.art("body", "front", ov, front, lg)
    # the double row of gold buttons and a gold piping line down the front edge of the coat
    for ov in (True,):
        for y in (4, 5, 6, 7):
            sk.put("body", "front", ov, 2, y, G[4] if y in (4, 7) else G[2])
            sk.put("body", "front", ov, 5, y, G[3] if y in (4, 7) else G[1])
    back = [
        "hjkjjkjh", "ikmjjmki", "bccdddcb", "bdcdddcb", "bdcdddcb", "bcddddcb", "bdcdddcb", "bdddddcb",
        "KHGGGGHK", "KIGHHGIK", "bdccccbb", "bppppppb",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg)
    side = ["hkjh", "ikmi", "cdcb", "ddcb", "cddb", "dddb", "cddb", "dddb", "KHGK", "KIGK", "ddcb", "bpcb"]
    for f in ("west", "east"):
        for ov in (True, False):
            sk.art("body", f, ov, side if f == "west" else L.flip(side), lg)

    # ------------------------------------------------------------ arms: giant steel epaulettes, navy cuffs; the right arm ends in the axe
    pauld = [
        "DEDC",
        "EFDC",
        "DDCB",
        "CCBA",
        "mnpn",
    ]
    cuff = [
        "mnqp",     # gold piping on the cuff
        "jlkj",
        "jkji",
        "ihhh",
    ]
    for part in ("arm_r", "arm_l"):
        prof = [1, 3, 4, 2] if part == "arm_r" else [2, 4, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, C, prof, 0, 11)
            L.panel(sk, part, "back", ov, C, [1, 3, 3, 2], 0, 11)
            L.panel(sk, part, "west", ov, C, [2, 3, 3, 1], 0, 11)
            L.panel(sk, part, "east", ov, C, [1, 3, 3, 2], 0, 11)
        for f in FR:
            sk.art(part, f, True, pauld, lg)
            sk.art(part, f, True, cuff, lg, oy=8)
        sk.fn(part, "top", True, lambda x, y, w, h: M[5] if (x + y) % 2 == 0 else M[4])
        if part == "arm_l":
            L.hands(sk, part, S, rows=(11,))
        else:
            for f in FR:
                for x in range(4):
                    sk.cv.put(sk.rect(part, f, True)[0] + x, sk.rect(part, f, True)[1] + 11, (0, 0, 0, 0))
    # the axe: a bladed steel gauntlet from the wrist to the fingertips; the outer column is the bright cutting edge; rivets and a dark socket ring
    axe = {
        "front": ["KBCB", "FDCB", "FDCA", "FEDB"],
        "back": ["KCCB", "ACCB", "ABCB", "ABCB"],
        "west": ["KDCB", "BECB", "BEDB", "BEDA"],
        "east": ["KCCB", "ACCB", "ABBB", "AABB"],
    }
    for f, rows in axe.items():
        sk.art("arm_r", f, False, rows, lg, oy=8)
        sk.art("arm_r", f, True, rows, lg, oy=8)
    for y in range(9, 12):
        sk.put("arm_r", "front", False, 0, y, M[5]); sk.put("arm_r", "front", True, 0, y, M[5])
        sk.put("arm_r", "west", False, 3, y, M[5]); sk.put("arm_r", "west", True, 3, y, M[5])
    sk.put("arm_r", "front", True, 2, 9, M[5]); sk.put("arm_r", "front", True, 2, 11, M[1])

    # ------------------------------------------------------------ legs: navy trousers, black boots with steel cuffs
    for part in ("leg_r", "leg_l"):
        prof = [1, 3, 3, 2] if part == "leg_r" else [2, 3, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, P, [x + 0 for x in prof], 0, 8, yadd=lambda y: 1 if y < 2 else 0)
            for f in ("back", "west", "east"):
                L.panel(sk, part, f, ov, P, [1, 2, 2, 1], 0, 8)
        for f in FR:
            for x in range(4):
                sk.put(part, f, True, x, 7, M[4] if x % 2 == 0 else M[3])
                sk.put(part, f, True, x, 8, M[3] if x % 2 == 0 else M[2])
                sk.put(part, f, True, x, 9, K[3] if x in (1, 2) else K[2])
                sk.put(part, f, True, x, 10, K[3] if x in (1, 2) else K[2])
                sk.put(part, f, True, x, 11, K[2] if x in (1, 2) else K[1])
        sk.put(part, "front", True, 1, 9, K[5]); sk.put(part, "front", True, 1, 10, K[4]); sk.put(part, "front", True, 2, 11, K[4])
        sk.put(part, "front", True, 1, 7, M[5])
        for f in FR:
            for x in range(4):
                for y in (8, 9, 10, 11):
                    sk.cv.put(sk.rect(part, f, False)[0] + x, sk.rect(part, f, False)[1] + y, tuple(K[2]) + (255,))

    img, glow = sk.finish()
    return img, glow, "anime_short", hair, False
