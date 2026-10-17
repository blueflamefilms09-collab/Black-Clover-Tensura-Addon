"""Why: builds every character of the arcs from its spec file. A character is one file tools/arc/char_<id>.py (a dict SPEC, optionally a function extra(kit, rig));
this driver discovers them all, builds each one with tools/gen_arc_kit.py into

    src/main/resources/assets/nusmp/geo/entity/arc_<id>.geo.json              the model
    src/main/resources/assets/nusmp/animations/entity/arc_<id>.animation.json  the clips (idle walk run attack cast cast_big hurt transform death [spawn] [chest_gem_reveal])
    src/main/resources/assets/nusmp/textures/entity/arc_<id>.png               the texture
    src/main/resources/assets/nusmp/textures/entity/arc_<id>_glow.png          the glow layer (additive, full bright)
    tools/render_preview/scenes.d/arc_<id>.json                                the preview scenes (python3 -B tools/render_preview/preview.py arc_<id>_front ...)
    tools/arc/manifest.json                                                    facts of every model (size, hitbox, stride, clips, bones): what ArcLook and the entity need

and, for a spec with a `form2` section, the same files for arc_<id>_form2 (the second form: the spec with the overrides laid over it).

    python3 -B tools/gen_arc_characters.py                  # every character
    python3 -B tools/gen_arc_characters.py heath_grice      # only the named ones
    python3 -B tools/gen_arc_characters.py --check          # rebuild into a temporary folder and fail if any committed file differs (the build is deterministic)
    python3 -B tools/gen_arc_characters.py --list           # the ids and what each is
    python3 -B tools/gen_arc_characters.py --validate       # only read and check the specs (no files written)
    python3 -B tools/gen_arc_characters.py --preview heath_grice [front back face idle walk ...]    # build, then render the preview scenes (RENDER_PREVIEW_BUILD may be set)
    python3 -B tools/gen_arc_characters.py --selftest       # build every name of the vocabulary and check the clips against the rules of the kit (a few minutes)
    python3 -B tools/gen_arc_characters.py --gallery hair   # a contact sheet of one vocabulary (hair eyes tops legs builds props accessories extras all); --out DIR
"""
import glob
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import geo_builder  # noqa: E402
import gen_arc_kit as kit  # noqa: E402
from arc_kit import scenes as SC, spec as SP  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, ".."))
ARC_DIR = os.path.join(HERE, "arc")
SCENES_DIR = os.path.join(HERE, "render_preview", "scenes.d")
MANIFEST = os.path.join(ARC_DIR, "manifest.json")
ASSETS = geo_builder.ROOT


def discover(only=()):
    """{id: (path, module)} of every tools/arc/char_<id>.py (or only the ids asked for)."""
    out = {}
    for path in sorted(glob.glob(os.path.join(ARC_DIR, "char_*.py"))):
        cid = os.path.basename(path)[len("char_"):-3]
        if only and cid not in only:
            continue
        spec_ = importlib.util.spec_from_file_location("arc_char_" + cid, path)
        mod = importlib.util.module_from_spec(spec_)
        spec_.loader.exec_module(mod)
        if not hasattr(mod, "SPEC"):
            raise SystemExit("%s has no SPEC dict" % os.path.relpath(path, REPO))
        out[cid] = (path, mod)
    missing = [o for o in only if o not in out]
    if missing:
        raise SystemExit("no character file for: %s (looked for tools/arc/char_<id>.py)" % ", ".join(missing))
    return out


def asset_paths(root, mid):
    return {"geo": os.path.join(root, "geo", "entity", "arc_%s.geo.json" % mid), "animations": os.path.join(root, "animations", "entity", "arc_%s.animation.json" % mid),
            "texture": os.path.join(root, "textures", "entity", "arc_%s.png" % mid), "glow": os.path.join(root, "textures", "entity", "arc_%s_glow.png" % mid)}


