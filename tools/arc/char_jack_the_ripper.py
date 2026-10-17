"""Why: Jack the Ripper, captain of the Green Mantis: the grinning, tall commoner who tells a delver that everything has a seam, and the Sanctuary's cheerful menace. Look (summarised
from search results, see docs/arc/npcb.md section 10): a very tall man (197 cm in the listing) with a shaggy head, a sadistic grin and a habit of licking his blades, in the Green
Mantis's green mantle; his Slash Magic grows mana blades on his forearms, which are the glowing edges built here. Added for the game: the torn sleeves, the dark-green hair and the
exact number of blades (the kit builds three on each forearm).
"""
SPEC = {
    "id": "jack_the_ripper",
    "name": "Jack the Ripper",
    "role": "npc",
    "build": "lanky",
    "height": 2.2,
    "skin": "tan",
    "face": {"expression": "sinister", "eyes": "narrow", "eye_color": "#3cb074", "brows": "angry", "mouth": "grin"},
    "hair": {"style": "shaggy", "color": "#1f3a2a"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#2e8a5a", "trim": "#c8e0c8", "length": "thigh", "ragged": True},
        "sleeves": {"kind": "torn"},
        "legs": {"kind": "baggy", "color": "#1c2a22"},
        "boots": {"kind": "boots", "color": "#1c1c20", "height": "calf"},
        "gloves": {"kind": "wraps", "color": "#d8d2c0"},
        "belt": {"kind": "double", "color": "#2a2218", "buckle": "#c8e0c8"},
    },
    "cape": {"kind": "tattered", "color": "#2e8a5a", "trim": "#c8e0c8", "length": 0.78, "emblem": {"kind": "crest", "color": "#c8e0c8", "size": 8}, "clasp": "#c8e0c8"},
    "glow": {"color": "#3cb074", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Green Mantis (arc/knights/CaptainsCast). No grimoire: the slash blades on the forearms are the whole of his magic.",
}


def extra(kit, rig):
    """Slash Magic: long mana blades laid along the outside of each forearm and reaching past the fist, glowing along the edge (three on each arm, the middle one the
    longest). Built from the kit's own painters, the way the Heath Grice frost is, and mirrored onto the right forearm; they ride on the forearm bones, so they swing with every
    clip."""
    d = rig.d
    blade = kit.metal(kit.shades("#d8f0d8", 5, 1.25), 41, glow=(60, 176, 116, 210), emboss=False)
    edge = kit.metal(kit.shades("#3cb074", 5, 1.2), 42, glow=(60, 176, 116, 240), emboss=False)
    for k, (dz, extra_len, fwd, out) in enumerate(((-1.3, 4.0, 186, -6), (0.0, 7.0, 190, -10), (1.3, 4.0, 194, -14))):
        rig.spike("forearm_l", (d.arm_x + d.aw / 2 - 0.1, d.elbow_y - 0.5, dz), 1.5, d.fore + extra_len, fwd, out, blade if k % 2 == 0 else edge, fracs=(0.7, 0.3), taper=0.45)
        for cube in rig.m.bones["forearm_l"]["cubes"][-2:]:
            rig.pk.twin(rig.m, "forearm_r", cube)
