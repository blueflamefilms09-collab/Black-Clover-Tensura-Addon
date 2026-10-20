#!/usr/bin/env python3
"""Generates the 200 Decennial bosses (docs/labyrinth/decennial_bosses.md).

    python3 -B tools/gen_decennial_roster.py            # (re)writes data/nusmp/mega/decennial/bosses/*.json, tools/lang/decennial.json, docs/labyrinth/decennial_roster.md
    python3 -B tools/gen_decennial_roster.py --check    # exit 1 when a file on disk differs from what this script builds (CI-safe)

Deterministic: no random numbers, no clock. Every choice is a function of the level and the hand-written tables in tools/decennial (60 archetypes, the given names, the lair pools of the halls):
  * levels -100..-1 are the Evil road, 1..100 the Holy road; the boss of level L stands on floor 10 * L;
  * a few levels are fixed by the owner's brief or by canon: evil_001 is a Lich in the Graveyard, evil_002 a Fallen King in the Ruined Throne Room, the seven gate levels and the last three levels of the
    Evil half belong to the devils of the Underworld Root (Tree of Qliphoth), and the 17 famous D&D villains of the Guardian's own pool each stand as the boss of one level of their tier;
  * every other level takes the archetype that fits its depth and has been used least (never the same archetype or hall twice in a row), then a given name nobody has, a byname, a place, a look
    with a tint and a size, and the numbers its depth asks for.
"""
import colorsys
import hashlib
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(HERE, "decennial"))
import names as NAMES  # noqa: E402
from evil import EVIL  # noqa: E402
from holy import HOLY  # noqa: E402

OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "decennial", "bosses")
LAIRS_JSON = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "decennial", "lairs.json")
SIGS_JSON = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "decennial", "signatures.json")
FAMOUS_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "dnd_bosses", "famous")
LANG_OUT = os.path.join(ROOT, "tools", "lang", "decennial.json")
DOC_OUT = os.path.join(ROOT, "docs", "labyrinth", "decennial_roster.md")

# ---------------------------------------------------------------------------------------------------------------------- the halls
ARENAS = {
    "graveyard": "The Weeping Graveyard", "ruined_throne_room": "The Ruined Throne Room", "bone_cathedral": "The Bone Cathedral", "blood_arena": "The Blood Arena",
    "forbidden_library": "The Library of Forbidden Lore", "vampire_ballroom": "The Crimson Ballroom", "hell_pit": "The Hell Pit", "abyss_rift_ring": "The Ring of the Rift",
    "witch_hollow": "The Witch's Hollow", "plague_swamp": "The Plague Swamp", "spider_den": "The Spider's Den", "mind_hive": "The Hive of Minds",
    "cathedral": "The Cathedral of Dawn", "celestial_court": "The Celestial Court", "sun_temple": "The Temple of the Sun", "hanging_gardens": "The Hanging Gardens",
    "sky_bridge": "The Bridge of Sky", "crystal_sanctum": "The Crystal Sanctum", "moonlit_glade": "The Moonlit Glade", "storm_spire": "The Storm Spire",
    "volcanic_forge": "The Volcanic Forge", "frozen_keep": "The Frozen Keep", "sunken_temple": "The Sunken Temple", "dragon_hoard_cavern": "The Dragon's Hoard",
    "colosseum": "The Colosseum", "mirror_hall": "The Hall of Mirrors", "clockwork_vault": "The Clockwork Vault", "void_observatory": "The Observatory of the Void",
}
ORD = ["First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh"]
for _k in range(1, 8):
    ARENAS["qliphoth_gate_%d" % _k] = "The %s Gate of the Underworld Root" % ORD[_k - 1]
ARENAS["qliphoth_root_throne"] = "The Root Throne"

# the lair actions each hall suits (by side where the hall is shared)
THEME_LAIRS = {
    "graveyard": ["grave_hands", "soul_fog", "bone_spikes"],
    "ruined_throne_room": ["pillar_fall", "shadow_tendrils", "chain_pull"],
    "bone_cathedral": ["bone_spikes", "grave_hands", "soul_fog"],
    "blood_arena": ["blood_rain", "bone_spikes", "chain_pull"],
    "forbidden_library": ["tome_storm", "eye_beams", "shadow_tendrils"],
    "vampire_ballroom": ["blood_rain", "shadow_tendrils", "chain_pull"],
    "hell_pit": ["hell_geysers", "lava_surge", "chain_pull"],
    "abyss_rift_ring": ["abyss_maw", "void_collapse", "shadow_tendrils"],
    "witch_hollow": ["root_burst", "plague_cloud", "shadow_tendrils"],
    "plague_swamp": ["plague_cloud", "spore_burst", "root_burst"],
    "spider_den": ["web_snare", "spore_burst", "root_burst"],
    "mind_hive": ["eye_beams", "spore_burst", "void_collapse"],
    "cathedral": ["holy_beams", "radiant_nova", "judgment_rings"],
    "celestial_court": ["judgment_rings", "holy_beams", "falling_stars"],
    "sun_temple": ["sunburst", "radiant_nova", "falling_stars"],
    "hanging_gardens": ["thorn_bloom", "moonbeam", "holy_beams"],
    "sky_bridge": ["wind_gusts", "storm_bolts", "falling_stars"],
    "crystal_sanctum": ["crystal_resonance", "holy_beams", "moonbeam"],
    "moonlit_glade": ["moonbeam", "thorn_bloom", "falling_stars"],
    "storm_spire": ["storm_bolts", "wind_gusts", "radiant_nova"],
    "volcanic_forge": {"evil": ["lava_surge", "hell_geysers", "steam_vents"], "holy": ["lava_surge", "steam_vents", "radiant_nova"]},
    "frozen_keep": {"evil": ["frost_nova", "pillar_fall", "bone_spikes"], "holy": ["frost_nova", "pillar_fall", "holy_beams"]},
    "sunken_temple": {"evil": ["tide_surge", "pillar_fall", "shadow_tendrils"], "holy": ["tide_surge", "pillar_fall", "holy_beams"]},
    "dragon_hoard_cavern": {"evil": ["lava_surge", "pillar_fall", "hell_geysers"], "holy": ["pillar_fall", "wind_gusts", "holy_beams"]},
    "colosseum": {"evil": ["pillar_fall", "bone_spikes", "chain_pull"], "holy": ["pillar_fall", "judgment_rings", "holy_beams"]},
    "mirror_hall": {"evil": ["mirror_shatter", "eye_beams", "shadow_tendrils"], "holy": ["mirror_shatter", "radiant_nova", "holy_beams"]},
    "clockwork_vault": {"evil": ["steam_vents", "pillar_fall", "chain_pull"], "holy": ["steam_vents", "pillar_fall", "judgment_rings"]},
    "void_observatory": {"evil": ["void_collapse", "eye_beams", "shadow_tendrils"], "holy": ["void_collapse", "falling_stars", "holy_beams"]},
}

