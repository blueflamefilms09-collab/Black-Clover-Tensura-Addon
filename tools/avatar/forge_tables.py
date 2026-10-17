"""Why: the Soul Forge's shipped data (30 Magic Aptitudes, 42 skills) is hand-balanced as TABLES here, in one place, and two scripts read them:
gen_forge_data.py writes data/nusmp/avatar_aptitudes/*.json and data/nusmp/avatar_skills/*.json, gen_aptitude_icons.py draws one circular icon per
aptitude. Keeping the numbers in one table means a rebalance is one edit and a regeneration, and the JUnit test that reads the shipped JSON still checks
every rule (real ids, real magic names, the tax ladder) so the table cannot drift from what ships.

Sources of the ids (nothing here is invented):
  * Tensura skills: the owner's Tensura 2.0.1.3 id reference, src/test/resources/com/newuniverse/nusmp/charter/tensura_skill_ids.txt (358 ids), and the
    API dump's registry class io.github.manasmods.tensura.registry.skill.UniqueSkills for which of them are Unique skills.
  * nusmp skills: SKILLS.register("...") lines of src/main/java/com/newuniverse/nusmp/skill/NUSkills.java (the mod's own Ultimate evolutions).
  * magic: the lower-cased names of the MagicType enum (src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java), colours from the grimoire books
    (super(MagicType.X, 0xFF......) in src/main/java/com/newuniverse/nusmp/book/*Book.java), lightened where the book colour is too dark for the UI.

Tax ladder (percent of the creator's maximum magicule taken for good when the aptitude is picked BY HAND; a rolled one is free):
COMMON 0, UNCOMMON 3-4, RARE 7-10, EPIC 12-15, LEGENDARY 20-25.
"""

