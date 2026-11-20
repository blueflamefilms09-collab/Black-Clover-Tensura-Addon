"""Hellblaze, The Demon Sin as a katana with a sheath (saya), made in Blender (docs/hellblaze_katana.md).

Run: python tools/blender/hellblaze_katana.py [--only katana,lod,saya] [--trial DIR] [--fast]

--trial DIR  writes the meshes, skins and previews into DIR instead of the mod's assets (for looking before replacing).
--fast       few samples and a small bake (for iterating only).

Three meshes, all black steel and dark red:
  hellblaze_the_demon_sin__katana       the blade in the main hand: slim curved single-edge blade, a thorn-ring tsuba, a black wrap with a dark-red thread, a sin-eye menuki
  hellblaze_the_demon_sin__katana_lod   the same weapon at about a tenth of the quads (WeaponUpgrade draws it from 12 blocks)
  hellblaze_the_demon_sin__saya         the sheath that hangs in the off hand while the katana is held (client/HellblazeSayaLayer)
Skins (textures/entity/weapon/up/): <id>.png, <id>_glow.png and <id>_veins.png; <id>_lod.png, <id>_lod_glow.png; <id>__saya.png, <id>__saya_glow.png.
The glow and veins skins are GREY masks: the colour is the tint of the mesh's `glow` line (dark red) and of the visual_upgrade json, so the purple of a hit is a tint, not a second texture.
The old broadsword (weapon_meshes/up/hellblaze_the_demon_sin*.txt, weapon_meshes/hellblaze_the_demon_sin.txt and their skins) is not touched.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402
from mathutils import Vector  # noqa: E402
from vassal_a import sharpen, sheet, yplate  # noqa: E402

import numpy as np  # noqa: E402
from PIL import Image, ImageFilter  # noqa: E402

ID = "hellblaze_the_demon_sin"
KATANA, LOD, SAYA = ID + "__katana", ID + "__katana_lod", ID + "__saya"
UP_MESH = os.path.join(bl.ASSETS, "weapon_meshes", "up")
UP_SKIN = os.path.join(bl.SKIN_DIR, "up")
# the place line of the old Hellblaze: the same origin and axis angle, so the old display transforms hold the katana in the same hand spot; the scale gives the in-hand length
PLACE_OX, PLACE_OY, PLACE_ANGLE = -0.10872, -0.13203, 44.750
HAND_LENGTH = 1.72                       # blocks (item units) the whole weapon is long; the class band of a longsword is 1.4 to 1.8
GRIP = 0.15                              # share of the length from the butt where the hand holds the tsuka
GLOW = ("A8142A", 0.16, 0.55, 1.0)       # the tint of the glow layer: dark red; WeaponRenderer shifts it to purple on a hit
FAST = False
THIN = "--broad" not in sys.argv     # katanas v2: a real katana blade (width 1/18 of its length, thickness a third of the width); --broad builds the old broad one (its meshes are kept in weapon_meshes/classic_vassal)


# ================================================================ materials
def metals():
    m = {}
    m["steel"] = bl.mat("hz_steel", rgb(34, 34, 42), metallic=1.0, rough=0.30, bump="brushed", bstr=0.12, bevel=0.08, groove=rgb(12, 12, 18), gamount=0.5)
    m["black"] = bl.mat("hz_black", rgb(30, 30, 38), metallic=0.9, rough=0.36, bump="hammer", bscale=0.5, bstr=0.10, bevel=0.2, groove=rgb(12, 12, 18), gamount=0.55)
    m["edge"] = bl.mat("hz_edge", rgb(118, 112, 126), metallic=1.0, rough=0.2, bevel=0.0)           # the polished back ridge catches the light
    m["hamon"] = bl.mat("hz_hamon", rgb(108, 14, 26), metallic=1.0, rough=0.16, emit=(0.5, 0.04, 0.05, 1.0), strength=0.5, bevel=0.0)
    m["edgeglow"] = bl.mat("hz_edgeglow", rgb(60, 8, 14), rough=0.4, emit=(1.0, 0.1, 0.1, 1.0), strength=1.0, bevel=0.0)
    m["groove"] = bl.mat("hz_groove", rgb(8, 8, 12), metallic=1.0, rough=0.5, bevel=0.0)
    m["vein"] = bl.mat("hz_vein", rgb(70, 6, 12), rough=0.5, emit=(0.86, 0.07, 0.07, 1.0), strength=0.9, bevel=0.0)
    m["habaki"] = bl.mat("hz_habaki", rgb(78, 14, 22), metallic=1.0, rough=0.24, bump="rings", bscale=3.2, bstr=0.25, bevel=0.15, groove=rgb(30, 6, 10), gamount=0.5)
    m["tsuba"] = bl.mat("hz_tsuba", rgb(26, 26, 33), metallic=1.0, rough=0.4, bump="hammer", bscale=0.6, bstr=0.14, bevel=0.2, groove=rgb(10, 10, 14), gamount=0.6)
    m["rim"] = bl.mat("hz_rim", rgb(70, 10, 18), metallic=1.0, rough=0.3, emit=(0.55, 0.04, 0.05, 1.0), strength=0.8, bevel=0.1)
    m["wrap"] = sharpen(bl.mat("hz_wrap", rgb(16, 16, 22), rough=0.6, bump="cross", bscale=1.25, bstr=0.9, groove=rgb(118, 10, 24), gamount=1.0, bevel=0.0), 0.18, 0.42)
    m["cord"] = bl.mat("hz_cord", rgb(120, 12, 26), rough=0.6, bump="wrap", bscale=2.5, bstr=0.6, groove=rgb(48, 4, 10), gamount=0.8, bevel=0.0)
    m["eye"] = bl.mat("hz_eye", rgb(150, 12, 28), metallic=0.3, rough=0.08, emit=(1.0, 0.12, 0.1, 1.0), strength=1.0, bevel=0.05)
    m["pupil"] = bl.mat("hz_pupil", rgb(3, 2, 4), rough=0.3, bevel=0.0)
    m["lacquer"] = bl.mat("hz_lacquer", rgb(14, 14, 19), metallic=0.25, rough=0.16, bump="grain", bscale=1.5, bstr=0.08, bevel=0.1, groove=rgb(6, 6, 9), gamount=0.4)
    return m


def reset():
    bl.reset(samples=6 if FAST else 24)


# ================================================================ the katana
def katana(out, lod=False):
    """The blade, tsuba, handle. lod=True: the same weapon with few sides and stations (the details are baked into the skin anyway, so they cost no quads at either level)."""
    reset()
    M = metals()
    random.seed(11)
    Y0, Y1 = 29.0, 99.6                                                        # the machi, the tip
    T, W0 = (0.52, 1.95) if THIN else (1.3, 3.0)                               # half thickness, half width at the machi (was 1.3 and 3.0: 1/12 as wide as long)
    YK = 90.0                                                                  # the kissaki begins
    K0 = 1.0 if THIN else 5.0 / 3.0                                            # the taper to the yokote: 0.3 cm (was 0.5)
    SIDES = 5 if lod else 12

    def hw(y):
        if y <= YK:
            return W0 - 0.3 * K0 * (y - Y0) / (YK - Y0)
        u = (y - YK) / (Y1 - YK)
        return max(0.05, (W0 - 0.3 * K0) * (1.0 - u ** 1.7))

    def cx(y):                                                                 # the back line keeps its curve (sori), the edge runs to it in the point
        u = max(0.0, (y - Y0) / (Y1 - Y0))
        return -(2.6 if THIN else 3.0) * u * u - ((W0 - 0.3 * K0) - hw(y) if y > YK else 0.0)

    def th(y):
        return T if y <= YK else T * (0.35 + 0.65 * (1 - (y - YK) / (Y1 - YK)))

    def sec(y):
        w, t = hw(y), th(y)
        return [(w, 0), (-0.2 * w, t), (-w, 0.6 * t), (-w, -0.6 * t), (-0.2 * w, -t)]

    def ji(y, k, s, lift):                                                     # a point on the edge-side face: k 0 at the ridge, 1 at the edge
        w, t = hw(y), th(y)
        return (cx(y) + (-0.2 * w + k * 1.2 * w), y, s * (t * (1 - k) + lift))

    def shi(y, k, s, lift):                                                    # a point on the back-side face: k 0 at the back, 1 at the ridge
        w, t = hw(y), th(y)
        return (cx(y) - w + k * 0.8 * w, y, s * (t * (0.6 + 0.4 * k) + lift))

    ys = [29, 52, 78, 90, 96, 99.6] if lod else [29, 36, 44, 52, 60, 68, 76, 83, 88, 90, 93, 96, 98.4, 99.6]
    bl.loft("blade", [bl.ring(y, sec(y), ox=cx(y)) for y in ys], M["steel"])
    # ---- baked details: nothing here is a quad of the mesh
    for s in (1, -1):
        yy = [Y0 + 7 + 0.75 * i for i in range(int((Y1 - Y0 - 7) / 0.75))]
        rows, rows2 = [], []
        for y in yy:                                                           # the hamon: a dark-red wave along the edge, and the line of light on the edge itself
            k0 = 0.62 + 0.1 * math.sin(y * 0.8) + 0.05 * math.sin(y * 2.2 + 1.0)
            k0 = k0 if y < YK else min(0.9, k0 + 0.45 * (y - YK) / (Y1 - YK))
            rows.append([ji(y, k0 + (0.985 - k0) * j / 3.0, s, 0.05) for j in range(4)])
            rows2.append([ji(y, 0.93, s, 0.07), ji(y, 0.985, s, 0.07)])
        sheet("hamon%d" % s, rows, M["hamon"], s=s)
        sheet("edgeglow%d" % s, rows2, M["edgeglow"], s=s)
        yy = [38 + 2.0 * i for i in range(27)]                                 # the groove of the back face and its line of light
        sheet("groove%d" % s, [[shi(y, 0.2 + 0.6 * j / 2.0, s, 0.05) for j in range(3)] for y in yy], M["groove"], s=s)
        sheet("grooveline%d" % s, [[shi(y, 0.44 + 0.12 * j, s, 0.1) for j in range(2)] for y in yy[1:-1]], M["vein"], s=s)
        for v in range(3):                                                     # the sin: three jagged veins climbing from the habaki on each face
            y_start, y_end = 36.5 + 1.5 * v, 52 + 11 * (v + 1) + 6 * random.random()
            k = 0.3 + 0.18 * v
            rows3 = []
            y = y_start
            while y < y_end:
                u = (y - y_start) / (y_end - y_start)
                k += 0.05 * (random.random() - 0.5) + 0.012 * math.sin(y * 1.3 + v)
                kk = min(0.78, max(0.12, k))
                d = 0.045 * (1.0 - 0.8 * u)
                rows3.append([ji(y, kk - d, s, 0.06), ji(y, kk + d, s, 0.06)])
                y += 0.9
            sheet("vein%d_%d" % (s, v), rows3, M["vein"], s=s)
    # ---- the habaki: a dark-red collar with rings
    hab = ((27.8, 2.45, 1.0), (36.2, 2.2, 0.75)) if lod else ((27.8, 2.45, 1.0), (31.4, 2.4, 0.98), (34.6, 2.3, 0.85), (36.2, 2.2, 0.75))
    if not THIN:
        hab = ((27.8, 3.55, 1.95), (36.2, 3.1, 1.25)) if lod else ((27.8, 3.55, 1.95), (31.4, 3.5, 1.9), (34.6, 3.3, 1.6), (36.2, 3.1, 1.25))
    bl.loft("habaki", [bl.ring(y, bl.rrect(w, t, 0.55 if not THIN else 0.4, 1 if lod else 2), ox=cx(y)) for y, w, t in hab], M["habaki"])
    # ---- the tsuba: a ring of eight thorns, the sin
    n = 16 if lod else 64
    thorn = [((3.0 + 2.1 * abs(math.cos(4 * (2 * math.pi * k / n))) ** 2.6) * math.cos(2 * math.pi * k / n),
              (3.0 + 2.1 * abs(math.cos(4 * (2 * math.pi * k / n))) ** 2.6) * math.sin(2 * math.pi * k / n) * 0.9) for k in range(n)]
    yplate("tsuba", thorn, 27.0, 28.1, M["tsuba"])
    rim = [(x * 0.96, 27.55, z * 0.96) for x, z in thorn]
    bl.sweep("tsuba_rim", rim + rim[:1], [(0.3, 0.42), (-0.3, 0.62), (-0.3, -0.62), (0.3, -0.42)], M["rim"], up=(0, 1, 0), caps=(False, False), part="tsuba", detail=True)
    if not lod:
        bl.lathe("guard_collar", [(2.15, 26.0), (2.55, 26.5), (2.55, 27.2), (2.3, 27.5)], M["rim"], sides=SIDES, squash=0.86)
    # ---- the tsuka: a black wrap with a dark-red thread, the menuki as the Sin's eye, the kashira
    prof = [(1.95, 26.6), (1.88, 23.5), (1.82, 20.5), (1.78, 17.5), (1.76, 14.5), (1.76, 11.5), (1.78, 8.5), (1.82, 5.5), (1.88, 2.5), (1.95, 0.2)]
    bl.lathe("tsuka_wrap", [prof[0], prof[5], prof[-1]] if lod else prof, M["wrap"], sides=SIDES, squash=0.86)
    m = None if lod else bl.gem("menuki", 0.85, M["eye"], center=(0, 15.0, 1.5), depth=0.3, crown=0.25, sides=6, axis="z", rx=0.8, ry=4.4, part="menuki")
    if not lod:
        bl.copy(m, "menuki_b", scale=(1, 1, -1))
    bl.lathe("kashira", [(2.4, 1.8), (2.05, -0.6), (1.2, -0.9)] if lod else [(1.9, 2.4), (2.4, 1.8), (2.48, 0.6), (2.05, -0.6), (1.2, -0.9)], M["black"], sides=SIDES, squash=0.85)
    bl.lathe("kashira_gem", [(0.9, -0.8), (1.5, -1.6), (0.01, -2.9)] if lod else [(0.9, -0.8), (1.5, -1.6), (1.1, -2.4), (0.01, -2.9)], M["eye"], sides=5 if lod else 8, squash=0.9)
    if lod:
        return _done(LOD, out, True)
    bl.lathe("kashira_ring", [(2.52, 1.7), (2.56, 1.2), (2.56, 0.9), (2.52, 0.4)], M["rim"], sides=SIDES, squash=0.85, part="kashira")
    # ---- the sageo: a dark-red cord and a short tassel
    bl.tube("sageo", [(2.0, 25.2, 0.6), (4.4, 24.8, 0.9), (5.9, 21.0, 0.4), (6.1, 15.5, 0.0), (6.05, 11.0, 0.0)], 0.4, M["cord"], sides=5, sub=2 if lod else 3)
    bl.lathe("sageo_tassel", [(0.45, 11.2), (0.8, 10.2), (0.85, 6.6)], M["cord"], sides=6, origin=(6.05, 0, 0), part="sageo")
    name = LOD if lod else KATANA
    return _done(name, out, lod)


# ================================================================ the sheath
def saya(out):
    """The scabbard: lacquered black, a dark-red mouth collar, a kurikata knob, a chape, thin glowing lines; the mouth is at y = 0, the tip at y = L. It follows the sori of the blade."""
    reset()
    M = metals()
    SF = 0.62 if THIN else 1.0                                                 # the sheath follows the thinner blade (was half width 3.75, half thickness 1.75)
    L, T = 78.0, 1.75 * SF

    def cx(y):
        return -3.2 * (y / L) ** 2

    def hw(y):
        return (3.75 - 0.85 * (y / L)) * SF

    ys = [0, 5, 12, 21, 30, 39, 48, 57, 65, 71, 75.5, L]
    bl.loft("saya_body", [bl.ring(y, bl.rrect(hw(y), T, 1.0), ox=cx(y)) for y in ys], M["lacquer"], part="saya")
    bl.loft("koiguchi", [bl.ring(y, bl.rrect(hw(y) + 0.28, T + 0.28, 1.05), ox=cx(y)) for y in (-0.6, 0.0, 3.6, 4.4)], M["habaki"])
    bl.loft("kojiri", [bl.ring(y, bl.rrect(max(0.2, (hw(y) + 0.22) * f), max(0.2, (T + 0.22) * f), 0.8), ox=cx(y)) for y, f in ((L - 6.4, 1.0), (L - 3.5, 0.98), (L - 1.0, 0.8), (L + 0.9, 0.34))], M["habaki"])
    bl.lathe("kurikata", [(0.01, 0.0), (1.0, 0.25), (1.35, 1.0), (1.2, 1.9), (0.01, 2.1)], M["rim"], sides=8, origin=(hw(11.0) + cx(11.0) - 0.4, 11.0, 0.0), axis="x", part="kurikata")
    bl.lathe("kurikata_bar", [(0.55, -1.4), (0.55, 1.4)], M["rim"], sides=6, origin=(hw(11.0) + cx(11.0) + 1.3, 11.0, 0.0), axis="z", part="kurikata")
    for s in (1, -1):                                                          # thin dark-red lines of light down both faces and a ring of thorns at the mouth, baked only
        pts = [(cx(y) - 0.9 + 0.9 * math.sin(y * 0.35) * 0.0, y) for y in range(6, 72, 2)]
        rows = [[(cx(y) - 1.1 * SF, y, s * (T + 0.06)), (cx(y) - 0.85 * SF, y, s * (T + 0.06))] for y in range(7, 70, 2)]
        sheet("line%d" % s, rows, M["vein"], s=s)
        rows = [[(cx(y) + 1.6 * SF, y, s * (T + 0.06)), (cx(y) + 1.82 * SF, y, s * (T + 0.06))] for y in range(7, 70, 2)]
        sheet("lineb%d" % s, rows, M["hamon"], s=s)
        for k in range(3):                                                     # three flame-veins climbing from the chape
            y0 = 66.0 - 5.0 * k
            rows = [[(cx(y) + (0.2 + 0.7 * math.sin(y * 0.9 + k)) * SF - 0.1, y, s * (T + 0.07)), (cx(y) + (0.2 + 0.7 * math.sin(y * 0.9 + k)) * SF + 0.1, y, s * (T + 0.07))] for y in [y0 + 0.9 * i for i in range(int((L - 4 - y0) / 0.9))]]
            sheet("flame%d_%d" % (s, k), rows, M["vein"], s=s)
    bl.tube("saya_sageo", [(hw(11.0) + cx(11.0) + 1.6, 11.0, 0.0), (hw(11.0) + 5.0, 6.0, 1.4), (hw(11.0) + 7.0, 1.0, 0.0), (hw(11.0) + 5.4, -4.0, -1.0)], 0.38, M["cord"], sides=5, sub=3)
    bl.lathe("saya_tassel", [(0.42, -4.2), (0.78, -5.4), (0.82, -9.0)], M["cord"], sides=6, origin=(hw(11.0) + 5.4, 0, -1.0), part="saya_sageo")
    return _done(SAYA, out, False)


# ================================================================ writing
def _done(name, out, lod):
    bake = (512 if lod else 1024) if FAST else (1024 if lod else 2048)
    import tempfile
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as fh:                  # bl_lib wants a header with a place line; post() writes the real one
        fh.write("place 0 0 45.000 0.015 0\n")
    try:
        return bl.finish(name, glow=GLOW, bake=bake, skin=256 if lod else 1024, budget=320 if lod else 2800, header_from=fh.name, **out)
    finally:
        os.unlink(fh.name)


def post(name, out_mesh, out_skin, hand=True, veins=True):
    """The mod-specific edits after bl_lib wrote the files: the place and grip lines (the katana keeps the old Hellblaze's hand spot), grey glow and veins skins."""
    path = os.path.join(out_mesh, name + ".txt")
    ymin, ymax = 1e9, -1e9
    for line in open(path):
        w = line.split()
        if w and w[0] == "qn":
            for k in range(4):
                y = float(w[2 + k * 8 + 1])
                ymin, ymax = min(ymin, y), max(ymax, y)
    lines = []
    for line in open(path).read().split("\n"):
        w = line.split()
        if w and w[0] in ("grip", "place"):
            continue
        lines.append(line)
        if line.startswith("#") and len(lines) == 1:
            if hand:
                lines.append("place %.5f %.5f %.3f %.6f %.3f" % (PLACE_OX, PLACE_OY, PLACE_ANGLE, HAND_LENGTH / (ymax - ymin), ymin))
                lines.append("grip %.3f" % GRIP)
            else:
                lines.append("place 0 0 45.000 0.015 %.3f" % ymin)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines))
    # COMBINED bakes the emission into the skin as a pale pink; the skin should be dark red where it glows, so those pixels are re-coloured from their own lightness
    sk = np.asarray(Image.open(os.path.join(out_skin, name + ".png")).convert("RGBA"), np.float32)
    gm = np.asarray(Image.open(os.path.join(out_skin, name + "_glow.png")).convert("RGBA"), np.float32)
    m = np.clip(gm[..., :3].max(axis=2) / 255.0 * 1.2, 0, 1)[..., None]
    lum = sk[..., :3].max(axis=2, keepdims=True)
    dark = np.concatenate([lum * 0.5, lum * 0.06, lum * 0.09], axis=2)
    sk[..., :3] = sk[..., :3] * (1 - m) + dark * m
    Image.fromarray(np.clip(sk, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out_skin, name + ".png"), optimize=True)
    # the glow skin keeps only its strength (a grey mask): the colour is the tint of the glow line, so a hit can shift it to purple
    g = np.asarray(Image.open(os.path.join(out_skin, name + "_glow.png")).convert("RGBA"), np.float32)
    v = g[..., :3].max(axis=2)
    grey = np.clip(np.stack([v, v, v, g[..., 3]], axis=2), 0, 255).astype(np.uint8)
    Image.fromarray(grey, "RGBA").save(os.path.join(out_skin, name + "_glow.png"), optimize=True)
    if veins:                                                                  # the flowing-veins skin: the same mask, softened, so the wave reads as light and not as a copy of the glow
        soft = np.asarray(Image.fromarray(grey, "RGBA").filter(ImageFilter.GaussianBlur(1.6)), np.float32)
        Image.fromarray(np.clip(0.55 * soft + 0.45 * grey, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out_skin, name + "_veins.png"), optimize=True)
    return ymin, ymax


