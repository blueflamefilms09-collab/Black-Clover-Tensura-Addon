#!/usr/bin/env python3
"""
Previews of the THEME-A rooms (1.59.0: Holy Church, Celestial Court, Sky Citadel, Mountain Monastery) as a player of that theme would see them.

    python3 tools/labyrinth/mega_tha_view.py mega/hall/holy_church_nave holy_church out_prefix --iso 0:26,8:34,0:38 --cam 13.5,10.6,40 --look 13.5,14,5
    python3 tools/labyrinth/mega_tha_view.py mega/hall/holy_church_nave none out_prefix --views            # the plain deepslate palette, four standard views

Why: the rooms are written in the deepslate palette and the Region Theme swaps the families at generation time, so a picture of the file alone shows the wrong
colours. This applies the SAME swap as `mega_theme_preview.py` (a Python port of `ThemeSwap`, explicit map first) to a copy of the template and then draws it
with the two renderers of the lead (`mega_shape_preview`: cutaway isometric; `mega_walk_preview`: a first-person ray caster), after teaching their colour
tables the handful of blocks these four themes bring in (concrete, polished andesite and diorite, bamboo, copper, stone bricks), which the table of the
original eleven themes had no colour for. The colours are a keyword table, not the game's textures: a judge of shape, light, silhouette and colour mix.
It never writes into the repository.
"""
import copy
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
import mega_walk_preview as WP  # noqa: E402
import mega_theme_preview as TP  # noqa: E402
from template import load  # noqa: E402

# colours of the blocks the four themes bring in (r, g, b, alpha); first match wins, tried before the shared table
_EXTRA = [
    (r"polished_andesite.*|andesite.*", (136, 136, 134, 255)),
    (r"polished_diorite.*|diorite.*", (200, 200, 198, 255)),
    (r"stone_brick.*|stone_bricks|cracked_stone_bricks|chiseled_stone_bricks", (122, 122, 122, 255)),
    (r"smooth_quartz.*|quartz_block|quartz_slab|quartz_stairs|quartz_pillar|chiseled_quartz_block", (236, 232, 224, 255)),
    (r"white_concrete", (240, 242, 244, 255)),
    (r"light_gray_concrete", (150, 150, 144, 255)),
    (r"light_blue_concrete", (60, 172, 218, 255)),
    (r"snow_block|snow", (246, 250, 250, 255)),
    (r"calcite", (225, 225, 220, 255)),
    (r"gold_block|raw_gold_block", (250, 214, 62, 255)),
    (r"bamboo_mosaic.*|bamboo_planks|bamboo_stairs|bamboo_slab", (214, 194, 106, 255)),
    (r"stripped_bamboo_block|bamboo_block", (220, 204, 112, 255)),
    (r"bamboo", (112, 160, 40, 255)),
    (r"red_terracotta", (143, 61, 46, 255)),
    (r"white_terracotta", (209, 178, 161, 255)),
    (r"spruce_.*|stripped_spruce_.*", (110, 80, 46, 255)),
    (r"oak_.*|stripped_oak_.*", (172, 140, 84, 255)),
    (r"birch_.*|stripped_birch_.*", (216, 200, 140, 255)),
    (r"cobblestone.*", (128, 128, 126, 255)),
    (r"mossy_cobblestone.*", (104, 128, 100, 255)),
    (r"cherry_leaves|pink_petals", (232, 160, 190, 255)),
    (r"cherry_log|stripped_cherry_log|cherry_wood", (140, 90, 90, 255)),
    (r"podzol", (104, 76, 44, 255)),
    (r"lily_pad", (40, 120, 40, 255)),
    (r"grass_block|short_grass|tall_grass|fern|large_fern|oak_leaves|azalea_leaves|flowering_azalea_leaves", (88, 150, 52, 255)),
    (r"moss_block|moss_carpet", (92, 130, 44, 255)),
    (r"gravel", (140, 134, 132, 255)),
    (r"smooth_stone.*|stone|stone_slab|stone_stairs", (140, 140, 140, 255)),
    (r"white_wool|white_carpet", (236, 236, 236, 255)),
    (r"red_carpet|red_wool", (170, 36, 36, 255)),
    (r"packed_ice|blue_ice|ice", (150, 190, 250, 200)),
    (r"white_stained_glass.*", (240, 240, 250, 120)),
    (r"hay_block", (210, 176, 56, 255)),
    (r"sea_lantern", (210, 240, 235, 255)),
]
_EXTRA = [(re.compile(p), c) for p, c in _EXTRA]
_orig_color = SP.color_of


