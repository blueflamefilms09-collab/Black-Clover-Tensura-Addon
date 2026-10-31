"""Shou Tucker (Fullmetal Alchemist, the Sewing Life Alchemist): a thin, stooped man with shaggy brown hair, round wire glasses over tired eyes with heavy bags, stubble and a mild, too-gentle smile; a
white laboratory coat (stained with a green-brown alchemical smear, a pocket full of pens) open over a brown waistcoat, a cream shirt and a loosened olive tie, the sleeves rolled to the elbow
(bare forearms), dark-brown trousers and worn brown shoes. Look from memory (no wiki could be fetched in the session)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_d as L  # noqa: E402

hx = L.hx
FR = L.FR


def paint(spec, cid, masks):
    sk = L.Sk(slim=True)
    S = L.skin_ramp("#eec7a6")
    hair = hx("#6a4a34")
    HT = L.ramp(hair, 5)
    T = L.lin("#b0a89a", "#fffaf0", 6, 0.8)          # the lab coat: warm white, soft grey-beige shadows
    V = L.ramp("#8a4e32", 6)                # the red-brown waistcoat
    SH = L.lin("#a9a8b0", "#f6f0dc", 6, 0.9)         # the cream shirt
    OL = L.ramp("#6a8a2c", 6)               # the green tie, and the green stain
    BR = L.ramp("#6a4c38", 6)               # trousers, shoes
    SV = L.ramp("#c4c8d0", 6)               # glasses, buttons
    lg = L.lg_from([("abcdef", V), ("ghijkl", OL), ("mnopqr", T), ("stuvwx", SH), ("ABCDEF", BR), ("GHIJKL", SV), ("01234", S)])
    L.paint_base_skin(sk, S)

    # ------------------------------------------------------------ head: shaggy brown hair (3D), tired eyes behind round glasses, bags, stubble, a mild smile
    L.head_sides(sk, S, HT, hair_side_rows=4, hair_back_rows=7, ear=True, sideburn=1)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else HT[2])
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    brow = HT[1]
    for ox, ix in ((1, 2), (6, 5)):                                  # worried brows: the inner end rises
        sk.put("head", "front", False, ox, 3, brow); sk.put("head", "front", False, ix, 3, HT[0])
    ir = (hx("#3a2a18"), hx("#7a5a30"), hx("#c49a58"))                # tired amber-brown eyes
    lash = hx("#3a2418")
    for ox, ix in ((1, 2), (6, 5)):
        sk.put("head", "front", False, ox, 4, lash); sk.put("head", "front", False, ix, 4, lash)     # half-shut upper lids
        sk.put("head", "front", False, ox, 5, ir[0]); sk.put("head", "front", False, ix, 5, ir[1])
        sk.put("head", "front", False, ox, 6, L.mix(S[2], hx("#8a6a90"), 0.4)); sk.put("head", "front", False, ix, 6, L.mix(S[2], hx("#8a6a90"), 0.4))   # eye bags
    sk.put("head", "front", False, 2, 5, hx("#fff4dc")); sk.put("head", "front", False, 5, 5, hx("#fff4dc"))
    sk.put("head", "front", False, 3, 5, S[4]); sk.put("head", "front", False, 4, 5, S[3])
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])
    stub = L.mix(S[1], hair, 0.45)
    for (x, y) in ((1, 7), (2, 7), (5, 7), (6, 7), (0, 6), (7, 6), (0, 7), (7, 7), (3, 7), (4, 7)):
        sk.put("head", "front", False, x, y, stub)
    for x, c in ((2, S[2]), (3, hx("#a85c58")), (4, hx("#c27468")), (5, hx("#8a504c"))):          # a small, kindly, tired smile (one corner up)
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 5, 6, S[2])
    # round wire glasses on the hat layer: a thin silver ring round each eye (translucent so the brows and bags show through), a bridge, temple arms, a lens glint
    ox0, oy0 = sk.rect("head", "front", True)[:2]

    def rim(x, y, a=170, c=SV[4]):
        sk.cv.put(ox0 + x, oy0 + y, (c[0], c[1], c[2], a))
    for x in (1, 2, 5, 6):
        rim(x, 3, 255, SV[3] if x in (2, 5) else SV[4])
    for y in (4,):
        for x in (0, 3, 4, 7):
            rim(x, y, 255, SV[2] if x in (0, 7) else SV[5])
    rim(0, 3, 255, SV[2]); rim(7, 3, 255, SV[2])
    for f in ("west", "east"):
        x0, y0 = sk.rect("head", f, True)[:2]
        for x in (range(4, 8) if f == "west" else range(0, 4)):
            sk.cv.put(x0 + x, y0 + 3, (SV[2][0], SV[2][1], SV[2][2], 255))

    # ------------------------------------------------------------ body: waistcoat and shirt under an open white lab coat
    for ov in (False,):
        L.panel(sk, "body", "front", ov, V, [1, 3, 3, 3, 3, 3, 2, 1], 0, 12)
        L.panel(sk, "body", "back", ov, T, [2, 4, 5, 5, 5, 5, 4, 2], 0, 12)
        for f in ("west", "east"):
            L.panel(sk, "body", f, ov, T, [2, 3, 3, 1] if f == "west" else [1, 3, 3, 2], 0, 12)
    front = [
        "opswwspo",     # 0 coat collar either side of the shirt collar
        "pqsijsqp",     # 1 tie knot
        "opdgidpo",     # 2 the tie hangs over the waistcoat
        "opcgjdpo",
        "ordKdcpn",     # 4 a silver waistcoat button
        "opcghdpo",
        "ordcgdpn",
        "opcKdcpo",     # 7 button
        "pqoAAopq",     # 8 the open coat falls over the hips, trousers show between
        "nopAAopn",
        "nooAAoon",
        "mnoAAonm",
    ]
    for ov in (True,):
        sk.art("body", "front", ov, front, lg)
        sk.art("body", "front", False, front, lg)
    # a breast pocket on the coat full of pens, a green-brown stain on the lower hem
    sk.put("body", "front", True, 1, 3, T[2]); sk.put("body", "front", True, 1, 4, T[1])
    sk.put("body", "front", True, 1, 2, hx("#2a5ac8")); sk.put("body", "front", True, 2, 2, hx("#d8b020")); sk.put("body", "front", True, 0, 2, hx("#c83030"))
    for (x, y, c) in ((6, 9, OL[2]), (6, 10, OL[3]), (7, 10, OL[2]), (5, 10, OL[1]), (6, 11, OL[2]), (7, 9, L.mix(OL[2], T[4], 0.5))):
        sk.put("body", "front", True, x, y, c)
    back = [
        "mnoqqonm", "nopqqpon", "nopqrpon", "nopppoon", "mnoppoon", "nooppoon", "nopooonm", "mnopoonm", "mnoonnmm", "mmnnnmmm", "mnnnnnnm", "mmnmmnmm",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg)
    side = ["pqon", "pqon", "popn", "oppn", "oppn", "ooon", "oopn", "oonm", "nonm", "mnnm", "nnmm", "mnmm"]
    for f in ("west", "east"):
        for ov in (True, False):
            sk.art("body", f, ov, side if f == "west" else L.flip(side), lg)
    sk.fn("body", "top", True, lambda x, y, w, h: T[4])

    # ------------------------------------------------------------ arms (slim): white coat sleeves rolled to the elbow, bare forearms
    sleeve = [
        "qrp",
        "qpo",
        "pon",
        "ono",
        "nom",
        "pqn",      # row 5-6: the rolled cuff, a thick light roll with a shadow under it
        "mlm",
    ]
    sleeve = [
        "qrp",
        "qpo",
        "pon",
        "ono",
        "nom",
        "qrq",      # the rolled cuff
        "omn",
    ]
    for part in ("arm_r", "arm_l"):
        prof = [2, 4, 4] if part == "arm_r" else [4, 4, 2]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, T, prof, 0, 7)
            L.panel(sk, part, "back", ov, T, [2, 4, 3], 0, 7)
            L.panel(sk, part, "west", ov, T, [3, 4, 4, 2], 0, 7)
            L.panel(sk, part, "east", ov, T, [2, 4, 4, 3], 0, 7)
        for f in ("front", "back"):
            sk.art(part, f, True, sleeve, lg)
        for f in ("west", "east"):
            sk.art(part, f, True, [r + "n" for r in sleeve], lg)
        sk.fn(part, "top", True, lambda x, y, w, h: T[5])
        L.bare_arm(sk, part, S, 7)
        sk.put(part, "front", False, 1, 9, L.mix(S[3], OL[2], 0.35)); sk.put(part, "front", False, 2 if part == "arm_r" else 0, 9, L.mix(S[3], OL[2], 0.25))   # an alchemy stain
        sk.put(part, "front", False, 1, 8, S[4])
    sk.put("arm_l", "front", True, 1, 7, SV[4])                      # a thin silver watch band
    for x in range(3):
        sk.put("arm_l", "front", True, x, 10, SV[3] if x == 1 else SV[2])
        sk.put("arm_l", "front", False, x, 10, SV[3] if x == 1 else SV[2])
    sk.put("arm_l", "front", False, 1, 10, SV[5])

    # ------------------------------------------------------------ legs: dark brown trousers, worn brown shoes
    for part in ("leg_r", "leg_l"):
        prof = [1, 3, 3, 2] if part == "leg_r" else [2, 3, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, BR, [x + 0 for x in prof], 0, 9, yadd=lambda y: 1 if y < 2 else 0)
            for f in ("back", "west", "east"):
                L.panel(sk, part, f, ov, BR, [1, 2, 2, 1], 0, 9)
        for f in FR:
            for x in range(4):
                sk.put(part, f, True, x, 8, BR[3] if x % 2 == 0 else BR[2])        # trouser cuff
                sk.put(part, f, True, x, 9, V[2] if x in (1, 2) else V[1])         # shoe
                sk.put(part, f, True, x, 10, V[3] if x in (1, 2) else V[1])
                sk.put(part, f, True, x, 11, BR[1] if x in (1, 2) else BR[0])      # sole
        sk.put(part, "front", True, 1, 10, V[5]); sk.put(part, "front", True, 2, 10, V[4]); sk.put(part, "front", True, 1, 8, BR[5])
        sk.put(part, "front", False, 1, 9, V[2]); sk.put(part, "front", False, 2, 9, V[2])

    img, glow = sk.finish()
    return img, glow, "anime_short", hair, True
