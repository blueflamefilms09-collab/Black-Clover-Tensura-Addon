#!/usr/bin/env python3
"""The canon looks of the contract devils (docs/devil_trial_model.md, 'Canon looks'): one model, one painted sheet and one glow mask PER DEVIL (26), drawn from the character's own design.

    python3 -B tools/blockbench/gen_devil_canon.py [--only id,id] [--preview]

WHAT THIS IS (rule 15, said plainly): Blockbench and Blender could not run in the session, so this is the FALLBACK. Each model is built from cubes by devil_canon_lib.py (a parametric humanoid
body plus feature builders, and five custom bodies for the devils that are not humanoid), described per devil in devil_canon_specs.py, written as Blockbench's own source format
(tools/blockbench/devil_canon/<id>.bbmodel) and exported from the same data to the game files:

    geo/entity/devil_canon_<id>.geo.json   animations/entity/devil_canon_<id>.animation.json   textures/entity/devil_canon_<id>.png + _glowmask.png

The .bbmodel files were NOT opened in Blockbench (rotation sign: see gen_devil_trial.py). Deterministic: re-running writes the same bytes. The 4.64.0 generic brute (gen_devil_trial.py) stays as the fallback.
"""
import fnmatch
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import devil_canon_lib as L  # noqa: E402
import devil_canon_specs as SP  # noqa: E402
import devil_canon_specs_b  # noqa: E402,F401
import devil_canon_specs_c  # noqa: E402,F401
import gen_devil_trial as T  # noqa: E402
import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

BONES, CUBES = T.BONES, T.CUBES
mix, tones, hexrgb = T.mix, T.tones, T.hexrgb
ASSETS = T.ASSETS
GEO_DIR = os.path.join(ASSETS, "geo", "entity")
ANIM_DIR = os.path.join(ASSETS, "animations", "entity")
TEX_DIR = T.TEX_DIR
BB_DIR = os.path.join(HERE, "devil_canon")

DEFAULT_COLORS = dict(skin="#8a5a3a", hair="#222030", accent="#c8a040", horn="#d8d0c0", cloth="#22222e", tooth="#ece6d6", metal="#a8a8b4", white="#ececf2", fur="#c8c0b0", flame="#ff8a30",
                      stone="#8a8478", bone="#d8d2c0", water="#5a9ae0", leaf="#3a8a4a", flower="#f6f2f4", flower2="#e8c850", wood="#6a4a30", rot="#7a8a3a", ink="#14101a", mist="#e8e0f0")


def color_table(spec):
    c = dict(DEFAULT_COLORS)
    c.update(spec["colors"])
    rgb = {k: hexrgb(v) for k, v in c.items()}
    sk = rgb["skin"]
    derive = dict(skin2=mix(sk, (0, 0, 0), 0.35), claw=rgb["horn"], cloth2=mix(rgb["accent"], (10, 8, 14), 0.55), membrane=mix(mix(sk, rgb["accent"], 0.45), (20, 16, 24), 0.4),
                  hoof=mix(sk, (10, 8, 10), 0.75), boot=mix(rgb["cloth"], (0, 0, 0), 0.3), glove=rgb["white"], fur2=mix(rgb["fur"], (30, 24, 20), 0.35), stone2=mix(rgb["stone"], (20, 18, 16), 0.4),
                  wood2=mix(rgb["wood"], (10, 8, 6), 0.45), horn_tip=rgb["horn"])
    for k, v in derive.items():
        rgb.setdefault(k, v)
    glow = hexrgb(spec["glow"])
    rgb["glow"] = glow
    rgb["gem"] = glow
    rgb["eye"] = hexrgb(spec.get("eye", spec["glow"]))
    rgb["sclera"] = hexrgb(spec.get("sclera", "#14101a"))
    return rgb, glow


