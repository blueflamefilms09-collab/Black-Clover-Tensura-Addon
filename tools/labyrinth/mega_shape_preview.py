#!/usr/bin/env python3
"""
A shape-aware preview and a block-light checker for the Mega-Dungeon templates (1.54.0), without the game (needs Pillow).

    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/straight_1x3_a out.png --y 8:18 --x 0:6 --z 0:30 --scale 12
    python3 tools/labyrinth/mega_shape_preview.py mega/junction/plaza_2x2_a out.png --top        # plan view, highest block per column
    python3 tools/labyrinth/mega_shape_preview.py mega/junction/plaza_2x2_a out.png --light      # plan view tinted by block light
    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/straight_1x3_a out.png --lit        # faces shaded by the block light in front of them
    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/bend_1x1_a out.png --rot 2 --x 2:8 --z 2:8 --y 8:15   # seen from the north-west instead (cut the near walls yourself)
    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/straight_1x3_a out.png --elev z --z 6:6 --y 8:16 --scale 40   # a cross-section at z=6, seen from the south
    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/straight_1x3_a out.png --elev x --x 1:2 --y 8:16 --scale 24   # the inside of the west wall, seen from the east
    python3 tools/labyrinth/mega_shape_preview.py mega/cap/alcove_lantern out.png --scale 24 --nocut
    python3 tools/labyrinth/mega_shape_preview.py mega/corridor/bend_1x1_a --lightreport         # only print the light numbers

Why a second preview next to `mega_preview.py`: that one paints every block as a full cube, so a stair, a slab, an open trapdoor, a lantern
or a chain all look like grey cubes, and the things that make a room look hand-crafted (an arch rising by stairs, an upside-down stair cove,
a lantern hanging on a chain, a candle on a slab, a carpet on the floor) cannot be judged, and a stair facing the wrong way goes unnoticed
until the game. Here every block becomes the boxes it really occupies (slab halves, stair steps with their corner shapes, trapdoor plates,
fence and wall posts, pane and bar posts, chains, rods, torches, lanterns, candles, carpets ...) and the three visible faces of each box are
painted from the same south-east viewpoint, so the picture matches `mega_preview.py` but shows the shapes. Armor stands are drawn as little
figures, markers (structure blocks) as magenta dots, jigsaw sockets as yellow dots, `light` blocks as pale dots.

The light checker exists because the dungeon's gloom clock (labyrinth/dnd/economy/GloomClock) counts every second spent at block light <= 4
with nothing lit in hand: a template that looks atmospheric but has a dark corner on the walking route would silently punish players. It floods
block light from every emitter through everything that does not block light (the usual 1 per step), then reports, over the cells a player can
stand in and reach from a doorway, the minimum, the share at or below 4 and the share at or below 7. Stairs, slabs and trapdoors are treated as
passing light (the real rule depends on which faces are open), so the numbers are slightly optimistic: aim for a minimum of 6 or more.
"""
import os
import re
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw  # noqa: E402
import mega_lib as L  # noqa: E402
import mega_preview as P  # noqa: E402
from template import load  # noqa: E402

DYES = {"white": (235, 235, 235), "orange": (230, 130, 40), "magenta": (190, 70, 190), "light_blue": (110, 170, 230), "yellow": (235, 210, 60),
        "lime": (120, 200, 50), "pink": (240, 150, 180), "gray": (90, 90, 95), "light_gray": (160, 160, 165), "cyan": (40, 150, 160),
        "purple": (130, 60, 180), "blue": (50, 70, 190), "brown": (110, 75, 45), "green": (70, 110, 40), "red": (180, 40, 40), "black": (30, 30, 34)}
_DYE_RE = re.compile(r"(%s)_(.*)" % "|".join(sorted(DYES, key=len, reverse=True)))


def path_of(block):
    return block.split(":", 1)[1] if ":" in block else block


def color_of(block):
    n = path_of(block)
    m = _DYE_RE.fullmatch(n)
    if m and not m.group(2).startswith(("sandstone", "terracotta_x")):
        c = DYES[m.group(1)]
        rest = m.group(2)
        if "glass" in rest:
            return (c[0], c[1], c[2], 130)
        return (c[0], c[1], c[2], 255)
    if n in ("water", "water_cauldron"):
        return (50, 100, 210, 170)
    if n == "cobweb":
        return (225, 225, 225, 90)
    if n in ("light",):
        return (255, 255, 160, 110)
    if "soul" in n and ("lantern" in n or "torch" in n or "campfire" in n or "fire" in n):
        return (90, 220, 240, 255)
    if n in ("copper_block", "waxed_copper_block") or "oxidized" in n:
        return (60, 170, 150, 255)
    if "reinforced_deepslate" in n:
        return (52, 84, 92, 255)
    if n in ("labyrinth_gate", "labyrinth_exit"):
        return (120, 90, 235, 190)
    if "chiseled_deepslate" in n:
        return (128, 132, 150, 255)
    if "polished_deepslate" in n:
        return (142, 146, 160, 255)
    if "cracked_deepslate_tile" in n:
        return (84, 86, 96, 255)
    if "deepslate_tile" in n:
        return (96, 99, 110, 255)
    if "cobbled_deepslate" in n:
        return (112, 114, 122, 255)
    if "cracked_deepslate_brick" in n:
        return (104, 106, 116, 255)
    if "deepslate_brick" in n:
        return (118, 121, 132, 255)
    if n in ("deepslate", "infested_deepslate"):
        return (88, 88, 98, 255)
    if "dark_oak" in n or n.startswith("stripped_dark_oak"):
        return (120, 82, 46, 255)
    if n.endswith("froglight"):
        return {"ochre_froglight": (245, 215, 120, 255), "verdant_froglight": (150, 235, 150, 255), "pearlescent_froglight": (235, 170, 235, 255)}.get(n, (240, 220, 150, 255))
    if n in ("sea_lantern", "glowstone", "shroomlight", "jack_o_lantern", "lantern"):
        return (255, 190, 90, 255)
    if n in ("campfire", "fire", "lava", "magma_block"):
        return (250, 120, 30, 255)
    if "bookshelf" in n:
        return (150, 105, 60, 255)
    if "hay" in n:
        return (200, 170, 60, 255)
    if "bone" in n:
        return (225, 220, 195, 255)
    if "skull" in n or n.endswith("_head"):
        return (225, 220, 205, 255)
    if "candle" in n:
        return (235, 225, 190, 255)
    if "end_rod" in n:
        return (250, 240, 255, 255)
    if "lightning_rod" in n:
        return (190, 120, 70, 255)
    return P.color_of(block)


