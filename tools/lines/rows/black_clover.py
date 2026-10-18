"""Black Clover skill lines (25): squads, roles and power families of the Clover Kingdom and beyond.
Every line is ONE recognisable technique family a player learns from a book; the numbers stay inside tools/lines/catalog.py.
"""
from lines_lib import Line, Stance, Stat, Art, Pas


def T(n, d, i, **p):
    return Stance(n, d, i, p)


def N(n, d, attr, amt, i, op="add"):
    return Stat(n, d, attr, amt, op, i)


def A(n, d, k, i, **p):
    return Art(n, d, k, p, i)


def P(n, d, k, i, **p):
    return Pas(n, d, k, p, i)


def line(id, name, desc, color, bg, icon, book, lore, st, sts, arts, pas, fin):
    return Line(id, name, "black_clover", desc, color, bg, icon, book, lore, st, sts, arts, pas, fin)


LINES = []

# ---------------------------------------------------------------- 1 to 5
LINES += [
    line("mana_skin_brawler", "Mana Skin Brawler",
         "Wrap your body in raw mana like a second hide and fight with fists alone. No grimoire spell, only muscle, mana flow and stubbornness.",
         "#c8553d", "iron", "minecraft:iron_chestplate", "Mana Skin Primer", "A beaten-up manual on pushing mana out through the skin instead of through a spell.",
         [T("Mana Skin Wrap", "Mana hardens over your body: tougher and steadier on your feet, but a little slower.", "minecraft:iron_chestplate",
            armor=4, knockback_res=.3, speed_pct=-.05, vfx="SPIRIT_AURA"),
          T("Overflowing Fist Stance", "Let the mana pour into your arms: heavier punches and sharper crits, but a thinner hide.", "minecraft:iron_axe",
            damage_pct=.25, crit_chance=.1, armor=-3, vfx="FLAME_EXPLOSION")],
         [N("Reserve Core", "Training the mana core in your gut adds to your vitality.", "max_health", 2, "minecraft:golden_apple"),
          N("Fist Weight", "Mana in the knuckles makes every punch land heavier.", "attack_damage", 1, "minecraft:iron_ingot"),
          N("Planted Feet", "A body that refuses to be pushed around.", "knockback_resistance", .1, "minecraft:anvil"),
          N("Hardened Hide", "Mana skin that soaks the hits.", "armor_toughness", 1, "minecraft:shield")],
         [A("Mana-Skin Rush", "Burst forward with a mana-wrapped shoulder and bowl over everything in your path.", "dash_strike", "minecraft:leather_boots", vfx="WIND_SLASH", damage=8),
          A("Skin-Cracking Uppercut", "A rising mana fist that launches the foe in front of you off the ground.", "uppercut", "minecraft:slime_ball", vfx="EARTH_SPIKES", damage=9),
          A("Roundhouse Breaker", "Spin on one heel and kick everyone around you several times.", "whirlwind", "minecraft:iron_boots", vfx="WIND_RING", damage=4, hits=5),
          A("Footfall Shockwave", "Stamp the ground and send a crack of mana roaring down a line in front of you.", "ground_wave", "minecraft:stone", vfx="STONE_SPIKES", damage=8),
          A("Mana Skin Flare", "Flush your whole body with mana for a short strength and resistance surge.", "buff_self", "minecraft:blaze_powder", effect="strength", effect2="resistance", amp=0, vfx="SPIRIT_AURA"),
          A("Pressure Burst", "Release the held mana in a pulse that freezes the creatures around you in place.", "stun_ring", "minecraft:heavy_core", vfx="WIND_RING"),
          A("Finishing Knuckle", "A brutal straight punch that hits far harder the weaker the foe already is.", "execute", "minecraft:netherite_ingot", vfx="FIRE_BURST", damage=10)],
         [P("Rhythm of Fists", "Land several punches in a row and the next one hits much harder.", "combo", "minecraft:clock", hits=4),
          P("Brawler's Resolve", "While you are in a fight your mana skin takes a share of every blow.", "reduce_when", "minecraft:iron_helmet", condition="in_combat"),
          P("Concussive Knuckles", "Your punches may shove the foe a step back.", "knockback_on_hit", "minecraft:piston", strength=.5),
          P("Building Heat", "Every hit in a row makes your next one a little stronger until you stop fighting.", "momentum", "minecraft:blaze_rod"),
          P("Unbroken Core", "When you are nearly dead the mana core flares and shields you for a few seconds.", "last_stand", "minecraft:totem_of_undying"),
          P("Victory Roar", "A kill heals you and fills you with fighting strength.", "kill_reward", "minecraft:bone", effect="strength", heal=3),
          P("Skin Mending", "Mana quietly closes your wounds, even out of combat.", "mending", "minecraft:honey_bottle"),
          P("Charging Bull Rush", "You hit harder while sprinting, as if the run itself were a punch.", "sprint_bonus", "minecraft:sugar")],
         [A("Mana Meteor Fist", "Leap high and slam down with a mana-blazing fist that cracks the ground and everything around the landing.", "leap_slam", "minecraft:magma_block",
            vfx="FLAME_EXPLOSION", damage=30, radius=5, knockback=1.5, cooldown=1200, cost=.05),
          A("Thousand Fist Storm", "A blur of punches in every direction around you, hundreds of mana-wrapped hits in a few seconds.", "whirlwind", "minecraft:netherite_sword",
            vfx="WIND_TORNADO", damage=8, hits=7, radius=4.5, cooldown=1300, cost=.05)]),

    line("mana_zone_sentinel", "Mana Zone Sentinel",
         "Spread your mana into the air around you and feel every living thing that moves inside it. You never fight blind: you read the strike before it comes.",
         "#4fb5c9", "dream", "minecraft:recovery_compass", "Zone Perception Notes", "Written by a scout who swore she could hear a bowstring drawn three hills away.",
         [T("Zone Spread", "Thin your mana across the area: you move lighter and see further, but your skin is bare.", "minecraft:spyglass",
            speed_pct=.15, armor=-2, crit_chance=.05, vfx="SPIRIT_AURA"),
          T("Zone Contracted", "Pull the zone tight against your body: steadier guard and a little regeneration, at the cost of speed.", "minecraft:shield",
            armor=4, regen=.5, speed_pct=-.1, vfx="MAGIC_CIRCLE")],
         [N("Sensing Reach", "The zone reaches a little further for your strikes.", "entity_interaction_range", .5, "minecraft:spyglass"),
          N("Light Step", "Reading the ground lets you move faster.", "movement_speed", .04, "minecraft:feather", op="mult"),
          N("Fortune Reader", "Sensing trouble early nudges luck your way.", "luck", 1, "minecraft:rabbit_foot")],
         [A("Pinpoint Sense", "Lock your zone onto one foe: it glows in your mind and takes extra damage for a while.", "mark", "minecraft:ender_eye", vfx="MAGIC_CIRCLE", range=24, bonus=.3),
          A("Zone Step", "Slip to where the zone says the gap is: a short teleport forward.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=9),
          A("Pressure Pulse", "Push a pulse of mana through a cone ahead that slows everything caught in it.", "debuff_cone", "minecraft:cobweb", effect="slowness", effect2="weakness", seconds2=4, vfx="WIND_SLASH"),
          A("Sensed Volley", "Throw a spread of mana darts at the foe you look at, each aimed by the zone.", "barrage", "minecraft:arrow", vfx="WIND_SLASH", shots=6, damage=3),
          A("Reading the Strike", "For a few seconds a share of the damage you take is thrown back at the attacker.", "ward_reflect", "minecraft:iron_bars", vfx="MIRROR_PANE"),
          A("Veiled Mana", "Thin your presence to nothing: creatures around you lose sight of you.", "smoke", "minecraft:ink_sac", vfx="SHADOW_POOL"),
          A("Long Sense Shot", "Fire a thin line of focused mana along your sight line, piercing everything.", "ray_shot", "minecraft:spectral_arrow", vfx="LIGHTNING_SPEAR", range=24, pierce=2)],
         [P("Dodge by Sensing", "You may slip a blow completely because you felt it coming.", "dodge", "minecraft:leather_boots"),
          P("Night Eyes of the Zone", "At night the zone shows you the dark: you have night vision.", "self_effect_when", "minecraft:golden_carrot", effect="night_vision", condition="night"),
          P("Radar Veil", "Enemies near you glow, shown by your zone.", "aura", "minecraft:glow_ink_sac", effect="glowing", target="enemies", radius=9),
          P("Weak Point Reading", "You see the seams: your blows may strike critically.", "crit", "minecraft:iron_sword"),
          P("Spotting the Dying", "You deal far more damage to foes that are nearly dead.", "execute_threshold", "minecraft:flint"),
          P("Studied Opponents", "Reading fights teaches you faster: kills give more experience.", "xp_bonus", "minecraft:experience_bottle"),
          P("Calm Breath", "While you stand still the zone steadies your breathing and you heal slowly.", "regen_when", "minecraft:honeycomb", condition="sneaking")],
         [A("Mana Zone: Total Domain", "Flood the whole area with your mana and freeze every creature around you in place for a heartbeat longer than a spell should allow.", "stun_ring", "minecraft:beacon",
            vfx="MAGIC_CIRCLE", radius=8, seconds=4, targets=7, cooldown=1300, cost=.06),
          A("Hundred-Step Reading", "Predict every dodge and chase your strike through one foe after another.", "chain_hit", "minecraft:chain",
            vfx="LIGHTNING_FIEND", damage=22, jumps=5, jump_range=8, cooldown=1200, cost=.05)]),

    line("black_bulls_vanguard", "Black Bulls Vanguard",
         "The reckless frontliner of a squad of misfits: draw the whole battlefield onto yourself, laugh at the damage and hit back twice as hard. Strength through spite.",
         "#3a3340", "city", "minecraft:black_banner", "Hideout Brawl Handbook", "A battered log from a squad hideout that is half battle plan, half bar tab.",
         [T("Reckless Charge", "Give up your guard for a harder punch and a faster stride.", "minecraft:iron_axe", damage_pct=.3, speed_pct=.1, armor=-4, vfx="WIND_SLASH"),
          T("Hold the Line", "Plant yourself and become the wall your squad leans on.", "minecraft:shield", armor=6, knockback_res=.5, speed_pct=-.15, damage_pct=-.1, vfx="SEAL_CHAINS")],
         [N("Bottomless Guts", "A body that keeps standing after everyone else would not.", "max_health", 4, "minecraft:cooked_beef"),
          N("Heavy Hand", "Brawls toughen the fists.", "attack_damage", 1.5, "minecraft:iron_axe"),
          N("Bull's Neck", "Hard to push around.", "knockback_resistance", .1, "minecraft:anvil")],
         [A("Rowdy Challenge", "Bellow an insult that draws every hostile creature nearby onto you.", "taunt", "minecraft:goat_horn", vfx="DEVIL_CIRCLE", radius=10, targets=6),
          A("Bullhorn Charge", "Lower your head and run through a line of foes, goring everyone in your path.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=9, distance=8),
          A("Squad Brawl Leap", "Jump into the crowd and land with a crunch that throws foes aside.", "leap_slam", "minecraft:hay_block", vfx="EARTH_SPIKES", damage=9, knockback=1.2),
          A("Barroom Sweep", "A wide swinging blow into the cone of foes in front of you.", "cone_strike", "minecraft:wooden_sword", vfx="WIND_RING", damage=8, half_angle=60, targets=5),
          A("Raider's Feast", "Bite into a foe in melee and heal on the flesh you take back.", "drain_bite", "minecraft:cooked_porkchop", vfx="FIRE_BURST", damage=8, heal_pct=.4),
          A("Hideout Cheer", "Shout the squad's battle cry: you and your allies around you gain strength.", "buff_aura", "minecraft:cake", effect="strength", vfx="SPIRIT_AURA", radius=9),
          A("Spite Haymaker", "A telegraphed, devastating punch that hits much harder against a wounded foe.", "execute", "minecraft:mace", vfx="FLAME_EXPLOSION", damage=12)],
         [P("Lucky Last Gasp", "Under a quarter of your health you hit much harder.", "dmg_when", "minecraft:redstone", condition="low_health", bonus=.4),
          P("Bar Fight Appetite", "A share of the damage you deal comes back to you as health.", "lifesteal", "minecraft:sweet_berries"),
          P("Spiked Pride", "Whoever strikes you takes some of the blow back.", "thorns", "minecraft:cactus"),
          P("Stubborn to the End", "When you are nearly dead you are shielded for a few seconds.", "last_stand", "minecraft:totem_of_undying"),
          P("Monster Hunter's Grudge", "You deal more damage to boss-class creatures.", "dmg_vs", "minecraft:wither_skeleton_skull", category="boss", bonus=.2),
          P("Never Backing Down", "The longer the brawl, the harder you hit: blows in a row grow stronger.", "momentum", "minecraft:blaze_powder"),
          P("Squad Spirit", "The people close to you fight with more fire in them.", "aura", "minecraft:red_banner", effect="strength", target="allies", radius=8)],
         [A("Underdog Overdrive", "Throw everything you have into one crushing sweep that sends a whole cone of foes flying.", "cone_strike", "minecraft:netherite_axe",
            vfx="FLAME_EXPLOSION", damage=34, range=6, half_angle=60, targets=6, knockback=1.5, cooldown=1250, cost=.05),
          A("All Hands Brawl", "Call the whole squad's fury down around you: foes nearby are torn at again and again.", "whirlwind", "minecraft:red_banner",
            vfx="DEVIL_CIRCLE", damage=8, hits=7, radius=5, cooldown=1200, cost=.05)]),

    line("crimson_lion_blazeguard", "Crimson Lion Blazeguard",
         "A noble knight of a house of flame: proud, scorching and shielding the people at your back. Every battle is an exhibition of lion-hearted heat.",
         "#d6402a", "magma", "minecraft:fire_charge", "Pride of the Crimson Lion", "A noble house's drill book: how to burn brightly without burning your friends.",
         [T("Lion's Pride", "Burn hotter: heavy damage and a flaming speed, with a light armor.", "minecraft:blaze_powder", damage_pct=.25, speed_pct=.05, armor=-2, crit_chance=.05, vfx="FIRE_SPIRAL"),
          T("Ember Guard", "Bank the flame inside your armor: tough, steady and warm.", "minecraft:iron_chestplate", armor=5, regen=.4, damage_pct=-.1, speed_pct=-.05, vfx="FLAME_EXPLOSION")],
         [N("Hearth in the Chest", "A noble's discipline gives you endurance.", "max_health", 2, "minecraft:golden_apple"),
          N("Noble Ember", "Flame in the blows.", "attack_damage", 1, "minecraft:blaze_rod"),
          N("Gilded Plate", "The family armor, polished.", "armor", 2, "minecraft:golden_chestplate"),
          N("Sure Footing", "A knight does not fall.", "knockback_resistance", .1, "minecraft:chainmail_boots")],
         [A("Flame Crescent", "Sweep a crescent of fire across the cone in front of you.", "cone_strike", "minecraft:flint_and_steel", vfx="FIRE_BURST", damage=8, half_angle=55),
          A("Lion's Mane Burst", "Detonate a ring of flame around you that pushes everything back.", "ring_burst", "minecraft:fire_charge", vfx="FLAME_EXPLOSION", damage=7, radius=4),
          A("Ember Volley", "Shoot a rapid fan of fire bolts at the foe you look at.", "barrage", "minecraft:blaze_rod", vfx="FIRE_SPIRAL", damage=3, shots=7),
          A("Wildfire Run", "Race forward in a trail of flame, scorching everything you pass.", "dash_strike", "minecraft:magma_cream", vfx="FLAME_TRAIL", damage=7),
          A("Kindled Pride", "Set your spirit alight: strength and fire resistance for a time.", "buff_self", "minecraft:blaze_powder", effect="strength", effect2="fire_resistance", vfx="FIRE_SPIRAL"),
          A("Sunfall Comet", "Call a ball of flame down on the place you look at after a short delay.", "meteor_drop", "minecraft:fire_charge", vfx="FLAME_EXPLOSION", damage=14, radius=4.5),
          A("Rally Flame", "Warm your allies around you with healing fire.", "heal_pulse", "minecraft:campfire", vfx="FIRE_BURST", amount=6)],
         [P("Burning Zeal", "While you are on fire you hit much harder.", "dmg_when", "minecraft:lava_bucket", condition="on_fire", bonus=.35),
          P("Smelting Hide", "Fire damage barely hurts you.", "resist", "minecraft:magma_block", damage_kind="fire", pct=.4),
          P("Scorching Scorn", "You deal more damage to creatures that are already aflame.", "dmg_vs", "minecraft:coal", category="burning", bonus=.25),
          P("Furnace Veins", "While you burn you heal slowly and regain strength.", "regen_when", "minecraft:furnace", condition="on_fire", per_second=.8),
          P("Dominant Blaze", "Your blows may weaken the target with a blast of heat.", "on_hit_effect", "minecraft:blaze_rod", effect="weakness", chance=.25),
          P("Roaring Lion", "An aura of warmth makes your allies stronger.", "aura", "minecraft:red_banner", effect="strength", target="allies"),
          P("Banked Fires", "When the flame is almost out you are shielded for a few seconds.", "last_stand", "minecraft:totem_of_undying")],
         [A("Solar Lion's Crown", "A towering pillar of sunfire crashes onto the place you look at, scorching a huge circle.", "meteor_drop", "minecraft:nether_star",
            vfx="FIRE_SPIRAL", damage=36, radius=7, delay=24, cooldown=1300, cost=.06),
          A("Crimson Lion Roar", "A shockwave of flame in the shape of a lion's roar bursts out of you in every direction.", "ring_burst", "minecraft:magma_block",
            vfx="FLAME_EXPLOSION", damage=26, radius=8, targets=8, knockback=2, cooldown=1200, cost=.05)]),

    line("golden_dawn_prodigy", "Golden Dawn Prodigy",
         "The elite of the elite: refined technique, effortless composure and a reservoir of mana that mocks everyone else. Every spell lands clean and on time.",
         "#e5b93c", "holy", "minecraft:golden_helmet", "Dawn Academy Compendium", "Gilded-edge lessons from an academy that only accepts the gifted.",
         [T("Gilded Composure", "Perfect poise: crits come easier and your guard is up, but you swing slowly.", "minecraft:golden_helmet", crit_chance=.12, armor=3, speed_pct=-.05, vfx="MAGIC_CIRCLE"),
          T("Dawn Flourish", "Unleash the talent: strong, quick strikes with the guard dropped.", "minecraft:golden_sword", damage_pct=.25, speed_pct=.1, armor=-3, vfx="SPIRIT_AURA")],
         [N("Overflowing Reserve", "A mana pool that never seems to run out; your body keeps up.", "max_health", 2, "minecraft:enchanted_golden_apple"),
          N("Polished Technique", "Training gives your blows refined weight.", "attack_damage", 1, "minecraft:golden_sword"),
          N("Dawn Pace", "A trained, easy gait.", "movement_speed", .05, "minecraft:golden_boots", op="mult"),
          N("Elite Plate", "Academy-grade finery that turns blows.", "armor", 2, "minecraft:golden_chestplate")],
         [A("Radiant Lance", "Fire a thin bar of golden light along your sight line that pierces through foes.", "ray_shot", "minecraft:spectral_arrow", vfx="LIGHTNING_SPEAR", damage=9, range=22, pierce=2),
          A("Dawn Step", "Appear a little ahead of where you were, leaving a flash of gold behind.", "blink", "minecraft:glowstone_dust", vfx="SPATIAL_RIFT", distance=10),
          A("Echoing Prism", "A prism of light flares around you, hitting everything near and throwing it back.", "ring_burst", "minecraft:amethyst_shard", vfx="MIRROR_PANE", damage=8, radius=4.5),
          A("Gilded Aegis", "Raise a golden ward that soaks up damage.", "shield", "minecraft:golden_apple", vfx="MAGIC_CIRCLE", absorb_hearts=5),
          A("Cascade of Rays", "Rain light bolts on the foe you look at in a fast barrage.", "barrage", "minecraft:glowstone", vfx="LIGHTNING_SPEAR", damage=4, shots=6),
          A("Brilliant Chain", "A bolt of light that arcs from foe to foe.", "chain_hit", "minecraft:lightning_rod", vfx="LIGHTNING_FIEND", damage=7),
          A("Wise Mending", "Heal yourself with a calm light and regenerate for a while.", "heal_self", "minecraft:glistering_melon_slice", vfx="SPIRIT_AURA", amount=8)],
         [P("Precision Casting", "Your blows may strike critically; they are well aimed.", "crit", "minecraft:iron_sword", chance=.15, mult=1.8),
          P("Poised in Combat", "You take less damage while at full health: no scratches on elites.", "reduce_when", "minecraft:golden_chestplate", condition="full_health", pct=.15),
          P("Showy Strike", "You deal more damage while at full health: the first blow is the best.", "dmg_when", "minecraft:gold_ingot", condition="full_health"),
          P("Dawn Breath", "You tire more slowly than other fighters.", "hunger_saver", "minecraft:golden_carrot"),
          P("Overflowing Tide", "A share of the damage you deal returns as health.", "lifesteal", "minecraft:glistering_melon_slice"),
          P("Gifted Student", "Kills give more experience: you learn quickly.", "xp_bonus", "minecraft:experience_bottle"),
          P("Refined Evasion", "You may avoid a blow entirely with a graceful step.", "dodge", "minecraft:feather")],
         [A("Radiant Dawn Break", "Gather golden mana overhead, then drop it as a pillar of light on the place you look at.", "meteor_drop", "minecraft:beacon",
            vfx="MAGIC_CIRCLE", damage=34, radius=6, delay=20, cooldown=1250, cost=.06),
          A("Golden Sunburst Ray", "A tremendous beam of gold that scours a long line ahead of you.", "ray_shot", "minecraft:end_rod",
            vfx="LIGHTNING_SPEAR", damage=36, range=30, pierce=3, cooldown=1200, cost=.05)]),
]
