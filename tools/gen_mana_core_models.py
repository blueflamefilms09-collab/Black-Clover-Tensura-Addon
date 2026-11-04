"""3.51.0: the rank Mana Cores (E, D, C, B, A, AA, S, SS x Standard / Beast / Ancient = 24 items, ids core_<rank>_<variant>) as 3D orbs.

The owner's 7 scanned GLBs (~950k triangles each) are far too heavy for a jar, so each one is reduced to a 24 x 12 UV sphere (288 quads = 576 triangles)
and its look is baked into a 256 x 128 equirect skin: for every texel the original surface point that lies furthest out along that direction is found
(the outer shell, not the inner star / veins) and its colour is read through the GLB's own UV map. The per-vertex radius of that shell (smoothed) shapes the
rough stone and the bulged purple orb. SS has no GLB: it re-uses the golden S orb, recoloured to crimson with gold light left in it.

    pip install trimesh numpy pillow scipy
    python3 -B tools/gen_mana_core_models.py [--src DIR_WITH_THE_GLBS] [--sheet /tmp/claude-0/out/mana_core_sheet.png]

Writes (all under src/main/resources/assets/nusmp/, nothing existing is touched):
  weapon_meshes/core_<rank>_<variant>.txt        the orb, in the dialect client/WeaponRenderer reads (placed on the item's centre, `orient manual`)
  textures/entity/weapon/core_<rank>_<variant>.png  the 256x128 skin;  ..._glow.png  the additive glow skin (D and up)
  textures/item/core_<rank>_<variant>.png        the 16x16 inventory icon
  models/item/core_<rank>_<variant>.json         the builtin/entity model (display transforms) and ..._icon.json (the flat icon model)
and appends the 24 ids to weapon_classes.json ("handitem"). Re-running rewrites the same files.

Per orb in game: 288 quads body (+288 additive glow quads from D up) = at most 576 quads / 2304 vertices per frame, no allocation (WeaponRenderer.emit reuses
two scratch vectors). Variants: standard = the original colours; beast = warmer, veined (dark branching veins); ancient = cooler, a ring of runes on the equator.
"""
import argparse
import colorsys
import glob
import json
import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DEFAULT_SRC = "/root/.claude/uploads/2c8ee7c0-aec7-5d43-b651-54cec528bb71"

SEG, RING = 24, 12            # longitude / latitude segments of the low-poly sphere
TW, TH = 256, 128             # skin size
R = 10.0                      # mesh radius in mesh units; place scale 0.02 -> a 0.2-block orb
VARIANTS = ("standard", "beast", "ancient")

# rank: (glb prefix or None, glow strength 0..1, glow rgb, glow speed, how much of the baked radius shapes the mesh)
RANKS = {
    "e":  ("acec4348", 0.0, "FFFFFF", 0.05, 1.0),
    "d":  ("c8a97a07", 0.30, "E8F4FF", 0.05, 0.5),
    "c":  ("5fa3618d", 0.45, "9FDFFF", 0.06, 0.3),
    "b":  ("6b98ae30", 0.85, "5FA8FF", 0.10, 0.3),
    "a":  ("8a6bc150", 0.85, "FF8A3A", 0.09, 0.8),
    "aa": ("11ccd3c8", 0.75, "B07CFF", 0.07, 0.6),
    "s":  ("42149775", 0.90, "FFD86B", 0.06, 0.2),
    "ss": (None,       1.00, "FF5A4A", 0.08, 0.2),      # the golden mesh, recoloured
}
ORDER = ("e", "d", "c", "b", "a", "aa", "s", "ss")


def log(*a):
    print(*a, flush=True)


# ================================================================ bake the GLB into a skin + a radius field
def directions(w, h):
    """Unit directions (h, w, 3) of an equirect grid, y up: theta from the +y pole, phi round."""
    th = (np.arange(h) + 0.5) / h * math.pi
    ph = (np.arange(w) + 0.5) / w * 2 * math.pi
    st, ct = np.sin(th)[:, None], np.cos(th)[:, None]
    return np.stack([st * np.cos(ph)[None, :], ct * np.ones((1, w)), st * np.sin(ph)[None, :]], axis=-1)


