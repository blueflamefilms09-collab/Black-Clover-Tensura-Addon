#!/usr/bin/env python3
"""Why: Better Combat plays a weapon's swing from a Player Animator "emote" JSON found under
assets/<namespace>/player_animations/<name>.json (1.38.1: the folder is player_animations, the Player Animator registry that Better Combat resolves `animation` ids through; 1.38.0 wrote attack_animations, which nothing loads, so swings had a pose but no animation). Writing keyframes as raw radians in JSON is unreadable and
easy to get subtly wrong (a tick out of order, a degree typed as a radian), so every animation is a small
table here: (tick, easing, {part: {axis: value}}), angles in DEGREES, offsets in pixels. The generator converts to the
file format (radians, degrees=false), checks that ticks ascend and that begin <= end <= stop, and writes one
standalone file per animation (a file that fails to parse is logged and skipped by Better Combat, it never takes another
animation or the game down).

Usage:  python tools/bettercombat/gen_animations.py [--check] [--list]
  --check  write nothing; exit 1 when a file on disk differs from what the tables produce
  --list   print the animation names

Schema followed (from memory of Better Combat 2.x / Player Animator, NOT loadable here, see docs/better_combat.md):
  {"format_version":"1.0.0","name":..,"author":..,"emote":{"isLoop":false,"returnTick":0,"beginTick":0,"endTick":E,
   "stopTick":S,"degrees":false,"moves":[{"tick":T,"easing":"EASEOUTSINE","turn":0,
   "rightArm":{"x":..,"y":..,"z":..,"pitch":..,"yaw":..,"roll":..}, ...}]}}
Only the parts head/torso/rightArm/leftArm/rightLeg/leftLeg are used. Sign conventions (pitch negative = limb swings
forward/up, yaw positive = limb swings towards the body's left) follow Minecraft's model rotations and are the part
most worth checking in game.
"""
import json, math, os, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "player_animations")
AUTHOR = "Multiverse of Anime"
PARTS = ("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg")
EASINGS = {"LINEAR", "CONSTANT", "EASEINSINE", "EASEOUTSINE", "EASEINOUTSINE", "EASEINQUAD", "EASEOUTQUAD",
           "EASEINOUTQUAD", "EASEINCUBIC", "EASEOUTCUBIC", "EASEINOUTCUBIC"}
ANGLE_KEYS = ("pitch", "yaw", "roll")
OFFSET_KEYS = ("x", "y", "z")

def P(**kw): return kw   # shorthand for one part's values

