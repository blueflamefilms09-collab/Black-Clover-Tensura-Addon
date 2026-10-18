"""art_hart3: the 3D models, skins, glow skins, sprites and item models of the Vassal Scythe, Katana, Fan, Carriage and Book (1.62.0, the Rising of the Shield Hero).

    python3 -B tools/heroweapon/art_hart3.py [id ...]          (no ids = all five, in the order scythe, katana, fan, carriage, book; idempotent, deterministic)
    HART3_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart3.py vassal_scythe      (also renders the preview sheets of what was written)

Why: the owner's rule 7 (the repo's rules file) says every weapon is a real 3D model in hand built to the Genesis Demon-Slayer's standard, keeps its old sprite as the
inventory icon, and has glow / effects that fit it. design.md section 11 lists the files; tests/HeroAssetsTest names every missing one. This script is the
whole art of five Vassal weapons, written with the owner's model method (tools/relics/mesh_lib.py, worked example tools/relics/wave23_models.py):
  0 the target is decided and written in the weapon's docstring (what the series shows, what is from memory, what is design);
  1 every proportion is a NAMED CONSTANT with a comment saying where it came from (the class table, the series, a real object);
  2 the geometry is built from real cross-sections (loft / sweep / lathe), never from boxes for a long form;
  3 each small raised part is its own named Part;
  4 normals are smoothed by angle per part, UVs are projected on the dominant axis, one 512 px skin per weapon;
  5 painted material plus a second additive glow skin (the gem is on the glow skin, in the weapon's colour);
  6 rendered, looked at and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.

For every id it writes
  assets/nusmp/textures/item/<id>.png                 the 16 x 16 sprite (the inventory icon), drawn by hand in art_hart3_sprites.py
  assets/nusmp/weapon_meshes/<id>.txt                 the mesh WeaponRenderer draws (per-vertex smoothed normals, the part list as comments)
  assets/nusmp/textures/entity/weapon/<id>.png        the painted skin, and <id>_glow.png the additive glow skin
  assets/nusmp/models/item/<id>.json and <id>_icon.json   the in-hand model (builtin/entity) and the flat inventory model
and tools/heroweapon/classes_hart3.json (the size class of each id, for the lead to merge into tools/relics/weapon_classes.json).

Axes (docs/weapon_scale_orientation.md): y runs along the weapon, the grip low and the head high; mesh -x is FORWARD (the cutting edge, the blade of a scythe,
the open side of a carriage); x is across, z through the weapon (+z toward the viewer in the profile view). Units are centimetres of the model: the scale
into the game is derived from the sprite and the class length (place_diagonal / place_upright below).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import art_hart3_lib as L                                               # noqa: E402
import art_hart3_sprites as sprites                                     # noqa: E402

ml = L.ml
w = L.w
V = L.v3

CLASSES = {}                                                            # id -> (class, why): written to classes_hart3.json


# ================================================================ placement, writing, previews
def _sprite_extent(icon_png):
    """(butt_px, top_px, length): the opaque sprite pixels farthest down-left and up-right along the 45 degree diagonal, and the diagonal's length in item units."""
    a = np.asarray(Image.open(icon_png).convert("RGBA"))
    ys, xs = np.nonzero(a[..., 3] > 40)
    s = ((xs + 0.5) + (15.5 - ys)) / 16.0 / math.sqrt(2.0)             # position along the diagonal, in item units
    lo, hi = int(np.argmin(s)), int(np.argmax(s))
    return (int(xs[lo]), int(ys[lo])), (int(xs[hi]), int(ys[hi])), float(s[hi] - s[lo])


def place_diagonal(m, icon_png, real_m, grip, hand_scale=0.85):
    """Lays the mesh on the 16 x 16 sprite's diagonal like mesh_lib.place_on_sprite (the butt on the sprite's lower left end, the head on its upper right
    end, the axis at 45 degrees, which every display transform of the mod is tuned for), but with the axis fixed at 45 degrees instead of the principal axis
    of the opaque pixels: a hooked scythe or a spread fan has a lopsided head that would tilt that axis. real_m is the length in blocks in the third person
    hand (the class table), the mesh is grown about the point `grip` (a share of the length from the butt) so the hand stays where it is."""
    (bx, by), (tx, ty), length = _sprite_extent(icon_png)
    b = np.array([(bx + 0.5) / 16.0, 1.0 - (by + 0.5) / 16.0])
    d = np.array([math.cos(math.radians(45.0)), math.sin(math.radians(45.0))])
    ymin, ymax = m.y_range()
    scale = length / max(ymax - ymin, 1e-6)
    kk = real_m / (hand_scale * length)
    origin = b + d * (grip * length) * (1 - kk)
    m.place(float(origin[0]), float(origin[1]), 45.0, scale * kk, ymin)
    return m.place_cfg


def write_class(item_id, cls, why):
    CLASSES[item_id] = (cls, why)


def preview_dir():
    return os.environ.get("HART3_PREVIEW")


def place_flat(m, length_blocks):
    """Stands a mesh whose long axis is y and whose up is -x (the carriage's space) upright in the item cube: the nose to the right (place angle 0), the roof
    up, the ground line (x = 0) at the foot, the whole thing centred, `length_blocks` long."""
    lo, hi = m.bbox()
    ymin, ymax = float(lo[1]), float(hi[1])
    sc = length_blocks / (ymax - ymin)
    height = -float(lo[0]) * sc                                      # roof to ground, in item units
    m.place(0.5 - length_blocks / 2, 0.5 - height / 2, 0.0, sc, ymin)
    return m.place_cfg


def finish_weapon(item_id, m, atlas, glow, upright=False, third_scale=None, display=None):
    """Writes the mesh and skins and the item models, prints the budget line and (with HART3_PREVIEW) renders the preview of the written files."""
    n = L.write_mesh(m, atlas, item_id, glow=glow)
    L.write_item_models(item_id, upright, third_scale=third_scale, display=display)
    print(L.stats_line(item_id, m, atlas) + "; %d quad lines written" % n)
    pv = preview_dir()
    if pv:
        L.preview_files(item_id, os.path.join(pv, item_id + ".png"))
    return n


# ================================================================ Vassal Scythe
# The target. L'Arc Berg's weapon, the Scythe Hero of Glass' world. From the series' wiki summaries (shield-hero.fandom.com/wiki/Vassal_Scythe and
# /wiki/L%27Arc_Berg_Sickle): its swing throws a spinning disc of energy, it can float a second scythe, and Naofumi made it a plume accessory; a hedged
# prop listing describes a black scythe with a curved blade and a spiked disc at the foot of the handle. Everything else is design with intent, following the
# logic builder's page (weapon_scythe.md of the hvc worktree): a noble reaper's weapon, a black lacquer shaft with silver bands and a crimson wrap over the grip,
# a bright steel blade with a darker spine and a bright polished edge, a bronze socket and medallion with the amethyst at the root of the blade (the series puts
# the weapon's jewel there on the back of the hand), the spiked disc of the listing as a crowned butt on a steel spike, the plume as a small tassel of feathers
# under the cuff, and a violet line along the cutting edge that is the glow skin's share of the "spinning disc".
# Class: scythe (2.0 to 2.6 blocks apparent, head at least 0.6 across, grip 0.30); a legendary weapon, so the upper half of the range.
SCY_REAL_M = 2.35                  # apparent length in the third person hand: the scythe class is 2.0 to 2.6, a legendary weapon takes the upper half
SCY_GRIP = 0.30                    # the class table's grip share: the hand stays where it is when the weapon grows
SCY_SNATH_Y0, SCY_SNATH_Y1 = 26.0, 206.0      # the pole runs from the spiked disc to the cuff (180 cm of a 240 cm weapon: a real snath is 150 to 170 cm of 220)
SCY_SNATH_R0, SCY_SNATH_R1 = 2.0, 1.75         # pole radius at the foot and under the cuff (a 4 cm pole, as a real snath is 3.5 to 4.5 cm through)
SCY_SNATH_BOW = 1.5                            # a real snath is S-curved; the pole bows this far (cm) either side of its axis
SCY_DISC_Y, SCY_DISC_R, SCY_DISC_T = 24.0, 8.4, 1.9     # the spiked wheel at the handle's end (the listing's "spiked disc"): height, radius, half thickness
SCY_DISC_SPIKES, SCY_DISC_SPIKE_L = 8, 7.0              # eight thorns round the rim
SCY_CUFF_Y0 = 184.0                            # the bronze socket the blade sits in starts here (28 cm tall)
SCY_BLADE_ROOT = (0.0, 213.0)                  # where the centre line of the blade crosses the pole's axis
SCY_BLADE_TAIL = (13.0, 211.0)                 # the heel: a spike that reaches back behind the pole (the reaper's counter-blade)
SCY_BLADE_TIP = (-88.0, 164.0)                 # the point: 88 cm forward of the pole and 49 cm below the root (the hook of a reaper's blade, about a third of 240)
SCY_BLADE_W = ((0.0, 0.0), (0.06, 12.0), (0.12, 21.0), (0.26, 25.0), (0.50, 19.0), (0.75, 10.5), (0.92, 4.2), (1.0, 0.0))   # width spine to edge along the blade (cm)
SCY_BLADE_T0, SCY_BLADE_T1 = 2.4, 0.8          # thickness at the heel and at the point
SCY_GEM_R, SCY_GEM_HALF = 5.4, 7.6             # the amethyst: girdle radius and how far its points reach out of the plate's middle on each side
SCY_GEM_AT = (-3.0, 211.5)                     # in the heel medallion, at the root of the blade
SCY_PLATE_R, SCY_PLATE_T = 8.6, 2.0            # the bronze medallion the stone is set in: radius and half thickness
SCY_HALO_R = 14.0                              # the ring of light round the jewel (the spinning disc of the swing); glow skin only
SCY_COL = dict(black=(30, 24, 42), steel=(178, 190, 210), spine=(109, 116, 130), bronze=(168, 130, 66), silver=(194, 202, 216), crimson=(110, 27, 43), amethyst=(104, 44, 190), cloth=(84, 40, 128))
SCY_GLOW = (150, 78, 255)                      # the amethyst's light
SCY_FEATHERS = ((-3.4, 195.0, 1.7, -0.30, 28.0), (-3.2, 195.0, 0.0, -0.08, 22.0), (-3.0, 195.0, -1.7, 0.16, 17.0))     # plume: x, y, z of the root, lean, length


def _scy_snath_x(y):
    """The pole's centre line: a long shallow S (a scythe's snath is bent so the wielder's hands sit away from the blade's plane)."""
    t = (y - SCY_SNATH_Y0) / (SCY_SNATH_Y1 - SCY_SNATH_Y0)
    return SCY_SNATH_BOW * math.sin(2 * math.pi * t) * (1 - t * 0.6)


def _scy_blade_centre():
    """The centre line of the blade as a polyline: the heel spike, the root over the pole, then a long forward sweep that hooks down to the point."""
    heel = L.bezier(SCY_BLADE_TAIL, (8.0, 212.0), (4.0, 213.0), SCY_BLADE_ROOT, 6)
    sweep = L.bezier(SCY_BLADE_ROOT, (-30.0, 230.0), (-80.0, 224.0), SCY_BLADE_TIP, 40)
    return L.to3(heel[:-1] + sweep)


