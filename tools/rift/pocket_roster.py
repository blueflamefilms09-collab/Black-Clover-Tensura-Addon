"""The roster of the 13 exclusive pocket dungeons of 1.99.0: the one table every pocket file is written from (tools/rift/gen_pockets.py).

Each tomb is the remnant of an old civilisation ("dungeons are ancient tombs and ruins that appear without warning": the owner's Black Clover canon), reached only through a mega-rare
dimensional rift. A pocket has: a look (the colour of its tear and of its skybox core, a biome of its own), a gravity rule, a block palette, a floor plan (grid, rooms, loops), elemental trap
rooms, three to five exclusive mobs (a body of the Mob Foundry or a vanilla/Tensura type with its own name, stat multipliers, size and aura), a mega-boss (a D&D profile + extras), a sealed
treasury (the Black Clover magics that unravel it, the force that breaks it, its Sigil), two relics that drop nowhere else (one from the boss, one - the primary artifact - from the treasury) and
the magics that resonate with the tomb's ancient spell.

Names are the mod's own; where a tomb is inspired by a work the inspiration is named in `series` and nothing is copied. Edit THIS file and run `python3 -B tools/rift/gen_pockets.py`.
"""

# ---------------------------------------------------------------------------- helpers
def M(id, name, base, fallback, role, hp=1.0, dmg=1.0, armor=0.0, speed=1.0, scale=1.0, kb=0.0, glow="#ffffff", aura=("minecraft:ash", "#ffffff"), weight=3, g=(1, 3), air=False, elite=False):
    return {"id": id, "name": name, "base": base, "fallback": fallback, "habitat": "air" if air else "ground", "role": role, "hp": hp, "damage": dmg, "armor": armor, "speed": speed,
            "scale": scale, "knockback": kb, "glow": glow, "aura": {"particle": aura[0], "color": aura[1]}, "weight": weight, "group_min": g[0], "group_max": g[1], "elite": elite}


def FB(id):
    return "foundry:" + id


SOFT_CRACKLE = "nusmp:rift.crackle"

POCKETS = []
BOSSES = {}
LINES = {}          # extra lang lines: key -> text


def pocket(**kw):
    POCKETS.append(kw)


