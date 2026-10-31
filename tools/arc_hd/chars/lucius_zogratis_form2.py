"""Lucius Zogratis, the divine form, HD 256x256: the painter of `lucius_zogratis` with the pale-blue and white layered robe with gold borders, the glowing golden sun medallion, white gloves, wide golden
glowing eyes, white spikes and golden light lines (docs/arc/hd_arc_bc5.md). The three pairs of wings, the antlers, the halo of gears, the cloak and the grimoire are the `pmx_lucius_zogratis_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

M = K.sibling("lucius_zogratis")
M.FORM2 = True
M.HAIR = "#e8e4f0"
M.HAIR_RGB = (232, 228, 240)

paint = M.paint
