"""Gowther, the Goat's Sin of Lust, HD 256x256: the painter of both forms (chars/gowther.py and chars/gowther_form2.py are thin wrappers).

Outfit of the 64x64 sheet (tools/arc/char_gowther.py, docs/arc/gowther.md): a knee-length indigo coat with pink piping, a high collar, a white shirt with pearl buttons, a pink cravat, white gloves, a
double belt with a pink buckle, calf boots with pink cuffs; the pink crest of the Goat on the chest glows, so do the eyes and the trim. The second form (the doll unbound) is the same coat in a lighter indigo
with pale-pink trim, long pale-pink hair, cracks of light on the face and light lines on the cloth, and the mounts of the mechanical wings on the back (the wings and the broken halo are separate models:
pmx_gowther_form2, they attach at the shoulder blades and above the head, so the back keeps two plain mount rings and nothing else there).

Drawn at 4x detail on BOTH layers (the game drops the second layer and the hair beyond 24 blocks): the round amber eyes with long lashes, thin plum brows, the doll's blush; the high collar and its piping;
the pink satin cravat and its pearl; the shirt with its placket and buttons shaded by the coat; the coat panels with sheen, creases and stitched piping; the crest brooch; the double belt and its pink
buckle; the cuffs, the white gloves with knuckles; the coat tails with their hem trim; the boots with turned cuffs, straps, welt and sole. Original art from memory of the character's look
(docs/arc/gowther.md lists the sources; the English and Japanese wikis could not be fetched in this session), nothing traced.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds2 as S  # noqa: E402

A, H = S.A, S.H

# ------------------------------------------------------------------------------------------------ palettes (spec: tools/arc/char_gowther.py)
SKIN = ("#e0bcb9", "#f7dcd4", "#fff0e8")
F1 = {
    "hair": "#b4457f", "hair_rgb": (180, 69, 127), "hair_pal": S.hair_pal("#b4457f", dark="#6a2250", light="#d5679f", high="#f7b6d9"),
    "style": "anime_short",
    "coat": "#2f2a66", "coat_l": "#4b4597", "coat_d": "#1e1b44", "belt": "#1e1b44", "belt_b": "#13112e",
    "trim": "#e68ac0", "trim_l": "#ffc6e8", "shirt": "#f4eef8", "boot": "#262050", "strap": "#4a3a80", "glove": "#f4eef8",
    "iris": ("#6e4310", "#e0a030", "#ffeaa8"), "eye_glow": "#ffcf70", "lash": "#3a1633", "brow": "#6a2650",
    "glow": "#ff8ad0", "form2": False,
}
F2 = dict(F1)
F2.update({
    "hair": "#ffacd8", "hair_rgb": (255, 172, 216), "hair_pal": S.hair_pal("#ffacd8", dark="#c0608f", light="#ffd0ea", high="#ffffff"),
    "style": "anime_long",
    "coat": "#4a448c", "coat_l": "#6a62b4", "coat_d": "#2c2864", "belt": "#2c2864", "belt_b": "#1c1944",
    "trim": "#ffd0ec", "trim_l": "#ffffff", "shirt": "#fbf6ff",
    "iris": ("#8a5a14", "#ffd070", "#fff6d0"), "eye_glow": "#ffd890", "lash": "#5a2a52", "brow": "#a85a8a",
    "glow": "#ffb0e4", "form2": True,
})


# ================================================================================================ the head
def head_front(cv, P):
    f = cv.face("head", "front")
    pal = P["hair_pal"]
    H.skin_face(f, SKIN, hair=None, side=0.08, chin=0.06, shadow=0.0)
    # the fringe: a soft mass at the top, parted a little left of the middle, locks falling to both sides of the face
    mass = S.hair_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.5, 2.0), (6.5, 2.5), (5.6, 1.8), (4.6, 1.2), (3.6, 1.7), (2.6, 2.2), (1.4, 2.6), (-0.3, 2.2)])
    S.strands(f, pal, (0.9, 2.1, 3.3, 4.9, 6.1, 7.2), 0.0, 1.9, mass)
    S.lock(f, 1.45, 1.7, 0.9, 2.6, 0.3, pal, 0.9)                                         # the long side-swept bang on the left
    S.lock(f, 2.75, 1.2, 1.2, 1.7, -0.1, pal, 1.0)
    S.lock(f, 6.55, 1.4, 1.1, 2.0, -0.2, pal, 1.0)
    S.lock(f, 5.4, 1.1, 1.1, 1.3, 0.1, pal, 0.9)
    S.lock(f, 0.38, 0.95, 0.9, 4.4, 0.1, pal, 1.0)                                         # sideburns that frame the face
    S.lock(f, 7.62, 0.95, 0.9, 4.4, -0.1, pal, 1.0)
    # the brows: thin and calm
    H.brow(f, (3.45, 3.95), (1.2, 3.7), P["brow"], 0.24, 0.10, arch=0.05)
    H.brow(f, (4.55, 3.95), (6.8, 3.7), P["brow"], 0.24, 0.10, arch=0.05)
    # the eyes: round doll eyes, amber, long lashes; the iris glows
    for ex in (2.3, 5.7):
        H.eye(f, ex, 5.1, 2.5, 2.5, P["iris"], lash=P["lash"], tilt=0.04, lash_w=0.36, flick=0.3, iris_r=1.06, glow=None, lid_shadow=0.22)
        f.glow_fill(f.m_ellipse(ex, 5.3, 0.62, 0.72), P["eye_glow"], 0.55)                          # a small light in the iris, the rest of the eye stays dark
    H.blush(f, 1.2, 6.35, 0.95, 0.5, "#f08ab0", 0.55)                                      # the doll's blush
    H.blush(f, 6.8, 6.35, 0.95, 0.5, "#f08ab0", 0.55)
    H.nose(f, 4.0, 6.15, "#d9a4a0", 0.22)
    H.mouth_smile(f, 4.0, 7.0, 1.6, color="#7a2a48", lip="#e07a98", depth=0.07, thick=0.22)
    if P["form2"]:
        crack(f, [(1.0, 2.4), (1.5, 3.1), (1.2, 3.9), (1.9, 4.5)], P)
        crack(f, [(0.5, 5.9), (1.2, 6.4), (1.1, 7.2), (1.8, 7.8)], P)
        crack(f, [(7.5, 2.8), (6.9, 3.6), (7.3, 4.3), (6.8, 5.0)], P)
        light_line(f, [(2.4, 6.55), (1.6, 6.85), (0.7, 6.8)], P)
        light_line(f, [(5.6, 6.55), (6.4, 6.85), (7.3, 6.8)], P)


def head_sides(cv, P):
    for nm in ("west", "east"):
        for ov in (False, True):
            S.ear(cv.face("head", nm, ov), nm, SKIN, 4.7)


def crack(f, pts, P):
    """A crack of the doll's porcelain: a dark seam with a glowing core (the glow map carries the light)."""
    f.fill(f.m_polyline(pts, 0.2), "#7a3260", 0.85)
    f.fill(f.m_polyline(pts, 0.1), "#ffe6f6", 0.95, glow=P["glow"])
    f.glow_fill(f.m_polyline(pts, 0.3, soft=3.0), P["glow"], 0.5)


