"""The two remade bosses, Son Goku and Sinbad (docs/boss_redesign_goku_sinbad.md), as player-model looks in the style of the Black Clover bosses (docs/bc_playermodels.md) and the devils
(docs/devil_looks.md): painted anime face, layered cube hair, gear parts, glow. SPECS is read by gen_gs.py.

Goku (8 looks, the owner's list): base, ssj (also used for SSJ2 with taller hair, see GOKU_STEP), ssj2, ssj3, god, blue, ui_sign, mui. The steps of GokuLadder map onto them in GsLooks.java.
Sinbad (8 looks): the king without a djinn and one look per djinn equip (baal, valefor, zepar, focalor, vepar, crocell, furfur). The ARMOUR of an equip is NOT in these looks: the client draws the
player's own Djinn Equip pieces (DjinnArmorLayer, the same meshes and the same forge-in animation) over the body; these looks are the BODY that changes under it (hair length and colour, eyes, robe
colour, glowing markings), like the Magi Djinn Equip does.

Sources (rule 10): English Dragon Ball wiki (dragonball.fandom.com) pages Super Saiyan, Super Saiyan 3, Super Saiyan God, Super Saiyan Blue and Ultra Instinct were fetched through the MediaWiki API in this
session (hair and eye colours of the forms; SSJ3's lost eyebrows and floor-long hair; God's crimson hair; Blue's cyan hair and eyes); the Japanese wiki was not needed. Sinbad: the lore file
docs/sinbad_boss_lore.md (written from the English Magi wiki by an earlier session): tan skin, golden eyes, very long PURPLE hair in a ponytail with one upright ahoge, white turban with feathers,
purple and white robes, pointed red shoes, hoop earrings, jewellery that is his metal vessels. The djinn body colours are the mod's own Djinn table (relics/djinn/Djinn.java), so the body matches
the armour pieces a player gets. The gi colours of Goku's Ultra Instinct forms (dark indigo with silver) are the earlier skin's choice (arc_goku_ui) and are kept.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bclib as L  # noqa: E402,F401
import bcparts as P  # noqa: E402
import bcparts3 as R3  # noqa: E402
import bcparts4 as Q4  # noqa: E402
import bcskin as S  # noqa: E402
import dvlib as D  # noqa: E402
import dvskin as DS  # noqa: E402
import dvtable as T  # noqa: E402
from dvchars import eyes, weg  # noqa: E402

import hd_detail as H  # noqa: E402

HEAD = "head"
TAN = ("#bf8560", "#dca47c", "#f3c8a2")
SUN = ("#b87a52", "#d79a72", "#eebb94")

# ================================================================================================ Goku
ORANGE, ORANGE_D, BLUE_GI, BLUE_L = "#ee7c1c", "#d8661a", "#2456c8", "#4c82ec"
NAVY, SILVER = "#1b2250", "#cfd6ff"


def _symbol(f, cx, cy, r, ink="#1a1a22", disc="#f4f2ea"):
    """The turtle school's mark: a pale disc with a plain glyph (three strokes and a cross bar)."""
    f.ellipse(cx, cy, r, r, disc, a=0.95)
    for dx in (-0.35, 0.0, 0.35):
        f.polyline([(cx + dx * r * 1.1, cy - r * 0.62), (cx + dx * r * 0.8, cy + r * 0.62)], ink, 0.07, a=0.9)
    f.polyline([(cx - r * 0.7, cy - r * 0.1), (cx + r * 0.7, cy - r * 0.1)], ink, 0.07, a=0.9)


def goku_marks(under, band, sym_on=True):
    """The blue undershirt sleeves to the wrist, the wristbands, the school symbol on the chest and the back. (kept: the first version of this look, not used by the 2nd pass below)"""
    def fn(cv):
        for part in ("arm_r", "arm_l"):
            for nm in ("front", "back", "west", "east", "top"):
                f = cv.face(part, nm, False)
                DS._cloth(f, f.m_rect(-0.2, -0.2, f.w + 0.2, 12.2, soft=1.0), under, folds=(1.3, 2.7) if nm in ("front", "back") else ())
                if nm != "top":
                    H.trim_band(f, 8.6, 11.9, band, spec=0.25)
        if sym_on:
            _symbol(cv.face("body", "front", False), 5.7, 4.6, 0.95)
            _symbol(cv.face("body", "back", False), 4.0, 5.2, 1.7)
    return fn


YELLOW, RED_B = "#f2c41c", "#d42a1c"


def goku_marks2(top, under, band, boot=(YELLOW, RED_B, BLUE_GI), sym_on=True):
    """The look of the Blockbench tutorial skin the owner sent (docs/boss_redesign_goku_sinbad.md): short orange gi sleeves over a short blue undershirt sleeve, bare forearms, blue wristbands, a blue V-neck
    undershirt under the crossed orange gi, a blue sash, orange pants with chunky cloth folds, and yellow boots with a red stripe and blue bits. `boot` = (base, stripe, bits)."""
    base, stripe, bits = boot

    def fn(cv):
        for part in ("arm_r", "arm_l"):
            for nm in ("front", "back", "west", "east", "top"):
                f = cv.face(part, nm, False)
                if nm == "top":
                    DS._cloth(f, f.m_all(), top)
                    continue
                DS._cloth(f, f.m_rect(-0.2, -0.2, f.w + 0.2, 2.6, soft=0.5), top, folds=(1.3, 2.7) if nm in ("front", "back") else ())
                DS._cloth(f, f.m_rect(-0.2, 2.4, f.w + 0.2, 5.4, soft=0.5), under)
                H.trim_band(f, 2.2, 2.9, H.tone(top, 0.7), spec=0.1, outline=0.2)
                H.trim_band(f, 8.2, 11.9, band, spec=0.25)
                H.trim_band(f, 8.2, 8.7, H.tone(band, 1.35), spec=0.3)
        fb = cv.face("body", "front", False)
        fb.fill(fb.m_poly([(2.3, -0.1), (5.7, -0.1), (4.0, 4.2)], soft=0.4), under)               # the blue V-neck undershirt
        H.trim_line(fb, [(2.3, 0.0), (4.0, 4.2)], H.tone(top, 0.62), width=0.22, spec=0.1, outline=0.2)   # the crossed lapels
        H.trim_line(fb, [(5.7, 0.0), (4.0, 4.2)], H.tone(top, 0.62), width=0.22, spec=0.1, outline=0.2)
        fb.fill(fb.m_rect(2.4, 5.2, 5.6, 7.4, soft=0.25), H.tone(top, 1.22), 0.55)                # the lighter patch on the belly
        if sym_on:
            _symbol(fb, 5.9, 5.6, 0.95)
            _symbol(cv.face("body", "back", False), 4.0, 5.2, 1.7)
        for part in ("leg_r", "leg_l"):
            for nm in ("front", "back", "west", "east"):
                f = cv.face(part, nm, False)
                for (x0, y0, x1, y1) in ((0.5, 1.6, 1.6, 3.8), (2.4, 4.6, 3.5, 6.6), (0.6, 6.4, 1.8, 7.8)):
                    f.fill(f.m_rect(x0, y0, x1, y1, soft=0.2), H.tone(top if False else ORANGE_D, 0.8), 0.7)
                DS._cloth(f, f.m_rect(-0.2, 8.4, f.w + 0.2, 12.2, soft=0.3), base, top=1.15, bottom=0.8)
                H.trim_band(f, 9.5, 10.6, stripe, spec=0.2, outline=0.3)
                f.fill(f.m_rect(0.7, 10.9, 1.9, 11.9, soft=0.2), bits)
                f.fill(f.m_rect(f.w - 1.9, 10.9, f.w - 0.7, 11.9, soft=0.2), bits)
    return fn


