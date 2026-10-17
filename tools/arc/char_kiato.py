"""Why: Kiato, the dancer-warrior of the Seabed Temple and Kahono's older brother: the exclusive caster of the mod's Butoh book (docs/exclusive/kt.md), who teaches a delver to step
out of a blow, so he has to read as lean, quiet and ready. Look (summarised from search results, see docs/arc/npcb.md section 10; no source described his hair or clothes, so both are a
choice): a tall lean man with short dark-blue hair and an aqua headband, bare arms wrapped at the wrist, wide white dance trousers, a teal vest and sash, and a curved dance-sword at his
hip whose edge glows aqua (the mod's Butoh colour).
"""
SPEC = {
    "id": "kiato",
    "name": "Kiato",
    "role": "npc",
    "build": "average",
    "height": 1.94,
    "skin": "tan",
    "face": {"expression": "calm", "eyes": "sharp", "eye_color": "#3aa8c0", "brows": "thick", "mouth": "line"},
    "hair": {"style": "short", "color": "#1c2a4a"},
    "outfit": {
        "top": {"kind": "vest", "color": "#2a5a6a", "trim": "#78d0e8"},
        "inner": {"kind": "bare"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "hakama", "color": "#e8f0f4"},
        "boots": {"kind": "sandals", "color": "#c8b48c"},
        "belt": {"kind": "sash", "color": "#78d0e8", "buckle": "#e8f0f8"},
        "bandages": {"parts": ["forearm_l", "forearm_r"], "color": "#e8f0f4"},
    },
    "accessories": [{"kind": "headband", "color": "#78d0e8"}],
    "props": {"right": {"kind": "katana", "color": "#c8d8e8", "color2": "#2a5a6a", "glow": "#78d0e8", "sheathed": True}},
    "glow": {"color": "#78d0e8", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Seabed Temple (arc/knights/SeabedTempleCast). The blade rests in its scabbard; the aqua is the Butoh book's colour (grimoire/CanonBook KIATO).",
}
