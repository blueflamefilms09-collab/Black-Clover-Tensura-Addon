#!/usr/bin/env python3
"""
Veldanava, the Creator God: the 16x16 icons of his seven relics, their item models and the lang lines (docs/veldanava.md).

    python3 -B tools/gen_veldanava_items.py            # writes the icons, the models, tools/lang/veldanava.json and merges it into en_us.json (idempotent)
    python3 -B tools/gen_veldanava_items.py --sheet    # also writes $ZOG_SCRATCH/previews_veldanava/items.png (the seven icons, 12x, for a look)

Writes assets/nusmp/textures/item/<id>.png and assets/nusmp/models/item/<id>.json (parent item/generated) for:
  star_king_dragon_core, genesis_fragment, creators_essence, justice_king_michael_fragment, covenant_king_uriel_fragment, nihility_fragment, veldanavas_divine_core
The spawn egg (veldanava_spawn_egg) needs only a model that inherits the vanilla egg template; it is tinted by the item itself.
"""
import json
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
LANG_FRAGMENT = os.path.join(HERE, "lang", "veldanava.json")
EN_US = os.path.join(ASSETS, "lang", "en_us.json")

OUTLINE = (14, 22, 34)
TEAL, TEAL_D, TEAL_L = (62, 224, 208), (10, 106, 120), (160, 246, 236)
GOLD, GOLD_D, GOLD_L = (242, 210, 122), (170, 128, 52), (255, 240, 190)
WHITE = (255, 255, 255)
VIOLET, VIOLET_D, VIOLET_L = (150, 100, 235), (70, 40, 130), (200, 170, 255)
VOID, VOID_L = (18, 12, 28), (52, 36, 84)


def mix(a, b, k):
    return tuple(int(round(a[i] * (1 - k) + b[i] * k)) for i in range(3))


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(x, y)] = c

    def fill(self, test, colour):
        """Every pixel whose centre passes test(x, y) gets colour(x, y) (a colour or a function)."""
        for y in range(16):
            for x in range(16):
                if test(x + 0.5, y + 0.5):
                    self.px[(x, y)] = colour(x + 0.5, y + 0.5) if callable(colour) else colour

    def outline(self, rgb=OUTLINE):
        cells = set(self.px)
        for (x, y) in cells:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                q = (x + dx, y + dy)
                if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    self.px[q] = rgb

    def save(self, name):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), (c[0], c[1], c[2], 255))
        os.makedirs(ICONS, exist_ok=True)
        im.save(os.path.join(ICONS, name + ".png"))
        return im


def inside(poly, x, y):
    n, c = len(poly), False
    j = n - 1
    for i in range(n):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            c = not c
        j = i
    return c


# ---------------------------------------------------------------- Star King Dragon Core: a teal dragon eye in a gold ring
def dragon_core():
    c = Canvas()
    cx = cy = 8.0

    def r(x, y):
        return math.hypot(x - cx, y - cy)

    c.fill(lambda x, y: r(x, y) <= 7.0, lambda x, y: GOLD_D if r(x, y) > 6.2 else GOLD if r(x, y) > 5.4 else mix(TEAL_D, TEAL, max(0.0, 1 - r(x, y) / 5.4)))
    c.fill(lambda x, y: r(x, y) <= 5.0, lambda x, y: mix(TEAL, TEAL_L, max(0.0, 1 - r(x, y) / 5.0) ** 1.4))
    c.fill(lambda x, y: abs(x - cx) <= 0.9 and abs(y - cy) <= 3.8 and (abs(x - cx) / 0.95) ** 2 + (abs(y - cy) / 3.9) ** 2 <= 1.0, (6, 30, 40))     # the slit pupil
    for (x, y) in ((5, 5), (6, 4)):
        c.put(x, y, WHITE)
    for (x, y) in ((8, 0), (8, 15), (0, 8), (15, 8)):                                                                                      # gold glints
        c.put(x, y, GOLD_L)
    c.outline()
    return c


