"""The 3.26.0 Skill Book: geometry, texture layout, display transforms and the model overrides (look 2, "the painted grimoires").

Shared by gen_book_textures_v2.py (paints the textures and writes the model files) and preview_book.py (renders them back), so the model, the atlases the generator paints and the picture
the previewer shows can never disagree about where a face looks in a texture. Pure python (no numpy / PIL), so the tests of the owner's pipeline can import it anywhere.

One book shape for all 55 looks (11 franchises x 5 rarities); the franchise decides the COVER texture (and a few accent pieces), the rarity decides the TRIM (corners, strap, gems, chain, claws,
page edge). Model space is 16 units = one block, the book is 9 wide x 13 high x 4 thick like the 1.82.0 book (X 3.5..12.5, Y 1.5..14.5, Z 6..10), the front cover looks SOUTH (+Z), the spine is
on the WEST (-X) side, the fore-edge (pages) on the EAST. Every model keeps to MAX_ELEMENTS elements.

Texture variables of a model:  #cover  the franchise's cover atlas (512 x 256: front, back, spine, edges, emblem plaque, leather, accent metal, slab)
                               #fx     the franchise's overlay atlas (288 x 208: raised relief ornaments with alpha, emissive glow layer with alpha)
                               #trim   the rarity's trim atlas (256 x 256: metal, strap, cloth, gems, chain, claw, fang, page edges, inlay)
                               #glow   the animated emissive page edge of the mythic books (32 x 32 frames)
Tint index 1 (the line's colour) is used by exactly three things: the ribbon, the emblem plaque and the small gem on the clasp strap.
"""
import math

# --------------------------------------------------------------------------------------------------------------------------------- names and ids
RARITIES = ["common", "uncommon", "rare", "epic", "mythic"]            # the rank is the index; the item property is (rank + 1) / 6
RARITY_STEPS = 6.0
PROP_STEPS = 20.0                                                      # nusmp:franchise = (index + 1) / PROP_STEPS, see gen_book_index.py
MAX_ELEMENTS = 140
LEGACY_FRANCHISE_COUNT = 10                                            # the franchises that have a 1.82.0 grey cover (fairy_tail came in 3.26.0 and has none)

# the unit of the model's uv space: 0..16 over a whole texture
BX0, BX1, BY0, BY1, BZ0, BZ1 = 3.5, 12.5, 1.5, 14.5, 6.0, 10.0         # the book's box
P1 = 0.625                                                             # the front panel's inset from the board edge (10 px)
P2 = 1.25                                                              # the second, higher panel's inset (20 px)
Z_FRONT, Z_P1, Z_P2 = 10.0, 10.2, 10.4                                 # the front surface of the board, of panel 1, of panel 2
Z_BACK = 6.0
PLAQUE = (8.0, 10.75)                                                  # centre of the tinted emblem plaque (x, y)
CENTRE = (8.0, 7.0)                                                    # centre of the cover motif and the rarity gems
LOW = (8.0, 4.1)                                                       # centre of the franchise accent at the foot of the cover


class Atlas:
    """A texture atlas: a pixel size and named pixel rectangles; uv() gives the model's 0..16 uv of a rectangle or of a sub rectangle of it (px, relative to the region)."""

    def __init__(self, w, h, regions):
        self.w, self.h, self.regions = w, h, regions

    def rect(self, name, sub=None):
        x0, y0, x1, y1 = self.regions[name]
        if sub is not None:
            sx0, sy0, sx1, sy1 = sub
            x0, y0, x1, y1 = x0 + sx0, y0 + sy0, x0 + sx1, y0 + sy1
        return x0, y0, x1, y1

    def uv(self, name, sub=None, flip_u=False, flip_v=False):
        x0, y0, x1, y1 = self.rect(name, sub)
        u0, v0, u1, v1 = x0 * 16.0 / self.w, y0 * 16.0 / self.h, x1 * 16.0 / self.w, y1 * 16.0 / self.h
        if flip_u:
            u0, u1 = u1, u0
        if flip_v:
            v0, v1 = v1, v0
        return [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)]


# ---- the franchise cover atlas: 16 px per model unit on the cover
COVER = Atlas(512, 256, {
    "front": (0, 0, 144, 208),        # 9 x 13 units
    "back": (144, 0, 288, 208),       # 9 x 13 units
    "spine": (288, 0, 352, 208),      # 4 x 13 units
    "edgeV": (352, 0, 368, 208),      # 1 x 13 units: the boards' fore-edge
    "edgeH": (368, 0, 512, 16),       # 9 x 1 unit: the boards' top edge
    "edgeH2": (368, 16, 512, 32),     # the bottom edge
    "emblem": (368, 32, 432, 96),     # the line colour's plaque (GREY, tinted by the client)
    "leather": (432, 32, 496, 64),    # the franchise's strap leather
    "metal": (432, 64, 496, 128),     # the franchise's accent metal
    "slab": (368, 96, 432, 160),      # the franchise's accent material (stone, wood, scale, cloth)
    "drop": (432, 128, 464, 160),     # a small glossy accent (slime drop, leaf, firefly)
})
# ---- the franchise overlay atlas
FX = Atlas(288, 208, {
    "relief": (0, 0, 144, 208),       # raised ornaments, alpha cut out, same pixel frame as the front cover
    "glow": (144, 0, 288, 208),       # the emissive layer, alpha cut out, same frame
})
# ---- the rarity trim atlas
TRIM = Atlas(256, 256, {
    "pageV": (0, 0, 32, 96),          # the fore-edge of the pages (vertical sheets)
    "pageH": (32, 0, 128, 32),        # the head and the tail of the pages
    "metal": (128, 0, 192, 64),       # corner plates, bands
    "metalDark": (192, 0, 256, 64),   # the inside of the plates, the bezels
    "strap": (0, 96, 64, 128),        # the clasp strap
    "cloth": (64, 96, 96, 128),       # the ribbon of the uncommon, the line's ribbon (grey, tinted) is in `ribbon`
    "ribbon": (96, 96, 128, 128),     # grey cloth tinted by the client (tint index 1)
    "gemDome": (0, 128, 32, 160),     # a round cut stone seen from the front, alpha cut out
    "gemSide": (32, 128, 48, 144),    # the sides of a stone's base
    "stud": (48, 128, 56, 136),       # a small teal stud
    "chain": (64, 128, 96, 160),      # a chain link seen flat, alpha cut out (hole in the middle)
    "chainEdge": (96, 128, 112, 160), # a chain link seen on edge
    "claw": (112, 128, 144, 160),     # a claw: dark horn with a pale ridge
    "fang": (144, 128, 176, 160),     # a silver fang
    "inlay": (0, 160, 64, 224),       # the glowing inlay plate (epic), alpha cut out
    "plaqueFrame": (64, 160, 128, 224),   # the bezel of the emblem plaque
    "tinted": (128, 160, 160, 192),   # the small gem of the clasp: grey, tinted by the client
    "bezel": (160, 160, 192, 192),    # a ring-shaped bezel under a gem
})
GLOW = Atlas(32, 32, {                # one frame of the animated mythic page edge (the png holds 8 frames below each other)
    "pageV": (0, 0, 16, 32),
    "pageH": (16, 0, 32, 16),
})
GLOW_FRAMES = 8


