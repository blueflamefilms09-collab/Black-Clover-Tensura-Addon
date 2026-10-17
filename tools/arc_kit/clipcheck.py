"""Why: the clips are generated, not keyed by hand, so the kit has to check its own work: an arm that sinks into the chest, an elbow that flips from one side to the other
between two frames, a one-shot that ends on another pose than the one it started from. These rules are read back from the animation data the way the game plays it (linear
interpolation between the written keys), so they judge what the player sees and not what the solver intended. The selftest runs them on a set of archetypes; the build runs
the arm rules on every character and, if a two-handed prop breaks them, carries that prop in one hand instead (a character that is built is a character that passes).
"""
import math

import numpy as np

from .anim import GRID
from .rig import rot_matrix

SINK_LIMIT = 1.6          # pixels an arm may sink into the torso (a tenth of a block) before it is a problem
FLIP_LIMIT = 100.0        # degrees the shoulder may turn between two samples of the grid before the elbow counts as having flipped
JOIN_LIMIT = 12.0         # degrees a one-shot's arm may end away from where it started (the swivel steps are 8 degrees apart)


def rot_angle(a, b):
    """The angle in degrees of the turn between two bone rotations given as (x, y, z) degrees (the same pose written with other Euler angles gives 0)."""
    R = rot_matrix(*a).T @ rot_matrix(*b)
    return math.degrees(math.acos(max(-1.0, min(1.0, (float(np.trace(R)) - 1.0) / 2.0))))


def _sample(tr, t, default):
    if not tr:
        return np.array(default, float)
    ks = sorted((float(k), v) for k, v in tr.items())
    if t <= ks[0][0]:
        return np.array(ks[0][1], float)
    if t >= ks[-1][0]:
        return np.array(ks[-1][1], float)
    for i in range(len(ks) - 1):
        if ks[i][0] <= t <= ks[i + 1][0]:
            u = (t - ks[i][0]) / max(1e-9, ks[i + 1][0] - ks[i][0])
            return np.array(ks[i][1], float) * (1 - u) + np.array(ks[i + 1][1], float) * u
    return np.array(ks[-1][1], float)


def pose_at(R, clip, t):
    """The pose dict the game would compute at second t of a written clip (linear interpolation, like GeoAnim)."""
    pose = {}
    for bone, tracks in clip["bones"].items():
        e = {}
        if "rotation" in tracks:
            e["rot"] = tuple(_sample(tracks["rotation"], t, (0, 0, 0)))
        if "position" in tracks:
            e["pos"] = tuple(_sample(tracks["position"], t, (0, 0, 0)))
        if "scale" in tracks:
            e["scale"] = tuple(_sample(tracks["scale"], t, (1, 1, 1)))
        pose[bone] = e
    return pose


def arm_depth(R, pose, side):
    """How far (pixels) the boxes of the upper arm and the forearm of one side reach into the torso box under `pose`."""
    d = R.d
    body_pose = {k: v for k, v in pose.items() if k not in ("root", "body")}
    sub = {k: v for k, v in body_pose.items() if k in ("arm_" + side, "forearm_" + side, "hand_" + side)}
    hx, hz = d.tw / 2 - 0.05, d.td / 2 - 0.05
    y0, y1 = d.hip_y + 0.3, d.sh_y - 0.1
    worst = 0.0
    for b in ("arm_" + side, "forearm_" + side):
        for cube in R.m.bones[b]["cubes"][:1]:
            o, s = cube["origin"], cube["size"]
            for k in range(8):
                p = (o[0] + (s[0] if k & 1 else 0), o[1] + (s[1] if k & 2 else 0), o[2] + (s[2] if k & 4 else 0))
                if b.startswith("arm_") and p[1] > d.sh_piv - 1.0:
                    continue
                q = R.pose_point(b, p, sub)
                if abs(q[0]) < hx and y0 < q[1] < y1 and abs(q[2]) < hz:
                    worst = max(worst, min(hx - abs(q[0]), hz - abs(q[2])))
    return worst


def worst_sink(R, clip, fps=30):
    """The deepest an arm sinks into the torso anywhere in a written clip (sampled at `fps`)."""
    worst = 0.0
    for k in range(int(clip["animation_length"] * fps) + 1):
        pose = pose_at(R, clip, k / float(fps))
        for side in ("l", "r"):
            worst = max(worst, arm_depth(R, pose, side))
    return worst


def worst_flip(clip, bone):
    """The largest turn (degrees per grid step) of a bone's rotation track between two written keys."""
    tr = clip["bones"].get(bone, {}).get("rotation")
    if not tr:
        return 0.0
    ks = sorted(tr, key=float)
    worst = 0.0
    for k0, k1 in zip(ks, ks[1:]):
        dt = max(1.0 / GRID, float(k1) - float(k0))
        worst = max(worst, rot_angle(tr[k0], tr[k1]) * (1.0 / GRID) / dt)
    return worst


def arm_problems(R):
    """The rules about the arms, for every clip of the rig (R.A.clips): a list of plain sentences, empty when all hold."""
    out = []
    clips = R.A.clips
    for name, c in clips.items():
        sink = worst_sink(R, c)
        if sink >= SINK_LIMIT:
            out.append("%s: an arm sinks %.1f px into the torso" % (name, sink))
        for bone in ("arm_l", "arm_r"):
            f = worst_flip(c, bone)
            if f >= FLIP_LIMIT:
                out.append("%s: %s turns %.0f degrees between two samples (the elbow flips)" % (name, bone, f))
    if "idle" in clips:
        idle0 = pose_at(R, clips["idle"], 0.0)
        for name in ("attack", "hurt", "cast_big"):
            if name not in clips:
                continue
            c = clips[name]
            end = pose_at(R, c, c["animation_length"])
            start = pose_at(R, c, 0.0)
            for bone in ("arm_l", "arm_r", "forearm_l", "forearm_r"):
                g = lambda p, b=bone: tuple(p.get(b, {}).get("rot", (0, 0, 0)))
                if rot_angle(g(end), g(start)) >= JOIN_LIMIT:
                    out.append("%s: %s ends %.0f degrees away from where it starts" % (name, bone, rot_angle(g(end), g(start))))
                if rot_angle(g(start), g(idle0)) >= 40.0:
                    out.append("%s: %s starts %.0f degrees away from the idle pose" % (name, bone, rot_angle(g(start), g(idle0))))
    return out
