#!/usr/bin/env python3
"""
Why: the four deep-and-infernal themes (THEME-D: fungal_underdark, infernal_foundry, drowned_crypt, spider_warrens, docs/labyrinth/mega_v2_thd.md) are rooms that nobody
can walk into before the game exists, and the templates are written in the deepslate PALETTE that the Region Theme processor restyles per sector. A picture of the raw
template shows grey deepslate and says nothing about the place a player will see. This script loads a finished template, applies the SAME block swap the processor
applies (`mega_theme_preview.retheme`, a port of ThemeSwap, with the theme's own data file) and draws it with the colours of the blocks that these themes bring in
(mushroom caps, obsidian, mud brick, dark prismarine, calcite, wool ...), so a room can be judged in its theme before it is ever placed.

    python3 tools/labyrinth/thd_preview.py mega/hall/fungal_underdark_drow_enclave fungal_underdark out_prefix            # iso SE + NW cutaway, plan, four walk views
    python3 tools/labyrinth/thd_preview.py <template> <theme> out_prefix --h 20 --only iso,plan                             # interior height 20 rows, only these pictures
    python3 tools/labyrinth/thd_preview.py <template> <theme> out_prefix --walk 20,10.6,5:20,10.6,30 --fov 90             # one custom first-person view: cam:look
    python3 tools/labyrinth/thd_preview.py <template> raw out_prefix                                                         # the template as written (no theme swap)

The pictures are sketches (flat colours, a block grid on every face), the same viewers as the other builders use (`mega_shape_preview`, `mega_walk_preview`): shape,
silhouette, light and the mix of colours can be judged, textures cannot.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
import mega_theme_preview as MTP  # noqa: E402
import mega_walk_preview as WP  # noqa: E402
from template import load  # noqa: E402

FR = L.FLOOR_ROW

#: colour rules for the blocks these themes bring in (first match wins; tried before the viewer's own table)
COLOURS = [
    (r"red_mushroom_block", (196, 38, 38, 255)), (r"brown_mushroom_block", (150, 100, 62, 255)), (r"mushroom_stem", (222, 212, 188, 255)),
    (r"obsidian", (22, 16, 36, 255)), (r"crying_obsidian", (92, 36, 150, 255)), (r"mycelium", (112, 96, 124, 255)), (r"podzol", (92, 66, 36, 255)),
    (r"mud_brick.*", (128, 104, 92, 255)), (r"packed_mud", (136, 104, 76, 255)), (r"mud", (62, 52, 52, 255)),
    (r"smooth_basalt|basalt|polished_basalt", (62, 62, 70, 255)), (r"polished_blackstone.*", (52, 46, 62, 255)), (r"blackstone.*|gilded_blackstone", (42, 38, 46, 255)),
    (r"warped_planks|warped_stairs|warped_slab|warped_fence.*|warped_trapdoor|warped_door", (44, 112, 104, 255)), (r"warped_wart_block", (24, 128, 112, 255)),
    (r"warped_stem|stripped_warped_stem", (62, 110, 112, 255)), (r"warped_nylium", (34, 130, 90, 255)), (r"warped_fungus|warped_roots|nether_sprouts", (40, 160, 130, 255)),
    (r"nether_wart_block", (110, 20, 24, 255)), (r"shroomlight", (252, 168, 74, 255)), (r"verdant_froglight", (160, 230, 100, 255)), (r"ochre_froglight", (240, 220, 120, 255)), (r"pearlescent_froglight", (230, 200, 240, 255)), (r"glow_lichen", (112, 190, 150, 120)), (r"red_mushroom|brown_mushroom", (190, 80, 70, 255)),
    (r"red_nether_bricks.*", (128, 28, 32, 255)), (r"cracked_nether_bricks|nether_bricks.*|nether_brick.*", (52, 26, 32, 255)), (r"chiseled_nether_bricks", (70, 30, 36, 255)),
    (r"magma_block", (200, 82, 28, 255)), (r"crimson_.*", (110, 44, 70, 255)), (r"netherrack", (100, 40, 40, 255)), (r"soul_sand|soul_soil", (86, 68, 56, 255)),
    (r"dark_prismarine.*", (46, 92, 78, 255)), (r"prismarine_bricks.*", (92, 170, 156, 255)), (r"prismarine.*", (110, 180, 164, 255)),
    (r"mossy_stone_bricks.*|mossy_cobblestone.*", (98, 120, 90, 255)), (r"moss_block|moss_carpet", (84, 112, 44, 255)),
    (r"dead_.*_coral_block|dead_.*coral.*", (120, 124, 136, 255)), (r"dripstone_block", (136, 108, 92, 255)), (r"pointed_dripstone", (150, 120, 100, 255)),
    (r"spruce_.*", (98, 72, 44, 255)), (r"kelp|kelp_plant|seagrass|tall_seagrass", (40, 130, 70, 255)), (r"sea_pickle", (120, 170, 60, 255)),
    (r"calcite", (222, 222, 218, 255)), (r"white_wool|white_carpet", (236, 236, 236, 255)), (r"cobweb", (232, 232, 232, 110)),
    (r"tuff.*|polished_tuff.*", (104, 106, 98, 255)), (r"cobbled_deepslate.*", (86, 88, 98, 255)), (r"deepslate_tile.*", (74, 76, 86, 255)),
    (r"turtle_egg|sniffer_egg", (232, 226, 200, 255)), (r"slime_block", (110, 200, 100, 190)), (r"honey_block", (240, 170, 40, 200)),
    (r"hay_block", (206, 176, 62, 255)), (r"iron_block|iron_bars|chain", (160, 160, 168, 255)), (r"anvil|chipped_anvil|damaged_anvil", (70, 70, 78, 255)),
    (r"lava", (255, 120, 20, 255)), (r"soul_campfire|soul_fire|soul_lantern|soul_torch|soul_wall_torch", (90, 220, 240, 255)), (r"gold_block", (240, 200, 60, 255)),
    (r"bone_block", (226, 220, 194, 255)), (r"copper_block|.*cut_copper.*|waxed_.*copper.*", (190, 112, 82, 255)), (r"lectern|bookshelf|chiseled_bookshelf", (150, 106, 60, 255)),
    (r"purple_stained_glass.*|magenta_stained_glass.*", (150, 70, 190, 130)), (r"lime_stained_glass.*", (130, 210, 60, 130)), (r"cyan_stained_glass.*|light_blue_stained_glass.*", (60, 170, 200, 130)),
]
_COMPILED = [(re.compile(p), c) for p, c in COLOURS]
_orig_color = SP.color_of


def colour(block):
    n = block.split(":", 1)[1] if ":" in block else block
    for pat, c in _COMPILED:
        if pat.fullmatch(n):
            return c
    return _orig_color(block)


SP.color_of = colour


def themed(path, name, theme_id):
    """The template loaded from `path`, with the swap of `theme_id` applied ('raw' keeps it as written)."""
    t = load(path, name)
    if theme_id and theme_id != "raw":
        exists = MTP.vanilla_ids().__contains__
        states = json.load(open(MTP.STATES, encoding="utf-8"))
        MTP.retheme(t, theme_id, exists, states)
    return t


def guess_top(t):
    """The last interior row: the highest row where more than 70 % of the interior columns carry a block is the ceiling's first layer; the interior ends under it."""
    sx, sy, sz = t.size
    cols = (sx - 4) * (sz - 4)
    by_row = {}
    for (x, y, z), c in t.cells.items():
        if 2 <= x < sx - 2 and 2 <= z < sz - 2 and y > FR + 2:
            by_row[y] = by_row.get(y, 0) + 1
    for y in sorted(by_row):
        if by_row[y] > 0.7 * cols:
            return y - 1
    return max((y for (_x, y, _z) in t.cells), default=FR + 12)