# --------------------------------------------------------------------------------------------------------------------------------- element helpers
def face(tex, uv, tint=None):
    f = {"uv": uv, "texture": tex}
    if tint is not None:
        f["tintindex"] = tint
    return f


def r3(v):
    return round(v, 3)


def cube(name, frm, to, faces, rot=None, glow=False):
    e = {"name": name, "from": [r3(v) for v in frm], "to": [r3(v) for v in to]}
    if rot is not None:
        axis, angle, origin = rot
        e["rotation"] = {"angle": angle, "axis": axis, "origin": [r3(v) for v in origin]}
    e["faces"] = faces
    if glow:
        e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return e


ALL = ("north", "south", "east", "west", "up", "down")


def solid(tex, uv, dirs=ALL, tint=None):
    """The same texture rectangle on several faces (small pieces that do not need their own mapping)."""
    return {d: face(tex, uv, tint) for d in dirs}


# ---- the mapping of a model-space rectangle on a board to the pixels of the cover image
def front_uv(x0, y0, x1, y1, name="front", atlas=COVER):
    return atlas.uv(name, ((x0 - BX0) * 16, (BY1 - y1) * 16, (x1 - BX0) * 16, (BY1 - y0) * 16))


def back_uv(x0, y0, x1, y1):
    return COVER.uv("back", ((BX1 - x1) * 16, (BY1 - y1) * 16, (BX1 - x0) * 16, (BY1 - y0) * 16))


def spine_uv(y0, y1, z0=BZ0, z1=BZ1):
    return COVER.uv("spine", ((z0 - BZ0) * 16, (BY1 - y1) * 16, (z1 - BZ0) * 16, (BY1 - y0) * 16))


def edge_h_uv(x0, x1, name="edgeH"):
    return COVER.uv(name, ((x0 - BX0) * 16, 0, (x1 - BX0) * 16, 16))


def edge_v_uv(y0, y1):
    return COVER.uv("edgeV", (0, (BY1 - y1) * 16, 16, (BY1 - y0) * 16))


def tr(name, sub=None, **kw):
    return TRIM.uv(name, sub, **kw)


# --------------------------------------------------------------------------------------------------------------------------------- the body (same for every look)
def body(rarity_i):
    """Boards, stepped panels, the spine with its bands, the page block, the ribbon. Returns the elements (about 40)."""
    E = []
    C, T = "#cover", "#trim"
    # ---- back board and its panel
    E.append(cube("back_board", [BX0, BY0, Z_BACK], [BX1, BY1, Z_BACK + 0.8], {
        "north": face(C, COVER.uv("back")), "east": face(C, COVER.uv("edgeV")), "up": face(C, edge_h_uv(BX0, BX1)), "down": face(C, edge_h_uv(BX0, BX1, "edgeH2"))}))
    E.append(cube("back_panel", [BX0 + P1, BY0 + P1, Z_BACK - 0.2], [BX1 - P1, BY1 - P1, Z_BACK], {
        "north": face(C, back_uv(BX0 + P1, BY0 + P1, BX1 - P1, BY1 - P1)), "east": face(C, COVER.uv("edgeV", (0, 0, 16, 16))),
        "up": face(C, edge_h_uv(BX0, BX0 + 1)), "down": face(C, edge_h_uv(BX0, BX0 + 1, "edgeH2"))}))
    # ---- front board and the two steps above it (real relief: the cover rises towards the middle)
    E.append(cube("front_board", [BX0, BY0, Z_FRONT - 0.8], [BX1, BY1, Z_FRONT], {
        "south": face(C, COVER.uv("front")), "east": face(C, COVER.uv("edgeV")), "up": face(C, edge_h_uv(BX0, BX1)), "down": face(C, edge_h_uv(BX0, BX1, "edgeH2"))}))
    for nm, inset, z0, z1 in (("front_p1", P1, Z_FRONT, Z_P1), ("front_p2", P2, Z_P1, Z_P2)):
        x0, y0, x1, y1 = BX0 + inset, BY0 + inset, BX1 - inset, BY1 - inset
        E.append(cube(nm, [x0, y0, z0], [x1, y1, z1], {
            "south": face(C, front_uv(x0, y0, x1, y1)), "east": face(C, COVER.uv("edgeV", (0, 0, 16, 16))), "west": face(C, COVER.uv("edgeV", (0, 0, 16, 16))),
            "up": face(C, edge_h_uv(BX0, BX0 + 1)), "down": face(C, edge_h_uv(BX0, BX0 + 1, "edgeH2"))}))
    # ---- the spine: three steps, wide to narrow, so that the back of the book is round when seen from above
    steps = [("spine_1", 3.0, BX0, 6.2, 9.8), ("spine_2", 2.7, 3.0, 6.6, 9.4), ("spine_3", 2.55, 2.7, 7.1, 8.9)]
    for nm, xa, xb, za, zb in steps:
        E.append(cube(nm, [xa, BY0, za], [xb, BY1, zb], {
            "west": face(C, spine_uv(BY0, BY1, za, zb)), "up": face(C, edge_h_uv(BX0, BX0 + 1)), "down": face(C, edge_h_uv(BX0, BX0 + 1, "edgeH2")),
            "south": face(C, edge_v_uv(BY0, BY1)), "north": face(C, edge_v_uv(BY0, BY1))}))
    # raised bands across the spine (the metal of the rarity), head and tail bands wider
    for i, (y0, y1) in enumerate(((1.7, 2.6), (4.6, 5.2), (9.8, 10.4), (13.4, 14.3))):
        E.append(cube("spine_band_%d" % (i + 1), [2.38, y0, 6.15], [2.62, y1, 9.85], {
            "west": face(T, tr("metal", (0, 0, 64, 16))), "up": face(T, tr("metal", (0, 16, 64, 20))), "down": face(T, tr("metalDark", (0, 16, 64, 20))),
            "north": face(T, tr("metalDark", (0, 16, 16, 20))), "south": face(T, tr("metalDark", (0, 16, 16, 20)))}))
    # ---- the page block: three boxes, narrow to wide to narrow in thickness, so that the fore-edge bulges like a real, slightly open book
    pv = lambda: face("#PV", PV_UV[0])
    ph = lambda: face("#PH", PH_UV[0])
    E.append(cube("pages_1", [3.7, 2.1, 6.8], [11.9, 13.9, 9.2], {"east": pv(), "up": ph(), "down": ph()}))
    E.append(cube("pages_2", [3.7, 2.1, 7.2], [12.1, 13.9, 8.8], {"east": pv(), "up": ph(), "down": ph()}))
    E.append(cube("pages_3", [3.7, 2.1, 7.7], [12.3, 13.9, 8.3], {"east": pv(), "up": ph(), "down": ph()}))
    # ---- the ribbon marker (tint index 1: the line colour), hanging out of the foot of the pages
    rib = lambda: face(T, tr("ribbon"), 1)
    E.append(cube("ribbon", [7.0, -0.2, 7.55], [7.75, 2.3, 7.85], solid(T, tr("ribbon"), ("north", "south", "east", "west", "down"), 1)))
    # the swallow-tail: two short tails splayed by 22.5 degrees
    for sgn, nm in ((-1, "ribbon_tail_l"), (1, "ribbon_tail_r")):
        E.append(cube(nm, [7.0 + (0.0 if sgn < 0 else 0.4), -1.25, 7.55], [7.35 + (0.0 if sgn < 0 else 0.4), -0.1, 7.85], solid(T, tr("ribbon"), ("north", "south", "east", "west", "down"), 1),
                      rot=("z", 22.5 * sgn, (7.375, -0.1, 7.7))))
    return E


