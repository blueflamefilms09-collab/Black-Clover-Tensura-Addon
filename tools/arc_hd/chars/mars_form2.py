"""Mars, Stones Aflame (second form), HD 256x256: the painter of `mars` with the Gates palette (warm crystal, orange stones and cracks, warm white hair), docs/arc/hd_arc_bc7.md.
The sword, the orbiting crystal, the stones on the chest and the forearm and the ring of flames are the `pmx_mars_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc7 as K  # noqa: E402

paint = K.sibling("mars").paint
