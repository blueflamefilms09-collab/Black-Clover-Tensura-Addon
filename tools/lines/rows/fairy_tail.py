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
