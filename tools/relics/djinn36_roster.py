"""djinn36_roster: the 24 Djinn added to the roster of twelve (Bael ... Murmur): names, colours, palettes, the fallback look of the old shared rig,
weapon sizes and names, particle palettes. The one table every djinn36_* tool reads; the Java side keeps the same facts in
client/relics/djinn/DjinnRoster36.java (a test-free mirror: tools/relics/djinn36_check.py compares the two).

The Djinn rows (name, title, affinity, body colour, glow) are the owner's list as handed to the logic builder; the logic builder owns the enum
(relics/djinn/Djinn.java), so nothing here parses it. Ids are the lower-case enum names, like the first twelve.
"""

# name, display, title, affinity, body colour, glow colour
ROWS = [
    ("BAEL", "Bael", "King of the First Throne", "STRENGTH", 0xC9A227, 0xFFF0A0),
    ("AGARES", "Agares", "Rider of Crocodile Tides", "WATER", 0x1FA37A, 0xB8FFE0),
    ("VALEFOR", "Valefor", "Thief of Storm-Hearts", "LIGHTNING", 0x5A3DB5, 0xE0D2FF),
    ("BARBATOS", "Barbatos", "Hunter of the Wild Green", "LIFE", 0x2E7D32, 0xCFFFB0),
    ("GUSION", "Gusion", "Seer of the Tide Mirror", "WATER", 0x3D7EAA, 0xD6F2FF),
    ("VEPAR", "Vepar", "Mermaid Sovereign of Wrecks", "WATER", 0x00A6B8, 0xBFFCFF),
    ("SHAX", "Shax", "Silencer of the Dove", "SOUND", 0x8E8EA8, 0xF0F0FF),
    ("BUER", "Buer", "Healer of the Wheel", "LIFE", 0x9CCC3B, 0xF4FFC0),
    ("ANDREALPHUS", "Andrealphus", "Peacock of the Stars", "LIGHT", 0x2F6FDB, 0xA8D4FF),
    ("KIMARIS", "Kimaris", "Rider of the Dark Steed", "STRENGTH", 0x3A2A3F, 0xFF6A8A),
    ("AMDUSIAS", "Amdusias", "Maestro of the Unicorn Horn", "SOUND", 0xD58AF0, 0xFFE3FF),
    ("BELIAL", "Belial", "Lord of Hollow Thrones", "HEAT", 0x7A0F2E, 0xFF7096),
    ("DECARABIA", "Decarabia", "Star Crab of the Deep", "LIGHT", 0xE0B45A, 0xFFF4C8),
    ("SEERE", "Seere", "Swift Wind Prince", "WIND", 0xE8E8C0, 0xFFFFFF),
    ("DANTALION", "Dantalion", "Many-Faced Scholar", "LIGHT", 0xC0A0FF, 0xF2E8FF),
    ("ANDROMALIUS", "Andromalius", "Seeker of Hidden Things", "LIGHTNING", 0x2B2B6B, 0x9CA8FF),
    ("GLASYA_LABOLAS", "Glasya-Labolas", "Hound of Slaughter", "STRENGTH", 0xA00000, 0xFF9090),
    ("HAAGENTI", "Haagenti", "Alchemist of the Winged Bull", "STRENGTH", 0xB87333, 0xFFD9A0),
    ("CROCELL", "Crocell", "Frost Sage", "WATER", 0x9ADCF0, 0xFFFFFF),
    ("FURCAS", "Furcas", "Old Knight of the Ice Wind", "WIND", 0xAAB7C8, 0xFFFFFF),
    ("BALAM", "Balam", "Three-Headed Prophet", "HEAT", 0xD1622A, 0xFFE0A0),
    ("ALLOCES", "Alloces", "Lion-Eyed Knight", "HEAT", 0xE8892B, 0xFFF0B0),
    ("CAMIO", "Camio", "Songbird of the Crows", "SOUND", 0x2A3340, 0xA0C8FF),
    ("MURMUR", "Murmur", "Death Drummer", "SOUND", 0x6B3FA0, 0xE8C8FF),
]

NAMES = [r[0] for r in ROWS]

