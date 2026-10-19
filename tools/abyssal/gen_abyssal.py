"""Why: builds every abyssal boss look (docs/labyrinth/abyssal_bosses.md) from its script tools/abyssal/look_<id>.py:

    python3 -B tools/abyssal/gen_abyssal.py                 # every look (the build is deterministic: the same script gives the same bytes)
    python3 -B tools/abyssal/gen_abyssal.py lich vecna      # only these
    python3 -B tools/abyssal/gen_abyssal.py --list
    python3 -B tools/abyssal/gen_abyssal.py --check         # model checks only (bones linked, uv inside the sheet, animation bones exist, texture + glow exist)

For each look and each of the three phases it writes
    assets/nusmp/geo/entity/abyssal_<id>_p<n>.geo.json            the model the boss renderer draws today (a humanoid is the PLAYER body with its skin plus the extras on it)
    assets/nusmp/textures/entity/abyssal_<id>_p<n>[_glow].png      its atlas (a humanoid's skin sits in the top-left 64x64)
and, once per look, assets/nusmp/animations/entity/abyssal_<id>.animation.json (the three phases share the clips).
A HUMANOID additionally writes the parts the player-model renderer will take over:
    textures/entity/abyssal/skin_<id>_p<n>[_glow].png             a real 64x64 player skin (classic arms unless the table says slim) and its glow skin
    geo/entity/abyssal_<id>_extras_p<n>.geo.json (+ png, _glow)   only the extras (crown, phylactery, wings ...) on anchor bones named after the player's parts
The one table that lists all of it is assets/nusmp/abyssal/looks.json (look id -> kind, models, skin, slim flag, extras with their anchors, glow, sizes), read by the game's AbyssalLooks.
Preview scenes go to tools/render_preview/scenes.d/abyssal_<id>.json (python3 -B tools/render_preview/preview.py abyssal_<id>_front ...).
"""
import importlib
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True
import lib  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = lib.ROOT
SCENES = os.path.join(REPO, "tools", "render_preview", "scenes.d")
TABLE = os.path.join(ASSETS, "abyssal", "looks.json")
KIT_TABLE = os.path.join(ASSETS, "playermodel", "looks_abyssal.json")     # the humanoids, in the format of the player-model kit (assets/nusmp/playermodel/looks.json)
LANG = os.path.join(REPO, "tools", "lang", "abyssal_bosses.json")        # a flat fragment of the lang keys (the lead merges it into en_us.json): look names and the caption of each phase change
ORDER = ["lich", "acererak", "vecna", "strahd", "zariel", "asmodeus", "orcus", "lich_king", "solar",
         "demogorgon", "beholder", "elder_brain", "kraken", "tiamat", "bahamut", "dracolich", "tarrasque", "lolth",
         "quasit", "vrock", "hezrou", "balor"]
