"""Fuegoleon's flame arm in the Djinn-Equip way (3.1): a sculpted FLAME LIMB forged in piece by piece, one GeoLite model per piece, plus the tongue sprite sheet.

    python3 -B tools/gen_fuegoleon_arm_djinn.py              # everything (geo, animations, textures, glow maps, the tongue sheet)
    python3 -B tools/gen_fuegoleon_arm_djinn.py tongues      # only the sprite sheet
    python3 -B tools/gen_fuegoleon_arm_djinn.py geo          # only the five limb pieces

The target is the owner's reference art of Fuegoleon (docs/previews/fuegoleon_arm_djinn/reference_arm.png): the right arm below the shoulder is a solid, sculpted limb of flame, NO armour: a
bright orange body with a hot yellow core, a dark red outline at the silhouette and flat cel-shaded bands (white-yellow, yellow, orange, red, dark red), a shoulder cap, an upper arm, an
elbow, a forearm, a wrist and a clenched fist, with flame tongues curling off the shoulder, the elbow (a big hooked tongue) and the wrist. What is taken from the Djinn Equip is the way it is
built and shown: a set of PIECES (shoulder, upper arm, elbow, forearm, fist), each its own model anchored on the arm, forged in one after the other with a flash.

Files written (assets/nusmp, the frame of tools/water_aura_lib.py: model pixels, y UP, front on -z, the player's right arm at x = -6):
    geo/entity/fuegoleonarm_<piece>.geo.json, animations/entity/fuegoleonarm_<piece>.animation.json (clip "idle": a few degrees of sway),
    textures/entity/fuegoleonarm_<piece>.png and _glow.png (the glow map: only the hot core, alpha 255 everywhere and black elsewhere: nothing under alpha 0),
    textures/aura/fad_tongue.png (four frames side by side, 48 x 96 each: one flame tongue, alpha blended, the colour under the clear texels is the outline colour so no dark fringe).
Cube budget: LIMB_CUBES (the test reads the files and counts).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import FnMat, Piece, Anim, sine_rot, grid, fbm, rgb  # noqa: E402
from geo_builder import ROOT  # noqa: E402

KEY = "fuegoleonarm"
CX, CZ = -6.0, 0.0                       # the axis of the arm in model pixels (x, z)

# the cel bands of the reference, hottest first
C_CORE, C_YELLOW, C_ORANGE, C_RED, C_DARK = rgb("FFF0A0"), rgb("FFC02E"), rgb("FF6A14"), rgb("DC300C"), rgb("86160A")
T_RED = rgb("F04A12")

LIMB_PIECES = ("shoulder", "upper", "elbow", "fore", "fist")


# ====================================================================================================================== the cel body
def flame_body(seed, y0, y1, core=0.5, outline=True, flat=False):
    """One face of the limb: flat cel bands, a hot streak down the middle, red sides and a dark red outline.

    `y0`..`y1` is the slice's height in model pixels: the bands are worked out in ABSOLUTE height (row 0 of a side face is y1), so the streaks run on from one slice of the arm into the next
    and the whole limb reads as one flowing body of flame, not a stack of blocks. The middle of the face is the hottest and the sides fall to red and then to a dark red outline, so the
    silhouette of the limb is outlined whichever way it is turned. `core` lifts the heat (the fist is hotter than the forearm).
    """
    def fn(face, w, h, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 389) % (2 ** 32))
        u, v, x, y = grid(w, h)
        n1 = fbm(rng, w, h, 2, 2)
        if face in ("up", "down"):
            d = np.hypot((u - 0.5) * 2.0, (v - 0.5) * 2.0)
            heat = 0.80 - 0.95 * d + (n1 - 0.5) * 0.2
        else:
            ay = y1 - v * (y1 - y0)                                                 # absolute height of every row
            mid = 0.5 + 0.10 * np.sin(ay * 0.62 + 0.4) + 0.05 * np.sin(ay * 1.7)
            side = np.abs(u - mid) * 2.0
            if flat:                                                                 # a finger or a knuckle: one warm face, hot on top, a dark crease below
                blobf = 1.0 - v
            blob = 0.5 + 0.5 * np.sin(ay * 0.74 + 3.0 * (u - mid) + 1.2 * np.sin(ay * 0.31)) * (1.0 - side)
            heat = 0.24 + 0.34 * core + 0.30 * blob + 0.07 * np.sin(ay * 1.31 + 1.7) - 0.60 * side ** 2.2 + (n1 - 0.5) * 0.12
            if flat:
                heat = 0.28 + 0.22 * core + 0.30 * blobf - 0.55 * side ** 3.0 - 0.9 * np.clip((v - 0.80) / 0.2, 0, 1)
        img = np.zeros((h, w, 4), np.float32)
        img[..., 3] = 255.0
        col = np.empty((h, w, 3), np.float32)
        col[:] = C_DARK
        col[heat > -0.06] = C_RED
        col[heat > 0.20] = C_ORANGE
        col[heat > 0.56] = C_YELLOW
        col[heat > 0.86] = C_CORE
        if outline and face not in ("up", "down"):
            edge = (u * w < 1.5) | (u * w > w - 1.5)
            col[edge] = C_DARK
        img[..., :3] = col
        g = np.zeros((h, w, 4), np.float32)
        g[..., 3] = 255.0
        hot = np.clip((heat - 0.45) / 0.40, 0.0, 1.0)
        g[..., :3] = (C_YELLOW * 0.55 + C_ORANGE * 0.45)[None, None, :] * (hot * 0.8)[..., None]
        return img, g
    return FnMat(fn, "flame_body")


def octagon(p, bone, y0, y1, w, d, seed, core=0.5, dx=0.0, dz=0.0, flat=False):
    """A rounded slice of the limb: a box and a smaller one turned 45 degrees (the section reads as a tube with cut corners, not a square). Two cubes. The straight box is at least 4.8 wide so it covers the player's own arm."""
    mat = flame_body(seed, y0, y1, core, flat=flat)
    cx, cz = CX + dx, CZ + dz
    org = (cx - w / 2.0, y0, cz - d / 2.0)
    p.box(bone, org, (w, y1 - y0, d), mat)
    bw, bd = w * 0.8, d * 0.8                                       # the turned box is smaller: the corners round off without poking out; the straight box covers the player's own arm
    p.box(bone, (cx - bw / 2.0, y0, cz - bd / 2.0), (bw, y1 - y0, bd), mat, rot=(0, 45, 0), pivot=(cx, (y0 + y1) / 2.0, cz))


