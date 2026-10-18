"""The Rising of the Shield Hero (1.62.0): the four Holy Weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/heroweapon/art_hart1.py [id ...]       (no ids = all eight looks; idempotent, deterministic)
    HART1_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart1.py legendary_shield      (also renders the mesh preview into <dir>)

Why this file exists: the owner's rule 7 says every weapon is a real 3D model in hand with its old 16 x 16 sprite kept as the inventory icon, and the
Genesis Demon-Slayer is the visual standard. The four Holy (Cardinal) Weapons are the face of the Shield Hero job, so each has two looks: the base look
(noble, plain, strong: clean steel, white-gold fittings and ONE bright colour, the gem) and the curse look (the same weapon corrupted by its sin: the same
grip and footprint, a more monstrous silhouette, a hotter glow). Per look it writes
  assets/nusmp/weapon_meshes/<id>.txt                  the mesh (smoothed per-vertex "qn" normals), placed like every other mesh
  assets/nusmp/textures/entity/weapon/<id>.png         the painted skin (512 px) and <id>_glow.png the additive glow skin (the gem, cracks, runes)
and, for the four base looks only,
  assets/nusmp/textures/item/<id>.png                  the 16 x 16 sprite (the inventory icon; hand-built pixels, not a downscaled render)
  assets/nusmp/models/item/<id>.json, <id>_icon.json   the in-hand model (builtin/entity + display transforms) and the flat inventory model
plus tools/heroweapon/classes_hart1.json (the size class of every id, for the lead to merge into weapon_classes.json).

The method, in order, for every look (each build_* below follows it):
  0 target decided: the series' weapon (gem colour from the wiki summaries, shape from the anime as remembered, said in each docstring) and the owner's text.
  1 proportions as NAMED CONSTANTS with a comment saying where each came from (real-life sizes of the class table, the series, or "design").
  2 geometry from real cross-sections (loft / sweep / revolve), never a box for a long form; the small raised parts are their own named Parts.
  3 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one 512 px skin per look.
  4 painted material (steel, gold, leather ... with highlights, wear, engraving) plus a second additive glow skin for the gem and the sin's fire.
  5 rendered (mesh_lib.preview and tools/relics/weapon_sheet.py's held view), LOOKED AT and corrected: silhouette, sections, raised parts, texture.

Axes (docs/weapon_scale_orientation.md): y runs along the weapon (grip low, tip high), mesh -x is forward (the edge side), z is the thickness. The shield and
the bow stand upright like the wave 23 relics: the shield's face looks to +z and its handle is behind (-z); the bow's belly (its grip) is at +x, its string
near x = 7 where the limb tips curl back to. Units are centimetres of the real weapon; `place` derives the in-game scale from the class table's apparent length
(the third-person display scale is 0.85 for the sword, the spear and the bow, 1.05 for the shield: see THIRD).
"""
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import mesh_lib as ml                                                    # noqa: E402
import wave1_models as w1                                                # noqa: E402  (add, paint_part, set_glow, hermite, noise2 ...)

V = np.array
ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
PREVIEW = os.environ.get("HART1_PREVIEW", "")
SKIN_PX = 512                                                            # one skin of 512 px per look (the art contract)

IDS = ["legendary_shield", "legendary_sword", "legendary_spear", "legendary_bow"]
LOOKS = [i for base in IDS for i in (base, base + "_curse")]

# ---------------------------------------------------------------- palette (rgb 0..255). The four weapons share the steel and the white gold; only the gem differs.
STEEL = (150, 158, 172)              # polished blue-grey steel: mesh_lib's own "steel" base, the clean metal of the base looks
STEEL_BRIGHT = (186, 194, 208)       # blades and bright edges
WHITE_GOLD = (226, 182, 78)          # the fittings: mesh_lib's gold, a little warmer; the set's second metal
DARK_STEEL = (82, 90, 108)           # the shield boss, the spear collar: steel in shadow
LEATHER = (98, 62, 40)
BLACK_IRON = (36, 32, 44)            # the curse looks' metal: iron with a purple cast
GEM = {                              # the series' gem colours (fandom wiki summaries: green shield, blue sword, red spear, yellow bow)
    "shield": (34, 196, 100), "sword": (46, 112, 255), "spear": (255, 62, 70), "bow": (255, 206, 60),
}
SIN = {                              # the curse looks' fire (design: wrath red, greed and gluttony gold, lust magenta, pride violet)
    "shield": (255, 58, 30), "sword": (255, 186, 38), "spear": (255, 58, 196), "bow": (176, 96, 255),
}


# ================================================================ generic geometry helpers
def add(m, name, material, **kw):
    """A new named part of the mesh (a thin alias so every builder reads the same)."""
    return m.add(ml.Part(name, material=material, **kw))


def unit(v):
    v = V(v, float)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-12 else v


def _axes(axis):
    """(along, u, v) of a named axis, right-handed (u x v = along), so a ring's angle runs counter-clockwise seen from +axis."""
    return {"x": (V((1.0, 0, 0)), V((0, 1.0, 0)), V((0, 0, 1.0))),
            "y": (V((0, 1.0, 0)), V((0, 0, 1.0)), V((1.0, 0, 0))),
            "z": (V((0, 0, 1.0)), V((1.0, 0, 0)), V((0, 1.0, 0)))}[axis]


def revolve(part, profile, sides=32, axis="z", origin=(0.0, 0.0, 0.0), start=0.0):
    """A surface of revolution of a (radius, along) polyline that may fold back on itself (a dished shield plate, which a lathe cannot make because
    its radius is not a single-valued function of the height). The profile runs counter-clockwise in the (radius right, along up) plane with the solid on
    its left, e.g. from the back centre out along the back, up the edge and in along the front; a radius of 0 is a single point. Faces are wound
    outward (the game culls back faces); an open profile keeps that winding, a closed one is checked by Part.ensure_outward at finish."""
    a, eu, ev = _axes(axis)
    o = V(origin, float)
    rings = []
    for (r, s) in profile:
        if r <= 1e-9:
            rings.append(ml.Ring([part.add_vertex(o + a * s)] * sides))
        else:
            pts = [o + a * s + (eu * math.cos(start + 2 * math.pi * i / sides) + ev * math.sin(start + 2 * math.pi * i / sides)) * r for i in range(sides)]
            rings.append(part.add_ring(pts))
    for k in range(len(rings) - 1):
        part.bridge(rings[k], rings[k + 1])
    return rings


def circle_path(r, z=0.0, n=40, cx=0.0, cy=0.0, start=0.0):
    """A closed counter-clockwise loop in a z = const plane (the first point repeated at the end so a sweep closes on itself)."""
    pts = [(cx + r * math.cos(start + 2 * math.pi * i / n), cy + r * math.sin(start + 2 * math.pi * i / n), z) for i in range(n)]
    return pts + [pts[0]]


def band_loop(part, r, z, section, n=36, arcs=1, cx=0.0, cy=0.0):
    """A closed band or tube round a circle in a z = const plane (a rim, a bezel, a ring bead). `section` is a (u, v) loop, u radial (outward), v up.
    It is built as ONE ring of stations so every join is exact (a chain of separate sweeps leaves hairline cracks where their end frames differ), but
    the faces are tagged into `arcs` separate unrolled strips: one long strip would waste most of the 512 px skin, short strips pack."""
    sec = [tuple(p) for p in section]
    m = len(sec)
    rings = []
    for i in range(n):
        a = 2 * math.pi * i / n
        rad = V((math.cos(a), math.sin(a), 0.0))
        rings.append(part.add_ring([rad * (r + u) + V((cx, cy, z + v)) for (u, v) in sec]))
    seg = [math.hypot(sec[(k + 1) % m][0] - sec[k][0], sec[(k + 1) % m][1] - sec[k][1]) for k in range(m)]
    arc = [0.0]
    for e in seg:
        arc.append(arc[-1] + e)
    step = 2 * math.pi * r / n
    per = n // arcs
    for k in range(arcs):
        cnt = per if k < arcs - 1 else n - per * (arcs - 1)
        rec = {"arc": [list(arc) for _ in range(cnt + 1)], "perim": [arc[-1]] * (cnt + 1), "along": [step * j for j in range(cnt + 1)],
               "length": step * cnt, "n": m}
        for j in range(cnt):
            i = k * per + j

            def tag(order, j=j, rec=rec):
                idxs = [q for (_s, q) in order]
                wrap = (m - 1) in idxs and 0 in idxs
                return ("loft", rec, [(j + (0 if sd == "a" else 1), m if (wrap and q == 0) else q) for (sd, q) in order])
            part.bridge(rings[i], rings[(i + 1) % n], flip=True, tag=tag)
    return rings


def bezier(p0, p1, p2, n=10, p3=None):
    """Points of a quadratic (or cubic when p3 is given) Bezier curve from p0 to the last point: the centre line of a horn, a thorn or a feather."""
    pts = []
    for i in range(n + 1):
        t = i / n
        if p3 is None:
            q = (1 - t) ** 2 * V(p0, float) + 2 * (1 - t) * t * V(p1, float) + t ** 2 * V(p2, float)
        else:
            q = (1 - t) ** 3 * V(p0, float) + 3 * (1 - t) ** 2 * t * V(p1, float) + 3 * (1 - t) * t ** 2 * V(p2, float) + t ** 3 * V(p3, float)
        pts.append(tuple(q))
    return pts


def horn(part, p0, p1, p2, r0, sides=6, n=9, up=(0, 0, 1), taper=1.0, base_r=None, p3=None):
    """A curved, tapering spike along a Bezier centre line: round at the root (radius r0), a point at the tip. taper > 1 keeps it thick longer."""
    pts = bezier(p0, p1, p2, n, p3)

    def sc(t):
        return (0.0, 0.0) if t > 0.985 else (r0 * (1 - t ** taper) ** 0.9 + 0.02, r0 * (1 - t ** taper) ** 0.9 + 0.02)
    return ml.sweep(part, pts, ml.circle(1.0, sides), scales=sc, caps=(True, False), up=up)


def cone(part, base, direction, length, r, sides=6, r_tip=0.0, caps=(True, False)):
    """A straight cone (a spike, a tooth, a claw) from `base` along `direction`."""
    st = [(0.0, 1.0, 1.0), (length, r_tip / max(r, 1e-9), r_tip / max(r, 1e-9))]
    return ml.loft(part, tuple(unit(direction)), tuple(base), st, ml.circle(r, sides), caps=caps)


def fang(part, base, direction, length, width, thick, up=(0, 0, 1), curve=0.0, bend=(0, 0, 0), n=6):
    """A flat, tapering tooth or blade-thorn: a lozenge section that narrows to a point; `curve` bends its tip along `bend`."""
    d = unit(direction)
    p0 = V(base, float)
    p2 = p0 + d * length + V(bend, float) * curve
    p1 = p0 + d * length * 0.55
    pts = bezier(p0, p1, p2, n)

    def sc(t):
        return (0.0, 0.0) if t > 0.985 else (width * (1 - t) ** 0.8 + 0.02, thick * (1 - t) ** 0.6 + 0.02)
    return ml.sweep(part, pts, ml.lozenge(1.0, 1.0), scales=sc, caps=(True, False), up=up)


def sphere(part, c, r, sides=12, stacks=8, squash=(1.0, 1.0, 1.0)):
    """A (squashable) ball: a lathe round y with a half-circle profile."""
    prof = [(-r * squash[1] * math.cos(math.pi * i / stacks), r * math.sin(math.pi * i / stacks)) for i in range(stacks + 1)]
    return ml.lathe(part, "y", c, prof, sides=sides, squash=squash[2] / max(squash[0], 1e-9) if squash[0] else 1.0)


def link_loop(part, centre, a_axis, b_axis, ra, rb, wire, sides=5, n=10):
    """One chain link: a flattened ring in the plane of `a_axis` / `b_axis` round `centre`, wire radius `wire`."""
    c, ua, ub = V(centre, float), unit(a_axis), unit(b_axis)
    pts = [tuple(c + ua * ra * math.cos(2 * math.pi * i / n) + ub * rb * math.sin(2 * math.pi * i / n)) for i in range(n)]
    pts.append(pts[0])
    return ml.sweep(part, pts, ml.circle(wire, sides), caps=(False, False), up=tuple(np.cross(ua, ub)))


# ================================================================ painting helpers (numpy; float32 RGBA 0..255 views of the skin)
def paint_metal(atlas, m, names, sheen=0.14, grain=0.045, seed=0):
    """A polished, lightly worn metal skin for the named parts in the part's own base colour: soft blotches and a fine grain, and on round (loft) parts a sheen
    that runs along the length. mesh_lib's default metal is brushed in long streaks, which on a gold pommel or a gold wing reads as wood grain."""
    for k, nm in enumerate(names):
        part = m.part(nm)
        base = np.array(part.base if part.base is not None else (200, 200, 200), np.float32)

        def fn(c, base=base, k=k):
            shade = 1 + grain * 2 * (noise(c, 16, seed + k * 7 + 1) - 0.5) + 0.02 * 2 * (noise(c, 2.4, seed + k * 7 + 2) - 0.5)
            img = base[None, None, :] * shade[..., None]
            if c.kind == "loft":
                img = img * (1 + sheen * band(c.S, 0.30, 0.30))[..., None]
            c.img[..., :3] = np.clip(img, 0, 255)
            c.img[..., 3] = 255
        paint_group(atlas, part, fn)


def polar(c, cx=0.0, cy=0.0):
    """(rho, theta) of every pixel of a dominant-axis group painted in model x / y (ctx.A, ctx.B are the model coordinates)."""
    dx, dy = c.A - cx, c.B - cy
    return np.hypot(dx, dy), np.arctan2(dy, dx)


def blend(img, rgb, mask, k=1.0):
    """img <- img + (rgb - img) * mask * k (mask 0..1, shape of the patch)."""
    m = (np.clip(mask, 0, 1) * k)[..., None]
    img[..., :3] = img[..., :3] * (1 - m) + np.array(rgb, np.float32) * m


def tone(img, mask, factor):
    """Multiplies the colour by 1 + (factor - 1) * mask: darken (factor < 1) or lighten (> 1) where the mask is."""
    m = (np.clip(mask, 0, 1) * (factor - 1) + 1)[..., None]
    img[..., :3] = np.clip(img[..., :3] * m, 0, 255)


def band(x, centre, width):
    """A soft 0..1 line profile of half width `width` round `centre` (smooth cosine falloff, in the units of x)."""
    d = np.abs(x - centre) / max(width, 1e-6)
    return np.clip(1 - d, 0, 1) ** 1.5


def glow_into(c, rgb, mask, strength=1.0, core=0.35):
    """Adds a glow patch to the glow skin where mask is set: the colour `rgb`, a hot core a little toward white where the mask is strong."""
    m = np.clip(mask, 0, 1).astype(np.float32)
    col = np.array(rgb, np.float32)
    hot = col + (255 - col) * core
    out = np.zeros(c.img.shape, np.float32)
    out[..., :3] = col[None, None, :] + (hot - col)[None, None, :] * (m ** 2)[..., None]
    out[..., 3] = m * 255 * strength
    c.glow[...] = np.maximum(c.glow, out)


def groove(c, mask, depth=0.5, rim=0.18):
    """An engraved line from a 0..1 mask: darkens the line and lightens a thin shoulder beside it (the owner's engraving look in mesh_lib.Atlas.line)."""
    m = np.clip(mask, 0, 1)
    tone(c.img, m, 1 - depth)
    sh = np.clip(np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3)), np.float32) / 255 - m, 0, 1)
    c.img[..., :3] += 255 * rim * sh[..., None]
    c.img[..., :3] = np.clip(c.img[..., :3], 0, 255)


def noise(c, cell, seed):
    """Smooth value noise 0..1 over the patch (features about `cell` px)."""
    return w1.noise2(c.img.shape[:2], cell, seed)