# the Look of each new Djinn in relics/djinn/Djinn.java (copied from the logic builder's table, docs/djinn_roster_36.json): horns, wings, tail, halo, ears.
# The unique and dark sets do not read it; the shared-rig fallback set (djinn/<id>/) and the Lesser Summon bust are drawn from it, and the tail rule
# (a tail piece exactly when the Look has a tail) is TAIL_NONE below.
FALLBACK_LOOK = {
    "BAEL": ("CROWN", "BLADE", "NONE", "SPOKES", False),
    "AGARES": ("RAM", "VEIL", "BLADE", "NONE", False),
    "VALEFOR": ("SPIKE", "BLADE", "WHIP", "RING", True),
    "BARBATOS": ("STAG", "VEIL", "WHIP", "NONE", True),
    "GUSION": ("CRESCENT", "VEIL", "NONE", "RUNES", False),
    "VEPAR": ("NONE", "VEIL", "TENDRIL", "RING", True),
    "SHAX": ("NONE", "FEATHER", "WHIP", "RING", True),
    "BUER": ("STAG", "FEATHER", "NONE", "RING", True),
    "ANDREALPHUS": ("CROWN", "FEATHER", "WHIP", "SPOKES", True),
    "KIMARIS": ("SPIKE", "MEMBRANE", "WHIP", "NONE", False),
    "AMDUSIAS": ("SPIKE", "FEATHER", "TENDRIL", "RUNES", False),
    "BELIAL": ("CROWN", "FIRE", "FLAME", "NONE", False),
    "DECARABIA": ("NONE", "MEMBRANE", "BLADE", "SPOKES", True),
    "SEERE": ("NONE", "FEATHER", "WHIP", "SPOKES", True),
    "DANTALION": ("CROWN", "VEIL", "NONE", "RUNES", True),
    "ANDROMALIUS": ("STAG", "BLADE", "TENDRIL", "RUNES", False),
    "GLASYA_LABOLAS": ("RAM", "MEMBRANE", "WHIP", "NONE", False),
    "HAAGENTI": ("CRESCENT", "FEATHER", "WHIP", "NONE", True),
    "CROCELL": ("CROWN", "VEIL", "NONE", "RING", True),
    "FURCAS": ("STAG", "BLADE", "NONE", "RING", False),
    "BALAM": ("RAM", "FIRE", "WHIP", "SPOKES", True),
    "ALLOCES": ("CROWN", "FIRE", "WHIP", "RING", True),
    "CAMIO": ("NONE", "FEATHER", "BLADE", "RUNES", True),
    "MURMUR": ("CRESCENT", "MEMBRANE", "TENDRIL", "SPOKES", False),
}

# the plate / trim / cloth / extra the light set is MADE of (see djinn_unique.PALETTE for the row format)
PALETTE = {
    "BAEL":           dict(plate=("gold", 0.55, (150, 108, 30)), trim=("bronze", 0.45), cloth=((128, 22, 38), 0.12), extra=("obsidian", 0.20)),
    "AGARES":         dict(plate=("bronze", 0.50, (22, 86, 70)), trim=("gold", 0.30), cloth=((20, 88, 80), 0.40), extra=("stone", 0.55)),
    "VALEFOR":        dict(plate=("iron", 0.60, (30, 22, 62)), trim=("silver", 0.50), cloth=((40, 24, 84), 0.50), extra=("obsidian", 0.30)),
    "BARBATOS":       dict(plate=("wood", 0.60, (64, 46, 26)), trim=("bronze", 0.40), cloth=((30, 92, 46), 0.50), extra=("bone", 0.30)),
    "GUSION":         dict(plate=("silver", 0.50, (160, 200, 224)), trim=("silver", 0.20), cloth=((60, 120, 160), 0.50), extra=("silver", 0.70)),
    "VEPAR":          dict(plate=("bronze", 0.45, (0, 110, 120)), trim=("gold", 0.30), cloth=((10, 112, 132), 0.50), extra=("bone", 0.35)),
    "SHAX":           dict(plate=("silver", 0.55, (220, 222, 240)), trim=("silver", 0.30), cloth=((232, 234, 246), 0.15), extra=("bone", 0.10)),
    "BUER":           dict(plate=("bronze", 0.40, (110, 140, 50)), trim=("gold", 0.50), cloth=((100, 150, 50), 0.50), extra=("wood", 0.40)),
    "ANDREALPHUS":    dict(plate=("obsidian", 0.55, (14, 26, 70)), trim=("gold", 0.40), cloth=((20, 60, 150), 0.50), extra=("silver", 0.50)),
    "KIMARIS":        dict(plate=("steel", 0.55, (126, 104, 150)), trim=("silver", 0.30), cloth=((110, 44, 82), 0.40), extra=("bone", 0.10)),
    "AMDUSIAS":       dict(plate=("silver", 0.40, (240, 222, 250)), trim=("gold", 0.40), cloth=((200, 120, 220), 0.50), extra=("bone", 0.10)),
    "BELIAL":         dict(plate=("obsidian", 0.60, (24, 6, 12)), trim=("gold", 0.50), cloth=((72, 10, 28), 0.50), extra=("iron", 0.40)),
    "DECARABIA":      dict(plate=("bronze", 0.40, (200, 120, 70)), trim=("gold", 0.50), cloth=((240, 224, 190), 0.20), extra=("bone", 0.30)),
    "SEERE":          dict(plate=("silver", 0.50, (236, 240, 226)), trim=("gold", 0.40), cloth=((238, 240, 214), 0.30), extra=("bone", 0.20)),
    "DANTALION":      dict(plate=("silver", 0.45, (210, 196, 250)), trim=("gold", 0.40), cloth=((120, 90, 180), 0.40), extra=("bone", 0.30)),
    "ANDROMALIUS":    dict(plate=("obsidian", 0.65, (10, 10, 36)), trim=("silver", 0.50), cloth=((24, 24, 82), 0.50), extra=("iron", 0.50)),
    "GLASYA_LABOLAS": dict(plate=("iron", 0.60, (46, 10, 10)), trim=("bronze", 0.40), cloth=((92, 10, 12), 0.50), extra=("bone", 0.25)),
    "HAAGENTI":       dict(plate=("bronze", 0.60, (120, 70, 26)), trim=("gold", 0.40), cloth=((72, 42, 30), 0.30), extra=("iron", 0.50)),
    "CROCELL":        dict(plate=("silver", 0.50, (190, 230, 246)), trim=("silver", 0.30), cloth=((204, 238, 250), 0.30), extra=("bone", 0.20)),
    "FURCAS":         dict(plate=("steel", 0.50, (140, 150, 168)), trim=("silver", 0.40), cloth=((90, 104, 132), 0.40), extra=("iron", 0.40)),
    "BALAM":          dict(plate=("bronze", 0.50, (150, 70, 24)), trim=("gold", 0.50), cloth=((150, 70, 30), 0.40), extra=("bone", 0.30)),
    "ALLOCES":        dict(plate=("gold", 0.40, (200, 120, 40)), trim=("gold", 0.60), cloth=((190, 90, 30), 0.40), extra=("bronze", 0.40)),
    "CAMIO":          dict(plate=("obsidian", 0.70, (12, 16, 24)), trim=("silver", 0.40), cloth=((26, 32, 48), 0.40), extra=("iron", 0.40)),
    "MURMUR":         dict(plate=("bone", 0.40, (120, 80, 160)), trim=("silver", 0.40), cloth=((80, 46, 122), 0.50), extra=("obsidian", 0.40)),
}

