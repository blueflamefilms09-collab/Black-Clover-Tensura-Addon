"""0.54: the spell-casting body animations (docs/cast_animation_guide.md) as a Blockbench / GeckoLib style .animation.json for the player.

    python3 tools/gen_cast_animations.py        ->  src/main/resources/assets/nusmp/animations/player/cast.animation.json

Bones are the player template's: head, body, right_arm, left_arm, right_leg, left_leg. Angles are the PLAYER MODEL'S OWN rotations in degrees
(xRot, yRot, zRot as in vanilla's HumanoidModel, the same convention as docs/grimoire_sword_draw.md): arm X -90 points straight forward and
-180 straight up; the LEFT arm swings out to its side with a NEGATIVE Z, the RIGHT arm with a POSITIVE Z; a positive Y turns an arm or the body
toward the player's right. Times are seconds (20 ticks = 1 s). Positions are pixels, Y up (the client turns them into the model's down-positive Y).
The summoned grimoire floats at the RIGHT hand, so the LEFT arm does the gestures and the right arm stays near the book.

How the client applies them (client/CastAnimClient): arms REPLACE the vanilla pose with the keyframes (blended in over 3 ticks, out over the last
4); the body, head and legs ADD to the vanilla pose (so walking and looking still show); a body position moves the whole figure (rise / sink).
Loops (chant, mana zone) close: the last key equals the first.
"""
import json
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "animations", "player", "cast.animation.json")

REST = (0, 0, 0)


def pose(**bones):
    """{bone: (rx, ry, rz)} or {bone: ((rx, ry, rz), (px, py, pz))}."""
    return bones


def clip(length, loop, keys):
    """keys: [(time, pose)] -> a Blockbench animation. A bone is animated from the first key that mentions it; each key may leave bones out."""
    bones = {}
    for t, p in keys:
        for bone, v in p.items():
            rot, pos = (v if isinstance(v[0], (tuple, list)) else (v, None))
            b = bones.setdefault(bone, {"rotation": {}, "position": {}})
            b["rotation"]["%.2f" % t] = list(rot)
            if pos is not None:
                b["position"]["%.2f" % t] = list(pos)
    out = {}
    for bone, ch in bones.items():
        e = {}
        if ch["rotation"]:
            e["rotation"] = ch["rotation"]
        if ch["position"]:
            e["position"] = ch["position"]
        out[bone] = e
    return {"loop": loop, "animation_length": length, "bones": out}


# the chant pose: the left palm raised forward and up over the book, the body leaning in, a combat stance
CHANT = dict(body=((6, 10, 0), (0, -0.5, 0)), left_arm=(-110, 0, -10), right_arm=(-30, 0, 0), left_leg=(-8, 0, 0), right_leg=(6, 0, 0))

