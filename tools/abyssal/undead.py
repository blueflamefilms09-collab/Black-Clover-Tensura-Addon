"""Why: the lich-family looks (lich, Acererak, Vecna, the Lich King) share one skeleton-in-a-robe player skin; this file paints it from a palette so that each of them differs by colour,
by what hangs on it (their own look files) and by a few flags, not by four copies of the same pixel art. Everything draws on the 64x64 player skin through the same Cv faces the geo cubes use.
"""
import math
import lib
from lib import Cv, flat, bone, cloth, over, on, rgb, shade, mix


def skull_head(skin, P, phase):
    """The skull on the base layer of the head. P: bone, dark, eye (glow colour), eye2 (second glow colour), eye_n (eye socket height 2..3), one_eye (Vecna: the left socket is sewn shut), crack."""
    b = P["bone"]
    skin.paint("head", bone(b, 0.05))
    dark = P.get("dark", "#120c18")
    en = min(3, P.get("eye_n", 2) + (1 if phase >= 3 else 0))
    f = skin.face("head", "front")
    sock = lambda x0: [(x0 + i, 3 - (en - 2) + j) for i in range(2) for j in range(en)]
    for x0, which in ((1, "l"), (5, "r")):
        if P.get("one_eye") and which == P["one_eye"]:                  # a sewn, scarred socket instead of an eye
            for (x, y) in sock(x0):
                f.px(x, y, shade(b, 0.55))
            f.line(x0, 3, x0 + 1, 4, "#3a2a30")
            f.line(x0 + 1, 3, x0, 4, "#3a2a30")
            continue
        for (x, y) in sock(x0):
            f.px(x, y, dark)
        gx = x0 if which == "l" else x0 + 1
        for j in range(en):
            f.px(gx, 3 - (en - 2) + j, P["eye"], P["eye"])
        f.glowpx(gx, 3 - (en - 2), P.get("eye2", P["eye"]))
    f.px(3, 5, dark)
    f.px(4, 5, dark)
    f.px(3, 6, shade(b, 0.7))
    f.px(4, 6, shade(b, 0.7))
    for x in range(1, 7):
        f.px(x, 7, b if x % 2 else shade(b, 0.45))                    # the teeth
    f.px(1, 6, shade(b, 0.62))
    f.px(6, 6, shade(b, 0.62))
    for x in (0, 7):
        f.px(x, 5, shade(b, 0.72))
    f.rect(1, 2, 2, 1, shade(b, 0.78))
    f.rect(5, 2, 2, 1, shade(b, 0.78))
    if P.get("brow_gems"):
        f.px(3, 1, P["gem"], P["gem"])
        f.px(4, 1, P["gem"], P["gem"])
    if phase >= 2 or P.get("cracked"):
        f.line(6, 0, 4, 2, dark, P.get("crack_glow", P["eye2"]))
        if phase >= 3:
            f.line(1, 0, 2, 2, dark, P.get("crack_glow", P["eye2"]))
    for face in ("right", "left"):
        s = skin.face("head", face)
        s.rect(2, 3, 2, 2, dark)                                       # the ear hole
        s.rect(0, 5, s.w, 1, shade(b, 0.8))
        s.glowpx(2, 3, P["eye2"], 140) if phase >= 2 else None
    back = skin.face("head", "back")
    back.line(3, 0, 4, 3, shade(b, 0.6))
    back.line(4, 3, 3, 5, shade(b, 0.6))
    top = skin.face("head", "top")
    top.line(3, 0, 3, 7, shade(b, 0.65))
    top.line(0, 3, 7, 4, shade(b, 0.65))


def hood(skin, P, phase):
    """The hat layer as a hood: cloth on top, back and sides, two rows and two columns of it on the front so the skull shows through a window."""
    c = P["robe"]
    m = cloth(c, 3, tatter=0, trim=P["trim"] if not P.get("no_hood_trim") else None)
    for f in ("top", "back", "right", "left", "bottom"):
        skin.paint("hat", m, (f,))
    fr = skin.face("hat", "front")
    m(fr)
    fr.clear(1, 2, 6, 6)
    fr.clear(2, 1, 4, 1)
    fr.rect(1, 2, 1, 6, shade(c, 0.7))
    fr.rect(6, 2, 1, 6, shade(c, 0.7))
    fr.rect(2, 1, 4, 1, P["trim"])
    for f in ("right", "left"):
        s = skin.face("hat", f)
        s.clear(0, 3, 2, 5)                                              # the hood opens at the cheek
        s.clear(s.w - 1, 6, 1, 2)


