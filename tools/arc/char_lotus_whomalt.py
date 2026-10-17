"""Why: Lotus Whomalt, the Diamond Kingdom general of the Dungeon Exploration Arc who would rather nap and fights with Smoke Magic: the model has to read as a tall, slouching,
tired man wrapped in smoke, not as a warrior. Look (facts only, from the anibase profile as summarised by search; see docs/arc/lotus_whomalt.md): about 36, 184 cm, tall and slim with a
relaxed, slouched posture, a middle-aged man with a casual bearing, a large scar on the chest (from his later fight with Yami), often wrapped in smoke. The mod's own additions: a long
general's coat of smoky violet-grey with the diamond of his kingdom on the back, a pipe whose ember is the only warm light on him, an open grimoire in his left hand (Magic Knights fight
with one; its pages glow the grey-violet of his smoke), tired eyes with bags, stubble, and thin wisps of smoke that rise from his shoulders and the pipe and sway in every clip.
Unverified: hair, clothes, colours; the chest scar is not drawn (the coat hides it).
"""
SPEC = {
    "id": "lotus_whomalt",
    "name": "Lotus Whomalt",
    "role": "boss",
    "build": "slim",
    "height": 2.05,
    "skin": "fair",
    "face": {"expression": "tired", "eyes": "tired", "eye_color": "#6a6478", "brows": "flat", "mouth": "line", "marks": ["stubble", "eye_bags"]},
    "hair": {"style": "messy", "color": "#4a443f", "streak": "#8a848a"},
    "outfit": {
        "top": {"kind": "coat", "color": "#3a3446", "trim": "#a8a4b8", "length": "calf", "collar": "high", "emblem": {"kind": "diamond", "where": "back", "size": 8, "color": "#c9c0dc"}},
        "inner": {"kind": "shirt", "color": "#bdb8c8"},
        "sleeves": {"kind": "long", "cuff": "#a8a4b8"},
        "legs": {"kind": "trousers", "color": "#26222e"},
        "boots": {"kind": "boots", "color": "#2a2428", "height": "calf"},
        "belt": {"kind": "plain", "color": "#3c3040", "buckle": "#a8a4b8"},
        "neck": {"kind": "cravat", "color": "#8a7fa8"},
    },
    "accessories": [{"kind": "pipe", "color": "#4a3828", "glow": "#ff8a30"}],
    "props": {"left": {"kind": "grimoire", "color": "#4a4258", "color2": "#a8a4b8", "glow": "#a690e8"}},
    "glow": {"color": "#c9c0dc", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "book", "idle": "slouch"},
    "notes": "Smoke Magic boss of the Dungeon Exploration Arc. Only the pipe ember, the grimoire pages and the smoke wisps glow.",
}


def extra(kit, rig):
    """Smoke on his own bones, swaying and breathing in every clip: one plume leaning away from each shoulder, a thin thread rising from the pipe, and a low wisp dragging
    behind the coat hem (so he reads as wrapped in smoke from every side, not as wearing earmuffs)."""
    d = rig.d
    smoke = kit.metal(kit.shades("#b8b0cc", 5, 1.1), 51, glow=(184, 176, 204, 70), emboss=False)
    smoke2 = kit.metal(kit.shades("#7d7498", 5, 1.1), 52, glow=(125, 116, 152, 50), emboss=False)
    for n, (side, ph) in enumerate(((1, 0.0), (-1, 1.9))):
        name = "smoke_%d" % n
        x0 = side * (d.tw / 2 + 0.6)
        y0 = d.sh_y - 1.2
        rig.bone(name, "body", (x0, y0, 0.0))
        for k in range(5):
            s = 2.8 - 0.4 * k
            x = x0 + side * 0.9 * k - s / 2
            z = -s / 2 + (0.7 if k % 2 else -0.5)
            rig.box(name, (x, y0 + k * 2.1, z), (s, 2.0, s), smoke if k % 2 == 0 else smoke2)
        rig.animate(name, sway=1.6, pulse=0.12, phase=ph, wind=0.6)
    my = d.head_y + 8.0 - (26.5 / 32.0) * 8.0
    rig.bone("smoke_pipe", "head", (0.9, my + 2.0, -8.2))
    for k in range(4):
        s = 1.3 - 0.2 * k
        rig.box("smoke_pipe", (0.9 - s / 2 - 0.35 * k, my + 2.3 + k * 1.7, -8.2 - s / 2 - 0.25 * k), (s, 1.5, s), smoke if k % 2 == 0 else smoke2)
    rig.animate("smoke_pipe", sway=2.2, pulse=0.15, phase=2.6, wind=0.8)
    rig.bone("smoke_hem", "body", (0.0, d.hip_y + 1.0, d.td / 2 + 0.4))
    for k in range(4):
        s = 3.0 - 0.5 * k
        rig.box("smoke_hem", (-s / 2 + (1.6 if k % 2 else -1.4), d.hip_y + 1.0 - k * 2.3 - 1.8, d.td / 2 + 0.2 + k * 0.8), (s, 2.1, s * 0.6), smoke2 if k % 2 == 0 else smoke)
    rig.animate("smoke_hem", sway=2.0, pulse=0.1, phase=3.7, wind=1.0)
