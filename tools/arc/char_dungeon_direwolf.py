"""Why: the Direwolf (entity nusmp:dungeon_direwolf, dungeon/DirewolfEntity), an apex predator of the Living Dungeon's food web: a huge, heavy, scarred dire wolf with blue-grey fur, a dark ridge
of spiking mane, pale paws, lower fangs and amber eyes, that sleeps curled up when it has eaten. The owner wants every mob of the mod built on the player's rig with the art kit, and a
creature becomes an anthropomorphic design with its features kept, so this is a wolf-man: a broad, heavily furred body with a wolf's muzzle and lower fangs (made in `extra`), tall wolf
ears, a bushy tail, a dark mane that runs down the back, pale boots for the paws and claws on the hands, old scars, amber eyes. It has the kit's clips plus `sleep` (curled up, made with
tools/remodel/lib.py). Fur colours are the old model's. See docs/arc/remodel.md.
"""
from remodel import lib

ENTITIES = ["dungeon_direwolf"]

SPEC = {
    "id": "dungeon_direwolf",
    "name": "Direwolf",
    "role": "boss",
    "build": "broad",
    "height": 1.9,
    "widen": 1.1,
    "skin": ["#3a4858", "#566a7e", "#7a8fa4", "#a4b8c8"],
    "face": {"expression": "angry", "eyes": "sharp", "eye_color": "#ffb020", "eye_glow": "#ffc040", "brows": "heavy", "mouth": "fangs", "nose": "none",
             "marks": ["scar_diag", "scar_cheek_r"]},
    "hair": {"style": "shaggy", "color": "#283038", "streak": "#566a7e"},
    "outfit": {
        "top": {"kind": "vest", "color": "#4a5c70", "trim": "#283038", "length": "hip", "collar": "fur", "collar_color": "#283038", "material": "fur", "ragged": True},
        "inner": {"kind": "bare"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "shorts", "color": "#4a5c70", "stripe": None},
        "boots": {"kind": "boots", "color": "#c8d4de", "height": "ankle"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#283038", "buckle": "#7a8fa4"},
    },
    "armor": {"pieces": ["spikes"], "style": "bone", "color": "#c8d4de", "trim": "#7a8fa4"},
    "ears": {"kind": "wolf", "color": "#8aa0b4", "inner": "#283038", "size": 2.0},
    "tail": {"kind": "wolf", "color": "#566a7e", "color2": "#a4b8c8", "length": 1.5},
    "glow": {"color": "#ffb020", "eyes": True, "props": False, "trim": False},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch", "extra": ["spawn"]},
}


def extra(kit, rig):
    """The wolf's muzzle with its dark nose and the two fangs of the lower jaw, a ruff of fur at the throat, and claws on both hands. Built on the head bone from the measured body, so
    it follows the build; the fur colours are the skin's own."""
    d = rig.d
    skin = rig.look.skin
    fur = [kit.mix(c, (200, 212, 224), 0.12) for c in skin]
    muz = kit.fur(fur, salt=61, flow="y", strand=1.6, root=0.3)
    rig.box("head", (-2.4, d.head_y - 0.4, -9.0), (4.8, 3.6, 5.2), muz, faces=("front", "left", "right", "top", "bottom"))
    nose = kit.solid((26, 24, 30))
    rig.box("head", (-1.2, d.head_y + 1.9, -9.5), (2.4, 1.6, 0.8), nose)
    fang = kit.bone_paint(kit.shades("#f2efe6", 5, 1.0), 62)
    for sg in (1, -1):
        rig.box("head", (sg * 1.5 - 0.4, d.head_y - 1.6, -8.8), (0.8, 1.8, 0.8), fang)
    ruff = kit.fur([kit.darken(c, 0.25) for c in skin], salt=63, flow="y", strand=1.8, root=0.2)
    rig.box("head", (-d.tw / 2 - 0.4, d.sh_y - 1.2, -d.td / 2 - 0.9), (d.tw + 0.8, 3.0, d.td + 1.8), ruff, faces=("front", "back", "left", "right", "bottom"))
    claw = kit.bone_paint(kit.shades("#ece6d6", 5, 1.0), 64)
    for sg, bone in ((1, "hand_l"), (-1, "hand_r")):
        for k in (-1, 0, 1):
            rig.spike(bone, (sg * d.arm_x + k * 1.0, d.wrist_y - d.hand + 0.4, -0.6), 0.8, 3.6, 180.0, 0.0, claw)


def post(kit, rig):
    """`sleep`: curled up, for a predator that has eaten (the renderer plays it while the entity says it is asleep)."""
    lib.sleep_pose(rig)
