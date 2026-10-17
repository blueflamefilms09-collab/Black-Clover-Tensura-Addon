"""Why: Eina Tulle, the half-elf adviser of the Guild who keeps Bell's records and tells every adventurer which floor to try next: the guide of the DanMachi saga, the Chronicle's voice, and
the friend who asks the player for the hunt of the Minotaur. Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a young woman with long brown hair, the pointed ears of a half-elf,
glasses and the Guild's dark green uniform with a white blouse. Added for the game, not from the source: the ledger she holds open (the kit's open book, which glows softly like the Chronicle).
"""
SPEC = {
    "id": "eina_tulle",
    "name": "Eina Tulle",
    "role": "npc",
    "build": "slim",
    "height": 1.8,
    "skin": "fair",
    "face": {"expression": "calm", "eye_color": "#4a9a5a", "brows": "thin"},
    "hair": {"style": "long", "color": "#7a5230"},
    "ears": {"kind": "elf", "size": 1.0},
    "outfit": {
        "top": {"kind": "uniform", "color": "#2c4a3a", "trim": "#d0b060", "length": "hip", "collar": "stand"},
        "inner": {"kind": "shirt", "color": "#f2f0ea"},
        "legs": {"kind": "skirt", "color": "#2c4a3a"},
        "boots": {"kind": "shoes", "color": "#4a3426"},
        "belt": {"kind": "plain", "color": "#3a2a1c", "buckle": "#d0b060"},
        "neck": {"kind": "cravat", "color": "#d0b060"},
    },
    "accessories": [{"kind": "glasses", "color": "#c8a050"}],
    "props": {"left": {"kind": "grimoire", "color": "#6a4a2a", "color2": "#d0b060", "glow": "#ffe08a"}},
    "glow": {"color": "#ffe08a", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "none", "cast": "book", "idle": "relaxed"},
}