GRADES = [(12, "special"), (30, "rare"), (52, "unique"), (75, "legend"), (92, "god"), (100, "genesis")]


def grade_of(n):
    for hi, g in GRADES:
        if n <= hi:
            return g
    return "genesis"


def tier_of(n):
    return min(5, (n - 1) // 20 + 1)


def hsh(*parts):
    return int(hashlib.sha256("|".join(str(p) for p in parts).encode("utf-8")).hexdigest()[:12], 16)


def shift_tint(hexcol, steps):
    r, g, b = [int(hexcol[i:i + 2], 16) / 255.0 for i in (1, 3, 5)]
    hh, ss, vv = colorsys.rgb_to_hsv(r, g, b)
    hh = (hh + 0.035 * steps) % 1.0
    r, g, b = colorsys.hsv_to_rgb(hh, ss, vv)
    return "#%02X%02X%02X" % (round(r * 255), round(g * 255), round(b * 255))


# ---------------------------------------------------------------------------------------------------------------------- fixed levels
# The Underworld Root (Tree of Qliphoth): seven gates, ten devils. depth -> profile. The names are the devils the mod already has (blackclover.Devil) plus one original warden.
QLIPHOTH = {
    14: dict(given="Walgner", title="Devil of Blood, Warden of the First Gate", devil="WALGNER", theme="qliphoth_gate_1", band=1, look=("vrock", "#8A1C2C", 1.0), creature="devil", sig="sanguine_pact",
             lairs=["black_sap", "root_burst"], abil=[["vampiric_bite"], ["tail_sweep"], ["blink"], ["life_drain"]], flags={"frightful": True, "summoner": True}, resist=["physical"], weapon="axe",
             lore="Walgner drinks what he is given. The first gate of the Underworld Root opened for him, and the first thing he did was ask whose blood was nearest.",
             phases=["Guest of the First Gate", "Blood Drunk", "Root-Fed", "Red Tide"]),
    28: dict(given="Nahamah", title="Devil of Flame, Warden of the Second Gate", devil="NAHAMAH", theme="qliphoth_gate_2", band=2, look=("balor", "#FF6A1A", 1.0), creature="devil", sig="hellfire_aura",
             lairs=["black_sap", "hell_geysers"], abil=[["tail_sweep"], ["dragon_breath"], ["wing_buffet"], ["frightful_presence"]], flags={"frightful": True, "summoner": True, "breath": "fire"},
             resist=["fire", "physical"], weapon="axe",
             lore="Nahamah burns the roots of the Tree to keep warm, and they grow back black so that she may burn them again. The second gate is the hottest room in the Underworld.",
             phases=["Ember at the Gate", "Roots Alight", "Furnace Unbound", "Black Fire"]),
    42: dict(given="Lilith", title="Devil of Ice, Warden of the Third Gate", devil="LILITH", theme="qliphoth_gate_3", band=3, look=("zariel", "#8FD8FF", 1.0), creature="devil", sig="frost_heart",
             lairs=["black_sap", "frost_nova"], abil=[["cantrip"], ["tail_sweep"], ["frightful_presence"], ["dragon_breath"]], flags={"frightful": True, "summoner": True, "breath": "frost"},
             resist=["freeze", "magic"], weapon="spear",
             lore="Lilith froze the sap of the third level into black glass, and walks on it. The roots here are brittle, and the silence is hers.",
             phases=["Frost at the Gate", "Glass Roots", "The Third Winter", "Black Ice"]),
    57: dict(given="Astaroth", title="Devil of Time, Warden of the Fourth Gate", devil="ASTAROTH", theme="qliphoth_gate_4", band=4, look=("vecna", "#D8C070", 1.05), creature="devil",
             sig="oathbreaker_charge", lairs=["root_burst", "chain_pull"], abil=[["blink"], ["counterspell"], ["gravity_crush"], ["death_touch"]], flags={"frightful": True, "summoner": True},
             resist=["magic", "lightning"], weapon="rapier",
             lore="Astaroth is always a moment ahead of you. By the time the roots of the fourth gate have closed on a hero, he has already gone on to the next.",
             phases=["A Moment Ahead", "Hours Slipping", "The Fourth Gate Stops", "Last Second"]),
    71: dict(given="Beelzebub", title="Devil of Space, Warden of the Fifth Gate", devil="BEELZEBUB", theme="qliphoth_gate_5", band=5, look=("demogorgon", "#7060C0", 1.0), creature="devil",
             sig="infernal_contract", lairs=["black_sap", "void_collapse"], abil=[["tail_sweep"], ["eye_rays"], ["gravity_crush"], ["mind_blast"]], flags={"frightful": True, "summoner": True},
             resist=["magic", "physical"], weapon="scythe",
             lore="Beelzebub folds the roots of the fifth level until no two of them are where they were. Nothing here is where you left it, including you.",
             phases=["Folded Gate", "Space Torn", "The Fifth Gate Opens", "Where You Were"]),
    85: dict(given="Liebe", title="Devil of the Null, Warden of the Sixth Gate", devil="LIEBE", theme="qliphoth_gate_6", band=6, look=("asmodeus", "#C0C0C8", 1.0), creature="devil", sig="stolen_light",
             lairs=["root_burst", "abyss_maw"], abil=[["counterspell"], ["magic_drain"], ["frightful_presence"], ["death_touch"]], flags={"frightful": True, "anti_magic": True, "summoner": True},
             resist=["magic", "lightning"], weapon="greatsword",
             lore="Liebe does not fight magic. She does not allow it. At the sixth gate, every spell that reaches her is a thing that never happened.",
             phases=["Silence at the Gate", "Magic Refused", "The Sixth Gate Stills", "The Null"]),
    97: dict(given="Ovrazel", title="the Threshold Devil, Warden of the Seventh Gate", devil="", theme="qliphoth_gate_7", band=7, look=("balor", "#201018", 1.1), creature="devil", sig="devil_union",
             lairs=["black_sap", "abyss_maw"], abil=[["tail_sweep"], ["dragon_breath"], ["gravity_crush"], ["haki_pulse"]], flags={"frightful": True, "anti_magic": True, "summoner": True, "breath": "fire"},
             resist=["fire", "magic", "physical"], weapon="greatsword",
             lore="Ovrazel is the last door before the throne, and the only devil of the Root that was never given a name by men. She has kept the seventh gate since before there was a tree.",
             phases=["The Last Door", "Gate Unsealed", "The Threshold Burns", "No Further"]),
    98: dict(given="Megicula", title="Supreme Devil of Curses", devil="MEGICULA", theme="qliphoth_root_throne", band=7, look=("strahd", "#701050", 1.15), creature="devil", sig="hex_cauldron",
             lairs=["black_sap", "shadow_tendrils"], abil=[["life_drain"], ["mind_blast"], ["death_touch"], ["frightful_presence"]], flags={"frightful": True, "anti_magic": True, "summoner": True, "absorb": True},
             resist=["magic", "poison"], weapon="staff",
             lore="Megicula wears the curses of ten thousand years as a gown. The first of the three Supreme Devils receives the party at the root of the Tree, and is gracious about it.",
             phases=["Gracious Hostess", "The Gown Unravels", "Curses Called Home", "Ten Thousand Years"]),
    99: dict(given="Zagred", title="Supreme Devil of the Word", devil="ZAGRED", theme="qliphoth_root_throne", band=7, look=("asmodeus", "#3A2A6A", 1.15), creature="devil", sig="sanguine_echo",
             lairs=["black_sap", "void_collapse"], abil=[["counterspell"], ["magic_drain"], ["death_touch"], ["frightful_presence"]], flags={"frightful": True, "anti_magic": True, "summoner": True},
             resist=["magic", "physical"], weapon="staff",
             lore="Zagred has only to say a thing for it to be true, and the roots of the Tree listen better than anyone. The second Supreme Devil speaks softly.",
             phases=["The Quiet Voice", "Words Made True", "The Roots Obey", "Last Word"]),
    100: dict(given="Lucifero", title="Supreme Devil of Gravity, Ruler of the Underworld Root", devil="LUCIFERO", theme="qliphoth_root_throne", band=7, look=("balor", "#2A1A4A", 1.3), creature="devil",
              sig="infernal_contract", lairs=["black_sap", "abyss_maw"], abil=[["tail_sweep"], ["gravity_crush"], ["haki_pulse"], ["death_touch"]],
              flags={"frightful": True, "anti_magic": True, "summoner": True, "absorb": True}, resist=["magic", "physical", "fire"], weapon="greatsword",
              lore="Lucifero sits on the Root Throne. The Tree is his body, the Underworld his weight, and every gate you passed was a door he held open for you. The last Supreme Devil is the end of the road.",
              phases=["Ruler of the Root", "The Weight Falls", "All Seven Gates", "The Whole Underworld"]),
}

# the owner's two examples
FIXED_EVIL = {1: ("lich", "graveyard"), 2: ("fallen_king", "ruined_throne_room")}
FIXED_HOLY = {1: ("paladin", "cathedral")}

# the 17 famous D&D villains each stand as the boss of one level of their tier (they keep their own name, look and legendary actions)
FAMOUS_LEVEL = {
    "strahd": (-23, "vampire_ballroom", "vampire"), "beholder": (-35, "mind_hive", "aberration"), "lich_king": (-47, "bone_cathedral", "undead"),
    "mind_flayer_elder_brain": (-52, "mind_hive", "aberration"), "lolth": (-63, "spider_den", "monstrosity"), "dracolich": (-66, "frozen_keep", "dragon"),
    "kraken": (-68, "sunken_temple", "monstrosity"), "zariel": (-74, "hell_pit", "devil"), "demogorgon": (-78, "abyss_rift_ring", "demon"), "orcus": (-82, "graveyard", "demon"),
    "acererak": (-88, "forbidden_library", "undead"), "tiamat": (-91, "dragon_hoard_cavern", "dragon"), "asmodeus": (-94, "ruined_throne_room", "devil"), "vecna": (-96, "void_observatory", "undead"),
    "solar": (55, "celestial_court", "celestial"), "tarrasque": (77, "colosseum", "monstrosity"), "bahamut": (90, "sky_bridge", "dragon"),
}
FAMOUS_SIG = {"strahd": "sanguine_pact", "beholder": "void_craving", "lich_king": "phylactery_ward", "mind_flayer_elder_brain": "void_craving", "lolth": "web_of_fate", "dracolich": "dragon_pride",
              "kraken": "tide_ward", "zariel": "devil_union", "demogorgon": "dread_presence", "orcus": "grave_tithe", "acererak": "phylactery_ward", "tiamat": "dragon_pride",
              "asmodeus": "infernal_contract", "vecna": "stolen_light", "solar": "radiant_ward", "tarrasque": "stone_covenant", "bahamut": "dragon_pride"}
FAMOUS_SOUND = {"strahd": "vampire_hiss", "beholder": "aberrant_hum", "lich_king": "undead_whisper", "mind_flayer_elder_brain": "aberrant_hum", "lolth": "spider_chitter", "dracolich": "dragon_roar",
                "kraken": "kraken_deep", "zariel": "devil_laugh", "demogorgon": "demon_growl", "orcus": "demon_growl", "acererak": "undead_whisper", "tiamat": "dragon_roar",
                "asmodeus": "devil_laugh", "vecna": "undead_whisper", "solar": "angelic_choir", "tarrasque": "stone_guardian", "bahamut": "dragon_roar"}
FAMOUS_WEAPON = {"strahd": "rapier", "beholder": "wand", "lich_king": "greatsword", "mind_flayer_elder_brain": "wand", "lolth": "dagger", "dracolich": "greatsword", "kraken": "spear", "zariel": "longsword",
                 "demogorgon": "scythe", "orcus": "mace", "acererak": "staff", "tiamat": "greatsword", "asmodeus": "greatsword", "vecna": "staff", "solar": "bow", "tarrasque": "hammer", "bahamut": "glaive"}
FAMOUS_ADDS = {"strahd": ["vex", "zombie"], "beholder": ["vex", "endermite"], "lich_king": ["skeleton", "stray"], "mind_flayer_elder_brain": ["zombie", "endermite"], "lolth": ["cave_spider", "spider"],
               "dracolich": ["skeleton", "vex"], "kraken": ["drowned"], "zariel": ["blaze", "vex"], "demogorgon": ["vex", "blaze"], "orcus": ["skeleton", "zombie", "wither_skeleton"],
               "acererak": ["skeleton", "vex"], "tiamat": ["blaze", "cave_spider", "bogged"], "asmodeus": ["blaze", "wither_skeleton"], "vecna": ["skeleton", "vex"],
               "solar": ["living_armor", "vex"], "tarrasque": ["living_armor"], "bahamut": ["living_armor", "vex"]}


FAMOUS_LORE = {
    "strahd": "Strahd rules his ballroom the way he rules the mist: completely, and forever. The Devil of Barovia has outlived every family that ever feared him, and means to outlive yours.",
    "beholder": "A sphere of hate with an eye for every kind of ruin. The Beholder has never been surprised, and does not intend to start.",
    "lich_king": "He wears the frost-steel of a king who sold his people for one more winter. The Lich King commands the dead of the Ossuary and a runeblade that remembers each of them.",
    "mind_flayer_elder_brain": "The Elder Brain floats in its jar of green ooze and thinks. Everything in the colony is a thought of it, and most of the thoughts are about you.",
    "lolth": "Lolth is the Queen of the Demonweb: half dark elf, half spider, and entirely patient. She makes promises in silk, and silk keeps.",
    "dracolich": "A dragon that died frozen and was not permitted to stay dead. The Dracolich guards the Frozen Crypt with a breath that has outlasted its owner's name.",
    "kraken": "The Kraken has eaten the sea beneath the Sunken Temple and begun on what stands above it. Its eight arms are the only part of it you will ever see move.",
    "zariel": "Once an angel, now an archduchess of the Nine Hells in burnt plate and a broken halo. Zariel keeps her war going, and you are on the front line.",
    "demogorgon": "The Prince of Demons has two heads that do not agree with each other, and a madness that agrees with everything. The rift opens wherever he looks.",
    "orcus": "The Demon Prince of Undeath wields a wand that unmakes the living. Orcus keeps the graveyard because he likes the company of what is left.",
    "acererak": "A lich who built a tomb to feed on the souls of those who tried to rob it. Acererak has read every page of your life, and has not found it interesting.",
    "tiamat": "Five heads, five breaths, and one craving. Tiamat the Five-Headed Queen counts the hoard of her cavern in the lives of heroes.",
    "asmodeus": "Lord of the Nine Hells, Asmodeus has never broken a contract, and has never needed to. The throne he sits on was made from the last person who argued.",
    "vecna": "The Whispered One lost a hand and an eye and gained the Hand and the Eye of Vecna. He is a god of secrets, and he knows yours.",
    "solar": "The Solar is the strongest of the angels, a bow of pure light in a hand that has never shaken. He is the judgement of the heavens, and you are the case.",
    "tarrasque": "The Last Sleeper wakes only when the world is already ending. The Tarrasque cannot be reasoned with, because it was never listening.",
    "bahamut": "The Platinum Dragon, king of the good dragons, descended from his mountain of clouds to measure the heroes of the dungeon. He will be fair. He will not be gentle.",
}

FAMOUS_NAME = {"lich_king": ("The Lich King", "Lord of the Ossuary"), "mind_flayer_elder_brain": ("The Elder Brain", "Mind of the Illithid Colony"),
               "dracolich": ("The Dracolich", "Wyrm of the Frozen Crypt"), "kraken": ("The Kraken", "Terror of the Black Tide")}


def full_name(given, byname):
    """'Jezreth the Undying Scholar' / 'Gehennis of the Silken Chains' / 'Malgrave, Keeper of the Ninth Hoard' / 'Ilvarune, Who Eats the Constellations'."""
    if byname.startswith(("the ", "of ")):
        return "%s %s" % (given, byname)
    return "%s, %s" % (given, byname)


# ---------------------------------------------------------------------------------------------------------------------- helpers
def phase_count(n, special=0):
    if special:
        return special
    return 2 if n <= 20 else 3 if n <= 70 else 4


def build_phases(names4, groups4, n, tier, summoner, agile, adds_kinds):
    pick = {2: [0, 3], 3: [0, 1, 3], 4: [0, 1, 2, 3]}[n]
    if n == 2:
        groups = [groups4[0], groups4[1] + groups4[2] + groups4[3]]
    elif n == 3:
        groups = [groups4[0], groups4[1], groups4[2] + groups4[3]]
    else:
        groups = list(groups4)
    at = {2: [1.0, 0.5], 3: [1.0, 0.66, 0.33], 4: [1.0, 0.75, 0.5, 0.25]}[n]
    speed = {2: [1.0, 1.08], 3: [1.0, 1.06, 1.12], 4: [1.0, 1.05, 1.10, 1.16]}[n]
    dmg = {2: [1.0, 1.15], 3: [1.0, 1.1, 1.25], 4: [1.0, 1.08, 1.18, 1.32]}[n]
    out = []
    for i in range(n):
        if summoner:
            cnt = [0, 2, 3, 3][i if n == 4 else (0 if i == 0 else (1 if i == 1 else 3 if n == 3 else 1))]
            if i > 0 and tier >= 4:
                cnt += 1
        else:
            cnt = 0 if i == 0 else 1 + (1 if i >= 2 else 0)
        cnt = min(cnt, 6)
        kinds = list(adds_kinds)[:3] if cnt > 0 else []
        out.append(dict(name=names4[pick[i]], at=at[i], abilities=groups[i], speed=round(min(1.35, speed[i] * (1.04 if agile else 1.0)), 2), damage=round(dmg[i], 2), add_count=cnt, add_kinds=kinds))
    return out


def adds_spec(tier, summoner, kinds):
    if not kinds:
        kinds = ["skeleton"]
    if summoner:
        return dict(pool=list(kinds), max_alive=min(14, 4 + 2 * tier), fight_cap=min(48, 14 + 8 * tier), wave=2 if tier >= 2 else 0, wave_every=45 if tier >= 2 else 0)
    return dict(pool=list(kinds), max_alive=3 + tier, fight_cap=8 + 4 * tier, wave=0, wave_every=0)


def lair_pool(theme, side):
    p = THEME_LAIRS[theme]
    return p[side] if isinstance(p, dict) else p


def pick_lairs(theme, side, n, seed, lair_count, tier):
    pool = lair_pool(theme, side)
    a = pool[seed % len(pool)]
    b = pool[(seed + 1) % len(pool)]
    base = [22, 20, 18, 16, 14][tier - 1]
    power = round(1.0 + 0.04 * tier, 2)
    out = [dict(id=a, every=base, from_phase=1, power=power)]
    if lair_count > 1:
        out.append(dict(id=b, every=base + 4, from_phase=2, power=power))
    return out


def dnd_numbers(tier, flags, famous=None):
    d = dict(resistances=[2, 3, 3, 4, 5][tier - 1], legendary_points=[3, 3, 4, 4, 5][tier - 1], anti_magic_aura=bool(flags.get("anti_magic")) and tier >= 2, frightful_aura=bool(flags.get("frightful")),
             absorb_magic=bool(flags.get("absorb")), breath=flags.get("breath", "fire"), reflective=bool(flags.get("reflective")))
    return d


def mana_core(given, tier):
    return dict(title="Core of %s" % given, tier=tier)


# ---------------------------------------------------------------------------------------------------------------------- build
def build():
    problems = []
    lairs_json = json.load(open(LAIRS_JSON, encoding="utf-8"))
    sigs_json = json.load(open(SIGS_JSON, encoding="utf-8"))
    for pool in THEME_LAIRS.values():
        for p in ([pool] if isinstance(pool, list) else pool.values()):
            for l in p:
                if l not in lairs_json:
                    problems.append("unknown lair " + l)
    for t in THEME_LAIRS:
        if t not in ARENAS:
            problems.append("unknown theme " + t)
    famous = {}
    for fn in sorted(os.listdir(FAMOUS_DIR)):
        famous[fn[:-5]] = json.load(open(os.path.join(FAMOUS_DIR, fn), encoding="utf-8"))
    bosses = {}  # level -> dict

    given_pool = {"evil": list(NAMES.EVIL), "holy": list(NAMES.HOLY)}
    for side in given_pool:
        given_pool[side].sort(key=lambda g: hsh("given", side, g))
    arch = {"evil": {a["key"]: a for a in EVIL}, "holy": {a["key"]: a for a in HOLY}}
    for side in arch:
        for a in arch[side].values():
            for s in a["sigs"]:
                if s not in sigs_json:
                    problems.append("%s: unknown signature %s" % (a["key"], s))
            for t in a["themes"]:
                if t not in ARENAS:
                    problems.append("%s: unknown theme %s" % (a["key"], t))
            for lk in a["abil"]:
                for ab in lk:
                    if ab not in ("tail_sweep", "blink", "cantrip", "wing_buffet", "frightful_presence", "magic_drain", "counterspell", "life_drain", "haki_pulse", "gravity_crush", "dragon_breath",
                                  "eye_rays", "mind_blast", "death_touch", "vampiric_bite"):
                        problems.append("%s: unknown ability %s" % (a["key"], ab))

    # ---- the devils of the Underworld Root and the famous villains first
    for depth, q in QLIPHOTH.items():
        level = -depth
        n = depth
        tier = tier_of(n)
        pc = phase_count(n, special=3 if n < 71 else 4)
        look, tint, scale = q["look"]
        flags = q["flags"]
        sc = scale
        lair_n = 2
        phases = build_phases(q["phases"], q["abil"], pc, tier, True, True, ["blaze", "vex", "wither_skeleton"])
        bosses[level] = dict(
            id="evil_%03d" % depth, level=level, side="evil", name=q["given"], title=q["title"], lore=q["lore"], archetype="qliphoth", theme=q["theme"], creature=q["creature"], resist=q["resist"][:3],
            signature=q["sig"], phases=phases, lair_actions=[dict(id=l, every=[22, 20, 18, 16, 14][tier - 1] + (0 if i == 0 else 4), from_phase=1 if i == 0 else 2, power=round(1.0 + 0.05 * tier, 2))
                                                           for i, l in enumerate(q["lairs"][:lair_n])],
            adds=adds_spec(tier, True, ["blaze", "vex", "wither_skeleton"]), loot=dict(grade=grade_of(n), kind="weapon", type=q["weapon"], keywords=["devil", "root", "underworld", "gate", q["given"].lower()]),
            mana_core=dict(title="Gate-Core of %s" % q["given"], tier=tier), look=look, tint=tint, scale=round(sc, 2), sound_set="devil_laugh", famous="",
            dnd=dnd_numbers(tier, flags), band=q["band"], devil=q["devil"])
    for fid, (level, theme, creature) in FAMOUS_LEVEL.items():
        f = famous[fid]
        n = abs(level)
        tier = tier_of(n)
        side = "evil" if level < 0 else "holy"
        full = f["name"]
        if fid in FAMOUS_NAME:
            nm, title = FAMOUS_NAME[fid]
        elif ", " in full:
            nm, title = full.split(", ", 1)
            title = title[0].upper() + title[1:] if title else title
        else:
            nm, title = full, "Boss of the %s" % ARENAS[theme][4:]
        forms = f["forms"]
        legends = [a["id"] for a in f.get("legendary_actions", [])]
        groups = [[legends[0]], [legends[1]] if len(legends) > 1 else [legends[0]], [legends[2]] if len(legends) > 2 else [legends[0]], [x for x in legends[3:]] or [legends[-1]]]
        flags = dict(anti_magic=f.get("anti_magic_aura"), frightful=f.get("frightful_aura"), absorb=f.get("absorb_magic"), reflective=f.get("reflective"), breath=f.get("breath", "fire"))
        phases = build_phases([forms[0], forms[1], forms[1], forms[2]], groups, 3, tier, True, False, FAMOUS_ADDS[fid])
        sd = abs(level)
        seed = hsh("famous-lairs", fid)
        la = pick_lairs(theme, side, 2, seed, 2, tier)
        bosses[level] = dict(
            id=("%s_%03d" % (side, n)), level=level, side=side, name=nm, title=title, lore=FAMOUS_LORE[fid], archetype="famous:" + fid, theme=theme, creature=creature, resist=[], signature=FAMOUS_SIG[fid], phases=phases, lair_actions=la, adds=adds_spec(tier, True, FAMOUS_ADDS[fid]),
            loot=dict(grade=grade_of(n), kind="weapon", type=FAMOUS_WEAPON[fid], keywords=[fid.split("_")[0]]), mana_core=mana_core(nm.replace("The ", ""), tier), look=f.get("look", ""),
            tint="#FFFFFF", scale=1.0, sound_set=FAMOUS_SOUND[fid], famous=fid,
            dnd=dict(resistances=f.get("resistances", 3), legendary_points=f.get("legendary_points", 3), anti_magic_aura=bool(f.get("anti_magic_aura")), frightful_aura=bool(f.get("frightful_aura")),
                     absorb_magic=bool(f.get("absorb_magic")), breath=f.get("breath", "fire"), reflective=bool(f.get("reflective"))), band=0, devil="")
        # the famous profile's lair actions are replaced by the hall's (the D&D lair ids stay on the Guardian's own profile)

    # ---- the rest, level by level, each side on its own
    for side in ("evil", "holy"):
        sign = -1 if side == "evil" else 1
        fixed = FIXED_EVIL if side == "evil" else FIXED_HOLY
        usage = {k: 0 for k in arch[side]}
        theme_use = {}
        last_arch, last_theme = [], []
        for n in range(1, 101):
            level = sign * n
            if level in bosses:
                last_theme.append(bosses[level]["theme"])
                continue
            tier = tier_of(n)
            if n in fixed:
                key, theme = fixed[n]
            else:
                cands = [a for a in arch[side].values() if a["lv"][0] <= n <= a["lv"][1]]
                if len(cands) < 3:
                    problems.append("%s level %d: only %d archetypes fit" % (side, n, len(cands)))
                best = None
                for a in cands:
                    score = usage[a["key"]] * 10 + (40 if a["key"] in last_arch[-3:] else 0) + (hsh("arch", side, n, a["key"]) % 7)
                    if best is None or score < best[0]:
                        best = (score, a["key"])
                key = best[1]
                a = arch[side][key]
                choices = list(a["themes"])
                tbest = None
                for t in choices:
                    ts = theme_use.get(t, 0) * 10 + (30 if t in last_theme[-2:] else 0) + (hsh("theme", side, n, t) % 5)
                    if tbest is None or ts < tbest[0]:
                        tbest = (ts, t)
                theme = tbest[1]
            a = arch[side][key]
            idx = usage[key]
            usage[key] += 1
            theme_use[theme] = theme_use.get(theme, 0) + 1
            last_arch.append(key)
            last_theme.append(theme)
            if not given_pool[side]:
                problems.append("out of given names for " + side)
                break
            given = given_pool[side].pop(0)
            byname = a["bynames"][(idx + hsh("by", side, n)) % len(a["bynames"])]
            place = a["places"][idx % len(a["places"])]
            title = "%s of %s" % (a["noun"], place) if not place.startswith("the ") or True else a["noun"]
            lore = a["lore"][idx % len(a["lore"])].format(name=given, place=place)
            look, tint, scale = a["looks"][(idx + hsh("look", side, n)) % len(a["looks"])]
            tint = shift_tint(tint, idx % 4 - 1)
            scale = round(max(0.7, min(1.5, scale * (0.96 + 0.02 * (idx % 3)) * (1.0 + 0.015 * tier))), 2)
            pc = phase_count(n)
            flags = a["flags"]
            summoner = bool(flags.get("summoner"))
            agile = key in ("dread_knight", "vampire_lord", "paladin", "sky_knight", "thunder_seraph", "mirror_duke", "shadow_reaper", "sin_duchess")
            phases = build_phases(a["phases"], a["abil"], pc, tier, summoner, agile, a["adds"])
            lair_count = 1 if n <= 12 else 2
            seed = hsh("lair", side, n)
            lairs = pick_lairs(theme, side, 2, seed, lair_count, tier)
            sig = a["sigs"][(idx + hsh("sig", side, n)) % len(a["sigs"])]
            resist = list(a["resist"])[:3]
            bosses[level] = dict(
                id="%s_%03d" % (side, n), level=level, side=side, name=full_name(given, byname), title=title, lore=lore, archetype=key, theme=theme, creature=a["creature"], resist=resist, signature=sig,
                phases=phases, lair_actions=lairs, adds=adds_spec(tier, summoner, a["adds"]), loot=dict(grade=grade_of(n), kind=a["kind"], type=a["weapon"], keywords=list(a["kw"])),
                mana_core=mana_core(given, tier), look=look, tint=tint, scale=scale, sound_set=a["sound"], famous="", dnd=dnd_numbers(tier, flags), band=0, devil="", given=given)

    # the Evil bands of the Underworld Root
    for level, b in bosses.items():
        if level < 0 and b["band"] == 0:
            depth = -level
            band = 1
            for i, start in enumerate((1, 15, 29, 43, 58, 72, 86)):
                if depth >= start:
                    band = i + 1
            b["band"] = band
    return bosses, problems


def checks(bosses, problems):
    names = {}
    titles = {}
    if len(bosses) != 200:
        problems.append("expected 200 bosses, got %d" % len(bosses))
    for level, b in sorted(bosses.items()):
        if b["name"] in names:
            problems.append("duplicate name %s (%s and %s)" % (b["name"], names[b["name"]], b["id"]))
        names[b["name"]] = b["id"]
        key = (b["title"], b["side"])
        if key in titles and not b["archetype"].startswith("famous"):
            problems.append("duplicate title %s (%s and %s)" % (b["title"], titles[key], b["id"]))
        titles[key] = b["id"]
        if b["theme"] not in ARENAS:
            problems.append("%s: unknown theme %s" % (b["id"], b["theme"]))
        if not (2 <= len(b["phases"]) <= 4):
            problems.append("%s: bad phase count" % b["id"])
        if not (1 <= len(b["lair_actions"]) <= 2):
            problems.append("%s: bad lair count" % b["id"])
    holy = sum(1 for b in bosses.values() if b["side"] == "holy")
    if holy != 100:
        problems.append("expected 100 holy bosses, got %d" % holy)
    return problems


def to_json(b):
    o = dict(b)
    o.pop("given", None)
    if not o["devil"]:
        o.pop("devil")
    if o["band"] == 0:
        o.pop("band")
    if not o["famous"]:
        o.pop("famous")
    order = ["id", "level", "side", "name", "title", "lore", "archetype", "theme", "creature", "resist", "signature", "phases", "lair_actions", "adds", "loot", "mana_core", "look", "tint", "scale",
             "sound_set", "famous", "dnd", "band", "devil"]
    res = {}
    for k in order:
        if k in o:
            res[k] = o[k]
    return json.dumps(res, indent=2, ensure_ascii=False) + "\n"


def lang_fragment(bosses):
    lairs_json = json.load(open(LAIRS_JSON, encoding="utf-8"))
    sigs_json = json.load(open(SIGS_JSON, encoding="utf-8"))
    out = {}
    for level, b in sorted(bosses.items()):
        out["decennial.boss.%s.name" % b["id"]] = b["name"]
        out["decennial.boss.%s.title" % b["id"]] = b["title"]
        out["decennial.boss.%s.lore" % b["id"]] = b["lore"]
        for i, p in enumerate(b["phases"], 1):
            out["decennial.boss.%s.phase.%d" % (b["id"], i)] = p["name"]
    for t, title in ARENAS.items():
        out["decennial.arena.%s" % t] = title
    for lid, l in lairs_json.items():
        if not lid.startswith("_"):
            out["decennial.lair.%s" % lid] = l["text"]
    for sid, s in sigs_json.items():
        if not sid.startswith("_"):
            out["decennial.signature.%s" % sid] = s["text"]
    out.update({
        "message.nusmp.mega.decennial.phase": "%1$s shifts into its next form: %2$s!",
        "message.nusmp.mega.decennial.sealed": "The doors of the hall seal behind you.",
        "message.nusmp.mega.decennial.unsealed": "The seal of the hall breaks.",
        "message.nusmp.mega.decennial.gate_open": "The great gate groans open. Take the Mana Core to the Gatekeeper.",
        "message.nusmp.mega.decennial.reset": "The hall resets. The fallen are forgotten, and the seals are restored.",
        "message.nusmp.mega.decennial.adds": "Something stirs in the ground!",
        "message.nusmp.mega.decennial.element.tombstone": "The tombstone cracks, and something answers.",
        "message.nusmp.mega.decennial.element.pillar": "The pillar breaks, and falls!",
        "message.nusmp.mega.decennial.element.root_node": "The root node bursts: lesser devils pour out!",
        "message.nusmp.mega.decennial.weak_point": "A weak point breaks: %1$s staggers.",
        "message.nusmp.mega.decennial.loot": "%1$s lets go of %2$s.",
        "message.nusmp.mega.decennial.describe": "Level %1$s (floor %2$s): %3$s, %4$s. Hall: %5$s. Phases: %6$s.",
        "message.nusmp.mega.decennial.band": "Gate band %1$s of the Underworld Root.",
        "message.nusmp.mega.decennial.core": "Core of %1$s",
        "message.nusmp.mega.decennial.loot_none": "The boss has no %1$s item left to give: every one is already in the world.",
        "message.nusmp.mega.decennial.signature.enrage": "%1$s is enraged!",
    })
    return out


def doc_table(bosses):
    rows = ["# The 200 Decennial bosses (generated)", "",
            "Written by `tools/gen_decennial_roster.py`; do not edit by hand. Level L is floor 10 x L (Holy positive, Evil negative). See `docs/labyrinth/decennial_bosses.md` for the why.", "",
            "| level | floor | id | name | title | hall | archetype | creature | phases | grade | look |", "|---|---|---|---|---|---|---|---|---|---|---|"]
    for level in sorted(bosses):
        b = bosses[level]
        rows.append("| %d | %d | %s | %s | %s | %s | %s | %s | %d | %s | %s |" % (level, level * 10, b["id"], b["name"], b["title"], b["theme"], b["archetype"], b["creature"], len(b["phases"]),
                                                                                  b["loot"]["grade"], b["look"]))
    return "\n".join(rows) + "\n"


def main():
    check = "--check" in sys.argv
    bosses, problems = build()
    checks(bosses, problems)
    if problems:
        for p in problems:
            print("PROBLEM:", p)
        sys.exit(1)
    files = {}
    for level, b in bosses.items():
        files[os.path.join(OUT_DIR, "%s.json" % b["id"])] = to_json(b)
    files[LANG_OUT] = json.dumps(lang_fragment(bosses), indent=2, ensure_ascii=False) + "\n"
    files[DOC_OUT] = doc_table(bosses)
    bad = 0
    for path, text in files.items():
        if check:
            if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
                print("differs:", path)
                bad += 1
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
    if check:
        # stale files
        have = set(os.listdir(OUT_DIR)) if os.path.isdir(OUT_DIR) else set()
        want = set(os.path.basename(p) for p in files if p.startswith(OUT_DIR))
        for extra in sorted(have - want):
            print("stale:", extra)
            bad += 1
        sys.exit(1 if bad else 0)
    print("wrote %d bosses (%d holy, %d evil), %d lang keys" % (len(bosses), sum(1 for b in bosses.values() if b["side"] == "holy"), sum(1 for b in bosses.values() if b["side"] == "evil"),
                                                              len(json.loads(files[LANG_OUT]))))


if __name__ == "__main__":
    main()