# ---------------------------------------------------------------- Genesis Fragment: a faceted shard, white-gold with teal edges
def genesis_fragment():
    c = Canvas()
    shard = [(8, 0.5), (12.2, 5.5), (10.5, 15.0), (6.0, 15.0), (3.6, 6.2)]
    c.fill(lambda x, y: inside(shard, x, y), lambda x, y: mix(GOLD_L, TEAL, min(1.0, max(0.0, (x - 3.6) / 8.6))) if y < 7 else mix(GOLD, TEAL_D, min(1.0, max(0.0, (x - 3.6) / 8.6))))
    c.fill(lambda x, y: inside([(8, 0.5), (8.8, 5.5), (7.2, 14.5), (6.0, 14.5), (5.2, 6.0)], x, y), lambda x, y: mix(WHITE, GOLD_L, y / 15.0))      # the lit facet
    for (x, y) in ((7, 2), (8, 4), (7, 7), (7, 10)):
        c.put(x, y, WHITE)
    for (x, y) in ((11, 6), (10, 10), (9, 13)):
        c.put(x, y, TEAL_D)
    c.put(13, 3, GOLD_L)
    c.put(2, 9, TEAL_L)
    c.outline()
    return c


# ---------------------------------------------------------------- Creator's Essence: a vial of liquid light
def creators_essence():
    c = Canvas()
    glass = (200, 232, 240)
    c.fill(lambda x, y: 6 <= x <= 10 and 1.5 <= y <= 5.0, glass)                                      # the neck
    c.fill(lambda x, y: 6.4 <= x <= 9.6 and 2.2 <= y <= 5.0, (60, 120, 130))
    c.fill(lambda x, y: math.hypot(x - 8, y - 10) <= 5.2, glass)                                      # the bulb
    c.fill(lambda x, y: math.hypot(x - 8, y - 10) <= 4.4 and y >= 7.5, lambda x, y: mix(WHITE, GOLD, min(1.0, (y - 7.5) / 6.0)))      # the light inside
    c.fill(lambda x, y: math.hypot(x - 8, y - 10) <= 4.4 and y < 7.5, (70, 140, 150))
    c.fill(lambda x, y: math.hypot(x - 8, y - 11) <= 2.0, lambda x, y: mix(WHITE, TEAL_L, math.hypot(x - 8, y - 11) / 2.0))
    c.fill(lambda x, y: 6.5 <= x <= 9.5 and 0 <= y <= 1.9, (150, 100, 60))                           # the cork
    for (x, y) in ((5, 8), (5, 9)):
        c.put(x, y, WHITE)
    for (x, y) in ((12, 5), (3, 12), (13, 11)):                                                       # motes of light outside
        c.put(x, y, GOLD_L)
    c.outline()
    return c


# ---------------------------------------------------------------- Justice King Michael Fragment: a golden shield shard with a wing stroke
def michael_fragment():
    c = Canvas()
    shield = [(2.5, 1.5), (13.5, 1.5), (13.5, 8.0), (8.0, 15.0), (2.5, 8.0)]
    c.fill(lambda x, y: inside(shield, x, y), lambda x, y: mix(GOLD_L, GOLD_D, min(1.0, (x - 2.5) / 11.0 * 0.5 + (y - 1.5) / 13.5 * 0.7)))
    c.fill(lambda x, y: inside([(4, 3), (12, 3), (12, 7.5), (8, 12.5), (4, 7.5)], x, y), lambda x, y: mix(WHITE, GOLD_L, min(1.0, (y - 3) / 9.0)))
    for (x, y) in ((8, 4), (8, 5), (8, 6), (8, 7), (8, 8), (8, 9), (6, 6), (7, 5), (10, 6), (9, 5), (5, 7), (11, 7)):                  # a sword with wings
        c.put(x, y, TEAL_D if (x, y) != (8, 4) else TEAL)
    c.put(8, 10, TEAL)
    c.outline()
    return c


