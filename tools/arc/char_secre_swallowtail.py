"""Why: Secre Swallowtail, the deadpan keeper of seals, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; her Seal Magic and its cyan neon
look are already in the mod, docs/owner_specs/sealing_secre_spec.md). Look (from the series and the wiki summaries): short black bob hair, a pale small body with dark
under-eye bags and flat slanted eyes, a black dress with thin straps and a short ruffled hem like feathers, black slippers, the Black Bull cloak held by a grey cord. Her
open grimoire floats beside her, its pages glowing cyan (the colour of her seals). Unverified: the length of the dress, the grey cord (drawn as a grey strap). The grimoire,
the cord's knot and the cloak's bull glow cyan. She does not wear the horns of her forbidden magic here. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "secre_swallowtail",
    "name": "Secre Swallowtail",
    "role": "npc",
    "build": "slim",
    "height": 1.72,
    "skin": "pale",
    "face": {"expression": "tired", "eyes": "narrow", "eye_color": "#2a2a38", "brows": "flat", "mouth": "line", "marks": ["eye_bags"]},
    "hair": {"style": "bob", "color": "#14141a"},
    "outfit": {
        "top": {"kind": "dress", "color": "#17171d", "trim": "#2c2c38", "length": "thigh", "ragged": True, "hem_trim": True},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "shoes", "color": "#15151a"},
        "neck": {"kind": "strap", "color": "#8a8a98"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.58, "ragged": True, "width": 0.9,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#8a8a98"},
    "props": {"float": {"kind": "grimoire", "color": "#1e4e7a", "color2": "#8ae8ff", "glow": "#5ad8ff"}},
    "glow": {"color": "#5ad8ff", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Black Bulls. The cyan grimoire floats beside her (her Seal Magic); no horns (they belong to her forbidden magic).",
}
