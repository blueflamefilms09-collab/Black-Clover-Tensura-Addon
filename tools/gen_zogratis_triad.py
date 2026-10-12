"""The Dark Triad of the Spade Kingdom (Dante, Vanica, Zenon Zogratis), their Dark Disciples and the Blood Beasts, built in the repo's GeoLite
pipeline like the Direwolf / Tower Wizard (tools/geo_builder.py -> client.geo.GeoDraw):

    python3 -B tools/gen_zogratis_triad.py               # everything
    python3 -B tools/gen_zogratis_triad.py dante_base    # only the named models (names below)
    python3 -B tools/gen_zogratis_triad.py --manifest out.json    # everything, and a manifest of files / bones / clips / sizes

Models (GeoSpec.of("zogratis", "<name>")), original pixel-art interpretations of the manga designs:
    dante_base dante_hybrid      Dante: broad, crown, swept-back hair, cross scar marks, goatee, green eyes, brown tunic, red scarf (Body Magic);
                                 hybrid = 100% Lucifero: violet skin patches, horns through the crown, glowing violet eyes, a gravity halo
    vanica_base vanica_hybrid    Vanica: slim, crown, black bob, red eye + black eyepatch with a gold emblem, open smile, belted short dress
                                 with a ring buckle, black sleeves + thigh-high boots; hybrid = 100% Megicula: glowing curse sigils, horns,
                                 long red claws, a halo of floating blood droplets
    zenon_base zenon_hybrid      Zenon: lean, crown, short black hair, vertical scar with cross bars through the forehead and one eye, pale eyes,
                                 maroon belted vest; hybrid = 100% Beelzebub: bone spines and ribs through the cloak, a bone crest, insect wing
                                 remnants, hollow glowing eyes
    disciple                     one hooded humanoid (Dark Disciple); the devil tint is a texture variant: disciple_lucifero / _megicula /
                                 _beelzebub / _astaroth (GeoSpec.of("zogratis", "disciple").skin("zogratis_disciple_lucifero"))
    blood_beast                  a four-legged beast made of living blood: lion-wolf build, mane of blood thorns, dripping, glowing red veins

Writes for each <name> (through tools/geo_builder.py):
    assets/nusmp/geo/entity/zogratis_<name>.geo.json
    assets/nusmp/animations/entity/zogratis_<name>.animation.json
    assets/nusmp/textures/entity/zogratis_<name>.png          humanoids 512x512 at 2 texels per model pixel (so a face is 16x16 texels), beast 256x256
    assets/nusmp/textures/entity/zogratis_<name>_glow.png     eyes, jewels, curses, devil marks, veins (additive, full bright)
and tools/render_preview/scenes.d/zogratis_triad.json (python3 -B tools/render_preview/preview.py triad_dante_base_views ...).

Humanoid clips (same names on every humanoid): idle walk run attack cast cast_big hurt transform death. Rotation signs of this frame (checked in the
preview): positive x swings a hanging limb FORWARD (arms raise forward, a lunge), negative x folds a knee / leans the body forward; positive z swings a
hanging limb towards +x (the model's left); positive y turns the head towards the model's right.
Every cube has a whole or half number size (2 texels per pixel) so the texels stay square.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from spirit_geo import Sheet, nz, fz, hsh, rgba  # noqa: E402

KEY = "zogratis"
S = 2                                                      # texels per model pixel on the humanoids
SIN, COS, TAU = math.sin, math.cos, 2 * math.pi
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "zogratis_triad.json")


# ------------------------------------------------------------------------------------------------------------- colour helpers
def A3(c):
    return np.array(c[:3], np.float32)


def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def smooth(a, b, x):
    t = clamp((x - a) / (b - a)) if b != a else (1.0 if x >= a else 0.0)
    return t * t * (3 - 2 * t)


def ramp(cols, t):
    """Colour along a list of colours, t in 0..1."""
    t = clamp(t) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return A3(cols[i]) * (1 - f) + A3(cols[i + 1]) * f


def mixc(a, b, t):
    return A3(a) * (1 - t) + A3(b) * t


def px(c, a=255):
    return rgba(c, a)


def r5(v):
    """Round to the nearest half pixel (a box side must be a multiple of 0.5 at 2 texels per pixel)."""
    return round(v * 2) / 2.0


FACE_K = {"top": 1.08, "bottom": 0.62, "front": 1.0, "back": 0.9, "left": 0.96, "right": 0.96}
SIDES = ("front", "back", "left", "right")


# ------------------------------------------------------------------------------------------------------------- the packer
class Pack(Sheet):
    """Boxes are collected and painted at finish(), tallest first. A box of size (X, Y, Z) model pixels takes (2(X+Z) S) x ((Z+Y) S) texels and is
    written with an explicit per-face uv (so the texel density is S per pixel). Twins reuse the uv of a painted box (mirror = true)."""

    def __init__(self, w, h, dens=S):
        super().__init__(w, h)
        self.dens = dens
        self.q = []
        self.tw = []

    def box(self, m, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None, mirror=False):
        size = tuple(max(1.0 / self.dens, round(v * self.dens) / self.dens) for v in size)          # whole texels
        tex = tuple(int(round(v * self.dens)) for v in size)
        c = m.cube(bone, tuple(origin), tuple(size), uv=(0, 0), inflate=inflate, rotation=rotation, pivot=pivot, mirror=mirror)
        self.q.append((c, tex, paint))
        return c

    def twin(self, m, bone, c):
        """The mirrored copy of a cube on the other side of the model (x -> -x), sharing its painted rectangle."""
        sx = c["size"][0]
        o = c["origin"]
        rot = c.get("rotation")
        piv = c.get("pivot")
        t = m.cube(bone, (-(o[0] + sx), o[1], o[2]), tuple(c["size"]), uv=(0, 0), inflate=c.get("inflate", 0.0),
                   rotation=None if not rot else (rot[0], -rot[1], -rot[2]), pivot=None if not rot else (-piv[0], piv[1], piv[2]), mirror=True)
        self.tw.append((t, c))
        return t

    @staticmethod
    def face_uv(u, v, X, Y, Z):
        return {"east": {"uv": [u, v + Z], "uv_size": [Z, Y]}, "north": {"uv": [u + Z, v + Z], "uv_size": [X, Y]},
                "west": {"uv": [u + Z + X, v + Z], "uv_size": [Z, Y]}, "south": {"uv": [u + 2 * Z + X, v + Z], "uv_size": [X, Y]},
                "up": {"uv": [u + Z, v], "uv_size": [X, Z]}, "down": {"uv": [u + Z + X, v], "uv_size": [X, Z]}}

    def finish(self):
        def key(k):
            c, (X, Y, Z), _ = self.q[k]
            return (-(Z + Y), -(2 * (X + Z)), k)
        self.placed = []
        for k in sorted(range(len(self.q)), key=key):
            c, (X, Y, Z), paint = self.q[k]
            u, v = self.alloc(2 * (X + Z), Z + Y)
            c["uv"] = self.face_uv(u, v, X, Y, Z)
            self.placed.append((u, v, X, Y, Z, paint))
        for t, src in self.tw:
            t["uv"] = json.loads(json.dumps(src["uv"]))
        self.paint_all()

    def paint_all(self):
        """(Re)paint every box at its place with the painters as they behave now (a painter may read a module-level palette)."""
        self.img[:] = 0
        self.glow[:] = 0
        for (u, v, X, Y, Z, paint) in self.placed:
            faces = {"top": (u + Z, v, X, Z), "bottom": (u + Z + X, v, X, Z), "right": (u, v + Z, Z, Y), "front": (u + Z, v + Z, X, Y),
                     "left": (u + Z + X, v + Z, Z, Y), "back": (u + 2 * Z + X, v + Z, X, Y)}
            for name, (fu, fv, fw, fh) in faces.items():
                for j in range(fh):
                    for i in range(fw):
                        cc, g = paint(name, i, j, fw, fh)
                        self._put(fu + i, fv + j, cc, g)


class Build:
    """A model under construction: bones, boxes (with mirrored pairs), animation."""

    def __init__(self, name, tw, th, dens=S):
        self.m = Model(KEY, name, tw, th)
        self.pk = Pack(tw, th, dens)
        self.A = Anim()
        self.name = name
        self.raise_by = 0.0
        self.variants = None                                     # optional hook(build, texture_height): more textures for the same model

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def box(self, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None):
        return self.pk.box(self.m, bone, origin, size, paint, inflate, rotation, pivot)

    def pair(self, bone_l, bone_r, origin, size, paint, inflate=0.0, rotation=None, pivot=None):
        """origin is the +x cube; the -x twin goes on bone_r (use the same bone twice for a single bone)."""
        c = self.pk.box(self.m, bone_l, origin, size, paint, inflate, rotation, pivot)
        self.pk.twin(self.m, bone_r, c)
        return c

    def raise_upper(self, dy):
        """The upper body is designed with the hips at y = 10; this lifts everything above them (everything but the root and the legs) by dy, so the
        legs (built in real coordinates) can be longer without touching a single upper-body number."""
        for n, b in self.m.bones.items():
            if n == "root" or n.startswith(("leg_", "shin_")):
                continue
            b["pivot"][1] += dy
            for c in b["cubes"]:
                c["origin"][1] += dy
                if "pivot" in c:
                    c["pivot"][1] += dy

    def finish(self, raise_by=None):
        if raise_by:
            self.raise_upper(raise_by)
        self.pk.finish()
        img, glow = self.pk.images()
        return img, glow

    def write_files(self):
        """The geo and animation files, compact (no indentation): the per-face uvs make the indented form several times bigger."""
        for sub, ext, data in (("geo", ".geo.json", self.m.geo()), ("animations", ".animation.json", self.A.data())):
            os.makedirs(os.path.join(ROOT, sub, "entity"), exist_ok=True)
            with open(os.path.join(ROOT, sub, "entity", self.m.id + ext), "w") as f:
                json.dump(data, f, separators=(",", ":"))

    def save(self, img, glow):
        used = self.pk.used()
        hh = 64
        while hh < used:
            hh *= 2
        if hh < self.pk.H:                                      # the sheet is only as tall as the boxes need
            img, glow = img.crop((0, 0, self.pk.W, hh)), glow.crop((0, 0, self.pk.W, hh))
            self.m.tex_h = hh
        self.write_files()
        self.m.save_texture(img, glow)
        if self.variants:
            self.variants(self, hh)
        print(self.m.id, "cubes", self.m.cube_count(), "verts/pass", self.m.cube_count() * 24, "texture", self.pk.W, "x", hh, "(rows used", used, ")")


# ------------------------------------------------------------------------------------------------------------- painters (texel space)
def bevel(c, face, i, j, fw, fh, k=0.07):
    """A very light edge: the first row / column catches light, the last one sits in shadow (side faces only)."""
    if face not in SIDES or fw < 5 or fh < 5:
        return c
    if j == 0 or i == 0:
        return c * (1 + k)
    if j == fh - 1 or i == fw - 1:
        return c * (1 - k)
    return c


def strand_id(across, along, salt, width=2.0, wander=1.0):
    x = across / width + wander * SIN(along * 0.13 + across * 0.31 + salt) / width * 2 + 0.4 * SIN(along * 0.37 + salt * 2.3)
    sid = math.floor(x)
    return sid, x - sid


def cloth(base, dark, light, salt=0, folds=0, seam=True, bevel_k=0.05, weave=1.0, lining=None, fold_amp=1.0):
    """Woven cloth: fine thread noise, soft vertical folds, a dark seam on the first row. lining: colour of the faces that face the body."""
    base, dark, light = A3(base), A3(dark), A3(light)

    def p(face, i, j, fw, fh):
        w = (2.2 if (i + j) % 2 == 0 else -2.2) * weave
        c = base + w + fz(i, j, salt) * 12 + nz(i * 0.4, j * 0.4, salt) * 8
        if face == "top":
            return px(c * 1.1), None
        if face == "bottom":
            return px(c * 0.6), None
        if folds:
            f = 0.5 + 0.5 * SIN(i * TAU * folds / max(6, fw) + salt * 1.7 + 0.35 * SIN(j * 0.25))
            c = c * (1.0 - 0.2 * fold_amp + 0.28 * fold_amp * f) + (light - base) * 0.4 * max(0.0, f - 0.78) * 3 * fold_amp
        c = c * (1.0 - 0.1 * (j / max(1, fh - 1)))
        if lining is not None and face == "front":
            c = A3(lining) + fz(i, j, salt + 3) * 8
        if seam and j == 0:
            c = dark
        return px(bevel(c, face, i, j, fw, fh, bevel_k)), None
    return p


def leather(base=(70, 48, 36), hi=(104, 74, 54), salt=0, stitch=(150, 118, 78), buckle=None, wrinkles=0.0, edge=True):
    base, hi = A3(base), A3(hi)

    def p(face, i, j, fw, fh):
        c = base + fz(i, j, salt) * 14 + nz(i * 0.5, j * 0.5, salt) * 10
        t = 0.5 + 0.5 * SIN(j * 0.9 + salt + 2 * SIN(i * 0.17))
        if wrinkles:
            c = c * (1 - wrinkles * 0.18 * t) + (hi - base) * wrinkles * 0.2 * (1 - t) * 0.5
        if face == "top":
            return px(c * 1.15), None
        if face == "bottom":
            return px(c * 0.55), None
        if edge and (j == 0 or j == fh - 1):
            c = c * 0.72
        elif stitch is not None and fh >= 4 and (j == 2 or j == fh - 3) and i % 2 == 0:
            c = A3(stitch)
        if buckle is not None and face == "front":
            r = buckle(i, j, fw, fh)
            if r is not None:
                return r
        return px(bevel(c, face, i, j, fw, fh, 0.05)), None
    return p


def skin(salt=0, tone=((236, 210, 190), (214, 178, 160), (248, 228, 214)), blush=0.0):
    base, dark = A3(tone[0]), A3(tone[1])

    def p(face, i, j, fw, fh):
        c = base * (0.95 + 0.06 * (nz(i * 0.3, j * 0.3, salt) + 0.5)) + fz(i, j, salt + 2) * 3
        v = j / max(1, fh - 1)
        c = c * (1 - 0.1 * v) + (dark - base) * 0.2 * v
        if face == "bottom":
            c = c * 0.7
        if blush and face in ("front", "left", "right"):
            c = c + np.array((14, -6, -4), np.float32) * blush
        return px(bevel(c, face, i, j, fw, fh, 0.04)), None
    return p


def fur(salt=1, cols=None, root=0.5, tip=0.35, saw=0, width=2.0, lift=0.0, deep=0.0, bottom_dark=True, wander=1.0):
    """Mantle fur: strands that wander a little as they hang, each with its own tone, dark between them and at the root, pale at the tip.
    saw: how many rows at the bottom of the side faces end in ragged strand tips (transparent) instead of a straight edge."""
    cols = cols or FUR_RAMP

    def p(face, i, j, fw, fh):
        along = j
        across = i
        sid, frac = strand_id(across, along, salt, width, wander)
        h = hsh(sid, salt, 3)
        v = j / max(1, fh - 1)
        t = 0.5 + 0.3 * h + 0.07 * SIN(along * 0.35 + h * 9) + lift
        if frac < 0.28:
            t -= 0.12
        elif frac > 0.78:
            t += 0.06
        if hsh(sid, j // 3, salt + 17) > 0.9:
            t += 0.18
        t += nz(i * 0.2, j * 0.2, salt) * 0.25
        t -= root * (1 - v) ** 2 * 0.55 + deep
        t += tip * v ** 2 * 0.5
        if face in ("top", "bottom"):
            c = ramp(cols, t) * (1.05 if face == "top" else 0.55)
            return px(c), None
        c = ramp(cols, t) * FACE_K[face]
        if saw and j >= fh - saw:
            ln = fh - saw * (0.15 + 0.85 * hsh(sid, salt, 5))
            if j > ln:
                return None, None
            c = c * 1.05
        return px(c), None
    return p


def hair(salt=1, base=(22, 22, 30), hi=(70, 74, 108), lo=(8, 8, 12), width=2.0, sheen=0.4):
    base, hi, lo = A3(base), A3(hi), A3(lo)

    def p(face, i, j, fw, fh):
        sid, frac = strand_id(i, j, salt, width, 1.4)
        h = hsh(sid, salt, 3)
        v = j / max(1, fh - 1)
        c = base * (0.75 + 0.5 * h)
        if frac < 0.3:
            c = c * 0.6 + lo * 0.4
        elif 0.45 < frac < 0.7 and h > 0.35:
            c = c + (hi - base) * sheen * (0.4 + 0.6 * h) * (1 - 0.5 * v)
        c = c + fz(i, j, salt + 6) * 5
        if face == "top":
            c = c * 1.15 + (hi - base) * 0.2 * (0.5 + nz(i * 0.2, j * 0.2, salt))
        if face == "bottom":
            c = c * 0.6
        return px(bevel(c, face, i, j, fw, fh, 0.04)), None
    return p


def bone_p(salt=0, glow=0, tip_light=True, base=None):
    """Bone: ivory at the tip, stained darker towards the root, growth rings."""
    ramp_b = base or BONE_RAMP

    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = (0.86 - 0.5 * v) if tip_light else 0.7
        t += fz(i, j, salt) * 0.16 + (0.0 if (i + j) % 5 else -0.07)
        c = ramp(ramp_b, t) * (0.92 + 0.08 * FACE_K[face])
        if face == "bottom":
            c = c * 0.7
        g = px(BONE_GLOW, glow * (0.3 + 0.9 * (1 - v) ** 1.5)) if glow else None
        return px(c), g
    return p


def solid(c, g=None, noise=6, salt=0):
    c = A3(c)

    def p(face, i, j, fw, fh):
        return px(c * FACE_K[face] + fz(i, j, salt) * noise), (px(g[:3], g[3]) if g else None)
    return p


def metal(base=(222, 180, 66), hi=(255, 226, 130), lo=(138, 96, 34), salt=0, jewel=None):
    """Gold: bright upper rows, dark lower rows, engraved dashes; jewel(i, j, fw, fh) may return (colour, glow) for a gem on the front face."""
    base, hi, lo = A3(base), A3(hi), A3(lo)

    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        c = ramp([hi, base, lo], 0.15 + 0.7 * v) + fz(i, j, salt) * 10
        if face == "top":
            c = hi * 0.95
        if face == "bottom":
            c = lo * 0.7
        if face == "front" and fh >= 4 and j == fh // 2 and i % 4 < 2:
            c = c * 0.82
        if jewel is not None and face in ("front", "back", "left", "right"):
            r = jewel(face, i, j, fw, fh)
            if r is not None:
                return r
        return px(bevel(c, face, i, j, fw, fh, 0.08)), None
    return p


def gem_front(col=(220, 40, 56), glow=(255, 90, 90)):
    """A square gem on the middle of the front face of a crown band."""
    def jewel(face, i, j, fw, fh):
        if face != "front":
            return None
        cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
        if abs(i - cx) <= 1.0 and abs(j - cy) <= 1.0:
            k = 1.0 if (i == int(cx) and j == int(cy)) else 0.7
            return px(A3(col) * k + (40 if k == 1.0 else 0)), px(glow, 150)
        return None
    return jewel


# palettes ------------------------------------------------------------------------------------------------------------
FUR_RAMP = [(86, 80, 90), (140, 135, 142), (188, 184, 188), (228, 224, 224), (250, 246, 242)]
BONE_RAMP = [(92, 80, 62), (160, 144, 112), (214, 202, 168), (240, 232, 204)]
BONE_GLOW = (160, 190, 255)
CLOAK = (30, 28, 38)
CLOAK_D = (12, 11, 17)
CLOAK_L = (84, 82, 108)
GOLD, GOLD_L, GOLD_D = (222, 180, 66), (255, 228, 130), (136, 92, 34)


# ------------------------------------------------------------------------------------------------------------- faces
def face_art(rows, legend, base, back=None):
    """The head's front face as 16 x 16 characters. legend: char -> (r, g, b) or ((r, g, b), (r, g, b, a) glow); '.' and unknown chars use base."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]

    def p(face, i, j, fw, fh):
        if face == "front":
            e = legend.get(rows[j][i])
            if e is not None:
                if isinstance(e[0], tuple):
                    return px(e[0]), px(e[1][:3], e[1][3])
                return px(e), None
        return base(face, i, j, fw, fh)
    return p


