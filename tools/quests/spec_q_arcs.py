"""Group "The Anime Arcs": one chapter per main arc of the story, in order.

Why: the owner wants every main arc of the series as a questline that ends in its boss. Where the boss exists in the mod the boss quest is live
(an advancement fired by the boss' death); where it does not, the boss quest is a locked placeholder on a hidden "future" advancement
(`nusmp:future/<arc>_<slot>`), so adding the boss later only needs its death hook to award that advancement (see docs/quests.md).
"""
from spec_qlib import T, R

ARC_ORDER = []   # (chapter key, final quest key) in order, used for the gating between arcs


def soon(c, key, title, at, arc, slot, deps, text):
    """A placeholder boss quest: locked, on a future advancement that nothing awards yet."""
    return c.q(key, title, at, "minecraft:barrier", "soon", "Awaiting the boss",
               text + "\nThis boss is not in the Multiverse yet. The quest completes by itself on the day the boss is added and defeated.",
               deps=deps, tasks=[T.adv("future/%s_%s" % (arc, slot))], rewards=[R.xp(150), R.loot("boss_trophy")])


def start(reg, key, title, icon, sub, intro, n, prev=None):
    c = reg.chapter(key, "arcs", "Arc %d: %s" % (n, title), icon, sub)
    deps = []
    if prev:
        deps = [prev[0] + "/welcome"]
        if n in (2, 3, 13):
            deps.append(prev[0] + "/" + prev[1])
    c.q("welcome", title, (0, 1), icon, "intro", "Arc %d" % n, intro, deps=deps, tasks=[T.check("Begin the arc")])
    return c


def build(reg):
    ARC_ORDER.clear()
    arc1(reg)
    arc2(reg)
    arc3(reg)
    arc4(reg)
    arc5(reg)
    arc6(reg)
    arc7(reg)
    arc8(reg)
    arc9(reg)
    arc10(reg)
    arc11(reg)
    arc12(reg)
    arc13(reg)


def prev_of(n):
    return ARC_ORDER[n - 2] if n >= 2 else None


# ---------------------------------------------------------------------------------------------------------- 1 Hage
def arc1(reg):
    c = start(reg, "arc01_hage", "Hage Village", "nusmp:grimoire", "Fifteen years old, a grimoire, and a promise.",
              "At the edge of the Clover Kingdom lies a village called Hage, and in it two orphans who made a promise: to become Wizard King. "
              "One has no magic at all. The other has more than anyone his age.\nThis arc is the day the grimoires choose.", 1)
    c.q("tower", "The Tower on the Hill", (1, 1), "minecraft:book", "step", "Find the wizard",
        "Every mage starts at a Grimoire Tower. Find one and speak to the wizard who keeps it.", deps=["welcome"],
        tasks=[T.adv("start/meet_wizard")])
    c.q("chosen", "The Grimoire Chooses", (2, 1), "nusmp:grimoire_altar", "mile", "A grimoire of your own",
        "On the day of the ceremony the grimoires choose. Some mages receive a three-leaf, a few a four-leaf, and now and then a mage "
        "receives something nobody has seen before.", deps=["tower"],
        tasks=[T.adv("start/ceremony"), T.adv("arcs/hage_village")])
    c.q("first_cast", "Your First Spell", (3, 1), "minecraft:blaze_powder", "step", "Cast",
        "Open the book and cast. The first spell is never the strongest. It is the one you remember.", deps=["chosen"],
        tasks=[T.adv("start/first_spell")])
    c.q("training", "Training in the Village", (4, 1), "minecraft:experience_bottle", "step", "Fifty casts",
        "Train until casting feels like breathing. Fifty casts will do for a start.", deps=["first_cast"], tasks=[T.adv("start/casts_50")])
    c.q("promise", "A Promise Under the Stars", (2, 0), "minecraft:white_banner", "side", "Find a rival",
        "Every mage worth the name has a rival. Find yours, speak, and promise something you cannot take back.", deps=["chosen"],
        tasks=[T.check("Make the promise")])
    c.q("stones", "Pocket Money", (3, 2), "nusmp:magic_stone", "side", "Sixteen stones",
        "A village boy can do a lot with a handful of magic stones. Gather sixteen.", deps=["first_cast"], tasks=[T.item("nusmp:magic_stone", 16)])
    c.q("final", "Bound by a Grimoire", (5, 1), "nusmp:grimoire", "final", "Five open pages",
        "You have a grimoire, a first spell and the habit of training. The village is behind you; the capital is ahead.\n"
        "Unlock five pages of your grimoire and take a reward for the road.", deps=["training"], tasks=[T.adv("start/pages_5")],
        rewards=[R.xp(100), R.choice("choice_mage")])
    ARC_ORDER.append(("arc01_hage", "final"))