def goku_outfit(top, under, band, belt, pants, boots, accent, trim=None, boot=(YELLOW, RED_B, BLUE_GI)):
    return dict(kind="coat", top=top, trim=trim or belt, pants=pants, boots=boots, belt=belt, accent=accent, gem=False, sleeve=0, marks=[goku_marks2(top, under, band, boot)])


def goku_hair(b, c, tall=1.0, spikes=5, long=0.0, tip=None, bangs=3, shell=True):
    """Goku's hair: a shell, tiers, a fan of tall spikes (the rows lean back), and three long bangs; `long` > 0 is the Super Saiyan 3 mane (down the back to the knees)."""
    if shell:
        P.hair_shell(b, c, g=0.3, thick=1.0, seed=1, layer="hair", front_low=30.8, side_low=28.8, back_low=25.4, side_front=-3.2)
        P.hair_tiers(b, c, n=3, y0=33.0, w0=9.0, shrink=1.5, h=1.2, seed=2, layer="hair")
    P.hair_spikes(b, c, n=max(3, spikes - 1), y=33.6, spread=3.6, length=7.2 * tall, w=4.8, seed=3, layer="hair", rows=2, back_lean=16.0 if long == 0 else 6.0, fan=40.0 if long == 0 else 20.0, tip=tip, depth_step=3.0)
    P.fringe(b, c, n=bangs * 3, span=8.2, y_top=31.8, length=3.8 + (0.4 if bangs > 3 else 0.0), seed=4, layer="hair_fine", rot=10.0, part=0.3)
    P.side_locks(b, c, y_top=29.6, length=5.0, count=1, seed=7, layer="hair_fine")
    if long > 0:
        P.long_hair(b, c, y_top=31.4, y_split=14.0, tail=long, tiers=3, count=7, width=9.2, seed=6, layer="hair_fine", sway=2.2, taper=0.2)


def goku_base(b):
    goku_hair(b, "#14121c", tall=1.0, spikes=5)
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#ffd23c", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_ssj(b):
    goku_hair(b, "#f2d23a", tall=1.1, spikes=5, tip="#fff3a0")
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#ffd23c", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_ssj2(b):
    goku_hair(b, "#f6d83a", tall=1.35, spikes=6, tip="#fffbc0", bangs=2)
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#ffd23c", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_ssj3(b):
    goku_hair(b, "#e8bd26", tall=0.85, spikes=5, long=19.0, tip="#ffe07a", bangs=2)
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#ffd23c", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_god(b):
    goku_hair(b, "#c8202a", tall=1.05, spikes=5, tip="#ff7a60")
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#ff7a60", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_blue(b):
    goku_hair(b, "#3ec4f4", tall=1.15, spikes=5, tip="#c6f2ff")
    P.bracers(b, False, y_top=21.6, rings=1, gold=BLUE_GI, gem="#8ae4ff", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_ui_sign(b):
    goku_hair(b, "#aeb6c8", tall=1.1, spikes=5, tip="#f0f4ff")                    # black roots and silver spikes: the dark base hair shows in the shell's lower rim (the skin hair field below)
    P.bracers(b, False, y_top=21.6, rings=1, gold=SILVER, gem="#e8f0ff", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)


def goku_mui(b):
    goku_hair(b, "#e6ecff", tall=1.25, spikes=6, tip="#ffffff", bangs=3)
    P.bracers(b, False, y_top=21.6, rings=2, gold=SILVER, gem="#ffffff", tassels=False, layer="main", sides=("r", "l"), gap=0.2, ring_h=1.0)
    P.halo(b, y=37.4, r=6.4, color="#e8f0ff", tilt=-4.0, spin=-10.0, rings=1)   # a slow halo of light behind the head: the calm of the perfected form


def goku(id, build, hair, e, brow, outfit, glow_marks=(), scale=1.0):
    return T.dv(id, build, TAN, hair, e, outfit, brow=brow, mouth=dict(kind="smile", color="#8a4a40", lip="#c47a68"), marks=list(glow_marks), scale=scale, body_skin=SUN, hairline=1.7)


def g_eyes(d, m, l, glow=None, **kw):
    k = dict(cy=4.95, h=2.1, tilt=0.16, lash_w=0.4)
    k.update(kw)
    return eyes(d, m, l, glow=glow, **k)


def g_brow(color, thick=0.3, angle=0.7):
    return dict(color=color, angle=angle, thick=thick)


GI = goku_outfit(ORANGE, BLUE_GI, BLUE_L, BLUE_GI, ORANGE_D, BLUE_GI, "#ffd23c")
GI_UI = goku_outfit(NAVY, "#3a4488", SILVER, SILVER, "#232b60", "#0e1230", "#cfd6ff", trim=SILVER, boot=("#232b60", SILVER, "#9aa6e8"))


def spark_lines(color):
    return lambda f: S.mark_lines(f, xs=(0.9, 7.1), y0=3.0, y1=6.2, color=color, glow=color, w=0.1, a=0.55)


GOKU = [
    goku("goku_base", goku_base, "#14121c", g_eyes("#120c0c", "#2a1c18", "#7a5e50"), g_brow("#14121c"), GI),
    goku("goku_ssj", goku_ssj, "#f2d23a", g_eyes("#0c5a56", "#26b8ae", "#bff8ee", glow="#7af0e0"), g_brow("#d8b82a"), GI),
    goku("goku_ssj2", goku_ssj2, "#f6d83a", g_eyes("#0c5a56", "#26b8ae", "#bff8ee", glow="#7af0e0"), g_brow("#d8b82a", angle=0.9), GI, glow_marks=[spark_lines("#fff27a")]),
    goku("goku_ssj3", goku_ssj3, "#e8bd26", g_eyes("#0c5a56", "#26b8ae", "#bff8ee", glow="#7af0e0", h=2.2), g_brow(TAN[1], thick=0.01), GI, glow_marks=[lambda f: S.mark_lines(f, xs=(2.0, 3.0, 5.0, 6.0), y0=0.4, y1=1.6, color="#9a5a3a", w=0.1, a=0.55)]),
    goku("goku_god", goku_god, "#c8202a", g_eyes("#5a0a10", "#d82a30", "#ffb8b0", glow="#ff4a50"), g_brow("#a8181e"), GI),
    goku("goku_blue", goku_blue, "#3ec4f4", g_eyes("#0a4a9a", "#2a9ae8", "#c8f0ff", glow="#8ae4ff"), g_brow("#2aa8d8"), GI, glow_marks=[spark_lines("#aee8ff")]),
    goku("goku_ui_sign", goku_ui_sign, "#aeb6c8", g_eyes("#5a6478", "#aab4cc", "#f4f8ff", glow="#dce4ff"), g_brow("#8c94a8"), GI_UI),
    goku("goku_mui", goku_mui, "#e6ecff", g_eyes("#8890a8", "#d8e0f8", "#ffffff", glow="#ffffff"), g_brow("#c0c8e0"), GI_UI, glow_marks=[spark_lines("#ffffff")], scale=1.0),
]

# ================================================================================================ Sinbad
PURPLE, PURPLE_D = "#6a3aa8", "#4a2480"
WHITE, GOLD = "#f4f0ea", "#e8b830"
SIN_SKIN = ("#b27648", "#cf935f", "#e6b383")

# (id, hair, hair tip, eye iris (dark, mid, light), glow, robe, trim, accent)  per Djinn of the player's table (relics/djinn/Djinn.java: color() and glow())
DJINN = {
    "baal":    ("#3c8cff", "#bfe4ff"),
    "valefor": ("#5a3db5", "#e0d2ff"),
    "zepar":   ("#d43a3a", "#ffb9a0"),
    "focalor": ("#62d6a0", "#e6fff1"),
    "vepar":   ("#00a6b8", "#bffcff"),
    "crocell": ("#9adcf0", "#ffffff"),
    "furfur":  ("#b77bff", "#f1ddff"),
}


def _earrings(b, layer="main"):
    geo = b.g(layer)
    metal = b.t_metal(GOLD, 8, 8)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 4.35 - 0.3, 26.3, -0.4), (0.6, 2.4, 0.6), metal, faces=("north", "south", "east", "west"))
        geo.cube(HEAD, (s * 4.35 - 0.4, 25.5, -0.5), (0.8, 0.8, 0.8), metal)