# ---------------------------------------------------------------------------- 1. ember_sanctum
pocket(
    id="ember_sanctum", name="Sanctum of the Ashen Forge", series="Black Clover: Dungeons (flame smiths)",
    lore="A smith-kingdom that fed its furnaces with its own sun-stone, buried with its king still at the anvil. The air tastes of cold iron and old coals.",
    accent="#ff6a1a", sky="#4a1208",
    biome=dict(sky="#5a1a0a", fog="#3a1208", water="#d84a14", water_fog="#4a1408", particle="minecraft:ash", particle_chance=0.04, ambient="minecraft:ambient.basalt_deltas.loop",
               mood="minecraft:ambient.basalt_deltas.mood", additions="minecraft:ambient.basalt_deltas.additions", music="minecraft:music.nether.basalt_deltas", weather="ash",
               soft_ambient="nusmp:pocket.ember_sanctum.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=0, negated=1, rivers=2, base=1.0),
    palette=dict(wall="minecraft:polished_blackstone_bricks", floor="minecraft:blackstone", ceiling="minecraft:chiseled_polished_blackstone", pillar="minecraft:gilded_blackstone",
                 accent="minecraft:red_nether_bricks", light="minecraft:shroomlight", glass="minecraft:red_stained_glass", liquid="minecraft:lava"),
    layout=dict(grid=7, rooms=18, cycles=0.35, boss_half=10), traps=["fire", "earth"],
    mobs=[M("slag_brute", "Slag Brute", FB("flame_brigade_cadet"), "minecraft:zombie", "brute", hp=1.5, dmg=1.2, armor=2, speed=0.95, scale=1.1, kb=0.3, glow="#ff7a2a", aura=("minecraft:flame", "#ff6a1a"), weight=5, g=(2, 4)),
          M("anvil_warden", "Anvil Warden", FB("hearthstone_salamander_smith"), "minecraft:vindicator", "tank", hp=2.2, dmg=1.0, armor=4, speed=0.85, scale=1.2, kb=0.6, glow="#ffb040", aura=("minecraft:lava", "#ff8a30"), weight=3, g=(1, 2)),
          M("cinder_adept", "Cinder Adept", FB("ember_choir_pyromancer"), "minecraft:witch", "caster", hp=1.0, dmg=1.4, armor=0, speed=1.0, scale=1.0, kb=0.0, glow="#ff5a1a", aura=("minecraft:small_flame", "#ff5a1a"), weight=3, g=(1, 3)),
          M("emberwing", "Emberwing", FB("ember_bat_flameling_swarm_king"), "minecraft:blaze", "skirmisher", hp=0.9, dmg=1.1, armor=0, speed=1.2, scale=1.0, kb=0.0, glow="#ff9a30", aura=("minecraft:flame", "#ffa030"), weight=4, g=(2, 4), air=True)],
    boss="ignareth_last_smith_king", theme="dwarven_forgehold",
    seal=dict(magic=["FLAME", "MAGMA", "EXPLOSION"], label="Flame", force=2400, sigil="Ember Sigil"),
    relics=dict(boss=["ignareths_forge_hammer"], treasury=["ashen_forgeheart"], primary="ashen_forgeheart"),
    resonance=["FLAME", "MAGMA", "EXPLOSION", "ASH"], collapse_seconds=150, weight=10, min_depth=0, floor_min=120)
BOSSES["ignareth_last_smith_king"] = dict(
    side="evil", tier=4, variant=0, name="Ignareth, the Last Smith-King", anime=["Black Clover: a flame-smith king of a buried civilisation"], dnd=["Efreeti Sultan", "Fire Giant King", "Balor"],
    forms=["Smith-King of the Forge", "Slag Colossus", "Heart of the Furnace"], resistances=4, legendary_points=4,
    legendary_actions=[("tail_sweep", 1), ("wing_buffet", 1), ("dragon_breath", 2), ("cantrip", 1)], lair_actions=["tremor", "falling_stars", "searing_light"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="fire", reflective=False, look="balor",
    mega=dict(damage=1.4, scale=1.15, intro="The anvils fall silent. Something vast stands up from the forge.", death="The furnace-light goes out of the Smith-King's eyes.", adds="emberwing", add_count=4))

# ---------------------------------------------------------------------------- 2. verdant_reliquary
pocket(
    id="verdant_reliquary", name="Verdant Reliquary", series="Black Clover: Dungeons (elven groves)",
    lore="The elves who raised this house of relics let the forest grow over them rather than leave. Roots have become its corridors, and the garden still guards what the gardeners buried.",
    accent="#58c23a", sky="#12351c",
    biome=dict(sky="#1c4a26", fog="#12351c", water="#3a9a4a", water_fog="#0c2a14", particle="minecraft:spore_blossom_air", particle_chance=0.03, ambient="minecraft:ambient.warped_forest.loop",
               mood="minecraft:ambient.warped_forest.mood", additions="minecraft:ambient.warped_forest.additions", music="minecraft:music.overworld.lush_caves", weather="spores",
               soft_ambient="nusmp:pocket.verdant_reliquary.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=0, rivers=2, base=1.0),
    palette=dict(wall="minecraft:mossy_stone_bricks", floor="minecraft:mossy_cobblestone", ceiling="minecraft:stone_bricks", pillar="minecraft:stripped_oak_log",
                 accent="minecraft:moss_block", light="minecraft:shroomlight", glass="minecraft:lime_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=19, cycles=0.4, boss_half=10), traps=["plant", "poison"],
    mobs=[M("briar_warden", "Briar Warden", FB("heartwood_treant_warden"), "minecraft:iron_golem", "tank", hp=2.0, dmg=1.0, armor=3, speed=0.8, scale=1.25, kb=0.6, glow="#58c23a", aura=("minecraft:happy_villager", "#58c23a"), weight=3, g=(1, 2)),
          M("thorn_stalker", "Thorn Stalker", FB("briar_rose_duelist"), "minecraft:spider", "skirmisher", hp=1.1, dmg=1.3, armor=0, speed=1.25, scale=1.0, kb=0.0, glow="#9ae04a", aura=("minecraft:composter", "#9ae04a"), weight=5, g=(2, 4)),
          M("sporecaller", "Sporecaller", FB("rotwood_treant_blightcaller"), "minecraft:witch", "caster", hp=1.1, dmg=1.3, armor=0, speed=0.95, scale=1.1, kb=0.1, glow="#8be04a", aura=("minecraft:sneeze", "#8be04a"), weight=3, g=(1, 3)),
          M("glade_wisp", "Glade Wisp", FB("wisp_lantern_fae"), "minecraft:vex", "caster", hp=0.8, dmg=1.1, armor=0, speed=1.1, scale=0.9, kb=0.0, glow="#c8ff8a", aura=("minecraft:end_rod", "#c8ff8a"), weight=4, g=(2, 3), air=True)],
    boss="thornmother_yselde", theme="feywild_grove",
    seal=dict(magic=["PLANT", "EARTH", "BRIAR", "WORLD_TREE"], label="Plant", force=2200, sigil="Seed Sigil"),
    relics=dict(boss=["thornmothers_briar_bow"], treasury=["reliquary_seedheart"], primary="reliquary_seedheart"),
    resonance=["PLANT", "EARTH", "BRIAR", "WORLD_TREE"], collapse_seconds=150, weight=10, min_depth=0, floor_min=60)
BOSSES["thornmother_yselde"] = dict(
    side="holy", tier=3, variant=1, name="Thornmother Yselde", anime=["Black Clover: a grove-mother of a buried elven house"], dnd=["Treant", "Dryad Queen", "Hezrou"],
    forms=["Thornmother", "Rootbound Matriarch", "Blighted Bloom"], resistances=3, legendary_points=3,
    legendary_actions=[("life_drain", 1), ("vampiric_bite", 2), ("mind_blast", 2), ("cantrip", 1)], lair_actions=["necrotic_mist", "tremor"],
    anti_magic_aura=False, frightful_aura=False, absorb_magic=False, mythic=True, execution=False, breath="poison", reflective=False, look="hezrou",
    mega=dict(damage=1.3, scale=1.1, intro="The roots of the reliquary draw tight. A voice like creaking timber says: you may take nothing.", death="The Thornmother's leaves fall, one by one, and do not grow back.", adds="thorn_stalker", add_count=4))

# ---------------------------------------------------------------------------- 3. frozen_cathedral
pocket(
    id="frozen_cathedral", name="Cathedral of the Silent Winter", series="Black Clover: Dungeons (ice kingdom)",
    lore="A choir sang the winter in and never sang it out. The nave is still full of kneeling frost, and the bells hang, frozen mid-note, over a vault no thaw has touched.",
    accent="#9fe3ff", sky="#24425c",
    biome=dict(sky="#2c5070", fog="#24425c", water="#6ab8e8", water_fog="#10304a", particle="minecraft:snowflake", particle_chance=0.06, ambient="minecraft:ambient.soul_sand_valley.loop",
               mood="minecraft:ambient.soul_sand_valley.mood", additions="minecraft:ambient.soul_sand_valley.additions", music="minecraft:music.overworld.frozen_peaks", weather="snow",
               soft_ambient="nusmp:pocket.frozen_cathedral.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=0, negated=1, rivers=1, base=1.0),
    palette=dict(wall="minecraft:calcite", floor="minecraft:smooth_quartz", ceiling="minecraft:packed_ice", pillar="minecraft:chiseled_quartz_block",
                 accent="minecraft:light_blue_concrete", light="minecraft:sea_lantern", glass="minecraft:light_blue_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=5, rooms=14, cycles=0.4, boss_half=10), traps=["frost", "wind"],
    mobs=[M("rime_penitent", "Rime Penitent", FB("ice_devil_palace_warden"), "minecraft:stray", "tank", hp=2.0, dmg=1.0, armor=4, speed=0.85, scale=1.2, kb=0.5, glow="#9fe3ff", aura=("minecraft:snowflake", "#cfefff"), weight=3, g=(1, 2)),
          M("choir_wraith", "Choir Wraith", FB("gelid_banshee_choirmaster"), "minecraft:stray", "caster", hp=1.0, dmg=1.4, armor=0, speed=1.0, scale=1.1, kb=0.0, glow="#bfeaff", aura=("minecraft:cloud", "#bfeaff"), weight=3, g=(1, 3)),
          M("glacier_knight", "Glacier Knight", FB("tombstone_revenant_knight"), "minecraft:stray", "brute", hp=1.6, dmg=1.2, armor=3, speed=0.95, scale=1.1, kb=0.3, glow="#7fc8ff", aura=("minecraft:snowflake", "#7fc8ff"), weight=5, g=(2, 4)),
          M("frost_gargoyle", "Frost Gargoyle", FB("cathedral_gargoyle_sentinel"), "minecraft:vex", "skirmisher", hp=1.2, dmg=1.1, armor=2, speed=1.1, scale=1.0, kb=0.2, glow="#9fe3ff", aura=("minecraft:cloud", "#dff4ff"), weight=4, g=(2, 3), air=True)],
    boss="rimeheart_archon", theme="frost",
    seal=dict(magic=["ICE", "WATER", "MIST", "SNOW", "ICE_WEDGE"], label="Ice", force=2200, sigil="Rime Sigil"),
    relics=dict(boss=["rimeheart_greatblade"], treasury=["silent_winter_mantle"], primary="silent_winter_mantle"),
    resonance=["ICE", "SNOW", "WATER", "MIST"], collapse_seconds=135, weight=10, min_depth=0, floor_min=140)
BOSSES["rimeheart_archon"] = dict(
    side="holy", tier=4, variant=0, name="Rimeheart Archon", anime=["Black Clover: the last high priest of a winter kingdom"], dnd=["Lich", "Ice Devil", "Frost Giant Jarl"],
    forms=["Rimeheart Archon", "Shattered Archon", "Winter Unbound"], resistances=4, legendary_points=4,
    legendary_actions=[("dragon_breath", 2), ("counterspell", 1), ("death_touch", 2), ("cantrip", 1)], lair_actions=["anti_magic_field", "searing_light", "tremor"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=True, mythic=True, execution=False, breath="frost", reflective=False, look="lich_king",
    mega=dict(damage=1.4, scale=1.15, intro="The frozen bells ring once. The Archon rises from the choir stalls with a sound like ice splitting.", death="The Archon's heart, a lump of blue ice, cracks and runs like water.", adds="glacier_knight", add_count=3))

# ---------------------------------------------------------------------------- 4. drowned_observatory
pocket(
    id="drowned_observatory", name="Drowned Observatory", series="Black Clover: Dungeons (sea astronomers)",
    lore="Astronomers read the stars through the sea until the sea rose to read them back. Rivers run along the ceilings of its halls, as if the tide had forgotten which way is down.",
    accent="#3ac8d8", sky="#0c3a4a",
    biome=dict(sky="#124a5c", fog="#0c3a4a", water="#2aa8c8", water_fog="#06242e", particle="minecraft:underwater", particle_chance=0.08, ambient="minecraft:ambient.underwater.loop",
               mood="minecraft:ambient.cave", additions="minecraft:ambient.underwater.loop.additions.rare", music="minecraft:music.under_water", weather="mist",
               soft_ambient="nusmp:pocket.drowned_observatory.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=0, rivers=3, base=1.0),
    palette=dict(wall="minecraft:dark_prismarine", floor="minecraft:prismarine_bricks", ceiling="minecraft:dark_prismarine", pillar="minecraft:prismarine",
                 accent="minecraft:cyan_terracotta", light="minecraft:sea_lantern", glass="minecraft:cyan_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=18, cycles=0.4, boss_half=10), traps=["water", "lightning"],
    mobs=[M("tide_cultist", "Tide Cultist", FB("drowned_cult_high_priest"), "minecraft:drowned", "caster", hp=1.1, dmg=1.3, armor=0, speed=1.0, scale=1.05, kb=0.0, glow="#3ac8d8", aura=("minecraft:bubble_pop", "#3ac8d8"), weight=3, g=(1, 3)),
          M("brine_marine", "Brine Marine", FB("sunken_banner_marine"), "minecraft:drowned", "brute", hp=1.6, dmg=1.2, armor=2, speed=0.95, scale=1.1, kb=0.3, glow="#2aa8c8", aura=("minecraft:bubble", "#2aa8c8"), weight=5, g=(2, 4)),
          M("deep_siren", "Deep Siren", FB("siren_of_the_drowned_choir"), "minecraft:witch", "caster", hp=1.0, dmg=1.4, armor=0, speed=1.0, scale=1.0, kb=0.0, glow="#8ae8ff", aura=("minecraft:dolphin", "#8ae8ff"), weight=3, g=(1, 2)),
          M("lantern_moth", "Lantern Moth", FB("moth_wing_cave_fiend"), "minecraft:vex", "skirmisher", hp=0.9, dmg=1.1, armor=0, speed=1.2, scale=1.1, kb=0.0, glow="#bff8ff", aura=("minecraft:glow", "#bff8ff"), weight=4, g=(2, 4), air=True)],
    boss="tidewatcher_leviathan", theme="drowned_crypt",
    seal=dict(magic=["WATER", "MIST", "MERCURY", "BUBBLE", "DEMON_WATER"], label="Water", force=2200, sigil="Tide Sigil"),
    relics=dict(boss=["deepwatch_harpoon"], treasury=["orrery_of_drowned_hours"], primary="orrery_of_drowned_hours"),
    resonance=["WATER", "MIST", "BUBBLE", "MERCURY"], collapse_seconds=165, weight=10, min_depth=0, floor_min=160)
BOSSES["tidewatcher_leviathan"] = dict(
    side="evil", tier=4, variant=1, name="Tidewatcher Leviathan", anime=["Black Clover: the sea-warden of a drowned astronomers' house"], dnd=["Kraken", "Aboleth", "Sea Hag Admiral"],
    forms=["Tidewatcher", "Drowned Leviathan", "Maelstrom Unbound"], resistances=4, legendary_points=4,
    legendary_actions=[("tail_sweep", 1), ("gravity_crush", 2), ("magic_drain", 1), ("haki_pulse", 3)], lair_actions=["tremor", "necrotic_mist"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="lightning", reflective=False, look="kraken",
    mega=dict(damage=1.3, scale=1.2, intro="Every river on the ceiling turns once, as if the sea were looking down. The Leviathan unfolds out of the dark water.", death="The tide pulls back from the Leviathan and does not return.", adds="brine_marine", add_count=3))

# ---------------------------------------------------------------------------- 5. stormspire_aerie
pocket(
    id="stormspire_aerie", name="Stormspire Aerie", series="Black Clover: Dungeons (sky riders)",
    lore="The sky riders built their roost on a spire of stone that no longer remembers the ground. Their halls have forgotten weight; their guardians have not forgotten their lances.",
    accent="#7aa8ff", sky="#34406a",
    biome=dict(sky="#405080", fog="#34406a", water="#5a8ad8", water_fog="#1a2a4a", particle="minecraft:cloud", particle_chance=0.03, ambient="minecraft:ambient.nether_wastes.loop",
               mood="minecraft:ambient.nether_wastes.mood", additions="minecraft:ambient.nether_wastes.additions", music="minecraft:music.overworld.jagged_peaks", weather="storm",
               soft_ambient="nusmp:pocket.stormspire_aerie.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=0, negated=2, rivers=0, base=1.0),
    palette=dict(wall="minecraft:tuff_bricks", floor="minecraft:polished_tuff", ceiling="minecraft:chiseled_tuff", pillar="minecraft:waxed_oxidized_copper",
                 accent="minecraft:light_gray_concrete", light="minecraft:sea_lantern", glass="minecraft:white_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=17, cycles=0.35, boss_half=10), traps=["lightning", "wind"],
    mobs=[M("cloud_knight", "Cloud Knight", FB("sky_citadel_cloudknight"), "minecraft:vex", "brute", hp=1.4, dmg=1.3, armor=2, speed=1.1, scale=1.1, kb=0.2, glow="#7aa8ff", aura=("minecraft:cloud", "#cfe0ff"), weight=4, g=(2, 3), air=True),
          M("storm_lancer", "Storm Lancer", FB("warband_centaur_lancer"), "minecraft:pillager", "skirmisher", hp=1.3, dmg=1.4, armor=1, speed=1.3, scale=1.1, kb=0.2, glow="#a8c8ff", aura=("minecraft:electric_spark", "#fff07a"), weight=4, g=(2, 3)),
          M("gale_dancer", "Gale Dancer", FB("windmill_reed_dancer"), "minecraft:witch", "caster", hp=1.0, dmg=1.3, armor=0, speed=1.1, scale=1.0, kb=0.0, glow="#cfeff6", aura=("minecraft:cloud", "#cfeff6"), weight=3, g=(1, 3)),
          M("cloud_giant", "Cloud Giant", FB("cloud_giant_courtier"), "minecraft:iron_golem", "tank", hp=2.2, dmg=1.1, armor=3, speed=0.85, scale=1.35, kb=0.6, glow="#8ab0ff", aura=("minecraft:cloud", "#e6f0ff"), weight=2, g=(1, 1)),
          M("thunder_roc", "Thunder Roc", FB("thunderbird_rider_of_the_spire"), "minecraft:phantom", "skirmisher", hp=1.1, dmg=1.3, armor=0, speed=1.3, scale=1.15, kb=0.0, glow="#fff07a", aura=("minecraft:electric_spark", "#fff07a"), weight=3, g=(1, 2), air=True)],
    boss="valkarin_stormwing", theme="sky_citadel",
    seal=dict(magic=["LIGHTNING", "STORM", "WIND", "AIR", "STAR"], label="Storm", force=2400, sigil="Gale Sigil"),
    relics=dict(boss=["valkarins_stormlance"], treasury=["aerie_courier_gloves"], primary="aerie_courier_gloves"),
    resonance=["STORM", "LIGHTNING", "WIND", "AIR"], collapse_seconds=150, weight=9, min_depth=0, floor_min=220)
BOSSES["valkarin_stormwing"] = dict(
    side="holy", tier=4, variant=0, name="Valkarin the Stormwing", anime=["Black Clover: the roost-lord of a vanished sky-rider house"], dnd=["Bahamut", "Roc", "Storm Giant"],
    forms=["Valkarin", "Stormwing Ascendant", "Tempest Crowned"], resistances=4, legendary_points=4,
    legendary_actions=[("wing_buffet", 1), ("dragon_breath", 2), ("blink", 1), ("frightful_presence", 2), ("haki_pulse", 3)], lair_actions=["falling_stars", "searing_light"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="lightning", reflective=False, look="bahamut",
    mega=dict(damage=1.4, scale=1.2, intro="Lightning walks along the spire. Valkarin lowers himself from the roof of the world.", death="The storm leaves Valkarin all at once, and the spire is very quiet.", adds="thunder_roc", add_count=3))

# ---------------------------------------------------------------------------- 6. gilded_crypt
pocket(
    id="gilded_crypt", name="Gilded Crypt of the Sun-Eater", series="Black Clover: Dungeons (desert kings)",
    lore="A desert dynasty swallowed the sun to keep its dead from rotting, and was in turn swallowed by the dunes. The gold is all still here. So is the patience of whatever swallowed the sun.",
    accent="#f0c040", sky="#5a4a1c",
    biome=dict(sky="#6a5624", fog="#5a4a1c", water="#c8a030", water_fog="#2a2008", particle="minecraft:white_ash", particle_chance=0.03, ambient="minecraft:ambient.nether_wastes.loop",
               mood="minecraft:ambient.cave", additions="minecraft:ambient.nether_wastes.additions", music="minecraft:music.overworld.stony_peaks", weather="ash",
               soft_ambient="nusmp:pocket.gilded_crypt.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=0, rivers=1, base=1.0),
    palette=dict(wall="minecraft:cut_sandstone", floor="minecraft:smooth_sandstone", ceiling="minecraft:chiseled_sandstone", pillar="minecraft:gilded_blackstone",
                 accent="minecraft:gold_block", light="minecraft:glowstone", glass="minecraft:yellow_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=18, cycles=0.35, boss_half=10), traps=["earth", "fire", "poison"],
    mobs=[M("sand_wight", "Sand Wight", FB("tomb_wight_sentry"), "minecraft:husk", "tank", hp=2.0, dmg=1.0, armor=3, speed=0.85, scale=1.15, kb=0.5, glow="#f0c040", aura=("minecraft:white_ash", "#f0d890"), weight=4, g=(1, 3)),
          M("canopic_priest", "Canopic Priest", FB("corpse_candle_bearer"), "minecraft:witch", "caster", hp=1.1, dmg=1.3, armor=0, speed=0.95, scale=1.05, kb=0.0, glow="#ffd860", aura=("minecraft:small_flame", "#ffd860"), weight=3, g=(1, 3)),
          M("scarab_swarmer", "Scarab Swarmer", FB("beetle_carapace_ravager"), "minecraft:silverfish", "skirmisher", hp=1.0, dmg=1.2, armor=2, speed=1.3, scale=1.1, kb=0.0, glow="#c8a030", aura=("minecraft:falling_nectar", "#c8a030"), weight=5, g=(3, 5)),
          M("dust_gargoyle", "Dust Gargoyle", FB("vault_gargoyle_gnawer"), "minecraft:vex", "brute", hp=1.2, dmg=1.2, armor=2, speed=1.05, scale=1.1, kb=0.2, glow="#e8c870", aura=("minecraft:white_ash", "#e8c870"), weight=3, g=(1, 3), air=True)],
    boss="anhetep_sun_eater", theme="shadow",
    seal=dict(magic=["SAND", "SANDSTONE", "EARTH", "LIGHT", "STONE"], label="Sand", force=2400, sigil="Sun Sigil"),
    relics=dict(boss=["anheteps_sunscourge"], treasury=["sun_eaters_locket"], primary="sun_eaters_locket"),
    resonance=["SAND", "SANDSTONE", "LIGHT", "EARTH"], collapse_seconds=165, weight=8, min_depth=10, floor_min=100)
BOSSES["anhetep_sun_eater"] = dict(
    side="evil", tier=4, variant=1, name="Anhetep, the Sun-Eater", anime=["Black Clover: a desert dynast who swallowed the sun"], dnd=["Mummy Lord", "Acererak", "Death Tyrant"],
    forms=["Anhetep", "Sun-Eater Unwrapped", "Eclipse Pharaoh"], resistances=4, legendary_points=4,
    legendary_actions=[("magic_drain", 1), ("life_drain", 1), ("death_touch", 2), ("eye_rays", 2), ("cantrip", 1)], lair_actions=["necrotic_mist", "anti_magic_field"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=True, mythic=True, execution=True, breath="fire", reflective=False, look="acererak",
    mega=dict(damage=1.4, scale=1.15, intro="The gold in the walls dims, all at once. Something very old unwraps itself on the dais.", death="Anhetep's mouth opens one last time, and the sun he swallowed comes out of it, and goes.", adds="canopic_priest", add_count=3))

# ---------------------------------------------------------------------------- 7. umbral_catacombs
pocket(
    id="umbral_catacombs", name="Umbral Catacombs of the Pact", series="Black Clover: Dungeons (devil pacts)",
    lore="A house that bargained with a devil and kept the bargain too well. Its catacombs run upside down in places, because the devil asked for a floor and was given a ceiling.",
    accent="#a04aff", sky="#160a24",
    biome=dict(sky="#1c0e30", fog="#160a24", water="#4a2a88", water_fog="#0a0414", particle="minecraft:soul", particle_chance=0.03, ambient="minecraft:ambient.soul_sand_valley.loop",
               mood="minecraft:ambient.soul_sand_valley.mood", additions="minecraft:ambient.soul_sand_valley.additions", music="minecraft:music.nether.soul_sand_valley", weather="mist",
               soft_ambient="nusmp:pocket.umbral_catacombs.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=2, negated=0, rivers=0, base=1.0),
    palette=dict(wall="minecraft:deepslate_bricks", floor="minecraft:polished_deepslate", ceiling="minecraft:chiseled_deepslate", pillar="minecraft:cracked_deepslate_bricks",
                 accent="minecraft:crying_obsidian", light="minecraft:pearlescent_froglight", glass="minecraft:purple_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=20, cycles=0.4, boss_half=11), traps=["dark", "blood"],
    mobs=[M("pact_blade", "Pact Blade", FB("ritual_blade_initiate_of_the_black_sun"), "minecraft:wither_skeleton", "skirmisher", hp=1.3, dmg=1.5, armor=1, speed=1.2, scale=1.05, kb=0.1, glow="#a04aff", aura=("minecraft:soul", "#a04aff"), weight=5, g=(2, 4)),
          M("shade_stalker", "Shade Stalker", FB("wraith_lord_of_the_cold_crypt"), "minecraft:wither_skeleton", "skirmisher", hp=1.2, dmg=1.4, armor=0, speed=1.25, scale=1.1, kb=0.0, glow="#7a3ac8", aura=("minecraft:squid_ink", "#7a3ac8"), weight=3, g=(1, 3)),
          M("devil_thrall", "Devil Thrall", FB("bone_devil_taskmaster"), "minecraft:vindicator", "brute", hp=1.8, dmg=1.3, armor=3, speed=0.9, scale=1.2, kb=0.4, glow="#c070ff", aura=("minecraft:soul_fire_flame", "#c070ff"), weight=3, g=(1, 2)),
          M("umbral_imp", "Umbral Imp", FB("bat_winged_whisper_imp"), "minecraft:vex", "caster", hp=0.8, dmg=1.2, armor=0, speed=1.25, scale=0.95, kb=0.0, glow="#d8a0ff", aura=("minecraft:witch", "#d8a0ff"), weight=4, g=(2, 4), air=True)],
    boss="nekrathul_pact_breaker", theme="abyss",
    seal=dict(magic=["DARK", "SHADOW", "SPATIAL", "CURSE", "DEMON_BEAST"], label="Darkness", force=2800, sigil="Umbral Sigil"),
    relics=dict(boss=["nekrathuls_pact_scythe"], treasury=["pactbreaker_shroud"], primary="pactbreaker_shroud"),
    resonance=["DARK", "SHADOW", "CURSE", "SPATIAL"], collapse_seconds=180, weight=4, min_depth=50, floor_min=420)
BOSSES["nekrathul_pact_breaker"] = dict(
    side="evil", tier=5, variant=0, name="Nekrathul, the Pact-Breaker", anime=["Black Clover: a devil's house-lord who broke his own pact"], dnd=["Archdevil", "Asmodeus", "Pit Fiend"],
    forms=["Nekrathul", "Pact Unbound", "Devil Crowned"], resistances=5, legendary_points=5,
    legendary_actions=[("counterspell", 1), ("mind_blast", 2), ("blink", 1), ("death_touch", 2), ("magic_drain", 1)], lair_actions=["anti_magic_field", "falling_stars", "necrotic_mist"],
    anti_magic_aura=True, frightful_aura=True, absorb_magic=False, mythic=True, execution=True, breath="fire", reflective=False, look="asmodeus",
    mega=dict(damage=1.5, scale=1.2, intro="The floor and the ceiling trade places in your mind. A voice that has not spoken in a thousand years says: another contract.", death="The pact tears itself in two, and Nekrathul is the thing between the halves.", adds="umbral_imp", add_count=5))

# ---------------------------------------------------------------------------- 8. starfall_archive
pocket(
    id="starfall_archive", name="Starfall Archive", series="Black Clover: Dungeons (star scribes)",
    lore="A library built where a star fell, by scribes who copied the star's light onto paper until the paper burned. Gravity is a suggestion here; the books float, and so do you.",
    accent="#b9a3ff", sky="#1a1040",
    biome=dict(sky="#201450", fog="#1a1040", water="#6a5ad8", water_fog="#0c0828", particle="minecraft:end_rod", particle_chance=0.02, ambient="minecraft:ambient.warped_forest.loop",
               mood="minecraft:ambient.warped_forest.mood", additions="minecraft:ambient.warped_forest.additions", music="minecraft:music.end", weather="clear",
               soft_ambient="nusmp:pocket.starfall_archive.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=2, rivers=1, base=1.0),
    palette=dict(wall="minecraft:deepslate_tiles", floor="minecraft:polished_blackstone", ceiling="minecraft:amethyst_block", pillar="minecraft:purpur_pillar",
                 accent="minecraft:amethyst_block", light="minecraft:pearlescent_froglight", glass="minecraft:blue_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=21, cycles=0.4, boss_half=10), traps=["light", "time"],
    mobs=[M("archive_scribe", "Archive Scribe", FB("lunar_archive_scribe"), "minecraft:witch", "caster", hp=1.0, dmg=1.4, armor=0, speed=1.0, scale=1.05, kb=0.0, glow="#b9a3ff", aura=("minecraft:enchant", "#b9a3ff"), weight=4, g=(1, 3)),
          M("star_oracle", "Star Oracle", FB("starfall_oracle"), "minecraft:evoker", "caster", hp=1.2, dmg=1.5, armor=0, speed=0.95, scale=1.1, kb=0.0, glow="#e0d0ff", aura=("minecraft:end_rod", "#e0d0ff"), weight=2, g=(1, 2)),
          M("ink_golem", "Ink Golem", FB("prismatic_golem_chimes"), "minecraft:iron_golem", "tank", hp=2.0, dmg=1.1, armor=4, speed=0.85, scale=1.25, kb=0.6, glow="#8a70e8", aura=("minecraft:squid_ink", "#8a70e8"), weight=3, g=(1, 2)),
          M("astral_wingmaiden", "Astral Wingmaiden", FB("deva_wingmaiden"), "minecraft:phantom", "skirmisher", hp=1.1, dmg=1.3, armor=0, speed=1.25, scale=1.1, kb=0.0, glow="#f0e0ff", aura=("minecraft:glow", "#f0e0ff"), weight=4, g=(2, 3), air=True)],
    boss="oriel_many_eyed_archivist", theme="astral",
    seal=dict(magic=["STAR", "LIGHT", "TIME", "GRAVITY", "COMPASS"], label="Star", force=2400, sigil="Astral Sigil"),
    relics=dict(boss=["orials_starfall_staff"], treasury=["archivists_starbound_codex"], primary="archivists_starbound_codex"),
    resonance=["STAR", "TIME", "GRAVITY", "COMPASS", "LIGHT"], collapse_seconds=165, weight=8, min_depth=20, floor_min=260)
BOSSES["oriel_many_eyed_archivist"] = dict(
    side="holy", tier=3, variant=1, name="Oriel, the Many-Eyed Archivist", anime=["Black Clover: the last archivist of a star-scribe house"], dnd=["Beholder", "Elder Brain", "Death Tyrant"],
    forms=["Oriel", "Archivist Awake", "Every Eye Open"], resistances=3, legendary_points=4,
    legendary_actions=[("eye_rays", 2), ("mind_blast", 2), ("magic_drain", 1), ("counterspell", 1)], lair_actions=["anti_magic_field", "searing_light"],
    anti_magic_aura=True, frightful_aura=False, absorb_magic=True, mythic=True, execution=False, breath="lightning", reflective=False, look="beholder",
    mega=dict(damage=1.3, scale=1.15, intro="Every book in the Archive turns its pages at once. Oriel opens, one eye after another, to read you.", death="The eyes of the Archivist close, one after another, and the last page is written.", adds="archive_scribe", add_count=4))

# ---------------------------------------------------------------------------- 9. crimson_abattoir
pocket(
    id="crimson_abattoir", name="Crimson Abattoir", series="Black Clover: Dungeons (blood cults)",
    lore="A cult's arena where every wound was an offering. The rivers on its ceilings are not water, and the floors remember every one who bled on them.",
    accent="#c01818", sky="#3a0a0e",
    biome=dict(sky="#4a1016", fog="#3a0a0e", water="#a01010", water_fog="#240408", particle="minecraft:crimson_spore", particle_chance=0.05, ambient="minecraft:ambient.crimson_forest.loop",
               mood="minecraft:ambient.crimson_forest.mood", additions="minecraft:ambient.crimson_forest.additions", music="minecraft:music.nether.crimson_forest", weather="spores",
               soft_ambient="nusmp:pocket.crimson_abattoir.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=0, rivers=1, base=1.0),
    palette=dict(wall="minecraft:nether_bricks", floor="minecraft:red_nether_bricks", ceiling="minecraft:red_nether_bricks", pillar="minecraft:stripped_crimson_stem",
                 accent="minecraft:nether_wart_block", light="minecraft:shroomlight", glass="minecraft:red_stained_glass", liquid="minecraft:lava"),
    layout=dict(grid=7, rooms=19, cycles=0.45, boss_half=10), traps=["blood", "fire"],
    mobs=[M("blood_butcher", "Blood Butcher", FB("executioner_of_the_black_mass"), "minecraft:vindicator", "brute", hp=1.7, dmg=1.4, armor=2, speed=0.95, scale=1.15, kb=0.3, glow="#c01818", aura=("minecraft:crimson_spore", "#c01818"), weight=4, g=(1, 3)),
          M("gore_hound", "Gore Hound", FB("blood_moon_werewolf_huntsman"), "minecraft:wolf", "skirmisher", hp=1.2, dmg=1.4, armor=0, speed=1.35, scale=1.05, kb=0.0, glow="#e03030", aura=("minecraft:damage_indicator", "#e03030"), weight=5, g=(2, 4)),
          M("sanguine_priest", "Sanguine Priest", FB("inquisitor_of_the_weeping_altar"), "minecraft:witch", "caster", hp=1.1, dmg=1.3, armor=0, speed=0.95, scale=1.05, kb=0.0, glow="#ff4a4a", aura=("minecraft:dripping_lava", "#ff4a4a"), weight=3, g=(1, 3)),
          M("carrion_bat", "Carrion Bat", FB("pit_bat_swarmlord"), "minecraft:vex", "skirmisher", hp=0.9, dmg=1.2, armor=0, speed=1.3, scale=1.1, kb=0.0, glow="#a01010", aura=("minecraft:crimson_spore", "#a01010"), weight=4, g=(2, 4), air=True)],
    boss="vorgrim_sanguine_warden", theme="vampire_castle",
    seal=dict(magic=["BLOOD", "BEAST", "BONE", "REINFORCEMENT", "BODY"], label="Blood", force=2400, sigil="Blood Sigil"),
    relics=dict(boss=["sanguine_wardens_cleaver"], treasury=["bloodbound_gauntlets"], primary="bloodbound_gauntlets"),
    resonance=["BLOOD", "BONE", "BEAST", "BODY"], collapse_seconds=150, weight=9, min_depth=0, floor_min=180)
BOSSES["vorgrim_sanguine_warden"] = dict(
    side="evil", tier=3, variant=0, name="Vorgrim, the Sanguine Warden", anime=["Black Clover: the warden of a blood cult's arena"], dnd=["Vampire Lord", "Strahd", "Blood Hunter"],
    forms=["Vorgrim", "Warden Unleashed", "Crimson Sovereign"], resistances=3, legendary_points=4,
    legendary_actions=[("vampiric_bite", 1), ("blink", 1), ("frightful_presence", 2), ("life_drain", 2)], lair_actions=["necrotic_mist", "tremor"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="fire", reflective=False, look="strahd",
    mega=dict(damage=1.4, scale=1.1, intro="The red rivers slow on the ceiling. Vorgrim steps out of the shadow behind the dais, already smiling.", death="Vorgrim laughs once, wetly, and the arena is quiet.", adds="carrion_bat", add_count=5))

# ---------------------------------------------------------------------------- 10. clockwork_vault
pocket(
    id="clockwork_vault", name="Clockwork Vault of Hours", series="Black Clover: Dungeons (time artificers)",
    lore="Artificers who tried to build a vault that no thief could outwait. They succeeded: the Vault still ticks, and it will wait for you, and wait, and wait.",
    accent="#e0a040", sky="#2a2418",
    biome=dict(sky="#342c1e", fog="#2a2418", water="#b08030", water_fog="#14100a", particle="minecraft:electric_spark", particle_chance=0.01, ambient="minecraft:ambient.nether_wastes.loop",
               mood="minecraft:ambient.cave", additions="minecraft:ambient.nether_wastes.additions", music="minecraft:music.overworld.dripstone_caves", weather="clear",
               soft_ambient="nusmp:pocket.clockwork_vault.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=2, negated=1, rivers=0, base=1.0),
    palette=dict(wall="minecraft:waxed_cut_copper", floor="minecraft:polished_andesite", ceiling="minecraft:waxed_cut_copper", pillar="minecraft:waxed_chiseled_copper",
                 accent="minecraft:raw_gold_block", light="minecraft:ochre_froglight", glass="minecraft:orange_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=20, cycles=0.45, boss_half=10), traps=["time", "lightning", "earth"],
    mobs=[M("gear_sentinel", "Gear Sentinel", FB("bellmetal_choir_golem"), "minecraft:iron_golem", "tank", hp=2.0, dmg=1.1, armor=4, speed=0.8, scale=1.2, kb=0.6, glow="#e0a040", aura=("minecraft:electric_spark", "#ffd070"), weight=3, g=(1, 2)),
          M("brass_lancer", "Brass Lancer", FB("dawnbreak_lancer_cavalry"), "minecraft:pillager", "skirmisher", hp=1.3, dmg=1.4, armor=2, speed=1.25, scale=1.1, kb=0.2, glow="#ffc060", aura=("minecraft:small_flame", "#ffc060"), weight=4, g=(2, 3)),
          M("tick_artificer", "Tick Artificer", FB("rune_smith_gnome_tinkerer"), "minecraft:witch", "caster", hp=1.0, dmg=1.3, armor=0, speed=1.0, scale=1.0, kb=0.0, glow="#ffe0a0", aura=("minecraft:reverse_portal", "#ffe0a0"), weight=3, g=(1, 3)),
          M("pendulum_gargoyle", "Pendulum Gargoyle", FB("gargoyle_spire_sentinel"), "minecraft:vex", "brute", hp=1.3, dmg=1.2, armor=3, speed=1.05, scale=1.15, kb=0.3, glow="#b08030", aura=("minecraft:electric_spark", "#b08030"), weight=3, g=(1, 3), air=True)],
    boss="veyl_chronarch", theme="gravity",
    seal=dict(magic=["TIME", "SEALING", "GRAVITY", "CREATION", "KEY"], label="Time", force=2600, sigil="Gear Sigil"),
    relics=dict(boss=["chronarchs_pendulum_sabre"], treasury=["vaultmakers_pocketwatch"], primary="vaultmakers_pocketwatch"),
    resonance=["TIME", "SEALING", "GRAVITY", "KEY"], collapse_seconds=180, weight=6, min_depth=20, floor_min=340)
BOSSES["veyl_chronarch"] = dict(
    side="evil", tier=4, variant=0, name="Veyl, the Chronarch", anime=["Black Clover: the vault-keeper automaton of a time-artificer house"], dnd=["Tarrasque", "Clockwork Colossus", "Modron Primus"],
    forms=["Veyl", "Chronarch Wound Tight", "Every Hour at Once"], resistances=4, legendary_points=4,
    legendary_actions=[("tail_sweep", 1), ("gravity_crush", 2), ("counterspell", 1), ("blink", 1)], lair_actions=["tremor", "falling_stars", "anti_magic_field"],
    anti_magic_aura=False, frightful_aura=False, absorb_magic=False, mythic=True, execution=False, breath="lightning", reflective=True, look="tarrasque",
    mega=dict(damage=1.4, scale=1.2, intro="Every clock in the Vault strikes the same second. The Chronarch unwinds from the centre of the hall.", death="The Chronarch winds down, and for one breath every clock in the Vault shows the same, true hour.", adds="brass_lancer", add_count=3))

# ---------------------------------------------------------------------------- 11. mirrored_halls
pocket(
    id="mirrored_halls", name="Halls of the Reflected King", series="Black Clover: Dungeons (mirror courts)",
    lore="A court that ruled by showing every visitor a better version of themselves, until no one could tell the visitors from the reflections. The King is whichever one is not you.",
    accent="#cfd8e8", sky="#3a4658",
    biome=dict(sky="#445268", fog="#3a4658", water="#8aa8d0", water_fog="#1a2230", particle="minecraft:end_rod", particle_chance=0.02, ambient="minecraft:ambient.soul_sand_valley.loop",
               mood="minecraft:ambient.soul_sand_valley.mood", additions="minecraft:ambient.soul_sand_valley.additions", music="minecraft:music.overworld.stony_peaks", weather="mist",
               soft_ambient="nusmp:pocket.mirrored_halls.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=1, rivers=1, base=1.0),
    palette=dict(wall="minecraft:quartz_bricks", floor="minecraft:smooth_quartz", ceiling="minecraft:chiseled_quartz_block", pillar="minecraft:quartz_pillar",
                 accent="minecraft:white_concrete", light="minecraft:sea_lantern", glass="minecraft:light_gray_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=19, cycles=0.45, boss_half=10), traps=["light", "wind"],
    mobs=[M("mirror_duelist", "Mirror Duelist", FB("radiant_mirror_duelist"), "minecraft:vindicator", "skirmisher", hp=1.3, dmg=1.5, armor=1, speed=1.25, scale=1.05, kb=0.1, glow="#e8f0ff", aura=("minecraft:end_rod", "#e8f0ff"), weight=5, g=(2, 4)),
          M("doppel_courtier", "Doppelganger Courtier", FB("doppelganger_masquerade_duelist"), "minecraft:vindicator", "skirmisher", hp=1.2, dmg=1.4, armor=0, speed=1.2, scale=1.0, kb=0.0, glow="#b0c0e0", aura=("minecraft:glow", "#b0c0e0"), weight=3, g=(1, 3)),
          M("glass_warden", "Glass Warden", FB("hollow_sunshield_penitent"), "minecraft:iron_golem", "tank", hp=2.0, dmg=1.0, armor=4, speed=0.85, scale=1.2, kb=0.5, glow="#cfd8e8", aura=("minecraft:cloud", "#cfd8e8"), weight=3, g=(1, 2)),
          M("shard_sparrow", "Shard Sparrow", FB("stained_glass_sparrow"), "minecraft:vex", "skirmisher", hp=0.8, dmg=1.2, armor=0, speed=1.3, scale=1.1, kb=0.0, glow="#f0f8ff", aura=("minecraft:glow", "#f0f8ff"), weight=4, g=(2, 4), air=True)],
    boss="reflected_king", theme="spatial",
    seal=dict(magic=["MIRROR", "COPY", "SPATIAL", "ILLUSION", "GLASS"], label="Mirror", force=2400, sigil="Silver Sigil"),
    relics=dict(boss=["reflected_kings_mirrorblade"], treasury=["silvered_sash_of_reflections"], primary="silvered_sash_of_reflections"),
    resonance=["MIRROR", "COPY", "ILLUSION", "SPATIAL"], collapse_seconds=150, weight=8, min_depth=10, floor_min=300)
BOSSES["reflected_king"] = dict(
    side="holy", tier=4, variant=1, name="The Reflected King", anime=["Black Clover: the mirror-king of a vanished court"], dnd=["Vecna", "Mirror Lord", "Mind Flayer Elder"],
    forms=["The Reflected King", "Two Kings in One Hall", "Every Mirror Crowned"], resistances=4, legendary_points=4,
    legendary_actions=[("eye_rays", 2), ("mind_blast", 2), ("counterspell", 1), ("blink", 1)], lair_actions=["searing_light", "anti_magic_field"],
    anti_magic_aura=False, frightful_aura=False, absorb_magic=True, mythic=True, execution=False, breath="lightning", reflective=True, look="vecna",
    mega=dict(damage=1.4, scale=1.1, intro="A hundred mirrors show a hundred kings. One of them is not a reflection, and he is looking at you.", death="The mirrors go dark one by one. The last one shows only you.", adds="mirror_duelist", add_count=4))

# ---------------------------------------------------------------------------- 12. radiant_basilica
pocket(
    id="radiant_basilica", name="Radiant Basilica of the Unfallen", series="Black Clover: Dungeons (light kingdom)",
    lore="A basilica that kept its lamps lit for a thousand years after its congregation was gone. Its seraph never fell, and has been waiting ever since for someone to be worthy of the vault.",
    accent="#ffd860", sky="#5a5030",
    biome=dict(sky="#6a5c38", fog="#5a5030", water="#e8d070", water_fog="#2a2410", particle="minecraft:glow", particle_chance=0.03, ambient="minecraft:ambient.warped_forest.loop",
               mood="minecraft:ambient.cave", additions="minecraft:ambient.warped_forest.additions", music="minecraft:music.overworld.meadow", weather="clear",
               soft_ambient="nusmp:pocket.radiant_basilica.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=0, negated=1, rivers=2, base=1.0),
    palette=dict(wall="minecraft:smooth_quartz", floor="minecraft:polished_diorite", ceiling="minecraft:calcite", pillar="minecraft:chiseled_quartz_block",
                 accent="minecraft:gold_block", light="minecraft:glowstone", glass="minecraft:yellow_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=5, rooms=15, cycles=0.4, boss_half=11), traps=["light", "fire"],
    mobs=[M("choir_knight", "Choir Knight", FB("holy_knight_grand_cross"), "minecraft:vindicator", "brute", hp=1.8, dmg=1.3, armor=3, speed=0.95, scale=1.15, kb=0.4, glow="#ffd860", aura=("minecraft:glow", "#ffd860"), weight=4, g=(1, 3)),
          M("bell_cantor", "Bell Cantor", FB("vesper_bell_cantor"), "minecraft:witch", "caster", hp=1.0, dmg=1.4, armor=0, speed=1.0, scale=1.05, kb=0.0, glow="#fff0a0", aura=("minecraft:end_rod", "#fff0a0"), weight=3, g=(1, 3)),
          M("dawn_lancer", "Dawn Lancer", FB("starlit_vigil_cavalier"), "minecraft:pillager", "skirmisher", hp=1.4, dmg=1.4, armor=2, speed=1.25, scale=1.1, kb=0.2, glow="#ffe880", aura=("minecraft:small_flame", "#ffe880"), weight=4, g=(2, 3)),
          M("seraph_scout", "Seraph Scout", FB("celestial_choir_seraphim_scout"), "minecraft:phantom", "caster", hp=1.0, dmg=1.3, armor=0, speed=1.2, scale=1.1, kb=0.0, glow="#fffbd0", aura=("minecraft:end_rod", "#fffbd0"), weight=4, g=(2, 3), air=True)],
    boss="auriel_unfallen_seraph", theme="radiant",
    seal=dict(magic=["LIGHT", "SWORD", "REINFORCEMENT", "STEEL", "DEMON_LIGHT"], label="Light", force=2600, sigil="Dawn Sigil"),
    relics=dict(boss=["aurliels_seraph_halberd"], treasury=["basilica_halo"], primary="basilica_halo"),
    resonance=["LIGHT", "SWORD", "STEEL", "REINFORCEMENT"], collapse_seconds=150, weight=8, min_depth=30, floor_min=380)
BOSSES["auriel_unfallen_seraph"] = dict(
    side="holy", tier=5, variant=0, name="Auriel, Seraph of the Unfallen", anime=["Black Clover: the seraph-guardian of a light kingdom's basilica"], dnd=["Solar", "Deva", "Planetar"],
    forms=["Auriel", "Seraph Ascendant", "Six Wings of Dawn"], resistances=5, legendary_points=5,
    legendary_actions=[("wing_buffet", 1), ("dragon_breath", 2), ("frightful_presence", 2), ("cantrip", 1), ("counterspell", 1)], lair_actions=["searing_light", "falling_stars"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="lightning", reflective=False, look="solar",
    mega=dict(damage=1.5, scale=1.15, intro="Every lamp in the Basilica burns white at once. Auriel looks at you with eyes that have never closed, and does not yet decide.", death="Auriel bows her head, and the light she wore settles on the floor like snow.", adds="seraph_scout", add_count=4))

# ---------------------------------------------------------------------------- 13. venom_hive
pocket(
    id="venom_hive", name="Venom Hive of the Hollow Queen", series="Black Clover: Dungeons (insect empires)",
    lore="An empire of insects that outlived its builders and its own queen's patience. The hive's rivers are sweet and wrong, its floors are wax, and it is always, always listening.",
    accent="#8be04a", sky="#1c3a12",
    biome=dict(sky="#244a18", fog="#1c3a12", water="#6ac84a", water_fog="#0c200a", particle="minecraft:warped_spore", particle_chance=0.05, ambient="minecraft:ambient.crimson_forest.loop",
               mood="minecraft:ambient.crimson_forest.mood", additions="minecraft:ambient.crimson_forest.additions", music="minecraft:music.overworld.swamp", weather="spores",
               soft_ambient="nusmp:pocket.venom_hive.ambient", soft_crackle=SOFT_CRACKLE),
    gravity=dict(inverted=1, negated=0, rivers=1, base=1.0),
    palette=dict(wall="minecraft:mud_bricks", floor="minecraft:packed_mud", ceiling="minecraft:mud_bricks", pillar="minecraft:stripped_mangrove_log",
                 accent="minecraft:honeycomb_block", light="minecraft:shroomlight", glass="minecraft:lime_stained_glass", liquid="minecraft:water"),
    layout=dict(grid=7, rooms=19, cycles=0.45, boss_half=10), traps=["poison", "plant"],
    mobs=[M("hive_drone", "Hive Drone", FB("maggot_hive_gourmand"), "minecraft:cave_spider", "brute", hp=1.5, dmg=1.3, armor=1, speed=1.0, scale=1.1, kb=0.2, glow="#8be04a", aura=("minecraft:sneeze", "#8be04a"), weight=5, g=(2, 4)),
          M("venom_dancer", "Venom Dancer", FB("lamia_venom_dancer"), "minecraft:spider", "skirmisher", hp=1.1, dmg=1.5, armor=0, speed=1.3, scale=1.05, kb=0.0, glow="#c8ff6a", aura=("minecraft:warped_spore", "#c8ff6a"), weight=4, g=(1, 3)),
          M("brood_priest", "Brood Priest", FB("slaughter_moth_hive_priest"), "minecraft:witch", "caster", hp=1.1, dmg=1.3, armor=0, speed=0.95, scale=1.1, kb=0.0, glow="#a0e050", aura=("minecraft:spore_blossom_air", "#a0e050"), weight=3, g=(1, 2)),
          M("wax_bloater", "Wax Bloater", FB("bloat_fly_courier"), "minecraft:vex", "skirmisher", hp=1.0, dmg=1.2, armor=0, speed=1.2, scale=1.15, kb=0.0, glow="#d8f08a", aura=("minecraft:falling_nectar", "#d8f08a"), weight=4, g=(2, 4), air=True)],
    boss="vess_hollow_queen", theme="spider_warrens",
    seal=dict(magic=["POISON", "BEAST", "PLANT", "POISON_PLANT", "FUNGUS"], label="Venom", force=2200, sigil="Hive Sigil"),
    relics=dict(boss=["broodqueens_stinger"], treasury=["hivemind_carapace_pendant"], primary="hivemind_carapace_pendant"),
    resonance=["POISON", "BEAST", "POISON_PLANT", "FUNGUS"], collapse_seconds=150, weight=10, min_depth=0, floor_min=80)
BOSSES["vess_hollow_queen"] = dict(
    side="evil", tier=3, variant=1, name="Vess, Queen of the Hollow Hive", anime=["Black Clover: the broodmother of a forgotten insect empire"], dnd=["Lolth", "Drider Matriarch", "Yochlol"],
    forms=["Vess", "Queen Unsealed", "The Hive Entire"], resistances=3, legendary_points=4,
    legendary_actions=[("vampiric_bite", 1), ("tail_sweep", 1), ("dragon_breath", 2), ("mind_blast", 2)], lair_actions=["necrotic_mist", "tremor"],
    anti_magic_aura=False, frightful_aura=True, absorb_magic=False, mythic=True, execution=False, breath="poison", reflective=False, look="lolth",
    mega=dict(damage=1.4, scale=1.15, intro="The hive stops humming. Something very large shifts in the wax, and many small things turn to face you.", death="The hum goes out of the hive, a note at a time, until there is only silence.", adds="venom_dancer", add_count=4))

# the order of the pockets is the order of their ids in the pool (never reorder: a saved pocket names its id, but the docs list this order)
IDS = [p["id"] for p in POCKETS]
