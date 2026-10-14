"""djinn_weapons: builds the 24 temporary Djinn weapons' assets from the Djinn armour set's own weapon piece (1.34.0).

For each of the twelve Djinn and each look (light = set u_<id>, dark = set d_<id>) it reads the held weapon piece
(assets/nusmp/djinn/<set>/weapon.txt: item space, the grip at the origin, the blade along +y, UV in 128 units of the set's armour skin) and writes

  weapon_meshes/djinn_weapon_<id>[_dark].txt          the baked mesh in WeaponRenderer's format (same quads, `place` lines the weapon on the sprite diagonal
                                                      with the hand where the armour layer holds it, the directive `orient manual`, white glow so the
                                                      painted glow skin shows as painted)
  textures/entity/weapon/djinn_weapon_<id>[_dark].png         the skin: the armour skin with everything the weapon's quads do not cover cleared
  textures/entity/weapon/djinn_weapon_<id>[_dark]_glow.png    the same for the additive glow skin
  models/item/djinn_weapon_<id>[_dark].json           builtin/entity with the display transforms; first person from tools/relics/display_presets.py
                                                      (the grip where the weapon is really held), marked "manual" so that tool leaves it alone
  models/item/djinn_weapon_<id>[_dark]_icon.json      the inventory icon model (handheld, the 16 x 16 sprite)
  textures/item/djinn_weapon_<id>[_dark].png          the 16 x 16 inventory icon, rendered from the mesh and its skin

The size rule (docs/weapon_scale_orientation.md, 1.34.0): long weapons are 2.2 to 3.6 blocks, so each weapon has a TARGET apparent length (third-person
blocks) below; the mesh is scaled to it. The edge / head of an asymmetric weapon is turned to -x (the orientation convention). Run:

    python tools/relics/djinn_weapons.py            # writes everything
    python tools/relics/djinn_weapons.py --check    # exit 1 if a file is missing or differs from what would be written (CI)
    python tools/relics/djinn_weapons.py --sheet out.png   # a contact sheet of the 24 icons
"""
import argparse
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import display_presets as dp       # noqa: E402
import weapon_orientation as wo    # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

DJINN = ["baal", "amon", "vinea", "focalor", "zagan", "zepar", "furfur", "marchosias", "sitri", "bifron", "paimon", "vassago"]
# the target apparent length (third-person blocks) of each weapon: the long ones in the 2.2 - 3.6 band of the size rule
TARGET = {"baal": 3.3, "amon": 2.8, "vinea": 3.2, "focalor": 3.4, "zagan": 2.8, "zepar": 2.7, "furfur": 2.3, "marchosias": 1.7,
          "sitri": 2.4, "bifron": 3.4, "paimon": 3.2, "vassago": 2.8}
THIRD_PERSON_SCALE = 0.85
# the point of the sprite the armour layer holds a weapon at (client/relics/djinn/DjinnArmorLayer.toItemSpace): the mesh origin goes here
HAND = 0.12
GLOW_LINE = "glow FFFFFF 0.14 0.55 1.0"
MASK_GROW = 2                       # pixels the used part of a skin is widened by, so no edge samples a cleared pixel


def item_id(d, dark):
    return "djinn_weapon_" + d + ("_dark" if dark else "")


def mesh_set(d, dark):
    return ("d_" if dark else "u_") + d


def skin_path(d, dark, glow=False):
    return os.path.join(ASSETS, "textures", "entity", "djinn", "%s_%s_armor%s.png" % (d, "d" if dark else "u", "_glow" if glow else ""))


# ================================================================ reading
def read_piece(path):
    """-> list of (layer, [8 floats * 4]) : x y z u v nx ny nz per vertex."""
    out = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v or v[0] != "qn":
                continue
            vals = [float(t) for t in v[2:34]]
            out.append((int(v[1]), [vals[k * 8:k * 8 + 8] for k in range(4)]))
    return out


def as_orientation_quads(quads):
    return [(layer, [(p[0], p[1], p[2]) for p in verts], [(p[5], p[6], p[7]) for p in verts]) for layer, verts in quads]