def build_vassal_scythe():
    item_id = "vassal_scythe"
    icon = sprites.write(item_id, L.ICON_DIR)
    m = ml.Mesh(item_id)
    cl = L.Centreline(_scy_blade_centre())
    wf = w.lerp_table(SCY_BLADE_W)

    def thick(t):
        return SCY_BLADE_T0 + (SCY_BLADE_T1 - SCY_BLADE_T0) * t
    # ---- the snath: a black lacquered pole, bowed, a little thicker where the hands go
    sn = L.add(m, "snath", "obsidian", base=SCY_COL["black"], smooth=60, uv_mode="loft", wear=0.3)
    ys = np.linspace(SCY_SNATH_Y0 - 3.0, SCY_SNATH_Y1 + 4.0, 19)
    L.tube(sn, [(_scy_snath_x(y), y, 0.0) for y in ys], lambda t: SCY_SNATH_R0 + (SCY_SNATH_R1 - SCY_SNATH_R0) * t + 0.18 * math.sin(math.pi * t), sides=8, up=(0, 0, 1))
    # ---- the grip wraps (two hand zones, the lower one at the class grip 0.30 of 240 cm) and the silver rings that bound them
    gw = L.add(m, "grip_wraps", "leather", base=SCY_COL["crimson"], smooth=50, uv_mode="loft", wear=0.6)
    gr = L.add(m, "snath_rings", "silver", base=SCY_COL["silver"], smooth=45, wear=0.5)
    for (ya, yb) in ((52.0, 94.0), (122.0, 150.0)):
        zs = np.linspace(ya, yb, 4)
        L.tube(gw, [(_scy_snath_x(y), y, 0.0) for y in zs], 2.35, sides=8, up=(0, 0, 1))
        for y in (ya - 1.6, yb):
            ml.collar(gr, y, 2.9, 1.6, lip=0.35, sides=8, axis="y", origin=(_scy_snath_x(y), 0, 0), caps=(False, False))
    for y in (110.0, 172.0):
        ml.collar(gr, y, 2.6, 1.3, lip=0.3, sides=8, axis="y", origin=(_scy_snath_x(y), 0, 0), caps=(False, False))
    # ---- the butt: a long spike, then the crowned wheel of ten thorns
    bt = L.add(m, "butt_cone", "steel", base=(176, 186, 204), smooth=45, wear=0.6)
    ml.lathe(bt, "y", (0, SCY_SNATH_Y0 - 2.0, 0), [(-SCY_SNATH_Y0 + 2.0, 0.0), (-SCY_SNATH_Y0 + 8.0, 0.9), (-10.0, 1.5), (-5.0, 2.3), (-2.0, 3.0), (0.0, 2.2), (2.0, 2.0)], sides=8)
    bd = L.add(m, "butt_disc", "obsidian", base=(36, 28, 50), smooth=40, wear=0.4)
    ml.lathe(bd, "y", (0, SCY_DISC_Y, 0), [(-SCY_DISC_T, 2.4), (-SCY_DISC_T * 0.4, SCY_DISC_R * 0.92), (0.0, SCY_DISC_R), (SCY_DISC_T * 0.4, SCY_DISC_R * 0.92), (SCY_DISC_T, 2.4)], sides=16)
    for k in range(SCY_DISC_SPIKES):
        a = 2 * math.pi * k / SCY_DISC_SPIKES
        base = (SCY_DISC_R * math.cos(a) * 0.95, SCY_DISC_Y, SCY_DISC_R * math.sin(a) * 0.95)
        L.spike(bd, base, (math.cos(a), -0.30, math.sin(a)), SCY_DISC_SPIKE_L, 1.3, sides=4)
    # ---- the cuff: the bronze socket under the blade
    cf = L.add(m, "cuff", "bronze", base=SCY_COL["bronze"], smooth=45, wear=0.5)
    ml.lathe(cf, "y", (_scy_snath_x(SCY_CUFF_Y0), SCY_CUFF_Y0, 0), [(0.0, 2.5), (1.5, 3.4), (6.0, 3.2), (9.0, 3.9), (12.0, 3.1), (18.0, 3.1), (24.0, 2.4), (28.0, 2.0)], sides=12)
    # ---- the blade: one-edged section, width and thickness tapering from the heel to the point, the edge on the inside of the hook
    bl = L.add(m, "blade", "steel", base=SCY_COL["steel"], smooth=18, uv_mode=None, wear=0.35)
    sec = L.single_edge(bevel=0.34, spine_round=0.12, spine_flat=0.6)

    def blade_scales(t):
        if t >= 0.995 or t <= 0.003:
            return (0.0, 0.0)
        return (wf(t), thick(t))
    ml.sweep(bl, L.resample_path(_scy_blade_centre(), 32), sec, scales=blade_scales, up=(0, 0, 1))
    # ---- the darker steel cap along the spine, tapering out at both ends
    sb = L.add(m, "blade_spine_band", "steel", base=SCY_COL["spine"], smooth=40, uv_mode="loft", wear=0.4)
    ts = np.linspace(0.10, 0.94, 18)
    spine = [tuple(list(cl.at(t, 0.5 * wf(t) - 0.15)) + [0.0]) for t in ts]
    ml.sweep(sb, spine, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.5 * math.sin(math.pi * min(1.0, t * 1.0)) ** 0.35, thick(0.10 + 0.84 * t) + 1.0), up=(0, 0, 1))
    # ---- the violet line along the cutting edge on both faces (the glow skin of the blade)
    rn = L.add(m, "blade_runes", "crystal", base=(150, 84, 235), glow=True, smooth=0, uv_mode="loft")
    for sgn in (1, -1):
        ts2 = np.linspace(0.14, 0.90, 16)
        line = [tuple(list(cl.at(t, -0.20 * wf(t))) + [sgn * (thick(t) / 2 + 0.05)]) for t in ts2]
        ml.sweep(rn, line, ml.lozenge(1.0, 1.0), scales=lambda t: (1.3 * (1 - 0.75 * t) if t < 0.97 else 0.0, 0.5 if t < 0.97 else 0.0), up=(0, 0, 1))
    # ---- the bronze heel medallion with the amethyst set through it (it shows on both faces)
    gx, gy = SCY_GEM_AT
    hp = L.add(m, "heel_plate", "bronze", base=SCY_COL["bronze"], smooth=40, wear=0.5)
    ml.lathe(hp, "z", (gx, gy, 0), [(-SCY_PLATE_T, 5.6), (-SCY_PLATE_T * 0.7, SCY_PLATE_R * 0.9), (-0.5, SCY_PLATE_R), (0.5, SCY_PLATE_R), (SCY_PLATE_T * 0.7, SCY_PLATE_R * 0.9), (SCY_PLATE_T, 5.6)], sides=16)
    gs = L.add(m, "gem_setting", "bronze", base=(204, 164, 84), smooth=50, wear=0.4)
    for sgn in (1, -1):
        L.torus(gs, (gx, gy, sgn * (SCY_PLATE_T + 0.15)), 4.6, 0.8, (0, 0, 1), n=14, sides=5)
        for k in range(6):
            a = 2 * math.pi * k / 6 + 0.26
            L.spike(gs, (gx + 4.7 * math.cos(a), gy + 4.7 * math.sin(a), sgn * (SCY_PLATE_T + 0.1)), (-0.28 * math.cos(a), -0.28 * math.sin(a), sgn * 1.0), 2.4, 0.6, sides=3, r_tip=0.2)
    gem = L.add(m, "gem", "crystal", base=SCY_COL["amethyst"], glow=True, smooth=0)
    L.bicone(gem, (gx, gy, 0.0), SCY_GEM_R, SCY_GEM_HALF, sides=6, axis="z")
    # ---- the spinning disc of the swing, as a halo that only the glow skin draws: a thin ring round the jewel with twelve ticks on it
    hl = m.add(ml.Part("disc_halo", layer=2, material="crystal", base=(150, 84, 235), smooth=0, uv_mode="loft"))
    for sgn in (1, -1):                                                     # one ring on each face, just outside the blade's skin, so each shows from its own side
        zz = sgn * (SCY_BLADE_T0 / 2 + 0.45)
        L.torus(hl, (gx, gy, zz), SCY_HALO_R, 0.4, (0, 0, 1), n=24, sides=3)
        L.torus(hl, (gx, gy, zz), SCY_HALO_R * 0.80, 0.24, (0, 0, 1), n=20, sides=3)
        for k in range(8):
            a = 2 * math.pi * k / 8 + math.pi / 8
            ml.loft(hl, (math.cos(a), math.sin(a), 0.0), (gx + SCY_HALO_R * 0.80 * math.cos(a), gy + SCY_HALO_R * 0.80 * math.sin(a), zz),
                    [(0.0, 1.0, 1.0), (SCY_HALO_R * 0.20, 1.0, 1.0)], ml.rect_ring(0.5, 0.3), caps=(False, False), up=(0, 0, 1))
    # ---- the plume: three feathers of dark violet cloth under the cuff (the accessory Naofumi made)
    pl = L.add(m, "plume", "cloth", base=SCY_COL["cloth"], smooth=20, wear=0.2)
    for (px, py, pz, lean, ln) in SCY_FEATHERS:
        ml.bar(pl, (px, py, pz), (px + lean * ln, py - ln, pz + 0.25 * lean * ln), 3.4, 0.2, up=(0, 0, 1), taper=ln * 0.55, end_scale=0.06)

    atlas = L.lay(m)
    _paint_scythe(m, atlas, cl, wf)
    place_diagonal(m, icon, SCY_REAL_M, SCY_GRIP)
    write_class(item_id, "scythe", "a tall scythe: the class length 2.0 to 2.6 blocks, head at least 0.6 across (the hook is 0.9)")
    finish_weapon(item_id, m, atlas, glow=("FFFFFF", 0.12, 0.4, 1.0))


def _paint_scythe(m, atlas, cl, wf):
    """The skin of the scythe: lacquer ticks on the pole, the lacing of the wraps, the polished edge and the engraved feathers of the blade, the lit amethyst."""
    def pole(c):
        S, B = c.S, c.B
        line = np.exp(-(((np.abs(S - 0.25)) / 0.014) ** 2)) + np.exp(-(((np.abs(S - 0.75)) / 0.014) ** 2))
        tick = ((B % 9.0) < 0.55) & ((np.abs(S - 0.25) < 0.07) | (np.abs(S - 0.75) < 0.07))
        a = np.clip(line * 0.55 + tick * 0.9, 0, 1)
        L.blend(c.img, (96, 84, 130), a * 0.55)                          # a sheen on the lacquer, no light: the page asks for nothing glowing on the shaft
    w.paint_part(atlas, m.part("snath"), pole)

    def wraps(c):
        thread = ((c.A * 0.9 + c.B * 0.9) % 1.7) < 0.32
        L.blend(c.img, (196, 82, 98), thread * 0.7)
    w.paint_part(atlas, m.part("grip_wraps"), wraps)

    def blade(c):
        if c.kind != "dom" or c.g.key[0] != 2:
            return
        X, Y, _ = L.model_coords(c)
        t, u, d = cl.locate(X, Y)
        shape = c.img.shape[:2]
        t, u = t.reshape(shape), u.reshape(shape)
        uf = u / np.maximum(wf(np.clip(t, 0.0, 1.0)) if False else np.vectorize(wf)(np.clip(t, 0.0, 1.0)), 0.5)
        bevel = L.smoothstep(-0.16, -0.5, uf)                           # 0 on the flat, 1 at the edge: the ground bevel
        lum = 0.82 + 0.28 * L.smoothstep(0.35, -0.5, uf) + 0.10 * np.sin(t * 40.0 + uf * 2.0)
        c.img[..., :3] = np.clip(c.img[..., :3] * lum[..., None], 0, 255)
        L.blend(c.img, (214, 220, 240), bevel * 0.85)                   # the polished cutting edge
        L.blend(c.img, (250, 252, 255), L.smoothstep(-0.46, -0.5, uf) * 0.7)
        L.blend(c.img, (74, 82, 102), np.exp(-(((uf - 0.30) / 0.03) ** 2)) * ((t > 0.11) & (t < 0.93)) * 0.65)      # a fine groove along the back of the blade
        w.set_glow(c, SCY_GLOW, 0.55, mask=np.exp(-(((uf + 0.465) / 0.032) ** 2)) * ((t > 0.08) & (t < 0.97)) * 0.9, core=0.2)     # the violet line along the cutting edge
    w.paint_part(atlas, m.part("blade"), blade)

    def feathers(d):                                                    # engraved feather strokes between the rune line and the back
        for k in range(11):
            t0 = 0.17 + 0.068 * k
            a = cl.at(t0, 0.30 * wf(t0))
            b = cl.at(t0 + 0.055, 0.04 * wf(t0 + 0.055) + 0.2)
            d.line([tuple(a), tuple(b)], 0.55, rgb=(70, 78, 104, 230))
            d.line([tuple(b + (0.0, 0.5)), tuple(a + (0.0, 0.5))], 0.2, rgb=(236, 242, 255, 170))
            d.line([tuple(a), tuple(b)], 0.18, glow=(160, 90, 255, 150))
    for sgn in (1, -1):
        L.decal(atlas, m.part("blade"), "z", sgn, feathers)
    L.paint_gem(atlas, m.part("gem"), SCY_COL["amethyst"], SCY_GLOW, strength=0.85, core=0.08)
    L.glow_part(atlas, m.part("blade_runes"), SCY_GLOW, 0.8, 0.05)
    L.glow_part(atlas, m.part("disc_halo"), SCY_GLOW, 0.55, 0.05)


# ================================================================ Vassal Katana
# The target. The Vassal Katana of Glass' world (candidate: Raphtalia; its first form, Katana 0, looks like the plain Beginner's Katana: wiki summary). The
# series gives no more, so this is design with intent, following the logic builder's page (weapon_katana.md of the hvc worktree): a real katana in the proportions
# of a real one (blade 72 cm, hilt 27 cm, a 4.3 cm radius guard, the tip bent toward the edge, a hardened-edge line along the blade) made noble and ornate as a
# Vassal weapon must be: a cherry-blossom guard (the five-petal sakura tsuba in dark iron, petals inlaid in gold and pierced), a gold collar and washers, an
# indigo diamond-wrapped hilt over white ray skin with two blossom menuki, and the weapon's jewel, a sakura-pink stone, set through the gold collar at the centre
# of the guard where it shows from the wielder's side, with a second small one as the bead at the end of the pommel. The hardening line and the petal openings
# glow faintly pink.
# Class: katana (1.4 to 1.8 blocks apparent, grip 0.20); a legendary weapon, so the upper half of the range.
KAT_REAL_M = 1.66
KAT_GRIP = 0.20
KAT_NAGASA = 72.0                  # blade length (a real katana blade is 60 to 73 cm)
KAT_Y_BASE = 30.6                  # where the blade leaves the habaki: pommel end 0, hilt 27 cm, guard, collar
KAT_W0, KAT_W1 = 3.1, 2.3          # blade width at the base and at the yokote (real: about 3.0 and 2.1 cm)
KAT_T0, KAT_T1 = 0.78, 0.55        # blade thickness (kasane) at the base and at the yokote (real: 0.7 and 0.5)
KAT_KISSAKI = 4.6                  # length of the point
KAT_SORI = 3.0                     # the tip is bent this far toward the edge, -x (real sori is 1 to 2.5 cm; a little more reads in the hand)
KAT_TSUBA_R, KAT_TSUBA_T = 4.3, 0.38      # the guard: radius (real 3.5 to 4.2) and half thickness (real 0.3 to 0.5)
KAT_Y_TSUBA = 29.9
KAT_TSUKA_Y0, KAT_TSUKA_Y1 = 1.2, 28.2    # the wrapped hilt (27 cm, a two handed grip)
KAT_COL = dict(steel=(176, 188, 208), iron=(43, 43, 51), gold=(205, 165, 76), ito=(35, 58, 107), same=(233, 230, 221), pink=(196, 66, 124))
KAT_GLOW = (255, 124, 178)         # the sakura pink stones' light
KAT_GEM_Y = KAT_Y_BASE + 2.3       # the main stone sits through the gold collar, a little above the guard
KAT_GEM_R = 1.15                   # its girdle radius (the page: about 0.04 block)


def _kat_spine_x(s):
    """x of the blade's spine at s (0 base .. 1 tip): a gentle curve whose tip bends toward the edge (-x), deepest toward the point."""
    return KAT_W0 / 2 - KAT_SORI * (s ** 1.7)


def _kat_width(s):
    """The blade's width at s: tapering to the yokote, then the point (an arc that meets the spine)."""
    tk = 1.0 - KAT_KISSAKI / KAT_NAGASA
    base = KAT_W0 + (KAT_W1 - KAT_W0) * min(1.0, s / tk)
    if s <= tk:
        return base
    return KAT_W1 * max(0.0, 1.0 - ((s - tk) / (1.0 - tk)) ** 1.5)


def _kat_sakura(R, notch=0.12):
    """r(theta) of the five petal guard: five petals whose centres lie at 90 degrees + k * 72, rounded, a sharp valley between them and a small notch at
    each tip (the cherry petal)."""
    def r(th):
        d = ((th - math.pi / 2 + math.pi / 5) % (2 * math.pi / 5)) - math.pi / 5            # signed angle to the nearest petal centre, -36 .. +36 degrees
        petal = max(0.0, math.cos(2.5 * d)) ** 0.8
        return R * (0.66 + 0.34 * petal - notch * math.exp(-(d / 0.10) ** 2))
    return r


