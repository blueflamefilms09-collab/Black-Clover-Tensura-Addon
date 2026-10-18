"""Why: the Dungeon Slime (entity nusmp:dungeon_slime, dungeon/DungeonSlimeEntity), the first rung of the Living Dungeon's food web: the game's own slime body in a teal gel, in three sizes by
growth. The old renderer was the vanilla slime model with its outer layer. A creature becomes an anthropomorphic character on the player's rig with its features kept, so this is a slime
child: a small round-headed body of teal gel (light, glassy tones with a pale core), a big smiling face with wide dark eyes and a blush, a dome of gel for hair with a drip, short stubby
limbs and a little gel tail. The renderer draws it translucent (client/remodel's look says so), the way the slime's outer layer was, and scales it by its hitbox (the entity grows through
three sizes). The colours are the old texture's (tools/gen_dungeon_assets.py). See docs/arc/remodel.md.
"""
ENTITIES = ["dungeon_slime"]

SPEC = {
    "id": "dungeon_slime",
    "name": "Dungeon Slime",
    "role": "boss",
    "build": "child",
    "height": 1.4,
    "widen": 1.25,
    "skin": ["#1a8a82", "#2eb8a8", "#5ae0cc", "#b4fff0"],
    "face": {"expression": "cheerful", "eyes": "round", "eye_color": "#0e2c30", "sclera": "#e8fffa", "brows": "none", "mouth": "smile", "nose": "none", "marks": ["blush", "light_lines"]},
    "hair": {"style": "bun", "color": "#4ad8c4", "streak": "#b4fff0"},
    "outfit": {
        "top": {"kind": "tank", "color": "#38c8b4", "trim": "#b4fff0", "length": "hip", "hem_trim": True, "material": "cloth"},
        "legs": {"kind": "shorts", "color": "#2eb8a8"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
    },
    "tail": {"kind": "cat", "color": "#38c8b4", "color2": "#b4fff0", "length": 0.7},
    "glow": {"color": "#8affe8", "eyes": False, "props": False, "trim": False},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed"},
}

HEAD_SCALE = 1.3


def post(kit, rig):
    """A slime is mostly head: the head bone is drawn HEAD_SCALE times as big in every clip."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
