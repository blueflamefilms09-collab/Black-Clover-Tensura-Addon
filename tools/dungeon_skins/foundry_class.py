"""Why: which of the Mob Foundry's 400 mobs are PEOPLE (and so go on the player model) and which are creatures that keep their own model, and which family of wear / posture each person has.
The owner's rule is "humanoid NPCs and bosses go on the player model; slimes, beasts, dragons, spirits and wisps stay on their own models", so the line is drawn here, once, as data:

  * the factions `slime` and `insect` stay (a gelatinous cube, a giant spider and a swarm of moths are not people);
  * `LEGACY` lists the other mobs whose concept is a creature, a spirit or a wisp (a dragon, an owlbear, a roper, a will-o'-wisp, a treant, a cursed spirit ...) with the reason;
  * every other mob is a person in the roster's own terms (a human, a goblin, an orc, an undead knight, a demon, an angel, a golem built like a man, a dragon-kin duelist ...): the art kit
    already built each of them as a person with horns / wings / a tail where the sheet says so, and the player model takes that over.

Pure Python (no numpy, no Pillow) so the guard and the tests can import it. The Java side does not read this: it asks the looks table (`looks_dungeon.json`) whether `foundry_<id>` exists.
"""
import re

LEGACY_FACTIONS = ("slime", "insect")

# id -> why it keeps the old model
LEGACY = {
    # beasts and monsters with a body that is not a person's
    "hollow_gnawer_pup": "a pup", "bramble_pup": "a pup", "ember_fox_kit": "a fox kit", "pit_bat_swarmlord": "a swarm of stirges",
    "black_wool_shepherd_dog": "a dog", "dire_rat_king_warren_lord": "a rat king", "rust_monster_scrapper": "a rust monster",
    "roper_cavern_strangler": "a roper", "gravemaw_wurm_corpse_rider": "a purple worm", "grinning_mimic_pretender": "a mimic",
    "behemoth_eater_of_floors": "a behemoth", "thornhide_owlbear_brawler": "an owlbear", "griffon_scout": "a griffon", "sphinx_riddle_warden": "a sphinx",
    "lammasu_temple_guardian": "a lammasu", "couatl_herald": "a couatl", "ki_rin_thunder_stag": "a ki-rin", "mossback_stag_lord": "a stag",
    "thunderhead_roc_patriarch": "a roc", "divine_beast_zephyr_hawk": "a divine beast", "sage_mode_toad_elder": "a toad",
    "lotus_pond_healer_frog": "a frog", "sanctum_moogle_bat_courier": "a moogle", "ember_bat_flameling_swarm_king": "a hellcat swarm",
    "vault_gargoyle_gnawer": "a small gargoyle", "cobblestone_gargoyle_pup": "a small gargoyle",
    # dragons and their kin that are dragons
    "black_dragon_mire_lord": "a dragon", "ancient_black_dragon_of_the_drowned_deep": "a dragon", "tarrasque_shell_walker": "the tarrasque",
    "crumb_trail_dragonet": "a dragonet", "pale_wyrm_of_the_frozen_hells": "a dragon", "sin_wyrm_of_greed_unending": "a wyrm",
    "tailed_calamity_of_the_sealed_valley": "a tailed beast", "dragon_god_of_the_sunless_sky": "a dragon god", "elder_dragon_dawn_warden": "an elder dragon",
    "ancient_gold_dragon_hierophant": "a dragon", "dragon_lord_gate_defender": "a dragon lord",
    # spirits, wisps, fey, plants
    "hearth_sprite": "a sprite", "wisp_lantern_fae": "a wisp", "will_o_wisp_lampwright": "a wisp", "stained_glass_sparrow": "a bird spirit",
    "pixie_court_rapier": "a pixie", "seed_pod_dryad": "a dryad", "cherry_blossom_dryad_queen": "a dryad", "heartwood_treant_warden": "a treant",
    "rotwood_treant_blightcaller": "a blighted treant", "verdant_warden_of_the_seed_vault": "a shambling mound", "ancestor_spirit_grove_mother": "a spirit",
    "great_spirit_of_the_four_leaf_crown": "a spirit", "spirit_guardian_of_the_four_leaf_altar": "a spirit", "pillar_ignition_warden": "a pillar of fire",
    "pillar_of_the_sunlit_inferno": "a pillar of fire", "cursed_spirit_grunt": "a cursed spirit", "cursed_spirit_grade_three": "a cursed spirit",
    "cursed_spirit_grade_two": "a cursed spirit", "cursed_spirit_special_grade": "a cursed spirit", "special_grade_curse_of_the_ninth_hour": "a cursed spirit",
    "kitchen_knife_poltergeist": "a poltergeist", "whisper_gallery_phantom": "a phantom",
}


