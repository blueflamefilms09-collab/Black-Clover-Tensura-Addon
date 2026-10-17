"""Why: Kahono, the priestess of the Seabed Temple whose singing heals and who dreams of being an idol of the surface: the exclusive caster of the mod's Song book
(docs/exclusive/kh.md), standing in the Sanctuary where the sky is, so she has to read as bright and young. Look (summarised from search results, see docs/arc/npcb.md section 10; no
source described her hair or robes, so both are a choice): a small young woman with sea-blue twin tails tied with lilac ribbons, a white-and-aqua temple dress with a shell pendant,
and a floating grimoire in the mod's Song colours (a lilac glow).
"""
SPEC = {
    "id": "kahono",
    "name": "Kahono",
    "role": "npc",
    "build": "small",
    "height": 1.7,
    "skin": "fair",
    "face": {"expression": "cheerful", "eyes": "big", "eye_color": "#3a90c8", "brows": "raised", "mouth": "smile", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "twin_tails", "color": "#6aa0d0", "ribbon": "#c890d8"},
    "outfit": {
        "top": {"kind": "dress", "color": "#f0f4f8", "trim": "#4aa8c0", "length": "knee", "collar": "wide"},
        "sleeves": {"kind": "bell", "cuff": "#4aa8c0"},
        "legs": {"kind": "leggings", "color": "#e8f0f4"},
        "boots": {"kind": "sandals", "color": "#c8b48c"},
        "belt": {"kind": "obi", "color": "#4aa8c0", "buckle": "#e8f0f8"},
        "neck": {"kind": "necklace", "color": "#e8f0f8", "gem": "#78d0e8"},
    },
    "accessories": [{"kind": "earrings", "color": "#e8f0f8"}],
    "props": {"float": {"kind": "grimoire", "color": "#e8d8f0", "glow": "#c890d8", "length": 0.75}},
    "glow": {"color": "#c890d8", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Seabed Temple (arc/knights/SeabedTempleCast). The lilac glow is the Song book's colour (grimoire/CanonBook KAHONO).",
}
