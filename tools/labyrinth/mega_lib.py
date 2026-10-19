#!/usr/bin/env python3
"""
The shared toolkit of the Mega-Dungeon templates (1.54.0, docs/labyrinth/mega.md): the grid, the palette, a template builder that knows
rooms and doorways, data markers, entities, orientation-aware furniture and the vanilla "build hacks" (skull lamps, torture chairs, hanging
chain beds, snake portals ...). Every template builder of the Mega-Dungeon imports this file (do NOT edit it: put your own helpers in your own
module), so the numbers cannot drift apart: they are the same as `labyrinth/mega/MegaGrid.java`.

Why a builder class and not raw `Template` calls: a doorway is not one block. It is a 3x4 hole through BOTH wall layers, a jigsaw in the
bottom-centre cell of the outer layer, a frame and a lintel, at an offset that must be the centre of a 9-block module, and the hole must stay
open whatever the room later puts in front of it. `MT.socket()` writes all of that from one call and `MT.finish()` carves the holes last, so a
room builder cannot forget the hole or put a pillar in it. `MT.set()` also checks every block state against a table of the properties vanilla
blocks really have (the type-check we have instead of a game: `facing=forward` on a stair raises here, not as a purple-and-black cube in game).

Vanilla Block Primacy Rule: architecture, traps and decoration use VANILLA blocks only. The few custom blocks (the Gatekeeper door, the relic
pedestal, the exit gate, trap blocks of 1.36.0 ...) are named in `CUSTOM` below; `mega_check.py` rejects any other `nusmp:` block.

Architecture uses ONLY the palette constants (the deepslate family and the dark-oak family): the Region Theme processor swaps those families
for another vanilla family (blackstone, nether brick, end stone ...) so one template looks different in every sector. Anything that is not in
the palette (furniture, glass, lanterns, iron bars, candles ...) is left alone by the theme.
"""
import functools
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbt  # noqa: E402
from template import Template, ORIENTATIONS  # noqa: E402

ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")

# ------------------------------------------------------------------ the grid (== MegaGrid.java)
MODULE = 9
CENTER = 4
FLOOR_ROW = 8
HEIGHT = 48
WALL = 2
DOOR_W = 3
DOOR_H = 4
DOOR_ROW = FLOOR_ROW + 1
FLOOR_Y = 64
DOOR = "nusmp:mega_door"
CHAIN = "nusmp:mega_chain"
VAULT = "nusmp:mega_vault"
ONWARD = "nusmp:mega_onward"
P_SIDE = "nusmp:mega/side"
P_CAP = "nusmp:mega/cap"
P_LANDING = "nusmp:mega/landing"
P_ARENA = "nusmp:mega/arena"
P_ANTECHAMBER = "nusmp:mega/antechamber"
P_VAULT = "nusmp:mega/vault"
P_ONWARD = "nusmp:mega/onward"
P_SAFE = "nusmp:mega/safe"
P_LINK = "nusmp:mega/link"
P_EMPTY = "minecraft:empty"
PROCESSORS = "nusmp:mega_interior"

SIDES = ("n", "e", "s", "w")
FACING = {"n": "north", "e": "east", "s": "south", "w": "west"}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
STEP = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
RIGHT_OF = {"north": "east", "east": "south", "south": "west", "west": "north"}
LEFT_OF = {v: k for k, v in RIGHT_OF.items()}

#: custom (nusmp:) blocks templates may use; everything else must be vanilla. Add to this list ONLY for a block that is mechanically necessary.
CUSTOM = {
    "nusmp:gatekeeper_door", "nusmp:peak_altar", "nusmp:relic_pedestal", "nusmp:labyrinth_exit", "nusmp:labyrinth_gate",
    "nusmp:sealed_archway", "nusmp:magic_barrier", "nusmp:ward_rune", "nusmp:spawn_glyph",
    "nusmp:pitfall_tile", "nusmp:snuffer_glyph", "nusmp:sigil_plate", "nusmp:riddle_stone",
    "nusmp:labyrinth_bricks", "nusmp:labyrinth_tiles", "nusmp:labyrinth_pillar", "nusmp:labyrinth_glyph", "nusmp:labyrinth_lantern", "nusmp:magicule_vein",
    "nusmp:guardian_altar",
}

# ------------------------------------------------------------------ the palette (the theme processor swaps these families)
# Architecture, deepslate family. Slot "brick": walls. "tile": floors and ceilings. "polished": pillars, trim, door frames. "cobbled": rough fill.
BRICK = "minecraft:deepslate_bricks"
BRICK_STAIRS = "minecraft:deepslate_brick_stairs"
BRICK_SLAB = "minecraft:deepslate_brick_slab"
BRICK_WALL = "minecraft:deepslate_brick_wall"
TILE = "minecraft:deepslate_tiles"
TILE_STAIRS = "minecraft:deepslate_tile_stairs"
TILE_SLAB = "minecraft:deepslate_tile_slab"
TILE_WALL = "minecraft:deepslate_tile_wall"
POLISHED = "minecraft:polished_deepslate"
POLISHED_STAIRS = "minecraft:polished_deepslate_stairs"
POLISHED_SLAB = "minecraft:polished_deepslate_slab"
POLISHED_WALL = "minecraft:polished_deepslate_wall"
CHISELED = "minecraft:chiseled_deepslate"
COBBLED = "minecraft:cobbled_deepslate"
COBBLED_STAIRS = "minecraft:cobbled_deepslate_stairs"
COBBLED_SLAB = "minecraft:cobbled_deepslate_slab"
COBBLED_WALL = "minecraft:cobbled_deepslate_wall"
CRACKED_BRICK = "minecraft:cracked_deepslate_bricks"
CRACKED_TILE = "minecraft:cracked_deepslate_tiles"
ROCK = "minecraft:deepslate"
# Wood, dark oak family (slot "wood": themes swap it for crimson, warped, cherry, spruce ...).
PLANKS = "minecraft:dark_oak_planks"
PLANKS_STAIRS = "minecraft:dark_oak_stairs"
PLANKS_SLAB = "minecraft:dark_oak_slab"
FENCE = "minecraft:dark_oak_fence"
FENCE_GATE = "minecraft:dark_oak_fence_gate"
TRAPDOOR = "minecraft:dark_oak_trapdoor"
WOOD_DOOR = "minecraft:dark_oak_door"
LOG = "minecraft:dark_oak_log"
STRIPPED_LOG = "minecraft:stripped_dark_oak_log"
BUTTON = "minecraft:dark_oak_button"
PLATE = "minecraft:dark_oak_pressure_plate"