def build_vassal_katana():
    item_id = "vassal_katana"
    icon = sprites.write(item_id, L.ICON_DIR)
    m = ml.Mesh(item_id)
    # ---- the blade: the path is the spine (mune), the section hangs off it toward the edge, so the point lies on the spine line like a real kissaki
    bl = L.add(m, "blade", "steel", base=KAT_COL["steel"], smooth=6, uv_mode=None, wear=0.25)
    ss = list(np.linspace(0.0, 0.92, 24)) + [0.945, 0.96, 0.972, 0.983, 0.992, 1.0]
    path = [(_kat_spine_x(s), KAT_Y_BASE - 1.0 + (KAT_NAGASA + 1.0) * s, 0.0) for s in ss]
    sc = []
    for s in ss:
        wd, th = _kat_width(s), KAT_T0 + (KAT_T1 - KAT_T0) * min(1.0, s / 0.94)
        sc.append((0.0, 0.0) if s >= 0.9999 else (wd, th))
    sec = [(-1.0, 0.0), (-0.70, 0.30), (-0.40, 0.5), (-0.04, 0.42), (0.0, 0.18), (0.0, -0.18), (-0.04, -0.42), (-0.40, -0.5), (-0.70, -0.30)]
    ml.sweep(bl, path, sec, scales=sc, up=(0, 0, 1))
    # ---- habaki (the blade collar) and the two washers
    hb = L.add(m, "habaki", "gold", base=KAT_COL["gold"], smooth=40, wear=0.4)
    ml.lathe(hb, "y", (0, KAT_Y_BASE, 0), [(0.0, 1.55), (0.5, 1.75), (3.6, 1.55), (4.2, 1.35), (4.7, 1.0)], sides=12, squash=0.52)
    ml.lathe(hb, "y", (0, KAT_Y_TSUBA + KAT_TSUBA_T, 0), [(0.0, 2.3), (0.3, 2.45), (0.5, 2.3)], sides=12, squash=0.62)
    ml.lathe(hb, "y", (0, KAT_Y_TSUBA - KAT_TSUBA_T - 0.5, 0), [(0.0, 2.3), (0.2, 2.45), (0.5, 2.3)], sides=12, squash=0.62)
    hg = L.add(m, "habaki_gem", "crystal", base=KAT_COL["pink"], glow=True, smooth=0)               # the main stone: a pointed bicone set through the collar so it shows on both faces
    ml.lathe(hg, "z", (0, KAT_GEM_Y, 0), [(-1.75, 0.0), (-0.3, KAT_GEM_R), (0.3, KAT_GEM_R), (1.75, 0.0)], sides=8, caps=(False, False))
    # ---- the guard: an iron five-petal blossom with a petal-shaped opening in each lobe (cut through the skin)
    ts = L.add(m, "tsuba", "iron", base=KAT_COL["iron"], smooth=35, wear=0.6)
    L.polar_prism(ts, "y", (0, KAT_Y_TSUBA, 0), KAT_TSUBA_T, _kat_sakura(KAT_TSUBA_R), n=60, bevel=0.04)
    ti = L.add(m, "tsuba_inlay", "gold", base=(232, 188, 96), smooth=30, wear=0.3)
    for sgn in (1, -1):
        for k in range(5):
            a = 2 * math.pi * k / 5 + math.pi / 2
            c, sn = math.cos(a), math.sin(a)
            y = KAT_Y_TSUBA + sgn * (KAT_TSUBA_T + 0.02)
            ml.bar(ti, (1.0 * c, y, 1.0 * sn), (3.1 * c, y, 3.1 * sn), 1.25, 0.16, up=(0, sgn, 0), taper=0.7, end_scale=0.35)
    # ---- the hilt: ray skin under indigo lacing, a slightly oval section that swells in the middle, then the collar and the two blossom menuki
    tk = L.add(m, "tsuka_wrap", "cloth", base=KAT_COL["ito"], smooth=45, uv_mode="loft", wear=0.4)
    st = []
    for i in range(11):
        f = i / 10
        st.append((KAT_TSUKA_Y0 + (KAT_TSUKA_Y1 - KAT_TSUKA_Y0) * f, 1.0 + 0.10 * math.sin(math.pi * f) - 0.05 * f, 0.84 + 0.08 * math.sin(math.pi * f)))
    ml.loft(tk, "y", (0, 0, 0), st, ml.circle(1.45, 12))
    fu = L.add(m, "guard_collar", "gold", base=KAT_COL["gold"], smooth=40, wear=0.4)
    ml.lathe(fu, "y", (0, KAT_TSUKA_Y1 - 0.4, 0), [(0.0, 1.55), (0.3, 1.78), (1.7, 1.78), (2.0, 1.55)], sides=12, squash=0.86)
    mk = L.add(m, "menuki", "gold", base=(236, 192, 100), smooth=30, wear=0.3)
    for sgn in (1, -1):                                                   # two small gold domes on each side of the hilt, under the lacing
        for y0 in (9.5, 17.5):
            L.cabochon(mk, (0.0, y0, sgn * 1.15), 0.7, 0.4, axis=(0, 0, sgn), sides=6, band=0.05, steps=2)
    # ---- the pommel: a gold cap and a second, smaller pink stone, a faceted drop, set in its end
    pc = L.add(m, "pommel_cap", "gold", base=KAT_COL["gold"], smooth=40, wear=0.4)
    ml.lathe(pc, "y", (0, 0.0, 0), [(0.0, 1.35), (0.35, 1.6), (0.8, 1.85), (1.6, 1.8), (1.9, 1.95), (2.5, 1.95), (2.8, 1.78), (3.4, 1.6)], sides=12, squash=0.86)
    gem = L.add(m, "pommel_gem", "crystal", base=KAT_COL["pink"], glow=True, smooth=0)
    ml.lathe(gem, "y", (0, 0.0, 0), [(0.5, 1.15), (0.0, 1.6), (-0.8, 1.7), (-1.8, 1.3), (-2.7, 0.65), (-3.2, 0.0)], sides=8, squash=0.9)

    atlas = L.lay(m)
    _paint_katana(m, atlas)
    place_diagonal(m, icon, KAT_REAL_M, KAT_GRIP)
    write_class(item_id, "katana", "a katana: the class length 1.4 to 1.8 blocks, tip bent toward the edge (-x)")
    finish_weapon(item_id, m, atlas, glow=("FFFFFF", 0.12, 0.4, 1.0))


def _paint_katana(m, atlas):
    """The skin of the katana: polished edge, wavy hardening line (hamon) with a pink glow, grain of the steel, the diamond lacing over white ray skin,
    the blossom openings of the guard, the two lit pink stones."""
    def blade(c):
        if c.kind != "dom" or c.g.key[0] != 2:
            return
        X, Y, _ = L.model_coords(c)
        shape = c.img.shape[:2]
        s = np.clip((Y - (KAT_Y_BASE - 1.0)) / (KAT_NAGASA + 1.0), 0.0, 1.0)
        wd = np.vectorize(_kat_width)(s)
        up = (X - np.vectorize(_kat_spine_x)(s)) / np.maximum(wd, 0.3)               # -1 at the edge .. 0 at the spine
        hamon = -0.60 + 0.060 * np.sin(Y * 0.50) + 0.040 * np.sin(Y * 1.31 + 1.3)     # the hardening line wanders between the edge and the ridge
        hard = L.smoothstep(0.03, -0.03, up - hamon)                                  # 1 on the hardened (edge) side
        rng = np.random.default_rng(5)
        streak = ml._noise(rng, shape[0], shape[1], 40.0, 1.3) - 0.5                  # the grain of the steel runs along the blade
        fine = ml._noise(rng, shape[0], shape[1], 5.0, 5.0) - 0.5
        jigane = np.array([168, 180, 202], np.float32) * (1.0 + 0.16 * streak[..., None] + 0.05 * fine[..., None])        # the soft steel
        yaki = np.array([244, 247, 251], np.float32) * (1.0 + 0.05 * streak[..., None])                                  # the hardened steel
        col = jigane * (1 - hard[..., None]) + yaki * hard[..., None]
        col = col * (0.90 + 0.14 * L.smoothstep(-0.45, 0.0, up))[..., None] * (0.94 + 0.10 * s)[..., None]
        shinogi = np.exp(-(((up + 0.40) / 0.022) ** 2))                               # the ridge line, a thin bright line
        col = col + 55 * shinogi[..., None]
        mune = L.smoothstep(-0.10, -0.02, up)                                          # the back of the blade is darker
        col = col * (1 - 0.30 * mune[..., None])
        c.img[..., :3] = np.clip(col, 0, 255)
        nioi = np.exp(-(((up - hamon) / 0.05) ** 2))                                  # the soft white mist along the hardening line
        L.blend(c.img, (252, 253, 255), nioi * 0.5)
        w.set_glow(c, KAT_GLOW, 0.4, mask=np.exp(-(((up - hamon - 0.02) / 0.05) ** 2)) * (s > 0.04) * (s < 0.97) * 0.8, core=0.2)
    w.paint_part(atlas, m.part("blade"), blade)

    def lacing(c):
        if c.kind != "loft":
            return
        per = float(max(c.g.origin["perim"]))
        P = per / 4.0
        f1 = ((c.A + c.B) / P) % 1.0
        f2 = ((c.A - c.B) / P) % 1.0
        l1, l2 = f1 < 0.36, f2 < 0.36
        crossing = l1 & l2
        line = l1 | l2
        shade = 0.60 + 0.55 * np.sin(np.where(l1, f1, f2) / 0.36 * math.pi)                      # each band is lit across its width: it looks raised
        ito = np.array(KAT_COL["ito"], np.float32) * shade[..., None]
        ito = np.where(crossing[..., None], ito * 0.78, ito)
        same = np.array(KAT_COL["same"], np.float32) * (0.82 + 0.18 * w.noise2(c.img.shape[:2], 2, 3)[..., None])
        dots = ((np.abs(((f1 - 0.62) * 8.0)) < 0.6) & (np.abs(((f2 - 0.62) * 8.0)) < 0.6))[..., None]
        same = np.where(dots, same * 0.78, same)
        c.img[..., :3] = np.where(line[..., None], ito, same)
    w.paint_part(atlas, m.part("tsuka_wrap"), lacing)

    def tsuba_iron(c):                                                     # hammered iron: faint concentric tooling rings and a mottled grain
        if c.kind != "dom" or c.g.key[0] != 1:
            return
        X, _, Z = L.model_coords(c)
        r = np.hypot(X, Z)
        grain = w.noise2(c.img.shape[:2], 4, 21) - 0.5
        rings = 0.5 + 0.5 * np.sin(r * 17.0 + grain * 3.0)
        col = np.array([54, 52, 64], np.float32) * (0.84 + 0.20 * rings[..., None] + 0.45 * grain[..., None])
        c.img[..., :3] = np.clip(col, 0, 255)
    w.paint_part(atlas, m.part("tsuba"), tsuba_iron)
    rim_r = _kat_sakura(KAT_TSUBA_R * 0.90)

    def tsuba_face(d):
        for k in range(5):                                                 # a petal shaped opening through the guard in each lobe, between the gold petals
            a = 2 * math.pi * k / 5 + math.pi / 2 + math.pi / 5
            ca, sa = math.cos(a), math.sin(a)
            petal = []
            for j in range(14):
                th = 2 * math.pi * j / 14
                lx, ly = 0.55 * math.cos(th), 0.95 * math.sin(th)          # 1.1 wide and 1.9 long, standing on the radius at 2.9 (in the valley between two inlays)
                petal.append(((2.9 + ly) * ca - lx * sa, KAT_Y_TSUBA, (2.9 + ly) * sa + lx * ca))
            d.polygon_m(petal, cut=True)
            d.line_m(petal + [petal[0]], 0.2, glow=(255, 134, 179, 150))
        rim = [(rim_r(2 * math.pi * j / 60) * math.cos(2 * math.pi * j / 60), KAT_Y_TSUBA, rim_r(2 * math.pi * j / 60) * math.sin(2 * math.pi * j / 60)) for j in range(61)]
        d.line_m(rim, 0.16, rgb=(214, 172, 82, 235), glow=(255, 140, 180, 110))
    for sgn in (1, -1):
        L.decal(atlas, m.part("tsuba"), "y", sgn, tsuba_face)
    L.paint_gem(atlas, m.part("pommel_gem"), KAT_COL["pink"], KAT_GLOW, strength=0.62, core=0.06, facets=8)
    L.paint_gem(atlas, m.part("habaki_gem"), KAT_COL["pink"], KAT_GLOW, strength=0.75, core=0.1, facets=8)


# ================================================================ Vassal Fan
# The target. Glass' weapon, the Fan Hero of Glass' world. From the series' wiki summaries (shield-hero.fandom.com/wiki/Vassal_Fan and /wiki/Glass): an iron
# (steel) war fan, used in a dance that throws storm-like wind blades; Naofumi made it a charm, a round jewel on a string hung from the handle; Glass wears a
# jet black kimono with silver embroidery. The look follows the logic builder's page (weapon_fan.md of the hvc worktree), which is design: the open fan of a
# gunsen, nine jet-black lacquered ribs between two sharpened steel blades, a pale silver-blue paper face with a plum branch embroidered in silver and white-pink
# blossoms, a thin silver rim with small steel serrations along the top arc, a short wrapped handle, a steel pivot collar with the weapon's jewel (spirit cyan)
# set through it so it shows on both faces, and the canon charm as a red tassel: a red cord, a red bead, the small jewel and red strands.
# Class: handitem (a held item, largest dimension 0.5 to 0.9 blocks, grip 0.5). A war fan is a weapon, so the top of the range: the span is about 0.92.
FAN_REAL_M = 0.80                  # apparent length of the weapon along the diagonal in the third person hand; the span across is 1.17 of it, so about 0.93
FAN_GRIP = 0.25                    # the hand holds the handle, a quarter of the way up the weapon
FAN_R = 44.0                       # radius of the leaf from the pivot (a real tessen is 25 to 40 cm; a legendary one is larger, and a fan reads best big)
FAN_HALF = math.radians(85.0)      # the half opening: 170 degrees open (the page asks for about 150 to 180: a half circle)
FAN_STICKS = 11                    # two steel blades and nine ribs (the page asks for nine ribs between the blades)
FAN_LEAF_IN = 5.2                  # the leaf starts this far from the pivot; below it the sticks are bare
FAN_PLEAT = 0.45                   # the leaf zig-zags this far (cm) either side of its plane between the ribs, like a folded fan
FAN_PIVOT = (0.0, 2.8)             # the pivot (kaname)
FAN_HANDLE_Y0 = -16.0
FAN_TEETH = 33                     # small steel serrations along the top arc, one every five degrees
FAN_COL = dict(ink=(21, 21, 27), steel=(154, 163, 173), face=(217, 228, 236), silver=(197, 206, 216), tassel=(179, 32, 42), petal=(247, 221, 230), jewel=(34, 150, 196))
FAN_GLOW = (92, 225, 255)          # the spirit cyan jewel's light


def _fan_angle(k):
    return -FAN_HALF + 2 * FAN_HALF * k / (FAN_STICKS - 1)


def _fan_dir(phi):
    return (math.sin(phi), math.cos(phi), 0.0)


def _fan_z(k):
    """The leaf's height at stick k: alternating either side of the plane (the blades sit in the plane)."""
    if k == 0 or k == FAN_STICKS - 1:
        return 0.0
    return FAN_PLEAT if k % 2 == 1 else -FAN_PLEAT


def _fan_branch():
    """The plum branch of the embroidery: a curve from the right of the leaf up and to the left (model cm), the blossom positions along it, and the twigs."""
    px, py = FAN_PIVOT
    pts = L.bezier((px + 19.0, py + 9.0), (px + 6.0, py + 7.0), (px - 10.0, py + 15.0), (px - 21.0, py + 27.0), 30)
    twigs = (((0.34, (-1.0, 0.9)), 8.0), ((0.62, (0.6, 1.0)), 7.0), ((0.82, (-0.3, 1.0)), 5.5))
    return pts, twigs


