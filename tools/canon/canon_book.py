"""The geometry, the texture atlas layout and the display transforms of the Canon Grimoire (the 3D hand model of every `charter_grimoire_<id>` item).

Shared by gen_canon_models.py (which paints the atlas and writes the model files) and preview_canon.py (which renders the model json back to a picture), so the model, the
atlas and the preview can never disagree about where a face looks in the texture. docs/canon/canon_grimoire.md explains every number.

The book stands upright in model space (16 units = one block): the FRONT cover looks SOUTH (+Z, like the front of an item sprite), the spine is on the WEST (-X) edge, the
fore-edge (pages) on the EAST. About 10.6 wide (with the spine), 13 high, 4 thick. The middle of its box is (8.2, 8, 8), close to the middle of the 16 unit space, so scaling
and rotating in the display block happen about the middle of the book.
"""

# --------------------------------------------------------------------------- the atlas
W, H = 192, 240            # pixels
UNIT = 8                   # pixels per model unit on the cover (the covers are 10 x 13 units = 80 x 104 px)

REG = {                    # name -> (x0, y0, x1, y1) in pixels
    "front": (0, 0, 80, 104),          # the front cover, 10 x 13 units, looking at it from the front
    "spine": (80, 0, 110, 104),        # the back of the book, z 6..10 (30 px) x 13 units
    "edgeV": (110, 0, 118, 104),       # the board's fore-edge strip (1 unit x 13)
    "edgeH": (0, 104, 80, 112),        # the board's head / tail strip (10 x 1 unit)
    "medal": (120, 0, 164, 44),        # the cover medallion: gold bezel, dark disc, the pressed shadow of the emblem (5.5 x 5.5 units)
    "glow": (120, 48, 160, 88),        # the emblem, full-bright, on a quad 5 x 5 units in front of the medallion
    "metal": (0, 112, 16, 128),        # gold plate with a bevel and a rivet
    "metalDark": (16, 112, 32, 128),   # darker bronze
    "ribbon": (32, 112, 40, 128),      # the marker ribbon
    "gem": (40, 112, 48, 120),         # the clasp's stone (full-bright)
    "strap": (48, 112, 64, 128),       # stitched dark leather
    "leather": (64, 112, 80, 128),     # plain leather in the cover colour
    "pageV": (110, 104, 134, 128),     # the fore-edge of the page block: lines run along v (up the book), 24 px = 3 units across the pages
    "pageH": (134, 104, 158, 128),     # head and tail of the page block: lines run along u
    "back": (0, 128, 80, 232),         # the back cover, looking at it from behind (pixel column 0 is the EAST edge)
}

# --------------------------------------------------------------------------- the box of the book
X0, X1 = 3.4, 13.0         # boards: spine side .. fore-edge
Y0, Y1 = 1.5, 14.5         # boards: foot .. head
Z0, Z1 = 6.0, 10.0         # back .. front (the outer bevels)
BOARD = 0.4                # a board proper
BEVEL = 0.3                # the stepped edge on top of it, 0.25 in from every side: the "rounded" edge
INSET = 0.25

GLOW = {"block_light": 15, "sky_light": 15}


def ruv(name, sub=None, flip_u=False, flip_v=False):
    """Pixel rect (the whole region, or `sub` = an absolute pixel rect) -> [u0, v0, u1, v1] of a model face (the model's uv space is 0..16 over the whole texture)."""
    r = sub if sub is not None else REG[name]
    u0, v0, u1, v1 = r[0] * 16.0 / W, r[1] * 16.0 / H, r[2] * 16.0 / W, r[3] * 16.0 / H
    if flip_u:
        u0, u1 = u1, u0
    if flip_v:
        v0, v1 = v1, v0
    return [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)]


def fx(x):          # model x -> pixel column on the front region
    return REG["front"][0] + (x - X0) * UNIT


def fy(y):          # model y -> pixel row on the front / back region (top of the cover is y = Y1)
    return REG["front"][1] + (Y1 - y) * UNIT


def bx(x):          # model x -> pixel column on the back region (seen from behind, so the east edge is the left)
    return REG["back"][0] + (X1 - x) * UNIT


def by(y):
    return REG["back"][1] + (Y1 - y) * UNIT


def sz(z):          # model z -> pixel column on the spine region
    r = REG["spine"]
    return r[0] + (z - Z0) / (Z1 - Z0) * (r[2] - r[0])


