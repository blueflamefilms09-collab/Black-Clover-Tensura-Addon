"""Why: the characters of the arcs already have bespoke GeoLite models (tools/arc/char_*.py, assets/nusmp/geo/entity/arc_<id>.geo.json) with wings, horns, tails, halos, capes and
weapons. The owner wants every humanoid on the player model, so the body, face, outfit and hair are replaced by the player model with a painted skin, and what is NOT body (the "extras")
is carried over from those models: this script cuts every extra bone out of an arc model, anchors it on the player-model part it belongs to (head, body, right or left arm), moves and
scales it from the arc rig into the player rig, and writes it as assets/nusmp/geo/entity/pmx_<id>.geo.json (+ animations/entity/pmx_<id>.animation.json). The textures are the arc model's own
(arc_<id>.png and its _glow.png), so nothing is repainted. Run:
    python3 -B tools/playermodel/extract_extras.py [id ...]          (no id: every humanoid of tools/playermodel/characters.py)

The file's root bones are named x_head, x_body, x_arm_r, x_arm_l (client/playermodel/ExtrasLayer draws each root with the model part of that name). Which bones are extras: everything that is
not the arc rig's own body (root, body, head, arms, forearms, hands, legs, shins, skirts, the old hair). The anchor of an extra is its nearest ancestor part: below `head` is the head, below a
hand / forearm / arm is that arm, anything else the body.

The transform: points (file frame: pixels, y up, front -z, +x the model's left) are moved by  p' = V + k * (p - A)  where A is the arc part's pivot, V the player model's pivot of that part, and
k = 2 / H (H = the arc model's height in blocks), because the kit draws the player model at H / 2 times its own size, so an extra that is 1 / s bigger in model pixels is the same size in the
world. The head and body anchors use the arc rig's neck joint as A (the arc body bone pivots at the hips, the player body at the neck).
"""
import copy
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
GEO = os.path.join(ASSETS, "geo", "entity")
ANIM = os.path.join(ASSETS, "animations", "entity")

RIG = {"root", "body", "head", "arm_l", "forearm_l", "hand_l", "leg_l", "shin_l", "skirt_l", "arm_r", "forearm_r", "hand_r", "leg_r", "shin_r", "skirt_r", "hair", "hair_back",
       "hair_tail_l", "hair_tail_r"}
SKIP = {"hair", "hair_back", "hair_tail_l", "hair_tail_r", "skirt_l", "skirt_r"}
# the vanilla player's part pivots in the file frame (y up, 24 px = the neck line): head, body, right arm, left arm (wide arms; slim moves the y by 0.5)
HOLD_ARM = -0.42       # radians the arm that holds a tilted prop is raised forward
VANILLA = {"head": (0.0, 24.0, 0.0), "body": (0.0, 24.0, 0.0), "arm_r": (-5.0, 22.0, 0.0), "arm_l": (5.0, 22.0, 0.0)}


# how far (degrees about x) a held prop is turned up out of the forward pointing rest pose the art kit gave it: the player model's arm hangs, the kit's arms were bent to the hold
PROP_TILT = {"sword": 38, "katana": 38, "dagger": 22, "greatsword": 52, "axe": 52, "rhitta": 52, "hammer": 46, "spear": 58, "staff": 62, "witch_staff": 62, "courechouse": 58, "wand": 30,
             "bow": 0, "orb": 0, "grimoire": 0, "broom": 40}


def prop_kinds(model):
    """{'prop_r': kind, 'prop_l': kind} of a model name ('escanor', 'escanor_form2') from its character sheet."""
    try:
        import characters
        base = model[:-6] if model.endswith("_form2") else model
        form = 2 if model.endswith("_form2") else 1
        spec = characters.normalized(base, form)
        props = spec.get("props") or {}
        return {"prop_r": (props.get("right") or {}).get("kind"), "prop_l": (props.get("left") or {}).get("kind")}
    except Exception:
        return {}


TEX = os.path.join(ASSETS, "textures", "entity")


def copy_texture(src_stem, dst_stem):
    """pmx_<name>.png (+ _glow) as a copy of the arc model's texture: the geo files of the mod follow the convention 'a model's texture has the model's name' (GeoAssetsTest)."""
    import shutil
    ok = False
    for suffix in ("", "_glow"):
        src = os.path.join(TEX, src_stem + suffix + ".png")
        if os.path.isfile(src):
            shutil.copyfile(src, os.path.join(TEX, dst_stem + suffix + ".png"))
            ok = True
    return ok


