"""The data and assets of the worthwhile boss drops (docs/boss_drops_worthwhile.md). Deterministic (no clock, fixed seeds); run it twice and nothing changes.

    python3 -B tools/gen_boss_signature_assets.py

This file is the ONE place that says what every boss pays. It writes, under src/main/resources:
  * data/anime_multiverse/boss_signature/<entity path>.json      one table per boss (read by bossdrops2.SignatureTable): tier, the signature item with its boon,
                                                                   the personal extras (existing items only) and, for the strongest bosses, the one-of-one relic
  * assets/anime_multiverse/textures/item/sig/<id>.png            16 x 16 inventory icon of every signature (a flat sprite, drawn by this script, NOT a Blender model)
  * assets/anime_multiverse/textures/item/boss_signature.png / boss_echo.png   the plain icons
  * assets/anime_multiverse/models/item/sig/<id>.json, boss_signature.json (custom_model_data overrides, one per boss), boss_echo.json
and under tools/: lang/bossdrops2.json (merge with tools/lang/merge_fragment.py) and boss_drops/signature_index.json (the order = the custom_model_data of the icon).
The order of SPEC is the order of bossdrops2/BossSigCatalog.java: append only (the number of a boss is the model number saved in every stack).
Names and lore are invented from each character's canon (no wiki was fetched, see the doc); the one-of-one relics are relics the mod already has (relics/wave/*).
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_boss_core_assets as core  # noqa: E402  (tones, shade, mix: one source for the icon style)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "anime_multiverse")
DATA = os.path.join(RES, "data", "anime_multiverse", "boss_signature")

TIER_SECONDS = {"lesser": 240, "mid": 360, "high": 600, "apex": 900}
ECHO_CHANCE = {"lesser": 0.0, "mid": 0.10, "high": 0.15, "apex": 0.20}
# the personal rows every tier pays besides the signature: existing items only (a themed row of the boss comes on top)
EXTRAS = {
    "lesser": [("anime_multiverse:magic_stone", 1.0, 2, 4), ("minecraft:emerald", 0.5, 1, 3)],
    "mid": [("anime_multiverse:magic_stone", 1.0, 4, 8), ("minecraft:emerald", 0.6, 2, 5), ("minecraft:experience_bottle", 0.6, 3, 6)],
    "high": [("anime_multiverse:magic_stone", 1.0, 8, 14), ("minecraft:diamond", 0.6, 1, 3), ("minecraft:experience_bottle", 0.8, 6, 12), ("minecraft:golden_apple", 0.35, 1, 1)],
    "apex": [("anime_multiverse:magic_stone", 1.0, 12, 24), ("minecraft:diamond", 0.8, 2, 5), ("minecraft:netherite_scrap", 0.5, 1, 2), ("minecraft:enchanted_golden_apple", 0.12, 1, 1)],
}
EFFECT_NAMES = {"strength": "Strength", "resistance": "Resistance", "regeneration": "Regeneration", "speed": "Speed", "haste": "Haste", "fire_resistance": "Fire Resistance",
                "night_vision": "Night Vision", "water_breathing": "Water Breathing", "absorption": "Absorption", "luck": "Luck", "jump_boost": "Jump Boost",
                "invisibility": "Invisibility", "slow_falling": "Slow Falling", "health_boost": "Health Boost", "dolphins_grace": "Dolphin's Grace", "conduit_power": "Conduit Power"}
ROMAN = ["I", "II", "III", "IV"]


def S(boss, tier, sig, lore, colour, accent, shape, boon, themed=None, relic=None):
    return dict(boss=boss, tier=tier, sig=sig, lore=lore, colour=colour, accent=accent, shape=shape, boon=boon, themed=themed, relic=relic)


# id (= the entity type path), tier, signature name, lore, colour, accent, icon shape, boon [(effect, amplifier)], themed row (item, chance, min, max), one-of-one relic id
SPEC = [
    # ---- the 22 grimoire bosses that already pay a trophy (their trophy stays: bossdrops/), by their entity path
    S("meliodas", "apex", "Hellblaze Heart", "A cinder of the Demon Sin's wrath. It is warm, and it is hungry.", "7A1432", "FF5E6E", "flame", [("strength", 1), ("fire_resistance", 0)], ("tensura:gold_coin", 0.5, 1, 2), "hellblaze_the_demon_sin"),
    S("escanor", "apex", "Noon-Born Sunmark", "Pride at its zenith: the mark burns brightest when the sun stands highest.", "E8A317", "FFE680", "orb", [("strength", 1), ("fire_resistance", 0), ("haste", 0)], ("tensura:gold_coin", 0.5, 1, 2), "noon_cleaver"),
    S("ban", "high", "Snatcher's Fox Fang", "The Fox's Sin keeps whatever it bites.", "3C9E6B", "D9E8A0", "fang", [("regeneration", 0), ("speed", 1)], ("anime_multiverse:beast_hide", 0.6, 1, 2), "mammons_vault_ring"),
    S("diane", "high", "Gideon Splinter", "A splinter of a giant's hammer. It is still heavier than it looks.", "E8832A", "FFD27A", "medal", [("resistance", 1), ("strength", 0)], ("minecraft:iron_block", 0.5, 1, 2), "hammer_of_thunderbolts"),
    S("king", "high", "Chastiefol Seedling", "A seed of the Fairy King's spirit spear. It wants to grow.", "6BBF4E", "FFD76A", "shard", [("speed", 1), ("jump_boost", 1), ("slow_falling", 0)], ("minecraft:amethyst_shard", 0.6, 3, 6), "fairy_glitter_gauntlets"),
    S("gowther", "high", "Goat's Lucid Dream", "A dream that remembers being a mind.", "E0559B", "FFC4E8", "orb", [("night_vision", 0), ("luck", 1), ("speed", 0)], ("minecraft:ender_pearl", 0.6, 1, 2), "pandoras_actor_mask"),
    S("merlin", "apex", "Sliver of Infinity", "Every spell she ever cast, folded into one splinter.", "7A5CFF", "FFD86B", "shard", [("haste", 1), ("absorption", 1), ("regeneration", 0)], ("minecraft:ender_pearl", 0.7, 2, 4), "astralis_the_eye_of_the_universe"),
    S("fana", "mid", "Hatred's Afterglow", "The Third Eye's hatred, banked low and still hot.", "E8421A", "FFB347", "flame", [("fire_resistance", 0), ("strength", 0)], ("minecraft:blaze_powder", 0.6, 3, 6)),
    S("rhya", "mid", "Dirgeroot Bloom", "A flower that grew where she sang. It hums a quiet song.", "8E7BE0", "FFD35A", "orb", [("regeneration", 0), ("resistance", 0)], ("minecraft:bone_meal", 0.7, 4, 8)),
    S("rades_spirito", "high", "Soulbound Lodestone", "A stone still tied to a soul that is no longer here.", "4A5578", "9AA6C8", "medal", [("night_vision", 0), ("speed", 0), ("resistance", 0)], ("anime_multiverse:disciple_sigil", 0.4, 1, 1)),
    S("patolli", "high", "Loaded Die of Fortune", "It always lands the way the Leader wants.", "FFE3A0", "FFFFFF", "medal", [("luck", 1), ("speed", 0)], ("minecraft:gold_nugget", 0.7, 6, 12)),
    S("heath_grice", "mid", "Knight's Rime Charm", "Frost that stayed behind after the Death Knight fell.", "7FD0FF", "EAF8FF", "shard", [("resistance", 0), ("regeneration", 0)], ("minecraft:blue_ice", 0.5, 2, 4)),
    S("lotus_whomalt", "mid", "Sideline General's Lotus Seal", "A seal that smells faintly of poison and flowers.", "7A6E94", "C9C0DC", "medal", [("regeneration", 0), ("night_vision", 0)], ("minecraft:fermented_spider_eye", 0.5, 1, 3)),
    S("mars", "mid", "Crystal Weapon Fragment", "A shard of a body that was a weapon first.", "A48CE8", "E6F4FF", "shard", [("strength", 0), ("resistance", 0)], ("minecraft:quartz", 0.6, 4, 8)),
    S("revchi_salik", "lesser", "Chain Thief's Link", "One link of a chain that was never meant to be cut.", "6E7A8C", "D4DCE8", "medal", [("speed", 1), ("haste", 0)], ("minecraft:chain", 0.6, 3, 6)),
    S("vetto", "mid", "Bound Beast's Clasp", "The clasp that held a beast back, and failed.", "D9622B", "FFC27A", "fang", [("speed", 1), ("jump_boost", 0)], ("anime_multiverse:beast_hide", 0.5, 1, 2)),
    S("minotaur", "high", "Labyrinth Bull Crest", "The crest of the bull that owned the maze.", "B4522E", "F0B070", "fang", [("strength", 0), ("resistance", 0)], ("anime_multiverse:beast_hide", 0.6, 1, 3), "minotaur_kings_labrys"),
    S("black_goliath", "high", "Wrath Core Sliver", "A sliver of the dungeon's wrath, still red at the edge.", "E0502A", "FFB060", "flame", [("strength", 1), ("fire_resistance", 0)], ("minecraft:obsidian", 0.6, 2, 4)),
    S("amphisbaena", "high", "Twin-Head Water Scale", "Two heads, one scale, and a pull like a river.", "4AB0FF", "FF6A5A", "fang", [("water_breathing", 0), ("dolphins_grace", 0), ("regeneration", 0)], ("anime_multiverse:lizardman_scale", 0.6, 1, 3)),
    S("udaeus", "high", "Palace Bone Seal", "A seal cut from the skeleton of the White Palace.", "9A8AFF", "E8E2CC", "medal", [("resistance", 0), ("strength", 0)], ("minecraft:bone_block", 0.6, 2, 4)),
    S("morris_libardirt", "high", "Scholar's Specimen Jar", "The jar is empty now. The lesson is not.", "FF9B58", "FFD9B0", "orb", [("haste", 0), ("night_vision", 0), ("luck", 0)], ("minecraft:writable_book", 0.5, 1, 1)),
    S("lucifero", "apex", "Sovereign's Gravity Shard", "A shard of a fallen king's weight. The air leans toward it.", "5B2A8C", "B58CFF", "shard", [("resistance", 1), ("strength", 1), ("slow_falling", 0)], ("minecraft:crying_obsidian", 0.6, 2, 4), "godking_crown"),
    # ---- the older Zogratis bosses, the devils and the dungeon floor bosses
    S("dante_zogratis", "apex", "Gravity Devil's Weight Stone", "It weighs what you owe.", "4B2D8A", "C9A7FF", "orb", [("resistance", 1), ("strength", 0), ("slow_falling", 0)], ("anime_multiverse:devil_horn", 0.5, 1, 2), "starsplitter_hammer"),
    S("vanica_zogratis", "apex", "Curse-Warding Blood Droplet", "A drop that turns every curse aside, and remembers each one.", "9B1230", "FF8FA3", "flame", [("regeneration", 1), ("resistance", 0)], ("minecraft:nether_wart", 0.6, 4, 8), "staff_of_blood_magic"),
    S("zenon_zogratis", "apex", "Spatial Devil's Door-Key", "A key to a door that is only there while you hold it.", "1C7C8C", "8CF2FF", "medal", [("speed", 1), ("jump_boost", 1), ("luck", 0)], ("minecraft:ender_pearl", 0.7, 2, 4), "gate_of_babylon_crest"),
    S("lucius_zogratis", "apex", "Time Devil's Stopped Second", "One second, kept. Nothing in it is moving.", "D9B43A", "F2E6B0", "orb", [("haste", 1), ("speed", 1), ("regeneration", 0)], ("minecraft:clock", 0.5, 1, 2), "time_hammer"),
    S("devil_avatar", "apex", "Walking Devil's Hellfire Coal", "A coal from a devil that walked. It does not go out.", "2A2024", "FF6A1A", "flame", [("fire_resistance", 0), ("strength", 1), ("resistance", 0)], ("anime_multiverse:devil_horn", 0.6, 1, 3), "incinerator_arm"),
    S("goliath", "high", "Pit Giant's Knuckle Stone", "A knuckle of the giant of the pit. It still feels like a fist.", "6B5A3C", "E3C98A", "orb", [("resistance", 1), ("strength", 0)], ("anime_multiverse:juggernaut_heart", 0.6, 1, 1)),
    S("charybdis_spawn", "high", "Whirlpool's Tidal Pearl", "A pearl that remembers being the middle of a whirlpool.", "1D4F9C", "9AD8FF", "orb", [("water_breathing", 0), ("dolphins_grace", 1), ("conduit_power", 0)], ("anime_multiverse:water_spirit_essence", 0.6, 1, 3)),
    S("zagred", "apex", "Word-Soul Devil's Last Word", "A word nobody finished saying. Everything near it listens.", "8A4CFF", "F2194D", "shard", [("absorption", 1), ("luck", 0), ("regeneration", 0)], ("anime_multiverse:heart_of_words", 0.5, 1, 1), "series_ancient_grimoire"),
    S("zagred_legacy", "high", "Legacy Word-Soul Ember", "What is left of the first Zagred: an ember with a voice.", "5A2FA8", "D03A5A", "flame", [("absorption", 0), ("luck", 0)], ("anime_multiverse:heart_of_words", 0.3, 1, 1)),
    # ---- the three single-boss arcs and the gag boss
    S("goku_boss", "apex", "Saiyan's Unbowed Spirit", "A spark of a fighter who gets up every time.", "F08A24", "4FA8FF", "orb", [("strength", 1), ("speed", 1), ("regeneration", 1)], ("minecraft:golden_carrot", 0.7, 4, 8), "ruyi_power_pole"),
    S("kakashi_hatake", "apex", "Copy-Ninja's Sealed Scroll", "A scroll with a hundred techniques and no instructions.", "8C96A8", "4FA8FF", "scroll", [("speed", 1), ("night_vision", 0), ("resistance", 0)], ("minecraft:lightning_rod", 0.5, 1, 1), "perfect_susanoo_plate"),
    S("sinbad_boss", "apex", "Seven Seas Vessel Shard", "A shard of a vessel that carried a king over seven seas.", "5B3A8C", "FFD54F", "shard", [("water_breathing", 0), ("dolphins_grace", 1), ("luck", 1), ("strength", 0)], ("minecraft:nautilus_shell", 0.6, 1, 2), "leviathans_crown_trident"),
    S("light_yagami", "lesser", "Torn Death Note Page", "A page with a name on it. It is not yours, this time.", "1F7A74", "E9E7E0", "scroll", [("luck", 0), ("night_vision", 0)], ("minecraft:apple", 0.7, 1, 2)),
    # ---- the tier-list bosses of the floors 1 to 20 (F = lesser, D = mid; three D bosses are heavier)
    S("charlos", "lesser", "Celestial Dragon's Gilded Tag", "Nobody was ever allowed to touch it. You may.", "E0C050", "F4F0D8", "medal", [("luck", 0), ("speed", 0)], ("minecraft:gold_ingot", 0.6, 2, 4)),
    S("zetsu", "lesser", "Plant Clone Spore Pod", "A pod that was part of a man, once. It breathes.", "2E7D32", "E8F5E9", "orb", [("regeneration", 0), ("night_vision", 0)], ("minecraft:spore_blossom", 0.4, 1, 1)),
    S("yammy_llargo", "lesser", "Zero Espada's Mask Fragment", "A shard of a mask. The zero on it has worn away.", "8D6E63", "FFCCBC", "shard", [("strength", 0), ("resistance", 0)], ("minecraft:bone", 0.7, 3, 6)),
    S("sogou_nobuyuki", "lesser", "Fairy King's Gilded Feather", "A king who was never one: the feather is real.", "FFD54F", "B3E5FC", "scroll", [("speed", 0), ("jump_boost", 0), ("slow_falling", 0)], ("minecraft:feather", 0.7, 4, 8)),
    S("mr_2_bon_clay", "lesser", "Okama Way Ballet Ribbon", "A ribbon from a very good show. Take a bow.", "F8BBD0", "FFFFFF", "scroll", [("speed", 1), ("jump_boost", 1)], ("minecraft:pink_dye", 0.7, 2, 4)),
    S("kurozumi_orochi", "lesser", "Shogun's Eightfold Scale", "One scale of eight. The other seven are somewhere else.", "7B1FA2", "E1BEE7", "fang", [("resistance", 0), ("strength", 0)], ("minecraft:scute", 0.4, 1, 2)),
    S("malty_melromarc", "mid", "Scarlet Princess' Accusation Seal", "A seal with one word on it: guilty.", "C62828", "FFCDD2", "medal", [("luck", 1), ("speed", 0)], ("minecraft:redstone", 0.7, 4, 8)),
    S("rachel", "mid", "Climber's Lighthouse Light", "A little light that was meant for climbing, and for nothing else.", "80DEEA", "FFFFFF", "orb", [("night_vision", 0), ("slow_falling", 0), ("jump_boost", 1)], ("minecraft:lantern", 0.5, 1, 1)),
    S("jugram_haschwalth", "high", "Balance Ritter's Scale Weight", "A weight that tips every scale toward the Quincy.", "ECEFF1", "90CAF9", "medal", [("resistance", 1), ("strength", 0), ("absorption", 0)], ("minecraft:iron_ingot", 0.7, 4, 8)),
    S("big_mom", "high", "Emperor's Soul Candy", "A candy with a little piece of someone in it.", "F06292", "FFE082", "orb", [("health_boost", 1), ("regeneration", 0), ("strength", 0)], ("minecraft:sweet_berries", 0.8, 6, 12)),
    S("code", "mid", "Kara's Karma Shard", "A shard of a vessel built to be filled with a stranger.", "455A64", "80DEEA", "shard", [("strength", 0), ("resistance", 0)], ("minecraft:ender_pearl", 0.5, 1, 2)),
    S("bertholdt_hoover", "high", "Colossal Titan's Steam Ash", "Ash that is still giving off steam. It keeps its heat.", "BF360C", "FFCCBC", "flame", [("fire_resistance", 0), ("resistance", 1), ("strength", 0)], ("minecraft:blaze_powder", 0.6, 3, 6)),
    S("axe_hand_morgan", "lesser", "Axe-Hand's Marine Badge", "A badge from a captain who ruled a very small sea.", "78909C", "FFE0B2", "medal", [("strength", 0), ("haste", 0)], ("minecraft:iron_nugget", 0.7, 6, 12)),
    S("foxy", "lesser", "Silver Fox's Return Coin", "It always comes back to the one who throws it.", "B0BEC5", "FF8A65", "medal", [("speed", 1), ("luck", 0)], ("minecraft:gold_nugget", 0.7, 6, 12)),
    S("shou_tucker", "mid", "Sewing-Life Chimera Thread", "A thread that stitches a living thing to another. Do not ask.", "8D6E63", "C5E1A5", "scroll", [("regeneration", 0), ("resistance", 0)], ("minecraft:string", 0.7, 4, 8)),
    S("ao", "mid", "Byakugan Seer's Pale Lens", "A lens that sees the way a hand reaches before it does.", "4FC3F7", "FFFFFF", "orb", [("night_vision", 0), ("speed", 0), ("luck", 0)], ("minecraft:amethyst_shard", 0.5, 2, 4)),
    # ---- the dungeon's own bosses, the judge, and the fallback for a boss that has no table yet
    S("floor_warden", "lesser", "Warden's Sealed Shadow Link", "A link of the chain that holds the floor shut.", "7A7A8A", "D8C070", "medal", [("resistance", 0), ("night_vision", 0)]),
    S("labyrinth_guardian", "high", "Sector Guardian's Core Splinter", "A splinter of a core that kept a whole sector standing.", "3B3B52", "B58CFF", "shard", [("resistance", 1), ("strength", 0), ("regeneration", 0)]),
    S("god_of_order", "apex", "Judge's Unsealed Verdict", "A verdict nobody heard read. It still applies.", "F2E6B0", "FFFFFF", "scroll", [("resistance", 1), ("absorption", 1), ("luck", 1), ("regeneration", 0)], None, "administrators_authority_key"),
    S("default", "mid", "Boss Remnant", "What is left of a boss nobody wrote a drop for yet.", "8C8C9C", "D8C070", "orb", [("strength", 0), ("resistance", 0)]),
]


# ---------------------------------------------------------------- the 16 x 16 icons: six flat shapes, tinted per boss
def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def icon(shape, colour, accent):
    t = core.tones(hexrgb(colour))
    a = hexrgb(accent)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = out.load()
    cx, cy = 7.5, 7.5

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = tuple(c) + (255,)

    if shape == "orb":                                        # a sphere with a bright cap and an accent ring
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - cy
                d = (dx * dx + dy * dy) ** 0.5
                if d > 6.2:
                    continue
                lit = (-dx - dy) / 8.0
                c = t[4] if (dx < -1.5 and dy < -1.5 and d < 4.0) else t[3] if lit > 0.35 else t[2] if lit > -0.1 else t[1] if lit > -0.5 else t[0]
                if d > 5.2:
                    c = core.mix(c, (0, 0, 0), 0.35)
                if abs(d - 3.0) < 0.5 and dy > 0:
                    c = a
                put(x, y, c)
    elif shape == "shard":                                    # a tall crystal with two facets and an accent core
        for y in range(1, 15):
            half = 1.5 + (y - 1) * 0.42 if y < 8 else 7 - (y - 8) * 0.95
            half = max(0.5, min(half, 4.6))
            for x in range(16):
                dx = x - cx
                if abs(dx) > half:
                    continue
                c = t[3] if dx < -half * 0.35 else t[1] if dx > half * 0.35 else t[2]
                if abs(dx) > half - 0.9:
                    c = t[0]
                if abs(dx) < 0.8 and 4 <= y <= 10:
                    c = a
                put(x, y, c)
    elif shape == "flame":                                    # a teardrop flame, hot core
        for y in range(1, 15):
            w = 1.0 + (y - 1) * 0.55 if y < 9 else 6.0 - (y - 9) * 1.0
            w = max(0.6, min(w, 5.0))
            sway = 1.0 if y < 6 else 0.0
            for x in range(16):
                dx = x - cx - (sway if y < 5 else 0)
                if abs(dx) > w:
                    continue
                c = t[3] if abs(dx) < w * 0.55 else t[2]
                if abs(dx) < w * 0.3 and y > 5:
                    c = a
                if abs(dx) < w * 0.14 and y > 8:
                    c = (255, 255, 235)
                if abs(dx) > w - 0.9:
                    c = t[1]
                put(x, y, c)
    elif shape == "fang":                                     # a curved tooth, root at the top
        for y in range(1, 15):
            w = 4.6 - (y - 1) * 0.33
            if w < 0.4:
                continue
            bend = (y - 1) * 0.28
            for x in range(16):
                dx = x - cx - bend
                if abs(dx) > w:
                    continue
                c = t[4] if dx < -w * 0.4 else t[3] if dx < 0 else t[2] if dx < w * 0.5 else t[1]
                if y < 4:
                    c = core.mix(c, a, 0.55)
                if abs(dx) > w - 0.8:
                    c = t[0]
                put(x, y, c)
    elif shape == "medal":                                    # a disc on a two-tail ribbon with an accent emblem
        for y in range(0, 6):
            for x in (5, 6, 9, 10):
                put(x, y + 9, t[1] if x in (5, 9) else t[2])
        for y in range(16):
            for x in range(16):
                dx, dy = x - cx, y - 5.5
                d = (dx * dx + dy * dy) ** 0.5
                if d > 5.4:
                    continue
                c = t[3] if d < 3.4 else t[2]
                if d > 4.4:
                    c = t[1]
                if abs(dx) + abs(dy) < 2.4:
                    c = a
                put(x, y, c)
    else:                                                     # scroll: a rolled page, two caps, an accent seal
        for y in range(3, 13):
            for x in range(3, 13):
                c = (235, 226, 200) if x % 5 else (214, 202, 170)
                put(x, y, c)
        for x in range(2, 14):
            put(x, 2, t[2])
            put(x, 3, t[1])
            put(x, 12, t[1])
            put(x, 13, t[2])
        for y in (2, 3, 12, 13):
            put(1, y, t[3])
            put(14, y, t[3])
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                put(8 + dx, 8 + dy, a)
    return out


# ---------------------------------------------------------------- writers
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def jdump(o):
    return json.dumps(o, indent=2, ensure_ascii=False) + "\n"


def boon_json(boon, seconds, plus=0):
    return [{"effect": "minecraft:" + e, "seconds": min(3600, seconds), "amplifier": min(3, a + plus)} for e, a in boon]


def boon_text(boon, seconds):
    parts = [EFFECT_NAMES[e] + (" " + ROMAN[a] if a else "") for e, a in boon]
    mins = seconds // 60
    return "Right-click to absorb it: %s for %d minutes." % (", ".join(parts), mins)


def table(s):
    tier = s["tier"]
    sec = TIER_SECONDS[tier]
    rows = [{"item": i, "chance": c, "min": lo, "max": hi} for (i, c, lo, hi) in EXTRAS[tier]]
    if s["themed"]:
        i, c, lo, hi = s["themed"]
        rows.append({"item": i, "chance": c, "min": lo, "max": hi})
    t = {"boss": s["boss"], "tier": tier,
         "signature": {"chance": 1.0, "min": 1, "max": 1, "boon": boon_json(s["boon"], sec)},
         "extras": rows}
    if s["relic"]:
        t["one_of_one"] = {"relic": s["relic"], "echo_chance": ECHO_CHANCE[tier], "echo_boon": boon_json(s["boon"], sec * 2, 1)}
    return t


def item_model(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/" + tex}}


def main():
    lang = {"item.anime_multiverse.boss_signature": "Boss Signature",
            "item.anime_multiverse.boss_echo": "Echo of %s",
            "bossdrops2.tooltip.echo": "A fading copy of %s. The real one already exists in this world.",
            "bossdrops2.tooltip.unique": "One of one: only one exists in the world.",
            "bossdrops2.msg.unique": "%s falls, and the one-of-one is yours: %s.",
            "bossdrops2.msg.echo": "%s already exists in this world. You receive its echo instead.",
            "bossdrops2.msg.share": "%s: you fought it, and it pays you for it.",
            "bossdrops2.msg.used": "You absorb %s.",
            "bossdrops2.msg.faded": "Its power has faded: nothing happens.",
            "bossdrops2.cmd.given": "Gave %s.",
            "bossdrops2.cmd.unknown": "No boss table named %s."}
    index = []
    overrides = []
    for n, s in enumerate(SPEC, start=1):
        bid = s["boss"]
        index.append({"id": bid, "tier": s["tier"], "shape": s["shape"], "color": s["colour"], "accent": s["accent"], "relic": s["relic"] or ""})
        write(os.path.join(DATA, bid + ".json"), jdump(table(s)))
        save(icon(s["shape"], s["colour"], s["accent"]), os.path.join(ASSETS, "textures", "item", "sig", bid + ".png"))
        write(os.path.join(ASSETS, "models", "item", "sig", bid + ".json"), jdump(item_model("sig/" + bid)))
        overrides.append({"predicate": {"custom_model_data": n}, "model": "anime_multiverse:item/sig/" + bid})
        lang["bossdrops2.sig.%s.name" % bid] = s["sig"]
        lang["bossdrops2.sig.%s.lore" % bid] = s["lore"]
        lang["bossdrops2.sig.%s.boon" % bid] = boon_text(s["boon"], TIER_SECONDS[s["tier"]])
        if s["relic"]:
            lang["bossdrops2.sig.%s.echo_boon" % bid] = boon_text([(e, min(3, a + 1)) for e, a in s["boon"]], TIER_SECONDS[s["tier"]] * 2)
    base = {"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/boss_signature"}, "overrides": overrides}
    write(os.path.join(ASSETS, "models", "item", "boss_signature.json"), jdump(base))
    write(os.path.join(ASSETS, "models", "item", "boss_echo.json"), jdump(item_model("boss_echo")))
    save(icon("orb", "8C8C9C", "D8C070"), os.path.join(ASSETS, "textures", "item", "boss_signature.png"))
    save(icon("shard", "B58CFF", "FFFFFF"), os.path.join(ASSETS, "textures", "item", "boss_echo.png"))
    write(os.path.join(HERE, "boss_drops", "signature_index.json"), jdump({"_comment": "The order is the custom_model_data of every signature stack (1-based) and the order of bossdrops2/BossSigCatalog.java: append only.", "bosses": index}))
    write(os.path.join(HERE, "lang", "bossdrops2.json"), jdump(lang))
    print("%d boss tables, %d icons, %d lang keys" % (len(SPEC), len(SPEC) + 2, len(lang)))


if __name__ == "__main__":
    main()