# ---------------------------------------------------------------- Covenant King Uriel Fragment: a violet shard threaded by a chain ring
def uriel_fragment():
    c = Canvas()
    shard = [(9.0, 0.5), (13.5, 7.0), (10.0, 15.0), (4.0, 12.0), (2.8, 5.0)]
    c.fill(lambda x, y: inside(shard, x, y), lambda x, y: mix(VIOLET_L, VIOLET_D, min(1.0, (x - 2.8) / 10.7 * 0.6 + (y - 0.5) / 14.5 * 0.5)))
    c.fill(lambda x, y: inside([(9, 0.5), (9.8, 6), (7.5, 13), (4.6, 11.0), (4.2, 5.0)], x, y), lambda x, y: mix(WHITE, VIOLET_L, y / 15.0))
    ring = lambda x, y: 2.6 <= math.hypot(x - 8, y - 8) <= 3.7                                       # a gold ring (a link of the Covenant's chain)
    c.fill(ring, lambda x, y: GOLD_L if x + y < 16 else GOLD_D)
    c.put(8, 4, GOLD_L)
    c.put(12, 2, VIOLET_L)
    c.put(3, 13, VIOLET_L)
    c.outline()
    return c


# ---------------------------------------------------------------- Nihility Fragment: a black shard with a violet rim, specks of stars in it
def nihility_fragment():
    c = Canvas()
    shard = [(7.0, 0.5), (12.5, 4.0), (13.5, 10.0), (9.0, 15.0), (3.0, 11.0), (2.5, 5.0)]
    c.fill(lambda x, y: inside(shard, x, y), lambda x, y: mix(VOID, VOID_L, min(1.0, math.hypot(x - 8, y - 8) / 6.5)))
    c.fill(lambda x, y: inside(shard, x, y) and not inside(shard_inner(), x, y), lambda x, y: mix(VIOLET_D, VIOLET, min(1.0, (x + y) / 26.0)))
    for (x, y) in ((6, 5), (9, 7), (7, 10), (10, 11), (5, 8)):
        c.put(x, y, VIOLET_L if (x + y) % 2 else WHITE)
    c.put(8, 8, (0, 0, 0))
    c.outline((8, 6, 18))
    return c


def shard_inner():
    return [(7.0, 2.2), (11.0, 4.8), (11.8, 9.6), (8.8, 13.2), (4.4, 10.2), (4.0, 5.6)]


# ---------------------------------------------------------------- Veldanava's Divine Core: the brightest of them, a white sun with gold ring and rays
def divine_core():
    c = Canvas()
    cx = cy = 8.0
    for k in range(8):                                                                               # eight rays of teal light
        a = k * math.pi / 4
        for t in (6.2, 7.0):
            c.put(int(cx + math.cos(a) * t), int(cy + math.sin(a) * t), TEAL if k % 2 == 0 else TEAL_L)
    def r(x, y):
        return math.hypot(x - cx, y - cy)
    c.fill(lambda x, y: r(x, y) <= 5.6, lambda x, y: GOLD_D if r(x, y) > 4.8 else GOLD if r(x, y) > 4.0 else mix(GOLD_L, WHITE, max(0.0, 1 - r(x, y) / 4.0)))
    c.fill(lambda x, y: r(x, y) <= 2.6, lambda x, y: mix(WHITE, TEAL_L, r(x, y) / 2.6 * 0.5))
    for (x, y) in ((7, 7), (8, 7), (7, 8)):
        c.put(x, y, WHITE)
    c.outline((20, 30, 44))
    return c


