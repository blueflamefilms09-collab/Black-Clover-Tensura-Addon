#!/usr/bin/env python3
"""
Rooms rebuilt, part 4: the walls. Every wall is divided into bays by the module grid (9 blocks): a pair of pilasters stands on every module border, the middle of
every bay holds either the door (when the shipped room has a socket there) or a feature panel (a niche with a lantern, a banner niche, bookshelves, a glowing slit
window, a tomb niche, a shrine niche, a barred cell, a forge niche, casks ...), and the strips between get a dado rail and a frieze. The four corners get piers.
Everything is placed only where the shipped room left the wall plain: a cell that holds something (a bookshelf, a window, a banner, a torch), a cell in a door
apron and a cell in front of which a prop stands is skipped, so no prop is ever left hanging and no doorway is narrowed. All masonry is palette masonry.
"""
import mega_lib as L
from rr_core import OPP, DV, FR, G, is_palette

P, C, B, T = L.POLISHED, L.CHISELED, L.BRICK, L.TILE
LANTERN, SOUL_LANTERN = L.LANTERN, L.SOUL_LANTERN


class Wall:
    def __init__(self, room, side):
        self.room, self.side = room, side
        self.into = room.into(side)
        self.out = OPP[self.into]
        self.inc = "east" if side in ("n", "s") else "south"
        self.dec = OPP[self.inc]
        self.length = room.lat_len(side)
        self.m = self.length // 9

    def cell(self, lat, d, y):
        return self.room.wcell(self.side, lat, d, y)

    def rtop(self, lat):
        """The row just under the roof in front of this wall at lateral position lat."""
        r = self.room
        x, y, z = self.cell(lat, 2, G)
        x = min(max(x, r.x0), r.x1)
        z = min(max(z, r.z0), r.z1)
        return r.roofmap[(x, z)] - 1

    def plain(self, lat, d, y):
        """True when the shipped room left this wall cell as plain masonry (or nothing) and it is not locked."""
        r = self.room
        c = self.cell(lat, d, y)
        if c in r.locked or c in r.reserved:
            return False
        cell = r.cells.get(c)
        return cell is None or (is_palette(cell.block) and not cell.block.endswith(("_stairs", "_slab", "_wall")))

    def free_front(self, lat0, lat1, y0, y1, depth=2):
        r = self.room
        for lat in range(lat0, lat1 + 1):
            for y in range(y0, y1 + 1):
                if not r.free(*self.cell(lat, depth, y)):
                    return False
        return True

    def put(self, lat, d, y, block, props=None, over=False):
        return self.room.put(*self.cell(lat, d, y), block, props, over=over)

    def force(self, lat, d, y, block, props=None):
        return self.room.force(*self.cell(lat, d, y), block, props)

    def carve(self, lat, d, y):
        c = self.cell(lat, d, y)
        if c in self.room.locked or c in self.room.reserved:
            return False
        cell = self.room.cells.get(c)
        if cell is not None and not is_palette(cell.block):
            return False
        self.room.cells.pop(c, None)
        return True

    def stair(self, lat, d, y, block, facing, half="bottom"):
        return self.room.stair(*self.cell(lat, d, y), block, facing, half)

    def door_lats(self):
        return [lat for side, lat, _ in self.room.doors if side == self.side]


# ---------------------------------------------------------------------------------------------------------------------------------- pilasters
def pilaster(room, side, lat0, style="engaged", shaft=P, cap=C):
    """A two-wide pilaster on the module border: the inner wall layer becomes `shaft`; 'engaged' leaves it flush, 'standing' also carries it one cell into the room
    (with a stair base and a flared capital) where that is free. Returns the number of cells written."""
    w = Wall(room, side)
    n = 0
    for lat in (lat0, lat0 + 1):
        if not (0 < lat < w.length - 1):
            return 0
        if not w.plain(lat, 1, G):
            return 0
    protrude = style == "standing" and all(room.free(*w.cell(lat, d, y)) for lat in (lat0, lat0 + 1) for d in (2, 3) for y in (G, G + 1, w.rtop(lat) - 1, w.rtop(lat)))
    for lat in (lat0, lat0 + 1):
        top = w.rtop(lat)
        for y in range(G, top + 1):
            if not w.plain(lat, 1, y):
                continue
            blk = cap if (y == top or y == G + 3 and style == "engaged") else shaft
            if w.force(lat, 1, y, blk):
                n += 1
        if protrude:
            for y in range(G, top + 1):
                if room.free(*w.cell(lat, 2, y)):
                    if w.put(lat, 2, y, cap if y == top else shaft):
                        n += 1
            if w.stair(lat, 3, G, L.POLISHED_STAIRS, w.out, "bottom"):
                n += 1
            if top > G + 4 and w.stair(lat, 3, top, L.POLISHED_STAIRS, w.out, "top"):
                n += 1
    return n