def _turban(b):
    """White turban in two wrapped bands with a gold brooch and three feathers (the king's, from the lore file)."""
    geo = b.g("main")
    cloth = b.t_cloth(WHITE, 16, 16, seed=3, folds=3)
    geo.cube(HEAD, (-4.7, 30.2, -4.7), (9.4, 1.6, 9.4), cloth)
    geo.cube(HEAD, (-4.45, 31.7, -4.45), (8.9, 1.5, 8.9), b.t_cloth("#e8e2da", 16, 16, seed=4, folds=3))
    geo.cube(HEAD, (-3.7, 33.1, -3.7), (7.4, 1.2, 7.4), cloth)
    b.g("fx").cube(HEAD, (-0.9, 30.4, -5.1), (1.8, 1.8, 0.5), b.t_gem("#d82a2a"), faces=("north", "up"))
    geo.cube(HEAD, (-1.3, 30.2, -5.0), (2.6, 2.2, 0.4), b.t_metal(GOLD, 8, 8), faces=("north", "up", "east", "west"))
    for i, (x, lean, h) in enumerate(((-1.8, -18.0, 7.0), (0.2, 0.0, 8.4), (2.2, 18.0, 7.0))):
        sp = b.t_spike("#f6f2f8", tw=12, th=28, seed=60 + i, tip="#d8c8f0")
        geo.cube(HEAD, (x - 0.9, 34.3, 1.5), (1.8, h, 0.0), sp, faces=("north",), rot=(-8.0, 0.0, -lean), pivot=(x, 34.3, 1.5))
        geo.cube(HEAD, (x, 34.3, 0.6), (0.0, h, 1.8), sp, faces=("east",), rot=(-8.0, 0.0, -lean), pivot=(x, 34.3, 1.5))


def _ponytail(b, c, length=18.0, hip=True):
    """The very long ponytail: a thick bound lock from the back of the head, tied high, swaying (the lore: very long purple hair in a ponytail), and one ahoge."""
    geo = b.g("hair_fine")
    tile = b.t_strand(c, tw=16, th=64, seed=21, prongs=3, tip=0.2)
    bone = b.sway("hair_fine", HEAD, "pony", (0.0, 30.4, 4.4), ax=3.0, az=4.0, walk=1.6, run=2.8)
    geo.card(bone, -2.0, 30.4, 4.5, 4.0, length, tile, rot=(0.0, 0.0, 0.0), pivot=(0.0, 30.4, 4.4))
    geo.cube(bone, (0.0, 30.4 - length, 2.5), (0.0, length, 4.0), tile, faces=("east",), pivot=(0.0, 30.4, 4.4))
    geo.cube(HEAD, (-1.1, 29.9, 4.0), (2.2, 2.2, 2.0), b.t_flat("#d8a830", 8, 8, 0.2))       # the gold tie
    sp = b.t_spike(c, tw=12, th=24, seed=23, tip=c)
    geo.cube(HEAD, (-0.7, 33.0, -0.5), (1.4, 5.0, 0.0), sp, faces=("north",), rot=(-6.0, 0.0, 0.0), pivot=(0.0, 33.0, -0.5))         # the ahoge


def sinbad_base(b):
    c = "#6a3aa8"
    D.shell(b, c, seed=11, back_low=26.0, tiers=1)
    P.fringe(b, c, n=9, span=8.2, y_top=31.6, length=3.6, seed=14, layer="hair_fine", rot=12.0, part=-0.4)
    P.side_locks(b, c, y_top=29.6, length=9.0, count=2, seed=17, layer="hair_fine")
    _ponytail(b, c, 22.0)
    _turban(b)
    _earrings(b)
    Q4.cloak_collar(b, WHITE, trim=GOLD, y=21.9, width=12.6, depth=6.8, thick=2.0, drop=3.4, back_drop=6.0, seed=70)
    Q4.robe(b, PURPLE, GOLD, y_top=13.4, length=12.0, rings=2, panels=10, radius=3.9, seed=76)
    Q4.stole(b, WHITE, GOLD, length=19.0, width=3.2, y_top=23.9)
    P.bracers(b, False, y_top=17.5, rings=3, gold=GOLD, gem="#d82a2a", tassels=False, layer="main", sides=("r", "l"))
    Q4.chest_gem(b, "#d82a2a", y=19.6, size=1.4)                                                      # the gold necklace's red jewel (Valefor's vessel)