def inspect(R, mid, root):
    """Reads the written files back and checks them: parents exist, every clip name is there, every track's bone exists, every cube face is inside the texture and not
    empty, the model stays inside a sane size. Returns the facts for the manifest and the list of problems."""
    from PIL import Image
    import numpy as np
    paths = asset_paths(root, mid)
    geo = json.load(open(paths["geo"]))["minecraft:geometry"][0]
    anim = json.load(open(paths["animations"]))["animations"]
    img, glow = Image.open(paths["texture"]), Image.open(paths["glow"])
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    problems = []
    if not (img.size == (tw, th) == glow.size):
        problems.append("texture size %s does not match the model's %s" % (img.size, (tw, th)))
    bones = {b["name"]: b for b in geo["bones"]}
    for b in geo["bones"]:
        if b.get("parent") and b["parent"] not in bones:
            problems.append("bone %s: parent %s missing" % (b["name"], b["parent"]))
    for cn in SC.CLIPS:
        if cn not in anim:
            problems.append("clip %s missing" % cn)
    for cn, c in anim.items():
        for bn in c["bones"]:
            if bn not in bones:
                problems.append("clip %s: bone %s not in the model" % (cn, bn))
        if c["animation_length"] <= 0:
            problems.append("clip %s: no length" % cn)
    ga = np.asarray(glow.convert("RGBA"))[..., 3]
    ca = np.asarray(img.convert("RGBA"))[..., 3]
    glow_bones = []
    faces = 0
    for b in geo["bones"]:
        has = False
        for c in b["cubes"]:
            for fk, f in c["uv"].items():
                faces += 1
                u, v = f["uv"]
                w, h = f["uv_size"]
                if u < 0 or v < 0 or u + w > tw or v + h > th:
                    problems.append("a %s face of a cube of %s is outside the texture" % (fk, b["name"]))
                    continue
                if ga[int(v):int(v + h), int(u):int(u + w)].any():
                    has = True
                if not ca[int(v):int(v + h), int(u):int(u + w)].any():
                    problems.append("a %s face of a cube of %s is fully transparent" % (fk, b["name"]))
        if has:
            glow_bones.append(b["name"])
    if not os.environ.get("ARC_FAST"):                          # the arm rules of the kit (see arc_kit/clipcheck.py), read from the written clips
        from arc_kit import clipcheck
        problems += ["arm rule: " + line for line in clipcheck.arm_problems(R)]
    return {"faces": faces, "glow_bones": glow_bones, "problems": problems, "clips": {k: {"length": c["animation_length"], "loop": c["loop"]} for k, c in anim.items()}}


def stride(R, clips):
    """Blocks of ground one cycle of the walk / run clip covers, from the thigh swing of the clip itself (two steps of 2 L sin(a))."""
    import math
    import numpy as np
    L = R.d.leg_len
    out = {}
    for name in ("walk", "run"):
        a = 0.0
        tr = R.A.clips.get(name, {}).get("bones", {}).get("leg_l", {}).get("rotation")
        if tr:
            a = max(abs(v[0]) for v in tr.values())
        out[name] = round(4.0 * L * math.sin(math.radians(min(80.0, a))) / 16.0, 2) if a else 0.0
    return out