PALETTE = {BRICK, BRICK_STAIRS, BRICK_SLAB, BRICK_WALL, TILE, TILE_STAIRS, TILE_SLAB, TILE_WALL, POLISHED, POLISHED_STAIRS, POLISHED_SLAB,
           POLISHED_WALL, CHISELED, COBBLED, COBBLED_STAIRS, COBBLED_SLAB, COBBLED_WALL, CRACKED_BRICK, CRACKED_TILE, ROCK,
           PLANKS, PLANKS_STAIRS, PLANKS_SLAB, FENCE, FENCE_GATE, TRAPDOOR, WOOD_DOOR, LOG, STRIPPED_LOG, BUTTON, PLATE}

AIR = "minecraft:air"
IRON_BARS = "minecraft:iron_bars"
CHAIN_BLOCK = "minecraft:chain"
LANTERN = "minecraft:lantern"
SOUL_LANTERN = "minecraft:soul_lantern"

MARKER_KINDS = {"arrival", "room_center", "spawn", "patrol", "seat", "hearth", "bed", "furniture", "guardian_spawn", "expansion_socket",
                "vault_center", "pedestal", "faction", "gatekeeper", "trap", "stair_exit", "puzzle"}

# ------------------------------------------------------------------ block-state schema (the type-check we have instead of a game)
_B = ("true", "false")
_H4 = ("north", "south", "east", "west")
_H6 = _H4 + ("up", "down")
_SIDE3 = ("none", "low", "tall")
_SCHEMA = [  # (regex of the block path, {property: allowed values or None for any})
    # first on purpose: a mod block (the Gatekeeper door ends in "_door", the pressure plates in "_plate") is never judged by a vanilla rule
    (r"nusmp:.*", None),
    (r"chiseled_bookshelf", {"facing": _H4, "slot_0_occupied": _B, "slot_1_occupied": _B, "slot_2_occupied": _B, "slot_3_occupied": _B,
                              "slot_4_occupied": _B, "slot_5_occupied": _B}),
    (r".*_stairs",{"facing": _H4, "half": ("top", "bottom"), "shape": ("straight", "inner_left", "inner_right", "outer_left", "outer_right"), "waterlogged": _B}),
    (r".*_slab", {"type": ("top", "bottom", "double"), "waterlogged": _B}),
    (r".*_wall", {"up": _B, "north": _SIDE3, "east": _SIDE3, "south": _SIDE3, "west": _SIDE3, "waterlogged": _B}),
    (r".*_fence", {"north": _B, "east": _B, "south": _B, "west": _B, "waterlogged": _B}),
    (r".*_fence_gate", {"facing": _H4, "open": _B, "powered": _B, "in_wall": _B}),
    (r".*_trapdoor", {"facing": _H4, "half": ("top", "bottom"), "open": _B, "powered": _B, "waterlogged": _B}),
    (r".*_door", {"facing": _H4, "half": ("upper", "lower"), "hinge": ("left", "right"), "open": _B, "powered": _B}),
    (r".*_bed", {"facing": _H4, "part": ("head", "foot"), "occupied": _B}),
    (r"(soul_)?lantern", {"hanging": _B, "waterlogged": _B}),
    (r"chain", {"axis": ("x", "y", "z"), "waterlogged": _B}),
    (r"(soul_|redstone_)?wall_torch", {"facing": _H4, "lit": _B}),
    (r"(soul_|redstone_)?torch", {"lit": _B}),
    (r"(.*_)?candle", {"candles": ("1", "2", "3", "4"), "lit": _B, "waterlogged": _B}),
    (r"(soul_)?campfire", {"facing": _H4, "lit": _B, "signal_fire": _B, "waterlogged": _B}),
    (r".*_pillar|.*_log|.*_wood|hay_block|bone_block|basalt|polished_basalt|deepslate|muddy_mangrove_roots|ochre_froglight|verdant_froglight|pearlescent_froglight|infested_deepslate|stripped_.*|purpur_pillar|quartz_pillar|chiseled_.*",
     {"axis": ("x", "y", "z")}),
    (r"iron_bars|.*_glass_pane|glass_pane", {"north": _B, "east": _B, "south": _B, "west": _B, "waterlogged": _B}),
    (r"lightning_rod|waxed_.*lightning_rod|.*_lightning_rod", {"facing": _H6, "powered": _B, "waterlogged": _B}),
    (r".*_wall_skull|.*_wall_head", {"facing": _H4, "powered": _B}),
    (r".*_wall_banner", {"facing": _H4}),
    (r".*_skull|.*_head", {"rotation": tuple(str(i) for i in range(16)), "powered": _B}),
    (r"ladder", {"facing": _H4, "waterlogged": _B}),
    (r"barrel", {"facing": _H6, "open": _B}),
    (r"chest|trapped_chest", {"facing": _H4, "type": ("single", "left", "right"), "waterlogged": _B}),
    (r"ender_chest", {"facing": _H4, "waterlogged": _B}),
    (r"lever", {"face": ("floor", "wall", "ceiling"), "facing": _H4, "powered": _B}),
    (r".*_button", {"face": ("floor", "wall", "ceiling"), "facing": _H4, "powered": _B}),
    (r".*_pressure_plate", {"powered": _B}),
    (r"tripwire_hook", {"facing": _H4, "attached": _B, "powered": _B}),
    (r"pointed_dripstone", {"vertical_direction": ("up", "down"), "thickness": ("tip_merge", "tip", "frustum", "middle", "base"), "waterlogged": _B}),
    (r"end_rod", {"facing": _H6}),
    (r"anvil|chipped_anvil|damaged_anvil", {"facing": _H4}),
    (r"grindstone", {"face": ("floor", "wall", "ceiling"), "facing": _H4}),
    (r"lectern", {"facing": _H4, "has_book": _B, "powered": _B}),
    (r".*_banner", {"rotation": tuple(str(i) for i in range(16))}),
    (r"brewing_stand", {"has_bottle_0": _B, "has_bottle_1": _B, "has_bottle_2": _B}),
    (r"light", {"level": tuple(str(i) for i in range(16)), "waterlogged": _B}),
    (r"water|lava", {"level": tuple(str(i) for i in range(16))}),
    (r"(smoker|furnace|blast_furnace)", {"facing": _H4, "lit": _B}),
    (r"(loom|stonecutter|carved_pumpkin|jack_o_lantern|dispenser|dropper|observer|piston|sticky_piston)", {"facing": _H6}),
    (r"cauldron|water_cauldron|lava_cauldron|powder_snow_cauldron", {"level": ("1", "2", "3")}),
    (r"composter", {"level": tuple(str(i) for i in range(9))}),
    (r"vine", {"up": _B, "north": _B, "east": _B, "south": _B, "west": _B}),
    (r"cave_vines|cave_vines_plant", {"berries": _B, "age": None}),
    (r"glow_lichen|sculk_vein", {"up": _B, "down": _B, "north": _B, "east": _B, "south": _B, "west": _B, "waterlogged": _B}),
    (r"moss_carpet|pale_moss_carpet", {"bottom": _B}),
    (r"structure_block", {"mode": ("save", "load", "corner", "data")}),
    (r"jigsaw", {"orientation": ORIENTATIONS}),
    (r"nusmp:.*", None),
    (r"cobweb|bookshelf|chiseled_bookshelf|crafting_table|smithing_table|cartography_table|fletching_table|enchanting_table|glass|tinted_glass|"
     r"sea_lantern|glowstone|shroomlight|magma_block|obsidian|crying_obsidian|bedrock|barrier|air|cave_air|void_air|soul_sand|soul_soil|netherrack|"
     r"gold_block|iron_block|copper_block|diamond_block|amethyst_block|budding_amethyst|bone_block|coal_block|redstone_block|redstone_lamp|"
     r"sponge|dripstone_block|calcite|tuff|moss_block|mud|packed_mud|ice|packed_ice|blue_ice|snow_block|snow|powder_snow|scaffolding|"
     r"flower_pot|bell|conduit|beacon|cake|dragon_egg|spawner|"
     r".*_planks|.*_wool|.*_carpet|.*_concrete|.*_terracotta|.*_stained_glass|.*_bricks|.*_tiles|.*_block|.*_ore|.*_leaves|.*_sapling|.*_roots|"
     r".*_sign|.*_hanging_sign|.*_banner|.*_wall_head|potted_.*|.*_coral.*|.*_amethyst.*|.*_froglight|.*_sandstone|.*_andesite|.*_diorite|.*_granite|"
     r".*_blackstone.*|.*_basalt|.*_quartz.*|.*_prismarine.*|.*_purpur.*|.*_copper.*|.*_tuff.*|.*_mud_brick.*|.*_cobblestone|.*_stone|.*_deepslate.*|"
     r"cobblestone|stone|dirt|gravel|sand|red_sand|grass_block|podzol|mycelium|clay|rooted_dirt|coarse_dirt|farmland|dirt_path|"
     r"cracked_.*|chiseled_.*|polished_.*|smooth_.*|cut_.*|mossy_.*|infested_.*|raw_.*|exposed_.*|weathered_.*|oxidized_.*|waxed_.*|"
     r"fire|soul_fire|heavy_core|trial_spawner|vault|crafter|decorated_pot|sculk.*|reinforced_deepslate|"
     r"short_grass|tall_grass|fern|large_fern|dead_bush|azalea|flowering_azalea|spore_blossom|hanging_roots|big_dripleaf|small_dripleaf|"
     r"brown_mushroom|red_mushroom|crimson_fungus|warped_fungus|crimson_roots|warped_roots|nether_sprouts|weeping_vines|twisting_vines|"
     r"bamboo|sugar_cane|kelp|seagrass|lily_pad|cactus|pumpkin|melon|hay_block|honey_block|honeycomb_block|slime_block|"
     r"nether_wart|nether_wart_block|warped_wart_block|bone_block|respawn_anchor|lodestone|target|note_block|jukebox|"
     r"item_frame|glow_item_frame|painting|armor_stand", None),
]