def light_line(f, pts, P, a=0.7):
    f.fill(f.m_polyline(pts, 0.12), "#ffffff", a * 0.7, glow=P["glow"])


# ================================================================================================ the body
def glow_edge(f, pts, P, width=0.2, a=0.35):
    """The piping of the coat glows a little (spec: glow trim)."""
    f.glow_fill(f.m_polyline(pts, width), P["glow"], a)


def collar_front(f, P):
    """The high collar: two stiff flaps that open on a V at the neck, piped in pink."""
    for sgn in (1.0, -1.0):
        x = (lambda v: v) if sgn > 0 else (lambda v: f.w - v)
        flap = f.m_poly([(x(0.0), 0.0), (x(2.65), 0.0), (x(2.5), 0.7), (x(1.7), 1.75), (x(0.0), 1.75)])
        f.fill(flap, H.lin((0, 0), (0, 1.75), [(0, H.tone(P["coat_l"], 1.1)), (1, H.tone(P["coat"], 0.82))]))
        H.bevel(f, flap, depth=0.22, gain=0.9, spec=0.2)
        H.trim_line(f, [(x(2.62), 0.0), (x(2.48), 0.7), (x(1.7), 1.78), (x(0.0), 1.78)], P["trim"], width=0.26, spec=0.4, outline=0.6)
        glow_edge(f, [(x(2.62), 0.0), (x(2.48), 0.7), (x(1.7), 1.78), (x(0.0), 1.78)], P, 0.2, 0.3)
        S.seam(f, [(x(2.0), 0.2), (x(1.5), 1.25), (x(0.2), 1.25)], P["trim_l"], 0.06, 0.5)
    H.shadow_under(f, 1.78, 0.5, 0.3)