def load(name):
    p = os.path.join(GEO, name + ".geo.json")
    if not os.path.isfile(p):
        return None
    return json.load(open(p))


def anchor_of(bones, name):
    """The anchor part of a bone: walk the parents; None for a bone that belongs to a leg or a skirt (not carried over)."""
    cur = name
    seen = 0
    while cur and seen < 40:
        seen += 1
        if cur == "head":
            return "head"
        if cur in ("arm_r", "forearm_r", "hand_r"):
            return "arm_r"
        if cur in ("arm_l", "forearm_l", "hand_l"):
            return "arm_l"
        if cur in ("leg_l", "leg_r", "shin_l", "shin_r", "skirt_l", "skirt_r"):
            return None
        if cur in ("body", "root"):
            return "body"
        cur = bones[cur].get("parent")
    return "body"


def extras_of(bones):
    out = []
    for n in bones:
        if n in RIG or n in SKIP:
            continue
        if anchor_of(bones, n) is None:
            continue
        out.append(n)
    return out


def expand_box_uv(c):
    """A box-format uv [u, v] as the per-face form (the sizes change when the cube is scaled, the texture rectangles must not)."""
    uv = c.get("uv")
    if not isinstance(uv, list):
        return c.get("uv")
    u, v = uv[0], uv[1]
    w, h, d = c["size"]
    return {"east": {"uv": [u, v + d], "uv_size": [d, h]}, "north": {"uv": [u + d, v + d], "uv_size": [w, h]}, "west": {"uv": [u + d + w, v + d], "uv_size": [d, h]},
            "south": {"uv": [u + 2 * d + w, v + d], "uv_size": [w, h]}, "up": {"uv": [u + d, v], "uv_size": [w, d]}, "down": {"uv": [u + d + w, v], "uv_size": [w, d]}}


def tf_point(p, A, V, k):
    return [round(V[i] + k * (p[i] - A[i]), 4) for i in range(3)]


