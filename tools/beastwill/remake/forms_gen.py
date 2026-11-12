#!/usr/bin/env python3
"""The 27 Beast Will forms: palettes, per-will layout, and build(wid) -> Scene. Preview: python3 forms_gen.py <ids|all>"""
import json, math, os, sys
import numpy as np
sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_parts import *
from forms_heads import head
from render_preview import SPEC

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = {w["id"]: w for w in json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))}

# body, back, belly, plate, trim, glow, [cloth, mane, extra(membrane/leaf/flame)], pattern
PAL = {
 # 3.76.0 look pass: every will keeps its identity colour but has real darks: body = the mid tone of the hide, back = its dark, belly = its light, plate = armour that differs in VALUE from the hide
 # (dark metal on a bright beast, pale bone on a dark one), trim = the bright metal edge, glow = one saturated accent. The 3.61.0 palettes are in git (and drawn by classicForms).
 "dragon_will": ("8C76E6", "46369A", "D6CAFF", "33286E", "E8B63C", "B592FF", "40205E", "5C46B4", "6C54C6", "scale"),
 "elderwood_guardian_will": ("4A6E30", "231A10", "6A9A44", "4A2E18", "9CBC4E", "35E0D0", "1F6A34", "2FBF5A", "2FBF5A", "bark"),
 "shadow_panther_will": ("2A2A40", "0E0E18", "44446A", "4A2C7C", "8E52E6", "D6FF4A", "2E1A52", "22223A", "3A2A5A", "fur"),
 "lion_will": ("D8981E", "8A560E", "F2D89C", "6A2414", "F8D870", "FFD040", "A82418", "8E2A14", "6E1E12", "fur"),
 "ridge_wolf_will": ("64798A", "34424E", "C6D4DE", "232D38", "C4E2FF", "8FD4FF", "2E3A4A", "9FB4C2", "A8B8C4", "fur"),
 "thunder_fang_chimera_will": ("D8AE2A", "8A6810", "F2E8A4", "2A3688", "BFE9FF", "8FDCFF", "2A3A7A", "A85E1A", "BFE9FF", "fur"),
 "crested_boar_will": ("8E7340", "54401F", "B89C66", "45423E", "EAD9B0", "FFAE48", "5A2A1E", "4E3A20", "7A7468", "fur"),
 "wind_skimmer_will": ("3CA8C6", "17607E", "EEF9FF", "223E94", "E6F6FF", "2CFAFF", "223E94", "9CF0FF", "1C7CB4", "fur"),
 "flame_serpent_will": ("3C1A12", "150806", "742C16", "92360E", "FFC13A", "FF5A18", "5A1A10", "FF5A18", "FF7A1A", "scale"),
 "ammit_will": ("B49E56", "74622A", "E6DAA6", "7E4E12", "ECCB58", "FFD24A", "7A2A1E", "B87E1A", "D6CC8E", "scale"),
 "apophis_will": ("3E1256", "14041E", "6E2C8C", "1C0A2A", "E0204A", "FF2A5A", "3A1450", "3A1450", "8F2BD9", "scale"),
 "cerberus_will": ("40201C", "140808", "70362A", "17171F", "FF7A2A", "FF5A18", "5A0C0C", "5A1010", "FF5A18", "fur"),
 "fenrir_will": ("7AA8C8", "30537A", "DCEEF8", "223A56", "E8F6FF", "5FE4FF", "2A4666", "3E6C98", "87F1FF", "fur"),
 "cu_sith_will": ("1F6E54", "0A2A20", "7CCCA8", "12342A", "C8FFE6", "7CFF8A", "0B2E22", "3E9E7A", "7CFF8A", "fur"),
 "kitsune_will": ("E66C34", "9A3A1A", "FFF0D8", "641A44", "F2C94C", "FF74AA", "8A2A5A", "FFF0D8", "FF7FB0", "fur"),
 "nue_will": ("5E4C80", "2A2040", "9080B4", "221838", "E8E04A", "E8E04A", "2A2040", "3A2A5A", "7A55B8", "fur"),
 "pegasus_will": ("CCD4E6", "6C7CA6", "F6F8FF", "B4822A", "F4D66E", "8FD0FF", "3462B4", "7EB4EA", "E4EAF6", "fur"),
 "minotaur_will": ("6E5630", "3A2B16", "8E7648", "36363C", "CC9A3C", "FF9A3A", "5A1A14", "2A1E10", "6A5532", "fur"),
 "sphinx_will": ("D8AE5C", "967634", "F2DEA0", "264EC6", "F2D270", "6A9CFF", "2B3361", "3A6FE8", "F4F0E0", "fur"),
 "hafgufa_will": ("1E8E98", "0C4850", "B7D8C0", "365646", "E8E1B0", "00E8E0", "0E5058", "1E8E98", "1E8E98", "skin"),
 "hydra_will": ("6C9A28", "2C4810", "DAE68A", "20441A", "F2E85A", "C8FF3A", "1F6B2A", "3A5A18", "7CA830", "scale"),
 "jormungandr_will": ("2458BA", "0A2856", "7AB6EE", "102A62", "7CFFB2", "7CFFB2", "0B2A5A", "1F4FB0", "6AA8E8", "scale"),
 "kraken_will": ("C8423A", "762020", "F2927A", "281C76", "3FE0D0", "3FE0D0", "2B1B6E", "D84A3E", "F0907A", "skin"),
 "nidhoggr_will": ("4C5C22", "2C1E0E", "B0B070", "BCA86A", "E4D494", "8CFF3A", "3B2A14", "3B2A14", "5A3A1A", "bone"),
 "scylla_charybdis_will": ("5C7270", "2C3836", "9CB2AC", "3A404C", "B23A3A", "3AE0D0", "3A4442", "5A6A68", "6A7A78", "stone"),
 "typhon_will": ("34283E", "120C18", "5E4A6A", "15111D", "FFC21F", "FF3A3A", "5A0A18", "D80023", "3D7BFF", "scale"),
 "yamata_no_orochi_will": ("B22058", "5C0A2C", "F2B2CA", "38220F", "F2C94C", "FF3A8A", "3A0D1A", "B02058", "E01070", "scale"),
}