# ------------------------------------------------------------------ block -> boxes (cell-local fractions)
def _px(*v):
    return tuple(a / 16.0 for a in v)


_FACE_VEC = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
_QUAD = {"NW": (0.0, 0.5, 0.0, 0.5), "NE": (0.5, 1.0, 0.0, 0.5), "SW": (0.0, 0.5, 0.5, 1.0), "SE": (0.5, 1.0, 0.5, 1.0)}
# vanilla StairBlock.SHAPE_BY_STATE: bit 1 = NW, 2 = NE, 4 = SW, 8 = SE; facing order S, W, N, E
_STAIR_BITS = {"straight": (12, 5, 3, 10), "inner_left": (14, 13, 7, 11), "inner_right": (13, 7, 11, 14),
               "outer_left": (8, 4, 1, 2), "outer_right": (4, 1, 2, 8)}
_FACING_IDX = {"south": 0, "west": 1, "north": 2, "east": 3}


def stair_boxes(props):
    facing = props.get("facing", "north")
    shape = props.get("shape", "straight")
    half = props.get("half", "bottom")
    bits = _STAIR_BITS.get(shape, _STAIR_BITS["straight"])[_FACING_IDX.get(facing, 2)]
    out = []
    if half == "bottom":
        out.append((0, 0, 0, 1, 0.5, 1))
        y0, y1 = 0.5, 1.0
    else:
        out.append((0, 0.5, 0, 1, 1, 1))
        y0, y1 = 0.0, 0.5
    for bit, q in ((1, "NW"), (2, "NE"), (4, "SW"), (8, "SE")):
        if bits & bit:
            x0, x1, z0, z1 = _QUAD[q]
            out.append((x0, y0, z0, x1, y1, z1))
    return out


def trapdoor_boxes(props):
    t = 3 / 16.0
    if props.get("open", "false") == "true":
        f = props.get("facing", "north")
        # the open plate stands against the block OPPOSITE to `facing` (it faces `facing`)
        return [{"north": (0, 0, 1 - t, 1, 1, 1), "south": (0, 0, 0, 1, 1, t), "west": (1 - t, 0, 0, 1, 1, 1), "east": (0, 0, 0, t, 1, 1)}[f]]
    return [(0, 0, 0, 1, t, 1)] if props.get("half", "bottom") == "bottom" else [(0, 1 - t, 0, 1, 1, 1)]


def post_arms(props, post, arm_y, arm_w, names=("north", "east", "south", "west"), arm_val=lambda v: v in ("true", "low", "tall")):
    lo, hi = 0.5 - post / 2.0, 0.5 + post / 2.0
    out = [(lo, 0, lo, hi, 1, hi)]
    a0, a1 = 0.5 - arm_w / 2.0, 0.5 + arm_w / 2.0
    for n in names:
        v = props.get(n, "false")
        if not arm_val(v):
            continue
        y0, y1 = arm_y
        if v == "tall":
            y1 = 1.0
        dx, dz = _FACE_VEC[n]
        if dx == 0:
            z0, z1 = (0.0, lo) if dz < 0 else (hi, 1.0)
            out.append((a0, y0, z0, a1, y1, z1))
        else:
            x0, x1 = (0.0, lo) if dx < 0 else (hi, 1.0)
            out.append((x0, y0, a0, x1, y1, a1))
    return out


