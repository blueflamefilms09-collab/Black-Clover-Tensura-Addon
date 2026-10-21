"""Wave 103 (1.111.0): 45 Normal and 22 Special grade gear and trinkets of everyday adventurers (the 8 weapons of the same wave are in weapons.py).

The row format is relic_rows_lib.R. The extra key `world` (the franchise or tradition each relic is a homage to) is only for the doc. Normal grade: one small real passive or one simple
act, cheap costs. Special grade: two small passives and one or two acts. Every ability is one of the shapes of Wave31Acts; nothing here touches food, hunger or exhaustion."""
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
ORIG = "Original"


def R(world, *a, **k):
    r = _R(*a, **k)
    r["world"] = world
    return r


NORMAL = [
    R(SLIME, "Great Jura Travelling Cloak", "BACK", "CLOAK", "NORMAL", ("#5a6a4a", "#b8a878", "#d8e0b0"), "cloak",
      "A grey-green travelling cloak of the kind sold in the markets at the edge of the Great Jura Forest. It sheds rain and keeps the road dust off the neck.",
      "a:speed:*0.03 r:cold:0.1", []),
    R(SLIME, "Little Slime Charm", "CHARM", "CHARM", "NORMAL", ("#4a9ad8", "#c8e8ff", "#8ae0ff"), "drop",
      "A palm-sized blue slime in a knotted cord that wobbles when it is happy. It mends a scratch if it is pressed gently.",
      "", [("Soft Squeeze", 90, 1, "heal:4", "The little slime pulses and a scratch closes.")]),
    R(SLIME, "Goblin Village Hide Vest", "CHEST", "LEATHER", "NORMAL", ("#7a5a38", "#c8a070", "#e8c890"), "plate",
      "A vest of stitched boar hide from the goblin village, patched many times and cut generously. It stops a bite and does not stop a conversation.",
      "a:armor:+1 a:hp:+2", []),
    R(SLIME, "Kobold Miner's Lamp", "CHARM", "ORB", "NORMAL", ("#8a6a2a", "#e8c050", "#ffe08a"), "lantern",
      "A tin lamp with a thick glass bead that the kobolds of the Dwargon mines hang from the belt. Its light is small and very steady.",
      "", [("Steady Light", 60, 1, "buff:night_vision:25:0", "The bead glows and the dark gets thinner.")]),
    R(SLIME, "Dwargon Smith's Leather Cap", "HEAD", "HOOD", "NORMAL", ("#6a4a32", "#a89070", "#d8c098"), "hood",
      "A scorched leather cap worn by the apprentices of the Dwargon forges. It has saved more eyebrows than helmets have saved heads.",
      "r:fire:0.15 a:armor:+1", []),
    R(BC, "Novice Magic Knight's Cap", "HEAD", "HAT", "NORMAL", ("#2a3a6a", "#e8e0c0", "#9ab0ff"), "hood",
      "A navy cap with a small clover stitched on it, handed to every new recruit of a Magic Knights squad. It is a little big, to be grown into.",
      "a:mp:**0.03 a:luck:+1", []),
    R(BC, "Four-Leaf Squire's Pin", "CHARM", "PENDANT", "NORMAL", ("#2f7a3a", "#d8e8a0", "#a8ff9a"), "clover",
      "A brass pin in the shape of a four-leaf clover, pinned to the coats of squires who have not yet got a squad. The luck is very small, and it is real.",
      "a:luck:+1", []),
    R(BC, "Hage Village Work Boots", "FEET", "BOOTS", "NORMAL", ("#6a5038", "#a89878", "#e0d0a8"), "boots",
      "Thick work boots from a poor village at the end of the kingdom, resoled so often that the soles are a different leather from the uppers. They are good for running with a heavy load.",
      "a:speed:*0.03 a:fall:+2", []),
    R(BC, "Bandaged Training Wraps", "HANDS", "GAUNTLETS", "NORMAL", ("#d8d0b8", "#8a6a50", "#fff0d0"), "glove",
      "Strips of linen wound from knuckle to elbow by someone who has been told to do three hundred more swings. They hold the wrist steady.",
      "a:atk:+0.5 a:kb:+0.05", []),
    R(MAGI, "Caravan Water Flask", "CHARM", "POTION", "NORMAL", ("#7a5a3a", "#c8d8e8", "#9ae0ff"), "vial",
      "A leather-wrapped flask from a desert caravan, always a little cool. A mouthful drives the dust out of the throat and the tiredness out of the legs.",
      "", [("Cool Mouthful", 75, 1, "regen:6:0+buff:speed:6:0", "A cold swallow, and the road is lighter.")]),
    R(MAGI, "Street Thief's Sash", "BELT", "BELT", "NORMAL", ("#a83a3a", "#e8d098", "#ffb090"), "belt",
      "A bright red sash from the markets of Balbadd, worn by boys who are faster than the guards. It shows where the wearer was last seen, and not where they are.",
      "a:speed:*0.03 a:sneak:*0.1", []),
    R(MAGI, "Sindria Trader's Scarf", "NECK", "AMULET", "NORMAL", ("#38a0a8", "#f0e8c8", "#9af0f0"), "amulet",
      "A light scarf of the sea-trade, woven with a small blue knot for safe voyages. It keeps the sun off the neck and the water off the mind.",
      "a:swim:*0.1 r:fire:0.1", []),
    R(DAN, "Supporter's Rucksack", "BACK", "MANTLE", "NORMAL", ("#7a6a48", "#c8b078", "#f0dca0"), "scroll",
      "A deep canvas rucksack for the porter who walks behind the adventurers in Orario and carries everything they will not. It is built for a long day on the stairs.",
      "a:kb:+0.05 a:hp:+2", []),
    R(DAN, "Familia Emblem Pin", "CHARM", "PENDANT", "NORMAL", ("#c8c0b0", "#8a2a2a", "#ffd8c0"), "pendant",
      "A small enamel pin of a Familia's emblem, given with a first blessing. It reminds the wearer who is waiting at home.",
      "a:hp:+2", [("Hearth Thought", 120, 1, "regen:5:0", "You think of home, and the ache eases.")]),
    R(DAN, "Babel Street Sandals", "FEET", "SANDALS", "NORMAL", ("#9a7a50", "#e0d098", "#f8e8b0"), "boots",
      "Light sandals for the stairs of the upper floors, lashed to the ankle with thin cord. They are quiet on stone.",
      "a:sneak:*0.1 a:fall:+2", []),
    R(FT, "Guild Mark Armband", "HANDS", "CHARM", "NORMAL", ("#c83a52", "#e8e0d0", "#ff9ab0"), "ring",
      "A red cloth band with the guild's stamp pressed in it, worn above the bracer by members who like to be seen. Friends find the wearer quickly in a crowd.",
      "", [("Rally Cry", 90, 1, "allies:6:speed:6:0", "You call out the guild's name and your friends hurry.")]),
    R(FT, "Happy's Fish Charm", "CHARM", "CHARM", "NORMAL", ("#4a8ad8", "#f0e0b0", "#9ad0ff"), "drop",
      "A charm of a dried blue fish, left behind by a flying cat who valued it above gold. It gives its owner a spring in the step.",
      "a:jump:+0.04 a:luck:+1", []),
    R(SH, "Melromarc Guard's Coif", "HEAD", "PLATE", "NORMAL", ("#8a8c98", "#4a5a8a", "#c8ccd8"), "helm",
      "A chain coif of the city guard of Melromarc, with a nasal bar. Its mail is thin and its stitching is honest.",
      "a:armor:+1 a:kb:+0.05", []),
    R(SH, "Filo Feather Charm", "CHARM", "CHARM", "NORMAL", ("#e8d898", "#a8c0f0", "#fff0b0"), "feather",
      "A pale feather kept in a leather loop, shed by a very large bird who likes to run. Its owner finds a gap in the stride when it is needed.",
      "", [("Quick Stride", 60, 1, "dash:5", "You take a long, bird-like step.")]),
    R(SH, "Medicine Peddler's Satchel", "BELT", "BELT", "NORMAL", ("#6a8a52", "#e8d8a8", "#c8f0a0"), "vial",
      "A satchel of dried herbs and little bottles, hung on the belt of a peddler who knows every weed in the kingdom. The best of them is always on top.",
      "", [("Herb Poultice", 100, 1, "heal:5+cleanse", "A bruised leaf is pressed on, and the sting is gone.")]),
    R(DND, "Ring of Minor Protection", "HANDS", "CHARM", "NORMAL", ("#a8a8b0", "#4a6a9a", "#c8d8ff"), "ring",
      "A plain iron ring set with a chip of blue glass, enchanted by a hedge wizard on a slow Tuesday. It turns the odd blow away.",
      "a:armor:+1 r:magic:0.1", []),
    R(DND, "Adventurer's Hooded Cloak", "BACK", "CLOAK", "NORMAL", ("#4a4a52", "#a89068", "#d0c8b0"), "cloak",
      "A grey wool cloak with a deep hood, the first thing in most adventurers' packs and the last thing they would give up. It keeps off the weather and the eyes.",
      "a:sneak:*0.1 r:cold:0.1", []),
    R(DND, "Studded Leather Jerkin", "CHEST", "LEATHER", "NORMAL", ("#5a4030", "#a8a8b0", "#e0d8c8"), "plate",
      "A jerkin of boiled leather with iron studs, the standard answer to the question of what a rogue should wear. It rattles a little less than mail.",
      "a:armor:+2", []),
    R(DND, "Everburning Torch Charm", "CHARM", "ORB", "NORMAL", ("#a85a1a", "#f0b040", "#ffd070"), "flame",
      "A little tin torch that never goes out and never gets hot, worn on a cord by those who dislike the dark. Struck against a wall it throws a spark.",
      "r:fire:0.1", [("Torch Flick", 45, 1, "aoe:3:4:fire:-:0:0+ignite:3:2", "You flick the torch and sparks jump.")]),
    R(SOLO, "E-Rank Hunter's Work Gloves", "HANDS", "GLOVES", "NORMAL", ("#4a4a58", "#8a98b8", "#b8c8ff"), "glove",
      "Padded gloves from the Hunters Association's supply counter, issued to rank E raiders who will not be asked to do anything difficult. They hold a blade well.",
      "a:atk:+0.5 a:aspd:+0.03", []),
    R(SOLO, "Gate Scavenger's Satchel", "BELT", "BELT", "NORMAL", ("#3a3a48", "#c8a058", "#ffe090"), "scroll",
      "A dark satchel for the odds and ends a scavenger can lift from a gate before it closes. A last bit of loot always seems to fit.",
      "a:luck:+1 a:hp:+2", []),
    R(SOLO, "Association Rubber Boots", "FEET", "BOOTS", "NORMAL", ("#2a2a38", "#6a7a98", "#a0b0e0"), "boots",
      "Black boots with a hard toe, standard issue for the clean-up teams who walk in after the raid. They do not slip on anything.",
      "a:fall:+3 a:kb:+0.05", []),
    R(REZERO, "Mansion Servant's Apron", "CHEST", "LEATHER", "NORMAL", ("#e0d8e8", "#4a3a6a", "#f0e8ff"), "plate",
      "A white and navy apron from a certain mansion in the countryside, starched against any stain. It is a surprisingly good thing to be wearing when a sword comes at you.",
      "a:armor:+1 a:hp:+2", []),
    R(REZERO, "Beatrice's Doll Charm", "CHARM", "CHARM", "NORMAL", ("#e8c870", "#8a3a58", "#ffe8b0"), "eye",
      "A small ringleted doll on a ribbon with a very serious face. It is not a spirit, and it still sits up when something dark comes near.",
      "a:luck:+1", [("Doll's Warning", 90, 1, "mark:10:6", "The doll turns its head toward something near.")]),
    R(KONO, "Axel Adventurer's Scarf", "NECK", "AMULET", "NORMAL", ("#c85a3a", "#f0d8a8", "#ffb890"), "amulet",
      "A long red scarf from a town where the adventurers are broke and proud of it. It has been used as a bandage, a towel and a flag.",
      "a:luck:+1 r:fire:0.1", []),
    R(OVL, "Nazarick Page's Sash", "BELT", "BELT", "NORMAL", ("#2a1a3a", "#c8a030", "#e8c860"), "belt",
      "A violet and gold sash of the lowest servants of a very large tomb. They are told that being noticed is worse than being useful.",
      "a:hp:+2 a:kb:+0.05", []),
    R(SAO, "Starter Town Leather Tunic", "CHEST", "LEATHER", "NORMAL", ("#7a5a3a", "#c8c0a8", "#e8dcc0"), "plate",
      "The tunic every player starts with, plain brown and a bit too clean. It is a reminder to be careful on the first floor.",
      "a:armor:+1 a:speed:*0.02", []),
    R(FATE, "Chaldea Standard Mystic Code", "CHEST", "COAT", "NORMAL", ("#2a3a5a", "#e0e0e8", "#a8c8ff"), "plate",
      "A black and white jacket from a research facility at the end of the world, issued to every Master in training. It carries a small charm against curses.",
      "a:armor:+1 r:curse:0.1", []),
    R(NARUTO, "Leaf Genin Forehead Protector", "HEAD", "CIRCLET", "NORMAL", ("#2a3a6a", "#b8c0c8", "#d8e0f0"), "circlet",
      "A blue cloth band with a small steel plate engraved with a village's leaf, worn by every fresh graduate. It is a good thing to bump into a wall with.",
      "a:armor:+1 a:aspd:+0.03", []),
    R(FMA, "Alchemist's Chalk Pouch", "BELT", "BELT", "NORMAL", ("#a89880", "#4a3a4a", "#f0e8d8"), "vial",
      "A pouch of chalk and a bit of charcoal, for the scribbling of circles on a floor. A circle takes a moment, and the ground helps for a moment.",
      "", [("Quick Circle", 100, 1, "shield:4:6", "You scratch a circle at your feet and it holds.")]),
    R(OP, "East Blue Sailor's Bandana", "HEAD", "HOOD", "NORMAL", ("#c83a3a", "#f0f0e8", "#ff9a8a"), "hood",
      "A red and white bandana of the sort tied by sailors, thieves and one particular boy. It keeps the sweat out of the eyes in a fight.",
      "a:swim:*0.1 a:aspd:+0.03", []),
    R(HXH, "Hunter Exam Number Plate", "NECK", "PENDANT", "NORMAL", ("#c8c8d0", "#4a4a5a", "#ffffff"), "tablet",
      "A numbered badge pinned to the chest of an applicant during the exam. Its owner is much more calm when the number is low.",
      "a:luck:+1 a:hp:+2", []),
    R(DB, "Weighted Training Wristbands", "HANDS", "CHARM", "NORMAL", ("#c8782a", "#4a3a2a", "#ffc880"), "ring",
      "Heavy cuffs sewn with lead, worn through a hard month of exercise and removed with a sigh. The arms come back stronger than they went in.",
      "a:atk:+0.5 a:kb:+0.05", []),
    R(BLEACH, "Academy Student's Black Robe", "CHEST", "ROBE", "NORMAL", ("#1a1a24", "#e8e8f0", "#a8a8c0"), "robe",
      "A black robe with a white collar, cut for students of a spirit academy who are told it will fit in time. It does not hinder a swing.",
      "a:hp:+2 r:dark:0.1", []),
    R(FRIEREN, "Elf Village Herb Pouch", "CHARM", "BELT", "NORMAL", ("#4a7a4a", "#e8e0b8", "#b8f0a0"), "vial",
      "A mossy pouch of dried leaves that every elf village keeps by the door. A pinch in the tea clears a head.",
      "", [("Calming Tea", 100, 1, "cleanse+regen:5:0", "A pinch of leaves, and the fog lifts.")]),
    R(MESHI, "Dungeon Cook's Apron", "CHEST", "LEATHER", "NORMAL", ("#c8b890", "#7a4a3a", "#f0e0b8"), "plate",
      "A stained canvas apron with deep pockets and a number of burns. It is the most comfortable armour in a dungeon.",
      "a:armor:+1 r:fire:0.15", []),
    R(YGO, "Duelist's Deck Box Belt", "BELT", "BELT", "NORMAL", ("#3a2a5a", "#d8b840", "#f0e098"), "tablet",
      "A belt with a hard case for a deck of forty cards. A top card is always drawn a bit sooner.",
      "a:aspd:+0.05 a:luck:+1", []),
    R(ORIG, "Wanderer's Waystone", "CHARM", "ORB", "NORMAL", ("#6a7a8a", "#c8d0d8", "#b8e0ff"), "gem",
      "A smooth river stone on a cord, picked up on a long walk and kept ever since. Rubbing it shows the way ahead more clearly.",
      "a:speed:*0.02", [("Rub the Stone", 80, 1, "mark:12:8", "The stone warms, and the road ahead is plain.")]),
    R(ORIG, "Pilgrim's Walking Boots", "FEET", "BOOTS", "NORMAL", ("#5a4a3a", "#a89070", "#d8c4a0"), "boots",
      "Resoled boots for a long pilgrimage, with the dust of three kingdoms ground into them. They go on from the very first step.",
      "a:speed:*0.04", []),
    R(ORIG, "Rope Climber's Gloves", "HANDS", "GLOVES", "NORMAL", ("#8a7050", "#d8c898", "#f0e0b8"), "glove",
      "Thick gloves with a rubbed palm, favoured by climbers and sailors. The wearer can reach a little further when it matters.",
      "a:reach:+0.5 a:fall:+2", []),
]

