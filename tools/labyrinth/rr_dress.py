#!/usr/bin/env python3
"""
Rooms rebuilt, part 6: wear and dressing.

wear:      cracks (cracked bricks and tiles), moss (moss blocks, moss carpet, vines on the wall faces), cobwebs under the vault, heaps of rubble at the foot of the walls,
           broken ribs. Wear is only ever drawn in cells that are free and not locked, so a doorway, a marker or a route is never narrowed by it.
stands:    the armour stands of the shipped room are not carried into a rebuilt one (the owner's decision: stands are a rare prop, not a set decoration, see
           labyrinth/props/ArmourStandRarity). Where one stood, a plinth prop (candles, a lantern or a skull) takes its place. Rooms of the armoury, barracks, guard,
           training and treasure types get exactly one empty stand SLOT (a lit niche with a plinth): the generator fills it only for the few rooms the rarity roll picks.
"""
import mega_lib as L
import nbt
from rr_core import FR, G, DIRS, DV, OPP, is_palette
from rr_wall import Wall, _frame

YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}


def crack(room, wall=0.10, floor=0.08, roof=0.05):
    rng = room.rng
    n = 0
    for (x, y, z), c in list(room.cells.items()):
        if y < FR or (x, y, z) in room.locked:
            continue
        if c.block == L.BRICK and rng.random() < wall:
            room.cells[(x, y, z)] = type(c)(L.CRACKED_BRICK, {}, None)
            n += 1
        elif c.block == L.TILE:
            p = floor if y == FR else roof
            if rng.random() < p:
                room.cells[(x, y, z)] = type(c)(L.CRACKED_TILE, {}, None)
                n += 1
    return n


def moss(room, patches=3, radius=3, density=0.7, vines=0.25):
    rng = room.rng
    n = 0
    for _ in range(patches):
        side = rng.choice(["n", "s", "w", "e"])
        lat = rng.randrange(3, room.lat_len(side) - 3)
        cx, cy, cz = room.wcell(side, lat, 2, G)
        for dz in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                x, z = cx + dx, cz + dz
                if not room.inside(x, z) or dx * dx + dz * dz > radius * radius or rng.random() > density:
                    continue
                f = room.cells.get((x, FR, z))
                if f is None or not is_palette(f.block):
                    continue
                if (x, G, z) in room.locked or (x, G, z) in room.cells:
                    continue
                if rng.random() < 0.5:
                    room.force(x, FR, z, "minecraft:moss_block")
                if rng.random() < 0.6:
                    room.put(x, G, z, "minecraft:moss_carpet")
                    n += 1
    # vines on the wall faces under the roof
    for side in ("n", "s", "w", "e"):
        w = Wall(room, side)
        for lat in range(3, w.length - 3):
            if rng.random() > vines * 0.15:
                continue
            top = w.rtop(lat)
            wall_cell = room.cells.get(w.cell(lat, 1, top - 1))
            if wall_cell is None or not L.is_full(wall_cell.block):
                continue
            face = OPP[room.into(side)]
            for k in range(rng.randrange(2, 6)):
                c = w.cell(lat, 2, top - 1 - k)
                if not room.free(*c) or room.cells.get(w.cell(lat, 1, top - 1 - k)) is None:
                    break
                if not L.is_full(room.cells[w.cell(lat, 1, top - 1 - k)].block):
                    break
                room.put(*c, "minecraft:vine", {face: "true", "up": "false", "north": "true" if face == "north" else "false",
                                                  "south": "true" if face == "south" else "false", "east": "true" if face == "east" else "false",
                                                  "west": "true" if face == "west" else "false"})
                n += 1
    return n


def cobwebs(room, count=6):
    rng = room.rng
    n = 0
    tries = 0
    while n < count and tries < count * 20:
        tries += 1
        x = rng.choice([room.x0, room.x1, rng.randrange(room.x0, room.x1 + 1)])
        z = rng.choice([room.z0, room.z1, rng.randrange(room.z0, room.z1 + 1)])
        r = room.roofmap.get((x, z))
        if r is None:
            continue
        y = r - 1
        if y < G + 9:
            continue
        if room.put(x, y, z, "minecraft:cobweb"):
            n += 1
    return n


def rubble(room, heaps=3):
    rng = room.rng
    n = 0
    for _ in range(heaps):
        side = rng.choice(["n", "s", "w", "e"])
        lat = rng.randrange(4, room.lat_len(side) - 4)
        cx, cy, cz = room.wcell(side, lat, 2, G)
        pat = [(0, 0, "block"), (1, 0, "slab"), (-1, 0, "stair"), (0, 1, "slab")]
        for (dx, dz, kind) in pat:
            x, z = cx + dx, cz + dz
            if not (room.inside(x, z) and room.free(x, G, z)):
                continue
            f = room.cells.get((x, FR, z))
            if f is None or not is_palette(f.block):
                continue
            if kind == "block":
                room.put(x, G, z, L.COBBLED)
            elif kind == "slab":
                room.put(x, G, z, L.COBBLED_SLAB, {"type": "bottom", "waterlogged": "false"})
            else:
                room.put(x, G, z, L.COBBLED_STAIRS, {"facing": room.into(side), "half": "bottom", "shape": "straight", "waterlogged": "false"})
            n += 1
    return n


