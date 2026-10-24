"""Ultimate Magic: one charge animation per kingdom (docs/magic/ultimate_animations.md), as Blockbench / GeckoLib style player clips.

    python3 tools/gen_ultimate_anims.py            ->  adds (or refreshes) ONLY the ult_* clips in
                                                       src/main/resources/assets/nusmp/animations/player/cast.animation.json
    python3 tools/gen_ultimate_anims.py --check    ->  prints the clips and their sizes, writes nothing

Every other clip of the file stays exactly as it is (rule 2: add the new, keep the old). Same format and conventions as tools/gen_cast_animations.py (read its header):
bones head / body / right_arm / left_arm / right_leg / left_leg, angles are the player model's own rotations in degrees (arm X -90 = straight forward, -180 = straight up;
the LEFT arm swings out with a NEGATIVE Z, the RIGHT arm with a POSITIVE Z; across the chest = left arm +Y, right arm -Y), times in seconds, 20 keys per second
(one key per game tick), positions in pixels, Y up. The client (client/CastAnimClient) applies ARMS and LEGS; the body keys are kept for the Blockbench master and are
ignored by the current client (so the personality of each clip is carried by the arms and legs alone).

Three clips per kingdom, named ult_<kingdom>_<phase>:
  start    the wind-up pose, 10 ticks. It is flagged "loop" ON PURPOSE and is 1.0 s long: the client only blends a new clip out of the previous one when the previous one loops
           (CastAnimClient.receive), and the server swaps to the loop clip at tick 10, so the start clip never wraps; its last pose equals the first pose of the loop clip.
  loop     the long hold (7 to 10 seconds in game): 4 to 6 second loops with several periods inside (breath, drift, a rare accent) so it never looks frozen. First key == last key.
  release  the throw / slam on the first pulse, 1.0 s (the cast's last pulse is at +20 ticks, where the server sends the stop), ends relaxed.
"""
import json
import math
import os
import sys

OUT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "animations", "player", "cast.animation.json"))
TICK = 0.05
BONES = ("body", "left_arm", "right_arm", "left_leg", "right_leg")
KINGDOMS = ("clover", "diamond", "spade", "heart")
PHASES = ("start", "loop", "release")
REST = {"body": (0, 0, 0), "left_arm": (0, 0, -4), "right_arm": (0, 0, 4), "left_leg": (0, 0, 0), "right_leg": (0, 0, 0)}


def ids():
    return ["ult_%s_%s" % (k, p) for k in KINGDOMS for p in PHASES]


# ---------------------------------------------------------------- pose maths
def P(body=None, la=None, ra=None, ll=None, rl=None, lift=0.0):
    """One pose: (x, y, z) per bone, plus the body's position lift in pixels. A bone left out stays at REST."""
    return {"body": body or REST["body"], "left_arm": la or REST["left_arm"], "right_arm": ra or REST["right_arm"],
            "left_leg": ll or REST["left_leg"], "right_leg": rl or REST["right_leg"], "lift": lift}


def mix(a, b, k):
    out = {}
    for bone in BONES:
        out[bone] = tuple(a[bone][i] + (b[bone][i] - a[bone][i]) * k for i in range(3))
    out["lift"] = a["lift"] + (b["lift"] - a["lift"]) * k
    return out


def add(a, bone, d):
    """A copy of the pose with an offset added to one bone."""
    out = dict(a)
    out[bone] = tuple(a[bone][i] + d[i] for i in range(3))
    return out


EASE = {
    "lin": lambda k: k,
    "smooth": lambda k: k * k * (3 - 2 * k),          # slow in, slow out: calm moves
    "out": lambda k: 1 - (1 - k) ** 3,                # fast start, soft landing: a throw
    "in": lambda k: k ** 3,                           # slow start, hard arrival: a wind-up into a strike
}


def story(points):
    """points: [(time, pose, ease)] -> one pose per game tick, each segment eased by the ease of the key it ends at."""
    frames = []
    n = int(round(points[-1][0] / TICK))
    for i in range(n + 1):
        t = i * TICK
        for j in range(1, len(points)):
            if t <= points[j][0] + 1e-9:
                t0, a, _ = points[j - 1]
                t1, b, e = points[j]
                k = 0.0 if t1 - t0 < 1e-9 else (t - t0) / (t1 - t0)
                frames.append((round(t, 2), mix(a, b, EASE[e](min(1.0, max(0.0, k))))))
                break
    return frames