# (id, name, rarity, tax_percent, magic, skills, tint, glyph, description)
APTITUDES = [
    # ---- COMMON: the four elements and the garden. Free to pick by hand.
    ("fire", "Fire", "COMMON", 0, "flame", ["tensura:flame_manipulation", "tensura:fire_ball", "tensura:fire_lance"], 0xFF6A1E, "flame",
     "Flame that answers the first call. Cheap, honest and always welcome."),
    ("water", "Water", "COMMON", 0, "water", ["tensura:water_manipulation", "tensura:water_blade", "tensura:water_cutter"], 0x4FA8FF, "drop",
     "Tide and rain. Calm until it is not."),
    ("wind", "Wind", "COMMON", 0, "wind", ["tensura:wind_manipulation", "tensura:wind_blade", "tensura:wind_cutter"], 0x8CFFC2, "swirl",
     "A quick, clean current that carries steel and spells alike."),
    ("earth", "Earth", "COMMON", 0, "earth", ["tensura:earth_manipulation", "tensura:earth_wall", "tensura:stone_shot"], 0xB08850, "mountain",
     "Stone and soil: patient, heavy, hard to move."),
    ("plant", "Plant", "COMMON", 0, "plant", ["tensura:self_regeneration", "tensura:poison"], 0x6CFF7A, "leaf",
     "Roots and thorns that mend friends and snare foes."),
    # ---- UNCOMMON
    ("ice", "Ice", "UNCOMMON", 3, "ice", ["tensura:icicle_lance", "tensura:ice_wall", "tensura:freeze"], 0x9FE6FF, "snow",
     "Frost that slows the world to a walk."),
    ("lightning", "Lightning", "UNCOMMON", 3, "lightning", ["tensura:lightning_manipulation", "tensura:thunder_lance", "tensura:thunder_orb"], 0xFFE14A, "bolt",
     "Speed you can see for one instant."),
    ("light", "Light", "UNCOMMON", 4, "light", ["tensura:light", "tensura:solar_beam", "tensura:holy_attack_resistance"], 0xFFF2A8, "sun",
     "Bright, holy and hard to hide from."),
    ("dark", "Dark", "UNCOMMON", 4, "dark", ["tensura:darkness", "tensura:darkness_cannon", "tensura:dark_cube"], 0x8E62D0, "moon",
     "Shadow that keeps its own counsel."),
    ("steel", "Steel", "UNCOMMON", 3, "steel", ["tensura:steel_strength", "tensura:body_armor", "tensura:strengthen_body"], 0xB8C2CC, "gear",
     "A body forged to take the blow."),
    ("poison", "Poison", "UNCOMMON", 3, "poison", ["tensura:poison", "tensura:paralysis", "tensura:poisonous_breath"], 0x8AD04A, "skull",
     "Slow, certain and unfair."),
    ("battle_aura", "Battle Aura", "UNCOMMON", 3, "", ["tensura:battlewill", "tensura:aura_sword", "tensura:aura_slash"], 0xFFB04A, "blade",
     "Aura drawn down the blade: the fighter's road, no grimoire needed."),
    # ---- RARE
    ("mercury", "Mercury", "RARE", 7, "mercury", ["tensura:liquidize", "tensura:molecular_manipulation"], 0xC9D1DB, "ripple",
     "Liquid silver that is a shield one moment and a spear the next."),
    ("mirror", "Mirror", "RARE", 8, "mirror", ["tensura:body_double", "tensura:doppelganger", "tensura:mirage"], 0xDDEEFF, "pane",
     "Two of you, and one of them is lying."),
    ("sealing", "Sealing", "RARE", 8, "sealing", ["tensura:magic_barrier", "tensura:ranged_barrier", "tensura:chant_annulment"], 0xE8C26A, "lock",
     "Circles that close, hold and silence."),
    ("thread", "Thread", "RARE", 8, "thread", ["tensura:sticky_steel_thread", "tensura:shadow_bind", "tensura:curse_bind"], 0xFF3355, "web",
     "Red lines of fate pulled tight."),
    ("blood", "Blood", "RARE", 8, "blood", ["tensura:blood_mist", "tensura:drain", "tensura:self_regeneration"], 0xC0102A, "bloodcell",
     "What is spilled is spent; what is drained is kept."),
    ("gravity", "Gravity", "RARE", 9, "gravity", ["tensura:gravity_manipulation", "tensura:gravity_field", "tensura:gravity_flight"], 0x9A5CE8, "orbit",
     "Weight is only a suggestion to you."),
    ("spatial", "Spatial", "RARE", 10, "spatial", ["tensura:spatial_manipulation", "tensura:spatial_motion", "tensura:spatial_storage"], 0xB088FF, "portal",
     "Doors where there are no doors."),
    ("spirit_whisperer", "Spirit Whisperer", "RARE", 7, "", ["tensura:summon_medium_elemental", "tensura:summon_greater_elemental", "tensura:spiritual_attack_resistance"], 0x7CF0D0, "wisp",
     "The spirits of the air and flame answer when you call."),
    ("sages_mind", "Sage's Mind", "RARE", 7, "", ["tensura:analytical_appraisal", "tensura:thought_acceleration", "tensura:search_enemy"], 0x5CD8FF, "eye",
     "A mind that reads the fight before it starts."),
    # ---- EPIC
    ("creation", "Creation", "EPIC", 14, "creation", ["tensura:molecular_manipulation", "tensura:magic_wall"], 0xFFD0F0, "spark",
     "Make what you need out of what is there."),
    ("dream", "Dream", "EPIC", 14, "dream", ["tensura:sleep_mist", "tensura:hypnos", "tensura:mirage"], 0xC8A8FF, "stars",
     "A world inside a world, and you hold the door."),
    ("soul", "Soul", "EPIC", 15, "soul", ["tensura:possession", "tensura:spiritual_attack_resistance"], 0x8FFFE0, "flameeye",
     "Hands that reach past the body."),
    ("dragon_blood", "Dragon Blood", "EPIC", 14, "", ["tensura:dragon_skin", "tensura:dragon_eye", "tensura:dragon_mode"], 0xFF7A3C, "scale",
     "Old fire in young veins."),
    ("overlords_presence", "Overlord's Presence", "EPIC", 13, "", ["tensura:demon_lord_haki", "tensura:majesty", "tensura:coercion"], 0xD84AFF, "crown",
     "Weak wills bow before you speak."),
    ("dice", "Dice", "EPIC", 12, "dice", ["tensura:unpredictability"], 0x4AE8B0, "d6",
     "Luck is a currency, and you are rich."),
    ("world_tree", "World Tree", "EPIC", 15, "world_tree", ["tensura:healing_rain", "tensura:self_regeneration", "tensura:healthcare"], 0x3CE08A, "tree",
     "Roots to the bottom of the world, a crown above the clouds."),
    # ---- LEGENDARY: the two the owner named as the heaviest.
    ("time", "Time", "LEGENDARY", 20, "time", ["tensura:thought_acceleration", "tensura:ultra_instinct"], 0xF5D76E, "clock",
     "The one thing nobody gets back, held in your hand."),
    ("anti_magic", "Anti-Magic", "LEGENDARY", 25, "anti_magic", ["tensura:magic_nullification", "tensura:magic_resistance", "tensura:anti_magic_area", "tensura:magic_jamming"],
     0xB04AD8, "slash", "A blade that cuts spells in half. The heaviest gift there is."),
]

