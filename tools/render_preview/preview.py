"""
Headless render preview for the two CLIENT render systems of the 0.53 expansion (prop painters and player aura painters) and the GeoLite
model drawer. See README.md in this folder.

    python tools/render_preview/preview.py                    # every scene of scenes.json and scenes.d/*.json
    python tools/render_preview/preview.py bronze_shield body_muscle
    python tools/render_preview/preview.py --list

It compiles the REAL render classes of the mod (PropDraw, AuraRender, AuraContext, MagicPropRenderer, PlayerAuraLayer, GeoDraw ...) and the
real <Name>Aura / <Name>PropPainter of the scene's attribute against tiny Minecraft stubs (stubs/) and JOML, runs them for every age of
the scene, records the quads in the order the game would draw them (Preview.java) and rasterises them here with a depth buffer: render
type by render type, with the light, culling, alpha test and blending of the vanilla entity render types.

Needs JDK 21 (javac / java), Python 3 with numpy and Pillow. JOML (and Gson for geo scenes) are found in tools/vfx_preview/lib or
tools/render_preview/lib, or downloaded once from Maven Central. Everything this script writes goes to $RENDER_PREVIEW_BUILD (default
build/render_preview); many instances can run at once when they use different build folders.
"""
import sys

sys.dont_write_bytecode = True          # never leave __pycache__ / .pyc anywhere in the tree

import hashlib
import json
import math
import os
import re
import shutil
import subprocess
import time
import urllib.request

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
MOD_ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets")
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))

JARS = {
    "joml": ("joml-1.10.5.jar", "https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar"),
    "gson": ("gson-2.11.0.jar", "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"),
}

REAL_BASE = ["client/prop/PropDraw.java", "client/prop/PropPainter.java", "client/prop/MagicPropRenderer.java", "client/aura/AuraPainter.java",
             "client/aura/AuraContext.java", "client/aura/AuraRender.java", "client/aura/PlayerAuraLayer.java", "aura/Aura.java", "prop/PropKind.java"]
REAL_GEO = ["client/geo/GeoModelData.java", "client/geo/GeoAnim.java", "client/geo/GeoDraw.java", "client/geo/GeoSpec.java", "client/geo/GeoModels.java"]

BUDGET_VERTICES = 6000
KINDS = ("prop", "aura", "geo")


# ================================================================================================ small helpers
def log(msg=""):
    print(msg, flush=True)


def snake(pascal):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", pascal).upper()


def enum_names(path):
    """Constant names of a Java enum file (PropKind / Aura)."""
    with open(path, encoding="utf-8") as f:
        text = f.read()
    text = re.sub(r"//[^\n]*|/\*.*?\*/", "", text, flags=re.S)             # comments first: javadoc may contain braces
    body = text[text.index("{") + 1:]
    return re.findall(r"^\s*([A-Z][A-Z0-9_]*)\s*[,;]?\s*$", body.split("}")[0], flags=re.M)


def hex_color(v, default):
    """'FFFFFFFF' / '0xFF88AAFF' / int -> int."""
    if v is None:
        return default
    if isinstance(v, (int, float)):
        return int(v)
    s = str(v).strip().lower()
    if s.startswith("0x"):
        s = s[2:]
    if s.startswith("#"):
        s = s[1:]
    n = int(s, 16)
    if len(s) <= 6:
        n |= 0xFF000000
    return n


def pack_light(v):
    """Scene light -> packed int: an int, '0xF000F0', or {"block": 0, "sky": 15}."""
    if v is None:
        return 15 << 20
    if isinstance(v, dict):
        return (int(v.get("block", 0)) << 4) | (int(v.get("sky", 0)) << 20)
    if isinstance(v, str):
        return int(v, 16) if v.lower().startswith("0x") or re.fullmatch(r"[0-9a-fA-F]{6,8}", v) else int(v)
    return int(v)


# ================================================================================================ jars and compilation
def find_jar(key):
    name, url = JARS[key]
    for env in (key.upper() + "_JAR",):
        p = os.environ.get(env)
        if p and os.path.isfile(p):
            return p
    cands = [os.path.join(HERE, "lib", name), os.path.join(ROOT, "tools", "vfx_preview", "lib", name), os.path.join(BUILD, "lib", name)]
    for c in cands:
        if os.path.isfile(c):
            return c
    if key == "gson":                                           # any Gson 2.x the machine already has (Gradle caches the one Minecraft uses)
        for base in (os.path.join(os.path.expanduser("~"), ".gradle"), os.path.join(os.path.expanduser("~"), ".m2")):
            for dp, _, fns in os.walk(base):
                for fn in fns:
                    if re.fullmatch(r"gson-2\.[0-9.]+\.jar", fn):
                        return os.path.join(dp, fn)
    for folder in (os.path.join(HERE, "lib"), os.path.join(BUILD, "lib")):
        try:
            os.makedirs(folder, exist_ok=True)
            tmp = os.path.join(folder, name + ".%d.tmp" % os.getpid())
            log("downloading %s ..." % name)
            urllib.request.urlretrieve(url, tmp)
            final = os.path.join(folder, name)
            os.replace(tmp, final)                              # atomic: parallel runs never see half a jar
            return final
        except Exception as e:                                  # unwritable folder or no network: try the next one
            log("  could not fetch %s into %s: %s" % (name, folder, e))
    sys.exit("ERROR: %s is missing; download %s into tools/render_preview/lib" % (name, url))


def java_files(folder):
    out = []
    for dp, _, fns in os.walk(folder):
        for fn in fns:
            if fn.endswith(".java"):
                out.append(os.path.join(dp, fn))
    return sorted(out)


def attr_names():
    """Every attribute that has an Aura or PropPainter class (to keep a prefix like 'Ice' from pulling in 'IceWedge')."""
    names = set()
    for sub, suffix in (("client/aura", "Aura.java"), ("client/prop", "PropPainter.java")):
        d = os.path.join(SRC, sub)
        if os.path.isdir(d):
            for fn in os.listdir(d):
                if fn.endswith(suffix) and len(fn) > len(suffix):
                    names.add(fn[:-len(suffix)])
    return names


def attr_sources(attr, others):
    """The real <Name>Aura.java / <Name>PropPainter.java and every helper in those folders whose name starts with <Name>."""
    longer = [o for o in others if o != attr and o.startswith(attr)]
    files = []
    for sub in ("client/aura", "client/prop"):
        d = os.path.join(SRC, sub)
        if not os.path.isdir(d):
            continue
        for fn in sorted(os.listdir(d)):
            if fn.startswith(attr) and fn.endswith(".java") and not any(fn.startswith(o) for o in longer):
                files.append(os.path.join(d, fn))
    return files


