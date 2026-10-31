"""Escanor, Pride in the Sun (second form, the day), HD 256x256 (hand-painted; the painter is chars/_escanor.py). See docs/arc/hd_arc_sds2.md."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _escanor as E  # noqa: E402


def paint(spec, cid, masks):
    return E.paint_form(spec, cid, masks, E.F2)
