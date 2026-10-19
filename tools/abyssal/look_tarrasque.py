"""Why: the TARRASQUE, the last sleeper: a wingless, squat, enormous beast on four pillar legs, a domed carapace of overlapping plates studded with spikes down its whole back, a bull-broad head with two
huge forward-curving horns, and a thick tail ending in a spiked club. Its Reflective Carapace is the glow in the plates: dull in phase 1, shimmering in phase 2, a blazing mirror in phase 3 together with
a bigger spread of horns. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, emissive, halo, wisp, bone, flesh, horn, TAU, keys, smooth
import beasts as B
import dragons as D

LOOK = dict(id="tarrasque", name="Tarrasque", kind="geo", height=3.0, glow="#ff5a20", boost=0.85, shadow=1.7, walk_cycle=2.3, run_cycle=3.8, dust=False,
            captions=["", "The carapace of the Tarrasque begins to shimmer: spells will come back.", "The mirror-carapace blazes. Nothing that is cast at the Tarrasque stays cast."])


def spec_for(phase):
    pal = dict(scale="#5a4636", belly="#8a7050", horn="#c8b890", eye="#ff5a20", tooth="#e0d4b0")
    head = dict(pal=dict(scale="#5a4636", neck="#5a4636", horn="#c8b890", eye="#ff5a20", mouth="#6a2a28", tooth="#e0d4b0"), rise=0.5, len=[5, 5], w=[9.0, 8.4], s=1.55, glow_mouth=phase >= 3)
    return dict(pal=pal, W=26, H=20, kind="tarrasque", heads=[head], wings=None, tail=[9, 9, 8, 8, 7, 6, 5])


def build(phase):
    c = Creature("tarrasque", phase, tex_w=512)
    info = D.dragon(c, spec_for(phase))
    hornm = bone("#c8b890", 0.1)
    s = (1.0, 1.1, 1.3)[phase - 1]
    # the two great forward-curving horns on the brow (the head's own horns sweep back; these are the Tarrasque's)
    hs = 1.55
    y = 18 + 20 - 3.0
    z = -15 - 10
    for sx in (-1, 1):
        p = (sx * 5.5 * hs / 1.55, y + 3.0, z - 4.0)
        for ln, wd, a in ((6.0 * s, 3.4, 22), (6.0 * s, 2.8, 52), (5.0 * s, 2.2, 82), (4.0 * s, 1.5, 108)):
            p = lib.beam(c, "neck_0_1", p, ln, wd, wd, (0, 0, -sx * a), hornm, k=1)
    for i in range(6):
        c.box("neck_0_1", (-3.0 + (i % 3) * 3.0 - 0.5, y + 5.0 + (i // 3), z - 2.0 - (i % 2) * 3.0), (1.0, 1.0, 1.0), emissive("#ff5a20", "#ff5a20"), k=1) if phase >= 2 else None
    c.note = None
    R = RIG
    D.rig(R, info, tail_amp=4, neck_amp=2)
    return c


def _extra(clip, t, L):
    return _EX(clip, t, L)


_probe = Creature("tarrasque", 1, tex_w=512)
_info = D.dragon(_probe, spec_for(1))
_EX = D.extra_motion(_info)
RIG = AnimRig(B.quad_motion(("leg_fl", "leg_fr", "leg_bl", "leg_br"), neck=["neck_0_0"], wings=[], stride=17, bob=0.9, rear=8, extra=_extra, heads=["neck_0_1"], lunge=14))
