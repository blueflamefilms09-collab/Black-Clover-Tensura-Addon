#!/usr/bin/env python3
"""
The Valkyrie Drill Lance as a REAL WEAPON ITEM (docs/vfx_remake/valkyrie_ingame.md; CLAUDE.md rules 6 and 7): a spear-class weapon in hand drawn by client/WeaponRenderer to the Genesis Demon-Slayer's standard,
built from the finished preview lance AB (tools/gen_valkyrie_lance.py: the A + B merge, a frosted striped cone in a dense foam mane with a crystal screw inside it, a glowing axis and a crystal needle).

    python3 tools/gen_valkyrie_lance.py ; python3 tools/gen_valkyrie_lance_model.py        (deterministic, idempotent)
    python3 tools/gen_weapon_models.py valkyrie_lance                                       (the hook of the weapon generator runs this file)
    python3 tools/item_preview/preview_weapons.py valkyrie_drill_lance

The mesh is split into two NAMED PARTS (docs/asset_pipeline.md rule 3: a moving part is a part of its own):
  grip    rigid: the grip, the rounded vamplate hand guard and the two ribbons trailing behind the butt (every quad whose highest point is at or under y = 8: the guard's top ring is y = 7.6)
  drill*  the part that turns: the cone, its crystal screw, the foam mane, the ribbons round the cone, the needle and the tip glow
The mesh file says `part grip` / `part drill`; client/WeaponRenderer draws the quads after `part drill` after a turn about the mesh's +y axis (the lance's axis, x = z = 0) by
angle = idle 60 degrees a second + what the holder's charge adds, up to 720 a second (book/ModeArmorValkyrieSpin.itemAngle): the client game time and the holder's use / swing time, no state, no packet.

Skins (128 x 128, the mesh uvs are in texels of that space like every weapon mesh):
  valkyrie_drill_lance.png        body (cutout, lit): the foam cards (64 x 64 at 0,0) and the flat tone swatches of the frost and the deep-blue bands (16 x 16 cells at 64..111, 0..31)
  valkyrie_drill_lance_glow.png   glow (additive, full bright, pulsing): the ribbons' veil (0,0), the tip glow (64,0), the glass tones of the crystal (cells at 0..47, 64) and the glowing axis (48, 64)
The crystal screw, the glass window of the groove, the ribbons and the tip are additive (the item draws no translucent pass): they shine and fade with the pulse.
The inventory icon (textures/item/valkyrie_drill_lance.png, 16 x 16) is a diagonal drill drawn by this script: a white and blue banded cone with a spiky white fringe, a blue grip, a cyan glint at the tip.
It also writes the model json and the icon json, the class rows (both copies of weapon_classes.json), the Better Combat attributes row and the display preset (first person).
"""
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "relics"))
import gen_valkyrie_lance as gl                                                     # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp")
PREVIEW = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
ID = "valkyrie_drill_lance"
REAL = 3.0                                                                          # metres in hand (spear class 2.8 to 3.6; 1 block = 1 m, the hand scale is 0.85)
GRIP = 0.25                                                                         # the guard sits at a quarter of the length: the hand holds there, and the size grows about it
GRIP_Y = gl.GRIP * gl.LEN["AB"]                                                     # 13.5 px: the grip point in mesh space
GRIP_SPLIT_Y = 8.0
GLOW = ("C4F4FF", 0.09, 0.55, 1.0)                                                  # the additive pass: a cool white-cyan, pulsing slowly

# ------------------------------------------------------------------ the skins
TONES = {6: [(150, 190, 225), (222, 242, 255), (255, 255, 255)],                    # layer 6 frost, 7 deep (the toon palettes of the preview)
         7: [(26, 74, 156), (44, 116, 206), (110, 178, 240)]}
GLASS = [(50, 128, 215), (110, 190, 245), (214, 244, 255)]
EMISSIVE = (205, 246, 255)
LIGHT = (-0.5, 0.88, 0.27)                                                          # the baked light of the preview (the game's own lighting adds to it)
SW = 16                                                                             # swatch cell size (texels)


def swatch_body(layer, tone):                                                       # centre of the cell of a flat band tone in the body skin
    return 64 + SW * tone + SW / 2.0, (0 if layer == 6 else SW) + SW / 2.0