def boxes_for(block, props):
    n = path_of(block)
    if n in ("air", "cave_air", "void_air", "barrier", "structure_void"):
        return []
    if n == "jigsaw" or n == "structure_block":
        return [(0.35, 0.0, 0.35, 0.65, 0.3, 0.65)]
    if n == "light":
        return [(0.42, 0.42, 0.42, 0.58, 0.58, 0.58)]
    if n.endswith("_stairs"):
        return stair_boxes(props)
    if n.endswith("_slab"):
        t = props.get("type", "bottom")
        return [(0, 0, 0, 1, 1, 1)] if t == "double" else ([(0, 0, 0, 1, 0.5, 1)] if t == "bottom" else [(0, 0.5, 0, 1, 1, 1)])
    if n.endswith("_trapdoor"):
        return trapdoor_boxes(props)
    if n.endswith("_fence"):
        return post_arms(props, 4 / 16.0, (6 / 16.0, 15 / 16.0), 2 / 16.0)
    if n.endswith("_fence_gate"):
        f = props.get("facing", "north")
        if f in ("north", "south"):
            return [(0, 5 / 16.0, 7 / 16.0, 2 / 16.0, 1, 9 / 16.0), (14 / 16.0, 5 / 16.0, 7 / 16.0, 1, 1, 9 / 16.0)] + \
                   ([] if props.get("open") == "true" else [(2 / 16.0, 6 / 16.0, 7 / 16.0, 14 / 16.0, 15 / 16.0, 9 / 16.0)])
        return [(7 / 16.0, 5 / 16.0, 0, 9 / 16.0, 1, 2 / 16.0), (7 / 16.0, 5 / 16.0, 14 / 16.0, 9 / 16.0, 1, 1)] + \
               ([] if props.get("open") == "true" else [(7 / 16.0, 6 / 16.0, 2 / 16.0, 9 / 16.0, 15 / 16.0, 14 / 16.0)])
    if n.endswith("_wall") and not n.endswith(("_wall_banner", "_wall_skull", "_wall_head", "_wall_torch", "_wall_sign", "_wall_fan")):
        out = []
        if props.get("up", "true") == "true":
            out.append((4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 1, 12 / 16.0))
        else:
            out.append((6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 1, 10 / 16.0))
        a0, a1 = 5 / 16.0, 11 / 16.0
        for d in ("north", "east", "south", "west"):
            v = props.get(d, "none")
            if v == "none":
                continue
            y1 = 1.0 if v == "tall" else 14 / 16.0
            dx, dz = _FACE_VEC[d]
            if dx == 0:
                out.append((a0, 0, 0.0 if dz < 0 else 0.5, a1, y1, 0.5 if dz < 0 else 1.0))
            else:
                out.append((0.0 if dx < 0 else 0.5, 0, a0, 0.5 if dx < 0 else 1.0, y1, a1))
        return out
    if n == "iron_bars" or n.endswith("glass_pane"):
        return post_arms(props, 2 / 16.0, (0.0, 1.0), 2 / 16.0)
    if n == "chain":
        ax = props.get("axis", "y")
        lo, hi = 6.5 / 16.0, 9.5 / 16.0
        return [{"y": (lo, 0, lo, hi, 1, hi), "x": (0, lo, lo, 1, hi, hi), "z": (lo, lo, 0, hi, hi, 1)}[ax]]
    if n == "lightning_rod" or n.endswith("_lightning_rod") or n == "end_rod":
        f = props.get("facing", "up")
        lo, hi = 7 / 16.0, 9 / 16.0
        return [{"up": (lo, 0, lo, hi, 1, hi), "down": (lo, 0, lo, hi, 1, hi), "north": (lo, lo, 0, hi, hi, 1), "south": (lo, lo, 0, hi, hi, 1),
                 "east": (0, lo, lo, 1, hi, hi), "west": (0, lo, lo, 1, hi, hi)}[f]]
    if n in ("torch", "soul_torch", "redstone_torch"):
        return [(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 10 / 16.0, 9 / 16.0)]
    if n in ("wall_torch", "soul_wall_torch", "redstone_wall_torch"):
        f = props.get("facing", "north")
        dx, dz = _FACE_VEC[f]
        cx, cz = 0.5 - dx * 0.3, 0.5 - dz * 0.3  # the torch leans out from the wall behind it (opposite to facing)
        return [(cx - 1 / 16.0, 3 / 16.0, cz - 1 / 16.0, cx + 1 / 16.0, 13 / 16.0, cz + 1 / 16.0)]
    if n in ("lantern", "soul_lantern"):
        if props.get("hanging", "false") == "true":
            return [(5 / 16.0, 1 / 16.0, 5 / 16.0, 11 / 16.0, 8 / 16.0, 11 / 16.0), (7 / 16.0, 8 / 16.0, 7 / 16.0, 9 / 16.0, 1, 9 / 16.0)]
        return [(5 / 16.0, 0, 5 / 16.0, 11 / 16.0, 7 / 16.0, 11 / 16.0)]
    if n == "candle" or n.endswith("_candle"):
        k = int(props.get("candles", "1"))
        spots = [(7, 7), (5, 8), (9, 6), (6, 5)][:k]
        return [(a / 16.0, 0, b / 16.0, (a + 2) / 16.0, 0.28 + 0.04 * i, (b + 2) / 16.0) for i, (a, b) in enumerate(spots)]
    if n in ("campfire", "soul_campfire"):
        return [(0, 0, 0, 1, 7 / 16.0, 1)]
    if n.endswith("_carpet") or n == "moss_carpet":
        return [(0, 0, 0, 1, 1 / 16.0, 1)]
    if n.endswith("_skull") or n.endswith("_head"):
        if n.endswith(("_wall_skull", "_wall_head")):
            f = props.get("facing", "north")
            dx, dz = _FACE_VEC[f]
            cx, cz = 0.5 - dx * 0.25, 0.5 - dz * 0.25
            return [(cx - 4 / 16.0, 4 / 16.0, cz - 4 / 16.0, cx + 4 / 16.0, 12 / 16.0, cz + 4 / 16.0)]
        return [(4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 8 / 16.0, 12 / 16.0)]
    if n.endswith("_wall_banner"):
        f = props.get("facing", "north")
        t = 3 / 16.0
        return [{"north": (1 / 16.0, -0.75, 1 - t, 15 / 16.0, 1, 1), "south": (1 / 16.0, -0.75, 0, 15 / 16.0, 1, t),
                 "west": (1 - t, -0.75, 1 / 16.0, 1, 1, 15 / 16.0), "east": (0, -0.75, 1 / 16.0, t, 1, 15 / 16.0)}[f]]
    if n.endswith("_banner"):
        return [(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 1, 9 / 16.0), (2 / 16.0, 0.3, 7 / 16.0, 14 / 16.0, 1.9, 9 / 16.0)]
    if n.endswith("_bed"):
        return [(0, 3 / 16.0, 0, 1, 9 / 16.0, 1), (0, 0, 0, 2 / 16.0, 3 / 16.0, 2 / 16.0), (14 / 16.0, 0, 14 / 16.0, 1, 3 / 16.0, 1)]
    if n in ("chest", "trapped_chest", "ender_chest"):
        return [(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 14 / 16.0, 15 / 16.0)]
    if n.endswith("_door"):
        f = props.get("facing", "north")
        t = 3 / 16.0
        if props.get("open", "false") == "true":
            f = {"north": "east", "east": "south", "south": "west", "west": "north"}[f] if props.get("hinge", "left") == "left" else \
                {"north": "west", "west": "south", "south": "east", "east": "north"}[f]
        return [{"north": (0, 0, 1 - t, 1, 1, 1), "south": (0, 0, 0, 1, 1, t), "west": (1 - t, 0, 0, 1, 1, 1), "east": (0, 0, 0, t, 1, 1)}[f]]
    if n == "ladder":
        f = props.get("facing", "north")
        t = 2 / 16.0
        return [{"north": (0, 0, 1 - t, 1, 1, 1), "south": (0, 0, 0, 1, 1, t), "west": (1 - t, 0, 0, 1, 1, 1), "east": (0, 0, 0, t, 1, 1)}[f]]
    if n.endswith("_button"):
        return [(5 / 16.0, 0, 6 / 16.0, 11 / 16.0, 2 / 16.0, 10 / 16.0)] if props.get("face", "wall") == "floor" else [(5 / 16.0, 6 / 16.0, 0, 11 / 16.0, 10 / 16.0, 2 / 16.0)]
    if n.endswith("_pressure_plate"):
        return [(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 1 / 16.0, 15 / 16.0)]
    if n in ("cauldron", "lava_cauldron", "powder_snow_cauldron", "water_cauldron"):
        return [(0, 0, 0, 1, 14 / 16.0, 1)]
    if n == "anvil" or n.endswith("_anvil"):
        return [(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 4 / 16.0, 14 / 16.0), (5 / 16.0, 4 / 16.0, 5 / 16.0, 11 / 16.0, 10 / 16.0, 11 / 16.0), (0, 10 / 16.0, 3 / 16.0, 1, 1, 13 / 16.0)]
    if n == "grindstone":
        return [(3 / 16.0, 3 / 16.0, 3 / 16.0, 13 / 16.0, 13 / 16.0, 13 / 16.0)]
    if n == "lectern":
        return [(0, 0, 0, 1, 2 / 16.0, 1), (4 / 16.0, 2 / 16.0, 4 / 16.0, 12 / 16.0, 12 / 16.0, 12 / 16.0), (0, 10 / 16.0, 0, 1, 14 / 16.0, 1)]
    if n == "brewing_stand":
        return [(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 14 / 16.0, 9 / 16.0), (1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 2 / 16.0, 15 / 16.0)]
    if n == "flower_pot" or n.startswith("potted_"):
        return [(5 / 16.0, 0, 5 / 16.0, 11 / 16.0, 6 / 16.0, 11 / 16.0)]
    if n == "pointed_dripstone":
        return [(5 / 16.0, 0, 5 / 16.0, 11 / 16.0, 1, 11 / 16.0)]
    if n in ("vine", "glow_lichen", "sculk_vein"):
        out = []
        t = 1 / 16.0
        for d, bx in (("north", (0, 0, 0, 1, 1, t)), ("south", (0, 0, 1 - t, 1, 1, 1)), ("west", (0, 0, 0, t, 1, 1)), ("east", (1 - t, 0, 0, 1, 1, 1))):
            if props.get(d) == "true":
                out.append(bx)
        if props.get("up") == "true":
            out.append((0, 1 - t, 0, 1, 1, 1))
        return out
    if n in ("cobweb", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "hanging_roots", "cave_vines", "cave_vines_plant", "spore_blossom"):
        return [(0.1, 0, 0.1, 0.9, 1, 0.9)]
    if n in ("scaffolding",):
        return [(0, 0, 0, 1, 1, 1)]
    if n in ("water",):
        return [(0, 0, 0, 1, 0.88, 1)]
    if n in ("snow",):
        return [(0, 0, 0, 1, 2 / 16.0, 1)]
    if n in ("conduit", "beacon"):
        return [(0.2, 0.2, 0.2, 0.8, 0.8, 0.8)]
    if n in ("bell",):
        return [(5 / 16.0, 4 / 16.0, 5 / 16.0, 11 / 16.0, 12 / 16.0, 11 / 16.0)]
    return [(0, 0, 0, 1, 1, 1)]


