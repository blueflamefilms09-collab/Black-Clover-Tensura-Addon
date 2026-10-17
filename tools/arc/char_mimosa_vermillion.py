"""Why: Mimosa Vermillion, the Golden Dawn's healer and the gentle Vermillion who chose another squad than her family's: the kindest face of the juniors in the Sanctuary,
who tells a delver to rest in a Safe Zone. Look (summarised from search results, see docs/arc/npcb.md section 10; the sources disagreed on hair and eyes): a young woman with long
wavy golden hair tied with a pink bow, green eyes, a cream blouse, short puffy white trousers and a brown corset under the Golden Dawn's gold mantle with its blue-furred shoulders.
Added for the game: the plant-green glow of the floating grimoire, and the blush.
"""
SPEC = {
    "id": "mimosa_vermillion",
    "name": "Mimosa Vermillion",
    "role": "npc",
    "build": "slim",
    "height": 1.84,
    "skin": "fair",
    "face": {"expression": "shy", "eyes": "big", "eye_color": "#58a85a", "brows": "sad", "mouth": "smile", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "long_wavy", "color": "#f2d878", "ribbon": "#f08ab8"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#f0e6c8", "trim": "#c8b48c", "length": "hip", "collar": "stand"},
        "sleeves": {"kind": "long", "cuff": "#c8b48c"},
        "legs": {"kind": "shorts", "color": "#f4f0e8"},
        "boots": {"kind": "boots", "color": "#6a4a30", "height": "knee"},
        "belt": {"kind": "plain", "color": "#7a5a3a", "buckle": "#c8a050"},
    },
    "cape": {"kind": "mantle", "color": "#c8a860", "trim": "#f0e0a8", "length": 0.6, "emblem": {"kind": "sun", "color": "#f0e0a8", "size": 7}, "clasp": "#c8a050"},
    "accessories": [{"kind": "shoulder_fur", "color": "#5f7fb0"}],
    "props": {"float": {"kind": "grimoire", "color": "#c8c858", "glow": "#8cc86a", "length": 0.8}},
    "glow": {"color": "#8cc86a", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Golden Dawn (arc/knights/GoldenDawnCast). Plant-green glow on the floating grimoire only.",
}
