"""Zagred (legacy), phase 4, HD 256x256: the painter of `zagred_legacy` with the phase-4 palette (redder, white-hot cracks and claws, lit knuckles), docs/arc/hd_arc_bc7.md.
The horns, the bat wings, the whip tail and the trident are the `pmx_zagred_legacy_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

paint = K.sibling("zagred_legacy").paint
