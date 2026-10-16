"""Helpers shared by the advancement and quest specs.

Why: the specs (spec_*.py) are long tables of data; everything that is not data (criterion builders, the id hash, the item and entity
vocabulary, the SNBT writer) lives here once so the tables stay short and a rule ("an id never changes") has exactly one home.
"""
import hashlib
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
MOD = "nusmp"


# ---------------------------------------------------------------------------------------------------------------- ids
def hid(key):
    """A stable 63-bit id for a stable key, as FTB Quests' 16 digit upper case hex string.

    Why: quest progress is stored per id, so regenerating the pack must never move an id. A hash of the key does that; the top bit is
    cleared because FTB parses the id with Long.parseLong(hex, 16), which rejects 16 digit values that start with 8..F."""
    n = int(hashlib.sha256(key.encode("utf-8")).hexdigest()[:16], 16) & 0x7FFFFFFFFFFFFFFF
    if n in (0, 1):
        n += 2
    return "%016X" % n


def hid_long(key):
    """The same id as a decimal long (reward table references are written as a long tag)."""
    return int(hid(key), 16)


# -------------------------------------------------------------------------------------------------------- criteria
# A criteria spec is a dict {"mode": "all"|"any", "list": [(name, trigger, conditions), ...]}.
def _crit(mode, items):
    return {"mode": mode, "list": items}


def ev(key, count=1, arg=None, name=None):
    """The mod's own trigger: nusmp:event (key, optional count, optional arg). See QST_TRG.md for the contract."""
    cond = {"key": key}
    if count and count != 1:
        cond["count"] = int(count)
    if arg is not None:
        cond["arg"] = arg
    nm = name or ("e_" + key.replace(".", "_") + ("_" + re.sub(r"[^a-z0-9_]", "_", arg) if arg else "") + (("_%d" % count) if count and count != 1 else ""))
    return _crit("all", [(nm, MOD + ":event", cond)])


def ev_any(*events):
    """Any one of several (key, arg) events: (key,) or (key, arg) or (key, arg, count) tuples."""
    out = []
    for e in events:
        key = e[0]
        arg = e[1] if len(e) > 1 else None
        count = e[2] if len(e) > 2 else 1
        out.extend(ev(key, count, arg)["list"])
    return _crit("any", out)


def ev_all(*events):
    """All of several events."""
    out = []
    for e in events:
        key = e[0]
        arg = e[1] if len(e) > 1 else None
        count = e[2] if len(e) > 2 else 1
        out.extend(ev(key, count, arg)["list"])
    return _crit("all", out)


def tick():
    return _crit("all", [("tick", "minecraft:tick", {})])


def impossible():
    return _crit("all", [("impossible", "minecraft:impossible", {})])


def _item_cond(item):
    return {"items": [{"items": item}]}


def have(*items, mode="all"):
    """Carry the item (inventory_changed). Items are 'nusmp:id' or 'minecraft:id'; mode 'all' needs every one, 'any' needs one."""
    out = []
    for it in items:
        nm = "have_" + it.split(":")[1]
        out.append((nm, "minecraft:inventory_changed", _item_cond(it)))
    return _crit(mode, out)


def killed(*entities, mode="any"):
    """Kill the entity type (vanilla trigger, the player must be the killer)."""
    out = []
    for e in entities:
        out.append(("killed_" + e.split(":")[1], "minecraft:player_killed_entity",
                    {"entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": e}}]}))
    return _crit(mode, out)


def boss_kill(path):
    """Defeat one of this mod's bosses: the event (squad credit is shared by the Java side) OR the vanilla kill, whichever happens."""
    c = ev("boss.defeated", 1, path, name="event")["list"] + killed("%s:%s" % (MOD, path))["list"]
    return _crit("any", [(n if n == "event" else "kill", t, cc) for n, t, cc in c])


def dimension(to):
    return _crit("all", [("entered", "minecraft:changed_dimension", {"to": to})])


