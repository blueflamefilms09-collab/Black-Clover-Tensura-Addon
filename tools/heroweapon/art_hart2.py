"""art_hart2: the 3D models, skins, glow skins, sprites and item models of the SEVEN STAR WEAPONS of The Rising of the Shield Hero (1.62.0).

    python3 -B tools/heroweapon/art_hart2.py [id ...]          (no ids = all seven; idempotent and deterministic)
    HART2_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart2.py vassal_hammer --preview      (also renders the sheet of each)
    HART2_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart2.py vassal_hammer --views        (also renders the textured third-person and first-person software views)
    python3 -B tools/heroweapon/art_hart2.py --selfcheck       (builds everything twice and compares the hashes of every output)

Why: the owner wants every hero weapon to be a real 3D model in the hand, built like the Genesis Demon-Slayer (the owner's rule 7 of the repository rules), with its 16 x 16 sprite kept as
the inventory icon and a gem, the series' central jewel, glowing in its colour. These are the seven weapons of Raphtalia's world: Hammer, Claw, Axe, Whip, Staff,
Gauntlet and Projectile. They are built in the order of the brief, simplest silhouette first, with the owner's method (tools/relics/mesh_lib.py): real cross
sections lofted, swept and turned, small raised parts as their own named parts, smoothed per-vertex normals, a painted skin of 512 px and a second additive glow
skin, 8 to 14 named parts and at most 2800 quads each. The shared tools (materials, the gem and its seven-sided setting, the Big Dipper engraving, the sprite
canvas, the item model writer, the previews, the audit) are in art_hart2_kit.py.

Per weapon this writes, under src/main/resources/assets/nusmp:
  weapon_meshes/<id>.txt                       the mesh (per-vertex "qn" lines, `grip` and `# part` lines written by mesh_lib)
  textures/entity/weapon/<id>.png, <id>_glow.png   the painted skin and its glow skin (512 px, UVs in the renderer's 128 space)
  textures/item/<id>.png                        the 16 x 16 sprite (the inventory icon), drawn by hand-built pixel code, not a render
  models/item/<id>.json, <id>_icon.json         the in-hand model (builtin/entity) and the flat inventory model
and tools/heroweapon/classes_hart2.json (the size class of each id: the lead merges it into the class table).

The family (design, not canon art; the sources are in docs/heroweapon/art_hart2.md): the colours of each weapon (metal, wood, trim and above all the gem and its glow)
are the ones the logic builders wrote in their look pages (docs/heroweapon/weapon_<name>.md of the hammer, claw, axe, whip, staff, gauntlet and projectile), so that the
colour a weapon glows in the hand is the colour of its skill effects. What the family shares is mine: the Seven Star name is the Big Dipper, so each gem sits in a
seven-sided setting and the Big Dipper is engraved in thin glowing lines where the weapon has a plate to carry it. Every weapon is built with mesh -x forward and its tip
up-forward on the sprite's diagonal (docs/weapon_scale_orientation.md), the gem and the engraving on BOTH faces of the mesh (a hand shows its -z face to onlookers and its
+z face to the first-person camera).

Every proportion is a named constant with a comment saying where it comes from. The units of a mesh are free (the final size is `apparent`, the length in blocks as a
viewer sees the weapon in the hand, chosen from the class range of docs/weapon_scale_orientation.md); here one unit is about a centimetre of the weapon as drawn.
"""
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import art_hart2_kit as K                                          # noqa: E402
from art_hart2_kit import shade, mix                               # noqa: E402

ml, w, V = K.ml, K.w, K.V
K.register_materials()

CLASS_OF = {"vassal_hammer": "hammer", "vassal_claw": "shortsword", "vassal_axe": "battleaxe", "vassal_whip": "coilwhip",
            "vassal_staff": "staff", "vassal_gauntlet": "handitem", "vassal_projectile": "dagger"}


# ================================================================ 1. the Vassal Hammer
# Target: the look the logic builder asks for in docs/heroweapon/weapon_hammer.md (hva): a big two-handed war hammer, a square block head on a straight haft, slightly flared at the
# faces, a short collar under the head and a flat pommel cap; nothing curved, nothing thin, no spikes on the face (the blunt, armour-breaking weapon); forged dark iron with
# brass; ONE large amber cabochon (the forge-fire coal, glow #FF9A2E) in a brass bezel on the head's side plate, and the seven-star mark (the seven dots of the Plough, the Big
# Dipper) on the striking face. Canon: vassal to the Legendary Shield with the Claw; the wiki lists the Small Hammer only and describes no look (summaries), so this is design.
# Design choices on top of that doc: a hub (a taller block with brass rims) between the two blocks carries the gem on both cheeks so it shows from either side, the bezel has
# seven sides (the family mark; the doc says eight), and the rear face (the face the first-person camera looks at: mesh +x comes toward the viewer) carries the seven-star mark
# and a ring of seven rivets too. Mesh -x is the striking face; the front block is longer than the rear (the peen), so the mass sits forward as the orientation rule asks.
HAMMER_APPARENT = 1.62          # blocks in the hand: class hammer is 1.3 - 1.8; the doc asks for about 1.6
HAMMER_GRIP = 0.25              # the class grip share: the hand stays at a quarter of the length from the butt
H_IRON, H_PLATE_STEEL, H_BRASS = K.hx("4A4F5C"), K.hx("7A8090"), K.hx("B8893A")      # the doc's palette: forged dark iron, bevels and face plates, worn brass
H_WALNUT, H_LEATHER, H_RIVET = K.hx("4B3220"), K.hx("2E2018"), K.hx("C9CED8")
H_AMBER, H_AMBER_GLOW = K.hx("FF9A2E"), K.hx("FF9A2E")
H_BUTT = -11.0                  # the pommel cap's lowest point
H_HAFT_TOP = 146.0              # the haft runs up into the head
H_HEAD_YC = 144.0               # the head's centre height: 144 + half the hub 31 = 175, the weapon is 186 long
H_BLOCK_HY, H_BLOCK_HZ = 27.0, 24.5                  # half height (y) and half depth (z) of the two blocks: a head 54 tall and 49 thick (a square-ish block, 0.34 on the face)
H_HUB_HY, H_HUB_HZ = 31.0, 28.0                      # the hub between the blocks stands 4 higher and 3.5 wider all round
H_FRONT_X, H_HUB_X, H_REAR_X = (-36.0, -11.0), (-11.0, 11.0), (11.0, 28.0)    # front block 25 long, hub 22, rear block 17: the front is longer than the rear
H_PLATE = 5.5                   # the face plates stand this far beyond the blocks (the striking face and the peen); head 33 + 41 + 11 = 85 long, 0.58 blocks in the hand
H_GRIP_Y = (18.0, 62.0)         # the leather grip (the hand is at a quarter of the 186 long weapon: y = 35)
H_MED_R, H_BEZEL_R, H_GEM_R = 13.5, 9.0, 6.6         # the brass medallion on each cheek, the seven-sided bezel, the cabochon (a 27 wide medallion on a 39 high flat of the hub)


def build_hammer():
    item = "vassal_hammer"
    m = ml.Mesh(item)
    yc = H_HEAD_YC
    sec_block = ml.octagon(2.0, 2.0, 0.50)                                # the blocks' section, half extents 1: half the half size cut off the corners
    sec_hub = ml.octagon(2.0, 2.0, 0.36)                                  # the hub has big flats: the medallion must sit flat on it
    # ---- haft: dark walnut, a long swelling lathe (slim in the grip, strong under the head)
    haft = K.add(m, "haft", "hero_walnut", base=H_WALNUT, smooth=60, uv_mode="loft", wear=0.6)
    ml.lathe(haft, "y", (0, 0, 0), [(0.0, 3.2), (20.0, 3.5), (60.0, 3.3), (100.0, 3.8), (126.0, 4.5), (H_HAFT_TOP, 5.0)], sides=12)
    # ---- grip: black-brown leather, spiral wrapped (the spiral is the brass wire below)
    grip = K.add(m, "grip_wrap", "hero_dark_leather", base=H_LEATHER, smooth=60, uv_mode="loft", wear=0.7)
    y0, y1 = H_GRIP_Y
    ml.loft(grip, "y", (0, 0, 0), [(y0, 0.92, 0.92), (y0 + 4, 1.0, 1.0), ((y0 + y1) / 2, 1.05, 1.05), (y1 - 4, 1.0, 1.0), (y1, 0.92, 0.92)], ml.circle(4.1, 12))
    # ---- brass fittings: bands at the ends of the grip and under the head, two straps (langets) up the haft, a wire wound over the grip
    fit = K.add(m, "haft_fittings", "hero_brass", base=H_BRASS, smooth=50, wear=0.6)
    for yy in (y0 - 3.0, y1 + 0.5, 100.0):
        ml.collar(fit, yy, 4.9, 3.0, lip=0.8, sides=10)
    for sgn in (-1, 1):
        ml.bar(fit, (0, 100.0, sgn * 3.9), (0, 122.0, sgn * 4.5), 4.6, 1.0, up=(0, 0, sgn), taper=6.0, end_scale=0.4)
    wire = [(4.42 * math.cos(2 * math.pi * 6.5 * t), y0 + 2.0 + (y1 - y0 - 4.0) * t, 4.42 * math.sin(2 * math.pi * 6.5 * t)) for t in np.linspace(0, 1, 42)]
    ml.sweep(fit, wire, ml.circle(0.45, 4), scales=lambda t: (1.0, 1.0) if 0.02 < t < 0.98 else (0.3, 0.3), caps=(False, False))
    # ---- the pommel cap: a flat, heavy brass cap (a short rounded bulb, no spike: the doc asks for a flat pommel cap)
    pom = K.add(m, "pommel_cap", "hero_brass", base=H_BRASS, smooth=40, wear=0.6)
    ml.lathe(pom, "y", (0, 0, 0), [(H_BUTT, 3.4), (H_BUTT + 1.4, 5.6), (-4.0, 6.0), (1.0, 5.8), (5.0, 5.4), (9.0, 4.0), (11.0, 3.4)], sides=10)
    # ---- the short collar under the head: a brass cone flaring into the head's underside
    col = K.add(m, "neck_collar", "hero_brass", base=H_BRASS, smooth=45, wear=0.6)
    yb = yc - H_HUB_HY
    ml.lathe(col, "y", (0, 0, 0), [(yb - 14.0, 5.2), (yb - 7.0, 6.6), (yb - 1.5, 10.5), (yb + 2.0, 12.0)], sides=12)
    # ---- the two blocks: forged dark iron, octagonal prisms, a little waisted (forged, not milled)
    bl = K.add(m, "head_blocks", "hero_dark_steel", base=H_IRON, smooth=30, wear=0.7)
    (fa, fb), (ha, hb), (ra, rb) = H_FRONT_X, H_HUB_X, H_REAR_X
    ml.loft(bl, "x", (0, yc, 0), [(fa, H_BLOCK_HY * 0.93, H_BLOCK_HZ * 0.93), (fa + 4, H_BLOCK_HY, H_BLOCK_HZ), (fb - 7, H_BLOCK_HY * 0.97, H_BLOCK_HZ * 0.97), (fb + 1, H_BLOCK_HY * 0.97, H_BLOCK_HZ * 0.97)], sec_block)
    ml.loft(bl, "x", (0, yc, 0), [(ra - 1, H_BLOCK_HY * 0.97, H_BLOCK_HZ * 0.97), (ra + 5, H_BLOCK_HY * 0.97, H_BLOCK_HZ * 0.97), (rb - 3, H_BLOCK_HY, H_BLOCK_HZ), (rb, H_BLOCK_HY * 0.93, H_BLOCK_HZ * 0.93)], sec_block)
    # ---- the hub: a taller block between them, lighter iron, framed by brass rims
    hub = K.add(m, "head_hub", "hero_steel", base=K.hx("5E6474"), smooth=30, wear=0.6, along="u")
    ml.loft(hub, "x", (0, yc, 0), [(ha, H_HUB_HY * 0.94, H_HUB_HZ * 0.94), (ha + 2, H_HUB_HY, H_HUB_HZ), (hb - 2, H_HUB_HY, H_HUB_HZ), (hb, H_HUB_HY * 0.94, H_HUB_HZ * 0.94)], sec_hub)
    bd = K.add(m, "head_rim_bands", "hero_brass", base=H_BRASS, smooth=35, wear=0.6)
    for xb in (ha, hb):
        ml.loft(bd, "x", (0, yc, 0), [(xb - 2.4, H_HUB_HY * 0.97, H_HUB_HZ * 0.97), (xb - 1.4, H_HUB_HY * 1.06, H_HUB_HZ * 1.06), (xb + 1.4, H_HUB_HY * 1.06, H_HUB_HZ * 1.06), (xb + 2.4, H_HUB_HY * 0.97, H_HUB_HZ * 0.97)], sec_hub)
    # ---- face plates: the striking face (front) and the blunt peen (rear), bevelled plates of the lighter steel
    fp = K.add(m, "face_plates", "hero_steel", base=H_PLATE_STEEL, smooth=30, wear=0.8)
    xf, xr = fa - H_PLATE, rb + H_PLATE
    ml.loft(fp, "x", (0, yc, 0), [(xf, H_BLOCK_HY * 0.88, H_BLOCK_HZ * 0.88), (xf + 1.2, H_BLOCK_HY * 1.06, H_BLOCK_HZ * 1.06), (fa - 1.6, H_BLOCK_HY * 1.10, H_BLOCK_HZ * 1.10), (fa + 0.5, H_BLOCK_HY * 1.0, H_BLOCK_HZ * 1.0)], sec_block)
    ml.loft(fp, "x", (0, yc, 0), [(rb - 0.5, H_BLOCK_HY * 1.0, H_BLOCK_HZ * 1.0), (rb + 1.5, H_BLOCK_HY * 1.08, H_BLOCK_HZ * 1.08), (xr - 1.2, H_BLOCK_HY * 1.04, H_BLOCK_HZ * 1.04), (xr, H_BLOCK_HY * 0.88, H_BLOCK_HZ * 0.88)], sec_block)
    # ---- the medallion on each cheek (a brass disc with seven rivets round it), the seven-sided bezel and the amber cabochon
    med = K.add(m, "medallions", "hero_brass", base=H_BRASS, smooth=40, wear=0.5)
    bez = K.add(m, "gem_bezels", "hero_old_gold", base=K.hx("A87A30"), smooth=30, wear=0.4)
    gem = K.add(m, "gems", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    riv = K.add(m, "rivets", "hero_white_steel", base=H_RIVET, smooth=30, wear=0.5)
    zc = H_HUB_HZ - 0.3                                                   # the hub's cheek plane (the flat of the octagon)
    for sgn in (-1, 1):
        ax = "z" if sgn > 0 else "-z"
        ml.lathe(med, ax, (0.0, yc, sgn * zc), [(-1.0, H_MED_R), (0.0, H_MED_R), (1.2, H_MED_R * 0.97), (2.4, H_MED_R * 0.88)], sides=20)
        K.rivet_ring(riv, (0.0, yc, sgn * (zc + 1.6)), H_MED_R - 2.2, 1.35, n=7, axis=ax)
        K.star_bezel(bez, (0.0, yc, sgn * (zc + 2.0)), H_BEZEL_R, 3.0, axis=ax)
        K.cabochon(gem, (0.0, yc, sgn * (zc + 4.0)), H_GEM_R, 4.4, axis=ax, sides=14)
    K.rivet_ring(riv, (xr - 0.3, yc, 0.0), 19.0, 1.5, n=7, axis="x")      # the rear face: seven rivets in the seven-star ring
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, H_AMBER)
    # the seven-star mark: the Big Dipper on the striking face (the front plate, mesh -x, seen by the one it is swung at) in amber dots and thin lines; the same on the rear face
    # (the face the first-person camera sees) and, fainter, on the cheeks round the gem; a thin amber line along the rim of each face plate
    for sgn, scale_ in ((-1, 1.7), (1, 1.5)):
        K.dipper_engraving(atlas, fp, "x", sgn, (0.0, yc), scale_, rotate_deg=(-14.0 if sgn < 0 else 14.0), glow_rgb=H_AMBER_GLOW, width=0.7, dot_r=1.5, strong=0.9, mirror=(sgn > 0))
        rim_r = H_BLOCK_HY * 0.80
        octo = [(rim_r * math.cos(math.radians(22.5 + 45 * i)) * (H_BLOCK_HZ / H_BLOCK_HY), yc + rim_r * math.sin(math.radians(22.5 + 45 * i))) for i in range(9)]
        K.engrave(atlas, fp, "x", sgn, segments=list(zip(octo[:-1], octo[1:])), width=0.6, dark=0.5, glow_rgb=H_AMBER_GLOW, glow_strength=0.32)
    for part in ("head_blocks", "head_hub"):
        for sgn in (1, -1):
            K.dipper_engraving(atlas, m.part(part), "z", sgn, (0.0, yc), 2.1, rotate_deg=-5.0, glow_rgb=H_AMBER_GLOW, width=0.7, dot_r=1.4, strong=0.55)
    return m, atlas