def _lum(c): return float(np.dot(np.asarray(c, float), [.2126, .7152, .0722])) / 255.0


def _sat(c, k):
    c = np.asarray(c, float); g = c.mean()
    return np.clip(g + (c - g) * k, 0, 255)


def shadow(h, f):
    """the dark of a colour: darker, a little cooler and more saturated (a grey-brown shadow is what made the old forms look washed out); a near-white keeps a blue-grey shadow instead of going black"""
    c = hx(h) if isinstance(h, str) else np.asarray(h, float)
    f = f + .18 * max(0.0, _lum(c) - .62)
    return np.clip(_sat(c * f, 1.25) * np.array([.90, .94, 1.14]), 0, 255)


def hilite(h, t):
    """the light of a colour: only a little towards white, so the mid tone stays the colour people see"""
    c = hx(h) if isinstance(h, str) else np.asarray(h, float)
    return mixc(_sat(c, 1.08), [255, 250, 238], t)


def roles(wid):
    b, bk, be, pl, tr, gl, cl, mn, ex, pat = PAL[wid]
    seed = sum(map(ord, wid))
    sh = shadow
    m = {}
    m["body"] = Mat("body", b, pat, dark=sh(b, .30), light=hilite(b, .20), seed=seed + 1)
    m["back"] = Mat("back", bk, pat, dark=sh(bk, .34), light=hilite(bk, .16), seed=seed + 2)
    m["belly"] = Mat("belly", be, pat, dark=sh(be, .40), light=hilite(be, .22), seed=seed + 3)
    m["plate"] = Mat("plate", pl, "plate", dark=sh(pl, .30), light=hilite(pl, .30), glow=gl, glow_amt=.12, seed=seed + 4)
    m["gold"] = Mat("gold", tr, "plate", dark=sh(tr, .34), light=hilite(tr, .34), glow=gl, glow_amt=.18, seed=seed + 5)
    m["glow"] = Mat("glow", _sat(mixc(hx(gl), [255, 255, 255], .12), 1.15), "energy", dark=sh(gl, .55), light=mixc(hx(gl), [255, 255, 255], .55), glow=gl, glow_amt=1.0, seed=seed + 6)
    m["eye"] = Mat("eye", mixc(hx(gl), [255, 255, 255], .35), "skin", dark=hx(gl) * .8, light=mixc(hx(gl), [255, 255, 255], .7), glow=gl, glow_amt=1.0, seed=seed + 19)
    m["dark"] = Mat("dark", sh(bk, .50), "skin", dark=sh(bk, .16), light=sh(bk, .85), seed=seed + 7)
    m["claw"] = Mat("claw", "E6DCC4", "bone", dark="7A705C", light="FFFAEE", seed=seed + 8)
    m["leather"] = Mat("leather", mixc(sh(cl, .8), [52, 34, 24], .5), "cloth", dark=sh(cl, .18), light=mixc(hx(cl), [190, 150, 110], .25), seed=seed + 9)
    m["cloth"] = Mat("cloth", cl, "cloth", dark=sh(cl, .30), light=hilite(cl, .22), seed=seed + 10)
    m["mane"] = Mat("mane", mn, "fur", dark=sh(mn, .30), light=hilite(mn, .22), seed=seed + 11)
    m["mane2"] = Mat("mane2", mixc(hx(mn), hx(bk), .4), "fur", dark=sh(mixc(hx(mn), hx(bk), .4), .28), light=hilite(mixc(hx(mn), hx(bk), .4), .14), seed=seed + 12)
    m["membrane"] = Mat("membrane", ex, "skin", dark=sh(ex, .34), light=hilite(ex, .22), glow=gl, glow_amt=.12, seed=seed + 13)
    m["feather"] = Mat("feather", ex, "cloth", dark=sh(ex, .38), light=hilite(ex, .3), seed=seed + 14)
    m["feather2"] = Mat("feather2", gl, "energy", dark=sh(gl, .5), light=mixc(hx(gl), [255, 255, 255], .6), glow=gl, glow_amt=.7, seed=seed + 15)
    m["flame"] = Mat("flame", gl, "energy", dark=hx(tr), light=[255, 240, 190], glow=gl, glow_amt=1.0, seed=seed + 16)
    m["leaf"] = Mat("leaf", ex, "cloth", dark=sh(ex, .34), light=hilite(ex, .25), seed=seed + 17)
    m["skin"] = Mat("skin", "D8A67C", "skin", dark="7A4A2E", light="F0CCA8", seed=seed + 18)
    return m