def check_state(block, props):
    """An error string for a block state that cannot exist (an unknown property or value of a block the schema knows), else None."""
    return _check_cached(block, tuple(sorted((props or {}).items())))


_REAL_STATES = None


def real_states():
    """{block path: {property: [allowed values]}} of EVERY vanilla 1.21.1 block, read from vanilla_block_states_1_21_1.json (empty if the file is missing).
    Source: the "summary" data of the public mcmeta repository (github.com/misode/mcmeta, branch 1.21.1-summary, blocks/data.min.json), which is generated
    from the game's own data generators, so it is the game's truth, not a hand-written table (the hand-written _SCHEMA below stays as the fallback and
    for the blocks of other mods)."""
    global _REAL_STATES
    if _REAL_STATES is None:
        import json
        p = os.path.join(HERE, "vanilla_block_states_1_21_1.json")
        _REAL_STATES = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {}
    return _REAL_STATES


@functools.lru_cache(maxsize=65536)
def _check_cached(block, items):
    props = dict(items)
    path = block.split(":", 1)[1] if ":" in block else block
    if block.startswith("minecraft:"):
        real = real_states().get(path)
        if real is not None:
            for k, v in props.items():
                if k not in real:
                    return "%s has no property %r (it has %s)" % (block, k, ", ".join(sorted(real)) or "none")
                if str(v) not in real[k]:
                    return "%s property %s=%r is not one of %s" % (block, k, v, ", ".join(real[k]))
            return None
    for pat, allowed in _SCHEMA:
        if re.fullmatch(pat, path) or re.fullmatch(pat, block):
            if allowed is None:
                return None
            for k, v in (props or {}).items():
                if k not in allowed:
                    return "%s has no property %r (it has %s)" % (block, k, ", ".join(sorted(allowed)))
                vals = allowed[k]
                if vals is not None and str(v) not in vals:
                    return "%s property %s=%r is not one of %s" % (block, k, v, ", ".join(vals))
            return None
    return None  # unknown block: the id check of mega_check.py covers it


def _is_vanilla(block):
    return block.startswith("minecraft:")


# ------------------------------------------------------------------ connection states
# Why: the Jigsaw pool element places a template with "known shape" (no neighbour updates afterwards), so a fence, wall, glass pane or
# iron bar keeps EXACTLY the connection properties saved in the NBT, and a stair keeps its saved corner shape. A template written without them
# shows every pane and bar as a lone post and every stair corner as a step. `autoconnect` fills in what the game would have worked out, with the
# vanilla rules (bars/panes join bars, panes, walls and full blocks; fences join fences, gates and full blocks; walls join walls, gates and full
# blocks and rise to `tall` under a block; stairs turn into inner/outer corners next to a perpendicular stair). Explicit properties are respected.
_FULL = re.compile(r"(.*_planks|.*_log|.*_wood|.*_bricks|.*_tiles|stone|cobblestone|mossy_cobblestone|deepslate|cobbled_deepslate|polished_.*|"
                   r"chiseled_.*|cracked_.*|smooth_.*|.*_block|.*_terracotta|.*_concrete|.*_wool|bricks|tuff|calcite|obsidian|crying_obsidian|bookshelf|"
                   r"glass|tinted_glass|.*_stained_glass|netherrack|soul_sand|soul_soil|sea_lantern|glowstone|shroomlight|crafting_table|smithing_table|"
                   r"furnace|blast_furnace|smoker|barrel|dirt|mud|packed_mud|ice|packed_ice|blue_ice|end_stone|purpur_block|prismarine|dark_prismarine|"
                   r".*sandstone|.*_copper|bone_block|reinforced_deepslate|sculk.*|moss_block|stripped_.*|.*_pillar|.*_ore|cut_.*)")