# ================================================================ 2. the Vassal Axe
# Target: the look of docs/heroweapon/weapon_axe.md (hva): a broad single-bit battle axe, a heavy steel head with a long, slightly curved cutting edge (mesh -x) that drops into a
# pronounced beard (the lower corner hooks down), a thick squared poll on the back (+x) with a short back spike, on a straight haft: a woodcutter's silhouette made noble; the
# blade the largest shape, the edge the first thing you see; steel blade with a bright edge bevel and a dark green etched channel across the cheek, a darker steel poll and spike,
# dark iron collars and rings with gold trim, a pale ash haft with a black-brown wrap, ONE emerald cabochon (glow #35E07C) in a bezel at the eye of the axe (the hub, where the
# blade meets the haft), the seven-star mark (seven tiny dots) on the poll. Canon: vassal to the Legendary Sword with the Gauntlet; forms Small Axe, Pipe Axe, Animal Needle Axe,
# Curse Axe; skills Great Quake, Guillotine ... (wiki summaries): no picture described, so the shape is design. The gem is on both cheeks of the hub so it shows from either side.
AXE_APPARENT = 1.82             # blocks in the hand: class battleaxe is 1.6 - 2.0 (head at least 0.75 across); the doc aims for 1.8
AXE_GRIP = 0.25
A_STEEL, A_BEVEL, A_INLAY = K.hx("9AA3B2"), K.hx("D5DCE8"), K.hx("3F8F66")     # the doc's palette: the blade, its edge bevel, the cheek inlay
A_POLL, A_IRON, A_GOLD = K.hx("6E7686"), K.hx("2F3238"), K.hx("B8893A")        # the poll and spike, the collars and rings, the trim
A_ASH, A_LEATHER = K.hx("9C7A4E"), K.hx("2E2018")                              # the haft, the grip wrap
A_EMERALD = K.hx("35E07C")
A_BUTT, A_HEAD_Y = -12.0, 150.0 # the butt cap's spike tip (the lowest point) and the height of the hub's centre
A_HUB_HY, A_HUB_HX, A_HUB_HZ = 20.0, 11.0, 11.0      # half height / width / depth of the hub block the bit and the poll grow out of (a 40 tall, 22 wide block)
A_GRIP_Y = (18.0, 74.0)         # the black-brown grip wrap (the hand at a quarter of the 205 long weapon is y = 39)
# the bearded bit, measured from the haft axis outwards: root half heights above and below the centre line, the heights at the two horns and where they are, the edge
# (the front-most point) and the root thickness. 112 tall at the horns, 60 reach from the hub: far above the 0.75 head minimum of the class.
A_BIT = dict(d0=6.0, h_hub_top=15.0, h_hub_bot=19.0, h_top=44.0, d_top=40.0, h_bot=68.0, d_bot=50.0, d_tip=66.0, t_hub=9.0)
A_POLL_X = (11.0, 31.0, 36.0)   # the poll: from the hub to its end block, then the face plate; the rear face carries the seven-star mark and the back spike
A_SPIKE = 15.0                  # the short back spike on the poll
A_GEM_R, A_BEZEL_R = 6.4, 9.2   # the emerald and its seven-sided setting on the hub cheek


def bit_profile(k):
    """The outline of the bit as functions of the distance d from the haft axis, usable on numbers and numpy arrays: half height above the centre line, half height
    below it, and the thickness. Each side rises from the hub in a flare (a smooth step) to its horn, then falls on a convex arc to the front-most point."""
    d0, d_tip = k["d0"], k["d_tip"]

    def side(d, h_hub, h_horn, d_horn):
        d = np.asarray(d, np.float64)
        t = np.clip((d - d0) / (d_horn - d0), 0.0, 1.0)
        rise = h_hub + (h_horn - h_hub) * (t * t * (3 - 2 * t)) ** 1.25
        u = np.clip((d - d_horn) / (d_tip - d_horn), 0.0, 1.0)
        fall = h_horn * np.sqrt(np.maximum(0.0, 1.0 - u * u))
        return np.where(d <= d_horn, rise, fall)

    def top(d):
        return side(d, k["h_hub_top"], k["h_top"], k["d_top"])

    def bot(d):
        return side(d, k["h_hub_bot"], k["h_bot"], k["d_bot"])

    def thick(d):
        d = np.asarray(d, np.float64)
        body = k["t_hub"] * (1.0 - np.clip((d - d0) / (d_tip - d0), 0.0, 1.0) ** 1.1) + 0.4
        return np.minimum(body, 0.25 * (top(d) + bot(d)) + 0.25)
    return top, bot, thick


def build_axe():
    item = "vassal_axe"
    m = ml.Mesh(item)
    yc = A_HEAD_Y
    # ---- haft: a straight octagonal shaft of pale ash, strong under the head
    haft = K.add(m, "haft", "hero_walnut", base=A_ASH, smooth=30, wear=0.6, along="v")
    ml.loft(haft, "y", (0, 0, 0), [(0.0, 3.0, 3.0), (30.0, 3.5, 3.5), (80.0, 3.3, 3.3), (120.0, 3.9, 3.9), (yc, 4.5, 4.5)], ml.octagon(2.0, 2.0, 0.55))
    grip = K.add(m, "grip_wrap", "hero_dark_leather", base=A_LEATHER, smooth=60, uv_mode="loft", wear=0.7)
    y0, y1 = A_GRIP_Y
    ml.loft(grip, "y", (0, 0, 0), [(y0, 0.93, 0.93), (y0 + 4, 1.0, 1.0), ((y0 + y1) / 2, 1.05, 1.05), (y1 - 4, 1.0, 1.0), (y1, 0.93, 0.93)], ml.circle(4.4, 12))
    # ---- dark iron fittings with gold trim: rings at the ends of the grip and under the head, the butt cap with its spike, the langets
    fit = K.add(m, "haft_fittings", "hero_dark_steel", base=A_IRON, smooth=40, wear=0.6)
    for yy in (y0 - 3.0, y1 + 0.5, 112.0):
        ml.collar(fit, yy, 5.2, 3.0, lip=0.8, sides=10)
    ml.lathe(fit, "y", (0, 0, 0), [(A_BUTT, 0.0), (-6.0, 1.8), (0.0, 4.6), (4.0, 4.9), (8.0, 4.2)], sides=8)
    for sgn in (-1, 1):                                                       # langets: iron straps from the head down the haft
        ml.bar(fit, (0, 112.0, sgn * 4.2), (0, yc - A_HUB_HY, sgn * 4.7), 4.8, 1.0, up=(0, 0, sgn), taper=6.0, end_scale=0.4)
    # ---- the hub, the eye of the axe: dark iron, gold rims at both ends; the bit and the poll grow out of its sides
    hub = K.add(m, "head_hub", "hero_dark_steel", base=A_IRON, smooth=30, wear=0.5)
    ml.loft(hub, "y", (0, 0, 0), [(yc - A_HUB_HY, A_HUB_HX * 0.80, A_HUB_HZ * 0.80), (yc - A_HUB_HY + 4, A_HUB_HX, A_HUB_HZ), (yc + A_HUB_HY - 4, A_HUB_HX, A_HUB_HZ), (yc + A_HUB_HY, A_HUB_HX * 0.80, A_HUB_HZ * 0.80)],
             ml.octagon(2.0, 2.0, 0.40))
    # ---- the bearded crescent bit (a lofted flat-ground lozenge: the height follows the outline, the thickness tapers to the edge)
    top, bot, thick = bit_profile(A_BIT)
    d0, d_tip, d_top, d_bot = A_BIT["d0"], A_BIT["d_tip"], A_BIT["d_top"], A_BIT["d_bot"]
    ds = sorted(set([d0 + (d_top - d0) * i / 8.0 for i in range(9)] + [d0 + (d_bot - d0) * i / 8.0 for i in range(9)] +
                    [d_top + (d_tip - d_top) * (1 - math.cos(math.pi / 2 * j / 6.0)) for j in range(1, 7)] + [d_bot + (d_tip - d_bot) * (1 - math.cos(math.pi / 2 * j / 6.0)) for j in range(1, 7)]))
    ds = [d for i, d in enumerate(ds) if i == 0 or d - ds[i - 1] > 2.0]
    ds[-1] = d_tip
    bit = K.add(m, "head_blade", "hero_blade_steel", base=A_STEEL, smooth=40, wear=0.6, along="u")
    stations = []
    for d in ds:
        ht, hb, t = float(top(d)), float(bot(d)), float(thick(d))
        stations.append((-d, (ht + hb) / 2.0, t, (ht - hb) / 2.0, 0.0))
    stations[-1] = (-d_tip, 0.0, 0.0, 0.0, 0.0)
    ml.loft(bit, "x", (0, yc, 0), stations, ml.bevelled(2.0, 2.0, 0.50))
    # ---- the poll: a thick squared block behind the hub in darker steel, with its face plate and the short back spike
    xp0, xp1, xp2 = A_POLL_X
    poll = K.add(m, "poll_block", "hero_steel", base=A_POLL, smooth=30, wear=0.6)
    sec_poll = ml.octagon(2.0, 2.0, 0.40)
    ml.loft(poll, "x", (0, yc, 0), [(xp0, 13.0, 11.5), (xp0 + 5.0, 15.5, 13.5), (xp1 - 3.0, 15.5, 13.5), (xp1, 14.0, 12.2)], sec_poll)
    ml.loft(poll, "x", (0, yc, 0), [(xp1 - 0.5, 14.0, 12.2), (xp1 + 1.6, 17.0, 14.8), (xp2 - 1.0, 17.0, 14.8), (xp2, 14.5, 12.4)], sec_poll)
    spike = K.add(m, "back_spike", "hero_steel", base=A_POLL, smooth=30, wear=0.5)
    ml.loft(spike, "x", (0, yc, 0), [(xp2 - 0.5, 6.4, 5.6), (xp2 + A_SPIKE * 0.5, 4.2, 3.6), (xp2 + A_SPIKE, 0.0, 0.0)], ml.octagon(2.0, 2.0, 0.5))
    # ---- gold trim: rims round the hub and a collar at the foot of the haft's top (the gold of the doc is trim only)
    trim = K.add(m, "hub_trim", "hero_brass", base=A_GOLD, smooth=30, wear=0.5)
    for yy in (yc - A_HUB_HY - 1.0, yc + A_HUB_HY - 3.0):
        ml.loft(trim, "y", (0, 0, 0), [(yy, A_HUB_HX * 1.00, A_HUB_HZ * 1.00), (yy + 0.8, A_HUB_HX * 1.05, A_HUB_HZ * 1.05), (yy + 2.6, A_HUB_HX * 1.05, A_HUB_HZ * 1.05), (yy + 3.4, A_HUB_HX * 1.00, A_HUB_HZ * 1.00)],
                 ml.octagon(2.0, 2.0, 0.40))
    # ---- the emerald: both cheeks of the hub, in the seven-sided gold bezel
    bez = K.add(m, "gem_bezels", "hero_brass", base=A_GOLD, smooth=30, wear=0.4)
    gem = K.add(m, "gems", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    for sgn in (-1, 1):
        ax = "z" if sgn > 0 else "-z"
        K.star_bezel(bez, (0.0, yc, sgn * (A_HUB_HZ - 0.4)), A_BEZEL_R, 2.6, axis=ax)
        K.cabochon(gem, (0.0, yc, sgn * (A_HUB_HZ + 1.6)), A_GEM_R, 4.2, axis=ax, sides=14)
    # ---- the top of the haft: a short iron cap with a small spear point (a woodcutter's axe has none; a Seven Star one carries a finial)
    fin = K.add(m, "haft_cap", "hero_dark_steel", base=A_IRON, smooth=35, wear=0.5)
    ml.loft(fin, "y", (0, 0, 0), [(yc + A_HUB_HY - 1.0, 3.6, 3.2), (yc + A_HUB_HY + 6.0, 4.6, 3.6), (yc + A_HUB_HY + 14.0, 6.4, 4.0), (yc + A_HUB_HY + 24.0, 0.0, 0.0)], ml.lozenge(2.0, 2.0, 0.0))
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, A_EMERALD)
    K.etched_bit(atlas, bit, top, bot, yc, d0, d_tip, steel=A_STEEL, bevel=A_BEVEL, inlay=A_INLAY, inlay_glow=A_EMERALD)
    # the seven-star mark: the Big Dipper in tiny green dots with the faintest lines on the broad face of the blade near the eye, and seven dots round the back spike on the poll
    for sgn in (1, -1):
        K.dipper_engraving(atlas, bit, "z", sgn, (-30.0 * sgn, yc - 3.0), 1.4, rotate_deg=-10.0, glow_rgb=A_EMERALD, width=0.5, dot_r=1.35, strong=0.9)
    pts = [(a_, b_ + yc) for (a_, b_) in K.ring_points(7, 11.0, 90.0)]
    K.engrave(atlas, poll, "x", 1, segments=[], dots=pts, width=0.5, dot_r=1.25, dark=0.5, glow_rgb=A_EMERALD, glow_strength=0.9)
    return m, atlas


