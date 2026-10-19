"""Why: the ELDER BRAIN of an illithid colony: a huge pink brain floating in a jar of green ooze, hung with tentacles that carry it across the floor. A glass jar with a brass rim and corner posts,
a glowing green ooze, a folded brain in two hemispheres with a stem, eight crawling tentacles round the base and two long ones for lashing. Phase 2 the glass cracks and the brain swells, phase 3
the jar bursts: shards of glass hang in the air round a bare, glowing brain ringed with psionic light. A full GeoLite body.
"""
import math
import lib
from lib import Creature, AnimRig, over, on, shade, flat, scales, emissive, halo, wisp, TAU, keys, smooth, bone, metal, glowfill, glass, brain, flesh, ring
import beasts as B

LOOK = dict(id="elder_brain", name="Elder Brain", kind="geo", height=3.0, glow="#c080ff", boost=0.95, shadow=1.0, walk_cycle=2.0, run_cycle=3.4, dust=False,
            captions=["", "The jar cracks. The Elder Brain swells and the whole hall hums.", "The jar bursts. A bare brain, ringed with psionic light, hangs among its own glass."])


def build(phase):
    c = Creature("elder_brain", phase, tex_w=512)
    c.bone("root", pivot=(0, 0, 0))
    c.bone("jar", "root", pivot=(0, 26, 0))
    c.bone("brain", "jar", pivot=(0, 34, 0))
    brass = metal("#a8884a", "#e8d080")
    pink = brain("#c8708e", "#7a3050")
    ooze = emissive("#3aa050", "#2a8a40", soft=0.4)
    # the jar: base plate, ring, posts, glass, ooze
    c.box("jar", (-11, 14, -11), (22, 3, 22), brass, k=2)
    c.box("jar", (-10, 17, -10), (20, 12.5, 20), ooze, k=1)
    if phase < 3:
        ring(c, "jar", 41.0, 2.5, 10.5, 10.5, 1.0, brass, k=1)
        for sx in (-1, 1):
            for sz in (-1, 1):
                c.box("jar", (sx * 10.0 - 1.1, 14, sz * 10.0 - 1.1), (2.2, 29.0, 2.2), brass, k=1)
        pane = glass("#9ae8b0")
        crack = phase >= 2
        for (ox, oz, sx_, sz_) in ((-9.5, -10.4, 19.0, 0.6), (-9.5, 9.8, 19.0, 0.6), (-10.4, -9.5, 0.6, 19.0), (9.8, -9.5, 0.6, 19.0)):
            def pn(cv, crack=crack):
                pane(cv)
                if crack and cv.face in ("front", "back", "left", "right") and cv.w > 6:
                    cv.line(2, 0, cv.w // 2, cv.h // 2, "#c8ffd8", "#80ffa0")
                    cv.line(cv.w // 2, cv.h // 2, cv.w - 3, cv.h - 1, "#c8ffd8", "#80ffa0")
                    cv.line(cv.w // 2, cv.h // 2, cv.w // 4, cv.h - 1, "#c8ffd8", "#80ffa0")
            c.box("jar", (ox, 17, oz), (sx_, 24, sz_), pn, k=1)
    else:
        for i in range(10):                                           # the shards that hang where the jar was
            a = TAU * i / 10
            nm = "x_shard%d" % i
            c.bone(nm, "root", pivot=(math.cos(a) * 14, 28 + (i % 3) * 5, math.sin(a) * 14))
            c.box(nm, (math.cos(a) * 14 - 1.0, 28 + (i % 3) * 5, math.sin(a) * 14 - 0.3), (2.4, 5.5, 0.6), glass("#bdf8c8"), k=1, rot=(12 * (i % 3), 30 * i, 18), pivot=(math.cos(a) * 14, 31, math.sin(a) * 14))
    # the brain: two hemispheres, a cerebellum and a stem
    s = 1.0 + 0.1 * (phase - 1)
    for sx in (-1, 1):
        B.blob(c, "brain", (sx * 5.4 * s, 35.5, 0.0), (11.5 * s, 11 * s, 15 * s), pink, bands=6, k=2)
    B.blob(c, "brain", (0, 31.5, 5.5), (9 * s, 6 * s, 6 * s), pink, bands=4, k=1)
    c.box("brain", (-1.6, 27.0, -1.6), (3.2, 6.0, 3.2), flat("#a85a78", 0.08), k=1)
    c.box("brain", (-13 * s, 28.5, -13 * s), (26 * s, 17 * s, 26 * s), halo("#d090ff", 0.55 + 0.1 * phase), k=1)
    if phase >= 3:
        for r_, y in ((11.0, 41.0), (14.0, 36.0)):
            c.bone("x_psi%d" % int(r_), "root", pivot=(0, y, 0))
            for i in range(12):
                a = TAU * i / 12
                c.box("x_psi%d" % int(r_), (math.cos(a) * r_ - 1.0, y - 0.4, math.sin(a) * r_ - 1.0), (2.0, 0.8, 2.0), emissive("#e0b0ff", "#c080ff", soft=0.2), k=1)
    # the tentacles
    skin = flesh("#8a4a7a", "#5a2a50", 0.02)
    tip = flesh("#b06a98", "#6a3a60", 0.02)
    names = []
    for i in range(8):
        a = TAU * i / 8
        dx, dz = math.cos(a), math.sin(a)
        names.append(B.tentacle(c, "jar", "x_t%d" % i, (dx * 7.5, 15.0, dz * 7.5), (dx, dz), [6.5, 6.5, 6, 5.5, 5, 4.5], [4.4, 4.0, 3.6, 3.2, 2.7, 2.2], skin, tilt=58 + (i % 2) * 8, curl=-8 if i % 2 else 10,
                                tip=tip, sucker=flesh("#e8a0c8", None)))
    for i, a in enumerate((-0.35, 0.35)):
        dx, dz = math.sin(a), -math.cos(a)
        nl = B.tentacle(c, "jar", "x_long%d" % i, (dx * 8.0, 17.0, dz * 8.0), (dx, dz), [7, 7, 7, 6, 6, 5, 5], [3.4, 3.0, 2.7, 2.4, 2.1, 1.8, 1.5], skin, tilt=72, curl=6, tip=tip)
        names.append(nl)
    R = RIG
    for i in range(8):
        B.sway_names(R, ["x_t%d_%d" % (i, s_) for s_ in range(6)], 0, 7, 1, 0.14, 2, 6, phase=i * 0.125)
    for i in range(2):
        B.sway_names(R, ["x_long%d_%d" % (i, s_) for s_ in range(7)], 0, 8, 1, 0.12, 2, 6, phase=i * 0.5)
    R.sway("brain", 3, 0.045, 2, 0.0, kind="scale")
    R.sway("brain", 1, 3, 1, 0.0)
    for i in range(10):
        R.sway("x_shard%d" % i, 1, 25, 1, i * 0.1)
        R.sway("x_shard%d" % i, 1, 1.5, 1, i * 0.1, kind="pos")
    R.sway("x_psi11", 1, 1.0, 1, 0, kind="spin")
    R.sway("x_psi14", 1, -1.0, 1, 0, kind="spin")
    return c


def _extra(clip, t, L):
    d = {}
    k = t / L
    if clip == "attack":
        lg = keys(k, [(0, 0.0), (0.22, -0.4), (0.42, 1.0), (0.7, 0.2), (1, 0.0)])
        for i in range(2):
            for s_ in range(7):
                d["x_long%d_%d" % (i, s_)] = {"rot": (-26 * lg, 0, 0)}
    if clip in ("cast", "cast_big"):
        up = 1.0 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
        d["brain"] = {"scale": (1 + 0.12 * up, 1 + 0.12 * up, 1 + 0.12 * up), "pos": (0, 2.0 * up, 0)}
        for i in range(8):
            d["x_t%d_0" % i] = {"rot": (-14 * up, 0, 0)}
    return d


RIG = AnimRig(B.float_motion("jar", hover=2.5, bob=1.0, tilt=2.2, extra=_extra, attack_dist=-4.0, rise=2.0))