def extract(model, out_name=None, height=None):
    """Writes pmx_<model>. Returns the info dict, or None when the model has no extras."""
    g = load("arc_" + model)
    if g is None:
        return None
    geo = g["minecraft:geometry"][0]
    desc = geo["description"]
    bones = {b["name"]: b for b in geo["bones"]}
    ex = extras_of(bones)
    if not ex:
        return None
    manifest = json.load(open(os.path.join(ROOT, "tools", "arc", "manifest.json")))["models"]
    h_blocks = height or manifest.get("arc_" + model.replace("arc_", ""), manifest.get(model, {})).get("height_blocks") or 2.0
    k = 2.0 / h_blocks
    neck = bones["head"]["pivot"]
    ref = {"head": neck, "body": neck, "arm_r": bones["arm_r"]["pivot"], "arm_l": bones["arm_l"]["pivot"]}
    out_bones = []
    kept = set()
    roots = {}
    # which ancestors have to be kept as empty bones so an extra hangs from the right joint
    chains = {}
    exset = set(ex)
    for n in ex:
        a = anchor_of(bones, n)
        chain = []
        cur = bones[n].get("parent")
        while cur and cur not in ("head", "body", "root", "arm_r", "arm_l") and cur not in exset:
            chain.append(cur)
            cur = bones[cur].get("parent")
        chains[n] = (a, list(reversed(chain)), cur if cur in exset else None)
    anchors = sorted({c[0] for c in chains.values()}, key=lambda a: ("head", "body", "arm_r", "arm_l").index(a))
    for a in anchors:
        roots[a] = "x_" + a
        out_bones.append({"name": "x_" + a, "pivot": list(VANILLA[a]), "cubes": []})
    emitted = set(roots.values())
    infos = []
    kinds_of = prop_kinds(model)
    holds = {}
    for n in ex:
        a, chain, extra_parent = chains[n]
        A, V = ref[a], VANILLA[a]
        parent = extra_parent if extra_parent and not chain else (roots[a] if not extra_parent else extra_parent)
        for c in chain:                                  # the empty joint bones between the anchor and the extra
            nm = c
            if nm not in emitted:
                b = bones[c]
                out_bones.append({"name": nm, "parent": parent, "pivot": tf_point(b["pivot"], A, V, k), "cubes": [], **({"rotation": b["rotation"]} if b.get("rotation") else {})})
                emitted.add(nm)
            parent = nm
        b = copy.deepcopy(bones[n])
        if n in ("prop_r", "prop_l") and n in kinds_of:
            tilt = PROP_TILT.get(kinds_of[n] or "", 0)
            if tilt:
                rot = list(b.get("rotation") or [0, 0, 0])
                rot[0] = rot[0] + tilt
                b["rotation"] = rot
                holds[a] = HOLD_ARM
        nb = {"name": n, "parent": parent, "pivot": tf_point(b["pivot"], A, V, k)}
        if parent is None:
            del nb["parent"]
        if b.get("rotation"):
            nb["rotation"] = b["rotation"]
        cubes = []
        for c in b.get("cubes", []):
            nc = dict(c)
            nc["uv"] = expand_box_uv(c)
            nc["origin"] = tf_point(c["origin"], A, V, k)
            nc["size"] = [round(s * k, 4) for s in c["size"]]
            if "pivot" in c:
                nc["pivot"] = tf_point(c["pivot"], A, V, k)
            if "inflate" in c:
                nc["inflate"] = round(c["inflate"] * k, 4)
            cubes.append(nc)
        nb["cubes"] = cubes
        out_bones.append(nb)
        emitted.add(n)
        infos.append((n, a, len(cubes)))
    # parents of extras that are themselves extras must be written before their children: a stable sort by depth
    names = {b["name"]: b for b in out_bones}

    def depth(b):
        d = 0
        cur = b
        while cur.get("parent") and cur["parent"] in names and d < 50:
            cur = names[cur["parent"]]
            d += 1
        return d

    out_bones.sort(key=depth)
    stem = out_name or ("pmx_" + model)
    ng = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {**desc, "identifier": "geometry." + stem}, "bones": out_bones}]}
    os.makedirs(GEO, exist_ok=True)
    json.dump(ng, open(os.path.join(GEO, stem + ".geo.json"), "w"), indent=1)
    # animations: only the tracks of the extras, positions scaled
    ap = os.path.join(ANIM, "arc_" + model + ".animation.json")
    if os.path.isfile(ap):
        anim = json.load(open(ap))
        outa = {"format_version": anim.get("format_version", "1.8.0"), "animations": {}}
        keep = set(ex)
        for cname, clip in anim["animations"].items():
            nclip = {k_: v for k_, v in clip.items() if k_ != "bones"}
            nb = {}
            for bn, tr in clip.get("bones", {}).items():
                if bn not in keep:
                    continue
                t2 = {}
                for kind, track in tr.items():
                    if kind == "position":
                        t2[kind] = scale_track(track, k)
                    else:
                        t2[kind] = track
                nb[bn] = t2
            nclip["bones"] = nb
            outa["animations"][cname] = nclip
        os.makedirs(ANIM, exist_ok=True)
        json.dump(outa, open(os.path.join(ANIM, stem + ".animation.json"), "w"), indent=1)
    copy_texture("arc_" + model, stem)
    return {"stem": stem, "anchors": anchors, "bones": infos, "k": k, "height": h_blocks, "holds": holds}


def scale_track(track, k):
    def sv(v):
        if isinstance(v, list):
            return [round(x * k, 4) if isinstance(x, (int, float)) else x for x in v]
        if isinstance(v, dict):
            return {kk: sv(vv) for kk, vv in v.items()}
        if isinstance(v, (int, float)):
            return round(v * k, 4)
        return v

    if isinstance(track, dict):
        return {t: sv(v) for t, v in track.items()}
    return sv(track)


def main():
    sys.path.insert(0, HERE)
    ids = sys.argv[1:]
    if not ids:
        import characters
        ids = characters.extras_models()
    done = 0
    for i in ids:
        info = extract(i)
        if info is None:
            print("%-28s no extras" % i)
            continue
        done += 1
        print("%-28s %s k=%.3f %s" % (i, info["stem"], info["k"], ", ".join("%s(%s,%d)" % b for b in info["bones"])))
    print("wrote %d extras models" % done)


if __name__ == "__main__":
    main()
