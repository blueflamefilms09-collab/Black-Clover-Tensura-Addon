"""Why: the LICH KING of the Ossuary: where the lich is a robed caster, he is a skeleton in dark frost-steel plate under a horned great helm with its visor open on the skull, a tattered fur-collared
cape, spiked pauldrons and a huge runeblade; ice crystals grow on him (phase 2) and a ring of rune light opens on the floor under his feet (phase 3). Player skin (skull base, plate on the layers) plus extras.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, shade, bone, cloth, metal, plate, emissive, glowfill, wisp, halo, humanoid_motion, beam, spike, curve, ring, TAU, keys, smooth, flat, fur)
import undead
import humans
import look_lich as L

LOOK = dict(id="lich_king", name="Lich King", kind="humanoid", slim=False, height=2.2, glow="#6ad8ff", boost=1.1, shadow=0.7, dust=True, undead=True,
            captions=["", "Ice crystals break through the Lich King's armour and the runeblade sings.", "The Frozen Throne answers: a ring of runes opens under the Lich King."])

P = dict(id="lich_king", bone="#c8d4dc", dark="#0a1018", robe="#2a3040", trim="#6ad8ff", eye="#6ad8ff", eye2="#a0f0ff", soul="#6ad8ff", rune="#6ad8ff", gem="#6ad8ff",
         armor="#2c3446", armor_trim="#6ad8ff", glow="#6ad8ff", coat="#2c3446", belt="#10141c", buckle="#6ad8ff", eye_n=2, hood=False)


def helm(sk, phase):
    """The hat layer as a great helm: plate over the whole head with an open window on the skull, a brow band and a nasal guard."""
    pm = plate("#2c3446", rim="#6ad8ff")
    for f in ("top", "back", "right", "left", "bottom"):
        sk.paint("hat", pm, (f,))
    fr = sk.face("hat", "front")
    pm(fr)
    fr.clear(1, 3, 6, 5)
    fr.rect(3, 3, 2, 3, "#2c3446")                                    # the nasal guard
    fr.rect(0, 2, 8, 1, "#6ad8ff", "#6ad8ff")
    for f in ("right", "left"):
        s = sk.face("hat", f)
        s.clear(3, 3, 5, 4)


def build(phase):
    sk = lib.Skin(slim=False, seed=lib.seed_of("lich_king", phase))
    undead.skull_head(sk, P, phase)
    undead.ribcage(sk, P, phase)
    undead.arms_legs(sk, P, phase)
    humans.armour(sk, P, phase)
    helm(sk, phase)
    sk.opaque_base()
    c = Humanoid("lich_king", phase, sk)
    pm = plate("#2c3446", rim="#6ad8ff")
    ice = emissive("#9ae4ff", "#6ad8ff", soft=0.35)
    # horned great helm
    c.bone("x_helm", "head", pivot=(0, 32, 0))
    for s in (-1, 1):
        curve(c, "x_helm", (s * 4.3, 30.5, 0.0), [4.0, 4.2, 3.6, 2.6], [2.2, 1.8, 1.4, 1.0], [(0, 0, -s * 62), (0, 0, -s * 18), (0, 0, s * 20), (0, 0, s * 38)], pm, k=1)
    for i, (x, h) in enumerate(((-2.4, 4.0), (0, 6.5), (2.4, 4.0))):
        spike(c, "x_helm", (x, 32.8, -2.0), h, 1.6, pm, rot=(-6, 0, 0), n=3)
    c.box("x_helm", (-0.8, 31.3, -4.7), (1.6, 1.8, 0.7), ice, k=1)
    c.note_extra("horned_helm", "head", ["x_helm"])
    # pauldrons
    for s, arm in ((-1, "arm_r"), (1, "arm_l")):
        c.box(arm, (s * 6.5 - 3.2, 21.6, -3.4), (6.4, 3.0, 6.8), pm, k=2)
        for i in range(3):
            spike(c, arm, (s * (5.4 + i * 1.7), 24.4, -1.2 + i * 1.2), 5.5 - i * 0.8, 1.7, pm, rot=(0, 0, -s * 16), n=3)
        if phase >= 2:
            for i in range(2):
                spike(c, arm, (s * (6.0 + i * 2.0), 24.2, 0.0), 6 + 3 * (phase - 1), 2.0, ice, rot=(0, 0, -s * (28 + i * 14)), n=3)
    c.note_extra("pauldrons", "arm_r", [])
    # cape and fur collar
    fr = fur("#8a98a8", "#d0dce8")
    c.bone("x_collar", "body", pivot=(0, 23, 0))
    c.box("x_collar", (-6.0, 22.0, -3.6), (12.0, 3.2, 7.2), fr, k=2)
    c.note_extra("fur_collar", "body", ["x_collar"])
    cape = cloth("#141a28", 4, tatter=5, trim="#6ad8ff")
    names = []
    for i, x in enumerate((-3.2, 0.0, 3.2)):
        n = "x_cape%d" % i
        c.bone(n, "body", pivot=(x, 22.8, 2.7))
        c.box(n, (x - 1.7, 0.5 - (i % 2) * 2.0, 2.4), (3.4, 22.4 + (i % 2) * 2.0, 0.9), cape, k=2)
        names.append(n)
    c.note_extra("cape", "body", names)
    # the runeblade
    c.bone("x_blade", "arm_r", pivot=(-6.0, 10.8, -1.2))
    c.box("x_blade", (-6.7, 8.2, -1.9), (1.5, 6.0, 1.5), bone("#1c2230", 0.1), k=1)
    c.box("x_blade", (-9.6, 13.6, -2.2), (7.2, 1.4, 2.4), metal("#2c3446", "#6ad8ff"), k=1)
    bl = 30 + 4 * (phase - 1)
    c.box("x_blade", (-7.4, 15.0, -1.6), (2.8, bl, 1.2), over(metal("#8aa0b8", "#e8f4ff"), on(("front", "back"), lambda cv: [cv.rect(cv.w // 2, y, 1, 2, "#6ad8ff", "#6ad8ff") for y in range(2, cv.h - 3, 5)])), k=2, rot=(-24, 0, 0), pivot=(-6.0, 15.0, -1.0))
    c.box("x_blade", (-9.0, 14.5, -2.6), (6.0, bl + 2, 3.2), halo("#6ad8ff", 0.7), k=1, rot=(-24, 0, 0), pivot=(-6.0, 15.0, -1.0))
    c.note_extra("runeblade", "arm_r", ["x_blade"])
    if phase >= 3:
        c.bone("x_runes", "root", pivot=(0, 0.3, 0))
        c.box("x_runes", (-18, 0.1, -18), (36, 0.4, 36), halo("#6ad8ff", 0.95), k=1)
        for i in range(8):
            a = TAU * i / 8
            c.box("x_runes", (math.cos(a) * 13 - 1.0, 0.2, math.sin(a) * 13 - 1.0), (2.0, 0.6, 2.0), emissive("#9ae4ff", "#6ad8ff"), k=1)
        c.note_extra("rune_ring", "root", ["x_runes"])
    c.note_extra("frost", "root", L.wisps(c, P, phase, colors=("#6ad8ff", "#c8f4ff"), count=(4, 8, 10)[phase - 1], height=(8, 14), radius=(9.5, 13.0)))
    R = RIG
    for i in range(3):
        R.sway("x_cape%d" % i, 0, 6, 1, 0.12 * i)
    R.sway("x_blade", 2, 1.5, 1)
    for i in range(10):
        R.sway("x_wisp%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        R.sway("x_wisp%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    R.sway("x_runes", 1, 1.0, 1, 0, kind="spin")
    return c


def _extra(clip, t, L_):
    d = {}
    if clip in ("cast", "cast_big"):
        d["x_blade"] = {"rot": (-10, 0, 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=22, arm_swing=14, bob=0.4, attack_hand="r"))