def is_person(row):
    """True when the mob goes on the player model."""
    return row["faction"] not in LEGACY_FACTIONS and row["id"] not in LEGACY


def partition(rows):
    """(persons, creatures) of the roster rows."""
    persons, creatures = [], []
    for r in rows:
        (persons if is_person(r) else creatures).append(r)
    return persons, creatures


def legacy_reason(row):
    if row["faction"] in LEGACY_FACTIONS:
        return "faction " + row["faction"]
    return LEGACY.get(row["id"], "")


# ------------------------------------------------------------------------------------------------ family: how a person wears and carries itself
_FAMILY_RULES = (
    # (family, source regex, faction set, id regex)
    ("goblin", r"goblin|kobold|hobgoblin|bugbear", {"goblin"}, r"goblin|kobold"),
    ("orc", r"\borc\b|ogre|troll|giant|titan|gnoll|minotaur", {"ogre"}, r"orc|ogre|troll|giant|titan|gnoll"),
    ("skeleton", r"skeleton|skeletal|lich|death knight|revenant|wight|mummy", set(), r"bone|skull|lich|skeleton|revenant|wight|mummy|death_knight"),
    ("rot", r"zombie|ghoul|ghast", set(), r"ghoul|zombie|ghast|shambling|rotten|carrion"),
    ("spectre", r"banshee|wraith|shadow|vampire|ghost|nightwalker", set(), r"banshee|wraith|shade|vampire|phantom"),
    ("infernal", r"imp|quasit|devil|demon|balor|marilith|succubus|pit fiend|fallen angel|hell|gate fiend|archdevil", {"demon"}, r"imp|devil|demon|balor|hell|succubus"),
    ("holy", r"angel|deva|planetar|solar|seraph|paladin|aasimar|cleric|monk|couatl", set(), r"angel|seraph|deva|planetar|solar|paladin|saint|holy|radiant|dawn|celestial"),
    ("construct", r"golem|animated|modron|armor|scarecrow|gargoyle", {"construct"}, r"golem|colossus|sentinel|automaton"),
    ("aquatic", r"kuo|aboleth|mind flayer|sahuagin|merfolk|sea|drowned", {"aquatic"}, r"drowned|tidal|sea|marsh|naiad|siren"),
    ("fey", r"elf|eladrin|drow|fey|druid|satyr|ranger|dryad", set(), r"elf|drow|druid|satyr"),
)

# families whose members stoop forward (the player model's back bends and the head juts), and families that walk with the arms held out in front when they hunt
STANCE = {"goblin": "hunch", "rot": "zombie", "skeleton": "", "spectre": "", "infernal": "", "orc": "", "holy": "", "construct": "", "aquatic": "hunch", "fey": ""}


def family_of(row):
    """The family of a roster row ('' for an ordinary person): decides the wear recipe of the skin and the stance of the look."""
    src = row["source"].lower()
    rid = row["id"]
    for fam, sre, facs, ire in _FAMILY_RULES:
        if row["faction"] in facs or re.search(sre, src) or re.search(ire, rid):
            return fam
    return ""


def stance_of(row):
    return STANCE.get(family_of(row), "")