# ----------------------------------------------------------------------------------------------------- 2 Entrance exam
def arc2(reg):
    c = start(reg, "arc02_exam", "Magic Knights Entrance Exam", "minecraft:enchanted_book", "Written, practical, and the one nobody expects.",
              "The Magic Knights take the best mages of the Clover Kingdom and sort them into squads. First they have to pass the exam: a written "
              "test, a practical test, and a hundred rivals.", 2, prev_of(2))
    c.q("register", "Registration", (1, 1), "minecraft:enchanted_book", "step", "Answer the wizard",
        "Registration is a questionnaire. Answer every question honestly: the wizard reads the rest from your grimoire.", deps=["welcome"],
        tasks=[T.adv("start/questionnaire"), T.adv("arcs/entrance_exam")])
    c.q("written", "The Written Exam", (2, 1), "minecraft:lectern", "step", "Study at the library",
        "Studying happens in the Grimoire Library dimension, where the wizards keep their books. Go there and read.", deps=["register"],
        tasks=[T.dim("nusmp:grimoire_library")])
    c.q("practical", "The Practical Exam", (3, 1), "minecraft:experience_bottle", "step", "Fifty casts",
        "The examiners watch your casting. Fifty spells, accurately aimed.", deps=["written"], tasks=[T.adv("start/casts_50")])
    c.q("pages", "Show Your Pages", (4, 1), "minecraft:written_book", "mile", "Ten pages",
        "A mage's grimoire is read in the exam. Ten open pages show real mastery.", deps=["practical"], tasks=[T.adv("start/pages_10")])
    c.q("signature", "A Spell to Be Known By", (4, 0), "minecraft:nether_star", "side", "A signature page",
        "Cast a signature page in front of the examiners. It is not required. It is remembered.", deps=["practical"],
        tasks=[T.adv("spells/signature")])
    c.q("study", "Study Group", (2, 2), "minecraft:white_banner", "side", "Found a squad",
        "Mages who study together pass together. Found a squad, even a small one.", deps=["register"], tasks=[T.adv("squads/created")])
    c.q("final", "Junior Magic Knight", (5, 1), "minecraft:shield", "final", "Admitted",
        "You passed. A robe, a rank and a squad lie ahead.", deps=["pages"], tasks=[T.adv("start/mastery")],
        rewards=[R.xp(150), R.choice("choice_knight")])
    ARC_ORDER.append(("arc02_exam", "final"))


# ------------------------------------------------------------------------------------------------------------ 3 Dungeon
def arc3(reg):
    c = start(reg, "arc03_dungeon", "The Dungeon Arc", "nusmp:labyrinth_gate", "The dungeon opened, and it was alive.",
              "A Dungeon appeared beneath the world, and the Magic Knights were sent to clear it. In this world the dungeon is a place that "
              "grows floor by floor, guards its stone, and feeds those who learn to cook.", 3, prev_of(3))
    c.q("rumour", "Rumours of a Dungeon", (1, 1), "minecraft:lantern", "step", "Enter",
        "Enter any dungeon of the Multiverse.", deps=["welcome"], tasks=[T.adv("dungeon/entered"), T.adv("arcs/dungeon_arc")])
    c.q("cook", "Delicious in Dungeon", (3, 2), "nusmp:purified_monster_meat", "side", "Eat",
        "Cook monster meat and eat it. The best way to survive is to eat what lives here.", deps=["rumour"], tasks=[T.adv("dungeon/meat")])
    c.q("goliath", "Pit Guardian: Goliath", (4, 1), "minecraft:heavy_core", "boss", "The first guardian",
        "A stone giant guards a floor of the dungeon. Defeat Goliath.", deps=["rumour"], tasks=[T.adv("bosses/goliath")])
    soon(c, "vetto", "Vetto", (5, 0), "dungeon_arc", "vetto", ["goliath"], "The Dungeon Arc ends with Vetto.")
    c.q("final", "Dungeon Cleared", (5, 1.8), "nusmp:labyrinth_gate", "final", "Out alive",
        "You came back up. The Magic Knights have a new story to tell.", deps=["goliath"], tasks=[T.adv("dungeon/goliath")],
        rewards=[R.xp(150), R.choice("choice_delver")])
    ARC_ORDER.append(("arc03_dungeon", "final"))