# 3.76.0 look pass: what makes each will its own beast. shoulder: slab | spike | fur | round | scale | fin | feather | bone | sleek ; collar: plate | ruff | scale | none ;
# emblem: gem | sun | fang | chevron | eye | leaf ; bulk = chest width factor ; horn / mane / wing / spine = size of the signature feature (1 = as before).
STYLE = {
 "dragon_will": dict(shoulder="spike", collar="scale", emblem="chevron", horn=1.45, wing=1.18, spine=3.0, mane=.9),
 "elderwood_guardian_will": dict(shoulder="round", collar="plate", emblem="leaf", bulk=1.06),
 "shadow_panther_will": dict(shoulder="sleek", collar="none", emblem="eye"),
 "lion_will": dict(shoulder="fur", collar="ruff", emblem="sun", mane=1.5, bulk=1.05),
 "ridge_wolf_will": dict(shoulder="fur", collar="ruff", emblem="fang", mane=1.2, spine=2.8),
 "thunder_fang_chimera_will": dict(shoulder="fur", collar="plate", emblem="fang", mane=1.25),
 "crested_boar_will": dict(shoulder="round", collar="plate", emblem="gem", bulk=1.1, spine=2.8),
 "wind_skimmer_will": dict(shoulder="feather", collar="none", emblem="chevron", wing=1.15),
 "flame_serpent_will": dict(shoulder="scale", collar="scale", emblem="chevron", spine=2.8),
 "ammit_will": dict(shoulder="scale", collar="plate", emblem="eye", bulk=1.08),
 "apophis_will": dict(shoulder="sleek", collar="scale", emblem="eye", horn=1.25, wing=1.1),
 "cerberus_will": dict(shoulder="spike", collar="ruff", emblem="fang", mane=1.1, spine=2.8),
 "fenrir_will": dict(shoulder="fur", collar="ruff", emblem="fang", mane=1.35),
 "cu_sith_will": dict(shoulder="fur", collar="ruff", emblem="leaf", mane=1.15),
 "kitsune_will": dict(shoulder="sleek", collar="ruff", emblem="gem"),
 "nue_will": dict(shoulder="fur", collar="plate", emblem="eye", bulk=1.06),
 "pegasus_will": dict(shoulder="slab", collar="plate", emblem="sun", wing=1.2, mane=1.15),
 "minotaur_will": dict(shoulder="round", collar="plate", emblem="gem", bulk=1.12, horn=1.4),
 "sphinx_will": dict(shoulder="slab", collar="plate", emblem="sun", wing=1.1),
 "hafgufa_will": dict(shoulder="fin", collar="plate", emblem="gem", bulk=1.1),
 "hydra_will": dict(shoulder="scale", collar="scale", emblem="chevron"),
 "jormungandr_will": dict(shoulder="fin", collar="scale", emblem="chevron", spine=2.8),
 "kraken_will": dict(shoulder="fin", collar="none", emblem="eye"),
 "nidhoggr_will": dict(shoulder="bone", collar="none", emblem="eye", wing=1.1, spine=2.8),
 "scylla_charybdis_will": dict(shoulder="fin", collar="plate", emblem="eye"),
 "typhon_will": dict(shoulder="spike", collar="plate", emblem="eye"),
 "yamata_no_orochi_will": dict(shoulder="scale", collar="scale", emblem="chevron"),
}


