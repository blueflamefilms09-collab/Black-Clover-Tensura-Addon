"""Why: the things a character wears or grows that are neither clothing nor a held prop: a cape, armour plates, hats and crowns, glasses and masks, horns, ears,
wings, a tail, a halo. Each is built from a few boxes with the same material painters, on its own bone where it should move (cape, wings, tail, tails of a
headband), so the clip generator can swing it without knowing what it is.
"""
import math

from . import color as C
from .core import (Canvas, SIDES, FACES, TAU, bandage_paint, bone_paint, canvas_plate, chain_paint, clump, fabric, feather, fur, hair_paint, hsh, leather, metal, mix, plate,
                   px, scale_paint, solid, pal)
from .outfit import Surf, emblem, mat, rows_bottom, rows_top, rows_range, everywhere, SIDES as _S
from .rig import rot_file, r5

SIN, COS = math.sin, math.cos


def build_all(R, S, Lk, body):
    d = R.d
    if S["cape"]:
        build_cape(R, S, Lk, body)
    if S["armor"]:
        build_armor(R, S, Lk, body)
    mask = S["face"].get("mask")
    if mask:
        build_mask(R, S, Lk, mask)
    for a in S["accessories"]:
        ACCESSORIES[a["kind"]](R, S, Lk, a)
    if S["ears"]:
        build_ears(R, S, Lk, S["ears"])
    if S["horns"]:
        build_horns(R, S, Lk, S["horns"])
    if S["wings"]:
        build_wings(R, S, Lk, S["wings"])
    if S["tail"]:
        build_tail(R, S, Lk, S["tail"])
    if S["halo"]:
        build_halo(R, S, Lk, S["halo"])


def _rgb(c, default):
    return C.rgb(c) if c is not None else C.rgb(default)


def _glow4(g, a=220):
    if g is None:
        return None
    r = C.rgb(g)
    return (r[0], r[1], r[2], a)


# ------------------------------------------------------------------------------------------------------------------------ cape
def build_cape(R, S, Lk, body):
    """A cape or cloak on the back: bones `cape` (the upper part, hinged at the shoulders) and `cape_low` (the lower part, hinged at the waist, two tiers that flare
    toward the hem); a shoulder drape on each arm for a cloak or mantle. The outer (back) face carries the emblem, the inner face the lining; a ragged hem has no bottom
    face, so its points are really pointed."""
    d = R.d
    Cp = S["cape"]
    kind = Cp["kind"]
    col = Cp["color"]
    trim = Cp["trim"] or C.mix(col, (255, 255, 255), 0.55)
    lining = Cp["lining"] or C.mix(col, (0, 0, 0), 0.35)
    length = Cp["length"]
    width = (d.tw + 4.0 + (2.0 * d.aw if kind in ("cloak", "hooded_cloak") else 0.0)) * Cp["width"]
    z0 = d.td / 2 + 0.3
    top_y = d.sh_y - 0.4
    hem_y = max(0.0, top_y - length * (top_y))
    total = top_y - hem_y
    split = top_y - total * 0.45
    mid = split - (split - hem_y) * 0.5
    ragged = Cp["ragged"] or kind == "tattered"
    R.bone("cape", "body", (0, top_y, z0))
    R.bone("cape_low", "cape", (0, split, z0))
    em = Cp.get("emblem")
    cp_mat = "heavy" if kind in ("tattered", "cloak", "hooded_cloak") else "cloth"
    hem_pal = C.shades(trim, 5, 1.0)
    outer_top = mat(cp_mat, col, 91, seam=False)
    outer_mid = mat(cp_mat, col, 95, seam=False)
    outer_low = mat(cp_mat, col, 96, hem=0 if ragged else 2, hem_cols=hem_pal, cut="b" if ragged else "", cut_depth=11, cut_width=4.0, cut_taper=2.4, seam=False)
    inner = mat("cloth", lining, 92, seam=False)
    dec = None
    if em and em.get("kind", "none") != "none":
        ew = int(round(em.get("size", 7) * 2))
        eh = int(round(em.get("size", 7) * 2 * em.get("aspect", 1.0)))
        cv = emblem(em["kind"], ew, eh, C.rgb(em.get("color") or trim), em.get("glow") and C.rgb(em["glow"]), None)
        dec = (cv, ew, eh)

    def face_paint(base_outer, y_off_px):
        s = Surf(base_outer)
        if dec:
            cv, ew, eh = dec

            def p(f, i, j, fw, fh):
                if f != "back":
                    return None, None
                x0 = (fw - ew) // 2
                y0 = int((em.get("y", 3)) * 2) - y_off_px
                if x0 <= i < x0 + ew and y0 <= j < y0 + eh:
                    return cv.at(i - x0, j - y0)
                return None, None
            s.region(lambda f, i, j, fw, fh: f == "back", p)
        s.region(lambda f, i, j, fw, fh: f == "front", inner)
        return s
    faces_top = SIDES + ("top",)
    R.box("cape", (-width / 2, split, z0), (width, top_y - split, 1.0), face_paint(outer_top, 0), faces=faces_top)
    R.box("cape_low", (-(width + 1.0) / 2, mid, z0 + 0.15), (width + 1.0, split - mid, 1.0), face_paint(outer_mid, int((top_y - split) * 2)), faces=SIDES)
    R.box("cape_low", (-(width + 2.4) / 2, hem_y, z0 + 0.4), (width + 2.4, mid - hem_y, 1.0), face_paint(outer_low, int((top_y - mid) * 2)),
          faces=SIDES if ragged else SIDES + ("bottom",))
    if kind in ("cloak", "hooded_cloak", "mantle"):
        drape = mat(cp_mat, col, 93, seam=False, cut="b" if ragged else "", cut_depth=4)
        c = R.box("arm_l", (d.arm_x - d.aw / 2 - 0.6, d.sh_y - 3.0, -d.ad / 2 - 0.6), (d.aw + 1.2, 4.0, d.ad + 1.2), drape, faces=SIDES + ("top",))
        R.pk.twin(R.m, "arm_r", c)
    if kind == "hooded_cloak" or Cp.get("hood"):
        hp = mat("heavy", col, 94, seam=False)
        R.box("cape", (-3.5, d.sh_y - 1.0, d.td / 2 - 0.2), (7.0, 4.5, 3.5), hp, faces=SIDES + ("top",))
        R.box("cape", (-3.0, d.sh_y + 1.5, d.td / 2 + 1.0), (6.0, 3.0, 2.0), hp, faces=SIDES + ("top",))
    if Cp.get("clasp"):
        R.box("body", (-1.0, d.sh_y - 1.8, -d.td / 2 - 0.4), (2.0, 1.8, 0.4), metal(C.shades(Cp["clasp"], 5, 1.2), 12), faces=("front",))
    R.facts["cape"] = {"kind": kind, "length": total}