def _piece(name):
    p = Piece(KEY, name, density=4)
    a = Anim()
    a.clip("idle", length=1.8, loop=True)
    p.bone("arm", (CX, 22.0, CZ), parent="root")
    return p, a


def piece_shoulder(out):
    p, a = _piece("shoulder")
    octagon(p, "arm", 21.4, 23.4, 6.4, 5.8, 11, 0.5)                 # the shoulder, over the sleeve
    octagon(p, "arm", 23.4, 25.5, 5.0, 4.8, 12, 0.6)                 # its rounded cap
    sine_rot(a, "idle", "arm", 1.8, 2, 1.2, 0.0)
    p.save(a)
    out.append(p)


def piece_upper(out):
    p, a = _piece("upper")
    octagon(p, "arm", 19.2, 21.4, 6.4, 5.6, 21, 0.5)                 # the bicep, the widest part
    octagon(p, "arm", 17.2, 19.2, 5.4, 5.0, 22, 0.45)
    sine_rot(a, "idle", "arm", 1.8, 2, 1.6, 0.12)
    p.save(a)
    out.append(p)


def piece_elbow(out):
    p, a = _piece("elbow")
    octagon(p, "arm", 15.4, 17.2, 4.8, 4.8, 31, 0.4)                 # the joint: narrow, the hollow between the muscles
    sine_rot(a, "idle", "arm", 1.8, 2, 2.0, 0.25)
    p.save(a)
    out.append(p)


def piece_fore(out):
    p, a = _piece("fore")
    octagon(p, "arm", 13.4, 15.4, 6.0, 5.4, 41, 0.5)                 # the forearm muscle
    octagon(p, "arm", 11.6, 13.4, 4.8, 4.8, 42, 0.5)                 # tapering to the wrist
    sine_rot(a, "idle", "arm", 1.8, 2, 2.4, 0.38)
    p.save(a)
    out.append(p)


def piece_fist(out):
    p, a = _piece("fist")
    octagon(p, "arm", 9.6, 11.6, 6.0, 5.6, 51, 0.6)                  # the back of the hand and the palm
    for k in range(4):                                                # four curled fingers, knuckles forward and down
        p.box("arm", (CX - 3.0 + k * 1.5, 7.6, CZ - 3.6 + 0.3 * (k % 2)), (1.5, 2.6, 2.4), flame_body(52 + k, 7.6, 10.2, 0.5, flat=True), d=3)
    p.box("arm", (CX + 2.3, 8.4, CZ - 1.8), (1.5, 2.2, 2.8), flame_body(56, 8.4, 10.6, 0.5, flat=True), d=3)      # the thumb along the side
    sine_rot(a, "idle", "arm", 1.8, 2, 2.8, 0.5)
    p.save(a)
    out.append(p)


GEO = {"shoulder": piece_shoulder, "upper": piece_upper, "elbow": piece_elbow, "fore": piece_fore, "fist": piece_fist}


