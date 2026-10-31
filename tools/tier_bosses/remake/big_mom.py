"""\"Big Mom\" Charlotte Linlin (One Piece, Yonko of the Big Mom Pirates): hand-painted skin, 3.37 remake round (group c).

Design (colours): hot-pink puff-sleeved gown, a white ruffled lace collar and scalloped lace cuffs, gold trim, brooch, bangles and hem, a deep plum flared skirt with a gold sash and a big bow
at the back, wine boots with gold cuffs; magenta hair (kit style anime_mane, huge), a gold tiara on row 2 of the hat layer, big violet-shadowed eyes with lashes, angry brows, a huge grin in
red lipstick. Look: from memory of the canon design (the wikis were not reachable), stylised to the roster sheet.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_c as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    pal = {}
    L.skin_pal(pal, L.hx("#f7d2c4"))
    L.hair_pal(pal, L.hx("#e8458c"), (0.45, 0.72, 1.0, 1.22, 1.5))
    L.put_ramp(pal, "dwWhH", L.hx("#f0608f"), (0.50, 0.74, 1.0, 1.18, 1.38))             # gown pink
    L.put_ramp(pal, "ntTN", L.hx("#a8146a"), (0.55, 0.8, 1.0, 1.3))                        # plum skirt
    L.put_ramp(pal, "gGyY", L.hx("#ffd24a"), (0.58, 0.84, 1.0, 1.3))                       # gold
    L.put_ramp(pal, "lqQ", L.hx("#fff2f6"), (0.84, 0.95, 1.04))                            # lace
    L.put_ramp(pal, "oOP", L.hx("#7e1f4c"), (0.6, 1.0, 1.4))                               # wine boots
    pal.update({"L": (74, 28, 96), "D": (96, 40, 128), "M": (156, 84, 196), "I": (224, 176, 244), "*": (255, 255, 255), "B": (92, 24, 72), "U": (142, 92, 214),
                "m": (150, 14, 58), "R": (226, 44, 92), "r": (255, 130, 160), "k": (60, 14, 48)})

    L.skin_base(cv, pal)

    # ---------------------------------------------------------------- head
    L.m(cv, "head", "front", [
        "33444433",
        "23344332",
        "bBbbbbBb",
        "cUccBBcU",
        "cLLccLLc",
        "cD*cc*Dc",
        "cMIcbIMc",
        "mRRqqRRm",
    ], pal)
    # angry brows: the inner ends sit lower than the outer ends
    L.setp(cv, "head", "front", 2, 3, pal["B"])
    L.setp(cv, "head", "front", 5, 3, pal["B"])
    L.setp(cv, "head", "front", 3, 3, pal["c"])
    L.setp(cv, "head", "front", 4, 3, pal["c"])
    L.setp(cv, "head", "front", 0, 6, pal["R"])           # blush
    L.setp(cv, "head", "front", 7, 6, pal["R"])
    L.head_shell(cv, pal, side_hair_rows=5)

    # ---------------------------------------------------------------- body base: the pink bodice with gold lacing
    L.m(cv, "body", "front", [
        "dWeffeWd",
        "dWWeeWWd",
        "dWWWWWWd",
        "dwWyyWwd",
        "dwhYyhwd",
        "dwWWWWwd",
        "dwWgWgwd",
        "dwWWWWwd",
        "kyGyyGyk",
        "ntTTTTtn",
        "ntTTTTtn",
        "nttTTttn",
    ], pal)
    L.m(cv, "body", "back", [
        "dwWWWWwd",
        "dwwWWwwd",
        "dwWgWgwd",
        "dwWWWWwd",
        "dwWgWgwd",
        "dwWWWWwd",
        "dwwWWwwd",
        "dwWWWWwd",
        "kyGyyGyk",
        "ntTTTTtn",
        "ntTTTTtn",
        "nttTTttn",
    ], pal)
    L.side_pair(cv, "body", ["dwWW", "dwWW", "dwWh", "dwWh", "dwWh", "dwWh", "dwWh", "dwWh", "kyGy", "ntTT", "ntTt", "nttt"], pal, overlay=False)
    L.fill(cv, "body", "top", pal["W"])
    L.fill(cv, "body", "bottom", pal["n"])

    # ---------------------------------------------------------------- arms base: pink sleeve, bare forearm and hand
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            def af(x, y, w, h, face=face):
                if y >= 8:
                    return pal["c"] if face == "front" else pal["b"]
                return pal["W"] if face == "front" else pal["w"]
            L.fill(cv, part, face, None, fn=af)
        L.fill(cv, part, "top", pal["h"])
        L.fill(cv, part, "bottom", pal["b"])

    # ---------------------------------------------------------------- legs base: plum skirt, pink and white striped stockings, wine boots
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y <= 6:
                    return pal["T"] if face == "front" else pal["t"]
                if y <= 8:
                    return pal["q"] if (y + x) % 2 else pal["h"]
                return pal["O"] if face == "front" else pal["o"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["t"])
        L.fill(cv, part, "bottom", pal["o"])

    # ================================================================ OVERLAY
    # the ruffled lace collar, the gold brooch heart, the gold-trimmed plum sash with its buckle, the flared skirt front
    L.m(cv, "body", "front", [
        "qQqqqqQq",
        "lqQeeQql",
        "wyqqqqyw",
        "wRRqqRRw",
        "wRrRRrRw",
        "wqRRRRqw",
        "wqqRRqqw",
        "kttkktkk",
        "gyYyyYyg",
        "ntNTTNtn",
        "NTTyTTTN",
        "yGyGGyGy",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "qQqqqqQq",
        "lqQQQQql",
        "........",
        "........",
        "...nn...",
        ".nnTTnn.",
        "nTTYyTTn",
        ".nnTTnn.",
        "gyYyyYyg",
        "ntTTTTtn",
        "NTTTTTTN",
        "yGyGGyGy",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["qQql", "lqql", "....", "....", "....", "....", "....", "ttkt", "gyYy", "ntTT", "NTTt", "yGyG"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["q"] if (x + y) % 2 else pal["Q"])

    # puffed sleeves with a gold epaulette and a scalloped lace cuff; gold bangle on the forearm
    arm = {
        "front": ["WhHh", "hHHh", "WhhW", "yGyY", "WwWw", "wWWw", "wwww", "qQql", "....", "gyYg", "....", "...."],
        "back": ["whWw", "wWWw", "wwWw", "gGyg", "wwww", "wwww", "wwww", "lqql", "....", "gyyg", "....", "...."],
        "west": ["WhHh", "hHHh", "WhhW", "yGyY", "WwWw", "wWWw", "wwww", "qQql", "....", "gyYg", "....", "...."],
        "east": ["whWw", "wWWw", "wwWw", "gGyg", "wwww", "wwww", "wwww", "lqql", "....", "gyyg", "....", "...."],
        "top": ["hHHh", "HHHH", "hHHh", "whWw"],
    }
    L.arm_pair(cv, arm, pal)

    # the skirt flare on the legs: plum folds, gold hem, a lace petticoat; wine boots with a gold cuff, a lace ruffle and a toe cap
    leg = {
        "front": ["nNTt", "ntNt", "NTyt", "ntNT", "NTTt", "ntNt", "yGyG", "qQql", "qlQq", "gyYy", "OPOo", "oooo"],
        "back": ["ntTt", "nttt", "ntTt", "nttt", "ntTt", "nttt", "gGyg", "lqql", "lqlq", "gyyg", "oOoo", "oooo"],
        "west": ["ntTT", "nttT", "nTTT", "nttT", "nTTt", "nttT", "yGyG", "qQql", "qlQq", "gyYy", "OPOo", "oooo"],
        "east": ["TTtn", "TttN", "TTTn", "TttN", "tTTn", "TttN", "GyGy", "lqQq", "qQlq", "yYyg", "oOPO", "oooo"],
    }
    L.leg_pair(cv, leg, pal)

    L.check_hat_top_empty(cv)
    # the gold tiara: row 2 of the hat layer, a jewel in the middle of the front
    for face in ("front", "west", "east", "back"):
        L.fill(cv, "head", face, None, overlay=True, fn=lambda x, y, w, h: (pal["Y"] if x in (3, 4) and face == "front" else (pal["y"] if (x + y) % 2 else pal["G"])) if y == 2 else None)
    L.setp(cv, "head", "front", 3, 2, (255, 70, 120), overlay=True)
    L.setp(cv, "head", "front", 4, 2, (255, 140, 170), overlay=True)
    L.check_hat_top_empty(cv)
    fx, fy, _, _ = cv.front_rect("head", True)
    glow = L.glow_image([(fx + 3, fy + 2, (255, 70, 120)), (fx + 4, fy + 2, (255, 140, 170))])
    return cv.image(), glow, "anime_mane", L.hx("#e8458c"), False
