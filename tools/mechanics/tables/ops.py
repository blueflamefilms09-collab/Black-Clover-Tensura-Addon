"""Why: the catalogue of the effect PRIMITIVES, TRIGGERS and COUNTERS of the dungeon mechanics library, the one place that says which words a mechanic may use,
what arguments each takes (name, type, default, range, allowed values) and which exclusive CHANNEL of the dungeon it occupies (two mechanics that need the same channel are
never stacked: the conflict table). gen_mechanics.py writes it out as data/nusmp/mega/mechanics/ops.json; the Java side (mega/mechanics/OpCatalog) reads that file, validates every
mechanic against it and the in-game self-test checks that every id here has an executor. Add a primitive HERE first, then give it an executor in PrimsPlayer / PrimsWorld / PrimsMob /
PrimsEnv (the self-test fails until both exist)."""

N, S, B = "num", "str", "bool"


def a(name, typ, default, lo=None, hi=None, enum=None):
    d = {"n": name, "t": typ, "d": default}
    if lo is not None: d["lo"] = lo
    if hi is not None: d["hi"] = hi
    if enum: d["e"] = enum
    return d


AT = ["auto", "anchor", "player", "room"]
WHO = ["players", "mobs", "all"]
SHAPES = ["ring", "disc", "cross", "scatter", "pillars", "walls", "line", "grid", "square"]

# id -> dict(group, channels, sustained, args)
OPS = {}


def op(id, group, args, channels=(), sustained=False, doc=""):
    assert id not in OPS, id
    OPS[id] = {"id": id, "group": group, "channels": list(channels), "sustained": sustained, "args": args, "doc": doc}


