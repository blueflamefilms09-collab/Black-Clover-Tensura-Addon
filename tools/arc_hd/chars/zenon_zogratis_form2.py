"""Zenon Zogratis, the hybrid of the Gates (100% Beelzebub), HD 256x256: the painter of `zenon_zogratis` with grey skin, hollow indigo-burning eyes, fangs, a ragged vest over a bone ribcage and spine,
bone pauldrons and bracers with glowing trim, and the bone-pale crest of the mohawk (docs/arc/hd_arc_bc5.md). The horns and the insect wings are the `pmx_zenon_zogratis_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

M = K.sibling("zenon_zogratis")
M.FORM2 = True
M.HAIR_STYLE = "mohawk"                                       # KEPT classic (hair_map): the sheet's mohawk, the bone crest
M.HAIR = "#d8d0b4"
M.HAIR_RGB = (216, 208, 180)
M.TANK = "#2a2230"

paint = M.paint
