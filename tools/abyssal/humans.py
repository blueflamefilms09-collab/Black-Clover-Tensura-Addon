"""Why: the living humanoid looks (Strahd, Zariel, Asmodeus, Orcus, Solar, the Lich King's armour) are painted onto the 64x64 player skin from one parameterised painter, the way undead.py does the
skeletons: a face with eyes, brows, a mouth, a hat layer that is hair, a coat or plate on the jacket layer, sleeves and cuffs, trousers and boots. A look changes the numbers in its palette dict P
and adds its own decals; the extras (wings, horns, capes, weapons) are its own file's business.

P keys: skin (tones: a colour), eye, eye_glow, hair (colour or None), hair_style (slick, long, mohawk, bald, crest), mouth (line, fangs, grin), brow, coat, trim, shirt, belt, buckle, boots, glove,
armor (None or a plate colour), armor_trim, glow (a trim glow colour or None), cuff, scale (a scale texture for the skin: demons), veins, phase_glow (extra glow per phase).
"""
import lib
from lib import over, on, rgb, shade, mix, flat, plate, scales, cloth, metal, fur, banded, flesh, Skin


def skin_mat(P):
    if P.get("scale"):
        return scales(P["skin"], P["scale"])
    return flesh(P["skin"], P.get("veins"), 0.015)


def face(sk, P, phase):
    sk.paint("head", skin_mat(P))
    f = sk.face("head", "front")
    sd = shade(P["skin"], 0.55)
    ey, g = P["eye"], P.get("eye_glow", P["eye"])
    big = P.get("eye_big", 0) + (1 if phase >= 3 and P.get("eye_grow", True) else 0)
    for x0 in (1, 5):
        f.rect(x0, 3, 2, 1 + (1 if big else 0), "#e8e2d8" if not P.get("eye_solid") else ey, g if P.get("eye_solid") else None)
        f.px(x0 + (1 if x0 == 1 else 0), 3, ey, g)
        if big:
            f.px(x0 + (1 if x0 == 1 else 0), 4, ey, g)
    br = P.get("brow", shade(P["skin"], 0.45))
    f.rect(1, 2, 2, 1, br)
    f.rect(5, 2, 2, 1, br)
    if P.get("angry", True):
        f.px(2, 2, shade(br, 0.7))
        f.px(5, 2, shade(br, 0.7))
    f.px(3, 5, sd)
    f.px(4, 5, sd)
    m = P.get("mouth", "line")
    mc = P.get("mouth_color", "#2a1018")
    if m == "line":
        f.rect(2, 6, 4, 1, mc)
    elif m == "fangs":
        f.rect(2, 6, 4, 1, mc)
        f.px(2, 7, "#f0ece0")
        f.px(5, 7, "#f0ece0")
    elif m == "grin":
        f.rect(1, 6, 6, 2, mc)
        for x in (1, 3, 5):
            f.px(x, 6, "#f0ece0")
        for x in (2, 4, 6):
            f.px(x, 7, "#f0ece0")
    if P.get("scar"):
        f.line(*P["scar"], "#7a2020")
    if P.get("mark"):
        f.rect(3, 0, 2, 1, P["mark"], P.get("mark_glow"))
    for side in ("right", "left"):
        s = sk.face("head", side)
        s.px(2, 4, sd)
    if P.get("pointed_ears"):
        pass


def hair(sk, P, phase):
    h = P.get("hair")
    if not h:
        return
    st = P.get("hair_style", "slick")
    hm = fur(h, shade(h, 1.4))
    for fc in ("top", "back", "right", "left"):
        sk.paint("hat", hm, (fc,))
    fr = sk.face("hat", "front")
    hm(fr)
    fr.clear(0, 2, 8, 6)
    if st == "slick":
        fr.clear(2, 1, 4, 1)
        fr.px(3, 1, h)
        fr.px(4, 1, h)
        fr.clear(3, 1, 2, 1)
        fr.px(0, 2, h); fr.px(7, 2, h)
        fr.px(0, 3, h); fr.px(7, 3, h)
    elif st == "long":
        fr.px(0, 2, h); fr.px(0, 3, h); fr.px(0, 4, h); fr.px(0, 5, h); fr.px(7, 2, h); fr.px(7, 3, h); fr.px(7, 4, h); fr.px(7, 5, h)
        fr.clear(2, 1, 4, 1)
    elif st == "crest":
        fr.clear(0, 0, 8, 1)
    for fc in ("right", "left", "back"):
        s = sk.face("hat", fc)
        if st == "long":
            s.rect(0, 0, s.w, 8, h)
        else:
            s.clear(0, 3, s.w, 5) if fc != "back" else None
    sk.face("hat", "bottom").img[...] = 0