def swatch_glow(kind, tone=0):                                                      # centre of the cell of a glass tone (0..2) or of the glowing axis (kind "core") in the glow skin
    return (SW * tone + SW / 2.0, 64 + SW / 2.0) if kind == "glass" else (48 + SW / 2.0, 64 + SW / 2.0)


def tone_of(n):
    hl = 0.5 + 0.5 * (n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2])
    return 2 if hl > 0.86 else 1 if hl > 0.55 else 0


def region_uv(x0, y0, u, v, size=64):                                               # a point (u, v in 0..1) of a size x size sheet placed at (x0, y0), inset half a texel
    return x0 + 0.5 + min(max(u, 0.0), 1.0) * (size - 1), y0 + 0.5 + min(max(v, 0.0), 1.0) * (size - 1)


def build_skins():
    body = np.zeros((128, 128, 4), np.uint8)
    glow = np.zeros((128, 128, 4), np.uint8)
    foam = np.asarray(Image.open(os.path.join(PREVIEW, "tex", "valk2_foam.png")).convert("RGBA"))
    veil = np.asarray(Image.open(os.path.join(PREVIEW, "tex", "valk2_veil.png")).convert("RGBA"))
    tip = np.asarray(Image.open(os.path.join(PREVIEW, "tex", "valk2_glow.png")).convert("RGBA"))
    body[0:64, 0:64] = foam
    body[..., :3][body[..., 3] < 4] = 0
    for layer in (6, 7):
        for t in range(3):
            x0, y0 = 64 + SW * t, 0 if layer == 6 else SW
            body[y0:y0 + SW, x0:x0 + SW] = (*TONES[layer][t], 255)
    glow[0:64, 0:64] = veil
    glow[0:64, 64:128] = tip
    for t in range(3):                                                              # the glass: the tone colour, translucent (additive: it only adds light)
        glow[64:64 + SW, SW * t:SW * t + SW] = (*GLASS[t], 150)
    glow[64:64 + SW, 48:48 + SW] = (*EMISSIVE, 235)
    glow[..., :3][glow[..., 3] < 4] = 0                                             # black under every transparent texel: it is drawn additively (docs/vfx_square_bug.md)
    return body, glow


# ------------------------------------------------------------------ the mesh
def mesh_quads():
    """[(part, 'body' | 'glow', pts5, normal)] in the order they are written: every quad of the rigid part first, then every quad of the drill."""
    m = gl.lance_ab(0)
    out = []
    for (layer, pts, n, rgb) in m.q:
        part = "grip" if max(p[1] for p in pts) <= GRIP_SPLIT_Y else "drill"
        n = tuple(float(c) for c in n)
        tn = tone_of(n)
        if layer == 3:                                                              # foam card: the body skin, alpha tested
            uv = [region_uv(0, 0, p[3], p[4]) for p in pts]
            out.append((part, "body", [(p[0], p[1], p[2], uv[i][0], uv[i][1]) for i, p in enumerate(pts)], n))
        elif layer in (6, 7):                                                       # frost / deep bands: a flat tone swatch of the body skin
            cx, cy = swatch_body(layer, tn)
            out.append((part, "body", [(p[0], p[1], p[2], cx, cy) for p in pts], n))
        elif layer == 4:                                                            # the ribbons: the veil sheet of the glow skin
            uv = [region_uv(0, 0, p[3], p[4]) for p in pts]
            out.append((part, "glow", [(p[0], p[1], p[2], uv[i][0], uv[i][1]) for i, p in enumerate(pts)], n))
        elif layer == 5:                                                            # the tip glow card
            uv = [region_uv(64, 0, p[3], p[4]) for p in pts]
            out.append((part, "glow", [(p[0], p[1], p[2], uv[i][0], uv[i][1]) for i, p in enumerate(pts)], n))
        elif layer == 9:                                                            # the crystal and the glass window of the groove
            cx, cy = swatch_glow("glass", tn)
            out.append((part, "glow", [(p[0], p[1], p[2], cx, cy) for p in pts], n))
        elif layer == 1:                                                            # the glowing axis
            cx, cy = swatch_glow("core")
            out.append((part, "glow", [(p[0], p[1], p[2], cx, cy) for p in pts], n))
        else:
            raise ValueError("unexpected layer %d in lance AB" % layer)
    out.sort(key=lambda q: 0 if q[0] == "grip" else 1)                              # stable: the order inside a part is the generator's
    return out


