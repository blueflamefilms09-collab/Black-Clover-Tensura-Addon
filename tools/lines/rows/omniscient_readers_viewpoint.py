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
