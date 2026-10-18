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
