"""Fana, the Evil Heart of her last stand, HD 256x256: the painter of `fana` with a brighter red dress with burning orange trim and cracks, burning orange eyes with slit pupils, a mad grin with fangs,
a gold third eye, a hot pink stone and glowing veins; the hair is the wild mane (`anime_mane`, hair_map) (docs/arc/hd_arc_bc5.md). The crown of flames, the grimoire and the spark of Salamander are the
`pmx_fana_form2` extras."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

M = K.sibling("fana")
M.FORM2 = True
M.HAIR_STYLE = "anime_spike_swept"                            # the hair that lifts: a swept-up crest that leaves the forehead (the burning third eye) free; hair_map has the closed anime_mane
M.HAIR = "#ff7a9e"
M.HAIR_RGB = (255, 122, 158)

paint = M.paint
