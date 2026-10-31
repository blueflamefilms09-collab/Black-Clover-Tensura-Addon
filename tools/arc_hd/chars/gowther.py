"""Gowther, the Goat's Sin of Lust, HD 256x256 (hand-painted; the painter is chars/_gowther.py, the second form is chars/gowther_form2.py). See docs/arc/hd_arc_sds2.md."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _gowther as G  # noqa: E402


def paint(spec, cid, masks):
    return G.paint_form(spec, cid, masks, G.F1)