def is_full_cube(block, props):
    b = boxes_for(block, props)
    return len(b) == 1 and b[0] == (0, 0, 0, 1, 1, 1) and color_of(block)[3] == 255


# ------------------------------------------------------------------ entities
def entity_boxes(ent):
    """Absolute-coordinate boxes of a template entity (armor stands as figures; anything else as a small box)."""
    nb = ent["nbt"]
    ident = str(nb.get("id", ""))
    x, y, z = [float(v) for v in ent["pos"]]
    if ident != "minecraft:armor_stand":
        return [(x - 0.25, y, z - 0.25, x + 0.25, y + 0.5, z + 0.25, (200, 60, 200, 255))]
    gear = {}
    names = ("feet", "legs", "chest", "head")
    try:
        for nm, it in zip(names, nb.get("ArmorItems", [])):
            gear[nm] = str(it.get("id", "")) if it else ""
    except Exception:  # noqa: BLE001  - odd equipment NBT: draw the stand in plain colours rather than fail a preview
        gear = {}

    def col(slot, default):
        i = gear.get(slot, "")
        if not i:
            return default
        for key, c in (("netherite", (60, 55, 60, 255)), ("diamond", (80, 220, 220, 255)), ("golden", (240, 210, 70, 255)), ("iron", (200, 200, 205, 255)),
                       ("chainmail", (150, 150, 150, 255)), ("leather", (140, 90, 50, 255)), ("skull", (225, 220, 205, 255)), ("head", (225, 220, 205, 255))):
            if key in i:
                return c
        return (180, 180, 180, 255)
    wood = (150, 120, 80, 255)
    return [(x - 0.2, y, z - 0.12, x - 0.05, y + 0.75, z + 0.12, col("legs", wood)), (x + 0.05, y, z - 0.12, x + 0.2, y + 0.75, z + 0.12, col("legs", wood)),
            (x - 0.25, y + 0.75, z - 0.15, x + 0.25, y + 1.4, z + 0.15, col("chest", wood)),
            (x - 0.2, y + 1.4, z - 0.2, x + 0.2, y + 1.85, z + 0.2, col("head", (205, 175, 130, 255)))]