def build_vassal_fan():
    item_id = "vassal_fan"
    icon = sprites.write(item_id, L.ICON_DIR)
    m = ml.Mesh(item_id)
    px, py = FAN_PIVOT
    # ---- the leaf: pale paper folded between the sticks, two sheets and an edge, one pleat per gap
    lf = L.add(m, "fan_leaf", "cloth", base=FAN_COL["face"], smooth=12, uv_mode=None, wear=0.1)
    rows = [FAN_LEAF_IN + (FAN_R - FAN_LEAF_IN) * j / 4 for j in range(5)]
    half = 0.14
    for k in range(FAN_STICKS - 1):
        phis = [_fan_angle(k) + (_fan_angle(k + 1) - _fan_angle(k)) * c / 3 for c in range(4)]
        zs = [_fan_z(k) + (_fan_z(k + 1) - _fan_z(k)) * c / 3 for c in range(4)]
        for sgn in (1, -1):
            grid = [[(px + r * math.sin(ph), py + r * math.cos(ph), z + sgn * half) for ph, z in zip(phis, zs)] for r in rows]
            L.grid_surface(lf, grid, (0, 0, sgn))
        for c in range(3):                                                  # the rim of the paper along the outer and the inner arc
            for r, outward in ((rows[-1], 1.0), (rows[0], -1.0)):
                a0 = (px + r * math.sin(phis[c]), py + r * math.cos(phis[c]))
                a1 = (px + r * math.sin(phis[c + 1]), py + r * math.cos(phis[c + 1]))
                o = (math.sin((phis[c] + phis[c + 1]) / 2) * outward, math.cos((phis[c] + phis[c + 1]) / 2) * outward, 0.0)
                L.quad(lf, (a0[0], a0[1], zs[c] + half), (a1[0], a1[1], zs[c + 1] + half), (a1[0], a1[1], zs[c + 1] - half), (a0[0], a0[1], zs[c] - half), o)
    # ---- the nine ribs: jet-black lacquered iron, a little wider toward the rim, each tipped with a rounded end
    rb = L.add(m, "fan_ribs", "obsidian", base=FAN_COL["ink"], smooth=40, uv_mode="loft", wear=0.3)
    for k in range(1, FAN_STICKS - 1):
        d = _fan_dir(_fan_angle(k))
        ml.loft(rb, d, (px, py, _fan_z(k)), [(1.0, 0.9, 0.45), (FAN_R - 2.0, 1.5, 0.5), (FAN_R + 0.6, 1.8, 0.5), (FAN_R + 1.3, 1.5, 0.45), (FAN_R + 1.9, 0.5, 0.3)], ml.octagon(1.0, 1.0, 0.22))
    # ---- a thin silver band along the rim of the leaf, so the half disc has a bright outline
    rm_ = L.add(m, "leaf_rim", "silver", base=FAN_COL["silver"], smooth=45, uv_mode="loft", wear=0.3)
    L.tube(rm_, [(px + (FAN_R + 0.05) * math.sin(ph), py + (FAN_R + 0.05) * math.cos(ph), 0.0) for ph in np.linspace(-FAN_HALF * 0.99, FAN_HALF * 0.99, 26)], 0.6, sides=5, up=(0, 0, 1))
    # ---- the two outer sticks: sharpened steel blades (a flat-ground double edge), ending in a point
    gd = L.add(m, "fan_guards", "steel", base=FAN_COL["steel"], smooth=30, uv_mode="loft", wear=0.3)
    for k in (0, FAN_STICKS - 1):
        d = _fan_dir(_fan_angle(k))
        ml.loft(gd, d, (px, py, 0.0), [(-1.0, 2.6, 1.4), (FAN_R * 0.5, 3.2, 1.5), (FAN_R + 0.6, 3.5, 1.4), (FAN_R + 2.6, 2.6, 1.1), (FAN_R + 5.2, 0.4, 0.5)], ml.bevelled(1.0, 1.0, 0.5))
    # ---- the serrations: small steel teeth standing out of the rim along the top arc, between the ribs
    tt = L.add(m, "fan_teeth", "steel", base=FAN_COL["steel"], smooth=20, wear=0.3)
    for i in range(FAN_TEETH):
        ph = -math.radians(80.0) + math.radians(160.0) * i / (FAN_TEETH - 1)
        d = _fan_dir(ph)
        L.spike(tt, (px + (FAN_R - 0.2) * d[0], py + (FAN_R - 0.2) * d[1], 0.0), d, 2.5, 0.85, sides=3, r_tip=0.0)
    # ---- the pivot: a steel collar with the jewel set through it
    pp = L.add(m, "pivot_plate", "steel", base=FAN_COL["silver"], smooth=40, wear=0.4)
    ml.lathe(pp, "z", (px, py, 0.0), [(-1.7, 3.0), (-1.3, 4.3), (-0.4, 4.6), (0.4, 4.6), (1.3, 4.3), (1.7, 3.0)], sides=16)
    gem = L.add(m, "pivot_gem", "crystal", base=FAN_COL["jewel"], glow=True, smooth=0)
    w.ellipsoid(gem, (px, py, 0.0), 2.7, 2.7, 3.4, sides=12, stacks=8)
    # ---- the handle: lacquer wrapped in cord, with silver rings, widening toward the pivot
    hd = L.add(m, "handle", "obsidian", base=FAN_COL["ink"], smooth=50, uv_mode="loft", wear=0.3)
    ml.lathe(hd, "y", (0, FAN_HANDLE_Y0, 0), [(0.0, 1.1), (0.7, 1.55), (3.0, 1.65), (8.0, 1.85), (13.0, 2.1), (16.0, 2.3), (18.0, 2.5), (19.3, 2.2)], sides=10, squash=0.82)
    hr = L.add(m, "handle_rings", "silver", base=FAN_COL["silver"], smooth=40, wear=0.4)
    for y in (FAN_HANDLE_Y0 + 0.2, FAN_HANDLE_Y0 + 6.5, FAN_HANDLE_Y0 + 13.0):
        ml.collar(hr, y, 2.0 + (y - FAN_HANDLE_Y0) * 0.02, 1.0, lip=0.25, sides=10, axis="y", origin=(0, 0, 0), oval=0.84, caps=(False, False))
    # ---- the tassel (the charm of the series): a red cord, a red lacquer bead, the small jewel in a silver ring, and red strands
    cc = L.add(m, "charm_cord", "cloth", base=FAN_COL["tassel"], smooth=50, uv_mode="loft", wear=0.1)
    L.tube(cc, [(0.0, FAN_HANDLE_Y0 + 0.3, 0.0), (0.4, FAN_HANDLE_Y0 - 1.6, 0.3), (0.7, FAN_HANDLE_Y0 - 3.2, 0.0)], 0.35, sides=5, up=(0, 0, 1))
    rbd = L.add(m, "charm_bead_red", "obsidian", base=FAN_COL["tassel"], smooth=0)
    w.ellipsoid(rbd, (0.7, FAN_HANDLE_Y0 - 4.3, 0.0), 1.25, 1.35, 1.25, sides=10, stacks=7)
    cb = L.add(m, "charm_bead", "crystal", base=FAN_COL["jewel"], glow=True, smooth=0)
    by = FAN_HANDLE_Y0 - 7.9
    w.ellipsoid(cb, (0.7, by, 0.0), 1.7, 1.9, 1.7, sides=10, stacks=7)
    L.torus(cc, (0.7, by, 0.0), 2.05, 0.28, (0, 0, 1), n=12, sides=4)
    L.tube(cc, [(0.7, FAN_HANDLE_Y0 - 5.6, 0.0), (0.7, by + 1.5, 0.0)], 0.3, sides=5, up=(0, 0, 1))
    for i, (dx, dz, ln) in enumerate(((-0.55, 0.45, 12.0), (0.0, -0.4, 14.0), (0.55, 0.35, 11.0), (0.2, 0.0, 13.0))):
        ml.bar(cc, (0.7 + dx, by - 1.9, dz), (0.7 + dx * 2.2, by - 1.9 - ln, dz * 1.5), 0.8, 0.1, up=(0, 0, 1), taper=ln * 0.35, end_scale=0.5)

    atlas = L.lay(m)
    _paint_fan(m, atlas)
    place_diagonal(m, icon, FAN_REAL_M, FAN_GRIP)
    write_class(item_id, "handitem", "a held war fan: measured by its largest dimension (the span, about 0.9 blocks)")
    finish_weapon(item_id, m, atlas, glow=("FFFFFF", 0.12, 0.4, 1.0))


def _paint_fan(m, atlas):
    """The skin of the fan: the pale paper with its folds and fibres, the silver embroidery (double border, a plum branch with white-pink blossoms) with a faint
    cyan shimmer, the black lacquer, the steel blades and teeth, the cyan jewel and the red tassel."""
    px, py = FAN_PIVOT
    face = np.array(FAN_COL["face"], np.float32)
    ink = np.array(FAN_COL["ink"], np.float32)
    steel = np.array(FAN_COL["steel"], np.float32)

    def paper(c):
        if c.kind != "dom" or c.g.key[0] != 2:
            return
        X, Y, _ = L.model_coords(c)
        r = np.hypot(X - px, Y - py)
        ph = np.arctan2(X - px, Y - py)
        u = (ph + FAN_HALF) / (2 * FAN_HALF) * (FAN_STICKS - 1)                     # 0 .. 10: the stick index along the leaf
        fold = np.abs((u % 1.0) - 0.5) * 2.0                                          # 1 on a rib, 0 mid-pleat
        pleat = np.where((np.floor(u).astype(int) % 2) == 0, 1.0, -1.0)
        fibre = w.noise2(c.img.shape[:2], 1.6, 31) - 0.5
        weave = w.noise2(c.img.shape[:2], 5.0, 32) - 0.5
        shade = 1.0 + 0.07 * pleat * (1 - fold) - 0.10 * (1 - fold) ** 2 + 0.05 * fibre + 0.04 * weave + 0.05 * np.clip((r - 14.0) / 30.0, 0, 1)
        col = face[None, None, :] * shade[..., None]
        c.img[..., :3] = np.clip(col, 0, 255)
    w.paint_part(atlas, m.part("fan_leaf"), paper)

    def embroidery(d):
        sil, sg = (176, 190, 208, 235), (110, 230, 255, 70)
        pink, pink_edge = (247, 221, 230, 255), (176, 150, 170, 255)
        R = FAN_R

        def arc(r, a0, a1, n=48):
            return [(px + r * math.sin(a0 + (a1 - a0) * i / n), py + r * math.cos(a0 + (a1 - a0) * i / n)) for i in range(n + 1)]
        for r, wcm in ((R - 2.2, 0.36), (R - 3.5, 0.18), (FAN_LEAF_IN + 5.0, 0.2)):                     # the double border and a fine ring near the pivot
            d.line(arc(r, -FAN_HALF + 0.05, FAN_HALF - 0.05), wcm, rgb=sil, glow=sg)
        pts, twigs = _fan_branch()
        d.line([(p[0], p[1]) for p in pts], 0.62, rgb=(108, 124, 150, 255), glow=sg)               # the branch, in silver thread
        d.line([(p[0] + 0.05, p[1] + 0.25) for p in pts], 0.2, rgb=(214, 224, 238, 220))
        blossoms = []
        for t in (0.07, 0.20, 0.50, 0.74, 0.97):
            blossoms.append(pts[int(round(t * (len(pts) - 1)))][:2])
        for (t, dirv), ln in twigs:
            base = pts[int(round(t * (len(pts) - 1)))]
            dl = math.hypot(*dirv)
            tip = (base[0] + dirv[0] / dl * ln, base[1] + dirv[1] / dl * ln)
            mid = (base[0] + dirv[0] / dl * ln * 0.5 + 0.8, base[1] + dirv[1] / dl * ln * 0.5)
            d.line([(base[0], base[1]), mid, tip], 0.4, rgb=(108, 124, 150, 255), glow=sg)
            blossoms.append(tip)
        for k, (cx, cy) in enumerate(blossoms):                                   # five-petal blossoms, white-pink with a silver outline and a darker heart
            rot = 0.6 * k
            for q in range(5):
                a = 2 * math.pi * q / 5 + rot
                d.ellipse_m((cx + 1.35 * math.cos(a), cy + 1.35 * math.sin(a), 0), 1.25, 1.25, rgb=pink_edge, glow=sg)
            for q in range(5):
                a = 2 * math.pi * q / 5 + rot
                d.ellipse_m((cx + 1.35 * math.cos(a), cy + 1.35 * math.sin(a), 0), 1.0, 1.0, rgb=pink)
            d.ellipse_m((cx, cy, 0), 0.75, 0.75, rgb=(222, 150, 176, 255), glow=(160, 245, 255, 130))
            for q in range(5):
                a = 2 * math.pi * q / 5 + rot + 0.3
                d.ellipse_m((cx + 0.95 * math.cos(a), cy + 0.95 * math.sin(a), 0), 0.17, 0.17, rgb=(150, 100, 130, 255))
        for (t, off) in ((0.28, (1.8, -1.9)), (0.58, (-1.2, 2.0)), (0.88, (1.3, 1.6))):                   # a few small buds on short stalks
            base = pts[int(round(t * (len(pts) - 1)))]
            tip = (base[0] + off[0], base[1] + off[1])
            d.line([(base[0], base[1]), tip], 0.3, rgb=(108, 124, 150, 255))
            d.ellipse_m((tip[0], tip[1], 0), 0.7, 0.7, rgb=pink, glow=sg)
    for sgn in (1, -1):
        L.decal(atlas, m.part("fan_leaf"), "z", sgn, embroidery)

    def lacquer(c):
        S = c.S if c.kind == "loft" else np.full(c.img.shape[:2], 0.5, np.float32)
        n = w.noise2(c.img.shape[:2], 3, 33) - 0.5
        sheen = np.exp(-(((np.abs(S - 0.25) % 0.5) / 0.07) ** 2))
        col = ink[None, None, :] * (0.9 + 0.2 * n[..., None]) + 38.0 * sheen[..., None] * np.array([0.9, 0.95, 1.2], np.float32)[None, None, :]
        c.img[..., :3] = np.clip(col, 0, 255)
    w.paint_part(atlas, m.part("fan_ribs"), lacquer)
    w.paint_part(atlas, m.part("handle"), lacquer)

    def wrap(c):
        lacquer(c)
        if c.kind == "loft":
            cord = ((c.A * 1.3 + c.B * 1.3) % 2.2) < 0.7
            cord2 = ((c.A * 1.3 - c.B * 1.3) % 2.2) < 0.7
            L.blend(c.img, (74, 74, 94), (cord | cord2) * 0.75)
    w.paint_part(atlas, m.part("handle"), wrap)

    def blade(c):
        S = c.S if c.kind == "loft" else np.full(c.img.shape[:2], 0.25, np.float32)
        n = w.noise2(c.img.shape[:2], 2, 34) - 0.5
        brush = 0.88 + 0.30 * (0.5 + 0.5 * np.cos(2 * math.pi * S * 2.0)) + 0.1 * n
        col = steel[None, None, :] * brush[..., None]
        c.img[..., :3] = np.clip(col, 0, 255)
        edge = np.maximum(np.exp(-(np.minimum(S, 1.0 - S) / 0.05) ** 2), np.exp(-(((S - 0.5) / 0.05) ** 2)))
        L.blend(c.img, (244, 248, 255), edge * 0.9)
    w.paint_part(atlas, m.part("fan_guards"), blade)
    w.paint_part(atlas, m.part("fan_teeth"), blade)
    w.paint_part(atlas, m.part("leaf_rim"), lambda c: (L.shade_img(c.img, 1.0), w.set_glow(c, FAN_GLOW, 0.10, mask=np.full(c.img.shape[:2], 0.6, np.float32), core=0.0)))
    L.paint_gem(atlas, m.part("pivot_gem"), FAN_COL["jewel"], FAN_GLOW, strength=0.8, core=0.25, facets=12)
    L.paint_gem(atlas, m.part("charm_bead"), FAN_COL["jewel"], FAN_GLOW, strength=0.8, core=0.25, facets=10)
    L.glow_part(atlas, m.part("pivot_plate"), FAN_GLOW, 0.12, 0.05)                           # a soft ring of light round the collar


