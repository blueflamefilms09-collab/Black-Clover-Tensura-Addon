"""Rhya, the Open Eye (second form), HD 256x256: the painter of `rhya` with the elf palette (white hair, gold glowing eyes, the third eye, the wider tattoo), docs/arc/hd_arc_bc7.md.
The hood, the cloak and the grimoire are the `pmx_rhya_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

paint = K.sibling("rhya").paint