LEGS = {"digi": ("shadow_panther_will", "ridge_wolf_will", "wind_skimmer_will", "flame_serpent_will", "cerberus_will", "fenrir_will", "cu_sith_will", "kitsune_will", "pegasus_will", "sphinx_will",
                 "dragon_will", "apophis_will", "nidhoggr_will", "typhon_will", "nue_will", "jormungandr_will", "hydra_will", "yamata_no_orochi_will")}


def weapon(c, kind):
    HY = 1.7                                                          # the hand sits 1.7 higher since 3.62.0 (shorter arm), the weapons follow it
    wsg = lambda c_, part, p0, p1, *a, **k: sg(c_, part, (p0[0], p0[1] + HY, p0[2]), (p1[0], p1[1] + HY, p1[2]), *a, **k)
    wcu = lambda c_, part, ctr, *a, **k: cu(c_, part, (ctr[0], ctr[1] + HY, ctr[2]), *a, **k)
    if kind == "fang":
        wsg(c, "armR", (-6, 8.5, -2.4), (-6.4, 1.2, -8.0), 1.8, 1.4, "membrane" if False else "plate", pad=.3)
        wsg(c, "armR", (-6.0, 8.0, -3.0), (-6.2, 3.0, -9.6), 1.2, 1.2, "glow", pad=.2)
    elif kind == "sword":
        wsg(c, "armR", (-6.2, 8.5, -2.6), (-6.4, 6.0, -12.4), 1.2, 3.0, "plate", pad=.2)
        wsg(c, "armR", (-6.2, 8.5, -2.6), (-6.4, 6.0, -12.4), 0.6, 3.4, "glow", pad=.1)
        wcu(c, "armR", (-6.2, 8.4, -3.0), (4.8, 1.0, 1.0), "gold")
    elif kind == "twin":
        for s in (-1, 1): wsg(c, "armR" if s < 0 else "armL", (s * 6.0, 8.5, -2.6), (s * 6.4, 5.0, -9.0), 1.4, 1.6, "plate", pad=.3)
    elif kind in ("lance", "staff", "axe"):
        wsg(c, "armR", (-6.2, 9.0, 0.4), (-6.2, 9.0, -14.0), 1.0, 1.0, "gold" if kind == "lance" else "leather", pad=.2)
        if kind == "lance": wsg(c, "armR", (-6.2, 9.0, -14.0), (-6.2, 9.0, -18.6), 1.8, 1.0, "plate", pad=.2)
        if kind == "axe": wcu(c, "armR", (-6.2, 9.0, -13.0), (1.0, 5.6, 4.4), "plate"); wcu(c, "armR", (-6.2, 9.0, -14.6), (.8, 4.0, 1.6), "gold")
        if kind == "staff": wcu(c, "armR", (-6.2, 9.0, -14.6), (2.4, 2.4, 2.4), "glow", rot=(45, 0, 45))
    elif kind == "fan":
        for k in range(5): wsg(c, "armR", (-6.2, 9.0, -3.0), (-6.2 + (k - 2) * 1.9, 9.0 + 4 - abs(k - 2) * .6, -9.0), 1.6, .6, "cloth" if k % 2 else "belly", pad=.2)


