"""Why: Charmy's cotton sheep comes in two bodies, the plain one (striker and binder) and the cook (a chef's hat, a blue neckerchief and a frying pan), which the art kit builds as two character
files; this module is the one place their shared design lives so the two cannot drift apart. It only returns a spec dict (docs/arc/remodel.md).
"""


def spec(cook):
    out = {
        "id": "cotton_sheep_cook" if cook else "cotton_sheep",
        "name": "Cotton Sheep Cook" if cook else "Cotton Sheep",
        "role": "boss",
        "build": "average",
        "height": 1.9,
        "widen": 1.3,
        "skin": ["#d8d0be", "#e8e2d2", "#f4f0e6", "#fffaf0"],
        "face": {"expression": "calm", "eyes": "round", "eye_color": "#c8d4e0", "sclera": "#fffdf4", "brows": "thin", "mouth": "smile", "nose": "small", "marks": ["blush"]},
        "hair": {"style": "shaggy", "color": "#fbf6ea"},
        "outfit": {
            "top": {"kind": "tunic", "color": "#f4efe2", "trim": "#d8d0be", "length": "thigh", "collar": "fur", "collar_color": "#fffaf0", "material": "fur"},
            "inner": {"kind": "bare"},
            "sleeves": {"kind": "long", "color": "#f4efe2", "cuff": "#fffaf0"},
            "legs": {"kind": "shorts", "color": "#f4efe2"},
            "boots": {"kind": "boots", "color": "#b8a890", "height": "ankle"},
            "gloves": {"kind": "none"},
        },
        "accessories": [{"kind": "shoulder_fur", "color": "#fffaf0"}],
        "horns": {"kind": "ram", "color": "#e8dcc0", "color2": "#b8a888", "size": 1.1},
        "ears": {"kind": "elf", "color": "#e8e2d2", "inner": "#f0b8b0", "size": 1.4},
        "tail": {"kind": "rat", "color": "#fffaf0", "length": 0.5},
        "glow": {"color": "#ffffff", "eyes": False, "props": False, "trim": False},
        "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed", "extra": ["spawn"]},
    }
    if cook:
        out["accessories"] = [{"kind": "shoulder_fur", "color": "#fffaf0"}, {"kind": "cap", "color": "#fffdf4", "color2": "#e8e4d8"}]
        out["outfit"]["neck"] = {"kind": "scarf", "color": "#3a6ad0"}
        out["props"] = {"right": {"kind": "mace", "color": "#6a6a72", "color2": "#8a5a3a", "length": 0.7}}
        out["clips"]["attack"] = "smash"
    return out
