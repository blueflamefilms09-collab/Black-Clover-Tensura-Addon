"""Bertholdt Hoover (Attack on Titan, the Colossal Titan's holder): the tallest of the cadets, shaggy dark-brown hair that falls over a sad, tired face with green-grey eyes, the brown Survey Corps
jacket with a white shirt and cravat, the brown leather harness straps and double belt with silver buckles, white trousers with thigh straps, tall brown boots with silver buckles, and the green
Scout cloak thrown over the shoulders with the white-and-blue Wings of Freedom on the back (drawn as an original V of two feathered wings). A faint ember-orange steam glows at the nape and wrists
(the Colossal Titan's heat). Look from memory (no wiki could be fetched in the session)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_d as L  # noqa: E402

hx = L.hx
FR = L.FR


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    S = L.skin_ramp("#ecc4a0")
    hair = hx("#35231a")
    HT = L.ramp(hair, 5)
    J = L.ramp("#6c4630", 6)            # the brown jacket
    Lt = L.ramp("#4a2c1a", 6)           # dark leather: harness, belts
    T = L.lin("#9a9cac", "#fbf5e4", 6, 0.9)      # white-cream: shirt, trousers
    GR = L.ramp("#42663f", 6)           # the green Scout cloak
    SV = L.ramp("#b9c1ca", 6)           # silver buckles
    TN = L.ramp("#a67a4c", 6)           # tan boot leather
    BL = L.ramp("#4f7fc6", 6)           # the blue wing
    BT = L.ramp("#6a4226", 6)           # boot leather
    lg = L.lg_from([("abcdef", J), ("ghijkl", Lt), ("mnopqr", T), ("stuvwx", GR), ("ABCDEF", SV), ("GHIJKL", TN), ("MNOPQR", BL), ("01234", S)])
    L.paint_base_skin(sk, S)

    # ------------------------------------------------------------ head: long shaggy dark hair (3D), a sad tired face with big shaded green-grey eyes
    L.head_sides(sk, S, HT, hair_side_rows=4, hair_back_rows=7, ear=False, sideburn=2)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else HT[2])
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    brow = HT[1]
    for ox, ix in ((1, 2), (6, 5)):                                  # sad brows: the inner end is raised
        sk.put("head", "front", False, ox, 4 - 1, brow)
        sk.put("head", "front", False, ix, 2, brow)
    sk.put("head", "front", False, 1, 3, HT[1]); sk.put("head", "front", False, 6, 3, HT[1])
    sk.put("head", "front", False, 2, 3, HT[0]); sk.put("head", "front", False, 5, 3, HT[0])
    ir = (hx("#1f3a34"), hx("#4a7a68"), hx("#9cc6ae"))
    lash = hx("#2a1a14")
    for ox, ix in ((1, 2), (6, 5)):
        sk.put("head", "front", False, ox, 4, lash); sk.put("head", "front", False, ix, 4, lash)           # heavy upper lids
        sk.put("head", "front", False, ox, 5, ir[0]); sk.put("head", "front", False, ix, 5, ir[1])
        sk.put("head", "front", False, ox, 6, ir[1]); sk.put("head", "front", False, ix, 6, ir[2])
    sk.put("head", "front", False, 2, 5, hx("#f4fff8")); sk.put("head", "front", False, 5, 5, hx("#f4fff8"))   # catch-lights
    sk.put("head", "front", False, 0, 4, L.mix(S[2], lash, 0.2)); sk.put("head", "front", False, 7, 4, L.mix(S[2], lash, 0.2))
    for ox, ix in ((1, 2), (6, 5)):                                  # eye bags under the tired eyes
        sk.put("head", "front", False, ox, 7, L.mix(S[2], hx("#7a6a8a"), 0.25)); sk.put("head", "front", False, ix, 7, L.mix(S[2], hx("#7a6a8a"), 0.25))
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])
    sk.put("head", "front", False, 3, 5, S[4]); sk.put("head", "front", False, 4, 5, S[3])
    for x, c in ((3, hx("#b0675e")), (4, hx("#b0675e"))):                   # a small downturned mouth below the nose
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 2, 7, S[1]); sk.put("head", "front", False, 5, 7, S[1])

    # ------------------------------------------------------------ body: brown jacket, leather harness, belts, white trousers top; the green cloak with the wings on the back
    for ov in (False, True):
        L.panel(sk, "body", "front", ov, J, [1, 3, 3, 3, 3, 3, 2, 1], 0, 9)
        L.panel(sk, "body", "back", ov, GR, [1, 3, 3, 3, 3, 3, 3, 1], 0, 12)
        for f in ("west", "east"):
            L.panel(sk, "body", f, ov, J, [2, 3, 3, 1] if f == "west" else [1, 3, 3, 2], 0, 9)
        L.panel(sk, "body", "front", ov, T, [2, 4, 4, 4, 4, 3, 3, 2], 9, 12)
        for f in ("west", "east"):
            L.panel(sk, "body", f, ov, T, [3, 4, 4, 2], 9, 12)
        sk.fn("body", "top", ov, lambda x, y, w, h: GR[3])
    front = [
        "bcmqqmcb",     # 0 stand collar and the white cravat
        "cdbpqbdc",     # 1
        "ddhbbhcc",     # 2 leather harness straps either side of the jacket seam
        "ddhdbhdc",     # 3
        "dehAbhcc",     # 4 silver stud
        "ddhdbhdc",     # 5
        "ddhdbhcc",     # 6
        "cdhdbhdc",     # 7
        "bhhhhhhb",     # 8 belt
        "hhAEEAhh",     # 9 belt with silver buckles
        "mnoqqonm",     # 10 white trousers top
        "nooqqoon",     # 11
    ]
    for ov in (True,):
        sk.art("body", "front", ov, front, lg)
    sk.art("body", "front", False, front, lg)
    sk.put("body", "front", True, 1, 10, Lt[2]); sk.put("body", "front", True, 6, 10, Lt[2])               # thigh straps start under the belt
    side = ["bchb", "cdcb", "ddcb", "dddb", "cddb", "dddb", "cdcb", "dcdb", "hhhh", "hAhh", "mnon", "noon"]
    for f in ("west", "east"):
        for ov in (True, False):
            sk.art("body", f, ov, side if f == "west" else L.flip(side), lg)
    wings = [
        ".rq..QR.",
        ".rpq.QP.",
        "..qpoPQ.",
        "..qpoOP.",
        "...poOP.",
        "....oO..",
        "....DE..",
    ]
    cloak_rows = [
        "stuvvuts", "suvwwvus", "stuvvuts", "stuvvuts", "stuvvvts", "stuvvvts", "stuvvvts", "stuvvvts", "sttuuuts", "sttuvuts", "sstuutss", "ssttttss",
    ]
    for ov in (True, False):
        sk.art("body", "back", ov, cloak_rows, lg)
        sk.art("body", "back", ov, wings, lg, oy=2, ox=0)

    # ------------------------------------------------------------ arms: the green cloak over the shoulders, brown sleeves, leather bracers
    cloak_sh = [
        "uvvt",
        "vwwu",
        "uvvt",
        "ttss",
        "sstt",     # the hem of the cloak
    ]
    brace = [
        "hjjh",
        "hAih",
        "hjjh",
    ]
    for part in ("arm_r", "arm_l"):
        prof = [1, 3, 4, 2] if part == "arm_r" else [2, 4, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, J, prof, 0, 11)
            L.panel(sk, part, "back", ov, J, [1, 3, 3, 2], 0, 11)
            L.panel(sk, part, "west", ov, J, [2, 3, 3, 1], 0, 11)
            L.panel(sk, part, "east", ov, J, [1, 3, 3, 2], 0, 11)
        for f in FR:
            sk.art(part, f, True, cloak_sh, lg)
            sk.art(part, f, True, brace, lg, oy=8)
        sk.fn(part, "top", True, lambda x, y, w, h: GR[4] if (x + y) % 2 == 0 else GR[3])
        L.hands(sk, part, S, rows=(11,))
        sk.put(part, "front", True, 1, 8, Lt[4]); sk.put(part, "front", True, 2, 10, Lt[1])

    # ------------------------------------------------------------ legs: white trousers with leather thigh straps, tall brown boots with silver buckles
    for part in ("leg_r", "leg_l"):
        prof = [1, 3, 4, 2] if part == "leg_r" else [2, 4, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, T, [x + 1 for x in prof], 0, 7)
            for f in ("back", "west", "east"):
                L.panel(sk, part, f, ov, T, [2, 3, 3, 2], 0, 7)
            L.panel(sk, part, "front", ov, BT, [1, 3, 3, 2], 7, 12)
            for f in ("back", "west", "east"):
                L.panel(sk, part, f, ov, BT, [1, 2, 2, 1], 7, 12)
        for f in FR:
            for x in range(4):
                sk.put(part, f, True, x, 1, Lt[3] if x % 2 == 0 else Lt[2])         # the strap round the upper thigh
                sk.put(part, f, True, x, 4, Lt[3] if x % 2 == 0 else Lt[2])         # and one above the knee
                sk.put(part, f, True, x, 7, TN[4] if x % 2 == 0 else TN[3])         # tan boot cuff
                sk.put(part, f, True, x, 8, TN[2])
                sk.put(part, f, True, x, 9, BT[4] if x in (1, 2) else BT[3])
                sk.put(part, f, True, x, 10, BT[3] if x in (1, 2) else BT[2])
                sk.put(part, f, True, x, 11, BT[1] if x in (1, 2) else BT[0])
        sk.put(part, "front", True, 1, 1, SV[4]); sk.put(part, "front", True, 1, 4, SV[4])
        sk.put(part, "front", True, 1, 9, SV[3]); sk.put(part, "front", True, 2, 9, SV[2])
        sk.put(part, "front", True, 2, 10, BT[5]); sk.put(part, "front", True, 1, 7, TN[5])

    img, glow = sk.finish()
    return img, glow, "anime_short_open", hair, False
