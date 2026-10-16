"""Why: the Avatar creation system (release 1.51.0) lets a player design a look on page 3 of the creation screen: skin tone, hair, outfit, horns,
wings, aura. Every choice must be a real model or texture in the game, so this script makes all of them, deterministically, from code (no
hand-painted files to lose). It writes (key "avatar"):

    assets/nusmp/geo/entity/avatar_hair_<0..15>.geo.json      + animation + texture   (GeoLite, drawn on the head by client/avatar/AvatarFeatureLayer)
    assets/nusmp/geo/entity/avatar_horn_<0..7>.geo.json       + texture + glow
    assets/nusmp/geo/entity/avatar_wing_<0..5>.geo.json       + animation + texture + glow
    assets/nusmp/geo/entity/avatar_extra_<n>.geo.json         capes / pauldrons / skirts of the outfits that have 3D parts
    assets/nusmp/geo/entity/avatar_aura_ring.geo.json         the aura ring under the avatar's feet (white, tinted by the aura colour at draw time)
    assets/nusmp/textures/entity/avatar/base_<human|bulky|small>.png   + _mask.png   body skin templates (grey flesh, tinted at runtime)
    assets/nusmp/textures/entity/avatar/outfit_<0..11>.png    64x64 overlays over the player skin layout
    assets/nusmp/textures/entity/avatar/eyes_glow.png         the iris pixels only (emissive layer; tinted by the eye colour, red in a frenzy)

    python3 -B tools/avatar/gen_avatar_client_assets.py             # write everything
    python3 -B tools/avatar/gen_avatar_client_assets.py --check     # read every geo/texture back (uv inside sheet, clips, parents)
    python3 -B tools/avatar/gen_avatar_client_assets.py --preview   # headless renders (tools/render_preview) of every part + contact sheets

Counts are mirrored by AppearanceData.HAIR_COUNT / OUTFIT_COUNT / HORN_COUNT / WING_COUNT (a unit test reads the files and compares).
Frame: head models are authored for a player's head (x -4..4, y 24..32, z -4..4, front -z; the layer moves them to the head's pose), wings and extras
for the body (y 12..24, back +z). Needs Pillow.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
from geo_builder import Model, Anim, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

KEY = "avatar"
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SCENES_OUT = os.path.join(HERE, "..", "render_preview", "scenes.d", "avatar.json")
SCRATCH = os.environ.get("AVATAR_SCRATCH", os.path.join(REPO, "build", "avatar_preview"))
TEX_DIR = os.path.join(ROOT, "textures", "entity", "avatar")

HAIR_COUNT, HORN_COUNT, WING_COUNT, OUTFIT_COUNT = 16, 8, 6, 12
EXTRA_OUTFITS = (1, 2, 3, 5, 8, 10, 11)
GROUPS = ("human", "bulky", "small")


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def hash2(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


# ============================================================================================ box atlas for the small GeoLite part models
class Part:
    """Collects boxes, packs their UV regions into one sheet, paints every region, and writes a Model. Boxes with the same size, colour and
    style share one region (the wings repeat the same feather many times)."""

    def __init__(self, name, tex_w=64, bones=None):
        self.name, self.tex_w = name, tex_w
        self.bones = bones or [("root", None, (0, 0, 0), None)]
        self.boxes = []
        self.anim = None

    def add(self, bone, origin, size, color, glow=None, rotation=None, pivot=None, inflate=0.0, style="strand", alpha=255, mirror=False):
        self.boxes.append(dict(bone=bone, origin=origin, size=tuple(int(s) for s in size), color=color, glow=glow, rotation=rotation,
                               pivot=pivot, inflate=inflate, style=style, alpha=alpha, mirror=mirror))

    def build(self):
        regions, order = {}, []
        for b in self.boxes:
            k = (b["size"], b["color"], b["glow"], b["style"], b["alpha"])
            if k not in regions:
                regions[k] = None
                order.append(k)
        x = y = row_h = 0
        for k in order:
            sx, sy, sz = k[0]
            w, h = 2 * (sx + sz), sy + sz
            if x + w > self.tex_w:
                x, y, row_h = 0, y + row_h, 0
            regions[k] = (x, y)
            x += w
            row_h = max(row_h, h)
        tex_h = max(16, ((y + row_h + 15) // 16) * 16)
        m = Model(KEY, self.name, self.tex_w, tex_h)
        for name, parent, pivot, rot in self.bones:
            m.bone(name, parent=parent, pivot=pivot, rotation=rot)
        skin, glow = m.paint()
        for b in self.boxes:
            k = (b["size"], b["color"], b["glow"], b["style"], b["alpha"])
            u, v = regions[k]
            m.cube(b["bone"], origin=b["origin"], size=b["size"], uv=(u, v), inflate=b["inflate"], rotation=b["rotation"], pivot=b["pivot"], mirror=b["mirror"])
        for k, (u, v) in regions.items():
            self._paint(skin, glow, u, v, k)
        self.model, self.skin, self.glow = m, skin, glow
        return m

    @staticmethod
    def _paint(skin, glow, u, v, k):
        (sx, sy, sz), color, gcol, style, alpha = k
        d, w, h = sz, sx, sy
        faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                 "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for fname, (fx, fy, fw, fh) in faces.items():
            base = {"top": 1.15, "bottom": 0.7}.get(fname, 0.94)
            for yy in range(fh):
                for xx in range(fw):
                    n = hash2(fx + xx, 3 if style == "strand" else fy + yy, len(color))
                    f = base
                    if style == "strand":
                        f *= 0.9 + 0.2 * n if (xx % 2 == 0) else 0.97
                        f *= 1.0 - 0.10 * (yy / max(1, fh - 1)) if fname not in ("top", "bottom") else 1.0
                    elif style == "plate":
                        edge = xx in (0, fw - 1) or yy in (0, fh - 1)
                        f *= 0.8 if edge else (1.06 - 0.1 * n)
                    elif style == "scale":
                        f *= 0.9 + 0.16 * (((xx + yy) % 3) / 2.0)
                    elif style == "bone":
                        f *= 0.92 + 0.12 * (yy / max(1, fh - 1)) * (-1 if fname == "front" else 1) + 0.04 * n
                    elif style == "membrane":
                        f *= 0.82 + 0.3 * n * (1.0 if (xx % 3) else 0.4)
                    px = shade(color, f)
                    skin.putpixel((fx + xx, fy + yy), (px[0], px[1], px[2], alpha))
                    if gcol is not None:
                        g = gcol[:3]
                        t = (1.0 - yy / max(1, fh - 1)) if fname in ("front", "back", "left", "right") else 1.0
                        glow.putpixel((fx + xx, fy + yy), (int(g[0] * t), int(g[1] * t), int(g[2] * t), 255))


def save_part(p, anim=None):
    m = p.build()
    m.save(anim)
    m.save_texture(p.skin, p.glow)   # always a glow map (black when nothing shines): the layer may ask for it of any part
    return m


def idle_sway(bones, length=2.4, amp=(4, 0, 0), phase_step=0.0):
    a = Anim()
    a.clip("idle", length=length, loop=True)
    for i, b in enumerate(bones):
        ph = (i * phase_step) % length
        t0 = ph
        a.rot("idle", b, {0.0: (0, 0, 0), length * 0.25: amp, length * 0.5: (0, 0, 0), length * 0.75: tuple(-v for v in amp), length: (0, 0, 0)})
    return a


# ============================================================================================ hair (16)
HAIR = [  # (name, base colour)
    ("short crop", (112, 78, 52)), ("spiky", (36, 34, 48)), ("long straight", (214, 220, 232)), ("ponytail", (232, 196, 96)),
    ("twin tails", (238, 138, 176)), ("bob", (38, 56, 120)), ("bun", (176, 44, 44)), ("mohawk", (70, 170, 96)),
    ("wild", (232, 120, 48)), ("side swept", (244, 244, 250)), ("braid", (136, 84, 52)), ("ahoge", (250, 226, 150)),
    ("drills", (238, 190, 60)), ("pompadour", (28, 32, 60)), ("layered long", (122, 76, 168)), ("hime", (20, 20, 28))]


def hair_cap(p, c, top=True, sides=3, back=7, fringe=2, bang_gap=False):
    if top:
        p.add("head", (-4, 32, -4), (8, 1, 8), c, inflate=0.3)
    if sides:
        for sx in (4, -5):
            p.add("head", (sx, 32 - sides, -4), (1, sides, 8), c, inflate=0.2)
    if back:
        p.add("head", (-4, 33 - back, 4), (8, back, 1), c, inflate=0.2)
    if fringe:
        if bang_gap:
            p.add("head", (-4, 32 - fringe, -5), (3, fringe + 0, 1), c, inflate=0.1)
            p.add("head", (1, 32 - fringe, -5), (3, fringe + 0, 1), c, inflate=0.1)
        else:
            p.add("head", (-4, 32 - fringe, -5), (8, fringe + 1, 1), c, inflate=0.1)


def build_hair(i):
    name, c = HAIR[i]
    hi = shade(c, 1.18)
    p = Part("hair_%d" % i, 64, bones=[("head", None, (0, 24, 0), None), ("tail", "head", (0, 30, 4), None), ("tail_r", "head", (-4, 30, 3), None),
                                       ("tail_l", "head", (4, 30, 3), None)])
    swayed = []
    if i == 0:
        hair_cap(p, c, sides=3, back=6, fringe=2)
    elif i == 1:  # spiky: cap plus lean-out spikes
        hair_cap(p, c, sides=2, back=5, fringe=1, bang_gap=True)
        for k, (x, z, rx, rz) in enumerate([(-3, -3, -25, -20), (0, -3.5, -30, 0), (3, -3, -25, 20), (-3.5, 1, 0, -30), (3.5, 1, 0, 30), (-1.5, 3.5, 30, -10), (1.5, 3.5, 30, 10), (0, 0, 0, 0)]):
            p.add("head", (x - 0.5, 33, z - 0.5), (1, 4, 1), c if k % 2 else hi, rotation=(rx, 0, rz), pivot=(x, 33, z))
    elif i == 2:  # long straight
        hair_cap(p, c, sides=6, back=7, fringe=2)
        p.add("head", (-4, 18, 4), (8, 9, 1), c, inflate=0.2)
        for sx in (4, -5):
            p.add("head", (sx, 23, -3), (1, 8, 4), c, inflate=0.1)
    elif i == 3:  # ponytail
        hair_cap(p, c, sides=3, back=6, fringe=2)
        p.add("tail", (-1, 21, 4), (2, 10, 2), c, pivot=(0, 30, 4))
        p.add("tail", (-1.5, 30, 3.5), (3, 2, 2), hi)
        swayed = ["tail"]
    elif i == 4:  # twin tails
        hair_cap(p, c, sides=3, back=5, fringe=2)
        for b, x in (("tail_l", 5), ("tail_r", -7)):
            p.add(b, (x, 19, 2), (2, 11, 2), c)
            p.add(b, (x - 0.5, 29, 1.5), (3, 2, 3), hi)
        swayed = ["tail_l", "tail_r"]
    elif i == 5:  # bob
        hair_cap(p, c, sides=5, back=6, fringe=2)
        p.add("head", (-4, 26, 4), (8, 1, 2), hi, inflate=0.1)
    elif i == 6:  # bun
        hair_cap(p, c, sides=3, back=5, fringe=2)
        p.add("head", (-2, 33, 1), (4, 3, 4), hi, inflate=0.2)
        p.add("head", (-3, 32, 0), (6, 1, 6), c)
    elif i == 7:  # mohawk
        p.add("head", (-1, 32, -4), (2, 4, 8), c, inflate=0.1)
        for k in range(4):
            p.add("head", (-1, 32 + 2 + (k % 2), -4 + k * 2), (2, 2, 2), hi if k % 2 else c, inflate=0.05)
        p.add("head", (-1, 25, 4), (2, 8, 1), c, inflate=0.1)
        p.add("head", (-4, 31, -4), (8, 1, 8), shade(c, 0.5), inflate=0.1)
    elif i == 8:  # wild, messy
        hair_cap(p, c, sides=4, back=7, fringe=2, bang_gap=True)
        for k in range(10):
            ang = k * 36 + 12
            x, z = 4.2 * math.sin(math.radians(ang)), 4.2 * math.cos(math.radians(ang))
            p.add("head", (x - 0.5, 31.5, z - 0.5), (1, 4, 1), hi if k % 3 == 0 else c, rotation=(-35 * math.cos(math.radians(ang)), 0, 35 * math.sin(math.radians(ang))), pivot=(x, 31.5, z))
    elif i == 9:  # side swept
        hair_cap(p, c, sides=3, back=6, fringe=3)
        p.add("head", (-5, 27, -5), (3, 7, 2), hi, inflate=0.1)
        p.add("head", (-5, 24, -4), (2, 4, 2), c)
    elif i == 10:  # braid
        hair_cap(p, c, sides=3, back=6, fringe=2)
        for k in range(5):
            p.add("tail", (-1 + (0.5 if k % 2 else -0.5), 26 - k * 2.5 + 3, 4 + 0.5), (2, 3, 2), hi if k % 2 else c, pivot=(0, 30, 4))
        swayed = ["tail"]
    elif i == 11:  # ahoge
        hair_cap(p, c, sides=3, back=6, fringe=2)
        p.add("head", (-0.5, 33, -1), (1, 3, 1), c, rotation=(0, 0, 18), pivot=(0, 33, -1))
        p.add("head", (-1.2, 35.6, -1), (1, 2, 1), hi, rotation=(0, 0, 42), pivot=(0, 33, -1))
    elif i == 12:  # drills
        hair_cap(p, c, sides=3, back=5, fringe=2)
        for sx in (1, -1):
            for k in range(4):
                p.add("head", (sx * (5.5 + k * 0.4) - 1.2, 28 - k * 2.2, -1.5 + (k % 2) * 1.0), (3, 3, 3), c if k % 2 else hi, inflate=-0.2 + 0.0)
    elif i == 13:  # pompadour
        hair_cap(p, c, sides=3, back=6, fringe=1)
        p.add("head", (-4, 32, -7), (8, 3, 4), hi, inflate=0.2)
        p.add("head", (-4, 32, -4), (8, 4, 4), c, inflate=0.1)
    elif i == 14:  # layered long with bangs
        hair_cap(p, c, sides=6, back=7, fringe=3, bang_gap=True)
        p.add("head", (-4, 17, 4), (8, 10, 1), c, inflate=0.25)
        p.add("head", (-3, 17, 5), (6, 6, 1), hi, inflate=0.05)
        for sx in (4, -5):
            p.add("head", (sx, 22, -3), (1, 9, 5), c, inflate=0.1)
    elif i == 15:  # hime cut
        hair_cap(p, c, sides=3, back=7, fringe=3)
        p.add("head", (-4, 17, 4), (8, 10, 1), c, inflate=0.25)
        for sx in (4, -6):
            p.add("head", (sx, 20, -4), (2, 12, 3), hi if sx > 0 else c, inflate=0.1)
        p.add("head", (-4, 31, -5), (8, 1, 1), shade(c, 0.6), inflate=0.1)
    anim = idle_sway(swayed, 2.4, (6, 0, 4), 0.35) if swayed else idle_sway(["head"], 4.0, (0, 0, 0))
    return p, anim


# ============================================================================================ horns (8)
HORN_COL = [((226, 214, 188), None), ((92, 86, 80), None), ((236, 200, 96), (255, 200, 80)), ((30, 28, 38), (170, 40, 200)),
            ((240, 240, 255), (180, 220, 255)), ((176, 40, 40), (255, 80, 40)), ((150, 108, 72), None), ((38, 30, 56), (120, 60, 255))]


def horn_chain(p, side, base, segs, colour, glow, size0=2, rise=2.2, out=0.6, back=0.0, curl=14):
    """A curved horn made of boxes stepping out and up; each box leans a little more than the one before it (curl, degrees)."""
    x, y, z = base
    s = size0
    for k in range(segs):
        t = k / max(1, segs - 1)
        sz = max(1, int(round(size0 - t * (size0 - 1))))
        ang = curl * k
        p.add("head", (side * x - sz / 2.0, y, z - sz / 2.0), (sz, 3, sz), colour, glow if (glow and k >= segs - 1) else None, style="bone",
              rotation=(0, 0, -side * ang), pivot=(side * x, y, z))
        x += out * math.cos(math.radians(ang * 1.2)) * 1.0
        y += rise * math.cos(math.radians(ang))
        z += back


def build_horn(i):
    c, g = HORN_COL[i]
    p = Part("horn_%d" % i, 64, bones=[("head", None, (0, 24, 0), None)])
    if i == 0:      # small nubs
        for s in (1, -1):
            horn_chain(p, s, (2.5, 31.6, -2.0), 2, c, g, size0=2, rise=1.4, out=0.3, curl=10)
    elif i == 1:    # ram curl
        for s in (1, -1):
            horn_chain(p, s, (4.4, 29.5, -0.5), 6, c, g, size0=3, rise=0.6, out=1.5, curl=-28)
    elif i == 2:    # long straight swept back, gold
        for s in (1, -1):
            horn_chain(p, s, (2.0, 31.8, -1.0), 5, c, g, size0=2, rise=2.3, out=0.5, back=0.9, curl=6)
    elif i == 3:    # crown of five spikes, glowing tips
        for k, (x, h, lean) in enumerate([(0, 6, 0), (2.6, 5, 14), (-2.6, 5, -14), (5.0, 3, 28), (-5.0, 3, -28)]):
            for j in range(h):
                p.add("head", (x - 1, 32.4 + j * 1.0 - 0.4, -3.2 - 0.5), (2, 1, 2) if j < h - 1 else (1, 1, 1), c if j < h - 2 else (g or c) if j == h - 1 else c,
                      glow=g if j == h - 1 else None, style="bone", rotation=(0, 0, -lean * (j / max(1, h))), pivot=(x, 32.4, -3.2))
        p.add("head", (-4, 32, -4.5), (8, 1, 1), c, inflate=0.3, style="plate")
    elif i == 4:    # unicorn
        for j in range(6):
            sz = 2 if j < 3 else 1
            p.add("head", (-sz / 2.0, 32 + j * 1.6, -3.2 - j * 0.5), (sz, 2, sz), c, glow=g if j > 3 else None, style="bone", rotation=(-18, 0, 0), pivot=(0, 32, -3.2))
    elif i == 5:    # oni twin, thick and short
        for s in (1, -1):
            horn_chain(p, s, (2.2, 31.8, -3.0), 3, c, g, size0=3, rise=1.6, out=0.2, curl=0)
            p.add("head", (s * 2.2 - 1, 36.2, -3.5), (2, 2, 2), shade(c, 1.1), glow=g, style="bone", rotation=(0, 0, 0), pivot=(s * 2.2, 36, -3))
    elif i == 6:    # antlers
        for s in (1, -1):
            for j, (dx, dy, ln) in enumerate([(0, 0, 5), (1, 3, 4), (2.2, 5, 3), (0.5, 7, 3)]):
                p.add("head", (s * (2.2 + dx) - 0.5, 31.8 + dy, -1.2), (1, ln, 1), c, style="bone", rotation=(0, 0, -s * (14 + 12 * (j % 2))), pivot=(s * (2.2 + dx), 31.8 + dy, -1.2))
    elif i == 7:    # thin bat curves, glowing
        for s in (1, -1):
            horn_chain(p, s, (3.2, 31.6, 0.0), 6, c, g, size0=1, rise=1.7, out=0.9, back=0.3, curl=17)
    return p, None


# ============================================================================================ wings (6)
def wing_rows(i):
    """(row name, z, [(angle, length)], width, base colour, tip colour, style) per wing design."""
    W, P = (250, 250, 255), (226, 232, 255)
    designs = {
        0: [("cov", 4.2, [(8, 6), (26, 7), (44, 8), (62, 7), (80, 6)], 3, W, (230, 236, 255), "strand"),
            ("sec", 3.1, [(-6, 9), (8, 11), (22, 13), (36, 14), (50, 13), (64, 11), (78, 9)], 3, (240, 242, 255), (206, 218, 255), "strand"),
            ("pri", 2.0, [(-34, 13), (-21, 16), (-8, 19), (5, 21), (18, 22), (31, 21), (45, 19), (60, 16), (76, 13)], 3, P, (170, 188, 240), "strand")],
        1: [("arm", 2.4, [(20, 14), (50, 12), (74, 9)], 1, (46, 28, 44), (70, 36, 64), "bone"),
            ("mem", 3.0, [(0, 17), (14, 18), (28, 17), (42, 16), (56, 14), (70, 12)], 5, (92, 28, 56), (150, 36, 70), "membrane")],
        2: [("arm", 2.4, [(24, 17), (52, 15), (76, 12)], 2, (56, 78, 52), (86, 110, 70), "scale"),
            ("mem", 3.0, [(-4, 15), (10, 19), (24, 21), (38, 20), (52, 18), (66, 15), (80, 12)], 6, (70, 120, 80), (110, 164, 92), "scale")],
        3: [("cov", 4.0, [(10, 7), (30, 8), (50, 8), (70, 7)], 4, (216, 250, 236), (166, 232, 255), "membrane"),
            ("sec", 3.0, [(-8, 10), (12, 12), (32, 12), (52, 11), (72, 9)], 5, (190, 238, 255), (232, 190, 255), "membrane")],
        4: [("cov", 4.2, [(8, 6), (26, 7), (44, 8), (62, 7), (80, 6)], 3, (36, 34, 50), (56, 44, 74), "strand"),
            ("sec", 3.1, [(-6, 9), (8, 11), (22, 13), (36, 14), (50, 13), (64, 11), (78, 9)], 3, (28, 26, 40), (52, 36, 82), "strand"),
            ("pri", 2.0, [(-34, 13), (-21, 16), (-8, 19), (5, 21), (18, 22), (31, 21), (45, 19), (60, 16), (76, 13)], 3, (22, 20, 32), (110, 44, 150), "strand")],
        5: [("shard", 2.6, [(-30, 10), (-14, 14), (2, 17), (18, 18), (34, 16), (50, 13), (66, 10)], 3, (80, 160, 220), (190, 240, 255), "plate"),
            ("inner", 3.6, [(0, 8), (20, 9), (40, 9), (60, 8)], 2, (60, 120, 190), (140, 210, 250), "plate")],
    }
    return designs[i]


def build_wing(i):
    p = Part("wing_%d" % i, 128, bones=[("root", None, (0, 0, 0), None)])
    rows = wing_rows(i)
    sweep = 22.0
    bones = [("root", None, (0, 0, 0), None)]
    for side, sx in (("l", 1), ("r", -1)):
        bones.append(("wing_" + side, "root", (sx * 2.6, 20.0, 3.0), (0, -sx * sweep, 0)))
        for (rn, z, fs, w, base, tip, style) in rows:
            bones.append(("%s_%s" % (side, rn), "wing_" + side, (sx * 2.6, 20.0, z + 0.5), None))
    p.bones = bones
    glow_tip = (120, 200, 255) if i == 5 else ((255, 220, 140) if i == 0 else None)
    for side, sx in (("l", 1), ("r", -1)):
        for (rn, z, fs, w, base, tip, style) in rows:
            for k, (ang, ln) in enumerate(fs):
                col = lerp(base, tip, 0.45 + 0.35 * (k % 2)) if style != "strand" else lerp(base, tip, 0.15 + 0.5 * (k / max(1, len(fs) - 1)))
                px, py, pz = sx * 2.6, 20.0, z
                p.add("%s_%s" % (side, rn), (px, py - w / 2.0, pz), (ln, w, 1), col, glow=glow_tip if (rn in ("pri", "shard") and glow_tip) else None, style=style,
                      rotation=(0, 0, ang if sx > 0 else 180.0 - ang), pivot=(px, py, pz + 0.5))
        p.add("wing_" + side, (sx * 1.4 - 1.5, 17.5, 2.2), (3, 5, 1), (176, 150, 90) if i not in (1, 2, 4) else (70, 60, 70), inflate=0.15, style="plate")
    L = 3.2
    a = Anim()
    a.clip("idle", length=L, loop=True)
    for side, sx in (("l", 1), ("r", -1)):
        a.rot("idle", "wing_" + side, {0.0: (0, 0, 0), L / 2: (0, -sx * 9, 1.5), L: (0, 0, 0)})
        for ri, (rn, *_r) in enumerate(rows):
            ph = ri * 0.22
            a.rot("idle", "%s_%s" % (side, rn), {0.0: (0, 0, 0), L * (0.25 + ph / 3): (0, 0, sx * 2.4), L * (0.75 + ph / 3): (0, 0, -sx * 2.4), L: (0, 0, 0)})
    return p, a


# ============================================================================================ outfit extras (3D parts)
def build_extra(n):
    p = Part("extra_%d" % n, 64, bones=[("body", None, (0, 24, 0), None), ("cape", "body", (0, 23, 2.2), None)])
    leather, cloth, trim, metal = (112, 72, 44), (60, 70, 130), (230, 196, 100), (170, 176, 190)
    if n == 1:     # leather shoulder pads + belt pouch
        for s in (1, -1):
            p.add("body", (s * 6 - 2.5, 22, -2.5), (5, 2, 5), leather, inflate=0.2, style="plate")
        p.add("body", (3, 12.5, -3.2), (3, 3, 1), shade(leather, 0.8), style="plate")
    elif n == 2:   # mage: a mantle round the shoulders and a long back panel
        p.add("body", (-5, 21.5, -3), (10, 2, 6), cloth, inflate=0.2, style="plate")
        p.add("cape", (-4, 4, 2.2), (8, 18, 1), shade(cloth, 0.85), style="strand")
        p.add("body", (-5.2, 21.5, -3.3), (10, 1, 1), trim)
    elif n == 3:   # knight: plate pauldrons + short cape
        for s in (1, -1):
            p.add("body", (s * 6.2 - 3, 21.6, -3), (6, 3, 6), metal, inflate=0.15, style="plate")
            p.add("body", (s * 6.2 - 2, 24.4, -2), (4, 1, 4), shade(metal, 1.1), inflate=0.1, style="plate")
        p.add("cape", (-4, 8, 2.2), (8, 15, 1), (150, 36, 44), style="strand")
        p.add("body", (-4.5, 22.6, 2.0), (9, 1, 1), trim)
    elif n == 5:   # noble coat tails
        p.add("cape", (-3.5, 3, 2.1), (3, 10, 1), (40, 40, 70), style="strand")
        p.add("cape", (0.5, 3, 2.1), (3, 10, 1), (40, 40, 70), style="strand")
        p.add("body", (-4.5, 12.4, -2.5), (9, 1, 5), trim, style="plate")
    elif n == 8:   # demon lord: spiked pauldrons + long dark cape
        for s in (1, -1):
            p.add("body", (s * 6.4 - 3, 21.4, -3), (6, 3, 6), (26, 22, 34), inflate=0.2, style="plate")
            for k in range(3):
                p.add("body", (s * (5 + k * 1.3) - 0.5, 24.2, -1 + k * 1.5), (1, 3, 1), (150, 130, 80), glow=(220, 60, 40), style="bone",
                      rotation=(0, 0, -s * (10 + k * 10)), pivot=(s * (5 + k * 1.3), 24.2, -1 + k * 1.5))
        p.add("cape", (-4.5, 0, 2.2), (9, 22, 1), (30, 18, 40), style="strand")
        p.add("cape", (-4.5, 0, 2.7), (9, 1, 1), (210, 170, 70), glow=(255, 150, 40))
    elif n == 10:  # formal: apron / tail panel front and back
        p.add("body", (-4.5, 9, -3), (9, 5, 1), (236, 236, 244), inflate=0.1, style="plate")
        p.add("cape", (-3, 6, 2.1), (6, 9, 1), (26, 26, 34), style="strand")
    elif n == 11:  # sage: hooded cloak back and collar
        p.add("cape", (-4.5, 2, 2.2), (9, 21, 1), (40, 84, 150), style="strand")
        p.add("body", (-5, 22, -3), (10, 2, 6), (52, 104, 176), inflate=0.2, style="plate")
        p.add("body", (-5, 25, 2.5), (10, 3, 2), (40, 84, 150), style="plate")
        p.add("body", (-4, 23.4, -3.5), (8, 1, 1), (150, 240, 255), glow=(120, 220, 255))
    anim = Anim()
    anim.clip("idle", length=3.0, loop=True)
    anim.rot("idle", "cape", {0.0: (0, 0, 0), 1.5: (4, 0, 0.8), 3.0: (0, 0, 0)})
    return p, anim


# ============================================================================================ aura ring
def build_aura():
    p = Part("aura_ring", 32, bones=[("root", None, (0, 0, 0), None), ("ring_a", "root", (0, 0, 0), None), ("ring_b", "root", (0, 0, 0), None),
                                     ("motes", "root", (0, 0, 0), None)])
    W = (255, 255, 255)
    n = 24
    for k in range(n):
        th = 360.0 * k / n
        r = 15.0
        cx, cz = r * math.sin(math.radians(th)), r * math.cos(math.radians(th))
        p.add("ring_a", (cx - 2, 0.0, cz - 0.5), (4, 1, 1), W, inflate=0.1, rotation=(0, th, 0), pivot=(cx, 0.5, cz), style="plate")
        if k % 3 == 0:
            p.add("ring_a", (cx - 0.5, 0.0, cz - 0.5), (1, 3, 1), W, inflate=0.05, style="plate")
    n2 = 16
    for k in range(n2):
        th = 360.0 * k / n2 + 11.25
        r = 11.0
        cx, cz = r * math.sin(math.radians(th)), r * math.cos(math.radians(th))
        p.add("ring_b", (cx - 1.5, 0.3, cz - 0.5), (3, 1, 1), (210, 210, 210), rotation=(0, th, 0), pivot=(cx, 0.8, cz), style="plate")
    for k in range(6):
        th = 60.0 * k
        r = 13.0
        cx, cz = r * math.sin(math.radians(th)), r * math.cos(math.radians(th))
        p.add("motes", (cx - 0.5, 4.0 + (k % 3) * 4, cz - 0.5), (1, 1, 1), W, style="plate")
    a = Anim()
    L = 8.0
    a.clip("idle", length=L, loop=True)
    a.rot("idle", "ring_a", {0.0: (0, 0, 0), L / 2: (0, 180, 0), L: (0, 360, 0)})
    a.rot("idle", "ring_b", {0.0: (0, 360, 0), L / 2: (0, 180, 0), L: (0, 0, 0)})
    a.pos("idle", "motes", {0.0: (0, 0, 0), L * 0.25: (0, 6, 0), L * 0.5: (0, 12, 0), L * 0.75: (0, 6, 0), L: (0, 0, 0)})
    a.rot("idle", "motes", {0.0: (0, 0, 0), L: (0, 360, 0)})
    return p, a


# ============================================================================================ skins: player layout helpers
PARTS1 = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4), "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}
PARTS2 = {"head": (32, 0, 8, 8, 8), "body": (16, 32, 8, 12, 4), "rarm": (40, 32, 4, 12, 4), "larm": (48, 48, 4, 12, 4), "rleg": (0, 32, 4, 12, 4), "lleg": (0, 48, 4, 12, 4)}


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


class Sheet:
    def __init__(self):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.im.load()

    def rect(self, x, y, w, h, c, a=255):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if 0 <= xx < 64 and 0 <= yy < 64:
                    self.px[xx, yy] = (c[0], c[1], c[2], a)

    def dot(self, x, y, c, a=255):
        self.rect(x, y, 1, 1, c, a)

    def face(self, parts, part, face, fn):
        fx, fy, fw, fh = faces(*parts[part])[face]
        for yy in range(fh):
            for xx in range(fw):
                c = fn(xx, yy, fw, fh)
                if c is not None:
                    self.dot(fx + xx, fy + yy, c[:3], c[3] if len(c) > 3 else 255)

    def all_faces(self, parts, part, fn, only=None):
        for fname in faces(*parts[part]):
            if only is None or fname in only:
                self.face(parts, part, fname, lambda x, y, w, h, fname=fname: fn(fname, x, y, w, h))


def build_base(group):
    """Grey-shaded flesh (the runtime multiplies it by the skin tone) with a mask image: R = flesh weight, G = iris. Linen undershirt and shorts."""
    sk, mk = Sheet(), Sheet()
    linen = (214, 204, 184)
    bulky, small = group == "bulky", group == "small"

    def flesh(fname, x, y, w, h, part=""):
        n = hash2(x + 31 * ord(fname[0]), y, sum(map(ord, part)) & 7)
        g = 236 - (0 if fname != "bottom" else 24) + int(10 * n) - int(18 * (y / max(1, h)) if fname in ("front", "back", "left", "right") else 0)
        if bulky:
            g -= 14 if (x + y) % 5 == 0 else 0
        return (g, g, g, 255)

    for part, parts_def in PARTS1.items():
        sk.all_faces(PARTS1, part, lambda f, x, y, w, h, part=part: flesh(f, x, y, w, h, part))
        mk.all_faces(PARTS1, part, lambda f, x, y, w, h: (255, 0, 0, 255))
    # undershirt on the body, and the first rows of the arms (short sleeves); shorts on the top of the legs
    sk.all_faces(PARTS1, "body", lambda f, x, y, w, h: (shade(linen, 0.9 + 0.1 * hash2(x, y, 5)) + (255,)))
    mk.all_faces(PARTS1, "body", lambda f, x, y, w, h: (0, 0, 0, 255))
    for arm in ("rarm", "larm"):
        sk.all_faces(PARTS1, arm, lambda f, x, y, w, h: (shade(linen, 0.92) + (255,)) if (y < 3 and f not in ("top", "bottom")) else None)
        mk.all_faces(PARTS1, arm, lambda f, x, y, w, h: (0, 0, 0, 255) if (y < 3 and f not in ("top", "bottom")) else None)
    for leg in ("rleg", "lleg"):
        sk.all_faces(PARTS1, leg, lambda f, x, y, w, h: (shade((120, 96, 72), 0.9 + 0.1 * hash2(x, y, 9)) + (255,)) if (y < 4 and f not in ("bottom",)) else None)
        mk.all_faces(PARTS1, leg, lambda f, x, y, w, h: (0, 0, 0, 255) if (y < 4 and f not in ("bottom",)) else None)
    # neckline and hem on the body
    sk.face(PARTS1, "body", "front", lambda x, y, w, h: (shade(linen, 0.7) + (255,)) if (y == 0 and 2 <= x <= 5) else ((shade(linen, 0.75) + (255,)) if y == h - 1 else None))
    # face
    hx, hy, hw, hh = faces(*PARTS1["head"])["front"]
    def put(x, y, c, m=(255, 0, 0)):
        sk.dot(hx + x, hy + y, c)
        mk.dot(hx + x, hy + y, m)
    brow = (66, 52, 44)
    ey = 4 if not small else 3
    for ex in (1, 5):
        put(ex, ey - 1, brow, (0, 0, 0)); put(ex + 1, ey - 1, brow, (0, 0, 0))
        if not small:
            put(ex, ey, (250, 250, 250), (0, 0, 0)); put(ex + 1, ey, (250, 250, 250), (0, 0, 0))
            put(ex, ey + 1, (150, 150, 150), (0, 255, 0)); put(ex + 1, ey + 1, (110, 110, 110), (0, 255, 0))
        else:     # goblin: big 2x3 eyes
            put(ex, ey, (250, 250, 250), (0, 0, 0)); put(ex + 1, ey, (250, 250, 250), (0, 0, 0))
            put(ex, ey + 1, (160, 160, 160), (0, 255, 0)); put(ex + 1, ey + 1, (160, 160, 160), (0, 255, 0))
            put(ex, ey + 2, (110, 110, 110), (0, 255, 0)); put(ex + 1, ey + 2, (110, 110, 110), (0, 255, 0))
    put(3, 5 if not small else 5, (200, 200, 200)); put(4, 5 if not small else 5, (200, 200, 200))  # nose shadow (flesh)
    mouth = (140, 70, 70)
    put(3, 6, mouth, (0, 0, 0)); put(4, 6, mouth, (0, 0, 0))
    if bulky:  # tusks and a heavy brow
        put(2, 6, (240, 236, 220), (0, 0, 0)); put(5, 6, (240, 236, 220), (0, 0, 0))
        for ex in (1, 2, 5, 6):
            put(ex, ey - 2, (150, 150, 150))
        for k in range(6):  # old scars on the arms
            sk.dot(44 + (k % 3), 24 + k, (190, 190, 190))
    # ears on the sides of the head (a little darker flesh)
    for fname in ("left", "right"):
        fx, fy, fw, fh = faces(*PARTS1["head"])[fname]
        for yy in range(3, 5):
            for xx in range(fw // 2 - 1, fw // 2 + 1):
                sk.dot(fx + xx, fy + yy, (206, 206, 206))
    # the hair area of the base: bald scalp as plain flesh with a faint hairline (hair is a 3D part)
    for a, b in ((sk, "skin"), (mk, "mask")):
        pass
    return sk.im, mk.im


def eyes_glow():
    """The iris pixels of the human group in white (the other groups put theirs in the same place or one row higher: the glow covers both)."""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    hx, hy, hw, hh = faces(*PARTS1["head"])["front"]
    for ex in (1, 5):
        for y in (4, 5):   # rows ey .. ey+1 of the human; the goblin row ey-1+... is within 3..5
            for x in (ex, ex + 1):
                im.putpixel((hx + x, hy + y), (255, 255, 255, 255))
        for x in (ex, ex + 1):
            im.putpixel((hx + x, hy + 6), (255, 255, 255, 255)) if False else None
    return im


# outfits: (name, main, second, trim, long robe?, collar, belt)
OUTFITS = [
    ("traveler", (120, 96, 66), (88, 70, 50), (210, 190, 140), False, True, True),
    ("adventurer", (96, 70, 48), (64, 52, 42), (200, 170, 100), False, False, True),
    ("mage robe", (64, 52, 150), (44, 36, 110), (230, 200, 90), True, True, True),
    ("knight", (150, 156, 170), (96, 102, 120), (210, 180, 90), False, True, True),
    ("diver", (30, 30, 40), (150, 36, 44), (230, 230, 240), False, False, True),
    ("noble", (60, 56, 100), (110, 40, 70), (235, 200, 110), False, True, True),
    ("assassin", (28, 28, 34), (54, 54, 64), (130, 30, 34), False, True, True),
    ("priestess", (240, 240, 248), (190, 196, 232), (240, 210, 110), True, True, True),
    ("demon regalia", (26, 22, 34), (80, 24, 40), (214, 170, 70), False, True, True),
    ("ranger", (62, 108, 70), (92, 70, 48), (190, 220, 150), False, True, True),
    ("formal", (30, 30, 38), (240, 240, 248), (190, 150, 80), False, True, False),
    ("sage cloak", (46, 92, 160), (30, 62, 120), (150, 240, 255), True, True, True),
]


def build_outfit(idx):
    name, main, second, trim, longrobe, collar, belt = OUTFITS[idx]
    s = Sheet()

    def cloth(f, x, y, w, h, c=main):
        n = hash2(x + 7 * ord(f[0]), y, idx)
        k = 0.92 + 0.12 * n
        if f in ("top", "bottom"):
            k *= 0.85
        return shade(c, k) + (255,)

    # layer 1 (the body-hugging shell) and layer 2 (the slightly bigger shell)
    for parts in (PARTS1, PARTS2):
        outer = parts is PARTS2
        c = main if not outer else shade(main, 1.04)
        s.all_faces(parts, "body", lambda f, x, y, w, h, c=c: cloth(f, x, y, w, h, c))
        for arm in ("rarm", "larm"):
            s.all_faces(parts, arm, lambda f, x, y, w, h, c=c: cloth(f, x, y, w, h, c) if (outer or y >= 0) else None)
            # cuffs
            s.all_faces(parts, arm, lambda f, x, y, w, h: (shade(trim, 0.9) + (255,)) if (y >= h - 2 and f not in ("top", "bottom")) else None)
        for leg in ("rleg", "lleg"):
            ln = 12 if longrobe else (7 if outer else 5)
            s.all_faces(parts, leg, lambda f, x, y, w, h, c=second, ln=ln: cloth(f, x, y, w, h, c) if (y < ln and f != "bottom") else None)
            # boots
            s.all_faces(parts, leg, lambda f, x, y, w, h: (shade((54, 40, 34), 0.9 + 0.2 * hash2(x, y)) + (255,)) if (y >= h - 3 and not longrobe) else None)
    # front details on the body, both layers
    for parts in (PARTS1, PARTS2):
        s.face(parts, "body", "front", lambda x, y, w, h: (shade(trim, 0.95) + (255,)) if x in (3, 4) and idx in (0, 2, 5, 7, 10, 11) and y > 1 else None)
        s.face(parts, "body", "front", lambda x, y, w, h: (shade(second, 1.0) + (255,)) if idx in (1, 3, 4, 9) and (x == y // 2 + 1 or x == 6 - y // 2) and y < 6 else None)
        if belt:
            for f in ("front", "back", "left", "right"):
                s.face(parts, "body", f, lambda x, y, w, h: (shade(second if idx != 4 else trim, 0.85) + (255,)) if y == h - 3 else None)
            s.face(parts, "body", "front", lambda x, y, w, h: (shade(trim, 1.1) + (255,)) if (y == h - 3 and x in (3, 4)) else None)
        if collar:
            for f in ("front", "back", "left", "right"):
                s.face(parts, "body", f, lambda x, y, w, h: (shade(trim, 0.95) + (255,)) if y == 0 else None)
    # outfit specific marks
    if idx == 3:   # plate: rivets
        s.face(PARTS1, "body", "front", lambda x, y, w, h: (230, 232, 240, 255) if (x in (1, 6) and y in (2, 6)) else None)
    if idx == 4:   # diver: a white emblem on the back
        s.face(PARTS1, "body", "back", lambda x, y, w, h: (235, 235, 245, 255) if (x in (3, 4) and 2 <= y <= 7) or (y == 4 and 2 <= x <= 5) else None)
    if idx == 6:   # assassin: a mask band and wraps
        s.face(PARTS2, "head", "front", lambda x, y, w, h: (30, 30, 36, 255) if y in (5, 6, 7) else None)
        for f in ("left", "right", "back"):
            s.face(PARTS2, "head", f, lambda x, y, w, h: (30, 30, 36, 255) if y in (5, 6, 7) else None)
    if idx == 8:   # demon regalia: gold filigree and red inlay
        s.face(PARTS1, "body", "front", lambda x, y, w, h: (200, 40, 50, 255) if (x in (2, 5) and 2 <= y <= 8) else ((214, 170, 70, 255) if (y in (1, 9) and 1 <= x <= 6) else None))
    if idx == 10:  # formal: white shirt front and a tie
        s.face(PARTS1, "body", "front", lambda x, y, w, h: (240, 240, 248, 255) if 2 <= x <= 5 and y > 0 else None)
        s.face(PARTS1, "body", "front", lambda x, y, w, h: (150, 30, 40, 255) if x in (3, 4) and 1 <= y <= 5 else None)
    if idx == 11:  # sage: glowing runes on the hem
        for parts in (PARTS1, PARTS2):
            s.face(parts, "body", "front", lambda x, y, w, h: (150, 240, 255, 255) if (y == h - 2 and x % 2 == 0) else None)
    if idx == 7:   # priestess: gold cross
        s.face(PARTS1, "body", "front", lambda x, y, w, h: (240, 210, 110, 255) if (x in (3, 4) and 2 <= y <= 7) or (y == 4 and 2 <= x <= 5) else None)
    if idx == 9:   # ranger: green hood trim on layer 2 head
        s.face(PARTS2, "head", "front", lambda x, y, w, h: (shade(main, 0.9) + (255,)) if y == 0 else None)
    return s.im


# ============================================================================================ writers
def save_geo(p, anim=None):
    return save_part(p, anim)


def build_all():
    os.makedirs(TEX_DIR, exist_ok=True)
    for i in range(HAIR_COUNT):
        p, a = build_hair(i)
        save_part(p, a)
    for i in range(HORN_COUNT):
        p, a = build_horn(i)
        save_part(p, a)
    for i in range(WING_COUNT):
        p, a = build_wing(i)
        save_part(p, a)
    for n in EXTRA_OUTFITS:
        p, a = build_extra(n)
        save_part(p, a)
    p, a = build_aura()
    save_part(p, a)
    for g in GROUPS:
        sk, mk = build_base(g)
        sk.save(os.path.join(TEX_DIR, "base_%s.png" % g))
        mk.save(os.path.join(TEX_DIR, "base_%s_mask.png" % g))
    for i in range(OUTFIT_COUNT):
        build_outfit(i).save(os.path.join(TEX_DIR, "outfit_%d.png" % i))
    eyes_glow().save(os.path.join(TEX_DIR, "eyes_glow.png"))


def part_ids():
    ids = ["hair_%d" % i for i in range(HAIR_COUNT)] + ["horn_%d" % i for i in range(HORN_COUNT)] + ["wing_%d" % i for i in range(WING_COUNT)]
    ids += ["extra_%d" % n for n in EXTRA_OUTFITS] + ["aura_ring"]
    return ids


def check():
    bad = 0
    for pid in part_ids():
        mid = "%s_%s" % (KEY, pid)
        g = json.load(open(os.path.join(ROOT, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
        tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                print("PROBLEM", mid, b["name"], "parent missing"); bad += 1
            for c in b["cubes"]:
                u, v = c["uv"]; sx, sy, sz = c["size"]
                if u + 2 * (sx + sz) > tw or v + sz + sy > th:
                    print("PROBLEM", mid, b["name"], "uv outside the sheet", c["uv"], c["size"]); bad += 1
        im = Image.open(os.path.join(ROOT, "textures", "entity", mid + ".png"))
        if im.size != (tw, th):
            print("PROBLEM texture", mid, im.size); bad += 1
        an = os.path.join(ROOT, "animations", "entity", mid + ".animation.json")
        if os.path.isfile(an):
            for cn, cl in json.load(open(an))["animations"].items():
                for bn in cl["bones"]:
                    if bn not in names:
                        print("PROBLEM", mid, cn, "animates unknown bone", bn); bad += 1
        print(mid, "cubes", sum(len(b["cubes"]) for b in g["bones"]))
    for g in GROUPS:
        for f in ("base_%s.png" % g, "base_%s_mask.png" % g):
            if Image.open(os.path.join(TEX_DIR, f)).size != (64, 64):
                print("PROBLEM", f); bad += 1
    for i in range(OUTFIT_COUNT):
        if Image.open(os.path.join(TEX_DIR, "outfit_%d.png" % i)).size != (64, 64):
            print("PROBLEM outfit", i); bad += 1
    print("PROBLEMS" if bad else "OK")
    return 1 if bad else 0


# ============================================================================================ previews
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile, glow="FFFFFFFF"):
    return {"kind": "geo", "model": "nusmp:geo/entity/avatar_%s.geo.json" % model, "texture": "nusmp:textures/entity/avatar_%s.png" % model,
            "space": "PLAYER", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": glow, "ages": ages, "tile": list(tile), "floor": False, "figure": False,
            "expect": [], "anim": anim, "views": views}


def scenes():
    sc = {}
    # PLAYER space: the file origin lands 1.5 blocks below the neck line; the head is at file y 24..32 -> world y 0.0 at the file origin
    for i in range(HAIR_COUNT):
        sc["avatar_hair_%d" % i] = scene("hair_%d" % i, "idle", [0, 30], [view("front", 0, 8, (0, 1.9, 0), 1.9), view("3/4 back", 150, 12, (0, 1.9, 0), 2.2)], (260, 260))
    for i in range(HORN_COUNT):
        sc["avatar_horn_%d" % i] = scene("horn_%d" % i, None, [0], [view("front", 0, 8, (0, 1.9, 0), 1.9), view("side", 90, 8, (0, 1.9, 0), 1.9)], (260, 260))
    for i in range(WING_COUNT):
        sc["avatar_wing_%d" % i] = scene("wing_%d" % i, "idle", [0, 40], [view("from behind", 180, 4, (0, 1.2, 0), 4.0), view("3/4 back", 150, 12, (0, 1.2, 0), 4.0)], (320, 300))
    for n in EXTRA_OUTFITS:
        sc["avatar_extra_%d" % n] = scene("extra_%d" % n, "idle", [0], [view("front", 0, 8, (0, 1.2, 0), 3.4), view("3/4 back", 150, 12, (0, 1.2, 0), 3.4)], (300, 300))
    sc["avatar_aura_ring"] = {"kind": "geo", "model": "nusmp:geo/entity/avatar_aura_ring.geo.json", "texture": "nusmp:textures/entity/avatar_aura_ring.png",
                              "space": "PROP", "layer": "ADDITIVE", "argb": "CC66CCFF", "glow": "00000000", "ages": [0, 80], "tile": [320, 260], "floor": False,
                              "figure": False, "expect": [], "anim": "idle", "views": [view("3/4", 30, 28, (0, 0.2, 0), 2.6)]}
    return sc


def write_scenes(path=SCENES_OUT):
    with open(path, "w") as f:
        json.dump(scenes(), f, indent=1)


def sheets():
    """Contact sheets of the skin groups and the outfits (the player layout, 6x scale) for a look at the textures."""
    os.makedirs(SCRATCH, exist_ok=True)
    im = Image.new("RGBA", (64 * 6 * 4 + 40, 64 * 6 * 2 + 30), (60, 70, 90, 255))
    x = 10
    for g in GROUPS:
        b = Image.open(os.path.join(TEX_DIR, "base_%s.png" % g)).resize((384, 384), Image.NEAREST)
        im.paste(b, (x, 10), b)
        x += 394
    im.save(os.path.join(SCRATCH, "sheet_base.png"))
    o = Image.new("RGBA", (64 * 4 * 6 + 70, 64 * 4 * 2 + 30), (60, 70, 90, 255))
    for i in range(OUTFIT_COUNT):
        t = Image.open(os.path.join(TEX_DIR, "outfit_%d.png" % i)).resize((256, 256), Image.NEAREST)
        o.paste(t, (10 + (i % 6) * 266, 10 + (i // 6) * 266), t)
    o.save(os.path.join(SCRATCH, "sheet_outfits.png"))


def render_previews(names=None):
    import shutil
    import subprocess
    sc = scenes()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp"))
    os.makedirs(SCRATCH, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "..", "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if "Picked up" not in l:
            print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(SCRATCH, n + ".png"))
    sheets()
    return r.returncode


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check())
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    build_all()
    write_scenes()