CLIPS = ["idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "death"]
# an evil boss of a sector whose profile names no look wears the demon of its tier on the sectors where the demon faction rules (data/nusmp/mega/factions/demon.json weights abyss, blood, flame and
# infernal_foundry at 5, the highest): the Abyssal road's own bosses, as the Abyss encounter tables' demons are its mobs
_DEMONS = ["quasit", "vrock", "hezrou", "balor", "balor"]
THEME_DEFAULTS = {t: {"evil": _DEMONS} for t in ("abyss", "blood", "flame", "infernal_foundry")}


def bounds(model_id):
    """The model's box in blocks from the cubes' own corners (rotations ignored, pivots ignored): the numbers for culling."""
    g = json.load(open(os.path.join(ASSETS, "geo", "entity", model_id + ".geo.json")))["minecraft:geometry"][0]
    lo, hi = [1e9] * 3, [-1e9] * 3
    n = 0
    for b in g["bones"]:
        for c in b["cubes"]:
            n += 1
            o, s = c["origin"], c["size"]
            for i in range(3):
                lo[i] = min(lo[i], o[i] - c.get("inflate", 0))
                hi[i] = max(hi[i], o[i] + s[i] + c.get("inflate", 0))
    return [v / 16 for v in lo], [v / 16 for v in hi], n


def check(model_ids, anim_id):
    """The checks of tools/geo_check.py plus: the shared animation names only bones that exist in at least one phase, every cube face lies inside the sheet."""
    from PIL import Image
    problems = []
    all_bones = set()
    for mid in model_ids:
        g = json.load(open(os.path.join(ASSETS, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
        tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
        names = {b["name"] for b in g["bones"]}
        all_bones |= names
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append("%s: bone %s has the missing parent %s" % (mid, b["name"], b["parent"]))
            for c in b["cubes"]:
                uv = c["uv"]
                if isinstance(uv, dict):
                    for f in uv.values():
                        x, y = f["uv"]
                        w, h = f["uv_size"]
                        if x < 0 or y < 0 or x + w > tw or y + h > th:
                            problems.append("%s: bone %s face outside the sheet %s" % (mid, b["name"], f))
                            break
        for suffix in (".png", "_glow.png"):
            p = os.path.join(ASSETS, "textures", "entity", mid + suffix)
            if not os.path.exists(p):
                problems.append("missing " + p)
            else:
                im = Image.open(p)
                if im.size != (tw, th):
                    problems.append("%s%s is %s, the model says %dx%d" % (mid, suffix, im.size, tw, th))
    ap = os.path.join(ASSETS, "animations", "entity", "abyssal_%s.animation.json" % anim_id)
    if not os.path.exists(ap):
        problems.append("missing " + ap)
    else:
        an = json.load(open(ap))["animations"]
        for cl in CLIPS:
            if cl not in an:
                problems.append("animation %s has no clip %s" % (anim_id, cl))
        for cn, cl in an.items():
            for bn in cl["bones"]:
                if bn not in all_bones:
                    problems.append("animation %s clip %s: bone %s is in no phase" % (anim_id, cn, bn))
    return problems


def check_extras(xname):
    """An extras file of the player-model kit: roots are x_<part>, a texture and glow of the sheet's size, and its own animation file."""
    from PIL import Image
    probs = []
    g = json.load(open(os.path.join(ASSETS, "geo", "entity", xname + ".geo.json")))["minecraft:geometry"][0]
    names = {b["name"] for b in g["bones"]}
    for b in g["bones"]:
        if not b.get("parent") and b["name"] not in ("x_head", "x_body", "x_arm_r", "x_arm_l", "x_leg_r", "x_leg_l"):
            probs.append("%s: root bone %s is not x_<part>" % (xname, b["name"]))
        if b.get("parent") and b["parent"] not in names:
            probs.append("%s: bone %s has the missing parent %s" % (xname, b["name"], b["parent"]))
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    for suffix in (".png", "_glow.png"):
        p = os.path.join(ASSETS, "textures", "entity", xname + suffix)
        if not os.path.exists(p) or Image.open(p).size != (tw, th):
            probs.append("%s%s missing or not %dx%d" % (xname, suffix, tw, th))
    if not os.path.exists(os.path.join(ASSETS, "animations", "entity", xname + ".animation.json")):
        probs.append(xname + " has no animation file")
    return probs


def scenes_for(look, models, tall):
    """Preview scenes: front and 3/4, back and side, the face, then a strip per clip (phase 1), then the three phases next to each other."""
    sc = {}
    anim = "animations/entity/abyssal_%s.animation.json" % look
    base = {"kind": "geo", "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "floor": True, "figure": False, "expect": [], "animations": anim}
    cy, dist = tall * 0.5, max(3.6, tall * 2.5)

    def one(name, model, clip, ages, views, tile):
        d = dict(base)
        d.update({"model": "geo/entity/abyssal_%s.geo.json" % model, "texture": "textures/entity/abyssal_%s.png" % model, "anim": clip, "ages": ages, "tile": tile, "views": views})
        sc[name] = d

    def view(n, yaw, pitch, dist_=None, ty=None):
        return {"name": n, "yaw": yaw, "pitch": pitch, "target": [0, round(ty if ty is not None else cy, 3), 0], "dist": round(dist_ or dist, 3)}
    for i, m in enumerate(models, start=1):
        p = "abyssal_%s_p%d" % (look, i)
        one(p + "_front", m, "idle", [0], [view("front", 0, 4), view("front 3/4", 35, 8)], [420, 560])
        one(p + "_back", m, "idle", [0], [view("back", 180, 5), view("side", 90, 4)], [420, 560])
        one(p + "_face", m, "idle", [0], [view("face", 0, 2, 2.4 + tall * 0.2, tall * 0.88), view("face 3/4", 32, 6, 2.4 + tall * 0.2, tall * 0.88)], [420, 420])
    ages = {"idle": [0, 16, 32, 48], "walk": [0, 5.5, 11, 16.5], "run": [0, 3.1, 6.2, 9.3], "attack": [0, 3.6, 7.2, 10.8, 14.4], "cast": [0, 6, 12, 18],
            "cast_big": [0, 9.6, 19.2, 28.8, 38.4], "hurt": [0, 1.8, 3.6, 5.4, 7.2], "death": [0, 11.2, 22.4, 33.6, 44.8]}
    for clip in CLIPS:
        one("abyssal_%s_%s" % (look, clip), models[0], clip, ages[clip], [view("3/4", 30, 6, dist * 1.15), view("side", 90, 4, dist * 1.15)] if clip in ("attack", "cast_big") else [view("3/4", 30, 6, dist * 1.15)], [340, 480])
    for i, m in enumerate(models, start=1):
        one("abyssal_%s_p%d_cast" % (look, i), m, "cast", [0, 8], [view("3/4", 30, 6, dist * 1.15)], [340, 480])
    return sc


def build(look_id, mod):
    L = dict(mod.LOOK)
    models, phases, extras_cubes, known, extra_files = [], [], 0, set(), []
    humanoid = L["kind"] == "humanoid"
    for phase in (1, 2, 3):
        c = mod.build(phase)
        img, glow = c.finish()
        c.save(img, glow)
        entry = {"model": c.name, "caption": L.get("captions", ["", "", ""])[phase - 1]}
        if humanoid:
            kid = "abyssal_%s_p%d" % (look_id, phase)                            # the id of this phase in the player-model kit's table (looks_abyssal.json)
            getattr(c, "kit_skin", c.skin).save_skin(kid)                       # textures/entity/playermodel/<kid>.png + _glow
            xname = "pmx_" + kid                                                # the extras: geo/entity/pmx_<kid>, roots x_head x_body x_arm_r x_arm_l
            extras_cubes, xbones = c.save_extras(img, glow, xname)
            extra_files.append((xname, set(xbones)))
            entry["skin"] = "playermodel/" + kid
            entry["extras_model"] = xname
            entry["extras"] = c.extras
        models.append(c.name)
        known |= set(c.m.bones)
        phases.append(entry)
    mod.RIG.write(look_id, only=known)
    for xname, xbones in extra_files:                                           # the extras' own clips: only the tracks of the bones the extras file has
        mod.RIG.write(look_id, only=xbones, filename=xname)
    lo, hi, n = bounds("abyssal_" + models[-1])
    height = L["height"]
    check_problems = check(["abyssal_" + m for m in models], look_id) + [pr for p in phases if humanoid for pr in check_extras(p["extras_model"])]
    grow_xz = round(max(abs(lo[0]), abs(hi[0]), abs(lo[2]), abs(hi[2])) * 1.2 + 0.3, 2)
    grow_up = round(max(0.3, hi[1] - height) * 1.2 + 0.3, 2)
    row = {"name": L["name"], "kind": L["kind"], "height": height, "walk_cycle": L.get("walk_cycle", 1.3 if humanoid else 2.0), "run_cycle": L.get("run_cycle", 2.3 if humanoid else 3.4),
           "shadow": L.get("shadow", 0.5), "grow_xz": L.get("grow_xz", grow_xz), "grow_up": L.get("grow_up", grow_up), "boost": L.get("boost", 1.0), "glow": L["glow"],
           "dust": bool(L.get("dust", False)), "lift": L.get("lift", 0.0), "animation": look_id, "phases": phases,
           "glow_level": L.get("glow_level", [0.75, 0.9, 1.0]), "cubes": n}
    if humanoid:
        row["humanoid"] = {"slim": bool(L.get("slim", False)), "part_scale": L.get("part_scale", {}), "hair": L.get("hair", ""), "hair_color": L.get("hair_color", ""),
                           "hold_r": L.get("hold_r", 0.0), "hold_l": L.get("hold_l", 0.0)}
    scenes = scenes_for(look_id, models, hi[1])
    with open(os.path.join(SCENES, "abyssal_%s.json" % look_id), "w") as f:
        json.dump(scenes, f, indent=1)
    return row, check_problems, n


def main(argv):
    ids = [a for a in argv if not a.startswith("--")]
    if "--list" in argv:
        print("\n".join(ORDER))
        return 0
    table = {"schema": 1, "looks": {}}
    if os.path.exists(TABLE):
        table = json.load(open(TABLE))
    bad = 0
    for look_id in ORDER:
        if ids and look_id not in ids:
            continue
        path = os.path.join(HERE, "look_%s.py" % look_id)
        if not os.path.exists(path):
            print("(no script yet for %s)" % look_id)
            continue
        mod = importlib.import_module("look_%s" % look_id)
        if "--check" in argv:
            table_row = table["looks"].get(look_id)
            if not table_row:
                continue
            probs = check(["abyssal_" + p["model"] for p in table_row["phases"]], look_id)
        else:
            row, probs, n = build(look_id, mod)
            table["looks"][look_id] = row
            print("%-12s %3d cubes, 3 phases" % (look_id, n))
        for p in probs:
            print("  PROBLEM", p)
            bad = 1
    order = {k: i for i, k in enumerate(ORDER)}
    table["looks"] = {k: table["looks"][k] for k in sorted(table["looks"], key=lambda k: order.get(k, 99))}
    table["theme_defaults"] = THEME_DEFAULTS
    kit = {"version": 1, "note": "generated by tools/abyssal/gen_abyssal.py: the humanoid abyssal bosses in the format of playermodel/looks.json (skin, slim, glow, hair, scale, extras, arm hold); do not edit by hand", "looks": {}}
    for k, row in table["looks"].items():
        if row["kind"] != "humanoid":
            continue
        h = row["humanoid"]
        for i, ph in enumerate(row["phases"], start=1):
            kid = "abyssal_%s_p%d" % (k, i)
            e = {"skin": kid, "slim": bool(h["slim"]), "scale": round(row["height"] / 1.875, 4)}
            if h.get("hold_r"):
                e["hold_r"] = h["hold_r"]
            if h.get("hold_l"):
                e["hold_l"] = h["hold_l"]
            e["glow"] = True
            if h.get("hair"):
                e["hair"] = h["hair"]
                e["hair_color"] = h["hair_color"]
            e["extras"] = [{"model": ph["extras_model"], "texture": ph["extras_model"], "glow": True}]
            kit["looks"][kid] = e
    if "--check" not in argv:
        lang = {}
        for k, row in table["looks"].items():
            lang["entity.nusmp.abyssal." + k] = row["name"]
            for i, ph in enumerate(row["phases"], start=1):
                if ph.get("caption"):
                    lang["message.nusmp.mega.dnd.look.%s.p%d" % (k, i)] = ph["caption"]
        os.makedirs(os.path.dirname(LANG), exist_ok=True)
        with open(LANG, "w") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False)
            f.write("\n")
        os.makedirs(os.path.dirname(KIT_TABLE), exist_ok=True)
        with open(KIT_TABLE, "w") as f:
            json.dump(kit, f, indent=1)
            f.write("\n")
        os.makedirs(os.path.dirname(TABLE), exist_ok=True)
        with open(TABLE, "w") as f:
            json.dump(table, f, indent=1)
    return bad


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