def broken_ribs(room, share=0.25):
    """Removes a share of the rib blocks hanging under the roof (the roof stays closed: ribs are only trim)."""
    rng = room.rng
    n = 0
    for (x, z), r in room.roofmap.items():
        c = room.cells.get((x, r - 1, z))
        below = room.cells.get((x, r - 2, z))
        if below is not None:
            continue            # a rib that holds something (a lamp, a crystal, a cage) stays
        if c is not None and c.block == L.POLISHED and rng.random() < share:
            room.remove(x, r - 1, z)
            n += 1
    return n


# ------------------------------------------------------------------------------------------------------------------------------- armour stands
def stand_nbt(x, y, z, yaw):
    def empty():
        return nbt.Compound({})
    stand = nbt.Compound({
        "id": "minecraft:armor_stand", "NoGravity": nbt.Byte(1), "Invulnerable": nbt.Byte(1), "Invisible": nbt.Byte(0), "ShowArms": nbt.Byte(1),
        "DisabledSlots": nbt.Int(4144959), "Rotation": nbt.List([nbt.Float(yaw), nbt.Float(0.0)]),
        "ArmorItems": nbt.List([empty(), empty(), empty(), empty()], tag_type=nbt.TAG_COMPOUND),
        "HandItems": nbt.List([empty(), empty()], tag_type=nbt.TAG_COMPOUND)})
    pos = nbt.List([nbt.Double(x + 0.5), nbt.Double(float(y)), nbt.Double(z + 0.5)], tag_type=nbt.TAG_DOUBLE)
    return nbt.Compound({"pos": pos, "blockPos": nbt.List([nbt.Int(x), nbt.Int(y), nbt.Int(z)]), "nbt": stand})


def strip_stands(room, prop="candles"):
    """Drops every armour stand entity of the shipped room. Where it stood (the cell above its plinth) a small prop goes: candles, a lantern or a skull."""
    keep = []
    removed = []
    for e in room.t.entities:
        if str(e.get("nbt", {}).get("id", "")) == "minecraft:armor_stand":
            bp = e.get("blockPos")
            if bp is not None:
                removed.append((int(bp[0]), int(bp[1]), int(bp[2])))
            continue
        keep.append(e)
    room.t.entities = keep
    n = 0
    for (x, y, z) in removed:
        here = room.cells.get((x, y, z))
        below = room.cells.get((x, y - 1, z))
        if here is not None and here.block.endswith("_slab") and here.props.get("type", "bottom") == "bottom" and below is not None and L.is_full(below.block):
            room.force(x, y, z, here.block, {"type": "top", "waterlogged": "false"})        # a pedestal: the slab becomes a top slab so a prop can stand on it
            y += 1
        elif here is not None or below is None or not L.is_full(below.block):
            continue
        if (x, y, z) in room.cells:
            continue
        if prop == "lantern":
            ok = room.put(x, y, z, L.LANTERN, {"hanging": "false", "waterlogged": "false"})
        elif prop == "skull":
            ok = room.put(x, y, z, "minecraft:skeleton_skull", {"rotation": str(room.rng.randrange(16)), "powered": "false"})
        else:
            ok = room.put(x, y, z, "minecraft:white_candle", {"candles": str(room.rng.randrange(1, 5)), "lit": "true", "waterlogged": "false"})
        n += 1 if ok else 0
    return len(removed), n


def stand_slot(room, side=None, lat=None):
    """Builds ONE armour-stand niche (3 wide, 4 high, a polished plinth, a candle at each side) in a wall bay and puts the empty template stand in it. Returns the
    (side, lat) used or None when no bay can take it. The stand is the template's own signature stand (invulnerable, no gravity): the generator keeps it only for the
    rooms the rarity roll picks, and for those it swaps the stand for a lootable one."""
    rng = room.rng
    options = []
    for s in ("n", "s", "w", "e"):
        w = Wall(room, s)
        doors = w.door_lats()
        for k in range(w.m):
            c = 9 * k + 4
            if any(abs(c - d) <= 4 for d in doors):
                continue
            options.append((s, c))
    rng.shuffle(options)
    if side is not None:
        options = [(side, lat)]
    for s, c in options:
        w = Wall(room, s)
        if not _frame(w, c, G + 1, G + 4):
            continue
        # the plinth: the sill of the niche is the wall cell at row G (set to polished by the frame); the stand stands on the niche floor row G + 1? it stands on the sill
        w.put(c - 1, 1, G + 1, "minecraft:white_candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
        w.put(c + 1, 1, G + 1, "minecraft:white_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
        x, y, z = w.cell(c, 1, G + 1)
        room.t.entities.append(stand_nbt(x, y, z, YAW[room.into(s)]))
        return (s, c)
    return None
