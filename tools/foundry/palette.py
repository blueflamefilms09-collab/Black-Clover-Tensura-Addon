"""Why: the 400 foundry mobs must not look alike, and the cheapest honest way to guarantee it is to give every roster row a three-colour PALETTE written with names
(`"crimson bone gold"`), so a human reads the row and sees the creature, and the guard can compare palettes as plain triples. The names are hand-picked mid-tones that
survive the dim light of the dungeon (the kit paints hard-edged ramps around each colour, see tools/arc_kit/color.py); the same table also names skin and hair colours.
Nothing here imports the art kit, so the roster guard and the JUnit-side tests can run without numpy or Pillow.
"""

COLORS = {
    # reds and oranges
    "crimson": "#a8202c", "blood": "#7a1018", "ember": "#d8501c", "rust": "#9a4a22", "brick": "#8c3a2c", "rose": "#c8506c", "coral": "#e07060",
    "scarlet": "#d02828", "salmon": "#e8806a", "terracotta": "#b85a3a", "clay": "#a0603a", "burgundy": "#6a1a2a", "maroon": "#5a1420", "wine": "#5a1a34",
    "magma": "#e8641c", "flame": "#f08020", "cinnabar": "#c43c2a",
    # yellows and golds
    "amber": "#d89020", "gold": "#d4a838", "saffron": "#e8b830", "sand": "#c8b078", "tan": "#b89a6a", "ochre": "#b8883a", "honey": "#e0a840", "bronze": "#9a6a32",
    "copper": "#b8683a", "mustard": "#c8a828", "wheat": "#d8c07a", "lemon": "#e8e060", "goldenrod": "#d8b020", "brass": "#a88a3a",
    # greens
    "moss": "#5a7a3a", "forest": "#2c5a34", "jade": "#2a9a72", "lime": "#8ab838", "olive": "#6a6a2a", "sage": "#8a9a70", "fern": "#4a8a4a", "toxic": "#70c030",
    "swamp": "#3a4a2a", "mint": "#78c8a8", "pine": "#1e4a34", "emerald": "#1a8a50", "chartreuse": "#a0d030", "lichen": "#8aa860", "bile": "#98a828",
    # blues and cyans
    "azure": "#3a78c8", "cobalt": "#2a50a8", "navy": "#1c2c5c", "sky": "#78b0e0", "ice": "#a8d8f0", "teal": "#2a8a8a", "aqua": "#40b8b8", "storm": "#4a5a78",
    "steel": "#8a98a8", "slate": "#4a5260", "turquoise": "#30c0b0", "seafoam": "#70b098", "cerulean": "#3a90c0", "denim": "#3a5a88", "periwinkle": "#7a80d0",
    "frost": "#c0e4f0", "deep": "#1c4a78",
    # purples and pinks
    "violet": "#6a3aa8", "indigo": "#3a2c80", "plum": "#5a2a58", "orchid": "#b068c0", "lavender": "#a898d8", "mauve": "#8a6a88", "magenta": "#b02880",
    "fuchsia": "#d040a0", "pink": "#e890b0", "peach": "#f0b898", "amethyst": "#8a50b8", "twilight": "#4a3a78", "grape": "#4a2468",
    # neutrals, metals, bones
    "ivory": "#ece4d0", "cream": "#e8dcc0", "bone": "#d4cbb0", "ash": "#a0a0a8", "silver": "#c0c4cc", "charcoal": "#34343c", "ink": "#1c1c24", "pitch": "#101014",
    "white": "#f4f4f8", "smoke": "#6a6a74", "fog": "#b8c0c8", "iron": "#5a5e68", "pewter": "#7a7e88", "pearl": "#f0eaf0", "halo": "#f4e8b0", "dawn": "#f0c890",
    "chalk": "#e0e0d8", "granite": "#6e6a68", "gunmetal": "#444a54", "platinum": "#d8dce4",
    # darks and browns
    "void": "#2a1a44", "abyss": "#1a2038", "gloom": "#2a2a3a", "night": "#14183a", "soot": "#2a2624", "mire": "#2a3428", "tar": "#181818", "leather": "#7a5434",
    "walnut": "#4a3422", "umber": "#5a3a22", "chestnut": "#7a4a2a", "mahogany": "#4a2418", "rosewood": "#6a3a3a", "mud": "#5a4a30", "dust": "#9a8a70",
    "coal": "#22201e", "ebony": "#16121a", "oxblood": "#4a0e14",
    # plain names (the skin tone names are also colours here; as a skin they still mean the kit's own tone ramps)
    "red": "#b82020", "brown": "#6a4a2a", "green": "#3a8a3a", "blue": "#2a5ac0", "purple": "#7a3a9a", "orange": "#e07a20", "yellow": "#e8d030", "black": "#18181c",
    "gray": "#808088", "dark": "#2c2420", "light": "#e8d8c0",
    # glows
    "lumen": "#fff0b0", "aether": "#a0e8ff", "spirit": "#b8ffe0", "hellfire": "#ff5a1a", "voidlight": "#b060ff", "venom": "#98ff40", "bloodlight": "#ff3050",
    "moonlight": "#c8d8ff", "sunfire": "#ffc840",
}

SKIN_NAMES = ("pale", "fair", "light", "tan", "brown", "dark", "olive", "ashen", "grey", "red", "blue", "green", "purple", "undead")


def hexof(name):
    """A palette name, a skin tone name, or a '#rrggbb' -> something the kit accepts (a name it knows, or a hex). Unknown names raise with the valid list."""
    s = name.strip().lower()
    if s.startswith("#"):
        return s
    if s in COLORS:
        return COLORS[s]
    raise KeyError("unknown colour name %r; use '#rrggbb', a skin tone or one of: %s" % (name, ", ".join(sorted(COLORS))))


def skin_or_color(name):
    """A skin spec for the kit: a tone name stays a name (the kit has a 4-tone ramp for it), a palette name or hex becomes a hex (the kit ramps it)."""
    s = name.strip().lower()
    if s in SKIN_NAMES:
        return s
    return hexof(s)
