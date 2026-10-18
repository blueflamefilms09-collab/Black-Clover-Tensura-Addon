"""Tower of God: 25 unique skill lines (Shinsu, Positions, roles, Ranker arts, Families, the Tower's own systems).

Canon note: names of Positions, Shinsu control and the big characters' tools follow the story; where the story is vague the
technique itself is invented in its spirit (the descriptions say what the art looks and feels like in the game, not what an official text says).
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "tower_of_god"


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
    id="tog_shinsu_shooting", name="Shinsu Shooting", franchise=F,
    desc="The first thing every Regular learns: pack Shinsu into your hand and fire it. Beams, volleys and bursts of raw blue pressure.",
    color="#4FC3F7", background="tower", icon="minecraft:amethyst_shard",
    book_name="Primer of Shinsu Shooting", book_lore="Page one says: breathe in the Tower, breathe out a bullet.",
    stances=[
        S("Open Stream Posture", "Let the Shinsu flow loose from your arm: your attacks hit harder but your guard thins.", "minecraft:blaze_powder", damage_pct=0.14, armor=-1, vfx="SPIRIT_AURA"),
        S("Packed Core Posture", "Compress your Shinsu into the chest: sturdier and steadier, a little slower to strike.", "minecraft:iron_ingot", armor=3, regen=0.4, damage_pct=-0.06, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Shinsu Density", "Your body holds more Shinsu and can take more.", "max_health", 2, "minecraft:heart_of_the_sea"),
        T("Output Pressure", "Every shot leaves your hand with more weight behind it.", "attack_damage", 1, "minecraft:blaze_rod"),
        T("Flow Speed", "Shinsu runs through your legs and you move lighter.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
    ],
    arts=[
        A("Shinsu Bolt", "Pack a bolt of blue Shinsu and fire it along your line of sight, punching through the first foes.", "ray_shot", "minecraft:arrow", vfx="LIGHTNING_SPEAR", damage=7, range=18, pierce=2),
        A("Palm Pressure Burst", "Slam Shinsu out of both palms in a ring that shoves everything near you away.", "ring_burst", "minecraft:wind_charge", vfx="WIND_RING", damage=5, radius=3.5),
        A("Needle Rain", "Loose a quick volley of thin Shinsu needles at the foe you are looking at.", "barrage", "minecraft:spectral_arrow", vfx="LIGHTNING_SPEAR", damage=3, shots=6),
        A("Fan Shot", "Spread Shinsu like a hand fan and blast a wide cone in front of you.", "cone_strike", "minecraft:feather", vfx="WIND_SLASH", damage=6, half_angle=55),
        A("Skipping Orb", "Throw an orb that hits one foe, then skips to the next nearest enemy.", "chain_hit", "minecraft:snowball", vfx="LIGHTNING_FIEND", damage=5, jumps=3),
        A("Pressure Drop", "Gather Shinsu overhead and let it fall as a crushing column on the spot you look at.", "meteor_drop", "minecraft:anvil", vfx="SPATIAL_RIFT", damage=11, radius=3.5),
        A("Overflow Guard", "Wrap yourself in a skin of excess Shinsu that soaks up damage.", "shield", "minecraft:shield", vfx="SPIRIT_AURA", absorb_hearts=4),
    ],
    passives=[
        P("Clean Shinsu Release", "Your shots leave your hand clean and sharp: sometimes they strike critically.", "crit", "minecraft:amethyst_shard", chance=0.12, mult=1.6),
        P("Rhythm of the Stream", "Land blow after blow and your Shinsu rhythm strengthens the following ones.", "combo", "minecraft:clock", hits=4, bonus=0.5),
        P("Steady Output", "Stay in the fight and your attacks grow stronger with every hit, until you stop.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=5),
        P("Numbing Pressure", "Your Shinsu bites down on the foe's limbs and may slow them.", "on_hit_effect", "minecraft:cobweb", effect="slowness", chance=0.22),
        P("Recirculation", "Your Shinsu slowly knits your wounds even outside battle.", "mending", "minecraft:glow_berries", seconds=18, amount=1),
        P("Tower Study", "Each foe you fell teaches you something: kills grant more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.1),
        P("Pressure Tolerance", "While you fight, the pressing weight of Shinsu around you hurts you less.", "reduce_when", "minecraft:chainmail_chestplate", condition="in_combat", pct=0.08),
    ],
    finishers=[
        A("Great Shinsu Cannon", "Charge all your Shinsu in one palm and fire a roaring beam that tears through a whole line of enemies.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=24, range=30, pierce=3, cost=0.06, cooldown=1500),
        A("Overflow Collapse", "Release every drop of Shinsu you hold in a huge ring that smashes everything around you.", "ring_burst", "minecraft:heavy_core", vfx="WIND_RING", damage=20, radius=6, targets=8, knockback=1.6, cost=0.07, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 2
LINES.append(Line(
    id="tog_shinsu_reinforcement", name="Shinsu Reinforcement", franchise=F,
    desc="Strengthen your body from the inside with Shinsu until fists, shoulders and skin are as hard as the Tower's floors. A wall that walks.",
    color="#90A4AE", background="iron", icon="minecraft:iron_chestplate",
    book_name="Treatise on the Reinforced Body", book_lore="Written by a Floor Guard who stopped counting the blows.",
    stances=[
        S("Stone Skin Stance", "Harden your Shinsu skin: heavy armor and knockback immunity, at the cost of speed.", "minecraft:stone", armor=5, knockback_res=0.5, speed_pct=-0.12, vfx="SPIRIT_AURA"),
        S("Pulse Fist Stance", "Push Shinsu into the arms: harder hits, a lighter hide.", "minecraft:iron_axe", damage_pct=0.18, armor=-2, vfx="WIND_RING"),
    ],
    stats=[
        T("Tempered Marrow", "Shinsu steeps your bones and raises your vitality.", "max_health", 2, "minecraft:bone"),
        T("Plated Hide", "A layer of Shinsu under the skin turns blows aside.", "armor", 1, "minecraft:iron_chestplate"),
        T("Rooted Footing", "You are hard to push around.", "knockback_resistance", 0.1, "minecraft:anvil"),
        T("Heavy Fist", "Your reinforced arms hit with real weight.", "attack_damage", 1, "minecraft:iron_axe"),
    ],
    arts=[
        A("Hardened Fist Strike", "Wrap a fist in dense Shinsu and uppercut the foe in front of you into the air.", "uppercut", "minecraft:iron_axe", vfx="WIND_SLASH", damage=7, launch=0.7),
        A("Bulwark of Shinsu", "Pull Shinsu over yourself in thick plates that absorb a heavy beating.", "shield", "minecraft:shield", vfx="SPIRIT_AURA", absorb_hearts=6, resist_amp=1),
        A("Provoking Pressure", "Release a wave of pressure that makes every hostile near you turn on you.", "taunt", "minecraft:goat_horn", vfx="DEVIL_CIRCLE", radius=9, targets=5),
        A("Earth Splitting Stomp", "Stomp a reinforced foot and send a cracking wave along the ground.", "ground_wave", "minecraft:iron_boots", vfx="EARTH_SPIKES", damage=6, length=9),
        A("Second Skin Recovery", "Let Shinsu flood your wounds and heal yourself with a short burst of regeneration.", "heal_self", "minecraft:golden_apple", vfx="SPIRIT_AURA", amount=7),
        A("Stillness Shock", "Slam both fists down; the shock freezes enemies around you in place for a moment.", "stun_ring", "minecraft:packed_ice", vfx="STONE_SPIKES", radius=4, seconds=2),
        A("Reinforced Cyclone", "Spin with armored limbs, hammering everything around you several times.", "whirlwind", "minecraft:mace", vfx="WIND_TORNADO", damage=3, hits=4),
    ],
    passives=[
        P("Shinsu Plating", "While you stay in the fight, damage against you is trimmed.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.1),
        P("Spiked Pressure", "Whoever hits you takes a share of the blow back through your Shinsu hide.", "thorns", "minecraft:iron_bars", pct=0.15),
        P("Burn Seal", "Your reinforced skin shrugs off fire.", "resist", "minecraft:fire_charge", damage_kind="fire", pct=0.2),
        P("Do Not Fall", "When you are nearly dead, Shinsu slams shut around you as a brief shield.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Resilient Metabolism", "Once the pressure is on, you heal slowly while fighting.", "regen_when", "minecraft:bread", condition="in_combat", per_second=0.5),
        P("Shoulder Check", "Your blows can shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
        P("Slow to Tire", "Reinforced muscle burns food slower.", "hunger_saver", "minecraft:cooked_beef", pct=0.15),
    ],
    finishers=[
        A("Fortress Body", "Become an immovable fortress for a long moment: a huge shield of absorption layered with resistance.", "shield", "minecraft:netherite_chestplate", vfx="SPIRIT_AURA", absorb_hearts=10, seconds=10, resist_amp=2, cost=0.05, cooldown=1600),
        A("Breaker Descent", "Leap high on reinforced legs and crash down so hard the ground ripples out in a ring.", "leap_slam", "minecraft:mace", vfx="EARTH_SPIKES", damage=24, radius=5, knockback=1.8, height=1.0, cost=0.06, cooldown=1500),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 3
LINES.append(Line(
    id="tog_reverse_flow_control", name="Reverse Flow Control", franchise=F,
    desc="Instead of pushing Shinsu out, you read the flow of the Shinsu around you and turn it back on whoever sent it. Counters, pulls and borrowed force.",
    color="#7E57C2", background="void", icon="minecraft:ender_eye",
    book_name="Notes on Reverse Flow", book_lore="The margin says: do not fight the current, hand it back.",
    stances=[
        S("Still Water Posture", "Stand loose and let blows slide past: tougher and calmer, but you hit lighter.", "minecraft:lapis_lazuli", armor=2, regen=0.5, damage_pct=-0.1, vfx="WATER_RING"),
        S("Backflow Posture", "Lean into the counter: your strikes grow sharp and critical, but you are easier to hurt.", "minecraft:ender_pearl", crit_chance=0.12, damage_pct=0.1, armor=-2, vfx="SPATIAL_RIFT"),
    ],
    stats=[
        T("Flow Sense", "You feel Shinsu moving in and out of every body near you.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Quiet Feet", "You glide between attacks with a lighter step.", "movement_speed", 0.05, "minecraft:leather_boots", op="mult"),
        T("Returned Pressure", "Redirected force sits in your arms as sharper blows.", "attack_damage", 1, "minecraft:ender_eye"),
    ],
    arts=[
        A("Backflow Wall", "For a few seconds a part of every blow you take is thrown back at the one who struck you.", "ward_reflect", "minecraft:glass_pane", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Drawing Current", "Reverse the flow around you and pull nearby foes in toward your hands.", "pull", "minecraft:lead", vfx="SPATIAL_RIFT", radius=8, strength=1.0, damage=3),
        A("Counter Palm", "A short palm strike that sends the foe in front of you flying upward on their own momentum.", "uppercut", "minecraft:ender_pearl", vfx="WIND_RING", damage=6, launch=0.8),
        A("Slip the Current", "Step sideways through the flow and reappear a few blocks ahead.", "blink", "minecraft:chorus_fruit", vfx="SPATIAL_RIFT", distance=9),
        A("Shinsu Jam", "Disturb the foes' own flow in a cone: it weakens them and slows them down.", "debuff_cone", "minecraft:fermented_spider_eye", vfx="SEAL_CHAINS", effect="weakness", effect2="slowness", seconds=7, seconds2=7),
        A("Tidal Return", "Turn the tide on the foes around you: a ring of redirected force that hits and throws them back.", "ring_burst", "minecraft:nautilus_shell", vfx="WATER_RING", damage=6, radius=4, knockback=1.2),
        A("Read the Opening", "Mark a foe whose flow you have read: it takes extra damage for a while.", "mark", "minecraft:compass", vfx="MAGIC_CIRCLE", bonus=0.3),
    ],
    passives=[
        P("Flow Reflex", "You may turn a blow completely aside before it lands.", "dodge", "minecraft:feather", chance=0.1),
        P("Riposte of the Current", "Whoever strikes you may be left slowed by the flow they disturbed.", "on_hurt_retaliate", "minecraft:string", effect="slowness", chance=0.25),
        P("Borrowed Weight", "Every blow you absorb sends a little back to the attacker.", "thorns", "minecraft:iron_nugget", pct=0.15),
        P("Opening Sense", "You hit much harder against nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.6),
        P("Calm Under Pressure", "While your health is full you strike with perfect rhythm and deal more damage.", "dmg_when", "minecraft:clock", condition="full_health", bonus=0.15),
        P("Weightless Fall", "You redirect your fall into the ground and take less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.4),
        P("Lowered Pressure", "Shinsu pressing in on you is bled away: you take less magic damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.2),
    ],
    finishers=[
        A("Whole Tower Backlash", "Turn every force aimed at you into a shield: for a while, half or more of every blow is thrown back at its sender.", "ward_reflect", "minecraft:ender_eye", vfx="MIRROR_PANE", seconds=9, reflect_pct=0.6, cost=0.05, cooldown=1700),
        A("Collapse of the Flow", "Reverse the whole flow around you at once: foes within range are dragged to your feet and crushed.", "pull", "minecraft:heavy_core", vfx="SPATIAL_RIFT", radius=11, strength=1.8, damage=20, targets=9, cost=0.07, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 4
LINES.append(Line(
    id="tog_fisherman", name="Fisherman", franchise=F,
    desc="The Fisherman Position casts glowing Shinsu lines and hooks to catch prey twice its size. You hook, mark and reel in whatever swims too close.",
    color="#26A69A", background="ocean", icon="minecraft:fishing_rod",
    book_name="The Angler's Ledger of the Tower", book_lore="Every hook in it is tied to a name and a number of lost sleeves.",
    stances=[
        S("Patient Angler Stance", "Wait for the bite: calm, slow, and hard to knock off your feet while you regain strength.", "minecraft:fishing_rod", regen=0.6, speed_pct=-0.08, knockback_res=0.3, vfx="WATER_RING"),
        S("Hooked Hunter Stance", "Stop waiting and strike: sharper and critical, but you leave your guard open.", "minecraft:tripwire_hook", damage_pct=0.15, crit_chance=0.1, armor=-1, vfx="THREAD_LINE"),
    ],
    stats=[
        T("Angler's Reach", "Your Shinsu line carries your hands further.", "entity_interaction_range", 0.5, "minecraft:lead"),
        T("Line Tension", "A tight line means a heavier blow.", "attack_damage", 1, "minecraft:string"),
        T("Steady Hands", "Hooking and reeling in comes quicker.", "attack_speed", 0.2, "minecraft:cod"),
        T("Deep Lungs", "A Fisherman who falls in the sea will come back up.", "max_health", 2, "minecraft:pufferfish"),
    ],
    arts=[
        A("Cast the Hook", "Throw a Shinsu hook at a foe far away and mark it: it takes extra damage while hooked.", "mark", "minecraft:tripwire_hook", vfx="THREAD_LINE", range=24, bonus=0.3),
        A("Reel In", "Haul the creatures around you toward yourself on glowing lines and bruise them on arrival.", "pull", "minecraft:fishing_rod", vfx="THREAD_LINE", radius=9, strength=1.1, damage=3),
        A("Bait Cloud", "Scatter shining bait in a cone: the foes in it start to glow and slow down.", "debuff_cone", "minecraft:glow_ink_sac", vfx="WATER_BURST", effect="glowing", effect2="slowness", seconds=7, seconds2=6),
        A("Harpoon Rush", "Lunge forward along a taut line and spear everything in your way.", "dash_strike", "minecraft:trident", vfx="WATER_BURST", damage=7, distance=7),
        A("Shinsu Net", "Cast a net of glowing line that freezes the creatures near you in place.", "stun_ring", "minecraft:cobweb", vfx="SEAL_CHAINS", radius=4.5, seconds=2),
        A("Barbed Lines", "Whip a barbed line that snags a foe and then leaps from enemy to enemy.", "chain_hit", "minecraft:iron_nugget", vfx="THREAD_LINE", damage=5, jumps=3),
        A("Floating Lures", "Send a spray of glowing lures darting at the foe you are looking at.", "barrage", "minecraft:salmon", vfx="WATER_BURST", damage=3, shots=6),
        A("Piranha Bite", "Strike a foe like a school of piranhas and heal from the bite.", "drain_bite", "minecraft:tropical_fish", vfx="WATER_RING", damage=6, heal_pct=0.3),
    ],
    passives=[
        P("Good Catch", "Whatever you fell may drop an extra stack of its loot.", "drop_bonus", "minecraft:salmon", chance=0.1),
        P("Deep Water Predator", "You hit sea and monster prey harder.", "dmg_vs", "minecraft:prismarine_shard", category="monster", bonus=0.15),
        P("Born on the Water", "While you stand in water you move much faster.", "self_effect_when", "minecraft:heart_of_the_sea", effect="speed", condition="in_water"),
        P("Salt Recovery", "In water your wounds close slowly.", "regen_when", "minecraft:kelp", condition="in_water", per_second=0.6),
        P("Bait and Wait", "A Fisherman learns to go long without a meal.", "hunger_saver", "minecraft:cooked_cod", pct=0.2),
        P("Hook in the Flesh", "Your blows leave a hook behind that may weaken the foe.", "on_hit_effect", "minecraft:iron_nugget", effect="weakness", chance=0.2),
        P("Fresh Haul", "Every kill is a catch: it heals you a little.", "kill_reward", "minecraft:cooked_salmon", heal=3, effect="none"),
    ],
    finishers=[
        A("The Great Catch", "Hook a single foe with a huge Shinsu line and pull the entire weight of the sea down on it, ending weak enemies outright.", "execute", "minecraft:fishing_rod", vfx="THREAD_LINE", damage=24, bonus=1.6, cost=0.06, cooldown=1500),
        A("Leviathan Tide", "Call a huge sea beast of Shinsu down from above onto the place you look at.", "meteor_drop", "minecraft:heart_of_the_sea", vfx="WATER_BURST", damage=26, radius=5, range=24, cost=0.08, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 5
LINES.append(Line(
    id="tog_spear_bearer", name="Spear Bearer", franchise=F,
    desc="The Spear Bearer Position fights at the end of a long shaft of Shinsu-infused metal. Straight thrusts, vaulting lunges and one killing point.",
    color="#FF7043", background="storm", icon="minecraft:trident",
    book_name="Forty Lessons on the Spear", book_lore="Thrust is a sentence; the last lesson is the full stop.",
    stances=[
        S("Spear Thrust Stance", "Weight forward, shaft level: faster and stronger, but you never block.", "minecraft:trident", damage_pct=0.12, speed_pct=0.04, armor=-1, vfx="WIND_SLASH"),
        S("Guarded Shaft Stance", "Hold the spear crosswise to cover yourself: sturdy, hard to push, a little weaker.", "minecraft:shield", armor=3, knockback_res=0.3, damage_pct=-0.05, vfx="WIND_RING"),
    ],
    stats=[
        T("Shaft Strength", "Years behind a spear thicken your arms.", "attack_damage", 1, "minecraft:stick"),
        T("Long Reach", "A Bearer fights from the far end of the weapon.", "entity_interaction_range", 0.5, "minecraft:lead"),
        T("Hunter's Stride", "You close on prey with a lighter step.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Iron Grip", "Nothing knocks the weapon from your hands: tougher armor.", "armor_toughness", 1, "minecraft:iron_ingot"),
    ],
    arts=[
        A("Piercing Thrust", "Drive the spear point straight ahead; the thrust skewers every foe in a line.", "ray_shot", "minecraft:trident", vfx="LIGHTNING_SPEAR", damage=8, range=10, pierce=3),
        A("Charging Lance", "Charge in a straight line with the spear leveled, running through whoever is in the way.", "dash_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=7, distance=8),
        A("Sweeping Shaft", "Swing the long shaft in an arc and sweep every foe in front of you.", "cone_strike", "minecraft:stick", vfx="WIND_SLASH", damage=6, half_angle=55, range=4.2),
        A("Vaulting Lunge", "Plant the shaft, vault up and drive down on the foes below.", "leap_slam", "minecraft:blaze_rod", vfx="LIGHTNING_SPEAR", damage=8, height=0.9),
        A("Thousand Points", "Stab so fast the air fills with spear tips: a rapid volley of thrusts at one target.", "barrage", "minecraft:iron_nugget", vfx="LIGHTNING_SPEAR", damage=3, shots=7),
        A("Windmill Spin", "Spin the spear overhead and cut every foe around you several times.", "whirlwind", "minecraft:iron_axe", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Killing Point", "Aim for the gap in the guard: this thrust hits much harder against weakened foes.", "execute", "minecraft:diamond_sword", vfx="LIGHTNING_SPEAR", damage=9, bonus=0.8),
    ],
    passives=[
        P("Giant Hunter", "Spear Bearers train to fell the Tower's great beasts: you hit bosses harder.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.15),
        P("Point Placement", "Your thrusts find the weak seams: critical hits are more likely.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Rushing Spear", "You deal more damage while sprinting, charging at the foe.", "sprint_bonus", "minecraft:sugar", bonus=0.2),
        P("Thrust Rhythm", "Every few hits in a row land far harder.", "combo", "minecraft:clock", hits=4, bonus=0.6),
        P("Finishing Point", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.7),
        P("Blood on the Shaft", "Each hit returns a little health.", "lifesteal", "minecraft:redstone", pct=0.05),
        P("Chain of Thrusts", "The longer a fight runs, the more your thrusts speed up in power.", "momentum", "minecraft:blaze_powder", per_stack=0.04, max_stacks=6),
    ],
    finishers=[
        A("Lurker Skewer", "Hurl all your Shinsu through the spear in one thrust that punches through the whole line of foes ahead.", "ray_shot", "minecraft:trident", vfx="LIGHTNING_SPEAR", damage=28, range=28, pierce=4, cost=0.06, cooldown=1600),
        A("Skybreaker Lance", "Throw the spear into the sky and call it down as a bolt on the place you look at.", "meteor_drop", "minecraft:lightning_rod", vfx="LIGHTNING_FIEND", damage=28, radius=4.5, cost=0.08, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 6
LINES.append(Line(
    id="tog_light_bearer", name="Light Bearer", franchise=F,
    desc="The Light Bearer carries a lamp of Shinsu through the dark floors: light that guides allies, mends them and burns the foes who fear it.",
    color="#FFD54F", background="holy", icon="minecraft:lantern",
    book_name="Lamp-Lighter's Handbook", book_lore="Wick trimmed, oil of Shinsu, one match; do not blink.",
    stances=[
        S("Beacon Posture", "Plant yourself like a lamp post: you regenerate steadily and stand a little firmer, but move slower.", "minecraft:lantern", regen=0.8, armor=1, speed_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Dawnglare Posture", "Turn the lamp into a lance of light: sharper strikes, but nothing shields you.", "minecraft:glowstone_dust", damage_pct=0.12, crit_chance=0.08, armor=-1, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Lantern Heart", "A warm light inside you keeps more life burning.", "max_health", 2, "minecraft:glowstone"),
        T("Fortune of the Lit Path", "Where you walk the light is kinder.", "luck", 1, "minecraft:gold_nugget"),
        T("Bright Step", "You walk lightly along your own light.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
        T("Burning Wick", "Light hits as hard as a blade.", "attack_damage", 1, "minecraft:torch"),
    ],
    arts=[
        A("Lamp Blessing", "Raise your lamp: you and your allies near you start to regenerate in the glow.", "buff_aura", "minecraft:lantern", vfx="SPIRIT_AURA", effect="regeneration", radius=8, seconds=10),
        A("Healing Glow", "A warm pulse of light that heals you and the allies around you.", "heal_pulse", "minecraft:glistering_melon_slice", vfx="SPIRIT_AURA", radius=7, amount=6),
        A("Blinding Flash", "Flare the lamp in a cone: everything caught in it is blinded and lit up for others to see.", "debuff_cone", "minecraft:glowstone_dust", vfx="MAGIC_CIRCLE", effect="blindness", effect2="glowing", seconds=5, seconds2=8),
        A("Lance of Light", "Fire a narrow beam of light down your line of sight that pierces the first foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=7, range=18, pierce=2),
        A("Path Illumination", "Light the way ahead: you gain swiftness and keen eyes for a while.", "buff_self", "minecraft:spyglass", vfx="SPIRIT_AURA", effect="speed", effect2="night_vision", seconds=14),
        A("Prism Barrier", "A shell of refracted light surrounds you and absorbs damage.", "shield", "minecraft:amethyst_shard", vfx="MAGIC_CIRCLE", absorb_hearts=4),
        A("Daybreak Burst", "A ring of white light that hits everything around you and pushes it away.", "ring_burst", "minecraft:sunflower", vfx="SPIRIT_AURA", damage=5, radius=4),
    ],
    passives=[
        P("Glow of Company", "Allies near you slowly regenerate in your light.", "aura", "minecraft:lantern", effect="regeneration", target="allies", radius=7),
        P("Daylight Mending", "In the day your wounds close by themselves.", "regen_when", "minecraft:sunflower", condition="day", per_second=0.5),
        P("Lamp Eyes", "At night you see in the dark.", "self_effect_when", "minecraft:golden_carrot", effect="night_vision", condition="night"),
        P("Wick Mending", "Light keeps repairing you even after the battle.", "mending", "minecraft:honey_bottle", seconds=18, amount=1),
        P("Anathema of the Dead", "The undead flinch from your light and take much more damage.", "dmg_vs", "minecraft:wither_skeleton_skull", category="undead", bonus=0.2),
        P("Lit Victory", "Each kill returns a little life and a short burst of speed.", "kill_reward", "minecraft:golden_apple", heal=3, effect="speed", seconds=4),
        P("Warding Glow", "The light dulls magical attacks against you.", "resist", "minecraft:glow_berries", damage_kind="magic", pct=0.2),
    ],
    finishers=[
        A("Beacon of the Tower", "Raise the lamp high: a vast wave of light heals you and every ally around you.", "heal_pulse", "minecraft:beacon", vfx="SPIRIT_AURA", radius=12, amount=18, cost=0.08, cooldown=1700),
        A("Dawnbreak Lance", "The lamp becomes a spear of white fire and rips through a long line of foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", damage=26, range=30, pierce=4, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 7
LINES.append(Line(
    id="tog_scout", name="Scout", franchise=F,
    desc="The Scout Position is eyes, ears and knife: quick on the stairs, quiet in the halls, and hard to find once it has marked you.",
    color="#66BB6A", background="city", icon="minecraft:spyglass",
    book_name="A Scout's Field Notes", book_lore="Three pages of maps and one line: never be where they look.",
    stances=[
        S("Silent Step Stance", "Walk on the toes: much faster, but a single blow hurts and you hit lighter.", "minecraft:leather_boots", speed_pct=0.12, armor=-1, damage_pct=-0.05, vfx="SHADOW_POOL"),
        S("Ambush Stance", "Crouch in wait: your first strikes crit often, at the price of speed.", "minecraft:iron_sword", crit_chance=0.15, damage_pct=0.08, speed_pct=-0.05, vfx="SHADOW_POOL"),
    ],
    stats=[
        T("Light Step", "You cover ground quickly.", "movement_speed", 0.05, "minecraft:rabbit_foot", op="mult"),
        T("Far Sight", "A Scout sees and strikes a little further out.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Quick Hands", "Your blade is out before they notice.", "attack_speed", 0.2, "minecraft:iron_nugget"),
        T("Soft Landing", "Falling off a stair is a Scout's daily habit.", "safe_fall_distance", 1.5, "minecraft:slime_ball"),
    ],
    arts=[
        A("Shadow Mark", "Tag a foe from a distance: it takes extra damage while marked.", "mark", "minecraft:ender_eye", vfx="SHADOW_POOL", bonus=0.3, range=26),
        A("Veil Step", "Step into the shadows and come out a few blocks ahead.", "blink", "minecraft:ender_pearl", vfx="SHADOW_POOL", distance=10),
        A("Dust Cloud", "Throw a cloud of Tower dust: foes near you are blinded while you slip away.", "smoke", "minecraft:gunpowder", vfx="ASH_FORMATION", radius=5, seconds=6),
        A("Knife Rush", "Dart forward in a blur and cut everything between you and your goal.", "dash_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=6, distance=8),
        A("Hamstring Cut", "Cut the legs of the foes in front of you in a cone: they move slowly and hit weakly.", "debuff_cone", "minecraft:flint", vfx="WIND_SLASH", effect="slowness", effect2="weakness", seconds=6, seconds2=6),
        A("Back Alley Stab", "Slip behind a weakened foe and strike where it hurts: massive damage against low health.", "execute", "minecraft:iron_sword", vfx="SHADOW_POOL", damage=8, bonus=0.9),
        A("Dart Flurry", "Flick a handful of needles at the foe you are looking at.", "barrage", "minecraft:arrow", vfx="WIND_SLASH", damage=3, shots=6),
    ],
    passives=[
        P("Slip the Blow", "A trained Scout may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.1),
        P("Runner's Edge", "You hit harder when you are sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.2),
        P("Shadow Hush", "While you crouch you slip out of sight.", "self_effect_when", "minecraft:fermented_spider_eye", effect="invisibility", condition="sneaking"),
        P("Roofline Reflex", "A Scout lands softly: less fall damage.", "fall_guard", "minecraft:leather", pct=0.4),
        P("Weak Point Eye", "You strike critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Opportunist", "Crouching in wait makes your first strikes bite harder.", "dmg_when", "minecraft:iron_sword", condition="sneaking", bonus=0.2),
        P("Pickpocket", "A kill may leave an extra drop in your hands.", "drop_bonus", "minecraft:gold_nugget", chance=0.1),
    ],
    finishers=[
        A("Vanishing Strike", "Vanish in a trail of shadow and reappear through the line of foes, slashing every one.", "dash_strike", "minecraft:netherite_sword", vfx="SHADOW_POOL", damage=24, distance=11, targets=6, cost=0.06, cooldown=1500),
        A("Assassin's Silence", "One perfect strike on a single weakened foe: the lower its health, the worse the blow.", "execute", "minecraft:diamond_sword", vfx="SHADOW_POOL", damage=26, bonus=2.0, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 8
LINES.append(Line(
    id="tog_wave_controller", name="Wave Controller", franchise=F,
    desc="A Wave Controller bends Shinsu into waves and uses their rhythm as a weapon. Rolling shocks, resonance and the heavy crest of a great breaker.",
    color="#29B6F6", background="storm", icon="minecraft:conduit",
    book_name="Waveform Manual", book_lore="The footnotes are all about timing; the main text is about not drowning.",
    stances=[
        S("Rising Wave Stance", "Swell with the wave: faster and stronger, but your hide is thin.", "minecraft:prismarine_crystals", damage_pct=0.14, speed_pct=0.05, armor=-2, vfx="WATER_RING"),
        S("Calm Sea Stance", "Lay yourself flat like a calm sea: regeneration and a firm stand.", "minecraft:lapis_lazuli", armor=2, regen=0.5, knockback_res=0.2, vfx="WATER_RING"),
    ],
    stats=[
        T("Wave Pressure", "Your Shinsu waves have more weight.", "attack_damage", 1, "minecraft:prismarine_shard"),
        T("Tide Lung", "Your body rides the pressure better.", "max_health", 2, "minecraft:nautilus_shell"),
        T("Surge Footwork", "You move with the swell.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Ripple Sense", "You feel waves through the ground from afar.", "entity_interaction_range", 0.4, "minecraft:sculk_sensor"),
    ],
    arts=[
        A("Shock Wave", "Send a rolling wave along the ground in front of you that knocks everything over.", "ground_wave", "minecraft:prismarine_crystals", vfx="WATER_BURST", damage=6, length=10, width=2.5),
        A("Tidal Ring", "A ring of water-like Shinsu spreads out from your feet and throws the foes back.", "ring_burst", "minecraft:heart_of_the_sea", vfx="WATER_RING", damage=5, radius=4),
        A("Crashing Breaker", "Slam a breaking wave into the cone in front of you.", "cone_strike", "minecraft:nautilus_shell", vfx="WATER_BURST", damage=7, half_angle=55),
        A("Resonance Chain", "Strike one foe; the wave resonates and jumps to the next ones nearby.", "chain_hit", "minecraft:amethyst_shard", vfx="LIGHTNING_FIEND", damage=5, jumps=4),
        A("Wave Ride", "Surf forward on a wave of Shinsu and break whatever it meets.", "dash_strike", "minecraft:prismarine_shard", vfx="WATER_BURST", damage=6, distance=8),
        A("Undertow", "Drag the creatures around you toward yourself on a hidden current.", "pull", "minecraft:kelp", vfx="WATER_RING", radius=8, strength=1.0),
        A("Sound Wave Stun", "Strike the air so hard that everything near you is stunned in place.", "stun_ring", "minecraft:bell", vfx="WIND_RING", radius=4, seconds=2),
    ],
    passives=[
        P("Ride the Swell", "The longer you fight the more the wave builds behind your blows.", "momentum", "minecraft:prismarine_crystals", per_stack=0.04, max_stacks=6),
        P("Crest Push", "Your blows may shove the foe a long way back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.3),
        P("Water Hum", "In water you recover steadily.", "regen_when", "minecraft:kelp", condition="in_water", per_second=0.6),
        P("Numbing Foam", "Your waves may leave the foe nauseous.", "on_hit_effect", "minecraft:fermented_spider_eye", effect="nausea", chance=0.2),
        P("Ground Static", "An aura around you slows enemies that come close.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
        P("Storm Insulation", "The wave carries lightning away from you.", "resist", "minecraft:lightning_rod", damage_kind="lightning", pct=0.25),
        P("Wave Rhythm", "Strike in time with the wave: every few hits land much harder.", "combo", "minecraft:clock", hits=4, bonus=0.5),
    ],
    finishers=[
        A("Tsunami Break", "Gather an enormous wall of Shinsu behind you and release it along the ground to flatten the lane ahead.", "ground_wave", "minecraft:heart_of_the_sea", vfx="WATER_BURST", damage=26, length=20, width=4, knockback=1.6, cost=0.06, cooldown=1700),
        A("Resonant Maelstrom", "Pull the waves around you into a spinning ring that batters everything inside it over and over.", "whirlwind", "minecraft:conduit", vfx="WATER_RING", damage=5, hits=8, radius=5, cost=0.08, cooldown=1800),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 9
LINES.append(Line(
    id="tog_floor_guard", name="Floor Guard", franchise=F,
    desc="A Floor Guard stands at a staircase and lets nothing pass. Heavy, loud, and very hard to get around: you hold the line for everyone behind you.",
    color="#8D6E63", background="ruin", icon="minecraft:shield",
    book_name="Orders of the Staircase", book_lore="The first order is: you do not leave your post. There is no second order.",
    stances=[
        S("Gate Post Stance", "Plant yourself at the stair: very heavy armor and immune to shoves, but slow.", "minecraft:iron_bars", armor=4, knockback_res=0.6, speed_pct=-0.15, vfx="SPIRIT_AURA"),
        S("Patrol Stance", "Walk the corridor: quicker, steadier, a little more dangerous.", "minecraft:compass", speed_pct=0.06, regen=0.3, damage_pct=0.05, vfx="WIND_RING"),
    ],
    stats=[
        T("Guard's Vigor", "Long watches harden your health.", "max_health", 2, "minecraft:cooked_beef"),
        T("Gate Armor", "Plate and Shinsu on the stair.", "armor", 1, "minecraft:iron_chestplate"),
        T("Toughened Gaze", "Not even a sharp blow gets through the full plate.", "armor_toughness", 1, "minecraft:netherite_ingot"),
        T("Sentry Reach", "You hold a longer arm's length.", "entity_interaction_range", 0.3, "minecraft:lead"),
    ],
    arts=[
        A("Halt!", "Slam your weapon and a pulse of Shinsu freezes the creatures around you in place.", "stun_ring", "minecraft:bell", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Challenge Roar", "Bellow a Guard's challenge: hostile creatures near you turn on you.", "taunt", "minecraft:goat_horn", vfx="DEVIL_CIRCLE", radius=9, targets=5),
        A("Barrier Wall", "Raise a wall of your own Shinsu around you that absorbs a lot of damage.", "shield", "minecraft:shield", vfx="SPIRIT_AURA", absorb_hearts=6, resist_amp=1),
        A("Shield Bash", "Rush forward behind the shield and slam everything ahead of you.", "dash_strike", "minecraft:iron_ingot", vfx="WIND_RING", damage=6, distance=5),
        A("Gate Slam", "Bring the weapon down and send a shock wave along the floor in front of you.", "ground_wave", "minecraft:anvil", vfx="STONE_SPIKES", damage=6, length=9),
        A("Intruder Mark", "Brand a trespasser: it takes more damage from everything while marked.", "mark", "minecraft:name_tag", vfx="MAGIC_CIRCLE", bonus=0.3),
        A("Guard's Counter", "Raise your guard: part of every blow you take is returned to the attacker for a while.", "ward_reflect", "minecraft:iron_chestplate", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Watch Mending", "A Guard keeps the ranks fit: heal yourself and the allies around you.", "heal_pulse", "minecraft:bread", vfx="SPIRIT_AURA", radius=6, amount=5),
    ],
    passives=[
        P("Gate Discipline", "While you are in a fight you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.1),
        P("Spiked Pauldrons", "Anyone who hits you takes part of the damage back.", "thorns", "minecraft:iron_bars", pct=0.18),
        P("Hold the Line", "When almost dead, a Guard raises a short shield and holds on.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Blast Cover", "Your plate absorbs blasts: less explosion damage.", "resist", "minecraft:tnt", damage_kind="explosion", pct=0.3),
        P("Trespasser Hunter", "You hit other players who slip by you harder.", "dmg_vs", "minecraft:player_head", category="player", bonus=0.15),
        P("Watch Rations", "A Guard stands for hours: you tire slower.", "hunger_saver", "minecraft:bread", pct=0.2),
        P("Answer in Kind", "Whoever hits you may be left weakened by the blow.", "on_hurt_retaliate", "minecraft:fermented_spider_eye", effect="weakness", chance=0.25),
    ],
    finishers=[
        A("Floor Lockdown", "Seal the whole staircase: every creature around you is held in place for a long moment.", "stun_ring", "minecraft:bell", vfx="SEAL_CHAINS", radius=9, seconds=5, targets=12, cost=0.08, cooldown=1900),
        A("Guardian's Verdict", "Leap up and drive your whole weight into the ground: a crater and a ring of broken stone.", "leap_slam", "minecraft:anvil", vfx="STONE_SPIKES", damage=26, radius=5, knockback=1.8, cost=0.06, cooldown=1600),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 10
LINES.append(Line(
    id="tog_inventory", name="Inventory", franchise=F,
    desc="The Inventory Position keeps a pocket of folded space and pulls weapons, supplies and surprises out of it. Blink, volley, and drop what you have stored on your enemies.",
    color="#AB47BC", background="stellar", icon="minecraft:ender_chest",
    book_name="Catalogue of a Folded Pocket", book_lore="Item 1: this book. Item 2: the pocket it is in. Item 3 is the problem.",
    stances=[
        S("Open Pocket Stance", "Keep the pocket open for quick draws: faster and sharper, but you carry less on your body.", "minecraft:barrel", speed_pct=0.05, crit_chance=0.06, armor=-1, vfx="SPATIAL_RIFT"),
        S("Sealed Pocket Stance", "Seal the pocket and wrap the extra Shinsu round yourself: firm and steady.", "minecraft:ender_chest", armor=3, regen=0.3, damage_pct=-0.05, vfx="SPATIAL_RIFT"),
    ],
    stats=[
        T("Deep Pockets", "The odds fall your way when you reach into the pocket.", "luck", 1, "minecraft:emerald"),
        T("Spatial Carry", "You reach blocks from further away.", "block_interaction_range", 0.5, "minecraft:chest"),
        T("Packed Vitality", "Stored Shinsu slowly turns into body.", "max_health", 2, "minecraft:golden_apple"),
        T("Quick Draw", "Whatever you need is in your hand before you finish thinking.", "attack_speed", 0.2, "minecraft:iron_nugget"),
    ],
    arts=[
        A("Withdraw Volley", "Pull a dozen stored blades from the pocket and fire them at the foe you are looking at.", "barrage", "minecraft:iron_sword", vfx="WEAPON_CONSTRUCTS", damage=3, shots=7),
        A("Stash Step", "Fold the space ahead of you: you step into the pocket and out a few blocks forward.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=10),
        A("Drop the Anvil", "Pull the heaviest thing out of your pocket and drop it onto the place you look at.", "meteor_drop", "minecraft:anvil", vfx="SPATIAL_RIFT", damage=11, radius=3.5),
        A("Pocket Vacuum", "Open a hole in space in front of you and suck the foes around you toward it.", "pull", "minecraft:ender_eye", vfx="SPATIAL_RIFT", radius=8, strength=1.0),
        A("Spare Armor", "Pull a layer of armor from storage: a strong shield of absorption.", "shield", "minecraft:iron_chestplate", vfx="WEAPON_CONSTRUCTS", absorb_hearts=5),
        A("Ration Pack", "Take bandages and rations from the pocket and heal yourself.", "heal_self", "minecraft:honey_bottle", vfx="SPIRIT_AURA", amount=7),
        A("Rift Cut", "Slice space in front of you with a blade out of the pocket: everyone in the cone is cut.", "cone_strike", "minecraft:netherite_sword", vfx="SPATIAL_RIFT", damage=7, half_angle=50),
    ],
    passives=[
        P("Sorted Loot", "Whatever you defeat may drop an extra stack of its loot.", "drop_bonus", "minecraft:chest", chance=0.1),
        P("Appraiser's Eye", "Kills pay out more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.12),
        P("Lucky Pocket", "In daylight, fortune follows your pocket.", "self_effect_when", "minecraft:rabbit_foot", effect="luck", condition="day"),
        P("Salvage", "Each kill refills you a little.", "kill_reward", "minecraft:golden_carrot", heal=3, effect="none"),
        P("Self-Repair Kit", "A repair kit in the pocket keeps your wounds closing.", "mending", "minecraft:honeycomb", seconds=18, amount=1),
        P("Folded Landing", "You land softly in the fold of the pocket and take less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.4),
        P("Out of Reach", "Space buckles around you and you may dodge a blow completely.", "dodge", "minecraft:ender_pearl", chance=0.1),
    ],
    finishers=[
        A("Armory Release", "Empty the whole armory at once: a storm of weapons pours out and hits the foe you look at.", "barrage", "minecraft:netherite_sword", vfx="WEAPON_CONSTRUCTS", damage=5, shots=12, range=18, cost=0.08, cooldown=1800),
        A("Pocket Dimension Fold", "Fold the space around you into your pocket for a moment: everything near you is held in stasis.", "stun_ring", "minecraft:ender_chest", vfx="SPATIAL_RIFT", radius=8, seconds=4, targets=10, cost=0.08, cooldown=1900),
    ],
))