# the page faces are swapped for the animated glow of the mythic books (#PV / #PH are resolved in model_json)
PV_UV = [TRIM.uv("pageV")]
PH_UV = [TRIM.uv("pageH")]


# --------------------------------------------------------------------------------------------------------------------------------- the trim (by rarity)
def corners(rarity_i):
    """Four corner caps that are not boxes: a wrap over the edge, an L of two arms along the board edges, a diamond boss at the point of the corner (turned 45 degrees), a plate on the back."""
    E = []
    T = "#trim"
    metal = lambda: face(T, tr("metal", (0, 0, 32, 32)))
    dark = lambda: face(T, tr("metalDark", (0, 0, 32, 32)))
    for nm, sx, sy in (("tl", -1, 1), ("tr", 1, 1), ("bl", -1, -1), ("br", 1, -1)):
        cx = BX0 if sx < 0 else BX1
        cy = BY1 if sy > 0 else BY0
        xa, xb = (cx - 0.12, cx + 0.95) if sx < 0 else (cx - 0.95, cx + 0.12)
        ya, yb = (cy - 0.95, cy + 0.12) if sy > 0 else (cy - 0.12, cy + 0.95)
        # the wrap round the edge (whole thickness)
        E.append(cube("cap_wrap_" + nm, [xa, ya, Z_BACK - 0.12], [xb, yb, Z_FRONT + 0.12], {d: metal() for d in ("north", "south", "east", "west", "up", "down")}))
        # the L: one arm along the top / bottom edge, one along the side edge, each tapering away from the corner (two steps)
        ex0, ex1 = (BX0 - 0.08, BX0 + 2.3) if sx < 0 else (BX1 - 2.3, BX1 + 0.08)
        ey0, ey1 = (BY1 - 2.3, BY1 + 0.08) if sy > 0 else (BY0 - 0.08, BY0 + 2.3)
        arm = 0.78
        hy0, hy1 = (ey1 - arm, ey1) if sy > 0 else (ey0, ey0 + arm)
        vx0, vx1 = (ex0, ex0 + arm) if sx < 0 else (ex1 - arm, ex1)
        E.append(cube("cap_arm_h_" + nm, [ex0, hy0, Z_FRONT], [ex1, hy1, Z_P2 + 0.04], {
            "south": face(T, tr("metal", (0, 0, 64, 24))), "north": dark(), "east": dark(), "west": dark(), "up": metal(), "down": metal()}))
        E.append(cube("cap_arm_v_" + nm, [vx0, ey0, Z_FRONT], [vx1, ey1, Z_P2 + 0.04], {
            "south": face(T, tr("metal", (0, 0, 24, 64))), "north": dark(), "east": dark(), "west": dark(), "up": metal(), "down": metal()}))
        # the boss at the point of the corner: a square turned by 45 degrees, so the cap has a pointed, jewelled look instead of a flat square
        bcx, bcy = (BX0 + 0.82, BY1 - 0.82) if (sx < 0 and sy > 0) else ((BX1 - 0.82, BY1 - 0.82) if sy > 0 else ((BX0 + 0.82, BY0 + 0.82) if sx < 0 else (BX1 - 0.82, BY0 + 0.82)))
        E.append(cube("cap_boss_" + nm, [bcx - 0.62, bcy - 0.62, Z_P2 + 0.04], [bcx + 0.62, bcy + 0.62, Z_P2 + 0.3], {
            "south": face(T, tr("metal", (16, 16, 48, 48))), "north": dark(), "east": dark(), "west": dark(), "up": metal(), "down": metal()}, rot=("z", 45.0, (bcx, bcy, Z_P2 + 0.1))))
        # the back plate
        E.append(cube("cap_back_" + nm, [ex0, min(hy0, ey0), Z_BACK - 0.34], [ex1, max(hy1, ey1), Z_BACK], {
            "north": face(T, tr("metal", (0, 0, 64, 64))), "south": dark(), "east": dark(), "west": dark(), "up": metal(), "down": metal()}))
    return E


