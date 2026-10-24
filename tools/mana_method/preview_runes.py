"""
Software preview of the Mana Method rune arrays.

The points come from the REAL game class (src/main/java/.../mana/method/RuneGeneratorUtil.java, compiled together with RuneElement / RuneSpec / RuneMods and the perf helpers it
uses and dumped by DumpRunes.java), so the quaternion that turns the plane toward the caster, the rings, the symbols and the star are the game's own. Each point is then drawn the way
the client spawns it: a tinted sprite of the REAL particle textures (mana_mote for the rings and the star, the element's rune glyph for the symbols), blended additively with a
perspective camera. It is not Minecraft (no world, no particle physics, no fading in and out), it shows shape, facing and the order in which the rune draws itself.

    python tools/mana_method/preview_runes.py            # writes docs/previews/mana_method/*.png

Needs JDK 21 (javac/java), Python 3 with numpy + Pillow, and JOML (tools/vfx_preview/lib/joml-1.10.5.jar, or the path in $JOML, or downloaded once from Maven Central).
"""
import json
import math
import os
import subprocess
import sys
import urllib.request

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")
BUILD = os.environ.get("MM_PREVIEW_BUILD") or os.path.join(ROOT, "build", "mana_method_preview")
JOML = os.environ.get("JOML") or os.path.join(ROOT, "tools", "vfx_preview", "lib", "joml-1.10.5.jar")
JOML_URL = "https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar"

SOURCES = [os.path.join(JAVA, "mana", "method", f) for f in ("RuneElement.java", "RuneMods.java", "RuneSpec.java", "RuneGeneratorUtil.java")] + [
    os.path.join(JAVA, "perf", "BoundedCache.java"), os.path.join(JAVA, "perf", "PerfMath.java"), os.path.join(HERE, "DumpRunes.java")]

