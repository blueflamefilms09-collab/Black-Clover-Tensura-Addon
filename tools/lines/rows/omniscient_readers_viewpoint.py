"""Omniscient Reader's Viewpoint: 25 unique skill lines (constellations, attributes, scenarios, the companions of Dokja's Company).

Canon is paraphrased from memory; where a detail was uncertain the line is built in the spirit of the work, not as quoted text.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from lines_lib import Line, Stance, Stat, Art, Pas  # noqa: E402

F = "omniscient_readers_viewpoint"


def S(name, desc, icon, **p):
    return Stance(name, desc, icon, p)


def T(name, desc, attribute, amount, icon, op="add"):
    return Stat(name, desc, attribute, amount, op, icon)


def A(name, desc, kind, icon, **p):
    return Art(name, desc, kind, p, icon)


def P(name, desc, kind, icon, **p):
    return Pas(name, desc, kind, p, icon)


LINES = []

# --- 1 to 5
LINES += [
    Line(
        id="omniscient_viewpoint", name="Omniscient Reader's Viewpoint", franchise=F,
        desc="The skill of the one reader who knows how the story ends. Read a foe's weak point, mark it, and finish the scene exactly as the last page says.",
        color="#4AA3FF", background="stellar", icon="minecraft:spyglass",
        book_name="Tome of the Only Reader", book_lore="Three thousand and some chapters, and only one person ever finished them.",
        stances=[
            S("Reader's Distance", "Step back from the page: a cool, quick stance that trades a little damage for speed and sharper crits.", "minecraft:spyglass",
              damage_pct=-0.1, speed_pct=0.12, crit_chance=0.1, vfx="MAGIC_CIRCLE"),
            S("Cover to Cover", "Commit to the whole book: hit much harder, but you leave your guard open while you read ahead.", "minecraft:written_book",
              damage_pct=0.25, armor=-3, vfx="MAGIC_CIRCLE"),
        ],
        stats=[
            T("Insightful Reader", "Years of reading sharpen your fortune: bonus luck.", "luck", 1, "minecraft:enchanted_book"),
            T("Bookworm's Endurance", "A body trained on long sleepless nights: more maximum health.", "max_health", 4, "minecraft:golden_apple"),
            T("Quick Page-Turn", "Flick through the fight faster: a bonus to attack speed.", "attack_speed", 0.05, "minecraft:feather", "mult"),
        ],
        arts=[
            A("Read the Opening Line", "Fix your eyes on a foe far away; a magic circle marks it and it takes extra damage for a while.", "mark", "minecraft:ender_eye", vfx="MAGIC_CIRCLE"),
            A("Spoiler Strike", "Strike the foe in front of you with a blow that lands far harder the weaker it already is.", "execute", "minecraft:iron_sword", vfx="WIND_SLASH"),
            A("Skim the Pages", "Skip ahead and appear a few blocks forward in a ripple of spatial light.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT"),
            A("Footnote Barrage", "Fire a volley of ink-bright bolts at the foe you are looking at.", "barrage", "minecraft:spectral_arrow", vfx="LIGHTNING_SPEAR", damage=3.5),
            A("Cross-Reference Chain", "One strike that leaps from foe to foe along the threads the story connects.", "chain_hit", "minecraft:lightning_rod", vfx="THREAD_LINE"),
            A("Turn the Page", "Sweep a cone of pale light that slows everything caught in it.", "debuff_cone", "minecraft:paper", effect="slowness", effect2="weakness", seconds2=4, vfx="MIRROR_PANE"),
            A("Line of Sight Spear", "A thin beam along your gaze that pierces a line of enemies.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR"),
        ],
        passives=[
            P("Knowing the Ending", "You deal much more damage to foes who are almost dead: you already know how this scene ends.", "execute_threshold", "minecraft:wither_skeleton_skull"),
            P("Foreseen Blow", "You sometimes step aside from a hit you saw coming.", "dodge", "minecraft:rabbit_foot"),
            P("Reader's Calm", "Crouching to read calms your body and grants you resistance.", "self_effect_when", "minecraft:glow_ink_sac", effect="resistance", condition="sneaking"),
            P("Weak Point Notes", "Your margin notes on bosses make you hit them harder.", "dmg_vs", "minecraft:writable_book", category="boss"),
            P("Margin Annotation", "Your blows sometimes land as critical hits, scribbled right on the weak point.", "crit", "minecraft:ink_sac"),
            P("Chapter Recap", "Your wounds close slowly over time as you recap the story.", "mending", "minecraft:clock"),
            P("Spoiled Opponents", "Struck foes sometimes become weakened, rattled by what you know.", "on_hit_effect", "minecraft:fermented_spider_eye", effect="weakness"),
        ],
        finishers=[
            A("Ending Spoiler", "Reveal the last line to the foe in front of you: a devastating blow that is deadliest on the nearly dead.", "execute", "minecraft:nether_star",
              vfx="MAGIC_CIRCLE", damage=24, bonus=1.2, cooldown=2400),
            A("Final Chapter Revealed", "Name the spot where the last page lands; after a heartbeat a comet of written light crashes there.", "meteor_drop", "minecraft:end_crystal",
              vfx="MAGIC_CIRCLE", damage=26, radius=6, cooldown=3000),
        ],
    ),
    Line(
        id="fourth_wall", name="Fourth Wall", franchise=F,
        desc="The invisible barrier between the reader and the story. Hold it up and nothing of the scenario can touch you; drop it and everything touches back.",
        color="#9AB6D4", background="tower", icon="minecraft:glass",
        book_name="Codex of the Unseen Barrier", book_lore="Between the reader and the page there is a wall that no scenario was written to cross.",
        stances=[
            S("Wall Raised", "The Fourth Wall stands whole: heavy armor and strong resistance to being pushed, but you move slowly behind it.", "minecraft:shield",
              armor=6, knockback_res=0.6, speed_pct=-0.15, vfx="MIRROR_PANE"),
            S("Breach in the Wall", "You open a crack to strike through: more damage and a touch of regeneration, at the cost of armor.", "minecraft:iron_bars",
              damage_pct=0.2, regen=0.8, armor=-2, vfx="MIRROR_PANE"),
        ],
        stats=[
            T("Wall Thickness", "The barrier hardens: bonus armor toughness.", "armor_toughness", 1, "minecraft:netherite_scrap"),
            T("Unbreakable Margins", "Nothing shoves you off the page: bonus knockback resistance.", "knockback_resistance", 0.1, "minecraft:anvil"),
            T("Sealed Pages", "A body bound tightly in its cover: more maximum health.", "max_health", 4, "minecraft:leather"),
        ],
        arts=[
            A("Raise the Fourth Wall", "A pane of shimmering glass rises around you, soaking up damage as absorption hearts.", "shield", "minecraft:shield", vfx="MIRROR_PANE"),
            A("Wall That Answers Back", "For a few seconds, part of the damage dealt to you is flung back at your attacker.", "ward_reflect", "minecraft:glass_pane", vfx="MIRROR_PANE"),
            A("Silence Beyond the Wall", "A ring of hush spreads and freezes the creatures around you in place for a moment.", "stun_ring", "minecraft:blue_ice", vfx="WATER_RING"),
            A("Bait the Reader's Gaze", "Draw the attention of nearby monsters onto yourself while you brace.", "taunt", "minecraft:bell", vfx="SPIRIT_AURA"),
            A("Wall Shockwave", "The barrier snaps outward in a burst that shoves everything away from you.", "ring_burst", "minecraft:wind_charge", vfx="WIND_RING", knockback=1.4),
            A("Mend the Margin", "Heal yourself and slowly regenerate behind the glass.", "heal_self", "minecraft:glistering_melon_slice", vfx="SPIRIT_AURA"),
            A("Fold the Page Inward", "Drag the creatures around you toward the wall, grinding them against it.", "pull", "minecraft:lead", vfx="SEAL_CHAINS"),
        ],
        passives=[
            P("Plot Armor", "While you are in a fight, you take less damage: the story refuses to kill you here.", "reduce_when", "minecraft:iron_chestplate", condition="in_combat"),
            P("Backlash of the Wall", "Part of the damage you take is returned to whoever struck you.", "thorns", "minecraft:cactus"),
            P("Insulated from the Blast", "You take less explosion damage.", "resist", "minecraft:tnt", damage_kind="explosion"),
            P("Wall Shove", "Your blows sometimes shove the foe back a step.", "knockback_on_hit", "minecraft:piston"),
            P("Last Page Barrier", "When nearly dead, a barrier rises around you for a few seconds.", "last_stand", "minecraft:totem_of_undying"),
            P("Behind the Wall", "While crouching you heal steadily, sheltered.", "regen_when", "minecraft:campfire", condition="sneaking"),
            P("Landing on the Spine", "You take less fall damage.", "fall_guard", "minecraft:hay_block"),
        ],
        finishers=[
            A("Wall of Absolute Separation", "The Fourth Wall closes completely: a huge absorption barrier with extra resistance for a long moment.", "shield", "minecraft:beacon",
              vfx="MIRROR_PANE", absorb_hearts=14, seconds=12, resist_amp=1, cooldown=2700),
            A("Wall Collapse", "The wall breaks all at once and a glass-bright shockwave shatters outward around you.", "ring_burst", "minecraft:tinted_glass", vfx="MIRROR_PANE",
              damage=22, radius=6, targets=10, knockback=1.6, cooldown=2400),
        ],
    ),
    Line(
        id="character_list", name="Character List", franchise=F,
        desc="A roster of personas that you can put on like costumes. Equip the Vanguard, then the Assassin, and play every part the scenario needs.",
        color="#E0B24A", background="city", icon="minecraft:writable_book",
        book_name="Dramatis Personae", book_lore="Each name on the list is a life you have read, and any of them can answer when you call.",
        stances=[
            S("Character: Vanguard", "Wear the Vanguard: sturdier and a bit stronger, but slow on your feet.", "minecraft:iron_helmet",
              armor=3, damage_pct=0.1, speed_pct=-0.1, vfx="SPIRIT_AURA"),
            S("Character: Assassin", "Wear the Assassin: fast and prone to crits, but thin-skinned.", "minecraft:leather_boots",
              speed_pct=0.2, crit_chance=0.15, armor=-3, vfx="SHADOW_POOL"),
        ],
        stats=[
            T("Roster Capacity", "More names fit on your list: bonus maximum health.", "max_health", 4, "minecraft:book"),
            T("Character Affinity", "You fit your roles snugly: bonus attack damage.", "attack_damage", 1, "minecraft:iron_sword"),
            T("Costume Change Speed", "Swap personas without losing a step: bonus movement speed.", "movement_speed", 0.04, "minecraft:leather_boots", "mult"),
        ],
        arts=[
            A("Equip: Vanguard's Might", "Take on the Vanguard's strength and toughness for a while.", "buff_self", "minecraft:iron_chestplate", effect="strength", effect2="resistance", vfx="SPIRIT_AURA"),
            A("Equip: Assassin's Tread", "Slip into the Assassin: swift feet and a higher jump for a while.", "buff_self", "minecraft:chainmail_boots", effect="speed", effect2="jump_boost", amp=1, vfx="SHADOW_POOL", seconds=14),
            A("Roster Rally", "Call the cast together: you and nearby allies gain strength.", "buff_aura", "minecraft:white_banner", effect="strength", vfx="SPIRIT_AURA"),
            A("Character Recovery", "Step off stage for a breath: heal and regenerate.", "heal_self", "minecraft:golden_carrot", vfx="SPIRIT_AURA"),
            A("Persona Swap Dash", "Change roles mid-charge and dash forward, striking all in your path.", "dash_strike", "minecraft:feather", vfx="WIND_SLASH"),
            A("Backstage Curtain", "Drop a curtain of smoke that blinds the creatures near you while you reposition.", "smoke", "minecraft:black_dye", vfx="SHADOW_POOL"),
            A("Persona Slash", "A cone-shaped sweep in the style of whichever role you wear.", "cone_strike", "minecraft:stone_sword", vfx="WIND_SLASH"),
        ],
        passives=[
            P("Quick Change", "While sprinting you gain a burst of extra speed.", "self_effect_when", "minecraft:sugar", effect="speed", condition="sprinting"),
            P("Supporting Cast", "Allies near you slowly regenerate.", "aura", "minecraft:red_banner", effect="regeneration", target="allies"),
            P("Ensemble Combo", "Every few blows in a row hit much harder.", "combo", "minecraft:blaze_rod"),
            P("Rising Action", "Blows in a row grow stronger the longer the fight goes on.", "momentum", "minecraft:redstone"),
            P("Typecast Weapon", "You deal more damage while holding a sword.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword"),
            P("Character Development", "Your kills give more experience.", "xp_bonus", "minecraft:experience_bottle"),
            P("Curtain Call", "A kill heals you and grants a brief burst of strength.", "kill_reward", "minecraft:gold_ingot", effect="strength"),
        ],
        finishers=[
            A("Total Character Overlay", "Layer every persona at once: very high strength and speed for a long moment.", "buff_self", "minecraft:enchanted_golden_apple",
              effect="strength", effect2="speed", amp=2, amp2=1, seconds=18, vfx="SPIRIT_AURA", cooldown=2700),
            A("Ensemble Finale", "The whole cast strikes: a blow that chains across many foes.", "chain_hit", "minecraft:totem_of_undying", vfx="THREAD_LINE",
              damage=18, jumps=6, cooldown=2400),
        ],
    ),
    Line(
        id="bookmark", name="Bookmark", franchise=F,
        desc="A ribbon laid between the pages of the world. Mark your place, skip ahead, and snap back to wherever you left the story.",
        color="#C0504D", background="ruin", icon="minecraft:lead",
        book_name="Ribbon-Bound Tome", book_lore="The reader keeps his place in a thousand worlds with a single red thread.",
        stances=[
            S("Page Held", "You hold your place firm: a little armor and a steady heal, but you will not run.", "minecraft:lodestone",
              armor=3, regen=1.0, speed_pct=-0.1, vfx="THREAD_LINE"),
            S("Ribbon Taut", "The ribbon pulls tight and so do you: much faster and a bit crit-happy, but you strike lighter.", "minecraft:string",
              speed_pct=0.2, crit_chance=0.1, damage_pct=-0.1, vfx="THREAD_LINE"),
        ],
        stats=[
            T("Ribbon Reach", "A longer ribbon: you can interact with entities from farther away.", "entity_interaction_range", 0.5, "minecraft:lead"),
            T("Placeholder Stride", "Your steps skip ahead: bonus movement speed.", "movement_speed", 0.04, "minecraft:rabbit_hide", "mult"),
            T("Dog-Eared Luck", "Pages fall open to the lucky ones: bonus luck.", "luck", 1, "minecraft:paper"),
        ],
        arts=[
            A("Return to the Marked Page", "Snap along the ribbon and reappear a good way forward.", "blink", "minecraft:recovery_compass", vfx="SPATIAL_RIFT", distance=10),
            A("Slip the Ribbon In", "Lay a bookmark on a distant foe, so it takes more damage while marked.", "mark", "minecraft:name_tag", vfx="THREAD_LINE"),
            A("Pull the Ribbon Tight", "Reel the creatures around you in on glowing threads.", "pull", "minecraft:fishing_rod", vfx="THREAD_LINE"),
            A("Ribbon Lash", "Dash forward as a streak of red thread, cutting everything in your path.", "dash_strike", "minecraft:red_dye", vfx="THREAD_LINE"),
            A("Skip Ahead a Chapter", "Leap high and crash down on the foes below.", "leap_slam", "minecraft:slime_ball", vfx="WIND_RING"),
            A("Fold the Corner", "Fold the world over yourself in a puff of smoke, blinding those nearby.", "smoke", "minecraft:gray_dye", vfx="SPATIAL_RIFT"),
            A("Ribbon Spear", "Hurl a thread of red light that pierces whatever stands in a line.", "ray_shot", "minecraft:arrow", vfx="THREAD_LINE"),
        ],
        passives=[
            P("Slip Between Pages", "You sometimes slide out of the way of a blow completely.", "dodge", "minecraft:phantom_membrane"),
            P("Skipping Ahead", "You hit harder while sprinting.", "sprint_bonus", "minecraft:leather_boots"),
            P("Soft Landing on the Spine", "You take less fall damage.", "fall_guard", "minecraft:feather"),
            P("Tangling Ribbon", "Whoever strikes you may be slowed by clinging thread.", "on_hurt_retaliate", "minecraft:cobweb", effect="slowness"),
            P("Chapter Skip", "While airborne you gain a boosted jump.", "self_effect_when", "minecraft:slime_block", effect="jump_boost", condition="airborne"),
            P("Rest at the Marked Page", "While crouching you quietly heal.", "regen_when", "minecraft:red_bed", condition="sneaking"),
            P("Light Reading", "You tire more slowly.", "hunger_saver", "minecraft:bread"),
        ],
        finishers=[
            A("Rewind to the Bookmark", "Turn back to the last page you marked: a powerful heal with a long regeneration.", "heal_self", "minecraft:clock",
              vfx="SPIRIT_AURA", amount=20, regen_seconds=14, cooldown=2700),
            A("Skip to the Ending", "A far dash that cuts down everything between you and the last page.", "dash_strike", "minecraft:ender_eye", vfx="THREAD_LINE",
              distance=12, damage=22, targets=8, cooldown=2400),
        ],
    ),
    Line(
        id="storyteller", name="Storyteller", franchise=F,
        desc="Words become weapons when the right person tells them. Weave a tale and the scenario bends: bolts of text, healing lullabies and a plot twist from the sky.",
        color="#F29BD3", background="dream", icon="minecraft:written_book",
        book_name="Volume of a Thousand Tellings", book_lore="A story is only alive while someone is telling it, and you never stop.",
        stances=[
            S("Tell It Softly", "A hushed, gentle telling: you regenerate slowly but your words hit less hard.", "minecraft:candle",
              regen=1.5, damage_pct=-0.15, vfx="SPIRIT_AURA"),
            S("Tell It Loud", "A thundering telling: faster and far more damaging, but you speak without cover.", "minecraft:note_block",
              damage_pct=0.25, speed_pct=0.05, armor=-3, vfx="MAGIC_CIRCLE"),
        ],
        stats=[
            T("Silver Tongue", "Fortune favours a good narrator: bonus luck.", "luck", 1, "minecraft:gold_nugget"),
            T("Narrator's Lungs", "A voice that carries: more maximum health.", "max_health", 4, "minecraft:goat_horn"),
            T("Steady Voice", "Nothing interrupts the tale: bonus knockback resistance.", "knockback_resistance", 0.1, "minecraft:bell"),
        ],
        arts=[
            A("Once Upon a Time", "A rapid volley of glowing words flies at the foe you are looking at.", "barrage", "minecraft:paper", vfx="MAGIC_CIRCLE", damage=3.5),
            A("And Then It Was Written", "A single beam of ink that pierces a line of foes.", "ray_shot", "minecraft:ink_sac", vfx="LIGHTNING_SPEAR"),
            A("Lullaby Verse", "A soft chorus heals you and nearby allies.", "heal_pulse", "minecraft:honey_bottle", vfx="SPIRIT_AURA"),
            A("Crescendo", "The tale swells and bursts outward in a ring of sound.", "ring_burst", "minecraft:firework_rocket", vfx="WIND_RING"),
            A("Cautionary Tale", "Tell a terrible ending to everything in front of you; they stagger weakened and sluggish.", "debuff_cone", "minecraft:wither_rose", effect="weakness", effect2="slowness", seconds2=5, vfx="SHADOW_POOL"),
            A("Heroic Chapter", "Narrate a hero's rise: you and your allies feel hasty.", "buff_aura", "minecraft:goat_horn", effect="haste", vfx="SPIRIT_AURA"),
            A("Plot Twist", "Name a place and after a short beat the story drops a comet on it.", "meteor_drop", "minecraft:fire_charge", vfx="FIRE_BURST", damage=13),
        ],
        passives=[
            P("Told and Retold", "Your kills give more experience.", "xp_bonus", "minecraft:experience_bottle"),
            P("Campfire Tale", "Allies near you slowly regenerate, listening.", "aura", "minecraft:campfire", effect="regeneration", target="allies"),
            P("Dramatic Beat", "Your blows sometimes hit critically at just the right moment.", "crit", "minecraft:jukebox"),
            P("Villain's Monologue", "You deal more damage to monsters, as every villain has to listen.", "dmg_vs", "minecraft:zombie_head", category="monster"),
            P("Feeding on Stories", "You heal for a share of the damage you deal.", "lifesteal", "minecraft:sweet_berries"),
            P("Fable's Shield", "You take less magic damage.", "resist", "minecraft:enchanted_book", damage_kind="magic"),
            P("Three-Act Structure", "Every few blows in a row hit much harder.", "combo", "minecraft:target"),
        ],
        finishers=[
            A("The Story That Never Ends", "A storm of word-bolts rains on the foe you look at.", "barrage", "minecraft:nether_star", vfx="MAGIC_CIRCLE",
              damage=9, shots=12, interval=2, cooldown=2400),
            A("Once Upon the Final Page", "Write the last scene: a huge meteor of ink lands on the spot you look at.", "meteor_drop", "minecraft:end_crystal", vfx="MAGIC_CIRCLE",
              damage=28, radius=7, cooldown=3000),
        ],
    ),
]

# --- 6 to 10
LINES += [
    Line(
        id="way_of_the_wind", name="Way of the Wind", franchise=F,
        desc="A swordsman's road that follows the breeze. Never stand still: dash, spin and fall on the enemy like a gale with a blade.",
        color="#7FD6A8", background="forest", icon="minecraft:feather",
        book_name="Scroll of the Passing Breeze", book_lore="The wind has no sheath, and it answers anyone who walks fast enough.",
        stances=[
            S("Breeze Stance", "A relaxed, drifting stance: quicker feet and a faint regeneration, but a lighter blade.", "minecraft:string",
              speed_pct=0.15, regen=0.5, damage_pct=-0.1, vfx="WIND_RING"),
            S("Gale Stance", "A reckless, raging stance: you are faster and cut harder, but a gust could knock you down.", "minecraft:wind_charge",
              speed_pct=0.3, damage_pct=0.1, armor=-4, vfx="WIND_TORNADO"),
        ],
        stats=[
            T("Light Step", "Your stride is a breath: bonus movement speed.", "movement_speed", 0.05, "minecraft:leather_boots", "mult"),
            T("Windborne Leap", "A spring in every step: a higher jump.", "jump_strength", 0.05, "minecraft:rabbit_foot"),
            T("Fall Like a Leaf", "You can drop farther without harm: a longer safe fall.", "safe_fall_distance", 1, "minecraft:feather"),
            T("Swift Blade Hand", "The blade follows the wind: bonus attack speed.", "attack_speed", 0.05, "minecraft:iron_sword", "mult"),
        ],
        arts=[
            A("Wind Step", "Dash forward in a streak of wind, cutting every foe you pass.", "dash_strike", "minecraft:sugar", vfx="WIND_SLASH"),
            A("Cyclone Dance", "Spin in place and slash everything around you several times.", "whirlwind", "minecraft:wind_charge", vfx="WIND_TORNADO"),
            A("Crescent of Wind", "A broad cone of cutting wind in front of you.", "cone_strike", "minecraft:iron_sword", vfx="WIND_SLASH"),
            A("Vanish on the Breeze", "Fade and reappear several blocks away, leaving only a ring of wind.", "blink", "minecraft:phantom_membrane", vfx="WIND_RING"),
            A("Descending Gale", "Leap up and crash down in a burst of air.", "leap_slam", "minecraft:elytra", vfx="WIND_RING"),
            A("Rolling Gust", "Send a low wave of wind rolling along the ground.", "ground_wave", "minecraft:white_wool", vfx="WIND_SLASH"),
            A("Rising Updraft", "Throw the foe in front of you into the air on an updraft.", "uppercut", "minecraft:fire_charge", vfx="WIND_SLASH"),
            A("Tailwind", "A gale at your back: you gain speed and haste for a while.", "buff_self", "minecraft:sugar", effect="speed", effect2="haste", amp=1, vfx="WIND_RING"),
        ],
        passives=[
            P("Wind at Your Back", "You deal more damage while sprinting.", "sprint_bonus", "minecraft:leather_boots"),
            P("Gathering Gale", "Blows in a row grow stronger as the gale builds.", "momentum", "minecraft:wind_charge"),
            P("Leaf in the Wind", "You sometimes drift out of the way of a blow.", "dodge", "minecraft:oak_leaves"),
            P("Feather Fall", "You take much less fall damage.", "fall_guard", "minecraft:feather"),
            P("Pushing Gust", "Your blows sometimes push the foe back on a puff of wind.", "knockback_on_hit", "minecraft:piston"),
            P("Cutting Draft", "Your blows sometimes strike critically.", "crit", "minecraft:shears"),
            P("Entangling Breeze", "Struck foes are sometimes slowed, caught in whirling air.", "on_hit_effect", "minecraft:cobweb", effect="slowness"),
            P("Light as Air", "You tire more slowly.", "hunger_saver", "minecraft:dried_kelp"),
        ],
        finishers=[
            A("Tempest Step", "Become the storm: a very long dash that tears through everything in its way.", "dash_strike", "minecraft:elytra", vfx="WIND_TORNADO",
              distance=14, damage=24, targets=10, cooldown=2400),
            A("Hundred-Gust Storm", "A towering whirlwind of blades of air around you, striking again and again.", "whirlwind", "minecraft:breeze_rod", vfx="WIND_TORNADO",
              damage=7, radius=6, hits=7, cooldown=2700),
        ],
    ),
    Line(
        id="regressor_hundred_lives", name="Regressor's Hundred Lives", franchise=F,
        desc="The weight of a hundred failed lives, remembered. You have seen every blow before, and you fight like someone who has already lost and learned.",
        color="#8E7CC3", background="void", icon="minecraft:clock",
        book_name="Chronicle of Returned Years", book_lore="Death is only the turn of a page, and he remembers every page.",
        stances=[
            S("Veteran's Patience", "Wait for the opening: you stand firm and slowly recover, but you do not hurry.", "minecraft:shield",
              armor=4, regen=0.8, speed_pct=-0.1, vfx="SPIRIT_AURA"),
            S("Last Regression Fury", "Everything you have left in one fight: hit far harder and crit, with almost no guard.", "minecraft:netherite_sword",
              damage_pct=0.3, crit_chance=0.1, armor=-4, vfx="SHADOW_POOL"),
        ],
        stats=[
            T("Lives of Experience", "A hundred lives of drill: bonus attack damage.", "attack_damage", 1, "minecraft:netherite_sword"),
            T("Hardened Soul", "A will that has died and returned: more maximum health.", "max_health", 6, "minecraft:totem_of_undying"),
            T("Scarred Hide", "Old wounds toughen you: bonus armor.", "armor", 1, "minecraft:iron_chestplate"),
            T("Unmoved Resolve", "You stand where you stand: bonus knockback resistance.", "knockback_resistance", 0.1, "minecraft:anvil"),
        ],
        arts=[
            A("Memory-Honed Cut", "A flawless cut learned over many lives, much stronger against the already wounded.", "execute", "minecraft:netherite_sword", vfx="WIND_SLASH"),
            A("Veteran's Counter", "For a few seconds a share of the damage you take is returned to the one who dealt it.", "ward_reflect", "minecraft:iron_sword", vfx="SPIRIT_AURA"),
            A("Cold Advance", "A swift straight dash that cuts through everything ahead.", "dash_strike", "minecraft:iron_boots", vfx="WIND_SLASH"),
            A("Remembered Weak Point", "A wide cone of precise cuts at every gap in the enemy's guard.", "cone_strike", "minecraft:stone_sword", vfx="WIND_SLASH"),
            A("Iron Resolve", "Steel yourself: a barrier of absorption hearts forms around you.", "shield", "minecraft:iron_chestplate", vfx="SPIRIT_AURA"),
            A("Sweep of Many Lives", "A spinning sword sweep that hits everything around you several times.", "whirlwind", "minecraft:chain", vfx="WIND_SLASH"),
            A("Retry the Scene", "Rewind your wounds a little: heal and regenerate.", "heal_self", "minecraft:clock", vfx="SPIRIT_AURA"),
        ],
        passives=[
            P("Not Dead Yet", "When nearly dead, you are briefly shielded.", "last_stand", "minecraft:totem_of_undying"),
            P("Harvest of Lives", "A kill heals you and grants a short resistance.", "kill_reward", "minecraft:bone", effect="resistance"),
            P("Seen This Before", "While your health is low, you take less damage.", "reduce_when", "minecraft:iron_helmet", condition="low_health"),
            P("Cornered Veteran", "While your health is low, you deal more damage.", "dmg_when", "minecraft:redstone", condition="low_health"),
            P("Memorised Pattern", "You sometimes avoid a blow whose pattern you remember.", "dodge", "minecraft:paper"),
            P("Practised Combo", "Every few blows in a row hit much harder.", "combo", "minecraft:iron_sword"),
            P("Scarred but Whole", "Your wounds close slowly over time.", "mending", "minecraft:honeycomb"),
        ],
        finishers=[
            A("Regression", "Return to the start of the scene: a large heal with a long regeneration.", "heal_self", "minecraft:recovery_compass",
              vfx="SPIRIT_AURA", amount=30, regen_seconds=16, cooldown=3000),
            A("Cut of a Hundred Lives", "A giant cone of memory-sharpened slashes, each one from a different life.", "cone_strike", "minecraft:netherite_sword", vfx="WIND_SLASH",
              damage=24, range=6.5, targets=9, half_angle=60, cooldown=2400),
        ],
    ),
    Line(
        id="black_heavenly_demon_sword", name="Black Heavenly Demon Sword", franchise=F,
        desc="A merciless sword style soaked in demonic qi. Every cut is a crescent of black light that drinks a little of whatever it touches.",
        color="#B3202A", background="blood", icon="minecraft:netherite_sword",
        book_name="Manual of the Black Heavenly Edge", book_lore="No sect taught it; it was taught by the dead who had tried to survive.",
        stances=[
            S("Demon's Stance", "Let the demon lead: a huge boost to your damage, but you drop all defence.", "minecraft:wither_skeleton_skull",
              damage_pct=0.3, armor=-4, vfx="SHADOW_POOL"),
            S("Heavenly Sheath", "Sheathe the edge behind a calm heaven: more armor and slow recovery, with a weaker blade.", "minecraft:iron_sword",
              armor=4, regen=0.6, damage_pct=-0.1, vfx="SPIRIT_AURA"),
        ],
        stats=[
            T("Black Edge", "A darker, sharper steel: bonus attack damage.", "attack_damage", 1, "minecraft:netherite_sword"),
            T("Demon Core", "The demonic core feeds your body: more maximum health.", "max_health", 4, "minecraft:wither_rose"),
            T("Sword Callus", "Hands and skin hardened by the blade: bonus armor.", "armor", 1, "minecraft:iron_chestplate"),
            T("Sword Qi Flow", "Qi through the wrists: bonus attack speed.", "attack_speed", 0.05, "minecraft:blaze_powder", "mult"),
        ],
        arts=[
            A("Black Crescent", "A wide cone of black sword qi in front of you.", "cone_strike", "minecraft:coal", vfx="SHADOW_POOL"),
            A("Demon-Splitting Wave", "Send a low black wave along the ground that cleaves everything in its path.", "ground_wave", "minecraft:obsidian", vfx="SHADOW_POOL"),
            A("Heavenly Rising Cut", "An upward slash that throws the foe in front of you into the air.", "uppercut", "minecraft:netherite_sword", vfx="WIND_SLASH"),
            A("Devouring Edge", "Cut a foe in front of you and drink some of the damage back as health.", "drain_bite", "minecraft:ghast_tear", vfx="SHADOW_POOL"),
            A("Descent of the Black Sword", "Leap high and slam down in a shock of black qi.", "leap_slam", "minecraft:anvil", vfx="SHADOW_POOL"),
            A("Qi Rot", "A cone of rotting demonic qi withers everyone it touches.", "debuff_cone", "minecraft:wither_rose", effect="wither", effect2="weakness", seconds2=5, vfx="POISON_BREATH"),
            A("Three Hundred Slashes", "A single cut that jumps from foe to foe in a chain of black light.", "chain_hit", "minecraft:iron_sword", vfx="SHADOW_POOL"),
        ],
        passives=[
            P("Sword Heart", "You deal more damage while holding a sword.", "dmg_when", "minecraft:diamond_sword", condition="holding_sword"),
            P("Demonic Qi", "Struck foes are sometimes withered by lingering qi.", "on_hit_effect", "minecraft:wither_skeleton_skull", effect="wither"),
            P("Blood Qi Circulation", "You heal for a share of the damage you deal.", "lifesteal", "minecraft:redstone"),
            P("Killing Intent", "Your blows sometimes strike critically.", "crit", "minecraft:flint"),
            P("Feast of the Fallen", "A kill heals you and grants brief strength.", "kill_reward", "minecraft:bone", effect="strength"),
            P("Qi-Hardened Body", "You take less wither damage.", "resist", "minecraft:obsidian", damage_kind="wither"),
            P("Overwhelming Edge", "Your blows sometimes shove the foe back.", "knockback_on_hit", "minecraft:iron_axe"),
        ],
        finishers=[
            A("Black Heavenly Demon Slash", "A huge, long wave of black qi that runs far across the ground and flings foes aside.", "ground_wave", "minecraft:netherite_sword",
              vfx="SHADOW_POOL", damage=26, length=16, width=4, knockback=1.2, cooldown=2400),
            A("Ten Thousand Demon Dance", "Become a storm of black blades: a spinning flurry that hits everyone around you.", "whirlwind", "minecraft:wither_skeleton_skull",
              vfx="SHADOW_POOL", damage=7, radius=6, hits=8, cooldown=2700),
        ],
    ),
    Line(
        id="breaking_the_sky_sword", name="Breaking the Sky Sword", franchise=F,
        desc="A sword that is swung at the heavens. One cut opens a seam in the cloud, and what stands beneath it learns what 'heavy' means.",
        color="#6EC6FF", background="storm", icon="minecraft:diamond_sword",
        book_name="Treatise on the Parted Heavens", book_lore="The sky is only the biggest thing a sword has not yet cut.",
        stances=[
            S("Skyward Guard", "Plant the blade upright against the sky: armor and strong knockback resistance, but a slower body.", "minecraft:shield",
              armor=3, knockback_res=0.5, speed_pct=-0.05, vfx="WIND_RING"),
            S("Heaven-Splitting Grip", "Both hands, all the weight: much heavier blows, a slower stride and a loose guard.", "minecraft:iron_axe",
              damage_pct=0.3, speed_pct=-0.1, armor=-2, vfx="LIGHTNING_SPEAR"),
        ],
        stats=[
            T("Sky-Piercing Resolve", "Cut through the clouds: bonus attack damage.", "attack_damage", 1, "minecraft:diamond_sword"),
            T("Heaven's Breath", "The air above feeds you: more maximum health.", "max_health", 4, "minecraft:golden_apple"),
            T("Cloud Walker", "Light on your feet before the leap: bonus movement speed.", "movement_speed", 0.04, "minecraft:white_wool", "mult"),
        ],
        arts=[
            A("Breaking the Sky Slash", "A thin, brilliant beam of sword light across the sky in front of you, piercing through foes.", "ray_shot", "minecraft:end_rod", vfx="LIGHTNING_SPEAR", range=20),
            A("Step Above the Clouds", "Cross the distance in one step and reappear a good way ahead.", "blink", "minecraft:ender_pearl", vfx="SPATIAL_RIFT", distance=10),
            A("Fallen Heaven", "Mark the spot you look at; after a short beat a piece of sky falls on it.", "meteor_drop", "minecraft:fire_charge", vfx="LIGHTNING_SPEAR", damage=14),
            A("Heaven's Verdict", "A blow that hits much harder the weaker the foe already is.", "execute", "minecraft:iron_sword", vfx="LIGHTNING_SPEAR"),
            A("Cleaving the Earth Line", "Drive the sword forward and send a straight cutting wave along the ground.", "ground_wave", "minecraft:iron_pickaxe", vfx="STONE_SPIKES"),
            A("Leap Beyond the Clouds", "Jump into the air and plunge back down onto the foes below.", "leap_slam", "minecraft:elytra", vfx="LIGHTNING_SPEAR"),
            A("Resolve of the Blade", "Steel your body: strength and resistance for a while.", "buff_self", "minecraft:iron_chestplate", effect="strength", effect2="resistance", vfx="SPIRIT_AURA"),
        ],
        passives=[
            P("Above the Clouds", "You deal more damage while airborne.", "dmg_when", "minecraft:elytra", condition="airborne"),
            P("Storm-Tempered", "You take less lightning damage.", "resist", "minecraft:lightning_rod", damage_kind="lightning"),
            P("Sky-Splitting Insight", "Your blows sometimes strike critically.", "crit", "minecraft:amethyst_shard"),
            P("Gathering Thunder", "Blows in a row grow stronger as the storm builds.", "momentum", "minecraft:copper_ingot"),
            P("Landing from Above", "You take far less fall damage.", "fall_guard", "minecraft:feather"),
            P("Cutting Down the Weak", "You deal much more damage to nearly dead foes.", "execute_threshold", "minecraft:iron_sword"),
            P("Calm Eye of the Storm", "You tire more slowly.", "hunger_saver", "minecraft:bread"),
        ],
        finishers=[
            A("Breaking the Sky", "The legendary cut: a bright beam that slices a great line through the sky and every foe along it.", "ray_shot", "minecraft:nether_star",
              vfx="LIGHTNING_SPEAR", damage=30, range=30, pierce=5, cooldown=2700),
            A("The Sky Falls", "The torn heavens come down on the place you look at in a huge impact.", "meteor_drop", "minecraft:end_crystal", vfx="LIGHTNING_SPEAR",
              damage=32, radius=8, cooldown=3000),
        ],
    ),
    Line(
        id="tyrant_dark_mirror", name="Tyrant's Dark Mirror", franchise=F,
        desc="The author who writes the world knows how to turn it round. Mirrors repeat strikes, bounce pain back and trap foes in their own reflections.",
        color="#6A5ACD", background="frost", icon="minecraft:echo_shard",
        book_name="Folio of Reflected Pages", book_lore="Nobody writes villains as well as someone who has been one in every draft.",
        stances=[
            S("Reflecting Surface", "Hold the mirror flat: sturdier and hard to push, but your own blows lose weight.", "minecraft:glass",
              armor=4, knockback_res=0.3, damage_pct=-0.1, vfx="MIRROR_PANE"),
            S("Shattering Surface", "Crack the glass and let the splinters fly: hit harder and crit more, with almost no defence.", "minecraft:prismarine_shard",
              damage_pct=0.25, crit_chance=0.1, armor=-3, vfx="MIRROR_PANE"),
        ],
        stats=[
            T("Tyrant's Wit", "Quick as a sharp reflection: bonus luck.", "luck", 1, "minecraft:amethyst_shard"),
            T("Glass Reflexes", "Fast and brittle: bonus attack speed.", "attack_speed", 0.05, "minecraft:feather", "mult"),
            T("Mirror-Bright Will", "Your will does not crack: more maximum health.", "max_health", 4, "minecraft:diamond"),
        ],
        arts=[
            A("Echo of the Strike", "One strike bounces from foe to foe through a series of mirror images.", "chain_hit", "minecraft:iron_sword", vfx="MIRROR_PANE"),
            A("Mirror That Answers", "For a few seconds a share of the damage taken is reflected back to the attacker.", "ward_reflect", "minecraft:glass_pane", vfx="MIRROR_PANE"),
            A("Step Through the Glass", "Step into one mirror and out of another a few blocks away.", "blink", "minecraft:ender_eye", vfx="MIRROR_PANE"),
            A("Hall of Panes", "Panes of glass burst out in a ring, damaging and shoving everything around you.", "ring_burst", "minecraft:tinted_glass", vfx="MIRROR_PANE"),
            A("Frame the Culprit", "Fix a distant foe in a mirror frame; it takes extra damage while framed.", "mark", "minecraft:painting", vfx="MIRROR_PANE"),
            A("Trapped in Reflection", "Freeze the creatures around you in their own reflections for a moment.", "stun_ring", "minecraft:blue_ice", vfx="MIRROR_PANE"),
            A("Splinter Volley", "A rapid volley of glass shards at the foe you are looking at.", "barrage", "minecraft:prismarine_shard", vfx="MIRROR_PANE"),
        ],
        passives=[
            P("Mirrored Pain", "Part of the damage you take is thrown back at the attacker.", "thorns", "minecraft:glass"),
            P("Facet Strike", "Your blows sometimes strike critically, finding a bright edge.", "crit", "minecraft:quartz"),
            P("Dazzling Reflection", "Whoever hits you may be blinded by the glare.", "on_hurt_retaliate", "minecraft:glowstone_dust", effect="blindness"),
            P("Pristine Glass", "While at full health you deal more damage.", "dmg_when", "minecraft:glass_bottle", condition="full_health"),
            P("Author's Instinct", "Your kills give more experience.", "xp_bonus", "minecraft:experience_bottle"),
            P("Reflected Blow", "You sometimes avoid a hit completely, as if it struck a reflection.", "dodge", "minecraft:ender_eye"),
            P("Spell-Turning Glass", "You take less magic damage.", "resist", "minecraft:enchanted_book", damage_kind="magic"),
        ],
        finishers=[
            A("Hall of Infinite Mirrors", "A thousand reflections burst outward at once and batter everyone around you.", "ring_burst", "minecraft:tinted_glass",
              vfx="MIRROR_PANE", damage=24, radius=7, targets=12, cooldown=2400),
            A("Sword of a Thousand Reflections", "A single cut that rebounds through many mirrors and many foes.", "chain_hit", "minecraft:diamond_sword", vfx="MIRROR_PANE",
              damage=20, jumps=8, jump_range=7, cooldown=2700),
        ],
    ),
]
