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
