#!/usr/bin/env python3
"""
Star V2, IN-GAME PATH preview.  Unlike star_new_preview.py (the preview round: quad generators written in Python), this runs the REAL layer classes of the game through the Java preview:

    BEFORE      ElementIdentityLayer (STAR_MAGIC: what the Star book drew for every page) / AuraLayer (the Star Shield aura): the code before the change = what vfx.element.star = V1 draws for the new shapes
    NEW         StarMagicLayer (the code that ships: textures stv_*, A anime cel mixed with B volumetric glow)

    python tools/gen_star_v2_textures.py            # once: the game textures stv_*.png
    python tools/vfx_preview/star_ingame.py         # needs JDK 21, numpy, Pillow, like preview.py
    python tools/vfx_preview/star_ingame.py extras  # only the V1 / reduced-flashes sheet

Output (docs/previews/vfx_remake/star/):
  star_ingame.png            BEFORE | NEW at three moments, every effect, same camera
  star_ingame_extras.png     the V1 option (old beam), reduced flashes, a far star, a crowd of stars
  star_ingame_budget.json    peak quads of every tick of every effect, old layer and new look, full detail and lowest detail; the prop painter's quads

HONEST LIMITS.  The placed stars and the Scutum pane are PROPS drawn by client.prop.StarPropPainter (PoseStack and render types: it cannot run headless), so their quads here are MIRRORED in
this file from the same numbers the painter uses (node(), pane() below); everything else is the real layer. Headless software rasteriser, painter's-order sort instead of a depth buffer, no bloom, no
world light, no depth against blocks, never seen in a game. The palette is a placeholder: no canon still of Star Magic was available.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402

ROOT = pv.ROOT
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "star")
CW, CH = 480, 360
GOLD, BLUE = 0xFFE08A, 0xBFDFFF
TEXP = "textures/particle/stv_%s.png"

# key, title, baseline (BEFORE) scene, [(scene, tick offset)], tile ticks (the tick of the FIRST part), what the new look is
ROWS = [
    ("node", "STAR_NODE  Star Seed: a comet from the hand, a pop, the placed star [A cel; the star is a prop]", "star_node", [("star_v2_node", 0)], [3, 8, 13]),
    ("lines", "STAR_LINK x4  constellation lines between four stars [A cel lines, B soft ribbon]", "star_lines", [("star_v2_lines", 0)], [2, 5, 12]),
    ("conj", "STAR_CONJ  Conjunction: leave, line, arrive; the star used bursts above [B glow]", "star_conjunction", [("star_v2_conj", 0)], [3, 6, 11]),
    ("scutum", "STAR_LINK x4 + the pane prop  Quartile Scutum [A cel]", "star_scutum", [("star_v2_scutum", 0)], [5, 12, 20]),
    ("pane_hit", "STAR_PANE  a hit on the pane [A cel ring + crack]", "star_scutum", [("star_v2_pane_hit", 0)], [1, 5, 10]),
    ("pane_break", "STAR_PANE  the pane breaks: shards [A cel]", "star_scutum", [("star_v2_pane_break", 0)], [1, 6, 12]),
    ("hasta", "STAR_LINK x4 charge, then STAR_LANCE  Quartile Hasta: A spear head + B laser tail (the stars are used up)", "star_hasta", [("star_v2_hasta_links", 0), ("star_v2_hasta", 9)], [6, 11, 15]),
    ("whip", "STAR_WHIP x4  Quartile Flagellum: the whip lashes through four stars [A cel]", "star_flagellum", [("star_v2_whip", 0)], [2, 6, 11]),
    ("dome", "STAR_DOME  Black Neverland: sigil, ring, rim stars, dust [B glow]", None, [("star_v2_dome", 0)], [5, 25, 60]),
]


# ------------------------------------------------------------------------------------------------ scenes and the layer's quads
def load_scenes():
    out = {}
    for fn in ("star_v2.json", "star_baseline.json"):
        with open(os.path.join(HERE, "scenes.d", fn), encoding="utf-8") as f:
            out.update({k: v for k, v in json.load(f).items() if not k.startswith("_")})
    return out


def run_quads(sc, classes, ages, tag, **over):
    """the real layer's quads of a scene (its 'also' activations included) at the given ticks: {age: [quad dicts]}"""
    s = dict(sc)
    s.update(over)
    s["ages"] = list(ages)
    acts = [s] + [dict(s, **e) for e in s.get("also", [])]
    out = {a: [] for a in ages}
    for k, a in enumerate(acts):
        a = dict(a)
        a.pop("also", None)
        data = pv.run_java(a, classes, os.path.join(pv.BUILD, f"stv_{tag}_{k}.json"))
        for fr in data["frames"]:
            out[int(fr["age"])].extend(fr["quads"])
    return out