# ------------------------------------------------------------------ creatures: players (or mobs / all with `who`)
op("effect", "creature", [a("id", S, "minecraft:slowness"), a("secs", N, 5, 0.5, 600), a("amp", N, 0, 0, 9), a("who", S, "players", enum=WHO)], doc="apply a mob effect")
op("damage", "creature", [a("amount", N, 2, 0, 400), a("pct", N, 0, 0, 90), a("kind", S, "magic", enum=["magic", "fire", "freeze", "fall", "drown", "wither", "generic", "sting", "lightning"]), a("who", S, "players", enum=WHO)], doc="hurt")
op("heal", "creature", [a("amount", N, 4, 0, 400), a("pct", N, 0, 0, 100), a("who", S, "players", enum=WHO)], doc="heal")
op("knockback", "creature", [a("power", N, 1.0, 0, 5), a("up", N, 0.3, 0, 2), a("from", S, "anchor", enum=["anchor", "random", "player"]), a("who", S, "players", enum=WHO)], doc="push away")
op("pull", "creature", [a("power", N, 0.8, 0, 4), a("who", S, "players", enum=WHO)], doc="drag towards the anchor")
op("launch", "creature", [a("power", N, 1.0, 0, 4), a("who", S, "players", enum=WHO)], doc="throw upward")
op("dash", "creature", [a("power", N, 1.0, 0, 4), a("dir", S, "random", enum=["random", "toward", "away", "north", "south", "east", "west"]), a("who", S, "players", enum=WHO)], doc="a sideways shove")
op("ignite", "creature", [a("secs", N, 4, 0.5, 30), a("who", S, "players", enum=WHO)], doc="set on fire")
op("freeze", "creature", [a("secs", N, 5, 0.5, 30), a("who", S, "players", enum=WHO)], doc="powder-snow chill")
op("starve", "creature", [a("amount", N, 4, 0, 20)], doc="drain a small share of magicules (the hunger ban: never the food bar)")
op("drain_mana", "creature", [a("pct", N, 10, 0, 100)], channels=["mana"], doc="drain magicules")
op("restore_mana", "creature", [a("pct", N, 20, 0, 100)], doc="restore magicules")
op("xp", "creature", [a("levels", N, -1, -30, 30)], doc="give or take experience levels")
op("gravity", "creature", [a("mode", S, "up", enum=["up", "heavy", "float", "spin"]), a("secs", N, 4, 0.5, 60), a("who", S, "players", enum=WHO)], channels=["gravity"], sustained=True, doc="flip or weigh gravity")
op("root", "creature", [a("secs", N, 3, 0.5, 30), a("who", S, "players", enum=WHO)], channels=["movement"], doc="hold in place")
op("tp_random", "creature", [a("radius", N, 6, 1, 40), a("who", S, "players", enum=WHO)], channels=["teleport"], doc="blink to a random safe spot of the area")
op("tp_anchor", "creature", [a("who", S, "players", enum=WHO)], channels=["teleport"], doc="to the anchor")
op("tp_swap", "creature", [], channels=["teleport"], doc="two players trade places")
op("tp_entry", "creature", [], channels=["teleport"], doc="back to where they came in")
op("tp_room", "creature", [a("mode", S, "random", enum=["random", "next", "prev", "first", "last"])], channels=["teleport"], doc="to another room of the floor")
op("mark", "creature", [a("secs", N, 10, 1, 120), a("who", S, "players", enum=WHO)], doc="glowing")
op("shuffle_hotbar", "creature", [], doc="swap two hotbar slots")
op("item_cooldown", "creature", [a("secs", N, 6, 0.5, 60)], doc="every item on cooldown")
op("curse_hand", "creature", [a("secs", N, 20, 1, 300), a("dmg", N, 1.0, 0, 20)], channels=["curse"], sustained=True, doc="attacking hurts the attacker")
op("take_item", "creature", [a("item", S, "minecraft:gold_nugget"), a("count", N, 1, 1, 64)], doc="remove items from the inventory")
op("give_item", "creature", [a("item", S, "minecraft:gold_nugget"), a("count", N, 1, 1, 64)], doc="give items")
op("give_loot", "creature", [a("table", S, "nusmp:chests/labyrinth_trial"), a("rolls", N, 1, 1, 4)], doc="roll a loot table into the inventory")
op("xp_orbs", "creature", [a("amount", N, 20, 1, 2000)], doc="drop experience")
op("burden", "creature", [a("secs", N, 6, 1, 120), a("per", N, 6, 1, 36)], channels=["movement"], sustained=True, doc="slowness by how full the inventory is")
op("speed", "creature", [a("factor", N, -0.4, -0.9, 2.0), a("secs", N, 6, 1, 120), a("who", S, "players", enum=WHO)], channels=["time"], sustained=True, doc="walk speed modifier (time dilation)")
op("disorient", "creature", [], doc="spin the view")
op("link", "creature", [a("radius", N, 8, 2, 30), a("dmg", N, 1.0, 0, 20), a("secs", N, 20, 1, 300)], channels=["tether"], sustained=True, doc="players tied together")
op("share_damage", "creature", [a("pct", N, 50, 10, 100), a("secs", N, 20, 1, 300)], channels=["burden"], sustained=True, doc="a blow is shared by the party")
op("downed", "creature", [a("secs", N, 12, 3, 60)], channels=["revive"], sustained=True, doc="a lethal blow leaves a downed player a mate can lift")
op("no_regen", "creature", [a("secs", N, 20, 1, 300)], channels=["regen"], sustained=True, doc="no natural healing")