def build(scenes, need_geo):
    os.makedirs(BUILD, exist_ok=True)
    joml = find_jar("joml")
    gson = find_jar("gson") if need_geo else None
    files = java_files(os.path.join(HERE, "stubs")) + [os.path.join(HERE, "Preview.java")] + [os.path.join(SRC, p.replace("/", os.sep)) for p in REAL_BASE]
    if need_geo:
        files += [os.path.join(SRC, p.replace("/", os.sep)) for p in REAL_GEO] + [os.path.join(HERE, "GeoPreview.java")]
        if os.path.isfile(os.path.join(HERE, "GeoCheck.java")):
            files.append(os.path.join(HERE, "GeoCheck.java"))
    others = attr_names()
    for sc in scenes.values():
        if sc.get("attr"):
            files += attr_sources(sc["attr"], others)
        for ex in sc.get("examples", []):
            files.append(os.path.join(HERE, "examples", ex + ".java"))
        for dep in sc.get("exampleFiles", []):
            files.append(os.path.join(HERE, "examples", dep))
    files = sorted(set(files))
    missing = [f for f in files if not os.path.isfile(f)]
    if missing:
        sys.exit("ERROR: missing source file(s): " + ", ".join(os.path.relpath(m, ROOT) for m in missing))
    sig = hashlib.sha1()
    for f in files:
        st = os.stat(f)
        sig.update(("%s|%d|%d\n" % (f, st.st_size, st.st_mtime_ns)).encode())
    sig.update((joml + str(gson)).encode())
    sig = sig.hexdigest()
    classes = os.path.join(BUILD, "classes")
    stamp = os.path.join(classes, ".sig")
    if os.path.isfile(stamp) and open(stamp).read() == sig:
        return classes, joml, gson
    shutil.rmtree(classes, ignore_errors=True)
    os.makedirs(classes)
    argfile = os.path.join(BUILD, "sources.txt")
    with open(argfile, "w", encoding="utf-8") as f:
        f.write("\n".join('"%s"' % p.replace("\\", "/") for p in files))
    cp = os.pathsep.join([joml] + ([gson] if gson else []))
    t0 = time.time()
    r = subprocess.run(["javac", "-nowarn", "-proc:none", "-encoding", "UTF-8", "-Xlint:none", "-cp", cp, "-d", classes, "@" + argfile],
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out = "\n".join(l for l in r.stdout.decode("utf-8", "replace").splitlines() if "Picked up" not in l and not l.startswith("Note:"))
    if r.returncode != 0:
        log(out)
        sys.exit("ERROR: javac failed (a real source does not compile against the preview stubs: fix the source, or add the missing Minecraft API to tools/render_preview/stubs)")
    with open(stamp, "w") as f:
        f.write(sig)
    log("compiled %d files in %.1f s" % (len(files), time.time() - t0))
    return classes, joml, gson


def run_java(classes, joml, gson, job, tag):
    jobfile = os.path.join(BUILD, "job_%s.json" % tag)
    meta = os.path.join(BUILD, "meta_%s.json" % tag)
    bindir = os.path.join(BUILD, "bin")
    os.makedirs(bindir, exist_ok=True)
    with open(jobfile, "w", encoding="utf-8") as f:
        json.dump({"scenes": job, "modAssets": MOD_ASSETS, "previewDir": HERE}, f)
    if os.path.isfile(meta):
        os.remove(meta)
    cp = os.pathsep.join([classes, joml] + ([gson] if gson else []))
    r = subprocess.run(["java", "-Xmx1g", "-Xss8m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-cp", cp, "Preview", jobfile, meta, bindir],
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=600)
    out = "\n".join(l for l in r.stdout.decode("utf-8", "replace").splitlines() if "Picked up" not in l)
    if out.strip():
        log(out)
    if r.returncode != 0 or not os.path.isfile(meta):
        sys.exit("ERROR: the Java half failed (see above)")
    with open(meta, encoding="utf-8") as f:
        return {s["name"]: s for s in json.load(f)["scenes"]}, bindir


# ================================================================================================ scenes
def load_scenes(extra_files=()):
    scenes, origin, errors = {}, {}, []
    files = [os.path.join(HERE, "scenes.json")]
    d = os.path.join(HERE, "scenes.d")
    if os.path.isdir(d):
        files += [os.path.join(d, n) for n in sorted(os.listdir(d)) if n.endswith(".json")]
    files += [os.path.abspath(f) for f in extra_files]
    for path in files:
        if not os.path.isfile(path):
            continue
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            errors.append("%s: %s" % (os.path.relpath(path, ROOT), e))
            continue
        for k, v in data.items():
            if k.startswith("_") or not isinstance(v, dict):
                continue
            if k in scenes:
                log("WARNING: scene '%s' is defined in %s and in %s (the later one wins)" % (k, os.path.relpath(origin[k], ROOT), os.path.relpath(path, ROOT)))
            scenes[k] = v
            origin[k] = path
    return scenes, origin, errors


PROP_KINDS = None
AURAS = None


def prepare_geo(s):
    """Resolves a geo scene's model like GeoSpec.of(key, name), or from a full path; returns the error strings."""
    errs = []
    space, layer = str(s.get("space", "PROP")).upper(), str(s.get("layer", "CUTOUT")).upper()
    if space not in ("PROP", "PLAYER"):
        errs.append('space must be "PROP" or "PLAYER" (got %r)' % s.get("space"))
    if layer not in ("CUTOUT", "TRANSLUCENT", "ADDITIVE"):
        errs.append('layer must be "CUTOUT", "TRANSLUCENT" or "ADDITIVE" (got %r)' % s.get("layer"))
    s["space"], s["layer"] = space, layer
    try:
        s["argb"] = hex_color(s.get("argb"), 0xFFFFFFFF)
        s["glow"] = hex_color(s.get("glow"), 0) if s.get("glow") not in (None, False) else 0
    except ValueError as e:
        errs.append("argb / glow must be hex ARGB such as FFFFFFFF (%s)" % e)
    m = s.get("model")
    if not m:
        return errs + ['a geo scene needs "model": "<key>_<name>" (assets/nusmp/geo/entity/<key>_<name>.geo.json) or a full path like "nusmp:geo/armor/robe.geo.json"']
    if re.fullmatch(r"[A-Za-z0-9]+_[A-Za-z0-9_]+", m):
        key, _, name = m.partition("_")
        s["modelKey"], s["modelName"] = key, name
        model_rl = "nusmp:geo/entity/%s.geo.json" % m
    else:
        model_rl = m if ":" in m else "nusmp:" + m
        s["modelRl"] = model_rl
        tex = s.get("texture")
        if not tex:
            errs.append('a geo scene with a full model path needs "texture" (e.g. "nusmp:textures/armor/black_bull_robe.png")')
        else:
            tex_rl = tex if ":" in tex else "nusmp:" + tex
            s["textureRl"] = tex_rl
            base = tex_rl[:-4] if tex_rl.endswith(".png") else tex_rl
            s["glowRl"] = s.get("glowTexture") or base + "_glow.png"
        mm = re.match(r"(?:[a-z0-9_.-]+:)?geo/(.+)\.geo\.json$", model_rl)
        anim = s.get("animations")
        if anim == "none":
            anim = None
        elif not anim and mm:
            cand = "nusmp:animations/%s.animation.json" % mm.group(1)
            anim = cand if find_asset(cand) else None
        if anim:
            s["animationsRl"] = anim if ":" in anim else "nusmp:" + anim
    if not find_asset(model_rl):
        errs.append("model file not found: %s (looked in %s)" % (model_rl, "; ".join(os.path.relpath(p, ROOT) for p in texture_candidates(model_rl))))
    return errs


def prepare(name, sc):
    """Validates a scene and fills in the defaults; returns (scene dict for Java and Python, list of error strings)."""
    global PROP_KINDS, AURAS
    errs = []
    s = dict(sc)
    s["name"] = name
    kind = s.get("kind")
    if kind not in KINDS:
        return s, ["kind must be one of %s (got %r)" % (", ".join(KINDS), kind)]
    ex = s.get("example", s.get("examples", []))
    s["examples"] = [ex] if isinstance(ex, str) else list(ex)
    for e in s["examples"]:
        if not os.path.isfile(os.path.join(HERE, "examples", e + ".java")):
            errs.append("example '%s' not found: tools/render_preview/examples/%s.java" % (e, e))
    attr = s.get("attr")
    if attr and kind in ("prop", "aura"):
        if not os.path.isfile(os.path.join(SRC, "client", "prop" if kind == "prop" else "aura", attr + ("PropPainter" if kind == "prop" else "Aura") + ".java")):
            errs.append("no real source for attr '%s' (client/%s/%s%s.java)" % (attr, "prop" if kind == "prop" else "aura", attr, "PropPainter" if kind == "prop" else "Aura"))
    if kind == "prop":
        if PROP_KINDS is None:
            PROP_KINDS = enum_names(os.path.join(SRC, "prop", "PropKind.java"))
        if "propKind" not in s:
            if not attr:
                errs.append("a prop scene needs \"propKind\" (e.g. \"BRONZE_1\") or \"attr\"")
            else:
                s["propKind"] = snake(attr) + "_1"
        if s.get("propKind") not in PROP_KINDS:
            guess = [k for k in PROP_KINDS if attr and snake(attr)[:4] in k][:4]
            errs.append("propKind %r is not a PropKind constant%s" % (s.get("propKind"), (" (did you mean " + ", ".join(guess) + "?)") if guess else ""))
    if kind == "aura":
        if AURAS is None:
            AURAS = enum_names(os.path.join(SRC, "aura", "Aura.java"))
        if "aura" not in s:
            if not attr:
                errs.append("an aura scene needs \"aura\" (e.g. \"BRONZE\") or \"attr\"")
            else:
                s["aura"] = snake(attr)
        if s.get("aura") not in AURAS:
            guess = [k for k in AURAS if attr and snake(attr)[:4] in k][:4]
            errs.append("aura %r is not an Aura constant%s" % (s.get("aura"), (" (did you mean " + ", ".join(guess) + "?)") if guess else ""))
    if kind == "geo":
        errs += prepare_geo(s)
    if kind in ("prop", "aura") and not attr and not s["examples"]:
        errs.append("give \"attr\" (the attribute whose real painter is drawn) or \"example\" (a painter in tools/render_preview/examples)")
    if "ages" not in s or not s["ages"]:
        s["ages"] = {"prop": [0, 12, 30, 60, 90], "aura": [4, 12, 40, 80, 112], "geo": [0, 20, 40, 60, 80]}[kind]
    s["ages"] = [float(a) for a in s["ages"]]
    if kind == "aura":
        s.setdefault("total", 120)
    if kind == "geo":
        s.setdefault("total", int(max(s["ages"])) + 100)
    s["light"] = pack_light(s.get("light"))
    s.setdefault("ssaa", None)
    return s, errs


# ================================================================================================ views
def basis(eye, target):
    f = np.array(target, float) - np.array(eye, float)
    f /= np.linalg.norm(f)
    r = np.cross(f, [0.0, 1.0, 0.0])
    if np.linalg.norm(r) < 1e-6:
        r = np.array([1.0, 0.0, 0.0])
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return f, r, u


def spherical(target, yaw, pitch, dist):
    """Eye at 'dist' from target, yaw degrees round the y axis starting at +z toward +x, pitch degrees above the horizon."""
    y, p = math.radians(yaw), math.radians(pitch)
    d = np.array([math.sin(y) * math.cos(p), math.sin(p), math.cos(y) * math.cos(p)])
    return np.array(target, float) + d * dist


def default_views(sc):
    kind = sc["kind"]
    player_space = kind == "aura" or (kind == "geo" and str(sc.get("space", "PROP")).upper() == "PLAYER")
    if player_space:
        base = [{"name": "front 3/4", "yaw": 35, "pitch": 12}, {"name": "side", "yaw": 90, "pitch": 6}]
    else:
        base = [{"name": "front 3/4", "yaw": 35, "pitch": 18}, {"name": "top 3/4", "yaw": 35, "pitch": 55}]
    views = sc.get("views")
    return [dict(v) for v in views] if views else base


def fit_distance(lo, hi, target, yaw, pitch, fov, aspect, margin=1.12):
    corners = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])], float)
    eye = spherical(target, yaw, pitch, 10.0)
    f, r, u = basis(eye, target)
    rel = corners - np.array(target, float)
    xs, ys, zs = rel @ r, rel @ u, rel @ f
    tv = math.tan(math.radians(fov) / 2)
    th = tv * aspect
    d = max(np.max(np.abs(xs) / th - zs), np.max(np.abs(ys) / tv - zs)) * margin
    return float(max(d, 0.5 - float(np.min(zs)), 0.8))


