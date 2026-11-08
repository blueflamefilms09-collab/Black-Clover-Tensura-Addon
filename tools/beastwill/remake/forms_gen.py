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
 "dragon_will": ("E4DEFF", "8A78D8", "F4F0FF", "B9A4FF", "E3B341", "C9B8FF", "5A48A8", "F2F0FF", "9C86E8", "scale"),
 "elderwood_guardian_will": ("3B5A2A", "231A10", "4F7A3A", "5A3C22", "35E0D0", "35E0D0", "1F7A3A", "2FBF5A", "2FBF5A", "bark"),
 "shadow_panther_will": ("1E1E2C", "0C0C14", "2E2E46", "2A1E40", "6A2FB0", "D6FF4A", "2E1A52", "1E1E2C", "3A2A5A", "fur"),
 "lion_will": ("D6A02A", "7A5A18", "D8C48C", "C9962A", "F6D676", "FFE070", "A82418", "92301A", "6E1E12", "fur"),
 "ridge_wolf_will": ("7C8F98", "4A5A66", "D0DDE6", "5A6A78", "E3F0FF", "BFE4FF", "2E3A4A", "C8D8E4", "A8B8C4", "fur"),
 "thunder_fang_chimera_will": ("D8C03C", "8A7418", "F0E8A8", "3A4A8A", "BFE9FF", "BFE9FF", "2A3A7A", "E8E060", "BFE9FF", "fur"),
 "crested_boar_will": ("8E7541", "5A4426", "B49A68", "6E675C", "EAD9B0", "FFD890", "4A3526", "5A4426", "7A7468", "fur"),
 "wind_skimmer_will": ("D8F4F8", "2CA8C8", "FFFFFF", "3A7CC8", "FFFFFF", "2CFAFF", "3A7CC8", "9CF0FF", "2CFAFF", "fur"),
 "flame_serpent_will": ("2A1410", "120806", "4A1E16", "2A1410", "FFC13A", "FF5A18", "5A1A10", "FF5A18", "FF7A1A", "scale"),
 "ammit_will": ("C9B87A", "A89050", "E4D8A8", "C58B1E", "D6B84A", "FFD24A", "7A5A1E", "C58B1E", "D6CC8E", "scale"),
 "apophis_will": ("2E0C3E", "14041E", "5A2070", "3A1450", "E0204A", "E0204A", "3A1450", "3A1450", "8F2BD9", "scale"),
 "cerberus_will": ("2E1414", "140808", "5A2A20", "1E1E28", "FF7A2A", "FF5A18", "4A0C0C", "5A1010", "FF5A18", "fur"),
 "fenrir_will": ("C8E8F2", "7AA0B8", "F4FBFF", "8AA8BE", "E8F6FF", "87F1FF", "4A6B8A", "E8F8FF", "87F1FF", "fur"),
 "cu_sith_will": ("1E5A48", "0B2E22", "7CC8A8", "2E6A58", "C8FFE6", "7CFF8A", "0B2E22", "9CE8C0", "7CFF8A", "fur"),
 "kitsune_will": ("F0987A", "B8584A", "FFF0D8", "8A2A5A", "FFF0D8", "FF7FB0", "8A2A5A", "FFF0D8", "FF7FB0", "fur"),
 "nue_will": ("5A4A7A", "2A2040", "8C7AB0", "3A2A5A", "E8E04A", "E8E04A", "2A2040", "3A2A5A", "7A55B8", "fur"),
 "pegasus_will": ("F0F2F8", "C8D4E4", "FFFFFF", "D9B34A", "D9B34A", "8FD0FF", "5A8ACA", "BFE0FF", "FFFFFF", "fur"),
 "minotaur_will": ("6A5532", "3A2C18", "8A7448", "4A3E2C", "C9983E", "FF9A3A", "3A2A18", "2A1E10", "6A5532", "fur"),
 "sphinx_will": ("D8B870", "A08848", "F0DCA0", "3A6FE8", "E0C27A", "6A9CFF", "2B3361", "3A6FE8", "FFFFFF", "fur"),
 "hafgufa_will": ("1E8E98", "0E5058", "B7D8C0", "5A7A68", "E8E1B0", "00E8E0", "0E5058", "1E8E98", "1E8E98", "skin"),
 "hydra_will": ("7CA830", "3A5A18", "D8E48A", "3A6B2A", "F2E85A", "C8FF3A", "1F6B2A", "3A5A18", "7CA830", "scale"),
 "jormungandr_will": ("1F4FB0", "0B2A5A", "6AA8E8", "1A3A7A", "7CFFB2", "7CFFB2", "0B2A5A", "1F4FB0", "6AA8E8", "scale"),
 "kraken_will": ("D84A3E", "8A2A2A", "F0907A", "3A2A8A", "3FE0D0", "3FE0D0", "2B1B6E", "D84A3E", "F0907A", "skin"),
 "nidhoggr_will": ("5A6A2A", "3B2A14", "B0B070", "C9B26B", "C9B26B", "8CFF3A", "3B2A14", "3B2A14", "5A3A1A", "bone"),
 "scylla_charybdis_will": ("6A7A78", "3A4442", "9AA8A4", "8C8F99", "B23A3A", "3AE0D0", "3A4442", "5A6A68", "6A7A78", "stone"),
 "typhon_will": ("2A2030", "120C18", "4A3A52", "1E1A28", "FFC21F", "FF3A3A", "5A0A18", "D80023", "3D7BFF", "scale"),
 "yamata_no_orochi_will": ("B02058", "6A0C34", "F0B0C8", "5A3A1E", "F2C94C", "FF3A8A", "3A0D1A", "B02058", "E01070", "scale"),
}