# ================================================================ 3. the Vassal Staff
# Target: the look of docs/heroweapon/weapon_staff.md (hvb): a tall, slender mage's staff with all the visual weight in the head; the head an OPEN ring held on the shaft by four small
# curved prongs, a faceted gem FLOATING in the middle (a double pyramid, not touching the ring) and a small upturned crescent on top of the ring; a lacquered dark indigo shaft,
# silver-white metal, pale gold trim, a dark violet leather grip, the gem violet-blue (glow #7B8CFF); seven small raised five-point stars down the front of the shaft, the Seven Star
# mark, on the body skin and soft on the glow skin. Canon: vassal to the Legendary Spear with the Projectile; accelerated casting, magic power-up points; forms Fenrir Rod,
# Siderite Staff ... (wiki summaries; no picture described): design. One change of mine: the ring is a little larger than the doc's 0.14 of the length (0.17), because the owner asks
# for bold heads. (The first-person camera looks along mesh +x, so the ring, whose axis is z as the doc asks, is seen obliquely from there: from the lower right of the screen.) The class staff is 2.0 - 2.6 blocks (the doc's 1.85 is from the older table): 2.2.
STAFF_APPARENT = 2.20           # blocks in the hand: class staff is 2.0 - 2.6
STAFF_GRIP = 0.40
S_INDIGO, S_SILVER, S_SILVER_HI = K.hx("262A5A"), K.hx("D3DAEA"), K.hx("F4F7FF")  # the doc's palette: lacquered shaft, the metal, its bright edges
S_GOLD, S_LEATHER, S_STAR = K.hx("E7D79A"), K.hx("3A2C3F"), K.hx("A9B5FF")        # pale gold trim, grip leather, the seven stars
S_VIOLET = K.hx("7B8CFF")       # the gem, glow 7B8CFF
S_BUTT, S_SHAFT_TOP, S_HEAD_Y = -9.0, 186.0, 236.0     # the foot's tip (lowest point), the top of the shaft where the collar begins, the common centre of the ring and the gem
S_RING_R = 21.0                 # radius of both rings: a ring head 46 wide (outer) on a 275 long staff, 0.17 of the length
S_RING_A_TUBE = 2.5             # tube radius of the big ring (xy plane, the profile view)
S_GEM_R, S_GEM_LONG = 11.0, 1.5 # the floating gem: seven-sided double pyramid, girdle radius and length over radius of each half (0.05 of the length, the doc)
S_GRIP_Y = (80.0, 128.0)        # the grip leather (the hand is at 40 percent of the 280 long weapon: y = 103)
S_STARS = (22.0, 76.0)          # the seven stars run down the shaft below the grip


def build_staff():
    item = "vassal_staff"
    m = ml.Mesh(item)
    yc = S_HEAD_Y
    R = S_RING_R
    # ---- shaft: lacquered dark indigo, a long gently swelling lathe
    shaft = K.add(m, "shaft", "hero_ebony", base=S_INDIGO, smooth=60, uv_mode="loft", wear=0.4)
    ml.lathe(shaft, "y", (0, 0, 0), [(0.0, 3.6), (30.0, 3.8), (80.0, 3.9), (130.0, 4.0), (170.0, 3.8), (S_SHAFT_TOP + 4.0, 4.2)], sides=12)
    grip = K.add(m, "grip_wrap", "hero_dark_leather", base=S_LEATHER, smooth=60, uv_mode="loft", wear=0.7)
    y0, y1 = S_GRIP_Y
    ml.loft(grip, "y", (0, 0, 0), [(y0, 0.93, 0.93), (y0 + 4, 1.0, 1.0), ((y0 + y1) / 2, 1.05, 1.05), (y1 - 4, 1.0, 1.0), (y1, 0.93, 0.93)], ml.circle(4.5, 12))
    # ---- silver bands on the shaft (round ones at the ends of the grip, two higher up), pale gold bands next to them
    bands = K.add(m, "shaft_bands", "hero_white_steel", base=S_SILVER, smooth=45, wear=0.4)
    for yy in (y0 - 3.0, y1 + 0.5, 150.0, 172.0):
        ml.collar(bands, yy, 5.2, 3.0, lip=0.8, sides=10)
    trim = K.add(m, "gold_trim", "hero_old_gold", base=S_GOLD, smooth=45, wear=0.4)
    for yy in (y0 - 6.0, y1 + 3.6, 146.5, 168.5):
        ml.collar(trim, yy, 4.9, 1.4, lip=0.35, sides=10)
    # ---- the foot: a pointed silver ferrule on the butt
    fer = K.add(m, "foot", "hero_white_steel", base=S_SILVER, smooth=40, wear=0.5)
    ml.lathe(fer, "y", (0, 0, 0), [(S_BUTT, 0.0), (-4.0, 1.6), (1.0, 4.0), (6.0, 5.0), (10.0, 4.5), (13.0, 4.0)], sides=8)
    # ---- the collar under the head: a silver sleeve that flares into a cup, and four small curved prongs that hold the ring
    vase = K.add(m, "collar", "hero_white_steel", base=S_SILVER, smooth=45, wear=0.4)
    ml.lathe(vase, "y", (0, 0, 0), [(S_SHAFT_TOP - 2.0, 4.2), (S_SHAFT_TOP + 5.0, 6.6), (S_SHAFT_TOP + 11.0, 8.6), (yc - R - 12.0, 5.6), (yc - R - 5.0, 4.2), (yc - R - 1.5, 3.2)], sides=12)
    prongs = K.add(m, "prongs", "hero_white_steel", base=S_SILVER, smooth=45, uv_mode="loft", wear=0.4)
    for sx, ang, dz in ((-1, 36.0, 2.4), (-1, 62.0, -2.4), (1, 36.0, -2.4), (1, 62.0, 2.4)):    # four prongs, two each side, a little apart in depth, from the collar up to the ring
        end = V((sx * R * math.cos(math.radians(ang)), yc - R * math.sin(math.radians(ang)), dz))
        p0 = V((sx * 5.2, S_SHAFT_TOP + 13.0, dz * 0.6))
        ml.sweep(prongs, K.bezier(p0, p0 + V((sx * (7.0 if ang < 50 else 4.0), 9.0, 0.0)), end + V((-sx * (2.0 if ang < 50 else 5.0), -(15.0 if ang < 50 else 9.0), 0.0)), end, 10), ml.circle(1.0, 6),
                 scales=lambda t: (1.6 - 0.45 * t, 1.6 - 0.45 * t))
    # ---- the open ring (the profile circle), and a pale gold thread of metal along its inside
    ring_a = K.add(m, "head_ring", "hero_white_steel", base=S_SILVER, smooth=50, uv_mode="loft", wear=0.4)
    K.torus(ring_a, (0.0, yc, 0.0), R, S_RING_A_TUBE, plane="xy", n=30, sides=8)
    inner = K.add(m, "head_ring_inner", "hero_old_gold", base=S_GOLD, smooth=50, uv_mode="loft", wear=0.4)
    K.torus(inner, (0.0, yc, 0.0), R - S_RING_A_TUBE - 0.4, 0.9, plane="xy", n=30, sides=5)
    # ---- the small upturned crescent on top of the ring (pale gold): an arc of 220 degrees open to the sky, tapering to two points
    cres = K.add(m, "crescent", "hero_old_gold", base=S_GOLD, smooth=40, uv_mode="loft", wear=0.4)
    rc, cy0 = 8.5, yc + R + 2.6 + 6.0
    cpts = []
    for i in range(0, 15):
        a_ = math.radians(-20.0 - 220.0 * i / 14.0)                           # from the right tip down through the bottom to the left tip, the opening up
        cpts.append((rc * math.cos(a_), cy0 + rc * math.sin(a_) * 1.0, 0.0))
    ml.sweep(cres, cpts, ml.bevelled(2.0, 2.0, 0.4), scales=lambda t: ((0.2 + 2.6 * math.sin(math.pi * min(1.0, max(0.0, t))) ** 0.8), (0.2 + 1.5 * math.sin(math.pi * min(1.0, max(0.0, t))) ** 0.8)), caps=(True, True), up=(0, 0, 1))
    # ---- the floating gem: a seven-sided double pyramid, violet-blue, held by nothing
    gem = K.add(m, "gem", "hero_gem", smooth=0, uv_mode="loft", wear=0.0)
    ml.gem(gem, (0.0, yc, 0.0), S_GEM_R, long=S_GEM_LONG, sides=7, axis="y")
    # ---- skin
    atlas = K.lay(m)
    K.paint_cut_gem(atlas, gem, S_VIOLET)
    K.glow_all(atlas, inner, S_VIOLET, 0.35, core=0.2)
    # the seven stars down the front of the shaft (both faces: the +z side and the -z side), lighter than the lacquer, soft on the glow skin
    ts = [(y_ / (S_SHAFT_TOP + 4.0)) for y_ in np.linspace(S_STARS[0], S_STARS[1], 7)]
    for col in (0.25, 0.75):
        K.stars_on_loft(atlas, shaft, ts, [col] * 7, 2.5, S_STAR, light=S_STAR, glow_rgb=S_STAR, glow_strength=0.55)
    return m, atlas


