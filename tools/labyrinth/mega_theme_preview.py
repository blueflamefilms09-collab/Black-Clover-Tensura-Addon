#!/usr/bin/env python3
"""
The Region Theme processor, seen without the game: renders a Mega-Dungeon template in every theme of `data/nusmp/mega/themes/*.json` (1.54.0, 1.55.0,
docs/labyrinth/mega_theme.md section 3 and 4).

    python3 tools/labyrinth/mega_theme_preview.py mega/vault/three_b out_prefix                  # one PNG per theme and a sheet of all of them
    python3 tools/labyrinth/mega_theme_preview.py mega/vault/three_a out_prefix --cam 13.5,10.8,18.5 --yaw 180 --pitch 6
    python3 tools/labyrinth/mega_theme_preview.py --table blood                                  # print the swap table of one theme (palette block -> target, how)
    python3 tools/labyrinth/mega_theme_preview.py --table-all > swaps.txt                        # the same for every theme (diff it against the Java printout)

Why: the processor changes every block of a sector and nobody can see the result without a game; this script applies the SAME rules to a template's cells
and renders it with the first-person viewer of `mega_vault_view.py`, so an owner can look at "the vault in Blood, in Frost, in Radiant" before a single sector
is built, and so a theme file with a wrong id or an ugly mix shows up on a picture. The rules are a Python port of `labyrinth/mega/theme/ThemeSwap.java` (the
explicit map first, then the family base plus the shape's suffix, singulars of bricks and tiles, the fallback slots, wood never falling back to stone; the
block states keep the properties the new block has, which is what `Block.withPropertiesOf` does; the light blocks move by the theme's `light` shift, never
under level 6). The Java class and its tests are the truth; `--table-all` prints exactly the lines `ThemeSwapTest`'s printout has, so the two can be diffed.
The colours come from a keyword table (the viewer's own plus the blocks the themes bring in): a judge of shape, light and mix, not of textures.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

THEMES = os.path.join(L.RES, "data", "nusmp", "mega", "themes")
STATES = os.path.join(HERE, "vanilla_block_states_1_21_1.json")
BLOCKS = os.path.join(HERE, "vanilla_blocks_1_21_1.txt")
ORDER = ["gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral", "radiant", "abyss"]
LIGHT_FLOOR = 6

# ---------------------------------------------------------------------------------------------------------------------------------------------- the rules
# (id, slot, shape): the 31 blocks of mega_lib.PALETTE, exactly as ThemeSwap.PALETTE lists them
SLOTS = ("brick", "tile", "polished", "chiseled", "cobbled", "cracked_brick", "cracked_tile", "rock", "wood")
FALLBACK = {
    "brick": ("tile", "polished", "cobbled"), "tile": ("brick", "polished", "cobbled"), "polished": ("brick", "tile", "cobbled"),
    "chiseled": ("polished", "brick", "tile"), "cobbled": ("brick", "polished", "tile"), "cracked_brick": ("brick", "tile"),
    "cracked_tile": ("tile", "brick"), "rock": ("cobbled", "brick"), "wood": (),
}
SUFFIX = {"plain": "", "stairs": "_stairs", "slab": "_slab", "wall": "_wall", "fence": "_fence", "fence_gate": "_fence_gate", "trapdoor": "_trapdoor",
          "door": "_door", "button": "_button", "pressure_plate": "_pressure_plate"}


def palette():
    """The 31 blocks of mega_lib.PALETTE as (id, slot, shape), in the order of ThemeSwap.PALETTE."""
    res = []

    def stone(plain, family, slot, shapes=("stairs", "slab", "wall")):
        res.append((plain, slot, "plain"))
        for shape in shapes:
            res.append((family + SUFFIX[shape], slot, shape))

    stone("deepslate_bricks", "deepslate_brick", "brick")
    stone("deepslate_tiles", "deepslate_tile", "tile")
    stone("polished_deepslate", "polished_deepslate", "polished")
    res.append(("chiseled_deepslate", "chiseled", "plain"))
    stone("cobbled_deepslate", "cobbled_deepslate", "cobbled")
    res += [("cracked_deepslate_bricks", "cracked_brick", "plain"), ("cracked_deepslate_tiles", "cracked_tile", "plain"), ("deepslate", "rock", "plain")]
    res += [("dark_oak_planks", "wood", "plain"), ("dark_oak_stairs", "wood", "stairs"), ("dark_oak_slab", "wood", "slab"), ("dark_oak_fence", "wood", "fence"),
            ("dark_oak_fence_gate", "wood", "fence_gate"), ("dark_oak_trapdoor", "wood", "trapdoor"), ("dark_oak_door", "wood", "door"),
            ("dark_oak_log", "wood", "log"), ("stripped_dark_oak_log", "wood", "stripped_log"), ("dark_oak_button", "wood", "button"),
            ("dark_oak_pressure_plate", "wood", "pressure_plate")]
    return [("minecraft:" + i, slot, shape) for i, slot, shape in res]


def candidates(base, slot, shape):
    if not base or not base.strip():
        return []
    ident = base.strip()
    ns, path = (ident.split(":", 1) + [""])[:2] if ":" in ident else ("minecraft", ident)
    ns += ":"
    if shape == "plain":
        return [ns + path]
    if slot == "wood":
        stem = path[:-len("_planks")] if path.endswith("_planks") else path
        if shape == "log":
            return [ns + stem + "_log", ns + stem + "_stem", ns + stem + "_block"]
        if shape == "stripped_log":
            return [ns + "stripped_" + stem + "_log", ns + "stripped_" + stem + "_stem", ns + "stripped_" + stem + "_block"]
        return [ns + stem + SUFFIX[shape]]
    suffix = SUFFIX[shape]
    out = [ns + path + suffix]
    if path.endswith("bricks") or path.endswith("tiles"):
        out.append(ns + path[:-1] + suffix)
    if path.endswith("_block"):
        out.append(ns + path[:-len("_block")] + suffix)
    return out


def resolve(entry, families, explicit, exists):
    """(target or None, how) for one palette block: how is SAME, EXPLICIT, DIRECT, DERIVED, FALLBACK or NONE."""
    block, slot, shape = entry
    ex = (explicit or {}).get(block)
    if ex and ex.strip() and exists(ex.strip()):
        return ex.strip(), ("SAME" if ex.strip() == block else "EXPLICIT")
    order = [slot] + list(FALLBACK.get(slot, ()))
    for s, sl in enumerate(order):
        cands = candidates((families or {}).get(sl), sl, shape)
        for c, target in enumerate(cands):
            if not exists(target):
                continue
            if target == block:
                return target, "SAME"
            return target, ("FALLBACK" if s > 0 else ("DIRECT" if c == 0 else "DERIVED"))
    return None, "NONE"


def swap_table(theme, exists):
    """{block id: (target, how)} for the 31 palette blocks and the explicit pairs that are not palette blocks (the order of ThemeSwap.table)."""
    out = {}
    for e in palette():
        out[e[0]] = resolve(e, theme.get("families"), theme.get("explicit"), exists)
    for src, dst in (theme.get("explicit") or {}).items():
        if src in out or src == dst or not exists(dst.strip()):
            continue
        out[src] = (dst.strip(), "EXPLICIT")
    return out


def shift_light(level, shift):
    """ThemeSwap.shiftLight: moved by the shift, never above 15, never under the floor of 6 (or the block's own lower level); level 0 stays 0."""
    if level <= 0:
        return level
    return max(min(level, LIGHT_FLOOR), min(15, level + shift))


# ---------------------------------------------------------------------------------------------------------------------------------------------- data
def vanilla_ids():
    ids = set()
    for ln in open(BLOCKS, encoding="utf-8"):
        t = ln.strip()
        if t and not t.startswith("#"):
            ids.add("minecraft:" + t)
    return ids


def theme(theme_id):
    return json.load(open(os.path.join(THEMES, theme_id + ".json"), encoding="utf-8"))


def theme_ids():
    found = sorted(os.path.basename(p)[:-5] for p in os.listdir(THEMES) if p.endswith(".json"))
    return [t for t in ORDER if t in found] + [t for t in found if t not in ORDER]


def printout(theme_id, exists):
    """The lines the Java harness prints for one theme: ``=== id`` then ``  source -> target  HOW`` per block, ids without the namespace."""
    lines = ["=== " + theme_id]
    for src, (dst, how) in swap_table(theme(theme_id), exists).items():
        lines.append("  %-44s -> %-44s %s" % (src.replace("minecraft:", ""), "-" if dst is None else dst.replace("minecraft:", ""), how))
    return lines


# ---------------------------------------------------------------------------------------------------------------------------------------------- the picture
EXTRA_COLOURS = [
    (r"warped_.*", (43, 104, 99), False), (r"crimson_.*", (101, 48, 70), False), (r"mangrove_.*", (117, 54, 48), False), (r"jungle_.*", (160, 115, 81), False),
    (r"spruce_.*", (114, 84, 48), False), (r"birch_.*", (192, 175, 121), False), (r"red_nether_brick.*", (124, 26, 30), False),
    (r"gilded_blackstone", (66, 52, 44), False), (r"end_stone.*", (219, 222, 158), False), (r"purpur.*", (169, 125, 169), False),
    (r"amethyst.*", (133, 97, 191), False), (r"mossy_.*", (100, 118, 90), False), (r"moss_block", (89, 109, 45), False), (r"sculk", (18, 32, 36), False),
    (r"tuff.*|polished_tuff.*", (100, 103, 96), False), (r".*oxidized.*copper.*", (80, 160, 135), False), (r".*weathered.*copper.*", (108, 153, 110), False),
    (r".*exposed.*copper.*", (161, 125, 103), False), (r"dark_prismarine.*", (51, 91, 75), False), (r"prismarine.*", (99, 171, 158), False),
    (r"smooth_quartz.*|quartz.*", (232, 228, 222), False), (r"smooth_stone.*", (158, 158, 158), False), (r"basalt.*|smooth_basalt", (72, 72, 78), False),
    (r"calcite", (224, 224, 220), False), (r"netherrack", (97, 38, 38), False),
]


def retheme(tpl, theme_id, exists, states):
    """Applies a theme to a loaded Template in place; returns how many blocks changed."""
    t = theme(theme_id)
    table = {k.replace("minecraft:", ""): (v[0] or "").replace("minecraft:", "") for k, v in swap_table(t, exists).items() if v[0]}
    shift = int(t.get("light", 0))
    n = 0
    for c in tpl.cells.values():
        if not c.block.startswith("minecraft:"):
            continue
        name = c.block[len("minecraft:"):]
        if name == "light" and shift:
            c.props = dict(c.props)
            c.props["level"] = str(shift_light(int(c.props.get("level", "15")), shift))
            continue
        target = table.get(name)
        if target and target != name:
            allowed = states.get(target, {})
            c.block = "minecraft:" + target
            c.props = {k: v for k, v in c.props.items() if k in allowed and v in allowed[k]}
            n += 1
    return n


def render_all(template_id, prefix, cam, yaw, pitch, fov, size):
    from PIL import Image, ImageDraw
    import mega_vault_view as V
    from template import load
    V._COMPILED[0:0] = [(re.compile(p), c, e) for p, c, e in EXTRA_COLOURS]
    exists = vanilla_ids().__contains__
    states = json.load(open(STATES, encoding="utf-8"))
    path = os.path.join(L.RES, "data", "nusmp", "structure", template_id + ".nbt")
    shots = []
    for tid in theme_ids():
        tpl = load(path, template_id)
        retheme(tpl, tid, exists, states)
        c = cam if cam else ((13.5, 10.8, tpl.size[2] - 14.0) if template_id.endswith("_b") else (13.5, 10.8, 18.5))
        img = V.render(V.Scene(tpl), c, yaw, pitch, fov, size[0], size[1])
        f = "%s_%s.png" % (prefix, tid)
        img.save(f)
        shots.append((tid, img))
        print("wrote", f)
    cols, tw, th = 3, 420, 262
    rows = (len(shots) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (tw + 8) + 8, rows * (th + 30) + 8), (18, 18, 24))
    d = ImageDraw.Draw(sheet)
    for i, (tid, img) in enumerate(shots):
        x, y = 8 + (i % cols) * (tw + 8), 8 + (i // cols) * (th + 30)
        sheet.paste(img.resize((tw, th), Image.LANCZOS), (x, y + 18))
        j = theme(tid)
        snd = j.get("sound", {}).get("id", "").replace("minecraft:", "")
        d.text((x, y + 2), "%s   light %+d   danger %.2f   %s" % (j.get("name", tid), j.get("light", 0), j.get("danger", 1.0), snd), fill=(235, 220, 160))
    sheet.save(prefix + "_sheet.png")
    print("wrote", prefix + "_sheet.png")


def main(argv):
    args = list(argv)

    def opt(name, default=None):
        if name in args:
            i = args.index(name)
            v = args[i + 1]
            del args[i:i + 2]
            return v
        return default

    exists = vanilla_ids().__contains__
    only = opt("--table")
    if only:
        print("\n".join(printout(only, exists)))
        return 0
    if "--table-all" in args:
        for tid in theme_ids():
            print("\n".join(printout(tid, exists)))
        return 0
    cam = opt("--cam")
    yaw, pitch, fov = float(opt("--yaw", "180")), float(opt("--pitch", "6")), float(opt("--fov", "95"))
    size = [int(v) for v in opt("--size", "560x350").split("x")]
    if len(args) < 2:
        print(__doc__)
        return 2
    render_all(args[0], args[1], [float(v) for v in cam.split(",")] if cam else None, yaw, pitch, fov, size)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