def shell(tree, dirs_o, rad, col, q, k=16, keep=0.96):
    """For query directions q (n,3): the colour and the radius of the OUTER shell point nearest each direction."""
    _, idx = tree.query(q, k=k)
    r = rad[idx]
    mx = r.max(axis=1, keepdims=True)
    w = (r >= mx * keep).astype(np.float64)
    cnt = w.sum(axis=1, keepdims=True)
    c = (col[idx] * w[..., None]).sum(axis=1) / cnt
    return c, (r * w).sum(axis=1) / cnt[:, 0]


def bake_glb(path):
    import trimesh
    from scipy.spatial import cKDTree
    g = list(trimesh.load(path, force="scene").geometry.values())[0]
    c = (g.bounds[0] + g.bounds[1]) / 2
    v = g.vertices - c
    rad = np.linalg.norm(v, axis=1)
    d = v / np.maximum(rad, 1e-9)[:, None]
    col = np.asarray(g.visual.to_color().vertex_colors, np.float64)[:, :3] / 255.0
    tree = cKDTree(d)
    dirs = directions(TW, TH).reshape(-1, 3)
    c_t, _ = shell(tree, d, rad, col, dirs)
    skin = c_t.reshape(TH, TW, 3)
    # the mesh vertices: (RING+1) x SEG, theta / phi at the vertices themselves
    th = np.arange(RING + 1) / RING * math.pi
    ph = np.arange(SEG) / SEG * 2 * math.pi
    vd = np.stack([np.sin(th)[:, None] * np.cos(ph)[None, :], np.cos(th)[:, None] * np.ones((1, SEG)), np.sin(th)[:, None] * np.sin(ph)[None, :]], -1).reshape(-1, 3)
    _, r_v = shell(tree, d, rad, col, vd, k=48, keep=0.0)           # mean radius over a wider patch = a smoothed field
    r_v = r_v.reshape(RING + 1, SEG)
    r_v[:, 0] = r_v[:, 0]                                            # the seam column is the same as column SEG (wrapped in the mesh writer)
    r_v[0, :] = r_v[0, :].mean()
    r_v[-1, :] = r_v[-1, :].mean()
    r_v = r_v / np.median(r_v)
    return skin, r_v


# ================================================================ recolour (variants, SS)
def rgb_to_hsv(a):
    mx, mn = a.max(-1), a.min(-1)
    df = mx - mn
    h = np.zeros_like(mx)
    m = df > 1e-9
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    h = np.where(m & (mx == r), ((g - b) / np.where(m, df, 1)) % 6, h)
    h = np.where(m & (mx == g), (b - r) / np.where(m, df, 1) + 2, h)
    h = np.where(m & (mx == b), (r - g) / np.where(m, df, 1) + 4, h)
    return np.stack([h / 6 % 1.0, np.where(mx > 1e-9, df / np.where(mx > 1e-9, mx, 1), 0), mx], -1)


def hsv_to_rgb(a):
    h, s, v = a[..., 0], a[..., 1], a[..., 2]
    i = np.floor(h * 6).astype(int) % 6
    f = h * 6 - np.floor(h * 6)
    p, q, t = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    out = np.zeros(a.shape)
    for k, (rr, gg, bb) in enumerate([(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)]):
        m = i == k
        out[..., 0] = np.where(m, rr, out[..., 0])
        out[..., 1] = np.where(m, gg, out[..., 1])
        out[..., 2] = np.where(m, bb, out[..., 2])
    return out


def shift(img, dh=0.0, ds=1.0, dv=1.0):
    h = rgb_to_hsv(img)
    h[..., 0] = (h[..., 0] + dh) % 1.0
    h[..., 1] = np.clip(h[..., 1] * ds, 0, 1)
    h[..., 2] = np.clip(h[..., 2] * dv, 0, 1)
    return hsv_to_rgb(h)