def mirror(quads):
    """x -> -x with the winding reversed (v0 v3 v2 v1), normals' x negated: front faces stay front faces."""
    out = []
    for layer, verts in quads:
        m = [[-p[0], p[1], p[2], p[3], p[4], -p[5], p[6], p[7]] for p in verts]
        out.append((layer, [m[0], m[3], m[2], m[1]]))
    return out


def fix_winding(quads):
    """Reorders a quad whose geometric normal opposes its stored normal (v0 v3 v2 v1); returns (quads, how many were reordered)."""
    out, n = [], 0
    for layer, verts in quads:
        a = np.array([verts[1][:3]]) - np.array([verts[0][:3]])
        b = np.array([verts[2][:3]]) - np.array([verts[0][:3]])
        g = np.cross(a, b)[0]
        stored = np.array(verts[0][5:8]) + np.array(verts[1][5:8]) + np.array(verts[2][5:8]) + np.array(verts[3][5:8])
        if np.dot(g, stored) < 0:
            out.append((layer, [verts[0], verts[3], verts[2], verts[1]]))
            n += 1
        else:
            out.append((layer, verts))
    return out, n


def bounds(quads):
    ys = [p[1] for _, verts in quads for p in verts]
    xs = [p[0] for _, verts in quads for p in verts]
    zs = [p[2] for _, verts in quads for p in verts]
    return min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)


def imbalance(quads, scale):
    """The audit's measure (tools/relics/weapon_orientation.py): the side of the haft the mass hangs on, in blocks (negative = toward -x)."""
    m = wo.measure(as_orientation_quads(quads), scale)
    return 0.0 if m.empty else m.imbalance


# ================================================================ building
def build(d, dark):
    """-> dict(quads, place, scale, ymin, length, mirrored, fixed)"""
    src = os.path.join(ASSETS, "djinn", mesh_set(d, dark), "weapon.txt")
    quads = read_piece(src)
    x0, x1, y0, y1, z0, z1 = bounds(quads)
    sc = TARGET[d] / ((y1 - y0) * THIRD_PERSON_SCALE)
    mirrored = False
    if imbalance(quads, sc) > 0.02:                    # the head hangs toward +x: the convention wants it forward, at -x
        quads = mirror(quads)
        mirrored = True
    quads, fixed = fix_winding(quads)
    x0, x1, y0, y1, z0, z1 = bounds(quads)
    ox = HAND + y0 * sc * math.sqrt(0.5)
    return dict(quads=quads, scale=sc, ymin=y0, ymax=y1, ox=ox, oy=ox, mirrored=mirrored, fixed=fixed, box=(x0, x1, y0, y1, z0, z1))


def mesh_text(d, dark, b):
    lines = ["# generated by tools/relics/djinn_weapons.py (%s): the %s weapon piece of the Djinn set, scaled to %.2f blocks" % (item_id(d, dark), mesh_set(d, dark), TARGET[d]),
             "place %.5f %.5f 45.000 %.6f %.3f" % (b["ox"], b["oy"], b["scale"], b["ymin"]),
             "orient manual",
             GLOW_LINE]
    for layer, verts in b["quads"]:
        lines.append("qn %d " % layer + " ".join("%.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f" % tuple(p) for p in verts))
    return "\n".join(lines) + "\n"


# ================================================================ skins
def masked_skin(d, dark, glow, quads):
    """The armour skin with every pixel the weapon's quads do not use cleared (alpha 0): a few per cent of a 1024 sheet stays, so the file is small."""
    sk = Image.open(skin_path(d, dark, glow)).convert("RGBA")
    w, h = sk.size
    m = Image.new("L", sk.size, 0)
    dr = ImageDraw.Draw(m)
    for _, verts in quads:
        dr.polygon([(p[3] * w / 128.0, p[4] * h / 128.0) for p in verts], fill=255)
    if MASK_GROW > 0:
        from PIL import ImageFilter
        m = m.filter(ImageFilter.MaxFilter(2 * MASK_GROW + 1))
    out = Image.new("RGBA", sk.size, (0, 0, 0, 0))
    out.paste(sk, mask=m)
    return out