# ------------------------------------------------------------------------------------------------ the PROP PAINTER, mirrored (client.prop.StarPropPainter)
def _col(alpha, rgb):
    return (int(round(max(0, min(1, alpha)) * 255)) << 24) | rgb


def _quad(tex, blend, pts, cam, color, flip=False):
    uv = [(0, 1), (1, 1), (1, 0), (0, 0)]
    return {"t": TEXP % tex, "b": blend, "o": 0 if blend == "ALPHA" else 3,
            "v": [[pts[i][0] - cam[0], pts[i][1] - cam[1], pts[i][2] - cam[2], uv[i][0], uv[i][1], color] for i in range(4)]}


def _bill(tex, blend, c, half, roll, cam, r, u, color):
    cs, sn = math.cos(roll) * half, math.sin(roll) * half
    rx = r * cs + u * sn
    ry = u * cs - r * sn
    c = np.array(c, float)
    return _quad(tex, blend, [c - rx - ry, c + rx - ry, c + rx + ry, c - rx + ry], cam, color)


def _clamp(x):
    return max(0.0, min(1.0, x))


def _twinkle(age, rate, amount, reduced):
    return 1 - amount * 0.5 if reduced else 1 - amount * 0.5 + amount * 0.5 * math.sin(age * rate)


def node(x, y, z, drop, age, cam, r, u, reduced=False, dist=0.0):
    """StarPropPainter.node: 7 quads near, 2 far (B bloom star + halo)"""
    a = min(_clamp(age / 6.0), _clamp((800 - age) / 24.0))
    c = (x, y, z)
    q = []
    if dist > 24.0:
        q.append(_bill("b_star", "ADD", c, 1.1, 0, cam, r, u, _col(0.95 * a, 0xFFFFFF)))
        q.append(_bill("b_glow", "ADD", c, 1.3, 0, cam, r, u, _col(0.4 * a, GOLD)))
        return q
    step = 0 if reduced else int(age / 2.0) & 1
    s = (0.62 if step == 0 else 0.55) * (0.85 + 0.15 * _clamp(age / 6.0))
    tw = _twinkle(age, 0.5, 0.3, reduced)
    q.append(_bill("b_glow", "ADD", c, 1.15, 0, cam, r, u, _col(0.45 * a * tw, GOLD)))
    q.append(_bill("a_star4", "ALPHA", c, s, 0, cam, r, u, _col(a, 0xFFFFFF)))
    roll = 0 if reduced else age * 0.06
    q.append(_bill("a_spark", "ALPHA", (x + 0.38, y + 0.30, z), 0.20 * tw, roll, cam, r, u, _col(0.9 * a, 0xFFFFFF)))
    q.append(_bill("a_spark", "ALPHA", (x - 0.32, y - 0.26, z), 0.16 * tw, -roll, cam, r, u, _col(0.8 * a, 0xFFFFFF)))
    if drop >= 1.0:
        top, bot = np.array([x, y - 0.2, z]), np.array([x, y - drop + 0.1, z])
        d = bot - top
        mid = (top + bot) / 2 - np.array(cam)
        side = np.cross(d, mid)
        side = side / (np.linalg.norm(side) + 1e-9) * 0.07
        q.append(_quad("b_glow", "ADD", [top - side, bot - side, bot + side, top + side], cam, _col(0.22 * a, BLUE)))
        rot = math.radians(0 if reduced else age * 1.2)
        cr, sr = math.cos(rot) * 1.5, math.sin(rot) * 1.5
        g = np.array([x, y - drop + 0.04, z])
        q.append(_quad("a_sigil", "ALPHA", [g + (-cr + sr, 0, -sr - cr), g + (cr + sr, 0, sr - cr), g + (cr - sr, 0, sr + cr), g + (-cr - sr, 0, -sr + cr)], cam, _col(0.85 * a, 0xFFFFFF)))
    return q


def pane(center, normal_yaw, w, h, pct, age, cam, r, u, reduced=False):
    """StarPropPainter.pane: the glare pane (front and back) and a four-point star on each corner"""
    a = min(_clamp(age / 5.0), _clamp((100 - age) / 10.0))
    k = a * (0.45 + 0.55 * pct / 100.0)
    side = np.array([-math.sin(normal_yaw), 0, math.cos(normal_yaw)])
    up = np.array([0, 1.0, 0])
    c = np.array(center, float)
    hw, hh = w / 2, h / 2
    q = [_quad("a_pane", "ALPHA", [c - side * hw - up * hh, c + side * hw - up * hh, c + side * hw + up * hh, c - side * hw + up * hh], cam, _col(0.9 * k, 0xFFFFFF))]
    tw = _twinkle(age, 0.4, 0.3, reduced)
    cs = 0.55 * tw * min(1.0, 0.6 + hw * 0.1)
    for i in range(4):
        p = c + side * (-hw if i & 1 == 0 else hw) + up * (-hh if i & 2 == 0 else hh)
        q.append(_bill("a_star4", "ALPHA", p, cs, 0, cam, r, u, _col(a, 0xFFFFFF)))
    return q


