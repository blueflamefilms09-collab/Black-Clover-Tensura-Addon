"""The assets of the Boss Cores (docs/boss_cores.md): 7 core icons, 28 armour icons (seven sets of four), the two worn layer textures of each set (64 x 32),
the item models, the 28 recipes (shapeless, made of cores) and the lang keys. Deterministic (no clock, fixed seeds); run it twice and nothing changes.

    python3 -B tools/gen_boss_core_assets.py

Writes under src/main/resources: assets/anime_multiverse/textures/item/<id>.png, textures/models/armor/bosscore_<set>_layer_1.png and _layer_2.png,
models/item/<id>.json, data/anime_multiverse/recipe/boss_core_<piece>.json, and merges tools/lang/boss_cores.json into lang/en_us.json (keys already there are left alone).
The set names and ids must match labyrinth/mega/cores/BossCoreRules.Kit (the Java test BossCoreAssetsTest reads the files this writes).
"""
import json
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "anime_multiverse")
DATA = os.path.join(RES, "data", "anime_multiverse")

# id, English name, base colour, accent colour, one-line effect (the tooltip), core lore
KITS = [
    ("aegis", "Aegis", "#3f6fb5", "#ffd24a", "Health and ward: more maximum health, and less damage from the damage types of other mods.",
     "A bastion crystal: it turns the blows of foreign powers aside."),
    ("striker", "Striker", "#b8322e", "#ff9a3c", "Weapon damage: more attack damage for any weapon; two pieces raise your hits, four deal a second damage type.",
     "A war crystal: whatever you swing hits harder."),
    ("wellspring_magicule", "Magicule Wellspring", "#6a4fc4", "#8fe0ff", "Magicule regeneration every second while worn.",
     "A spring of magicules, frozen into a gem."),
    ("wellspring_aura", "Aura Wellspring", "#d9a21b", "#fff2b0", "Aura regeneration every second while worn.",
     "A spring of aura, frozen into a gem."),
    ("wellspring_ki", "Ki Wellspring", "#1fa89a", "#9bffe9", "Ki regeneration every second while worn (Ki is the magicule of this mod; Saiyans get more).",
     "A spring of Ki, frozen into a gem."),
    ("wellspring_nihility", "Nihility Wellspring", "#2a2538", "#c46bff", "Nihility: active skill cooldowns fall faster; four pieces wipe a harmful effect every second.",
     "A hollow in the shape of a gem: it takes away what holds a caster back."),
    ("ascendant", "Ascendant", "#2f9e5a", "#f5ffcf", "Progression: kills add a share of the victim's EP to your base EP (Telesma and Malice of Tensura No Index are not wired yet).",
     "A crystal of growth: what you defeat makes you more."),
]
PIECES = [("helmet", "Helmet", 3), ("chestplate", "Chestplate", 5), ("leggings", "Leggings", 4), ("boots", "Boots", 3)]


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def mix(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def tones(base):
    """dark, shadow, mid, light, highlight"""
    return [shade(base, 0.38), shade(base, 0.66), base, mix(base, (255, 245, 225), 0.35), mix(base, (255, 255, 255), 0.75)]


# ---------------------------------------------------------------- core icons
def core_icon(base, accent):
    t = tones(rgb(base))
    acc = rgb(accent)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    cx, cy = 7.5, 8.0
    # the glow: a soft ring of the accent colour round the gem
    for y in range(16):
        for x in range(16):
            d = abs(x - cx) / 7.5 + abs(y - cy) / 7.5
            if 0.92 < d <= 1.18:
                px[x, y] = acc + (70,)
    # the gem: a diamond with four facets
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = abs(dx) / 6.0 + abs(dy) / 7.0
            if d > 1.0:
                continue
            if d > 0.86:
                c = t[0]                                   # outline
            elif dy < -2 and dx < 0:
                c = t[3]                                   # upper left facet: light
            elif dy < -2:
                c = t[2]                                   # upper right facet
            elif dx < 0:
                c = t[2]                                   # lower left facet
            else:
                c = t[1]                                   # lower right facet: shadow
            if d < 0.2:
                c = acc                                    # the glowing heart
            px[x, y] = c + (255,)
    px[5, 5] = t[4] + (255,)
    px[6, 4] = t[4] + (255,)
    return im


# ---------------------------------------------------------------- armour icons (masks of 16 x 16)
MASKS = {
    "helmet": [
        "................", "................", "....########....", "...##########...", "..############..", "..############..", "..###......###..", "..###......###..",
        "..##........##..", "..##........##..", "................", "................", "................", "................", "................", "................"],
    "chestplate": [
        "................", "..###......###..", ".####......####.", ".#####....#####.", ".##############.", "..############..", "..############..", "..############..",
        "..############..", "..############..", "..############..", "...##########...", "...##########...", "................", "................", "................"],
    "leggings": [
        "................", "..############..", "..############..", "..############..", "..####....####..", "..####....####..", "..###......###..", "..###......###..",
        "..###......###..", "..###......###..", "..###......###..", "..###......###..", "..###......###..", "................", "................", "................"],
    "boots": [
        "................", "................", "................", "................", "................", "...###....###...", "...###....###...", "...###....###...",
        "...###....###...", "..####....####..", ".#####....#####.", ".#####....#####.", "................", "................", "................", "................"],
}
GEM = {"helmet": (7, 3), "chestplate": (7, 6), "leggings": (7, 2), "boots": (4, 9)}


def armour_icon(piece, base, accent):
    t = tones(rgb(base))
    acc = rgb(accent)
    mask = MASKS[piece]
    on = set((x, y) for y, row in enumerate(mask) for x, ch in enumerate(row) if ch == "#")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for (x, y) in on:
        up, left = (x, y - 1) in on, (x - 1, y) in on
        down, right = (x, y + 1) in on, (x + 1, y) in on
        if not (up and left):
            c = t[3]
        elif not (down and right):
            c = t[1]
        else:
            c = t[2]
        px[x, y] = c + (255,)
    for (x, y) in on:                                       # a one pixel dark outline round the whole piece
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in on and px[nx, ny][3] == 0:
                px[nx, ny] = t[0] + (255,)
    gx, gy = GEM[piece]                                     # the set's core, set in the piece
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        px[gx + dx, gy + dy] = acc + (255,)
    px[gx, gy] = t[4] + (255,)
    for x in range(16):                                     # an accent trim along the lowest edge row of the mask
        ys = [y for (xx, y) in on if xx == x]
        if ys and x % 2 == 0:
            px[x, max(ys)] = mix(acc, t[1], 0.35) + (255,)
    return im


# ---------------------------------------------------------------- worn layers (64 x 32)
def layer(kit_index, kit_id, base, accent, number):
    b, a = rgb(base), rgb(accent)
    rng = random.Random(1000 + kit_index * 7 + number)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 255))
    px = im.load()
    for y in range(32):
        for x in range(64):
            c = b
            if kit_id == "aegis":                            # riveted plates
                if x % 8 == 0 or y % 8 == 0:
                    c = shade(b, 0.55)
                elif x % 8 == 4 and y % 8 == 4:
                    c = mix(b, a, 0.7)
            elif kit_id == "striker":                        # diagonal blade scoring
                if (x + y) % 8 == 0:
                    c = shade(b, 0.55)
                elif (x - y) % 16 == 0:
                    c = mix(b, a, 0.55)
            elif kit_id == "ascendant":                      # rising chevrons
                if (y + abs(x % 8 - 4)) % 8 == 0:
                    c = mix(b, a, 0.55)
                elif y % 8 == 7:
                    c = shade(b, 0.6)
            else:                                            # the wellsprings: wave lines
                if (y + (3 if (x // 4) % 2 else 0)) % 8 == 0:
                    c = mix(b, a, 0.5)
                elif y % 8 == 4:
                    c = shade(b, 0.7)
            if number == 1 and y % 16 == 15:
                c = mix(c, a, 0.4)
            n = rng.randint(-5, 5)
            px[x, y] = tuple(max(0, min(255, v + n)) for v in c) + (255,)
    return im


# ---------------------------------------------------------------- writers
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def model(item_id):
    return json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/" + item_id}}, indent=2) + "\n"


def recipe(kit_id, piece, count):
    core = "anime_multiverse:core_" + kit_id
    return json.dumps({"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": [{"item": core} for _ in range(count)],
                       "result": {"id": "anime_multiverse:%s_%s" % (kit_id, piece), "count": 1}}, indent=2) + "\n"


def lang():
    out = {}
    for kit_id, name, _b, _a, effect, lore in KITS:
        out["item.anime_multiverse.core_%s" % kit_id] = "%s Core" % name
        out["item.anime_multiverse.core_%s.lore" % kit_id] = lore
        out["tooltip.anime_multiverse.boss_set.%s" % kit_id] = effect
        for piece, pname, _n in PIECES:
            out["item.anime_multiverse.%s_%s" % (kit_id, piece)] = "%s %s" % (name, pname)
    out["tooltip.anime_multiverse.boss_core.drop"] = "Paid once per boss hall by the Decennial bosses of floor 250 and deeper."
    out["tooltip.anime_multiverse.boss_set.bonus"] = "Two and four pieces of one set add a bonus; sets worn together share a cap."
    out["message.anime_multiverse.boss_core.drop"] = "%1$s leaves %2$s x%3$s."
    return out


def merge_lang(entries):
    """Appends the keys that en_us.json does not have yet before its closing brace (one key per line, like the rest of the file)."""
    path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(path, encoding="utf-8") as f:
        text = f.read()
    missing = [(k, v) for k, v in entries.items() if '"%s":' % k not in text]
    if not missing:
        return 0
    body = text.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    add = ",\n".join("  %s: %s" % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in missing)
    write(path, body + ",\n" + add + "\n}\n")
    return len(missing)


def main():
    icons = 0
    for i, (kit_id, name, base, accent, _e, _l) in enumerate(KITS):
        save(core_icon(base, accent), os.path.join(ASSETS, "textures", "item", "core_%s.png" % kit_id))
        write(os.path.join(ASSETS, "models", "item", "core_%s.json" % kit_id), model("core_" + kit_id))
        icons += 1
        for piece, _pn, count in PIECES:
            item_id = "%s_%s" % (kit_id, piece)
            save(armour_icon(piece, base, accent), os.path.join(ASSETS, "textures", "item", item_id + ".png"))
            write(os.path.join(ASSETS, "models", "item", item_id + ".json"), model(item_id))
            write(os.path.join(DATA, "recipe", "boss_core_%s.json" % item_id), recipe(kit_id, piece, count))
            icons += 1
        for number in (1, 2):
            save(layer(i, kit_id, base, accent, number), os.path.join(ASSETS, "textures", "models", "armor", "bosscore_%s_layer_%d.png" % (kit_id, number)))
    entries = lang()
    write(os.path.join(HERE, "lang", "boss_cores.json"), json.dumps(entries, indent=2, ensure_ascii=False) + "\n")
    added = merge_lang(entries)
    print("%d icons, %d layer textures, %d recipes, %d lang keys added" % (icons, len(KITS) * 2, len(KITS) * len(PIECES), added))


if __name__ == "__main__":
    main()
