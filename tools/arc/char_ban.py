"""Why: Ban, the Fox's Sin of Greed, who cannot die because he drank from the Fountain of Youth, steals with Snatch and fights with the four-section staff Courechouse.
The look follows what the sources agree on (docs/arc/ban.md lists them): a very tall, lean, muscular man with pale spiky hair and scarlet eyes, an open jacket over a bare
chest, a fox mark low on the left of the stomach. Added for the game and NOT from a source: the exact colours of the jacket and trousers, the scar on the cheek and the
stubble, Courechouse drawn as four iron sections joined by glowing links (a prop of this character's own, registered here as the kit's manual allows: the kit's staff is a
mage's staff with a crystal), the green of the glow (the Fountain's colour). The `form2` is his last life, when the Fountain's light has taken him over: the jacket is
darker and torn, green lines run over his face, the eyes shine and a pair of fairy wings of light opens behind him (an addition for the game: Elaine, whom he lost, is a fairy).
"""
import gen_arc_kit as kit


def p_courechouse(pb):
    """Courechouse: four iron sections of a staff, each joined to the next by a gold ring and a glowing link (the chain), a blunt iron cap at the foot and a steel point on top."""
    L = pb.L(46.0)
    iron = pb.metal(pb.col or "#9aa0aa", 3)
    gold = pb.metal("#c8a040", 6)
    g = None if pb.glow is None else (pb.glow[0], pb.glow[1], pb.glow[2], 230)
    link = pb.metal("#7be0a4", 8, glow=g)
    sec = L / 4.0
    y = -16.0
    for _k in range(4):
        pb.box(-0.8, y, -0.8, 1.6, sec - 1.8, 1.6, iron)
        pb.box(-1.1, y + sec - 1.8, -1.1, 2.2, 0.9, 2.2, gold)
        pb.box(-0.55, y + sec - 0.9, -0.55, 1.1, 0.9, 1.1, link)
        y += sec
    pb.box(-1.1, -16.8, -1.1, 2.2, 1.0, 2.2, gold)
    pb.box(-1.0, y - 0.2, -1.0, 2.0, 2.0, 2.0, gold)
    pb.box(-0.6, y + 1.8, -0.6, 1.2, 4.0, 1.2, pb.steel(2))


kit.register_prop("courechouse", kit.PropDef(p_courechouse, attack="cast_jab", cast="magic", orient=((0.0, 0.0, 1.0), "forward")))
kit.recipes.HOLD["courechouse"] = kit.recipes.HOLD["staff"]

SPEC = {
    "id": "ban",
    "name": "Ban",
    "role": "boss",
    "build": "average",
    "height": 2.12,
    "skin": "tan",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#c83a3a", "brows": "thin", "mouth": "smirk", "marks": ["scar_cheek_l", "stubble"]},
    "hair": {"style": "spiky_back", "color": "#d6dae6"},
    "outfit": {
        "top": {"kind": "vest", "color": "#5a1c24", "trim": "#2a1014", "collar": "fur", "open": True, "open_top": 9, "open_bottom": 3},
        "inner": {"kind": "bare"},
        "sleeves": {"kind": "long", "color": "#5a1c24", "cuff": "#2a1014"},
        "legs": {"kind": "trousers", "color": "#1e1c22"},
        "boots": {"kind": "boots", "color": "#241c1a", "height": "calf"},
        "gloves": {"kind": "fingerless", "color": "#241c1a"},
        "belt": {"kind": "double", "color": "#3c281c", "buckle": "#9aa0aa", "pouches": True},
    },
    "props": {"right": {"kind": "courechouse", "color": "#9aa0aa", "glow": "#7be0a4"}},
    "glow": {"color": "#7be0a4", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "sweep", "cast": "magic", "idle": "relaxed", "extra": []},
    "form2": {
        "name": "Ban, the Last Life",
        "face": {"eye_color": "#7be0a4", "eye_glow": "#a8ffcc", "marks": ["scar_cheek_l", "light_lines"]},
        "hair": {"color": "#eef2f8"},
        "outfit": {
            "top": {"color": "#40161c", "ragged": True},
            "sleeves": {"kind": "torn"},
        },
        "wings": {"kind": "fairy", "color": "#a8ffcc", "color2": "#e8fff0", "glow": "#7be0a4", "span": 1.0},
        "glow": {"color": "#a8ffcc", "eyes": True, "props": True, "trim": True},
    },
}