def crimson_gold(img):
    """SS: the golden orb recoloured: dark crimson body, the bright gold light kept."""
    lum = img @ np.array([0.299, 0.587, 0.114])
    body = np.stack([0.10 + 0.55 * lum, 0.01 + 0.05 * lum, 0.02 + 0.07 * lum], -1)
    k = np.clip((lum - 0.80) / 0.12, 0, 1)[..., None]
    gold = np.clip(img * np.array([1.0, 0.85, 0.55]), 0, 1)
    return np.clip(body * (1 - k) + gold * k, 0, 1)


def rnd(seed):
    return np.random.RandomState(seed)


def veins(img, seed):
    """Beast: dark, slightly red branching veins drawn as random walks that wrap round the equirect."""
    rs = rnd(seed)
    out = img.copy()
    mask = np.zeros((TH, TW))
    for _ in range(14):
        x, y = rs.uniform(0, TW), rs.uniform(10, TH - 10)
        ang = rs.uniform(0, 2 * math.pi)
        for step in range(rs.randint(40, 90)):
            ang += rs.normal(0, 0.35)
            x = (x + math.cos(ang)) % TW
            y = min(TH - 2, max(1, y + math.sin(ang) * 0.8))
            mask[int(y), int(x)] = 1.0
            if rs.rand() < 0.05:                                    # a branch
                bx, by, ba = x, y, ang + rs.choice([-0.9, 0.9])
                for _ in range(rs.randint(6, 18)):
                    ba += rs.normal(0, 0.4)
                    bx = (bx + math.cos(ba)) % TW
                    by = min(TH - 2, max(1, by + math.sin(ba) * 0.8))
                    mask[int(by), int(bx)] = 0.8
    soft = mask.copy()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        soft = np.maximum(soft, 0.35 * np.roll(np.roll(mask, dx, 1), dy, 0))
    vein = np.array([0.30, 0.04, 0.03])
    return out * (1 - 0.65 * soft[..., None]) + vein * 0.65 * soft[..., None]