ELEMENTS = ["FIRE", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT"]
GLYPH_TEX = {"rune_flame": "rune_flame_3.png", "rune_wave": "rune_wave_3.png", "rune_luna": "rune_luna_3.png", "rune_key": "rune_key_3.png", "rune_bolt": "rune_bolt_3.png",
             "rune_leaf": "rune_leaf_3.png"}


def ensure_joml():
    global JOML
    if os.path.exists(JOML):
        return
    os.makedirs(os.path.dirname(JOML), exist_ok=True)
    urllib.request.urlretrieve(JOML_URL, JOML)


def dump(scenes):
    """scenes: list of (name, element, mods, true, progress, look, center, spin, flare) -> list of dicts from DumpRunes."""
    ensure_joml()
    os.makedirs(BUILD, exist_ok=True)
    subprocess.run(["javac", "-cp", JOML, "-d", BUILD] + SOURCES, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""), capture_output=True)
    lines = []
    for name, el, mods, true, progress, look, center, spin, flare in scenes:
        lines.append("%s %s %d %s %.4f %.5f %.5f %.5f %.4f %.4f %.4f %.4f %s" % (name, el, mods, str(true).lower(), progress, look[0], look[1], look[2], center[0], center[1], center[2], spin,
                                                                                    str(flare).lower()))
    res = subprocess.run(["java", "-cp", BUILD + os.pathsep + JOML, "DumpRunes"], input="\n".join(lines), capture_output=True, text=True, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    return [json.loads(l) for l in res.stdout.strip().splitlines() if l.startswith("{")]


_sprites = {}


def sprite(file):
    if file not in _sprites:
        im = Image.open(os.path.join(TEX, file)).convert("RGBA")
        a = np.asarray(im).astype(np.float32) / 255.0
        lum = a[..., :3].max(axis=2) * a[..., 3]                     # the glow of the sprite
        _sprites[file] = lum
    return _sprites[file]


def tinted(file, size, rgb):
    key = (file, size, rgb)
    if key not in _sprites:
        lum = sprite(file)
        im = Image.fromarray((lum * 255).astype(np.uint8)).resize((size, size), Image.BICUBIC)
        l = np.asarray(im).astype(np.float32) / 255.0
        col = np.array(rgb, dtype=np.float32) / 255.0
        _sprites[key] = l[..., None] * col[None, None, :]
    return _sprites[key]


def background(w, h):
    y = np.linspace(0, 1, h)[:, None, None]
    top, bot = np.array([8, 10, 22], np.float32), np.array([30, 22, 44], np.float32)
    return (top * (1 - y) + bot * y) * np.ones((h, w, 3), np.float32) / 255.0


def project(p, cam, fwd, right, up, w, h, fov):
    d = np.array(p) - cam
    z = d @ fwd
    if z < 0.2:
        return None
    f = (w / 2) / math.tan(math.radians(fov) / 2)
    return (w / 2 + (d @ right) / z * f, h / 2 - (d @ up) / z * f, z, f)


def camera(cam, target, upw=(0, 1, 0)):
    cam, target = np.array(cam, float), np.array(target, float)
    fwd = target - cam
    fwd /= np.linalg.norm(fwd)
    up = np.array(upw, float)
    if abs(fwd @ up) > 0.99:
        up = np.array([0, 0, 1.0])
    right = np.cross(fwd, up)
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    return cam, fwd, right, up


def draw_view(scene, cam, target, w=300, h=300, fov=62):
    cam, fwd, right, up = camera(cam, target)
    img = background(w, h)
    tint = scene["tint"]
    rgb = ((tint >> 16) & 255, (tint >> 8) & 255, tint & 255)
    glyph = GLYPH_TEX.get(scene["glyph"], "rune_flame_3.png")
    pts = []
    for x, y, z, kind, ring in scene["points"]:
        pr = project((x, y, z), cam, fwd, right, up, w, h, fov)
        if pr is not None:
            pts.append((pr[2], pr, kind))
    for _, (sx, sy, z, f), kind in sorted(pts, key=lambda t: -t[0]):         # far to near
        if kind == 1:
            tex, world = glyph, 0.34
        elif kind == 3:
            tex, world = "mana_mote_2.png", 0.30
        else:
            tex, world = "mana_mote_0.png", 0.24
        size = max(3, int(world * f / z))
        spr = tinted(tex, size, rgb if kind != 3 else tuple(min(255, c + 70) for c in rgb))
        x0, y0 = int(sx - size / 2), int(sy - size / 2)
        xa, ya, xb, yb = max(0, x0), max(0, y0), min(w, x0 + size), min(h, y0 + size)
        if xb <= xa or yb <= ya:
            continue
        img[ya:yb, xa:xb] += spr[ya - y0:yb - y0, xa - x0:xb - x0] * (1.15 if kind == 1 else 0.9)
    img = 1 - np.exp(-img * 1.6)                                           # soft tone map: additive glows do not clip to flat white
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def side_view(scene, caster, look, w=300, h=300, scale=36.0):
    """Orthographic side schematic in the vertical plane of the look: the caster (left), the look arrow, the rune plane seen edge on and its normal (red) pointing back at the caster."""
    im = Image.fromarray((background(w, h) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    hl = math.hypot(look[0], look[2])
    hx, hz = (look[0] / hl, look[2] / hl) if hl > 0.05 else (1.0, 0.0)            # the horizontal axis of the drawing: along the look (x when looking straight up or down)
    cx, cy = w * 0.22, h * 0.5
    def pt(x, y, z):
        return (cx + ((x - caster[0]) * hx + (z - caster[2]) * hz) * scale, cy - (y - caster[1]) * scale)
    d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(230, 230, 255), width=2)
    ex, ey = pt(caster[0] + look[0] * 2.0, caster[1] + look[1] * 2.0, caster[2] + look[2] * 2.0)
    d.line([cx, cy, ex, ey], fill=(160, 200, 255), width=2)
    d.ellipse([ex - 3, ey - 3, ex + 3, ey + 3], fill=(160, 200, 255))
    tint = scene["tint"]
    rgb = ((tint >> 16) & 255, (tint >> 8) & 255, tint & 255)
    sx = sy = 0.0
    for x, y, z, kind, ring in scene["points"]:
        px, py = pt(x, y, z)
        sx += px
        sy += py
        d.ellipse([px - 1.8, py - 1.8, px + 1.8, py + 1.8], fill=rgb)
    k = len(scene["points"])
    n = scene["normal"]
    nx, ny = (n[0] * hx + n[2] * hz), -n[1]
    d.line([sx / k, sy / k, sx / k + nx * 55, sy / k + ny * 55], fill=(255, 110, 110), width=2)
    return im


def label(im, text, sub=None):
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, im.width, 16], fill=(0, 0, 0))
    d.text((4, 2), text, fill=(235, 235, 255))
    if sub:
        d.text((4, im.height - 14), sub, fill=(170, 175, 205))
    return im