def wave(length, step, fn):
    """A loop sampled every `step` seconds; the last key is the first key (so it closes)."""
    n = int(round(length / step))
    frames = [(round(i * step, 2), fn(i * step)) for i in range(n)]
    frames.append((round(length, 2), frames[0][1]))
    return frames


def s(t, length, cycles, phase=0.0):
    return math.sin(2 * math.pi * (cycles * t / length + phase))


def tent(t, centre, half):
    """A short accent: 0 outside, 1 at the centre."""
    return max(0.0, 1.0 - abs(t - centre) / half)


def thin(track):
    """Drops every key that lies on the straight line between its neighbours (the client interpolates linearly, so the motion is unchanged)."""
    keys = list(track.items())
    kept = [keys[0]]
    for i in range(1, len(keys) - 1):
        (t0, v0), (t1, v1), (t2, v2) = kept[-1], keys[i], keys[i + 1]
        k = (float(t1) - float(t0)) / (float(t2) - float(t0))
        if all(abs(v1[j] - (v0[j] + (v2[j] - v0[j]) * k)) <= 0.06 for j in range(3)):
            continue
        kept.append(keys[i])
    kept.append(keys[-1])
    return dict(kept)


def clip(length, loop, frames):
    bones = {}
    for bone in BONES:
        rot = {}
        pos = {}
        for t, pose in frames:
            rot["%.2f" % t] = [round(v, 1) for v in pose[bone]]
            if bone == "body":
                pos["%.2f" % t] = [0, round(pose["lift"], 1), 0]
        bones[bone] = {"rotation": thin(rot)}
        if bone == "body":
            bones[bone]["position"] = thin(pos)
    return {"loop": loop, "animation_length": length, "bones": bones}


# ================================================================ CLOVER: defiant, arms thrown wide, chest out (Asta, Yami, Mereoleona)
def clover():
    stance = dict(ll=(4, 0, -11), rl=(-2, 0, 11))
    low = P(body=(4, 0, 0), la=(12, 0, -22), ra=(12, 0, 22), ll=(0, 0, -4), rl=(0, 0, 4), lift=-0.5)
    flung = P(body=(-12, 0, 0), la=(-26, 0, -104), ra=(-26, 0, 104), lift=1.0, **stance)
    hold0 = P(body=(-8, 0, 0), la=(-18, 0, -88), ra=(-18, 0, 88), lift=0.5, **stance)

    def loop_pose(t):                                                     # 4.0 s: breath (2 cycles), drift (1 cycle), two roars at 1.0 s (big) and 3.0 s (small)
        L = 4.0
        breath, drift = s(t, L, 2), s(t, L, 1, 0.1)
        roar = 1.0 * tent(t, 1.0, 0.45) + 0.55 * tent(t, 3.0, 0.4)
        p = hold0
        p = add(p, "left_arm", (-5 * breath - 7 * roar, 0, -3 * drift - 9 * roar))
        p = add(p, "right_arm", (-5 * s(t, L, 2, 0.12) - 7 * roar, 0, 3 * drift + 9 * roar))
        p = add(p, "body", (-3 * roar + 1.5 * breath, 0, 0))
        p = add(p, "left_leg", (2 * drift, 0, -1.5 * roar))
        p = add(p, "right_leg", (-2 * drift, 0, 1.5 * roar))
        return dict(p, lift=0.5 + 0.5 * breath + 0.8 * roar)

    base = loop_pose(0.0)                                                 # the start clip ends exactly where the loop begins
    start = clip(1.0, True, story([(0.0, low, "lin"), (0.20, flung, "out"), (0.50, base, "smooth"), (1.0, base, "lin")]))
    loop = clip(4.0, True, wave(4.0, 0.1, loop_pose))
    # release: from the wide stance the fists go overhead, then SLAM down in front (contact at 0.2 s), the weight crashes forward, the arms fall wide and relax
    up = P(body=(-14, 0, 0), la=(-168, 0, -26), ra=(-168, 0, 26), ll=(2, 0, -12), rl=(-2, 0, 12), lift=1.5)
    slam = P(body=(18, 0, 0), la=(-28, 6, -10), ra=(-28, -6, 10), ll=(-22, 0, -10), rl=(18, 0, 10), lift=-2.0)
    rebound = P(body=(10, 0, 0), la=(-12, 0, -40), ra=(-12, 0, 40), ll=(-14, 0, -8), rl=(10, 0, 8), lift=-1.0)
    end = P(body=(2, 0, 0), la=(0, 0, -8), ra=(0, 0, 8), ll=(-2, 0, -2), rl=(2, 0, 2))
    release = clip(1.0, False, story([(0.0, loop_pose(0.0), "lin"), (0.10, up, "out"), (0.20, slam, "in"), (0.35, slam, "lin"), (0.50, rebound, "out"), (1.0, end, "smooth")]))
    return {"ult_clover_start": start, "ult_clover_loop": loop, "ult_clover_release": release}


