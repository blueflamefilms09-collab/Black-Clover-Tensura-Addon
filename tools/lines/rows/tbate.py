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

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="aether_destruction", name="Aether Arts: Destruction", franchise=F,
    desc="Aether in its most unforgiving aspect. Lances, collapses and cleaves that do not stop at armor, for a fighter who ends fights fast and pays in guard.",
    color="#AD1457", background="ruin", icon="minecraft:dragon_breath",
    book_name="Aether Scroll of Ruin", book_lore="What aether touches ends.",
    stances=[
        S("Destruction Aspect", "Draw the destructive aspect to the fore: ruinous blows, almost no guard.", "minecraft:dragon_breath", "DEVIL_CIRCLE",
          damage_pct=.4, armor=-4, crit_chance=.05),
        S("Restrained Aspect", "Hold the aspect on a short leash: a calmer guard and recovery, weaker blows.", "minecraft:crying_obsidian", "MAGIC_CIRCLE",
          armor=2, regen=.4, damage_pct=-.05),
    ],
    stats=[
        T("Aether Vein", "Raises your maximum health.", "max_health", 4, "add", "minecraft:chorus_fruit"),
        T("Ruinous Edge", "Raises your attack damage.", "attack_damage", 1.4, "add", "minecraft:netherite_scrap"),
        T("Cleaving Reach", "Lets you hit foes from farther away.", "entity_interaction_range", .4, "add", "minecraft:end_rod"),
    ],
    arts=[
        A("Aether Lance", "A thin lance of destructive aether pierces everything along your gaze.", "ray_shot", "minecraft:end_rod", "LIGHTNING_SPEAR",
          damage=9, range=22, pierce=3),
        A("Aetheric Collapse", "Make the air itself fold in on the place you look at.", "meteor_drop", "minecraft:crying_obsidian", "SPATIAL_RIFT",
          damage=13, radius=4, range=20, delay=26),
        A("Rend", "A great cleave across the cone in front of you.", "cone_strike", "minecraft:netherite_axe", "WIND_SLASH",
          damage=9, range=4.5, half_angle=45, targets=3, knockback=1),
        A("Shatter Line", "Split the ground in a long line of aether.", "ground_wave", "minecraft:obsidian", "SPATIAL_RIFT",
          damage=7, length=12, width=2.5),
        A("Detonate", "Your aether flares around you and throws foes back.", "ring_burst", "minecraft:tnt", "FLAME_EXPLOSION",
          damage=8, radius=4.2, targets=6, knockback=1.8),
        A("Hollow Strike", "A strike that deletes a wounded foe: far more damage the weaker it is.", "execute", "minecraft:netherite_sword", "DEVIL_CIRCLE",
          damage=10, range=3.6, bonus=1),
        A("Ruin Aspect", "Stoke the destructive aspect: strength and haste for a while.", "buff_self", "minecraft:blaze_powder", "DEVIL_CIRCLE",
          effect="strength", effect2="haste", seconds=14, amp=1, amp2=0),
    ],
    passives=[
        P("Breaker of Titans", "Deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=.22),
        P("Armor Pierce", "Your blows may strike critically.", "crit", "minecraft:flint", chance=.14, mult=1.8),
        P("Finishing Aether", "Deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:wither_rose", threshold=.25, bonus=.6),
        P("Blast Hardened Body", "Explosions barely scratch you.", "resist", "minecraft:tnt", damage_kind="explosion"),
        P("Ruinous Rhythm", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_nugget", hits=4, bonus=.55),
        P("Last Spark", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=.3, seconds=6),
        P("Withering Touch", "Your blows may wither the foe.", "on_hit_effect", "minecraft:wither_rose", effect="wither", seconds=4, chance=.2),
    ],
    finishers=[
        A("World-Rending Cleave", "One impossible cut that splits the ground in front of you for a great distance.", "ground_wave", "minecraft:netherite_block", "SPATIAL_RIFT",
          damage=34, length=22, width=5, knockback=2, cooldown=1500, cost=.07),
        A("Aether Annihilation Lance", "A lance of ruinous aether drives through everything ahead.", "ray_shot", "minecraft:dragon_breath", "LIGHTNING_SPEAR",
          damage=40, range=36, pierce=6, cooldown=1600, cost=.08),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="aevum_hourglass", name="Aevum: Hourglass of Ages", franchise=F,
    desc="A time-bending art of the Aevum: sand that slows the foe, shortens the fight and stretches the moments you need. Slow-and-wait control with a sudden fall of years.",
    color="#FFB300", background="sand", icon="minecraft:sand",
    book_name="The Aevum Hourglass", book_lore="Every grain is a second someone else will not get.",
    stances=[
        S("Slowing Sand", "Let the sand fall thick: a sturdy guard and recovery, a touch sluggish.", "minecraft:sand", "ASH_FORMATION",
          armor=2, regen=.5, speed_pct=-.03),
        S("Quickened Sand", "Pour the sand faster: much swifter, but easier to hit.", "minecraft:sugar", "WIND_RING",
          speed_pct=.25, armor=-2),
    ],
    stats=[
        T("Grain of Life", "Raises your maximum health.", "max_health", 4, "add", "minecraft:glass_bottle"),
        T("Sand Step", "You are quicker on your feet.", "movement_speed", .05, "mult", "minecraft:sand"),
        T("Slipping Seconds", "Raises your attack speed.", "attack_speed", .08, "mult", "minecraft:clock"),
    ],
    arts=[
        A("Frozen Instant", "Stop a single instant: the creatures near you cannot move.", "stun_ring", "minecraft:clock", "MAGIC_CIRCLE",
          radius=5, seconds=3, targets=6),
        A("Echo Ward", "For a while, part of every blow you take is returned one moment later.", "ward_reflect", "minecraft:glass", "MIRROR_PANE",
          seconds=7, reflect_pct=.4),
        A("Skipped Seconds", "Skip over the middle of the dash: you flash across and cut everything on the way.", "dash_strike", "minecraft:iron_sword", "SPATIAL_RIFT",
          distance=9, damage=7, targets=4),
        A("Shared Tempo", "Give you and your allies a share of your quickened time.", "buff_aura", "minecraft:sugar", "WIND_RING",
          effect="speed", radius=8, seconds=12, amp=0),
        A("Weight of Hours", "Hours pile up on a cone of foes: they weaken and slow.", "debuff_cone", "minecraft:anvil", "ASH_FORMATION",
          range=6, half_angle=50, seconds=8, effect="slowness", effect2="weakness", seconds2=6),
        A("Beam of Elapsed Years", "A beam that ages everything it crosses.", "ray_shot", "minecraft:wither_rose", "SPATIAL_RIFT",
          damage=8, range=18, pierce=3),
        A("Suspended Strike", "Hang the foe in front of you in the air for a moment, then strike.", "uppercut", "minecraft:feather", "WIND_RING",
          damage=7, range=3.6, launch=1.2),
    ],
    passives=[
        P("Slowing Field", "Enemies around you are slowed by the heavy air.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
        P("Sidestepped Blow", "A chance to dodge a blow entirely.", "dodge", "minecraft:ender_pearl", chance=.12, rest=100),
        P("Time Gained", "Blows in a row grow stronger until you stop fighting.", "momentum", "minecraft:clock", per_stack=.05, max_stacks=6),
        P("Saved Hours", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=.2),
        P("Borrowed Moment", "A kill heals you and quickens you.", "kill_reward", "minecraft:glass_bottle", effect="speed", heal=2, seconds=5),
        P("Quiet Hour", "Take less damage at night.", "reduce_when", "minecraft:black_bed", condition="night", pct=.12),
        P("Perfect Moment", "Your blows may strike critically.", "crit", "minecraft:glowstone_dust", chance=.13, mult=1.7),
    ],
    finishers=[
        A("Moment Eternal", "Stop the whole field for a long breath.", "stun_ring", "minecraft:totem_of_undying", "MAGIC_CIRCLE",
          radius=10, seconds=4, targets=14, cooldown=1800, cost=.07),
        A("Century Fall", "A hundred years of weight fall on the place you look at.", "meteor_drop", "minecraft:anvil", "ASH_FORMATION",
          damage=32, radius=7, range=24, delay=36, cooldown=1600, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="asura_ascension", name="Asura Ascension", franchise=F,
    desc="The bloodline of the gods of Epheotus. A towering, almost unkillable fighter who stands where a mountain would fall, crushes with pressure and lands like a falling sky.",
    color="#FFD700", background="tower", icon="minecraft:gold_block",
    book_name="Hymn of Epheotus", book_lore="They did not come to Dicathen to be gentle.",
    stances=[
        S("Asura Body", "A body made for lifting mountains: tough and unmovable, a little slow.", "minecraft:gold_block", "MIRROR_PANE",
          armor=4, damage_pct=.15, knockback_res=.3, speed_pct=-.05),
        S("Asura's Fury", "Anger sets the pressure free: huge blows, a thin guard.", "minecraft:blaze_powder", "DEVIL_CIRCLE",
          damage_pct=.35, armor=-3, speed_pct=.05),
    ],
    stats=[
        T("Divine Frame", "Raises your maximum health.", "max_health", 8, "add", "minecraft:enchanted_golden_apple"),
        T("Godly Strength", "Raises your attack damage.", "attack_damage", 1.4, "add", "minecraft:gold_ingot"),
        T("Sovereign Hide", "Raises your armor toughness.", "armor_toughness", 1, "add", "minecraft:netherite_ingot"),
        T("Unmoving", "Makes you very hard to knock back.", "knockback_resistance", .1, "add", "minecraft:anvil"),
    ],
    arts=[
        A("Asura's Descent", "Leap and fall like a spear thrown by a god.", "leap_slam", "minecraft:gold_block", "EARTH_SPIKES",
          damage=11, radius=3.6, height=1, knockback=1.8),
        A("Radiant Lance of Epheotus", "A lance of golden light through everything ahead.", "ray_shot", "minecraft:end_rod", "LIGHTNING_SPEAR",
          damage=9, range=22, pierce=3),
        A("Crushing Pressure", "Let your pressure fall: the creatures around you cannot move.", "stun_ring", "minecraft:obsidian", "MAGIC_CIRCLE",
          radius=5, seconds=2, targets=6),
        A("Asura's Dance of War", "Whirl in a ring of gold, hitting everything around you again and again.", "whirlwind", "minecraft:golden_sword", "WIND_TORNADO",
          damage=3.5, radius=3.6, hits=6),
        A("Immovable Mountain", "Plant your feet: a huge absorption shield and resistance.", "shield", "minecraft:gold_block", "MIRROR_PANE",
          absorb_hearts=10, seconds=9, resist_amp=1),
        A("Godkiller Blow", "A single blow that is deadlier the weaker the foe is.", "execute", "minecraft:netherite_axe", "FLAME_EXPLOSION",
          damage=10, range=3.8, bonus=1),
        A("Challenge of the Elders", "Your voice makes every hostile in range come for you.", "taunt", "minecraft:goat_horn", "MAGIC_CIRCLE",
          radius=10, targets=8, resist_seconds=4),
    ],
    passives=[
        P("Divine Endurance", "Take less damage while in combat.", "reduce_when", "minecraft:golden_chestplate", condition="in_combat", pct=.12),
        P("Elder's Contempt", "Deal more damage to bosses.", "dmg_vs", "minecraft:dragon_egg", category="boss", bonus=.22),
        P("Never Fallen", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=.3, seconds=7),
        P("Bloodline Mending", "Your wounds close over time, even out of combat.", "mending", "minecraft:golden_apple", seconds=10, amount=1),
        P("Hungry Gods", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:rotten_flesh", pct=.06),
        P("Pride Spikes", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:gold_nugget", pct=.2, rest=20),
        P("Thunderous Fist", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=.7, chance=.35),
    ],
    finishers=[
        A("Ten Thousand Leagues", "The sky itself comes down on the place you look at.", "meteor_drop", "minecraft:nether_star", "FLAME_EXPLOSION",
          damage=36, radius=7.5, range=26, delay=36, cooldown=1700, cost=.08),
        A("Epheotus Judgement", "A ring of golden shock bursts out of you in every direction.", "ring_burst", "minecraft:beacon", "SPIRIT_AURA",
          damage=28, radius=10, targets=16, knockback=2.5, cooldown=1500, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="dragon_will", name="Indrath's Dragon Will", franchise=F,
    desc="The will of the great dragon's lineage. A proud style of breath, claws and wings, that stands over the field and judges it.",
    color="#FFA000", background="storm", icon="minecraft:dragon_head",
    book_name="Scale of the Great Dragon", book_lore="Pride is a dragon's first armor.",
    stances=[
        S("Dragon's Pride", "Stand tall: stronger blows and a firmer guard, but heavier on your feet.", "minecraft:dragon_head", "FIRE_SPIRAL",
          damage_pct=.25, armor=2, speed_pct=-.04),
        S("Wyrm's Flight", "Spread your wings: much faster and a faint recovery, but a thin guard.", "minecraft:elytra", "WIND_RING",
          speed_pct=.2, regen=.4, armor=-2),
    ],
    stats=[
        T("Draconic Heart", "Raises your maximum health.", "max_health", 6, "add", "minecraft:dragon_breath"),
        T("Scale Plating", "Raises your armor.", "armor", 1.5, "add", "minecraft:scute"),
        T("Wingbeat", "You are quicker on your feet.", "movement_speed", .04, "mult", "minecraft:feather"),
        T("Talon Strength", "Raises your attack damage.", "attack_damage", 1.1, "add", "minecraft:iron_sword"),
    ],
    arts=[
        A("Breath of Indrath", "A column of dragonfire pierces everything along your gaze.", "ray_shot", "minecraft:dragon_breath", "FLAME_EXPLOSION",
          damage=9.5, range=20, pierce=3),
        A("Dragon's Gaze", "Fix a foe in a dragon's stare: it is marked and takes extra damage.", "mark", "minecraft:ender_eye", "MAGIC_CIRCLE",
          range=22, seconds=10, bonus=.3),
        A("Dive of the Wyrm", "Rise on your wings, then fall on the foes below.", "leap_slam", "minecraft:elytra", "WIND_RING",
          damage=10, radius=3.4, height=1.1, knockback=1.6),
        A("Wing Gust", "A beat of the wings sends a wave of wind along the ground.", "ground_wave", "minecraft:phantom_membrane", "WIND_SLASH",
          damage=6, length=11, width=3),
        A("Scale Guard", "Raise a guard of scales: a thick absorption shield.", "shield", "minecraft:scute", "MIRROR_PANE",
          absorb_hearts=8, seconds=9, resist_amp=1),
        A("Dragon's Roar", "A roar that pins the creatures around you in place.", "stun_ring", "minecraft:goat_horn", "WIND_RING",
          radius=5, seconds=2, targets=6),
        A("Tail Sweep", "Spin and sweep your tail around you again and again.", "whirlwind", "minecraft:iron_axe", "WIND_TORNADO",
          damage=3.5, radius=3.6, hits=5),
    ],
    passives=[
        P("Dragonfire Skin", "Take far less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire"),
        P("Hoard Instinct", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:emerald", chance=.12),
        P("Wyrm's Hunt", "Deal more damage to monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=.2),
        P("Stubborn Pride", "While in combat you stand tougher.", "self_effect_when", "minecraft:shield", effect="resistance", condition="in_combat"),
        P("Wings Open", "Take much less fall damage.", "fall_guard", "minecraft:feather", pct=.6),
        P("Sunlit Scales", "Heal while it is daytime.", "regen_when", "minecraft:sunflower", condition="day", per_second=.5),
        P("Heavy Claws", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=.6, chance=.35),
    ],
    finishers=[
        A("Indrath's Judgement Breath", "The full breath of a dragon: a huge beam through the field.", "ray_shot", "minecraft:dragon_breath", "FLAME_EXPLOSION",
          damage=38, range=34, pierce=6, cooldown=1600, cost=.08),
        A("Draconic Cataclysm", "A burning star dives from the sky onto the place you look at.", "meteor_drop", "minecraft:fire_charge", "FLAME_EXPLOSION",
          damage=34, radius=7, range=26, delay=36, cooldown=1600, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="bonded_beast", name="Bonded Beast: Sylv's Wing", franchise=F,
    desc="A fighter and a bonded dragon-beast who share one fight. The line is a support brawler's: heal, rally, strike together, and always keep the partner within reach.",
    color="#4DD0E1", background="forest", icon="minecraft:wolf_spawn_egg",
    book_name="The Bond Journal", book_lore="Two names, one heartbeat.",
    stances=[
        S("Bond Link", "Keep the link wide open: a gentle regeneration and a firmer guard.", "minecraft:lead", "SPIRIT_AURA",
          regen=.7, armor=1),
        S("Joint Hunt", "Hunt as one: stronger and sharper, a thinner guard.", "minecraft:bone", "WIND_SLASH",
          damage_pct=.2, crit_chance=.08, armor=-2),
    ],
    stats=[
        T("Shared Heartbeat", "Raises your maximum health.", "max_health", 4, "add", "minecraft:heart_of_the_sea"),
        T("Partner's Scales", "Raises your armor.", "armor", 1, "add", "minecraft:scute"),
        T("Paired Reflexes", "You are quicker on your feet.", "movement_speed", .04, "mult", "minecraft:sugar"),
    ],
    arts=[
        A("Pack Roar", "A roar from both of you: you and your allies grow stronger for a while.", "buff_aura", "minecraft:goat_horn", "SPIRIT_AURA",
          effect="strength", radius=9, seconds=12, amp=0),
        A("Bonded Warmth", "The link floods the group with comfort: you and your allies heal.", "heal_pulse", "minecraft:honey_bottle", "SPIRIT_AURA",
          radius=8, amount=6),
        A("Spirit Bolts", "Your partner and you loose a volley of bolts together.", "barrage", "minecraft:amethyst_shard", "SPIRIT_AURA",
          damage=3.2, shots=7, range=15),
        A("Link Step", "Trade places with your partner a few blocks ahead.", "blink", "minecraft:ender_pearl", "SPATIAL_RIFT",
          distance=10),
        A("Shared Bite", "Strike a foe together: you heal from the damage dealt.", "drain_bite", "minecraft:bone", "SPIRIT_AURA",
          damage=7, range=3.4, heal_pct=.4),
        A("Guardian Stance", "Your partner and you draw the attention of every hostile in range.", "taunt", "minecraft:shield", "MIRROR_PANE",
          radius=9, targets=6, resist_seconds=4),
        A("Joint Pounce", "Spring together at the foes below and land hard.", "leap_slam", "minecraft:anvil", "WIND_RING",
          damage=9, radius=3.2, height=1, knockback=1.4),
    ],
    passives=[
        P("Pack Warmth", "Allies around you resist a bit more.", "aura", "minecraft:white_wool", effect="resistance", target="allies", radius=7),
        P("Bonded Leech", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:rotten_flesh", pct=.06),
        P("Partner's Watch", "A chance to dodge a blow entirely, warned by your partner.", "dodge", "minecraft:ender_eye", chance=.11, rest=100),
        P("Shared Rations", "You tire more slowly.", "hunger_saver", "minecraft:cooked_beef", pct=.2),
        P("Hearth Bond", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=.12),
        P("Sunny Spirits", "In daylight you move more lightly.", "self_effect_when", "minecraft:sunflower", effect="speed", condition="day"),
        P("Protective Flare", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:blaze_powder", pct=.2, rest=20),
    ],
    finishers=[
        A("Twin Roar", "Both of you roar: a ring of force bursts outward.", "ring_burst", "minecraft:goat_horn", "SPIRIT_AURA",
          damage=26, radius=9, targets=14, knockback=2.2, cooldown=1400, cost=.07),
        A("Linked Breath", "A single beam fired together, through everything ahead.", "ray_shot", "minecraft:end_rod", "LIGHTNING_SPEAR",
          damage=36, range=32, pierce=5, cooldown=1500, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="elenoir_wind_weaver", name="Elenoir Wind-Weaver", franchise=F,
    desc="Elf wind magic: blades of air, a gale to lift friends, a step that skips the ground. A fast, elegant caster who never quite stays where the foe thinks she is.",
    color="#66BB6A", background="forest", icon="minecraft:feather",
    book_name="Leaf-Song Codex of Elenoir", book_lore="The forest hums. Learn the tune.",
    stances=[
        S("Gale Dancer", "Dance with the wind: much faster and light on your feet, but a thin guard.", "minecraft:feather", "WIND_RING",
          speed_pct=.2, armor=-1, crit_chance=.05),
        S("Elven Guard", "Stand with the grove behind you: a firmer guard and gentle recovery.", "minecraft:oak_leaves", "ELF_CIRCLE",
          armor=3, regen=.5, knockback_res=.1),
    ],
    stats=[
        T("Elven Grace", "You are quicker on your feet.", "movement_speed", .05, "mult", "minecraft:sugar"),
        T("Leaf Heart", "Raises your maximum health.", "max_health", 4, "add", "minecraft:oak_sapling"),
        T("Spring in the Step", "Raises your jump strength.", "jump_strength", .12, "mult", "minecraft:rabbit_foot"),
        T("Sharp Gale", "Raises your attack damage.", "attack_damage", .9, "add", "minecraft:iron_sword"),
    ],
    arts=[
        A("Cutting Gale", "A thin blade of wind races along the ground in front of you.", "ground_wave", "minecraft:iron_sword", "WIND_SLASH",
          damage=6, length=11, width=2),
        A("Wind Dance", "Spin in a swirl of blades, hitting everything around you.", "whirlwind", "minecraft:feather", "WIND_TORNADO",
          damage=3.2, radius=3.5, hits=6),
        A("Gale Step", "Slip into a gust and step out several blocks ahead.", "blink", "minecraft:chorus_fruit", "WIND_RING",
          distance=11),
        A("Tailwind Blessing", "A fresh wind carries you and your allies: faster for a while.", "buff_aura", "minecraft:white_banner", "WIND_RING",
          effect="speed", radius=9, seconds=12, amp=0),
        A("Smothering Gust", "A heavy wind drives into a cone of foes: they stumble and slow.", "debuff_cone", "minecraft:cobweb", "WIND_TORNADO",
          range=6, half_angle=50, seconds=8, effect="nausea", effect2="slowness", seconds2=6),
        A("Updraft Hook", "A hook of wind pulls the creatures around you in.", "pull", "minecraft:fishing_rod", "WIND_RING",
          radius=8, strength=1, targets=5, damage=4),
        A("Zephyr Crash", "Rise on the wind, then crash down on the foes below.", "leap_slam", "minecraft:anvil", "WIND_RING",
          damage=9, radius=3.2, height=1.1, knockback=1.4),
    ],
    passives=[
        P("Leaf Dodge", "A chance to dodge a blow entirely.", "dodge", "minecraft:oak_leaves", chance=.13, rest=90),
        P("Treetop Landing", "Take much less fall damage.", "fall_guard", "minecraft:feather", pct=.6),
        P("Windrunner", "Deal more damage while sprinting.", "sprint_bonus", "minecraft:leather_boots", bonus=.2),
        P("Dayspring Mend", "Heal while it is daytime.", "regen_when", "minecraft:sunflower", condition="day", per_second=.5),
        P("Springing Aura", "Allies around you jump higher.", "aura", "minecraft:rabbit_foot", effect="jump_boost", target="allies", radius=7),
        P("Keen Edge", "Your blows may strike critically.", "crit", "minecraft:flint", chance=.13, mult=1.7),
        P("Forest Patience", "Your wounds close over time, even out of combat.", "mending", "minecraft:apple", seconds=15, amount=1),
    ],
    finishers=[
        A("Elenoir's Hurricane", "A hurricane of blades whirls about you and everything near.", "whirlwind", "minecraft:wind_charge", "WIND_TORNADO",
          damage=8, radius=5.5, hits=9, interval=4, cooldown=1300, cost=.07),
        A("Sky-Splitting Gale", "A great gale cuts a lane across the field.", "ground_wave", "minecraft:elytra", "WIND_SLASH",
          damage=30, length=22, width=5, knockback=1.8, cooldown=1400, cost=.06),
    ],
))
