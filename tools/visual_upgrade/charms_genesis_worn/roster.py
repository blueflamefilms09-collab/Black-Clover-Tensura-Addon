"""The slice `charms_genesis_worn`: 18 God grade charm relics and 9 Genesis grade non-weapon relics (charms, gauntlet, two crowns, the Gate of Babylon crest).

Ids, grades, slots and palettes are copied from the RelicDefs in src/main/java/com/newuniverse/nusmp/relics/wave (Wave25, Wave30, Wave31, Wave33 ... Wave79);
`lore` is the description text of the def, which the designs follow.
"""

# id, display name, grade, slot, (main, trim, glow), lore
ITEMS = [
    # ---- God grade charms (18)
    ("the_space_stone_or_the_tesseract", "The Space Stone", "GOD", "CHARM", (0x2A4A9A, 0x8FD0FF, 0x3A8BFF),
     "A small blue cube of folded space. Mark a place, then step to it with up to nine willing companions."),
    ("the_mind_stone", "The Mind Stone", "GOD", "CHARM", (0xB89A20, 0xFFF0A0, 0xFFD93A),
     "A small yellow gem that hears. Senses every thinking creature within two hundred blocks."),
    ("the_power_stone", "The Power Stone", "GOD", "CHARM", (0x6A2AB0, 0xD0A0FF, 0xA04AF0),
     "A small purple gem of raw power. Throws back every foe round you."),
    ("the_time_stone_or_the_eye_of_agamotto", "The Time Stone", "GOD", "CHARM", (0x2A8A4A, 0xA0FFC0, 0x3AE07A),
     "A small green gem that sees what is to come: the Eye of Agamotto."),
    ("the_reality_stone", "The Reality Stone", "GOD", "CHARM", (0xA02A2A, 0xFF9A9A, 0xE03A3A),
     "A small red gem that rewrites: polymorph, seeming, mirage."),
    ("the_soul_stone", "The Soul Stone", "GOD", "CHARM", (0xB05A10, 0xFFC080, 0xFF8A2A),
     "A small orange gem that keeps the dead."),
    ("deck_of_many_things", "Deck of Many Things", "GOD", "CHARM", (0x1A1A22, 0xE6B94A, 0xFFD27A),
     "Twenty-two cards in a gold-edged case."),
    ("warrens_key", "Warren's Key", "GOD", "CHARM", (0x8A5A2A, 0xD8B060, 0xE8D8A0),
     "A long bronze skeleton key with a labyrinth for a bit."),
    ("rose_petal", "Rose Petal", "GOD", "CHARM", (0x5E7C93, 0x2FBF8A, 0xE8F6F0),
     "A ghost-white rose that blooms into a thorned halberd."),
    ("administrators_authority_key", "Administrator's Authority Key", "GOD", "CHARM", (0xF0F0F8, 0x6A6A8A, 0xFFFFFF),
     "A key of one who could change anything. The world rearranges to be polite."),
    ("archivists_starbound_codex", "Archivist's Starbound Codex", "GOD", "CHARM", (0x1A1040, 0xB9A3FF, 0xE8D8FF),
     "A codex that writes itself under the stars, kept by the many-eyed archivist."),
    ("behelit_egg", "Behelit Egg", "GOD", "CHARM", (0xC8A040, 0x2A0A2A, 0xFFD870),
     "A small egg with a face that looks closed."),
    ("kamishs_fang_charm", "Kamish's Fang Charm", "GOD", "CHARM", (0x2A0A0A, 0xFF5A2A, 0xFFB060),
     "A fang of an ancient dragon, still warm."),
    ("moon_tower_black_sigil", "Moon-Tower Black Sigil", "GOD", "CHARM", (0x0A0A1A, 0x8A6AE8, 0xC0A8FF),
     "A black sigil of the strongest thing that cannot be seen."),
    ("orrery_of_drowned_hours", "Orrery of Drowned Hours", "GOD", "CHARM", (0x0C3A4A, 0x3AC8D8, 0x8AF0FF),
     "A brass orrery of seven drowned planets, from the observatory under the sea."),
    ("pocket_watch_of_return", "Pocket Watch of Return", "GOD", "CHARM", (0xC8C8D4, 0xF0D890, 0xFFFFFF),
     "A silver pocket watch whose hands run backwards at the worst possible moment."),
    ("series_ancient_grimoire", "Serie's Ancient Grimoire", "GOD", "CHARM", (0xE8E0F0, 0x6A4A9A, 0xD8B8FF),
     "An elf's book of spells older than the languages it was written in."),
    ("vaultmakers_pocketwatch", "Vaultmaker's Pocketwatch", "GOD", "CHARM", (0x2A2418, 0xE0A040, 0xFFE0A0),
     "The pocketwatch of the artificer who built the Vault of Hours, running a hand behind the day."),
    # ---- Genesis grade non-weapons (9)
    ("holy_grail_fragment", "Holy Grail Fragment", "GENESIS", "CHARM", (0xE8D070, 0x4A3A20, 0xFFF0A0),
     "A golden fragment of a cup that grants a wish and charges it to everyone else."),
    ("philosophers_stone_shard", "Philosopher's Stone Shard", "GENESIS", "CHARM", (0xC8302A, 0xF0E0A0, 0xFF6A5A),
     "A shard of a red stone that is made of a great many people. It hums with all of them."),
    ("prison_realm_cube", "Prison Realm Cube", "GENESIS", "CHARM", (0xE8E4D4, 0x4A3A2A, 0xFFFFFF),
     "A cube of seals that has held someone who should not be held."),
    ("seal_of_the_sealed_god", "Seal of the Sealed God", "GENESIS", "CHARM", (0xE8E0F0, 0x6A3A9A, 0xD8A8FF),
     "A fist-sized seal that has held something large for a long time."),
    ("sukunas_twenty_fingers", "Sukuna's Twenty Fingers", "GENESIS", "CHARM", (0x2A1414, 0xE8D0B8, 0xFF3A3A),
     "All twenty, together. The King of Curses is not here, but his pride is."),
    ("the_infinity_gauntlet", "The Infinity Gauntlet", "GENESIS", "HANDS", (0x20202A, 0xC9A24A, 0xFFD27A),
     "A black gauntlet with a ribcage palm and six sockets for the Infinity Stones."),
    ("absolute_beings_crown_shard", "Absolute Being's Crown Shard", "GENESIS", "HEAD", (0xE8E0F8, 0x6A4AA8, 0xD8C0FF),
     "A shard of a crown of a power above the monarchs."),
    ("zahards_crown_fragment", "Zahard's Crown Fragment", "GENESIS", "HEAD", (0xF0E8C0, 0x8A6AC8, 0xFFE8A0),
     "A fragment of the crown of the one at the top of the Tower."),
    ("gate_of_babylon_crest", "Gate of Babylon Crest", "GENESIS", "BACK", (0xE8C850, 0x5A3A10, 0xFFF0A0),
     "A golden mantle woven with doors, each one open on a different treasury."),
]

BY_ID = {i[0]: i for i in ITEMS}
GOD = [i[0] for i in ITEMS if i[2] == "GOD"]
GENESIS = [i[0] for i in ITEMS if i[2] == "GENESIS"]
assert len(GOD) == 18 and len(GENESIS) == 9, (len(GOD), len(GENESIS))
KIND = {"CHARM": "charm", "HANDS": "hands", "HEAD": "head", "BACK": "back"}