def head_paint(art, skin_p, hair_p, ear=(150, 120, 110), sideburn=3, hair_rows=9, back_rows=12, extra=None):
    """Head: the front face from the art, the sides and back mostly hair (the hair cubes cover them), an ear on each side, skin underneath.
    sideburn: columns of skin at the front of the sides; hair_rows: rows of the side hair; extra(face, i, j, fw, fh) may override a texel."""
    def p(face, i, j, fw, fh):
        if face == "front":
            r = art(face, i, j, fw, fh)
        elif face in ("left", "right"):
            f = i if face == "left" else fw - 1 - i                       # distance from the front edge
            skin_here = (f < sideburn and j >= 2) or j >= hair_rows
            if 5 <= f <= 7 and 7 <= j <= 11:                              # the ear
                e = A3(ear) * (0.78 if (f == 5 or j == 7) else 1.0) * (0.85 if (f == 6 and 8 <= j <= 10) else 1.0)
                r = px(e), None
            elif skin_here:
                r = skin_p(face, i, j, fw, fh)
            else:
                r = hair_p(face, i, j, fw, fh)
        elif face == "back":
            r = hair_p(face, i, j, fw, fh) if j < back_rows else skin_p(face, i, j, fw, fh)
        elif face == "top":
            r = hair_p(face, i, j, fw, fh)
        else:
            r = skin_p(face, i, j, fw, fh)
        if extra is not None:
            o = extra(face, i, j, fw, fh)
            if o is not None:
                return o
        return r
    return p


# ------------------------------------------------------------------------------------------------------------- the humanoid rig
HIP, SHO, ELB, NECK = 10.0, 19.5, 14.0, 23.0                      # design coordinates of the upper body
UP = 1.5                                                           # the upper body is lifted by this (Build.raise_upper): the hips end at y = 11.5
LEG_HIP, LEG_KNEE = HIP + UP, 5.5                                  # the legs are built in real coordinates


def rig_bones(h, P, extra_head=()):
    tw, td, aw, lx = P["tw"], P["td"], P["aw"], P["lx"]
    sx = tw / 2 + aw / 2 + 0.1
    P["sx"] = sx
    tz = td / 2 + 0.2
    P["tz"] = tz
    h.bone("root", None, (0, 0, 0))
    h.bone("body", "root", (0, HIP, 0))
    h.bone("head", "body", (0, NECK, 0))
    h.bone("hair", "head", (0, 31, 0))
    h.bone("crown", "head", (0, 31.4, 0))
    h.bone("fur", "body", (0, 20, 0))
    h.bone("cloak", "body", (0, SHO, tz))
    h.bone("cloak_top", "cloak", (0, SHO, tz))
    h.bone("cloak_bottom", "cloak_top", (0, 9.5, tz))
    for suf, sg in (("_l", 1), ("_r", -1)):
        h.bone("arm" + suf, "body", (sg * sx, SHO, 0))
        h.bone("forearm" + suf, "arm" + suf, (sg * sx, ELB, 0))
        h.bone("leg" + suf, "root", (sg * lx, LEG_HIP, 0))
        h.bone("shin" + suf, "leg" + suf, (sg * lx, LEG_KNEE, 0))


def humanoid_common(h, P, S_):
    """Legs, arms, torso, collar, fur mantle, crown, cloak: everything the three siblings share, with the proportions of P and the painters of S_:
    S_ keys torso, belt, skirt, thigh, boot, foot, sleeve_u, sleeve_f, hand, head, collar."""
    tw, td, aw, ad, lw, ld, lx, sx = P["tw"], P["td"], P["aw"], P["ad"], P["lw"], P["ld"], P["lx"], P["sx"]
    # legs: thigh (trousers), shin (boot), foot
    h.pair("leg_l", "leg_r", (lx - lw / 2, LEG_KNEE, -ld / 2), (lw, LEG_HIP - LEG_KNEE, ld), S_["thigh"])
    h.pair("shin_l", "shin_r", (lx - lw / 2, 0.0, -ld / 2), (lw, LEG_KNEE, ld), S_["boot"], inflate=0.15)
    h.pair("shin_l", "shin_r", (lx - lw / 2 - 0.25, 0.0, -ld / 2 - 2.5), (lw + 0.5, 2, ld + 2.0), S_["foot"], inflate=0.05)
    # torso, belt, skirt
    h.box("body", (-tw / 2, HIP, -td / 2), (tw, 10, td), S_["torso"])
    h.box("body", (-tw / 2, HIP, -td / 2), (tw, 2, td), S_["belt"], inflate=0.35)
    if S_.get("skirt"):
        sk = P.get("skirt", (tw + 1.5, 3, td + 1.5, 7.0))
        h.box("body", (-sk[0] / 2, sk[3], -sk[2] / 2), (r5(sk[0]), sk[1], r5(sk[2])), S_["skirt"])
    # arms: upper sleeve (+ forearm bone with hand)
    h.pair("arm_l", "arm_r", (sx - aw / 2, ELB, -ad / 2), (aw, 6, ad), S_["sleeve_u"], inflate=0.1)
    h.pair("forearm_l", "forearm_r", (sx - aw / 2 + 0.25, 10, -ad / 2 + 0.25), (aw - 0.5, 4, ad - 0.5), S_["sleeve_f"], inflate=0.1)
    h.pair("forearm_l", "forearm_r", (sx - (aw - 1) / 2, 7.5, -(ad - 1) / 2), (aw - 1, 2.5, ad - 1), S_["hand"])
    # head
    h.box("head", (-4, NECK, -4), (8, 8, 8), S_["head"])


def tuft(h, x, y, z, w, ln, dx, dz, tilt, paint, down=0.0):
    """One tuft of fur: a small box standing on (x, y, z), tilted by tilt degrees away from the vertical towards the outward direction (dx, dz)
    (and a little further when down > 0), so a mass of them reads as a shaggy ruff instead of plates."""
    ang = tilt + down
    h.box("fur", (x - w / 2, y, z - w / 2), (w, r5(ln), w), paint, rotation=(ang * dz, 0, -ang * dx), pivot=(x, y, z))


def collar_and_fur(h, P, fur_p, collar_p, spike_p, rng_salt=100, tall=1.0, front=True):
    """The tall black high collar and the huge white-grey fur mantle: a dark fur core (a drooping slab over each shoulder, a stand-up back slab and
    two side pieces round the neck) covered by tilted tufts (a field over the shoulders, a drop of tufts along the outer rim, two rings round the neck)
    so it reads as a shaggy ruff; the chest stays open in a V."""
    tw, td, hw = P["tw"], P["td"], P["hw"]
    hb = td / 2
    ck = r5(tw * 0.34 + 0.6)
    h.box("fur", (-ck, 19, -(hb + 0.5)), (2 * ck, 5.0, r5(td + 1.0)), collar_p, inflate=0.25)                                       # the black high collar
    core = lambda s_: fur_p(s_)
    for sg, nm in ((1, 0), (-1, 1)):                                                                                                 # shoulder slabs
        ox = 2.0 if sg > 0 else -hw + 0.5
        h.box("fur", (ox, 19.4, -(hb + 1.5)), (r5(hw - 2.5), 3.0, r5(td + 4.0)), core(rng_salt + 2 + nm), rotation=(0, 0, sg * 9), pivot=(sg * 3.0, 22.0, 0))
    bw = r5(hw - 3.0)
    h.box("fur", (-bw, 20.5, hb - 0.2), (2 * bw, 6.0 * tall, 2.5), core(rng_salt + 1), rotation=(10, 0, 0), pivot=(0, 21.0, hb))      # back slab
    for sg, nm in ((1, 0), (-1, 1)):                                                                                                 # neck side pieces
        ox = (ck + 0.2) if sg > 0 else -(ck + 0.2 + 2.5)
        h.box("fur", (ox, 20.8, -(hb + 0.3)), (2.5, 5.0 * tall, r5(td + 2.5)), core(rng_salt + 8 + nm), rotation=(0, 0, -sg * 12), pivot=(sg * ck, 21.0, 0))
    n = 0
    sp = lambda: spike_p(rng_salt + 30 + (n % 23))
    # a field of tufts over the shoulders and the upper back, tilting away from the neck
    for sg in (1, -1):
        for ix, x0 in enumerate((3.2, 5.4, 7.6, hw - 1.3)):
            for iz, z0 in enumerate((-(hb + 1.6), -(hb - 0.6), hb - 2.8 + hb, hb + 1.6, hb + 3.0)):
                if iz == 0 and (not front or ix < 2):
                    continue
                hv = hsh(ix, iz + (3 if sg > 0 else 0), rng_salt)
                dx, dz = sg * (0.55 + 0.1 * ix), (z0 / (hb + 3.0)) * 0.9
                nrm = math.hypot(dx, dz) or 1.0
                outer = ix == 3
                tuft(h, sg * x0 + (hv - 0.5), 21.4 - (0.6 if outer else 0.0), z0, 2.0 if hv > 0.45 else 1.5, 2.5 + 2.0 * hv + (0.5 if ix == 0 else 0.0),
                     dx / nrm, dz / nrm, 18 + 14 * hv + 12 * ix, sp(), down=(30 if outer else 0))
                n += 1
    # the rim: a drop of tufts hanging out over the arms
    for sg in (1, -1):
        for k, z0 in enumerate((-(hb + 1.5), -(hb - 0.5), hb - 1.0, hb + 1.0, hb + 3.0)):
            hv = hsh(k, sg + 5, rng_salt)
            tuft(h, sg * (hw + 0.2), 20.2, z0, 2.0, 2.5 + hv * 1.5, sg, 0.0, 78 + 10 * hv, sp())
            n += 1
    # two rings of tufts standing round the neck (the front stays open)
    for ring, (rad_x, rad_z, y0, base_t) in enumerate(((ck + 2.4, hb + 2.2, 24.2, 16), (ck + 4.2, hb + 3.2, 22.6, 34))):
        for k in range(11 if ring == 0 else 13):
            a = math.radians(64 + k * (232.0 / (10 if ring == 0 else 12)))
            dx, dz = SIN(a), -COS(a)
            hv = hsh(k, ring + 40, rng_salt)
            tuft(h, dx * rad_x, y0, dz * rad_z, 2.0, (3.0 + 1.5 * hv) * tall, dx, dz, base_t + 10 * hv, sp())
            n += 1
    if front:
        for sg in (1, -1):                                                                                                           # the edge of the V on the chest
            for k, y0 in enumerate((20.5, 18.6)):
                tuft(h, sg * (2.6 + 0.6 * k), y0, -(hb + 1.4), 1.5, 2.5, sg * 0.6, -0.8, 30 + 8 * k, sp(), down=40)
                n += 1
    return n


def crown(h, P, gem=(220, 40, 56), gem_glow=(255, 96, 96), tilt=(0, 0, 0), tall=1.0, shift=(0.0, 0.0)):
    """A gold crown on the hair: a band of four boxes (the hair shows inside it), pointed prongs, a gem on the front. shift moves it (x, z)."""
    gp = metal(jewel=gem_front(gem, gem_glow))
    plain = metal()
    y0 = 31.5
    sx_, sz_ = shift
    rt = tilt if any(tilt) else None
    pv = (sx_, y0, sz_)
    h.box("crown", (-5.0 + sx_, y0, -5.0 + sz_), (10.0, 1.5, 0.5), gp, rotation=rt, pivot=pv)
    h.box("crown", (-5.0 + sx_, y0, 4.5 + sz_), (10.0, 1.5, 0.5), plain, rotation=rt, pivot=pv)
    h.box("crown", (4.5 + sx_, y0, -4.5 + sz_), (0.5, 1.5, 9.0), plain, rotation=rt, pivot=pv)
    h.box("crown", (-5.0 + sx_, y0, -4.5 + sz_), (0.5, 1.5, 9.0), plain, rotation=rt, pivot=pv)
    prongs = [(-4.0, -5.0, 1.5), (-2.0, -5.0, 2.5), (0.0, -5.0, 3.0), (2.0, -5.0, 2.5), (4.0, -5.0, 1.5),
              (-4.0, 4.5, 1.5), (-1.5, 4.5, 2.5), (1.5, 4.5, 2.5), (4.0, 4.5, 1.5)]
    for (x, z, hh) in prongs:
        hh = hh * tall
        h.box("crown", (x - 0.75 + sx_, y0 + 1.5, z + sz_), (1.5, r5(hh * 0.5), 0.5), plain, rotation=rt, pivot=pv)
        h.box("crown", (x - 0.25 + sx_, y0 + 1.5 + r5(hh * 0.5), z + sz_), (0.5, r5(hh * 0.5) + 0.5, 0.5), plain, rotation=rt, pivot=pv)
    for z in (-2.0, 2.0):
        h.box("crown", (4.5 + sx_, y0 + 1.5, z - 0.5 + sz_), (0.5, 2.0 * tall, 1.0), plain, rotation=rt, pivot=pv)
        h.box("crown", (-5.0 + sx_, y0 + 1.5, z - 0.5 + sz_), (0.5, 2.0 * tall, 1.0), plain, rotation=rt, pivot=pv)
    h.box("crown", (-0.5 + sx_, y0 + 0.25, -5.5 + sz_), (1.0, 1.0, 0.5), solid(gem, (gem_glow[0], gem_glow[1], gem_glow[2], 150), 4), rotation=rt, pivot=pv)


def cloak_parts(h, P, cloak_p, hem_p, side_p=None):
    """The long black cloak: a back panel in two segments (cloak_top / cloak_bottom, so it sways), side drapes behind the arms that flare out."""
    tw, td, tz = P["tw"], P["td"], P["tz"]
    w = r5(tw + 6.0)
    h.box("cloak_top", (-w / 2, 9.5, tz - 0.1), (w, 10.5, 1.5), cloak_p)
    h.box("cloak_bottom", (-(w + 5.0) / 2, 0.5 - UP, tz + 0.1), (w + 5.0, 9.0 + UP, 1.5), hem_p)
    sx, aw = P["sx"], P["aw"]
    sp = side_p or cloak_p
    for sg, nm in ((1, "l"), (-1, "r")):
        ox = (sx + aw / 2 + 0.3) if sg > 0 else -(sx + aw / 2 + 0.3 + 1.6)
        h.box("cloak_top", (ox, 9.5, 0.5), (1.5, 10.5, r5(td / 2 + 1.5)), sp, rotation=(0, 0, sg * 9), pivot=(sg * (sx + aw / 2 + 0.3), 20, 0))


# =========================================================================================================================================
#                                                              humanoid animation
# =========================================================================================================================================
def cyc(a, clip, bone, kind, L, fn, n=1, steps=16):
    """A looping track: fn(theta) -> (x, y, z) sampled steps times over the clip length L, with n repeats of theta in 0..TAU."""
    frames = {}
    for k in range(steps + 1):
        frames[L * k / steps] = tuple(round(v, 3) for v in fn(TAU * n * k / steps))
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def swing(base, amp, ph=0.0):
    """base + amp * (sin(theta + ph) - sin(ph)) per axis: equal to base at theta = 0, so a loop starts on the neutral pose."""
    return lambda th: tuple(base[k] + amp[k] * (SIN(th + ph) - SIN(ph)) for k in range(3))


def keys(a, clip, bone, kind, pts):
    """Keyframes from (time, (x, y, z)) pairs."""
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, {t: tuple(v) for t, v in pts})


NEUTRAL = {"arm_l": (0, 0, 3.5), "arm_r": (0, 0, -3.5), "forearm_l": (9, 0, 0), "forearm_r": (9, 0, 0), "cloak_top": (-2, 0, 0), "cloak_bottom": (-2, 0, 0)}


def N(bone):
    return NEUTRAL.get(bone, (0, 0, 0))


def add(v, d):
    return tuple(v[k] + d[k] for k in range(3))


def pose(a, clip, bone, frames):
    """frames: {time: delta} where delta is relative to the bone's neutral rotation."""
    keys(a, clip, bone, "rot", [(t, add(N(bone), d)) for t, d in sorted(frames.items())])


LIVELY = dict(
    dante=dict(L_walk=1.1, L_run=0.66, arm=0.8, bob=1.0, lean=1.0, idle_head=1.0, breath=1.3, stomp=1.0, cast_arms=0.95),
    vanica=dict(L_walk=0.9, L_run=0.52, arm=1.1, bob=0.8, lean=1.1, idle_head=1.6, breath=0.9, stomp=0.3, cast_arms=1.1),
    zenon=dict(L_walk=1.0, L_run=0.6, arm=0.5, bob=0.5, lean=0.8, idle_head=0.5, breath=0.6, stomp=0.4, cast_arms=1.0),
    disciple=dict(L_walk=1.0, L_run=0.6, arm=0.7, bob=0.7, lean=1.0, idle_head=1.0, breath=1.0, stomp=0.4, cast_arms=1.0),
)


