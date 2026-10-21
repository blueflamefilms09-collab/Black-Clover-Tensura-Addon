"""Wave 105 (1.111.0, key normal_c): 115 Normal grade charms, tokens, badges, tools of a trade, armour pieces and monster-drop trinkets (the 6 weapons of the same wave are in weapons.py).

Theme of this wave: consumable-style and utility relics (charms, tokens, seals, badges, pouches, lamps, compasses, keys, scroll cases, trade-tool charms), the basic armour pieces (HEAD / CHEST /
FEET) of the worlds of the mod, and the trinkets made from monster drops. Every name is tied to a place, a guild or a creature of its franchise.

The row format is relic_rows_lib.R. The extra key `world` (the franchise it is a homage to) is only for the doc. Normal grade: one or two small real passives OR one simple act at 1% magicules;
nothing here touches food, hunger or exhaustion. Every ability is one of the shapes of Wave31Acts."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from relic_rows_lib import R as _R  # noqa: E402

SLIME = "That Time I Got Reincarnated as a Slime"
BC = "Black Clover"
MAGI = "Magi: The Labyrinth of Magic"
DAN = "Is It Wrong to Try to Pick Up Girls in a Dungeon?"
FT = "Fairy Tail"
SH = "The Rising of the Shield Hero"
DND = "Dungeons and Dragons"
SOLO = "Solo Leveling"
REZERO = "Re:Zero"
KONO = "KonoSuba"
OVL = "Overlord"
SAO = "Sword Art Online"
FATE = "Fate/stay night"
NARUTO = "Naruto"
FMA = "Fullmetal Alchemist"
OP = "One Piece"
HXH = "Hunter x Hunter"
DB = "Dragon Ball"
BLEACH = "Bleach"
FRIEREN = "Frieren: Beyond Journey's End"
MESHI = "Delicious in Dungeon"
YGO = "Yu-Gi-Oh!"
SDS = "The Seven Deadly Sins"
MUSHOKU = "Mushoku Tensei"
GS = "Goblin Slayer"
MIA = "Made in Abyss"
AOT = "Attack on Titan"
MHA = "My Hero Academia"
DS = "Demon Slayer"
MH = "Monster Hunter"
SPICE = "Spice and Wolf"
MASHLE = "Mashle: Magic and Muscles"
JJK = "Jujutsu Kaisen"
TOG = "Tower of God"
SHADOW = "The Eminence in Shadow"
ORIG = "Original"


def _glow(h):
    h = h.lstrip("#")
    c = [int(h[i:i + 2], 16) for i in (0, 2, 4)]
    return "#%02x%02x%02x" % tuple(min(255, int(v + (255 - v) * 0.55)) for v in c)


def N(world, name, slot, look, main, trim, icon, desc, passives="", acts=()):
    r = _R(name, slot, look, "NORMAL", (main, trim, _glow(main)), icon, desc, passives, list(acts))
    r["world"] = world
    return r


ROWS = [
    # ------------------------------------------------------------------ CHARM: trade-tool charms, drops, tokens, consumables
    N(GS, "Goblin Slayer's Tooth Tally", "CHARM", "PENDANT", "#6a6258", "#c8c0a8", "tooth",
      "A cord of small yellowed goblin teeth, one for every nest cleared. Its wearer remembers every one of them and swings a little harder for it.", "a:atk:+0.5"),
    N(SLIME, "Orc Disaster Tusk Charm", "CHARM", "CHARM", "#8a6a48", "#e8dcc0", "tooth",
      "A curved tusk from the great swarm of orcs, carved with a ward against bad luck of the wrong kind. It makes the body stubborn against a shove.", "a:kb:+0.08"),
    N(SLIME, "Lizardman Marsh Scale Charm", "CHARM", "AMULET", "#3a7a5a", "#a8d8b0", "gem",
      "A single green scale from a lizardman of the marsh clans, polished until it shows the sky. It turns a glancing blow like a good plate.", "a:armor:+1"),
    N(SLIME, "Starwolf Fang Pendant", "CHARM", "PENDANT", "#6a7a9a", "#e8f0ff", "tooth",
      "A fang of the storm wolves that run the Great Jura Forest, tied with blue thread. It makes the owner's step quick and light.", "a:speed:*0.03"),
    N(SLIME, "Hobgoblin Warband Token", "CHARM", "CHARM", "#7a5a3a", "#c8a068", "coin",
      "A clay token pressed with the mark of a hobgoblin warband. Holding it up makes nearby friends fall into step.", "",
      [("Warband Call", 90, 1, "allies:6:speed:6:0", "You raise the token and your friends quicken.")]),
    N(SLIME, "Dwargon Smith's Hammer Charm", "CHARM", "CHARM", "#5a5e6a", "#d0a060", "tablet",
      "A hammer the size of a thumb, struck by a Dwargon smith as his first piece of work and worn ever since. It lends a little weight to every swing.", "a:atk:+0.5"),
    N(FRIEREN, "Priest's Poultice Kit Charm", "CHARM", "POTION", "#7aa078", "#f0ecd0", "vial",
      "A tiny roll of linen, moss and salve on a cord, the kit every travelling priest keeps for the road. One press closes a small cut.", "",
      [("Quick Poultice", 90, 1, "heal:4", "You press the moss to the cut and the cut gives way.")]),
    N(MESHI, "Dungeon Cook's Ladle Charm", "CHARM", "CHARM", "#9a7a4a", "#e8d8b0", "cup",
      "A wooden ladle no longer than a finger, hung on the string of a cook who makes soup out of what the dungeon sends. A sip puts the weariness out.", "",
      [("Hot Broth Sip", 100, 1, "regen:6:0", "A warm sip, and the ache settles.")]),
    N(MIA, "Delver's Brass Lamp", "CHARM", "ORB", "#9a7a3a", "#f0d078", "lantern",
      "A scratched brass lamp of the divers who descend the Abyss, burning an oil that never smokes. Its glow is small and honest.", "",
      [("Lamp Glow", 60, 1, "buff:night_vision:25:0", "The little flame steadies and the gloom thins.")]),
    N(AOT, "Survey Corps Signal Flare Charm", "CHARM", "CHARM", "#4a6a4a", "#e8e0b8", "horn",
      "A stub of coloured smoke powder in a green sleeve, worn by scouts who ride beyond the walls. It lets friends know where the wearer stands.", "",
      [("Flare Mark", 80, 1, "mark:14:8", "Smoke climbs into the air and everyone near is seen.")]),
    N(DAN, "Orario Delver's Compass", "CHARM", "ORB", "#6a5a3a", "#d8c890", "compass",
      "A pocket compass sold on Babel Street to newcomers who still think the dungeon has only one way down. It at least tells them where up is.", "",
      [("Find Bearings", 80, 1, "mark:12:8", "The needle swings and shows who is near.")]),
    N(SLIME, "Dwargon Vault Key Charm", "CHARM", "CHARM", "#a08040", "#f0e098", "key",
      "A small bronze key from the dwarven vault district, cut to open nothing in particular. People swear it brings a little luck at the dice.", "a:luck:+1"),
    N(NARUTO, "Chunin Exam Scroll Case Charm", "CHARM", "TOME", "#8a3a2a", "#e8d8a0", "scroll",
      "A bamboo scroll case on a cord, empty or full depending on who you ask. A quick tug swaps the wearer with a log in a blink of the eye.", "",
      [("Quick Swap", 70, 1, "dash:4", "A puff, a log, and you are somewhere else.")]),
    N(HXH, "Hunter Exam Number Tag", "CHARM", "CHARM", "#c8c0a0", "#8a3a3a", "tablet",
      "A numbered tag pinned to the chest of every examinee on the first day. It is lucky to still be wearing it on the last.", "a:luck:+1"),
    N(OP, "Grand Line Berry Purse Charm", "CHARM", "CHARM", "#a07a2a", "#f0d878", "coin",
      "A small purse of worn leather that has never held more than a few berries and never been quite empty either. It brings a coin to those who need one.", "a:luck:+1"),
    N(NARUTO, "Shinobi Smoke Pellet Pouch", "CHARM", "POTION", "#4a4a5a", "#c8c8d8", "vial",
      "A drawstring pouch of smoke pellets, thrown at the feet of anyone who is too close. The cloud pushes them back a step.", "",
      [("Smoke Burst", 60, 1, "gale:4:1.0", "A pellet cracks and a grey cloud shoves the nearest foes away.")]),
    N(DB, "Half Senzu Bean Charm", "CHARM", "CHARM", "#6a9a4a", "#e8f0b8", "drop",
      "Half a hard green bean in a glass bead on a string. Half a bean is not much, and enough to get up from the ground.", "",
      [("Half Bean", 120, 1, "heal:5", "You chew the bitter half bean and the bruises lift.")]),
    N(DB, "Capsule Case Charm", "CHARM", "ORB", "#d8c040", "#5a8ad8", "gem",
      "A yellow capsule that holds a tent, a bike, or on a bad day a shield. This one pops open into a thin guard for a short moment.", "",
      [("Pop the Capsule", 90, 1, "shield:6:6", "A capsule pops and a thin shell surrounds you.")]),
    N(BLEACH, "Soul Candy Pill Case", "CHARM", "POTION", "#c85a7a", "#f8e0e8", "vial",
      "A tiny case of soul candy, the sweet that gets a soul back into its body when it has gone out. One piece mends a scratch.", "",
      [("Soul Candy", 90, 1, "heal:4", "You pop a candy and the sting recedes.")]),
    N(BLEACH, "Hell Butterfly Pin", "CHARM", "CHARM", "#2a2a3a", "#c8a8e8", "feather",
      "A small black butterfly pin of a Seireitei messenger. It flutters a little whenever a stranger is near.", "",
      [("Messenger Flutter", 80, 1, "mark:14:8", "The pin beats its wings, and you see who is near.")]),
    N(YGO, "Card Sleeve Locket", "CHARM", "TOME", "#2a3a6a", "#e8d878", "tablet",
      "A clear sleeve that holds a single card, kept by a duelist who never plays it. Fingers flick faster when it is close.", "a:aspd:+0.04"),
    N(FATE, "Spare Mana Gem", "CHARM", "ORB", "#c83a4a", "#f0d8d8", "gem",
      "A cheap ruby the size of a bead, drained and filled again by a magus on a budget. It nudges the reserve up a notch.", "a:mp:**0.03"),
    N(FT, "Lacrima Spark Lamp", "CHARM", "ORB", "#4a7ac8", "#e8f0ff", "lantern",
      "A small lacrima, the crystal that powers lamps and radios in the guild towns. A tap makes a crackle of lightning jump from it.", "",
      [("Lacrima Crackle", 60, 1, "bolt:6:5:bolt:-:0:0", "A spark of blue light snaps off the crystal.")]),
    N(FMA, "State Alchemist's Pocket Watch", "CHARM", "CHARM", "#a09070", "#e8e0c0", "hourglass",
      "A plain silver watch with a worn lid, carried by the military's alchemists. It reminds the owner that every minute is for moving.", "a:speed:*0.02"),
    N(SDS, "Boar Hat Tavern Token", "CHARM", "CHARM", "#b86a8a", "#f8e0d0", "coin",
      "A token of the travelling tavern with a pig on it, good for one meal and one wild story. It fortifies the owner's heart.", "a:hp:+2"),
    N(KONO, "Crimson Tassel of Axel", "CHARM", "CHARM", "#a82a2a", "#f0d0b0", "star",
      "A red tassel torn from the hat of a young archwizard in the first town. A tug sets off a tiny, loud pop.", "",
      [("Tiny Pop", 60, 1, "aoe:3:5:expl:-:0:0", "A small bang rattles whatever is close to you.")]),
    N(MUSHOKU, "Roxy's Water Pearl", "CHARM", "ORB", "#4aa0d8", "#e0f4ff", "drop",
      "A round pearl of still water given by a young mage to her student. It lets the carrier swim more easily.", "a:swim:*0.1"),
    N(SOLO, "Cracked Mana Crystal", "CHARM", "ORB", "#4a3a8a", "#a898e8", "gem",
      "A crystal with a flaw down the middle, looted from a low-rank gate and not worth much to the Association. A little mana still leaks from it.", "a:mp:**0.03"),
    N(SAO, "Hotbar Potion Pouch", "CHARM", "POTION", "#3a6a9a", "#e0eaf8", "vial",
      "A belt-sized pouch of crude health potions as sold on the first floor, with a ring slot for easy reach. Quaffing one is a single motion.", "",
      [("Quaff Potion", 100, 1, "heal:4", "You down a cheap potion and the bar creeps back.")]),
    N(JJK, "Jujutsu High Warding Strip", "CHARM", "TOME", "#e8e0c8", "#8a2a2a", "tablet",
      "A paper strip with a brush-drawn ward, handed out to first-year students on their first mission. It dulls the bite of cursed things.", "r:curse:0.1"),
    N(SHADOW, "Shadow Garden Cipher Coin", "CHARM", "CHARM", "#2a2a3e", "#9a8ad8", "coin",
      "A black coin with a notch, passed between members of a society nobody admits to. It helps its holder go unseen.", "a:sneak:*0.05"),
    N(DS, "Wisteria Incense Sachet", "CHARM", "POTION", "#8a6aa8", "#f0e0f8", "vial",
      "A sachet of wisteria flowers, burned by the Corps to keep demons out of their halls. A whiff clears a sour mood.", "",
      [("Wisteria Whiff", 90, 1, "cleanse", "A sweet violet smoke carries the sickness off.")]),

    # ------------------------------------------------------------------ NECK: guild, academy and kingdom badges
    N(SLIME, "Tempest Federation Citizen Medal", "NECK", "PENDANT", "#3a8ac8", "#e8f4ff", "star",
      "A bright medal given to every new citizen of the monster nation, stamped with a slime. It makes its wearer sturdier.", "a:hp:+2"),
    N(SLIME, "Dwargon Merchant Guild Seal", "NECK", "AMULET", "#8a5a2a", "#f0d078", "coin",
      "A heavy bronze seal worn by traders licensed in the dwarven capital. It opens doors and a few purses.", "a:luck:+1"),
    N(SLIME, "Blumund Adventurer Tag", "NECK", "PENDANT", "#7a7e88", "#c8d0d8", "tablet",
      "A bare iron tag of the guild of a small kingdom, stamped with the owner's rank. Nobody has bothered to polish it.", "a:armor:+1"),
    N(BC, "Clover Kingdom Citizen Seal", "NECK", "AMULET", "#2a6a3a", "#e8e0a0", "clover",
      "A green seal with the four-leaf clover of the kingdom, issued to every commoner who registers. Its small luck is the kingdom's gift.", "a:luck:+1"),
    N(MAGI, "Magnoshutatt Academy Medal", "NECK", "LOCKET", "#4a4ab0", "#e8e8ff", "book",
      "A blue medal of the academy of magicians, given to students who pass their first term. Its owner's mana pool runs a little deeper.", "a:mp:**0.03"),
    N(REZERO, "Lugunica Royal Guard Crest", "NECK", "PENDANT", "#a02a3a", "#f0d878", "star",
      "A small crimson crest of the royal knights of Lugunica, handed to the squires who bear their water. It steadies the shoulders.", "a:kb:+0.05"),
    N(DND, "Harper's Brooch Pin", "NECK", "LOCKET", "#6a6a8a", "#d8c878", "star",
      "A silver harp and crescent pin of an old network of spies and bards who guard the weak. It lends a little quiet to its wearer's step.", "a:sneak:*0.05"),
    N(SOLO, "Ahjin Guild Pin", "NECK", "PENDANT", "#2a5a8a", "#d8e8ff", "star",
      "A blue pin of the biggest guild of hunters in the country, which hires D-ranks to carry things. It is thought to keep the bearer in one piece.", "a:hp:+2"),
    N(BLEACH, "Seireitei Academy Badge", "NECK", "AMULET", "#e8e4d8", "#2a2a2a", "star",
      "A white and black badge of the academy that trains Soul Reapers, the first thing a new student is asked to guard. It sharpens the footing.", "a:fall:+2"),
    N(OP, "Marine Recruit Badge", "NECK", "PENDANT", "#2a4a7a", "#f0f0f0", "star",
      "A cheap enamel badge in navy and white of a recruit's first day, with the seagull stamped on it. It makes the wearer hard to knock down.", "a:kb:+0.05"),
    N(AOT, "Wings of Freedom Pin", "NECK", "LOCKET", "#2a5a4a", "#e8e0c8", "feather",
      "A small pin of two crossed wings, worn by every scout who went outside the walls and came back. It makes the heart a little bigger.", "a:hp:+2"),
    N(MHA, "Hero Course Badge", "NECK", "AMULET", "#c82a2a", "#f8f0e0", "star",
      "A red badge of a high school that trains heroes, issued on the first day of the first year. It fills the wearer with a little extra spirit.", "a:hp:+2"),
    N(MASHLE, "Easton Academy Crest", "NECK", "PENDANT", "#5a2a7a", "#e8d878", "crown",
      "A violet crest of a school where magic decides rank. Its wearer, amusingly, is strong without it, and it knocks nobody back.", "a:armor:+1"),
    N(OVL, "Copper Adventurer Plate of E-Rantel", "NECK", "PENDANT", "#a86a3a", "#e8b080", "tablet",
      "A copper plate of the lowest rank of the guild of the walled city, with a name scratched on the back. Its owner stands up from a fall faster.", "a:fall:+2"),

    # ------------------------------------------------------------------ BACK: cloaks, capes and mantles
    N(DND, "Ranger's Moss Scout Cloak", "BACK", "CLOAK", "#3a5a3a", "#a8c088", "cloak",
      "A cloak dyed with moss and bark, worn by scouts who need to be a shape in the trees. It hushes the wearer's step.", "a:sneak:*0.1"),
    N(SLIME, "Dire Wolf Pelt Mantle", "BACK", "MANTLE", "#7a7468", "#d8d0c0", "cloak",
      "A grey pelt of a dire wolf, tanned in the goblin village and worn like a mantle. Its warm hair keeps off the chill.", "r:cold:0.12"),
    N(SPICE, "Wayfaring Pedlar's Wagon Cape", "BACK", "CAPE", "#8a6a3a", "#e8d098", "cape",
      "A cape of waxed cotton worn by pedlars who spend most nights outdoors. It sheds the rain and nothing else.", "r:cold:0.1"),
    N(BC, "Squad Trainee's Mantle", "BACK", "MANTLE", "#2a3a5a", "#d8d0a0", "cape",
      "A short mantle in the squad's colour given to the trainees who sweep the barracks. It is a bit stiff and well stitched.", "a:armor:+1"),
    N(HXH, "Hunter Exam Traveller's Poncho", "BACK", "CLOAK", "#8a8a5a", "#e8e8c8", "cloak",
      "A rough poncho worn by examinees who sleep on the road to the next test. Its owner learns to move quietly in it.", "a:sneak:*0.08"),
    N(DS, "Corps Foot Soldier's Haori", "BACK", "CAPE", "#2a5a4a", "#f0e8d0", "cape",
      "A checkered haori of the corps' foot soldiers, thin and warm. It leaves the arms free for swinging.", "a:aspd:+0.04"),
    N(OP, "Deckhand's Oilskin Cape", "BACK", "CAPE", "#c8a838", "#4a3a2a", "cape",
      "A yellow oilskin that keeps a deckhand dry in a storm and visible in the fog. The wearer finds it easier to swim in.", "a:swim:*0.1"),
    N(FT, "Job Board Wanderer's Duster", "BACK", "MANTLE", "#a85a3a", "#f0d8a0", "cloak",
      "A dusty duster of a mage who takes jobs from the board at every guild hall. It has been mended more times than washed.", "a:speed:*0.02 a:hp:+1"),
    N(REZERO, "Priestella Gondolier's Cape", "BACK", "CAPE", "#4aa0c8", "#f0f4f0", "cape",
      "A pale blue cape of the water-city gondoliers, sewn with a waterproof thread. It keeps the weather off the shoulders.", "r:cold:0.1"),
    N(GS, "Silver Rank Plain Cloak", "BACK", "CLOAK", "#6a6a72", "#b8b8c0", "cloak",
      "A grey cloak of an adventurer who prefers not to be remembered, stained by many fights. It softens a blow a little.", "r:proj:0.1"),

    # ------------------------------------------------------------------ BELT: tool belts, pouches, bandoliers
    N(SLIME, "Dwargon Smith's Tool Belt", "BELT", "BELT", "#6a4a2a", "#b8b8c0", "belt",
      "A wide leather belt hung with tongs, punches and a little hammer. Its owner's blows land with a smith's patience.", "a:atk:+0.5"),
    N(SLIME, "Orc Hide Utility Belt", "BELT", "BELT", "#5a6a3a", "#c8c088", "belt",
      "A belt cut from the green hide of an orc that no longer needs it, studded with iron rings. It holds up a weight of rubble.", "a:kb:+0.08"),
    N(NARUTO, "Kunai Holster Belt", "BELT", "BELT", "#3a3a4a", "#b8c0d0", "belt",
      "A black belt with a holster on each hip and a roll of ninja wire. It makes the wearer swift on the draw.", "a:aspd:+0.05"),
    N(MH, "Gathering Hunter's Belt", "BELT", "BELT", "#7a5a2a", "#e8d098", "belt",
      "A belt of pouches for herbs, honey and bones, worn by guild hunters between quests. The wearer carries more than they look.", "a:hp:+2"),
    N(MESHI, "Delver's Ration Belt", "BELT", "BELT", "#8a6a3a", "#e8d8a0", "belt",
      "A belt with a row of spice tins and a knife loop, tied by a party's cook. It sharpens the nose for anything out of place.", "a:luck:+1"),
    N(MIA, "Cave Raider's Rope Belt", "BELT", "BELT", "#9a8a5a", "#e8e0b8", "belt",
      "A coil of strong rope laced around the waist, worn by those who climb down the great hole. It softens a hard landing.", "a:fall:+3"),
    N(OP, "Marine Cutlass Baldric", "BELT", "BELT", "#2a4a7a", "#e8e8f0", "belt",
      "A navy sash with a loop for a cutlass, the first piece of kit given to a recruit. It braces the stance.", "a:kb:+0.05 a:aspd:+0.03"),
    N(DND, "Spell Component Belt", "BELT", "BELT", "#5a3a7a", "#d8c880", "belt",
      "A belt of small pouches for bat guano, sulphur and the other everyday dirt of casting. Its owner's reserve is a little deeper.", "a:mp:**0.03"),
    N(SOLO, "Hunter Guild Utility Belt", "BELT", "BELT", "#2a2a3a", "#6a98d8", "belt",
      "A grey utility belt of the hunters who dive for the guilds, with room for rope, flare and a potion. Its owner moves on light feet.", "a:speed:*0.02 a:fall:+1"),
    N(TOG, "Regular's Shinsu Pouch Belt", "BELT", "BELT", "#3a6a7a", "#c8f0f0", "belt",
      "A belt of tiny pouches for the sand of the tower, worn by the regulars who climb. It adds spring to the knees.", "a:jump:+0.04"),
    N(DB, "Turtle School Weighted Belt", "BELT", "BELT", "#d86a2a", "#2a2a2a", "belt",
      "An orange belt hung with iron plates, worn by students who are told to carry more than they should. It makes the wearer hard to push.", "a:kb:+0.08"),
    N(JJK, "Curse Tool Sash", "BELT", "BELT", "#2a2a2a", "#a02a2a", "belt",
      "A black sash with a sheath of odd, rusted charms, worn by exorcists-in-training. It softens the touch of dark things.", "r:dark:0.12"),

    # ------------------------------------------------------------------ HANDS: gloves and wraps
    N(SLIME, "Dwargon Forge Gloves", "HANDS", "GLOVES", "#6a4a3a", "#c8a070", "glove",
      "Thick leather gloves with a rim of singed hair, worn by apprentices at the great forge. They keep the heat off the knuckles.", "r:fire:0.15"),
    N(DND, "Archer's Finger Guards", "HANDS", "GAUNTLETS", "#7a5a3a", "#d8c8a0", "glove",
      "Three leather tabs for the drawing fingers, tied at the wrist. They steady a shot and a swing.", "a:reach:+0.3"),
    N(FRIEREN, "Cleric's Bandage Wraps", "HANDS", "GLOVES", "#e8e4d0", "#7aa078", "glove",
      "Clean linen wraps knotted at the wrist by a cleric who has run out of time to mend. They make a hand more steady.", "a:hp:+2"),
    N(NARUTO, "Genin Arm Guards", "HANDS", "GAUNTLETS", "#2a3a5a", "#a8b8d8", "glove",
      "Steel-backed arm guards in navy cloth, given to new genin on their first mission. They block a glancing strike.", "a:armor:+1"),
    N(OP, "Deckhand's Rope Gloves", "HANDS", "GLOVES", "#a8884a", "#e8d8a0", "glove",
      "Fingerless gloves worn thin by a thousand hauls on the line. They keep the grip when everything else is wet.", "a:swim:*0.1"),
    N(MAGI, "Thief's Lockpick Gloves", "HANDS", "GLOVES", "#3a3a4a", "#c8b878", "glove",
      "Thin black gloves with a pick in each seam, favoured by the boys of Balbadd. They let the wearer move without a rustle.", "a:sneak:*0.08"),
    N(MESHI, "Cook's Oven Mitts", "HANDS", "GLOVES", "#c8a868", "#8a3a3a", "glove",
      "Quilted mitts big enough for a dwarf's pot, scorched along the thumbs. They make a hot thing painless to hold.", "r:fire:0.15"),
    N(HXH, "Beast Tamer's Falconry Glove", "HANDS", "GAUNTLETS", "#7a4a2a", "#d8b878", "glove",
      "A thick glove with a leather perch for a bird, worn by the hunters who have a pet or two. It lends a long reach.", "a:reach:+0.3"),
    N(MIA, "Cradle Digger's Gloves", "HANDS", "GLOVES", "#5a5a6a", "#d8d0a8", "glove",
      "Rough gloves with iron grit sewn into the palm, worn by miners at the lip of the abyss. They make a landing less painful.", "a:fall:+2"),
    N(REZERO, "Sword Saint Trainee's Grip Wrap", "HANDS", "GAUNTLETS", "#a82a2a", "#e8d8c0", "glove",
      "A red wrap bound tight around the sword hand of a trainee in an old house of swordsmen. It steadies the draw.", "a:aspd:+0.04"),
    N(MH, "Guild Skinner's Gloves", "HANDS", "GLOVES", "#6a5a3a", "#c8c8b8", "glove",
      "Thick gloves for a skinner who cuts hide off the monsters the hunters bring in. They shrug off a nick from a claw.", "a:armor:+1"),
    N(DB, "Tournament Fist Wraps", "HANDS", "CHARM", "#d89a2a", "#f8f0d0", "glove",
      "Yellow wraps taped by an arena official before a bout. They make the knuckles bite a little harder.", "a:atk:+0.5"),

    # ------------------------------------------------------------------ HEAD
    N(NARUTO, "Leaf Village Forehead Protector", "HEAD", "PLATE", "#2a3a6a", "#c8d0e0", "helm",
      "A cloth band with a steel plate bearing the symbol of a hidden village. Tying it on is a small promise to come back.", "a:armor:+1 a:hp:+1"),
    N(AOT, "Survey Corps Cadet Cap", "HEAD", "HAT", "#4a6a4a", "#d8d0a0", "hood",
      "A green cadet's cap of the scout regiment, brim turned down against the wind. It makes the wearer a bit harder to shove.", "a:kb:+0.05"),
    N(OP, "Marine Recruit's Cap", "HEAD", "HAT", "#ececf4", "#2a4a7a", "hood",
      "A white cap with the navy's anchor on it, issued before the first drill. It keeps the sun out and the pride in.", "a:hp:+2"),
    N(HXH, "Exam Hall Bandana", "HEAD", "HOOD", "#3a6a8a", "#e8d878", "hood",
      "A blue bandana knotted at the back, worn by examinees in the long marches. It is damp with sweat and surprisingly lucky.", "a:luck:+1"),
    N(MAGI, "Magnoshutatt Student Cap", "HEAD", "HAT", "#3a3ab0", "#e8e8ff", "hood",
      "A tasselled cap of the academy of magicians, worn crooked by students on the way to class. It slightly widens the mind.", "a:mp:**0.03"),
    N(SH, "Falmuth Footman's Kettle Helm", "HEAD", "PLATE", "#7a7e88", "#4a5a8a", "helm",
      "A round kettle helm of the foot soldiers of a small kingdom, dented by hasty fitting. It takes the blow on the crown.", "a:armor:+2"),
    N(DS, "Corps Fox Mask Replica", "HEAD", "HOOD", "#e8e0d0", "#c82a2a", "mask",
      "A painted fox mask like the ones the old teacher gives to students he believes in. It carries a small ward.", "r:curse:0.1 a:luck:+1"),
    N(MHA, "Hero Course Goggles", "HEAD", "CIRCLET", "#d86a2a", "#2a2a3a", "eye",
      "Orange training goggles with a rubber band, a first-year's hero kit. They keep dust out of the eyes.", "a:jump:+0.04"),
    N(KONO, "Axel Apprentice Wizard's Hat", "HEAD", "HAT", "#4a2a6a", "#e8b838", "hood",
      "A crooked witch's hat bought from a stall in the first town, too large for most heads. It holds a bit of mana in the point.", "a:mp:**0.03"),
    N(MESHI, "Dungeon Cook's Bandana", "HEAD", "HOOD", "#c85a3a", "#f8f0d0", "hood",
      "A red kerchief tied back by a cook who prefers dragon to beef. It keeps the sweat out of the soup.", "r:fire:0.12"),
    N(BLEACH, "Hollow Mask Fragment", "HEAD", "CIRCLET", "#e8e4dc", "#2a2a2a", "mask",
      "A chipped sliver of bone-white mask that fits on a brow. It is dead, and still a little afraid of the light.", "r:holy:0.1"),
    N(SOLO, "Gate Raider's Rank Helmet", "HEAD", "PLATE", "#3a3a4a", "#68a8d8", "helm",
      "A dented grey helmet of a hunter who raids low-rank gates, with a visor that never shuts. It saves the skull.", "a:armor:+1 a:kb:+0.03"),

    # ------------------------------------------------------------------ CHEST
    N(BC, "Magic Knight Recruit's Coat", "CHEST", "COAT", "#2a3a6a", "#d8d0a0", "plate",
      "A navy coat with the clover on the pocket, the plain uniform of a recruit before they are given a squad robe. It is stitched to last.", "a:armor:+1 a:hp:+1"),
    N(AOT, "Survey Corps Cadet Jacket", "CHEST", "LEATHER", "#6a5a3a", "#4a6a4a", "plate",
      "A brown jacket with green trim and harness straps, worn by cadets who have yet to see a titan. It holds a body together.", "a:armor:+2"),
    N(OP, "Marine Recruit's Uniform", "CHEST", "SUIT", "#dcdcea", "#2a4a7a", "plate",
      "A white and blue uniform with a stiff collar, as handed out in the navy's boot camp. It stands up to a scuffle.", "a:armor:+1 a:kb:+0.03"),
    N(HXH, "Exam Examinee's Waistcoat", "CHEST", "COAT", "#5a7a4a", "#e8e0b8", "plate",
      "A green waistcoat with many pockets, worn by examinees who will carry everything on their back. It stays out of the way.", "a:armor:+1 a:speed:*0.02"),
    N(NARUTO, "Genin Flak Vest", "CHEST", "LEATHER", "#4a5a3a", "#c8c8b0", "plate",
      "A green flak vest with a steel lining, issued after the genin exam. It is almost light enough to forget.", "a:armor:+2"),
    N(SOLO, "Gate Raider's Padded Coat", "CHEST", "COAT", "#2a2a3a", "#68a8d8", "plate",
      "A padded grey coat with reflective stitching, standard issue for anyone entering a low-rank gate. It turns the first claw.", "a:armor:+1 a:hp:+2"),
    N(SLIME, "Orc Warband Scale Mail", "CHEST", "PLATE", "#5a6a3a", "#a89868", "plate",
      "A mail of iron scales sewn to green hide, worn by the warriors of the swarm. It is ill-fitted and sturdy.", "a:armor:+2"),
    N(SLIME, "Lizardman Scale Cuirass", "CHEST", "LEATHER", "#3a7a5a", "#e8dca0", "plate",
      "A cuirass of overlapping green scales of the marsh clan, light and cool. It gives with the body.", "a:armor:+1 a:speed:*0.02"),
    N(REZERO, "Priestella Guard's Tabard", "CHEST", "COAT", "#3a7aa8", "#f0f0e8", "plate",
      "A pale tabard of the guards who watch the water gates, wet at the hem. It is warmer than it looks.", "a:armor:+1 r:cold:0.1"),
    N(KONO, "Axel Adventurer's Leather Coat", "CHEST", "LEATHER", "#8a5a3a", "#e8d098", "plate",
      "A brown coat of a green adventurer in a starter town who chooses the cheapest armour in the shop. It has survived a lot.", "a:armor:+1 a:luck:+1"),
    N(MAGI, "Kou Soldier's Lamellar", "CHEST", "PLATE", "#8a2a2a", "#d8b868", "plate",
      "A red lamellar of the empire's footmen, laced with cord. It spreads a blow over the whole chest.", "a:armor:+2"),
    N(BLEACH, "Soul Reaper Shihakusho", "CHEST", "ROBE", "#1a1a2a", "#e8e4d8", "robe",
      "The plain black kimono of a Soul Reaper, worn under any haori. It lets a body move and a spirit hide.", "a:speed:*0.02 r:dark:0.1"),

    # ------------------------------------------------------------------ FEET
    N(NARUTO, "Shinobi Sandals", "FEET", "SANDALS", "#2a3a5a", "#c8d0e0", "boots",
      "Open-toed navy sandals of the hidden villages, light as a breath. They help the wearer run along walls.", "a:speed:*0.03"),
    N(OP, "Marine Recruit's Boots", "FEET", "BOOTS", "#2a2a3a", "#c8c8d8", "boots",
      "Black boots polished for the inspection and scuffed by the first day. They stand up to a fall off a mast.", "a:fall:+2"),
    N(AOT, "Survey Corps Cadet Boots", "FEET", "BOOTS", "#6a4a2a", "#d8c8a0", "boots",
      "Brown leather boots with a heel clip for the harness gear. They land softly after a swing.", "a:fall:+3"),
    N(HXH, "Exam Marathon Shoes", "FEET", "BOOTS", "#c8c8d0", "#d83a3a", "boots",
      "Worn running shoes of an examinee who has run a hundred miles behind a tester. They are quick and unhurried.", "a:speed:*0.03"),
    N(BC, "Magic Knight Recruit's Boots", "FEET", "BOOTS", "#26366a", "#d8d0a0", "boots",
      "Navy boots with a brass buckle, given to recruits for the first long march. They are firm on rough ground.", "a:speed:*0.02 a:fall:+1"),
    N(SLIME, "Orc Hide Greaves", "FEET", "GREAVES", "#5a6a3a", "#b8a878", "boots",
      "Greaves cut from the green hide of an orc, hardened with oil and laced with sinew. They plant the feet firmly.", "a:kb:+0.08"),
    N(SLIME, "Lizardman Marsh Greaves", "FEET", "GREAVES", "#3a7a5a", "#d8e8c0", "boots",
      "Lightweight scale greaves of the marsh clans, good for wading. They make swimming a little easier.", "a:swim:*0.1"),
    N(SLIME, "Dwargon Miner's Steel-Toe Boots", "FEET", "BOOTS", "#6a5a4a", "#a8a8b8", "boots",
      "Heavy boots with a steel cap, worn in the deep mines where a falling rock is common. They save a toe and a knee.", "a:armor:+1 a:fall:+1"),
    N(MIA, "Delver's Rope-Soled Boots", "FEET", "BOOTS", "#8a7a4a", "#e8e0b8", "boots",
      "Boots with a sole of woven rope, worn by those who go down on a line. They grip any ledge.", "a:fall:+3"),
    N(DS, "Corps Straw Zori", "FEET", "SANDALS", "#c8a868", "#4a3a2a", "boots",
      "Straw sandals with a white cord, the footwear of a slayer who walks everywhere. They soften every step.", "a:sneak:*0.08"),
    N(MAGI, "Kou Soldier's Marching Sandals", "FEET", "SANDALS", "#9a6a3a", "#e8d098", "boots",
      "Sturdy marching sandals of the imperial footmen, laced to the knee. They keep a long road short.", "a:speed:*0.03"),
    N(BLEACH, "Soul Reaper Waraji", "FEET", "SANDALS", "#e8e0c8", "#2a2a2a", "boots",
      "White straw sandals with black ties, worn under the shihakusho. Their tread is hushed.", "a:sneak:*0.08"),
    N(MHA, "Training Ground Sneakers", "FEET", "BOOTS", "#d8d8e0", "#c82a2a", "boots",
      "Red-trimmed white sneakers from a hero school's obstacle course. They add spring to the jump.", "a:jump:+0.04"),
]
