"""Fairy Tail: 25 NEW magic lines (the four hand-built ones, Fire/Lightning/Sky Dragon Slayer and Ice-Make, are NOT repeated here).

Each line is one mage, magic or role of the guild. Short helpers keep the rows readable: S = stance, T = stat, A = art or finisher, P = passive.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "fairy_tail"


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
    id="requip_titania", name="Requip: Titania's Armory", franchise=F,
    desc="The Knight's Requip: swap armor and blades mid-fight. Heavy armor for the wall, light armor for the charge, and a rain of summoned swords for everything between.",
    color="#C0392B", background="iron", icon="minecraft:iron_chestplate",
    book_name="Armory Ledger of the Knight", book_lore="Every sword, every armor, kept ready in a pocket of space.",
    stances=[
        S("Heart Kreuz Armor", "Plate up in the armor of the guild's wall: much tougher and harder to shove, a little slower.", "minecraft:iron_chestplate", "MIRROR_PANE",
          armor=5, knockback_res=.3, speed_pct=-.06),
        S("Flight Armor", "Swap to a light suit: faster and sharper, but you take hits more easily.", "minecraft:chainmail_chestplate", "WIND_SLASH",
          speed_pct=.25, damage_pct=.1, armor=-3),
    ],
    stats=[
        T("Titania's Resolve", "Raises your maximum health.", "max_health", 4, "add", "minecraft:golden_apple"),
        T("Armory Strength", "Raises your attack damage.", "attack_damage", 1, "add", "minecraft:iron_sword"),
        T("Armored Poise", "Makes you harder to knock back.", "knockback_resistance", .08, "add", "minecraft:anvil"),
        T("Swordmaster's Reach", "Lets you hit foes from a little farther away.", "entity_interaction_range", .3, "add", "minecraft:stick"),
    ],
    arts=[
        A("Circle Sword", "Summon blades that orbit you and slash everything around you several times.", "whirlwind", "minecraft:iron_sword", "WEAPON_CONSTRUCTS",
          damage=3.5, radius=3.6, hits=5),
        A("Flame Empress Cut", "Change into the Flame Empress Armor and cut a blazing cone in front of you.", "cone_strike", "minecraft:blaze_rod", "FIRE_SPIRAL",
          damage=8, range=4, half_angle=45, targets=3),
        A("Adamantine Armor", "Don a plate that shrugs off almost anything: a thick absorption shield and extra resistance.", "shield", "minecraft:netherite_chestplate", "MIRROR_PANE",
          absorb_hearts=7, seconds=8, resist_amp=1),
        A("Lightning Empress Blitz", "Slip into the lightning armor and streak forward, slashing all in the lane.", "dash_strike", "minecraft:lightning_rod", "LIGHTNING_FIEND",
          distance=9, damage=7, targets=4),
        A("Blumenblatt", "Heaven's Wheel Armor: loose a flower of swords at the foe you face.", "barrage", "minecraft:arrow", "WEAPON_CONSTRUCTS",
          damage=3, shots=8, range=14),
        A("Knight's Challenge", "Plant your sword and call every hostile in range to fight you.", "taunt", "minecraft:shield", "MAGIC_CIRCLE",
          radius=9, targets=6, resist_seconds=4),
        A("Black Wing Moon Flash", "A single crescent slash that bites far deeper into wounded foes.", "execute", "minecraft:netherite_sword", "WIND_SLASH",
          damage=9, range=3.8, bonus=.8),
        A("Sea Empress Sweep", "Sweep a long wave of water-edged steel along the ground.", "ground_wave", "minecraft:prismarine_shard", "WATER_BURST",
          damage=7, length=10, width=2.5),
    ],
    passives=[
        P("Knight's Discipline", "Take less damage while a sword is in your hand.", "reduce_when", "minecraft:iron_helmet", condition="holding_sword", pct=.1),
        P("Swordmaster's Edge", "Deal more damage while holding a sword.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword", bonus=.15),
        P("Flame Empress Plating", "Fire armor in the closet: take less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire"),
        P("Armor Piercer", "Your blows may find the gap and strike critically.", "crit", "minecraft:flint", chance=.12, mult=1.6),
        P("Titania Never Falls", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=.3, seconds=6),
        P("Armored Charge", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=.5, chance=.3),
        P("Requip Rest", "Wounds close slowly while you rest in your spare armor.", "mending", "minecraft:iron_ingot", seconds=18, amount=1),
    ],
    finishers=[
        A("Heaven's Wheel: Rain of a Hundred Blades", "A circle of hundreds of swords forms in the sky and then falls on the spot you look at.", "meteor_drop", "minecraft:iron_sword", "WEAPON_CONSTRUCTS",
          damage=28, radius=6, range=22, delay=30, cooldown=1500, cost=.06),
        A("Purgatory Armor: Giant's Cleave", "Take the Purgatory Armor and bring a giant mace-blade across a wide cone.", "cone_strike", "minecraft:netherite_axe", "FLAME_EXPLOSION",
          damage=26, range=6, half_angle=65, targets=10, knockback=1.2, cooldown=1300, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="requip_gunslinger", name="Requip: Gunslinger", franchise=F,
    desc="Requip used as a marksman's trick: guns materialise in your hands already loaded. Patient shots from afar, a spray of lead up close, and a bounty hunter's eye for weak spots.",
    color="#A1887F", background="sand", icon="minecraft:crossbow",
    book_name="Dead-Eye Almanac", book_lore="Six chambers, one breath, no second chance.",
    stances=[
        S("Steady Aim", "Plant your feet and breathe out: your shots hit harder and crit more often, but you move slowly.", "minecraft:spyglass", "MAGIC_CIRCLE",
          damage_pct=.25, crit_chance=.12, speed_pct=-.2),
        S("Quickdraw", "Hip-fire stance: faster on your feet, but your damage is a little lower.", "minecraft:clock", "WIND_SLASH",
          speed_pct=.2, damage_pct=-.05, crit_chance=.05),
    ],
    stats=[
        T("Gunhand Strength", "Raises your attack damage.", "attack_damage", 1, "add", "minecraft:flint"),
        T("Sure Footing", "You are a little quicker.", "movement_speed", .04, "mult", "minecraft:leather_boots"),
        T("Hawk's Reach", "You can interact with things farther away.", "entity_interaction_range", .4, "add", "minecraft:spyglass"),
    ],
    arts=[
        A("Sharpshooter's Mark", "Fix a mark on a distant foe: it takes extra damage for a while.", "mark", "minecraft:target", "MAGIC_CIRCLE",
          range=24, seconds=10, bonus=.3),
        A("Sniper's Bullet", "A single bullet that pierces everything along your line of sight.", "ray_shot", "minecraft:spectral_arrow", "LIGHTNING_SPEAR",
          damage=10, range=28, pierce=2),
        A("Rapid Fire Requip", "Cycle through guns and empty them at the foe you look at.", "barrage", "minecraft:crossbow", "WEAPON_CONSTRUCTS",
          damage=3, shots=10, range=16, interval=2),
        A("Gunsmoke Veil", "Fire a smoke round at your feet and blind the creatures around you.", "smoke", "minecraft:gunpowder", "ASH_FORMATION",
          radius=5, seconds=6),
        A("Ricochet Round", "A bullet that bounces from foe to foe.", "chain_hit", "minecraft:iron_nugget", "LIGHTNING_FIEND",
          damage=5, jumps=4, jump_range=6),
        A("Holster Step", "Requip yourself a few steps ahead, out of the line of fire.", "blink", "minecraft:ender_pearl", "SPATIAL_RIFT",
          distance=9),
        A("Dead-Eye Shot", "A calm headshot that hits much harder the weaker the foe.", "execute", "minecraft:arrow", "WIND_SLASH",
          damage=9, range=5, bonus=1.0),
    ],
    passives=[
        P("Held Breath", "Deal more damage while sneaking, as you line up the shot.", "dmg_when", "minecraft:leather_helmet", condition="sneaking", bonus=.2),
        P("Weak-Spot Eye", "Deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:glass_bottle", threshold=.25, bonus=.6),
        P("Gunslinger's Crit", "Your shots may land critical hits.", "crit", "minecraft:glowstone_dust", chance=.15, mult=1.7),
        P("Quick Feet", "A chance to dodge a blow entirely.", "dodge", "minecraft:feather", chance=.1, rest=100),
        P("Bounty Hunter", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:emerald", chance=.1),
        P("Rolling Combo", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_nugget", hits=4, bonus=.5),
        P("Smoke and Powder", "Hunger fades more slowly on the trail.", "hunger_saver", "minecraft:bread", pct=.2),
        P("Cold Veteran", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=.12),
    ],
    finishers=[
        A("Dragon-Piercing Cannon", "Requip a siege cannon and fire one shell through every foe in front of you.", "ray_shot", "minecraft:tnt", "FLAME_EXPLOSION",
          damage=34, range=34, pierce=5, cooldown=1500, cost=.06),
        A("Hundred-Bullet Storm", "Surround yourself with floating muzzles and empty them all at the foe you face.", "barrage", "minecraft:firework_rocket", "WEAPON_CONSTRUCTS",
          damage=5, shots=16, range=18, interval=2, cooldown=1200, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="celestial_spirit_magic", name="Celestial Spirit Magic", franchise=F,
    desc="Sign contracts with the spirits behind the zodiac gates and call them to fight at your side. A caster who wins by opening the right gate at the right moment.",
    color="#F1C40F", background="stellar", icon="minecraft:nether_star",
    book_name="Keyring of the Twelve Gates", book_lore="Silver keys and golden keys, and the promise to keep your word.",
    stances=[
        S("Gate Key Ready", "Hold your keys loose and your mind clear: faster magic recovery and a gentle regeneration.", "minecraft:gold_ingot", "MAGIC_CIRCLE",
          regen=.6, speed_pct=.05),
        S("Star Dress", "Wear a spirit's power as a dress: stronger blows and quicker feet, but a thinner guard.", "minecraft:amethyst_shard", "SPIRIT_AURA",
          damage_pct=.2, speed_pct=.1, armor=-2),
    ],
    stats=[
        T("Contract Vitality", "Raises your maximum health.", "max_health", 4, "add", "minecraft:glistering_melon_slice"),
        T("Starlit Grace", "You are a little quicker.", "movement_speed", .04, "mult", "minecraft:sugar"),
        T("Whip Hand", "Raises your attack damage.", "attack_damage", .8, "add", "minecraft:lead"),
        T("Lucky Stars", "Raises your luck.", "luck", 1, "add", "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Lion Spirit: Regulus Impact", "Open the Lion's gate and have Loke charge down the line, striking every foe in his path.", "dash_strike", "minecraft:gold_nugget", "SPIRIT_AURA",
          distance=8, damage=8, targets=4),
        A("Water Bearer's Flood", "Aquarius pours a torrent over everything near you.", "ring_burst", "minecraft:water_bucket", "WATER_BURST",
          damage=6, radius=4, targets=6, knockback=1),
        A("Maiden's Burrow", "Virgo digs a tunnel: you reappear a few blocks forward.", "blink", "minecraft:dirt", "SPATIAL_RIFT",
          distance=10),
        A("Archer's Volley", "Sagittarius looses a flight of arrows at the foe you look at.", "barrage", "minecraft:bow", "SPIRIT_AURA",
          damage=3, shots=7, range=16),
        A("Fleuve d'étoiles", "Snap your starlit whip and drag the creatures around you toward yourself.", "pull", "minecraft:lead", "THREAD_LINE",
          radius=8, strength=1, targets=5, damage=4),
        A("Golden Bull's Axe", "Taurus hefts his axe and launches the foe in front of you into the air.", "uppercut", "minecraft:iron_axe", "WIND_SLASH",
          damage=9, range=3.5, launch=.9),
        A("Ram's Warm Wool", "Aries wraps you and your allies in soft wool that mends wounds.", "heal_pulse", "minecraft:white_wool", "SPIRIT_AURA",
          radius=7, amount=6),
        A("Scorpion's Sand Buster", "Scorpio fires a spiral of sand through the line ahead.", "ray_shot", "minecraft:sand", "WIND_TORNADO",
          damage=8, range=18, pierce=2),
    ],
    passives=[
        P("Spirit Bond", "Wounds close slowly: your spirits look after you.", "mending", "minecraft:heart_of_the_sea", seconds=16, amount=1),
        P("Starlight Eyes", "At night you see clearly by the stars.", "self_effect_when", "minecraft:spyglass", effect="night_vision", condition="night"),
        P("Moon Tide", "Heal while it is night.", "regen_when", "minecraft:clock", condition="night", per_second=.4),
        P("Celestial Presence", "Allies near you slowly regenerate.", "aura", "minecraft:glow_berries", effect="regeneration", target="allies", radius=7),
        P("Plue's Luck", "A chance to avoid a blow completely.", "dodge", "minecraft:carrot", chance=.1, rest=100),
        P("Contract Ledger", "Kills give more experience.", "xp_bonus", "minecraft:writable_book", pct=.12),
        P("Honoured Promise", "A kill heals you and refreshes your speed.", "kill_reward", "minecraft:gold_ingot", effect="speed", heal=2, seconds=4),
    ],
    finishers=[
        A("Urano Metria", "Draw the seven stars into a rain of light on the place you look at.", "meteor_drop", "minecraft:nether_star", "MAGIC_CIRCLE",
          damage=30, radius=7, range=24, delay=35, cooldown=1600, cost=.07),
        A("Celestial Spirit King's Summons", "Open the gate no key can: a ring of cosmic force bursts outward from you.", "ring_burst", "minecraft:end_crystal", "SPIRIT_AURA",
          damage=24, radius=8, targets=14, knockback=2, cooldown=1400, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="satan_soul", name="Take-Over: Satan Soul", franchise=F,
    desc="Absorb a demon's soul and wear its body. Dark lightning, shadow wings and an unnerving smile for a mage who hits like a wrecking ball when it counts.",
    color="#7B1FA2", background="void", icon="minecraft:amethyst_shard",
    book_name="Demon's Take-Over Primer", book_lore="Smile sweetly. Then become what they fear.",
    stances=[
        S("Satan Soul", "Take on the demon's body: heavy damage and speed, but you lose armor.", "minecraft:wither_skeleton_skull", "DEVIL_CIRCLE",
          damage_pct=.3, speed_pct=.1, armor=-3),
        S("Halphas Form", "Take the stoutest demon's body: very tough and unmovable, but slow.", "minecraft:obsidian", "SHADOW_POOL",
          armor=6, knockback_res=.4, speed_pct=-.1),
    ],
    stats=[
        T("Demon Blood", "Raises your maximum health.", "max_health", 4, "add", "minecraft:nether_wart"),
        T("Satan's Claws", "Raises your attack damage.", "attack_damage", 1.2, "add", "minecraft:netherite_scrap"),
        T("Shadow Wings", "You fall more gently.", "safe_fall_distance", 2, "add", "minecraft:phantom_membrane"),
    ],
    arts=[
        A("Darkness Stream", "Hurl a black torrent along the ground in front of you.", "ground_wave", "minecraft:black_dye", "SHADOW_POOL",
          damage=7, length=11, width=2.5),
        A("Evil Spark", "A bolt of dark lightning that jumps from foe to foe.", "chain_hit", "minecraft:prismarine_crystals", "LIGHTNING_FIEND",
          damage=6, jumps=4, jump_range=6),
        A("Demon Wing Dive", "Beat your wings and shoot forward, clawing everything on the way.", "dash_strike", "minecraft:elytra", "DEVIL_CIRCLE",
          distance=9, damage=7, targets=4),
        A("Fear of the Demon", "A glare that fills a cone of foes with darkness and weakness.", "debuff_cone", "minecraft:ender_eye", "DEVIL_CIRCLE",
          range=6, half_angle=50, seconds=8, effect="weakness", effect2="darkness", seconds2=6),
        A("Evil Explosion", "Detonate demon power around you in a ring.", "ring_burst", "minecraft:fire_charge", "FLAME_EXPLOSION",
          damage=7, radius=4, targets=6, knockback=1.5),
        A("Shadow Slip", "Fold into your own shadow and reappear a few steps forward.", "blink", "minecraft:black_stained_glass_pane", "SHADOW_POOL",
          distance=9),
        A("Succubus Kiss", "A close bite that restores your health with the damage dealt.", "drain_bite", "minecraft:redstone", "DEVIL_CIRCLE",
          damage=7, range=3.4, heal_pct=.45),
    ],
    passives=[
        P("She-Devil's Fury", "Deal more damage when you are low on health.", "dmg_when", "minecraft:blaze_powder", condition="low_health", bonus=.25),
        P("Hellfire Skin", "Fire barely scratches the body of a demon.", "resist", "minecraft:magma_cream", damage_kind="fire"),
        P("Cruel Smile", "Foes you strike may be seized by weakness.", "on_hit_effect", "minecraft:fermented_spider_eye", effect="weakness", seconds=4, chance=.25),
        P("Demon's Appetite", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:rotten_flesh", pct=.07),
        P("Unholy Aura", "Enemies around you are slowed by the demon's presence.", "aura", "minecraft:soul_lantern", effect="slowness", target="enemies", radius=5),
        P("Merciless Streak", "Each blow in a row makes the next stronger.", "momentum", "minecraft:iron_nugget", per_stack=.05, max_stacks=6),
        P("Soul Siphon", "A kill heals you and fills you with strength.", "kill_reward", "minecraft:soul_sand", effect="strength", heal=3, seconds=5),
    ],
    finishers=[
        A("Soul Extinction", "Gather the demon's power into one huge beam that tears through everything ahead.", "ray_shot", "minecraft:dragon_breath", "DEVIL_CIRCLE",
          damage=36, range=32, pierce=6, cooldown=1500, cost=.07),
        A("Demon Lord's Descent", "Wings spread, you fall on the foes below with the weight of a demon.", "leap_slam", "minecraft:dragon_head", "SHADOW_POOL",
          damage=30, radius=7, height=1.2, knockback=2.2, cooldown=1300, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="beast_arm_takeover", name="Take-Over: Beast Arm", franchise=F,
    desc="Take over only the part of a beast you need: a mountain-sized arm, a wolf's speed, a bull's horns. A fighter's magic about being a man, a wall and a hammer all at once.",
    color="#6D4C41", background="forest", icon="minecraft:bone",
    book_name="Real Man's Take-Over Manual", book_lore="Strength you can punch with. Half-form, full heart.",
    stances=[
        S("Beast Arm", "Swell one arm into a monster's limb: heavy blows and a stout body, but you are slow.", "minecraft:iron_axe", "EARTH_SPIKES",
          damage_pct=.3, armor=2, speed_pct=-.1, knockback_res=.2),
        S("Wolf Gait", "Take over a wolf's legs: much faster, a little less punch.", "minecraft:leather_boots", "WIND_SLASH",
          speed_pct=.3, damage_pct=-.05, crit_chance=.05),
    ],
    stats=[
        T("Beast Heart", "Raises your maximum health.", "max_health", 6, "add", "minecraft:beef"),
        T("Thick Hide", "Raises your armor.", "armor", 1.5, "add", "minecraft:leather"),
        T("Rooted Stance", "Makes you very hard to knock back.", "knockback_resistance", .1, "add", "minecraft:stone"),
        T("Long Arm", "Lets you hit foes from farther away.", "entity_interaction_range", .4, "add", "minecraft:bone"),
    ],
    arts=[
        A("Beast Arm: Iron Fist", "Wrap a monstrous fist and punch the foes in front of you.", "cone_strike", "minecraft:iron_block", "EARTH_SPIKES",
          damage=9, range=3.8, half_angle=35, targets=2, knockback=1),
        A("Bull's Gore", "Lower the horns and charge through whatever stands ahead.", "dash_strike", "minecraft:goat_horn", "WIND_SLASH",
          distance=7, damage=8, targets=3),
        A("Ground Pound", "Raise both arms and bring them down on the ground, shaking foes around you.", "leap_slam", "minecraft:anvil", "EARTH_SPIKES",
          damage=9, radius=3.4, height=.8, knockback=1.4),
        A("Man's Roar", "A bellow that stuns the creatures around you for a moment.", "stun_ring", "minecraft:goat_horn", "WIND_RING",
          radius=4.5, seconds=2, targets=5),
        A("Claw Upper", "A clawed uppercut that launches the foe in front of you.", "uppercut", "minecraft:flint", "WIND_SLASH",
          damage=8, range=3.2, launch=.8),
        A("Boar's Hide", "Thicken your skin: an absorption shield and added resistance.", "shield", "minecraft:turtle_scute", "MIRROR_PANE",
          absorb_hearts=6, seconds=8, resist_amp=1),
        A("Bear Hug", "Seize the creatures around you and drag them in close.", "pull", "minecraft:honeycomb", "EARTH_SPIKES",
          radius=6, strength=1.2, targets=4, damage=5),
    ],
    passives=[
        P("Man's Resolve", "Take less damage while at full health.", "reduce_when", "minecraft:golden_chestplate", condition="full_health", pct=.12),
        P("Beast Instinct", "A chance to avoid a blow completely.", "dodge", "minecraft:rabbit_foot", chance=.1, rest=90),
        P("Heavy Hands", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=.6, chance=.35),
        P("Pain Tolerance", "Wounds close over time, even out of combat.", "mending", "minecraft:apple", seconds=15, amount=1),
        P("Tough as Nails", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:cactus", pct=.2, rest=20),
        P("Wolf Nose", "Deal more damage to animals and beasts.", "dmg_vs", "minecraft:bone_meal", category="animal", bonus=.2),
        P("Hearty Meals", "You tire more slowly.", "hunger_saver", "minecraft:cooked_beef", pct=.2),
        P("Unbreakable Spirit", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:shield", threshold=.3, seconds=6),
    ],
    finishers=[
        A("Beast Arm: Colossus Smash", "Swell the arm to the size of a tower and slam it into the ground, hurting everyone nearby.", "ring_burst", "minecraft:netherite_block", "EARTH_SPIKES",
          damage=26, radius=7, targets=12, knockback=2.3, cooldown=1400, cost=.06),
        A("Beast Soul: Full Rampage", "Go berserk in a whirl of claws and horns hitting everything around you again and again.", "whirlwind", "minecraft:bone", "WIND_TORNADO",
          damage=8, radius=5, hits=8, interval=4, cooldown=1300, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="iron_dragon_slayer", name="Iron Dragon Slayer", franchise=F,
    desc="Eat metal, become metal. A dragon slayer whose body turns to scales of iron and whose arms become clubs, swords and spears: the toughest bruiser of the dragon magics.",
    color="#78909C", background="iron", icon="minecraft:iron_block",
    book_name="Scrap-Eater's Scale Book", book_lore="Chew the rust, spit the steel.",
    stances=[
        S("Iron Scale Hide", "Let iron scales cover your skin: a lot of armor and weight, but slower feet.", "minecraft:iron_chestplate", "MIRROR_PANE",
          armor=6, knockback_res=.3, speed_pct=-.08),
        S("Steel Rage", "Throw the hide off for a brawler's fury: heavy blows, thinner defence.", "minecraft:iron_axe", "WIND_SLASH",
          damage_pct=.3, speed_pct=.05, armor=-2),
    ],
    stats=[
        T("Dragon Marrow", "Raises your maximum health.", "max_health", 4, "add", "minecraft:iron_ingot"),
        T("Iron Plating", "Raises your armor.", "armor", 1.5, "add", "minecraft:iron_chestplate"),
        T("Steel Bones", "Raises your armor toughness.", "armor_toughness", 1, "add", "minecraft:netherite_ingot"),
        T("Club Arm", "Raises your attack damage.", "attack_damage", 1.2, "add", "minecraft:iron_pickaxe"),
    ],
    arts=[
        A("Iron Dragon's Club", "Stretch your arm into a long iron club and strike everything in a line.", "ray_shot", "minecraft:stick", "WEAPON_CONSTRUCTS",
          damage=8, range=12, pierce=3),
        A("Iron Dragon's Roar", "Breathe a blast of rusty shards over a cone in front of you.", "cone_strike", "minecraft:iron_nugget", "EARTH_SPIKES",
          damage=8, range=4.5, half_angle=45, targets=3, knockback=.8),
        A("Metal Meal", "Devour a scrap of iron: gain strength and resistance for a time.", "buff_self", "minecraft:raw_iron", "SPIRIT_AURA",
          effect="strength", effect2="resistance", seconds=14, amp=1, amp2=0),
        A("Demon Log Sword Rain", "Sprout a dozen iron spears from your arm and hurl them at the foe you look at.", "barrage", "minecraft:trident", "WEAPON_CONSTRUCTS",
          damage=3.5, shots=7, range=14),
        A("Iron Dragon's Rising Hammer", "A hammerhead of iron that smashes the foe in front of you skyward.", "uppercut", "minecraft:iron_axe", "EARTH_SPIKES",
          damage=9, range=3.4, launch=.9),
        A("Iron Dragon's Hard Scales", "Plate yourself over with thick scales: an absorption shield.", "shield", "minecraft:iron_horse_armor", "MIRROR_PANE",
          absorb_hearts=8, seconds=8, resist_amp=1),
        A("Iron Meteor Drop", "Launch yourself up and come down as a ton of iron.", "leap_slam", "minecraft:anvil", "EARTH_SPIKES",
          damage=10, radius=3.4, height=.9, knockback=1.5),
    ],
    passives=[
        P("Metal Body", "Arrows and bolts glance off iron skin: take less projectile damage.", "resist", "minecraft:iron_trapdoor", damage_kind="projectile"),
        P("Spiked Scales", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:chain", pct=.2, rest=20),
        P("Heavy Frame", "Take less damage while in combat.", "reduce_when", "minecraft:iron_helmet", condition="in_combat", pct=.1),
        P("Scrap Appetite", "You tire more slowly, living on metal.", "hunger_saver", "minecraft:raw_copper", pct=.2),
        P("Rusty Backlash", "Whoever strikes you may be slowed by rusting shards.", "on_hurt_retaliate", "minecraft:iron_nugget", effect="slowness", seconds=3, chance=.25),
        P("Anvil Weight", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:smithing_table", strength=.6, chance=.35),
        P("Black Steel Fury", "Deal more damage while in combat.", "dmg_when", "minecraft:coal_block", condition="in_combat", bonus=.15),
    ],
    finishers=[
        A("Black Steel Dragon Breath", "A roar of whirling blades sweeping a long, wide lane of the ground.", "ground_wave", "minecraft:blast_furnace", "STONE_SPIKES",
          damage=30, length=18, width=4, knockback=1.6, cooldown=1400, cost=.06),
        A("Dragon Force: Iron Rampage", "Go berserk in full iron scales and hammer everything around you again and again.", "whirlwind", "minecraft:netherite_axe", "EARTH_SPIKES",
          damage=8, radius=5, hits=9, interval=4, cooldown=1300, cost=.07),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="shadow_dragon_slayer", name="Shadow Dragon Slayer", franchise=F,
    desc="Become a darkness that cuts: the dragon slayer of shadows slips through gloom, blinds the field and finishes foes with a single claw. A hunter's kit of mobility and marks.",
    color="#37474F", background="ruin", icon="minecraft:black_dye",
    book_name="Primer of the Shadowed Fang", book_lore="Light makes a shadow. Shadow does not need light.",
    stances=[
        S("Shadow Cloak", "Wrap the shadows around you: quicker, a little more likely to crit, but easier to hurt.", "minecraft:black_wool", "SHADOW_POOL",
          speed_pct=.15, crit_chance=.08, armor=-1),
        S("Shadow Drive", "Let the dragon out: strong blows and a sharper edge, at the cost of defence and speed.", "minecraft:wither_rose", "DEVIL_CIRCLE",
          damage_pct=.3, crit_chance=.1, armor=-4, speed_pct=-.05),
    ],
    stats=[
        T("Gloom Body", "Raises your maximum health.", "max_health", 2, "add", "minecraft:coal"),
        T("Shadow Stride", "You are quicker on your feet.", "movement_speed", .05, "mult", "minecraft:black_carpet"),
        T("Claw Edge", "Raises your attack damage.", "attack_damage", 1, "add", "minecraft:flint"),
        T("Wallrunner", "Raises your step height: climb ledges at a run.", "step_height", .3, "add", "minecraft:ladder"),
    ],
    arts=[
        A("Shadow Dragon's Slash", "Streak forward as a blade of darkness and slash everything you cross.", "dash_strike", "minecraft:netherite_sword", "SHADOW_POOL",
          distance=8, damage=7, targets=4),
        A("Shadow Fog", "Spread a cloud of darkness that blinds the creatures around you.", "smoke", "minecraft:black_stained_glass", "ASH_FORMATION",
          radius=5.5, seconds=6),
        A("Shadow Step", "Melt into the nearest shadow and reappear several blocks ahead.", "blink", "minecraft:ender_pearl", "SHADOW_POOL",
          distance=11),
        A("Future Rogue's Foresight", "See the foe's next move: mark it so it takes extra damage.", "mark", "minecraft:ender_eye", "MAGIC_CIRCLE",
          range=20, seconds=9, bonus=.3),
        A("Shadow Dragon's Roar", "A breath of gloom that darkens and slows a cone of foes.", "debuff_cone", "minecraft:fermented_spider_eye", "SHADOW_POOL",
          range=6, half_angle=45, seconds=7, effect="slowness", effect2="darkness", seconds2=5),
        A("Claw of the Hidden Dragon", "A single killing claw, far deadlier against wounded foes.", "execute", "minecraft:iron_sword", "WIND_SLASH",
          damage=10, range=3.6, bonus=1.0),
        A("Shadow Dragon's Wing Chain", "Lash out with dark wings that jump from foe to foe.", "chain_hit", "minecraft:phantom_membrane", "SHADOW_POOL",
          damage=5, jumps=4, jump_range=6),
    ],
    passives=[
        P("Gloom Evasion", "A chance to dodge a blow entirely.", "dodge", "minecraft:feather", chance=.12, rest=100),
        P("Night Hunter", "Deal more damage at night.", "dmg_when", "minecraft:clock", condition="night", bonus=.2),
        P("Lair Walker", "Underground, you are swifter in the dark.", "self_effect_when", "minecraft:deepslate", effect="speed", condition="underground"),
        P("Blinding Strike", "Your blows may leave foes blind.", "on_hit_effect", "minecraft:ink_sac", effect="blindness", seconds=3, chance=.2),
        P("Hidden Edge", "Your blows may strike critically.", "crit", "minecraft:flint", chance=.15, mult=1.7),
        P("Final Claw", "Deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_nugget", threshold=.25, bonus=.6),
        P("Rooftop Dash", "Deal more damage while sprinting.", "sprint_bonus", "minecraft:leather_boots", bonus=.2),
        P("Hunter's Harvest", "A kill heals you and quickens you.", "kill_reward", "minecraft:rotten_flesh", effect="speed", heal=2, seconds=4),
    ],
    finishers=[
        A("Shadow Dragon's Black Breath", "Unleash a lance of pure shadow that pierces everything in front of you.", "ray_shot", "minecraft:obsidian", "SHADOW_POOL",
          damage=35, range=30, pierce=5, cooldown=1500, cost=.07),
        A("Eclipse Rend", "A final slash from the dark: devastating against anything already hurt.", "execute", "minecraft:netherite_sword", "DEVIL_CIRCLE",
          damage=34, range=5, bonus=2, cooldown=1200, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="white_dragon_slayer", name="White Dragon Slayer", franchise=F,
    desc="Holy light given a dragon's roar. A bright, aggressive caster-fighter whose magic both burns the dark and mends the friends at his side.",
    color="#FFF8E1", background="holy", icon="minecraft:glowstone",
    book_name="Radiant Scale Codex", book_lore="Shine first. Be forgiven later.",
    stances=[
        S("White Drive", "Light overflows: stronger blows and quicker steps, a thin guard.", "minecraft:glowstone_dust", "SPIRIT_AURA",
          damage_pct=.3, speed_pct=.1, armor=-2),
        S("Holy Radiance", "A calm halo of light: a gentle regeneration and a firmer guard.", "minecraft:sea_lantern", "MAGIC_CIRCLE",
          regen=1, armor=2, knockback_res=.1),
    ],
    stats=[
        T("Dragon Light", "Raises your maximum health.", "max_health", 4, "add", "minecraft:glow_berries"),
        T("Brilliant Strength", "Raises your attack damage.", "attack_damage", 1, "add", "minecraft:gold_ingot"),
        T("Swift Radiance", "You are quicker on your feet.", "movement_speed", .05, "mult", "minecraft:sugar"),
    ],
    arts=[
        A("White Dragon's Claw", "A flash-bright claw that launches the foe in front of you.", "uppercut", "minecraft:white_dye", "SPIRIT_AURA",
          damage=8, range=3.3, launch=.8),
        A("White Dragon's Blessing", "A glow that heals you and your allies around you.", "heal_pulse", "minecraft:honey_bottle", "SPIRIT_AURA",
          radius=7, amount=6),
        A("Holy Ray", "A lance of white light that pierces everything along your gaze.", "ray_shot", "minecraft:end_rod", "LIGHTNING_SPEAR",
          damage=9, range=22, pierce=3),
        A("Light of Courage", "A warm light gives you and your allies strength for a time.", "buff_aura", "minecraft:beacon", "MAGIC_CIRCLE",
          effect="strength", radius=8, seconds=12, amp=0),
        A("Holy Nova", "Let the light burst outward around you.", "ring_burst", "minecraft:sea_lantern", "FLAME_EXPLOSION",
          damage=7, radius=4, targets=6, knockback=1.2),
        A("Light Dash", "Flash forward on a trail of white light, cutting down whatever is in the path.", "dash_strike", "minecraft:glowstone_dust", "LIGHTNING_FIEND",
          distance=9, damage=7, targets=4),
        A("Aegis of Dawn", "For a while, part of the damage you take is flung back as light.", "ward_reflect", "minecraft:totem_of_undying", "MIRROR_PANE",
          seconds=7, reflect_pct=.4),
    ],
    passives=[
        P("Daylight Mend", "Heal while it is daytime.", "regen_when", "minecraft:sunflower", condition="day", per_second=.5),
        P("Smite the Dark", "Deal more damage to the undead.", "dmg_vs", "minecraft:golden_sword", category="undead", bonus=.25),
        P("Banner of Light", "Allies around you are strengthened.", "aura", "minecraft:white_banner", effect="strength", target="allies", radius=7),
        P("Battle Glow", "While fighting, you move with a hastened hand.", "self_effect_when", "minecraft:clock", effect="haste", condition="in_combat"),
        P("Sacred Leech", "Heal for a share of the damage you deal.", "lifesteal", "minecraft:glistering_melon_slice", pct=.06),
        P("Shining Rhythm", "Every few blows in a row hit much harder.", "combo", "minecraft:gold_nugget", hits=4, bonus=.5),
        P("Last Light", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", threshold=.3, seconds=6),
    ],
    finishers=[
        A("White Dragon's Holy Judgement", "A pillar of white light falls on the place you look at.", "meteor_drop", "minecraft:beacon", "SPIRIT_AURA",
          damage=30, radius=6, range=24, delay=30, cooldown=1500, cost=.07),
        A("Crown Burst", "A radiant cone of light fans across the field in front of you.", "cone_strike", "minecraft:nether_star", "LIGHTNING_SPEAR",
          damage=28, range=7, half_angle=60, targets=10, knockback=1.3, cooldown=1300, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="poison_dragon_slayer", name="Poison Dragon Slayer", franchise=F,
    desc="Venom, listening and patience. A dragon slayer who afflicts, drains and hunts by the beat of a foe's heart.",
    color="#6A1B9A", background="blood", icon="minecraft:spider_eye",
    book_name="Venom Scroll of the Serpent Dragon", book_lore="Every heartbeat is a countdown.",
    stances=[
        S("Venom Skin", "Seep poison through your skin: you regenerate and are tougher, but less swift.", "minecraft:poisonous_potato", "POISON_BREATH",
          regen=.8, armor=2, speed_pct=-.05),
        S("Cobra Strike", "Coil up and strike: strong blows and extra crit chance, a thin guard.", "minecraft:spider_eye", "WIND_SLASH",
          damage_pct=.25, crit_chance=.1, armor=-3),
    ],
    stats=[
        T("Serpent Blood", "Raises your maximum health.", "max_health", 4, "add", "minecraft:fermented_spider_eye"),
        T("Sharp Fangs", "Raises your attack damage.", "attack_damage", 1, "add", "minecraft:spider_eye"),
        T("Heart Listener", "Reach foes a little farther away.", "entity_interaction_range", .4, "add", "minecraft:echo_shard"),
    ],
    arts=[
        A("Poison Dragon's Roar", "A cloud of venom over a cone: poison and queasiness.", "debuff_cone", "minecraft:poisonous_potato", "POISON_BREATH",
          range=6, half_angle=50, seconds=9, effect="poison", effect2="nausea", seconds2=6),
        A("Poison Dragon's Fang", "A venomous bite that also feeds you with the foe's strength.", "drain_bite", "minecraft:spider_eye", "POISON_BREATH",
          damage=7, range=3.4, heal_pct=.4),
        A("Serpent Wave", "A rolling wave of venom along the ground.", "ground_wave", "minecraft:slime_ball", "POISON_BREATH",
          damage=6, length=11, width=2.5),
        A("Heartbeat Hunt", "Listen for the foe's pulse: it is marked and takes extra damage.", "mark", "minecraft:sculk_sensor", "MAGIC_CIRCLE",
          range=24, seconds=10, bonus=.28),
        A("Toxic Haze", "Exhale a green haze that blinds creatures around you while you slip away.", "smoke", "minecraft:green_dye", "POISON_BREATH",
          radius=5, seconds=6),
        A("Venom Whirl", "Spin with poisoned fists, hitting everything around you.", "whirlwind", "minecraft:fermented_spider_eye", "POISON_BREATH",
          damage=3.5, radius=3.4, hits=5),
        A("Coil Retreat", "Slip away from the fight with a quick hop of a few blocks.", "blink", "minecraft:string", "SPATIAL_RIFT",
          distance=8),
    ],
    passives=[
        P("Venomous Touch", "Your blows may poison the foe.", "on_hit_effect", "minecraft:spider_eye", effect="poison", seconds=4, chance=.3),
        P("Toxic Skin", "Whoever strikes you may be poisoned.", "on_hurt_retaliate", "minecraft:poisonous_potato", effect="poison", seconds=4, chance=.25),
        P("Antivenom Blood", "Take less magic damage.", "resist", "minecraft:milk_bucket", damage_kind="magic"),
        P("Rotting Cloud", "Enemies around you are slowly poisoned.", "aura", "minecraft:fermented_spider_eye", effect="poison", target="enemies", radius=5),
        P("Man Hunter", "Deal more damage to other players.", "dmg_vs", "minecraft:skeleton_skull", category="player", bonus=.18),
        P("Still Waters", "Heal while sneaking, waiting for the shot.", "regen_when", "minecraft:lily_pad", condition="sneaking", per_second=.5),
        P("Feast of the Fallen", "A kill heals you and grants regeneration.", "kill_reward", "minecraft:rotten_flesh", effect="regeneration", heal=3, seconds=5),
    ],
    finishers=[
        A("Hydra Burst", "Many heads of venom burst outward from you in every direction.", "ring_burst", "minecraft:dragon_breath", "POISON_BREATH",
          damage=25, radius=7, targets=12, knockback=1.5, cooldown=1400, cost=.06),
        A("Twin Fang Execution", "Two final bites that cripple the foe and return health to you.", "drain_bite", "minecraft:spider_eye", "POISON_BREATH",
          damage=36, range=4, heal_pct=.75, cooldown=1200, cost=.06),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------------------
LINES.append(Line(
    id="heavenly_fire_god_slayer", name="Heavenly Fire God Slayer", franchise=F,
    desc="Black flame that does not care what it burns. An arrogant, punishing fire style that gets stronger the more it burns, to the user as well.",
    color="#212121", background="magma", icon="minecraft:soul_campfire",
    book_name="Tome of the Black Pyre", book_lore="Gods burn too.",
    stances=[
        S("Heaven's Blaze", "Wreath yourself in black fire: hits harder and faster, but burns through your guard.", "minecraft:soul_torch", "FIRE_SPIRAL",
          damage_pct=.35, speed_pct=.05, armor=-3),
        S("Pyre Sovereign", "Stand like a king in a ring of flame: tougher, with a faint recovery.", "minecraft:crying_obsidian", "FLAME_TRAIL",
          armor=3, knockback_res=.2, regen=.5),
    ],
    stats=[
        T("Pyre Heart", "Raises your maximum health.", "max_health", 4, "add", "minecraft:blaze_powder"),
        T("Scorching Hand", "Raises your attack damage.", "attack_damage", 1.3, "add", "minecraft:blaze_rod"),
        T("Ash Step", "You are quicker on your feet.", "movement_speed", .04, "mult", "minecraft:soul_sand"),
    ],
    arts=[
        A("Black Flame Lash", "A whip of black fire that leaps from foe to foe.", "chain_hit", "minecraft:soul_torch", "FLAME_TRAIL",
          damage=6, jumps=4, jump_range=6),
        A("Burning Blessing", "Take the flame into yourself: strength and haste for a time.", "buff_self", "minecraft:blaze_powder", "FIRE_BURST",
          effect="strength", effect2="haste", seconds=14, amp=1, amp2=0),
        A("Pyre Plunge", "Leap high and come down in a burst of black flame.", "leap_slam", "minecraft:magma_block", "FLAME_EXPLOSION",
          damage=10, radius=3.4, height=.9, knockback=1.4),
        A("Hell's Tide", "A wall of fire rolls along the ground in front of you.", "ground_wave", "minecraft:fire_charge", "FLAME_TRAIL",
          damage=7, length=11, width=2.5),
        A("Pyre Tornado", "Spin in a column of black fire, hitting everything around you.", "whirlwind", "minecraft:campfire", "FIRE_SPIRAL",
          damage=3.5, radius=3.5, hits=5),
        A("Gaze of Ruin", "A burning glare that weakens and blinds the foes in a cone.", "debuff_cone", "minecraft:ender_eye", "FIRE_BURST",
          range=6, half_angle=50, seconds=7, effect="weakness", effect2="blindness", seconds2=4),
        A("Falling Pyre", "Call a star of black fire down where you look.", "meteor_drop", "minecraft:fire_charge", "FLAME_EXPLOSION",
          damage=14, radius=4, range=20, delay=28),
    ],
    passives=[
        P("Pyre Rage", "While burning you gain strength.", "self_effect_when", "minecraft:blaze_powder", effect="strength", condition="on_fire"),
        P("Heat of Heaven", "Take far less fire damage.", "resist", "minecraft:fire_charge", damage_kind="fire"),
        P("Ember Mending", "Heal while burning: you feed on the flame.", "regen_when", "minecraft:magma_cream", condition="on_fire", per_second=.8),
        P("Scorching Crit", "Your blows may strike critically.", "crit", "minecraft:flint_and_steel", chance=.13, mult=1.7),
        P("Climbing Heat", "Blows in a row grow stronger until you stop fighting.", "momentum", "minecraft:campfire", per_stack=.05, max_stacks=6),
        P("Burning Pride", "Part of the damage you take is returned in flame.", "thorns", "minecraft:lava_bucket", pct=.2, rest=20),
        P("Cinder Diet", "You tire more slowly, fed by flame.", "hunger_saver", "minecraft:cooked_porkchop", pct=.2),
        P("Pyre Reaper", "Deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:coal", threshold=.25, bonus=.6),
    ],
    finishers=[
        A("Black Sun", "Call a huge sphere of black fire down on the place you look at.", "meteor_drop", "minecraft:nether_star", "FLAME_EXPLOSION",
          damage=32, radius=7, range=24, delay=35, cooldown=1600, cost=.07),
        A("Fire God's Tsunami", "A wave of black flame sweeps across the ground in front of you.", "ground_wave", "minecraft:lava_bucket", "FLAME_TRAIL",
          damage=30, length=20, width=5, knockback=1.8, cooldown=1300, cost=.06),
    ],
))
