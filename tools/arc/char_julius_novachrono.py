"""Why: Julius Novachrono, the Wizard King: the first host of the Sanctuary, the one who tells the story of the arcs and whose will already surfaces in the mod's Lucius fight, so he has
to read as warm, light and a little otherworldly. Look (summarised from search results, see docs/arc/npcb.md section 10; no source gave his colours, so the white-and-gold robe and the
pale hair are a choice): a tall man with an easy smile, in a floor-length white robe with wide sleeves and gold trim and a white mantle carrying the Clover Kingdom's clover; his grimoire is
the one with no cover, a drum of loose pages, which turns slowly at his side (the same idea as the mod's 3D Time grimoire of docs/robe_anime.md and book/TimeBook). The drum is built in
extra() below because the kit has no prop for it.
"""
SPEC = {
    "id": "julius_novachrono",
    "name": "Julius Novachrono",
    "role": "npc",
    "build": "average",
    "height": 2.02,
    "skin": "fair",
    "face": {"expression": "cheerful", "eyes": "smile", "eye_color": "#c8a050", "brows": "raised", "mouth": "smile"},
    "hair": {"style": "curtains", "color": "#e0cf9a"},
    "outfit": {
        "top": {"kind": "robe", "color": "#f0ead8", "trim": "#d8b050", "collar": "wide", "length": "floor"},
        "sleeves": {"kind": "wide", "cuff": "#d8b050"},
        "legs": {"kind": "trousers", "color": "#e8e0c8"},
        "boots": {"kind": "boots", "color": "#5a4a30", "height": "ankle"},
        "belt": {"kind": "sash", "color": "#d8b050", "buckle": "#fff2b0"},
    },
    "cape": {"kind": "mantle", "color": "#f4f0e4", "trim": "#d8b050", "lining": "#d8c89a", "length": 0.7, "emblem": {"kind": "clover", "color": "#d8b050", "size": 9}, "clasp": "#d8b050"},
    "accessories": [{"kind": "tassel", "color": "#d8b050"}],
    "glow": {"color": "#e8dcc0", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "relaxed"},
    "notes": "The court (arc/knights/CourtCast). The grimoire is the coverless page drum of extra(), not a kit prop.",
}


def extra(kit, rig):
    """His Time grimoire: a drum of loose pages with no cover, floating at his right hip and turning slowly. Two square blocks of pages, one turned 45 degrees, make the octagon of a
    drum; two thin gold bands round it hold the pages together; it turns about its own axis in every clip."""
    d = rig.d
    pages = kit.fabric(kit.shades("#efe3c4", 5, 0.55), 61, period=3.0, amp=0.12, seam=False)
    gold = kit.metal(kit.shades("#d8b050", 5, 1.2), 62, emboss=False, glow=(216, 176, 80, 200))
    cx, cy, cz = -(d.tw / 2 + 6.5), d.hip_y + d.torso_h * 0.42, -1.5
    w, h = 5.4, 6.4
    rig.bone("drum", "root", (cx, cy, cz))
    rig.box("drum", (cx - w / 2, cy - h / 2, cz - w / 2), (w, h, w), pages)
    rig.box("drum", (cx - w / 2, cy - h / 2, cz - w / 2), (w, h, w), pages, rotation=(0, 45, 0), pivot=(cx, cy, cz))
    for dy in (-h / 2 + 1.0, h / 2 - 1.6):
        rig.box("drum", (cx - w / 2 - 0.25, cy + dy, cz - w / 2 - 0.25), (w + 0.5, 0.6, w + 0.5), gold)
        rig.box("drum", (cx - w / 2 - 0.25, cy + dy, cz - w / 2 - 0.25), (w + 0.5, 0.6, w + 0.5), gold, rotation=(0, 45, 0), pivot=(cx, cy, cz))
    rig.animate("drum", sway=0.0, spin=0.1, spin_axis=1)