def front_face(x0, x1, y0, y1):
    """uv of the SOUTH face of a box that spans x0..x1, y0..y1, cut out of the front cover art at the same place."""
    return ruv("front", (round(fx(x0), 2), round(fy(y1), 2), round(fx(x1), 2), round(fy(y0), 2)))


def back_face(x0, x1, y0, y1):
    return ruv("back", (round(bx(x1), 2), round(by(y1), 2), round(bx(x0), 2), round(by(y0), 2)))


def spine_face(z0, z1):
    """uv of the WEST face of a spine step spanning z0..z1 (full height), cut out of the spine art at the same place (u runs north to south)."""
    r = REG["spine"]
    return ruv("spine", (round(sz(z0), 2), r[1], round(sz(z1), 2), r[3]))


def medal_face(x0, x1, y0, y1, cx=8.2, cy=8.4, half=2.75):
    """uv of the SOUTH face of a part of the medallion (a box that spans x0..x1, y0..y1 around the centre cx, cy of a medal 2*half wide)."""
    r = REG["medal"]
    k = (r[2] - r[0]) / (2 * half)
    return ruv("medal", (round(r[0] + (x0 - (cx - half)) * k, 2), round(r[1] + ((cy + half) - y1) * k, 2), round(r[0] + (x1 - (cx - half)) * k, 2), round(r[1] + ((cy + half) - y0) * k, 2)))


def page_east(z0, z1):
    """The fore-edge of a page slice spanning z0..z1 (u runs from the south, z1, to the north, z0)."""
    r = REG["pageV"]
    a = (9.3 - z1) * UNIT
    b = (9.3 - z0) * UNIT
    return ruv("pageV", (round(r[0] + a, 2), r[1], round(r[0] + b, 2), r[3]))


def page_top(z0, z1):
    r = REG["pageH"]
    a = (z0 - 6.7) * UNIT
    b = (z1 - 6.7) * UNIT
    return ruv("pageH", (r[0], round(r[1] + a, 2), r[2], round(r[1] + b, 2)))


def page_side():
    return ruv("pageH")


# --------------------------------------------------------------------------- the elements
def f(uv, tint=None):
    d = {"uv": uv, "texture": "#0"}
    return d


