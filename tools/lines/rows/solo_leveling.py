"""Solo Leveling: 27 unique skill lines (hunter classes, the System, the Shadow Monarch's arts, Monarch powers, Ruler authorities, dagger and assassin techniques, and two Evolution skills: Antares and Baek Yoonho).

Canon note: names follow the story's vocabulary; every numeric effect and wording is this mod's own design.
"""
from lines_lib import Line, Stance, Stat, Art, Pas

F = "solo_leveling"


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
    id="shadow_extraction", name="Arise: Shadow Extraction", franchise=F,
    desc="The Shadow Monarch's first gift: rip the shades of the fallen out of the dark and make their strength your own. A necromancer's line of drain, fear and harvested souls.",
    color="#6a3de8", background="void", icon="minecraft:wither_skeleton_skull",
    book_name="Grimoire of the Fallen Shades", book_lore="Every grave is a recruiting office, and the dead never say no.",
    stances=[
        S("Stance of the Gathering Dark", "Shadows pool around your feet and sharpen your blows, but they leave your guard thin.", "minecraft:soul_lantern", damage_pct=0.25, armor=-2, vfx="SHADOW_POOL"),
        S("Stance of Quiet Graves", "A cold stillness settles on you: slower to move, hard to hurt and slowly knitting your wounds.", "minecraft:soul_campfire", armor=4, speed_pct=-0.1, regen=1.0, vfx="DEVIL_CIRCLE"),
    ],
    stats=[
        T("Monarch's Mana Pool", "Ashborn's reserves flow through you: more maximum health.", "max_health", 2, "minecraft:ender_eye"),
        T("Soul-Bound Resolve", "The dead stand behind you and your hide hardens.", "armor_toughness", 1, "minecraft:netherite_scrap"),
        T("Shade-Touched Reflexes", "Your shadow moves a heartbeat before you do.", "movement_speed", 0.03, "minecraft:phantom_membrane", op="mult"),
        T("Grave Reach", "Your arms stretch through the dark, extending your reach.", "entity_interaction_range", 0.3, "minecraft:ender_pearl"),
    ],
    arts=[
        A("Arise!", "Reach into a foe's last breath and tear a shade of its strength loose; you heal for a large share of the blow.", "drain_bite", "minecraft:soul_torch", damage=7, heal_pct=0.35, vfx="SHADOW_POOL"),
        A("Shade Surge", "Black mist erupts around you and hurls nearby foes back.", "ring_burst", "minecraft:black_dye", damage=6, radius=4, knockback=0.9, vfx="DEVIL_CIRCLE"),
        A("Soldier's Chain Lash", "Whips of shadow lash one foe and leap from enemy to enemy.", "chain_hit", "minecraft:chain", damage=6, jumps=3, vfx="SEAL_CHAINS"),
        A("Dread Whisper", "The dead whisper through a cone before you, sapping the strength and slowing the steps of the living.", "debuff_cone", "minecraft:fermented_spider_eye", effect="weakness", effect2="slowness", seconds=6, seconds2=6, vfx="SHADOW_POOL"),
        A("Umbral Grasp", "Hands of shadow drag the creatures around you to your feet.", "pull", "minecraft:lead", radius=7, strength=1.0, damage=3, vfx="SEAL_CHAINS"),
        A("Marked for Extraction", "Brand a foe with violet sigils; it takes extra damage while you prepare to claim its shade.", "mark", "minecraft:name_tag", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Shade Ward", "A cloak of grave mist flings part of every blow you suffer back at the attacker.", "ward_reflect", "minecraft:echo_shard", reflect_pct=0.35, vfx="DEVIL_CIRCLE"),
    ],
    passives=[
        P("Spoils of the Fallen", "Every kill heals you and fills you with the dead one's vigor.", "kill_reward", "minecraft:rotten_flesh", heal=4, effect="strength", seconds=6),
        P("Gravebound Hunger", "Your blows siphon life back to you.", "lifesteal", "minecraft:spider_eye", pct=0.06),
        P("Midnight Dominion", "Under the night sky your shadows swell and you strike harder.", "dmg_when", "minecraft:clock", condition="night", bonus=0.2),
        P("Presence of the Host", "An unseen army stands at your shoulder; allies near you feel stronger.", "aura", "minecraft:white_banner", effect="strength", target="allies", radius=7),
        P("Shadows Mend Shadows", "While fighting, your wounds are stitched by creeping dark.", "regen_when", "minecraft:glow_ink_sac", condition="in_combat", per_second=0.6),
        P("Last Shade Standing", "When you are nearly dead a shade throws itself over you and shields you.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Harvest of Souls", "Kills yield extra experience as the shades pay tribute.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
    ],
    finishers=[
        A("Arise, My Army", "Call the entire host of shades: you and your allies in a wide ring gain a long surge of Strength.", "buff_aura", "minecraft:nether_star", effect="strength", radius=12, seconds=20, amp=1, cooldown=1500, vfx="MAGIC_CIRCLE"),
        A("Requiem of the Shadow Monarch", "A tidal wave of shadow explodes around you, smashing every foe nearby.", "ring_burst", "minecraft:dragon_breath", damage=22, radius=6, targets=8, knockback=1.5, cooldown=1400, vfx="SHADOW_POOL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 2
LINES.append(Line(
    id="shadow_exchange", name="Shadow Exchange", franchise=F,
    desc="Trade places with a shadow soldier and appear wherever your shades stand. A line of teleports, ambush strikes and vanishing acts.",
    color="#3b2f9e", background="dream", icon="minecraft:ender_pearl",
    book_name="Treatise of Swapped Places", book_lore="Where your shadow stands, you may already be.",
    stances=[
        S("Stance of the Slipping Shade", "Light and weightless: you move faster and strike with extra bite, at the cost of armor.", "minecraft:feather", speed_pct=0.2, damage_pct=0.1, armor=-3, vfx="SHADOW_POOL"),
        S("Stance of the Held Breath", "You make yourself small and hard to read: sturdier and quicker to recover, but your blows are weaker.", "minecraft:ink_sac", armor=3, regen=0.8, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Swapper's Footwork", "Practice at being elsewhere makes you quick.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Weightless Landing", "A shadow always catches you when you fall.", "safe_fall_distance", 1.5, "minecraft:slime_ball"),
        T("Edge of the Blade", "Appearing behind a foe gives you the first strike.", "attack_damage", 1, "minecraft:iron_sword"),
    ],
    arts=[
        A("Swap with the Shade", "Trade places with your shadow and pop out several blocks ahead.", "blink", "minecraft:ender_eye", distance=10, vfx="SPATIAL_RIFT"),
        A("Shadow Step Cut", "Melt into the floor and surface through the enemy line, cutting everything in your path.", "dash_strike", "minecraft:iron_sword", distance=7, damage=7, vfx="SHADOW_POOL"),
        A("Veil of Black Mist", "Thick black mist swallows the creatures around you, blinding them while you slip away.", "smoke", "minecraft:coal", radius=5, seconds=6, vfx="SHADOW_POOL"),
        A("Reappear Behind", "Step out of nowhere and carve a cone of steel across the enemies ahead.", "cone_strike", "minecraft:stone_sword", damage=8, range=4, vfx="WIND_SLASH"),
        A("Quickening Dark", "Your shadows quicken your limbs: Speed and Haste for a short while.", "buff_self", "minecraft:sugar", effect="speed", effect2="haste", seconds=14, amp=1, vfx="SPIRIT_AURA"),
        A("Fall Upon Them", "Leap from the dark and slam onto the foes below.", "leap_slam", "minecraft:anvil", damage=8, radius=3, height=0.9, vfx="SHADOW_POOL"),
        A("Coup from the Void", "A blow that hits much harder the weaker the foe already is.", "execute", "minecraft:wither_rose", damage=9, bonus=0.8, vfx="SPATIAL_RIFT"),
    ],
    passives=[
        P("Never Where You Look", "You may slip a blow completely by trading with your shadow.", "dodge", "minecraft:rabbit_foot", chance=0.1),
        P("Footprints of Smoke", "Chaining hits builds momentum that makes you hit harder.", "momentum", "minecraft:gunpowder", max_stacks=6),
        P("Rushing Silhouette", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:golden_boots", bonus=0.25),
        P("Shadow Cushion", "You take far less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.5),
        P("Vital Flicker", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.7),
        P("Unseen While Crouched", "While sneaking you fade from sight.", "self_effect_when", "minecraft:glass", effect="invisibility", condition="sneaking"),
        P("Ambush Predator", "You hit harder while sneaking.", "dmg_when", "minecraft:spyglass", condition="sneaking", bonus=0.35),
    ],
    finishers=[
        A("Chain of Exchanges", "Hop through every shadow in sight in a blink, striking each foe in a long line.", "dash_strike", "minecraft:chorus_fruit", distance=10, damage=20, targets=6, cooldown=1300, vfx="SPATIAL_RIFT"),
        A("Monarch's Doppelganger", "Leave a dark double behind and strike from the other side: a huge cone of void steel.", "cone_strike", "minecraft:end_crystal", damage=22, range=6, half_angle=60, targets=6, cooldown=1400, vfx="MIRROR_PANE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 3
LINES.append(Line(
    id="igris_blood_red", name="Igris, the Blood-Red Knight", franchise=F,
    desc="Fight beside the crimson knight whose swordsmanship you inherited: clean arcs, brutal executions and a never-yielding oath.",
    color="#b3122b", background="blood", icon="minecraft:netherite_sword",
    book_name="Oath of the Crimson Cape", book_lore="Kneel only to your Monarch; everything else gets the blade.",
    stances=[
        S("Knight's Salute", "A disciplined, upright stance: heavier blows, but you cannot afford to move much.", "minecraft:iron_sword", damage_pct=0.25, speed_pct=-0.08, vfx="WIND_SLASH"),
        S("Crimson Guard", "Blade held across the chest: harder to hurt and harder to push around, but you strike lighter.", "minecraft:shield", armor=5, knockback_res=0.4, damage_pct=-0.15, vfx="FIRE_BURST"),
    ],
    stats=[
        T("Knight's Edge", "Years of drilling sharpen your sword arm.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Tempered Plate", "A knight's armor settles on your frame.", "armor", 1, "minecraft:iron_chestplate"),
        T("Oathbound Spine", "Nothing knocks a sworn knight off his feet.", "knockback_resistance", 0.1, "minecraft:anvil"),
        T("Cape Whip", "Sword speed that makes the red cape snap.", "attack_speed", 0.05, "minecraft:red_banner", op="mult"),
    ],
    arts=[
        A("Crimson Blade Arc", "A broad red crescent of steel that cuts the foes in front of you.", "cone_strike", "minecraft:iron_sword", damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Knight's Charge", "Lower the shoulder and run the foes down along a straight line.", "dash_strike", "minecraft:iron_horse_armor", distance=7, damage=8, vfx="FLAME_TRAIL"),
        A("Headsman's Verdict", "One decisive cut that is far deadlier against a wounded foe.", "execute", "minecraft:iron_axe", damage=10, bonus=1.0, vfx="WIND_SLASH"),
        A("Blood Wake", "Send a wave of red slash along the ground ahead of you.", "ground_wave", "minecraft:redstone", damage=7, length=10, width=2, vfx="WIND_SLASH"),
        A("Red Cape Cyclone", "Spin in place, your blade and cape sweeping everything around you again and again.", "whirlwind", "minecraft:red_dye", damage=4, hits=5, radius=3.5, vfx="WIND_RING"),
        A("Knight Rises", "A rising slash that launches the foe in front of you into the air.", "uppercut", "minecraft:wooden_sword", damage=8, launch=0.8, vfx="WIND_SLASH"),
        A("Oath of Steel", "Swear the knight's oath: Strength and Resistance for a while.", "buff_self", "minecraft:iron_ingot", effect="strength", effect2="resistance", seconds=14, vfx="SPIRIT_AURA"),
    ],
    passives=[
        P("Flowing Swordplay", "Every few sword blows in a row land with extra force.", "combo", "minecraft:stone_sword", hits=4, bonus=0.6),
        P("Cutting Edge", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Execution Doctrine", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.8),
        P("Blade Held High", "You take less damage while wielding a sword.", "reduce_when", "minecraft:shield", condition="holding_sword", pct=0.12),
        P("Swordsman's Poise", "You hit harder while wielding a sword.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword", bonus=0.2),
        P("Shield-Splitting Blow", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.35),
        P("Knight of the Monarch", "Against bosses your strikes bite deeper.", "dmg_vs", "minecraft:wither_skeleton_skull", category="boss", bonus=0.2),
        P("Unbowed", "When nearly dead, the knight's oath shields you briefly.", "last_stand", "minecraft:totem_of_undying", seconds=5),
    ],
    finishers=[
        A("Sword of the Crimson Reckoning", "A single blood-red wave of steel that devastates the whole line ahead.", "ground_wave", "minecraft:netherite_sword", damage=24, length=16, width=4, knockback=1.2, cooldown=1400, vfx="FLAME_EXPLOSION"),
        A("Hundred Cuts of the Knight", "Spin through a flurry of cuts that ravages everything around you.", "whirlwind", "minecraft:diamond_axe", damage=6, hits=7, radius=5, cooldown=1300, vfx="WIND_TORNADO"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 4
LINES.append(Line(
    id="beru_ant_king", name="Beru, the Ant King", franchise=F,
    desc="The insect marshal's savage grace: leaping pounces, mandible rushes and regeneration that makes him all but unkillable. Feral, fast and hungry.",
    color="#2f6b3a", background="ruin", icon="minecraft:spider_eye",
    book_name="Chronicle of the Chitin Throne", book_lore="Loyalty is the only thing harder than his shell.",
    stances=[
        S("Swarm-Lord's Hunt", "Hunched low, mandibles open: much faster and deadlier, but thin-skinned.", "minecraft:spider_eye", speed_pct=0.2, damage_pct=0.2, armor=-3, vfx="POISON_BREATH"),
        S("Chitin Carapace", "Plates lock into place: heavily armored and regenerating, but ponderous.", "minecraft:turtle_helmet", armor=6, regen=1.2, speed_pct=-0.12, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Regenerating Marrow", "Ant-king vitality: more maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Mandible Power", "Jaws and claws that rend steel.", "attack_damage", 1.5, "minecraft:bone"),
        T("Pouncing Legs", "Powerful hind legs improve your jumping.", "jump_strength", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Wall-Climber's Grip", "Sure claws let you step up higher.", "step_height", 0.4, "minecraft:cobweb"),
    ],
    arts=[
        A("Mandible Rush", "Bolt forward on four legs and rake everything you pass through.", "dash_strike", "minecraft:bone", distance=8, damage=8, vfx="FLAME_TRAIL"),
        A("Swarm Claw", "A claw strike that jumps from one foe to the next.", "chain_hit", "minecraft:flint", damage=6, jumps=4, vfx="WIND_SLASH"),
        A("Ant King's Feast", "Sink your jaws into a foe and heal for a large share.", "drain_bite", "minecraft:rotten_flesh", damage=8, heal_pct=0.4, vfx="POISON_BREATH"),
        A("Sky-Rending Pounce", "Leap high and crash onto the foes below, shaking the ground.", "leap_slam", "minecraft:slime_ball", damage=9, radius=3.5, height=1.0, vfx="EARTH_SPIKES"),
        A("Chitin Shriek", "An ear-splitting screech hurls everything around you away.", "ring_burst", "minecraft:goat_horn", damage=6, radius=4.5, knockback=1.3, vfx="WIND_RING"),
        A("Formic Spray", "Spit a cone of caustic venom that poisons and blinds.", "debuff_cone", "minecraft:fermented_spider_eye", effect="poison", effect2="blindness", seconds=7, seconds2=4, vfx="POISON_BREATH"),
        A("Regrowth Molt", "Shed your wounds in a molt, healing and regenerating.", "heal_self", "minecraft:honeycomb", amount=8, regen_seconds=8, vfx="SPIRIT_AURA"),
    ],
    passives=[
        P("Insatiable Maw", "Your blows heal you.", "lifesteal", "minecraft:cooked_beef", pct=0.08),
        P("Endless Regeneration", "In the thick of a fight your wounds close quickly.", "regen_when", "minecraft:golden_carrot", condition="in_combat", per_second=0.9),
        P("Venomous Claws", "Your claws may poison the foe.", "on_hit_effect", "minecraft:poisonous_potato", effect="poison", seconds=5, chance=0.3),
        P("Bane of Kings", "You deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.25),
        P("Hunt Rush", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.3),
        P("Self-Mending Shell", "Your wounds close over time, even out of combat.", "mending", "minecraft:turtle_scute", seconds=14),
        P("Spiked Exoskeleton", "A share of the damage you take is thrown back at attackers.", "thorns", "minecraft:pointed_dripstone", pct=0.2),
        P("Devour the Fallen", "Each kill heals you and sharpens your speed.", "kill_reward", "minecraft:bone_meal", heal=5, effect="speed", seconds=6),
    ],
    finishers=[
        A("Ant King's Rampage", "Rip through the foes in front of you in a blur of mandibles and claws.", "dash_strike", "minecraft:netherite_axe", distance=11, damage=22, targets=6, cooldown=1300, vfx="FLAME_TRAIL"),
        A("Plague of Wings", "A swarm bursts from your back, devouring everything around you.", "whirlwind", "minecraft:honey_bottle", damage=6, hits=7, radius=6, cooldown=1400, vfx="POISON_BREATH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 5
LINES.append(Line(
    id="iron_shadow_guard", name="Iron, the Shadow Vanguard", franchise=F,
    desc="A towering tank-soldier of the shadow army: planted feet, a wall of dark steel and a roar that makes every enemy look at him.",
    color="#4d5663", background="iron", icon="minecraft:iron_chestplate",
    book_name="Ledger of the Black Wall", book_lore="They said nothing passes. Nothing has.",
    stances=[
        S("Planted Wall", "Dig in: huge armor and knockback immunity, but you move like a wall.", "minecraft:iron_block", armor=8, knockback_res=0.7, speed_pct=-0.2, vfx="EARTH_SPIKES"),
        S("Vanguard's Advance", "Shoulder forward: a little faster and harder-hitting, with lighter armor.", "minecraft:iron_boots", speed_pct=0.05, damage_pct=0.15, armor=2, vfx="WIND_SLASH"),
    ],
    stats=[
        T("Wall of Black Iron", "Layered shadow plate: much more armor.", "armor", 2, "minecraft:iron_chestplate"),
        T("Anchored Bulk", "Mass that cannot be moved: strong knockback resistance.", "knockback_resistance", 0.15, "minecraft:anvil"),
        T("Reservoir of Vigor", "A deep well of vitality: more maximum health.", "max_health", 4, "minecraft:golden_apple"),
        T("Heavy Plate Toughness", "Armor that shrugs off the worst blows.", "armor_toughness", 1, "minecraft:diamond_chestplate"),
    ],
    arts=[
        A("Iron's Challenge", "Bellow a challenge that drags the attention of every hostile nearby onto you.", "taunt", "minecraft:goat_horn", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
        A("Obsidian Greatshield", "Raise a dark shield that soaks a mountain of damage.", "shield", "minecraft:shield", absorb_hearts=6, resist_amp=1, vfx="MIRROR_PANE"),
        A("Unbroken Guard", "For a time a big share of the damage you take is flung back at the attacker.", "ward_reflect", "minecraft:crying_obsidian", reflect_pct=0.4, vfx="EARTH_SPIKES"),
        A("Pavise Slam", "Slam the shield into the ground, stunning everything around you.", "stun_ring", "minecraft:iron_door", radius=4.5, seconds=2, vfx="STONE_SPIKES"),
        A("Shieldline Advance", "Drive a wave of force forward along the ground behind your shield.", "ground_wave", "minecraft:iron_bars", damage=6, length=10, width=3, knockback=1.2, vfx="STONE_SPIKES"),
        A("Hook the Horde", "Drag foes near you into your guard.", "pull", "minecraft:chain", radius=8, strength=1.1, targets=6, damage=4, vfx="SEAL_CHAINS"),
        A("Rampart's Rally", "Shout orders: you and your allies gain Resistance.", "buff_aura", "minecraft:bell", effect="resistance", radius=9, seconds=12, vfx="MAGIC_CIRCLE"),
    ],
    passives=[
        P("Iron Hide", "You take less damage whenever you are in the thick of a fight.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.12),
        P("Bristling Plates", "Part of the damage you take is returned to the attacker.", "thorns", "minecraft:iron_nugget", pct=0.25),
        P("Stone in the Storm", "Explosions break against you.", "resist", "minecraft:tnt", damage_kind="explosion", pct=0.3),
        P("Immovable Last Stand", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Self-Repairing Plate", "Your wounds close slowly over time, even out of combat.", "mending", "minecraft:iron_block", seconds=12, amount=1),
        P("Shield Bash Reflex", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.4),
        P("Fall Like a Boulder", "You take less fall damage.", "fall_guard", "minecraft:cobblestone", pct=0.6),
    ],
    finishers=[
        A("Fortress Unbroken", "Raise an immense wall of absorption and resistance around yourself.", "shield", "minecraft:beacon", absorb_hearts=12, seconds=14, resist_amp=1, cooldown=1500, vfx="MIRROR_PANE"),
        A("Vanguard's Quake", "Stamp the earth and send a brutal shockwave through all nearby foes.", "ring_burst", "minecraft:anvil", damage=21, radius=6, targets=8, knockback=1.8, cooldown=1300, vfx="STONE_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 6
LINES.append(Line(
    id="monarchs_domain", name="Monarch's Domain", franchise=F,
    desc="Claim the ground around you as your own territory: pressure that pins, crushes and drags foes while you stand untouched at its centre.",
    color="#7d63ff", background="stellar", icon="minecraft:end_crystal",
    book_name="Edict of the Sovereign Ground", book_lore="Within the Domain, even gravity asks permission.",
    stances=[
        S("Domain Expanded", "Your territory swells, crushing and empowering: more damage and armor, but you are slow to move.", "minecraft:beacon", damage_pct=0.2, armor=3, speed_pct=-0.12, vfx="MAGIC_CIRCLE"),
        S("Domain Contracted", "Tighten the domain around your body: swifter and self-healing, with softer blows.", "minecraft:amethyst_shard", speed_pct=0.12, regen=0.8, damage_pct=-0.1, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Weight of Sovereignty", "The domain's pressure toughens your frame.", "armor_toughness", 1, "minecraft:netherite_chestplate"),
        T("Throne-Room Vigor", "More maximum health from standing on your own ground.", "max_health", 2, "minecraft:golden_apple"),
        T("Unmoved by Lesser Forces", "Hard to knock off your feet.", "knockback_resistance", 0.1, "minecraft:obsidian"),
    ],
    arts=[
        A("Gravity Clamp", "The domain bears down on the ground around you, freezing creatures in place for a moment.", "stun_ring", "minecraft:anvil", radius=5, seconds=2, vfx="MAGIC_CIRCLE"),
        A("Domain Collapse", "The edges of your territory fold inward and drag every creature toward the centre.", "pull", "minecraft:ender_eye", radius=9, strength=1.3, targets=6, damage=4, vfx="SPATIAL_RIFT"),
        A("Sovereign's Decree", "Declare the law of the domain: you and your allies gain Strength.", "buff_aura", "minecraft:white_banner", effect="strength", radius=10, seconds=14, vfx="MAGIC_CIRCLE"),
        A("Crushing Pressure", "Raw pressure floods a cone in front of you, slowing and exhausting everything in it.", "debuff_cone", "minecraft:heavy_core", effect="slowness", effect2="mining_fatigue", seconds=6, seconds2=6, amp=1, vfx="SEAL_CHAINS"),
        A("Domain Shockwave", "The domain pulses once, striking every creature in range.", "ring_burst", "minecraft:crying_obsidian", damage=7, radius=5, targets=7, knockback=1.0, vfx="MAGIC_CIRCLE"),
        A("Sovereign's Gaze", "Fix your stare on one creature in your territory; it takes extra damage for a while.", "mark", "minecraft:ender_eye", bonus=0.35, vfx="DEVIL_CIRCLE"),
        A("Aegis of the Domain", "A sphere of the domain hardens around you as a thick absorption shield.", "shield", "minecraft:shield", absorb_hearts=6, resist_amp=1, vfx="MIRROR_PANE"),
    ],
    passives=[
        P("Oppressive Field", "Enemies near you are constantly slowed by the weight of the domain.", "aura", "minecraft:cobweb", effect="slowness", target="enemies", radius=7),
        P("The Domain Endures", "While in a fight the domain softens every blow you take.", "reduce_when", "minecraft:obsidian", condition="in_combat", pct=0.12),
        P("Warded Territory", "Magic breaks against your territory.", "resist", "minecraft:amethyst_shard", damage_kind="magic", pct=0.3),
        P("Unchallenged Sovereign", "At full health your blows land harder.", "dmg_when", "minecraft:gold_block", condition="full_health", bonus=0.2),
        P("Calm of the Throne Room", "Deep underground, in the dark, your wounds close steadily.", "regen_when", "minecraft:deepslate", condition="underground", per_second=0.8),
        P("Crushing Weight", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.3),
        P("Unyielding Law", "A share of the damage you take is thrown back at the attacker.", "thorns", "minecraft:iron_nugget", pct=0.2),
    ],
    finishers=[
        A("Absolute Domain: Stillness", "The domain closes like a fist: every creature around you is locked in place.", "stun_ring", "minecraft:nether_star", radius=8, seconds=3, targets=10, cooldown=1500, vfx="SPATIAL_RIFT"),
        A("Domain Collapse: Falling Star", "A star of compressed domain falls on the place you look at.", "meteor_drop", "minecraft:fire_charge", damage=26, radius=6, range=24, cooldown=1400, vfx="FLAME_EXPLOSION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 7
LINES.append(Line(
    id="rulers_authority", name="Ruler's Authority", franchise=F,
    desc="Invisible hands of force bend the world to your will: hurl, lift and crush from across the room without touching a thing.",
    color="#9fb6ff", background="tower", icon="minecraft:end_rod",
    book_name="Compendium of the Unseen Hand", book_lore="Not a spell. A command the air has agreed to obey.",
    stances=[
        S("Grip Tightened", "Your will clenches around everything: heavier blows at the price of mobility.", "minecraft:iron_block", damage_pct=0.25, speed_pct=-0.1, vfx="SEAL_CHAINS"),
        S("Open Palm", "A light, open will: fast and hard to hit but with softer blows.", "minecraft:feather", speed_pct=0.15, armor=-1, regen=0.5, damage_pct=-0.05, vfx="WIND_RING"),
    ],
    stats=[
        T("Reach of the Will", "Telekinetic arms extend your reach.", "entity_interaction_range", 0.6, "minecraft:lead"),
        T("Weightless Mind", "A clear mind speeds your reflexes.", "attack_speed", 0.06, "minecraft:clock", op="mult"),
        T("Force Plating", "Ever-present force plates across the body.", "armor", 1, "minecraft:chainmail_chestplate"),
    ],
    arts=[
        A("Ruler's Hand", "An invisible hand snatches creatures around you and slams them toward you.", "pull", "minecraft:lead", radius=8, strength=1.2, targets=5, damage=5, vfx="THREAD_LINE"),
        A("Telekinetic Lance", "Compress force into a spear that pierces everything along your line of sight.", "ray_shot", "minecraft:end_rod", damage=9, range=22, pierce=2, vfx="LIGHTNING_SPEAR"),
        A("Rain of Thrown Things", "Rubble, blades and debris fly at your target one after another.", "barrage", "minecraft:flint", damage=3, shots=7, range=16, vfx="WEAPON_CONSTRUCTS"),
        A("Lift", "A hand under the chin throws the foe in front of you high into the air.", "uppercut", "minecraft:wind_charge", damage=7, range=4, launch=1.0, vfx="WIND_RING"),
        A("Hurled Blades", "Weapons ripped from the air strike one foe and ricochet to the next.", "chain_hit", "minecraft:iron_sword", damage=6, jumps=4, vfx="WEAPON_CONSTRUCTS"),
        A("Grasp of the Ruler", "An unseen hand pins a single creature; it takes extra damage for a while.", "mark", "minecraft:lead", bonus=0.3, vfx="THREAD_LINE"),
        A("Force Barrier", "A wall of force absorbs a great deal of damage for a short time.", "shield", "minecraft:glass", absorb_hearts=5, vfx="MIRROR_PANE"),
    ],
    passives=[
        P("Weightless Float", "While airborne you are cushioned by an invisible hand.", "self_effect_when", "minecraft:feather", effect="slow_falling", condition="airborne"),
        P("Deflecting Hands", "Projectiles are plucked from the air: you take much less projectile damage.", "resist", "minecraft:arrow", damage_kind="projectile", pct=0.35),
        P("Hands in the Way", "Unseen hands may bat a blow away entirely.", "dodge", "minecraft:rabbit_foot", chance=0.1),
        P("Finishing Squeeze", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_bars", threshold=0.25, bonus=0.7),
        P("Effortless Will", "Telekinesis costs you little: you tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Heavy Hand", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.6, chance=0.4),
        P("Meditative Focus", "Crouch in stillness and your wounds close.", "regen_when", "minecraft:enchanted_book", condition="sneaking", per_second=0.9),
    ],
    finishers=[
        A("Ruler's Crush", "Seize an entire rank of enemies at once and squeeze: a huge piercing lance of force.", "ray_shot", "minecraft:nether_star", damage=26, range=30, pierce=4, cooldown=1400, vfx="LIGHTNING_SPEAR"),
        A("Storm of a Thousand Hands", "The air fills with unseen hands that hammer one foe again and again.", "barrage", "minecraft:end_crystal", damage=4, shots=10, range=20, cooldown=1300, vfx="WEAPON_CONSTRUCTS"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 8
LINES.append(Line(
    id="kasaka_venom_fang", name="Kasaka's Venom Fang", franchise=F,
    desc="A poison-dripping dagger and the serpent's patience: lunge, poison and wait for the venom to finish the job.",
    color="#6fc93a", background="forest", icon="minecraft:fermented_spider_eye",
    book_name="Notes on the Serpent's Tooth", book_lore="Cut once. The rest is arithmetic.",
    stances=[
        S("Fang Drawn", "Dagger out and ready: quick and sharp, but with little protection.", "minecraft:iron_sword", speed_pct=0.1, damage_pct=0.2, armor=-3, vfx="POISON_BREATH"),
        S("Coiled Viper", "Wait, coiled and watchful: critical strikes come easier, at the cost of damage per blow.", "minecraft:lead", crit_chance=0.2, damage_pct=-0.1, armor=2, vfx="SHADOW_POOL"),
    ],
    stats=[
        T("Serpentine Reflexes", "A lunging snake's speed.", "attack_speed", 0.06, "minecraft:rabbit_foot", op="mult"),
        T("Venomed Edge", "A dagger edge honed on poison.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Slither Stride", "Low and light on your feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Viper Lunge", "Dart forward and stab everything in your line, leaving the wounds green.", "dash_strike", "minecraft:iron_sword", distance=6, damage=7, vfx="POISON_BREATH"),
        A("Venom Mist", "Exhale a cone of green mist that poisons and withers everything in it.", "debuff_cone", "minecraft:fermented_spider_eye", effect="poison", effect2="wither", seconds=8, seconds2=4, amp=1, vfx="POISON_BREATH"),
        A("Hydra Fangs", "Strike from three angles at once: the stab jumps between foes.", "chain_hit", "minecraft:spider_eye", damage=5, jumps=3, vfx="POISON_BREATH"),
        A("Fang Sip", "Drive the dagger in and drink some of the foe's life.", "drain_bite", "minecraft:rotten_flesh", damage=6, heal_pct=0.3, vfx="POISON_BREATH"),
        A("Rending Bite", "Slash a tight cone with the venom dagger.", "cone_strike", "minecraft:stone_sword", damage=7, range=3.5, half_angle=45, vfx="WIND_SLASH"),
        A("Needle Spit", "Spit a spray of venom needles at the foe you look at.", "barrage", "minecraft:arrow", damage=2.5, shots=6, range=12, vfx="POISON_BREATH"),
        A("Spore Cloud", "Burst a spore bomb that blinds the creatures around you.", "smoke", "minecraft:red_mushroom", radius=5, seconds=5, vfx="POISON_BREATH"),
    ],
    passives=[
        P("Venom Coating", "Your blows may poison the foe.", "on_hit_effect", "minecraft:poisonous_potato", effect="poison", seconds=6, chance=0.35),
        P("Hit the Vitals", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.18, mult=1.8),
        P("Needle Combo", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_nugget", hits=4, bonus=0.5),
        P("Hunter of Beasts", "You deal more damage to animals.", "dmg_vs", "minecraft:beef", category="animal", bonus=0.25),
        P("Poisoned Reprisal", "Whoever strikes you may be poisoned.", "on_hurt_retaliate", "minecraft:spider_eye", effect="poison", seconds=5, chance=0.35),
        P("Sap the Dying", "Kills heal you and speed you up.", "kill_reward", "minecraft:sugar", heal=3, effect="speed", seconds=5),
        P("Quick Silence", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:wither_rose", threshold=0.2, bonus=0.6),
    ],
    finishers=[
        A("Serpent's Deathblow", "Strike a poisoned foe for a huge execution blow.", "execute", "minecraft:netherite_sword", damage=28, range=4.5, bonus=2.0, cooldown=1300, vfx="POISON_BREATH"),
        A("King Cobra's Cloud", "A vast cloud of venom erupts from your blade, afflicting everything in a wide ring.", "ring_burst", "minecraft:dragon_breath", damage=20, radius=6, targets=7, knockback=0.8, cooldown=1400, vfx="POISON_BREATH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 9
LINES.append(Line(
    id="stealth_skill", name="Skill: Stealth", franchise=F,
    desc="Hide your presence from monsters and hunters alike: dissolve into the dark, relocate silently and strike where the guard is thinnest.",
    color="#33415c", background="city", icon="minecraft:ink_sac",
    book_name="Manual of the Silent Gate", book_lore="Be the part of the dungeon nobody remembers seeing.",
    stances=[
        S("Veiled Presence", "You fade into the background: faster and harder to pin down, but easily hurt.", "minecraft:glass", speed_pct=0.15, armor=-2, crit_chance=0.1, vfx="MIRROR_PANE"),
        S("Held Still", "You become part of the wall: sturdy and healing slowly, with weak blows.", "minecraft:stone", armor=4, regen=0.8, damage_pct=-0.15, speed_pct=-0.05, vfx="SHADOW_POOL"),
    ],
    stats=[
        T("Quiet Steps", "Muffled footfalls, swifter movement.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Rooftop Landing", "A hunter's sure landings.", "safe_fall_distance", 2, "minecraft:feather"),
        T("Eyes in the Dark", "Fortune favours the unseen.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Dissolve into Dark", "Release a puff of shadow that blinds foes while you vanish.", "smoke", "minecraft:coal", radius=5, seconds=6, vfx="SHADOW_POOL"),
        A("Assassin's Hush", "A stab to the weak point that does huge damage to a wounded foe.", "execute", "minecraft:iron_sword", damage=9, bonus=1.2, vfx="WIND_SLASH"),
        A("Ghost Cloak", "Wrap yourself in stealth: Invisibility and Speed for a short while.", "buff_self", "minecraft:glass", effect="invisibility", effect2="speed", seconds=12, vfx="MIRROR_PANE"),
        A("Pick the Weak Link", "Study a target and mark it; it takes extra damage while you hunt it.", "mark", "minecraft:spyglass", bonus=0.35, range=22, vfx="MAGIC_CIRCLE"),
        A("Thrown Dagger", "Flick a dagger down your line of sight, piercing what it hits.", "ray_shot", "minecraft:iron_nugget", damage=7, range=16, pierce=2, vfx="WIND_SLASH"),
        A("Stealth Relocation", "Slip a few blocks forward unseen.", "blink", "minecraft:ender_pearl", distance=9, vfx="SHADOW_POOL"),
        A("Backstab Sweep", "A swift cone of knife work behind the guard.", "cone_strike", "minecraft:stone_sword", damage=7, range=3.5, vfx="WIND_SLASH"),
    ],
    passives=[
        P("Dark-Adapted Eyes", "At night you see as clearly as day.", "self_effect_when", "minecraft:golden_carrot", effect="night_vision", condition="night"),
        P("Backstabber", "You hit harder while sneaking.", "dmg_when", "minecraft:iron_sword", condition="sneaking", bonus=0.35),
        P("Vital Point", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.9),
        P("Ghost Step", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
        P("Pickpocket's Eye", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.15),
        P("Sprint Skill", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.25),
        P("Lean Hunter", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.25),
    ],
    finishers=[
        A("Perfect Assassination", "Appear in front of one foe and end it with one precise cut.", "execute", "minecraft:netherite_sword", damage=30, range=6, bonus=2.0, cooldown=1300, vfx="SHADOW_POOL"),
        A("Night's Curtain", "The whole room goes dark and every creature around you goes blind.", "smoke", "minecraft:black_dye", radius=9, seconds=14, cooldown=1500, vfx="SHADOW_POOL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 10
LINES.append(Line(
    id="bloodlust_pressure", name="Bloodlust", franchise=F,
    desc="Let your killing intent leak out and the weak simply stop: stunned, nauseous and frozen in place while the strong remember fear.",
    color="#c1272d", background="storm", icon="minecraft:red_dye",
    book_name="Primer on Killing Intent", book_lore="The first hunter to flinch has already lost.",
    stances=[
        S("Killing Intent Unleashed", "All restraint dropped: far more damage, with nothing spared for defence.", "minecraft:redstone_block", damage_pct=0.3, armor=-2, vfx="FIRE_BURST"),
        S("Restrained Menace", "A tight, cold hold on the hunger: sturdy and steady, with softer blows.", "minecraft:iron_chestplate", armor=3, knockback_res=0.5, damage_pct=-0.1, vfx="DEVIL_CIRCLE"),
    ],
    stats=[
        T("Predator's Frame", "A body built to intimidate.", "max_health", 2, "minecraft:cooked_beef"),
        T("Raw Menace", "Heavy hands.", "attack_damage", 1.5, "minecraft:netherite_axe"),
        T("Stare-Down Reach", "Your gaze reaches further.", "entity_interaction_range", 0.4, "minecraft:ender_eye"),
    ],
    arts=[
        A("Bloodlust Gaze", "Your killing intent washes over a cone, sickening and weakening everything in it.", "debuff_cone", "minecraft:ender_eye", effect="nausea", effect2="weakness", seconds=7, seconds2=7, vfx="DEVIL_CIRCLE"),
        A("Paralyzing Presence", "Your aura slams down: nearby creatures freeze in place.", "stun_ring", "minecraft:wither_skeleton_skull", radius=5, seconds=2, vfx="SHADOW_POOL"),
        A("Pressure Wave", "Killing intent turned physical: a ring of force that throws foes back.", "ring_burst", "minecraft:redstone", damage=6, radius=4.5, knockback=1.3, vfx="WIND_RING"),
        A("Cull the Weak", "Finish a foe that is already frightened and hurt.", "execute", "minecraft:iron_axe", damage=8, bonus=1.0, vfx="FIRE_BURST"),
        A("Drunk on Dread", "Feed on the fear around you: Strength and Speed for a while.", "buff_self", "minecraft:red_dye", effect="strength", effect2="speed", seconds=14, vfx="SPIRIT_AURA"),
        A("Bait the Beast", "Roar and every hostile nearby turns to look at you.", "taunt", "minecraft:goat_horn", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
        A("Rippling Terror", "A stormy spin of menace that hits all around you several times.", "whirlwind", "minecraft:netherite_axe", damage=3.5, hits=5, radius=4, vfx="WIND_TORNADO"),
    ],
    passives=[
        P("Dread Aura", "Enemies near you are constantly weakened.", "aura", "minecraft:wither_skeleton_skull", effect="weakness", target="enemies", radius=7),
        P("Fear Bites Back", "Whoever strikes you may be sickened.", "on_hurt_retaliate", "minecraft:fermented_spider_eye", effect="nausea", seconds=5, chance=0.3),
        P("Feast on Fear", "Kills heal you and give you Strength.", "kill_reward", "minecraft:rotten_flesh", heal=4, effect="strength", seconds=6),
        P("Mercy Is Weakness", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_axe", threshold=0.25, bonus=0.7),
        P("Cornered Beast", "At low health your blows hit much harder.", "dmg_when", "minecraft:blaze_powder", condition="low_health", bonus=0.35),
        P("Hardened by Fights", "In the thick of a fight you take less damage.", "reduce_when", "minecraft:iron_ingot", condition="in_combat", pct=0.12),
        P("Blood Memory", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.15),
    ],
    finishers=[
        A("Monarch's Roar", "A roar that shatters the nerve of everything near you: a mighty ring of force that hurls them away.", "ring_burst", "minecraft:dragon_head", damage=22, radius=7, targets=8, knockback=2.0, cooldown=1400, vfx="WIND_RING"),
        A("Wall of Killing Intent", "A lane of crushing menace sweeps ahead of you.", "ground_wave", "minecraft:netherite_scrap", damage=24, length=18, width=5, cooldown=1300, vfx="DEVIL_CIRCLE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 11
LINES.append(Line(
    id="quicksilver_rush", name="Quicksilver", franchise=F,
    desc="Time seems to wade through syrup around you. Dashes, afterimages and a blur of cuts for the hunter who is simply never where the blow lands.",
    color="#5de0ff", background="storm", icon="minecraft:lightning_rod",
    book_name="Quicksilver Field Notes", book_lore="Haste is a skill. Patience is what you do with the time it buys.",
    stances=[
        S("Mercury Flow", "You pour yourself across the field: blindingly fast but paper-thin.", "minecraft:sugar", speed_pct=0.28, armor=-3, damage_pct=0.05, vfx="LIGHTNING_FIEND"),
        S("Anchor Point", "Plant a heel and trade the speed for heavier cuts and a steadier stance.", "minecraft:iron_boots", armor=3, knockback_res=0.3, damage_pct=0.15, speed_pct=-0.05, vfx="WIND_RING"),
    ],
    stats=[
        T("Silver Stride", "Faster legs.", "movement_speed", 0.05, "minecraft:golden_boots", op="mult"),
        T("Flickering Hands", "Faster blades.", "attack_speed", 0.06, "minecraft:clock", op="mult"),
        T("Spring-Loaded Heels", "A higher jump.", "jump_strength", 0.05, "minecraft:rabbit_foot", op="mult"),
        T("Rooftop Runner", "Step up kerbs and ledges without slowing.", "step_height", 0.4, "minecraft:ladder"),
    ],
    arts=[
        A("Mercury Dash", "Burst through the foes ahead of you in one silver streak.", "dash_strike", "minecraft:golden_boots", distance=9, damage=7, vfx="LIGHTNING_FIEND"),
        A("Flicker Step", "Teleport a short way in a flash of light.", "blink", "minecraft:ender_pearl", distance=8, vfx="LIGHTNING_SPEAR"),
        A("Ricochet Cuts", "A slash that zigzags between foes at impossible speed.", "chain_hit", "minecraft:iron_sword", damage=5, jumps=4, jump_range=6, vfx="WIND_SLASH"),
        A("Silver Spin", "Spin like a top, cutting everything around you many times.", "whirlwind", "minecraft:iron_axe", damage=3, hits=5, radius=3.5, interval=3, vfx="WIND_RING"),
        A("Time Dilation Rush", "The world slows around you: Speed and Haste for a while.", "buff_self", "minecraft:clock", effect="speed", effect2="haste", seconds=15, amp=1, vfx="SPIRIT_AURA"),
        A("Silver Rising", "A rising strike so fast the foe leaves the ground before it flinches.", "uppercut", "minecraft:wind_charge", damage=7, launch=0.9, vfx="WIND_SLASH"),
        A("Afterimage Volley", "Your afterimages throw a volley of blades at the foe you look at.", "barrage", "minecraft:iron_nugget", damage=2.5, shots=7, range=14, interval=2, vfx="WEAPON_CONSTRUCTS"),
    ],
    passives=[
        P("Gathering Speed", "Blows in a row build momentum that makes you hit harder.", "momentum", "minecraft:gunpowder", max_stacks=6, per_stack=0.05),
        P("Run-Up Impact", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:sugar", bonus=0.3),
        P("Always in Motion", "While sprinting you move even faster.", "self_effect_when", "minecraft:feather", effect="speed", condition="sprinting"),
        P("Second Wind of the Runner", "While sprinting your wounds mend.", "regen_when", "minecraft:golden_carrot", condition="sprinting", per_second=0.8),
        P("Wasn't There", "You may avoid a blow completely.", "dodge", "minecraft:phantom_membrane", chance=0.12),
        P("Flurry Rhythm", "Every few blows in a row hit much harder.", "combo", "minecraft:flint", hits=3, bonus=0.5),
        P("Speed Feeds Speed", "Each kill heals you and gives you Speed.", "kill_reward", "minecraft:sugar", heal=3, effect="speed", seconds=6, amp=1),
    ],
    finishers=[
        A("Quicksilver Tempest", "Run through the whole enemy line, a streak of cuts from end to end.", "dash_strike", "minecraft:lightning_rod", distance=10, damage=20, targets=6, cooldown=1300, vfx="LIGHTNING_FIEND"),
        A("Thousand-Step Dance", "A storm of cuts around you that outruns the eye.", "whirlwind", "minecraft:diamond_sword", damage=6, hits=7, radius=5, interval=3, cooldown=1400, vfx="WIND_TORNADO"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 12
LINES.append(Line(
    id="kamish_wrath", name="Kamish's Wrath", franchise=F,
    desc="The fury of a slain dragon bound in a dagger: diving fire, black flame beams and scales that laugh at heat and lightning.",
    color="#ff5a1f", background="magma", icon="minecraft:blaze_rod",
    book_name="Scale and Ember", book_lore="It died once. Its anger did not get the memo.",
    stances=[
        S("Dragon's Hunger", "Fire in the belly: far more damage, little care for your own skin.", "minecraft:fire_charge", damage_pct=0.3, armor=-3, vfx="FLAME_EXPLOSION"),
        S("Scaled Hide", "Scales rise across your body: tough, steady and healing, with softer blows.", "minecraft:turtle_helmet", armor=5, knockback_res=0.3, regen=0.5, damage_pct=-0.1, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Draconic Might", "The dragon's strength flows in the blade.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Drake Vitality", "A dragon's lifespan: more maximum health.", "max_health", 3, "minecraft:enchanted_golden_apple"),
        T("Scale Plating", "Overlapping scales harden your skin.", "armor", 1.5, "minecraft:netherite_chestplate"),
        T("Wyrm Reach", "Long arms and a long dagger.", "entity_interaction_range", 0.4, "minecraft:blaze_rod"),
    ],
    arts=[
        A("Dragon Dive", "Leap into the sky and crash down in a ball of fire.", "leap_slam", "minecraft:fire_charge", damage=9, radius=3.5, height=1.1, vfx="FLAME_EXPLOSION"),
        A("Claw Rake", "Four claws of flame rake a cone in front of you.", "cone_strike", "minecraft:blaze_powder", damage=8, range=4, half_angle=50, vfx="FIRE_BURST"),
        A("Black Fire Beam", "A beam of black flame pierces a long line of enemies.", "ray_shot", "minecraft:magma_cream", damage=9, range=22, pierce=2, vfx="FIRE_SPIRAL"),
        A("Falling Ember Star", "Call a burning star down on the spot you look at.", "meteor_drop", "minecraft:lava_bucket", damage=14, radius=4.5, range=22, vfx="FLAME_EXPLOSION"),
        A("Scorched Earth Wake", "Drag the dagger and send a trail of flame along the ground.", "ground_wave", "minecraft:magma_block", damage=7, length=11, width=2, vfx="FLAME_TRAIL"),
        A("Draconic Blood", "Wounds close over in a burst of dragon vitality.", "heal_self", "minecraft:golden_apple", amount=9, regen_seconds=6, vfx="FIRE_BURST"),
        A("Dragon's Challenge", "Roar and every hostile nearby turns to face you.", "taunt", "minecraft:dragon_head", radius=9, targets=5, vfx="FLAME_EXPLOSION"),
    ],
    passives=[
        P("Dragonhide: Flame", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.4),
        P("Dragonhide: Thunder", "You take much less lightning damage.", "resist", "minecraft:lightning_rod", damage_kind="lightning", pct=0.35),
        P("Burning Fury", "While you are on fire your blows hit harder.", "dmg_when", "minecraft:blaze_powder", condition="on_fire", bonus=0.3),
        P("Scorching Scales", "A share of the damage you take is burned back into the attacker.", "thorns", "minecraft:magma_cream", pct=0.25),
        P("Dragon's Last Breath", "When nearly dead the dragon's spirit shields you.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Tyrant's Eye", "Your blows may strike critically.", "crit", "minecraft:ender_eye", chance=0.15, mult=1.8),
        P("Hardened by the Hoard", "In the thick of a fight you take less damage.", "reduce_when", "minecraft:gold_block", condition="in_combat", pct=0.12),
    ],
    finishers=[
        A("Kamish's Wrath: Black Fire Breath", "The dragon breathes through you: a huge black flame pierces everything in a long line.", "ray_shot", "minecraft:dragon_breath", damage=26, range=30, pierce=4, cooldown=1400, vfx="FIRE_SPIRAL"),
        A("Wyrm's Descent", "Soar and fall as a comet of dragonfire, shattering the ground and everything on it.", "leap_slam", "minecraft:nether_star", damage=28, radius=6, height=1.2, knockback=1.6, cooldown=1300, vfx="FLAME_EXPLOSION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 13
LINES.append(Line(
    id="frost_monarch", name="Monarch of Frost", franchise=F,
    desc="Command a blizzard that never melts: glacial roads, flash freezes and a mantle of snow that keeps the cold in your favour.",
    color="#8fd8ff", background="frost", icon="minecraft:blue_ice",
    book_name="Annals of the Endless Winter", book_lore="Cold is not the absence of heat. It is a decision.",
    stances=[
        S("Winter's Embrace", "Frost wraps you in an icy shell: armoured and healing, but slow.", "minecraft:packed_ice", armor=4, regen=0.6, speed_pct=-0.12, vfx="WATER_RING"),
        S("Blizzard Step", "Ride the wind of the storm: faster and sharper, with thinner armor.", "minecraft:snowball", speed_pct=0.12, damage_pct=0.15, armor=-1, vfx="WIND_TORNADO"),
    ],
    stats=[
        T("Frostbound Plate", "Layers of hardened ice shield your body.", "armor", 1.5, "minecraft:iron_chestplate"),
        T("Glacier Weight", "You are as hard to move as a glacier.", "knockback_resistance", 0.1, "minecraft:ice"),
        T("Heart of Winter", "More maximum health.", "max_health", 2, "minecraft:golden_apple"),
    ],
    arts=[
        A("Hoarfrost Nova", "A ring of killing frost bursts out of you.", "ring_burst", "minecraft:snow_block", damage=6, radius=4.5, knockback=0.7, vfx="WATER_RING"),
        A("Blizzard Breath", "Exhale a cone of snow that slows and blinds everything in it.", "debuff_cone", "minecraft:powder_snow_bucket", effect="slowness", effect2="blindness", seconds=7, seconds2=3, amp=1, vfx="WIND_TORNADO"),
        A("Ice Spear Volley", "Throw a lance of ice that pierces the line before you.", "ray_shot", "minecraft:blue_ice", damage=8, range=20, pierce=2, vfx="WATER_BURST"),
        A("Flash Freeze", "The air itself locks: creatures around you are frozen in place.", "stun_ring", "minecraft:packed_ice", radius=5, seconds=2.5, vfx="WATER_RING"),
        A("Glacier Road", "A road of cracking ice runs ahead of you, hammering what it crosses.", "ground_wave", "minecraft:ice", damage=7, length=12, width=3, knockback=0.6, vfx="STONE_SPIKES"),
        A("Ice Aegis", "A shell of ice forms around you as an absorption shield.", "shield", "minecraft:glass", absorb_hearts=6, seconds=8, vfx="MIRROR_PANE"),
        A("Hailstone Drop", "A hail of ice falls on the place you look at.", "meteor_drop", "minecraft:snowball", damage=12, radius=4, range=20, vfx="WATER_BURST"),
    ],
    passives=[
        P("Frostbound Flesh", "You take much less freezing damage.", "resist", "minecraft:powder_snow_bucket", damage_kind="freeze", pct=0.5),
        P("Snow Mantle", "Under rain or snow the weather soothes you and you take less damage.", "reduce_when", "minecraft:snow_block", condition="raining", pct=0.15),
        P("Killing Cold", "Enemies near you are constantly slowed by the cold.", "aura", "minecraft:blue_ice", effect="slowness", target="enemies", radius=7),
        P("Meltwater Mending", "In water your wounds close steadily.", "regen_when", "minecraft:water_bucket", condition="in_water", per_second=1.0),
        P("Frostbite Touch", "Your blows may slow the foe.", "on_hit_effect", "minecraft:ice", effect="slowness", seconds=5, chance=0.35),
        P("Rime Thorns", "Part of the damage you take returns to the attacker as frost.", "thorns", "minecraft:prismarine_shard", pct=0.2),
        P("Winter's Foe", "You deal more damage to burning creatures.", "dmg_vs", "minecraft:fire_charge", category="burning", bonus=0.3),
    ],
    finishers=[
        A("Absolute Zero", "For a heartbeat everything around you stops: a wide freeze of every creature in range.", "stun_ring", "minecraft:nether_star", radius=8, seconds=3, targets=12, cooldown=1500, vfx="WATER_RING"),
        A("Spear of the Frost Monarch", "A colossal spear of ice speeds along your sight line, piercing everything.", "ray_shot", "minecraft:blue_ice", damage=26, range=30, pierce=4, cooldown=1400, vfx="WATER_BURST"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 14
LINES.append(Line(
    id="beast_monarch", name="Monarch of Beasts", franchise=F,
    desc="Lead the pack as the apex of every wild thing: pounces, rending claws and a howl that lifts the whole party.",
    color="#c8863a", background="forest", icon="minecraft:leather",
    book_name="The Alpha's Almanac", book_lore="Every pack is a family. This one has fangs.",
    stances=[
        S("Feral Surge", "Drop to all fours: faster and deadlier, with less armor.", "minecraft:bone", speed_pct=0.15, damage_pct=0.2, armor=-2, vfx="WIND_RING"),
        S("Pack Alpha", "Stand tall at the front of the pack: steady, resilient and healing.", "minecraft:white_banner", armor=3, regen=0.8, knockback_res=0.3, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Alpha's Frame", "More maximum health.", "max_health", 3, "minecraft:cooked_beef"),
        T("Rending Claws", "Heavier paws.", "attack_damage", 1, "minecraft:flint"),
        T("Predator's Gait", "Fast, loping strides.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Sure-Footed", "Hop over low ledges without slowing down.", "step_height", 0.5, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Claw Pounce", "Spring at the foes and crash down on them.", "leap_slam", "minecraft:rabbit_foot", damage=8, radius=3.2, height=0.9, vfx="WIND_RING"),
        A("Pack's Call", "Throw back your head and howl: you and your allies gain Speed.", "buff_aura", "minecraft:goat_horn", effect="speed", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Maul and Devour", "Sink your teeth into a foe and heal from the bite.", "drain_bite", "minecraft:beef", damage=7, heal_pct=0.35, vfx="FIRE_BURST"),
        A("Rending Maul", "A frenzy of claws and teeth around you.", "whirlwind", "minecraft:bone", damage=3.5, hits=5, radius=3.5, vfx="WIND_RING"),
        A("Hurling Swipe", "A heavy paw swing that tosses the foe skyward.", "uppercut", "minecraft:leather", damage=7, launch=0.8, vfx="WIND_SLASH"),
        A("Claw Tear", "Four parallel slashes across a cone in front of you.", "cone_strike", "minecraft:flint", damage=8, range=3.8, half_angle=50, vfx="WIND_SLASH"),
        A("Alpha's Challenge", "A roar that makes every hostile nearby turn to you.", "taunt", "minecraft:bone_block", radius=8, targets=5, vfx="DEVIL_CIRCLE"),
    ],
    passives=[
        P("Moon Vitality", "At night your wounds heal faster.", "regen_when", "minecraft:clock", condition="night", per_second=0.9),
        P("Apex Predator", "You deal more damage to hostile monsters.", "dmg_vs", "minecraft:rotten_flesh", category="monster", bonus=0.2),
        P("Moonlit Might", "At night you are visibly stronger.", "self_effect_when", "minecraft:white_wool", effect="strength", condition="night"),
        P("Hamstring Strike", "Your blows may slow the foe.", "on_hit_effect", "minecraft:string", effect="slowness", seconds=5, chance=0.3),
        P("Pack Bond", "Your allies near you run faster.", "aura", "minecraft:bone", effect="speed", target="allies", radius=8),
        P("Fresh Kill", "Each kill heals you and speeds you up.", "kill_reward", "minecraft:beef", heal=4, effect="speed", seconds=6),
        P("Shoulder-Shove", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.5, chance=0.35),
    ],
    finishers=[
        A("Primal Frenzy", "A feral storm of claws around you, without pause.", "whirlwind", "minecraft:netherite_axe", damage=6, hits=7, radius=5, cooldown=1300, vfx="WIND_TORNADO"),
        A("Beast King's Charge", "Stampede through the enemy line, trampling everything.", "dash_strike", "minecraft:iron_horse_armor", distance=11, damage=22, targets=6, cooldown=1400, vfx="FLAME_TRAIL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 15
LINES.append(Line(
    id="plague_monarch", name="Monarch of Plagues", franchise=F,
    desc="Command swarms, rot and contagion: a creeping line of poison that spreads from foe to foe and feeds on the dying.",
    color="#98b62a", background="ruin", icon="minecraft:rotten_flesh",
    book_name="Almanac of Blight and Buzzing", book_lore="The first cough is the cheapest. The rest are compounding interest.",
    stances=[
        S("Cloak of Flies", "A cloud of insects shrouds you: tougher and healing, but you move slowly.", "minecraft:cobweb", armor=3, speed_pct=-0.06, regen=0.6, vfx="POISON_BREATH"),
        S("Pestilent Crescendo", "Let the contagion run wild: far more damage, no guard.", "minecraft:fermented_spider_eye", damage_pct=0.3, armor=-3, vfx="POISON_BREATH"),
    ],
    stats=[
        T("Carrier's Constitution", "The plague spares you: more maximum health.", "max_health", 2, "minecraft:golden_apple"),
        T("Swarm Reflex", "Insects scout ahead: slightly faster.", "movement_speed", 0.03, "minecraft:feather", op="mult"),
        T("Chitin Skin", "A hardened second skin.", "armor", 1, "minecraft:turtle_helmet"),
    ],
    arts=[
        A("Pestilent Breath", "Exhale a cone of sickly fog that poisons and starves everything it touches.", "debuff_cone", "minecraft:fermented_spider_eye", effect="poison", effect2="hunger", seconds=8, seconds2=8, amp=1, vfx="POISON_BREATH"),
        A("Contagion Leap", "A diseased lash that jumps from foe to foe.", "chain_hit", "minecraft:spider_eye", damage=5, jumps=4, jump_range=6, falloff=0.1, vfx="POISON_BREATH"),
        A("Cloud of Flies", "A swarm of biting flies blinds the creatures around you.", "smoke", "minecraft:black_dye", radius=5, seconds=6, vfx="POISON_BREATH"),
        A("Swarm Eruption", "The swarm bursts out of you in a ring and knocks everything back.", "ring_burst", "minecraft:honeycomb", damage=6, radius=4.5, knockback=0.9, vfx="POISON_BREATH"),
        A("Mark of Contagion", "Infect one creature; it takes extra damage while the plague works.", "mark", "minecraft:poisonous_potato", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Swarm Recycles Life", "Insects pull vitality out of the air and give it to you and your allies.", "heal_pulse", "minecraft:honey_bottle", amount=6, radius=7, vfx="SPIRIT_AURA"),
        A("Maggot's Feast", "Bite a foe and feed on the rot.", "drain_bite", "minecraft:rotten_flesh", damage=6, heal_pct=0.3, vfx="POISON_BREATH"),
    ],
    passives=[
        P("Blight Touch", "Your blows may wither the foe.", "on_hit_effect", "minecraft:wither_rose", effect="wither", seconds=4, chance=0.25),
        P("Miasma Field", "Enemies near you are constantly poisoned.", "aura", "minecraft:spider_eye", effect="poison", target="enemies", radius=6),
        P("Immune to Rot", "You take much less wither damage.", "resist", "minecraft:rotten_flesh", damage_kind="wither", pct=0.5),
        P("Self-Mending Chitin", "Your wounds close over time, even out of combat.", "mending", "minecraft:honeycomb", seconds=14),
        P("Feast on the Fallen", "Kills restore your health.", "kill_reward", "minecraft:bone_meal", heal=5, effect="none", seconds=0),
        P("Rot in the Wound", "A share of the damage you take returns to the attacker.", "thorns", "minecraft:pointed_dripstone", pct=0.2),
        P("Grave Vitality", "Underground, where the plague breeds, your wounds close fast.", "regen_when", "minecraft:deepslate", condition="underground", per_second=0.9),
    ],
    finishers=[
        A("The Great Swarm", "Everything within range is stripped by a rolling cloud of insects.", "ring_burst", "minecraft:beehive", damage=20, radius=7, targets=8, knockback=0.8, cooldown=1400, vfx="POISON_BREATH"),
        A("Blight of Ages", "A cone of terminal rot: a very long withering poison on everything in front of you.", "debuff_cone", "minecraft:wither_skeleton_skull", effect="wither", effect2="poison", seconds=14, seconds2=14, amp=1, range=8, cooldown=1300, vfx="POISON_BREATH"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 16
LINES.append(Line(
    id="iron_body_monarch", name="Monarch of the Iron Body", franchise=F,
    desc="Fight bare-skinned and unafraid: a body hardened into living steel that needs no armor, shrugs off falling anvils and answers with open-handed hammer blows.",
    color="#8d97a6", background="iron", icon="minecraft:anvil",
    book_name="Treatise on the Unarmored Mountain", book_lore="Armor is for people who plan to be hit. He plans to be hit and not care.",
    stances=[
        S("Iron Body Hardened", "Lock every muscle: enormous armor and knockback immunity, but lumbering.", "minecraft:iron_block", armor=6, knockback_res=0.5, speed_pct=-0.15, vfx="EARTH_SPIKES"),
        S("Hammer Fist Stance", "Drop the guard and swing: heavy blows from a body that still takes hits well.", "minecraft:mace", damage_pct=0.3, armor=-1, vfx="STONE_SPIKES"),
    ],
    stats=[
        T("Steel Sinews", "Muscle like cable: harder strikes.", "attack_damage", 1.5, "minecraft:iron_pickaxe"),
        T("Mountain Constitution", "A huge pool of vitality.", "max_health", 4, "minecraft:golden_apple"),
        T("Rooted Stance", "Barely any force can shift you.", "knockback_resistance", 0.15, "minecraft:obsidian"),
        T("Iron Skin", "Skin the toughness of plate.", "armor", 1.5, "minecraft:iron_chestplate"),
    ],
    arts=[
        A("Iron Fist Uppercut", "A fist like a falling anvil, upside down: it launches the foe into the sky.", "uppercut", "minecraft:iron_block", damage=9, launch=0.9, vfx="STONE_SPIKES"),
        A("Body of Iron", "Harden your skin: Strength and Resistance for a while.", "buff_self", "minecraft:iron_ingot", effect="strength", effect2="resistance", seconds=16, vfx="EARTH_SPIKES"),
        A("Colossus Drop", "Jump and land with your whole weight, cracking the ground around you.", "leap_slam", "minecraft:anvil", damage=9, radius=3.5, height=0.8, knockback=1.0, vfx="STONE_SPIKES"),
        A("Hammering Palm", "A clap-wide palm strike across a short, heavy cone.", "cone_strike", "minecraft:mace", damage=9, range=3.5, half_angle=45, knockback=0.9, vfx="WIND_SLASH"),
        A("Tempered Recovery", "Breathe, flex and close your wounds.", "heal_self", "minecraft:golden_carrot", amount=9, regen_seconds=7, vfx="SPIRIT_AURA"),
        A("Steel Quake", "Stamp so hard the ring of ground around you jumps.", "ring_burst", "minecraft:cobbled_deepslate", damage=7, radius=4.5, knockback=1.2, vfx="STONE_SPIKES"),
        A("Crushing Verdict", "A single fist that hits much harder the weaker the foe already is.", "execute", "minecraft:netherite_axe", damage=10, bonus=0.8, vfx="EARTH_SPIKES"),
    ],
    passives=[
        P("Needs No Armor", "Without armor on, you take much less damage.", "reduce_when", "minecraft:iron_chestplate", condition="unarmored", pct=0.2),
        P("Bare-Knuckle Edge", "Without armor on, you hit harder.", "dmg_when", "minecraft:iron_nugget", condition="unarmored", bonus=0.25),
        P("Falling Anvil", "You take much less fall damage.", "fall_guard", "minecraft:anvil", pct=0.6),
        P("Planted Feet", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.7, chance=0.4),
        P("Spiked Knuckles", "A share of the damage you take is returned to the attacker.", "thorns", "minecraft:iron_nugget", pct=0.2),
        P("Iron Recovery", "Your wounds close slowly, even out of combat.", "mending", "minecraft:iron_block", seconds=12, amount=1),
        P("Pinpoint Impact", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.12, mult=1.7),
    ],
    finishers=[
        A("Mountain Falls", "Leap high and land as a mountain: a punishing shockwave that shatters all footing.", "leap_slam", "minecraft:netherite_block", damage=28, radius=6.5, height=1.2, knockback=1.8, cooldown=1400, vfx="STONE_SPIKES"),
        A("Hammer of the Iron Monarch", "A single blow that sends a shockwave through the ground along your line of sight.", "ground_wave", "minecraft:mace", damage=24, length=16, width=4, knockback=1.6, cooldown=1300, vfx="EARTH_SPIKES"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 17
LINES.append(Line(
    id="white_flame_sovereign", name="Sovereign of White Flames", franchise=F,
    desc="A fire that burns hotter than red and cleans what it touches: needles of white flame, purging warmth for friends and a core that does not scorch you.",
    color="#fff1c9", background="holy", icon="minecraft:glowstone_dust",
    book_name="Canticle of the Pale Pyre", book_lore="Not all fire destroys. This one only decides.",
    stances=[
        S("Pale Blaze", "The flame burns bright and white: much stronger blows with a thin guard.", "minecraft:fire_charge", damage_pct=0.25, armor=-2, vfx="FIRE_SPIRAL"),
        S("Ember Veil", "A low hearth-glow wraps you: warmth that heals, steadier armor, softer blows.", "minecraft:campfire", regen=0.8, armor=2, damage_pct=-0.05, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Core of White Fire", "More maximum health from the inner flame.", "max_health", 2, "minecraft:glowstone"),
        T("Incandescent Edge", "Heat on every blow.", "attack_damage", 1, "minecraft:blaze_rod"),
        T("Ashen Footfall", "Flame lightens your step.", "movement_speed", 0.03, "minecraft:golden_boots", op="mult"),
        T("Radiant Mantle", "A halo of heat toughens you.", "armor_toughness", 1, "minecraft:gold_ingot"),
    ],
    arts=[
        A("Flame Needle Rain", "Hundreds of white needles streak into the foe you look at.", "barrage", "minecraft:blaze_powder", damage=2.5, shots=7, range=14, vfx="FIRE_BURST"),
        A("Cinder Rush", "Leave a white trail as you tear through the line ahead.", "dash_strike", "minecraft:fire_charge", distance=7, damage=7, vfx="FLAME_TRAIL"),
        A("Pillar Dance", "Columns of white flame wheel around you.", "whirlwind", "minecraft:glowstone", damage=3.5, hits=5, radius=4, vfx="FIRE_SPIRAL"),
        A("White Nova", "A sudden white burst that knocks everyone back.", "ring_burst", "minecraft:glowstone_dust", damage=6, radius=4.5, knockback=0.9, vfx="FLAME_EXPLOSION"),
        A("Purging Warmth", "Pure heat mends you and the allies near you.", "heal_pulse", "minecraft:golden_apple", amount=6, radius=7, vfx="SPIRIT_AURA"),
        A("Arc Flame", "A bolt of white fire that leaps from foe to foe.", "chain_hit", "minecraft:blaze_rod", damage=5, jumps=4, vfx="FIRE_SPIRAL"),
        A("Searing Lance", "A thin, white-hot lance that pierces a line of foes.", "ray_shot", "minecraft:end_rod", damage=9, range=22, pierce=3, vfx="FIRE_SPIRAL"),
    ],
    passives=[
        P("Burning Heart", "While you are on fire your blows hit harder.", "dmg_when", "minecraft:blaze_powder", condition="on_fire", bonus=0.3),
        P("Born of the Flame", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.5),
        P("Fireproof Spirit", "While you are burning you are immune to the pain: Fire Resistance.", "self_effect_when", "minecraft:magma_cream", effect="fire_resistance", condition="on_fire"),
        P("Cleansing Ash", "Your blows heal you.", "lifesteal", "minecraft:honey_bottle", pct=0.06),
        P("Revealing Light", "Your blows may make the foe glow.", "on_hit_effect", "minecraft:glowstone_dust", effect="glowing", seconds=8, chance=0.35),
        P("Hearth of the Flame", "Allies near you slowly regenerate.", "aura", "minecraft:campfire", effect="regeneration", target="allies", radius=7),
        P("Lantern-Bearer's Luck", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:lantern", chance=0.15),
    ],
    finishers=[
        A("White Sun Descends", "A sun of white fire falls on the place you look at.", "meteor_drop", "minecraft:nether_star", damage=30, radius=7, range=26, cooldown=1500, vfx="FLAME_EXPLOSION"),
        A("Pyres of the Sovereign", "A tall wave of white flame sweeps the ground in front of you.", "ground_wave", "minecraft:blaze_rod", damage=22, length=16, width=5, cooldown=1300, vfx="FLAME_TRAIL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 18
LINES.append(Line(
    id="hunter_healer", name="Hunter-Class Healer", franchise=F,
    desc="The raid's lifeline: light that mends the whole party, barriers that hold the line and a dazzling flash that buys everyone a moment to breathe.",
    color="#7ae3a3", background="holy", icon="minecraft:glistering_melon_slice",
    book_name="Field Guide for Raid Healers", book_lore="You are not here to fight. You are here so they can.",
    stances=[
        S("Aegis Stance", "You hold the back line, hard to hurt and healing constantly, but you barely hit back.", "minecraft:shield", armor=4, regen=1.2, damage_pct=-0.2, speed_pct=-0.05, vfx="SPIRIT_AURA"),
        S("Frontline Medic", "Run to whoever is falling: quicker and tougher at blows, with no passive healing.", "minecraft:golden_boots", speed_pct=0.1, damage_pct=0.1, vfx="WIND_RING"),
    ],
    stats=[
        T("Reservoir of Mana", "A deeper life pool to share.", "max_health", 3, "minecraft:golden_apple"),
        T("Calm Hands", "Steadier hands, more luck.", "luck", 1, "minecraft:rabbit_foot"),
        T("Medic's Dash", "Reaching the wounded faster.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Sanctified Garb", "Blessed cloth toughens you.", "armor_toughness", 1, "minecraft:white_wool"),
    ],
    arts=[
        A("Radiant Mending", "A pulse of light heals you and every ally around you.", "heal_pulse", "minecraft:glistering_melon_slice", amount=8, radius=8, vfx="SPIRIT_AURA"),
        A("Barrier of Light", "A bright wall of absorption wraps you.", "shield", "minecraft:glass", absorb_hearts=6, seconds=8, vfx="MIRROR_PANE"),
        A("Blessing of the Raid", "You and your allies gain regeneration for a long while.", "buff_aura", "minecraft:honey_bottle", effect="regeneration", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Mana-Rich Recovery", "A deep draught of mana closes your own wounds.", "heal_self", "minecraft:experience_bottle", amount=10, regen_seconds=8, vfx="SPIRIT_AURA"),
        A("Holy Burst", "A ring of holy light throws foes away from you.", "ring_burst", "minecraft:glowstone_dust", damage=5, radius=4, knockback=1.2, vfx="MAGIC_CIRCLE"),
        A("Dazzling Flash", "A blinding flash locks nearby creatures in place.", "stun_ring", "minecraft:end_rod", radius=5, seconds=2, vfx="MAGIC_CIRCLE"),
        A("Smiting Ray", "A thin ray of judgment down your line of sight.", "ray_shot", "minecraft:glowstone", damage=7, range=18, pierce=2, vfx="LIGHTNING_SPEAR"),
    ],
    passives=[
        P("Soothing Presence", "While in a fight your wounds slowly close.", "regen_when", "minecraft:glistering_melon_slice", condition="in_combat", per_second=0.8),
        P("Healing Field", "Allies near you slowly regenerate.", "aura", "minecraft:honey_block", effect="regeneration", target="allies", radius=8),
        P("Anathema", "You deal more damage to the undead.", "dmg_vs", "minecraft:bone", category="undead", bonus=0.3),
        P("Gentle Recovery", "Your wounds close over time, even out of combat.", "mending", "minecraft:honeycomb", seconds=12, amount=1),
        P("Prayer in Peril", "At low health you take much less damage.", "reduce_when", "minecraft:totem_of_undying", condition="low_health", pct=0.25),
        P("Student of the Raid", "Experience comes easier when you support.", "xp_bonus", "minecraft:book", pct=0.2),
        P("Economy of Mana", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
    ],
    finishers=[
        A("Hand of Resurrection", "A massive wave of healing floods you and every ally around you.", "heal_pulse", "minecraft:totem_of_undying", amount=20, radius=12, cooldown=1500, vfx="SPIRIT_AURA"),
        A("Sanctuary Dome", "A great dome of light: a huge absorption shield with heavy resistance.", "shield", "minecraft:beacon", absorb_hearts=12, seconds=14, resist_amp=1, cooldown=1400, vfx="MIRROR_PANE"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 19
LINES.append(Line(
    id="hunter_mage", name="Hunter-Class Mage", franchise=F,
    desc="A glass cannon with a library in her head: arcane lances, chain lightning, short-range blinks and a meteor on a long cooldown.",
    color="#4a7dff", background="tower", icon="minecraft:enchanted_book",
    book_name="Spellbook of the Gate Mage", book_lore="Three words, four seconds, and the room changes.",
    stances=[
        S("Casting Posture", "Stand and deliver: much stronger spells, with no defence and no hurry.", "minecraft:enchanting_table", damage_pct=0.25, armor=-2, speed_pct=-0.08, vfx="MAGIC_CIRCLE"),
        S("Mana Shell", "Wrap your mana around you: sturdy and steadily healing, with weaker spells.", "minecraft:lapis_lazuli", armor=3, regen=0.8, damage_pct=-0.1, vfx="MIRROR_PANE"),
    ],
    stats=[
        T("Spell Power", "Raw output of the caster.", "attack_damage", 1.5, "minecraft:blaze_rod"),
        T("Mana Pool Depth", "More maximum health to burn.", "max_health", 2, "minecraft:lapis_block"),
        T("Casting Rhythm", "Faster combat tempo.", "attack_speed", 0.06, "minecraft:clock", op="mult"),
    ],
    arts=[
        A("Arcane Lance", "A single piercing bolt of pure mana down your sight line.", "ray_shot", "minecraft:amethyst_shard", damage=9, range=22, pierce=2, vfx="LIGHTNING_SPEAR"),
        A("Chain Lightning", "A bolt that hops from foe to foe.", "chain_hit", "minecraft:lightning_rod", damage=6, jumps=4, vfx="LIGHTNING_FIEND"),
        A("Frost Nova", "A burst of frost around you knocks creatures back.", "ring_burst", "minecraft:snow_block", damage=6, radius=4.5, knockback=1.0, vfx="WATER_RING"),
        A("Magic Missile Salvo", "A salvo of small bolts at the foe you look at.", "barrage", "minecraft:spectral_arrow", damage=2.5, shots=7, range=16, vfx="MAGIC_CIRCLE"),
        A("Blink", "A quick hop through space.", "blink", "minecraft:ender_pearl", distance=9, vfx="SPATIAL_RIFT"),
        A("Meteor Strike", "Pull a small meteor down onto the spot you look at.", "meteor_drop", "minecraft:fire_charge", damage=14, radius=4, range=22, vfx="FLAME_EXPLOSION"),
        A("Fire Wave", "A cone of fire sweeping the ground in front.", "cone_strike", "minecraft:blaze_powder", damage=8, range=4.5, half_angle=55, vfx="FIRE_BURST"),
    ],
    passives=[
        P("Mana Ward", "You take much less magic damage.", "resist", "minecraft:lapis_lazuli", damage_kind="magic", pct=0.35),
        P("Spell Crit", "Your spells may strike critically.", "crit", "minecraft:ender_eye", chance=0.15, mult=1.8),
        P("Mana Thrift", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Meditation", "Crouch and your wounds mend.", "regen_when", "minecraft:book", condition="sneaking", per_second=1.0),
        P("Finishing Spell", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:blaze_rod", threshold=0.25, bonus=0.7),
        P("Study of the Gates", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Barrier Flicker", "A thin barrier may turn aside a blow completely.", "dodge", "minecraft:glass", chance=0.1),
    ],
    finishers=[
        A("Cataclysm", "A vast meteor smashes down on the place you look at.", "meteor_drop", "minecraft:nether_star", damage=32, radius=7, range=28, cooldown=1500, vfx="FLAME_EXPLOSION"),
        A("Grand Arcane Beam", "A great beam of mana pierces everything along your sight line.", "ray_shot", "minecraft:end_crystal", damage=26, range=32, pierce=4, cooldown=1300, vfx="LIGHTNING_SPEAR"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 20
LINES.append(Line(
    id="goliath_colossal_fist", name="Goliath, the Colossal Fist", franchise=F,
    desc="The strongest Fighter on the continent: wading in, throwing boulders, stamping the ground flat and hitting like a falling building. Inspired by the nation-level Fighter class of the story.",
    color="#d98c1f", background="city", icon="minecraft:mace",
    book_name="Biography of a Walking Earthquake", book_lore="Subtlety is for people who cannot lift the building.",
    stances=[
        S("Wrecking Posture", "Squared up and swinging: huge damage, moderate speed loss.", "minecraft:netherite_axe", damage_pct=0.3, speed_pct=-0.08, vfx="STONE_SPIKES"),
        S("Titan's Guard", "Hands up, feet planted: sturdy and hard to knock back, with lighter blows.", "minecraft:iron_chestplate", armor=4, knockback_res=0.6, damage_pct=-0.1, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Colossal Strength", "Fists like sledges.", "attack_damage", 2, "minecraft:netherite_pickaxe"),
        T("Bulk of a Giant", "Immense vitality.", "max_health", 4, "minecraft:cooked_beef"),
        T("Heavy Footing", "Hard to push around.", "knockback_resistance", 0.12, "minecraft:anvil"),
        T("Wrecker's Reach", "Long arms of a giant.", "entity_interaction_range", 0.5, "minecraft:lead"),
    ],
    arts=[
        A("Colossus Footfall", "Stomp and send a crack of force down the ground ahead.", "ground_wave", "minecraft:cobblestone", damage=8, length=12, width=3, knockback=1.2, vfx="STONE_SPIKES"),
        A("Titan's Pounce", "Spring with surprising speed and land on the enemy cluster.", "leap_slam", "minecraft:anvil", damage=10, radius=3.5, height=1.0, knockback=1.2, vfx="STONE_SPIKES"),
        A("Haymaker", "A single huge swing that hits much harder against a wounded foe.", "execute", "minecraft:mace", damage=10, bonus=0.9, vfx="WIND_SLASH"),
        A("Seismic Clap", "A thunder-clap of hands that freezes the nearby creatures in place.", "stun_ring", "minecraft:bell", radius=4.5, seconds=2, vfx="WIND_RING"),
        A("Overcharge Muscle", "Flex until the shirt gives: Strength and Haste for a while.", "buff_self", "minecraft:golden_apple", effect="strength", effect2="haste", seconds=16, amp=1, vfx="FLAME_EXPLOSION"),
        A("Boulder Barrage", "Rip up stone and throw it, again and again, at the foe you look at.", "barrage", "minecraft:cobblestone", damage=3, shots=5, range=14, vfx="STONE_SPIKES"),
        A("Come At Me", "A bellow that draws every hostile nearby to you.", "taunt", "minecraft:goat_horn", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
    ],
    passives=[
        P("Boss-Breaker", "You deal more damage to bosses.", "dmg_vs", "minecraft:dragon_head", category="boss", bonus=0.25),
        P("Shoulder-Check", "Your blows may shove the foe back.", "knockback_on_hit", "minecraft:piston", strength=0.7, chance=0.4),
        P("Massive Frame", "At full health you take much less damage.", "reduce_when", "minecraft:iron_block", condition="full_health", pct=0.15),
        P("Bruiser's Crit", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.12, mult=1.8),
        P("Blood of the Strong", "Your blows heal you.", "lifesteal", "minecraft:cooked_beef", pct=0.07),
        P("Not Done Yet", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=6),
        P("Spoils of a Giant", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.15),
    ],
    finishers=[
        A("Ground Zero", "Slam the earth and flatten everything near you.", "ring_burst", "minecraft:tnt", damage=24, radius=7, targets=9, knockback=2.0, cooldown=1400, vfx="STONE_SPIKES"),
        A("Breaker of Walls", "Run through the line, one fist to every face along the road.", "dash_strike", "minecraft:netherite_block", distance=11, damage=24, targets=6, cooldown=1300, vfx="FLAME_TRAIL"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 21
LINES.append(Line(
    id="hunter_ranger", name="Hunter-Class Ranger", franchise=F,
    desc="Death at a distance: pinning shots, rolling backsteps and a rain of arrows for the hunter who never lets the boss close the gap.",
    color="#3fae6a", background="forest", icon="minecraft:bow",
    book_name="Ranger's Guide to Boss Rooms", book_lore="The best place to stand is wherever it is not.",
    stances=[
        S("Hawk's Perch", "Hold still and aim: sharper crits, a little extra damage, slower on your feet.", "minecraft:spyglass", crit_chance=0.2, damage_pct=0.1, speed_pct=-0.1, vfx="WIND_RING"),
        S("Skirmisher's Run", "Keep moving: much faster, with less armor and lighter blows.", "minecraft:leather_boots", speed_pct=0.18, armor=-1, damage_pct=-0.05, vfx="WIND_SLASH"),
    ],
    stats=[
        T("Steady Aim", "Better, more patient hands.", "attack_damage", 1, "minecraft:arrow"),
        T("Light Footing", "Faster feet.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Long Sight", "You reach further with every action.", "entity_interaction_range", 0.6, "minecraft:spyglass"),
        T("Landing Roll", "Safe falls from tall perches.", "safe_fall_distance", 2, "minecraft:feather"),
    ],
    arts=[
        A("Piercing Shot", "A single arrow that punches through a whole line of enemies.", "ray_shot", "minecraft:spectral_arrow", damage=9, range=26, pierce=3, vfx="LIGHTNING_SPEAR"),
        A("Arrow Rain", "Loose a fast volley at the foe you look at.", "barrage", "minecraft:arrow", damage=2.5, shots=8, range=18, vfx="WEAPON_CONSTRUCTS"),
        A("Hunter's Mark", "Mark a target; it takes extra damage from everything you do.", "mark", "minecraft:name_tag", bonus=0.35, range=26, vfx="MAGIC_CIRCLE"),
        A("Backstep Roll", "Roll away from the front line.", "blink", "minecraft:leather_boots", distance=7, vfx="WIND_RING"),
        A("Ricochet Arrow", "An arrow that bounces from target to target.", "chain_hit", "minecraft:tipped_arrow", damage=5, jumps=4, jump_range=6, vfx="WIND_SLASH"),
        A("Net Shot", "A weighted net in a cone slows and tangles everything it hits.", "debuff_cone", "minecraft:string", effect="slowness", effect2="weakness", seconds=7, seconds2=4, amp=1, vfx="THREAD_LINE"),
        A("Skyfall Volley", "A cloud of arrows falls on the target spot.", "meteor_drop", "minecraft:arrow", damage=12, radius=4, range=26, vfx="WEAPON_CONSTRUCTS"),
    ],
    passives=[
        P("Eagle Eye", "Your shots may strike critically.", "crit", "minecraft:spyglass", chance=0.18, mult=1.8),
        P("Scavenger's Eye", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.18),
        P("Mercy Shot", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:spectral_arrow", threshold=0.25, bonus=0.7),
        P("Bug Hunter", "You deal more damage to spiders and insects.", "dmg_vs", "minecraft:string", category="arthropod", bonus=0.3),
        P("Treetop Landing", "You take much less fall damage.", "fall_guard", "minecraft:oak_sapling", pct=0.5),
        P("Daylight Marksman", "In daylight your aim is better and you hit harder.", "dmg_when", "minecraft:sunflower", condition="day", bonus=0.2),
        P("Calm Breath", "Crouched in a perch, your wounds close.", "regen_when", "minecraft:glow_berries", condition="sneaking", per_second=0.8),
    ],
    finishers=[
        A("Hundred Arrow Storm", "An unbroken wall of arrows at the target.", "barrage", "minecraft:crossbow", damage=4, shots=9, range=24, cooldown=1300, vfx="WEAPON_CONSTRUCTS"),
        A("Dragon-Piercing Shot", "One enormous arrow that pierces everything on the line.", "ray_shot", "minecraft:trident", damage=28, range=36, pierce=4, cooldown=1400, vfx="LIGHTNING_SPEAR"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 22
LINES.append(Line(
    id="system_daily_quest", name="The System: Daily Quest", franchise=F,
    desc="Obey the System's blue windows: boosts for each quest accepted, level-up pulses, analysis of every enemy and the grim bargain of the Penalty Zone.",
    color="#38c8ff", background="stellar", icon="minecraft:writable_book",
    book_name="The Player's Handbook (System Edition)", book_lore="[Quest Accepted.] You were not asked.",
    stances=[
        S("Daily Quest Mode", "Chase the quest bar: faster and sharper, but with no time for guarding.", "minecraft:clock", speed_pct=0.08, damage_pct=0.12, armor=-1, vfx="MAGIC_CIRCLE"),
        S("Penalty Zone Survival", "Endure the Penalty Zone: heavy armor, slow regeneration, little speed.", "minecraft:iron_chestplate", armor=5, regen=0.5, knockback_res=0.3, speed_pct=-0.1, vfx="SEAL_CHAINS"),
    ],
    stats=[
        T("Stat Point: Vitality", "Spend a point on health.", "max_health", 2, "minecraft:golden_apple"),
        T("Stat Point: Strength", "Spend a point on power.", "attack_damage", 1, "minecraft:iron_sword"),
        T("Stat Point: Agility", "Spend a point on speed.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
        T("Stat Point: Sense", "Spend a point on perception and luck.", "luck", 1, "minecraft:ender_eye"),
    ],
    arts=[
        A("System Boost", "[Buff Applied.] Strength and Speed for a while.", "buff_self", "minecraft:experience_bottle", effect="strength", effect2="speed", seconds=14, vfx="MAGIC_CIRCLE"),
        A("Status Window: Analyze", "Read the target's stat window; it takes extra damage from you for a while.", "mark", "minecraft:map", bonus=0.3, range=22, vfx="MIRROR_PANE"),
        A("Sprint Dash", "Hit the sprint key of reality and cut through the line ahead.", "dash_strike", "minecraft:sugar", distance=7, damage=7, vfx="WIND_SLASH"),
        A("Level-Up Recovery", "[Level Up!] Your wounds close in a rush of new stat points.", "heal_self", "minecraft:golden_carrot", amount=10, regen_seconds=7, vfx="SPIRIT_AURA"),
        A("Level-Up Pulse", "A ring of blue light erupts with every point you earn.", "ring_burst", "minecraft:lapis_lazuli", damage=6, radius=4.5, knockback=0.8, vfx="MAGIC_CIRCLE"),
        A("System Barrier", "[Shield Generated.] A barrier of blue light absorbs the damage.", "shield", "minecraft:glass", absorb_hearts=5, seconds=8, vfx="MIRROR_PANE"),
        A("Raid Quest Accepted", "[Party Buff.] You and your allies gain Haste.", "buff_aura", "minecraft:blue_banner", effect="haste", radius=10, seconds=14, vfx="MAGIC_CIRCLE"),
    ],
    passives=[
        P("Double Experience", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.25),
        P("Quest Reward", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.15),
        P("[Level Up!]", "Each kill heals you and gives you Haste.", "kill_reward", "minecraft:emerald", heal=3, effect="haste", seconds=6),
        P("Auto-Recovery", "Your wounds close over time, even out of combat.", "mending", "minecraft:golden_apple", seconds=14),
        P("Combo Counter", "Blows in a row grow stronger.", "momentum", "minecraft:clock", max_stacks=6),
        P("Stamina Management", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
        P("Penalty Avoided", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=5),
    ],
    finishers=[
        A("Job Change Awakening", "[Job Change Complete.] A long surge of Strength and Resistance.", "buff_self", "minecraft:nether_star", effect="strength", effect2="resistance", seconds=30, amp=1, cooldown=1800, vfx="MAGIC_CIRCLE"),
        A("Daily Quest Complete", "[Reward Granted.] A burst of healing for you and your allies.", "heal_pulse", "minecraft:enchanted_golden_apple", amount=20, radius=10, cooldown=1500, vfx="SPIRIT_AURA"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 23
LINES.append(Line(
    id="hunter_sword_dance", name="Hunter Sword Dance", franchise=F,
    desc="A swordswoman's flowing style: crescents of steel, water-soft steps and a still-water cut that arrives before the foe knows. Inspired by the S-rank sword hunters of the story.",
    color="#5ac8e6", background="ocean", icon="minecraft:diamond_sword",
    book_name="The Still-Water Sword Manual", book_lore="The blade is just the last inch of the dance.",
    stances=[
        S("Flowing Stance", "Move like a stream: quick and likely to crit, but light on armor.", "minecraft:water_bucket", speed_pct=0.12, crit_chance=0.1, armor=-1, vfx="WATER_RING"),
        S("Still Water", "Calm like a pond: armored and slowly healing, with gentler blows.", "minecraft:heart_of_the_sea", armor=3, regen=0.6, damage_pct=-0.05, vfx="WATER_BURST"),
    ],
    stats=[
        T("Fine Edge", "A keen blade hand.", "attack_damage", 1.5, "minecraft:diamond_sword"),
        T("Wrist Speed", "Quick, accurate strokes.", "attack_speed", 0.07, "minecraft:clock", op="mult"),
        T("Light Footwork", "Nimble feet.", "movement_speed", 0.03, "minecraft:leather_boots", op="mult"),
    ],
    arts=[
        A("Dance of Blades", "Spin through a ring of overlapping cuts.", "whirlwind", "minecraft:diamond_sword", damage=3.5, hits=5, radius=3.5, vfx="WATER_RING"),
        A("Crescent Flow", "A broad flowing crescent across the foes in front.", "cone_strike", "minecraft:iron_sword", damage=8, range=4, half_angle=55, vfx="WATER_BURST"),
        A("Water Step", "Slide a few blocks forward as if on a stream.", "blink", "minecraft:heart_of_the_sea", distance=8, vfx="WATER_RING"),
        A("Still Water Cut", "A cut so calm it lands much harder against a wounded foe.", "execute", "minecraft:golden_sword", damage=9, bonus=1.0, vfx="WIND_SLASH"),
        A("Tide Slash", "A long wave of steel runs along the ground.", "ground_wave", "minecraft:prismarine_shard", damage=7, length=12, width=2, vfx="WATER_BURST"),
        A("Calm Mind", "Clear your mind: Haste and Regeneration for a while.", "buff_self", "minecraft:lily_pad", effect="haste", effect2="regeneration", seconds=14, vfx="SPIRIT_AURA"),
        A("Ripple Strikes", "A cut that jumps from foe to foe like ripples.", "chain_hit", "minecraft:prismarine_crystals", damage=5, jumps=4, vfx="WATER_RING"),
    ],
    passives=[
        P("Flowing Combo", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_sword", hits=3, bonus=0.5),
        P("Pressure Point", "Your blows may strike critically.", "crit", "minecraft:flint", chance=0.15, mult=1.8),
        P("Willow Reed", "You may sway aside from a blow completely.", "dodge", "minecraft:lily_pad", chance=0.12),
        P("Swordswoman's Poise", "You hit harder while wielding a sword.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword", bonus=0.2),
        P("Sword Focus", "While you wield a sword you feel quicker: Haste.", "self_effect_when", "minecraft:golden_sword", effect="haste", condition="holding_sword"),
        P("Blade Breath", "With a sword in hand your wounds slowly close.", "regen_when", "minecraft:iron_sword", condition="holding_sword", per_second=0.6),
        P("Economy of Motion", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.25),
    ],
    finishers=[
        A("Tempest of a Hundred Blades", "A raging storm of cuts spins up around you.", "whirlwind", "minecraft:netherite_sword", damage=6, hits=7, radius=5, interval=3, cooldown=1400, vfx="WIND_TORNADO"),
        A("Great Tide Cut", "A single immense wave of steel along the ground.", "ground_wave", "minecraft:trident", damage=24, length=18, width=4, knockback=1.0, cooldown=1300, vfx="WATER_BURST"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 24
LINES.append(Line(
    id="double_dungeon_survivor", name="Double Dungeon Survivor", franchise=F,
    desc="Live through the room nobody was supposed to walk out of: smoke and rubble, spite-reflecting wards and the strength that only shows up when you are down to your last heart.",
    color="#a89a7a", background="sand", icon="minecraft:totem_of_undying",
    book_name="Account of the Room That Doesn't Let You Out", book_lore="Seven survivors. Four did not need to be told twice.",
    stances=[
        S("Cornered Rat", "Back to the wall, teeth out: stronger blows and no guard at all.", "minecraft:rotten_flesh", damage_pct=0.2, armor=-2, vfx="DEVIL_CIRCLE"),
        S("Prayer to the Statue", "Kneel and endure: heavy armor and steady healing, but you can barely move.", "minecraft:stone_bricks", armor=4, regen=1.0, speed_pct=-0.12, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("Stubborn Heart", "More maximum health.", "max_health", 3, "minecraft:golden_apple"),
        T("Rubble Plating", "Dust and dried blood harden the hide.", "armor", 1, "minecraft:leather_chestplate"),
        T("Braced for the Hit", "Hard to knock off your feet.", "knockback_resistance", 0.1, "minecraft:cobblestone"),
        T("Survivor's Luck", "Small mercies.", "luck", 1, "minecraft:rabbit_foot"),
    ],
    arts=[
        A("Dust of the Ruin", "Kick up the dust of the collapsed hall and blind the creatures around you.", "smoke", "minecraft:gravel", radius=5, seconds=6, vfx="ASH_FORMATION"),
        A("Cling to Life", "Grit your teeth and drag yourself back from the edge.", "heal_self", "minecraft:glistering_melon_slice", amount=9, regen_seconds=8, vfx="SPIRIT_AURA"),
        A("Spite Reflection", "For a short while, a large share of damage you take is thrown back at the attacker.", "ward_reflect", "minecraft:crying_obsidian", reflect_pct=0.4, vfx="DEVIL_CIRCLE"),
        A("Desperate Lunge", "A desperate charge through whatever stands in front of you.", "dash_strike", "minecraft:rabbit_foot", distance=6, damage=8, vfx="FLAME_TRAIL"),
        A("Thrown Rubble", "Heave chunks of the ceiling at the foe you look at.", "barrage", "minecraft:cobblestone", damage=2.5, shots=5, range=12, vfx="STONE_SPIKES"),
        A("Find the Weak Point", "Spot where the thing is cracked; it takes extra damage for a while.", "mark", "minecraft:spyglass", bonus=0.3, vfx="MAGIC_CIRCLE"),
        A("Cry of the Survivor", "Scream, and everything nearby stops for a breath.", "stun_ring", "minecraft:goat_horn", radius=4.5, seconds=2, vfx="WIND_RING"),
    ],
    passives=[
        P("Not Dead Yet", "When nearly dead you are shielded for a while.", "last_stand", "minecraft:totem_of_undying", seconds=7),
        P("Pain Is a Teacher", "At low health you take much less damage.", "reduce_when", "minecraft:iron_ingot", condition="low_health", pct=0.25),
        P("Last Heart's Fury", "At low health your blows land much harder.", "dmg_when", "minecraft:redstone", condition="low_health", bonus=0.4),
        P("Second Wind", "At low health your wounds close faster.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.2),
        P("Slip the Strike", "You may avoid a blow completely.", "dodge", "minecraft:feather", chance=0.12),
        P("Roll with the Fall", "You take much less fall damage.", "fall_guard", "minecraft:hay_block", pct=0.5),
        P("Rationed Strength", "You tire more slowly.", "hunger_saver", "minecraft:bread", pct=0.3),
    ],
    finishers=[
        A("Refuse to Die", "A huge, roaring recovery that pulls you back to life.", "heal_self", "minecraft:enchanted_golden_apple", amount=25, regen_seconds=12, cooldown=1600, vfx="SPIRIT_AURA"),
        A("Last Roar of the Weakest", "Everything you have left, thrown outward in one ring.", "ring_burst", "minecraft:dragon_head", damage=22, radius=6, targets=8, knockback=1.8, cooldown=1400, vfx="FLAME_EXPLOSION"),
    ],
))

# ---------------------------------------------------------------------------------------------------------------------------------- 25
LINES.append(Line(
    id="raid_captain_command", name="Raid Captain's Command", franchise=F,
    desc="Lead the clear: call targets, draw the boss's eye, rally the party and strike where the plan says to. A support line built on marks, taunts and auras.",
    color="#e0b43a", background="city", icon="minecraft:goat_horn",
    book_name="Orders of the Raid Captain", book_lore="The plan survives until first contact. The captain does not.",
    stances=[
        S("Commander's Post", "Hold the middle of the formation: armored and steady, hitting less.", "minecraft:white_banner", armor=3, regen=0.5, damage_pct=-0.08, vfx="MAGIC_CIRCLE"),
        S("Point of the Spear", "Charge at the head of the party: faster and stronger, with less armor.", "minecraft:iron_sword", speed_pct=0.1, damage_pct=0.18, armor=-2, vfx="WIND_SLASH"),
    ],
    stats=[
        T("Veteran's Vigor", "More maximum health.", "max_health", 3, "minecraft:cooked_beef"),
        T("Captain's Plate", "Better armor.", "armor", 1.5, "minecraft:iron_chestplate"),
        T("Voice That Carries", "Your orders reach farther.", "entity_interaction_range", 0.4, "minecraft:goat_horn"),
        T("Calm in the Chaos", "Sturdy under pressure.", "armor_toughness", 1, "minecraft:diamond_chestplate"),
    ],
    arts=[
        A("Focus Fire", "Call a target; everyone's blows on it hurt more for a while.", "mark", "minecraft:name_tag", bonus=0.35, range=24, vfx="MAGIC_CIRCLE"),
        A("Battle Cry", "A shout that gives you and your allies Strength.", "buff_aura", "minecraft:goat_horn", effect="strength", radius=10, seconds=14, vfx="SPIRIT_AURA"),
        A("Draw Aggro", "Bait the boss: hostile creatures nearby turn on you.", "taunt", "minecraft:red_banner", radius=9, targets=6, vfx="DEVIL_CIRCLE"),
        A("Coordinated Strike", "A command-blow that jumps from target to target along the line.", "chain_hit", "minecraft:iron_sword", damage=5, jumps=4, vfx="WIND_SLASH"),
        A("Flush Them Out", "A hooked pull that drags nearby creatures into the killing zone.", "pull", "minecraft:chain", radius=8, strength=1.0, targets=5, damage=4, vfx="SEAL_CHAINS"),
        A("Captain's Cleave", "A cone-shaped sword cut for the creatures in front of you.", "cone_strike", "minecraft:diamond_sword", damage=8, range=4, half_angle=50, vfx="WIND_SLASH"),
        A("Field Triage", "Heal yourself and the allies near you.", "heal_pulse", "minecraft:glistering_melon_slice", amount=7, radius=8, vfx="SPIRIT_AURA"),
    ],
    passives=[
        P("Inspiring Presence", "Allies near you take less damage: Resistance.", "aura", "minecraft:white_banner", effect="resistance", target="allies", radius=8),
        P("Boss Analysis", "You deal more damage to bosses.", "dmg_vs", "minecraft:spyglass", category="boss", bonus=0.25),
        P("Shared Spoils", "Kills give more experience.", "xp_bonus", "minecraft:experience_bottle", pct=0.2),
        P("Hold the Line", "In the thick of a fight you take less damage.", "reduce_when", "minecraft:shield", condition="in_combat", pct=0.12),
        P("Loot Split", "A kill may drop an extra stack of loot.", "drop_bonus", "minecraft:chest", chance=0.18),
        P("Drilled Combination", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_sword", hits=4, bonus=0.5),
        P("Captain Goes Last", "When nearly dead you are briefly shielded.", "last_stand", "minecraft:totem_of_undying", seconds=5),
    ],
    finishers=[
        A("All-Out Assault", "The order everyone has waited for: you and your allies gain a long, strong surge of Strength.", "buff_aura", "minecraft:beacon", effect="strength", radius=14, seconds=25, amp=1, cooldown=1600, vfx="MAGIC_CIRCLE"),
        A("Combined Arms Barrage", "Call the whole party's long-range fire onto the spot you look at.", "meteor_drop", "minecraft:tnt", damage=28, radius=7, range=28, cooldown=1400, vfx="FLAME_EXPLOSION"),
    ],
))

# Evolution rows (3.26.0): like A / P but with the evolved overrides of a passive and the ultimate flag of an art
def PE(name, desc, kind, icon, evolved, **p):
    return Pas(name, desc, kind, p, icon, evolved=evolved)


def AU(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon, ultimate=True)


# ---------------------------------------------------------------------------------------------------------------------------------- 26 (Evolution skill, 3.26.0: docs/skills/evolution_ten.md row 3)
LINES.append(Line(
    id="dragon_king_antares", name="Dragon Monarch's Breath", franchise=F,
    desc="The power of Antares, Monarch of Destruction and King of Dragons: a roar that breaks the spirit, a breath that erases whatever it touches and a curse that stores every blow for the day the Monarch finally wakes.",
    color="#ff4a2b", background="blood", icon="minecraft:dragon_head",
    book_name="Chronicle of the Monarch of Destruction", book_lore="The oldest of the nine never raised his voice. He did not have to; the sky did it for him.",
    stances=[
        S("Dragon Monarch's Might", "The Monarch's wrath fills you: much harder blows, but you stop caring about your own guard.", "minecraft:fire_charge", damage_pct=0.35, armor=-3, vfx="FLAME_EXPLOSION"),
        S("Dragon Hide", "Scales of the oldest dragon cover you: heavy armour you cannot be shoved out of, at the price of a little speed.", "minecraft:netherite_chestplate", armor=6, knockback_res=0.5, speed_pct=-0.08, vfx="EARTH_SPIKES"),
    ],
    stats=[
        T("Monarch's Might", "The strength of the king of dragons in every blow.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Dragon Heart", "A heart that has outlived empires: more maximum health.", "max_health", 3, "minecraft:enchanted_golden_apple"),
        T("Draconic Scales", "Scales harder than any plate.", "armor", 1.5, "minecraft:netherite_chestplate"),
        T("Unshaken Mass", "A body that no blow can shove aside.", "knockback_resistance", 0.12, "minecraft:anvil"),
    ],
    arts=[
        A("Breath of Destruction", "Exhale a cone of annihilating flame that burns everything in front of you.", "cone_strike", "minecraft:dragon_breath", damage=10, range=5, half_angle=55, targets=5, vfx="FLAME_EXPLOSION"),
        A("Dragon's Fear", "A mana-laden roar that saps the courage of everything in front of you: slowed and weakened.", "debuff_cone", "minecraft:goat_horn", effect="slowness", effect2="weakness", seconds=7, seconds2=7, amp=1, range=6, vfx="DEVIL_CIRCLE"),
        A("Tail Sweep", "Lash out with a dragon's tail and hurl away everything around you.", "ring_burst", "minecraft:bone", damage=8, radius=4.5, knockback=1.2, targets=6, vfx="FIRE_BURST"),
        A("Rending Claw", "Rake the foe in front of you and throw it into the air.", "uppercut", "minecraft:blaze_powder", damage=9, launch=0.9, vfx="FIRE_BURST"),
        A("Breath of Destruction: Shift", "Crouching, the breath narrows into a beam of white-hot destruction that pierces a long line of foes.", "ray_shot", "minecraft:magma_cream", damage=12, range=26, pierce=3, vfx="FIRE_SPIRAL"),
        A("Wing Dive", "Spread vast wings, climb and crash down in a ball of fire.", "leap_slam", "minecraft:elytra", damage=11, radius=4, height=1.2, knockback=1.2, vfx="FLAME_EXPLOSION"),
        A("Dragon's Fear: Shift", "Crouching, the roar becomes a hammer: lesser creatures around you are frozen in terror.", "stun_ring", "minecraft:wither_skeleton_skull", radius=6, seconds=2.5, targets=8, vfx="DEVIL_CIRCLE"),
        A("Tail Sweep: Shift", "Crouching, you spin with the whole body of the dragon and strike all around you several times.", "whirlwind", "minecraft:netherite_axe", damage=5, radius=4.5, hits=5, vfx="FIRE_SPIRAL"),
    ],
    passives=[
        PE("Dragon's Curse", "Your blows store a share of their damage on the foe as a curse (Debt). It ticks while it waits, and the Monarch's Ultimate consumes it.", "debt_store", "minecraft:crying_obsidian",
           {"store_pct": 0.6, "max_pct": 0.9, "hold_seconds": 20}, store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=14, vfx="FLAME_EXPLOSION"),
        P("Dragonhide: Flame", "You take much less fire damage.", "resist", "minecraft:lava_bucket", damage_kind="fire", pct=0.4),
        PE("Monarch's Presence", "Enemies near you are constantly weakened by the weight of the Monarch's presence.", "aura", "minecraft:wither_rose",
           {"radius": 9, "amp": 1}, effect="weakness", target="enemies", radius=6),
        PE("Slayer of Sovereigns", "You deal much more damage to bosses.", "dmg_vs", "minecraft:nether_star", {"bonus": 0.6}, category="boss", bonus=0.3),
        P("Ancient Fury", "Your blows may strike with the force of an age-old wrath.", "crit", "minecraft:ender_eye", chance=0.12, mult=1.8),
        P("Ashes of Destruction", "Your blows may wither the foe as if burned to ash.", "on_hit_effect", "minecraft:coal", effect="wither", seconds=4, chance=0.3),
        P("Scorching Scales", "Part of the damage you take is burned back into the attacker.", "thorns", "minecraft:magma_cream", pct=0.2),
        P("Last Roar of the King", "When nearly dead the dragon's spirit shields you.", "last_stand", "minecraft:totem_of_undying", seconds=7),
    ],
    finishers=[
        A("Spiritual Body Manifestation", "Your true form rises: for a long while you are mightier and nearly unbreakable.", "buff_self", "minecraft:dragon_egg", effect="strength", effect2="resistance", seconds=30, amp=1, amp2=1, cooldown=1700, vfx="FLAME_EXPLOSION"),
        AU("Monarch of Destruction", "The Monarch wakes: every curse stored on the foes around you detonates at once in the largest blast of fire in the pack.", "debt_burst", "minecraft:nether_star",
          damage=50, mult=4.0, radius=14, targets=10, cost=0.08, cooldown=18000, vfx="FLAME_EXPLOSION"),
    ],
    evolution={"requirement": "boss_kills", "count": 10,
               "text": "Slay 10 bosses while Dragon Monarch's Breath is switched on to awaken the Monarch of Destruction.",
               "ultimate_name": "Ultimate Skill, Monarch of Destruction"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 27 (Evolution skill, 3.26.0: docs/skills/evolution_ten.md row 4)
LINES.append(Line(
    id="white_tiger_baek", name="Tiger's Claws", franchise=F,
    desc="Baek Yoonho's way of fighting: the White Tiger Guild master turns hands and body into those of a great white beast, reads his prey with the Eyes of the Beast and grows fiercest when the next blow could end him.",
    color="#e8e8f0", background="frost", icon="minecraft:white_wool",
    book_name="Field Notes of the White Tiger", book_lore="The first rule of the guild: when the claws come out, the allies step back.",
    stances=[
        S("Tiger Form", "Hands and hair turn to the white beast: faster and far harder-hitting, with thinner guard.", "minecraft:white_wool", damage_pct=0.25, speed_pct=0.15, armor=-2, vfx="WIND_SLASH"),
        S("Prowl", "Low and silent: very quick and hard to pin down, but you wear no armour of your own.", "minecraft:rabbit_foot", speed_pct=0.22, crit_chance=0.08, armor=-4, vfx="WIND_RING"),
    ],
    stats=[
        T("Beast's Strength", "The tiger's raw strength in every swing.", "attack_damage", 1.5, "minecraft:iron_axe"),
        T("Predator's Gait", "A hunter's swift stride.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Guild Master's Stamina", "He keeps fighting after a beating that would end others: more maximum health.", "max_health", 3, "minecraft:cooked_porkchop"),
        T("Claw Speed", "Claws that strike twice before you blink.", "attack_speed", 0.06, "minecraft:rabbit_foot", op="mult"),
    ],
    arts=[
        A("Claw Slash", "Rake the cone in front of you with claws of white fur and bone.", "cone_strike", "minecraft:iron_sword", damage=8, range=4, half_angle=55, vfx="WIND_SLASH"),
        A("Pounce", "Spring at the foe and rake everything in the path of the leap.", "dash_strike", "minecraft:feather", damage=9, distance=8, targets=4, vfx="WIND_SLASH"),
        A("Beast's Roar", "A tiger's roar that saps the nerve of all in front of you: slowed and weakened.", "debuff_cone", "minecraft:goat_horn", effect="slowness", effect2="weakness", seconds=6, seconds2=6, amp=1, vfx="WIND_RING"),
        A("Rending Bleed", "Tear open the foe in front of you: the weaker it already is, the deeper the claws go.", "execute", "minecraft:red_dye", damage=10, bonus=1.0, range=3.8, vfx="WIND_SLASH"),
        A("Claw Combo: Shift", "Crouching, you whirl and rake everything around you again and again.", "whirlwind", "minecraft:iron_axe", damage=4, radius=3.8, hits=6, vfx="WIND_SLASH"),
        A("Pounce: Shift", "Crouching, you coil and make a huge leap, landing on the foes below with all your weight.", "leap_slam", "minecraft:rabbit_hide", damage=10, radius=3.5, height=1.4, knockback=0.9, vfx="WIND_RING"),
        A("Eyes of the Beast", "Yellow slit eyes read the foe you look at: it takes more damage for a while.", "mark", "minecraft:spider_eye", range=20, seconds=10, bonus=0.35, vfx="SPIRIT_AURA"),
        A("Beast's Roar: Shift", "Crouching, the roar becomes a shockwave: creatures around you freeze in place.", "stun_ring", "minecraft:white_wool", radius=5, seconds=2, targets=6, vfx="WIND_RING"),
    ],
    passives=[
        PE("Rending Wounds", "Your claws leave wounds that bleed later: a share of your damage is stored on the foe (Debt). The Ultimate rips it all open at once.", "debt_store", "minecraft:redstone",
           {"store_pct": 0.5, "max_pct": 0.8, "hold_seconds": 18}, store_pct=0.25, tick_seconds=2, max_pct=0.5, hold_seconds=12, vfx="WIND_SLASH"),
        PE("Reflexes of the Beast", "Your reflexes are sharp enough to avoid a blow completely.", "dodge", "minecraft:rabbit_foot", {"chance": 0.22}, chance=0.1),
        P("Night Hunter", "At night the beast's eyes see everything.", "self_effect_when", "minecraft:fermented_spider_eye", effect="night_vision", condition="night"),
        P("Predator's Charge", "You deal more damage while sprinting at your prey.", "sprint_bonus", "minecraft:sugar", bonus=0.25),
        PE("Won't Stay Down", "When a blow would have ended you, the beast shields you for a while.", "last_stand", "minecraft:totem_of_undying", {"seconds": 11, "rest": 1200}, seconds=6, rest=2400),
        P("Scent of Blood", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:rotten_flesh", threshold=0.25, bonus=0.6),
        P("Relentless Claws", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_sword", hits=4, bonus=0.5),
        P("Trophy of the Hunt", "A kill heals you and fills you with speed.", "kill_reward", "minecraft:bone", heal=3, effect="speed", seconds=5),
    ],
    finishers=[
        A("Full Beast Form", "The transformation completes: for a long while you are far stronger and far faster.", "buff_self", "minecraft:white_banner", effect="strength", effect2="speed", seconds=25, amp=1, amp2=1, cooldown=1500, vfx="WIND_RING"),
        AU("White Tiger Awakened", "The beast is fully awake: a huge pounce, then every wound you have opened bursts at once.", "debt_burst", "minecraft:diamond_axe",
          damage=32, mult=3.0, radius=9, targets=10, cost=0.07, cooldown=15000, vfx="WIND_SLASH"),
    ],
    evolution={"requirement": "lethal_survived", "count": 15,
               "text": "Survive 15 blows that would have left you at 2 health or less while Tiger's Claws is switched on to awaken the White Tiger.",
               "ultimate_name": "Ultimate Skill, White Tiger Awakened"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 28 (3.27.0, Evolution skill, docs/skills/evolution_ten_b.md row 5)
LINES.append(Line(
    id="cha_hae_in_sword", name="Cha Hae-In's Sword", franchise=F,
    desc="The S-rank sword hunter who smells mana the way others hear a voice: she reads her prey before it moves, marks the weak point and finishes it with a single thrust. Evolves after thirty elite kills.",
    color="#c2e6ff", background="city", icon="minecraft:iron_sword",
    book_name="Vice Guild Master's Sword Log", book_lore="She smells the fight before she sees it, and she has never once been wrong about who is lying.",
    stances=[
        S("Hunter's Poise", "A swordswoman's balanced stance: faster, sharper crits and a little more damage.", "minecraft:iron_sword", speed_pct=0.1, crit_chance=0.1, damage_pct=0.1, vfx="WIND_SLASH"),
        S("Mana Sense", "Breathe in and read the room: slow, patient and healing, with a very keen eye for the weak point.", "minecraft:spider_eye", speed_pct=-0.1, crit_chance=0.15, regen=0.6, armor=2, vfx="SPIRIT_AURA"),
    ],
    stats=[
        T("S-Rank Edge", "Cutting power of a hunter few can match.", "attack_damage", 1.5, "minecraft:iron_sword"),
        T("Hunter's Speed", "Quick, certain steps.", "movement_speed", 0.04, "minecraft:leather_boots", op="mult"),
        T("Mana Reserves", "More maximum health from a deep mana pool.", "max_health", 2, "minecraft:golden_apple"),
        T("Long Blade Reach", "A little more reach with the long sword.", "entity_interaction_range", 0.4, "minecraft:spyglass"),
    ],
    arts=[
        A("Mana Scent", "Breathe in the mana of one monster: it takes more damage for a while.", "mark", "minecraft:fermented_spider_eye", range=24, seconds=10, bonus=0.35, vfx="SPIRIT_AURA"),
        A("Mana Scent: Shift", "Crouching, the scent reveals every weak point in front of you: the monsters are weakened and made to glow.", "debuff_cone", "minecraft:glowstone_dust", effect="weakness", effect2="glowing", seconds=7, seconds2=7, amp=1, range=6, vfx="SPIRIT_AURA"),
        A("Swift Thrust", "A lightning thrust that goes through every monster in your path.", "dash_strike", "minecraft:iron_sword", distance=8, damage=8, targets=4, vfx="WIND_SLASH"),
        A("Falling Blade", "Crouching, you leap high and come down point-first on the monsters below.", "leap_slam", "minecraft:feather", damage=10, radius=3.5, height=1.2, knockback=0.8, vfx="WIND_RING"),
        A("Mana Blade Wave", "A thin crescent of mana flies from the sword and pierces a line of foes.", "ray_shot", "minecraft:light_blue_dye", damage=8, range=24, pierce=3, vfx="WIND_SLASH"),
        A("Needle Flurry", "Crouching, a swarm of mana needles flies at the monster you look at.", "barrage", "minecraft:iron_nugget", damage=2.5, shots=8, range=16, vfx="WIND_SLASH"),
        A("Guard and Counter", "Meet the blow with the flat of the blade: for a moment a share of the damage you take is flung back.", "ward_reflect", "minecraft:shield", seconds=5, reflect_pct=0.45, vfx="MIRROR_PANE"),
        A("Moonrise Cut", "Crouching, a rising cut that launches the foe in front of you.", "uppercut", "minecraft:prismarine_shard", damage=9, launch=0.8, vfx="WIND_SLASH"),
    ],
    passives=[
        PE("Scent of Weakness", "Your blows store delayed damage on the foe, the weak points she smelled, and your Debt arts collect it.", "debt_store", "minecraft:fermented_spider_eye", {"store_pct": 0.45, "max_pct": 0.75, "hold_seconds": 18}, store_pct=0.25, tick_seconds=3, max_pct=0.5, hold_seconds=12, vfx="SPIRIT_AURA"),
        PE("Precise Strike", "Your blows may strike critically.", "crit", "minecraft:flint", {"chance": 0.28, "mult": 2.1}, chance=0.15, mult=1.8),
        PE("Finishing Instinct", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_nugget", {"threshold": 0.3, "bonus": 1.0}, threshold=0.2, bonus=0.6),
        P("Swordswoman's Guard", "While you hold a sword you take less damage.", "reduce_when", "minecraft:iron_chestplate", condition="holding_sword", pct=0.12),
        P("Keen Senses", "While you hold a sword you are quick: Speed.", "self_effect_when", "minecraft:sugar", effect="speed", condition="holding_sword"),
        P("Tagged by Scent", "Your blows may make the foe glow where she can follow it.", "on_hit_effect", "minecraft:glowstone_dust", effect="glowing", seconds=8, chance=0.35),
        P("Trained Against Mana", "You take much less magic damage.", "resist", "minecraft:enchanted_book", damage_kind="magic", pct=0.3),
        P("Hunter's Reward", "A kill heals you and may grant a moment of speed.", "kill_reward", "minecraft:bone", heal=2, effect="speed", seconds=4),
    ],
    finishers=[
        A("Radiant Blade Dance", "A flurry of blinding cuts spins up around you.", "whirlwind", "minecraft:diamond_sword", damage=5, hits=8, radius=4.5, interval=3, cooldown=1400, vfx="WIND_TORNADO"),
        AU("Vice Master's Verdict", "The weak points she has smelled all open at once: every Debt left on the foes around you comes due in a single sweeping thrust.", "debt_burst", "minecraft:diamond_sword",
           damage=24, mult=2.4, radius=12, targets=12, cost=0.07, cooldown=16000, vfx="WIND_SLASH"),
    ],
    evolution={"requirement": "elite_kills", "count": 30,
               "text": "Slay 30 elite or boss creatures while Cha Hae-In's Sword is switched on.",
               "ultimate_name": "Ultimate Skill, Vice Master's Verdict"},
))

# ---------------------------------------------------------------------------------------------------------------------------------- 29 (3.27.0, Evolution skill, docs/skills/evolution_ten_b.md row 6)
LINES.append(Line(
    id="baran_demon_monarch", name="Baran, Monarch of Demons", franchise=F,
    desc="The king of the Demon Castle: lightning that tears the air, a charge in white flame, a legion that pours from hell-portals and a hunger that feeds on blood. Evolves after eighty thousand damage dealt.",
    color="#9b2d5e", background="ruin", icon="minecraft:netherite_helmet",
    book_name="Chronicle of the Demon Castle's King", book_lore="He does not ask the weak to kneel. He waits, and the castle does the asking.",
    stances=[
        S("Demon King's Wrath", "The castle's king in full fury: much harder blows, with the guard let down.", "minecraft:crying_obsidian", damage_pct=0.3, armor=-3, vfx="DEVIL_CIRCLE"),
        S("Blood Throne", "Sit enthroned in blood-red armour: heavy, hard to shove and slowly healing, but slow.", "minecraft:netherite_chestplate", armor=5, knockback_res=0.5, regen=0.6, speed_pct=-0.1, vfx="SEAL_CHAINS"),
    ],
    stats=[
        T("Demon King's Might", "A monarch's crushing strength.", "attack_damage", 1.5, "minecraft:netherite_sword"),
        T("Castle Lord's Vitality", "Maximum health of a king no wound can unseat.", "max_health", 3, "minecraft:golden_apple"),
        T("Infernal Plate", "Armour forged in the Demon Castle.", "armor", 1.5, "minecraft:netherite_chestplate"),
        T("Lord of Lightning", "Faster, hungrier blows.", "attack_speed", 0.06, "minecraft:lightning_rod", op="mult"),
    ],
    arts=[
        A("Lightning Breath", "Exhale a torrent of lightning in a cone in front of you.", "cone_strike", "minecraft:lightning_rod", damage=10, range=5, half_angle=50, targets=5, vfx="LIGHTNING_SPEAR"),
        A("Lightning Breath: Shift", "Crouching, the breath becomes a bolt that leaps from foe to foe.", "chain_hit", "minecraft:copper_ingot", damage=6, jumps=5, jump_range=6, vfx="LIGHTNING_FIEND"),
        A("White Flame Charge", "Turn into a streak of white flame and tear through every monster in your path.", "dash_strike", "minecraft:fire_charge", distance=9, damage=9, targets=4, vfx="FLAME_TRAIL"),
        A("White Flame Charge: Shift", "Crouching, you hurl yourself high and crash down in a ring of white fire.", "leap_slam", "minecraft:blaze_rod", damage=11, radius=4, height=1.2, knockback=1.0, vfx="FLAME_EXPLOSION"),
        A("Hell's Army", "Portals tear open around you and the legion bursts out, hurling the monsters back.", "ring_burst", "minecraft:wither_skeleton_skull", damage=9, radius=5, knockback=1.2, targets=7, vfx="DEVIL_CIRCLE"),
        A("Hell's Army: Shift", "Crouching, the legion's terror freezes the creatures around you in place.", "stun_ring", "minecraft:skeleton_skull", radius=6, seconds=2.5, targets=8, vfx="DEVIL_CIRCLE"),
        A("Blood Crystal", "Bite into a foe and drain its blood into the crystal: you heal for a share of the damage.", "drain_bite", "minecraft:ghast_tear", damage=9, heal_pct=0.5, range=3.6, vfx="SHADOW_POOL"),
        A("Doppelganger", "A blood-born double stands in your place: you gain a shield of absorption.", "shield", "minecraft:ender_eye", absorb_hearts=7, seconds=10, resist_amp=1, vfx="SHADOW_POOL"),
    ],
    passives=[
        PE("Blood Tithe", "Your blows store delayed damage on the foe as a tithe of blood, and your Debt arts collect it.", "debt_store", "minecraft:redstone", {"store_pct": 0.6, "max_pct": 0.9, "hold_seconds": 20}, store_pct=0.3, tick_seconds=3, max_pct=0.5, hold_seconds=14, vfx="DEVIL_CIRCLE"),
        PE("Hunger of the Blood Crystal", "Your blows heal you.", "lifesteal", "minecraft:red_dye", {"pct": 0.15}, pct=0.07),
        PE("Lightning-Born", "You take much less lightning damage.", "resist", "minecraft:lightning_rod", {"pct": 0.6}, damage_kind="lightning", pct=0.4),
        P("Demon's Brand", "Your blows may slow the foe as if chained.", "on_hit_effect", "minecraft:chain", effect="slowness", seconds=4, chance=0.3),
        P("Undying King", "When nearly dead your wounds close quickly.", "regen_when", "minecraft:glistering_melon_slice", condition="low_health", per_second=1.6),
        P("Soul Harvest", "A kill heals you and fills you with strength.", "kill_reward", "minecraft:soul_lantern", heal=3, effect="strength", seconds=5),
        P("Lord of the Castle", "Underground, in your own halls, you deal more damage.", "dmg_when", "minecraft:deepslate_bricks", condition="underground", bonus=0.3),
        P("Dread Presence", "Enemies near you are slowed by the weight of the Demon King.", "aura", "minecraft:wither_rose", effect="slowness", target="enemies", radius=6),
    ],
    finishers=[
        A("Castle's Judgment", "A column of demonic lightning falls on the place you look at.", "meteor_drop", "minecraft:nether_star", damage=32, radius=7, range=24, delay=18, cooldown=1500, vfx="DEVIL_CIRCLE"),
        AU("Monarch of Demons", "The king takes the field: every tithe of blood stored on the foes around you is called in at once.", "debt_burst", "minecraft:dragon_head",
           damage=55, mult=4.0, radius=15, targets=12, cost=0.08, cooldown=18000, vfx="DEVIL_CIRCLE"),
    ],
    evolution={"requirement": "damage_dealt", "count": 80000,
               "text": "Deal 80000 damage to hostile creatures while Baran, Monarch of Demons is switched on.",
               "ultimate_name": "Ultimate Skill, Monarch of Demons"},
))
