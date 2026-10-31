"""Vanica Zogratis, the hybrid of the Gates (100% Megicula), HD 256x256: the painter of `vanica_zogratis` with red skin, a dark dress with coral trim, a slit glowing eye, fangs and glowing marks
(docs/arc/hd_arc_bc5.md). The horns, the thorn halo, the blood orb and the claws are the `pmx_vanica_zogratis_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

M = K.sibling("vanica_zogratis")
M.FORM2 = True

paint = M.paint
