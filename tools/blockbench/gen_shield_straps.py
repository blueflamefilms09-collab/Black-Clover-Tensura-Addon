#!/usr/bin/env python3
"""Shield arm straps (docs/visual_upgrade/shield_fix.md, section "Straps"): two leather bands that wrap the holder's forearm, a grip bar and the strap tabs.

    python3 -B tools/blockbench/gen_shield_straps.py

WHAT THIS IS (rule 15, said plainly): neither Blender nor Blockbench could run in the session, so this is the FALLBACK. The cubes below are written as Blockbench's own
source format (tools/blockbench/shield_straps.bbmodel, NOT opened in Blockbench) and exported from the same list to the game file
(assets/anime_multiverse/shield_straps.json, read by visual/shields/ShieldStraps) plus the grain texture (textures/entity/weapon/shield_straps.png, painted with numpy / PIL).
Open the .bbmodel once and look before editing it by hand. Deterministic: re-running writes the same bytes.

The cubes live in the ARM frame, in blocks: +y runs down the forearm toward the wrist, the arm's axis is x = z = 0, +z is the side that faces the plate (the shield's face), and the
minecraft arm is 0.25 x 0.25 across. y = 0 is the shoulder pivot; the elbow is at 4 px = 0.25, the wrist at 10 px = 0.625. The two strap tabs from the rings to the plate are NOT cubes
here: their length depends on the shield, so ShieldStraps builds them (the "attach" points below say where they start).
"""
import json
import os
import uuid

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")

INNER, OUTER, WIDTH = 0.14, 0.17, 0.07          # ring: inner half 0.14 (0.28 across, the arm is 0.25), outer half 0.17 (0.34), 0.07 wide along the arm
RINGS = {"upper": 0.3125, "lower": 0.5500}      # ring centres down the arm (blocks): 5 px, 8.8 px
BAR_Y = 0.656                                   # the grip bar, over the hand (10.5 px)

cubes = []


def cube(name, part, lo, hi):
    cubes.append({"name": name, "part": part, "from": [round(v, 4) for v in lo], "to": [round(v, 4) for v in hi]})


for nm, yc in RINGS.items():
    y0, y1 = yc - WIDTH / 2, yc + WIDTH / 2
    cube(nm + "_ring_front", "leather", (-OUTER, y0, INNER), (OUTER, y1, OUTER))
    cube(nm + "_ring_back", "leather", (-OUTER, y0, -OUTER), (OUTER, y1, -INNER))
    cube(nm + "_ring_left", "leather", (-OUTER, y0, -INNER), (-INNER, y1, INNER))
    cube(nm + "_ring_right", "leather", (INNER, y0, -INNER), (OUTER, y1, INNER))
    cube(nm + "_buckle", "trim", (-0.05, yc - 0.045, OUTER), (0.05, yc + 0.045, OUTER + 0.015))
cube("grip_bar", "leather", (-0.12, BAR_Y - 0.02, 0.17), (0.12, BAR_Y + 0.02, 0.21))
cube("grip_cap_left", "trim", (-0.14, BAR_Y - 0.025, 0.165), (-0.12, BAR_Y + 0.025, 0.215))
cube("grip_cap_right", "trim", (0.12, BAR_Y - 0.025, 0.165), (0.14, BAR_Y + 0.025, 0.215))
cube("bracket_left", "leather", (-0.10, RINGS["lower"] + 0.02, 0.17), (-0.06, BAR_Y + 0.02, 0.21))
cube("bracket_right", "leather", (0.06, RINGS["lower"] + 0.02, 0.17), (0.10, BAR_Y + 0.02, 0.21))

