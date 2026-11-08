#!/usr/bin/env python3
"""Procedural preview sheets of the 27 Beast Will designs (Acquire = human + streaks + one piece; Integrate = beast-humanoid in armour).
Reads beastwill_remake_v3.json (colours) and the SPEC table below (body features). Output: preview_sheet_{1,2,3}.png"""
import json, math, random, sys, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
S = 3                     # supersampling
FW, FH = 220, 320         # figure box

def hx(h): h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
def mix(a, b, t): return tuple(int(a[i] + (b[i]-a[i])*t) for i in range(3))
def shade(c, f): return tuple(max(0, min(255, int(x*f))) for x in c)

# head: (skull_r, snout_len, snout_w, kind) ; ears: pointed|round|fox|long|none ; horns: none|swept|bull|ram|antler|crest|branch
# tail: none|whip|thick|bushy|serpent|spiked|fox|flame|fluke|tuft ; wings: none|membrane|feather|small|tattered
SPEC = {
 "dragon_will":        dict(head="dragon", ears="none",    horns="swept",  tail="spiked",  wings="membrane", spikes=1, plates=1, mane=1, weapon="fang", fx="aura"),
 "elderwood_guardian_will": dict(head="plant", ears="none", horns="branch", tail="none",  wings="none",     spikes=0, plates=1, mane=0, weapon="twin", fx="vines"),
 "shadow_panther_will":dict(head="cat",    ears="pointed", horns="none",   tail="whip",    wings="none",     spikes=0, plates=0, mane=0, weapon="claws", fx="ragged"),
 "lion_will":          dict(head="lion",   ears="round",   horns="none",   tail="tuft",    wings="none",     spikes=0, plates=1, mane=1, weapon="claws", fx="aura"),
 "ridge_wolf_will":    dict(head="wolf",   ears="pointed", horns="none",   tail="bushy",   wings="none",     spikes=1, plates=0, mane=1, weapon="claws", fx="aura"),
 "thunder_fang_chimera_will": dict(head="lion", ears="round", horns="ram", tail="serpent", wings="small",    spikes=0, plates=1, mane=1, weapon="claws", fx="lightning"),
 "crested_boar_will":  dict(head="boar",   ears="round",   horns="crest",  tail="tuft",    wings="none",     spikes=1, plates=1, mane=0, weapon="tusk", fx="dust"),
 "wind_skimmer_will":  dict(head="bird",   ears="none",    horns="crest",  tail="feather", wings="feather",  spikes=0, plates=0, mane=0, weapon="claws", fx="wind"),
 "flame_serpent_will": dict(head="dragon", ears="none",    horns="crest",  tail="flame",   wings="none",     spikes=1, plates=1, mane=0, weapon="sword", fx="flame"),
 "ammit_will":         dict(head="croc",   ears="round",   horns="none",   tail="thick",   wings="none",     spikes=0, plates=1, mane=1, weapon="jaws", fx="aura"),
 "apophis_will":       dict(head="cobra",  ears="none",    horns="swept",  tail="serpent2",wings="tattered", spikes=0, plates=0, mane=0, weapon="claws", fx="ragged"),
 "cerberus_will":      dict(head="wolf",   ears="pointed", horns="none",   tail="serpent", wings="none",     spikes=1, plates=1, mane=1, weapon="claws", fx="flame", sideheads=2),
 "fenrir_will":        dict(head="wolf",   ears="pointed", horns="none",   tail="bushy",   wings="none",     spikes=0, plates=1, mane=1, weapon="claws", fx="chain"),
 "cu_sith_will":       dict(head="wolf",   ears="long",    horns="none",   tail="curl",    wings="none",     spikes=0, plates=0, mane=1, weapon="claws", fx="ragged"),
 "kitsune_will":       dict(head="fox",    ears="fox",     horns="none",   tail="fox",     wings="none",     spikes=0, plates=0, mane=0, weapon="fan",   fx="flame"),
 "nue_will":           dict(head="ape",    ears="round",   horns="none",   tail="serpent", wings="none",     spikes=0, plates=1, mane=0, weapon="claws", fx="lightning"),
 "pegasus_will":       dict(head="horse",  ears="pointed", horns="none",   tail="whip",    wings="feather",  spikes=0, plates=1, mane=1, weapon="lance", fx="aura"),
 "minotaur_will":      dict(head="bull",   ears="round",   horns="bull",   tail="tuft",    wings="none",     spikes=0, plates=1, mane=0, weapon="axe",   fx="dust"),
 "sphinx_will":        dict(head="human",  ears="none",    horns="none",   tail="tuft",    wings="feather",  spikes=0, plates=1, mane=1, weapon="staff", fx="aura", nemes=1),
 "hafgufa_will":       dict(head="whale",  ears="none",    horns="none",   tail="fluke",   wings="none",     spikes=0, plates=1, mane=0, weapon="none",  fx="water", island=1),
 "hydra_will":         dict(head="snake",  ears="none",    horns="none",   tail="serpent", wings="none",     spikes=0, plates=1, mane=0, weapon="claws", fx="venom", sideheads=4),
 "jormungandr_will":   dict(head="snake",  ears="none",    horns="crest",  tail="serpent", wings="none",     spikes=1, plates=1, mane=0, weapon="claws", fx="water", fins=1),
 "kraken_will":        dict(head="squid",  ears="none",    horns="none",   tail="none",    wings="none",     spikes=0, plates=1, mane=0, weapon="none",  fx="water", tentacles=6),
 "nidhoggr_will":      dict(head="dragon", ears="none",    horns="branch", tail="spiked",  wings="tattered", spikes=1, plates=0, mane=0, weapon="claws", fx="roots"),
 "scylla_charybdis_will": dict(head="human", ears="none",  horns="none",   tail="none",    wings="none",     spikes=0, plates=1, mane=1, weapon="none",  fx="whirl", waistheads=6),
 "typhon_will":        dict(head="dragon", ears="none",    horns="swept",  tail="serpent2",wings="membrane", spikes=1, plates=1, mane=0, weapon="sword", fx="lightning", sideheads=4),
 "yamata_no_orochi_will": dict(head="snake", ears="none",  horns="none",   tail="serpent", wings="none",     spikes=0, plates=1, mane=0, weapon="none",  fx="water", sideheads=7),
}
# where the single Acquire piece sits and what it looks like: arm|leg|hip|head|shoulder|neck
PIECE = {"dragon_will":"arm","elderwood_guardian_will":"arm","shadow_panther_will":"hip","lion_will":"neck","ridge_wolf_will":"arm",
 "thunder_fang_chimera_will":"arm","crested_boar_will":"head","wind_skimmer_will":"arm","flame_serpent_will":"arm","ammit_will":"arm",
 "cerberus_will":"arm","fenrir_will":"arm","cu_sith_will":"hip","kitsune_will":"hip","nue_will":"leg","pegasus_will":"leg","minotaur_will":"arm",
 "sphinx_will":"head","apophis_will":"arm","hafgufa_will":"shoulder","hydra_will":"arm","jormungandr_will":"arm","kraken_will":"arm",
 "nidhoggr_will":"arm","scylla_charybdis_will":"shoulder","typhon_will":"arm","yamata_no_orochi_will":"arm"}

