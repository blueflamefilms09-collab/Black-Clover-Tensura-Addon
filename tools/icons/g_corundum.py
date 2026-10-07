"""Icon module of Corundum Magic (0.53). STUB: replace glyph() with the attribute's own symbol (see tools/gen_attribute_icons.py)."""
import gen_skill_icons as G

ENTRY = ("Corundum Magic", 0xFF6A7A, 0xFFCDD2, "corundum and silver", "Corundum")


def glyph(d):
    d.polygon(G.P([(0, -9), (7, 0), (0, 9), (-7, 0)]), fill=255)
    d.polygon(G.P([(0, -3.5), (3, 0), (0, 3.5), (-3, 0)]), fill=150)
