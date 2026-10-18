"""Why: the Painted Constructs (entity nusmp:painted_construct, Painting Magic's living illustrations) come in three kinds (a hound, an einherjar knight, a giant) and seven paints (ink, fire, water,
ice, wind, earth, lightning), and the old renderer drew the paint as a texture per paint. Here the body is painted ONCE in a pale, almost white brush-stroke paint and the renderer
tints it with the paint's colour (the tint multiplies the pale texture, which keeps its brush strokes), so three characters serve the seven paints instead of twenty-one. Each kind is its own
character file (the kit builds one body per file); this module holds what they share, and only returns spec dicts. See docs/arc/remodel.md.
"""

PAPER = ["#b4b8c8", "#d0d4e0", "#e8eaf2", "#ffffff"]          # the wet white paint: shadows are a cool grey, the lit side is white
STROKE = "#c8ccdc"


def base(cid, name, build, height, **over):
    out = {
        "id": cid,
        "name": name,
        "role": "boss",
        "build": build,
        "height": height,
        "skin": PAPER,
        "face": {"expression": "calm", "eyes": "glow", "eye_color": "#ffffff", "eye_glow": "#ffffff", "brows": "none", "mouth": "none", "nose": "none", "marks": ["war_paint"]},
        "hair": {"style": "bald", "color": "#e8eaf2"},
        "outfit": {"top": {"kind": "bare"}, "legs": {"kind": "shorts", "color": STROKE}, "boots": {"kind": "bare"}, "gloves": {"kind": "none"}},
        "glow": {"color": "#ffffff", "eyes": True, "props": True, "trim": True},
        "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed", "extra": ["spawn"]},
    }
    out.update(over)
    return out
