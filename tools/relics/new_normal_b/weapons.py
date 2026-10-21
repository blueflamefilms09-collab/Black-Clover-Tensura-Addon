"""Wave 104 weapons: 6 Normal (damage 20-30) melee weapons of everyday adventurers. One table; gen.py writes the Java, the lang and the class rows from it, models.py builds the 3D mesh, the
skins, the sprite and the item models of each. Passives and acts use the vocabulary of relic_rows_lib (Wave31Acts shapes).

An entry: id, name, grade, cls (the weapon class of weapon_classes.json, which is also the GradeDamage weapon type), speed (attacks a second), look (Look enum), pal (main, trim, glow),
world (the franchise it is a homage to), lore, passives (relic_rows_lib tokens), acts (relic_rows_lib act tuples: name, cooldown s, mana %, spec, flavour[, uses per day, needs sneak]),
engr (Tensura engravings), doc (what the model looks like)."""

WEAPONS = [
    dict(id="selection_hopefuls_bokken", name="Selection Hopeful's Bokken", grade="NORMAL", cls="katana", speed=1.7, look="SWORD", pal=(0xB89060, 0x3A2A1A, 0xFFD8A0),
         world="Demon Slayer", lore="A plain oak training sword carried up the mountain by the hopefuls of the final selection, worn smooth by a month of cuts before breakfast. It teaches a clean line before a sharp one.",
         passives="a:aspd:+0.03", acts=[("Oak Cut", 25, 1, "bolt:3:5:hit:-:0:0", "You cut the way you were drilled, and the line is clean.")],
         engr=["swift"], doc="A slightly curved oak blade with a darker grain, a round black tsuba, a rope-wrapped grip and a worn pommel cap."),
    dict(id="genin_academy_kunai", name="Genin Academy Kunai", grade="NORMAL", cls="dagger", speed=2.2, look="SWORD", pal=(0x4A4E5A, 0x9AA0B0, 0xB8C8FF),
         world="Naruto", lore="The plain black kunai handed to every graduate of the leaf academy with a lecture about not throwing it at friends. It is balanced for throwing and fine for peeling fruit.",
         passives="a:sneak:*0.05", acts=[("Kunai Throw", 20, 1, "bolt:8:5:hit:-:0:0", "You flick the kunai and it bites.")],
         engr=["swift"], doc="A leaf-shaped dark steel blade, a short wrapped grip and a large ring pommel with a loop of cloth cord."),
    dict(id="east_blue_cutlass", name="East Blue Cutlass", grade="NORMAL", cls="sabre", speed=1.7, look="SWORD", pal=(0xA0A8B4, 0x6A4A2A, 0xE0E8F0),
         world="One Piece", lore="A curved sailor's cutlass with a basket guard, bought cheap from a harbour smith by anyone who wanted to look like a pirate and then learned to be one. It cuts rope and rivals alike.",
         passives="a:swim:*0.05", acts=[("Deck Slash", 25, 1, "aoe:3:5:hit:-:0:0", "A wide cut across the deck, and the nearest ones step back.")],
         engr=["sturdy"], doc="A broad curved single-edged blade, a sweeping brass basket guard, a dark wood grip and a flat brass pommel."),
    dict(id="axel_frontier_club", name="Axel Frontier Club", grade="NORMAL", cls="club", speed=1.3, look="HAMMER", pal=(0x7A5230, 0x8A8E98, 0xFFD090),
         world="KonoSuba", lore="A knotty oak club with a few iron nails driven through the end, favoured by the beginner adventurers of a town at the edge of the demon king's lands. It is stronger than its owner usually is.",
         passives="a:kb:+0.05", acts=[("Frontier Bash", 30, 1, "aoe:3:5:hit:slow:3:0", "A heavy swing that leaves the nearest ones dazed.")],
         engr=["crushing"], doc="A thick tapering oak club with iron nails driven in around the head, leather straps on the grip and an iron butt cap."),
    dict(id="apprentice_birch_wand", name="Apprentice Birch Wand", grade="NORMAL", cls="wand", speed=1.5, look="WAND", pal=(0xD8D0B8, 0x6AA070, 0xB8FFD0),
         world="Frieren: Beyond Journey's End", lore="A slim birch wand cut and polished by a village mage for her first apprentice. It throws a little spark and teaches that magic mostly means patience.",
         passives="a:mp:**0.03", acts=[("Petal Spark", 20, 1, "bolt:10:5:magic:-:0:0", "A pale green spark leaps from the tip.")],
         engr=["magic_weapon"], doc="A pale slim birch rod with a small green bud-shaped crystal at the tip, a wrapped grip and a ring of leaves carved near the base."),
    dict(id="hunter_exam_machete", name="Hunter Exam Machete", grade="NORMAL", cls="cleaver", speed=1.6, look="SWORD", pal=(0x8A9096, 0x3A4A3A, 0xC8E0C0),
         world="Hunter x Hunter", lore="A broad bush machete issued at a swamp checkpoint to those who still believed the exam was a walk in the woods. It hacks vines and the occasional liar.",
         passives="a:atk:+0.5", acts=[("Brush Cut", 25, 1, "aoe:3:5:hit:-:0:0", "You hack a wide arc through the undergrowth and whatever hides in it.")],
         engr=["sturdy"], doc="A wide single-edged blade with a clipped tip, a green cord-wrapped handle with a lanyard hole and a short black guard."),
]