def _djinn_body(key):
    """The body under a Djinn Equip: the same king with a mane instead of the ponytail, the robe and stole in the djinn's colours, the vessel glowing; no turban (the helm is the armour layer's)."""
    col, glow = DJINN[key]

    def build(b):
        hc = H.tone(col, 1.0)
        D.shell(b, col, seed=11, back_low=24.0, tiers=2)
        P.fringe(b, col, n=9, span=8.2, y_top=31.6, length=4.0, seed=14, layer="hair_fine", rot=12.0, part=-0.4)
        P.side_locks(b, col, y_top=29.6, length=12.0, count=2, seed=17, layer="hair_fine")
        P.long_hair(b, col, y_top=31.4, y_split=17.0, tail=18.0, tiers=3, count=7, width=9.0, seed=6, layer="hair_fine", sway=2.8, taper=0.1)
        _earrings(b)
        Q4.robe(b, H.tone(col, 0.45), glow, y_top=13.4, length=12.0, rings=2, panels=10, radius=3.9, seed=76)
        Q4.stole(b, glow, "#ffffff", length=19.0, width=3.2, y_top=23.9)
        P.bracers(b, False, y_top=17.5, rings=3, gold=glow, gem=col, tassels=False, layer="main", sides=("r", "l"))
        Q4.chest_gem(b, glow, y=19.6, size=1.8)
        Q4.studs(b, [(-2.6, 18.6, -2.7), (2.6, 18.6, -2.7)], color=glow, size=0.9)
    return build


def _sin_marks(glow):
    return weg(glow, 0.1)


def sinbad(id, build, hair, iris, glow, top, trim, accent, marks=(), e_glow=None):
    return T.dv(id, build, SIN_SKIN, hair, eyes(*iris, glow=e_glow, cy=4.95, h=2.0, tilt=0.12, lash_w=0.42),
                dict(kind="robe", top=top, trim=trim, accent=accent, gem=False, marks=list(marks), sleeve=12), brow=dict(color=H.tone(hair, 0.8), angle=0.4, thick=0.26),
                mouth=dict(kind="smirk", color="#7a3a30", lip="#b86a58"), marks=[], scale=1.04, body_skin=SIN_SKIN, hairline=1.7)


SINBAD_V1 = [sinbad("sinbad_base", sinbad_base, "#6a3aa8", ("#8a5a08", "#e8b020", "#fff0a0"), "#ffd23c", PURPLE, GOLD, "#ffd23c", e_glow="#ffd23c")]      # the first pass (kept, not in SPECS)
for _k, (_c, _g) in DJINN.items():
    SINBAD_V1.append(sinbad("sinbad_" + _k, _djinn_body(_k), _c, (H.tone(_c, 0.4), _c, _g), _g, H.tone(_c, 0.42), _g, _g, marks=[_sin_marks(_g)], e_glow=_g))

# ================================================================================================ Sinbad, second pass: the owner's reference images
# images/12.png = the king (white turban with a gold ornament, long loose purple hair, white and silver collar, white robe with a navy hem and a navy front sweep, gold arm band, wing sandals)
# images/13.png = Baal   (blue hair, a gold antler diadem with a red gem, blue feather and scale armour, gold chain necklace)
# images/14.png = Furfur (dark purple hair with orange tips, a huge feather fan, an orange ring of light, gold collar, black and orange sash, feather cuffs, black thorn marks)
# images/15.png = Zepar  (red hair, gold and red jewellery, a gold sun buckle on a dark green sash, a red skirt with green tatters, feather cuffs, red tribal marks, a pink ring of light)
# Valefor, Focalor, Vepar and Crocell have no reference: they use the same design language in their own table colours.
NAVY_S, NAVY_D, SILVER_S = "#2c2c70", "#1a1a46", "#cfd4e0"


def _s_feathers(b, bone, items, color, tip, seed, layer="main", w=2.2, down=True):
    """Feather cards. items = (x, y, z, length, lean, forward); down: hang from y with the tip down (the spike tile has its tip up, so it is turned over), else rise from y."""
    geo = b.g(layer)
    for i, (x, y, z, ln, lean, fwd) in enumerate(items):
        t = b.t_spike(color, tw=12, th=28, seed=seed + i % 4, tip=tip)
        geo.cube(bone, (x - w / 2, y, z), (w, ln, 0.0), t, faces=("north",), rot=(fwd, 0.0, (180.0 + lean) if down else lean), pivot=(x, y, z))


def _s_turban(b):
    """The king's white turban (image 12): two wrapped bands, a red brow gem in a gold plate, and a gold ornament on top: a ring with a fan of five gold feathers."""
    geo = b.g("main")
    cloth = b.t_cloth(WHITE, 16, 16, seed=3, folds=3)
    geo.cube(HEAD, (-4.7, 30.2, -4.7), (9.4, 1.6, 9.4), cloth)
    geo.cube(HEAD, (-4.45, 31.7, -4.45), (8.9, 1.5, 8.9), b.t_cloth("#e8e2da", 16, 16, seed=4, folds=3))
    geo.cube(HEAD, (-3.7, 33.1, -3.7), (7.4, 1.0, 7.4), cloth)
    b.g("fx").cube(HEAD, (-0.9, 30.5, -5.1), (1.8, 1.8, 0.5), b.t_gem("#d82a2a"), faces=("north", "up"))
    geo.cube(HEAD, (-1.3, 30.3, -5.0), (2.6, 2.2, 0.4), b.t_metal(GOLD, 8, 8), faces=("north", "up", "east", "west"))
    geo.cube(HEAD, (-1.0, 34.1, -1.0), (2.0, 0.8, 2.0), b.t_metal(GOLD, 8, 8))
    items = [(x, 34.6, -0.2, 2.6 + 0.6 * (2 - abs(i - 2)), lean, -6.0) for i, (x, lean) in enumerate(((-2.2, 52.0), (-1.1, 26.0), (0.0, 0.0), (1.1, -26.0), (2.2, -52.0)))]
    _s_feathers(b, HEAD, items, GOLD, "#fff0a0", 80, w=1.8, down=False)


