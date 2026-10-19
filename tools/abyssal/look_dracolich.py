"""Why: the DRACOLICH of the frozen crypt: a dragon that is nothing but bone, a spine and a cage of ribs with a soul fire burning inside, a long skull with cyan fire in its sockets, two wings of bare
finger-bones hung with torn, dark teal membrane, a bone tail, and a phylactery gem (phase 3) floating over its back. Phase 2 the fire in the ribs swells and the skull ignites. Full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, emissive, halo, wisp, bone, TAU, keys, smooth
import beasts as B
import dragons as D

LOOK = dict(id="dracolich", name="Dracolich", kind="geo", height=3.2, glow="#7affd8", boost=0.9, shadow=1.2, walk_cycle=2.1, run_cycle=3.6, dust=True,
            captions=["", "Soul fire swells in the Dracolich's ribs and its skull ignites.", "The phylactery rises over its back, and the dead dragon burns white."])


def spec_for(phase):
    pal = dict(scale="#d6d0b4", belly="#b8b094", horn="#bcb698", eye="#7affd8", bone="#d6d0b4", wingbone="#cfc9ae", tooth="#e8e4cc")
    head = dict(pal=dict(scale="#d6d0b4", neck="#d6d0b4", horn="#bcb698", eye="#7affd8", mouth="#102820", bone="#d6d0b4", tooth="#e8e4cc"), rise=1.2, len=[6, 6, 6, 5], w=[4.6, 4.2, 3.8, 3.4],
                crest=True, s=1.05, glow_mouth=phase >= 2)
    return dict(pal=pal, W=16, H=13, skeletal=True, heads=[head], wings=dict(type="bat", span=(48, 52, 56)[phase - 1], membrane="#1c4a48", vein="#0a2220", ragged=True, lift=22, sweep=26),
                tail=[8, 8, 8, 7, 7, 6, 6, 5, 5, 4])


def build(phase):
    c = Creature("dracolich", phase, tex_w=512)
    info = D.dragon(c, spec_for(phase))
    if phase >= 3:
        c.bone("x_soul", "body", pivot=(0, 50, 0))
        c.box("x_soul", (-2.2, 46, -2.2), (4.4, 4.4, 4.4), emissive("#e8fff8", "#7affd8", soft=0.1), k=1)
        c.box("x_soul", (-8, 40, -8), (16, 16, 16), halo("#7affd8", 0.9), k=1)
    c.note = None
    R = RIG
    D.rig(R, info, wing_amp=5, neck_amp=4)
    R.sway("x_soul", 1, 1.5, 1, 0, kind="pos")
    R.sway("x_soul", 1, 1.0, 1, 0, kind="spin")
    return c


def _extra(clip, t, L):
    return _EX(clip, t, L)


_probe = Creature("dracolich", 1, tex_w=512)
_info = D.dragon(_probe, spec_for(1))
_EX = D.extra_motion(_info)
RIG = AnimRig(B.quad_motion(("leg_fl", "leg_fr", "leg_bl", "leg_br"), neck=["neck_0_0"], wings=_info["wings"], stride=20, bob=0.5, rear=12, extra=_extra, heads=["neck_0_2"], lunge=18))