def clasp(rarity_i):
    """The clasp strap round the fore-edge with the small line-colour gem on it (tint index 1) and a tongue on the front cover."""
    E = []
    T = "#trim"
    st = lambda sub=(0, 0, 64, 32): face(T, tr("strap", sub))
    sd = lambda: face(T, tr("metalDark", (0, 0, 32, 16)))
    y0, y1 = 6.4, 8.4
    E.append(cube("strap_back", [11.0, y0, Z_BACK - 0.38], [12.95, y1, Z_BACK], {"north": st(), "east": st(), "up": st((0, 0, 64, 8)), "down": st((0, 0, 64, 8)), "west": st((0, 0, 8, 32))}))
    E.append(cube("strap_edge", [12.5, y0, Z_BACK - 0.38], [12.95, y1, Z_FRONT + 0.55], {"east": st((0, 0, 64, 32)), "up": st((0, 0, 8, 8)), "down": st((0, 0, 8, 8)), "north": st((0, 0, 8, 32)), "south": st((0, 0, 8, 32))}))
    E.append(cube("strap_block", [11.25, y0, Z_FRONT], [12.95, y1, Z_FRONT + 0.55], {"south": st(), "up": st((0, 0, 64, 8)), "down": st((0, 0, 64, 8)), "west": st((0, 0, 8, 32))}))
    E.append(cube("strap_tongue", [9.1, y0 + 0.2, Z_P2], [11.25, y1 - 0.2, Z_P2 + 0.15], {"south": st(), "up": st((0, 0, 64, 4)), "down": st((0, 0, 64, 4)), "west": st((0, 0, 8, 16))}))
    E.append(cube("strap_tip", [8.7, y0 + 0.55, Z_P2], [9.1, y1 - 0.55, Z_P2 + 0.15], {"south": st((0, 8, 16, 24)), "up": st((0, 0, 16, 4)), "down": st((0, 0, 16, 4)), "west": st((0, 0, 8, 16))}))
    # the small gem on the clasp: a bezel, a tinted stone
    gx, gy = 11.95, 7.4
    E.append(cube("clasp_bezel", [gx - 0.62, gy - 0.62, Z_FRONT + 0.55], [gx + 0.62, gy + 0.62, Z_FRONT + 0.72], {"south": face(T, tr("bezel")), "up": sd(), "down": sd(), "east": sd(), "west": sd()}))
    E.append(cube("clasp_bezel_r", [gx - 0.62, gy - 0.62, Z_FRONT + 0.55], [gx + 0.62, gy + 0.62, Z_FRONT + 0.72], {"south": face(T, tr("bezel")), "up": sd(), "down": sd(), "east": sd(), "west": sd()},
                  rot=("z", 45, (gx, gy, Z_FRONT + 0.6))))
    E.append(cube("clasp_gem", [gx - 0.5, gy - 0.5, Z_FRONT + 0.72], [gx + 0.5, gy + 0.5, Z_FRONT + 0.85], {"south": face(T, tr("tinted"), 1), "up": face(T, tr("tinted", (0, 0, 16, 8)), 1),
                                                                                                         "down": face(T, tr("tinted", (0, 0, 16, 8)), 1), "east": face(T, tr("tinted", (0, 0, 8, 16)), 1), "west": face(T, tr("tinted", (0, 0, 8, 16)), 1)}))
    # two rivets
    for k, yy in enumerate((y0 + 0.28, y1 - 0.28)):
        E.append(cube("rivet_%d" % (k + 1), [11.3, yy - 0.14, Z_FRONT + 0.55], [11.58, yy + 0.14, Z_FRONT + 0.7], solid(T, tr("metal", (0, 0, 8, 8)), ("south", "up", "down", "east", "west"))))
    return E


def plaque():
    """The line colour's emblem plaque (tint index 1) on the upper part of the cover, in a bezel."""
    E = []
    T, C = "#trim", "#cover"
    cx, cy = PLAQUE
    s = 1.6
    E.append(cube("plaque_frame", [cx - s, cy - s, Z_P2], [cx + s, cy + s, Z_P2 + 0.16], {"south": face(T, tr("plaqueFrame")), "up": face(T, tr("metalDark", (0, 0, 32, 8))),
                                                                                         "down": face(T, tr("metalDark", (0, 0, 32, 8))), "east": face(T, tr("metalDark", (0, 0, 8, 16))), "west": face(T, tr("metalDark", (0, 0, 8, 16)))}))
    s2 = 1.32
    E.append(cube("plaque", [cx - s2, cy - s2, Z_P2 + 0.16], [cx + s2, cy + s2, Z_P2 + 0.34], {"south": face(C, COVER.uv("emblem"), 1), "up": face(C, COVER.uv("emblem", (0, 0, 64, 8)), 1),
                                                                                              "down": face(C, COVER.uv("emblem", (0, 0, 64, 8)), 1), "east": face(C, COVER.uv("emblem", (0, 0, 8, 64)), 1), "west": face(C, COVER.uv("emblem", (0, 0, 8, 64)), 1)}))
    return E


def overlays():
    """The raised relief of the cover (alpha ornaments) and the emissive layer (alpha), both on panel 2."""
    x0, y0, x1, y1 = BX0 + P2, BY0 + P2, BX1 - P2, BY1 - P2
    E = [cube("relief", [x0, y0, Z_P2 + 0.03], [x1, y1, Z_P2 + 0.04], {"south": face("#fx", front_uv(x0, y0, x1, y1, "relief", FX))})]
    E.append(cube("glowlayer", [x0, y0, Z_P2 + 0.06], [x1, y1, Z_P2 + 0.07], {"south": face("#fx", front_uv(x0, y0, x1, y1, "glow", FX))}, glow=True))
    return E


# ---- gems: a bezel of two overlapping squares, a dome decal with alpha, a raised core
def gem(name, cx, cy, r, z, glow=False, domeSub=None, bezel=True, tex="gemDome"):
    T = "#trim"
    E = []
    h = r * 0.66
    side = lambda: face(T, tr("bezel", (0, 0, 8, 8)))
    if bezel:
        E.append(cube(name + "_b1", [cx - h, cy - h, z], [cx + h, cy + h, z + 0.22], {"south": face(T, tr("bezel")), "up": side(), "down": side(), "east": side(), "west": side()}))
        E.append(cube(name + "_b2", [cx - r * 0.71, cy - r * 0.71, z], [cx + r * 0.71, cy + r * 0.71, z + 0.22], {"south": face(T, tr("bezel")), "up": side(), "down": side(), "east": side(), "west": side()},
                      rot=("z", 45, (cx, cy, z))))
    k = r * 0.58
    E.append(cube(name + "_core", [cx - k, cy - k, z + 0.22], [cx + k, cy + k, z + 0.5], {
        "south": face(T, tr(tex)), "up": face(T, tr("gemSide")), "down": face(T, tr("gemSide")), "east": face(T, tr("gemSide")), "west": face(T, tr("gemSide"))}, glow=glow))
    E.append(cube(name + "_dome", [cx - r * 0.86, cy - r * 0.86, z + 0.52], [cx + r * 0.86, cy + r * 0.86, z + 0.53], {"south": face(T, tr(tex))}, glow=glow))
    return E


def studs(name, cx, cy, radius, z, n=8, size=0.42):
    E = []
    T = "#trim"
    for i in range(n):
        a = i * 2 * math.pi / n + math.pi / n
        x, y = cx + radius * math.cos(a), cy + radius * math.sin(a)
        E.append(cube("%s_%d" % (name, i + 1), [x - size / 2, y - size / 2, z], [x + size / 2, y + size / 2, z + size * 0.85], solid(T, tr("stud"), ("south", "up", "down", "east", "west"))))
    return E