# name -> (endTick, stopTick, [(tick, easing, {part: P(...)})])
# Every animation is described as ready pose -> wind-up -> strike -> follow-through -> recover.
ANIMS = {
    # two-handed overhead slam: blade raised behind the head, torso folds forward on the strike
    "greatsword_slam": (16, 20, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-55, yaw=8), "leftArm": P(pitch=-60, yaw=-8), "torso": P(pitch=0)}),
        (4, "EASEOUTSINE", {"rightArm": P(pitch=-165, yaw=6, roll=6), "leftArm": P(pitch=-160, yaw=-6, roll=-6),
                            "torso": P(pitch=-12), "head": P(pitch=-8), "rightLeg": P(pitch=8), "leftLeg": P(pitch=-8)}),
        (6, "LINEAR", {"rightArm": P(pitch=-172, yaw=6, roll=6), "leftArm": P(pitch=-168, yaw=-6, roll=-6),
                       "torso": P(pitch=-15), "head": P(pitch=-8), "rightLeg": P(pitch=8), "leftLeg": P(pitch=-8)}),
        (9, "EASEINQUAD", {"rightArm": P(pitch=-45, yaw=4), "leftArm": P(pitch=-50, yaw=-4),
                           "torso": P(pitch=28), "head": P(pitch=12), "rightLeg": P(pitch=-22), "leftLeg": P(pitch=14)}),
        (12, "EASEOUTCUBIC", {"rightArm": P(pitch=-40, yaw=4), "leftArm": P(pitch=-45, yaw=-4),
                              "torso": P(pitch=30), "head": P(pitch=14), "rightLeg": P(pitch=-24), "leftLeg": P(pitch=14)}),
        (16, "EASEINOUTSINE", {"rightArm": P(pitch=-30), "leftArm": P(pitch=-35), "torso": P(pitch=10), "head": P(pitch=4)}),
    ]),
    # wide horizontal cut right to left, shoulders drive the blade round
    "greatsword_sweep": (13, 17, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-60, yaw=0), "leftArm": P(pitch=-65, yaw=0)}),
        (4, "EASEOUTSINE", {"rightArm": P(pitch=-72, yaw=-58, roll=8), "leftArm": P(pitch=-78, yaw=-50),
                            "torso": P(yaw=-42), "head": P(yaw=22), "rightLeg": P(pitch=10), "leftLeg": P(pitch=-10)}),
        (5, "LINEAR", {"rightArm": P(pitch=-72, yaw=-60, roll=8), "leftArm": P(pitch=-78, yaw=-52),
                       "torso": P(yaw=-44), "head": P(yaw=22), "rightLeg": P(pitch=10), "leftLeg": P(pitch=-10)}),
        (8, "EASEINQUAD", {"rightArm": P(pitch=-80, yaw=62), "leftArm": P(pitch=-82, yaw=70),
                           "torso": P(yaw=52), "head": P(yaw=-26), "rightLeg": P(pitch=-14), "leftLeg": P(pitch=10)}),
        (10, "EASEOUTCUBIC", {"rightArm": P(pitch=-78, yaw=70), "leftArm": P(pitch=-80, yaw=76),
                              "torso": P(yaw=58), "head": P(yaw=-28), "rightLeg": P(pitch=-14), "leftLeg": P(pitch=10)}),
        (13, "EASEINOUTSINE", {"rightArm": P(pitch=-40, yaw=20), "leftArm": P(pitch=-45, yaw=24), "torso": P(yaw=15)}),
    ]),
    # the mirror of the sweep: left to right, so a combo flows back the way it came
    "greatsword_sweep_back": (13, 17, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-80, yaw=62), "leftArm": P(pitch=-82, yaw=70), "torso": P(yaw=40)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-85, yaw=72, roll=-6), "leftArm": P(pitch=-88, yaw=78),
                            "torso": P(yaw=56), "head": P(yaw=-26), "rightLeg": P(pitch=-10), "leftLeg": P(pitch=10)}),
        (7, "EASEINQUAD", {"rightArm": P(pitch=-70, yaw=-56), "leftArm": P(pitch=-76, yaw=-48),
                           "torso": P(yaw=-46), "head": P(yaw=24), "rightLeg": P(pitch=10), "leftLeg": P(pitch=-12)}),
        (9, "EASEOUTCUBIC", {"rightArm": P(pitch=-68, yaw=-64), "leftArm": P(pitch=-74, yaw=-56),
                             "torso": P(yaw=-52), "head": P(yaw=26), "rightLeg": P(pitch=10), "leftLeg": P(pitch=-12)}),
        (13, "EASEINOUTSINE", {"rightArm": P(pitch=-40, yaw=-10), "leftArm": P(pitch=-45, yaw=-12), "torso": P(yaw=-8)}),
    ]),
    # low draw: blade pulled from the hip and cut across and up, torso dipped
    "katana_draw": (10, 14, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-15, yaw=0), "leftArm": P(pitch=-20, yaw=-10)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-14, yaw=38, roll=10), "leftArm": P(pitch=-30, yaw=-26, roll=-10),
                            "torso": P(pitch=14, yaw=34), "head": P(yaw=-18), "rightLeg": P(pitch=-12), "leftLeg": P(pitch=14)}),
        (4, "LINEAR", {"rightArm": P(pitch=-14, yaw=40, roll=10), "leftArm": P(pitch=-30, yaw=-28, roll=-10),
                       "torso": P(pitch=14, yaw=36), "head": P(yaw=-18), "rightLeg": P(pitch=-12), "leftLeg": P(pitch=14)}),
        (6, "EASEINQUAD", {"rightArm": P(pitch=-92, yaw=-48), "leftArm": P(pitch=-70, yaw=-30),
                           "torso": P(pitch=6, yaw=-40), "head": P(yaw=20), "rightLeg": P(pitch=-18), "leftLeg": P(pitch=14)}),
        (8, "EASEOUTCUBIC", {"rightArm": P(pitch=-100, yaw=-58), "leftArm": P(pitch=-72, yaw=-34),
                             "torso": P(pitch=4, yaw=-46), "head": P(yaw=22)}),
        (10, "EASEINOUTSINE", {"rightArm": P(pitch=-50, yaw=-20), "leftArm": P(pitch=-40, yaw=-10), "torso": P(yaw=-15)}),
    ]),
    # rising cut: blade starts low by the knee and climbs through the target
    "katana_rise": (10, 14, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-45, yaw=-25), "leftArm": P(pitch=-50, yaw=-15)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-12, yaw=-30, roll=8), "leftArm": P(pitch=-22, yaw=-22),
                            "torso": P(pitch=16, yaw=-22), "head": P(pitch=6), "rightLeg": P(pitch=-14), "leftLeg": P(pitch=12)}),
        (6, "EASEINQUAD", {"rightArm": P(pitch=-150, yaw=12), "leftArm": P(pitch=-140, yaw=6),
                           "torso": P(pitch=-12, yaw=18), "head": P(pitch=-8), "rightLeg": P(pitch=-8), "leftLeg": P(pitch=10)}),
        (8, "EASEOUTCUBIC", {"rightArm": P(pitch=-160, yaw=14), "leftArm": P(pitch=-148, yaw=8),
                             "torso": P(pitch=-14, yaw=20), "head": P(pitch=-10)}),
        (10, "EASEINOUTSINE", {"rightArm": P(pitch=-80, yaw=0), "leftArm": P(pitch=-70, yaw=0), "torso": P(pitch=0)}),
    ]),
    # the katana thrust that ends the combo: pull back to the hip, step in, drive the point straight out
    "katana_thrust": (9, 13, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-60), "leftArm": P(pitch=-65)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-48, yaw=38, roll=6), "leftArm": P(pitch=-56, yaw=16),
                            "torso": P(yaw=28), "head": P(yaw=-14), "rightLeg": P(pitch=10), "leftLeg": P(pitch=-8)}),
        (5, "EASEINQUAD", {"rightArm": P(pitch=-96, yaw=-4), "leftArm": P(pitch=-92, yaw=-2),
                           "torso": P(pitch=10, yaw=-14), "head": P(yaw=8), "rightLeg": P(pitch=-34), "leftLeg": P(pitch=22)}),
        (7, "EASEOUTCUBIC", {"rightArm": P(pitch=-98, yaw=-5), "leftArm": P(pitch=-94, yaw=-3),
                             "torso": P(pitch=12, yaw=-16), "rightLeg": P(pitch=-36), "leftLeg": P(pitch=24)}),
        (9, "EASEINOUTSINE", {"rightArm": P(pitch=-60), "leftArm": P(pitch=-65), "torso": P(pitch=0)}),
    ]),
    # one-handed lunge for rapier and dagger stabs: retract, step in, point out
    "rapier_lunge": (8, 11, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-70, yaw=0), "leftArm": P(pitch=10, roll=-12)}),
        (2, "EASEOUTSINE", {"rightArm": P(pitch=-52, yaw=22, roll=4), "leftArm": P(pitch=20, roll=-20),
                            "torso": P(yaw=14), "rightLeg": P(pitch=6)}),
        (4, "EASEOUTCUBIC", {"rightArm": P(pitch=-96, yaw=-2), "leftArm": P(pitch=34, roll=-30),
                             "torso": P(pitch=14, yaw=-16), "head": P(yaw=10), "rightLeg": P(pitch=-40), "leftLeg": P(pitch=26)}),
        (6, "LINEAR", {"rightArm": P(pitch=-98, yaw=-2), "leftArm": P(pitch=36, roll=-32),
                       "torso": P(pitch=15, yaw=-17), "rightLeg": P(pitch=-42), "leftLeg": P(pitch=28)}),
        (8, "EASEINOUTSINE", {"rightArm": P(pitch=-70), "leftArm": P(pitch=10, roll=-12), "torso": P(pitch=0)}),
    ]),
    # one-handed overhead chop for axes and cleavers
    "axe_chop": (11, 15, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-60), "leftArm": P(pitch=8, roll=-10)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-158, yaw=6, roll=8), "leftArm": P(pitch=12, roll=-18),
                            "torso": P(pitch=-10, yaw=10), "head": P(pitch=-6), "rightLeg": P(pitch=6), "leftLeg": P(pitch=-6)}),
        (5, "LINEAR", {"rightArm": P(pitch=-165, yaw=6, roll=8), "leftArm": P(pitch=12, roll=-18),
                       "torso": P(pitch=-12, yaw=10), "head": P(pitch=-6)}),
        (7, "EASEINQUAD", {"rightArm": P(pitch=-38, yaw=-6), "leftArm": P(pitch=20, roll=-14),
                           "torso": P(pitch=24, yaw=-10), "head": P(pitch=12), "rightLeg": P(pitch=-20), "leftLeg": P(pitch=12)}),
        (9, "EASEOUTCUBIC", {"rightArm": P(pitch=-32, yaw=-6), "leftArm": P(pitch=22, roll=-14),
                             "torso": P(pitch=26, yaw=-10), "head": P(pitch=14)}),
        (11, "EASEINOUTSINE", {"rightArm": P(pitch=-30), "leftArm": P(pitch=8), "torso": P(pitch=8), "head": P(pitch=3)}),
    ]),
    # crossing dual slash: both blades start wide and scissor through the target
    "twin_cross": (11, 15, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-62, yaw=-6), "leftArm": P(pitch=-62, yaw=6)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-70, yaw=-60, roll=10), "leftArm": P(pitch=-70, yaw=60, roll=-10),
                            "torso": P(pitch=4), "head": P(pitch=-2), "rightLeg": P(pitch=8), "leftLeg": P(pitch=-8)}),
        (4, "LINEAR", {"rightArm": P(pitch=-72, yaw=-62, roll=10), "leftArm": P(pitch=-72, yaw=62, roll=-10), "torso": P(pitch=4)}),
        (7, "EASEINQUAD", {"rightArm": P(pitch=-84, yaw=48), "leftArm": P(pitch=-84, yaw=-48),
                           "torso": P(pitch=12, yaw=0), "rightLeg": P(pitch=-16), "leftLeg": P(pitch=12)}),
        (9, "EASEOUTCUBIC", {"rightArm": P(pitch=-86, yaw=58), "leftArm": P(pitch=-86, yaw=-58), "torso": P(pitch=14)}),
        (11, "EASEINOUTSINE", {"rightArm": P(pitch=-55, yaw=-6), "leftArm": P(pitch=-55, yaw=6), "torso": P(pitch=0)}),
    ]),
    # whip: arm cocked back and up, then a fast forward snap that carries the lash out
    "whip_lash": (10, 14, [
        (0, "EASEOUTSINE", {"rightArm": P(pitch=-40), "leftArm": P(pitch=6, roll=-10)}),
        (3, "EASEOUTSINE", {"rightArm": P(pitch=-165, yaw=-10, roll=6), "leftArm": P(pitch=14, roll=-22),
                            "torso": P(pitch=-12, yaw=14), "head": P(pitch=-6), "rightLeg": P(pitch=6)}),
        (4, "LINEAR", {"rightArm": P(pitch=-168, yaw=-10, roll=6), "torso": P(pitch=-13, yaw=14)}),
        (6, "EASEINQUAD", {"rightArm": P(pitch=-78, yaw=4), "leftArm": P(pitch=18, roll=-16),
                           "torso": P(pitch=16, yaw=-10), "head": P(pitch=8), "rightLeg": P(pitch=-22), "leftLeg": P(pitch=12)}),
        (8, "EASEOUTCUBIC", {"rightArm": P(pitch=-84, yaw=6), "torso": P(pitch=18, yaw=-12), "head": P(pitch=10)}),
        (10, "EASEINOUTSINE", {"rightArm": P(pitch=-40), "leftArm": P(pitch=6, roll=-10), "torso": P(pitch=0), "head": P(pitch=0)}),
    ]),
}