# ------------------------------------------------------------------ world: blocks (every change goes through a journal and is restored)
op("place", "world", [a("block", S, "minecraft:stone"), a("shape", S, "ring", enum=SHAPES), a("radius", N, 4, 1, 12), a("height", N, 1, 1, 6), a("count", N, 12, 1, 80), a("at", S, "auto", enum=AT), a("secs", N, 0, 0, 600)], doc="place blocks into free air")
op("floor_swap", "world", [a("block", S, "minecraft:magma_block"), a("shape", S, "disc", enum=["disc", "ring", "checker", "scatter", "room"]), a("radius", N, 3, 1, 12), a("at", S, "auto", enum=AT), a("secs", N, 0, 0, 600)], channels=["floor"], doc="replace the floor layer")
op("pit", "world", [a("radius", N, 2, 1, 6), a("depth", N, 1, 1, 2), a("at", S, "auto", enum=AT), a("secs", N, 8, 1, 120)], channels=["floor"], doc="open a shallow pit")
op("flood", "world", [a("height", N, 2, 1, 8), a("block", S, "minecraft:water"), a("at", S, "room", enum=AT), a("secs", N, 10, 1, 300)], channels=["water"], doc="raise liquid over the floor")
op("cage", "world", [a("radius", N, 3, 1, 8), a("height", N, 3, 2, 6), a("block", S, "minecraft:iron_bars"), a("at", S, "auto", enum=AT), a("secs", N, 0, 0, 300)], channels=["cage"], doc="bars round the party")
op("seal_doors", "world", [a("block", S, "minecraft:iron_bars"), a("secs", N, 0, 0, 600)], channels=["doors"], doc="bar every doorway of the room")
op("unseal", "world", [], doc="lift the seal and cages of this mechanic")
op("lights_out", "world", [a("radius", N, 10, 2, 30), a("at", S, "auto", enum=AT), a("secs", N, 0, 0, 600)], channels=["light"], doc="snuff the light sources")
op("lights_on", "world", [a("count", N, 6, 1, 30), a("level", N, 15, 1, 15), a("at", S, "auto", enum=AT), a("secs", N, 0, 0, 600)], channels=["light"], doc="invisible light blocks")
op("wall_blink", "world", [a("block", S, "minecraft:stone_bricks"), a("period", N, 40, 10, 400), a("radius", N, 4, 2, 10), a("secs", N, 30, 2, 300), a("at", S, "auto", enum=AT)], channels=["walls"], sustained=True, doc="walls that come and go")
op("slider", "world", [a("block", S, "minecraft:smooth_stone_slab"), a("axis", S, "x", enum=["x", "z", "y"]), a("range", N, 6, 2, 14), a("period", N, 10, 2, 60), a("radius", N, 2, 1, 5), a("secs", N, 30, 2, 300), a("at", S, "auto", enum=AT)], channels=["platform"], sustained=True, doc="a platform that travels")
op("ceiling", "world", [a("block", S, "minecraft:stone"), a("period", N, 12, 2, 60), a("radius", N, 4, 2, 9), a("secs", N, 12, 2, 120), a("dmg", N, 2, 0, 60), a("at", S, "auto", enum=AT)], channels=["ceiling"], sustained=True, doc="a ceiling that comes down and goes up again")
op("trail", "world", [a("block", S, "minecraft:glass"), a("life", N, 40, 5, 400), a("secs", N, 20, 1, 300)], channels=["trail"], sustained=True, doc="a bridge that forms under the feet and fades")
op("prop", "world", [a("kind", S, "pedestal", enum=["plate", "lever", "pedestal", "lectern", "button", "chest"]), a("count", N, 3, 1, 8), a("at", S, "auto", enum=AT)], doc="interactive props of this mechanic")