def _s_hair(b, c, tail=19.0, y_split=20.0, locks=15.0, tip=None, count=8):
    D.shell(b, c, seed=11, back_low=25.0, tiers=2)
    P.fringe(b, c, n=9, span=8.2, y_top=31.6, length=4.2, seed=14, layer="hair_fine", rot=12.0, part=-0.4)
    P.side_locks(b, c, y_top=29.6, length=locks, count=2, seed=17, layer="hair_fine")
    P.long_hair(b, c, y_top=31.4, y_split=y_split, tail=tail, tiers=3, count=count, width=9.4, seed=6, layer="hair_fine", sway=2.6, taper=0.12)
    if tip:                                                   # a second, outer layer of lighter strands (the tips of 14, the red of 15)
        P.long_hair(b, tip, y_top=16.0, y_split=12.0, tail=12.0, tiers=2, count=max(4, count - 4), width=8.2, seed=9, layer="hair_fine", sway=3.0, taper=0.3, z0=5.3)


def _s_usekh(b, main, stripe, trim, drop=4.4):
    """The wide collar of image 12 (silver and white with blue stripes) and the gold collars of 13, 14 and 15: a ring round the neck, three rows hanging in a V on the chest, a back plate."""
    geo = b.g("main")
    ring = b.t_metal(trim, 16, 8)
    geo.cube("body", (-4.7, 23.2, -3.2), (9.4, 1.2, 6.4), ring)
    for i in range(3):
        w = 9.2 - 2.5 * i
        tile = b.t_cloth(main if i != 1 else stripe, 16, 8, seed=90 + i, trim=trim if i == 2 else None, trim_h=1, folds=5, hem="scallop")
        geo.card("body", 0.0, 23.3 - 1.5 * i, -3.22 - 0.06 * i, w, 1.9 if i < 2 else drop - 3.0, tile)
    geo.card("body", 0.0, 23.3, 3.25, 8.8, 3.6, b.t_cloth(main, 16, 16, seed=94, trim=trim, trim_h=1, folds=4))
    for s in (-1, 1):
        geo.cube("body", (s * 4.45 - 0.4, 22.0, -2.9), (0.8, 2.4, 5.8), b.t_cloth(main, 8, 16, seed=95, folds=2))


def _s_chains(b, gold, y=19.4, pendant=None):
    """Two loops of gold chain on the bare chest with a pendant ring (images 13, 14, 15)."""
    geo = b.g("main")
    metal = b.t_metal(gold, 16, 4, band=False, rivets=False)
    for i, (w, yy) in enumerate(((6.4, y), (4.6, y - 1.7))):
        geo.cube("body", (-w / 2, yy, -2.78), (w, 0.35, 0.3), metal)
        for s in (-1, 1):
            geo.cube("body", (s * w / 2 - 0.2, yy + 0.35, -2.78), (0.4, 1.6 - 0.4 * i, 0.3), metal)
    if pendant:
        b.g("fx").cube("body", (-0.75, y - 4.0, -2.95), (1.5, 1.5, 0.5), b.t_gem(pendant), faces=("north", "up"))
    geo.cube("body", (-1.0, y - 3.4, -2.9), (2.0, 0.5, 0.4), b.t_metal(gold, 8, 8))


def _s_belt(b, cloth, gold, gem, y=12.4, sun=False, sash=None):
    """The belt: a wide cloth band with a round gold buckle (a sun disc on 15, a gem on the others); `sash` hangs a second cloth tail from it."""
    geo = b.g("main")
    geo.cube("body", (-4.35, y, -2.35), (8.7, 1.9, 4.7), b.t_cloth(cloth, 32, 8, seed=97, folds=6, trim=gold, trim_h=1))
    r = 1.7 if sun else 1.3
    geo.cube("body", (-r, y - 0.1, -2.6), (2 * r, 2.1, 0.5), b.t_metal(gold, 16, 16), faces=("north", "up", "down", "east", "west"))
    b.g("fx").cube("body", (-0.6, y + 0.4, -2.95), (1.2, 1.2, 0.5), b.t_gem(gem), faces=("north", "up"))
    if sun:
        for k in range(8):
            ang = k * 45.0
            geo.cube("body", (-0.35, y + 0.95 + r - 0.2, -2.62), (0.7, 0.9, 0.35), b.t_metal(gold, 8, 8), faces=("north", "up"), rot=(0.0, 0.0, ang), pivot=(0.0, y + 0.95, -2.62))
    if sash:
        geo.card("body", 3.4, y + 0.2, -2.55, 2.2, 9.0, b.t_cloth(sash, 16, 24, seed=98, hem="zigzag", folds=3))


def _s_skirt(b, main, hem, panel, panel_hem, trim, length=13.0, rings=2, tatter=None):
    """The long skirt: two rings of cloth cards round the hips (hem band in `hem`), and a front and a back sweep in `panel` with a jagged hem (the navy sweep of image 12, the green tatters of 15)."""
    Q4.robe(b, main, hem, y_top=13.4, length=length, rings=rings, panels=10, radius=3.9, seed=76)
    geo = b.g("main")
    geo.card("body", 0.0, 13.3, -4.95, 5.8, length - 0.6, b.t_cloth(panel, 16, 32, seed=101, hem="zigzag", trim=trim, trim_h=1, folds=3))
    geo.card("body", 0.0, 13.3, 4.95, 6.4, length - 1.0, b.t_cloth(panel, 16, 32, seed=102, hem="zigzag", trim=trim, trim_h=1, folds=3))
    for s in (-1, 1):
        sw = b.sway("main", "body", "sksw_%d" % (1 if s > 0 else 0), (s * 4.6, 13.3, -1.0), ax=2.0, az=2.4, walk=2.0, run=3.2, phase=0.7 if s > 0 else 0.0)
        geo.card(sw, s * 4.75, 13.3, -1.0, 4.4, length - 2.0, b.t_cloth(panel_hem, 16, 32, seed=103, hem="zigzag", folds=3), rot=(0.0, 90.0, 0.0), pivot=(s * 4.75, 13.3, -1.0))
    if tatter:
        items = [(-4.0 + 1.0 * i, 2.8, -4.8 if i % 2 else 4.8, 3.6 + 0.6 * (i % 3), (-1) ** i * 6.0, 0.0) for i in range(9)]
        _s_feathers(b, "body", items, tatter[0], tatter[1], 140, w=1.9)


def _s_sandals(b, gold, wing):
    """Wing sandals (images 12 and 15): a gold toe plate with a point and a small wing on the outer ankle."""
    geo = b.g("main")
    metal = b.t_metal(gold, 16, 8)
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        bone = "leg_" + sd
        geo.cube(bone, (s * 1.9 - 1.6, 0.0, -4.2), (3.2, 1.0, 2.2), metal)
        geo.cube(bone, (s * 1.9 - 0.8, 0.3, -5.2), (1.6, 0.7, 1.2), metal)
        geo.cube(bone, (s * 1.9 - 2.4, 1.0, -2.6), (4.8, 0.7, 5.2), metal)
        items = [(s * 3.4, 3.2 + 0.0 * k, 0.3 - 0.8 * k, 3.0 + 0.7 * k, s * (-70.0 + 18.0 * k), 0.0) for k in range(3)]
        _s_feathers(b, bone, items, wing, "#ffffff", 150 + (3 if s > 0 else 0), w=1.5, down=False)