# the number of thorns of the Dark Djinn's halo ring
DARK_SPIKES = {n: 7 + (i * 5) % 6 for i, n in enumerate(NAMES)}

# weapon: target apparent length in blocks (third person). The size rule of DjinnWeaponAssetsTest: 2.2 to 3.6 for everything but the claws of Marchosias,
# so the short kinds (daggers, fans, pincers, claws) are drawn at 2.2 to 2.4. Kinds, names and acts: djinn36_weapon_data (the logic builder's table).
TARGET = {
    "BAEL": 3.1, "AGARES": 3.4, "VALEFOR": 2.3, "BARBATOS": 2.7, "GUSION": 2.9, "VEPAR": 3.0, "SHAX": 2.3, "BUER": 2.9, "ANDREALPHUS": 3.3, "KIMARIS": 3.5,
    "AMDUSIAS": 2.6, "BELIAL": 3.2, "DECARABIA": 2.3, "SEERE": 2.8, "DANTALION": 2.8, "ANDROMALIUS": 3.2, "GLASYA_LABOLAS": 2.3, "HAAGENTI": 2.7,
    "CROCELL": 3.0, "FURCAS": 3.4, "BALAM": 3.1, "ALLOCES": 3.0, "CAMIO": 2.8, "MURMUR": 2.8,
}
# the six Djinn whose Look has no tail (Look.Tail.NONE in Djinn.java): no tail piece is built and the motion row has no tail segments
TAIL_NONE = {"BAEL", "GUSION", "BUER", "DANTALION", "CROCELL", "FURCAS"}

