"""The 36 dark Djinn weapons of the slice `djinn_dark_weapons`: for each Djinn the recipe (which motifs, from the Djinn's own look, affinity and weapon kind in docs/djinn_roster_36.json),
the orbiting piece, the colours of the effects and the particle layers. The recipe is data; the geometry is dkit.py, the pipeline build.py.
"""
import json
import os
import zlib

import dkit as K

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ROSTER = json.load(open(os.path.join(ROOT, "docs", "djinn_roster_36.json"), encoding="utf-8"))

# the orbiting piece of each Djinn (a different small object round the weapon): its lore in one word
ORBIT = {"baal": "bolt", "amon": "flame", "vinea": "bubble", "focalor": "feather", "zagan": "leaf", "zepar": "claw", "furfur": "note", "marchosias": "flame", "sitri": "star",
         "bifron": "bolt", "paimon": "feather", "vassago": "bubble", "bael": "rune", "agares": "shard", "valefor": "bolt", "barbatos": "leaf", "gusion": "shard", "vepar": "bubble",
         "shax": "feather", "buer": "leaf", "andrealphus": "star", "kimaris": "orb", "amdusias": "note", "belial": "flame", "decarabia": "shard", "seere": "feather",
         "dantalion": "rune", "andromalius": "rune", "glasya_labolas": "claw", "haagenti": "rune", "crocell": "shard", "furcas": "shard", "balam": "flame", "alloces": "flame",
         "camio": "feather", "murmur": "note"}

# affinity -> (vein wave speed, vein wavelength factor, rate of the rising motes, spark spread of the edge line, swing ghosts, orbit speed)
AFF = {"Lightning": (0.55, 0.8, 0.0, 0.020, 4, 0.07), "Heat": (0.35, 1.0, 0.030, 0.006, 3, 0.05), "Water": (0.22, 1.2, -0.008, 0.004, 3, 0.035), "Wind": (0.45, 0.9, 0.0, 0.012, 4, 0.06),
       "Life": (0.18, 1.1, 0.010, 0.004, 3, 0.03), "Strength": (0.15, 1.3, -0.010, 0.004, 3, 0.03), "Sound": (0.30, 1.0, 0.0, 0.008, 3, 0.045), "Light": (0.25, 1.0, 0.007, 0.005, 3, 0.04)}

PASSES = ["body", "core_glow", "vein_wave", "halo_shell", "orbit_pieces"]
AMBIENT = ["motes", "halo_ring", "edge_line", "ground_cast", "wrist_flare", "light_shaft", "accent_flare", "orbit_pieces"]
ANIMATIONS = ["glow_pulse", "core_flicker", "vein_wave_flow", "halo_shell_breath", "orbit_revolve", "orbit_piece_spin", "orbit_bob"]


def item_id(did):
    return "djinn_weapon_%s_dark" % did


def spec_of(d):
    did = d["id"]
    text = "".join(c for c in d["name"].upper() if c.isalpha())[:9]
    return {"id": did, "item": item_id(did), "kind": d["weapon"]["kind"], "affinity": d["affinity"], "look": d["look"], "color": int(d["color"], 16), "glow": int(d["glow"], 16),
            "text": text, "orbit": ORBIT[did], "name": d["name"], "title": d["title"], "anchors": ANCHORS.get(did, {})}


# per-weapon anchor overrides, found by looking at the previews (empty = the frame's own measure)
ANCHORS = {}

SPECS = {d["id"]: spec_of(d) for d in ROSTER}
ORDER = [d["id"] for d in ROSTER]


def recipe(C):
    """Adds every motif of the Djinn to the context. Order matters only for part names."""
    sp = C.sp
    look = sp["look"]
    h = zlib.crc32(sp["id"].encode())
    K.grip_bands(C, 4)
    K.pommel_crown(C, 7)
    K.name_plate(C)
    K.eye_gem(C)
    K.halo_rings(C, 2 if look["halo"] == "NONE" else 1)
    if look["horns"] not in ("CROWN", "SPIKE") or h % 2:
        K.thorn_collar(C, n=8)
    if look["halo"] in ("NONE", "RING"):
        K.gem_ring(C, n=6)
    if look["tail"] == "NONE" or h % 3 == 0:
        K.chain(C, links=8, side=1.0 if h % 2 else -1.0)
    K.horns(C, look["horns"])
    K.wings(C, look["wings"])
    K.tail(C, look["tail"])
    K.halo(C, look["halo"])
    K.AFFINITY[sp["affinity"]](C)


