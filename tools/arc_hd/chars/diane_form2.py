"""Diane in Drole's Dance (second form), HD 256x256: the same painter as `diane` with the golden palette and the crystal plates (docs/arc/hd_arc_sds1.md)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

paint = K.sibling("diane").paint
