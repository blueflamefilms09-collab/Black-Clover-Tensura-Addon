"""Group "Bosses and Challenges": the boss gauntlet, the Lucius god-tier series, and hard-mode quests.

Why: the arc chapters follow the story; this group is for players who want a ladder of fights in difficulty order, with the optional
no-relic and stage-clear challenges of the Lucius arena, and the long-term deeds of the Multiverse.
"""
from spec_qlib import T, R


def build(reg):
    gauntlet(reg)
    lucius(reg)
    hard(reg)


def gauntlet(reg):
    c = reg.chapter("gauntlet", "bosses", "The Boss Gauntlet", "minecraft:wither_skeleton_skull",
                    "Every boss the Multiverse has, from the pit guardians to the final hour.")
    c.q("welcome", "The Gauntlet", (0, 3), "minecraft:wither_skeleton_skull", "intro", "A ladder of fights",
        "Every boss of the Multiverse is on this ladder, in rough order of difficulty. Boss kills count for the whole team: a squad that hunts "
        "together shares the credit, and this book is shared by the team too.\nThe ladder has four tiers: the floor bosses, the Dark Triad and "
        "their friends, the devils and the God of Order, and the last Zogratis.", tasks=[T.check("Lace up your boots")])
    c.q("first", "First Blood", (1, 3), "minecraft:iron_sword", "step", "Any boss",
        "Defeat any boss of the Multiverse.", deps=["welcome"], tasks=[T.adv("bosses/first")])
    # tier 1
    c.q("daemon", "Daemon of the Grimoire", (2, 1), "minecraft:wither_skeleton_skull", "boss", "A grimoire that fights back",
        "A Grimoire Daemon guards a book too old to read. Defeat one.", deps=["first"], tasks=[T.adv("bosses/grimoire_daemon")])
    c.q("gale", "Lord of the Gale", (2, 2), "minecraft:feather", "boss", "The Wind Spirit Lord",
        "Defeat the Wind Spirit Lord.", deps=["first"], tasks=[T.adv("bosses/wind_spirit_lord")])
    c.q("warden", "The Floor Warden", (2, 3), "minecraft:iron_bars", "boss", "Keeper of a Labyrinth floor",
        "Defeat a Warden of the Labyrinth.", deps=["first"], tasks=[T.adv("bosses/floor_warden")])
    c.q("goliath", "Goliath", (2, 4), "minecraft:heavy_core", "boss", "The pit guardian",
        "Defeat Goliath in its pit.", deps=["first"], tasks=[T.adv("bosses/goliath")])
    c.q("charybdis", "Spawn of Charybdis", (2, 5), "minecraft:nautilus_shell", "boss", "The whirlpool",
        "Defeat the Spawn of Charybdis in its basin.", deps=["first"], tasks=[T.adv("bosses/charybdis_spawn")])
    c.q("tier1", "Tier I Cleared", (3.5, 3), "minecraft:iron_sword", "mile", "Three bosses",
        "Defeat three bosses in total.", deps=["daemon", "gale", "warden", "goliath", "charybdis"], dep_any=True,
        tasks=[T.adv("bosses/count_3")])
    # tier 2
    c.q("dante", "Dante", (5, 1.5), "nusmp:dante_gravity_crown", "boss", "Gravity",
        "Defeat Dante Zogratis.", deps=["tier1"], tasks=[T.adv("bosses/dante_zogratis")])
    c.q("vanica", "Vanica", (5, 3), "minecraft:ender_eye", "boss", "Curses",
        "Defeat Vanica Zogratis.", deps=["tier1"], tasks=[T.adv("bosses/vanica_zogratis")])
    c.q("zenon", "Zenon", (5, 4.5), "nusmp:zenon_spatial_ring", "boss", "Gates",
        "Defeat Zenon Zogratis.", deps=["tier1"], tasks=[T.adv("bosses/zenon_zogratis")])
    c.q("triad", "The Dark Triad Falls", (6.5, 3), "nusmp:charter_grimoire_dante", "mile", "All three",
        "Dante, Vanica and Zenon, all down.", deps=["dante", "vanica", "zenon"], tasks=[T.adv("bosses/triad")],
        rewards=[R.xp(400), R.loot("legend_trophy")])
    c.q("ten", "Slayer of Legends", (8, 3), "minecraft:netherite_sword", "mile", "Ten bosses",
        "Defeat ten bosses in total.", deps=["triad"], tasks=[T.adv("bosses/count_10")])
    # tier 3
    c.q("zagred", "Zagred", (9.5, 1.5), "nusmp:last_word", "boss", "The Devil of the Word",
        "Defeat Zagred.", deps=["ten"], tasks=[T.adv("bosses/zagred")])
    c.q("avatar", "The Devil's Avatar", (9.5, 3), "nusmp:devil_horn", "boss", "A devil in a body",
        "Defeat the Devil Avatar.", deps=["ten"], tasks=[T.adv("bosses/devil_avatar")])
    c.q("order", "The God of Order", (9.5, 4.5), "nusmp:notice_of_order", "boss", "Judge of the ledger",
        "Defeat the God of Order. He judges relics, and he does not like losing.", deps=["ten"], tasks=[T.adv("bosses/god_of_order")])
    c.q("calamity", "Calamity", (11, 3), "minecraft:wither_skeleton_skull", "mile", "Twenty-five bosses",
        "Defeat twenty-five bosses in total.", deps=["zagred", "avatar", "order"], tasks=[T.adv("bosses/count_25")])
    # tier 4
    c.q("lucius", "Lucius, the Final Hour", (12.5, 3), "nusmp:zogratis_hourglass", "boss", "The last Zogratis",
        "Defeat Lucius Zogratis in the arena of the Final Hour.", deps=["calamity"], tasks=[T.adv("bosses/lucius_zogratis")],
        rewards=[R.xp(600), R.loot("legend_trophy")])
    c.q("all", "No Boss Left Standing", (14, 3), "minecraft:dragon_egg", "final", "All twelve",
        "Every boss the Multiverse has, defeated. The gauntlet is closed.", deps=["lucius"], tasks=[T.adv("bosses/all")],
        rewards=[R.xp(1500), R.loot("legend_trophy"), R.choice("choice_knight")])
    # hunters (kill tasks)
    c.q("disciples", "Clean Up the Disciples", (3.5, 6), "nusmp:disciple_sigil", "side", "Twenty dark disciples",
        "The Zogratis keep disciples. Kill twenty Dark Disciples.", deps=["first"], tasks=[T.kill("nusmp:dark_disciple", 20)],
        rewards=[R.xp(60), R.loot("arc_cache")])
    c.q("beasts", "Blood Beasts", (5, 6), "minecraft:bone", "side", "Ten blood beasts",
        "Kill ten Blood Beasts.", deps=["disciples"], tasks=[T.kill("nusmp:blood_beast", 10)], rewards=[R.xp(50), R.loot("arc_cache")])
    c.q("clones", "Echoes", (6.5, 6), "minecraft:ender_pearl", "side", "Zogratis clones",
        "Kill ten Zogratis clones.", deps=["beasts"], tasks=[T.kill("nusmp:zogratis_clone", 10)], rewards=[R.xp(60), R.loot("arc_cache")])
    c.q("basilisks", "Basilisk Hunt", (3.5, 7.5), "nusmp:raw_basilisk_fillet", "side", "Five basilisks",
        "Kill five basilisks of the dungeon.", deps=["first"], tasks=[T.kill("nusmp:basilisk", 5)], rewards=[R.xp(50), R.loot("dungeon_cache")])
    c.q("direwolves", "Direwolf Pack", (5, 7.5), "nusmp:direwolf_pelt", "side", "Ten direwolves",
        "Kill ten dungeon direwolves.", deps=["basilisks"], tasks=[T.kill("nusmp:dungeon_direwolf", 10)], rewards=[R.xp(50), R.loot("dungeon_cache")])
    c.q("armour", "Empty Armour", (6.5, 7.5), "minecraft:iron_chestplate", "side", "Fifteen living armours",
        "Kill fifteen living armours.", deps=["direwolves"], tasks=[T.kill("nusmp:living_armor", 15)], rewards=[R.xp(60), R.loot("dungeon_cache")])