# ================================================================ Vassal Carriage
# The target. The Vassal Carriage is Fitoria's, the Filolial queen's (wiki summaries: shield-hero.fandom.com/wiki/Vassal_Carriage, /wiki/Fitoria): the base form
# ("Small Carriage") is shaped like an ancient Roman chariot, the weapon gives fast transport and protection to those aboard, in the series she arrives behind a
# light blue Filolial and the carriage carries a gemstone on its roof like the other Legendary and Vassal weapons; her plumage is white with blue and yellow
# feathers, her dress red and white. The logic builder's page (docs/heroweapon/weapon_carriage.md of the hvb worktree) fixes what the ENTITY needs, and this mesh
# is the entity's model, drawn about 3.2 times larger by its renderer, which does not use the `place` line:
#   mesh space  +y is the nose, -x is UP (the roof is at low x, the wheels touch the ground at x = 0, the highest x), z is the width (a wheel at +z and one at -z);
#   proportions (shares of L, the whole length along y): body 0.70 L, pole and yoke 0.30 L ahead of it, width over the wheels 0.60 L, height to the top of the canopy
#   0.58 L, wheel diameter 0.34 L, seat surfaces 0.17 L above the ground (driver 0.17 L ahead of the middle of the body, two passengers 0.19 L behind it and
#   0.14 L either side); named parts: wheel_left, wheel_right, axle, chassis, cabin, canopy, posts, bench_front, bench_rear, pole, yoke, lantern, gem, plumes;
#   nothing thinner than 1.2 % of L, nothing loose or hanging free.
# So: a wide low open chariot-carriage, ivory with gold, deep blue upholstery, a shallow domed canopy on four posts, two big spoked wheels, one long draught pole
# with a yoke for the Filolial, two upright feather plumes, and the weapon's jewel, a faceted amber stone, in the lantern at the canopy's front (the logic page)
# with a second one on the roof (the series). Design with intent: the palette is the logic page's (a wiki summary gave no picture).
# Held in the hand it stands upright on its wheels, nose to the right of the item cube (place angle 0), about 1.0 block long.
CAR_L = 120.0                      # the whole length, tail to the end of the yoke (cm); the item cube holds it 1.0 block long
CAR_BODY = 0.70 * CAR_L            # 84: the body, tail to the front of the dash
CAR_WHEEL_R = 0.17 * CAR_L         # 20.4: wheel diameter 0.34 L
CAR_HALF_W = 0.30 * CAR_L          # 36: half the width over the wheels (0.60 L)
CAR_WHEEL_T = 5.0                  # wheel thickness: a rim 0.025 L = 3.0 cm thick at least
CAR_WHEEL_Z = CAR_HALF_W - 3.3       # a wheel's centre plane: its hub cap, the outermost part, stands 3.3 cm outside it, so the width over the wheels is exactly 0.60 L
CAR_AXLE_F = 38.0                  # the axle's place along the body (a little behind the middle, where a chariot's weight is)
CAR_SEAT_U = 0.17 * CAR_L          # 20.4: the seat surfaces
CAR_TOP_U = 0.58 * CAR_L           # 69.6: the top of the canopy
CAR_DRIVER_F = CAR_BODY / 2 + 0.17 * CAR_L           # 62.4: ahead of the middle of the body
CAR_PASS_F, CAR_PASS_W = CAR_BODY / 2 - 0.19 * CAR_L, 0.14 * CAR_L     # 19.2 behind it, 16.8 either side
CAR_BODY_HW = 26.0                 # half the width of the body between the wheels
CAR_FLOOR_U = (9.0, 13.0)          # the floor plate
CAR_POST_U = (22.0, 60.0, 30.0)    # the four slim posts: foot of the rear pair (the wall is 23.6 cm high there), top of all four, foot of the front pair (the wall is 32.8 cm high there)
CAR_CANOPY_U = (58.0, CAR_TOP_U)   # the canopy's rim to its top: a dome 11.6 cm high on an 86 x 62 cm plan
CAR_CANOPY_PLAN = (86.0, 62.0, 15.0)     # its length, width and corner radius (with the rim's 3.5 % flare it spans y = 0 to 89: flush with the tail, 5 cm over the dash)
CAR_LIP = 2.4                      # the straight rim band before the dome begins (painted gold)
CAR_PLUME_U, CAR_PLUME_F, CAR_PLUME_Z = 63.8, 75.0, 24.0         # where the two plumes root: on the canopy's front corners (the dome is 64 cm high there)
CAR_FEATHERS = ((-14.0, 24.0, 6.2), (8.0, 30.0, 6.8), (30.0, 22.0, 6.0))      # each plume: three feathers (lean from upright in degrees, length, width in cm); a lean is toward the nose
CAR_FEATHER_T, CAR_FEATHER_GAP = 1.6, 1.7        # a feather is 1.6 cm thick (the floor of the model is 1.44), stacked 1.7 cm apart so they do not fight
CABIN_START = {}                   # how far along the cabin wall each of its three sweeps starts (cm), by the sweep's loft record: the painter needs it to run a pattern on across the joints
CAR_MIN = 0.012 * CAR_L            # 1.44 cm: nothing is thinner than this
CAR_COL = dict(ivory=(242, 235, 214), gold=(216, 169, 58), blue=(47, 75, 134), walnut=(107, 68, 41), plume=(255, 255, 255), sky=(112, 172, 232), amber=(255, 194, 71))
CAR_GLOW = (255, 194, 71)          # the jewels' light
CAR_JEWEL = (196, 104, 10)         # the amber stone's body colour on the main skin (darker than its light, so the facets read)
CAR_ROYAL = (52, 88, 164)          # the canopy's outside: a royal blue between the logic page's deep blue and its sky blue
CAR_CANOPY_F = 44.5                # the canopy's centre along the body
CAR_YOKE_F = CAR_L - 4.5           # the yoke's centre: its ring and knobs end at y = 120 = L, so the model is exactly L long (the renderer scales by 3.2 / length)
CAR_UP = (-1.0, 0.0, 0.0)          # mesh space up


def _up(u, f, w):
    """A point given as (height above the ground, along the nose, across) in the mesh space of the entity: x = -height, y = along, z = across."""
    return (-u, f, w)


def _dplan(half_w, f0, f1, arc_r, n=10):
    """The plan of the body, centred on its middle: a D (straight sides, a half round front, a square tail), as (f - centre, w) points."""
    c = (f0 + f1) / 2
    fc = f1 - arc_r
    pts = [(f0 - c, -half_w), (fc - c, -half_w)]
    for i in range(1, n):
        a = -math.pi / 2 + math.pi * i / n
        pts.append((fc + arc_r * math.cos(a) - c, arc_r * math.sin(a) * half_w / arc_r))
    pts += [(fc - c, half_w), (f0 - c, half_w)]
    return pts, c


def _canopy_profile():
    """The canopy's stations (height above its rim, scale of the plan): a slightly flared rim band, then a circular arc of a dome that closes at the top."""
    top = CAR_CANOPY_U[1] - CAR_CANOPY_U[0]
    dome = top - CAR_LIP
    st = [(0.0, 0.975, 0.975), (1.0, 1.035, 1.035), (CAR_LIP, 1.0, 1.0)]
    for k in range(1, 8):
        a = (k / 7.0) * (math.pi / 2) * 0.985
        st.append((CAR_LIP + dome * math.sin(a), math.cos(a), math.cos(a)))
    return st


def _car_wheel(m, name, zc):
    """One wheel in the x-y plane (axle along z): a felloe ring, eight flat spokes and a hub with a cap; centre (-R, axle, zc), the ground at x = 0."""
    R = CAR_WHEEL_R
    cx, cy = -R, CAR_AXLE_F
    wp = L.add(m, name, "wood", base=CAR_COL["walnut"], smooth=40, wear=0.35)
    ring = [(cx + (R - 1.9) * math.cos(a), cy + (R - 1.9) * math.sin(a), zc) for a in np.linspace(0, 2 * math.pi, 21)]
    ml.sweep(wp, ring, [(-0.5, -0.5), (0.5, -0.5), (0.5, 0.5), (-0.5, 0.5)], scales=lambda t: (3.8, CAR_WHEEL_T), caps=(False, False), up=(0, 0, 1))      # felloe 3.8 deep, 5.0 wide
    for k in range(8):
        a = 2 * math.pi * k / 8 + math.pi / 8
        ml.loft(wp, (math.cos(a), math.sin(a), 0.0), (cx, cy, zc), [(2.5, 2.8, 2.0), (R - 3.0, 2.2, 1.6)], ml.rect_ring(1.0, 1.0), caps=(False, False), up=(0, 0, 1))
    ml.lathe(wp, "z", (cx, cy, zc), [(-2.9, 3.0), (-2.2, 4.6), (2.2, 4.6), (2.9, 3.0)], sides=10)
    sgn = 1.0 if zc > 0 else -1.0
    ml.lathe(wp, "z", (cx, cy, zc + sgn * 2.4), [(0.0, 2.9), (sgn * 0.45, 2.5), (sgn * 0.9, 0.0)], sides=10)
    return wp


def build_vassal_carriage():
    item_id = "vassal_carriage"
    sprites.write(item_id, L.ICON_DIR)
    m = ml.Mesh(item_id)
    # ---- the two wheels, each its own part so a renderer can turn each about the axle line (x = -20.4, y = 38, along z)
    _car_wheel(m, "wheel_left", -CAR_WHEEL_Z)
    _car_wheel(m, "wheel_right", CAR_WHEEL_Z)
    # ---- the axle: two stubs from the hubs into the body's sides (a chariot's axle runs under its floor, so nothing crosses the riders' feet)
    ax = L.add(m, "axle", "iron", base=(70, 66, 62), smooth=45, wear=0.5)
    for sgn in (1, -1):
        ml.loft(ax, (0, 0, sgn), (-CAR_WHEEL_R, CAR_AXLE_F, sgn * 22.0), [(0.0, 1.0, 1.0), (11.0, 1.0, 1.0)], ml.circle(2.0, 8), caps=(False, True))
    # ---- the chassis: the floor plate (a D with a half round front) and a gold-edged under-frame
    ch = L.add(m, "chassis", "bone", base=CAR_COL["ivory"], smooth=40, wear=0.2)
    plan, fc = _dplan(CAR_BODY_HW, 0.0, CAR_BODY, 26.0)
    ml.loft(ch, CAR_UP, _up(CAR_FLOOR_U[0], fc, 0.0), [(0.0, 0.94, 0.94), (0.8, 1.0, 1.0), (CAR_FLOOR_U[1] - CAR_FLOOR_U[0] - 0.8, 1.0, 1.0), (CAR_FLOOR_U[1] - CAR_FLOOR_U[0], 0.97, 0.97)], plan)
    for sgn in (1, -1):                                            # two longitudinal rails under the floor, between the wheels
        ml.loft(ch, "y", (-CAR_FLOOR_U[0] + 2.0, 0.0, sgn * 14.0), [(4.0, 1.0, 1.0), (CAR_BODY - 10.0, 1.0, 1.0)], ml.rounded_rect(4.5, 3.4, 0.8, 1), caps=(True, True))
    # ---- the cabin: the low walls, rising to a curved dash in front, open at the back
    cb = L.add(m, "cabin", "bone", base=CAR_COL["ivory"], smooth=40, uv_mode="loft", wear=0.2)
    side_f = 58.0
    ts = [0.3 * k / 4 for k in range(5)] + [0.3 + 0.4 * k / 8 for k in range(1, 9)] + [0.7 + 0.3 * k / 4 for k in range(1, 5)]      # left wall, round the front, right wall
    path, hts = [], []
    for t in ts:
        if t < 0.3 - 1e-9:
            f, wz = side_f * t / 0.3, -CAR_BODY_HW
        elif t < 0.7 - 1e-9:
            a = -math.pi / 2 + math.pi * (t - 0.3) / 0.4
            f, wz = side_f + 26.0 * math.cos(a), 26.0 * math.sin(a)
        else:
            f, wz = side_f * (1 - (t - 0.7) / 0.3), CAR_BODY_HW
        h = 11.0 + 15.0 * (1.0 - abs(2.0 * t - 1.0)) ** 1.6           # 11 cm high at the tail, rising to 26 cm at the dash
        path.append(_up(CAR_FLOOR_U[1] - 0.6 + h / 2, f, wz))
        hts.append(h)
    cum = np.concatenate([[0.0], np.cumsum(np.linalg.norm(np.diff(np.array(path), axis=0), axis=1))])
    CABIN_START.clear()
    cuts = (0, 4, 12, 16)                                            # three pieces, so the skin lays them out as three shorter strips instead of one long one
    for k, lf in enumerate(L.sweep_pieces(cb, path, ml.rect_ring(1.0, 1.0), [(2.6, h) for h in hts], cuts, up=CAR_UP)):
        CABIN_START[id(lf.record)] = float(cum[cuts[k]])
    CABIN_START["front"] = float(cum[8])                              # the way along the wall to the middle of the dash
    # ---- the four slim posts and the canopy
    ps = L.add(m, "posts", "gold", base=CAR_COL["gold"], smooth=45, uv_mode="loft", wear=0.35)
    for (pf, pz, u0) in ((6.0, -26.0, CAR_POST_U[0]), (6.0, 26.0, CAR_POST_U[0]), (72.0, -21.9, CAR_POST_U[2]), (72.0, 21.9, CAR_POST_U[2])):     # each stands on the wall's centre line
        ml.loft(ps, CAR_UP, _up(u0, pf, pz), [(0.0, 1.0, 1.0), (CAR_POST_U[1] - u0, 1.0, 1.0)], ml.circle(1.8, 8), caps=(False, False))
    cn = L.add(m, "canopy", "bone", base=CAR_COL["ivory"], smooth=40, wear=0.2)
    ml.loft(cn, CAR_UP, _up(CAR_CANOPY_U[0], CAR_CANOPY_F, 0.0), _canopy_profile(), ml.rounded_rect(CAR_CANOPY_PLAN[0], CAR_CANOPY_PLAN[1], CAR_CANOPY_PLAN[2], 4))
    # ---- the benches: the driver's in the middle ahead, the rear bench for two with a back; deep blue upholstery
    bf = L.add(m, "bench_front", "leather", base=CAR_COL["blue"], smooth=35, wear=0.3)
    ml.loft(bf, CAR_UP, _up(CAR_FLOOR_U[1] - 0.5, CAR_DRIVER_F, 0.0), [(0.0, 1.0, 1.0), (CAR_SEAT_U - CAR_FLOOR_U[1] + 0.5, 1.0, 1.0)], ml.rounded_rect(16.0, 24.0, 3.0, 2))
    ml.loft(bf, CAR_UP, _up(CAR_FLOOR_U[1] - 0.5, CAR_DRIVER_F - 8.5, 0.0), [(0.0, 1.0, 1.0), (15.0, 1.0, 1.0)], ml.rounded_rect(3.0, 24.0, 1.2, 1))
    br = L.add(m, "bench_rear", "leather", base=CAR_COL["blue"], smooth=35, wear=0.3)
    ml.loft(br, CAR_UP, _up(CAR_FLOOR_U[1] - 0.5, CAR_PASS_F, 0.0), [(0.0, 1.0, 1.0), (CAR_SEAT_U - CAR_FLOOR_U[1] + 0.5, 1.0, 1.0)], ml.rounded_rect(16.0, 52.0, 3.0, 2))
    ml.loft(br, CAR_UP, _up(CAR_FLOOR_U[1] - 0.5, CAR_PASS_F - 9.5, 0.0), [(0.0, 1.0, 1.0), (22.0, 1.0, 1.0)], ml.rounded_rect(3.5, 52.0, 1.5, 1))
    # ---- the draught pole and the yoke, gold, ahead of the dash
    pl = L.add(m, "pole", "gold", base=CAR_COL["gold"], smooth=45, wear=0.3)
    pts = [_up(15.0 + 12.0 * t, CAR_BODY - 6.0 + (CAR_YOKE_F - (CAR_BODY - 6.0)) * t, 0.0) for t in np.linspace(0, 1, 6)]
    L.tube(pl, pts, 2.1, sides=8, up=CAR_UP)
    yk = L.add(m, "yoke", "gold", base=CAR_COL["gold"], smooth=45, wear=0.3)
    ml.loft(yk, "z", (_up(27.0, CAR_YOKE_F, 0.0)[0], CAR_YOKE_F, -15.0), [(0.0, 1.0, 1.0), (30.0, 1.0, 1.0)], ml.circle(2.0, 8))
    for sgn in (1, -1):
        w.ellipsoid(yk, _up(27.0, CAR_YOKE_F, sgn * 15.5), 2.9, 2.9, 2.9, sides=8, stacks=5)
    L.torus(yk, _up(27.0, CAR_YOKE_F, 0.0), 3.4, 1.0, (0, 0, 1), n=12, sides=5)
    # ---- the lantern at the front of the canopy: a rigid bracket, a gold cage, the jewel in it
    ln = L.add(m, "lantern", "gold", base=CAR_COL["gold"], smooth=40, wear=0.3)
    lf, lu = 94.0, 50.0
    L.tube(ln, [_up(CAR_CANOPY_U[0] + 3.0, 86.0, 0.0), _up(CAR_CANOPY_U[0] + 5.0, 90.0, 0.0), _up(lu + 11.0, lf, 0.0)], 1.5, sides=6, up=CAR_UP)
    ml.lathe(ln, CAR_UP, _up(lu - 1.4, lf, 0.0), [(0.0, 2.6), (1.4, 3.8), (1.9, 3.5)], sides=8)                         # the base plate of the cage
    ml.lathe(ln, CAR_UP, _up(lu + 9.0, lf, 0.0), [(0.0, 3.6), (1.2, 4.2), (3.4, 0.0)], sides=8)                          # the roof cap
    for k in range(4):
        a = 2 * math.pi * k / 4 + math.pi / 4
        L.rod(ln, _up(lu + 0.4, lf + 3.2 * math.cos(a), 3.2 * math.sin(a)), _up(lu + 9.4, lf + 3.2 * math.cos(a), 3.2 * math.sin(a)), 0.9, 0.9, sides=5)    # four corner bars
    # ---- the jewels: a faceted amber stone in the lantern and one on the roof (the series puts the weapon's jewel there)
    gm = L.add(m, "gem", "crystal", base=CAR_COL["amber"], glow=True, smooth=0, uv_mode="loft")
    L.bicone(gm, _up(lu + 4.7, lf, 0.0), 2.7, 4.0, sides=8, axis=CAR_UP)
    ml.lathe(gm, CAR_UP, _up(CAR_TOP_U - 0.6, CAR_CANOPY_F, 0.0), [(-1.6, 0.0), (0.0, 2.6), (1.4, 3.5), (2.8, 3.5), (4.6, 2.2), (6.8, 0.0)], sides=8, caps=(False, False))
    gc = L.add(m, "plumes", "cloth", base=CAR_COL["plume"], smooth=25, uv_mode="loft", wear=0.2)                          # the feathers stand on the front posts
    for sgn in (1, -1):
        for k, (ang, ln_, wd) in enumerate(CAR_FEATHERS):
            a = math.radians(ang)
            d = np.array([-math.cos(a), math.sin(a), sgn * 0.06])
            d /= np.linalg.norm(d)
            root = (-CAR_PLUME_U, CAR_PLUME_F, sgn * (CAR_PLUME_Z + (k - 1) * CAR_FEATHER_GAP))
            ml.loft(gc, tuple(d), root, [(0.0, 0.30 * wd, CAR_FEATHER_T), (0.10 * ln_, 0.62 * wd, CAR_FEATHER_T), (0.40 * ln_, wd, CAR_FEATHER_T), (0.70 * ln_, 0.80 * wd, CAR_FEATHER_T),
                                          (0.90 * ln_, 0.38 * wd, CAR_FEATHER_T * 0.85), (ln_, 0.0, 0.0)], ml.rounded_rect(1.0, 1.0, 0.34, 1), up=(0, 0, 1))

    atlas = L.lay(m)
    _paint_carriage(m, atlas)
    place_flat(m, 1.0)
    write_class(item_id, "handitem", "a small carriage held upright on its wheels, nose to the right: measured by its largest dimension (1.0 block long in the item cube)")
    finish_weapon(item_id, m, atlas, glow=("FFFFFF", 0.12, 0.45, 1.0), upright=True, display="carriage")