def orbit(C):
    K.orbit_piece(C, C.sp["orbit"])


def particles(sp, length_blocks):
    """The seven ambient particle layers of the Djinn (the token `fx` = the mod's own particles themed and tinted by the colour; the table is read by client/vupgrade/godb)."""
    c, g = "%06X" % sp["color"], "%06X" % sp["glow"]
    rise = AFF[sp["affinity"]][2]
    spread = AFF[sp["affinity"]][3]
    layers = [
        {"kind": "motes", "particle": "fx", "rgb": g, "rate": 1.1, "speed": rise if rise else 0.008, "size": 1.0},
        {"kind": "halo_ring", "particle": "fx", "rgb": c, "rate": 1.3, "speed": 0.0, "size": 0.9},
        {"kind": "edge_line", "particle": "fx", "rgb": g, "rate": 0.9, "speed": spread, "size": 0.9},
        {"kind": "ground_cast", "particle": "fx", "rgb": c, "rate": 1.0, "speed": 0.0, "size": 1.1},
        {"kind": "wrist_flare", "particle": "fx", "rgb": g, "rate": 0.5, "speed": 0.01, "size": 1.0},
        {"kind": "light_shaft", "particle": "fx", "rgb": g, "rate": 1.1, "speed": 0.03, "size": 0.8},
        {"kind": "accent_flare", "particle": "fx", "rgb": "FFFFFF", "rate": 0.12, "speed": 0.02, "size": 1.0},
    ]
    return layers


def json_table(sp, place_scale, ymin, ymax, orbit_count, orbit_radius, nbody, norbit_piece, nlod):
    did = sp["item"]
    c, g = "%06X" % sp["color"], "%06X" % sp["glow"]
    vs, vw, _rise, _spread, ghosts, ospeed = AFF[sp["affinity"]]
    length_blocks = round((ymax - ymin) * place_scale * 0.85, 3)
    return {
        "item": did, "version": 1,
        "mesh": "up/" + did, "lod_mesh": "up/" + did + "_lod", "orbit_mesh": "up/" + did + "_orbit",
        "axis": {"y0": round(ymin, 3), "y1": round(ymax, 3), "length_blocks": length_blocks},
        "passes": PASSES, "ambient_layers": AMBIENT, "animations": ANIMATIONS,
        "palette": {"core": g, "accent": "FFFFFF" if sp["glow"] != 0xFFFFFF else "F0F0F0", "vein": g, "halo": c},
        "veins": {"speed": vs, "wavelength": round(44.0 * vw * max(0.6, (ymax - ymin) / 60.0), 1), "base": 0.18, "peak": 1.0},
        "halo": {"scale": 1.075, "alpha": 60, "breath": 0.03, "speed": 0.05},
        "core": {"flicker": 0.10, "flicker_speed": 0.37},
        "orbiters": [{"count": orbit_count, "radius": round(orbit_radius, 2), "y_frac": 0.78, "speed": ospeed, "spin": 0.11, "bob": 2.2, "bob_speed": 0.05, "tilt": 0.3, "phase": 0.0, "scale": 1.0,
                      "mesh": "up/" + did + "_orbit"}],
        "swing_trail": {"rgb": g, "ghosts": ghosts, "lag_deg": 8.0, "alpha": 84},
        "particles": {"layers": particles(sp, length_blocks), "ground_radius": 1.15, "shaft_height": 1.9},
        "lod": {"near_blocks": 12.0, "additive_blocks": 24.0, "particle_blocks": 32.0},
        "budget": {"body_quads": nbody, "orbit_instances": orbit_count, "orbit_piece_quads": norbit_piece, "lod_quads": nlod},
    }
