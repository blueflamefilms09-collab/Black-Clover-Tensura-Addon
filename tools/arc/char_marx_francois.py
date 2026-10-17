"""Why: Marx Francois, aide to the Wizard King: the exact, long-suffering keeper of the Sanctuary's records, who explains the rules of this mod's dungeon (parties, the System, Guardian
floors, the Charter, the Hourglass), so he has to read as formal and tidy. Look (summarised from search results, see docs/arc/npcb.md section 10; only the mushroom-cut hair and the
formal robes are from a source, the colours are a choice): a smaller man of 1.67 m with a neat bowl-cut of dark hair, a serious face and a deep-blue aide's coat with silver trim and a short
cape with the clover; he holds his ledger, an open book, in his left hand, and his talking gesture is writing in the air.
"""
SPEC = {
    "id": "marx_francois",
    "name": "Marx Francois",
    "role": "npc",
    "build": "average",
    "height": 1.86,
    "skin": "light",
    "face": {"expression": "stern", "eyes": "sharp", "eye_color": "#5a6aa8", "brows": "flat", "mouth": "line"},
    "hair": {"style": "bob", "color": "#3a2a22"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2a3a6a", "trim": "#c8d4e8", "collar": "high", "buttons": True, "length": "knee"},
        "inner": {"kind": "shirt", "color": "#e8ecf4"},
        "sleeves": {"kind": "long", "cuff": "#c8d4e8"},
        "legs": {"kind": "trousers", "color": "#1e2438"},
        "boots": {"kind": "boots", "color": "#1e2028", "height": "calf"},
        "gloves": {"kind": "gloves", "color": "#e8ecf4"},
        "belt": {"kind": "plain", "color": "#1e2438", "buckle": "#c8d4e8"},
    },
    "cape": {"kind": "cape", "color": "#2a3a6a", "trim": "#c8d4e8", "length": 0.6, "emblem": {"kind": "clover", "color": "#c8d4e8", "size": 8}, "clasp": "#c8d4e8"},
    "props": {"left": {"kind": "grimoire", "color": "#2a3a6a", "length": 0.65}},
    "glow": {"color": "#8aa0d0", "eyes": False, "props": False, "trim": False},
    "clips": {"cast": "book", "idle": "proud"},
    "notes": "The court (arc/knights/CourtCast). The ledger is a kit grimoire with no glow; the book cast style is his talking clip.",
}