def cravat(f, P):
    """A pink satin jabot with a pearl at the knot."""
    trim, tl = P["trim"], P["trim_l"]
    jab = f.m_poly([(2.75, 0.0), (5.25, 0.0), (5.55, 0.9), (5.0, 1.5), (5.45, 3.5), (4.0, 2.85), (2.55, 3.5), (3.0, 1.5), (2.45, 0.9)])
    f.fill(jab, H.lin((0, 0), (0, 3.5), [(0, H.tone(trim, 1.15)), (0.5, trim), (1, H.tone(trim, 0.7))]))
    f.fill(f.m_polyline([(4.0, 1.9), (4.0, 2.85)], 0.2) * jab, H.tone(trim, 0.55), 0.7)                   # the fold down the middle
    f.fill(f.m_polyline([(3.0, 1.6), (3.25, 3.1)], 0.22) * jab, tl, 0.55)                                 # sheen on the left lobe
    f.fill(f.m_polyline([(5.0, 1.6), (4.8, 3.0)], 0.18) * jab, H.tone(trim, 0.6), 0.5)
    f.fill(f.m_polyline([(2.55, 0.95), (3.0, 1.5), (5.0, 1.5), (5.45, 0.95)], 0.12), H.tone(trim, 0.5), 0.6)     # the lower edge of the neckband
    f.fill(f.m_polyline([(2.55, 3.45), (4.0, 2.85), (5.45, 3.45)], 0.18), H.tone(trim, 0.45), 0.75)
    knot = f.m_ellipse(4.0, 1.15, 0.95, 0.7)
    f.fill(knot, H.rad((3.75, 0.95), 1.2, [(0, tl), (0.5, trim), (1, H.tone(trim, 0.6))]))
    H.shadow_under(f, 3.45, 0.5, 0.26, x0=2.4, x1=5.6)
    H.stud(f, 4.0, 1.2, 0.33, "#fffaf2", rim_color="#b8a8c8", spec=0.95)                                  # the pearl


def brooch(f, cx, cy, P):
    """The crest of the Goat: a small pink shield with a dark rim, a light centre and two horns; it glows."""
    pts = [(cx - 0.8, cy - 0.85), (cx + 0.8, cy - 0.85), (cx + 0.8, cy + 0.15), (cx, cy + 0.95), (cx - 0.8, cy + 0.15)]
    f.fill(f.m_poly([(x * 1.0 + (0.08 if x > cx else -0.08), y + (0.1 if y > cy else -0.08)) for x, y in pts]), "#2a1230", 0.8)
    m = f.m_poly(pts)
    f.fill(m, H.lin((cx - 0.8, cy - 0.85), (cx + 0.8, cy + 0.95), [(0, H.tone(P["trim"], 1.3)), (0.5, P["trim"]), (1, H.tone(P["trim"], 0.62))]))
    H.bevel(f, m, depth=0.2, gain=1.1, spec=0.5)
    f.fill(f.m_polyline([(cx - 0.45, cy - 0.35), (cx - 0.2, cy + 0.05), (cx, cy + 0.35), (cx + 0.2, cy + 0.05), (cx + 0.45, cy - 0.35)], 0.17), "#4a1a50", 0.9)     # the horns of the Goat
    f.fill(f.m_ellipse(cx, cy + 0.12, 0.2, 0.2), "#fff4fc", 0.9)
    f.glow_fill(f.m_poly(pts, soft=3.0), P["glow"], 0.85)


