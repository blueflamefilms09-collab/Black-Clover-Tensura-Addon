"""Group "The Journey": prologue, magic mastery, weapons and relics, dungeon and labyrinth, spirits and devils, squads and kingdoms.

Why: these six chapters follow the systems of the mod rather than the anime plot. They are the how-to of the Multiverse; the arc chapters
(spec_q_arcs.py) are the story built on top of them. Every advancement task points at an advancement of spec_adv.py.
"""
from spec_qlib import T, R
from spec_magic import BRANCHES, MAGICS

NAME = {m: n for m, n, _ in MAGICS}


def build(reg):
    prologue(reg)
    mastery(reg)
    relics(reg)
    dungeon(reg)
    spirits(reg)
    squads(reg)


# ============================================================================================================ prologue
def prologue(reg):
    c = reg.chapter("prologue", "journey", "Prologue: First Steps", "nusmp:grimoire",
                    "Your first grimoire, your first spell, your first page.")
    c.q("welcome", "Welcome to the Multiverse", (0, 1), "nusmp:grimoire", "intro", "Where every world is a page",
        "Magic is not something you are taught here. It is something that chooses you.\n"
        "This book follows you from your first grimoire to the last boss of the Multiverse. Every quest is tied to an advancement, "
        "so what you do in the world is what you tick off here. Nothing is lost if you skip ahead: quests unlock in a gentle order, "
        "and the paths are yours to take.",
        tasks=[T.check("Begin the journey")])
    c.q("book", "This Very Book", (1, 1), "minecraft:book", "step", "The quest book",
        "You are holding the quest book now. Chapters on the left are grouped: The Journey teaches the systems of the mod, The Anime Arcs "
        "follow the story arc by arc, and Bosses and Challenges is the gauntlet. Rewards are claimed with a click; some are shared with "
        "your team.", deps=["welcome"], tasks=[T.check("Read the book")])
    c.q("grimoire", "A Grimoire of Your Own", (2, 1), "nusmp:grimoire", "mile", "The book that chooses",
        "A grimoire is a mage's tool, shield and soul in one book. It holds your magic, your pages and your mastery.\n"
        "Receive yours at a Grimoire Altar at the top of a Grimoire Tower, or let the Tower Wizard hand it over after his questions.",
        deps=["book"], tasks=[T.adv("start/first_grimoire")], rewards=[R.xp(40), R.loot("journey_cache")])
    c.q("open_book", "Open the Cover", (3, 1), "nusmp:grimoire_summons", "step", "Read your own book",
        "Open the grimoire. The pages inside are your spells; the first few are open from the start, the rest wait for your mastery.",
        deps=["grimoire"], tasks=[T.adv("start/open_book")])
    c.q("ceremony", "The Acceptance Ceremony", (3, 0), "nusmp:grimoire_altar", "mile", "Chosen at the tower",
        "Once a year the grimoires choose every mage who comes to a Grimoire Tower. If you missed the day, any altar will take you in later.\n"
        "Take part in the ceremony and the book is yours.", deps=["grimoire"], tasks=[T.adv("start/ceremony")])
    c.q("collector", "Collector of Covers", (4, 0), "minecraft:lectern", "side", "Five grimoires",
        "Some mages carry more than one. Receive five grimoires over your journeys.", deps=["ceremony"],
        tasks=[T.adv("start/five_grimoires")], rewards=[R.xp(40), R.loot("magic_cache")])
    c.q("wizard", "The Tower Wizard", (3, 2), "minecraft:book", "step", "Ask him anything",
        "A wizard waits at the top of every Grimoire Tower. He keeps the library of the Multiverse and asks the questions that decide "
        "which magic is yours. Find a tower and speak to him.", deps=["grimoire"], tasks=[T.adv("start/meet_wizard")])
    c.q("questionnaire", "The Wizard's Questions", (4, 2), "minecraft:enchanted_book", "step", "Your born magic",
        "Answer every question. Your answers do not pick your magic; they let the wizard see which magic you already are.",
        deps=["wizard"], tasks=[T.adv("start/questionnaire")], rewards=[R.xp(30), R.loot("magic_cache")])
    c.q("stone", "Crystallised Mana", (2, 3), "nusmp:magic_stone", "step", "The dungeon's coin",
        "Monsters of the Multiverse leave magic stones when they fall. Pick one up.", deps=["grimoire"],
        tasks=[T.adv("start/magic_stone")])
    c.q("stones8", "A Purse of Stones", (3, 3), "nusmp:magic_stone", "step", "Eight stones",
        "Gather eight magic stones. They pay for much of what the dungeon sells and the squads need.", deps=["stone"],
        tasks=[T.item("nusmp:magic_stone", 8)], rewards=[R.xp(25), R.item("minecraft:emerald", 4)])
    c.q("first_spell", "First Spell", (4, 1), "minecraft:blaze_powder", "mile", "Cast!",
        "Choose a page, hold the grimoire and cast. Every spell costs mana; the bar shows what you have left.\n"
        "Every successful cast feeds your mastery.", deps=["open_book"], tasks=[T.adv("start/first_spell")])
    c.q("mana", "Spending Mana", (5, 0), "minecraft:lapis_lazuli", "step", "Twenty-five castings",
        "Every spell costs mana and mana refills slowly. Cast twenty-five times to learn the rhythm of spending and recovering.", deps=["first_spell"],
        tasks=[T.adv("start/mana_use")])
    c.q("casts50", "Practice Makes Perfect", (6, 0), "minecraft:experience_bottle", "step", "Fifty casts",
        "Fifty casts. This is where mastery really starts to move.", deps=["mana"], tasks=[T.adv("start/casts_50")])
    c.q("casts500", "Tireless Caster", (7, 0), "minecraft:nether_star", "side", "Five hundred casts",
        "Five hundred casts. A mage who never stops training.", deps=["casts50"], tasks=[T.adv("start/casts_500")],
        rewards=[R.xp(75), R.loot("magic_cache")])
    c.q("codex", "The Codex", (5, 2), "nusmp:compat_codex", "step", "Everything in one book",
        "The Codex menu lists every magic, relic and boss you have discovered. Open it with its key and look up your first magic.",
        deps=["first_spell"], tasks=[T.check("Open the Codex")])
    c.q("kingdoms_menu", "Four Kingdoms", (6, 2), "minecraft:white_banner", "side", "Clover, Spade, Heart, Diamond",
        "The Four Kingdoms menu shows which kingdom accepted you and what each kingdom stands for. Take a look.", deps=["codex"],
        tasks=[T.check("Open the Four Kingdoms menu")])
    c.q("first_page", "A New Page", (5, 1), "minecraft:writable_book", "step", "Pages open by mastery",
        "Pages never open by killing; only mastery opens them. Keep casting until a new page unlocks.", deps=["first_spell"],
        tasks=[T.adv("start/first_page")])
    c.q("pages5", "Five Pages Deep", (6, 1), "minecraft:written_book", "step", "Five open pages",
        "Five pages unlocked. Some pages ask for a rank as well: zone pages for Intermediate Magic Knights, signature pages for Senior.",
        deps=["first_page"], tasks=[T.adv("start/pages_5")])
    c.q("pages10", "A Full Grimoire", (7, 1), "minecraft:enchanted_book", "mile", "Ten pages",
        "Ten pages. A grimoire that has grown with its master.", deps=["pages5"], tasks=[T.adv("start/pages_10")])
    c.q("mastery", "Mastery", (8, 1), "minecraft:nether_star", "final", "Fill the bar",
        "Fill the mastery bar of a grimoire. This is the end of the Prologue and the start of everything else.\n"
        "Pick a reward for the road: the Journey has many paths.", deps=["pages10"], tasks=[T.adv("start/mastery")],
        rewards=[R.xp(200), R.choice("choice_mage")])


