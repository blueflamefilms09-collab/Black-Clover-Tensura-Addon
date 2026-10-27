"""DanMachi: 27 unique skill lines (familia Blessings and Statuses, adventurer Skills and Magic, chant magic, Developmental Abilities, Argonaut, familia roles).

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
        T("Gale-Cushioned Landing", "The gale softens every landing.", "safe_fall_distance", 2, "minecraft:feather"),
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
        P("Tactical Combination", "Every few blows in a row hit much harder.", "combo", "minecraft:golden_sword", hits=4, bonus=0.5),
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
        A("Hold the Doorway", "A bellowed order: you and your allies gain Resistance.", "buff_aura", "minecraft:bell", effect="resistance", radius=9, seconds=12, vfx="MAGIC_CIRCLE"),
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

# ---------------------------------------------------------------------------------------------------------------------------------- 16
LINES.append(Line(
    id="crozzo_magic_sword", name="Crozzo's Magic Sword", franchise=F,
    desc="A smith's gamble: a sword that fires real flame with every swing and shatters when the fire is spent. Overload for tremendous damage, then hold the shard.",
    color="#d9421c", background="magma", icon="minecraft:netherite_sword",
    book_name="Forging Notes on a Sword That Burns Out", book_lore="A weapon that lasts forever is a weapon that was never really used.",
    stances=[
        S("Overload Stance", "Push the sword to its limit: huge damage, fragile for it.", "minecraft:fire_charge", damage_pct=0.35, armor=-3, vfx="FLAME_EXPLOSION"),
        S("Careful Edge", "Hold the flame back: sturdier, steadier, and the blade lasts.", "minecraft:iron_sword", armor=3, regen=0.3, damage_pct=0.0, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Smith's Hand", "A strong, steady forearm.", "attack_damage", 1.5, "minecraft:iron_pickaxe"),
        T("Forge-Hardened", "Years at the fire thicken the skin.", "armor", 1, "minecraft:iron_chestplate"),
        T("Burning Stamina", "More maximum health.", "max_health", 2, "minecraft:golden_apple"),
    ],
    arts=[
        A("Magic Sword Swing", "A fiery arc that scorches every monster in front.", "cone_strike", "minecraft:netherite_sword", damage=8, range=4, half_angle=50, vfx="FIRE_BURST"),
        A("Crozzo Flame Slash", "The blade fires a long bar of flame that pierces a line of monsters.", "ray_shot", "minecraft:blaze_rod", damage=9, range=22, pierce=2, vfx="FIRE_SPIRAL"),
        A("Whetstone Ritual", "A quick sharpening: Strength and Haste for a while.", "buff_self", "minecraft:flint", effect="strength", effect2="haste", seconds=14, vfx="FIRE_BURST"),
        A("Lava Rift", "Strike the ground and a seam of flame races away ahead.", "ground_wave", "minecraft:magma_block", damage=7, length=12, width=2, vfx="FLAME_TRAIL"),
        A("Overload Blast", "The sword vents all at once in a ring of fire.", "ring_burst", "minecraft:fire_charge", damage=7, radius=4.5, knockback=1.0, vfx="FLAME_EXPLOSION"),
        A("Final Cut", "One heavy swing, much stronger against a wounded monster.", "execute", "minecraft:iron_axe", damage=10, bonus=0.9, vfx="WIND_SLASH"),
        A("Elder Flame Strike", "A pillar of flame drops on the place you look at.", "meteor_drop", "minecraft:lava_bucket", damage=14, radius=4, range=22, vfx="FLAME_EXPLOSION"),
    ],
    passives=[
        P("Flame-Edged Fury", "While you are on fire your blows hit harder.", "dmg_when", "minecraft:blaze_powder", condition="on_fire", bonus=0.3),
        P("Forge Hardened", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.4),
        P("Heat-Cracked Crits", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Searing Cut", "Your cuts may make the monster glow.", "on_hit_effect", "minecraft:glowstone_dust", effect="glowing", seconds=8, chance=0.35),
        P("Backlash", "A share of the damage you take burns the attacker.", "thorns", "minecraft:magma_cream", pct=0.2),
        P("Ore Sense", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:raw_iron", chance=0.15),
        P("Break the Weak", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.7),
    ],
    finishers=[
        A("Crozzo: Last Light", "The sword spends itself in one gigantic line of flame.", "ray_shot", "minecraft:nether_star", damage=28, range=32, pierce=4, cooldown=1400, vfx="FIRE_SPIRAL"),
        A("Blade Breaks", "The blade shatters in a huge ring of burning shards.", "ring_burst", "minecraft:tnt", damage=26, radius=7, targets=8, knockback=1.8, cooldown=1500, vfx="FLAME_EXPLOSION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 17
LINES.append(Line(
    id="hephaistos_forge", name="Hephaistos Forge-Master", franchise=F,
    desc="Wear your workshop on the battlefield: tempered plates, hammer arcs, whetstone focus and a party that comes out better equipped than it went in.",
    color="#c97a2b", background="iron", icon="minecraft:blast_furnace",
    book_name="The Apprentice Smith's Catalogue", book_lore="Everything breaks. A smith is the one who brings the next one.",
    stances=[
        S("Forge Stance", "Stand like an anvil: heavy armor, slow healing and a slow step.", "minecraft:anvil", armor=4, regen=0.5, speed_pct=-0.1, vfx="FIRE_BURST"),
        S("Hammer Swing", "Wind up the hammer: much more damage, a little less guard.", "minecraft:iron_pickaxe", damage_pct=0.25, armor=-1, vfx="STONE_SPIKES"),
    ],
    stats=[
        T("Hammer Arm", "A smith's forearm.", "attack_damage", 1.5, "minecraft:iron_axe"),
        T("Apron of Plates", "Layers of tempered leather and iron.", "armor", 1.5, "minecraft:iron_chestplate"),
        T("Furnace-Fed Vigor", "A strong heart from a hot workshop.", "max_health", 3, "minecraft:cooked_beef"),
        T("Quick Anvil Hands", "Fast, accurate strikes.", "attack_speed", 0.05, "minecraft:clock", op="mult"),
    ],
    arts=[
        A("Whetstone Focus", "A moment with the stone: Haste and Resistance for a while.", "buff_self", "minecraft:flint", effect="haste", effect2="resistance", seconds=14, vfx="FIRE_BURST"),
        A("Anvil Drop", "Leap and bring your weight down like an anvil.", "leap_slam", "minecraft:anvil", damage=9, radius=3.2, height=0.9, vfx="STONE_SPIKES"),
        A("Tempered Gear", "Temper the party's gear on the spot: you and your allies gain Resistance.", "buff_aura", "minecraft:smithing_table", effect="resistance", radius=9, seconds=12, vfx="MAGIC_CIRCLE"),
        A("Hammer Arc", "A wide arc of the forge hammer.", "cone_strike", "minecraft:iron_pickaxe", damage=8, range=3.8, half_angle=50, knockback=0.9, vfx="WIND_SLASH"),
        A("Forge Plate", "A quickly-riveted plate of absorption.", "shield", "minecraft:iron_block", absorb_hearts=6, seconds=8, vfx="MIRROR_PANE"),
        A("Furnace Smoke", "A cloud of coal smoke blinds the monsters around you.", "smoke", "minecraft:coal", radius=5, seconds=6, vfx="ASH_FORMATION"),
        A("Spark Shower", "A shower of sparks from a heavy strike bursts out in a ring.", "ring_burst", "minecraft:flint_and_steel", damage=6, radius=4, knockback=0.9, vfx="FIRE_BURST"),
    ],
    passives=[
        P("Self-Repairing Gear", "Your wounds close slowly, even out of combat.", "mending", "minecraft:iron_block", seconds=12, amount=1),
        P("Master's Eye for Ore", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:raw_gold", chance=0.18),
        P("Craftsman's Experience", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Forge-Blast Training", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.35),
        P("Steady Breath at the Bellows", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Cellar Dweller's Haste", "Deep underground you work faster: Haste.", "self_effect_when", "minecraft:iron_pickaxe", effect="haste", condition="underground"),
        P("Cool in the Cellar", "Underground you take less damage.", "reduce_when", "minecraft:deepslate", condition="underground", pct=0.12),
    ],
    finishers=[
        A("Masterwork Plating", "A full set of masterwork plate: a large absorption shield and heavy Resistance.", "shield", "minecraft:netherite_chestplate", absorb_hearts=12, seconds=14, resist_amp=1, cooldown=1500, vfx="MIRROR_PANE"),
        A("Hammer of the Gods", "A falling hammer of forged light crashes down on the place you look at.", "meteor_drop", "minecraft:anvil", damage=28, radius=6.5, range=26, cooldown=1400, vfx="STONE_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 18
LINES.append(Line(
    id="cinder_ella", name="Cinder Ella", franchise=F,
    desc="Magic that changes your face until midnight: borrowed identities, blurred outlines and a mirror dress that sends blows back where they came from.",
    color="#d8b4ff", background="stellar", icon="minecraft:pumpkin",
    book_name="Twelve Strokes of a Bell", book_lore="All the magic ends at midnight. Everything else you did stays done.",
    stances=[
        S("Borrowed Face", "Someone else's confidence: quicker with sharper crits and little armor.", "minecraft:glass", speed_pct=0.1, crit_chance=0.1, armor=-1, vfx="MIRROR_PANE"),
        S("Midnight Bell", "Hold your breath and your composure: armored and slowly healing, with gentler blows.", "minecraft:bell", armor=3, regen=0.6, damage_pct=-0.1, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Light-Fingered Reflexes", "Quick hands.", "attack_speed", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Sure Little Feet", "Nimble steps.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Pocketful of Luck", "A brush of fortune.", "luck", 1.5, "minecraft:emerald"),
    ],
    arts=[
        A("Transformation Spell", "Take someone else's shape: Invisibility and Speed for a while.", "buff_self", "minecraft:pumpkin", effect="invisibility", effect2="speed", seconds=14, vfx="MIRROR_PANE"),
        A("Ashen Fog", "A cloud of ash and glitter blinds the monsters around you.", "smoke", "minecraft:charcoal", radius=5, seconds=6, vfx="ASH_FORMATION"),
        A("Ball-Gown Twirl", "Twirl in a flash and appear a few blocks ahead.", "blink", "minecraft:ender_pearl", distance=9, vfx="SPATIAL_RIFT"),
        A("Illusory Confusion", "A cone of dizzying images sickens and slows everything in it.", "debuff_cone", "minecraft:glass", effect="nausea", effect2="slowness", seconds=7, seconds2=5, vfx="MIRROR_PANE"),
        A("Mirror Dress", "For a short while, a lot of the damage you take bounces back.", "ward_reflect", "minecraft:glass_bottle", reflect_pct=0.4, vfx="MIRROR_PANE"),
        A("Study the Mark", "Take a good look at a monster; it takes extra damage while you do.", "mark", "minecraft:spyglass", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Glass Slipper Dash", "A swift dash that leaves a shimmer of glass behind.", "dash_strike", "minecraft:glass", distance=7, damage=7, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Unseen Little Thief", "While sneaking you fade from sight.", "self_effect_when", "minecraft:glass", effect="invisibility", condition="sneaking"),
        P("Slip Between Blows", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.13),
        P("Sticky Fingers", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.18),
        P("Keen Learner", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Midnight Strength", "At night your wounds close faster.", "regen_when", "minecraft:clock", condition="night", per_second=0.9),
        P("Dizzy Glamour", "Monsters near you are constantly sickened.", "aura", "minecraft:fermented_spider_eye", effect="nausea", target="enemies", radius=6),
        P("Cat's Landing", "You take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.5),
    ],
    finishers=[
        A("Midnight Masquerade", "A vast curtain of glitter and ash hides you from every monster in range.", "smoke", "minecraft:black_dye", radius=9, seconds=14, cooldown=1500, vfx="ASH_FORMATION"),
        A("Ball of Illusions", "You and your allies in a wide ring vanish from sight for a long while.", "buff_aura", "minecraft:ender_eye", effect="invisibility", radius=12, seconds=20, cooldown=1600, vfx="MIRROR_PANE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 19
LINES.append(Line(
    id="dungeon_supporter", name="Supporter's Pack", franchise=F,
    desc="The porter who makes the dive possible: potion rounds, thrown bombs, escape powder and a pack so well organized that loot never gets left behind.",
    color="#a98c5a", background="ocean", icon="minecraft:chest",
    book_name="The Supporter's Packing List", book_lore="If it's not in the pack, it was never going to save you.",
    stances=[
        S("Heavy Pack Stance", "A full pack and a straight back: tougher and healing, slower on your feet.", "minecraft:barrel", armor=3, regen=0.3, speed_pct=-0.1, vfx="SPIRIT_AURA"),
        S("Light-Packed Sprint", "Travel light: much faster, with less armor.", "minecraft:leather_boots", speed_pct=0.15, armor=-1, vfx="WIND_RING"),
    ],
    stats=[
        T("Porter's Back", "More maximum health from lugging a pack.", "max_health", 3, "minecraft:cooked_beef"),
        T("Sure-Footed Climber", "Step over rubble without losing pace.", "step_height", 0.5, "minecraft:ladder"),
        T("Quick Thinker", "A brush of luck.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Potion Rounds", "Hand out potions: you and your allies are healed.", "heal_pulse", "minecraft:potion", amount=7, radius=8, vfx="SPIRIT_AURA"),
        A("Thrown Bombs", "Hurl bombs from the pack at the monster you look at.", "barrage", "minecraft:gunpowder", damage=3, shots=6, range=14, vfx="FLAME_EXPLOSION"),
        A("Escape Powder", "A pinch of dust blinds nearby monsters so you can slip out.", "smoke", "minecraft:glowstone_dust", radius=5, seconds=6, vfx="ASH_FORMATION"),
        A("Quick Retreat", "Duck out of the fight a few blocks back.", "blink", "minecraft:leather_boots", distance=8, vfx="WIND_RING"),
        A("Spiced Rations", "A bite for everyone: you and your allies gain Speed.", "buff_aura", "minecraft:cooked_beef", effect="speed", radius=9, seconds=14, vfx="SPIRIT_AURA"),
        A("Bomb Pouch Burst", "The whole pouch goes off at once around you.", "ring_burst", "minecraft:tnt", damage=7, radius=4.2, knockback=1.2, vfx="FLAME_EXPLOSION"),
        A("Spot the Loot", "Point out the monster worth taking down first; it takes extra damage for a while.", "mark", "minecraft:spyglass", bonus=0.3, vfx="MAGIC_CIRCLE"),
    ],
    passives=[
        P("Pack Rat", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.22),
        P("Packed Lunch", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.35),
        P("Slide Down Slopes", "You take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.5),
        P("Bomb Handler", "You take much less explosion damage.", "resist", "minecraft:tnt", damage_kind="explosion", pct=0.4),
        P("Slippery Porter", "You may slip a blow completely.", "dodge", "minecraft:feather", chance=0.12),
        P("Fortune in the Pack", "In daylight your luck holds.", "self_effect_when", "minecraft:emerald", effect="luck", condition="day"),
        P("Patching Up", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=14),
    ],
    finishers=[
        A("Bombardment Pack", "Empty the whole pack on one monster in a stream of bombs.", "barrage", "minecraft:tnt", damage=5, shots=9, range=22, cooldown=1300, vfx="FLAME_EXPLOSION"),
        A("Emergency Potion Reserve", "The last potions come out all at once: a huge heal for you and your allies.", "heal_pulse", "minecraft:splash_potion", amount=20, radius=11, cooldown=1500, vfx="SPIRIT_AURA"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 20
LINES.append(Line(
    id="monster_tamer", name="Tamer's Bond", franchise=F,
    desc="Befriend what the dungeon throws at you: calling beasts, calming the wild, sharing health with creatures and fighting with their instincts.",
    color="#5c9e5a", background="forest", icon="minecraft:saddle",
    book_name="A Tamer's Bestiary", book_lore="Teeth mean something different when they like you.",
    stances=[
        S("Bonded Stance", "Stand with the bond at your back: armored and healing, with gentler blows.", "minecraft:lead", armor=2, regen=0.6, damage_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Beast-Aspect", "Borrow the beast's fury: more damage and speed, thin armor.", "minecraft:bone", damage_pct=0.2, speed_pct=0.1, armor=-2, vfx="WIND_RING"),
    ],
    stats=[
        T("Beastmaster's Frame", "More maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Wild Reflexes", "A beast's quick feet.", "movement_speed", 0.04, "minecraft:rabbit_foot", op="mult"),
        T("Calming Presence", "Steady hands, steady reach.", "entity_interaction_range", 0.4, "minecraft:lead"),
    ],
    arts=[
        A("Call of the Wild", "A strange call that pulls the monsters around you toward you.", "pull", "minecraft:lead", radius=9, strength=1.0, targets=5, damage=3, vfx="THREAD_LINE"),
        A("Beast-Aspect Pounce", "Spring like a hunting cat and crash down on the monsters.", "leap_slam", "minecraft:rabbit_foot", damage=8, radius=3.2, height=1.0, vfx="WIND_RING"),
        A("Lure", "Draw the attention of the hostile creatures nearby onto yourself.", "taunt", "minecraft:cooked_beef", radius=8, targets=5, vfx="DEVIL_CIRCLE"),
        A("Bonded Bite", "A bite alongside your bonded companion: you heal for a share of the damage.", "drain_bite", "minecraft:bone", damage=7, heal_pct=0.35, vfx="FIRE_BURST"),
        A("Shared Bond", "Share the bond with your allies: you all gain Speed.", "buff_aura", "minecraft:white_wool", effect="speed", radius=9, seconds=14, vfx="SPIRIT_AURA"),
        A("Calming Whistle", "A single bright note that stops everything nearby in its tracks.", "stun_ring", "minecraft:goat_horn", radius=5, seconds=2, vfx="WIND_RING"),
        A("Beast Medicine", "A quick poultice for you and the allies near you.", "heal_pulse", "minecraft:honey_bottle", amount=6, radius=8, vfx="SPIRIT_AURA"),
    ],
    passives=[
        P("Naturalist's Insight", "You deal more damage to animals.", "dmg_vs", "minecraft:beef", category="animal", bonus=0.25),
        P("Shared Heartbeat", "Allies near you slowly regenerate.", "aura", "minecraft:honey_block", effect="regeneration", target="allies", radius=8),
        P("Companion's Share", "Your blows heal you.", "lifesteal", "minecraft:beef", pct=0.06),
        P("Daylight Companionship", "By day your wounds close faster.", "regen_when", "minecraft:sunflower", condition="day", per_second=0.8),
        P("Bond's Protection", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Trail Rations", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Beast's Instinct", "You may avoid a blow completely.", "dodge", "minecraft:rabbit_foot", chance=0.1),
    ],
    finishers=[
        A("Wild Stampede Bond", "A rush of the whole wild: you and your allies run at double speed for a long while.", "buff_aura", "minecraft:nether_star", effect="speed", radius=13, seconds=22, amp=1, cooldown=1600, vfx="SPIRIT_AURA"),
        A("Beast Chorus Howl", "Every bonded voice howls at once in one ring of force.", "ring_burst", "minecraft:goat_horn", damage=22, radius=6.5, targets=8, knockback=1.6, cooldown=1400, vfx="WIND_RING"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 21
LINES.append(Line(
    id="dian_cecht_healer", name="Dian Cecht Healer", franchise=F,
    desc="The familia of medics: pulses of mending, tonics for the party, antiseptic wards and a shock of smelling salts when someone nods off at the wrong moment.",
    color="#7ae0b0", background="dream", icon="minecraft:glistering_melon_slice",
    book_name="The Orario Infirmary Ledger", book_lore="You bring them back. They owe you nothing but a bad joke.",
    stances=[
        S("Bedside Manner", "Calm hands and a calm voice: strong regeneration and armor, almost no offence.", "minecraft:white_bed", regen=1.2, armor=2, damage_pct=-0.2, vfx="SPIRIT_AURA"),
        S("Triage Rush", "Run to the wounded: faster, lightly armored.", "minecraft:golden_boots", speed_pct=0.12, armor=-1, vfx="WIND_RING"),
    ],
    stats=[
        T("Reserve of Mana", "More maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Sterile Garb", "Tougher cloth.", "armor_toughness", 1, "minecraft:white_wool"),
        T("Quick to the Bedside", "Faster feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Mending Pulse", "A wave of healing for you and every ally nearby.", "heal_pulse", "minecraft:glistering_melon_slice", amount=8, radius=8, vfx="SPIRIT_AURA"),
        A("Self-Prescription", "Dose yourself: a big heal followed by regeneration.", "heal_self", "minecraft:potion", amount=10, regen_seconds=8, vfx="SPIRIT_AURA"),
        A("Panacea Tonic", "Drink a tonic: Regeneration and Absorption for a while.", "buff_self", "minecraft:honey_bottle", effect="regeneration", effect2="absorption", seconds=16, vfx="SPIRIT_AURA"),
        A("Ward of Health", "You and your allies gain Regeneration for a while.", "buff_aura", "minecraft:golden_carrot", effect="regeneration", radius=10, seconds=14, vfx="MAGIC_CIRCLE"),
        A("Antiseptic Ward", "A shell of clean light absorbs a good deal of damage.", "shield", "minecraft:glass", absorb_hearts=6, seconds=8, vfx="MIRROR_PANE"),
        A("Sedative Flash", "A burst of calming light freezes nearby monsters for a moment.", "stun_ring", "minecraft:end_rod", radius=5, seconds=2, vfx="MAGIC_CIRCLE"),
        A("Smelling-Salts Shock", "A sharp shock that sends the monsters around you stumbling back.", "ring_burst", "minecraft:glowstone_dust", damage=5, radius=4, knockback=1.2, vfx="MAGIC_CIRCLE"),
    ],
    passives=[
        P("Bedside Radiance", "Allies near you slowly regenerate.", "aura", "minecraft:honey_block", effect="regeneration", target="allies", radius=8),
        P("Meditative Practice", "Crouched, your wounds close fast.", "regen_when", "minecraft:book", condition="sneaking", per_second=1.0),
        P("Chart Review", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=12, amount=1),
        P("Case Studies", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Poison and Rot Training", "You take much less wither damage.", "resist", "minecraft:wither_rose", damage_kind="wither", pct=0.45),
        P("Steady Under Stress", "At low health you take much less damage.", "reduce_when", "minecraft:totem_of_undying", condition="low_health", pct=0.25),
        P("Anathema to the Dead", "You deal more damage to the undead.", "dmg_vs", "minecraft:bone", category="undead", bonus=0.3),
    ],
    finishers=[
        A("Dian Cecht's Miracle", "A miracle of healing for you and every ally in a wide ring.", "heal_pulse", "minecraft:totem_of_undying", amount=22, radius=11, cooldown=1500, vfx="SPIRIT_AURA"),
        A("Full-Party Recovery", "A long, strong Regeneration for you and all your allies.", "buff_aura", "minecraft:beacon", effect="regeneration", radius=12, seconds=25, amp=1, cooldown=1600, vfx="MAGIC_CIRCLE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 22
LINES.append(Line(
    id="hunter_ability", name="Developmental Ability: Hunter", franchise=F,
    desc="The Level-up ability of a monster-slayer: spotting weak points, harvesting more from each kill and cleaving through whatever the dungeon spawns.",
    color="#b8472d", background="ruin", icon="minecraft:bow",
    book_name="Hunter's Ledger of Kills", book_lore="Every monster is just a lesson with a long tail.",
    stances=[
        S("Stalker's Stance", "Quiet and patient: sharper crits and a little extra damage, slightly slower.", "minecraft:spyglass", crit_chance=0.15, damage_pct=0.1, speed_pct=-0.05, vfx="WIND_SLASH"),
        S("Trophy Hunter", "Go for the big prize: much more damage, thin armor.", "minecraft:dragon_head", damage_pct=0.25, armor=-2, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Monster-Slayer's Arm", "Strong, practiced hands.", "attack_damage", 1.5, "minecraft:iron_axe"),
        T("Stalker's Footing", "Fast, quiet feet.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Hunter's Vigor", "A pool of health to burn.", "max_health", 2, "minecraft:cooked_beef"),
        T("Eagle's Reach", "Further eye, further reach.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
    ],
    arts=[
        A("Hunting Mark", "Mark a monster; it takes extra damage from you for a while.", "mark", "minecraft:name_tag", bonus=0.35, range=24, vfx="MAGIC_CIRCLE"),
        A("Hunter's Kill Shot", "A blow that is devastating against an already wounded monster.", "execute", "minecraft:iron_sword", damage=10, bonus=1.1, vfx="WIND_SLASH"),
        A("Trophy Cleave", "A wide cleave through the monsters in front of you.", "cone_strike", "minecraft:iron_axe", damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Rending Furrow", "Drag the blade and cut a furrow along the floor ahead.", "ground_wave", "minecraft:stone_axe", damage=7, length=12, width=2, vfx="WIND_SLASH"),
        A("Field Dressing Bite", "A savage cut that heals you for a share of the damage it deals.", "drain_bite", "minecraft:beef", damage=7, heal_pct=0.3, vfx="FIRE_BURST"),
        A("Trap Darts", "A burst of darts at the monster you look at.", "barrage", "minecraft:arrow", damage=2.5, shots=6, range=16, vfx="WEAPON_CONSTRUCTS"),
        A("Cover Scent", "A pinch of herbs and smoke hides you from the monsters around you.", "smoke", "minecraft:fern", radius=5, seconds=6, vfx="ASH_FORMATION"),
    ],
    passives=[
        P("Monster Hunter", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=0.3),
        P("Trophy Drops", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.25),
        P("Fresh Kill, Fresh Strength", "Each kill heals you and gives you Strength.", "kill_reward", "minecraft:beef", heal=4, effect="strength", seconds=6),
        P("Weak-Spot Trained", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.7),
        P("Veteran's Eye", "Your blows may strike critically.", "crit", "minecraft:spyglass", chance=0.15, mult=1.8),
        P("Hunting Rhythm", "Blows in a row grow stronger.", "momentum", "minecraft:flint", max_stacks=6),
        P("Spiked Gear", "A share of the damage you take returns to the attacker.", "thorns", "minecraft:pointed_dripstone", pct=0.2),
    ],
    finishers=[
        A("Hunter's Great Kill", "A single blow meant for the biggest thing in the room.", "execute", "minecraft:netherite_axe", damage=30, range=5, bonus=2.0, cooldown=1300, vfx="FIRE_BURST"),
        A("Monster Sweep", "Clear the room with a vast sweeping cleave.", "cone_strike", "minecraft:netherite_sword", damage=22, range=6, half_angle=70, targets=6, cooldown=1400, vfx="WIND_SLASH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 23
LINES.append(Line(
    id="endure_ability", name="Developmental Ability: Endure", franchise=F,
    desc="Outlast everything: a body that shrugs off blows, regenerates in a pinch and hands back some of the pain. The patient face of Level-up growth.",
    color="#6f8f9f", background="city", icon="minecraft:iron_chestplate",
    book_name="Treatise on Not Falling Down", book_lore="Winning is optional. Being there at the end is not.",
    stances=[
        S("Unyielding Stance", "Brace the body: huge armor and knockback resistance, slow to move.", "minecraft:iron_block", armor=6, knockback_res=0.5, speed_pct=-0.15, vfx="EARTH_SPIKES"),
        S("Marathon Stance", "Pace yourself: strong regeneration at the cost of damage.", "minecraft:golden_carrot", regen=1.5, damage_pct=-0.1, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Vast Reserve", "A huge pool of health.", "max_health", 5, "minecraft:golden_apple"),
        T("Hardened Body", "Layers of toughened tissue.", "armor", 2, "minecraft:iron_chestplate"),
        T("Planted Heels", "Very hard to push.", "knockback_resistance", 0.15, "minecraft:anvil"),
        T("Scarred Toughness", "A hide that blunts the worst hits.", "armor_toughness", 1, "minecraft:netherite_scrap"),
    ],
    arts=[
        A("Deep Reserve Breath", "Breathe deep: a big heal and then regeneration.", "heal_self", "minecraft:bread", amount=10, regen_seconds=9, vfx="SPIRIT_AURA"),
        A("Hardening Breath", "Draw yourself tight: Resistance and Regeneration for a while.", "buff_self", "minecraft:iron_ingot", effect="resistance", effect2="regeneration", seconds=14, vfx="EARTH_SPIKES"),
        A("Pain Returned", "For a short while a big share of the damage you take is flung back.", "ward_reflect", "minecraft:crying_obsidian", reflect_pct=0.4, vfx="DEVIL_CIRCLE"),
        A("Stubborn Wall", "A thick shell of stubbornness that absorbs a lot of damage.", "shield", "minecraft:shield", absorb_hearts=7, resist_amp=1, vfx="MIRROR_PANE"),
        A("Endurance Shove", "Lean into the monsters and push a wave of force along the ground.", "ground_wave", "minecraft:cobblestone", damage=6, length=9, width=3, knockback=1.5, vfx="STONE_SPIKES"),
        A("Landing Stand", "Leap and land like a boulder, shaking the monsters near.", "leap_slam", "minecraft:anvil", damage=8, radius=3.2, height=0.8, knockback=1.2, vfx="STONE_SPIKES"),
        A("Hold Your Ground", "A stomp that freezes the monsters around you.", "stun_ring", "minecraft:iron_boots", radius=4.5, seconds=2, vfx="EARTH_SPIKES"),
    ],
    passives=[
        P("Pain Threshold", "In a fight you take less damage.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.14),
        P("Never Down", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Quiet Recovery", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=10, amount=1),
        P("Scrape and Rise", "At low health your wounds close fast.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.3),
        P("Stone-Faced", "Explosions break against you.", "resist", "minecraft:tnt", damage_kind="explosion", pct=0.35),
        P("Padded Landing", "You take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.6),
        P("Marathon Lungs", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.35),
    ],
    finishers=[
        A("Endure: Immortal Wall", "An enormous shell of absorption and Resistance: the wall nobody gets through.", "shield", "minecraft:beacon", absorb_hearts=12, seconds=14, resist_amp=1, cooldown=1500, vfx="MIRROR_PANE"),
        A("Refuse the Count", "From the floor to your feet: a vast heal with long regeneration.", "heal_self", "minecraft:enchanted_golden_apple", amount=25, regen_seconds=12, cooldown=1600, vfx="SPIRIT_AURA"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 24
LINES.append(Line(
    id="takemikazuchi_bushi", name="Takemikazuchi Bushi", franchise=F,
    desc="Eastern swordsmanship of the dojo-familia: drawn blades, one-cut kills and a calm that makes monsters hesitate. Inspired by the Japanese-style fighters of the story.",
    color="#d94b4b", background="storm", icon="minecraft:iron_sword",
    book_name="Dojo Scroll of the Thunder God", book_lore="The sheath is half the sword.",
    stances=[
        S("Iaido Stance", "Hand on the hilt: much more damage and better crits, but you barely move.", "minecraft:iron_sword", damage_pct=0.3, crit_chance=0.1, speed_pct=-0.1, vfx="WIND_SLASH"),
        S("Water-Mirror Guard", "Still and ready: sturdy and hard to push, with gentler cuts.", "minecraft:shield", armor=3, knockback_res=0.2, damage_pct=-0.1, vfx="WATER_RING"),
    ],
    stats=[
        T("Dojo Discipline", "Clean, practiced strokes.", "attack_damage", 1.5, "minecraft:iron_sword"),
        T("Sure Footwork", "Dojo steps.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Seated Mind", "A calm that reaches further.", "entity_interaction_range", 0.4, "minecraft:lantern"),
        T("Padded Armor", "Light, disciplined armor.", "armor", 1, "minecraft:leather_chestplate"),
    ],
    arts=[
        A("Zanshin", "Complete awareness: Haste and Resistance for a while.", "buff_self", "minecraft:lantern", effect="haste", effect2="resistance", seconds=14, vfx="SPIRIT_AURA"),
        A("Iaido Draw", "Draw and cut in a single motion through the monsters ahead.", "dash_strike", "minecraft:iron_sword", distance=7, damage=9, vfx="WIND_SLASH"),
        A("One-Cut Kill", "A clean cut, much deadlier against a wounded monster.", "execute", "minecraft:diamond_sword", damage=10, bonus=1.1, vfx="WIND_SLASH"),
        A("Crescent Moon Cut", "A wide crescent across the front.", "cone_strike", "minecraft:golden_sword", damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Falling Petal Step", "Slip forward as softly as a falling petal.", "blink", "minecraft:cherry_sapling", distance=8, vfx="WIND_RING"),
        A("Wind-Cut Wave", "A thin cut of wind skims the ground ahead.", "ground_wave", "minecraft:white_dye", damage=7, length=12, width=2, vfx="WIND_SLASH"),
        A("Swallow Rise", "An upward cut that sends the monster flying.", "uppercut", "minecraft:wooden_sword", damage=8, launch=0.9, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Sharpened Edge", "Your cuts may strike critically.", "crit", "minecraft:flint", chance=0.17, mult=1.9),
        P("Final Stroke", "You deal much more damage to nearly dead monsters.", "execute_threshold", "minecraft:diamond_sword", threshold=0.25, bonus=0.7),
        P("Sheathed Composure", "While you hold a sword you take less damage.", "reduce_when", "minecraft:shield", condition="holding_sword", pct=0.12),
        P("Swordsman's Presence", "While you hold a sword you hit harder.", "dmg_when", "minecraft:iron_sword", condition="holding_sword", bonus=0.2),
        P("Willow in the Wind", "You may sway aside from a blow completely.", "dodge", "minecraft:lily_pad", chance=0.12),
        P("Even Breathing", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Scroll Study", "Your wounds close slowly, even out of combat.", "mending", "minecraft:book", seconds=14),
    ],
    finishers=[
        A("Iaido: Single Flash", "A single cut of light, devastating against one monster.", "execute", "minecraft:netherite_sword", damage=30, range=6, bonus=2.0, cooldown=1300, vfx="WIND_SLASH"),
        A("Hundred-Step Wind Cut", "A long cut of wind runs across the whole room.", "ground_wave", "minecraft:trident", damage=22, length=18, width=4, knockback=1.0, cooldown=1400, vfx="WIND_SLASH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 25
LINES.append(Line(
    id="little_rookie", name="Little Rookie's Courage", franchise=F,
    desc="A small adventurer facing something far too big: gather your nerve, hit it from below, throw everything you have and refuse to look away.",
    color="#7fd7ff", background="holy", icon="minecraft:wooden_sword",
    book_name="The Rookie's First Big Monster", book_lore="The trick, apparently, is to keep going after it is no longer a good idea.",
    stances=[
        S("Wide-Eyed Courage", "Eyes wide, heart racing: faster and stronger but wide open.", "minecraft:golden_carrot", damage_pct=0.2, speed_pct=0.1, armor=-2, vfx="SPIRIT_AURA"),
        S("Duck and Cover", "Keep your head down: armored and healing, a little slow.", "minecraft:shield", armor=4, regen=0.5, speed_pct=-0.05, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Small but Quick", "A little speed for a little hero.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
        T("Rookie's Nerve", "More maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Beginner's Luck", "Good fortune for the green.", "luck", 1.5, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Gather Courage", "Take a deep breath: Strength and Speed for a while.", "buff_self", "minecraft:sugar", effect="strength", effect2="speed", seconds=14, vfx="SPIRIT_AURA"),
        A("Rookie's Great Leap", "Jump much higher than you should and land on the monster.", "leap_slam", "minecraft:rabbit_foot", damage=9, radius=3.2, height=1.2, vfx="WIND_RING"),
        A("Giant Flip", "Get under something large and throw it into the air.", "uppercut", "minecraft:iron_sword", damage=9, launch=1.0, vfx="WIND_SLASH"),
        A("Deep Breath", "Calm down and stitch yourself back together.", "heal_self", "minecraft:bread", amount=9, regen_seconds=7, vfx="SPIRIT_AURA"),
        A("Frantic Firebolts", "Fire everything you have in a hurry.", "barrage", "minecraft:fire_charge", damage=2.5, shots=7, range=14, vfx="FIRE_BURST"),
        A("Eyes on It", "Never look away: the monster takes extra damage while you watch it.", "mark", "minecraft:ender_eye", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Knife Guard", "A knife held up in front of you absorbs a surprising amount of damage.", "shield", "minecraft:iron_nugget", absorb_hearts=5, seconds=8, vfx="MIRROR_PANE"),
    ],
    passives=[
        P("Minotaur Slayer", "You deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.3),
        P("Still Standing", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Hard to Kill", "At low health your wounds close fast.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.2),
        P("Adrenaline", "Each kill heals you and gives you Speed.", "kill_reward", "minecraft:sugar", heal=3, effect="speed", seconds=6),
        P("Beginner's Crit", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Quick Duck", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
        P("Learn Fast", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.25),
    ],
    finishers=[
        A("Impossible Strike", "The attack that should not work: a leap that comes down like a falling star.", "leap_slam", "minecraft:nether_star", damage=30, radius=6, height=1.3, knockback=1.8, cooldown=1400, vfx="FLAME_EXPLOSION"),
        A("Overcharged Spark", "Everything you have, pushed out in one line of light.", "ray_shot", "minecraft:end_crystal", damage=27, range=30, pierce=4, cooldown=1300, vfx="LIGHTNING_SPEAR"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 26 (3.26.0, Evolution skill)
def PE(name, desc, kind, icon, evolved, **p):
    """A passive with an `evolved` override (the stronger numbers it uses once the line has evolved)."""
    return Pas(name, desc, kind, p, icon, evolved)


def AU(name, desc, kind, icon, **p):
    """The Ultimate Evolution art (flag `ultimate`, kind debt_burst)."""
    return Art(name, desc, kind, p, icon, ultimate=True)


LINES.append(Line(
    id="haruhime_uchide", name="Uchide no Kozuchi", franchise=F,
    desc="The Renard priestess's Level Boost: a chanted prayer that lifts every ally near you one step beyond their limit, fox-tails that carry the blessing to many at once, and a quiet debt that the borrowed strength always asks back. Evolves once you have reached three hundred allies with your blessings.",
    color="#ffb6e0", background="dream", icon="minecraft:pink_petals",
    book_name="Haruhime's Prayer Scroll", book_lore="She cannot lift herself. She has never needed to; she lifts everyone else, and then asks them kindly to come home.",
    stances=[
        S("Fox Prayer", "Fold your hands and keep a prayer ready: gentle regeneration and a lighter step, with softer blows.", "minecraft:sweet_berries", regen=0.8, speed_pct=0.05, damage_pct=-0.15, vfx="SPIRIT_AURA"),
        S("Chanting", "Stand still and chant: slow and sheltered, while the allies near you grow strong.", "minecraft:bell", speed_pct=-0.25, armor=3, knockback_res=0.3, regen=0.4, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Renard's Light Step", "A fox's quick, quiet feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Quiet Resolve", "More maximum health than a priestess should need.", "max_health", 2, "minecraft:golden_carrot"),
        T("Far-Reaching Prayer", "Your blessings reach a little further.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
        T("Shrine Maiden's Fortune", "A small luck that rides with the prayer.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Uchide no Kozuchi", "Level Boost: you and your allies around you gain Strength for a while, as if one level higher.", "buff_aura", "minecraft:blaze_powder", effect="strength", radius=9, seconds=20, amp=1, vfx="MAGIC_CIRCLE"),
        A("Uchide no Kozuchi: Shift", "A wider but shorter boost: Speed for you and every ally in a big circle.", "buff_aura", "minecraft:sugar", effect="speed", radius=14, seconds=9, amp=1, vfx="WIND_RING"),
        A("Kokonoe Tails", "Fox-tails carry the blessing: you and your allies gain Resistance for a while.", "buff_aura", "minecraft:white_wool", effect="resistance", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Healing Prayer", "A soft prayer heals you and the allies around you.", "heal_pulse", "minecraft:honey_bottle", amount=7, radius=9, vfx="SPIRIT_AURA"),
        A("Quiet Prayer: Shift", "Kneel and pray for yourself, since the boost never reaches you: a heal and a little regeneration.", "heal_self", "minecraft:glistering_melon_slice", amount=9, regen_seconds=7, vfx="SPIRIT_AURA"),
        A("Sacred Sword Blessing", "A thin blade of blessed light gathers on your hands: Haste and Resistance for a while.", "buff_self", "minecraft:amethyst_shard", effect="haste", effect2="resistance", seconds=14, vfx="MAGIC_CIRCLE"),
        A("Fox-Fire Bolt", "A bolt of pale fox-fire along your line of sight.", "ray_shot", "minecraft:fire_charge", damage=7, range=20, pierce=2, vfx="FIRE_BURST"),
        A("Fox-Fire: Shift", "A scatter of fox-fire sparks at the monster you look at.", "barrage", "minecraft:glowstone_dust", damage=2.5, shots=8, range=15, vfx="FLAME_TRAIL"),
    ],
    passives=[
        PE("Prayer Radiance", "Allies near you slowly regenerate.", "aura", "minecraft:honey_block", {"radius": 12, "amp": 1}, effect="regeneration", target="allies", radius=8),
        P("Boost Afterglow", "Allies near you run a little faster.", "aura", "minecraft:feather", effect="speed", target="allies", radius=6),
        PE("Borrowed Strength", "Your blows store delayed damage on the foe: the strength you lend always asks for something back, and your Debt arts collect it.", "debt_store", "minecraft:amethyst_cluster", {"store_pct": 0.4, "max_pct": 0.7, "hold_seconds": 18}, store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="MAGIC_CIRCLE"),
        P("Shrine Maiden's Calm", "Crouched, your wounds close.", "regen_when", "minecraft:glow_berries", condition="sneaking", per_second=0.9),
        PE("Fox's Flight", "You may slip a blow completely.", "dodge", "minecraft:phantom_membrane", {"chance": 0.2}, chance=0.12),
        P("Prayer Beads", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=12, amount=1),
        P("Last Prayer", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Spiritual Ward", "You take much less magic damage.", "resist", "minecraft:enchanted_book", damage_kind="magic", pct=0.3),
    ],
    finishers=[
        A("Kokonoe: Nine-Tail Blessing", "All your tails at once: a long, strong Strength for you and every ally in a wide circle.", "buff_aura", "minecraft:nether_star", effect="strength", radius=13, seconds=25, amp=2, cooldown=1600, vfx="MAGIC_CIRCLE"),
        AU("Uchide no Kozuchi: Full Moon", "The moon-bright limit of the Level Boost: every Debt your blows left on the foes around you comes due in one blast.", "debt_burst", "minecraft:end_crystal", damage=18, mult=2.0, radius=12, targets=12, cost=0.07, cooldown=15000, vfx="SPIRIT_AURA"),
    ],
    evolution={"requirement": "ally_buffs", "count": 300,
               "text": "Reach 300 allies with the buffing and healing arts of this skill.",
               "ultimate_name": "Ultimate Skill, Uchide no Kozuchi: Full Moon"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 27 (3.26.0, Evolution skill)
LINES.append(Line(
    id="ryuu_astrea_blade", name="Astraea's Wooden Sword", franchise=F,
    desc="The elf blade dancer's justice: a wooden sword that parries and strikes back, a clean dash, a charged aria and a debt of justice that waits to be paid. Evolves after four hundred arts of this skill.",
    color="#66d9a0", background="ruin", icon="minecraft:wooden_sword",
    book_name="Ledger of the Last Astraea Blade", book_lore="Justice is not a feeling. It is a debt, and she has never once defaulted.",
    stances=[
        S("Elf Poise", "Light on the toes and ready to parry: faster, sharper, a little more damage.", "minecraft:feather", speed_pct=0.1, crit_chance=0.08, damage_pct=0.1, vfx="WIND_RING"),
        S("Wind Guard", "Plant the wooden sword and let the wind hold the line: sturdy and hard to push, slower to strike.", "minecraft:shield", armor=4, knockback_res=0.4, damage_pct=-0.1, speed_pct=-0.05, vfx="ELF_CIRCLE"),
    ],
    stats=[
        T("Blade Dancer's Strength", "Clean, practiced strokes.", "attack_damage", 1.5, "minecraft:wooden_sword"),
        T("Duelist's Tempo", "Quicker recovery between cuts.", "attack_speed", 0.06, "minecraft:clock", op="mult"),
        T("Elven Stride", "Light, long steps.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Ironed Resolve", "A hide that blunts the worst hits.", "armor_toughness", 1, "minecraft:iron_ingot"),
    ],
    arts=[
        A("Wooden-Sword Combo", "A fast cone of cuts through the monsters in front of you.", "cone_strike", "minecraft:wooden_sword", damage=7, range=3.6, half_angle=40, targets=3, vfx="WIND_SLASH"),
        A("Gale Sweep", "Spin the wooden sword in a wide circle and cut everything near you several times.", "whirlwind", "minecraft:stick", damage=3, radius=3.6, hits=4, vfx="WIND_TORNADO"),
        A("Star-Dust Arrow", "A single green arrow of wind and light that pierces the monsters in a line.", "ray_shot", "minecraft:arrow", damage=7, range=22, pierce=3, vfx="ELF_CIRCLE"),
        A("Parry and Riposte", "Turn the blade just so: for a moment a big share of the damage you take is flung back.", "ward_reflect", "minecraft:iron_sword", seconds=5, reflect_pct=0.5, vfx="MIRROR_PANE"),
        A("Bleeding Cut", "A thin, deep cut that poisons and slows everything in a cone.", "debuff_cone", "minecraft:string", effect="poison", effect2="slowness", seconds=6, seconds2=4, range=4, vfx="WIND_SLASH"),
        A("Dash Strike", "Dash forward and cut every monster in your path.", "dash_strike", "minecraft:leather_boots", distance=7, damage=7, vfx="WIND_SLASH"),
        A("Dash Strike: Shift", "A much longer dash, ending in a harder cut.", "dash_strike", "minecraft:golden_boots", distance=12, damage=10, targets=4, vfx="WIND_RING"),
        A("Aria of Justice: Shift", "Hold the verse, then bring a charged pillar of green light down where you look.", "meteor_drop", "minecraft:amethyst_shard", delay=24, damage=16, radius=5, range=18, vfx="ELF_CIRCLE"),
    ],
    passives=[
        PE("Justice Owed", "Your blows store delayed damage on the foe, a debt of justice that your Debt arts collect.", "debt_store", "minecraft:written_book", {"store_pct": 0.4, "max_pct": 0.7, "hold_seconds": 18}, store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="ELF_CIRCLE"),
        PE("Parry Reflex", "You may turn a blow aside completely.", "dodge", "minecraft:rabbit_foot", {"chance": 0.22}, chance=0.12),
        PE("Riposte Edge", "A share of the damage you take returns to the attacker.", "thorns", "minecraft:pointed_dripstone", {"pct": 0.4}, pct=0.2),
        P("Blade Dancer's Rhythm", "Blows in a row grow stronger.", "momentum", "minecraft:flint", max_stacks=6),
        P("Wooden-Sword Discipline", "Every few blows in a row hit much harder.", "combo", "minecraft:wooden_sword", hits=4, bonus=0.5),
        P("Sworn Guard", "While you hold a sword you take less damage.", "reduce_when", "minecraft:shield", condition="holding_sword", pct=0.12),
        P("Magic Defense Training", "You take much less magic damage.", "resist", "minecraft:enchanted_book", damage_kind="magic", pct=0.3),
        P("Hunter's Eye", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:spyglass", category="monster", bonus=0.25),
    ],
    finishers=[
        A("Ten Comrades' Strike", "Ten remembered blows leap from foe to foe, one for every comrade who fell before you.", "chain_hit", "minecraft:totem_of_undying", damage=14, jumps=9, jump_range=6, vfx="ELF_CIRCLE", cooldown=1400),
        AU("Luminous Wind: Aria of Justice", "The full aria: every Debt of justice your blows left on the foes around you comes due in a single storm of green light.", "debt_burst", "minecraft:nether_star", damage=20, mult=2.2, radius=13, targets=12, cost=0.07, cooldown=16000, vfx="ELF_CIRCLE"),
    ],
    evolution={"requirement": "arts_cast", "count": 400,
               "text": "Cast the arts of this skill 400 times.",
               "ultimate_name": "Ultimate Skill, Luminous Wind: Aria of Justice"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 28 (3.27.0, Evolution skill, docs/skills/evolution_ten_b.md row 4)
LINES.append(Line(
    id="ais_sword_princess", name="Ais, the Sword Princess", franchise=F,
    desc="The Loki Familia's Sword Princess: a wind barrier that sharpens every step, a storm that answers her chant, a blade that never wastes a stroke and a toll the wind collects later. Evolves after eight hundred kills.",
    color="#f2d56b", background="storm", icon="minecraft:golden_sword",
    book_name="Sword Princess's Field Journal", book_lore="She has never said what she is chasing. The wind around her says it for her, and it says it very fast.",
    stances=[
        S("Sword Princess Poise", "Light feet, a cool head and a clean edge: faster, sharper crits and a little more damage.", "minecraft:golden_sword", speed_pct=0.12, crit_chance=0.1, damage_pct=0.1, vfx="WIND_RING"),
        S("Wind Barrier", "Hold the wind close around you: sturdy, hard to shove and slowly healing, at the price of speed.", "minecraft:breeze_rod", armor=3, knockback_res=0.4, regen=0.5, speed_pct=-0.08, vfx="WIND_TORNADO"),
    ],
    stats=[
        T("Level Six Strength", "A veteran's raw cutting power.", "attack_damage", 1.5, "minecraft:iron_sword"),
        T("Swordswoman's Tempo", "Quicker, cleaner strokes.", "attack_speed", 0.07, "minecraft:clock", op="mult"),
        T("Wind-Touched Stride", "The wind carries your steps.", "movement_speed", 0.04, "minecraft:feather", op="mult"),
        T("Adventurer's Vitality", "More maximum health from years in the dungeon.", "max_health", 2, "minecraft:golden_carrot"),
    ],
    arts=[
        A("Ariel", "Wrap yourself in a wind barrier: Speed and Resistance for a while.", "buff_self", "minecraft:wind_charge", effect="speed", effect2="resistance", seconds=14, amp=1, vfx="WIND_TORNADO"),
        A("Ariel: Shift", "Crouching, the wind throws you a long way forward in one step.", "blink", "minecraft:feather", distance=11, vfx="WIND_SLASH"),
        A("Tempest", "The storm answers: a burst of wind-blades around you that throws the monsters back.", "ring_burst", "minecraft:breeze_rod", damage=8, radius=4.5, knockback=1.1, targets=6, vfx="WIND_TORNADO"),
        A("Tempest: Shift", "Crouching, the storm turns inward and drags the monsters around you onto your blade.", "pull", "minecraft:phantom_membrane", radius=8, strength=1.0, targets=6, damage=4, vfx="WIND_RING"),
        A("Sword Princess's Cut", "A wide, flawless cut through the monsters in front of you.", "cone_strike", "minecraft:iron_sword", damage=9, range=4, half_angle=50, targets=4, vfx="WIND_SLASH"),
        A("Rising Gale", "Crouching, you cut upward on a rising gust and launch the foe in front of you.", "uppercut", "minecraft:elytra", damage=9, launch=0.9, vfx="WIND_SLASH"),
        A("Avenger", "Fix your eyes on one monster: it takes more damage for a while.", "mark", "minecraft:spyglass", range=22, seconds=10, bonus=0.35, vfx="SPIRIT_AURA"),
        A("Avenger: Shift", "Crouching, one decisive stroke: it hits far harder the weaker the monster already is.", "execute", "minecraft:netherite_sword", damage=11, bonus=1.2, range=4, vfx="WIND_SLASH"),
    ],
    passives=[
        PE("Wind's Toll", "Your blows store delayed damage on the foe as the wind keeps cutting after you, and your Debt arts collect it.", "debt_store", "minecraft:echo_shard", {"store_pct": 0.45, "max_pct": 0.75, "hold_seconds": 18}, store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="WIND_SLASH"),
        PE("Sword Princess's Footwork", "You may slip a blow completely.", "dodge", "minecraft:rabbit_foot", {"chance": 0.22}, chance=0.12),
        PE("Avenger's Resolve", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:wither_skeleton_skull", {"bonus": 0.5}, category="monster", bonus=0.25),
        PE("Swordsman's Mastery", "You hit harder while wielding a sword.", "dmg_when", "minecraft:diamond_sword", {"bonus": 0.4}, condition="holding_sword", bonus=0.2),
        P("Wind Deflection", "Arrows and bolts bend around you: much less projectile damage.", "resist", "minecraft:shield", damage_kind="projectile", pct=0.35),
        P("Feather Landing", "You take much less fall damage.", "fall_guard", "minecraft:phantom_membrane", pct=0.6),
        P("Dungeon Diver's Stamina", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.25),
        P("Trophy of the Hunt", "A kill heals you and fills you with speed.", "kill_reward", "minecraft:bone", heal=2, effect="speed", seconds=4),
    ],
    finishers=[
        A("Hundred Winds", "The wind-blades multiply into a whirl of cuts all around you.", "whirlwind", "minecraft:netherite_sword", damage=5, hits=8, radius=4.5, interval=3, cooldown=1400, vfx="WIND_TORNADO"),
        AU("Tempest Unbound", "The storm is let go: every toll the wind has left on the monsters around you comes due in a single gale.", "debt_burst", "minecraft:nether_star",
           damage=22, mult=2.3, radius=13, targets=12, cost=0.07, cooldown=16000, vfx="WIND_TORNADO"),
    ],
    evolution={"requirement": "kills", "count": 800,
               "text": "Slay 800 creatures while Ais, the Sword Princess is switched on.",
               "ultimate_name": "Ultimate Skill, Tempest Unbound"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 29 (3.28.0, Evolution skill, docs/skills/evolution_ten_c.md row 10; canon notes in docs/skills/research/danmachi_c.md)
LINES.append(Line(
    id="lefiya_viridis_mage", name="Lefiya Viridis, the Thousand Elf", franchise=F,
    desc="The Loki Familia's young elf mage: homing arrows of light, a rain of fire spheres, a ring that borrows the spells of other elves and a chant held crouched and still. Every unfinished verse leaves a debt that the Thousand Elf collects. Evolves after five hundred arts of this skill.",
    color="#4ccf9a", background="forest", icon="minecraft:amethyst_shard",
    book_name="Lefiya's Chant Notebook", book_lore="Every margin is full of other people's spells, copied out in a very careful hand. She swears she understands all of them.",
    stances=[
        S("Elf Ring Ready", "Keep a borrowed circle on your arm: your spells hit harder and your feet are lighter, with a thinner guard.", "minecraft:glow_berries", damage_pct=0.18, speed_pct=0.06, armor=-2, vfx="ELF_CIRCLE"),
        S("Long Chant", "Stand still and let the verse run: slow, sheltered and slowly healing.", "minecraft:enchanted_book", speed_pct=-0.25, armor=3, knockback_res=0.3, regen=0.5, vfx="MAGIC_CIRCLE"),
    ],
    stats=[
        T("Mage's Endurance", "A little more maximum health for a caster who is always in the middle of a chant.", "max_health", 2, "minecraft:golden_carrot"),
        T("Magic Stat Growth", "A cleaner, stronger strike from the staff.", "attack_damage", 1.2, "minecraft:blaze_rod"),
        T("Nimble Chanter", "Quicker feet when the chant is done.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Elf Dust Reach", "The long staff's reach.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
    ],
    arts=[
        A("Arcs Ray", "Homing arrows of light: the blow jumps from monster to monster.", "chain_hit", "minecraft:spectral_arrow", damage=6, jumps=5, jump_range=6, vfx="ELF_CIRCLE"),
        A("Arcs Ray: Shift", "Crouching, the arrows join into one long, piercing ray of light.", "ray_shot", "minecraft:end_rod", damage=9, range=26, pierce=4, vfx="LIGHTNING_SPEAR"),
        A("Fusillade Fallarica", "A volley of fire spheres at the monster you look at.", "barrage", "minecraft:fire_charge", damage=3, shots=8, range=18, vfx="FIRE_BURST"),
        A("Fusillade Fallarica: Shift", "Crouching, the whole fusillade drops on the one place you look at after a short delay.", "meteor_drop", "minecraft:magma_cream", delay=26, damage=15, radius=5, range=22, vfx="FLAME_EXPLOSION"),
        A("Fairy Canon", "The fairy echo doubles the force of your attack magic: Strength and Haste for a while.", "buff_self", "minecraft:glowstone_dust", effect="strength", effect2="haste", seconds=14, vfx="MAGIC_CIRCLE"),
        A("Elf Ring: Borrowed Winter", "Cast an elder elf's frost chant with the ring: the monsters around you freeze in place.", "stun_ring", "minecraft:blue_ice", radius=5, seconds=3, targets=6, vfx="WATER_RING"),
        A("Elf Ring: Borrowed Ward", "Crouching, borrow a warding verse: a shield of absorption around you.", "shield", "minecraft:lapis_lazuli", absorb_hearts=6, seconds=9, vfx="MIRROR_PANE"),
        A("Elf Dust Short Staff", "When they get too close, the short staff strikes a cone of monsters.", "cone_strike", "minecraft:stick", damage=7, range=3.4, half_angle=45, targets=3, vfx="WIND_SLASH"),
    ],
    passives=[
        PE("Unfinished Verse", "Your blows store delayed damage on the foe, a verse left half-sung, and your Debt arts collect it.", "debt_store", "minecraft:written_book", {"store_pct": 0.45, "max_pct": 0.75, "hold_seconds": 18},
           store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="ELF_CIRCLE"),
        PE("Chant Posture", "Crouched and still, your spells hit harder.", "dmg_when", "minecraft:lectern", {"bonus": 0.45}, condition="sneaking", bonus=0.25),
        PE("Magic Ward Training", "You take much less magic damage.", "resist", "minecraft:shield", {"pct": 0.5}, damage_kind="magic", pct=0.3),
        P("Thousand Elf's Memory", "Kills give more experience: every chant you see goes in the notebook.", "xp_bonus", "minecraft:experience_bottle", pct=0.25),
        P("Mind Recovery", "Crouched, your wounds close faster.", "regen_when", "minecraft:glistering_melon_slice", condition="sneaking", per_second=0.9),
        P("Elf Dust Resonance", "Your blows may strike critically as the twin staves ring.", "crit", "minecraft:prismarine_crystals", chance=0.14, mult=1.8),
        P("Elven Ward Aura", "Allies near you resist a bit more, covered by your chant.", "aura", "minecraft:white_wool", effect="resistance", target="allies", radius=7),
        P("Mending Verse", "Your wounds close slowly, even out of combat.", "mending", "minecraft:honeycomb", seconds=14, amount=1),
    ],
    finishers=[
        A("Double Canon: Twin Release", "Both held circles let go at once: a huge burst of force around you.", "ring_burst", "minecraft:end_crystal", damage=28, radius=9, targets=14, knockback=2, cooldown=1500, cost=0.07, vfx="ELF_CIRCLE"),
        AU("Elf Ring: Thousand Elf", "Every verse left unfinished on the monsters around you is finished at once, in a chorus of borrowed spells.", "debt_burst", "minecraft:nether_star",
           damage=22, mult=2.3, radius=13, targets=12, cost=0.07, cooldown=16000, vfx="ULT_THOUSAND_ELF"),
    ],
    evolution={"requirement": "arts_cast", "count": 500,
               "text": "Cast the arts of this skill 500 times.",
               "ultimate_name": "Ultimate Skill, Elf Ring: Thousand Elf"},
))
