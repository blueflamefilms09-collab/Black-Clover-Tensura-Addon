"""spec: what an item of the slice is made of, and how a spec becomes files.

An `Item` names the builder function (fills a `VU`), the star texture style, the way the hand model becomes the worn model, the hold style in the hand, the ambient layers
(for the fx json) and the signature effect of each act (for act_vfx.txt). `bake(item)` builds the full model and the lod model, writes both skins, glow maps, star texture
and model files, and returns the numbers the checks and the report use.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import fxtex  # noqa: E402
import metrics as MT  # noqa: E402
import roster as R  # noqa: E402
import vu as V  # noqa: E402

CAPS = {"GOD": {"mesh_vertices": 2500, "drawn_vertices": 2500, "lod_ratio": 0.15, "skin": 256, "lod_skin": 64},
        "GENESIS": {"mesh_vertices": 6000, "drawn_vertices": 6000, "lod_ratio": 0.15, "skin": 512, "lod_skin": 96}}


class Item:
    def __init__(self, rid, build, star, hand_size=0.62, hand_style="palm", worn="orbit", worn_scale=0.5, worn_offset=(11.2, -8.0, 0.0), anchor="body", pieces=None,
                 layers=(), acts=None, sound=None, seed=1, extra_worn=None, skin=None):
        self.rid, self.build, self.star = rid, build, star
        self.hand_size, self.hand_style = hand_size, hand_style
        self.worn, self.worn_scale, self.worn_offset, self.anchor = worn, worn_scale, worn_offset, anchor
        self.pieces = pieces                  # for items whose worn pieces are more than one model: [(suffix, anchor, mirror)]
        self.layers = list(layers)
        self.acts = acts or {}
        self.sound = sound or "nusmp:relic.ambient.%s" % rid
        self.seed = seed
        self.extra_worn = extra_worn
        self.skin = skin


def make(item, lod):
    info = R.BY_ID[item.rid]
    grade = info[2]
    cap = CAPS[grade]
    vm = V.VU(item.rid + ("_lod" if lod else ""), lod=lod, skin=(cap["lod_skin"] if lod else (item.skin or cap["skin"])), seed=item.seed)
    item.build(vm, info)
    vm.build()
    return vm


def bake(item, verbose=True):
    """Builds, checks and writes one item. Returns a dict of numbers."""
    info = R.BY_ID[item.rid]
    grade = info[2]
    cap = CAPS[grade]
    pal = info[4]
    full = make(item, False)
    low = make(item, True)
    # textures
    skin_t, glow_t = full.write_textures(item.rid)
    low.write_textures(item.rid + "_lod")
    dark = tuple(max(0, int(c * 0.10)) for c in ((pal[0] >> 16) & 255, (pal[0] >> 8) & 255, pal[0] & 255))
    mid = tuple(int(c * 0.55) for c in ((pal[2] >> 16) & 255, (pal[2] >> 8) & 255, pal[2] & 255))
    hot = ((pal[1] >> 16) & 255, (pal[1] >> 8) & 255, pal[1] & 255)
    sd = item.star
    fxtex.save_star(os.path.join(V.TEX_DIR, item.rid + "_star.png"), sd.get("dark", dark), sd.get("mid", mid), sd.get("hot", hot), sd.get("style", "nebula"), item.seed * 17 + 3)
    # models
    fs = full.stats()
    ls = low.stats()
    errs = []
    if fs["vertices"] > cap["mesh_vertices"]:
        errs.append("mesh %d vertices over the cap %d" % (fs["vertices"], cap["mesh_vertices"]))
    if fs["drawn"] > cap["drawn_vertices"]:
        errs.append("drawn %d vertices over %d" % (fs["drawn"], cap["drawn_vertices"]))
    if ls["vertices"] > cap["lod_ratio"] * fs["vertices"]:
        errs.append("lod %d vertices is %.1f%% of %d (cap %d%%)" % (ls["vertices"], 100.0 * ls["vertices"] / fs["vertices"], fs["vertices"], int(cap["lod_ratio"] * 100)))
    star_name = item.rid + "_star.png"
    sc, off = (item.worn_scale, item.worn_offset) if item.worn == "orbit" else (1.0, (0, 0, 0))
    full.write_model(item.rid, item.rid, anchor=item.anchor if item.worn != "orbit" else "body", star_tex=star_name, lod_name=item.rid + "_lod", comment=grade)
    low.write_model(item.rid + "_lod", item.rid + "_lod", anchor=item.anchor if item.worn != "orbit" else "body", star_tex=star_name, comment=grade + " lod")
    if item.worn == "orbit":
        full.write_model(item.rid + "_worn", item.rid, anchor="body", scale=sc, offset=off, star_tex=star_name, lod_name=item.rid + "_worn_lod", comment=grade + " worn")
        low.write_model(item.rid + "_worn_lod", item.rid + "_lod", anchor="body", scale=sc, offset=off, star_tex=star_name, comment=grade + " worn lod")
    out = {"id": item.rid, "full": fs, "lod": ls, "skin_texels": skin_t, "glow_texels": glow_t, "errors": errs, "groups": [g["name"] for g in full.groups[1:]],
           "group_kinds": sorted({g["kind"] for g in full.groups[1:]})}
    if verbose and os.environ.get("VU_PARTS"):
        print("   parts:", ", ".join("%s=%d" % kv for kv in sorted(full.part_counts().items(), key=lambda kv: -kv[1])))
        print("   lod  :", ", ".join("%s=%d" % kv for kv in sorted(low.part_counts().items(), key=lambda kv: -kv[1])))
    if verbose:
        print("%-42s quads %4d (glowmap %3d star %3d halo %3d) drawn %5d verts, lod %3d quads (%4.1f%%), groups %2d, skin %6d texels %s" % (
            item.rid, fs["quads"], fs["glowmap"], fs["star"], fs["halo"], fs["drawn"], ls["quads"], 100.0 * ls["vertices"] / max(1, fs["vertices"]), fs["groups"], skin_t,
            ("ERRORS: " + "; ".join(errs)) if errs else ""))
    return out


def geo_layer(name, typ, group, views=("hand", "worn")):
    """A geometry layer: a named animation group of the model draws it (a ring, shards, shafts, a portal ...)."""
    return {"name": name, "kind": "geometry", "type": typ, "group": group, "views": list(views)}


def part_layer(name, typ, particle, color, count=1, every=6, radius=0.35, rise=0.01, speed=0.0, dy=0.0, views=("hand", "worn")):
    """A particle layer spawned in the world while the relic is held or worn (vanilla particle id, 0xRRGGBB colour for a dust layer)."""
    d = {"name": name, "kind": "particle", "type": typ, "particle": particle, "count": count, "every": every, "radius": radius, "rise": rise, "views": list(views)}
    if color is not None:
        d["color"] = "%06X" % color
    if speed:
        d["speed"] = speed
    if dy:
        d["dy"] = dy
    return d


def step(shape, rgb, power, dur, where):
    """One act effect (relics/vu/VuActTable step)."""
    return "%s:%06X:%g:%d:%s" % (shape, rgb, power, dur, where)


def fx_json(item):
    return {"id": item.rid, "ambient_sound": item.sound, "draw_ticks": 12, "layers": [dict(l) for l in item.layers]}


def hand_line(item):
    """The `hand` and `worn` lines of overrides.txt."""
    rid = item.rid
    if item.worn == "orbit":
        hand = "hand %s vu/%s | size=%.2f style=%s" % (rid, rid, item.hand_size, item.hand_style)
        worn = "worn %s vu/%s_worn" % (rid, rid)
    else:
        parts = " ".join("vu/%s%s@%s%s" % (item.rid, suf, anc, "~" if mir else "") for (suf, anc, mir) in item.pieces)
        hand = "hand %s %s | size=%.2f style=%s" % (rid, parts, item.hand_size, item.hand_style)
        worn = "worn %s %s" % (rid, parts)
    return hand, worn


def act_lines(item):
    out = []
    for key, steps in item.acts.items():
        out.append("act %s %s %s" % (item.rid, key, " ".join(steps)))
    return out
