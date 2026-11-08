#!/usr/bin/env python3
"""Why: the in-game spirit art of the 3.60.0 Beast Will remake. For every will three WHITE glow maps (the painter tints them with the will's colour), drawn from the very 3D design the owner approved
(render3d.py: Minecraft-player proportions, oriented boxes):
  bwform_<stem>_acq.png  Acquire: the human runner with afterimages and the ONE equipped piece (burning bright),
  bwform_<stem>_body.png Integrate: the full armoured beast-humanoid with its signature weapon (shaded, rim-lit),
  bwform_<stem>_acc.png  Integrate accent: a rim halo + the emissive parts (eyes, core gem, flames); for the four wills whose heads multiply it is ONE head, drawn several times.
192 x 192, rgb = 0 under every fully transparent pixel (SafeAdditiveTextureScanTest). Fixed seeds: the output is the same every run.
Run from the repo root: python3 tools/beastwill/remake/gen_form_textures.py [will_id ...]   (writes into src/main/resources/assets/nusmp/textures/particle; --sheet PATH writes a tinted contact sheet)"""
import json, math, os, random, sys
import numpy as np
from PIL import Image, ImageFilter
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import render3d as R
from render_preview import SPEC, PIECE, hx, mix, shade, lum

ROOT = os.path.join(HERE, "..", "..", "..")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
N = 192; SS = 3
HEAD_KIND = {"dragon": "dragon", "snake": "snake", "wolf": "wolf"}
STEM = {"shadow_panther": "panther", "elderwood_guardian": "elder", "wind_skimmer": "raptor", "flame_serpent": "serpent", "ridge_wolf": "wolf", "crested_boar": "boar", "thunder_fang_chimera": "chimera"}
def stem(wid): s = wid[:-5]; return STEM.get(s, s)
MULTI = {"hydra_will", "typhon_will", "scylla_charybdis_will", "yamata_no_orochi_will"}

def fit_sq(boxes, yaw, pitch, fill):
    x0 = y0 = 1e9; x1 = y1 = -1e9
    Rm = R.rot_x(pitch) @ R.rot_y(yaw)
    for b in boxes:
        ax = (Rm @ b.ax.T).T; c = Rm @ b.c
        for sx in (-1, 1):
            for sy in (-1, 1):
                for sz in (-1, 1):
                    p = c + ax[0]*b.h[0]*sx + ax[1]*b.h[1]*sy + ax[2]*b.h[2]*sz
                    x0 = min(x0, p[0]); x1 = max(x1, p[0]); y0 = min(y0, p[1]); y1 = max(y1, p[1])
    sc = N*SS*fill/max(x1-x0, y1-y0)
    return sc, (y0+y1)/2, (x0+x1)/2

def render_luma(boxes, yaw, pitch, fill, frame=None):
    """returns (luma 0..1, coverage 0..1, emissive 0..1) as float arrays N x N."""
    sc, cy, cx = frame or fit_sq(boxes, yaw, pitch, fill)
    S = N*SS
    # shift boxes so the centre column is the figure's centre (rasterise centres x on 0)
    img, glow = R.rasterise(boxes, yaw, pitch, S, S, sc, cy)
    rgb = img[..., :3]; a = img[..., 3] / 255.0
    L = (0.299*rgb[..., 0] + 0.587*rgb[..., 1] + 0.114*rgb[..., 2]) / 255.0
    em = np.clip(glow.max(axis=2) / 255.0, 0, 1)
    def down(x): return np.asarray(Image.fromarray((np.clip(x, 0, 1)*255).astype(np.uint8)).resize((N, N), Image.LANCZOS), np.float32) / 255.0
    return down(L), down(a), down(em)

def to_rgba(L, A, boost_edge=True, floor=0.30):
    """white glow map: brightness = a lifted luma + a bright rim on the silhouette, alpha = coverage."""
    m = A > 0.02
    if m.any():
        hi = np.percentile(L[m], 96) or 1.0
        Ln = np.clip(L / hi, 0, 1)
    else: Ln = L
    v = floor + (1-floor) * Ln**1.2
    if boost_edge:
        a8 = Image.fromarray((A*255).astype(np.uint8))
        inner = np.asarray(a8.filter(ImageFilter.MinFilter(5)), np.float32) / 255.0
        rim = np.clip(A - inner, 0, 1)
        v = np.clip(v + rim*0.9, 0, 1)
    return v

def write(name, v, A):
    rgb = (np.clip(v, 0, 1)*255).astype(np.uint8)
    a = (np.clip(A, 0, 1)*255).astype(np.uint8)
    rgb[a == 0] = 0
    im = np.dstack([rgb, rgb, rgb, a])
    Image.fromarray(im, "RGBA").save(os.path.join(OUT_DIR, name + ".png"), optimize=True)

def colours(d):
    cols = [hx(c) for c in d["spirit"]["colors"]]
    if lum(cols[0]) < 70: cols = [mix(cols[0], (255, 255, 255), .30), cols[1] if lum(cols[1]) > 60 else mix(cols[1], (255, 255, 255), .4), cols[2]]
    return cols