def belts(f, P, front=False):
    S.belt_straps(f, P["belt"], P["belt_b"], 9.0, 11.0, split=10.05, stitch_color=P["trim"])
    f.fill(f.m_rect(0.0, 9.95, f.w, 10.1), P["trim"], 0.55)                                                # a pink line between the straps
    glow_edge(f, [(0.0, 10.02), (f.w, 10.02)], P, 0.16, 0.25)
    if front:
        for x in (1.1, 6.9):
            H.stud(f, x, 9.55, 0.2, P["trim"])
        f.ellipse(4.0, 10.02, 1.18, 1.2, "#2a1230", a=0.9)                                                 # the buckle: a pink ring with a gem
        f.ellipse(4.0, 10.0, 1.08, 1.1, H.metal_paint(8.9, 11.1, P["trim"], 1.05))
        f.ellipse(4.0, 10.0, 0.7, 0.72, "#3a1840")
        H.gem(f, 4.0, 10.0, 0.5, P["trim_l"], setting=None, facets=5, spec=0.9, glow=P["glow"])


def body_front(f, P, ov):
    coat, cl, cd, trim = P["coat"], P["coat_l"], P["coat_d"], P["trim"]
    S.panel(f, f.m_all(), coat, 1.28, 0.78, 0.08, folds=[0.9, 7.1], fold_depth=0.10, y0=1.8, y1=9.0)
    for x0, x1 in ((0.0, 2.3), (5.7, 8.0)):                                                          # the two coat panels: sheen and a lit edge
        pm = f.m_rect(x0, 1.5, x1, 9.0)
        H.sheen(f, pm, [x0 + 0.6 if x0 < 4 else x0 + 0.55], width=0.2, strength=0.2, tilt=0.05, y0=2.0, y1=8.6, color="#b8aee0")
        S.edge_light(f, pm, x0, x0 + 0.2, cl, 1.6, 0.35)
    # the shirt in the opening of the coat
    pl = f.m_poly([(2.55, 0.0), (5.45, 0.0), (5.85, 9.1), (2.15, 9.1)])
    H.cloth(f, pl, P["shirt"], top=1.08, bottom=0.88, round_=0.04, fold_xs=[3.3, 4.8], fold_depth=0.07, fold_y0=3.0, fold_y1=9.0, weave_k=0.0)
    f.shade(pl * np.clip(1.0 - np.minimum(f.X - (2.55 - (f.Y / 9.1) * 0.4), (5.45 + (f.Y / 9.1) * 0.4) - f.X) / 0.55, 0, 1), 0.74, 0.9)
    f.fill(f.m_line((4.0, 3.2), (4.0, 9.0), 0.17, 0.17) * pl, H.tone(P["shirt"], 0.58), 0.55)           # the placket
    f.fill(f.m_line((4.17, 3.2), (4.17, 9.0), 0.1, 0.1) * pl, H.tone(P["shirt"], 1.2), 0.5)
    for y in (4.5, 6.0, 7.5):
        H.stud(f, 4.0, y, 0.24, "#f7f0e0", rim_color="#9a8aae", spec=0.9)
    # piping along the opening of the coat
    for s, x0, x1 in ((-1.0, 2.5, 2.1), (1.0, 5.5, 5.9)):
        pts = [(x0, 1.7), (x1, 9.05)]
        H.trim_line(f, pts, trim, width=0.3, spec=0.4, outline=0.6)
        glow_edge(f, pts, P, 0.2, 0.3)
        S.seam(f, [(x0 + s * 0.42, 2.2), (x1 + s * 0.46, 9.0)], P["trim_l"], 0.06, 0.55)
    collar_front(f, P)
    cravat(f, P)
    brooch(f, 6.75, 4.0, P)
    if P["form2"]:                                                                                    # the light lines of the unbound doll run along the coat
        for pts in ([(6.0, 5.3), (6.3, 7.6), (6.1, 8.8)], [(1.7, 2.4), (1.4, 5.0), (1.6, 8.8)]):
            light_line(f, pts, P, 0.55)
    belts(f, P, True)
    # the coat tail under the belt: the two panels part at the middle
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(coat, 0.95)), (1, H.tone(coat, 0.8))]))
    f.fill(f.m_line((4.0, 11.0), (4.0, 12.0), 0.2, 0.2), H.tone(cd, 0.5), 0.9)
    f.fill(f.m_line((2.0, 11.1), (1.9, 12.0), 0.14, 0.14), H.tone(coat, 0.6), 0.5)
    f.fill(f.m_line((6.0, 11.1), (6.1, 12.0), 0.14, 0.14), H.tone(coat, 0.6), 0.5)
    H.shadow_under(f, 11.0, 0.5, 0.25)