# (file, id, kind, name, ep_tax_percent, min_ep, tint, description)
SKILLS = [
    # ---- Tensura Unique skills (the registry class UniqueSkills, minus the ones that steal or erase other skills)
    ("great_sage", "tensura:great_sage", "unique", "Great Sage", 8, 1500, 0x5CD8FF, "A mind that answers before the question ends."),
    ("predator", "tensura:predator", "unique", "Predator", 8, 1500, 0x8A3CFF, "Devour, learn, keep."),
    ("analyst", "tensura:analyst", "unique", "Analyst", 4, 600, 0x5CD8FF, "Every weakness has a number."),
    ("observer", "tensura:observer", "unique", "Observer", 4, 600, 0x7CF0B0, "Sees what others walk past."),
    ("researcher", "tensura:researcher", "unique", "Researcher", 4, 600, 0x9FE6FF, "Learns faster than the lesson is taught."),
    ("seer", "tensura:seer", "unique", "Seer", 5, 800, 0xC8A8FF, "A glimpse of the next heartbeat."),
    ("seeker", "tensura:seeker", "unique", "Seeker", 4, 600, 0xFFE14A, "Finds the thing that does not want to be found."),
    ("sniper", "tensura:sniper", "unique", "Sniper", 4, 600, 0xFFB04A, "One shot, one place."),
    ("thrower", "tensura:thrower", "unique", "Thrower", 3, 400, 0xFFB04A, "Anything becomes a weapon."),
    ("traveler", "tensura:traveler", "unique", "Traveler", 5, 800, 0x8CFFC2, "No road is too long."),
    ("healer", "tensura:healer", "unique", "Healer", 5, 800, 0x7CF0B0, "Hands that close wounds."),
    ("guardian", "tensura:guardian", "unique", "Guardian", 5, 800, 0xE8C26A, "Stands where the blow would land."),
    ("fighter", "tensura:fighter", "unique", "Fighter", 4, 500, 0xFF6A6A, "A body that remembers every fight."),
    ("martial_master", "tensura:martial_master", "unique", "Martial Master", 6, 1000, 0xFF8A4A, "Technique past strength."),
    ("berserker", "tensura:berserker", "unique", "Berserker", 6, 1000, 0xFF3355, "Rage with a purpose."),
    ("berserk", "tensura:berserk", "unique", "Berserk", 6, 1000, 0xC0102A, "Pain becomes power."),
    ("commander", "tensura:commander", "unique", "Commander", 6, 1000, 0xFFD060, "Allies fight as one."),
    ("shadow_striker", "tensura:shadow_striker", "unique", "Shadow Striker", 6, 1000, 0x8E62D0, "Strikes from where you were not looking."),
    ("severer", "tensura:severer", "unique", "Severer", 7, 1200, 0xDDEEFF, "Cuts what should not be cut."),
    ("reaper", "tensura:reaper", "unique", "Reaper", 7, 1200, 0x6E5AA8, "Takes what is already lost."),
    ("reflector", "tensura:reflector", "unique", "Reflector", 6, 1000, 0xDDEEFF, "What is thrown at you returns."),
    ("survivor", "tensura:survivor", "unique", "Survivor", 5, 800, 0x9CC0A0, "Still standing."),
    ("unyielding", "tensura:unyielding", "unique", "Unyielding", 5, 800, 0xB8C2CC, "Does not bend, does not break."),
    ("royal_beast", "tensura:royal_beast", "unique", "Royal Beast", 8, 1500, 0xC98A4A, "The beast in you wears a crown."),
    ("musician", "tensura:musician", "unique", "Musician", 3, 400, 0xFFD0F0, "A tune that steadies friends."),
    ("mathematician", "tensura:mathematician", "unique", "Mathematician", 4, 600, 0xFFFFFF, "The world is a sum, and you do the sum first."),
    ("chef", "tensura:chef", "unique", "Chef", 2, 200, 0xFFB04A, "A good meal is a good spell."),
    # ---- the Tensura sins: the owner's example "Gluttony". Their category is read from the registry at runtime (Unique or Ultimate), this is only the hint.
    ("envy", "tensura:envy", "ultimate", "Envy", 12, 3000, 0x3CE08A, "Wants what others have, and takes the shape of it."),
    ("gluttony", "tensura:gluttony", "ultimate", "Gluttony", 14, 3500, 0x8A3CFF, "Hunger that never fills, strength that never ends."),
    ("greed", "tensura:greed", "ultimate", "Greed", 12, 3000, 0xE8C468, "Everything within reach is already yours."),
    ("lust", "tensura:lust", "ultimate", "Lust", 12, 3000, 0xFF8AD8, "Desire as a weapon."),
    ("pride", "tensura:pride", "ultimate", "Pride", 14, 3500, 0xFFD060, "Will that refuses to kneel."),
    ("sloth", "tensura:sloth", "ultimate", "Sloth", 12, 3000, 0x9CC0A0, "Rest so deep the world waits for you."),
    ("wrath", "tensura:wrath", "ultimate", "Wrath", 14, 3500, 0xFF3355, "Anger given a shape and a target."),
    # ---- this mod's own Ultimate skills (SkillType.ULTIMATE in NUSkills): Tensura 2.0.1.3 ships no Ultimate skill ids of its own
    ("lord_of_summons", "nusmp:lord_of_summons", "ultimate", "Lord of Summons", 16, 4000, 0x9A7CFF, "Every summon obeys, every gate opens."),
    ("goddess_of_faith", "nusmp:goddess_of_faith", "ultimate", "Goddess of Faith", 16, 4000, 0xFFF2A8, "Belief made into strength."),
    ("heros_argonaut", "nusmp:heros_argonaut", "ultimate", "Hero's Argonaut", 15, 3500, 0xFFD060, "A hero's growth with no ceiling."),
    ("hell_conqueror", "nusmp:hell_conqueror", "ultimate", "Hell Conqueror", 15, 3500, 0xFF6A1E, "Walks through fire and keeps the flame."),
    ("immortal_will", "nusmp:immortal_will", "ultimate", "Immortal Will", 15, 3500, 0xB8C2CC, "Refuses the end."),
    ("adolla_burst", "nusmp:adolla_burst", "ultimate", "Adolla Burst", 16, 4000, 0xFF8A4A, "The flame of the other world."),
    ("black_wings", "nusmp:black_wings", "ultimate", "Black Wings", 14, 3000, 0x4A3A6A, "Direction, bent to your will."),
    ("gold_experience_requiem", "nusmp:gold_experience_requiem", "ultimate", "Gold Experience Requiem", 18, 5000, 0xFFD060, "Life, and the return to zero."),
]