def prop_quads(sc, tick, reduced=False, detail_far=False):
    props = sc.get("props")
    if not props:
        return []
    cam = np.array(sc["cam"], float)
    f, r, u = pv.basis(sc["cam"], sc["target"])
    out = []
    for x, y, z, drop, a0, a1 in props.get("nodes", []):
        if a0 <= tick < a1:
            dist = 30.0 if detail_far else 0.0
            out.extend(node(x, y, z, drop, tick - a0, cam, r, u, reduced, dist))
    p = props.get("pane")
    if p:
        out.extend(pane(p["center"], p["yaw"], p["w"], p["h"], p["pct"], tick, cam, r, u, reduced))
    return out


# ------------------------------------------------------------------------------------------------ tiles
def tile(quads, sc, label, sub="", w=None, h=None):
    if w:
        pv.W, pv.H = w, h
    img = pv.background(sc, sc["cam"], sc["target"])
    img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
    img = pv.raster(img, quads, sc["cam"], sc["target"])
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, t.width, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, t.height - 13), sub, fill=(255, 235, 150))
    pv.W, pv.H = CW, CH
    return t


def nothing_tile(sc, text):
    pv.W, pv.H = CW, CH
    img = pv.background(sc, sc["cam"], sc["target"])
    img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, t.width, 14], fill=(14, 16, 20))
    d.text((5, 2), text, fill=(255, 255, 255))
    return t


def frame_quads(scenes, parts, T, classes, tag, mirror_reduced=False, **over):
    """all quads of one tile: the layer's quads of every part at its own tick, plus the mirrored props of the first scene"""
    quads = []
    for name, off in parts:
        age = T - off
        sc = scenes[name]
        if age < 0 or age >= sc["duration"]:
            continue
        quads.extend(run_quads(sc, classes, [age], f"{tag}_{name}_{age}", **over)[age])
    quads.extend(prop_quads(scenes[parts[0][0]], T, mirror_reduced))
    return quads


def peak(scenes, parts, classes, detail, tag, **over):
    """peak quads over every tick of the effect's life (the layer's quads only; the painter's are counted in the doc)"""
    worst = 0
    for name, off in parts:
        sc = scenes[name]
        ages = list(range(0, int(sc["duration"]) + 1))
        s = dict(sc)
        s["detail"] = detail
        s.update(over)
        per = {a: 0 for a in ages}
        acts = [s] + [dict(s, **e) for e in s.get("also", [])]
        for k, a in enumerate(acts):
            a = dict(a)
            a.pop("also", None)
            a["ages"] = ages
            data = pv.run_java(a, classes, os.path.join(pv.BUILD, f"stv_peak_{tag}_{name}_{k}.json"))
            for fr in data["frames"]:
                per[int(fr["age"])] += len(fr["quads"])
        worst = max(worst, max(per.values()))
    return worst


def sheet_all(scenes, classes):
    rows, budget = [], {}
    for key, title, base, parts, ticks in ROWS:
        main = scenes[parts[0][0]]
        tiles = []
        if base:
            bsc = scenes[base]
            fr = run_quads(bsc, classes, [bsc["ages"][1]], f"before_{key}")[bsc["ages"][1]]
            before_n = len(fr)
            tiles.append(tile(fr, main, f"BEFORE {bsc['layer']} {bsc['shape']}  t={bsc['ages'][1]}  {before_n} quads"))
        else:
            tiles.append(nothing_tile(main, "BEFORE: nothing (a new effect)"))
        for T in ticks:
            q = frame_quads(scenes, parts, T, classes, f"new_{key}")
            tiles.append(tile(q, main, f"NEW StarMagicLayer + props  t={T}  {len(q)} quads"))
        rows.append((title, tiles))
        b = {"before_scene": base}
        for det in (1.0, 0.5):
            b[f"new_layer_peak_quads_detail_{det}"] = peak(scenes, parts, classes, det, f"{key}_{det}")
        v1 = peak(scenes, parts, classes, 1.0, f"{key}_v1", star_style="V1")
        b["v1_peak_quads"] = v1
        props = scenes[parts[0][0]].get("props", {})
        b["prop_painter_quads_full"] = len(props.get("nodes", [])) * 7 + (6 if props.get("pane") else 0)
        budget[key] = b
        print(key, b)
    cols = max(len(t) for _, t in rows)
    sheet = Image.new("RGB", (CW * cols, CH * len(rows) + 30), (16, 18, 22))
    ImageDraw.Draw(sheet).text((8, 8), "STAR in the game path:  BEFORE (real old layer) | NEW (real StarMagicLayer; the placed stars and the pane are props: mirrored from the painter's numbers)   same camera; software render, no bloom, not seen in a game; PLACEHOLDER palette", fill=(255, 255, 255))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(sheet).text((6, 30 + i * CH + CH - 28), title, fill=(180, 255, 180))
    os.makedirs(OUTDIR, exist_ok=True)
    sheet.save(os.path.join(OUTDIR, "star_ingame.png"))
    with open(os.path.join(OUTDIR, "star_ingame_budget.json"), "w", encoding="utf-8") as f:
        json.dump(budget, f, indent=1)
    print("->", os.path.join(OUTDIR, "star_ingame.png"))