# ------------------------------------------------------------------------------------------------------------------------ armour
def build_armor(R, S, Lk, body):
    d = R.d
    A = S["armor"]
    pcs = A["pieces"]
    style = A["style"]
    col = A["color"]
    trim = A["trim"]
    glow = _glow4(A["glow"], 190)
    kind = {"plate": "plate", "chain": "chain", "leather": "leather", "bone": "bone", "scale": "scale"}[style]
    base = mat(kind, col, 101, glow=glow, trim=C.cloth_palette(trim, 5, 1.2)) if kind == "plate" else mat(kind, col, 101)
    tr = metal(C.cloth_palette(trim, 5, 1.2), 102, glow=glow)
    if "breastplate" in pcs:
        h = d.torso_h * 0.72
        s = Surf(base)
        em = A.get("emblem")
        if em:
            fw, fh = int(round((d.tw + 0.8) * 2)), int(round(h * 2))
            ew = int(round(min(d.tw * 0.7, 7) * 2))
            cv = emblem(em["kind"] if isinstance(em, dict) else em, ew, ew, C.rgb((em.get("color") if isinstance(em, dict) else None) or trim), glow and glow[:3], None)
            c2 = Canvas(fw, fh)
            for yy in range(ew):
                for xx in range(ew):
                    c, g = cv.at(xx, yy)
                    if c is not None or g is not None:
                        c2.put((fw - ew) // 2 + xx, 4 + yy, c, 255, g[:3] if g else None, g[3] if g else 255)
            s.decal("front", c2)
        s.region(rows_bottom(2), tr, faces=SIDES)
        R.box("body", (-(d.tw + 0.8) / 2, d.hip_y + d.torso_h - h, -(d.td + 0.8) / 2), (d.tw + 0.8, h, d.td + 0.8), s)
    if "gorget" in pcs:
        R.box("body", (-3.8, d.sh_y - 0.6, -3.4), (7.6, 2.2, 6.8), mat(kind, col, 103), inflate=0.2)
    if "pauldrons" in pcs:
        for sg, bn in ((1, "arm_l"), (-1, "arm_r")):
            pass
        R.pair("arm_l", "arm_r", (d.arm_x - d.aw / 2 - 1.2, d.sh_y - 2.8, -d.ad / 2 - 1.2), (d.aw + 2.4, 3.8, d.ad + 2.4), base, faces=SIDES + ("top", "bottom"))
        R.pair("arm_l", "arm_r", (d.arm_x - d.aw / 2 - 1.6, d.sh_y - 5.0, -d.ad / 2 - 0.9), (d.aw + 3.2, 2.2, d.ad + 1.8), tr, faces=SIDES + ("top", "bottom"))
        if "spikes" in pcs:
            for dz in (-1.0, 1.0):
                R.spike("arm_l", (d.arm_x + 0.0, d.sh_y + 1.0, dz * (d.ad / 2 - 0.3)), 1.6, 5.0 * max(1.0, d.top / 32), 0, -12 if True else 0, tr, fracs=(0.6, 0.4))
            for c in R.m.bones["arm_l"]["cubes"][-4:]:
                R.pk.twin(R.m, "arm_r", c)
    if "bracers" in pcs or "gauntlets" in pcs:
        fw_ = max(2.0, d.aw - 0.5)
        fd_ = max(2.0, d.ad - 0.5)
        n = d.fore * (0.55 if "bracers" in pcs and "gauntlets" not in pcs else 0.85)
        R.pair("forearm_l", "forearm_r", (d.arm_x - fw_ / 2 - 0.4, d.wrist_y + (d.fore - n), -fd_ / 2 - 0.4), (fw_ + 0.8, n, fd_ + 0.8), base, faces=SIDES + ("top", "bottom"))
        if "gauntlets" in pcs:
            hw_ = max(2.0, fw_ - 0.5)
            R.pair("hand_l", "hand_r", (d.arm_x - hw_ / 2 - 0.4, d.wrist_y - d.hand - 0.2, -(max(2.0, fd_ - 0.5)) / 2 - 0.4), (hw_ + 0.8, d.hand + 0.2, max(2.0, fd_ - 0.5) + 0.8), tr)
    if "greaves" in pcs or "sabatons" in pcs:
        sh_len = d.knee_y - 2.0
        if "greaves" in pcs:
            R.pair("shin_l", "shin_r", (d.leg_x - d.lw / 2 - 0.2, 2.0 + sh_len * 0.1, -d.ld / 2 - 0.2), (d.lw + 0.4, sh_len * 0.85, d.ld + 0.4), base, faces=SIDES + ("top",))
            R.pair("leg_l", "leg_r", (d.leg_x - d.lw / 2 - 0.4, d.knee_y - 1.2, -d.ld / 2 - 0.6), (d.lw + 0.8, 3.0, d.ld + 1.2), tr)          # the knee cop
        if "sabatons" in pcs:
            R.pair("shin_l", "shin_r", (d.leg_x - d.lw / 2 - 0.5, 0.0, -d.ld / 2 - 2.5), (d.lw + 1.0, 2.2, d.ld + 3.0), base)
    if "tassets" in pcs:
        h = min(7.0, d.hip_y * 0.5)
        R.pair("skirt_l", "skirt_r", (0.2, d.hip_y - h + 0.6, -d.td / 2 - 1.0), (d.tw / 2 + 1.0, h, 1.2), base, faces=SIDES + ("bottom",))
        R.pair("skirt_l", "skirt_r", (d.tw / 2 - 0.2, d.hip_y - h + 0.6, -d.td / 2 - 0.4), (1.2, h, d.td + 0.8), base, faces=SIDES + ("bottom",))
    if "helm" in pcs:
        _helm(R, S, Lk, base, tr, glow)


def _helm(R, S, Lk, base, tr, glow):
    d = R.d
    T = d.head_y + 8.0
    eye = C.rgb(S["face"].get("eye_glow") or S["glow"].get("color") or "#ff5030")
    s = Surf(base)
    slit = Canvas(36, 36)
    for x in range(5, 31):
        for y in (15, 16, 17):
            slit.put(x, y, (14, 10, 18), 255, eye if y == 16 and S["glow"]["eyes"] else None, 230)
    s.decal("front", slit)
    R.box("head", (-4.6, d.head_y - 0.3, -4.6), (9.2, 8.8, 9.2), s, faces=("front", "left", "right", "back", "top"), dens={"front": 4})
    R.box("head", (-4.9, d.head_y + 5.5, -4.9), (9.8, 1.0, 9.8), tr, faces=("front", "left", "right", "back", "top", "bottom"))
    R.box("head", (-0.6, T + 0.4, -4.6), (1.2, 2.0, 9.2), tr, faces=SIDES + ("top",))


# ------------------------------------------------------------------------------------------------------------------------ the mask on the face
def build_mask(R, S, Lk, M):
    """A plate over the face: half (nose to chin), full, visor (an eye band), skull, oni (with fangs and horn stumps), veil, plague (a beak), fox. It stands 0.5 px
    off the face, so it reads as a mask in profile too; the eye holes glow when `eye_glow` is set."""
    d = R.d
    kind = M["kind"]
    col = C.rgb(M["color"])
    col2 = C.rgb(M["color2"])
    g = C.rgb(M["glow"]) if M.get("glow") else None
    eg = C.rgb(M["eye_glow"]) if M.get("eye_glow") else None
    cols = C.shades(col, 5, 1.1)
    W, H = 34, 34
    cv = Canvas(W, H)
    hi, mid, lo = cols[3], cols[2], cols[1]
    dark = col2

    def fill(y0, y1, x0=0, x1=W - 1):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                t = 3 if (x < 4 and y < y0 + 9) or (y == y0) else 2
                if x >= W - 4:
                    t = 1
                if y >= y1 - 1:
                    t = 1
                cv.put(x, y, cols[t])
    if kind == "half":
        fill(17, 33)
        for x in range(0, W):
            cv.put(x, 17, cols[4] if x % 2 else hi)
        for x in range(13, 21):                                  # mouth slit and lines
            cv.put(x, 27, dark)
        for y in range(19, 31):
            cv.put(W // 2, y, cols[1])
    elif kind in ("full", "skull", "oni", "fox", "plague", "veil"):
        fill(0, 33)
        # eye holes
        for (ex0, ex1) in ((6, 14), (19, 27)):
            for y in range(14, 20):
                for x in range(ex0, ex1 + 1):
                    cv.put(x, y, dark, 255, eg, 235 if eg else 255)
        if kind == "skull":
            for y in range(20, 24):
                for x in (16, 17):
                    cv.put(x, y, dark)
            for x in range(8, 26, 2):
                for y in range(27, 33):
                    cv.put(x, y, dark if y > 26 else mid)
        elif kind == "oni":
            for y in range(0, 5):
                for x in range(0, W):
                    cv.put(x, y, cols[1])
            for x in range(8, 26):
                cv.put(x, 28, dark)
            for x in (10, 24):
                for y in range(28, 33):
                    cv.put(x, y, (240, 236, 220))
            for x in range(4, 30):
                cv.put(x, 12, dark) if x < 8 or x > 25 else None
        elif kind == "plague":
            pass
        elif kind == "fox":
            for x in range(8, 26):
                cv.put(x, 24, dark)
        elif kind == "veil":
            cv.c[:] = 0
            fill(18, 33)
    elif kind == "visor":
        fill(12, 21)
        for x in range(2, W - 2):
            for y in (15, 16, 17):
                cv.put(x, y, dark, 255, eg, 235 if eg else 255)
    p = canvas_plate(cv)
    full = kind in ("full", "skull", "oni", "fox", "plague", "veil")
    y0 = d.head_y + (0.0 if full else (0.0 if kind == "half" else 3.0))
    h = 8.0 if full else (4.5 if kind == "half" else 3.2)
    if kind == "half":
        y0 = d.head_y
    elif kind == "visor":
        y0 = d.head_y + 3.0
        h = 2.9
    R.box("head", (-4.25, y0, -4.8), (8.5, h, 0.8), mask_painter(cv, W, H, kind, y0, h, d), faces=("front", "left", "right", "top", "bottom"), dens={"front": 4})
    if kind == "plague":
        R.box("head", (-1.3, d.head_y + 1.0, -9.0), (2.6, 3.0, 4.5), mat("leather", M["color"], 111), faces=SIDES + ("top", "bottom"))
    if kind == "oni":
        for sg in (1, -1):
            R.box("head", (sg * 2.6 - 0.6, d.head_y + 8.0, -3.0), (1.2, 2.5, 1.2), mat("bone", "#e8e0c8", 112), faces=SIDES + ("top",))
    if kind == "fox":
        R.box("head", (-1.0, d.head_y + 1.5, -6.6), (2.0, 2.0, 2.0), mat("leather", M["color2"], 113))


def mask_painter(cv, W, H, kind, y0, h, d):
    """The face of the mask plate: the 34 x 34 canvas stretched over the plate's front; the sides and top are the mask's rim."""
    cols_rim = None

    def p(face, i, j, fw, fh):
        if face == "front":
            x = int(i * W / max(1, fw))
            full = H if kind in ("full", "skull", "oni", "fox", "plague", "veil") else H
            if kind == "half":
                y = int(17 + j * (H - 17) / max(1, fh))
            elif kind == "visor":
                y = int(12 + j * 10 / max(1, fh))
            else:
                y = int(j * H / max(1, fh))
            c, g = cv.at(min(W - 1, x), min(H - 1, y))
            if c is not None or g is not None:
                return c, g
            return None, None
        c = cv.at(10, 10)[0] or (120, 120, 130, 255)
        return px(mix(c[:3], (0, 0, 0), 0.25)), None
    return p


# ------------------------------------------------------------------------------------------------------------------------ head accessories
def _acc_glasses(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#1c1c24"
    p = metal(C.shades(col, 5, 1.1), 3, emboss=False)
    cy = d.head_y + 4.0 - 0.4                                   # eye height: canvas y 16..20 of 32 => 1.5..3.5 px below the middle
    ey = d.head_y + 8.0 - (18.0 / 32.0) * 8.0
    z = -4.35
    for sg in (1, -1):
        cx = sg * 2.6
        for (dx, dy, w, h) in ((-1.9, 1.5, 3.8, 0.5), (-1.9, -1.6, 3.8, 0.5), (-1.9, -1.6, 0.5, 3.6), (1.4, -1.6, 0.5, 3.6)):
            R.box("head", (cx + dx, ey + dy, z), (w, h, 0.5), p, faces=("front", "left", "right", "top", "bottom"))
        R.box("head", (cx + (-0.1 if True else 0), ey - 1.1, z - 0.05), (0.01 + 0.0, 0.01, 0.01), p) if False else None
    R.box("head", (-0.8, ey + 0.6, z), (1.6, 0.5, 0.5), p, faces=("front", "top"))
    for sg in (1, -1):
        R.box("head", (sg * 4.2 - 0.25, ey + 0.8, -4.2), (0.5, 0.5, 8.2), p, faces=("left", "right", "top"))


def _acc_monocle(R, S, Lk, a):
    d = R.d
    p = metal(C.shades(a["color"] or "#c8a040", 5, 1.2), 3, emboss=False)
    ey = d.head_y + 8.0 - (18.0 / 32.0) * 8.0
    z = -4.35
    cx = 2.6
    for (dx, dy, w, h) in ((-1.9, 1.5, 3.8, 0.5), (-1.9, -1.6, 3.8, 0.5), (-1.9, -1.6, 0.5, 3.6), (1.4, -1.6, 0.5, 3.6)):
        R.box("head", (cx + dx, ey + dy, z), (w, h, 0.5), p, faces=("front", "left", "right", "top", "bottom"))
    R.box("head", (cx + 1.6, ey - 5.0, z), (0.3, 4.0, 0.3), p, faces=SIDES)


def _acc_goggles(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#5a4a3c"
    lens = C.rgb(a["glow"] or a["color2"] or "#7ad8ff")
    strap = leather(C.shades(col, 5, 0.9), 4)
    y = d.head_y + 5.6
    R.box("head", (-4.4, y, -4.4), (8.8, 1.4, 8.8), strap, faces=SIDES, key="gstrap")
    for sg in (1, -1):
        R.box("head", (sg * 2.2 - 1.6, y - 0.6, -5.2), (3.2, 2.6, 1.2), metal(C.shades(col, 5, 1.1), 5), faces=SIDES + ("top", "bottom"))
        R.box("head", (sg * 2.2 - 1.1, y - 0.1, -5.5), (2.2, 1.6, 0.4), solid(lens, (lens[0], lens[1], lens[2], 235)), faces=("front",))


def _acc_eyepatch(R, S, Lk, a):
    d = R.d
    p = leather(C.shades(a["color"] or "#1c1c24", 5, 0.9), 3)
    ey = d.head_y + 8.0 - (18.0 / 32.0) * 8.0
    side = 1 if (a["side"] or "left") == "left" else -1
    R.box("head", (side * 2.6 - 1.9, ey - 1.9, -4.35), (3.8, 3.8, 0.5), p, faces=("front", "left", "right", "top", "bottom"))
    R.box("head", (-4.25, ey + 0.8, -4.25), (8.5, 0.6, 8.5), p, faces=SIDES, rotation=(0, 0, 0))


def _acc_headband(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#1c1c24"
    p = mat("cloth", col, 120, seam=False)
    y = d.head_y + 5.6
    R.box("head", (-4.4, y, -4.4), (8.8, 1.5, 8.8), p, faces=SIDES + ("top", "bottom"))
    if a["color2"] is not None:
        R.box("head", (-1.3, y + 0.2, -4.7), (2.6, 1.1, 0.4), metal(C.shades(a["color2"], 5, 1.2), 4), faces=("front",))
    R.bone("ribbon", "head", (0, y + 0.7, 4.4))
    R.box("ribbon", (-1.2, y - 5.0, 4.4), (1.2, 6.0, 0.6), p, rotation=(0, 0, 8), pivot=(0, y + 0.7, 4.4), faces=("front", "back", "left", "right", "bottom"))
    R.box("ribbon", (0.2, y - 6.0, 4.4), (1.2, 7.0, 0.6), p, rotation=(0, 0, -8), pivot=(0, y + 0.7, 4.4), faces=("front", "back", "left", "right", "bottom"))


def _acc_bandana(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#8a2a2a"
    p = mat("cloth", col, 121, seam=False, patch=C.shades(col, 5, 1.2))
    T = d.head_y + 8.0
    R.box("head", (-4.5, T - 3.2, -4.5), (9.0, 3.4, 9.0), p, faces=SIDES + ("top",))
    R.box("head", (-4.5, T - 0.6, -4.5), (9.0, 1.4, 9.0), p, faces=("top",))
    R.bone("ribbon", "head", (0, T - 2.0, 4.5))
    R.box("ribbon", (-1.5, T - 7.0, 4.5), (3.0, 6.0, 0.8), p, faces=("front", "back", "left", "right", "bottom"))


def _acc_cap(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#3a4a6a"
    p = mat("cloth", col, 122, seam=False)
    T = d.head_y + 8.0
    R.box("head", (-4.6, T - 2.6, -4.6), (9.2, 3.6, 9.2), p, faces=SIDES + ("top",))
    R.box("head", (-4.0, T - 2.2, -8.0), (8.0, 0.8, 3.6), p, faces=("front", "top", "bottom", "left", "right"))


def _acc_beret(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#8a2a2a"
    p = mat("cloth", col, 123, seam=False)
    T = d.head_y + 8.0
    R.box("head", (-5.0, T - 0.6, -5.0), (10.0, 2.0, 10.0), p, faces=SIDES + ("top", "bottom"), rotation=(0, 0, 8), pivot=(0, T, 0))
    R.box("head", (-0.5, T + 1.4, -0.5), (1.0, 1.0, 1.0), p)


def _acc_wizard_hat(R, S, Lk, a, witch=False):
    d = R.d
    col = a["color"] or ("#2a2038" if witch else "#3a4a8a")
    band = a["color2"] or "#c8a040"
    p = mat("cloth", col, 124, seam=False, patch=C.shades(col, 5, 1.3))
    T = d.head_y + 8.0
    R.box("head", (-7.0, T - 1.0, -7.0), (14.0, 1.2, 14.0), p, faces=SIDES + ("top", "bottom"))
    R.box("head", (-4.8, T - 0.2, -4.8), (9.6, 2.0, 9.6), mat("cloth", band, 125, seam=False), faces=SIDES)
    R.bone("hat", "head", (0, T + 1.5, 0))
    y = T + 1.8
    w = 8.4
    ang = 0.0
    x = 0.0
    for k in range(4):
        h = 3.8
        c = R.box("hat", (x - w / 2, y, -w / 2), (w, h, w), p, rotation=(0, 0, -ang) if ang else None, pivot=(x, y, 0) if ang else None, faces=SIDES + ("top",))
        d_ = rot_file((0, h, 0), 0, 0, -ang)
        x, y = x + d_[0], y + d_[1]
        w = max(1.5, w * 0.68)
        ang += 9.0 if not witch else 14.0
    if witch:
        R.box("head", (-4.6, T + 1.8, -4.6), (9.2, 1.0, 9.2), mat("cloth", band, 126), faces=SIDES)


def _acc_witch_hat(R, S, Lk, a):
    _acc_wizard_hat(R, S, Lk, a, witch=True)


def _acc_crown(R, S, Lk, a):
    d = R.d
    col = a["color"] or "#d8a830"
    g = _glow4(a["glow"], 180)
    p = metal(C.shades(col, 5, 1.3), 6, glow=g)
    T = d.head_y + 8.0
    R.box("head", (-4.4, T - 0.2, -4.4), (8.8, 1.8, 8.8), p, faces=SIDES + ("top", "bottom"))
    for k, (x, z) in enumerate(((-3.4, -3.4), (0.0, -3.9), (3.4, -3.4), (-3.9, 0.0), (3.9, 0.0), (-3.4, 3.4), (0.0, 3.9), (3.4, 3.4))):
        h = 2.8 if k % 2 == 0 else 1.8
        R.box("head", (x - 0.6, T + 1.6, z - 0.6), (1.2, h, 1.2), p)
    gem = C.rgb(a["color2"] or "#c83040")
    R.box("head", (-0.7, T + 0.2, -4.6), (1.4, 1.2, 0.4), metal(C.shades(gem, 5, 1.2), 7, glow=(gem[0], gem[1], gem[2], 220), emboss=False), faces=("front",))


def _acc_tiara(R, S, Lk, a):
    d = R.d
    p = metal(C.shades(a["color"] or "#d8d8e8", 5, 1.3), 6, glow=_glow4(a["glow"], 160))
    T = d.head_y + 8.0
    R.box("head", (-4.3, T - 1.8, -4.4), (8.6, 0.8, 1.0), p, faces=SIDES + ("top",))
    for k, x in enumerate((-2.4, 0.0, 2.4)):
        R.box("head", (x - 0.5, T - 1.8, -4.4), (1.0, 1.0 + (1.6 if k == 1 else 0.6), 0.8), p)


def _acc_circlet(R, S, Lk, a):
    d = R.d
    p = metal(C.shades(a["color"] or "#c8a040", 5, 1.2), 6, glow=_glow4(a["glow"], 160))
    T = d.head_y + 8.0
    R.box("head", (-4.3, T - 2.4, -4.3), (8.6, 0.7, 8.6), p, faces=SIDES + ("top",))
    gem = C.rgb(a["color2"] or "#38b8d0")
    R.box("head", (-0.6, T - 2.6, -4.6), (1.2, 1.4, 0.4), metal(C.shades(gem, 5, 1.2), 7, glow=(gem[0], gem[1], gem[2], 230), emboss=False), faces=("front",))


def _acc_earrings(R, S, Lk, a):
    d = R.d
    p = metal(C.shades(a["color"] or "#d8a830", 5, 1.3), 6, glow=_glow4(a["glow"], 160))
    y = d.head_y + 1.9
    for sg in (1, -1):
        R.box("head", (sg * 4.2 - 0.5, y, -0.5), (1.0, 2.0, 1.0), p, faces=SIDES + ("bottom",))


def _acc_ear_cuffs(R, S, Lk, a):
    _acc_earrings(R, S, Lk, a)


def _acc_cigarette(R, S, Lk, a):
    d = R.d
    my = d.head_y + 8.0 - (26.5 / 32.0) * 8.0
    R.bone("cigarette", "head", (-1.4, my, -4.3))
    R.box("cigarette", (-1.6, my - 0.3, -7.4), (0.6, 0.6, 3.2), fabric(C.shades("#e8e4dc", 5, 0.5), 7, period=3.0, amp=0.05, seam=False), rotation=(-14, 0, 0), pivot=(-1.3, my, -4.3))
    em = C.rgb(a["glow"] or "#ff8a30")
    R.box("cigarette", (-1.65, my - 0.9, -7.9), (0.7, 0.7, 0.6), solid(em, (em[0], em[1], em[2], 255)), rotation=(-14, 0, 0), pivot=(-1.3, my, -4.3))


def _acc_pipe(R, S, Lk, a):
    d = R.d
    my = d.head_y + 8.0 - (26.5 / 32.0) * 8.0
    p = leather(C.shades(a["color"] or "#5a3c28", 5, 0.9), 3)
    R.box("head", (1.0, my - 0.4, -7.4), (0.8, 0.8, 3.4), p)
    R.box("head", (0.4, my - 0.4, -9.2), (2.0, 2.4, 2.0), p)
    em = C.rgb(a["glow"] or "#ff8a30")
    R.box("head", (0.9, my + 1.9, -8.7), (1.0, 0.3, 1.0), solid(em, (em[0], em[1], em[2], 200)))


def _acc_pocket_watch(R, S, Lk, a):
    d = R.d
    gold = metal(C.shades(a["color"] or "#d8a830", 5, 1.3), 6, glow=_glow4(a["glow"], 120))
    y = d.hip_y + 1.0
    R.bone("watch", "body", (d.tw / 2 - 1.5, y + 2.0, -d.td / 2 - 0.5))
    R.box("watch", (d.tw / 2 - 3.0, y - 3.4, -d.td / 2 - 1.1), (2.4, 2.4, 0.8), gold, faces=SIDES + ("top", "bottom"))
    R.box("watch", (d.tw / 2 - 2.4, y - 1.0, -d.td / 2 - 0.9), (0.5, 3.4, 0.4), gold, faces=SIDES)
    R.box("watch", (d.tw / 2 - 2.8, y - 4.0, -d.td / 2 - 1.18), (1.6, 1.6, 0.2), solid((236, 232, 220)), faces=("front",))


def _acc_shoulder_fur(R, S, Lk, a):
    d = R.d
    p = fur(C.shades(a["color"] or "#e8e4dc", 5, 0.9), 131, flow="y", root=0.5, cut="b", cut_depth=4)
    R.pair("arm_l", "arm_r", (d.arm_x - d.aw / 2 - 1.0, d.sh_y - 3.2, -d.ad / 2 - 1.2), (d.aw + 2.0, 4.2, d.ad + 2.4), p, faces=SIDES + ("top",))


def _acc_backpack(R, S, Lk, a):
    d = R.d
    p = leather(C.shades(a["color"] or "#5a3c28", 5, 0.9), 132)
    R.box("body", (-3.5, d.hip_y + 2.0, d.td / 2), (7.0, d.torso_h - 3.0, 3.6), p)
    R.box("body", (-3.0, d.hip_y + d.torso_h - 3.0, d.td / 2 + 3.4), (6.0, 2.0, 0.6), mat("cloth", a["color2"] or "#c8a040", 133))


def _acc_satchel(R, S, Lk, a):
    d = R.d
    p = leather(C.shades(a["color"] or "#5a3c28", 5, 0.9), 134)
    R.box("body", (d.tw / 2 - 0.4, d.hip_y - 3.0, -2.5), (2.4, 5.0, 5.0), p)
    R.box("body", (-d.tw / 2, d.hip_y + d.torso_h - 0.6, -d.td / 2 - 0.4), (d.tw, 0.9, d.td + 0.8), p, rotation=(0, 0, 30), pivot=(0, d.hip_y + d.torso_h - 0.6, 0), faces=SIDES)


def _acc_quiver(R, S, Lk, a):
    d = R.d
    p = leather(C.shades(a["color"] or "#4a3020", 5, 0.9), 135)
    R.box("body", (-2.5, d.hip_y + 3.0, d.td / 2 + 0.2), (3.0, d.torso_h * 0.7, 2.6), p, rotation=(0, 0, -16), pivot=(0, d.hip_y + d.torso_h * 0.6, d.td / 2))
    for k in range(3):
        R.box("body", (-2.2 + k * 0.9, d.hip_y + d.torso_h * 0.75 + 1.0 + k * 0.4, d.td / 2 + 0.8), (0.5, 3.0, 0.5), solid((200, 190, 160)), rotation=(0, 0, -16), pivot=(0, d.hip_y + d.torso_h * 0.6, d.td / 2))


def _acc_mask_band(R, S, Lk, a):
    d = R.d
    p = mat("cloth", a["color"] or "#1c1c24", 136, seam=False)
    R.box("head", (-4.25, d.head_y + 2.4, -4.25), (8.5, 2.8, 8.5), p, faces=SIDES)


def _acc_flower(R, S, Lk, a):
    d = R.d
    T = d.head_y + 8.0
    col = C.rgb(a["color"] or "#e86a9a")
    cen = C.rgb(a["color2"] or "#f2d070")
    for k in range(4):
        ang = k * 90.0
        R.box("head", (3.0 - 0.8, T - 1.4, -3.6), (1.6, 1.2, 1.2), solid(col, None, 6), rotation=(0, 0, ang), pivot=(3.0, T - 0.8, -3.0))
    R.box("head", (2.4, T - 1.2, -3.9), (1.2, 1.2, 1.0), solid(cen))


def _acc_ribbon(R, S, Lk, a):
    d = R.d
    T = d.head_y + 8.0
    p = mat("cloth", a["color"] or "#c83040", 137, seam=False)
    for sg in (1, -1):
        R.box("head", (3.4 * sg - 1.4 if sg > 0 else -4.8, T - 0.4, 1.0), (2.8, 2.0, 1.4), p)
    R.box("head", (-0.7, T - 0.2, 1.2), (1.4, 1.4, 1.2), p)


def _acc_veil(R, S, Lk, a):
    d = R.d
    T = d.head_y + 8.0
    p = mat("cloth", a["color"] or "#2a2038", 138, seam=False)
    R.box("head", (-4.7, T - 1.6, -4.7), (9.4, 1.8, 9.4), p, faces=SIDES + ("top",))
    R.box("head", (-4.7, d.sh_y - 3.0, 4.0), (9.4, 8.0, 0.8), p, faces=SIDES + ("bottom",))


def _acc_tassel(R, S, Lk, a):
    d = R.d
    R.bone("tassel", "body", (d.tw / 2 - 1.0, d.hip_y + 1.0, -d.td / 2))
    p = fabric(C.shades(a["color"] or "#c83040", 5, 0.9), 139, period=3.0, seam=False)
    R.box("tassel", (d.tw / 2 - 1.8, d.hip_y - 5.0, -d.td / 2 - 0.8), (1.6, 6.0, 0.8), p, faces=SIDES + ("bottom",))


def _acc_chain(R, S, Lk, a):
    d = R.d
    p = metal(C.shades(a["color"] or "#aeb8c8", 5, 1.3), 140, emboss=False)
    for sg in (1, -1):
        R.box("body", (sg * 1.8 - 0.4, d.hip_y + 1.0, -d.td / 2 - 0.5), (0.8, d.torso_h - 2.0, 0.5), p, rotation=(0, 0, -sg * 28), pivot=(sg * 1.8, d.hip_y + d.torso_h - 1.0, -d.td / 2), faces=("front", "left", "right"))


ACCESSORIES = {"glasses": _acc_glasses, "monocle": _acc_monocle, "eyepatch": _acc_eyepatch, "headband": _acc_headband, "bandana": _acc_bandana, "cap": _acc_cap,
               "beret": _acc_beret, "wizard_hat": _acc_wizard_hat, "witch_hat": _acc_witch_hat, "crown": _acc_crown, "tiara": _acc_tiara, "circlet": _acc_circlet,
               "earrings": _acc_earrings, "cigarette": _acc_cigarette, "pipe": _acc_pipe, "pocket_watch": _acc_pocket_watch, "shoulder_fur": _acc_shoulder_fur,
               "backpack": _acc_backpack, "quiver": _acc_quiver, "goggles": _acc_goggles, "mask_band": _acc_mask_band, "flower": _acc_flower, "ribbon": _acc_ribbon,
               "veil": _acc_veil, "ear_cuffs": _acc_ear_cuffs, "tassel": _acc_tassel, "chain": _acc_chain, "satchel": _acc_satchel}


# ------------------------------------------------------------------------------------------------------------------------ ears, horns
def build_ears(R, S, Lk, E):
    d = R.d
    kind = E["kind"]
    sk = Lk.skin
    col = E["color"]
    inner = E["inner"]
    sz = E["size"]
    T = d.head_y + 8.0
    skin_p = hair_paint(salt=3, cols=Lk.skin) if col is None and kind in ("elf", "pointed", "fin") else None
    from .core import skin_paint
    outer = skin_paint(Lk.skin, 2) if kind in ("elf", "pointed", "fin") and col is None else (hair_paint(salt=4, cols=C.shades(col or Lk.H[1], 4, 1.0)) if kind not in ("elf", "pointed", "fin") else skin_paint(C.skin_palette(col), 2))
    inn = solid(C.rgb(inner or mix(Lk.skin[2], (210, 100, 120), 0.5)))
    ey = d.head_y + 4.0
    if kind in ("elf", "pointed"):
        ln = (5.5 if kind == "elf" else 2.8) * sz
        for k in range(3):
            w = 1.6 - 0.45 * k
            R.pair("head", "head", (4.0 + k * 1.4 - 0.2, ey + 0.2 + k * 0.8, -0.8 + 0.0), (ln / 3.0 + 0.3, w, 1.2 - 0.25 * k), outer, rotation=(0, 0, 0), faces=SIDES + ("top", "bottom"))
    elif kind == "fin":
        R.pair("head", "head", (4.0, ey - 1.5, -1.5), (1.0, 5.0 * sz, 3.0), outer, faces=SIDES + ("top",))
    else:
        h = {"cat": 3.4, "wolf": 3.8, "fox": 4.2, "rabbit": 8.5}[kind] * sz
        w = {"cat": 3.0, "wolf": 3.0, "fox": 3.0, "rabbit": 2.6}[kind]
        x0 = {"cat": 2.6, "wolf": 3.0, "fox": 2.8, "rabbit": 1.8}[kind]
        lean = {"cat": 12.0, "wolf": 10.0, "fox": 8.0, "rabbit": 12.0}[kind]
        R.bone("ear_l", "head", (x0, T - 0.5, 0.0))
        R.bone("ear_r", "head", (-x0, T - 0.5, 0.0))
        n = 3 if kind != "rabbit" else 4
        for k in range(n):
            wk = max(0.7, w * (1.0 - k / float(n)) * 0.95)
            hk = h / n
            y = T - 0.5 + k * hk
            p = outer
            c = R.box("ear_l", (x0 - wk / 2 + k * 0.1, y, -0.6), (wk, hk + 0.2, 1.2), outer, rotation=(0, 0, -lean), pivot=(x0, T - 0.5, 0.0), faces=SIDES + ("top",))
            R.pk.twin(R.m, "ear_r", c)
            if k < n - 1:
                ci = R.box("ear_l", (x0 - wk / 2 + 0.5 + k * 0.1, y, -0.9), (max(0.5, wk - 1.0), hk + 0.2, 0.4), inn, rotation=(0, 0, -lean), pivot=(x0, T - 0.5, 0.0), faces=("front",))
                R.pk.twin(R.m, "ear_r", ci)


def build_horns(R, S, Lk, Hn):
    d = R.d
    kind = Hn["kind"]
    sz = Hn["size"]
    col = Hn["color"]
    g = _glow4(Hn["glow"], 200)
    p = bone_paint(C.shades(col, 5, 1.1), 141) if kind in ("antlers", "oni", "small") else metal(C.shades(col, 5, 1.0), 141, glow=g, emboss=False) if Hn["glow"] else bone_paint(C.shades(col, 5, 1.1), 141)
    T = d.head_y + 8.0
    R.bone("horns", "head", (0, T - 1.0, 0))
    if kind == "devil":
        for sg in (1, -1):
            base = (sg * 2.6, T - 0.4, -0.8)
            _horn_chain(R, "horns", base, sg, [(3.6, 10, -22), (3.4, 16, -20), (3.0, 20, -22), (2.4, 22, -24)], 2.2 * sz, p)
    elif kind == "ram":
        for sg in (1, -1):
            base = (sg * 3.4, T - 1.2, 0.2)
            _horn_chain(R, "horns", base, sg, [(3.0, 40, 12), (3.0, 40, 32), (3.0, 40, 52), (3.0, 30, 70), (2.5, 20, 80)], 2.6 * sz, p)
    elif kind == "oni":
        for sg in (1, -1):
            base = (sg * 2.0, T - 0.4, -2.4)
            _horn_chain(R, "horns", base, sg, [(3.2, 6, 4), (3.2, 8, 6)], 2.2 * sz, p)
    elif kind == "single":
        _horn_chain(R, "horns", (0.0, T - 0.4, -3.0), 1, [(4.0, 10, 40), (4.0, 10, 30), (3.5, 8, 25), (3.0, 6, 20)], 1.8 * sz, p, mirror=False)
    elif kind == "antlers":
        for sg in (1, -1):
            base = (sg * 2.4, T - 0.4, -0.4)
            _horn_chain(R, "horns", base, sg, [(5.0, 10, -8), (5.0, 12, -4), (4.0, 12, 4)], 1.6 * sz, p)
            for (ox, oy, ang) in ((0.0, 0.4, 50.0), (0.0, 0.7, 40.0)):
                pass
            _horn_chain(R, "horns", (sg * 4.0, T + 3.0, -0.4), sg, [(3.5, 35, -30), (3.0, 30, -20)], 1.0 * sz, p)
            _horn_chain(R, "horns", (sg * 5.0, T + 6.0, -0.4), sg, [(3.0, 28, -30), (2.6, 24, -20)], 0.9 * sz, p)
    elif kind == "bull":
        for sg in (1, -1):
            base = (sg * 4.2, T - 1.5, -1.0)
            _horn_chain(R, "horns", base, sg, [(3.4, 60, 6), (3.6, 40, 8), (3.4, 30, 10), (2.6, 24, 14)], 2.2 * sz, p)
    else:
        for sg in (1, -1):
            base = (sg * 2.8, T - 0.4, -1.0)
            _horn_chain(R, "horns", base, sg, [(2.2, 8, -18), (2.0, 10, -20)], 1.8 * sz, p)


def _horn_chain(R, bone, base, sg, segs, w, p, mirror=True):
    """A horn of tapering boxes: each segment is (length, outward lean, back lean) in degrees relative to the vertical; the lean accumulates into the next one."""
    x, y, z = base
    ax = az = 0.0
    wd = w
    for (ln, tz, tx) in segs:
        az += tz * sg
        ax += tx * 0.0 + (0.0)
        # tx: bend toward the back (positive: backwards)
        ax = -(tx)
        ln = r5(ln)
        wdr = max(0.6, r5(wd))
        R.box(bone, (x - wdr / 2, y, z - wdr / 2), (wdr, ln, wdr), p, rotation=(ax, 0, -az), pivot=(x, y, z))
        d_ = rot_file((0, ln, 0), ax, 0, -az)
        x, y, z = x + d_[0], y + d_[1], z + d_[2]
        wd *= 0.72


# ------------------------------------------------------------------------------------------------------------------------ wings, tail, halo
def build_wings(R, S, Lk, W):
    d = R.d
    kind = W["kind"]
    col = W["color"]
    col2 = W["color2"] or C.mix(col, (255, 255, 255), 0.5)
    g = _glow4(W["glow"], 150)
    span = W["span"]
    base_len = max(14.0, d.top * 0.55) * span
    P = (2.4, d.sh_y - 2.0, d.td / 2 + 0.8)
    for suf, sg in (("_l", 1), ("_r", -1)):
        R.bone("wing" + suf, "body", (sg * P[0], P[1], P[2]), rotation=(0, sg * -22.0 if not W["folded"] else sg * -70.0, 0))
        R.bone("wing_tip" + suf, "wing" + suf, (sg * P[0], P[1], P[2]))
    cols = C.shades(col, 5, 1.0)
    cols2 = C.shades(col2, 5, 1.0)
    if kind in ("feathered", "angel"):
        _wing_feathers(R, P, base_len, cols, cols2, g, rows=2 if kind == "angel" else 1, count=8 if kind == "angel" else 7)
    elif kind in ("bat", "dragon"):
        _wing_membrane(R, P, base_len, cols, cols2, g, dragon=(kind == "dragon"))
    elif kind == "fairy":
        _wing_fairy(R, P, base_len * 0.8, cols, cols2, g)
    else:
        _wing_blades(R, P, base_len, cols, cols2, g)
    R.facts["wings"] = kind


def _wing_feathers(R, P, L, cols, cols2, g, rows=2, count=7):
    for row in range(rows):
        cnt = count if row == 0 else count - 1
        for k in range(cnt):
            t = k / float(cnt - 1)
            a = 18.0 + t * 100.0                           # degrees from the +y axis toward +x (up, then out, then down)
            ln = L * (0.62 + 0.38 * SIN(math.pi * (0.1 + 0.9 * t))) * (0.55 if row == 1 else 1.0)
            ln = r5(ln)
            w = 4.2 if row == 0 else 4.8
            p = feather(cols if row == 0 else cols2, tip_glow=g, salt=k + row * 11, dither=0.05, tip_frac=0.2)
            bone = "wing_l" if t < 0.5 else "wing_tip_l"
            cz = P[2] + 0.35 * k + (-0.6 if row else 0.0)
            c = R.box(bone, (P[0] - w / 2, P[1], cz - 0.3), (w, ln, 0.6), p, rotation=(0, 0, -a), pivot=(P[0], P[1], cz), faces=("front", "back"), dens=4, key="wf%d_%d_%d" % (row, k, int(ln * 2)))
            R.pk.twin(R.m, bone[:-2] + "_r", c)


def _wing_membrane(R, P, L, cols, cols2, g, dragon=False):
    bone_p = bone_paint(cols2, 7)
    mem = fabric(cols, 8, period=5.0, amp=0.18, seam=False, cut="b", cut_depth=3)
    rays = [(22.0, 1.0), (52.0, 0.92), (78.0, 0.82), (100.0, 0.66)]
    for k, (a, f) in enumerate(rays):
        ln = r5(L * f)
        bone = "wing_l" if k < 2 else "wing_tip_l"
        c = R.box(bone, (P[0] - 0.5, P[1], P[2] - 0.5), (1.0, ln, 1.0), bone_p, rotation=(0, 0, -a), pivot=P)
        R.pk.twin(R.m, bone[:-2] + "_r", c)
        if k > 0:
            a0, f0 = rays[k - 1]
            am = (a + a0) / 2.0
            lm = r5(L * (f + f0) / 2.0 * 0.96)
            wid = max(4.0, 2 * lm * math.sin(math.radians((a - a0) / 2.0)) + 0.8)
            c2 = R.box(bone, (P[0] - wid / 2, P[1], P[2] - 0.2), (wid, lm, 0.4), mem, rotation=(0, 0, -am), pivot=P, faces=("front", "back"), dens=4)
            R.pk.twin(R.m, bone[:-2] + "_r", c2)
    if dragon:
        for k in range(3):
            c = R.box("wing_l", (P[0] - 0.5, P[1] + L * (0.25 + 0.2 * k), P[2] - 0.5), (1.0, 3.0, 1.0), bone_p, rotation=(0, 0, -(22 + 8 * k)), pivot=P)
            R.pk.twin(R.m, "wing_r", c)


def _wing_fairy(R, P, L, cols, cols2, g):
    glass = fabric(cols, 9, period=6.0, amp=0.12, seam=False)
    for k, (a, f, w) in enumerate(((20.0, 1.0, 5.0), (60.0, 0.8, 4.4), (112.0, 0.55, 3.6), (150.0, 0.45, 3.0))):
        ln = r5(L * f)
        bone = "wing_l" if k < 2 else "wing_tip_l"
        p = feather(cols if k % 2 == 0 else cols2, tip_glow=g or (200, 240, 255, 150), salt=k, tip_frac=0.5, shaft=False, barbs=False)
        c = R.box(bone, (P[0] - w / 2, P[1], P[2] - 0.2), (w, ln, 0.4), p, rotation=(0, 0, -a), pivot=P, faces=("front", "back"), dens=4)
        R.pk.twin(R.m, bone[:-2] + "_r", c)


def _wing_blades(R, P, L, cols, cols2, g):
    for k in range(5):
        a = 20.0 + k * 22.0
        ln = r5(L * (1.0 - 0.1 * k))
        p = metal(cols, 10 + k, glow=g, emboss=False)
        bone = "wing_l" if k < 3 else "wing_tip_l"
        c = R.box(bone, (P[0] - 1.1, P[1], P[2] - 0.4 - 0.5 * k), (2.2, ln, 0.8), p, rotation=(0, 0, -a), pivot=P)
        R.pk.twin(R.m, bone[:-2] + "_r", c)


def build_tail(R, S, Lk, Tl):
    """A tail of five rods on the bones tail1 .. tail5 (each the child of the one before, so a clip can wag and curl it): it leaves the base of the spine, slopes
    back and down, and curves by the kind: cat (curls up at the end), wolf and fox (bushy, hang), devil (thin, an arrow tip), lizard and dragon (thick, taper),
    rat (thin, bare)."""
    d = R.d
    kind = Tl["kind"]
    col = Tl["color"]
    col2 = Tl["color2"] or col
    g = _glow4(Tl["glow"], 200)
    n = 5
    seg = r5(d.top * 0.105 * Tl["length"])
    base = {"cat": (1.8, 30, (-6, -8, -4, 12, 30)), "wolf": (3.0, 38, (6, 6, 8, 6, 4)), "fox": (3.4, 28, (4, 6, 8, 10, 8)), "devil": (1.2, 24, (-4, -8, -6, 10, 16)),
            "lizard": (3.0, 18, (4, 5, 8, 10, 12)), "dragon": (3.4, 16, (4, 6, 10, 12, 14)), "rat": (1.2, 30, (6, 8, 8, 8, 8))}[kind]
    w0, beta, turns = base
    widths = {"fox": (0.85, 1.1, 1.2, 1.1, 0.7), "wolf": (0.85, 1.0, 1.05, 0.95, 0.6), "cat": (1.0, 1.0, 0.95, 0.9, 0.8), "devil": (1.0, 0.9, 0.8, 0.7, 0.6),
              "lizard": (1.0, 0.85, 0.7, 0.5, 0.35), "dragon": (1.0, 0.85, 0.7, 0.55, 0.4), "rat": (1.0, 0.9, 0.8, 0.7, 0.6)}[kind]
    furry = kind in ("wolf", "fox", "cat")
    py, pz = d.hip_y + 0.5, d.td / 2 - 0.2
    prev = "body"
    for k in range(n):
        name = "tail%d" % (k + 1)
        R.bone(name, prev, (0, py, pz))
        prev = name
        beta = max(-20.0, min(80.0, beta + turns[k]))
        w = max(0.8, r5(w0 * widths[k]))
        paint = (fur(C.shades(col2 if (kind == "fox" and k == n - 1) else col, 5, 1.0), 150 + k, flow="y", root=0.3) if furry else
                 scale_paint(C.shades(col, 5, 1.1), 150 + k) if kind in ("lizard", "dragon") else leather(C.shades(col, 5, 0.9), 150 + k, seam=False))
        rx = beta - 90.0
        R.box(name, (-w / 2, py - seg, pz - w / 2), (w, seg + 0.4, w), paint, rotation=(rx, 0, 0), pivot=(0, py, pz), faces=SIDES + ("top", "bottom"))
        dv = rot_file((0, -seg, 0), rx, 0, 0)
        py, pz = py + dv[1], pz + dv[2]
    if kind == "devil":
        R.box("tail5", (-1.5, py - 1.0, pz - 0.4), (3.0, 3.0, 0.8), metal(C.shades(col2, 5, 1.2), 155, glow=g, emboss=False), rotation=(rx, 0, 0), pivot=(0, py + 1.0, pz))
    R.facts["tail"] = kind


def halo_canvas(kind, r, color, glow):
    """The halo plate at 4 texels per pixel: a ring (with gaps for broken, spokes for gears, points for thorns / flames)."""
    n = int(r * 8)
    cv = Canvas(n, n)
    c0 = n / 2.0
    R_ = r * 4
    cols = C.shades(color, 5, 1.2)
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c0, y + 0.5 - c0
            rr = math.hypot(dx, dy) / R_
            ang = (math.degrees(math.atan2(dy, dx)) + 360.0) % 360.0
            on = False
            tone = 0.6
            if kind in ("ring", "broken"):
                if 0.84 <= rr <= 0.98:
                    on = True
                    tone = 0.85 if rr < 0.9 else 0.55
                    if kind == "broken" and 110 < ang < 160:
                        on = False
            elif kind == "gears":
                seg = ang % 15.0
                dseg = min(seg, 15.0 - seg)
                arc = rr * R_ * math.radians(dseg)
                if 0.84 <= rr <= 0.98:
                    on = True
                    tone = 0.8
                elif 0.5 <= rr < 0.84 and arc < 0.9:
                    on = True
                    tone = 0.6
                elif 0.5 <= rr < 0.54 or 0.98 < rr <= 1.0 and int(ang / 7.5) % 2 == 0:
                    on = True
                    tone = 0.75
            elif kind in ("thorns", "flames"):
                if 0.80 <= rr <= 0.90:
                    on = True
                    tone = 0.8
                spike = (ang % 30.0)
                tip = abs(spike - 15.0) / 15.0
                if 0.9 < rr <= 0.98 + 0.02 * 5 * (1.0 - tip) and tip < 0.55 - (rr - 0.9) * 4:
                    on = True
                    tone = 0.7 + 0.25 * (rr - 0.9) * 10
            if on:
                cv.put(x, y, pal(cols, tone + (0.04 if (x + y) % 2 else 0)), 255, glow, 215)
    return cv


def build_halo(R, S, Lk, Hl):
    d = R.d
    r = 7.0 * Hl["radius"]
    col = Hl["color"]
    g = C.rgb(Hl["glow"]) if Hl["glow"] else None
    cv = halo_canvas(Hl["kind"], r, col, g)
    T = d.head_y + 8.0
    cy = T + 3.0 + r * 0.4
    cz = 4.8 if Hl["behind"] else -0.0
    R.bone("halo", "head", (0, cy, cz))
    R.box("halo", (-r, cy - r, cz - 0.25), (2 * r, 2 * r, 0.5), canvas_plate(cv), faces=("front", "back"), dens=4)
    R.facts["halo"] = Hl["kind"]