data = {
    "version": 1,
    "note": "arm frame, blocks; written by tools/blockbench/gen_shield_straps.py (script fallback, not made in Blockbench or Blender)",
    "arm_half": 0.125,
    "cubes": cubes,
    # where the two strap tabs to the plate start (arm frame): on the upper ring's buckle, and over the grip bar for the lower one
    "attach": [{"name": "upper", "at": [0, RINGS["upper"], OUTER + 0.015]}, {"name": "lower", "at": [0, BAR_Y, 0.21]}],
    "tab": {"width": 0.07, "thick": 0.03, "rivet": 0.05, "rivet_thick": 0.02, "overlap": 0.03},
    # first person: no arm is drawn there, so only the grip bar and its two mounts, low on the back of the plate. z is relative to the plate's back (negative = toward the camera).
    "firstPerson": [
        {"name": "fp_bar", "part": "leather", "from": [-0.025, -0.44, -0.12], "to": [0.025, -0.20, -0.08]},
        {"name": "fp_mount_low", "part": "trim", "from": [-0.045, -0.44, -0.12], "to": [0.045, -0.40, 0.02]},
        {"name": "fp_mount_high", "part": "trim", "from": [-0.045, -0.24, -0.12], "to": [0.045, -0.20, 0.02]},
    ],
}


def texture():
    """128 x 128 grey: left half leather grain with a stitch column, right half brushed metal; both are tinted by the renderer."""
    rng = np.random.default_rng(7)
    g = rng.normal(0, 1, (128, 128))
    for _ in range(2):                                     # a little blur: grain, not noise
        g = (g + np.roll(g, 1, 0) + np.roll(g, 1, 1) + np.roll(g, -1, 0)) / 4
    img = np.zeros((128, 128), np.float32)
    img[:, :64] = 0.80 + 0.45 * g[:, :64]
    img[:, 64:] = 0.90 + 0.12 * rng.normal(0, 1, (128, 1))  # brushed: variation by row
    img[::8, :62] *= 0.82                                  # faint creases
    img[:, 62] = 0.55                                      # a stitch column at the edge of the leather half
    img[::3, 62] = 0.95
    img = np.clip(img, 0, 1)
    rgba = np.stack([img, img, img, np.ones_like(img)], -1)
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def bbmodel():
    """Blockbench source: every cube grey (units: pixels = blocks * 16)."""
    els = []
    for c in cubes + [{"name": "arm_reference", "part": "ref", "from": [-0.125, 0.25, -0.125], "to": [0.125, 0.625, 0.125]}]:
        els.append({"name": c["name"], "type": "cube", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "shield_straps/" + c["name"])), "from": [v * 16 for v in c["from"]],
                    "to": [v * 16 for v in c["to"]], "rotation": [0, 0, 0], "origin": [0, 0, 0], "color": {"leather": 3, "trim": 5, "ref": 0}[c["part"]], "export": c["part"] != "ref",
                    "faces": {f: {"uv": [0, 0, 4, 4], "texture": 0} for f in ("north", "east", "south", "west", "up", "down")}})
    return {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False}, "name": "shield_straps", "model_identifier": "shield_straps",
            "resolution": {"width": 128, "height": 128}, "elements": els,
            "outliner": [{"name": "straps", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "shield_straps/group")), "origin": [0, 0, 0], "children": [e["uuid"] for e in els]}],
            "textures": [{"name": "shield_straps.png", "relative_path": "../../src/main/resources/assets/anime_multiverse/textures/entity/weapon/shield_straps.png"}],
            "_note": "written by gen_shield_straps.py (script fallback); arm_reference is a 4 x 4 px forearm piece for scale and is not exported (export=false). Not opened in Blockbench."}


def main():
    with open(os.path.join(ASSETS, "shield_straps.json"), "w", newline="\n") as f:
        json.dump(data, f, indent=1)
        f.write("\n")
    texture().save(os.path.join(ASSETS, "textures", "entity", "weapon", "shield_straps.png"))
    with open(os.path.join(HERE, "shield_straps.bbmodel"), "w", newline="\n") as f:
        json.dump(bbmodel(), f, indent=1)
        f.write("\n")
    print("cubes", len(cubes), "quads", len(cubes) * 6)


if __name__ == "__main__":
    main()