def make_views(sc, bounds, tile):
    """Final cameras: spherical views fitted to the bounds of everything drawn (same for every column of a row)."""
    aspect = tile[0] / tile[1]
    fov = float(sc.get("fov", 40))
    lo, hi = bounds
    centre = [(lo[i] + hi[i]) / 2 for i in range(3)]
    out = []
    for v in default_views(sc):
        vf = float(v.get("fov", fov))
        if "eye" in v:
            eye, target = list(v["eye"]), list(v.get("target", centre))
        else:
            target = list(v.get("target", centre))
            yaw, pitch = float(v.get("yaw", 35)), float(v.get("pitch", 15))
            dist = float(v["dist"]) if "dist" in v else fit_distance(lo, hi, target, yaw, pitch, vf, aspect)
            eye = list(spherical(target, yaw, pitch, dist))
        out.append({"name": v.get("name", "view"), "eye": [float(x) for x in eye], "target": [float(x) for x in target], "fov": vf})
    return out


def nominal_views(sc):
    out = []
    for v in default_views(sc):
        if "eye" in v:
            out.append({"eye": v["eye"], "target": v.get("target", [0, 1, 0])})
        else:
            t = v.get("target", [0, 1, 0])
            out.append({"eye": list(spherical(t, float(v.get("yaw", 35)), float(v.get("pitch", 15)), 8.0)), "target": t})
    return out