# ------------------------------------------------------------------ light
LIGHT_FIXED = {"lantern": 15, "soul_lantern": 10, "torch": 14, "wall_torch": 14, "soul_torch": 10, "soul_wall_torch": 10, "redstone_torch": 7,
               "redstone_wall_torch": 7, "fire": 15, "soul_fire": 10, "lava": 15, "glowstone": 15, "sea_lantern": 15, "shroomlight": 15,
               "ochre_froglight": 15, "verdant_froglight": 15, "pearlescent_froglight": 15, "jack_o_lantern": 15, "end_rod": 14, "beacon": 15,
               "conduit": 15, "magma_block": 3, "crying_obsidian": 10, "enchanting_table": 7, "ender_chest": 7, "amethyst_cluster": 5,
               "large_amethyst_bud": 4, "medium_amethyst_bud": 2, "small_amethyst_bud": 1, "sculk_catalyst": 6, "brewing_stand": 1, "nether_portal": 11,
               "glow_lichen": 7, "respawn_anchor": 0, "lava_cauldron": 15, "cave_vines": 0}
LIGHT_LIT = {"campfire": 15, "soul_campfire": 10, "furnace": 13, "blast_furnace": 13, "smoker": 13, "redstone_lamp": 15, "copper_bulb": 15}
_OPAQUE_EXTRA = re.compile(r"bookshelf|chiseled_bookshelf|crafting_table|smithing_table|cartography_table|fletching_table|barrel|furnace|blast_furnace|smoker|"
                           r"tinted_glass|crafter|spawner|vault|trial_spawner|hay_block|bone_block|sculk|moss_block|dripstone_block|netherrack")
_TRANSPARENT = re.compile(r".*glass|.*glass_pane|iron_bars|.*leaves|ice|packed_ice|blue_ice|frosted_ice|slime_block|honey_block|water|lava|cobweb|.*stairs|.*slab|"
                          r".*fence|.*fence_gate|.*_wall|.*trapdoor|.*_door|ladder|chain|lantern|soul_lantern|.*torch|.*candle|campfire|soul_campfire|.*skull|"
                          r".*head|.*banner|lightning_rod|end_rod|.*_bed|.*carpet|cauldron|.*_cauldron|chest|trapped_chest|ender_chest|anvil|.*_anvil|grindstone|"
                          r"lectern|brewing_stand|flower_pot|potted_.*|pointed_dripstone|vine|glow_lichen|sculk_vein|.*button|.*pressure_plate|bell|conduit|"
                          r"scaffolding|snow|light|air|cave_air|void_air|structure_block|jigsaw|armor_stand|.*sign|.*coral.*|.*sapling|.*amethyst_bud|amethyst_cluster")


def light_emission(block, props):
    n = path_of(block)
    if n == "light":
        return int(props.get("level", "15"))
    if n in LIGHT_LIT:
        return LIGHT_LIT[n] if props.get("lit", "true") == "true" else 0
    if n == "candle" or n.endswith("_candle"):
        return 3 * int(props.get("candles", "1")) if props.get("lit", "false") == "true" else 0
    if n == "water_cauldron":
        return 0
    return LIGHT_FIXED.get(n, 0)


def blocks_light(block, props):
    n = path_of(block)
    if _TRANSPARENT.fullmatch(n):
        return False
    if _OPAQUE_EXTRA.fullmatch(n):
        return True
    return L.is_full(block) or n in ("deepslate", "stone", "cobblestone", "dirt", "obsidian", "reinforced_deepslate", "bedrock", "gilded_blackstone",
                                     "blackstone", "tuff", "calcite", "end_stone", "purpur_block", "quartz_block", "smooth_quartz", "prismarine", "sandstone")


def light_field(t):
    """{(x,y,z): level} of block light in template `t` (each emitter floods 1 per step through cells that do not block light)."""
    size = t.size
    opaque = {}
    emit = {}
    for p, c in t.cells.items():
        e = light_emission(c.block, c.props)
        if e > 0:
            emit[p] = e
        if blocks_light(c.block, c.props):
            opaque[p] = True
    level = dict(emit)
    q = deque(sorted(emit, key=lambda p: -emit[p]))
    while q:
        p = q.popleft()
        cur = level[p]
        if cur <= 1:
            continue
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nb = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
            if not (0 <= nb[0] < size[0] and 0 <= nb[1] < size[1] and 0 <= nb[2] < size[2]):
                continue
            if nb in opaque:
                continue
            if level.get(nb, 0) < cur - 1:
                level[nb] = cur - 1
                q.append(nb)
    return level


