"""The God of High School: 27 unique skill lines (martial arts, Ki, Charyeok and the gods that lend it, tournament roles).

Canon note: martial arts (Taekwondo, Muay Thai, Capoeira, Judo, Hapkido, Kendo ...) are real styles written as game arts; Charyeok (borrowed power) lines
follow the story's idea of a being or god lending its strength, while the exact moves are invented in its spirit (what the art looks and feels like in the game).
Lines 26 and 27 (3.26.0) are Evolution skills (docs/skills/evolution_ten.md rows 9 and 10): they carry an `evolution` block, evolved passives and an Ultimate finisher.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "god_of_high_school"


def S(name, desc, icon, **p):
    return Stance(name, desc, icon, p)


def T(name, desc, attribute, amount, icon, op="add"):
    return Stat(name, desc, attribute, amount, op, icon)


def A(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon)


def P(name, desc, kind, icon, **p):
    return Pas(name, desc, kind, p, icon)


LINES = []

# ---------------------------------------------------------------------------------------------------------------- 1
LINES.append(Line(
    id="goh_taekwondo", name="Taekwondo Kicker", franchise=F,
    desc="Korean kicking art built on speed, height and hip power. Flying side kicks, axe kicks from above and a tornado kick that clears the ring.",
    color="#EF5350", background="storm", icon="minecraft:iron_boots",
    book_name="Kyorugi Handbook", book_lore="The first rule in the margin: if your foot is not higher than their head, you are late.",
    stances=[
        S("Kyorugi Bounce Stance", "Bounce on the balls of your feet: quick and sharp-kicking, with a loose guard.", "minecraft:leather_boots", speed_pct=0.08, crit_chance=0.05, armor=-1, vfx="WIND_RING"),
        S("Poomsae Form Stance", "Hold the clean forms: firm and steady, at a slightly softer strike.", "minecraft:white_banner", armor=2, regen=0.4, damage_pct=-0.03, vfx="WIND_RING"),
    ],
    stats=[
        T("Kicker's Legs", "Years of kicks give you a higher jump.", "jump_strength", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Hip Power", "A kick comes from the hip, not the foot.", "attack_damage", 1, "minecraft:iron_boots"),
        T("Quick Footwork", "You close the gap in a blink.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Flexible Frame", "Your kicks come out faster.", "attack_speed", 0.2, "minecraft:feather"),
    ],
    arts=[
        A("Flying Side Kick", "Launch yourself forward with a side kick that hits everything along your path.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=7, distance=8),
        A("Rising Crescent Kick", "Whip a high crescent kick under the foe's chin and send it into the air.", "uppercut", "minecraft:feather", vfx="WIND_SLASH", damage=7, launch=0.8),
        A("Tornado Kick", "Spin in the air with a series of kicks that hit everyone around you.", "whirlwind", "minecraft:wind_charge", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Axe Kick Drop", "Leap high and bring your heel down on the foes below.", "leap_slam", "minecraft:iron_axe", vfx="WIND_SLASH", damage=8, height=1.0),
        A("Rapid Fire Kicks", "Throw a blur of snap kicks at the foe in front of you.", "barrage", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=3, shots=6),
        A("Roundhouse Sweep", "A big roundhouse that swings through a wide cone ahead.", "cone_strike", "minecraft:iron_leggings", vfx="WIND_SLASH", damage=7, half_angle=60),
        A("Skip Step", "Skip off the floor and reappear a few blocks ahead.", "blink", "minecraft:chorus_fruit", vfx="WIND_RING", distance=8),
    ],
    passives=[
        P("Head-Height Precision", "Your kicks land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Kick Combination", "Every few kicks in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Run-Up Power", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.2),
        P("Slip the Strike", "You may slip a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        P("Rhythm of the Match", "The longer the fight, the more your kicks gather strength.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Light Landing", "You land softly and take less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.4),
        P("Heel Drive", "Your blows may knock the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
    ],
    finishers=[
        A("Thousand Kick Barrage", "A thousand kicks in a heartbeat: a huge flurry at the foe in front of you.", "barrage", "minecraft:diamond_boots", vfx="WIND_SLASH", damage=5, shots=12, range=10, cost=0.08, cooldown=1800),
        A("Meteor Axe Kick", "Rocket into the sky and fall as a single devastating heel drop that rings out in a shockwave.", "leap_slam", "minecraft:mace", vfx="WIND_RING", damage=26, radius=5, knockback=1.8, height=1.2, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 2
LINES.append(Line(
    id="goh_muay_thai", name="Muay Thai Clinch", franchise=F,
    desc="The art of eight limbs: elbows, knees, shins and a clinch that drags the foe in close. Slow to start, brutal at close range.",
    color="#F9A825", background="blood", icon="minecraft:golden_boots",
    book_name="The Eight Limbs Ledger", book_lore="Every entry is a shin bruise with a date next to it.",
    stances=[
        S("Orthodox Guard Stance", "Hands high, chin down: sturdy and hard to push back, but slower.", "minecraft:iron_chestplate", armor=3, knockback_res=0.3, speed_pct=-0.08, vfx="SPIRIT_AURA"),
        S("Eight Limbs Stance", "Open up every limb: stronger blows and crits, thinner guard.", "minecraft:golden_boots", damage_pct=0.15, crit_chance=0.05, armor=-1, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Conditioned Shins", "Years of bag work make your shins like wood.", "armor", 1, "minecraft:iron_leggings"),
        T("Iron Neck", "You can take a head-shot.", "max_health", 2, "minecraft:cooked_beef"),
        T("Clinch Strength", "When you hold something, it stays held.", "attack_damage", 1, "minecraft:golden_axe"),
        T("Ring Stance", "A low, steady base.", "knockback_resistance", 0.1, "minecraft:anvil"),
    ],
    arts=[
        A("Clinch Pull", "Grab the creatures around you and haul them in to elbow range.", "pull", "minecraft:lead", vfx="WIND_RING", radius=6, strength=0.9, damage=4),
        A("Jumping Knee", "Spring into the foe in front of you with a knee that hits harder the weaker it is.", "execute", "minecraft:iron_axe", vfx="WIND_SLASH", damage=8, bonus=0.8),
        A("Spinning Elbow", "Spin on the heel with elbows out and hit everything around you.", "whirlwind", "minecraft:golden_axe", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Elbow Combo", "Hit the foe in front with an elbow, then the next, and the next.", "chain_hit", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=5, jumps=3),
        A("Wai Kru Focus", "Bow, breathe and settle: strength and resistance for a while.", "buff_self", "minecraft:golden_apple", vfx="SPIRIT_AURA", effect="strength", effect2="resistance", seconds=12),
        A("Teep Lift", "A push kick that lifts the foe off its feet.", "uppercut", "minecraft:golden_boots", vfx="WIND_SLASH", damage=7),
        A("Corner Recovery", "A moment in the corner: heal and regenerate.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=7),
    ],
    passives=[
        P("Shin Guard", "Part of every blow against you is returned.", "thorns", "minecraft:iron_leggings", pct=0.15),
        P("Elbow Guard", "While in a fight, you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.1),
        P("Blood in the Ring", "Part of what you deal comes back as health.", "lifesteal", "minecraft:redstone", pct=0.05),
        P("Knockout Sense", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.7),
        P("Combination Rhythm", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.5),
        P("Knee Shove", "Your strikes may drive the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
        P("Fight Fuel", "A kill restores a little health.", "kill_reward", "minecraft:cooked_beef", heal=3, effect="none"),
    ],
    finishers=[
        A("Eight Limbs Storm", "Every limb at once: a vicious storm of knees and elbows around you.", "whirlwind", "minecraft:golden_boots", vfx="WIND_TORNADO", damage=5, hits=8, radius=4.5, cost=0.09, cooldown=1800),
        A("Fight-Ending Knee", "One clean knee to the body: far deadlier the lower the foe's health.", "execute", "minecraft:netherite_axe", vfx="FIRE_BURST", damage=26, bonus=2.2, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 3
LINES.append(Line(
    id="goh_capoeira", name="Capoeira Ginga", franchise=F,
    desc="Dance and fight of the roda: constant swaying, hand-stands, sweeping low kicks and moves from upside-down. Hard to hit, harder to read.",
    color="#43A047", background="city", icon="minecraft:rabbit_foot",
    book_name="Songbook of the Roda", book_lore="The lyrics tell you how to move; the footnotes tell you what to do about the knife.",
    stances=[
        S("Ginga Sway", "Never stand still: much faster and elusive, but thin-skinned.", "minecraft:rabbit_foot", speed_pct=0.1, crit_chance=0.04, armor=-1, vfx="WIND_RING"),
        S("Roda Guard", "Settle low into the circle: steadier, with some armor and recovery.", "minecraft:leather_chestplate", armor=2, regen=0.4, speed_pct=0.02, vfx="WIND_RING"),
    ],
    stats=[
        T("Acrobat's Spine", "You jump higher than your size should allow.", "jump_strength", 0.08, "minecraft:rabbit_foot", op="mult"),
        T("Swaying Step", "Your feet never stop.", "movement_speed", 0.05, "minecraft:leather_boots", op="mult"),
        T("Dancer's Balance", "You land on your hands and feet from any height.", "safe_fall_distance", 2, "minecraft:slime_ball"),
        T("Spinning Power", "Kicks from a spin carry real weight.", "attack_damage", 1, "minecraft:iron_boots"),
    ],
    arts=[
        A("Esquiva Roll", "Roll under the blow and come up a few blocks away.", "blink", "minecraft:chorus_fruit", vfx="WIND_RING", distance=8),
        A("Rabo de Arraia", "Cartwheel into a low kick that carries you through the foes in front.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=7, distance=7),
        A("Dust Kick", "Sweep your foot through the sand: foes in front of you are blinded and slowed.", "debuff_cone", "minecraft:sand", vfx="ASH_FORMATION", effect="blindness", effect2="slowness", seconds=5, seconds2=6),
        A("Au Flip Kick", "Flip upside down and come down on the foes below with both heels.", "leap_slam", "minecraft:rabbit_foot", vfx="WIND_RING", damage=8, height=1.0),
        A("Martelo Sweep", "A hammer-kick that sweeps the whole cone ahead.", "cone_strike", "minecraft:iron_leggings", vfx="WIND_SLASH", damage=7, half_angle=60),
        A("Berimbau Rhythm", "Hear the berimbau and let it quicken you: speed and haste for a while.", "buff_self", "minecraft:note_block", vfx="SPIRIT_AURA", effect="speed", effect2="haste", seconds=14),
        A("Cloud of Sand", "Throw a handful of sand: everyone near you cannot see where you went.", "smoke", "minecraft:sand", vfx="ASH_FORMATION", radius=5, seconds=6),
    ],
    passives=[
        P("Ginga Evade", "A trained dancer slips most blows: you may dodge them completely.", "dodge", "minecraft:feather", chance=0.14),
        P("Flowing Combat", "The longer you keep dancing, the harder you strike.", "momentum", "minecraft:note_block", per_stack=0.04, max_stacks=6),
        P("Running Rhythm", "While sprinting, you gain a boost of speed.", "self_effect_when", "minecraft:sugar", effect="speed", condition="sprinting"),
        P("Safe Landing", "You take less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.5),
        P("Heel Catch", "Surprising kicks land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Dance Breath", "While sprinting you heal slowly.", "regen_when", "minecraft:glow_berries", condition="sprinting", per_second=0.5),
        P("Sandy Heels", "Your kicks may slow the foe with a spray of sand.", "on_hit_effect", "minecraft:sand", effect="slowness", chance=0.22),
    ],
    finishers=[
        A("Dance of a Thousand Spins", "A ferocious whirling dance of kicks that hits everything around you again and again.", "whirlwind", "minecraft:music_disc_cat", vfx="WIND_TORNADO", damage=5, hits=8, radius=4.5, cost=0.09, cooldown=1800),
        A("Roda Crescendo", "The circle closes and every beat lands at once: a huge ring that throws foes back.", "ring_burst", "minecraft:note_block", vfx="WIND_RING", damage=22, radius=6, targets=8, knockback=1.8, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 4
LINES.append(Line(
    id="goh_judo", name="Judo Throw", franchise=F,
    desc="The gentle way, turned into a weapon: grab, unbalance and throw. You use the foe's own weight, and the mat is always near.",
    color="#1E88E5", background="iron", icon="minecraft:white_banner",
    book_name="Kodokan Kitbag Notes", book_lore="A folded belt is tucked between the pages.",
    stances=[
        S("Kumi-Kata Grip Stance", "Take the grip first: sturdy, hard to shove, a little slow.", "minecraft:lead", armor=2, knockback_res=0.4, speed_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Throwing Stance", "Lean into the throw: stronger blows, with less guard.", "minecraft:white_banner", damage_pct=0.15, armor=-1, vfx="WIND_RING"),
    ],
    stats=[
        T("Grip Strength", "A grip that does not let go.", "attack_damage", 1, "minecraft:lead"),
        T("Low Center", "You are hard to push over.", "knockback_resistance", 0.15, "minecraft:anvil"),
        T("Iron Back", "Years on the mat toughen the spine.", "max_health", 2, "minecraft:cooked_beef"),
        T("Break-Fall Skill", "You hit the mat, not the floor.", "safe_fall_distance", 2, "minecraft:white_wool"),
    ],
    arts=[
        A("Grab the Collar", "Seize the creatures around you and drag them into throwing range.", "pull", "minecraft:lead", vfx="WIND_RING", radius=6, strength=0.9, damage=3),
        A("Uchi Mata", "An inner-thigh throw that hoists the foe in front of you high into the air.", "uppercut", "minecraft:white_banner", vfx="WIND_SLASH", damage=7, launch=0.9),
        A("Counter Throw", "For a few seconds, a share of every blow you take is turned back on the attacker.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Tatami Slam", "Jump and drive the foe into the floor beneath you.", "leap_slam", "minecraft:white_wool", vfx="WIND_RING", damage=8, height=0.8),
        A("Choke Hold", "A quick hold that stops the creatures around you in place for a moment.", "stun_ring", "minecraft:chain", vfx="SEAL_CHAINS", radius=3.5, seconds=2),
        A("Ukemi", "Tuck, roll and spread the impact: a shield of absorption.", "shield", "minecraft:leather_chestplate", vfx="SPIRIT_AURA", absorb_hearts=5),
        A("Ippon", "A perfect throw reserved for a weakened foe: devastating at low health.", "execute", "minecraft:diamond_sword", vfx="WIND_SLASH", damage=9, bonus=0.9),
    ],
    passives=[
        P("Balance Break", "You may avoid a blow by moving with it.", "dodge", "minecraft:feather", chance=0.1),
        P("Rolling With It", "While fighting you take less damage.", "reduce_when", "minecraft:leather_chestplate", condition="in_combat", pct=0.1),
        P("Use Their Force", "Part of each blow against you is thrown back.", "thorns", "minecraft:iron_nugget", pct=0.15),
        P("Break-Fall Master", "You take much less fall damage.", "fall_guard", "minecraft:white_wool", pct=0.6),
        P("Mat Endurance", "You tire slowly.", "hunger_saver", "minecraft:bread", pct=0.2),
        P("Ippon Instinct", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.6),
        P("Unmovable Foundation", "When nearly dead, you plant your feet and a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
    ],
    finishers=[
        A("Grand Throw of the Century", "Grab everyone around you and send them flying in a single massive throw.", "pull", "minecraft:white_banner", vfx="WIND_RING", radius=10, strength=1.7, damage=20, targets=9, cost=0.06, cooldown=1800),
        A("Immovable Mat", "Become the mat: a thick wall of absorption and resistance for a long while.", "shield", "minecraft:white_wool", vfx="SPIRIT_AURA", absorb_hearts=10, seconds=10, resist_amp=2, cost=0.05, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 5
LINES.append(Line(
    id="goh_hapkido", name="Hapkido Redirect", franchise=F,
    desc="Joint locks, wrist turns and soft redirection that make the attacker break their own balance. A calm, close and tricky style.",
    color="#AB47BC", background="stellar", icon="minecraft:phantom_membrane",
    book_name="The Soft Way Folio", book_lore="A manual of eleven wrist turns and one very long warning.",
    stances=[
        S("Circle Step Stance", "Move in circles around the blow: quick and hard to pin down, a little thin.", "minecraft:leather_boots", speed_pct=0.06, armor=-1, vfx="WIND_RING"),
        S("Yielding Guard Stance", "Give way and let the strike run past: steadier and healing.", "minecraft:white_dye", armor=2, regen=0.5, damage_pct=-0.04, vfx="WIND_RING"),
    ],
    stats=[
        T("Wrist Mastery", "Your wrists turn what others cannot.", "attack_damage", 1, "minecraft:iron_nugget"),
        T("Fluid Stride", "You never stop moving.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Joint Sense", "You feel where a foe is open at once.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Calm Pulse", "Your heart hardly raises under attack.", "max_health", 2, "minecraft:honey_bottle"),
    ],
    arts=[
        A("Joint Strike", "Strike the elbows and knees in a cone ahead: the foes slow down and hit weaker.", "debuff_cone", "minecraft:iron_nugget", vfx="WIND_SLASH", effect="slowness", effect2="weakness", seconds=7, seconds2=7),
        A("Wrist Pull", "Draw the creatures around you in on a locked wrist.", "pull", "minecraft:lead", vfx="WIND_RING", radius=7, strength=1.0, damage=3),
        A("Redirect", "For a while, a share of every blow you take is sent back to its source.", "ward_reflect", "minecraft:phantom_membrane", vfx="MIRROR_PANE", reflect_pct=0.45),
        A("Palm Heel Rise", "A palm into the chin that lifts the foe off the ground.", "uppercut", "minecraft:iron_axe", vfx="WIND_SLASH", damage=7),
        A("Sidestep Entry", "Step off the line of the attack and appear a few blocks away.", "blink", "minecraft:ender_pearl", vfx="WIND_RING", distance=8),
        A("Pressure Point Lock", "Strike the nerves around you: the creatures nearby are stunned for a moment.", "stun_ring", "minecraft:iron_nugget", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Read the Balance", "Spot the off-balance foe: it takes extra damage while marked.", "mark", "minecraft:compass", vfx="MAGIC_CIRCLE", bonus=0.3),
    ],
    passives=[
        P("Soft Guard", "You may redirect a blow entirely and take nothing.", "dodge", "minecraft:feather", chance=0.12),
        P("Joint Retort", "Whoever hits you may be left weakened.", "on_hurt_retaliate", "minecraft:fermented_spider_eye", effect="weakness", chance=0.25),
        P("Wrist Barbs", "A share of each blow against you is returned.", "thorns", "minecraft:iron_nugget", pct=0.15),
        P("Break the Joint", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.6),
        P("Calm Breathing", "Slow breathing mends your wounds over time.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Controlled Fall", "You take less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.4),
        P("Studied Opening", "Your attacks land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
    ],
    finishers=[
        A("Perfect Reversal", "For a long moment, more than half of all damage you take is turned back on whoever dealt it.", "ward_reflect", "minecraft:totem_of_undying", vfx="MIRROR_PANE", seconds=10, reflect_pct=0.65, cost=0.05, cooldown=1700),
        A("Hundred Locks", "Lock every foe around you in a single motion; all are held for a long moment.", "stun_ring", "minecraft:chain", vfx="SEAL_CHAINS", radius=8, seconds=4, targets=10, cost=0.08, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 6
LINES.append(Line(
    id="goh_kendo", name="Kendo Way of the Sword", franchise=F,
    desc="Discipline with a bamboo or steel sword: a straight cut, a loud kiai and a calm after-strike. Few moves, flawlessly timed.",
    color="#B71C1C", background="ruin", icon="minecraft:iron_sword",
    book_name="Shinai Primer", book_lore="Strike where you look, look where you strike, and never look away.",
    stances=[
        S("Chudan Stance", "Sword at the center: balanced, with a sturdy guard and a clean cut.", "minecraft:iron_sword", armor=2, damage_pct=0.05, speed_pct=-0.03, vfx="WIND_SLASH"),
        S("Jodan Stance", "Sword overhead and all-in: crit-heavy, but you leave the body open.", "minecraft:diamond_sword", damage_pct=0.15, crit_chance=0.08, armor=-2, vfx="WIND_SLASH"),
    ],
    stats=[
        T("Sword Arm", "A hundred thousand cuts in the wrist.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Do Armor", "Practiced posture turns blows.", "armor", 1, "minecraft:iron_chestplate"),
        T("Zanshin Awareness", "You feel the room around you.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Quick Cut", "Your cut leaves the sheath fast.", "attack_speed", 0.2, "minecraft:iron_nugget"),
    ],
    arts=[
        A("Men Strike", "A straight overhead cut that lances through a line of foes.", "ray_shot", "minecraft:iron_sword", vfx="WIND_SLASH", damage=8, range=10, pierce=2),
        A("Do Cut", "A flat cut at the flank that sweeps the cone in front of you.", "cone_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=7, half_angle=55),
        A("Kote Rush", "Close the distance in one step and cut everything on the way.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=7, distance=7),
        A("Tsuki", "A precise thrust at a weakened foe: far stronger at low health.", "execute", "minecraft:diamond_sword", vfx="WIND_SLASH", damage=9, bonus=0.9),
        A("Kiai", "A loud shout from the belly that freezes the creatures around you for a moment.", "stun_ring", "minecraft:goat_horn", vfx="WIND_RING", radius=4, seconds=2),
        A("Zanshin Focus", "A calm state after the cut: haste and resistance for a while.", "buff_self", "minecraft:clock", vfx="SPIRIT_AURA", effect="haste", effect2="resistance", seconds=12),
        A("Hundred Cuts", "A rapid series of small cuts at the foe you look at.", "barrage", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=3, shots=6),
    ],
    passives=[
        P("Clean Cut", "Your cuts land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.8),
        P("Finishing Stroke", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:diamond_sword", threshold=0.25, bonus=0.7),
        P("Rhythm of Cuts", "Every few cuts in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Swordsman's Focus", "While you hold a sword, you deal more damage.", "dmg_when", "minecraft:iron_sword", condition="holding_sword", bonus=0.15),
        P("Flow of Strikes", "The longer the fight, the harder you cut.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Evasive Footwork", "You may slip a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        P("Do Resolve", "While fighting you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.08),
    ],
    finishers=[
        A("Heaven-Splitting Men", "A single overhead cut so clean it tears through every foe along a long line.", "ray_shot", "minecraft:netherite_sword", vfx="WIND_SLASH", damage=28, range=26, pierce=4, cost=0.06, cooldown=1600),
        A("Nito Whirl", "Two blades whirl in a ring of cuts that hit everything around you again and again.", "whirlwind", "minecraft:diamond_sword", vfx="WIND_TORNADO", damage=5, hits=8, radius=4.5, cost=0.09, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 7
LINES.append(Line(
    id="goh_jumeok_fist", name="Jumeok Fist", franchise=F,
    desc="Pure fist-fighting: jabs, hooks, hammer fists and a punch heavy enough to send a shock along the floor. A brawler who ends fights in a few blows.",
    color="#E65100", background="magma", icon="minecraft:iron_axe",
    book_name="Knuckle Wraps of the Street", book_lore="No pictures, just bruises in the margins.",
    stances=[
        S("Peekaboo Guard", "Fists up and head tucked: tough and steady, a little slower.", "minecraft:shield", armor=3, knockback_res=0.3, speed_pct=-0.06, vfx="FIRE_BURST"),
        S("Brawler's Charge", "Come in swinging: very strong blows with a thin guard.", "minecraft:blaze_powder", damage_pct=0.18, armor=-2, crit_chance=0.04, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Knuckle Power", "Bare hands that crack stone.", "attack_damage", 1, "minecraft:iron_axe"),
        T("Bruiser's Chest", "A chest you can punch all day.", "max_health", 2, "minecraft:cooked_beef"),
        T("Wrapped Hands", "Taped fists shrug off the strikes.", "armor", 1, "minecraft:leather_chestplate"),
        T("Fast Hands", "Your jabs come out in a blink.", "attack_speed", 0.2, "minecraft:iron_nugget"),
    ],
    arts=[
        A("Jab Storm", "Throw a blur of jabs at the foe you look at.", "barrage", "minecraft:iron_nugget", vfx="FIRE_BURST", damage=3, shots=7, range=6),
        A("Rising Fist", "A rising uppercut that sends the foe in front of you into the air.", "uppercut", "minecraft:iron_axe", vfx="FIRE_BURST", damage=8, launch=0.8),
        A("Punch Wave", "Drive your fist into the air and send the shock along the floor.", "ground_wave", "minecraft:stone", vfx="EARTH_SPIKES", damage=6, length=10),
        A("Knuckle Wrap", "Wrap the fists: you strike harder and recover faster for a while.", "buff_self", "minecraft:string", vfx="FIRE_BURST", effect="strength", effect2="haste", seconds=12),
        A("Ricochet Hook", "Land a hook that bounces on to the next foe.", "chain_hit", "minecraft:iron_axe", vfx="FIRE_BURST", damage=5, jumps=3),
        A("Hammer Fist", "Leap and bring both fists down on the foes below.", "leap_slam", "minecraft:anvil", vfx="FIRE_BURST", damage=8, height=0.8),
        A("Breather Between Rounds", "Roll the shoulders and breathe: heal and regenerate.", "heal_self", "minecraft:cooked_beef", vfx="SPIRIT_AURA", amount=7),
    ],
    passives=[
        P("Rhythm of Fists", "Every few punches in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Haymaker Push", "Your punches may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.3),
        P("Lucky Punch", "Your punches land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.14, mult=1.8),
        P("Heating Up", "The longer the fight, the harder each punch lands.", "momentum", "minecraft:blaze_powder", per_stack=0.04, max_stacks=6),
        P("Taste of Blood", "A share of the damage you deal returns as health.", "lifesteal", "minecraft:redstone", pct=0.05),
        P("Dig In", "When almost dead, a brief shield saves you.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Brawler's Pride", "While your health is full you hit harder.", "dmg_when", "minecraft:iron_axe", condition="full_health", bonus=0.15),
    ],
    finishers=[
        A("Earth-Cracking Punch", "A single full-power punch that tears the ground open in a long wave.", "ground_wave", "minecraft:netherite_axe", vfx="EARTH_SPIKES", damage=28, length=20, width=4, knockback=1.6, cost=0.06, cooldown=1700),
        A("Thousand Fist Flurry", "A storm of fists at the foe in front of you.", "barrage", "minecraft:iron_axe", vfx="FIRE_BURST", damage=5, shots=12, range=7, cost=0.08, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 8
LINES.append(Line(
    id="goh_ki_cultivation", name="Ki Cultivation", franchise=F,
    desc="Breath, focus and the flow of Ki through the body. Close your eyes, gather the glow and let it out as a beam, a ring or a healing breath.",
    color="#00E5FF", background="dream", icon="minecraft:ender_eye",
    book_name="Breath Manual", book_lore="Eleven pages of breathing exercises and one of regret.",
    stances=[
        S("Still Mind Stance", "Settle the Ki: firm, calm and recovering, but you strike lightly.", "minecraft:amethyst_shard", armor=2, regen=0.6, damage_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Flowing Ki Stance", "Let the Ki run through the limbs: faster and stronger, but not guarded.", "minecraft:glowstone_dust", damage_pct=0.12, speed_pct=0.05, armor=-2, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Ki Reserve", "A deep well of inner energy.", "max_health", 2, "minecraft:glow_berries"),
        T("Ki Strike", "Ki sharpens each blow.", "attack_damage", 1, "minecraft:amethyst_shard"),
        T("Clear Pulse", "Calm breathing keeps you light.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
        T("Ki Armor", "Ki hardens the skin.", "armor_toughness", 1, "minecraft:iron_ingot"),
    ],
    arts=[
        A("Ki Surge", "Pull Ki into your muscles: strength and speed for a while.", "buff_self", "minecraft:glowstone_dust", vfx="SPIRIT_AURA", effect="strength", effect2="speed", seconds=14),
        A("Ki Blast", "Push Ki out of the palm as a beam that lances through the first foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=7, range=18, pierce=2),
        A("Ki Circulation", "Run Ki through the wounds: heal and regenerate.", "heal_self", "minecraft:glow_berries", vfx="SPIRIT_AURA", amount=7),
        A("Ki Burst", "Release the Ki around your body in a ring that throws everything back.", "ring_burst", "minecraft:amethyst_shard", vfx="SPIRIT_AURA", damage=5, radius=4),
        A("Ki Guard", "Spread the Ki over your skin as a barrier that absorbs damage.", "shield", "minecraft:iron_ingot", vfx="SPIRIT_AURA", absorb_hearts=4),
        A("Ki Chain", "Throw a Ki bolt that leaps from foe to foe.", "chain_hit", "minecraft:lightning_rod", vfx="LIGHTNING_FIEND", damage=5, jumps=3),
        A("Ki Drop", "Gather Ki overhead and drop it on the spot you look at.", "meteor_drop", "minecraft:heavy_core", vfx="SPIRIT_AURA", damage=11, radius=3.5),
    ],
    passives=[
        P("Inner Mending", "Ki slowly closes your wounds, in battle or out.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Quiet Recovery", "While you crouch and breathe, you heal.", "regen_when", "minecraft:glow_berries", condition="sneaking", per_second=0.8),
        P("Warding Ki", "You take less magic damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.2),
        P("Practiced Breath", "Kills teach you more: increased experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.12),
        P("Frugal Body", "You tire slowly.", "hunger_saver", "minecraft:bread", pct=0.2),
        P("Ki Veil", "While fighting you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.08),
        P("Ki Presence", "Allies near you feel stronger.", "aura", "minecraft:glowstone_dust", effect="strength", target="allies", radius=6),
    ],
    finishers=[
        A("Ki Cannon", "Pour every drop of Ki into one palm and fire a huge beam through the foes ahead.", "ray_shot", "minecraft:beacon", vfx="LIGHTNING_SPEAR", damage=28, range=30, pierce=4, cost=0.06, cooldown=1600),
        A("Ki Meteor", "Call down a vast column of Ki on the place you look at.", "meteor_drop", "minecraft:end_crystal", vfx="SPIRIT_AURA", damage=30, radius=5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 9
LINES.append(Line(
    id="goh_nyuh_weapon_forms", name="Nyuh Weapon Forms", franchise=F,
    desc="Shape your Ki into a weapon and fight with it: spear, blade, bow, chain and shield, each its own form that you switch between in the middle of a fight.",
    color="#26C6DA", background="storm", icon="minecraft:trident",
    book_name="Catalogue of Forms", book_lore="A fold-out chart of eleven weapons, none of which you can actually hold.",
    stances=[
        S("Blade Form Stance", "Keep a Ki blade in hand: sharper and quicker strikes, with a light guard.", "minecraft:iron_sword", damage_pct=0.12, crit_chance=0.05, armor=-1, vfx="WEAPON_CONSTRUCTS"),
        S("Shield Form Stance", "Hold a Ki shield instead: heavy armor, slow regeneration, softer blows.", "minecraft:shield", armor=3, regen=0.4, damage_pct=-0.05, vfx="WEAPON_CONSTRUCTS"),
    ],
    stats=[
        T("Form Mastery", "Your Ki weapons hit harder.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Wielder's Reach", "Your Ki weapons add inches.", "entity_interaction_range", 0.5, "minecraft:stick"),
        T("Ki Lungs", "Maintaining a form takes breath.", "max_health", 2, "minecraft:cooked_cod"),
        T("Weapon Dancer", "You move lightly while armed.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Blade Form Cut", "Shape a Ki blade and cut a wide cone ahead.", "cone_strike", "minecraft:iron_sword", vfx="WEAPON_CONSTRUCTS", damage=7, half_angle=55),
        A("Bow Form Shot", "Shape a Ki bow and fire a piercing arrow along your sight.", "ray_shot", "minecraft:bow", vfx="WEAPON_CONSTRUCTS", damage=7, range=20, pierce=2),
        A("Spear Form Thrust", "Shape a Ki spear and charge in a line, spearing everything.", "dash_strike", "minecraft:trident", vfx="WEAPON_CONSTRUCTS", damage=7, distance=8),
        A("Shield Form Wall", "Shape a Ki shield: a thick shell of absorption.", "shield", "minecraft:shield", vfx="WEAPON_CONSTRUCTS", absorb_hearts=5),
        A("Dagger Form Throw", "Shape a handful of daggers and throw them at the foe you look at.", "barrage", "minecraft:iron_nugget", vfx="WEAPON_CONSTRUCTS", damage=3, shots=6),
        A("Chain Scythe Form", "Whirl a Ki chain scythe around you and hit everything near several times.", "whirlwind", "minecraft:chain", vfx="WEAPON_CONSTRUCTS", damage=3, hits=4),
        A("Hammer Form Smash", "Shape a Ki hammer and bring it down on the foes below.", "leap_slam", "minecraft:mace", vfx="WEAPON_CONSTRUCTS", damage=8, height=0.9),
        A("Form Switch", "Change forms in an instant: you gain haste and strength for a while.", "buff_self", "minecraft:blaze_rod", vfx="WEAPON_CONSTRUCTS", effect="haste", effect2="strength", seconds=12),
    ],
    passives=[
        P("Form Resonance", "While you hold a sword, you deal more damage.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword", bonus=0.15),
        P("Sharp Construct", "Ki weapons strike critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Sustained Form", "The longer you hold a form, the harder it hits.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Weaponized Reflex", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        P("Construct Armor", "Projectiles bounce off your Ki weapons.", "resist", "minecraft:iron_ingot", damage_kind="projectile", pct=0.3),
        P("Spiked Form", "Part of each blow against you is returned.", "thorns", "minecraft:iron_bars", pct=0.15),
        P("Weapon Rhythm", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.5),
        P("Salvaged Ki", "A kill returns some health.", "kill_reward", "minecraft:golden_carrot", heal=3, effect="none"),
    ],
    finishers=[
        A("Armory of Ki", "Raise a whole armory from Ki and send it all at the foe in front of you.", "barrage", "minecraft:netherite_sword", vfx="WEAPON_CONSTRUCTS", damage=5, shots=12, range=18, cost=0.08, cooldown=1800),
        A("Ultimate Form Spear", "A giant spear of Ki that punches through a whole line of foes.", "ray_shot", "minecraft:trident", vfx="WEAPON_CONSTRUCTS", damage=28, range=28, pierce=4, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 10
LINES.append(Line(
    id="goh_monkey_king", name="Monkey King's Charyeok", franchise=F,
    desc="Borrow the Great Sage's trickery: an extending staff, cloud somersaults, hair that turns into clones and an unbeatable grin. Fast, playful and very hard to pin down.",
    color="#FFC107", background="magma", icon="minecraft:blaze_rod",
    book_name="The Great Sage's Stolen Peaches", book_lore="Nobody has ever actually read this one: it keeps changing its own title.",
    stances=[
        S("Cloud-Walker Stance", "Skip across the air on a cloud: very fast, but a bit thin-skinned.", "minecraft:feather", speed_pct=0.1, crit_chance=0.04, armor=-1, vfx="SPIRIT_AURA"),
        S("Golden Hoop Stance", "Settle the staff on your shoulder: tougher and steadier, at a slower pace.", "minecraft:blaze_rod", armor=3, regen=0.3, speed_pct=-0.05, vfx="FIRE_SPIRAL"),
    ],
    stats=[
        T("Monkey Agility", "A body that bounces off ceilings.", "movement_speed", 0.05, "minecraft:rabbit_foot", op="mult"),
        T("Stolen Peach Vigor", "A bite of heaven's peach keeps you hardy.", "max_health", 2, "minecraft:golden_apple"),
        T("Cudgel Arm", "A staff that weighs a ton is light in your hand.", "attack_damage", 1, "minecraft:blaze_rod"),
        T("Cloud Leap", "Your jump shames the nearest cloud.", "jump_strength", 0.08, "minecraft:slime_ball", op="mult"),
    ],
    arts=[
        A("Cloud Somersault", "Flip onto a golden cloud and land a good distance ahead.", "blink", "minecraft:cobweb", vfx="SPIRIT_AURA", distance=11),
        A("Staff Extend", "Grow the staff to a great length and stab it through a line of foes.", "ray_shot", "minecraft:blaze_rod", vfx="FIRE_SPIRAL", damage=8, range=14, pierce=3),
        A("Hair Clones", "Pluck your hairs and blow: a cloud of tiny monkeys darts at the foe in front of you.", "barrage", "minecraft:string", vfx="SPIRIT_AURA", damage=3, shots=7),
        A("Golden Cudgel Smash", "Leap and bring the heavy cudgel down on the foes below.", "leap_slam", "minecraft:blaze_rod", vfx="FIRE_SPIRAL", damage=8, height=1.0),
        A("Staff Whirl", "Spin the cudgel like a propeller and hit everything around you.", "whirlwind", "minecraft:stick", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Seventy-Two Changes", "Change shape, shimmer and slip away: speed and invisibility for a while.", "buff_self", "minecraft:ender_eye", vfx="SPIRIT_AURA", effect="speed", effect2="invisibility", seconds=12),
        A("Peach Feast", "Bite a stolen peach: heal and regenerate.", "heal_self", "minecraft:golden_apple", vfx="SPIRIT_AURA", amount=7),
        A("Monkey Business", "Hop from foe to foe with a trick blow that bounces on.", "chain_hit", "minecraft:gold_nugget", vfx="WIND_SLASH", damage=5, jumps=4),
    ],
    passives=[
        P("Trickster's Rhythm", "The longer you fight, the more every blow gains.", "momentum", "minecraft:gold_nugget", per_stack=0.04, max_stacks=6),
        P("Monkey Dodge", "You may slip a blow completely.", "dodge", "minecraft:feather", chance=0.14),
        P("Heaven's Cheek", "Your strikes land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Staff Technique", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Peach Tree Mending", "Your wounds slowly close, even out of combat.", "mending", "minecraft:golden_apple", seconds=16, amount=1),
        P("Ledge Hopper", "You take much less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.5),
        P("Sprinting Sage", "While sprinting you move even faster.", "self_effect_when", "minecraft:sugar", effect="speed", condition="sprinting"),
    ],
    finishers=[
        A("Great Sage Equalling Heaven", "Raise the staff to the heavens and let the Sage's might fall in a huge ring.", "ring_burst", "minecraft:nether_star", vfx="FIRE_SPIRAL", damage=24, radius=7, targets=9, knockback=1.8, cost=0.06, cooldown=1800),
        A("Sky-Piercing Staff", "Extend the staff through the sky: a giant beam through every foe ahead.", "ray_shot", "minecraft:end_rod", vfx="FIRE_SPIRAL", damage=30, range=30, pierce=5, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 11
LINES.append(Line(
    id="goh_poseidon", name="Poseidon's Charyeok", franchise=F,
    desc="Borrow the sea god's command of water: tidal waves, whirlpools that drag foes in and a trident of surging blue light. Tough and forceful.",
    color="#1565C0", background="ocean", icon="minecraft:trident",
    book_name="Tide-Book of the Deep", book_lore="The pages are always damp, whatever you do.",
    stances=[
        S("Deep Current Stance", "Be the current: heavy armor and regeneration, but movement slows.", "minecraft:prismarine_shard", armor=3, regen=0.4, speed_pct=-0.06, vfx="WATER_RING"),
        S("Surging Tide Stance", "Be the tide: fast and strong, but nothing guards you.", "minecraft:heart_of_the_sea", damage_pct=0.15, speed_pct=0.05, armor=-2, vfx="WATER_BURST"),
    ],
    stats=[
        T("Sea God's Constitution", "Salt and storm toughen the body.", "max_health", 2, "minecraft:heart_of_the_sea"),
        T("Trident Arm", "A trident feels light in your hand.", "attack_damage", 1, "minecraft:trident"),
        T("Wave Grip", "The sea cannot push you.", "knockback_resistance", 0.1, "minecraft:prismarine_bricks"),
        T("Long Stride of the Tide", "You move like a tide.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Whirlpool", "A whirlpool forms around you and drags nearby foes into its center.", "pull", "minecraft:nautilus_shell", vfx="WATER_RING", radius=9, strength=1.1, damage=4),
        A("Tidal Wave", "Send a rolling wall of water along the floor ahead.", "ground_wave", "minecraft:prismarine_crystals", vfx="WATER_BURST", damage=7, length=12, width=3),
        A("Sea Surge", "A ring of surging water bursts out of you and throws the foes back.", "ring_burst", "minecraft:prismarine_shard", vfx="WATER_RING", damage=6, radius=4),
        A("Trident Thrust", "Hurl a trident of blue light along your sight; it pierces the foes ahead.", "ray_shot", "minecraft:trident", vfx="WATER_BURST", damage=8, range=18, pierce=3),
        A("Wall of Water", "A curtain of sea water rises around you and absorbs damage.", "shield", "minecraft:water_bucket", vfx="WATER_RING", absorb_hearts=5),
        A("Rain of Mercy", "A soft rain falls: you and the allies around you are healed.", "heal_pulse", "minecraft:glass_bottle", vfx="WATER_RING", radius=7, amount=6),
        A("Leviathan's Fall", "Call down a vast wave on the spot you look at.", "meteor_drop", "minecraft:conduit", vfx="WATER_BURST", damage=11, radius=4),
    ],
    passives=[
        P("Sea Legs", "You deal more damage while in the water.", "dmg_when", "minecraft:trident", condition="in_water", bonus=0.2),
        P("Tidal Recovery", "Water heals you steadily.", "regen_when", "minecraft:kelp", condition="in_water", per_second=0.8),
        P("Storm Hide", "You take less lightning damage.", "resist", "minecraft:lightning_rod", damage_kind="lightning", pct=0.3),
        P("Deep Aura", "Enemies near you are slowed by the pressure of the sea.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
        P("Salt Spray", "Your blows may leave the foe nauseous.", "on_hit_effect", "minecraft:fermented_spider_eye", effect="nausea", chance=0.2),
        P("Riptide Shove", "Your blows may drive the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.3),
        P("Breaker Fall", "The sea softens your falls.", "fall_guard", "minecraft:water_bucket", pct=0.5),
    ],
    finishers=[
        A("Sea-Splitting Torrent", "Split the room with a monster wave that sweeps the whole lane ahead.", "ground_wave", "minecraft:heart_of_the_sea", vfx="WATER_BURST", damage=28, length=22, width=5, knockback=1.8, cost=0.06, cooldown=1700),
        A("Abyssal Maelstrom", "A colossal whirlpool opens and drags everything around you to its center.", "pull", "minecraft:conduit", vfx="WATER_RING", radius=12, strength=1.9, damage=20, targets=10, cost=0.06, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 12
LINES.append(Line(
    id="goh_mighty_style", name="Mighty Style", franchise=F,
    desc="Fight with sheer, unbelievable strength: boulders for fists, a shout that rattles the arena and a stomp that opens the floor. No tricks; just a bigger hammer.",
    color="#6D4C41", background="magma", icon="minecraft:mace",
    book_name="Mighty Weights Chart", book_lore="Everything in it is measured in how far it flew.",
    stances=[
        S("Boulder Stance", "Stand like a boulder: huge armor and immunity to pushes, but slow.", "minecraft:stone", armor=5, knockback_res=0.6, speed_pct=-0.12, vfx="EARTH_SPIKES"),
        S("Rampaging Stance", "Run at things and smash them: huge strength, nothing protecting you.", "minecraft:mace", damage_pct=0.2, speed_pct=0.03, armor=-3, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Titan Marrow", "Bones like steel beams.", "max_health", 2, "minecraft:iron_block"),
        T("Mighty Arm", "Arms like tree trunks.", "attack_damage", 1, "minecraft:mace"),
        T("Rock Skin", "Skin like old stone.", "armor", 1, "minecraft:stone"),
        T("Planted Feet", "Almost impossible to push.", "knockback_resistance", 0.15, "minecraft:anvil"),
    ],
    arts=[
        A("Cannon Fist", "A heavy punch that launches the foe in front of you into the sky.", "uppercut", "minecraft:mace", vfx="EARTH_SPIKES", damage=9, launch=1.0),
        A("Mighty Stomp", "Leap and come down so hard that the floor shakes.", "leap_slam", "minecraft:anvil", vfx="EARTH_SPIKES", damage=9, height=0.9, knockback=1.0),
        A("Mighty Roar", "A deep shout draws every hostile near you to your fists.", "taunt", "minecraft:goat_horn", vfx="DEVIL_CIRCLE", radius=9, targets=5),
        A("Hammer Spin", "Swing the arms like hammers and hit everything around you several times.", "whirlwind", "minecraft:iron_axe", vfx="WIND_TORNADO", damage=4, hits=4),
        A("Fault Line", "Break the floor in a long crack that tears along the ground ahead.", "ground_wave", "minecraft:cobblestone", vfx="EARTH_SPIKES", damage=7, length=10, width=2.5),
        A("Stone Body", "Harden your body: resistance and strength for a while.", "buff_self", "minecraft:stone_bricks", vfx="EARTH_SPIKES", effect="resistance", effect2="strength", seconds=12),
        A("Quake Clap", "Clap so hard that the creatures around you are stunned for a moment.", "stun_ring", "minecraft:bell", vfx="STONE_SPIKES", radius=4.5, seconds=2),
    ],
    passives=[
        P("Colossus Shove", "Your blows hurl the foe a long way.", "knockback_on_hit", "minecraft:piston", strength=0.7, chance=0.35),
        P("Stone Hide", "While fighting you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.12),
        P("Giant Slayer", "You hit bosses harder.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.2),
        P("Boulder Retort", "Whoever hits you takes part of the blow back.", "thorns", "minecraft:cobblestone", pct=0.18),
        P("Not Going Down", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Gorging Appetite", "A share of each blow you deal returns as health.", "lifesteal", "minecraft:cooked_beef", pct=0.05),
        P("Tireless", "You tire slowly.", "hunger_saver", "minecraft:bread", pct=0.25),
    ],
    finishers=[
        A("Mighty Quake", "Smash the arena floor: everything around you is stunned for a long time.", "stun_ring", "minecraft:anvil", vfx="STONE_SPIKES", radius=8, seconds=4, targets=10, cost=0.08, cooldown=1900),
        A("Fist of the Mighty", "One huge two-handed smash that rips the floor open for a long lane ahead.", "ground_wave", "minecraft:mace", vfx="EARTH_SPIKES", damage=30, length=20, width=4, knockback=1.8, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 13
LINES.append(Line(
    id="goh_tournament_contender", name="Tournament Contender", franchise=F,
    desc="You fight in the bracket: warm-up, read your opponent, counter the opening and finish in front of the crowd. Every round teaches you more.",
    color="#FF5722", background="city", icon="minecraft:gold_ingot",
    book_name="Bracket Program Notes", book_lore="Each page is a round. The last page is blank: that is the final.",
    stances=[
        S("Round One Stance", "Play it safe: steady and tough, slightly weaker blows.", "minecraft:iron_chestplate", armor=2, regen=0.3, damage_pct=-0.03, vfx="SPIRIT_AURA"),
        S("Final Round Stance", "Go all-in: far stronger and faster, wide open.", "minecraft:gold_ingot", damage_pct=0.18, speed_pct=0.04, armor=-2, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Ring Conditioning", "Months of training for this.", "max_health", 2, "minecraft:cooked_beef"),
        T("Crowd Roar", "The crowd lifts every blow.", "attack_damage", 1, "minecraft:gold_ingot"),
        T("Ring Craft", "You know the ring.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Podium Luck", "Brackets bend your way.", "luck", 1, "minecraft:emerald"),
    ],
    arts=[
        A("Warm-Up", "A fast warm-up before the bell: you gain strength and haste.", "buff_self", "minecraft:clock", vfx="SPIRIT_AURA", effect="strength", effect2="haste", seconds=14),
        A("Read the Opponent", "Study a foe: it takes extra damage while marked.", "mark", "minecraft:spyglass", vfx="MAGIC_CIRCLE", bonus=0.3),
        A("Opening Rush", "Burst in right after the bell and strike everything on the way.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=7, distance=8),
        A("Counter Round", "For a few seconds, part of every blow you take is thrown back at the attacker.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Corner Break", "Use the break between rounds: heal and regenerate.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=7),
        A("Knockout Blow", "The blow that ends the match: far stronger against a weakened foe.", "execute", "minecraft:diamond_sword", vfx="FIRE_BURST", damage=9, bonus=0.9),
        A("Crowd Pleaser", "A showy spin that hits everything around you several times.", "whirlwind", "minecraft:firework_star", vfx="WIND_TORNADO", damage=3, hits=4),
    ],
    passives=[
        P("Prize Money", "Whatever you beat may drop an extra stack of loot.", "drop_bonus", "minecraft:gold_nugget", chance=0.1),
        P("Match Experience", "Every kill gives more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
        P("Round Rhythm", "Every few hits in a row land far harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Victory Lap", "A kill returns health and a short burst of speed.", "kill_reward", "minecraft:golden_apple", heal=3, effect="speed", seconds=4),
        P("Final Round Grit", "When nearly dead, a brief shield saves you.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Spotlight Strike", "Your hits land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Bracket Momentum", "The longer the fight, the stronger every blow.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
    ],
    finishers=[
        A("Championship Fury", "Everything for the title: a ring of force that throws every foe around you.", "ring_burst", "minecraft:gold_block", vfx="FIRE_BURST", damage=22, radius=6.5, targets=9, knockback=1.7, cost=0.06, cooldown=1800),
        A("Title Match Knockout", "One ruinous blow at a single weakened foe.", "execute", "minecraft:netherite_sword", vfx="FIRE_BURST", damage=28, bonus=2.2, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 14
LINES.append(Line(
    id="goh_zeus_charyeok", name="Zeus's Charyeok", franchise=F,
    desc="Borrow the king of the gods' thunder: lightning steps, forks of lightning and a clap that stuns a hall. Fast, loud and bright.",
    color="#FFEB3B", background="storm", icon="minecraft:lightning_rod",
    book_name="Thunder Ledger of the Sky King", book_lore="It sparks if you touch the cover with wet hands.",
    stances=[
        S("Thunder Step Stance", "Crackle with static: very fast and sharp, with a thin guard.", "minecraft:lightning_rod", speed_pct=0.1, crit_chance=0.06, armor=-1, vfx="LIGHTNING_FIEND"),
        S("Storm Crown Stance", "Gather the storm over your head: tougher and stronger, but slower.", "minecraft:gold_ingot", armor=2, damage_pct=0.1, speed_pct=-0.05, vfx="LIGHTNING_FIEND"),
    ],
    stats=[
        T("Storm Heart", "Thunder in your chest.", "max_health", 2, "minecraft:glowstone"),
        T("Bolt Strike", "Lightning in the knuckles.", "attack_damage", 1, "minecraft:lightning_rod"),
        T("Static Stride", "Charge in the soles.", "movement_speed", 0.05, "minecraft:leather_boots", op="mult"),
        T("Sky Reach", "Thunder travels far.", "entity_interaction_range", 0.4, "minecraft:end_rod"),
    ],
    arts=[
        A("Lightning Step", "Turn into a bolt and dash along the line ahead, shocking everything.", "dash_strike", "minecraft:lightning_rod", vfx="LIGHTNING_FIEND", damage=7, distance=9),
        A("Chain Lightning", "A bolt jumps from foe to foe and lights them all up.", "chain_hit", "minecraft:copper_ingot", vfx="LIGHTNING_FIEND", damage=6, jumps=4),
        A("Fork Lightning", "A forked bolt tears through the cone in front of you.", "cone_strike", "minecraft:lightning_rod", vfx="LIGHTNING_SPEAR", damage=7, half_angle=55),
        A("Thunderbolt", "Throw a thunderbolt along your sight, piercing the foes in the line.", "ray_shot", "minecraft:trident", vfx="LIGHTNING_SPEAR", damage=8, range=20, pierce=3),
        A("Static Paralysis", "Shock the creatures around you into stillness for a moment.", "stun_ring", "minecraft:redstone", vfx="LIGHTNING_FIEND", radius=4, seconds=2),
        A("Storm's Favor", "The storm blesses you: speed and strength for a while.", "buff_self", "minecraft:glowstone_dust", vfx="LIGHTNING_FIEND", effect="speed", effect2="strength", seconds=14),
        A("Thunder Clap", "Clap your hands and send a ring of thunder around you.", "ring_burst", "minecraft:bell", vfx="LIGHTNING_FIEND", damage=6, radius=4),
        A("Zeus Strike", "Call a lightning bolt down onto the spot you look at.", "meteor_drop", "minecraft:lightning_rod", vfx="LIGHTNING_SPEAR", damage=12, radius=3.5),
    ],
    passives=[
        P("Lightning Skin", "You take much less lightning damage.", "resist", "minecraft:lightning_rod", damage_kind="lightning", pct=0.4),
        P("Rain Surge", "In the rain your blows hit harder.", "dmg_when", "minecraft:water_bucket", condition="raining", bonus=0.2),
        P("Thunder Strike", "Your hits land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.8),
        P("Mounting Charge", "The longer the fight, the more charge your hits carry.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Numbing Shock", "Your blows may leave the foe slowed.", "on_hit_effect", "minecraft:cobweb", effect="slowness", chance=0.22),
        P("Storm Sprint", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.2),
        P("Crackling Air", "Enemies near you are slowed by the static.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
    ],
    finishers=[
        A("Heaven's Hammer", "Call the king's thunder on the spot you look at, a gigantic pillar of lightning.", "meteor_drop", "minecraft:beacon", vfx="LIGHTNING_SPEAR", damage=30, radius=5.5, cost=0.09, cooldown=1900),
        A("Storm Sovereign's Roar", "A ring of thunder bursts from your body and hurls every foe around you.", "ring_burst", "minecraft:end_crystal", vfx="LIGHTNING_FIEND", damage=24, radius=7, targets=9, knockback=1.8, cost=0.06, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 15
LINES.append(Line(
    id="goh_hades_charyeok", name="Hades's Charyeok", franchise=F,
    desc="Borrow the lord of the dead's grasp: shadow that pulls, drains and withers. The weaker the foe, the quicker it falls to your hand.",
    color="#5E35B1", background="void", icon="minecraft:wither_skeleton_skull",
    book_name="Ledger of the Quiet Halls", book_lore="Every name in it is crossed off, eventually.",
    stances=[
        S("Underworld Cloak Stance", "Wrap yourself in the dark: tougher and steady, but your blows are lighter.", "minecraft:black_dye", armor=2, regen=0.4, damage_pct=-0.04, vfx="SHADOW_POOL"),
        S("Reaper's Grip Stance", "Reach for the dying: much stronger, crit-prone, no guard.", "minecraft:wither_rose", damage_pct=0.15, crit_chance=0.06, armor=-2, vfx="SHADOW_POOL"),
    ],
    stats=[
        T("Dead King's Vigor", "Death has no hold on you yet.", "max_health", 2, "minecraft:wither_skeleton_skull"),
        T("Grave Grip", "A grasp that does not let go.", "attack_damage", 1, "minecraft:wither_rose"),
        T("Shadow Skin", "Shadow sits on the skin and turns blows.", "armor", 1, "minecraft:netherite_chestplate"),
        T("Silent Tread", "Shadows do not make noise.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Soul Drain", "Bite into the foe in front of you and drink in its life.", "drain_bite", "minecraft:spider_eye", vfx="SHADOW_POOL", damage=7, heal_pct=0.4),
        A("Grasp of Tartarus", "Hands of shadow drag the creatures around you toward you.", "pull", "minecraft:chain", vfx="SHADOW_POOL", radius=8, strength=1.0, damage=4),
        A("Pall of Death", "A cloud of death rolls over the cone ahead: the foes in it wither and go dark.", "debuff_cone", "minecraft:wither_rose", vfx="SHADOW_POOL", effect="wither", effect2="darkness", seconds=6, seconds2=5),
        A("Name in the Ledger", "Write a foe's name in the ledger: it takes extra damage until the mark fades.", "mark", "minecraft:writable_book", vfx="DEVIL_CIRCLE", bonus=0.3),
        A("Underworld Rift", "The ground tears open and a wave of shadow runs along it.", "ground_wave", "minecraft:obsidian", vfx="SHADOW_POOL", damage=6, length=11),
        A("Veil of the Dead", "A shroud of shadow: the creatures near you cannot see.", "smoke", "minecraft:black_dye", vfx="SHADOW_POOL", radius=5, seconds=6),
        A("Reaper's Toll", "The bell tolls once: a heavy blow on a weakened foe, far stronger at low health.", "execute", "minecraft:netherite_axe", vfx="DEVIL_CIRCLE", damage=9, bonus=0.9),
    ],
    passives=[
        P("Hungry Shadow", "A share of every blow you deal returns as health.", "lifesteal", "minecraft:redstone", pct=0.06),
        P("Harvest of Souls", "Kills restore a little health and grant a short burst of strength.", "kill_reward", "minecraft:soul_lantern", heal=3, effect="strength", seconds=4),
        P("Withering Touch", "Your hits may wither the foe.", "on_hit_effect", "minecraft:wither_rose", effect="wither", chance=0.2),
        P("Deathless Skin", "You take less wither damage.", "resist", "minecraft:wither_skeleton_skull", damage_kind="wither", pct=0.4),
        P("Last Rites", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:netherite_axe", threshold=0.25, bonus=0.7),
        P("Elude Fate", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        P("Night Mending", "At night your wounds close quickly.", "regen_when", "minecraft:black_dye", condition="night", per_second=0.6),
    ],
    finishers=[
        A("Gate of the Dead", "Open the gates of the underworld around you: a wide ring of shadow and force.", "ring_burst", "minecraft:crying_obsidian", vfx="DEVIL_CIRCLE", damage=22, radius=6.5, targets=9, knockback=1.6, cost=0.06, cooldown=1800),
        A("Judgment of Hades", "Drop an executioner's sentence on a single foe: ruinous against the wounded.", "execute", "minecraft:wither_skeleton_skull", vfx="DEVIL_CIRCLE", damage=28, bonus=2.3, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 16
LINES.append(Line(
    id="goh_ares_charyeok", name="Ares's Charyeok", franchise=F,
    desc="Borrow the war god's rage: a roar that rallies the front line, charges that crack shields and a fury that grows with every fallen enemy.",
    color="#C62828", background="blood", icon="minecraft:netherite_axe",
    book_name="Field Manual of the Red Banner", book_lore="Page eleven is stained. Page twelve is missing.",
    stances=[
        S("Shield Wall Stance", "Hold the line: heavy armor and firm footing, less offense.", "minecraft:shield", armor=3, knockback_res=0.3, damage_pct=-0.04, vfx="FIRE_BURST"),
        S("Bloodlust Stance", "Charge in with a roar: very strong blows, almost no guard.", "minecraft:netherite_axe", damage_pct=0.2, speed_pct=0.03, armor=-3, vfx="FLAME_EXPLOSION"),
    ],
    stats=[
        T("War Hardened", "Years of battle.", "max_health", 2, "minecraft:cooked_beef"),
        T("Warhammer Arm", "Arms made for axes.", "attack_damage", 1, "minecraft:netherite_axe"),
        T("Bronze Plating", "Battered but solid.", "armor", 1, "minecraft:iron_chestplate"),
        T("Charge Footing", "You hit the ground running.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Battle Challenge", "A war cry that turns the nearby hostile creatures on you.", "taunt", "minecraft:goat_horn", vfx="DEVIL_CIRCLE", radius=9, targets=5),
        A("War Cry", "You and the allies around you grow stronger for a while.", "buff_aura", "minecraft:red_banner", vfx="FIRE_BURST", effect="strength", radius=9, seconds=12),
        A("Phalanx Charge", "Charge forward and slam everything in your path.", "dash_strike", "minecraft:iron_axe", vfx="FIRE_BURST", damage=8, distance=8),
        A("Axe Sweep", "Cleave the whole cone in front of you.", "cone_strike", "minecraft:netherite_axe", vfx="FIRE_BURST", damage=8, half_angle=55),
        A("Rampage", "Whirl with the axe and hit everything around you several times.", "whirlwind", "minecraft:iron_axe", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Battlefield Fire", "Call a column of war-fire on the place you look at.", "meteor_drop", "minecraft:fire_charge", vfx="FLAME_EXPLOSION", damage=11, radius=3.5),
        A("Field Surgeon", "Bind your wounds mid-battle: heal and regenerate.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=7),
    ],
    passives=[
        P("Battle Rage", "While fighting you deal more damage.", "dmg_when", "minecraft:netherite_axe", condition="in_combat", bonus=0.12),
        P("Rising Fury", "The longer the battle, the more each strike carries.", "momentum", "minecraft:blaze_powder", per_stack=0.04, max_stacks=6),
        P("Spoils of War", "Kills restore health.", "kill_reward", "minecraft:cooked_beef", heal=3, effect="none"),
        P("Blood Rite", "A share of each blow you deal returns as health.", "lifesteal", "minecraft:redstone", pct=0.05),
        P("Axe Rhythm", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Brutal Blow", "Your strikes land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.8),
        P("Fireproof Hide", "You take less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire", pct=0.25),
    ],
    finishers=[
        A("Warlord's Command", "Roar a command that fills your allies with fury: a long, strong buff.", "buff_aura", "minecraft:red_banner", vfx="FLAME_EXPLOSION", effect="strength", amp=1, radius=12, seconds=22, cost=0.08, cooldown=1900),
        A("Wrath of the Red Banner", "Call a field of fire down on the place you look at.", "meteor_drop", "minecraft:fire_charge", vfx="FLAME_EXPLOSION", damage=30, radius=5.5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 17
LINES.append(Line(
    id="goh_athena_charyeok", name="Athena's Charyeok", franchise=F,
    desc="Borrow the goddess of wisdom's aegis: a shield for yourself and your allies, a mark on the weak point and a spear thrown with perfect aim. A fighter who plans two moves ahead.",
    color="#B0BEC5", background="holy", icon="minecraft:shield",
    book_name="Owl-Scribe's Strategy Codex", book_lore="Every plan has a second plan underlined twice.",
    stances=[
        S("Aegis Bearer Stance", "Hold the aegis forward: a lot of armor and stability, at a slower pace.", "minecraft:shield", armor=4, knockback_res=0.4, speed_pct=-0.08, vfx="MIRROR_PANE"),
        S("Strategist's Stance", "Think and strike: sharper, with better crits, and a little lighter on the feet.", "minecraft:writable_book", damage_pct=0.1, crit_chance=0.08, speed_pct=0.03, armor=-1, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Owl's Composure", "A calm mind keeps the heart going.", "max_health", 2, "minecraft:ender_eye"),
        T("Bronze Aegis", "A bronze-bright skin.", "armor", 1, "minecraft:golden_chestplate"),
        T("Wise Hand", "Spear thrown with a clear head.", "attack_damage", 1, "minecraft:trident"),
        T("Vision of the Owl", "Your reach is long.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
    ],
    arts=[
        A("Aegis Wall", "Raise a bronze-bright shell that absorbs damage.", "shield", "minecraft:shield", vfx="MIRROR_PANE", absorb_hearts=5, resist_amp=1),
        A("Weak Point Sight", "See where the foe will fail: it takes extra damage while marked.", "mark", "minecraft:spyglass", vfx="MAGIC_CIRCLE", bonus=0.3, range=26),
        A("Spear of Wisdom", "Throw a spear of white light with perfect aim: it pierces the foes in a line.", "ray_shot", "minecraft:trident", vfx="LIGHTNING_SPEAR", damage=8, range=20, pierce=3),
        A("Phalanx Formation", "You and the allies near you stand firm: resistance for a while.", "buff_aura", "minecraft:iron_bars", vfx="MIRROR_PANE", effect="resistance", radius=9, seconds=12),
        A("Mirror of Reason", "For a few seconds a share of every blow is thrown back at the attacker.", "ward_reflect", "minecraft:glass_pane", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Owl's Blessing", "A feather of light falls: you and your allies are healed.", "heal_pulse", "minecraft:feather", vfx="MAGIC_CIRCLE", radius=7, amount=6),
        A("Gorgon Gaze", "Your stare freezes the creatures near you in place.", "stun_ring", "minecraft:ender_eye", vfx="MAGIC_CIRCLE", radius=4, seconds=2),
    ],
    passives=[
        P("Strategist's Calm", "While fighting you take less damage.", "reduce_when", "minecraft:golden_chestplate", condition="in_combat", pct=0.1),
        P("Learned from Every Battle", "Every kill gives more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
        P("Shot-Deflecting Shield", "Arrows and missiles bounce off your aegis.", "resist", "minecraft:iron_ingot", damage_kind="projectile", pct=0.3),
        P("Bronze Barbs", "Part of every blow against you is returned.", "thorns", "minecraft:iron_nugget", pct=0.15),
        P("Anticipation", "You may step out of the way of a blow.", "dodge", "minecraft:feather", chance=0.1),
        P("Monster Study", "You deal more damage to monsters.", "dmg_vs", "minecraft:writable_book", category="monster", bonus=0.15),
        P("Hold the Standard", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
    ],
    finishers=[
        A("Aegis of the City", "A giant aegis for a long moment: huge absorption and resistance.", "shield", "minecraft:golden_chestplate", vfx="MIRROR_PANE", absorb_hearts=10, seconds=10, resist_amp=2, cost=0.05, cooldown=1600),
        A("Owl's Judgement", "A glowing spear thrown through the whole line of foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=28, range=30, pierce=5, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 18
LINES.append(Line(
    id="goh_hermes_charyeok", name="Hermes's Charyeok", franchise=F,
    desc="Borrow the messenger god's winged sandals: bursts of impossible speed, darts in a flurry and a blur of afterimages. You win by never being where the blow lands.",
    color="#80DEEA", background="sand", icon="minecraft:feather",
    book_name="Winged Courier's Waybill", book_lore="Delivered before it was sent.",
    stances=[
        S("Winged Sandals Stance", "Run lighter than air: huge speed, fragile guard.", "minecraft:feather", speed_pct=0.14, armor=-2, damage_pct=-0.03, vfx="WIND_RING"),
        S("Herald's Stand Stance", "Plant your feet: steadier, with armor and regeneration.", "minecraft:leather_chestplate", armor=2, regen=0.4, speed_pct=0.02, vfx="WIND_RING"),
    ],
    stats=[
        T("Messenger's Pace", "The fastest legs in the arena.", "movement_speed", 0.06, "minecraft:leather_boots", op="mult"),
        T("Quick Strike", "Your blows come in a blink.", "attack_speed", 0.25, "minecraft:feather"),
        T("Swift Heart", "A quick pulse, a long life.", "max_health", 2, "minecraft:golden_carrot"),
        T("Sky Leap", "Your spring launches you over fences.", "jump_strength", 0.06, "minecraft:rabbit_foot", op="mult"),
    ],
    arts=[
        A("Blur Dash", "Vanish into a streak and cut through the foes in a line.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH", damage=7, distance=9),
        A("Winged Skip", "Flick forward in an instant to a spot a few blocks ahead.", "blink", "minecraft:feather", vfx="WIND_RING", distance=10),
        A("Afterimage Barrage", "Your afterimages strike the foe in front of you from every side.", "barrage", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=3, shots=7),
        A("Hermes' Boon", "The god's speed fills you: a long run of speed and jump height.", "buff_self", "minecraft:sugar", vfx="WIND_RING", effect="speed", amp=1, effect2="jump_boost", seconds=14),
        A("Trickster's Smoke", "Throw a puff of smoke: the creatures near you lose sight of you.", "smoke", "minecraft:gunpowder", vfx="ASH_FORMATION", radius=5, seconds=6),
        A("Relay Strike", "A strike that bounces from one foe to the next like a baton.", "chain_hit", "minecraft:iron_sword", vfx="WIND_SLASH", damage=5, jumps=4),
        A("Heel Flick", "Flick up with the heel and send the foe in front of you skyward.", "uppercut", "minecraft:leather_boots", vfx="WIND_SLASH", damage=7),
    ],
    passives=[
        P("Sprint Fury", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.25),
        P("Blur Evade", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.15),
        P("Speed Builds", "The longer the fight goes, the more your blows add up.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Courier's Rush", "While sprinting you move even faster.", "self_effect_when", "minecraft:sugar", effect="speed", condition="sprinting"),
        P("Sandal Cushion", "You take much less fall damage.", "fall_guard", "minecraft:feather", pct=0.5),
        P("Light Meals", "You tire slowly.", "hunger_saver", "minecraft:bread", pct=0.2),
        P("Wingtip Reflex", "Your hits land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
    ],
    finishers=[
        A("Speed of the Messenger", "A thousand afterimages strike at once: a giant volley at the foe in front of you.", "barrage", "minecraft:diamond_boots", vfx="WIND_SLASH", damage=5, shots=12, range=14, cost=0.08, cooldown=1800),
        A("Lightning Courier", "Streak through the whole line of foes ahead in a single blink.", "dash_strike", "minecraft:elytra", vfx="WIND_SLASH", damage=24, distance=13, targets=9, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 19
LINES.append(Line(
    id="goh_hephaestus_charyeok", name="Hephaestus's Charyeok", franchise=F,
    desc="Borrow the smith god's forge: weapons hammered out of nothing, an anvil dropped from above and hot metal chains. You fight from a burning workshop.",
    color="#FF6F00", background="magma", icon="minecraft:anvil",
    book_name="The Divine Smith's Apprentice Book", book_lore="Burned at the edges, nine times corrected in the margins.",
    stances=[
        S("Tempered Plate Stance", "Wear the forge: heavy armor, slow movement.", "minecraft:iron_chestplate", armor=4, knockback_res=0.3, speed_pct=-0.08, vfx="FLAME_EXPLOSION"),
        S("Hammer in Hand Stance", "Swing without thinking: much stronger, a thin guard.", "minecraft:mace", damage_pct=0.15, armor=-2, vfx="FLAME_EXPLOSION"),
    ],
    stats=[
        T("Forge Heat", "A hot heart.", "max_health", 2, "minecraft:magma_cream"),
        T("Hammer Arm", "Arms that swing an anvil.", "attack_damage", 1, "minecraft:mace"),
        T("Slag Coat", "Metal scale on the skin.", "armor", 1, "minecraft:iron_ingot"),
        T("Heavy Stance", "You cannot be shoved off the anvil.", "knockback_resistance", 0.1, "minecraft:anvil"),
    ],
    arts=[
        A("Divine Arsenal", "Hammer out a rack of weapons and throw them at the foe you look at.", "barrage", "minecraft:iron_sword", vfx="WEAPON_CONSTRUCTS", damage=3, shots=7),
        A("Molten Spray", "Spray molten metal across the cone in front of you.", "cone_strike", "minecraft:lava_bucket", vfx="FLAME_EXPLOSION", damage=7, half_angle=55),
        A("Forge Barrier", "Wrap yourself in a plate of cooling steel that absorbs damage.", "shield", "minecraft:iron_chestplate", vfx="WEAPON_CONSTRUCTS", absorb_hearts=5),
        A("Anvil Drop", "Drop an anvil on the spot you look at after a moment.", "meteor_drop", "minecraft:anvil", vfx="WEAPON_CONSTRUCTS", damage=11, radius=3.5),
        A("Tempering", "Temper your body: you gain resistance and strength for a while.", "buff_self", "minecraft:blast_furnace", vfx="FLAME_EXPLOSION", effect="resistance", effect2="strength", seconds=12),
        A("Hot Chains", "Glowing chains lock the creatures around you in place.", "stun_ring", "minecraft:chain", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Sledge Leap", "Leap and drive a sledge into the foes below.", "leap_slam", "minecraft:mace", vfx="FLAME_EXPLOSION", damage=8, height=0.9),
    ],
    passives=[
        P("Forge-Proof", "You take much less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire", pct=0.4),
        P("Scalding Plate", "Part of every blow against you is returned in a flash of heat.", "thorns", "minecraft:iron_bars", pct=0.18),
        P("Burning Resolve", "While you are on fire, you take less damage.", "reduce_when", "minecraft:blaze_powder", condition="on_fire", pct=0.2),
        P("Smith's Hand", "While you hold a sword, you deal more damage.", "dmg_when", "minecraft:iron_sword", condition="holding_sword", bonus=0.15),
        P("Anvil Ring", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
        P("Quenched Edge", "Your blows land critically more often.", "crit", "minecraft:flint", chance=0.1, mult=1.7),
        P("Cooling Down", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
    ],
    finishers=[
        A("Forge of the Gods", "Hammer out a whole armory and loose it at once.", "barrage", "minecraft:netherite_sword", vfx="WEAPON_CONSTRUCTS", damage=5, shots=12, range=18, cost=0.08, cooldown=1800),
        A("Divine Anvil", "Drop a mountain-sized anvil on the spot you look at.", "meteor_drop", "minecraft:smithing_table", vfx="WEAPON_CONSTRUCTS", damage=30, radius=5.5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 20
LINES.append(Line(
    id="goh_artemis_charyeok", name="Artemis's Charyeok", franchise=F,
    desc="Borrow the huntress's moon-bright bow: silver arrows, rain of arrows, a mark on the quarry and a silent step through the trees. A sniper whose prey never sees the shot.",
    color="#C5E1A5", background="forest", icon="minecraft:bow",
    book_name="Huntress's Quarry Book", book_lore="Every page has a single name and a single arrow.",
    stances=[
        S("Draw and Hold Stance", "Hold the draw: a sharper eye with crits, but the body stays still.", "minecraft:bow", crit_chance=0.1, damage_pct=0.08, speed_pct=-0.06, vfx="ELF_CIRCLE"),
        S("Silent Runner Stance", "Run through the trees: fast and light, with a thin guard.", "minecraft:leather_boots", speed_pct=0.1, armor=-1, damage_pct=-0.03, vfx="ELF_CIRCLE"),
    ],
    stats=[
        T("Huntress's Eye", "Your aim reaches a long way.", "entity_interaction_range", 0.5, "minecraft:spyglass"),
        T("Steady Draw", "A long, quiet bow arm.", "attack_damage", 1, "minecraft:bow"),
        T("Forest Stride", "You run as the forest allows.", "movement_speed", 0.05, "minecraft:leather_boots", op="mult"),
        T("Quick Nock", "Another arrow on the string at once.", "attack_speed", 0.2, "minecraft:arrow"),
    ],
    arts=[
        A("Silver Arrow", "Loose a silver arrow that pierces through the first foes.", "ray_shot", "minecraft:spectral_arrow", vfx="ELF_CIRCLE", damage=8, range=24, pierce=2),
        A("Arrow Rain", "Loose a rapid volley at the foe you look at.", "barrage", "minecraft:arrow", vfx="ELF_CIRCLE", damage=3, shots=7, range=16),
        A("Moon Mark", "Mark the quarry in silver: it takes extra damage while marked.", "mark", "minecraft:glow_item_frame", vfx="ELF_CIRCLE", bonus=0.3, range=28),
        A("Forest Step", "Slip behind a trunk and appear a few blocks away.", "blink", "minecraft:oak_leaves", vfx="ELF_CIRCLE", distance=9),
        A("Ricochet Shot", "An arrow that hits the first foe and bounces on to the next.", "chain_hit", "minecraft:arrow", vfx="ELF_CIRCLE", damage=5, jumps=4),
        A("Cloaking Mist", "A pale mist hides you from the creatures near you.", "smoke", "minecraft:white_dye", vfx="ASH_FORMATION", radius=5, seconds=6),
        A("Finishing Arrow", "An arrow at a weakened quarry that bites far harder at low health.", "execute", "minecraft:tipped_arrow", vfx="ELF_CIRCLE", damage=9, bonus=0.9),
    ],
    passives=[
        P("Keen Eye", "Your hits land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.14, mult=1.8),
        P("Moonlight Hunter", "At night you deal more damage.", "dmg_when", "minecraft:clock", condition="night", bonus=0.2),
        P("Vanish into the Leaves", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.12),
        P("Hunter's Dash", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.2),
        P("Quarry's End", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:bow", threshold=0.25, bonus=0.7),
        P("Owl Eyes", "At night you see clearly.", "self_effect_when", "minecraft:golden_carrot", effect="night_vision", condition="night"),
        P("Trophy Skinning", "Whatever you fell may drop an extra stack of loot.", "drop_bonus", "minecraft:leather", chance=0.1),
    ],
    finishers=[
        A("Moonpiercer", "Loose a giant silver arrow that goes through the whole line of foes ahead.", "ray_shot", "minecraft:end_rod", vfx="ELF_CIRCLE", damage=30, range=32, pierce=5, cost=0.06, cooldown=1700),
        A("Hunt of a Thousand Arrows", "A rain of arrows from the sky onto the foe in front of you.", "barrage", "minecraft:tipped_arrow", vfx="ELF_CIRCLE", damage=5, shots=12, range=18, cost=0.08, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 21
LINES.append(Line(
    id="goh_apollo_charyeok", name="Apollo's Charyeok", franchise=F,
    desc="Borrow the sun god's gifts: healing light, a golden beam and a glare that blinds. Gentle with friends, a solar flare to foes.",
    color="#FFCA28", background="holy", icon="minecraft:sunflower",
    book_name="Hymnal of the Sun Chariot", book_lore="Best read at noon. At night it quietly glows.",
    stances=[
        S("Noon Radiance Stance", "Stand in full light: sturdy, recovering, and slightly softer in attack.", "minecraft:sunflower", armor=2, regen=0.6, damage_pct=-0.04, vfx="SPIRIT_AURA"),
        S("Solar Flare Stance", "Burn bright: stronger and faster, with a thin guard.", "minecraft:blaze_powder", damage_pct=0.14, speed_pct=0.04, armor=-2, vfx="FIRE_SPIRAL"),
    ],
    stats=[
        T("Sunlit Heart", "Warm light keeps you healthy.", "max_health", 2, "minecraft:golden_apple"),
        T("Golden Strike", "A golden shine behind every blow.", "attack_damage", 1, "minecraft:gold_ingot"),
        T("Daybreak Step", "You run towards the sun.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Muse's Fortune", "Songs go in your favor.", "luck", 1, "minecraft:note_block"),
    ],
    arts=[
        A("Healing Hymn", "A bright hymn that heals you and the allies around you.", "heal_pulse", "minecraft:note_block", vfx="SPIRIT_AURA", radius=7, amount=6),
        A("Chariot's Blessing", "A golden rush for you and your allies: haste for a while.", "buff_aura", "minecraft:gold_ingot", vfx="FIRE_SPIRAL", effect="haste", radius=9, seconds=12),
        A("Sunbeam", "A narrow beam of sunlight pierces the first foes ahead.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=8, range=20, pierce=2),
        A("Solar Glare", "A blinding glare in a cone: the foes are blinded and lit up.", "debuff_cone", "minecraft:glowstone", vfx="FIRE_SPIRAL", effect="blindness", effect2="glowing", seconds=5, seconds2=8),
        A("Dawn Mending", "The first light of dawn: heal and regenerate.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=8),
        A("Flare Ring", "A ring of flame-bright light bursts out of you.", "ring_burst", "minecraft:fire_charge", vfx="FIRE_BURST", damage=6, radius=4),
        A("Sun Chariot's Fall", "Call down a burst of sun on the spot you look at.", "meteor_drop", "minecraft:magma_block", vfx="FLAME_EXPLOSION", damage=12, radius=3.5),
    ],
    passives=[
        P("Sunrise Salve", "In the day your wounds close on their own.", "regen_when", "minecraft:glow_berries", condition="day", per_second=0.6),
        P("Midday Might", "In the day your blows are stronger.", "dmg_when", "minecraft:gold_ingot", condition="day", bonus=0.2),
        P("Chorus of Light", "Allies near you slowly regenerate.", "aura", "minecraft:note_block", effect="regeneration", target="allies", radius=7),
        P("Sunlit Vigor", "In the day you feel stronger.", "self_effect_when", "minecraft:sunflower", effect="strength", condition="day"),
        P("Bright Salve", "Your wounds close over time.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Smite the Dead", "The undead take much more damage from you.", "dmg_vs", "minecraft:skeleton_skull", category="undead", bonus=0.2),
        P("Sun-Tempered", "You take much less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire", pct=0.3),
    ],
    finishers=[
        A("Dawn of Mercy", "A great wave of golden light heals you and every ally around you.", "heal_pulse", "minecraft:beacon", vfx="SPIRIT_AURA", radius=12, amount=18, cost=0.08, cooldown=1700),
        A("Solar Cannon", "A massive beam of sun that burns a lane through the foes ahead.", "ray_shot", "minecraft:glowstone", vfx="FIRE_SPIRAL", damage=28, range=30, pierce=4, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 22
LINES.append(Line(
    id="goh_charyeok_vessel", name="Charyeok Vessel", franchise=F,
    desc="A body that can hold a borrowed power. You draw a spirit's strength into yourself and spend it as bursts, beams, blinks and borrowed armor. Flexible, raw and unfinished.",
    color="#7C4DFF", background="dream", icon="minecraft:echo_shard",
    book_name="Contract of the Borrowed Hour", book_lore="Terms: you may keep it until it wants to leave.",
    stances=[
        S("Open Vessel Stance", "Leave the door open: stronger blows and crits, thin guard.", "minecraft:echo_shard", damage_pct=0.14, crit_chance=0.06, armor=-2, vfx="SPIRIT_AURA"),
        S("Closed Vessel Stance", "Close the door: sturdier and recovering, at lower offense.", "minecraft:amethyst_shard", armor=3, regen=0.5, damage_pct=-0.05, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Receptive Body", "A body that holds a borrowed power.", "max_health", 2, "minecraft:echo_shard"),
        T("Borrowed Strength", "Strength that is not yours, but works.", "attack_damage", 1, "minecraft:amethyst_shard"),
        T("Spirit Step", "Light, as if carried.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
        T("Contract Luck", "The terms go your way.", "luck", 1, "minecraft:emerald"),
    ],
    arts=[
        A("Borrow Power", "Draw a spirit's strength into your body: strength and resistance for a while.", "buff_self", "minecraft:echo_shard", vfx="SPIRIT_AURA", effect="strength", effect2="resistance", seconds=14),
        A("Echo Burst", "Release the borrowed power in a ring around you.", "ring_burst", "minecraft:amethyst_shard", vfx="SPIRIT_AURA", damage=5, radius=4),
        A("Spirit Step", "Slip sideways into the spirit side and reappear ahead.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=9),
        A("Borrowed Beam", "Fire the borrowed power as a beam through the first foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=7, range=18, pierce=2),
        A("Return the Loan", "Pay back part of the power by healing: heal and regenerate.", "heal_self", "minecraft:glow_berries", vfx="SPIRIT_AURA", amount=7),
        A("Borrowed Aegis", "Wrap yourself in the spirit's armor: it absorbs damage.", "shield", "minecraft:amethyst_block", vfx="SPIRIT_AURA", absorb_hearts=4),
        A("Possession Chain", "A strike that jumps from foe to foe like a spirit.", "chain_hit", "minecraft:soul_torch", vfx="LIGHTNING_FIEND", damage=5, jumps=3),
    ],
    passives=[
        P("Spirit Sense", "Every kill gives more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
        P("Resonant Presence", "Allies near you feel stronger.", "aura", "minecraft:echo_shard", effect="strength", target="allies", radius=6),
        P("Borrowed Time", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Spirit Strike", "Your blows land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Settling of the Spirit", "Your wounds close over time.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Spirit Bones", "In the dark of night you move faster.", "self_effect_when", "minecraft:soul_torch", effect="speed", condition="night"),
        P("Hex Ward", "You take less magic damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.25),
    ],
    finishers=[
        A("Full Possession", "Let the power take you completely: an enormous ring of force around you.", "ring_burst", "minecraft:nether_star", vfx="SPIRIT_AURA", damage=24, radius=7, targets=9, knockback=1.8, cost=0.06, cooldown=1800),
        A("Contract Collapse", "Burn the contract: a pillar of spirit-force on the spot you look at.", "meteor_drop", "minecraft:end_crystal", vfx="SPATIAL_RIFT", damage=28, radius=5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 23
LINES.append(Line(
    id="goh_heracles_labors", name="Heracles's Labors", franchise=F,
    desc="Borrow the hero of the twelve labors: a lion-skin hide, a hydra's regrowth, a stag's speed and a strength that has beaten monsters. You are built to fight things bigger than you.",
    color="#D84315", background="ruin", icon="minecraft:leather_chestplate",
    book_name="The Twelve Labors, Abridged", book_lore="Eleven are done. The twelfth is sitting in front of you.",
    stances=[
        S("Lion-Skin Stance", "Wear the lion's hide: huge armor and stability, slow to act.", "minecraft:leather_chestplate", armor=4, knockback_res=0.4, speed_pct=-0.1, vfx="EARTH_SPIKES"),
        S("Labor Stance", "Take on the labor: stronger, tireless, and a bit too bold.", "minecraft:iron_axe", damage_pct=0.16, regen=0.2, armor=-2, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Demigod's Vitality", "A long-lived body.", "max_health", 2, "minecraft:enchanted_golden_apple"),
        T("Lion-Hide Skin", "A hide no blade has found.", "armor", 1, "minecraft:leather"),
        T("Club Arm", "A club feels like a feather.", "attack_damage", 1, "minecraft:iron_axe"),
        T("Strong Legs", "Legs for chasing stags.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Lion-Hide Guard", "Pull the lion's hide around you as a thick shield of absorption.", "shield", "minecraft:leather_chestplate", vfx="EARTH_SPIKES", absorb_hearts=5, resist_amp=1),
        A("Hide-Breaker", "A blow reserved for creatures already weakened: far stronger at low health.", "execute", "minecraft:iron_axe", vfx="EARTH_SPIKES", damage=9, bonus=0.9),
        A("Hydra Heads", "A strike that leaps from foe to foe like a hydra's many heads.", "chain_hit", "minecraft:slime_ball", vfx="POISON_BREATH", damage=5, jumps=4),
        A("Stag Catch", "Catch the creatures around you with a lasso and drag them to you.", "pull", "minecraft:lead", vfx="THREAD_LINE", radius=8, strength=1.0, damage=3),
        A("Stymphalian Shot", "Loose an iron-tipped arrow that pierces a line of foes.", "ray_shot", "minecraft:arrow", vfx="WIND_SLASH", damage=7, range=20, pierce=3),
        A("Hydra Regrowth", "Your wounds regrow like a hydra's heads: heal and regenerate.", "heal_self", "minecraft:golden_apple", vfx="POISON_BREATH", amount=8),
        A("Labor's Leap", "Leap and crash down with the full weight of a hero.", "leap_slam", "minecraft:anvil", vfx="EARTH_SPIKES", damage=8, height=0.9),
    ],
    passives=[
        P("Monster Slayer", "You deal more damage to monsters.", "dmg_vs", "minecraft:zombie_head", category="monster", bonus=0.2),
        P("Hero's Appetite", "A share of every blow you deal returns as health.", "lifesteal", "minecraft:cooked_beef", pct=0.05),
        P("Hydra Blood", "When wounded you recover health continuously.", "regen_when", "minecraft:golden_apple", condition="low_health", per_second=1.0),
        P("Lion Hide", "While you are fighting you take less damage.", "reduce_when", "minecraft:leather_chestplate", condition="in_combat", pct=0.1),
        P("Spoils of the Labor", "Kills restore health.", "kill_reward", "minecraft:cooked_beef", heal=4, effect="none"),
        P("Unbowed Hero", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Trophy of the Hunt", "Whatever you defeat may drop an extra stack of loot.", "drop_bonus", "minecraft:leather", chance=0.1),
    ],
    finishers=[
        A("Twelfth Labor", "Gather your whole strength in one blow, devastating against a wounded foe.", "execute", "minecraft:netherite_axe", vfx="EARTH_SPIKES", damage=28, bonus=2.3, cost=0.06, cooldown=1600),
        A("Nemean Roar", "The lion's roar rolls out as a huge ring that throws every foe around you.", "ring_burst", "minecraft:goat_horn", vfx="EARTH_SPIKES", damage=22, radius=6.5, targets=9, knockback=1.7, cost=0.06, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 24
LINES.append(Line(
    id="goh_pankration_grappler", name="Pankration Grappler", franchise=F,
    desc="Wrestling and all-in grappling: tackles, suplexes, pins and a taunt that makes the other side lose their heads. Dirty and fair at once.",
    color="#8E24AA", background="iron", icon="minecraft:leather_leggings",
    book_name="Sand-Pit Grappler's Record", book_lore="The scorecard is covered in dirt and several footprints.",
    stances=[
        S("Wrestler's Crouch", "Get low: armor, firm footing, slower.", "minecraft:leather_leggings", armor=3, knockback_res=0.3, speed_pct=-0.06, vfx="WIND_RING"),
        S("Showman Stance", "Play to the crowd: stronger blows, but you are wide open.", "minecraft:gold_ingot", damage_pct=0.15, crit_chance=0.05, armor=-2, vfx="WIND_RING"),
    ],
    stats=[
        T("Wrestler's Back", "A back like a door.", "max_health", 2, "minecraft:cooked_beef"),
        T("Bear Hug", "Your arms close like a vice.", "attack_damage", 1, "minecraft:iron_axe"),
        T("Mat Footing", "Floors are your friend.", "knockback_resistance", 0.1, "minecraft:white_wool"),
        T("Crowd Stamina", "Fights run long.", "armor_toughness", 1, "minecraft:iron_ingot"),
    ],
    arts=[
        A("Double-Leg Tackle", "Shoot in low and carry the foes in your way with you.", "dash_strike", "minecraft:iron_boots", vfx="WIND_RING", damage=7, distance=7),
        A("Rope-Burn Pull", "Drag the creatures around you into the ropes with you.", "pull", "minecraft:lead", vfx="WIND_RING", radius=7, strength=1.0, damage=3),
        A("Body Slam", "Leap and slam your whole weight onto the foes below.", "leap_slam", "minecraft:white_wool", vfx="WIND_RING", damage=8, height=0.8),
        A("Referee's Count", "Pin the creatures near you: they are held in place.", "stun_ring", "minecraft:bell", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Belly-to-Back Suplex", "Heave the foe in front of you up and over.", "uppercut", "minecraft:white_banner", vfx="WIND_SLASH", damage=8, launch=0.9),
        A("Trash Talk", "Talk the hostile creatures around you into coming for you.", "taunt", "minecraft:name_tag", vfx="DEVIL_CIRCLE", radius=8, targets=5),
        A("Tag Out", "Step out of the ring for a moment: heal and regenerate.", "heal_self", "minecraft:bread", vfx="SPIRIT_AURA", amount=7),
    ],
    passives=[
        P("Take the Hit", "While you are fighting you take less damage.", "reduce_when", "minecraft:leather_chestplate", condition="in_combat", pct=0.1),
        P("Bear Hug Barbs", "Part of every blow against you is returned.", "thorns", "minecraft:iron_nugget", pct=0.15),
        P("Crowd Fuel", "Every kill gives you some health.", "kill_reward", "minecraft:cooked_beef", heal=3, effect="none"),
        P("Powerhouse", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.3),
        P("Pin Rhythm", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Mat Breath", "You tire slowly.", "hunger_saver", "minecraft:bread", pct=0.25),
        P("Hold Your Ground", "When nearly dead, a brief shield holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
    ],
    finishers=[
        A("Championship Slam", "Leap to the top rope and fall: a crater-making slam on the foes below.", "leap_slam", "minecraft:anvil", vfx="WIND_RING", damage=26, radius=5, knockback=1.8, height=1.1, cost=0.06, cooldown=1600),
        A("Arena Lockdown", "Pin everything around you with one huge grab: all are held for a long moment.", "stun_ring", "minecraft:chain", vfx="SEAL_CHAINS", radius=8, seconds=4, targets=10, cost=0.08, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 25
LINES.append(Line(
    id="goh_tiger_crane_kungfu", name="Tiger and Crane Kung Fu", franchise=F,
    desc="Two classic animal styles in one body: the tiger's claw for power, the crane's wing for balance and evasion. Hard strikes, then a hop away.",
    color="#FFA000", background="forest", icon="minecraft:honeycomb",
    book_name="Scroll of Tiger and Crane", book_lore="The tiger paragraphs are underlined in red; the crane paragraphs in white.",
    stances=[
        S("Tiger Stance", "Low and ready to pounce: hard-hitting, with a thinner guard.", "minecraft:rabbit_hide", damage_pct=0.15, crit_chance=0.04, armor=-1, vfx="WIND_SLASH"),
        S("Crane Stance", "On one leg and light: quick and elusive, a bit softer.", "minecraft:feather", speed_pct=0.08, armor=1, damage_pct=-0.04, vfx="WIND_RING"),
    ],
    stats=[
        T("Tiger Claw", "A hand that can grip stone.", "attack_damage", 1, "minecraft:rabbit_hide"),
        T("Crane Balance", "Standing on one foot is easy for you.", "safe_fall_distance", 2, "minecraft:feather"),
        T("Dragon Breath", "Slow breathing, long life.", "max_health", 2, "minecraft:honey_bottle"),
        T("Mantis Speed", "Rapid hands.", "attack_speed", 0.2, "minecraft:iron_nugget"),
    ],
    arts=[
        A("Tiger Claw Rake", "Rake the cone in front of you with five fingers like blades.", "cone_strike", "minecraft:rabbit_hide", vfx="WIND_SLASH", damage=7, half_angle=55),
        A("Crane Hop", "Hop a good way on one leg like a crane.", "blink", "minecraft:feather", vfx="WIND_RING", distance=8),
        A("Mantis Jabs", "A hail of tiny jabs at the foe you look at.", "barrage", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=3, shots=6),
        A("Dragon Tail Sweep", "Spin low and sweep everything around you several times.", "whirlwind", "minecraft:string", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Drunken Rhythm", "Sway and stumble perfectly: speed and jump height for a while.", "buff_self", "minecraft:sweet_berries", vfx="SPIRIT_AURA", effect="speed", effect2="jump_boost", seconds=14),
        A("Snake Fang", "A swift, poisonous finger strike at a weakened foe.", "execute", "minecraft:spider_eye", vfx="WIND_SLASH", damage=8, bonus=0.8),
        A("Wing Block", "Wings spread and protect: a shell of absorption.", "shield", "minecraft:phantom_membrane", vfx="WIND_RING", absorb_hearts=4),
    ],
    passives=[
        P("Tiger Pounce", "Your hits land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Crane Evade", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.12),
        P("Flow of the Forms", "Every few strikes in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Light Foot", "You take much less fall damage.", "fall_guard", "minecraft:phantom_membrane", pct=0.5),
        P("Snake Venom", "Your hits may poison the foe.", "on_hit_effect", "minecraft:spider_eye", effect="poison", chance=0.2),
        P("Breath of the Dragon", "Your wounds close over time.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Fighting Spirit", "While you are fighting you move faster.", "self_effect_when", "minecraft:sugar", effect="speed", condition="in_combat"),
    ],
    finishers=[
        A("Hundred Tiger Claws", "A sweeping tiger strike that rips a huge cone in front of you.", "cone_strike", "minecraft:netherite_sword", vfx="WIND_SLASH", damage=28, range=5.5, half_angle=70, targets=9, cost=0.06, cooldown=1700),
        A("Crane's Soaring Fall", "Leap high on the crane's wing and come down like a stone.", "leap_slam", "minecraft:elytra", vfx="WIND_RING", damage=26, radius=5, knockback=1.7, height=1.2, cost=0.06, cooldown=1600),
    ],
))

# 3.26.0 helpers for the Evolution lines: a passive with an `evolved` override and the Ultimate finisher
def PE(name, desc, kind, icon, evolved=None, **p):
    return Pas(name, desc, kind, p, icon, evolved or {})


def AU(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon, ultimate=True)


# ---------------------------------------------------------------------------------------------------------------- 26  (3.26.0, Evolution skill, design row 9)
LINES.append(Line(
    id="goh_haetae_jade", name="Haetae to Jade Emperor", franchise=F,
    desc="Han Daewi's water Charyeok: the young Haetae's bubbles, waves and hard water grow, through a direct contract, into the Jade Emperor's Wisdom of the Sage. Endure enough punishment and the water becomes gravity itself.",
    color="#3FB6FF", background="ocean", icon="minecraft:heart_of_the_sea",
    book_name="Haetae's Water Ledger", book_lore="The first pages are damp and written in a child's hand; the last ones are dry, heavy, and press down on the desk.",
    stances=[
        S("Haetae's Tide Stance", "Let the young Haetae's water flow around you: you heal and move lightly, while your blows stay ordinary.", "minecraft:heart_of_the_sea", regen=1.0, speed_pct=0.06, vfx="WATER_RING"),
        S("Hard Water Barrier", "Thicken the water around you into a firm shell: strong armor and footing, but you move slowly and strike softer.", "minecraft:prismarine_shard", armor=4, knockback_res=0.4, speed_pct=-0.1, damage_pct=-0.05, vfx="WATER_RING"),
    ],
    stats=[
        T("Haetae's Bond", "A bond with a water creature keeps you well.", "max_health", 2, "minecraft:heart_of_the_sea"),
        T("Karate Fist", "A full-contact karate base behind every swing.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Tide Stride", "You step as lightly as a wave.", "movement_speed", 0.04, "minecraft:kelp", op="mult"),
        T("Sage's Patience", "The water in you does not give way.", "armor_toughness", 1, "minecraft:prismarine_crystals"),
    ],
    arts=[
        A("Water Arrows", "Loose a rapid volley of water arrows at the foe in front of you.", "barrage", "minecraft:tipped_arrow", vfx="WATER_BURST", damage=3, shots=6, range=14),
        A("Water Arrows: Shift", "Crouch to compress the volley into one piercing arrow of hard water that runs through a line of foes.", "ray_shot", "minecraft:spectral_arrow", vfx="WATER_BURST", damage=8, range=24, pierce=3),
        A("Water Wave", "Send a wall of water rolling along the ground ahead of you.", "ground_wave", "minecraft:water_bucket", vfx="WATER_RING", damage=6, length=11, width=3, knockback=0.7),
        A("Water Wave: Shift", "Crouch to let the wave burst outward around you as a defensive ring that throws foes back.", "ring_burst", "minecraft:nautilus_shell", vfx="WATER_RING", damage=5, radius=4.5, targets=6, knockback=1.2),
        A("Bubble Lift", "Wrap yourself in bubbles: you drift down softly and leap far.", "buff_self", "minecraft:glass_bottle", vfx="WATER_BURST", effect="slow_falling", effect2="jump_boost", seconds=14),
        A("Healing Mist", "A faint healing mist rises: you and the allies near you recover health.", "heal_pulse", "minecraft:potion", vfx="WATER_RING", radius=7, amount=6),
        A("Haetae's Breath", "After the direct contract the Haetae lends its breath: a cold, hard-water gust that rakes a cone ahead.", "cone_strike", "minecraft:conduit", vfx="WATER_BURST", damage=9, range=5, half_angle=55, targets=5, knockback=0.9),
        A("Haetae's Breath: Shift", "Crouch to hold the breath longer: foes in the cone are chilled and slowed by hard water.", "debuff_cone", "minecraft:blue_ice", vfx="WATER_RING", range=7, half_angle=45, seconds=8, effect="slowness", effect2="weakness", seconds2=5),
    ],
    passives=[
        PE("Tidal Pressure", "Your blows press water into the foe: part of the damage is stored as Debt, ticks while it waits, and is called in by your Ultimate.", "debt_store", "minecraft:prismarine_bricks",
          store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WATER_RING", evolved={"store_pct": 0.45, "max_pct": 0.75, "hold_seconds": 20}),
        PE("Hard Water Skin", "While you are fighting, water thickens over your skin and you take less damage.", "reduce_when", "minecraft:prismarine_shard", condition="in_combat", pct=0.1, evolved={"pct": 0.22}),
        PE("Water Barrier", "Your barrier blunts magic and borrowed power, even another Charyeok.", "resist", "minecraft:heart_of_the_sea", damage_kind="magic", pct=0.2, evolved={"pct": 0.4}),
        PE("Haetae's Mending Water", "While you stand in water your wounds close quickly.", "regen_when", "minecraft:kelp", condition="in_water", per_second=1.5),
        PE("Soaking Blow", "Your blows may leave the foe sodden and slow.", "on_hit_effect", "minecraft:cod", effect="slowness", chance=0.25),
        PE("Haetae's Swim", "In water you breathe freely.", "self_effect_when", "minecraft:turtle_helmet", effect="water_breathing", condition="in_water"),
        PE("Second Life", "When nearly dead, a brief shield of water holds you up, as the Haetae brought you back once before.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=6),
        PE("Tide Mending", "Your wounds slowly close over time, even out of combat.", "mending", "minecraft:glow_ink_sac", seconds=16, amount=1),
    ],
    finishers=[
        A("Haetae's Deluge", "Call the grown Haetae's whole tide: a great ring of water that throws every foe around you.", "ring_burst", "minecraft:trident", vfx="WATER_BURST", damage=22, radius=7, targets=10, knockback=1.8, cost=0.06, cooldown=1700),
        AU("Wisdom of the Sage", "The Jade Emperor's Wisdom: gravity bears down on every foe around you, and all the Debt stored in them is called in at once.", "debt_burst", "minecraft:nether_star", vfx="SPATIAL_RIFT",
          damage=30, mult=2.5, radius=9, targets=20, cost=0.08, cooldown=16000),
    ],
    evolution={"requirement": "damage_taken", "count": 3000, "params": {"damage_kind": "any"},
               "text": "Take 3000 damage of any kind while this skill is on: the Haetae grows, the contract deepens and the Wisdom of the Sage opens.",
               "ultimate_name": "Ultimate Skill, Wisdom of the Sage"},
))

# ---------------------------------------------------------------------------------------------------------------- 27  (3.26.0, Evolution skill, design row 10)
LINES.append(Line(
    id="goh_lu_bu_moonlight", name="Yeo-Po Bong-Seon", franchise=F,
    desc="Yoo Mira's Moonlight Sword Style, a wooden blade that cuts steel, joined to the Charyeok of Lu Bu and his flying horse Red Hare. Cut down enough foes and the hero of legend rides with you.",
    color="#F0E6A0", background="stellar", icon="minecraft:wooden_sword",
    book_name="Bongseon Master's Scroll", book_lore="The twenty-fifth name on the scroll is still wet; a horse's hoofprint is pressed into the last page.",
    stances=[
        S("Moonlight Stance", "Hold the blade low, in either hand: quick, sharp and prone to critical cuts, with a lighter guard.", "minecraft:wooden_sword", speed_pct=0.06, crit_chance=0.08, damage_pct=0.04, armor=-1, vfx="WIND_RING"),
        S("Mountain Stance", "Plant yourself like a mountain: firm armor and footing, but slower to act.", "minecraft:iron_horse_armor", armor=4, knockback_res=0.5, speed_pct=-0.12, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Bongseon's Edge", "A wooden sword that cuts steel and concrete.", "attack_damage", 1, "minecraft:wooden_sword"),
        T("Either-Hand Mastery", "The blade swings as fast from the left hand as from the right.", "attack_speed", 0.2, "minecraft:feather"),
        T("Red Hare's Stride", "A horse's pace is in your legs.", "movement_speed", 0.04, "minecraft:saddle", op="mult"),
        T("Master's Bearing", "Twenty-five masters stand behind your back.", "armor_toughness", 1, "minecraft:iron_horse_armor"),
    ],
    arts=[
        A("Moonlight Slash", "A wide crescent cut of moonlight across the cone in front of you.", "cone_strike", "minecraft:wooden_sword", vfx="WIND_SLASH", damage=8, range=4, half_angle=55, targets=4, knockback=0.5),
        A("Moonlight Slash: Shift", "Crouch to stretch the crescent into a long wave of moonlight along the ground.", "ground_wave", "minecraft:end_rod", vfx="WIND_SLASH", damage=7, length=14, width=2, knockback=0.4),
        A("Sky Piercer Lunge", "Lunge forward with the point of the blade, hitting everything in your path.", "dash_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=8, distance=9, targets=4),
        A("Sky Piercer Lunge: Shift", "Crouch to loose the lunge as one long beam that pierces a whole line of foes.", "ray_shot", "minecraft:spectral_arrow", vfx="WIND_SLASH", damage=9, range=26, pierce=4),
        A("Red Hare Charge", "Call Red Hare and gallop a long way forward in a single breath.", "blink", "minecraft:saddle", vfx="WIND_RING", distance=13),
        A("Return the Blade", "For a short while, part of every blow against you is returned to the attacker by the blade.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", seconds=7, reflect_pct=0.4),
        A("Steel-Cutting Stroke", "One clean stroke, far deadlier against a foe already wounded.", "execute", "minecraft:golden_sword", vfx="WIND_SLASH", damage=10, bonus=1.0),
        A("Lu Bu's Might", "Borrow the hero's strength: raised power and speed for a while, and feathers in your hair.", "buff_self", "minecraft:golden_horse_armor", vfx="SPIRIT_AURA", effect="strength", effect2="speed", seconds=16),
    ],
    passives=[
        PE("Moonlit Wounds", "Your cuts store part of their damage on the foe as Debt: it ticks while it waits, and your Ultimate calls it in.", "debt_store", "minecraft:amethyst_shard",
          store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WIND_SLASH"),
        PE("Moonlit Edge", "Your cuts land critically more often.", "crit", "minecraft:glowstone_dust", chance=0.12, mult=1.7, evolved={"chance": 0.22, "mult": 2.1}),
        PE("Two-Handed Flow", "Every few cuts in a row hit much harder.", "combo", "minecraft:clock", hits=4, bonus=0.6, evolved={"hits": 3, "bonus": 1.0}),
        PE("Willow Step", "You may slip a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        PE("Duel by Night", "You deal more damage at night, under the moon.", "dmg_when", "minecraft:ender_eye", condition="night", bonus=0.15),
        PE("Red Hare's Momentum", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:golden_carrot", bonus=0.2, evolved={"bonus": 0.4}),
        PE("Tally of the Slain", "Every kill restores some health.", "kill_reward", "minecraft:bone", heal=3, effect="none"),
        PE("Finishing Eye", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.2, bonus=0.6),
    ],
    finishers=[
        A("Moonlight Final Cut", "Gather the whole style into one stroke, devastating against a wounded foe.", "execute", "minecraft:netherite_sword", vfx="WIND_SLASH", damage=28, bonus=2.2, cost=0.06, cooldown=1700),
        AU("Sky Piercer on Red Hare", "Ride Red Hare through the whole field and pierce the sky: every foe in reach takes the blow plus all the Debt stored in it.", "debt_burst", "minecraft:diamond_horse_armor", vfx="LIGHTNING_SPEAR",
          damage=28, mult=2.4, radius=9, targets=20, cost=0.07, cooldown=15000),
    ],
    evolution={"requirement": "kills", "count": 666,
               "text": "Defeat 666 foes while this skill is on: the hero of legend answers and the Sky Piercer on Red Hare opens.",
               "ultimate_name": "Ultimate Skill, Sky Piercer Red Hare"},
))

# ---------------------------------------------------------------------------------------------------------------- 28  (3.27.0, Evolution skill, design row 9 of evolution_ten_b.md)
LINES.append(Line(
    id="goh_ilpyo_sword", name="Ilpyo's Family Blade", franchise=F,
    desc="Park Ilpyo's sword of the Park family, drawn on Ssam-su Taekkyeon footwork and lit by the fox flame of the Nine-Tails Guardian. Cut down enough foes and the ninth tail is let loose.",
    color="#D0D8E0", background="iron", icon="minecraft:iron_sword",
    book_name="Park Family Blade Record", book_lore="Every page is a name struck through in silver ink; the last one is singed at the edge, and nine thin fox hairs are pressed into the spine.",
    stances=[
        S("Taekkyeon Sway Stance", "Rock on the balls of your feet with the blade low, as Taekkyeon does: quick, evasive and a little reckless.", "minecraft:iron_sword", speed_pct=0.07, crit_chance=0.05, damage_pct=0.03, armor=-1, vfx="WIND_RING"),
        S("Nine-Tails Guard", "Let the fox flame wrap the blade and your arms: firm armor and footing, but slow to move.", "minecraft:blaze_powder", armor=3, knockback_res=0.4, regen=0.5, speed_pct=-0.1, vfx="FIRE_SPIRAL"),
    ],
    stats=[
        T("Family Blade Edge", "A sword that has been kept in the Park family for generations.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Taekkyeon Footwork", "The Taekkyeon beat is in your heels.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Flexible Frame", "A body bent far past ordinary limits swings the blade faster.", "attack_speed", 0.2, "minecraft:feather"),
        T("Fox-Eyed Reach", "You judge a foe's distance like a fox.", "entity_interaction_range", 0.5, "minecraft:spyglass"),
    ],
    arts=[
        A("Family Blade Draw", "Draw the blade in one breath and cut a wide arc across the cone in front of you.", "cone_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=8, range=4, half_angle=50, targets=4, knockback=0.4),
        A("Family Blade Draw: Shift", "Crouch to let the cut skip from foe to foe, each a little weaker than the last.", "chain_hit", "minecraft:iron_nugget", vfx="WIND_SLASH", damage=7, jumps=4, jump_range=6),
        A("Falling Leaf Inferno", "Spin through the foes around you, the blade leaving leaves of fire in its wake.", "whirlwind", "minecraft:fire_charge", vfx="FIRE_SPIRAL", damage=3, radius=4, hits=4),
        A("Falling Leaf Inferno: Shift", "Crouch to leap high and come down in a ring of burning leaves.", "leap_slam", "minecraft:campfire", vfx="FLAME_EXPLOSION", damage=8, height=1.0, radius=3.5, knockback=0.7),
        A("Third Stance Hwachook", "Burn forward on the third Taekkyeon stance, hitting everything in your path.", "dash_strike", "minecraft:golden_boots", vfx="FLAME_TRAIL", damage=8, distance=9, targets=4),
        A("Third Stance Hwachook: Shift", "Crouch to hook the blade up under the foe and fling it into the air.", "uppercut", "minecraft:blaze_rod", vfx="FLAME_TRAIL", damage=8, launch=0.8),
        A("Pumbalgi Step", "Step on the Taekkyeon beat and slip a short way ahead.", "blink", "minecraft:rabbit_foot", vfx="WIND_RING", distance=8),
        A("Fox-Fire Volley", "Loose a volley of small fox flames at the foe you look at.", "barrage", "minecraft:blaze_powder", vfx="FIRE_SPIRAL", damage=3, shots=6, range=14),
    ],
    passives=[
        PE("Silver Ledger", "Your cuts store part of their damage on the foe as Debt: it ticks while it waits, and your Ultimate calls it in.", "debt_store", "minecraft:iron_ingot",
          store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="FIRE_SPIRAL", evolved={"store_pct": 0.4, "max_pct": 0.7, "hold_seconds": 20}),
        PE("Taekkyeon Rhythm", "Blows in a row grow stronger until you stop fighting.", "momentum", "minecraft:clock", per_stack=0.04, max_stacks=6, evolved={"per_stack": 0.07, "max_stacks": 9}),
        PE("Sway Away", "You may sway out of a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        PE("Fox-Flame Skin", "The Nine-Tails flame blunts fire.", "resist", "minecraft:magma_cream", damage_kind="fire", pct=0.2, evolved={"pct": 0.45}),
        PE("Silver Thirst", "Your blade drinks a share of the damage it deals.", "lifesteal", "minecraft:redstone", pct=0.05, evolved={"pct": 0.1}),
        PE("Tally of Names", "Every kill restores some health and quickens your step.", "kill_reward", "minecraft:name_tag", heal=3, effect="speed", seconds=4),
        PE("Shoving Cut", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.4, chance=0.3),
        PE("Foxfire Veil", "Fire clings to the blade while you fight, searing the foe.", "on_hit_effect", "minecraft:flint_and_steel", effect="wither", chance=0.2),
    ],
    finishers=[
        A("Triple Destruction Infernal Fang", "Three fox-fang cuts in one breath rip a huge cone of fire in front of you.", "cone_strike", "minecraft:netherite_sword", vfx="FLAME_EXPLOSION", damage=28, range=5.5, half_angle=70, targets=9, knockback=1.2, cost=0.06, cooldown=1700),
        AU("Ninth Tail Unleashed", "Let the ninth tail loose: white hair, fox eyes and a flame that finds every foe around you, calling in all the Debt stored in them at once.", "debt_burst", "minecraft:nether_star", vfx="FIRE_SPIRAL",
          damage=28, mult=2.4, radius=9, targets=20, cost=0.07, cooldown=15000),
    ],
    evolution={"requirement": "kills", "count": 1500,
               "text": "Defeat 1500 foes while this skill is on: the Nine-Tails Guardian answers and the Ninth Tail Unleashed opens.",
               "ultimate_name": "Ultimate Skill, Ninth Tail Unleashed"},
))

# ---------------------------------------------------------------------------------------------------------------- 29  (3.27.0, Evolution skill, design row 10 of evolution_ten_b.md)
LINES.append(Line(
    id="goh_taejin_charyeok", name="Taejin's Renewal Charyeok", franchise=F,
    desc="Jin Taejin, the old master of Renewal Taekwondo, a style made to kill gods, and a man who bears borrowed power's price. Survive enough blows that should have killed you and the old master's miracle answers.",
    color="#E8A23A", background="tower", icon="minecraft:golden_boots",
    book_name="Old Master's Mountain Notes", book_lore="The handwriting is shaky and the margins are full of kick diagrams; one line, underlined twice, says that a god can bleed like anyone.",
    stances=[
        S("Renewal Taekwondo Stance", "The god-killing stance of the old master: heavy, precise kicks, with a thinner guard.", "minecraft:golden_boots", damage_pct=0.12, crit_chance=0.05, armor=-1, vfx="SPIRIT_AURA"),
        S("Mountain Hermit's Breath", "Breathe as you did on the mountain: wounds close and the body holds firm, but you strike softer and slower.", "minecraft:honey_bottle", armor=3, regen=1.2, knockback_res=0.3, speed_pct=-0.08, damage_pct=-0.05, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Old Master's Constitution", "A body that has outlived many fights.", "max_health", 3, "minecraft:golden_apple"),
        T("God-Slayer's Heel", "Every kick is meant to drop a god.", "attack_damage", 1, "minecraft:golden_boots"),
        T("Mountain Hide", "Years in the mountains toughened your skin.", "armor", 1, "minecraft:iron_chestplate"),
        T("Unmoved", "You do not give ground.", "knockback_resistance", 0.1, "minecraft:anvil"),
    ],
    arts=[
        A("Recoilless Kick", "A precise kick whose force does not rebound: a shock line that pierces a row of foes.", "ray_shot", "minecraft:spectral_arrow", vfx="SPIRIT_AURA", damage=8, range=20, pierce=3),
        A("Recoilless Kick: Shift", "Crouch to kick low and shake the brain: every foe around you is stunned for a moment.", "stun_ring", "minecraft:golden_boots", vfx="WIND_RING", radius=4, seconds=2, targets=5),
        A("God-Slayer Heel Drop", "Mark the place you look at: after a short breath a heel from above comes down on it.", "meteor_drop", "minecraft:anvil", vfx="SPIRIT_AURA", delay=20, damage=14, radius=4, range=18),
        A("God-Slayer Heel Drop: Shift", "Crouch to stamp the ground and send a shock wave rolling ahead of you.", "ground_wave", "minecraft:gold_ingot", vfx="EARTH_SPIKES", damage=7, length=11, width=3, knockback=0.6),
        A("Hermit's Breath", "Breathe slowly the way you did on the mountain: you heal and regenerate for a while.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=8, regen_seconds=8),
        A("Hermit's Breath: Shift", "Crouch to harden the body: strength and resistance for a while.", "buff_self", "minecraft:enchanted_golden_apple", vfx="SPIRIT_AURA", effect="resistance", effect2="strength", seconds=14),
        A("Old Master's Gaze", "Read the foe in front of you: it is marked and takes more damage for a while.", "mark", "minecraft:ender_eye", vfx="MAGIC_CIRCLE", range=18, seconds=10, bonus=0.3),
        A("Break Borrowed Power", "Meet a god's strength with your own: for a while part of every blow against you is thrown back.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", seconds=7, reflect_pct=0.4),
    ],
    passives=[
        PE("Master's Ledger", "Your kicks store part of their damage on the foe as Debt: it ticks while it waits, and your Ultimate calls it in.", "debt_store", "minecraft:gold_ingot",
          store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="SPIRIT_AURA", evolved={"store_pct": 0.42, "max_pct": 0.75, "hold_seconds": 22}),
        PE("Not Yet", "When nearly dead, a brief shield of will holds you up.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5, rest=1800, evolved={"seconds": 9, "rest": 1200}),
        PE("Hard Old Bones", "While you are badly hurt you take less damage.", "reduce_when", "minecraft:bone", condition="low_health", pct=0.12, evolved={"pct": 0.28}),
        PE("Against Borrowed Power", "Renewal Taekwondo was made against gods: you take less magic and borrowed-power damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.2, evolved={"pct": 0.45}),
        PE("Old Master's Rebuke", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:iron_chestplate", pct=0.15),
        PE("Second Wind", "While you fight, you slowly recover.", "regen_when", "minecraft:golden_carrot", condition="in_combat", per_second=0.6),
        PE("Cornered Master", "You deal more damage while you are badly hurt.", "dmg_when", "minecraft:redstone", condition="low_health", bonus=0.2),
        PE("Mountain Mending", "Your wounds close over time, even out of combat.", "mending", "minecraft:glow_berries", seconds=16, amount=1),
    ],
    finishers=[
        A("Heaven-Splitting Heel", "The old master's whole weight in one stamp: a great ring of force that throws every foe around you.", "ring_burst", "minecraft:netherite_boots", vfx="WIND_RING", damage=24, radius=7, targets=10, knockback=1.9, cost=0.06, cooldown=1700),
        AU("God-Killing Form", "Renewal Taekwondo in full, the form made to kill gods: every foe around you takes the blow plus all the Debt stored in it.", "debt_burst", "minecraft:nether_star", vfx="SPIRIT_AURA",
          damage=30, mult=2.5, radius=9, targets=20, cost=0.08, cooldown=16000),
    ],
    evolution={"requirement": "lethal_survived", "count": 10,
               "text": "Survive 10 blows that would have left you at 2 health or less while this skill is on: the old master's miracle answers and the God-Killing Form opens.",
               "ultimate_name": "Ultimate Skill, God-Killing Form"},
))