# ----------------------------------------------------------------------------------------------------------- 4 Dark Triad
def arc4(reg):
    c = start(reg, "arc04_triad", "Dark Triad: The Royal Capital", "minecraft:nether_star", "A signature spell to defend the capital.",
              "A raid on the Royal Capital calls every Magic Knight to the walls. This is the arc where a mage's signature spell stops being "
              "a page and becomes a name.", 4, prev_of(4))
    c.q("call", "The Capital Calls", (1, 1), "minecraft:bell", "step", "Cast a signature page",
        "The bell rings for every knight. Answer it with a signature page.", deps=["welcome"],
        tasks=[T.adv("spells/signature"), T.adv("arcs/dark_triad")])
    c.q("ten", "Known by Name", (2, 1), "minecraft:beacon", "step", "Ten signatures",
        "Cast signature pages ten times.", deps=["call"], tasks=[T.adv("spells/signature_10")])
    c.q("relic", "A Relic of Many Worlds", (3, 1), "nusmp:bag_of_holding", "step", "Arm yourself",
        "Obtain a relic. The capital's armoury has opened its doors, in a manner of speaking.", deps=["ten"], tasks=[T.adv("relics/first")])
    c.q("wear", "Attuned", (4, 1), "nusmp:ring_of_power", "step", "Wear it",
        "Wear or wield a relic.", deps=["relic"], tasks=[T.adv("relics/equipped")])
    c.q("squad", "Hold the Wall Together", (3, 2), "minecraft:white_banner", "side", "Squads defend",
        "Found a squad: the capital is defended by squads.", deps=["ten"], tasks=[T.adv("squads/created")])
    soon(c, "champion", "The Capital Raid's Champion", (5, 0), "dark_triad", "boss", ["wear"], "The raid on the capital ends with a champion.")
    c.q("final", "Defenders of the Capital", (5, 1.8), "minecraft:shield", "final", "Held the wall",
        "The walls held. The capital knows your name now.", deps=["wear"], tasks=[T.adv("spells/signature_50")],
        rewards=[R.xp(150), R.choice("choice_knight")])
    ARC_ORDER.append(("arc04_triad", "final"))


# ------------------------------------------------------------------------------------------------------ 5 Eye of Midnight
def arc5(reg):
    c = start(reg, "arc05_eye", "Eye of the Midnight Sun", "nusmp:ring_of_power", "Old treasures, older grudges.",
              "The Eye of the Midnight Sun hunts relics from the oldest age. To match them you will need relics of your own, and the forge to "
              "work them.", 5, prev_of(5))
    c.q("hold", "Hold a Relic", (1, 1), "nusmp:bag_of_holding", "step", "Obtain one",
        "Obtain a relic of any grade.", deps=["welcome"], tasks=[T.adv("relics/first")])
    c.q("wear", "Wear What You Hold", (2, 1), "nusmp:ring_of_power", "step", "Attune",
        "Wear a relic and let it answer.", deps=["hold"], tasks=[T.adv("relics/equipped"), T.adv("arcs/eye_midnight")])
    c.q("forge", "Reforged", (3, 1), "nusmp:relic_forge", "mile", "Work it at the forge",
        "Use a Relic Forge on a relic.", deps=["wear"], tasks=[T.adv("relics/forge_block"), T.adv("relics/forged")])
    c.q("rare", "Rare Grade", (4, 1), "nusmp:ring_of_power", "step", "Rare",
        "Hold a relic of Rare grade.", deps=["forge"], tasks=[T.adv("relics/grade_rare")])
    c.q("socket", "Set the Core", (4, 2), "minecraft:amethyst_shard", "side", "Socket a core",
        "Set a socket core into a relic.", deps=["forge"], tasks=[T.adv("relics/socketed")])
    soon(c, "eye", "The Eye of the Midnight Sun", (5, 0), "eye_midnight", "boss", ["rare"], "The Eye of the Midnight Sun has a leader.")
    c.q("final", "Relic Hunter", (5, 1.8), "minecraft:nether_star", "final", "Unique grade",
        "A relic of Unique grade. The Eye of the Midnight Sun would pay a kingdom for it.", deps=["rare"],
        tasks=[T.adv("relics/grade_unique")], rewards=[R.xp(150), R.choice("choice_boon")])
    ARC_ORDER.append(("arc05_eye", "final"))


