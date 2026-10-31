"""Merlin, Infinity Unbound (second form), HD 256x256 (hand-painted; the painter is chars/_merlin.py). See docs/arc/hd_arc_sds2.md."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _merlin as M  # noqa: E402


def paint(spec, cid, masks):
    return M.paint_form(spec, cid, masks, M.F2)