# ================================================================ model json
def display_json(d, dark, b):
    """The model json: builtin/entity, third person as every weapon, first person / ground / frame from display_presets with the grip where it is held."""
    box = b["box"]
    info = dp.MeshInfo(b["ox"], b["oy"], 45.0, b["scale"], b["ymin"], box)
    grip_frac = (0.0 - b["ymin"]) / max(1e-9, (box[3] - b["ymin"]))
    gx = b["ox"] + grip_frac * info.length * math.cos(math.radians(45.0))
    info.grip = lambda fraction=None, _g=(gx, gx, 0.5): _g          # the hand holds the weapon at its mesh origin, not a quarter up
    rot = (0, -90, 25)
    s, t, ok = dp.preset(info, rot)
    ground, fixed = dp.view_scales(info.length)

    def ent(rotation, translation, scale):
        return {"rotation": list(rotation), "translation": list(translation), "scale": [scale] * 3}

    sc3 = THIRD_PERSON_SCALE
    disp = {
        "thirdperson_righthand": ent((0, -90, 55), (0, 4, 0.5), sc3),
        "thirdperson_lefthand": ent((0, 90, -55), (0, 4, 0.5), sc3),
        "firstperson_righthand": ent((0, -90, 25), t, s),
        "firstperson_lefthand": ent((0, 90, -25), t, s),
        "ground": ent((0, 0, 0), (0, 2, 0), ground),
        "head": ent((0, 180, 0), (0, 13, 7), 1),
        "fixed": ent((0, 180, 0), (0, 0, 0), fixed),
        "gui": ent((0, 0, 0), (0, 0, 0), 1),
    }
    for k in ("ground", "head", "fixed", "gui"):
        disp[k]["scale"] = [int(v) if float(v) == int(v) else v for v in disp[k]["scale"]]
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id(d, dark)},
             "display": disp, "nusmp_display_preset": "manual",
             "nusmp_djinn_weapon": {"length_blocks": round(info.length * sc3, 3), "first_person_scale": s, "tip_in_view": ok}}
    return json.dumps(model, indent=2) + "\n"


def icon_model_json(d, dark):
    return json.dumps({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id(d, dark)}}, indent=2) + "\n"