# ================================================================ DIAMOND: disciplined, feet planted, hands joined in front and slowly lifted, rigid and regal
def diamond():
    feet = dict(ll=(0, 0, -3), rl=(0, 0, 3))
    belt = P(body=(2, 0, 0), la=(-52, 22, 0), ra=(-52, -22, 0), lift=0.0, **feet)
    chest = P(body=(0, 0, 0), la=(-80, 24, 0), ra=(-80, -24, 0), lift=0.0, **feet)

    def loop_pose(t):                                                     # 6.0 s: the joined hands rise slowly to the face and sink again (1 cycle), a faint tremor of power (6 cycles), nothing else moves
        L = 6.0
        rise = 0.5 - 0.5 * math.cos(2 * math.pi * t / L)                  # 0 .. 1 .. 0
        tremor = s(t, L, 6)
        x = -80 - 30 * rise
        y = 24 + 4 * rise
        la = (x + 0.8 * tremor, y, 0.6 * s(t, L, 3))
        ra = (x - 0.8 * tremor, -y, -0.6 * s(t, L, 3))
        return P(body=(0, 0, 0), la=la, ra=ra, ll=(0, 0, -3 + 0.5 * s(t, L, 1)), rl=(0, 0, 3 - 0.5 * s(t, L, 1)), lift=0.2 * rise)

    base = loop_pose(0.0)
    start = clip(1.0, True, story([(0.0, P(), "lin"), (0.25, belt, "smooth"), (0.50, base, "smooth"), (1.0, base, "lin")]))
    loop = clip(6.0, True, wave(6.0, 0.1, loop_pose))
    # release: the hands draw in to the chest, then part and are held out open like a decree, the stance does not move; a slow, dignified lowering
    draw = P(body=(2, 0, 0), la=(-96, 30, 0), ra=(-96, -30, 0), **feet)
    decree = P(body=(-4, 0, 0), la=(-88, -34, -14), ra=(-88, 34, 14), **feet)
    held = P(body=(-4, 0, 0), la=(-84, -38, -20), ra=(-84, 38, 20), **feet)
    end = P(body=(0, 0, 0), la=(0, 0, -6), ra=(0, 0, 6), **feet)
    release = clip(1.0, False, story([(0.0, loop_pose(0.0), "lin"), (0.10, draw, "smooth"), (0.20, decree, "out"), (0.50, held, "lin"), (1.0, end, "smooth")]))
    return {"ult_diamond_start": start, "ult_diamond_loop": loop, "ult_diamond_release": release}


# ================================================================ SPADE: cold and sharp, arms crossed over the chest, then thrust outward like blades
def spade():
    feet = dict(ll=(-8, 0, -2), rl=(10, 0, 3))
    crossed = P(body=(4, 0, 0), la=(-70, 52, 0), ra=(-92, -52, 0), lift=-0.4, **feet)
    over = P(body=(6, 0, 0), la=(-68, 60, 0), ra=(-92, -60, 0), lift=-0.4, **feet)

    def loop_pose(t):                                                     # 5.0 s: almost perfectly still (cold), a very slow drift, and two sharp jolts at 1.5 s and 3.6 s
        L = 5.0
        drift = s(t, L, 1)
        jolt = tent(t, 1.5, 0.1) + 0.7 * tent(t, 3.6, 0.1)
        la = (-70 + 2.6 * drift - 2 * jolt, 52 + 2.0 * drift + 5 * jolt, 0)
        ra = (-92 - 2.6 * drift - 2 * jolt, -52 - 2.0 * drift - 5 * jolt, 0)
        return P(body=(4 + 0.5 * drift, 0, 0), la=la, ra=ra, ll=(-8 + 1.5 * drift, 0, -2), rl=(10 - 1.5 * drift, 0, 3), lift=-0.4)

    base = loop_pose(0.0)
    start = clip(1.0, True, story([(0.0, P(), "lin"), (0.20, over, "out"), (0.30, base, "lin"), (1.0, base, "lin")]))
    loop = clip(5.0, True, wave(5.0, 0.05, loop_pose))
    # release: the arms whip tighter for two ticks, then are thrust outward and forward as two blades (contact at 0.15 s), held cold, then dropped with no ceremony
    tight = P(body=(8, 0, 0), la=(-62, 70, 0), ra=(-96, -70, 0), lift=-0.8, **feet)
    thrust = P(body=(-6, 0, 0), la=(-92, -34, -14), ra=(-92, 34, 14), ll=(-14, 0, -2), rl=(16, 0, 3), lift=0.0)
    hold = P(body=(-6, 0, 0), la=(-94, -38, -16), ra=(-94, 38, 16), ll=(-14, 0, -2), rl=(16, 0, 3), lift=0.0)
    end = P(body=(0, 0, 0), la=(0, 0, -6), ra=(0, 0, 6), ll=(-2, 0, 0), rl=(2, 0, 0))
    release = clip(1.0, False, story([(0.0, loop_pose(0.0), "lin"), (0.10, tight, "in"), (0.15, thrust, "out"), (0.55, hold, "lin"), (0.65, P(body=(0, 0, 0), la=(-30, -10, -8), ra=(-30, 10, 8), **feet), "out"), (1.0, end, "smooth")]))
    return {"ult_spade_start": start, "ult_spade_loop": loop, "ult_spade_release": release}


