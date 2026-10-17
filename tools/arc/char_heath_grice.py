"""Why: Heath Grice (also spelled Graice), the Eye of the Midnight Sun member who fights with Ice magic: the first BOSS of the roster and the
reference character that proves the boss half of the art kit (a tall, thin build, a scar, belts, a glowing frost, a second form with wings and a halo, the clips of a
boss). Look (from the Black Clover wiki, see docs/arc/art_kit.md for the sources): a slim man with slanted eyes, short grey hair combed back, a long scar from the right
side of the forehead across the face to the left cheek, a black military-style jumpsuit that covers his whole body, two belts strapped round his neck, a pocket watch.
When he uses purified devil power he grows wings and a halo and his clothes turn light, which is the `form2` below.
The wiki also says he keeps checking a pocket watch (calm, calculating, puts a timer on everything), so one hangs from his belt. Added for the game, not from the source:
the orbiting ice crystal and the frost on his shoulders and forearms (his magic is Ice), the faint glow of the watch, and the eye colours (a guess).
"""
SPEC = {
    "id": "heath_grice",
    "name": "Heath Grice",
    "role": "boss",
    "build": "slim",
    "height": 2.08,
    "skin": "pale",
    "face": {"expression": "cold", "eyes": "sharp", "eye_color": "#8aa0d8", "eye_glow": "#a6e4ff", "brows": "flat", "mouth": "line", "marks": ["scar_diag", "eye_bags"]},
    "hair": {"style": "slick", "color": "#c4c8d8"},
    "outfit": {
        "top": {"kind": "jumpsuit", "color": "#15151c", "trim": "#4a4a5a", "buttons": True},
        "sleeves": {"kind": "long", "cuff": "#2a2a34"},
        "legs": {"kind": "trousers", "color": "#15151c"},
        "boots": {"kind": "boots", "color": "#101014", "height": "calf"},
        "gloves": {"kind": "gloves", "color": "#101014"},
        "belt": {"kind": "double", "color": "#2a2a32", "buckle": "#9aa4b8"},
        "neck": {"kind": "belts", "color": "#2a2a32"},
    },
    "accessories": [{"kind": "pocket_watch", "color": "#b8c0d0", "glow": "#a6e4ff"}],
    "props": {"float": {"kind": "orb", "color": "#bfe8ff", "glow": "#8ad8ff"}},
    "glow": {"color": "#8ad8ff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud", "extra": []},
    "form2": {
        "name": "Heath Grice, Purified",
        "hair": {"color": "#f4f4fa"},
        "face": {"eye_color": "#f2d070", "eye_glow": "#ffe58a", "marks": ["scar_diag", "light_lines"]},
        "outfit": {"top": {"color": "#e4e8f4", "trim": "#c8a040"}, "sleeves": {"cuff": "#c8a040"}, "legs": {"color": "#e4e8f4"}, "boots": {"color": "#c8ccd8"},
                   "gloves": {"color": "#c8ccd8"}, "belt": {"color": "#c8a040", "buckle": "#fff2b0"}, "neck": {"color": "#c8a040"}},
        "wings": {"kind": "angel", "color": "#e8ecf8", "color2": "#fff2c8", "glow": "#ffe58a", "span": 1.0},
        "halo": {"kind": "ring", "color": "#f2d070", "glow": "#ffe58a", "radius": 1.0},
        "glow": {"color": "#ffe58a", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """Frost: a ring of ice shards on each shoulder and a fan of icicles along the forearms, all glowing. Built from the kit's own painters; rig.facts tells
    the clip generator nothing about them (they ride on the arm bones)."""
    d = rig.d
    ice = kit.metal(kit.shades("#bfe8ff", 5, 1.25), 21, glow=(138, 216, 255, 190), emboss=False)
    ice2 = kit.metal(kit.shades("#8ad8ff", 5, 1.2), 22, glow=(138, 216, 255, 230), emboss=False)
    for k, (dz, ln, tilt) in enumerate(((-1.2, 5.0, 14), (0.0, 7.0, 4), (1.2, 5.5, -8))):
        c = rig.spike("arm_l", (d.arm_x + 0.2, d.sh_y - 0.6, dz), 1.8, ln, tilt, -18 - 6 * k, ice if k % 2 else ice2, fracs=(0.62, 0.38))
        for cube in rig.m.bones["arm_l"]["cubes"][-2:]:
            rig.pk.twin(rig.m, "arm_r", cube)
    for k in range(3):
        c = rig.box("forearm_l", (d.arm_x + 1.0, d.wrist_y + 1.0 + k * 1.2, -0.6 + k * 0.4), (0.9, 3.5 - 0.5 * k, 1.0), ice2, rotation=(0, 0, -12 - 8 * k), pivot=(d.arm_x + 1.0, d.wrist_y + 1.0 + k * 1.2, 0))
        rig.pk.twin(rig.m, "forearm_r", c)
