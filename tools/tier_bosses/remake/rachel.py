"""Rachel (Tower of God): hand-painted skin, 3.37 remake round.

Design: long chestnut-brown hair (kit anime_long_open) tied with a white ribbon; a pale cyan fitted dress with a white high collar, a white tabard panel with bright cyan edges, a dark belt
with a white clasp, a flared short skirt with a white scalloped hem, a white scalloped capelet over the shoulders, cyan sleeves with big white ruffle cuffs, dark charcoal leggings and slate
boots with white folded cuffs. Face: round amber-brown eyes with lashes, flat cold brows, a thin straight mouth, no blush. Slim arms.
Colour blocking: cyan / white / charcoal / brown hair. Original pixel art, painted from memory of the character's look (Tower of God).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_b as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=True)
    pal = {}
    skin = L.ramp(L.hx("#f6dfcc"), (0.82, 0.94, 1.0, 1.06, 1.12))
    for ch, c in zip("abce", skin[:4]):
        pal[ch] = c
    hair = L.hx("#7a4528")
    for ch, c in zip("12345", L.ramp(hair, (0.55, 0.75, 1.0, 1.2, 1.45))):
        pal[ch] = c
    for ch, c in zip("drRqQ", L.ramp(L.hx("#3aa6c0"), (0.42, 0.68, 1.0, 1.22, 1.5))):
        pal[ch] = c
    pal.update({"y": (170, 196, 206), "w": (208, 230, 238), "W": (234, 247, 250), "h": (248, 253, 255), "H": (255, 255, 255)})
    pal.update({"j": (14, 22, 28), "k": (30, 42, 50), "K": (48, 64, 74), "J": (74, 94, 106)})
    pal.update({"L": (58, 30, 24), "D": (92, 46, 28), "M": (176, 104, 56), "I": (236, 180, 108), "*": (255, 250, 246), "B": (92, 52, 34), "m": (150, 96, 98)})

    for part in K.PARTS:
        for face in ("front", "back", "west", "east", "top", "bottom"):
            L.fill(cv, part, face, pal["c"] if face in ("front", "top") else pal["b"])

    # ------------------------------------------------------------------ head
    L.m(cv, "head", "front", [
        "22333322",
        "23344332",
        "bbbbbbbb",
        "cBBccBBc",
        "LLLccLLL",
        "cD*cc*Dc",
        "cMIccIMc",
        "cccmmccc",
    ], pal)
    L.setp(cv, "head", "front", 4, 6, pal["b"])
    for face in ("west", "east"):
        L.fill(cv, "head", face, None, fn=lambda x, y, w, h: pal["3"] if y <= 2 else (pal["2"] if (y <= 5 and x >= 3) else pal["b"]))
    L.fill(cv, "head", "top", None, fn=lambda x, y, w, h: pal["4"] if y < 4 else pal["3"])
    L.fill(cv, "head", "back", None, fn=lambda x, y, w, h: pal["3"] if y < 5 else pal["2"])
    L.fill(cv, "head", "bottom", pal["b"])
    # the white ribbon: a band round row 2 of the hat layer with a bow on the west side
    L.m(cv, "head", "west", ["........", "..wW....", "hHWWWWWW", "..WH....", "........", "........", "........", "........"], pal, overlay=True)
    L.m(cv, "head", "east", ["........", "........", "WWWWWWHh", "........", "........", "........", "........", "........"], pal, overlay=True)
    L.m(cv, "head", "back", ["........", "........", "wWwWwWwW", "........", "........", "........", "........", "........"], pal, overlay=True)
    L.m(cv, "head", "front", ["........", "........", "wW....Ww", "........", "........", "........", "........", "........"], pal, overlay=True)

    # ------------------------------------------------------------------ body base (the dress)
    L.m(cv, "body", "front", [
        "RRwWWwRR",
        "RqRWhRqR",
        "RQQhHQQR",
        "RRQhhQRR",
        "rRQWhQRr",
        "rRQWWQRr",
        "rRQWWQRr",
        "rrQwWQrr",
        "kKjHHjKk",
        "RRRWhRRR",
        "rRQWhQRr",
        "rrQwWQrr",
    ], pal)
    L.m(cv, "body", "back", [
        "rrwwwwrr",
        "rRRRRRRr",
        "rRRrrRRr",
        "rRRrrRRr",
        "rrRrrRrr",
        "rrRrrRrr",
        "rrrrrrrr",
        "rrrrrrrr",
        "kKjJJjKk",
        "rRRrrRRr",
        "rrRrrRrr",
        "rrrrrrrr",
    ], pal)
    L.side_pair(cv, "body", ["RRRr", "RRRr", "RRRr", "RRRr", "RRrr", "RRrr", "RRrr", "RRrr", "kKjk", "RRRr", "RRrr", "rrrr"], pal, overlay=False)
    L.fill(cv, "body", "top", pal["w"])
    L.fill(cv, "body", "bottom", pal["d"])
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: (pal["c"] if face == "front" else pal["b"]) if y >= 11 else (pal["R"] if face == "front" else pal["r"]))
        L.fill(cv, part, "top", pal["w"])
        L.fill(cv, part, "bottom", pal["b"])
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: (pal["K"] if face == "front" else pal["k"]) if y < 6 else (pal["k"] if face == "front" else pal["j"]))
        L.fill(cv, part, "top", pal["K"])
        L.fill(cv, part, "bottom", pal["j"])

    # ================================================================== OVERLAY
    L.m(cv, "body", "front", [
        "hHhHHhHh",
        "WhH..HhW",
        ".WhHHhW.",
        "..WhhW..",
        "..Q..Q..",
        "..QqqQ..",
        "..Q..Q..",
        "..QqqQ..",
        "kKjHHjKk",
        "RqRWhRqR",
        "rRQWhQRr",
        "wWhWWhWw",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "wWhHHhWw",
        "wWhhhhWw",
        "wWwWWwWw",
        "RRQqqQRR",
        "RRRqqRRR",
        "rRRRRRRr",
        "rRRrrRRr",
        "rRRrrRRr",
        "kKjJJjKk",
        "RRQqqQRR",
        "rRRqqRRr",
        "wWhWWhWw",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["wWhH", "wWhH", "wWwW", "RRQR", "RRRR", "RRRR", "RRrr", "RRrr", "kKjk", "RRQR", "RRRr", "wWhW"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["H"] if (x + y) % 2 else pal["h"])

    A_ = {
        "front": ["hWw", "WhW", "wWw", "rQr", "RqR", "RqR", "RRr", "rRr", "RRr", "wHw", "WhW", "..."],
        "back": ["wWw", "WhW", "wWw", "qQq", "rRr", "rRr", "rRr", "rRr", "rRr", "wWw", "wWw", "..."],
        "west": ["hWwh", "WhHW", "wWwW", "rQrQ", "RqRr", "RqRr", "RRrr", "rRrr", "RRrr", "wHHw", "WhhW", "...."],
        "east": ["wWWw", "wWhw", "wwWw", "qQqq", "rRrr", "rRrr", "rrrr", "rrrr", "rrrr", "wWWw", "wwww", "...."],
        "top": ["hHh", "WhH", "wWh", "wWw"],
    }
    L.arm_pair(cv, A_, pal)
    Lg = {
        "front": ["RqRq", "RRQq", "wHWh", "r...", "r...", "r...", "hHHh", "WwWw", "KJKk", "KJKk", "kKkk", "jjjj"],
        "back": ["RRrR", "rRRr", "wWwW", "....", "....", "....", "WWWW", "wWwW", "kKkk", "kKkk", "kkkk", "jjjj"],
        "west": ["RRqR", "RRQR", "wHWh", "....", "....", "....", "hHWW", "WwWw", "kKJk", "kKJk", "kkKk", "jjjj"],
        "east": ["RrRR", "rRRr", "wWwW", "....", "....", "....", "WWWh", "wWwW", "kKkk", "kKkk", "kkkk", "jjjj"],
    }
    L.leg_pair(cv, Lg, pal)
    L.check_hat_top_empty(cv)
    return cv.image(), None, "anime_long_open", L.hx("#7a4528"), True