_NOT_FULL = re.compile(r".*(stairs|slab|wall|fence|gate|door|trapdoor|bars|pane|torch|lantern|chain|candle|carpet|rod|skull|head|banner|sign|button|"
                       r"plate|lever|vine|lichen|cauldron|stand|anvil|bed|campfire|chest|pointed|cobweb|bell|brewing|flower|pot|ladder|hook|rail).*")
_DIRS4 = (("north", 0, -1), ("east", 1, 0), ("south", 0, 1), ("west", -1, 0))


def _path(block):
    return block.split(":", 1)[1] if ":" in block else block


def is_full(block):
    n = _path(block)
    return bool(_FULL.fullmatch(n)) and not _NOT_FULL.fullmatch(n)


def autoconnect(t):
    """Fills the connection properties of bars, panes, fences, walls and the corner shape of stairs in template `t` (see the note above)."""
    cells = t.cells

    def at(x, y, z):
        return cells.get((x, y, z))

    def is_stairs(c):
        return c is not None and _path(c.block).endswith("_stairs")

    def joins_pane(c):
        if c is None:
            return False
        n = _path(c.block)
        return n == "iron_bars" or n.endswith("glass_pane") or n.endswith("_wall") or is_full(c.block)

    def joins_fence(c, dname):
        if c is None:
            return False
        n = _path(c.block)
        if n.endswith("_fence"):
            return True
        if n.endswith("_fence_gate"):
            return c.props.get("facing", "north") in (("east", "west") if dname in ("north", "south") else ("north", "south"))
        return is_full(c.block)

    def joins_wall(c, dname):
        if c is None:
            return False
        n = _path(c.block)
        if n.endswith("_wall"):
            return True
        if n.endswith("_fence_gate"):
            return c.props.get("facing", "north") in (("east", "west") if dname in ("north", "south") else ("north", "south"))
        return is_full(c.block)

    for (x, y, z), c in list(cells.items()):
        n = _path(c.block)
        if n == "iron_bars" or n.endswith("glass_pane"):
            props = dict(c.props)
            for d, dx, dz in _DIRS4:
                if d not in props:
                    props[d] = "true" if joins_pane(at(x + dx, y, z + dz)) else "false"
            cells[(x, y, z)] = type(c)(c.block, props, c.nbt)
        elif n.endswith("_fence"):
            props = dict(c.props)
            for d, dx, dz in _DIRS4:
                if d not in props:
                    props[d] = "true" if joins_fence(at(x + dx, y, z + dz), d) else "false"
            cells[(x, y, z)] = type(c)(c.block, props, c.nbt)
        elif n.endswith("_wall") and not n.endswith("_wall_banner") and not n.endswith("_wall_skull") and not n.endswith("_wall_head"):
            props = dict(c.props)
            above = at(x, y + 1, z)
            above_full = above is not None and (is_full(above.block) or _path(above.block).endswith("_wall"))
            sides = {}
            for d, dx, dz in _DIRS4:
                if d in props:
                    sides[d] = props[d]
                    continue
                if joins_wall(at(x + dx, y, z + dz), d):
                    sides[d] = "tall" if above_full else "low"
                else:
                    sides[d] = "none"
                props[d] = sides[d]
            if "up" not in props:
                conn = {d: sides[d] != "none" for d in sides}
                straight = (conn["north"] and conn["south"] and not conn["east"] and not conn["west"]) or \
                           (conn["east"] and conn["west"] and not conn["north"] and not conn["south"])
                up = True
                if straight:
                    both_tall = (sides["north"] == "tall" and sides["south"] == "tall") or (sides["east"] == "tall" and sides["west"] == "tall")
                    up = False if both_tall else (above is not None and not _path(above.block).endswith("_wall") and above.block != "minecraft:air")
                if above is not None and _path(above.block).endswith("_wall") and above.props.get("up", "true") == "true":
                    up = True
                props["up"] = "true" if up else "false"
            cells[(x, y, z)] = type(c)(c.block, props, c.nbt)
        elif is_stairs(c):
            if "shape" in c.props:
                continue
            facing = c.props.get("facing", "north")
            half = c.props.get("half", "bottom")
            vec = {d: (dx, dz) for d, dx, dz in _DIRS4}
            ccw = {"north": "west", "west": "south", "south": "east", "east": "north"}
            opposite = {"north": "south", "south": "north", "east": "west", "west": "east"}
            axis = lambda d: "z" if d in ("north", "south") else "x"  # noqa: E731

            def can_take(direction):
                dx, dz = vec[direction]
                nb = at(x + dx, y, z + dz)
                return not is_stairs(nb) or nb.props.get("facing", "north") != facing or nb.props.get("half", "bottom") != half

            shape = "straight"
            fx, fz = vec[facing]
            front = at(x + fx, y, z + fz)
            if is_stairs(front) and front.props.get("half", "bottom") == half and axis(front.props.get("facing", "north")) != axis(facing) \
                    and can_take(opposite[front.props.get("facing", "north")]):
                shape = "outer_left" if front.props.get("facing", "north") == ccw[facing] else "outer_right"
            else:
                bx, bz = vec[opposite[facing]]
                back = at(x + bx, y, z + bz)
                if is_stairs(back) and back.props.get("half", "bottom") == half and axis(back.props.get("facing", "north")) != axis(facing) \
                        and can_take(back.props.get("facing", "north")):
                    shape = "inner_left" if back.props.get("facing", "north") == ccw[facing] else "inner_right"
            if shape != "straight":
                props = dict(c.props)
                props["shape"] = shape
                cells[(x, y, z)] = type(c)(c.block, props, c.nbt)


# ------------------------------------------------------------------ the template builder
class Frame:
    """Orientation-aware placement: local (r, u, f) = (to the right, up, forward) while looking along `facing`. `at(r,u,f)` is a world-local cell;
    `dir(rel)` turns 'forward' / 'back' / 'left' / 'right' into a compass name, so furniture is written once and works for all four rotations."""

    def __init__(self, mt, x, y, z, facing):
        if facing not in STEP:
            raise ValueError("facing must be north/east/south/west, got %r" % (facing,))
        self.mt, self.x, self.y, self.z, self.facing = mt, x, y, z, facing
        self.fx, self.fz = STEP[facing]
        self.rx, self.rz = STEP[RIGHT_OF[facing]]

    def at(self, r, u, f):
        return (self.x + r * self.rx + f * self.fx, self.y + u, self.z + r * self.rz + f * self.fz)

    def dir(self, rel):
        if rel == "forward":
            return self.facing
        if rel == "back":
            return OPPOSITE[self.facing]
        if rel == "right":
            return RIGHT_OF[self.facing]
        if rel == "left":
            return LEFT_OF[self.facing]
        raise ValueError("relative direction must be forward/back/left/right, got %r" % (rel,))

    def put(self, r, u, f, block, props=None, nbt_=None):
        x, y, z = self.at(r, u, f)
        self.mt.set(x, y, z, block, props, nbt_)
        return self

    def box(self, r1, u1, f1, r2, u2, f2, block, props=None):
        for r in range(min(r1, r2), max(r1, r2) + 1):
            for u in range(min(u1, u2), max(u1, u2) + 1):
                for f in range(min(f1, f2), max(f1, f2) + 1):
                    self.put(r, u, f, block, props)
        return self

    def sub(self, r, u, f, rel="forward"):
        x, y, z = self.at(r, u, f)
        return Frame(self.mt, x, y, z, self.dir(rel))