def lucius(reg):
    c = reg.chapter("lucius_god", "bosses", "Lucius: The God Tier", "nusmp:zogratis_hourglass",
                    "Three stages, a hit cap, and a clock that does not stop.")
    c.q("welcome", "The Final Hour", (0, 2), "nusmp:zogratis_hourglass", "intro", "Lucius, balanced",
        "Lucius Zogratis can be fought only in his own arena, and only through the Hourglass of the Final Hour.\n"
        "The fight has three stages with one health bar each; a hard cap on how much a single hit can take; true damage as a percent of "
        "your health; and a clock of decay. It is balanced for two to four players. Nothing here is an exam you can cheese.",
        tasks=[T.check("Read the rules of the arena")])
    c.q("hourglass", "The Hourglass", (1, 2), "nusmp:zogratis_hourglass", "step", "Carry it",
        "Carry the Hourglass of the Final Hour.", deps=["welcome"], tasks=[T.adv("world/hourglass_item")])
    c.q("sigil", "Return Sigil", (1, 3.5), "nusmp:arena_return_sigil", "side", "A way home",
        "Carry an Arena Return Sigil.", deps=["welcome"], tasks=[T.adv("world/sigil")])
    c.q("turn", "Turn the Glass", (2, 2), "nusmp:zogratis_hourglass", "step", "Begin",
        "Use the Hourglass and let the final hour begin.", deps=["hourglass"], tasks=[T.adv("world/hourglass_used")])
    c.q("enter", "The Final Hour", (3, 2), "nusmp:arena_return_sigil", "mile", "Step into the arena",
        "Enter the arena.", deps=["turn"], tasks=[T.adv("world/arena")])
    c.q("dim", "The Arena Dimension", (3, 3.5), "nusmp:arena_return_sigil", "side", "Arrive",
        "Arrive in the Zogratis Arena dimension.", deps=["turn"], tasks=[T.dim("nusmp:zogratis_arena")])
    c.q("stage2", "The Second Stage", (4, 1), "nusmp:zogratis_hourglass", "boss", "One bar down",
        "Push Lucius into his second stage. Each stage is a full bar and a new arsenal.", deps=["enter"], tasks=[T.adv("bosses/lucius_stage2")])
    c.q("stage3", "The Third Stage", (5.5, 1), "nusmp:zogratis_hourglass", "boss", "Two bars down",
        "Push Lucius into his third and final stage.", deps=["stage2"], tasks=[T.adv("bosses/lucius_stage3")])
    c.q("all_stages", "Three Stages, One Life", (7, 1), "nusmp:zogratis_hourglass", "mile", "Through all three",
        "Push Lucius through all three stages.", deps=["stage3"], tasks=[T.adv("challenges/lucius_all_stages")],
        rewards=[R.xp(400), R.loot("legend_trophy")])
    c.q("decay", "Time Decay", (4, 3), "minecraft:clock", "side", "Feel the hour",
        "Feel the passing hour ravage you. Time Decay is how Lucius' clock bites.", deps=["enter"], tasks=[T.adv("challenges/time_decay")])
    c.q("severed", "Severed", (5.5, 3), "minecraft:iron_sword", "side", "A severing blow",
        "Be struck by a severing blow.", deps=["decay"], tasks=[T.adv("challenges/severed")])
    c.q("nullified", "Nullified", (7, 3), "minecraft:barrier", "side", "Your relics fail",
        "Have your relics nullified by the arena.", deps=["severed"], tasks=[T.adv("challenges/nullified")])
    c.q("victory", "Hour's End", (8.5, 2), "minecraft:totem_of_undying", "final", "Win in the arena",
        "Win in the arena. Take a reward: you have earned it.", deps=["all_stages"], tasks=[T.adv("world/arena_victory")],
        rewards=[R.xp(500), R.loot("legend_trophy"), R.choice("choice_knight")])
    c.q("kill", "Lucius Falls", (8.5, 0), "nusmp:zogratis_hourglass", "boss", "The last Zogratis",
        "Defeat Lucius Zogratis.", deps=["all_stages"], tasks=[T.adv("bosses/lucius_zogratis")], rewards=[R.xp(600), R.loot("legend_trophy")])
    c.q("norelic", "With Bare Hands", (10, 2), "minecraft:iron_sword", "boss", "No relic at all",
        "Win in the arena carrying no relic at all: not in your hands, not in your slots, not in your bag. Skill, grimoire and nothing else.",
        deps=["victory"], tasks=[T.adv("challenges/norelic")], rewards=[R.xp(1500), R.loot("legend_trophy"), R.item("minecraft:nether_star", 2, team=True)],
        optional=True)
    c.q("stages_no_death", "A Clean Run", (10, 3.5), "minecraft:shield", "side", "Honour system",
        "Win without a single death on your team. This one is on your honour: tick it when it is true.", deps=["victory"],
        tasks=[T.check("No one on my team died")], optional=True)
    c.q("hit_cap", "Death of a Thousand Cuts", (10, 0.5), "minecraft:netherite_sword", "side", "Honour system",
        "A single hit can never take more than a few percent of a stage's bar. Land fifty clean hits on a stage and tick this.",
        deps=["stage2"], tasks=[T.check("Fifty clean hits on a stage")], optional=True)
    c.q("final", "The God Falls", (11.5, 2), "nusmp:zogratis_hourglass", "final", "The end of the arena",
        "Lucius is finished, and so is his hour. Nothing in the Multiverse ever asked more of you.", deps=["victory"],
        tasks=[T.adv("bosses/lucius_zogratis")], rewards=[R.xp(1000), R.loot("legend_trophy")])