# ================================================================ 4. the Vassal Claw
# Target: the look of docs/heroweapon/weapon_claw.md (hva): a hand-claw, a gauntlet glove whose knuckle bar carries three long curved blades that fan slightly outward and sweep
# forward and up (the blades are the weapon, the glove the handle; a fist full of claws from the front, a strip of three curved blades from the side), the lightest and smallest of
# the four striking weapons; black-brown leather, steel plates, bright blades with a cold edge highlight, worn gold, ONE amber-orange cabochon (glow #FFA030) in a small gold bezel
# on the back plate over the knuckles, the seven-star mark (seven tiny dots) on the back plate. Canon: vassal to the Legendary Shield with the Hammer; forms Small Claw, Rope Claw,
# Dragon Head Claws, Cursed Claw (wiki summaries; no picture described), so the shape is design. Mesh y is the forearm and the blades, mesh x the width of the hand (the talons are
# side by side across it, so the profile view shows the whole fan); the plate and the gem face +-z, the faces onlookers and the first-person camera see; the blades curve toward -x
# like a katana's. The doc aims at 0.75 blocks from strap to blade tips: a cuff that small could not close round a player's arm (0.25 wide), so the claw is 1.0 long, the blades
# about half of it.
CLAW_APPARENT = 1.00            # blocks in the hand: shortsword class is 1.0 - 1.3, the nearest size class of the table (a hand weapon with blades as long as a dagger's)
CLAW_GRIP = 0.20
C_LEATHER, C_STITCH, C_STEEL, C_BEVEL = K.hx("3A2A22"), K.hx("6B4A33"), K.hx("8E96A8"), K.hx("B9C2D6")        # the doc's palette
C_BLADE, C_ROOT, C_EDGE, C_GOLD = K.hx("C9D2E3"), K.hx("5A6070"), K.hx("8FB4FF"), K.hx("B8893A")
C_AMBER = K.hx("FFA030")
C_CUFF_Y, C_HAND_Y, C_BAR_Y = (0.0, 40.0), (34.0, 74.0), 76.0   # the bracer, the hand plate (they overlap), the knuckle bar the talons grow from
C_CUFF_AB = (17.5, 12.5)        # half width (x: the width of the hand) and half depth (z) of the bracer: 35 wide, 0.28 blocks at the scale below, wider than a player's 0.25 wide arm
C_HAND_AB = (18.0, 12.8)        # the plate over the hand is a little wider than the bracer
C_GEM_R, C_BEZEL_R = 5.6, 8.0   # the cabochon (about 0.1 across in the hand) and the small seven-sided bezel on the back of the hand
# the talons as cubic Beziers in the xy plane: base, two controls, tip (all x, y), base half width and half thickness; lengths 92, 86 and 66 and a thumb spur of 44, a little less
# fanned than the first sketch (the doc says "slightly outward"); the two small hooks behind the blades are the spurs of the doc
C_TALONS = {
    "mid": ((0.0, C_BAR_Y), (0.0, C_BAR_Y + 34.0), (-4.0, C_BAR_Y + 64.0), (-15.0, C_BAR_Y + 92.0), 5.4, 3.0),
    "left": ((-13.0, C_BAR_Y), (-16.0, C_BAR_Y + 28.0), (-26.0, C_BAR_Y + 54.0), (-40.0, C_BAR_Y + 78.0), 4.9, 2.7),
    "right": ((13.0, C_BAR_Y), (14.0, C_BAR_Y + 24.0), (14.0, C_BAR_Y + 44.0), (7.0, C_BAR_Y + 62.0), 4.6, 2.6),
    "spur_thumb": ((-16.0, 42.0), (-27.0, 50.0), (-36.0, 62.0), (-41.0, 80.0), 4.4, 2.5),
    "spur_left": ((-21.0, C_BAR_Y - 2.0), (-28.0, C_BAR_Y - 4.0), (-33.0, C_BAR_Y - 12.0), (-33.0, C_BAR_Y - 22.0), 3.2, 2.0),
    "spur_right": ((21.0, C_BAR_Y - 2.0), (28.0, C_BAR_Y - 4.0), (33.0, C_BAR_Y - 12.0), (33.0, C_BAR_Y - 22.0), 3.2, 2.0),
}


def build_claw():
    item = "vassal_claw"
    m = ml.Mesh(item)
    ca, cb = C_CUFF_AB
    ha, hb = C_HAND_AB
    ell = ml.ellipse(1.0, 1.0, 14)
    # ---- the bracer: black-brown leather, flared at the mouth, narrowing at the wrist; its mouth is open (the arm goes in) with a lining so no hole shows
    cuff = K.add(m, "cuff", "hero_dark_leather", base=C_LEATHER, smooth=55, wear=0.8, uv_mode="loft")
    ml.loft(cuff, "y", (0, 0, 0), [(C_CUFF_Y[0], ca * 1.00, cb * 1.00), (6.0, ca * 0.92, cb * 0.92), (22.0, ca * 0.82, cb * 0.82), (34.0, ca * 0.80, cb * 0.80), (C_CUFF_Y[1], ca * 0.90, cb * 0.88)], ell, caps=(False, False))
    lin = K.add(m, "cuff_lining", "hero_dark_leather", base=K.hx("241A15"), smooth=55, wear=0.4, uv_mode="loft")
    ml.loft(lin, "y", (0, 0, 0), [(0.5, ca * 0.93, cb * 0.93), (6.0, ca * 0.86, cb * 0.86), (22.0, ca * 0.76, cb * 0.76), (34.0, ca * 0.74, cb * 0.74)], ell, caps=(False, False))
    lin._flip_all()                                                            # the lining faces inward: seen through the open mouth
    # ---- steel lames round the bracer and the wrist strap (a band of the same leather, lighter stitching) with a gold buckle on each side
    lames = K.add(m, "cuff_lames", "hero_steel", base=C_STEEL, smooth=40, wear=0.7)
    for y0, y1, k0 in ((2.0, 9.0, 1.06), (13.0, 19.0, 0.97), (23.0, 29.0, 0.88)):
        ml.loft(lames, "y", (0, 0, 0), [(y0, ca * k0 * 0.97, cb * k0 * 0.97), (y0 + 1.0, ca * k0 * 1.04, cb * k0 * 1.04), (y1 - 1.0, ca * k0 * 1.04, cb * k0 * 1.04), (y1, ca * k0 * 0.97, cb * k0 * 0.97)], ell)
    gold = K.add(m, "buckles", "hero_brass", base=C_GOLD, smooth=30, wear=0.4)
    for yb, kk in ((10.5, 0.97), (26.5, 0.88)):
        for sgn in (-1, 1):
            z0 = sgn * cb * kk
            ml.box(gold, (-4.2, yb - 2.2, z0 + (0.0 if sgn > 0 else -2.2)), (4.2, yb + 2.2, z0 + (2.2 if sgn > 0 else 0.0)), chamfer=0.5)
    # ---- the plate over the back of the hand (and its twin, the palm guard): a boxy steel shell with three finger ridges, a gold collar where it meets the bracer
    hp = K.add(m, "back_plate", "hero_steel", base=C_STEEL, smooth=40, wear=0.7, along="u")
    ml.loft(hp, "y", (0, 0, 0), [(C_HAND_Y[0], ha * 0.80, hb * 0.82), (C_HAND_Y[0] + 5.0, ha * 0.94, hb * 0.96), (46.0, ha, hb), (64.0, ha, hb), (C_HAND_Y[1] - 3.0, ha * 0.94, hb * 0.92), (C_HAND_Y[1], ha * 0.82, hb * 0.78)], ml.rounded_rect(2.0, 2.0, 0.62, 3))
    for sgn in (-1, 1):
        for xr in (-10.0, 0.0, 10.0):
            ml.bar(hp, (xr, 63.0, sgn * (hb - 0.4)), (xr, 73.5, sgn * (hb - 1.0)), 5.2, 1.5, up=(0, 0, sgn), taper=2.4, end_scale=0.5)
    gc = K.add(m, "hand_collar", "hero_brass", base=C_GOLD, smooth=40, wear=0.5)
    ml.loft(gc, "y", (0, 0, 0), [(C_HAND_Y[0] - 3.0, ha * 0.80, hb * 0.82), (C_HAND_Y[0] - 1.8, ha * 0.90, hb * 0.92), (C_HAND_Y[0] + 3.0, ha * 0.97, hb * 1.0), (C_HAND_Y[0] + 4.2, ha * 0.90, hb * 0.92)], ml.ellipse(1.0, 1.0, 14))
    # ---- the knuckle bar: a rounded bar across the hand with a gold socket under each blade
    kb = K.add(m, "knuckle_bar", "hero_brass", base=C_GOLD, smooth=40, wear=0.5)
    ml.loft(kb, "x", (0, C_BAR_Y - 1.0, 0), [(-ha - 4.0, 2.2, 5.6), (-ha * 0.7, 3.8, 7.2), (0.0, 4.2, 7.8), (ha * 0.7, 3.8, 7.2), (ha + 4.0, 2.2, 5.6)], ml.ellipse(1.0, 1.0, 10))
    for nm in ("mid", "left", "right"):
        p0, p1 = C_TALONS[nm][0], C_TALONS[nm][1]
        d = V((p1[0] - p0[0], p1[1] - p0[1], 0.0))
        ml.lathe(kb, tuple(d), (p0[0], p0[1] - 5.0, 0.0), [(0.0, 5.2), (5.0, 6.6), (9.0, 6.2), (12.0, 4.6)], sides=8)
    # ---- the blades: swept lozenges that taper to needle points (a dark root, bright steel beyond), the thumb spur and the two small hooks behind the blades
    tal = K.add(m, "blades", "hero_blade_steel", base=C_BLADE, smooth=40, wear=0.5, uv_mode="loft")
    for nm, (p0, p1, p2, p3, w0, t0) in C_TALONS.items():
        pts = K.bezier((p0[0], p0[1], 0.0), (p1[0], p1[1], 0.0), (p2[0], p2[1], 0.0), (p3[0], p3[1], 0.0), 18 if not nm.startswith("spur_l") and nm != "spur_right" else 10)
        ml.sweep(tal, pts, ml.bevelled(2.0, 2.0, 0.55), scales=lambda t, w0=w0, t0=t0: ((w0 * (1.0 - t) ** 0.85 + 0.12) if t < 0.995 else 0.0, (t0 * (1.0 - t) ** 0.7 + 0.1) if t < 0.995 else 0.0), up=(0, 0, 1))
    # ---- the gem on both sides of the back plate (the doc's one cabochon, visible from either side), in its small seven-sided gold bezel
    bez = K.add(m, "gem_bezels", "hero_brass", base=C_GOLD, smooth=30, wear=0.4)
    gem = K.add(m, "gems", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    for sgn in (-1, 1):
        ax = "z" if sgn > 0 else "-z"
        K.star_bezel(bez, (0.0, 54.0, sgn * (hb - 0.8)), C_BEZEL_R, 2.4, axis=ax)
        K.cabochon(gem, (0.0, 54.0, sgn * (hb + 1.0)), C_GEM_R, 3.8, axis=ax, sides=14)
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, C_AMBER)
    # the seven-star mark on the back plate: the Big Dipper out from the gem in thin lines and seven tiny amber dots, on both sides
    for sgn in (1, -1):
        K.dipper_engraving(atlas, hp, "z", sgn, (0.0, 52.0), 1.35, rotate_deg=-8.0, glow_rgb=C_AMBER, width=0.6, dot_r=1.1, strong=0.85)

    def blades(c):                                                       # a dark root, bright steel beyond; a cold hairline of glow along the two knife edges
        T = np.clip(c.T, 0, 1)
        root = np.array(C_ROOT, np.float32)
        k_ = np.clip(1.0 - T / 0.16, 0, 1)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - k_) + root * k_ * (0.85 + 0.3 * c.img[..., :3] / 255.0)
        S = c.S
        e = np.exp(-(((S - 0.0) / 0.05) ** 2)) + np.exp(-(((S - 1.0) / 0.05) ** 2)) + np.exp(-(((S - 0.5) / 0.05) ** 2))
        gl = np.zeros(c.img.shape, np.float32)
        gl[..., :3] = np.array(C_EDGE, np.float32)
        gl[..., 3] = np.clip(e * np.clip((T - 0.12) * 1.6, 0, 1), 0, 1) * 85
        c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, tal, blades)
    return m, atlas


# ================================================================ 5. the Vassal Gauntlet
# Target: the look of docs/heroweapon/weapon_gauntlet.md (hvb): ONE right-hand armoured gauntlet, a closed fist: a flared vambrace cuff, a heavy plated back of the hand, four finger
# plates curled into the palm (each in two bands), the thumb plate folded over them, a row of four knuckle studs; brushed steel with darker recesses, a leather strap and lining,
# tiger-orange trim along the cuff edge and the bezel, a band of white fur inside the cuff opening (the Hakuko holder), a large amber-orange oval cabochon (glow #FFA43A, bright
# core #FFD27A) in a gold bezel in the middle of the back of the hand, thin orange engraving lines across the back plate (soft glow). Canon: vassal to the Legendary Sword with the
# Axe; skill power-up points; chose Fohl Fayon, a white tiger demi-human, after his sister Atla's death (wiki summaries; no picture described): design. The fist is built the way a
# fist is punched: the forearm along mesh y, the back of the hand facing +x (so the first-person camera, which looks along +x, sees it with its gem), the fingers folded toward -x
# under the front, the thumb across them on the +z side; the outer side (-z), the one onlookers see, is a flat plate with a second, smaller gem. The doc aims at 0.55 blocks from
# cuff to knuckles; a cuff that small could not swallow a player's arm (0.25 wide), so the cuff is 0.3 wide and the whole gauntlet 0.9 long: that is how "the hand display looks right".
GAUNT_APPARENT = 0.90           # blocks in the hand: the size class `handitem` is 0.5 - 0.9 by its largest dimension
GAUNT_GRIP = 0.25
G_STEEL, G_RECESS, G_LEATHER = K.hx("9AA3B2"), K.hx("4A5160"), K.hx("5B3A2A")       # the doc's palette: brushed steel, darker recesses, leather
G_ORANGE, G_FUR, G_AMBER, G_CORE = K.hx("E58A2B"), K.hx("F2EEE6"), K.hx("FFA43A"), K.hx("FFD27A")
G_GOLD = K.hx("C9A24A")
G_CUFF_AB = (11.5, 14.0)        # half height (x) and half width (z) of the vambrace at its mouth: 28 wide, 0.3 blocks at the scale below
G_HAND_Y = (36.0, 70.0)         # the hand shell
G_HAND_AB = (10.5, 13.0)        # its half height (x, the back of the hand is the +x face) and half width (z)
G_FINGER_Z = (-9.9, -3.3, 3.3, 9.9)   # the four fingers side by side across the hand, 6.6 apart, each 4.3 in radius (they touch, as in a fist)
G_FINGER_R = 4.3
G_GEM_R, G_BEZEL_R = 6.0, 8.8   # the amber cabochon (oval) and its seven-sided setting on the back of the hand (and, a little smaller, on the side plate)


