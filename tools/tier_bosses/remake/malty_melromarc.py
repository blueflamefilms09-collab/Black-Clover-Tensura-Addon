"""Malty S. Melromarc (The Rising of the Shield Hero, princess of Melromarc): hand-painted skin, 3.37 remake round.

Design: long crimson hair (kit anime_long_open) under a gold tiara with a ruby; a crimson royal dress with a cream tabard panel and gold edges, a gold belt with a ruby, a flared
short skirt with a gold hem, a short crimson mantle with a gold collar on the back, gold-rimmed puffed shoulders over cream sleeves with gold cuffs, wine thigh-high stockings with gold
garters and maroon boots with gold cuffs. Face: sharp red eyes with long lashes, thin brows, a smirk in red lipstick. Slim arms.
Colour blocking: crimson / cream / gold / maroon. Original pixel art, painted from memory of the character's canon look.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_b as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=True)
    pal = {}
    skin = L.ramp(L.hx("#f7dcc8"), (0.82, 0.94, 1.0, 1.06, 1.12))
    for ch, c in zip("abce", skin[:4]):
        pal[ch] = c
    hair = L.hx("#c8141f")
    for ch, c in zip("12345", L.ramp(hair, (0.55, 0.75, 1.0, 1.18, 1.38))):
        pal[ch] = c
    for ch, c in zip("drRqQ", L.ramp(L.hx("#c3222b"), (0.46, 0.72, 1.0, 1.2, 1.42))):
        pal[ch] = c
    pal.update({"y": (196, 168, 152), "w": (226, 200, 186), "W": (244, 226, 212), "h": (253, 241, 230), "H": (255, 252, 246)})
    for ch, c in zip("GgoOX", L.ramp(L.hx("#e0b030"), (0.5, 0.72, 1.0, 1.2, 1.5))):
        pal[ch] = c
    pal.update({"j": (34, 12, 18), "k": (62, 20, 28), "K": (92, 32, 40), "J": (126, 52, 56)})
    pal.update({"u": (46, 14, 28), "U": (76, 24, 44), "v": (104, 40, 62)})
    pal.update({"L": (62, 16, 24), "D": (150, 24, 32), "M": (214, 70, 64), "I": (255, 168, 130), "*": (255, 250, 250), "B": (122, 34, 36), "P": (186, 28, 56), "Y": (240, 190, 180)})

    for part in K.PARTS:
        for face in ("front", "back", "west", "east", "top", "bottom"):
            L.fill(cv, part, face, pal["c"] if face in ("front", "top") else pal["b"])

    # ------------------------------------------------------------------ head (scalp roots, the shaded face)
    L.m(cv, "head", "front", [
        "22333322",
        "23344332",
        "bbbbbbbb",
        "cBBccBBc",
        "LLLccLLL",
        "cD*cc*Dc",
        "cMIccIMc",
        "cccPPbcc",
    ], pal)
    L.setp(cv, "head", "front", 4, 6, pal["b"])
    L.setp(cv, "head", "front", 6, 7, pal["b"])
    for face in ("west", "east"):
        L.fill(cv, "head", face, None, fn=lambda x, y, w, h: pal["3"] if y <= 2 else (pal["2"] if (y <= 5 and x >= 3) else pal["b"]))
    L.fill(cv, "head", "top", None, fn=lambda x, y, w, h: pal["4"] if y < 4 else pal["3"])
    L.fill(cv, "head", "back", None, fn=lambda x, y, w, h: pal["3"] if y < 5 else pal["2"])
    L.fill(cv, "head", "bottom", pal["b"])
    # the tiara: a gold band on row 2 of the hat layer, a ruby in the middle, a point standing up on row 1
    L.m(cv, "head", "front", [
        "........",
        "...OO...",
        "gGoOOoGg",
        "........",
        "........",
        "........",
        "........",
        "........",
    ], pal, overlay=True)
    for face in ("west", "east"):
        L.m(cv, "head", face, ["........", "........", "ooooGGGG", "........", "........", "........", "........", "........"], pal, overlay=True)
    L.m(cv, "head", "back", ["........", "........", "GoGoGoGo", "........", "........", "........", "........", "........"], pal, overlay=True)
    L.setp(cv, "head", "front", 3, 2, pal["Q"], overlay=True)          # the ruby
    L.setp(cv, "head", "front", 4, 2, pal["R"], overlay=True)

    # ------------------------------------------------------------------ body base: the dress under the mantle, cream panel, gold belt
    L.m(cv, "body", "front", [
        "RRccccRR",
        "RqRhhRqR",
        "RQRhHRQR",
        "RRRhhRRR",
        "rRRWhRRr",
        "rRRWWRRr",
        "rRRWWRRr",
        "rrRwWRrr",
        "GgoXQogG",
        "RRRhhRRR",
        "rRRWhRRr",
        "rrRwWRrr",
    ], pal)
    L.m(cv, "body", "back", [
        "rrrrrrrr",
        "rRRRRRRr",
        "rRRrrRRr",
        "rRRrrRRr",
        "rrRrrRrr",
        "rrRrrRrr",
        "rrrrrrrr",
        "rrrrrrrr",
        "GgoooogG",
        "rRRrrRRr",
        "rrRrrRrr",
        "rrrrrrrr",
    ], pal)
    L.side_pair(cv, "body", ["RRRr", "RRRr", "RRRr", "RRRr", "RRrr", "RRrr", "RRrr", "RRrr", "GoGg", "RRRr", "RRrr", "rrrr"], pal, overlay=False)
    L.fill(cv, "body", "top", pal["g"])
    L.fill(cv, "body", "bottom", pal["d"])

    # arms base: cream sleeve, bare hand
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: (pal["c"] if face == "front" else pal["b"]) if y >= 11 else (pal["W"] if face == "front" else pal["w"]))
        L.fill(cv, part, "top", pal["R"])
        L.fill(cv, part, "bottom", pal["b"])

    # legs base: wine thigh-high stockings, maroon boots
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y >= 6:
                    return pal["K"] if face == "front" else pal["k"]
                return pal["U"] if face == "front" else pal["u"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["U"])
        L.fill(cv, part, "bottom", pal["j"])

    # ================================================================== OVERLAY
    # the mantle: a short crimson cape on the back with a gold collar and a gold scalloped hem, a gold collar on the shoulders at the front
    L.m(cv, "body", "front", [
        "gGoOOoGg",
        "RqG..GqR",
        "R.g..g.R",
        "..GQQG..",
        "..g..g..",
        "..G..G..",
        "..g..g..",
        "..G..G..",
        "gGoOQoGg",
        "RqRhhRqR",
        "rRRWhRRr",
        "rrRwWRrr",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "hHWwwWHh",
        "WhHWWHhW",
        "RqRRRRqR",
        "RRRrrRRR",
        "RRRrrRRR",
        "rRRrrRRr",
        "rRRrrRRr",
        "rRRrrRRr",
        "GgoooogG",
        "RRRrrRRR",
        "rRRrrRRr",
        "GoGoGoGo",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["GoOg", "qRRg", "RRRg", "RRrg", "RRrg", "RRrg", "RRrg", "RRrg", "GoGg", "RRrg", "RRrg", "GoGo"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["O"] if (x + y) % 2 else pal["o"])

    # slim arms (3 wide, right arm, x = 0 outer): crimson puff shoulder with a gold rim, cream sleeve, gold cuff, hand
    A_ = {
        "front": [
            "qQg",
            "RqG",
            "RRg",
            "GgG",
            "WhW",
            "WhW",
            "WWw",
            "wWw",
            "WWw",
            "GoG",
            "OGg",
            "...",
        ],
        "back": [
            "RRg",
            "RRG",
            "rRg",
            "GgG",
            "wWw",
            "wWw",
            "wWw",
            "wWw",
            "wWw",
            "GoG",
            "OGg",
            "...",
        ],
        "west": [
            "RqQg",
            "RqRG",
            "RRrg",
            "GgGg",
            "WhWw",
            "WhWw",
            "WWww",
            "wWww",
            "WWww",
            "GooG",
            "OGGg",
            "....",
        ],
        "east": [
            "gRRr",
            "GRrr",
            "grrr",
            "gGgG",
            "wWww",
            "wWww",
            "wwww",
            "wwww",
            "wwww",
            "GooG",
            "gGGg",
            "....",
        ],
        "top": ["qQg", "RRg", "RRG", "rrg"],
    }
    # the top face of a slim arm is 3 x 4
    L.arm_pair(cv, A_, pal)

    # skirt flare + garters + boots on the legs
    Lg = {
        "front": [
            "RQRq",
            "GgoG",
            "....",
            "....",
            "....",
            "gGog",
            "gGoO",
            "ooGg",
            "KJKk",
            "KJKk",
            "kKkk",
            "jjjj",
        ],
        "back": [
            "RRrR",
            "GgoG",
            "....",
            "....",
            "....",
            "gGog",
            "gGoo",
            "ooGg",
            "kKkk",
            "kKkk",
            "kkkk",
            "jjjj",
        ],
        "west": [
            "RRqR",
            "GgoG",
            "....",
            "....",
            "....",
            "gGog",
            "gGoO",
            "ooGg",
            "kKJk",
            "kKJk",
            "kkKk",
            "jjjj",
        ],
        "east": [
            "RrRR",
            "GgoG",
            "....",
            "....",
            "....",
            "gGog",
            "gGoo",
            "ooGg",
            "kKkk",
            "kKkk",
            "kkkk",
            "jjjj",
        ],
    }
    L.leg_pair(cv, Lg, pal)

    L.check_hat_top_empty(cv)
    return cv.image(), None, "anime_long_open", L.hx("#c8141f"), True
