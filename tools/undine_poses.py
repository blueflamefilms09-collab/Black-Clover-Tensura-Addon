"""The pose clips of the Undine (docs/magic/undine.md, "Poses"): welcome, clasp, wave, sorrow, rise, cast, power, flinch and farewell, as animation clips of undine_body.animation.json. The rest pose of the
model is the arms spread, palms up (the owner's second still); all values below are rotation OFFSETS in degrees (x, y, z) from it, positions are in 1/16 block.

Clips that end in a resting pose come in two flavours: the companion rests with her arms relaxed ("relaxed" base: what the idle clip does) and the full form with them spread ("spread" base), so the
clip name has a "_f" for the full form (client: UndinePose#clip). The clips that loop (welcome, clasp, wave, sorrow) do not depend on the form.
"""
import math

RELAXED = {"arm": lambda s: (0, 0, -s * 17.0), "fore": lambda s: (-24.0, 0, -s * 6.0), "hand": lambda s: (8.0, 0, 0)}
SPREAD = {"arm": lambda s: (0, 0, 0), "fore": lambda s: (0, 0, 0), "hand": lambda s: (0, 0, 0)}
TAG = ((1, "l"), (-1, "r"))


def _add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def _loop_track(T, fn, steps=16):
    return {T * i / steps: fn(i / steps) for i in range(steps + 1)}


