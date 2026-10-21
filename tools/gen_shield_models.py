"""Writes the item model jsons of every shield-class item with vanilla's shield display (1.110.0, "fix shield relics/weapons to be used and facing the same way
as a basic Minecraft shield").

Every shield of the mod is a builtin/entity model (WeaponRenderer draws its 3D mesh), so its hand pose comes from the model's display block exactly as the
vanilla shield's does. The numbers below are minecraft:item/shield and minecraft:item/shield_blocking of 1.21.1. The 3D mesh is put on the same frame as
vanilla's ShieldModel by client/WeaponRenderer (visual/shields/ShieldFrame: centred on the plate, its face on the plate's front plane, y up, face on +z),
so the same display values turn it the same way. The gui entry stays an identity: the inventory keeps the old flat sprite (the <id>_icon model).

The set of ids is visual/shields/ShieldSet.java; ShieldModelsTest reads these jsons and compares them with ShieldPreset.java. Run: python3 tools/gen_shield_models.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
M = os.path.join(ROOT, "src/main/resources/assets/nusmp/models/item")

IDS = "magebane_shield legendary_shield aegis_of_the_abyss celestial_support heros_shield thunder_shield calamity_wave_aegis hylian_hero_shield".split()


def t(rot, tr, sc):
    return {"rotation": list(rot), "translation": list(tr), "scale": [sc, sc, sc]}


# minecraft:item/shield (1.21.1)
SHIELD = {
    "thirdperson_righthand": t((0, 90, 0), (10, 6, -4), 1.0),
    "thirdperson_lefthand": t((0, 90, 0), (10, 6, 12), 1.0),
    "firstperson_righthand": t((0, 180, 5), (-10, 2, 0), 1.25),
    "firstperson_lefthand": t((0, 180, 5), (10, 0, 0), 1.25),
    "gui": t((0, 0, 0), (0, 0, 0), 1.0),            # vanilla: [15, -25, -5] on the entity model; here the inventory shows the flat sprite icon, which stays upright
    "fixed": t((0, 180, 0), (-2, 4, -5), 0.5),
    "ground": t((0, 0, 0), (4, 4, 2), 0.25),
    "head": t((0, 0, 0), (0, 0, 0), 1.0),           # vanilla has no head entry (the identity)
}
# minecraft:item/shield_blocking (1.21.1): only the two hands differ
BLOCKING = dict(SHIELD)
BLOCKING.update({
    "thirdperson_righthand": t((45, 135, 0), (3.51, 11, -2), 1.0),
    "thirdperson_lefthand": t((45, 135, 0), (13.51, 3, 5), 1.0),
    "firstperson_righthand": t((0, 180, -5), (-15, 5, -11), 1.25),
    "firstperson_lefthand": t((0, 180, -5), (5, 5, -11), 1.25),
})

ORDER = ["thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "gui", "fixed", "ground", "head"]


def model(i, display, blocking):
    d = {
        "parent": "minecraft:builtin/entity",
        "gui_light": "front",
        "textures": {"particle": "nusmp:item/" + i},
        "display": {k: display[k] for k in ORDER},
    }
    if not blocking:
        d["overrides"] = [{"predicate": {"blocking": 1}, "model": "nusmp:item/" + i + "_blocking"}]
    return d


def main():
    for i in IDS:
        for suf, disp in (("", SHIELD), ("_blocking", BLOCKING)):
            path = os.path.join(M, i + suf + ".json")
            with open(path, "w") as f:
                json.dump(model(i, disp, suf != ""), f, indent=2)
                f.write("\n")
            print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
