"""Why: the Charybdis Spawn (entity nusmp:charybdis_spawn, dungeon/floor/charybdis/CharybdisSpawnBoss), the Deep Waterways' floor boss: an ancient squid-whale leviathan, about five blocks of body
and seven with its arms, teal and deep-blue skin in rows of cyan scales, a scarred blind eye and a living one, a dark beak ringed with eight arms and two long feeding tentacles, great
ragged side fins, a whale's fluke, barnacles and two broken harpoons in its back. The hand-built model (tools/gen_charybdis.py, kept) is replaced by the art kit's version, a creature
as an anthropomorphic character on the player's rig: a huge teal-scaled humanoid titan that floats, with one blind scarred eye and one cyan eye, a beak and a ring of eight arms where a
mouth would be (made in `extra`), two long feeding tentacles from the shoulders (segmented, they sway in every clip), fin-membrane wings for the ragged side fins, a heavy tail that ends in
a whale's fluke, barnacles and two broken harpoons stuck in its back. `form2` is the flushed body of the Gates: white-hot scales, a violet back, rose veins. See docs/arc/remodel.md.
"""
import math

ENTITIES = ["charybdis_spawn"]

TEAL = {"skin": ["#164a5a", "#1e6a7c", "#2e8a98", "#4ab0b4"], "scale": "#2a8a9a", "trim": "#40f0e0", "glow": "#40f0e0"}


def spec():
    t = TEAL
    return {
        "id": "charybdis_spawn",
        "name": "Charybdis Spawn",
        "role": "boss",
        "build": "huge",
        "height": 3.2,
        "widen": 1.15,
        "skin": t["skin"],
        "face": {"expression": "demon", "eyes": "slit", "eye_color": "#60f4ff", "eye_glow": "#80f8ff", "brows": "angry", "mouth": "none", "nose": "none",
                 "marks": ["scar_eye_l", "scar_brow_l", "scar_cheek_l"]},
        "hair": {"style": "bald", "color": "#164a5a"},
        "outfit": {
            "top": {"kind": "bare"},
            "legs": {"kind": "shorts", "color": "#1e5a6a"},
            "boots": {"kind": "bare"},
            "gloves": {"kind": "none"},
            "belt": {"kind": "plain", "color": "#123848", "buckle": "#40f0e0"},
        },
        "armor": {"pieces": ["breastplate", "pauldrons", "bracers", "greaves"], "style": "scale", "color": t["scale"], "trim": t["trim"], "glow": t["glow"]},
        "ears": {"kind": "fin", "color": "#2e8a98", "inner": "#40f0e0", "size": 2.0},
        "wings": {"kind": "dragon", "color": "#1e6a7c", "color2": "#40c8d0", "glow": "#40f0e0", "span": 1.15},
        "tail": {"kind": "dragon", "color": "#1e6a7c", "color2": "#40f0e0", "glow": "#40f0e0", "length": 1.25},
        "accessories": [{"kind": "eyepatch", "color": "#c8d8d8", "side": "left"}],
        "glow": {"color": "#40f0e0", "eyes": True, "props": True, "trim": True},
        "clips": {"attack": "punch", "cast": "magic", "idle": "hover", "extra": ["spawn"]},
        "form2": {
            "name": "Charybdis Spawn, Flushed",
            "skin": ["#7a3a5a", "#a05a7a", "#c88aa0", "#f0c0c8"],
            "face": {"eye_color": "#ffe0f0", "eye_glow": "#ffe0f0", "marks": ["scar_eye_l", "scar_brow_l", "veins", "cracks"]},
            "armor": {"color": "#f0e8f0", "trim": "#ff8ab0", "glow": "#ffc0e0"},
            "wings": {"color": "#7a3a8a", "color2": "#f0b0e0", "glow": "#ffc0e0"},
            "tail": {"color": "#a05a8a", "color2": "#ffc0e0", "glow": "#ffc0e0"},
            "ears": {"color": "#a05a8a", "inner": "#ffc0e0"},
            "glow": {"color": "#ffc0e0", "eyes": True, "props": True, "trim": True},
        },
    }


SPEC = spec()