def finger_path(z, cx=1.2, cy=68.0, a=5.4, b=8.2, n_arc=11):
    """The centre line of one folded finger in the xy plane at depth z: along the top of the hand, round the front knuckles on an ellipse and back under to the palm."""
    pts = [(G_HAND_AB[0] - 1.8, 58.0, z), (G_HAND_AB[0] - 1.4, 63.0, z)]
    for i in range(n_arc + 1):
        al = math.radians(2.0 + 228.0 * i / n_arc)                  # clockwise from the top, over the front (y max at 90 degrees), under, and back
        pts.append((cx + a * math.cos(al) + 1.8, cy + b * math.sin(al), z))
    return pts


def build_gauntlet():
    item = "vassal_gauntlet"
    m = ml.Mesh(item)
    ca, cz = G_CUFF_AB
    ha, hz = G_HAND_AB
    ell = ml.ellipse(1.0, 1.0, 14)
    # ---- the vambrace: brushed steel flared at the mouth, narrowing at the wrist; a leather lining seen through the open mouth; two steel lames; a rolled tiger-orange edge
    cuff = K.add(m, "vambrace", "hero_steel", base=G_STEEL, smooth=50, wear=0.7, along="v")
    ml.loft(cuff, "y", (0, 0, 0), [(0.0, ca * 1.00, cz * 1.00), (5.0, ca * 0.93, cz * 0.94), (20.0, ca * 0.80, cz * 0.82), (32.0, ca * 0.78, cz * 0.80), (G_HAND_Y[0] + 2.0, ca * 0.86, cz * 0.88)], ell, caps=(False, False))
    lin = K.add(m, "vambrace_lining", "hero_dark_leather", base=G_LEATHER, smooth=55, wear=0.4, uv_mode="loft")
    ml.loft(lin, "y", (0, 0, 0), [(0.5, ca * 0.93, cz * 0.93), (5.0, ca * 0.87, cz * 0.88), (20.0, ca * 0.74, cz * 0.76), (30.0, ca * 0.72, cz * 0.74)], ell, caps=(False, False))
    lin._flip_all()
    lames = K.add(m, "vambrace_lames", "hero_steel", base=K.hx("B4BCC9"), smooth=40, wear=0.6)
    for y0, y1, k0 in ((9.0, 13.0, 0.92), (21.0, 25.0, 0.82)):
        ml.loft(lames, "y", (0, 0, 0), [(y0, ca * k0 * 0.98, cz * k0 * 0.98), (y0 + 0.9, ca * k0 * 1.05, cz * k0 * 1.05), (y1 - 0.9, ca * k0 * 1.05, cz * k0 * 1.05), (y1, ca * k0 * 0.98, cz * k0 * 0.98)], ell)
    trim = K.add(m, "cuff_trim", "hero_brass", base=G_ORANGE, smooth=40, wear=0.5)
    ml.loft(trim, "y", (0, 0, 0), [(-0.8, ca * 0.98, cz * 0.98), (0.2, ca * 1.06, cz * 1.06), (3.2, ca * 1.06, cz * 1.06), (4.2, ca * 0.98, cz * 0.98)], ell)
    fur = K.add(m, "cuff_fur", "hero_cord", base=G_FUR, smooth=70, uv_mode="loft", wear=0.2)
    fpts = [(ca * 0.93 * math.cos(2 * math.pi * i / 20.0), -0.6, cz * 0.93 * math.sin(2 * math.pi * i / 20.0)) for i in range(21)]
    ml.sweep(fur, fpts, ml.circle(2.6, 6), caps=(False, False), up=(0, 1, 0))
    # ---- the wrist strap: a leather band round the wrist with a gold buckle on each flat
    st = K.add(m, "wrist_strap", "hero_dark_leather", base=G_LEATHER, smooth=45, wear=0.7)
    ml.loft(st, "y", (0, 0, 0), [(27.0, ca * 0.84, cz * 0.86), (27.8, ca * 0.90, cz * 0.92), (32.2, ca * 0.90, cz * 0.92), (33.0, ca * 0.84, cz * 0.86)], ell)
    buckle = K.add(m, "buckles", "hero_brass", base=G_GOLD, smooth=30, wear=0.4)
    for sgn in (-1, 1):
        z0 = sgn * cz * 0.90
        ml.box(buckle, (-3.6, 28.2, z0 + (0.0 if sgn > 0 else -1.8)), (3.6, 31.8, z0 + (1.8 if sgn > 0 else 0.0)), chamfer=0.5)
    # ---- the hand: a boxy steel shell (rounded-rectangle sections, slightly domed on the back), the back of the hand is its +x face; a nose fills the curl of the fingers
    hand = K.add(m, "back_plate", "hero_steel", base=G_STEEL, smooth=40, wear=0.7, along="u")
    ml.loft(hand, "y", (0, 0, 0), [(G_HAND_Y[0], ha * 0.84, hz * 0.86), (G_HAND_Y[0] + 5.0, ha * 0.96, hz * 0.97), (50.0, ha, hz), (62.0, ha, hz), (G_HAND_Y[1] - 2.0, ha * 0.95, hz * 0.95), (G_HAND_Y[1], ha * 0.90, hz * 0.92),
                                   (G_HAND_Y[1] + 5.0, ha * 0.74, hz * 0.90), (G_HAND_Y[1] + 10.0, ha * 0.50, hz * 0.84)], ml.rounded_rect(2.0, 2.0, 0.55, 3))
    # ---- the fingers: four folded plates of steel (two bands each are painted as seams), the thumb plate across them on the +z side, a stud on each knuckle
    fin = K.add(m, "fingers", "hero_steel", base=G_STEEL, smooth=55, uv_mode="loft", wear=0.7)
    for z in G_FINGER_Z:
        ml.sweep(fin, finger_path(z), ml.octagon(2.0, 2.0, 0.55), scales=lambda t: (G_FINGER_R * (1.0 - 0.10 * t), G_FINGER_R * (1.0 - 0.10 * t)), caps=(True, True), up=(0, 0, 1))
    thumb = [(3.0, 42.0, 14.6), (5.0, 54.0, 16.6), (4.4, 66.0, 15.8), (-2.0, 74.0, 11.4)]
    ml.sweep(fin, K.bezier(thumb[0], thumb[1], thumb[2], thumb[3], 12), ml.octagon(2.0, 2.0, 0.55), scales=lambda t: (5.0 - 1.0 * t, 5.0 - 1.0 * t), caps=(True, True), up=(1, 0, 0))
    studs = K.add(m, "knuckle_studs", "hero_brass", base=G_ORANGE, smooth=30, wear=0.4)
    for z in G_FINGER_Z:
        K.spike(studs, (1.2 + 5.4 * math.cos(math.radians(50)) + 1.8, 68.0 + 8.2 * math.sin(math.radians(50)), z), (0.45, 0.89, 0.0), 7.0, 2.9, 0.0, sides=4)
    # ---- the gem: the back of the hand (+x, an oval along the hand) and a smaller one on the side plate (-z), each in a seven-sided gold bezel
    bez = K.add(m, "gem_bezels", "hero_brass", base=G_GOLD, smooth=30, wear=0.4)
    gem = K.add(m, "gems", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    K.star_bezel(bez, (ha - 0.6, 52.0, 0.0), G_BEZEL_R, 2.6, axis="x")
    K.cabochon(gem, (ha + 1.0, 52.0, 0.0), G_GEM_R, 4.0, axis="x", sides=14, squash=1.0)
    K.star_bezel(bez, (0.0, 52.0, -(hz - 0.6)), G_BEZEL_R * 0.8, 2.4, axis="-z")
    K.cabochon(gem, (0.0, 52.0, -(hz + 0.9)), G_GEM_R * 0.8, 3.6, axis="-z", sides=14)
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, G_AMBER)
    # darker recesses: a groove panel round the gem on the back of the hand and on the side plate (enamel_rect in the dark recess colour, with an orange hairline), the two bands
    # of every finger are dark seams across it. The bevel gives the panel a shadowed upper-left lip and a lit lower-right lip so that it reads as a sunk plate and not as a hole
    # (the recess colour of the doc, #4A5160, is close to a grey-blue sky or a preview background)
    K.enamel_rect(atlas, hand, "x", 1, (-10.0, G_HAND_Y[0] + 2.5, 10.0, G_HAND_Y[1] - 2.0), enamel=G_RECESS, border=G_ORANGE, border_w=0.9, glow_border=G_AMBER, bevel=2.4)
    K.enamel_rect(atlas, hand, "z", -1, (-8.2, G_HAND_Y[0] + 2.5, 8.2, G_HAND_Y[1] - 2.0), enamel=G_RECESS, border=G_ORANGE, border_w=0.9, glow_border=G_AMBER, bevel=2.4)

    def seams(c):
        T = c.T
        s1 = np.exp(-(((T - 0.30) / 0.018) ** 2)) + np.exp(-(((T - 0.62) / 0.018) ** 2))
        c.img[..., :3] = c.img[..., :3] * (1 - 0.7 * s1[..., None]) + np.array(G_RECESS, np.float32) * 0.7 * s1[..., None]
    w.paint_part(atlas, fin, seams)
    # the thin orange engraving across the back plate (the Big Dipper out from under the gem, soft; at this scale the tail star lies about 4 texels above the panel's lower line,
    # the head stars sit under the finger plates) and a heptagram through seven dots on the side plate
    K.dipper_engraving(atlas, hand, "x", 1, (0.0, 54.0), 0.9, rotate_deg=-90.0, glow_rgb=G_AMBER, width=0.5, dot_r=1.0, strong=0.8)
    pts = [(a_, b_ + 52.0) for (a_, b_) in K.ring_points(7, 9.5, 90.0)]
    K.engrave(atlas, hand, "z", -1, segments=[(pts[i], pts[(i + 2) % 7]) for i in range(7)], dots=pts, width=0.5, dot_r=1.0, dark=0.5, glow_rgb=G_AMBER, glow_strength=0.7)
    K.glow_all(atlas, studs, G_AMBER, 0.28, core=0.3)

    def fluff(c):                                                  # the fur: white with warm grey shadow and soft noise, no hard texture
        n = w.noise2(c.img.shape[:2], 2.2, 55)
        c.img[..., :3] = np.array(G_FUR, np.float32) * (0.82 + 0.28 * n[..., None])
    w.paint_part(atlas, fur, fluff)
    return m, atlas


# ================================================================ 6. the Vassal Whip
# Target: the look of docs/heroweapon/weapon_whip.md (hva): a braided whip held coiled: a stout handle, two or three loops of the lash gathered at the hand, the free lash trailing
# ahead in one smooth S-curve (one bend and a half, never a zigzag) that tapers to a weighted gem tip in a small cage with four tiny spikes (the morning-star hint); braided dark
# leather with a lighter braid line, gold rings and a guard disc, steel spikes, TWO violet cabochons (the pommel gem, the series' central jewel, and the faceted tip gem; glow
# #9B5CFF), the seven-star mark (seven tiny dots) on the guard disc. Canon: vassal to the Legendary Bow with the Carriage; it controls monsters, once drove those that pulled the
# vassal chariot; forms Chain, Mace, Rope Whip, Morning Star (Gigantic Star), Dragon Hourglass Sand Whip (Portal Whip) (wiki summaries; no look described): design. The whole cord
# is ONE sweep (about 70 points), so the weapon costs few quads. Mesh y runs along the grip and the lash; the coil and the lash curve in the xy plane (the coil turned out of it,
# so the first-person camera sees it as a ring too); mesh -x is the side the lash leans to (forward).
WHIP_APPARENT = 1.62            # blocks in the hand: class coilwhip is 1.1 - 1.7 and the doc asks for about 1.5; 1.62 also satisfies the extended `whip` class (1.6 - 2.4), so the weapon passes the audit even before the class fragment is merged
WHIP_GRIP = 0.15
W_LEATHER, W_BRAID, W_GOLD, W_STEEL = K.hx("4A3426"), K.hx("6B4A33"), K.hx("C9A24A"), K.hx("9AA3B2")     # the doc's palette
W_VIOLET = K.hx("9B5CFF")
W_BUTT = -8.0                   # the pommel's back
W_HANDLE_Y = (0.0, 46.0)        # the braided handle: 28 percent of the weapon, the hand at 15 percent of the length (y = 15)
W_COIL_C, W_COIL_R = (14.0, 64.0), (15.5, 10.5)   # the centre of the loops beside the handle, their radius going from the first loop to the second
W_COIL_TILT = 32.0              # the loops are turned this many degrees about y out of the profile plane, so the first-person camera (looking along +x) sees them as a ring too
W_CORD_R = (3.1, 1.15)          # the cord's radius in the coil and at the very tip
W_GEM_R, W_BEZEL_R = 5.2, 7.6   # the pommel's cabochon (about 0.1 across) and its seven-sided bezel