def runes(img, seed):
    """Ancient: a ring of rune ticks on the equator (two thin bands with glyph-like strokes between), pale gold."""
    rs = rnd(seed)
    out = img.copy()
    y0 = TH // 2
    gold = np.array([1.0, 0.86, 0.5])
    for x in range(TW):
        out[y0 - 9, x] = out[y0 - 9, x] * 0.4 + gold * 0.6
        out[y0 + 9, x] = out[y0 + 9, x] * 0.4 + gold * 0.6
    n = 16
    for g in range(n):
        x = int((g + 0.5) / n * TW)
        kind = rs.randint(4)
        for dy in range(-6, 7):
            if kind == 0 or abs(dy) <= 1 and kind == 3:
                out[y0 + dy, x] = gold
            if kind == 1 and dy % 2 == 0:
                out[y0 + dy, (x + (3 if dy > 0 else -3)) % TW] = gold
                out[y0 + dy, x] = gold
            if kind == 2:
                out[y0 + dy, (x + dy // 2) % TW] = gold
        for dx in range(-3, 4):
            if kind in (0, 3):
                out[y0 + (6 if kind == 0 else 0), (x + dx) % TW] = gold
            if kind == 1:
                out[y0 - 5, (x + dx) % TW] = gold
    return out


def variant(rank, name, base, seed):
    if name == "standard":
        return base
    if name == "beast":
        return np.clip(veins(shift(base, dh=0.02, ds=1.05, dv=1.0) * np.array([1.10, 0.96, 0.82]), seed), 0, 1)
    return np.clip(runes(shift(base, dh=-0.07, ds=1.0, dv=0.98) * np.array([0.92, 1.0, 1.10]), seed), 0, 1)


def glow_map(skin, strength):
    """The additive glow skin: the bright, saturated parts of the orb (the rest black), scaled by the rank's strength."""
    lum = skin @ np.array([0.299, 0.587, 0.114])
    sat = rgb_to_hsv(skin)[..., 1]
    k = np.clip((lum - 0.35) / 0.4, 0, 1) * (0.5 + 0.5 * sat)
    return np.clip(skin * k[..., None] * strength, 0, 1)


# ================================================================ mesh
def mesh_text(cid, rank, rfield, glow):
    _, gstr, grgb, gspeed, disp = RANKS[rank]
    rr = 1.0 + (np.clip(rfield, 0.8, 1.08) - 1.0) * disp
    rr = rr * R
    verts = np.zeros((RING + 1, SEG + 1, 3))
    nrm = np.zeros_like(verts)
    for i in range(RING + 1):
        th = math.pi * i / RING
        for j in range(SEG + 1):
            ph = 2 * math.pi * (j % SEG) / SEG
            d = np.array([math.sin(th) * math.cos(ph), math.cos(th), math.sin(th) * math.sin(ph)])
            verts[i, j] = d * rr[i, j % SEG]
            nrm[i, j] = d
    out = ["# generated by tools/gen_mana_core_models.py (%s)" % cid,
           "place 0.50000 0.50000 90.000 0.020000 0.000",
           "orient manual"]
    if glow:
        out.append("glow %s %.2f %.2f %.2f" % (grgb, gspeed, 0.35, 1.0))

    def quads(layer):
        for i in range(RING):
            for j in range(SEG):
                cs = [(i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)]
                parts = ["qn", str(layer)]
                for (a, b) in cs:
                    x, y, z = verts[a, b]
                    nx, ny, nz = nrm[a, b]
                    u, v = 128.0 * b / SEG, 128.0 * a / RING
                    parts += ["%.3f" % x, "%.3f" % y, "%.3f" % z, "%.2f" % u, "%.2f" % v, "%.2f" % nx, "%.2f" % ny, "%.2f" % nz]
                out.append(" ".join(parts))
    quads(0)
    if glow:
        quads(2)
    return "\n".join(out) + "\n"


# ================================================================ icon, json, preview
def render_orb(skin, size, light=(-0.45, 0.6, 0.65), spin=0.0):
    """Shade the equirect skin on a sphere (orthographic), RGBA float (size,size,4)."""
    ys, xs = np.mgrid[0:size, 0:size]
    px = (xs + 0.5) / size * 2 - 1
    py = 1 - (ys + 0.5) / size * 2
    rr = px * px + py * py
    inside = rr <= 1.0
    pz = np.sqrt(np.clip(1 - rr, 0, 1))
    n = np.stack([px, py, pz], -1)
    ph = (np.arctan2(pz, px) + spin) % (2 * math.pi)                # the +z hemisphere faces us
    th = np.arccos(np.clip(py, -1, 1))
    u = np.clip((ph / (2 * math.pi) * TW).astype(int), 0, TW - 1)
    v = np.clip((th / math.pi * TH).astype(int), 0, TH - 1)
    col = skin[v, u]
    L = np.array(light) / np.linalg.norm(light)
    lam = np.clip(n @ L, 0, 1)
    rim = np.clip(1 - pz, 0, 1) ** 2
    shade = 0.45 + 0.75 * lam
    img = np.clip(col * shade[..., None] + rim[..., None] * 0.10, 0, 1)
    spec = np.clip(n @ (L + np.array([0, 0, 1.0])) / np.linalg.norm(L + np.array([0, 0, 1.0])), 0, 1) ** 40
    img = np.clip(img + spec[..., None] * 0.35, 0, 1)
    a = inside.astype(np.float64)[..., None]
    return np.concatenate([img, a], -1)


def to_img(arr):
    return Image.fromarray((np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA" if arr.shape[-1] == 4 else "RGB")


def icon(skin):
    big = to_img(render_orb(skin, 64))
    return big.resize((16, 16), Image.LANCZOS)


DISPLAY = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}


def write(path, data, mode="w"):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, mode) as f:
        f.write(data)


def add_classes(ids):
    path = os.path.join(ASSETS, "weapon_classes.json")
    text = open(path).read()
    data = json.loads(text)
    new = [i for i in ids if i not in data["weapons"]]
    if not new:
        return 0
    # append-only textual edit: keeps the file's layout, adds the ids just before the closing brace of "weapons"
    key = text.index('"weapons"')
    end = text.index("}", key)
    body = text[key:end].rstrip()
    ins = "".join(',\n    "%s": "handitem"' % i for i in new)
    write(path, text[:key] + body + ins + "\n  " + text[end:])
    json.loads(open(path).read())                                   # still valid
    return len(new)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default=DEFAULT_SRC)
    ap.add_argument("--sheet", default="")
    args = ap.parse_args()
    baked = {}
    for rank in ORDER:
        pre = RANKS[rank][0]
        if pre is None:
            continue
        files = glob.glob(os.path.join(args.src, pre + "*.glb"))
        if not files:
            sys.exit("missing GLB %s* in %s" % (pre, args.src))
        log("bake", rank, os.path.basename(files[0]))
        baked[rank] = bake_glb(files[0])
    s_skin, s_r = baked["s"]
    baked["ss"] = (crimson_gold(s_skin), s_r)
    ids, sheet = [], {}
    for ri, rank in enumerate(ORDER):
        skin0, rfield = baked[rank]
        for vi, name in enumerate(VARIANTS):
            cid = "core_%s_%s" % (rank, name)
            ids.append(cid)
            skin = variant(rank, name, skin0, 100 * ri + vi)
            gstr = RANKS[rank][1]
            os.makedirs(os.path.join(ASSETS, "textures", "entity", "weapon"), exist_ok=True)
            to_img(skin).save(os.path.join(ASSETS, "textures", "entity", "weapon", cid + ".png"), optimize=True)
            if gstr > 0:
                to_img(glow_map(skin, gstr)).save(os.path.join(ASSETS, "textures", "entity", "weapon", cid + "_glow.png"), optimize=True)
            write(os.path.join(ASSETS, "weapon_meshes", cid + ".txt"), mesh_text(cid, rank, rfield, gstr > 0))
            os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
            icon(skin).save(os.path.join(ASSETS, "textures", "item", cid + ".png"), optimize=True)
            m = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + cid}, "display": DISPLAY}
            write(os.path.join(ASSETS, "models", "item", cid + ".json"), json.dumps(m, indent=2) + "\n")
            write(os.path.join(ASSETS, "models", "item", cid + "_icon.json"),
                  json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + cid}}, indent=2) + "\n")
            sheet[cid] = (skin, glow_map(skin, gstr) if gstr > 0 else None, RANKS[rank][2])
    log("classes added:", add_classes(ids))
    if args.sheet:
        cell = 160
        cols = 6
        img = Image.new("RGB", (cols * cell, 4 * cell), (96, 96, 100))
        order = [("core_%s_%s" % (r, v)) for r in ORDER for v in VARIANTS]
        for i, cid in enumerate(order):
            skin, glow, grgb = sheet[cid]
            o = render_orb(skin, 128, spin=0.3)
            rgb = o[..., :3].copy()
            if glow is not None:                                   # the additive glow pass as the game does it, at full pulse
                g = render_orb(glow, 128, light=(0, 0, 1), spin=0.3)[..., :3]
                rgb = np.clip(rgb + g, 0, 1)
            tile = to_img(np.concatenate([rgb, o[..., 3:]], -1))
            x, y = (i % cols) * cell + (cell - 128) // 2, (i // cols) * cell + 4
            img.paste(tile, (x, y), tile)
            ic = icon(skin).resize((24, 24), Image.NEAREST)
            img.paste(ic, (x + 128 - 20, y + 128 - 2), ic)
        os.makedirs(os.path.dirname(args.sheet), exist_ok=True)
        img.save(args.sheet)
        log("sheet", args.sheet)
    log("done", len(ids), "orbs")


if __name__ == "__main__":
    main()