class Fig:
    def __init__(self):
        self.im = Image.new("RGBA", (FW*S, FH*S), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)
    def P(self, *pts): return [(x*S, y*S) for x, y in pts]
    def poly(self, pts, fill, outline=None):
        self.d.polygon(self.P(*pts), fill=fill, outline=outline)
    def ell(self, cx, cy, rx, ry, fill, outline=None):
        self.d.ellipse([(cx-rx)*S, (cy-ry)*S, (cx+rx)*S, (cy+ry)*S], fill=fill, outline=outline, width=S)
    def limb(self, pts, w, fill):
        self.d.line(self.P(*pts), fill=fill, width=int(w*S), joint="curve")
        for x, y in pts: self.ell(x, y, w/2, w/2, fill)
    def layer_blur(self, r, alpha_mul=1.0):
        pass

def glow(fig, cx, cy, rx, ry, col, a=110, blur=18):
    g = Image.new("RGBA", fig.im.size, (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    gd.ellipse([(cx-rx)*S, (cy-ry)*S, (cx+rx)*S, (cy+ry)*S], fill=col+(a,))
    g = g.filter(ImageFilter.GaussianBlur(blur*S/3))
    fig.im = Image.alpha_composite(fig.im, g); fig.d = ImageDraw.Draw(fig.im)

def human(f, skin, outline, x0=0, alpha=255, arm_pose="down"):
    sk = skin + (alpha,)
    # legs
    for sx in (-1, 1):
        f.limb([(110+sx*10, 150), (110+sx*13, 200), (110+sx*10, 255)], 13, sk)
        f.ell(110+sx*10, 262, 9, 4, sk)
    f.poly([(88, 82), (132, 82), (126, 150), (94, 150)], sk)
    for sx in (-1, 1):
        f.limb([(110+sx*22, 86), (110+sx*30, 120), (110+sx*28, 152)], 11, sk)
    f.ell(110, 56, 15, 17, sk)
    f.limb([(110, 70), (110, 82)], 10, sk)

def eyes(f, hx_, hy, col, w=7, gap=8):
    for sx in (-1, 1):
        f.poly([(hx_+sx*gap-w*0.5, hy), (hx_+sx*gap+w*0.5, hy-2), (hx_+sx*gap+sx*w*0.6, hy+2)], col)

def streaks(f, col, n=7, side=-1, seed=0, a=190):
    rnd = random.Random(seed)
    lay = Image.new("RGBA", f.im.size, (0, 0, 0, 0)); ld = ImageDraw.Draw(lay)
    for i in range(n):
        y = rnd.randint(70, 250); l = rnd.randint(30, 80); x = 110 + side*rnd.randint(30, 70)
        ld.line([(x*S, y*S), ((x+side*l)*S, (y+rnd.randint(-3, 3))*S)], fill=col+(a,), width=int(rnd.choice((2, 3, 4))*S/1.6))
    lay = lay.filter(ImageFilter.GaussianBlur(S*0.6))
    f.im = Image.alpha_composite(f.im, lay); f.d = ImageDraw.Draw(f.im)

def piece(f, where, M, A, A2):
    col = M; dk = shade(M, .55)
    if where == "arm":   f.limb([(80, 118), (82, 150)], 15, dk); f.limb([(80, 120), (82, 148)], 11, col); f.ell(81, 150, 7, 7, A)
    elif where == "leg": f.limb([(100, 232), (100, 246)], 17, dk); f.limb([(100, 233), (100, 245)], 12, col)
    elif where == "hip": f.limb([(120, 146), (150, 168), (166, 150)], 8, dk); f.limb([(120, 146), (150, 166), (164, 152)], 5, col); f.ell(165, 150, 5, 5, A)
    elif where == "head":f.poly([(95, 44), (125, 44), (122, 52), (98, 52)], col); f.ell(110, 48, 4, 4, A)
    elif where == "shoulder": f.ell(82, 86, 15, 11, dk); f.ell(82, 84, 11, 8, col); f.poly([(76, 78), (82, 66), (88, 78)], A)
    elif where == "neck": f.ell(110, 76, 22, 8, dk); f.ell(110, 75, 19, 6, col)

def draw_acquire(w, spec, cols, skin):
    f = Fig(); M, A, A2 = cols
    glow(f, 110, 170, 60, 110, M, a=70, blur=24)
    for k, al in ((3, 40), (2, 80), (1, 130)):         # afterimages
        tmp = Fig(); human(tmp, mix(M, (255, 255, 255), .3), None, alpha=al)
        sh = Image.new("RGBA", f.im.size, (0, 0, 0, 0)); sh.paste(tmp.im, (-k*18*S, 0))
        f.im = Image.alpha_composite(f.im, sh); f.d = ImageDraw.Draw(f.im)
    streaks(f, M, 8, side=1, seed=hash(w) % 997)
    human(f, skin, None)
    piece(f, PIECE[w], M, A, A2)
    eyes(f, 110, 55, A, w=6, gap=6)
    return f.im

def head_draw(f, sp, M, A, A2, D, L, hx_=110, hy=56, scale=1.0):
    k = sp["head"]; r = 16*scale
    if k in ("lion",) or sp.get("mane"):
        mc = A2 if k == "lion" else L
        if k == "lion": f.ell(hx_, hy+4, 30*scale, 30*scale, shade(M, .75)); f.ell(hx_, hy+4, 24*scale, 24*scale, M)
    # ears
    e = sp["ears"]
    for sx in (-1, 1):
        if e == "pointed": f.poly([(hx_+sx*6*scale, hy-10*scale), (hx_+sx*12*scale, hy-34*scale), (hx_+sx*17*scale, hy-8*scale)], D)
        elif e == "round": f.ell(hx_+sx*14*scale, hy-12*scale, 6*scale, 6*scale, D)
        elif e == "fox":   f.poly([(hx_+sx*5*scale, hy-10*scale), (hx_+sx*13*scale, hy-42*scale), (hx_+sx*20*scale, hy-6*scale)], D); f.poly([(hx_+sx*8*scale, hy-12*scale), (hx_+sx*13*scale, hy-34*scale), (hx_+sx*17*scale, hy-9*scale)], A)
        elif e == "long":  f.poly([(hx_+sx*10*scale, hy-8*scale), (hx_+sx*24*scale, hy+18*scale), (hx_+sx*14*scale, hy+22*scale)], D)
    # horns
    h = sp["horns"]
    for sx in (-1, 1):
        if h == "swept":  f.limb([(hx_+sx*9*scale, hy-12*scale), (hx_+sx*20*scale, hy-26*scale), (hx_+sx*34*scale, hy-24*scale)], 5*scale, A2)
        elif h == "bull": f.limb([(hx_+sx*14*scale, hy-8*scale), (hx_+sx*30*scale, hy-14*scale), (hx_+sx*34*scale, hy-34*scale)], 6*scale, A2)
        elif h == "ram":  f.limb([(hx_+sx*14*scale, hy-8*scale), (hx_+sx*32*scale, hy-8*scale), (hx_+sx*30*scale, hy+8*scale)], 6*scale, A2)
        elif h == "branch": f.limb([(hx_+sx*8*scale, hy-14*scale), (hx_+sx*14*scale, hy-34*scale), (hx_+sx*8*scale, hy-48*scale)], 3*scale, A2); f.limb([(hx_+sx*13*scale, hy-30*scale), (hx_+sx*26*scale, hy-38*scale)], 3*scale, A2)
    if h == "crest": f.poly([(hx_-4*scale, hy-14*scale), (hx_, hy-40*scale), (hx_+4*scale, hy-14*scale)], A2); f.poly([(hx_+4*scale, hy-14*scale), (hx_+14*scale, hy-34*scale), (hx_+10*scale, hy-12*scale)], A2)
    # skull + snout
    face = D if k not in ("human", "fox", "squid", "whale") else mix(M, (255, 255, 255), .15)
    f.ell(hx_, hy, r*1.05, r, M if k in ("human",) else shade(M, .9))
    if k in ("wolf", "dragon", "horse", "fox"): f.ell(hx_, hy+14*scale, 8*scale, 11*scale, shade(M, .8)); f.ell(hx_, hy+22*scale, 3.5*scale, 2.5*scale, (20, 20, 25))
    if k == "croc":  f.poly([(hx_-9*scale, hy+4*scale), (hx_+9*scale, hy+4*scale), (hx_+7*scale, hy+34*scale), (hx_-7*scale, hy+34*scale)], shade(M, .85)); [f.poly([(hx_+sx*7*scale, hy+14*scale+i*7*scale), (hx_+sx*3*scale, hy+17*scale+i*7*scale), (hx_+sx*7*scale, hy+20*scale+i*7*scale)], (240, 240, 240)) for sx in (-1, 1) for i in range(3)]
    if k == "boar":  f.ell(hx_, hy+10*scale, 11*scale, 8*scale, shade(M, .7)); [f.limb([(hx_+sx*9*scale, hy+10*scale), (hx_+sx*16*scale, hy+14*scale), (hx_+sx*14*scale, hy+2*scale)], 3.5*scale, (240, 235, 215)) for sx in (-1, 1)]
    if k == "bird":  f.poly([(hx_-8*scale, hy+2*scale), (hx_+8*scale, hy+2*scale), (hx_, hy+24*scale)], A2)
    if k == "cat":   f.ell(hx_, hy+8*scale, 7*scale, 5*scale, shade(M, .7)); [f.line if False else None for _ in range(0)]
    if k == "bull":  f.ell(hx_, hy+10*scale, 12*scale, 9*scale, shade(M, .7)); f.ell(hx_, hy+14*scale, 4*scale, 2*scale, A2)
    if k == "ape":   f.ell(hx_, hy+4*scale, 11*scale, 10*scale, mix(M, (255, 255, 255), .25))
    if k == "cobra": f.poly([(hx_-30*scale, hy+14*scale), (hx_-8*scale, hy-14*scale), (hx_+8*scale, hy-14*scale), (hx_+30*scale, hy+14*scale), (hx_, hy+36*scale)], shade(M, .7)); f.ell(hx_, hy, r, r, M)
    if k == "snake": f.ell(hx_, hy+8*scale, 9*scale, 12*scale, shade(M, .8)); [f.poly([(hx_+sx*3*scale, hy+18*scale), (hx_+sx*2*scale, hy+26*scale), (hx_+sx*5*scale, hy+19*scale)], (245, 245, 245)) for sx in (-1, 1)]
    if k == "squid": f.ell(hx_, hy-16*scale, 14*scale, 30*scale, shade(M, .8)); f.ell(hx_, hy-14*scale, 7*scale, 22*scale, A2)
    if k == "whale": f.ell(hx_, hy+4*scale, 20*scale, 16*scale, shade(M, .85)); f.ell(hx_, hy+14*scale, 14*scale, 5*scale, mix(M, (255, 255, 255), .45))
    if k == "plant": [f.poly([(hx_+sx*6*scale+i*sx*4*scale, hy-14*scale), (hx_+sx*14*scale+i*sx*7*scale, hy-30*scale), (hx_+sx*10*scale+i*sx*7*scale, hy-12*scale)], A2) for sx in (-1, 1) for i in range(2)]
    if sp.get("nemes"):
        f.poly([(hx_-22*scale, hy-8*scale), (hx_+22*scale, hy-8*scale), (hx_+30*scale, hy+40*scale), (hx_-30*scale, hy+40*scale)], A2); f.ell(hx_, hy+2*scale, r, r*1.05, M)
    # eyes
    eyes(f, hx_, hy-1*scale, A if k not in ("human",) else A2, w=7*scale, gap=7*scale)

def draw_integrate(w, spec, cols, skin):
    sp = spec; f = Fig(); M, A, A2 = cols; D = shade(M, .45); L = mix(M, (255, 255, 255), .45)
    dark_body = shade(M, .55)
    cx = 110
    # --- wings (behind)
    wg = sp["wings"]
    for sx in (-1, 1):
        if wg == "membrane": f.poly([(cx+sx*18, 90), (cx+sx*100, 24), (cx+sx*84, 70), (cx+sx*104, 82), (cx+sx*76, 106), (cx+sx*86, 124), (cx+sx*28, 118)], shade(M, .6)); f.limb([(cx+sx*18, 90), (cx+sx*100, 24)], 3, A2)
        if wg == "feather":
            for i in range(6): f.poly([(cx+sx*18, 92+i*3), (cx+sx*(104-i*8), 36+i*20), (cx+sx*(90-i*8), 52+i*20)], mix(M, (255, 255, 255), .2+.1*i%0.5))
        if wg == "small": f.poly([(cx+sx*18, 92), (cx+sx*62, 52), (cx+sx*50, 100)], shade(M, .6))
        if wg == "tattered": f.poly([(cx+sx*18, 90), (cx+sx*96, 34), (cx+sx*80, 62), (cx+sx*100, 70), (cx+sx*70, 84), (cx+sx*84, 112), (cx+sx*28, 112)], shade(M, .35))
    if sp.get("fins"): [f.poly([(cx+sx*20, 96), (cx+sx*50, 70), (cx+sx*44, 118)], A) for sx in (-1, 1)]
    # --- tail (behind)
    t = sp["tail"]
    if t in ("whip", "thick", "spiked", "bushy", "tuft", "curl", "flame", "feather", "serpent", "serpent2", "fluke"):
        wd = {"whip": 5, "thick": 14, "spiked": 12, "bushy": 14, "tuft": 5, "curl": 7, "flame": 11, "feather": 9, "serpent": 8, "serpent2": 7, "fluke": 12}[t]
        pts = [(cx+6, 160), (cx+34, 196), (cx+64, 214), (cx+84, 190)] if t not in ("serpent2",) else [(cx+6, 160), (cx+40, 210), (cx+70, 250), (cx+40, 284)]
        f.limb(pts, wd, dark_body)
        if t == "spiked": [f.poly([(x-4, y-2), (x, y-14), (x+4, y-2)], A2) for x, y in pts[1:]]
        if t == "flame": glow(f, pts[-1][0], pts[-1][1], 16, 22, A2, a=190, blur=10)
        if t == "tuft": f.ell(*pts[-1], 9, 9, A2)
        if t == "fluke": f.poly([(pts[-1][0]-18, pts[-1][1]-8), (pts[-1][0], pts[-1][1]+6), (pts[-1][0]+18, pts[-1][1]-8)], M)
        if t == "serpent": f.ell(pts[-1][0], pts[-1][1], 7, 6, M); eyes(f, pts[-1][0], pts[-1][1]-1, A, w=3, gap=3)
        if t == "serpent2": f.limb([(cx-6, 160), (cx-40, 210), (cx-70, 250), (cx-40, 284)], 7, dark_body)
    if t == "fox":
        for i in range(5):
            a = -0.9 + i*0.45
            f.limb([(cx, 156), (cx+math.sin(a)*40, 156+math.cos(a)*10-22), (cx+math.sin(a)*72, 190+i*2-30)], 13, mix(M, (255, 255, 255), .1*i))
            f.ell(cx+math.sin(a)*72, 190+i*2-30, 7, 9, A2)
    # --- tentacles (behind)
    for i in range(sp.get("tentacles", 0)):
        sx = -1 if i % 2 == 0 else 1; k = i//2
        f.limb([(cx+sx*18, 120), (cx+sx*(56+k*10), 150+k*34), (cx+sx*(70+k*8), 218+k*20), (cx+sx*(46+k*14), 282)], 11-k*2, shade(M, .7))
        f.ell(cx+sx*(46+k*14), 282, 5, 5, A2)
    # --- extra necks/heads behind
    for i in range(sp.get("sideheads", 0)):
        sx = -1 if i % 2 == 0 else 1; k = i//2
        hxp = cx+sx*(34+k*18); hyp = 62-k*12 if sp.get("sideheads") > 2 else 70
        f.limb([(cx+sx*10, 90), (cx+sx*(24+k*10), 84-k*6), (hxp, hyp+12)], 9, shade(M, .6))
        sub = dict(sp); sub["horns"] = "none" if sp["head"] in ("snake",) else sp["horns"]; sub["ears"] = sp["ears"]; sub["mane"] = 0; sub["nemes"] = 0
        head_draw(f, sub, M, A, A2, D, L, hx_=hxp, hy=hyp, scale=.62)
    # --- legs (digitigrade) + feet claws
    if not sp.get("waistheads") and sp["head"] != "squid":
        for sx in (-1, 1):
            f.limb([(cx+sx*13, 150), (cx+sx*22, 190), (cx+sx*10, 232), (cx+sx*14, 262)], 15, dark_body)
            f.limb([(cx+sx*13, 152), (cx+sx*20, 188)], 12, M); f.ell(cx+sx*20, 176, 11, 17, M)
            f.poly([(cx+sx*14-8, 262), (cx+sx*14+8, 262), (cx+sx*14+sx*12, 272), (cx+sx*14-sx*2, 270)], dark_body)
            [f.poly([(cx+sx*14+sx*i*5-2, 268), (cx+sx*14+sx*i*5+2, 268), (cx+sx*14+sx*i*5+sx*3, 280)], (235, 232, 220)) for i in range(3)]
    else:
        f.poly([(cx-24, 150), (cx+24, 150), (cx+36, 276), (cx-36, 276)], shade(M, .55))
    # --- torso + plates
    f.poly([(cx-26, 84), (cx+26, 84), (cx+17, 152), (cx-17, 152)], dark_body)
    if sp.get("plates"):
        for i in range(5): f.poly([(cx-18+i, 92+i*12), (cx+18-i, 92+i*12), (cx+16-i, 100+i*12), (cx-16+i, 100+i*12)], mix(A2, M, .35))
    f.poly([(cx-34, 80), (cx-14, 76), (cx-20, 98), (cx-42, 96)], M); f.poly([(cx+34, 80), (cx+14, 76), (cx+20, 98), (cx+42, 96)], M)  # pauldrons
    if sp.get("island"): [f.poly([(cx-30+i*14, 84), (cx-23+i*14, 62-(i%2)*8), (cx-16+i*14, 84)], (60, 120, 70)) for i in range(5)]
    # --- arms + claws
    for sx in (-1, 1):
        f.limb([(cx+sx*32, 92), (cx+sx*44, 126), (cx+sx*38, 160)], 13, dark_body)
        f.limb([(cx+sx*32, 94), (cx+sx*42, 124)], 10, M)
        [f.poly([(cx+sx*38+i*4-2, 162), (cx+sx*38+i*4+2, 162), (cx+sx*38+i*5, 178)], (240, 236, 224)) for i in (-1, 0, 1)]
    # --- waist heads (scylla)
    for i in range(sp.get("waistheads", 0)):
        a = math.pi*0.1 + i*(math.pi*0.8/5); xx = cx-math.cos(a)*44; yy = 168+math.sin(a)*-18+ (i%2)*12
        f.limb([(cx+(xx-cx)*0.4, 150), (xx, yy+4)], 8, shade(M, .6))
        sub = dict(sp); sub.update(head="wolf", ears="pointed", horns="none", mane=0, nemes=0)
        head_draw(f, sub, A, A2, A, shade(A, .5), mix(A, (255, 255, 255), .4), hx_=xx, hy=yy, scale=.5)
    # --- head
    head_draw(f, sp, M, A, A2, D, L)
    # --- back spikes
    if sp.get("spikes"):
        for i in range(4): f.poly([(cx-5+i*0, 88+i*16), (cx-18, 84+i*16-12), (cx-10, 98+i*16)], A2); f.poly([(cx+5, 88+i*16), (cx+18, 84+i*16-12), (cx+10, 98+i*16)], A2)
    # --- weapon
    wp = sp["weapon"]
    if wp == "fang":   f.poly([(cx+42, 150), (cx+50, 90), (cx+54, 150)], L, outline=A2)
    elif wp == "sword":f.poly([(cx-46, 168), (cx-52, 96), (cx-42, 168)], L, outline=A2)
    elif wp == "twin": [f.poly([(cx+sx*46, 160), (cx+sx*56, 100), (cx+sx*50, 160)], A2) for sx in (-1, 1)]
    elif wp == "lance":f.limb([(cx+48, 200), (cx+52, 60)], 3, A2); f.poly([(cx+48, 60), (cx+56, 44), (cx+56, 60)], L)
    elif wp == "axe":  f.limb([(cx+46, 190), (cx+50, 90)], 3.5, A2); f.poly([(cx+50, 88), (cx+78, 76), (cx+76, 112)], L)
    elif wp == "staff":f.limb([(cx+48, 200), (cx+52, 70)], 3, A2); f.ell(cx+52, 64, 7, 7, L)
    elif wp == "fan":  [f.poly([(cx-44, 160), (cx-70+i*10, 120), (cx-62+i*10, 162)], mix(A2, M, .2*i/3)) for i in range(4)]
    elif wp == "tusk": [f.poly([(cx+sx*38, 168), (cx+sx*50, 190), (cx+sx*36, 188)], (240, 235, 215)) for sx in (-1, 1)]
    elif wp == "jaws": f.poly([(cx+36, 156), (cx+56, 140), (cx+50, 176)], A2)
    # --- fx
    fx = sp["fx"]
    rnd = random.Random(sum(map(ord, w)))
    if fx == "lightning":
        for _ in range(4):
            x, y = cx+rnd.randint(-50, 50), 40+rnd.randint(0, 40); pts = [(x, y)]
            for i in range(5): x += rnd.randint(-10, 10); y += 30; pts.append((x, y))
            f.d.line(f.P(*pts), fill=A+(255,), width=int(2*S))
    if fx == "vines":
        for sx in (-1, 1): f.limb([(cx+sx*24, 150), (cx+sx*50, 200), (cx+sx*34, 250), (cx+sx*54, 290)], 4, A2)
    if fx in ("water", "whirl"):
        for i in range(3): f.d.arc([(cx-70+i*10)*S, (230+i*12)*S, (cx+70-i*10)*S, (290+i*6)*S], 0, 180, fill=A2+(230,), width=int(3*S))
    if fx == "flame": glow(f, cx, 110, 36, 60, A2, a=70, blur=16)
    if fx == "chain": [f.ell(cx-52+i*10, 190+ (i%2)*3, 4, 3, (210, 220, 235), outline=(90, 100, 120)) for i in range(4)]
    if fx == "venom": [f.ell(cx+rnd.randint(-60, 60), rnd.randint(190, 280), 3, 4, A2) for _ in range(10)]
    if fx == "roots": [f.limb([(cx-50+i*18, 292), (cx-46+i*18, 270), (cx-52+i*18, 252)], 3, (80, 60, 30)) for i in range(6)]
    if fx in ("wind", "dust"): streaks(f, L, 7, side=1, seed=3)
    if fx == "ragged":
        for _ in range(16): x = cx+rnd.randint(-40, 40); y = rnd.randint(70, 250); f.poly([(x, y), (x+rnd.randint(8, 18), y-6), (x+3, y+rnd.randint(10, 20))], shade(M, .8)+(0,)[:0])
    return f.im

def rim(im, col, grow=5, alpha=215):
    a = im.split()[3]
    big = a.filter(ImageFilter.MaxFilter(grow*2+1))
    edge = Image.new("RGBA", im.size, col+(0,))
    mask = Image.eval(big, lambda v: 255 if v > 140 else 0)
    inner = Image.eval(a, lambda v: 255 if v > 140 else 0)
    ring = Image.composite(Image.new("L", im.size, 0), mask, inner)
    ring = ring.point(lambda v: alpha if v else 0)
    edge.putalpha(ring)
    return Image.alpha_composite(edge, im)

def lum(c): return 0.3*c[0]+0.59*c[1]+0.11*c[2]

def card(w, data, spec, W=500, H=420, font=None, fsmall=None):
    cols = [hx(c) for c in data["spirit"]["colors"]]
    ocols = list(cols)
    if lum(cols[0]) < 70: cols = [mix(cols[0], (255, 255, 255), .30), cols[1] if lum(cols[1]) > 60 else mix(cols[1], (255,255,255), .4), cols[2]]
    base = Image.new("RGBA", (W, H), (16, 18, 26, 255)); d = ImageDraw.Draw(base)
    g = Image.new("RGBA", (W, H), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    gd.rectangle([0, 0, W, H], fill=cols[0]+(26,)); base = Image.alpha_composite(base, g); d = ImageDraw.Draw(base)
    skin = (150, 156, 168)
    a = draw_acquire(w, spec, cols, skin).resize((FW*3//2*2//3*1, FH*3//2*2//3*1), Image.LANCZOS) if False else draw_acquire(w, spec, cols, skin)
    i = draw_integrate(w, spec, cols, skin)
    ga = Fig(); glow(ga, 110, 170, 85, 140, cols[0], a=95, blur=30)
    sc = 0.95
    i = rim(i, mix(cols[2], (255, 255, 255), .35), grow=3, alpha=200)
    i = Image.alpha_composite(ga.im, i)
    a = a.resize((int(FW*sc), int(FH*sc)), Image.LANCZOS); i = i.resize((int(FW*sc*1.12), int(FH*sc*1.12)), Image.LANCZOS)
    base.alpha_composite(a, (20, 88)); base.alpha_composite(i, (W-i.width-8, 62))
    d.text((14, 8), data["name"].upper().replace(" WILL", ""), fill=(255, 255, 255), font=font)
    d.text((14, 34), f"{data['beast'].split('(')[0].strip()[:44]}", fill=(190, 196, 210), font=fsmall)
    tag = f"{data['minRank']}{' LEGENDARY' if data.get('legendary') else ''}  |  {data['tag']}"
    d.text((W-12, 12), tag, fill=cols[0], font=fsmall, anchor="ra")
    d.text((22, 62), "ACQUIRE  (human + 1 piece)", fill=(205, 210, 225), font=fsmall)
    d.text((W-14, 62), "INTEGRATE  (full beast-humanoid)", fill=(205, 210, 225), font=fsmall, anchor="ra")
    ep = data["acquire"]["equip_piece"].split(";")[0].split("(")[0].strip()
    d.text((14, H-62), ep[:62], fill=(150, 156, 170), font=fsmall)
    d.text((14, H-44), f"Integrate drain {data['integrate']['drainPct']}%  strain {data['integrate']['strainPerSecond']}/s  |  Acquire strain {data['acquire']['strainPerSecond']}/s", fill=(150, 156, 170), font=fsmall)
    _ = (W-14, H-52); d.text((W+999, H-52), f"drain {data['integrate']['drainPct']}%  strain {data['integrate']['strainPerSecond']}/s", fill=(150, 156, 170), font=fsmall, anchor="ra")
    for k, c in enumerate(cols): d.rectangle([16+k*40, H-22, 52+k*40, H-12], fill=c)
    d.rectangle([0, 0, W-1, H-1], outline=cols[0], width=2)
    return base

def main():
    ws = json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))
    try: font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 21); fs = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    except Exception: font = fs = ImageFont.load_default()
    only = sys.argv[1:]
    W, H = 500, 420
    groups = [ws[0:9], ws[9:18], ws[18:27]]
    for gi, g in enumerate(groups, 1):
        sheet = Image.new("RGBA", (W*3, H*3), (8, 9, 14, 255))
        for n, data in enumerate(g):
            c = card(data["id"], data, SPEC[data["id"]], W, H, font, fs)
            sheet.alpha_composite(c, ((n % 3)*W, (n//3)*H))
        out = os.path.join(HERE, f"preview_sheet_{gi}.png"); sheet.convert("RGB").save(out); print(out)

if __name__ == "__main__": main()