# ================================================================ HEART: calm and graceful, one hand on the chest, the other opening upward like a flower
def heart():
    feet = dict(ll=(-4, 0, -2), rl=(4, 0, 2))
    chest_hand = (-62, -34, 0)
    opening0 = P(body=(-2, 0, 0), la=(-72, 6, -32), ra=chest_hand, lift=0.0, **feet)

    def loop_pose(t):                                                     # 4.8 s: the open hand blooms up and sinks (1 cycle) with a small circle (2 cycles); the hand on the chest only breathes (3 cycles)
        L = 4.8
        bloom = 0.5 - 0.5 * math.cos(2 * math.pi * t / L)                 # 0 .. 1 .. 0
        la = (-72 - 36 * bloom + 2.5 * s(t, L, 2), 6 - 6 * bloom, -32 - 10 * bloom + 3 * s(t, L, 2, 0.25))
        ra = (chest_hand[0] + 2 * s(t, L, 3), chest_hand[1] + 1.5 * s(t, L, 3, 0.25), 0)
        return P(body=(-2 + 1.2 * s(t, L, 3), 0, 0), la=la, ra=ra, ll=(-4, 0, -2 + 1.2 * s(t, L, 1)), rl=(4, 0, 2 - 1.2 * s(t, L, 1)), lift=0.3 * bloom)

    base = loop_pose(0.0)
    start = clip(1.0, True, story([(0.0, P(), "lin"), (0.30, P(body=(-2, 0, 0), la=(-60, 4, -26), ra=chest_hand, **feet), "smooth"), (0.50, base, "smooth"), (1.0, base, "lin")]))
    loop = clip(4.8, True, wave(4.8, 0.1, loop_pose))
    # release: both hands open outward in an embrace, the arc of the left hand rising first; a long soft landing
    rise = P(body=(-4, 0, 0), la=(-140, 0, -46), ra=(-62, -20, 8), lift=0.6, **feet)
    embrace = P(body=(-6, 0, 0), la=(-40, 0, -64), ra=(-40, 0, 64), lift=0.4, **feet)
    settle = P(body=(-2, 0, 0), la=(-18, 0, -40), ra=(-18, 0, 40), lift=0.0, **feet)
    end = P(body=(0, 0, 0), la=(0, 0, -6), ra=(0, 0, 6), **feet)
    release = clip(1.0, False, story([(0.0, loop_pose(0.0), "lin"), (0.12, rise, "smooth"), (0.22, embrace, "out"), (0.50, embrace, "lin"), (0.70, settle, "smooth"), (1.0, end, "smooth")]))
    return {"ult_heart_start": start, "ult_heart_loop": loop, "ult_heart_release": release}


def all_clips():
    out = {}
    for fn in (clover, diamond, spade, heart):
        out.update(fn())
    assert sorted(out) == sorted(ids()), sorted(out)
    return out


def main():
    clips = all_clips()
    if "--check" in sys.argv:
        for name in ids():
            c = clips[name]
            keys = max(len(b["rotation"]) for b in c["bones"].values())
            print("%-22s loop=%-5s length=%.2fs keys/bone=%d" % (name, c["loop"], c["animation_length"], keys))
        return
    with open(OUT) as f:
        data = json.load(f)
    data["animations"].update(clips)
    with open(OUT, "w", newline="\n") as f:
        json.dump(data, f, indent=1)
        f.write("\n")
    print("added or refreshed", len(clips), "ult_* clips in", OUT)


if __name__ == "__main__":
    main()
