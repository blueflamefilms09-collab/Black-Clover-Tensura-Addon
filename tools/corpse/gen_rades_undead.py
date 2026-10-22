"""Rades Spirito's numbered undead: models, animations and textures for No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl and No.0 Michael Caesal.

    python3 -B tools/corpse/gen_rades_undead.py              # all five
    python3 -B tools/corpse/gen_rades_undead.py david        # one

Why this exists: the owner asked for custom-looking undead mobs that match the series art of Rades Spirito's Soul Corpse Magic. Each is a GeoLite model built from boxes (tools/geo_builder.py,
the same toolset and the same Body / material helpers as tools/gen_corpse_assets.py, imported here), with a texture per decay stage (stage 0 to 3, the stage the
nusmp:soul_corpse effect carries; later stages are the first plus more dirt and tears) and a glow map per skin that client/corpse/RadesUndeadRenderer draws with the safe additive helper
(violet soul light, and what each figure glows with in the art: Jimmy's golden spiral eyes, Alfredo's visor and spirit aura, Carl's soul and mitre gem, Michael's palms).

The art that was followed (the owner's screenshots of the wiki images):
    No.4 Jimmy    a bandaged, mummy-like dark figure crouching; big golden spiral eyes; straps and buckles.
    No.3 David    a huge hunched stitched patchwork giant in blue-grey and brown; a round belt ring; a sewn face.
    No.2 Alfredo  a tall armoured, belted, dark-skinned bound figure with a hood and a visor; a blue-white spirit aura.
    No.1 Carl     a tall thin priest-like figure in long white ornate robes and a tall white mitre (a box hat); a dark wild-haired body inside the robe.
    No.0 Michael  a lean figure in a white featureless mask with a grey ponytail; a blue-grey robe, a red sash; dark belts and bandages on the arms; arms spread wide.

Writes under src/main/resources/assets/nusmp: geo/entity/rades_<name>.geo.json, animations/entity/rades_<name>.animation.json,
textures/entity/rades_<name>.png, rades_<name>_s1..3.png and a *_glow.png for each. Deterministic (hash noise only). Run python as `python3 -B`.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_corpse_assets as G  # noqa: E402
from geo_builder import Model, Anim, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

S, C, TAU = math.sin, math.cos, 2 * math.pi
fbm, hsh, ramp, dim, lerp, rgb, clamp = G.fbm, G.hsh, G.ramp, G.dim, G.lerp, G.rgb, G.clamp
lx, kf, cyc, sw, loop = G.lx, G.kf, G.cyc, G.sw, G.loop

VIOLET = (170, 130, 255)          # the Soul Corpse soul light
GOLD_EYE = (255, 206, 70)
PALE_BLUE = (190, 225, 255)
DIRT = G.DIRT
THREAD = (20, 16, 18)
THREAD_L = (56, 46, 44)


class RBody(G.Body):
    """The Body of gen_corpse_assets, with the 'rades' key (files rades_<name>.*)."""

    def __init__(self, name, tex=128):
        self.name = name
        self.m = Model("rades", name, tex, tex)
        self.sheet = G.Sheet(tex, tex)
        self.tex = tex

    def box(self, bone, origin, size, mat, **kw):
        """Box-uv boxes need whole-pixel sizes (the texture layout is per pixel): a size is rounded to the nearest pixel, at least 1."""
        return super().box(bone, origin, tuple(max(1, int(math.floor(v + 0.5))) for v in size), mat, **kw)


# =============================================================================================================== materials
def tonal(p, cols, salt, k=0.4):
    return ramp(cols, 0.2 + 0.6 * fbm(p.x * k, p.y * k, salt))


def soiled(p, c, amount=1.0):
    return dim(c, 1.0 - DIRT[p.stage] * amount)


def edge_dark(p, c, k=0.82):
    return dim(c, k) if p.edge == 0 else c


def fabric(p, cols, salt=2, tear=0.0, under=(14, 12, 14), weave=True, hem=False, glow_hem=None, dirt=1.0):
    """Cloth with a weave, soil that deepens per stage, torn holes (which show 'under') and optionally a frayed hem."""
    grain = 0.5 + (0.22 * S(p.x * 1.7 + salt) * C(p.y * 1.3) if weave else 0) + 0.25 * (fbm(p.x * 0.5, p.y * 0.5, salt) - 0.5) * 2
    c = ramp(cols, grain)
    c = soiled(p, c, dirt)
    holes = tear + 0.06 * p.stage
    if holes and fbm(p.x * 0.55, p.y * 0.55, salt + 5) > 1.0 - holes:
        c = under
    if hem and p.face in ("front", "back", "left", "right") and p.t > 0.85 and hsh(int(p.x), int(p.y), salt + 6) < (p.t - 0.85) * 5.0 * (0.5 + 0.25 * p.stage):
        c = (16, 14, 16)
    g = None
    if glow_hem and p.t > 0.9 and p.face in ("front", "back", "left", "right") and p.edge == 0:
        g = (*glow_hem, 70)
    return G.shade_face(p, edge_dark(p, c)), g


def bandage(p, salt=11, cols=((120, 110, 92), (172, 160, 134), (206, 194, 166)), under=(52, 42, 40), gap=0.0):
    """Wrapped bandage: diagonal strips with a dark line between them, frayed ends and, with 'gap', skin showing through."""
    d = (int(p.y) + int(p.x * 0.7) + (p.fid * 2)) % 4
    c = tonal(p, cols, salt, 0.6)
    if d == 0:
        c = dim(c, 0.72)
    if hsh(int(p.x), int(p.y), salt + 2) > 0.94:
        c = dim(c, 0.85)
    c = soiled(p, c, 1.1)
    g = None
    cut = gap + 0.05 * p.stage
    if cut and fbm(p.x * 0.6, p.y * 0.6, salt + 4) > 1.0 - cut:
        c = under
        if hsh(int(p.x), int(p.y), salt + 8) > 0.9:
            g = (*VIOLET, 120)
    return G.shade_face(p, edge_dark(p, c, 0.8)), g


def leather(p, cols=((20, 17, 18), (40, 34, 34), (66, 56, 52)), salt=3, stitch=True):
    c = tonal(p, cols, salt, 0.7)
    c = soiled(p, c, 0.8)
    if stitch and p.edge == 1 and (int(p.x) + int(p.y)) % 2 == 0:
        c = lerp(c, (150, 130, 100), 0.55)
    return G.shade_face(p, edge_dark(p, c, 0.7)), None


def brass(p, salt=5):
    c = ramp([(92, 70, 34), (160, 124, 56), (212, 176, 92)], 0.3 + 0.5 * fbm(p.x * 0.8, p.y * 0.8, salt))
    if p.edge == 0:
        c = dim(c, 0.7)
    return G.shade_face(p, soiled(p, c, 0.3)), None


def gold(p, salt=7):
    c = ramp([(150, 112, 40), (214, 172, 72), (252, 220, 120)], 0.3 + 0.55 * fbm(p.x * 0.8, p.y * 0.8, salt))
    if p.edge == 0:
        c = dim(c, 0.78)
    return G.shade_face(p, soiled(p, c, 0.25)), None


def ornate(p, cols, salt=2, tear=0.0, under=(14, 12, 14)):
    """White ceremonial cloth: the weave, a gold embroidered band every few pixels down the front and back and gold diamonds between them."""
    c, g = fabric(p, cols, salt, tear=tear, under=under, hem=True)
    if p.face in ("front", "back") and p.t < 0.88:
        k = int(p.x) % 6
        if k == 0:
            c = G.shade_face(p, (206, 164, 64))
        elif k == 3 and int(p.y) % 4 in (1, 2) and p.t > 0.08:
            c = G.shade_face(p, (176, 138, 54))
    return c, g


def steel(p, salt=6, rust=0.0):
    c = ramp([(34, 36, 44), (74, 78, 90), (122, 128, 142)], 0.25 + 0.55 * fbm(p.x * 0.6, p.y * 0.35, salt))
    if rust and fbm(p.x * 0.5, p.y * 0.5, salt + 2) > 0.66 - rust - 0.04 * p.stage:
        c = ramp([(58, 34, 24), (100, 58, 32), (136, 80, 40)], hsh(int(p.x), int(p.y), salt))
    if p.edge == 0:
        c = dim(c, 0.72)
    return G.shade_face(p, soiled(p, c, 0.4)), None


def flesh(p, cols, salt, vein=(70, 46, 84), wound=0.0, scar=True, rot=True):
    """A dead body: tonal skin, violet veins, scars, rot spots that grow with the stage."""
    c = tonal(p, cols, salt, 0.45)
    c = soiled(p, c, 0.6)
    g = None
    if abs(fbm(p.x * 0.5 + 40, p.y * 0.5, salt + 5) - 0.5) < 0.028:
        c = lerp(c, vein, 0.75)
        g = (*VIOLET, 46)
    if rot and fbm(p.x * 0.45, p.y * 0.45, salt + 3) > 1.0 - 0.06 * (p.stage + 1):
        c = ramp(G.ROTC, 0.5)
    if wound and fbm(p.x * 0.5, p.y * 0.6, salt + 8) > 1.0 - wound:
        c = ramp(G.WOUND, hsh(int(p.x), int(p.y), salt))
    return G.shade_face(p, c), g


def patchwork(p, tones, salt, cell=9.0, seam_w=0.8, wound=0.0):
    """A stitched patchwork body: cells of several tones with a dark thread seam and cross-stitches along it (and soul light in the stitches)."""
    edge, cid, sid = G.voronoi(p.x, p.y, cell, salt)
    n = len(tones)
    ti, si = min(n - 1, int(cid * n)), min(n - 1, int(sid * n))
    c = tonal(p, tones[ti], salt + 2, 0.4)
    c = soiled(p, c, 0.7)
    g = None
    if wound and fbm(p.x * 0.5, p.y * 0.6, salt + 8) > 1.0 - wound:
        c = ramp(G.WOUND, hsh(int(p.x), int(p.y), salt))
    if ti != si and edge < seam_w:
        c = THREAD if edge < seam_w * 0.5 else THREAD_L
        if (int(p.x) + 2 * int(p.y)) % 5 == 0:
            g = (*VIOLET, 46 + 10 * p.stage)
    elif ti != si and edge < seam_w + 0.9:
        c = lerp(c, THREAD, 0.35)
    if abs(fbm(p.x * 0.5 + 40, p.y * 0.5, salt + 5) - 0.5) < 0.02:
        c = lerp(c, (60, 44, 70), 0.6)
    return G.shade_face(p, c), g


def hair(p, cols, salt=21):
    c = ramp(cols, 0.15 + 0.7 * hsh(int(p.x * 1.0), int(p.y * 0.6), salt) * 0.6 + 0.4 * fbm(p.x * 0.3, p.y * 1.4, salt))
    return G.shade_face(p, c), None


def mask_white(p, salt=31):
    """A smooth white featureless mask: a faint fine grain, a thin cool-grey rim, and nothing else."""
    c = ramp([(214, 214, 220), (238, 238, 242), (250, 250, 252)], 0.4 + 0.5 * fbm(p.x * 0.3, p.y * 0.3, salt))
    c = soiled(p, c, 0.5)
    g = None
    if p.edge == 0:
        c = dim(c, 0.84)
    if p.face == "front" and p.edge == 1 and p.i % 3 == 0:
        g = (*VIOLET, 38)
    return G.shade_face(p, c), g


def glow_gem(p, color):
    return G.shade_face(p, lerp(color, (255, 255, 255), 0.4)), (*color, 255)


# spiral eye masks (1 = gold, 0 = the dark swirl); 4x4
SPIRAL = ("1111", "1001", "1011", "1110")


def spiral_eye(p, ox, oy, base):
    """A big golden spiral eye painted on a front face at (ox, oy), 4x4. Returns (colour, glow) or None outside it."""
    x, y = p.i - ox, p.j - oy
    if not (0 <= x < 4 and 0 <= y < 4):
        return None
    if SPIRAL[y][x] == "1":
        return (250, 196, 64), (*GOLD_EYE, 255)
    return (40, 24, 8), (*GOLD_EYE, 110)


# =============================================================================================================== animation builder
def biped(a, b, P):
    """Shared clips of a two-legged figure; P tunes it. b: role -> bone name (root, body, head, arm_l, arm_r, leg_l, leg_r)."""
    arm_l, arm_r = (P["arm_rest"], 0, -P.get("spread", 4)), (P["arm_rest"], 0, P.get("spread", 4))
    loop(a, "idle", 2.4)
    cyc(a, "idle", b["body"], "rot", 2.4, sw((P["lean"], 0, 0), (1.2, 0, 0.8)))
    cyc(a, "idle", b["head"], "rot", 2.4, sw((P["head"], 0, 0), (2.0, 3.0, 2.0), 0.9))
    cyc(a, "idle", b["arm_l"], "rot", 2.4, sw(arm_l, (3.0, 0, 1.5), 0.4))
    cyc(a, "idle", b["arm_r"], "rot", 2.4, sw(arm_r, (3.0, 0, 1.5), 1.6))
    for name, L, amp, lean, bob in (("walk", 1.0, P["walk"], P["lean"] - 2, P.get("bob", 0.5)), ("run", 0.6, P["run"], P["lean"] - 12, P.get("bob", 0.5) * 2)):
        loop(a, name, L)
        cyc(a, name, b["leg_l"], "rot", L, sw((0, 0, 0), (amp, 0, 0)))
        cyc(a, name, b["leg_r"], "rot", L, sw((0, 0, 0), (-amp, 0, 0)))
        sa = P.get("arm_swing", 0.8)
        cyc(a, name, b["arm_l"], "rot", L, sw((arm_l[0], 0, arm_l[2]), (-amp * sa, 0, 0)))
        cyc(a, name, b["arm_r"], "rot", L, sw((arm_r[0], 0, arm_r[2]), (amp * sa, 0, 0)))
        cyc(a, name, b["body"], "rot", L, sw((lean, 0, 0), (1.5, 3.0, 0)), n=2)
        cyc(a, name, b["body"], "pos", L, lambda th, bob=bob: (0, abs(S(th)) * bob, 0), n=1)
        cyc(a, name, b["head"], "rot", L, sw((P["head"] + (6 if name == "run" else 0), 0, 0), (2.0, 4.0, 1.5), 0.6), n=2)
    a.clip("hurt", 0.4, False)
    a.rot("hurt", b["body"], kf((0.0, (P["lean"], 0, 0)), (0.1, (P["lean"] + 14, 0, 4)), (0.4, (P["lean"], 0, 0))))
    a.rot("hurt", b["head"], kf((0.0, (P["head"], 0, 0)), (0.1, (P["head"] - 16, 0, 5)), (0.4, (P["head"], 0, 0))))
    a.rot("hurt", b["arm_l"], kf((0.0, arm_l), (0.1, (arm_l[0] - 25, 0, arm_l[2] - 10)), (0.4, arm_l)))
    a.rot("hurt", b["arm_r"], kf((0.0, arm_r), (0.1, (arm_r[0] - 25, 0, arm_r[2] + 10)), (0.4, arm_r)))
    a.clip("death", 1.0, False)
    a.rot("death", b["root"], kf((0.0, (0, 0, 0)), (0.35, (14, 0, 2)), (1.0, (88, 0, 6))))
    a.rot("death", b["body"], kf((0.0, (P["lean"], 0, 0)), (1.0, (0, 0, 0))))
    a.rot("death", b["head"], kf((0.0, (P["head"], 0, 0)), (1.0, (-20, 12, 6))))
    a.rot("death", b["arm_l"], kf((0.0, arm_l), (1.0, (-70, 0, -40))))
    a.rot("death", b["arm_r"], kf((0.0, arm_r), (1.0, (-30, 0, 55))))
    a.rot("death", b["leg_l"], kf((0.0, (0, 0, 0)), (1.0, (-8, 0, -6))))
    a.rot("death", b["leg_r"], kf((0.0, (0, 0, 0)), (1.0, (10, 0, 7))))


BONES = dict(root="root", body="body", head="head", arm_l="arm_l", arm_r="arm_r", leg_l="leg_l", leg_r="leg_r")


# =============================================================================================================== NO.4 JIMMY
def make_jimmy():
    B = RBody("jimmy", 128)
    m = B.m
    # about 29 px tall, crouched: knees bent, back bowed, arms hanging to the ground, a head too big for the body.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (3, 11, 0))
    m.bone("shin_l", "leg_l", (3, 6, 0))
    m.bone("leg_r", "root", (-3, 11, 0))
    m.bone("shin_r", "leg_r", (-3, 6, 0))
    m.bone("body", "root", (0, 11, 0))
    m.bone("chest", "body", (0, 15, 0))
    m.bone("head", "chest", (0, 22, -1.5))
    m.bone("jaw", "head", (0, 23, -3))
    m.bone("arm_l", "chest", (6.5, 21, 0))
    m.bone("fore_l", "arm_l", (6.5, 14, 0))
    m.bone("arm_r", "chest", (-6.5, 21, 0))
    m.bone("fore_r", "arm_r", (-6.5, 14, 0))

    skin = lambda p, s=41: flesh(p, [(46, 38, 40), (74, 62, 62), (104, 90, 86)], s, wound=0.05)
    wrap = lambda p, s=42, gap=0.06: bandage(p, s, gap=gap)
    strap = lambda p: leather(p)
    buckle = lambda p: brass(p)

    # legs (painted left, mirrored right): wrapped thigh, a strap and buckle at the knee, a wrapped shin, a bare clawed foot
    c = B.box("leg_l", (lx("l", 3, 4), 6, -2.2), (4, 5, 4.4), wrap)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 3, 4.6), 6.2, -2.6), (4.6, 1, 5.2), strap)
    B.twin("leg_r", c)
    c = B.box("shin_l", (lx("l", 3, 3.6), 1, -1.8), (3.6, 5, 3.6), lambda p: wrap(p, 44, 0.12))
    B.twin("shin_r", c)
    c = B.box("shin_l", (lx("l", 3, 4), 0, -4), (4, 2, 6), skin)
    B.twin("shin_r", c)
    # pelvis + chest: wrapped, a diagonal strap with a buckle, a belt, bare dark skin through the gaps
    B.box("body", (-4, 11, -2.6), (8, 4, 5.2), lambda p: wrap(p, 46, 0.10))
    B.box("body", (-4.4, 12.2, -3.0), (8.8, 1.6, 6.0), strap)
    B.box("body", (-1, 12.1, -3.5), (2, 1.8, 1), buckle)
    B.box("chest", (-5, 15, -3), (10, 7, 6), lambda p: wrap(p, 47, 0.14))
    B.box("chest", (-5.3, 17.5, -3.3), (10.6, 1.4, 6.6), strap)
    B.box("chest", (-1.2, 17.3, -3.9), (2.4, 1.8, 1), buckle)
    B.box("chest", (-5.2, 15.2, -3.2), (3, 6.6, 6.4), strap)                         # a shoulder strap, left side of the figure
    B.box("chest", (-1, 21, -2), (2, 1, 4), skin)
    # the big head: a wrapped skull, dark skin round two large golden spiral eyes, a strapped jaw
    def head_mat(p):
        if p.face == "front":
            for ox in (1, 6):
                r = spiral_eye(p, ox, 2, None)
                if r:
                    return G.shade_face(p, r[0]) if False else r
            if 1 <= p.j <= 6 and (0 <= p.i - 0 <= 10) and (p.i in (0, 5, 10) or p.j in (1, 6)):
                return dim(skin(p, 48)[0], 0.8), None
            if p.j >= 6:
                return wrap(p, 49, 0.02)
        return wrap(p, 50, 0.07) if not (p.face == "front") else wrap(p, 51, 0.04)
    B.box("head", (-5.5, 22.5, -5.5), (11, 8, 8), head_mat)
    B.box("jaw", (-4, 21.2, -4.6), (8, 2, 6.4), lambda p: wrap(p, 52, 0.05))
    B.box("jaw", (-4.3, 21.5, -5.0), (8.6, 1, 7.2), strap)
    # arms: long, wrapped, strapped; claws
    c = B.box("arm_l", (lx("l", 6.5, 3.6), 14, -1.8), (3.6, 7, 3.6), lambda p: wrap(p, 53, 0.08))
    B.twin("arm_r", c)
    c = B.box("arm_l", (lx("l", 6.5, 4), 17.5, -2), (4, 1, 4), strap)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 6.5, 3.6), 6, -1.8), (3.6, 8, 3.6), lambda p: wrap(p, 54, 0.16))
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 6.5, 4.2), 9.5, -2.1), (4.2, 1, 4.2), strap)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 6.5, 4), 2.5, -2), (4, 4, 4), skin)
    B.twin("fore_r", c)
    for fx in (0, 1, 2):
        c = B.box("fore_l", (lx("l", 6.5, 4) + 0.4 + fx * 1.4, -0.5, -1.4), (1, 3.5, 1), lambda p: (G.shade_face(p, (196, 188, 168)), None))
        B.twin("fore_r", c)

    a = Anim()
    P = dict(lean=-18, head=18, arm_rest=-14, walk=34, run=52, spread=6, bob=0.6)
    biped(a, BONES, P)
    for nm, L, k in (("walk", 1.0, 36), ("run", 0.6, 60)):
        cyc(a, nm, "shin_l", "rot", L, lambda th, k=k: (-k * max(0.0, C(th)) * 0.6, 0, 0))
        cyc(a, nm, "shin_r", "rot", L, lambda th, k=k: (-k * max(0.0, -C(th)) * 0.6, 0, 0))
        cyc(a, nm, "fore_l", "rot", L, lambda th: (24 + 14 * max(0.0, S(th)), 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (24 + 14 * max(0.0, -S(th)), 0, 0))
    cyc(a, "idle", "fore_l", "rot", 2.4, sw((14, 0, 0), (3, 0, 0), 0.5))
    cyc(a, "idle", "fore_r", "rot", 2.4, sw((14, 0, 0), (3, 0, 0), 1.9))
    # attack: a pounce and a raking swing of both claws
    a.clip("attack", 0.7, True)
    a.rot("attack", "body", kf((0.0, (-24, 0, 0)), (0.2, (-8, -20, 0)), (0.34, (-44, 26, 0)), (0.52, (-34, 8, 0)), (0.7, (-24, 0, 0))))
    a.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.2, (0, -1.0, 2)), (0.34, (0, 0.5, -5)), (0.7, (0, 0, 0))))
    a.rot("attack", "arm_r", kf((0.0, (-8, 0, 6)), (0.2, (-150, 0, 10)), (0.34, (40, 0, -12)), (0.52, (30, 0, -8)), (0.7, (-8, 0, 6))))
    a.rot("attack", "arm_l", kf((0.0, (-8, 0, -6)), (0.2, (-20, 0, -20)), (0.34, (70, 0, -8)), (0.52, (50, 0, -8)), (0.7, (-8, 0, -6))))
    a.rot("attack", "head", kf((0.0, (24, 0, 0)), (0.2, (34, -8, 0)), (0.34, (16, 10, 0)), (0.7, (24, 0, 0))))
    a.rot("attack", "jaw", kf((0.0, (0, 0, 0)), (0.2, (8, 0, 0)), (0.34, (34, 0, 0)), (0.52, (6, 0, 0)), (0.7, (0, 0, 0))))
    a.rot("attack", "leg_l", kf((0.0, (0, 0, 0)), (0.2, (-10, 0, 0)), (0.34, (24, 0, 0)), (0.7, (0, 0, 0))))
    a.rot("attack", "leg_r", kf((0.0, (0, 0, 0)), (0.2, (16, 0, 0)), (0.34, (-20, 0, 0)), (0.7, (0, 0, 0))))
    return B, a


# =============================================================================================================== NO.3 DAVID
def make_david():
    B = RBody("david", 256)
    m = B.m
    # about 54 px tall (3.4 blocks) and 26 wide: a huge hunched giant sewn from blue-grey and brown pieces, small head, arms to the knees, a round belt ring at the waist.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (6, 22, 0))
    m.bone("shin_l", "leg_l", (6, 11, 0))
    m.bone("leg_r", "root", (-6, 22, 0))
    m.bone("shin_r", "leg_r", (-6, 11, 0))
    m.bone("body", "root", (0, 22, 0))
    m.bone("chest", "body", (0, 31, 0))
    m.bone("head", "chest", (0, 44, -3))
    m.bone("arm_l", "chest", (14, 43, 0))
    m.bone("fore_l", "arm_l", (14, 29, 0))
    m.bone("arm_r", "chest", (-14, 43, 0))
    m.bone("fore_r", "arm_r", (-14, 29, 0))

    BLUE = [(62, 78, 100), (92, 112, 138), (124, 144, 166)]
    BROWN = [(70, 50, 36), (108, 80, 56), (142, 108, 78)]
    GREY = [(78, 84, 88), (112, 118, 120), (146, 150, 150)]
    tones = [BLUE, BROWN, GREY, BLUE, BROWN]
    pw = lambda p, s=61, w=0.06: patchwork(p, tones, s, cell=15.0, seam_w=0.9, wound=w)
    trousers = lambda p: fabric(p, [(44, 34, 26), (74, 58, 42), (104, 84, 62)], 62, tear=0.10, under=BLUE[0], hem=True)
    belt = lambda p: leather(p, [(24, 18, 14), (46, 34, 26), (74, 56, 40)], 63)
    ring = lambda p: brass(p, 64)

    c = B.box("leg_l", (lx("l", 6, 10), 11, -5), (10, 11, 10), trousers)
    B.twin("leg_r", c)
    c = B.box("shin_l", (lx("l", 6, 9), 3, -4.5), (9, 8, 9), lambda p: pw(p, 65))
    B.twin("shin_r", c)
    c = B.box("shin_l", (lx("l", 6, 11), 0, -7), (11, 4, 14), lambda p: fabric(p, [(30, 24, 20), (52, 42, 32), (80, 64, 46)], 66, tear=0.05, under=BROWN[0]))
    B.twin("shin_r", c)
    # belly, belt with the big round ring, barrel chest, a torn waistcoat
    B.box("body", (-10, 22, -6.5), (20, 9, 13), lambda p: pw(p, 67))
    B.box("body", (-10.8, 22, -7.3), (21.6, 4, 14.6), belt)
    # the ring: a square-cut round ring made of four bars at the front, standing proud of the belt
    B.box("body", (-4, 21, -9.2), (8, 2, 2), ring)
    B.box("body", (-4, 27, -9.2), (8, 2, 2), ring)
    B.box("body", (-4, 23, -9.2), (2, 4, 2), ring)
    B.box("body", (2, 23, -9.2), (2, 4, 2), ring)
    B.box("chest", (-12, 31, -7), (24, 13, 14), pw)
    B.box("chest", (-12.6, 36, -7.6), (6, 8, 15.2), lambda p: fabric(p, [(40, 30, 24), (66, 50, 38), (96, 76, 56)], 68, tear=0.14, under=BLUE[1]))
    B.box("chest", (6.6, 36, -7.6), (6, 8, 15.2), lambda p: fabric(p, [(40, 30, 24), (66, 50, 38), (96, 76, 56)], 69, tear=0.14, under=BLUE[1]))
    c = B.box("chest", (lx("l", 14, 10), 42, -5.5), (10, 4, 11), lambda p: pw(p, 70))        # shoulder humps
    B.twin("chest", c)
    # the small sewn head sunk forward between the shoulders: stitched brow, one eye sewn shut, a wide stitched mouth, a bolt at each temple
    def face(p):
        c, g = pw(p, 71, 0.04)
        if p.face == "front":
            if p.j in (3, 4) and 2 <= p.i <= 4:
                return (14, 14, 18), (*VIOLET, 255)
            if p.j == 3 and 7 <= p.i <= 9:
                return THREAD, None
            if p.j in (2, 4) and 7 <= p.i <= 9 and (p.i + p.j) % 2 == 0:
                return THREAD, None
            if p.j == 8 and 1 <= p.i <= 10:
                return (THREAD if p.i % 2 == 0 else THREAD_L), None
            if p.j == 7 and 1 <= p.i <= 10 and p.i % 2 == 1:
                return (186, 176, 150), None
            if p.j == 2 and 1 <= p.i <= 10:
                return dim(c, 0.65), None
        return c, g
    B.box("head", (-6, 43, -8.5), (12, 11, 10), face)
    B.box("head", (-7.2, 47, -3.5), (1.4, 2, 2), ring)
    B.box("head", (5.8, 47, -3.5), (1.4, 2, 2), ring)
    # arms: huge upper arms, forearms with a stitched cuff, fists
    c = B.box("arm_l", (lx("l", 14, 9), 29, -4.5), (9, 15, 9), pw)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 14, 9), 17, -4.5), (9, 12, 9), lambda p: pw(p, 72))
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 14, 10), 16, -5), (10, 2, 10), belt)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 14, 11), 8, -5.5), (11, 9, 11), lambda p: pw(p, 73, 0.06))
    B.twin("fore_r", c)

    a = Anim()
    P = dict(lean=-14, head=12, arm_rest=-6, walk=24, run=32, spread=7, bob=0.8, arm_swing=0.5)
    biped(a, BONES, P)
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (12, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (12, 0, 0))
    # attack: both fists up and a double hammer; cast (the ground slam wind-up): fists overhead, a deep breath, then slammed down
    a.clip("attack", 1.0, True)
    a.rot("attack", "body", kf((0.0, (-14, 0, 0)), (0.3, (6, 0, 0)), (0.5, (-34, 0, 0)), (0.75, (-24, 0, 0)), (1.0, (-14, 0, 0))))
    a.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.3, (0, 1.5, 3)), (0.5, (0, -3, -4)), (1.0, (0, 0, 0))))
    a.rot("attack", "arm_l", kf((0.0, (-6, 0, -7)), (0.3, (-170, 0, -10)), (0.5, (30, 0, -6)), (0.75, (10, 0, -6)), (1.0, (-6, 0, -7))))
    a.rot("attack", "arm_r", kf((0.0, (-6, 0, 7)), (0.3, (-170, 0, 10)), (0.5, (30, 0, 6)), (0.75, (10, 0, 6)), (1.0, (-6, 0, 7))))
    a.rot("attack", "head", kf((0.0, (12, 0, 0)), (0.3, (-4, 0, 0)), (0.5, (26, 0, 0)), (1.0, (12, 0, 0))))
    a.rot("attack", "leg_l", kf((0.0, (0, 0, 0)), (0.5, (-12, 0, 0)), (1.0, (0, 0, 0))))
    a.rot("attack", "leg_r", kf((0.0, (0, 0, 0)), (0.5, (12, 0, 0)), (1.0, (0, 0, 0))))
    a.clip("cast", 0.8, True)
    a.rot("cast", "body", kf((0.0, (-14, 0, 0)), (0.5, (10, 0, 0)), (0.8, (12, 0, 0))))
    a.pos("cast", "body", kf((0.0, (0, 0, 0)), (0.5, (0, 2, 3)), (0.8, (0, 2.5, 3))))
    a.rot("cast", "arm_l", kf((0.0, (-6, 0, -7)), (0.5, (-175, 0, -10)), (0.8, (-178, 0, -12))))
    a.rot("cast", "arm_r", kf((0.0, (-6, 0, 7)), (0.5, (-175, 0, 10)), (0.8, (-178, 0, 12))))
    a.rot("cast", "head", kf((0.0, (12, 0, 0)), (0.5, (-10, 0, 0)), (0.8, (-12, 0, 0))))
    return B, a


# =============================================================================================================== NO.2 ALFREDO
def make_alfredo():
    B = RBody("alfredo", 128)
    m = B.m
    # about 41 px tall: dark-skinned, bound in straps and chains, armoured, hooded with a visor; a ring of blue-white spirit light turns round him.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (4, 15, 0))
    m.bone("leg_r", "root", (-4, 15, 0))
    m.bone("body", "root", (0, 15, 0))
    m.bone("chest", "body", (0, 22, 0))
    m.bone("head", "chest", (0, 33, -1))
    m.bone("arm_l", "chest", (9.5, 31, 0))
    m.bone("fore_l", "arm_l", (9.5, 22, 0))
    m.bone("arm_r", "chest", (-9.5, 31, 0))
    m.bone("fore_r", "arm_r", (-9.5, 22, 0))
    m.bone("aura", "root", (0, 20, 0))

    DARK = [(64, 42, 32), (96, 66, 48), (128, 92, 68)]
    skin = lambda p, s=81: flesh(p, DARK, s, vein=(70, 60, 110), wound=0.03)
    cloth = lambda p, s=82: fabric(p, [(38, 36, 52), (64, 60, 84), (96, 92, 122)], s, tear=0.05, under=(10, 8, 14), hem=True)
    strap = lambda p: leather(p, [(20, 16, 16), (40, 30, 26), (68, 50, 40)], 83)
    plate = lambda p: steel(p, 84, rust=0.08)
    chain = lambda p: steel(p, 85, rust=0.14)
    buckle = lambda p: brass(p, 86)

    c = B.box("leg_l", (lx("l", 4, 6), 8, -3), (6, 7, 6), cloth)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 6.6), 1, -3.3), (6.6, 8, 6.6), plate)                    # greaves
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 8), 4, -3.9), (8, 1.5, 7.8), strap)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 4, 7), 0, -5), (7, 2, 9), plate)
    B.twin("leg_r", c)
    # waist: a thick belt, buckle, tassets
    B.box("body", (-7.5, 15, -4.5), (15, 7, 9), cloth)
    B.box("body", (-8, 18.5, -5), (16, 2.5, 10), strap)
    B.box("body", (-2, 18.3, -5.6), (4, 3, 1), buckle)
    c = B.box("body", (lx("l", 6.5, 4), 12, -4.7), (4, 6, 1), plate)
    B.twin("body", c)
    # chest: bare dark skin bound with crossed straps, a chain, pauldrons
    B.box("chest", (-8, 22, -4.5), (16, 11, 9), skin)
    B.box("chest", (-8.4, 22, -4.9), (4, 11, 9.8), strap)                                    # crossed straps (a wide band each side)
    B.box("chest", (4.4, 22, -4.9), (4, 11, 9.8), strap)
    B.box("chest", (-3, 25, -5.0), (6, 1.5, 1), chain)
    B.box("chest", (-3, 28, -5.0), (6, 1.5, 1), chain)
    B.box("chest", (-1, 23, -5.4), (2, 8, 1), strap)
    B.box("chest", (-1.5, 27.2, -5.9), (3, 3, 1), buckle)
    c = B.box("chest", (lx("l", 11, 7), 31, -4.5), (7, 3, 9), plate)
    B.twin("chest", c)
    c = B.box("chest", (lx("l", 11, 8), 33.5, -4.5), (8, 1, 9), strap)
    B.twin("chest", c)
    B.box("chest", (-4, 33, -3.5), (8, 2, 7), chain)                                          # gorget / neck chains
    # hooded head with a dark visor slit that shines blue-white
    def hood(p):
        if p.face == "front":
            if p.j == 3 and 0 <= p.i <= 7:
                return (8, 8, 12), (*PALE_BLUE, 255)
            if p.j == 2 and 0 <= p.i <= 7 and p.edge > 0:
                return (16, 16, 24), (*PALE_BLUE, 70)
            if p.j == 4 and p.i % 2 == 0:
                return dim(cloth(p, 87)[0], 0.7), (*PALE_BLUE, 60)
        return cloth(p, 88)
    B.box("head", (-4.5, 35, -5), (9, 8, 9), hood)
    B.box("head", (-4, 43, -3.5), (8, 1, 6), cloth)
    B.box("head", (-5, 34, -4.5), (10, 3, 3), cloth)
    B.box("head", (-2.5, 35.5, 3.5), (5, 6, 2.4), cloth)                                       # the hood's drape behind
    # arms: bound upper arms, a heavy vambrace, wrapped fists
    c = B.box("arm_l", (lx("l", 9.5, 6), 22, -3), (6, 9, 6), skin)
    B.twin("arm_r", c)
    c = B.box("arm_l", (lx("l", 9.5, 7), 25, -3.5), (7, 1.5, 7), strap)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 7), 14, -3.5), (7, 8, 7), plate)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 7.6), 17, -3.8), (7.6, 1.5, 7.6), chain)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 7), 7, -3.5), (7, 7, 7), skin)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 9.5, 7.6), 10, -3.8), (7.6, 2, 7.6), lambda p: bandage(p, 89, gap=0.04))
    B.twin("fore_r", c)
    # the spirit aura: six blue-white flames turning round him, glowing in full
    flame = lambda p: (G.shade_face(p, (118, 168, 236)), (*PALE_BLUE, 150))
    for k in range(6):
        an = k * 60.0
        B.box("aura", (-1, 3 + (k % 3) * 8, 11), (2, 5, 1), flame, rotation=(0, an, 0), pivot=(0, 0, 0))

    a = Anim()
    P = dict(lean=-6, head=6, arm_rest=-6, walk=24, run=34, spread=8, bob=0.5)
    biped(a, BONES, P)
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (12, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (12, 0, 0))
    for nm, L in (("idle", 2.4), ("walk", 1.0), ("run", 0.6), ("attack", 1.0), ("cast", 1.0)):
        if nm not in a.clips:
            continue
        a.rot(nm, "aura", {L * k / 8: (0, 360.0 * k / 8 * (1 if nm in ("idle", "attack") else 2), 0) for k in range(9)})
    # attack: a hammering overhead strike with both fists
    a.clip("attack", 1.0, True)
    a.rot("attack", "body", kf((0.0, (-6, 0, 0)), (0.3, (14, 0, 0)), (0.5, (-26, 0, 0)), (0.75, (-14, 0, 0)), (1.0, (-6, 0, 0))))
    a.rot("attack", "arm_l", kf((0.0, (-6, 0, -8)), (0.3, (-170, 0, -8)), (0.5, (30, 0, -4)), (0.75, (10, 0, -6)), (1.0, (-6, 0, -8))))
    a.rot("attack", "arm_r", kf((0.0, (-6, 0, 8)), (0.3, (-170, 0, 8)), (0.5, (30, 0, 4)), (0.75, (10, 0, 6)), (1.0, (-6, 0, 8))))
    a.rot("attack", "fore_l", kf((0.0, (14, 0, 0)), (0.3, (40, 0, 0)), (0.5, (6, 0, 0)), (1.0, (14, 0, 0))))
    a.rot("attack", "fore_r", kf((0.0, (14, 0, 0)), (0.3, (40, 0, 0)), (0.5, (6, 0, 0)), (1.0, (14, 0, 0))))
    a.rot("attack", "head", kf((0.0, (6, 0, 0)), (0.3, (-8, 0, 0)), (0.5, (22, 0, 0)), (1.0, (6, 0, 0))))
    a.rot("attack", "aura", {1.0 * k / 8: (0, 360.0 * k / 8, 0) for k in range(9)})
    # cast (the soul barrier): arms flung wide, the spirit ring spinning up
    a.clip("cast", 1.0, True)
    a.rot("cast", "arm_l", kf((0.0, (-6, 0, -8)), (0.3, (-10, 0, -80)), (1.0, (-10, 0, -84))))
    a.rot("cast", "arm_r", kf((0.0, (-6, 0, 8)), (0.3, (-10, 0, 80)), (1.0, (-10, 0, 84))))
    a.rot("cast", "head", kf((0.0, (6, 0, 0)), (0.3, (-12, 0, 0)), (1.0, (-14, 0, 0))))
    a.rot("cast", "body", kf((0.0, (-6, 0, 0)), (0.3, (6, 0, 0)), (1.0, (8, 0, 0))))
    a.rot("cast", "aura", {1.0 * k / 8: (0, 720.0 * k / 8, 0) for k in range(9)})
    return B, a


# =============================================================================================================== NO.1 CARL
def make_carl():
    B = RBody("carl", 128)
    m = B.m
    # about 34 px tall and thin: long white ornate robes with gold trim, a tall white box mitre with a gem, a dark wild-haired body showing where the robe opens.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (2, 12, 0))
    m.bone("leg_r", "root", (-2, 12, 0))
    m.bone("body", "root", (0, 12, 0))
    m.bone("chest", "body", (0, 17, 0))
    m.bone("head", "chest", (0, 22, 0))
    m.bone("arm_l", "chest", (5.5, 21, 0))
    m.bone("fore_l", "arm_l", (5.5, 14, 0))
    m.bone("arm_r", "chest", (-5.5, 21, 0))
    m.bone("fore_r", "arm_r", (-5.5, 14, 0))
    m.bone("skirt_f", "body", (0, 12, -3.4))
    m.bone("skirt_b", "body", (0, 12, 3.0))

    WHITE = [(176, 176, 172), (222, 220, 212), (250, 248, 240)]
    robe = lambda p, s=91, t=0.03: ornate(p, WHITE, s, tear=t, under=(24, 20, 26))
    robe_in = lambda p: fabric(p, [(120, 118, 120), (172, 170, 170), (210, 208, 204)], 92, tear=0.03, under=(24, 20, 26), hem=True)
    trim = lambda p: gold(p, 93)
    furry = lambda p: hair(p, [(10, 8, 12), (26, 22, 30), (48, 42, 54)], 94)
    skin = lambda p: flesh(p, [(120, 114, 128), (160, 154, 168), (198, 192, 206)], 95, vein=(110, 70, 150))

    # legs: bare dark under the robe, barely seen
    c = B.box("leg_l", (lx("l", 2, 3), 0, -1.5), (3, 12, 3), furry)
    B.twin("leg_r", c)
    # the dark, wild-haired body inside the open robe
    B.box("body", (-3.5, 12, -2.2), (7, 5, 4.4), furry)
    B.box("chest", (-3.8, 17, -2.4), (7.6, 5, 4.8), furry)
    def soul(p):
        return (70, 110, 150), (*VIOLET, 190)
    B.box("chest", (-1.2, 18.5, -2.8), (2.4, 2.4, 1), soul)
    # the robe: back, two front panels leaving the chest open, shoulders (a mantle), a gold collar, stole
    B.box("chest", (-4.5, 12.5, 2.0), (9, 10, 1.2), robe)
    B.box("chest", (-4.8, 12.5, -3.1), (2.6, 10, 1.2), robe)
    B.box("chest", (2.2, 12.5, -3.1), (2.6, 10, 1.2), robe)
    B.box("chest", (-5.5, 21.3, -3.2), (11, 2.2, 6.4), robe)
    B.box("chest", (-5.8, 21.0, -3.6), (11.6, 0.8, 7.2), trim)
    B.box("chest", (-2.7, 13, -3.5), (1, 10, 1), trim)                                         # the gold edges of the open front
    B.box("chest", (1.7, 13, -3.5), (1, 10, 1), trim)
    # skirts (swaying): long, with gold hems
    B.box("skirt_f", (-4.2, 0, -1.0), (4.2, 12, 1.4), robe)
    B.box("skirt_f", (0.0, 0, -1.0), (4.2, 12, 1.4), robe)
    B.box("skirt_f", (-4.4, 0, -1.2), (8.8, 1, 1.8), trim)
    B.box("skirt_b", (-4.6, 0, -0.4), (9.2, 12, 1.4), robe_in)
    c = B.box("body", (lx("l", 4.2, 1.4), 0, -3.2), (1.4, 12, 6.4), robe)
    B.twin("body", c)
    # head: ashen face with glowing violet eyes, wild black hair out from under the mitre, the tall white box mitre with a gold band and a gem
    def face(p):
        c, g = skin(p)
        if p.face == "front":
            if p.j in (2, 3) and p.i in (1, 4):
                return (12, 10, 16), (*VIOLET, 255)
            if p.j == 4 and 1 <= p.i <= 4:
                return THREAD, None
            if p.j == 1 and 0 <= p.i <= 5:
                return dim(c, 0.7), None
        return c, g
    B.box("head", (-3, 22, -3), (6, 6, 6), face)
    for k, (ox, oz, h) in enumerate(((-3.8, -1.6, 5), (-3.8, 0.6, 6), (2.8, -1.6, 5), (2.8, 0.6, 6), (-1.5, 3.2, 6), (0.5, 3.4, 5))):
        B.box("head", (ox, 21.5, oz), (1.2, h, 1.2), furry, rotation=(0, 0, -14 if ox < 0 else 14), pivot=(ox, 26, oz))
    B.box("head", (-3.6, 27.5, -3.6), (7.2, 1.2, 7.2), trim)                                    # the mitre's brim
    B.box("head", (-3.4, 28.5, -2.6), (7, 8, 5), lambda p: ornate(p, WHITE, 96, tear=0.02, under=(30, 24, 30)))
    B.box("head", (-1, 28.5, -3.0), (2, 8, 1), trim)                                           # the gold band down the front
    B.box("head", (-3.6, 35.5, -2.8), (7, 1, 6), trim)
    B.box("head", (-1, 31.6, -3.6), (2, 2, 1), lambda p: glow_gem(p, (150, 110, 255)))
    # arms: wide white sleeves with gold cuffs, bony hands
    c = B.box("arm_l", (lx("l", 5.5, 4), 14, -2), (4, 8, 4), robe)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 5), 6.5, -2.5), (5, 7.5, 5), robe)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 5.4), 6, -2.7), (5.4, 1, 5.4), trim)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 2), 2, -1), (2, 4.2, 2), skin)
    B.twin("fore_r", c)

    a = Anim()
    P = dict(lean=-3, head=3, arm_rest=-6, walk=22, run=34, spread=5, bob=0.35)
    biped(a, BONES, P)
    for nm, L in (("idle", 2.4), ("walk", 1.0), ("run", 0.6)):
        cyc(a, nm, "skirt_f", "rot", L, sw((0, 0, 0), (-(6 if nm != "idle" else 2), 0, 0), 0.3))
        cyc(a, nm, "skirt_b", "rot", L, sw((0, 0, 0), ((6 if nm != "idle" else 2), 0, 0), 0.3))
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (14, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (14, 0, 0))
    a.clip("attack", 0.6, True)
    a.rot("attack", "arm_r", kf((0.0, (-6, 0, 5)), (0.2, (-110, 0, 6)), (0.34, (80, 0, 2)), (0.6, (-6, 0, 5))))
    a.rot("attack", "fore_r", kf((0.0, (10, 0, 0)), (0.2, (60, 0, 0)), (0.34, (0, 0, 0)), (0.6, (10, 0, 0))))
    a.rot("attack", "body", kf((0.0, (-3, 0, 0)), (0.2, (4, 0, 0)), (0.34, (-14, 0, 0)), (0.6, (-3, 0, 0))))
    a.clip("cast", 0.8, True)
    a.rot("cast", "arm_r", kf((0.0, (-6, 0, 5)), (0.25, (95, 0, 12)), (0.8, (100, 0, 14))))
    a.rot("cast", "fore_r", kf((0.0, (10, 0, 0)), (0.25, (8, 0, 0)), (0.8, (6, 0, 0))))
    a.rot("cast", "arm_l", kf((0.0, (-6, 0, -5)), (0.25, (40, 0, -30)), (0.8, (46, 0, -34))))
    a.rot("cast", "body", kf((0.0, (-3, 0, 0)), (0.25, (-8, 0, 0)), (0.8, (-10, 0, 0))))
    a.rot("cast", "head", kf((0.0, (3, 0, 0)), (0.25, (10, 0, 0)), (0.8, (12, 0, 0))))
    return B, a


# =============================================================================================================== NO.0 MICHAEL CAESAL
def make_michael():
    B = RBody("michael", 128)
    m = B.m
    # about 33 px tall and lean: a smooth white featureless mask, a grey ponytail, a blue-grey robe, a red sash, dark belts and bandages on the arms, arms spread wide.
    m.bone("root", None, (0, 0, 0))
    m.bone("leg_l", "root", (2.4, 14, 0))
    m.bone("leg_r", "root", (-2.4, 14, 0))
    m.bone("body", "root", (0, 14, 0))
    m.bone("chest", "body", (0, 19, 0))
    m.bone("head", "chest", (0, 26, 0))
    m.bone("tail", "head", (0, 30, 3.2))
    m.bone("tail2", "tail", (0, 25, 4))
    m.bone("arm_l", "chest", (5.5, 25, 0))
    m.bone("fore_l", "arm_l", (5.5, 18, 0))
    m.bone("arm_r", "chest", (-5.5, 25, 0))
    m.bone("fore_r", "arm_r", (-5.5, 18, 0))
    m.bone("skirt_f", "body", (0, 14, -2.8))
    m.bone("skirt_b", "body", (0, 14, 2.6))
    m.bone("sash", "body", (0, 16, -3.2))

    BLUEGREY = [(52, 62, 78), (82, 96, 116), (116, 130, 150)]
    robe = lambda p, s=101, t=0.03: fabric(p, BLUEGREY, s, tear=t, under=(18, 18, 26), hem=True)
    robe_d = lambda p: fabric(p, [(34, 40, 52), (56, 66, 84), (80, 92, 112)], 102, tear=0.03, under=(14, 14, 20), hem=True)
    red = lambda p: fabric(p, [(96, 14, 24), (150, 26, 38), (196, 46, 52)], 103, tear=0.02, under=(40, 6, 12), dirt=0.6)
    belt = lambda p: leather(p, [(18, 15, 16), (36, 30, 30), (60, 50, 48)], 104)
    wrap = lambda p: bandage(p, 105, cols=((54, 48, 46), (96, 88, 82), (138, 128, 118)), gap=0.04)
    skin = lambda p: flesh(p, [(150, 142, 146), (190, 182, 186), (222, 216, 220)], 106, vein=(120, 100, 160), wound=0.0)
    grey = lambda p: hair(p, [(86, 88, 96), (138, 140, 148), (190, 192, 198)], 107)

    # legs under the robe: dark trousers, wrapped boots
    c = B.box("leg_l", (lx("l", 2.4, 3.6), 0, -1.8), (3.6, 14, 3.6), robe_d)
    B.twin("leg_r", c)
    c = B.box("leg_l", (lx("l", 2.4, 4.2), 0, -2.1), (4.2, 4, 4.2), belt)
    B.twin("leg_r", c)
    # torso
    B.box("body", (-3.8, 14, -2.4), (7.6, 5, 4.8), robe_d)
    B.box("chest", (-4.2, 19, -2.5), (8.4, 7, 5), robe)
    B.box("chest", (-4.5, 24.8, -2.8), (9, 1.4, 5.6), belt)                                    # a collar belt
    B.box("chest", (-4.4, 19, -2.6), (1.0, 6, 5.2), robe_d)                                    # the open robe's dark facings
    B.box("chest", (3.4, 19, -2.6), (1.0, 6, 5.2), robe_d)
    # the red sash round the waist, knotted at the front with two tails
    B.box("body", (-4.2, 15.5, -2.9), (8.4, 3, 5.8), red)
    B.box("sash", (-1.4, 8, -0.6), (1.4, 8, 0.8), red)
    B.box("sash", (0.2, 7, -0.6), (1.4, 9, 0.8), red)
    # the long robe skirt, split at the front (swaying)
    B.box("skirt_f", (-4.4, 2, -1.0), (4.0, 12, 1.2), robe)
    B.box("skirt_f", (0.4, 2, -1.0), (4.0, 12, 1.2), robe)
    B.box("skirt_b", (-4.8, 2, -0.4), (9.6, 12, 1.2), robe_d)
    B.box("skirt_f", (-4.6, 2, -1.2), (9.2, 1, 1.6), belt)
    # head: a smooth white featureless mask, a grey ponytail tied high and falling behind
    B.box("head", (-3, 26, -3.4), (6, 7, 6.4), mask_white)
    B.box("head", (-3.4, 31.6, -0.4), (6.8, 1.6, 3.6), grey)                                    # hair rising over the crown and back
    B.box("head", (-3.2, 26.4, 2.4), (6.4, 5.6, 1.6), grey)
    B.box("head", (-1, 24.8, -1), (2, 1.4, 2), skin)                                           # neck
    B.box("tail", (-1.2, 24, 2.6), (2.4, 6, 2.4), grey)
    B.box("tail2", (-1.0, 17, 3.0), (2, 8, 2), grey)
    B.box("tail2", (-0.6, 11, 3.2), (1.2, 6, 1.2), grey)
    # arms spread wide: robe sleeves, dark belts and bandages on the forearms, pale hands
    c = B.box("arm_l", (lx("l", 5.5, 3.8), 18, -1.9), (3.8, 8, 3.8), robe)
    B.twin("arm_r", c)
    c = B.box("arm_l", (lx("l", 5.5, 4.4), 19.5, -2.2), (4.4, 1.2, 4.4), belt)
    B.twin("arm_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 3.4), 10, -1.7), (3.4, 8, 3.4), wrap)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 4.0), 12.4, -2.0), (4.0, 1.2, 4.0), belt)
    B.twin("fore_r", c)
    c = B.box("fore_l", (lx("l", 5.5, 4.0), 8.4, -2.0), (4.0, 1.2, 4.0), belt)
    B.twin("fore_r", c)

    def palm(p):
        c, g = skin(p)
        if p.face == "bottom":
            g = (*VIOLET, 200)
        return c, g
    c = B.box("fore_l", (lx("l", 5.5, 3), 5, -1.5), (3, 5, 3), palm)
    B.twin("fore_r", c)
    for fx in (0, 1, 2):
        c = B.box("fore_l", (lx("l", 5.5, 3) + 0.3 + fx * 1.0, 2, -1.0), (0.8, 3.2, 0.8), skin)
        B.twin("fore_r", c)

    a = Anim()
    # the arms hang at the sides at rest only a little away from the body in the clips; "idle" holds them wide as in the art (spread 62 degrees)
    P = dict(lean=-2, head=2, arm_rest=0, walk=30, run=48, spread=72, bob=0.4, arm_swing=0.12)
    biped(a, BONES, P)
    for nm, L in (("idle", 2.4), ("walk", 1.0), ("run", 0.6)):
        cyc(a, nm, "skirt_f", "rot", L, sw((0, 0, 0), (-(8 if nm != "idle" else 2), 0, 0), 0.3))
        cyc(a, nm, "skirt_b", "rot", L, sw((0, 0, 0), ((8 if nm != "idle" else 2), 0, 0), 0.3))
        cyc(a, nm, "tail", "rot", L, sw((-16, 0, 0), (6 if nm != "idle" else 3, 3, 0), 0.9))
        cyc(a, nm, "tail2", "rot", L, sw((-6, 0, 0), (8 if nm != "idle" else 4, 4, 0), 1.6))
        cyc(a, nm, "sash", "rot", L, sw((0, 0, 0), (-(10 if nm != "idle" else 3), 0, 4), 0.6))
    for nm in ("walk", "run"):
        L = 1.0 if nm == "walk" else 0.6
        cyc(a, nm, "fore_l", "rot", L, lambda th: (14, 0, 0))
        cyc(a, nm, "fore_r", "rot", L, lambda th: (14, 0, 0))
    # attack: a sweeping double slash, arms swung across and back
    a.clip("attack", 0.6, True)
    a.rot("attack", "body", kf((0.0, (-2, 0, 0)), (0.18, (-6, -24, 0)), (0.34, (-12, 28, 0)), (0.6, (-2, 0, 0))))
    a.rot("attack", "arm_r", kf((0.0, (0, 0, 72)), (0.18, (-100, 0, 24)), (0.34, (60, 0, 8)), (0.6, (0, 0, 72))))
    a.rot("attack", "arm_l", kf((0.0, (0, 0, -72)), (0.18, (-30, 0, -50)), (0.34, (80, 0, -20)), (0.6, (0, 0, -72))))
    a.rot("attack", "fore_r", kf((0.0, (14, 0, 0)), (0.18, (40, 0, 0)), (0.34, (0, 0, 0)), (0.6, (14, 0, 0))))
    a.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.18, (0, 0, 1.5)), (0.34, (0, 0, -3.5)), (0.6, (0, 0, 0))))
    # cast (the soul barrage): the arms lifted overhead and open, the head back, the sash and ponytail lifting
    a.clip("cast", 0.8, True)
    a.rot("cast", "arm_l", kf((0.0, (0, 0, -72)), (0.3, (-150, 0, -30)), (0.8, (-160, 0, -36))))
    a.rot("cast", "arm_r", kf((0.0, (0, 0, 72)), (0.3, (-150, 0, 30)), (0.8, (-160, 0, 36))))
    a.rot("cast", "head", kf((0.0, (2, 0, 0)), (0.3, (-18, 0, 0)), (0.8, (-20, 0, 0))))
    a.rot("cast", "body", kf((0.0, (-2, 0, 0)), (0.3, (8, 0, 0)), (0.8, (10, 0, 0))))
    a.rot("cast", "tail", {0.8 * k / 6: (-44 + 8 * S(TAU * k / 6), 0, 0) for k in range(7)})
    a.rot("cast", "sash", {0.8 * k / 6: (40 + 10 * S(TAU * k / 6), 0, 0) for k in range(7)})
    return B, a


MODELS = {"jimmy": make_jimmy, "david": make_david, "alfredo": make_alfredo, "carl": make_carl, "michael": make_michael}


def build(name):
    B, anim = MODELS[name]()
    B.m.save(anim)
    d = os.path.join(ROOT, "textures", "entity")
    os.makedirs(d, exist_ok=True)
    for stage in range(4):
        img, glow = B.paint(stage)
        suffix = "" if stage == 0 else "_s%d" % stage
        img.save(os.path.join(d, "rades_%s%s.png" % (name, suffix)))
        glow.save(os.path.join(d, "rades_%s%s_glow.png" % (name, suffix)))
    print("%s: %d cubes, texture sheet used %d of %d rows" % (name, B.m.cube_count(), B.sheet.y + B.sheet.row, B.tex))


TARGET_Y = {"jimmy": 0.9, "david": 1.7, "alfredo": 1.3, "carl": 1.05, "michael": 1.05}
DIST = {"jimmy": 5.0, "david": 8.5, "alfredo": 6.5, "carl": 5.5, "michael": 5.5}


def write_scenes():
    """The headless preview scenes (python3 -B tools/render_preview/preview.py rades_david ...): front / back / three-quarter of the base skin and the four clips."""
    import json
    scenes = {}
    for n in MODELS:
        ty, dist = TARGET_Y[n], DIST[n]
        base = {"kind": "geo", "realFiles": ["perf/PerfProbe.java", "perf/PerfSettings.java", "perf/PerfMath.java", "perf/RlCache.java", "perf/VfxQuality.java", "perf/BoundedCache.java", "perf/RollingStat.java"], "model": "rades_" + n, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "figure": False}
        scenes["rades_%s" % n] = dict(base, ages=[0], tile=[420, 420], views=[
            {"name": "front", "yaw": 0, "pitch": 6, "target": [0, ty, 0], "dist": dist},
            {"name": "back", "yaw": 180, "pitch": 6, "target": [0, ty, 0], "dist": dist},
            {"name": "front3q", "yaw": 35, "pitch": 14, "target": [0, ty, 0], "dist": dist}])
        scenes["rades_%s_pose" % n] = dict(base, ages=[0], tile=[520, 520], anim="idle", views=[
            {"name": "side", "yaw": 90, "pitch": 4, "target": [0, ty, 0], "dist": dist},
            {"name": "front3q", "yaw": 35, "pitch": 14, "target": [0, ty, 0], "dist": dist},
            {"name": "back3q", "yaw": 215, "pitch": 10, "target": [0, ty, 0], "dist": dist}])
        for clip in ("idle", "walk", "attack", "cast"):
            if n in ("jimmy",) and clip == "cast":
                continue
            scenes["rades_%s_%s" % (n, clip)] = dict(base, ages=[0, 5, 10, 15], tile=[340, 300], anim=clip, views=[
                {"name": "side", "yaw": 90, "pitch": 4, "target": [0, ty, 0], "dist": dist},
                {"name": "front3q", "yaw": 35, "pitch": 14, "target": [0, ty, 0], "dist": dist}])
    path = os.path.join(HERE, "..", "render_preview", "scenes.d", "rades_undead.json")
    with open(path, "w") as f:
        json.dump(scenes, f, indent=1)
    print("wrote", os.path.relpath(path, os.path.join(HERE, "..", "..")))


if __name__ == "__main__":
    want = sys.argv[1:] or list(MODELS)
    for n in MODELS:
        if n in want:
            build(n)
    write_scenes()
