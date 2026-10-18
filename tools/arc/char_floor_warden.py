"""Why: the Floor Warden (entity nusmp:floor_warden, labyrinth/seal/FloorWardenEntity) is the Labyrinth's mini-boss, the thing that drops the Shadow Key, and the Labyrinth Guardian
(nusmp:labyrinth_guardian, labyrinth/mega/guard) is the same body grown colossal, so ONE model serves both (the renderer scales the Guardian by its own size attribute). The lore
the entity class states: "the Dungeon's own armour grown huge on the Spire's magicules". It was the vanilla zombie model in a violet re-tint of the Living Armor's skin; this is the art
kit's version, a huge player-proportioned knight of empty armour: a closed helm with a single slit of light, heavy plate in dark violet with gold seal-trim, a tabard that carries the
Labyrinth's seal, a ragged cape and the great hammer its Ground Slam is struck with. There is nobody inside (the face under the helm is not shown; the slit glows). Colours: the
Living Armor's mollusk-violet, lightened so it reads in a dim hall. See docs/arc/remodel.md. Not canon: a game design.
"""
ENTITIES = ["floor_warden", "labyrinth_guardian"]

SPEC = {
    "id": "floor_warden",
    "name": "Floor Warden",
    "role": "boss",
    "build": "huge",
    "height": 3.2,
    "widen": 0.92,
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
    "cape": {"kind": "tattered", "color": "#3c3060", "trim": "#d8b058", "length": 0.9, "width": 1.1, "ragged": True, "lining": "#2a2236"},
    "armor": {"pieces": ["helm", "pauldrons", "breastplate", "bracers", "greaves", "sabatons", "tassets", "gorget"], "style": "plate", "color": "#7a68b0", "trim": "#d8b058",
              "glow": "#c8a0ff"},
    "props": {"right": {"kind": "hammer", "color": "#8a7ab8", "color2": "#3a3050", "glow": "#c8a0ff", "length": 1.0}},
    "glow": {"color": "#c8a0ff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "smash", "cast": "magic", "idle": "proud"},
}
