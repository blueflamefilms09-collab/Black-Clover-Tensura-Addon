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

# ---------------------------------------------------------------------------------------------------------------- 11
LINES.append(Line(
    id="tog_guide", name="Guide", franchise=F,
    desc="A Guide leads a team of climbers up the Tower: finds the route, keeps everyone fast and fed, and knows exactly where the beast is weak.",
    color="#FFB74D", background="dream", icon="minecraft:compass",
    book_name="The Guide's Pocket Map", book_lore="Half of it is a map of the Tower, the other half is who was supposed to be on it.",
    stances=[
        S("Lead the Way Stance", "Walk at the front and set the pace: quicker and steadier, a little less dangerous.", "minecraft:compass", speed_pct=0.06, regen=0.3, damage_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Shepherd's Vigil Stance", "Fall back to the rear and watch the group: tougher and regenerating, but slower.", "minecraft:shield", armor=2, regen=0.5, speed_pct=-0.05, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Navigator's Stride", "A Guide never hurries and is never late.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Cool Head", "Nerves of a person who has done this a hundred times.", "max_health", 2, "minecraft:golden_carrot"),
        T("Knowing Eye", "You see the target a little farther out.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Lucky Route", "The path you pick tends to be the good one.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Chart the Route", "Show your group the way: you and the allies near you gain swiftness.", "buff_aura", "minecraft:map", vfx="SPIRIT_AURA", effect="speed", radius=9, seconds=12),
        A("Rest Stop", "Call a short halt: you and the allies around you are healed.", "heal_pulse", "minecraft:bread", vfx="SPIRIT_AURA", radius=7, amount=6),
        A("Point Out the Weakness", "Call out a foe's weak spot: it takes extra damage from everyone while marked.", "mark", "minecraft:spyglass", vfx="MAGIC_CIRCLE", bonus=0.3, range=28),
        A("Rally Cry", "A short shout that makes you and your allies around you hit harder.", "buff_aura", "minecraft:goat_horn", vfx="SPIRIT_AURA", effect="strength", radius=8, seconds=10),
        A("Shortcut", "You know a shortcut: step forward a good distance in an instant.", "blink", "minecraft:chorus_fruit", vfx="SPATIAL_RIFT", distance=10),
        A("Lose the Hunters", "Throw a smoke pot and the creatures near you lose track of you.", "smoke", "minecraft:gunpowder", vfx="ASH_FORMATION", radius=5, seconds=6),
        A("Draw the Beast Off", "Make yourself the target so the rest of the team can walk away.", "taunt", "minecraft:lead", vfx="DEVIL_CIRCLE", radius=9, targets=5),
    ],
    passives=[
        P("Pathfinder's Aura", "Allies close to you walk a little faster.", "aura", "minecraft:sugar", effect="speed", target="allies", radius=7),
        P("Lessons of Many Floors", "You gain more experience from each kill.", "xp_bonus", "minecraft:experience_bottle", pct=0.12),
        P("Easy Pace", "While you crouch and take a breather you slowly heal.", "regen_when", "minecraft:glow_berries", condition="sneaking", per_second=0.5),
        P("Packed Rations", "You tire slowly on long ascents.", "hunger_saver", "minecraft:cooked_beef", pct=0.2),
        P("Read the Strike", "You may step out of the way of a blow.", "dodge", "minecraft:feather", chance=0.1),
        P("First Aid", "Your wounds mend slowly over time.", "mending", "minecraft:honey_bottle", seconds=18, amount=1),
        P("Sure Footing", "Falling a few floors is routine; less fall damage.", "fall_guard", "minecraft:slime_ball", pct=0.4),
    ],
    finishers=[
        A("Hold the Stairs Together", "Call the whole team around you: you and your allies gain a wall of resistance for a long while.", "buff_aura", "minecraft:beacon", vfx="SPIRIT_AURA", effect="resistance", amp=1, radius=12, seconds=22, cost=0.08, cooldown=1900),
        A("Exit Route", "The Guide's final trick: step through a hidden door and appear far away.", "blink", "minecraft:ender_eye", vfx="SPATIAL_RIFT", distance=22, cost=0.05, cooldown=1500),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 12
LINES.append(Line(
    id="tog_regular_climber", name="Regular", franchise=F,
    desc="A plain Regular, with no special blood and no special weapon, climbing on stubbornness. You survive what should have killed you and strike when it counts.",
    color="#A1887F", background="sand", icon="minecraft:leather_boots",
    book_name="Regular's Survival Notes", book_lore="Scribbled in the margins: it is not about winning, it is about being still standing at the end.",
    stances=[
        S("Survivor Stance", "Keep your head down and live: regeneration and some armor, less damage dealt.", "minecraft:leather_chestplate", armor=2, regen=0.6, damage_pct=-0.05, vfx="SPIRIT_AURA"),
        S("All-In Stance", "Throw everything at it: very strong and a little fast, but you expose yourself completely.", "minecraft:iron_sword", damage_pct=0.2, armor=-3, speed_pct=0.05, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Stubborn Body", "Some people just refuse to fall.", "max_health", 2, "minecraft:bread"),
        T("Climber's Calves", "Years of stairs make for a good jump.", "jump_strength", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Cold Nerves", "You keep a clear head while being hit.", "armor_toughness", 1, "minecraft:iron_ingot"),
        T("Test Hands", "Quick fingers for every test the Tower throws.", "attack_speed", 0.2, "minecraft:iron_nugget"),
    ],
    arts=[
        A("Desperate Strike", "Hit with everything you have: much stronger the weaker your opponent is.", "execute", "minecraft:iron_sword", vfx="WIND_SLASH", damage=8, bonus=0.8),
        A("Bandage and Breathe", "Take a moment in the middle of a fight: heal and regenerate.", "heal_self", "minecraft:honeycomb", vfx="SPIRIT_AURA", amount=7),
        A("Scramble", "Run for it: you gain a burst of speed and a burst of jump height.", "buff_self", "minecraft:rabbit_foot", vfx="WIND_RING", effect="speed", effect2="jump_boost", seconds=12),
        A("Dirty Uppercut", "Throw a fist at the foe in front of you and launch it into the air.", "uppercut", "minecraft:iron_axe", vfx="WIND_SLASH", damage=7),
        A("Lucky Bounce", "Throw something at a foe and have it bounce off to the next one.", "chain_hit", "minecraft:snowball", vfx="WIND_SLASH", damage=5, jumps=3),
        A("Stair Jump", "Spring up and land on the foes below with everything you have.", "leap_slam", "minecraft:leather_boots", vfx="WIND_RING", damage=7, height=0.9),
        A("Stir Up the Dust", "Kick up a cloud of dust: foes close to you cannot see where you went.", "smoke", "minecraft:sand", vfx="ASH_FORMATION", radius=5, seconds=5),
    ],
    passives=[
        P("Not Dead Yet", "When you are nearly dead, you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Second Wind", "When wounded you recover health continuously.", "regen_when", "minecraft:golden_apple", condition="low_health", per_second=1.0),
        P("Scab Over", "Your wounds close slowly all the time.", "mending", "minecraft:honey_bottle", seconds=16, amount=1),
        P("Back to the Wall", "The closer to death you are, the less you take.", "reduce_when", "minecraft:chainmail_chestplate", condition="low_health", pct=0.15),
        P("Cornered Rat", "A cornered Regular bites hard: more damage when wounded.", "dmg_when", "minecraft:iron_sword", condition="low_health", bonus=0.25),
        P("Eat Anything", "You can go a long time between meals.", "hunger_saver", "minecraft:rotten_flesh", pct=0.25),
        P("Survivor's Spoils", "Every kill restores a little health.", "kill_reward", "minecraft:apple", heal=3, effect="none"),
        P("School of Hard Floors", "Every foe you beat teaches you something: more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.12),
    ],
    finishers=[
        A("Never Give Up", "Refuse to die: a huge heal and a long regeneration right when you need it.", "heal_self", "minecraft:enchanted_golden_apple", vfx="SPIRIT_AURA", amount=24, regen_seconds=12, cost=0.05, cooldown=1600),
        A("All or Nothing", "A single, ruinous blow aimed at one foe; it hits far harder the less health that foe has.", "execute", "minecraft:netherite_sword", vfx="FIRE_BURST", damage=26, bonus=2.2, cost=0.06, cooldown=1500),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 13
LINES.append(Line(
    id="tog_irregular", name="Irregular", franchise=F,
    desc="An Irregular enters the Tower through a door that was never opened for anyone, with a power nobody has measured. Wild leaps, sudden surges and a stubborn refusal to be explained.",
    color="#D32F2F", background="blood", icon="minecraft:nether_star",
    book_name="Record of an Unlisted Entrant", book_lore="The examiners put a question mark where the entry's rank should be.",
    stances=[
        S("Wild Surge Stance", "Let the unknown power run: huge damage and sharper crits, nothing guarding you.", "minecraft:blaze_powder", damage_pct=0.2, crit_chance=0.08, armor=-3, vfx="FIRE_BURST"),
        S("Eye of the Storm Stance", "Go calm, completely: sturdier, regenerating, but slower to act.", "minecraft:echo_shard", armor=3, regen=0.6, speed_pct=-0.08, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Unmeasured Reserves", "Nobody knows how much you can take.", "max_health", 2, "minecraft:nether_star"),
        T("Untamed Strength", "A strength that has never been trained and never needs to be.", "attack_damage", 1, "minecraft:blaze_rod"),
        T("Irregular Fortune", "Chance bends your way, oddly often.", "luck", 1, "minecraft:emerald"),
        T("Blind Spot of the Tower", "The Tower has no entry for you, and neither does your speed.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Pit Leap", "Throw yourself into the air and crash down on the foes below.", "leap_slam", "minecraft:mace", vfx="FIRE_BURST", damage=8, height=1.0),
        A("Unexpected Swing", "A strike nobody can predict: a wide cone ahead of you.", "cone_strike", "minecraft:iron_axe", vfx="WIND_SLASH", damage=7, half_angle=55),
        A("Impossible Dodge", "Vanish from where you were and appear a few blocks ahead.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=9),
        A("Unbalanced Whirl", "Spin wildly and hit everything around you several times.", "whirlwind", "minecraft:iron_axe", vfx="WIND_TORNADO", damage=3, hits=4),
        A("Surge of the Unknown", "An unexplained power fills you: you grow stronger and faster for a while.", "buff_self", "minecraft:nether_star", vfx="FIRE_SPIRAL", effect="strength", effect2="speed", seconds=14),
        A("Hungry Bite", "Sink your teeth into the foe in front of you and drink in some of its life.", "drain_bite", "minecraft:spider_eye", vfx="SHADOW_POOL", damage=7, heal_pct=0.35),
        A("Fall of the Unlisted", "Call down something that was never in the Tower's records onto the place you look at.", "meteor_drop", "minecraft:crying_obsidian", vfx="DEVIL_CIRCLE", damage=12, radius=4),
    ],
    passives=[
        P("Unpredictable Crit", "Your strikes land critically more often than anyone expects.", "crit", "minecraft:spectral_arrow", chance=0.14, mult=1.8),
        P("Lashing Out", "Whoever strikes you may be left withered by the unknown power.", "on_hurt_retaliate", "minecraft:wither_rose", effect="wither", chance=0.25),
        P("Feral Appetite", "Part of every blow you deal returns to you as health.", "lifesteal", "minecraft:redstone", pct=0.06),
        P("Rising Tempo", "The longer the fight, the more your blows grow in strength.", "momentum", "minecraft:blaze_powder", per_stack=0.04, max_stacks=6),
        P("No Entry for Death", "At death's door a brief shield saves you.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("No Record of the Blow", "You may avoid a blow as if it had never been thrown.", "dodge", "minecraft:feather", chance=0.1),
        P("Cornered Surge", "When wounded, the power in you pushes every blow harder.", "dmg_when", "minecraft:iron_sword", condition="low_health", bonus=0.25),
        P("Unclassified Hide", "The Tower does not know what hurts you, so the blow is lessened: less magic damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.2),
    ],
    finishers=[
        A("Outside the System", "Burst outward with a power that has no name: a vast ring of force that throws every foe around you.", "ring_burst", "minecraft:nether_star", vfx="DEVIL_CIRCLE", damage=24, radius=7, targets=9, knockback=1.8, cost=0.07, cooldown=1800),
        A("Eruption of the Unmeasured", "Everything you have in a single beam that tears a lane through the foes in front of you.", "ray_shot", "minecraft:end_crystal", vfx="FIRE_SPIRAL", damage=30, range=30, pierce=4, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 14
LINES.append(Line(
    id="tog_headon_channel", name="Headon Channeler", franchise=F,
    desc="A Channeler hangs a Shinsu-conducting Headon crystal on the body and pours charge through it. Arcs of lightning, cannon beams and a crystal that sings when it is full.",
    color="#FF8A65", background="magma", icon="minecraft:prismarine_crystals",
    book_name="Manual of the Charged Crystal", book_lore="Do not stand near the owner when the crystal is humming.",
    stances=[
        S("Crystal Held Close", "Keep the charge pulled tight to your chest: sturdier and steady, hitting a little softer.", "minecraft:amethyst_shard", armor=2, regen=0.4, damage_pct=-0.03, vfx="LIGHTNING_FIEND"),
        S("Crystal Unleashed", "Let the crystal run hot: faster and far stronger, but the charge burns your guard away.", "minecraft:prismarine_crystals", damage_pct=0.2, armor=-2, speed_pct=0.04, vfx="LIGHTNING_FIEND"),
    ],
    stats=[
        T("Conductive Veins", "Charge runs through you cleanly and hits harder.", "attack_damage", 1, "minecraft:copper_ingot"),
        T("Crystal Heart", "The crystal lends the body its resilience.", "max_health", 2, "minecraft:prismarine_shard"),
        T("Resonant Frame", "Your frame hums and deflects blows.", "armor", 1, "minecraft:iron_chestplate"),
        T("Charged Step", "A small current in the legs.", "movement_speed", 0.03, "minecraft:redstone", op="mult"),
    ],
    arts=[
        A("Arc Jump", "Release an arc that hits one foe, then jumps through the next ones nearby.", "chain_hit", "minecraft:lightning_rod", vfx="LIGHTNING_FIEND", damage=6, jumps=4),
        A("Crystal Lance", "Fire a narrow beam of crystal-charged Shinsu through the foes in front of you.", "ray_shot", "minecraft:prismarine_shard", vfx="LIGHTNING_SPEAR", damage=8, range=16, pierce=2),
        A("Charge Overdrive", "Push more charge through the body: you gain haste and strength for a while.", "buff_self", "minecraft:glowstone_dust", vfx="LIGHTNING_FIEND", effect="haste", effect2="strength", seconds=12),
        A("Headon Pulse", "Pulse the crystal and throw a ring of charge outward.", "ring_burst", "minecraft:redstone_lamp", vfx="LIGHTNING_FIEND", damage=5, radius=4),
        A("Discharge Cut", "Vent charge through a thin blade of light: bonus damage against weakened foes.", "execute", "minecraft:iron_sword", vfx="LIGHTNING_SPEAR", damage=8, bonus=0.7),
        A("Rolling Thunder", "Send a line of crackling charge along the floor ahead of you.", "ground_wave", "minecraft:copper_block", vfx="LIGHTNING_FIEND", damage=6, length=11),
        A("Grounding Plate", "Hold the charge flat in front of you as a shell that absorbs damage.", "shield", "minecraft:copper_ingot", vfx="LIGHTNING_FIEND", absorb_hearts=4),
    ],
    passives=[
        P("Spark of Insight", "Your charged strikes land critically more often.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Static Field", "A weak field around you slows nearby enemies.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
        P("Building Charge", "The longer you fight, the more charge your blows carry.", "momentum", "minecraft:redstone", per_stack=0.04, max_stacks=6),
        P("Lightning Rod", "The crystal draws bolts off you: less lightning damage.", "resist", "minecraft:lightning_rod", damage_kind="lightning", pct=0.35),
        P("Live Wire", "While you are in a fight you deal more damage.", "dmg_when", "minecraft:blaze_rod", condition="in_combat", bonus=0.12),
        P("Overload Finish", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.6),
        P("Siphoned Charge", "Part of each hit's charge feeds back into your health.", "lifesteal", "minecraft:glowstone_dust", pct=0.05),
    ],
    finishers=[
        A("Headon Overcharge", "Dump the entire charge through the crystal: a giant ring of lightning that hammers everything around you.", "ring_burst", "minecraft:beacon", vfx="LIGHTNING_FIEND", damage=22, radius=6.5, targets=9, knockback=1.6, cost=0.07, cooldown=1800),
        A("Shatter the Crystal", "Break the Headon and let its stored charge fall as a pillar of lightning where you look.", "meteor_drop", "minecraft:end_crystal", vfx="LIGHTNING_FIEND", damage=30, radius=5, cost=0.09, cooldown=2000),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 15
LINES.append(Line(
    id="tog_suspendium_arms", name="Suspendium Arms", franchise=F,
    desc="Suspendium, the Tower's cold, weightless metal, forged into blades and hammers whose weight changes as you swing them. Heavy where it counts, light where it must be.",
    color="#B0BEC5", background="iron", icon="minecraft:netherite_ingot",
    book_name="Smith's Book of Light Metal", book_lore="Every page is a different weight; the binding is a curse word.",
    stances=[
        S("Heavy Alloy Stance", "Make the metal heavy: a big boost to damage and some armor, but it slows you down.", "minecraft:netherite_ingot", damage_pct=0.15, speed_pct=-0.1, armor=1, vfx="WEAPON_CONSTRUCTS"),
        S("Light Alloy Stance", "Make the metal weightless: much faster and crit-prone, a little softer.", "minecraft:iron_nugget", speed_pct=0.1, crit_chance=0.06, damage_pct=-0.05, vfx="WEAPON_CONSTRUCTS"),
    ],
    stats=[
        T("Alloy Wrists", "A smith's wrists hold heavy blades with ease.", "attack_damage", 1, "minecraft:iron_axe"),
        T("Forgeborn Skin", "Heat has made you tougher.", "armor", 1, "minecraft:iron_chestplate"),
        T("Anvil Stance", "Your feet stay planted when you are hit.", "knockback_resistance", 0.1, "minecraft:anvil"),
        T("Tempered Reach", "The long arm of the hammer.", "entity_interaction_range", 0.4, "minecraft:stick"),
    ],
    arts=[
        A("Greatblade Arc", "Swing a Suspendium greatblade in a wide arc and cut every foe in the cone.", "cone_strike", "minecraft:netherite_sword", vfx="WEAPON_CONSTRUCTS", damage=7, half_angle=55),
        A("Thrown Blades", "Fling a volley of blades from your belt at the foe you are looking at.", "barrage", "minecraft:iron_sword", vfx="WEAPON_CONSTRUCTS", damage=3, shots=6),
        A("Change the Weight", "Make your own armor heavier: you gain resistance but slow down.", "buff_self", "minecraft:anvil", vfx="WEAPON_CONSTRUCTS", effect="resistance", effect2="slowness", seconds=12),
        A("Hammer Fall", "Leap and bring a weightless-then-heavy hammer down on the foes below.", "leap_slam", "minecraft:mace", vfx="WEAPON_CONSTRUCTS", damage=8, height=0.9),
        A("Lightweight Rush", "Make your weapon weightless and dash through the line of foes ahead.", "dash_strike", "minecraft:iron_sword", vfx="WIND_SLASH", damage=6, distance=8),
        A("Suspended Chains", "Chains of light metal lock the creatures around you in place for a moment.", "stun_ring", "minecraft:chain", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Rising Edge", "Drive the blade upward from below and launch the foe in front of you.", "uppercut", "minecraft:iron_sword", vfx="WIND_SLASH", damage=7),
    ],
    passives=[
        P("Weapon Mastery", "While you hold a sword you deal more damage.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword", bonus=0.15),
        P("Spiked Plates", "Blows against you are partly returned.", "thorns", "minecraft:iron_bars", pct=0.15),
        P("Weight Distribution", "While fighting you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat", pct=0.1),
        P("Hammer Shock", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
        P("Fine Edge", "A well honed edge: critical hits are more likely.", "crit", "minecraft:flint", chance=0.1, mult=1.7),
        P("Arrow Plating", "Plates turn aside arrows and other missiles.", "resist", "minecraft:iron_ingot", damage_kind="projectile", pct=0.3),
        P("Last Hammer Blow", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.6),
    ],
    finishers=[
        A("Suspendium Colossus", "Raise a storm of giant blades out of thin air and loose them all on the foe you look at.", "barrage", "minecraft:netherite_sword", vfx="WEAPON_CONSTRUCTS", damage=5, shots=12, range=18, cost=0.08, cooldown=1800),
        A("Anvil of the Tower", "Call a falling anvil of Suspendium onto the place you look at.", "meteor_drop", "minecraft:anvil", vfx="WEAPON_CONSTRUCTS", damage=30, radius=5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 16
LINES.append(Line(
    id="tog_ranker_authority", name="Ranker's Authority", franchise=F,
    desc="You have climbed beyond the floors where ordinary people stop. A Ranker's presence presses on the weak, and a single command can bring a hall to its knees.",
    color="#FDD835", background="tower", icon="minecraft:golden_helmet",
    book_name="Ledger of the Ranker", book_lore="The first page lists the floors cleared; the last page lists the debts.",
    stances=[
        S("Ranker's Bearing", "Stand in your full authority: tougher and slightly stronger, but never quick.", "minecraft:golden_helmet", armor=3, damage_pct=0.05, speed_pct=-0.05, vfx="MAGIC_CIRCLE"),
        S("Unleashed Ranker", "Drop the restraint: devastating strikes and crits, but the bearing falls away.", "minecraft:netherite_sword", damage_pct=0.2, armor=-2, crit_chance=0.05, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Ranker's Pressure", "Your power is felt in every strike.", "attack_damage", 1, "minecraft:netherite_sword"),
        T("Veteran Frame", "Hundreds of floors have hardened your body.", "max_health", 2, "minecraft:golden_apple"),
        T("Weight of Titles", "Your name alone stops blows short.", "armor_toughness", 1, "minecraft:gold_ingot"),
        T("Reach of Authority", "Your command carries further than it should.", "entity_interaction_range", 0.4, "minecraft:bell"),
    ],
    arts=[
        A("Command: Kneel", "Speak an order of authority and the creatures near you are frozen in place.", "stun_ring", "minecraft:bell", vfx="MAGIC_CIRCLE", radius=4.5, seconds=2),
        A("Judgment Strike", "Call down a weighted strike on the place you look at.", "meteor_drop", "minecraft:gold_block", vfx="SPATIAL_RIFT", damage=11, radius=3.5),
        A("Authority Cleave", "A wide cleave that cuts the cone in front of you in two.", "cone_strike", "minecraft:netherite_sword", vfx="WIND_SLASH", damage=7, half_angle=60),
        A("Banner of Rank", "Raise your banner: you and the allies near you grow stronger for a while.", "buff_aura", "minecraft:white_banner", vfx="SPIRIT_AURA", effect="strength", radius=9, seconds=12),
        A("Sentence", "A cold, exact blow aimed at a weakened foe: it hits much harder at low health.", "execute", "minecraft:diamond_sword", vfx="MAGIC_CIRCLE", damage=9, bonus=0.8),
        A("Floor Stride", "Cross the floor in a heartbeat, cutting down everything between you and your destination.", "dash_strike", "minecraft:golden_boots", vfx="WIND_SLASH", damage=6, distance=9),
        A("Rank Plate", "Spread your presence over your body as a thick, golden plate of absorption.", "shield", "minecraft:golden_chestplate", vfx="MAGIC_CIRCLE", absorb_hearts=5),
    ],
    passives=[
        P("Giant Killer", "Rankers fight the Tower's greatest beasts: bonus damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.2),
        P("Composed Under Fire", "While fighting you shrug off part of every blow.", "reduce_when", "minecraft:golden_chestplate", condition="in_combat", pct=0.1),
        P("Name Carries Weight", "Your authority repels others: enemies near you are weakened.", "aura", "minecraft:fermented_spider_eye", effect="weakness", target="enemies", radius=6),
        P("Practiced Strike", "A Ranker's blows are precise: more critical hits.", "crit", "minecraft:spectral_arrow", chance=0.12, mult=1.7),
        P("Climber's Wisdom", "Floors cleared make you wiser: kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
        P("Last Floor Standing", "When nearly dead, you stand firm behind a brief shield.", "last_stand", "minecraft:totem_of_undying", threshold=0.3, seconds=5),
        P("Taking the Spoils", "Kills reward you with health and a short surge of strength.", "kill_reward", "minecraft:golden_apple", heal=3, effect="strength", seconds=4),
    ],
    finishers=[
        A("Ranker's Authority", "Let the full weight of your rank fall on the room: every foe around you is held for a long moment.", "stun_ring", "minecraft:nether_star", vfx="MAGIC_CIRCLE", radius=10, seconds=5, targets=12, cost=0.09, cooldown=1900),
        A("Floor-Splitting Blow", "Bring your weapon down and split the floor ahead of you in a long, deep crack.", "ground_wave", "minecraft:netherite_axe", vfx="EARTH_SPIKES", damage=28, length=20, width=4, knockback=1.6, cost=0.06, cooldown=1700),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 17
LINES.append(Line(
    id="tog_faerie", name="Faerie", franchise=F,
    desc="A Faerie is a small, bright, quick being of the Tower, half dream and half mischief. It drifts, blinks, tangles and lulls, and leaves glitter wherever it goes.",
    color="#F48FB1", background="dream", icon="minecraft:pink_petals",
    book_name="A Faerie's Little Book of Tricks", book_lore="Written on a petal and not meant to be read in one sitting.",
    stances=[
        S("Drifting Wing Stance", "Float on the wing: much faster, but a single hit will hurt you.", "minecraft:feather", speed_pct=0.1, armor=-2, damage_pct=-0.03, vfx="SPIRIT_AURA"),
        S("Dew Veil Stance", "Wrap yourself in morning dew: you regenerate and hold firm, but you move slowly.", "minecraft:glow_berries", armor=2, regen=0.6, speed_pct=-0.04, vfx="WATER_RING"),
    ],
    stats=[
        T("Pixie Grace", "You flutter faster than you walk.", "movement_speed", 0.05, "minecraft:feather", op="mult"),
        T("Dew Heart", "A small bright heart that mends fast.", "max_health", 2, "minecraft:glow_berries"),
        T("Featherfall", "A Faerie can fall a very long way and not mind.", "safe_fall_distance", 2, "minecraft:phantom_membrane"),
        T("Bright Fortune", "Wishes land in your hands more often than they should.", "luck", 1, "minecraft:gold_nugget"),
    ],
    arts=[
        A("Fairy Step", "Flit forward in a shower of glitter to a spot a few blocks away.", "blink", "minecraft:pink_petals", vfx="ELF_CIRCLE", distance=9),
        A("Pollen Cloud", "Puff pollen in a cone: the foes in it feel sick and heavy.", "debuff_cone", "minecraft:spore_blossom", vfx="POISON_BREATH", effect="nausea", effect2="slowness", seconds=6, seconds2=6),
        A("Wish Mending", "Make a small wish: you and the allies around you are healed.", "heal_pulse", "minecraft:glistering_melon_slice", vfx="ELF_CIRCLE", radius=7, amount=6),
        A("Glimmer Bolt", "Fire a bright dart of glitter along your line of sight.", "ray_shot", "minecraft:glowstone_dust", vfx="LIGHTNING_SPEAR", damage=6, range=16, pierce=2),
        A("Dream Veil", "Pull dream-fog over the creatures around you: they lose sight of you.", "smoke", "minecraft:white_dye", vfx="ELF_CIRCLE", radius=5, seconds=6),
        A("Ring-a-Ring", "A twirl of sparks that jumps from foe to foe in a tangled dance.", "chain_hit", "minecraft:blaze_powder", vfx="ELF_CIRCLE", damage=5, jumps=4),
        A("Airborne Whim", "Float on a gust: you gain slow falling and a leap for a while.", "buff_self", "minecraft:wind_charge", vfx="WIND_RING", effect="slow_falling", effect2="jump_boost", seconds=14),
    ],
    passives=[
        P("Light as Air", "While you are in the air you move faster.", "self_effect_when", "minecraft:feather", effect="speed", condition="airborne"),
        P("Sprite's Luck", "You may skip out of the way of a blow.", "dodge", "minecraft:rabbit_foot", chance=0.12),
        P("Moonlit Mending", "At night your wounds heal on their own.", "regen_when", "minecraft:glow_berries", condition="night", per_second=0.6),
        P("Faerie Court", "Allies near you feel luckier.", "aura", "minecraft:gold_nugget", effect="luck", target="allies", radius=7),
        P("Gentle Landing", "You take much less fall damage.", "fall_guard", "minecraft:phantom_membrane", pct=0.5),
        P("Glitter Blind", "Your blows may leave the foe blinded by glitter.", "on_hit_effect", "minecraft:glowstone_dust", effect="blindness", chance=0.2),
        P("Petal Mending", "A few petals of care keep mending you.", "mending", "minecraft:pink_petals", seconds=16, amount=1),
    ],
    finishers=[
        A("Midsummer Dream", "Put the whole room to sleep in a flash of dream light: the creatures around you are held in place.", "stun_ring", "minecraft:blue_orchid", vfx="ELF_CIRCLE", radius=8, seconds=4, targets=10, cost=0.08, cooldown=1900),
        A("Wishing Star", "Call down a bright star onto the spot you are looking at.", "meteor_drop", "minecraft:nether_star", vfx="SPIRIT_AURA", damage=26, radius=5, cost=0.09, cooldown=1900),
    ],
))

# ---------------------------------------------------------------------------------------------------------------- 18
LINES.append(Line(
    id="tog_zahard_decree", name="Zahard's Decree", franchise=F,
    desc="The Tower runs on the King's law, and you carry a scrap of it. Pass sentence on foes, turn blows back by law, and bring a room to order.",
    color="#CFD8DC", background="stellar", icon="minecraft:beacon",
    book_name="The Statute Scroll of the Tower", book_lore="Seventeen articles; the eighteenth is erased and everyone knows what it said.",
    stances=[
        S("Decree of Order", "Hold the room in lawful stillness: sturdier and steady, but your blows are a little softer.", "minecraft:white_banner", armor=3, regen=0.3, damage_pct=-0.05, vfx="MAGIC_CIRCLE"),
        S("Decree of Force", "Enforce the law with the blade: far stronger blows, nothing left to protect you.", "minecraft:netherite_sword", damage_pct=0.18, armor=-2, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Royal Constitution", "The King's law keeps you whole.", "max_health", 2, "minecraft:golden_apple"),
        T("Weight of the Throne", "Blows glance off the one who carries the law.", "armor_toughness", 1, "minecraft:gold_ingot"),
        T("Reach of the Decree", "Your word carries further than your arm.", "entity_interaction_range", 0.4, "minecraft:bell"),
        T("Enforcer's Hand", "A hand that has delivered a lot of sentences.", "attack_damage", 1, "minecraft:iron_sword"),
    ],
    arts=[
        A("Edict of Judgment", "Name a foe in the law: it takes extra damage from every blow while the edict holds.", "mark", "minecraft:writable_book", vfx="MAGIC_CIRCLE", bonus=0.3, range=26),
        A("Law of Return", "For a while part of every blow you take is thrown back at whoever delivered it.", "ward_reflect", "minecraft:shield", vfx="MIRROR_PANE", reflect_pct=0.4),
        A("Decree: Halt", "Order the creatures around you to stop; they are frozen in place.", "stun_ring", "minecraft:bell", vfx="SEAL_CHAINS", radius=4, seconds=2),
        A("Decree of Silence", "Speak a ban in a cone: the foes in it grow weak and tired.", "debuff_cone", "minecraft:paper", vfx="SEAL_CHAINS", effect="weakness", effect2="mining_fatigue", seconds=7, seconds2=7),
        A("Sentence Carried Out", "A clean blow reserved for the already wounded: it deals far more at low health.", "execute", "minecraft:diamond_sword", vfx="MAGIC_CIRCLE", damage=9, bonus=0.8),
        A("Mandate of Strength", "Grant your allies near you the strength of the law for a while.", "buff_aura", "minecraft:white_banner", vfx="MAGIC_CIRCLE", effect="strength", radius=9, seconds=12),
        A("Verdict Falls", "A column of white light lands on the spot you look at after a short pause.", "meteor_drop", "minecraft:end_rod", vfx="SPATIAL_RIFT", damage=11, radius=3.5),
    ],
    passives=[
        P("Scales of the Law", "You hit other players harder: the law is stricter between climbers.", "dmg_vs", "minecraft:player_head", category="player", bonus=0.15),
        P("Swift Justice", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword", threshold=0.25, bonus=0.6),
        P("Pressure of Authority", "Enemies close to you are slowed by the weight of the law.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=5),
        P("Reciprocity", "Part of each blow against you is returned.", "thorns", "minecraft:iron_bars", pct=0.15),
        P("Barred from Decay", "You take less wither damage.", "resist", "minecraft:wither_rose", damage_kind="wither", pct=0.3),
        P("Review of Cases", "Each foe you defeat grants more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.12),
        P("Presumption of Armor", "In combat the law keeps you from the worst: you take less damage.", "reduce_when", "minecraft:golden_chestplate", condition="in_combat", pct=0.1),
    ],
    finishers=[
        A("Zahard's Judgment", "Call the sentence of the King down on the spot you look at: a heavy pillar of white fire.", "meteor_drop", "minecraft:beacon", vfx="SPATIAL_RIFT", damage=30, radius=5.5, cost=0.09, cooldown=2000),
        A("Absolute Law", "For a long breath, over half of every blow you take is thrown back on the attacker.", "ward_reflect", "minecraft:totem_of_undying", vfx="MIRROR_PANE", seconds=10, reflect_pct=0.65, cost=0.05, cooldown=1700),
    ],
))