class MT:
    """A Mega-Dungeon template: `mx` x `mz` modules, 48 high, floor at row 8. Build the shell with `room()`, add sockets with `socket()`,
    furnish with raw `set` / `fill` / `Frame`, mark with `marker()`, then `finish()` and `save()`."""

    def __init__(self, name, mx, mz, height=HEIGHT):
        if height != HEIGHT:
            raise ValueError("every Mega template is %d high (got %d): the floor row must be the same everywhere" % (HEIGHT, height))
        self.t = Template(name, mx * MODULE, height, mz * MODULE)
        self.name = name
        self.W, self.D = mx * MODULE, mz * MODULE
        self.mx, self.mz = mx, mz
        self.rng = random.Random("nusmp:mega:" + name)
        self.sockets = []        # (side, offset, kwargs)
        self.reserved = set()    # cells that must stay clear (the doorway holes)
        self.finished = False
        self.connect = True      # fill in fence/wall/pane/bar connections and stair corners at finish()

    # ---- raw cells
    def set(self, x, y, z, block, props=None, nbt_=None):
        if not (0 <= x < self.W and 0 <= y < HEIGHT and 0 <= z < self.D):
            raise ValueError("%s: (%d,%d,%d) is outside the %dx%dx%d template" % (self.name, x, y, z, self.W, HEIGHT, self.D))
        if ":" not in block:
            block = "minecraft:" + block
        err = check_state(block, props)
        if err:
            raise ValueError("%s: bad block state at (%d,%d,%d): %s" % (self.name, x, y, z, err))
        if block.startswith("nusmp:") and block not in CUSTOM:
            raise ValueError("%s: %s is not a custom block a Mega template may use (Vanilla Block Primacy Rule; see mega_lib.CUSTOM)" % (self.name, block))
        if (x, y, z) in self.reserved and block != AIR:
            raise ValueError("%s: (%d,%d,%d) is inside a doorway hole and must stay clear (%s)" % (self.name, x, y, z, block))
        self.t.set(x, y, z, block, props, nbt_)
        return self

    def get(self, x, y, z):
        return self.t.get(x, y, z)

    def remove(self, x, y, z):
        self.t.remove(x, y, z)
        return self

    def fill(self, a, b, block, props=None):
        (x1, y1, z1), (x2, y2, z2) = a, b
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for x in range(min(x1, x2), max(x1, x2) + 1):
                    self.set(x, y, z, block, props)
        return self

    def hollow(self, a, b, block, props=None):
        (x1, y1, z1), (x2, y2, z2) = a, b
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for x in range(min(x1, x2), max(x1, x2) + 1):
                    if x in (min(x1, x2), max(x1, x2)) or y in (min(y1, y2), max(y1, y2)) or z in (min(z1, z2), max(z1, z2)):
                        self.set(x, y, z, block, props)
        return self

    def column(self, x, z, y1, y2, block, props=None):
        return self.fill((x, y1, z), (x, y2, z), block, props)

    def frame(self, x, y, z, facing):
        return Frame(self, x, y, z, facing)

    def pick(self, choices):
        """Weighted pick from [(weight, value), ...] with the template's own seeded random (deterministic bytes)."""
        total = sum(w for w, _ in choices)
        r = self.rng.random() * total
        for w, v in choices:
            r -= w
            if r < 0:
                return v
        return choices[-1][1]

    # ---- the shell
    def room(self, h, floor=TILE, wall=BRICK, ceiling=TILE, wear=0.12, ceiling_layers=2):
        """Floor row, two-thick walls up to `h` interior rows, a two-thick ceiling. `wear` is the chance of a cracked block in the walls and
        floor (deterministic). Interior stays unset (void is air): never fill it."""
        top = FLOOR_ROW + h
        for z in range(self.D):
            for x in range(self.W):
                self.set(x, FLOOR_ROW, z, CRACKED_TILE if self.rng.random() < wear and floor == TILE else floor)
                for k in range(ceiling_layers):
                    self.set(x, top + 1 + k, z, ceiling)
        for y in range(FLOOR_ROW + 1, top + 1):
            for z in range(self.D):
                for x in range(self.W):
                    if x < WALL or x >= self.W - WALL or z < WALL or z >= self.D - WALL:
                        self.set(x, y, z, CRACKED_BRICK if self.rng.random() < wear and wall == BRICK else wall)
        return self

    def corner_pillars(self, h, block=POLISHED):
        """A pillar in each outer corner (2x2) for the full interior height: reads as a buttress and hides the wall seam."""
        top = FLOOR_ROW + h
        for (x0, z0) in ((0, 0), (self.W - 2, 0), (0, self.D - 2), (self.W - 2, self.D - 2)):
            for y in range(FLOOR_ROW + 1, top + 1):
                for dx in (0, 1):
                    for dz in (0, 1):
                        self.set(x0 + dx, y, z0 + dz, block)
        return self

    # ---- doorways
    def socket(self, side, offset, pool=P_SIDE, name=DOOR, target=DOOR, final_state=AIR, hole=True, selection_priority=0, placement_priority=0,
               frame=True):
        """A door socket on `side` ('n','e','s','w') at `offset` along that side (must be the centre of a module: 4, 13, 22 ...). Writes the 3x4
        hole through both wall layers (at finish), the frame and the jigsaw. `final_state` is what the jigsaw cell becomes when nothing attaches
        (air = an open niche; a wall block or a door block for a sealed end; then pass hole=False if you fill the hole yourself)."""
        edge = self.W if side in ("n", "s") else self.D
        if side not in SIDES:
            raise ValueError("side must be one of n/e/s/w, got %r" % (side,))
        if not (0 <= offset < edge and offset % MODULE == CENTER):
            raise ValueError("%s: socket offset %d on side %s of a %d-wide wall is not a module centre (%s)" % (
                self.name, offset, side, edge, [k * MODULE + CENTER for k in range(edge // MODULE)]))
        self.sockets.append((side, offset, dict(pool=pool, name=name, target=target, final_state=final_state, hole=hole,
                                                selection_priority=selection_priority, placement_priority=placement_priority, frame=frame)))
        for (x, y, z) in self._hole_cells(side, offset):
            self.reserved.add((x, y, z))
        return self

    def _lateral(self, side, offset, lat, depth, y):
        """World-local cell of lateral position `lat` (absolute along the wall), `depth` layers in from the outer face, at row y."""
        if side == "n":
            return (lat, y, depth)
        if side == "s":
            return (lat, y, self.D - 1 - depth)
        if side == "w":
            return (depth, y, lat)
        return (self.W - 1 - depth, y, lat)

    def _hole_cells(self, side, offset):
        cells = []
        for depth in range(WALL):
            for lat in range(offset - 1, offset + 2):
                for y in range(DOOR_ROW, DOOR_ROW + DOOR_H):
                    cells.append(self._lateral(side, offset, lat, depth, y))
        return cells

    def socket_cell(self, side, offset):
        """The jigsaw cell of a socket (outer wall layer, first row above the floor)."""
        return self._lateral(side, offset, offset, 0, DOOR_ROW)

    def sockets_on(self, side):
        edge = self.W if side in ("n", "s") else self.D
        return [k * MODULE + CENTER for k in range(edge // MODULE)]

    def all_sockets(self, pool=P_SIDE, sides=SIDES, **kw):
        """A socket at every module centre of the given sides."""
        for s in sides:
            for off in self.sockets_on(s):
                self.socket(s, off, pool=pool, **kw)
        return self

    def finish(self):
        """Carves every doorway hole (and writes its frame and jigsaw). Call once, after the walls and the furniture."""
        if self.finished:
            return self
        self.finished = True
        for side, offset, kw in self.sockets:
            cells = self._hole_cells(side, offset)
            for (x, y, z) in cells:
                self.reserved.discard((x, y, z))
            if kw["hole"]:
                for (x, y, z) in cells:
                    self.remove(x, y, z)
            if kw["frame"]:
                for depth in range(WALL):
                    for lat in (offset - 2, offset + 2):
                        for y in range(DOOR_ROW, DOOR_ROW + DOOR_H + 1):
                            x, yy, z = self._lateral(side, offset, lat, depth, y)
                            if 0 <= x < self.W and 0 <= z < self.D:
                                self.t.set(x, yy, z, POLISHED if depth == 0 else BRICK)
                    for lat in range(offset - 2, offset + 3):
                        x, yy, z = self._lateral(side, offset, lat, depth, DOOR_ROW + DOOR_H)
                        if 0 <= x < self.W and 0 <= z < self.D:
                            self.t.set(x, yy, z, CHISELED if lat == offset else POLISHED)
            x, y, z = self.socket_cell(side, offset)
            self.t.cells.pop((x, y, z), None)
            self.t.jigsaw(x, y, z, FACING[side] + "_up", kw["name"], kw["target"], kw["pool"], final_state=kw["final_state"], joint="aligned",
                          selection_priority=kw["selection_priority"], placement_priority=kw["placement_priority"])
        if self.connect:
            autoconnect(self.t)
        return self

    # ---- markers and entities
    def marker(self, x, y, z, kind, **args):
        """A data marker (vanilla structure block, DATA mode): read before placement, never placed. `kind` is one of MARKER_KINDS; args become
        `;key=value` (a `facing` is rotated with the piece). Stands in the AIR cell above what it describes (see MarkerKind.java)."""
        if kind not in MARKER_KINDS:
            raise ValueError("unknown marker kind %r (one of %s)" % (kind, ", ".join(sorted(MARKER_KINDS))))
        meta = "nusmp:" + kind + "".join(";%s=%s" % (k, v) for k, v in args.items())
        data = nbt.Compound({"id": "minecraft:structure_block", "mode": "DATA", "metadata": meta, "name": "", "author": "",
                             "posX": nbt.Int(0), "posY": nbt.Int(1), "posZ": nbt.Int(0), "sizeX": nbt.Int(0), "sizeY": nbt.Int(0), "sizeZ": nbt.Int(0),
                             "rotation": "NONE", "mirror": "NONE", "integrity": nbt.Float(1.0), "seed": nbt.Long(0),
                             "ignoreEntities": nbt.Byte(1), "powered": nbt.Byte(0), "showair": nbt.Byte(0), "showboundingbox": nbt.Byte(1)})
        self.t.set(x, y, z, "minecraft:structure_block", {"mode": "data"}, data)
        return self

    def entity(self, x, y, z, entity_nbt, dx=0.5, dy=0.0, dz=0.5):
        """A template entity at the cell (x,y,z) plus (dx,dy,dz); `entity_nbt` is its NBT with an `id`."""
        if "id" not in entity_nbt:
            raise ValueError("entity nbt needs an id")
        pos = nbt.List([nbt.Double(x + dx), nbt.Double(y + dy), nbt.Double(z + dz)], tag_type=nbt.TAG_DOUBLE)
        self.t.entities.append(nbt.Compound({"pos": pos, "blockPos": nbt.List([nbt.Int(x), nbt.Int(y), nbt.Int(z)]), "nbt": nbt.Compound(entity_nbt)}))
        return self

    def armor_stand(self, x, y, z, yaw=0.0, head=None, chest=None, legs=None, feet=None, mainhand=None, invisible=False, arms=True,
                    dx=0.5, dy=0.0, dz=0.5):
        """An armor stand with equipment ids (e.g. head='minecraft:wither_skeleton_skull'); marker-sized and without gravity so it never moves."""
        def item(i):
            return nbt.Compound({"id": i, "count": nbt.Int(1)}) if i else nbt.Compound({})
        stand = nbt.Compound({
            "id": "minecraft:armor_stand", "NoGravity": nbt.Byte(1), "Invulnerable": nbt.Byte(1), "Invisible": nbt.Byte(1 if invisible else 0),
            "ShowArms": nbt.Byte(1 if arms else 0), "DisabledSlots": nbt.Int(4144959), "Rotation": nbt.List([nbt.Float(yaw), nbt.Float(0.0)]),
            "ArmorItems": nbt.List([item(feet), item(legs), item(chest), item(head)], tag_type=nbt.TAG_COMPOUND),
            "HandItems": nbt.List([item(mainhand), item(None)], tag_type=nbt.TAG_COMPOUND),
        })
        return self.entity(x, y, z, stand, dx=dx, dy=dy, dz=dz)

    # ---- output
    def validate(self):
        if not self.finished:
            raise ValueError("%s: call finish() before save()" % self.name)
        problems = []
        for (x, y, z) in self.reserved:
            if self.t.get(x, y, z) is not None:
                problems.append("hole cell (%d,%d,%d) is filled" % (x, y, z))
        return problems

    def save(self, root=RES):
        self.finish()
        problems = self.validate()
        if problems:
            raise ValueError("%s: %s" % (self.name, "; ".join(problems)))
        return self.t.save(root)

    def ascii(self, layer_from=FLOOR_ROW, layer_to=FLOOR_ROW + 12):
        text = self.t.ascii_layers().split("\n")
        keep = []
        want = None
        for line in text:
            if line.startswith("y="):
                want = layer_from <= int(line[2:]) <= layer_to
            if want is None or want or not line or line.startswith("legend") or line.startswith("jigsaw"):
                keep.append(line)
        return "\n".join(keep)


# ------------------------------------------------------------------ geometry helpers
def disk_cells(cx, cz, r):
    """Integer cells of a filled disc of radius r (+0.5 so even and odd sizes look round)."""
    out = []
    for z in range(int(cz - r) - 1, int(cz + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            if (x - cx) ** 2 + (z - cz) ** 2 <= (r + 0.5) ** 2:
                out.append((x, z))
    return out


def ring_cells(cx, cz, r, thick=1):
    inner = set(disk_cells(cx, cz, r - thick))
    return [c for c in disk_cells(cx, cz, r) if c not in inner]


def dome_cells(cx, cy, cz, r, thick=1):
    """Cells of the upper half of a spherical shell: centre (cx, cy, cz), outer radius r, `thick` thick."""
    out = []
    for y in range(cy, int(cy + r) + 2):
        for z in range(int(cz - r) - 1, int(cz + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                d = ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5
                if r - thick < d <= r + 0.3:
                    out.append((x, y, z))
    return out


# ------------------------------------------------------------------ the vanilla build hacks
def skull_lamp(mt, x, y, z, facing="south", style="sconce", skull="wither_skeleton"):
    """A skull that lights the room, from vanilla blocks only. `sconce`: a skull on the wall with a soul torch on the wall block above its head
    (a flaming-skull sconce). `candle`: a skull on a slab between lit candles. `lantern`: a skull on top of a lantern's cage of iron bars.
    Why: the brief's "skull lamps" without a custom block: the light is a torch, candle or lantern the game already tracks (so the Gloom clock
    counts it as a light source) and the skull is pure decoration. (x,y,z) is the skull cell; for `sconce` the skull hangs on the wall behind it:
    `facing` is the direction the skull FACES (away from the wall)."""
    sk = "minecraft:%s_skull" % skull
    wall_sk = "minecraft:%s_wall_skull" % skull
    if style == "sconce":
        mt.set(x, y, z, wall_sk, {"facing": facing})
        mt.set(x, y + 1, z, "minecraft:soul_wall_torch", {"facing": facing})
    elif style == "candle":
        mt.set(x, y - 1, z, POLISHED_SLAB, {"type": "bottom"})
        mt.set(x, y, z, sk, {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[facing])})
        f = Frame(mt, x, y, z, facing)
        f.put(-1, -1, 0, POLISHED_SLAB, {"type": "bottom"})
        f.put(1, -1, 0, POLISHED_SLAB, {"type": "bottom"})
        f.put(-1, 0, 0, "minecraft:red_candle", {"candles": "2", "lit": "true"})
        f.put(1, 0, 0, "minecraft:red_candle", {"candles": "3", "lit": "true"})
    elif style == "lantern":
        mt.set(x, y - 1, z, LANTERN, {"hanging": "false"})
        mt.set(x, y, z, sk, {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[facing])})
    else:
        raise ValueError("skull_lamp style must be sconce, candle or lantern")
    return mt


def torture_chair(mt, x, y, z, facing="south"):
    """A torture chair: a stair seat, an open iron-trapdoor backrest bristling with lightning-rod spikes, iron-bar arm restraints with
    chains, a skull on the headrest and a dripstone spike in front. `facing` is the direction the SITTER faces; (x,y,z) is the seat cell.
    Why: the brief's "torture chairs" from vanilla only: a lightning rod is a 2x2-pixel pole, which pointed at the sitter's back reads as a
    spike, and stairs, trapdoors and chains are all things the game's collision already handles, so nothing here is a custom block."""
    f = Frame(mt, x, y, z, facing)
    f.put(0, 0, 0, PLANKS_STAIRS, {"facing": f.dir("back"), "half": "bottom"})
    f.put(-1, 0, 0, IRON_BARS)
    f.put(1, 0, 0, IRON_BARS)
    for u in (1, 2):
        f.put(0, u, -1, "minecraft:iron_trapdoor", {"facing": f.dir("forward"), "half": "bottom", "open": "true"})
        f.put(0, u, 0, "minecraft:lightning_rod", {"facing": f.dir("forward")})
    f.put(0, 3, -1, "minecraft:skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[f.dir("forward")])})
    for r in (-1, 1):
        f.put(r, 1, 0, CHAIN_BLOCK, {"axis": "y"})
        f.put(r, 2, 0, CHAIN_BLOCK, {"axis": "y"})
    f.put(0, 0, 1, "minecraft:pointed_dripstone", {"vertical_direction": "up", "thickness": "tip"})
    return mt


def chain_bed(mt, x, y, z, facing="south", color="red", height=5):
    """A bed hung from the ceiling by four chains. (x,y,z) is the foot cell, the head is one cell `facing`-ward; `height` is how many chain
    blocks hang above each end. Why: the brief's "hanging chain beds": a vanilla bed, plus `chain` blocks (which need no support in a template),
    plus an iron-bar frame under it so it reads as hung rather than floating."""
    f = Frame(mt, x, y, z, facing)
    f.put(0, 0, 0, "minecraft:%s_bed" % color, {"facing": f.dir("forward"), "part": "foot"})
    f.put(0, 0, 1, "minecraft:%s_bed" % color, {"facing": f.dir("forward"), "part": "head"})
    for r in (-1, 1):
        for fo in (0, 1):
            for u in range(0, height):
                f.put(r, u, fo, CHAIN_BLOCK, {"axis": "y"})
    f.put(0, -1, 0, IRON_BARS)
    f.put(0, -1, 1, IRON_BARS)
    return mt


def snake_portal(mt, x, y, z, facing="south", w=7, h=9):
    """A decorative snake portal: an oval ring of waxed oxidised copper scales with lightning-rod fins on its outer edge, a blackstone head with
    end-rod eyes and open iron-trapdoor jaws on top, and a purple glass-pane membrane with an invisible light block behind it. (x,y,z) is the
    bottom-centre cell of the ring; `facing` is the direction the portal FACES. Not functional: a doorway-shaped picture, a wall decoration.
    Why: the brief's "snake portals" from lightning rods and trapdoors: the light block makes the membrane glow without a custom block."""
    f = Frame(mt, x, y, z, facing)
    half = w // 2
    for u in range(h):
        t = (2.0 * u / max(1, h - 1)) - 1.0
        span = int(round(half * (1.0 - t * t) ** 0.5))
        for sign in (-1, 1):
            f.put(sign * span, u, 0, "minecraft:waxed_oxidized_cut_copper")
            if 0 < u < h - 1:
                f.put(sign * (span + 1), u, 0, "minecraft:lightning_rod", {"facing": f.dir("right") if sign > 0 else f.dir("left")})
        for r in range(-span + 1, span):
            f.put(r, u, 0, "minecraft:purple_stained_glass_pane", {f.dir("right"): "true", f.dir("left"): "true"})
    f.put(0, h, 0, "minecraft:polished_blackstone")
    f.put(0, h + 1, 0, "minecraft:polished_blackstone")
    f.put(-1, h, 0, "minecraft:end_rod", {"facing": f.dir("forward")})
    f.put(1, h, 0, "minecraft:end_rod", {"facing": f.dir("forward")})
    f.put(0, h - 1, 1, "minecraft:iron_trapdoor", {"facing": f.dir("back"), "half": "bottom", "open": "true"})
    f.put(0, h // 2, -1, "minecraft:light", {"level": "11"})
    return mt


def table(mt, x, y, z, facing="south", length=2, top=PLANKS_SLAB):
    """A table of `length` cells along `facing`: top slabs on fence legs at both ends. (x,y,z) is the first TOP cell (the legs stand at y-1)."""
    f = Frame(mt, x, y, z, facing)
    for k in range(length):
        f.put(0, 0, k, top, {"type": "top"})
    for k in (0, length - 1):
        f.put(0, -1, k, FENCE)
    return mt


def chair(mt, x, y, z, facing="south", wood=PLANKS_STAIRS):
    """A stair chair; `facing` is the direction the sitter faces (the stair ascends behind them)."""
    mt.set(x, y, z, wood, {"facing": OPPOSITE[facing], "half": "bottom"})
    return mt


def chandelier(mt, x, y_ceiling, z, drop=4, radius=1, lantern=LANTERN):
    """A chandelier hung from the ceiling cell `y_ceiling`: a chain of `drop` blocks and a ring of lanterns round its foot.
    Every one of the five chains runs all the way up to the ceiling (the four side lanterns used to hang from a one-block chain stub that ended in the
    air next to the centre chain: it looked fine in a flat preview and floated in the game, which tools/labyrinth/mega_arch_check.py reports)."""
    y = y_ceiling - drop - 1
    for cx, cz in ((x, z), (x + radius, z), (x - radius, z), (x, z + radius), (x, z - radius)):
        for k in range(1, drop + 1):
            mt.set(cx, y_ceiling - k, cz, CHAIN_BLOCK, {"axis": "y"})
        mt.set(cx, y, cz, lantern, {"hanging": "true"})
    return mt


def banner(mt, x, y, z, facing="south", color="red"):
    """A wall banner on the wall behind (x,y,z); `facing` is the direction it faces (away from the wall)."""
    mt.set(x, y, z, "minecraft:%s_wall_banner" % color, {"facing": facing})
    return mt


def candles(mt, x, y, z, count=3, color="orange"):
    mt.set(x, y, z, "minecraft:%s_candle" % color, {"candles": str(max(1, min(4, count))), "lit": "true"})
    return mt


def statue(mt, x, y, z, facing="south", head="minecraft:iron_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
           feet="minecraft:iron_boots", weapon="minecraft:iron_sword"):
    """An armoured statue: a polished plinth and an armor stand in a full set (an entity of the template: no custom block)."""
    mt.set(x, y, z, POLISHED)
    mt.set(x, y + 1, z, POLISHED_SLAB, {"type": "bottom"})
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
    mt.armor_stand(x, y + 1, z, yaw=yaw, head=head, chest=chest, legs=legs, feet=feet, mainhand=weapon, dy=0.5)
    return mt


def bookshelf_wall(mt, x, y, z, facing="south", width=5, height=3):
    """A wall of bookshelves `width` wide (to the right of x) and `height` tall, with a lectern-free walkway in front."""
    f = Frame(mt, x, y, z, facing)
    for r in range(width):
        for u in range(height):
            f.put(r, u, 0, "minecraft:bookshelf")
    return mt


def cage(mt, x, y, z, w=3, d=3, h=4, skull=True):
    """An iron-bar cell: bars on all four sides and the top with a chain hanging from the ceiling bar, a skull inside. (x,y,z) is the min corner."""
    for dx in range(w):
        for dz in range(d):
            for dy in range(h):
                edge = dx in (0, w - 1) or dz in (0, d - 1)
                if edge or dy == h - 1:
                    mt.set(x + dx, y + dy, z + dz, IRON_BARS)
    if skull:
        mt.set(x + w // 2, y, z + d // 2, "minecraft:skeleton_skull", {"rotation": "5"})
    return mt


def forge(mt, x, y, z, facing="south"):
    """A forge corner: a lava cauldron behind a blast furnace, an anvil and a grindstone in front. (x,y,z) is the furnace cell."""
    f = Frame(mt, x, y, z, facing)
    f.put(0, 0, 0, "minecraft:blast_furnace", {"facing": f.dir("forward"), "lit": "true"})
    f.put(-1, 0, 0, "minecraft:lava_cauldron")
    f.put(1, 0, 0, "minecraft:smithing_table")
    f.put(0, 0, 2, "minecraft:anvil", {"facing": f.dir("right")})
    f.put(2, 0, 1, "minecraft:grindstone", {"face": "floor", "facing": f.dir("forward")})
    return mt


def brewing_corner(mt, x, y, z, facing="south"):
    """A potion bench: brewing stands and a cauldron on a slab counter with hanging lanterns above."""
    f = Frame(mt, x, y, z, facing)
    for r in range(3):
        f.put(r, 0, 0, PLANKS_SLAB, {"type": "top"})
    f.put(0, 1, 0, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    f.put(1, 1, 0, "minecraft:cauldron")
    f.put(2, 1, 0, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "true", "has_bottle_2": "false"})
    f.put(1, 3, 0, CHAIN_BLOCK, {"axis": "y"})
    f.put(1, 2, 0, LANTERN, {"hanging": "true"})
    return mt


def checker_floor(mt, x1, z1, x2, z2, a="minecraft:polished_blackstone", b="minecraft:polished_diorite", y=FLOOR_ROW):
    """A chequerboard floor (the reference vault's look): `a` and `b` alternate by (x+z). Replaces the floor row inside the box."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            mt.set(x, y, z, a if (x + z) % 2 == 0 else b)
    return mt