def gnoise(c, sigma, seed):
    """Smooth Gaussian-field noise 0..1 over the patch (features about `sigma` px): white noise low-passed in the frequency domain. Its contour lines wind like
    real cracks, which the bicubic value noise of `noise` does not (that one has axis-aligned corners)."""
    h, w = c.img.shape[:2]
    rng = np.random.default_rng(seed)
    f = np.fft.rfft2(rng.standard_normal((h, w)))
    fy = np.fft.fftfreq(h)[:, None]
    fx = np.fft.rfftfreq(w)[None, :]
    f *= np.exp(-2 * (math.pi * sigma) ** 2 * (fx ** 2 + fy ** 2))
    g = np.fft.irfft2(f, s=(h, w))
    g = (g - g.mean()) / max(float(g.std()), 1e-9)
    return np.clip(0.5 + 0.25 * g, 0, 1).astype(np.float32)


def to_gem(c, rgb, hi, t, glow_strength=1.0, seed=3, spec=None, core=0.0):
    """Paints a cabochon: `t` is 0 at the dark rim of the stone and 1 at the polished crown. The body skin stays a deep, saturated colour (the additive glow
    layer is added on top of it in game, so a bright body would burn out to white); the glow skin carries the light: full colour at the crown, a soft halo at
    the rim. `spec` (0..1) is a small white highlight, `hi` only tints the brightest body pixels."""
    t = np.clip(t, 0, 1)
    n = noise(c, 5, seed)
    base = np.array(rgb, np.float32)
    top = base * 0.42 + np.array(hi, np.float32) * 0.18
    deep = base * 0.2
    k = (t ** 1.4)[..., None]
    c.img[..., :3] = deep[None, None, :] + (top - deep)[None, None, :] * k
    c.img[..., :3] *= (0.9 + 0.2 * n)[..., None]
    if spec is not None:
        c.img[..., :3] = c.img[..., :3] * (1 - 0.85 * spec[..., None]) + 255 * 0.85 * spec[..., None]
    c.img[..., 3] = 255
    glow_into(c, rgb, 0.3 + 0.6 * t ** 1.2, glow_strength * 0.9, core=core)
    if spec is not None:
        c.glow[..., :3] = np.clip(c.glow[..., :3] + 120 * (spec[..., None] * (c.glow[..., 3:4] > 0)), 0, 255)


# ================================================================ placement and writing (shared by the four weapons)
APPARENT = {                   # blocks as seen in the third-person hand, inside the class table's range (docs/weapon_scale_orientation.md 10.3)
    "legendary_shield": 1.06,  # class shield 0.90 - 1.20: the upper half, these are the heroes' weapons
    "legendary_sword": 1.70,   # class longsword 1.40 - 1.80
    "legendary_spear": 3.30,   # class spear 2.80 - 3.60
    "legendary_bow": 1.50,     # class bow 1.30 - 1.60
}
THIRD = {"legendary_shield": 1.05, "legendary_bow": 0.85, "legendary_sword": 0.85, "legendary_spear": 0.85}   # the third-person display scale of each model json (the shield: the Magebane Shield's, the bow: its own)
_BASE_PLACE = {}               # the base look's place line, reused by the curse look so the hand fits (same scale, same offsets)


def upright_place(mesh, item_id, hold_x=None):
    """`place` of an upright weapon (the shield, the bow): centred on the item cube at angle 90, long enough that mesh length x third-person scale is the class
    table's apparent length. The item cube's centre is where the hand holds, so `hold_x` (the mesh x of the grip, default the middle of the box) goes there."""
    ymin, ymax = mesh.y_range()
    lo, hi = mesh.bbox()
    length = APPARENT[item_id] / THIRD[item_id]
    scale = length / (ymax - ymin)
    cx = (lo[0] + hi[0]) / 2 if hold_x is None else hold_x
    return (0.5 - cx * scale, 0.5 - length / 2, 90.0, scale, ymin)


def base_place(base, builder, place_of):
    """The base look's `place` line, whichever look is being built: a curse look reuses it so the plate, blade, shaft or limbs keep the base look's exact size and the
    hand fits (building a curse look alone must give the same file as building both). Builds the base mesh in memory when it was not built in this run."""
    if base not in _BASE_PLACE:
        write_icon(base)                                                 # the diagonal weapons are placed on their sprite
        mesh, _ = builder(False, write=False)
        _BASE_PLACE[base] = place_of(mesh)
    return _BASE_PLACE[base]


def write_look(item_id, mesh, atlas, place, glow, quiet=False):
    """Writes the mesh and the two skins of one look and returns its statistics. The curse look has the id `<base>_curse` and its own skins. The meshes carry
    `orient manual`: their orientation, size and grip were checked on the audit sheets, and the runtime safety net (client/relics/item/WeaponNormaliser) must not
    second-guess a polearm's head, a bow or a shield (docs/weapon_scale_orientation.md: those read as false positives)."""
    mesh.place(*place)
    mesh.set_glow(*glow)
    mesh.orient_cfg = "manual"
    os.makedirs(MESH_DIR, exist_ok=True)
    n = mesh.write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment="generated by tools/heroweapon/art_hart1.py")
    atlas.save(item_id)
    st = mesh.stats(atlas)
    st["lines"] = n
    if not quiet:
        print(f"{item_id}: {len(mesh.parts)} parts, {n} quad lines {st['quads']}, length {st['length']:.1f} cm, skin {atlas.size}px at {atlas.ppu:.2f} px/cm")
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        ml.preview(mesh, None, os.path.join(PREVIEW, item_id + ".png"), atlas=atlas, normals="vertex")
    return st


def diag_place(mesh, item_id, hold):
    """`place` of a diagonal weapon (the sword, the spear): the mesh's y axis on the sprite's diagonal, grown to the class table's apparent length, with the
    mesh point at `hold` (a share of its length: the middle of the hand grip) put where the hand takes a sprite weapon, so the hand closes on the real grip.
    The growth point of mesh_lib.place_on_sprite is solved for that: the mesh point at share f lands at  grip (1 - k) + f k  along the sprite axis (k = how much
    longer the mesh is than the sprite), and the sprite is held at the axis point nearest the vanilla hilt (0.25, 0.25) (tools/relics/display_presets.py)."""
    icon = os.path.join(ICON_DIR, item_id + ".png")
    origin, angle, length = ml.sprite_axis(icon)
    k = APPARENT[item_id] / (THIRD[item_id] * length)
    d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
    held = float(np.dot(np.array([0.25, 0.25]) - origin, d)) / length
    grip = (held - hold * k) / (1.0 - k)
    return mesh.place_on_sprite(icon, real_m=APPARENT[item_id], grip=grip, hand_scale=THIRD[item_id])


