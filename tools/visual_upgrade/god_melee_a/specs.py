"""The effect spec of an upgraded item: assets/nusmp/visual_upgrade/<id>.json (read by client/upgrade/UpgradeSpecs at start-up and by the data tests).

One file per item. The generators fill it from a short dict (see items_*.py); this module only completes and writes it. The signature id of an act is crc32("<item>:<act>") & 0x7FFFFFFF:
the server sends it in the seed of the effect packet (relics/wave/UpgradeSignatures), the client layer looks the recipe up by it.
"""
import json
import os
import zlib

import common as C

PASSES = ["skin", "glow_core", "veins_flow", "halo_shell"]
IDLE = ["glow_breath", "orbit_parts", "flow_veins", "halo_breath", "rune_flicker"]


def sig_id(item_id, act):
    return zlib.crc32(("%s:%s" % (item_id, act)).encode()) & 0x7FFFFFFF


def build(item_id, *, rgb, rgb2, ambient, swing, acts, length, tilt, veins_cfg=None, halo_cfg=None, glow=None, sound=None, idle=None, orbit=True, lod_near=12.0, lod_glow_off=24.0,
          flow=None, note=""):
    """rgb / rgb2 = the item's two palette colours (0..255 triples); ambient = list of layer dicts; swing = dict; acts = [(act key, recipe list)]; length = blocks, tilt = degrees from the vertical."""
    spec = {
        "id": item_id, "version": 1, "slice": C.SLICE, "grade": "god",
        "palette": {"primary": list(rgb), "secondary": list(rgb2)},
        "maps": {"skin": "up/%s.png" % item_id, "glow": "up/%s_glow.png" % item_id, "veins": "up/%s_veins.png" % item_id},
        "lod": {"mesh": "up/%s_lod" % item_id, "near": lod_near, "glow_off": lod_glow_off, "skin": "up/%s_lod.png" % item_id, "glow": "up/%s_lod_glow.png" % item_id},
        "passes": list(PASSES),
        "pass_cfg": {
            "veins": veins_cfg or {"rgb": list(rgb), "speed": 0.55, "wavelength": 26.0, "min": 0.25, "max": 1.0},
            "halo": halo_cfg or {"rgb": list(rgb2), "scale": 1.045, "alpha_min": 18, "alpha_max": 70, "speed": 0.09},
            "flicker": {"amount": 0.12, "speed": 0.7},
        },
        "idle": list(idle or IDLE),
        "ambient": ambient,
        "hand": {"length": length, "tilt": tilt},
        "swing": swing,
        "acts": [{"act": a, "id": sig_id(item_id, a), "recipe": r} for a, r in acts],
        "ambient_sound": sound or ("nusmp:item.upgrade.%s.hum" % item_id),
        "note": note,
    }
    if glow:
        spec["glow"] = glow
    if not orbit:
        spec["idle"] = [x for x in spec["idle"] if x != "orbit_parts"]
    return spec


def write(spec):
    os.makedirs(C.SPEC_DIR, exist_ok=True)
    __import__("runpy").run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "custom_particles.py"))["convert"](spec)   # 1.110.0: every particle token of the table becomes one of the mod's own (tools/visual_upgrade/custom_particles.py)
    path = os.path.join(C.SPEC_DIR, spec["id"] + ".json")
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(spec, fh, indent=1)
        fh.write("\n")
    return path
