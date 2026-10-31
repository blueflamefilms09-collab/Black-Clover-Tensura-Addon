"""HD skins for the Tier List bosses (3.38.0): the one module a character painter imports.  See docs/bosses/hd_outfits.md ("HOW TO PAINT A CHARACTER IN HD").

    import hdlib as H
    img64, glow64, hair, hair_rgb, slim = H.blueprint(cid, spec, masks)          # the 64x64 hand-painted remake of tools/tier_bosses/remake/<id>.py
    cv = H.Canvas(256, slim)                                                      # the HD canvas: every model pixel is a 4x4 block of texels
    H.from64(cv, img64, glow64, materials={...}, skip=lambda part, face, overlay: ...)   # smooth upscale + gradients + outlines + material details
    f = cv.face("body", "front")                                                   # one UV island; draw in model pixels with f.poly / f.ellipse / f.line / H.gem / H.stitch ...
    skin, glow = cv.finish()                                                       # PIL images (glow None when nothing glows)
    return skin, glow, hair, hair_rgb, slim                                        # the same tuple contract as the remake modules

`H.hd_paint(cid, blueprint64)` is the generic bridge in one call (no character detail).
"""
import importlib.util
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

from hd_core import (Canvas, Face, FACES, PARTS, blur, col, rgb8, mix, tone, ramp, lum, smoothstep, shift, union, minus, lin, rad, hsh, cov, downsample, downsample_glow, to_array)  # noqa: F401,E402
from hd_detail import (bevel, rim, shadow_under, folds, weave, sheen, scallops, stitch, fringe, stud, gem, eye, brow, blush, mouth_smile, nose, metal_paint, trim_line, trim_band, skin_face, cloth, heart_pts, sparkle)  # noqa: F401,E402
from hd_bridge import (Materials, family, near, exact, from64, hd_paint, bridge_glow, cross_layer_shadow, upscale_labels)  # noqa: F401,E402

TB = os.path.abspath(os.path.join(HERE, ".."))


def blueprint(cid, spec, masks):
    """Runs tools/tier_bosses/remake/<cid>.py (the 64x64 hand-painted version) and returns its tuple (skin64, glow64 or None, hair style, hair rgb, slim)."""
    path = os.path.join(TB, "remake", cid + ".py")
    if not os.path.isfile(path):
        raise FileNotFoundError("no 64x64 remake module for " + cid + " (" + path + ")")
    s = importlib.util.spec_from_file_location("remake_" + cid, path)
    mod = importlib.util.module_from_spec(s)
    s.loader.exec_module(mod)
    return mod.paint(spec, cid, masks)


def hd_module_path(cid):
    return os.path.join(HERE, cid + ".py")