# ----------------------------------------------------------------------------------------------------- 6 Witches' Forest
def arc6(reg):
    c = start(reg, "arc06_witches", "The Witches' Forest", "minecraft:string", "Thread, a cat, and a forest that watches back.",
              "Deep in the forest the witches keep to themselves, and their magic is thread and trap. A cat named Rouge is lost, and a forest "
              "full of traps stands between her and her witch.", 6, prev_of(6))
    c.q("thread", "Thread Magic", (1, 1), "minecraft:string", "step", "Cast it",
        "Cast a spell of Thread Magic.", deps=["welcome"], tasks=[T.adv("magic/cast_thread")])
    c.q("trap", "Caught in the Web", (2, 1), "minecraft:string", "step", "Spring a trap",
        "Spring a thread trap. They are everywhere in the forest.", deps=["thread"], tasks=[T.adv("spells/art_thread_trap")])
    c.q("rouge", "Rouge Is Safe", (3, 1), "minecraft:string", "mile", "The lost cat",
        "Save Rouge, the cat of the thread witch. She will thank you in her own way.", deps=["trap"],
        tasks=[T.adv("spells/art_rouge"), T.adv("arcs/witches_forest")])
    c.q("plants", "The Forest Magics", (3, 2), "minecraft:oak_sapling", "side", "Plants of every kind",
        "The forest answers to nature's magics. Cast Plant, Tree, Vine and Briar.", deps=["trap"],
        tasks=[T.adv("magic/cast_plant"), T.adv("magic/cast_tree"), T.adv("magic/cast_vine"), T.adv("magic/cast_briar")])
    c.q("page", "A Witch's Page", (4, 1), "minecraft:writable_book", "mile", "The thread witch's page",
        "Unlock Vanessa Enoteca's thread page. Only a grimoire of the right kind will read it.", deps=["rouge"],
        tasks=[T.adv("spells/page_thread_trap")], optional=True)
    soon(c, "queen", "The Witch Queen", (5, 0), "witches_forest", "boss", ["rouge"], "The forest has a queen.")
    c.q("final", "Out of the Forest", (5, 1.8), "minecraft:string", "final", "Through the thread",
        "You came through the forest with the cat in your arms. That is more than most can say.", deps=["rouge"],
        tasks=[T.adv("spells/special")], rewards=[R.xp(150), R.choice("choice_mage")])
    ARC_ORDER.append(("arc06_witches", "final"))


