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


def sit_pose(rig, length=3.2):
    """The pose of something sitting on the floor with its legs out in front ('sit' clip): the root lowered so the hips rest on the floor, the thighs forward, the hands in the lap, a slow
    breath and a slow tail-less sway of the head. Every bone is rotated from its rest, so it fits any build."""
    d = rig.d
    sink = -(d.hip_y - d.ld * 0.5)
    tracks = {
        "root": {"position": {0.0: (0, sink, 0), length: (0, sink, 0)}},
        "body": {"rotation": {0.0: (-4, 0, 0), length / 2: (-6, 0, 0), length: (-4, 0, 0)}},
        "head": {"rotation": {0.0: (4, 0, 0), length / 2: (6, 3, 0), length: (4, 0, 0)}},
        "leg_l": {"rotation": {0.0: (84, 0, 6), length: (84, 0, 6)}},
        "leg_r": {"rotation": {0.0: (84, 0, -6), length: (84, 0, -6)}},
        "shin_l": {"rotation": {0.0: (-8, 0, 0), length: (-8, 0, 0)}},
        "shin_r": {"rotation": {0.0: (-8, 0, 0), length: (-8, 0, 0)}},
        "arm_l": {"rotation": {0.0: (36, 0, 4), length: (36, 0, 4)}},
        "arm_r": {"rotation": {0.0: (36, 0, -4), length: (36, 0, -4)}},
        "forearm_l": {"rotation": {0.0: (34, 0, 0), length: (34, 0, 0)}},
        "forearm_r": {"rotation": {0.0: (34, 0, 0), length: (34, 0, 0)}},
    }
    add_clip(rig, "sit", length, tracks, loop=True)


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


def strip_cubes(rig, keep=("head",)):
    """Removes every cube that is not on a bone of `keep` (called from an `extra`): a character that is only a head, such as the Cotton Cloud, whose body is a heap of wool with a face.
    The bones stay (the clips address them), only their cubes go, so the clips and the checks still hold."""
    for name, bone in rig.m.bones.items():
        if name not in keep:
            bone["cubes"] = []


def shift(rig, bone, dx, dy, dz):
    """Adds (dx, dy, dz) pixels to a bone's position in every clip (creating the track when a clip has none): puts a part of a model somewhere the kit's body never has it."""
    for clip in rig.A.clips.values():
        entry = clip["bones"].setdefault(bone, {})
        pos = entry.setdefault("position", {"0.00": [0.0, 0.0, 0.0], _key(clip["animation_length"]): [0.0, 0.0, 0.0]})
        for key in pos:
            pos[key] = [round(pos[key][0] + dx, 4), round(pos[key][1] + dy, 4), round(pos[key][2] + dz, 4)]


def scale_head(rig, sx, sy, sz):
    """The head bone drawn at (sx, sy, sz) times its size in every clip (a flat pillow of a head, a giant's big head ...): a steady scale track, as the kit's own characters do in `post`."""
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (sx, sy, sz), clip["animation_length"]: (sx, sy, sz)})


def _sample(track, t):
    """The value of a clip track {"0.00": [x, y, z], ...} at second t (linear between keys, held outside them), as the game plays it."""
    keys = sorted((float(k), v) for k, v in track.items())
    if t <= keys[0][0]:
        return tuple(keys[0][1])
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 1.0
            return tuple(x + (y - x) * f for x, y in zip(a, b))
    return tuple(keys[-1][1])


def pose_clip(rig, name, src, t, length, adds=None, ramp=0.0, loop=False, sway=None):
    """A clip that HOLDS the pose another clip of the model has at second `t`, so the arms stay solved against the prop by the kit (a staff levelled at a target is the attack's thrust
    frozen at its hit, not an arm guessed by hand). `adds` = {bone: (x, y, z)} degrees added to that pose's rotation (a bow leans the body and turns the arms back by the same angle);
    `ramp` > 0 eases in from the source clip's first pose over that many seconds; `sway` = {bone: (x, y, z)} a slow breath of that amplitude (a loop must not look frozen)."""
    clip = rig.A.clips[src]["bones"]
    adds, sway = adds or {}, sway or {}
    out = {}
    for bone in set(clip) | set(adds) | set(sway):
        if not rig.has(bone):
            continue
        tracks = clip.get(bone, {})
        entry = {}
        for kind in ("rotation", "position", "scale"):
            track = tracks.get(kind)
            if not track and not (kind == "rotation" and (bone in adds or bone in sway)):
                continue
            rest = (1.0, 1.0, 1.0) if kind == "scale" else (0.0, 0.0, 0.0)
            hold = _sample(track, t) if track else rest
            first = _sample(track, 0.0) if track else rest
            if kind == "rotation" and bone in adds:
                hold = tuple(h + a for h, a in zip(hold, adds[bone]))
            if kind == "rotation" and bone in sway:
                keys = {length * k / 4.0: tuple(h + math.sin(2 * math.pi * k / 4.0) * a for h, a in zip(hold, sway[bone])) for k in range(5)}
            elif ramp > 0.0:
                keys = {0.0: first, ramp: hold, length: hold}
            else:
                keys = {0.0: hold, length: hold}
            entry[kind] = {_key(tt): [round(float(v), 4) for v in vec] for tt, vec in keys.items()}
        if entry:
            out[bone] = entry
    rig.A.clips[name] = {"loop": bool(loop), "animation_length": float(length), "bones": out}