# ------------------------------------------------------------------ the inventory icon: a diagonal drill (16 x 16)
def draw_icon():
    S = 16
    img = np.zeros((S, S, 4), np.uint8)
    OUTLINE = (18, 36, 78, 255)
    WHITE, WHITE2 = (240, 250, 255), (205, 230, 250)
    BLUE, BLUE2 = (58, 128, 214), (30, 84, 170)
    x0, y0, x1, y1 = 1.6, 14.4, 14.2, 1.8                                            # grip end -> tip, pixel centres (y down)
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    ux, uy = dx / L, dy / L
    px_, py_ = -uy, ux
    cells = {}
    for yy in range(S):
        for xx in range(S):
            cx, cy = xx + 0.5, yy + 0.5
            t = ((cx - x0) * ux + (cy - y0) * uy) / L
            w = (cx - x0) * px_ + (cy - y0) * py_
            if t < -0.02 or t > 1.0:
                continue
            col = None
            if t < 0.19:                                                             # the blue grip
                if abs(w) <= 0.95:
                    col = BLUE2 if int(t * 40) % 2 == 0 else BLUE
            elif t < 0.27:                                                           # the rounded hand guard
                if abs(w) <= 2.5 - abs(t - 0.23) * 6:
                    col = WHITE2
            else:                                                                    # the cone: wide at the base, a needle at the tip, white and blue bands across it
                s = (t - 0.27) / 0.73
                half = 3.2 * (1 - s) ** 0.85 + 0.45
                if abs(w) <= half:
                    band = int((t - 0.27) * 16.5)
                    col = WHITE if band % 2 == 0 else BLUE
                    if abs(w) > half - 0.75 and band % 2 == 0:
                        col = WHITE2
            if col is not None:
                cells[(xx, yy)] = col
    # the spiky white foam fringe round the lower third of the cone: single pixels leaning back toward the grip
    for (t, side) in ((0.33, 1), (0.40, -1), (0.46, 1), (0.52, -1), (0.38, 1), (0.30, -1), (0.57, 1), (0.44, -1)):
        s = (t - 0.27) / 0.73
        half = 3.2 * (1 - s) ** 0.85 + 0.45
        w = side * (half + 0.7)
        tt = t - 0.045
        cx, cy = x0 + ux * tt * L + px_ * w, y0 + uy * tt * L + py_ * w
        xx, yy = int(cx), int(cy)
        if 0 <= xx < S and 0 <= yy < S and (xx, yy) not in cells:
            cells[(xx, yy)] = WHITE
    # the cyan glint of the tip
    for k, c in ((0.0, (150, 245, 255)), (0.045, (210, 255, 255))):
        xx, yy = int(x1 - ux * (0.2 + k * L) + 0.0), int(y1 - uy * (0.2 + k * L))
        if 0 <= xx < S and 0 <= yy < S:
            cells[(xx, yy)] = c
    xx, yy = int(x1 + ux * 0.2), int(y1 + uy * 0.2)
    if 0 <= xx < S and 0 <= yy < S:
        cells[(xx, yy)] = (170, 250, 255)
    for (xx, yy), c in cells.items():
        img[yy, xx] = (*c, 255)
    # the 1 px dark outline round everything
    solid = np.zeros((S, S), bool)
    for (xx, yy) in cells:
        solid[yy, xx] = True
    for yy in range(S):
        for xx in range(S):
            if solid[yy, xx]:
                continue
            for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = xx + ddx, yy + ddy
                if 0 <= nx < S and 0 <= ny < S and solid[ny, nx]:
                    img[yy, xx] = OUTLINE
                    break
    return img


# ------------------------------------------------------------------ placement on the icon's diagonal (tools/gen_weapon_models.py write(), for this one mesh)
def sprite_axis(icon):
    a = np.asarray(icon)
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    P = np.stack([(xs + 0.5) / S, 1 - (ys + 0.5) / S], 1)
    c = P.mean(0)
    d = np.linalg.svd(P - c)[2][0]
    if d[0] < 0:
        d = -d
    t = (P - c) @ d
    return c + d * t.min(), math.degrees(math.atan2(d[1], d[0])), t.max() - t.min()


