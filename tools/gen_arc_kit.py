"""Why: the front door of the art kit of the characters of the arcs. A character is ONE python file tools/arc/char_<id>.py that exposes a dict SPEC (see
docs/arc/art_kit.md for every field) and optionally `extra(kit, rig)` for a signature part; tools/gen_arc_characters.py turns every such file into a GeoLite model
(assets/nusmp/geo|animations|textures/entity/arc_<id>*). This module re-exports what a character file or an `extra` may need: the builder, the painters, the
colour helpers, the vocabularies (names of hair styles, eyes, outfits ...) and the registry of props.

    import gen_arc_kit as kit
    R = kit.build_character(SPEC, extra)         # a finished Rig (nothing written yet)
    kit.save(R)                                  # geo, animation, texture, glow texture under src/main/resources/assets/nusmp

Inside `extra(kit, rig)`: `rig` is the Rig under construction (rig.box / rig.pair / rig.spike / rig.clump / rig.bone, rig.d = the measured body, rig.spec = the
normalised spec, rig.look = the palettes); add bones and cubes with the painters exported here. Clips are generated after `extra`, so a bone you add can be given motion
with `rig.animate(bone, sway=..., spin=..., pulse=...)` (see the manual) or simply left still; a function `post(kit, rig)` in the character file may touch up the finished
clips afterwards (rig.A.clips).
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

from arc_kit import color, core, face, hair, outfit, props, spec, accessories, recipes, scenes  # noqa: E402,F401
from arc_kit.build import build_character, save, Look, skeleton  # noqa: E402,F401
from arc_kit.color import rgb, shades, mix, lighten, darken, SKIN_TONES, NAMED, skin_palette, cloth_palette  # noqa: E402,F401
from arc_kit.core import (Canvas, Pack, canvas_paint, canvas_plate, decal_paint, over, band, band_px, only, fabric, leather, plate, chain_paint, fur, bone_paint,  # noqa: E402,F401
                          bandage_paint, scale_paint, metal, solid, hair_paint, skin_paint, feather, clump, blade_paint, pal, px)
from arc_kit.outfit import mat, emblem, Surf, rows_top, rows_bottom, rows_frac, rows_range, everywhere  # noqa: E402,F401
from arc_kit.props import PROPS, PropDef, PB  # noqa: E402,F401
from arc_kit.rig import Dims, Rig, BUILDS, rot_file  # noqa: E402,F401
from arc_kit.spec import normalize, SpecError  # noqa: E402,F401

HAIR_STYLES = hair.HAIR_STYLES
EYE_STYLES = face.EYE_STYLES
BROW_STYLES = face.BROW_STYLES
MOUTH_STYLES = face.MOUTH_STYLES
FACE_MARKS = face.MARKS
EXPRESSIONS = tuple(face.EXPRESSIONS)
TOP_KINDS = outfit.TOP_KINDS
LEG_KINDS = outfit.LEG_KINDS
BOOT_KINDS = outfit.BOOT_KINDS
EMBLEMS = outfit.EMBLEMS
PROP_KINDS = props.PROP_KINDS
ACCESSORY_KINDS = spec.ACCESSORY_KINDS
BUILD_NAMES = tuple(BUILDS)
CLIP_NAMES = scenes.CLIPS
OPTIONAL_CLIPS = scenes.OPTIONAL


def register_prop(name, definition):
    """Adds a prop kind (a PropDef) so a spec can use it; see docs/arc/art_kit.md "How to add a prop"."""
    props.PROPS[name] = definition
    props.PROP_KINDS = tuple(props.PROPS)
    global PROP_KINDS
    PROP_KINDS = props.PROP_KINDS