def build(wid):
    sp = SPEC[wid]
    m = roles(wid)
    sh = Sheet(256, 256); sc = Scene(sh)
    c = Ctx(sc, m, sp, seed=sum(map(ord, wid)))
    kind = sp["head"]
    digi = wid in LEGS["digi"]
    # ---- body
    sc.def_bone("breath", (0, 18.0, 0), None, [("scale", 1, .022, 1, 0.0), ("scale", 0, .014, 1, 0.1), ("scale", 2, .014, 1, 0.1)])
    st = STYLE.get(wid, {})
    with sc.in_bone("breath"):
        torso(c, bulk=st.get("bulk", 1.0), chest="plate" if sp.get("plates") else "belly")
        gorget(c, kind=st.get("collar", "plate"))
        if sp.get("plates"): scale_plates(c)
        emblem(c, kind=st.get("emblem", "gem"))
    belt_and_cloth(c, front_cloth=wid not in ("hafgufa_will", "kraken_will", "scylla_charybdis_will"))
    if sp.get("spikes"): spine_ridge(c, size=st.get("spine", 2.2))
    elif wid != "hafgufa_will":                                               # 3.77.0: every back has a ridge that breaks the box outline: fur, a dorsal fin, leaves or scale plates
        spine_ridge(c, n=4, size=1.9, mat="mane" if sp.get("mane") else ("membrane" if st.get("shoulder") == "fin" else ("leaf" if kind == "plant" else "plate")))
    # ---- arms
    arm(c, 1, "armL", pauldron_kind=st.get("shoulder", "slab"), spiked=bool(sp.get("spikes")), fin=bool(sp.get("fins")))
    arm(c, -1, "armR", pauldron_kind=st.get("shoulder", "slab"), spiked=bool(sp.get("spikes")), fin=bool(sp.get("fins")))
    # ---- legs
    for s_, pn in ((1, "legL"), (-1, "legR")):
        if digi: leg_digi(c, s_, pn, hoof=wid in ("pegasus_will", "minotaur_will"))
        else: leg_straight(c, s_, pn)
    # ---- head
    head(c, kind, V(0, 28, 0), 1.0, "head")
    ears(c, sp["ears"], V(0, 28, 0)) if sp["ears"] != "none" else None
    if sp["horns"] != "none": horns(c, sp["horns"], V(0, 28, 0), scale=st.get("horn", 1.0))
    if sp.get("mane"):
        mk = st.get("mane", 1.0)                                              # 3.76.0: the mane is the lion's and the great wolves' signature, so they get a bigger one
        mane_ring(c, "head", "mane", "mane2", 31.2, 5.0 + (mk - 1) * 1.2, 3.6 * mk, 1.6 * mk, 15, 52, 308, w=3.0 * mk ** .5, t=2.0, seed=1, bone="mane1")
        mane_ring(c, "head", "mane2", "mane", 28.6, 5.4 + (mk - 1) * 1.6, 4.2 * mk, 2.1 * mk, 17, 40, 320, w=3.2 * mk ** .5, t=2.2, seed=2, bone="mane2")
    # ---- extra heads on necks, waist heads, tentacles, island, chains
    n = sp.get("sideheads", 0)
    sub = {"dragon": "dragon", "wolf": "wolf"}.get(kind, "snake") if wid != "typhon_will" else "dragon"
    if n:
        angs = {2: (-52, 52), 4: (-42, 42, -78, 78), 7: (-26, 26, -52, 52, -78, 78, 0)}.get(n, (-50, 50, -80, 80, -25, 25))
        for i, a in enumerate(angs[:n]):
            ar = math.radians(a)
            sx = math.sin(ar)
            tip = V(sx * (7.5 if n > 2 else 7.5), 27.0 + 5.2 * math.cos(ar) * (1 if n > 2 else .4) + (i // 2) * 2.2 + (3 if a == 0 else 0), (-1.0 if n <= 2 else 1.0) - abs(sx) * 2.0 + (3.0 if a == 0 else 0))
            base = V(sx * 2.4, 23.5, 0.6)
            pts = curve(base, tip, (sx * 1.5, 1.5, 0), 5)
            nb = "neck%d" % i
            sc.def_bone(nb, base, None, [("rot", 2, 4.0 * (1 if sx >= 0 else -1), 2, i * .17), ("rot", 0, 5.0, 2, i * .17 + .2)])
            with sc.in_bone(nb):
                cn(c, "body", pts, taper(3.0, 2.2, 5), taper(3.0, 2.2, 5), "body", mat2="belly")
                head(c, sub, tip + V(0, 1.0, -1.2), .62, "body", ear="none", lite=n > 2)          # 3.77.0: the two side heads of Cerberus are whole heads; four or seven are the light version
    if sp.get("waistheads"):
        for i in range(sp["waistheads"]):
            a = math.radians(-100 + i * 200 / (sp["waistheads"] - 1))
            base = V(math.sin(a) * 4.0, 12.5, -math.cos(a) * 2.2 + 0.0)
            tip = V(math.sin(a) * 11.0, 10.5 - (i % 2) * 2.5, -math.cos(a) * 8.0 - 1.0)
            pts = curve(base, tip, (0, -1.0, 0), 4)
            wb = "waist%d" % i
            sc.def_bone(wb, base, None, [("rot", 0, 6.0, 2, i * .21), ("rot", 2, 5.0 * (1 if math.sin(a) >= 0 else -1), 2, i * .21 + .3)])
            with sc.in_bone(wb):
                cn(c, "body", pts, taper(3.0, 2.2, 4), taper(3.0, 2.2, 4), "body", mat2="back")
                head(c, "wolf", tip + V(0, .2, -.8), .55, "body", ear="none", lite=True)
    if sp.get("tentacles"):
        for i in range(sp["tentacles"]):
            a = -78 + i * 156 / (sp["tentacles"] - 1)
            ar = math.radians(a)
            base = V(math.sin(ar) * 3.2, 20.0 - (i % 2) * 3.0, 2.8)
            mid = V(math.sin(ar) * 10.0, 14.0 - (i % 2) * 4.0, 7.0 + (i % 2) * 2.0)
            tip = V(math.sin(ar) * 14.0, 3.0 + (i % 2) * 2.0, 4.0 + (i % 3) * 3.0)
            pts = curve(base, mid, (0, 0, 2), 5) + curve(mid, tip, (math.sin(ar) * 2.0, 0, 3), 5)[1:]
            tb1, tb2 = "tent%d" % i, "tentB%d" % i
            sc.def_bone(tb1, pts[0], None, [("rot", 2, 7.0 * (1 if ar >= 0 else -1), 2, i * .13), ("rot", 0, 6.0, 2, i * .13 + .2)])
            sc.def_bone(tb2, pts[len(pts) // 2], tb1, [("rot", 2, 9.0 * (1 if ar >= 0 else -1), 2, i * .13 + .15), ("rot", 0, 8.0, 2, i * .13 + .35)])
            TBN = [tb1] * (len(pts) // 2) + [tb2] * (len(pts) - 1 - len(pts) // 2)
            cn(c, "back", pts, taper(3.4, .9, len(pts)), taper(3.4, .9, len(pts)), "body", mat2="belly", bone=TBN)
            for q in range(1, len(pts) - 1, 1): cu(c, "back", pts[q] + V(0, -1.2, -.8), (.9, .9, .9), "gold", bone=TBN[q])
    if sp.get("island"):
        sc.def_bone("island", (0, 30.0, 4.4), None, [("pos", 1, .7, 1, 0.0), ("rot", 1, 4.0, 1, 0.0)])
        sc.cur = "island"
        for i, (x, y, z, w_, h_, d_) in enumerate(((0, 25.5, 4.4, 9, 3, 6), (-2, 28, 5.2, 6, 3, 4.6), (2.4, 28.4, 4.6, 4.6, 2.6, 4), (0, 31, 5.4, 3.4, 2.4, 3.2), (-3.4, 24.6, 3.6, 3.4, 2.4, 3.6))):
            cu(c, "back", (x, y, z), (w_, h_, d_), "plate", dt=(i % 3) - 1, rot=(0, i * 17, 0))
        for i, x in enumerate((-2, 1.6)): sg(c, "back", (x, 30.0, 5.2), (x, 36.0 - i * 2, 5.2), 1.4, 1.4, "dark"); cu(c, "back", (x, 37.0 - i * 2, 5.2), (5.0, 3.4, 5.0), "leaf", rot=(0, 20 * i, 0))
        for i in range(6): cu(c, "back", (-4 + i * 1.6, 23.0 + (i % 3) * .8, 3.9), (1.4, 1.4, 1.4), "claw")
        sc.cur = "root"
    if wid == "fenrir_will":
        for s_ in (-1, 1):
            sc.def_bone("chain" + ("L" if s_ > 0 else "R"), (s_ * 6.0, 11.0, -1.0), None, [("rot", 0, 9.0, 2, 0.0 if s_ > 0 else .5), ("rot", 2, 5.0, 2, .25)])
            sc.cur = "chain" + ("L" if s_ > 0 else "R")
            for k in range(5): cu(c, "armL" if s_ > 0 else "armR", (s_ * 6.0, 10.4 - k * 1.7, -1.0 + (k % 2) * .4), (1.8 if k % 2 == 0 else .6, 2.2, .6 if k % 2 == 0 else 1.8), "gold", rot=(0, 0, 0))
            sc.cur = "root"
    # ---- tail, wings, weapon
    if sp["tail"] != "none": tail(c, sp["tail"])
    w_ = sp["wings"]
    wk = st.get("wing", 1.0)
    if w_ in ("membrane", "tattered", "small"):
        for s_ in (-1, 1): wing_membrane(c, s_, span=(22 if w_ != "small" else 12) * wk, rise=(14 if w_ != "small" else 8) * wk, drop=15 if w_ != "small" else 8, tattered=(w_ == "tattered"), fingers=4 if w_ != "small" else 3)
    elif w_ == "feather":
        for s_ in (-1, 1): wing_feather(c, s_, span=20 * wk)
    weapon(c, sp.get("weapon", "claws"))
    return sc


def render_ids(ids, tag="forms", cols=3):
    from forms_preview import render, player_cards, sheet_of
    ims, labels = [], []
    pl = player_cards(head=False)
    for wid in ids:
        sc = build(wid); cards = sc.cards()
        a = render(pl + cards, sc.sh, yaw=22, pitch=8, W=330, H=450, scale=10.0)
        b = render(pl + cards, sc.sh, yaw=158, pitch=8, W=330, H=450, scale=10.0)
        ims += [a, b]; labels += [wid + " (%d cubes)" % len(sc.boxes), "back"]
    out = os.path.join(HERE, "prev_%s.png" % tag)
    sheet_of(ims, labels, out, cols=cols * 2 // 1 if False else 4, cw=330, ch=450)
    return out


if __name__ == "__main__":
    ids = sys.argv[1:] or list(PAL)
    if ids == ["all"]: ids = list(PAL)
    print(render_ids(ids, "gen"))