def acquire_art(w, d):
    sp = SPEC[w]; M, A, A2 = colours(d)
    grey = (150, 150, 150)
    def runner(emi_piece):
        r = R.player(swing=.8, skin=grey, shirt=grey, pants=(120, 120, 120), hair=(110, 110, 110))
        n0 = len(r)
        if PIECE[w] == "arm": r += R.gauntlet_for_run(M, A)
        else: r += R.build_acquire(w, sp, M, A, A2)[n0:]
        for b in r[n0:]: b.emi = emi_piece
        return r
    main = runner(True)
    for b in main[-2:]: b.emi = True
    ghosts = []
    for k, al in ((3, .20), (2, .34), (1, .55)):
        gp = R.player(swing=.8, skin=grey, shirt=grey, pants=(120, 120, 120), hair=(110, 110, 110))
        for b in gp: b.c = b.c + R.V([0, 0, -k*6.0]); b.noise = .04
        ghosts.append((gp, al))
    yaw, pitch = math.radians(38), -0.10
    everything = main + ghosts[0][0]
    fr = fit_sq(everything, yaw, pitch, .88)
    L = np.zeros((N, N), np.float32); A_ = np.zeros((N, N), np.float32); E = np.zeros((N, N), np.float32)
    for gp, al in ghosts:
        l, a, e = render_luma(gp, yaw, pitch, .88, fr)
        L = np.maximum(L, l*al); A_ = np.maximum(A_, a*al)
    l, a, e = render_luma(main, yaw, pitch, .88, fr)
    m = a > 0.05
    L = np.where(m, l, L); A_ = np.maximum(A_, a)
    v = to_rgba(np.maximum(L, e), A_, floor=.35)
    v = np.where(e > 0.3, 1.0, v)                    # the equipped piece burns white-hot: the painter's tint colours it
    return v, np.maximum(A_, e)

def integrate_boxes(w, d):
    sp = dict(SPEC[w]); sp["fx"] = "none"
    M, A, A2 = colours(d)
    return R.build_integrate(w, sp, M, A, A2), sp, (M, A, A2)

def body_art(w, d):
    ib, sp, cols = integrate_boxes(w, d)
    yaw, pitch = math.radians(24), -0.08
    L, A_, E = render_luma(ib, yaw, pitch, .86)
    v = to_rgba(np.maximum(L, E), A_, floor=.24)
    return v, A_, E, ib

def accent_art(w, d, ib, E_body, A_body):
    a8 = Image.fromarray((np.clip(A_body, 0, 1)*255).astype(np.uint8))
    halo = np.asarray(a8.filter(ImageFilter.GaussianBlur(9)), np.float32) / 255.0
    core = np.asarray(a8.filter(ImageFilter.MinFilter(3)), np.float32) / 255.0
    ring = np.clip(halo*1.25 - core*0.9, 0, 1)                         # a corona that hugs the figure, empty behind the body itself
    hot = np.clip(E_body*1.4, 0, 1)
    A_ = np.clip(np.maximum(ring*0.85, hot), 0, 1)
    v = np.clip(0.55 + ring*0.45 + hot, 0, 1)
    return v, A_

def head_art(w, d):
    sp = dict(SPEC[w]); M, A, A2 = colours(d)
    k = sp["head"]; k = k if k in HEAD_KIND else "snake"
    sub = dict(sp); sub.update(horns="none", mane=0, nemes=0, ears="none")
    body = shade(M, .78)
    boxes = [R.obox((0, 0, 0), (9.6, 9.6, 9.6), body, face=True)] + R.head_feats(0, 0, 0, k, sub, M, A, A2, sc=1.2, main=False)
    boxes.append(R.limb((0, -3, -1), (0, -14, -3), 4.4, body))      # the neck it hangs from
    yaw, pitch = math.radians(38), -0.06
    L, A_, E = render_luma(boxes, yaw, pitch, .86)
    v = to_rgba(np.maximum(L, E), A_, floor=.34)
    return v, A_

def make(w, d):
    s = stem(w)
    v, a = acquire_art(w, d); write(f"bwform_{s}_acq", v, a)
    v, a, e, ib = body_art(w, d); write(f"bwform_{s}_body", v, a)
    if w in MULTI:
        v2, a2 = head_art(w, d)
    else:
        v2, a2 = accent_art(w, d, ib, e, a)
    write(f"bwform_{s}_acc", v2, a2)

def sheet(path, designs):
    ids = [d["id"] for d in designs]
    W = N*3
    img = Image.new("RGB", (W*2, N*((len(ids)+1)//2)), (8, 9, 13))
    for i, d in enumerate(designs):
        s = stem(d["id"]); tint = hx(d["core"]["tint"] if lum(hx(d["core"]["tint"])) >= 70 else "#5C5C62")
        for j, layer in enumerate(("acq", "body", "acc")):
            t = np.asarray(Image.open(os.path.join(OUT_DIR, f"bwform_{s}_{layer}.png")).convert("RGBA"), np.float32) / 255.0
            col = np.array(tint, np.float32) / 255.0
            out = t[..., :3] * col * t[..., 3:4] * 1.6
            tile = Image.fromarray((np.clip(out, 0, 1)*255).astype(np.uint8))
            img.paste(tile, ((i % 2)*W + j*N, (i//2)*N))
    img.save(path); print(path)

if __name__ == "__main__":
    argv = sys.argv[1:]
    sheet_path = argv[argv.index("--sheet")+1] if "--sheet" in argv else None
    args = [a for a in argv if not a.startswith("--") and a != sheet_path]
    designs = json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))
    sel = [d for d in designs if not args or d["id"] in args]
    for d in sel:
        make(d["id"], d); print(d["id"], flush=True)
    if sheet_path: sheet(sheet_path, sel)