# =============================================================================================================== mastery
def mastery(reg):
    c = reg.chapter("magic_mastery", "journey", "Magic Mastery", "minecraft:enchanted_book",
                    "One chain for every family of magic: awaken, cast, sign, master.")
    c.q("welcome", "The Many Magics", (0, 4), "minecraft:enchanted_book", "intro", "One hundred and five magics",
        "The Multiverse knows 105 magics, from the flame of a lion to the word of a devil. They are gathered here in nine branches. "
        "Each branch is a chain: awaken the first magic, cast every magic of the branch, cast a signature page, and finally master "
        "them all.\nStart wherever your grimoire points you; the branches can be done in any order.",
        tasks=[T.check("Look over the branches")])
    for i, (bid, title, icon, blurb, fams) in enumerate(BRANCHES):
        row = i
        first = fams[0]
        c.q(bid + "_awaken", title + ": Awaken", (2, row), icon, "step", blurb.capitalize(),
            "Cast %s Magic, the first of this branch. %s" % (NAME[first], "The magics of this branch: " + ", ".join(NAME[f] for f in fams[:6])
                                                          + (", and more." if len(fams) > 6 else ".")),
            deps=["welcome"], tasks=[T.adv("magic/cast_" + first)])
        c.q(bid + "_all", title + ": Every Magic", (3.5, row), icon, "step", "%d magics" % len(fams),
            "Cast each of the %d magics of this branch once. Not all of them are in the same grimoire: magics can be learned, merged "
            "and borrowed over a long career." % len(fams), deps=[bid + "_awaken"],
            tasks=[T.adv("magic/cast_" + f) for f in fams], rewards=[R.xp(60 + 5 * len(fams)), R.loot("magic_cache")])
        c.q(bid + "_sign", title + ": Signature", (5, row), icon, "mile", "A spell to be known by",
            "A signature page is the spell a mage is known for. Unlock and cast one of this branch's signature pages.",
            deps=[bid + "_all"], tasks=[T.adv("magic/branch_%s_signature" % bid)])
        c.q(bid + "_master", title + ": Mastery", (6.5, row), icon, "boss", "Master of the branch",
            "Every magic of the branch cast, and a signature page after it. You are a master of this branch.",
            deps=[bid + "_sign"], tasks=[T.adv("magic/branch_%s_all" % bid)], rewards=[R.xp(150), R.loot("arc_cache")])
    # the ladder of distinct magics, down the right edge
    ladder = [(5, "Five Kinds of Magic", "5"), (10, "Ten Kinds of Magic", "10"), (25, "Two-and-Twenty-Five", "25"),
              (50, "Half a Hundred", "50")]
    prev = "welcome"
    for j, (n, title, sub) in enumerate(ladder):
        c.q("distinct_%d" % n, title, (8.5, j), "minecraft:enchanted_book", "step", "%s different magics" % sub,
            "Cast %d different magics. Magic you have cast counts once however often you cast it." % n, deps=[prev] if j else ["welcome"],
            tasks=[T.adv("magic/distinct_%d" % n)])
        prev = "distinct_%d" % n
    c.q("distinct_all", "Every Magic There Is", (10, 4), "minecraft:nether_star", "final", "All 105",
        "All 105 magics, cast at least once. Nobody has done it. Be the first.\nAs a reward, choose how you want to spend what you have earned.",
        deps=["distinct_50"] + [b[0] + "_master" for b in BRANCHES[:1]], tasks=[T.adv("magic/distinct_105")],
        rewards=[R.xp(1000), R.loot("legend_trophy")])
    c.q("souls", "Know Your Soul", (0, 7), "minecraft:ender_eye", "side", "Eleven soul types",
        "Every soul has a nature: Flame, Water, Wind, Earth, Light, Darkness, Space, Time, Battle, Fantasy, or Empty. Cast at least one magic "
        "of each soul to see how far your own nature reaches.", deps=["welcome"],
        tasks=[T.adv("magic/soul_" + g) for g in ["flame", "water", "wind", "earth", "light", "darkness", "space", "time", "battle",
                                                  "fantasy", "empty"]], rewards=[R.xp(120), R.loot("magic_cache")])
    c.q("merge", "Two Magics, One Spell", (2, 9.5), "minecraft:smithing_table", "mile", "Magic merging",
        "Some mages learn to merge two magics into a single spell. It costs more, hits harder, and is the road to the rarest abilities.",
        deps=["welcome"], tasks=[T.adv("spells/merged")])
    c.q("merge5", "Alchemist of Magic", (3.5, 9.5), "minecraft:smithing_table", "boss", "Five merges",
        "Merge magics five times.", deps=["merge"], tasks=[T.adv("spells/merged_5")], rewards=[R.xp(150), R.loot("arc_cache")])
    c.q("characters", "Walk in Their Shoes", (5, 9.5), "minecraft:player_head", "mile", "Canon pages",
        "Some pages belong to one mage of the story and open only to their grimoire. Unlock one.", deps=["merge"],
        tasks=[T.adv("spells/characters")])
    c.q("roster", "The Whole Roster", (6.5, 9.5), "minecraft:totem_of_undying", "boss", "Twenty canon pages",
        "Unlock the signature page of every canon mage on the list: Yami, Nacht, Luck, Gordon, Henry, Zora, Fuegoleon, Yuno, William, Jack, "
        "Rill, Dorothy, Kaiser, Nozel, Secre, Vanessa, Lucius, Rades, Kahono and Kiato.", deps=["characters"],
        tasks=[T.adv("spells/characters_all")], rewards=[R.xp(500), R.loot("legend_trophy")], optional=True)
    c.q("arts", "Strange Arts", (2, 11), "minecraft:amethyst_shard", "step", "The special arts",
        "Dance a Butoh, sing a song, raise a corpse, convert a paladin, save Rouge, spring a thread trap, travel through shadows, stop time. "
        "Do any one of them.", deps=["welcome"], tasks=[T.adv("spells/special")])
    c.q("unite", "Four Forms of the Shadow", (3.5, 11), "minecraft:ender_eye", "boss", "Canis, Equus, Felis, Gallus",
        "A shadow mage can unite with a beast. Take all four Unite forms.", deps=["arts"], tasks=[T.adv("spells/unite_all")],
        rewards=[R.xp(200), R.loot("arc_cache")], optional=True)