SPECIAL = [
    R(BC, "Squad Vice-Captain's Circlet", "HEAD", "CIRCLET", "SPECIAL", ("#7a1a2a", "#e8c860", "#ff9aa8"), "circlet",
      "A thin silver circlet with a red stone, lent by a squad to the one who keeps the rest in line. The mana runs smoother through it.",
      "a:mp:**0.06 a:luck:+1 a:armor:+1", [("Command Voice", 90, 2, "allies:8:strength:8:0", "You raise your voice, and the squad stands straighter.")]),
    R(SLIME, "Brumund Knight-Captain's Helm", "HEAD", "PLATE", "SPECIAL", ("#7a7e8a", "#b83a3a", "#d8dce8"), "helm",
      "A steel helm with a red plume from the neighbouring kingdom's knights, tough enough to take the first blow of a lance.",
      "a:armor:+2 a:kb:+0.1 r:proj:0.15", [("Hold the Line", 80, 2, "shield:8:8", "You plant your feet and the line holds.")]),
    R(FT, "S-Class Trial Hood", "HEAD", "HOOD", "SPECIAL", ("#3a2a5a", "#e0c040", "#c8a0ff"), "hood",
      "A hood worn by the guild's candidates to the S-Class trial on a far island. It is lined with tough thread and an old nerve.",
      "a:hp:+4 s:danger_sense", [("Steady Nerve", 90, 2, "cleanse+buff:resistance:6:0", "You take a breath and the fear leaves.")]),
    R(DND, "Elven Chain Shirt", "CHEST", "LEATHER", "SPECIAL", ("#b8c8d0", "#4a7a58", "#e8f8ff"), "plate",
      "A shirt of fine linked rings made by the elves of a grey forest, light as cloth and almost as quiet. It is worn under a plain tunic by those who would rather not be seen to need it.",
      "a:armor:+3 a:sneak:*0.1", [("Ring Ward", 80, 2, "shield:10:8", "The rings shiver and take the blow.")]),
    R(BC, "Golden Dawn Recruit's Coat", "CHEST", "COAT", "SPECIAL", ("#e8c040", "#6a4a1a", "#fff0a0"), "plate",
      "A gold and white coat of a famous squad, for a recruit who has done something worth noticing. It is cut for freedom of movement and pride.",
      "a:armor:+2 a:hp:+4 a:aspd:+0.05", [("Golden Rush", 70, 2, "buff:speed:8:1+shield:6:6", "A gold flash runs down the coat, and you are away.")]),
    R(SH, "Tortoise-Shell Cuirass", "CHEST", "PLATE", "SPECIAL", ("#5a7a4a", "#c8b070", "#b8e8a0"), "plate",
      "A cuirass of a spirit tortoise's shell, sanded and fitted by a smith in the eastern fields. It is heavy and it does not crack.",
      "a:armor:+3 a:kb:+0.1 r:expl:0.15", [("Shell Up", 90, 3, "shield:12:8+buff:resistance:6:0", "You draw in, and the shell is all that can be seen.")]),
    R(SLIME, "Beastkin Hunter's Boots", "FEET", "BOOTS", "SPECIAL", ("#6a4a2a", "#c8a860", "#f0d890"), "boots",
      "Soft-soled boots of the beastkin hunters, made for the long chase through the forest. They know the ground by the feel of it.",
      "a:speed:*0.06 a:sneak:*0.1 a:fall:+3", [("Pounce", 40, 2, "dash:8", "You spring ahead like a hunting cat.")]),
    R(REZERO, "Kingdom Courier's Boots", "FEET", "BOOTS", "SPECIAL", ("#2a4a6a", "#d8b840", "#a8d0ff"), "boots",
      "Blue boots of the royal couriers who carry the sealed word from city to city. A good courier never stops for water.",
      "a:speed:*0.06 a:jump:+0.04", [("Courier's Sprint", 70, 2, "buff:speed:10:1+dash:6", "You tighten the laces and the miles get short.")]),
    R(DND, "Boots of the Winding Path", "FEET", "SANDALS", "SPECIAL", ("#4a7a5a", "#e0d498", "#b8f0c8"), "boots",
      "Soft boots of a ranger of the green road, that turn the way ahead into a short corner. A bramble is not an obstacle to them.",
      "a:speed:*0.05 a:fall:+4 e:slow_falling:0", [("Short Cut", 60, 2, "blink:7", "The path folds and you are on the other side of it.")]),
    R(FT, "Silver Guild Pendant", "NECK", "PENDANT", "SPECIAL", ("#c0c8d8", "#4a5a8a", "#e8f0ff"), "pendant",
      "A silver pendant of a mage guild's second rank, struck with the guild's mark and the owner's name. It keeps the owner's magic in order.",
      "a:mp:**0.06 r:magic:0.2", [("Mark of Rank", 90, 2, "buff:resistance:8:0+cleanse", "The pendant shines, and the guild's name stands behind you.")]),
    R(DAN, "Familia Falna Locket", "NECK", "LOCKET", "SPECIAL", ("#d8c070", "#7a2a2a", "#fff0b0"), "pendant",
      "A gold locket holding a lock of a goddess's hair and a crumb of the blessing she gave. It beats a little faster in a fight.",
      "a:hp:+4 a:aspd:+0.05 a:luck:+1", [("Goddess's Pulse", 100, 2, "second:0.2:6", "The locket beats, and the wound closes a little.")]),
    R(MAGI, "Desert Dancer's Veil Cape", "BACK", "CAPE", "SPECIAL", ("#d8a850", "#8a3a5a", "#ffe0a0"), "cape",
      "A thin veil-cape from the dancers of the desert palaces, cut so that it will not catch on a blade. It billows when the wearer turns, and the blade is not where it was.",
      "a:speed:*0.05 a:sneak:*0.1 r:fire:0.2", [("Veil Step", 50, 2, "blink:6", "The veil swirls, and you are elsewhere.")]),
    R(SOLO, "D-Rank Raider's Mantle", "BACK", "MANTLE", "SPECIAL", ("#2a3050", "#8aa0d8", "#c0d4ff"), "cape",
      "A heavy mantle for raiders who have cleared enough low gates to be trusted with a middle one. It has the faint smell of dungeon dust.",
      "a:armor:+2 a:hp:+4 s:danger_sense", [("Ready Stance", 80, 2, "shield:8:8+buff:resistance:6:0", "You draw the mantle round and brace.")]),
    R(KONO, "Adventurer's Mana-Flask Belt", "BELT", "BELT", "SPECIAL", ("#4a2a5a", "#e0b040", "#d0a0ff"), "vial",
      "A belt with five loops of small glass flasks for the explosion-minded. A sip of the right one puts a bit of power back.",
      "a:mp:**0.08 a:hp:+2", [("Mana Sip", 100, 2, "buff:haste:10:0+regen:6:0", "You swallow a thimbleful, and the world speeds up.")]),
    R(MUSHOKU, "Guild Veteran's Sash", "BELT", "BELT", "SPECIAL", ("#8a2a2a", "#d8c8a0", "#ffa090"), "belt",
      "A faded sash worn by mercenaries of the northern guild who have survived ten contracts. Its colour is not what it was, and neither is its owner's.",
      "a:hp:+4 a:armor:+1 a:kb:+0.1", [("Old Campaigner", 80, 2, "second:0.2:5", "A seasoned breath, and the wound is a little better.")]),
    R(SOLO, "Gate-Raider Grip Gloves", "HANDS", "GLOVES", "SPECIAL", ("#2a3a4a", "#68a8d8", "#a8e0ff"), "glove",
      "Gloves with a slip-proof palm, worn by raiders who climb out of gates by the skin of their teeth. They hold their grip when the rest of the body will not.",
      "a:atk:+1 a:aspd:+0.06 a:reach:+0.5", [("Hard Grip", 60, 2, "buff:strength:6:0+shield:6:6", "You tighten your hands and the blow lands harder.")]),
    R(BC, "Mana Skin Wrap", "HANDS", "GAUNTLETS", "SPECIAL", ("#4a2a6a", "#9a7ad8", "#d0b8ff"), "glove",
      "A violet wrap drawn through a loop of mana and shaped into a thin skin on the forearm. It is a first step toward the mana skin of a real knight.",
      "a:armor:+2 a:atk:+1 a:mp:**0.04", [("Mana Skin", 70, 2, "shield:10:8", "Mana spreads over the skin like a second layer.")]),
    R(FT, "Dragon Slayer Apprentice Bracers", "HANDS", "CHARM", "SPECIAL", ("#c85a2a", "#e8d098", "#ffb070"), "ring",
      "Scarred bracers of those who study the old lost magic from a guild elder who never finishes a lesson. They feel warm and a bit dangerous.",
      "a:atk:+1 r:fire:0.25", [("Warm Fist", 50, 2, "aoe:4:8:fire:-:0:0+ignite:4:3", "You clench the bracers, and a gout of heat leaps.")]),
    R(SLIME, "Spirit Whisper Bell", "CHARM", "ORB", "SPECIAL", ("#a0d8e8", "#e8f4ff", "#c8f0ff"), "horn",
      "A little glass bell that rings only when a spirit is near. It is a sweet note, and the owner is more at ease for hearing it.",
      "a:luck:+2 s:magic_sense", [("Ring the Bell", 80, 2, "mark:16:10+cleanse", "The bell sounds, and the spirits show you who is near.")]),
    R(DND, "Compass of True North", "CHARM", "ORB", "SPECIAL", ("#8a7a4a", "#e0d498", "#ffe8a0"), "compass",
      "A brass compass that points always north, and never tells a lie about it. In a maze it is worth ten lanterns.",
      "a:speed:*0.04 a:luck:+1", [("True Heading", 70, 2, "mark:20:12+buff:speed:8:0", "The needle swings and settles, and you know where to go.")]),
    R(DAN, "Hi-Potion Vial Bandolier", "CHARM", "POTION", "SPECIAL", ("#2a8a6a", "#d8e8c8", "#a0ffd8"), "vial",
      "A bandolier of green vials sold at the Guild stalls for a lot more than they should. The top one never seems to run dry.",
      "a:hp:+2", [("Hi-Potion", 100, 2, "heal:10+cleanse", "A vial, a swallow, and the pain is gone."), ("Quick Potion", 120, 2, "regen:10:1", "You down a second one and the mending carries on.", 0, True)]),
    R(FRIEREN, "Quiet Mage's Flower Locket", "CHARM", "AMULET", "SPECIAL", ("#d8d0e8", "#6a5a9a", "#f0e8ff"), "amulet",
      "A locket of a quiet mage's habit, holding a faded flower that is never replaced. It dulls the edge of a strange spell.",
      "r:magic:0.25 a:mp:**0.05 a:luck:+1", [("Quiet Focus", 90, 2, "buff:resistance:8:0+cleanse", "You hold the flower, and the noise settles.")]),
]

ROWS = NORMAL + SPECIAL
