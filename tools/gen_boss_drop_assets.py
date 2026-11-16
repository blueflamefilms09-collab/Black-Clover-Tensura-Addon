"""The assets and data of the boss drop framework (docs/bosses/boss_drops.md). Deterministic (no clock, fixed seeds); run it twice and nothing changes.

    python3 -B tools/gen_boss_drop_assets.py

Reads tools/boss_drops/bosses.json (the 23 bosses: id, trophy name, colour, accent) and the neutral relic that tools/blender/boss_trophy.py made in Blender
(tools/boss_drops/trophy_relic.txt / .png / _glow.png), and writes under src/main/resources:
  * assets/anime_multiverse/weapon_meshes/trophy_<id>.txt           the Blender relic mesh, its glow colour set to the boss colour
  * assets/anime_multiverse/textures/entity/weapon/trophy_<id>.png and _glow.png   the neutral skin tinted per boss (crystal = colour, metal = accent tinted)
  * assets/anime_multiverse/textures/item/trophy_<id>.png            16 x 16 inventory icons (the 2D sprite stays the inventory icon, like the other solid items)
  * assets/anime_multiverse/models/item/trophy_<id>.json and trophy_<id>_icon.json   the builtin/entity hand model (a copy of the Beast Will Core's) and the sprite model
  * the "handitem" entry of every trophy in both weapon_classes.json copies
  * the four Lucifero armour pieces and the four Trophy Plate pieces: icons, item models, the worn layers (64 x 32, two per set)
  * data/anime_multiverse/recipe/trophy_plate_<piece>.json (shapeless, made of boss trophies), data/anime_multiverse/tags/item/boss_trophies.json
  * data/anime_multiverse/boss_drops/<boss>.json                      the drop tables, see BossDropTable
  * tools/lang/boss_lucifero_drops.json                               the lang keys (merged into en_us.json at release time, never edited here)
Then run:  python3 -B tools/weapon_grip/grip_table.py --write   (the grip golden file must know the new meshes).
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_boss_core_assets as core  # noqa: E402  (armour_icon, tones, rgb, shade, mix: one source for the icon style)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "anime_multiverse")
DATA = os.path.join(RES, "data", "anime_multiverse")
SRC = os.path.join(HERE, "boss_drops")

# ---- the equipment families (ids must match bossdrops/BossDropCatalog.java)
LUCIFERO = ("lucifero", "Lucifero's", "#2a1a3f", "#e0b04a")
PLATE = ("trophy_plate", "Trophy Plate", "#6b6f7a", "#d9a84a")
PIECES = [("helmet", "Helmet"), ("chestplate", "Chestplate"), ("leggings", "Leggings"), ("boots", "Boots")]
PLATE_COST = {"helmet": 5, "chestplate": 8, "leggings": 7, "boots": 4}

# ---- the drop tiers: silver coin range, gold coin on a player's first kill, bonus gold chance
TIER = {
    "lesser": dict(silver=(3, 8), gold_first=1, bonus=0.0),
    "mid": dict(silver=(6, 14), gold_first=1, bonus=0.10),
    "high": dict(silver=(10, 24), gold_first=2, bonus=0.25),
}
TIER_OF = {}
for _b in ("minotaur", "black_goliath", "amphisbaena", "udaeus", "vetto", "mars", "revchi_salik", "lotus_whomalt", "heath_grice", "morris_libardirt"):
    TIER_OF[_b] = "lesser"
for _b in ("ban", "diane", "king", "gowther", "rades_spirito", "patolli", "fana", "rhya"):
    TIER_OF[_b] = "mid"
for _b in ("meliodas", "escanor", "merlin", "vanica"):
    TIER_OF[_b] = "high"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# ---------------------------------------------------------------- the relic, tinted
def tint_skin(src, colour, accent):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    c, a = hexrgb(colour), hexrgb(accent)
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, al = px[x, y]
            if al == 0:
                continue
            lum = (r + g + b) / 3.0
            if lum >= 170:                                   # the crystal: the boss colour, a touch of the accent
                out = core.mix(core.shade(c, 0.78 + 0.35 * (lum - 170) / 85.0), a, 0.15)
            else:                                            # the metal: grey with the accent worked in
                k = max(0.25, lum / 110.0)
                out = core.mix((int(lum), int(lum), int(lum)), core.shade(a, 0.7 * k), 0.5 if lum > 35 else 0.15)
            px[x, y] = tuple(max(0, min(255, int(v))) for v in out) + (al,)
    return im


def tint_glow(src, colour, accent):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    c, a = hexrgb(colour), hexrgb(accent)
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, al = px[x, y]
            if al == 0:
                continue
            lum = (r + g + b) / 3.0
            out = core.shade(core.mix(c, a, 0.35), min(1.4, lum / 128.0))
            px[x, y] = tuple(max(0, min(255, int(v))) for v in out) + (al,)
    return im


def mesh_for(neutral_text, colour):
    out = []
    for line in neutral_text.splitlines():
        if line.startswith("glow "):
            parts = line.split()
            parts[1] = colour.lstrip("#").upper()
            line = " ".join(parts)
        out.append(line)
    return "\n".join(out) + "\n"


# ---------------------------------------------------------------- the 16 x 16 trophy icon: a gem in three claws on a ringed base
BASE = ["................", "................", "................", "................", "................", "................", "................", "................",
        "................", "................", "................", "................", "...##########...", "..############..", "..############..", "................"]
CLAWS = [(3, y) for y in range(5, 12)] + [(12, y) for y in range(5, 12)] + [(7, 12), (8, 12)]


def trophy_icon(colour, accent):
    t = core.tones(hexrgb(colour))
    acc = hexrgb(accent)
    metal = core.tones(core.mix((120, 120, 128), acc, 0.45))
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(BASE):                          # the base
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = (metal[3] if y == 12 else metal[1] if y == 14 else metal[2]) + (255,)
    for (x, y) in CLAWS[:14]:                               # the two side claws
        px[x, y] = metal[2] + (255,)
    cx, cy = 7.5, 6.0
    for y in range(16):                                     # the gem: a tall diamond
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = abs(dx) / 3.6 + abs(dy) / 5.6
            if d > 1.0:
                continue
            if d > 0.84:
                c = t[0]
            elif dx < 0 and dy < 1:
                c = t[3]
            elif dx < 0:
                c = t[2]
            elif dy < 1:
                c = t[2]
            else:
                c = t[1]
            if d < 0.22:
                c = acc
            px[x, y] = c + (255,)
    px[6, 3] = t[4] + (255,)
    px[6, 4] = t[4] + (255,)
    for x in (4, 11):                                       # the claw tips close over the gem's shoulder
        px[x, 4] = metal[3] + (255,)
    return im


# ---------------------------------------------------------------- worn layers (64 x 32), one pattern per set
def layer(kind, base, accent, number):
    import random
    b, a = hexrgb(base), hexrgb(accent)
    rng = random.Random(7000 + (1 if kind == "lucifero" else 2) * 10 + number)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 255))
    px = im.load()
    for y in range(32):
        for x in range(64):
            c = b
            if kind == "lucifero":                           # violet-black plate with gold gravity ripples
                ring = (abs(x - 32) + abs(y - 16) * 2) % 12
                if ring == 0:
                    c = core.mix(b, a, 0.7)
                elif ring == 1:
                    c = core.shade(b, 0.6)
                elif (x + y) % 16 == 0:
                    c = core.mix(b, (110, 70, 170), 0.5)
            else:                                            # trophy plate: riveted steel with a gold seam
                if x % 8 == 0 or y % 8 == 0:
                    c = core.shade(b, 0.55)
                elif x % 8 == 4 and y % 8 == 4:
                    c = core.mix(b, a, 0.8)
                elif y % 16 == 8:
                    c = core.mix(b, a, 0.35)
            if number == 1 and y % 16 == 15:
                c = core.mix(c, a, 0.4)
            n = rng.randint(-5, 5)
            px[x, y] = tuple(max(0, min(255, v + n)) for v in c) + (255,)
    return im


def item_model(item_id):
    return json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/" + item_id}}, indent=2) + "\n"


def hand_model(item_id, template):
    m = json.loads(template)
    m["textures"]["particle"] = "anime_multiverse:item/" + item_id
    return json.dumps(m, indent=2) + "\n"


# ---------------------------------------------------------------- the drop tables
def drop_table(b):
    bid = b["id"]
    row = lambda item, chance, lo, hi, first=False: {"item": item, "chance": chance, "min": lo, "max": hi, "first_kill_only": first, "killer_only": True}
    trophy = "anime_multiverse:trophy_" + bid
    if bid == "lucifero":
        rows = [row(trophy, 1.0, 1, 2), row("tensura:silver_coin", 1.0, 20, 40), row("tensura:gold_coin", 1.0, 2, 3, True)]
        rows += [row("anime_multiverse:lucifero_" + p, 0.25, 1, 1) for p, _n in PIECES]          # about one piece a kill (four rows of a quarter)
        rows += [row("anime_multiverse:lucifero_" + p, 0.04, 1, 1) for p, _n in PIECES]          # and a small chance of a second copy
        return {"boss": bid, "max_stacks": 10, "drops": rows}
    t = TIER[TIER_OF[bid]]
    rows = [row(trophy, 1.0, 1, 2), row("tensura:silver_coin", 1.0, *t["silver"]), row("tensura:gold_coin", 1.0, t["gold_first"], t["gold_first"], True)]
    if t["bonus"] > 0:
        rows.append(row("tensura:gold_coin", t["bonus"], 1, 1))
    return {"boss": bid, "max_stacks": 6, "drops": rows}


# ---------------------------------------------------------------- lang
def lang(bosses):
    out = {}
    for b in bosses:
        out["item.anime_multiverse.trophy_" + b["id"]] = b["trophy"]
    for kind, name, _b, _a in (LUCIFERO, PLATE):
        for piece, pname in PIECES:
            out["item.anime_multiverse.%s_%s" % (kind, piece)] = "%s %s" % (name, pname)
    out["tooltip.anime_multiverse.trophy"] = "A trophy of a boss kill: material for the Trophy Plate armour."
    out["tooltip.anime_multiverse.boss_gear.lucifero"] = "Armour of the Spade devil Lucifero: it drops from him."
    out["tooltip.anime_multiverse.boss_gear.trophy_plate"] = "Plate beaten from boss trophies."
    return out


def add_classes(path, ids):
    """Adds a "handitem" line for every id right after the Beast Will Core's line (one key per line, like the rest of the file)."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    missing = [i for i in ids if '"%s":' % i not in text]
    if not missing:
        return 0
    anchor = '    "beast_will_core": "handitem",\n'
    assert anchor in text, path
    add = "".join('    "%s": "handitem",\n' % i for i in missing)
    write(path, text.replace(anchor, anchor + add, 1))
    return len(missing)


