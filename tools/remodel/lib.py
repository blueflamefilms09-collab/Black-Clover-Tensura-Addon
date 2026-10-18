"""Why: the small helpers the remodelled mobs share (docs/arc/remodel.md), kept out of the character files because a character file is never edited by another builder and
must not copy another's code. It holds what the art kit does not: extra named clips (a sleeping pose, the God of Order's read / beam / bow), a squash of a whole model (the
slime's body), and the deep merge that skin variants use. Nothing here touches the kit: it only calls its public hooks (`rig.A.clips`, `rig.bone`, `rig.animate`).
"""
import copy
import math


def overlay(base, over):
    """`base` with `over` laid over it, dicts merged key by key, anything else replaced (a copy; neither argument changes). The skins of one body are specs built this way."""
    out = copy.deepcopy(base)
    for k, v in over.items():
        if isinstance(v, dict) and isinstance(out.get(k), dict):
            out[k] = overlay(out[k], v)
        else:
            out[k] = copy.deepcopy(v)
    return out


def _key(t):
    return "%.2f" % t


def add_clip(rig, name, length, tracks, loop=False):
    """Adds a clip the kit does not make. `tracks` is {bone: {"rotation": {seconds: (x, y, z)}, "position": {...}, "scale": {...}}} (degrees, pixels, factors, as in the files).
    A bone the model lacks is skipped, so one pose table fits a model with or without wings, a tail or a cape. The clip name is then playable by a renderer that asks for it."""
    bones = {}
    for bone, tr in tracks.items():
        if not rig.has(bone):
            continue
        entry = {}
        for kind, frames in tr.items():
            entry[kind] = {_key(t): [round(float(v), 4) for v in vec] for t, vec in sorted(frames.items())}
        bones[bone] = entry
    rig.A.clips[name] = {"loop": bool(loop), "animation_length": float(length), "bones": bones}


def hold(length, vec, loop=False):
    """A track that goes from rest (0) to `vec` over the first third of the clip and holds it (a pose, not a motion)."""
    a = length * 0.3
    return {0.0: (0, 0, 0), a: vec, length: vec} if not loop else {0.0: vec, length: vec}


def breathe(length, axis, amp, steps=4):
    """A slow sine on one axis of a bone, closing on its first key (for a held pose that should not look frozen)."""
    out = {}
    for k in range(steps + 1):
        v = [0.0, 0.0, 0.0]
        v[axis] = amp * math.sin(2 * math.pi * k / steps)
        out[length * k / steps] = tuple(v)
    return out


def sleep_pose(rig, length=3.2):
    """The pose a sleeping predator takes ('sleep' clip): the body folded forward and down onto bent legs, the head on the arms, a slow breath. Every bone is rotated from its
    rest, so it fits any build; the root is lowered by a fraction of the leg length so the feet stay on the floor."""
    d = rig.d
    sink = -d.leg_len * 0.62
    tracks = {
        "root": {"position": {0.0: (0, sink, 0), length: (0, sink, 0)}},
        "body": {"rotation": {0.0: (-52, 0, 0), length / 2: (-55, 0, 0), length: (-52, 0, 0)}},
        "head": {"rotation": {0.0: (-30, 0, 8), length / 2: (-34, 0, 8), length: (-30, 0, 8)}},
        "leg_l": {"rotation": {0.0: (-62, 0, 6), length: (-62, 0, 6)}},
        "leg_r": {"rotation": {0.0: (-62, 0, -6), length: (-62, 0, -6)}},
        "shin_l": {"rotation": {0.0: (112, 0, 0), length: (112, 0, 0)}},
        "shin_r": {"rotation": {0.0: (112, 0, 0), length: (112, 0, 0)}},
        "arm_l": {"rotation": {0.0: (48, 0, 14), length: (48, 0, 14)}},
        "arm_r": {"rotation": {0.0: (48, 0, -14), length: (48, 0, -14)}},
        "forearm_l": {"rotation": {0.0: (50, 0, 0), length: (50, 0, 0)}},
        "forearm_r": {"rotation": {0.0: (50, 0, 0), length: (50, 0, 0)}},
    }
    add_clip(rig, "sleep", length, tracks, loop=True)


def scarf(kit, rig, color, length=9.0, width=3.0, salt=70, ring=True):
    """A scarf: a ring round the neck (`ring`) and two tails that hang down the back and swing in every clip. The kit's `neck.scarf` is a thin line, which a red scarf that is part of a
    character's silhouette (Dante's) is not. Placed from the measured body (`rig.d`)."""
    d = rig.d
    cloth = kit.mat("cloth", kit.rgb(color), salt, seam=False)
    if ring:
        nw = max(5.0, d.tw * 0.62)
        rig.box("head", (-nw / 2, d.sh_y - 0.9, -d.td / 2 - 0.5), (nw, 2.8, d.td + 1.0), cloth)
    rig.bone("scarf", "body", (0.0, d.sh_y - 1.0, d.td / 2 - 0.4))
    rig.box("scarf", (-width * 0.9, d.sh_y - 1.0 - length, d.td / 2 - 0.5), (width, length, 0.9), cloth)
    rig.box("scarf", (width * 0.1, d.sh_y - 1.0 - length * 0.8, d.td / 2 - 0.5), (width, length * 0.8, 0.9), cloth)
    rig.animate("scarf", sway=1.2)