ITEMS = [
    ("star_king_dragon_core", dragon_core, "Star King Dragon Core",
     "The eye of the Star King Dragon, cut from its heart. A crafting core: it can be socketed into a Grimoire to turn its magic into raw Stardust.",
     "Dropped by Veldanava (1 per fighter). Combat compatibility: socketed into a Grimoire it would convert all elemental magic into raw, unblockable Stardust magic, and it is the main ingredient of the Aura of the Creator (those two uses are crafted by other systems; this item is the material)."),
    ("genesis_fragment", genesis_fragment, "Genesis Fragment",
     "A shard of the moment of creation. Used at an anvil or forge to push a weapon past the enchanting limits.",
     "Dropped by Veldanava (3 per fighter). Combat compatibility: used in an anvil or a forge to upgrade weapons beyond the usual enchantment limits, granting Unbreakable and a faster base attack speed (the upgrade recipe is crafted by other systems; this item is the material)."),
    ("creators_essence", creators_essence, "Creator's Essence",
     "Use it to refill your health, food and magicules and to clear every cooldown. The panic button of a raid.",
     "Dropped by Veldanava (1 per fighter). Right-click: full health and food, every bad effect removed, Regeneration and Absorption for a while, the magicules full and every item and hero-weapon cooldown cleared. It is used up."),
    ("justice_king_michael_fragment", michael_fragment, "Justice King Michael Fragment",
     "Consume it to be guarded by an absolute barrier that turns away one fatal blow a day.",
     "Rare drop of Veldanava. Right-click to consume it once: from then on, the first blow each day that would kill you is turned away (the barrier is checked before totems and other saves). Kept in your player data, through death."),
    ("covenant_king_uriel_fragment", uriel_fragment, "Covenant King Uriel Fragment",
     "Held in the hand it lets you step through space: a dash of up to fourteen blocks with no fall damage.",
     "Rare drop of Veldanava. Right-click to step up to 14 blocks along your look direction to the furthest spot your body fits, with no fall damage (2 second cooldown). It is not used up: a vastly better Ender Pearl that never breaks."),
    ("nihility_fragment", nihility_fragment, "Nihility Fragment",
     "Held in the off hand it coats your blows in void: what you strike cannot heal for five seconds.",
     "Rare drop of Veldanava. Keep it in the off hand. Anything you strike with a weapon in the main hand cannot regain health for 5 seconds (the healing-debuff the guide calls Anti-Magic), which answers bosses that regenerate. It is not used up."),
    ("veldanavas_divine_core", divine_core, "Veldanava's Divine Core",
     "A limitless source of power. In the off hand it negates the cost of ultimate spells.",
     "Ultra-rare drop of Veldanava (about 1 in 100 per fighter). Combat compatibility: held in the off hand it is meant to negate the mana or stamina cost of any ultimate spell of any anime system in the mod (the hook is in the Ultimate Magic system's cost step; the item itself is the key)."),
]

LANG = {
    "entity.nusmp.veldanava": "Veldanava",
    "entity.nusmp.will_of_the_creator": "Will of the Creator",
    "item.nusmp.veldanava_spawn_egg": "Veldanava Spawn Egg",
    "nusmp.veldanava.name.dragon": "Star King Dragon",
    "nusmp.veldanava.name.humanoid": "Creator God",
    "nusmp.veldanava.name.avatar": "Dark Avatar of God",
    "subtitles.entity.nusmp.veldanava": "Veldanava acts",
}


def write_model(name, spec):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
        json.dump(spec, f, indent=2)
        f.write("\n")


def main():
    sheet = Image.new("RGBA", (16 * len(ITEMS) * 12 + 8 * (len(ITEMS) + 1), 16 * 12 + 16), (40, 44, 56, 255))
    for i, (iid, draw, name, short, card) in enumerate(ITEMS):
        im = draw().save(iid)
        write_model(iid, {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + iid}})
        LANG["item.nusmp." + iid] = name
        LANG["item.nusmp." + iid + ".desc"] = short
        LANG["item.nusmp." + iid + ".card"] = card
        sheet.paste(im.resize((192, 192), Image.NEAREST), (8 + i * (192 + 8), 8), im.resize((192, 192), Image.NEAREST))
    write_model("veldanava_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})
    with open(LANG_FRAGMENT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(LANG, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(EN_US, encoding="utf-8") as f:
        en = json.load(f)
    added = changed = 0
    for k, v in LANG.items():
        if k not in en:
            added += 1
        elif en[k] != v:
            changed += 1
        en[k] = v
    with open(EN_US, "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("icons + models for %d items, lang: %d added, %d updated" % (len(ITEMS), added, changed))
    if "--sheet" in sys.argv:
        out = os.path.join(os.environ.get("ZOG_SCRATCH", "/tmp"), "previews_veldanava")
        os.makedirs(out, exist_ok=True)
        sheet.save(os.path.join(out, "items.png"))
        print("sheet:", os.path.join(out, "items.png"))


if __name__ == "__main__":
    main()
