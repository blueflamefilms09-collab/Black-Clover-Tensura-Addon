"""Why: the Floor Warden (entity anime_multiverse:floor_warden, labyrinth/seal/FloorWardenEntity) is the Labyrinth's mini-boss, the thing that drops the Shadow Key, and the Labyrinth Guardian
(anime_multiverse:labyrinth_guardian, labyrinth/mega/guard) is the same body grown colossal, so ONE model serves both (the renderer scales the Guardian by its own size attribute). The lore
the entity class states: "the Dungeon's own armour grown huge on the Spire's magicules". It was the vanilla zombie model in a violet re-tint of the Living Armor's skin; the art kit's first
version was a huge player-proportioned knight of empty armour (kept as tools/arc/legacy/char_floor_warden_v1.py.txt).

REWORKED (docs/dungeon_bosses_rework.md, owner's request "make the dungeon bosses better looking"): the same empty armour, but now it reads as a boss from across a hall. A pair of ram horns
and a broken crown of seal thorns on the helm, spiked pauldrons and a taller, wider frame (3.4 blocks), a longer tattered mantle, a longer hammer with a glowing head, the Seal itself as a
floating violet orb at its left hand, and (the lore) the keyhole of the Shadow Key it guards: a lit keyhole plate on the chest with the key hanging from it on a chain, plus three chains with
seal-tags that swing from the belt. Nobody is inside; the slit of the helm glows. Colours stay the Living Armor's mollusk-violet so the family reads; the trim is gold and the glow lilac.
Not canon: a game design.
"""
ENTITIES = ["floor_warden", "labyrinth_guardian"]

SPEC = {
    "id": "floor_warden",
    "name": "Floor Warden",
    "role": "boss",
    "build": "huge",
    "height": 3.4,
    "widen": 1.0,
    "skin": ["#2a2236", "#3a3050", "#4c4068", "#605484"],
    "face": {"expression": "dead", "eyes": "glow", "eye_color": "#d8b8ff", "eye_glow": "#c8a0ff", "brows": "none", "mouth": "none", "nose": "none"},
    "hair": {"style": "bald", "color": "#2a2236"},
    "outfit": {
        "top": {"kind": "tabard", "color": "#4a3c70", "trim": "#d8b058", "length": "knee", "collar": "none", "hem_trim": True, "ragged": True,
                "emblem": {"kind": "diamond", "where": "chest", "size": 9, "glow": "#c8a0ff"}},
        "inner": {"kind": "none"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#3a3050"},
        "boots": {"kind": "boots", "color": "#3a3050", "height": "knee"},
        "gloves": {"kind": "gauntlets", "color": "#6a5a98"},
        "belt": {"kind": "double", "color": "#2a2236", "buckle": "#d8b058"},
    },
    "cape": {"kind": "mantle", "color": "#3c3060", "trim": "#d8b058", "length": 1.0, "width": 1.25, "ragged": True, "lining": "#2a2236", "emblem": {"kind": "rune", "size": 10, "glow": "#c8a0ff"}},
    "horns": {"kind": "ram", "color": "#2a2236", "glow": "#c8a0ff", "size": 1.25},
    "halo": {"kind": "thorns", "color": "#4c4068", "glow": "#d8b8ff", "radius": 1.15, "behind": True},
    "armor": {"pieces": ["helm", "pauldrons", "breastplate", "bracers", "greaves", "sabatons", "tassets", "gorget", "gauntlets", "spikes"], "style": "plate", "color": "#7a68b0", "trim": "#d8b058",
              "glow": "#c8a0ff"},
    "props": {"right": {"kind": "hammer", "color": "#8a7ab8", "color2": "#3a3050", "glow": "#c8a0ff", "length": 1.3},
              "float": {"kind": "orb", "color": "#a888e8", "glow": "#d8b8ff", "length": 1.2}},
    "glow": {"color": "#c8a0ff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "smash", "cast": "magic", "idle": "proud"},
}


def extra(kit, rig):
    """The Shadow Key's keyhole: a lit plate on the chest with the key hanging from it on a chain, and three seal-tag chains that swing from the belt (they hang like cloth in every clip)."""
    d = rig.d
    plate = kit.metal(kit.shades("#3a3050", 5, 1.2), 41, glow=(200, 160, 255, 160), emboss=False)
    lit = kit.metal(kit.shades("#d8b8ff", 5, 1.25), 42, glow=(216, 184, 255, 255), emboss=False)
    gold = kit.metal(kit.shades("#d8b058", 5, 1.2), 43, glow=(216, 176, 88, 120), emboss=False)
    rig.box("body", (-2.0, d.sh_y - 7.0, -d.td / 2 - 0.7), (4.0, 5.0, 0.8), plate)               # the plate of the keyhole
    rig.box("body", (-0.6, d.sh_y - 5.4, -d.td / 2 - 1.1), (1.2, 1.2, 0.5), lit)                  # the round of the hole, lit
    rig.box("body", (-0.3, d.sh_y - 6.6, -d.td / 2 - 1.1), (0.6, 1.4, 0.5), lit)                  # the slot below it
    rig.bone("key", "body", (0.0, d.sh_y - 7.0, -d.td / 2 - 1.0))                                  # the key on its chain
    rig.box("key", (-0.3, d.sh_y - 11.5, -d.td / 2 - 1.2), (0.6, 4.2, 0.6), gold)
    rig.box("key", (-1.2, d.sh_y - 12.6, -d.td / 2 - 1.2), (2.4, 1.6, 0.6), gold)
    rig.animate("key", sway=0.8)
    for k, x in enumerate((-4.5, 0.0, 4.5)):
        name = "tag%d" % k
        rig.bone(name, "body", (x, d.hip_y + 1.0, -d.td / 2 - 0.4))
        rig.box(name, (x - 0.3, d.hip_y - 6.0, -d.td / 2 - 0.6), (0.6, 6.5, 0.6), plate)
        rig.box(name, (x - 1.1, d.hip_y - 9.0, -d.td / 2 - 0.7), (2.2, 3.0, 0.5), lit if k == 1 else plate)
        rig.animate(name, sway=1.2, phase=0.7 * k)