# The in-hand display transforms (docs/asset_pipeline.md): the sword and the spear take the standard diagonal transforms of the other swords; the shield takes
# the Magebane Shield's (held on the forearm, face out, with its raised "blocking" model); the bow has its own, solved with tools/relics/weapon_sheet.py's hand
# chain so that it is held upright with its belly forward like an archer's, not lying flat like the wave 23 bows: the item cube's centre is the hand, the grip
# is at the cube's centre, the display turns the cube so that the mesh's +y is up, its +x (the belly) is forward and its +z (the face) is outward.
BOW_DISPLAY = {   # 1.63.0: vanilla's own Bow display (the 1.62.0 hand-solved [75, 90, 0] stood the bow up in front of the player: owner's screenshot 81); tools/relics/bow_vanilla.py also places the meshes on the sprite diagonal
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def write_item_models(item_id):
    """models/item/<id>.json (builtin/entity + display transforms) and <id>_icon.json (the flat sprite model); the shield also gets its raised "blocking" model;
    then the first-person, ground and frame scales of a long mesh exactly as tools/relics/display_presets.py would set them (so the lead's later run changes nothing)."""
    def dump(name, data):
        with open(os.path.join(MODEL_DIR, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(data, indent=2))

    wi.write_models(item_id, UPRIGHT[item_id])                           # the icon model and a first in-hand model (the standard displays)
    with open(os.path.join(MODEL_DIR, item_id + ".json"), encoding="utf-8") as f:
        model = json.load(f)
    if item_id == "legendary_shield":
        with open(os.path.join(MODEL_DIR, "magebane_shield.json"), encoding="utf-8") as f:
            src = json.load(f)
        with open(os.path.join(MODEL_DIR, "magebane_shield_blocking.json"), encoding="utf-8") as f:
            raised = json.load(f)
        model["display"] = src["display"]
        model["overrides"] = [{"predicate": {"blocking": 1}, "model": "nusmp:item/" + item_id + "_blocking"}]
        raised["textures"] = {"particle": "nusmp:item/" + item_id}
        dump(item_id + "_blocking", raised)
    elif item_id == "legendary_bow":
        model["display"] = BOW_DISPLAY
    if item_id in ("legendary_shield", "legendary_bow"):
        model["nusmp_display_preset"] = "manual"                          # hand-tuned: tools/relics/display_presets.py leaves it alone, the renderer skips its net
    dump(item_id, model)
    sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
    import display_presets as dp
    mesh = dp.read_mesh(os.path.join(MESH_DIR, item_id + ".txt"))
    plan = dp.plan_for(item_id, model, mesh)
    if item_id != "legendary_bow":                                       # 1.63.0: the bow is vanilla's (BOW_DISPLAY above); the others follow the rule
        dump(item_id, dp.apply_plan(model, plan))
    return plan


def lay(m):
    """The atlas of a finished mesh: layout, then the default painted materials; the builders paint their details on top."""
    atlas = ml.Atlas(SKIN_PX)
    atlas.layout(m)
    atlas.paint(m)
    return atlas


# ---------------------------------------------------------------- painting helpers that need the group's pixel <-> model mapping
def px_of(c, x, y):
    """The (column, row) inside the patch of the model point (x, y), for a dominant-axis group seen from +z (a = x, b = y)."""
    return ((x - c.g.origin[0]) * c.g.ppu, (c.g.origin[1] - y) * c.g.ppu)


def stroke_mask(c, polylines, width_cm, soft=0.7):
    """A 0..1 mask of polylines given in model (x, y) on a +z group: strokes `width_cm` wide, antialiased (drawn on a 3x canvas, then blurred)."""
    h, w = c.img.shape[:2]
    k = 3
    im = Image.new("L", (w * k, h * k), 0)
    d = ImageDraw.Draw(im)
    for pl in polylines:
        pts = [(px_of(c, x, y)[0] * k, px_of(c, x, y)[1] * k) for (x, y) in pl]
        d.line(pts, fill=255, width=max(1, int(round(width_cm * c.g.ppu * k))), joint="curve")
    im = im.resize((w, h), Image.BILINEAR)
    if soft > 0:
        im = im.filter(ImageFilter.GaussianBlur(soft))
    return np.asarray(im, np.float32) / 255.0


def poly_mask(c, polys, soft=0.6):
    """A 0..1 mask of filled polygons given in model (x, y) on a +z group (antialiased like stroke_mask)."""
    h, w = c.img.shape[:2]
    k = 3
    im = Image.new("L", (w * k, h * k), 0)
    d = ImageDraw.Draw(im)
    for pg in polys:
        d.polygon([(px_of(c, x, y)[0] * k, px_of(c, x, y)[1] * k) for (x, y) in pg], fill=255)
    im = im.resize((w, h), Image.BILINEAR)
    if soft > 0:
        im = im.filter(ImageFilter.GaussianBlur(soft))
    return np.asarray(im, np.float32) / 255.0


def rhombus(cx, cy, along, across, angle):
    """The four corners of a rhombus centred on (cx, cy): half-diagonals `along` (turned by `angle`) and `across`."""
    ca, sa = math.cos(angle), math.sin(angle)
    return [(cx + ca * along, cy + sa * along), (cx - sa * across, cy + ca * across), (cx - ca * along, cy - sa * along), (cx + sa * across, cy - ca * across)]


def lathe_theta(c, prof, s0=None):
    """(turn, radius) of every pixel of a loft group made by ml.lathe from `prof` = [(s, r)]: `turn` is the fraction 0..1 round the axis, `radius` the ring
    radius there. Needed because each ring of a lathe is centred in the unrolled strip, so a vertical line of the strip is NOT a line of constant angle."""
    ss = np.array([q[0] for q in prof], np.float64)
    rr = np.array([q[1] for q in prof], np.float64)
    along = c.B + (ss[0] if s0 is None else s0)
    r = np.maximum(np.interp(along, ss, rr), 1e-3)
    pmax = 2 * math.pi * rr.max()
    perim = 2 * math.pi * r
    return np.clip((c.A - (pmax - perim) / 2) / perim, 0, 1), r


def smooth01(x, lo, hi):
    """A smoothstep from 0 at `lo` to 1 at `hi` (either order)."""
    t = np.clip((x - lo) / (hi - lo), 0, 1)
    return t * t * (3 - 2 * t)


def paint_group(atlas, part, fn):
    """Calls fn(ctx) for every UV group of `part` (mesh_lib's wave1 helper: ctx.A / ctx.B model coordinates or arc / length, T along, S across)."""
    w1.paint_part(atlas, part, fn)


# ================================================================ the 16 x 16 sprites (the inventory icons, hand-built pixels) and the item models
# Why pixels and not a downscaled render: the sprite stays the inventory icon (rule 7), and a render at 16 px turns to mud. Each icon is a miniature in the
# weapon's palette on a transparent ground with the one-pixel dark outline of the other relic icons; the sword and the spear lie on the sprite diagonal (the mesh
# is placed on it), the shield and the bow stand upright (placed on the sprite's box).
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import wave1_icons as wi                                                 # noqa: E402  (Canvas, mix, shade, write_models: the owner's icon helpers)


def icon_sword():
    cv = wi.Canvas()
    mid, light, dark = (206, 214, 230), (248, 251, 255), (128, 142, 170)
    for i in range(9):                                                   # the blade: two pixels wide, light on the upper left, a dark lower edge
        x, y = 6 + i, 9 - i
        cv.put(x, y, mid)
        if i < 8:
            cv.put(x, y - 1, light)
        if i > 0:
            cv.put(x + 1, y, dark)
    cv.put(14, 1, light)
    gold, gold2 = (238, 192, 84), (170, 124, 40)
    for (x, y, c) in ((3, 8, gold2), (4, 9, gold), (6, 11, gold), (7, 12, gold2), (4, 8, gold), (6, 12, gold2)):   # the winged crossguard
        cv.put(x, y, c)
    cv.put(5, 10, GEM["sword"])                                          # the blue gem at its centre
    cv.put(5, 9, (176, 218, 255))
    for (x, y) in ((4, 11), (3, 12), (2, 13)):                           # the wrapped grip
        cv.put(x, y, (52, 76, 146) if (x + y) % 2 else (30, 46, 100))
    cv.put(1, 14, gold)
    cv.put(2, 14, gold2)
    cv.put(1, 15, gold2)
    return cv, (22, 28, 64)


def icon_spear():
    cv = wi.Canvas()
    steel, light, dark = (206, 214, 230), (248, 251, 255), (120, 134, 164)
    gold, gold2, red = (238, 192, 84), (170, 124, 40), (196, 52, 62)
    for i in range(9):                                                   # the long shaft: a silver pole, two pixels wide, gold bands and a red wrap
        x, y = 2 + i, 13 - i
        col = gold if i in (1, 7) else (red if i in (3, 4) else (232, 236, 244))
        cv.put(x, y, col)
        cv.put(x + 1, y, wi.shade(col, 0.62))
    for (x, y, c) in ((11, 4, steel), (12, 3, steel), (13, 2, steel), (14, 1, steel), (11, 3, light), (12, 2, light), (13, 1, light),
                      (12, 4, steel), (13, 3, steel), (14, 2, dark), (13, 4, dark), (15, 0, light)):   # the leaf head along the diagonal
        cv.put(x, y, c)
    cv.put(10, 5, GEM["spear"])                                          # the red gem in the socket, between two gold wings
    cv.put(9, 4, gold)
    cv.put(11, 6, gold)
    cv.put(10, 4, gold2)
    cv.put(11, 5, gold2)
    cv.put(1, 14, gold2)
    cv.put(2, 14, gold)
    return cv, (36, 40, 66)


def icon_shield():
    cv = wi.Canvas()
    gold, gold2 = (238, 192, 84), (170, 124, 40)
    for y in range(16):                                                  # the disc: a diagonal sheen from the upper left
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.6:
                cv.put(x, y, wi.mix((214, 222, 238), (136, 150, 182), min(1.0, max(0.0, (x + y - 6) / 18.0))))
    cv.ring(7.5, 7.5, 5.7, 6.7, gold)
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):                  # the rim rivets
        cv.put(x, y, gold2)
    for k in (3, 4, 5):                                                  # four ribs in a pinwheel, one pixel wide
        cv.put(7, k, gold)
        cv.put(8, 15 - k, gold)
        cv.put(k, 8, gold)
        cv.put(15 - k, 7, gold)
    for (x, y, c) in ((7, 7, (176, 255, 206)), (8, 7, GEM["shield"]), (7, 8, GEM["shield"]), (8, 8, (22, 150, 84))):   # the green gem
        cv.put(x, y, c)
    return cv, (24, 52, 44)


def icon_bow():
    cv = wi.Canvas()
    gold, gold2, white = (238, 192, 84), (170, 124, 40), (238, 240, 248)
    for y in range(16):                                                  # the stave: the belly bulges to the left, the string is the line x = 12
        t = (y - 7.5) / 7.5
        x = round(12 - 7.0 * (1 - t * t) ** 0.9)
        in_grip = 5 <= y <= 10
        cv.put(x, y, gold if in_grip else white)
        cv.put(x + 1, y, gold2 if in_grip else (170, 180, 202))
    for y in (0, 1, 14, 15):
        cv.put(12 - (1 if y in (1, 14) else 0), y, gold)
    cv.line(12, 0, 12, 15, (240, 232, 196))                              # the string
    cv.put(5, 7, GEM["bow"])                                             # the yellow gem in the grip
    cv.put(5, 8, (255, 168, 40))
    cv.put(4, 7, (255, 244, 170))
    return cv, (70, 52, 16)


ICON_FUNCS = {"legendary_shield": icon_shield, "legendary_sword": icon_sword, "legendary_spear": icon_spear, "legendary_bow": icon_bow}
UPRIGHT = {"legendary_shield": True, "legendary_bow": True, "legendary_sword": False, "legendary_spear": False}


def write_icon(item_id):
    """The 16 x 16 sprite textures/item/<id>.png (outlined like the other relic icons) and the two item models."""
    cv, outline = ICON_FUNCS[item_id]()
    cv.outline(outline)
    os.makedirs(ICON_DIR, exist_ok=True)
    cv.image().save(os.path.join(ICON_DIR, item_id + ".png"))
    return cv


def sheet_of_icons(path):
    """An enlarged contact sheet of the four sprites for looking at them (12 x)."""
    z = 14
    sh = Image.new("RGBA", (4 * (16 * z + 10) + 10, 16 * z + 20), (74, 76, 92, 255))
    for k, iid in enumerate(IDS):
        big = Image.open(os.path.join(ICON_DIR, iid + ".png")).convert("RGBA").resize((16 * z, 16 * z), Image.NEAREST)
        sh.paste(big, (10 + k * (16 * z + 10), 10), big)
    sh.save(path)


# ================================================================ 1. THE LEGENDARY SHIELD (Naofumi Iwatani; Wrath)
# Target (the wiki summaries give only the green gem; the rest is from memory of the anime and design): the Small Shield is a plain round steel shield with a
# gold rolled rim, a boss and one green gem at the centre. The four ribs are a compass for the Four Cardinal Heroes. The Shield of Wrath (curse look) is the same
# shield turned black, spiked and burning, with a maw of teeth round the gem. Real size: a 62 cm round shield, scaled to 1.06 blocks apparent.
SH_R = 31.0                    # outer radius, rim included
SH_RIM_W, SH_RIM_T = 3.8, 3.6  # the rolled rim band: radial width, thickness (design: a stout rim reads from a block away)
SH_RIM_R = SH_R - SH_RIM_W / 2  # the rim's centre line
SH_EDGE_R = 29.6               # the plate's own edge, under the rim
SH_BOSS_R = 12.2               # the boss round the gem
SH_GEM_R = 5.0                 # the cabochon's footprint radius: 16 % of the shield's radius, big enough to read as the jewel from a block away
SH_GEM_H = 4.4                 # and its height
SH_GEM_Z = 5.75                # where the gem sits (the boss top)
SH_BOSS_PROFILE = [(2.6, SH_BOSS_R), (3.3, SH_BOSS_R - 0.2), (4.2, 11.4), (5.0, 10.2), (5.5, 8.6), (SH_GEM_Z, 6.2)]   # (height, radius) of the boss dome
SH_RIB_R0, SH_RIB_R1 = 11.4, 27.0   # the four cardinal ribs run from the boss to the rim
SH_BEAD_R = 18.2               # the raised ring bead between the ribs
SH_TICK_R = 14.9               # the engraved compass ticks round the boss
SH_STRAP_Y = -9.0              # the arm strap and the hand grip behind the shield: where the wielder holds it
SH_SPIKES = 12                 # the Wrath shield's rim spikes (long and short alternating)
SH_MAW = 12                    # the Wrath shield's teeth round the gem

_SH_FRONT = w1.hermite([(0, 3.5), (6, 3.45), (10, 3.1), (13, 2.2), (16, 1.4), (20, 1.1), (25, 1.05), (28, 1.3), (SH_EDGE_R, 1.5)])
_SH_BACK = w1.hermite([(0, -0.7), (10, -1.0), (20, -1.4), (27, -1.7), (SH_EDGE_R, -1.7)])


def shield_front(r):
    """Height of the plate's face at radius r (a shallow dish round a domed centre)."""
    return float(_SH_FRONT(r))


def shield_plate(m, curse):
    p = add(m, "plate", "iron" if curse else "steel", base=BLACK_IRON if curse else STEEL, smooth=40, wear=0.6)
    back = [(r, float(_SH_BACK(r))) for r in (0.0, 10.0, 20.0, SH_EDGE_R)]                          # out along the dished back
    front = [(r, shield_front(r)) for r in (SH_EDGE_R, 26.0, 22.5, 19.0, 15.5, 12.0, 8.0, 4.0, 0.0)]   # up the edge, in along the face
    revolve(p, back + front, sides=32 if curse else 40)
    return p


def shield_rim(m, curse):
    p = add(m, "rim", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=40, uv_mode="loft", wear=0.7)
    band_loop(p, SH_RIM_R, 0.0, ml.octagon(SH_RIM_W, SH_RIM_T, 1.0), n=36 if curse else 48, arcs=4)
    return p


def shield_ribs(m, curse):
    """Four cardinal ribs (long tapering bars on the face) and the raised ring bead they cross: the compass of the Four Cardinal Heroes."""
    ribs = add(m, "ribs", "iron" if curse else "gold", base=(52, 44, 58) if curse else WHITE_GOLD, smooth=45, uv_mode="loft", wear=0.6)
    for k in range(4):
        phi = k * math.pi / 2
        pts = [(r * math.cos(phi), r * math.sin(phi), shield_front(r) + 0.3) for r in np.linspace(SH_RIB_R0, SH_RIB_R1, 8)]
        ml.sweep(ribs, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (3.4 * (1 - 0.72 * t), 1.25 * (1 - 0.35 * t)), caps=(True, True), up=(0, 0, 1))
    bead = add(m, "field_ring", "iron" if curse else "gold", base=(48, 42, 54) if curse else WHITE_GOLD, smooth=60, uv_mode="loft", wear=0.6)
    sd, nn = (5, 24) if curse else (6, 40)
    band_loop(bead, SH_BEAD_R, shield_front(SH_BEAD_R) + 0.35, ml.circle(0.85, sd), n=nn, arcs=4)
    band_loop(bead, SH_BOSS_R + 0.15, 2.75, ml.circle(0.9, sd), n=nn, arcs=2)                       # the collar where the boss meets the plate
    return ribs, bead


def rivet(part, pos, r, sides=5, h=None):
    """A small domed rivet standing on a z-up surface (cheap: a three-band lathe that closes in a point)."""
    h = h or r * 0.85
    ml.lathe(part, "z", pos, [(-0.15, r), (0.0, r), (h * 0.55, r * 0.78), (h, 0.0)], sides=sides, caps=(False, False))


def shield_studs(m, curse):
    s = add(m, "studs", "steel", base=(70, 62, 72) if curse else STEEL_BRIGHT, smooth=40, wear=0.5)
    for i in range(0 if curse else 12):                                  # the rivets of the rim band (the Wrath look has spikes there instead)
        a = 2 * math.pi * (i + 0.5) / 12
        rivet(s, (SH_RIM_R * math.cos(a), SH_RIM_R * math.sin(a), SH_RIM_T / 2 - 0.1), 0.95, 4)
    for k in range(4):                                                   # the studs at the ribs' tips
        a = k * math.pi / 2
        r = SH_RIB_R1 - 1.0
        rivet(s, (r * math.cos(a), r * math.sin(a), shield_front(r) + 0.6), 1.45, 6)
    return s


def shield_boss(m, curse):
    """The boss: a smooth dome that rises from the plate to the gem's bezel (its eight flutes are painted, a sculpted flute only made it lumpy)."""
    b = add(m, "boss", "iron" if curse else "steel", base=(44, 38, 50) if curse else (112, 120, 138), smooth=45, wear=0.5, uv_mode="loft")
    ml.lathe(b, "z", (0, 0, 0), SH_BOSS_PROFILE, sides=32 if curse else 40, caps=(False, True))
    return b


def shield_gem(m, curse):
    """The cabochon: a smooth dome in a bezel. The base look holds it with eight small gold claws; the Wrath look has a maw of twelve long teeth round it (the
    stone is an eye in a gaping mouth)."""
    bez = add(m, "gem_setting", "iron" if curse else "gold", base=(60, 50, 64) if curse else WHITE_GOLD, smooth=50 if not curse else 35, wear=0.4)
    band_loop(bez, SH_GEM_R + 0.55, SH_GEM_Z + 0.15, ml.circle(1.05, 6), n=24 if curse else 32, arcs=1)
    if curse:
        for i in range(SH_MAW):
            a = 2 * math.pi * (i + 0.5) / SH_MAW
            r0 = SH_GEM_R + 1.0
            base = (r0 * math.cos(a), r0 * math.sin(a), SH_GEM_Z - 0.3)
            d = (-math.cos(a) * 0.32, -math.sin(a) * 0.32, 0.95)
            cone(bez, base, d, 7.2 if i % 2 == 0 else 5.0, 1.45 if i % 2 == 0 else 1.2, sides=4)
    else:
        for i in range(8):
            a = 2 * math.pi * (i + 0.5) / 8
            base = (SH_GEM_R * 1.02 * math.cos(a), SH_GEM_R * 1.02 * math.sin(a), SH_GEM_Z + 0.2)
            d = (-math.cos(a) * 0.55, -math.sin(a) * 0.55, 0.85)
            fang(bez, base, d, 3.6, 1.5, 1.0, up=(0, 0, 1), n=4)
    g = add(m, "gem", "crystal", base=SIN["shield"] if curse else GEM["shield"], glow=True, smooth=60, uv_mode="loft")
    prof = [(-0.5, SH_GEM_R * 0.86), (0.0, SH_GEM_R), (SH_GEM_H * 0.3, SH_GEM_R * 0.97), (SH_GEM_H * 0.62, SH_GEM_R * 0.78), (SH_GEM_H * 0.86, SH_GEM_R * 0.4),
            (SH_GEM_H, 0.0)]
    ml.lathe(g, "z", (0, 0, SH_GEM_Z), prof, sides=16, caps=(True, False))
    return bez, g


def shield_wrath(m):
    """The Shield of Wrath's rim spikes: twelve curved horns round the edge, long and short alternating, swept forward and swirled one way like a saw."""
    sp = add(m, "horns", "iron", base=(40, 34, 46), smooth=50, uv_mode="loft", wear=0.6)   # not named spikes: mesh_lib reads that word as "the business end" and writes grip 1.0
    for i in range(SH_SPIKES):
        a = 2 * math.pi * i / SH_SPIKES + math.pi / SH_SPIKES
        ln = 16.0 if i % 2 == 0 else 9.5
        rad, tang = V((math.cos(a), math.sin(a), 0.0)), V((-math.sin(a), math.cos(a), 0.0))
        p0 = rad * (SH_R - 2.2) + V((0, 0, 0.4))
        p1 = p0 + rad * ln * 0.62 + tang * ln * 0.10 + V((0, 0, 0.05 * ln))
        p2 = p0 + rad * ln * 0.82 + tang * ln * 0.30 + V((0, 0, 0.42 * ln))
        horn(sp, tuple(p0), tuple(p1), tuple(p2), 2.5 if i % 2 == 0 else 2.0, sides=4, n=5, up=(0, 0, 1))
    return sp


def shield_back(m, curse):
    """The arm strap (a leather bridge across the back) and the hand grip bar: the wielder's hold; the same in both looks so the hand fits."""
    s = add(m, "arm_strap", "leather", base=(40, 30, 36) if curse else LEATHER, smooth=45, uv_mode="loft", wear=0.7)
    pts = []
    for t in np.linspace(0, 1, 9):
        x = -15.0 + 30.0 * t
        pts.append((x, SH_STRAP_Y, float(_SH_BACK(abs(x))) - 0.3 - 3.4 * math.sin(math.pi * t) ** 0.6))
    ml.sweep(s, pts, ml.rect_ring(6.0, 0.9), caps=(True, True), up=(0, 0, 1))
    g = add(m, "grip_bar", "steel", base=(60, 54, 66) if curse else STEEL, smooth=50, wear=0.6)
    ml.lathe(g, "x", (0, 6.0, -4.9), [(-6.4, 0.9), (-5.8, 1.45), (5.8, 1.45), (6.4, 0.9)], sides=8)
    for sgn in (-1, 1):                                                  # the brackets that carry the bar to the plate
        ml.box(g, (sgn * 5.6 - 0.9, 3.9, -4.9), (sgn * 5.6 + 0.9, 8.1, -0.9), chamfer=0.35)
    return s, g


def paint_shield(m, atlas, curse):
    """The skin of the shield: spun steel with engraved lines and a compass of glowing diamonds, the gem's polish, the painted flutes of the boss."""
    if curse:
        return paint_shield_curse(m, atlas)
    gem_rgb, gem_hi = GEM["shield"], (150, 255, 190)
    paint_metal(atlas, m, ["rim", "ribs", "field_ring", "gem_setting"], seed=100)

    def plate(c):
        if c.kind == "dom" and c.g.key == (2, -1):                                       # the back: darker spun steel, a faint border line
            rho, _ = polar(c)
            rng = np.random.default_rng(12)
            rings = np.interp(rho, np.linspace(0, 40, 520), rng.random(520))
            shade = 1 + 0.06 * (rings - 0.5) + 0.12 * (noise(c, 30, 6) - 0.5)
            c.img[..., :3] = np.clip(np.array((124, 132, 148), np.float32)[None, None, :] * shade[..., None], 0, 255)
            c.img[..., 3] = 255
            tone(c.img, smooth01(rho, 25.0, SH_EDGE_R), 0.75)
            groove(c, band(rho, 24.0, 0.25), 0.4)
            return
        if c.kind != "dom" or c.g.key != (2, 1):
            return
        rho, th = polar(c)
        rng = np.random.default_rng(11)
        rings = np.interp(rho, np.linspace(0, 40, 520), rng.random(520))              # concentric spinning marks, 0.08 cm apart
        base = np.array((178, 186, 200), np.float32)                                  # a brighter steel than the default: this is the polished face
        shade = 1 + 0.05 * (rings - 0.5) + 0.12 * (noise(c, 36, 5) - 0.5) + 0.04 * (noise(c, 3, 8) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        tone(c.img, smooth01(rho, 24.0, SH_EDGE_R), 0.7)                                  # the shadow under the rim
        tone(c.img, band(rho, SH_BOSS_R + 0.9, 1.7), 0.78)                                # and round the boss
        tone(c.img, band(rho, 19.5, 5.5) * 0.5, 1.1)                                      # the sheen of the dish
        groove(c, band(rho, 26.3, 0.3), 0.5)                                             # the double border line
        groove(c, band(rho, 25.2, 0.24), 0.42)
        groove(c, band(rho, SH_TICK_R, 0.26), 0.48)                                      # the compass ring round the boss
        groove(c, band(rho, SH_TICK_R - 1.7, 0.22), 0.4)
        ang = np.abs(((th / (2 * math.pi)) * 24.0) % 1.0 - 0.5)                           # 24 wide ticks between the two rings
        tick = np.clip(1 - ang / (0.2 * SH_TICK_R / np.maximum(rho, 1)), 0, 1) * (np.abs(rho - (SH_TICK_R - 0.85)) < 0.55)
        groove(c, tick, 0.5)
        dia = [rhombus(SH_TICK_R * math.cos(a), SH_TICK_R * math.sin(a), 1.3, 0.75, a) for a in [math.pi / 4 + k * math.pi / 2 for k in range(4)]]
        dm = poly_mask(c, dia)
        blend(c.img, (30, 60, 52), dm, 0.9)                                              # four diamonds of dark enamel set in the ring ...
        glow_into(c, gem_rgb, np.clip(dm * 1.0, 0, 1), 0.9)                              # ... lit from within in the gem's green
        glow_into(c, gem_rgb, band(rho, SH_BOSS_R + 1.6, 0.14), 0.4)                     # a thin inlay line round the boss

    def rim(c):
        if c.kind != "loft":
            return
        groove(c, band(c.A, 3.91, 0.16) * (((c.B % 2.6) < 1.7).astype(np.float32)), 0.45)   # engraved dashes along the top flat of the band
        tone(c.img, band(c.A, 3.91, 1.3), 1.08)

    def boss(c):
        t = np.clip(c.T, 0, 1)
        base = np.array((126, 134, 152), np.float32)
        shade = 0.78 + 0.5 * np.clip(t, 0, 1) ** 1.2 + 0.05 * (noise(c, 4, 2) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        turn, rad = lathe_theta(c, SH_BOSS_PROFILE)
        flute = 0.5 + 0.5 * np.cos(turn * 16 * math.pi)                                   # eight soft flutes (a thin engraved line would stair-step on the skin)
        tone(c.img, flute * smooth01(t, 0.05, 0.35), 0.82)
        tone(c.img, band(t, 0.78, 0.22), 1.15)

    def gem(c):
        t = np.clip(c.T, 0, 1)
        hl = np.exp(-(((c.S - 0.28) / 0.07) ** 2) - (((c.T - 0.7) / 0.14) ** 2))
        to_gem(c, gem_rgb, gem_hi, 0.12 + 0.88 * t ** 0.85, spec=np.clip(hl * 0.8, 0, 1), core=0.22)

    paint_group(atlas, m.part("plate"), plate)
    paint_group(atlas, m.part("rim"), rim)
    paint_group(atlas, m.part("boss"), boss)
    paint_group(atlas, m.part("gem"), gem)


def crack_lines(seed, n_main, r0, r1, wobble, step=1.7):
    """Radial cracks as polylines in model (x, y): from r0 outward, evenly spread in angle, each a polar walk whose angle drifts smoothly (a persistent turn),
    with two branches at about 40 % and 70 % of its length (the same cracks are painted on the plate and the rim, so every part is cracked in step)."""
    rng = np.random.default_rng(seed)
    lines = []
    for k in range(n_main):
        a = 2 * math.pi * k / n_main + float(rng.uniform(-0.12, 0.12))
        r = r0 + float(rng.uniform(0, 1.5))
        pts = [(r * math.cos(a), r * math.sin(a))]
        steps = int(float(rng.uniform(0.7, 1.0)) * (r1 - r0) / step)
        turn = 0.0
        marks = {int(steps * 0.4): 1.0, int(steps * 0.7): -1.0}
        for st in range(steps):
            turn = 0.7 * turn + float(rng.uniform(-wobble, wobble))
            a += turn
            r += step
            pts.append((r * math.cos(a), r * math.sin(a)))
            if st in marks:
                b, rb, br, bt = a + marks[st] * float(rng.uniform(0.5, 0.8)), r, [(r * math.cos(a), r * math.sin(a))], 0.0
                for _ in range(int(rng.integers(3, 6))):
                    bt = 0.6 * bt + float(rng.uniform(-wobble, wobble))
                    b += bt
                    rb += step * 0.85
                    br.append((rb * math.cos(b), rb * math.sin(b)))
                lines.append(br)
        lines.append(pts)
    return lines


def contour_cracks(c, sigma, seed, level=0.5, width=1.1):
    """Thin winding cracks as the contour lines of smooth Gaussian noise (a web, like cooled lava): a 0..1 mask whose lines are `width` px wide wherever the noise
    has a slope (a band of the noise value alone would swell into blobs where the field is flat)."""
    n = gnoise(c, sigma, seed)
    gy, gx = np.gradient(n)
    d = np.abs(n - level) / (np.hypot(gx, gy) + 1e-4)
    return np.clip(1 - d / width, 0, 1) ** 1.2


def paint_shield_curse(m, atlas):
    """The Shield of Wrath: black iron, burning red cracks (radial ones painted in step on the plate, the boss and the rim, a fine web between them), a red gem,
    spikes that glow at their roots."""
    red, hot = SIN["shield"], (255, 190, 90)
    cracks = crack_lines(31, 9, SH_BOSS_R + 0.6, SH_EDGE_R - 1.0, 0.1)
    paint_metal(atlas, m, ["ribs", "field_ring", "gem_setting"], sheen=0.2, grain=0.08, seed=110)

    def plate(c):
        if c.kind == "dom" and c.g.key == (2, -1):
            rho, _ = polar(c)
            c.img[..., :3] = np.clip(np.array((44, 38, 52), np.float32)[None, None, :] * (1 + 0.2 * (noise(c, 12, 6) - 0.5))[..., None], 0, 255)
            c.img[..., 3] = 255
            tone(c.img, smooth01(rho, 24.0, SH_EDGE_R), 0.7)
            return
        if c.kind != "dom" or c.g.key != (2, 1):
            return
        rho, th = polar(c)
        rng = np.random.default_rng(13)
        rings = np.interp(rho, np.linspace(0, 40, 520), rng.random(520))
        shade = 1 + 0.07 * (rings - 0.5) + 0.22 * (noise(c, 26, 5) - 0.5) + 0.08 * (noise(c, 3, 8) - 0.5)
        c.img[..., :3] = np.clip(np.array((52, 46, 60), np.float32)[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        blend(c.img, (96, 42, 30), np.clip((noise(c, 30, 9) - 0.45) * 2.2, 0, 1), 0.45)         # scorch stains
        tone(c.img, smooth01(rho, 24.0, SH_EDGE_R), 0.7)
        tone(c.img, band(rho, SH_BOSS_R + 0.9, 1.7), 0.7)
        groove(c, band(rho, 26.3, 0.3), 0.55)
        groove(c, band(rho, SH_TICK_R, 0.26), 0.5)
        main = stroke_mask(c, cracks, 0.5)
        web = contour_cracks(c, 5, 21, 0.5, 1.2) * smooth01(rho, 21.5, 23.0) * (1 - smooth01(rho, 25.0, 25.9)) * (gnoise(c, 14, 77) > 0.5)
        wide = stroke_mask(c, cracks, 1.6, soft=1.2)
        tone(c.img, wide, 0.55)                                                       # charred round the cracks
        core = np.clip(main * 1.4, 0, 1)
        blend(c.img, red, core, 0.85)
        blend(c.img, hot, np.clip(main * main * 1.6, 0, 1), 0.9)
        blend(c.img, red, web, 0.7)
        glow_into(c, red, np.maximum(core, web * 0.6), 1.0, core=0.4)
        dia = [rhombus(SH_TICK_R * math.cos(a), SH_TICK_R * math.sin(a), 1.6, 0.9, a) for a in [math.pi / 4 + k * math.pi / 2 for k in range(4)]]
        dm = poly_mask(c, dia)
        blend(c.img, (60, 14, 10), dm, 0.9)
        glow_into(c, red, dm, 0.95)

    def rim(c):
        if c.kind != "loft":
            return
        c.img[..., :3] = np.clip(np.array((58, 50, 62), np.float32)[None, None, :] * (1 + 0.25 * (noise(c, 6, 4) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        web = contour_cracks(c, 4, 33, 0.52, 1.2) * (noise(c, 30, 78) > 0.55)
        blend(c.img, red, web, 0.8)
        glow_into(c, red, web, 0.9, core=0.3)
        tone(c.img, band(c.A, 3.91, 1.3), 1.15)

    def boss(c):
        t = np.clip(c.T, 0, 1)
        shade = 0.75 + 0.45 * t ** 1.2 + 0.18 * (noise(c, 5, 2) - 0.5)
        c.img[..., :3] = np.clip(np.array((60, 52, 70), np.float32)[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        web = contour_cracks(c, 4, 41, 0.5, 1.2) * smooth01(t, 0.1, 0.4) * (noise(c, 16, 79) > 0.5)
        blend(c.img, red, web, 0.8)
        glow_into(c, red, web, 0.9, core=0.3)

    def gem(c):
        t = np.abs(np.clip(c.T, 0, 1))
        hl = np.exp(-(((c.S - 0.28) / 0.07) ** 2) - (((c.T - 0.7) / 0.14) ** 2))
        to_gem(c, red, (255, 170, 120), 0.12 + 0.88 * t ** 0.85, spec=np.clip(hl * 0.7, 0, 1), core=0.3)

    def spikes(c):
        t = np.clip(c.T, 0, 1)
        shade = 0.8 + 0.3 * (noise(c, 4, 3) - 0.5)
        c.img[..., :3] = np.clip(np.array((42, 36, 48), np.float32)[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        hotroot = np.clip(1 - t * 2.2, 0, 1) ** 1.3
        blend(c.img, red, hotroot, 0.7)
        glow_into(c, red, hotroot * 0.8, 0.9, core=0.25)

    paint_group(atlas, m.part("plate"), plate)
    paint_group(atlas, m.part("rim"), rim)
    paint_group(atlas, m.part("boss"), boss)
    paint_group(atlas, m.part("gem"), gem)
    paint_group(atlas, m.part("horns"), spikes)


def build_shield(curse=False, write=True):
    iid = "legendary_shield_curse" if curse else "legendary_shield"
    base = "legendary_shield"
    if write and not curse:
        write_icon(base)
    m = ml.Mesh(iid)
    shield_plate(m, curse)
    shield_rim(m, curse)
    shield_ribs(m, curse)
    shield_studs(m, curse)
    shield_boss(m, curse)
    shield_gem(m, curse)
    shield_back(m, curse)
    if curse:
        shield_wrath(m)
    atlas = lay(m)
    paint_shield(m, atlas, curse)
    if not write:
        return m, atlas
    if not curse:
        _BASE_PLACE[base] = upright_place(m, base)
    place = base_place(base, build_shield, lambda mm: upright_place(mm, base))
    st = write_look(iid, m, atlas, place, ("FFFFFF", 0.10, 0.55, 1.0))
    if not curse:
        write_item_models(base)
    return st


# ================================================================ 2. THE LEGENDARY SWORD (Ren Amaki; Greed and Gluttony)
# Target (wiki summaries: a blue gem; the rest from memory of the anime and design): a straight, slender double-edged hero's sword, a gold winged crossguard with
# the blue gem at its centre, a blue-wrapped grip and a round gold pommel. Real size: a 150 cm bastard sword, scaled to 1.70 blocks apparent (class longsword).
SW_LEN = 150.0                 # pommel to tip
SW_POMMEL_TOP = 7.2            # the pommel's height
SW_GRIP = (6.6, 27.0)          # the grip's y range: 20 cm, a two-hand hold
SW_GUARD_Y = 29.6              # the crossguard's centre and the gem's
SW_BLADE_Y0 = 32.0             # where the blade starts (the ricasso collar covers its first centimetres)
SW_HOLD = 0.115                # where the hand holds, as a share of the length (the middle of the grip), for the placement on the sprite
SW_GEM_R, SW_GEM_D = 3.6, 3.9  # the lens-shaped cabochon through the guard: radius and half thickness (it stands out of both faces)
SW_GUARD_T = 4.4               # the guard core's thickness (z)
SW_GEM_PROFILE = [(-SW_GEM_D, 0.0), (-SW_GEM_D * 0.86, SW_GEM_R * 0.52), (-SW_GEM_D * 0.55, SW_GEM_R * 0.86), (-SW_GEM_D * 0.25, SW_GEM_R * 0.98), (0.0, SW_GEM_R),
                  (SW_GEM_D * 0.25, SW_GEM_R * 0.98), (SW_GEM_D * 0.55, SW_GEM_R * 0.86), (SW_GEM_D * 0.86, SW_GEM_R * 0.52), (SW_GEM_D, 0.0)]   # (z, radius) of the lens

_SW_W = w1.hermite([(32, 6.8), (60, 6.7), (90, 6.4), (110, 5.9), (126, 5.0), (138, 3.7), (146, 1.9), (150, 0.0)])     # blade width: a long leaf taper
_SW_T = w1.hermite([(32, 1.7), (80, 1.5), (120, 1.1), (150, 0.25)])                                                    # blade thickness


SW_TOOTH = 6.4                 # the curse blade's tooth pitch (cm), its tooth height and the span the teeth run over
SW_TOOTH_H = 2.0
SW_TEETH_Y = (50.0, 140.0)


def sword_width(y, curse):
    """The full width of the blade at height y: the leaf envelope, and for the curse look a row of backward-hooked teeth on both edges (each tooth
    rises abruptly at its tip and slopes back to the next valley, like a shark's, so the blade catches what it cuts)."""
    w = max(float(_SW_W(y)), 0.0)
    if curse and SW_TEETH_Y[0] <= y < SW_TEETH_Y[1]:
        t = ((y - SW_TEETH_Y[0]) / SW_TOOTH) % 1.0
        fade = min(1.0, (y - SW_TEETH_Y[0]) / 8.0, (SW_TEETH_Y[1] - y) / 14.0)
        w += 2 * SW_TOOTH_H * fade * (1.0 - t / 0.94 if t < 0.94 else 0.0)
    return w


def sword_stations(curse):
    ys = [SW_BLADE_Y0, SW_BLADE_Y0 + 2.0, 46.0]
    if curse:
        y = SW_TEETH_Y[0]
        while y + SW_TOOTH <= SW_TEETH_Y[1]:
            ys += [y, y + 0.55 * SW_TOOTH, y + 0.94 * SW_TOOTH - 0.01]
            y += SW_TOOTH
        ys += [y, y + 0.5 * (SW_TEETH_Y[1] - y)]
        ys = [v for v in ys if v < 138.0]
        ys += [138.0, 143.0, 147.0, 149.4, 150.0]
    else:
        ys += [60.0, 75.0, 90.0, 105.0, 118.0, 128.0, 136.0, 142.0, 146.5, 149.3, 150.0]
    return sorted(set(ys))


def sword_blade(m, curse):
    """A slender hexagonal blade (flat faces, ground edges) with a long leaf-shaped point. Its faces are painted: the fuller and the runes are texture. The curse
    look has a saw of hooked teeth along both edges."""
    b = add(m, "blade", "iron" if curse else "steel", base=(34, 30, 42) if curse else STEEL_BRIGHT, smooth=18, wear=0.5)
    st = []
    for y in sword_stations(curse):
        w = sword_width(y, curse)
        t = max(float(_SW_T(y)), 0.0)
        st.append((y, w, t) if y < 150.0 else (y, 0.0, 0.0))
    ml.loft(b, "y", (0, 0, 0), st, ml.bevelled(1.0, 1.0, 0.42), caps=(True, True))
    return b


def sword_guard(m, curse):
    """The crossguard: a core block and, each side, a long wing curving up toward the blade and a short feather curving down toward the grip. The curse look turns
    the wings into two hooked mandibles with teeth on their inner edge, and a row of teeth clamps the blade's root: the sword is a maw."""
    g = add(m, "guard", "iron" if curse else "gold", base=(46, 38, 52) if curse else WHITE_GOLD, smooth=40, wear=0.5)
    ml.loft(g, "y", (0, 0, 0), [(26.6, 0.75, 0.75), (27.5, 1.0, 1.0), (31.4, 1.0, 1.0), (33.0, 0.8, 0.8)], ml.rounded_rect(10.0, SW_GUARD_T, 1.2, 2), caps=(True, True))
    if curse:
        for sgn in (-1, 1):
            p0, p1, p2, p3 = (sgn * 4.0, SW_GUARD_Y + 0.6, 0.0), (sgn * 17.0, SW_GUARD_Y - 1.0, 0.0), (sgn * 21.0, SW_GUARD_Y + 12.0, 0.0), (sgn * 15.0, SW_GUARD_Y + 17.0, 0.0)
            pts = bezier(p0, p1, p2, 10, p3)
            ml.sweep(g, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (0.0, 0.0) if t > 0.985 else (6.0 * (1 - t) ** 0.9 + 0.2, 3.4 * (1 - 0.7 * t)),
                     caps=(True, False), up=(0, 0, 1))
            for tt, ln in ((0.40, 4.4), (0.58, 4.0), (0.76, 3.4)):                 # the teeth on the mandible's inner edge
                q = V(bezier(p0, p1, p2, 40, p3)[int(tt * 40)])
                cone(g, tuple(q + V((-sgn * 1.4, 0.4, 0.0))), (-sgn * 0.8, 0.6, 0.0), ln, 1.25, sides=4)
            p0, p1, p2 = (sgn * 4.0, SW_GUARD_Y - 1.4, 0.0), (sgn * 11.0, SW_GUARD_Y - 3.0, 0.0), (sgn * 15.5, SW_GUARD_Y - 10.0, 0.0)           # the lower tusks
            ml.sweep(g, bezier(p0, p1, p2, 6), ml.lozenge(1.0, 1.0), scales=lambda t: (0.0, 0.0) if t > 0.985 else (4.0 * (1 - t) ** 0.9 + 0.2, 2.8 * (1 - 0.6 * t)),
                     caps=(True, False), up=(0, 0, 1))
        for i in range(5):                                                         # the lower jaw: teeth clamped over the blade's root, on both faces
            for zs in (-1, 1):
                x = -3.0 + 1.5 * i
                cone(g, (x, 32.4, zs * (SW_GUARD_T / 2 - 0.3)), (0.0, 0.93, zs * 0.18), 4.8 if i % 2 == 0 else 3.6, 0.8, sides=4)
        return g
    for sgn in (-1, 1):
        for (x0, x1, y0, rise, pw, w0, t0) in ((3.8, 19.0, SW_GUARD_Y + 0.4, 7.4, 1.7, 4.8, 2.8), (3.8, 13.6, SW_GUARD_Y - 1.2, -5.8, 1.5, 3.5, 2.3)):
            pts = [(sgn * (x0 + (x1 - x0) * t), y0 + rise * t ** pw, 0.0) for t in np.linspace(0, 1, 8)]
            ml.sweep(g, pts, ml.lozenge(1.0, 1.0), scales=lambda t, w0=w0, t0=t0: (0.0, 0.0) if t > 0.985 else (w0 * (1 - 0.9 * t ** 0.9), t0 * (1 - 0.8 * t)),
                     caps=(True, False), up=(0, 0, 1))
    return g


def sword_gem(m, curse):
    """The lens-shaped cabochon through the guard (it shows on both faces) and its two round bezels."""
    bez = add(m, "gem_setting", "iron" if curse else "gold", base=(60, 50, 64) if curse else WHITE_GOLD, smooth=50, wear=0.4)
    for sgn in (-1, 1):
        band_loop(bez, SW_GEM_R + 0.35, sgn * (SW_GUARD_T / 2 - 0.1), ml.circle(0.75, 6), n=18, arcs=1, cx=0.0, cy=SW_GUARD_Y)
    g = add(m, "gem", "crystal", base=SIN["sword"] if curse else GEM["sword"], glow=True, smooth=60, uv_mode=None if curse else "loft")   # the eye is mapped flat: a straight pupil
    ml.lathe(g, "z", (0, SW_GUARD_Y, 0), SW_GEM_PROFILE, sides=18, caps=(False, False))
    return bez, g


def sword_hilt(m, curse):
    """Pommel, wrapped grip, its three gold rings and the collar where the blade leaves the guard."""
    pm = add(m, "pommel", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=45, wear=0.5, uv_mode="loft")
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 0.0), (0.4, 1.4), (1.2, 2.5), (2.4, 3.2), (3.6, 3.45), (4.8, 3.1), (5.9, 2.3), (6.6, 1.7), (SW_POMMEL_TOP, 1.45)], sides=16, caps=(False, True))
    if curse:                                                            # six spikes radiate from the pommel: a hungry mace-head
        for k in range(6):
            a = 2 * math.pi * k / 6
            cone(pm, (3.1 * math.cos(a), 3.6, 3.1 * math.sin(a)), (math.cos(a), -0.15, math.sin(a)), 4.4, 1.0, sides=4)
    gr = add(m, "grip", "leather", base=(24, 22, 30) if curse else (40, 58, 112), smooth=50, wear=0.6, uv_mode="loft")
    ml.lathe(gr, "y", (0, 0, 0), [(SW_GRIP[0], 1.5), (8.0, 1.72), (16.0, 1.9), (25.0, 1.72), (SW_GRIP[1], 1.5)], sides=10, caps=(False, False))
    rg = add(m, "grip_rings", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=50, wear=0.4)
    for y in (SW_GRIP[0] + 0.3, 16.4, SW_GRIP[1] - 1.4):
        ml.collar(rg, y, 2.0, 1.1, lip=0.3, sides=12, axis="y")
    bc = add(m, "blade_collar", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=40, wear=0.5)
    ml.loft(bc, "y", (0, 0, 0), [(31.2, 0.9, 0.9), (32.0, 1.0, 1.0), (35.6, 1.0, 1.0), (36.6, 0.72, 0.72)], ml.rounded_rect(8.0, 3.1, 0.9, 2), caps=(False, True))
    return pm, gr, rg, bc


def paint_sword(m, atlas, curse):
    """The skin of the sword: a polished blade with a long engraved fuller whose core glows blue, runes near the guard, the grip's wrap, the lens gem."""
    if curse:
        return paint_sword_curse(m, atlas)
    gem_rgb, gem_hi = GEM["sword"], (170, 214, 255)
    paint_metal(atlas, m, ["pommel", "grip_rings", "blade_collar", "guard", "gem_setting"], seed=200)

    def blade(c):
        if c.kind != "dom":
            return
        ax, sg = c.g.key
        if ax == 2:                                                                     # the two flat faces: a: x (or -x), b: y
            x, y = c.A, c.B
            hw = np.maximum(np.interp(y, [32, 150], [3.4, 0.0]), 0.2)
            base = np.array((200, 208, 222), np.float32)
            shade = 1 + 0.07 * (noise(c, 22, 4 + sg) - 0.5) + 0.04 * (noise(c, 2.5, 7) - 0.5)
            c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
            c.img[..., 3] = 255
            across = np.abs(x) / np.maximum(hw, 0.2)
            tone(c.img, smooth01(across, 0.7, 1.0), 1.12)                                # the ground edge catches the light
            tone(c.img, band(across, 0.0, 0.5) * 0.4, 0.9)
            fl = np.interp(y, [34, 44, 112, 134], [0.0, 1.0, 1.0, 0.0])                   # the fuller: engraved twin lines round a glowing core
            groove(c, (band(np.abs(x), 0.95, 0.2) * fl) * (y > 33), 0.55)
            core = band(x, 0.0, 0.34) * np.clip(fl, 0, 1) * smooth01(y, 36, 44) * (1 - smooth01(y, 110, 132))
            blend(c.img, (60, 110, 190), core, 0.8)
            glow_into(c, gem_rgb, core, 0.9)
        else:                                                                           # the ground bevels
            c.img[..., :3] = np.clip(np.array((226, 232, 242), np.float32)[None, None, :] * (1 + 0.05 * (noise(c, 12, 3) - 0.5))[..., None], 0, 255)
            c.img[..., 3] = 255

    def grip(c):
        ph = ((c.B + c.A * 0.55) / 3.1) % 1.0                                             # the wrap: diagonal bands with a stitched seam between them
        base = np.array((40, 58, 112), np.float32)
        shade = 0.82 + 0.28 * np.sin(ph * math.pi) ** 1.5 + 0.08 * (noise(c, 3, 9) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        groove(c, band(ph, 0.0, 0.07) + band(ph, 1.0, 0.07), 0.55)

    def gem(c):
        t = np.abs(2 * np.clip(c.T, 0, 1) - 1)                                           # the crown of each face is bright, the girdle dark
        hl = np.exp(-(((c.S - 0.3) / 0.07) ** 2) - (((t - 0.62) / 0.15) ** 2))
        to_gem(c, gem_rgb, gem_hi, 0.12 + 0.88 * t ** 0.85, spec=np.clip(hl * 0.8, 0, 1), core=0.22)

    paint_group(atlas, m.part("blade"), blade)
    paint_group(atlas, m.part("grip"), grip)
    paint_group(atlas, m.part("gem"), gem)


_SW_TAB_Y = np.linspace(32.0, 150.0, 400)


def paint_sword_curse(m, atlas):
    """The Sword of Greed and Gluttony: a black serrated blade whose fuller is a maw (two rows of pale teeth round a glowing amber throat), amber veins, a
    black grip wound with gold wire, an amber eye with a slit pupil."""
    amber, hot = SIN["sword"], (255, 236, 150)
    tab_w = np.array([sword_width(float(v), True) for v in _SW_TAB_Y])
    paint_metal(atlas, m, ["pommel", "grip_rings", "blade_collar", "guard", "gem_setting"], sheen=0.2, grain=0.08, seed=210)

    def blade(c):
        if c.kind != "dom":
            return
        ax, sg = c.g.key
        if ax != 2:                                                                     # the flanks of the teeth (a cutting edge): dull gold, like the gilded edges
            c.img[..., :3] = np.clip(np.array((150, 114, 42), np.float32)[None, None, :] * (1 + 0.2 * (noise(c, 6, 3) - 0.5))[..., None], 0, 255)
            c.img[..., 3] = 255
            return
        x, y = c.A, c.B
        hw = np.maximum(np.interp(y, _SW_TAB_Y, tab_w) / 2, 0.2)
        c.img[..., :3] = np.clip(np.array((40, 35, 48), np.float32)[None, None, :] * (1 + 0.3 * (noise(c, 18, 4 + sg) - 0.5) + 0.08 * (noise(c, 2.5, 7) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        across = np.abs(x) / hw
        gilt = smooth01(across, 0.80, 0.90)                                              # the ground edges are gilded (Greed): a gold line each side that follows the teeth
        blend(c.img, (222, 172, 52), gilt, 0.9)
        glow_into(c, (255, 190, 60), gilt, 0.4, core=0.2)
        mw = np.interp(y, [40, 60, 120, 138], [0.0, 1.25, 0.8, 0.0])                    # the maw: half width of the mouth along the blade
        mouth = (np.abs(x) < mw) * smooth01(y, 38, 44) * (1 - smooth01(y, 132, 138))
        ph = ((y + np.where(x > 0, 0.0, 1.3)) / 2.6) % 1.0
        tri = np.abs(ph - 0.5) * 2                                                       # 0 at a tooth's tip, 1 between teeth
        x_in = 0.12 + 0.62 * mw * tri
        throat = mouth * (np.abs(x) < x_in)
        teeth = mouth * (np.abs(x) >= x_in)
        blend(c.img, (232, 220, 190), teeth, 0.95)                                       # pale teeth
        blend(c.img, amber, throat, 0.95)
        blend(c.img, hot, throat * (np.abs(x) < x_in * 0.45), 0.9)
        glow_into(c, amber, throat, 1.0, core=0.35)
        veins = contour_cracks(c, 6, 51 + sg, 0.5, 1.1) * (across < 0.8) * (noise(c, 30, 79 + sg) > 0.55) * (1 - mouth)
        blend(c.img, amber, veins, 0.8)
        glow_into(c, amber, veins, 0.8, core=0.25)

    def grip(c):
        ph = ((c.B + c.A * 0.55) / 3.1) % 1.0
        shade = 0.85 + 0.25 * np.sin(ph * math.pi) ** 1.5 + 0.08 * (noise(c, 3, 9) - 0.5)
        c.img[..., :3] = np.clip(np.array((26, 22, 30), np.float32)[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        blend(c.img, (214, 170, 60), band(ph, 0.0, 0.06) + band(ph, 1.0, 0.06), 0.9)   # gold wire between the turns

    def gem(c):
        """The eye: an amber stone with a dark upright slit for a pupil, painted straight in x / y on the two crowns (the girdle is a thin dark band)."""
        if c.kind != "dom" or c.g.key[0] != 2:
            c.img[..., :3] = np.array((120, 70, 12), np.float32)
            c.img[..., 3] = 255
            glow_into(c, amber, np.full(c.img.shape[:2], 0.25, np.float32), 0.5, core=0.1)
            return
        x, y = c.A, c.B - SW_GUARD_Y
        rho = np.hypot(x, y) / SW_GEM_R
        t = np.clip(1 - rho ** 1.5, 0, 1)
        hl = np.exp(-(((x + 0.38 * SW_GEM_R) / (0.2 * SW_GEM_R)) ** 2) - (((y - 0.4 * SW_GEM_R) / (0.2 * SW_GEM_R)) ** 2))
        to_gem(c, amber, (255, 236, 160), 0.15 + 0.85 * t, spec=np.clip(hl * 0.7, 0, 1), core=0.25)
        half_h = 0.78 * SW_GEM_R                                                        # the slit: a thin lens, upright
        w = 0.17 * SW_GEM_R * np.sqrt(np.clip(1 - (y / half_h) ** 2, 0, 1))
        slit = smooth01(w - np.abs(x), -0.12, 0.12) * (np.abs(y) < half_h)
        c.img[..., :3] *= (1 - 0.92 * slit)[..., None]
        c.glow[..., 3] *= (1 - 0.85 * slit)

    paint_group(atlas, m.part("blade"), blade)
    paint_group(atlas, m.part("grip"), grip)
    paint_group(atlas, m.part("gem"), gem)


def build_sword(curse=False, write=True):
    iid = "legendary_sword_curse" if curse else "legendary_sword"
    base = "legendary_sword"
    if write and not curse:
        write_icon(base)                                                 # the mesh is placed on its sprite, so the sprite comes first
    m = ml.Mesh(iid)
    sword_blade(m, curse)
    sword_guard(m, curse)
    sword_gem(m, curse)
    sword_hilt(m, curse)
    atlas = lay(m)
    paint_sword(m, atlas, curse)
    if not write:
        return m, atlas
    if not curse:
        _BASE_PLACE[base] = diag_place(m, base, SW_HOLD)
    place = base_place(base, build_sword, lambda mm: diag_place(mm, base, SW_HOLD))
    st = write_look(iid, m, atlas, place, ("FFFFFF", 0.12, 0.5, 1.0))
    if not curse:
        write_item_models(base)
    return st


# ================================================================ 3. THE LEGENDARY SPEAR (Motoyasu Kitamura; Lust)
# Target (wiki summaries: a red gem; the shape is from memory of the anime and design): a long spear, a silver shaft with gold bands and a wrapped hold, a gold
# socket that cages the red gem between two swept wings, and a leaf-shaped head with a strong midrib. The twelve ticks engraved round the socket nod to the
# spear's time-reversal lore (its best-known form is the Dragon Minute Hand Spear). Real size: 3.3 m, 3.30 blocks apparent (class spear 2.80 - 3.60).
SP_LEN = 330.0                 # butt to tip
SP_SHAFT = (14.0, 244.0)       # the silver shaft
SP_HOLD = 0.335                # the hand's place, a share of the length (class table: spear grip 0.33), the middle of the wrapped hold
SP_WRAP = (98.0, 124.0)        # the wrapped hold (about the share 0.30 - 0.38)
SP_SOCKET = (240.0, 262.0)     # the gold socket the head sits in
SP_GEM_Y, SP_GEM_R = 268.0, 3.7   # the red gem: an orb in a cage, between the socket and the blade
SP_BLADE = (277.0, 330.0)      # the leaf-shaped head
SP_BANDS = (30.0, 62.0, 92.0, 132.0, 162.0, 192.0, 222.0)   # the gold bands along the shaft
SP_SOCKET_PROFILE = [(SP_SOCKET[0], 1.75), (SP_SOCKET[0] + 2.0, 2.45), (SP_SOCKET[0] + 6.0, 2.35), (SP_SOCKET[0] + 14.0, 2.05), (SP_SOCKET[1] - 1.5, 2.55), (SP_SOCKET[1], 1.9)]

_SP_W = w1.hermite([(277, 3.4), (282, 8.0), (290, 11.6), (299, 12.2), (310, 10.4), (319, 6.8), (325, 3.0), (330, 0.0)])    # head width: a bay-leaf outline
_SP_T = w1.hermite([(277, 2.2), (290, 2.8), (306, 2.4), (320, 1.7), (330, 0.0)])                                            # head thickness through the midrib
_SP_TAB_Y = np.linspace(SP_BLADE[0], SP_BLADE[1], 60)
_SP_TAB_W = np.array([max(float(_SP_W(v)), 0.0) for v in _SP_TAB_Y])


def spear_shaft(m, curse):
    """The silver shaft, the butt spike, the wrapped hold and the gold bands."""
    bc = add(m, "butt_cap", "iron" if curse else "steel", base=(60, 54, 70) if curse else STEEL_BRIGHT, smooth=45, wear=0.5, uv_mode="loft")
    ml.lathe(bc, "y", (0, 0, 0), [(0.0, 0.0), (2.5, 0.9), (7.0, 1.75), (10.5, 2.05), (12.5, 1.9), (SP_SHAFT[0] + 1.0, 1.6)], sides=10, caps=(False, False))
    sh = add(m, "shaft", "iron" if curse else "silver", base=(40, 36, 50) if curse else (206, 210, 222), smooth=50, wear=0.5, uv_mode="loft")
    ys = [SP_SHAFT[0], 72.0, 130.0, 188.0, SP_SHAFT[1] + 1.0]                      # four pieces end to end: one 230 cm strip would cap the whole skin at 2 px / cm
    rs = [1.55, 1.64, 1.66, 1.62, 1.5]
    for k in range(4):
        ml.lathe(sh, "y", (0, 0, 0), [(ys[k], rs[k]), (ys[k + 1], rs[k + 1])], sides=10, caps=(False, False))
    gw = add(m, "grip_wrap", "leather", base=(28, 22, 34) if curse else (124, 30, 44), smooth=55, wear=0.6, uv_mode="loft")
    ml.lathe(gw, "y", (0, 0, 0), [(SP_WRAP[0], 1.72), (SP_WRAP[0] + 2.0, 1.86), (SP_WRAP[1] - 2.0, 1.86), (SP_WRAP[1], 1.72)], sides=10, caps=(False, False))
    bd = add(m, "bands", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=50, wear=0.4, uv_mode="loft")
    for y in SP_BANDS + (SP_WRAP[0] - 1.6, SP_WRAP[1] + 0.4):
        ml.collar(bd, y, 1.95, 1.2, lip=0.3, sides=10, axis="y", caps=(False, False))
    return bc, sh, gw, bd


def spear_socket(m, curse):
    """The gold socket (engraved with the twelve ticks), the cage of claws round the orb, and the two swept wings below the head."""
    so = add(m, "socket", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=45, wear=0.4, uv_mode="loft")
    ml.lathe(so, "y", (0, 0, 0), SP_SOCKET_PROFILE, sides=12, caps=(False, False))
    cl = add(m, "claws", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=40, wear=0.4)
    for k in range(4):                                                   # four curved prongs round the orb, meeting above it
        a = k * math.pi / 2 + math.pi / 4
        ca, sa = math.cos(a), math.sin(a)
        pts = []
        for t in np.linspace(0, 1, 7):
            rr = 2.3 + 2.4 * math.sin(math.pi * min(1.0, t * 1.0)) ** 0.8 * (1 - 0.55 * t)
            pts.append((rr * ca, SP_SOCKET[1] - 1.0 + (SP_BLADE[0] - SP_SOCKET[1] + 3.0) * t, rr * sa))
        ml.sweep(cl, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (1.5 * (1 - 0.55 * t), 0.7), caps=(True, True), up=(0, 1, 0))
    wg = add(m, "wings", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=40, wear=0.5)
    for sgn in (-1, 1):                                                  # the swept wings: flat blades curving down and out from the head's collar
        pts = [(sgn * (2.2 + 8.6 * t), SP_BLADE[0] - 0.5 - 13.0 * t ** 1.6, 0.0) for t in np.linspace(0, 1, 8)]
        ml.sweep(wg, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (0.0, 0.0) if t > 0.985 else (2.6 * (1 - 0.88 * t), 1.5 * (1 - 0.6 * t)), caps=(True, False), up=(0, 0, 1))
    return so, cl, wg


def spear_gem(m, curse):
    g = add(m, "gem", "crystal", base=SIN["spear"] if curse else GEM["spear"], glow=True, smooth=60, uv_mode="loft")
    sphere(g, (0, SP_GEM_Y, 0), SP_GEM_R, sides=14, stacks=9)
    return g


def spear_head(m, curse):
    """The leaf head: a diamond section (a sharp midrib) over a bay-leaf outline, a collar where it meets the cage."""
    hd = add(m, "spearhead", "iron" if curse else "steel", base=(70, 62, 80) if curse else STEEL_BRIGHT, smooth=18, wear=0.5)
    ys = [277.0, 280.0, 285.0, 291.0, 298.0, 305.0, 312.0, 318.0, 323.0, 327.0, 329.2, 330.0]
    st = [(y, max(float(_SP_W(y)), 0.0), max(float(_SP_T(y)), 0.0)) if y < 330.0 else (y, 0.0, 0.0) for y in ys]
    ml.loft(hd, "y", (0, 0, 0), st, ml.lozenge(1.0, 1.0), caps=(True, True))
    hc = add(m, "head_collar", "iron" if curse else "gold", base=(58, 50, 62) if curse else WHITE_GOLD, smooth=45, wear=0.4, uv_mode="loft")
    ml.lathe(hc, "y", (0, 0, 0), [(SP_BLADE[0] - 3.0, 1.7), (SP_BLADE[0] - 2.0, 2.7), (SP_BLADE[0] + 1.0, 2.7), (SP_BLADE[0] + 2.2, 1.9)], sides=12, caps=(False, False))
    return hd, hc


def spear_lust(m):
    """The Spear of Lust's additions: a thorned vine winding up the shaft, backward-hooked barbs along the head's edges and two short chains hanging from
    the wing tips. Same shaft, same hold, same head outline: the hand fits."""
    vn = add(m, "vine", "cloth", base=(70, 18, 70), smooth=50, uv_mode="loft", wear=0.4)
    y0, y1, turns, n = 176.0, SP_SOCKET[0] - 4.0, 2.75, 36
    pts = w1.helix(lambda t: 2.45 + 0.25 * math.sin(t * 9.0), y0, y1, turns, n, phase=0.4)
    ml.sweep(vn, pts, ml.circle(1.0, 4), scales=lambda t: (0.0, 0.0) if t > 0.99 or t < 0.01 else (0.62 * min(1.0, t * 8.0, (1 - t) * 8.0 + 0.2),) * 2, caps=(True, True), up=(0, 1, 0))
    th = add(m, "thorns", "iron", base=(30, 22, 36), smooth=45, uv_mode="loft", wear=0.5)
    for k in range(11):
        t = 0.06 + 0.9 * k / 10
        a = 0.4 + 2 * math.pi * turns * t
        r = 2.45 + 0.25 * math.sin(t * 9.0)
        base = (r * math.cos(a), y0 + (y1 - y0) * t, r * math.sin(a))
        cone(th, base, (math.cos(a), -0.35, math.sin(a)), 3.8 - 1.2 * t, 0.85, sides=4)
    for sgn in (-1, 1):                                                  # the barbs along the head's edges, hooked back toward the socket
        for yy, ln in ((293.0, 8.0), (303.0, 9.5), (314.0, 7.5)):
            hw = max(float(_SP_W(yy)), 0.0) / 2 * 0.86
            p0 = V((sgn * hw, yy, 0.0))
            p1 = p0 + V((sgn * ln * 0.62, ln * 0.05, 0.0))
            p2 = p0 + V((sgn * ln * 0.78, -ln * 0.55, 0.0))
            horn(th, tuple(p0), tuple(p1), tuple(p2), 1.5, sides=4, n=4, up=(0, 0, 1))
    ch = add(m, "chains", "iron", base=(54, 44, 62), smooth=55, wear=0.7)
    for sgn in (-1, 1):                                                  # two chains of six links each, hung from the wing tips
        for k in range(6):
            c = V((sgn * (10.9 - 0.25 * k), SP_BLADE[0] - 13.8 - 3.4 * k, 0.0))
            link_loop(ch, tuple(c), (0, 1, 0), (0, 0, 1) if k % 2 else (1, 0, 0), 1.75, 1.1, 0.34, sides=4, n=8)
    return vn, th, ch


def paint_spear(m, atlas, curse):
    """The skin of the spear: a mirror-bright head with its engraved fuller lines and a glowing midrib, the red wrap, the ticks on the socket, the orb."""
    if curse:
        return paint_spear_curse(m, atlas)
    gem_rgb, gem_hi = GEM["spear"], (255, 150, 150)
    paint_metal(atlas, m, ["butt_cap", "shaft", "bands", "claws", "wings", "head_collar"], seed=300)

    def head(c):
        if c.kind != "dom":
            return
        ax, sg = c.g.key
        if ax != 2:
            c.img[..., :3] = np.array((228, 234, 244), np.float32)
            c.img[..., 3] = 255
            return
        x, y = c.A, c.B
        hw = np.maximum(np.interp(y, _SP_TAB_Y, _SP_TAB_W) / 2, 0.2)
        base = np.array((206, 214, 228), np.float32)
        shade = 1 + 0.07 * (noise(c, 20, 6 + sg) - 0.5) + 0.04 * (noise(c, 2.5, 5) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        across = np.abs(x) / hw
        tone(c.img, smooth01(across, 0.62, 1.0), 1.14)                                  # the ground edges catch the light
        tone(c.img, band(across, 0.0, 0.34) * 0.5, 0.9)                                  # a shaded band either side of the midrib
        along = (y > SP_BLADE[0] + 8) * (y < SP_BLADE[1] - 8)
        groove(c, band(np.abs(x) - 0.56 * hw, 0.0, 0.22) * along, 0.5)                    # a fuller line each side (an absolute width, so it never breaks up)
        mid = band(x, 0.0, 0.3) * smooth01(y, SP_BLADE[0] + 8, SP_BLADE[0] + 16) * (1 - smooth01(y, SP_BLADE[1] - 14, SP_BLADE[1] - 4))
        blend(c.img, (150, 40, 54), mid, 0.8)
        glow_into(c, gem_rgb, mid, 0.85)                                                # the midrib glows red

    def socket(c):
        turn, _ = lathe_theta(c, SP_SOCKET_PROFILE)
        base = np.array(WHITE_GOLD, np.float32)
        c.img[..., :3] = np.clip(base[None, None, :] * (1 + 0.10 * (noise(c, 3, 4) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        dial = np.clip(band(c.T, 0.62, 0.2) * 1.8, 0, 1)                                 # a recessed dial band round the socket ...
        tone(c.img, dial, 0.72)
        blend(c.img, (255, 238, 176), band((turn * 12.0) % 1.0, 0.5, 0.2) * dial, 0.9)    # ... with twelve pale ticks: the clock of the spear's lore
        groove(c, band(c.T, 0.4, 0.035) + band(c.T, 0.84, 0.035), 0.5)

    def wrap(c):
        ph = ((c.B + c.A * 0.7) / 3.0) % 1.0
        base = np.array((124, 30, 44), np.float32)
        shade = 0.8 + 0.3 * np.sin(ph * math.pi) ** 1.5 + 0.06 * (noise(c, 3, 9) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        groove(c, band(ph, 0.0, 0.07) + band(ph, 1.0, 0.07), 0.55)

    def gem(c):
        t = 0.25 + 0.75 * np.clip(1 - np.abs(2 * c.T - 1) ** 1.3, 0, 1)
        hl = np.exp(-(((c.S - 0.3) / 0.08) ** 2) - (((c.T - 0.68) / 0.13) ** 2))
        to_gem(c, gem_rgb, gem_hi, t, spec=np.clip(hl * 0.8, 0, 1), core=0.15)

    paint_group(atlas, m.part("spearhead"), head)
    paint_group(atlas, m.part("socket"), socket)
    paint_group(atlas, m.part("grip_wrap"), wrap)
    paint_group(atlas, m.part("gem"), gem)


def paint_spear_curse(m, atlas):
    """The Spear of Lust: a black head with a glowing magenta midrib and a web of veins, magenta-lit roots on the thorns, a purple vine, a black shaft with
    magenta veins, a hot orb."""
    pink, hot = SIN["spear"], (255, 190, 235)
    paint_metal(atlas, m, ["butt_cap", "bands", "claws", "wings", "head_collar", "chains"], sheen=0.2, grain=0.08, seed=310)

    def head(c):
        if c.kind != "dom":
            return
        ax, sg = c.g.key
        if ax != 2:
            c.img[..., :3] = np.array((92, 70, 104), np.float32)
            c.img[..., 3] = 255
            return
        x, y = c.A, c.B
        hw = np.maximum(np.interp(y, _SP_TAB_Y, _SP_TAB_W) / 2, 0.2)
        c.img[..., :3] = np.clip(np.array((36, 28, 46), np.float32)[None, None, :] * (1 + 0.3 * (noise(c, 16, 6 + sg) - 0.5) + 0.06 * (noise(c, 2.5, 5) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        across = np.abs(x) / hw
        tone(c.img, smooth01(across, 0.6, 1.0), 1.7)
        mid = band(x, 0.0, 0.42) * smooth01(y, SP_BLADE[0] + 6, SP_BLADE[0] + 14) * (1 - smooth01(y, SP_BLADE[1] - 12, SP_BLADE[1] - 3))
        veins = contour_cracks(c, 5, 61 + sg, 0.5, 1.1) * (across < 0.85) * (noise(c, 26, 80 + sg) > 0.5)
        blend(c.img, pink, np.maximum(mid, veins), 0.9)
        blend(c.img, hot, mid * mid, 0.7)
        glow_into(c, pink, np.maximum(mid, veins * 0.8), 1.0, core=0.3)

    def socket(c):
        turn, _ = lathe_theta(c, SP_SOCKET_PROFILE)
        c.img[..., :3] = np.clip(np.array((46, 38, 54), np.float32)[None, None, :] * (1 + 0.3 * (noise(c, 3, 4) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        tick = band((turn * 12.0) % 1.0, 0.5, 0.11) * band(c.T, 0.62, 0.15)
        blend(c.img, pink, tick, 0.9)
        glow_into(c, pink, tick, 0.8, core=0.2)

    def shaft(c):
        c.img[..., :3] = np.clip(np.array((34, 28, 42), np.float32)[None, None, :] * (1 + 0.3 * (noise(c, 6, 3) - 0.5))[..., None], 0, 255)
        c.img[..., 3] = 255
        v = contour_cracks(c, 4, 71, 0.5, 1.1) * (noise(c, 20, 81) > 0.52)
        ph = (c.B / 21.0 + c.A / max(c.g.ext[0], 1e-6)) % 1.0                           # a thin ley line spirals round the whole shaft (one turn every 21 cm)
        ley = np.clip(1 - np.minimum(ph, 1 - ph) * 21.0 * c.g.ppu / 1.3, 0, 1)
        m2 = np.maximum(v, ley)
        blend(c.img, pink, m2, 0.85)
        glow_into(c, pink, m2, 0.8, core=0.2)

    def wrap(c):
        ph = ((c.B + c.A * 0.7) / 3.0) % 1.0
        shade = 0.85 + 0.25 * np.sin(ph * math.pi) ** 1.5 + 0.06 * (noise(c, 3, 9) - 0.5)
        c.img[..., :3] = np.clip(np.array((26, 20, 32), np.float32)[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        blend(c.img, pink, band(ph, 0.0, 0.06) + band(ph, 1.0, 0.06), 0.85)

    def vine(c):
        c.img[..., :3] = np.clip(np.array((72, 20, 74), np.float32)[None, None, :] * (0.8 + 0.4 * noise(c, 3, 5))[..., None], 0, 255)
        c.img[..., 3] = 255
        glow_into(c, pink, 0.25 * band(c.S, 0.5, 0.35), 0.6, core=0.1)

    def thorns(c):
        t = np.clip(c.T, 0, 1)
        c.img[..., :3] = np.clip(np.array((34, 24, 40), np.float32)[None, None, :] * (0.8 + 0.4 * noise(c, 3, 6))[..., None], 0, 255)
        c.img[..., 3] = 255
        root = np.clip(1 - t * 1.8, 0, 1) ** 1.3
        blend(c.img, pink, root, 0.7)
        glow_into(c, pink, root * 0.8, 0.9, core=0.2)

    def gem(c):
        t = 0.25 + 0.75 * np.clip(1 - np.abs(2 * c.T - 1) ** 1.3, 0, 1)
        hl = np.exp(-(((c.S - 0.3) / 0.08) ** 2) - (((c.T - 0.68) / 0.13) ** 2))
        to_gem(c, pink, hot, t, spec=np.clip(hl * 0.7, 0, 1), core=0.25)

    paint_group(atlas, m.part("spearhead"), head)
    paint_group(atlas, m.part("socket"), socket)
    paint_group(atlas, m.part("shaft"), shaft)
    paint_group(atlas, m.part("grip_wrap"), wrap)
    paint_group(atlas, m.part("vine"), vine)
    paint_group(atlas, m.part("thorns"), thorns)
    paint_group(atlas, m.part("gem"), gem)


def build_spear(curse=False, write=True):
    iid = "legendary_spear_curse" if curse else "legendary_spear"
    base = "legendary_spear"
    if write and not curse:
        write_icon(base)
    m = ml.Mesh(iid)
    spear_shaft(m, curse)
    spear_socket(m, curse)
    spear_gem(m, curse)
    spear_head(m, curse)
    if curse:
        spear_lust(m)
    atlas = lay(m)
    paint_spear(m, atlas, curse)
    if not write:
        return m, atlas
    if not curse:
        _BASE_PLACE[base] = diag_place(m, base, SP_HOLD)
    place = base_place(base, build_spear, lambda mm: diag_place(mm, base, SP_HOLD))
    st = write_look(iid, m, atlas, place, ("FFFFFF", 0.12, 0.5, 1.0))
    if not curse:
        write_item_models(base)
    return st


# ================================================================ 4. THE LEGENDARY BOW (Itsuki Kawasumi; Pride)
# Target (wiki summaries: a yellow gem; the shape is from memory of the anime and design): an elegant white recurve longbow with gold fittings and gold inlay
# lines along the limbs, swept-back gold feathers at the grip (the angel look the Pride series later corrupts), the yellow gem in the grip and a string of light.
# Real size: 160 cm tip to tip, 1.50 blocks apparent (class bow 1.30 - 1.60). Upright like the wave 23 bows: the belly (the grip) is at +x, the string near x = 7.
BW_HALF = 80.0                 # half the tip-to-tip length
BW_BELLY = 22.0                # how far the grip stands in front of x = 0 (the tips curl back to x = 7 where the string is: a brace height of 15 cm)
BW_RISER = 12.0                # half the straight riser
BW_POWER = 1.9                 # how the limbs fall away from the belly
BW_CURL = 7.0                  # the recurve: how far the tips curl forward again
BW_TIP_X = BW_CURL             # where the string is anchored
BW_GEM_Y = 7.6                 # the gem sits above the hand
BW_GEM_R, BW_GEM_D = 3.1, 3.7  # the lens-shaped cabochon through the riser: radius and half thickness

_BW_RISER_W = w1.hermite([(-14, 3.0), (-10, 3.7), (-5, 4.0), (0, 3.9), (4, 5.8), (7.6, 6.8), (11, 4.6), (14, 3.0)])      # the riser's width across (x) along its height
_BW_RISER_T = w1.hermite([(-14, 3.4), (-10, 4.4), (-5, 4.7), (0, 4.6), (4, 5.0), (7.6, 5.2), (11, 4.2), (14, 3.4)])      # and its thickness (z)


def bow_x(y):
    """x of the limb's centre line at height y: the riser is straight, then the limbs fall away to the tips and curl forward again."""
    u = max(0.0, (abs(y) - BW_RISER) / (BW_HALF - BW_RISER))
    c = max(0.0, (u - 0.78) / 0.22)
    return BW_BELLY * (1.0 - u ** BW_POWER) + BW_CURL * c * c


def limb_path(sign, n=14, y0=None):
    y0 = BW_RISER - 1.0 if y0 is None else y0
    return [(bow_x(sign * (y0 + (BW_HALF - y0) * i / n)), sign * (y0 + (BW_HALF - y0) * i / n), 0.0) for i in range(n + 1)]


def limb_half(t, a, b):
    """Linear taper from a (the root) to b (the tip) along a limb, t in 0..1."""
    return a + (b - a) * t


def bow_limbs(m, curse):
    """The two stave limbs (white, tapering, flexing in the draw states), their gold inlay lines and tip caps."""
    mat = ("iron", (44, 36, 62)) if curse else ("silver", (226, 230, 240))
    for sgn, name in ((1, "limb_upper"), (-1, "limb_lower")):
        p = add(m, name, mat[0], base=mat[1], smooth=55, uv_mode="loft", wear=0.5)
        ml.sweep(p, limb_path(sgn), ml.ellipse(1.0, 1.0, 8), scales=lambda t: (0.0, 0.0) if t > 0.992 else (limb_half(t, 1.75, 0.62), limb_half(t, 2.5, 0.85)),
                 caps=(True, True), up=(0, 0, 1))
    inl = add(m, "limb_inlay", "iron" if curse else "gold", base=(80, 40, 120) if curse else WHITE_GOLD, smooth=45, uv_mode="loft", wear=0.4)
    for sgn in (1, -1):
        for zs in (1, -1):
            pts = []
            for i in range(0, 11):
                t = i / 10
                y = sgn * (BW_RISER + 1.0 + (BW_HALF - BW_RISER - 10.0) * t)
                pts.append((bow_x(y), y, zs * limb_half(t, 2.35, 0.8)))
            ml.sweep(inl, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (limb_half(t, 1.5, 0.5), 0.34), caps=(True, True), up=(0, 0, 1))
    tp = add(m, "limb_horns" if curse else "limb_tips", "iron" if curse else "gold", base=(44, 34, 64) if curse else WHITE_GOLD, smooth=45, wear=0.4, uv_mode="loft")
    for sgn in (1, -1):
        y1 = sgn * BW_HALF
        d = unit((bow_x(y1) - bow_x(sgn * (BW_HALF - 4.0)), 4.0 * sgn, 0.0))
        c0 = V((bow_x(sgn * (BW_HALF - 3.0)), sgn * (BW_HALF - 3.0), 0.0))
        ml.loft(tp, tuple(d), tuple(c0), [(0.0, 0.8, 0.8), (1.0, 1.25, 1.25), (4.2, 1.1, 1.1), (6.0, 0.6, 0.6), (7.4, 0.0, 0.0)], ml.circle(1.0, 8), caps=(False, False))
    return tp


def bow_riser(m, curse):
    r = add(m, "riser", "iron" if curse else "gold", base=(58, 46, 78) if curse else WHITE_GOLD, smooth=45, wear=0.5)
    ys = [-14.0, -10.0, -5.0, 0.0, 4.0, 7.6, 11.0, 14.0]
    ml.loft(r, "y", (BW_BELLY, 0, 0), [(y, float(_BW_RISER_W(y)), float(_BW_RISER_T(y))) for y in ys], ml.rounded_rect(1.0, 1.0, 0.22, 2), caps=(True, True))
    gw = add(m, "grip_wrap", "leather", base=(40, 28, 56) if curse else (206, 178, 120), smooth=55, wear=0.6, uv_mode="loft")
    ml.loft(gw, "y", (BW_BELLY, 0, 0), [(-9.6, 4.5, 4.8), (-8.4, 4.9, 5.2), (0.4, 4.9, 5.2), (1.8, 4.5, 4.8)], ml.ellipse(1.0, 1.0, 10), caps=(False, False))
    return r, gw


def bow_wings(m, curse):
    """Swept-back feathers at the grip: two per limb root (three, longer and black, in the curse look), flat blades in the bow's plane: the angel look that
    Pride turns to a fallen angel's."""
    wg = add(m, "wings", "iron" if curse else "gold", base=(40, 30, 58) if curse else (232, 214, 150), smooth=40, uv_mode="loft", wear=0.4)
    if curse:
        blades = ((BW_BELLY - 2.0, 10.5, BW_BELLY - 19.0, 46.0, 4.6), (BW_BELLY - 2.0, 9.5, BW_BELLY - 16.0, 33.0, 4.0), (BW_BELLY - 2.0, 8.5, BW_BELLY - 10.5, 22.0, 3.2))
    else:
        blades = ((BW_BELLY - 2.0, 10.5, BW_BELLY - 15.5, 36.0, 4.2), (BW_BELLY - 2.0, 9.5, BW_BELLY - 12.0, 24.0, 3.2))
    for sgn in (1, -1):
        for (rx, ry, ex, ey, w0) in blades:
            p0 = V((rx, sgn * ry, 0.0))
            p2 = V((ex, sgn * ey, 0.0))
            p1 = V((rx - 1.5, sgn * (ry + (ey - ry) * 0.55), 0.0))
            ml.sweep(wg, bezier(p0, p1, p2, 6), ml.lozenge(1.0, 1.0), scales=lambda t, w0=w0: (0.0, 0.0) if t > 0.985 else (w0 * (1 - t ** 1.2) ** 0.8 + 0.1, 0.9 * (1 - 0.5 * t)),
                     caps=(True, False), up=(0, 0, 1))
    return wg


def bow_crest(m):
    """The Bow of Pride's crest: a crown on the riser above the grip and an inverted one below it, five points each (the limb is the middle point, two blades
    flank it on each side), on a curved band; and two long horns that carry the tips on (named limb_ so they flex with the draw)."""
    cr = add(m, "crest", "iron", base=(44, 34, 64), smooth=40, uv_mode="loft", wear=0.5)
    for sgn in (1, -1):
        by = sgn * (BW_RISER - 0.8)
        pts = [(BW_BELLY + 7.6 * math.sin(a), by - sgn * 1.6 * math.cos(a) ** 2, 0.0) for a in np.linspace(-math.pi / 2, math.pi / 2, 7)]
        ml.sweep(cr, pts, ml.rect_ring(1.7, 4.4), caps=(True, True), up=(0, 0, 1))        # the band under the points
        for (dx, ln, ang) in ((-5.6, 8.5, 38.0), (-3.0, 12.5, 16.0), (3.0, 12.5, -16.0), (5.6, 8.5, -38.0)):
            a = math.radians(ang)
            p0 = V((BW_BELLY + dx, by, 0.0))
            tip = p0 + V((-math.sin(a) * ln, sgn * math.cos(a) * ln, 0.0))
            mid = p0 + (tip - p0) * 0.5
            ml.sweep(cr, bezier(p0, mid, tip, 4), ml.lozenge(1.0, 1.0), scales=lambda t, ln=ln: (0.0, 0.0) if t > 0.985 else (2.6 * (1 - t) ** 0.8 + 0.1, 1.5 * (1 - 0.6 * t)),
                     caps=(True, False), up=(0, 0, 1))
    hn = m.part("limb_horns")                                            # the tip caps made it in bow_limbs; the horns grow out of them
    for sgn in (1, -1):
        p0 = V((bow_x(sgn * BW_HALF), sgn * (BW_HALF - 1.0), 0.0))
        horn(hn, tuple(p0), tuple(p0 + V((3.0, sgn * 9.0, 0.0))), tuple(p0 + V((13.0, sgn * 17.0, 0.0))), 1.15, sides=5, n=6, up=(0, 0, 1))
    return cr, hn


def bow_gem(m, curse):
    bez = add(m, "gem_setting", "iron" if curse else "gold", base=(60, 46, 80) if curse else WHITE_GOLD, smooth=50, wear=0.4)
    for sgn in (-1, 1):
        band_loop(bez, BW_GEM_R + 0.35, sgn * 2.55, ml.circle(0.7, 5), n=14, arcs=1, cx=BW_BELLY, cy=BW_GEM_Y)
    g = add(m, "gem", "crystal", base=SIN["bow"] if curse else GEM["bow"], glow=True, smooth=60, uv_mode="loft")
    d, r = BW_GEM_D, BW_GEM_R
    prof = [(-d, 0.0), (-d * 0.86, r * 0.52), (-d * 0.55, r * 0.86), (-d * 0.25, r * 0.98), (0.0, r), (d * 0.25, r * 0.98), (d * 0.55, r * 0.86), (d * 0.86, r * 0.52), (d, 0.0)]
    ml.lathe(g, "z", (BW_BELLY, BW_GEM_Y, 0), prof, sides=16, caps=(False, False))
    return bez, g


def bow_string(m, curse, n=12):
    """The string: a thin straight line tip to tip at x = the tips' x, many points so the draw states can bend it into a V. It is light, so it glows."""
    st = add(m, "string", "cloth", base=(200, 150, 255) if curse else (255, 236, 170), wear=0.1, smooth=70, uv_mode="loft", glow=True)
    pts = [(BW_TIP_X, BW_HALF - 2.0 * BW_HALF * i / n, 0.0) for i in range(n + 1)]
    rr = 0.34 if curse else 0.24
    ml.sweep(st, pts, ml.circle(1.0, 4), scales=lambda t: (rr, rr), caps=(True, True))
    return st


BW_ARROW_LEN = 74.0            # the arrow's length (its nock at the string, its head past the riser)
BW_ARROW_Z = 3.3               # it rests on the side of the riser
BW_DRAW, BW_FLEX = 32.0, 8.0   # how far the nock travels at full draw, how far each limb tip comes back (the rig of tools/relics/ranged_meshes.py)


def bow_arrow(m, curse):
    """The nocked arrow of the draw states (parts named arrow_*; the rest mesh leaves them out): a thin shaft, a leaf head and three vanes. The Holy Bow makes its
    own arrows, so the nocked arrow is an arrow of LIGHT: all three parts are painted bright and carry a strong glow in the bow's colour."""
    x0, z = BW_TIP_X, BW_ARROW_Z
    sh = add(m, "arrow_shaft", "cloth", base=(222, 190, 255) if curse else (255, 244, 205), smooth=60, uv_mode="loft", glow=True)
    ml.lathe(sh, "x", (0, 0, z), [(x0 - 1.5, 0.62), (x0, 0.4), (x0 + BW_ARROW_LEN - 6.0, 0.4)], sides=6)
    hd = add(m, "arrow_head", "iron" if curse else "gold", base=(230, 200, 255) if curse else (255, 246, 214), glow=True, smooth=40)
    ml.lathe(hd, "x", (0, 0, z), [(x0 + BW_ARROW_LEN - 6.4, 0.5), (x0 + BW_ARROW_LEN - 3.4, 1.15), (x0 + BW_ARROW_LEN, 0.0)], sides=6, caps=(True, False))
    fl = add(m, "arrow_fletch", "cloth", base=(200, 150, 255) if curse else (255, 250, 228), smooth=30, glow=True)
    for k in range(3):
        a = k * 2 * math.pi / 3
        ml.bar(fl, (x0 + 1.4, 0.0, z), (x0 + 12.0, 1.9 * math.cos(a), z + 1.9 * math.sin(a)), 0.7, 0.14, up=(0, 0, 1) if k else (0, 1, 0), taper=3.0, end_scale=0.9)
    return sh, hd, fl


def emit_bow_states(mesh, atlas, item_id, place, glow):
    """Writes <id>.txt (rest: no arrow) and <id>_pull1..3 (drawn: limbs flexed, the string a V, the arrow riding the nock) from a finished mesh, with the
    core's rig (tools/relics/ranged_meshes.py), exactly as the relic bows of wave 23 are made. Every state keeps the rest mesh's UVs and so its skins."""
    import ranged_meshes as rm
    mesh.place(*place)
    mesh.set_glow(*glow)
    mesh.orient_cfg = "manual"
    mesh.finish()
    rig = rm.Rig(tip_a=(BW_TIP_X, BW_HALF, 0.0), tip_b=(BW_TIP_X, -BW_HALF, 0.0), draw_dir=(-1, 0, 0), max_draw=BW_DRAW, flex=BW_FLEX,
                 grip=(BW_BELLY, 0.0, 0.0), limb_axis=(0, 1, 0), limb_span=BW_HALF)
    nock = rig.flex + rig.max_draw

    def build(f, with_arrow):
        out = ml.Mesh(mesh.name)
        out.place_cfg, out.glow_cfg, out.orient_cfg = mesh.place_cfg, mesh.glow_cfg, mesh.orient_cfg
        for p in mesh.parts:
            role = rm._role(p.name)
            if role == "arrow" and not with_arrow:
                continue
            c = rm._copy_part(p)
            if role == "limb":
                c.V = [r for r in rm.displace_limb(rig, np.array(p.V), 0.0, f)]
            elif role == "string":
                c.V = [r for r in rm.displace_string(rig, np.array(p.V), 0.0, f)]
            elif role == "arrow":
                c.V = [r for r in np.atleast_2d(np.array(p.V)) + rig.d() * nock * f]
            out.add(c)
        return out

    os.makedirs(MESH_DIR, exist_ok=True)
    n = build(0.0, False).write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, "vertex", comment="generated by tools/heroweapon/art_hart1.py (rest)")
    for suffix, f, _ in rm.BOW_STATES:
        build(f, True).write_txt(os.path.join(MESH_DIR, item_id + suffix + ".txt"), atlas, "vertex", comment="generated by tools/heroweapon/art_hart1.py (%s, f = %.2f)" % (suffix, f))
    atlas.save(item_id)
    return n


def paint_bow(m, atlas, curse):
    """The skin of the bow: white limbs with a faint grain (in the curse look black with violet veins), the wrapped grip, the gem, the string of light, the
    glow of the inlay, the crest and the horns in the curse look."""
    violet, hot = SIN["bow"], (225, 190, 255)
    gem_rgb, gem_hi = (violet, hot) if curse else (GEM["bow"], (255, 246, 190))
    paint_metal(atlas, m, ["riser", "gem_setting"] + (["limb_horns"] if curse else ["limb_tips", "limb_inlay"]), sheen=0.2 if curse else 0.14, grain=0.08 if curse else 0.045, seed=400)

    def limb(c):
        base = np.array((38, 30, 54) if curse else (226, 230, 240), np.float32)
        shade = 0.9 + 0.2 * (noise(c, 14, 3) - 0.5) + 0.06 * (noise(c, 2.5, 4) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        tone(c.img, band(c.T, 1.0, 0.25), 0.9)
        if curse:
            v = contour_cracks(c, 3, 91, 0.5, 1.2) * (noise(c, 12, 92) > 0.5) * smooth01(c.T, 0.1, 0.3)
            blend(c.img, violet, v, 0.8)
            glow_into(c, violet, v, 0.85, core=0.25)

    def inlay(c):
        if not curse:                                                                   # the gold inlay glows faintly in the gem's yellow
            glow_into(c, GEM["bow"], np.full(c.img.shape[:2], 0.5, np.float32), 0.45, core=0.2)
            return
        c.img[..., :3] = np.array((150, 90, 230), np.float32)
        c.img[..., 3] = 255
        glow_into(c, violet, np.ones(c.img.shape[:2], np.float32) * 0.75, 0.8, core=0.3)

    def grip(c):
        ph = ((c.B + c.A * 0.5) / 2.4) % 1.0
        base = np.array((30, 24, 40) if curse else (206, 178, 120), np.float32)
        shade = 0.8 + 0.3 * np.sin(ph * math.pi) ** 1.5 + 0.06 * (noise(c, 3, 9) - 0.5)
        c.img[..., :3] = np.clip(base[None, None, :] * shade[..., None], 0, 255)
        c.img[..., 3] = 255
        if curse:
            blend(c.img, violet, band(ph, 0.0, 0.06) + band(ph, 1.0, 0.06), 0.85)
        else:
            groove(c, band(ph, 0.0, 0.07) + band(ph, 1.0, 0.07), 0.5)

    def gem(c):
        t = np.abs(2 * np.clip(c.T, 0, 1) - 1)
        hl = np.exp(-(((c.S - 0.3) / 0.07) ** 2) - (((t - 0.62) / 0.15) ** 2))
        to_gem(c, gem_rgb, gem_hi, 0.12 + 0.88 * t ** 0.85, spec=np.clip(hl * 0.8, 0, 1), core=0.25 if curse else 0.15)

    def string(c):
        c.img[..., :3] = np.array((226, 190, 255) if curse else (240, 226, 170), np.float32)
        c.img[..., 3] = 255
        glow_into(c, violet if curse else (255, 232, 130), np.ones(c.img.shape[:2], np.float32), 0.75, core=0.4)

    def wings(c):
        t = np.clip(c.T, 0, 1)
        if curse:
            c.img[..., :3] = np.clip(np.array((36, 28, 52), np.float32)[None, None, :] * (0.85 + 0.3 * noise(c, 3, 5))[..., None], 0, 255)
            c.img[..., 3] = 255
            vein = band(c.S, 0.5, 0.2) * np.clip(1 - t * 1.1, 0, 1)
            blend(c.img, violet, vein, 0.8)
            glow_into(c, violet, vein * 0.85, 0.85, core=0.25)
        else:
            base = np.array((236, 218, 156), np.float32)
            c.img[..., :3] = np.clip(base[None, None, :] * (0.9 + 0.2 * noise(c, 3, 5))[..., None], 0, 255)
            c.img[..., 3] = 255
            groove(c, band(c.S, 0.5, 0.05) * np.clip(1 - t, 0, 1), 0.35)

    def crest(c):
        t = np.clip(c.T, 0, 1)
        c.img[..., :3] = np.clip(np.array((40, 30, 58), np.float32)[None, None, :] * (0.85 + 0.3 * noise(c, 3, 6))[..., None], 0, 255)
        c.img[..., 3] = 255
        root = np.clip(1 - t * 1.4, 0, 1) ** 1.2
        blend(c.img, violet, root, 0.7)
        glow_into(c, violet, root * 0.8, 0.9, core=0.25)

    for nm in ("limb_upper", "limb_lower"):
        paint_group(atlas, m.part(nm), limb)
    paint_group(atlas, m.part("limb_inlay"), inlay)
    paint_group(atlas, m.part("grip_wrap"), grip)
    paint_group(atlas, m.part("gem"), gem)
    paint_group(atlas, m.part("string"), string)
    paint_group(atlas, m.part("wings"), wings)
    if curse:
        paint_group(atlas, m.part("crest"), crest)
        paint_group(atlas, m.part("limb_horns"), crest)

    def light(c):                                                                       # the nocked arrow is made of light (it only exists in the draw states)
        c.img[..., :3] = np.array((226, 196, 255) if curse else (255, 246, 214), np.float32)
        c.img[..., 3] = 255
        glow_into(c, violet if curse else (255, 226, 120), np.ones(c.img.shape[:2], np.float32), 0.95, core=0.5)

    for nm in ("arrow_shaft", "arrow_head", "arrow_fletch"):
        paint_group(atlas, m.part(nm), light)


def build_bow(curse=False, write=True):
    iid = "legendary_bow_curse" if curse else "legendary_bow"
    base = "legendary_bow"
    if write and not curse:
        write_icon(base)
    m = ml.Mesh(iid)
    bow_limbs(m, curse)
    bow_riser(m, curse)
    bow_wings(m, curse)
    bow_gem(m, curse)
    if curse:
        bow_crest(m)
    bow_string(m, curse)
    bow_arrow(m, curse)
    atlas = lay(m)
    paint_bow(m, atlas, curse)
    if not write:
        return m, atlas
    if not curse:
        _BASE_PLACE[base] = upright_place(m, base, hold_x=BW_BELLY)
    place = base_place(base, build_bow, lambda mm: upright_place(mm, base, hold_x=BW_BELLY))
    n = emit_bow_states(m, atlas, iid, place, ("FFFFFF", 0.12, 0.5, 1.0))
    st = m.stats(atlas)
    st["lines"] = n
    print(f"{iid}: {len(m.parts)} parts (4 states), {n} quad lines at rest, length {st['length']:.1f} cm, skin {atlas.size}px at {atlas.ppu:.2f} px/cm")
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        ml.preview(os.path.join(MESH_DIR, iid + ".txt"), None, os.path.join(PREVIEW, iid + ".png"), normals="vertex")
    if not curse:
        write_item_models(base)
    return st


BUILDERS = {"legendary_shield": lambda: build_shield(False), "legendary_shield_curse": lambda: build_shield(True),
            "legendary_sword": lambda: build_sword(False), "legendary_sword_curse": lambda: build_sword(True),
            "legendary_spear": lambda: build_spear(False), "legendary_spear_curse": lambda: build_spear(True),
            "legendary_bow": lambda: build_bow(False), "legendary_bow_curse": lambda: build_bow(True)}

# the class of every look in tools/relics/weapon_classes.json (the lead merges the fragment; the curse looks are listed too, the id of a curse mesh is not a
# state suffix the class table strips)
DESIGN_LENGTH = {"legendary_shield": 2 * SH_R, "legendary_sword": SW_LEN, "legendary_spear": SP_LEN, "legendary_sword_curse": SW_LEN, "legendary_spear_curse": SP_LEN}   # cm, the named constants
CLASSES = {"legendary_shield": "shield", "legendary_sword": "longsword", "legendary_spear": "spear", "legendary_bow": "bow"}


def write_classes():
    """tools/heroweapon/classes_hart1.json: look id -> class of tools/relics/weapon_classes.json (an existing class: no new one is needed)."""
    out = {}
    for base, cls in CLASSES.items():
        out[base] = cls
        out[base + "_curse"] = cls
    path = os.path.join(HERE, "classes_hart1.json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=2)
        f.write("\n")
    return path


def output_files():
    """Every file the script writes, relative to the assets (the files of HeroAssetsTest, the bow's draw states, the shield's raised model)."""
    files = []
    for base in IDS:
        for iid in (base, base + "_curse"):
            files += ["weapon_meshes/%s.txt" % iid, "textures/entity/weapon/%s.png" % iid, "textures/entity/weapon/%s_glow.png" % iid]
        files += ["models/item/%s.json" % base, "models/item/%s_icon.json" % base, "textures/item/%s.png" % base]
    for suf in ("_pull1", "_pull2", "_pull3"):
        files += ["weapon_meshes/legendary_bow%s.txt" % suf, "weapon_meshes/legendary_bow_curse%s.txt" % suf]
    files.append("models/item/legendary_shield_blocking.json")
    return files


def self_check(only=None):
    """Checks every written look against the art contract: quads <= 2800 (the whole file, glow copies included), 8 to 14 named parts, the grip directive absent or
    <= 0.5 (an upside-down mesh otherwise), `orient manual`, finite numbers, UVs inside the 128 space, unit normals, the two skins and the sprite present.
    Returns a list of problems (empty = all fine)."""
    problems = []
    for rel in output_files():
        if not os.path.exists(os.path.join(ASSETS, rel)):
            problems.append("missing " + rel)
    for rel in output_files():
        if not rel.startswith("weapon_meshes/"):
            continue
        iid = os.path.basename(rel)[:-4]
        if only and not any(iid.startswith(o) for o in only):
            continue
        quads, parts, place, grip, orient = 0, [], None, None, False
        ylo, yhi = 1e9, -1e9
        for line in open(os.path.join(ASSETS, rel), encoding="utf-8"):
            v = line.split()
            if not v:
                continue
            if v[0] == "place":
                place = [float(x) for x in v[1:6]]
            elif v[0] == "grip":
                grip = float(v[1])
            elif v[0] == "orient":
                orient = v[1] == "manual"
            elif v[0] == "#" and len(v) > 2 and v[1] == "part":
                parts.append(v[2])
            elif v[0] == "qn":
                quads += 1
                vals = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
                ylo, yhi = min(ylo, float(vals[:, 1].min())), max(yhi, float(vals[:, 1].max()))
                if not np.all(np.isfinite(vals)):
                    problems.append(iid + ": a non-finite number")
                if vals[:, 3:5].min() < -0.01 or vals[:, 3:5].max() > 128.01:
                    problems.append(iid + ": a UV outside the 128 space")
                if np.abs(np.linalg.norm(vals[:, 5:8], axis=1) - 1).max() > 0.02:
                    problems.append(iid + ": a normal that is not a unit vector")
            elif v[0] == "q":
                quads += 1
        n_parts = len(set(parts))
        if quads > 2800:
            problems.append("%s: %d quads (budget 2800)" % (iid, quads))
        if not 8 <= n_parts <= 14:
            problems.append("%s: %d named parts (8 to 14)" % (iid, n_parts))
        if grip is not None and grip > 0.5:
            problems.append("%s: grip directive %.3f would turn the mesh over" % (iid, grip))
        if not orient:
            problems.append(iid + ": no `orient manual`")
        if place is None or len(place) != 5:
            problems.append(iid + ": no place line")
        want_len = DESIGN_LENGTH.get(iid)
        if want_len is not None and abs((yhi - ylo) - want_len) > 0.6:
            problems.append("%s: %.1f cm long, the design says %.1f" % (iid, yhi - ylo, want_len))
        problems += skin_holes(iid)
    return problems


def skin_holes(iid):
    """A quad whose corners or centre fall on a transparent skin pixel would show a see-through crack in the game (the skin is a cutout): none may."""
    base = re.sub(r"_pull[123]$", "", iid)
    skin = np.asarray(Image.open(os.path.join(SKIN_DIR, base + ".png")).convert("RGBA"))
    k = skin.shape[1] / 128.0
    holes = 0
    for line in open(os.path.join(MESH_DIR, iid + ".txt"), encoding="utf-8"):
        v = line.split()
        if not v or v[0] != "qn" or v[1] != "0":
            continue
        uv = np.array([[float(v[2 + i * 8 + 3]), float(v[2 + i * 8 + 4])] for i in range(4)])
        for (u, w) in list(uv) + [uv.mean(axis=0)]:
            if skin[int(min(max(w * k, 0), skin.shape[0] - 1)), int(min(max(u * k, 0), skin.shape[1] - 1)), 3] < 128:
                holes += 1
    return ["%s: %d transparent skin samples" % (iid, holes)] if holes else []


def file_hashes():
    """SHA-256 of every file the script writes (the asset files and the class fragment), for the determinism check."""
    import hashlib
    paths = {rel: os.path.join(ASSETS, rel) for rel in output_files()}
    paths["classes_hart1.json"] = os.path.join(HERE, "classes_hart1.json")
    out = {}
    for rel, path in paths.items():
        if os.path.exists(path):
            with open(path, "rb") as f:
                out[rel] = hashlib.sha256(f.read()).hexdigest()
    return out


def main(argv):
    flags = [a for a in argv if a.startswith("-")]
    want = [a for a in argv if not a.startswith("-")] or list(BUILDERS)
    for iid in LOOKS:
        if iid in want and iid in BUILDERS:
            BUILDERS[iid]()
    print("wrote", os.path.relpath(write_classes(), ROOT))
    if "--check" in flags:
        first = file_hashes()
        for iid in LOOKS:
            if iid in want and iid in BUILDERS:
                BUILDERS[iid]()
        write_classes()
        second = file_hashes()
        diff = [k for k in first if first[k] != second.get(k)]
        print("determinism: %d files, %s" % (len(first), "identical on a second run" if not diff else "DIFFERENT: " + ", ".join(diff)))
        if diff:
            return 1
    problems = self_check(want if len(want) < len(BUILDERS) else None)
    for pr in problems:
        print("PROBLEM", pr)
    print("self check:", "all fine" if not problems else "%d problems" % len(problems))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