# ------------------------------------------------------------------ mobs
GROUPS = ["undead", "beast", "construct", "fiend", "arcane", "swarm", "elemental", "knight"]
op("spawn_group", "mob", [a("group", S, "undead", enum=GROUPS), a("count", N, 3, 1, 24), a("ring", N, 6, 2, 20), a("hunt", B, True), a("hp", N, 1.0, 0.5, 6)], channels=["summon"], doc="a band of monsters round the anchor")
op("spawn_ambush", "mob", [a("group", S, "undead", enum=GROUPS), a("count", N, 3, 1, 24), a("hp", N, 1.0, 0.5, 6)], channels=["summon"], doc="monsters from the dark edge of the room")
op("spawn_guard", "mob", [a("group", S, "construct", enum=GROUPS), a("count", N, 2, 1, 12), a("still", B, True), a("hp", N, 1.5, 0.5, 8)], channels=["summon"], doc="guards that hold their post")
op("spawn_swarm", "mob", [a("kind", S, "silverfish", enum=["silverfish", "endermite", "vex", "bee", "cave_spider"]), a("count", N, 6, 1, 30)], channels=["summon"], doc="a swarm of small things")
op("spawn_elite", "mob", [a("group", S, "knight", enum=GROUPS), a("hp", N, 3.0, 1, 12)], channels=["summon"], doc="one big monster")
op("spawn_chest", "world", [a("loot", S, "nusmp:chests/labyrinth_trial"), a("mimic", N, 0.3, 0, 1), a("cursed", B, False), a("at", S, "auto", enum=AT)], doc="a chest, maybe a mimic, maybe cursed")
op("buff_mobs", "mob", [a("id", S, "minecraft:strength"), a("secs", N, 10, 1, 300), a("amp", N, 0, 0, 5)], doc="effect on the monsters of the area")
op("shield_mobs", "mob", [a("secs", N, 0, 0, 600)], channels=["shield"], doc="monsters take no damage until unshielded")
op("unshield", "mob", [], doc="lift the shield")
op("kill_mobs", "mob", [], doc="remove the mechanic's monsters")
op("heal_mobs", "mob", [a("pct", N, 50, 1, 100)], doc="heal the monsters")
op("enrage", "mob", [a("secs", N, 15, 1, 120), a("speed", N, 0.3, 0, 1.5)], doc="faster and stronger")
op("tether", "mob", [a("pct", N, 80, 10, 95)], channels=["shield"], sustained=True, doc="followers take less damage while their leader lives")
op("reflect", "mob", [a("pct", N, 30, 5, 100), a("secs", N, 20, 1, 300)], channels=["reflect"], sustained=True, doc="blows are returned to the attacker")
op("summon_wave", "mob", [a("group", S, "undead", enum=GROUPS), a("count", N, 3, 1, 12), a("waves", N, 3, 1, 8), a("period", N, 200, 40, 1200)], channels=["summon"], sustained=True, doc="waves on a timer")
op("split_on_death", "mob", [a("count", N, 2, 1, 4)], sustained=True, doc="a slain monster leaves smaller ones")
op("swap_mob", "mob", [], channels=["teleport"], doc="a monster trades places with a player")
op("doppelganger", "mob", [a("count", N, 1, 1, 4)], channels=["summon"], doc="a hostile double of a player")
op("call_dm", "mob", [], channels=["summon"], doc="ask the Dungeon Master for an ambush")