def corner_pier(room, cx, cz, dx, dz, top_of=None):
    """A 2 x 2 pier in an interior corner: (cx, cz) is the corner cell, (dx, dz) the direction into the room."""
    cells = [(cx + i * dx, cz + j * dz) for i in (0, 1) for j in (0, 1)]
    tops = [room.roofmap[c] - 1 for c in cells]
    top = min(tops)
    for (x, z) in cells:
        for y in range(G, top + 1):
            if not room.free(x, y, z):
                return 0
    n = 0
    for (x, z) in cells:
        for y in range(G, top + 1):
            if room.put(x, y, z, C if y == top else P):
                n += 1
    return n


# ---------------------------------------------------------------------------------------------------------------------------------- string courses
def courses(room, dado=True, frieze=True, mid=False, accent=C):
    """A dado rail (row G + 2) and a frieze (the row under the roof) along the inner layer of every wall, on plain masonry only; `mid` adds a second rail halfway."""
    n = 0
    for side in ("n", "s", "w", "e"):
        w = Wall(room, side)
        for lat in range(2, w.length - 2):
            if dado and w.plain(lat, 1, G + 2):
                if w.force(lat, 1, G + 2, P if lat % 3 else accent):
                    n += 1
            if frieze:
                top = w.rtop(lat)
                if top > G + 6 and w.plain(lat, 1, top):
                    if w.force(lat, 1, top, accent if lat % 2 == 0 else P):
                        n += 1
            if mid:
                top = w.rtop(lat)
                y = (G + top) // 2
                if y > G + 4 and w.plain(lat, 1, y):
                    if w.force(lat, 1, y, P):
                        n += 1
    return n


# ---------------------------------------------------------------------------------------------------------------------------------- feature panels
def _frame(w, c, y0, y1, half=1, front_from=G):
    """The recess (inner layer, lat c-half..c+half, rows y0..y1) with a polished frame: sill, lintel and jambs. Returns False when the wall there is not plain."""
    for lat in range(c - half - 1, c + half + 2):
        for y in range(y0 - 1, y1 + 2):
            if not w.plain(lat, 1, y):
                return False
    if not w.free_front(c - half, c + half, front_from, y1 + 1):
        return False
    for lat in range(c - half, c + half + 1):
        for y in range(y0, y1 + 1):
            w.carve(lat, 1, y)
    for lat in range(c - half - 1, c + half + 2):
        for y in range(y0 - 1, y1 + 2):
            edge = lat in (c - half - 1, c + half + 1) or y in (y0 - 1, y1 + 1)
            if edge:
                w.force(lat, 1, y, P if (lat != c or y != y1 + 1) else C)
    return True


#: block entities each niche kind places (banners, skulls, barrels, pots, a furnace): the rebuild adds a handful per room at most
BE_NEED = {"lantern": 1, "banner": 4, "tomb": 3, "shrine": 1, "grate": 1, "forge": 2, "casks": 5}


