"""Why: Dorothy Unsworth, captain of the Coral Peacock: nearly always asleep, never unaware, and fond of macarons, the Sanctuary's dreamiest captain, who tells a delver that rest is a
weapon. Look (summarised from search results, see docs/arc/npcb.md section 10): a small woman with short lilac hair, a pointed witch's hat decorated with flowers and a coral cloak with
the squad's peacock; she is drawn asleep (closed eyes, a faint smile, a blush) because that is how she is nearly always seen. Added for the game: the dress and its colours (no source
describes them), the pink glow of the floating grimoire.
"""
SPEC = {
    "id": "dorothy_unsworth",
    "name": "Dorothy Unsworth",
    "role": "npc",
    "build": "small",
    "height": 1.64,
    "skin": "fair",
    "face": {"expression": "cheerful", "eyes": "closed", "eye_color": "#4a6aa8", "brows": "flat", "mouth": "smile", "marks": ["blush"]},
    "hair": {"style": "bob", "color": "#b090d0"},
    "outfit": {
        "top": {"kind": "dress", "color": "#f0e4ee", "trim": "#e88aa0", "length": "knee", "collar": "wide"},
        "sleeves": {"kind": "bell", "cuff": "#e88aa0"},
        "legs": {"kind": "leggings", "color": "#f4f0f4"},
        "boots": {"kind": "shoes", "color": "#e88aa0", "height": "ankle"},
        "belt": {"kind": "sash", "color": "#e88aa0", "buckle": "#f0d8e0"},
    },
    "cape": {"kind": "cloak", "color": "#e88aa0", "trim": "#f0d8e0", "lining": "#f0d8e0", "length": 0.78, "emblem": {"kind": "wings", "color": "#40b0b8", "size": 8}, "clasp": "#f0d8e0"},
    "accessories": [{"kind": "witch_hat", "color": "#b090d0", "color2": "#e88aa0"}, {"kind": "flower", "color": "#f08ab8", "color2": "#f2d070"}],
    "props": {"float": {"kind": "grimoire", "color": "#e88aa0", "glow": "#ffb0d0", "length": 0.75}},
    "glow": {"color": "#ffb0d0", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Coral Peacock (arc/knights/CaptainsCast). Drawn asleep: the closed eyes are the point.",
}
