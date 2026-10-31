"""Vetto, the Mythical Beast (second form), HD 256x256: the painter of `vetto` with the Mythical Beast palette (white hair and beard, burning red eyes and paint, glowing beast marks), docs/arc/hd_arc_bc7.md.
The bull horns and the dragon tail are the `pmx_vetto_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

paint = K.sibling("vetto").paint