def extra(kit, rig):
    """The leviathan's own parts, placed from the measured body so they fit both forms: the beak and the ring of eight arms where a mouth would be, two long segmented feeding tentacles
    from the shoulders, the whale's fluke on the end of the tail, barnacles on the back and two broken harpoons stuck in it. Colours follow the form's skin."""
    d = rig.d
    S = rig.spec
    skin = rig.look.skin
    pale = [kit.mix(c, (225, 245, 245), 0.35) for c in skin]
    glow = tuple(int(S["glow"]["color"][i:i + 2], 16) for i in (1, 3, 5))
    sucker = kit.metal(kit.shades(S["armor"]["trim"], 5, 1.1), 71, glow=glow + (200,), emboss=False)
    beak = kit.bone_paint(kit.shades("#2a2630", 5, 1.0), 72)
    # the beak: a dark hooked wedge, and eight short arms round it that wave
    rig.box("head", (-2.0, d.head_y - 1.0, -8.6), (4.0, 3.0, 4.6), beak, faces=("front", "left", "right", "top", "bottom"))
    rig.box("head", (-1.0, d.head_y - 2.8, -8.4), (2.0, 2.0, 2.4), beak, faces=("front", "left", "right", "bottom"))
    rig.bone("beak_arms", "head", (0.0, d.head_y, -5.0))
    arm = kit.skin_paint(skin, salt=73)
    for k in range(8):
        a = k * math.pi / 4.0
        x, y = 3.6 * math.cos(a), 1.2 + 3.0 * math.sin(a)
        rig.box("beak_arms", (x - 0.7, d.head_y + y - 4.5, -7.2), (1.4, 5.0, 1.4), arm, rotation=(18.0, 0, 0), pivot=(x, d.head_y + y, -7.2), faces=("front", "back", "left", "right", "bottom"))
        rig.box("beak_arms", (x - 0.45, d.head_y + y - 5.0, -7.5), (0.9, 0.9, 0.9), sucker)
    rig.animate("beak_arms", sway=1.0, phase=0.7)
    # two long feeding tentacles from the shoulders: four segments each, every one swinging from the one before
    for sg, nm in ((1, "l"), (-1, "r")):
        prev, px_, py_ = "body", sg * (d.tw / 2 + 1.0), d.sh_y - 3.0
        for k in range(4):
            name = "tentacle_%s%d" % (nm, k)
            rig.bone(name, prev, (px_, py_, d.td / 2 - 1.0))
            w = 5.0 - 0.8 * k
            seg = 13.0
            rig.box(name, (px_ - w / 2, py_ - seg, d.td / 2 - 1.0 - w / 2), (w, seg + 0.4, w), kit.skin_paint(pale if k % 2 else skin, salt=74 + k), faces=("front", "back", "left", "right", "bottom"))
            if k == 3:
                rig.box(name, (px_ - 1.6, py_ - seg - 1.0, d.td / 2 - 2.6), (3.2, 1.4, 3.2), sucker)
            rig.animate(name, sway=1.1, phase=0.5 * k + (0.0 if sg > 0 else 1.1))
            prev, py_ = name, py_ - seg
    # the whale's fluke: the tail's last segment, found with the kit's own recurrence for a dragon tail (accessories.build_tail), gets two wide flat blades
    n, seg, beta, turns = 5, round(d.top * 0.105 * S["tail"]["length"], 1), 16.0, (4, 6, 10, 12, 14)
    py, pz = d.hip_y + 0.5, d.td / 2 - 0.2
    for k in range(n):
        beta = max(-20.0, min(80.0, beta + turns[k]))
        dv = kit.rot_file((0, -seg, 0), beta - 90.0, 0, 0)
        py, pz = py + dv[1], pz + dv[2]
    fl = kit.fabric(kit.shades(S["wings"]["color"], 5, 1.0), salt=75, period=6.0, amp=0.14, seam=False)
    for sg in (1, -1):
        rig.box("tail5", (min(0.0, sg * 13.0), py - 0.6, pz - 3.5), (13.0, 1.2, 7.0), fl, rotation=(0, -sg * 14.0, 0), pivot=(0, py, pz), faces=("top", "bottom", "front", "back", "left", "right"))
    # barnacles on the back and two broken harpoons
    gray = kit.bone_paint(kit.shades("#b8c4c0", 5, 1.0), 76)
    for k, (bx, by) in enumerate(((-3.0, 4.0), (2.5, 7.0), (-1.0, 10.0), (3.5, 2.0), (-4.0, 8.0))):
        rig.box("body", (bx - 1.0, d.hip_y + by, d.td / 2 + 0.1), (2.0 + (k % 2), 1.6, 1.2 + 0.4 * (k % 3)), gray)
    iron = kit.metal(kit.shades("#4a4a52", 5, 1.0), 77, emboss=False)
    rope = kit.fabric(kit.shades("#8a7a5a", 5, 0.8), salt=78, seam=False)
    for sx, rz in ((-3.0, 14.0), (3.5, -18.0)):
        rig.box("body", (sx - 0.5, d.hip_y + 5.0, d.td / 2 + 0.3), (1.0, 12.0, 1.0), iron, rotation=(-26.0, 0, rz), pivot=(sx, d.hip_y + 5.0, d.td / 2 + 0.3))
        rig.box("body", (sx - 0.7, d.hip_y + 11.0, d.td / 2 + 3.0), (1.4, 5.0, 1.4), rope, rotation=(-26.0, 0, rz), pivot=(sx, d.hip_y + 5.0, d.td / 2 + 0.3))


HEAD_SCALE = 1.5


def post(kit, rig):
    """The beak and its ring of arms are built on an 8 px head: the head bone is drawn HEAD_SCALE times as big in every clip so the squid's face reads from across the arena."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