# particle palette: the ambient dust of each Djinn (hex, hex) is derived from the colours in Java; this table is the PYTHON mirror the check uses:
# name -> (main key, main rate, second key, second rate, anchor)
AMBIENT = {
    "BAEL": ("mote", 9, "spark", 3, "ABOVE"), "AGARES": ("bubble", 8, "ash", 3, "FEET"), "VALEFOR": ("crit", 9, "spark", 4, "WINGS"),
    "BARBATOS": ("petal", 7, "feather", 3, "WINGS"), "GUSION": ("drip", 7, "mote2", 4, "ABOVE"), "VEPAR": ("bubble", 11, "snow", 3, "WINGS"),
    "SHAX": ("feather", 9, "enchant", 5, "ABOVE"), "BUER": ("spore", 8, "petal", 4, "BODY"), "ANDREALPHUS": ("mote2", 10, "enchant", 5, "WINGS"),
    "KIMARIS": ("smoke", 9, "flame", 4, "FEET"), "AMDUSIAS": ("note", 4, "mote", 7, "ABOVE"), "BELIAL": ("ash", 8, "smoke", 5, "ABOVE"),
    "DECARABIA": ("drip", 5, "shard", 5, "FEET"), "SEERE": ("wind", 12, "snow", 3, "FEET"), "DANTALION": ("enchant", 11, "ink", 3, "ABOVE"),
    "ANDROMALIUS": ("ink", 7, "crit", 6, "BODY"), "GLASYA_LABOLAS": ("ember", 10, "ash", 5, "BODY"), "HAAGENTI": ("flame", 6, "smoke", 5, "HAND"),
    "CROCELL": ("snow", 12, "shard", 3, "WINGS"), "FURCAS": ("snow", 7, "wind", 5, "BODY"), "BALAM": ("flame", 8, "mote", 4, "ABOVE"),
    "ALLOCES": ("ember", 9, "spark", 4, "WINGS"), "CAMIO": ("feather", 6, "note", 3, "WINGS"), "MURMUR": ("spark", 6, "note", 3, "HAND"),
}

# per-Djinn motion row (flap, tailSegs, haloSpin, backSpin, backPulse, weaponScale, tailWave): all distinct (the unit test asks for it)
STYLES = {
    "BAEL": (0.9, 4, 0.0, 0.0, 0.02, 0.75, 0.7), "AGARES": (0.7, 6, 0.0, 0.0, 0.00, 0.80, 1.4), "VALEFOR": (1.4, 4, 1.8, 0.0, 0.07, 0.95, 1.3),
    "BARBATOS": (0.8, 5, 1.0, 0.0, 0.03, 0.80, 1.1), "GUSION": (0.6, 3, 1.6, 0.4, 0.02, 0.85, 0.6), "VEPAR": (0.65, 4, 0.0, 1.0, 0.00, 0.80, 1.5),
    "SHAX": (1.2, 2, 0.5, 0.0, 0.04, 0.85, 0.5), "BUER": (0.9, 4, 0.7, 1.6, 0.00, 0.80, 0.8), "ANDREALPHUS": (1.0, 5, 0.9, 0.0, 0.09, 0.78, 1.0),
    "KIMARIS": (1.3, 6, 0.0, 0.0, 0.05, 0.70, 1.2), "AMDUSIAS": (1.1, 3, 1.3, 0.0, 0.08, 0.90, 0.9), "BELIAL": (0.5, 3, 0.4, 0.3, 0.06, 0.72, 0.4),
    "DECARABIA": (0.75, 3, 2.2, 0.5, 0.00, 0.80, 0.7), "SEERE": (1.9, 4, 2.6, -1.8, 0.00, 0.85, 1.7), "DANTALION": (0.9, 2, 1.4, 0.0, 0.06, 0.88, 0.6),
    "ANDROMALIUS": (0.8, 5, 0.6, 0.0, 0.10, 0.80, 0.9), "GLASYA_LABOLAS": (1.5, 5, 0.0, 0.0, 0.00, 1.00, 1.6), "HAAGENTI": (0.6, 4, 0.0, 0.9, 0.00, 0.75, 0.6),
    "CROCELL": (1.1, 2, 2.8, 0.6, 0.01, 0.85, 0.5), "FURCAS": (0.55, 2, 0.0, 0.0, 0.00, 0.72, 0.4), "BALAM": (1.2, 5, 1.1, 0.0, 0.11, 0.82, 1.3),
    "ALLOCES": (1.6, 4, 1.7, 1.4, 0.04, 0.76, 1.1), "CAMIO": (1.7, 3, 0.0, 0.7, 0.00, 0.90, 1.0), "MURMUR": (0.85, 3, 0.8, 0.0, 0.12, 0.82, 0.7),
}


def style_row(name):
    row = list(STYLES[name])
    if name in TAIL_NONE:
        row[1] = 0
        row[6] = 1.0
    return tuple(row)


def ids():
    return [n.lower() for n in NAMES]


def row(name):
    for r in ROWS:
        if r[0] == name:
            return r
    raise KeyError(name)


def djinn_dict(name):
    """The dict the old generators (gen_djinn_art / gen_djinn_unique) pass around."""
    _, disp, title, aff, col, glow = row(name)
    h, w, t, ha, ears = FALLBACK_LOOK[name]
    return dict(name=name, id=name.lower(), display=disp, title=title, affinity=aff, color=col, glow=glow, form=disp + " Maso",
                look=dict(horns=h, wings=w, tail=t, halo=ha, ears=ears))