def humanoid_clips(a, who):
    """Every humanoid clip on the shared rig. Neutral pose = arms a little out, elbows a little bent, the cloak hanging a touch behind; every one-shot clip
    starts and ends on it (so holding the last frame looks like idle), except death (a collapsed pose) and nothing else."""
    k = LIVELY[who]

    # ------------------------------------------------------------------------------------------------ idle (loop 3.2 s)
    L = 3.2
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.18 * k["breath"] * SIN(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.8 * k["breath"], 1.2, 0.5), 0.6))
    cyc(a, "idle", "body", "scale", L, lambda t: (1, 1 + 0.008 * k["breath"] * SIN(t), 1 + 0.014 * k["breath"] * SIN(t)))
    cyc(a, "idle", "head", "rot", L, swing((0, 0, 0), (1.6 * k["idle_head"], 4.0 * k["idle_head"], 1.2 * k["idle_head"]), 0.7))
    cyc(a, "idle", "hair", "rot", L, swing((0, 0, 0), (2.0, 0, 1.6), 1.2))
    cyc(a, "idle", "fur", "rot", L, swing((0, 0, 0), (0.8, 0, 0.8), 0.4))
    cyc(a, "idle", "fur", "scale", L, lambda t: (1, 1 + 0.01 * SIN(t + 0.3) - 0.01 * SIN(0.3), 1))
    cyc(a, "idle", "cloak_top", "rot", L, swing(N("cloak_top"), (1.6, 0, 0.9), 0.6))
    cyc(a, "idle", "cloak_bottom", "rot", L, swing(N("cloak_bottom"), (2.6, 0, 1.4), 1.2))
    for suf, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "idle", "arm" + suf, "rot", L, swing(N("arm" + suf), (2.0 * k["arm"], 0, sg * 0.8), 0.4 + (0.5 if suf == "_r" else 0)))
        cyc(a, "idle", "forearm" + suf, "rot", L, swing(N("forearm" + suf), (2.4 * k["arm"], 0, 0), 1.0))
    if who == "vanica":                                                      # she fidgets: a hand drifts up to her cheek and down again
        keys(a, "idle", "arm_r", "rot", [(0.0, (0, 0, -3.5)), (0.8, (0, 0, -3.5)), (1.4, (46, 0, -10)), (2.2, (46, 0, -10)), (2.8, (0, 0, -3.5)), (3.2, (0, 0, -3.5))])
        keys(a, "idle", "forearm_r", "rot", [(0.0, (9, 0, 0)), (0.8, (9, 0, 0)), (1.4, (92, 0, 0)), (2.2, (92, 0, 0)), (2.8, (9, 0, 0)), (3.2, (9, 0, 0))])
    for suf, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "idle", "leg" + suf, "rot", L, lambda t, sg=sg: (0, 0, 0.8 * sg * SIN(t)))

    # ------------------------------------------------------------------------------------------------ walk (loop)
    L = k["L_walk"]
    a.clip("walk", L, True)
    for suf, ph in (("_l", 0.0), ("_r", math.pi)):
        sg = 1 if suf == "_l" else -1
        cyc(a, "walk", "leg" + suf, "rot", L, lambda t, ph=ph: (28 * SIN(t + ph), 0, 0))
        cyc(a, "walk", "shin" + suf, "rot", L, lambda t, ph=ph: (-34 * max(0.0, COS(t + ph)) ** 1.2 - 4, 0, 0))
        cyc(a, "walk", "arm" + suf, "rot", L, lambda t, ph=ph, sg=sg: (-24 * k["arm"] * SIN(t + ph), 0, sg * 3.5))
        cyc(a, "walk", "forearm" + suf, "rot", L, lambda t, ph=ph: (9 + 16 * k["arm"] * max(0.0, -SIN(t + ph)), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.5 * k["bob"] * COS(2 * t), 0), 1)
    cyc(a, "walk", "body", "rot", L, lambda t: (-1.5 * k["lean"], 3.0 * SIN(t), 1.0 * SIN(t)), 1)
    cyc(a, "walk", "head", "rot", L, lambda t: (1.0 * SIN(2 * t + 1.6), -2.5 * SIN(t), -0.8 * SIN(t)), 1)
    cyc(a, "walk", "hair", "rot", L, lambda t: (3.0 * SIN(2 * t + 0.4), 0, 1.2 * SIN(t)), 1)
    cyc(a, "walk", "fur", "rot", L, lambda t: (1.0 * SIN(2 * t + 0.8), 0, 0.8 * SIN(t)), 1)
    cyc(a, "walk", "cloak_top", "rot", L, lambda t: (-6 + 3.0 * SIN(2 * t + 0.5), 0, 1.5 * SIN(t)), 1)
    cyc(a, "walk", "cloak_bottom", "rot", L, lambda t: (-9 + 5.0 * SIN(2 * t + 1.1), 0, 2.5 * SIN(t + 0.4)), 1)

    # ------------------------------------------------------------------------------------------------ run (loop)
    L = k["L_run"]
    a.clip("run", L, True)
    for suf, ph in (("_l", 0.0), ("_r", math.pi)):
        sg = 1 if suf == "_l" else -1
        cyc(a, "run", "leg" + suf, "rot", L, lambda t, ph=ph: (52 * SIN(t + ph), 0, 0))
        cyc(a, "run", "shin" + suf, "rot", L, lambda t, ph=ph: (-78 * max(0.0, COS(t + ph + 0.3)) ** 1.1 - 8, 0, 0))
        cyc(a, "run", "arm" + suf, "rot", L, lambda t, ph=ph, sg=sg: (-46 * k["arm"] * SIN(t + ph), 0, sg * 5))
        cyc(a, "run", "forearm" + suf, "rot", L, lambda t, ph=ph: (70 + 28 * k["arm"] * max(0.0, -SIN(t + ph)), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 1.1 * k["bob"] * COS(2 * t) + 0.2, 0), 1)
    cyc(a, "run", "body", "rot", L, lambda t: (-11 * k["lean"] + 1.5 * SIN(2 * t), 5.0 * SIN(t), 1.5 * SIN(t)), 1)
    cyc(a, "run", "head", "rot", L, lambda t: (6 + 1.5 * SIN(2 * t + 1.6), -4.0 * SIN(t), 0), 1)
    cyc(a, "run", "hair", "rot", L, lambda t: (-14 + 4.0 * SIN(2 * t + 0.4), 0, 2.0 * SIN(t)), 1)
    cyc(a, "run", "fur", "rot", L, lambda t: (-3 + 2.0 * SIN(2 * t + 0.8), 0, 1.2 * SIN(t)), 1)
    cyc(a, "run", "cloak_top", "rot", L, lambda t: (-26 + 5.0 * SIN(2 * t + 0.5), 0, 2.5 * SIN(t)), 1)
    cyc(a, "run", "cloak_bottom", "rot", L, lambda t: (-34 + 8.0 * SIN(2 * t + 1.1), 0, 4 * SIN(t + 0.4)), 1)

    # ------------------------------------------------------------------------------------------------ attack (one-shot 0.9 s): wind-up, slash, follow-through
    L = 0.9
    a.clip("attack", L, False)
    T = (0.0, 0.26, 0.4, 0.52, 0.9)
    pose(a, "attack", "arm_r", dict(zip(T, ((0, 0, 0), (152, 0, -12), (22, 0, 6), (6, 0, 4), (0, 0, 0)))))
    pose(a, "attack", "forearm_r", dict(zip(T, ((0, 0, 0), (50, 0, 0), (6, 0, 0), (10, 0, 0), (0, 0, 0)))))
    pose(a, "attack", "arm_l", dict(zip(T, ((0, 0, 0), (-34, 0, 8), (-14, 0, 8), (-6, 0, 4), (0, 0, 0)))))
    pose(a, "attack", "forearm_l", dict(zip(T, ((0, 0, 0), (26, 0, 0), (14, 0, 0), (6, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "body", "rot", list(zip(T, ((0, 0, 0), (7, -13, 0), (-15, 14, 0), (-9, 8, 0), (0, 0, 0)))))
    keys(a, "attack", "body", "pos", list(zip(T, ((0, 0, 0), (0, 0.2, 1.2), (0, -0.8 * k["stomp"] - 0.4, -2.6), (0, -0.3, -1.2), (0, 0, 0)))))
    keys(a, "attack", "head", "rot", list(zip(T, ((0, 0, 0), (3, -9, 0), (-7, 8, 0), (-3, 4, 0), (0, 0, 0)))))
    keys(a, "attack", "leg_l", "rot", list(zip(T, ((0, 0, 0), (-10, 0, 0), (32, 0, 0), (18, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "shin_l", "rot", list(zip(T, ((0, 0, 0), (-12, 0, 0), (-10, 0, 0), (-6, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "leg_r", "rot", list(zip(T, ((0, 0, 0), (12, 0, 0), (-22, 0, 0), (-10, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "shin_r", "rot", list(zip(T, ((0, 0, 0), (-26, 0, 0), (-30, 0, 0), (-12, 0, 0), (0, 0, 0)))))
    pose(a, "attack", "cloak_top", dict(zip(T, ((0, 0, 0), (-14, 0, 0), (10, 0, 0), (-4, 0, 0), (0, 0, 0)))))
    pose(a, "attack", "cloak_bottom", dict(zip(T, ((0, 0, 0), (-22, 0, 0), (14, 0, 0), (-6, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "fur", "rot", list(zip(T, ((0, 0, 0), (-4, 0, 0), (5, 0, 0), (0, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "hair", "rot", list(zip(T, ((0, 0, 0), (-6, 0, 0), (10, 0, 0), (2, 0, 0), (0, 0, 0)))))

    # ------------------------------------------------------------------------------------------------ cast (loop 1.6 s): both arms raised, hands open, a slow hover
    L = 1.6
    a.clip("cast", L, True)
    ca = k["cast_arms"]
    for suf, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "cast", "arm" + suf, "rot", L, lambda t, sg=sg: (102 * ca + 5 * SIN(t), -sg * (16 + 3 * SIN(t + 1.0)), sg * 3.5))
        cyc(a, "cast", "forearm" + suf, "rot", L, lambda t: (12 + 8 * SIN(t + 0.6), 0, 0))
        cyc(a, "cast", "leg" + suf, "rot", L, lambda t, sg=sg: (-3 + 2 * SIN(t), 0, sg * 5))
        cyc(a, "cast", "shin" + suf, "rot", L, lambda t: (-8 + 2 * SIN(t + 1), 0, 0))
    cyc(a, "cast", "body", "pos", L, lambda t: (0, 0.6 + 0.5 * SIN(t), 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (3 + 1.0 * SIN(t + 0.5), 0, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (5 + 1.5 * SIN(t + 0.4), 2.0 * SIN(t), 0))
    cyc(a, "cast", "hair", "rot", L, lambda t: (-9 + 3.0 * SIN(t + 0.6), 0, 2.0 * SIN(t)))
    cyc(a, "cast", "fur", "rot", L, lambda t: (-2 + 1.2 * SIN(t + 0.8), 0, 1.0 * SIN(t)))
    cyc(a, "cast", "fur", "scale", L, lambda t: (1, 1.03 + 0.02 * SIN(t), 1.02))
    cyc(a, "cast", "cloak_top", "rot", L, lambda t: (-13 + 4.0 * SIN(t + 1.0), 0, 2.0 * SIN(t)))
    cyc(a, "cast", "cloak_bottom", "rot", L, lambda t: (-18 + 6.0 * SIN(t + 1.6), 0, 3.0 * SIN(t + 0.5)))

    # ------------------------------------------------------------------------------------------------ cast_big (one-shot 2.4 s): gather, rise, arms overhead, slam
    L = 2.4
    a.clip("cast_big", L, False)
    T = (0.0, 0.55, 0.9, 1.3, 1.65, 1.95, 2.4)
    for suf, sg in (("_l", 1), ("_r", -1)):
        pose(a, "cast_big", "arm" + suf, dict(zip(T, ((0, 0, 0), (62, 0, sg * 4), (140, -sg * 8, -sg * 14), (176, 0, -sg * 36), (174, 0, -sg * 38), (24, 0, sg * 6), (0, 0, 0)))))
        pose(a, "cast_big", "forearm" + suf, dict(zip(T, ((0, 0, 0), (84, 0, 0), (38, 0, 0), (14, 0, 0), (10, 0, 0), (6, 0, 0), (0, 0, 0)))))
        keys(a, "cast_big", "leg" + suf, "rot", list(zip(T, ((0, 0, 0), (45, 0, 0), (-6, 0, sg * 5), (-12, 0, sg * 7), (-12, 0, sg * 7), (16, 0, 0), (0, 0, 0)))))
        keys(a, "cast_big", "shin" + suf, "rot", list(zip(T, ((0, 0, 0), (-75, 0, 0), (-14, 0, 0), (-20, 0, 0), (-20, 0, 0), (-24, 0, 0), (0, 0, 0)))))
    keys(a, "cast_big", "body", "pos", list(zip(T, ((0, 0, 0), (0, -2.8, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, -1.6, -1.0), (0, 0, 0)))))
    keys(a, "cast_big", "root", "pos", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 3.0, 0), (0, 5.0, 0), (0, 5.0, 0), (0, 0.6, 0), (0, 0, 0)))))
    keys(a, "cast_big", "body", "rot", list(zip(T, ((0, 0, 0), (-9, 0, 0), (6, 0, 0), (12, 0, 0), (12, 0, 0), (-14, 0, 0), (0, 0, 0)))))
    keys(a, "cast_big", "head", "rot", list(zip(T, ((0, 0, 0), (-12, 0, 0), (8, 0, 0), (18, 0, 0), (18, 0, 0), (-12, 0, 0), (0, 0, 0)))))
    pose(a, "cast_big", "cloak_top", dict(zip(T, ((0, 0, 0), (6, 0, 0), (-22, 0, 0), (-30, 0, 0), (-32, 0, 0), (8, 0, 0), (0, 0, 0)))))
    pose(a, "cast_big", "cloak_bottom", dict(zip(T, ((0, 0, 0), (10, 0, 0), (-30, 0, 4), (-42, 0, -4), (-44, 0, 4), (12, 0, 0), (0, 0, 0)))))
    keys(a, "cast_big", "hair", "rot", list(zip(T, ((0, 0, 0), (6, 0, 0), (-18, 0, 0), (-26, 0, 0), (-28, 0, 0), (12, 0, 0), (0, 0, 0)))))
    keys(a, "cast_big", "fur", "rot", list(zip(T, ((0, 0, 0), (4, 0, 0), (-6, 0, 0), (-9, 0, 0), (-9, 0, 0), (5, 0, 0), (0, 0, 0)))))
    keys(a, "cast_big", "fur", "scale", list(zip(T, ((1, 1, 1), (1, 0.97, 1), (1, 1.06, 1.03), (1, 1.1, 1.05), (1, 1.1, 1.05), (1, 1, 1), (1, 1, 1)))))

    # ------------------------------------------------------------------------------------------------ hurt (one-shot 0.45 s)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    keys(a, "hurt", "body", "rot", list(zip(T, ((0, 0, 0), (10, 0, 3), (0, 0, 0)))))
    keys(a, "hurt", "body", "pos", list(zip(T, ((0, 0, 0), (0, 0, 1.6), (0, 0, 0)))))
    keys(a, "hurt", "head", "rot", list(zip(T, ((0, 0, 0), (14, 6, 0), (0, 0, 0)))))
    for suf, sg in (("_l", 1), ("_r", -1)):
        pose(a, "hurt", "arm" + suf, dict(zip(T, ((0, 0, 0), (-22, 0, sg * 12), (0, 0, 0)))))
        pose(a, "hurt", "forearm" + suf, dict(zip(T, ((0, 0, 0), (18, 0, 0), (0, 0, 0)))))
        keys(a, "hurt", "leg" + suf, "rot", list(zip(T, ((0, 0, 0), (-8 * sg, 0, 0), (0, 0, 0)))))
    pose(a, "hurt", "cloak_top", dict(zip(T, ((0, 0, 0), (-12, 0, 0), (0, 0, 0)))))
    pose(a, "hurt", "cloak_bottom", dict(zip(T, ((0, 0, 0), (-20, 0, 0), (0, 0, 0)))))
    keys(a, "hurt", "hair", "rot", list(zip(T, ((0, 0, 0), (-14, 0, 0), (0, 0, 0)))))
    keys(a, "hurt", "fur", "rot", list(zip(T, ((0, 0, 0), (-6, 0, 0), (0, 0, 0)))))

    # ------------------------------------------------------------------------------------------------ transform (one-shot 3.0 s): crouch and shake, rise with arms spread wide, hold, settle
    L = 3.0
    a.clip("transform", L, False)
    T = (0.0, 0.5, 0.95, 1.3, 1.9, 2.4, 3.0)
    for suf, sg in (("_l", 1), ("_r", -1)):
        pose(a, "transform", "arm" + suf, dict(zip(T, ((0, 0, 0), (40, 0, sg * -6), (30, 0, sg * 74), (24, 0, sg * 84), (22, 0, sg * 82), (10, 0, sg * 30), (0, 0, 0)))))
        pose(a, "transform", "forearm" + suf, dict(zip(T, ((0, 0, 0), (88, 0, 0), (20, 0, 0), (12, 0, 0), (14, 0, 0), (10, 0, 0), (0, 0, 0)))))
        keys(a, "transform", "leg" + suf, "rot", list(zip(T, ((0, 0, 0), (45, 0, 0), (-4, 0, sg * 8), (-8, 0, sg * 10), (-8, 0, sg * 10), (-2, 0, 0), (0, 0, 0)))))
        keys(a, "transform", "shin" + suf, "rot", list(zip(T, ((0, 0, 0), (-75, 0, 0), (-8, 0, 0), (-12, 0, 0), (-12, 0, 0), (-4, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "body", "pos", list(zip(T, ((0, 0, 0), (0, -2.8, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "root", "pos", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 2.5, 0), (0, 4.0, 0), (0, 4.0, 0), (0, 1.0, 0), (0, 0, 0)))))
    keys(a, "transform", "body", "rot", list(zip(T, ((0, 0, 0), (-12, 0, 0), (8, 0, 0), (12, 0, 0), (12, 0, 0), (4, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "body", "scale", list(zip(T, ((1, 1, 1), (1, 0.97, 1.02), (1, 1.03, 1.03), (1, 1.04, 1.04), (1, 1.04, 1.04), (1, 1.01, 1.01), (1, 1, 1)))))
    keys(a, "transform", "head", "rot", list(zip(T, ((0, 0, 0), (-16, 0, 0), (14, 0, 0), (24, 0, 0), (24, 0, 0), (8, 0, 0), (0, 0, 0)))))
    pose(a, "transform", "cloak_top", dict(zip(T, ((0, 0, 0), (6, 0, 0), (-24, 0, 5), (-34, 0, -4), (-34, 0, 5), (-8, 0, 0), (0, 0, 0)))))
    pose(a, "transform", "cloak_bottom", dict(zip(T, ((0, 0, 0), (10, 0, 0), (-34, 0, -6), (-46, 0, 7), (-46, 0, -6), (-12, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "hair", "rot", list(zip(T, ((0, 0, 0), (10, 0, 0), (-24, 0, 4), (-30, 0, -4), (-30, 0, 4), (-8, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "fur", "rot", list(zip(T, ((0, 0, 0), (6, 0, 0), (-8, 0, 0), (-12, 0, 0), (-12, 0, 0), (-2, 0, 0), (0, 0, 0)))))
    keys(a, "transform", "fur", "scale", list(zip(T, ((1, 1, 1), (1, 0.96, 1), (1.04, 1.12, 1.06), (1.08, 1.2, 1.1), (1.08, 1.2, 1.1), (1.02, 1.04, 1.02), (1, 1, 1)))))

    # ------------------------------------------------------------------------------------------------ death (one-shot 2.8 s, holds): stagger, kneel, topple forward
    L = 2.8
    a.clip("death", L, False)
    T = (0.0, 0.35, 0.95, 1.5, 2.2, 2.8)
    for suf, sg in (("_l", 1), ("_r", -1)):
        pose(a, "death", "arm" + suf, dict(zip(T, ((0, 0, 0), (-30, 0, sg * 16), (30, 0, sg * 12), (38, 0, sg * 10), (-8, 0, sg * 30), (-8, 0, sg * 30)))))
        pose(a, "death", "forearm" + suf, dict(zip(T, ((0, 0, 0), (24, 0, 0), (20, 0, 0), (10, 0, 0), (6, 0, 0), (6, 0, 0)))))
        keys(a, "death", "leg" + suf, "rot", list(zip(T, ((0, 0, 0), (-10 * sg, 0, 0), (62, 0, sg * 5), (80, 0, sg * 6), (6, 0, 0), (6, 0, 0)))))
        keys(a, "death", "shin" + suf, "rot", list(zip(T, ((0, 0, 0), (-20, 0, 0), (-118, 0, 0), (-124, 0, 0), (-10, 0, 0), (-10, 0, 0)))))
    keys(a, "death", "body", "rot", list(zip(T, ((0, 0, 0), (14, 0, 4), (-8, 0, 0), (-22, 0, 0), (-4, 0, 0), (-4, 0, 0)))))
    keys(a, "death", "body", "pos", list(zip(T, ((0, 0, 0), (0, 0, 2.0), (0, -6.2, 0), (0, -7.4, 0), (0, -9.5, 0), (0, -9.5, 0)))))
    keys(a, "death", "head", "rot", list(zip(T, ((0, 0, 0), (20, 0, 0), (-8, 0, 0), (-34, 0, 4), (-6, 0, 10), (-6, 0, 10)))))
    pose(a, "death", "cloak_top", dict(zip(T, ((0, 0, 0), (-14, 0, 0), (-4, 0, 0), (-10, 0, 0), (18, 0, 0), (18, 0, 0)))))
    pose(a, "death", "cloak_bottom", dict(zip(T, ((0, 0, 0), (-26, 0, 0), (6, 0, 0), (-4, 0, 0), (26, 0, 0), (26, 0, 0)))))
    keys(a, "death", "fur", "rot", list(zip(T, ((0, 0, 0), (-8, 0, 0), (2, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)))))
    keys(a, "death", "hair", "rot", list(zip(T, ((0, 0, 0), (-16, 0, 0), (6, 0, 0), (22, 0, 0), (8, 0, 0), (8, 0, 0)))))
    keys(a, "death", "crown", "rot", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-12, 0, 14), (-12, 0, 14)))))
    keys(a, "death", "root", "rot", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-90, 0, 0), (-90, 0, 0)))))
    keys(a, "death", "root", "pos", list(zip(T, ((0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 4.0, 0), (0, 4.0, 0)))))


# =========================================================================================================================================
#                                                              hybrid helpers (devil marks, horns, orbiting rings)
# =========================================================================================================================================
def overlay(paint, fn):
    """A painter that runs paint(...) and then fn(face, i, j, fw, fh, (colour, glow)) -> (colour, glow) | None (None keeps the texel)."""
    def p(face, i, j, fw, fh):
        r = paint(face, i, j, fw, fh)
        o = fn(face, i, j, fw, fh, r)
        return r if o is None else o
    return p


GLYPHS = (
    ("..###..", ".#...#.", "#..#..#", "#.###.#", "#..#..#", ".#...#.", "..###.."),
    ("#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"),
    (".##.##.", "#.....#", ".......", "###.###", ".......", "#.....#", ".##.##."),
    ("...#...", "...#...", "#######", "...#...", "...#...", "..#.#..", ".#...#."),
    ("#.#.#.#", ".......", "#######", ".......", "#.#.#.#", ".......", "#######"),
)


def sigils(seed, col=(236, 34, 58), glow=(255, 70, 90), a=230, cell=7, density=0.5, faces=("front", "back", "left", "right"), body=(0, 0, 0), base_mix=0.0):
    """Glowing curse sigils: small glyphs from a handful of 7 x 7 patterns scattered over the cells of a face, painted into the colour and the glow."""
    def f(face, i, j, fw, fh, r):
        if face not in faces:
            return None
        cx, cy = i // cell, j // cell
        if hsh(cx, cy, seed) > density:
            return None
        g = GLYPHS[int(hsh(cx, cy, seed + 7) * len(GLYPHS)) % len(GLYPHS)]
        lx, ly = i - cx * cell, j - cy * cell
        if lx >= 7 or ly >= 7 or g[ly][lx] != "#":
            return None
        return px(col), px(glow, a)
    return f


def lines_glow(seed, col=(180, 100, 255), glow=(190, 120, 255), a=190, step=5, faces=("front", "left", "right", "back")):
    """Thin glowing veins: wandering vertical lines (violet veins creeping over the skin)."""
    def f(face, i, j, fw, fh, r):
        if face not in faces:
            return None
        x = i + 2.2 * SIN(j * 0.31 + seed) + 1.4 * SIN(j * 0.83 + seed * 2)
        if abs((x % step) - step / 2.0) < 0.55 and hsh(int(x // step), j // 6, seed) > 0.4:
            return px(col), px(glow, a)
        return None
    return f


def spin(a, clip, bone, turns=1.0, axis=1, steps=4):
    """A bone turning about one axis for the whole clip: whole turns, so a loop is seamless."""
    L = a.clips[clip]["animation_length"]
    fr = {}
    for k in range(steps + 1):
        v = [0.0, 0.0, 0.0]
        v[axis] = 360.0 * turns * k / steps
        fr[L * k / steps] = tuple(v)
    a.rot(clip, bone, fr)


def spin_all(a, bone, period, axis=1, sign=1.0):
    """spin() in every clip, a whole number of turns per clip (the nearest to one turn per `period` seconds)."""
    for clip, c in a.clips.items():
        L = c["animation_length"]
        n = max(1, round(L / period))
        spin(a, clip, bone, sign * n, axis)


def bob_all(a, bone, amp, period, phase=0.0):
    for clip, c in a.clips.items():
        L = c["animation_length"]
        n = max(1, round(L / period))
        fr = {}
        for k in range(9):
            t = L * k / 8
            fr[t] = (0, round(amp * (SIN(TAU * n * k / 8 + phase) - SIN(phase)), 3), 0)
        a.pos(clip, bone, fr)


def flap_all(a, bone, amp, period, sign=1.0, axis=2, base=(0, 0, 0), phase=0.0):
    """A wing flapping in every clip (whole beats per clip): rotation about `axis` by base + amp * sin."""
    for clip, c in a.clips.items():
        L = c["animation_length"]
        n = max(1, round(L / period))
        fr = {}
        for k in range(17):
            t = L * k / 16
            v = list(base)
            v[axis] = base[axis] + sign * amp * (SIN(TAU * n * k / 16 + phase) - SIN(phase))
            fr[t] = tuple(v)
        a.rot(clip, bone, fr)


def grow(a, clip, bones, t0, t1, lo=0.04):
    """In `clip` the bones scale up from almost nothing (t0) to full size (t1): the devil features bursting out during the transformation."""
    L = a.clips[clip]["animation_length"]
    for b in bones:
        a.scale(clip, b, {0.0: (lo, lo, lo), t0: (lo, lo, lo), t1: (1.12, 1.12, 1.12), t1 + 0.25: (1, 1, 1), L: (1, 1, 1)})


def ring(h, bone, n, radius, y, size, paint, tilt=0.0, phase=0.0):
    """n small boxes round a circle of the given radius at height y, long side along the ring: halo segments."""
    sx, sy, sz = size
    for k in range(n):
        ang = phase + 360.0 * k / n
        h.box(bone, (radius - sx / 2, y, -sz / 2), size, paint, rotation=(0, ang, 0), pivot=(0, y, 0))


def orb_paint(dark, glow):
    """A small orb: a dark core with a glowing rim, brightest at the centre of each face."""
    def p(face, i, j, fw, fh):
        cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
        r = math.hypot(i - cx, j - cy) / max(0.7, math.hypot(cx, cy))
        k = clamp(1.0 - r * 0.9)
        return px(A3(dark) * (0.7 + 0.5 * k)), px(glow, 255 * (0.35 + 0.65 * k))
    return p


def halo_paint(col, a=190):
    def p(face, i, j, fw, fh):
        return None, px(col, a * (0.7 + 0.3 * FACE_K[face]))
    return p


def horn_p(salt=0, glow=None, cols=((30, 22, 44), (60, 44, 84), (120, 90, 150))):
    """A horn: dark at the root, ridged rings, a lighter tip (glow optional)."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.2 + 0.6 * (1 - v) + 0.1 * fz(i, j, salt)
        if j % 4 == 0:
            t -= 0.15
        c = ramp(cols, t) * FACE_K[face]
        g = px(glow[:3], glow[3] * (1 - v) ** 1.6) if glow else None
        return px(c), g
    return p


def creep(seed, region, thr=0.55, dark=(70, 40, 110), light=(126, 78, 182), glow=(170, 100, 255, 120), faces=("front", "back", "left", "right"), scale=0.45, hard=None):
    """Patches of devil skin creeping over a surface: region(face, i, j, fw, fh) -> 0..1 says where they are likely; noise makes the edge ragged;
    the outer rim of a patch is a lighter, glowing colour, the inside the dark colour."""
    def f(face, i, j, fw, fh, r):
        if face not in faces:
            return None
        w = region(face, i, j, fw, fh)
        if w <= 0:
            return None
        m = w * 1.25 + (nz(i * scale, j * scale, seed) * 0.9)
        if m > thr + 0.13:
            base = A3(dark) + fz(i, j, seed + 3) * 10
            return px(base * (0.9 + 0.1 * FACE_K[face])), (px(glow[:3], glow[3] * 0.25) if glow[3] > 60 and (i + j) % 7 == 0 else None)
        if m > thr:
            return px(light), px(glow[:3], glow[3])
        return None
    return f


def jaw_region(face, i, j, fw, fh):
    v = j / max(1, fh - 1)
    if face == "front":
        u = abs(i - (fw - 1) / 2.0) / ((fw - 1) / 2.0)
        return clamp((v - 0.5) * 2.4) * (0.3 + 0.7 * u)
    if face in ("left", "right"):
        return clamp((v - 0.45) * 2.2)
    return 0.0


def hand_region(face, i, j, fw, fh):
    return clamp(j / max(1, fh - 1) * 1.1 + 0.1)


def pair_bone(h, name, parent, pivot, rot=None, parent_r=None):
    """Bones <name>_l (+x) and <name>_r (-x) with mirrored pivot and rotation."""
    for suf, sg in (("_l", 1), ("_r", -1)):
        par = (parent + suf) if parent_r is None else parent
        rr = None if not rot else (rot[0], rot[1] * sg, rot[2] * sg)
        h.bone(name + suf, par, (pivot[0] * sg, pivot[1], pivot[2]), rr)


# =========================================================================================================================================
#                                                              DANTE
# =========================================================================================================================================
D_SKIN = ((244, 220, 204), (218, 184, 166), (252, 236, 224))
D_TUNIC, D_TUNIC_D, D_TUNIC_L = (118, 80, 52), (74, 48, 32), (156, 112, 74)
D_RED, D_RED_D, D_RED_L = (176, 34, 42), (112, 18, 28), (222, 78, 72)
D_BOOT, D_BOOT_L = (88, 60, 42), (128, 92, 60)
D_TROUSER, D_TROUSER_D = (72, 62, 58), (44, 38, 36)

DANTE_FACE = [
    "HHHHHHHHHHHHHHHH",
    "HHHhHHHHHHhHHHHH",
    "HH.....k......HH",
    "HH..kkkkkkkk..HH",
    "Hs.....k......sH",
    "Hs..kkkkkkkk..sH",
    "H......k.......H",
    "H.s..........s.H",
    "H..bbbb..bbbb..H",
    "s..lGGl..lGGl..s",
    "s..wggw..wggw..s",
    "...s..........s.",
    "...s.n....n.s...",
    "....mMMMMMMm....",
    "s...ttttttt....s",
    "s....tttttt....s",
]
DANTE_LEG = {
    "H": (22, 22, 30), "h": (56, 60, 88), "k": (12, 10, 14), "b": (30, 24, 24), "l": (56, 40, 38), "w": (210, 212, 202),
    "g": ((78, 168, 90), (90, 230, 120, 70)), "G": ((36, 100, 56), (50, 160, 80, 50)), "n": (200, 160, 144), "m": (176, 96, 92),
    "M": (122, 58, 60), "t": (34, 28, 28), "s": (212, 176, 158),
}


def dante_painters(form):
    skin_p = skin(1, D_SKIN)
    legend = dict(DANTE_LEG)
    if form == "hybrid":
        legend["g"] = ((176, 96, 255), (190, 110, 255, 255))
        legend["G"] = ((96, 40, 170), (150, 70, 255, 200))
        legend["w"] = (40, 24, 60)
        for ch, r in (("s", ((84, 52, 124), (140, 80, 255, 90))), ("u", ((118, 74, 170), (170, 100, 255, 150)))):
            legend[ch] = r
    art = face_art(DANTE_FACE, legend, skin_p)
    hp = hair(2, (26, 26, 36), (92, 100, 150), sheen=0.7)

    tunic = cloth(D_TUNIC, D_TUNIC_D, D_TUNIC_L, 3, folds=3, seam=False)

    def torso(face, i, j, fw, fh):
        c, g = tunic(face, i, j, fw, fh)
        if face == "front":
            mid = fw // 2
            if i in (mid - 1, mid):
                return px(A3(D_TUNIC_D) * 0.8), None                           # the placket
            if i in (mid - 2, mid + 1):
                return px(A3(D_TUNIC_L) * 0.9), None
            if i in (mid - 1, mid) and j % 6 == 3:
                return px(GOLD), None
            if j == fh - 1:
                return px(A3(D_TUNIC_D)), None
        if face == "back" or face == "front":
            if i < 2 or i > fw - 3:
                c = px(A3(c[:3]) * 0.88)
        return c, g
    belt = leather((46, 32, 26), (80, 58, 44), 4, buckle=lambda i, j, fw, fh: (px(GOLD_L if (i - fw // 2) in (-3, -2, 2, 3) or j in (1, fh - 2) else GOLD_D), None) if abs(i - fw // 2 + 0.5) < 3.6 and 0 < j < fh - 1 else None)
    sleeve_u = cloth(D_TUNIC, D_TUNIC_D, D_TUNIC_L, 5, folds=2, seam=False)
    sleeve_f_leather = leather((52, 36, 28), (96, 68, 48), 6, stitch=(150, 118, 78))
    sleeve_f = cloth(D_TUNIC, D_TUNIC_D, D_TUNIC_L, 7, folds=2, seam=False)

    def forearm(face, i, j, fw, fh):
        if j >= fh * 0.45:
            return sleeve_f_leather(face, i, j, fw, fh)                         # a leather bracer on the lower arm
        return sleeve_f(face, i, j, fw, fh)
    hand = skin(8, D_SKIN)

    def hand_p(face, i, j, fw, fh):
        c, g = hand(face, i, j, fw, fh)
        if face in ("front", "back") and j >= fh - 3 and i % 3 == 2:
            c = px(A3(c[:3]) * 0.78)
        return c, g
    thigh = cloth(D_TROUSER, D_TROUSER_D, (96, 84, 78), 9, folds=2, seam=False)
    boot = leather(D_BOOT, D_BOOT_L, 10, stitch=(180, 140, 90), wrinkles=1.0)

    def boot_p(face, i, j, fw, fh):
        if j < 4 and face in SIDES:
            c, g = leather((126, 92, 62), (166, 124, 84), 12, stitch=None)(face, i, j, fw, fh)     # the lighter folded cuff
            return c, g
        return boot(face, i, j, fw, fh)
    foot = leather((70, 48, 34), (104, 74, 52), 13, stitch=None)
    skirt = cloth(D_TUNIC, D_TUNIC_D, D_TUNIC_L, 14, folds=4, seam=False)

    def skirt_p(face, i, j, fw, fh):
        c, g = skirt(face, i, j, fw, fh)
        if j >= fh - 2 and face in SIDES:
            return px(A3(D_TUNIC_D) * 1.1 + fz(i, j, 3) * 8), None              # a dark hem
        return c, g
    return dict(torso=torso, belt=belt, skirt=skirt_p, thigh=thigh, boot=boot_p, foot=foot, sleeve_u=sleeve_u, sleeve_f=forearm, hand=hand_p,
                skin=skin_p, hair=hp, art=art)


def scarf_p(salt=1):
    red = cloth(D_RED, D_RED_D, D_RED_L, salt, folds=3, seam=False, weave=1.4)

    def p(face, i, j, fw, fh):
        c, g = red(face, i, j, fw, fh)
        if face in SIDES and j >= fh - 3:
            sid = i // 2
            if j - (fh - 3) > 3 * hsh(sid, salt, 5):                              # fringe
                return None, None
        if face in SIDES and j % 7 == 5:
            c = px(A3(c[:3]) * 0.86)
        return c, g
    return p


def cloak_painters(edge=(66, 62, 76), tint=None, glow_edge=None, dark=False):
    """The cloak: black cloth with soft folds; the lower segment ends in ragged dark-grey fur (darker, with a glowing rim, for a hybrid form)."""
    base = tint[0] if tint else CLOAK
    cp = cloth(base, CLOAK_D, CLOAK_L, 21, folds=5, seam=False, weave=1.3, lining=(30, 20, 30), fold_amp=2.4)
    cols = [(10, 10, 14), (24, 22, 30), (44, 42, 54), (74, 72, 86), (104, 102, 118)]
    if dark:
        cols = [(4, 4, 8), (12, 10, 18), (26, 22, 38), (46, 40, 66), (70, 60, 100)]
    furp = fur(22, cols, root=0.2, tip=0.5, saw=6, width=2.0)

    def hem(face, i, j, fw, fh):
        if face in SIDES and j >= fh - 10:
            c, g = furp(face, i, j, fw, fh)
            if glow_edge and c is not None and j >= fh - 4 and hsh(i // 2, j, 9) > 0.55:
                g = px(glow_edge[:3], glow_edge[3] * (0.4 + 0.6 * (j - (fh - 4)) / 3.0))
            return c, g
        return cp(face, i, j, fw, fh)
    return cp, hem


def build_dante(form):
    P = dict(tw=9, td=5, aw=4.5, ad=4.5, lw=4, ld=4.5, lx=2.35, hw=10.5)
    h = Build("dante_" + form, 512, 512)
    h.raise_by = UP
    rig_bones(h, P)
    pt = dante_painters(form)
    S_ = dict(pt)
    S_["head"] = head_paint(pt["art"], pt["skin"], pt["hair"], ear=(212, 176, 158), sideburn=3, hair_rows=9, back_rows=12)
    if form == "hybrid":
        S_["head"] = overlay(S_["head"], creep(71, jaw_region, 0.66))
        S_["hand"] = overlay(S_["hand"], creep(72, hand_region, 0.5))
        S_["sleeve_f"] = overlay(S_["sleeve_f"], lines_glow(73, a=150, step=6, faces=("front", "back")))
    humanoid_common(h, P, S_)
    collar_and_fur(h, P, lambda s: fur(s, FUR_RAMP, root=0.7, tip=0.2, saw=4, deep=0.15), solid_collar(), lambda s: fur(s, FUR_RAMP, root=0.6, tip=0.8, width=1.4))
    cp, hem = cloak_painters(glow_edge=(170, 90, 255, 200) if form == "hybrid" else None, dark=form == "hybrid")
    cloak_parts(h, P, cp, hem)
    crown(h, P, gem=(150, 70, 240) if form == "hybrid" else (220, 40, 56), gem_glow=(190, 110, 255) if form == "hybrid" else (255, 96, 96))
    # the red scarf: two tails hanging down the chest under the fur
    h.box("body", (0.5, 11.0, -(P["td"] / 2 + 0.95)), (3.0, 8.6, 0.8), scarf_p(1), rotation=(0, 0, 3), pivot=(2, 19.6, 0))
    h.box("body", (-3.1, 12.5, -(P["td"] / 2 + 0.9)), (2.4, 7.2, 0.8), scarf_p(2), rotation=(0, 0, -4), pivot=(-2, 19.6, 0))
    # hair: swept back, a quiff, the nape
    hp = pt["hair"]
    h.box("hair", (-4.25, 30.5, -4.25), (8.5, 2.5, 8.5), hp)
    h.box("hair", (-3.0, 31.5, -4.75), (6.0, 1.5, 2.5), hp, rotation=(14, 0, 0), pivot=(0, 31.2, -4.0))
    h.box("hair", (-4.25, 25.5, 3.9), (8.5, 7.5, 1.5), hp)
    h.box("hair", (-3.5, 24.4, 4.4), (7.0, 2.0, 1.4), hp)
    # nose and goatee
    h.box("head", (-0.5, 24.7, -5.0), (1.0, 1.5, 1.0), skin(15, D_SKIN))
    h.box("head", (-1.0, 23.2, -4.5), (2.0, 1.5, 0.5), hair(16, (40, 32, 30), (84, 70, 64)))
    humanoid_clips(h.A, "dante")
    if form == "hybrid":
        dante_hybrid_extras(h, P)
    return h, P


def solid_collar():
    """The tall black high collar: a fine knit with ribs."""
    base = cloth((22, 21, 28), (8, 8, 12), (44, 42, 56), 31, folds=0, seam=False, weave=1.6)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face in SIDES and i % 3 == 0:
            c = px(A3(c[:3]) * 0.82)
        if face in SIDES and j == 0:
            c = px(A3(c[:3]) * 1.25)
        return c, g
    return p


def dante_hybrid_extras(h, P):
    """100% Lucifero: curved horns through the crown, a gravity halo (two counter-turning rings and three dark orbs, glow only), all bursting out in
    the transform clip."""
    horn = horn_p(81, glow=(190, 110, 255, 200))
    h.bone("horns", "head", (0, 31, 0))
    # each horn: a stout root, a middle leaning out and back, a hooked tip
    h.pair("horns", "horns", (2.0, 31.0, -1.5), (2.0, 3.0, 2.0), horn, rotation=(0, 0, -10), pivot=(3.0, 31.0, -0.5))
    h.pair("horns", "horns", (3.4, 33.6, -1.2), (1.5, 3.0, 1.5), horn, rotation=(14, 0, -32), pivot=(4.1, 33.6, -0.5))
    h.pair("horns", "horns", (5.2, 36.0, -0.2), (1.0, 2.5, 1.0), horn, rotation=(34, 0, -58), pivot=(5.7, 36.0, 0.3))
    # the halo
    h.bone("halo", "body", (0, 22, 0))
    h.bone("halo_tilt", "halo", (0, 22, 0), (16, 0, 10))
    h.bone("halo_in", "halo_tilt", (0, 22, 0))
    h.bone("orbs", "body", (0, 22, 0))
    hp_ = halo_paint((170, 96, 255), 200)
    hq_ = halo_paint((120, 60, 220), 140)
    ring(h, "halo", 18, 15.0, 22.0, (0.5, 0.5, 3.0), hp_)
    ring(h, "halo_in", 14, 11.5, 22.0, (0.5, 0.5, 2.5), hq_, phase=12)
    orb = orb_paint((24, 14, 44), (190, 120, 255))
    for k, (r_, y_, ph) in enumerate(((9.0, 16.0, 0), (9.0, 22.0, 120), (9.0, 28.0, 240))):
        x_, z_ = r_ * COS(math.radians(ph)), r_ * SIN(math.radians(ph))
        h.box("orbs", (x_ - 1.0, y_ - 1.0, z_ - 1.0), (2.0, 2.0, 2.0), orb)
    A = h.A
    spin_all(A, "halo", 7.0)
    spin_all(A, "halo_in", 5.0, sign=-1.0)
    spin_all(A, "orbs", 4.0, sign=-1.0)
    bob_all(A, "orbs", 0.8, 3.0)
    grow(A, "transform", ("horns",), 0.8, 1.7)
    grow(A, "transform", ("halo", "orbs"), 1.0, 2.0)


# =========================================================================================================================================
#                                                              VANICA
# =========================================================================================================================================
V_SKIN = ((250, 230, 222), (224, 190, 186), (255, 244, 238))
V_DRESS, V_DRESS_D, V_DRESS_L = (116, 58, 54), (70, 32, 34), (158, 90, 78)
V_BLACK, V_BLACK_D, V_BLACK_L = (30, 28, 38), (12, 11, 17), (66, 64, 84)
V_STRAP = (62, 38, 32)
VANICA_FACE = [
    "HHHHHHHHHHHHHHHH",
    "HHHhHHHHHHHhHHHH",
    "HHHHHHHHHHHHHHHH",
    "HHHHHHHHHHHHHH.H",
    "HHHHHHHHHHH....H",
    "HHHHHHH........H",
    "kkkkkkkkkkkkkkkk",
    "H.kkkkk.eeeeeee.",
    "H.kkkkk.llllll..",
    "H.kkYkk.wrrrrw..",
    "s.kYykk.wrWrrw..",
    "s.kkYkk.s.ssss.p",
    "s..p..m.XXXXXXm.",
    "s....mMMMMMMMMm.",
    "s.....MMTTTTMM..",
    "s......mmmmmm...",
]
def _put(rows, cells, ch):
    rows = [list(r) for r in rows]
    for (r, c) in cells:
        rows[r][c] = ch
    return ["".join(r) for r in rows]


VANICA_FACE_H = _put(VANICA_FACE, [(11, 9), (11, 11), (11, 13), (12, 10), (12, 12), (11, 1), (12, 2), (12, 3), (13, 1), (13, 2)], "S")
VANICA_LEG = {
    "H": (24, 22, 34), "h": (96, 100, 156), "k": (14, 12, 18), "e": (40, 28, 36), "l": (44, 22, 30), "w": (244, 238, 238),
    "r": ((210, 42, 66), (255, 60, 90, 160)), "R": ((140, 20, 40), (200, 30, 60, 120)), "W": ((255, 255, 255), (255, 200, 200, 255)),
    "Y": ((236, 194, 74), (255, 214, 100, 110)), "y": (150, 104, 40), "m": (200, 72, 90), "M": (116, 24, 42), "T": (226, 104, 124), "X": (250, 248, 244),
    "s": (226, 196, 190), "p": (246, 168, 170), "S": ((220, 24, 48), (255, 40, 70, 255)),
}


def ring_buckle(outer=1.0, inner=0.5):
    """A flat gold ring (a hole in the middle): the big belt buckle."""
    def p(face, i, j, fw, fh):
        if face not in ("front", "back"):
            return px(GOLD_D), None
        cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
        r = math.hypot(i - cx, j - cy) / (min(fw, fh) / 2.0)
        if r > outer + 0.15 or r < inner - 0.05:
            return None, None
        k = 0.7 + 0.5 * (1 - (j / max(1, fh - 1)))
        return px(A3(GOLD) * k + (18 if (i + j) % 4 == 0 else 0)), None
    return p


def straps_cross(base, edge, thick=2.0):
    """An X of two straps over a torso front: painted on top of the cloth painter."""
    def over(c, face, i, j, fw, fh):
        if face != "front":
            return c
        u, v = i / max(1, fw - 1), j / max(1, fh - 1)
        d1 = abs((u - 0.0) * fh - v * fw * 1.0) / math.hypot(fh, fw) * 1.0
        d2 = abs((u - 1.0) * fh + v * fw * 1.0) / math.hypot(fh, fw) * 1.0
        for d in (d1, d2):
            if d < thick * 0.5:
                return A3(base) * (0.8 + 0.3 * (j % 2)) + fz(i, j, 3) * 6
            if d < thick * 0.5 + 0.8:
                return A3(edge)
        return c
    return over


def vanica_painters(form):
    skin_p = skin(31, V_SKIN)
    legend = dict(VANICA_LEG)
    if form == "hybrid":
        legend["r"] = ((236, 30, 60), (255, 40, 70, 255))
        legend["R"] = ((170, 10, 40), (255, 30, 60, 230))
        legend["w"] = (250, 214, 214)
    base_face = face_art(VANICA_FACE_H if form == "hybrid" else VANICA_FACE, legend, skin_p)
    hp = hair(32, (24, 22, 34), (96, 100, 156), sheen=0.6)
    dress = cloth(V_DRESS, V_DRESS_D, V_DRESS_L, 33, folds=3, seam=False, weave=1.2)
    cross = straps_cross(V_STRAP, (40, 24, 22), 2.5)

    def torso(face, i, j, fw, fh):
        c, g = dress(face, i, j, fw, fh)
        return px(cross(A3(c[:3]), face, i, j, fw, fh)), g
    belt = leather((74, 46, 34), (110, 74, 54), 34, stitch=(170, 130, 84))
    skirt = cloth(V_DRESS, V_DRESS_D, V_DRESS_L, 35, folds=6, seam=False, weave=1.2)

    def skirt_p(face, i, j, fw, fh):
        c, g = skirt(face, i, j, fw, fh)
        if face in SIDES and j >= fh - 2:
            return px(A3(V_DRESS_D) * 0.9 + fz(i, j, 3) * 6), None
        if face in SIDES and i % 6 == 0:
            c = px(A3(c[:3]) * 0.86)
        return c, g
    black = cloth(V_BLACK, V_BLACK_D, V_BLACK_L, 36, folds=2, seam=False, weave=1.0)
    boot = leather((28, 26, 34), (74, 70, 90), 37, stitch=None, wrinkles=1.0)

    def thigh_p(face, i, j, fw, fh):
        if j < 3 and face in SIDES:
            return leather(V_STRAP, (110, 74, 54), 38, stitch=(170, 130, 84))(face, i, j, fw, fh)       # a garter strap
        return boot(face, i, j, fw, fh)
    hand = skin(39, V_SKIN)

    def hand_p(face, i, j, fw, fh):
        c, g = hand(face, i, j, fw, fh)
        if face in ("front", "back") and j >= fh - 2:
            c = px((226, 76, 90))                                                                       # nails
        return c, g
    foot = leather((24, 22, 30), (70, 66, 86), 40, stitch=None)
    return dict(torso=torso, belt=belt, skirt=skirt_p, thigh=thigh_p, boot=boot, foot=foot, sleeve_u=black, sleeve_f=black, hand=hand_p, skin=skin_p,
                hair=hp, art=base_face, legend=legend)


def build_vanica(form):
    P = dict(tw=7, td=4, aw=3, ad=3, lw=3.5, ld=4, lx=1.85, hw=9.0, skirt=(10, 3.5, 7.5, 6.5))
    h = Build("vanica_" + form, 512, 512)
    h.raise_by = UP
    rig_bones(h, P)
    pt = vanica_painters(form)
    S_ = dict(pt)
    S_["head"] = head_paint(pt["art"], pt["skin"], pt["hair"], ear=(240, 206, 200), sideburn=3, hair_rows=9, back_rows=10)
    if form == "hybrid":
        S_["hand"] = overlay(S_["hand"], sigils(172, cell=5, density=0.6))
        for key_, sd in (("sleeve_u", 173), ("sleeve_f", 174), ("thigh", 175), ("boot", 176)):
            S_[key_] = overlay(S_[key_], sigils(sd, cell=7, density=0.42))
        S_["torso"] = overlay(S_["torso"], sigils(177, cell=7, density=0.3, col=(250, 70, 80), faces=("back", "left", "right")))
    humanoid_common(h, P, S_)
    collar_and_fur(h, P, lambda s: fur(s, FUR_RAMP, root=0.7, tip=0.2, saw=4, deep=0.15), solid_collar(), lambda s: fur(s, FUR_RAMP, root=0.6, tip=0.8, width=1.4), rng_salt=300)
    cp, hem = cloak_painters(glow_edge=(255, 40, 70, 200) if form == "hybrid" else None, dark=form == "hybrid")
    cloak_parts(h, P, cp, hem)
    crown(h, P, tilt=(0, 0, 5), shift=(0.4, 0.0), gem=(230, 20, 50) if form == "hybrid" else (220, 40, 56), gem_glow=(255, 40, 70) if form == "hybrid" else (255, 96, 96))
    hp = pt["hair"]
    # the ring buckle on the belt and the second belt
    h.box("body", (-1.5, 10.0, -(P["td"] / 2 + 0.7)), (3.0, 3.0, 0.5), ring_buckle())
    h.box("body", (-P["tw"] / 2, 8.0, -P["td"] / 2), (P["tw"], 1, P["td"]), leather((62, 38, 32), (100, 70, 50), 41, stitch=None), inflate=0.45, rotation=(0, 0, -7), pivot=(0, 9.0, 0))
    # bob hair: cap, back, side curtains (the eyepatch side longer), bangs
    h.box("hair", (-4.25, 30.5, -4.25), (8.5, 2.5, 8.5), hp)
    h.box("hair", (-4.25, 25.0, 3.9), (8.5, 7.0, 1.5), hp)
    h.box("hair", (-5.0, 25.0, -3.5), (1.0, 6.5, 7.5), hp)                                    # -x (the viewer's left): long curtain
    h.box("hair", (4.0, 27.5, -1.0), (1.0, 4.0, 5.0), hp)                                     # +x: short, tucked behind the ear
    h.box("hair", (-4.5, 29.0, -4.85), (5.0, 2.5, 1.0), hp, rotation=(0, 0, -6), pivot=(-2, 31, -4.5))
    h.box("hair", (0.5, 30.0, -4.85), (3.5, 1.5, 1.0), hp, rotation=(0, 0, 6), pivot=(2, 31, -4.5))
    # the black eyepatch, raised a little, with its gold emblem (viewer's left eye, -x)
    patch = cloth((18, 16, 22), (8, 8, 10), (46, 44, 58), 42, seam=False)

    def patch_p(face, i, j, fw, fh):
        if face == "front":
            cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
            if abs(i - cx) + abs(j - cy) <= 2.6:
                ed = abs(i - cx) + abs(j - cy) > 1.6
                return px(A3(GOLD_D) if ed else A3(GOLD_L) * 0.95), px(GOLD, 120)
        return patch(face, i, j, fw, fh)
    h.box("head", (-4.5, 26.0, -4.5), (2.5, 2.5, 0.5), patch_p)
    h.box("head", (-0.5, 24.6, -4.9), (1.0, 1.5, 1.0), skin(43, V_SKIN))                       # nose
    humanoid_clips(h.A, "vanica")
    if form == "hybrid":
        vanica_hybrid_extras(h, P)
    return h, P


def vanica_hybrid_extras(h, P):
    """100% Megicula: small curved horns, long red nail-claws on both hands, and a halo of floating blood droplets (three rings turning at different
    speeds); the curse sigils are painted into the textures. Everything bursts out in the transform clip."""
    horn = horn_p(181, glow=(255, 50, 80, 170), cols=((50, 8, 20), (110, 18, 40), (200, 70, 90)))
    h.bone("horns", "head", (0, 31, 0))
    h.pair("horns", "horns", (2.0, 31.0, -2.5), (1.5, 2.5, 1.5), horn, rotation=(-18, 0, -14), pivot=(2.7, 31.0, -1.8))
    h.pair("horns", "horns", (2.9, 33.2, -3.4), (1.0, 2.5, 1.0), horn, rotation=(-40, 0, -30), pivot=(3.4, 33.2, -2.9))
    # claws: five thin crimson blades on each hand (the forearm bone), hooked forward
    claw = horn_p(182, glow=(255, 70, 90, 200), cols=((120, 10, 30), (200, 30, 56), (255, 150, 160)))
    sx = P["sx"]
    h.bone("claws_l", "forearm_l", (sx, 8.0, 0))
    h.bone("claws_r", "forearm_r", (-sx, 8.0, 0))
    for k in range(5):
        dx = -0.9 + 0.45 * k
        h.pair("claws_l", "claws_r", (sx + dx - 0.25, 4.2, -0.9 + 0.1 * k), (0.5, 3.5, 0.5), claw, rotation=(24 + 3 * k, 0, 3 * (k - 2)), pivot=(sx + dx, 7.7, -0.6))
    # droplets
    drop = orb_paint((120, 6, 20), (255, 40, 70))
    for nm, ph, ys in (("drops_a", 0, (30.0, 24.0, 17.0)), ("drops_b", 120, (28.0, 21.0, 15.0)), ("drops_c", 240, (26.0, 19.0, 33.0))):
        h.bone(nm, "body", (0, 22, 0))
        for k, y_ in enumerate(ys):
            a_ = math.radians(ph + 100 * k + 20)
            r_ = 11.0 + 2.0 * k
            h.box(nm, (r_ * COS(a_) - 0.5, y_, r_ * SIN(a_) - 0.5), (1.0, 1.5, 1.0), drop)
    A = h.A
    spin_all(A, "drops_a", 4.5)
    spin_all(A, "drops_b", 6.0, sign=-1.0)
    spin_all(A, "drops_c", 5.0)
    bob_all(A, "drops_a", 1.0, 2.5)
    bob_all(A, "drops_b", 1.3, 3.5, 1.0)
    bob_all(A, "drops_c", 0.8, 3.0, 2.0)
    grow(A, "transform", ("horns", "claws_l", "claws_r"), 0.8, 1.7)
    grow(A, "transform", ("drops_a", "drops_b", "drops_c"), 1.0, 2.0)


# =========================================================================================================================================
#                                                              ZENON
# =========================================================================================================================================
Z_SKIN = ((238, 216, 206), (208, 180, 176), (250, 238, 230))
Z_VEST, Z_VEST_D, Z_VEST_L = (132, 54, 50), (78, 30, 32), (180, 92, 80)
Z_SHIRT, Z_SHIRT_D, Z_SHIRT_L = (48, 44, 58), (22, 20, 30), (84, 80, 102)
Z_BELT = (112, 78, 54)
ZENON_FACE = [
    "HHHHHHHHHHHHHHHH",
    "HHhHHHHHHHHhHHHH",
    "HHHHHHHHHHHHHHHH",
    "HH.HH.HHH.HH.HHH",
    "H..H...H...H...H",
    "Hs.kkkkkkkkkk.sH",
    "Hs........k...sH",
    "H.s.eeee.ekee.sH",
    "H..llll..lklll.H",
    "s..wiiw..wkiw..s",
    "s..wIIw..wkIw..s",
    "..ss.......k.ss.",
    "..ss.n.nn.kkkk..",
    "s.....mmmm.k....",
    "s...............",
    "................",
]
ZENON_LEG = {
    "H": (22, 22, 30), "h": (60, 64, 92), "k": (12, 10, 14), "e": (30, 26, 28), "l": (74, 58, 60), "w": (228, 230, 232),
    "i": ((186, 204, 218), (150, 180, 220, 70)), "I": ((124, 146, 168), (110, 140, 190, 40)), "s": (208, 182, 176), "n": (204, 172, 166), "m": (178, 118, 118),
}


def zenon_painters(form):
    skin_p = skin(51, Z_SKIN)
    legend = dict(ZENON_LEG)
    if form == "hybrid":                                                  # hollow sockets with a glowing pinpoint
        legend["i"] = ((14, 16, 20), (190, 215, 255, 255))
        legend["I"] = ((6, 6, 12), (120, 160, 255, 150))
        legend["w"] = (10, 10, 14)
        legend["l"] = (30, 24, 24)
    art = face_art(ZENON_FACE, legend, skin_p)
    hp = hair(52, (22, 22, 30), (70, 76, 108), sheen=0.5)
    shirt = cloth(Z_SHIRT, Z_SHIRT_D, Z_SHIRT_L, 53, folds=3, seam=False, weave=1.0)
    vest = cloth(Z_VEST, Z_VEST_D, Z_VEST_L, 54, folds=3, seam=False, weave=1.3)
    strap = leather(Z_BELT, (126, 90, 62), 55, stitch=(170, 140, 100))

    def torso(face, i, j, fw, fh):
        if face == "front":
            mid = fw / 2.0
            u = abs(i + 0.5 - mid) / mid                                         # 0 centre, 1 edge
            if u < 0.28 + 0.0 * j and j < fh * 0.7:
                c, g = shirt(face, i, j, fw, fh)                                 # the dark shirt showing in the V of the open vest
                return c, g
            c, g = vest(face, i, j, fw, fh)
            for sgn in (1, -1):                                                  # two straps crossing the chest
                t = (i - (mid - 0.5)) * sgn
                d = abs(j - (0.2 * fh + 0.45 * t + 0.1 * fh))
                if d < 1.6:
                    c, g = strap(face, i, j, fw, fh)
            if abs(i + 0.5 - mid) < 1.0 and j % 5 == 2 and j > 3:
                return px((200, 200, 208)), None                                  # a stud
            return c, g
        return vest(face, i, j, fw, fh)
    belt = leather((46, 34, 30), (84, 64, 50), 56, buckle=lambda i, j, fw, fh: (px((198, 198, 208) if (abs(i - fw // 2) in (2,) or j in (1, fh - 2)) else (96, 96, 108)), None) if abs(i - fw // 2 + 0.5) < 3 and 0 < j < fh - 1 else None)
    skirt = cloth(Z_VEST, Z_VEST_D, Z_VEST_L, 57, folds=4, seam=False, weave=1.3)

    def skirt_p(face, i, j, fw, fh):
        c, g = skirt(face, i, j, fw, fh)
        if face in SIDES and j >= fh - 2:
            return px(A3(Z_VEST_D) * 0.95), None
        return c, g
    trouser = cloth((38, 36, 46), (16, 15, 22), (66, 64, 82), 58, folds=2, seam=False)
    boot = leather((30, 28, 34), (76, 72, 88), 59, stitch=(90, 88, 100), wrinkles=1.0)
    hand = skin(60, Z_SKIN)
    brace = leather((38, 30, 30), (84, 64, 54), 61, stitch=(150, 130, 100))

    def forearm(face, i, j, fw, fh):
        if j >= fh * 0.4:
            return brace(face, i, j, fw, fh)
        return shirt(face, i, j, fw, fh)
    foot = leather((26, 24, 30), (70, 66, 82), 62, stitch=None)
    return dict(torso=torso, belt=belt, skirt=skirt_p, thigh=trouser, boot=boot, foot=foot, sleeve_u=shirt, sleeve_f=forearm, hand=hand, skin=skin_p,
                hair=hp, art=art, legend=legend)


def build_zenon(form):
    P = dict(tw=8, td=4, aw=3.5, ad=3.5, lw=3.5, ld=4, lx=2.0, hw=9.5, skirt=(9.5, 3, 5.5, 7.0))
    h = Build("zenon_" + form, 512, 512)
    h.raise_by = UP
    rig_bones(h, P)
    pt = zenon_painters(form)
    S_ = dict(pt)
    S_["head"] = head_paint(pt["art"], pt["skin"], pt["hair"], ear=(220, 192, 184), sideburn=3, hair_rows=8, back_rows=10)
    if form == "hybrid":
        S_["head"] = overlay(S_["head"], creep(191, jaw_region, 0.5, dark=(196, 188, 164), light=(236, 230, 208), glow=(160, 190, 255, 90)))
        S_["hand"] = overlay(S_["hand"], creep(192, hand_region, 0.62, dark=(196, 188, 164), light=(236, 230, 208), glow=(160, 190, 255, 90)))
        S_["torso"] = overlay(S_["torso"], ribs_overlay)
    humanoid_common(h, P, S_)
    collar_and_fur(h, P, lambda s: fur(s, FUR_RAMP, root=0.7, tip=0.2, saw=4, deep=0.15), solid_collar(), lambda s: fur(s, FUR_RAMP, root=0.6, tip=0.8, width=1.4), rng_salt=500)
    cp, hem = cloak_painters(glow_edge=(140, 170, 255, 150) if form == "hybrid" else None, dark=form == "hybrid")
    cloak_parts(h, P, cp, hem)
    crown(h, P, gem=(90, 120, 220) if form == "hybrid" else (220, 40, 56), gem_glow=(150, 180, 255) if form == "hybrid" else (255, 96, 96))
    hp = pt["hair"]
    # short messy hair: a cap, spikes, a ragged fringe, the nape
    h.box("hair", (-4.25, 30.5, -4.25), (8.5, 2.5, 8.5), hp)
    for k, (x, z, ln, tx, tz) in enumerate(((-3.0, -2.0, 2.5, 18, -10), (-1.0, 0.5, 3.0, 4, 6), (1.5, -1.5, 2.5, -12, -8), (3.0, 1.5, 2.5, -20, 6), (-2.5, 2.5, 2.5, 14, 14), (0.5, 3.0, 2.0, -6, 18))):
        h.box("hair", (x - 0.75, 32.5, z - 0.75), (1.5, ln, 1.5), hp, rotation=(tz, 0, tx), pivot=(x, 32.5, z))
    for k, (x, ln) in enumerate(((-3.5, 2.0), (-1.5, 2.5), (0.5, 1.5), (2.0, 2.5), (3.5, 2.0))):
        h.box("hair", (x - 0.75, 31.0 - ln + 0.5, -4.75), (1.5, ln, 1.0), hp, rotation=(0, 0, 4 - 2 * k), pivot=(x, 31.5, -4.5))
    h.box("hair", (-4.25, 27.0, 3.9), (8.5, 5.5, 1.5), hp)
    h.box("hair", (-0.5, 24.6, -4.9), (1.0, 1.5, 1.0), skin(63, Z_SKIN))                       # nose
    humanoid_clips(h.A, "zenon")
    if form == "hybrid":
        zenon_hybrid_extras(h, P)
    return h, P


def ribs_overlay(face, i, j, fw, fh, r):
    """Exposed ribs and a breastbone across the vest front."""
    if face != "front":
        return None
    mid = fw / 2.0
    u = abs(i + 0.5 - mid) / mid
    if u < 0.12 and 2 < j < fh - 3:
        return px((226, 220, 198)), px((160, 190, 255), 70)
    if 0.12 <= u < 0.85 and 3 < j < fh - 4:
        jj = j + int(3 * u * u * 2)
        if jj % 5 == 1:
            return px(A3((222, 214, 190)) * (1 - 0.25 * u)), px((160, 190, 255), 60)
    return None


def wing_p(salt=0):
    """An insect wing remnant: bone-pale veins radiating from the root and a membrane of fine dither (every other texel), faintly glowing."""
    def p(face, i, j, fw, fh):
        if face not in ("front", "back"):
            return px((200, 192, 164)), None
        u, v = i / max(1, fw - 1), j / max(1, fh - 1)
        # the root is at the bottom edge (v = 1) for a wing standing up; veins fan out from it
        vein = abs(((u - 0.15) * fw * 0.5) - round(((u - 0.15) * fw * 0.5) * 1.0 / 2.0) * 2.0) < 0.55 and v < 0.97
        edge = i == 0 or i == fw - 1 or j == 0
        cross = j % 6 == 3 and hsh(i // 3, j // 6, salt) > 0.35
        if edge or vein or cross:
            return px((200, 192, 164)), px((160, 190, 255), 70)
        if (i + j) % 2 == 0 and hsh(i // 2, j // 2, salt) > 0.15:
            return px((150, 160, 186)), px((160, 190, 255), 30)
        return None, None
    return p


def zenon_hybrid_extras(h, P):
    """100% Beelzebub: a row of vertebra spikes and rib arcs grown through the back of the cloak, three spines over each shoulder, bone blades along each
    forearm, a bony crest and brow horns, and two pairs of insect wing remnants that shiver; the exposed ribs are painted on the vest."""
    tz = P["tz"]
    sx = P["sx"]
    bone = lambda k_, g=50: bone_p(k_, g)
    # the spine and ribs, on a bone of their own (child of the cloak) so they sway and can grow
    h.bone("ribs", "cloak_top", (0, 15, tz + 1.4))
    for k, y in enumerate((18.8, 16.3, 13.8, 11.3)):
        h.box("ribs", (-0.75, y, tz + 1.3), (1.5, 3.0, 1.5), bone(200 + k), rotation=(52, 0, 0), pivot=(0, y, tz + 1.3))
    for k, y in enumerate((17.8, 15.3, 12.8)):
        h.pair("ribs", "ribs", (0.9, y, tz + 1.5), (3.0, 1.0, 1.0), bone(210 + k, 30), rotation=(0, 0, -10), pivot=(0.9, y, tz + 1.5))
        h.pair("ribs", "ribs", (3.8, y - 0.5, tz + 1.5), (3.0, 1.0, 1.0), bone(214 + k, 30), rotation=(0, 0, -32), pivot=(3.8, y - 0.2, tz + 1.5))
        h.pair("ribs", "ribs", (6.3, y - 1.8, tz + 1.5), (2.5, 1.0, 1.0), bone(218 + k, 30), rotation=(0, 0, -58), pivot=(6.3, y - 1.5, tz + 1.5))
    # shoulder spines, on a bone of their own (child of the mantle)
    h.bone("spines", "fur", (0, 21, 0))
    for k, (x, ln, ang) in enumerate(((6.8, 6.0, 22), (8.4, 5.0, 40), (9.8, 4.0, 60))):
        h.pair("spines", "spines", (x, 21.5, -0.8 + 1.2 * k), (1.5, ln, 1.5), bone(230 + k), rotation=(0, 0, -ang), pivot=(x + 0.7, 21.5, -0.1 + 1.2 * k))
    # forearm blades
    for suf in ("_l", "_r"):
        h.bone("blades" + suf, "forearm" + suf, ((1 if suf == "_l" else -1) * sx, 14, 0))
    for k, (y, ln) in enumerate(((13.2, 4.0), (11.8, 3.5), (10.4, 3.0))):
        h.pair("blades_l", "blades_r", (sx + 1.4, y, 0.4), (1.0, 1.0, ln), bone(240 + k), rotation=(-8, 24, 0), pivot=(sx + 1.4, y, 0.4))
    # crest and brow horns
    h.bone("crest", "head", (0, 31, 0))
    for k, (z, hh) in enumerate(((-3.0, 3.5), (-1.0, 3.0), (1.0, 2.5), (3.0, 2.0))):
        h.box("crest", (-0.5, 30.6, z - 0.75), (1.0, hh, 1.5), bone(250 + k, 40), rotation=(10, 0, 0), pivot=(0, 31, z))
    h.pair("crest", "crest", (2.0, 29.2, -4.6), (1.0, 2.5, 1.0), bone(256, 40), rotation=(0, 0, -28), pivot=(2.5, 29.2, -4.1))
    h.pair("crest", "crest", (4.0, 25.6, -2.6), (1.0, 1.5, 1.5), bone(257, 40), rotation=(0, 0, -50), pivot=(4.3, 26.5, -2.0))
    # insect wing remnants: two pairs standing up from the shoulder blades
    wp = wing_p(260)
    pair_bone(h, "wing", "body", (3.0, 18.5, tz + 1.2), (12, 0, -36), parent_r=True)
    pair_bone(h, "wing2", "body", (2.4, 15.5, tz + 1.2), (6, 0, -62), parent_r=True)
    h.pair("wing_l", "wing_r", (3.0, 18.5, tz + 1.2), (4.0, 13.0, 0.5), wp)
    h.pair("wing2_l", "wing2_r", (2.4, 15.5, tz + 1.2), (3.0, 9.0, 0.5), wing_p(261))
    A = h.A
    flap_all(A, "wing_l", 7.0, 0.45, 1.0)
    flap_all(A, "wing_r", 7.0, 0.45, -1.0, phase=0.4)
    flap_all(A, "wing2_l", 9.0, 0.38, 1.0, phase=1.0)
    flap_all(A, "wing2_r", 9.0, 0.38, -1.0, phase=1.4)
    grow(A, "transform", ("ribs", "spines", "blades_l", "blades_r"), 0.8, 1.7)
    grow(A, "transform", ("crest",), 0.6, 1.4)
    grow(A, "transform", ("wing_l", "wing_r", "wing2_l", "wing2_r"), 1.1, 2.1)


# =========================================================================================================================================
#                                                              DARK DISCIPLE (one hooded humanoid, four devil tints)
# =========================================================================================================================================
DEVILS = {
    "lucifero": dict(robe=(28, 18, 46), robe_d=(10, 7, 20), robe_l=(58, 42, 92), trim=(112, 72, 190), trim_d=(58, 34, 104), trim_l=(176, 128, 255), glow=(190, 120, 255)),
    "megicula": dict(robe=(44, 14, 20), robe_d=(16, 6, 9), robe_l=(84, 28, 38), trim=(176, 28, 52), trim_d=(100, 14, 30), trim_l=(255, 90, 110), glow=(255, 60, 84)),
    "beelzebub": dict(robe=(22, 25, 48), robe_d=(8, 9, 22), robe_l=(46, 52, 96), trim=(70, 96, 214), trim_d=(36, 52, 128), trim_l=(143, 176, 255), glow=(143, 176, 255)),
    "astaroth": dict(robe=(16, 26, 42), robe_d=(6, 11, 20), robe_l=(36, 56, 88), trim=(204, 164, 62), trim_d=(120, 90, 30), trim_l=(255, 224, 130), glow=(255, 214, 120)),
}
BASE_TINT = dict(robe=(30, 28, 38), robe_d=(11, 10, 15), robe_l=(60, 58, 76), trim=(150, 150, 164), trim_d=(84, 84, 96), trim_l=(222, 222, 236), glow=(224, 224, 240))
TINT = {}
TINT_NAME = [""]


def set_tint(name, d):
    TINT.clear()
    TINT.update(d)
    TINT_NAME[0] = name


def dyn(factory):
    """A painter that rebuilds itself when the palette (TINT) changes, so one model can be painted in several devil tints."""
    st = {"k": None, "p": None}

    def p(face, i, j, fw, fh):
        if st["k"] != TINT_NAME[0]:
            st["k"] = TINT_NAME[0]
            st["p"] = factory()
        return st["p"](face, i, j, fw, fh)
    return p


DISCIPLE_FACE = [
    "################",
    "################",
    "################",
    "################",
    "##MmmmmssmmmmM##",
    "##MmmmmssmmmmM##",
    "##MmdddmmdddmM##",
    "##MmeeemmeeemmM#",
    "##MmeeemmeeemmM#",
    "##MmdddmmdddmM##",
    "##MmmmmnnmmmmM##",
    "##MmmmmnnmmmmM##",
    "##MmmmmmmmmmmM##",
    "##Mmtutututu mM#",
    "##Mmutututut mM#",
    "###MMmmmmmmMM###",
]


def disciple_face_painter():
    rows = [r.replace(" ", "m")[:16].ljust(16, "m") for r in DISCIPLE_FACE]
    mask = A3((222, 214, 192))
    mask_d = A3((178, 168, 142))
    hood = A3((9, 8, 13))
    hood_p = cloth((13, 12, 18), (6, 5, 9), (30, 28, 40), 311, folds=3, seam=False, weave=0.8)

    def p(face, i, j, fw, fh):
        if face != "front":
            return hood_p(face, i, j, fw, fh)
        ch = rows[j][i]
        gl = TINT["glow"]
        if ch == "#":
            return px(hood + fz(i, j, 5) * 4), None
        if ch == "m":
            return px(mask + fz(i, j, 6) * 8 - (10 if j > 11 else 0)), None
        if ch == "M":
            return px(mask_d + fz(i, j, 7) * 6), None
        if ch == "e":
            return px(A3(gl) * 0.9 + 40), px(gl, 255)
        if ch == "d":
            return px((14, 10, 16)), None
        if ch == "n":
            return px((26, 20, 22)), None
        if ch == "s":
            return px(A3(TINT["trim_l"]) * 0.9), px(gl, 230)
        if ch == "t":
            return px((236, 230, 212)), None
        if ch == "u":
            return px((60, 50, 50)), None
        return px(mask), None
    return p


def disciple_painters():
    robe = dyn(lambda: cloth(TINT["robe"], TINT["robe_d"], TINT["robe_l"], 301, folds=4, seam=False, weave=1.2))
    trim = dyn(lambda: cloth(TINT["trim"], TINT["trim_d"], TINT["trim_l"], 302, folds=2, seam=False, weave=1.0))
    leath = dyn(lambda: leather((34, 30, 36), (80, 74, 90), 303, stitch=None, wrinkles=1.0))
    glove = dyn(lambda: leather((30, 28, 34), (74, 70, 86), 304, stitch=None))

    def torso(face, i, j, fw, fh):
        if face == "front":
            mid = fw // 2
            if i in (mid - 1, mid) or (i in (mid - 2, mid + 1) and j < fh - 1):
                c, g = trim(face, i, j, fw, fh)
                return c, px(TINT["glow"], 38) if i in (mid - 1, mid) else None
            if j in (6, 7) and abs(i - mid + 0.5) < 3.5:
                return px(TINT["trim_l"]), px(TINT["glow"], 150)                  # a chest sigil bar
        return robe(face, i, j, fw, fh)

    def belt(face, i, j, fw, fh):
        if face == "front" and abs(i - fw // 2 + 0.5) < 1.6 and 0 < j < fh - 1:
            return px(TINT["trim_l"]), px(TINT["glow"], 230)                       # the clasp
        return trim(face, i, j, fw, fh)

    def sleeve_f(face, i, j, fw, fh):
        if j >= fh * 0.62:
            c, g = trim(face, i, j, fw, fh)
            return c, (px(TINT["glow"], 50) if j == fh - 1 else None)
        return robe(face, i, j, fw, fh)

    def skirt(face, i, j, fw, fh):
        if face in SIDES and j >= fh - 3:
            c, g = trim(face, i, j, fw, fh)
            return c, px(TINT["glow"], 40)
        if face == "front" and i in (fw // 2 - 1, fw // 2):
            return trim(face, i, j, fw, fh)
        return robe(face, i, j, fw, fh)
    return dict(torso=torso, belt=belt, skirt=skirt, thigh=robe, boot=leath, foot=leath, sleeve_u=robe, sleeve_f=sleeve_f, hand=glove, robe=robe, trim=trim)


def build_disciple():
    set_tint("base", BASE_TINT)
    P = dict(tw=8, td=4, aw=4, ad=4, lw=3.5, ld=4, lx=2.0, hw=9.0, skirt=(10, 6, 6.5, 4.0))
    h = Build("disciple", 512, 512)
    h.raise_by = UP
    rig_bones(h, P)
    h.bone("hood", "head", (0, 30, 0))
    h.bone("mantle", "body", (0, 20, 0))
    pt = disciple_painters()
    S_ = dict(pt)
    S_["head"] = disciple_face_painter()
    humanoid_common(h, P, S_)
    rb, tr = pt["robe"], pt["trim"]
    # the hood: top, back, sides, a brow that shades the mask, and a tip falling back
    h.box("hood", (-4.5, 30.5, -4.5), (9.0, 2.0, 9.0), rb)
    h.box("hood", (-4.5, 23.0, 4.0), (9.0, 8.5, 1.5), rb)
    h.pair("hood", "hood", (4.0, 23.5, -4.0), (1.0, 8.0, 8.5), rb)
    h.box("hood", (-4.5, 29.5, -5.0), (9.0, 2.0, 1.5), rb)
    h.box("hood", (-1.5, 32.0, 0.5), (3.0, 3.5, 3.0), rb, rotation=(30, 0, 0), pivot=(0, 32.5, 2.0))
    h.pair("hood", "hood", (3.6, 24.0, -5.0), (1.0, 5.5, 1.0), rb)
    # the mantle over the shoulders in the devil's colour, two spiked pauldrons
    h.box("mantle", (-8.0, 19.5, -4.0), (16.0, 3.0, 8.0), tr)
    h.pair("mantle", "mantle", (6.0, 22.0, -0.75), (1.5, 4.5, 1.5), dyn(lambda: horn_p(321, glow=TINT["glow"] + (200,), cols=(TINT["trim_d"], TINT["trim"], TINT["trim_l"]))), rotation=(0, 0, -24), pivot=(6.7, 22.0, 0))
    h.pair("mantle", "mantle", (7.6, 21.2, -0.75), (1.5, 3.5, 1.5), dyn(lambda: horn_p(322, glow=TINT["glow"] + (200,), cols=(TINT["trim_d"], TINT["trim"], TINT["trim_l"]))), rotation=(0, 0, -46), pivot=(8.3, 21.2, 0))
    # the robe's tails (back panel in two segments, so it sways), ragged and edged with the devil's colour
    ragged = dyn(lambda: fur(331, [TINT["robe_d"], TINT["robe"], TINT["robe_l"], TINT["trim_d"], TINT["trim"]], root=0.1, tip=0.6, saw=5, width=2.0))

    def hem(face, i, j, fw, fh):
        if face in SIDES and j >= fh - 8:
            c, g = ragged(face, i, j, fw, fh)
            return c, (px(TINT["glow"], 80) if (c is not None and j >= fh - 3) else None)
        return rb(face, i, j, fw, fh)
    cloak_parts(h, P, rb, hem)
    humanoid_clips(h.A, "disciple")
    alias_tracks(h.A, "hair", "hood", 0.45)
    alias_tracks(h.A, "fur", "mantle", 0.8)
    drop_empty(h)

    def variants(b, hh):
        for name, d in DEVILS.items():
            set_tint(name, d)
            b.pk.paint_all()
            img, glow = b.pk.images()
            img, glow = img.crop((0, 0, b.pk.W, hh)), glow.crop((0, 0, b.pk.W, hh))
            out = os.path.join(ROOT, "textures", "entity")
            img.save(os.path.join(out, "%s_%s_%s.png" % (KEY, b.name, name)))
            glow.save(os.path.join(out, "%s_%s_%s_glow.png" % (KEY, b.name, name)))
        set_tint("base", BASE_TINT)
    h.variants = variants
    return h, P


def alias_tracks(a, src, dst, k=1.0):
    """In every clip, the bone dst gets the rotation (scaled by k) and scale tracks of src: a hood that sways like hair, a mantle that moves like fur."""
    for c in a.clips.values():
        t = c["bones"].get(src)
        if not t:
            continue
        o = c["bones"].setdefault(dst, {})
        if "rotation" in t:
            o["rotation"] = {tm: [round(v * k, 3) for v in val] for tm, val in t["rotation"].items()}
        if "scale" in t:
            o["scale"] = {tm: [1 + (v - 1) * k for v in val] for tm, val in t["scale"].items()}


def drop_empty(h):
    """Removes bones that carry no cube and have no children (the shared rig's hair / crown / fur on a model that has none)."""
    changed = True
    while changed:
        changed = False
        parents = {b.get("parent") for b in h.m.bones.values()}
        for n in [n for n, b in h.m.bones.items() if not b["cubes"] and n not in parents and n != "root"]:
            del h.m.bones[n]
            h.m.order.remove(n)
            changed = True


# =========================================================================================================================================
#                                                              BLOOD BEAST (a lion-wolf of living blood, Vanica's summon)
# =========================================================================================================================================
BLOOD = [(40, 2, 8), (92, 5, 16), (146, 10, 26), (200, 24, 40), (240, 78, 90)]
VEIN_GLOW = (255, 40, 64)


def blood(salt, vein=1.0, sheen=1.0, dark=0.0, glow_a=235):
    """Living blood: a wet, mottled red with bright specular specks and a network of thin veins (the contour lines of a noise field) that glow."""
    def p(face, i, j, fw, fh):
        n = nz(i * 0.2, j * 0.2, salt) + 0.5
        t = 0.3 + 0.48 * n + 0.1 * (fz(i, j, salt + 3) + 0.5) - dark
        v = j / max(1, fh - 1)
        t -= 0.08 * v
        c = ramp(BLOOD, t) * FACE_K[face]
        if sheen and face in ("top", "front", "left", "right") and hsh(i, j, salt + 9) > 0.985:
            c = c * 0.5 + A3(BLOOD[4]) * 0.6
        w = min(abs(nz(i * 0.15 + 5.0, j * 0.15, salt + 21) - 0.01), abs(nz(i * 0.21 + 11.0, j * 0.19 + 3.0, salt + 33) - 0.01) * 1.3)
        th = 0.0105 * vein
        g = None
        if w < th:
            c = A3((110, 0, 12)) * (0.8 + 0.4 * (1 - w / th))
            g = px(VEIN_GLOW, glow_a * (1.0 - 0.5 * w / th))
        return px(bevel(c, face, i, j, fw, fh, 0.04)), g
    return p


def thorn_p(salt, glow_tip=True):
    """A blood thorn: dark crimson at the root, bright at the tip, the tip glowing."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.75 - 0.6 * v + 0.1 * fz(i, j, salt)
        c = ramp(BLOOD, t) * FACE_K[face]
        g = px(VEIN_GLOW, 220 * (1 - v) ** 1.5) if glow_tip else None
        return px(c), g
    return p


def beast_eye(face, i, j, fw, fh):
    return px((255, 232, 150)), px((255, 220, 110), 255)


def beast_tooth(face, i, j, fw, fh):
    v = j / max(1, fh - 1)
    return px(ramp([(168, 150, 130), (236, 226, 206), (252, 248, 236)], 0.3 + 0.7 * (1 - v))), None


def beast_claw(face, i, j, fw, fh):
    v = j / max(1, fh - 1)
    return px(ramp([(20, 2, 6), (70, 6, 14), (150, 20, 30)], 0.9 - v * 0.9)), None


def beast_mouth(face, i, j, fw, fh):
    c = A3((110, 8, 24)) + fz(i, j, 5) * 14
    return px(c * FACE_K[face]), px((255, 40, 64), 60)


def beast_head(salt):
    base = blood(salt, 1.2, 1.0)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face == "bottom":
            return beast_mouth(face, i, j, fw, fh)
        if face in ("left", "right") and j >= fh - 2:
            return px((60, 2, 10)), None                                        # the lip line
        return c, g
    return p


def beast_jaw(salt):
    base = blood(salt, 1.0, 1.0, dark=0.05)

    def p(face, i, j, fw, fh):
        if face == "top":
            return beast_mouth(face, i, j, fw, fh)
        return base(face, i, j, fw, fh)
    return p


def build_beast():
    h = Build("blood_beast", 512, 512)
    P = {}
    hip_y, L1, L2, HZ = 12.5, 6.0, 5.0, 6.0
    knee_y, hock_y = hip_y - L1, hip_y - L1 - L2
    hock_h = hip_y - L1 * COS(math.radians(30)) - L2 * COS(math.radians(25))
    h.bone("root", None, (0, 0, 0))
    h.bone("body", "root", (0, 14, 3))
    h.bone("chest", "body", (0, 14, -5))
    h.bone("mane", "chest", (0, 16, -11))
    h.bone("spine_f", "chest", (0, 18, -6))
    h.bone("spine_b", "body", (0, 17, 3))
    h.bone("neck", "chest", (0, 15, -11))
    h.bone("head", "neck", (0, 16, -17))
    h.bone("jaw", "head", (0, 13.5, -23))
    pair_bone(h, "ear", "head", (3, 20, -19), parent_r=True)
    pair_bone(h, "fa", "chest", (4.5, 13, -8), parent_r=True)
    pair_bone(h, "fb", "fa", (4.5, 7.5, -8))
    pair_bone(h, "fp", "fb", (4.5, 3, -8))
    pair_bone(h, "ha", "body", (4.5, hip_y, HZ), (30, 0, 0), parent_r=True)
    pair_bone(h, "hb", "ha", (4.5, knee_y, HZ), (-55, 0, 0))
    pair_bone(h, "hf", "hb", (4.5, hock_y, HZ), (25, 0, 0))
    tail = ((11.5, 10.0), (11.5, 14.0), (11.5, 18.0), (11.5, 22.0))
    prev = "body"
    for n, (y, z) in enumerate(tail):
        h.bone("tail_%d" % (n + 1), prev, (0, y, z), (14 + 6 * n, 0, 0))
        prev = "tail_%d" % (n + 1)
    drips = ((("jaw", (-1.0, 11.5, -28.0)), ("jaw", (1.2, 11.5, -26.0)), ("chest", (-2.5, 9.0, -6.0)), ("body", (2.0, 9.5, 2.0)), ("body", (-1.5, 9.5, 6.0)), ("chest", (3.0, 9.0, -9.0))))
    for n, (par, pos) in enumerate(drips):
        h.bone("drip_%d" % (n + 1), par, (pos[0], pos[1] + 1.5, pos[2]))

    bl = blood
    # torso
    h.box("chest", (-5.0, 9.0, -10.0), (10.0, 9.0, 9.0), bl(401, 1.0))
    h.box("body", (-4.5, 9.5, -1.0), (9.0, 8.5, 9.0), bl(402, 1.0))
    h.box("chest", (-4.0, 8.5, -9.0), (8.0, 2.0, 7.0), bl(403, 0.7, dark=0.1))                  # the deep chest, a darker keel
    # neck, head
    h.box("neck", (-3.0, 12.5, -16.5), (6.0, 6.0, 7.0), bl(404, 1.2), rotation=(12, 0, 0), pivot=(0, 15, -10))
    h.box("head", (-3.5, 13.0, -23.0), (7.0, 6.5, 6.5), beast_head(405))
    h.box("head", (-2.25, 13.0, -28.5), (4.5, 3.5, 5.5), beast_head(406))
    h.box("head", (-1.0, 15.5, -29.5), (2.0, 1.5, 1.0), solid((40, 2, 10), (255, 40, 64, 60)))
    h.box("jaw", (-2.0, 11.5, -29.0), (4.0, 1.5, 6.0), beast_jaw(407))
    h.pair("head", "head", (1.2, 11.8, -28.6), (0.5, 1.5, 0.5), beast_tooth)
    h.pair("jaw", "jaw", (1.2, 13.0, -28.6), (0.5, 1.5, 0.5), beast_tooth)
    h.pair("head", "head", (1.6, 12.3, -26.4), (0.5, 1.0, 0.5), beast_tooth)
    h.pair("jaw", "jaw", (1.6, 13.0, -26.4), (0.5, 1.0, 0.5), beast_tooth)
    h.pair("head", "head", (1.8, 12.5, -24.4), (0.5, 1.0, 0.5), beast_tooth)
    h.pair("head", "head", (1.9, 16.3, -23.4), (1.5, 1.0, 0.5), beast_eye)
    h.pair("head", "head", (1.7, 17.4, -23.4), (2.5, 0.5, 1.0), bl(408, 0.5, dark=0.2), rotation=(0, 0, -14), pivot=(3.0, 17.6, -23.0))        # the brow
    h.pair("ear_l", "ear_r", (2.0, 19.5, -20.0), (2.0, 2.5, 1.0), thorn_p(409), rotation=(0, 0, -12), pivot=(3.0, 19.5, -19.5))
    # the mane of blood thorns round the head and shoulders, and a ridge down the spine
    for k in range(12):
        a_ = 360.0 * k / 12
        r_ = 5.0
        x_, y_ = r_ * COS(math.radians(a_)), 16.0 + r_ * SIN(math.radians(a_))
        if y_ < 12.5 and abs(x_) < 3.5:
            continue
        ln = 4.0 + 1.0 * hsh(k, 3, 410)
        h.box("mane", (x_ - 0.75, y_, -12.5), (1.5, ln, 1.5), thorn_p(411 + k), rotation=(20, 0, a_ - 90), pivot=(x_, y_, -12.0))
    for k, z_ in enumerate((-8.5, -5.5, -2.5)):
        h.box("spine_f", (-0.75, 18.0, z_ - 0.75), (1.5, 3.0 - 0.5 * k, 1.5), thorn_p(430 + k), rotation=(24, 0, 0), pivot=(0, 18.0, z_))
    for k, z_ in enumerate((0.5, 3.5, 6.5)):
        h.box("spine_b", (-0.75, 17.5, z_ - 0.75), (1.5, 2.5 - 0.5 * k, 1.5), thorn_p(440 + k), rotation=(28, 0, 0), pivot=(0, 17.5, z_))
    # legs (the front ones straight, the hind ones a zigzag held by their rest rotations)
    h.pair("fa_l", "fa_r", (2.5, 7.5, -9.75), (3.5, 6.0, 3.5), bl(450, 1.0))
    h.pair("fb_l", "fb_r", (3.0, 3.0, -9.5), (3.0, 5.0, 3.0), bl(451, 1.0))
    h.pair("fp_l", "fp_r", (2.5, 0.0, -11.5), (4.0, 3.0, 5.0), bl(452, 1.0))
    for k in range(3):
        h.pair("fp_l", "fp_r", (2.7 + 1.2 * k, 0.0, -12.5), (0.5, 1.0, 1.0), beast_claw)
    h.pair("ha_l", "ha_r", (2.2, knee_y - 0.5, HZ - 2.5), (4.5, L1 + 1.0, 5.0), bl(453, 1.0))
    h.pair("hb_l", "hb_r", (2.75, hock_y - 0.5, HZ - 1.75), (3.5, L2 + 1.0, 3.5), bl(454, 1.0))
    h.pair("hf_l", "hf_r", (2.5, hock_y - hock_h, HZ - 1.5), (3.5, 3.0, 3.0), bl(455, 1.0))
    h.pair("hf_l", "hf_r", (2.5, hock_y - hock_h, HZ - 4.5), (4.0, 1.5, 3.0), bl(456, 1.0))
    for k in range(3):
        h.pair("hf_l", "hf_r", (2.7 + 1.2 * k, hock_y - hock_h, HZ - 5.5), (0.5, 1.0, 1.0), beast_claw)
    # tail: thick, tapering, ending in a tuft of thorns
    for n, (w, hh, d) in enumerate(((4.0, 4.0, 4.5), (3.5, 3.5, 4.5), (3.0, 3.0, 4.5), (2.5, 2.5, 4.0))):
        h.box("tail_%d" % (n + 1), (-w / 2, tail[n][0] - hh / 2, tail[n][1]), (w, hh, d), bl(460 + n, 1.0))
    h.box("tail_4", (-0.75, 11.0, 26.0), (1.5, 2.0, 2.5), thorn_p(470), rotation=(0, 0, 0))
    h.box("tail_4", (-0.75, 12.0, 25.0), (1.5, 3.0, 1.5), thorn_p(471), rotation=(-20, 0, 0), pivot=(0, 12.0, 25.5))
    # dripping blood: small drops hanging from the jaw, the belly and the paws (animated by the drip bones)
    for n, (par, pos) in enumerate(drips):
        h.box("drip_%d" % (n + 1), (pos[0] - 0.5, pos[1] - 0.5, pos[2] - 0.5), (1.0, 2.0, 1.0), blood(480 + n, 0.0, 0.0, glow_a=0))
    beast_clips(h.A, tail)
    return h, P


def beast_gait(a, clip, L, ph_f, ph_h, side, up_f=24.0, up_h=22.0, flex_f=40.0, flex_h=32.0, n=1):
    fn_up = lambda amp, ph: (lambda th: (amp * COS(th + ph), 0, 0))
    lift = lambda ph: (lambda th: max(0.0, -SIN(th + ph)) ** 0.85)
    cyc(a, clip, "fa_" + side, "rot", L, fn_up(up_f, ph_f), n)
    cyc(a, clip, "fb_" + side, "rot", L, lambda th: (-flex_f * lift(ph_f)(th), 0, 0), n)
    cyc(a, clip, "fp_" + side, "rot", L, lambda th: (16 * lift(ph_f)(th) - 5 * SIN(th + ph_f), 0, 0), n)
    cyc(a, clip, "ha_" + side, "rot", L, fn_up(up_h, ph_h), n)
    cyc(a, clip, "hb_" + side, "rot", L, lambda th: (-flex_h * lift(ph_h + 0.4)(th), 0, 0), n)
    cyc(a, clip, "hf_" + side, "rot", L, lambda th: (20 * lift(ph_h + 0.4)(th) - 8 * SIN(th + ph_h), 0, 0), n)


def beast_tail(a, clip, L, amps, base=(0, 0, 0, 0), n=1, lag=0.6):
    for k in range(4):
        cyc(a, clip, "tail_%d" % (k + 1), "rot", L, lambda th, k=k: (base[k] + 1.5 * SIN(th * 2 + k), amps[k] * SIN(th - lag * k), 0), n)


def beast_clips(a, tail):
    kf = lambda *pts: {t: v for t, v in pts}
    # ---------------------------------------------------------------- idle (loop 3.2 s)
    L = 3.2
    a.clip("idle", L, True)
    cyc(a, "idle", "chest", "scale", L, lambda t: (1, 1 + 0.02 * SIN(t), 1 + 0.028 * SIN(t)), 1)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.2 * SIN(t), 0))
    cyc(a, "idle", "neck", "rot", L, lambda t: (1.5 * SIN(2 * t + 0.5), 3.0 * SIN(t + 0.6), 0))
    cyc(a, "idle", "head", "rot", L, lambda t: (2.0 * SIN(2 * t + 1.0), 6.0 * SIN(t + 1.4), 1.5 * SIN(t)))
    cyc(a, "idle", "jaw", "rot", L, lambda t: (-3.0 - 2.4 * SIN(t), 0, 0), 2)
    cyc(a, "idle", "mane", "scale", L, lambda t: (1 + 0.03 * SIN(2 * t), 1 + 0.06 * SIN(2 * t + 0.4), 1 + 0.03 * SIN(2 * t)), 1)
    cyc(a, "idle", "mane", "rot", L, lambda t: (1.5 * SIN(t + 0.4), 0, 1.2 * SIN(t)))
    a.rot("idle", "ear_l", kf((0.0, (0, 0, 0)), (1.4, (0, 0, 0)), (1.5, (8, 0, -16)), (1.62, (0, 0, 0)), (1.74, (6, 0, -10)), (1.86, (0, 0, 0)), (3.2, (0, 0, 0))))
    a.rot("idle", "ear_r", kf((0.0, (0, 0, 0)), (2.4, (0, 0, 0)), (2.5, (8, 0, 16)), (2.62, (0, 0, 0)), (2.74, (6, 0, 10)), (2.86, (0, 0, 0)), (3.2, (0, 0, 0))))
    beast_tail(a, "idle", L, (7, 9, 11, 13))
    # ---------------------------------------------------------------- walk (loop 1.0 s)
    L = 1.0
    a.clip("walk", L, True)
    beast_gait(a, "walk", L, 0.0, 0.5, "l")
    beast_gait(a, "walk", L, math.pi, math.pi + 0.5, "r")
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.5 * COS(2 * t + 0.5), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (0.8 * SIN(2 * t), 2.0 * SIN(t), 0))
    cyc(a, "walk", "chest", "rot", L, lambda t: (-1.2 * SIN(2 * t + 0.4), -2.4 * SIN(t), 0))
    cyc(a, "walk", "neck", "rot", L, lambda t: (2.5 * SIN(2 * t + 1.6), 1.5 * SIN(t), 0))
    cyc(a, "walk", "head", "rot", L, lambda t: (-4.0 * SIN(2 * t + 2.0), 2.0 * SIN(t + 1.0), 0))
    cyc(a, "walk", "jaw", "rot", L, lambda t: (-4.0 - 1.5 * SIN(2 * t), 0, 0))
    cyc(a, "walk", "mane", "rot", L, lambda t: (3.0 * SIN(2 * t + 0.8), 0, 0))
    beast_tail(a, "walk", L, (8, 11, 14, 17), lag=0.7)
    # ---------------------------------------------------------------- run (loop 0.55 s)
    L = 0.55
    a.clip("run", L, True)
    for sd, d in (("l", 0.0), ("r", 0.45)):
        beast_gait(a, "run", L, d, d + 2.7, sd, 42.0, 38.0, 56.0, 46.0)
    cyc(a, "run", "body", "pos", L, lambda t: (0, 1.6 * COS(t + 0.3), 0))
    cyc(a, "run", "body", "rot", L, lambda t: (7.0 * SIN(t + 0.3), 0, 0))
    cyc(a, "run", "chest", "rot", L, lambda t: (-9.0 * SIN(t + 0.3), 0, 0))
    cyc(a, "run", "neck", "rot", L, lambda t: (-12 + 3.0 * SIN(t + 1.0), 0, 0))
    cyc(a, "run", "head", "rot", L, lambda t: (4 + 3.0 * SIN(t + 1.6), 0, 0))
    cyc(a, "run", "jaw", "rot", L, lambda t: (-18 - 6.0 * SIN(t + 0.4), 0, 0))
    cyc(a, "run", "mane", "rot", L, lambda t: (-14 + 4.0 * SIN(t + 1.0), 0, 0))
    for k, (b, am) in enumerate(((-22, 4), (-14, 6), (-10, 8), (-8, 10))):
        cyc(a, "run", "tail_%d" % (k + 1), "rot", L, lambda t, b=b, am=am, k=k: (b + 3.0 * SIN(t - 0.5 * k), am * SIN(t - 0.8 * k), 0))
    # ---------------------------------------------------------------- attack (one-shot 0.9 s): crouch, leap with the jaws wide, snap, recover
    L = 0.9
    a.clip("attack", L, False)
    T = (0.0, 0.18, 0.34, 0.5, 0.66, 0.9)
    keys(a, "attack", "body", "pos", list(zip(T, ((0, 0, 0), (0, -2.2, 2.0), (0, 1.2, -6.5), (0, 0.4, -8.5), (0, -0.6, -3.0), (0, 0, 0)))))
    keys(a, "attack", "body", "rot", list(zip(T, ((0, 0, 0), (-4, 0, 0), (-7, 0, 0), (-4, 0, 0), (-1, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "chest", "rot", list(zip(T, ((0, 0, 0), (-9, 0, 0), (10, 0, 0), (5, 0, 0), (-2, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "neck", "rot", list(zip(T, ((0, 0, 0), (-10, 0, 0), (14, 0, 0), (-6, 0, 0), (0, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "head", "rot", list(zip(T, ((0, 0, 0), (-6, 0, 0), (18, 0, 0), (10, 0, 0), (-8, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "jaw", "rot", list(zip(T, ((0, 0, 0), (-10, 0, 0), (-52, 0, 0), (-56, 0, 0), (-2, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "mane", "rot", list(zip(T, ((0, 0, 0), (-18, 0, 0), (-22, 0, 0), (-22, 0, 0), (-4, 0, 0), (0, 0, 0)))))
    keys(a, "attack", "mane", "scale", list(zip(T, ((1, 1, 1), (1, 1.3, 1), (1, 1.45, 1), (1, 1.45, 1), (1, 1.1, 1), (1, 1, 1)))))
    for sd, ph in (("l", 0.0), ("r", 0.05)):
        keys(a, "attack", "fa_" + sd, "rot", list(zip(T, ((0, 0, 0), (-14, 0, 0), (52 + 40 * ph, 0, 0), (62, 0, 0), (14, 0, 0), (0, 0, 0)))))
        keys(a, "attack", "fb_" + sd, "rot", list(zip(T, ((0, 0, 0), (20, 0, 0), (-14, 0, 0), (-22, 0, 0), (-6, 0, 0), (0, 0, 0)))))
        keys(a, "attack", "fp_" + sd, "rot", list(zip(T, ((0, 0, 0), (0, 0, 0), (-18, 0, 0), (-26, 0, 0), (-6, 0, 0), (0, 0, 0)))))
        keys(a, "attack", "ha_" + sd, "rot", list(zip(T, ((0, 0, 0), (22, 0, 0), (-34, 0, 0), (-26, 0, 0), (-6, 0, 0), (0, 0, 0)))))
        keys(a, "attack", "hb_" + sd, "rot", list(zip(T, ((0, 0, 0), (-22, 0, 0), (18, 0, 0), (14, 0, 0), (4, 0, 0), (0, 0, 0)))))
        keys(a, "attack", "hf_" + sd, "rot", list(zip(T, ((0, 0, 0), (12, 0, 0), (-26, 0, 0), (-18, 0, 0), (-4, 0, 0), (0, 0, 0)))))
    for k, (a0, a1) in enumerate(((-24, 8), (-16, 12), (-10, 14), (-8, 16))):
        keys(a, "attack", "tail_%d" % (k + 1), "rot", list(zip(T, ((0, 0, 0), (a0, 0, 0), (a0 - 6, a1, 0), (a0 - 4, -a1, 0), (a0 * 0.3, 0, 0), (0, 0, 0)))))
    # ---------------------------------------------------------------- hurt (one-shot 0.4 s)
    a.clip("hurt", 0.4, False)
    a.pos("hurt", "body", {0.0: (0, 0, 0), 0.08: (0, 0.4, 3.0), 0.4: (0, 0, 0)})
    a.rot("hurt", "chest", {0.0: (0, 0, 0), 0.08: (7, 0, 3), 0.4: (0, 0, 0)})
    a.rot("hurt", "neck", {0.0: (0, 0, 0), 0.08: (12, 0, 0), 0.4: (0, 0, 0)})
    a.rot("hurt", "head", {0.0: (0, 0, 0), 0.08: (14, 0, 0), 0.4: (0, 0, 0)})
    a.rot("hurt", "jaw", {0.0: (0, 0, 0), 0.08: (-34, 0, 0), 0.24: (-12, 0, 0), 0.4: (0, 0, 0)})
    a.scale("hurt", "chest", {0.0: (1, 1, 1), 0.08: (1.12, 0.9, 1.12), 0.4: (1, 1, 1)})
    for sd, sg in (("l", 1), ("r", -1)):
        a.rot("hurt", "fa_" + sd, {0.0: (0, 0, 0), 0.08: (16, 0, 0), 0.4: (0, 0, 0)})
    a.rot("hurt", "tail_1", {0.0: (0, 0, 0), 0.08: (-26, 0, 0), 0.4: (0, 0, 0)})
    # ---------------------------------------------------------------- death (one-shot 1.6 s, holds): the legs fold and it collapses into a puddle of blood
    L = 1.6
    a.clip("death", L, False)
    T = (0.0, 0.3, 0.9, 1.6)
    keys(a, "death", "root", "scale", list(zip(T, ((1, 1, 1), (1.04, 0.92, 1.04), (1.18, 0.4, 1.18), (1.3, 0.07, 1.3)))))
    keys(a, "death", "body", "pos", list(zip(T, ((0, 0, 0), (0, -1.5, 1.0), (0, -5.0, 1.5), (0, -6.0, 1.5)))))
    keys(a, "death", "head", "rot", list(zip(T, ((0, 0, 0), (10, 0, 0), (22, 0, 0), (26, 0, 0)))))
    keys(a, "death", "jaw", "rot", list(zip(T, ((0, 0, 0), (-30, 0, 0), (-40, 0, 0), (-12, 0, 0)))))
    for sd, sg in (("l", 1), ("r", -1)):
        keys(a, "death", "fa_" + sd, "rot", list(zip(T, ((0, 0, 0), (-10, 0, 0), (50, 0, sg * 10), (60, 0, sg * 14)))))
        keys(a, "death", "ha_" + sd, "rot", list(zip(T, ((0, 0, 0), (10, 0, 0), (50, 0, sg * 10), (60, 0, sg * 14)))))
    keys(a, "death", "mane", "scale", list(zip(T, ((1, 1, 1), (1, 1.2, 1), (1, 0.6, 1), (1, 0.1, 1)))))
    # ---------------------------------------------------------------- spawn (one-shot 1.4 s): it rises out of a puddle of blood
    L = 1.4
    a.clip("spawn", L, False)
    T = (0.0, 0.5, 0.95, 1.4)
    keys(a, "spawn", "root", "scale", list(zip(T, ((1.3, 0.07, 1.3), (1.12, 0.5, 1.12), (0.96, 1.08, 0.96), (1, 1, 1)))))
    keys(a, "spawn", "head", "rot", list(zip(T, ((26, 0, 0), (12, 0, 0), (-6, 0, 0), (0, 0, 0)))))
    keys(a, "spawn", "jaw", "rot", list(zip(T, ((-12, 0, 0), (-30, 0, 0), (-24, 0, 0), (0, 0, 0)))))
    keys(a, "spawn", "mane", "scale", list(zip(T, ((1, 0.1, 1), (1, 0.7, 1), (1, 1.3, 1), (1, 1, 1)))))
    # ---------------------------------------------------------------- drips: every clip, a drop grows, falls and fades, out of step with each other
    for clip, c in a.clips.items():
        L = c["animation_length"]
        n = max(1, round(L / 1.0))
        for k in range(6):
            fr_s, fr_p = {}, {}
            ph = k / 6.0
            for q in range(13):
                u = (q / 12.0 * n + ph) % 1.0
                grow_ = min(1.0, u / 0.45)
                fall = max(0.0, (u - 0.5) / 0.5)
                fr_s[L * q / 12] = (1, round(0.25 + 0.75 * grow_ if u < 0.5 else 1.0 - 0.9 * fall, 3), 1)
                fr_p[L * q / 12] = (0, round(-5.0 * fall * fall, 3), 0)
            a.scale(clip, "drip_%d" % (k + 1), fr_s)
            a.pos(clip, "drip_%d" % (k + 1), fr_p)


def beast_scenes():
    sc = {}
    t = (0, 0.7, 0)
    sc["triad_blood_beast_views"] = scene("blood_beast", "idle", (0,), (460, 380), [view("front", 0, 5, t, 5.2), view("front 3/4", 35, 12, t, 5.2),
                                                                                  view("side", 90, 4, t, 5.2), view("back", 180, 6, t, 5.2)])
    for clip, ages in (("walk", (0, 5, 10, 15)), ("run", (0, 3, 6, 9)), ("attack", (0, 4, 7, 10, 14, 18)), ("death", (0, 6, 18, 32)), ("spawn", (0, 10, 19, 28)),
                       ("hurt", (0, 2, 4, 8)), ("idle", (0, 20, 40, 60))):
        sc["triad_blood_beast_%s" % clip] = scene("blood_beast", clip, ages, (360, 300), [view("front 3/4", 35, 10, t, 5.6), view("side", 90, 4, t, 5.6)])
    sc["triad_blood_beast_head"] = scene("blood_beast", "idle", (0,), (420, 380), [view("face", 0, 4, (0, 1.0, 1.55), 2.6), view("face 3/4", 40, 8, (0, 1.0, 1.55), 2.6)])
    return sc


# =========================================================================================================================================
#                                                              preview scenes + main
# =========================================================================================================================================
def view(name, yaw, pitch, target, dist, fov=None):
    v = {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}
    if fov:
        v["fov"] = fov
    return v


def scene(model, anim=None, ages=(0,), tile=(360, 520), views=None, figure=False, expect=()):
    s = {"kind": "geo", "model": KEY + "_" + model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages),
         "tile": list(tile), "floor": False, "figure": figure, "expect": list(expect)}
    if anim:
        s["anim"] = anim
    s["views"] = views
    return s


def humanoid_scenes(name, h=1.1):
    t = (0, h, 0)
    sc = {}
    sc["triad_%s_views" % name] = scene(name, "idle", (0,), (440, 620), [view("front", 0, 3, t, 4.3), view("front 3/4", 35, 8, t, 4.3)])
    sc["triad_%s_views2" % name] = scene(name, "idle", (0,), (440, 620), [view("back", 180, 6, t, 4.3), view("side", 90, 4, t, 4.3)])
    sc["triad_%s_head" % name] = scene(name, "idle", (0,), (420, 420), [view("face", 0, 2, (0, 1.72, 0), 1.7), view("face 3/4", 30, 6, (0, 1.72, 0), 1.7),
                                                                         view("head side", 90, 4, (0, 1.72, 0), 1.9)])
    for clip, ages in (("attack", (0, 6, 9, 12, 18)), ("cast", (0, 10, 20, 30)), ("cast_big", (0, 14, 28, 40, 54)), ("transform", (0, 20, 40, 55)),
                       ("walk", (0, 5, 10, 15)), ("run", (0, 3, 6, 9)), ("death", (0, 12, 26, 40, 50)), ("hurt", (0, 3, 6)), ("idle", (0, 20, 40, 60))):
        sc["triad_%s_%s" % (name, clip)] = scene(name, clip, ages, (300, 440), [view("front 3/4", 35, 6, t, 5.0), view("side", 90, 4, t, 5.0)])
    return sc


def disciple_skin_scenes():
    """One scene per devil tint of the Dark Disciple (the full-path form of a geo scene names the texture)."""
    out = {}
    for dev in DEVILS:
        s = scene("disciple", "idle", (0, 40), (330, 480), [view("front 3/4", 30, 6, (0, 0.95, 0), 4.2)])
        s["model"] = "nusmp:geo/entity/%s_disciple.geo.json" % KEY
        s["texture"] = "nusmp:textures/entity/%s_disciple_%s.png" % (KEY, dev)
        s["animations"] = "nusmp:animations/entity/%s_disciple.animation.json" % KEY
        out["triad_disciple_%s" % dev] = s
    return out


def write_scenes(names):
    sc = {}
    for n in names:
        if n.startswith("disciple"):
            sc.update(humanoid_scenes(n, 0.95))
            sc.update(disciple_skin_scenes())
        elif n == "blood_beast":
            sc.update(beast_scenes())
        else:
            sc.update(humanoid_scenes(n))
    os.makedirs(os.path.dirname(SCENES_OUT), exist_ok=True)
    with open(SCENES_OUT, "w") as f:
        json.dump(sc, f, indent=1)


def _rot(p, r):
    """The file's rotation (degrees): about x first, then y, then z (GeoDraw's ZYX order), applied to the point p."""
    rx, ry, rz = (math.radians(v) for v in r)
    x, y, z = p
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x, y, z)


def model_bounds(m, only=None):
    """The axis-aligned bounds (pixels, file frame) of every cube of the model in its rest pose (bone and cube rotations applied).
    only: an optional set of bone names to include. Returns (min, max) as 3-lists."""
    lo, hi = [1e9] * 3, [-1e9] * 3
    for name, b in m.bones.items():
        if only is not None and name not in only:
            continue
        chain = []
        n = name
        while n:
            chain.append(m.bones[n])
            n = m.bones[n].get("parent")
        for c in b["cubes"]:
            o, sz, g = c["origin"], c["size"], c.get("inflate", 0.0)
            for dx in (-g, sz[0] + g):
                for dy in (-g, sz[1] + g):
                    for dz in (-g, sz[2] + g):
                        pnt = (o[0] + dx, o[1] + dy, o[2] + dz)
                        if c.get("rotation"):
                            pv = c["pivot"]
                            q = _rot((pnt[0] - pv[0], pnt[1] - pv[1], pnt[2] - pv[2]), c["rotation"])
                            pnt = (q[0] + pv[0], q[1] + pv[1], q[2] + pv[2])
                        for bb in chain:
                            if bb.get("rotation"):
                                pv = bb["pivot"]
                                q = _rot((pnt[0] - pv[0], pnt[1] - pv[1], pnt[2] - pv[2]), bb["rotation"])
                                pnt = (q[0] + pv[0], q[1] + pv[1], q[2] + pv[2])
                        for k in range(3):
                            lo[k] = min(lo[k], pnt[k])
                            hi[k] = max(hi[k], pnt[k])
    return lo, hi


def glow_bones(m, glow_img):
    """The bones whose cubes carry any glow pixel (alpha > 20 in the glow texture): what a renderer may want to pulse."""
    a = np.asarray(glow_img.convert("RGBA"))[..., 3]
    out = []
    for name, b in m.bones.items():
        hit = False
        for c in b["cubes"]:
            uv = c["uv"]
            if not isinstance(uv, dict):
                continue
            for f in uv.values():
                u, v = f["uv"]
                w, hh = f["uv_size"]
                if v < a.shape[0] and a[int(v):int(v + hh), int(u):int(u + w)].max(initial=0) > 20:
                    hit = True
                    break
            if hit:
                break
        if hit:
            out.append(name)
    return out


def prune_anim(a, m):
    """Drops the tracks of bones the model does not have (a clip written for the shared rig also runs on a smaller one)."""
    for c in a.clips.values():
        for b in [b for b in c["bones"] if b not in m.bones]:
            del c["bones"][b]


CLIPS_HUMANOID = ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death")
CLIPS_BEAST = ("idle", "walk", "run", "attack", "hurt", "death", "spawn")
ONE_SHOT = {"attack", "cast_big", "hurt", "transform", "death", "spawn"}

EXTRAS = {
    "dante_hybrid": dict(glow_colour="violet (190,110,255), matches DevilStyle.GRAVITY", extra_bones=["horns", "halo", "halo_tilt", "halo_in", "orbs"], spinning=["halo", "halo_in", "orbs"], bobbing=["orbs"],
                         grows_in_transform=["horns", "halo", "orbs"], note="halo and orbs are glow-only (the main texture is transparent there)"),
    "vanica_hybrid": dict(glow_colour="blood red (255,40,70), matches DevilStyle.CURSE", extra_bones=["horns", "claws_l", "claws_r", "drops_a", "drops_b", "drops_c"], spinning=["drops_a", "drops_b", "drops_c"],
                          bobbing=["drops_a", "drops_b", "drops_c"], grows_in_transform=["horns", "claws_l", "claws_r", "drops_a", "drops_b", "drops_c"],
                          note="the blood droplets orbit the torso at radius 11-15 px"),
    "zenon_hybrid": dict(glow_colour="icy indigo-white (150,180,255), matches DevilStyle.SPATIAL", extra_bones=["ribs", "spines", "blades_l", "blades_r", "crest", "wing_l", "wing_r", "wing2_l", "wing2_r"],
                         flapping=["wing_l", "wing_r", "wing2_l", "wing2_r"], grows_in_transform=["ribs", "spines", "blades_l", "blades_r", "crest", "wing_l", "wing_r", "wing2_l", "wing2_r"],
                         note="the wings are a vein lattice with a dithered membrane (cutout friendly); on a TRANSLUCENT layer the membrane would need real alpha"),
}


def check_outputs(names):
    """Re-reads what was written: json loads, every uv rectangle lies inside its texture, every animation bone exists, the clip set is complete, the
    texture has pixels and the glow texture has glow. Prints one line per model; exits non-zero on a problem."""
    from PIL import Image
    bad = 0
    for n in names:
        mid = "%s_%s" % (KEY, n)
        g = json.load(open(os.path.join(ROOT, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
        an = json.load(open(os.path.join(ROOT, "animations", "entity", mid + ".animation.json")))["animations"]
        tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
        img = Image.open(os.path.join(ROOT, "textures", "entity", mid + ".png"))
        glow = Image.open(os.path.join(ROOT, "textures", "entity", mid + "_glow.png"))
        probs = []
        if img.size != (tw, th) or glow.size != (tw, th):
            probs.append("texture size %s / %s != %dx%d" % (img.size, glow.size, tw, th))
        names_ = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names_:
                probs.append("bone %s: missing parent %s" % (b["name"], b["parent"]))
            for c in b["cubes"]:
                for f in c["uv"].values():
                    u, v = f["uv"]
                    w, hh = f["uv_size"]
                    if u < 0 or v < 0 or u + w > tw or v + hh > th:
                        probs.append("bone %s: uv %s outside the texture" % (b["name"], f))
        want = CLIPS_BEAST if n == "blood_beast" else CLIPS_HUMANOID
        for cn in want:
            if cn not in an:
                probs.append("missing clip " + cn)
        for cn, cl in an.items():
            for bn in cl["bones"]:
                if bn not in names_:
                    probs.append("animation %s: unknown bone %s" % (cn, bn))
        if np.asarray(img.convert("RGBA"))[..., 3].max() == 0 or np.asarray(glow.convert("RGBA"))[..., 3].max() == 0:
            probs.append("empty texture or empty glow")
        print(mid, "cubes", sum(len(b["cubes"]) for b in g["bones"]), "bones", len(g["bones"]), "clips", len(an), "OK" if not probs else "PROBLEMS")
        for pr in probs[:10]:
            print("   ", pr)
            bad = 1
    return bad


def write_manifest(infos, path):
    manifest = {
        "package": "art-triad", "geo_key": KEY, "generator": "tools/gen_zogratis_triad.py (python3 -B tools/gen_zogratis_triad.py [names...]; idempotent)",
        "preview_scenes": "tools/render_preview/scenes.d/zogratis_triad.json (python3 -B tools/render_preview/preview.py triad_dante_base_views ...)",
        "frame": ("file frame: pixels (1/16 block), y up, origin at the feet, model front on -z, +x the model's left; GeoDraw.Space.PROP (front on +z at yaw 0). "
                  "rotation signs: +x swings a hanging limb forward and raises the chin, -x folds a knee / leans the body forward; +z swings a hanging limb to +x; +y turns the head to the model's right"),
        "texel_density": "humanoids 2 texels per model pixel (a face is 16x16 texels), every texture 512 px wide and as tall as its boxes need (power of two); beast the same density",
        "models": infos,
        "renderer_notes": [
            "GeoSpec.of(\"zogratis\", \"<name>\") names geo/animation/texture/glow; the glow texture is drawn additive and full bright with GeoDraw.paint(..., glowArgb): a pulse of 0.75..1.0 grey looks right, a flash of white on transform",
            "humanoid clips: idle (loop 3.2), walk and run (loops, lengths per model, drive them by the walk-animation position like DirewolfRenderer), cast (loop 1.6, arms raised), attack (one-shot, right arm slash), cast_big (one-shot 2.4, gather - rise - overhead - slam), hurt (one-shot 0.45), transform (one-shot 3.0), death (one-shot 2.8, holds the last frame)",
            "every one-shot clip except death starts and ends on the idle pose, so holding its last frame looks like idle; clips are sampled by seconds since the move began (GeoAnim holds the last keyframe of a non-looping clip)",
            "death kneels, topples forward and finishes lying face down by rotating the root bone about the feet (root rot x -90, pos y +4): do NOT also apply vanilla's death roll to these models",
            "cast_big and transform lift the whole model with the root bone (legs included) so the body never separates from the legs",
            "hybrid models have every bone of their base model (same clips, same names) plus extras; EXTRAS lists them. Their transform clip makes the extras grow from nothing, so the renderer can swap to the hybrid model at the start of the 100% transformation and play its transform clip (the base model's transform is the power-up before it)",
            "the Dark Disciple is one model; its tint is a texture variant: GeoSpec.of(\"zogratis\",\"disciple\").skin(\"zogratis_disciple_<devil>\") with <devil> in lucifero, megicula, beelzebub, astaroth (zogratis_disciple.png is the neutral grey tint)",
            "the Blood Beast clips: idle walk run attack hurt death (collapses into a puddle by scaling the root bone flat) spawn (rises from a puddle); the drip_* bones drip in every clip",
            "suggested renderer scale (the models are 2.03 blocks to the head top and 2.38 to the crown tips at scale 1): Dante 0.9, Zenon 0.87, Vanica 0.82 (she is the smallest of the three) so the crown tips stand about 2.0-2.15 blocks over the 0.7 x 2.0 hitbox; the Disciple (hood tip 2.34) 0.82; the Blood Beast 0.75 (3.26 blocks long at scale 1)",
            "vertex counts are per pass; the glow pass draws the model a second time",
        ],
    }
    produced = []
    for sub in ("geo/entity", "animations/entity", "textures/entity"):
        d = os.path.join(ROOT, sub)
        for fn in sorted(os.listdir(d)):
            if fn.startswith(KEY + "_"):
                produced.append(os.path.relpath(os.path.join(d, fn), REPO).replace(os.sep, "/"))
    produced += ["tools/gen_zogratis_triad.py", "tools/render_preview/scenes.d/zogratis_triad.json"]
    manifest["files_produced"] = produced
    with open(path, "w") as f:
        json.dump(manifest, f, indent=1)


BUILDERS = {}




def model_info(name, h, img, glow):
    m, A = h.m, h.A
    lo, hi = model_bounds(m)
    info = {
        "spec": 'GeoSpec.of("%s", "%s")' % (KEY, name),
        "files": {"geo": "src/main/resources/assets/nusmp/geo/entity/%s_%s.geo.json" % (KEY, name),
                  "animations": "src/main/resources/assets/nusmp/animations/entity/%s_%s.animation.json" % (KEY, name),
                  "texture": "src/main/resources/assets/nusmp/textures/entity/%s_%s.png" % (KEY, name),
                  "glow": "src/main/resources/assets/nusmp/textures/entity/%s_%s_glow.png" % (KEY, name)},
        "texture_size": [m.tex_w, m.tex_h], "cubes": m.cube_count(), "vertices_per_pass": m.cube_count() * 24,
        "bounds_px": {"min": [round(v, 1) for v in lo], "max": [round(v, 1) for v in hi]},
        "size_blocks": {"width": round((hi[0] - lo[0]) / 16, 2), "height": round((hi[1] - lo[1]) / 16, 2), "depth": round((hi[2] - lo[2]) / 16, 2)},
        "bones": [{"name": b["name"], "parent": b.get("parent"), "pivot": b["pivot"]} for b in (m.bones[n] for n in m.order)],
        "glow_bones": glow_bones(m, glow),
        "clips": {cn: {"length": c["animation_length"], "loop": c["loop"], "bones_animated": len(c["bones"])} for cn, c in A.clips.items()},
    }
    if name in ("dante_base", "vanica_base", "zenon_base", "dante_hybrid", "vanica_hybrid", "zenon_hybrid"):
        hd = model_bounds(m, {"head"})
        info["body_height_blocks"] = round(hd[1][1] / 16, 2)
        info["crown_tip_height_blocks"] = round(hi[1] / 16, 2)
        info["recommended"] = {"hitbox": [0.7, 2.0], "eye_height": 1.65, "renderer_scale": {"dante": 0.9, "vanica": 0.82, "zenon": 0.87}[name.split("_")[0]], "shadow_radius": 0.55}
        info["humanoid_bones"] = ["root", "body", "head", "hair", "crown", "fur", "cloak", "cloak_top", "cloak_bottom", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r", "shin_l", "shin_r"]
        info["design"] = {"dante": "broad and heavy, crown, swept-back black hair, cross scar marks, goatee, green eyes, brown tunic, red scarf, brown boots",
                          "vanica": "slim, crown, black bob, red eye + black eyepatch with a gold emblem on the viewer's left, open smile, belted maroon dress with a gold ring buckle, black sleeves and thigh-high boots",
                          "zenon": "lean, crown, short messy black hair, vertical scar with cross bars through the forehead and the viewer's right eye, pale eyes, maroon vest with crossed straps over a dark shirt"}[name.split("_")[0]]
    if name in EXTRAS:
        info["extras"] = EXTRAS[name]
    if name == "disciple":
        info["recommended"] = {"hitbox": [0.6, 1.95], "eye_height": 1.65, "renderer_scale": 0.82, "shadow_radius": 0.45}
        info["skins"] = {d: "zogratis_disciple_%s (+ _glow); glow %s" % (d, DEVILS[d]["glow"]) for d in DEVILS}
        info["extra_bones"] = ["hood (sways like hair)", "mantle (moves like fur)"]
    if name == "blood_beast":
        info["recommended"] = {"hitbox": [1.2, 1.0], "eye_height": 0.85, "renderer_scale": 0.75, "shadow_radius": 0.9}
        info["extra_bones"] = ["mane", "spine_f", "spine_b", "drip_1..drip_6"]
        info["legs"] = "fa/fb/fp (front, +_l/_r) ha/hb/hf (hind zigzag held by rest rotations), tail_1..tail_4"
    return info


def main():
    args = sys.argv[1:]
    manifest_path = None
    if "--manifest" in args:                                   # --manifest <file>: also write a manifest of every file, bone, clip and size
        k = args.index("--manifest")
        manifest_path = args[k + 1]
        del args[k:k + 2]
    which = [a for a in args if not a.startswith("-")]
    done = []
    infos = {}
    for name, build in BUILDERS.items():
        if which and name not in which:
            continue
        h, P = build()
        prune_anim(h.A, h.m)
        img, glow = h.finish(h.raise_by)
        h.save(img, glow)
        infos[name] = model_info(name, h, img, glow)
        done.append(name)
    if not which:
        write_scenes([n for n in BUILDERS])
        if manifest_path:
            write_manifest(infos, manifest_path)
    bad = check_outputs(done)
    if bad:
        sys.exit(1)


BUILDERS["dante_base"] = lambda: build_dante("base")
BUILDERS["dante_hybrid"] = lambda: build_dante("hybrid")
BUILDERS["vanica_base"] = lambda: build_vanica("base")
BUILDERS["vanica_hybrid"] = lambda: build_vanica("hybrid")
BUILDERS["zenon_base"] = lambda: build_zenon("base")
BUILDERS["zenon_hybrid"] = lambda: build_zenon("hybrid")
BUILDERS["disciple"] = build_disciple
BUILDERS["blood_beast"] = build_beast

if __name__ == "__main__":
    main()
