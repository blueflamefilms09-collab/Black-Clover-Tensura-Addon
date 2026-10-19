"""Why: TIAMAT, the five-headed queen of evil dragons: one body, five necks fanning from the shoulders, a head of each chromatic colour (red in the middle and highest, blue and green either side,
black and white on the flanks), a great pair of leathery wings and a barbed tail. Phase 2 every mouth burns with its own breath, phase 3 the wings grow, the back veins glow and a ring of the five
colours circles behind the necks. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, bone, TAU, keys, smooth
import beasts as B
import dragons as D

LOOK = dict(id="tiamat", name="Tiamat", kind="geo", height=3.5, glow="#ff6a3a", boost=0.9, shadow=1.6, walk_cycle=2.1, run_cycle=3.6, dust=False,
            captions=["", "All five mouths of Tiamat fill with their own breath.", "Tiamat's wings spread to their full span and the five colours circle her necks."])

HEADS = [
    dict(name="red", scale="#a82a20", horn="#2a1a18", eye="#ff7a3a", mouth="#c04020"),
    dict(name="blue", scale="#2a5ac0", horn="#1a2230", eye="#6ab8ff", mouth="#3a70d0"),
    dict(name="green", scale="#3a8a3a", horn="#1a2a1a", eye="#9aff6a", mouth="#4a9a3a"),
    dict(name="black", scale="#26262e", horn="#0e0e12", eye="#c070ff", mouth="#4a3a5a"),
    dict(name="white", scale="#d6e4f0", horn="#8aa0b8", eye="#e0fcff", mouth="#9ac0d8"),
]


def build(phase):
    c = Creature("tiamat", phase, tex_w=512)
    pal = dict(scale="#4a2438", belly="#9a7a5a", horn="#1a1218", eye="#ff7a3a", wingbone="#2a1418")
    order = [(0, 0.0, 1.45, 0.0), (1, 38.0, 1.15, 6.5), (2, -38.0, 1.15, -6.5), (3, 70.0, 0.8, 10.0), (4, -70.0, 0.8, -10.0)]
    heads = []
    for idx, ang, rise, x in order:
        h = HEADS[idx]
        heads.append(dict(pal=dict(scale=h["scale"], neck=h["scale"], horn=h["horn"], eye=h["eye"], mouth=h["mouth"], tooth="#ece4d0"), angle=ang, rise=rise, x=x, s=1.0 if idx == 0 else 0.92,
                          len=[8, 8, 8, 6] if idx == 0 else [7, 7, 7, 5.5], w=[5.8, 5.2, 4.6, 4.0], crest=phase >= 2, glow_mouth=phase >= 2))
    span = (50, 54, 62)[phase - 1]
    spec = dict(pal=pal, W=20, H=15, heads=heads, wings=dict(type="bat", span=span, membrane="#6a2a3a", vein="#2a0e18", lift=24, sweep=26), tail_barb=True)
    info = D.dragon(c, spec)
    if phase >= 3:
        for i in range(5):                                             # veins of light down the back
            c.box("body", (-1.0 + (i - 2) * 3.2, 18 + 15 + 0.1, -13 + i * 6.5), (2.0, 0.5, 5.5), emissive("#ff7a3a", "#ff7a3a"), k=1)
        c.bone("x_colors", "root", pivot=(0, 40, 4))
        for i, h in enumerate(HEADS):
            a = TAU * i / 5
            nm = "x_color%d" % i
            c.bone(nm, "x_colors", pivot=(math.cos(a) * 17, 40 + math.sin(a) * 9, 6))
            c.box(nm, (math.cos(a) * 17 - 1.5, 40 + math.sin(a) * 9 - 1.5, 5.0), (3.0, 3.0, 3.0), emissive(h["eye"], h["eye"], soft=0.1), k=1)
            c.box(nm, (math.cos(a) * 17 - 4.0, 40 + math.sin(a) * 9 - 4.0, 3.5), (8.0, 8.0, 6.0), halo(h["eye"], 0.8), k=1)
    R = RIG
    D.rig(R, info)
    R.sway("x_colors", 2, 1.0, 1, 0, kind="spin")
    return c


def _build_rig():
    c = Creature("tiamat", 1)
    return None


def _extra(clip, t, L):
    return _EX(clip, t, L)


# the motion needs the bone names, which do not depend on the phase: build once to read them
_probe = Creature("tiamat", 1, tex_w=512)
_info = D.dragon(_probe, dict(pal=dict(scale="#000", belly="#000", horn="#000", eye="#000"), W=20, H=15,
                              heads=[dict(pal=dict(scale="#000", horn="#000", eye="#000"), angle=a, rise=1.0, x=x, len=[5, 5, 5, 4.5]) for a, x in ((0, 0), (38, 6.5), (-38, -6.5), (70, 10), (-70, -10))],
                              wings=dict(type="bat", span=50, membrane="#000", lift=24, sweep=26)))
_EX = D.extra_motion(_info)
RIG = AnimRig(B.quad_motion(("leg_fl", "leg_fr", "leg_bl", "leg_br"), neck=["neck_%d_0" % h for h in range(5)], wings=_info["wings"], stride=22, bob=0.6, rear=12, extra=_extra,
                            heads=["neck_%d_2" % h for h in range(5)]))