CLIPS = {
    # every page while the key is held: the hand rises and gathers, a slow breathing loop
    "cast_chant": clip(1.0, True, [
        (0.00, CHANT),
        (0.50, dict(body=((4, 8, 0), (0, -0.3, 0)), left_arm=(-100, 0, -14), right_arm=(-28, 0, 0), left_leg=(-8, 0, 0), right_leg=(6, 0, 0))),
        (1.00, CHANT),
    ]),
    # HAND OUT: the palm shoots forward (bolts, rays, spears)
    "cast_release_thrust": clip(0.8, False, [
        (0.00, dict(body=(6, 25, 0), left_arm=(-110, 0, -10), right_arm=(-30, 0, 0))),
        (0.05, dict(body=(-4, -15, 0), left_arm=(-95, -10, 0), right_arm=(-28, 0, 0))),
        (0.15, dict(body=(-6, -20, 0), left_arm=(-98, -10, 0), right_arm=(-26, 0, 0))),
        (0.25, dict(body=(4, -8, 0), left_arm=(-70, 0, 10), right_arm=(-18, 0, 0))),
        (0.80, dict(body=REST, left_arm=REST, right_arm=REST)),
    ]),
    # HAND TO THE SIDE: the arm crosses the chest and is flung out to the left side (slashes, arcs, waves)
    "cast_release_sweep": clip(0.9, False, [
        (0.00, dict(body=(6, 30, 0), left_arm=(-80, 60, 0), right_arm=(-30, 0, 0))),
        (0.10, dict(body=(0, -25, 0), left_arm=(-80, -70, 0), right_arm=(-26, 0, 0))),
        (0.25, dict(body=(0, -25, 0), left_arm=(-78, -72, 0), right_arm=(-26, 0, 0))),
        (0.40, dict(body=(3, -10, 0), left_arm=(-60, -30, -6), right_arm=(-14, 0, 0))),
        (0.90, dict(body=REST, left_arm=REST, right_arm=REST)),
    ]),
    "cast_release_side": clip(0.9, False, [
        (0.00, dict(body=(4, 14, 0), left_arm=(-110, 0, -10), right_arm=(-30, 0, 0))),
        (0.08, dict(body=(0, -18, 0), left_arm=(-12, -8, -88), right_arm=(-26, 0, 6))),
        (0.30, dict(body=(0, -20, 0), left_arm=(-10, -8, -92), right_arm=(-24, 0, 8))),
        (0.50, dict(body=(2, -8, 0), left_arm=(-8, 0, -60), right_arm=(-14, 0, 4))),
        (0.90, dict(body=REST, left_arm=REST, right_arm=REST)),
    ]),
    # HAND UP: the palm is thrown straight up over the head (lightning, pillars, falling stars, summons from above)
    "cast_release_up": clip(0.9, False, [
        (0.00, dict(body=(6, 10, 0), left_arm=(-110, 0, -10), right_arm=(-30, 0, 0))),
        (0.08, dict(body=((-9, 0, 0), (0, 0.6, 0)), left_arm=(-172, 0, -8), right_arm=(-26, 0, 0))),
        (0.30, dict(body=((-10, 0, 0), (0, 0.6, 0)), left_arm=(-178, 0, -6), right_arm=(-24, 0, 0))),
        (0.50, dict(body=(-3, 0, 0), left_arm=(-140, 0, -8), right_arm=(-16, 0, 0))),
        (0.90, dict(body=REST, left_arm=REST, right_arm=REST)),
    ]),
    # BOTH HANDS UP then driven down (zones: meteorites, pillars, domes); a small rise on the wind-up, a crouch on the contact
    "cast_release_slam": clip(1.1, False, [
        (0.00, dict(body=((-12, 0, 0), (0, 2, 0)), left_arm=(-170, 0, -6), right_arm=(-170, 0, 6))),
        (0.15, dict(body=((-12, 0, 0), (0, 2, 0)), left_arm=(-172, 0, -6), right_arm=(-172, 0, 6))),
        (0.22, dict(body=((20, 0, 0), (0, -2, 0)), left_arm=(-40, 0, -4), right_arm=(-40, 0, 4))),
        (0.50, dict(body=((20, 0, 0), (0, -2, 0)), left_arm=(-40, 0, -4), right_arm=(-40, 0, 4))),
        (0.75, dict(body=((8, 0, 0), (0, -1, 0)), left_arm=(-20, 0, -8), right_arm=(-20, 0, 8))),
        (1.10, dict(body=(REST, REST), left_arm=REST, right_arm=REST)),
    ]),
    # the overcharge hold: arms spread to the sides, palms open, the chest lifted, the legs hanging, a slow hover bob
    "cast_mana_zone": clip(1.6, True, [
        (0.00, dict(body=((-8, 0, 0), (0, 2, 0)), left_arm=(-30, 0, -70), right_arm=(-30, 0, 70), left_leg=(10, 0, 0), right_leg=(4, 0, 0))),
        (0.40, dict(body=((-8, 0, 0), (0, 3, 0)), left_arm=(-32, 0, -72), right_arm=(-32, 0, 72), left_leg=(10, 0, 0), right_leg=(4, 0, 0))),
        (0.80, dict(body=((-8, 0, 0), (0, 2, 0)), left_arm=(-30, 0, -70), right_arm=(-30, 0, 70), left_leg=(10, 0, 0), right_leg=(4, 0, 0))),
        (1.20, dict(body=((-8, 0, 0), (0, 1, 0)), left_arm=(-28, 0, -68), right_arm=(-28, 0, 68), left_leg=(10, 0, 0), right_leg=(4, 0, 0))),
        (1.60, dict(body=((-8, 0, 0), (0, 2, 0)), left_arm=(-30, 0, -70), right_arm=(-30, 0, 70), left_leg=(10, 0, 0), right_leg=(4, 0, 0))),
    ]),
    # the finisher: chant pose, a breath in, an explosive spread to both sides, a hold, a slow settle
    "cast_signature": clip(1.6, False, [
        (0.00, CHANT),
        (0.40, CHANT),
        (0.60, dict(body=((-10, 0, 0), (0, 0.5, 0)), left_arm=(-60, 0, -40), right_arm=(-60, 0, 40), left_leg=REST, right_leg=REST)),
        (0.70, dict(body=((-14, 0, 0), (0, 1.5, 0)), left_arm=(-30, 0, -95), right_arm=(-30, 0, 95), left_leg=(6, 0, 0), right_leg=(6, 0, 0))),
        (1.00, dict(body=((-14, 0, 0), (0, 1.5, 0)), left_arm=(-30, 0, -95), right_arm=(-30, 0, 95), left_leg=(6, 0, 0), right_leg=(6, 0, 0))),
        (1.60, dict(body=(REST, REST), left_arm=REST, right_arm=REST, left_leg=REST, right_leg=REST)),
    ]),
    # the chant broke off, the page was sealed, or the spell fizzled: the hand drops, the body flinches back
    "cast_fail": clip(0.5, False, [
        (0.00, dict(body=(6, 10, 0), left_arm=(-110, 0, -10), right_arm=(-30, 0, 0))),
        (0.10, dict(body=(-6, 0, 0), left_arm=(0, 0, -5), right_arm=(-10, 0, 0))),
        (0.50, dict(body=REST, left_arm=REST, right_arm=REST)),
    ]),
}


def main():
    data = {"format_version": "1.8.0", "animations": CLIPS}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", newline="\n") as f:
        json.dump(data, f, indent=1)
        f.write("\n")
    print("wrote", os.path.normpath(OUT), len(CLIPS), "clips")


if __name__ == "__main__":
    main()
