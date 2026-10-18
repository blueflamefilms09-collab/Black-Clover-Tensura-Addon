"""The 54 plushies of tools/elite/gen_plushies.py: one function per mob, each building a distinct plush with plush_lib.Plush (a big head, a small body, stubby limbs and the mob's own features).

Every function takes nothing and returns a Plush. The standard sitting plush is std(): a body 5 high, a head 7 high that overlaps it, two stub arms and two stub legs stretched forward.
The face is always on the NORTH faces of the head (the front of the plush). All sizes are whole numbers (texture pixels), positions may be halves.
"""
from plush_lib import Plush, hexc, mix, shade

MOBS = {}


def mob(fn):
    MOBS[fn.__name__.replace("m_", "")] = fn
    return fn


def std(pid, base, head=(8, 7, 7), body=(6, 5, 5), head_color=None, body_color=None, arm=(2, 3, 2), arm_color=None, leg=(2, 2, 3), leg_color=None, arms=True, legs=True,
        translucent=False, noise=7, head_y=None):
    P = Plush(pid, base, translucent)
    bw, bh, bd = body
    hw, hh, hd = head
    P.body = P.cube("body", body, (8 - bw / 2, 0, 6), body_color or base, noise=noise)
    hy = bh - 1 if head_y is None else head_y
    P.head = P.cube("head", head, (8 - hw / 2, hy, 4.5), head_color or base, noise=noise)
    if arms:
        aw, ah, ad = arm
        P.cube("arm_l", arm, (8 - bw / 2 - aw + 0.5, bh - ah - 0.5, 6.5), arm_color or body_color or base, noise=noise)
        P.cube("arm_r", arm, (8 + bw / 2 - 0.5, bh - ah - 0.5, 6.5), arm_color or body_color or base, noise=noise)
    if legs:
        lw, lh, ld = leg
        P.cube("leg_l", leg, (8 - bw / 2 + 0.5, 0, 7 - ld), leg_color or base, noise=noise)
        P.cube("leg_r", leg, (8 + bw / 2 - lw - 0.5, 0, 7 - ld), leg_color or base, noise=noise)
    return P


def hgeo(P):
    """The head's min corner and size: (x0, y0, z0, w, h, d)."""
    x, y, z = P.head.pos
    w, h, d = P.head.size
    return x, y, z, w, h, d


def ears(P, color, inner=None, size=(2, 3, 1), out=0.0, drop=0.0, rot=None, tip=None, back=2.5):
    """Two ears on the top corners of the head. `rot` tilts them outward (angle about z); `tip` colours the top row of the front face."""
    x, y, z, w, h, d = hgeo(P)
    ew, eh, ed = size
    for side, ex in (("l", x + 0.5 - out), ("r", x + w - ew - 0.5 + out)):
        r = None
        if rot:
            r = ("z", rot if side == "r" else -rot, (ex + ew / 2, y + h - drop, z + back))
        c = P.cube("ear_" + side, size, (ex, y + h - drop - 0.5, z + back), color, rot=r)
        if inner and ew >= 2 and eh >= 2:
            P.rect(c, "north", 0 if side == "r" else ew - 1, 1, 1, eh - 1, inner)
            P.rect(c, "north", 1 if side == "r" else 0, 1, 1, eh - 1, inner) if ew >= 3 else None
        if tip:
            P.rect(c, "north", 0, 0, ew, 1, tip)
            P.rect(c, "east", 0, 0, ed, 1, tip)
            P.rect(c, "west", 0, 0, ed, 1, tip)
    return P