# ================================================================================================ textures and light
_tex_cache = {}
_missing = set()


def texture_candidates(rl):
    ns, _, path = rl.partition(":")
    if not path:
        ns, path = "minecraft", rl
    c = []
    if ns == "nusmp":
        c.append(os.path.join(MOD_ASSETS, "nusmp", path))
    c.append(os.path.join(HERE, "assets", ns, path))
    if ns == "nusmp":
        c.append(os.path.join(HERE, "assets", path))
        c.append(os.path.join(HERE, "examples", path))
    return c


def find_asset(rl):
    """The file a ResourceLocation string resolves to (mod assets, then this tool's assets / examples), or None."""
    for c in texture_candidates(rl):
        if os.path.isfile(c):
            return c
    return None


def get_texture(rl):
    """(float32 H x W x 4 array, found flag) for a ResourceLocation string; a missing texture is solid magenta."""
    if rl is None:
        return None, True
    if rl not in _tex_cache:
        found = None
        for c in texture_candidates(rl):
            if os.path.isfile(c):
                found = c
                break
        if found is None:
            _tex_cache[rl] = (np.array([[[1.0, 0.0, 1.0, 1.0]]], np.float32), False)
        else:
            _tex_cache[rl] = (np.asarray(Image.open(found).convert("RGBA"), dtype=np.float32) / 255.0, True)
    return _tex_cache[rl]


def _brightness(level):
    f = level / 15.0
    return f / (4.0 - 3.0 * f)


def _lightmap_lut(gamma=0.5, sky_darken=1.0):
    """The vanilla lightmap (overworld, noon, default brightness option): colour for [block light][sky light]."""
    lut = np.zeros((16, 16, 3), np.float32)
    sky_col = np.array([sky_darken, sky_darken, 1.0]) * 0.65 + 0.35
    f1 = sky_darken * 0.95 + 0.05
    for b in range(16):
        for s in range(16):
            sb = _brightness(s) * f1
            bb = _brightness(b) * 1.5
            v = np.array([bb, bb * ((bb * 0.6 + 0.4) * 0.6 + 0.4), bb * (bb * bb * 0.6 + 0.4)]) + sky_col * sb
            v = np.clip(v * 0.96 + 0.75 * 0.04, 0, 1)
            ng = 1 - (1 - v) ** 4
            v = v + (ng - v) * gamma
            lut[b, s] = np.clip(v * 0.96 + 0.75 * 0.04, 0, 1)
    return lut


LIGHTMAP = _lightmap_lut()
_L0 = np.array([0.2, 1.0, -0.7])
_L0 /= np.linalg.norm(_L0)
_L1 = np.array([-0.2, 1.0, 0.7])
_L1 /= np.linalg.norm(_L1)


def shade(rt, V):
    """Per-vertex colour (rgb, alpha) like the entity vertex shader: colour * min(1, (n.L0 + n.L1) * 0.6 + 0.4) * lightmap(block, sky)."""
    rgb = V[..., 5:8] / 255.0
    a = V[..., 8] / 255.0
    if rt["lit"]:
        n = V[..., 11:14].astype(np.float64)
        d0 = np.maximum(n @ _L0, 0.0)
        d1 = np.maximum(n @ _L1, 0.0)
        mix = np.minimum(1.0, (d0 + d1) * 0.6 + 0.4)
        bl = np.clip(np.round(V[..., 9]), 0, 15).astype(np.int32)
        sk = np.clip(np.round(V[..., 10]), 0, 15).astype(np.int32)
        rgb = rgb * mix[..., None] * LIGHTMAP[bl, sk]
    return rgb.astype(np.float64), a.astype(np.float64)


# ================================================================================================ rasteriser
NEAR = 0.05


class View:
    def __init__(self, eye, target, fov, W, H):
        self.eye = np.array(eye, float)
        self.f, self.r, self.u = basis(eye, target)
        self.fov, self.W, self.H = fov, W, H
        self.k = (H / 2) / math.tan(math.radians(fov) / 2)


def background(view, floor_on=True):
    """Sky gradient, checker floor (1 block squares) and the floor's depth, for the whole tile."""
    W, H, k = view.W, view.H, view.k
    top, horizon = np.array([0.40, 0.44, 0.52]), np.array([0.62, 0.64, 0.66])
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)
    dirs = view.f[None, None, :] * k + view.r[None, None, :] * (xx[..., None] + 0.5 - W / 2) - view.u[None, None, :] * (yy[..., None] + 0.5 - H / 2)
    dirs /= np.linalg.norm(dirs, axis=2, keepdims=True)
    t = np.clip(dirs[..., 1] * 1.6 + 0.35, 0, 1)[..., None]
    img = horizon * (1 - t) + top * t
    zinv = np.zeros((H, W), np.float64)
    if floor_on and view.eye[1] > 0.05:
        hit = dirs[..., 1] < -1e-4
        dist = np.where(hit, view.eye[1] / np.where(hit, -dirs[..., 1], 1.0), 0.0)
        gx, gz = view.eye[0] + dirs[..., 0] * dist, view.eye[2] + dirs[..., 2] * dist
        checker = ((np.floor(gx) + np.floor(gz)) % 2 == 0).astype(np.float64)
        gcol = np.array([0.43, 0.43, 0.41])[None, None, :] * (0.88 + 0.12 * checker[..., None])
        # a thin darker line on the block borders
        edge = (np.minimum(gx - np.floor(gx), np.floor(gx) + 1 - gx) < 0.012) | (np.minimum(gz - np.floor(gz), np.floor(gz) + 1 - gz) < 0.012)
        gcol = np.where(edge[..., None], gcol * 0.8, gcol)
        fog = np.clip(dist / 45.0, 0, 1)[..., None]
        gcol = gcol * (1 - fog) + horizon * fog
        img = np.where(hit[..., None], gcol, img)
        depth = dist * (dirs @ view.f)
        zinv = np.where(hit, 1.0 / np.maximum(depth, 1e-6) * (1 - 2e-4), 0.0)
    return img, zinv


