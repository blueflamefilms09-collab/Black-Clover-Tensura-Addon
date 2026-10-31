"""Foxy, the Silver Fox (One Piece, captain of the Foxy Pirates): hand-painted skin, 3.37 remake round (group c).

Design: a showman in a long pale blue-grey captain's coat with orange piping, a huge fluffy silver fur collar, dark slate pauldrons rimmed in orange, an orange sash with a cream buckle, a cream
shirt, dark trousers and dark boots with big orange cuffs; a big silver afro (kit style anime_afro, a clear fox-like mane), sharp brown eyes, arched brows, a smirk, a grey goatee; the Foxy
Pirates fox-head emblem in orange on the back of the coat. Colour blocking: pale coat / dark plates and trousers / orange / silver fur. Look: from memory of the canon design (the wikis were not
reachable) and the roster sheet (silver afro, goatee, coat, sash).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_c as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    pal = {}
    L.skin_pal(pal, L.hx("#e9c3a8"), (0.74, 0.87, 1.0, 1.07, 1.14))
    L.hair_pal(pal, L.hx("#d4d8e2"), (0.52, 0.74, 1.0, 1.15, 1.3))
    L.put_ramp(pal, "dwWhH", L.hx("#8ea6b6"), (0.5, 0.74, 1.0, 1.2, 1.4))             # pale blue-grey coat
    L.put_ramp(pal, "ZzsSX", L.hx("#4a6270"), (0.5, 0.8, 1.0, 1.3, 1.7))                  # dark slate plates
    L.put_ramp(pal, "gGyY", L.hx("#ff8a3c"), (0.55, 0.8, 1.0, 1.3))                       # orange
    L.put_ramp(pal, "lqQ", L.hx("#f1e8d6"), (0.82, 0.95, 1.05))                           # cream shirt
    L.put_ramp(pal, "ntTN", L.hx("#33444f"), (0.6, 0.85, 1.0, 1.25))                      # trousers
    L.put_ramp(pal, "oOP", L.hx("#202c35"), (0.7, 1.0, 1.5))                              # boots
    pal.update({"L": (40, 30, 34), "D": (96, 60, 36), "M": (150, 104, 62), "I": (214, 170, 110), "*": (252, 248, 240), "B": (96, 98, 108), "m": (170, 102, 96), "k": (70, 72, 82), "j": (126, 128, 138), "V": (24, 30, 36)})

    L.skin_base(cv, pal)
    # ---------------------------------------------------------------- head
    L.m(cv, "head", "front", [
        "34555543",
        "23455432",
        "bbbbbbbb",
        "cBBccBBc",
        "cLLccLLc",
        "cD*cb*Dc",
        "cccmmmcc",
        "ccjkkjcc",
    ], pal)
    L.setp(cv, "head", "front", 0, 3, pal["b"])
    L.setp(cv, "head", "front", 7, 3, pal["b"])
    L.setp(cv, "head", "front", 1, 6, pal["b"])
    L.head_shell(cv, pal, h_hi="5", h_mid="4", h_lo="3", h_dk="2", side_hair_rows=3)

    # ---------------------------------------------------------------- body base: cream shirt, dark trousers
    L.m(cv, "body", "front", [
        "WWqQQqWW",
        "WWqQQqWW",
        "WWqQqqWW",
        "WWlQqlWW",
        "WWqqqqWW",
        "WWlqqlWW",
        "WWqqqqWW",
        "WWqqqqWW",
        "GyYqqYyG",
        "ntTTTTtn",
        "ntTTTTtn",
        "nttTTttn",
    ], pal)
    L.fill(cv, "body", "back", None, fn=lambda x, y, w, h: pal["W"] if y < 9 else pal["t"])
    L.fill(cv, "body", "west", None, fn=lambda x, y, w, h: pal["w"] if y < 9 else pal["n"])
    L.fill(cv, "body", "east", None, fn=lambda x, y, w, h: pal["w"] if y < 9 else pal["n"])
    L.fill(cv, "body", "top", pal["W"])
    L.fill(cv, "body", "bottom", pal["V"])
    L.setp(cv, "body", "front", 3, 0, pal["e"])
    L.setp(cv, "body", "front", 4, 0, pal["c"])
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            L.fill(cv, part, face, None, fn=lambda x, y, w, h, face=face: (pal["c"] if face == "front" else pal["b"]) if y >= 9 else (pal["W"] if face == "front" else pal["w"]))
        L.fill(cv, part, "top", pal["h"])
        L.fill(cv, part, "bottom", pal["b"])
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y >= 8:
                    return pal["O"] if face == "front" else pal["o"]
                return pal["T"] if face == "front" else pal["t"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["t"])
        L.fill(cv, part, "bottom", pal["V"])

    # ================================================================ OVERLAY
    L.m(cv, "body", "front", [
        "543..345",
        "45343454",
        "54W..W45",
        "HWT..TWw",
        "WWT..TWw",
        "HWT..TWw",
        "WWT..TWw",
        "GyyyyyyG",
        "gGyqQyGg",
        "WWT..TWw",
        "WWT..TWw",
        "yGy..yGy",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "54333345",
        "45343454",
        "HWWWWWWw",
        "WWyWWyWw",
        "WWyyyyWw",
        "WWykkyWw",
        "WWWqqWWw",
        "GyyyyyyG",
        "gGyyyyGg",
        "WWWWWWWw",
        "WwWWWWww",
        "yGyyyyGy",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["5434", "4543", "dwWW", "dwWW", "dwWW", "dwWW", "dwWW", "GyyG", "gGyG", "dwWW", "dwwW", "yGyy"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["4"] if (x + y) % 2 else pal["5"])

    arm = {
        "front": ["5453", "SsZz", "szzZ", "GyyG", "HWwd", "WWwd", "Wwwd", "yYYy", "yGGy", "....", "....", "...."],
        "back": ["4534", "ssZz", "szzZ", "gGGg", "wwdd", "wwdd", "wddd", "gyyg", "gGGg", "....", "....", "...."],
        "west": ["4354", "zZsS", "ZzzS", "GyyG", "dwWH", "dwwW", "ddwW", "GyYy", "GGyG", "....", "....", "...."],
        "east": ["3454", "SZzs", "SzzZ", "gGGg", "ddww", "ddww", "dddw", "gyyg", "gGGg", "....", "....", "...."],
        "top": ["5454", "4545", "5454", "4545"],
    }
    L.arm_pair(cv, arm, pal)
    leg = {
        "front": ["WWwT", "HWwT", "WWwT", "WWwT", "WWwt", "WWwt", "yGGt", "....", "yYYy", "OPOo", "OOOo", "ooVV"],
        "back": ["wwwd", "wwwd", "wwwd", "wwdd", "wwdd", "wddd", "gGGg", "....", "gyyg", "OOoo", "OOoo", "ooVV"],
        "west": ["dwWH", "dwWW", "dwww", "dwww", "dwww", "dwww", "GyyG", "....", "GyYy", "oOPO", "oOOO", "VooO"],
        "east": ["HWwd", "WWwd", "wwwd", "wwwd", "wwwd", "wwwd", "GyyG", "....", "yYyG", "OPOo", "OOOo", "OooV"],
    }
    L.leg_pair(cv, leg, pal)

    L.check_hat_top_empty(cv)
    glow = None
    return cv.image(), glow, "anime_afro", L.hx("#d4d8e2"), False