# ---------------------------------------------------------------------------------------------- 7 Underwater Temple
def arc7(reg):
    c = start(reg, "arc07_temple", "The Underwater Temple", "minecraft:nautilus_shell", "Tides, ice and a whirlpool with a name.",
              "Under the waves stands a temple older than any kingdom. To enter it you will need the magics of water, and a way past what lives "
              "in its basin.", 7, prev_of(7))
    c.q("water", "Water Magic", (1, 1), "minecraft:water_bucket", "step", "Cast it",
        "Cast a spell of Water Magic.", deps=["welcome"], tasks=[T.adv("magic/cast_water"), T.adv("arcs/underwater_temple")])
    c.q("soul", "Water Soul", (2, 1), "minecraft:water_bucket", "step", "Any water magic",
        "Cast any magic of the Water Soul: tide, ice, mercury, mist.", deps=["water"], tasks=[T.adv("magic/soul_water")])
    c.q("ice", "Ice and Mist", (3, 1), "minecraft:powder_snow_bucket", "side", "Frost and fog",
        "Cast Ice Magic and Mist Magic.", deps=["soul"], tasks=[T.adv("magic/cast_ice"), T.adv("magic/cast_mist")])
    c.q("basin", "The Whirlpool", (3, 2), "minecraft:nautilus_shell", "mile", "Spawn of Charybdis",
        "In a flooded basin the Spawn of Charybdis stirs the water. Defeat it.", deps=["soul"], tasks=[T.adv("bosses/charybdis_spawn")])
    soon(c, "guardian", "Guardian of the Temple", (4, 0), "underwater_temple", "boss", ["soul"], "The temple has a guardian of its own.")
    c.q("final", "Out of the Deep", (5, 1.8), "minecraft:conduit", "final", "Surface",
        "The temple let you go. Take something from its hoard.", deps=["basin"], tasks=[T.adv("dungeon/charybdis")],
        rewards=[R.xp(150), R.choice("choice_boon")])
    ARC_ORDER.append(("arc07_temple", "final"))


# ------------------------------------------------------------------------------------------ 8 Royal Knights Selection
def arc8(reg):
    c = start(reg, "arc08_selection", "Royal Knights Selection Exam", "nusmp:labyrinth_gate", "A dungeon of sealed doors, a team of three, a key.",
              "The Royal Knights pick their next members in a tournament of squads and trials. In this world the trial is the Labyrinth: a dungeon "
              "with doors that open only to those who earned them.", 8, prev_of(8))
    c.q("squad", "Form a Team", (1, 1), "minecraft:white_banner", "step", "A squad of three",
        "The selection exam is a team event. Found a squad.", deps=["welcome"], tasks=[T.adv("squads/created")])
    c.q("gate", "Through the Gate", (2, 1), "nusmp:labyrinth_gate", "mile", "Enter the spire",
        "Cross the Gate and enter the Labyrinth.", deps=["squad"], tasks=[T.dim("nusmp:mega_dungeon"), T.adv("arcs/royal_knights")])
    c.q("trap", "Tabletop Traps", (3, 2), "nusmp:pitfall_tile", "side", "Mind the floor",
        "Pitfalls, glyphs and riddle stones decorate the exam. Carry one.", deps=["gate"], tasks=[T.adv("dungeon/traps")])
    c.q("warden", "The Floor's Warden", (3, 1), "minecraft:iron_bars", "boss", "A Warden",
        "Defeat the Warden of a floor.", deps=["gate"], tasks=[T.adv("bosses/floor_warden")])
    c.q("key", "The Shadow Key", (4, 1), "nusmp:shadow_key", "step", "A key from the dark",
        "Carry a Shadow Key.", deps=["warden"], tasks=[T.adv("dungeon/shadow_key")])
    c.q("seal", "A Seal Opens", (5, 1), "minecraft:tripwire_hook", "mile", "Turn it",
        "Open a seal with the key.", deps=["key"], tasks=[T.adv("dungeon/seal")])
    soon(c, "examiner", "The Examiner", (6, 0), "royal_knights", "boss", ["seal"], "The exam has an examiner.")
    c.q("final", "Vault Opened", (6, 1.8), "nusmp:relic_pedestal", "final", "The spire's top",
        "Open the vault at the top. This is how the Royal Knights are chosen.", deps=["seal"], tasks=[T.adv("dungeon/vault")],
        rewards=[R.xp(300), R.choice("choice_delver")])
    ARC_ORDER.append(("arc08_selection", "final"))


