"""Jugram Haschwalth (Bleach, the Wandenreich's Grandmaster / Yhwach's right hand): hand-painted skin, 3.37 remake round.

Design: a white knee-length Wandenreich coat with a raised blue-trimmed collar worn open over a black vest (blue cross emblem), steel pauldrons and bulky steel gauntlets with rivets,
a steel belt with a blue gem, charcoal trousers, flared steel-and-white boots; long pale ash-blond hair (kit style anime_long), pale blue sharp eyes, thin brows, a cold calm mouth.
Colour blocking: white / black / steel / one blue accent. Original pixel art, painted from memory of the character's canon look.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_b as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    skin = L.ramp(L.hx("#f3d9cf"), (0.80, 0.93, 1.0, 1.06, 1.12))
    pal = {}
    # skin a b c e
    for ch, c in zip("abce", (skin[0], skin[1], skin[2], skin[3])):
        pal[ch] = c
    hair = L.hx("#fff8e0")
    for ch, c in zip("12345", L.ramp(hair, (0.62, 0.8, 1.0, 1.1, 1.2))):
        pal[ch] = c
    # white coat: d w W h H
    for ch, c in zip("dwWhH", L.ramp(L.hx("#dfe5ee"), (0.60, 0.80, 1.0, 1.10, 1.20))):
        pal[ch] = c
    # black vest: k V v u
    pal.update({"k": (10, 12, 22), "V": (22, 26, 40), "v": (38, 44, 66), "u": (60, 70, 98)})
    # steel: Z z s S X
    pal.update({"Z": (44, 54, 76), "z": (84, 99, 126), "s": (128, 146, 176), "S": (176, 194, 220), "X": (232, 241, 252)})
    # blue trim: n t T
    pal.update({"n": (24, 70, 130), "t": (52, 126, 206), "T": (128, 196, 244), "N": (206, 238, 255)})
    # trousers: p q r y
    pal.update({"p": (16, 20, 32), "q": (28, 34, 52), "r": (44, 53, 78), "y": (70, 82, 112)})
    # eyes / brow / mouth
    pal.update({"L": (38, 40, 62), "D": (46, 76, 140), "M": (108, 150, 214), "I": (182, 214, 246), "*": (252, 252, 255), "B": (150, 138, 118), "m": (176, 126, 128), "R": (236, 188, 188)})

    # ------------------------------------------------------------------ base layer: skin everywhere first
    for part in K.PARTS:
        for face in ("front", "back", "west", "east", "top", "bottom"):
            L.fill(cv, part, face, pal["c"] if face in ("front", "top") else pal["b"], overlay=False)

    # ------------------------------------------------------------------ head
    L.m(cv, "head", "front", [
        "22333322",
        "23344332",
        "bbbbbbbb",
        "cBBccBBc",
        "cLLccLLc",
        "cD*cc*Dc",
        "cMIccIMc",
        "cccmmccc",
    ], pal)
    L.setp(cv, "head", "front", 4, 6, pal["b"])                       # nose shade
    L.setp(cv, "head", "front", 0, 6, pal["b"])
    L.setp(cv, "head", "front", 7, 6, pal["b"])
    L.setp(cv, "head", "front", 3, 5, pal["e"])
    for face in ("west", "east"):
        L.fill(cv, "head", face, None, fn=lambda x, y, w, h: pal["3"] if y <= 2 else (pal["2"] if (y <= 5 and x >= 3) else pal["b"]))
    L.fill(cv, "head", "top", None, fn=lambda x, y, w, h: pal["4"] if y < 4 else pal["3"])
    L.fill(cv, "head", "back", None, fn=lambda x, y, w, h: pal["3"] if y < 5 else pal["2"])
    L.fill(cv, "head", "bottom", pal["b"])

    # ------------------------------------------------------------------ body base: black vest, blue cross, steel belt, trousers
    L.m(cv, "body", "front", [
        "VVVkkVVV",
        "VvVVVVvV",
        "VVVttVVV",
        "VVtTTtVV",
        "VVVtnVVV",
        "VVVtnVVV",
        "VVVVVVVV",
        "VvVVVVvV",
        "ZsSzzSsZ",
        "qrrqqrrq",
        "pqrqqrqp",
        "pqqppqqp",
    ], pal)
    L.m(cv, "body", "back", [
        "kVVVVVVk",
        "VVvVVvVV",
        "VVVVVVVV",
        "VVvVVvVV",
        "VVVVVVVV",
        "VVVVVVVV",
        "VVVVVVVV",
        "VVVVVVVV",
        "ZzsZZsZZ",
        "pqqqqqqp",
        "pqqqqqqp",
        "ppqppqpp",
    ], pal)
    L.side_pair(cv, "body", ["VVVV", "VVVv", "VVVV", "VVVV", "VVVV", "VVVV", "VVVV", "VVvV", "ZzsZ", "pqqp", "pqqp", "ppqp"], pal, overlay=False)
    L.fill(cv, "body", "top", None, fn=lambda x, y, w, h: pal["V"])
    L.fill(cv, "body", "bottom", pal["p"])

    # ------------------------------------------------------------------ arms base: coat sleeve, steel under-gauntlet, bare hand
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            def af(x, y, w, h, face=face):
                if y >= 11:
                    return pal["c"] if face == "front" else pal["b"]
                if y >= 8:
                    return pal["z"]
                if y >= 6:
                    return pal["W"] if face == "front" else pal["w"]
                return pal["W"] if face == "front" else pal["w"]
            L.fill(cv, part, face, None, fn=af)
        L.fill(cv, part, "top", pal["s"])
        L.fill(cv, part, "bottom", pal["b"])

    # ------------------------------------------------------------------ legs base: charcoal trousers, steel boots
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y >= 8:
                    return pal["z"] if face == "front" else pal["Z"]
                return pal["r"] if face == "front" else pal["q"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["q"])
        L.fill(cv, part, "bottom", pal["Z"])

    # ================================================================== OVERLAY: the coat, collar, pauldrons, gauntlets, boots
    # the coat is open over the black vest: two white panels (x0-1, x6-7) with a thin blue piping on the open edge, tails below the belt
    L.m(cv, "body", "front", [
        "NTh..hTN",
        "WTh..hTW",
        "WT....TW",
        "hT....Th",
        "WT....TW",
        "hT....Th",
        "WT....TW",
        "wT....Tw",
        "zsSNNSsz",
        "wWh..hWw",
        "wWW..WWw",
        "wTT..TTw",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "TNNNNNNT",
        "WWWhhWWW",
        "wWWdhWWw",
        "wWWdhWWw",
        "wwWdhWww",
        "wwWdhWww",
        "wwWdhWww",
        "wwWdhWww",
        "zsSzzSsz",
        "wWWdhWWw",
        "wwWdhWww",
        "wwTTTTww",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["WWWT", "WhWT", "WWWT", "WhWT", "wWWT", "wWWT", "wwWT", "wwWT", "zsSz", "wWWT", "wwWT", "wTTT"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["T"] if y in (0, h - 1) or x in (0, w - 1) else (None if x in (3, 4) and y < 3 else pal["N"] if (x + y) % 2 else pal["t"]))

    # arm sleeves (the right arm, x = 0 is the outer edge): steel pauldron with a dark rim, white sleeve, one blue cuff row, bulky steel gauntlet with rivets, bare hand
    L_arm = {
        "front": ["SSSs", "sssz", "szzZ", "ZZZZ", "WWWw", "WhWw", "WWWw", "TTtt", "SssZ", "sSzZ", "zzZZ", "...."],
        "back": ["sSSs", "sssz", "szzZ", "ZZZZ", "wWWw", "wWhw", "wWWw", "ttnn", "sszZ", "sszZ", "zzZZ", "...."],
        "west": ["SSSs", "sXsz", "szzZ", "ZZZZ", "WWHh", "WhHh", "WWhh", "TTtt", "SssZ", "sXzZ", "zzZZ", "...."],
        "east": ["sssz", "sszz", "szzZ", "ZZZZ", "wwWh", "wwWh", "wwhh", "ttnn", "szzZ", "szzZ", "zZZZ", "...."],
        "top": ["SSSs", "sXsz", "szzZ", "ZZZZ"],
    }
    L.arm_pair(cv, L_arm, pal)

    # coat tails and boots on the legs (right leg: x = 3 is the inner edge, the open coat shows the charcoal trousers there)
    L_leg = {
        "front": [
            "WWh.",
            "WWh.",
            "wWh.",
            "wWh.",
            "wWw.",
            "wwW.",
            "TTT.",
            "SSSS",
            "ssss",
            "sSsz",
            "sSzz",
            "ZZZZ",
        ],
        "back": [
            "WWWW",
            "wWWW",
            "wWWW",
            "wwWW",
            "wwwW",
            "wwww",
            "TTTT",
            "SSSS",
            "ssss",
            "ssss",
            "szzz",
            "ZZZZ",
        ],
        "west": [
            "WWWT",
            "wWWT",
            "wWWT",
            "wwWT",
            "wwwT",
            "wwwT",
            "TTTT",
            "SSSS",
            "ssss",
            "SSsz",
            "SSzz",
            "ZZZZ",
        ],
        "east": [
            "TWWW",
            "TwWW",
            "TwwW",
            "TwwW",
            "TwwW",
            "Twww",
            "TTTT",
            "SSSS",
            "ssss",
            "ssss",
            "szzz",
            "ZZZZ",
        ],
    }
    L.leg_pair(cv, L_leg, pal)

    L.check_hat_top_empty(cv)
    glow = None
    return cv.image(), glow, "anime_long_open", L.hx("#fff8e0"), False