def main():
    global FAST
    args = sys.argv[1:]
    only = ["katana", "lod", "saya"]
    trial = None
    i = 0
    while i < len(args):
        if args[i] == "--only":
            only = args[i + 1].split(",")
            i += 1
        elif args[i] == "--trial":
            trial = args[i + 1]
            i += 1
        elif args[i] == "--fast":
            FAST = True
        i += 1
    out_mesh = os.path.join(trial, "mesh") if trial else UP_MESH
    out_skin = os.path.join(trial, "skin") if trial else UP_SKIN
    out_prev = os.path.join(trial or os.path.join(bl.ROOT, "docs", "hellblaze", "previews"))
    os.makedirs(out_prev, exist_ok=True)
    out = dict(out_mesh=out_mesh, out_skin=out_skin)
    if "katana" in only:
        katana(dict(out, preview=os.path.join(out_prev, "katana_blender.png")))
        ymin, ymax = post(KATANA, out_mesh, out_skin)
        print("katana: y %.2f .. %.2f, %.2f blocks long in hand, scale %.6f" % (ymin, ymax, HAND_LENGTH, HAND_LENGTH / (ymax - ymin)))
    if "lod" in only:
        katana(dict(out), lod=True)
        post(LOD, out_mesh, out_skin, veins=False)
    if "saya" in only:
        saya(dict(out, preview=os.path.join(out_prev, "saya_blender.png")))
        post(SAYA, out_mesh, out_skin, hand=False, veins=False)


if __name__ == "__main__":
    main()