def _s_swirl(b, color, r=13.5, y=18.0, ring2=True):
    """The ring of light behind him (the orange loop of image 14, the pink one of 15): a big upright ring behind the back and a second loop crossing it at an angle, both in the fx layer (drawn additive, so the colour is kept deep)."""
    geo = b.g("fx")
    col = H.tone(color, 0.62)
    tile = b.t_ring(col, rings=1)
    bone = b.sway("fx", "body", "swirl_back", (0.0, y, 5.2), ax=0.0, az=2.0, walk=1.0, run=1.6, bob=0.35)
    geo.cube(bone, (-r, y - r, 5.2), (2 * r, 2 * r, 0.05), tile, faces=("north", "south"), pivot=(0.0, y, 5.2))
    if ring2:
        t2 = b.t_ring(H.tone(color, 0.5), rings=2, seed=3)
        b2 = b.sway("fx", "body", "swirl_cross", (0.0, y + 1.0, 5.6), ax=0.0, az=3.0, walk=1.0, run=1.6, bob=-0.3, phase=1.6)
        geo.cube(b2, (-r * 0.8, y + 1.0 - r * 0.8, 5.6), (r * 1.6, r * 1.6, 0.05), t2, faces=("north", "south"), rot=(0.0, 0.0, 38.0), pivot=(0.0, y + 1.0, 5.6))


def _s_shoulders(b, kind, main, hi, tip, gold, seed=120):
    """Shoulder armour: feather rows (images 13, 14 and the feathered mantle), scale plates, or crystal shards; one on each arm."""
    geo = b.g("main")
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        bone = "arm_" + sd
        cx = s * 6.0
        if kind == "none":
            continue
        base = b.t_cloth(main, 16, 16, seed=seed + 3, folds=3) if kind == "feather" else b.t_metal(main, 16, 16)
        geo.cube(bone, (cx - 2.85, 20.9, -2.85), (5.7, 3.6, 5.7), base)
        geo.cube(bone, (cx - 3.0, 23.6, -3.0), (6.0, 0.7, 6.0), b.t_metal(gold, 16, 4, band=False, rivets=False))
        if kind == "feather":
            items = []
            for k in range(3):
                y = 24.0 - 1.6 * k
                ln = 3.8 + 0.6 * k
                for j, dx in enumerate((-1.1, 1.1)):
                    items.append((cx + dx + s * 0.35 * k, y, -3.0 - 0.07 * k, ln, s * (8.0 + 6.0 * k), 6.0))
                    items.append((cx + dx + s * 0.35 * k, y, 3.0 + 0.07 * k, ln, s * (8.0 + 6.0 * k), -6.0))
            _s_feathers(b, bone, items, hi if sd == "r" else main, tip, seed + (0 if sd == "r" else 7), w=2.3)
            _s_feathers(b, bone, [(cx + s * 3.05, 24.0, z, 4.4, s * 14.0, 0.0) for z in (-1.4, 1.4)], main, tip, seed + 20, w=2.3)
        elif kind == "scale":
            for k in range(3):
                geo.cube(bone, (cx - 3.05 - 0.05 * k, 22.6 - 1.2 * k, -3.05), (6.1 + 0.1 * k, 1.0, 6.1), b.t_metal(hi if k % 2 else main, 16, 8))
        elif kind == "crystal":
            dia = Q4.t_diamond(b, hi, edge=main)
            for k, z in enumerate((-1.6, 0.0, 1.6)):
                geo.cube(bone, (cx + s * 0.9 - 1.0, 24.3, z), (2.0, 3.6 + 0.6 * (1 - abs(k - 1)), 0.0), dia, faces=("north",), rot=(0.0, 0.0, -s * (24.0 + 8.0 * k)), pivot=(cx + s * 0.9, 24.3, z))
                geo.cube(bone, (cx + s * 0.9, 24.3, z - 1.0), (0.0, 3.6 + 0.6 * (1 - abs(k - 1)), 2.0), dia, faces=("east",), rot=(0.0, 0.0, -s * (24.0 + 8.0 * k)), pivot=(cx + s * 0.9, 24.3, z))


def _s_cuffs(b, kind, main, tip, gold, seed=160):
    """Forearm cuffs: a cloth band with a fall of dark feathers (14, 15), or plain gold and metal bands (12 and the rest)."""
    geo = b.g("main")
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        bone = "arm_" + sd
        cx = s * 6.0
        if kind == "feather":
            geo.cube(bone, (cx - 2.6, 13.4, -2.6), (5.2, 3.4, 5.2), b.t_cloth(main, 16, 16, seed=seed, folds=3))
            geo.cube(bone, (cx - 2.7, 16.4, -2.7), (5.4, 0.7, 5.4), b.t_metal(gold, 16, 4, band=False, rivets=False))
            items = []
            for k, z in enumerate((-2.7, -1.0, 1.0, 2.7)):
                items.append((cx - 1.0 + (k % 2) * 2.0, 14.0, z, 4.6 + 0.8 * (k % 3), s * (4.0 + 5.0 * k), 0.0))
            items += [(cx + s * 2.75, 14.0, z, 4.4, s * 10.0, 0.0) for z in (-1.2, 1.2)]
            _s_feathers(b, bone, items, main, tip, seed + 5, w=2.2)
        else:
            for i in range(2):
                geo.cube(bone, (cx - 2.5, 13.2 + i * 1.5, -2.5), (5.0, 1.2, 5.0), b.t_metal(gold, 16, 16))


def _s_armband(b, gold, y=21.0):
    geo = b.g("main")
    metal = b.t_metal(gold, 16, 8)
    geo.cube("arm_r", (-8.6, y, -2.6), (5.2, 1.4, 5.2), metal)
    geo.cube("arm_r", (-8.5, y + 1.7, -2.5), (5.0, 0.5, 5.0), b.t_metal(gold, 16, 4, band=False, rivets=False))