# ================================================================ the 16 x 16 icon
def render_icon(d, dark, b, size=16, ss=8):
    """Renders the mesh as it stands on the sprite diagonal (the way the old flat icons show a weapon) from its skin, lit a little, glow added; outlined."""
    body = Image.open(skin_path(d, dark, False)).convert("RGBA")
    glow = Image.open(skin_path(d, dark, True)).convert("RGBA")
    bw, bh = body.size
    bpx, gpx = np.array(body), np.array(glow)
    N = size * ss
    inv = math.sqrt(0.5)
    tris = []
    pts = []
    for layer, verts in b["quads"]:
        proj = [((p[1] + p[0]) * inv, (p[1] - p[0]) * inv, p[2]) for p in verts]    # sprite x right, y up; z toward the viewer
        pts.extend((q[0], q[1]) for q in proj)
        nz = sum(p[7] for p in verts)
        ny = sum(p[6] for p in verts)
        nx = sum(p[5] for p in verts)
        n = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
        shade = 0.62 + 0.38 * max(0.0, (-0.4 * nx / n + 0.5 * ny / n + 0.76 * nz / n))
        tris.append((layer, proj, verts, shade, nz))
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    pad = 1.6 * ss
    span = max(max(xs) - min(xs), max(ys) - min(ys), 1e-6)
    k = (N - 2 * pad) / span
    cx, cy = (max(xs) + min(xs)) / 2.0, (max(ys) + min(ys)) / 2.0

    def to_px(q):
        return ((q[0] - cx) * k + N / 2.0, N / 2.0 - (q[1] - cy) * k)

    col = np.zeros((N, N, 3))
    alpha = np.zeros((N, N))
    zbuf = np.full((N, N), -1e9)
    add = np.zeros((N, N, 3))
    for layer, proj, verts, shade, nz in sorted(tris, key=lambda t: (t[0] != 0)):
        if nz <= 0:
            continue                                                   # a face turned away from the viewer
        P = [to_px(q) for q in proj]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, bb, c = (P[i] for i in tri)
            x0, x1 = int(max(0, math.floor(min(a[0], bb[0], c[0])))), int(min(N - 1, math.ceil(max(a[0], bb[0], c[0]))))
            y0, y1 = int(max(0, math.floor(min(a[1], bb[1], c[1])))), int(min(N - 1, math.ceil(max(a[1], bb[1], c[1]))))
            den = (bb[1] - c[1]) * (a[0] - c[0]) + (c[0] - bb[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            for py in range(y0, y1 + 1):
                for px in range(x0, x1 + 1):
                    qx, qy = px + 0.5, py + 0.5
                    l1 = ((bb[1] - c[1]) * (qx - c[0]) + (c[0] - bb[0]) * (qy - c[1])) / den
                    l2 = ((c[1] - a[1]) * (qx - c[0]) + (a[0] - c[0]) * (qy - c[1])) / den
                    l3 = 1 - l1 - l2
                    if l1 < -0.01 or l2 < -0.01 or l3 < -0.01:
                        continue
                    z = l1 * proj[tri[0]][2] + l2 * proj[tri[1]][2] + l3 * proj[tri[2]][2]
                    vs = [verts[tri[0]], verts[tri[1]], verts[tri[2]]]
                    u = (l1 * vs[0][3] + l2 * vs[1][3] + l3 * vs[2][3]) / 128.0
                    v = (l1 * vs[0][4] + l2 * vs[1][4] + l3 * vs[2][4]) / 128.0
                    tx, ty = min(bw - 1, max(0, int(u * bw))), min(bh - 1, max(0, int(v * bh)))
                    if layer == 0:
                        if z >= zbuf[py, px]:
                            r = bpx[ty, tx]
                            if r[3] < 40:
                                continue
                            zbuf[py, px] = z
                            col[py, px] = r[:3] * shade
                            alpha[py, px] = 1.0
                            add[py, px] = 0
                    elif layer == 2 and z >= zbuf[py, px] - 0.05 and alpha[py, px] > 0:
                        gcol = gpx[ty, tx]
                        if gcol[3] > 40:
                            add[py, px] = np.maximum(add[py, px], gcol[:3] * 0.75)
    col = np.minimum(255.0, col + add)
    # down to size x size: premultiplied box filter, alpha by coverage
    cov = alpha.reshape(size, ss, size, ss).mean(axis=(1, 3))
    prem = (col * alpha[..., None]).reshape(size, ss, size, ss, 3).sum(axis=(1, 3))
    cnt = np.maximum(alpha.reshape(size, ss, size, ss).sum(axis=(1, 3)), 1e-9)
    rgb = prem / cnt[..., None]
    solid = cov > 0.16
    img = np.zeros((size, size, 4), np.uint8)
    img[solid, :3] = np.clip(rgb[solid], 0, 255).astype(np.uint8)
    img[solid, 3] = 255
    out = img.copy()
    for y in range(size):                                              # a dark outline round the shape, as the other item sprites have
        for x in range(size):
            if solid[y, x]:
                continue
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < size and 0 <= xx < size and solid[yy, xx]:
                    out[y, x] = (22, 14, 30, 255)
                    break
    return Image.fromarray(out, "RGBA")


# ================================================================ the lang fragment
NAMES = {
    "baal": ("Baal's Thunder Lance", "Baal's Black Thunder Lance"), "amon": ("Amon's Flame Greatsword", "Amon's Black Sun Greatsword"),
    "vinea": ("Vinea's Tide Trident", "Vinea's Drowned Trident"), "focalor": ("Focalor's Gale Glaive", "Focalor's Black Gale Glaive"),
    "zagan": ("Zagan's Bloom Staff", "Zagan's Withered Staff"), "zepar": ("Zepar's Titan Axe", "Zepar's Black Titan Axe"),
    "furfur": ("Furfur's War Horn", "Furfur's Black War Horn"), "marchosias": ("Marchosias' Ember Claws", "Marchosias' Black Ember Claws"),
    "sitri": ("Sitri's Dawn Bow", "Sitri's Dusk Bow"), "bifron": ("Bifron's Storm Glaive", "Bifron's Black Storm Glaive"),
    "paimon": ("Paimon's Twin-Moon Glaive", "Paimon's Black Twin-Moon Glaive"), "vassago": ("Vassago's Abyssal Scythe", "Vassago's Black Abyssal Scythe"),
}
# act key -> (name, description); the keys are DjinnWeaponRules' (a test keeps the two in step)
ACTS = {
    "thunder_throw": ("Thunder Throw", "Hurls a bolt along your gaze that pierces up to five foes, slows them, and jumps on to the nearest others."),
    "judgement_step": ("Judgement Step", "Dashes forward in a trail of lightning, striking and slowing everything you pass."),
    "sun_cleave": ("Sun Cleave", "A wide arc of sunfire in front of you: it burns every foe it touches and shoves them back."),
    "ember_wall": ("Ember Wall", "Raises a wall of flame two blocks ahead: foes in it burn and are thrown back; you shrug off fire for five seconds."),
    "tide_spear": ("Tide Spear", "A jet of water along your gaze: it hits everything in its line and pushes it away."),
    "frost_surge": ("Frost Surge", "A cone of freezing water: foes are hurt, frozen and slowed to a crawl."),
    "cyclone_sweep": ("Cyclone Sweep", "Spins a ring of wind round you that throws every foe near you outward and up."),
    "updraft_leap": ("Updraft Leap", "Leaps high on a column of wind (and floats down gently), lifting the foes at your feet."),
    "bloom_heal": ("Bloom Heal", "A garden opens round you: you and your allies are healed and mended, foes are hurt and rooted by vines."),
    "thorn_snare": ("Thorn Snare", "Thorns lash up to three foes in front of you: they are hurt, rooted and poisoned."),
    "titan_slam": ("Titan Slam", "Brings the axe down: a shockwave round you hurts every foe near, harder close in, and throws them into the air."),
    "rock_breaker": ("Rock Breaker", "A crushing blow on up to three foes ahead: the more armour they wear the harder it lands, and it leaves them weakened and fragile."),
    "resonance_wave": ("Resonance Wave", "A cone of sound: foes are hurt, thrown back and silenced."),
    "dissonance": ("Dissonance", "A burst of clashing chords round you: foes near you are hurt, dizzied and slowed."),
    "ember_rush": ("Ember Rush", "Pounces forward in a streak of fire, setting alight everything in the path."),
    "brand": ("Brand", "Brands the foe in your sights: it burns, glows, and takes more damage from everyone for eight seconds."),
    "dawn_arrow": ("Dawn Arrow", "Looses a shaft of dawn light that never drops: it strikes the first two foes in its line, twice as hard against the undead."),
    "radiant_burst": ("Radiant Burst", "A flash of first light round you: foes are hurt and blinded, you and your allies are healed."),
    "chain_lightning": ("Chain Lightning", "Lightning leaps from foe to foe, a little weaker at each jump."),
    "storm_pillars": ("Storm Pillars", "Marks three places (on your foes, else ahead of you): a pillar of storm light falls on each a moment later, stilling what it hits."),
    "crescent_wind": ("Crescent Wind", "Two crescents of wind sweep out to either side of your gaze, cutting everything in them and leaving the wounds bleeding."),
    "heavenly_dash": ("Heavenly Dash", "A dash on the wind, high and long, that cuts every foe on its way."),
    "abyssal_reap": ("Abyssal Reap", "A wide sweep of the scythe: foes are hurt and filled with dread, and a share of the harm comes back to you as health."),
    "drowning_pull": ("Drowning Pull", "Pulls every foe near you in on a black whirlpool: they are hurt, slowed and lose their breath."),
}


def lang_fragment():
    """The English strings of the weapons (the lead merges them into assets/nusmp/lang/en_us.json)."""
    out = {}
    for d in DJINN:
        for dark in (False, True):
            out["item.nusmp." + item_id(d, dark)] = NAMES[d][1 if dark else 0]
    for key, (name, desc) in ACTS.items():
        out["djinn_weapon.act." + key] = name
        out["djinn_weapon.act." + key + ".desc"] = desc
    out.update({
        "tooltip.nusmp.djinn_weapon.temp": "A temporary weapon: %s's Djinn Equip made it. It lasts as long as the Equip; your own item is kept safe and returns when it ends.",
        "tooltip.nusmp.djinn_weapon.admin": "A weapon of %s's Djinn (admin). It does not depend on a Djinn Equip.",
        "tooltip.nusmp.djinn_weapon.dark": "Black rukh: it hits harder and drinks more Magoi.",
        "tooltip.nusmp.djinn_weapon.act1": "Right-click: %s (%s%% of your Magoi, %s s)",
        "tooltip.nusmp.djinn_weapon.act2": "Sneak + right-click, Full Equip only: %s (%s%% of your Magoi, %s s)",
        "tooltip.nusmp.djinn_weapon.onhit": "Every blow: %s",
        "tooltip.nusmp.djinn_weapon.rules": "It cannot be dropped, stored or moved away from its slot; it is always back in your hand.",
        "djinn_weapon.msg.summoned": "%s is in your hand.",
        "djinn_weapon.msg.returned": "The Djinn weapon fades. Your own items are back.",
        "djinn_weapon.msg.busy": "The Djinn is gathering an Extreme Magic: the weapon is still.",
        "djinn_weapon.msg.locked": "%s opens in the Full Equip.",
        "djinn_weapon.msg.cooldown": "%s needs %s s more.",
        "djinn_weapon.msg.magoi": "Your Magoi is too low for that.",
        "djinn_weapon.msg.nothing": "There is nothing in reach to strike.",
        "djinn_weapon.cmd.nodjinn": "That player holds no Djinn: name one (weapon give <player> <djinn> [dark]).",
        "djinn_weapon.cmd.given": "Gave %s to %s.",
        "djinn_weapon.cmd.cleared": "Removed %s Djinn weapon(s) from %s and ended the loan.",
    })
    return out


# ================================================================ writing
def outputs(d, dark):
    """-> {relative path under assets/nusmp: bytes or str} for one weapon."""
    b = build(d, dark)
    iid = item_id(d, dark)
    files = {
        "weapon_meshes/%s.txt" % iid: mesh_text(d, dark, b),
        "models/item/%s.json" % iid: display_json(d, dark, b),
        "models/item/%s_icon.json" % iid: icon_model_json(d, dark),
    }
    import io
    for name, glow in ((iid, False), (iid + "_glow", True)):
        buf = io.BytesIO()
        masked_skin(d, dark, glow, b["quads"]).save(buf, "PNG", optimize=True)
        files["textures/entity/weapon/%s.png" % name] = buf.getvalue()
    buf = io.BytesIO()
    render_icon(d, dark, b).save(buf, "PNG", optimize=True)
    files["textures/item/%s.png" % iid] = buf.getvalue()
    return files, b


def all_outputs():
    out, info = {}, {}
    for d in DJINN:
        for dark in (False, True):
            f, b = outputs(d, dark)
            out.update(f)
            info[item_id(d, dark)] = b
    return out, info


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="exit 1 if a file is missing or differs")
    ap.add_argument("--sheet", help="write a contact sheet of the icons to this png")
    ap.add_argument("--lang", help="write the English lang fragment to this json file")
    args = ap.parse_args(argv)
    if args.lang:
        with open(args.lang, "w", encoding="utf-8", newline="\n") as f:
            json.dump(lang_fragment(), f, indent=2, ensure_ascii=False)
            f.write("\n")
    files, info = all_outputs()
    bad = []
    for rel, data in sorted(files.items()):
        path = os.path.join(ASSETS, *rel.split("/"))
        raw = data if isinstance(data, bytes) else data.encode("utf-8")
        if args.check:
            if not os.path.exists(path) or open(path, "rb").read() != raw:
                bad.append(rel)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(raw)
    for iid, b in info.items():
        print("%-34s apparent %.2f blocks  scale %.5f%s%s" % (iid, (b["box"][3] - b["ymin"]) * b["scale"] * THIRD_PERSON_SCALE, b["scale"],
                                                             "  mirrored" if b["mirrored"] else "", "  rewound %d" % b["fixed"] if b["fixed"] else ""))
    if args.sheet:
        sheet = Image.new("RGBA", (6 * 140, 4 * 140), (40, 40, 48, 255))
        for i, (iid, b) in enumerate(info.items()):
            d = next(x for x in DJINN if iid.startswith("djinn_weapon_" + x))
            ic = Image.open(os.path.join(ASSETS, "textures", "item", iid + ".png")).convert("RGBA").resize((128, 128), Image.NEAREST)
            sheet.paste(ic, ((i % 6) * 140 + 6, (i // 6) * 140 + 6), ic)
        sheet.save(args.sheet)
    if args.check and bad:
        print("out of date or missing:", ", ".join(bad[:20]), "..." if len(bad) > 20 else "")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