def roles(wid):
    b, bk, be, pl, tr, gl, cl, mn, ex, pat = PAL[wid]
    seed = sum(map(ord, wid))
    sh = lambda h, f: hx(h) * f
    m = {}
    m["body"] = Mat("body", b, pat, dark=sh(b, .38), light=mixc(hx(b), [255, 255, 255], .4), seed=seed + 1)
    m["back"] = Mat("back", bk, pat, dark=sh(bk, .4), light=mixc(hx(bk), [255, 255, 255], .3), seed=seed + 2)
    m["belly"] = Mat("belly", be, pat, dark=sh(be, .5), light=mixc(hx(be), [255, 255, 255], .45), seed=seed + 3)
    m["plate"] = Mat("plate", pl, "plate", dark=sh(pl, .38), light=mixc(hx(pl), [255, 255, 255], .5), glow=gl, glow_amt=.25, seed=seed + 4)
    m["gold"] = Mat("gold", tr, "plate", dark=sh(tr, .35), light=mixc(hx(tr), [255, 255, 255], .5), glow=gl, glow_amt=.3, seed=seed + 5)
    m["glow"] = Mat("glow", mixc(hx(gl), [255, 255, 255], .35), "energy", dark=hx(gl), light=[255, 255, 255], glow=gl, glow_amt=1.0, seed=seed + 6)
    m["eye"] = Mat("eye", mixc(hx(gl), [255, 255, 255], .6), "skin", dark=hx(gl) * .85, light=[255, 255, 255], glow=gl, glow_amt=1.0, seed=seed + 19)
    m["dark"] = Mat("dark", sh(bk, .55), "skin", dark=sh(bk, .2), light=sh(bk, 1.0), seed=seed + 7)
    m["claw"] = Mat("claw", "ECE4D0", "bone", dark="9A927C", light="FFFFFF", seed=seed + 8)
    m["leather"] = Mat("leather", mixc(sh(cl, 1.0), [60, 40, 28], .5), "cloth", dark=sh(cl, .2), light=mixc(hx(cl), [200, 160, 120], .3), seed=seed + 9)
    m["cloth"] = Mat("cloth", cl, "cloth", dark=sh(cl, .35), light=mixc(hx(cl), [255, 255, 255], .35), seed=seed + 10)
    m["mane"] = Mat("mane", mn, "fur", dark=sh(mn, .35), light=mixc(hx(mn), [255, 255, 255], .35), seed=seed + 11)
    m["mane2"] = Mat("mane2", mixc(hx(mn), hx(bk), .35), "fur", dark=sh(mn, .2), light=mixc(hx(mn), [255, 255, 255], .2), seed=seed + 12)
    m["membrane"] = Mat("membrane", ex, "skin", dark=sh(ex, .4), light=mixc(hx(ex), [255, 255, 255], .35), glow=gl, glow_amt=.2, seed=seed + 13)
    m["feather"] = Mat("feather", ex, "cloth", dark=sh(ex, .45), light=mixc(hx(ex), [255, 255, 255], .5), seed=seed + 14)
    m["feather2"] = Mat("feather2", gl, "energy", dark=hx(gl) * .6, light=[255, 255, 255], glow=gl, glow_amt=.7, seed=seed + 15)
    m["flame"] = Mat("flame", gl, "energy", dark=hx(tr), light=[255, 245, 200], glow=gl, glow_amt=1.0, seed=seed + 16)
    m["leaf"] = Mat("leaf", ex, "cloth", dark=sh(ex, .4), light=mixc(hx(ex), [255, 255, 255], .4), seed=seed + 17)
    m["skin"] = Mat("skin", "E2B48C", "skin", dark="8A5A3A", light="F6D6B6", seed=seed + 18)
    return m