def pose_clips(max_ribbons, sectors):
    out = {}
    TAU = math.tau

    def hair(T, ts, amp_fn, loop_sway=0.0, sway_len=None):
        """per-ribbon tracks: amp_fn(k) -> (rx, ry, rz) amplitude of key k (before the per ribbon / per side pattern); plus an optional gentle sway on top for loops"""
        tr = {}
        for idx in range(max_ribbons):
            for side, sg in (("l", 1), ("r", -1)):
                fr = {}
                for k, t in enumerate(ts):
                    a = amp_fn(k)
                    v = (a[0] * math.sin(idx), sg * a[1], sg * a[2])
                    if loop_sway:
                        ph = (idx * 0.137) % 1.0
                        w = math.sin((t / T + ph + (0.08 if side == "r" else 0)) * TAU) * loop_sway
                        v = (v[0] + 2.2 * w, v[1] + w, v[2] + 1.1 * w)
                    fr[t] = v
                tr["hr_%s%d" % (side, idx)] = {"rotation": fr}
        return tr

    def skirt(T, ts, amp_fn):
        tr = {}
        for k in range(sectors):
            az = (k + 0.5) / sectors * TAU
            tx, tz = math.cos(az), -math.sin(az)
            fr = {t: (amp_fn(i, t) * tx, 0.0, amp_fn(i, t) * tz) for i, t in enumerate(ts)}
            tr["sk%d" % k] = {"rotation": fr}
        return tr

    def sk_wave(T, ts, amp):
        """a flowing skirt for a loop: each strip swings with its own phase"""
        tr = {}
        for k in range(sectors):
            az = (k + 0.5) / sectors * TAU
            tx, tz = math.cos(az), -math.sin(az)
            fr = {}
            for t in ts:
                a = amp * math.sin((t / T + k / sectors) * TAU)
                fr[t] = (a * tx, 0.0, a * tz)
            tr["sk%d" % k] = {"rotation": fr}
        return tr

    # ============================================================ welcome: arms open (the second still), a soft tilt of the head, hair and gown flowing; loops
    T = 3.2
    ts = [T * i / 16 for i in range(17)]
    b = {"float": {"position": {t: (0, 0.6 * math.sin(t / T * TAU), 0) for t in ts}}, "head": {"rotation": {t: (-2 + 1.5 * math.sin(t / T * TAU), 0, 4 * math.sin(t / T * TAU + 1.0)) for t in ts}},
         "torso": {"rotation": {t: (0.8 * math.sin(t / T * TAU), 0, 0) for t in ts}}}
    for s, tag in TAG:
        b["arm_" + tag] = {"rotation": {t: (0, 0, s * (-3.0 + 2.0 * math.sin(t / T * TAU + 0.5 * s))) for t in ts}}
        b["fore_" + tag] = {"rotation": {t: (-8.0 + 3 * math.sin(t / T * TAU), 0, 0) for t in ts}}
        b["hand_" + tag] = {"rotation": {t: (-6.0 + 3 * math.sin(t / T * TAU + 1), 0, 0) for t in ts}}
    b.update(hair(T, ts, lambda k: (0, 0, 0), loop_sway=3.6))
    b.update(sk_wave(T, ts, 3.0))
    out["welcome"] = {"loop": True, "length": T, "bones": b}

    # ============================================================ clasp: the hands together in front of her, a calm breath; loops
    T = 3.6
    ts = [T * i / 16 for i in range(17)]
    b = {"float": {"position": {t: (0, 0.7 * math.sin(t / T * TAU), 0) for t in ts}}, "head": {"rotation": {t: (4 + 1.2 * math.sin(t / T * TAU), 1.5 * math.sin(t / T * TAU), 0) for t in ts}},
         "torso": {"rotation": {t: (1.5 + 0.8 * math.sin(t / T * TAU), 0, 0) for t in ts}}}
    for s, tag in TAG:
        b["arm_" + tag] = {"rotation": {t: (34.0, 0, -s * 50.0) for t in ts}}
        b["fore_" + tag] = {"rotation": {t: (-72.0 + 2 * math.sin(t / T * TAU), -s * 62.0, 0) for t in ts}}
        b["hand_" + tag] = {"rotation": {t: (-12.0, 0, 0) for t in ts}}
    b.update(hair(T, ts, lambda k: (0, 0, 0), loop_sway=2.4))
    b.update(sk_wave(T, ts, 2.2))
    out["clasp"] = {"loop": True, "length": T, "bones": b}

    # ============================================================ wave: one hand raised, the wrist waving; loops (the right arm; the left rests)
    T = 2.4
    ts = [T * i / 16 for i in range(17)]
    b = {"float": {"position": {t: (0, 0.8 * math.sin(t / T * TAU), 0) for t in ts}}, "head": {"rotation": {t: (0, 0, -6 + 1.5 * math.sin(t / T * 2 * TAU)) for t in ts}}}
    s, tag = -1, "r"
    b["arm_r"] = {"rotation": {t: (0, 0, -s * -92.0 + 0) for t in ts}}
    b["fore_r"] = {"rotation": {t: (-10.0, 0, s * (14.0 * math.sin(t / T * 2 * TAU))) for t in ts}}
    b["hand_r"] = {"rotation": {t: (0, 0, s * (24.0 * math.sin(t / T * 2 * TAU + 0.8))) for t in ts}}
    b["arm_l"] = {"rotation": {t: (0, 0, -17.0 + 1.0 * math.sin(t / T * TAU)) for t in ts}}
    b["fore_l"] = {"rotation": {t: (-24.0, 0, -6.0) for t in ts}}
    b["hand_l"] = {"rotation": {t: (8.0, 0, 0) for t in ts}}
    b.update(hair(T, ts, lambda k: (0, 0, 0), loop_sway=3.0))
    b.update(sk_wave(T, ts, 2.6))
    out["wave"] = {"loop": True, "length": T, "bones": b}

    # ============================================================ sorrow: head bowed, the hands low and together, hair and gown drooping and slow; loops
    T = 4.0
    ts = [T * i / 16 for i in range(17)]
    b = {"float": {"position": {t: (0, -1.2 + 0.5 * math.sin(t / T * TAU), 0) for t in ts}}, "head": {"rotation": {t: (17 + 1.5 * math.sin(t / T * TAU), 0, 3) for t in ts}},
         "neck": {"rotation": {t: (6, 0, 0) for t in ts}}, "torso": {"rotation": {t: (4 + 0.8 * math.sin(t / T * TAU), 0, 0) for t in ts}}}
    for s, tag in TAG:
        b["arm_" + tag] = {"rotation": {t: (14.0, 0, -s * 40.0) for t in ts}}
        b["fore_" + tag] = {"rotation": {t: (-58.0, -s * 66.0, 0) for t in ts}}
        b["hand_" + tag] = {"rotation": {t: (-14.0, 0, 0) for t in ts}}
    b.update(hair(T, ts, lambda k: (7.0, 0, 0), loop_sway=1.6))
    b.update(sk_wave(T, ts, 1.2))
    out["sorrow"] = {"loop": True, "length": T, "bones": b}

    # ============================================================ the event poses that end in a resting pose, per form
    for full, suffix, base in ((False, "", RELAXED), (True, "_f", SPREAD)):
        # ---- rise: out of the waves (the gown and body rise from below, the arms open, the hair is flung up)
        T = 1.2
        ts = [0.0, 0.5, 0.8, T]
        up = -34.0 if full else -14.0
        b = {"float": {"position": dict(zip(ts, [(0, up, 0), (0, up * 0.25, 0), (0, 2.0, 0), (0, 0, 0)]))}}
        b["head"] = {"rotation": dict(zip(ts, [(10.0, 0, 0), (2.0, 0, 0), (-6.0, 0, 0), (0, 0, 0)]))}
        for s, tag in TAG:
            b["arm_" + tag] = {"rotation": dict(zip(ts, [_add(base["arm"](s), (0, 0, -s * 14)), (0, 0, -s * 6.0), (0, 0, s * 5.0), base["arm"](s)]))}
            b["fore_" + tag] = {"rotation": dict(zip(ts, [_add(base["fore"](s), (-10, 0, 0)), (-6.0, 0, 0), (-2.0, 0, 0), base["fore"](s)]))}
            b["hand_" + tag] = {"rotation": dict(zip(ts, [base["hand"](s), (0, 0, 0), (-4.0, 0, 0), base["hand"](s)]))}
        b.update(hair(T, ts, lambda k: (0, 0, 0) if k == 0 else (-7.0, 10.0, 9.0) if k == 2 else (4.0, 5.0, 4.0) if k == 1 else (0, 0, 0)))
        b.update(skirt(T, ts, lambda i, t: [-4.0, 4.0, 9.0, 0.0][i]))
        out["rise" + suffix] = {"loop": "hold", "length": T, "bones": b}

        # ---- cast: the arms spread, palms up, hair flaring and the gown swelling; back to rest at the end
        T = 1.2
        ts = [0.0, 0.25, 0.6, 0.85, T]
        b = {"float": {"position": dict(zip(ts, [(0, 0, 0), (0, -0.6, 0), (0, 1.8, 0), (0, 1.0, 0), (0, 0, 0)]))},
             "head": {"rotation": dict(zip(ts, [(0, 0, 0), (4.0, 0, 0), (-5.0, 0, 0), (-3.0, 0, 0), (0, 0, 0)]))}}
        for s, tag in TAG:
            b["arm_" + tag] = {"rotation": dict(zip(ts, [base["arm"](s), _add(base["arm"](s), (0, 0, -s * 5.0)), (0, 0, s * 4.0), (0, 0, s * 1.0), base["arm"](s)]))}
            b["fore_" + tag] = {"rotation": dict(zip(ts, [base["fore"](s), _add(base["fore"](s), (-8, 0, 0)), (-4.0, 0, s * 3.0), (0, 0, 0), base["fore"](s)]))}
            b["hand_" + tag] = {"rotation": dict(zip(ts, [base["hand"](s), (14.0, 0, 0), (-6.0, 0, 0), (0, 0, 0), base["hand"](s)]))}
            b["band_" + tag] = {"scale": dict(zip(ts, [(1, 1, 1), (1, 1, 1), (1.25, 1.4, 1.25), (1.1, 1.15, 1.1), (1, 1, 1)]))}
        b.update(hair(T, ts, lambda k: [(0, 0, 0), (0, -2.0, 0), (6.0, 8.0, 7.0), (3.0, 4.0, 3.0), (0, 0, 0)][k]))
        b.update(skirt(T, ts, lambda i, t: [0.0, -1.5, 6.0, 3.0, 0.0][i]))
        out["cast" + suffix] = {"loop": "hold", "length": T, "bones": b}

        # ---- power: the swell: the arms flung wide, the head back, the hair and gown flaring hard; holds, then settles
        T = 2.0
        ts = [0.0, 0.3, 0.55, 1.4, T]
        b = {"float": {"position": dict(zip(ts, [(0, 0, 0), (0, 3.0, 0), (0, 4.0, 0), (0, 2.5, 0), (0, 0, 0)])),
                       "scale": dict(zip(ts, [(1, 1, 1), (1.0, 1.02, 1.0), (1.05, 1.04, 1.05), (1.03, 1.02, 1.03), (1, 1, 1)]))},
             "head": {"rotation": dict(zip(ts, [(0, 0, 0), (-6.0, 0, 0), (-10.0, 0, 0), (-8.0, 0, 0), (0, 0, 0)]))},
             "torso": {"rotation": dict(zip(ts, [(0, 0, 0), (-3.0, 0, 0), (-5.0, 0, 0), (-4.0, 0, 0), (0, 0, 0)]))}}
        for s, tag in TAG:
            b["arm_" + tag] = {"rotation": dict(zip(ts, [base["arm"](s), (0, 0, s * 20.0), (0, 0, s * 30.0), (0, 0, s * 28.0), base["arm"](s)]))}
            b["fore_" + tag] = {"rotation": dict(zip(ts, [base["fore"](s), (-4.0, 0, s * 4.0), (0, 0, s * 8.0), (-3.0, 0, s * 6.0), base["fore"](s)]))}
            b["hand_" + tag] = {"rotation": dict(zip(ts, [base["hand"](s), (-12.0, 0, 0), (-18.0, 0, 0), (-14.0, 0, 0), base["hand"](s)]))}
            b["band_" + tag] = {"scale": dict(zip(ts, [(1, 1, 1), (1.2, 1.3, 1.2), (1.4, 1.6, 1.4), (1.3, 1.45, 1.3), (1, 1, 1)]))}
        b.update(hair(T, ts, lambda k: [(0, 0, 0), (9.0, 14.0, 12.0), (14.0, 20.0, 17.0), (11.0, 16.0, 13.0), (0, 0, 0)][k], loop_sway=0.0))
        b.update(skirt(T, ts, lambda i, t: [0.0, 8.0, 13.0, 10.0, 0.0][i]))
        out["power" + suffix] = {"loop": "hold", "length": T, "bones": b}

        # ---- flinch: a quick recoil: the body leans back, the arms pull in, the hair whips
        T = 0.5
        ts = [0.0, 0.1, 0.25, T]
        b = {"torso": {"rotation": dict(zip(ts, [(0, 0, 0), (-9.0, 0, 0), (-3.0, 0, 0), (0, 0, 0)]))},
             "head": {"rotation": dict(zip(ts, [(0, 0, 0), (-12.0, 0, 3.0), (-4.0, 0, 1.0), (0, 0, 0)]))},
             "float": {"position": dict(zip(ts, [(0, 0, 0), (0, 0.8, 0), (0, 0.2, 0), (0, 0, 0)]))}}
        for s, tag in TAG:
            b["arm_" + tag] = {"rotation": dict(zip(ts, [base["arm"](s), _add(base["arm"](s), (0, 0, -s * 14)), _add(base["arm"](s), (0, 0, -s * 5)), base["arm"](s)]))}
            b["fore_" + tag] = {"rotation": dict(zip(ts, [base["fore"](s), _add(base["fore"](s), (-22, 0, 0)), _add(base["fore"](s), (-8, 0, 0)), base["fore"](s)]))}
            b["hand_" + tag] = {"rotation": dict(zip(ts, [base["hand"](s), (14.0, 0, 0), (4.0, 0, 0), base["hand"](s)]))}
        b.update(hair(T, ts, lambda k: [(0, 0, 0), (12.0, 6.0, 5.0), (4.0, 2.0, 2.0), (0, 0, 0)][k]))
        b.update(skirt(T, ts, lambda i, t: [0.0, -5.0, -2.0, 0.0][i]))
        out["flinch" + suffix] = {"loop": "hold", "length": T, "bones": b}

        # ---- farewell: the hands come to the chest, the head bows, and she sinks into the water while the gown collapses
        T = 1.4
        ts = [0.0, 0.5, 0.9, T]
        b = {"float": {"position": dict(zip(ts, [(0, 0, 0), (0, 0.5, 0), (0, -6.0, 0), (0, -16.0 if full else -10.0, 0)])),
                       "scale": dict(zip(ts, [(1, 1, 1), (1, 1, 1), (0.97, 0.8, 0.97), (0.9, 0.45, 0.9)]))},
             "head": {"rotation": dict(zip(ts, [(0, 0, 0), (9.0, 0, 0), (12.0, 0, 0), (14.0, 0, 0)]))}}
        for s, tag in TAG:
            b["arm_" + tag] = {"rotation": dict(zip(ts, [base["arm"](s), (14.0, 0, -s * 34.0), (18.0, 0, -s * 36.0), (18.0, 0, -s * 36.0)]))}
            b["fore_" + tag] = {"rotation": dict(zip(ts, [base["fore"](s), (-58.0, s * 28.0, 0), (-62.0, s * 32.0, 0), (-62.0, s * 32.0, 0)]))}
            b["hand_" + tag] = {"rotation": dict(zip(ts, [base["hand"](s), (-12.0, 0, 0), (-12.0, 0, 0), (-12.0, 0, 0)]))}
        b.update(hair(T, ts, lambda k: [(0, 0, 0), (4.0, 0, 0), (8.0, 0, 0), (10.0, 0, 0)][k]))
        b.update(skirt(T, ts, lambda i, t: [0.0, 1.0, -2.0, -4.0][i]))
        out["farewell" + suffix] = {"loop": "hold", "length": T, "bones": b}
    return out