def manifest_entry(R, mid, st, ins, S, form):
    lo, hi = SC.bounds(R)
    top = max(hi[1], R.d.top) / 16.0
    sc = stride(R, None)
    width = max(0.5, round(max(R.d.tw, R.d.td) / 16.0 * 1.25, 2))
    half = max(abs(lo[0]), abs(hi[0])) / 16.0
    deep = max(abs(lo[2]), abs(hi[2])) / 16.0
    height = round(R.d.top / 16.0, 2)
    return {"model": "arc_" + mid, "spec": 'GeoSpec.of("arc", "%s")' % mid, "form": form, "role": S["role"], "name": S["name"], "build": S["build"],
            "files": [os.path.relpath(p, REPO) for p in asset_paths(ASSETS, mid).values()], "texture": st["texture"], "cubes": st["cubes"], "faces_per_pass": ins["faces"],
            "height_blocks": height, "height_with_hair_and_horns_blocks": round(top, 2),
            "bounds_blocks": {"min": [round(v / 16.0, 2) for v in lo], "max": [round(v / 16.0, 2) for v in hi]},
            "recommended_hitbox_blocks": {"width": min(width, 1.2) if S["build"] != "huge" else 1.6, "height": height},
            "arc_look": {"height": height, "walkCycle": sc["walk"] or 1.4, "runCycle": sc["run"] or 2.4, "shadow": round(max(0.35, half * 0.7), 2),
                         "growXz": round(max(0.8, half + 0.5, deep + 0.5), 2), "growUp": round(max(0.4, top - height + 0.4), 2), "glow": True},
            "grip": R.facts.get("grip") or "", "notes": [R.facts["grip_note"]] if R.facts.get("grip_note") else [],
            "clips": ins["clips"], "bones": [b for b in R.m.order], "glow_bones": ins["glow_bones"], "problems": ins["problems"]}


def build_one(cid, mod, root, scenes_dir, form2=False):
    raw = dict(mod.SPEC)
    raw.setdefault("id", cid)
    if raw["id"] != cid:
        raise SystemExit("tools/arc/char_%s.py: SPEC id %r must be %r (the file name decides the id)" % (cid, raw["id"], cid))
    extra = getattr(mod, "extra", None)
    mid = cid
    S_raw = raw
    if form2:
        S_raw = SP.form2_spec(raw)
        S_raw["id"] = cid
        mid = cid + "_form2"
    kit_mod = kit
    R = kit.build_character(dict(S_raw), extra=extra, kit=kit_mod, file_id=cid, model_name=mid, form=2 if form2 else 1, post=getattr(mod, "post", None))
    geo_builder.ROOT = root
    st = kit.save(R)
    ins = inspect(R, mid, root)
    os.makedirs(scenes_dir, exist_ok=True)
    scene_path = os.path.join(scenes_dir, "arc_%s.json" % mid)
    SC.write_scenes(R, {n: (v["length"], v["loop"]) for n, v in ins["clips"].items()}, scene_path)
    return R, st, ins


def run(ids, root, scenes_dir, manifest_path, quiet=False):
    chars = discover(ids)
    mani = {}
    if os.path.isfile(manifest_path) and ids:
        mani = json.load(open(manifest_path))["models"]
    problems = 0
    for cid, (path, mod) in chars.items():
        raw = mod.SPEC
        forms = [False] + ([True] if raw.get("form2") else [])
        for f2 in forms:
            R, st, ins = build_one(cid, mod, root, scenes_dir, f2)
            S = R.spec
            mid = cid + ("_form2" if f2 else "")
            entry = manifest_entry(R, mid, st, ins, S, 2 if f2 else 1)
            mani[mid] = entry
            if not quiet:
                print("%-24s %3d cubes %4d quads  texture %dx%d  height %.2f blocks  clips %s" % (mid, st["cubes"], st["quads"], st["texture"][0], st["texture"][1], entry["height_blocks"],
                                                                                              ",".join(ins["clips"])))
            for p in ins["problems"]:
                print("PROBLEM", mid, p)
                problems += 1
    with open(manifest_path, "w") as f:
        json.dump({"generator": "python3 -B tools/gen_arc_characters.py", "frame": "pixels (1/16 block), y up, the model's front on -z, +x the model's left, origin at the feet; GeoDraw.Space.PROP",
                   "models": {k: mani[k] for k in sorted(mani)}}, f, indent=1)
        f.write("\n")
    return problems