def extras(scenes, classes):
    """the V1 option (the new shapes drawn as the old beam), reduced flashes, a far star, a crowd of stars (the per-frame ceiling)"""
    rows = []
    cases = [
        ("conj_v1", "STAR_CONJ with vfx.element.star = V1 (the old starlight beam)  <->  V2 on the left", "star_v2_conj", 1, {"star_style": "V1"}, False),
        ("lance_v1", "STAR_LANCE with V1  <->  V2 on the left", "star_v2_hasta", 1, {"star_style": "V1"}, False),
        ("conj_reduced", "STAR_CONJ, reduced flashes ON at tick 3 (no speed lines, no lens flare, no hard star)  <->  normal on the left", "star_v2_conj", 0, {"reduced": 1}, True),
        ("lance_reduced", "STAR_LANCE, reduced flashes ON at tick 2  <->  normal on the left", "star_v2_hasta", 0, {"reduced": 1}, True),
        ("node_reduced", "the placed star with reduced flashes (no blinking, no twinkle, no spin)  <->  normal on the left", "star_v2_lines", 1, {"reduced": 1}, True),
    ]
    for key, title, name, ai, extra, red in cases:
        sc = scenes[name]
        t = sc["ages"][ai]
        norm = frame_quads(scenes, [(name, 0)], t, classes, f"ex_{key}_n")
        alt = frame_quads(scenes, [(name, 0)], t, classes, f"ex_{key}_x", mirror_reduced=red, **extra)
        rows.append((title, [tile(norm, sc, f"V2 normal  t={t}  {len(norm)} quads"), tile(alt, sc, f"{key}  t={t}  {len(alt)} quads")]))
    # a far star (the B bloom sprite) next to a near one, and 40 stars in view (the frame ceiling: 48 full, the rest 2 quads)
    sc = dict(scenes["star_v2_lines"])
    cam = np.array(sc["cam"], float)
    f, r, u = pv.basis(sc["cam"], sc["target"])
    near, far, crowd = [], [], []
    for i, p in enumerate([(-3, 4, 7), (2.5, 4.6, 6), (3.5, 3.2, 10.5), (-2, 2.8, 11)]):
        near.extend(node(p[0], p[1], p[2], p[1], 30, cam, r, u))
        far.extend(node(p[0], p[1], p[2], p[1], 30, cam, r, u, dist=40.0))
    for i in range(40):
        x, y, z = -9 + (i % 10) * 2.0, 2.0 + (i // 10) * 1.3, 6 + (i // 10) * 1.2
        crowd.extend(node(x, y, z, y, 30, cam, r, u, dist=0.0 if i < 24 else 40.0))
    rows.append(("four stars near (7 quads each)  |  the same four far (2 quads: B bloom star + halo)  |  a crowd of 40: the first 24 in full, the rest as far sprites",
                 [tile(near, sc, f"near  {len(near)} quads"), tile(far, sc, f"far  {len(far)} quads"), tile(crowd, sc, f"crowd of 40  {len(crowd)} quads")]))
    cols = max(len(t) for _, t in rows)
    sheet = Image.new("RGB", (CW * cols, CH * len(rows)), (16, 18, 22))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, i * CH))
        ImageDraw.Draw(sheet).text((6, i * CH + CH - 28), title, fill=(180, 255, 180))
    sheet.save(os.path.join(OUTDIR, "star_ingame_extras.png"))
    print("->", os.path.join(OUTDIR, "star_ingame_extras.png"))


def main():
    pv.W, pv.H = CW, CH
    scenes = load_scenes()
    os.makedirs(pv.BUILD, exist_ok=True)
    os.makedirs(OUTDIR, exist_ok=True)
    classes = pv.build(["StarMagicLayer", "ElementIdentityLayer", "AuraLayer"])
    what = sys.argv[1:] or ["all", "extras"]
    if "all" in what:
        sheet_all(scenes, classes)
    if "extras" in what:
        extras(scenes, classes)


if __name__ == "__main__":
    main()
