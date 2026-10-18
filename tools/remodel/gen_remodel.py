"""Why: builds the remodelled mobs (docs/arc/remodel.md). The models themselves are ordinary art-kit characters (tools/arc/char_<id>.py, built by
tools/gen_arc_characters.py into assets/nusmp/{geo,animations,textures}/entity/arc_<id>*); this driver adds the one thing the kit's driver does not do: SKINS. A mob that comes
in several colours (the Dark Disciple of each devil, the Devil Avatar of Megicula, the Guardian that wears the Warden's body) is one body repainted, so its character file lists
`VARIANTS = {name: spec-or-callable}` and this driver builds each variant with the same extra() and writes ONLY its two textures (arc_<id>_<name>.png and _glow.png). The
geometry and the clips are shared (the renderer draws the base model with `GeoSpec.skin("arc_<id>_<name>")`), which is how a variant costs 10 KB instead of 310 KB.

    python3 -B tools/remodel/gen_remodel.py                  # every remodelled character (those whose file defines ENTITIES) and every skin
    python3 -B tools/remodel/gen_remodel.py dark_disciple    # only the named ones
    python3 -B tools/remodel/gen_remodel.py --check          # the kit's own --check, then every skin rebuilt into a temp folder and compared by pixels
    python3 -B tools/remodel/gen_remodel.py --list           # id, entity ids, skins

A variant whose geometry differs from the base (a different texture size or uv layout) is an error: it would not fit the shared model.
"""
import glob
import json
import os
import shutil
import sys
import tempfile

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, TOOLS)
import geo_builder  # noqa: E402
import gen_arc_characters as D  # noqa: E402
import gen_arc_kit as kit  # noqa: E402
from arc_kit import spec as SP  # noqa: E402


def remodelled():
    """{id: module} of every character file that declares ENTITIES (the entity ids it draws)."""
    out = {}
    for cid, (path, mod) in D.discover(()).items():
        if hasattr(mod, "ENTITIES"):          # an empty list is a tier or variant model that an entity draws through another file's entity id
            out[cid] = mod
    return out


def variants_of(mod):
    """{name: (raw spec, form)}: a VARIANTS value is a spec dict, a callable returning one, or a tuple of either and a form number (2 = the second form's skin)."""
    out = {}
    for name, v in (getattr(mod, "VARIANTS", None) or {}).items():
        form = 1
        if isinstance(v, tuple):
            v, form = v
        raw = v() if callable(v) else v
        out[name] = (dict(raw), form)
    return out


def build_skin(cid, mod, name, raw, form, root):
    """Builds one skin and writes its two textures under root; returns (width, height) of the texture."""
    raw = dict(raw)
    raw.setdefault("id", cid)
    if form == 2:
        raw["form2"] = raw.get("form2") or mod.SPEC.get("form2")
        raw = SP.form2_spec(raw)
        raw["id"] = cid
    model = "%s_%s" % (cid, name)
    R = kit.build_character(dict(raw), extra=getattr(mod, "extra", None), kit=kit, file_id=cid, model_name=model, form=form, post=getattr(mod, "post", None))
    geo_builder.ROOT = root
    img, glow = R.finish()
    img, glow, hh = R.cropped(img, glow)
    R.m.tex_h = hh
    R.m.save_texture(img, glow)
    return R.pk.W, hh


def base_size(cid, form):
    mid = cid + ("_form2" if form == 2 else "")
    m = json.load(open(os.path.join(D.MANIFEST)))["models"].get(mid)
    return tuple(m["texture"]) if m else None


def run(ids, root):
    mods = remodelled()
    pick = [i for i in (ids or mods) if i in mods]
    missing = [i for i in ids if i not in mods]
    if missing:
        raise SystemExit("not a remodelled character (no ENTITIES in tools/arc/char_<id>.py): %s" % ", ".join(missing))
    bad = 0
    if root == D.ASSETS:
        bad += D.run(tuple(pick), D.ASSETS, D.SCENES_DIR, D.MANIFEST)
    for cid in pick:
        for name, (raw, form) in variants_of(mods[cid]).items():
            size = build_skin(cid, mods[cid], name, raw, form, root)
            want = base_size(cid, form)
            flag = "" if want == size else "   PROBLEM: the base model's texture is %s" % (want,)
            if flag:
                bad += 1
            print("%-34s skin  texture %dx%d%s" % ("%s_%s" % (cid, name), size[0], size[1], flag))
    geo_builder.ROOT = D.ASSETS
    if root == D.ASSETS:
        write_skin_scenes(mods)
    return bad


def write_skin_scenes(mods):
    """tools/render_preview/scenes.d/remodel_skins.json: a front view of every skin (python3 -B tools/render_preview/preview.py remodel_<id>_<skin>), so a repaint can be looked at."""
    scenes = {}
    for cid, mod in sorted(mods.items()):
        for name, (raw, form) in variants_of(mod).items():
            base = os.path.join(D.SCENES_DIR, "arc_%s%s.json" % (cid, "_form2" if form == 2 else ""))
            if not os.path.isfile(base):
                continue
            sc = json.load(open(base))["arc_%s%s_front" % (cid, "_form2" if form == 2 else "")]
            sc = dict(sc)
            sc["model"] = "nusmp:geo/entity/arc_%s%s.geo.json" % (cid, "_form2" if form == 2 else "")
            sc["texture"] = "nusmp:textures/entity/arc_%s_%s.png" % (cid, name)
            sc["animations"] = "nusmp:animations/entity/arc_%s%s.animation.json" % (cid, "_form2" if form == 2 else "")
            scenes["remodel_%s_%s" % (cid, name)] = sc
    path = os.path.join(D.SCENES_DIR, "remodel_skins.json")
    with open(path, "w") as f:
        json.dump(scenes, f, indent=1)
        f.write("\n")


def check():
    mods = remodelled()
    bad = D.check()
    tmp = tempfile.mkdtemp(prefix="remodel_check_")
    try:
        root = os.path.join(tmp, "assets", "nusmp")
        n = 0
        for cid, mod in mods.items():
            for name, (raw, form) in variants_of(mod).items():
                build_skin(cid, mod, name, raw, form, root)
        for dp, _, fns in os.walk(root):
            for fn in fns:
                src = os.path.join(dp, fn)
                dst = os.path.join(D.ASSETS, os.path.relpath(src, root))
                n += 1
                if not D.same_file(src, dst):
                    print("DIFFERS", os.path.relpath(dst, D.REPO))
                    bad += 1
        print("checked %d skin textures: %d bad in total" % (n, bad))
        return 1 if bad else 0
    finally:
        geo_builder.ROOT = D.ASSETS
        shutil.rmtree(tmp, ignore_errors=True)


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    if "--list" in argv:
        for cid, mod in remodelled().items():
            print("%-22s %-40s skins: %s" % (cid, ",".join(mod.ENTITIES), ",".join(variants_of(mod)) or "-"))
        return 0
    if "--check" in argv:
        return check()
    return 1 if run(tuple(args), D.ASSETS) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
