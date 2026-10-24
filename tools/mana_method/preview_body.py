"""
The Body Rune (Gadjah's way, 3.6.0): rune arrays written on the caster's own body, through the game's own render path: the real RuneWritingLayer with two RuneHoldViews set up exactly as the client
sets them up (RuneClient.bodyView: paintTicks 24, the chest rune at scale 0.3 on the chest 1.15 above the feet and 0.32 in front of it, from the second tier also the rune at the feet at scale 0.85),
the real RuneGlyphLayout, and the numbers of the real pure classes (BodyRuneRules via DumpMm36.java: pictures and duration per stage).

  rune_body.png   TAP (one picture: the chest rune), HALF HOLD and FULL HOLD (chest rune and rune at the feet), all written; then the full hold being written (ticks 8 and 16) and fading at the end

    python tools/mana_method/preview_body.py

Honest stand-ins: the dark capsule is a stand-in for the player (the picture has no player model). The pictures follow the body every frame in the game (RuneClient.onRenderBodies); here the body stands
still, so this shows the look, not the following. The attribute gifts (speed, attack, jump ...) are server-side numbers and have no picture.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mm36_common as mm  # noqa: E402

pv = mm.pv
PAINT_TICKS = 24
CHEST_SCALE, FEET_SCALE = 0.3, 0.85          # RuneClient.CHEST_SCALE / FEET_SCALE
GROUND = 68.38


def body_scenes(pack, looks, pictures, ages, end_age, cam, target):
    """The chest rune and (from two pictures) the rune at the feet of a body at the origin facing +z (yaw 0), as RuneClient.onRenderBodies places them."""
    chest_c = [0.0, GROUND + 1.15, 0.32]
    base = {"layer": "RuneWritingLayer", "shape": "RUNE_WRITING", "duration": 700, "seed": pack, "budget": 1200, "detail": 1.0, "cam": cam, "target": target, "ages": list(ages),
            "paint": [PAINT_TICKS, end_age, -1], "figures": [[0.0, GROUND, 0.0, 1.8]], "ground": GROUND, "sky": [0.14, 0.18, 0.2], "horizon": [0.22, 0.26, 0.28]}
    out = [dict(base, **{"from": chest_c, "to": [chest_c[0], chest_c[1], chest_c[2] - 1.0], "power": CHEST_SCALE})]
    if pictures > 1:
        feet_c = [0.0, GROUND + 0.05, 0.0]
        look = looks["up"]
        out.append(dict(base, **{"from": feet_c, "to": [feet_c[0] + look[0], feet_c[1] + look[1], feet_c[2] + look[2]], "power": FEET_SCALE}))
    return out


def main():
    os.makedirs(pv.BUILD, exist_ok=True)
    nums = mm.numbers()
    classes = pv.build(["RuneWritingLayer"])
    pack = nums["packs"]["FIRE"]
    looks = nums["looks"]
    pv.FOV = 62.0                                                             # a closer, narrower view than the held rune's: the pictures are small
    cam, target = [0.9, GROUND + 2.0, -2.9], [0.0, GROUND + 0.75, 0.0]
    names = {"BASIC": "TAP", "CHARGED": "HALF HOLD", "FULL": "FULL HOLD"}
    shots = []
    for st in nums["stages"]:
        scs = body_scenes(pack, looks, st["bodyPictures"], (40,), st["bodyTicks"] + 100, cam, target)
        for im, verts, age in mm.merged(classes, "body_%s" % st["stage"], scs):
            shots.append(mm.half(mm.label(im, "%s: %d picture(s), lasts %d s" % (names[st["stage"]], st["bodyPictures"], st["bodyTicks"] // 20), "tick %d, %d v" % (age, verts))))
    full = nums["stages"][2]
    for ages, end, text in (((8,), 700, "FULL HOLD, being written"), ((16,), 700, "FULL HOLD, being written"), ((58,), 60, "FULL HOLD, fading at the end")):
        scs = body_scenes(pack, looks, full["bodyPictures"], ages, end, cam, target)
        for im, verts, age in mm.merged(classes, "body_full_%d" % ages[0], scs):
            shots.append(mm.half(mm.label(im, text, "tick %d, %d v" % (age, verts))))
    mm.sheet(shots, "rune_body.png", "Body Rune: the runes written on the body, a chest rune and (from the second tier) a rune at the feet; the capsule stands in for the player", 3)
    print("wrote", mm.OUT)


if __name__ == "__main__":
    sys.exit(main())