class Raster:
    def __init__(self, view, img, zinv):
        self.v = view
        self.img = img
        self.zb = zinv
        self.W, self.H = view.W, view.H

    # ---- one triangle: perspective-correct, depth tested, alpha tested, blended
    def tri(self, rt, tex, sx, sy, iz, A):
        x0, x1, x2 = sx
        y0, y1, y2 = sy
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if area == 0.0 or area != area:
            return
        if rt["cull"] and area >= 0.0:                      # front faces run counter-clockwise on screen = negative area in image coordinates
            return
        W, H = self.W, self.H
        minx = max(int(math.floor(min(x0, x1, x2))), 0)
        maxx = min(int(math.ceil(max(x0, x1, x2))), W)
        miny = max(int(math.floor(min(y0, y1, y2))), 0)
        maxy = min(int(math.ceil(max(y0, y1, y2))), H)
        if minx >= maxx or miny >= maxy:
            return
        inv = 1.0 / area
        px = np.arange(minx, maxx, dtype=np.float64) + 0.5
        py = np.arange(miny, maxy, dtype=np.float64) + 0.5
        b1 = ((px - x0) * ((y2 - y0) * inv))[None, :] - ((py - y0) * ((x2 - x0) * inv))[:, None]
        b2 = ((py - y0) * ((x1 - x0) * inv))[:, None] - ((px - x0) * ((y1 - y0) * inv))[None, :]
        b0 = 1.0 - b1 - b2
        mask = (b1 >= -1e-7) & (b2 >= -1e-7) & (b0 >= -1e-7)
        idx = np.nonzero(mask)
        if idx[0].size == 0:
            return
        rows, cols = idx[0] + miny, idx[1] + minx
        c1, c2 = b1[idx], b2[idx]
        c0 = 1.0 - c1 - c2
        invz = c0 * iz[0] + c1 * iz[1] + c2 * iz[2]
        zold = self.zb[rows, cols]
        if rt["deq"]:
            ok = np.abs(invz - zold) <= 2e-4 * np.maximum(zold, 1e-9)
        else:
            ok = invz >= zold * (1.0 - 1e-6)
        ok &= invz > 0
        if not ok.any():
            return
        if not ok.all():
            rows, cols, c0, c1, c2, invz = rows[ok], cols[ok], c0[ok], c1[ok], c2[ok], invz[ok]
        w = np.column_stack((c0 * iz[0], c1 * iz[1], c2 * iz[2])) / invz[:, None]
        att = w @ A                                         # u, v, r, g, b, a per pixel
        if tex is None:
            texel_rgb, texel_a = 1.0, np.ones(rows.size)
        else:
            th, tw = tex.shape[:2]
            ui = np.minimum(((att[:, 0] + rt["uo"]) % 1.0 * tw).astype(np.int32), tw - 1)
            vi = np.minimum(((att[:, 1] + rt["vv"]) % 1.0 * th).astype(np.int32), th - 1)
            texel = tex[vi, ui]
            texel_rgb, texel_a = texel[:, :3], texel[:, 3].astype(np.float64)
        disc = rt["disc"]
        if disc != "none":
            keep = (texel_a >= 0.1) if disc == "tex" else (texel_a * att[:, 5] >= 0.1)
            if not keep.all():
                if not keep.any():
                    return
                rows, cols, invz, att, texel_a = rows[keep], cols[keep], invz[keep], att[keep], texel_a[keep]
                texel_rgb = texel_rgb[keep] if tex is not None else 1.0
        rgb = texel_rgb * att[:, 2:5]
        blend = rt["blend"]
        if blend == "none":
            self.img[rows, cols] = rgb
        else:
            a = (texel_a * att[:, 5])[:, None]
            dst = self.img[rows, cols]
            self.img[rows, cols] = (dst + rgb * a) if blend == "additive" else (dst + (rgb - dst) * a)
        if rt["dw"]:
            self.zb[rows, cols] = invz

    # ---- one batch (all quads of a run)
    def run(self, rt, V, tex):
        n = V.shape[0]
        if n == 0:
            return
        v = self.v
        P = V[..., 0:3].astype(np.float64)
        finite = np.isfinite(P).all(axis=(1, 2))
        P = np.where(np.isfinite(P), P, 0.0)
        C = P - v.eye
        x, y, z = C @ v.r, C @ v.u, C @ v.f
        vo = 0.99975586 if rt["vo"] else 1.0
        rgb, a = shade(rt, V)
        att = np.concatenate([V[..., 3:5].astype(np.float64), rgb, a[..., None]], axis=2)     # (n, 4, 6)
        order = np.arange(n)
        if rt["sort"]:                                       # sortOnUpload: far to near by the quad's centre (distance to the camera)
            d2 = ((P.mean(axis=1) - v.eye) ** 2).sum(axis=1)
            order = np.argsort(-d2, kind="stable")
        zs = np.maximum(z, 1e-9)
        sx = v.W / 2 + x / zs * v.k
        sy = v.H / 2 - y / zs * v.k
        iz = 1.0 / (zs * vo)
        sxl, syl, izl, zl = sx.tolist(), sy.tolist(), iz.tolist(), z.tolist()
        for qi in order.tolist():
            if not finite[qi]:
                continue
            zq = zl[qi]
            if min(zq) >= NEAR:
                for t in ((0, 1, 2), (0, 2, 3)):
                    self.tri(rt, tex, [sxl[qi][i] for i in t], [syl[qi][i] for i in t], [izl[qi][i] for i in t], att[qi][list(t)])
            elif max(zq) > NEAR:
                for t in ((0, 1, 2), (0, 2, 3)):
                    self.clipped(rt, tex, [(x[qi][i], y[qi][i], z[qi][i], att[qi][i]) for i in t], vo)

    def clipped(self, rt, tex, poly, vo):
        """A triangle crossing the near plane: clip it in camera space, then draw the fan."""
        out = []
        for i in range(len(poly)):
            a, b = poly[i], poly[(i + 1) % len(poly)]
            ain, bin_ = a[2] >= NEAR, b[2] >= NEAR
            if ain:
                out.append(a)
            if ain != bin_:
                t = (NEAR - a[2]) / (b[2] - a[2])
                out.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, NEAR, a[3] + (b[3] - a[3]) * t))
        v = self.v
        for i in range(1, len(out) - 1):
            tri = (out[0], out[i], out[i + 1])
            self.tri(rt, tex, [v.W / 2 + p[0] / p[2] * v.k for p in tri], [v.H / 2 - p[1] / p[2] * v.k for p in tri], [1.0 / (p[2] * vo) for p in tri],
                     np.array([p[3] for p in tri]))


