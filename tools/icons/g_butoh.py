"""Icon module of Butoh Magic (0.53). STUB: replace glyph() with the attribute's own symbol (see tools/gen_attribute_icons.py)."""
import gen_skill_icons as G

ENTRY = ("Butoh Magic", 0xFF5A8A, 0xFFC8D8, "dance and gold", "Dance")


def glyph(d):
    d.polygon(G.P([(0, -9), (7, 0), (0, 9), (-7, 0)]), fill=255)
    d.polygon(G.P([(0, -3.5), (3, 0), (0, 3.5), (-3, 0)]), fill=150)
