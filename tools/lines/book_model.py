"""The geometry, the texture layout and the display transforms of the 3D Skill Book (1.82.0).

Shared by gen_book_textures.py (which draws the textures and writes the model files) and preview_book.py (which renders the model json back to a picture), so the model, the
atlas the generator paints and the picture the previewer shows can never disagree about where a face looks in the texture.

The book stands upright in model space (16 units = one block): the FRONT cover looks SOUTH (+Z, like the front of an item sprite), the spine is on the WEST (-X) edge, the fore-edge
(pages) on the EAST. 9 wide, 13 high, 4 thick. See docs/skilltree/lines_book_art.md for the reasons behind every number.
"""

# --------------------------------------------------------------------------- the texture layout
# base atlas (64 x 64 px, 4 px = 1 uv unit): everything that is NOT the cover and is NOT tinted by the line colour (except the ribbon, which is a grey the client tints)
BASE_SIZE = 64
BASE_REGIONS = {                      # name -> (x0, y0, x1, y1) in pixels
    "pageV": (0, 0, 16, 16),          # fore-edge: the pages seen from the side, vertical stripes
    "pageH": (16, 0, 32, 16),         # head and tail: the pages seen from above / below, horizontal stripes
    "paper": (0, 16, 16, 32),         # plain paper
    "ribbon": (16, 16, 24, 32),       # a grey cloth, tinted by the client (tint index 1)
    "gem": (24, 16, 32, 24),          # the clasp's stone
    "metal": (32, 0, 64, 32),         # gold plate with a bevel and rivets (corner caps, bands, clasp)
    "metalDark": (32, 32, 64, 64),    # darker bronze (the clasp's catch and the bands' inner side)
}
# franchise cover (128 x 128 px, 8 px = 1 uv unit): ONLY greys, so that the tint of the line colour is clean
COVER_SIZE = 128
COVER_REGIONS = {
    "front": (0, 0, 72, 104),         # 9 x 13 units: the cover with the embossed frame and the emblem
    "spine": (72, 0, 104, 104),       # 4 x 13 units
    "edgeH": (0, 104, 72, 112),       # 9 x 1 unit: the cover's top and bottom edge
    "edgeV": (104, 0, 112, 104),      # 1 x 13 units: the cover's fore-edge
}


def uv(size, rect, flip_u=False, flip_v=False):
    """Pixel rect -> the [u0, v0, u1, v1] of a model face (the model's uv space is 0..16 over the whole texture)."""
    k = 16.0 / size
    u0, v0, u1, v1 = rect[0] * k, rect[1] * k, rect[2] * k, rect[3] * k
    if flip_u:
        u0, u1 = u1, u0
    if flip_v:
        v0, v1 = v1, v0
    return [round(u0, 4), round(v0, 4), round(u1, 4), round(v1, 4)]


def base_uv(name, **kw):
    return uv(BASE_SIZE, BASE_REGIONS[name], **kw)


def cover_uv(name, **kw):
    return uv(COVER_SIZE, COVER_REGIONS[name], **kw)


# --------------------------------------------------------------------------- the elements
def face(tex, uvr, tint=None):
    f = {"uv": uvr, "texture": tex}
    if tint is not None:
        f["tintindex"] = tint
    return f