def ribcage(skin, P, phase, parts=True):
    b, dark = P["bone"], P.get("dark", "#120c18")
    skin.paint("body", bone(b, 0.04))
    f = skin.face("body", "front")
    f.rect(0, 0, 8, 12, shade(b, 0.4))
    f.rect(3, 0, 2, 12, shade(b, 0.85))                               # the breastbone
    for y in (1, 3, 5, 7):
        f.rect(0, y, 3, 1, b)
        f.rect(5, y, 3, 1, b)
        f.px(0, y, shade(b, 0.7))
        f.px(7, y, shade(b, 0.7))
    f.rect(2, 9, 4, 2, shade(b, 0.6))                                  # the pelvis
    f.rect(3, 11, 2, 1, dark)
    if phase >= 2:
        for y in (2, 4, 6, 8):
            f.rect(0, y, 3, 1, dark, P["soul"])                      # the soul fire shows between the ribs
            f.rect(5, y, 3, 1, dark, P["soul"])
    bk = skin.face("body", "back")
    bk.rect(0, 0, 8, 12, shade(b, 0.5))
    bk.rect(3, 0, 2, 12, shade(b, 0.9))
    for y in (1, 3, 5, 7):
        bk.rect(0, y, 3, 1, b)
        bk.rect(5, y, 3, 1, b)


def robe_body(skin, P, phase):
    """The jacket layer: an open robe over the ribs (a V at the throat), a dark belt, a tattered hem, a rune on the back."""
    c, t = P["robe"], P["trim"]
    m = cloth(c, 3, tatter=0)
    for f in ("top", "bottom", "right", "left"):
        skin.paint("jacket", cloth(c, 3, tatter=2, trim=t), (f,))
    fr = skin.face("jacket", "front")
    cloth(c, 3, tatter=2)(fr)
    fr.clear(2, 0, 4, 2)
    fr.clear(3, 2, 2, 7)
    fr.clear(3, 0, 2, 9)
    for y in range(0, 9):                                                # the V of the neckline
        w = max(1, 4 - y // 2)
        fr.clear(4 - w // 2 - (0 if w % 2 == 0 else 0), y, w, 1) if False else None
    fr.rect(0, 0, 1, 11, t)
    fr.rect(7, 0, 1, 11, t)
    fr.rect(2, 9, 4, 2, shade(c, 0.6)) if False else None
    belt = P.get("belt", "#1c1420")
    fr.rect(0, 8, 8, 2, belt)
    fr.rect(3, 8, 2, 2, P.get("buckle", t))
    if P.get("rune"):
        fr.glowpx(3, 8, P["rune"]); fr.glowpx(4, 9, P["rune"])
    bk = skin.face("jacket", "back")
    cloth(c, 3, tatter=3)(bk)
    bk.rect(0, 8, 8, 2, belt)
    if P.get("rune"):
        r = P["rune"]
        for (x, y) in ((3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (4, 4), (3, 5), (4, 5), (2, 6), (5, 6)):
            bk.px(x, y, shade(c, 1.5), r)
    for f in ("right", "left"):
        s = skin.face("jacket", f)
        s.rect(0, 8, s.w, 2, belt)


def arms_legs(skin, P, phase, long_sleeves=True, gloves=None):
    b, c, t = P["bone"], P["robe"], P["trim"]
    for part, sl in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        skin.paint(part, bone(b, 0.04))
        for f in ("front", "back", "right", "left"):
            cv = skin.face(part, f)
            cv.rect(0, 6, cv.w, 1, shade(b, 0.6))                      # the elbow joint
            cv.rect(0, 11, cv.w, 1, shade(b, 0.7))                      # the wrist
            if P.get("bone_gloves"):
                cv.rect(0, 9, cv.w, 3, shade(b, 0.8))
        sleeve = cloth(c, 3, tatter=2, trim=t)
        for f in ("front", "back", "right", "left", "top"):
            cv = skin.face(sl, f)
            sleeve(cv)
            if f != "top":
                cv.clear(0, 6 if long_sleeves else 4, cv.w, 6 if long_sleeves else 8)
                for x in range(cv.w):                                   # the ragged cuff
                    cv.px(x, 6 + (x * 5 + 2) % 3 - (1 if long_sleeves else 3), c) if x % 2 == 0 else None
    for part, pt in (("rleg", "rpants"), ("lleg", "lpants")):
        skin.paint(part, bone(b, 0.04))
        for f in ("front", "back", "right", "left"):
            cv = skin.face(part, f)
            cv.rect(0, 5, cv.w, 1, shade(b, 0.55))                      # the knee
            cv.rect(0, 11, cv.w, 1, shade(b, 0.6))
        sk = cloth(c, 3, tatter=3)
        for f in ("front", "back", "right", "left", "top"):
            cv = skin.face(pt, f)
            sk(cv)
            if f != "top":
                cv.clear(0, 7, cv.w, 5)


def make_skin(P, phase, slim=False):
    sk = lib.Skin(slim=slim, seed=lib.seed_of(P["id"], phase))
    skull_head(sk, P, phase)
    ribcage(sk, P, phase)
    arms_legs(sk, P, phase)
    if P.get("hood", True):
        hood(sk, P, phase)
    robe_body(sk, P, phase)
    sk.opaque_base()
    return sk