# ------------------------------------------------------------------------------------------------------ 9 Spade raid
def arc9(reg):
    c = start(reg, "arc09_spade_raid", "Spade Kingdom Raid", "minecraft:black_banner", "Dante, Vanica and Zenon.",
              "The Spade Kingdom sends its Dark Triad. Three siblings, three kinds of magic, and a bond between them that makes the second fall "
              "harder than the first.", 9, prev_of(9))
    c.q("charter", "A Written Charter", (1, 1), "nusmp:charter_grimoire_dante", "step", "Boss charters",
        "A charter grimoire marks a boss hunt as a squad affair. Carry one.", deps=["welcome"], tasks=[T.adv("squads/charter")])
    c.q("spade", "A Grimoire of Spades", (1, 2), "minecraft:black_banner", "side", "The Spade suit",
        "Receive a grimoire from the Spade Kingdom.", deps=["welcome"], tasks=[T.adv("world/kingdom_spade")])
    c.q("first", "First Blood", (2, 1), "minecraft:iron_sword", "step", "Fell a boss",
        "Defeat any boss of the Multiverse.", deps=["charter"], tasks=[T.adv("bosses/first"), T.adv("arcs/spade_raid")])
    c.q("dante", "Dante, Gravity Incarnate", (3, 0), "nusmp:dante_gravity_crown", "boss", "The gravity of the Triad",
        "Dante bends the weight of everything around him. Defeat him.", deps=["first"], tasks=[T.adv("bosses/dante_zogratis")])
    c.q("vanica", "Vanica, Mistress of Curses", (3, 1), "minecraft:ender_eye", "boss", "The curse-weaver",
        "Vanica's curses brand the unwary. Defeat her.", deps=["first"], tasks=[T.adv("bosses/vanica_zogratis")])
    c.q("zenon", "Zenon, the Spatial Hunter", (3, 2), "nusmp:zenon_spatial_ring", "boss", "He walks between gates",
        "Zenon opens gates and steps between them. Defeat him.", deps=["first"], tasks=[T.adv("bosses/zenon_zogratis")])
    c.q("final", "The Dark Triad Falls", (5, 1), "nusmp:charter_grimoire_zenon", "final", "All three",
        "Dante, Vanica and Zenon are down. Their bond is broken.\nBosses give rare relics: look for the Gravity Crown and the Spatial Ring.",
        deps=["dante", "vanica", "zenon"], tasks=[T.adv("bosses/triad")], rewards=[R.xp(400), R.loot("legend_trophy")])
    ARC_ORDER.append(("arc09_spade_raid", "final"))


# ------------------------------------------------------------------------------------------------ 10 Heart / elves
def arc10(reg):
    c = start(reg, "arc10_heart", "Heart Kingdom: Elf Reincarnation", "minecraft:red_banner", "Spirits, contracts, and what they remember.",
              "The Heart Kingdom keeps the old pacts with the spirits. An elf who was reincarnated remembers something she should not, and the "
              "spirits are not pleased.", 10, prev_of(10))
    c.q("heart", "A Grimoire of Hearts", (1, 2), "minecraft:red_banner", "side", "The Heart suit",
        "Receive a grimoire from the Heart Kingdom.", deps=["welcome"], tasks=[T.adv("world/kingdom_heart")])
    c.q("charm", "Spirit Charm", (1, 1), "nusmp:spirit_charm", "step", "Carry a charm",
        "Carry a Spirit Charm.", deps=["welcome"], tasks=[T.adv("spirits/charm")])
    c.q("lord", "True Body of an Element", (2, 1), "nusmp:spirit_core_fire", "step", "Salamander, Undine, Sylph, Gnome",
        "Meet one of the four elemental Spirit Lords.", deps=["charm"], tasks=[T.adv("spirits/meet_elemental")])
    c.q("contract", "A Spirit Answers", (3, 1), "minecraft:soul_lantern", "mile", "A pact",
        "Sign a contract with a spirit.", deps=["lord"], tasks=[T.adv("spirits/contract"), T.adv("arcs/heart_elf")])
    c.q("cores", "Four Spirit Cores", (4, 1), "nusmp:spirit_core_earth", "mile", "Fire, water, wind, earth",
        "Carry the four spirit cores.", deps=["contract"], tasks=[T.adv("spirits/cores")])
    c.q("gale", "Lord of the Gale", (4, 2), "minecraft:feather", "boss", "The Wind Spirit Lord",
        "Defeat the Wind Spirit Lord.", deps=["contract"], tasks=[T.adv("bosses/wind_spirit_lord")], optional=True)
    soon(c, "elf", "The Reincarnated Elf", (5, 0), "heart_elf", "boss", ["cores"], "The elf's story ends in a fight.")
    c.q("final", "Pacts Kept", (5, 1.8), "nusmp:spirit_lord_skill", "final", "Spirit Lord",
        "Carry the Spirit Lord skill and keep your word.", deps=["cores"], tasks=[T.adv("spirits/lord_skill")],
        rewards=[R.xp(150), R.choice("choice_mage")])
    ARC_ORDER.append(("arc10_heart", "final"))