def chain(rarity_i, path):
    """A chain of flat and edge-on links along a list of (x, y, angle) points (angle about Z in 22.5 degree steps, links alternate flat / edge)."""
    E = []
    T = "#trim"
    for i, (x, y, ang) in enumerate(path):
        if i % 2 == 0:
            E.append(cube("link_%d" % (i + 1), [x - 0.85, y - 0.5, Z_P2 + 0.34], [x + 0.85, y + 0.5, Z_P2 + 0.54], {"south": face(T, tr("chain")), "up": face(T, tr("chainEdge", (0, 0, 16, 8))),
                                                                                                                        "down": face(T, tr("chainEdge", (0, 0, 16, 8))), "east": face(T, tr("chainEdge", (0, 0, 8, 16))), "west": face(T, tr("chainEdge", (0, 0, 8, 16)))},
                          rot=("z", ang, (x, y, Z_P2 + 0.4)) if ang else None))
        else:
            E.append(cube("link_%d" % (i + 1), [x - 0.55, y - 0.28, Z_P2 + 0.28], [x + 0.55, y + 0.28, Z_P2 + 0.66], {"south": face(T, tr("chainEdge")), "up": face(T, tr("chainEdge", (0, 0, 16, 8))),
                                                                                                                      "down": face(T, tr("chainEdge", (0, 0, 16, 8))), "east": face(T, tr("chainEdge", (0, 0, 8, 16))), "west": face(T, tr("chainEdge", (0, 0, 8, 16)))},
                          rot=("z", ang, (x, y, Z_P2 + 0.45)) if ang else None))
    return E


def chain_path(x_from, y_from, x_to, y_to, sag=0.0, step=0.8):
    """Links from one point to the other, with a sag in the middle; the angle snaps to the 22.5 degree steps the model format allows."""
    n = max(2, int(math.hypot(x_to - x_from, y_to - y_from) / step))
    pts = []
    prev = None
    for i in range(n + 1):
        t = i / n
        x = x_from + (x_to - x_from) * t
        y = y_from + (y_to - y_from) * t - sag * 4 * t * (1 - t)
        pts.append((x, y))
    out = []
    for i, (x, y) in enumerate(pts):
        a = pts[min(i + 1, n)], pts[max(i - 1, 0)]
        ang = math.degrees(math.atan2(a[0][1] - a[1][1], a[0][0] - a[1][0]))
        ang = max(-45.0, min(45.0, round(ang / 22.5) * 22.5))
        out.append((x, y, ang))
    return out


def claw_row(xs, scales, y_base=12.4, z_base=5.95, radius=2.5):
    """The dragon-claw fastener of the mythic book: claws that rise out of the back cover, arch over the top edge and bite into the front cover. Each claw is a curved chain of tapered boxes."""
    E = []
    T = "#trim"
    # directions (degrees from +Y towards +Z): up, then turning forward and down. phi <= 45: box long on Y with angle phi; 45 < phi <= 135: box long on Z with angle phi - 90; > 135: long on Y, angle phi - 180
    phis = [0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5]
    for ci, (x, sc) in enumerate(zip(xs, scales)):
        y, z = y_base, z_base
        L = radius * math.pi / len(phis) * 1.02 * sc
        n = len(phis)
        for si, phi in enumerate(phis):
            t0, t1 = si / n, (si + 1) / n
            w0 = 1.0 * sc * (1 - 0.72 * t0) + 0.22
            th = 0.85 * sc * (1 - 0.7 * t0) + 0.2
            rad = math.radians(phi)
            dy, dz = math.cos(rad), math.sin(rad)
            ym, zm = y + dy * L / 2, z + dz * L / 2
            if phi <= 45 or phi > 135:
                ang = phi if phi <= 45 else phi - 180
                frm, to = [x - w0 / 2, ym - L / 2, zm - th / 2], [x + w0 / 2, ym + L / 2, zm + th / 2]
            else:
                ang = phi - 90
                frm, to = [x - w0 / 2, ym - th / 2, zm - L / 2], [x + w0 / 2, ym + th / 2, zm + L / 2]
            tex = face(T, tr("claw", (0, 0, 32, 32)))
            E.append(cube("claw_%d_%d" % (ci + 1, si + 1), frm, to, {d: tex for d in ALL}, rot=("x", ang, (x, ym, zm)) if ang else None))
            y += dy * L
            z += dz * L
    return E


def fangs(sc=1.0):
    """The silver fang ornament on the lower fore-edge corner of the mythic book: a curved plate with three fangs."""
    E = []
    T = "#trim"
    fg = lambda: face(T, tr("fang"))
    x0, y0 = 10.2, 1.5
    E.append(cube("fang_plate", [x0, y0 - 0.1, Z_P2 + 0.2], [BX1 + 0.35, y0 + 1.6, Z_P2 + 0.52], {"south": face(T, tr("metal", (0, 0, 48, 32))), "up": fg(), "down": fg(), "east": fg(), "west": fg()}))
    for i, (fx, fy, ln) in enumerate(((10.7, 3.1, 1.3), (11.5, 3.3, 1.6), (12.3, 3.0, 1.2))):
        E.append(cube("fang_%d" % (i + 1), [fx - 0.28, fy - 0.1, Z_P2 + 0.3], [fx + 0.28, fy + ln, Z_P2 + 0.62], {d: fg() for d in ALL}, rot=("z", -22.5 if i == 0 else (0 if i == 1 else 22.5), (fx, fy, Z_P2 + 0.4))))
    return E


