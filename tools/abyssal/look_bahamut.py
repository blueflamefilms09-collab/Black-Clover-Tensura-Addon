"""Why: BAHAMUT, the Platinum Dragon, the one good dragon of the set: a sleek silver-white body, a single long-necked head with swept horns, a crest and two pairs of long whiskers, wide feathered
wings edged in blue light and a finned tail. Phase 2 a halo of platinum light stands behind the head and the eyes blaze, phase 3 the wings throw a corona of rays. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, TAU, keys, smooth
import beasts as B
import dragons as D

LOOK = dict(id="bahamut", name="Bahamut", kind="geo", height=3.2, glow="#6ad8ff", boost=0.9, shadow=1.3, walk_cycle=2.1, run_cycle=3.6, dust=False,
            captions=["", "Bahamut's eyes blaze and a halo of platinum light opens behind him.", "The Platinum Dragon spreads his wings: a corona of light fills the hall."])


def spec_for(phase, dummy=False):
    pal = dict(scale="#d4dce8", belly="#f0f4fa", horn="#8aa0c0", eye="#6ad8ff", wingbone="#9ab0c8", tooth="#fffbe8")
    head = dict(pal=dict(scale="#dce4f0", neck="#dce4f0", horn="#8aa0c0", eye="#6ad8ff", mouth="#d08a9a", tooth="#fffbe8"), rise=1.2, len=[8, 8, 7, 6, 5], w=[5.2, 4.8, 4.4, 4.0, 3.6], whiskers=True, crest=True,
                s=1.05, glow_mouth=phase >= 3)
    return dict(pal=pal, W=14, H=12, heads=[head], wings=dict(type="feather", span=(50, 54, 60)[phase - 1], c1="#e8eef6", c2="#c4d2e4", tip="#6ad8ff", glow="#6ad8ff", sweep=24, raise_=24), tail=[8, 8, 8, 7, 7, 6, 6, 5, 5, 4])


def build(phase):
    c = Creature("bahamut", phase, tex_w=512)
    info = D.dragon(c, spec_for(phase))
    if phase >= 2:
        c.bone("x_halo", "root", pivot=(0, 52, -16))
        n = 18
        for i in range(n):
            a = TAU * i / n
            x, y = math.cos(a) * 9, 52 + math.sin(a) * 9
            c.box("x_halo", (x - 1.3, y - 1.3, -26.0), (2.6, 2.6, 1.4), emissive("#e8fcff", "#6ad8ff", soft=0.15), k=1, rot=(0, 0, math.degrees(a)), pivot=(x, y, -25.5))
        c.box("x_halo", (-12, 40, -27.0), (24, 24, 1.0), halo("#6ad8ff", 0.55), k=1)
    if phase >= 3:
        c.bone("x_rays", "body", pivot=(0, 36, 4))
        for i in range(14):
            a = TAU * i / 14
            ln = 18 + (i % 2) * 8
            x, y = math.cos(a) * 12, 36 + math.sin(a) * 10
            c.box("x_rays", (x - 0.8, y, 6.0), (1.6, ln, 0.6), wisp("#bff0ff", 1.0), k=1, rot=(0, 0, math.degrees(a) - 90), pivot=(x, y, 6.2))
    R = RIG
    D.rig(R, info, wing_amp=5, neck_amp=4)
    R.sway("x_halo", 2, 1.0, 1, 0, kind="spin")
    R.sway("x_rays", 2, 0.5, 1, 0, kind="spin")
    return c


def _extra(clip, t, L):
    return _EX(clip, t, L)


_probe = Creature("bahamut", 1, tex_w=512)
_info = D.dragon(_probe, spec_for(1))
_EX = D.extra_motion(_info)
RIG = AnimRig(B.quad_motion(("leg_fl", "leg_fr", "leg_bl", "leg_br"), neck=["neck_0_0"], wings=_info["wings"], stride=20, bob=0.7, rear=14, extra=_extra, heads=["neck_0_2"], lunge=18))