def downsample(img, ss):
    if ss == 1:
        return img
    h, w = img.shape[0] // ss, img.shape[1] // ss
    return img[:h * ss, :w * ss].reshape(h, ss, w, ss, 3).mean(axis=(1, 3))


# ================================================================================================ frame data
class Frame:
    """The recorded runs of one frame: [(render type dict, quads array (n, 4, 14), is_base, is_figure)]."""

    def __init__(self, entry, quads, offset=(0.0, 0.0)):
        self.entry = entry
        self.runs = []
        for r in entry["runs"]:
            V = quads[r["off"]:r["off"] + r["n"]].astype(np.float64)
            if offset != (0.0, 0.0) and V.size:
                V = V.copy()
                V[..., 0] += offset[0]
                V[..., 2] += offset[1]
            self.runs.append((r["rt"], V, r["base"], entry.get("fig", False)))


def painter_runs(frame):
    return [(rt, V) for rt, V, base, fig in frame.runs if not base and not fig and V.shape[0]]


def frame_bounds(frames, include_base=True):
    pts = []
    for fr in frames:
        for rt, V, base, fig in fr.runs:
            if fig or V.shape[0] == 0 or (base and not include_base):
                continue
            P = V[..., 0:3].reshape(-1, 3)
            P = P[np.isfinite(P).all(axis=1)]
            if P.size:
                pts.append((P.min(axis=0), P.max(axis=0)))
    if not pts:
        return np.array([-1.0, 0.0, -1.0]), np.array([1.0, 2.0, 1.0])
    lo = np.min([p[0] for p in pts], axis=0)
    hi = np.max([p[1] for p in pts], axis=0)
    return lo, hi


# ================================================================================================ checks done in numpy
def geometry_issues(frame):
    """Zero-area quads, windings that contradict the declared normal, quads that are entirely transparent."""
    out = []
    zero = mismatch = 0
    ex_zero = ex_mis = None
    tot_vertices = 0
    nonzero_alpha = False
    for rt, V in painter_runs(frame):
        tot_vertices += V.shape[0] * 4
        if (V[..., 8] > 0).any():
            nonzero_alpha = True
        P = V[..., 0:3]
        ok = np.isfinite(P).all(axis=(1, 2))
        n = np.cross(P[:, 2] - P[:, 0], P[:, 3] - P[:, 1])
        area = 0.5 * np.linalg.norm(n, axis=1)
        z = ok & (area < 1e-9)
        if z.any():
            zero += int(z.sum())
            ex_zero = ex_zero or "%s quad %d" % (rt["name"], int(np.nonzero(z)[0][0]))
        if rt["fmt"] == "entity":
            nm = V[..., 11:14].mean(axis=1)
            dot = np.einsum("ij,ij->i", n, nm) / np.maximum(np.linalg.norm(n, axis=1) * np.linalg.norm(nm, axis=1), 1e-12)
            m = ok & (area >= 1e-9) & (np.linalg.norm(nm, axis=1) > 0.3) & (dot < -0.2)
            if m.any():
                mismatch += int(m.sum())
                ex_mis = ex_mis or "%s quad %d" % (rt["name"], int(np.nonzero(m)[0][0]))
    if zero:
        out.append(("WARNING", "ZERO_AREA", "%d quads have zero area (all four corners on one line or one point), e.g. %s" % (zero, ex_zero)))
    if mismatch:
        out.append(("WARNING", "NORMAL_MISMATCH", "%d quads are wound the other way round from their normal (their front face looks opposite to the lighting normal), "
                    "e.g. %s: culling render types show them from the wrong side and the light is wrong" % (mismatch, ex_mis)))
    if tot_vertices and not nonzero_alpha:
        out.append(("WARNING", "ALPHA_ZERO", "every vertex has alpha 0: nothing is visible"))
    if tot_vertices > BUDGET_VERTICES:
        out.append(("WARNING", "BUDGET", "%d vertices in one frame (budget note: keep a layer or prop under %d)" % (tot_vertices, BUDGET_VERTICES)))
    return out, tot_vertices


# ================================================================================================ fonts and labels
_font_cache = {}


def font(size=12):
    if size not in _font_cache:
        f = None
        for name in ("DejaVuSans.ttf", "DejaVuSansMono.ttf", "arial.ttf", "Arial.ttf", "LiberationSans-Regular.ttf"):
            try:
                f = ImageFont.truetype(name, size)
                break
            except Exception:
                continue
        _font_cache[size] = f or ImageFont.load_default()
    return _font_cache[size]


def label(draw, xy, text, fill=(255, 255, 255), size=12, back=(0, 0, 0, 150)):
    f = font(size)
    box = draw.textbbox(xy, text, font=f)
    draw.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=back)
    draw.text(xy, text, font=f, fill=fill)


def wrap(text, width):
    lines = []
    for para in text.split("\n"):
        while len(para) > width:
            cut = para.rfind(" ", 0, width)
            cut = cut if cut > 20 else width
            lines.append(para[:cut])
            para = "  " + para[cut:].lstrip()
        lines.append(para)
    return lines


# ================================================================================================ one scene
class Result:
    def __init__(self, name):
        self.name = name
        self.issues = []            # (level, code, msg)
        self.seen = set()

    def add(self, level, code, msg):
        key = (code, msg.split("\n")[0])
        if key not in self.seen:
            self.seen.add(key)
            self.issues.append((level, code, msg))

    def codes(self):
        return {c for _, c, _ in self.issues}

    def errors(self):
        return [i for i in self.issues if i[0] == "ERROR"]


def figure_offset(sc, fig_frame, frames, views0):
    """Where the 1.8 block stand-in goes: at the target, else to the right of the prop as the first camera sees it."""
    if sc.get("target"):
        t = sc["target"]
        return (float(t[0]), float(t[2]))
    lo, hi = frame_bounds(frames)
    cx, cz = (lo[0] + hi[0]) / 2, (lo[2] + hi[2]) / 2
    rad = 0.5 * math.hypot(hi[0] - lo[0], hi[2] - lo[2])
    yaw = math.radians(float(views0[0].get("yaw", 35))) if views0 and "yaw" in views0[0] else math.radians(35)
    right = (math.cos(yaw), -math.sin(yaw))
    d = rad + 0.9
    return (cx + right[0] * d, cz + right[1] * d)