def standable_cells(t, first_only=False):
    """Cells a player can stand in (feet row) and reach from a doorway: solid below, two free cells above, flood-filled from the jigsaw sockets."""
    size = t.size

    def solid_below(x, y, z):
        c = t.cells.get((x, y - 1, z))
        if c is None:
            return False
        n = path_of(c.block)
        if n in ("air", "cave_air", "void_air", "light", "structure_block", "jigsaw", "water", "lava", "cobweb", "moss_carpet", "snow") or n.endswith(
                ("_carpet", "_pressure_plate", "_button", "_banner", "_wall_banner", "_skull", "_head", "_torch", "_candle", "candle", "_sign", "vine")):
            return False
        return True

    def free(x, y, z):
        c = t.cells.get((x, y, z))
        if c is None:
            return True
        n = path_of(c.block)
        if n in ("air", "cave_air", "void_air", "light", "structure_block", "jigsaw", "cobweb", "short_grass", "tall_grass", "fern", "vine", "glow_lichen",
                 "candle", "torch", "soul_torch", "wall_torch", "soul_wall_torch", "lantern", "soul_lantern", "chain", "skull", "wall_skull",
                 "moss_carpet", "hanging_roots", "snow", "sculk_vein"):
            return True
        if n.endswith(("_carpet", "_wall_banner", "_pressure_plate", "_button", "_candle", "_wall_skull", "_wall_head", "_skull", "_head", "_banner",
                       "_wall_sign", "_sign")):
            return True
        if n.endswith("_slab") and c.props.get("type") == "bottom":
            return True
        if n.endswith("_trapdoor") and (c.props.get("open") == "true" or c.props.get("half", "bottom") == "bottom"):
            return True
        return False

    ring = 0 if size[0] <= 3 else L.WALL      # the 2-thick wall ring (doorway holes are part of it) is a passage, not a place to stand: light it from both sides

    def standable(x, y, z):
        return (ring <= x < size[0] - ring and 1 <= y < size[1] - 2 and ring <= z < size[2] - ring
                and solid_below(x, y, z) and free(x, y, z) and free(x, y + 1, z))

    # seeds: the cell in front of every socket (inside the room)
    seeds = []
    for (x, y, z), o, _d in t.jigsaws():
        f = o.split("_")[0]
        dx, dz = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}.get(f, (0, 0))
        # step inside twice (through the 2-thick wall) to land in the interior (a cap has no wall ring: its jigsaw cell is the first cell)
        k = 2 if ring else 0
        seeds.append((x + k * dx, y, z + k * dz))
    seen = set()
    q = deque()
    for s in (seeds[:1] if first_only else seeds):
        if standable(*s):
            seen.add(s)
            q.append(s)
    while q:
        x, y, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):  # walk on the level, step up one, step down one
                nb = (x + dx, y + dy, z + dz)
                if nb in seen or not standable(*nb):
                    continue
                if dy == 1 and not free(x, y + 2, z):  # the step up needs headroom above the old cell
                    continue
                seen.add(nb)
                q.append(nb)
    return seen


def connectivity(t):
    """Walkability check: (unreachable sockets, unreachable markers). A socket counts as reachable when the cell two blocks inside its doorway can be
    walked to from the first socket; a marker (spawn, patrol, room_center) when the cell it stands in can. Uses the same walking model as the light check
    (stand on a solid block, two free cells above, one-block steps up and down)."""
    ring = 0 if t.size[0] <= 3 else L.WALL
    cells = standable_cells(t, first_only=True)
    seeds = []
    for (x, y, z), o, _d in t.jigsaws():
        f = o.split("_")[0]
        dx, dz = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}.get(f, (0, 0))
        k = 2 if ring else 0
        seeds.append(((x, y, z), (x + k * dx, y, z + k * dz)))
    bad_sockets = [pos for pos, seed in seeds if not any((seed[0], yy, seed[2]) in cells for yy in (seed[1] - 1, seed[1], seed[1] + 1))]
    bad_markers = []
    for p, c in t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            meta = str(c.nbt.get("metadata", ""))
            kind = meta.split(";")[0].replace("nusmp:", "")
            if kind in ("spawn", "patrol", "room_center", "arrival", "seat", "hearth", "bed", "guardian_spawn") and p not in cells:
                bad_markers.append((p, meta))
    return bad_sockets, bad_markers


def light_report(t, name=""):
    lf = light_field(t)
    cells = standable_cells(t)
    vals = []
    for (x, y, z) in cells:
        vals.append(lf.get((x, y + 1, z), 0))  # the eye cell (feet row + 1)
    if not vals:
        return {"cells": 0, "min": 0, "dark4": 0.0, "dim7": 0.0, "mean": 0.0}
    n = len(vals)
    return {"cells": n, "min": min(vals), "dark4": sum(1 for v in vals if v <= 4) / float(n), "dim7": sum(1 for v in vals if v <= 7) / float(n),
            "mean": sum(vals) / float(n)}


def format_report(name, r):
    return "%-44s cells %5d  min light %2d  mean %4.1f  <=4: %5.1f%%  <=7: %5.1f%%" % (name, r["cells"], r["min"], r["mean"], 100 * r["dark4"], 100 * r["dim7"])


# ------------------------------------------------------------------ rendering
def _rot_box(b, rot, sx, sz):
    """Rotates an absolute box (x0, y0, z0, x1, y1, z1, ...) a quarter turn `rot` times clockwise (seen from above) about the template's footprint."""
    x0, y0, z0, x1, y1, z1 = b[:6]
    rest = b[6:]
    for _ in range(rot):
        # (x, z) -> (sz - z, x): a box maps to a box with swapped extents
        x0, z0, x1, z1 = sz - z1, x0, sz - z0, x1
        sx, sz = sz, sx
    return (x0, y0, z0, x1, y1, z1) + tuple(rest)