def main():
    with open(os.path.join(SRC, "bosses.json"), encoding="utf-8") as f:
        bosses = json.load(f)["bosses"]
    with open(os.path.join(SRC, "trophy_relic.txt"), encoding="utf-8") as f:
        neutral = f.read()
    with open(os.path.join(ASSETS, "models", "item", "beast_will_core.json"), encoding="utf-8") as f:
        template = f.read()
    ids = []
    for b in bosses:
        tid = "trophy_" + b["id"]
        ids.append(tid)
        write(os.path.join(ASSETS, "weapon_meshes", tid + ".txt"), mesh_for(neutral, b["color"]))
        save(tint_skin(os.path.join(SRC, "trophy_relic.png"), b["color"], b["accent"]), os.path.join(ASSETS, "textures", "entity", "weapon", tid + ".png"))
        save(tint_glow(os.path.join(SRC, "trophy_relic_glow.png"), b["color"], b["accent"]), os.path.join(ASSETS, "textures", "entity", "weapon", tid + "_glow.png"))
        save(trophy_icon(b["color"], b["accent"]), os.path.join(ASSETS, "textures", "item", tid + ".png"))
        write(os.path.join(ASSETS, "models", "item", tid + ".json"), hand_model(tid, template))
        write(os.path.join(ASSETS, "models", "item", tid + "_icon.json"), item_model(tid))
        write(os.path.join(DATA, "boss_drops", b["id"] + ".json"), json.dumps(drop_table(b), indent=2) + "\n")
    for kind, _name, base, accent in (LUCIFERO, PLATE):
        for piece, _pn in PIECES:
            item_id = "%s_%s" % (kind, piece)
            save(core.armour_icon(piece, base, accent), os.path.join(ASSETS, "textures", "item", item_id + ".png"))
            write(os.path.join(ASSETS, "models", "item", item_id + ".json"), item_model(item_id))
        for number in (1, 2):
            save(layer(kind, base, accent, number), os.path.join(ASSETS, "textures", "models", "armor", "%s_layer_%d.png" % (kind, number)))
    for piece, _pn in PIECES:
        recipe = {"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": [{"tag": "anime_multiverse:boss_trophies"} for _ in range(PLATE_COST[piece])],
                  "result": {"id": "anime_multiverse:trophy_plate_" + piece, "count": 1}}
        write(os.path.join(DATA, "recipe", "trophy_plate_%s.json" % piece), json.dumps(recipe, indent=2) + "\n")
    write(os.path.join(DATA, "tags", "item", "boss_trophies.json"), json.dumps({"replace": False, "values": ["anime_multiverse:" + i for i in ids]}, indent=2) + "\n")
    added = add_classes(os.path.join(ASSETS, "weapon_classes.json"), ids) + add_classes(os.path.join(HERE, "relics", "weapon_classes.json"), ids)
    entries = lang(bosses)
    write(os.path.join(HERE, "lang", "boss_lucifero_drops.json"), json.dumps(entries, indent=2, ensure_ascii=False) + "\n")
    print("%d trophies, 8 armour pieces, %d drop tables, %d class lines added, %d lang keys written to tools/lang/boss_lucifero_drops.json" % (len(ids), len(bosses), added, len(entries)))


if __name__ == "__main__":
    main()
