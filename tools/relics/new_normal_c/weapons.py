"""Wave 105 weapons: 4 Normal (damage 20-30) melee tools of a trade and monster-drop arms. One table; gen.py writes the Java, the lang and the class rows from it, models.py builds the 3D mesh,
the skins, the sprite and the item models of each. Passives and acts use the vocabulary of relic_rows_lib (Wave31Acts shapes).

An entry: id, name, grade, cls (the weapon class of weapon_classes.json, which is also the GradeDamage weapon type), speed (attacks a second), look (Look enum), pal (main, trim, glow),
world (the franchise it is a homage to), lore, passives (relic_rows_lib tokens), acts (relic_rows_lib act tuples: name, cooldown s, mana %, spec, flavour[, uses per day, needs sneak]),
engr (Tensura engravings), doc (what the model looks like)."""

WEAPONS = [
    dict(id="orc_tusk_cleaver", name="Orc Tusk Cleaver", grade="NORMAL", cls="shortsword", speed=1.6, look="SWORD", pal=(0xA8A090, 0x6A4A30, 0xE8E0C8),
         world="That Time I Got Reincarnated as a Slime", lore="A broad chopping blade ground out of an orc's tusk and bound to a wooden grip with sinew, left behind after the swarm broke on the forest. It is rough, and it bites.",
         passives="a:kb:+0.05", acts=[("Tusk Chop", 25, 1, "aoe:3:6:hit:-:0:0", "You bring the heavy tusk down in a short, ugly arc.")],
         engr=["crushing"], doc="A curved, creamy ivory tusk blade with a ridged edge, a wrapped sinew grip, a small iron cap and a loop of cord at the pommel."),
    dict(id="direwolf_fang_spear", name="Direwolf Fang Spear", grade="NORMAL", cls="spear", speed=1.3, look="STAFF", pal=(0x7A6A58, 0xD8D0C0, 0xC8E0FF),
         world="That Time I Got Reincarnated as a Slime", lore="A hunting spear tipped with the great fang of a direwolf and wrapped with its grey fur, carried by scouts who prefer to stay out of reach of the pack.",
         passives="a:reach:+0.4", acts=[("Fang Thrust", 30, 1, "bolt:5:7:hit:-:0:0", "You stab from far out, with the fang ahead of you.")],
         engr=["swift"], doc="A long ash pole with a curved ivory fang head, a wrap of grey fur under the head, a leather-bound grip and an iron butt cap."),
    dict(id="delvers_pickaxe", name="Delver's Pickaxe", grade="NORMAL", cls="handaxe", speed=1.4, look="AXE", pal=(0x8A8C98, 0x7A5A3A, 0xE0E8F0),
         world="Made in Abyss", lore="A short-hafted pick used by cave raiders to chip relics out of the abyss walls, which can serve as a weapon when the walls fight back. Its point is chipped and good.",
         passives="a:atk:+0.5", acts=[("Pick Strike", 30, 1, "bolt:4:6:hit:-:0:0", "You drive the point in with a short swing.")],
         engr=["severance"], doc="A short oak haft with a long steel pick on one side and a small flat hammer face on the other, bound in rope and capped with iron."),
    dict(id="dungeon_cooks_ladle", name="Dungeon Cook's Ladle", grade="NORMAL", cls="hammer", speed=1.1, look="HAMMER", pal=(0x9A9AA6, 0x8A5A32, 0xFFD8A0),
         world="Delicious in Dungeon", lore="A big iron ladle that stirs a pot of stew the size of a cauldron and can be swung as a club when something in the pot starts to walk away. It smells of good dinner.",
         passives="a:hp:+2", acts=[("Ladle Bonk", 35, 1, "aoe:3:6:hit:slow:3:0", "A heavy, round-faced bonk that rings like a gong.")],
         engr=["sturdy"], doc="A long wooden handle with an iron bowl at the end, a hanging hook on the haft, a leather strap and a dented rim."),
]