def _color(block):
    n = SP.path_of(block)
    if not re.fullmatch(r".*(_stained_glass|_stained_glass_pane)", n):
        for pat, c in _EXTRA:
            if pat.fullmatch(n):
                return c
    return _orig_color(block)


SP.color_of = _color


def template_of(ref):
    """A Template from a saved file ('mega/hall/x') or an in-memory one (anything with .cells)."""
    if hasattr(ref, "cells"):
        return copy.deepcopy(ref)
    path = os.path.join(L.RES, "data", "nusmp", "structure", ref + ".nbt")
    return load(path, ref)


def themed(ref, theme_id):
    """A copy of the template with a theme applied ('none' / '' / 'gravity' leaves the deepslate palette)."""
    tpl = template_of(ref)
    if theme_id and theme_id not in ("none", "gravity"):
        exists = TP.vanilla_ids().__contains__
        states = json.load(open(TP.STATES, encoding="utf-8"))
        TP.retheme(tpl, theme_id, exists, states)
    return tpl


def iso(tpl, out, xr, yr, zr, scale=12, lit=True, rot=0):
    SP.render(tpl, out, xr, yr, zr, scale, lit=lit, rot=rot)


def walk(tpl, out, cam, look, fov=85.0, size=(800, 450), scene=None, gamma=1.4):
    scene = scene or WP.Scene(tpl)
    img = WP.render(scene, cam, look, fov, size, gamma=gamma)
    img.save(out)
    return scene


def _range(s):
    a, b = s.split(":")
    return (int(a), int(b))


def main(argv):
    args = list(argv)
    if len(args) < 3 or "-h" in args:
        print(__doc__)
        return 0
    ref, theme_id, prefix = args[0], args[1], args[2]
    rest = args[3:]
    tpl = themed(ref, theme_id)
    sx, sy, sz = tpl.size
    i = 0
    cam = look = None
    fov = 85.0
    size = (800, 450)
    scene = None
    n = 0
    while i < len(rest):
        a = rest[i]
        if a == "--iso":
            x, y, z = rest[i + 1].split(",")
            iso(tpl, "%s_iso%d.png" % (prefix, n), _range(x), _range(y), _range(z), scale=int(rest[i + 2]) if i + 2 < len(rest) and rest[i + 2].isdigit() else 12)
            print("wrote %s_iso%d.png" % (prefix, n))
            n += 1
            i += 2
        elif a == "--cam":
            cam = tuple(float(v) for v in rest[i + 1].split(","))
            i += 2
        elif a == "--look":
            look = tuple(float(v) for v in rest[i + 1].split(","))
            scene = walk(tpl, "%s_walk%d.png" % (prefix, n), cam, look, fov, size, scene)
            print("wrote %s_walk%d.png" % (prefix, n))
            n += 1
            i += 2
        elif a == "--fov":
            fov = float(rest[i + 1])
            i += 2
        elif a == "--size":
            size = tuple(int(v) for v in rest[i + 1].split("x"))
            i += 2
        elif a == "--views":
            for tag, c, lk in WP.standard_views(tpl):
                walk(tpl, "%s_%s.png" % (prefix, tag), c, lk, fov, size)
                print("wrote %s_%s.png" % (prefix, tag))
            i += 1
        else:
            i += 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
