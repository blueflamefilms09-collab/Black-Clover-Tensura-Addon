"""The clips of the Spirit of Light (docs/magic/spirit_light.md): the poses of the lion cub and of the seraph, one clip each, written to assets/nusmp/animations/entity/spiritlight_{cub,seraph}.animation.json
by tools/gen_spirit_light_geo.py. The names, lengths and faces are the table of the server rules (spiritlight.LightMoodRules: POSES); SpiritLightAssetsTest checks that the files and the table agree.

A face is not a texture swap: the base face is painted on the head, the other moods are thin overlay slabs in front of it, one bone each (`face_<mood>`; the base one is `face_base`). Every clip scales
the bones of the faces it does not show to almost nothing, so a pose and its face always go together. Rotation signs are those of client.geo.GeoDraw (checked in the headless preview).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spirit_light_lib as L  # noqa: E402

CUB_FACES = ("happy", "sleepy", "fierce", "surprised")
SERAPH_FACES = ("serene", "stern", "compassionate", "radiant")
CUB_FACE_BONES = {"happy": "face_base", "sleepy": "face_sleepy", "fierce": "face_fierce", "surprised": "face_surprised"}
SERAPH_FACE_BONES = {"serene": "face_base", "stern": "face_stern", "compassionate": "face_compassionate", "radiant": "face_radiant"}

# pose -> (length in seconds, loops, face). The table of spiritlight.LightMoodRules.
CUB_POSES = {"idle": (3.2, True, "happy"), "cast": (1.0, False, "fierce"), "pounce": (1.3, False, "fierce"), "roll": (1.6, False, "happy"), "stretch": (2.2, False, "sleepy"),
             "lick": (2.6, False, "happy"), "roar": (1.1, False, "fierce"), "sleep": (4.0, True, "sleepy"), "hurt": (0.6, False, "surprised"), "victory": (1.8, False, "happy")}
SERAPH_POSES = {"idle": (6.0, True, "serene"), "cast": (1.2, False, "stern"), "wings_open": (2.2, False, "radiant"), "halo_spin": (2.4, False, "serene"),
                "bless": (2.4, False, "compassionate"), "prayer": (5.0, True, "serene"), "hurt": (0.7, False, "stern"), "victory": (2.4, False, "radiant")}

OFF = 0.001


def faces(length, active, face_bones):
    """The scale tracks that show one face and hide the others."""
    out = {}
    for mood, bone in face_bones.items():
        v = (1.0, 1.0, 1.0) if mood == active else (OFF, OFF, OFF)
        out[bone] = {"scale": {0.0: v, float(length): v}}
    return out


def K(times, vals):
    return L.keys(times, vals)


def clip(length, loop, bones, face, face_bones):
    b = dict(bones)
    b.update(faces(length, face, face_bones))
    return {"loop": loop, "length": length, "bones": b}


def loopsine(T, amp, axis=0, phase=0.0, st=8, bias=0.0):
    return L.sine(T, amp, axis, phase, steps=st, bias=bias)



def flip_x(clips, bones):
    """The x rotation of GeoDraw turns a part that points FORWARD (a head, a jaw, an arm, the top of the body) UP for a positive angle; the clips below were written the other way round for those
    parts (a negative angle lifts), so their x angles are negated here once (checked in the headless preview). A part that points backward (the tail) keeps its sign: a negative angle lifts it."""
    for c in clips.values():
        for bn in bones:
            tr = c["bones"].get(bn, {}).get("rotation")
            if tr:
                c["bones"][bn]["rotation"] = {t_: (-v[0], v[1], v[2]) for t_, v in tr.items()}
    return clips


# ====================================================================================================================== the lion cub
def cub_clips():
    clips = {}
    st = 8
    # ---------------------------------------------------------------- idle: breathing, a tilt of the head, an ear flick, the mane shimmering, the tail swishing, the halo turning
    T = CUB_POSES["idle"][0]
    bones = {
        "body": {"scale": {t: (1.0, 1.0 + 0.016 * math.sin(t / T * math.tau), 1.0 + 0.010 * math.sin(t / T * math.tau)) for t in L.sine(T, 0, 0, steps=st)}},
        "head": {"rotation": L.merge(loopsine(T, 2.5, 0, 0.1), loopsine(T, 3.0, 1, 0.3), loopsine(T, 1.5, 2, 0.0))},
        "ear_L": {"rotation": K([0, 1.9, 2.1, 2.4, 2.6, T], [(0, 0, 0), (0, 0, 0), (0, 0, -9), (0, 0, 0), (0, 0, -5), (0, 0, 0)])},
        "mane_back": {"scale": {t: (1.0 + 0.012 * math.sin(t / T * math.tau), 1.0 + 0.020 * math.sin(t / T * math.tau + 1.0), 1.0) for t in L.sine(T, 0, 0, steps=st)}},
        "mane_L": {"rotation": loopsine(T, 3.0, 2, 0.15)}, "mane_R": {"rotation": loopsine(T, -3.0, 2, 0.15)},
        "mane_top": {"rotation": loopsine(T, 2.5, 0, 0.35)}, "mane_chest": {"rotation": loopsine(T, 1.8, 0, 0.5)},
        "arm_L": {"rotation": loopsine(T, 3.0, 0, 0.0)}, "arm_R": {"rotation": loopsine(T, 3.0, 0, 0.5)},
        "t0": {"rotation": L.merge(loopsine(T, 14.0, 1, 0.0), loopsine(T, 3.0, 0, 0.2))},
        "t1": {"rotation": L.merge(loopsine(T, 18.0, 1, -0.1), loopsine(T, 4.0, 0, 0.1))},
        "t2": {"rotation": loopsine(T, 20.0, 1, -0.2)},
        "tuft": {"rotation": L.merge(loopsine(T, 22.0, 1, -0.3), loopsine(T, 6.0, 0, -0.1))},
        "halo_tilt": {"rotation": L.merge(loopsine(T, 4.0, 0, 0.0), loopsine(T, 4.0, 2, 0.25))},
        "halo": {"rotation": L.spin(T, 1.0, 1, steps=st)},
    }
    clips["idle"] = clip(T, True, bones, "happy", CUB_FACE_BONES)
    # ---------------------------------------------------------------- cast (the beam): the head lifts, the jaw opens, the chest swells, the mane flares, the paws plant, the tail rises, the halo spins up
    CT = CUB_POSES["cast"][0]
    tt = [0.0, 0.18, 0.42, 0.7, 1.0]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (4, 0, 0), (-5, 0, 0), (-3, 0, 0), (0, 0, 0)]), "scale": K(tt, [(1, 1, 1), (0.98, 0.98, 1), (1.06, 1.06, 1.04), (1.03, 1.03, 1.02), (1, 1, 1)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (10, 0, 0), (-16, 0, 0), (-12, 0, 0), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (30, 0, 0), (26, 0, 0), (0, 0, 0)])},
        "mane_back": {"scale": K(tt, [(1, 1, 1), (1, 1, 1), (1.18, 1.14, 1.2), (1.1, 1.08, 1.1), (1, 1, 1)])},
        "mane_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 0, 14), (0, 0, 8), (0, 0, 0)])},
        "mane_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 0, -14), (0, 0, -8), (0, 0, 0)])},
        "mane_top": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-12, 0, 0), (-8, 0, 0), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-8, 0, 0), (-5, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-8, 0, 0), (-5, 0, 0), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-26, 0, 0), (-18, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-14, 0, 0), (-8, 0, 0), (0, 0, 0)])},
        "halo": {"rotation": L.spin(CT, 3.0, 1, steps=4)},
    }
    clips["cast"] = clip(CT, False, bones, "fierce", CUB_FACE_BONES)
    # ---------------------------------------------------------------- pounce: a crouch, a spring forward, a landing and a shake
    PT = CUB_POSES["pounce"][0]
    tt = [0.0, 0.28, 0.50, 0.78, 1.05, PT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (14, 0, 0), (-18, 0, 0), (8, 0, 0), (-3, 0, 0), (0, 0, 0)]),
                 "position": K(tt, [(0, 0, 0), (0, -3, 3), (0, 9, -12), (0, -2, -14), (0, 0, -6), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (12, 0, 0), (-14, 0, 0), (8, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (22, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (-10, 0, 0), (-60, 0, 0), (-25, 0, 0), (-4, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (-10, 0, 0), (-60, 0, 0), (-25, 0, 0), (-4, 0, 0), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (-18, 0, 0), (-30, 8, 0), (-12, -8, 0), (0, 0, 0), (0, 0, 0)])},
        "t1": {"rotation": K(tt, [(0, 0, 0), (-8, 0, 0), (-18, -10, 0), (-6, 10, 0), (0, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (-10, 0, 0), (-22, 14, 0), (-8, -14, 0), (0, 0, 0), (0, 0, 0)])},
        "mane_back": {"scale": K(tt, [(1, 1, 1), (1, 1, 1), (1.12, 1.1, 1.1), (1.05, 1.05, 1.05), (1, 1, 1), (1, 1, 1)])},
        "halo": {"rotation": L.spin(PT, 2.0, 1, steps=4)},
    }
    clips["pounce"] = clip(PT, False, bones, "fierce", CUB_FACE_BONES)
    # ---------------------------------------------------------------- roll: over on its back and round once, happy
    RT = CUB_POSES["roll"][0]
    tt = [0.0, 0.25, 0.65, 1.05, 1.35, RT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (-6, 0, 0), (-6, 0, 180), (-6, 0, 360), (0, 0, 360), (0, 0, 360)]),
                 "position": K(tt, [(0, 0, 0), (0, -4, 0), (0, -6, 0), (0, -4, 0), (0, 0, 0), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-50, 0, 25), (-30, 0, -20), (0, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-50, 0, -25), (-30, 0, 20), (0, 0, 0), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 20, 0), (0, -20, 0), (0, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 30, 0), (0, -30, 0), (0, 0, 0), (0, 0, 0)])},
        "halo_tilt": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 0, -360), (0, 0, -360), (0, 0, -360), (0, 0, -360)])},
        "halo": {"rotation": L.spin(RT, 1.0, 1, steps=4)},
    }
    clips["roll"] = clip(RT, False, bones, "happy", CUB_FACE_BONES)
    # ---------------------------------------------------------------- stretch: the front goes down and forward, the back end up, a wide yawn, then shake it off
    ST = CUB_POSES["stretch"][0]
    tt = [0.0, 0.5, 1.4, 1.8, ST]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (22, 0, 0), (22, 0, 0), (4, 0, 0), (0, 0, 0)]), "position": K(tt, [(0, 0, 0), (0, -2, -4), (0, -2, -4), (0, 0, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-18, 0, 0), (-26, 0, 0), (6, 0, 0), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (8, 0, 0), (36, 0, 0), (4, 0, 0), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (-70, 0, -6), (-70, 0, -6), (-10, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (-70, 0, 6), (-70, 0, 6), (-10, 0, 0), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (-30, 0, 0), (-34, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (-14, 0, 0), (-20, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "ear_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 18), (0, 0, 22), (0, 0, 0), (0, 0, 0)])},
        "ear_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, -18), (0, 0, -22), (0, 0, 0), (0, 0, 0)])},
        "halo": {"rotation": L.spin(ST, 1.0, 1, steps=4)},
    }
    clips["stretch"] = clip(ST, False, bones, "sleepy", CUB_FACE_BONES)
    # ---------------------------------------------------------------- lick: sits up, raises a paw to the mouth and licks it, the head tilting down
    LT = CUB_POSES["lick"][0]
    tt = [0.0, 0.4, 0.8, 1.1, 1.4, 1.7, 2.0, LT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (-3, 0, 0), (-3, 0, 4), (-3, 0, 4), (-3, 0, 4), (-3, 0, 4), (-3, 0, 2), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (14, 0, 6), (20, 0, 8), (16, 0, 8), (20, 0, 8), (16, 0, 8), (10, 0, 4), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (10, 0, 0), (2, 0, 0), (10, 0, 0), (2, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (-90, 0, 18), (-112, 0, 22), (-104, 0, 22), (-112, 0, 22), (-104, 0, 22), (-60, 0, 8), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (0, 14, 0), (0, -14, 0), (0, 14, 0), (0, -14, 0), (0, 14, 0), (0, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (0, 20, 0), (0, -20, 0), (0, 20, 0), (0, -20, 0), (0, 20, 0), (0, 0, 0), (0, 0, 0)])},
        "halo": {"rotation": L.spin(LT, 1.0, 1, steps=4)},
    }
    clips["lick"] = clip(LT, False, bones, "happy", CUB_FACE_BONES)
    # ---------------------------------------------------------------- roar (the pulse): a bigger version of the cast: head high, the jaw wide, the whole mane out
    OT = CUB_POSES["roar"][0]
    tt = [0.0, 0.2, 0.45, 0.8, OT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (6, 0, 0), (-9, 0, 0), (-6, 0, 0), (0, 0, 0)]), "scale": K(tt, [(1, 1, 1), (0.97, 0.97, 1), (1.09, 1.09, 1.06), (1.05, 1.05, 1.03), (1, 1, 1)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (14, 0, 0), (-24, 0, 0), (-18, 0, 0), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (38, 0, 0), (32, 0, 0), (0, 0, 0)])},
        "mane_back": {"scale": K(tt, [(1, 1, 1), (1, 1, 1), (1.25, 1.2, 1.28), (1.15, 1.12, 1.15), (1, 1, 1)])},
        "mane_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 0, 20), (0, 0, 12), (0, 0, 0)])},
        "mane_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (0, 0, -20), (0, 0, -12), (0, 0, 0)])},
        "mane_top": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-18, 0, 0), (-12, 0, 0), (0, 0, 0)])},
        "mane_chest": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-14, 0, 0), (-8, 0, 0), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-12, 0, -6), (-8, 0, -4), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-12, 0, 6), (-8, 0, 4), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-34, 0, 0), (-24, 0, 0), (0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (0, 0, 0), (-20, 0, 0), (-12, 0, 0), (0, 0, 0)])},
        "halo": {"rotation": L.spin(OT, 4.0, 1, steps=4)},
    }
    clips["roar"] = clip(OT, False, bones, "fierce", CUB_FACE_BONES)
    # ---------------------------------------------------------------- sleep (loops): curled up small, the head down, the tail round, slow breath
    SLT = CUB_POSES["sleep"][0]
    bones = {
        "body": {"position": K([0, SLT], [(0, -5, 0), (0, -5, 0)]),
                 "scale": {t: (1.04, 0.80 + 0.02 * math.sin(t / SLT * math.tau), 1.06) for t in L.sine(SLT, 0, 0, steps=st)},
                 "rotation": K([0, SLT], [(10, 0, 0), (10, 0, 0)])},
        "head": {"rotation": L.merge(K([0, SLT], [(34, 0, 10), (34, 0, 10)]), loopsine(SLT, 1.5, 0, 0.0))},
        "arm_L": {"rotation": K([0, SLT], [(-22, 0, 4), (-22, 0, 4)])}, "arm_R": {"rotation": K([0, SLT], [(-22, 0, -4), (-22, 0, -4)])},
        "ear_L": {"rotation": K([0, SLT], [(0, 0, -24), (0, 0, -24)])}, "ear_R": {"rotation": K([0, SLT], [(0, 0, 24), (0, 0, 24)])},
        "t0": {"rotation": K([0, SLT], [(10, 70, 0), (10, 70, 0)])}, "t1": {"rotation": K([0, SLT], [(0, 40, 0), (0, 40, 0)])},
        "t2": {"rotation": K([0, SLT], [(0, 30, 0), (0, 30, 0)])}, "tuft": {"rotation": L.merge(K([0, SLT], [(0, 30, 0), (0, 30, 0)]), loopsine(SLT, 4.0, 1, 0.0))},
        "mane_back": {"scale": {t: (1.0, 0.95 + 0.012 * math.sin(t / SLT * math.tau), 1.0) for t in L.sine(SLT, 0, 0, steps=st)}},
        "halo": {"rotation": L.spin(SLT, 0.5, 1, steps=st)},
        "halo_tilt": {"rotation": loopsine(SLT, 3.0, 0, 0.0)},
    }
    clips["sleep"] = clip(SLT, True, bones, "sleepy", CUB_FACE_BONES)
    # ---------------------------------------------------------------- hurt: a flinch: squashed, the head thrown back, the mane bristling, the ears flat
    HT = CUB_POSES["hurt"][0]
    tt = [0.0, 0.08, 0.2, 0.36, HT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (10, 0, 6), (6, 0, -5), (2, 0, 2), (0, 0, 0)]), "scale": K(tt, [(1, 1, 1), (1.05, 0.88, 1.05), (1.02, 0.95, 1.02), (1, 0.99, 1), (1, 1, 1)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-14, 0, -6), (-8, 0, 4), (-2, 0, 0), (0, 0, 0)])},
        "jaw": {"rotation": K(tt, [(0, 0, 0), (14, 0, 0), (8, 0, 0), (0, 0, 0), (0, 0, 0)])},
        "mane_back": {"scale": K(tt, [(1, 1, 1), (1.14, 1.1, 1.14), (1.08, 1.06, 1.08), (1.02, 1.02, 1.02), (1, 1, 1)])},
        "ear_L": {"rotation": K(tt, [(0, 0, 0), (0, 0, 28), (0, 0, 18), (0, 0, 4), (0, 0, 0)])}, "ear_R": {"rotation": K(tt, [(0, 0, 0), (0, 0, -28), (0, 0, -18), (0, 0, -4), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0), (-20, 0, 0), (-10, 0, 0), (0, 0, 0), (0, 0, 0)])},
    }
    clips["hurt"] = clip(HT, False, bones, "surprised", CUB_FACE_BONES)
    # ---------------------------------------------------------------- victory: a proud strut on the spot, chest out, tail high, the paws marching, the halo spinning
    VT = CUB_POSES["victory"][0]
    tt = [0.0, 0.225, 0.45, 0.675, 0.9, 1.125, 1.35, 1.575, VT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0)] + [(-7, 0, 0)] * 7 + [(0, 0, 0)]),
                 "position": K(tt, [(0, 0, 0), (0, 3, 0), (0, 0, 0), (0, 3, 0), (0, 0, 0), (0, 3, 0), (0, 0, 0), (0, 3, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-10, 0, 6), (-10, 0, 0), (-10, 0, -6), (-10, 0, 0), (-10, 0, 6), (-10, 0, 0), (-8, 0, -4), (0, 0, 0)])},
        "arm_L": {"rotation": K(tt, [(0, 0, 0), (-16, 0, 0), (6, 0, 0), (-16, 0, 0), (6, 0, 0), (-16, 0, 0), (6, 0, 0), (-16, 0, 0), (0, 0, 0)])},
        "arm_R": {"rotation": K(tt, [(0, 0, 0), (6, 0, 0), (-16, 0, 0), (6, 0, 0), (-16, 0, 0), (6, 0, 0), (-16, 0, 0), (6, 0, 0), (0, 0, 0)])},
        "t0": {"rotation": K(tt, [(0, 0, 0)] + [(-30, 0, 0)] * 7 + [(0, 0, 0)])},
        "tuft": {"rotation": K(tt, [(0, 0, 0), (-16, 12, 0), (-16, -12, 0), (-16, 12, 0), (-16, -12, 0), (-16, 12, 0), (-16, -12, 0), (-16, 12, 0), (0, 0, 0)])},
        "mane_back": {"scale": K(tt, [(1, 1, 1)] + [(1.1, 1.08, 1.1)] * 7 + [(1, 1, 1)])},
        "halo": {"rotation": L.spin(VT, 2.0, 1, steps=8)},
    }
    clips["victory"] = clip(VT, False, bones, "happy", CUB_FACE_BONES)
    return flip_x(clips, ("body", "head", "jaw", "arm_L", "arm_R", "mane_top", "mane_chest"))


# ====================================================================================================================== the seraph
def seraph_clips():
    clips = {}
    st = 8
    FB = SERAPH_FACE_BONES

    def spread(a):
        """Wings: the left wing spreads with a negative z turn, the right with a positive one (the model's left is +x)."""
        return {"wingA_L": (0, 0, -a), "wingA_R": (0, 0, a)}

    def wingtrack(name, times, amounts, scale=1.0):
        return {name + "_L": {"rotation": K(times, [(0, 0, -a * scale) for a in amounts])}, name + "_R": {"rotation": K(times, [(0, 0, a * scale) for a in amounts])}}

    def armtrack(times, lx, lz, rx=None, rz=None):
        """Arms: rotation x turns the arm forward (positive), rotation z turns the left arm out (positive) and the right arm out (negative)."""
        rx = lx if rx is None else rx
        rz = [-v for v in lz] if rz is None else rz
        return {"arm_L": {"rotation": K(times, [(x, 0, z) for x, z in zip(lx, lz)])}, "arm_R": {"rotation": K(times, [(x, 0, z) for x, z in zip(rx, rz)])}}

    def forearm(times, lx, lz):
        return {"fore_L": {"rotation": K(times, [(x, 0, z) for x, z in zip(lx, lz)])}, "fore_R": {"rotation": K(times, [(x, 0, -z) for x, z in zip(lx, lz)])}}

    def rings(T, f1=1.0, f2=-2.0, f3=2.0, steps=st):
        return {"ring1": {"rotation": L.spin(T, f1, 1, steps=steps)}, "ring2": {"rotation": L.spin(T, f2, 1, steps=steps)}, "ring3": {"rotation": L.spin(T, f3, 1, steps=steps)}}

    # ---------------------------------------------------------------- idle: the slow hover of a guardian: the wings breathe, the rings turn, the robe sways, the gem spins
    T = SERAPH_POSES["idle"][0]
    bones = {
        "body": {"position": {t: (0, 0.8 * math.sin(t / T * math.tau), 0) for t in L.sine(T, 0, 0, steps=st)}},
        "robe_low": {"rotation": L.merge(loopsine(T, 1.6, 2, 0.0), loopsine(T, 1.0, 0, 0.25))},
        "robe_mid": {"rotation": loopsine(T, 1.0, 2, 0.1)},
        "head": {"rotation": L.merge(loopsine(T, 1.6, 0, 0.2), loopsine(T, 2.0, 1, 0.0))},
        "prism": {"rotation": L.spin(T, 3.0, 1, steps=st)},
        "prism_hover": {"position": {t: (0, 1.2 * math.sin(t / T * math.tau + 1.0), 0) for t in L.sine(T, 0, 0, steps=st)}},
        "arm_L": {"rotation": K([0, T], [(6, 0, 4), (6, 0, 4)])}, "arm_R": {"rotation": K([0, T], [(6, 0, -4), (6, 0, -4)])},
    }
    bones.update(rings(T))
    bones["wingA_L"] = {"rotation": L.merge(loopsine(T, -5.0, 2, 0.0), K([0, T], [(0, 0, 0), (0, 0, 0)]))}
    bones["wingA_R"] = {"rotation": loopsine(T, 5.0, 2, 0.0)}
    bones["wingB_L"] = {"rotation": loopsine(T, -3.0, 2, 0.2)}
    bones["wingB_R"] = {"rotation": loopsine(T, 3.0, 2, 0.2)}
    bones["wingC_L"] = {"rotation": loopsine(T, -2.0, 2, 0.4)}
    bones["wingC_R"] = {"rotation": loopsine(T, 2.0, 2, 0.4)}
    clips["idle"] = clip(T, True, bones, "serene", FB)
    # ---------------------------------------------------------------- cast (the beam): the arms come forward, the face wings part, the flying wings sweep back, the core flares, the rings spin up
    CT = SERAPH_POSES["cast"][0]
    tt = [0.0, 0.25, 0.5, 0.85, CT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (4, 0, 0), (-6, 0, 0), (-3, 0, 0), (0, 0, 0)])},
        "head": {"scale": K(tt, [(1, 1, 1), (1, 1, 1), (1.14, 1.14, 1.14), (1.06, 1.06, 1.06), (1, 1, 1)])},
        "prism": {"rotation": L.spin(CT, 4.0, 1, steps=4)},
    }
    bones.update(armtrack(tt, [6, 40, 88, 80, 6], [4, 4, 4, 4, 4]))
    bones.update(forearm(tt, [0, 10, 4, 4, 0], [0, 0, 0, 0, 0]))
    bones.update(wingtrack("wingA", tt, [0, 0, 26, 16, 0]))
    bones.update(wingtrack("wingC", tt, [0, 0, 60, 40, 0]))
    bones.update(wingtrack("wingB", tt, [0, 0, 24, 14, 0]))
    bones.update(rings(CT, 2.0, -4.0, 4.0, 4))
    clips["cast"] = clip(CT, False, bones, "stern", FB)
    # ---------------------------------------------------------------- wings_open: every wing opens wide, the face wings part to show the face, the rings flare, the gem rises
    WT = SERAPH_POSES["wings_open"][0]
    tt = [0.0, 0.7, 1.5, WT]
    bones = {
        "body": {"position": K(tt, [(0, 0, 0), (0, 2, 0), (0, 2.5, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-6, 0, 0), (-8, 0, 0), (0, 0, 0)])},
        "prism_hover": {"position": K(tt, [(0, 0, 0), (0, 5, 0), (0, 6, 0), (0, 0, 0)])},
        "prism": {"rotation": L.spin(WT, 3.0, 1, steps=4)},
    }
    bones.update(armtrack(tt, [6, 6, 6, 6], [4, 50, 56, 4]))
    bones.update(wingtrack("wingA", tt, [0, 40, 46, 0]))
    bones.update(wingtrack("wingB", tt, [0, 56, 66, 0]))
    bones.update(wingtrack("wingC", tt, [0, 86, 96, 0]))
    bones.update(rings(WT, 1.0, -2.0, 2.0, 4))
    clips["wings_open"] = clip(WT, False, bones, "radiant", FB)
    # ---------------------------------------------------------------- halo_spin: the rings turn fast, the body lifts a little
    HT = SERAPH_POSES["halo_spin"][0]
    tt = [0.0, 0.4, 1.8, HT]
    bones = {
        "body": {"position": K(tt, [(0, 0, 0), (0, 3, 0), (0, 3, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-5, 0, 0), (-5, 0, 0), (0, 0, 0)])},
        "prism": {"rotation": L.spin(HT, 6.0, 1, steps=4)},
    }
    bones.update(armtrack(tt, [6, 20, 20, 6], [4, 30, 30, 4]))
    bones.update(wingtrack("wingA", tt, [0, 18, 18, 0]))
    bones.update(wingtrack("wingC", tt, [0, 30, 30, 0]))
    bones.update(rings(HT, 4.0, -6.0, 6.0, 4))
    clips["halo_spin"] = clip(HT, False, bones, "serene", FB)
    # ---------------------------------------------------------------- bless: the arms open wide, palms up, the head tilts with compassion
    BT = SERAPH_POSES["bless"][0]
    tt = [0.0, 0.6, 1.8, BT]
    bones = {
        "head": {"rotation": K(tt, [(0, 0, 0), (8, 0, 6), (8, 0, 6), (0, 0, 0)])},
        "body": {"position": K(tt, [(0, 0, 0), (0, 1.5, 0), (0, 1.5, 0), (0, 0, 0)])},
    }
    bones.update(armtrack(tt, [6, 18, 18, 6], [4, 62, 62, 4]))
    bones.update(forearm(tt, [0, 50, 50, 0], [0, 10, 10, 0]))
    bones.update(wingtrack("wingA", tt, [0, 24, 24, 0]))
    bones.update(wingtrack("wingC", tt, [0, 66, 66, 0]))
    bones.update(wingtrack("wingB", tt, [0, 30, 30, 0]))
    bones.update(rings(BT, 1.5, -2.0, 2.0, 4))
    clips["bless"] = clip(BT, False, bones, "compassionate", FB)
    # ---------------------------------------------------------------- prayer (loops): the head bowed, the hands clasped at the chest, the wings folded close, a slow hover
    PT = SERAPH_POSES["prayer"][0]
    bones = {
        "body": {"position": {t: (0, -1.5 + 0.7 * math.sin(t / PT * math.tau), 0) for t in L.sine(PT, 0, 0, steps=st)}},
        "head": {"rotation": L.merge(K([0, PT], [(22, 0, 0), (22, 0, 0)]), loopsine(PT, 1.2, 0, 0.0))},
        "robe_low": {"rotation": loopsine(PT, 1.0, 2, 0.0)},
        "prism": {"rotation": L.spin(PT, 2.0, 1, steps=st)},
    }
    bones.update(armtrack([0, PT], [55, 55], [-6, -6]))
    bones.update(forearm([0, PT], [78, 78], [-40, -40]))
    bones.update(wingtrack("wingA", [0, PT], [-22, -22]))
    bones.update(wingtrack("wingC", [0, PT], [-14, -14]))
    bones.update(wingtrack("wingB", [0, PT], [-6, -6]))
    bones.update(rings(PT, 0.5, -1.0, 1.0, st))
    clips["prayer"] = clip(PT, True, bones, "serene", FB)
    # ---------------------------------------------------------------- hurt: a recoil: the body leans back, the wings shiver in, the rings stutter
    UT = SERAPH_POSES["hurt"][0]
    tt = [0.0, 0.08, 0.2, 0.4, UT]
    bones = {
        "body": {"rotation": K(tt, [(0, 0, 0), (-10, 0, 4), (-5, 0, -3), (-1, 0, 1), (0, 0, 0)]), "position": K(tt, [(0, 0, 0), (0, 0, 3), (0, 0, 1), (0, 0, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-12, 0, 0), (-6, 0, 0), (0, 0, 0), (0, 0, 0)])},
    }
    bones.update(armtrack(tt, [6, 24, 14, 8, 6], [4, 14, 8, 4, 4]))
    bones.update(wingtrack("wingA", tt, [0, -14, -6, -2, 0]))
    bones.update(wingtrack("wingC", tt, [0, 22, 10, 4, 0]))
    bones.update(wingtrack("wingB", tt, [0, 10, 4, 0, 0]))
    bones.update(rings(UT, 1.0, -2.0, 2.0, 4))
    clips["hurt"] = clip(UT, False, bones, "stern", FB)
    # ---------------------------------------------------------------- victory: the wings sweep up and wide, the arms raised, the rings and the gem spin, the whole figure rising
    VT = SERAPH_POSES["victory"][0]
    tt = [0.0, 0.5, 1.2, 1.9, VT]
    bones = {
        "body": {"position": K(tt, [(0, 0, 0), (0, 4, 0), (0, 5, 0), (0, 3, 0), (0, 0, 0)])},
        "head": {"rotation": K(tt, [(0, 0, 0), (-8, 0, 0), (-10, 0, 0), (-6, 0, 0), (0, 0, 0)])},
        "prism_hover": {"position": K(tt, [(0, 0, 0), (0, 5, 0), (0, 6, 0), (0, 4, 0), (0, 0, 0)])},
        "prism": {"rotation": L.spin(VT, 6.0, 1, steps=8)},
    }
    bones.update(armtrack(tt, [6, 4, 4, 4, 6], [4, 150, 160, 120, 4]))
    bones.update(wingtrack("wingA", tt, [0, -34, -40, -24, 0]))
    bones.update(wingtrack("wingC", tt, [0, 80, 94, 60, 0]))
    bones.update(wingtrack("wingB", tt, [0, 50, 60, 36, 0]))
    bones.update(rings(VT, 3.0, -5.0, 5.0, 8))
    clips["victory"] = clip(VT, False, bones, "radiant", FB)
    return flip_x(clips, ("body", "head"))
