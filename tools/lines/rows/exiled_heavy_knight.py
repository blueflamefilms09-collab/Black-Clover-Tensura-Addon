"""Unique Skill Lines of "The Exiled Heavy Knight Knows How to Game the System" (25 lines).

The fantasy of this franchise: a knight who treats the world like a game he has mastered: threat tables, animation cancels, stat allocation, boss patterns, cheese
spots, hidden classes. The lines are invented in the spirit of the work (the game-system ideas), they do not claim to be canon skill names.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "exiled_heavy_knight"


def S(n, d, i, **p):
    return Stance(n, d, i, p)


def T(n, d, attr, amt, i, op="add"):
    return Stat(n, d, attr, amt, op, i)


def A(n, d, kind, i, **p):
    return Art(n, d, kind, p, i)


def P(n, d, kind, i, **p):
    return Pas(n, d, kind, p, i)


def LN(id, name, desc, color, bg, icon, bn, bl, stances, stats, arts, passives, finishers):
    return Line(id=id, name=name, franchise=F, desc=desc, color=color, background=bg, icon=icon, book_name=bn, book_lore=bl,
                stances=stances, stats=stats, arts=arts, passives=passives, finishers=finishers)


LINES = [
    # 1 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("aggro_manipulation", "Threat Table Mastery",
       "Read and bend the hidden threat table of every monster so the whole room hits the armor and nothing else. A tank's discipline: hold attention, soak it, hand it back.",
       "#B03A2E", "iron", "minecraft:bell",
       "Ledger of the Held Line", "Every number on the threat table is a leash, and he holds all the ends.",
       [S("Hate Generator Posture", "Plant your feet and grow loud: heavy armor and a wall of knockback resistance, at the price of slower swings and a little speed.", "minecraft:iron_chestplate",
          damage_pct=-0.1, armor=4, knockback_res=0.3, speed_pct=-0.05, vfx="MAGIC_CIRCLE"),
        S("Threat Handoff Posture", "Stay light on your feet and shave your armor to keep the spotlight moving between you and your allies.", "minecraft:leather_boots",
          speed_pct=0.12, armor=-1, damage_pct=0.05, vfx="WIND_RING")],
       [T("Raid-Tank Constitution", "Extra hearts for the one who is meant to be hit.", "max_health", 4, "minecraft:golden_apple"),
        T("Plate of Mitigation", "Thicker armor values from tuning your gear for the threat table.", "armor", 1.5, "minecraft:iron_chestplate"),
        T("Anchor Stance Weight", "You are harder to shove out of position.", "knockback_resistance", 0.1, "minecraft:anvil"),
        T("Toughness Rating Bonus", "Armor toughness against the biggest hits.", "armor_toughness", 1, "minecraft:netherite_scrap")],
       [A("Aggro Spike", "Slam your shield on the ground and roar: nearby monsters turn on you and are dazed with resistance for a moment.", "taunt", "minecraft:bell", radius=9, targets=6),
        A("Threat Hook", "Fling a hooked chain of light and haul the creatures around you into striking range.", "pull", "minecraft:fishing_rod", vfx="SEAL_CHAINS", damage=4),
        A("Hate Shockwave", "A ring of force bursts from your shield and shoves back everything that crowded you.", "ring_burst", "minecraft:shield", vfx="WIND_RING", damage=6, knockback=1.2),
        A("Mitigation Window", "Brace behind a glowing barrier of absorption hearts for a few seconds.", "shield", "minecraft:iron_block", vfx="MAGIC_CIRCLE", absorb_hearts=6),
        A("Counter-Taunt Smash", "A broad shield sweep that hits a cone of foes in front of you.", "cone_strike", "minecraft:iron_axe", vfx="WIND_SLASH", damage=8, half_angle=55),
        A("Fixate Mark", "Pin a glowing target on one enemy so everyone hits it harder.", "mark", "minecraft:spyglass", vfx="DEVIL_CIRCLE"),
        A("Pillar of Provocation", "Steel yourself: resistance and glowing, the unmistakable silhouette of the tank.", "buff_self", "minecraft:lightning_rod", effect="resistance", effect2="glowing", vfx="SPIRIT_AURA", amp=1)],
       [P("Spiked Aggro Plate", "Part of every blow you take is thrown back at the attacker.", "thorns", "minecraft:cactus", pct=0.2),
        P("Mitigation Rotation", "You take less damage while locked in combat.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.1),
        P("Raid Resistance Aura", "Allies standing close to you gain resistance.", "aura", "minecraft:beacon", effect="resistance", target="allies"),
        P("Cooldown Panic Button", "When nearly dead, you are briefly shielded.", "last_stand", "minecraft:totem_of_undying"),
        P("Healer's Overtime", "A steady heal while the fight is on.", "regen_when", "minecraft:glistering_melon_slice", condition="in_combat"),
        P("Bash Knockback Rider", "Your hits may shove the target back.", "knockback_on_hit", "minecraft:piston"),
        P("Slow the Attacker", "Whoever hits you may be slowed.", "on_hurt_retaliate", "minecraft:cobweb", effect="slowness")],
       [A("Total Aggro Lock", "Every creature in a wide ring around you freezes to stare at you for a few breathless seconds.", "stun_ring", "minecraft:crying_obsidian",
          vfx="SEAL_CHAINS", radius=7, seconds=4, targets=10, cooldown=1500),
        A("Wipe Prevention Pulse", "A rolling holy pulse mends you and every ally in range.", "heal_pulse", "minecraft:enchanted_golden_apple", vfx="ELF_CIRCLE", radius=10, amount=14, cooldown=1400)]),

    # 2 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("animation_cancel", "Animation Cancel Technique",
       "Skip the recovery of every move: end a lunge halfway, step out of a swing, chain the next one before the last has landed. Frames are the only currency.",
       "#1ABC9C", "storm", "minecraft:feather",
       "Frame Data of the Skipped Heartbeat", "Nothing is slow if you refuse to finish it.",
       [S("Frame-Skip Stance", "Lean forward on the balls of your feet: much faster, a little crit, but your armor is pulled thin.", "minecraft:rabbit_foot",
          speed_pct=0.2, armor=-2, crit_chance=0.05, vfx="WIND_RING"),
        S("Recovery-Cancel Stance", "Swing without the wind-down: stronger blows and some speed, a thinner guard.", "minecraft:iron_sword",
          speed_pct=0.1, damage_pct=0.12, armor=-1, vfx="WIND_SLASH")],
       [T("Input Buffer Reflexes", "A faster swing recovery.", "attack_speed", 0.05, "minecraft:clock", op="mult"),
        T("Cancel-Step Legs", "A permanent boost to walking speed.", "movement_speed", 0.05, "minecraft:sugar", op="mult"),
        T("Hop-Cancel Spring", "You jump higher between moves.", "jump_strength", 0.08, "minecraft:slime_ball", op="mult"),
        T("Ledge Skip Soles", "You step up whole blocks without slowing.", "step_height", 0.2, "minecraft:leather_boots")],
       [A("Cancelled Lunge", "Lunge forward in a streak of wind and hit everything on the line before the animation can finish.", "dash_strike", "minecraft:iron_sword", vfx="WIND_SLASH", distance=7, damage=7),
        A("Input Buffer Blink", "Vanish from your spot and reappear ahead; the swing you queued is already waiting.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=9),
        A("Rising Cancel", "Cancel into an uppercut that throws the nearest foe skyward.", "uppercut", "minecraft:flint", vfx="WIND_SLASH", launch=0.9),
        A("Spin-Cancel Cyclone", "Spin on the spot, canceling each turn into the next, and shred what surrounds you.", "whirlwind", "minecraft:chain", vfx="WIND_TORNADO", hits=5),
        A("Hop-Cancel Slam", "A short leap canceled into a crash that cracks the ground below.", "leap_slam", "minecraft:mace", vfx="STONE_SPIKES"),
        A("Cancel Chain", "A blow that skips off the first target and hops to the next in line.", "chain_hit", "minecraft:iron_nugget", vfx="LIGHTNING_SPEAR", jumps=4),
        A("Weave Slash", "A wide, fast crescent slash before the foe can react.", "cone_strike", "minecraft:stone_sword", vfx="WIND_SLASH", damage=7, range=4.2)],
       [P("Sprint Window Bonus", "You hit harder while sprinting.", "sprint_bonus", "minecraft:leather_boots", bonus=0.25),
        P("Chain Frame Momentum", "Consecutive blows build up strength until you stop fighting.", "momentum", "minecraft:amethyst_shard"),
        P("I-Frame Roll", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane"),
        P("Hasted by the Run", "You are fast while sprinting.", "self_effect_when", "minecraft:sugar", effect="speed", condition="sprinting", amp=1),
        P("Canceled Combo Counter", "Every few blows in a row hit much harder.", "combo", "minecraft:blaze_powder"),
        P("Soft Landing Cancel", "You take less fall damage.", "fall_guard", "minecraft:feather"),
        P("Shove Cancel", "Your hits may knock the target back.", "knockback_on_hit", "minecraft:string")],
       [A("Zero-Frame Combo", "A blur: you dash through a whole pack in a single heartbeat and strike each of them.", "dash_strike", "minecraft:netherite_sword", vfx="LIGHTNING_FIEND",
          distance=13, damage=26, targets=9, cooldown=1300),
        A("Infinite Cancel Barrage", "Loose a flurry of cancelled shots at the foe you look at, every one skipping the last's recovery.", "barrage", "minecraft:crossbow", vfx="WIND_SLASH",
          damage=6, shots=10, range=20, cooldown=1300)]),

    # 3 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("perfect_parry_timing", "Perfect Parry Timing",
       "Win by being at the right frame: a parry that lands in the narrowest window turns the enemy's own strike into the opening. High risk, huge payoff.",
       "#F1C40F", "holy", "minecraft:shield",
       "Manual of the Narrow Window", "A tenth of a second is wide enough for a whole duel.",
       [S("Parry Window Stance", "Hold the guard high: sturdier and a little crit, but you swing slower and from a defensive crouch.", "minecraft:shield",
          armor=2, crit_chance=0.1, speed_pct=-0.05, vfx="MIRROR_PANE"),
        S("Counter-Opening Stance", "Open yourself on purpose: harder counters, thinner armor.", "minecraft:golden_sword",
          damage_pct=0.2, armor=-2, vfx="DEVIL_CIRCLE")],
       [T("Tempered Guard Arm", "Armor toughness from a perfectly timed guard.", "armor_toughness", 1, "minecraft:iron_ingot"),
        T("Quick Hands", "Faster swing recovery after a parry.", "attack_speed", 0.05, "minecraft:clock", op="mult"),
        T("Duelist's Reach", "A little longer reach to land the counter.", "entity_interaction_range", 0.4, "minecraft:stick"),
        T("Calm Heart", "A few more hearts, borne of staying calm.", "max_health", 2, "minecraft:apple")],
       [A("Frame-Perfect Parry", "For a few seconds, a share of every blow you take is thrown back at whoever swung.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", reflect_pct=0.45),
        A("Punish Window Strike", "A heavy strike that lands harder the weaker the opponent already is.", "execute", "minecraft:iron_sword", vfx="WIND_SLASH", damage=9, bonus=0.7),
        A("Staggering Deflection", "A ringing clang freezes everyone around you mid-swing.", "stun_ring", "minecraft:bell", vfx="MIRROR_PANE", radius=4, seconds=2),
        A("Guard-Break Launcher", "Smash upward through a raised guard and throw the foe off their feet.", "uppercut", "minecraft:golden_axe", vfx="WIND_SLASH", damage=8),
        A("Counter Crescent", "A clean crescent cut that rewards a parry that just landed.", "cone_strike", "minecraft:diamond_sword", vfx="WIND_SLASH", damage=8, half_angle=35),
        A("Read the Tell", "Study the foe: it takes extra damage for a while.", "mark", "minecraft:spyglass", vfx="MIRROR_PANE", bonus=0.35),
        A("Frozen Breath Cone", "A cold, sharp breath of fear: foes in front are slowed and weakened.", "debuff_cone", "minecraft:snowball", effect="slowness", effect2="weakness", vfx="WIND_RING", seconds=6, seconds2=6)],
       [P("Window Awareness", "You may dodge a blow completely.", "dodge", "minecraft:feather", chance=0.12),
        P("Counter Crit", "Your blows may strike critically.", "crit", "minecraft:iron_sword", chance=0.15),
        P("Reflect Barbs", "Part of every blow is returned to the attacker.", "thorns", "minecraft:cactus"),
        P("Opening Finisher", "Much more damage to nearly dead foes.", "execute_threshold", "minecraft:wooden_sword"),
        P("Blade-In-Hand Focus", "More damage while a sword is in hand.", "dmg_when", "minecraft:stone_sword", condition="holding_sword"),
        P("Dizzying Counter", "Whoever hits you may be made nauseous.", "on_hurt_retaliate", "minecraft:fermented_spider_eye", effect="nausea"),
        P("Calm After the Clash", "Your wounds close slowly over time.", "mending", "minecraft:glow_berries")],
       [A("Perfect Counter Verdict", "One concentrated strike that ends a hurt foe outright, rewarding a flawless read.", "execute", "minecraft:netherite_sword", vfx="LIGHTNING_SPEAR",
          damage=28, range=5, bonus=1.2, cooldown=1300),
        A("Mirror Guard Overdrive", "A long window in which the world's blows come back at the senders almost undiminished.", "ward_reflect", "minecraft:crying_obsidian", vfx="MIRROR_PANE",
          seconds=12, reflect_pct=0.7, cooldown=1500)]),

    # 4 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("min_max_build", "Min-Max Build Theory",
       "Every stat point goes exactly where it pays off most. Switch allocations on the fly between a cannon and a wall, then spend your cleverness on beams and burst.",
       "#8E44AD", "tower", "minecraft:enchanted_book",
       "Spreadsheet of the Optimal Allocation", "Nothing is wasted: every point has a job.",
       [S("Glass Cannon Allocation", "All points into offense: huge damage and a little speed, paid with armor.", "minecraft:tnt",
          damage_pct=0.35, armor=-4, speed_pct=0.05, vfx="FIRE_BURST"),
        S("Tank Allocation", "All points into defense: armor and knockback resistance, with slower, lighter blows.", "minecraft:iron_block",
          armor=6, damage_pct=-0.15, speed_pct=-0.1, knockback_res=0.3, vfx="MAGIC_CIRCLE")],
       [T("Strength Point Surplus", "Spare points go to raw attack power.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Vitality Point Surplus", "Spare points go to health.", "max_health", 4, "minecraft:golden_apple"),
        T("Defense Point Surplus", "Spare points go to armor.", "armor", 1, "minecraft:iron_chestplate"),
        T("Luck Point Dump", "A few points slipped into luck, just in case.", "luck", 1, "minecraft:rabbit_foot")],
       [A("Stat Point Dump", "Overload your body for a moment: strength and speed surge at once.", "buff_self", "minecraft:nether_star", effect="strength", effect2="speed", vfx="SPIRIT_AURA", seconds=14, amp=1),
        A("Weak Point Calculation", "A cold stare locates the seam in a foe's defense: it takes more damage for a while.", "mark", "minecraft:spyglass", vfx="MAGIC_CIRCLE", bonus=0.4),
        A("Overtuned Beam", "Fire a thin, perfectly tuned beam of force that pierces several foes in a row.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=9, pierce=3),
        A("Potion Reallocation", "Gulp a reserve potion: a quick heal and a trickle of regeneration.", "heal_self", "minecraft:potion", vfx="ELF_CIRCLE", amount=9),
        A("Party Respec", "Give yourself and your allies nearby a haste boost, as if each had been optimized.", "buff_aura", "minecraft:clock", effect="haste", vfx="MAGIC_CIRCLE", seconds=14),
        A("Optimal Finisher", "A strike that finds the exact weakness: it hits much harder the weaker the foe.", "execute", "minecraft:diamond_sword", vfx="WIND_SLASH", damage=9),
        A("Damage Cap Breaker", "A thin wave of force along the ground that ignores the usual limit and crashes through a line of foes.", "ground_wave", "minecraft:netherite_axe", vfx="STONE_SPIKES", damage=9, length=11)],
       [P("Critical Build", "Your blows may strike critically.", "crit", "minecraft:diamond", chance=0.18, mult=1.7),
        P("Full-HP Bonus", "More damage while at full health.", "dmg_when", "minecraft:golden_carrot", condition="full_health"),
        P("Efficient Leech", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:redstone"),
        P("Grind Efficiency", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle"),
        P("Low-HP Passive", "You take less damage when nearly dead.", "reduce_when", "minecraft:iron_ingot", condition="low_health", pct=0.2),
        P("Strength Aura", "Allies close to you gain strength.", "aura", "minecraft:beacon", effect="strength", target="allies"),
        P("Stamina Curve", "You tire more slowly.", "hunger_saver", "minecraft:bread")],
       [A("Hard-Cap Breaker Beam", "A massive, tight beam that bores through a whole line of enemies.", "ray_shot", "minecraft:netherite_ingot", vfx="LIGHTNING_FIEND", damage=30, range=28, pierce=5, cooldown=1400),
        A("Respec Meteor", "Redistribute the sky: after a pause, a meteor lands exactly where you looked.", "meteor_drop", "minecraft:fire_charge", vfx="FLAME_EXPLOSION", damage=34, radius=6, cooldown=1500)]),

    # 5 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("cheese_strategy", "Cheese Strategy",
       "The monsters' AI has holes and he knows each of them: blind them, leash them to a corner, snipe from where they cannot reach. Not honorable, extremely effective.",
       "#27AE60", "forest", "minecraft:cobweb",
       "Notes on the Unfair Corner", "Every boss has a spot where it cannot reach you.",
       [S("Out-of-Reach Posture", "Hold a spot the foes cannot path to: heavier armor and a little regeneration, at the cost of mobility.", "minecraft:ladder",
          armor=3, speed_pct=-0.1, regen=0.6, vfx="SHADOW_POOL"),
        S("Pillar-Hop Posture", "Keep bouncing from perch to perch: nimble and fast, but nearly unarmored.", "minecraft:scaffolding",
          speed_pct=0.15, armor=-2, crit_chance=0.05, vfx="WIND_RING")],
       [T("Ledge Climber", "Step up blocks as if they were stairs.", "step_height", 0.3, "minecraft:ladder"),
        T("Pillar Springs", "A higher jump to reach the safe perch.", "jump_strength", 0.1, "minecraft:slime_block", op="mult"),
        T("Soft-Landing Habit", "A longer safe fall.", "safe_fall_distance", 2, "minecraft:hay_block"),
        T("Long Arm of the Exploit", "Hit foes from just beyond their reach.", "entity_interaction_range", 0.5, "minecraft:blaze_rod")],
       [A("Leash-Break Smoke", "A cloud of smoke blinds the creatures around you while you slip free of their leash.", "smoke", "minecraft:gunpowder", vfx="SHADOW_POOL", radius=6),
        A("Safe-Spot Sniping", "From a spot they cannot reach, loose a patient volley at the target.", "barrage", "minecraft:bow", vfx="WIND_SLASH", shots=6, range=18),
        A("Pathing Confusion", "A wave of dizzying sparks in a cone: foes in front go blind and lose their way.", "debuff_cone", "minecraft:fermented_spider_eye", effect="blindness", effect2="slowness", vfx="POISON_BREATH"),
        A("Door-Frame Slip", "Pop through the narrowest gap: a short teleport forward.", "blink", "minecraft:ender_eye", vfx="SPATIAL_RIFT", distance=7),
        A("Stuck-Mob Drag", "Yank stragglers off their ledge and drag them into your kill zone.", "pull", "minecraft:lead", vfx="THREAD_LINE", damage=4),
        A("Predictable Pattern", "Mark the foe: it has repeated its move once too often and takes extra damage.", "mark", "minecraft:compass", vfx="MAGIC_CIRCLE"),
        A("Aggro Leash", "Draw nearby monsters to a spot of your choosing.", "taunt", "minecraft:lead", vfx="SEAL_CHAINS")],
       [P("Corner Evasion", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane"),
        P("Cushioned Fall", "You take less fall damage.", "fall_guard", "minecraft:hay_block"),
        P("High-Ground Bonus", "More damage while airborne.", "dmg_when", "minecraft:feather", condition="airborne"),
        P("Quiet Posture", "Less damage while sneaking.", "reduce_when", "minecraft:leather_helmet", condition="sneaking", pct=0.18),
        P("Hidden Spot Cloak", "While sneaking you turn invisible.", "self_effect_when", "minecraft:fermented_spider_eye", effect="invisibility", condition="sneaking"),
        P("Webbing Strikes", "Your blows may slow the foe.", "on_hit_effect", "minecraft:cobweb", effect="slowness"),
        P("Opportunist's Loot", "A kill heals you a little and gives a short burst of speed.", "kill_reward", "minecraft:gold_nugget", effect="speed")],
       [A("Staircase Trap Barrage", "A long volley of shots into a pack that cannot climb the stairs to answer.", "barrage", "minecraft:crossbow", vfx="WIND_SLASH", damage=6, shots=12, range=24, cooldown=1300),
        A("Cliff-Edge Drop", "Pull a stone from the sky: after a brief delay it falls on the spot you looked at.", "meteor_drop", "minecraft:dripstone_block", vfx="STONE_SPIKES", damage=32, radius=5, cooldown=1400)]),

    # 6 ------------------------------------------------------------------------------------------------------------------------------------------
    LN("hidden_class_berserker", "Hidden Class: Berserker Knight",
       "A class no menu lists: unlocked by being near death at the right level with the right weapon. Hits harder the more it bleeds, and heals itself by hitting.",
       "#922B21", "blood", "minecraft:netherite_axe",
       "Scroll of the Unlisted Class", "It only appears for those who ignore the warnings.",
       [S("Frenzy Allocation", "Throw the guard away: big damage and crit, painfully low armor.", "minecraft:iron_axe",
          damage_pct=0.3, armor=-3, crit_chance=0.1, vfx="FLAME_TRAIL"),
        S("Bloodrush Calm", "A controlled rage: regeneration and a bit of speed with a modest damage boost.", "minecraft:redstone",
          regen=1.5, speed_pct=0.05, damage_pct=0.1, vfx="SPIRIT_AURA")],
       [T("Berserker Might", "A permanent boost to attack power.", "attack_damage", 1.5, "minecraft:netherite_axe"),
        T("Blood Reserve", "A few more hearts.", "max_health", 2, "minecraft:redstone"),
        T("Rage Tempo", "Swing noticeably faster.", "attack_speed", 0.06, "minecraft:blaze_powder", op="mult"),
        T("Unflinching Stance", "You are harder to shove around.", "knockback_resistance", 0.1, "minecraft:anvil")],
       [A("Crash Down Axe", "Leap high and crash into the ground with an axe-fall that rocks the foes below.", "leap_slam", "minecraft:iron_axe", vfx="STONE_SPIKES", damage=9, radius=3.5),
        A("Rampage Spin", "Spin, axe out, and slash everything around you several times.", "whirlwind", "minecraft:netherite_axe", vfx="WIND_TORNADO", hits=5, damage=3.5),
        A("Bloody Bite", "Rip into a foe in front of you and heal for a share of what you took.", "drain_bite", "minecraft:rotten_flesh", vfx="DEVIL_CIRCLE", heal_pct=0.4),
        A("Charge of the Unlisted", "Rush forward in a rolling charge that smashes through the first foes in your way.", "dash_strike", "minecraft:iron_boots", vfx="FLAME_TRAIL", damage=7),
        A("Shock of the Roar", "A thunderous roar sends a ring of pressure out of your chest.", "ring_burst", "minecraft:goat_horn", vfx="WIND_RING", damage=6),
        A("Axe Cleave", "A wide axe arc that hits a fan of enemies.", "cone_strike", "minecraft:golden_axe", vfx="FLAME_EXPLOSION", damage=9, half_angle=60),
        A("Bleed It Out", "Drink in the rage: strength and speed for a short while.", "buff_self", "minecraft:red_dye", effect="strength", effect2="speed", vfx="FIRE_BURST")],
       [P("Blood Siphon", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:redstone", pct=0.08),
        P("Wounded Regeneration", "A steady heal at low health.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.0),
        P("Desperate Fury", "More damage at low health.", "dmg_when", "minecraft:blaze_powder", condition="low_health", bonus=0.3),
        P("Rampage Streak", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_axe"),
        P("Feast on the Fallen", "A kill heals you and fills you with strength.", "kill_reward", "minecraft:bone", effect="strength", heal=3),
        P("Not Yet", "When nearly dead, a brief shield.", "last_stand", "minecraft:totem_of_undying"),
        P("Gut Punch Crits", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15)],
       [A("Unlocked Skill: Rampage", "A rolling wave of splintered earth tears out of your axe and runs far ahead of you.", "ground_wave", "minecraft:netherite_axe", vfx="EARTH_SPIKES",
          damage=28, length=16, width=3, cooldown=1300),
        A("Hidden Class Cataclysm", "Release all the stored rage in a single ring of explosive force.", "ring_burst", "minecraft:tnt", vfx="FLAME_EXPLOSION", damage=26, radius=8, targets=12, cooldown=1400)]),
]