def cube(name, frm, to, faces):
    return {"name": name, "from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}


def elements():
    E = []
    X0, X1, Y0, Y1, Z0, Z1 = 3.5, 12.5, 1.5, 14.5, 6.0, 10.0           # the book's box
    cover_t = 0.8
    CF = "#cover"
    BA = "#base"
    metal = lambda: face(BA, base_uv("metal"))
    metal_dark = lambda: face(BA, base_uv("metalDark"))
    # --- the two boards ------------------------------------------------------------------------------------------------------
    E.append(cube("front_board", [X0, Y0, Z1 - cover_t], [X1, Y1, Z1], {
        "south": face(CF, cover_uv("front"), 1),
        "east": face(CF, cover_uv("edgeV"), 1),
        "up": face(CF, cover_uv("edgeH"), 1),
        "down": face(CF, cover_uv("edgeH"), 1)}))
    E.append(cube("back_board", [X0, Y0, Z0], [X1, Y1, Z0 + cover_t], {
        "north": face(CF, cover_uv("front"), 1),
        "east": face(CF, cover_uv("edgeV"), 1),
        "up": face(CF, cover_uv("edgeH"), 1),
        "down": face(CF, cover_uv("edgeH"), 1)}))
    # --- the spine: three steps, wide to narrow, so that the back of the book is round when seen from above ---------------------
    E.append(cube("spine_1", [3.0, Y0, 6.2], [X0, Y1, 9.8], {
        "west": face(CF, cover_uv("spine"), 1), "up": face(CF, cover_uv("edgeH"), 1), "down": face(CF, cover_uv("edgeH"), 1)}))
    E.append(cube("spine_2", [2.75, Y0, 6.6], [3.0, Y1, 9.4], {
        "west": face(CF, cover_uv("spine"), 1), "up": face(CF, cover_uv("edgeH"), 1), "down": face(CF, cover_uv("edgeH"), 1)}))
    E.append(cube("spine_3", [2.6, Y0, 7.2], [2.75, Y1, 8.8], {
        "west": face(CF, cover_uv("spine"), 1), "up": face(CF, cover_uv("edgeH"), 1), "down": face(CF, cover_uv("edgeH"), 1)}))
    # three raised bands across the spine
    for i, (y0, y1) in enumerate(((2.9, 3.7), (7.1, 7.9), (12.3, 13.1))):
        E.append(cube("spine_band_%d" % (i + 1), [2.5, y0, 6.1], [3.5, y1, 9.9], {
            "west": metal(), "up": metal(), "down": metal(), "north": metal(), "south": metal()}))
    # --- the page block: three boxes, narrow to wide to narrow again in thickness, so the fore-edge bulges like a real, slightly open book ---
    pv, ph = face(BA, base_uv("pageV")), face(BA, base_uv("pageH"))
    E.append(cube("pages_1", [3.7, 2.1, 6.8], [11.9, 13.9, 9.2], {"east": pv, "up": ph, "down": ph}))
    E.append(cube("pages_2", [3.7, 2.1, 7.2], [12.1, 13.9, 8.8], {"east": pv, "up": ph, "down": ph}))
    E.append(cube("pages_3", [3.7, 2.1, 7.7], [12.3, 13.9, 8.3], {"east": pv, "up": ph, "down": ph}))
    # --- the ribbon marker, hanging out of the foot of the pages (tint index 1: the line colour) -------------------------------
    rib = lambda: face(BA, base_uv("ribbon"), 1)
    E.append(cube("ribbon", [6.9, -0.4, 7.5], [7.9, 2.3, 7.9], {"north": rib(), "south": rib(), "east": rib(), "west": rib(), "down": rib()}))
    E.append(cube("ribbon_tail", [6.9, -1.1, 7.5], [7.4, -0.4, 7.9], {"north": rib(), "south": rib(), "east": rib(), "west": rib(), "down": rib()}))
    # --- the four corner caps (metal, a little proud of the boards on every side) ---------------------------------------------
    caps = {"top_west": ([3.4, 12.3, 5.7], [5.6, 14.7, 10.3]), "bottom_west": ([3.4, 1.3, 5.7], [5.6, 3.7, 10.3]),
            "top_east": ([10.4, 12.3, 5.7], [12.7, 14.7, 10.3]), "bottom_east": ([10.4, 1.3, 5.7], [12.7, 3.7, 10.3])}
    for name, (a, b) in caps.items():
        E.append(cube("cap_" + name, a, b, {d: metal() for d in ("north", "south", "east", "west", "up", "down")}))
    # --- the clasp on the fore-edge: a strap round the edge, a tongue on the front cover, a catch, a stone ---------------------
    E.append(cube("clasp_strap", [12.45, 6.3, 5.75], [12.85, 9.7, 10.25], {d: metal() for d in ("north", "south", "east", "up", "down")}))
    E.append(cube("clasp_tongue", [10.7, 6.3, 10.0], [12.85, 9.7, 10.35], {d: metal() for d in ("south", "east", "up", "down", "west")}))
    E.append(cube("clasp_back_plate", [11.6, 6.3, 5.65], [12.85, 9.7, 6.0], {d: metal() for d in ("north", "east", "up", "down", "west")}))
    E.append(cube("clasp_catch", [12.85, 7.4, 7.3], [13.15, 8.6, 8.7], {d: metal_dark() for d in ("north", "south", "east", "up", "down")}))
    gem = face(BA, base_uv("gem"))
    E.append(cube("clasp_stone", [11.3, 7.3, 10.35], [12.2, 8.7, 10.7], {d: gem for d in ("south", "east", "west", "up", "down")}))
    return E


def base_model(cover_texture):
    """The 3D body of the book (no loader): elements, textures and the display block."""
    return {
        "_comment": "GENERATED by tools/lines/gen_book_textures.py (geometry in tools/lines/book_model.py). Do not edit by hand.",
        "textures": {"base": "nusmp:item/skill_book_3d/base", "cover": cover_texture, "particle": "nusmp:item/skill_book_3d/base"},
        "elements": elements(),
        "display": DISPLAY,
    }


# --------------------------------------------------------------------------- the display transforms (Blockbench style: rotation degrees, translation in 1/16 block, scale)
# Left-hand entries have the SAME numbers as the right-hand ones on purpose: ItemTransform.apply negates tx, ry and rz of a left-hand context by itself, so equal numbers
# give the mirrored pose (cover turned outward on both sides). Tuned with preview_book.py; the reasons are in docs/skilltree/lines_book_art.md section 3.
_TP = {"rotation": [60, 150, 0], "translation": [0.0, 1.0, 3.0], "scale": [0.5, 0.5, 0.5]}
_FP = {"rotation": [-15, -15, 15], "translation": [-2.9, 4.3, -1.3], "scale": [0.42, 0.42, 0.42]}
DISPLAY = {
    "thirdperson_righthand": _TP,
    "thirdperson_lefthand": _TP,
    "firstperson_righthand": _FP,
    "firstperson_lefthand": _FP,
    "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
    "gui": {"rotation": [20, -155, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]},
}