def whip_path():
    """The centre line of the whole cord: out of the handle, two loops clockwise round W_COIL_C in a plane tilted out of the profile (a little more depth on each so they
    never touch), then an S-shaped lash (one bend and a half)."""
    cx, cy = W_COIL_C
    al = math.radians(W_COIL_TILT)
    h = V((math.cos(al), 0.0, math.sin(al)))                              # the loops' horizontal axis, turned from +x toward +z
    nrm = V((-math.sin(al), 0.0, math.cos(al)))                           # the loops' normal: the drift of the second turn runs along it
    c0 = V((cx, cy, 0.0))
    pts = [(0.0, W_HANDLE_Y[1] - 1.0, 0.0), (0.0, W_HANDLE_Y[1] + 7.0, 0.0), (0.0, W_HANDLE_Y[1] + 13.0, 0.0)]
    n = 22
    for i in range(1, 2 * n + 1):                                         # two turns, clockwise from the left-most point of the circle
        th = math.radians(180.0 - 360.0 * i / n)
        r = W_COIL_R[0] + (W_COIL_R[1] - W_COIL_R[0]) * i / (2 * n)
        p = c0 + h * (r * math.cos(th)) + V((0.0, r * math.sin(th), 0.0)) + nrm * (0.4 + 6.8 * i / (2 * n))
        pts.append(tuple(p))
    x0, y0, z0 = pts[-1]
    seg1 = K.bezier((x0, y0, z0), (x0 - 1.0, y0 + 24.0, z0 - 0.4), (-26.0, 104.0, 3.0), (-15.0, 124.0, 1.2), 12)
    seg2 = K.bezier((-15.0, 124.0, 1.2), (-7.0, 136.0, 0.4), (8.0, 142.0, -1.0), (4.0, 156.0, -2.2), 8)
    pts += seg1[1:] + seg2[1:]
    return pts


def build_whip():
    item = "vassal_whip"
    m = ml.Mesh(item)
    # ---- the handle: braided dark leather, a little swollen in the middle, gold rings at both ends and a gold wire spiral
    hd = K.add(m, "grip_braid", "hero_dark_leather", base=W_LEATHER, smooth=60, uv_mode="loft", wear=0.7)
    y0, y1 = W_HANDLE_Y
    ml.loft(hd, "y", (0, 0, 0), [(y0 - 2.0, 0.84, 0.84), (y0 + 3.0, 1.0, 1.0), ((y0 + y1) / 2, 1.1, 1.1), (y1 - 5.0, 1.0, 1.0), (y1, 0.86, 0.86)], ml.circle(4.3, 10))
    gold = K.add(m, "handle_rings", "hero_brass", base=W_GOLD, smooth=45, wear=0.5)
    for yy in (y0 - 3.5, y1 - 7.0):
        ml.collar(gold, yy, 5.1, 3.4, lip=0.9, sides=10)
    wire = [(4.95 * math.cos(2 * math.pi * 5.5 * t), y0 + 4.0 + (y1 - y0 - 18.0) * t, 4.95 * math.sin(2 * math.pi * 5.5 * t)) for t in np.linspace(0, 1, 36)]
    ml.sweep(gold, wire, ml.circle(0.42, 4), scales=lambda t: (1.0, 1.0) if 0.03 < t < 0.97 else (0.3, 0.3), caps=(False, False))
    # ---- the guard disc where the handle meets the lash (a gold disc facing +-z, the seven-star mark engraved on it)
    gd = K.add(m, "guard_disc", "hero_brass", base=W_GOLD, smooth=40, wear=0.5)
    ml.lathe(gd, "z", (0.0, y1 + 1.0, 0.0), [(-2.2, 7.6), (-1.0, 8.4), (1.0, 8.4), (2.2, 7.6)], sides=18)
    # ---- the pommel: a gold disc facing +-z, a violet gem on each face in a seven-sided bezel, a short taper to the handle
    pom = K.add(m, "pommel", "hero_brass", base=W_GOLD, smooth=40, wear=0.5)
    ml.lathe(pom, "z", (0.0, W_BUTT + 6.0, 0.0), [(-4.6, 6.2), (-3.4, 7.4), (3.4, 7.4), (4.6, 6.2)], sides=18)
    ml.loft(pom, "y", (0.0, 0.0, 0.0), [(W_BUTT + 0.5, 0.0, 0.0), (W_BUTT + 3.0, 3.0, 3.0), (y0 - 1.0, 4.0, 4.0)], ml.circle(1.0, 8))
    bez = K.add(m, "gem_bezels", "hero_brass", base=W_GOLD, smooth=30, wear=0.4)
    gem = K.add(m, "gems", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    for sgn in (-1, 1):
        ax = "z" if sgn > 0 else "-z"
        K.star_bezel(bez, (0.0, W_BUTT + 6.0, sgn * 3.6), W_BEZEL_R, 2.0, axis=ax)
        K.cabochon(gem, (0.0, W_BUTT + 6.0, sgn * 4.8), W_GEM_R, 3.4, axis=ax, sides=12)
    # ---- the cord: handle top, the coil and the lash as one braided sweep
    cord = K.add(m, "coil_and_lash", "hero_dark_leather", base=W_LEATHER, smooth=60, uv_mode="loft", wear=0.6)
    pts = whip_path()
    ml.sweep(cord, pts, ml.circle(1.0, 6), scales=lambda t: ((W_CORD_R[0] if t < 0.52 else W_CORD_R[0] + (W_CORD_R[1] - W_CORD_R[0]) * ((t - 0.52) / 0.48) ** 0.9),) * 2, caps=(True, False), up=(0, 0, 1))
    # ---- the weighted tip: a gold cage of four curved bars round a faceted violet ball, a cap on the cord's end and four tiny steel spikes on the top (the morning-star hint)
    tip = V(pts[-1], float)
    d = K.unit(V(pts[-1]) - V(pts[-4]))
    ref = V((0.0, 0.0, 1.0))
    e1 = K.unit(np.cross(d, ref))
    e2 = np.cross(d, e1)
    cage = K.add(m, "tip_cage", "hero_brass", base=W_GOLD, smooth=45, uv_mode="loft", wear=0.4)
    ml.lathe(cage, tuple(d), tuple(tip - d * 4.0), [(0.0, 1.0), (2.0, 2.8), (5.0, 3.6)], sides=7)
    c0_ = tip + d * 10.0
    for k_ in range(4):
        a_ = math.pi / 4 + k_ * math.pi / 2
        off = e1 * math.cos(a_) + e2 * math.sin(a_)
        bar_pts = [tuple(tip + d * 1.5 + off * 3.2), tuple(c0_ + off * 6.4 - d * 3.0), tuple(c0_ + off * 6.6 + d * 2.0), tuple(c0_ + d * 9.6 + off * 2.2)]
        ml.sweep(cage, K.bezier(*bar_pts, 6), ml.circle(0.75, 4), caps=(True, True), up=(0, 0, 1))
    spk = K.add(m, "tip_spikes", "hero_steel", base=W_STEEL, smooth=30, wear=0.4)
    for k_ in range(4):
        a_ = k_ * math.pi / 2
        off = e1 * math.cos(a_) + e2 * math.sin(a_)
        K.spike(spk, tuple(c0_ + d * 9.0 + off * 1.8), tuple(d * 0.8 + off * 0.6), 5.0, 1.2, 0.0, sides=4)
    tgem = K.add(m, "tip_gem", "hero_gem", smooth=0, uv_mode="loft", wear=0.0)
    ml.gem(tgem, tuple(c0_), 4.6, long=1.35, sides=8, axis=tuple(d))
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, W_VIOLET)
    K.paint_cut_gem(atlas, tgem, W_VIOLET, facets=8)
    K.braid(atlas, cord, dark=0.55, period_a=2.4, period_b=3.4, light=0.35)
    K.braid(atlas, hd, dark=0.6, period_a=2.0, period_b=2.8, light=0.30)
    # a hairline of violet thread inlaid along the lash, brighter toward the tip (soft glow), and the seven-star mark on the guard disc
    def thread(c):
        S = c.S
        line = np.exp(-(((S - 0.5) / 0.05) ** 2))
        gl = np.zeros(c.img.shape, np.float32)
        gl[..., :3] = np.array(W_VIOLET, np.float32)
        gl[..., 3] = line * np.clip((c.T - 0.50) * 2.0, 0, 1) ** 1.2 * 70
        c.glow[...] = np.maximum(c.glow, gl)
        c.img[..., :3] = c.img[..., :3] * (1 - 0.35 * line[..., None]) + np.array(W_VIOLET, np.float32) * 0.35 * line[..., None] * np.clip((c.T - 0.50) * 2.0, 0, 1)[..., None]
    w.paint_part(atlas, cord, thread)
    for sgn in (1, -1):
        K.dipper_engraving(atlas, gd, "z", sgn, (0.0, y1 + 1.0), 0.55, rotate_deg=-10.0, glow_rgb=W_VIOLET, width=0.35, dot_r=0.7, strong=0.9)
    return m, atlas


# ================================================================ 7. the Vassal Projectile
# Target: the look of docs/heroweapon/weapon_projectile.md (hvb): ONE throwing knife of crystal (the wiki's base form is a semi-transparent knife that becomes chakrams, kunai,
# hatchets, boomerangs and short spears): a leaf-shaped double-edged blade with a centre ridge, a short oval guard, a cord-wrapped grip and a RING pommel (an open ring a finger
# could go through) that holds the gem; the blade about 0.60 of the length, the grip 0.25, the ring 0.15; the look "semi-transparent" is painted (the cutout render type cannot
# blend): aqua in the middle, near white at the edges, darker veins along the ridge like a crack in ice, a very faint sheen along the ridge on the glow skin; dark teal guard and ring,
# a two-tone teal cord, ONE round cabochon in the ring, emerald-teal (glow #38F0C0, core #B8FFEA), and a short cord loop from the ring. Canon: vassal to the Legendary Spear with the
# Staff; Rishia Ivyred's; its first form was a wavering semi-transparent shadow of the true one; Air Strike Throw (wiki summaries). The gem is a boss on both faces of the ring so it
# shows from either side. The doc says apparent length 0.5 (the dagger class of the older table); the table now says 0.45 - 0.70 and a Seven Star weapon sits in its upper half: 0.62.
PROJ_APPARENT = 0.62            # blocks in the hand: class dagger is 0.45 - 0.70
PROJ_GRIP = 0.20
P_AQUA, P_WHITE, P_VEIN = K.hx("A8EDE6"), K.hx("E6FFFB"), K.hx("6BCFC4")           # the doc's palette: the blade, its edges, the veins
P_TEAL, P_TEAL_HI, P_CORD_A, P_CORD_B = K.hx("24545A"), K.hx("4FA3A8"), K.hx("1C3F44"), K.hx("3C7C80")
P_EMERALD, P_CORE = K.hx("38F0C0"), K.hx("B8FFEA")
P_RING_R, P_RING_T = 7.6, 1.6   # the ring pommel: centreline radius and tube radius (the ring is 0.15 of the length across, 16.4 on a 110 long knife)
P_GRIP_Y = (15.0, 41.0)         # the cord-wrapped grip, 0.25 of the length
P_GUARD_Y = 42.5                # the oval guard
P_BLADE_Y = (44.0, 109.0)       # the blade, 0.6 of the length, tip at 109
P_BLADE_W = [(0.0, 4.4), (0.08, 6.2), (0.30, 7.4), (0.55, 6.4), (0.78, 3.8), (0.94, 1.3), (1.0, 0.0)]   # half widths along the blade (fraction of its length): a leaf, widest at a third
P_GEM_R = 4.6                   # the gem boss on the ring (0.05 of the length, a little more as a boss)