# ----------------------------------------------------------------------------------------------------- 11 Diamond
def arc11(reg):
    c = start(reg, "arc11_diamond", "The Diamond Kingdom", "minecraft:diamond", "Crystals, mergers and a kingdom that keeps secrets.",
              "The Diamond Kingdom is known for crystals and for merging magics: two spells, one result. Its mages guard their methods "
              "well.", 11, prev_of(11))
    c.q("diamond", "A Grimoire of Diamonds", (1, 2), "minecraft:diamond", "side", "The Diamond suit",
        "Receive a grimoire from the Diamond Kingdom.", deps=["welcome"], tasks=[T.adv("world/kingdom_diamond")])
    c.q("crystal", "Crystal and Corundum", (1, 1), "minecraft:amethyst_shard", "step", "Gem magics",
        "Cast Crystal Magic and Corundum Magic.", deps=["welcome"], tasks=[T.adv("magic/cast_crystal"), T.adv("magic/cast_corundum")])
    c.q("merge", "Two Magics, One Spell", (2, 1), "minecraft:smithing_table", "mile", "First merger",
        "Merge two magics into one spell.", deps=["crystal"], tasks=[T.adv("spells/merged"), T.adv("arcs/diamond_kingdom")])
    c.q("merge5", "Alchemist of Magic", (3, 1), "minecraft:smithing_table", "mile", "Five mergers",
        "Merge magics five times.", deps=["merge"], tasks=[T.adv("spells/merged_5")])
    c.q("mirror", "Mirror and Glass", (3, 2), "minecraft:glass_bottle", "side", "Glass magics",
        "Cast Mirror Magic and Glass Magic.", deps=["merge"], tasks=[T.adv("magic/cast_mirror"), T.adv("magic/cast_glass")])
    soon(c, "champion", "The Diamond Kingdom's Champion", (4, 0), "diamond_kingdom", "boss", ["merge5"], "The Diamond Kingdom keeps a champion.")
    c.q("final", "The Alchemist's Art", (5, 1.8), "minecraft:smithing_table", "final", "Ten pages and a merge",
        "Ten pages of your grimoire and magics that have been merged more than once: a mage the Diamond Kingdom would hire.",
        deps=["merge5"], tasks=[T.adv("start/pages_10")], rewards=[R.xp(150), R.choice("choice_mage")])
    ARC_ORDER.append(("arc11_diamond", "final"))


