"""Fairy Tail: Evolution Unique skill lines (3.26.0): Gray's Ice-Make and Erza's Requip: The Knight.

Source (rule 10): the English Fairy Tail wiki, read through search digests only (docs/skills/research/fairy_tail.md sections 1 and 3; the wiki pages themselves could not be opened
and no Japanese wiki page was reached). Items that research file marks UNVERIFIED / NOT FOUND (magic costs, Sea Empress effects, Nakagami's origin, Hammer Shield, Eagle Geyser, ...)
are not used. Natsu Dragneel is excluded by the owner.

Canon note: names follow the story's vocabulary; every numeric effect and wording is this mod's own design. The Evolution requirement and Ultimate names are fixed by docs/skills/evolution_ten.md.
Format: Passive-Toggle = stance 1, Active Held = stance 2, Active Press = the arts, Active Crouch = the "<Art>: Shift" arts, Debt = the `debt_store` passive, Ultimate = the last finisher (`debt_burst`).
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "fairy_tail"


def A(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon)


def U(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon, ultimate=True)


def P(name, desc, kind, icon, evolved=None, **p):
    return Pas(name, desc, kind, p, icon, evolved or {})


def S(name, desc, icon, **p):
    return Stance(name, desc, icon, p)


def T(name, desc, attribute, amount, icon, op="add"):
    return Stat(name, desc, attribute, amount, op, icon)


LINES = []

# ---------------------------------------------------------------------------------------------------------------------------------- 1 (design row 7)
LINES.append(Line(
    id="gray_ice_make", name="Ice-Make", franchise=F,
    desc="Gray Fullbuster's two-handed ice sorcery: shape lances, floors, hammers and saucers out of cold, then grow into the Ice Devil Slayer's blizzard. "
         "Frostbite stores the cold on your foes and lets it bite later, until the Iced Shell seals everything in absolute zero.",
    color="#8fd8ff", background="frost", icon="minecraft:blue_ice",
    book_name="Codex of Static Ice-Make", book_lore="Cast with both hands, the way Ur taught: whatever you can picture, you can freeze into shape.",
    evolution={"requirement": "damage_dealt", "count": 50000,
               "text": "Deal 50,000 damage to hostile creatures while Ice-Make is switched on to awaken the Ice Devil Slayer and unlock the Iced Shell.",
               "ultimate_name": "Ultimate Skill, Iced Shell: Absolute Zero"},
    stances=[
        S("Static Ice-Make Stance", "Cold gathers around your hands: your blows hit harder and may strike critically, while your footing grows a little light.",
          "minecraft:snowball", damage_pct=0.18, crit_chance=0.1, speed_pct=-0.05, vfx="WATER_RING"),
        S("Ice Shield Posture", "Raise a flower of ice before you: much harder to hurt and to push around, but slow to move and weaker in attack.",
          "minecraft:shield", armor=7, knockback_res=0.5, speed_pct=-0.15, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Ur's Discipline", "Years of cold training toughen your body.", "max_health", 2, "minecraft:packed_ice"),
        T("Frost-Hardened Skin", "A layer of cold hardens your hide.", "armor", 1, "minecraft:iron_chestplate"),
        T("Quick Hands", "Casting with both hands makes you strike faster.", "attack_speed", 0.05, "minecraft:snowball", op="mult"),
        T("Fighter's Stride", "A nimble all-rounder is quick on his feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Ice-Make: Lance", "Hurl a long ice lance that pierces the foes along your line of sight.", "ray_shot", "minecraft:trident",
          damage=7, range=18, pierce=2, vfx="WIND_SLASH"),
        A("Ice-Make: Floor", "Freeze the ground around you: every creature on it is held in place for a moment.", "stun_ring", "minecraft:ice",
          radius=5, seconds=2, targets=6, vfx="WATER_RING"),
        A("Ice-Make: Hammer", "A great ice hammer forms above the place you look at, then drops on it after a short delay.", "meteor_drop", "minecraft:anvil",
          damage=13, radius=4, delay=24, vfx="MIRROR_PANE"),
        A("Ice-Make: Saucer", "Launch a spinning ice disc that saws through one foe and leaps on to the next.", "chain_hit", "minecraft:prismarine_shard",
          damage=6, jumps=3, vfx="WIND_RING"),
        A("Ice-Make: Geyser", "Freeze the ground ahead and raise a line of ice spikes that impales everything in it.", "ground_wave", "minecraft:powder_snow_bucket",
          damage=7, length=11, width=2, vfx="STONE_SPIKES"),
        A("Ice Devil's Rage", "Roar a blizzard from your mouth across a cone: foes are slowed and their strength drains away.", "debuff_cone", "minecraft:snow_block",
          effect="slowness", effect2="weakness", seconds=6, seconds2=6, range=6, vfx="POISON_BREATH"),
        A("Ice-Make: Lance: Shift", "Crouch to cast many lances at once: a rapid volley at the foe you look at.", "barrage", "minecraft:spectral_arrow",
          damage=3, shots=6, range=16, vfx="WIND_SLASH"),
        A("Ice-Make: Cannon: Shift", "Crouch to build a small ornate ice cannon that blasts a heavy shot through the foes ahead.", "ray_shot", "minecraft:blue_ice",
          damage=11, range=14, pierce=2, cooldown=170, vfx="WATER_BURST"),
    ],
    passives=[
        P("Frostbite", "Your blows store cold damage on the foe (Debt). It keeps biting while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:heart_of_the_sea",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WATER_RING", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Chilling Touch", "Your blows may slow the foe with creeping cold.", "on_hit_effect", "minecraft:snowball",
          effect="slowness", seconds=3, chance=0.25, evolved={"chance": 0.5, "seconds": 6}),
        P("Cold-Born Hide", "The cold barely hurts you: you take less freezing damage.", "resist", "minecraft:leather_chestplate",
          damage_kind="freeze", pct=0.25),
        P("Two-Handed Casting", "Every few blows in a row land with extra force, as both hands work together.", "combo", "minecraft:iron_sword",
          hits=4, bonus=0.5),
        P("Ice Eater's Appetite", "Devil Slayer magic feeds on cold: standing in water heals you.", "regen_when", "minecraft:prismarine_crystals",
          condition="in_water", per_second=0.7),
        P("Unburdened Ice-Maker", "Without armor to weigh you down, your blows hit harder.", "dmg_when", "minecraft:iron_helmet",
          condition="unarmored", bonus=0.2, evolved={"bonus": 0.4}),
        P("Devil Slayer's Bane", "Ice Devil Slayer magic is deadly to monsters: you deal more damage to them.", "dmg_vs", "minecraft:wither_skeleton_skull",
          category="monster", bonus=0.15),
    ],
    finishers=[
        A("Ice-Make: Unlimited", "One-Sided Chaotic Dance: a storm of countless ice swords bursts out around you and cuts down every foe nearby.", "ring_burst", "minecraft:diamond_sword",
          damage=22, radius=7, targets=8, knockback=1.2, cooldown=1400, vfx="WIND_TORNADO"),
        U("Iced Shell: Absolute Zero", "Cross your arms and seal the world in ice: every foe around you takes a huge blow plus many times all the frostbite stored on it.", "debt_burst", "minecraft:nether_star",
          damage=34, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="WATER_RING"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 2 (design row 8)
LINES.append(Line(
    id="erza_requip_knight", name="Requip: The Knight", franchise=F,
    desc="Erza Scarlet's Requip: swap armor and weapon in the middle of a fight, one pocket dimension of gear at a time. "
         "Each swap leaves a blow waiting on the foe, and a long road of kills ends in the Armadura Fairy and its rain of swords.",
    color="#c8203c", background="iron", icon="minecraft:netherite_chestplate",
    book_name="Ledger of the Requip Knight", book_lore="Every blade she needs is already waiting in the pocket dimension: she only has to call it.",
    evolution={"requirement": "kills", "count": 1000,
               "text": "Defeat 1,000 creatures while Requip: The Knight is switched on to earn the Armadura Fairy and unlock the Heaven's Wheel.",
               "ultimate_name": "Ultimate Skill, Armadura Fairy: Heaven's Wheel"},
    stances=[
        S("Heart Kreuz Stance", "Swap into the knight's plate: sturdier and a stronger swordhand, but a little heavier on your feet.",
          "minecraft:iron_chestplate", armor=4, damage_pct=0.12, speed_pct=-0.04, vfx="WEAPON_CONSTRUCTS"),
        S("Adamantine Guard", "Hold the heaviest armor: far harder to hurt and to push around, but slow and your blows are weaker.",
          "minecraft:shield", armor=9, knockback_res=0.6, speed_pct=-0.2, damage_pct=-0.12, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Titania's Blade-Arm", "Constant sword drills sharpen every cut.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Pocket Dimension Plate", "Armor stored in another space settles on your frame.", "armor", 1, "minecraft:diamond_chestplate"),
        T("Swift Requip", "Swapping gear mid-fight makes you quick with the blade.", "attack_speed", 0.05, "minecraft:golden_sword", op="mult"),
        T("Fairy's Pride", "Her pride in the guild keeps her standing: more maximum health.", "max_health", 2, "minecraft:golden_apple"),
    ],
    arts=[
        A("Knight's Blade Combo", "A broad sweep of the requipped sword that cuts every foe in front of you.", "cone_strike", "minecraft:iron_sword",
          damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Circle Sword", "Swords spin out around you in a ring and slash everything near.", "ring_burst", "minecraft:diamond_sword",
          damage=7, radius=4, targets=6, knockback=0.8, vfx="WEAPON_CONSTRUCTS"),
        A("Blumenblatt", "Loose a volley of swords at the foe you look at.", "barrage", "minecraft:arrow",
          damage=3, shots=6, range=14, vfx="WEAPON_CONSTRUCTS"),
        A("Armor Swap: Flame Empress", "Requip the Flame Empress armor mid-fight: Fire Resistance and Strength for a while.", "buff_self", "minecraft:fire_charge",
          effect="fire_resistance", effect2="strength", seconds=14, vfx="SPATIAL_RIFT"),
        A("Lightning Empress Spear", "Requip a spear that fires a bolt of lightning through the foes ahead.", "ray_shot", "minecraft:lightning_rod",
          damage=8, range=16, pierce=2, vfx="LIGHTNING_SPEAR"),
        A("Purgatory Hammer", "Requip a spiked hammer, leap and bring it down on the foes below.", "leap_slam", "minecraft:iron_axe",
          damage=9, radius=3.5, height=0.9, vfx="FIRE_BURST"),
        A("Sword: Shift", "Crouch to draw out a longer, narrower blade: a deep cut through the line ahead.", "cone_strike", "minecraft:netherite_sword",
          damage=11, range=6, half_angle=28, targets=4, vfx="WIND_SLASH"),
        A("Requip Rush", "Crouch to swap armor in a flash and charge forward, striking everything in the way.", "dash_strike", "minecraft:iron_boots",
          distance=9, damage=8, targets=4, vfx="SPATIAL_RIFT"),
    ],
    passives=[
        P("Blades Left Waiting", "Each swap leaves a blow waiting on the foe (Debt). It ticks while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:chain",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WEAPON_CONSTRUCTS", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Knight's Plate", "You take less damage while holding a sword.", "reduce_when", "minecraft:iron_chestplate",
          condition="holding_sword", pct=0.12, evolved={"pct": 0.25}),
        P("Titania's Edge", "Your blows may strike critically.", "crit", "minecraft:flint",
          chance=0.12, mult=1.6, evolved={"chance": 0.2, "mult": 2.0}),
        P("Flame Empress Plating", "The Flame Empress armor halves the fire that reaches you: you take less fire damage.", "resist", "minecraft:blaze_powder",
          damage_kind="fire", pct=0.3),
        P("Lightning Empress Plating", "The Lightning Empress armor turns aside the storm: you take less lightning damage.", "resist", "minecraft:lightning_rod",
          damage_kind="lightning", pct=0.3),
        P("Swordswoman's Rhythm", "Every few sword blows in a row land with extra force.", "combo", "minecraft:stone_sword",
          hits=4, bonus=0.55, evolved={"bonus": 1.0}),
        P("Pandemonium Veteran", "Each kill heals you and sharpens you with Strength, as when she cut through a horde.", "kill_reward", "minecraft:experience_bottle",
          heal=3, effect="strength", seconds=5),
        P("Monster-Slayer's Habit", "You deal more damage to monsters.", "dmg_vs", "minecraft:zombie_head",
          category="monster", bonus=0.15),
    ],
    finishers=[
        A("Ataraxia Armor: Dancing Blades", "The sword-wings take to the air and dance from foe to foe, striking several in a row.", "chain_hit", "minecraft:netherite_axe",
          damage=14, jumps=5, jump_range=7, cooldown=1300, vfx="WEAPON_CONSTRUCTS"),
        U("Armadura Fairy: Heaven's Wheel", "Requip the strongest armor and rain swords on everything around you: a huge blow plus many times all the Debt stored on each foe.", "debt_burst", "minecraft:nether_star",
          damage=34, mult=3.0, radius=12, targets=10, cost=0.08, cooldown=18000, vfx="WEAPON_CONSTRUCTS"),
    ],
))


# ================================================================================================================================ 3.27.0 batch B (docs/skills/evolution_ten_b.md rows 1-3)
# Source (rule 10): English Fairy Tail wiki, read through WebSearch digests only (the pages could not be opened); one Japanese search added nothing usable
# (docs/skills/research/fairy_tail_b.md). Numbers are the mod's own design; UNVERIFIED items of that file are not used as names.

# ---------------------------------------------------------------------------------------------------------------------------------- B1 (design row 1)
LINES.append(Line(
    id="natsu_fire_dragon", name="Fire Dragon Slayer", franchise=F,
    desc="Natsu Dragneel's Fire Dragon Slayer magic: breathe, punch and ram your way through a fight with flame, eat fire to keep going, and never stay down. "
         "Every blow leaves embers smouldering on the foe, until the Lightning Fire Dragon Mode sets them all off at once.",
    color="#ff5a1f", background="magma", icon="minecraft:blaze_powder",
    book_name="Scale of the Fire Dragon Slayer", book_lore="Igneel's lesson was short: eat the flame, and the flame will do what you tell it.",
    evolution={"requirement": "elite_kills", "count": 150,
               "text": "Defeat 150 elite or boss creatures while Fire Dragon Slayer is switched on to ignite the Lightning Fire Dragon Mode and unlock it.",
               "ultimate_name": "Ultimate Skill, Lightning Fire Dragon Mode"},
    stances=[
        S("Fire Dragon Stance", "Flame rolls over your fists: your blows hit harder and may strike critically, and you run a little hotter and faster.",
          "minecraft:fire_charge", damage_pct=0.2, crit_chance=0.08, speed_pct=0.04, vfx="FIRE_SPIRAL"),
        S("Flame-Eater's Posture", "Plant your feet and swallow the fire around you: you heal steadily and are hard to shift, but your blows are weaker.",
          "minecraft:magma_block", armor=4, regen=1.0, knockback_res=0.4, damage_pct=-0.1, vfx="FLAME_TRAIL"),
    ],
    stats=[
        T("Igneel's Training", "Years of dragon training toughen your frame.", "max_health", 2, "minecraft:golden_apple"),
        T("Dragon Fists", "Fists that live in flame hit harder.", "attack_damage", 1.0, "minecraft:blaze_rod"),
        T("Hot-Blooded Stride", "A fighter who never stops is quick on his feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Salamander's Hide", "Heat-hardened skin turns blows aside.", "armor", 1, "minecraft:iron_chestplate"),
    ],
    arts=[
        A("Fire Dragon's Roar", "Breathe a blast of flame along your line of sight that burns through the foes in front of you.", "ray_shot", "minecraft:fire_charge",
          damage=8, range=16, pierce=2, vfx="FLAME_EXPLOSION"),
        A("Fire Dragon's Iron Fist", "Wrap your fist in flame and punch the foes in front of you.", "cone_strike", "minecraft:blaze_rod",
          damage=9, range=3.5, half_angle=45, targets=3, knockback=0.6, vfx="FIRE_BURST"),
        A("Fire Dragon's Wing Attack", "Sweep both arms out in a wave of flame that hits everything around you.", "ring_burst", "minecraft:feather",
          damage=6, radius=4.5, targets=6, knockback=1.0, vfx="FIRE_SPIRAL"),
        A("Fire Dragon's Sword Horn", "Set your head alight and ram forward, striking everything in the way.", "dash_strike", "minecraft:iron_sword",
          distance=8, damage=8, targets=4, vfx="FLAME_TRAIL"),
        A("Fire Dragon's Claw", "A flaming kick that launches the foe in front of you into the air.", "uppercut", "minecraft:magma_cream",
          damage=8, range=3.2, launch=0.9, vfx="FIRE_BURST"),
        A("Fire Dragon's Brilliant Flame", "Press your hands together and drop a great ball of fire on the place you look at after a short delay.", "meteor_drop", "minecraft:fire_charge",
          damage=14, radius=4.5, delay=22, vfx="FLAME_EXPLOSION"),
        A("Fire Dragon's Roar: Shift", "Crouch to roar a long wall of fire along the ground that scorches every foe on it.", "ground_wave", "minecraft:lava_bucket",
          damage=8, length=12, width=3, vfx="FLAME_TRAIL"),
        A("Crimson Lotus: Flame Blade: Shift", "Crouch to spin in a blade of flame that cuts down everything around you several times.", "whirlwind", "minecraft:netherite_sword",
          damage=4, radius=4, hits=5, vfx="FIRE_SPIRAL"),
    ],
    passives=[
        P("Smouldering Debt", "Your blows leave embers on the foe (Debt). They keep burning while they wait, and your Debt arts cash them in.", "debt_store", "minecraft:campfire",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="FIRE_BURST", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Eating the Flame", "While you are burning, the fire feeds you: you hit with the strength of a dragon.", "self_effect_when", "minecraft:blaze_powder",
          effect="strength", condition="on_fire", amp=1),
        P("Dragon's Immunity to Fire", "Fire barely touches a Fire Dragon Slayer: you take much less fire damage.", "resist", "minecraft:magma_block",
          damage_kind="fire", pct=0.5),
        P("Rising Heat", "Blows in a row grow stronger the longer the fight goes on.", "momentum", "minecraft:fire_charge",
          per_stack=0.05, max_stacks=5, evolved={"per_stack": 0.08, "max_stacks": 8}),
        P("Fists That Send You Flying", "Your blows may knock the foe back in a burst of flame.", "knockback_on_hit", "minecraft:slime_ball",
          strength=0.5, chance=0.35),
        P("Salamander's Finish", "You deal much more damage to nearly beaten foes.", "execute_threshold", "minecraft:blaze_rod",
          threshold=0.2, bonus=0.6, evolved={"threshold": 0.3, "bonus": 1.2}),
        P("Never Gives Up", "When you are close to falling, a flame wall of will briefly shields you.", "last_stand", "minecraft:totem_of_undying",
          threshold=0.3, seconds=5, rest=1800),
    ],
    finishers=[
        A("Crimson Lotus: Exploding Flame Blade", "Spin in a ring of crimson flame, then burst it outward: a huge blow to every foe around you.", "ring_burst", "minecraft:netherite_axe",
          damage=24, radius=7, targets=8, knockback=1.5, cooldown=1400, vfx="FLAME_EXPLOSION"),
        U("Lightning Fire Dragon Mode", "Wrap yourself in lightning-charged flame: a huge blow to every foe around you plus many times all the embers stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=36, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="FIRE_SPIRAL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- B2 (design row 2)
LINES.append(Line(
    id="laxus_lightning_dragon", name="Lightning Dragon Slayer", franchise=F,
    desc="Laxus Dreyar's Lightning Dragon Slayer magic: fast, heavy and loud, with a body that eats lightning and a storm that answers every blow. "
         "Each hit leaves a static charge on the foe, until the Red Lightning Dragon Mode discharges every one of them.",
    color="#ffe14a", background="storm", icon="minecraft:lightning_rod",
    book_name="Lacrima of the Lightning Dragon", book_lore="A grandson's grudge, a dragon's heart and a storm that never ran dry: thunder has to be earned.",
    evolution={"requirement": "boss_kills", "count": 8,
               "text": "Defeat 8 boss creatures while Lightning Dragon Slayer is switched on to wake the Red Lightning Dragon Mode and unlock it.",
               "ultimate_name": "Ultimate Skill, Red Lightning Dragon Mode"},
    stances=[
        S("Lightning Body Stance", "Lightning runs through your limbs: you strike harder, may land critical hits and move faster, but wear your guard thin.",
          "minecraft:lightning_rod", damage_pct=0.18, crit_chance=0.1, speed_pct=0.08, armor=-1, vfx="LIGHTNING_FIEND"),
        S("Lightning Eater's Posture", "Stand still and swallow the storm: you are heavily armoured and heal steadily, but slow and weak in attack.",
          "minecraft:copper_block", armor=6, regen=1.0, speed_pct=-0.15, damage_pct=-0.1, vfx="LIGHTNING_SPEAR"),
    ],
    stats=[
        T("Dragon Lacrima Heart", "A dragon's heart beats in your chest: more maximum health.", "max_health", 2, "minecraft:heart_of_the_sea"),
        T("Thunder Fists", "Fists charged with lightning hit harder.", "attack_damage", 1.2, "minecraft:iron_sword"),
        T("Lightning Reflexes", "Lightning-fast hands strike faster.", "attack_speed", 0.06, "minecraft:golden_sword", op="mult"),
        T("Heavy Frame", "A big man is hard to push around.", "knockback_resistance", 0.05, "minecraft:iron_chestplate"),
    ],
    arts=[
        A("Lightning Dragon's Roar", "Breathe a bolt of lightning in front of you: foes in the cone are slowed and weakened as the current locks their muscles.", "debuff_cone", "minecraft:lightning_rod",
          effect="slowness", effect2="weakness", seconds=6, seconds2=6, range=7, half_angle=45, amp=1, vfx="LIGHTNING_FIEND"),
        A("Lightning Dragon's Iron Fist", "Wrap a fist in lightning and punch the foe in front of you; the weaker it is, the harder you hit.", "execute", "minecraft:iron_ingot",
          damage=10, range=3.5, bonus=0.8, vfx="LIGHTNING_SPEAR"),
        A("Lightning Dragon's Jaw", "Join your hands, leap and bring them down on the foes below in a crater of lightning.", "leap_slam", "minecraft:iron_axe",
          damage=11, radius=3.5, height=0.8, knockback=0.8, vfx="LIGHTNING_FIEND"),
        A("Lightning Dragon's Breakdown Fist", "A giant fist of pure lightning shoots out of your hand and pierces the foes ahead.", "ray_shot", "minecraft:copper_ingot",
          damage=9, range=18, pierce=3, vfx="LIGHTNING_SPEAR"),
        A("Roaring Thunder", "Charge forward and punch through everything in the way with a lightning-charged fist.", "dash_strike", "minecraft:glowstone_dust",
          distance=9, damage=9, targets=4, vfx="LIGHTNING_FIEND"),
        A("Raging Bolt", "Gather the storm above you and call a huge bolt down on the place you look at after a short delay.", "meteor_drop", "minecraft:lightning_rod",
          damage=15, radius=4.5, delay=26, vfx="LIGHTNING_SPEAR"),
        A("Lightning Dragon's Roar: Shift", "Crouch to throw the roar in an arc: a lightning blast bursts all around you instead of ahead.", "ring_burst", "minecraft:amethyst_shard",
          damage=7, radius=5, targets=7, knockback=1.0, vfx="LIGHTNING_FIEND"),
        A("Lightning Body: Shift", "Crouch to turn into lightning and reappear a long way ahead.", "blink", "minecraft:ender_pearl",
          distance=11, vfx="LIGHTNING_SPEAR"),
    ],
    passives=[
        P("Static Debt", "Your blows leave a static charge on the foe (Debt). It crackles while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:copper_ingot",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="LIGHTNING_FIEND", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Lightning Eater", "A Lightning Dragon Slayer feeds on the storm: you take much less lightning damage.", "resist", "minecraft:lightning_rod",
          damage_kind="lightning", pct=0.6),
        P("Static Shock", "Whoever strikes you is jolted and may be slowed.", "on_hurt_retaliate", "minecraft:redstone",
          effect="slowness", seconds=3, chance=0.3, rest=100, evolved={"chance": 0.5, "seconds": 5}),
        P("Faster than Thunder", "You may slip out of the way of a blow at lightning speed.", "dodge", "minecraft:sugar",
          chance=0.1, rest=100, evolved={"chance": 0.18}),
        P("Charging Bolt", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:feather",
          bonus=0.25),
        P("Storm-Hearted Crits", "Your blows may strike with a thunderclap.", "crit", "minecraft:glowstone_dust",
          chance=0.12, mult=1.6),
        P("Grandson of the Master", "Strong foes bring out your best: you deal more damage to bosses.", "dmg_vs", "minecraft:wither_skeleton_skull",
          category="boss", bonus=0.2),
    ],
    finishers=[
        A("Fairy Law Dome", "Light gathers into a great dome over the field and strikes everyone in it: foes around you are stunned in place for a few seconds.", "stun_ring", "minecraft:beacon",
          radius=9, seconds=4, targets=10, cooldown=1500, vfx="MAGIC_CIRCLE"),
        U("Red Lightning Dragon Mode", "Lightning beyond lightning: a huge blow to every foe around you plus many times all the static stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=36, mult=3.0, radius=12, targets=10, cost=0.08, cooldown=18000, vfx="LIGHTNING_SPEAR"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- B3 (design row 3)
LINES.append(Line(
    id="wendy_sky_dragon", name="Sky Dragon Slayer", franchise=F,
    desc="Wendy Marvell's Sky Dragon Slayer magic: healing breath, and the Troia, Vernier, Arms and Armor enchantments that make a whole party faster, stronger and tougher. "
         "Her blows are gentle but patient, and her Debt is wind that waits, until the Shattering Light brings it all down.",
    color="#7fd6ff", background="holy", icon="minecraft:feather",
    book_name="Primer of the Sky Dragon Slayer", book_lore="Grandeeney taught her that the sky is no weapon: it holds the wounded up, until the day it must hold the enemy down.",
    evolution={"requirement": "ally_buffs", "count": 400,
               "text": "Reach 400 allies with the buffs and healing of Sky Dragon Slayer to awaken the Shattering Light and unlock it.",
               "ultimate_name": "Ultimate Skill, Shattering Light: Sky Drill"},
    stances=[
        S("Sky Dragon's Blessing", "Clean air gathers around you: you heal slowly and move faster, but your blows are lighter.",
          "minecraft:feather", regen=1.0, speed_pct=0.08, damage_pct=-0.05, vfx="WIND_RING"),
        S("Healer's Prayer", "Kneel and hold the wind close: much harder to hurt and to push around, but slow and weak in attack.",
          "minecraft:shield", armor=6, knockback_res=0.5, speed_pct=-0.2, damage_pct=-0.15, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Grandeeney's Breath", "A dragon's lessons strengthen your body.", "max_health", 2, "minecraft:golden_apple"),
        T("Light as the Wind", "A small, quick girl is quick on her feet.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Wind Cushion", "Air slows your falls.", "safe_fall_distance", 1, "minecraft:feather"),
        T("Healer's Resolve", "Resolve under a gentle manner stiffens your guard.", "armor_toughness", 1, "minecraft:iron_chestplate"),
    ],
    arts=[
        A("Troia", "A ball of healing magic that mends you and the allies around you.", "heal_pulse", "minecraft:glistering_melon_slice",
          radius=7, amount=6, vfx="SPIRIT_AURA"),
        A("Vernier", "Chant the enchantment: you and your allies around you move much faster for a while.", "buff_aura", "minecraft:sugar",
          effect="speed", radius=8, seconds=14, amp=1, vfx="WIND_RING"),
        A("Arms", "Chant the enchantment: you and your allies around you hit harder for a while.", "buff_aura", "minecraft:iron_sword",
          effect="strength", radius=8, seconds=14, amp=0, vfx="MAGIC_CIRCLE"),
        A("Armor", "Chant the enchantment: you and your allies around you take less damage for a while.", "buff_aura", "minecraft:iron_chestplate",
          effect="resistance", radius=8, seconds=14, amp=0, vfx="MIRROR_PANE"),
        A("Sky Dragon's Roar", "Breathe a hurricane along your line of sight that tears through the foes in front of you.", "ray_shot", "minecraft:white_banner",
          damage=7, range=18, pierce=3, vfx="WIND_TORNADO"),
        A("Sky Dragon's Wing Attack", "Whirlwinds twist off both arms and hit everything near you.", "ring_burst", "minecraft:elytra",
          damage=5, radius=4.5, targets=6, knockback=1.2, vfx="WIND_RING"),
        A("Sky Dragon's Breath: Shift", "Crouch to breathe healing air over your own wounds: you heal and regenerate for a while.", "heal_self", "minecraft:glass_bottle",
          amount=10, regen_seconds=8, vfx="SPIRIT_AURA"),
        A("Sky Dragon's Claw: Shift", "Crouch to leap and bring a leg down in a gust that crashes onto the foes below.", "leap_slam", "minecraft:leather_boots",
          damage=8, radius=3.5, height=1.0, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Waiting Wind", "Your blows leave a pressure of wind on the foe (Debt). It builds while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:string",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WIND_RING", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Healing Air", "A gentle wind surrounds you and the allies near you, and keeps them regenerating.", "aura", "minecraft:glistering_melon_slice",
          effect="regeneration", target="allies", radius=7, amp=0, evolved={"radius": 10, "amp": 1}),
        P("Clean Air Heals", "High in the open air you heal faster.", "regen_when", "minecraft:feather",
          condition="airborne", per_second=0.8, evolved={"per_second": 1.6}),
        P("Mending Breath", "Your wounds close over time, even out of combat.", "mending", "minecraft:honey_bottle",
          seconds=18, amount=1, evolved={"seconds": 10, "amount": 2}),
        P("Catching Wind", "The wind catches you: you take far less fall damage.", "fall_guard", "minecraft:phantom_membrane",
          pct=0.6),
        P("Riding the Updraft", "While in the air you fall slowly, as the sky holds you up.", "self_effect_when", "minecraft:elytra",
          effect="slow_falling", condition="airborne", amp=0),
        P("Eating the Air", "A Sky Dragon Slayer feeds on air: you tire more slowly.", "hunger_saver", "minecraft:bread",
          pct=0.3),
    ],
    finishers=[
        A("Ile Armor", "Chant the strengthened enchantment twice over: you and your allies in a wide ring take far less damage for a long while.", "buff_aura", "minecraft:diamond_chestplate",
          effect="resistance", radius=13, seconds=24, amp=2, cost=0.05, cooldown=1800, vfx="MAGIC_CIRCLE"),
        U("Shattering Light: Sky Drill", "A barrier of wind closes in on every foe around you and drills down: a huge blow plus many times all the wind pressure stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=32, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="WIND_TORNADO"),
    ],
))


# ================================================================================================================================ 3.28.0 batch C (docs/skills/evolution_ten_c.md rows 1-4)
# Source (rule 10): English Fairy Tail wiki, read through WebSearch digests only (fairytail.fandom.com itself was unreachable from this environment); Japanese searches added
# little (docs/skills/research/fairy_tail_c.md). Names that file marks UNVERIFIED or INVENTED are labelled there; every number and wording is the mod's own design.

# ---------------------------------------------------------------------------------------------------------------------------------- C1 (design row 1)
LINES.append(Line(
    id="gajeel_iron_dragon", name="Iron Dragon Slayer", franchise=F,
    desc="Gajeel Redfox's Iron Dragon Slayer magic: turn your arms into clubs and blades, scale your skin with iron and roar a storm of shards. "
         "You are built to be hit: every blow you take makes you harder, and every blow you land leaves iron rusting on the foe, until the Iron Shadow Dragon Mode brings it all down at once.",
    color="#8a96a8", background="iron", icon="minecraft:iron_block",
    book_name="Ledger of the Iron Dragon Slayer", book_lore="Metalicana taught him one thing before he vanished: eat the iron, and the iron will carry you through whatever hits you.",
    evolution={"requirement": "damage_taken", "count": 4000, "params": {"damage_kind": "any"},
               "text": "Take 4,000 damage while Iron Dragon Slayer is switched on to harden into the Iron Shadow Dragon Mode and unlock it.",
               "ultimate_name": "Ultimate Skill, Iron Shadow Dragon Mode"},
    stances=[
        S("Iron Dragon Scales Stance", "Iron plates grow over your skin: much sturdier and harder to shove, and your blows land heavier, but you are slower on your feet.",
          "minecraft:iron_chestplate", armor=5, damage_pct=0.1, knockback_res=0.3, speed_pct=-0.08, vfx="MIRROR_PANE"),
        S("Iron Eater's Posture", "Plant your feet and swallow the metal around you: you heal steadily and cannot be moved, but your blows are weaker.",
          "minecraft:anvil", armor=7, regen=1.0, knockback_res=0.6, speed_pct=-0.15, damage_pct=-0.1, vfx="STONE_SPIKES"),
    ],
    stats=[
        T("Metalicana's Training", "Years of a dragon's rough lessons toughen your frame.", "max_health", 2, "minecraft:golden_apple"),
        T("Iron Rod Arms", "Arms that turn into iron rods hit harder.", "attack_damage", 1.2, "minecraft:iron_sword"),
        T("Scaled Skin", "A thin coat of iron turns blows aside.", "armor", 1, "minecraft:iron_chestplate"),
        T("Heavy Footing", "A body of iron is hard to push around.", "knockback_resistance", 0.05, "minecraft:iron_boots"),
    ],
    arts=[
        A("Iron Dragon's Club", "Turn your arm into a long iron club and sweep it through the foes in front of you.", "cone_strike", "minecraft:iron_axe",
          damage=9, range=4, half_angle=50, targets=3, knockback=0.9, vfx="WEAPON_CONSTRUCTS"),
        A("Iron Dragon's Sword", "Turn your arm into a blade and charge forward, cutting everything in your way.", "dash_strike", "minecraft:iron_sword",
          distance=8, damage=9, targets=4, vfx="WIND_SLASH"),
        A("Iron Dragon's Roar", "Breathe a blast of iron shards along your line of sight that tears through the foes ahead.", "ray_shot", "minecraft:iron_nugget",
          damage=9, range=16, pierce=3, vfx="STONE_SPIKES"),
        A("Iron Dragon's Hard Fist", "A fist of solid iron: it hits much harder the weaker the foe is.", "execute", "minecraft:iron_ingot",
          damage=10, range=3.5, bonus=0.8, vfx="FIRE_BURST"),
        A("Iron Dragon's Scales", "Cover yourself in iron scales: a thick shield of absorption that soaks up the next blows.", "shield", "minecraft:iron_chestplate",
          absorb_hearts=6, seconds=10, resist_amp=1, vfx="MIRROR_PANE"),
        A("Iron Dragon's Demon Logs", "Throw a volley of iron logs from your arm at the foe you look at.", "barrage", "minecraft:chain",
          damage=3, shots=6, range=14, vfx="WEAPON_CONSTRUCTS"),
        A("Iron Dragon's Club: Shift", "Crouch to leap high and bring the club down on the foes below.", "leap_slam", "minecraft:anvil",
          damage=11, radius=3.5, height=1.0, knockback=1.0, vfx="STONE_SPIKES"),
        A("Iron Dragon's Scales: Shift", "Crouch to plant yourself and bellow: nearby hostile creatures turn on you while your scales soak the blows.", "taunt", "minecraft:iron_helmet",
          radius=9, targets=6, resist_seconds=5, vfx="MIRROR_PANE"),
    ],
    passives=[
        P("Rusting Iron", "Your blows leave rusting iron in the foe (Debt). It eats at it while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:iron_nugget",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WEAPON_CONSTRUCTS", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Iron Spikes", "Scales grow spikes where you are struck: part of the damage you take is returned to the attacker.", "thorns", "minecraft:iron_bars",
          pct=0.15, rest=20, evolved={"pct": 0.3}),
        P("Hardened in Battle", "Once the fight begins your scales harden and you take less damage.", "reduce_when", "minecraft:iron_boots",
          condition="in_combat", pct=0.1, evolved={"pct": 0.2}),
        P("Eating Iron", "A Dragon Slayer feeds on his element: part of the damage you deal comes back as health.", "lifesteal", "minecraft:iron_ingot",
          pct=0.06),
        P("Iron Will", "When you are badly hurt you dig in and gain Resistance.", "self_effect_when", "minecraft:netherite_scrap",
          effect="resistance", condition="low_health", amp=1),
        P("Iron Fist Launch", "Your blows may knock the foe back across the room.", "knockback_on_hit", "minecraft:piston",
          strength=0.6, chance=0.3),
        P("Shrapnel-Proof Hide", "Iron scales turn arrows and thrown things aside: you take less projectile damage.", "resist", "minecraft:crossbow",
          damage_kind="projectile", pct=0.3),
    ],
    finishers=[
        A("Iron Dragon's Rampage", "Whirl in a storm of iron rods that hits everything around you again and again.", "whirlwind", "minecraft:netherite_axe",
          damage=6, radius=5, hits=7, cooldown=1400, vfx="WEAPON_CONSTRUCTS"),
        U("Iron Shadow Dragon Mode", "Iron and shadow become one: a huge blow to every foe around you plus many times all the rust stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=34, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="SHADOW_POOL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- C2 (design row 2)
LINES.append(Line(
    id="mirajane_satan_soul", name="Take Over: Satan Soul", franchise=F,
    desc="Mirajane Strauss's Take Over magic: put on a demon's soul and its wings, and spit dark orbs, sparks and showers without pause. "
         "The more you cast the more the demon speaks through you, and the Debt it leaves on foes is cashed in by the Hell's Core.",
    color="#b0305a", background="blood", icon="minecraft:nether_wart",
    book_name="Grimoire of the Demon Take Over", book_lore="She hid it for two years behind a bar and a smile. The demon was never gone: it was only waiting for someone to ask.",
    evolution={"requirement": "arts_cast", "count": 600,
               "text": "Cast the arts of Take Over: Satan Soul 600 times to let the demon take full control and unlock the Hell's Core.",
               "ultimate_name": "Ultimate Skill, Satan Soul: Hell's Core"},
    stances=[
        S("Satan Soul Stance", "Wings of a demon unfold: your blows hit harder and may strike critically, and you move faster, but you wear your guard a little thin.",
          "minecraft:wither_rose", damage_pct=0.18, crit_chance=0.08, speed_pct=0.06, armor=-1, vfx="DEVIL_CIRCLE"),
        S("Sitri's Poise", "Hold the cold, commanding calm of Sitri: you are better guarded and slowly heal, but you hit more softly.",
          "minecraft:soul_lantern", armor=4, regen=1.0, speed_pct=-0.05, damage_pct=-0.08, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Demon's Vigor", "A demon's soul in your chest: more maximum health.", "max_health", 2, "minecraft:heart_of_the_sea"),
        T("Take Over Strength", "Borrowed claws hit harder.", "attack_damage", 1.0, "minecraft:iron_sword"),
        T("Demon Wings", "Wings of a demon make you light on your feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Bar Mistress's Hands", "Quick hands from years of pouring and fighting.", "attack_speed", 0.05, "minecraft:golden_sword", op="mult"),
    ],
    arts=[
        A("Evil Explosion", "Gather dark magic around you and burst it outward: a blast that hits every foe near you.", "ring_burst", "minecraft:fire_charge",
          damage=7, radius=4.5, targets=6, knockback=1.1, cost=0.015, cooldown=120, vfx="DEVIL_CIRCLE"),
        A("Evil Spark", "Hurl a crackling spark of darkness at a foe; it jumps from foe to foe.", "chain_hit", "minecraft:redstone",
          damage=6, jumps=4, cost=0.015, cooldown=120, vfx="LIGHTNING_FIEND"),
        A("Evil Orb", "Spit a sphere of dark magic that bores through the foes ahead.", "ray_shot", "minecraft:ender_eye",
          damage=8, range=16, pierce=2, cost=0.015, cooldown=100, vfx="SHADOW_POOL"),
        A("Evil Shower", "Rain a volley of dark shots on the foe you look at.", "barrage", "minecraft:spectral_arrow",
          damage=3, shots=7, range=14, cost=0.02, cooldown=160, vfx="DEVIL_CIRCLE"),
        A("Darkness Stream", "Send a rolling wave of darkness along the ground that blasts everything on it.", "ground_wave", "minecraft:black_dye",
          damage=7, length=11, width=3, cost=0.015, cooldown=120, vfx="SHADOW_POOL"),
        A("Satan Blast", "Call a great column of demon fire down on the place you look at after a short delay.", "meteor_drop", "minecraft:blaze_rod",
          damage=14, radius=4.5, delay=22, cost=0.025, cooldown=220, vfx="FLAME_EXPLOSION"),
        A("Satan Soul: Sitri: Shift", "Crouch to turn Sitri's commanding gaze on the room: foes around you are held in place for a moment.", "stun_ring", "minecraft:amethyst_shard",
          radius=6, seconds=3, targets=7, cost=0.02, cooldown=240, vfx="MAGIC_CIRCLE"),
        A("Darkness Stream: Shift", "Crouch to let the stream curl inward and drag the foes around you toward yourself.", "pull", "minecraft:ghast_tear",
          radius=8, strength=1.0, targets=6, damage=4, cost=0.02, cooldown=180, vfx="SEAL_CHAINS"),
    ],
    passives=[
        P("Demon's Debt", "Your blows leave a demon's mark on the foe (Debt). It burns while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:crying_obsidian",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="DEVIL_CIRCLE", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Night of the Demon", "The demon is strongest in the dark: you deal more damage at night.", "dmg_when", "minecraft:black_dye",
          condition="night", bonus=0.2, evolved={"bonus": 0.4}),
        P("Sitri's Presence", "Foes close to you feel a demon's gaze and grow weaker.", "aura", "minecraft:fermented_spider_eye",
          effect="weakness", target="enemies", radius=6, amp=0, evolved={"radius": 9}),
        P("Demon's Hunger", "Part of the damage you deal feeds the demon and heals you.", "lifesteal", "minecraft:ghast_tear",
          pct=0.06),
        P("Soul-Bound Skin", "A take-over soul turns spells aside: you take less magic damage.", "resist", "minecraft:enchanted_book",
          damage_kind="magic", pct=0.3),
        P("Devil's Reward", "Each kill heals you and sharpens you with Strength.", "kill_reward", "minecraft:experience_bottle",
          heal=3, effect="strength", seconds=5),
        P("Demon Queen's Sting", "Your blows may strike with a demon's cruelty.", "crit", "minecraft:flint",
          chance=0.12, mult=1.6),
    ],
    finishers=[
        A("Soul Extinction", "Gather the soul into one huge blast of dark magic and loose it through every foe ahead of you.", "ray_shot", "minecraft:end_crystal",
          damage=24, range=22, pierce=5, cost=0.05, cooldown=1400, vfx="SHADOW_POOL"),
        U("Satan Soul: Hell's Core", "The demon takes full control: a huge blow to every foe around you plus many times all the demon's marks stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=36, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="DEVIL_CIRCLE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- C3 (design row 3)
LINES.append(Line(
    id="gildarts_crash", name="Crash Magic", franchise=F,
    desc="Gildarts Clive's Crash: whatever you touch comes apart, stone, steel or spell. "
         "Break the ground, shatter the magic thrown at you, walk through walls, and let every cracked foe wait for the All Crush that finishes the job.",
    color="#b86bff", background="ruin", icon="minecraft:cracked_stone_bricks",
    book_name="Fragments of the Crash Mage", book_lore="The strongest mage in the guild has never said how Crash feels. He just walks, and the world gets out of his way.",
    evolution={"requirement": "damage_dealt", "count": 120000,
               "text": "Deal 120,000 damage to hostile creatures while Crash Magic is switched on to unlock the All Crush.",
               "ultimate_name": "Ultimate Skill, All Crush"},
    stances=[
        S("Crash Stance", "Everything you touch begins to crack: your blows hit very hard and may strike critically, but you give up some guard.",
          "minecraft:tnt", damage_pct=0.25, crit_chance=0.1, armor=-1, vfx="STONE_SPIKES"),
        S("Ace's Stride", "Walk through the fight as if it were not there: you are hard to push around and slowly heal, but you hit more softly.",
          "minecraft:netherite_boots", armor=3, regen=1.0, knockback_res=0.5, damage_pct=-0.1, vfx="ASH_FORMATION"),
    ],
    stats=[
        T("S-Class Frame", "Decades of S-class jobs leave a body that does not break.", "max_health", 3, "minecraft:golden_apple"),
        T("Crashing Hands", "Hands that unmake what they touch hit harder.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Ace's Toughness", "A lifetime of near-misses stiffens your guard.", "armor_toughness", 1, "minecraft:netherite_chestplate"),
        T("Long Road Legs", "A hundred-year journey leaves a long stride.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Crash", "Touch the foe in front of you and unmake it: the weaker it is, the more of it comes apart.", "execute", "minecraft:cracked_stone_bricks",
          damage=11, range=3.5, bonus=1.0, vfx="STONE_SPIKES"),
        A("Crash Wave", "Thrust out your hand and send a cross of crushing force through the foes ahead.", "ray_shot", "minecraft:crossbow",
          damage=10, range=18, pierce=3, vfx="ASH_FORMATION"),
        A("Crash Cushion", "Crush the ground under you into a soft bed: a shield of absorption that eats the next blows.", "shield", "minecraft:cobblestone",
          absorb_hearts=7, seconds=10, resist_amp=1, vfx="STONE_SPIKES"),
        A("Ground Crash", "Leap high and shatter the ground under the foes below.", "leap_slam", "minecraft:anvil",
          damage=12, radius=4, height=1.0, knockback=1.2, vfx="STONE_SPIKES"),
        A("Spell Crush", "Crush the magic thrown at you for a moment: part of the damage you take is thrown back at the attacker.", "ward_reflect", "minecraft:echo_shard",
          seconds=8, reflect_pct=0.5, vfx="MIRROR_PANE"),
        A("Wall Crash", "Break straight through whatever is in the way: vanish ahead through the rubble.", "blink", "minecraft:ender_pearl",
          distance=11, vfx="ASH_FORMATION"),
        A("Crash: Shift", "Crouch to touch a foe and crack its guard: it takes more damage from everyone for a while.", "mark", "minecraft:flint",
          range=16, seconds=10, bonus=0.4, vfx="SEAL_CHAINS"),
        A("Crash Wave: Shift", "Crouch to crush the sky above the place you look at: rubble falls on it after a short delay.", "meteor_drop", "minecraft:tnt_minecart",
          damage=16, radius=4.5, delay=24, vfx="STONE_SPIKES"),
    ],
    passives=[
        P("Cracks in Everything", "Your blows leave cracks in the foe (Debt). They spread while they wait, and your Debt arts cash them in.", "debt_store", "minecraft:cracked_stone_bricks",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="STONE_SPIKES", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Nothing Stands", "You deal much more damage to nearly beaten foes.", "execute_threshold", "minecraft:wither_skeleton_skull",
          threshold=0.2, bonus=0.6, evolved={"threshold": 0.3, "bonus": 1.2}),
        P("Ace of the Guild", "Strong foes bring out your best: you deal more damage to bosses.", "dmg_vs", "minecraft:netherite_ingot",
          category="boss", bonus=0.2, evolved={"bonus": 0.4}),
        P("Used to Falling", "You walk away from a long drop: you take far less fall damage.", "fall_guard", "minecraft:feather",
          pct=0.6),
        P("Unbothered", "Your wounds close over time, even out of combat.", "mending", "minecraft:honey_bottle",
          seconds=18, amount=1),
        P("Crash Against the Blast", "Crash unmakes the blast before it reaches you: you take less explosion damage.", "resist", "minecraft:tnt",
          damage_kind="explosion", pct=0.4),
        P("S-Class Instinct", "Your blows may strike critically.", "crit", "minecraft:glowstone_dust",
          chance=0.12, mult=1.7),
    ],
    finishers=[
        A("Crash: Shatter Field", "Crash the whole battlefield at once: a huge blow to every foe around you.", "ring_burst", "minecraft:obsidian",
          damage=26, radius=8, targets=10, knockback=1.6, cost=0.06, cooldown=1500, vfx="STONE_SPIKES"),
        U("All Crush", "Everything the Crash has cracked comes apart together: a huge blow to every foe around you plus many times all the cracks stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=38, mult=3.0, radius=12, targets=10, cost=0.08, cooldown=18000, vfx="ASH_FORMATION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- C4 (design row 4)
LINES.append(Line(
    id="juvia_water_body", name="Water Body", franchise=F,
    desc="Juvia Lockser's Water Magic: slice with water, wrap foes in bubbles, and let your own body turn to water so the blow that should have killed you passes through. "
         "Rain is on your side, and every drop leaves a pressure on the foe until the Great Flood.",
    color="#3f8fe0", background="ocean", icon="minecraft:heart_of_the_sea",
    book_name="Rain-Woman's Water Primer", book_lore="Wherever she went the rain followed. Then one day it stopped, and she found out it had been hers all along.",
    evolution={"requirement": "lethal_survived", "count": 12,
               "text": "Survive 12 blows that would have left you at 2 health or less while Water Body is switched on to flood the field and unlock the Great Flood.",
               "ultimate_name": "Ultimate Skill, Water Body: Great Flood"},
    stances=[
        S("Water Body Stance", "Your body flows: you are lighter and faster and strike critically more often, but you wear your guard thin.",
          "minecraft:water_bucket", speed_pct=0.08, crit_chance=0.08, damage_pct=0.06, armor=-1, vfx="WATER_RING"),
        S("Rain-Woman's Veil", "Let the rain hold you: you heal slowly and are hard to shift, but you hit more softly.",
          "minecraft:kelp", armor=3, regen=1.5, knockback_res=0.3, damage_pct=-0.1, vfx="WATER_BURST"),
    ],
    stats=[
        T("Water-Woman's Body", "A body that can flow survives more: more maximum health.", "max_health", 3, "minecraft:heart_of_the_sea"),
        T("Slicing Currents", "Water sharpened to cut hits harder.", "attack_damage", 1.0, "minecraft:prismarine_shard"),
        T("Flowing Steps", "A body that moves like a stream is quick on its feet.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Water Slice", "Whip a blade of pressurised water through the foes in front of you.", "cone_strike", "minecraft:prismarine_shard",
          damage=8, range=4.5, half_angle=45, targets=4, vfx="WATER_BURST"),
        A("Water Nebula", "A swirling nebula of water bursts around you and hits every foe near you.", "ring_burst", "minecraft:nautilus_shell",
          damage=6, radius=4.5, targets=6, knockback=1.0, vfx="WATER_RING"),
        A("Water Cyclone", "Spin in a cyclone of water that hits everything around you again and again.", "whirlwind", "minecraft:conduit",
          damage=4, radius=4, hits=6, vfx="WATER_RING"),
        A("Hydro Screw", "A drill of water bursts up under the foe in front of you and throws it into the air.", "uppercut", "minecraft:prismarine_crystals",
          damage=8, range=3.4, launch=0.9, vfx="WATER_BURST"),
        A("Water Jigsaw", "Many thin saws of water cut at the foe you look at.", "barrage", "minecraft:cod",
          damage=3, shots=6, range=14, vfx="WATER_BURST"),
        A("Water Body Form", "Let your body flow: your wounds close and you regenerate for a while.", "heal_self", "minecraft:potion",
          amount=10, regen_seconds=8, vfx="WATER_RING"),
        A("Water Body Form: Shift", "Crouch to turn to water and flow a long way ahead.", "blink", "minecraft:pufferfish",
          distance=10, vfx="WATER_BURST"),
        A("Water Slice: Shift", "Crouch to send the blade along the ground as a wave that cuts everything on it.", "ground_wave", "minecraft:sea_lantern",
          damage=7, length=11, width=3, vfx="WATER_BURST"),
    ],
    passives=[
        P("Drops Left Behind", "Your blows leave a pressure of water on the foe (Debt). It builds while it waits, and your Debt arts cash it in.", "debt_store", "minecraft:lapis_lazuli",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WATER_RING", evolved={"store_pct": 0.5, "max_pct": 0.8}),
        P("Body of Water", "A blow may pass straight through you as if you were water.", "dodge", "minecraft:glass_bottle",
          chance=0.1, rest=100, evolved={"chance": 0.2}),
        P("Not Yet", "When you are close to falling, your body turns to water and briefly shields you.", "last_stand", "minecraft:totem_of_undying",
          threshold=0.3, seconds=5, rest=1800),
        P("Reforming Body", "Your wounds close over time, even out of combat.", "mending", "minecraft:honey_bottle",
          seconds=18, amount=1),
        P("Rain Woman", "In the rain you feel at home: you heal while it falls.", "regen_when", "minecraft:kelp",
          condition="raining", per_second=0.8, evolved={"per_second": 1.6}),
        P("Water Quenches Fire", "Fire struggles to burn a body of water: you take less fire damage.", "resist", "minecraft:water_bucket",
          damage_kind="fire", pct=0.35),
        P("At Home in the Sea", "While you stand in water you breathe freely.", "self_effect_when", "minecraft:tropical_fish",
          effect="water_breathing", condition="in_water", amp=0),
    ],
    finishers=[
        A("Water Nebula: Barrier", "Wrap you and your place in a sphere of flowing water: a thick shield of absorption that takes the next blows.", "shield", "minecraft:conduit",
          absorb_hearts=10, seconds=14, resist_amp=1, cost=0.05, cooldown=1500, vfx="WATER_RING"),
        U("Water Body: Great Flood", "The field floods: a huge blow to every foe around you plus many times all the water pressure stored on each of them.", "debt_burst", "minecraft:nether_star",
          damage=32, mult=3.0, radius=11, targets=10, cost=0.08, cooldown=18000, vfx="WATER_BURST"),
    ],
))