def render(t, out, xr, yr, zr, scale, top=False, light=False, markers=True, lit=False, rot=0):
    cells = {p: c for p, c in t.cells.items() if xr[0] <= p[0] <= xr[1] and yr[0] <= p[1] <= yr[1] and zr[0] <= p[2] <= zr[1]}
    if top or light:
        render_plan(t, out, cells, xr, zr, scale, light)
        return
    items = []  # (x0, y0, z0, x1, y1, z1, color, cell)
    solid = {p for p, c in cells.items() if is_full_cube(c.block, c.props)}
    lf = light_field(t) if lit else None
    for (x, y, z), c in cells.items():
        n = path_of(c.block)
        if (x + 1, y, z) in solid and (x, y, z + 1) in solid and (x, y + 1, z) in solid:
            continue
        if not markers and n in ("structure_block", "jigsaw"):
            continue
        col = color_of(c.block)
        if n == "structure_block":
            col = (255, 0, 255, 255)
        elif n == "jigsaw":
            col = (255, 230, 0, 255)
        for b in boxes_for(c.block, c.props):
            items.append((x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], col, (x, y, z)))
    for ent in t.entities:
        try:
            ex, ey, ez = [float(v) for v in ent["pos"]]
        except Exception:  # noqa: BLE001
            continue
        if not (xr[0] <= ex <= xr[1] + 1 and yr[0] <= ey <= yr[1] + 2 and zr[0] <= ez <= zr[1] + 1):
            continue
        for b in entity_boxes(ent):
            items.append(tuple(b) + (None,))
    if not items:
        Image.new("RGB", (64, 64), (18, 18, 24)).save(out)
        return
    if rot:
        items = [_rot_box(b[:7], rot, t.size[0], t.size[2]) + (b[7],) for b in items]
    tw, th, vh = 2.0 * scale, float(scale), float(scale)

    def proj(x, y, z):
        return ((x - z) * tw / 2.0, (x + z) * th / 2.0 - y * vh)

    pts = []
    for (x0, y0, z0, x1, y1, z1, _c, _cell) in items:
        for x in (x0, x1):
            for y in (y0, y1):
                for z in (z0, z1):
                    pts.append(proj(x, y, z))
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    pad = 6
    img = Image.new("RGB", (int(maxx - minx) + 2 * pad, int(maxy - miny) + 2 * pad), (18, 18, 24))  # RGB, so translucent ink never leaves holes in the alpha channel
    d = ImageDraw.Draw(img, "RGBA")
    ox, oy = -minx + pad, -miny + pad
    items.sort(key=lambda b: ((b[0] + b[3]) * 0.5 + (b[1] + b[4]) * 0.5 + (b[2] + b[5]) * 0.5, b[1]))
    for (x0, y0, z0, x1, y1, z1, col, cell) in items:
        def pp(x, y, z):
            a, b = proj(x, y, z)
            return (a + ox, b + oy)
        a = col[3]
        left = [pp(x0, y1, z1), pp(x1, y1, z1), pp(x1, y0, z1), pp(x0, y0, z1)]      # south face (+z)
        right = [pp(x1, y1, z1), pp(x1, y1, z0), pp(x1, y0, z0), pp(x1, y0, z1)]    # east face (+x)
        topf = [pp(x0, y1, z0), pp(x1, y1, z0), pp(x1, y1, z1), pp(x0, y1, z1)]     # top face
        fl, fr, ft = 0.78, 0.60, 1.0
        if lf is not None and cell is not None:
            cx, cy, cz = cell
            own = lf.get(cell, 0)
            fl *= 0.25 + 0.75 * max(own, lf.get((cx, cy, cz + 1), 0)) / 15.0
            fr *= 0.25 + 0.75 * max(own, lf.get((cx + 1, cy, cz), 0)) / 15.0
            ft *= 0.25 + 0.75 * max(own, lf.get((cx, cy + 1, cz), 0)) / 15.0
        d.polygon(left, fill=P.shade(col, fl)[:3] + (a,))
        d.polygon(right, fill=P.shade(col, fr)[:3] + (a,))
        d.polygon(topf, fill=P.shade(col, ft)[:3] + (a,))
        if scale >= 8:
            for poly in (left, right, topf):
                d.line(poly + [poly[0]], fill=(8, 8, 12, 90), width=1)
    img.save(out)