def niche(room, side, c, kind, rng, color="red", soul=False):
    """A feature panel centred on lateral position c of `side`. kinds: lantern, banner, books, slit, tomb, shrine, grate, forge, casks, plain. Returns True when built."""
    w = Wall(room, side)
    lamp = SOUL_LANTERN if soul else LANTERN
    top = w.rtop(c)
    if room.be_added + BE_NEED.get(kind, 0) > room.be_cap:
        return False                                  # the niche needs block entities (banners, skulls, barrels, a furnace) and the room's budget is spent
    if kind == "lantern":
        if not _frame(w, c, G + 1, G + 4):
            return False
        w.put(c, 1, G + 1, lamp, {"hanging": "false", "waterlogged": "false"})
        w.put(c - 1, 1, G + 1, "minecraft:white_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
        w.put(c + 1, 1, G + 1, "minecraft:white_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
        w.put(c, 1, G + 3, "minecraft:%s_wall_banner" % color, {"facing": w.into})
        return True
    if kind == "banner":
        if not _frame(w, c, G + 1, G + 5):
            return False
        w.put(c, 1, G + 4, "minecraft:%s_wall_banner" % color, {"facing": w.into})
        w.put(c, 1, G + 3, "minecraft:%s_wall_banner" % color, {"facing": w.into})
        w.put(c - 1, 1, G + 4, "minecraft:black_wall_banner", {"facing": w.into})
        w.put(c + 1, 1, G + 4, "minecraft:black_wall_banner", {"facing": w.into})
        w.put(c, 1, G + 1, "minecraft:red_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
        return True
    if kind == "books":
        for lat in range(c - 2, c + 3):
            for y in range(G + 1, G + 5):
                if not w.plain(lat, 1, y):
                    return False
        for lat in range(c - 1, c + 2):
            for y in range(G + 1, G + 5):
                w.force(lat, 1, y, "minecraft:bookshelf")
        for y in range(G + 1, G + 5):
            w.force(c - 2, 1, y, P)
            w.force(c + 2, 1, y, P)
        return True
    if kind == "slit":
        for lat in range(c - 1, c + 2):
            for y in range(G + 2, G + 6):
                if not w.plain(lat, 1, y) or not w.plain(lat, 0, y):
                    return False
        if not w.free_front(c, c, G + 2, G + 5):
            return False
        glow = rng.choice(["minecraft:sea_lantern", "minecraft:shroomlight", "minecraft:ochre_froglight"])
        pane = "minecraft:%s_stained_glass_pane" % rng.choice(["orange", "yellow", "light_blue", "purple", "red"])
        for y in range(G + 2, G + 6):
            w.force(c, 0, y, glow)
            w.force(c, 1, y, pane)
            w.force(c - 1, 1, y, P)
            w.force(c + 1, 1, y, P)
        w.force(c, 1, G + 6, C)
        return True
    if kind == "tomb":
        if not _frame(w, c, G + 1, G + 3):
            return False
        for lat in range(c - 1, c + 2):
            w.put(lat, 1, G + 1, "minecraft:bone_block")
            w.put(lat, 1, G + 2, "minecraft:skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[w.into]), "powered": "false"})
        w.put(c, 1, G + 3, "minecraft:cobweb")
        for lat in range(c - 1, c + 2):
            w.put(lat, 2, G + 1, L.IRON_BARS)
            w.put(lat, 2, G + 2, L.IRON_BARS)
            w.put(lat, 2, G + 3, L.IRON_BARS)
        return True
    if kind == "shrine":
        if not _frame(w, c, G + 1, G + 4):
            return False
        w.put(c, 1, G + 1, L.POLISHED_SLAB, {"type": "top", "waterlogged": "false"})          # a top slab: a candle needs a flat top face to stand on
        w.put(c - 1, 1, G + 1, L.POLISHED_STAIRS, {"facing": w.dec, "half": "bottom", "shape": "straight", "waterlogged": "false"})
        w.put(c + 1, 1, G + 1, L.POLISHED_STAIRS, {"facing": w.inc, "half": "bottom", "shape": "straight", "waterlogged": "false"})
        w.put(c, 1, G + 2, "minecraft:%s_candle" % rng.choice(["white", "red", "purple"]), {"candles": "4", "lit": "true", "waterlogged": "false"})
        w.put(c, 1, G + 4, "minecraft:%s_wall_banner" % color, {"facing": w.into})
        return True
    if kind == "grate":
        if not _frame(w, c, G + 1, G + 4):
            return False
        for lat in range(c - 1, c + 2):
            for y in range(G + 1, G + 5):
                w.put(lat, 2, y, L.IRON_BARS)
        w.put(c, 1, G + 4, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        w.put(c, 1, G + 3, SOUL_LANTERN if soul else LANTERN, {"hanging": "true", "waterlogged": "false"})
        w.put(c, 1, G + 1, "minecraft:skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[w.into]), "powered": "false"})
        return True
    if kind == "forge":
        if not _frame(w, c, G + 1, G + 4):
            return False
        w.put(c, 1, G + 1, "minecraft:blast_furnace", {"facing": w.into, "lit": "false"})
        w.put(c - 1, 1, G + 1, "minecraft:anvil", {"facing": w.inc})
        w.put(c + 1, 1, G + 1, "minecraft:grindstone", {"face": "floor", "facing": w.into})
        w.put(c, 1, G + 2, "minecraft:orange_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
        w.put(c, 1, G + 4, "minecraft:black_wall_banner", {"facing": w.into})
        return True
    if kind == "casks":
        if not _frame(w, c, G + 1, G + 3):
            return False
        for lat in range(c - 1, c + 2):
            w.put(lat, 1, G + 1, "minecraft:barrel", {"facing": w.into, "open": "false"})
        w.put(c - 1, 1, G + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
        w.put(c + 1, 1, G + 2, "minecraft:decorated_pot")
        return True
    return False


# ---------------------------------------------------------------------------------------------------------------------------------- door crowns
def door_crown(room, side, lat, banner="red"):
    """The head of a door above the cut: rows 16 and up over the wall layers, a stepped pediment (7, 5, 3 polished blocks) with a chiselled keystone and a banner on each
    side. Only the inner layer (the room's face) is touched."""
    w = Wall(room, side)
    top = w.rtop(lat)
    if top < G + 12:
        return 0
    n = 0
    y0 = 16
    rows = [(y0, 3, P), (y0 + 1, 2, P), (y0 + 2, 1, C)]
    for y, half, blk in rows:
        if y > top - 1:
            continue
        for dl in range(-half, half + 1):
            if w.plain(lat + dl, 1, y):
                if w.force(lat + dl, 1, y, C if (dl == 0 and y > y0) else blk):
                    n += 1
    for sgn in (-1, 1):
        if top > y0 + 3 and w.free_front(lat + 3 * sgn, lat + 3 * sgn, y0, y0 + 1):
            w.put(lat + 3 * sgn, 2, y0, "minecraft:%s_wall_banner" % banner, {"facing": w.into})
    return n


# ---------------------------------------------------------------------------------------------------------------------------------- pillars
def pillar(room, x, z, size=2):
    """A free-standing pillar (size x size) from the floor to the roof: a polished shaft with a chiselled band, a stair base and a flared stair capital round it. It is only
    built where the whole shaft and the ring round its foot are free (no furniture, no marker, no door apron), so a room never loses a prop or a walking lane to it."""
    cells = [(x + i, z + j) for i in range(size) for j in range(size)]
    ring = [(a, b) for a in range(x - 1, x + size + 1) for b in range(z - 1, z + size + 1) if (a, b) not in cells]
    for (a, b) in cells + ring:
        if not room.inside(a, b) or (a, b) not in room.roofmap and (a, b) not in cells:
            return 0
    top = min(room.roofmap[c] for c in cells) - 1
    if top < G + 6:
        return 0
    for (a, b) in cells:
        for y in range(G, top + 1):
            if not room.free(a, y, b):
                return 0
    for (a, b) in ring:
        for y in (G, G + 1, G + 2):
            if not room.free(a, y, b):
                return 0
    n = 0
    for (a, b) in cells:
        for y in range(G, top + 1):
            blk = C if y in (G + 3, top) else P
            if room.put(a, y, b, blk):
                n += 1
    for (a, b) in ring:
        if a < x and z <= b < z + size:
            face = "east"
        elif a >= x + size and z <= b < z + size:
            face = "west"
        elif b < z and x <= a < x + size:
            face = "south"
        elif b >= z + size and x <= a < x + size:
            face = "north"
        else:
            face = None                                         # a corner of the ring: a plain slab
        if face is None:
            room.put(a, G, b, L.POLISHED_SLAB, {"type": "bottom", "waterlogged": "false"})
            if top > G + 6 and room.free(a, top, b):
                room.put(a, top, b, L.POLISHED_SLAB, {"type": "top", "waterlogged": "false"})
        else:
            room.stair(a, G, b, L.POLISHED_STAIRS, face, "bottom")
            if top > G + 6 and room.free(a, top, b):
                room.stair(a, top, b, L.POLISHED_STAIRS, face, "top")
        n += 1
    return n


def pillars(room, size=2):
    """Pillars at the crossings of the module borders (x and z = 9 k - 1 .. 9 k): where the ribs of a groin vault meet. Returns how many were built."""
    n = 0
    for i in range(1, room.W // 9):
        for j in range(1, room.D // 9):
            if pillar(room, 9 * i - 1, 9 * j - 1, size):
                n += 1
    return n


# ---------------------------------------------------------------------------------------------------------------------------------- balconies
def balcony(room, side, y0=G + 8, depth=2, shelves=False, rail="minecraft:iron_bars"):
    """A gallery along the wall of `side`: a ledge `depth` cells deep at row y0, resting on the pilasters and a stair corbel course under it, a railing on its edge, and over it
    bookshelves (`shelves`) or the plain wall. Built only on the stretches where the whole box (rows y0 - 1 .. y0 + 3) is free and out of the door aprons and the room is tall
    enough. Returns the number of ledge cells."""
    w = Wall(room, side)
    n = 0
    for lat in range(3, w.length - 3):
        top = w.rtop(lat)
        if top < y0 + 4:
            continue
        cells = [w.cell(lat, 2 + d, y) for d in range(depth) for y in (y0 - 1, y0, y0 + 1, y0 + 2)]
        if not all(room.free(*c) for c in cells):
            continue
        for d in range(depth):
            w.put(lat, 2 + d, y0, L.PLANKS_SLAB, {"type": "bottom", "waterlogged": "false"})
            if d == 0:
                w.stair(lat, 2, y0 - 1, L.POLISHED_STAIRS, w.out, "top")
        w.put(lat, 2 + depth - 1, y0 + 1, rail)
        n += 1
        if shelves and w.plain(lat, 1, y0 + 1):
            for y in range(y0 + 1, min(top, y0 + 4)):
                if w.plain(lat, 1, y):
                    w.force(lat, 1, y, "minecraft:bookshelf")
    return n