LEGS = {"digi": ("shadow_panther_will", "ridge_wolf_will", "wind_skimmer_will", "flame_serpent_will", "cerberus_will", "fenrir_will", "cu_sith_will", "kitsune_will", "pegasus_will", "sphinx_will",
                 "dragon_will", "apophis_will", "nidhoggr_will", "typhon_will", "nue_will", "jormungandr_will", "hydra_will", "yamata_no_orochi_will")}


def weapon(c, kind):
    if kind == "fang":
        sg(c, "armR", (-6, 8.5, -2.4), (-6.4, 1.2, -8.0), 1.8, 1.4, "membrane" if False else "plate", pad=.3)
        sg(c, "armR", (-6.0, 8.0, -3.0), (-6.2, 3.0, -9.6), 1.2, 1.2, "glow", pad=.2)
    elif kind == "sword":
        sg(c, "armR", (-6.2, 8.5, -2.6), (-6.4, 6.0, -12.4), 1.2, 3.0, "plate", pad=.2)
        sg(c, "armR", (-6.2, 8.5, -2.6), (-6.4, 6.0, -12.4), 0.6, 3.4, "glow", pad=.1)
        cu(c, "armR", (-6.2, 8.4, -3.0), (4.8, 1.0, 1.0), "gold")
    elif kind == "twin":
        for s in (-1, 1): sg(c, "armR" if s < 0 else "armL", (s * 6.0, 8.5, -2.6), (s * 6.4, 5.0, -9.0), 1.4, 1.6, "plate", pad=.3)
    elif kind in ("lance", "staff", "axe"):
        sg(c, "armR", (-6.2, 9.0, 0.4), (-6.2, 9.0, -14.0), 1.0, 1.0, "gold" if kind == "lance" else "leather", pad=.2)
        if kind == "lance": sg(c, "armR", (-6.2, 9.0, -14.0), (-6.2, 9.0, -18.6), 1.8, 1.0, "plate", pad=.2)
        if kind == "axe": cu(c, "armR", (-6.2, 9.0, -13.0), (1.0, 5.6, 4.4), "plate"); cu(c, "armR", (-6.2, 9.0, -14.6), (.8, 4.0, 1.6), "gold")
        if kind == "staff": cu(c, "armR", (-6.2, 9.0, -14.6), (2.4, 2.4, 2.4), "glow", rot=(45, 0, 45))
    elif kind == "fan":
        for k in range(5): sg(c, "armR", (-6.2, 9.0, -3.0), (-6.2 + (k - 2) * 1.9, 9.0 + 4 - abs(k - 2) * .6, -9.0), 1.6, .6, "cloth" if k % 2 else "belly", pad=.2)