def doors(t):
    """[(cam, look)] first-person views from just inside every doorway, looking at the middle of the room."""
    out = []
    sx, sy, sz = t.size
    eye = FR + 1 + 1.6
    for (x, y, z), o, _d in t.jigsaws():
        face = o.split("_")[0]
        cx, cz = {"north": (x + 0.5, 2.6), "south": (x + 0.5, sz - 2.6), "west": (2.6, z + 0.5), "east": (sx - 2.6, z + 0.5)}[face]
        out.append(((cx, eye, cz), (sx / 2.0, eye - 0.4, sz / 2.0)))
    return out


def main(argv):
    args = list(argv)
    opts = {}
    for key in ("--h", "--only", "--walk", "--fov", "--size", "--scale", "--res", "--root", "--x", "--y", "--z"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    if len(args) < 3:
        print(__doc__)
        return 2
    template, theme_id, prefix = args[0], args[1], args[2]
    root = opts.get("--root", L.RES)
    path = os.path.join(root, "data", "nusmp", "structure", template + ".nbt")
    t = themed(path, template, theme_id)
    sx, sy, sz = t.size
    only = set((opts.get("--only") or "iso,plan,walk").split(","))
    top = int(opts["--h"]) + FR if "--h" in opts else guess_top(t)
    scale = int(opts.get("--scale", 11))
    os.makedirs(os.path.dirname(os.path.abspath(prefix)), exist_ok=True)
    if "iso" in only:
        def rng(key, lo, hi):
            if key not in opts:
                return (lo, hi)
            a, b = opts[key].split(":")
            return (int(a or lo), int(b or hi))
        xr, yr, zr = rng("--x", 0, sx - 3), rng("--y", 0, top), rng("--z", 0, sz - 3)
        SP.render(t, prefix + "_isoSE.png", xr, yr, zr, scale, markers=False)
        if "--x" not in opts and "--z" not in opts:
            SP.render(t, prefix + "_isoNW.png", (2, sx - 1), yr, (2, sz - 1), scale, markers=False, rot=2)
        print("wrote", prefix + "_isoSE.png", prefix + "_isoNW.png", "(interior top row %d)" % top)
    if "plan" in only:
        SP.render(t, prefix + "_plan.png", (0, sx - 1), (int(opts["--y"].split(":")[0] or 0), int(opts["--y"].split(":")[1])) if "--y" in opts else (0, top), (0, sz - 1), max(10, scale), top=True)
        print("wrote", prefix + "_plan.png")
    if "walk" in only:
        scene = WP.Scene(t, res=int(opts.get("--res", 4)))
        w, h = (int(v) for v in opts.get("--size", "800x450").split("x"))
        fov = float(opts.get("--fov", 90))
        if "--walk" in opts:
            views = []
            for k, spec in enumerate(opts["--walk"].split(";")):
                cam, look = spec.split(":")
                views.append(("c%d" % k, tuple(float(v) for v in cam.split(",")), tuple(float(v) for v in look.split(","))))
        else:
            views = [("d%d" % i, c, lk) for i, (c, lk) in enumerate(doors(t)[:4])]
            ey = FR + 1 + 1.6
            views += [("mid%s" % tag, (sx / 2.0, ey, sz / 2.0), look) for tag, look in (("N", (sx / 2.0, ey, 0.0)), ("S", (sx / 2.0, ey, float(sz))))]
        for tag, cam, look in views:
            img = WP.render(scene, cam, look, fov, (w, h))
            img.save("%s_walk_%s.png" % (prefix, tag))
            print("wrote %s_walk_%s.png" % (prefix, tag))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
