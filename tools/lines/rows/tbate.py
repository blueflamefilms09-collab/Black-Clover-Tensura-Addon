"""The Beginning After the End: 25 NEW lines (mana cores, Beast Will, aether, Asuras and Alacryans, bonded beasts, weapon styles, Lance roles, Dicathen's races).

Where a canon detail was uncertain the line invents something in the spirit of the story. Helpers: S = stance, T = stat, A = art or finisher, P = passive.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "tbate"


def S(name, desc, icon, vfx="WIND_SLASH", **p):
    return Stance(name, desc, icon, dict(p, vfx=vfx))


def T(name, desc, attribute, amount, op="add", icon="minecraft:iron_ingot"):
    return Stat(name, desc, attribute, amount, op, icon)


def A(name, desc, kind, icon, vfx="WIND_SLASH", **p):
    return Art(name, desc, kind, dict(p, vfx=vfx), icon)


def P(name, desc, kind, icon, **p):
    return Pas(name, desc, kind, p, icon)


LINES = []

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="core_refinement", name="Mana Core Refinement", franchise=F,
    desc="The long road from a black core to a silver one. Absorb mana, refine it, and spend it in bursts of pressure; a patient line that grows stronger the longer you meditate.",
    color="#FBC02D", background="stellar", icon="minecraft:amethyst_shard",
    book_name="Core Refinement Primer", book_lore="A black core becomes silver one breath at a time.",
    stances=[
        S("Core Meditation", "Settle into the rhythm of absorption: steady recovery and a firm guard, but slow to act.", "minecraft:amethyst_block", "SPIRIT_AURA",
          regen=.9, armor=1, speed_pct=-.04),
        S("Core Overflow", "Let the core spill over: strong, quick, and raw, with almost no guard.", "minecraft:glowstone", "MAGIC_CIRCLE",
          damage_pct=.25, speed_pct=.08, armor=-3),
    ],
    stats=[
        T("Refined Core", "Raises your maximum health.", "max_health", 4, "add", "minecraft:amethyst_shard"),
        T("Mana-Tempered Body", "Raises your armor.", "armor", 1, "add", "minecraft:copper_ingot"),
        T("Flowing Strength", "Raises your attack damage.", "attack_damage", .9, "add", "minecraft:iron_sword"),
        T("Core Reach", "Lets you hit foes from a little farther away.", "entity_interaction_range", .3, "add", "minecraft:spyglass"),
    ],
    arts=[
        A("Absorb Ambient Mana", "Draw the mana around you into your core: heal and regenerate for a while.", "heal_self", "minecraft:lapis_lazuli", "SPIRIT_AURA",
          amount=8, regen_seconds=6),
        A("Core Surge", "Push mana through every channel: strength and haste for a time.", "buff_self", "minecraft:blaze_powder", "MAGIC_CIRCLE",
          effect="strength", effect2="haste", seconds=14, amp=1, amp2=0),
        A("Mana Shell", "Condense mana into a shell around you: absorption and resistance.", "shield", "minecraft:glass", "MIRROR_PANE",
          absorb_hearts=7, seconds=8, resist_amp=1),
        A("Mana Pulse", "Release a ring of raw mana pressure around you.", "ring_burst", "minecraft:heart_of_the_sea", "MAGIC_CIRCLE",
          damage=6.5, radius=4, targets=6, knockback=1.4),
        A("Pressure of a Higher Stage", "Let your core stage press down on a cone of foes: they weaken and slow.", "debuff_cone", "minecraft:crying_obsidian", "SPIRIT_AURA",
          range=6, half_angle=50, seconds=9, effect="weakness", effect2="slowness", seconds2=7),
        A("Core Beam", "Condense a bolt of mana and send it through everything along your gaze.", "ray_shot", "minecraft:end_rod", "LIGHTNING_SPEAR",
          damage=9, range=20, pierce=3),
        A("Mana Vortex", "Spin the mana around you and drag nearby creatures into it.", "pull", "minecraft:ender_eye", "WIND_RING",
          radius=8, strength=1, targets=5, damage=4),
    ],
    passives=[
        P("Slow Refining", "Your wounds close over time as the core refines them.", "mending", "minecraft:amethyst_shard", seconds=14, amount=1),
        P("Quick Study", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=.15),
        P("Efficient Flow", "You tire more slowly: your mana feeds the body.", "hunger_saver", "minecraft:bread", pct=.2),
        P("Mana-Tempered Skin", "Take less magic damage.", "resist", "minecraft:enchanting_table", damage_kind="magic"),
        P("Full Core", "Deal more damage while at full health.", "dmg_when", "minecraft:glowstone_dust", condition="full_health", bonus=.18),
        P("Breath Control", "Heal while sneaking, breathing in rhythm.", "regen_when", "minecraft:lily_pad", condition="sneaking", per_second=.5),
        P("Tidy Aftermath", "A kill restores a little of your health.", "kill_reward", "minecraft:lapis_lazuli", effect="none", heal=3, seconds=0),
    ],
    finishers=[
        A("Breakthrough Shockwave", "Break into the next stage: the pressure bursts out of you in a vast ring.", "ring_burst", "minecraft:nether_star", "MAGIC_CIRCLE",
          damage=26, radius=9, targets=14, knockback=2.2, cooldown=1500, cost=.07),
        A("Overflowing Core Beam", "Let the whole core pour out in one beam through the field.", "ray_shot", "minecraft:dragon_breath", "LIGHTNING_SPEAR",
          damage=36, range=32, pierce=6, cooldown=1600, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="beast_will", name="Beast Will", franchise=F,
    desc="Instinct from a beast's will, grafted onto a human fighter. The line feels for prey, pounces, rends and grows more dangerous with every kill.",
    color="#8E0000", background="blood", icon="minecraft:bone",
    book_name="Primal Will Codex", book_lore="It is hunger. It is also loyalty. Learn the difference.",
    stances=[
        S("Predator Crouch", "Drop low and watch: quicker, with a better chance to crit, but a thin guard.", "minecraft:leather_boots", "SHADOW_POOL",
          speed_pct=.15, crit_chance=.1, armor=-2),
        S("Ancient Bulk", "Roll your shoulders and take the hit: tougher and heavier, but slower.", "minecraft:bone_block", "EARTH_SPIKES",
          armor=4, knockback_res=.3, speed_pct=-.08, damage_pct=.1),
    ],
    stats=[
        T("Wild Heart", "Raises your maximum health.", "max_health", 6, "add", "minecraft:beef"),
        T("Raking Claws", "Raises your attack damage.", "attack_damage", 1.2, "add", "minecraft:iron_pickaxe"),
        T("Hunter's Stride", "You are quicker on your feet.", "movement_speed", .05, "mult", "minecraft:leather_boots"),
    ],
    arts=[
        A("Scent the Prey", "Fix on a foe's scent: it is marked and takes extra damage.", "mark", "minecraft:spider_eye", "SPIRIT_AURA",
          range=22, seconds=10, bonus=.3),
        A("Pounce", "Spring from a crouch and tear into everything in your path.", "dash_strike", "minecraft:bone", "WIND_SLASH",
          distance=8, damage=8, targets=3),
        A("Predator's Bite", "Sink your teeth into a foe and take back some of its life.", "drain_bite", "minecraft:rotten_flesh", "SHADOW_POOL",
          damage=7, range=3.4, heal_pct=.4),
        A("Feral Surge", "Let the will run hot: strength and speed for a while.", "buff_self", "minecraft:blaze_powder", "SPIRIT_AURA",
          effect="strength", effect2="speed", seconds=14, amp=1, amp2=1),
        A("Rending Claws", "Rake three claws across the foes in front of you.", "cone_strike", "minecraft:iron_axe", "WIND_SLASH",
          damage=9, range=4, half_angle=45, targets=3),
        A("Killing Instinct", "A bite for the throat: deadlier the weaker the foe.", "execute", "minecraft:netherite_sword", "DEVIL_CIRCLE",
          damage=10, range=3.6, bonus=1),
        A("Hunter's Leap", "Bound into the air and land on the prey below.", "leap_slam", "minecraft:anvil", "WIND_RING",
          damage=9, radius=3.2, height=1, knockback=1.4),
    ],
    passives=[
        P("Beast's Edge", "Your blows may strike critically.", "crit", "minecraft:flint", chance=.14, mult=1.7),
        P("Blood Taste", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:redstone", pct=.07),
        P("Chase Instinct", "Deal more damage while sprinting.", "sprint_bonus", "minecraft:leather_boots", bonus=.2),
        P("Moonlit Hunger", "At night your strength rises.", "self_effect_when", "minecraft:clock", effect="strength", condition="night"),
        P("Trophy Hunter", "A kill heals you and quickens you.", "kill_reward", "minecraft:bone", effect="speed", heal=2, seconds=4),
        P("Sixth Sense", "A chance to dodge a blow entirely.", "dodge", "minecraft:ender_eye", chance=.11, rest=100),
        P("Apex Predator", "Deal more damage to animals and monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=.2),
        P("Hunting Rhythm", "Blows in a row grow stronger until you stop fighting.", "momentum", "minecraft:bone_meal", per_stack=.05, max_stacks=6),
    ],
    finishers=[
        A("Apex Hunt", "A single fatal strike, almost certain against a wounded foe.", "execute", "minecraft:netherite_axe", "DEVIL_CIRCLE",
          damage=34, range=5, bonus=2, cooldown=1200, cost=.06),
        A("Rampage of the Beast", "Lose yourself in the will: claws on everything around you, over and over.", "whirlwind", "minecraft:bone_block", "WIND_TORNADO",
          damage=8, radius=5, hits=9, interval=4, cooldown=1300, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="realmheart_physique", name="Realmheart Physique", franchise=F,
    desc="Mana woven into every cell of the body, so the flesh itself is a spell. A defender's line: skin that turns edges, wounds that close mid-fight, and a counter for those who dare to strike.",
    color="#00ACC1", background="holy", icon="minecraft:heart_of_the_sea",
    book_name="Realmheart Manual", book_lore="The heart sees the realm, and the realm answers.",
    stances=[
        S("Realmheart Flow", "Let the mana move through every limb: a firm guard and steady recovery, a little slow.", "minecraft:heart_of_the_sea", "SPIRIT_AURA",
          regen=1, armor=2, speed_pct=-.03),
        S("Reinforced Strike", "Feed the mana into your strikes: heavier blows, a lighter guard.", "minecraft:iron_axe", "WIND_SLASH",
          damage_pct=.22, armor=-1, crit_chance=.05),
    ],
    stats=[
        T("Mana Marrow", "Raises your maximum health.", "max_health", 6, "add", "minecraft:bone_meal"),
        T("Mana-Woven Skin", "Raises your armor.", "armor", 1.5, "add", "minecraft:prismarine_shard"),
        T("Anchored Footing", "Makes you hard to knock back.", "knockback_resistance", .08, "add", "minecraft:stone"),
        T("Tough Fist", "Raises your attack damage.", "attack_damage", .8, "add", "minecraft:iron_ingot"),
    ],
    arts=[
        A("Mana Skin", "Coat your skin in mana: a thick absorption shield.", "shield", "minecraft:prismarine_bricks", "MIRROR_PANE",
          absorb_hearts=8, seconds=9, resist_amp=1),
        A("Reflecting Skin", "For a while, part of the damage you take is thrown back at the attacker.", "ward_reflect", "minecraft:shield", "MIRROR_PANE",
          seconds=7, reflect_pct=.4),
        A("Reinforced Palm", "A palm full of mana that flings the foe in front of you into the air.", "uppercut", "minecraft:iron_block", "WIND_SLASH",
          damage=8, range=3.4, launch=.9),
        A("Flow Reinforcement", "Run mana along muscle and bone: strength and resistance for a while.", "buff_self", "minecraft:glow_berries", "SPIRIT_AURA",
          effect="strength", effect2="resistance", seconds=14, amp=0, amp2=1),
        A("Shockwave Step", "Stamp mana into the ground and send a wave in front of you.", "ground_wave", "minecraft:deepslate", "STONE_SPIKES",
          damage=6, length=10, width=2.5, knockback=1),
        A("Anchor Call", "Your presence pulls every hostile in range to you.", "taunt", "minecraft:lodestone", "MAGIC_CIRCLE",
          radius=9, targets=6, resist_seconds=4),
        A("Mending Mana", "Pulse mana outward: heal yourself and your allies nearby.", "heal_pulse", "minecraft:glistering_melon_slice", "SPIRIT_AURA",
          radius=7, amount=6),
    ],
    passives=[
        P("Mana Hide", "Take less damage while at full health.", "reduce_when", "minecraft:golden_chestplate", condition="full_health", pct=.12),
        P("Sealed Wounds", "Your wounds close over time, even out of combat.", "mending", "minecraft:golden_apple", seconds=12, amount=1),
        P("Counter-Current", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:prismarine_shard", pct=.2, rest=20),
        P("Staggering Defense", "Whoever strikes you may be slowed.", "on_hurt_retaliate", "minecraft:cobweb", effect="slowness", seconds=3, chance=.25),
        P("Mana-Hardened Fall", "Take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=.5),
        P("Steady Blood", "Heal while you are in the middle of a fight.", "regen_when", "minecraft:redstone", condition="in_combat", per_second=.4),
        P("Last Wall", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=.3, seconds=7),
    ],
    finishers=[
        A("Realmheart Overflow", "Let the mana flood outward from your heart in a huge ring.", "ring_burst", "minecraft:conduit", "SPIRIT_AURA",
          damage=26, radius=9, targets=14, knockback=2, cooldown=1500, cost=.07),
        A("Heartwood of the Realm", "A great restoring surge for you and your allies.", "heal_pulse", "minecraft:enchanted_golden_apple", "SPIRIT_AURA",
          radius=11, amount=20, cooldown=1500, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="aether_step", name="Aether Step", franchise=F,
    desc="Aether used to cross distance: step through a heartbeat, vanish from where the blow lands and appear behind the foe. The line of speed, afterimages and the single decisive strike.",
    color="#7C4DFF", background="void", icon="minecraft:ender_pearl",
    book_name="Treatise on Stepping Between", book_lore="It is not speed. It is not being where they expected.",
    stances=[
        S("Afterimage Stance", "Leave an echo behind with every move: swift and evasive, but a thin guard.", "minecraft:phantom_membrane", "SPATIAL_RIFT",
          speed_pct=.22, armor=-2, crit_chance=.05),
        S("Anchored Step", "Plant your next step: a steadier guard and better strikes, slower on your feet.", "minecraft:obsidian", "MIRROR_PANE",
          armor=3, damage_pct=.12, speed_pct=-.06),
    ],
    stats=[
        T("Thin Air", "You are quicker on your feet.", "movement_speed", .06, "mult", "minecraft:feather"),
        T("Stepper's Heart", "Raises your maximum health.", "max_health", 2, "add", "minecraft:ender_pearl"),
        T("Edge Between", "Raises your attack speed.", "attack_speed", .08, "mult", "minecraft:iron_sword"),
        T("Longstep", "Raises your step height.", "step_height", .3, "add", "minecraft:ladder"),
    ],
    arts=[
        A("Aether Step", "Step through space and appear several blocks ahead.", "blink", "minecraft:ender_pearl", "SPATIAL_RIFT",
          distance=12),
        A("Afterimage Veil", "Leave blurs of yourself that confuse the creatures around you.", "smoke", "minecraft:gray_dye", "ASH_FORMATION",
          radius=5, seconds=6),
        A("Step Slash", "Step through the foes in your line and cut each as you pass.", "dash_strike", "minecraft:iron_sword", "WIND_SLASH",
          distance=10, damage=8, targets=4),
        A("Blur Combo", "Strike one foe and appear at the next, again and again.", "chain_hit", "minecraft:iron_nugget", "THREAD_LINE",
          damage=5.5, jumps=5, jump_range=7),
        A("Step Behind", "Appear behind the foe and cut a weak point: far more damage to a wounded foe.", "execute", "minecraft:netherite_sword", "SPATIAL_RIFT",
          damage=9, range=3.8, bonus=.9),
        A("Quicken the Aether", "Run on the aether: speed and haste for a while.", "buff_self", "minecraft:sugar", "WIND_RING",
          effect="speed", effect2="haste", seconds=14, amp=2, amp2=1),
        A("Phase Cut", "Cut through the ground and send a line of aether in front of you.", "ground_wave", "minecraft:amethyst_shard", "SPATIAL_RIFT",
          damage=6, length=11, width=2),
    ],
    passives=[
        P("Vanishing Dodge", "A good chance to dodge a blow entirely.", "dodge", "minecraft:phantom_membrane", chance=.15, rest=80),
        P("Strike From Nowhere", "Deal more damage while sprinting.", "sprint_bonus", "minecraft:leather_boots", bonus=.22),
        P("Weak-Point Eye", "Deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:ender_eye", threshold=.25, bonus=.6),
        P("Stepping Rhythm", "Blows in a row grow stronger until you stop fighting.", "momentum", "minecraft:clock", per_stack=.05, max_stacks=6),
        P("Light Foot", "Take much less fall damage.", "fall_guard", "minecraft:feather", pct=.5),
        P("Hidden Step", "Take less damage while sprinting.", "reduce_when", "minecraft:iron_boots", condition="sprinting", pct=.1),
        P("Fleeting Edge", "Your blows may strike critically.", "crit", "minecraft:flint", chance=.14, mult=1.7),
    ],
    finishers=[
        A("Hundred Steps of the Sovereign", "A flurry of steps carries you through every foe around you, cutting each many times.", "whirlwind", "minecraft:netherite_sword", "SPATIAL_RIFT",
          damage=8, radius=5.5, hits=10, interval=3, cooldown=1300, cost=.07),
        A("Step Beyond the Horizon", "Cross the whole field in one stride, cutting everything on the way.", "dash_strike", "minecraft:ender_eye", "SPATIAL_RIFT",
          distance=14, damage=30, targets=10, cooldown=1300, cost=.06),
    ],
))