# ------------------------------------------------------------------ environment and feedback
op("weather", "env", [a("mode", S, "rain", enum=["rain", "thunder", "clear"]), a("secs", N, 20, 2, 300)], channels=["weather"], sustained=True, doc="weather for the players of the area only")
op("lightning", "env", [a("count", N, 2, 1, 8), a("dmg", N, 4, 0, 60), a("spread", N, 5, 1, 16)], doc="visual bolts that hurt")
op("projectile", "env", [a("kind", S, "arrow", enum=["arrow", "snowball", "small_fireball", "spectral_arrow", "egg"]), a("count", N, 3, 1, 16), a("speed", N, 1.2, 0.3, 3), a("dmg", N, 2, 0, 60)], doc="shots from the walls")
op("shockwave", "env", [a("radius", N, 6, 2, 16), a("dmg", N, 3, 0, 60), a("power", N, 0.8, 0, 3), a("secs", N, 2, 0.5, 8)], sustained=True, doc="a ring that runs outward")
op("blast", "env", [a("radius", N, 4, 1, 12), a("dmg", N, 4, 0, 80), a("power", N, 1.2, 0, 4)], doc="a burst with no block damage")
op("wind", "env", [a("dir", S, "north", enum=["north", "south", "east", "west", "up", "spiral"]), a("power", N, 0.12, 0.02, 0.6), a("secs", N, 6, 1, 120)], channels=["wind"], sustained=True, doc="a steady push")
op("quake", "env", [a("secs", N, 4, 1, 30), a("dmg", N, 0, 0, 20)], sustained=True, doc="the room shakes")
op("rukh", "env", [a("align", S, "auto", enum=["auto", "light", "dark"]), a("shape", S, "burst", enum=["burst", "ring", "column", "aura"]), a("count", N, 10, 1, 12)], doc="Rukh particles")
op("sound", "env", [a("id", S, "minecraft:block.beacon.ambient"), a("vol", N, 1.0, 0, 4), a("pitch", N, 1.0, 0.2, 2)], doc="a sound")
op("title", "env", [a("text", S, ""), a("mode", S, "actionbar", enum=["actionbar", "title", "chat"])], doc="a message to the players of the area")
op("counter_add", "env", [a("name", S, "count"), a("n", N, 1, -100, 100)], doc="count something for the counter / trigger")
op("reveal", "env", [a("secs", N, 6, 1, 60)], doc="show the props of the mechanic")

# ------------------------------------------------------------------ triggers (what sets a mechanic off)
TRIGGERS = {
    "floor_enter": {"args": [], "doc": "once, when the first player steps into the mechanic's area"},
    "area_enter": {"args": [a("cooldown", N, 200, 20, 6000)], "doc": "every time a player comes into the area"},
    "timer": {"args": [a("every", N, 200, 20, 6000)], "doc": "every N ticks while a player is inside"},
    "proximity": {"args": [a("r", N, 4, 1, 16), a("cooldown", N, 100, 10, 6000)], "doc": "a player within r blocks of the anchor"},
    "sight": {"args": [a("range", N, 14, 3, 40), a("cooldown", N, 100, 10, 6000)], "doc": "a player looks at the anchor"},
    "kill": {"args": [a("n", N, 3, 1, 30)], "doc": "n monsters die in the area"},
    "hp_below": {"args": [a("frac", N, 0.5, 0.05, 0.95), a("cooldown", N, 200, 20, 6000)], "doc": "a player under a fraction of their health"},
    "item_use": {"args": [a("item", S, "any"), a("cooldown", N, 100, 10, 6000)], "doc": "a player uses an item (any, food, light, weapon, magic, or an id)"},
    "interact": {"args": [a("cooldown", N, 60, 5, 6000)], "doc": "a player uses a prop block of the mechanic"},
    "damage_taken": {"args": [a("min", N, 1, 0, 40), a("cooldown", N, 60, 5, 6000)], "doc": "a player is hurt in the area"},
    "sprint": {"args": [a("ticks", N, 40, 5, 400), a("cooldown", N, 100, 10, 6000)], "doc": "a player sprints for a while"},
    "idle": {"args": [a("ticks", N, 100, 20, 1200), a("cooldown", N, 100, 10, 6000)], "doc": "a player stands still for a while"},
    "sneak": {"args": [a("ticks", N, 60, 10, 600), a("cooldown", N, 100, 10, 6000)], "doc": "a player sneaks for a while"},
    "jump": {"args": [a("n", N, 3, 1, 20), a("cooldown", N, 100, 10, 6000)], "doc": "a player jumps n times"},
    "boss_hp": {"args": [a("frac", N, 0.5, 0.05, 0.95)], "doc": "the boss of the arena falls under a fraction of its health"},
    "party_size": {"args": [a("n", N, 2, 2, 8), a("cooldown", N, 200, 20, 6000)], "doc": "n or more players in the area"},
    "death": {"args": [a("cooldown", N, 100, 10, 6000)], "doc": "a player dies in the area"},
    "dark": {"args": [a("light", N, 4, 0, 14), a("ticks", N, 60, 10, 600), a("cooldown", N, 100, 10, 6000)], "doc": "a player stays in the dark"},
}