def hard(reg):
    c = reg.chapter("hard_mode", "bosses", "Hard Mode", "minecraft:netherite_ingot",
                    "Long-term deeds for those who have done the rest.")
    c.q("welcome", "Trials of the Multiverse", (0, 2), "minecraft:netherite_ingot", "intro", "Harder deeds",
        "These quests are all optional and mostly long. They ask for the whole of the Multiverse: every magic, every boss, every stone. "
        "The rewards are big, and most of them are shared with your team.", tasks=[T.check("Accept the trials")])
    c.q("ten_boss", "Armed and Ready", (1, 1), "minecraft:enchanted_golden_apple", "mile", "Ten pages and three bosses",
        "Ten pages unlocked and three bosses down.", deps=["welcome"], tasks=[T.adv("challenges/ten_pages_boss")], optional=True)
    c.q("paladins", "An Army of Souls", (2, 0), "minecraft:totem_of_undying", "mile", "Five paladins",
        "Convert five souls into paladins.", deps=["welcome"], tasks=[T.adv("challenges/paladins_5")], optional=True)
    c.q("corpses", "Necromancer", (2, 1), "minecraft:wither_skeleton_skull", "mile", "Ten corpses",
        "Raise ten soul corpses.", deps=["welcome"], tasks=[T.adv("challenges/corpses_10")], optional=True)
    c.q("genesis", "Genesis in Hand", (2, 2), "minecraft:nether_star", "boss", "Genesis grade",
        "Hold a relic of Genesis grade.", deps=["ten_boss"], tasks=[T.adv("challenges/genesis_relic")], optional=True,
        rewards=[R.xp(500), R.loot("legend_trophy")])
    c.q("all_magic", "Master of Every Magic", (2, 3), "minecraft:nether_star", "boss", "All 105",
        "Cast all 105 magics.", deps=["welcome"], tasks=[T.adv("challenges/all_magic")], optional=True,
        rewards=[R.xp(1000), R.loot("legend_trophy")])
    c.q("seals", "Twelve Seals of the Djinn", (3, 0), "nusmp:djinn_seal_paimon", "boss", "All twelve",
        "Carry all twelve Djinn seals.", deps=["paladins"], tasks=[T.adv("relics/djinn_seals")], optional=True,
        rewards=[R.xp(400), R.loot("legend_trophy")])
    c.q("vecna", "The Pair of Vecna", (3, 1), "nusmp:hand_of_vecna", "boss", "Hand and Eye",
        "Carry the Hand and Eye of Vecna.", deps=["corpses"], tasks=[T.adv("relics/vecna")], optional=True,
        rewards=[R.xp(300), R.loot("legend_trophy")])
    c.q("stones", "All Six Stones", (3, 2), "nusmp:the_infinity_gauntlet", "boss", "Six stones and a gauntlet",
        "Carry all six Infinity Stones and the Gauntlet.", deps=["genesis"], tasks=[T.adv("relics/stones_all")], optional=True,
        rewards=[R.xp(2000), R.loot("legend_trophy")])
    c.q("cores", "Nine Devil Cores", (3, 3), "nusmp:devil_core_beelzebub", "boss", "The whole choir",
        "Carry all nine devil cores.", deps=["all_magic"], tasks=[T.adv("spirits/devil_cores_all")], optional=True,
        rewards=[R.xp(1000), R.loot("legend_trophy")])
    c.q("charters", "The Complete Charter", (4, 1), "nusmp:charter_grimoire_lucius", "mile", "Seven charters",
        "Carry all seven boss charter grimoires.", deps=["seals", "vecna"], dep_any=True, tasks=[T.adv("squads/charter_all")], optional=True)
    c.q("four_forms", "Four Forms of the Shadow", (4, 3), "minecraft:ender_eye", "mile", "Unite",
        "Take all four Unite forms.", deps=["cores"], tasks=[T.adv("spells/unite_all")], optional=True)
    c.q("roster", "The Whole Roster", (5, 2), "minecraft:totem_of_undying", "boss", "Twenty canon pages",
        "Unlock the canon page of every mage on the Walk in Their Shoes list.", deps=["stones", "cores"], dep_any=True,
        tasks=[T.adv("spells/characters_all")], optional=True, rewards=[R.xp(500), R.loot("legend_trophy")])
    c.q("final", "Legend of the Multiverse", (7, 2), "minecraft:nether_star", "final", "Everything",
        "Every trial answered. There is nothing left to prove; there is only the next server.", deps=["roster", "charters", "four_forms"],
        tasks=[T.check("I am a legend")], rewards=[R.xp(5000), R.loot("legend_trophy"), R.loot("legend_trophy")], optional=True)
