"""Mr. 2 Bon Clay (One Piece, Bentham, the Okama Kenpo ballet master): hand-painted skin, 3.37 remake round.

Design: a huge black afro (kit anime_afro); a heavily made-up face: arched brows, thick blue-violet eyeshadow, big purple eyes with lashes, pink cheeks and a wide grin in red lipstick with
white teeth; a hot-pink leotard bodice with a white heart and a deep V neck, a white feather boa collar and fluffy white feather epaulettes with pink tips, a magenta sash with a white
tutu (scalloped white and pink) flaring over the hips, bare pale upper arms with a pink armband, long white satin gloves with pink feather cuffs, white tights and white ballet boots with
pink feather tops and pink soles. Wide arms. Colour blocking: hot pink / white / black / blue-violet makeup.
Original pixel art, painted from memory of the character's canon look (a ballet / swan motif) in the mod's pink-and-white palette.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_b as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    pal = {}
    skin = L.ramp(L.hx("#fbe4da"), (0.82, 0.93, 1.0, 1.05, 1.1))
    for ch, c in zip("abce", skin[:4]):
        pal[ch] = c
    hair = L.hx("#2a2a3e")
    for ch, c in zip("12345", L.ramp(hair, (0.55, 0.78, 1.0, 1.3, 1.7))):
        pal[ch] = c
    for ch, c in zip("drRqQ", L.ramp(L.hx("#ff4f9a"), (0.45, 0.72, 1.0, 1.14, 1.3))):
        pal[ch] = c
    pal.update({"y": (204, 192, 208), "w": (232, 224, 238), "W": (250, 246, 252), "h": (255, 252, 255), "H": (255, 255, 255)})
    pal.update({"G": (150, 20, 80), "g": (214, 40, 112), "o": (255, 90, 160), "O": (255, 150, 200)})
    pal.update({"S": (88, 104, 232), "s": (58, 58, 170), "T": (150, 170, 255)})
    pal.update({"L": (24, 16, 44), "D": (74, 34, 140), "M": (136, 84, 206), "I": (206, 176, 255), "*": (255, 252, 255), "B": (34, 30, 52), "P": (214, 28, 60), "Y": (255, 160, 176), "t": (255, 255, 255)})

    for part in K.PARTS:
        for face in ("front", "back", "west", "east", "top", "bottom"):
            L.fill(cv, part, face, pal["c"] if face in ("front", "top") else pal["b"])

    # ------------------------------------------------------------------ head: the made-up face
    L.m(cv, "head", "front", [
        "22333322",
        "23344332",
        "eeeeeeee",
        "sSTccTSs",
        "cLLccLLc",
        "cD*cc*Dc",
        "YMIccIMY",
        "cPPttPPc",
    ], pal)
    # arched brows over the eyeshadow: two black pixels on row 2 (the fringe leaves row 2 free)
    for x in (1, 2, 5, 6):
        L.setp(cv, "head", "front", x, 2, pal["B"])
    L.setp(cv, "head", "front", 0, 3, pal["s"])
    L.setp(cv, "head", "front", 7, 3, pal["s"])
    L.setp(cv, "head", "front", 3, 6, pal["b"])
    for face in ("west", "east"):
        L.fill(cv, "head", face, None, fn=lambda x, y, w, h: pal["3"] if y <= 2 else (pal["2"] if (y <= 4 and x >= 3) else pal["b"]))
    L.fill(cv, "head", "top", None, fn=lambda x, y, w, h: pal["4"] if y < 4 else pal["3"])
    L.fill(cv, "head", "back", None, fn=lambda x, y, w, h: pal["3"] if y < 5 else pal["2"])
    L.fill(cv, "head", "bottom", pal["b"])

    # ------------------------------------------------------------------ body base: pink leotard bodice, white heart, deep V neck, magenta sash
    L.m(cv, "body", "front", [
        "RRceecRR",
        "RqReeRqR",
        "RQRRRRQR",
        "RRWRRWRR",
        "RRWWWWRR",
        "RRRWWRRR",
        "rRRRRRRr",
        "rGGggGGr",
        "gGoOOoGg",
        "WhHWWHhW",
        "wWqWWqWw",
        "qQHqqHQq",
    ], pal)
    L.m(cv, "body", "back", [
        "rrrrrrrr",
        "rRRRRRRr",
        "rRRrrRRr",
        "rRRrrRRr",
        "rrRrrRrr",
        "rrRrrRrr",
        "rrrrrrrr",
        "rGGggGGr",
        "gGoOOoGg",
        "wWhWWhWw",
        "wwWWWWww",
        "qQwqqwQq",
    ], pal)
    L.side_pair(cv, "body", ["RRRr", "RRRr", "RRRr", "RRRr", "RRrr", "RRrr", "RRrr", "GGgG", "gGoO", "WhWw", "wWww", "qQwq"], pal, overlay=False)
    L.fill(cv, "body", "top", pal["W"])
    L.fill(cv, "body", "bottom", pal["d"])

    # arms base: bare pale skin; white satin glove from the elbow down
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            def af(x, y, w, h, face=face):
                if y >= 7:
                    return pal["W"] if face == "front" else pal["w"]
                return pal["e"] if face == "front" else pal["c"]
            L.fill(cv, part, face, None, fn=af)
        L.fill(cv, part, "top", pal["e"])
        L.fill(cv, part, "bottom", pal["w"])
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: pal["W"] if face == "front" else pal["w"])
        L.fill(cv, part, "top", pal["W"])
        L.fill(cv, part, "bottom", pal["R"])

    # ================================================================== OVERLAY
    L.m(cv, "body", "front", [
        "wWhHHhWw",
        "WhH..HhW",
        "........",
        "........",
        "........",
        "........",
        "........",
        "........",
        "GgoOOoGg",
        "WhHWWHhW",
        "wWqWWqWw",
        "qQHqqHQq",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "wWhHHhWw",
        "WhHwwHhW",
        "wwWWWWww",
        "........",
        "........",
        "........",
        "........",
        "........",
        "GgoOOoGg",
        "WhHWWHhW",
        "wWqWWqWw",
        "qQHqqHQq",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["wWhH", "WhHw", "wwWW", "....", "....", "....", "....", "....", "GgoO", "WhHw", "wWqW", "qQHq"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["H"] if (x + y) % 2 else pal["W"])

    # wide arms (right arm, x = 0 outer): feather epaulette, pink armband, white glove with a pink feather cuff
    A_ = {
        "front": ["hHWw", "HWhW", "WhHq", "wQqQ", "RqRr", "....", "....", "qQqQ", "WhHw", "WHhw", "wWWw", "WhHW"],
        "back": ["wWhH", "WhHW", "wWWq", "qQqQ", "rRrr", "....", "....", "qQqQ", "wWhw", "wWhw", "wwWw", "wWww"],
        "west": ["hHWw", "HWhW", "WhHq", "wQqQ", "RqRr", "....", "....", "qQqQ", "WhHw", "WHhw", "wWWw", "WhHW"],
        "east": ["wWhH", "WhHW", "wWWq", "qQqQ", "rRrr", "....", "....", "qQqQ", "wWhw", "wWhw", "wwWw", "wWww"],
        "top": ["HhHW", "hWHh", "WHhW", "wWwW"],
    }
    L.arm_pair(cv, A_, pal)
    # tutu on the hips (leg rows 0-3) and white ballet boots with pink feather tops
    Lg = {
        "front": ["WhHW", "wWHw", "qQHq", "QqQq", "....", "....", "QqQq", "hHhH", "SWWS", "WSSW", "SWWS", "RqRr"],
        "back": ["wWhW", "wwWW", "qQwq", "QqQq", "....", "....", "QqQq", "WWwW", "wWhw", "wWhw", "wwww", "RrRr"],
        "west": ["WhHW", "wWHw", "qQHq", "QqQq", "....", "....", "QqQq", "hHhH", "SWWS", "WSSW", "SWWS", "RqRr"],
        "east": ["wWhW", "wwWW", "qQwq", "QqQq", "....", "....", "QqQq", "WWwW", "wWhw", "wWhw", "wwww", "RrRr"],
    }
    L.leg_pair(cv, Lg, pal)
    L.check_hat_top_empty(cv)
    return cv.image(), None, "anime_afro", L.hx("#2a2a3e"), False