def _band(d, half, soft=0.32):
    """1 inside a stripe of half width `half` (cm) about the zero of a distance field `d`, with a soft edge (0.32 cm is half a texel at the carriage's 1.6 px per cm)."""
    return L.smoothstep(half + soft, max(half - soft, 0.0), np.abs(d))


def _d_depth(f, z):
    """How far inside the floor's D plan a point lies (cm, negative outside): straight sides at |z| = CAR_BODY_HW, a square tail, a half round front."""
    r = CAR_BODY_HW
    fc = CAR_BODY - r
    inner = np.minimum(r - np.abs(z), f)
    return np.where(f > fc, np.minimum(inner, r - np.hypot(np.maximum(f - fc, 0.0), z)), inner)


def _canopy_depth(f, z):
    """How far inside the canopy's rounded-rectangle plan a point lies (cm, negative outside)."""
    ln, wd, rr = CAR_CANOPY_PLAN
    qx = np.abs(f - CAR_CANOPY_F) - (ln / 2 - rr)
    qz = np.abs(z) - (wd / 2 - rr)
    return rr - (np.hypot(np.maximum(qx, 0.0), np.maximum(qz, 0.0)) + np.minimum(np.maximum(qx, qz), 0.0))


def _paint_carriage(m, atlas):
    """The skin of the carriage, every pattern drawn in model coordinates so it sits where the part is: ivory enamel with gold beads and a vine of engraved
    light on the cabin, a royal blue canopy with eight gold ribs round a rosette, deep blue buttoned upholstery, gilt posts / pole / yoke / lantern, walnut
    spokes in gold-rimmed wheels, white feathers fading to sky blue, and the two amber jewels (the glow skin: the jewels strong, the vine soft, the hubs faint)."""
    gold = np.array(CAR_COL["gold"], np.float32)
    ivory = np.array(CAR_COL["ivory"], np.float32)
    blue = np.array(CAR_COL["blue"], np.float32)
    walnut = np.array(CAR_COL["walnut"], np.float32)
    sky = np.array(CAR_COL["sky"], np.float32)
    royal = np.array(CAR_ROYAL, np.float32)
    plume = np.array(CAR_COL["plume"], np.float32)
    gold_hi = np.array((255, 228, 140), np.float32)
    gold_lo = np.array((138, 96, 28), np.float32)

    def nz(c, cell, seed):
        return w.noise2(c.img.shape[:2], cell, seed) - 0.5

    def fill(c, rgb, seed, var=0.05, cell=4.0):
        """A flat painted surface: `rgb` with soft cloudy variation and a little fine grain, so a big plane does not look printed."""
        k = 1.0 + var * 2 * nz(c, cell, seed) + 0.03 * nz(c, 1.3, seed + 9)
        c.img[..., :3] = np.clip(np.asarray(rgb, np.float32)[None, None, :] * k[..., None], 0, 255)

    def gilt(c, seed):
        """Gold leaf: a warm base, soft streaks and a few bright glints."""
        n = nz(c, 3.0, seed)
        g = nz(c, 1.2, seed + 4)
        col = gold[None, None, :] * (0.90 + 0.34 * n + 0.12 * g)[..., None]
        glint = np.clip(n * 3.0 - 0.55, 0, 1)[..., None]
        c.img[..., :3] = np.clip(col + (gold_hi - gold)[None, None, :] * glint * 0.7, 0, 255)

    def mix(c, rgb, mask, amount=1.0):
        L.blend(c.img, rgb, np.clip(mask, 0, 1) * amount)

    def shine(c, rgb, mask, strength=1.0):
        """Light on the glow skin where mask is set (the stronger of what is there and the new)."""
        a = np.clip(mask, 0, 1) * 255.0 * strength
        old = c.glow[..., 3]
        c.glow[..., :3] = np.where((a > old)[..., None], np.asarray(rgb, np.float32)[None, None, :], c.glow[..., :3])
        c.glow[..., 3] = np.maximum(old, a)

    # ---------------------------------------------------------------- the wheels: gold felloe with a groove and bevel lines, walnut spokes with grain, a gold hub
    R = CAR_WHEEL_R
    cxw, cyw = -R, CAR_AXLE_F

    def wheel(zc):
        def fn(c):
            X, Y, Z = L.model_coords(c)
            n = nz(c, 2.2, 31)
            if c.g.key[0] == 2:
                r = np.hypot(X - cxw, Y - cyw)
                ang = np.arctan2(Y - cyw, X - cxw)
                ph = np.mod(ang - math.radians(22.5), math.pi / 4)
                dsp = r * np.sin(np.minimum(ph, math.pi / 4 - ph))              # cm from the axis of the nearest spoke
                grain = 0.5 + 0.5 * np.sin(r * 1.9 + n * 7.0)
                wood = walnut[None, None, :] * (0.78 + 0.26 * grain + 0.30 * n + 0.18 * (1.0 - np.clip(dsp / 1.6, 0, 1)))[..., None]
                fel = L.smoothstep(R - 3.95, R - 3.75, r)[..., None]
                gold_col = gold[None, None, :] * (0.90 + 0.34 * n)[..., None]
                col = wood * (1 - fel) + gold_col * fel
                bevel = np.maximum(_band(r - (R - 0.5), 0.34), _band(r - (R - 3.3), 0.30))
                groove = _band(r - (R - 1.9), 0.42)
                col = col * (1 - 0.55 * groove[..., None] * fel) + gold_hi[None, None, :] * (0.55 * bevel[..., None] * fel)
                hub = L.smoothstep(4.95, 4.7, r)[..., None]
                hub_col = gold[None, None, :] * (0.95 + 0.25 * n)[..., None]
                ring = np.maximum(_band(r - 3.05, 0.3), _band(r - 4.55, 0.25))
                hub_col = hub_col * (1 - 0.5 * ring[..., None]) + gold_hi[None, None, :] * (0.25 * _band(r - 1.6, 0.35)[..., None])
                col = col * (1 - hub) + hub_col * hub
                c.img[..., :3] = np.clip(col, 0, 255)
                if c.g.key[1] == (1 if zc > 0 else -1):                          # the hub cap on the outer face holds a faint amber light
                    shine(c, CAR_GLOW, L.smoothstep(2.9, 1.6, r), 0.30)
            else:
                dz = np.abs(Z - zc)
                gold_col = gold[None, None, :] * (0.90 + 0.34 * n)[..., None]
                inlay = _band(dz, 1.05, 0.2)[..., None]
                wood = walnut[None, None, :] * (0.88 + 0.4 * n)[..., None]
                c.img[..., :3] = np.clip(gold_col * (1 - inlay) + wood * inlay, 0, 255)
                mix(c, gold_lo, _band(dz - 1.35, 0.16), 0.7)
        return fn
    w.paint_part(atlas, m.part("wheel_left"), wheel(-CAR_WHEEL_Z))
    w.paint_part(atlas, m.part("wheel_right"), wheel(CAR_WHEEL_Z))

    # ---------------------------------------------------------------- the canopy: royal blue, eight gold ribs round a rosette, a gold rim band, a deep blue lining
    cf = CAR_CANOPY_F
    stretch = CAR_CANOPY_PLAN[0] / CAR_CANOPY_PLAN[1]
    rim_x = -(CAR_CANOPY_U[0] + CAR_LIP + 0.15)

    def canopy(c):
        X, Y, Z = L.model_coords(c)
        ax, sg = c.g.key
        n = nz(c, 3.5, 41)
        if ax == 0:
            dep = _canopy_depth(Y, Z)
            qf, qz = Y - cf, Z * stretch
            rho = np.hypot(qf, qz)
            th = np.arctan2(qz, qf)
            dth = np.abs(np.mod(th + math.pi / 8, math.pi / 4) - math.pi / 8)
            d_rib = rho * np.sin(dth)
            if sg < 0:                                                           # the roof seen from above
                col = royal[None, None, :] * (1.0 + 0.16 * n + 0.10 * np.clip(1.0 - rho / 46.0, 0, 1))[..., None]
                c.img[..., :3] = np.clip(col, 0, 255)
                rib = _band(d_rib, 0.55) * L.smoothstep(6.0, 8.0, rho)
                mix(c, gold_lo, _band(d_rib, 0.95) * L.smoothstep(6.0, 8.0, rho), 0.55)
                mix(c, gold, rib)
                mix(c, gold_hi, _band(d_rib, 0.18) * L.smoothstep(6.0, 8.0, rho), 0.8)
                petal = 6.4 + 1.5 * np.cos(8 * th)
                rose = L.smoothstep(petal + 0.3, petal - 0.3, rho)
                mix(c, gold_lo, L.smoothstep(petal + 1.1, petal + 0.2, rho), 0.6)
                mix(c, gold, rose)
                mix(c, gold_hi, _band(rho - 3.4, 0.3) * rose, 0.7)
                for rr, hw in ((21.0, 0.4), (33.0, 0.4)):                         # two fine gold rings between the ribs
                    mix(c, gold, _band(rho - rr, hw), 0.9)
                mix(c, gold, L.smoothstep(2.2, 0.6, dep))                         # the rim of the dome
                mix(c, gold_lo, _band(dep - 2.6, 0.25), 0.5)
            else:                                                                 # the lining seen from below
                col = blue[None, None, :] * (1.0 + 0.14 * n)[..., None]
                c.img[..., :3] = np.clip(col, 0, 255)
                mix(c, gold, _band(d_rib, 0.4) * L.smoothstep(5.0, 7.0, rho), 0.55)
                mix(c, gold, _band(dep - 2.4, 0.5))
                mix(c, gold_hi, L.smoothstep(5.0, 3.0, rho), 0.9)
        else:
            lip = (X > rim_x)
            col = np.where(lip[..., None], gold[None, None, :] * (0.92 + 0.3 * n)[..., None], royal[None, None, :] * (1.0 + 0.16 * n)[..., None])
            c.img[..., :3] = np.clip(col, 0, 255)
            mix(c, gold_lo, _band(X - (rim_x + 1.3), 0.22), 0.75)
            mix(c, gold_hi, _band(X - (rim_x + 0.45), 0.2), 0.6)
    w.paint_part(atlas, m.part("canopy"), canopy)

    # ---------------------------------------------------------------- the posts: twisted gold rope with a ring at each end
    def posts(c):
        gilt(c, 51)
        if c.kind == "loft":
            spiral = 0.5 + 0.5 * np.sin(2 * math.pi * (c.S * 3.0 + c.B * 0.22))
            c.img[..., :3] = np.clip(c.img[..., :3] * (0.76 + 0.34 * spiral)[..., None], 0, 255)
            ends = np.minimum(c.B, c.g.ext[1] - c.B)
            mix(c, gold_hi, _band(ends - 1.1, 0.5), 0.8)
            mix(c, gold_lo, _band(ends - 2.1, 0.25), 0.6)
    w.paint_part(atlas, m.part("posts"), posts)

    # ---------------------------------------------------------------- the cabin: outer wall ivory with two gold beads and a vine of light, inner wall blue padding
    def cabin(c):
        if c.kind != "loft":
            gilt(c, 61)                                                           # the gilt plates at the tail ends of the walls
            return
        seg, u, ln = L.ring_coords(c)
        along = c.B + CABIN_START.get(id(c.g.origin), 0.0)                       # the way along the whole wall, from the tail of the left side
        n = nz(c, 3.0, 62)
        out = seg == 2
        inn = seg == 0
        top = seg == 1
        fill(c, ivory * 0.84, 63)
        col = c.img[..., :3].copy()
        # outer face: u runs from the top edge down
        fo = np.broadcast_to(np.asarray(ivory, np.float32), col.shape) * (1.0 + 0.10 * n)[..., None]
        col = np.where(out[..., None], fo, col)
        # inner face: padded blue with buttoned diamonds
        p = 4.6
        q1, q2 = (u + along) / p, (u - along) / p
        d1 = np.abs(np.mod(q1 + 0.5, 1.0) - 0.5) * p / math.sqrt(2)
        d2 = np.abs(np.mod(q2 + 0.5, 1.0) - 0.5) * p / math.sqrt(2)
        pad = blue[None, None, :] * (1.0 + 0.18 * n + 0.10 * np.clip(d1 * d2 / 3.0, 0, 1))[..., None]
        pad = pad * (1 - 0.30 * _band(np.minimum(d1, d2), 0.25)[..., None])
        col = np.where(inn[..., None], pad, col)
        tg = gold[None, None, :] * (0.90 + 0.34 * n)[..., None]
        col = np.where(top[..., None], tg, col)
        c.img[..., :3] = np.clip(col, 0, 255)
        btn = L.smoothstep(0.9, 0.6, np.hypot(d1, d2))
        mix(c, gold_hi, btn * inn, 0.95)
        mix(c, gold, _band(ln - u - 0.55, 0.5) * inn)                             # gold piping along the top edge of the padding
        # outer face details
        d_top, d_bot = u, ln - u
        for pos, hw in ((1.5, 0.42), (2.7, 0.2)):
            mix(c, gold, _band(d_top - pos, hw) * out)
        mix(c, gold, _band(d_bot - 1.4, 0.4) * out)
        mix(c, gold_lo, _band(d_top - 1.5, 0.95) * out, 0.35)
        zone = ln - 3.6 - 3.4
        mid = 3.6 + zone / 2
        amp = np.clip(zone / 2 - 0.9, 0.0, 2.4)
        dm, vm = along - CABIN_START["front"], u - ln / 2                         # the crest on the middle of the dash: where it is on the wall, in cm
        rc = np.hypot(dm, vm)
        clear = 1.0 - L.smoothstep(7.4, 6.4, rc) * (ln > 20.0)
        vine = _band(u - (mid + amp * np.sin(2 * math.pi * along / 9.0)), 0.36) * (zone > 2.0) * out * clear
        mix(c, gold, vine)
        leaf = L.smoothstep(0.85, 0.45, np.hypot(np.mod(along, 9.0) - 2.25, u - (mid + amp))) * (zone > 2.0) * out * clear
        mix(c, gold_hi, leaf, 0.9)
        crest = out * (ln > 20.0)
        petals = 1.7 + 1.35 * np.abs(np.cos(4.0 * np.arctan2(vm, dm)))
        ring1, ring2 = _band(rc - 5.4, 0.42), _band(rc - 4.0, 0.2)
        star = L.smoothstep(petals + 0.25, petals - 0.25, rc) * (rc > 0.9)
        mix(c, gold, np.maximum(ring1, ring2) * crest)
        mix(c, gold, star * crest)
        mix(c, gold_hi, L.smoothstep(1.0, 0.5, rc) * crest, 0.95)
        shine(c, CAR_GLOW, np.maximum(np.maximum(vine, leaf), np.maximum(np.maximum(ring1, star), L.smoothstep(1.0, 0.5, rc)) * crest), 0.33)
        mix(c, gold_hi, _band(np.minimum(u, ln - u) - 0.18, 0.14) * top, 0.6)     # a bright line along each edge of the gold top
        beads = L.smoothstep(0.75, 0.4, np.hypot(np.mod(along, 3.4) - 1.7, u - ln / 2)) * top
        mix(c, gold_hi, beads, 0.9)
    w.paint_part(atlas, m.part("cabin"), cabin)

    # ---------------------------------------------------------------- the chassis: ivory floor with a gold border, a gold stripe on its edge, walnut rails
    def chassis(c):
        X, Y, Z = L.model_coords(c)
        ax, sg = c.g.key
        fill(c, ivory, 71, 0.04)
        if ax == 0 and sg < 0:
            dep = _d_depth(Y, Z)
            mix(c, gold, _band(dep - 2.4, 0.5))
            mix(c, gold_hi, _band(dep - 2.4, 0.15), 0.6)
            mix(c, blue, _band(dep - 3.8, 0.28), 0.7)
        elif ax == 0:
            fill(c, ivory * 0.74, 72, 0.04)
            rail = (np.abs(np.abs(Z) - 14.0) < 1.75) & (Y > 3.0) & (Y < CAR_BODY - 8.0)
            fill_w = walnut[None, None, :] * (0.9 + 0.3 * nz(c, 2.0, 73))[..., None]
            c.img[..., :3] = np.where(rail[..., None], fill_w, c.img[..., :3])
        else:
            mix(c, gold, _band(X + 11.0, 0.55))
            mix(c, gold_lo, _band(X + 11.0, 0.9), 0.3)
            rail = X > -9.05
            wood = walnut[None, None, :] * (0.9 + 0.3 * nz(c, 2.0, 74))[..., None]
            c.img[..., :3] = np.where(rail[..., None], wood, c.img[..., :3])
    w.paint_part(atlas, m.part("chassis"), chassis)

    # ---------------------------------------------------------------- the benches: deep blue upholstery, buttoned diamonds, gold buttons and piping
    def bench(seed):
        def fn(c):
            X, Y, Z = L.model_coords(c)
            ax, sg = c.g.key
            fill(c, blue, seed, 0.08, 3.0)
            a = b = None
            if ax == 0 and sg < 0:
                a, b = Y, Z
            elif ax == 1:
                a, b = X, Z
            if a is not None:
                p = 5.2
                d1 = np.abs(np.mod((a + b) / p + 0.5, 1.0) - 0.5) * p / math.sqrt(2)
                d2 = np.abs(np.mod((a - b) / p + 0.5, 1.0) - 0.5) * p / math.sqrt(2)
                puff = np.clip(d1 * d2 / 4.0, 0, 1)
                c.img[..., :3] = np.clip(c.img[..., :3] * (0.86 + 0.22 * puff)[..., None], 0, 255)
                mix(c, (18, 30, 70), _band(np.minimum(d1, d2), 0.26), 0.55)
                mix(c, gold_hi, L.smoothstep(0.85, 0.55, np.hypot(d1, d2)), 0.95)
            else:
                mix(c, gold, _band(X + CAR_SEAT_U - 0.7, 0.4) if ax in (1, 2) else 0.0, 0.9)
        return fn
    w.paint_part(atlas, m.part("bench_front"), bench(81))
    w.paint_part(atlas, m.part("bench_rear"), bench(82))

    # ---------------------------------------------------------------- the draught pole, the yoke and the lantern: gilt, with rings
    def pole(c):
        X, Y, Z = L.model_coords(c)
        gilt(c, 91)
        if Y is not None:
            ph = np.mod(Y - CAR_BODY + 6.0, 12.0) - 6.0
            mix(c, gold_hi, _band(ph, 0.5), 0.75)
            mix(c, gold_lo, _band(np.abs(ph) - 1.4, 0.3), 0.7)
    w.paint_part(atlas, m.part("pole"), pole)
    w.paint_part(atlas, m.part("yoke"), lambda c: gilt(c, 92))
    w.paint_part(atlas, m.part("lantern"), lambda c: gilt(c, 93))

    # ---------------------------------------------------------------- the feathers: white, fading to sky blue at the tip, a pale quill and barbs
    def feathers(c):
        if c.kind != "loft":
            fill(c, plume, 101, 0.02)
            return
        seg, u, ln = L.ring_coords(c)
        T = np.clip(c.B / max(c.g.ext[1], 1e-6), 0, 1)                             # 0 at the root, 1 at the tip
        n = nz(c, 1.6, 102)
        tip = L.smoothstep(0.42, 0.95, T)[..., None]
        col = plume[None, None, :] * (1 - tip) + sky[None, None, :] * tip
        broad = (seg == 3) | (seg == 7)                                           # the two broad faces of the lens-shaped section; the rest are its chamfered edges
        lat = np.abs(u - ln / 2)                                                  # cm from the middle of a broad face: 0 on the quill
        barb = 0.5 + 0.5 * np.sin(c.B * 3.1 + lat * 2.4)                          # barbs sweep back from the quill
        col = col * (0.93 + 0.09 * barb + 0.05 * n)[..., None]
        c.img[..., :3] = np.clip(col, 0, 255)
        mix(c, (206, 214, 226), _band(np.where(broad, lat, 9.0), 0.28), 0.9)     # the quill: a pale line down the middle of each broad face
    w.paint_part(atlas, m.part("plumes"), feathers)

    # ---------------------------------------------------------------- the jewels: faceted amber, the strongest light of the glow skin
    L.paint_gem(atlas, m.part("gem"), CAR_JEWEL, CAR_GLOW, strength=0.55, core=0.06, facets=8)