def snout(P, size, color, nose=None, y=1.5, nostrils=None, x=None):
    hx, hy, hz, hw, hh, hd = hgeo(P)
    w, h, d = size
    c = P.cube("snout", size, (x if x is not None else 8 - w / 2, hy + y, hz - d + 0.5), color)
    if nose:
        P.rect(c, "north", w // 2 - 1, 0, 2, 1, nose)
    if nostrils:
        P.dots(c, "north", [(1, h - 2), (w - 2, h - 2)], nostrils)
    return c


def horns(P, color, size=(1, 2, 1), out=1.0, tip=None, y=0.0):
    hx, hy, hz, hw, hh, hd = hgeo(P)
    for side, x in (("l", hx + 0.5 - out), ("r", hx + hw - size[0] - 0.5 + out)):
        c = P.cube("horn_" + side, size, (x, hy + hh - 0.5 + y, hz + 2.5), color, noise=3)
        if tip:
            P.rect(c, "north", 0, 0, size[0], 1, tip)
    return P


def tail(P, size, color, pos=None, rot=None, tipcolor=None, name="tail"):
    bx, by, bz = P.body.pos
    bw, bh, bd = P.body.size
    pos = pos or (8 - size[0] / 2, 0.5, bz + bd - 0.5)
    c = P.cube(name, size, pos, color, rot=rot)
    if tipcolor:
        P.rect(c, "south", 0, 0, size[0], 1, tipcolor)
        P.rect(c, "up", 0, size[2] - 1, size[0], 1, tipcolor)
    return c


def spots(P, cube, faces, color, count, seed, wmax=2, hmax=2):
    from plush_lib import hash2
    for f in faces:
        rx, ry, rw, rh = cube.rects[f]
        for k in range(count):
            x = int(hash2(k, seed, 1) * (rw - 1))
            y = int(hash2(k, seed, 2) * (rh - 1))
            w = 1 + int(hash2(k, seed, 3) * wmax)
            h = 1 + int(hash2(k, seed, 4) * hmax)
            P.rect(cube, f, x, y, min(w, rw - x), min(h, rh - y), color)


def stripes(P, cube, faces, color, every=3, offset=1, horizontal=True, alpha_mix=1.0):
    for f in faces:
        rx, ry, rw, rh = cube.rects[f]
        for j in range(rh):
            for i in range(rw):
                if ((j if horizontal else i) + offset) % every == 0:
                    old = P.tex.px[rx + i, ry + j]
                    c = mix(old[:3], color, alpha_mix)
                    P.put(cube, f, i, j, c)


def belly(P, color, w=None, h=None):
    c = P.body
    bw, bh, bd = c.size
    P.rect(c, "north", 1, 1, (w or bw - 2), (h or bh - 2), color)


def face(P, eye=(24, 20, 30), style="dot", y=3, gap=None, size=1, mouth=None, mouth_color=(60, 30, 40), mouth_y=5, hi=True, blush=None, lid=None):
    P.eyes(P.head, style, eye, y=y, gap=gap, size=size, hi=hi, lid=lid)
    if mouth:
        P.mouth(P.head, mouth, mouth_color, y=mouth_y)
    if blush:
        P.blush(P.head, blush, y=y + 1)


# ====================================================================================================== the plushies
@mob
def m_allay():
    P = std("allay", hexc("7ED3F2"), head=(8, 7, 7), body=(5, 5, 4), body_color=hexc("5FB6E0"), arm_color=hexc("7ED3F2"), arm=(2, 3, 2))
    face(P, eye=(20, 40, 70), style="wide", y=3, gap=3, mouth="smile", mouth_color=(40, 80, 120), blush=(255, 190, 220))
    for side, x in (("l", 3.0), ("r", 10.0)):
        P.cube("wing_" + side, (3, 5, 1), (x, 2.5, 10.2), hexc("BFEBFF"), rot=("y", -22.5 if side == "l" else 22.5, (x + 1.5, 4, 10.7)), noise=3)
    P.cube("halo", (4, 1, 4), (6, 11.5, 5.5), hexc("FFF6B0"), noise=0)
    return P


@mob
def m_armadillo():
    shell = hexc("9A7B6A")
    P = std("armadillo", hexc("C9A596"), head=(7, 6, 6), body=(7, 5, 6), body_color=shell, arm_color=hexc("C9A596"), leg_color=hexc("C9A596"))
    for f in ("up", "north", "south", "east", "west"):
        stripes(P, P.body, [f], shade(shell, 0.72), every=2, offset=0, horizontal=True)
    face(P, eye=(30, 22, 24), y=3, gap=3, mouth="line", mouth_color=(120, 70, 70))
    snout(P, (3, 2, 2), hexc("D9B5A8"), nose=(110, 70, 70), y=1.0)
    ears(P, hexc("C9A596"), inner=hexc("E8B8B0"), size=(2, 2, 1))
    tail(P, (2, 2, 3), hexc("9A7B6A"), pos=(7, 1, 11.5))
    return P


@mob
def m_axolotl():
    pink, dark = hexc("F6A8C8"), hexc("E2709E")
    P = std("axolotl", pink, head=(9, 5, 7), body=(6, 4, 6), head_y=2.5, arm=(2, 2, 2), leg=(2, 2, 2))
    face(P, eye=(30, 20, 40), style="dot", y=2, gap=5, mouth="smile", mouth_color=(150, 60, 100), mouth_y=3, blush=(255, 120, 150))
    hx, hy, hz, hw, hh, hd = hgeo(P)
    for side, sx, ang in (("l", hx - 2, -22.5), ("r", hx + hw + 1, 22.5)):
        for k in range(3):
            P.cube("gill_%s%d" % (side, k), (1, 3, 1), (sx + (1 if side == "l" else 0) - 0.5 * (1 if side == "l" else -1) * 0, hy + 1 + k * 1.2, hz + 3), dark, rot=("z", ang if side == "r" else -ang, (sx + 0.5, hy + 1, hz + 3.5)), noise=2)
    tail(P, (2, 4, 5), pink, pos=(7, 0.5, 10.5))
    belly(P, hexc("FFD0E2"), h=2)
    return P


@mob
def m_bat():
    brown, mem = hexc("4A3A3E"), hexc("6A545C")
    P = std("bat", brown, head=(6, 6, 6), body=(4, 5, 4), arm=(1, 3, 1), leg=(1, 2, 2), head_y=3.5)
    face(P, eye=(255, 220, 90), style="slit", y=2, gap=3, mouth="fangs", mouth_color=(30, 16, 20), mouth_y=4, hi=False)
    ears(P, brown, inner=hexc("C98A98"), size=(2, 4, 1), out=0.0)
    for side, x, ang in (("l", 0.5, -22.5), ("r", 10.5, 22.5)):
        P.cube("wing_" + side, (5, 1, 6), (x, 2.5, 7.5), mem, rot=("z", ang if side == "r" else -ang, (x + 2.5, 3, 10)), noise=5)
        P.cube("wingbone_" + side, (5, 1, 1), (x, 3.0, 7.5), brown, rot=("z", ang if side == "r" else -ang, (x + 2.5, 3, 10)), noise=2)
    return P


@mob
def m_bee():
    yel, blk = hexc("F4C430"), hexc("2A2218")
    P = std("bee", yel, head=(7, 6, 6), body=(7, 6, 6), arm=(1, 2, 1), leg=(1, 2, 2), arms=True, head_y=3)
    stripes(P, P.body, ["north", "south", "east", "west", "up"], blk, every=3, offset=1, horizontal=False)
    face(P, eye=(24, 20, 30), style="tall", y=2, gap=4, size=2, mouth="smile", mouth_color=(60, 40, 20), mouth_y=4)
    hx, hy, hz, hw, hh, hd = hgeo(P)
    for side, x in (("l", hx + 1.5), ("r", hx + hw - 2.5)):
        P.cube("antenna_" + side, (1, 3, 1), (x, hy + hh - 0.5, hz + 1.5), blk, rot=("x", -22.5, (x + 0.5, hy + hh, hz + 2)), noise=0)
    for side, x, ang in (("l", 4.0, -22.5), ("r", 9.0, 22.5)):
        P.cube("wing_" + side, (3, 1, 5), (x, 6.3, 7), hexc("E8F6FF"), rot=("z", ang if side == "r" else -ang, (x + 1.5, 6.5, 9.5)), noise=2)
    P.cube("sting", (1, 1, 2), (7.5, 1.5, 12), blk, noise=0)
    return P


@mob
def m_blaze():
    yel, ora = hexc("F6C230"), hexc("E8851E")
    P = std("blaze", yel, head=(8, 7, 7), body=(4, 4, 4), body_color=hexc("C86A18"), arms=False, legs=False, head_y=4)
    face(P, eye=(30, 16, 10), style="angry", y=3, gap=4, size=1, mouth="line", mouth_color=(120, 50, 10), mouth_y=5, hi=False)
    for k, (x, z) in enumerate(((2, 6), (12, 6), (2, 10), (12, 10))):
        P.cube("rod%d" % k, (2, 6, 2), (x, 0, z), ora if k % 2 else yel, noise=10)
    P.cube("rod_f", (2, 4, 2), (7, 0, 3.5), yel, noise=10)
    stripes(P, P.head, ["up"], ora, every=3, offset=0, horizontal=False, alpha_mix=0.6)
    return P


@mob
def m_camel():
    sand, dark = hexc("D8B26C"), hexc("B48C4C")
    P = std("camel", sand, head=(7, 7, 7), body=(8, 6, 7), arm=(2, 3, 2), leg=(2, 3, 3), head_y=4)
    face(P, eye=(40, 28, 20), style="dot", y=2, gap=4, size=1, mouth="line", mouth_color=(100, 70, 40), mouth_y=5)
    P.cube("hump_f", (4, 3, 4), (6, 6, 6.5), dark, noise=6)
    P.cube("hump_b", (4, 3, 4), (6, 6, 9.5), dark, noise=6)
    snout(P, (5, 4, 3), hexc("E6C888"), nose=(80, 50, 30), y=0.5)
    ears(P, sand, inner=hexc("E8B8A0"), size=(2, 2, 1), out=-0.5)
    tail(P, (1, 3, 1), dark, pos=(7.5, 1, 13))
    return P


@mob
def m_cat():
    ora, dk = hexc("E8A15A"), hexc("B8662A")
    P = std("cat", ora, head=(8, 7, 7), body=(6, 5, 6), arm=(2, 3, 2), leg=(2, 2, 3))
    stripes(P, P.head, ["up"], dk, every=3, offset=0, horizontal=False)
    stripes(P, P.body, ["up", "east", "west"], dk, every=3, offset=1, horizontal=False)
    face(P, eye=(60, 150, 70), style="slit", y=3, gap=4, size=1, hi=False)
    P.mouth(P.head, "cat", (200, 100, 110), y=5)
    P.rect(P.head, "north", 3, 4, 2, 1, (240, 150, 160))
    for y in (4, 5):
        P.put(P.head, "north", 0, y, (250, 240, 220))
        P.put(P.head, "north", 7, y, (250, 240, 220))
    ears(P, ora, inner=hexc("F2B8B0"), size=(2, 3, 1), tip=dk)
    belly(P, hexc("F8E6C8"), h=3)
    tail(P, (2, 2, 4), ora, pos=(7, 3, 11.5), rot=("x", -22.5, (8, 4, 11.5)), tipcolor=dk)
    return P


@mob
def m_chicken():
    P = std("chicken", hexc("F4F4F0"), head=(6, 6, 6), body=(6, 5, 6), arm=(1, 3, 3), arm_color=hexc("E8E8E2"), leg=(1, 2, 1), leg_color=hexc("E8B02A"), head_y=3.5)
    face(P, eye=(24, 20, 30), style="dot", y=2, gap=4, size=1, hi=False)
    snout(P, (2, 1, 2), hexc("F2B02A"), y=1.5)
    P.cube("wattle", (1, 2, 1), (7.5, 4.5, 2.5), hexc("D83A2A"), noise=0)
    P.cube("comb", (1, 2, 3), (7.5, 10, 5.5), hexc("D83A2A"), noise=0)
    P.cube("feathers", (4, 4, 1), (6, 2, 12), hexc("E8E8E2"), rot=("x", 22.5, (8, 4, 12)))
    return P


@mob
def m_cow():
    wht, blk = hexc("EDEDE8"), hexc("2A2622")
    P = std("cow", wht, head=(8, 7, 7), body=(8, 6, 6), arm=(2, 3, 2), leg=(2, 3, 3), leg_color=hexc("D8D4CC"))
    spots(P, P.body, ["up", "east", "west", "north", "south"], blk, 3, 2, wmax=3, hmax=3)
    spots(P, P.head, ["up", "east", "west"], blk, 2, 5, wmax=3, hmax=3)
    face(P, eye=(24, 20, 24), style="dot", y=2, gap=5, mouth=None)
    snout(P, (6, 3, 2), hexc("E8B6A8"), nostrils=(120, 70, 70), y=0.0)
    horns(P, hexc("D8C79E"), size=(1, 2, 1), out=-0.5)
    ears(P, wht, inner=hexc("E8B6A8"), size=(2, 1, 1), out=1.5, drop=3.5, back=3)
    tail(P, (1, 4, 1), blk, pos=(7.5, 0.5, 12))
    return P


@mob
def m_creeper():
    g1, g2 = hexc("5EBB4E"), hexc("3C8A34")
    P = std("creeper", g1, head=(8, 8, 8), body=(6, 6, 4), arms=False, legs=False, head_y=5, noise=12)
    spots(P, P.head, ["up", "east", "west", "south"], g2, 14, 1, wmax=2, hmax=2)
    spots(P, P.body, ["north", "south", "east", "west"], g2, 10, 2, wmax=2, hmax=2)
    k = (20, 40, 20)
    P.rect(P.head, "north", 1, 2, 2, 2, k)
    P.rect(P.head, "north", 5, 2, 2, 2, k)
    P.rect(P.head, "north", 3, 4, 2, 2, k)
    P.rect(P.head, "north", 2, 5, 1, 2, k)
    P.rect(P.head, "north", 5, 5, 1, 2, k)
    for k2, (x, z) in enumerate(((4.5, 5.0), (9.5, 5.0), (4.5, 8.5), (9.5, 8.5))):
        P.cube("foot%d" % k2, (2, 2, 2), (x - 1, 0, z), g1, noise=12)
    return P


@mob
def m_dolphin():
    bl, wh = hexc("6C8CAA"), hexc("D8E6F0")
    P = std("dolphin", bl, head=(7, 6, 6), body=(6, 5, 7), arms=False, legs=False, head_y=1.5)
    face(P, eye=(14, 20, 30), style="dot", y=2, gap=5, mouth="smile", mouth_color=(40, 60, 80), mouth_y=4)
    snout(P, (3, 2, 4), hexc("7A9AB8"), y=0.5)
    belly(P, wh, h=2)
    P.cube("dorsal", (1, 4, 3), (7.5, 5, 8), bl, rot=("x", 22.5, (8, 5, 9)), noise=3)
    P.cube("fin_l", (4, 1, 3), (1.5, 2, 7), bl, rot=("z", 22.5, (5, 2.5, 8)), noise=3)
    P.cube("fin_r", (4, 1, 3), (10.5, 2, 7), bl, rot=("z", -22.5, (11, 2.5, 8)), noise=3)
    P.cube("fluke", (7, 1, 3), (4.5, 1, 13), bl, noise=3)
    P.cube("tail", (2, 3, 3), (7, 1, 12), bl, noise=3)
    return P


@mob
def m_donkey():
    gry, wht = hexc("8C7B6B"), hexc("E8E0D4")
    P = std("donkey", gry, head=(7, 7, 7), body=(7, 6, 7), arm=(2, 3, 2), leg=(2, 3, 3), head_y=4)
    face(P, eye=(24, 18, 14), style="dot", y=2, gap=5, mouth=None)
    snout(P, (5, 4, 3), wht, nose=(60, 40, 40), nostrils=(120, 90, 80), y=0.0)
    ears(P, gry, inner=wht, size=(2, 5, 1), out=0.5, rot=22.5, tip=hexc("3A2E28"))
    P.cube("mane", (1, 3, 5), (7.5, 9.5, 5.5), hexc("3A2E28"), noise=3)
    tail(P, (1, 4, 1), hexc("3A2E28"), pos=(7.5, 0.5, 13))
    return P


@mob
def m_drowned():
    skin, shirt = hexc("5EA496"), hexc("3E8C9E")
    P = std("drowned", skin, head=(8, 8, 8), body=(6, 6, 4), body_color=shirt, arm=(2, 2, 5), arm_color=skin, leg=(2, 3, 2), leg_color=hexc("2C5A6A"), head_y=5)
    face(P, eye=(120, 240, 230), style="glow", y=3, gap=4, mouth="line", mouth_color=(20, 50, 50), mouth_y=6, hi=False)
    spots(P, P.head, ["north", "up", "east", "west"], hexc("4A8A7C"), 6, 7, wmax=2, hmax=2)
    P.cube("hair", (8, 1, 8), (4, 13, 4.5), hexc("3A5A3A"), noise=6)
    spots(P, P.body, ["north", "east", "west"], hexc("9A7E4A"), 4, 3, wmax=3, hmax=2)
    return P


@mob
def m_elder_guardian():
    stone, spike = hexc("93A89E"), hexc("C9B58A")
    P = std("elder_guardian", stone, head=(10, 9, 10), body=(6, 3, 5), arms=False, legs=False, head_y=2, noise=9)
    # the big eye on the front
    P.rect(P.head, "north", 3, 2, 4, 4, (240, 240, 232))
    P.rect(P.head, "north", 4, 3, 2, 2, (210, 90, 40))
    P.rect(P.head, "north", 4, 3, 1, 1, (20, 10, 10))
    P.rect(P.head, "north", 5, 4, 1, 1, (20, 10, 10))
    spots(P, P.head, ["up", "east", "west", "south"], hexc("6C8C86"), 10, 9, wmax=3, hmax=2)
    hx, hy, hz, hw, hh, hd = hgeo(P)
    for k, (x, y, z) in enumerate(((5.5, 11, 4.5), (3, 9, 8), (11, 9, 8), (3, 4, 6), (12, 4, 6), (5.5, 11, 10), (9, 11, 6.5), (5, 2, 4.5), (10, 2, 4.5), (2, 6.5, 9.5), (12.5, 6.5, 9.5))):
        P.cube("spike%d" % k, (1, 1, 1) if k % 3 else (2, 2, 2), (x, y, z), spike, noise=3)
    for k, (x, z) in enumerate(((3, 12), (7, 12.5), (11, 12))):
        P.cube("fin%d" % k, (2, 4, 1), (x, 3 + (k % 2), z), shade(stone, 0.85), noise=4)
    return P


@mob
def m_ender_dragon():
    blk, prp = hexc("26222C"), hexc("D07CFF")
    P = std("ender_dragon", blk, head=(7, 5, 6), body=(7, 6, 6), arms=False, legs=False, head_y=3.5, noise=5)
    P.eyes(P.head, "glow", prp, y=1, gap=4, size=1)
    snout(P, (4, 2, 3), hexc("33303A"), nostrils=(190, 120, 230), y=0.5)
    hx, hy, hz, hw, hh, hd = hgeo(P)
    for side, x in (("l", hx + 0.5), ("r", hx + hw - 1.5)):
        P.cube("horn_" + side, (1, 3, 1), (x, hy + hh - 0.5, hz + 4.5), hexc("6A6270"), rot=("x", 22.5, (x + 0.5, hy + hh, hz + 4.5)), noise=2)
    for side, x, ang in (("l", 0.0, -22.5), ("r", 9.0, 22.5)):
        P.cube("wing_" + side, (7, 1, 7), (x, 5.5, 7), shade(blk, 1.25), rot=("z", ang if side == "r" else -ang, (x + 3.5, 6, 10)), noise=5)
        P.cube("wing_bone_" + side, (7, 1, 1), (x, 6.0, 7), hexc("5A5462"), rot=("z", ang if side == "r" else -ang, (x + 3.5, 6, 10)), noise=3)
    for k in range(3):
        P.cube("spine%d" % k, (1, 2, 1), (7.5, 6, 6.5 + k * 2), hexc("6A6270"), noise=2)
    P.cube("tail1", (3, 3, 3), (6.5, 0.5, 12), blk, noise=5)
    P.cube("tail_tip", (1, 1, 2), (7.5, 0.5, 14.5), hexc("6A6270"), noise=2)
    return P


@mob
def m_enderman():
    blk, prp = hexc("1C1822"), hexc("E070FF")
    P = std("enderman", blk, head=(6, 5, 6), body=(4, 5, 3), arm=(1, 7, 1), leg=(1, 5, 1), head_y=9.5, noise=3)
    P.head.pos = (5, 9.5, 5)
    P.rect(P.head, "north", 0, 2, 2, 1, (255, 255, 255))
    P.rect(P.head, "north", 4, 2, 2, 1, (255, 255, 255))
    P.rect(P.head, "north", 1, 2, 1, 1, prp)
    P.rect(P.head, "north", 4, 2, 1, 1, prp)
    P.rect(P.head, "north", 2, 4, 2, 1, (60, 20, 80))
    for c in P.cubes:
        if c.name == "body":
            c.pos = (6, 5, 6.5)
        if c.name == "leg_l":
            c.pos = (6.5, 0, 7.5)
        if c.name == "leg_r":
            c.pos = (8.5, 0, 7.5)
        if c.name == "arm_l":
            c.pos = (4.5, 3.5, 7.5)
        if c.name == "arm_r":
            c.pos = (10.5, 3.5, 7.5)
    return P


@mob
def m_evoker():
    skin, robe = hexc("C09A80"), hexc("8E8E94")
    P = std("evoker", skin, head=(8, 7, 7), body=(6, 7, 5), body_color=robe, arm=(6, 2, 2), arm_color=robe, leg=(2, 1, 2), leg_color=robe, head_y=6)
    for c in P.cubes:
        if c.name == "arm_l":
            c.pos = (5, 4.5, 4.2)
        if c.name == "arm_r":
            c.pos = (5, 4.5, 4.2)
    P.cubes = [c for c in P.cubes if c.name != "arm_r"]
    face(P, eye=(70, 110, 90), style="angry", y=3, gap=4, mouth="line", mouth_color=(90, 50, 40), mouth_y=5)
    P.rect(P.head, "north", 1, 2, 6, 1, (60, 40, 36))
    n = P.cube("nose", (2, 3, 2), (7, 8.5, 3), skin, noise=4)
    P.rect(n, "north", 0, 0, 2, 3, shade(skin, 0.92))
    stripes(P, P.body, ["north"], hexc("4A4A52"), every=4, offset=0, horizontal=False)
    P.rect(P.body, "north", 2, 0, 2, 7, hexc("4A4A52"))
    return P


@mob
def m_fox():
    ora, wht = hexc("E0762C"), hexc("F4EFE6")
    P = std("fox", ora, head=(8, 6, 6), body=(5, 5, 6), arm=(2, 3, 2), arm_color=hexc("2A2220"), leg=(2, 2, 3), leg_color=hexc("2A2220"), head_y=3.5)
    face(P, eye=(30, 20, 14), style="slit", y=2, gap=5, hi=False)
    P.rect(P.head, "north", 0, 3, 8, 3, wht)
    P.rect(P.head, "north", 2, 3, 4, 1, ora)
    snout(P, (3, 2, 3), wht, nose=(20, 16, 16), y=0.5)
    ears(P, ora, inner=hexc("2A2220"), size=(2, 3, 1), tip=hexc("2A2220"))
    belly(P, wht, h=3)
    tail(P, (3, 3, 4), ora, pos=(6.5, 1, 11.5), rot=("x", -22.5, (8, 2.5, 11.5)), tipcolor=wht)
    return P


@mob
def m_frog():
    grn, wht = hexc("6AAA4C"), hexc("E8F0D8")
    P = std("frog", grn, head=(9, 4, 7), body=(6, 4, 6), head_y=2.5, arm=(1, 3, 1), leg=(2, 2, 3))
    P.rect(P.head, "north", 1, 2, 7, 1, (50, 90, 40))
    P.cube("eye_l", (3, 3, 3), (4.5, 6.5, 5), grn, noise=5)
    P.cube("eye_r", (3, 3, 3), (8.5, 6.5, 5), grn, noise=5)
    for n in ("eye_l", "eye_r"):
        c = P.named[n]
        P.rect(c, "north", 0, 0, 3, 3, (240, 200, 60))
        P.rect(c, "north", 1, 1, 1, 2, (20, 20, 20))
    belly(P, wht, h=2)
    spots(P, P.body, ["up"], hexc("4A8A36"), 4, 4, wmax=2, hmax=2)
    P.cube("thigh_l", (2, 3, 3), (3.5, 0, 8), grn, noise=5)
    P.cube("thigh_r", (2, 3, 3), (10.5, 0, 8), grn, noise=5)
    return P


@mob
def m_ghast():
    wht = hexc("EDEDEE")
    P = std("ghast", wht, head=(11, 10, 10), body=(1, 1, 1), arms=False, legs=False, head_y=5, noise=5)
    P.cubes = [c for c in P.cubes if c.name != "body"]
    P.head.pos = (2.5, 5, 3)
    k = (50, 40, 50)
    P.rect(P.head, "north", 2, 4, 2, 1, k)
    P.rect(P.head, "north", 7, 4, 2, 1, k)
    P.put(P.head, "north", 1, 5, (150, 200, 255))
    P.put(P.head, "north", 10, 5, (150, 200, 255))
    P.rect(P.head, "north", 4, 7, 3, 2, (200, 90, 90))
    for i in range(3):
        for j in range(3):
            P.cube("tent_%d%d" % (i, j), (1, 4 - (i + j) % 2, 1), (4 + i * 3, 1 + (i + j) % 2, 5 + j * 2.5), shade(wht, 0.93), noise=4)
    return P


@mob
def m_glow_squid():
    tl, gl = hexc("2C7488"), hexc("7EF0D8")
    P = std("glow_squid", tl, head=(7, 8, 7), body=(1, 1, 1), arms=False, legs=False, head_y=4, noise=6)
    P.cubes = [c for c in P.cubes if c.name != "body"]
    P.head.pos = (4.5, 4, 5)
    spots(P, P.head, ["north", "east", "west", "up", "south"], gl, 9, 11, wmax=2, hmax=2)
    P.rect(P.head, "north", 0, 3, 2, 2, (240, 250, 255))
    P.rect(P.head, "north", 5, 3, 2, 2, (240, 250, 255))
    P.rect(P.head, "north", 0, 4, 1, 1, (10, 30, 40))
    P.rect(P.head, "north", 6, 4, 1, 1, (10, 30, 40))
    for k in range(8):
        a = k * 45
        import math
        x = 8 + math.cos(math.radians(a)) * 2.6 - 0.5
        z = 8.5 + math.sin(math.radians(a)) * 2.6 - 0.5
        P.cube("tent%d" % k, (1, 4, 1), (x, 0.2 + (k % 2) * 0.8, z), shade(tl, 1.1), noise=4)
        P.put(P.named["tent%d" % k], "north", 0, 1, gl)
    return P


@mob
def m_goat():
    wht, hrn = hexc("E6E1D8"), hexc("C9BE9C")
    P = std("goat", wht, head=(7, 7, 7), body=(7, 6, 6), arm=(2, 3, 2), leg=(2, 3, 3), head_y=4)
    face(P, eye=(220, 190, 70), style="wide", y=3, gap=5, hi=False)
    P.rect(P.head, "north", 1, 3, 1, 1, (20, 20, 20))
    P.rect(P.head, "north", 5, 3, 1, 1, (20, 20, 20))
    snout(P, (4, 3, 2), hexc("D8D0C4"), nose=(70, 60, 60), y=0.5)
    P.cube("beard", (1, 3, 1), (7.5, 4.5, 3), hexc("CFC8BC"), noise=4)
    for side, x in (("l", 4.5), ("r", 10.5)):
        P.cube("horn_%s" % side, (1, 3, 1), (x, 11, 6), hrn, noise=3)
        P.cube("horn2_%s" % side, (1, 1, 2), (x, 13.5, 6.6), hrn, noise=3)
    ears(P, wht, inner=hexc("E8B8B0"), size=(2, 1, 1), out=1.5, drop=3.5, back=3)
    tail(P, (1, 2, 1), wht, pos=(7.5, 3, 12.5))
    return P


@mob
def m_guardian():
    grn, spike = hexc("4E8F8A"), hexc("D8B07A")
    P = std("guardian", grn, head=(9, 8, 8), body=(1, 1, 1), arms=False, legs=False, head_y=3, noise=8)
    P.cubes = [c for c in P.cubes if c.name != "body"]
    P.head.pos = (3.5, 3, 4)
    P.rect(P.head, "north", 3, 2, 3, 3, (240, 240, 232))
    P.rect(P.head, "north", 4, 3, 1, 1, (210, 100, 30))
    spots(P, P.head, ["up", "east", "west", "south"], hexc("3A726E"), 8, 21, wmax=3, hmax=2)
    for k, (x, y, z) in enumerate(((4, 11, 6), (8, 11, 6), (3, 8, 5), (12, 8, 5), (3, 4, 10), (12, 4, 10), (6, 11, 9), (9, 2.5, 5))):
        P.cube("spike%d" % k, (1, 1, 1), (x, y, z), spike, noise=2)
    P.cube("tail", (3, 4, 3), (6.5, 4, 12), shade(grn, 0.9), noise=4)
    P.cube("fin_t", (1, 3, 3), (7.5, 8, 11.5), hexc("3A726E"), noise=4)
    return P


@mob
def m_hoglin():
    brn, pk = hexc("A86C48"), hexc("E0A8A0")
    P = std("hoglin", brn, head=(9, 7, 7), body=(8, 6, 7), arm=(2, 3, 2), leg=(2, 3, 3), head_y=3.5)
    face(P, eye=(30, 20, 20), style="angry", y=2, gap=5, hi=False)
    snout(P, (7, 3, 2), pk, nostrils=(120, 60, 60), y=0.0)
    P.cube("tusk_l", (1, 3, 1), (4.5, 3.5, 2), hexc("F2EEDC"), noise=2)
    P.cube("tusk_r", (1, 3, 1), (10.5, 3.5, 2), hexc("F2EEDC"), noise=2)
    P.cube("ear_l", (3, 1, 3), (0.8, 8, 6), pk, rot=("z", 22.5, (2, 8.5, 7)), noise=4)
    P.cube("ear_r", (3, 1, 3), (12.2, 8, 6), pk, rot=("z", -22.5, (14, 8.5, 7)), noise=4)
    for k in range(4):
        P.cube("mane%d" % k, (2, 2, 1), (7, 11 if k < 2 else 6.5, 6 + (k % 2) * 3 + (0 if k < 2 else 1)), hexc("5A3A28"), noise=4)
    spots(P, P.body, ["north", "east", "west", "up"], hexc("8A5A3A"), 3, 6, wmax=2, hmax=2)
    return P


@mob
def m_horse():
    brn, mane = hexc("9A6A42"), hexc("2E2018")
    P = std("horse", brn, head=(6, 8, 7), body=(7, 6, 7), arm=(2, 3, 2), leg=(2, 3, 3), leg_color=hexc("7A5030"), head_y=4)
    face(P, eye=(24, 16, 12), style="dot", y=2, gap=4, mouth=None)
    P.rect(P.head, "north", 2, 0, 2, 6, hexc("F2EAD8"))
    snout(P, (4, 4, 3), hexc("B58A62"), nostrils=(60, 40, 30), y=0.0)
    ears(P, brn, inner=hexc("E0B8A0"), size=(1, 3, 1), out=0.0, tip=mane)
    P.cube("mane", (1, 8, 3), (7.5, 5, 9.2), mane, noise=3)
    P.cube("mane_h", (1, 3, 4), (7.5, 11, 5.5), mane, noise=3)
    tail(P, (2, 5, 1), mane, pos=(7, 0.5, 13))
    return P


@mob
def m_iron_golem():
    gry, vine = hexc("C2B9A8"), hexc("5A8A3E")
    P = std("iron_golem", gry, head=(6, 5, 6), body=(8, 6, 5), arm=(3, 8, 3), leg=(3, 3, 3), head_y=5.0, noise=8)
    P.head.pos = (5, 9.5, 5)
    P.rect(P.head, "north", 1, 1, 1, 1, (200, 40, 30))
    P.rect(P.head, "north", 4, 1, 1, 1, (200, 40, 30))
    P.rect(P.head, "north", 0, 0, 6, 1, (90, 84, 76))
    n = P.cube("nose", (2, 4, 2), (7, 7.5, 3.2), shade(gry, 0.85), noise=5)
    P.rect(n, "north", 0, 0, 2, 4, shade(gry, 0.8))
    spots(P, P.body, ["north", "east", "west"], vine, 4, 3, wmax=2, hmax=3)
    spots(P, P.named["arm_l"], ["north", "east", "west"], vine, 2, 4, wmax=2, hmax=3)
    for c in P.cubes:
        if c.name == "body":
            c.pos = (4, 3, 6)
        if c.name == "arm_l":
            c.pos = (0.5, 2.5, 6.5)
        if c.name == "arm_r":
            c.pos = (12.5, 2.5, 6.5)
        if c.name == "leg_l":
            c.pos = (5, 0, 6.5)
        if c.name == "leg_r":
            c.pos = (8, 0, 6.5)
    return P


@mob
def m_llama():
    cr, wl = hexc("E6D2A8"), hexc("F4EAD0")
    P = std("llama", cr, head=(4, 4, 5), body=(7, 6, 6), arm=(2, 3, 2), leg=(2, 3, 3), head_y=9, noise=6)
    P.head.pos = (6, 9.5, 5)
    P.cube("neck", (3, 5, 3), (6.5, 5.5, 5.8), cr, noise=6)
    face(P, eye=(24, 18, 14), style="dot", y=1, gap=2, mouth=None)
    snout(P, (3, 2, 2), hexc("F0DEB8"), nose=(80, 60, 50), y=0.2)
    ears(P, cr, inner=hexc("E8B8A8"), size=(1, 3, 1), out=-0.3)
    P.cube("carpet", (7, 1, 6), (4.5, 6, 6), hexc("B42A3A"), noise=2)
    P.rect(P.named["carpet"], "up", 0, 1, 7, 1, (240, 230, 200))
    P.rect(P.named["carpet"], "north", 0, 0, 7, 1, (240, 230, 200))
    tail(P, (1, 2, 1), cr, pos=(7.5, 3, 12.5))
    return P


@mob
def m_magma_cube():
    dk, gl = hexc("3A1612"), hexc("FF9A22")
    P = std("magma_cube", dk, head=(10, 5, 10), body=(10, 5, 10), arms=False, legs=False, head_y=0, noise=10)
    P.head.pos = (3, 4.5, 3)
    P.body.pos = (3, 0, 3)
    spots(P, P.body, ["north", "east", "west", "south"], gl, 6, 31, wmax=3, hmax=1)
    spots(P, P.head, ["up", "east", "west", "south"], hexc("7A2A18"), 6, 41, wmax=3, hmax=2)
    P.rect(P.head, "north", 1, 1, 3, 2, (255, 230, 120))
    P.rect(P.head, "north", 6, 1, 3, 2, (255, 230, 120))
    P.rect(P.head, "north", 2, 3, 6, 1, gl)
    P.rect(P.head, "north", 1, 1, 1, 1, (160, 40, 10))
    P.rect(P.head, "north", 6, 1, 1, 1, (160, 40, 10))
    return P


@mob
def m_mooshroom():
    red, wht = hexc("B02222"), hexc("EEE8E0")
    P = std("mooshroom", red, head=(8, 7, 7), body=(8, 6, 6), arm=(2, 3, 2), leg=(2, 3, 3), leg_color=hexc("8A1818"))
    spots(P, P.body, ["up", "east", "west", "north", "south"], wht, 3, 12, wmax=3, hmax=3)
    spots(P, P.head, ["up", "east", "west"], wht, 2, 13, wmax=2, hmax=2)
    face(P, eye=(24, 20, 24), style="dot", y=2, gap=5)
    snout(P, (6, 3, 2), hexc("E8D0C4"), nostrils=(120, 80, 70), y=0.0)
    horns(P, hexc("D8C79E"), size=(1, 2, 1), out=-0.5)
    for k, (x, z) in enumerate(((5, 7.5), (9.5, 8.5))):
        P.cube("stalk%d" % k, (1, 2, 1), (x + 1, 6, z), hexc("D8CFB0"), noise=3)
        cap = P.cube("cap%d" % k, (3, 1, 3), (x, 8, z - 1), hexc("C03030") if k == 0 else hexc("8A5A3A"), noise=4)
        P.dots(cap, "up", [(0, 0), (2, 1), (1, 2)], (240, 240, 230))
    return P


@mob
def m_ocelot():
    cr, sp = hexc("E8D08A"), hexc("7A5A2A")
    P = std("ocelot", cr, head=(7, 6, 6), body=(5, 5, 7), arm=(2, 3, 2), leg=(2, 2, 3), head_y=3.5)
    spots(P, P.body, ["up", "east", "west"], sp, 7, 51, wmax=2, hmax=2)
    spots(P, P.head, ["up", "east", "west"], sp, 4, 52, wmax=2, hmax=1)
    face(P, eye=(80, 150, 70), style="slit", y=2, gap=4, hi=False)
    P.mouth(P.head, "cat", (190, 110, 110), y=4)
    ears(P, cr, inner=hexc("F2C8B0"), size=(2, 2, 1))
    belly(P, hexc("F8F0DC"), h=3)
    tail(P, (1, 1, 4), cr, pos=(7.5, 2, 12), rot=("x", -22.5, (8, 2.5, 12)), tipcolor=sp)
    return P


@mob
def m_panda():
    wht, blk = hexc("F2F2EE"), hexc("22202A")
    P = std("panda", wht, head=(8, 7, 7), body=(7, 6, 6), arm=(2, 3, 2), arm_color=blk, leg=(2, 3, 3), leg_color=blk, head_y=4)
    P.rect(P.head, "north", 0, 2, 3, 2, blk)
    P.rect(P.head, "north", 5, 2, 3, 2, blk)
    P.put(P.head, "north", 1, 2, (255, 255, 255))
    P.put(P.head, "north", 6, 2, (255, 255, 255))
    snout(P, (4, 3, 2), hexc("F0ECE4"), nose=blk, y=0.5)
    ears(P, blk, size=(2, 2, 1), out=0.2)
    P.rect(P.body, "north", 0, 0, 7, 2, blk)
    return P


@mob
def m_parrot():
    red, blu = hexc("D63A33"), hexc("2E62C8")
    P = std("parrot", red, head=(5, 5, 5), body=(5, 6, 4), arm=(1, 4, 3), arm_color=blu, leg=(1, 1, 1), leg_color=hexc("8A8A82"), head_y=4.5)
    face(P, eye=(24, 20, 20), style="dot", y=1, gap=3, hi=True)
    snout(P, (2, 3, 2), hexc("9A9A94"), y=0.5)
    P.cube("crest", (1, 2, 3), (7.5, 9.5, 5.5), hexc("E85A4A"), noise=3)
    P.cube("tail", (3, 1, 6), (6.5, 2.2, 9.5), blu, rot=("x", 22.5, (8, 2.7, 10)), noise=4)
    P.cube("tail2", (1, 1, 4), (7.5, 2.3, 11.5), hexc("3AC8A0"), rot=("x", 22.5, (8, 2.7, 10)), noise=3)
    return P


@mob
def m_phantom():
    bl, ey = hexc("4A5CA0"), hexc("7BFF9A")
    P = std("phantom", bl, head=(7, 5, 6), body=(6, 4, 7), arms=False, legs=False, head_y=2, noise=6)
    P.eyes(P.head, "glow", ey, y=1, gap=4, size=1)
    P.rect(P.head, "north", 2, 3, 3, 1, (30, 40, 70))
    for side, x, ang in (("l", 0.0, -22.5), ("r", 9.0, 22.5)):
        P.cube("wing_" + side, (7, 1, 6), (x, 3.5, 6.5), hexc("7080C0"), rot=("z", ang if side == "r" else -ang, (x + 3.5, 4, 9.5)), noise=6)
        P.cube("rib_" + side, (7, 1, 1), (x, 4.0, 6.5), hexc("C8D0E8"), rot=("z", ang if side == "r" else -ang, (x + 3.5, 4, 9.5)), noise=2)
    P.cube("tail1", (3, 2, 3), (6.5, 0.5, 13), bl, noise=5)
    P.cube("spike_t", (1, 1, 2), (7.5, 0.5, 14.0), hexc("C8D0E8"), noise=2)
    return P


@mob
def m_pig():
    pk = hexc("F0A8A8")
    P = std("pig", pk, head=(8, 7, 7), body=(7, 6, 6), arm=(2, 3, 2), leg=(2, 3, 3), leg_color=hexc("E09090"), head_y=3.5)
    face(P, eye=(24, 20, 24), style="dot", y=2, gap=5, hi=True)
    snout(P, (4, 3, 1), hexc("F4BDB8"), nostrils=(150, 70, 80), y=0.0)
    ears(P, hexc("E89898"), size=(2, 2, 1), out=0.5, rot=22.5)
    tail(P, (1, 1, 1), hexc("E89898"), pos=(7.5, 3, 12))
    return P


@mob
def m_piglin():
    skin, gold = hexc("C98B6B"), hexc("F2CC40")
    P = std("piglin", skin, head=(8, 6, 6), body=(6, 6, 4), body_color=hexc("5A3A2A"), arm=(2, 4, 2), arm_color=skin, leg=(2, 2, 3), leg_color=hexc("3A2A22"), head_y=4.5)
    face(P, eye=(30, 20, 20), style="dot", y=2, gap=5, hi=False)
    snout(P, (4, 3, 1), hexc("D9A080"), nostrils=(110, 60, 50), y=0.0)
    P.cube("tusk_l", (1, 2, 1), (5.2, 4.2, 3.0), (246, 240, 220), noise=2)
    P.cube("tusk_r", (1, 2, 1), (9.8, 4.2, 3.0), (246, 240, 220), noise=2)
    P.cube("ear_l", (3, 3, 1), (1.2, 7.8, 6.5), skin, rot=("z", 22.5, (3, 9, 7)), noise=4)
    P.cube("ear_r", (3, 3, 1), (11.8, 7.8, 6.5), skin, rot=("z", -22.5, (13, 9, 7)), noise=4)
    P.rect(P.body, "north", 0, 0, 6, 1, gold)
    P.rect(P.body, "north", 2, 1, 2, 2, gold)
    P.cube("loin", (4, 3, 1), (6, 0.5, 5.8), gold, noise=3)
    return P


@mob
def m_polar_bear():
    wht = hexc("F0F0F2")
    P = std("polar_bear", wht, head=(7, 6, 6), body=(8, 6, 7), arm=(3, 3, 2), leg=(3, 2, 3), leg_color=hexc("DCDCE0"), head_y=4)
    face(P, eye=(24, 24, 30), style="dot", y=2, gap=4)
    snout(P, (4, 3, 3), hexc("F8F8F6"), nose=(20, 20, 24), y=0.3)
    ears(P, hexc("E8E8EC"), size=(2, 2, 1), out=-0.2)
    tail(P, (2, 2, 1), hexc("E8E8EC"), pos=(7, 2, 13))
    return P


@mob
def m_rabbit():
    brn, wht = hexc("A98462"), hexc("F4EEE6")
    P = std("rabbit", brn, head=(6, 6, 6), body=(5, 5, 6), arm=(1, 3, 1), leg=(2, 3, 3), head_y=3.5)
    face(P, eye=(24, 18, 20), style="dot", y=2, gap=4, hi=True)
    P.rect(P.head, "north", 2, 4, 2, 1, (240, 150, 160))
    P.dots(P.head, "north", [(2, 5), (3, 5)], (250, 250, 245))
    ears(P, brn, inner=hexc("F2B8B0"), size=(2, 6, 1), out=-0.3, back=3, rot=22.5)
    belly(P, wht, h=3)
    tail(P, (3, 3, 2), wht, pos=(6.5, 1.5, 12))
    P.cube("haunch_l", (2, 3, 4), (4, 0, 8), brn, noise=5)
    P.cube("haunch_r", (2, 3, 4), (10, 0, 8), brn, noise=5)
    return P


@mob
def m_sheep():
    wool, skin = hexc("F2F0EA"), hexc("C8B8A4")
    P = std("sheep", skin, head=(6, 6, 6), body=(9, 7, 7), body_color=wool, arm=(2, 3, 2), arm_color=skin, leg=(2, 3, 3), leg_color=skin, head_y=3.5, noise=9)
    face(P, eye=(30, 24, 24), style="dot", y=2, gap=4, hi=True)
    P.cube("cap", (7, 2, 7), (4.5, 9.5, 4), wool, noise=9)
    ears(P, skin, size=(2, 1, 1), out=1.5, drop=3.5, back=3)
    snout(P, (3, 2, 1), hexc("D8C8B4"), nose=(90, 70, 70), y=0.5)
    P.cube("wool_b", (9, 2, 3), (3.5, 6, 8.5), wool, noise=9)
    return P


@mob
def m_shulker():
    pr, lid = hexc("8A5A94"), hexc("A87CB0")
    P = std("shulker", pr, head=(4, 3, 4), body=(10, 5, 10), arms=False, legs=False, head_y=4, noise=6)
    P.body.pos = (3, 0, 3)
    P.head.pos = (6, 5, 4)
    P.rect(P.head, "north", 0, 0, 4, 3, (232, 214, 160))
    P.rect(P.head, "north", 1, 1, 2, 1, (60, 40, 70))
    P.rect(P.head, "north", 1, 1, 1, 1, (240, 240, 120))
    c = P.cube("lid", (10, 5, 10), (3, 7, 3), lid, noise=6)
    stripes(P, P.named["lid"], ["north", "south", "east", "west"], shade(lid, 0.82), every=5, offset=2, horizontal=True)
    for f in ("up",):
        P.rect(c, f, 4, 4, 2, 2, shade(lid, 0.82))
    stripes(P, P.body, ["north", "south", "east", "west"], shade(pr, 0.82), every=5, offset=0, horizontal=True)
    return P


@mob
def m_skeleton():
    bone, dk = hexc("D2D2CA"), hexc("22222A")
    P = std("skeleton", bone, head=(8, 8, 8), body=(6, 6, 4), arm=(1, 5, 1), leg=(1, 3, 1), head_y=5, noise=5)
    P.rect(P.head, "north", 1, 3, 2, 2, dk)
    P.rect(P.head, "north", 5, 3, 2, 2, dk)
    P.rect(P.head, "north", 2, 6, 4, 1, dk)
    for x in (3, 5):
        P.put(P.head, "north", x, 6, bone)
    P.rect(P.head, "north", 3, 5, 2, 1, shade(bone, 0.8))
    stripes(P, P.body, ["north"], dk, every=2, offset=1, horizontal=True)
    P.rect(P.body, "north", 2, 0, 2, 6, shade(bone, 0.7))
    return P


@mob
def m_slime():
    inner, outer = hexc("4CA23A"), hexc("7EDB66")
    P = std("slime", inner, head=(6, 5, 6), body=(6, 4, 6), arms=False, legs=False, head_y=0, translucent=True, noise=6)
    P.head.pos = (5, 2.0, 5)
    P.body.pos = (5, 1.0, 5)
    P.cubes = [c for c in P.cubes if c.name != "body"]
    P.rect(P.head, "north", 1, 1, 1, 2, (24, 50, 24))
    P.rect(P.head, "north", 4, 1, 1, 2, (24, 50, 24))
    P.rect(P.head, "north", 2, 3, 2, 1, (24, 50, 24))
    shell = P.cube("shell", (10, 9, 10), (3, 0, 3), outer, noise=6)
    # the shell is see-through: lower alpha on every pixel of it
    for f, (rx, ry, rw, rh) in shell.rects.items():
        for j in range(rh):
            for i in range(rw):
                r, g, b, a = P.tex.px[rx + i, ry + j]
                edge = i in (0, rw - 1) or j in (0, rh - 1)
                P.tex.px[rx + i, ry + j] = (r, g, b, 190 if edge else 120)
    return P


@mob
def m_sniffer():
    red, moss = hexc("8E3C2C"), hexc("5E8A3E")
    P = std("sniffer", red, head=(8, 6, 7), body=(10, 7, 8), arm=(3, 3, 3), leg=(3, 3, 3), head_y=3, noise=6)
    P.body.pos = (3, 0, 6)
    P.head.pos = (4, 3.0, 3.5)
    spots(P, P.body, ["up"], moss, 18, 61, wmax=3, hmax=3)
    P.rect(P.body, "up", 1, 1, 8, 5, moss)
    for c in P.cubes:
        if c.name == "arm_l":
            c.pos = (0.8, 0.0, 6.5)
        if c.name == "arm_r":
            c.pos = (12.2, 0.0, 6.5)
    face(P, eye=(240, 220, 200), style="dot", y=2, gap=5, hi=False)
    P.rect(P.head, "north", 1, 2, 1, 1, (20, 20, 20))
    P.rect(P.head, "north", 6, 2, 1, 1, (20, 20, 20))
    snout(P, (8, 3, 3), hexc("D8A090"), nostrils=(120, 60, 60), y=0.0)
    ears(P, hexc("B05A48"), inner=hexc("E8B0A0"), size=(2, 3, 1), out=1.0, rot=22.5)
    P.cube("plant", (2, 2, 2), (5, 7.0, 8), hexc("7AB84A"), noise=6)
    return P


@mob
def m_spider():
    blk, rd = hexc("2E2622"), hexc("E82020")
    P = std("spider", blk, head=(6, 5, 6), body=(6, 4, 7), arms=False, legs=False, head_y=1.5, noise=5)
    P.body.pos = (5, 1.5, 7)
    P.head.pos = (5, 2.0, 3.5)
    for (x, y) in ((1, 1), (2, 1), (4, 1), (5, 1), (1, 2), (4, 2)):
        P.put(P.head, "north", x, y, rd)
    P.rect(P.head, "north", 2, 3, 2, 1, (30, 10, 10))
    P.cube("fang_l", (1, 2, 1), (5.7, 2.2, 2.9), hexc("8A8A82"), noise=2)
    P.cube("fang_r", (1, 2, 1), (9.3, 2.2, 2.9), hexc("8A8A82"), noise=2)
    spots(P, P.body, ["up"], hexc("6A2A22"), 3, 71, wmax=2, hmax=2)
    for k in range(4):
        z = 4.5 + k * 1.8
        P.cube("legl%d" % k, (5, 1, 1), (0.0, 2.0, z), hexc("3A302A"), rot=("z", -22.5, (5, 2.5, z + 0.5)), noise=3)
        P.cube("legr%d" % k, (5, 1, 1), (11.0, 2.0, z), hexc("3A302A"), rot=("z", 22.5, (11, 2.5, z + 0.5)), noise=3)
    return P


@mob
def m_squid():
    bl, lt = hexc("30508A"), hexc("5A7CB4")
    P = std("squid", bl, head=(7, 9, 7), body=(1, 1, 1), arms=False, legs=False, head_y=4, noise=6)
    P.cubes = [c for c in P.cubes if c.name != "body"]
    P.head.pos = (4.5, 4, 5)
    P.rect(P.head, "north", 0, 3, 2, 2, (240, 240, 240))
    P.rect(P.head, "north", 5, 3, 2, 2, (240, 240, 240))
    P.rect(P.head, "north", 0, 4, 1, 1, (14, 20, 40))
    P.rect(P.head, "north", 6, 4, 1, 1, (14, 20, 40))
    P.rect(P.head, "north", 2, 0, 3, 9, lt)
    import math
    for k in range(8):
        a = k * 45
        x = 8 + math.cos(math.radians(a)) * 2.6 - 0.5
        z = 8.5 + math.sin(math.radians(a)) * 2.6 - 0.5
        P.cube("tent%d" % k, (1, 5, 1), (x, 0, z), lt, noise=4)
    return P


@mob
def m_strider():
    red, hair = hexc("B83232"), hexc("D8A460")
    P = std("strider", red, head=(8, 6, 6), body=(8, 6, 6), body_color=hexc("A82A2A"), arm=(1, 4, 1), leg=(2, 4, 2), leg_color=hexc("8A2A28"), head_y=3.5)
    face(P, eye=(30, 20, 20), style="dot", y=2, gap=5, hi=False)
    P.rect(P.head, "north", 1, 4, 6, 1, (60, 20, 20))
    for k in range(5):
        P.cube("hair%d" % k, (1, 3, 1), (4.5 + k * 1.5, 11 - (k % 2) * 0.5, 6 + (k % 3)), hair, noise=5)
    for k in range(3):
        P.cube("bristle%d" % k, (1, 2, 1), (4.5 + k * 3, 6, 7.5), hair, noise=5)
    return P


@mob
def m_turtle():
    shell, skin = hexc("4A8A3A"), hexc("8AC070")
    P = std("turtle", skin, head=(4, 3, 4), body=(9, 3, 9), body_color=shell, arms=False, legs=False, head_y=1, noise=7)
    P.body.pos = (3.5, 2, 4)
    P.head.pos = (6, 2, 1.5)
    for f in ("up",):
        for k in range(3):
            P.rect(P.body, f, 1 + k * 3, 1, 1, 7, shade(shell, 0.7))
            P.rect(P.body, f, 1, 1 + k * 3, 7, 1, shade(shell, 0.7))
    P.cube("belly", (9, 2, 9), (3.5, 0, 4), hexc("D8C890"), noise=6)
    P.eyes(P.head, "dot", (24, 24, 20), y=1, gap=2)
    P.rect(P.head, "north", 1, 2, 2, 1, (60, 90, 40))
    for k, (x, z, w, d) in enumerate(((1.5, 3.0, 3, 2), (11.5, 3.0, 3, 2), (1.5, 11.0, 3, 2), (11.5, 11.0, 3, 2))):
        P.cube("flipper%d" % k, (w, 1, d), (x, 0.5, z), skin, noise=5)
    return P


@mob
def m_vex():
    bl, ey = hexc("8EA6D8"), hexc("FF4040")
    P = std("vex", bl, head=(5, 5, 5), body=(3, 4, 3), arm=(1, 3, 1), leg=(1, 3, 1), head_y=3, noise=5)
    P.eyes(P.head, "angry", ey, y=1, gap=3, size=1)
    P.rect(P.head, "north", 1, 3, 3, 1, (60, 70, 120))
    for side, x, ang in (("l", 2.0, -22.5), ("r", 10.0, 22.5)):
        P.cube("wing_" + side, (4, 1, 6), (x, 4, 8), hexc("D8E8FF"), rot=("z", ang if side == "r" else -ang, (x + 2, 4.5, 11)), noise=4)
    P.cube("sword", (1, 6, 1), (11.0, 1.0, 5.0), hexc("A8A8B4"), noise=3)
    P.cube("guard", (3, 1, 1), (10, 2.5, 5.0), hexc("6A6A78"), noise=3)
    return P


@mob
def m_villager():
    skin, robe = hexc("C9967A"), hexc("6B4A32")
    P = std("villager", skin, head=(8, 7, 7), body=(6, 6, 5), body_color=robe, arm=(6, 2, 2), arm_color=robe, leg=(2, 1, 2), leg_color=robe, head_y=5)
    P.cubes = [c for c in P.cubes if c.name != "arm_r"]
    for c in P.cubes:
        if c.name == "arm_l":
            c.pos = (5, 3.5, 4.2)
    face(P, eye=(60, 120, 70), style="wide", y=3, gap=4, hi=False)
    P.rect(P.head, "north", 0, 2, 8, 1, (70, 44, 32))
    n = P.cube("nose", (2, 4, 2), (7, 6.5, 2.8), skin, noise=4)
    P.rect(n, "north", 0, 0, 2, 4, shade(skin, 0.92))
    P.rect(P.body, "north", 2, 0, 2, 6, shade(robe, 0.75))
    return P


@mob
def m_warden():
    blk, bone, cy = hexc("1C262A"), hexc("BDB89A"), hexc("3AD8D0")
    P = std("warden", blk, head=(8, 6, 7), body=(9, 7, 6), arm=(3, 7, 3), leg=(3, 3, 3), head_y=5, noise=6)
    P.body.pos = (3.5, 3, 6)
    P.head.pos = (4, 9.5, 4.5)
    for c in P.cubes:
        if c.name == "arm_l":
            c.pos = (0.2, 2.5, 6.5)
        if c.name == "arm_r":
            c.pos = (12.8, 2.5, 6.5)
        if c.name == "leg_l":
            c.pos = (4.5, 0, 6.5)
        if c.name == "leg_r":
            c.pos = (8.5, 0, 6.5)
    P.rect(P.head, "north", 1, 2, 2, 1, (10, 16, 18))
    P.rect(P.head, "north", 5, 2, 2, 1, (10, 16, 18))
    P.rect(P.head, "north", 2, 4, 4, 1, (10, 16, 18))
    P.rect(P.body, "north", 3, 2, 3, 3, hexc("2A3A3E"))
    P.rect(P.body, "north", 4, 3, 1, 1, cy)
    P.cube("plate_l", (4, 2, 4), (3.0, 10.0, 6.5), bone, noise=4)
    P.cube("plate_r", (4, 2, 4), (9.0, 10.0, 6.5), bone, noise=4)
    for side, x, ang in (("l", 0.8, -22.5), ("r", 11.2, 22.5)):
        c = P.cube("tendril_" + side, (4, 4, 1), (x, 10.5, 7.5), shade(blk, 1.2), rot=("z", ang if side == "r" else -ang, (x + 2, 12.5, 8)), noise=4)
        P.rect(c, "north", 1, 1, 2, 2, cy)
    P.cube("sensor", (2, 1, 2), (7, 15.5, 6.5), hexc("2A3A3E"), noise=3)
    return P


@mob
def m_witch():
    skin, hat, robe = hexc("7BA060"), hexc("3A2A5A"), hexc("5A3A78")
    P = std("witch", skin, head=(8, 7, 7), body=(6, 5, 5), body_color=robe, arm=(6, 2, 2), arm_color=robe, leg=(2, 1, 2), leg_color=robe, head_y=4)
    P.cubes = [c for c in P.cubes if c.name != "arm_r"]
    for c in P.cubes:
        if c.name == "arm_l":
            c.pos = (5, 2.5, 4.2)
    face(P, eye=(60, 20, 40), style="angry", y=3, gap=4, hi=False)
    n = P.cube("nose", (2, 3, 2), (7, 5.8, 2.8), skin, noise=4)
    P.put(n, "north", 0, 2, (90, 60, 40))
    P.put(n, "north", 1, 2, (90, 60, 40))
    P.put(P.head, "north", 1, 5, (60, 90, 40))
    P.cube("brim", (12, 1, 12), (2, 10.4, 2.5), hat, noise=4)
    P.cube("hat1", (8, 2, 8), (4, 11.4, 4.5), hat, noise=4)
    P.cube("hat2", (6, 2, 6), (5, 13.2, 5.3), hat, rot=("x", 22.5, (8, 13.2, 8)), noise=4)
    P.cube("hat3", (3, 1, 3), (6.5, 14.8, 7.6), shade(hat, 1.1), rot=("x", 22.5, (8, 14.8, 9)), noise=4)
    P.rect(P.named["hat1"], "north", 0, 0, 8, 1, hexc("D8B040"))
    P.cube("hair", (2, 3, 1), (4.2, 7.2, 10.6), hexc("C0452A"), noise=4)
    return P


@mob
def m_wither():
    blk, bone = hexc("2A2A30"), hexc("8A8A92")
    P = std("wither", blk, head=(6, 6, 6), body=(2, 7, 2), arms=False, legs=False, head_y=8, noise=5)
    P.body.pos = (7, 0, 8)
    P.head.pos = (5, 7.5, 5.5)
    for k, y in enumerate((2.0, 3.8, 5.6)):
        P.cube("rib%d" % k, (6 if k < 2 else 4, 1, 2), (5 if k < 2 else 6, y, 8), bone, noise=4)
    P.rect(P.head, "north", 0, 2, 2, 2, (10, 10, 14))
    P.rect(P.head, "north", 4, 2, 2, 2, (10, 10, 14))
    P.put(P.head, "north", 0, 2, (230, 230, 240))
    P.put(P.head, "north", 5, 2, (230, 230, 240))
    P.rect(P.head, "north", 1, 5, 4, 1, (10, 10, 14))
    for side, x in (("l", 0.5), ("r", 11.5)):
        c = P.cube("head_" + side, (4, 4, 4), (x, 8.5, 6), blk, noise=5)
        P.rect(c, "north", 0, 1, 1, 1, (10, 10, 14))
        P.rect(c, "north", 3, 1, 1, 1, (10, 10, 14))
        P.rect(c, "north", 1, 3, 2, 1, (10, 10, 14))
    P.cube("tail", (2, 3, 2), (7, 0, 10), bone, noise=4)
    return P


@mob
def m_wolf():
    gry, lt = hexc("C4C4C0"), hexc("E8E8E4")
    P = std("wolf", gry, head=(7, 6, 6), body=(5, 5, 7), arm=(2, 3, 2), leg=(2, 2, 3), head_y=3.5)
    face(P, eye=(70, 50, 20), style="dot", y=2, gap=4, hi=False)
    snout(P, (3, 3, 3), lt, nose=(20, 20, 24), y=0.3)
    ears(P, gry, inner=hexc("A07070"), size=(2, 2, 1), tip=hexc("5A5A60"))
    belly(P, lt, h=3)
    tail(P, (2, 2, 4), gry, pos=(7, 2, 12.0), rot=("x", -22.5, (8, 3, 12)), tipcolor=hexc("5A5A60"))
    return P


@mob
def m_zombie():
    skin, shirt = hexc("6F9E58"), hexc("34A2B8")
    P = std("zombie", skin, head=(8, 8, 8), body=(6, 6, 4), body_color=shirt, arm=(2, 2, 5), arm_color=skin, leg=(2, 3, 2), leg_color=hexc("3A3A8A"), head_y=5, noise=10)
    P.rect(P.head, "north", 1, 3, 2, 2, (20, 30, 20))
    P.rect(P.head, "north", 5, 3, 2, 2, (20, 30, 20))
    P.rect(P.head, "north", 2, 6, 4, 1, (40, 60, 30))
    spots(P, P.head, ["north", "up", "east", "west"], hexc("5A8A46"), 6, 81, wmax=2, hmax=2)
    P.put(P.head, "north", 3, 5, hexc("4A7A38"))
    spots(P, P.body, ["north"], hexc("2A8294"), 3, 82, wmax=3, hmax=2)
    return P