def render_scene(name, sc, sm, bindir, ssaa_override, verbose):
    """Rasterises one scene's recorded frames into a contact sheet. sm = the Java meta of the scene."""
    t0 = time.time()
    res = Result(name)
    for i in sm.get("issues", []):
        res.add(i["level"], i["code"], i["msg"])
    kind = sc["kind"]
    tile = list(sc.get("tile", [300, 400] if (kind == "aura" or (kind == "geo" and str(sc.get("space", "PROP")).upper() == "PLAYER")) else [360, 340]))
    ss = int(ssaa_override or sc.get("ssaa") or 2)
    ages = sm["ages"]
    frames_idx = sm["frames"]
    binpath = os.path.join(bindir, name + ".bin")
    quads = np.fromfile(binpath, dtype=">f4").astype(np.float32).reshape(-1, 4, 14) if os.path.isfile(binpath) and sm["quads"] else np.zeros((0, 4, 14), np.float32)
    pool = sm["pool"]
    frames = {}

    def frame_of(pi, off=(0.0, 0.0)):
        key = (pi, off)
        if key not in frames:
            frames[key] = Frame(pool[pi], quads, off)
        return frames[key]

    # the stand-in figure of a prop scene
    fig_pool = sm.get("figure", -1)
    n_views = len(frames_idx[0]) if frames_idx else 0
    if not frames_idx:
        return res, None, 0.0
    base_frames = [frame_of(frames_idx[a][0]) for a in range(len(ages))]
    figoff = None
    if fig_pool >= 0:
        figoff = figure_offset(sc, pool[fig_pool], base_frames, default_views(sc))
    # per-frame issues from Java, and the numpy checks
    zero_painter = []
    for a, age in enumerate(ages):
        for v in range(n_views):
            fr = frame_of(frames_idx[a][v])
            for i in fr.entry["issues"]:
                res.add(i["level"], i["code"], "age %s: %s" % (("%g" % age), i["msg"]) if i["code"] in ("EXCEPTION", "MISSING_ELEMENTS", "STALE_CONSUMER", "UNBALANCED_POSE") else i["msg"])
            if v == 0:
                gi, verts = geometry_issues(fr)
                for lvl, code, msg in gi:
                    res.add(lvl, code, msg)
                if verts == 0 and sm.get("registered", True) and not fr.entry.get("err"):
                    zero_painter.append(age)
    if zero_painter and len(zero_painter) < len(ages):
        res.add("NOTE", "EMPTY_FRAME", "nothing drawn at age %s" % ", ".join("%g" % a for a in zero_painter))
    elif zero_painter and sm.get("registered", True):
        res.add("WARNING", "NOTHING_DRAWN", "the painter drew nothing at any age")

    # cameras: fitted to what was recorded (view 0 frames hold the geometry)
    all_frames = list(base_frames)
    if figoff is not None:
        all_frames.append(frame_of(fig_pool, figoff))
    lo, hi = frame_bounds([f for f in all_frames if f.runs] if all_frames else [], True)
    if figoff is not None:
        lo2, hi2 = frame_bounds([frame_of(fig_pool, figoff)], True) if False else (lo, hi)
    fig_frame = frame_of(fig_pool, figoff) if figoff is not None else None
    pts_lo, pts_hi = [lo], [hi]
    if fig_frame is not None:
        for rt, V, base, fig in fig_frame.runs:
            if V.shape[0]:
                P = V[..., 0:3].reshape(-1, 3)
                pts_lo.append(P.min(axis=0))
                pts_hi.append(P.max(axis=0))
    lo, hi = np.min(pts_lo, axis=0), np.max(pts_hi, axis=0)
    views = make_views(sc, (lo, hi), tile)
    return res, dict(views=views, bounds=(lo, hi), tile=tile, ss=ss, figoff=figoff, frames_idx=frames_idx, ages=ages, frame_of=frame_of, fig_pool=fig_pool,
                     n_views=n_views, base_frames=base_frames, t0=t0), 0.0


def draw_sheet(name, sc, sm, res, st, verbose):
    kind = sc["kind"]
    tile, ss = st["tile"], st["ss"]
    views, ages, frames_idx, frame_of = st["views"], st["ages"], st["frames_idx"], st["frame_of"]
    W, H = tile
    n_cols, n_rows = len(ages), len(views)
    fig_frame = frame_of(st["fig_pool"], st["figoff"]) if st["figoff"] is not None else None
    stats = []
    sheet_tiles = {}
    missing = set()
    bg_cache = {}
    for r, vw in enumerate(views):
        view = View(vw["eye"], vw["target"], vw["fov"], W * ss, H * ss)
        key = r
        bg_cache[key] = background(view, bool(sc.get("floor", True)))
        for c, age in enumerate(ages):
            fr = frame_of(frames_idx[c][min(r, st["n_views"] - 1)])
            img = bg_cache[key][0].copy()
            zb = bg_cache[key][1].copy()
            ras = Raster(view, img, zb)
            runs = (fig_frame.runs if fig_frame else []) + fr.runs
            for rt, V, base, fig in runs:
                if V.shape[0] == 0:
                    continue
                tex, found = get_texture(rt["tex"])
                if not found and rt["tex"] not in missing:
                    missing.add(rt["tex"])
                ras.run(rt, V, tex)
            tile_img = Image.fromarray((np.clip(downsample(ras.img, ss), 0, 1) * 255 + 0.5).astype(np.uint8))
            sheet_tiles[(r, c)] = tile_img
    for tx in sorted(missing):
        res.add("WARNING", "MISSING_TEXTURE", "texture %s not found (looked in %s): drawn magenta" % (tx, "; ".join(os.path.relpath(p, ROOT) for p in texture_candidates(tx))))
    # the per-frame report lines
    lines = []
    for c, age in enumerate(ages):
        fr = frame_of(frames_idx[c][0])
        counts = {}
        for rt, V in painter_runs(fr):
            key = rt["name"]
            counts[key] = counts.get(key, 0) + V.shape[0] * 4
        total = sum(counts.values())
        lines.append("   age %6g : %5d verts  %s%s" % (age, total, " | ".join("%s %d" % (k, v) for k, v in counts.items()), "   [error]" if fr.entry.get("err") else ""))
        if verbose:
            for rt, V, base, fig in fr.runs:
                lines.append("        %s %-26s %-44s %4d quads" % ("base" if base else "    ", rt["name"], rt["tex"] or "-", V.shape[0]))
    # compose
    margin, head, foot_lines = 6, 30, []
    for lvl, code, msg in res.issues:
        for i, l in enumerate(wrap("%s [%s] %s" % (lvl, code, msg), 150)):
            foot_lines.append((lvl, l))
    foot_lines = foot_lines[:14]
    foot = 8 + 15 * len(foot_lines) if foot_lines else 0
    sheet = Image.new("RGB", (n_cols * W + (n_cols + 1) * margin, head + n_rows * H + (n_rows + 1) * margin + foot), (24, 24, 28))
    d = ImageDraw.Draw(sheet, "RGBA")
    title = "%s   [%s%s]" % (name, kind, (" " + sc.get("propKind", sc.get("aura", sc.get("model", "")))) if kind in ("prop", "aura", "geo") else "")
    d.text((margin + 2, 7), title, font=font(14), fill=(235, 235, 240))
    for (r, c), im in sheet_tiles.items():
        ox, oy = margin + c * (W + margin), head + margin + r * (H + margin)
        sheet.paste(im, (ox, oy))
        dd = ImageDraw.Draw(sheet, "RGBA")
        age = ages[c]
        if kind == "aura":
            total = float(sc.get("total", 120))
            fade = max(0.0, min(1.0, min(age / 8.0, (total - age) / 8.0)))
            txt = "t=%g/%g  fade %.2f" % (age, total, fade)
        elif kind == "geo":
            txt = "age %g  (%.2f s)" % (age, age / 20.0)
        else:
            txt = "age %g/%d" % (age, int(sc.get("maxLife", 100)))
        fr = frame_of(frames_idx[c][min(r, st["n_views"] - 1)])
        verts = sum(V.shape[0] * 4 for rt, V in painter_runs(fr))
        label(dd, (ox + 5, oy + 4), "%s   %d v" % (txt, verts) if r == 0 else txt, size=11)
        if c == 0:
            label(dd, (ox + 5, oy + H - 18), views[r]["name"], size=11, fill=(255, 235, 160))
        if fr.entry.get("err"):
            label(dd, (ox + 5, oy + H - 36), "ERROR", fill=(255, 120, 120), size=12)
    for i, (lvl, l) in enumerate(foot_lines):
        col = {"ERROR": (255, 120, 120), "WARNING": (255, 210, 110), "NOTE": (170, 200, 255)}.get(lvl, (220, 220, 220))
        d.text((margin + 2, head + n_rows * H + (n_rows + 1) * margin + 5 + 15 * i), l, font=font(11), fill=col)
    path = os.path.join(BUILD, name + ".png")
    sheet.save(path)
    return path, lines


