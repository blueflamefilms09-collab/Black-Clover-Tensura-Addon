#!/usr/bin/env python3
"""Generates the shipped data of the records system: data/nusmp/elite/records/*.json, titles/*.json and titles/synergies/extra.json.

Why a generator: 200 records and 80 titles follow a few series (a ladder of kill counts, of EP values, of distances ...), and the titles refer to record ids; writing the
JSON by hand would let an id drift. The generator is the single source; the JSON files it writes are what the game reads (and what a datapack can override). The names
and the numbers are this mod's own design (the original addon's lists are not available). Run from the repository root:  python tools/elite/gen_records_data.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "data", "nusmp", "elite")

# ------------------------------------------------------------------ helpers
RECORDS = {}   # category -> list of dicts
IDS = set()


def fmt(n):
    if n >= 1_000_000_000:
        return ("%g" % (n / 1e9)) + " billion"
    if n >= 1_000_000:
        return ("%g" % (n / 1e6)) + " million"
    return "{:,}".format(int(n)) if float(n).is_integer() else str(n)


def rec(cat, rid, name, flavor, rarity, cond, objective, hidden=False):
    assert rid not in IDS, rid
    IDS.add(rid)
    desc = (flavor + " " if flavor else "") + objective
    RECORDS.setdefault(cat, []).append({
        "id": rid, "name": name, "description": desc, "category": cat, "rarity": rarity, "hidden": hidden, "condition": cond,
    })


def km(m):
    return fmt(m // 1000) + " km" if m >= 1000 else fmt(m) + " m"


# ------------------------------------------------------------------ EXPLORATION
BIOMES = [
    ("plains", "Open Fields", "The wind carries nothing but grass.", "common"),
    ("desert", "Sands Underfoot", "Every step is a little deeper.", "common"),
    ("swamp", "Mire Wader", "The water is warmer than it should be.", "common"),
    ("taiga", "Pine Breath", "Needles, snow and woodsmoke.", "common"),
    ("savanna", "Golden Grass", "Acacia shade is the only roof.", "common"),
    ("jungle", "Canopy Climber", "Light comes down in coins.", "uncommon"),
    ("badlands", "Red Mesa", "Layers of old sea floor, baked red.", "uncommon"),
    ("cherry_grove", "Petal Storm", "Pink snow that never melts.", "uncommon"),
    ("lush_caves", "Hidden Garden", "A garden under the stone.", "uncommon"),
    ("warped_forest", "Warped Walker", "A forest that grew on the wrong side of the world.", "uncommon"),
    ("soul_sand_valley", "Valley of Souls", "The ground whispers back.", "uncommon"),
    ("ice_spikes", "Spiked Frost", "A frozen field of blades.", "rare"),
    ("mushroom_fields", "Fungal Isle", "No monster has ever walked here.", "rare"),
    ("basalt_deltas", "Basalt Pilgrim", "Ash falls like slow rain.", "rare"),
    ("end_highlands", "Beyond the Void", "Land floating over nothing.", "rare"),
    ("deep_dark", "Where Light Fears", "Something here is listening.", "epic"),
]
for b, name, fl, rar in BIOMES:
    rec("exploration", "biome_" + b, name, fl, rar, {"type": "biome_discovery", "biome": "minecraft:" + b}, "Discover the %s biome." % b.replace("_", " "))
for n, name, fl, rar in [(10, "Ten Horizons", "A short list, but a start.", "uncommon"), (25, "Atlas Pages", "Your map has begun to fill.", "rare"),
                         (40, "Cartographer's Dream", "You know the world by its colours.", "epic"), (60, "Every Corner", "There is nowhere left to be a stranger.", "legendary")]:
    rec("exploration", "biomes_%d" % n, name, fl, rar, {"type": "biome_discovery", "count": n}, "Discover %d different biomes." % n)

STRUCTS = [
    ("village", "minecraft:village_*", "Hearth and Hammer", "Smoke from a chimney, finally.", "common"),
    ("shipwreck", "minecraft:shipwreck*", "Wreck Diver", "Someone else's last voyage.", "common"),
    ("ruined_portal", "minecraft:ruined_portal*", "Broken Gate", "A door to somewhere, left ajar.", "common"),
    ("mineshaft", "minecraft:mineshaft*", "Old Timbers", "Rails run into the dark.", "common"),
    ("desert_pyramid", "minecraft:desert_pyramid", "Sun-Baked Tomb", "The trap under the pressure plate is patient.", "uncommon"),
    ("jungle_pyramid", "minecraft:jungle_pyramid", "Moss and Levers", "Puzzles built by people who hated visitors.", "uncommon"),
    ("pillager_outpost", "minecraft:pillager_outpost", "Raiders' Watch", "Banners and crossbows.", "uncommon"),
    ("igloo", "minecraft:igloo", "Cold Comfort", "A bed in the snow.", "uncommon"),
    ("swamp_hut", "minecraft:swamp_hut", "Hag's Cottage", "The cauldron is still warm.", "uncommon"),
    ("trail_ruins", "minecraft:trail_ruins", "Trail of the Buried", "Pottery shards and old brushes.", "rare"),
    ("monument", "minecraft:monument", "Drowned Throne", "Guardians keep watch under the tide.", "rare"),
    ("fortress", "minecraft:fortress", "Nether Bastion", "A fortress of bricks older than the fire.", "rare"),
    ("bastion_remnant", "minecraft:bastion_remnant", "Gold Hoard", "Piglins guard their shine.", "rare"),
    ("mansion", "minecraft:mansion", "Woodland Manor", "The invitation was a mistake.", "epic"),
    ("end_city", "minecraft:end_city", "City at the Edge", "Purple towers and a ship in the sky.", "epic"),
    ("ancient_city", "minecraft:ancient_city", "Sunken Silence", "Do not make a sound.", "epic"),
    ("trial_chambers", "minecraft:trial_chambers", "Trialgoer", "The spawner counts every mistake.", "epic"),
    ("stronghold", "minecraft:stronghold", "Gate of the End", "A ring of eyes, a door of fire.", "rare"),
]
for s, pat, name, fl, rar in STRUCTS:
    rec("exploration", "struct_" + s, name, fl, rar, {"type": "structure_discovery", "structure": pat}, "Find a %s." % s.replace("_", " "))
for n, name, fl, rar in [(5, "Ruin Hopper", "Five doors opened.", "common"), (10, "Delver", "Ten places that were not meant to be found.", "uncommon"),
                         (15, "Grave Robber's Almanac", "Fifteen kinds of dark.", "epic"), (20, "Keeper of Ruins", "You have stood in all of them.", "legendary")]:
    rec("exploration", "structs_%d" % n, name, fl, rar, {"type": "structure_discovery", "count": n}, "Find %d different kinds of structure." % n)

DISTS = [("all", 1000, "First Kilometre", "", "common"), ("all", 10000, "Ten Thousand Steps", "", "uncommon"), ("all", 50000, "Long Road", "", "rare"),
         ("all", 100000, "A Hundred Kilometres", "", "epic"), ("all", 500000, "Around the Edge", "", "legendary"), ("all", 1000000, "Further Than the Horizon", "Mile after mile.", "mythic"),
         ("walk", 20000, "Footsore", "", "uncommon"), ("sprint", 25000, "Never Stop Running", "", "rare"), ("swim", 2000, "Strong Swimmer", "", "uncommon"),
         ("boat", 5000, "Boatman", "", "uncommon"), ("fly", 10000, "Skybound", "", "uncommon"), ("elytra", 5000, "On Borrowed Wings", "", "rare"),
         ("elytra", 50000, "Wind Rider", "", "epic"), ("mount", 20000, "Born in the Saddle", "", "rare")]
for mode, m, name, fl, rar in DISTS:
    what = {"all": "in total", "walk": "on foot", "sprint": "sprinting", "swim": "swimming", "boat": "by boat", "fly": "flying", "elytra": "gliding on elytra", "mount": "on a mount"}[mode]
    rec("exploration", "dist_%s_%dk" % (mode, m // 1000), name, fl, rar, {"type": "distance_traveled", "meters": m, "mode": mode}, "Travel %s %s." % (km(m), what))

# ------------------------------------------------------------------ COMBAT
for n, name, rar in [(10, "First Blood", "common"), (100, "Hundred Down", "common"), (500, "Veteran of Many Fights", "uncommon"), (1000, "A Thousand Falls", "rare"),
                     (5000, "Five Thousand Souls", "epic"), (10000, "Ten Thousand Graves", "legendary"), (50000, "The Long Harvest", "mythic")]:
    rec("combat", "kills_%d" % n, name, "", rar, {"type": "kill_count", "count": n, "group": "all"}, "Kill %s creatures." % fmt(n))
rec("combat", "kills_hostile_250", "Monster Season", "", "uncommon", {"type": "kill_count", "count": 250, "group": "hostile"}, "Kill 250 hostile monsters.")
rec("combat", "kills_hostile_2500", "Night Shift", "Dusk to dawn, every night.", "epic", {"type": "kill_count", "count": 2500, "group": "hostile"}, "Kill 2,500 hostile monsters.")
rec("combat", "kills_passive_100", "Provisioner", "", "common", {"type": "kill_count", "count": 100, "group": "passive"}, "Kill 100 passive animals.")
for n, name, fl, rar, hid in [(1, "A Line Crossed", "It is not the same after.", "uncommon", True), (10, "Duellist", "", "rare", False), (50, "Bloodied Name", "", "epic", False)]:
    rec("combat", "kills_player_%d" % n, name, fl, rar, {"type": "kill_count", "count": n, "group": "player"}, "Kill %s other player%s." % (fmt(n), "" if n == 1 else "s"), hid)
for n, name, rar in [(1, "Giant Felled", "uncommon"), (5, "Boss Rush", "rare"), (25, "Raid Boss Regular", "epic")]:
    rec("combat", "kills_boss_%d" % n, name, "", rar, {"type": "kill_count", "count": n, "group": "boss"}, "Kill %s boss%s." % (fmt(n), "" if n == 1 else "es"))
ENT = [("creeper", "Quiet Hiss", 50, "common"), ("zombie", "Grave Pit", 200, "common"), ("skeleton", "Bone Collector", 100, "common"), ("spider", "Web Cutter", 100, "common"),
       ("slime", "Squish", 50, "common"), ("enderman", "Do Not Look", 50, "uncommon"), ("blaze", "Rod Harvest", 50, "uncommon"), ("ghast", "Cry Back", 10, "uncommon"),
       ("wither_skeleton", "Skull Collector", 25, "uncommon"), ("piglin_brute", "Brute Force", 10, "uncommon"), ("evoker", "Totem Thief", 5, "uncommon"),
       ("witch", "Brew Breaker", 25, "uncommon"), ("phantom", "Insomniac", 50, "uncommon"), ("guardian", "Thorns and Spines", 25, "uncommon"),
       ("ravager", "Beast Tamer, Sort Of", 5, "rare"), ("shulker", "Open the Box", 20, "rare"), ("elder_guardian", "Curse Lifted", 1, "rare"),
       ("ender_dragon", "Dragonslayer", 1, "epic"), ("wither", "Withered Away", 1, "epic"), ("warden", "Hush", 1, "legendary")]
for e, name, n, rar in ENT:
    rec("combat", "kill_%s_%d" % (e, n), name, "", rar, {"type": "kill_entity", "entity": "minecraft:" + e, "count": n}, "Kill %s %s%s." % (fmt(n), e.replace("_", " "), "" if n == 1 else "s"))
rec("combat", "kill_ender_dragon_5", "Dragon's Bane", "The End has a revolving door.", "legendary", {"type": "kill_entity", "entity": "minecraft:ender_dragon", "count": 5}, "Kill the Ender Dragon 5 times.", True)
rec("combat", "kill_wither_5", "Five Withers", "", "legendary", {"type": "kill_entity", "entity": "minecraft:wither", "count": 5}, "Kill 5 Withers.", True)
for n, name, rar in [(100, "Local Pest Control", "uncommon"), (1000, "Tensura Hunter", "rare"), (5000, "Scourge of the Other World", "epic")]:
    rec("combat", "kill_tensura_%d" % n, name, "", rar, {"type": "kill_entity", "entity": "tensura:*", "count": n}, "Kill %s creatures of Tensura." % fmt(n))
for n, name, fl, rar in [(20, "A Good Punch", "", "common"), (50, "Heavy Hand", "", "uncommon"), (100, "Wall Cracker", "", "rare"), (250, "Armour Breaker", "", "epic"),
                         (500, "Siege Strike", "", "epic"), (1000, "One Blow", "Enough to end a duel.", "legendary"), (5000, "Mountain Splitter", "", "legendary"),
                         (10000, "World Breaker", "Not a number, a fact of nature.", "mythic")]:
    rec("combat", "hit_%d" % n, name, fl, rar, {"type": "single_hit_damage", "damage": n}, "Deal %s damage in a single hit." % fmt(n))

# ------------------------------------------------------------------ SURVIVAL
for n, name, fl, rar in [(1, "It Happens", "", "common"), (5, "Slow Learner", "", "common"), (10, "Stubborn", "", "uncommon"), (25, "Respawn Regular", "", "uncommon"),
                         (50, "Undying, Mostly", "", "rare"), (100, "Phoenix", "Every time, back up.", "epic"), (250, "Cat With Many Lives", "", "legendary"),
                         (500, "Death's Favourite Customer", "", "mythic")]:
    rec("survival", "deaths_%d" % n, name, fl, rar, {"type": "death_count", "count": n}, "Die %s time%s." % (fmt(n), "" if n == 1 else "s"))

# ------------------------------------------------------------------ POWER
EPS = [(500, "500", "Stirring", "common"), (2000, "2k", "Not Just a Slime", "common"), (10000, "10k", "Awakened Power", "uncommon"), (50000, "50k", "Named Rank", "uncommon"),
       (100000, "100k", "Prodigy", "rare"), (500000, "500k", "Calamity Class", "rare"), (1000000, "1m", "A Million Echoes", "epic"), (5000000, "5m", "Disaster Rank", "epic"),
       (10000000, "10m", "Beyond Rank", "legendary"), (100000000, "100m", "Apex", "legendary"), (1000000000, "1b", "Fills the Sky", "mythic")]
for ep, tag, name, rar in EPS:
    rec("power", "ep_" + tag, name, "", rar, {"type": "ep_reached", "ep": ep}, "Reach %s EP." % fmt(ep))
rec("power", "demon_seed", "Seed of Ruin", "A seed that wants to be a throne.", "rare", {"type": "demon_lord_status", "status": "seed"}, "Carry a Demon Lord seed.")
rec("power", "demon_true", "Demon Lord", "The world has one more nightmare.", "legendary", {"type": "demon_lord_status", "status": "true"}, "Awaken as a true Demon Lord.")
rec("power", "hero_egg", "Hero's Egg", "Something warm is waiting to hatch.", "rare", {"type": "true_hero_status", "status": "egg"}, "Carry a Hero's Egg.")
rec("power", "hero_true", "True Hero", "The world picked you.", "legendary", {"type": "true_hero_status", "status": "true"}, "Awaken as a true Hero.")
rec("power", "higher_spiritual", "Higher Being", "Flesh is a courtesy.", "epic", {"type": "higher_being_status", "status": "spiritual"}, "Become a spiritual higher being.")
rec("power", "higher_divine", "Divine Ascendant", "Not a creature, a principle.", "mythic", {"type": "higher_being_status", "status": "divine"}, "Ascend to a divine race.")

# ------------------------------------------------------------------ SKILLS
for n, name, rar in [(1, "First Skill", "common"), (5, "Five Skills", "common"), (10, "Ten Skills", "uncommon"), (20, "Twenty Skills", "uncommon"), (40, "Skill Collector", "rare"),
                     (75, "Walking Library", "epic"), (150, "Arcane Archive", "legendary")]:
    rec("skills", "skills_%d" % n, name, "", rar, {"type": "skill_learned", "count": n}, "Learn %d skills." % n)
rec("skills", "skills_300", "All Of It", "There is no more room on the shelf.", "mythic", {"type": "skill_learned", "count": 300}, "Learn 300 skills.", True)
SK = [("intrinsic", 1, "Born With It", "common"), ("common", 5, "Common Knowledge", "common"), ("common", 15, "Jack of Trades", "uncommon"), ("resistance", 3, "Thick Skinned", "uncommon"),
      ("resistance", 10, "Nothing Hurts", "rare"), ("extra", 3, "Extra Credit", "uncommon"), ("extra", 10, "Beyond Common", "rare"), ("unique", 1, "One of a Kind", "rare"),
      ("unique", 3, "Trio of the Unique", "epic"), ("unique", 5, "A Gallery of Uniques", "legendary"), ("ultimate", 1, "Ultimate Skill", "legendary"), ("ultimate", 3, "Three Ultimates", "mythic")]
for t, n, name, rar in SK:
    rid = "skill_%s_%d" % (t, n)
    rec("skills", rid, name, "", rar, {"type": "skill_learned", "count": n, "skill_type": t}, "Learn %d %s skill%s." % (n, t, "" if n == 1 else "s"))
rec("skills", "skill_great_sage", "Sage's Voice", "A voice that answers before you ask.", "epic", {"type": "skill_learned", "skill": "tensura:great_sage"}, "Learn Great Sage.")
rec("skills", "skill_demon_lord_haki", "Overlord's Presence", "", "rare", {"type": "skill_learned", "skill": "tensura:demon_lord_haki"}, "Learn Demon Lord Haki.")
rec("skills", "skill_hero_haki", "Hero's Presence", "", "rare", {"type": "skill_learned", "skill": "tensura:hero_haki"}, "Learn Hero Haki.")

# ------------------------------------------------------------------ CRAFTING
QN = ["Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythical"]
QR = ["common", "uncommon", "rare", "epic", "legendary", "mythic"]
QNAME = ["Plain Work", "Honest Work", "Fine Work", "Masterwork", "Legendary Work", "Work of Myth"]
for q in range(1, 7):
    rec("crafting", "forgeq_%d" % q, QNAME[q - 1], "", QR[q - 1], {"type": "forge_quality", "quality": q}, "Forge an item of %s quality." % QN[q - 1])
for n, name, rar in [(1, "First Strike", "common"), (10, "Apprentice's Ten", "common"), (50, "Fifty Anvils", "uncommon"), (100, "Hundred Heats", "uncommon"), (250, "Master's Workload", "rare"),
                     (500, "Forge-Born", "epic"), (1000, "A Thousand Sparks", "legendary")]:
    rec("crafting", "forge_%d" % n, name, "", rar, {"type": "forge_count", "count": n}, "Forge %s item%s." % (fmt(n), "" if n == 1 else "s"))
for q, n, name, rar in [(4, 25, "Quality Over Quantity", "rare"), (5, 10, "Legend Smith", "epic"), (6, 3, "Myth Maker", "legendary")]:
    rec("crafting", "forge_q%d_x%d" % (q, n), name, "", rar, {"type": "forge_count", "count": n, "min_quality": q}, "Forge %d items of %s quality or better." % (n, QN[q - 1]))

# ------------------------------------------------------------------ DOMINION
for n, name, rar in [(1, "A Plot of Your Own", "common"), (9, "Homestead", "common"), (25, "Manor Grounds", "uncommon"), (64, "Small Barony", "uncommon"), (100, "Territory", "rare"),
                     (256, "Duchy", "epic"), (500, "Principality", "epic"), (1000, "Realm", "mythic")]:
    rec("dominion", "ftb_%d" % n, name, "", rar, {"type": "ftb_chunks_claimed", "count": n}, "Hold %s claimed chunk%s with FTB Chunks." % (fmt(n), "" if n == 1 else "s"))

# ------------------------------------------------------------------ WAR
for n, name, rar in [(1, "First Casualty", "common"), (10, "Skirmisher", "common"), (50, "Front Line", "uncommon"), (100, "Veteran Soldier", "uncommon"), (500, "War Machine", "rare"),
                     (1000, "Thousand Banners", "epic"), (5000, "Reaper of Fields", "legendary")]:
    rec("war", "warkills_%d" % n, name, "", rar, {"type": "war_kills", "count": n}, "Make %s kill%s in wars." % (fmt(n), "" if n == 1 else "s"))
for n, name, rar in [(1, "First Victory", "common"), (3, "Winning Streak", "uncommon"), (5, "Battle-Proven", "uncommon"), (10, "Conqueror", "rare"), (25, "Warlord's Ledger", "epic"),
                     (50, "Undefeated", "legendary"), (100, "Age of Banners", "mythic")]:
    rec("war", "warwins_%d" % n, name, "", rar, {"type": "war_wins", "count": n}, "Win %s war%s." % (fmt(n), "" if n == 1 else "s"))
for n, name, rar in [(1, "Invited", "common"), (3, "Familiar Face", "uncommon"), (5, "Council Regular", "rare"), (10, "Ten Nights", "rare"), (25, "Elder of the Feast", "epic"),
                     (50, "Fifty Walpurgis", "legendary")]:
    rec("war", "walp_%d" % n, name, "", rar, {"type": "walpurgis_attended", "count": n}, "Attend %s Walpurgis council%s." % (fmt(n), "" if n == 1 else "s"))

# ------------------------------------------------------------------ CALAMITY
for n, tag, name, rar in [(1000, "1k", "Scratch the Disaster", "uncommon"), (10000, "10k", "Dent in the Calamity", "rare"), (100000, "100k", "Calamity Wound", "rare"),
                          (1000000, "1m", "Million-Point Strike", "epic"), (10000000, "10m", "Ten Million Reasons", "legendary"), (100000000, "100m", "Break the Sky", "mythic")]:
    rec("calamity", "caldmg_" + tag, name, "", rar, {"type": "calamity_damage", "amount": n}, "Deal %s damage to calamities." % fmt(n))
for n, name, rar in [(1, "Calamity Down", "rare"), (3, "Three Disasters", "rare"), (5, "Disaster Hunter", "epic"), (10, "Catastrophe Bane", "epic"), (25, "Weather of the World", "legendary"),
                     (50, "End of Disasters", "mythic")]:
    rec("calamity", "calkill_%d" % n, name, "", rar, {"type": "calamity_kill", "count": n}, "Kill %d calamit%s." % (n, "y" if n == 1 else "ies"))
rec("calamity", "caldmg_secret", "Insult to Injury", "You hit it while it was already down.", "uncommon", {"type": "calamity_damage", "amount": 50}, "Deal 50 damage to a calamity.", True)

# ------------------------------------------------------------------ TITLES
TITLES = []   # list of dicts, per file group


def attr(a, v, op="add_value"):
    return {"attribute": a, "amount": v, "operation": op}


SPEED, LUCK, HEALTH, ATTACK = "minecraft:generic.movement_speed", "minecraft:generic.luck", "minecraft:generic.max_health", "minecraft:generic.attack_damage"
ARMOR, TOUGH, KB, ASPD, ABS = "minecraft:generic.armor", "minecraft:generic.armor_toughness", "minecraft:generic.knockback_resistance", "minecraft:generic.attack_speed", "minecraft:generic.max_absorption"
BREACH, EREACH, FALL, MINING = "minecraft:player.block_interaction_range", "minecraft:player.entity_interaction_range", "minecraft:generic.safe_fall_distance", "minecraft:player.mining_efficiency"
OXY, WATER = "minecraft:generic.oxygen_bonus", "minecraft:generic.water_movement_efficiency"
MAG, AURA, MREG, MAST, LEARN = "tensura:max_magicule", "tensura:max_aura", "tensura:magicule_regeneration_multiplier", "tensura:ability_mastery_gain", "tensura:ability_learning_gain"
B = "add_multiplied_base"


def title(group, tid, name, desc, rarity, tags, reqs, attrs=(), effects=(), stats=None):
    TITLES.append((group, {"id": tid, "name": name, "description": desc, "rarity": rarity, "tags": list(tags), "requirements": list(reqs),
                           "bonuses": {"attributes": list(attrs), "effects": [{"effect": e, "amplifier": a} for e, a in effects], "stats": dict(stats or {})}}))


def R(rid):
    assert rid in IDS, rid
    return {"record": rid}


# explorer
title("explorer", "wanderer", "Wanderer", "You have walked a thousand metres and want more.", "common", ["explorer"], [R("dist_all_1k")], [attr(SPEED, 0.02, B)])
title("explorer", "pathfinder", "Pathfinder", "Ten kilometres of your own tracks.", "uncommon", ["explorer"], [R("dist_all_10k")], [attr(SPEED, 0.03, B)])
title("explorer", "trailblazer", "Trailblazer", "Fifty kilometres, and a trail behind you.", "rare", ["explorer"], [R("dist_all_50k")], [attr(SPEED, 0.04, B), attr(BREACH, 0.5)])
title("explorer", "world_walker", "World Walker", "A hundred kilometres under your boots.", "epic", ["explorer"], [R("dist_all_100k")], [attr(SPEED, 0.05, B), attr(FALL, 2.0)])
title("explorer", "horizon_chaser", "Horizon Chaser", "Five hundred kilometres: the horizon runs from you.", "legendary", ["explorer"], [R("dist_all_500k")], [attr(SPEED, 0.06, B)], [("minecraft:jump_boost", 0)])
title("explorer", "seafarer", "Seafarer", "At home on boat and wave.", "uncommon", ["explorer"], [R("dist_boat_5k"), R("dist_swim_2k")], [attr(OXY, 2.0), attr(WATER, 0.1)])
title("explorer", "biome_scholar", "Biome Scholar", "Ten biomes in your notebook.", "uncommon", ["explorer"], [R("biomes_10")], [attr(LUCK, 0.5)])
title("explorer", "naturalist", "Naturalist", "Twenty-five biomes, catalogued.", "rare", ["explorer"], [R("biomes_25")], [attr(LUCK, 1.0)])
title("explorer", "cartographer", "Cartographer", "Forty biomes: the map is yours.", "epic", ["explorer"], [R("biomes_40")], [attr(LUCK, 1.0)], (), {"xp_bonus_percent": 5})
title("explorer", "ruin_delver", "Ruin Delver", "Five kinds of ruin, five doors opened.", "common", ["explorer"], [R("structs_5")], [attr(BREACH, 0.5)])
title("explorer", "relic_hunter", "Relic Hunter", "Ten kinds of ruin, and what they held.", "rare", ["explorer"], [R("structs_10")], [attr(LUCK, 1.0)], (), {"xp_bonus_percent": 3})
title("explorer", "deep_diver", "Deep Diver", "You walked the Deep Dark and its sunken city and came back.", "epic", ["explorer"], [R("biome_deep_dark"), R("struct_ancient_city")], [attr(LUCK, 0.5)], [("minecraft:night_vision", 0)])
title("explorer", "nether_walker", "Nether Walker", "The Nether's three strangest biomes did not stop you.", "uncommon", ["explorer"], [R("biome_warped_forest"), R("biome_soul_sand_valley"), R("biome_basalt_deltas")], (), [("minecraft:fire_resistance", 0)])
title("explorer", "end_voyager", "End Voyager", "You have been to the city at the edge.", "rare", ["explorer"], [R("struct_end_city")], [attr(FALL, 3.0)])

# combat
title("combat", "brawler", "Brawler", "A hundred creatures know your fist.", "common", ["combat"], [R("kills_100")], [attr(ATTACK, 0.5)])
title("combat", "slayer", "Slayer", "Five hundred kills and a steady hand.", "uncommon", ["combat"], [R("kills_500")], [attr(ATTACK, 1.0)])
title("combat", "monster_hunter", "Monster Hunter", "A thousand kills, a quarter of them monsters.", "rare", ["combat"], [R("kills_1000"), R("kills_hostile_250")], [attr(ATTACK, 1.0), attr(ARMOR, 1.0)])
title("combat", "veteran", "Veteran", "Five thousand fights survived.", "epic", ["combat"], [R("kills_5000")], [attr(ATTACK, 0.05, B), attr(HEALTH, 2.0)])
title("combat", "reaper", "Reaper", "Ten thousand graves, and each one feeds you a little.", "legendary", ["combat"], [R("kills_10000")], [attr(ATTACK, 0.08, B)], (), {"kill_heal": 1})
title("combat", "blade_calamity", "Blade Calamity", "Fifty thousand kills: a disaster with a sword.", "mythic", ["combat"], [R("kills_50000")], [attr(ATTACK, 0.12, B), attr(ASPD, 0.05, B)], (), {"kill_heal": 2})
title("combat", "dragon_slayer", "Dragon Slayer", "The Ender Dragon fell.", "epic", ["combat", "boss"], [R("kill_ender_dragon_1")], [attr(ATTACK, 1.0)], (), {"boss_damage_percent": 5})
title("combat", "wither_bane", "Wither's Bane", "The Wither withered.", "epic", ["combat", "boss"], [R("kill_wither_1")], [attr(ARMOR, 2.0)], (), {"boss_damage_percent": 5})
title("combat", "warden_slayer", "Warden Slayer", "You made the Warden hush.", "legendary", ["combat", "boss"], [R("kill_warden_1")], [attr(HEALTH, 4.0)], (), {"boss_damage_percent": 8})
title("combat", "giant_slayer", "Giant Slayer", "Five bosses, one ravager's worth of contempt.", "rare", ["combat", "boss"], [R("kills_boss_5")], [attr(ATTACK, 0.5)], (), {"boss_damage_percent": 3})
title("combat", "oathbreaker", "Oathbreaker", "Ten duels, none of them fair.", "rare", ["combat"], [R("kills_player_10")], [attr(ASPD, 0.03, B)])
title("combat", "heavy_hitter", "Heavy Hitter", "A single blow of a hundred.", "rare", ["combat"], [R("hit_100")], [attr(ATTACK, 1.0)])
title("combat", "one_blow", "One Blow", "A single blow of a thousand.", "legendary", ["combat"], [R("hit_1000")], [attr(ATTACK, 0.10, B)])
title("combat", "world_breaker", "World Breaker", "A single blow of ten thousand.", "mythic", ["combat"], [R("hit_10000")], [attr(ATTACK, 0.15, B), attr(EREACH, 0.5)])
title("combat", "tensura_hunter", "Tensura Hunter", "A thousand kills among the creatures of the other world.", "rare", ["combat"], [R("kill_tensura_1000")], (), (), {"xp_bonus_percent": 4})

# survival
title("survival", "survivor", "Survivor", "You died once and noticed.", "common", ["survivor"], [R("deaths_1")], [attr(ABS, 2.0)])
title("survival", "tenacious", "Tenacious", "Ten deaths, ten returns.", "uncommon", ["survivor"], [R("deaths_10")], [attr(KB, 0.05)])
title("survival", "undying", "Undying", "Fifty deaths and still here.", "rare", ["survivor"], [R("deaths_50")], [attr(HEALTH, 2.0)])
title("survival", "phoenix", "Phoenix", "A hundred deaths, a hundred dawns.", "epic", ["survivor"], [R("deaths_100")], [attr(HEALTH, 4.0), attr(ARMOR, 1.0)])

# power
title("power", "awakened", "Awakened", "Ten thousand EP: your magicules stir.", "uncommon", ["power"], [R("ep_10k")], [attr(MAG, 0.02, B)])
title("power", "prodigy", "Prodigy", "A hundred thousand EP.", "rare", ["power"], [R("ep_100k")], [attr(MAG, 0.03, B), attr(AURA, 0.03, B)])
title("power", "ascendant", "Ascendant", "A million EP.", "epic", ["power"], [R("ep_1m")], [attr(MAG, 0.04, B), attr(AURA, 0.04, B)])
title("power", "transcendent", "Transcendent", "Ten million EP: you have left the scale.", "legendary", ["power", "ascended"], [R("ep_10m")], [attr(MAG, 0.06, B), attr(AURA, 0.06, B)])
title("power", "apex_being", "Apex Being", "A billion EP: nothing above you is left to measure.", "mythic", ["power", "ascended"], [R("ep_1b")], [attr(MAG, 0.10, B), attr(AURA, 0.10, B)], (), {"ep_gain_percent": 5})
title("power", "demon_lord_seed", "Seed of Ruin", "A Demon Lord seed rests in you.", "rare", ["power", "crown"], [R("demon_seed")], [attr(MAG, 0.03, B)])
title("power", "demon_lord", "Demon Lord", "You wear the crown of the Demon Lords.", "legendary", ["power", "crown"], [R("demon_true")], [attr(MAG, 0.08, B), attr(AURA, 0.04, B)])
title("power", "hero_egg", "Hero's Egg", "A hero waits inside the shell.", "rare", ["power", "crown"], [R("hero_egg")], [attr(AURA, 0.03, B)])
title("power", "true_hero", "True Hero", "The world chose you.", "legendary", ["power", "crown"], [R("hero_true")], [attr(AURA, 0.08, B), attr(MAG, 0.04, B)])
title("power", "higher_being", "Higher Being", "You are more than flesh.", "epic", ["power", "ascended"], [R("higher_spiritual")], [attr(MREG, 0.10, B)])
title("power", "divine_ascendant", "Divine Ascendant", "You are a principle.", "mythic", ["power", "ascended"], [R("higher_divine")], [attr(MREG, 0.15, B), attr(MAG, 0.05, B), attr(AURA, 0.05, B)])

# skills
title("skills", "apprentice", "Apprentice", "Five skills, and you want a sixth.", "common", ["skills"], [R("skills_5")], [attr(LEARN, 0.05, B)])
title("skills", "adept", "Adept", "Twenty skills.", "uncommon", ["skills"], [R("skills_20")], [attr(MAST, 0.05, B)])
title("skills", "skill_collector", "Skill Collector", "Forty skills.", "rare", ["skills"], [R("skills_40")], [attr(MAST, 0.08, B), attr(LEARN, 0.08, B)])
title("skills", "sage", "Sage", "Seventy-five skills.", "epic", ["skills"], [R("skills_75")], [attr(MAST, 0.10, B)])
title("skills", "arcane_archive", "Arcane Archive", "A hundred and fifty skills.", "legendary", ["skills"], [R("skills_150")], [attr(MAST, 0.15, B), attr(LEARN, 0.15, B)])
title("skills", "unique_bearer", "Unique Bearer", "A Unique skill is yours.", "rare", ["skills"], [R("skill_unique_1")], [attr(MAST, 0.05, B)])
title("skills", "ultimate_wielder", "Ultimate Wielder", "An Ultimate skill is yours.", "legendary", ["skills"], [R("skill_ultimate_1")], [attr(MAST, 0.10, B)])
title("skills", "skill_overlord", "Skill Overlord", "Three Ultimates and five Uniques.", "mythic", ["skills"], [R("skill_ultimate_3"), R("skill_unique_5")], [attr(MAST, 0.20, B)], (), {"ep_gain_percent": 5})

# crafting
title("crafting", "apprentice_smith", "Apprentice Smith", "The first strike.", "common", ["crafting"], [R("forge_1")], [attr(MINING, 0.5)])
title("crafting", "journeyman_smith", "Journeyman Smith", "Fifty items.", "uncommon", ["crafting"], [R("forge_50")], (), (), {"forge_quality_bonus": 2})
title("crafting", "master_smith", "Master Smith", "Two hundred and fifty items.", "rare", ["crafting"], [R("forge_250")], (), (), {"forge_quality_bonus": 4})
title("crafting", "grand_forgemaster", "Grand Forgemaster", "A legendary-quality item left your forge.", "epic", ["crafting"], [R("forgeq_5")], (), (), {"forge_quality_bonus": 6})
title("crafting", "mythic_forger", "Mythic Forger", "A mythical item, and a thousand more.", "mythic", ["crafting"], [R("forgeq_6"), R("forge_1000")], [attr(MINING, 1.0)], (), {"forge_quality_bonus": 10})
title("crafting", "quality_seeker", "Quality Seeker", "Twenty-five items of epic quality or better.", "rare", ["crafting"], [R("forge_q4_x25")], (), (), {"forge_quality_bonus": 3})

# dominion
title("dominion", "landowner", "Landowner", "A plot with your name on it.", "common", ["dominion"], [R("ftb_1")], (), (), {"coin_bonus_percent": 2})
title("dominion", "lord_of_the_manor", "Lord of the Manor", "Twenty-five claimed chunks.", "uncommon", ["dominion"], [R("ftb_25")], [attr(ABS, 2.0)], (), {"coin_bonus_percent": 3})
title("dominion", "territorial_lord", "Territorial Lord", "A hundred claimed chunks.", "rare", ["dominion"], [R("ftb_100")], (), (), {"coin_bonus_percent": 5})
title("dominion", "sovereign", "Sovereign", "Five hundred claimed chunks.", "epic", ["dominion"], [R("ftb_500")], [attr(HEALTH, 2.0)], (), {"coin_bonus_percent": 8})
title("dominion", "emperor", "Emperor", "A thousand claimed chunks: a realm.", "mythic", ["dominion"], [R("ftb_1000")], [attr(HEALTH, 4.0)], (), {"coin_bonus_percent": 12})

# war
title("war", "soldier", "Soldier", "Your first kill in a war.", "common", ["war"], [R("warkills_1")], [attr(ARMOR, 0.5)])
title("war", "warrior", "Warrior", "Fifty war kills.", "uncommon", ["war"], [R("warkills_50")], [attr(ARMOR, 1.0)], (), {"war_damage_percent": 2})
title("war", "warlord", "Warlord", "Five hundred war kills.", "rare", ["war"], [R("warkills_500")], [attr(ARMOR, 1.0)], (), {"war_damage_percent": 4})
title("war", "conqueror", "Conqueror", "Ten wars won.", "epic", ["war"], [R("warwins_10")], [attr(HEALTH, 2.0)], (), {"war_damage_percent": 6})
title("war", "supreme_commander", "Supreme Commander", "Fifty wars won.", "legendary", ["war"], [R("warwins_50")], [attr(HEALTH, 4.0)], (), {"war_damage_percent": 10})
title("war", "envoy", "Envoy", "You were invited to the council.", "common", ["war", "council"], [R("walp_1")], [attr(LUCK, 0.5)])
title("war", "council_regular", "Council Regular", "Five councils attended.", "rare", ["war", "council"], [R("walp_5")], (), (), {"council_vote_weight": 1})
title("war", "walpurgis_veteran", "Walpurgis Veteran", "Twenty-five councils attended.", "epic", ["war", "council"], [R("walp_25")], (), (), {"council_vote_weight": 2})

# calamity
title("calamity", "calamity_witness", "Calamity Witness", "You struck a calamity and lived.", "uncommon", ["calamity"], [R("caldmg_1k")], (), (), {"calamity_damage_percent": 2})
title("calamity", "calamity_hunter", "Calamity Hunter", "One calamity brought down.", "rare", ["calamity"], [R("calkill_1")], (), (), {"calamity_damage_percent": 4})
title("calamity", "calamity_bane", "Calamity Bane", "Ten calamities brought down.", "epic", ["calamity"], [R("calkill_10")], [attr(ATTACK, 0.03, B)], (), {"calamity_damage_percent": 6})
title("calamity", "disaster_end", "End of Disasters", "Fifty calamities brought down.", "legendary", ["calamity"], [R("calkill_50")], [attr(ATTACK, 0.05, B)], (), {"calamity_damage_percent": 10})

# meta
total = sum(len(v) for v in RECORDS.values())
title("meta", "chronicle_keeper", "Chronicle Keeper", "Forty records written down.", "uncommon", ["meta"], [{"records": {"count": 40}}], [attr(LUCK, 0.5)])
title("meta", "archivist", "Archivist", "Eighty records.", "rare", ["meta"], [{"records": {"count": 80}}], [attr(LUCK, 1.0)])
title("meta", "grand_archivist", "Grand Archivist", "A hundred and forty records.", "epic", ["meta"], [{"records": {"count": 140}}], [attr(LUCK, 1.5)], (), {"xp_bonus_percent": 5})
title("meta", "legend_of_legends", "Legend of Legends", "Fifteen legendary records or better.", "legendary", ["meta"], [{"records": {"count": 15, "rarity": "legendary"}}], [attr(LUCK, 2.0)])
title("meta", "point_hunter", "Point Hunter", "Five hundred record points.", "epic", ["meta"], [{"points": 500}], [attr(LUCK, 1.0)])
title("meta", "completionist", "Completionist", "Nine records in ten.", "mythic", ["meta"], [{"records": {"count": int(total * 0.9)}}], [attr(LUCK, 3.0)], (), {"xp_bonus_percent": 10})

# ------------------------------------------------------------------ data synergies (the built-in ones are in BuiltinSynergies.java)
SYNERGIES = [
    {"id": "legends_chorus", "name": "Legends' Chorus", "description": "Three legendary titles sing together.", "tag": "rarity:legendary", "count": 3,
     "bonuses": {"attributes": [attr(HEALTH, 4.0), attr(ATTACK, 0.05, B)], "effects": [], "stats": {}}},
    {"id": "mythic_convergence", "name": "Mythic Convergence", "description": "Two mythic titles, one fate.", "tag": "rarity:mythic", "count": 2,
     "bonuses": {"attributes": [attr(HEALTH, 6.0), attr(MAG, 0.06, B), attr(AURA, 0.06, B)], "effects": [], "stats": {"xp_bonus_percent": 5}}},
    {"id": "slayers_creed", "name": "Slayer's Creed", "description": "Three titles from the great kills.", "tag": "boss", "count": 3,
     "bonuses": {"attributes": [attr(ATTACK, 1.0)], "effects": [], "stats": {"boss_damage_percent": 6}}},
    {"id": "ascended_host", "name": "Ascended Host", "description": "Three titles of the ascended.", "tag": "ascended", "count": 3,
     "bonuses": {"attributes": [attr(MREG, 0.10, B)], "effects": [], "stats": {"ep_gain_percent": 3}}},
    {"id": "merchant_king", "name": "Merchant King", "description": "A sovereign with a grandmaster's forge.", "requires": ["sovereign", "grand_forgemaster"],
     "bonuses": {"attributes": [], "effects": [], "stats": {"coin_bonus_percent": 6, "forge_quality_bonus": 3}}},
    {"id": "combat_discipline", "name": "Combat Discipline", "description": "Six titles of combat.", "tag": "combat", "count": 6,
     "bonuses": {"attributes": [attr(ATTACK, 0.04, B), attr(ARMOR, 1.0)], "effects": [], "stats": {}}},
    {"id": "seekers_creed", "name": "Seeker's Creed", "description": "Four titles of skill.", "tag": "skills", "count": 4,
     "bonuses": {"attributes": [attr(MAST, 0.06, B), attr(LEARN, 0.06, B)], "effects": [], "stats": {}}},
    {"id": "hall_of_records", "name": "Hall of Records", "description": "Three titles of the chronicle.", "tag": "meta", "count": 3,
     "bonuses": {"attributes": [attr(LUCK, 1.0)], "effects": [], "stats": {"xp_bonus_percent": 4}}},
]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main():
    for cat, items in RECORDS.items():
        write(os.path.join(ROOT, "records", cat + ".json"), {"records": items})
    groups = {}
    for g, t in TITLES:
        groups.setdefault(g, []).append(t)
    for g, items in groups.items():
        write(os.path.join(ROOT, "titles", g + ".json"), {"titles": items})
    write(os.path.join(ROOT, "titles", "synergies", "extra.json"), {"synergies": SYNERGIES})
    print("records:", total, "titles:", len(TITLES), "synergies (data):", len(SYNERGIES))
    pts = {"common": 1, "uncommon": 2, "rare": 4, "epic": 8, "legendary": 16, "mythic": 32}
    print("total points:", sum(pts[r["rarity"]] for v in RECORDS.values() for r in v))


if __name__ == "__main__":
    main()