def write_mesh(quads, icon):
    ys = [p[1] for (_, _, pts, _) in quads for p in pts]
    ymin, ymax = 0.0, max(ys)                                                       # the butt is y = 0; the trailing ribbons hang below it
    origin, angle, length = sprite_axis(icon)
    scale = length / (ymax - ymin)
    k = REAL / (0.85 * length)
    d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
    origin = origin + d * (GRIP * length) * (1 - k)                                  # grow about the grip, not the butt
    scale *= k
    lines = ["# generated by tools/gen_valkyrie_lance_model.py (%s)" % ID,
             "place %.5f %.5f %.3f %.6f %.3f" % (origin[0], origin[1], angle, scale, ymin),
             "glow %s %.2f %.2f %.2f" % GLOW,
             "orient manual",
             "gripxyz 0.000 %.3f 0.000" % GRIP_Y]
    part = None
    n = 0
    for (pt, kind, pts, nrm) in quads:
        if pt != part:
            part = pt
            lines.append("part " + ("drill" if pt == "drill" else "grip"))
        lines.append("q %d %s %.3f %.3f %.3f" % (0 if kind == "body" else 2, " ".join("%.3f %.3f %.3f %.2f %.2f" % tuple(p) for p in pts), *nrm))
        n += 1
    os.makedirs(os.path.join(ASSETS, "weapon_meshes"), exist_ok=True)
    with open(os.path.join(ASSETS, "weapon_meshes", ID + ".txt"), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    return n, ymax, (origin, angle, scale)


# ------------------------------------------------------------------ json rows (text insertion: every other row stays byte for byte)
def class_rows():
    game = os.path.join(ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8", newline="").read()
    row = '    "%s": "spear",' % ID
    if re.search(r'^    "%s": "[^"]*",' % ID, text, re.M):
        text = re.sub(r'^    "%s": "[^"]*",' % ID, row, text, flags=re.M)
    else:
        text = re.sub(r'(^    "spirit_of_boreas": "bow",)', lambda mt: mt.group(1) + "\n" + row, text, count=1, flags=re.M)
    open(game, "w", encoding="utf-8", newline="").write(text)
    tools = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")
    text = open(tools, encoding="utf-8", newline="").read()
    block = '    "%s": {\n      "class": "spear"\n    },' % ID
    if '"%s": {' % ID in text:
        text = re.sub(r'    "%s": \{\s*"class": "[^"]*"\s*\},' % ID, lambda mt: block, text)
    else:
        text = re.sub(r'(    "spirit_of_boreas": \{\s*"class": "bow"\s*\},)', lambda mt: mt.group(1) + "\n" + block, text, count=1)
    open(tools, "w", encoding="utf-8", newline="").write(text)


def write_models():
    import new197_lib as nl
    nl.write_models(ID)
    with open(os.path.join(DATA, "weapon_attributes", ID + ".json"), "w", encoding="utf-8", newline="\n") as f:      # Better Combat: a spear, a little reach
        json.dump({"parent": "bettercombat:spear", "attributes": {"range_bonus": 1.0}}, f, indent=2)
        f.write("\n")


def presets():
    import gen_spirit_blades_models as sb
    return sb.presets(ID)


def main(args=None):
    os.makedirs(os.path.join(ASSETS, "textures", "entity", "weapon"), exist_ok=True)
    icon = draw_icon()
    Image.fromarray(icon, "RGBA").save(os.path.join(ASSETS, "textures", "item", ID + ".png"))
    body, glow = build_skins()
    Image.fromarray(body, "RGBA").save(os.path.join(ASSETS, "textures", "entity", "weapon", ID + ".png"), optimize=True)
    Image.fromarray(glow, "RGBA").save(os.path.join(ASSETS, "textures", "entity", "weapon", ID + "_glow.png"), optimize=True)
    n, ymax, place = write_mesh(mesh_quads(), Image.fromarray(icon, "RGBA"))
    class_rows()
    write_models()
    e = presets()
    print("%s: %d quads, length %.1f px, place %s, preset %s (%.2f blocks)" % (ID, n, ymax, tuple(round(float(v), 4) for v in (*place[0], place[1], place[2])), e["action"], e["length_blocks"]))


if __name__ == "__main__":
    main(sys.argv[1:])