def _s_antlers(b, gold, gem, tall=1.0):
    """The gold antler diadem of image 13: a brow band with a red gem, and from each side a long beam sweeping up and out with two forks."""
    geo = b.g("main")
    metal = b.t_metal(gold, 8, 16)
    geo.cube(HEAD, (-5.1, 30.6, -5.1), (10.2, 0.9, 1.0), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 5.1 - (0.0 if s < 0 else 1.0), 30.6, -5.1), (1.0, 0.9, 10.2), metal)
    geo.cube(HEAD, (-5.1, 30.6, 4.1), (10.2, 0.9, 1.0), metal)
    b.g("fx").cube(HEAD, (-0.9, 30.2, -5.55), (1.8, 1.9, 0.55), b.t_gem(gem), faces=("north", "up"))
    geo.cube(HEAD, (-1.5, 31.3, -5.1), (3.0, 0.6, 0.6), metal)                       # the little crest over the gem
    for s in (-1, 1):
        px, py, pz = s * 2.2, 31.4, -5.0
        # main beam out and up, then a fork up and a fork out
        geo.cube(HEAD, (px - 0.3, py, pz - 0.3), (0.6, 5.2 * tall, 0.6), metal, rot=(0.0, 0.0, -s * 52.0), pivot=(px, py, pz))
        for k, (ln, ang) in enumerate(((3.4, 22.0), (2.6, 74.0))):
            qx, qy = px + s * (1.6 + 1.9 * k), py + (1.7 + 1.2 * k) * tall
            geo.cube(HEAD, (qx - 0.28, qy, pz - 0.28), (0.56, ln * tall, 0.56), metal, rot=(0.0, 0.0, -s * ang), pivot=(qx, qy, pz))


