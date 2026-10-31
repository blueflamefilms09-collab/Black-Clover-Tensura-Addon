"""Meliodas, the Demon King (second form), HD 256x256: the painter of `meliodas` with the dark palette, the slit red eyes, the sigil, the cracks and the veins (docs/arc/hd_arc_sds1.md)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _sds1_kit as K  # noqa: E402

M = K.sibling("meliodas")
M.FORM2 = True
M.VEST, M.YOKE, M.PIPE = "#18141a", "#7a1432", "#d8486a"
M.TROUS = "#18141a"
M.BOOT = "#241c1e"
M.BELT_A, M.BELT_B = "#3a1a24", "#1c1015"
M.GOLD = "#7a1432"
M.INK, M.INK_L = "#12060c", "#ff3a5a"

paint = M.paint
