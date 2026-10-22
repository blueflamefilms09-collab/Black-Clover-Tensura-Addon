#!/usr/bin/env python3
"""
Rooms rebuilt, part 7: the halls. `rebuild(name, variant)` opens a shipped hall template, runs the passes of a recipe on it and returns the rebuilt Room. A recipe is
a plain dict (see RECIPES), chosen by the hall's family and the variant number; every random choice inside it comes from the generator seeded with (name, variant).

The order matters and is the order of the passes below: strip the old roof, build the new roof (a height field over the interior), carry the chains and pillars up to
it, repaint the floor, stand the pilasters and corner piers, run the string courses, build the feature panels, crown the doors, hang the lights, balance the light,
add wear, drop the armour stands (and, in the rooms that may have one, build the single stand slot).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import rr_core as C  # noqa: E402
import rr_roof as R  # noqa: E402
import rr_floor as F  # noqa: E402
import rr_wall as W  # noqa: E402
import rr_light as LT  # noqa: E402
import rr_dress as DR  # noqa: E402
from rr_core import FR, G  # noqa: E402

# ------------------------------------------------------------------------------------------------------------------------------- families
FAMILIES = [
    # (family, substrings of the template stem that put a hall in it), first match wins
    ("crypt", ("crypt", "ossuary", "catacomb", "graveyard", "mausoleum", "sarcophagus", "necropolis", "coffin", "burial", "tomb")),
    ("cells", ("cell_block", "torture", "penal", "prison", "cage")),
    ("library", ("library", "reading_hall", "scriptorium", "stacks", "archive", "scroll")),
    ("arms", ("armory", "forge", "foundry", "training", "guard_post", "barracks", "dojo", "casino")),
    ("sacred", ("shrine", "temple", "church", "cloister", "nave", "cathedral", "monastery", "prayer", "chapel", "celestial", "sundial")),
    ("hearth", ("great_hall", "feast", "banquet", "kitchen", "larder", "hatchery", "den", "mirror", "colonnade", "terrace", "manor")),
    ("water", ("cistern", "lava", "grotto", "darklake", "drowned", "sunken", "flooded", "geode", "mine")),
    ("lab", ("alchemy", "farm", "enclave", "spider", "web", "cocoon")),
]

#: panel features per family (weights); every kind is in rr_wall.niche
PANELS = {
    "crypt": {"tomb": 4, "shrine": 2, "lantern": 2, "grate": 1, "slit": 1},
    "cells": {"grate": 4, "tomb": 1, "forge": 1, "lantern": 2},
    "library": {"books": 5, "lantern": 2, "slit": 2, "shrine": 1},
    "arms": {"forge": 3, "banner": 3, "lantern": 2, "casks": 2},
    "sacred": {"shrine": 4, "slit": 4, "banner": 2, "lantern": 2},
    "hearth": {"banner": 3, "lantern": 3, "slit": 2, "casks": 2, "books": 1},
    "water": {"lantern": 3, "slit": 3, "casks": 1, "shrine": 1},
    "lab": {"casks": 3, "lantern": 3, "books": 2, "slit": 2},
    "other": {"lantern": 3, "banner": 2, "slit": 2, "casks": 1, "books": 1, "shrine": 1},
}

#: families whose rooms may hold the rare armour stand slot
STAND_FAMILIES = {"arms"}
STAND_STEMS = ("armory", "barracks", "guard_post", "training", "vault", "hoard", "counting", "reliquary", "gem_vault", "closet", "pantry")

SOUL_FAMILIES = {"crypt", "cells"}


def family_of(name):
    stem = name.split("/")[-1]
    for fam, keys in FAMILIES:
        if any(k in stem for k in keys):
            return fam
    return "other"


def stand_eligible(name):
    stem = name.split("/")[-1]
    return any(k in stem for k in STAND_STEMS)


# ------------------------------------------------------------------------------------------------------------------------------- recipes
def recipes_for(family):
    """Four recipes per family (variants 1..4). They differ in the roof, the ribs, the floor, the wall rhythm, pillars, balconies, coffers, the lights and the wear:
    1 a groin-vaulted hall on pillars, 2 a barrel-vaulted hall with galleries and lamp posts, 3 a low coffered hall with heavier wear, 4 a hall under a special roof (gable,
    dome or hip) with a mid course on the walls. The families bend this matrix to their purpose."""
    soul = family in SOUL_FAMILIES
    ruin = family in ("crypt",)
    base = {
        1: dict(roof=("groin", 5), ribs="diag", floor="grid", pilaster="standing", corner=True, pillars=True, balcony=None, coffers=None,
                courses=dict(dado=True, frieze=True), light="chandelier", plight=True, posts=False,
                wear=dict(crack=0.08, moss=0, cobweb=4, rubble=1, broken=0.0), crown=True),
        2: dict(roof=("barrel", 5), ribs="cross", floor="runner", pilaster="engaged", corner=True, pillars=False, balcony=dict(shelves=family == "library"), coffers=None,
                courses=dict(dado=True, frieze=True, mid=True), light="hanging", plight=True, posts=True,
                wear=dict(crack=0.12, moss=1, cobweb=5, rubble=2, broken=0.0), crown=True),
        3: dict(roof=("flat", 0), ribs="cross", floor="checker", pilaster="standing", corner=True, pillars=False, balcony=None, coffers="minecraft:ochre_froglight",
                courses=dict(dado=True, frieze=False), light="rib", plight=True, posts=False,
                wear=dict(crack=0.16, moss=0, cobweb=8, rubble=3, broken=0.3), crown=True),
        4: dict(roof=("gable", 5), ribs="none", floor="rosette", pilaster="engaged", corner=False, pillars=True, balcony=None, coffers=None,
                courses=dict(dado=False, frieze=True, mid=True), light="chandelier", plight=False, posts=True,
                wear=dict(crack=0.06, moss=2, cobweb=3, rubble=0, broken=0.0), crown=True),
    }
    if family == "sacred":
        base[3] = dict(base[3], roof=("dome", 6), floor="inlay", ribs="none", coffers=None)
        base[4] = dict(base[4], roof=("groin", 6), floor="border", ribs="diag", balcony=dict(shelves=False))
        base[2] = dict(base[2], balcony=dict(shelves=False))
    elif family == "library":
        base[1] = dict(base[1], floor="border", pillars=False)
        base[4] = dict(base[4], roof=("barrel", 4), floor="inlay", ribs="cross", pillars=False, balcony=dict(shelves=True))
        base[3] = dict(base[3], coffers="minecraft:shroomlight")
    elif family == "arms":
        base[1] = dict(base[1], floor="checker")
        base[2] = dict(base[2], roof=("gable", 4), floor="grid", ribs="none", balcony=None)
        base[3] = dict(base[3], coffers="minecraft:glowstone")
    elif family == "hearth":
        base[3] = dict(base[3], roof=("hip", 5), floor="runner", ribs="none", coffers=None)
        base[4] = dict(base[4], roof=("groin", 4), ribs="cross")
    elif family == "water":
        base[1] = dict(base[1], floor="diamond")
        base[3] = dict(base[3], roof=("barrel", 4), floor="ruin", coffers=None)
        base[2] = dict(base[2], balcony=None)
    elif family in ("crypt", "cells"):
        base[1] = dict(base[1], floor="diamond", wear=dict(base[1]["wear"], crack=0.18, cobweb=8, rubble=3))
        base[2] = dict(base[2], floor="ruin", balcony=None)
        base[3] = dict(base[3], coffers="minecraft:shroomlight")
        base[4] = dict(base[4], roof=("groin", 4), floor="grid", ribs="diag")
    elif family == "lab":
        base[2] = dict(base[2], balcony=None)
    for v in base.values():
        v["soul"] = soul
        v["ruin"] = ruin
    return base


def rib_pred(room, kind):
    if kind == "diag":
        return R.rib_lines(room, diag=True)
    if kind == "cross":
        return R.rib_lines(room)
    return None


def make_roof(room, spec):
    kind, want = spec
    rise = R.clamp_rise(room, want)
    if room.ceil >= 33:
        rise = min(rise, 2)
    if kind == "groin":
        return R.groin(room, rise)
    if kind == "barrel":
        return R.barrel(room, rise, "z" if room.D >= room.W else "x")
    if kind == "dome":
        return R.dome(room, rise)
    if kind == "gable":
        return R.gable(room, rise, "z" if room.D >= room.W else "x")
    if kind == "hip":
        return R.hip(room, rise)
    return R.flat(room, 0)


def build(name, variant, rise_scale=1.0, restraint=0):
    """One attempt: returns the rebuilt Room (not saved). `restraint` 0 = everything the recipe says; 1 = no lamp posts, no rubble, pilasters stay flush; 2 = also no corner piers
    and no standing pilasters at all. A retry with more restraint is what the generator does when the checks say a prop narrowed a route."""
    fam = family_of(name)
    rc = recipes_for(fam)[variant]
    room = C.Room(name, variant)
    if not room.regular:
        raise ValueError("%s: not a regular hall (no flat roof found)" % name)
    kind, want = rc["roof"]
    spec = (kind, int(round(want * rise_scale)))
    room.strip_roof()
    hf = make_roof(room, spec)
    R.build_roof(room, hf, rib=rib_pred(room, rc["ribs"]))
    R.extend_up(room)
    R.fix_hangers(room)
    F.pattern(room, rc["floor"])
    # pilasters on every module border, corner piers
    for side in ("n", "s", "w", "e"):
        w = W.Wall(room, side)
        for k in range(1, w.m):
            W.pilaster(room, side, 9 * k - 1, "engaged" if restraint >= 1 else rc["pilaster"])
    if rc["corner"] and restraint < 2:
        for (cx, cz, dx, dz) in ((room.x0, room.z0, 1, 1), (room.x1, room.z0, -1, 1), (room.x0, room.z1, 1, -1), (room.x1, room.z1, -1, -1)):
            W.corner_pier(room, cx, cz, dx, dz)
    if rc["pillars"] and restraint < 2:
        W.pillars(room)
    if rc["balcony"] is not None and restraint < 2 and room.ceil >= 21:
        sides = ("n", "s") if room.W >= room.D else ("w", "e")
        for side in sides:
            W.balcony(room, side, shelves=rc["balcony"]["shelves"])
    if rc["coffers"]:
        R.coffers(room, 4, rc["coffers"])
    W.courses(room, **rc["courses"])
    # the one armour stand slot goes in before the niches take every free bay (only in the rooms that may hold a rare stand)
    if stand_eligible(name):
        room.stand_slot = DR.stand_slot(room)
    weights = PANELS.get(fam, PANELS["other"])
    kinds = [k for k, wt in weights.items() for _ in range(wt)]
    color = room.rng.choice(["red", "black", "purple", "blue", "green", "orange"])
    for side in ("n", "s", "w", "e"):
        w = W.Wall(room, side)
        doors = w.door_lats()
        for k in range(w.m):
            c = 9 * k + 4
            if any(abs(c - d) <= 3 for d in doors):
                continue
            for attempt in range(4):
                kind_k = room.rng.choice(kinds)
                if W.niche(room, side, c, kind_k, room.rng, color, rc["soul"]):
                    break
    if rc["crown"]:                          # the crowns last: their banners are block entities and take what is left of the room's budget
        for side in ("n", "s", "w", "e"):
            for lat in W.Wall(room, side).door_lats():
                W.door_crown(room, side, lat, color)
    # lights
    y_lamp = 16 + room.rng.randrange(0, 2)
    LT.scheme(room, rc["light"], y_lamp, False)
    if rc["plight"]:
        LT.pilaster_lights(room, rc["soul"], every=2)
    if rc["posts"] and restraint < 1:
        LT.posts(room, rc["soul"])
    LT.balance(room, 6, y_lamp, False)
    # wear
    wr = rc["wear"]
    DR.crack(room, wall=wr["crack"], floor=wr["crack"] * 0.8)
    if wr["moss"]:
        DR.moss(room, patches=wr["moss"])
    DR.cobwebs(room, wr["cobweb"])
    if wr["rubble"] and restraint < 1:
        DR.rubble(room, wr["rubble"])
    if wr["broken"]:
        DR.broken_ribs(room, wr["broken"])
    # armour stands: none carried over; one empty slot in the rooms that may hold one
    DR.strip_stands(room, prop=room.rng.choice(["candles", "lantern", "skull"]))
    return room


def explicit_defaults(t):
    """Writes the default `type` / `half` / `open` into slabs, stairs and trapdoors that lack them (see mega_decor_r1.Hall.explicit_defaults)."""
    for pos, c in list(t.cells.items()):
        p = C.path_of(c.block)
        add = {}
        if p.endswith("_slab") and "type" not in c.props:
            add["type"] = "bottom"
        elif p.endswith("_stairs") and "half" not in c.props:
            add["half"] = "bottom"
        elif p.endswith("_trapdoor"):
            if "half" not in c.props:
                add["half"] = "bottom"
            if "open" not in c.props:
                add["open"] = "false"
        if add:
            t.cells[pos] = type(c)(c.block, dict(c.props, **add), c.nbt)


def finish(room):
    explicit_defaults(room.t)
    L.autoconnect(room.t)
    return room
