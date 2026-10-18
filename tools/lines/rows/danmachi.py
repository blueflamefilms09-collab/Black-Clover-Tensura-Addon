"""DanMachi: 25 unique skill lines (familia Blessings and Statuses, adventurer Skills and Magic, chant magic, Developmental Abilities, Argonaut, familia roles).

Canon note: names follow the story's vocabulary; every numeric effect and wording is this mod's own design. Where a detail was uncertain it is invented in the story's spirit.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "danmachi"


def A(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon)


def P(name, desc, kind, icon, **p):
    return Pas(name, desc, kind, p, icon)


def S(name, desc, icon, **p):
    return Stance(name, desc, icon, p)


def T(name, desc, attribute, amount, icon, op="add"):
    return Stat(name, desc, attribute, amount, op, icon)


LINES = []

# ---------------------------------------------------------------------------------------------------------------------------------- 1
LINES.append(Line(
    id="hestia_falna", name="Falna: Hestia's Blessing", franchise=F,
    desc="The goddess's mark on your back: Status that climbs with every dungeon floor, experience that never wastes and a hearth to come home to.",
    color="#ff8a3d", background="city", icon="minecraft:campfire",
    book_name="Hestia Familia Status Scroll", book_lore="A small goddess, a smaller church, and a back that keeps getting stronger.",
    stances=[
        S("Dungeon Dive Stance", "Eyes ahead and no time to rest: quicker and sharper, lightly armored.", "minecraft:lantern", speed_pct=0.1, damage_pct=0.15, armor=-1, vfx="SPIRIT_AURA"),
        S("Hearthbound Rest", "Settle by the hearth: healing, sturdy, a bit sluggish.", "minecraft:campfire", regen=1.0, armor=3, speed_pct=-0.1, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Strength Status Rises", "Your Strength column on the Falna climbs.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Endurance Status Rises", "Your Endurance column climbs: more maximum health.", "max_health", 2, "minecraft:cooked_beef"),
        T("Dexterity Status Rises", "Your Dexterity column climbs: faster hands.", "attack_speed", 0.05, "minecraft:clock", op="mult"),
        T("Agility Status Rises", "Your Agility column climbs: quicker feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Dungeon Slash", "A solid cone-shaped cut across the monsters in front.", "cone_strike", "minecraft:iron_sword", damage=7, range=3.8, half_angle=45, vfx="WIND_SLASH"),
        A("Adventurer's Lunge", "Lunge through the line of monsters before you.", "dash_strike", "minecraft:iron_boots", distance=6, damage=7, vfx="WIND_SLASH"),
        A("Falna Surge", "The Blessing flares on your back: Strength and Speed for a while.", "buff_self", "minecraft:blaze_powder", effect="strength", effect2="speed", seconds=14, vfx="SPIRIT_AURA"),
        A("Hearth Rest", "A moment by the hearth, in the middle of a fight: heal and regenerate.", "heal_self", "minecraft:bread", amount=8, regen_seconds=6, vfx="FIRE_BURST"),
        A("Hearth Ward", "A warm shield of hearth-light absorbs a good deal of damage.", "shield", "minecraft:lantern", absorb_hearts=5, seconds=8, vfx="SPIRIT_AURA"),
        A("Read the Status", "Study the monster's strengths; it takes extra damage while you do.", "mark", "minecraft:book", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Divine Release", "A release of godly light throws the monsters around you back.", "ring_burst", "minecraft:glowstone_dust", damage=6, radius=4, knockback=1.0, vfx="MAGIC_CIRCLE"),
    ],
    passives=[
        P("Excelia Gathering", "Kills give more experience: Excelia piles up.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Hearthfire Recovery", "Your wounds close slowly over time, even out of combat.", "mending", "minecraft:campfire", seconds=14),
        P("Drop Item Chance", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.15),
        P("Magic Stone Harvest", "Each kill heals you.", "kill_reward", "minecraft:amethyst_shard", heal=3, effect="none", seconds=0),
        P("The Goddess's Whisper", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Growing Resolve", "Blows in a row grow stronger.", "momentum", "minecraft:flint", max_stacks=5),
        P("Falna Guard", "At low health you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="low_health", pct=0.2),
    ],
    finishers=[
        A("Familia Gathering", "The whole familia lights up: you and your allies gain a long surge of Strength.", "buff_aura", "minecraft:white_banner", effect="strength", radius=12, seconds=20, amp=1, cooldown=1500, vfx="MAGIC_CIRCLE"),
        A("Hestia's Embrace", "A goddess's hug heals you and your allies in a wide ring.", "heal_pulse", "minecraft:golden_apple", amount=20, radius=11, cooldown=1400, vfx="SPIRIT_AURA"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 2
LINES.append(Line(
    id="bell_firebolt", name="Bell's Firebolt", franchise=F,
    desc="Chantless fire magic fired as fast as the next breath: streams of bolts, a charged lance and the nerve to keep running straight at the monster.",
    color="#ff4d2e", background="storm", icon="minecraft:fire_charge",
    book_name="Firebolt: A Rookie's Grimoire", book_lore="No incantation. Just a very strong opinion.",
    stances=[
        S("Rapid-Fire Stance", "Plant yourself and let the bolts fly: far more damage, slower on your feet.", "minecraft:blaze_rod", damage_pct=0.25, speed_pct=-0.05, armor=-2, vfx="FIRE_BURST"),
        S("Burning Sprint", "Run and gun: fast and light but fragile.", "minecraft:fire_charge", speed_pct=0.15, armor=-1, damage_pct=0.05, vfx="FLAME_TRAIL"),
    ],
    stats=[
        T("Rookie's Magic", "More sting from every bolt.", "attack_damage", 1, "minecraft:blaze_powder"),
        T("Sparks in the Legs", "A touch more speed.", "movement_speed", 0.04, "minecraft:golden_boots", op="mult"),
        T("Small but Stubborn", "A little more maximum health.", "max_health", 2, "minecraft:golden_carrot"),
    ],
    arts=[
        A("Firebolt Rapid Fire", "Fire a stream of bolts at the monster you look at.", "barrage", "minecraft:blaze_powder", damage=3, shots=6, range=16, interval=2, vfx="FIRE_BURST"),
        A("Charged Firebolt", "A bolt held for a heartbeat: a single piercing line of flame.", "ray_shot", "minecraft:blaze_rod", damage=9, range=22, pierce=2, vfx="FIRE_SPIRAL"),
        A("Arcing Firebolt", "A bolt that leaps from monster to monster.", "chain_hit", "minecraft:fire_charge", damage=5, jumps=4, vfx="LIGHTNING_FIEND"),
        A("Blazing Dash", "Burst through the monsters in front with a trail of sparks.", "dash_strike", "minecraft:flint_and_steel", distance=7, damage=7, vfx="FLAME_TRAIL"),
        A("Firebolt Boost Jump", "Fire a bolt at your feet and ride it into the air, then drop on the monsters.", "leap_slam", "minecraft:firework_rocket", damage=8, radius=3.2, height=1.0, vfx="FLAME_EXPLOSION"),
        A("Scorched Sidestep", "Fire a short burst behind you and slide away.", "blink", "minecraft:gunpowder", distance=8, vfx="FIRE_BURST"),
        A("Backdraft", "A ring of fire thrown outward when monsters close in.", "ring_burst", "minecraft:magma_cream", damage=6, radius=4, knockback=0.9, vfx="FLAME_EXPLOSION"),
    ],
    passives=[
        P("Fire-Eater", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.4),
        P("Lucky Shot", "Your bolts may strike critically.", "crit", "minecraft:ender_eye", chance=0.15, mult=1.8),
        P("Hot on the Heels", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.25),
        P("Singed Trail", "Your bolts may slow the monster.", "on_hit_effect", "minecraft:magma_cream", effect="slowness", seconds=4, chance=0.3),
        P("Blast Shove", "Your bolts may shove the monster back.", "knockback_on_hit", "minecraft:fire_charge", strength=0.5, chance=0.35),
        P("Running Rookie", "You may duck a blow completely.", "dodge", "minecraft:rabbit_foot", chance=0.1),
        P("No Chant Needed", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
    ],
    finishers=[
        A("Firebolt: Burst Fire", "A relentless barrage of bolts, one after the other, at one monster.", "barrage", "minecraft:fire_charge", damage=4, shots=9, range=22, interval=2, cooldown=1300, vfx="FIRE_BURST"),
        A("Hero's Spark", "A great bolt of focused flame pierces everything on the line.", "ray_shot", "minecraft:nether_star", damage=27, range=30, pierce=4, cooldown=1400, vfx="FIRE_SPIRAL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 3
LINES.append(Line(
    id="argonaut_charge", name="Argonaut", franchise=F,
    desc="The Skill that rewards daring: charge the strike, loose it all at once and hit the thing everyone else ran from.",
    color="#4aa3ff", background="tower", icon="minecraft:iron_sword",
    book_name="The Charge of the Hero", book_lore="You do not need to be strong. You need to be about to be.",
    stances=[
        S("Charging Stance", "Gather the strike: far greater damage while you slow down to focus.", "minecraft:iron_sword", damage_pct=0.3, speed_pct=-0.1, vfx="SPIRIT_AURA"),
        S("Run Toward the Light", "Charge in: faster and tougher at the front, at the cost of damage.", "minecraft:white_banner", speed_pct=0.1, armor=3, damage_pct=-0.1, vfx="WIND_RING"),
    ],
    stats=[
        T("Hero's Resolve", "Your will hardens your hide.", "armor", 1, "minecraft:iron_chestplate"),
        T("Courage Under Fire", "More maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Charged Edge", "The stored power shows in your sword arm.", "attack_damage", 1.5, "minecraft:diamond_sword"),
    ],
    arts=[
        A("Charge Up", "Draw the power to a point: a long surge of Strength and Haste.", "buff_self", "minecraft:blaze_powder", effect="strength", effect2="haste", seconds=14, amp=1, vfx="SPIRIT_AURA"),
        A("Charge Dash", "Release the stored power in a straight line through the monsters.", "dash_strike", "minecraft:iron_boots", distance=8, damage=8, vfx="WIND_SLASH"),
        A("Heroic Finisher", "One focused blow that hits much harder the weaker the monster is.", "execute", "minecraft:diamond_sword", damage=10, bonus=1.0, vfx="WIND_SLASH"),
        A("Hero's Leap", "Leap and crash down with the charge behind you.", "leap_slam", "minecraft:anvil", damage=9, radius=3.2, height=1.0, vfx="EARTH_SPIKES"),
        A("Giant-Felling Upper", "A rising blow that sends even a big monster airborne.", "uppercut", "minecraft:iron_axe", damage=8, launch=0.8, vfx="WIND_SLASH"),
        A("Release the Charge", "All the stored power goes outward at once in a ring.", "ring_burst", "minecraft:glowstone", damage=7, radius=4.5, knockback=1.2, vfx="FLAME_EXPLOSION"),
        A("Rookie's Dare", "A shout that makes the monsters nearby turn and look at you.", "taunt", "minecraft:goat_horn", radius=8, targets=5, vfx="DEVIL_CIRCLE"),
    ],
    passives=[
        P("Charge Stacks", "Blows in a row build a charge that adds damage.", "momentum", "minecraft:redstone", max_stacks=7, per_stack=0.05),
        P("Three-Beat Rhythm", "Every few blows in a row hit much harder.", "combo", "minecraft:flint", hits=3, bonus=0.55),
        P("Courage in Crisis", "At low health your blows land harder.", "dmg_when", "minecraft:blaze_powder", condition="low_health", bonus=0.35),
        P("Heroic Momentum", "Each kill heals you and fills you with Strength.", "kill_reward", "minecraft:emerald", heal=3, effect="strength", seconds=6),
        P("Finish What You Start", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.7),
        P("Not Backing Down", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Run-Up Bonus", "You take less damage while sprinting.", "reduce_when", "minecraft:iron_boots", condition="sprinting", pct=0.15),
    ],
    finishers=[
        A("Argonaut: Full Charge", "Hold it, hold it, release: a punishing run through everything in front of you.", "dash_strike", "minecraft:nether_star", distance=11, damage=24, targets=6, cooldown=1400, vfx="FLAME_TRAIL"),
        A("Hero's Descent", "Leap and fall like a thrown spear, shattering the floor.", "leap_slam", "minecraft:netherite_sword", damage=28, radius=6, height=1.2, knockback=1.6, cooldown=1300, vfx="EARTH_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 4
LINES.append(Line(
    id="hestia_knife", name="Hestia Knife", franchise=F,
    desc="A divine dagger that shines with its owner's Blessing: quick cuts, thrown blades and a thrust through the one chink in the armor.",
    color="#2e3a8c", background="void", icon="minecraft:iron_nugget",
    book_name="The Knife That Grows", book_lore="The blade is the one thing in the room that never stops getting stronger.",
    stances=[
        S("Knife Fighter's Crouch", "Low and fast: quicker, with sharper crits and little armor.", "minecraft:iron_sword", speed_pct=0.1, crit_chance=0.15, armor=-2, vfx="WIND_SLASH"),
        S("Guard of the Hearth", "Dagger up, shoulder forward: sturdier and healing, with softer blows.", "minecraft:shield", armor=3, regen=0.5, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Divine Edge", "The knife's glow adds bite.", "attack_damage", 1.5, "minecraft:iron_sword"),
        T("Quick Hands", "Fast, light thrusts.", "attack_speed", 0.07, "minecraft:clock", op="mult"),
        T("Fleet Feet", "Nimble movement.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Knife Through the Chink", "Slip the blade between plates for a blow much deadlier against a wounded foe.", "execute", "minecraft:iron_nugget", damage=9, bonus=1.0, vfx="WIND_SLASH"),
        A("Thrown Hestia Knife", "Hurl the dagger in a straight line; it flies through the monsters in its path.", "ray_shot", "minecraft:iron_sword", damage=8, range=18, pierce=2, vfx="WEAPON_CONSTRUCTS"),
        A("Dagger Flurry", "A fast, tight cone of cuts at close range.", "cone_strike", "minecraft:stone_sword", damage=7, range=3.4, half_angle=45, vfx="WIND_SLASH"),
        A("Mark the Weak Point", "Spot the creature's soft spot; it takes more damage for a while.", "mark", "minecraft:spyglass", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Divine Sidestep", "Slip aside in a flash of the knife's glow.", "blink", "minecraft:ender_pearl", distance=7, vfx="SPIRIT_AURA"),
        A("Rising Blade", "An upward stab that throws the monster off its feet.", "uppercut", "minecraft:wooden_sword", damage=7, launch=0.8, vfx="WIND_SLASH"),
        A("Knife Rain", "Loose a handful of throwing knives at the monster you look at.", "barrage", "minecraft:flint", damage=2.5, shots=6, range=14, vfx="WEAPON_CONSTRUCTS"),
    ],
    passives=[
        P("Weak-Spot Strike", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.18, mult=1.8),
        P("Glowing Edge", "Your cuts may make the monster glow.", "on_hit_effect", "minecraft:glowstone_dust", effect="glowing", seconds=8, chance=0.35),
        P("Monster Slayer's Knife", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=0.2),
        P("Dagger Grip", "While you hold a blade you are faster.", "self_effect_when", "minecraft:iron_sword", effect="speed", condition="holding_sword"),
        P("Final Thrust", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:iron_nugget", threshold=0.25, bonus=0.7),
        P("Parrying Edge", "You take less magic damage.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.3),
        P("Blessing Leeches Life", "Your blows heal you.", "lifesteal", "minecraft:glistering_melon_slice", pct=0.06),
    ],
    finishers=[
        A("Hestia Knife: Divine Thrust", "A single flawless thrust that is devastating against a wounded monster.", "execute", "minecraft:netherite_sword", damage=30, range=4.5, bonus=2.0, cooldown=1300, vfx="SPIRIT_AURA"),
        A("Hearth Blade Cross", "Two swift cuts that make a cross of light in front of you.", "cone_strike", "minecraft:diamond_sword", damage=22, range=6, half_angle=60, targets=6, cooldown=1400, vfx="MIRROR_PANE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 5
LINES.append(Line(
    id="liaris_freese", name="Liaris Freese", franchise=F,
    desc="Power that grows from longing: the harder you chase the person ahead, the faster you rise. Rapid boosts, steady recovery and an unreasonable amount of experience.",
    color="#ffd24a", background="dream", icon="minecraft:experience_bottle",
    book_name="Notes on a Rapidly Growing Wish", book_lore="Admiration, it turns out, is a kind of fuel.",
    stances=[
        S("Longing Stance", "Everything pointed forward: more damage and speed, no guard.", "minecraft:golden_apple", damage_pct=0.2, speed_pct=0.08, armor=-2, vfx="SPIRIT_AURA"),
        S("Steady Resolve", "Hold your ground and keep your eyes on the goal: tough and healing, with softer blows.", "minecraft:iron_chestplate", armor=4, regen=0.7, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Rapid Growth", "Faster-rising vitality.", "max_health", 3, "minecraft:golden_apple"),
        T("Rising Strength", "A swelling sword arm.", "attack_damage", 1.5, "minecraft:iron_sword"),
        T("Chasing Stride", "You run faster the further behind you feel.", "movement_speed", 0.04, "minecraft:golden_boots", op="mult"),
        T("Heroic Spirit", "A little extra luck for a rookie.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Wish to Catch Up", "Your longing flares: Haste and Speed for a long time.", "buff_self", "minecraft:sugar", effect="haste", effect2="speed", seconds=16, amp=1, vfx="SPIRIT_AURA"),
        A("Idol in Sight", "Fix your gaze on one monster; it takes extra damage while you pursue it.", "mark", "minecraft:ender_eye", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Resolve Renewed", "Your will mends your body.", "heal_self", "minecraft:golden_carrot", amount=9, regen_seconds=8, vfx="SPIRIT_AURA"),
        A("Striving Dance", "Spin through the monsters, never stopping, hitting each one over and over.", "whirlwind", "minecraft:iron_sword", damage=3.5, hits=5, radius=3.5, vfx="WIND_RING"),
        A("Yearning Pulse", "A wave of longing bursts out of you.", "ring_burst", "minecraft:glowstone_dust", damage=6, radius=4.2, knockback=0.9, vfx="MAGIC_CIRCLE"),
        A("Chase the Back", "Dash straight ahead after whatever is in front of you.", "dash_strike", "minecraft:feather", distance=8, damage=7, vfx="WIND_SLASH"),
        A("Resolve Wall", "A wall of stubbornness that soaks up hits.", "shield", "minecraft:shield", absorb_hearts=5, seconds=8, vfx="MIRROR_PANE"),
    ],
    passives=[
        P("Growth Spurt", "Kills give much more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.3),
        P("Desperate Recovery", "At low health your wounds close fast.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.2),
        P("Fuel from a Win", "Each kill heals you and gives you Strength.", "kill_reward", "minecraft:blaze_powder", heal=3, effect="strength", seconds=6),
        P("Underdog", "You deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.25),
        P("Light Stomach", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Self-Healing Spirit", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=14),
        P("Lucky Hit", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.14, mult=1.8),
    ],
    finishers=[
        A("Liaris Freese: Hero's Wish", "The skill at its peak: a massive Strength and Resistance surge.", "buff_self", "minecraft:nether_star", effect="strength", effect2="resistance", seconds=30, amp=1, cooldown=1800, vfx="SPIRIT_AURA"),
        A("Burst of Longing", "A full-power ring of light that blasts everything around you.", "ring_burst", "minecraft:glowstone", damage=22, radius=6.5, targets=8, knockback=1.6, cooldown=1400, vfx="MAGIC_CIRCLE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 6
LINES.append(Line(
    id="airiel_wind_maiden", name="Airiel: Wind Maiden", franchise=F,
    desc="A sword wrapped in a gale: dashes, crescents and a descent from the sky, all carried by a wind that never leaves the blade. The Sword Princess's way.",
    color="#f3e07a", background="holy", icon="minecraft:golden_sword",
    book_name="Gale Armor Treatise", book_lore="The wind is a good friend, and quiet about it.",
    stances=[
        S("Wind-Sheathed", "The gale wraps you: much faster and sharper, with thinner armor.", "minecraft:feather", speed_pct=0.15, damage_pct=0.1, armor=-2, vfx="WIND_RING"),
        S("Sword Princess's Guard", "Blade held in a perfect guard: steadier, harder to push, with gentler cuts.", "minecraft:shield", armor=3, knockback_res=0.3, damage_pct=-0.05, vfx="WIND_SLASH"),
    ],
    stats=[
        T("Gale-Honed Edge", "A wind-sharpened blade.", "attack_damage", 1.5, "minecraft:golden_sword"),
        T("Tailwind", "A wind at your back.", "movement_speed", 0.05, "minecraft:golden_boots", op="mult"),
        T("Sword Princess's Poise", "Calm, flowing swing speed.", "attack_speed", 0.06, "minecraft:clock", op="mult"),
        T("Weightless Landing", "The gale softens every landing.", "safe_fall_distance", 2, "minecraft:feather"),
    ],
    arts=[
        A("Airiel Armor", "The wind takes the shape of armor: Speed and Resistance for a while.", "buff_self", "minecraft:wind_charge", effect="speed", effect2="resistance", seconds=14, vfx="WIND_RING"),
        A("Tempest Dash", "Ride the wind through the monsters ahead.", "dash_strike", "minecraft:feather", distance=8, damage=7, vfx="WIND_SLASH"),
        A("Wind-Edged Cut", "A bright crescent of wind and steel.", "cone_strike", "minecraft:golden_sword", damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Cyclone Dance", "Spin in a tight tornado of cuts.", "whirlwind", "minecraft:iron_sword", damage=3.5, hits=5, radius=3.5, vfx="WIND_TORNADO"),
        A("Gale Slash", "A thin wave of wind skims the ground ahead.", "ground_wave", "minecraft:white_dye", damage=7, length=12, width=2, vfx="WIND_SLASH"),
        A("Descending Storm", "Leap high on the wind and crash down on the monsters below.", "leap_slam", "minecraft:elytra", damage=9, radius=3.2, height=1.1, vfx="WIND_RING"),
        A("Wind Lance", "Compress the wind into a spear that pierces a line of monsters.", "ray_shot", "minecraft:breeze_rod", damage=8, range=22, pierce=2, vfx="LIGHTNING_SPEAR"),
    ],
    passives=[
        P("Wind-Footed Draw", "While you hold a sword you are faster.", "self_effect_when", "minecraft:golden_sword", effect="speed", condition="holding_sword"),
        P("Edge of the Gale", "Your cuts may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Slip the Wind", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
        P("Wind Cushion", "You take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.6),
        P("Aerial Strike", "While airborne your blows hit harder.", "dmg_when", "minecraft:elytra", condition="airborne", bonus=0.3),
        P("Gale Push", "Your cuts may shove the monster back.", "knockback_on_hit", "minecraft:wind_charge", strength=0.5, chance=0.35),
        P("Wind Deflection", "The wind turns arrows and bolts aside: less projectile damage.", "resist", "minecraft:arrow", damage_kind="projectile", pct=0.35),
    ],
    finishers=[
        A("Tempest: Sword Princess's Gale", "A great wave of wind and steel sweeps the ground ahead.", "ground_wave", "minecraft:diamond_sword", damage=22, length=18, width=5, knockback=1.2, cooldown=1300, vfx="WIND_SLASH"),
        A("Airiel: Storm Maiden", "A whirling storm of cuts rises around you.", "whirlwind", "minecraft:wind_charge", damage=6, hits=7, radius=5, interval=3, cooldown=1400, vfx="WIND_TORNADO"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 7
LINES.append(Line(
    id="rea_laevateinn", name="Rea Laevateinn", franchise=F,
    desc="Elven chant magic of fire: stand still, speak the verses and loose a flame sword of terrible reach. Slow to begin, hard to forget.",
    color="#ff6a2a", background="magma", icon="minecraft:blaze_rod",
    book_name="Long Chant of the Flame Sword", book_lore="Every verse is a step toward a fire you may not like.",
    stances=[
        S("Chanting Stance", "Feet planted, voice steady: much more damage, but you cannot afford to move.", "minecraft:lectern", damage_pct=0.3, speed_pct=-0.2, vfx="MAGIC_CIRCLE"),
        S("Warded Caster", "A guarded stance of wind and cloth: armored and slowly healing, with less damage.", "minecraft:shield", armor=4, regen=0.5, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Flame Mage's Power", "More fire in every verse.", "attack_damage", 1.5, "minecraft:blaze_powder"),
        T("Elven Vitality", "A long-lived body.", "max_health", 2, "minecraft:golden_apple"),
        T("Mage's Mantle", "A warded robe.", "armor_toughness", 1, "minecraft:leather_chestplate"),
    ],
    arts=[
        A("Chant Complete", "The verse finishes: Strength and Fire Resistance for a while.", "buff_self", "minecraft:enchanted_book", effect="strength", effect2="fire_resistance", seconds=16, vfx="MAGIC_CIRCLE"),
        A("Flare Sweep", "A flame-bright cone sweeping in front of you.", "cone_strike", "minecraft:blaze_powder", damage=8, range=4.5, half_angle=55, vfx="FIRE_BURST"),
        A("Fire Road", "A road of flame races ahead of you.", "ground_wave", "minecraft:magma_block", damage=7, length=12, width=2, vfx="FLAME_TRAIL"),
        A("Verse of Eruption", "The ground around you bursts into flame.", "ring_burst", "minecraft:fire_charge", damage=7, radius=4.5, knockback=0.9, vfx="FLAME_EXPLOSION"),
        A("Laevateinn Beam", "A narrow beam of flame pierces everything along your sight line.", "ray_shot", "minecraft:blaze_rod", damage=9, range=24, pierce=3, vfx="FIRE_SPIRAL"),
        A("Flame Sword Descent", "A sword of flame falls on the place you look at.", "meteor_drop", "minecraft:lava_bucket", damage=14, radius=4.5, range=24, vfx="FLAME_EXPLOSION"),
        A("Fire-Veil Ward", "A shell of low flame absorbs some damage.", "shield", "minecraft:campfire", absorb_hearts=5, seconds=8, vfx="FIRE_BURST"),
    ],
    passives=[
        P("Kin of Fire", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.45),
        P("Mage's Crit", "Your spells may strike critically.", "crit", "minecraft:ender_eye", chance=0.15, mult=1.8),
        P("Closing Verse", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:blaze_rod", threshold=0.25, bonus=0.7),
        P("Chant Breathing", "Crouch and breathe, and your wounds close.", "regen_when", "minecraft:book", condition="sneaking", per_second=1.0),
        P("Magic Experience", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Flame Against the Dead", "You deal more damage to the undead.", "dmg_vs", "minecraft:bone", category="undead", bonus=0.3),
        P("Verse of Defiance", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
    ],
    finishers=[
        A("Rea Laevateinn: Burning Judgment", "The full chant: a colossal flame sword crashes down.", "meteor_drop", "minecraft:nether_star", damage=32, radius=7, range=28, cooldown=1500, vfx="FLAME_EXPLOSION"),
        A("Surge of the Flame Sword", "A ring of flame bursts outward from you.", "ring_burst", "minecraft:fire_charge", damage=24, radius=7, targets=9, knockback=1.6, cooldown=1300, vfx="FLAME_EXPLOSION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 8
LINES.append(Line(
    id="wynn_fimbulvetr", name="Wynn Fimbulvetr", franchise=F,
    desc="Elven chant magic of ice: the long winter arrives on cue. Freezing breath, a pull toward the cold and slow, crushing control of the room.",
    color="#9fe3ff", background="frost", icon="minecraft:packed_ice",
    book_name="Verses of the Great Winter", book_lore="Fimbulvetr: three winters with no summer between. Speak it carefully.",
    stances=[
        S("Winter Chant", "Slow, cold and clear: stronger spells, almost still on your feet.", "minecraft:blue_ice", damage_pct=0.25, speed_pct=-0.15, vfx="WATER_RING"),
        S("Frost Mantle", "An icy cloak: armored and healing, with weaker blows.", "minecraft:snow_block", armor=4, regen=0.7, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Winter Spirit", "The power of the cold.", "attack_damage", 1, "minecraft:snowball"),
        T("Ice-Edged Bearing", "The cold toughens your skin.", "armor", 1, "minecraft:iron_chestplate"),
        T("Rooted in the Frost", "Hard to push around.", "knockback_resistance", 0.1, "minecraft:ice"),
        T("Hush of Snow", "A little extra luck from quiet snow.", "luck", 1, "minecraft:lily_of_the_valley"),
    ],
    arts=[
        A("Winter's Breath", "Exhale a cone of killing cold that slows and blinds.", "debuff_cone", "minecraft:powder_snow_bucket", effect="slowness", effect2="blindness", seconds=7, seconds2=3, amp=1, vfx="WIND_TORNADO"),
        A("Fimbulvetr's Hush", "Everything near you is locked in ice for a moment.", "stun_ring", "minecraft:blue_ice", radius=5, seconds=2, vfx="WATER_RING"),
        A("Whiteout Drag", "A swirl of snow drags the monsters toward you.", "pull", "minecraft:snowball", radius=8, strength=1.0, targets=5, damage=4, vfx="WIND_TORNADO"),
        A("Glacier Lane", "A road of ice runs ahead and cracks as it goes.", "ground_wave", "minecraft:packed_ice", damage=7, length=12, width=3, vfx="STONE_SPIKES"),
        A("Frost Aegis", "A thick shell of ice absorbs a large amount of damage.", "shield", "minecraft:ice", absorb_hearts=6, seconds=8, vfx="MIRROR_PANE"),
        A("Spreading Rime", "A bolt of frost that leaps from monster to monster.", "chain_hit", "minecraft:prismarine_crystals", damage=5, jumps=4, vfx="WATER_BURST"),
        A("Snowburst", "A blast of snow in a ring around you.", "ring_burst", "minecraft:snow_block", damage=6, radius=4.5, knockback=0.9, vfx="WATER_RING"),
    ],
    passives=[
        P("Winter-Born", "You take much less freezing damage.", "resist", "minecraft:powder_snow_bucket", damage_kind="freeze", pct=0.5),
        P("Chill Aura", "Monsters near you are constantly slowed.", "aura", "minecraft:blue_ice", effect="slowness", target="enemies", radius=7),
        P("Snowfall Mantle", "Under rain or snow you take less damage.", "reduce_when", "minecraft:snow_block", condition="raining", pct=0.15),
        P("Frostbite", "Your spells may slow the monster.", "on_hit_effect", "minecraft:ice", effect="slowness", seconds=5, chance=0.35),
        P("Hoarfrost Thorns", "A share of the damage you take returns as frost.", "thorns", "minecraft:prismarine_shard", pct=0.2),
        P("Thaw Recovery", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=14),
        P("Foe of Flame", "You deal more damage to burning monsters.", "dmg_vs", "minecraft:fire_charge", category="burning", bonus=0.3),
    ],
    finishers=[
        A("Fimbulvetr: Long Winter", "The whole room freezes: every creature in range is locked in place.", "stun_ring", "minecraft:nether_star", radius=8, seconds=3, targets=10, cooldown=1500, vfx="WATER_RING"),
        A("Wall of White Silence", "A huge wave of ice rolls across the ground ahead.", "ground_wave", "minecraft:blue_ice", damage=22, length=18, width=5, knockback=1.0, cooldown=1300, vfx="STONE_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 9
LINES.append(Line(
    id="luminous_wind", name="Luminous Wind", franchise=F,
    desc="The quiet elf's wind: a hooded silence, a breeze that helps friends and cuts foes, and a precise blow when the moment is right. Part assassin, part healer.",
    color="#6fd6a8", background="forest", icon="minecraft:glow_berries",
    book_name="Hooded Notes of the Quiet Elf", book_lore="Revenge keeps well. Kindness keeps better.",
    stances=[
        S("Hooded Silence", "Move like a rumor: quicker, sharper crits, thin armor.", "minecraft:leather_helmet", speed_pct=0.1, crit_chance=0.1, armor=-1, vfx="WIND_RING"),
        S("Lion's Reserve", "Hold back and mend: armored and healing, with gentle blows.", "minecraft:shield", armor=3, regen=0.5, damage_pct=-0.05, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Elven Grace", "Light, quiet feet.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Silent Edge", "A precise hand.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Wind in the Lungs", "More maximum health.", "max_health", 2, "minecraft:golden_carrot"),
    ],
    arts=[
        A("Wind Veil", "A swirl of luminous dust blinds the creatures around you while you slip away.", "smoke", "minecraft:glowstone_dust", radius=5, seconds=6, vfx="WIND_RING"),
        A("Gale Step", "Slip a few blocks forward on a gust.", "blink", "minecraft:feather", distance=9, vfx="WIND_SLASH"),
        A("Luminous Needles", "A fan of small wind-blades flies at the monster you look at.", "barrage", "minecraft:iron_nugget", damage=2.5, shots=7, range=16, vfx="WIND_SLASH"),
        A("Wind's Blessing", "A kind breeze: you and your allies gain Speed.", "buff_aura", "minecraft:white_dye", effect="speed", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Soothing Gale", "A soft wind heals you and the allies near you.", "heal_pulse", "minecraft:honey_bottle", amount=6, radius=8, vfx="SPIRIT_AURA"),
        A("Elven Execution", "One quiet, accurate blow, deadly against a wounded monster.", "execute", "minecraft:wooden_sword", damage=9, bonus=1.0, vfx="WIND_SLASH"),
        A("Cutting Wind", "A cone of wind that weakens everything in it.", "debuff_cone", "minecraft:string", effect="weakness", effect2="none", seconds=7, amp=1, vfx="WIND_TORNADO"),
    ],
    passives=[
        P("Elf's Reflexes", "You may avoid a blow completely.", "dodge", "minecraft:rabbit_foot", chance=0.12),
        P("Unseen Under the Hood", "While sneaking you fade from sight.", "self_effect_when", "minecraft:glass", effect="invisibility", condition="sneaking"),
        P("Opening Strike", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.16, mult=1.9),
        P("Wind Companions", "Allies near you run faster.", "aura", "minecraft:feather", effect="speed", target="allies", radius=8),
        P("Meditative Breath", "Crouched, your wounds close.", "regen_when", "minecraft:glow_berries", condition="sneaking", per_second=0.9),
        P("Forest Landing", "You take much less fall damage.", "fall_guard", "minecraft:oak_sapling", pct=0.5),
        P("Merciless Moment", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:iron_nugget", threshold=0.2, bonus=0.7),
    ],
    finishers=[
        A("Luminous Wind: Mercy and Reckoning", "A great gust of light that heals you and your allies in a wide ring.", "heal_pulse", "minecraft:golden_apple", amount=20, radius=11, cooldown=1500, vfx="SPIRIT_AURA"),
        A("Hooded Verdict", "Appear before a single monster and end it in one cut.", "execute", "minecraft:netherite_sword", damage=30, range=6, bonus=2.0, cooldown=1300, vfx="WIND_SLASH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 10
LINES.append(Line(
    id="braver_command", name="Braver's Command", franchise=F,
    desc="Lead the line from the front with a spear, a plan and a very good eye for weak points. A tactician-warrior line of marks, rallying calls and thrown spears. Inspired by the pallum captain of the story.",
    color="#e2c25a", background="tower", icon="minecraft:golden_helmet",
    book_name="Tactics of the Brave Captain", book_lore="Courage is a method, not a mood.",
    stances=[
        S("Vanguard's Brave Heart", "Take the front: more damage and armor, slightly slower.", "minecraft:golden_helmet", damage_pct=0.12, armor=2, speed_pct=-0.05, vfx="MAGIC_CIRCLE"),
        S("Tactician's Calm", "Read the field: nimble and healing, with lighter hits and armor.", "minecraft:map", speed_pct=0.1, regen=0.6, damage_pct=-0.05, armor=-1, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Spear Arm", "Disciplined strikes.", "attack_damage", 1.5, "minecraft:trident"),
        T("Captain's Guard", "Reliable armor.", "armor", 1.5, "minecraft:iron_chestplate"),
        T("Long Reach", "A spear's reach.", "entity_interaction_range", 0.5, "minecraft:lead"),
        T("Veteran's Health", "A veteran's endurance.", "max_health", 2, "minecraft:cooked_beef"),
    ],
    arts=[
        A("Weak Point Call", "Spot the opening and call it out; the monster takes extra damage.", "mark", "minecraft:spyglass", bonus=0.35, range=24, vfx="MAGIC_CIRCLE"),
        A("Pallum's Rally", "A rousing call: you and your allies gain Strength.", "buff_aura", "minecraft:goat_horn", effect="strength", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Spear Throw", "A long, hard throw that pierces a line of monsters.", "ray_shot", "minecraft:trident", damage=9, range=24, pierce=3, vfx="WEAPON_CONSTRUCTS"),
        A("Spear Rush", "A fast lunge through the monsters ahead.", "dash_strike", "minecraft:golden_boots", distance=7, damage=8, vfx="WIND_SLASH"),
        A("Whirling Spear", "The spear strikes one monster and carries on to the next.", "chain_hit", "minecraft:stick", damage=5, jumps=4, vfx="WIND_SLASH"),
        A("Braver's Challenge", "Step forward and every monster nearby turns to face the captain.", "taunt", "minecraft:bell", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
        A("Shield-Wall Thrust", "A forward thrust that fans out into a cone of force.", "cone_strike", "minecraft:iron_sword", damage=8, range=4.2, half_angle=45, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Rallying Presence", "Allies near you hit harder: Strength.", "aura", "minecraft:white_banner", effect="strength", target="allies", radius=8),
        P("Monster Expert", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=0.2),
        P("Drilled Combination", "Every few blows in a row hit much harder.", "combo", "minecraft:golden_sword", hits=4, bonus=0.5),
        P("Eye for the Throat", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Close the Opening", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:trident", threshold=0.25, bonus=0.6),
        P("Field Dressing", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=14),
        P("Shielded Advance", "You take much less projectile damage.", "resist", "minecraft:shield", damage_kind="projectile", pct=0.3),
    ],
    finishers=[
        A("Hero's Command", "The order that turns a fight: you and your allies gain a long, strong surge of Strength.", "buff_aura", "minecraft:beacon", effect="strength", radius=13, seconds=25, amp=1, cooldown=1600, vfx="MAGIC_CIRCLE"),
        A("Gungnir Throw", "A great spear thrown the length of the room, piercing everything.", "ray_shot", "minecraft:nether_star", damage=28, range=34, pierce=4, cooldown=1400, vfx="WEAPON_CONSTRUCTS"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 11
LINES.append(Line(
    id="amazon_berserker", name="Amazon Berserker", franchise=F,
    desc="Fight like the warrior-women of the Amazons: huge swings, a rage that feeds on its own wounds and a charge that does not care who is in the way.",
    color="#e0453a", background="blood", icon="minecraft:netherite_axe",
    book_name="War Songs of the Amazon Tribe", book_lore="Fear is a thing other people have.",
    stances=[
        S("Berserk Stance", "Rage uncaged: huge damage, a little faster, almost no armor.", "minecraft:redstone_block", damage_pct=0.35, armor=-4, speed_pct=0.05, vfx="FIRE_BURST"),
        S("Amazon Discipline", "Hold the rage in: sturdy and steadily healing, with softer blows.", "minecraft:iron_chestplate", armor=3, regen=0.5, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Tribal Might", "Arms like a siege engine.", "attack_damage", 2, "minecraft:netherite_axe"),
        T("Warrior's Vigor", "More maximum health.", "max_health", 3, "minecraft:cooked_beef"),
        T("Light-Footed Giant", "Quick for her size.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Amazon Spin", "Spin the great blade in a full ring.", "whirlwind", "minecraft:iron_axe", damage=3.5, hits=5, radius=4, vfx="WIND_RING"),
        A("Greatsword Smash", "Leap and smash the blade down on the monsters.", "leap_slam", "minecraft:anvil", damage=10, radius=3.5, height=1.0, knockback=1.2, vfx="STONE_SPIKES"),
        A("Rage Awakens", "The Berserk takes hold: a strong surge of Strength and Speed.", "buff_self", "minecraft:redstone", effect="strength", effect2="speed", seconds=14, amp=1, vfx="FIRE_BURST"),
        A("Skyward Cleave", "An upward cut that hurls the monster high.", "uppercut", "minecraft:iron_axe", damage=9, launch=1.0, vfx="WIND_SLASH"),
        A("Berserker Charge", "Charge in a straight line, knocking everything aside.", "dash_strike", "minecraft:iron_horse_armor", distance=8, damage=8, vfx="FLAME_TRAIL"),
        A("Blood Frenzy Bite", "A savage blow that tears life back from the monster.", "drain_bite", "minecraft:beef", damage=8, heal_pct=0.4, vfx="FIRE_BURST"),
        A("War Cry Blast", "A thunderous cry knocks everything around you back.", "ring_burst", "minecraft:goat_horn", damage=6, radius=4.5, knockback=1.4, vfx="WIND_RING"),
    ],
    passives=[
        P("Rage Feeds on Wounds", "At low health your blows land much harder.", "dmg_when", "minecraft:redstone", condition="low_health", bonus=0.4),
        P("Blood-Drunk", "Your blows heal you.", "lifesteal", "minecraft:rotten_flesh", pct=0.08),
        P("Brutal Instinct", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.14, mult=1.9),
        P("Triumph of the Tribe", "Each kill heals you and gives you Strength.", "kill_reward", "minecraft:bone", heal=4, effect="strength", seconds=6),
        P("Cleaver's Shove", "Your blows may shove the monster back.", "knockback_on_hit", "minecraft:piston", strength=0.7, chance=0.4),
        P("Fighter's Reprieve", "While in a fight your wounds slowly close.", "regen_when", "minecraft:golden_carrot", condition="in_combat", per_second=0.7),
        P("Rising Fury", "Blows in a row grow stronger.", "momentum", "minecraft:blaze_powder", max_stacks=6),
    ],
    finishers=[
        A("Berserk: Hundred-Blade Rage", "A storm of great-sword cuts spins up around you.", "whirlwind", "minecraft:netherite_axe", damage=6, hits=7, radius=5, interval=3, cooldown=1300, vfx="WIND_TORNADO"),
        A("Greatsword's Fall", "Leap and fall as a mountain, ending in a huge impact.", "leap_slam", "minecraft:netherite_block", damage=28, radius=6, height=1.2, knockback=1.8, cooldown=1400, vfx="STONE_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 12
LINES.append(Line(
    id="dwarf_guardian", name="Dwarf Guardian", franchise=F,
    desc="Planted behind a shield and wielding a heavy axe: take the hit, return it, drink to your health and hold the line while the party does what it must.",
    color="#8a6b3a", background="ruin", icon="minecraft:diamond_chestplate",
    book_name="The Guardian's Tankard Log", book_lore="He is not slow. He simply refuses to be elsewhere.",
    stances=[
        S("Fortress Stance", "Plant your feet: huge armor, near-immune to knockback, very slow.", "minecraft:iron_block", armor=7, knockback_res=0.6, speed_pct=-0.18, vfx="EARTH_SPIKES"),
        S("Axe-Bearer's Stance", "Raise the axe: stronger blows with a modest slowdown.", "minecraft:iron_axe", damage_pct=0.2, armor=1, speed_pct=-0.05, vfx="STONE_SPIKES"),
    ],
    stats=[
        T("Dwarven Plate", "Armor that outlasts the owner.", "armor", 2, "minecraft:diamond_chestplate"),
        T("Anvil Constitution", "A huge pool of health.", "max_health", 4, "minecraft:golden_apple"),
        T("Rooted Boots", "Almost impossible to push.", "knockback_resistance", 0.15, "minecraft:iron_boots"),
        T("Toughened Hide", "Tough skin under the plate.", "armor_toughness", 1, "minecraft:netherite_scrap"),
    ],
    arts=[
        A("Wall of the Dwarf", "Raise the shield: a hardy wall of absorption with Resistance.", "shield", "minecraft:shield", absorb_hearts=6, resist_amp=1, vfx="MIRROR_PANE"),
        A("Axe Sweep", "A heavy cone of axe-work in front of the shield.", "cone_strike", "minecraft:iron_axe", damage=9, range=3.8, half_angle=50, vfx="WIND_SLASH"),
        A("Guardian's Roar", "A bellow that makes every hostile monster look your way.", "taunt", "minecraft:goat_horn", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
        A("Hammer Quake", "Drive the axe haft into the floor; a shockwave runs out ahead.", "ground_wave", "minecraft:anvil", damage=7, length=10, width=3, knockback=1.2, vfx="STONE_SPIKES"),
        A("Dwarven Spirits", "A swig from the flask: heal and regenerate.", "heal_self", "minecraft:honey_bottle", amount=9, regen_seconds=7, vfx="SPIRIT_AURA"),
        A("Hold the Line", "A bellowed order: you and your allies gain Resistance.", "buff_aura", "minecraft:bell", effect="resistance", radius=9, seconds=12, vfx="MAGIC_CIRCLE"),
        A("Stalwart Reprisal", "For a time, a big share of the damage you take is thrown back at the attacker.", "ward_reflect", "minecraft:crying_obsidian", reflect_pct=0.4, vfx="EARTH_SPIKES"),
    ],
    passives=[
        P("Unmoved Guardian", "In the thick of a fight you take less damage.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.13),
        P("Blast-Proof", "You take much less explosion damage.", "resist", "minecraft:tnt", damage_kind="explosion", pct=0.35),
        P("Shield Bash Thorns", "Part of the damage you take returns to the attacker.", "thorns", "minecraft:iron_nugget", pct=0.25),
        P("Dwarf's Constitution", "Your wounds close slowly, even out of combat.", "mending", "minecraft:iron_block", seconds=12, amount=1),
        P("Stand Your Ground", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Shield Check", "Your blows may shove the monster back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.4),
        P("Prospector's Eye", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:raw_gold", chance=0.15),
    ],
    finishers=[
        A("Bastion Unbreakable", "A mountain of absorption and resistance wraps around you.", "shield", "minecraft:beacon", absorb_hearts=12, seconds=14, resist_amp=1, cooldown=1500, vfx="MIRROR_PANE"),
        A("Earthbreaker Swing", "One swing that cracks the floor outward in a ring.", "ring_burst", "minecraft:anvil", damage=22, radius=6.5, targets=8, knockback=1.8, cooldown=1300, vfx="STONE_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 13
LINES.append(Line(
    id="werewolf_gale_kick", name="Werewolf's Gale Kick", franchise=F,
    desc="A beast-person kicker in magic boots: charging, flipping, ricocheting kicks and a howl that rattles the nerves. Fast, rude and hard to pin down.",
    color="#8c9bb0", background="storm", icon="minecraft:iron_boots",
    book_name="Kicker's Notes on Flosvirt", book_lore="The boots do half the talking. The other half is the kick.",
    stances=[
        S("Hunter's Prowl", "Low and tense: much faster and a bit stronger, with little armor.", "minecraft:rabbit_foot", speed_pct=0.2, damage_pct=0.1, armor=-3, vfx="WIND_SLASH"),
        S("Stubborn Wolf", "Dig in and refuse to move: sturdier and slowly healing, with gentler blows.", "minecraft:bone", armor=3, knockback_res=0.4, regen=0.3, damage_pct=-0.05, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Boot-Driven Power", "Heavy kicks.", "attack_damage", 1.5, "minecraft:iron_boots"),
        T("Wolf's Legs", "Fast legs.", "movement_speed", 0.05, "minecraft:golden_boots", op="mult"),
        T("Spring in the Step", "A taller jump.", "jump_strength", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Thick Fur", "Tough hide.", "armor", 1, "minecraft:leather_chestplate"),
    ],
    arts=[
        A("Gale Kick Rush", "Burst forward in a spinning kick through the monsters.", "dash_strike", "minecraft:iron_boots", distance=8, damage=8, vfx="WIND_SLASH"),
        A("Crescent Kick", "A rising kick that launches the monster in front of you.", "uppercut", "minecraft:leather_boots", damage=8, launch=0.9, vfx="WIND_SLASH"),
        A("Ricochet Kick", "A kick that bounces from monster to monster.", "chain_hit", "minecraft:golden_boots", damage=5, jumps=4, vfx="WIND_RING"),
        A("Wolf Pounce Step", "Teleport a short way forward in a flash.", "blink", "minecraft:rabbit_foot", distance=8, vfx="SPATIAL_RIFT"),
        A("Wolf's Stomp", "Jump and stomp, rattling everything below.", "leap_slam", "minecraft:anvil", damage=9, radius=3.2, height=1.0, vfx="STONE_SPIKES"),
        A("Howl of Contempt", "A mocking howl rattles a cone of monsters, weakening and blinding them.", "debuff_cone", "minecraft:goat_horn", effect="weakness", effect2="blindness", seconds=7, seconds2=3, vfx="WIND_TORNADO"),
        A("Flurry of Kicks", "A whirling flurry of kicks in all directions.", "whirlwind", "minecraft:iron_boots", damage=3.5, hits=5, radius=3.5, vfx="WIND_RING"),
    ],
    passives=[
        P("Kick Rhythm", "Blows in a row grow stronger.", "momentum", "minecraft:gunpowder", max_stacks=6, per_stack=0.05),
        P("Charging Wolf", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.3),
        P("Boot Heel Crit", "Your kicks may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Taste of Blood", "Your blows heal you.", "lifesteal", "minecraft:beef", pct=0.07),
        P("Moon-Chaser", "At night you run faster.", "self_effect_when", "minecraft:clock", effect="speed", condition="night"),
        P("Monster Hunter's Pride", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=0.2),
        P("Wolf's Reflexes", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
    ],
    finishers=[
        A("Flosvirt: Raging Tempest", "A whirlwind of magic-boot kicks with no pause.", "whirlwind", "minecraft:diamond_boots", damage=6, hits=7, radius=5, interval=3, cooldown=1300, vfx="WIND_TORNADO"),
        A("Fang-Strike Charge", "Run the whole line down in one rolling dash.", "dash_strike", "minecraft:netherite_boots", distance=11, damage=22, targets=6, cooldown=1400, vfx="FLAME_TRAIL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 14
LINES.append(Line(
    id="freya_charm", name="Charm of the Goddess", franchise=F,
    desc="Beauty as a weapon: monsters drawn in, entranced, weakened and softened by a goddess's favour that spares the allies. Inspired by the Goddess of Beauty's familia.",
    color="#e56bd6", background="dream", icon="minecraft:pink_dye",
    book_name="Silver Hair and Silver Eyes", book_lore="To be looked at is to be spoken for.",
    stances=[
        S("Enthralling Presence", "A radiant calm: armored and healing, with gentler blows.", "minecraft:pink_dye", armor=2, regen=0.5, damage_pct=-0.1, vfx="SPIRIT_AURA"),
        S("Silver Gaze", "A sharper, colder look: more damage and speed, thin armor.", "minecraft:ender_eye", damage_pct=0.2, speed_pct=0.05, armor=-2, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Grace of the Goddess", "More maximum health.", "max_health", 2, "minecraft:glistering_melon_slice"),
        T("Silken Reflexes", "Light, quick steps.", "movement_speed", 0.03, "minecraft:feather", op="mult"),
        T("Favoured by Fortune", "A hand of luck.", "luck", 1.5, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Beckoning Call", "A pull on every monster around you, drawing them to your feet.", "pull", "minecraft:lead", radius=9, strength=1.1, targets=6, damage=3, vfx="THREAD_LINE"),
        A("Chosen Favorite", "Choose a monster; it takes extra damage while under your gaze.", "mark", "minecraft:rose_bush", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Entranced", "Monsters near you stop where they are, lost in your gaze.", "stun_ring", "minecraft:ender_eye", radius=5, seconds=2, vfx="MAGIC_CIRCLE"),
        A("Goddess's Favor", "You and your allies feel the favour: Strength for a while.", "buff_aura", "minecraft:white_banner", effect="strength", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Silver Blessing", "A shimmering blessing that heals you and the allies around you.", "heal_pulse", "minecraft:honey_bottle", amount=6, radius=8, vfx="SPIRIT_AURA"),
        A("Jealous Retort", "For a short time a good share of the damage you take is thrown back.", "ward_reflect", "minecraft:crying_obsidian", reflect_pct=0.35, vfx="MIRROR_PANE"),
        A("Shimmering Mist", "A lovely, opaque mist that blinds the creatures around you.", "smoke", "minecraft:pink_dye", radius=5, seconds=6, vfx="SPIRIT_AURA"),
    ],
    passives=[
        P("Overwhelming Beauty", "Monsters near you are constantly weakened.", "aura", "minecraft:rose_bush", effect="weakness", target="enemies", radius=7),
        P("Untouchable at Her Best", "At full health you take less damage.", "reduce_when", "minecraft:gold_ingot", condition="full_health", pct=0.15),
        P("Spurned Reprisal", "Whoever strikes you may be slowed.", "on_hurt_retaliate", "minecraft:cobweb", effect="slowness", seconds=4, chance=0.3),
        P("Gifts of Admirers", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.18),
        P("Basking in Daylight", "By day your wounds close faster.", "regen_when", "minecraft:sunflower", condition="day", per_second=0.8),
        P("Elusive Muse", "You may slip a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
        P("Tribute", "Each kill heals you.", "kill_reward", "minecraft:glistering_melon_slice", heal=4, effect="none", seconds=0),
    ],
    finishers=[
        A("Charm: Absolute Enthrallment", "Every creature in a wide ring is lost in your gaze and cannot move.", "stun_ring", "minecraft:nether_star", radius=8, seconds=3, targets=10, cooldown=1500, vfx="MAGIC_CIRCLE"),
        A("The Goddess's Blessing", "A mighty blessing: you and your allies gain long Resistance.", "buff_aura", "minecraft:beacon", effect="resistance", radius=13, seconds=25, amp=1, cooldown=1600, vfx="SPIRIT_AURA"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 15
LINES.append(Line(
    id="boar_warrior_might", name="Boar Warrior's Might", franchise=F,
    desc="The strongest warrior of the strongest familia: earth-shaking waves, a stampede nobody stops and a hand that picks monsters up and throws them. Inspired by the boar-man champion of the story.",
    color="#6b4a2b", background="sand", icon="minecraft:mace",
    book_name="Chronicle of the Unbeaten Warrior", book_lore="He has never needed to be told twice. He has rarely needed to be told once.",
    stances=[
        S("Champion's Stance", "Planted and unshakable: huge armor and almost immovable, but ponderous.", "minecraft:iron_block", armor=6, knockback_res=0.6, speed_pct=-0.15, vfx="EARTH_SPIKES"),
        S("Warrior's Onslaught", "Swing out: more damage and speed, with lighter armor.", "minecraft:mace", damage_pct=0.25, speed_pct=0.05, armor=-1, vfx="STONE_SPIKES"),
    ],
    stats=[
        T("Champion's Strength", "Pure, overwhelming force.", "attack_damage", 2, "minecraft:mace"),
        T("Boar Hide", "A pelt like armor.", "armor", 2, "minecraft:iron_chestplate"),
        T("Untouchable Mass", "Hard to move.", "knockback_resistance", 0.15, "minecraft:anvil"),
        T("Warrior's Heart", "A huge pool of health.", "max_health", 4, "minecraft:cooked_beef"),
    ],
    arts=[
        A("Earthbreak", "Drive the weapon down and send a crack of force along the floor.", "ground_wave", "minecraft:cobblestone", damage=8, length=12, width=3, knockback=1.3, vfx="STONE_SPIKES"),
        A("Stampede", "Charge forward, trampling everything in the way.", "dash_strike", "minecraft:iron_horse_armor", distance=8, damage=8, vfx="FLAME_TRAIL"),
        A("Hardened Muscle", "Tense every muscle: Strength and Resistance for a while.", "buff_self", "minecraft:iron_ingot", effect="strength", effect2="resistance", seconds=16, vfx="EARTH_SPIKES"),
        A("Boar's Verdict", "One huge swing, devastating against wounded monsters.", "execute", "minecraft:netherite_axe", damage=10, bonus=0.9, vfx="WIND_SLASH"),
        A("Seize and Throw", "Grab the nearby monsters and fling them toward you.", "pull", "minecraft:chain", radius=8, strength=1.2, targets=5, damage=5, vfx="SEAL_CHAINS"),
        A("Shoulder-Slam Shock", "A mighty shoulder charge sends a shockwave around you.", "ring_burst", "minecraft:obsidian", damage=7, radius=4.2, knockback=1.3, vfx="STONE_SPIKES"),
        A("Champion's Sweep", "A massive cone-shaped sweep of the weapon.", "cone_strike", "minecraft:mace", damage=10, range=4, half_angle=55, knockback=1.0, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Warrior's Calm", "In the thick of a fight you take less damage.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.13),
        P("Giant-Killer", "You deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.25),
        P("Pushing Power", "Your blows may shove the monster back.", "knockback_on_hit", "minecraft:piston", strength=0.8, chance=0.45),
        P("Armored Spikes", "A share of the damage you take returns to the attacker.", "thorns", "minecraft:pointed_dripstone", pct=0.2),
        P("Unbowed Champion", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Planted Landing", "You take much less fall damage.", "fall_guard", "minecraft:anvil", pct=0.6),
        P("Strength Feeds Strength", "Your blows heal you.", "lifesteal", "minecraft:cooked_beef", pct=0.06),
    ],
    finishers=[
        A("Warrior's Wrath", "The ground splits ahead of you in one titanic shockwave.", "ground_wave", "minecraft:netherite_block", damage=24, length=18, width=5, knockback=1.8, cooldown=1400, vfx="STONE_SPIKES"),
        A("Unstoppable Charge", "Run the whole line down, hitting everyone on it.", "dash_strike", "minecraft:netherite_axe", distance=11, damage=24, targets=6, cooldown=1300, vfx="FLAME_TRAIL"),
    ],
))