def build(name):
    end, stop, frames = ANIMS[name]
    moves = []
    last = -1
    for tick, easing, parts in frames:
        if tick <= last: raise SystemExit("%s: ticks must ascend strictly (tick %d after %d)" % (name, tick, last))
        if easing not in EASINGS: raise SystemExit("%s: unknown easing %s" % (name, easing))
        last = tick
        mv = {"tick": tick, "easing": easing, "turn": 0}
        for part in PARTS:
            if part not in parts: continue
            vals = {}
            for k in OFFSET_KEYS:
                if k in parts[part]: vals[k] = round(float(parts[part][k]), 4)
            for k in ANGLE_KEYS:
                if k in parts[part]: vals[k] = round(math.radians(parts[part][k]), 4)
            if vals: mv[part] = vals
        for part in parts:
            if part not in PARTS: raise SystemExit("%s: unknown part %s" % (name, part))
        moves.append(mv)
    if not (0 <= last <= end <= stop): raise SystemExit("%s: need beginTick 0 <= last move %d <= endTick %d <= stopTick %d" % (name, last, end, stop))
    # Better Combat's own files (checked against its repository, assets/bettercombat/player_animations) use ONE part and ONE axis per move
    # entry, isLoop as the string "false", returnTick 2; the same form is written here so the Player Animator reader sees exactly what it
    # is known to read.
    out = []
    for part in PARTS:
        for axis in OFFSET_KEYS + ANGLE_KEYS:
            for tick, easing, parts in frames:
                if part in parts and axis in parts[part]:
                    v = float(parts[part][axis])
                    v = round(math.radians(v), 4) if axis in ANGLE_KEYS else round(v, 4)
                    out.append({"tick": tick, "easing": easing, "turn": 0, part: {axis: v}})
    return {"name": name, "author": AUTHOR, "description": name.replace("_", " "),
            "emote": {"isLoop": "false", "returnTick": 2, "beginTick": 0, "endTick": end, "stopTick": stop,
                      "degrees": False, "moves": out}}

def render(name): return json.dumps(build(name), indent=2) + "\n"

def main(argv):
    if "--list" in argv:
        for n in ANIMS: print(n)
        return 0
    check = "--check" in argv
    bad = 0
    os.makedirs(OUT, exist_ok=True)
    for n in ANIMS:
        path = os.path.join(OUT, n + ".json")
        text = render(n)
        cur = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if cur == text: continue
        if check:
            print("differs or missing: " + os.path.relpath(path, ROOT)); bad += 1
        else:
            open(path, "w", encoding="utf-8", newline="\n").write(text)
            print("wrote " + os.path.relpath(path, ROOT))
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