# --------------------------------------------------------------------------------------------------- 12 Final arc
def arc12(reg):
    c = start(reg, "arc12_final", "Spade Kingdom Invasion: The Final Arc", "nusmp:zogratis_hourglass", "Lucius Zogratis and the Final Hour.",
              "The last of the Zogratis siblings has been waiting. He does not need a bloated bar of health: he is a tactician with a hundred "
              "spells and a clock of his own. The Hourglass of the Final Hour carries a challenger to his arena.", 12, prev_of(12))
    c.q("hourglass", "Hourglass of the Final Hour", (1, 1), "nusmp:zogratis_hourglass", "step", "Carry it",
        "Carry the Hourglass of the Final Hour.", deps=["welcome"], tasks=[T.adv("world/hourglass_item")])
    c.q("turn", "Turn the Glass", (2, 1), "nusmp:zogratis_hourglass", "step", "Begin",
        "Use the Hourglass and let the final hour begin.", deps=["hourglass"], tasks=[T.adv("world/hourglass_used")])
    c.q("arena", "The Arena", (3, 1), "nusmp:arena_return_sigil", "mile", "Arrive",
        "Arrive in the Zogratis Arena. Lucius can be fought only here.", deps=["turn"],
        tasks=[T.dim("nusmp:zogratis_arena"), T.adv("arcs/spade_final")])
    c.q("sigil", "Return Sigil", (3, 2), "nusmp:arena_return_sigil", "side", "A way home",
        "Carry an Arena Return Sigil, the way out.", deps=["turn"], tasks=[T.adv("world/sigil")])
    c.q("stage2", "The Second Stage", (4, 0), "nusmp:zogratis_hourglass", "step", "Three stages, one bar each",
        "Push Lucius into his second stage.", deps=["arena"], tasks=[T.adv("bosses/lucius_stage2")])
    c.q("stage3", "The Third Stage", (5, 0), "nusmp:zogratis_hourglass", "mile", "The last bar",
        "Push Lucius into his third and final stage.", deps=["stage2"], tasks=[T.adv("bosses/lucius_stage3")])
    c.q("lucius", "Lucius, the Final Hour", (5, 1), "nusmp:zogratis_hourglass", "boss", "The last Zogratis",
        "Defeat Lucius Zogratis in his arena.", deps=["arena"], tasks=[T.adv("bosses/lucius_zogratis")],
        rewards=[R.xp(600), R.loot("legend_trophy")])
    c.q("final", "Hour's End", (6, 1), "minecraft:totem_of_undying", "final", "Victory",
        "Win in the arena. The Spade Kingdom's invasion ends here.", deps=["lucius"], tasks=[T.adv("world/arena_victory")],
        rewards=[R.xp(500), R.loot("legend_trophy"), R.choice("choice_knight")])
    ARC_ORDER.append(("arc12_final", "final"))


# ------------------------------------------------------------------------------------------------------- 13 Devils
def arc13(reg):
    c = start(reg, "arc13_devil", "The Devil Arc", "nusmp:devil_horn", "Zagred, the Avatar, and the devils who are not here yet.",
              "Beneath the Multiverse live the devils. They bargain, they rule, and when one is called into the world it is rarely alone.", 13,
              prev_of(13))
    c.q("contract", "A Devil's Contract", (1, 1), "nusmp:devil_contract", "step", "Read the last line",
        "Carry a devil contract.", deps=["welcome"], tasks=[T.adv("spirits/devil_contract")])
    c.q("union", "Devil Union", (2, 1), "nusmp:devil_horn", "mile", "Fuse",
        "Unite with a devil.", deps=["contract"], tasks=[T.adv("spirits/devil_union"), T.adv("arcs/devil_arc")])
    c.q("core", "A Devil's Core", (3, 1), "nusmp:devils_core", "step", "Take the core",
        "Carry a devil core.", deps=["union"], tasks=[T.adv("spirits/devil_core")])
    c.q("zagred", "Zagred, Devil of the Word", (4, 0), "nusmp:last_word", "boss", "The word that kills",
        "Zagred speaks and the world rewrites itself. Defeat him.", deps=["core"], tasks=[T.adv("bosses/zagred")])
    c.q("zagred2", "Zagred Unbound", (5, 0), "nusmp:last_word", "step", "The second phase",
        "Push Zagred into his second phase.", deps=["zagred"], tasks=[T.adv("bosses/zagred_phase2")], optional=True)
    c.q("avatar", "The Devil's Avatar", (4, 2), "nusmp:devil_horn", "boss", "A body for a devil",
        "A devil's avatar walks the world. Defeat it.", deps=["core"], tasks=[T.adv("bosses/devil_avatar")])
    soon(c, "megicula", "Megicula", (5, 3), "devil_arc", "megicula", ["avatar"], "Megicula is the first of the devils.")
    soon(c, "lucifero", "Lucifero", (6, 3), "devil_arc", "lucifero", ["avatar"], "Lucifero is the devil of the third circle.")
    soon(c, "astaroth", "Astaroth", (7, 3), "devil_arc", "astaroth", ["avatar"], "Astaroth is the devil of the second.")
    c.q("final", "The Devils Quelled", (6, 1), "nusmp:devil_core_zagred", "final", "For now",
        "Zagred and the avatar are down. The rest are still waiting; the book will tell you when they come.",
        deps=["zagred", "avatar"], tasks=[T.adv("bosses/devil_avatar")], rewards=[R.xp(500), R.loot("legend_trophy"), R.choice("choice_boon")])
    ARC_ORDER.append(("arc13_devil", "final"))