# --------------------------------------------------------------------------------------------------------------------------------- the rarity decoration of the cover
def decoration(rarity_i, franchise):
    E = []
    T = "#trim"
    cx, cy = CENTRE
    if rarity_i == 1:                       # uncommon: a green cloth band round the foot of the cover and a plain bow at the spine
        E.append(cube("band_front", [BX0 + 0.05, 2.2, Z_FRONT], [BX1 - 0.05, 3.3, Z_P2 + 0.2], {"south": face(T, tr("cloth")), "up": face(T, tr("cloth", (0, 0, 32, 8))), "down": face(T, tr("cloth", (0, 0, 32, 8))),
                                                                                                "east": face(T, tr("cloth", (0, 0, 8, 16))), "west": face(T, tr("cloth", (0, 0, 8, 16)))}))
        E.append(cube("band_back", [BX0 + 0.05, 2.2, Z_BACK - 0.2], [BX1 - 0.05, 3.3, Z_BACK], {"north": face(T, tr("cloth")), "up": face(T, tr("cloth", (0, 0, 32, 8))), "down": face(T, tr("cloth", (0, 0, 32, 8)))}))
        E.append(cube("band_knot", [BX0 + 0.9, 1.95, Z_P2 + 0.2], [BX0 + 2.5, 3.55, Z_P2 + 0.42], {"south": face(T, tr("cloth")), "up": face(T, tr("cloth", (0, 0, 32, 8))), "down": face(T, tr("cloth", (0, 0, 32, 8))),
                                                                                                   "east": face(T, tr("cloth", (0, 0, 8, 16))), "west": face(T, tr("cloth", (0, 0, 8, 16)))}))
    elif rarity_i == 2:                     # rare: one big sapphire in a silver bezel
        E += gem("gem", cx, cy, 1.3, Z_P2 + 0.06)
    elif rarity_i == 3:                     # epic: the glowing violet inlay, two amethysts, the chain
        E.append(cube("inlay", [cx - 2.35, cy - 2.35, Z_P2 + 0.08], [cx + 2.35, cy + 2.35, Z_P2 + 0.09], {"south": face(T, tr("inlay"))}, glow=True))
        E += chain(rarity_i, chain_path(BX0 + 0.45, 8.9, BX1 - 0.55, 5.1, sag=0.5, step=1.05))
        E += gem("gemL", cx - 2.0, cy, 0.82, Z_P2 + 0.3, glow=False)
        E += gem("gemR", cx + 2.0, cy, 0.82, Z_P2 + 0.3, glow=False)
    elif rarity_i == 4:                     # mythic: the big ruby ringed with teal studs, the chain, the claws, the fangs
        E.append(cube("inlay", [cx - 2.9, cy - 2.9, Z_P2 + 0.08], [cx + 2.9, cy + 2.9, Z_P2 + 0.09], {"south": face(T, tr("inlay"))}, glow=True))
        E += chain(rarity_i, chain_path(BX0 + 0.45, 9.6, BX1 - 0.55, 8.4, sag=1.2, step=1.05))
        E += gem("ruby", cx, cy, 1.62, Z_P2 + 0.34)
        E += studs("stud", cx, cy, 2.1, Z_P2 + 0.34, 12, 0.36)
        E += claw_row([5.6, 7.0, 8.4, 9.8, 11.2], [0.82, 1.0, 1.1, 1.0, 0.82])
        E += fangs()
    return E