def body_back(f, P, ov):
    coat, cl, cd, trim = P["coat"], P["coat_l"], P["coat_d"], P["trim"]
    S.panel(f, f.m_all(), coat, 1.26, 0.78, 0.08, folds=[1.4, 2.9, 5.1, 6.6], fold_depth=0.09, y0=1.8, y1=9.0)
    H.sheen(f, f.m_rect(0, 1.8, 8, 9.0), [1.5, 6.4], width=0.22, strength=0.18, tilt=0.04, y0=2.0, y1=8.6, color="#b8aee0")
    band = f.m_rect(0, 0, 8, 1.7)                                                                    # the back of the high collar
    f.fill(band, H.lin((0, 0), (0, 1.7), [(0, H.tone(cl, 1.1)), (1, H.tone(coat, 0.82))]))
    H.bevel(f, band, depth=0.22, gain=0.9, spec=0.2)
    H.trim_line(f, [(0.0, 1.72), (8.0, 1.72)], trim, width=0.26, spec=0.4, outline=0.6)
    glow_edge(f, [(0.0, 1.72), (8.0, 1.72)], P, 0.2, 0.3)
    H.shadow_under(f, 1.8, 0.5, 0.3)
    S.seam(f, [(4.0, 2.1), (4.0, 8.9)], P["trim_l"], 0.07, 0.55)                                    # the centre seam
    f.fill(f.m_line((4.0, 2.1), (4.0, 8.9), 0.17, 0.17), H.tone(cd, 0.8), 0.5)
    for x in (2.3, 5.7):                                                                             # darts
        f.fill(f.m_polyline([(x, 2.6), (x + (0.1 if x < 4 else -0.1), 5.2), (x, 8.8)], 0.14), H.tone(coat, 0.55), 0.5)
    if P["form2"]:                                                                                   # the mounts of the mechanical wings sit at the shoulder blades
        for cx in (2.15, 5.85):
            f.fill(f.m_ellipse(cx, 3.1, 1.15, 1.15), "#241f56", 0.95)
            f.fill(f.m_ring(cx, 3.1, 0.9, 0.32), H.metal_paint(2.2, 4.0, "#c8bde8", 1.1))
            f.fill(f.m_ring(cx, 3.1, 0.52, 0.12), H.tone(trim, 1.0), 0.9, glow=P["glow"])
            f.glow_fill(f.m_ellipse(cx, 3.1, 0.3, 0.3), P["glow"], 0.9)
    belts(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(coat, 0.95)), (1, H.tone(coat, 0.8))]))
    f.fill(f.m_line((4.0, 11.0), (4.0, 12.0), 0.2, 0.2), H.tone(cd, 0.5), 0.9)
    H.shadow_under(f, 11.0, 0.5, 0.25)