# ================================================================ Vassal Book
# The target. The Vassal Book of Glass' world (wiki summaries: shield-hero.fandom.com/wiki/Vassal_Book): its holders are Kyo Ethnina and later Ethnobalt, it is the
# weapon that absorbs materials, and its gem is BLUE (the wiki's summary of the page says "a blue gem that can absorb materials"; the logic builder's page asked
# for a white-gold one without that fact, so the series wins here). Everything else follows the logic builder's page (weapon_book.md of the hvc worktree), which
# is design: a heavy hardback spellbook held closed and upright like a grimoire, deep teal-blue leather, gold corner caps and a gold border, four raised bands on
# the spine, a quill-and-feather emblem under the jewel, the jewel in a gold eight-point star, a leather clasp strap that hangs open with its gold buckle, a red
# ribbon from the foot and three loose pages standing out of the top (the book is alive and restless) whose lines of text glow. On the glow skin a ring of rune
# ticks and four channels run from the star to the corner caps, the way the book draws materials in.
# Class: handitem (a held book, largest dimension 0.5 to 0.9 blocks); the cover is drawn 0.78 block tall in the item's cube and 0.72 times that in the third person hand.
BOOK_H, BOOK_W, BOOK_T = 36.0, 26.0, 8.5       # a large spellbook: 36 cm tall, 26 wide, 8.5 thick (a heavy tome; the Book of Mispells is 28 x 17 x 6.5)
BOOK_BOARD = 0.9                               # a cover board's thickness
BOOK_OVER = 0.7                                # the boards stand this far beyond the pages on three sides
BOOK_FRAME_INSET = 2.0                         # the raised gold frame on the front cover
BOOK_GEM_R, BOOK_GEM_H = 4.1, 2.6              # the sapphire: radius of its base and height of its dome
BOOK_GEM_AT = (0.6, 21.0)                      # the jewel on the front cover (x from the middle, y up from the foot): above the middle, the emblem has room below it
BOOK_STAR_OUT, BOOK_STAR_IN = 9.2, 6.2         # the gold star the jewel sits in: radius over its eight points and between them
BOOK_CORNER = 5.2                              # a corner cap's leg length
BOOK_THIRD = 0.72                              # third person display scale (upright items use 0.55; a heavy tome is drawn larger)
BOOK_VIEW_H = 0.78                             # the cover is drawn this tall (blocks) in the item cube; the loose pages and the ribbon stand out beyond it
BOOK_COL = dict(teal=(31, 59, 82), teal_dark=(24, 46, 66), gold=(201, 162, 74), page=(239, 230, 207), ribbon=(166, 35, 46), strap=(58, 42, 32), sapphire=(40, 112, 240))
BOOK_GLOW = (70, 150, 255)                     # the sapphire's light
BOOK_PAGE_GLOW = (190, 236, 255)               # the lines of text on the loose pages: soft cyan-white
BOOK_SHEETS = ((-5.0, -13.0, 8.0, 10.5, 2.6), (0.0, 3.0, 11.0, 11.5, 2.0), (5.0, 15.0, 9.0, 10.0, 3.0))    # loose pages: root x, lean from upright (deg), height, width, curl toward the viewer (cm)
BOOK_SHEET_T = 0.2                             # a sheet of paper's thickness (cm)


def _book_feather(em, base_pt, ang_deg, length, width, z):
    """The quill-and-feather emblem: a leaf-shaped vane lying on the cover from its quill (base_pt) along ang_deg, raised a little off the leather."""
    a = math.radians(ang_deg)
    d = (math.cos(a), math.sin(a), 0.0)
    o = (base_pt[0], base_pt[1], z)
    st = [(0.0, 0.5, 0.55), (0.10 * length, 0.7, 0.6), (0.20 * length, 1.9, 0.6), (0.48 * length, width, 0.6), (0.76 * length, 0.82 * width, 0.6), (0.94 * length, 0.34 * width, 0.5), (length, 0.0, 0.0)]
    ml.loft(em, d, o, st, ml.rounded_rect(1.0, 1.0, 0.34, 1), up=(0, 0, 1))
    ml.bar(em, (o[0], o[1], z + 0.2), (o[0] + d[0] * length * 0.97, o[1] + d[1] * length * 0.97, z + 0.2), 0.55, 0.2, up=(0, 0, 1), taper=length * 0.14, end_scale=0.4)       # the rachis


