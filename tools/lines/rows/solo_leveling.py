"""Solo Leveling: 25 unique skill lines (hunter classes, the System, the Shadow Monarch's arts, Monarch powers, Ruler authorities, dagger and assassin techniques).

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
        P("Self-Mending Shell", "Your wounds close over time, even out of combat.", "mending", "minecraft:scute", seconds=14),
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
        A("Monarch's Roar", "A roar that shatters the nerve of everything near you: a mighty ring of force and stunning.", "ring_burst", "minecraft:dragon_head", damage=22, radius=7, targets=8, knockback=2.0, cooldown=1400, vfx="WIND_RING"),
        A("Wall of Killing Intent", "A lane of crushing menace sweeps ahead of you.", "ground_wave", "minecraft:netherite_scrap", damage=24, length=18, width=5, cooldown=1300, vfx="DEVIL_CIRCLE"),
    ],
))