def sheet(rows, title, path, pad=4):
    w, h = rows[0][0].size
    n = max(len(r) for r in rows)
    out = Image.new("RGB", (n * (w + pad) + pad, len(rows) * (h + pad) + pad + 22), (6, 6, 12))
    ImageDraw.Draw(out).text((8, 5), title, fill=(255, 235, 170))
    for ri, row in enumerate(rows):
        for ci, im in enumerate(row):
            out.paste(im, (pad + ci * (w + pad), 22 + pad + ri * (h + pad)))
    os.makedirs(OUT, exist_ok=True)
    out.save(os.path.join(OUT, path))
    return out


def unit(v):
    v = np.array(v, float)
    return v / np.linalg.norm(v)


def main():
    center_off = 2.6                                                      # RuneRules.RUNE_DISTANCE
    # ------------------------------------------------------------------ 1. the plane faces the caster whatever the look
    looks = [("south, level", (0, 0, 1)), ("east, 35 deg down", (1, -0.7, 0)), ("up 55 deg", (0.3, 1.4, 0.5)), ("north-west", (-1, 0.1, -1)), ("steep down", (0.2, -3.5, 0.3)),
             ("west, 20 deg up", (-1, 0.36, 0))]
    scenes, meta = [], []
    caster = np.array([0.0, 70.0, 0.0])
    for i, (nm, lk) in enumerate(looks):
        l = unit(lk)
        c = caster + l * center_off
        scenes.append(("face%d" % i, "FIRE", 3, True, 1.0, l, c, 0.0, False))
        meta.append((nm, l, c))
    data = dump(scenes)
    top, eye = [], []
    for sc, (nm, l, c) in zip(data, meta):
        eye.append(label(draw_view(sc, caster, c, 300, 300, 100), "caster's eye: look " + nm))
        top.append(label(side_view(sc, caster, l), "from the side: the plane edge on, red = its normal"))
    sheet([eye, top], "Mana Method rune (True Flame, both runes): seen from the caster's eye the array is a face-on figure whatever the look; from the side its normal points back at the caster",
          "rune_faces_the_caster.png")

    # ------------------------------------------------------------------ 2. the rune draws itself
    look = unit((0.35, -0.12, 1.0))
    c = caster + look * center_off
    stages = [0.12, 0.3, 0.5, 0.7, 0.88, 1.0]
    scenes = [("seq%d" % i, "FIRE", 3, True, p, look, c, p * 3.0, False) for i, p in enumerate(stages)] + [("flare", "FIRE", 3, True, 1.0, look, c, 3.2, True)]
    data = dump(scenes)
    cam = caster + np.array([-1.5, 0.35, 0.0])                         # a bystander a little to the side
    imgs = []
    for sc, p in zip(data, stages + [1.0]):
        imgs.append(label(draw_view(sc, cam, c, 260, 260, 80), "%d%% of the charge" % round(p * 100) if sc["name"] != "flare" else "release: outer ring flares", "%d points" % len(sc["points"])))
    sheet([imgs], "The rune draws itself over its charge: outer ring, inner rings, the symbols, the star; then the release (True Flame, seen by a bystander)", "rune_drawing_sequence.png")

    # ------------------------------------------------------------------ 3. the elements
    scenes = []
    for el in ELEMENTS:
        scenes.append(("rune_" + el, el, 1, False, 1.0, look, c, 0.4, False))
    for el in ELEMENTS:
        scenes.append(("true_" + el, el, 3, True, 1.0, look, c, 0.4, False))
    data = dump(scenes)
    row1 = [label(draw_view(sc, cam, c, 230, 230, 80), sc["name"].replace("rune_", "rune: ") + "  (%d pts)" % len(sc["points"])) for sc in data[:6]]
    row2 = [label(draw_view(sc, cam, c, 230, 230, 80), sc["name"].replace("true_", "True ") + "  (%d pts)" % len(sc["points"])) for sc in data[6:]]
    sheet([row1, row2], "Rune arrays by element (top: a single modifier, 3 rings; bottom: True Magic with both runes, 5 rings, larger). Glyph particles: rune_flame / wave / luna / key / bolt / leaf", "rune_elements.png")
    for sc in data:
        assert len(sc["points"]) <= 400
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
