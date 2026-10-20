"""The 24 God grade worn relics of the slice `god_worn` (visual upgrade 1.102.0): id, slot, look, palette (main, trim, glow) and the lore line the model is built from.

Palette and look are copied from the RelicDef of each item (src/main/java/com/newuniverse/nusmp/relics/wave/Wave*.java); a test (GodWornUpgradeTest) keeps this list and the
catalog in step. `anchors` says where the worn pieces hang: the arm and leg pieces come as a pair (right + mirrored left), everything else is one piece on the body or head.
"""

# id, display name, slot, look, (main, trim, glow), lore
ITEMS = [
    # ---- BACK
    ("flammes_founder_cloak", "Flamme's Founder Cloak", "BACK", "CLOAK", (0xF0E8D8, 0xC8A860, 0xFFF0C0),
     "A cloak of the founder of human magic; a spell that has no name, always the right spell."),
    ("mantle_of_the_overlord", "Mantle of the Overlord", "BACK", "CLOAK", (0x1A1020, 0x9A2A3A, 0xFF5A7A),
     "A mantle with more folds than cloth; a faint courteous laugh."),
    ("pactbreaker_shroud", "Pactbreaker Shroud", "BACK", "CLOAK", (0x160A24, 0xA04AFF, 0xD8A0FF),
     "The black between two pacts; no contract holds on the wearer."),
    ("shadow_monarchs_mantle", "Shadow Monarch's Mantle", "BACK", "CLOAK", (0x0A0814, 0x5A2A9A, 0xA060FF),
     "A darkness that stands up when its wearer does; fallen foes rise to it."),
    ("shadows_slime_cloak", "Shadow's Slime Cloak", "BACK", "CLOAK", (0x12101A, 0x4A3AA8, 0x8A70FF),
     "A living violet slime that is a coat, a sword and a very good listener."),
    ("silent_winter_mantle", "Silent Winter Mantle", "BACK", "MANTLE", (0x24425C, 0x9FE3FF, 0xCFF4FF),
     "A mantle of woven hoarfrost from the Archon's cathedral."),
    ("six_faced_dragons_scale", "Six-Faced Dragon's Scale", "BACK", "MANTLE", (0x4A2A2A, 0xD8B050, 0xFF9A4A),
     "One scale of a dragon-god with six faces and no patience."),
    ("urek_mazinos_chain_mantle", "Urek Mazino's Chain Mantle", "BACK", "MANTLE", (0x2A1A1A, 0xC8A030, 0xFFD860),
     "A heavy mantle of chains with a very heavy name."),
    # ---- BELT
    ("excaliburs_scabbard_avalon", "Excalibur's Scabbard Avalon", "BELT", "BELT", (0x2A6A5A, 0xD8C060, 0xA0FFD8),
     "An ornate scabbard of green and gold; its owner is not hurt where it is worn, and does not age."),
    ("silvered_sash_of_reflections", "Silvered Sash of Reflections", "BELT", "BELT", (0x3A4658, 0xCFD8E8, 0xF0F8FF),
     "A sash of silvered glass links; a blow reaches a reflection first."),
    # ---- CHEST
    ("berserker_armor_shard", "Berserker Armor Shard", "CHEST", "PLATE", (0x2A2A30, 0xA82A2A, 0xFF3A3A),
     "A shard of the armor that takes a man's pain and gives him more of it."),
    ("orsteds_dragon_god_armor", "Orsted's Dragon-God Armor", "CHEST", "PLATE", (0x1A1A24, 0x8A2A3A, 0xFF5A6A),
     "The armor of a very old, patient and strong figure; a hundred curses worn lightly."),
    # ---- FEET
    ("greaves_of_the_wind_walker", "Greaves of the Wind Walker", "FEET", "GREAVES", (0x1E2038, 0xC9A45A, 0x7A6CFF),
     "Cursed greaves that make you quicker and wind-proof; eight wind spells."),
    # ---- NECK
    ("white_whistle", "White Whistle", "NECK", "PENDANT", (0xF4F4F0, 0xD8C878, 0xFFFFFF),
     "A white whistle worn by those who have seen the bottom."),
    # ---- HANDS
    ("hand_of_vecna", "Hand of Vecna", "HANDS", "GAUNTLETS", (0x8E8A78, 0xC9A24A, 0xE0245E),
     "A withered grey hand in an ornate gold gauntlet; grafted to the wrist."),
    ("aerie_courier_gloves", "Aerie Courier Gloves", "HANDS", "GLOVES", (0x34406A, 0x7AA8FF, 0xC8DCFF),
     "Riding gloves of the sky couriers; the wind takes the wearer's side."),
    ("bloodbound_gauntlets", "Bloodbound Gauntlets", "HANDS", "GAUNTLETS", (0x3A0A0E, 0xC01818, 0xFF5A5A),
     "Iron gauntlets with veins of red from the arena where every wound was an offering."),
    ("incinerator_arm", "Incinerator Arm", "HANDS", "GAUNTLETS", (0x2A2430, 0xC86A3A, 0xFFB060),
     "A black mechanical arm that stores fire for a single blast."),
    # ---- HEAD
    ("eye_of_vecna", "Eye of Vecna", "HEAD", "CIRCLET", (0xC9A24A, 0xE0245E, 0xFFD84A),
     "A pink-irised eye on a red cord, pressed into your socket."),
    ("basilica_halo", "Basilica Halo", "HEAD", "CIRCLET", (0x5A5030, 0xFFD860, 0xFFF4B0),
     "A ring of unfallen light, worn by no one for a thousand years."),
    ("godking_crown", "Godking Crown", "HEAD", "CROWN", (0x3C4A44, 0x61766C, 0x5CF0B4),
     "Ten minutes of flight, three Tensura skills merged; then the crown takes its price."),
    ("limitless_blindfold", "Limitless Blindfold", "HEAD", "HOOD", (0x10101A, 0x8AC8FF, 0xC8E8FF),
     "A black blindfold over six blue eyes."),
    ("odins_all_seeing_eye", "Odin's All-Seeing Eye", "HEAD", "CIRCLET", (0xC8D8F0, 0xD8B860, 0xA8D8FF),
     "A pale eye set in a gold band, taken from a god's tomb."),
    ("ras_dawn_crown", "Ra's Dawn Crown", "HEAD", "CROWN", (0xF4C850, 0xFFF0C0, 0xFFE080),
     "A crown that holds a small piece of the dawn."),
]

IDS = [i[0] for i in ITEMS]
BY_ID = {i[0]: i for i in ITEMS}
PAIRED_SLOTS = ("HANDS", "FEET")


def slot_of(rid):
    return BY_ID[rid][2]


def palette_of(rid):
    return BY_ID[rid][4]