def build(wid):
    sp = SPEC[wid]
    m = roles(wid)
    sh = Sheet(256, 256); sc = Scene(sh)
    c = Ctx(sc, m, sp, seed=sum(map(ord, wid)))
    kind = sp["head"]
    digi = wid in LEGS["digi"]
    # ---- body
    sc.def_bone("breath", (0, 18.0, 0), None, [("scale", 1, .022, 1, 0.0), ("scale", 0, .014, 1, 0.1), ("scale", 2, .014, 1, 0.1)])
    with sc.in_bone("breath"):
        torso(c, bulk=1.0)
        gorget(c)
        if sp.get("plates"): scale_plates(c)
        emblem(c, kind="sun" if wid in ("lion_will", "sphinx_will", "dragon_will") else "gem")
    belt_and_cloth(c, front_cloth=wid not in ("hafgufa_will", "kraken_will", "scylla_charybdis_will"))
    if sp.get("spikes"): spine_ridge(c)
    # ---- arms
    arm(c, 1, "armL", spiked=bool(sp.get("spikes")), fin=bool(sp.get("fins")))
    arm(c, -1, "armR", spiked=bool(sp.get("spikes")), fin=bool(sp.get("fins")))
    # ---- legs
    for s_, pn in ((1, "legL"), (-1, "legR")):
        if digi: leg_digi(c, s_, pn, hoof=wid in ("pegasus_will", "minotaur_will"))
        else: leg_straight(c, s_, pn)
    # ---- head
    head(c, kind, V(0, 28, 0), 1.0, "head")
    ears(c, sp["ears"], V(0, 28, 0)) if sp["ears"] != "none" else None
    if sp["horns"] != "none": horns(c, sp["horns"], V(0, 28, 0))
    if sp.get("mane"):
        mane_ring(c, "head", "mane", "mane2", 31.2, 5.0, 3.6, 1.6, 15, 52, 308, w=3.0, t=2.0, seed=1, bone="mane1")
        mane_ring(c, "head", "mane2", "mane", 28.6, 5.4, 4.2, 2.1, 17, 40, 320, w=3.2, t=2.2, seed=2, bone="mane2")
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
                head(c, sub, tip + V(0, 1.0, -1.2), .62, "body", ear="none")
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
                head(c, "wolf", tip + V(0, .2, -.8), .55, "body", ear="none")
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
            sc.def_bone("chain" + ("L" if s_ > 0 else "R"), (s_ * 6.0, 10.0, -1.0), None, [("rot", 0, 9.0, 2, 0.0 if s_ > 0 else .5), ("rot", 2, 5.0, 2, .25)])
            sc.cur = "chain" + ("L" if s_ > 0 else "R")
            for k in range(5): cu(c, "armL" if s_ > 0 else "armR", (s_ * 6.0, 8.2 - k * 2.0, -1.0 + (k % 2) * .4), (1.8 if k % 2 == 0 else .6, 2.2, .6 if k % 2 == 0 else 1.8), "gold", rot=(0, 0, 0))
            sc.cur = "root"
    # ---- tail, wings, weapon
    if sp["tail"] != "none": tail(c, sp["tail"])
    w_ = sp["wings"]
    if w_ in ("membrane", "tattered", "small"):
        for s_ in (-1, 1): wing_membrane(c, s_, span=22 if w_ != "small" else 12, rise=14 if w_ != "small" else 8, drop=15 if w_ != "small" else 8, tattered=(w_ == "tattered"), fingers=4 if w_ != "small" else 3)
    elif w_ == "feather":
        for s_ in (-1, 1): wing_feather(c, s_)
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