def _s_circlet(b, gold, gem, fan=0, y=30.5):
    """A gold head band (14, 15) with a gem, and optionally a small fan of gold points above the brow (the diadems of the equips without a reference)."""
    geo = b.g("main")
    metal = b.t_metal(gold, 8, 16)
    geo.cube(HEAD, (-5.1, y, -5.1), (10.2, 0.9, 1.0), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 5.1 - (0.0 if s < 0 else 1.0), y, -5.1), (1.0, 0.9, 10.2), metal)
    geo.cube(HEAD, (-5.1, y, 4.1), (10.2, 0.9, 1.0), metal)
    b.g("fx").cube(HEAD, (-0.9, y - 0.4, -5.55), (1.8, 1.9, 0.55), b.t_gem(gem), faces=("north", "up"))
    if fan:
        items = [(x, y + 0.8, -5.0, 3.2 + 0.7 * (fan - abs(i - fan // 2)) / fan, lean, 0.0) for i, (x, lean) in enumerate(((-2.8, 30.0), (-1.4, 15.0), (0.0, 0.0), (1.4, -15.0), (2.8, -30.0))[:fan])]
        _s_feathers(b, HEAD, items, gold, "#fff0a0", 180, w=1.5, down=False)


def _s_crystal_crown(b, col, hi, gold):
    geo = b.g("main")
    metal = b.t_metal(gold, 8, 16)
    geo.cube(HEAD, (-5.1, 30.6, -5.1), (10.2, 0.9, 1.0), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * 5.1 - (0.0 if s < 0 else 1.0), 30.6, -5.1), (1.0, 0.9, 10.2), metal)
    geo.cube(HEAD, (-5.1, 30.6, 4.1), (10.2, 0.9, 1.0), metal)
    dia = Q4.t_diamond(b, hi, edge=col)
    for i, (x, h) in enumerate(((-3.6, 3.0), (-1.8, 4.4), (0.0, 5.6), (1.8, 4.4), (3.6, 3.0))):
        geo.cube(HEAD, (x - 0.9, 31.4, -5.05), (1.8, h, 0.0), dia, faces=("north",))
        geo.cube(HEAD, (x, 31.4, -5.95), (0.0, h, 1.8), dia, faces=("east",))


def _s_fan(b, main, tip, n=9, length=13.0, spread=84.0, y=24.0, seed=200):
    """The big fan of dark feathers behind the head and shoulders (image 14): n long feathers rising from the shoulder blades and fanning out wide, swaying a little."""
    items = []
    for i in range(n):
        u = (i / (n - 1.0)) * 2.0 - 1.0
        items.append((u * 3.0, y - 1.5 * abs(u), 5.6 + 0.1 * abs(u), length * (1.0 - 0.3 * abs(u)) + 1.5 * ((i * 7) % 3) / 3.0, u * spread, -10.0))
    bone = b.sway("main", "body", "mane_fan", (0.0, y, 5.6), ax=1.5, az=2.2, walk=2.0, run=3.4, cast=2.0)
    _s_feathers(b, bone, items, main, tip, seed, w=5.4, down=False)


def _s_marks(style, glow, armor, hi, gold):
    """Painted body marks: glowing lines down the arms (all djinn), plus the tribal red lines of Zepar, the black thorn cracks of Furfur, and feather-scale shading on the upper arms of the armoured ones."""
    base = weg(glow, 0.1)

    def fn(cv):
        base(cv)
        fb = cv.face("body", "front", False)
        if style == "tribal":
            for pts in (((1.0, 1.6), (2.4, 3.6), (1.6, 6.2)), ((7.0, 1.6), (5.6, 3.6), (6.4, 6.2)), ((3.4, 5.6), (4.0, 7.4), (4.6, 5.6))):
                fb.polyline(list(pts), armor, 0.32, a=0.95)
            for part in ("arm_r", "arm_l"):
                for nm in ("front", "back"):
                    f = cv.face(part, nm, False)
                    f.polyline([(0.5, 3.0), (2.0, 4.2), (3.4, 3.0)], armor, 0.26, a=0.95)
                    f.polyline([(0.5, 5.4), (2.0, 6.6), (3.4, 5.4)], armor, 0.26, a=0.95)
        elif style == "thorns":
            for pts in (((1.6, 4.4), (2.9, 6.0), (2.2, 8.6), (3.3, 10.4)), ((6.4, 4.4), (5.1, 6.0), (5.8, 8.6), (4.7, 10.4)), ((3.7, 6.6), (4.0, 8.4), (4.3, 6.6))):
                fb.polyline(list(pts), "#120a1c", 0.34, a=0.95)
                fb.polyline(list(pts), armor, 0.1, a=0.9, glow=armor)
        if style in ("scales", "tribal", "thorns"):
            return
        for part in ("arm_r", "arm_l"):
            for nm in ("front", "back", "west", "east"):
                f = cv.face(part, nm, False)
                for row in range(0, 4):
                    y = 6.0 + row * 1.4
                    for x in range(-1 + (row % 2), int(f.w) + 1, 2):
                        f.ellipse(x + 0.6, y, 0.95, 0.85, armor if (row + x) % 2 == 0 else hi, a=0.55)
    return fn


def sinbad2(id, build, hair, iris, glow, cloth, trim, accent, marks=(), e_glow=None, boots=None, body=None):
    return T.dv(id, build, SIN_SKIN, hair, eyes(*iris, glow=e_glow, cy=4.95, h=2.0, tilt=0.12, lash_w=0.42),
                dict(kind="bare", top=cloth, pants=cloth, boots=boots or cloth, trim=trim, accent=accent, gem=False, marks=list(marks), sleeve=0), brow=dict(color=H.tone(hair, 0.8), angle=0.4, thick=0.26),
                mouth=dict(kind="smirk", color="#7a3a30", lip="#b86a58"), marks=[], scale=1.04, body_skin=SIN_SKIN, hairline=1.7)


def _s_base(b):
    """Image 12: the king."""
    c = "#6a3aa8"
    _s_hair(b, c, tail=18.0, y_split=21.0, locks=14.0)
    _s_turban(b)
    _earrings(b)
    _s_usekh(b, "#e8eaf2", "#4a5ab0", SILVER_S, drop=5.0)
    _s_skirt(b, WHITE, NAVY_S, NAVY_S, WHITE, GOLD, length=13.2)
    _s_belt(b, NAVY_D, GOLD, "#d82a2a", sash=WHITE)
    _s_armband(b, GOLD)
    for i in range(3):                                                       # the silver cuff on the left wrist
        b.g("main").cube("arm_l", (3.5, 12.4 + 1.2 * i, -2.5), (5.0, 1.0, 5.0), b.t_metal(SILVER_S, 16, 8))
    _s_sandals(b, GOLD, "#ffffff")


# per djinn: hair, hair tip layer, armour, armour light, jewel metal, crown, shoulders, cuffs, skirt (main, hem, panel), belt, gem, swirl, back fan, marks style, tatters
LOOKS_S = {
    "baal":    dict(hair="#3c8cff", tip=None, armor="#3f8fc8", hi="#bfe4ff", gold=GOLD, crown="antler", sh="feather", cuffs="feather", skirt=("#1f5a96", "#bfe4ff", "#163e6a"), belt="#163e6a", gem="#d82a4a", ring="#8fd0ff", fan=None, style="scales", tatter=None),
    "valefor": dict(hair="#5a3db5", tip=None, armor="#7a5ad8", hi="#e0d2ff", gold=GOLD, crown="fan", sh="feather", cuffs="feather", skirt=("#3a2a78", "#e0d2ff", "#2a1c5a"), belt="#2a1c5a", gem="#e0d2ff", ring="#c8b4ff", fan=("#7a5ad8", "#e0d2ff"), style="scales", tatter=None),
    "zepar":   dict(hair="#d43a3a", tip="#7a1424", armor="#6a1424", hi="#d43a3a", gold=GOLD, crown="circlet", sh="none", cuffs="feather", skirt=("#c8283a", "#2a5a3a", "#2e6a46"), belt="#1c3a2a", gem="#ffb9a0", ring="#ff8a9a", fan=None, style="tribal", tatter=("#5a1020", "#d43a3a")),
    "focalor": dict(hair="#62d6a0", tip=None, armor="#3aa882", hi="#e6fff1", gold=GOLD, crown="fan", sh="scale", cuffs="band", skirt=("#2a7a62", "#e6fff1", "#1c5a48"), belt="#1c5a48", gem="#e6fff1", ring="#a8f0d0", fan=None, style="plain", tatter=None),
    "vepar":   dict(hair="#00a6b8", tip=None, armor="#0a8a9a", hi="#bffcff", gold=GOLD, crown="fan", sh="scale", cuffs="feather", skirt=("#0a6a7a", "#bffcff", "#08505e"), belt="#08505e", gem="#bffcff", ring="#7af0ff", fan=None, style="plain", tatter=None),
    "crocell": dict(hair="#9adcf0", tip=None, armor="#6ab0d0", hi="#ffffff", gold=SILVER_S, crown="crystal", sh="crystal", cuffs="band", skirt=("#a8d8ee", "#ffffff", "#7ab8d8"), belt="#6ab0d0", gem="#ffffff", ring="#ffffff", fan=None, style="plain", tatter=None),
    "furfur":  dict(hair="#7a3ab0", tip="#ff9a3a", armor="#241438", hi="#b77bff", gold=GOLD, crown="circlet", sh="none", cuffs="feather", skirt=("#e8782a", "#1c1428", "#7a2a10"), belt="#1c1428", gem="#ff9a3a", ring="#ff9a3a", fan=("#2a1840", "#b77bff"), style="thorns", tatter=("#241438", "#ff9a3a")),
}


def _s_djinn_body(key):
    """The body under a Djinn Equip, in the design of the reference images: long loose hair in the djinn's colour, a gold crown or diadem, a wide gold collar and chains on the bare chest, feather / scale / crystal shoulders, feather cuffs, a belt with a round buckle, a long skirt with a jagged sweep, wing sandals, and a ring of light."""
    k = LOOKS_S[key]
    col, glow = DJINN[key]

    def build(b):
        _s_hair(b, k["hair"], tail=19.0, y_split=19.0, locks=15.0, tip=k["tip"], count=9)
        _earrings(b)
        crown = k["crown"]
        if crown == "antler":
            _s_antlers(b, k["gold"], k["gem"])
        elif crown == "crystal":
            _s_crystal_crown(b, k["armor"], k["hi"], k["gold"])
        else:
            _s_circlet(b, k["gold"], k["gem"], fan=5 if crown == "fan" else 0)
        if k["fan"]:
            _s_fan(b, k["fan"][0], k["fan"][1], n=9 if key == "furfur" else 7, length=20.0 if key == "furfur" else 15.0)
        _s_shoulders(b, k["sh"], k["armor"], k["hi"], k["hi"], k["gold"])
        _s_usekh(b, k["armor"] if k["sh"] != "none" else k["gold"], k["hi"], k["gold"], drop=4.0)
        _s_chains(b, k["gold"], pendant=k["gem"])
        _s_cuffs(b, k["cuffs"], k["armor"], k["hi"], k["gold"])
        sk = k["skirt"]
        _s_skirt(b, sk[0], H.tone(sk[0], 0.7), sk[2], sk[1], k["gold"], length=13.0, tatter=k["tatter"])
        _s_belt(b, k["belt"], k["gold"], k["gem"], sun=(key == "zepar"), sash=sk[2])
        _s_sandals(b, k["gold"], k["hi"])
        _s_swirl(b, k["ring"])
    return build


SINBAD = [sinbad2("sinbad_base", _s_base, "#6a3aa8", ("#8a5a08", "#e8b020", "#fff0a0"), "#ffd23c", WHITE, GOLD, "#ffd23c", e_glow="#ffd23c", boots="#e8e0d0")]
for _key, (_c, _g) in DJINN.items():
    _l = LOOKS_S[_key]
    SINBAD.append(sinbad2("sinbad_" + _key, _s_djinn_body(_key), _l["hair"], (H.tone(_c, 0.4), _c, _g), _g, _l["skirt"][0], _l["gold"], _g,
                          marks=[_s_marks(_l["style"], _g, _l["armor"], _l["hi"], _l["gold"])], e_glow=_g, boots=_l["gold"]))

SPECS = GOKU + SINBAD
IDS = [s["id"] for s in SPECS]
