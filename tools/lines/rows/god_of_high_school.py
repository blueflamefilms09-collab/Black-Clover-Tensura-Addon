"""The God of High School: 25 unique skill lines (martial arts, Ki, Charyeok and the gods that lend it, tournament roles).

Canon note: martial arts (Taekwondo, Muay Thai, Capoeira, Judo, Hapkido, Kendo ...) are real styles written as game arts; Charyeok (borrowed power) lines
follow the story's idea of a being or god lending its strength, while the exact moves are invented in its spirit (what the art looks and feels like in the game).
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
        A("Second Wind", "Roll the shoulders and breathe: heal and regenerate.", "heal_self", "minecraft:cooked_beef", vfx="SPIRIT_AURA", amount=7),
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