# ============================================================================================================== relics
def relics(reg):
    c = reg.chapter("weapons_relics", "journey", "Weapons and Relics", "nusmp:ring_of_power",
                    "Relics of many worlds, their grades, the forge, and the named blades.")
    c.q("welcome", "Relics of Many Worlds", (0, 2), "nusmp:bag_of_holding", "intro", "Each one exists once",
        "A relic is a treasure from another world. There is only one of each in the entire server, written in the Unique Ledger of the God of Order.\n"
        "They cannot be crafted and they do not drop from ordinary monsters: you will find them in dungeon vaults, in the Labyrinth's vault at the "
        "top, and as rare rewards from the oldest bosses.", tasks=[T.check("Open the Ledger")])
    c.q("first", "Relic of Many Worlds", (1, 2), "nusmp:bag_of_holding", "mile", "Your first",
        "Obtain a relic.", deps=["welcome"], tasks=[T.adv("relics/first")])
    c.q("equipped", "Attuned", (2, 2), "nusmp:ring_of_power", "step", "Wear it",
        "Wear or wield a relic. Some relics ask you to attune to them first; hold them for a while and they answer.",
        deps=["first"], tasks=[T.adv("relics/equipped")])
    grades = [("normal", "Normal Grade"), ("special", "Special Grade"), ("rare", "Rare Grade"), ("unique", "Unique Grade"),
              ("legend", "Legend Grade"), ("god", "God Grade"), ("genesis", "Genesis Grade")]
    prev = "equipped"
    for i, (g, ttl) in enumerate(grades):
        kind = "step" if i < 4 else ("mile" if i < 6 else "boss")
        c.q("grade_" + g, ttl, (3 + i, 2), "nusmp:ring_of_power" if i < 4 else "minecraft:nether_star", kind,
            "Grade %d of 7" % (i + 1),
            "Hold a relic of %s. %s" % (ttl, "Legend and above can carry a Djinn." if g == "legend" else
                                         "The higher the grade, the more it lifts the wearer's existence points." if i < 6 else
                                         "The highest grade there is. Hold one and the world watches you."),
            deps=[prev], tasks=[T.adv("relics/grade_" + g)],
            rewards=[R.xp(20 + 30 * i * i // 2), R.loot("arc_cache" if i < 5 else "legend_trophy")] if i >= 3 else None)
        prev = "grade_" + g
    c.q("count10", "A Small Hoard", (1, 0), "nusmp:bag_of_holding", "side", "Ten relics",
        "Obtain ten relics over time.", deps=["first"], tasks=[T.adv("relics/count_10")])
    c.q("count30", "A Museum of Many Worlds", (2, 0), "nusmp:bag_of_holding", "side", "Thirty relics",
        "Thirty relics. Even the God of Order raises an eyebrow.", deps=["count10"], tasks=[T.adv("relics/count_30")],
        rewards=[R.xp(150), R.loot("arc_cache")])
    c.q("pedestal", "On a Pedestal", (1, 3.5), "nusmp:relic_pedestal", "side", "Display your finds",
        "Carry a Relic Pedestal, the plinth the Labyrinth's vault uses.", deps=["first"], tasks=[T.adv("relics/pedestal")])
    c.q("forge_block", "The Relic Forge", (2, 3.5), "nusmp:relic_forge", "step", "Where relics are worked",
        "Carry a Relic Forge. It engraves, reforges and sockets relics.", deps=["first"], tasks=[T.adv("relics/forge_block")])
    c.q("forged", "Reforged", (3, 3.5), "minecraft:anvil", "mile", "The forge speaks",
        "Use the forge on a relic.", deps=["forge_block"], tasks=[T.adv("relics/forged")])
    c.q("socketed", "Socketed", (4, 3.5), "minecraft:amethyst_shard", "step", "Cores in sockets",
        "Set a socket core into a relic. Sockets give boons and change how a relic looks.", deps=["forged"],
        tasks=[T.adv("relics/socketed")])
    c.q("tier", "Tier Ritual", (5, 3.5), "minecraft:beacon", "mile", "Beyond the grade",
        "Raise a relic through a tier ritual. A higher tier scales its acts.", deps=["socketed"], tasks=[T.adv("relics/tier")])
    c.q("ledger", "In the Ledger", (3, 5), "nusmp:notice_of_order", "step", "The God of Order keeps count",
        "Be entered into the Unique Ledger of the God of Order. Relics are unique, and the ledger knows.", deps=["first"],
        tasks=[T.adv("relics/ledger")])
    c.q("djinn", "A Djinn Answers", (4, 5), "nusmp:djinn_seal_baal", "mile", "Bind a Djinn",
        "A Legend relic, or above, can serve as a metal vessel. Bind a Djinn into one.", deps=["grade_legend"],
        tasks=[T.adv("relics/djinn")])
    c.q("seals", "Twelve Seals of the Djinn", (5, 5), "nusmp:djinn_seal_paimon", "boss", "All twelve",
        "Carry all twelve Djinn seals: Amon, Baal, Bifron, Focalor, Furfur, Marchosias, Paimon, Sitri, Vassago, Vinea, Zagan, Zepar.",
        deps=["djinn"], tasks=[T.adv("relics/djinn_seals")], rewards=[R.xp(400), R.loot("legend_trophy")], optional=True)
    c.q("pair", "A Matched Pair", (3, 6.5), "nusmp:twin_hook_swords", "mile", "Right hand and left",
        "Pair relics are two weapons that are one relic: they come together, the world holds one pair, and the bonus works only with a half in "
        "each hand. Carry both halves of the Twin Hook Swords.", deps=["equipped"], tasks=[T.adv("relics/pair")])
    c.q("pairs_all", "Three Pairs", (4, 6.5), "nusmp:whirling_blades", "boss", "Hook, Whirl, Deflect",
        "Carry both halves of the Twin Hook Swords, the Whirling Blades and the Twin Swords of Deflection.", deps=["pair"],
        tasks=[T.adv("relics/pairs_all")], rewards=[R.xp(200), R.loot("legend_trophy")], optional=True)
    c.q("vecna", "The Pair of Vecna", (5, 6.5), "nusmp:hand_of_vecna", "boss", "Hand and Eye",
        "Carry both the Hand and the Eye of Vecna. Worn and attuned together, they are a bonded pair with a set bonus of their own.", deps=["equipped"],
        tasks=[T.adv("relics/vecna")], rewards=[R.xp(300), R.loot("legend_trophy")])
    c.q("stone", "A Stone of Infinity", (9, 3.5), "nusmp:the_power_stone", "mile", "Six stones in the world",
        "Carry any Infinity Stone. There are six, and each exists once.", deps=["grade_genesis"], tasks=[T.adv("relics/stone_any")])
    c.q("gauntlet", "The Infinity Gauntlet", (10, 3.5), "nusmp:the_infinity_gauntlet", "mile", "A fist of stones",
        "Carry the Infinity Gauntlet. Every stone you carry is set in it and answers to its power.", deps=["stone"],
        tasks=[T.adv("relics/gauntlet")])
    c.q("all_stones", "All Six Stones", (11, 3.5), "nusmp:the_infinity_gauntlet", "final", "Snap",
        "All six Infinity Stones and the Gauntlet, carried at once. Choose carefully what you do with the power.", deps=["gauntlet"],
        tasks=[T.adv("relics/stones_all")], rewards=[R.xp(2000), R.loot("legend_trophy")])
    # ---- weapons
    c.q("w_first", "Arms of Many Worlds", (0, 8), "nusmp:magic_tool_sword", "intro", "Named weapons",
        "Beside the relics there are named blades: the Demon-Slayer of the Genesis grade, the wizard-king's Elsdocia, the Rimeheart Runeblade, "
        "the Otherworld Trident. Each carries Tensura engravings that fit its lore.", tasks=[T.adv("weapons/first")])
    c.q("mana_tools", "Forged of Mana", (1, 8), "nusmp:magic_tool_bow", "step", "Sword, spear and bow",
        "Carry the mana-forged sword, spear and bow.", deps=["w_first"], tasks=[T.adv("weapons/mana_tools")])
    c.q("engraved", "Engraved", (2, 8), "minecraft:grindstone", "step", "Tensura engravings",
        "Engrave a weapon at the Relic Forge. Severance, barrier piercing, holy weapon and magic interference are engravings of Tensura Reincarnated.",
        deps=["w_first"], tasks=[T.adv("spells/engraved")])
    c.q("katana", "A Cut Through Magic", (1, 9.5), "nusmp:demon_slasher_katana", "step", "The slasher's katana",
        "Carry the Demon Slasher Katana. It cuts through magic and barriers alike.", deps=["w_first"], tasks=[T.adv("weapons/katana")])
    c.q("miasma", "Miasma Katana", (2, 9.5), "nusmp:miasma_infused_katana", "step", "A dimension-cutting darkness",
        "Carry the Miasma-Infused Katana.", deps=["katana"], tasks=[T.adv("weapons/miasma")])
    c.q("rapier", "Spell-Forged Rapier", (1, 11), "nusmp:spell_forged_rapier", "step", "A spatial edge",
        "Carry the Spell-Forged Rapier.", deps=["w_first"], tasks=[T.adv("weapons/rapier")])
    c.q("greatsword", "Sword Rain", (2, 11), "nusmp:severing_greatsword", "step", "A heavy cleaver",
        "Carry the Severing Greatsword.", deps=["rapier"], tasks=[T.adv("weapons/greatsword")])
    c.q("runeblade", "Rimeheart Runeblade", (3, 8), "nusmp:rimeheart_runeblade", "step", "A cryo mana lattice",
        "Carry the Rimeheart Runeblade.", deps=["w_first"], tasks=[T.adv("weapons/runeblade")])
    c.q("demon_slayer", "The Demon-Slayer", (4, 8), "nusmp:demon_slayer_sword", "mile", "The Genesis blade",
        "Carry the Demon-Slayer Sword, the Genesis grade anti-magic blade. In hand it is a full three-dimensional weapon with a glow of its own.",
        deps=["runeblade"], tasks=[T.adv("weapons/demon_slayer")])
    c.q("genesis", "Genesis Edge", (5, 8), "nusmp:demon_slayer_sword", "boss", "Awaken the Genesis power",
        "Strike with the Genesis Demon-Slayer.", deps=["demon_slayer"], tasks=[T.adv("weapons/genesis")])
    c.q("trio", "Three Anti-Magic Blades", (4, 9.5), "nusmp:demon_dweller_sword", "mile", "Slayer, Dweller, Destroyer",
        "Carry the Demon-Slayer, the Demon-Dweller and the Demon-Destroyer.", deps=["demon_slayer"], tasks=[T.adv("weapons/anti_magic_trio")])
    c.q("licht", "Blades of Light", (5, 9.5), "nusmp:licht_dweller_sword", "step", "Licht's white blades",
        "Carry the Licht Dweller and the Licht Destroyer.", deps=["trio"], tasks=[T.adv("weapons/licht")])
    c.q("elsdocia", "Elsdocia", (4, 11), "nusmp:elsdocia", "mile", "The wizard kings' blade",
        "Carry Elsdocia, the blade of the wizard kings. Its engravings are legacy of the wizard kings, magic interference, magicule "
        "absorption and barrier piercing.", deps=["w_first"], tasks=[T.adv("weapons/elsdocia")])
    c.q("trident", "Otherworld Trident", (5, 11), "nusmp:otherworld_trident", "step", "Zagred's trident",
        "Carry the Otherworld Trident.", deps=["elsdocia"], tasks=[T.adv("weapons/trident")])
    c.q("last_word", "The Last Word", (6, 11), "nusmp:last_word", "step", "A quill-blade",
        "Carry the quill-blade Last Word.", deps=["trident"], tasks=[T.adv("weapons/last_word")])
    c.q("null_blade", "Null Blade", (6, 9.5), "nusmp:null_blade", "side", "Nothing and then nothing",
        "Carry the Null Blade.", deps=["trio"], tasks=[T.adv("weapons/null_blade")])
    c.q("ten_engravings", "Ten Engravings", (3, 12.5), "minecraft:netherite_sword", "final", "An armoury, engraved",
        "Engrave ten weapons at the forge. You are the owner of an armoury the ledger itself would envy.",
        deps=["engraved", "elsdocia"], tasks=[T.adv("weapons/armory")], rewards=[R.xp(250), R.choice("choice_knight")])


# ============================================================================================================== dungeon
def dungeon(reg):
    c = reg.chapter("dungeon_labyrinth", "journey", "Dungeon and Labyrinth", "nusmp:labyrinth_gate",
                    "The dungeon under the Gateway, its floor guardians, and what is cooked in it.")
    c.q("welcome", "Where the Dungeon Is Alive", (0, 2), "nusmp:labyrinth_gate", "intro", "One place, one rule: nothing is free",
        "The Multiverse has one dungeon: the Labyrinth, a place that grows as you go, reached through the Gateway on the surface, with trials, "
        "wardens, keys and a vault at the end of every stretch.\n"
        "It cannot be mined away. The only ways on are the ones the designer left.", tasks=[T.check("Pack your bag")])
    c.q("entered", "Dungeon Delver", (1, 1), "minecraft:lantern", "step", "Enter any dungeon",
        "Enter a dungeon of the Multiverse by any door.", deps=["welcome"], tasks=[T.adv("dungeon/entered")])
    c.q("meat", "Delicious in Dungeon", (4, 1), "nusmp:purified_monster_meat", "step", "Cook what you kill",
        "Monster meat is poison raw. Purify it, cook it, and eat. The dungeon feeds those who cook.", deps=["entered"],
        tasks=[T.adv("dungeon/meat")], rewards=[R.xp(25), R.item("minecraft:honey_bottle", 2)])
    c.q("basilisk", "Basilisk Fillet", (5, 0.5), "nusmp:roast_basilisk_fillet", "side", "A delicacy",
        "Eat a roast basilisk fillet.", deps=["meat"], tasks=[T.adv("dungeon/basilisk")])
    c.q("direwolf", "Direwolf Haunch", (5, 1.5), "nusmp:roast_direwolf_haunch", "side", "A hearty haunch",
        "Eat a roast direwolf haunch.", deps=["meat"], tasks=[T.adv("dungeon/direwolf")])
    c.q("goliath", "Pit Guardian", (4, 3), "minecraft:heavy_core", "boss", "Goliath",
        "A stone giant guards a floor of the dungeon. Defeat Goliath.", deps=["entered"],
        tasks=[T.adv("dungeon/goliath")], rewards=[R.xp(150), R.loot("boss_trophy")])
    c.q("charybdis", "The Whirlpool", (4, 4.5), "minecraft:nautilus_shell", "boss", "Spawn of Charybdis",
        "In flooded halls the Spawn of Charybdis waits. Defeat it.", deps=["entered"], tasks=[T.adv("dungeon/charybdis")],
        rewards=[R.xp(150), R.loot("boss_trophy")])
    c.q("both_floors", "Both Floors Clear", (6, 3.75), "minecraft:beacon", "final", "Goliath and Charybdis",
        "Both floor bosses defeated. The dungeon recognises you.", deps=["goliath", "charybdis"],
        tasks=[T.adv("dungeon/both_floors")], rewards=[R.xp(300), R.choice("choice_delver")])
    # labyrinth
    c.q("gate", "The Labyrinth", (1, 6), "nusmp:labyrinth_gate", "mile", "A dungeon of sealed doors",
        "The Labyrinth is reached through the Gateway that stands on the surface. Cross it.", deps=["welcome"], tasks=[T.dim("nusmp:mega_dungeon")],
        rewards=[R.xp(60), R.loot("dungeon_cache")])
    c.q("gate_event", "Gate of the Labyrinth", (2, 6), "nusmp:labyrinth_gate", "step", "Past the threshold",
        "Pass the gate and stand on the first floor.", deps=["gate"], tasks=[T.adv("dungeon/labyrinth_event")])
    c.q("archway", "Sealed Archway", (3, 5), "nusmp:sealed_archway", "side", "Trial doors",
        "A sealed archway opens only when its trial is done. Carry one.", deps=["gate_event"], tasks=[T.adv("dungeon/archway")])
    c.q("traps", "Tabletop Traps", (3, 7), "nusmp:pitfall_tile", "side", "Pitfalls, glyphs, riddles",
        "The Labyrinth speaks tabletop: pitfall tiles, snuffer glyphs, riddle stones. Carry one.", deps=["gate_event"],
        tasks=[T.adv("dungeon/traps")])
    c.q("warden", "Warden Down", (4, 6), "minecraft:iron_bars", "boss", "The floor's warden",
        "Every floor has a Warden who keeps its door. Defeat one.", deps=["gate_event"], tasks=[T.adv("dungeon/warden")])
    c.q("shadow_key", "The Shadow Key", (5, 6), "nusmp:shadow_key", "mile", "A key that eats light",
        "The Warden's key opens one door in the dark. Carry a Shadow Key.", deps=["warden"], tasks=[T.adv("dungeon/shadow_key")])
    c.q("seal", "A Seal Opens", (6, 6), "minecraft:tripwire_hook", "mile", "Turn the key",
        "Open a seal of the Labyrinth with the key.", deps=["shadow_key"], tasks=[T.adv("dungeon/seal")])
    c.q("altar", "Peak Altar", (7, 5), "nusmp:peak_altar", "side", "The altar at the peak",
        "Carry a Peak Altar, the altar where the floor's master is called.", deps=["seal"], tasks=[T.adv("dungeon/peak_altar")])
    c.q("vault", "The Vault", (8, 6), "nusmp:relic_pedestal", "final", "At the top of the spire",
        "Open the vault at the top. Relics wait on its pedestals, and each can be taken once for all time.", deps=["seal"],
        tasks=[T.adv("dungeon/vault")], rewards=[R.xp(300), R.choice("choice_delver")])


# ============================================================================================================== spirits
def spirits(reg):
    c = reg.chapter("spirits_devils", "journey", "Spirits and Devils", "nusmp:spirit_charm",
                    "Contracts with spirits, bargains with devils, and what each one costs.")
    c.q("welcome", "Powers That Answer", (0, 2), "nusmp:spirit_charm", "intro", "Spirit or devil?",
        "Some mages do not rely on their own magic alone. A Spirit Lord lends its element; a devil lends its power at a price.\n"
        "Both give extra pages for your grimoire. Neither is free: a spirit asks for trust, a devil for a contract read last.",
        tasks=[T.check("Listen")])
    c.q("charm", "Spirit Charm", (1, 1), "nusmp:spirit_charm", "step", "The key to the spirits",
        "Carry a Spirit Charm. Use it to call a small spirit; sneak-use to choose the element.", deps=["welcome"], tasks=[T.adv("spirits/charm")])
    c.q("skill", "Spirit Lord", (2, 1), "nusmp:spirit_lord_skill", "mile", "The skill itself",
        "Carry the Spirit Lord skill.", deps=["charm"], tasks=[T.adv("spirits/lord_skill")])
    c.q("elemental", "True Body of an Element", (2, 0), "nusmp:spirit_core_fire", "step", "Salamander, Undine, Sylph, Gnome",
        "Four spirit lords have true bodies, and only one mage per element can call each. Meet one.", deps=["charm"],
        tasks=[T.adv("spirits/meet_elemental")])
    c.q("cores", "Four Spirit Cores", (3, 0), "nusmp:spirit_core_earth", "boss", "Fire, water, wind, earth",
        "Carry the fire, water, wind and earth spirit cores.", deps=["elemental"], tasks=[T.adv("spirits/cores")],
        rewards=[R.xp(100), R.loot("arc_cache")])
    c.q("contract", "A Spirit Answers", (3, 1), "minecraft:soul_lantern", "mile", "Sign with a spirit",
        "Sign a contract with a spirit. It gives you more room in your grimoire: four extra open pages.", deps=["skill"],
        tasks=[T.adv("spirits/contract")])
    c.q("wind_lord", "Lord of the Gale", (4, 1), "minecraft:feather", "boss", "The Wind Spirit Lord",
        "The Wind Spirit Lord has proven too stubborn to be called. Defeat it.", deps=["contract"], tasks=[T.adv("bosses/wind_spirit_lord")])
    c.q("devil_contract", "A Devil's Contract", (1, 3), "nusmp:devil_contract", "step", "Read the last line first",
        "Carry a devil contract. The price is always on the last line.", deps=["welcome"], tasks=[T.adv("spirits/devil_contract")])
    c.q("devil_core", "A Devil's Core", (2, 3), "nusmp:devils_core", "mile", "Eight cores and one",
        "Carry a devil core. Zagred, Megicula, Lucifero, Astaroth, Liebe, Lilith, Nahamah, Walgner and Beelzebub each have one.",
        deps=["devil_contract"], tasks=[T.adv("spirits/devil_core")])
    c.q("devil_union", "Devil Union", (3, 3), "nusmp:devil_horn", "boss", "Fuse with a devil",
        "Unite with a devil. The devil's power is yours and so is its hunger.", deps=["devil_contract"], tasks=[T.adv("spirits/devil_union")],
        rewards=[R.xp(300), R.loot("legend_trophy")])
    c.q("corruption", "Soul Corruption", (4, 3), "minecraft:wither_skeleton_skull", "side", "The first crack",
        "Feel your soul corrode. It is how a devil's price shows up.", deps=["devil_union"], tasks=[T.adv("spirits/soul_corruption")])
    c.q("paladin_mark", "Soul Paladin", (4, 4), "minecraft:totem_of_undying", "side", "Marked a paladin",
        "Be marked a paladin.", deps=["devil_contract"], tasks=[T.adv("spirits/paladin")])
    c.q("all_cores", "Nine Devil Cores", (5, 3), "nusmp:devil_core_beelzebub", "final", "The whole set",
        "Carry all nine devil cores at once. Choose what to do with a devil's whole choir.", deps=["devil_core"],
        tasks=[T.adv("spirits/devil_cores_all")], rewards=[R.xp(1000), R.loot("legend_trophy")])


# ============================================================================================================== squads
def squads(reg):
    c = reg.chapter("squads_kingdoms", "journey", "Squads and Kingdoms", "minecraft:white_banner",
                    "Magic Knight squads on FTB Teams, the four kingdoms and the arena of the Final Hour.")
    c.q("welcome", "Magic Knights Do Not Fight Alone", (0, 2), "minecraft:white_banner", "intro", "Squads are FTB Teams",
        "A Magic Knight squad is one FTB Teams party flagged as a squad. The captain is the team owner, the vice-captain is chosen by the "
        "captain, everyone else is a member. Quest progress in this book is shared across the team: when anyone completes a quest task, "
        "the whole squad has it.\nSquads also keep a star score and show up on the standings.", tasks=[T.check("Read the squad rules")])
    c.q("created", "A Squad of Your Own", (1, 1), "minecraft:white_banner", "mile", "Found a squad",
        "Found a Magic Knight squad. You need the rank to create one.", deps=["welcome"], tasks=[T.adv("squads/created")])
    c.q("joined", "Join the Ranks", (1, 3), "minecraft:red_banner", "mile", "Or join one",
        "Join a squad someone else has founded.", deps=["welcome"], tasks=[T.adv("squads/joined")])
    c.q("captain", "Captain", (2, 1), "minecraft:black_banner", "step", "The team owner",
        "Be named the captain of a squad.", deps=["created"], tasks=[T.adv("squads/captain")])
    c.q("vice", "Vice-Captain", (3, 1), "minecraft:shield", "step", "Second in command",
        "Name a vice-captain, or become one.", deps=["captain"], tasks=[T.adv("squads/vice")], dep_any=False)
    c.q("treasury", "Squad Treasury", (4, 1.5), "nusmp:magic_stone", "mile", "Everyone chips in",
        "Submit 256 magic stones to the squad's treasury. Item tasks are shared by the whole team: every member can put stones in, "
        "and the quest completes when the total reaches 256.", deps=["captain", "joined"],
        tasks=[T.item("nusmp:magic_stone", 256, consume=True)], rewards=[R.xp(100, True), R.item("minecraft:emerald", 16, team=True),
                                                                                 R.loot("dungeon_cache")])
    c.q("standings", "On the Standings", (5, 1.5), "minecraft:lectern", "step", "Squad stars",
        "Squads earn stars and are ranked on the standings. Look up your squad's place with the squad standings command, and tick this "
        "off once you are on the list.", deps=["treasury"], tasks=[T.check("I am on the standings")])
    c.q("rank_one", "First Place", (6, 1.5), "minecraft:beacon", "boss", "Top of the standings",
        "Be first on the standings. It is a promise between the squad and the book: tick it when it is true.", deps=["standings"],
        tasks=[T.check("My squad is first")], rewards=[R.xp(300, True), R.item("minecraft:diamond", 8, team=True)], optional=True)
    c.q("charter", "A Written Canon", (3, 3), "nusmp:charter_grimoire_goliath", "step", "Boss canons",
        "A canon grimoire marks a boss hunt as a squad affair. Carry one.", deps=["created", "joined"], dep_any=True,
        tasks=[T.adv("squads/charter")])
    c.q("charter_all", "The Complete Canon", (4, 3), "nusmp:charter_grimoire_lucius", "boss", "Seven canons",
        "All seven boss canons, carried by the squad.", deps=["charter"], tasks=[T.adv("squads/charter_all")],
        rewards=[R.xp(300), R.loot("legend_trophy")], optional=True)
    # team-wide quests: the tasks are split between members, the progress and the claim are the team's
    c.q("hunt_floors", "Squad Hunt: The Floors", (6, 3), "minecraft:heavy_core", "mile", "Split the work",
        "One member takes Goliath, another the Charybdis spawn, a third a Warden. Quest progress belongs to the whole FTB team, so the "
        "quest completes when each of the three has fallen to anyone in the squad.", deps=["created", "joined"], dep_any=True,
        tasks=[T.adv("bosses/goliath"), T.adv("bosses/charybdis_spawn"), T.adv("bosses/floor_warden")],
        rewards=[R.xp(150), R.item("minecraft:diamond", 4, team=True), R.loot("dungeon_cache")])
    c.q("hunt_triad", "Squad Hunt: The Triad", (7, 3), "nusmp:charter_grimoire_dante", "boss", "Three siblings, three members",
        "Dante, Vanica and Zenon in one night. Any member can land any of the kills; the squad needs all three.", deps=["hunt_floors"],
        tasks=[T.adv("bosses/dante_zogratis"), T.adv("bosses/vanica_zogratis"), T.adv("bosses/zenon_zogratis")],
        rewards=[R.xp(400), R.item("minecraft:netherite_scrap", 4, team=True), R.loot("legend_trophy")], optional=True)
    c.q("hunt_devils", "Squad Hunt: The Devils", (8, 3), "nusmp:devil_horn", "boss", "Zagred and the Avatar",
        "Both devils the Multiverse has made, shared between the members of the squad.", deps=["hunt_triad"],
        tasks=[T.adv("bosses/zagred"), T.adv("bosses/devil_avatar")], rewards=[R.xp(500), R.item("minecraft:nether_star", 1, team=True)],
        optional=True)
    c.q("armoury", "Squad Armoury", (4, 0), "minecraft:iron_ingot", "step", "Pool the iron",
        "Submit 128 iron ingots. Like the treasury, this is a team item task: the squad as a whole fills it, one member at a time.",
        deps=["created", "joined"], dep_any=True, tasks=[T.item("minecraft:iron_ingot", 128, consume=True)],
        rewards=[R.xp(60), R.item("minecraft:golden_apple", 6, team=True)])
    c.q("squad_library", "Squad Library", (5, 0), "minecraft:book", "step", "Pool the lapis",
        "Submit 256 lapis lazuli for the squad's library. Anything a member submits counts for all of them.", deps=["armoury"],
        tasks=[T.item("minecraft:lapis_lazuli", 256, consume=True)], rewards=[R.xp(60), R.item("minecraft:experience_bottle", 24, team=True)])
    c.q("top_ten", "Standings: Top Ten", (6, 0.5), "minecraft:lectern", "step", "Honour system",
        "Reach the top ten of the squad standings. This one cannot be seen by the book: tick it when it is true.", deps=["standings"],
        tasks=[T.check("Top ten")], optional=True)
    c.q("top_three", "Standings: Top Three", (7, 0.5), "minecraft:lectern", "mile", "Honour system",
        "Reach the top three.", deps=["top_ten"], tasks=[T.check("Top three")], optional=True, rewards=[R.xp(200), R.item("minecraft:diamond", 4, team=True)])
    # kingdoms
    c.q("kingdoms", "Four Kingdoms", (0, 5), "minecraft:white_banner", "mile", "Clover, Spade, Heart, Diamond",
        "Every grimoire carries the suit of the kingdom that accepted it. Receive yours.", deps=["welcome"], tasks=[T.adv("world/kingdoms")])
    xs = [("clover", "Clover Kingdom", "minecraft:oak_sapling"), ("spade", "Spade Kingdom", "minecraft:black_banner"),
          ("heart", "Heart Kingdom", "minecraft:red_banner"), ("diamond", "Diamond Kingdom", "minecraft:diamond")]
    for i, (k, ttl, icon) in enumerate(xs):
        c.q("k_" + k, ttl, (1.5 + i, 5), icon, "side", "A grimoire of this suit",
            "Receive a grimoire from the %s." % ttl, deps=["kingdoms"], tasks=[T.adv("world/kingdom_" + k)])
    c.q("all_kingdoms", "Citizen of All Four", (6, 5), "minecraft:beacon", "boss", "One of each",
        "A grimoire from each of the four kingdoms. Rare, and a little suspicious.", deps=["k_clover", "k_spade", "k_heart", "k_diamond"],
        tasks=[T.adv("world/all_kingdoms")], rewards=[R.xp(300), R.loot("legend_trophy")], optional=True)
    c.q("library", "Grimoire Library", (1, 7), "minecraft:lectern", "step", "The library dimension",
        "Enter the Grimoire Library dimension, where the wizards keep their books.", deps=["kingdoms"], tasks=[T.dim("nusmp:grimoire_library")])
    c.q("dream", "The Dream World", (2.5, 7), "minecraft:glow_ink_sac", "step", "Dream Magic's place",
        "Enter the Dream dimension.", deps=["kingdoms"], tasks=[T.dim("nusmp:dream")])
    c.q("codex", "Compat Codex", (4, 7), "nusmp:compat_codex", "side", "Cross-mod arts",
        "Carry the Compat Codex, the book of cross-mod arts.", deps=["library"], tasks=[T.adv("world/codex")])
    c.q("final", "Squad Victory", (9.5, 3), "minecraft:totem_of_undying", "final", "The last page of the chapter",
        "You have a squad, a treasury and a place in the kingdoms. A bigger fight is waiting: the arena of the Final Hour has a chapter of "
        "its own in this book.", deps=["treasury", "kingdoms"], tasks=[T.check("Ready for the Final Hour")],
        rewards=[R.xp(150), R.choice("choice_knight")])
