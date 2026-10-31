"""Dante Zogratis, the hybrid of the Gates (100% Lucifero), HD 256x256: the painter of `dante_zogratis` with violet skin, the mauve tunic with lilac trim, slit glowing eyes, fangs and glowing cracks
(docs/arc/hd_arc_bc5.md). The horns and the broken halo are the `pmx_dante_zogratis_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

M = K.sibling("dante_zogratis")
M.FORM2 = True
M.HAIR = "#241a34"
M.HAIR_RGB = (36, 26, 52)
M.PATCH = "#5a3a52"

paint = M.paint