def build_vassal_book():
    item_id = "vassal_book"
    sprites.write(item_id, L.ICON_DIR)
    m = ml.Mesh(item_id)
    x0, x1 = -BOOK_W / 2, BOOK_W / 2
    zf = BOOK_T / 2                                                         # the front cover's outer face
    gx, gy = BOOK_GEM_AT
    gold = BOOK_COL["gold"]
    # ---- the two boards, chamfered, teal leather
    fc = L.add(m, "cover_front", "leather", base=BOOK_COL["teal"], smooth=30, wear=0.5)
    ml.box(fc, (x0 - BOOK_OVER * 0.2, 0.0, zf - BOOK_BOARD), (x1 + BOOK_OVER, BOOK_H, zf), chamfer=0.3)
    bc = L.add(m, "cover_back", "leather", base=BOOK_COL["teal_dark"], smooth=30, wear=0.5)
    ml.box(bc, (x0 - BOOK_OVER * 0.2, 0.0, -zf), (x1 + BOOK_OVER, BOOK_H, -zf + BOOK_BOARD), chamfer=0.3)
    # ---- the page block, a little inset, and the spine with its four raised gold bands
    pg = L.add(m, "pages", "bone", base=BOOK_COL["page"], smooth=20, wear=0.3)
    ml.box(pg, (x0 + 0.9, BOOK_OVER, -zf + BOOK_BOARD), (x1 - 0.1, BOOK_H - BOOK_OVER, zf - BOOK_BOARD), chamfer=0.15)
    sp = L.add(m, "spine", "leather", base=BOOK_COL["teal_dark"], smooth=45, uv_mode="loft", wear=0.5)
    ml.loft(sp, "y", (x0 + 0.5, 0.0, 0.0), [(0.0, 1.0, 1.0), (BOOK_H, 1.0, 1.0)], ml.ellipse(2.4, BOOK_T / 2, 16))
    sb = L.add(m, "spine_bands", "gold", base=gold, smooth=40, wear=0.4)
    for k in range(4):
        y = BOOK_H * (0.14 + 0.24 * k)
        ml.lathe(sb, "y", (x0 + 0.5, y - 0.6, 0.0), [(0.0, 2.5), (0.3, 2.72), (0.9, 2.72), (1.2, 2.5)], sides=14, squash=BOOK_T / 2 / 2.4 * 1.0, caps=(False, False))
    # ---- the four gilded corner caps and the raised gold frame on the front cover
    cc = L.add(m, "corner_caps", "gold", base=gold, smooth=35, wear=0.5)
    tri = lambda sx, sy: [(0.0, 0.0), (sx * BOOK_CORNER, 0.0), (0.0, sy * BOOK_CORNER)]
    for (cx_, cy_, sx, sy) in ((x0 - 0.1, BOOK_H, 1, -1), (x1 + BOOK_OVER, BOOK_H, -1, -1), (x0 - 0.1, 0.0, 1, 1), (x1 + BOOK_OVER, 0.0, -1, 1)):
        pts = tri(sx, sy) if sx * sy < 0 else tri(sx, sy)[::-1]
        ml.loft(cc, "z", (cx_, cy_, zf - 0.02), [0.0, 0.42], pts)
    fr = L.add(m, "cover_frame", "gold", base=gold, smooth=40, wear=0.4)
    a = BOOK_FRAME_INSET
    fp = [(x0 + a + 0.6, a, zf), (x1 - a + 0.4, a, zf), (x1 - a + 0.4, BOOK_H - a, zf), (x0 + a + 0.6, BOOK_H - a, zf)]
    for i in range(4):
        ml.bar(fr, fp[i], fp[(i + 1) % 4], 0.7, 0.2, up=(0, 0, 1), section="rect")
    # ---- the gold eight-point star the jewel sits in, with a ring round the stone and eight small prongs over its edge
    gs = L.add(m, "gem_mount", "gold", base=(222, 184, 92), smooth=45, wear=0.3)
    L.polar_prism(gs, "z", (gx, gy, zf + 0.22), 0.3, lambda th: BOOK_STAR_IN + (BOOK_STAR_OUT - BOOK_STAR_IN) * (0.5 + 0.5 * math.cos(8.0 * th)) ** 2.2, n=64, bevel=0.04)
    L.torus(gs, (gx, gy, zf + 0.6), BOOK_GEM_R + 0.35, 0.65, (0, 0, 1), n=18, sides=5)
    for k in range(8):
        ang = 2 * math.pi * k / 8 + math.pi / 8
        L.spike(gs, (gx + (BOOK_GEM_R + 0.45) * math.cos(ang), gy + (BOOK_GEM_R + 0.45) * math.sin(ang), zf + 0.5), (-0.5 * math.cos(ang), -0.5 * math.sin(ang), 1.0), 2.1, 0.52, sides=3, r_tip=0.2)
    # ---- the quill-and-feather emblem under the star: a raised feather with its quill, and one small feather crossing it
    em = L.add(m, "emblem", "gold", base=(214, 176, 82), smooth=40, wear=0.4)
    _book_feather(em, (gx - 6.6, 6.0), 12.0, 13.2, 3.5, zf + 0.12)
    _book_feather(em, (gx + 6.0, 4.0), 168.0, 9.2, 2.3, zf + 0.12)
    # ---- the jewel: a sapphire cabochon (the wiki: the Vassal Book's gem is blue)
    gem = L.add(m, "gem", "crystal", base=BOOK_COL["sapphire"], glow=True, smooth=30)
    L.cabochon(gem, (gx, gy, zf + 0.15), BOOK_GEM_R, BOOK_GEM_H, axis="z", sides=16, squash=1.0)
    # ---- the clasp strap: dark brown leather hanging open from the fore-edge, a gold buckle on its end
    cl = L.add(m, "clasp_strap", "leather", base=BOOK_COL["strap"], smooth=30, wear=0.5)
    sx0, sy0, sz = x1 + BOOK_OVER, BOOK_H * 0.70, zf - 0.55
    strap = [(sx0 - 0.4, sy0), (sx0 + 1.2, sy0 + 0.1), (sx0 + 2.6, sy0 - 1.5), (sx0 + 3.2, sy0 - 4.6), (sx0 + 3.2, sy0 - 8.6), (sx0 + 2.8, sy0 - 12.6), (sx0 + 2.3, sy0 - 15.6)]
    ml.sweep(cl, [(px_, py_, sz) for (px_, py_) in strap], ml.rect_ring(1.0, 1.0), scales=[(0.4, 2.7)] * len(strap), up=(0, 0, 1))
    bx, by = strap[-1][0] + 0.15, strap[-1][1] - 1.6                            # the buckle: a gold frame lying in the strap's plane, with its tongue
    bk = L.add(m, "buckle", "gold", base=gold, smooth=35, wear=0.4)
    fz = 1.75
    for (p0, p1) in (((bx, by + 1.7, sz - fz), (bx, by + 1.7, sz + fz)), ((bx, by - 1.7, sz - fz), (bx, by - 1.7, sz + fz)), ((bx, by + 1.7, sz - fz), (bx, by - 1.7, sz - fz)), ((bx, by + 1.7, sz + fz), (bx, by - 1.7, sz + fz))):
        ml.bar(bk, p0, p1, 0.55, 0.27, up=(1, 0, 0), section="rect")
    ml.bar(bk, (bx, by + 1.7, sz), (bx, by - 1.1, sz), 0.4, 0.22, up=(1, 0, 0), taper=0.8, end_scale=0.4)
    # ---- the red ribbon bookmark from the foot, with a swallow-tail end
    rb = L.add(m, "ribbon", "cloth", base=BOOK_COL["ribbon"], smooth=25, uv_mode="loft", wear=0.2)
    pts = [(-3.5 + 0.5 * math.sin(t * 2.4), -9.5 * t + 0.6, 0.4 * t) for t in np.linspace(0, 1, 7)]
    ml.sweep(rb, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.9 * (1.0 - 0.1 * t) if t < 0.95 else 0.0, 0.13 if t < 0.95 else 0.0))
    # ---- three loose pages standing out of the top, fanned and curling toward the viewer
    lp = L.add(m, "loose_pages", "bone", base=BOOK_COL["page"], smooth=25, uv_mode="loft", wear=0.2)
    for k, (rx, lean, hgt, wid, curl) in enumerate(BOOK_SHEETS):
        a = math.radians(lean)
        pts = []
        for t in np.linspace(0.0, 1.0, 9):
            pts.append((rx + math.sin(a) * hgt * t, BOOK_H - BOOK_OVER - 1.2 + math.cos(a) * hgt * t, -0.6 + 0.6 * k + curl * t ** 2.4))
        ml.sweep(lp, pts, ml.rect_ring(1.0, 1.0), scales=lambda t, wid=wid: (wid * (1.0 - 0.12 * t) if t < 0.97 else wid * 0.5, BOOK_SHEET_T), up=(0, 0, 1))

    atlas = L.lay(m)
    _paint_book(m, atlas, (gx, gy, zf))
    sc = BOOK_VIEW_H / BOOK_H                                                       # the cover is 0.80 block tall in the item cube
    L.place_upright(m, sc)
    write_class(item_id, "handitem", "a heavy spellbook held upright: measured by its largest dimension (the cover 0.78 block tall in the cube, the loose pages and ribbon beyond it; 0.72 of that in the third person hand)")
    finish_weapon(item_id, m, atlas, glow=("FFFFFF", 0.14, 0.45, 1.0), upright=True, third_scale=BOOK_THIRD)


def _paint_book(m, atlas, centre):
    """The skin of the book: teal tooled leather with gold lines and scrolls, the rune ring that glows, the quill emblem, page edges, strap, the lit sapphire, and text that glows on the loose pages."""
    cx0, cy0, zf = centre
    gold = BOOK_COL["gold"]
    gold_hi = (255, 230, 150)
    teal = np.array(BOOK_COL["teal"], np.float32)
    teal_dark = np.array(BOOK_COL["teal_dark"], np.float32)

    def leather(c):
        if c.kind != "dom" or c.g.key[0] != 2:
            return
        n = w.noise2(c.img.shape[:2], 3, 61) - 0.5
        X, Y, _ = L.model_coords(c)
        r = np.hypot(X - cx0, Y - cy0) if c.g.key[1] > 0 else np.zeros_like(X)
        vign = (0.86 + 0.28 * L.smoothstep(18.0, 4.0, r))[..., None] if c.g.key[1] > 0 else 0.92
        col = teal[None, None, :] * (0.92 + 0.22 * n[..., None]) * vign
        c.img[..., :3] = np.clip(col, 0, 255)
    w.paint_part(atlas, m.part("cover_front"), leather)

    def front(d):
        if d.g.key[0] != 2:
            return
        z = zf
        # tooled border: two fine gold lines between the frame and the edge
        for ins, wd in ((1.0, 0.22), (3.2, 0.18)):
            xa, xb, ya, yb = -BOOK_W / 2 + ins + 0.6, BOOK_W / 2 - ins + 0.4, ins, BOOK_H - ins
            d.line_m([(xa, ya, z), (xb, ya, z), (xb, yb, z), (xa, yb, z), (xa, ya, z)], wd, rgb=gold + (210,), glow=(255, 215, 120, 60))
        # the rune ring: sixteen ticks round the star, glowing, and a thin glowing circle outside them
        for k in range(16):
            ang = math.radians(k * 22.5 + 11.25)
            r0, r1 = 10.0, 11.1
            d.line_m([(cx0 + r0 * math.cos(ang), cy0 + r0 * math.sin(ang), z), (cx0 + r1 * math.cos(ang), cy0 + r1 * math.sin(ang), z)], 0.3, rgb=(120, 190, 255, 255), glow=(90, 170, 255, 235))
        d.line_m([(cx0 + 11.7 * math.cos(2 * math.pi * k / 48), cy0 + 11.7 * math.sin(2 * math.pi * k / 48), z) for k in range(49)], 0.26, rgb=(120, 190, 255, 220), glow=(90, 170, 255, 170))
        # four channels from the ring to the corner caps, with a node halfway: the way the book draws materials in
        for (sx, sy) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            tx, ty = cx0 + sx * 8.8, (BOOK_H - 4.0 if sy > 0 else 4.2)
            ang = math.atan2(ty - cy0, tx - cx0)
            p0 = (cx0 + 11.7 * math.cos(ang), cy0 + 11.7 * math.sin(ang))
            d.line_m([(p0[0], p0[1], z), (tx, ty, z)], 0.26, rgb=(120, 190, 255, 255), glow=(90, 170, 255, 210))
            mid = ((p0[0] + tx) / 2, (p0[1] + ty) / 2)
            d.ellipse_m((mid[0], mid[1], z), 0.5, 0.5, rgb=(160, 215, 255, 255), glow=(130, 200, 255, 255))
        # gold filigree scrolls in the four corners
        for (sx, sy) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            ox, oy = sx * (BOOK_W / 2 - 7.5) + 0.4, BOOK_H / 2 + sy * (BOOK_H / 2 - 8.5)
            pts = []
            for t in np.linspace(0, 3.2 * math.pi, 30):
                rr = 0.25 + 0.38 * t
                pts.append((ox + sx * rr * math.cos(t), oy + sy * rr * math.sin(t), z))
            d.line_m(pts, 0.2, rgb=gold + (200,))
    L.decal(atlas, m.part("cover_front"), "z", 1, front)

    def back(c):
        if c.kind != "dom" or c.g.key[0] != 2:
            return
        n = w.noise2(c.img.shape[:2], 3, 62) - 0.5
        c.img[..., :3] = np.clip(teal_dark[None, None, :] * (0.9 + 0.22 * n[..., None]), 0, 255)
    w.paint_part(atlas, m.part("cover_back"), back)

    def back_lines(d):
        if d.g.key[0] != 2:
            return
        z = -zf
        d.line_m([(-BOOK_W / 2 + 1.6, 1.6, z), (BOOK_W / 2 - 1.2, 1.6, z), (BOOK_W / 2 - 1.2, BOOK_H - 1.6, z), (-BOOK_W / 2 + 1.6, BOOK_H - 1.6, z), (-BOOK_W / 2 + 1.6, 1.6, z)], 0.24, rgb=gold + (210,))
        for r_, wd in ((7.0, 0.3), (5.0, 0.22)):
            d.line_m([(0.6 + r_ * math.cos(2 * math.pi * k / 40), BOOK_H / 2 + r_ * math.sin(2 * math.pi * k / 40), z) for k in range(41)], wd, rgb=gold + (220,), glow=(255, 215, 120, 50))
    L.decal(atlas, m.part("cover_back"), "z", -1, back_lines)

    def pages(c):
        n = np.sin(c.B * 11.0 + c.A * 0.15)[..., None] * 9
        base = np.array(BOOK_COL["page"], np.float32) * (0.94 + 0.1 * (w.noise2(c.img.shape[:2], 3, 63)[..., None] - 0.5))
        c.img[..., :3] = np.clip(base + n, 0, 255)
        w.set_glow(c, (255, 214, 140), 0.07, mask=np.full(c.img.shape[:2], 0.6, np.float32), core=0.0)      # the page block's edge: a very faint warm light
    w.paint_part(atlas, m.part("pages"), pages)

    def spine(c):
        n = w.noise2(c.img.shape[:2], 3, 64) - 0.5
        c.img[..., :3] = np.clip(teal_dark[None, None, :] * (0.92 + 0.2 * n[..., None]), 0, 255)
        if c.kind == "loft":
            ph = np.mod(c.B / BOOK_H, 0.24)
            L.blend(c.img, gold, np.exp(-(((ph - 0.12) / 0.02) ** 2)) * 0.5)           # tooled gold lines between the bands
    w.paint_part(atlas, m.part("spine"), spine)

    def strap(c):
        n = w.noise2(c.img.shape[:2], 2, 65) - 0.5
        base = np.array(BOOK_COL["strap"], np.float32) * (0.9 + 0.3 * n[..., None])
        c.img[..., :3] = np.clip(base, 0, 255)
        if c.kind == "loft":
            L.blend(c.img, (150, 112, 70), (np.abs(c.S - 0.25) < 0.012) * 0.7 + (np.abs(c.S - 0.75) < 0.012) * 0.7)    # stitching along both edges
    w.paint_part(atlas, m.part("clasp_strap"), strap)

    def ribbon(c):
        col = np.array(BOOK_COL["ribbon"], np.float32)
        sheen = 0.88 + 0.16 * np.sin(c.S * 6.28 * 2)[..., None]
        c.img[..., :3] = np.clip(col * sheen, 0, 255)
    w.paint_part(atlas, m.part("ribbon"), ribbon)

    def sheets(c):
        n = w.noise2(c.img.shape[:2], 2, 66) - 0.5
        base = np.array(BOOK_COL["page"], np.float32) * (0.95 + 0.12 * n[..., None])
        c.img[..., :3] = np.clip(base, 0, 255)
        if c.kind != "loft":
            return
        seg, u, ln = L.ring_coords(c)
        face = (seg == 1) | (seg == 3)
        row = np.floor(c.B / 1.35)
        rng = np.mod(np.sin(row * 12.9898 + c.g.ext[1] * 3.1) * 43758.5453, 1.0)
        extent = 0.30 + 0.62 * rng
        inside = (u / np.maximum(ln, 1e-3) > 0.10) & (u / np.maximum(ln, 1e-3) < 0.10 + 0.82 * extent)
        line = (np.abs(np.mod(c.B, 1.35) - 0.675) < 0.2) & inside & face
        L.blend(c.img, (70, 78, 96), line * 0.75)
        w.set_glow(c, BOOK_PAGE_GLOW, 0.55, mask=line.astype(np.float32) * 0.9, core=0.25)
    w.paint_part(atlas, m.part("loose_pages"), sheets)
    L.glow_part(atlas, m.part("gem_mount"), (255, 214, 140), 0.14, 0.05)
    L.glow_part(atlas, m.part("cover_frame"), (255, 214, 140), 0.10, 0.05)
    L.paint_gem(atlas, m.part("gem"), BOOK_COL["sapphire"], BOOK_GLOW, strength=0.85, core=0.2, facets=1)

    def emblem_barbs(d):                                                          # fine barbs on the two raised feathers: short dark strokes slanting away from the rachis
        if d.g.key[0] != 2:
            return
        z = zf + 0.5
        for (base_pt, ang, length, width) in (((cx0 - 6.6, 6.0), 12.0, 13.2, 3.5), ((cx0 + 6.0, 4.0), 168.0, 9.2, 2.3)):
            a = math.radians(ang)
            dx, dy = math.cos(a), math.sin(a)
            nx, ny = -dy, dx
            for k in range(1, 14):
                t = 0.14 + 0.058 * k
                if t > 0.93:
                    break
                px, py = base_pt[0] + dx * length * t, base_pt[1] + dy * length * t
                wd = width * math.sin(math.pi * min(1.0, 0.18 + 0.82 * t)) * 0.42
                for sgn in (1, -1):
                    d.line_m([(px, py, z), (px + dx * 0.9 * wd + sgn * nx * wd, py + dy * 0.9 * wd + sgn * ny * wd, z)], 0.14, rgb=(120, 88, 30, 220))
    L.decal(atlas, m.part("emblem"), "z", 1, emblem_barbs)


BUILDERS = {"vassal_scythe": build_vassal_scythe, "vassal_katana": build_vassal_katana, "vassal_fan": build_vassal_fan, "vassal_carriage": build_vassal_carriage, "vassal_book": build_vassal_book}


def main(only=None):
    ids = only or list(BUILDERS)
    for i in ids:
        BUILDERS[i]()
    write_classes_fragment()


def write_classes_fragment():
    """tools/heroweapon/classes_hart3.json: the size class of each weapon, in the shape of tools/relics/weapon_classes.json's `weapons` map, for the lead to merge."""
    path = L.CLASSES_OUT
    old = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            old = json.load(f).get("weapons", {})
    for k, (cls, why) in CLASSES.items():
        old[k] = {"class": cls, "why": why}
    L.write_json(path, {"_doc": "Size classes of the hart3 weapons (tools/heroweapon/art_hart3.py); merge into tools/relics/weapon_classes.json 'weapons', then run tools/relics/audit_weapon_scale.py --write.",
                        "weapons": {k: old[k] for k in sorted(old)}})


if __name__ == "__main__":
    main(sys.argv[1:])
