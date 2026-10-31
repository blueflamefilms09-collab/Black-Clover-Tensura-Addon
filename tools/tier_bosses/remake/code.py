"""Code (Boruto, Kara's Inner): hand-painted skin, 3.37 remake round (group c).

Design: tall cold figure in a slate high-collared battle robe, a charcoal tabard down the front framed by two cyan glyph lines (the seal-script motif of the roster sheet), steel pauldrons with a cyan
rim, bulky steel gauntlets over black gloves, a steel belt with a cyan gem, robe skirt panels with a cyan hem, steel greaves over black boots; spiky white hair (kit style anime_spiky), glowing cyan
eyes under flat brows, a cyan seal on the forehead. Colour blocking: slate / charcoal / white hair / one cyan glow accent (the cyan texels are the glow map). From memory of the canon design
(Kara, white hair, cold bearing; the wikis were not reachable) and the roster's sheet (robe, forehead sigil).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_c as L  # noqa: E402

K = L.K


def paint(spec, cid, masks):
    cv = K.Canvas(slim=False)
    pal = {}
    L.skin_pal(pal, L.hx("#ead2c8"), (0.74, 0.87, 1.0, 1.07, 1.13))
    L.hair_pal(pal, L.hx("#f2f5ff"), (0.56, 0.76, 1.0, 1.1, 1.2))
    L.put_ramp(pal, "dwWhH", L.hx("#4b5f6b"), (0.55, 0.76, 1.0, 1.2, 1.42))              # slate robe
    L.put_ramp(pal, "ntTN", L.hx("#2a353c"), (0.6, 0.85, 1.0, 1.3))                        # charcoal tabard
    L.put_ramp(pal, "ZzsSX", L.hx("#6c8591"), (0.5, 0.75, 1.0, 1.28, 1.6))                 # steel
    L.put_ramp(pal, "gGyY", L.hx("#6fe3f0"), (0.5, 0.78, 1.0, 1.3))                        # cyan glow
    pal.update({"k": (12, 16, 20), "V": (26, 32, 38), "v": (46, 54, 62), "L": (30, 40, 50), "B": (58, 66, 78), "m": (128, 86, 92), "*": (240, 252, 255), "C": pal["y"], "c2": pal["Y"]})

    L.skin_base(cv, pal)
    # ---------------------------------------------------------------- head
    L.m(cv, "head", "front", [
        "34555543",
        "23455432",
        "bbbyybbb",
        "cBBccBBc",
        "cLLccLLc",
        "cC*ccYCc",
        "cbbccbbc",
        "cccmmccc",
    ], pal)
    L.setp(cv, "head", "front", 4, 5, pal["C"])
    L.setp(cv, "head", "front", 5, 5, pal["*"])
    L.setp(cv, "head", "front", 3, 6, pal["b"])
    L.setp(cv, "head", "front", 3, 2, pal["Y"])
    L.setp(cv, "head", "front", 4, 2, pal["y"])
    L.head_shell(cv, pal, h_hi="5", h_mid="4", h_lo="3", h_dk="2", side_hair_rows=3)

    # ---------------------------------------------------------------- body base: charcoal under-robe
    L.fill(cv, "body", "front", None, fn=lambda x, y, w, h: pal["T"] if y < 8 else pal["V"])
    L.fill(cv, "body", "back", None, fn=lambda x, y, w, h: pal["t"])
    L.fill(cv, "body", "west", None, fn=lambda x, y, w, h: pal["t"])
    L.fill(cv, "body", "east", None, fn=lambda x, y, w, h: pal["t"])
    L.fill(cv, "body", "top", pal["w"])
    L.fill(cv, "body", "bottom", pal["V"])
    L.setp(cv, "body", "front", 3, 0, pal["f"])
    L.setp(cv, "body", "front", 4, 0, pal["e"])
    for part in ("arm_r", "arm_l"):
        for face in ("front", "back", "west", "east"):
            def af(x, y, w, h, face=face):
                if y >= 11:
                    return pal["v"] if face == "front" else pal["V"]
                if y >= 8:
                    return pal["s"] if face == "front" else pal["z"]
                return pal["W"] if face == "front" else pal["w"]
            L.fill(cv, part, face, None, fn=af)
        L.fill(cv, part, "top", pal["s"])
        L.fill(cv, part, "bottom", pal["V"])
    for part in ("leg_r", "leg_l"):
        for face in ("front", "back", "west", "east"):
            def lf(x, y, w, h, face=face):
                if y >= 11:
                    return pal["v"] if face == "front" else pal["V"]
                if y >= 8:
                    return pal["s"] if face == "front" else pal["z"]
                return pal["T"] if face == "front" else pal["t"]
            L.fill(cv, part, face, None, fn=lf)
        L.fill(cv, part, "top", pal["t"])
        L.fill(cv, part, "bottom", pal["k"])

    # ================================================================ OVERLAY
    L.m(cv, "body", "front", [
        "zSs..sSz",
        "wzsGGszw",
        "wWyTTyWw",
        "wWGNTGWw",
        "wWGTTGWw",
        "wWGNTGWw",
        "wWyTTyWw",
        "ZzzzzzzZ",
        "ZsSyYSsZ",
        "wWGNTGWw",
        "wWGTTGWw",
        "GgyGGygG",
    ], pal, overlay=True)
    L.m(cv, "body", "back", [
        "zSSSSSSz",
        "wzsssszw",
        "wWWHHWWw",
        "wWGyyGWw",
        "wWGTTGWw",
        "wWGGGGWw",
        "wWWHHWWw",
        "ZzzzzzzZ",
        "ZsSSSSsZ",
        "wWWHHWWw",
        "wWWHHWWw",
        "GgyGGygG",
    ], pal, overlay=True)
    L.side_pair(cv, "body", ["dwWw", "dwsz", "dwwd", "dwwd", "dwwd", "dwwd", "dwwd", "ZzzZ", "ZsSZ", "dwwd", "dwwd", "gGyG"], pal)
    L.fill(cv, "body", "top", None, overlay=True, fn=lambda x, y, w, h: pal["S"] if y in (0, h - 1) or x in (0, w - 1) else (None if x in (3, 4) and y < 3 else pal["s"]))

    # arms: steel pauldron with a cyan rim, slate sleeve, a cyan cuff, a bulky steel gauntlet with a cyan gem, black glove
    arm = {
        "front": ["XSsz", "SsZz", "szzZ", "GyyG", "HWwd", "WWwd", "Wwdd", "yGGy", "SssZ", "sSyZ", "szzZ", "vVVk"],
        "back": ["SsSz", "ssZz", "szzZ", "gGGg", "wwdd", "wwdd", "wddd", "gGGg", "sszZ", "sszZ", "zzZZ", "VVVk"],
        "west": ["zsSX", "zZsS", "ZzzS", "GyyG", "dwWH", "dwwW", "ddwW", "GGyy", "ZssS", "ZsSs", "ZzzS", "kVVv"],
        "east": ["Xszs", "SZzs", "SzzZ", "gGGg", "ddww", "ddww", "dddw", "gGGg", "SssZ", "SssZ", "ZzzZ", "kVVV"],
        "top": ["XSsz", "SSsz", "SsZz", "szzZ"],
    }
    L.arm_pair(cv, arm, pal)

    leg = {
        "front": ["WWwd", "WWwd", "WWwd", "WWwd", "WWwd", "WWwd", "Wwwd", "gyyG", "SXsZ", "SssZ", "szzZ", "vVVk"],
        "back": ["wwwd", "wwwd", "wwwd", "wwwd", "wwwd", "wwdd", "wddd", "gGGg", "sszZ", "sszZ", "zzZZ", "VVVk"],
        "west": ["dwww", "dwww", "dwww", "dwww", "dwww", "dwww", "dwww", "gGyG", "zSsZ", "zssZ", "zzZZ", "VVVk"],
        "east": ["wwwd", "wwwd", "wwwd", "wwwd", "wwwd", "wwwd", "wwwd", "GyGg", "ZsSz", "ZssZ", "ZZzz", "kVVV"],
    }
    L.leg_pair(cv, leg, pal)

    L.check_hat_top_empty(cv)
    glow = L.glow_of(cv, [pal["y"], pal["Y"], pal["G"]])
    return cv.image(), glow, "anime_spiky", L.hx("#f2f5ff"), False