def render_elevation(t, out, xr, yr, zr, scale, axis, lit=False):
    """Orthographic elevation: `axis` "z" looks north from the south side (a cross-section of a corridor, columns = x), "x" looks west from the east
    side (a wall elevation, columns = z). Only the boxes inside the given ranges are drawn, the nearest ones on top, far ones slightly darker, so a
    narrow range is a clean section through the template and a wide one a view down its length."""
    cells = {p: c for p, c in t.cells.items() if xr[0] <= p[0] <= xr[1] and yr[0] <= p[1] <= yr[1] and zr[0] <= p[2] <= zr[1]}
    lf = light_field(t) if lit else None
    items = []
    for (x, y, z), c in cells.items():
        col = color_of(c.block)
        n = path_of(c.block)
        if n == "structure_block":
            col = (255, 0, 255, 255)
        elif n == "jigsaw":
            col = (255, 230, 0, 255)
        for b in boxes_for(c.block, c.props):
            items.append((x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5], col, (x, y, z)))
    for ent in t.entities:
        try:
            ex, ey, ez = [float(v) for v in ent["pos"]]
        except Exception:  # noqa: BLE001
            continue
        if xr[0] <= ex <= xr[1] + 1 and yr[0] <= ey <= yr[1] + 2 and zr[0] <= ez <= zr[1] + 1:
            for b in entity_boxes(ent):
                items.append(tuple(b) + (None,))
    if not items:
        Image.new("RGB", (64, 64), (18, 18, 24)).save(out)
        return
    if axis == "z":
        hx0, hx1, dz0, dz1 = xr[0], xr[1] + 1, zr[0], zr[1] + 1
    else:
        hx0, hx1, dz0, dz1 = zr[0], zr[1] + 1, xr[0], xr[1] + 1
    y0, y1 = yr[0], yr[1] + 2
    W_, H_ = int((hx1 - hx0) * scale), int((y1 - y0) * scale)
    img = Image.new("RGB", (W_, H_), (18, 18, 24))
    d = ImageDraw.Draw(img, "RGBA")
    if axis == "z":
        items.sort(key=lambda b: (b[2] + b[5]) * 0.5)           # far (small z) first
    else:
        items.sort(key=lambda b: -(b[0] + b[3]) * 0.5)          # far (large x ... seen from the east: small x is far) -> draw large x last
        items.sort(key=lambda b: (b[0] + b[3]) * 0.5)
    for (bx0, by0, bz0, bx1, by1, bz1, col, cell) in items:
        if axis == "z":
            ax0, ax1, depth = bx0, bx1, (bz0 + bz1) * 0.5 - dz0
            span = dz1 - dz0
        else:
            ax0, ax1, depth = bz0, bz1, (bx0 + bx1) * 0.5 - dz0
            span = dz1 - dz0
        fade = 0.55 + 0.45 * (depth / max(1.0, span))
        f = fade
        if lf is not None and cell is not None:
            cx, cy, cz = cell
            nb = (cx, cy, cz + 1) if axis == "z" else (cx + 1, cy, cz)
            f *= 0.25 + 0.75 * max(lf.get(cell, 0), lf.get(nb, 0)) / 15.0
        px0, px1 = (ax0 - hx0) * scale, (ax1 - hx0) * scale
        py0, py1 = H_ - (by1 - y0) * scale, H_ - (by0 - y0) * scale
        d.rectangle([px0, py0, px1 - 1, py1 - 1], fill=P.shade(col, f)[:3] + (col[3],))
        if scale >= 8:
            d.rectangle([px0, py0, px1 - 1, py1 - 1], outline=(8, 8, 12, 80))
    img.save(out)


LIGHT_COLORS = [(0, (170, 30, 30)), (5, (225, 120, 40)), (8, (225, 205, 70)), (11, (140, 205, 90)), (14, (230, 250, 200))]


def light_color(v):
    c = LIGHT_COLORS[0][1]
    for lim, col in LIGHT_COLORS:
        if v >= lim:
            c = col
    return c


def render_plan(t, out, cells, xr, zr, scale, light):
    w, h = (xr[1] - xr[0] + 1) * scale, (zr[1] - zr[0] + 1) * scale
    img = Image.new("RGB", (w, h), (18, 18, 24))
    d = ImageDraw.Draw(img)
    lf = light_field(t) if light else None
    stand = standable_cells(t) if light else None
    best = {}
    for (x, y, z), c in cells.items():
        if path_of(c.block) in ("air", "light"):
            continue
        if (x, z) not in best or y > best[(x, z)][0]:
            best[(x, z)] = (y, c)
    for (x, z), (y, c) in best.items():
        rect = [(x - xr[0]) * scale, (z - zr[0]) * scale, (x - xr[0] + 1) * scale - 1, (z - zr[0] + 1) * scale - 1]
        if light:
            col = (52, 52, 58)
            d.rectangle(rect, fill=col)
        else:
            base = color_of(c.block)
            f = 0.6 + 0.4 * (y - L.FLOOR_ROW) / 30.0 if y > L.FLOOR_ROW else 0.55
            d.rectangle(rect, fill=P.shade(base, f)[:3])
    if light:
        for (x, y, z) in stand:
            if not (xr[0] <= x <= xr[1] and zr[0] <= z <= zr[1]):
                continue
            v = lf.get((x, y + 1, z), 0)
            d.rectangle([(x - xr[0]) * scale, (z - zr[0]) * scale, (x - xr[0] + 1) * scale - 1, (z - zr[0] + 1) * scale - 1], fill=light_color(v))
    img.save(out)


def main(argv):
    args = list(argv)
    if not args or "-h" in args or "--help" in args:
        print(__doc__)
        return 0
    opts = {}
    for key in ("--x", "--y", "--z", "--scale", "--root", "--elev", "--rot"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    flags = {f for f in ("--top", "--light", "--lightreport", "--nocut", "--nomarkers", "--lit") if f in args}
    args = [a for a in args if a not in flags]
    name = args[0]
    path = os.path.join(opts.get("--root", L.RES), "data", "nusmp", "structure", name + ".nbt")
    t = load(path, name)
    if "--lightreport" in flags:
        print(format_report(name, light_report(t, name)))
        bs, bm = connectivity(t)
        if bs or bm:
            print("   NOT WALKABLE: sockets %s markers %s" % (bs, bm))
        return 0
    out = args[1]
    scale = int(opts.get("--scale", 10))
    sx, sy, sz = t.size
    if "--nocut" in flags:
        xr, yr, zr = (0, sx - 1), (0, sy - 1), (0, sz - 1)
    else:
        xr, yr, zr = P.parse_range(opts.get("--x"), 0, sx - 1), P.parse_range(opts.get("--y"), 0, sy - 1), P.parse_range(opts.get("--z"), 0, sz - 1)
    if "--elev" in opts:
        render_elevation(t, out, xr, yr, zr, scale, opts["--elev"], lit="--lit" in flags)
    else:
        render(t, out, xr, yr, zr, scale, top="--top" in flags, light="--light" in flags, markers="--nomarkers" not in flags, lit="--lit" in flags,
               rot=int(opts.get("--rot", 0)))
    print("wrote", out)
    if "--light" in flags:
        print(format_report(name, light_report(t, name)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