# --------------------------------------------------------------------------------------------------------------------------------- franchise accents
def accent(franchise):
    """A few real pieces that make the franchise's cover a silhouette of its own. Foot of the cover is LOW; the centre is kept free for the rarity gems."""
    E = []
    C = "#cover"
    lx, ly = LOW
    mt = lambda sub=(0, 0, 64, 64): face(C, COVER.uv("metal", sub))
    sl = lambda sub=(0, 0, 64, 64): face(C, COVER.uv("slab", sub))
    dr = lambda sub=(0, 0, 32, 32): face(C, COVER.uv("drop", sub))
    lt = lambda sub=(0, 0, 64, 32): face(C, COVER.uv("leather", sub))
    if franchise == "exiled_heavy_knight":                  # the scaled pad on the top edge that the claws grip, and a ridge of spikes
        E.append(cube("scale_pad", [4.6, BY1 - 0.05, 6.15], [11.6, BY1 + 0.32, 9.85], {"up": sl(), "north": sl((0, 0, 64, 16)), "south": sl((0, 0, 64, 16)), "east": sl((0, 0, 16, 16)), "west": sl((0, 0, 16, 16))}))
        for i, x in enumerate((5.4, 6.6, 7.8, 9.0, 10.2)):
            E.append(cube("ridge_%d" % (i + 1), [x - 0.3, BY1 + 0.32, 7.2], [x + 0.3, BY1 + 0.78 + (0.12 if i % 2 else 0), 8.8], {d: sl((0, 0, 16, 16)) for d in ALL}))
    elif franchise == "tensura":                            # three glossy slime drops at the foot
        for i, (x, s) in enumerate(((lx - 1.5, 0.62), (lx, 0.82), (lx + 1.5, 0.55))):
            E.append(cube("drop_%d" % (i + 1), [x - s, ly - s * 0.75, Z_P2 + 0.02], [x + s, ly + s * 0.75, Z_P2 + 0.3], {"south": dr(), "up": dr((0, 0, 32, 8)), "down": dr((0, 0, 32, 8)), "east": dr((0, 0, 8, 32)), "west": dr((0, 0, 8, 32))}))
            E.append(cube("dropc_%d" % (i + 1), [x - s * 0.62, ly - s * 0.45, Z_P2 + 0.3], [x + s * 0.62, ly + s * 0.55, Z_P2 + 0.46], {"south": dr((0, 0, 32, 32)), "up": dr((0, 0, 32, 8)), "east": dr((0, 0, 8, 32)), "west": dr((0, 0, 8, 32))}))
    elif franchise == "black_clover":                       # the silver clover plate
        for i, (dx, dy) in enumerate(((0, 0.62), (0, -0.62), (0.62, 0), (-0.62, 0))):
            E.append(cube("leaf_%d" % (i + 1), [lx + dx - 0.55, ly + dy - 0.55, Z_P2 + 0.02], [lx + dx + 0.55, ly + dy + 0.55, Z_P2 + 0.26], {"south": mt(), "up": mt((0, 0, 32, 8)), "down": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}, rot=("z", 45, (lx + dx, ly + dy, Z_P2 + 0.1))))
        E.append(cube("leaf_core", [lx - 0.4, ly - 0.4, Z_P2 + 0.26], [lx + 0.4, ly + 0.4, Z_P2 + 0.42], {"south": mt((0, 0, 32, 32)), "up": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32)), "down": mt((0, 0, 32, 8))}))
    elif franchise == "magi":                               # the tooled leather strap across the foot of the cover with a buckle
        E.append(cube("belt", [BX0 + 0.05, ly - 0.55, Z_FRONT], [BX1 + 0.1, ly + 0.55, Z_P2 + 0.16], {"south": lt(), "up": lt((0, 0, 64, 8)), "down": lt((0, 0, 64, 8)), "east": lt((0, 0, 8, 32)), "west": lt((0, 0, 8, 32))}))
        E.append(cube("belt_back", [BX0 + 0.05, ly - 0.55, Z_BACK - 0.16], [BX1 + 0.1, ly + 0.55, Z_BACK], {"north": lt(), "up": lt((0, 0, 64, 8)), "down": lt((0, 0, 64, 8))}))
        E.append(cube("buckle", [lx - 0.9, ly - 0.8, Z_P2 + 0.16], [lx + 0.9, ly + 0.8, Z_P2 + 0.34], {"south": mt(), "up": mt((0, 0, 32, 8)), "down": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}))
        E.append(cube("buckle_hole", [lx - 0.45, ly - 0.35, Z_P2 + 0.34], [lx + 0.45, ly + 0.35, Z_P2 + 0.44], {"south": lt((0, 0, 32, 16)), "up": lt((0, 0, 32, 8))}))
    elif franchise == "solo_leveling":                      # wisps of shadow smoke hanging off the lower edge
        for i, (x, w, h) in enumerate(((4.6, 1.2, 1.5), (6.4, 1.0, 1.1), (8.3, 1.4, 1.6), (10.3, 1.0, 1.2), (11.7, 0.9, 1.0))):
            E.append(cube("wisp_%d" % (i + 1), [x - w / 2, BY0 - h, Z_P2 - 0.2 - 0.0 + 0.0], [x + w / 2, BY0 + 0.3, Z_P2 - 0.19], {"south": sl((0, 0, 32, 48))}))
    elif franchise == "danmachi":                           # gold filigree plate and white firefly lights
        E.append(cube("filigree", [lx - 1.6, ly - 0.6, Z_P2 + 0.02], [lx + 1.6, ly + 0.6, Z_P2 + 0.2], {"south": mt(), "up": mt((0, 0, 48, 8)), "down": mt((0, 0, 48, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}))
        for i, (x, y) in enumerate(((5.1, 5.2), (10.9, 5.6), (6.2, 9.1), (9.9, 9.4), (8.0, 2.9))):
            E.append(cube("firefly_%d" % (i + 1), [x - 0.2, y - 0.2, Z_P2 + 0.1], [x + 0.2, y + 0.2, Z_P2 + 0.4], {d: dr((0, 0, 8, 8)) for d in ("south", "up", "down", "east", "west")}, glow=True))
    elif franchise == "tbate":                              # three green leaves growing at the foot
        for i, (dx, dy, a) in enumerate(((-1.2, 0.2, 22.5), (0, 0.5, 0), (1.2, 0.2, -22.5))):
            E.append(cube("leaf_%d" % (i + 1), [lx + dx - 0.45, ly + dy - 0.65, Z_P2 + 0.02], [lx + dx + 0.45, ly + dy + 0.65, Z_P2 + 0.2], {"south": dr(), "up": dr((0, 0, 16, 4)), "down": dr((0, 0, 16, 4)), "east": dr((0, 0, 4, 16)), "west": dr((0, 0, 4, 16))},
                          rot=("z", a, (lx + dx, ly + dy, Z_P2 + 0.1))))
    elif franchise == "tower_of_god":                       # the steel cross
        E.append(cube("cross_h", [BX0 + P2 - 0.1, 6.55, Z_P2 + 0.04], [BX1 - P2 + 0.1, 7.45, Z_P2 + 0.3], {"south": mt(), "up": mt((0, 0, 64, 8)), "down": mt((0, 0, 64, 8)), "east": mt((0, 0, 8, 16)), "west": mt((0, 0, 8, 16))}))
        E.append(cube("cross_v", [7.55, BY0 + P2 - 0.1, Z_P2 + 0.04], [8.45, 9.1, Z_P2 + 0.3], {"south": mt(), "up": mt((0, 0, 16, 8)), "down": mt((0, 0, 16, 8)), "east": mt((0, 0, 8, 64)), "west": mt((0, 0, 8, 64))}))
        for i, (x, y) in enumerate(((5.0, 7.0), (11.0, 7.0), (8.0, 3.2))):
            E.append(cube("cross_end_%d" % (i + 1), [x - 0.55, y - 0.55, Z_P2 + 0.3], [x + 0.55, y + 0.55, Z_P2 + 0.44], {"south": mt((0, 0, 32, 32)), "up": mt((0, 0, 32, 8)), "down": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}))
    elif franchise == "god_of_high_school":                 # the brass lock on the strap
        E.append(cube("lock", [10.9, 6.2, Z_FRONT + 0.85], [12.9, 8.6, Z_FRONT + 1.05], {"south": mt(), "up": mt((0, 0, 32, 8)), "down": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}))
        E.append(cube("lock_keyhole", [11.65, 7.0, Z_FRONT + 1.05], [12.15, 7.8, Z_FRONT + 1.12], {"south": sl((0, 0, 8, 16))}))
        E.append(cube("lock_bar", [10.6, 7.2, Z_FRONT + 0.7], [11.0, 7.7, Z_FRONT + 0.95], {d: mt((0, 0, 16, 16)) for d in ("south", "up", "down", "east", "west")}))
    elif franchise == "omniscient_readers_viewpoint":       # four stone plates and two silver rings
        for i, (x, y) in enumerate(((5.7, 3.7), (10.3, 3.7))):
            E.append(cube("slab_%d" % (i + 1), [x - 1.05, y - 0.85, Z_P2 + 0.02], [x + 1.05, y + 0.85, Z_P2 + 0.3], {"south": sl(), "up": sl((0, 0, 32, 8)), "down": sl((0, 0, 32, 8)), "east": sl((0, 0, 8, 32)), "west": sl((0, 0, 8, 32))}))
        for i, (x, y) in enumerate(((5.7, 3.7), (10.3, 3.7))):
            E.append(cube("ring_%d" % (i + 1), [x - 0.5, y - 0.5, Z_P2 + 0.3], [x + 0.5, y + 0.5, Z_P2 + 0.42], {"south": mt((0, 0, 32, 32)), "up": mt((0, 0, 32, 8)), "down": mt((0, 0, 32, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))}))
        E.append(cube("slab_mid", [lx - 0.8, ly - 0.55, Z_P2 + 0.02], [lx + 0.8, ly + 0.55, Z_P2 + 0.24], {"south": sl((0, 0, 32, 32)), "up": sl((0, 0, 32, 8)), "down": sl((0, 0, 32, 8)), "east": sl((0, 0, 8, 32)), "west": sl((0, 0, 8, 32))}))
    elif franchise == "fairy_tail":                         # the gold wing of the guild mark: five feather steps rising to the right
        for i in range(5):
            x = lx - 2.0 + i * 0.95
            E.append(cube("wing_%d" % (i + 1), [x - 0.42, ly - 0.7 + i * 0.22, Z_P2 + 0.02], [x + 0.42, ly + 0.2 + i * 0.5, Z_P2 + 0.26], {"south": mt(), "up": mt((0, 0, 16, 8)), "down": mt((0, 0, 16, 8)), "east": mt((0, 0, 8, 32)), "west": mt((0, 0, 8, 32))},
                          rot=("z", -22.5, (x, ly, Z_P2 + 0.1))))
    return E


# --------------------------------------------------------------------------------------------------------------------------------- the whole model
def elements(franchise, rarity):
    ri = RARITIES.index(rarity)
    E = body(ri) + corners(ri) + clasp(ri) + plaque() + overlays() + accent(franchise) + decoration(ri, franchise)
    return E


def textures(franchise, rarity):
    t = {"cover": "nusmp:item/skill_book_v2/%s" % franchise, "fx": "nusmp:item/skill_book_v2/%s_fx" % franchise, "trim": "nusmp:item/skill_book_v2/trim_%s" % rarity,
         "particle": "nusmp:item/skill_book_v2/trim_%s" % rarity}
    if rarity == "mythic":
        t["glow"] = "nusmp:item/skill_book_v2/glow_mythic"
    return t


def model_json(franchise, rarity):
    ri = RARITIES.index(rarity)
    els = elements(franchise, rarity)
    mythic = rarity == "mythic"
    pv, ph = ("#glow", GLOW.uv("pageV")) if mythic else ("#trim", TRIM.uv("pageV")), ("#glow", GLOW.uv("pageH")) if mythic else ("#trim", TRIM.uv("pageH"))
    for e in els:
        if e["name"].startswith("pages_"):
            for d, f in list(e["faces"].items()):
                if f["texture"] == "#PV":
                    e["faces"][d] = {"uv": pv[1], "texture": pv[0]}
                elif f["texture"] == "#PH":
                    e["faces"][d] = {"uv": ph[1], "texture": ph[0]}
            if mythic:
                e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return {"_comment": "GENERATED by tools/lines/gen_book_textures_v2.py (geometry in tools/lines/book_model_v2.py). Do not edit by hand.",
            "parent": "nusmp:item/skill_book_v2", "textures": textures(franchise, rarity), "elements": els}


def parent_json():
    """skill_book_v2.json: the display block and the default textures every look shares."""
    return {"_comment": "GENERATED by tools/lines/gen_book_textures_v2.py (geometry in tools/lines/book_model_v2.py). Do not edit by hand.",
            "textures": {"particle": "nusmp:item/skill_book_v2/trim_common"}, "display": DISPLAY}


def icon_json(franchise, rarity):
    """The flat painted icon: one quad with the painted 64 x 64 picture and one with its tinted layer (tint index 1: ribbon, plaque, small gem). Zero thickness, so the inventory draws two quads."""
    base = "nusmp:item/skill_book_icons/%s_%s" % (franchise, rarity)
    q = lambda z, tex, tint=None: {"from": [0, 0, z], "to": [16, 16, z], "faces": {"south": face(tex, [0, 0, 16, 16], tint)}}
    return {"_comment": "GENERATED by tools/lines/gen_book_textures_v2.py. The painted inventory icon of one franchise x rarity.",
            "textures": {"icon": base, "tint": base + "_tint", "particle": base}, "elements": [q(8.0, "#icon"), q(8.01, "#tint", 1)],
            "display": {"gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}}


def wrapper_json(franchise, rarity):
    """skill_book_v2st_<f>_<r>.json: the body in hand / on the ground / in frames, the painted icon in the inventory."""
    return {"loader": "neoforge:separate_transforms", "gui_light": "front", "textures": {"particle": "nusmp:item/skill_book_v2/trim_%s" % rarity},
            "base": {"parent": "nusmp:item/skill_book_v2_%s_%s" % (franchise, rarity)},
            "perspectives": {"gui": {"parent": "nusmp:item/skill_book_icon_%s_%s" % (franchise, rarity)}}}


# --------------------------------------------------------------------------------------------------------------------------------- display (hand, ground, frame)
# Left-hand entries have the SAME numbers as the right-hand ones on purpose (ItemTransform.apply negates tx, ry and rz of a left hand by itself: the mirrored pose, cover turned outward on both sides).
_TP = {"rotation": [60, 150, 0], "translation": [0.0, 1.0, 3.0], "scale": [0.55, 0.55, 0.55]}
_FP = {"rotation": [-15, -15, 15], "translation": [-2.9, 4.3, -1.3], "scale": [0.46, 0.46, 0.46]}
DISPLAY = {
    "thirdperson_righthand": _TP,
    "thirdperson_lefthand": _TP,
    "firstperson_righthand": _FP,
    "firstperson_lefthand": _FP,
    "ground": {"rotation": [-78, 0, 8], "translation": [0, 1.5, 0], "scale": [0.7, 0.7, 0.7]},
    "gui": {"rotation": [20, -155, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
}


# --------------------------------------------------------------------------------------------------------------------------------- the overrides
def franchise_threshold(i):
    """Half a step below (i + 1) / PROP_STEPS, like the 1.82.0 overrides."""
    return round((i + 1) / PROP_STEPS - 0.5 / PROP_STEPS, 6)


def rarity_threshold(rank):
    return round((rank + 1) / RARITY_STEPS - 0.5 / RARITY_STEPS, 6)


def new_overrides(franchises):
    """The 55 overrides of look 2, ordered by (franchise, rarity) ascending: the game compares each predicate with >= and the LAST matching override wins, so (f, r) is the last match for a stack of
    franchise value f and rarity value r (every entry after it needs a higher franchise or a higher rarity)."""
    out = []
    for i, f in enumerate(franchises):
        for r, name in enumerate(RARITIES):
            out.append({"predicate": {"nusmp:franchise": franchise_threshold(i), "nusmp:rarity": rarity_threshold(r)}, "model": "nusmp:item/skill_book_v2st_%s_%s" % (f, name)})
    return out


def legacy_overrides(franchises):
    """The 1.82.0 covers again, for the client setting legacyLook (nusmp:look = 1): listed LAST, so they win over the new ones when the property is 1. A franchise that has no 1.82.0 cover (fairy_tail,
    added in 3.26.0) gets the generic grey 3D book, skill_book_st_generic, instead of the cover of the franchise before it."""
    return [{"predicate": {"nusmp:look": 1.0, "nusmp:franchise": franchise_threshold(i)},
             "model": "nusmp:item/skill_book_st_%s" % (f if i < LEGACY_FRANCHISE_COUNT else "generic")} for i, f in enumerate(franchises)]


def skill_book_overrides(old_overrides, franchises):
    """The whole override list of skill_book.json. The game keeps the LAST matching override, so the order is the priority, lowest first: the 1.82.0 franchise overrides (the fall-back when the
    rarity property is missing), then the 55 new ones, then the legacy ones (they only match while nusmp:look is 1). Both generators call this, so skill_book.json is the same either way."""
    return list(old_overrides) + new_overrides(franchises) + legacy_overrides(franchises)