# ====================================================================================================================== the tongue sheet
FRAMES, FW, FH, SS = 4, 48, 96, 4


def tongue_frame(f, ss=SS):
    """One frame of the flame tongue: a tall curling lick with a hooked tip and two small side licks, drawn in flat cel bands (outline, orange, yellow, pale core) at ss times the size.

    Returns (rgb float (h, w, 3), alpha float (h, w)) at ss resolution. The four frames sway and breathe a little differently, which the painter plays as a 4 frame loop.
    """
    W, H = FW * ss, FH * ss
    ph = f * math.pi / 2.0
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)

    def lick(x0, y0, length, width, bend, hook, phase, steps=40):
        """Fills one tongue: base at (x0, y0) pixels (frame units), growing up by `length`, swaying, the tip hooking sideways by `hook`."""
        left, right = [], []
        for i in range(steps + 1):
            t = i / steps
            cx = x0 + bend * math.sin(t * 2.4 + phase) * t * 1.0 + hook * (t ** 3.0)
            cy = y0 - length * t
            pinch = min(1.0, t / 0.22) ** 0.7                                    # a narrow root (it joins the arm), the belly near a quarter of the way up, a long sharp tip
            wd = width * 0.5 * pinch * (1.0 - t) ** 0.62 * (1.0 + 0.16 * math.sin(t * 8.0 + phase * 1.6))
            left.append(((cx - wd) * ss, cy * ss))
            right.append(((cx + wd) * ss, cy * ss))
        d.polygon(left + right[::-1], fill=255)

    lick(20, 95, 92, 22, 9.0, 19.0, ph)                                   # the main tongue, hooked at the tip
    lick(12 + 2.0 * math.sin(ph), 95, 50 + 5 * math.sin(ph + 1), 15, -4.0, -9.0, ph + 1.7)      # a small lick on the left
    lick(36 + 2.0 * math.cos(ph), 95, 40 + 5 * math.cos(ph), 14, 3.5, 7.0, ph + 3.1)            # and one on the right
    mask = np.asarray(im, dtype=np.float32) / 255.0
    # bands by the distance to the edge, from repeated erosion of the hi-res mask (cheap and crisp)
    bands = []
    for r in (1.2, 2.2, 4.4):
        er = im.filter(ImageFilter.MinFilter(int(round(r * ss)) * 2 + 1))
        bands.append(np.asarray(er, dtype=np.float32) / 255.0)
    yy = np.mgrid[0:H, 0:W][0].astype(np.float32) / H                       # 0 top, 1 bottom
    col = np.empty((H, W, 3), np.float32)
    col[:] = C_DARK                                                         # the outline (1 texel at final size)
    col[bands[0] > 0.5] = T_RED
    col[bands[1] > 0.5] = C_ORANGE
    col[bands[2] > 0.5] = C_YELLOW
    # the upper part of a tongue cools to red-orange at the very tip
    tip = (yy < 0.16) & (mask > 0.5)
    col[tip & (bands[1] <= 0.5)] = C_RED
    return col, mask


def make_tongues():
    ss = SS
    sheet = np.zeros((FH, FW * FRAMES, 4), np.float32)
    for f in range(FRAMES):
        col, a = tongue_frame(f)
        for c in range(3):                                                  # the colour and the alpha are reduced on their own: no dark fringe at the soft edge
            ch = Image.fromarray(np.clip(col[..., c], 0, 255).astype(np.uint8), "L").resize((FW, FH), Image.BOX)
            sheet[:, f * FW:(f + 1) * FW, c] = np.asarray(ch, dtype=np.float32)
        al = Image.fromarray((a * 255).astype(np.uint8), "L").resize((FW, FH), Image.BOX)
        sheet[:, f * FW:(f + 1) * FW, 3] = np.asarray(al, dtype=np.float32)
    # where nothing is drawn the colour stays the outline colour (the box filter already mixed it from opaque neighbours only at the edge)
    clear = sheet[..., 3] < 1.0
    sheet[clear, :3] = C_RED
    os.makedirs(os.path.join(ROOT, "textures", "aura"), exist_ok=True)
    path = os.path.join(ROOT, "textures", "aura", "fad_tongue.png")
    Image.fromarray(np.clip(sheet, 0, 255).astype(np.uint8), "RGBA").save(path)
    print(path, sheet.shape)


def main(argv):
    want = argv or ["geo", "tongues"]
    if "geo" in want:
        out = []
        for name in LIMB_PIECES:
            GEO[name](out)
        total = 0
        for m in out:
            print("%-28s cubes %3d" % (m.id, m.cube_count()))
            total += m.cube_count()
        print("limb cubes", total)
    if "tongues" in want:
        make_tongues()


if __name__ == "__main__":
    main(sys.argv[1:])