def same_file(src, dst):
    """Whether two generated files are the same: JSON byte for byte, a PNG by its decoded pixels (the bytes of a PNG depend on the version of the image library)."""
    if not os.path.isfile(dst):
        return False
    if src.endswith(".png"):
        from PIL import Image
        a, b = Image.open(src).convert("RGBA"), Image.open(dst).convert("RGBA")
        return a.size == b.size and a.tobytes() == b.tobytes()
    with open(src, "rb") as f1, open(dst, "rb") as f2:
        return f1.read() == f2.read()


def check():
    """Rebuilds everything into a temporary folder and compares it with the committed files (JSON byte for byte, PNG by pixels)."""
    if os.environ.get("ARC_FAST"):
        print("ARC_FAST is set: unset it, a restricted build can never equal the committed files")
        return 1
    tmp = tempfile.mkdtemp(prefix="arc_check_")
    try:
        root = os.path.join(tmp, "assets", "nusmp")
        sc = os.path.join(tmp, "scenes")
        man = os.path.join(tmp, "manifest.json")
        problems = run((), root, sc, man, quiet=True)
        bad = 0
        pairs = []
        for dp, _, fns in os.walk(root):
            for fn in fns:
                src = os.path.join(dp, fn)
                pairs.append((src, os.path.join(ASSETS, os.path.relpath(src, root))))
        for fn in os.listdir(sc):
            pairs.append((os.path.join(sc, fn), os.path.join(SCENES_DIR, fn)))
        pairs.append((man, MANIFEST))
        for src, dst in pairs:
            if not same_file(src, dst):
                print("DIFFERS", os.path.relpath(dst, REPO))
                bad += 1
        print("checked %d files: %d differ, %d problems" % (len(pairs), bad, problems))
        return 1 if (bad or problems) else 0
    finally:
        geo_builder.ROOT = ASSETS
        shutil.rmtree(tmp, ignore_errors=True)


def list_chars():
    for cid, (path, mod) in discover(()).items():
        S = SP.normalize(dict(mod.SPEC), cid)
        print("%-22s %-5s %-8s %s%s" % (cid, S["role"], S["build"], S["name"], "  (+ form2)" if S["form2"] else ""))


def preview(ids, names):
    env = dict(os.environ)
    scenes = []
    for cid in ids:
        base = "arc_%s_" % cid
        sc_file = os.path.join(SCENES_DIR, "arc_%s.json" % cid)
        allsc = json.load(open(sc_file))
        pick = [base + n for n in names] if names else list(allsc)
        scenes += [s for s in pick if s in allsc]
    cmd = [sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + scenes
    return subprocess.run(cmd, cwd=REPO, env=env).returncode


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    if os.environ.get("ARC_FAST") and "--gallery" not in argv and "--selftest" not in argv:
        print("ARC_FAST is set: the clips are restricted, so nothing built now may be committed (unset it for the real build)")
    if "--list" in argv:
        list_chars()
        return 0
    if "--validate" in argv:
        for cid, (path, mod) in discover(tuple(args)).items():
            SP.normalize(dict(mod.SPEC), cid)
            print("ok", cid)
        return 0
    if "--check" in argv:
        return check()
    if "--selftest" in argv:
        from arc_kit import selftest
        return selftest.run()
    if "--gallery" in argv:
        from arc_kit import gallery
        out = argv[argv.index("--out") + 1] if "--out" in argv else os.path.join(REPO, "build", "arc_gallery")
        which = [a for a in argv[argv.index("--gallery") + 1:] if not a.startswith("--") and a != out]
        return gallery.run(which[0] if which else "all", out, REPO, os.path.join(HERE, "render_preview", "preview.py"))
    if "--preview" in argv:
        ids = [a for a in args if os.path.isfile(os.path.join(ARC_DIR, "char_%s.py" % a))]
        names = [a for a in args if a not in ids]
        discover(tuple(ids))
        run(tuple(ids), ASSETS, SCENES_DIR, MANIFEST)
        return preview(ids or list(discover(()).keys()), names)
    return 1 if run(tuple(args), ASSETS, SCENES_DIR, MANIFEST) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