def coat(sk, P, phase):
    """Jacket + sleeves + pants layers: a coat with an open front over the shirt, a belt, cuffs, boots."""
    c, t = P["coat"], P.get("trim", "#c8a040")
    shirt = P.get("shirt", "#d8d0c0")
    sk.paint("body", flat(shirt, 0.06))
    for part in ("rarm", "larm"):
        sk.paint(part, skin_mat(P))
    for part in ("rleg", "lleg"):
        sk.paint(part, flat(P.get("legs", "#1c1a22"), 0.06))
    cm = cloth(c, 3, trim=t, trim_rows=1)
    for f in ("top", "bottom", "back", "right", "left"):
        sk.paint("jacket", cm, (f,))
    fr = sk.face("jacket", "front")
    cm(fr)
    fr.clear(3, 0, 2, 9)
    fr.rect(2, 0, 1, 9, t)
    fr.rect(5, 0, 1, 9, t)
    fr.rect(0, 8, 8, 2, P.get("belt", "#20161a"))
    fr.rect(3, 8, 2, 2, P.get("buckle", t))
    if P.get("buttons"):
        for y in (1, 3, 5, 7):
            fr.px(2, y, P["buttons"])
            fr.px(5, y, P["buttons"])
    if P.get("emblem_glow"):
        fr.glowpx(3, 8, P["emblem_glow"])
    for sl in ("rsleeve", "lsleeve"):
        for f in ("front", "back", "right", "left", "top", "bottom"):
            cv = sk.face(sl, f)
            cm(cv)
            if f in ("front", "back", "right", "left"):
                cv.rect(0, 9, cv.w, 3, P.get("cuff", t))
                cv.rect(0, 9, cv.w, 1, shade(P.get("cuff", t), 1.3))
    for pt in ("rpants", "lpants"):
        for f in ("front", "back", "right", "left", "top", "bottom"):
            cv = sk.face(pt, f)
            flat(P.get("legs", "#1c1a22"), 0.06)(cv)
            if f in ("front", "back", "right", "left"):
                cv.rect(0, 7, cv.w, 5, P.get("boots", "#14100f"))
                cv.rect(0, 7, cv.w, 1, P.get("boot_trim", shade(P.get("boots", "#14100f"), 1.8)))


def armour(sk, P, phase):
    """Plate over the coat layers: breastplate, pauldron caps, vambraces, gauntlets, greaves, a glow in the trim seams."""
    a = P["armor"]
    tr = P.get("armor_trim", "#c8a040")
    g = P.get("glow")
    pm = plate(a, rim=tr)
    for f in ("front", "back", "right", "left", "top", "bottom"):
        cv = sk.face("jacket", f)
        pm(cv)
        if f in ("front", "back"):
            cv.rect(0, 0, cv.w, 1, tr)
            cv.rect(0, 9, cv.w, 3, shade(a, 0.7))
            cv.rect(0, 9, cv.w, 1, tr, g)
            if f == "front":
                cv.line(3, 2, 3, 7, shade(a, 0.5), g)
                cv.line(4, 2, 4, 7, shade(a, 0.5), g)
                cv.rect(2, 3, 4, 1, shade(a, 1.3))
    for sl in ("rsleeve", "lsleeve"):
        for f in ("front", "back", "right", "left", "top", "bottom"):
            cv = sk.face(sl, f)
            pm(cv)
            if f in ("front", "back", "right", "left"):
                cv.rect(0, 0, cv.w, 2, shade(a, 1.2))
                cv.rect(0, 2, cv.w, 1, tr, g)
                cv.clear(0, 5, cv.w, 1) if False else None
                cv.rect(0, 6, cv.w, 1, tr, g)
                cv.rect(0, 9, cv.w, 3, shade(a, 0.75))
    for pt in ("rpants", "lpants"):
        for f in ("front", "back", "right", "left", "top", "bottom"):
            cv = sk.face(pt, f)
            pm(cv)
            if f in ("front", "back", "right", "left"):
                cv.rect(0, 5, cv.w, 1, tr, g)
                cv.rect(0, 9, cv.w, 3, shade(a, 0.65))


def scalp(sk, P):
    """For the player-model kit, which draws the hair itself (its own styles, tinted, with springs): the skin only has the scalp under the hair, in the hair colour."""
    h = P.get("hair")
    if not h:
        return
    hm = fur(h, shade(h, 1.4))
    sk.paint("head", hm, ("top", "back"))
    for f in ("right", "left"):
        cv = sk.face("head", f)
        hm(cv)
        cv.rect(0, 4, cv.w, cv.h - 4, P["skin"])
        cv.rect(2, 3, 2, 2, shade(P["skin"], 0.55))
    fr = sk.face("head", "front")
    fr.rect(0, 0, 8, 1, h)
    fr.rect(0, 1, 1, 2, h)
    fr.rect(7, 1, 1, 2, h)


def make_skin(P, phase, slim=False, extra=None):
    sk = Skin(slim=slim, seed=lib.seed_of(P["id"], phase))
    face(sk, P, phase)
    coat(sk, P, phase)
    if P.get("armor"):
        armour(sk, P, phase)
    if P.get("kit_hair"):
        scalp(sk, P)
    else:
        hair(sk, P, phase)
    if extra:
        extra(sk, P, phase)
    sk.opaque_base()
    return sk