def talk(entity):
    return _crit("all", [("talked", "minecraft:player_interacted_with_entity",
                          {"entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]})])


def effect(eff):
    return _crit("all", [("afflicted", "minecraft:effects_changed", {"effects": {eff: {}}})])


def consume(item):
    return _crit("all", [("consumed", "minecraft:consume_item", {"item": {"items": item}})])


def placed(block):
    return _crit("all", [("placed", "minecraft:placed_block",
                          {"location": [{"condition": "minecraft:block_state_property", "block": block}]})])


# --------------------------------------------------------------------------------------------------- advancement
class Adv:
    """One advancement of the spec. Fields mirror the JSON; `path` is the id below nusmp: ('start/first_grimoire')."""
    def __init__(self, path, parent, title, desc, icon, crit, frame="task", xp=0, hidden=False, toast=True, chat=None,
                 background=None, tag=None):
        self.path = path
        self.parent = parent
        self.title = title
        self.desc = desc
        self.icon = icon
        self.crit = crit
        self.frame = frame
        self.xp = xp
        self.hidden = hidden
        self.toast = toast
        self.chat = (frame in ("goal", "challenge")) if chat is None else chat
        self.background = background
        self.tag = tag
        self.future = False

    @property
    def id(self):
        return "%s:%s" % (MOD, self.path)

    @property
    def lang_base(self):
        return "advancement.%s.%s" % (MOD, self.path.replace("/", "."))


class Spec:
    """Collector with small builders. Order of `advs` is the file-write order (stable output)."""
    def __init__(self):
        self.advs = []
        self.by_path = {}

    def add(self, *a, **kw):
        adv = Adv(*a, **kw)
        if adv.path in self.by_path:
            raise ValueError("duplicate advancement " + adv.path)
        self.advs.append(adv)
        self.by_path[adv.path] = adv
        return adv

    def has(self, path):
        return path in self.by_path


def slug(s):
    return re.sub(r"[^a-z0-9_]+", "_", s.lower()).strip("_")


# ------------------------------------------------------------------------------------------------------ vocabulary
def known_items():
    """Every nusmp item or block id the mod ships: the lang file keys (item.nusmp.x / block.nusmp.x) with a models/item json."""
    lang = json.load(open(os.path.join(RES, "assets", MOD, "lang", "en_us.json"), encoding="utf-8"))
    models = {f[:-5] for f in os.listdir(os.path.join(RES, "assets", MOD, "models", "item"))}
    ids = set()
    for k in lang:
        m = re.match(r"^(?:item|block)\.%s\.([a-z0-9_]+)$" % MOD, k)
        if m and m.group(1) in models:
            ids.add(m.group(1))
    return ids


VANILLA_ICONS = {
    "diamond", "emerald", "gold_ingot", "iron_ingot", "netherite_ingot", "netherite_scrap", "nether_star", "ender_pearl", "ender_eye",
    "book", "enchanted_book", "writable_book", "written_book", "experience_bottle", "golden_apple", "enchanted_golden_apple",
    "totem_of_undying", "compass", "clock", "map", "filled_map", "recovery_compass", "echo_shard", "amethyst_shard", "lapis_lazuli",
    "redstone", "glowstone_dust", "blaze_powder", "blaze_rod", "ghast_tear", "dragon_breath", "dragon_egg", "heart_of_the_sea",
    "nautilus_shell", "conduit", "beacon", "end_crystal", "shield", "iron_sword", "diamond_sword", "netherite_sword", "bow", "crossbow",
    "trident", "mace", "barrier", "iron_bars", "lectern", "enchanting_table", "anvil", "grindstone", "smithing_table", "brewing_stand",
    "cauldron", "campfire", "bell", "lantern", "soul_lantern", "candle", "white_banner", "red_banner", "black_banner", "feather",
    "string", "spider_eye", "bone", "skeleton_skull", "wither_skeleton_skull", "player_head", "bundle", "name_tag", "lead", "saddle",
    "oak_sapling", "cherry_sapling", "mangrove_propagule", "vine", "mushroom_stem", "red_mushroom", "wheat", "bread", "cake",
    "cooked_beef", "honey_bottle", "potion", "splash_potion", "water_bucket", "lava_bucket", "powder_snow_bucket", "ice", "snowball",
    "sand", "clay_ball", "stone", "cobblestone", "obsidian", "crying_obsidian", "end_stone", "purpur_block", "bedrock", "copper_ingot",
    "raw_iron", "quartz", "prismarine_shard", "prismarine_crystals", "sculk_catalyst", "sculk_shrieker", "ominous_trial_key", "trial_key",
    "wind_charge", "breeze_rod", "heavy_core", "ominous_bottle", "tripwire_hook", "string", "painting", "item_frame", "glass_bottle",
    "ink_sac", "glow_ink_sac", "music_disc_13", "note_block", "jukebox", "goat_horn", "firework_rocket", "tnt", "flint_and_steel",
    "fire_charge", "magma_cream", "slime_ball", "gunpowder", "leather", "rabbit_foot", "phantom_membrane", "turtle_helmet", "elytra",
    "chain", "iron_chestplate", "diamond_chestplate", "netherite_chestplate", "diamond_helmet", "golden_helmet", "chainmail_chestplate",
    "dirt", "grass_block", "oak_log", "crafting_table", "chest", "ender_chest", "shulker_box", "respawn_anchor", "lodestone",
}


def check_icon(item_id, nusmp_known):
    ns, name = item_id.split(":")
    if ns == MOD:
        return name in nusmp_known
    return ns == "minecraft" and name in VANILLA_ICONS


# ------------------------------------------------------------------------------------------------ SNBT writer
_SIMPLE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def q(s):
    """A quoted SNBT string (only the escapes FTB Library's reader knows: backslash and double quote)."""
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


class D(float):
    """A double tag (written with the d suffix)."""


class L(int):
    """A long tag (written with the L suffix)."""


class B(int):
    pass


def _val(v, ind):
    pad = "\t" * ind
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, L):
        return "%dL" % int(v)
    if isinstance(v, D):
        s = repr(float(v))
        if s.endswith(".0"):
            return s + "d"
        return s + "d"
    if isinstance(v, int):
        return str(v)
    if isinstance(v, float):
        return repr(v) + "d"
    if isinstance(v, str):
        return q(v)
    if isinstance(v, dict):
        if not v:
            return "{ }"
        lines = ["{"]
        for k, x in v.items():
            key = k if _SIMPLE.match(k) else q(k)
            lines.append("%s\t%s: %s" % (pad, key, _val(x, ind + 1)))
        lines.append(pad + "}")
        return "\n".join(lines)
    if isinstance(v, (list, tuple)):
        if not v:
            return "[ ]"
        if all(isinstance(x, (str, int, float)) and not isinstance(x, bool) for x in v) and sum(len(str(x)) for x in v) < 70 \
                and not any(isinstance(x, str) and len(x) > 40 for x in v):
            return "[" + ", ".join(_val(x, ind + 1) for x in v).replace(", ", ", ") + "]"
        lines = ["["]
        for x in v:
            lines.append("%s\t%s" % (pad, _val(x, ind + 1)))
        lines.append(pad + "]")
        return "\n".join(lines)
    raise TypeError("cannot write %r" % (v,))


def snbt(d):
    """Serialize a compound dict to FTB style SNBT text (tab indentation, newline separated, unquoted keys)."""
    assert isinstance(d, dict)
    return _val(d, 0) + "\n"