def cube(name, frm, to, faces, glow=False):
    e = {"name": name, "from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if glow:
        e["neoforge_data"] = dict(GLOW)
    return e


def all_faces(uv, names=("north", "south", "east", "west", "up", "down")):
    return {n: f(uv) for n in names}


def elements():
    E = []
    eV, eH = ruv("edgeV"), ruv("edgeH")
    metal, metal_d = ruv("metal"), ruv("metalDark")
    leather, strap = ruv("leather"), ruv("strap")
    # --- the front board and the stepped bevel on top of it (the "rounded" edge) ---------------------------------------------------------------------
    zf0, zf1 = Z1 - BEVEL - BOARD, Z1 - BEVEL                    # 9.3 .. 9.7
    E.append(cube("front_board", [X0, Y0, zf0], [X1, Y1, zf1], {
        "south": f(ruv("front")), "east": f(eV), "up": f(eH), "down": f(eH)}))
    bx0, bx1, by0, by1 = X0 + INSET, X1 - INSET, Y0 + INSET, Y1 - INSET
    E.append(cube("front_bevel", [bx0, by0, zf1], [bx1, by1, Z1], {
        "south": f(front_face(bx0, bx1, by0, by1)), "east": f(eV), "west": f(eV), "up": f(eH), "down": f(eH)}))
    # --- the back board and its bevel -------------------------------------------------------------------------------------------------------------
    zb0, zb1 = Z0 + BEVEL, Z0 + BEVEL + BOARD                    # 6.3 .. 6.7
    E.append(cube("back_board", [X0, Y0, zb0], [X1, Y1, zb1], {
        "north": f(ruv("back")), "east": f(eV), "up": f(eH), "down": f(eH)}))
    E.append(cube("back_bevel", [bx0, by0, Z0], [bx1, by1, zb0], {
        "north": f(back_face(bx0, bx1, by0, by1)), "east": f(eV), "west": f(eV), "up": f(eH), "down": f(eH)}))
    # --- the spine: three steps, wide to narrow, so the back of the book is round when seen from above --------------------------------------------
    steps = (("spine_1", 3.0, X0, Z0, Z1), ("spine_2", 2.7, 3.0, 6.35, 9.65), ("spine_3", 2.5, 2.7, 6.9, 9.1))
    for name, xa, xb, za, zb in steps:
        E.append(cube(name, [xa, Y0, za], [xb, Y1, zb], {"west": f(spine_face(za, zb)), "up": f(leather), "down": f(leather), "north": f(leather), "south": f(leather)}))
    # four raised bands across the back (the "bound tome" silhouette): each is the spine's own three-step profile grown by 0.05, so it is a ridge that follows the rounded back
    for i, (ya, yb) in enumerate(((3.7, 4.3), (6.4, 7.0), (9.0, 9.6), (11.7, 12.3))):
        for k, (name, xa, xb, za, zb) in enumerate(steps):
            E.append(cube("band_%d_%d" % (i + 1, k + 1), [xa - 0.05, ya, za - 0.05], [xb, yb, zb + 0.05],
                          all_faces(metal, ("west", "up", "down", "north", "south"))))
    # --- the page block: five slices, narrow to wide to narrow in z (and in how far the fore-edge reaches), so it bulges like a real, slightly opened book.
    #     Tops are disjoint (no z-fighting); the small steps between slices get a face only where they show.
    slices = (("pages_1", 6.7, 7.3, 12.5), ("pages_2", 7.3, 7.8, 12.7), ("pages_3", 7.8, 8.2, 12.8), ("pages_4", 8.2, 8.7, 12.7), ("pages_5", 8.7, 9.3, 12.5))
    for k, (name, za, zb, xe) in enumerate(slices):
        fc = {"east": f(page_east(za, zb)), "up": f(page_top(za, zb)), "down": f(page_top(za, zb))}
        if name in ("pages_2", "pages_3"):
            fc["north"] = f(page_side())
        if name in ("pages_3", "pages_4"):
            fc["south"] = f(page_side())
        E.append(cube(name, [X0, 2.0, za], [xe, 14.0, zb], fc))
    # --- the ribbon marker, hanging from the foot of the pages, with a notched tail -------------------------------------------------------------
    rb = ruv("ribbon")
    E.append(cube("ribbon", [7.0, -0.6, 7.7], [7.8, 2.4, 8.1], all_faces(rb, ("north", "south", "east", "west", "down"))))
    E.append(cube("ribbon_tail_a", [7.0, -1.2, 7.7], [7.3, -0.6, 8.1], all_faces(rb, ("north", "south", "east", "west", "down"))))
    E.append(cube("ribbon_tail_b", [7.5, -1.2, 7.7], [7.8, -0.6, 8.1], all_faces(rb, ("north", "south", "east", "west", "down"))))
    # --- the four corner caps (metal, a little proud of the boards on every side) -------------------------------------------------------------------
    caps = {"top_west": ([3.3, 12.6, 5.9], [5.4, 14.6, 10.12]), "bottom_west": ([3.3, 1.4, 5.9], [5.4, 3.4, 10.12]),
            "top_east": ([11.0, 12.6, 5.9], [13.1, 14.6, 10.12]), "bottom_east": ([11.0, 1.4, 5.9], [13.1, 3.4, 10.12])}
    for name, (a, b) in caps.items():
        E.append(cube("cap_" + name, a, b, all_faces(metal)))
    # --- a thin raised metal frame round the front cover -------------------------------------------------------------------------------------------
    for name, a, b in (("frame_top", [4.0, 13.7, 10.0], [12.4, 13.9, 10.07]), ("frame_bottom", [4.0, 2.1, 10.0], [12.4, 2.3, 10.07]),
                       ("frame_west", [4.0, 2.3, 10.0], [4.2, 13.7, 10.07]), ("frame_east", [12.2, 2.3, 10.0], [12.4, 13.7, 10.07])):
        E.append(cube(name, a, b, all_faces(metal, ("south", "up", "down", "east", "west"))))
    # --- the medallion: a plus-shaped octagon (two overlapping boxes), the emblem is painted on it and a full-bright quad lies on top ------------------
    E.append(cube("medal_wide", [5.45, 6.65, 10.0], [10.95, 10.15, 10.28], dict(
        {"south": f(medal_face(5.45, 10.95, 6.65, 10.15))}, **{n: f(metal) for n in ("up", "down", "east", "west")})))
    E.append(cube("medal_tall", [6.45, 5.65, 10.0], [9.95, 11.15, 10.28], dict(
        {"south": f(medal_face(6.45, 9.95, 5.65, 11.15))}, **{n: f(metal) for n in ("up", "down", "east", "west")})))
    E.append(cube("emblem_glow", [5.7, 5.9, 10.28], [10.7, 10.9, 10.33], {"south": f(ruv("glow"))}, glow=True))
    # --- the clasp: a leather strap round the fore-edge, a buckle plate with a glowing stone on the front cover ------------------------------------------
    E.append(cube("strap_front", [10.9, 7.2, 10.0], [13.1, 8.8, 10.2], {"south": f(strap), "up": f(strap), "down": f(strap)}))
    E.append(cube("strap_edge", [13.0, 7.2, 5.8], [13.2, 8.8, 10.2], {"east": f(strap), "up": f(strap), "down": f(strap), "north": f(strap), "south": f(strap)}))
    E.append(cube("strap_back", [10.9, 7.2, 5.8], [13.1, 8.8, 6.0], {"north": f(strap), "up": f(strap), "down": f(strap), "west": f(strap)}))
    E.append(cube("strap_tip", [10.75, 7.1, 9.95], [11.1, 8.9, 10.22], all_faces(metal, ("south", "west", "up", "down"))))
    E.append(cube("buckle_plate", [11.6, 6.9, 10.2], [12.6, 9.1, 10.36], all_faces(metal, ("south", "east", "west", "up", "down"))))
    E.append(cube("buckle_stone", [11.8, 7.5, 10.36], [12.4, 8.5, 10.44], all_faces(ruv("gem"), ("south", "east", "west", "up", "down")), glow=True))
    return E


# --------------------------------------------------------------------------- the display transforms
# Blockbench style: rotation degrees, translation in 1/16 block (the game clamps to +-80), scale (clamped to 4).  The maths and the reasons are in docs/canon/canon_grimoire.md.
# Left-hand entries have the SAME numbers as the right-hand ones on purpose: ItemTransform.apply negates tx, ry and rz of a left-hand context by itself, so equal numbers
# give the mirrored pose (the cover turned outward on both sides).
_TP = {"rotation": [47, 120, 12], "translation": [-1.21, 0.96, 1.1], "scale": [0.55, 0.55, 0.55]}
_FP = {"rotation": [-19, -13, 6], "translation": [-2.63, 4.79, -0.49], "scale": [0.38, 0.38, 0.38]}
DISPLAY = {
    "thirdperson_righthand": _TP,
    "thirdperson_lefthand": _TP,
    "firstperson_righthand": _FP,
    "firstperson_lefthand": _FP,
    "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "gui": {"rotation": [20, -155, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 12, 0], "scale": [0.8, 0.8, 0.8]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
}


def body_model(ident):
    """The 3D body of a book (no loader): elements, textures and the display block."""
    return {
        "_comment": "GENERATED by tools/canon/gen_canon_models.py (geometry in tools/canon/canon_book.py). Do not edit by hand.",
        "texture_size": [W, H],
        "textures": {"0": "nusmp:item/charter_grimoire_" + ident, "particle": "nusmp:item/charter_grimoire_%s_icon" % ident},
        "elements": elements(),
        "display": DISPLAY,
    }


def wrapper_model(ident):
    """The registered model: the 3D body in every context but the GUI, where the old flat icon stays (neoforge:separate_transforms, as skill_book.json)."""
    return {
        "_comment": "GENERATED by tools/canon/gen_canon_models.py. The 3D body is charter_grimoire_%s_3d.json; the inventory shows the old sprite (_icon)." % ident,
        "loader": "neoforge:separate_transforms",
        "gui_light": "front",
        "textures": {"particle": "nusmp:item/charter_grimoire_%s_icon" % ident},
        "base": {"parent": "nusmp:item/charter_grimoire_%s_3d" % ident},
        "perspectives": {"gui": {"parent": "nusmp:item/charter_grimoire_%s_icon" % ident}},
    }


def _check_regions():
    names = list(REG)
    for i, a in enumerate(names):
        ra = REG[a]
        assert 0 <= ra[0] < ra[2] <= W and 0 <= ra[1] < ra[3] <= H, a
        for b in names[i + 1:]:
            rb = REG[b]
            assert ra[2] <= rb[0] or rb[2] <= ra[0] or ra[3] <= rb[1] or rb[3] <= ra[1], "regions overlap: %s %s" % (a, b)


_check_regions()