# ================================================================================================ main
def main(argv):
    names = []
    opts = {"list": False, "ssaa": None, "verbose": False, "files": []}
    i = 0
    while i < len(argv):
        a = argv[i]
        if a in ("-h", "--help"):
            print(__doc__)
            return 0
        if a == "--list":
            opts["list"] = True
        elif a == "--ssaa":
            i += 1
            opts["ssaa"] = int(argv[i])
        elif a == "--scenes":
            i += 1
            opts["files"].append(argv[i])
        elif a in ("-v", "--verbose"):
            opts["verbose"] = True
        elif a.startswith("-"):
            sys.exit("unknown option " + a)
        else:
            names.append(a)
        i += 1
    scenes, origin, load_errors = load_scenes(opts["files"])
    for e in load_errors:
        log("ERROR: cannot read " + e)
    if opts["list"]:
        for n, s in scenes.items():
            log("%-34s %-5s %-14s %s" % (n, s.get("kind", "?"), s.get("attr") or ",".join(s.get("examples", [s.get("example", "")]) if isinstance(s.get("examples", s.get("example")), list) else [str(s.get("example", ""))]), os.path.relpath(origin[n], ROOT)))
        return 0
    if not names:
        names = list(scenes)
    bad = [n for n in names if n not in scenes]
    if bad:
        sys.exit("unknown scene(s): %s\navailable: %s" % (", ".join(bad), ", ".join(scenes)))
    prepared, failed = {}, []
    for n in names:
        s, errs = prepare(n, scenes[n])
        if errs:
            for e in errs:
                log("ERROR [%s] %s" % (n, e))
            failed.append(n)
        else:
            prepared[n] = s
    if not prepared:
        return 1
    need_geo = any(s["kind"] == "geo" for s in prepared.values())
    classes, joml, gson = build(prepared, need_geo)
    # pass 1: nominal cameras
    job = []
    for n, s in prepared.items():
        j = dict(s)
        j["views"] = nominal_views(s)
        job.append(j)
    metas, bindir = run_java(classes, joml, gson, job, "a")
    # pass 2 (only scenes whose painter looked at the camera): the real cameras
    prestate = {}
    redo = []
    for n, s in prepared.items():
        res, st, _ = render_scene(n, s, metas[n], bindir, opts["ssaa"], opts["verbose"])
        prestate[n] = (res, st)
        if st is not None and metas[n].get("cameraUsed"):
            redo.append((n, st))
    if redo:
        job = []
        for n, st in redo:
            j = dict(prepared[n])
            j["views"] = [{"eye": v["eye"], "target": v["target"]} for v in st["views"]]
            job.append(j)
        metas2, _ = run_java(classes, joml, gson, job, "b")
        for n, st in redo:
            metas[n] = metas2[n]
            res, st2, _ = render_scene(n, prepared[n], metas[n], bindir, opts["ssaa"], opts["verbose"])
            prestate[n] = (res, st2)
    n_err = n_warn = 0
    failed_expect = []
    for n, s in prepared.items():
        res, st = prestate[n]
        sm = metas[n]
        log("== %s  [%s%s]  %d ages" % (n, s["kind"], " " + (s.get("propKind") or s.get("aura") or s.get("model") or ""), len(s["ages"])))
        if st is None:
            path, lines = None, []
        else:
            path, lines = draw_sheet(n, s, sm, res, st, opts["verbose"])
        for l in lines:
            log(l)
        expect = set(s.get("expect", []))
        for lvl, code, msg in res.issues:
            first, *rest = msg.split("\n")
            tag = " (expected)" if code in expect else ""
            log("   %s [%s]%s %s" % (lvl, code, tag, first))
            for r in rest[:12]:
                log("        " + r)
        shown = [(lvl, code) for lvl, code, _ in res.issues if code not in expect]
        n_err += sum(1 for lvl, _ in shown if lvl == "ERROR")
        n_warn += sum(1 for lvl, _ in shown if lvl == "WARNING")
        if expect:
            miss = sorted(expect - res.codes())
            if miss:
                failed_expect.append(n)
                log("   EXPECTATION FAILED: the scene expected %s but they did not occur" % ", ".join(miss))
            else:
                log("   expectations met: %s" % ", ".join(sorted(expect)))
        if path:
            log("   -> %s  (%.1f s)" % (os.path.relpath(path, ROOT) if path.startswith(ROOT) else path, time.time() - st["t0"]))
    log("")
    log("%d scenes: %d errors, %d warnings%s%s" % (len(prepared), n_err, n_warn, (", %d with bad config" % len(failed)) if failed else "",
                                                   (", %d failed expectations" % len(failed_expect)) if failed_expect else ""))
    return 1 if (n_err or failed or failed_expect or load_errors) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