def body_side(f, P, name):
    coat, cl, trim = P["coat"], P["coat_l"], P["trim"]
    S.panel(f, f.m_all(), coat, 1.26, 0.78, 0.08, folds=[1.3, 2.7], fold_depth=0.08, y0=1.8, y1=9.0)
    band = f.m_rect(0, 0, f.w, 1.7)
    f.fill(band, H.lin((0, 0), (0, 1.7), [(0, H.tone(cl, 1.1)), (1, H.tone(coat, 0.82))]))
    H.bevel(f, band, depth=0.22, gain=0.9, spec=0.2)
    H.trim_line(f, [(0.0, 1.72), (f.w, 1.72)], trim, width=0.26, spec=0.4, outline=0.6)
    glow_edge(f, [(0.0, 1.72), (f.w, 1.72)], P, 0.2, 0.3)
    H.shadow_under(f, 1.8, 0.5, 0.3)
    edge = f.w - 0.7 if name == "west" else 0.7                                                       # the front edge of the coat, piped
    H.trim_line(f, [(edge, 2.0), (edge, 9.0)], trim, width=0.24, spec=0.35, outline=0.5)
    S.seam(f, [(f.w - edge, 2.4), (f.w - edge, 8.9)], P["trim_l"], 0.06, 0.4)
    belts(f, P)
    f.fill(f.m_rect(0.0, 11.0, f.w, 12.0), H.lin((0, 11.0), (0, 12.0), [(0, H.tone(coat, 0.95)), (1, H.tone(coat, 0.8))]))
    H.shadow_under(f, 11.0, 0.5, 0.25)


def body(cv, P):
    for nm in S.SIDES:
        for f, ov in zip(S.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, P, ov)
            elif nm == "back":
                body_back(f, P, ov)
            else:
                body_side(f, P, nm)


