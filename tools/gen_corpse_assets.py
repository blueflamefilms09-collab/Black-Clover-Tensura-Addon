"""Soul Corpse Magic assets (Rades Spirito): the decay overlays that go on ANY body, and the three named puppets Jimmy, Alfredo and Carl.

    python3 -B tools/gen_corpse_assets.py              # everything
    python3 -B tools/gen_corpse_assets.py overlays     # only the overlays
    python3 -B tools/gen_corpse_assets.py jimmy carl   # only the named ones

Why this exists: the Frankenstein look of a Soul Corpse is two things. (1) A patterned overlay texture that client/corpse/CorpseDecayLayer
draws over the model of any creature that carries the nusmp:soul_corpse effect (stitches, patchwork discolouration, rot, exposed bone,
four stages that strengthen as the body decays) and a faint soul glow. (2) Three dedicated GeoLite models for the named puppets, each
with its own silhouette, four skins (stage 0 to 3, the same stage the effect carries) and a glow map per skin. Everything is drawn with
deterministic noise, so a run always gives the same files and stage N+1 is stage N plus more decay.

Writes (assets/nusmp, through tools/geo_builder.py for the models):
    textures/entity/corpse_decay_0..3.png          64x64 RGBA overlay, one per decay stage (tinted pale green-grey by the layer)
    textures/entity/corpse_soul.png                64x64 soft additive wisp texture for the glow pass
    geo/entity/corpse_<name>.geo.json              jimmy, alfredo, carl
    animations/entity/corpse_<name>.animation.json clips: idle, walk, run, attack, hurt, death (Carl also: cast)
    textures/entity/corpse_<name>.png, _s1.png, _s2.png, _s3.png and a *_glow.png for each (eyes, stitch light, Carl's soul core)

The roles of the three are INVENTED (canon only names them): Jimmy a lanky fast brawler, Alfredo a slow barrel-chested tank, Carl a thin
hooded caster with a bone-shard bandolier. See docs/exclusive/rs.md. Run python as `python3 -B`.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

S = math.sin
C = math.cos
TAU = 2 * math.pi


# =============================================================================================================== noise and colour helpers
def hsh(*k):
    """A deterministic hash of integers/strings to 0..1."""
    h = 2166136261
    for v in k:
        if isinstance(v, str):
            for ch in v:
                h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
        else:
            h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
        h ^= h >> 13
        h = (h * 0x5BD1E995) & 0xFFFFFFFF
        h ^= h >> 15
    return (h & 0xFFFFFF) / float(0x1000000)


def vn(x, y, salt=0):
    """Smooth value noise, 0..1."""
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = hsh(x0, y0, salt), hsh(x0 + 1, y0, salt)
    c, d = hsh(x0, y0 + 1, salt), hsh(x0 + 1, y0 + 1, salt)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(x, y, salt=0):
    return 0.55 * vn(x, y, salt) + 0.3 * vn(x * 2.1, y * 2.1, salt + 7) + 0.15 * vn(x * 4.3, y * 4.3, salt + 13)


def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def lerp(a, b, t):
    return tuple(a[k] + (b[k] - a[k]) * t for k in range(len(a)))


def ramp(cols, t):
    t = clamp(t) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    return lerp(cols[i], cols[i + 1], t - i)


def dim(c, k):
    return tuple(clamp(v * k, 0, 255) for v in c)


def rgb(c):
    return tuple(int(round(clamp(v, 0, 255))) for v in c)


def voronoi(x, y, cell, salt=0):
    """(distance to the nearest cell border in pixels-ish, cell id 0..1) for a patchwork."""
    gx, gy = x / cell, y / cell
    cx, cy = math.floor(gx), math.floor(gy)
    best, second, bid, sid = 9e9, 9e9, 0.0, 0.0
    for oy in range(-1, 2):
        for ox in range(-1, 2):
            px, py = cx + ox + hsh(cx + ox, cy + oy, salt) * 0.8 + 0.1, cy + oy + hsh(cx + ox, cy + oy, salt + 1) * 0.8 + 0.1
            d = math.hypot(gx - px, gy - py)
            h = hsh(cx + ox, cy + oy, salt + 2)
            if d < best:
                second, sid, best, bid = best, bid, d, h
            elif d < second:
                second, sid = d, h
    return (second - best) * cell * 0.5, bid, sid


def tone_of(cid):
    """Which of the three patch tones a cell id picks (0 grey-green, 1 bluish, 2 yellowed)."""
    return 0 if cid < 0.4 else 1 if cid < 0.72 else 2


# decay amounts per stage: how much of a surface is rotten, how much bone shows, how many stitches, how dark the dirt
ROT = (0.10, 0.24, 0.40, 0.58)
BONE_LIM = (9.0, 0.74, 0.65, 0.57)       # noise cut-off above which a surface is bare bone, per stage (about 0%, 3%, 12%, 30% of it)
DIRT = (0.0, 0.10, 0.22, 0.36)

# --- palette: pale green-grey dead skin, three patch tones, rot, bone, thread
SKIN = [(70, 84, 76), (98, 114, 100), (128, 144, 126), (160, 174, 152)]
SKIN_B = [(64, 78, 92), (88, 104, 120), (118, 134, 150), (150, 164, 176)]            # a bluish patch
SKIN_Y = [(92, 92, 66), (122, 122, 88), (152, 150, 112), (182, 178, 140)]             # a yellowed patch
ROTC = [(34, 40, 30), (52, 60, 40), (74, 84, 52)]
VEIN = (58, 44, 66)
BONEC = [(88, 80, 66), (176, 168, 146), (222, 214, 190), (240, 234, 214)]
THREAD = (22, 18, 18)
THREAD_L = (58, 48, 44)
WOUND = [(52, 18, 22), (92, 30, 34), (126, 46, 48)]
EYE_GLOW = (190, 235, 255)
SOUL_GLOW = (110, 210, 190)


# =============================================================================================================== the overlays for any body
def stitch_line(px, x0, y0, x1, y1, n_cross=3):
    """A thread stitched between two points: the thread itself and short cross-stitches over it (pixels set to opaque thread colour)."""
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for k in range(steps + 1):
        x = round(x0 + (x1 - x0) * k / steps)
        y = round(y0 + (y1 - y0) * k / steps)
        put(px, x, y, THREAD, 235)
    dx, dy = (x1 - x0) / steps, (y1 - y0) / steps
    nx, ny = -dy, dx
    for k in range(0, steps + 1, 2):
        x, y = x0 + dx * k, y0 + dy * k
        for s in (-1, 1):
            put(px, round(x + nx * s), round(y + ny * s), THREAD, 215)


def put(px, x, y, color, alpha):
    if 0 <= x < len(px[0]) and 0 <= y < len(px):
        px[y][x] = (color[0], color[1], color[2], alpha)


# regions of the humanoid 64x64 layout (x0, y0, w, h) of every face: used to put joint stitches where a person would have them
HUMANOID = {
    "head_front": (8, 8, 8, 8), "head_right": (0, 8, 8, 8), "head_left": (16, 8, 8, 8), "head_back": (24, 8, 8, 8),
    "body_front": (20, 20, 8, 12), "body_back": (32, 20, 8, 12),
    "rarm_front": (44, 20, 4, 12), "rarm_back": (52, 20, 4, 12), "rleg_front": (4, 20, 4, 12), "rleg_back": (12, 20, 4, 12),
    "larm_front": (36, 52, 4, 12), "larm_back": (44, 52, 4, 12), "lleg_front": (20, 52, 4, 12), "lleg_back": (28, 52, 4, 12),
}


def overlay(stage):
    W = H = 64
    px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]
    for y in range(H):
        for x in range(W):
            # patchwork: cells of three tones, borders are seams
            edge, cid, sid = voronoi(x + 0.5, y + 0.5, 12.0, 3)
            seam = tone_of(cid) != tone_of(sid)
            tone = (SKIN, SKIN_B, SKIN_Y)[tone_of(cid)]
            base = ramp(tone, 0.25 + 0.5 * fbm(x * 0.35, y * 0.35, 5))
            a = 40 + int(60 * (stage + 1) / 4.0 * (0.6 + 0.8 * hsh(int(x // 9), int(y // 9), 9)))     # the patches show through more as it rots
            col = base
            if cid < 0.4 and stage == 0:
                a = 18                                                                               # at stage 0 only some patches are marked at all
            n = fbm(x * 0.42, y * 0.42, 11)
            if n > 1.0 - ROT[stage]:                                                                  # rot: mottled green-brown, dark
                col = ramp(ROTC, (n - (1.0 - ROT[stage])) * 4.0)
                a = max(a, 150 + int(70 * stage / 3.0))
            v = abs(fbm(x * 0.5 + 40, y * 0.5, 17) - 0.5)
            if v < 0.025 + 0.01 * stage:                                                              # veins
                col = VEIN
                a = max(a, 130)
            b = fbm(x * 0.28 + 9, y * 0.28 + 5, 23)
            if stage > 0 and b > BONE_LIM[stage]:                                                     # exposed bone, with a dark torn rim
                col = ramp(BONEC, 0.35 + 0.65 * min(1.0, (b - BONE_LIM[stage]) / 0.12))
                a = 235
            elif stage > 0 and b > BONE_LIM[stage] - 0.03:
                col = ramp(WOUND, hsh(x, y, 4))
                a = 225
            if seam and edge < 0.7:                                                                   # seams between patches of different tone
                col = THREAD_L if edge > 0.35 else THREAD
                a = 230
            elif seam and edge < 1.5 and (x + y) % 2 == 0:
                col = THREAD
                a = max(a, 175)
            px[y][x] = (*rgb(col), min(255, a))
    # joint stitches where the humanoid layout has joints (a person-shaped body gets proper surgery)
    def seam_row(face, row, a=3, b=None):
        x0, y0, w, h = HUMANOID[face]
        stitch_line(px, x0 + a, y0 + row, x0 + w - 1 - (a if b is None else b), y0 + row)

    for f in ("rarm_front", "rarm_back", "larm_front", "larm_back"):
        seam_row(f, 5, 0, 0)
    for f in ("rleg_front", "rleg_back", "lleg_front", "lleg_back"):
        seam_row(f, 6, 0, 0)
    seam_row("body_front", 9, 0, 0)
    x0, y0, w, h = HUMANOID["body_front"]
    stitch_line(px, x0 + 4, y0 + 1, x0 + 4, y0 + 10)                                                  # the long surgery line down the chest
    x0, y0, w, h = HUMANOID["head_front"]
    stitch_line(px, x0 + 1, y0 + 2, x0 + 6, y0 + 3)                                                   # across the brow
    stitch_line(px, x0 + 6, y0 + 3, x0 + 6, y0 + 7)                                                   # down the cheek
    if stage >= 2:                                                                                    # ribs through the chest
        x0, y0, w, h = HUMANOID["body_front"]
        for r in range(3):
            for x in range(x0 + 1, x0 + w - 1):
                put(px, x, y0 + 1 + r * 2, BONEC[2], 235)
                put(px, x, y0 + 2 + r * 2, BONEC[0], 235)
        x0, y0, w, h = HUMANOID["head_front"]
        for x in range(x0 + 1, x0 + 7):
            put(px, x, y0 + 7, BONEC[2], 235 if x % 2 == 0 else 120)                                  # bared teeth
    img = Image.new("RGBA", (W, H))
    for y in range(H):
        for x in range(W):
            img.putpixel((x, y), px[y][x])
    return img


def soul_wisps():
    W = H = 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            n = fbm(x * 0.16, y * 0.1, 31)
            ridge = 1.0 - abs(n - 0.5) * 6.0                                                          # long soft ridges: smoke wisps
            if ridge <= 0:
                continue
            k = ridge * (0.4 + 0.6 * fbm(x * 0.3, y * 0.3, 37))
            c = lerp((60, 150, 140), (200, 245, 235), k)
            img.putpixel((x, y), (*rgb(c), int(255 * clamp(k * 0.9))))
    return img


# =============================================================================================================== a tiny box-uv sheet for the named models
class Sheet:
    """Shelf allocator for box-uv rectangles plus the list of boxes to paint (so the same layout is painted four times, once per stage)."""

    def __init__(self, w, h):
        self.W, self.H = w, h
        self.x = self.y = self.row = 0
        self.items = []

    def alloc(self, w, h):
        if self.x + w > self.W:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        if self.y + h > self.H:
            raise RuntimeError("texture sheet full")
        u, v = self.x, self.y
        self.x += w + 1
        self.row = max(self.row, h)
        return u, v


class Body:
    """A model with its boxes. box() lays one out and remembers how to paint it; twin() mirrors a box to the other side sharing its paint."""

    def __init__(self, name, tex=128):
        self.name = name
        self.m = Model("corpse", name, tex, tex)
        self.sheet = Sheet(tex, tex)
        self.tex = tex

    def bone(self, name, parent=None, pivot=(0, 0, 0)):
        self.m.bone(name, parent, pivot)

    def box(self, bone, origin, size, mat, inflate=0.0, rotation=None, pivot=None, **kw):
        X, Y, Z = [int(v) for v in size]
        assert all(float(v).is_integer() for v in size), (bone, size)
        u, v = self.sheet.alloc(2 * (X + Z), Z + Y)
        c = self.m.cube(bone, origin, (X, Y, Z), uv=(u, v), inflate=inflate, rotation=rotation, pivot=pivot)
        self.sheet.items.append(dict(u=u, v=v, X=X, Y=Y, Z=Z, mat=mat, kw=kw, id=len(self.sheet.items)))
        return c

    def twin(self, bone, c, **_):
        """The mirror of a cube on the other side (x -> -x): same uv, mirror = true."""
        o = c["origin"]
        rot = c.get("rotation")
        piv = c.get("pivot")
        return self.m.cube(bone, (-(o[0] + c["size"][0]), o[1], o[2]), tuple(c["size"]), uv=tuple(c["uv"]), inflate=c.get("inflate", 0.0),
                           rotation=None if not rot else (rot[0], -rot[1], -rot[2]), pivot=None if not piv else (-piv[0], piv[1], piv[2]), mirror=True)

    def paint(self, stage):
        img = Image.new("RGBA", (self.tex, self.tex), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex, self.tex), (0, 0, 0, 0))
        for it in self.sheet.items:
            u, v, X, Y, Z = it["u"], it["v"], it["X"], it["Y"], it["Z"]
            faces = {"top": (u + Z, v, X, Z), "bottom": (u + Z + X, v, X, Z), "right": (u, v + Z, Z, Y), "front": (u + Z, v + Z, X, Y),
                     "left": (u + Z + X, v + Z, Z, Y), "back": (u + 2 * Z + X, v + Z, X, Y)}
            for face, (fu, fv, fw, fh) in faces.items():
                for j in range(fh):
                    for i in range(fw):
                        c, g = it["mat"](Pix(face, i, j, fw, fh, it["id"], stage, X, Y, Z), **it["kw"])
                        img.putpixel((fu + i, fv + j), (*rgb(c), 255))
                        if g is not None:
                            glow.putpixel((fu + i, fv + j), (*rgb(g[:3]), int(g[3])))
        return img, glow


class Pix:
    """One texel of one face of one box: where it is on the face, how far down the box, which stage, and a noise coordinate that is continuous over the box."""

    def __init__(self, face, i, j, fw, fh, bid, stage, X, Y, Z):
        self.face, self.i, self.j, self.fw, self.fh, self.bid, self.stage = face, i, j, fw, fh, bid, stage
        self.X, self.Y, self.Z = X, Y, Z
        fid = {"top": 0, "bottom": 1, "right": 2, "front": 3, "left": 4, "back": 5}[face]
        self.fid = fid
        # a coordinate system per face that is continuous across the box edges well enough for patchwork
        self.x = i + 0.5 + 17 * fid + 5 * bid
        self.y = j + 0.5 + 11 * bid
        self.t = (j + 0.5) / fh                      # 0 at the top of a side face, 1 at the bottom
        self.edge = min(i, fw - 1 - i, j, fh - 1 - j)


def shade_face(p, c):
    k = {"top": 1.08, "bottom": 0.7, "front": 1.0, "back": 0.9, "left": 0.86, "right": 0.86}[p.face]
    return dim(c, k)


# =============================================================================================================== materials
def skin(p, tones=(0.0, 0.4, 0.72), salt=1, bone_prone=0.0, wound=0.0, cell=9.0, glow_seams=True, tint=1.0):
    """Dead patchwork skin: three tones of patches, thread seams, rot, veins, wounds, and bone where the body has torn open."""
    edge, cid, sid = voronoi(p.x, p.y, cell, salt)
    tone = SKIN if cid < tones[1] else SKIN_B if cid < tones[2] else SKIN_Y
    seam = (cid < tones[1]) != (sid < tones[1]) or (cid < tones[2]) != (sid < tones[2])
    c = ramp(tone, 0.2 + 0.6 * fbm(p.x * 0.4, p.y * 0.4, salt + 2))
    c = dim(c, tint)
    g = None
    n = fbm(p.x * 0.45, p.y * 0.45, salt + 3)
    if n > 1.0 - ROT[p.stage] * 0.8:
        c = ramp(ROTC, (n - (1.0 - ROT[p.stage] * 0.8)) * 4.0)
    if abs(fbm(p.x * 0.5 + 40, p.y * 0.5, salt + 5) - 0.5) < 0.03:
        c = lerp(c, VEIN, 0.8)
    if wound and fbm(p.x * 0.5, p.y * 0.6, salt + 8) > 1.0 - wound:
        c = ramp(WOUND, hsh(int(p.x), int(p.y), salt))
    if bone_prone and p.stage > 0:
        b = fbm(p.x * 0.3 + 9, p.y * 0.3 + 5, salt + 11)
        lim = 0.5 + (BONE_LIM[p.stage] - 0.5) / bone_prone
        if b > lim:
            c = ramp(BONEC, 0.3 + 0.7 * min(1.0, (b - lim) / 0.12))
            if hsh(int(p.x), int(p.y), salt + 12) > 0.93:
                c = BONEC[0]
        elif b > lim - 0.035:
            c = ramp(WOUND, hsh(int(p.x), int(p.y), salt + 13))
    if seam and edge < 0.65:
        c = THREAD if edge < 0.35 else THREAD_L
        if glow_seams:
            g = (*SOUL_GLOW, 30 + 14 * p.stage)
    elif seam and edge < 1.4 and (int(p.x) + int(p.y)) % 2 == 0:
        c = lerp(c, THREAD, 0.7)
    c = dim(c, 1.0 - DIRT[p.stage] * 0.6)
    return shade_face(p, c), g


def cloth(p, ramp_cols, salt=2, tear=0.0, hem=False, stripes=0, under=None):
    """Worn cloth or leather: a woven grain, dirt that deepens with the stage, torn holes (showing 'under' or a dark hole) and a frayed hem."""
    grain = 0.5 + 0.25 * S(p.x * 1.6 + salt) * C(p.y * 1.3) + 0.25 * (fbm(p.x * 0.5, p.y * 0.5, salt) - 0.5) * 2
    c = ramp(ramp_cols, grain)
    if stripes and int(p.x) % stripes == 0:
        c = dim(c, 0.82)
    c = dim(c, 1.0 - DIRT[p.stage] * 1.1)
    holes = tear + 0.07 * p.stage
    if holes and fbm(p.x * 0.5, p.y * 0.5, salt + 5) > 1.0 - holes:
        c = under if under is not None else (14, 12, 14)
    if hem and p.face in ("front", "back", "left", "right") and p.t > 0.8 and hsh(int(p.x), int(p.y), salt + 6) < (p.t - 0.8) * 4.0 * (0.5 + 0.2 * p.stage):
        c = (16, 14, 16)
    if p.edge == 0:
        c = dim(c, 0.84)
    return shade_face(p, c), None


def boneMat(p, salt=4):
    grain = fbm(p.x * 0.7, p.y * 0.35, salt)
    c = ramp(BONEC[1:], 0.25 + 0.7 * grain)
    if abs(fbm(p.x * 0.8, p.y * 0.8, salt + 3) - 0.5) < 0.03:
        c = BONEC[0]
    c = dim(c, 1.0 - DIRT[p.stage] * 0.4)
    return shade_face(p, c), None


def metal(p, salt=6, rust=True):
    c = ramp([(46, 48, 54), (88, 92, 100), (134, 138, 146)], 0.3 + 0.5 * fbm(p.x * 0.6, p.y * 0.3, salt))
    if rust and fbm(p.x * 0.5, p.y * 0.5, salt + 2) > 0.62 - 0.05 * p.stage:
        c = ramp([(70, 38, 24), (118, 62, 32), (150, 84, 40)], hsh(int(p.x), int(p.y), salt))
    if p.edge == 0:
        c = dim(c, 0.78)
    return shade_face(p, c), None


def bolt(p):
    c, _ = metal(p, 9, False)
    if p.edge >= 1 or (p.i == p.fw // 2 and p.j == p.fh // 2):
        c = dim(c, 1.25)
    return c, None


def face_mat(p, eyes, salt=7, brow=0.0, mouth_row=None, jaw_bone=0.0, eye_socket=None, bone_prone=0.5):
    """A head: skin everywhere; on the front face dark eye sockets with a glow in them, a stitched mouth and (rotting) bared teeth."""
    c, g = skin(p, salt=salt, bone_prone=bone_prone, wound=0.08)
    if p.face == "front":
        for (ex, ey, ew, eh) in eyes:
            if ex <= p.i < ex + ew and ey <= p.j < ey + eh:
                c = (16, 16, 20)
                g = (*EYE_GLOW, 255)
                if eye_socket and p.i == ex and p.j == ey and ew > 1:
                    g = (*EYE_GLOW, 150)
            elif ex - 1 <= p.i < ex + ew + 1 and ey - 1 <= p.j < ey + eh + 1:
                c = dim(c, 0.55)
        if mouth_row is not None and p.j == mouth_row:
            c = THREAD if p.i % 2 == 0 else dim(THREAD_L, 0.9)
            g = None
        if mouth_row is not None and p.j == mouth_row + 1 and p.stage >= 2 and p.i % 2 == 1:
            c = BONEC[2]
        if brow and p.j == 1:
            c = dim(c, 0.75)
    return c, g


def glow_dot(p, c, color, alpha=255):
    return c, (*color, alpha)


# =============================================================================================================== animation helpers
def kf(*pairs):
    return {t: v for t, v in pairs}


def loop(a, clip, length):
    a.clip(clip, length, True)


def cyc(a, clip, bone, kind, L, fn, n=1, steps=12):
    frames = {}
    for k in range(steps + 1):
        frames[L * k / steps] = tuple(fn(TAU * n * k / steps))
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def sw(base, amp, ph=0.0):
    return lambda th: tuple(base[k] + amp[k] * S(th + ph) for k in range(3))


def humanoid_anims(a, bones, p):
    """Shared clips for the three bodies; p tunes the amplitudes. bones: dict role -> bone name (arm_l, arm_r, leg_l, leg_r, body, head, ...)."""
    b = bones
    # idle: breathing, a slow sway, the head lolling
    loop(a, "idle", 2.4)
    cyc(a, "idle", b["body"], "rot", 2.4, sw((p["lean"], 0, 0), (1.2, 0, 0.8)))
    cyc(a, "idle", b["head"], "rot", 2.4, sw((p["head"], 0, 0), (2.0, 3.0, 2.0), 0.9))
    cyc(a, "idle", b["arm_l"], "rot", 2.4, sw((p["arm_rest"], 0, -4), (3.0, 0, 1.5), 0.4))
    cyc(a, "idle", b["arm_r"], "rot", 2.4, sw((p["arm_rest"], 0, 4), (3.0, 0, 1.5), 1.6))
    # walk and run: opposite arms and legs, a drag of the feet
    for name, L, amp, lean, bob in (("walk", 1.0, p["walk"], p["lean"] - 2, 0.5), ("run", 0.6, p["run"], p["lean"] - 12, 1.0)):
        loop(a, name, L)
        cyc(a, name, b["leg_l"], "rot", L, sw((0, 0, 0), (amp, 0, 0)))
        cyc(a, name, b["leg_r"], "rot", L, sw((0, 0, 0), (-amp, 0, 0)))
        cyc(a, name, b["arm_l"], "rot", L, sw((p["arm_rest"], 0, -3), (-amp * 0.8, 0, 0)))
        cyc(a, name, b["arm_r"], "rot", L, sw((p["arm_rest"], 0, 3), (amp * 0.8, 0, 0)))
        cyc(a, name, b["body"], "rot", L, sw((lean, 0, 0), (1.5, 3.0, 0)), n=2)
        cyc(a, name, b["body"], "pos", L, lambda th, bob=bob: (0, abs(S(th)) * bob, 0), n=1)
        cyc(a, name, b["head"], "rot", L, sw((p["head"] + (6 if name == "run" else 0), 0, 0), (2.0, 4.0, 1.5), 0.6), n=2)
    # hurt: a flinch back
    a.clip("hurt", 0.4, False)
    a.rot("hurt", b["body"], kf((0.0, (p["lean"], 0, 0)), (0.1, (p["lean"] + 14, 0, 4)), (0.4, (p["lean"], 0, 0))))
    a.rot("hurt", b["head"], kf((0.0, (p["head"], 0, 0)), (0.1, (p["head"] - 16, 0, 5)), (0.4, (p["head"], 0, 0))))
    a.rot("hurt", b["arm_l"], kf((0.0, (p["arm_rest"], 0, -4)), (0.1, (p["arm_rest"] - 25, 0, -14)), (0.4, (p["arm_rest"], 0, -4))))
    a.rot("hurt", b["arm_r"], kf((0.0, (p["arm_rest"], 0, 4)), (0.1, (p["arm_rest"] - 25, 0, 14)), (0.4, (p["arm_rest"], 0, 4))))
    # death: topples backward and lies there (the renderer plays it once across the death ticks)
    a.clip("death", 1.0, False)
    a.rot("death", b["root"], kf((0.0, (0, 0, 0)), (0.35, (14, 0, 2)), (1.0, (88, 0, 6))))
    a.rot("death", b["body"], kf((0.0, (p["lean"], 0, 0)), (1.0, (0, 0, 0))))
    a.rot("death", b["head"], kf((0.0, (p["head"], 0, 0)), (1.0, (-20, 12, 6))))
    a.rot("death", b["arm_l"], kf((0.0, (p["arm_rest"], 0, -4)), (1.0, (-70, 0, -40))))
    a.rot("death", b["arm_r"], kf((0.0, (p["arm_rest"], 0, 4)), (1.0, (-30, 0, 55))))
    a.rot("death", b["leg_l"], kf((0.0, (0, 0, 0)), (1.0, (-8, 0, -6))))
    a.rot("death", b["leg_r"], kf((0.0, (0, 0, 0)), (1.0, (10, 0, 7))))


# =============================================================================================================== JIMMY: lanky fast brawler
def lx(side, center, width):
    """Origin x of a box of 'width' whose centre is 'center' on the left (+x) side, or the mirror of it on the right."""
    return center - width / 2.0 if side == "l" else -center - width / 2.0


def make_jimmy():
    B = Body("jimmy", 128)
    m = B.m
    # 31 px tall, hunched, arms down to the knees, a ragged jacket, bandaged forearms, bony claws. The right side is painted like the left.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (2.5, 13, 0))
    m.bone("shin_l", "leg_l", (2.5, 7, 0))
    m.bone("leg_r", "root", (-2.5, 13, 0))
    m.bone("shin_r", "leg_r", (-2.5, 7, 0))
    m.bone("body", "root", (0, 13, 0))
    m.bone("chest", "body", (0, 18, 0))
    m.bone("head", "chest", (0, 24, -1))
    m.bone("jaw", "head", (0, 24.6, -2.4))
    m.bone("arm_l", "chest", (5.5, 22, 0))
    m.bone("fore_l", "arm_l", (5.5, 15, 0))
    m.bone("arm_r", "chest", (-5.5, 22, 0))
    m.bone("fore_r", "arm_r", (-5.5, 15, 0))
    m.bone("coat", "body", (0, 15, 1.8))

    leg_skin = lambda p: skin(p, salt=21, bone_prone=0.8)
    jeans = lambda p: cloth(p, [(34, 40, 52), (58, 68, 86), (82, 94, 114)], salt=22, tear=0.05, hem=True, under=SKIN[1])
    jacket = lambda p: cloth(p, [(46, 30, 24), (80, 52, 38), (112, 76, 54)], salt=23, tear=0.06, hem=True, under=SKIN[0])
    wrap = lambda p: cloth(p, [(120, 112, 92), (170, 160, 132), (204, 194, 166)], salt=24, tear=0.04, stripes=3, under=SKIN[0])
    hair = lambda p: cloth(p, [(12, 10, 12), (26, 22, 24), (48, 40, 40)], salt=25)

    # legs (painted on the left, mirrored for the right): torn jeans on the thigh, a bandaged shin, a bare stitched foot
    c = B.box("leg_l", (lx("l", 2.5, 3), 7, -2), (3, 6, 4), jeans)
    B.twin("leg_r", c)
    c = B.box("shin_l", (lx("l", 2.5, 3), 2, -1.5), (3, 5, 3), wrap)
    B.twin("shin_r", c)
    c = B.box("shin_l", (lx("l", 2.5, 4), 0, -3.5), (4, 2, 5), leg_skin)
    B.twin("shin_r", c)
    # torso
    B.box("body", (-3.5, 13, -2), (7, 5, 4), lambda p: skin(p, salt=26, bone_prone=1.0, wound=0.1))
    B.box("chest", (-4, 18, -2), (8, 5, 4), lambda p: skin(p, salt=27, bone_prone=1.2, wound=0.1))
    B.box("chest", (-4.5, 17.5, 1.9), (9, 6, 1), jacket)                                                                    # jacket back
    B.box("chest", (-4.5, 17.5, -2.9), (2, 6, 1), jacket)                                                                   # open front flaps
    B.box("chest", (2.5, 17.5, -2.9), (2, 6, 1), jacket)
    B.box("chest", (-5.2, 22.5, -2.2), (4, 1, 5), jacket)                                                                   # shoulder pads
    B.box("chest", (1.2, 22.5, -2.2), (4, 1, 5), jacket)
    B.box("coat", (-4, 9, 0), (8, 6, 1), jacket)                                                                            # the ragged tail
    B.box("chest", (-1, 20, -2.6), (2, 1, 1), bolt)                                                                         # a staple across the chest seam
    B.box("chest", (-1, 23, -1), (2, 1, 2), lambda p: skin(p, salt=28))
    # head: tilted, a low brow, an empty socket, a crooked jaw, a ridge of hair
    B.box("head", (-3, 24, -4), (6, 6, 6), lambda p: face_mat(p, eyes=[(1, 2, 2, 2), (4, 2, 1, 1)], salt=29, brow=1.0, mouth_row=4, eye_socket=True))
    B.box("jaw", (-2, 23, -3.8), (4, 2, 5), lambda p: skin(p, salt=30, bone_prone=1.4))
    B.box("head", (-0.5, 30, -3), (1, 1, 6), hair)
    # arms
    c = B.box("arm_l", (lx("l", 5.5, 3), 15, -1.5), (3, 7, 3), lambda p: skin(p, salt=31, bone_prone=1.1, wound=0.1))
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 3), 7, -1.5), (3, 8, 3), wrap)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 4), 3, -2), (4, 4, 4), lambda p: skin(p, salt=32, bone_prone=0.9))
    B.twin("fore_r", c)
    for fx in (0, 1, 2):
        c = B.box("fore_l", (lx("l", 5.5, 4) + 0.5 + fx * 1.3, 0, -1.5), (1, 3, 1), boneMat)
        B.twin("fore_r", c)

    a = Anim()
    P = dict(lean=-10, head=14, arm_rest=-6, walk=36, run=54)
    bones = dict(root="root", body="body", head="head", arm_l="arm_l", arm_r="arm_r", leg_l="leg_l", leg_r="leg_r")
    humanoid_anims(a, bones, P)
    for nm, L, k in (("walk", 1.0, 40), ("run", 0.6, 70)):
        cyc(a, nm, "shin_l", "rot", L, lambda th, k=k: (-k * max(0.0, C(th)), 0, 0))
        cyc(a, nm, "shin_r", "rot", L, lambda th, k=k: (-k * max(0.0, -C(th)), 0, 0))
        cyc(a, nm, "fore_l", "rot", L, lambda th: (30 + 16 * max(0.0, S(th)), 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (30 + 16 * max(0.0, -S(th)), 0, 0))
    cyc(a, "idle", "fore_l", "rot", 2.4, sw((12, 0, 0), (3, 0, 0), 0.5))
    cyc(a, "idle", "fore_r", "rot", 2.4, sw((12, 0, 0), (3, 0, 0), 1.9))
    # attack: a lunge and a raking swing of the right claw, the left follows low
    a.clip("attack", 0.7, True)
    a.rot("attack", "body", kf((0.0, (-10, 0, 0)), (0.2, (-2, -22, 0)), (0.34, (-26, 28, 0)), (0.52, (-18, 8, 0)), (0.7, (-10, 0, 0))))
    a.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.2, (0, -1.2, 2)), (0.34, (0, 0.5, -4)), (0.7, (0, 0, 0))))
    a.rot("attack", "arm_r", kf((0.0, (-6, 0, 4)), (0.2, (-150, 0, 10)), (0.34, (40, 0, -12)), (0.52, (30, 0, -8)), (0.7, (-6, 0, 4))))
    a.rot("attack", "fore_r", kf((0.0, (30, 0, 0)), (0.2, (60, 0, 0)), (0.34, (8, 0, 0)), (0.7, (30, 0, 0))))
    a.rot("attack", "arm_l", kf((0.0, (-6, 0, -4)), (0.2, (-20, 0, -20)), (0.34, (70, 0, -8)), (0.52, (50, 0, -8)), (0.7, (-6, 0, -4))))
    a.rot("attack", "fore_l", kf((0.0, (30, 0, 0)), (0.34, (20, 0, 0)), (0.7, (30, 0, 0))))
    a.rot("attack", "head", kf((0.0, (14, 0, 0)), (0.2, (24, -8, 0)), (0.34, (6, 10, 0)), (0.7, (14, 0, 0))))
    a.rot("attack", "jaw", kf((0.0, (0, 0, 0)), (0.2, (8, 0, 0)), (0.34, (34, 0, 0)), (0.52, (6, 0, 0)), (0.7, (0, 0, 0))))
    a.rot("attack", "leg_l", kf((0.0, (0, 0, 0)), (0.2, (-10, 0, 0)), (0.34, (30, 0, 0)), (0.7, (0, 0, 0))))
    a.rot("attack", "leg_r", kf((0.0, (0, 0, 0)), (0.2, (16, 0, 0)), (0.34, (-24, 0, 0)), (0.7, (0, 0, 0))))
    return B, a


# =============================================================================================================== ALFREDO: barrel-chested tank
def make_alfredo():
    B = Body("alfredo", 128)
    m = B.m
    # 41 px tall, 16 wide. Short thick legs, a belly and a barrel chest, a small head sunk between hunched shoulders, fists like anvils.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (4, 14, 0))
    m.bone("leg_r", "root", (-4, 14, 0))
    m.bone("body", "root", (0, 14, 0))
    m.bone("chest", "body", (0, 20, 0))
    m.bone("head", "chest", (0, 33, -1.5))
    m.bone("jaw", "head", (0, 34, -3))
    m.bone("arm_l", "chest", (9.5, 30, 0))
    m.bone("fore_l", "arm_l", (9.5, 21, 0))
    m.bone("arm_r", "chest", (-9.5, 30, 0))
    m.bone("fore_r", "arm_r", (-9.5, 21, 0))

    skin_a = lambda p: skin(p, salt=41, bone_prone=0.7, wound=0.06, cell=11)
    trousers = lambda p: cloth(p, [(40, 38, 34), (68, 64, 56), (96, 90, 78)], salt=42, tear=0.08, hem=True, under=SKIN[1])
    leather = lambda p: cloth(p, [(34, 22, 16), (62, 40, 28), (90, 60, 40)], salt=43, tear=0.03)
    plate = lambda p: metal(p, 44)
    patch = lambda p: skin(p, salt=45, bone_prone=0.6, cell=8, tones=(0.0, 0.34, 0.66))

    # legs: trousers to the knee, a bare shin, an iron band, a big foot
    c = B.box("leg_l", (lx("l", 4, 6), 8, -3), (6, 6, 6), trousers)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 6), 2, -3), (6, 6, 6), lambda p: skin(p, salt=46, bone_prone=0.8, cell=9))
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 8), 4, -4), (8, 1, 8), plate)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 6), 0, -5), (6, 2, 8), skin_a)
    B.twin("leg_r", c)
    # belly, belt, staples
    B.box("body", (-7, 16, -4), (14, 4, 8), patch)
    B.box("body", (-7.5, 14, -4.5), (15, 2, 9), leather)
    B.box("body", (-1.5, 13.8, -5.1), (3, 3, 1), plate)
    for k in range(3):
        B.box("body", (-5 + k * 4, 16.5, -4.8), (1, 3, 1), bolt)
    # barrel chest, harness, shoulder plates
    B.box("chest", (-8, 20, -4.5), (16, 11, 9), skin_a)
    B.box("chest", (-8.4, 24, -4.9), (4, 7, 10), leather)
    B.box("chest", (4.4, 24, -4.9), (4, 7, 10), leather)
    B.box("chest", (-1, 20.5, -5.0), (2, 8, 1), leather)
    B.box("chest", (-2, 26, -5.4), (4, 3, 1), plate)
    c = B.box("chest", (lx("l", 10.2, 5), 29, -3.5), (5, 3, 7), plate)
    B.twin("chest", c)
    # neck, head, jaw, bolts
    B.box("chest", (-3, 31, -3), (6, 2, 6), skin_a)
    B.box("head", (-3, 33, -4.5), (6, 6, 6), lambda p: face_mat(p, eyes=[(1, 2, 1, 1), (4, 2, 1, 1)], salt=47, brow=1.0, mouth_row=4, bone_prone=0.6))
    B.box("jaw", (-3, 32, -5.2), (6, 2, 5), lambda p: skin(p, salt=48, bone_prone=1.2))
    for k in (0, 1):
        B.box("head", (-2.5 + k * 3.4, 39, -2.0), (1, 2, 1), bolt)
    # arms: a heavy upper arm, a scarred forearm with an iron band, a fist wrapped at the knuckles
    c = B.box("arm_l", (lx("l", 9.5, 6), 21, -3.5), (6, 9, 7), patch)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 7), 14, -4), (7, 7, 8), lambda p: skin(p, salt=49, bone_prone=0.7, wound=0.08))
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 8), 17, -4.5), (8, 2, 9), plate)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 8), 7, -4.5), (8, 7, 9), lambda p: skin(p, salt=50, bone_prone=0.5, cell=7))
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 8), 11, -4.7), (8, 2, 9), leather)
    B.twin("fore_r", c)

    a = Anim()
    P = dict(lean=-4, head=6, arm_rest=-4, walk=26, run=36)
    bones = dict(root="root", body="body", head="head", arm_l="arm_l", arm_r="arm_r", leg_l="leg_l", leg_r="leg_r")
    humanoid_anims(a, bones, P)
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (14, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (14, 0, 0))
    # attack: both fists up over the head and a double hammer slam
    a.clip("attack", 1.0, True)
    a.rot("attack", "body", kf((0.0, (-4, 0, 0)), (0.3, (14, 0, 0)), (0.5, (-26, 0, 0)), (0.75, (-14, 0, 0)), (1.0, (-4, 0, 0))))
    a.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.3, (0, 1, 3)), (0.5, (0, -2, -3)), (1.0, (0, 0, 0))))
    a.rot("attack", "arm_l", kf((0.0, (-4, 0, -4)), (0.3, (-170, 0, -8)), (0.5, (30, 0, -4)), (0.75, (10, 0, -4)), (1.0, (-4, 0, -4))))
    a.rot("attack", "arm_r", kf((0.0, (-4, 0, 4)), (0.3, (-170, 0, 8)), (0.5, (30, 0, 4)), (0.75, (10, 0, 4)), (1.0, (-4, 0, 4))))
    a.rot("attack", "fore_l", kf((0.0, (14, 0, 0)), (0.3, (40, 0, 0)), (0.5, (6, 0, 0)), (1.0, (14, 0, 0))))
    a.rot("attack", "fore_r", kf((0.0, (14, 0, 0)), (0.3, (40, 0, 0)), (0.5, (6, 0, 0)), (1.0, (14, 0, 0))))
    a.rot("attack", "head", kf((0.0, (6, 0, 0)), (0.3, (-8, 0, 0)), (0.5, (22, 0, 0)), (1.0, (6, 0, 0))))
    a.rot("attack", "jaw", kf((0.0, (0, 0, 0)), (0.3, (4, 0, 0)), (0.5, (30, 0, 0)), (0.8, (0, 0, 0))))
    a.rot("attack", "leg_l", kf((0.0, (0, 0, 0)), (0.5, (-12, 0, 0)), (1.0, (0, 0, 0))))
    a.rot("attack", "leg_r", kf((0.0, (0, 0, 0)), (0.5, (12, 0, 0)), (1.0, (0, 0, 0))))
    return B, a


# =============================================================================================================== CARL: hooded caster
def make_carl():
    B = Body("carl", 128)
    m = B.m
    # 30 px tall and thin. A long dark-violet coat open at the front over an exposed ribcage with a glowing soul in it, a deep hood, a bandolier of bone
    # shards, one hand held out with shards turning round it, and a ring of shards turning behind the head.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (2, 12, 0))
    m.bone("leg_r", "root", (-2, 12, 0))
    m.bone("body", "root", (0, 12, 0))
    m.bone("chest", "body", (0, 17, 0))
    m.bone("head", "chest", (0, 24, 0))
    m.bone("arm_l", "chest", (5, 22, 0))
    m.bone("fore_l", "arm_l", (5, 15, 0))
    m.bone("arm_r", "chest", (-5, 22, 0))
    m.bone("fore_r", "arm_r", (-5, 15, 0))
    m.bone("skirt_f", "body", (0, 13, -2.7))
    m.bone("skirt_b", "body", (0, 13, 2.2))
    m.bone("orbit", "head", (0, 27, 1.5))
    m.bone("orbit_hand", "fore_r", (-5, 7, -1))

    coat = lambda p: cloth(p, [(24, 18, 40), (44, 32, 66), (68, 52, 96)], salt=61, tear=0.05, hem=True, under=(10, 8, 16))
    coat_in = lambda p: cloth(p, [(14, 10, 22), (28, 20, 40), (44, 32, 62)], salt=62, tear=0.03, hem=True, under=(8, 6, 12))
    leg_skin = lambda p: skin(p, salt=63, bone_prone=1.6, tint=0.9)
    bandage = lambda p: cloth(p, [(120, 112, 92), (170, 160, 132), (204, 194, 166)], salt=64, tear=0.05, stripes=3, under=SKIN[0])

    c = B.box("leg_l", (lx("l", 2, 3), 0, -1.5), (3, 12, 3), leg_skin)
    B.twin("leg_r", c)
    # spine, ribcage, soul core
    B.box("body", (-3, 12, -1.5), (6, 5, 3), lambda p: skin(p, salt=65, bone_prone=2.2, wound=0.1))
    B.box("chest", (-3.5, 17, -2), (7, 6, 4), rib_mat)
    B.box("chest", (-1.2, 18.5, -1.6), (2, 2, 2), soul_core)
    B.box("chest", (-1, 17, 1.6), (2, 6, 1), boneMat)
    # coat: shoulders, back, two front panels, skirts
    B.box("chest", (-4.5, 21.5, -2.6), (9, 2, 5), coat)
    B.box("chest", (-4.5, 15.5, 2.0), (9, 8, 1), coat_in)
    B.box("chest", (-4.5, 13, -2.8), (2, 10, 1), coat)
    B.box("chest", (2.5, 13, -2.8), (2, 10, 1), coat)
    B.box("skirt_f", (-3.5, 3, -0.5), (3, 10, 1), coat)
    B.box("skirt_f", (0.5, 3, -0.5), (3, 10, 1), coat)
    B.box("skirt_b", (-4, 3, -0.5), (8, 10, 1), coat_in)
    c = B.box("body", (lx("l", 3.6, 1), 3, -2), (1, 10, 4), coat)
    B.twin("body", c)
    # bandolier of bone shards across the chest
    for k in range(5):
        B.box("chest", (-3 + k * 1.4, 21.0 - k * 1.1, -3.0), (1, 3, 1), boneMat, rotation=(0, 0, -25), pivot=(-3 + k * 1.4, 21.0 - k * 1.1, -3.0))
    B.box("chest", (-4, 19.8, -2.9), (8, 1, 1), lambda p: cloth(p, [(60, 40, 26), (90, 62, 40), (118, 84, 56)], salt=66), rotation=(0, 0, -25), pivot=(0, 20, -2.9))
    # neck, face, hood
    B.box("chest", (-1, 23, -1), (2, 1, 2), lambda p: skin(p, salt=67))
    B.box("head", (-3, 24, -3), (6, 6, 6), lambda p: face_mat(p, eyes=[(1, 3, 1, 1), (4, 3, 1, 1)], salt=68, mouth_row=5, bone_prone=0.9))
    B.box("head", (-3.5, 24, -3.5), (7, 7, 7), coat)
    B.box("head", (-1.5, 30.5, 0.0), (3, 3, 4), coat, rotation=(-24, 0, 0), pivot=(0, 30.5, 2))
    # arms in sleeves, bony hands
    c = B.box("arm_l", (lx("l", 5, 3), 15, -1.5), (3, 7, 3), coat)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 5, 3), 8, -1.5), (3, 7, 3), coat)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5, 2), 4, -1.0), (2, 4, 2), boneMat)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5, 3), 7.5, -1.6), (3, 1, 3), bandage)
    B.twin("fore_r", c)
    # shards: a ring turning behind the head, three round the right hand
    for k in range(4):
        B.box("orbit", (-0.5, 24, 5.0), (1, 5, 1), boneMat, rotation=(0, k * 90, 0), pivot=(0, 24, 1.5))
    for k in range(3):
        B.box("orbit_hand", (-6.0, 5, -4.2), (1, 4, 1), boneMat, rotation=(0, k * 120, 0), pivot=(-5, 5, -1))

    a = Anim()
    P = dict(lean=-4, head=4, arm_rest=-6, walk=34, run=52)
    bones = dict(root="root", body="body", head="head", arm_l="arm_l", arm_r="arm_r", leg_l="leg_l", leg_r="leg_r")
    humanoid_anims(a, bones, P)
    for nm, L in (("idle", 2.4), ("walk", 1.0), ("run", 0.6)):
        a.rot(nm, "orbit", {L * k / 8: (0, 360.0 * k / 8 * (1 if L == 2.4 else 2), 0) for k in range(9)})
        a.rot(nm, "orbit_hand", {L * k / 6: (0, 360.0 * k / 6, 0) for k in range(7)})
        cyc(a, nm, "skirt_f", "rot", L, sw((0, 0, 0), (-(6 if nm != "idle" else 2), 0, 0), 0.3))
        cyc(a, nm, "skirt_b", "rot", L, sw((0, 0, 0), ((6 if nm != "idle" else 2), 0, 0), 0.3))
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (18, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (18, 0, 0))
    cyc(a, "idle", "fore_l", "rot", 2.4, sw((10, 0, 0), (3, 0, 0), 0.5))
    cyc(a, "idle", "fore_r", "rot", 2.4, sw((10, 0, 0), (3, 0, 0), 1.9))
    # attack: a stab with the shard hand; cast: the arm held out, the shards spinning up
    a.clip("attack", 0.6, True)
    a.rot("attack", "arm_r", kf((0.0, (-6, 0, 4)), (0.2, (-110, 0, 6)), (0.34, (80, 0, 2)), (0.6, (-6, 0, 4))))
    a.rot("attack", "fore_r", kf((0.0, (10, 0, 0)), (0.2, (60, 0, 0)), (0.34, (0, 0, 0)), (0.6, (10, 0, 0))))
    a.rot("attack", "body", kf((0.0, (-4, 0, 0)), (0.2, (4, 0, 0)), (0.34, (-14, 0, 0)), (0.6, (-4, 0, 0))))
    a.rot("attack", "orbit_hand", {0.6 * k / 6: (0, 720.0 * k / 6, 0) for k in range(7)})
    a.clip("cast", 0.8, True)
    a.rot("cast", "arm_r", kf((0.0, (-6, 0, 4)), (0.25, (95, 0, 12)), (0.8, (100, 0, 14))))
    a.rot("cast", "fore_r", kf((0.0, (10, 0, 0)), (0.25, (8, 0, 0)), (0.8, (6, 0, 0))))
    a.rot("cast", "arm_l", kf((0.0, (-6, 0, -4)), (0.25, (40, 0, -30)), (0.8, (46, 0, -34))))
    a.rot("cast", "body", kf((0.0, (-4, 0, 0)), (0.25, (-8, 0, 0)), (0.8, (-10, 0, 0))))
    a.rot("cast", "head", kf((0.0, (4, 0, 0)), (0.25, (10, 0, 0)), (0.8, (12, 0, 0))))
    a.rot("cast", "orbit_hand", {0.8 * k / 8: (0, 1080.0 * k / 8, 0) for k in range(9)})
    a.rot("cast", "orbit", {0.8 * k / 8: (0, 720.0 * k / 8, 0) for k in range(9)})
    return B, a


def rib_mat(p):
    """The open ribcage: a dark body cavity crossed by pale ribs."""
    if p.face in ("front", "back"):
        if p.j % 2 == 1 and 0 < p.i < p.fw - 1:
            return shade_face(p, ramp(BONEC[1:], 0.4 + 0.5 * hsh(p.i, p.j, 3))), None
        return shade_face(p, (20, 12, 20)), (*SOUL_GLOW, 40) if p.face == "front" and 1 < p.i < p.fw - 2 else None
    return boneMat(p)


def soul_core(p):
    """A small glowing soul in the chest."""
    g = (*SOUL_GLOW, 255)
    return (150, 235, 215), g


MODELS = {"jimmy": make_jimmy, "alfredo": make_alfredo, "carl": make_carl}


def build(name):
    B, anim = MODELS[name]()
    B.m.save(anim)
    for stage in range(4):
        img, glow = B.paint(stage)
        suffix = "" if stage == 0 else "_s%d" % stage
        tex_name = "corpse_%s%s" % (name, suffix)
        d = os.path.join(ROOT, "textures", "entity")
        os.makedirs(d, exist_ok=True)
        img.save(os.path.join(d, tex_name + ".png"))
        glow.save(os.path.join(d, tex_name + "_glow.png"))
    print("%s: %d cubes, texture sheet used %d rows" % (name, B.m.cube_count(), B.sheet.y + B.sheet.row))


def build_overlays():
    d = os.path.join(ROOT, "textures", "entity")
    os.makedirs(d, exist_ok=True)
    for s in range(4):
        overlay(s).save(os.path.join(d, "corpse_decay_%d.png" % s))
    soul_wisps().save(os.path.join(d, "corpse_soul.png"))
    print("overlays: 4 stages + wisps")


if __name__ == "__main__":
    want = sys.argv[1:] or ["overlays", "jimmy", "alfredo", "carl"]
    if "overlays" in want:
        build_overlays()
    for n in MODELS:
        if n in want:
            build(n)