def build_projectile():
    item = "vassal_projectile"
    m = ml.Mesh(item)
    yb0, yb1 = P_BLADE_Y
    wf = w.hermite(P_BLADE_W)
    # ---- the blade: a lofted leaf with a diamond section (the centre ridge), thickness following the width, a needle point
    blade = K.add(m, "blade", "hero_gem", base=P_AQUA, smooth=40, wear=0.2, along="v")
    st = []
    for i in range(0, 17):
        u = i / 16.0
        hw = max(float(wf(u)), 0.0)
        st.append((yb0 + (yb1 - yb0) * u, hw if u < 1.0 else 0.0, (0.34 * hw + 0.25) if u < 1.0 else 0.0))
    ml.loft(blade, "y", (0, 0, 0), st, ml.lozenge(2.0, 2.0, 0.0))
    # ---- the edge glint: a thin lighter strip lying along both cutting edges (a raised crystal bead, half sunk into the edge)
    glint = K.add(m, "edge_glint", "hero_gem", base=P_WHITE, smooth=50, uv_mode="loft", wear=0.0)
    for sgn in (-1, 1):
        pts = [(sgn * (max(float(wf(u)), 0.0) - 0.15), yb0 + (yb1 - yb0) * u, 0.0) for u in [0.10 + 0.82 * i / 14.0 for i in range(15)]]
        ml.sweep(glint, pts, ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (0.5 * (1.0 - 0.7 * t) + 0.12, 0.55 * (1.0 - 0.6 * t) + 0.12), caps=(True, True), up=(0, 0, 1))
    # ---- the guard: a short dark oval with brighter edges
    gd = K.add(m, "guard", "hero_dark_steel", base=P_TEAL, smooth=40, wear=0.5)
    w.ellipsoid(gd, (0.0, P_GUARD_Y, 0.0), 8.2, 2.1, 3.6, sides=14, stacks=6)
    # ---- the grip: a cord wrap in two tones, a little swollen, with a bead at each end
    grip = K.add(m, "grip", "hero_cord", base=P_CORD_A, smooth=60, uv_mode="loft", wear=0.4)
    y0, y1 = P_GRIP_Y
    ml.loft(grip, "y", (0, 0, 0), [(y0, 0.86, 0.86), (y0 + 3.0, 1.0, 1.0), ((y0 + y1) / 2, 1.1, 1.1), (y1 - 3.0, 1.0, 1.0), (y1, 0.88, 0.88)], ml.circle(3.1, 10))
    # ---- the ring pommel and the gem boss at its lowest point (both faces), the short cord loop hanging from it
    ring = K.add(m, "pommel_ring", "hero_dark_steel", base=P_TEAL, smooth=50, uv_mode="loft", wear=0.5)
    cy = P_RING_R - 0.2
    K.torus(ring, (0.0, cy, 0.0), P_RING_R, P_RING_T, plane="xy", n=22, sides=6)
    ml.loft(ring, "y", (0, 0, 0), [(y0 - 4.0, 2.4, 2.4), (y0 + 1.0, 3.0, 3.0)], ml.circle(1.0, 8))             # the neck that joins the ring to the grip
    bez = K.add(m, "gem_bezels", "hero_dark_steel", base=P_TEAL_HI, smooth=30, wear=0.4)
    gem = K.add(m, "gem", "hero_gem", smooth=70, uv_mode="loft", wear=0.0)
    for sgn in (-1, 1):
        ax = "z" if sgn > 0 else "-z"
        K.star_bezel(bez, (0.0, 0.8, sgn * 0.6), P_GEM_R + 2.4, 1.8, axis=ax)
        K.cabochon(gem, (0.0, 0.8, sgn * 1.7), P_GEM_R, 3.2, axis=ax, sides=12)
    loop = K.add(m, "cord_loop", "hero_cord", base=P_CORD_B, smooth=60, uv_mode="loft", wear=0.3)
    cp = [(-5.4, 3.0, 0.0), (-8.2, -2.0, 0.6), (-8.6, -9.0, 1.0), (-6.4, -15.0, 0.6), (-4.4, -17.0, 0.0)]
    ml.sweep(loop, cp, ml.circle(0.95, 5), scales=lambda t: (1.0, 1.0) if t < 0.86 else (1.0 + 1.6 * (t - 0.86) / 0.14, 1.0 + 1.6 * (t - 0.86) / 0.14), caps=(True, True), up=(0, 0, 1))
    # ---- skin
    atlas = K.lay(m)
    K.paint_gem(atlas, gem, P_EMERALD)

    def crystal(c):                                                 # aqua in the middle, white at the edges, veins along the ridge like a crack in ice, a faint sheen on the glow skin
        S, T = c.S, c.T
        edge = np.maximum(np.exp(-(((S - 0.0) / 0.10) ** 2)), np.maximum(np.exp(-(((S - 1.0) / 0.10) ** 2)), np.exp(-(((S - 0.5) / 0.10) ** 2))))
        ridge = np.maximum(np.exp(-(((S - 0.25) / 0.07) ** 2)), np.exp(-(((S - 0.75) / 0.07) ** 2)))
        n1 = w.noise2(c.img.shape[:2], 5, 91 + int(c.g.rect[0]) % 7)
        n2 = w.noise2(c.img.shape[:2], 2.4, 17)
        crack = (np.abs(n1 - 0.5) < 0.016).astype(np.float32) * (0.35 + 0.65 * ridge)
        base = np.array(P_AQUA, np.float32)[None, None, :] * (0.88 + 0.2 * n2[..., None])
        col = base + (np.array(P_WHITE, np.float32) - base) * (0.35 + 0.5 * edge)[..., None]
        col = col * (1 - 0.30 * ridge[..., None]) + np.array(P_VEIN, np.float32) * (0.30 * ridge[..., None])
        col = col * (1 - 0.55 * crack[..., None]) + np.array(P_VEIN, np.float32) * (0.55 * crack[..., None])
        c.img[..., :3] = col
        gl = np.zeros(c.img.shape, np.float32)
        gl[..., :3] = np.array(P_EMERALD, np.float32)
        gl[..., 3] = np.clip(ridge * 0.9 + edge * 0.15, 0, 1) * np.clip(T * 1.4, 0, 1) * 46
        c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, blade, crystal)

    def glint_paint(c):                                             # near white, a touch of glow so the edges look lit from inside
        c.img[..., :3] = np.array(P_WHITE, np.float32) * (0.9 + 0.1 * w.noise2(c.img.shape[:2], 2.0, 8)[..., None])
        gl = np.zeros(c.img.shape, np.float32)
        gl[..., :3] = np.array(P_CORE, np.float32)
        gl[..., 3] = 60
        c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, glint, glint_paint)

    def cord(c):                                                    # the two-tone diagonal wrap
        stripe = (((c.A / 1.9) + (c.B / 2.6)) % 1.0) < 0.5
        c.img[..., :3] = np.where(stripe[..., None], np.array(P_CORD_A, np.float32), np.array(P_CORD_B, np.float32)) * (0.85 + 0.3 * w.noise2(c.img.shape[:2], 1.6, 4)[..., None])
    w.paint_part(atlas, grip, cord)
    w.paint_part(atlas, loop, cord)
    return m, atlas


# ---------------------------------------------------------------- the sprites (16 x 16, hand-built pixel code; the inventory icons)
# Why: the owner keeps the old 16 x 16 sprite as the inventory icon of every 3D weapon. Each is drawn from primitives on the diagonal (butt low left, tip up right) in the weapon's
# own palette (the constants of its builder), one dark outline pixel is added round it, and the gem is the brightest, most saturated spot.
def _gem_px(cv, x, y, rgb):
    """A 2 x 2 gem: highlight, body, two shades."""
    cv.put(x, y, K.mix(rgb, (255, 255, 255), 0.55))
    cv.put(x + 1, y, rgb)
    cv.put(x, y + 1, shade(rgb, 0.8))
    cv.put(x + 1, y + 1, shade(rgb, 0.6))


def sprite_hammer():
    """The hammer on the diagonal: walnut haft with a black-brown grip and brass rings, a chunky iron head across it (the long front block toward the upper left), a brass hub
    with the amber gem, a brass pommel cap."""
    cv = K.Sprite()
    walnut, walnut_hi, walnut_lo = H_WALNUT, shade(H_WALNUT, 1.5), shade(H_WALNUT, 0.6)
    leather, leather_hi = H_LEATHER, K.hx("5A4332")
    brass, brass_hi, brass_lo = H_BRASS, K.hx("E8C57A"), shade(H_BRASS, 0.62)
    iron, iron_hi, iron_lo = K.hx("5B6173"), K.hx("A4ACBF"), K.hx("353A47")
    for i in range(0, 9):                                              # the haft, from the pommel to the head
        x, y = 2 + i, 13 - i
        cv.put(x, y, walnut if i % 2 else walnut_hi)
        cv.put(x + 1, y, walnut_lo)
    for i in (2, 3, 4):                                                # the grip
        x, y = 2 + i, 13 - i
        cv.put(x, y, leather if i != 3 else leather_hi)
        cv.put(x + 1, y, shade(leather, 0.6))
    cv.put(1, 14, brass)                                               # the pommel cap
    cv.put(2, 14, brass_lo)
    cv.put(1, 13, brass_hi)
    cv.put(5, 8, brass)                                                # a ring under the head
    cv.put(4, 9, brass_lo)

    def head(k, dx):
        if k == 0:                                                     # the striking face, bright
            return iron_hi if dx else iron
        if k in (2, 3):                                                # the brass hub
            return (brass_lo, brass, brass, brass_hi)[dx]
        if k == 5:
            return None if dx == 3 else iron_lo
        return (iron_lo, iron, iron, iron_hi)[dx]
    cv.band(7, 2, 6, 4, head)
    _gem_px(cv, 10, 4, H_AMBER)
    return cv, (22, 18, 20)


def sprite_axe():
    """The axe on the diagonal: a bearded crescent bit of steel with a green etched line to the upper left of the iron hub (the emerald on it), a poll with a back spike to the
    lower right, a pale ash haft with a black-brown grip."""
    cv = K.Sprite()
    ash, ash_hi, ash_lo = A_ASH, shade(A_ASH, 1.3), shade(A_ASH, 0.62)
    leather, leather_hi = A_LEATHER, K.hx("5A4332")
    steel, steel_hi, steel_lo = A_STEEL, A_BEVEL, shade(A_STEEL, 0.62)
    iron, iron_hi = A_IRON, K.hx("5A6070")
    gold = A_GOLD
    for i in range(0, 12):                                             # the haft from the butt cap to the hub
        x, y = 2 + i, 13 - i
        cv.put(x, y, ash if i % 2 else ash_hi)
        cv.put(x + 1, y, ash_lo)
    for i in (2, 3, 4):                                                # the grip
        x, y = 2 + i, 13 - i
        cv.put(x, y, leather if i % 2 else leather_hi)
        cv.put(x + 1, y, shade(leather, 0.6))
    cv.put(1, 14, iron_hi)                                             # the butt cap
    cv.put(2, 14, iron)
    cx, cy = 10.2, 5.9                                                 # the hub on the haft, at about 70 % of the length

    def head(u, v):
        d = -u                                                         # distance forward of the haft (the bit leads on -x)
        if 0.8 <= d <= 5.8:                                            # the bearded bit: flares from the haft, the beard (below the centre line, v < 0) reaches farther
            if v >= 0:
                h = 3.3 * min(1.0, (d - 0.5) / 2.2) * (1.0 if d < 3.2 else max(0.0, 1.0 - (d - 3.2) / 2.8))
            else:
                h = 5.0 * min(1.0, (d - 0.5) / 2.8) * (1.0 if d < 4.0 else max(0.0, 1.0 - (d - 4.0) / 2.0))
            if abs(v) <= h:
                if abs(v) >= h - 0.9 or d >= 5.0:
                    return steel_hi
                if abs(abs(v) - 0.6 * h) < 0.55:                       # the green etched line
                    return A_INLAY
                return steel if v > 0.8 else steel_lo
            return None
        if 0.0 <= u <= 3.0 and abs(v) <= 1.9:                          # the poll behind the hub
            return K.hx("7C8496") if u < 2.2 else shade(A_POLL, 0.8)
        return None
    cv.shape(cx, cy, head, reach=9)
    for (x, y) in ((10, 5), (11, 5), (10, 6), (11, 6)):                # the hub block with the gem
        cv.put(x, y, iron)
    _gem_px(cv, 10, 5, A_EMERALD)
    cv.put(12, 4, gold)                                                # the cap above the hub, a small point
    cv.put(12, 3, iron_hi)
    cv.put(13, 2, iron_hi)
    cv.put(13, 7, steel_hi)                                            # the back spike
    cv.put(14, 8, steel)
    return cv, (18, 20, 28)


def sprite_staff():
    """The staff on the diagonal: an indigo shaft with a violet leather grip and silver bands, a silver ring head with a gold crescent on it and the violet-blue gem floating in it."""
    cv = K.Sprite()
    indigo, indigo_hi, indigo_lo = K.hx("343A7A"), K.hx("5560A8"), K.hx("1A1D45")
    grip, grip_hi = S_LEATHER, K.hx("6A5072")
    silver, silver_hi, silver_lo = S_SILVER, S_SILVER_HI, K.hx("8D96B2")
    gold, gold_hi = S_GOLD, K.hx("FFF2C0")
    for i in range(0, 10):                                             # the shaft from the foot up to the ring
        x, y = 2 + i, 13 - i
        cv.put(x, y, indigo if i % 2 else indigo_hi)
        cv.put(x + 1, y, indigo_lo)
    for i in (3, 4, 5):                                                # the grip
        x, y = 2 + i, 13 - i
        cv.put(x, y, grip if i % 2 else grip_hi)
        cv.put(x + 1, y, shade(grip, 0.6))
    cv.put(1, 14, silver)                                              # the foot
    cv.put(2, 14, silver_lo)
    cv.put(8, 7, silver_hi)                                            # the collar under the head
    cv.put(9, 6, silver)
    cx, cy = 11.9, 4.3
    cv.ring(cx, cy, 2.45, 3.4, silver)                                 # the ring head
    for (x, y) in ((10, 2), (11, 1), (14, 3), (14, 5), (10, 6)):
        cv.put(x, y, silver_hi)
    cv.put(13, 7, silver_lo)
    _gem_px(cv, 11, 3, S_VIOLET)                                       # the floating gem
    cv.put(12, 5, shade(S_VIOLET, 0.8))
    for (x, y) in ((11, 0), (12, 0), (13, 0)):                         # the crescent on top
        cv.put(x, y, gold)
    cv.put(10, -0, gold_hi)
    cv.put(14, 0, gold_hi)
    return cv, (14, 16, 40)