# ================================================================================================ the arms
def glove(f, P, name, y0=9.3):
    """A white glove: the cuff turns over it, soft lilac shadows, knuckles and finger lines."""
    g = f.m_band(y0, 12.0)
    f.fill(g, H.lin((0, y0), (0, 12.0), [(0, H.tone(P["glove"], 0.9)), (0.4, P["glove"]), (1, H.tone(P["glove"], 0.82))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(g, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
    H.sheen(f, g, [f.w * 0.4], width=0.3, strength=0.12, tilt=0.0, y0=y0 + 0.4, y1=11.6, color="#ffffff")
    H.shadow_under(f, y0, 0.7, 0.22, mask=g)
    if name in ("front", "back"):
        for x in (f.w * 0.3, f.w * 0.5, f.w * 0.7):
            f.fill(f.m_line((x, 10.9), (x, 11.9), 0.18, 0.12) * g, "#b7a9cf", 0.7)
        f.fill(f.m_band(10.75, 10.95) * g, "#b7a9cf", 0.5)
    else:
        f.fill(f.m_line((0.6, 10.2), (1.6, 10.7), 0.2, 0.14) * g, "#b7a9cf", 0.6)                      # the thumb crease
    f.fill(f.m_band(11.75, 12.0) * g, "#b7a9cf", 0.5)


def arm(cv, part, P):
    coat, trim = P["coat"], P["trim"]
    for nm in S.SIDES:
        for f, ov in zip(S.pair(cv, part, nm), (False, True)):
            sl = f.m_band(0.0, 9.3)
            S.panel(f, sl, coat, 1.26, 0.8, 0.12, folds=[f.w * 0.3, f.w * 0.72], fold_depth=0.08, y0=0.8, y1=8.4)
            H.sheen(f, sl, [f.w * 0.3], width=0.2, strength=0.18, tilt=0.03, y0=1.0, y1=8.0, color="#b8aee0")
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), H.tone(coat, 0.7), 0.7)                               # the shoulder seam
            S.seam(f, [(0.0, 0.62), (f.w, 0.62)], P["trim_l"], 0.055, 0.35, (0.3, 0.2), 0.15)
            H.shadow_under(f, 8.0, 0.9, 0.0)
            glove(f, P, nm)
            H.trim_band(f, 8.0, 9.35, trim, spec=0.45, outline=0.6)                                    # the cuff, piped in pink
            S.seam(f, [(0.12, 8.7), (f.w - 0.12, 8.7)], P["trim_l"], 0.06, 0.6, (0.3, 0.18), 0.2)
            glow_edge(f, [(0.0, 8.1), (f.w, 8.1)], P, 0.16, 0.25)
            H.shadow_under(f, 9.35, 0.5, 0.3)
            if P["form2"] and nm in ("front", "west", "east") and part == "arm_r" and nm != "east":
                light_line(f, [(f.w * 0.5, 1.4), (f.w * 0.55, 4.0), (f.w * 0.5, 7.3)], P, 0.5)
            if P["form2"] and nm in ("front", "east", "west") and part == "arm_l" and nm != "west":
                light_line(f, [(f.w * 0.5, 1.4), (f.w * 0.45, 4.0), (f.w * 0.5, 7.3)], P, 0.5)


def arms(cv, P):
    arm(cv, "arm_r", P)
    arm(cv, "arm_l", P)


# ================================================================================================ the legs
def leg(f, P, name):
    coat, trim = P["coat"], P["trim"]
    tail = f.m_band(0.0, 6.0)
    S.panel(f, tail, coat, 1.2, 0.8, 0.12, folds=[1.1, 2.9], fold_depth=0.10, y0=0.4, y1=5.8)
    H.sheen(f, tail, [f.w * 0.3], width=0.2, strength=0.15, tilt=0.03, y0=0.5, y1=5.4, color="#b8aee0")
    H.trim_band(f, 5.55, 6.2, trim, spec=0.4, outline=0.6)                                              # the hem of the coat tail
    S.seam(f, [(0.12, 5.2), (f.w - 0.12, 5.2)], P["trim_l"], 0.055, 0.45, (0.3, 0.18), 0.2)
    glow_edge(f, [(0.0, 5.62), (f.w, 5.62)], P, 0.16, 0.25)
    H.shadow_under(f, 6.2, 0.9, 0.36)                                                                   # the tail hangs over the boot
    boot = f.m_band(6.2, 11.2)
    f.fill(boot, H.lin((0, 6.2), (0, 11.2), [(0, H.tone(P["boot"], 1.5)), (0.45, P["boot"]), (1, H.tone(P["boot"], 0.7))]))
    cuff = f.m_band(6.2, 7.5)                                                                           # the turned boot cuff, pink
    f.fill(cuff, H.metal_paint(6.2, 7.5, trim, 0.9))
    H.bevel(f, cuff, depth=0.32, gain=1.1, spec=0.4)
    f.fill(f.m_band(7.4, 7.55), H.tone(trim, 0.4), 0.8)
    H.shadow_under(f, 7.5, 0.7, 0.32)
    H.sheen(f, f.m_band(7.7, 11.0), [f.w * 0.3], width=0.2, strength=0.3, tilt=0.04, y0=7.8, y1=10.6, color="#a79ad0")
    f.fill(f.m_polyline([(0.0, 9.6), (f.w * 0.5, 9.85), (f.w, 9.65)], 0.11), H.tone(P["boot"], 0.5), 0.7)    # ankle wrinkles
    if name == "front":                                                                                  # the strap with its buckle, and the toe cap
        f.rect(0.0, 8.5, f.w, 9.2, P["strap"], r=0.0)
        f.fill(f.m_rect(0.0, 8.5, f.w, 8.64), H.tone(trim, 1.0), 0.45)
        H.stud(f, f.w * 0.5, 8.85, 0.3, trim)
        f.ellipse(f.w * 0.5, 10.35, 1.2, 0.65, H.tone(P["boot"], 1.9), a=0.35, soft=2.0)
    S.sole(f, 11.0, 12.0, welt="#6a5a8a")
    if name in ("west", "east"):
        S.seam(f, [(f.w * 0.5, 0.3), (f.w * 0.5, 5.4)], P["trim_l"], 0.055, 0.3)


def legs(cv, P):
    for part in ("leg_r", "leg_l"):
        for nm in S.SIDES:
            for f in S.pair(cv, part, nm):
                leg(f, P, nm)


# ================================================================================================ paint
def paint_form(spec, cid, masks, P):
    spec["hair"]["color"] = P["hair"]                                                               # the scalp and the 3D hair share one colour
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=P["style"])
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, P["hair_rgb"], skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    cov = S.covers(cv)
    head_front(cv, P)
    head_sides(cv, P)
    body(cv, P)
    arms(cv, P)
    legs(cv, P)
    S.hold_cover(cv, cov)
    skin, glow = cv.finish()
    return skin, glow, hair, P["hair_rgb"], slim