# ------------------------------------------------------------------ counters (how a player beats or disarms it)
COUNTERS = {
    "none": {"args": [], "props": "none", "doc": "cannot be beaten: it must be survived or left behind"},
    "kill_all": {"args": [a("group", S, "undead", enum=GROUPS), a("count", N, 3, 1, 12)], "props": "mobs", "doc": "kill every warden the mechanic spawns"},
    "kill_marked": {"args": [a("group", S, "knight", enum=GROUPS)], "props": "mobs", "doc": "kill the one marked warden"},
    "item_offer": {"args": [a("item", S, "minecraft:gold_ingot"), a("count", N, 1, 1, 16)], "props": "pedestal", "doc": "give an item to the altar"},
    "stand_plate": {"args": [a("secs", N, 6, 1, 60)], "props": "plate", "doc": "stand on the plate for a while"},
    "sneak_through": {"args": [a("secs", N, 8, 2, 60)], "props": "none", "doc": "sneak for a while in the area"},
    "hold_light": {"args": [a("secs", N, 8, 2, 60)], "props": "none", "doc": "carry a light for a while"},
    "sequence": {"args": [a("steps", N, 4, 2, 8)], "props": "pedestal", "doc": "use the props in the right order"},
    "lever": {"args": [], "props": "lever", "doc": "pull the lever"},
    "survive": {"args": [a("secs", N, 30, 5, 300)], "props": "none", "doc": "last out the time"},
    "all_present": {"args": [a("secs", N, 4, 1, 60)], "props": "plate", "doc": "every living party member in the area at once"},
    "split_plates": {"args": [a("n", N, 2, 2, 6)], "props": "plate", "doc": "n different players on n plates together"},
    "carry": {"args": [a("item", S, "minecraft:amethyst_shard")], "props": "pedestal", "doc": "carry the item from one pedestal to the other"},
    "no_damage": {"args": [a("secs", N, 20, 5, 120)], "props": "none", "doc": "take no damage for a while"},
    "quiet": {"args": [a("secs", N, 15, 3, 120)], "props": "none", "doc": "neither sprint nor jump for a while"},
    "pay_xp": {"args": [a("levels", N, 3, 1, 30)], "props": "pedestal", "doc": "pay experience levels at the altar"},
    "pay_hp": {"args": [a("hearts", N, 3, 1, 10)], "props": "pedestal", "doc": "pay health at the altar"},
    "pay_item": {"args": [a("item", S, "minecraft:iron_ingot"), a("count", N, 4, 1, 32)], "props": "pedestal", "doc": "pay a price in items"},
    "pillars_sync": {"args": [a("secs", N, 4, 1, 20), a("steps", N, 3, 2, 6)], "props": "pedestal", "doc": "light every pillar within seconds of each other"},
    "mirror": {"args": [a("steps", N, 3, 2, 6)], "props": "pedestal", "doc": "repeat a shown pattern"},
}

CHANNEL_EXCLUSIVE = ["gravity", "light", "water", "weather", "time", "doors", "shield", "reflect", "revive", "burden", "tether", "mana", "regen", "wind", "curse", "walls", "platform", "ceiling", "cage", "trail", "floor", "movement"]
EFFECT_CHANNELS = {
    "minecraft:darkness": "vision", "minecraft:blindness": "vision",
    "minecraft:levitation": "gravity", "minecraft:slow_falling": "gravity",
    "minecraft:slowness": "movement", "minecraft:speed": "time", "minecraft:haste": "time", "minecraft:mining_fatigue": "time",
    "minecraft:hunger": "burden", "minecraft:nausea": "vision",
}
# channels two mechanics may not share, even when only one of them has the effect: exclusive list above + vision
CHANNEL_EXCLUSIVE.append("vision")
