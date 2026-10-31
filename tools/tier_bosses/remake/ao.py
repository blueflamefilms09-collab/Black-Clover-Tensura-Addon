"""Ao (Boruto / Naruto, the Hidden Mist shinobi with the stolen Byakugan): hand-painted skin, 3.37 remake round (group c).

Design: a sky-blue knee-length coat with white trim and a white stand collar, worn open over a navy flak vest (steel zipper, chest pockets) with a navy obi sash, pale steel pauldrons, steel bracers
with navy straps, white-wrapped shins and dark boots with white cuffs; black-navy swept hair (kit style anime_swept), a navy shinobi headband with a steel plate on row 2 of the head, ONE
glowing pale-lavender Byakugan eye with temple veins and an eyepatch over the other, calm line mouth. Back of the coat: a white ring crest. The Byakugan eye is the glow map. Look: from memory
of the canon design (the wikis were not reachable); the colours follow the roster sheet (sky blue, white, navy).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_c as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    pal = {}
    L.skin_pal(pal, L.hx("#f0d3c3"), (0.76, 0.88, 1.0, 1.07, 1.13))
    L.hair_pal(pal, L.hx("#2a2f58"), (0.5, 0.74, 1.0, 1.28, 1.6))
    L.put_ramp(pal, "dwWhH", L.hx("#4fb4ea"), (0.55, 0.78, 1.0, 1.2, 1.4))              # sky-blue coat
    L.put_ramp(pal, "lqQ", L.hx("#f4f8ff"), (0.8, 0.94, 1.06))                            # white trim, collar, wraps
    L.put_ramp(pal, "ntTN", L.hx("#1b3a74"), (0.55, 0.8, 1.0, 1.3))                       # navy vest
    L.put_ramp(pal, "ZzsSX", L.hx("#8db0c6"), (0.5, 0.75, 1.0, 1.25, 1.55))                # pale steel
    L.put_ramp(pal, "oOP", L.hx("#12203c"), (0.7, 1.0, 1.5))                              # boots
    pal.update({"k": (14, 16, 26), "V": (28, 30, 44), "v": (150, 142, 224), "I": (212, 206, 250), "M": (164, 154, 230), "*": (255, 255, 255), "L": (46, 44, 70), "B": (44, 48, 78), "m": (160, 112, 112)})

    L.skin_base(cv, pal)
    # ---------------------------------------------------------------- head: hair rows, the headband with its steel plate, a veined Byakugan eye, an eyepatch
    L.m(cv, "head", "front", [
        "23444432",
        "12343421",
        "TTsSSsTT",
        "vBBcVVVV",
        "vLLcckkV",
        "cMIcckkc",
        "cMMcbccc",
        "cbcmmcbc",
    ], pal)
    L.setp(cv, "head", "front", 0, 5, pal["v"])
    L.setp(cv, "head", "front", 3, 6, pal["b"])
    L.head_shell(cv, pal, h_hi="4", h_mid="3", h_lo="2", h_dk="1", side_hair_rows=3)
    # the headband runs round the head (row 2) on the sides and the back
    for face in ("west", "east", "back"):
        L.fill(cv, "head", face, None, fn=lambda x, y, w, h: pal["T"] if y == 2 else None)
    L.setp(cv, "head", "front", 3, 2, pal["X"])

    # ---------------------------------------------------------------- body base: navy flak vest, steel zipper, pockets, belt, trousers
    L.m(cv, "body", "front", [
        "nTTefTTn",
        "nTTSzTTn",
        "nTtSzTtn",
        "nNNSzNNn",
        "nNtSztNn",
        "nNNSzNNn",
        "nTtSzTtn",
        "kVVSXVVk",
        "kVvSXvVk",
        "ntTTTTtn",
        "ntTTTTtn",
        "nttTTttn",
    ], pal)
    L.fill(cv, "body", "back", None, fn=lambda x, y, w, h: pal["T"] if y < 7 else (pal["V"] if y < 9 else pal["t"]))
    L.fill(cv, "body", "west", None, fn=lambda x, y, w, h: pal["t"] if y < 7 else (pal["V"] if y < 9 else pal["n"]))
    L.fill(cv, "body", "east", None, fn=lambda x, y, w, h: pal["t"] if y < 7 else (pal["V"] if y < 9 else pal["n"]))
    L.fill(cv, "body", "top", pal["T"])
    L.fill(cv, "body", "bottom", pal["V"])
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: (pal["c"] if face == "front" else pal["b"]) if y >= 11 else ((pal["s"] if face == "front" else pal["z"]) if y >= 8 else (pal["W"] if face == "front" else pal["w"])))
        L.fill(cv, part, "top", pal["S"])
        L.fill(cv, part, "bottom", pal["b"])
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y >= 10:
                    return pal["O"] if face == "front" else pal["o"]
                if y >= 8:
                    return pal["q"] if face == "front" else pal["l"]
                return pal["T"] if face == "front" else pal["t"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["t"])
        L.fill(cv, part, "bottom", pal["V"])

    # ================================================================ OVERLAY
    L.m(cv, "body", "front", [
        "QqQ..QqQ",
        "WHq..qHW",
        "WWq..qWw",
        "HWq..qWw",
        "WWq..qWw",
        "HWq..qWw",
        "WWq..qWw",
        "ntTSXTtn",
        "ntTSXTtn",
        "WWq..qWw",
        "WWq..qWw",
        "qQq..qQq",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "QqQQQQqQ",
        "wWwHHwWw",
        "HWWWWWWw",
        "WWqQQqWw",
        "WWQwwQWw",
        "WWqQQqWw",
        "WWWHHWWw",
        "ntTTTTtn",
        "ntTTTTtn",
        "WWWHHWWw",
        "WWWHHWWw",
        "qQqqqqQq",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["qQqQ", "dwWH", "dwWW", "dwWW", "dwWW", "dwWW", "dwWW", "ntTt", "ntTt", "dwWW", "dwwW", "qQqq"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["Q"] if y in (0, h - 1) or x in (0, w - 1) else (None if x in (3, 4) and y < 3 else pal["q"]))

    arm = {
        "front": ["XSSs", "SSsz", "sszZ", "nTTn", "WHwd", "WWwd", "Wwwd", "qQQq", "SssZ", "sSzZ", "szzZ", "...."],
        "back": ["SSSs", "ssZz", "sszZ", "ntTn", "wwdd", "wwdd", "wddd", "lqql", "sszZ", "sszZ", "zzZZ", "...."],
        "west": ["zsSX", "zZsS", "ZzzS", "ntTn", "dwWH", "dwwW", "ddwW", "lqQq", "ZssS", "ZsSs", "ZzzS", "...."],
        "east": ["Xszs", "SZzs", "SzzZ", "ntTn", "ddww", "ddww", "dddw", "lqql", "SssZ", "SssZ", "ZzzZ", "...."],
        "top": ["XSsz", "SSsz", "SsZz", "szzZ"],
    }
    L.arm_pair(cv, arm, pal)
    leg = {
        "front": ["WWwt", "HWwt", "WWwt", "WWwt", "WWwt", "WWwt", "qQqt", "....", "qlql", "lqlq", "zSsZ", "OPOo"],
        "back": ["wwwt", "wwwt", "wwwt", "wwdt", "wwdt", "wddt", "lqqt", "....", "qlql", "lqlq", "zzsz", "OOoo"],
        "west": ["dwWH", "dwWW", "dwww", "dwww", "dwww", "dwww", "lqQq", "....", "lqQl", "qlQq", "zSsZ", "oOPO"],
        "east": ["HWwd", "WWwd", "wwwd", "wwwd", "wwwd", "wwwd", "qQql", "....", "lQql", "lqQq", "ZsSz", "OPOo"],
    }
    L.leg_pair(cv, leg, pal)

    L.check_hat_top_empty(cv)
    # the headband also on row 2 of the hat layer (front, sides, back), a steel plate in the middle of the front
    for face in ("front", "west", "east", "back"):
        L.fill(cv, "head", face, None, overlay=True, fn=lambda x, y, w, h, face=face: (pal["S"] if (face == "front" and x in (3, 4)) else (pal["s"] if (face == "front" and x in (2, 5)) else pal["T"] if (x + y) % 2 else pal["t"])) if y == 2 else None)
    L.check_hat_top_empty(cv)
    fx, fy, _, _ = cv.front_rect("head", False)
    glow = L.glow_image([(fx + 1, fy + 5, pal["M"]), (fx + 2, fy + 5, pal["v"]), (fx + 1, fy + 6, pal["v"])])
    return cv.image(), glow, "anime_short_open", L.hx("#2a2f58"), False