def sprite_claw():
    """The claw on the diagonal: a dark leather bracer at the lower left, a steel hand shell with the amber gem, a brass knuckle bar and three bright blades fanned up and to the
    right from it (the middle one the longest) with a spur curling up on the left."""
    cv = K.Sprite()
    leather, leather_hi, leather_lo = C_LEATHER, K.hx("6B4A33"), K.hx("241A15")
    steel, steel_hi, steel_lo = C_STEEL, C_BEVEL, K.hx("5C6478")
    gold, gold_hi, gold_lo = C_GOLD, K.hx("E8C57A"), shade(C_GOLD, 0.62)
    silver, silver_hi, silver_lo = C_BLADE, (250, 252, 255), K.hx("7E889E")
    cv.block(1, 11, 3, 14, leather, leather_hi, leather_lo)            # the bracer
    cv.put(2, 12, steel_hi)                                            # its lame and buckle
    cv.put(1, 13, gold)
    cv.block(3, 8, 7, 12, steel, steel_hi, steel_lo)                   # the hand shell
    cv.put(3, 12, steel_lo)
    cv.put(7, 8, steel_lo)
    for (x, y) in ((6, 7), (7, 8), (8, 9)):                            # the knuckle bar, across the weapon
        cv.put(x, y, gold if (x + y) % 2 else gold_hi)
    cv.put(9, 10, gold_lo)
    cv.put(5, 6, gold_lo)
    cv.line(6, 6, 7, 0, silver, silver_hi)                             # the left blade, leaning up and curling to the left
    cv.put(8, 0, silver_hi)
    cv.line(7, 7, 13, 1, silver_lo, silver_hi)                         # the middle blade, the longest, up the diagonal
    cv.put(14, 0, silver_hi)
    cv.line(9, 9, 15, 6, silver_lo, silver)                            # the right blade, shorter, leaning to the right
    cv.put(15, 5, silver_hi)
    cv.line(3, 9, 2, 5, silver_lo, silver)                             # the spur curling up on the left
    cv.put(2, 4, silver_hi)
    cv.put(8, 7, C_EDGE)                                               # a hint of the cold edge at the blade roots
    _gem_px(cv, 5, 9, C_AMBER)                                         # the amber gem on the back of the hand
    return cv, (22, 18, 20)


def sprite_gauntlet():
    """The gauntlet seen from the back of the hand, turned onto the diagonal: a steel vambrace with an orange edge and a leather strap at the lower left, the back of the hand (a dark
    recess with the amber gem in it), four fingers folded over the front with dark seams between them and an orange stud each, and the thumb at the side."""
    cv = K.Sprite()
    steel, steel_hi, steel_lo = G_STEEL, K.hx("DDE3EC"), K.hx("626B7C")
    recess = K.hx("3C4352")
    orange, orange_hi = G_ORANGE, K.hx("FFC070")
    leather = G_LEATHER

    def cuff(u, v):
        if abs(u) <= 3.1 - 0.10 * v and -8.8 <= v <= -2.4:
            if v < -7.7:
                return orange if u > -1.0 else orange_hi
            if -5.6 < v < -4.6:
                return leather
            return steel_hi if u < -1.2 else (steel if u < 1.2 else steel_lo)
        return None
    cv.shape(8.0, 8.0, cuff, reach=11)

    def hand(u, v):
        if abs(u) <= 3.6 and -2.9 <= v <= 2.5:                         # the back of the hand: a steel frame round a dark recess
            if abs(u) <= 2.4 and -1.8 <= v <= 1.5:
                return recess
            return steel_hi if u < -2.4 else (steel_lo if u > 2.6 else steel)
        if abs(u) <= 3.6 and 2.5 < v <= 5.2:                          # the four folded fingers over the front: light plates with dark seams between
            col = int(math.floor((u + 3.6) / 1.8))
            if abs(((u + 3.6) % 1.8) - 0.0) < 0.28 and col > 0:
                return steel_lo
            return steel_lo if v > 4.5 else (steel_hi if col % 2 == 0 else steel)
        if -5.2 <= u < -3.6 and -0.8 <= v <= 3.4:                     # the thumb on the outer side
            return steel_hi if v > 1.6 else steel
        return None
    cv.shape(9.2, 6.6, hand, reach=11)
    _gem_px(cv, 9, 6, G_AMBER)                                         # the amber gem on the back of the hand
    for (x, y) in ((11, 1), (12, 2), (13, 3), (14, 4)):                # the studs on the knuckles
        cv.put(x, y, orange_hi)
    return cv, (22, 24, 30)


def sprite_whip():
    """The whip on the diagonal: a dark braided handle with gold wire and the violet pommel gem at the lower left, two loops of cord beside it, the lash running up to the right in an
    S and ending in a gold cage with a violet gem."""
    cv = K.Sprite()
    dark, dark_hi = W_LEATHER, W_BRAID
    cord, cord_hi, cord_lo = K.hx("5C402F"), K.hx("8A6244"), K.hx("33231A")
    gold, gold_hi, gold_lo = W_GOLD, K.hx("F0D488"), shade(W_GOLD, 0.62)
    violet = W_VIOLET
    for i in range(0, 5):                                              # the handle
        x, y = 2 + i, 13 - i
        cv.put(x, y, dark if i % 2 else dark_hi)
        cv.put(x + 1, y, shade(dark, 0.6))
    cv.put(3, 12, gold)
    cv.put(5, 10, gold_hi)
    cv.put(1, 14, gold)                                                # the pommel with its gem
    cv.put(2, 14, gold_lo)
    cv.put(1, 13, K.mix(violet, (255, 255, 255), 0.55))
    cv.put(2, 13, violet)
    cv.ring(8.0, 7.4, 1.5, 3.0, cord)                                  # the coil: two loops seen edge on make a thick ring
    for (x, y) in ((6, 5), (7, 5), (9, 10), (10, 9)):
        cv.put(x, y, cord_hi)
    for (x, y) in ((10, 6), (11, 7), (6, 9), (7, 10)):
        cv.put(x, y, cord_lo)
    cv.put(5, 8, cord)
    cv.put(5, 9, cord_lo)                                              # the cord from the handle into the coil
    for (x, y) in ((9, 4), (10, 3), (10, 2), (11, 1)):                 # the S of the lash up to the tip
        cv.put(x, y, cord_hi if (x + y) % 2 else cord)
    cv.put(12, 1, gold)                                                # the cage
    cv.put(13, 0, gold_hi)
    cv.put(13, 1, gold_lo)
    cv.put(14, 0, gold)
    cv.put(13, 2, gold_lo)
    cv.put(14, 1, K.mix(violet, (255, 255, 255), 0.5))                 # the gem in it
    cv.put(14, 2, violet)
    return cv, (30, 20, 14)


def sprite_projectile():
    """The projectile on the diagonal: a pale aqua crystal knife with a dark teal guard, a two-tone cord grip and a teal ring pommel with the emerald-teal gem, a short cord loop."""
    cv = K.Sprite()
    aqua, white, vein = P_AQUA, P_WHITE, P_VEIN
    teal, teal_hi = P_TEAL, P_TEAL_HI
    cord_a, cord_b = P_CORD_A, P_CORD_B
    for i in range(0, 8):                                              # the blade, a leaf, two pixels wide in the middle
        x, y = 6 + i, 9 - i
        cv.put(x, y, aqua if i % 2 else white)
        if 1 <= i <= 5:
            cv.put(x + 1, y, shade(aqua, 0.78))
            cv.put(x, y - 1, white)
    cv.put(14, 1, white)
    cv.put(8, 7, vein)
    cv.put(10, 5, vein)
    cv.put(5, 9, teal_hi)                                              # the guard across the blade's foot
    cv.put(6, 10, teal)
    cv.put(4, 8, teal)
    cv.put(7, 10, teal_hi)
    for i in range(0, 3):                                              # the cord grip
        x, y = 4 - i, 10 + i
        cv.put(x, y, cord_b if i % 2 else cord_a)
        cv.put(x + 1, y, cord_a)
    cv.ring(2.4, 13.0, 1.2, 2.3, teal)                                 # the ring pommel
    cv.put(1, 14, K.mix(P_EMERALD, (255, 255, 255), 0.55))             # the gem in the ring
    cv.put(2, 14, P_EMERALD)
    cv.put(1, 15, shade(P_EMERALD, 0.7))
    cv.put(0, 13, cord_b)                                              # the cord loop
    cv.put(0, 14, cord_a)
    return cv, (14, 36, 40)


# id -> (mesh builder, sprite drawer, butt pixel, tip pixel, apparent length in blocks, grip share, height of the middle of the grip in mesh units). The two pixels are the ends of the mesh's axis on the 16 x 16 sprite
# (x right, y down): a 45 degree diagonal, so the mesh and the icon agree and the display transforms of every other weapon fit.
SPECS = {
    "vassal_hammer": (build_hammer, sprite_hammer, (1.5, 14.5), (13.5, 2.5), HAMMER_APPARENT, HAMMER_GRIP, sum(H_GRIP_Y) / 2),
    "vassal_axe": (build_axe, sprite_axe, (1.5, 14.5), (14.5, 1.5), AXE_APPARENT, AXE_GRIP, sum(A_GRIP_Y) / 2),
    "vassal_staff": (build_staff, sprite_staff, (1.5, 14.5), (14.5, 1.5), STAFF_APPARENT, STAFF_GRIP, sum(S_GRIP_Y) / 2),
    "vassal_claw": (build_claw, sprite_claw, (1.5, 14.5), (14.5, 1.5), CLAW_APPARENT, CLAW_GRIP, 20.0),
    "vassal_gauntlet": (build_gauntlet, sprite_gauntlet, (1.5, 14.5), (14.5, 1.5), GAUNT_APPARENT, GAUNT_GRIP, 20.0),
    "vassal_whip": (build_whip, sprite_whip, (1.5, 14.5), (14.5, 1.5), WHIP_APPARENT, WHIP_GRIP, sum(W_HANDLE_Y) / 2),
    "vassal_projectile": (build_projectile, sprite_projectile, (1.5, 14.5), (14.5, 1.5), PROJ_APPARENT, PROJ_GRIP, sum(P_GRIP_Y) / 2),
}


def make(item, preview=False, views=False):
    """Builds one weapon: the sprite, the mesh and skins, the item models; returns the stats of the mesh and the audit row. `preview` also renders the sheet (mesh views, the
    held view next to Steve, the sprite), `views` the two textured software views of K.held_render and K.first_person_render."""
    builder, sprite, butt, tip, apparent, grip, grip_mid = SPECS[item]
    cv, outline = sprite()
    K.save_sprite(item, cv, outline)
    m, atlas = builder()
    st = K.finish(item, m, atlas, butt, tip, apparent, grip, grip_mid)
    n = st["quads_written"]
    if n > MAX_QUADS:
        raise SystemExit("%s: %d quads is over the budget of %d" % (item, n, MAX_QUADS))
    if not 8 <= len(m.parts) <= 14:
        raise SystemExit("%s: %d parts is outside the budget of 8 to 14" % (item, len(m.parts)))
    row = K.audit(item)
    st["audit"] = row
    if preview:
        st["preview"] = K.preview(item)
    if views:
        os.makedirs(K.PREVIEW_DIR, exist_ok=True)
        K.held_render(item, os.path.join(K.PREVIEW_DIR, item + "_held.png"))
        K.first_person_render(item, os.path.join(K.PREVIEW_DIR, item + "_first_person.png"), size=(1280, 720), crop=(640, 0, 1280, 720))
    return st


def write_class_fragment():
    """tools/heroweapon/classes_hart2.json: the size class of each id (existing classes only, so no table of the game has to grow); the lead merges it."""
    frag = {"_doc": "hart2 (the Seven Star weapons): id -> a class of assets/nusmp/weapon_classes.json. No new class is needed. Merge into the `weapons` map of "
                    "tools/relics/weapon_classes.json (then python tools/relics/audit_weapon_scale.py --write copies it to the game).",
            "classes": {}, "weapons": {k: v for k, v in sorted(CLASS_OF.items())}}
    with open(K.CLASS_FRAGMENT, "w", newline="\n") as f:
        json.dump(frag, f, indent=1)
        f.write("\n")


MAX_QUADS = 2800                # body plus glow quads of one mesh file (the budget of every relic wave)


def main(argv):
    ids = [a for a in argv if not a.startswith("--")] or list(SPECS)
    for item in ids:
        if item not in SPECS:
            raise SystemExit("unknown id %s; known: %s" % (item, ", ".join(SPECS)))
    write_class_fragment()
    if "--selfcheck" in argv:
        for item in ids:
            make(item)
            a = K.sha(K.outputs_of(item))
            make(item)
            b = K.sha(K.outputs_of(item))
            print("%-20s %s %s" % (item, a, "identical on a second run" if a == b else "DIFFERENT on a second run: %s" % b))
            if a != b:
                raise SystemExit(1)
        return
    for item in ids:
        st = make(item, preview="--preview" in argv, views="--views" in argv)
        au = st["audit"]
        print("%-18s parts %2d  quads %4d (body %d, glow %d)  skin %.2f px/unit  apparent %.2f (class %s %.2f-%.2f)  head %.2f  flags %s %s%s" % (
            item, st["parts"], st["quads_written"], st["quads"][0], st["quads"][2], st["atlas_ppu"], au["apparent"], au["class"], au["min"], au["max"], au["head"],
            ",".join(au["flags"]) or "-", ",".join(au["status"]) or "ok", "  " + st.get("preview", "")))


if __name__ == "__main__":
    main(sys.argv[1:])