# ---------------------------------------------------------------- painting
class Canvas:
    """A face of a cube as texels; coordinates are in model pixels of a nominal face size (sw x sh) so the same marks work at any density."""

    def __init__(self, img, glow, sw, sh, cols):
        self.img, self.glow, self.cols = img, glow, cols
        self.fh, self.fw = img.shape[0], img.shape[1]
        self.sw, self.sh = float(sw), float(sh)

    def col(self, c):
        if isinstance(c, str) and c.startswith("#"):
            return hexrgb(c)
        return self.cols[c]

    def put(self, tx, ty, c, gl):
        if 0 <= tx < self.fw and 0 <= ty < self.fh:
            self.img[ty, tx] = self.col(c)
            if gl:
                self.glow[ty, tx, :3] = self.col("glow") if gl is True else self.col(gl)
                self.glow[ty, tx, 3] = 255
            else:
                self.glow[ty, tx, 3] = 0

    def rect(self, x0, y0, x1, y1, c, gl=False):
        a, b = int(math.floor(x0 * self.fw / self.sw + 1e-6)), int(math.floor(y0 * self.fh / self.sh + 1e-6))
        e, f = int(math.ceil(x1 * self.fw / self.sw - 1e-6)), int(math.ceil(y1 * self.fh / self.sh - 1e-6))
        for ty in range(b, max(f, b + 1)):
            for tx in range(a, max(e, a + 1)):
                self.put(tx, ty, c, gl)

    def line(self, x0, y0, x1, y1, c, gl=False):
        n = int(max(abs(x1 - x0) * self.fw / self.sw, abs(y1 - y0) * self.fh / self.sh) * 2) + 1
        for i in range(n + 1):
            t = i / float(n)
            self.put(int(math.floor((x0 + (x1 - x0) * t) * self.fw / self.sw)), int(math.floor((y0 + (y1 - y0) * t) * self.fh / self.sh)), c, gl)

    def tex(self, fx, fy, w, h, c, gl=False):
        """w x h texels centred on the fractional point (fx, fy) of the face: for marks that must stay the same size at every density (eyes on a wing)."""
        cx, cy = int(fx * self.fw), int(fy * self.fh)
        for ty in range(cy - h // 2, cy - h // 2 + h):
            for tx in range(cx - w // 2, cx - w // 2 + w):
                self.put(tx, ty, c, gl)


def run_ops(cv, ops):
    for op in ops:
        k = op[0]
        if k == "r":
            cv.rect(*op[1:6], *(op[6:7] or (False,)))
        elif k == "l":
            cv.line(*op[1:6], *(op[6:7] or (False,)))
        elif k == "p":
            cv.rect(op[1], op[2], op[1] + 0.01, op[2] + 0.01, op[3], *(op[4:5] or (False,)))
        elif k == "t":
            cv.tex(*op[1:6], *(op[6:7] or (False,)))


def mat_extra(mat, img, face, fw, fh, seed, tn):
    rs = T.rng_for(*seed, "x")
    if mat == "fur" or mat == "fur2":
        for x in range(0, fw, 2):
            img[:, x] *= 0.88 + 0.16 * rs.rand()
        img[::3, :] *= 0.95
    elif mat in ("stone", "stone2"):
        img *= (0.9 + 0.2 * rs.rand(fh, fw, 1)).astype(np.float32)
    elif mat == "bone":
        if fh > 4:
            img[rs.randint(1, fh - 1), :] *= 0.86
        img[:, 0] *= 0.9
    elif mat in ("wood", "wood2"):
        img[:, 0::3] *= 0.86
        img *= (0.94 + 0.1 * rs.rand(fh, 1, 1)).astype(np.float32)
    elif mat == "water":
        img[1::3, :] *= 1.14
        img[:, 2::5] *= 0.9
    elif mat in ("flower", "flower2"):
        img[:, fw // 2:fw // 2 + 1] *= 0.92
    elif mat == "leaf":
        img[:, fw // 2:fw // 2 + 1] *= 0.8
    elif mat == "rot":
        img *= (0.85 + 0.3 * rs.rand(fh, fw, 1)).astype(np.float32)
    return img


def detail_pass(img, mat, face, fw, fh, seed, spec, rgb):
    """Polish pass, the second texture pass (docs/devil_trial_model.md, 'Polish pass'), on the base colour only (the glow mask is not touched): ambient occlusion down the tall faces, a rim light on dark
    materials so a black coat still reads cube by cube, cloth folds, scales and plate panel lines where the spec asks (spec['detail']), a soft blotch noise on skin, fur and stone grain. Deterministic."""
    if mat in ("glow", "gem", "flame", "horn_tip") or fw < 3 or fh < 3:
        return img
    cfg = spec.get("detail", {})
    rs = T.rng_for(*seed, "detail")
    ys = np.arange(fh, dtype=np.float32)[:, None]
    xs = np.arange(fw, dtype=np.float32)[None, :]
    side = face not in ("up", "down")
    mean = float(T.lum(tuple(img.reshape(-1, 3).mean(axis=0))))
    mult = np.ones((fh, fw), np.float32)
    if side and fh >= 5:                                                       # AO: the lower third is darker, the top row catches light
        mult *= 1.0 - 0.16 * np.clip((ys / (fh - 1.0) - 0.62) / 0.38, 0, 1)
        mult[0:1, :] *= 1.07
    if mat in cfg.get("folds", ("cloth", "cloth2", "boot", "glove", "white", "fur2")) and side:     # cloth folds: slightly slanted ridges and a deeper crease
        period = 4.0 + float(rs.randint(0, 3))
        ph = float(rs.rand()) * 6.28
        mult *= 1.0 + 0.085 * np.sin(xs * 6.2832 / period + ys * 0.22 + ph)
        crease = int(rs.randint(1, max(2, fw - 1)))
        mult[:, crease:crease + 1] *= 0.86
    if mat in cfg.get("scales", ()) and side:                                    # scales / plates in rows, offset every other row
        row = (ys // 3).astype(np.int32)
        col = ((xs + (row % 2) * 2) % 4).astype(np.int32)
        mult *= np.where(ys % 3 == 2, 0.80, 1.0) * np.where(col == 0, 0.88, 1.0)
        mult *= np.where((ys % 3 == 0) & (col == 2), 1.10, 1.0)
    if mat in cfg.get("plates", ()) and fw > 5 and fh > 5:                      # an inset panel line and a rivet in each corner
        mult[2:3, 2:fw - 2] *= 0.78
        mult[fh - 3:fh - 2, 2:fw - 2] *= 0.78
        mult[2:fh - 2, 2:3] *= 0.78
        mult[2:fh - 2, fw - 3:fw - 2] *= 0.78
        for yy, xx in ((3, 3), (3, fw - 4), (fh - 4, 3), (fh - 4, fw - 4)):
            mult[yy, xx] *= 1.22
    if mat in ("skin", "skin2", "fur", "stone", "stone2", "wood", "wood2", "rot", "hair", "mist") and fw > 4:   # low-frequency blotches break the flat colour
        blot = rs.rand(max(2, fh // 3), max(2, fw // 3)).astype(np.float32)
        blot = np.kron(blot, np.ones((3, 3), np.float32))[:fh, :fw]
        mult *= 0.95 + 0.1 * np.pad(blot, ((0, fh - blot.shape[0]), (0, fw - blot.shape[1])), mode="edge")
    img = img * mult[..., None]
    if mean < 62:                                                              # dark materials: a faint cool rim light on the top and left edge so the shapes read
        rim = np.zeros((fh, fw), np.float32)
        rim[0, :] += 11
        rim[:, 0] += 7
        if face == "up":
            rim += 9
        img = img + rim[..., None] * np.array([0.9, 0.95, 1.1], np.float32)
    return img


def sheet_for(spec, k, pos, size):
    rgb, glow_c = color_table(spec)
    tn = {m: tones(c) for m, c in rgb.items()}
    gl_rgb = np.array(glow_c, np.float32)
    base = np.zeros((size, size, 4), np.float32)
    glow = np.zeros((size, size, 4), np.float32)
    paint = spec.get("paint", {})
    for i, c in enumerate(CUBES):
        w, h, d = c["size"]
        mat = c["mat"]
        if mat not in tn:
            raise SystemExit("%s: no colour for material '%s' (cube %s)" % (spec["id"], mat, c["name"]))
        fsz = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        nominal = {"face": (8, 8), "chest": (12, 6.4), "abs": (8.4, 4.2), "jaw": (6.8, 1.9)}.get(c["paint"])
        for face, (u, v, fw, fh) in T.face_rects(c, k, pos[i]).items():
            img = T.paint_material(mat, tn[mat], face, fw, fh, (c["name"], face))
            img = mat_extra(mat, img, face, fw, fh, (c["name"], face), tn)
            img = detail_pass(img, mat, face, fw, fh, (c["name"], face), spec, rgb)
            g = np.zeros((fh, fw, 4), np.float32)
            if mat in ("glow", "gem"):
                img = img * 0.45 + gl_rgb * 0.55
                g[..., :3], g[..., 3] = gl_rgb, 255
            elif mat == "flame":
                t = np.linspace(0.0, 1.0, fh, dtype=np.float32)[:, None, None] if face not in ("up", "down") else 0.5
                img = np.broadcast_to(np.array(tn["flame"][1], np.float32) * (0.85 + 0.35 * (1 - t)) + 0.0, (fh, fw, 3)).copy()
                g[..., :3] = img
                g[..., 3] = 255
            elif mat == "horn_tip":
                t = np.linspace(1.0, 0.0, fh, dtype=np.float32)[:, None, None] if face not in ("up", "down") else 1.0
                img = img * (1 - 0.4 * t) + gl_rgb * 0.4 * t                     # polish pass: a soft glow at the very tip (4.71.0 lit the whole last segment, which read as a detached chip)
                g[..., :3] = gl_rgb
                g[..., 3] = (255 * np.clip((np.broadcast_to(t, (fh, fw, 1))[..., 0] - 0.5) / 0.5, 0, 1)) if face not in ("up", "down") else 255
            img = np.clip(img, 0, 255)
            fkey = {"north": "n", "south": "s", "east": "e", "west": "w", "up": "u", "down": "d"}[face]
            for key, ops in paint.items():
                names, _, faces = key.partition("|")
                if fkey not in (faces or "n"):
                    continue
                if names == c["paint"] or fnmatch.fnmatch(c["name"], names):
                    nw, nh = nominal if (nominal and names == c["paint"]) else fsz[face]
                    cv = Canvas(img, g, nw, nh, rgb)
                    run_ops(cv, ops)
            base[v:v + fh, u:u + fw] = np.concatenate([np.clip(img, 0, 255), np.full((fh, fw, 1), 255, np.float32)], axis=2)
            glow[v:v + fh, u:u + fw] = g
    return base, glow


def choose_density():
    for size, ks in ((256, (2.0, 1.75, 1.5)), (512, (2.0, 1.75, 1.5, 1.25, 1.0))):
        T.SHEET = size
        for k in ks:
            pos = T.pack(k)
            if pos is not None:
                return size, k, pos
    raise SystemExit("does not fit 512")


# ---------------------------------------------------------------- the clips
def clips_for(spec, bones):
    base = T.clips()
    names = {b["name"] for b in bones}
    out = {}
    for cn, clip in base.items():
        bs = {bn: tr for bn, tr in clip["bones"].items() if bn in names}
        out[cn] = dict(clip, bones=bs)
    TAU = 2 * math.pi
    for bn, ch, amp, ph in spec.get("sway", []):                         # extra idle / walk motion for the devil's own bones: (bone, 'rotation', (ax, ay, az), phase)
        if bn not in names:
            continue
        for cn, Ln in (("animation.devil_trial.idle", 3.2), ("animation.devil_trial.walk", 1.0)):
            f = (lambda t, Ln=Ln: tuple(a * math.sin(TAU * t / Ln + ph) for a in amp))
            out[cn]["bones"].setdefault(bn, {})[ch] = T.keys(f, Ln, 0.2 if Ln > 2 else 0.125)
    for cn in ("animation.devil_trial.idle", "animation.devil_trial.walk", "animation.devil_trial.attack"):
        for bn in list(out[cn]["bones"]):
            if not out[cn]["bones"][bn]:
                del out[cn]["bones"][bn]
    return out


# ---------------------------------------------------------------- one devil
def build_devil(spec):
    L.reset()
    if spec["body"] == "humanoid":
        D = L.humanoid(spec)
        top = D["top"] + (8.0 if spec.get("horns") else 0.0)
    else:
        info = L.CUSTOM[spec["body"]](spec)
        top = info["top"]
    size, k, pos = choose_density()
    base, glow = sheet_for(spec, k, pos, size)
    return size, k, pos, base, glow, top


def save_png(arr, path):
    T.save_png(arr, path)


def export(spec, write=True):
    size, k, pos, base, glow, top = build_devil(spec)
    anims = clips_for(spec, BONES)
    ident = "devil_canon_" + spec["id"]
    geo = T.geo_doc(k, pos, ident)
    if write:
        for d in (GEO_DIR, ANIM_DIR, TEX_DIR, BB_DIR):
            os.makedirs(d, exist_ok=True)
        save_png(base, os.path.join(TEX_DIR, ident + ".png"))
        save_png(glow, os.path.join(TEX_DIR, ident + "_glowmask.png"))
        json.dump(geo, open(os.path.join(GEO_DIR, ident + ".geo.json"), "w"), indent=1)
        json.dump({"format_version": "1.8.0", "animations": anims}, open(os.path.join(ANIM_DIR, ident + ".animation.json"), "w"), indent=1)
        img = Image.fromarray((np.clip(base, 0, 255) + 0.5).astype(np.uint8), "RGBA")
        json.dump(T.bb_doc(k, pos, img, anims, name=ident), open(os.path.join(BB_DIR, spec["id"] + ".bbmodel"), "w"), indent=1)
    stats = dict(id=spec["id"], body=spec["body"], bones=len(BONES), cubes=len(CUBES), quads=len(CUBES) * 6, sheet=size, density=k, top_px=round(top, 1), height_blocks=round(top / 16.0, 2),
                 lit_texels=int((glow[..., 3] > 0).sum()), source=spec["source"])
    return stats, (base, glow), geo["minecraft:geometry"][0], anims


def main():
    only = None
    if "--only" in sys.argv:
        only = set(sys.argv[sys.argv.index("--only") + 1].split(","))
    import dvtable
    ids = [d["id"] for d in dvtable.DEVILS]
    SP.SPECS.sort(key=lambda s: ids.index(s["id"]))
    if not only:
        assert [s["id"] for s in SP.SPECS] == ids, "missing specs: %s" % [i for i in ids if i not in [s["id"] for s in SP.SPECS]]
    all_stats, sheets = [], {}
    for spec in SP.SPECS:
        if only and spec["id"] not in only:
            continue
        st, sheet, geo, anims = export(spec)
        all_stats.append(st)
        sheets[spec["id"]] = (sheet, geo, anims)
        print("%-16s %-9s cubes=%3d quads=%4d sheet=%d k=%.2f height=%.2f lit=%d" % (spec["id"], spec["body"], st["cubes"], st["quads"], st["sheet"], st["density"], st["height_blocks"], st["lit_texels"]))
    if not only:
        json.dump(all_stats, open(os.path.join(HERE, "devil_canon_stats.json"), "w"), indent=1)
    if "--preview" in sys.argv:
        import preview_canon
        preview_canon.main(sheets, SP.SPECS, all_stats)


if __name__ == "__main__":
    main()
