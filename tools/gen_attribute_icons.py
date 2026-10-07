"""0.53: the three icons (+ the symbol layer) of each NEW attribute of the Black Clover Magic and VFX expansion.

    python tools/gen_attribute_icons.py              # every new attribute
    python tools/gen_attribute_icons.py bronze gel   # only these

Reuses tools/gen_skill_icons.py (frame, backgrounds, symbol rendering, reduce) so the look matches the 0.23 set exactly; only the
glyph and the colour entry of each attribute come from its own module, tools/icons/g_<key>.py:

    ENTRY = (display name, glow RGB, body RGB, prompt colour words, signature spell)
    def glyph(d): ...      # d = ImageDraw on a 256 px 'L' canvas; draw with G.P(points) (glyph units: origin centre, y down, +-12);
                           # 255 = body, ~150 = facet.  Import helpers with:  import gen_skill_icons as G

It never touches the 0.23 icons of the existing magics (no clean-up of the folder, no sheet): it only writes
textures/skill/grimoire/<key>.png, icons/<key>_buff.png, icons/<key>_ultimate.png and icons/sym_<key>.png of the keys it is given.
Deterministic: each key has a fixed seed (its index in attribute_table.NEW).
"""
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import attribute_table as T          # noqa: E402
import gen_skill_icons as G          # noqa: E402

MODS = os.path.join(HERE, "icons")


def load(key):
    path = os.path.join(MODS, "g_" + key + ".py")
    spec = importlib.util.spec_from_file_location("g_" + key, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main():
    keys = [k.lower() for k in sys.argv[1:]] or [a["key"].lower() for a in T.NEW]
    order = [a["key"].lower() for a in T.NEW]
    os.makedirs(os.path.join(G.TEX, "grimoire"), exist_ok=True)
    os.makedirs(G.OUT, exist_ok=True)
    for key in keys:
        mod = load(key)
        G.MAGIC[key] = tuple(mod.ENTRY)
        G.GLYPHS[key] = mod.glyph
        i = 60 + order.index(key)                                     # seeds above the 0.23 set's (index*7+1..3 for 0..59)
        act, buf, ult = (G.reduce(G.variant_active(key, i * 7 + 1)), G.reduce(G.variant_buff(key, i * 7 + 2)),
                         G.reduce(G.variant_ultimate(key, i * 7 + 3)))
        G.save(act, os.path.join(G.TEX, "grimoire", key + ".png"))
        G.save(buf, os.path.join(G.OUT, key + "_buff.png"))
        G.save(ult, os.path.join(G.OUT, key + "_ultimate.png"))
        G.save(G.reduce(G.symbol_layer(key)), os.path.join(G.OUT, "sym_" + key + ".png"))
        print("icons:", key)


if __name__ == "__main__":
    main()
