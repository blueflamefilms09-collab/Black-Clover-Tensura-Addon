"""
Software preview of the glyph-ring look of the Mana Method rune (the owner's reference frame: rings of readable rune glyphs with a bright core and an orange-red outline, a tight ring
round the hands, a bigger tilted outer ring, loose glyphs floating above, yellow lightning arcs, an open glowing grimoire).

The quads come from the REAL class (src/main/java/.../mana/method/RuneGlyphLayout.java, compiled with RuneGeneratorUtil and dumped by DumpGlyphRings.java); the glyph sprites are the atlas of
gen_rune_glyphs.py. Every glyph is warped with a perspective transform to its projected quad and added to the frame (additive, like the game's glow blend). The caster's body is a flat
placeholder to give the framing of the reference; it is not a model.

    python tools/mana_method/gen_rune_glyphs.py        # first: the atlases
    python tools/mana_method/preview_glyph_rings.py    # writes docs/previews/mana_method/rune_glyph_rings.png, rune_glyph_charge.png, rune_glyph_lod.png

Needs JDK 21, Python 3 with numpy + Pillow, and JOML (tools/vfx_preview/lib/joml-1.10.5.jar, or $JOML).
"""
import json
import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")
ATLAS_DIR = os.path.join(OUT, "glyph_atlas")
BUILD = os.environ.get("MM_PREVIEW_BUILD") or os.path.join(ROOT, "build", "mana_method_preview")
JOML = os.environ.get("JOML") or os.path.join(ROOT, "tools", "vfx_preview", "lib", "joml-1.10.5.jar")
REFERENCE = os.environ.get("MM_REFERENCE") or os.path.join(HERE, "reference_frame.png")

SOURCES = [os.path.join(JAVA, "mana", "method", f) for f in ("RuneElement.java", "RuneMods.java", "RuneSpec.java", "RuneRules.java", "RuneGeneratorUtil.java", "RuneGlyphLayout.java")] + [
    os.path.join(JAVA, "perf", "BoundedCache.java"), os.path.join(JAVA, "perf", "PerfMath.java"), os.path.join(HERE, "DumpGlyphRings.java")]
TINT_OF = {"FIRE": "flame", "WATER": "water", "WIND": "wind", "EARTH": "earth", "LIGHTNING": "lightning", "PLANT": "plant"}
W, H = 576, 347
ARC = {"gold": (255, 225, 60), "flame": (255, 190, 60), "water": (90, 190, 255), "wind": (150, 255, 210), "earth": (230, 190, 110), "lightning": (255, 235, 70), "plant": (150, 240, 90)}


def dump(scenes):
    os.makedirs(BUILD, exist_ok=True)
    subprocess.run(["javac", "-cp", JOML, "-d", BUILD] + SOURCES, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""), capture_output=True)
    # (the format of DumpGlyphRings: name element mods true age charge look(3) centre(3) camera(3) detail)
    lines = ["%s %s %d %s %.3f %d %s %s %s %.4f" % (n, el, mods, str(tr).lower(), age, charge, " ".join("%.5f" % x for x in look), " ".join("%.5f" % x for x in ctr),
                                                  " ".join("%.5f" % x for x in cam), det) for n, el, mods, tr, age, charge, look, ctr, cam, det in scenes]
    res = subprocess.run(["java", "-cp", BUILD + os.pathsep + JOML, "DumpGlyphRings"], input="\n".join(lines), capture_output=True, text=True, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    return [json.loads(l) for l in res.stdout.strip().splitlines() if l.startswith("{")]


_atlas = {}


def tile(tint, cell):
    if tint not in _atlas:
        _atlas[tint] = Image.open(os.path.join(ATLAS_DIR, "rune_glyphs_%s.png" % tint)).convert("RGBA")
    r, c = divmod(cell, 8)
    t = _atlas[tint].crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))
    return t.resize((96, 96), Image.BICUBIC)


def coeffs(dst, src):
    """Perspective coefficients that map OUTPUT pixels (dst quad) to INPUT pixels (src quad), as PIL wants."""
    a = []
    b = []
    for (x, y), (u, v) in zip(dst, src):
        a.append([x, y, 1, 0, 0, 0, -u * x, -u * y])
        a.append([0, 0, 0, x, y, 1, -v * x, -v * y])
        b += [u, v]
    return np.linalg.solve(np.array(a, float), np.array(b, float))


class Cam:
    def __init__(self, pos, target, fov, w, h):
        self.pos = np.array(pos, float)
        f = np.array(target, float) - self.pos
        self.fwd = f / np.linalg.norm(f)
        up = np.array([0, 1.0, 0])
        if abs(self.fwd @ up) > 0.99:
            up = np.array([0, 0, 1.0])
        self.right = np.cross(self.fwd, up)
        self.right /= np.linalg.norm(self.right)
        self.up = np.cross(self.right, self.fwd)
        self.w, self.h = w, h
        self.f = (w / 2) / math.tan(math.radians(fov) / 2)

    def project(self, p):
        d = np.array(p, float) - self.pos
        z = d @ self.fwd
        if z < 0.15:
            return None
        return (self.w / 2 + (d @ self.right) / z * self.f, self.h / 2 - (d @ self.up) / z * self.f)


def body(img):
    """A flat placeholder for the caster so the rings sit at the framing of the reference frame (cape, torso, two hands)."""
    d = ImageDraw.Draw(img)
    cx = W * 0.5
    d.polygon([(cx - 270, 0), (cx - 190, H), (cx + 200, H), (cx + 280, 0)], fill=(104, 82, 98))
    d.polygon([(cx - 110, 0), (cx - 80, H), (cx + 90, H), (cx + 120, 0)], fill=(38, 34, 44))
    d.ellipse([cx - 62, 150, cx - 12, 190], fill=(210, 190, 176))
    d.ellipse([cx + 8, 150, cx + 58, 190], fill=(210, 190, 176))


def background():
    y = np.linspace(0, 1, H)[:, None, None]
    top, bot = np.array([34, 52, 46], np.float32), np.array([20, 24, 30], np.float32)
    im = Image.fromarray((top * (1 - y) + bot * y).astype(np.uint8) * np.ones((H, W, 3), np.uint8))
    body(im)
    return np.asarray(im).astype(np.float32) / 255.0


def page_texture():
    """The open book's pages: gold with a darker patterned border (the reference's grimoire)."""
    t = Image.new("RGBA", (96, 96), (255, 214, 96, 255))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, 95, 95], outline=(150, 70, 20, 255), width=5)
    d.rectangle([12, 12, 83, 83], outline=(255, 244, 170, 255), width=3)
    for k in range(18, 80, 14):
        d.polygon([(k, 48), (k + 7, 41), (k + 14, 48), (k + 7, 55)], fill=(190, 90, 25, 255))
    for k in range(18, 80, 14):
        d.line([(k, 22), (k + 7, 30)], fill=(190, 90, 25, 255), width=2)
        d.line([(k, 74), (k + 7, 66)], fill=(190, 90, 25, 255), width=2)
    return t


def warp(sprite, quad, cam_pts):
    """Warps an RGBA sprite onto the projected quad: returns (rgb float, alpha float) full-frame arrays."""
    w, h = sprite.size
    # sprite corners: top-left, top-right, bottom-right, bottom-left  <->  quad corners given in that order
    src = [(0, 0), (w, 0), (w, h), (0, h)]
    c = coeffs(cam_pts, src)
    im = sprite.transform((W, H), Image.PERSPECTIVE, tuple(c), Image.BICUBIC)
    a = np.asarray(im).astype(np.float32) / 255.0
    return a[..., :3], a[..., 3]


def render(scene, cam, tint, label=None, show_stats=True):
    img = background()
    glow = np.zeros((H, W, 3), np.float32)
    add = np.zeros((H, W, 3), np.float32)
    # the book first (behind): glow, then the pages
    if scene["book"] is not None:
        q = scene["book"]
        gl = [cam.project(q[24 + k * 3:24 + k * 3 + 3]) for k in range(4)]
        if all(gl):
            g = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
            gd = ImageDraw.Draw(g)
            for r in range(32, 0, -1):
                gd.ellipse([32 - r, 32 - r, 32 + r, 32 + r], fill=(255, 210, 70, int(255 * (1 - r / 32) ** 2.2)))
            rgb, a = warp(g, None, [gl[3], gl[2], gl[1], gl[0]])
            glow += rgb * a[..., None] * 0.9 * scene["bookLight"]
        for side in range(2):
            pts = [cam.project(q[side * 12 + k * 3:side * 12 + k * 3 + 3]) for k in range(4)]
            if all(pts):
                # corners: 0 spine-bottom, 1 spine-top, 2 outer-top, 3 outer-bottom; texture left page mirrored so the pattern is symmetric
                tex = page_texture()
                dst = [pts[1], pts[2], pts[3], pts[0]] if side == 1 else [pts[2], pts[1], pts[0], pts[3]]
                rgb, a = warp(tex, None, dst)
                add += rgb * a[..., None] * (0.55 + 0.45 * scene["bookLight"])
    # the glyphs, far to near
    order = sorted(range(len(scene["glyphs"])), key=lambda i: -np.linalg.norm(np.array(scene["glyphs"][i]["c"]) - cam.pos))
    for i in order:
        g = scene["glyphs"][i]
        if g["light"] <= 0.01:
            continue
        c, r, u = (np.array(g[k]) for k in ("c", "r", "u"))
        quad = [c - r + u, c + r + u, c + r - u, c - r - u]                          # top-left, top-right, bottom-right, bottom-left of the sprite
        pts = [cam.project(p) for p in quad]
        if not all(pts):
            continue
        rgb, a = warp(tile(tint, g["cell"]), None, pts)
        add += rgb * a[..., None] * min(1.7, g["light"])
    # the lightning arcs: a wide soft strip and a thin bright core
    layer = Image.new("RGB", (W, H), (0, 0, 0))
    core = Image.new("RGB", (W, H), (0, 0, 0))
    dl, dc = ImageDraw.Draw(layer), ImageDraw.Draw(core)
    for a in scene["arcs"]:
        q = a["q"]
        pts = [cam.project(q[k * 3:k * 3 + 3]) for k in range(4)]
        if not all(pts):
            continue
        mid1 = ((pts[0][0] + pts[1][0]) / 2, (pts[0][1] + pts[1][1]) / 2)
        mid2 = ((pts[2][0] + pts[3][0]) / 2, (pts[2][1] + pts[3][1]) / 2)
        dl.line([mid1, mid2], fill=ARC[tint], width=7)
        dc.line([mid1, mid2], fill=(255, 255, 220), width=2)
    soft = np.asarray(layer.filter(ImageFilter.GaussianBlur(3.0))).astype(np.float32) / 255.0
    sharp = np.asarray(layer).astype(np.float32) / 255.0 * 0.35 + np.asarray(core).astype(np.float32) / 255.0
    out = img + glow + add + soft * 0.9 + sharp
    out = out / (1.0 + 0.0 * out)
    im = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))
    if show_stats:
        d = ImageDraw.Draw(im)
        d.rectangle([0, 0, W, 15], fill=(0, 0, 0))
        d.text((4, 2), label or "", fill=(235, 235, 255))
        d.text((W - 250, 2), "%d quads (glyphs %d, arcs %d, book %d)" % (scene["quads"], len(scene["glyphs"]), len(scene["arcs"]), 3 if scene["book"] else 0), fill=(255, 230, 150))
    return im


def sheet(images, path, title, cols, pad=4):
    w, h = images[0].size
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (cols * (w + pad) + pad, rows * (h + pad) + pad + 20), (6, 6, 12))
    ImageDraw.Draw(out).text((8, 4), title, fill=(255, 235, 170))
    for i, im in enumerate(images):
        out.paste(im, (pad + (i % cols) * (w + pad), 20 + pad + (i // cols) * (h + pad)))
    out.save(os.path.join(OUT, path))
    return out


def main():
    look = np.array([0.0, -0.05, 1.0])
    look /= np.linalg.norm(look)
    caster = np.array([0.0, 70.0, 0.0])
    centre = caster + look * 2.6                                               # RuneRules.RUNE_DISTANCE
    cam_pos = caster + np.array([0.30, 0.55, -0.35])                           # a shoulder behind and above the caster, so the glyph faces are seen as the caster sees them
    cam = Cam(cam_pos, centre + np.array([0.0, -0.1, 0.0]), 78, W, H)
    charge = 18
    scenes = [("final", "FIRE", 3, True, 18.0, charge, look, centre, cam_pos, 1.0)]
    for age in (1, 3, 5, 7, 9, 11, 13, 15, 18):
        scenes.append(("c%d" % age, "FIRE", 3, True, float(age), charge, look, centre, cam_pos, 1.0))
    scenes.append(("rel", "FIRE", 3, True, 21.0, charge, look, centre, cam_pos, 1.0))
    for det in (1.0, 0.75, 0.5, 0.2):
        scenes.append(("lod%.2f" % det, "FIRE", 3, True, 16.0, charge, look, centre, cam_pos, det))
    for el in ("FIRE", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT"):
        scenes.append(("el" + el, el, 1, el == "LIGHTNING", 16.0, charge, look, centre, cam_pos, 1.0))
    data = {d["name"]: d for d in dump(scenes)}
    for d in data.values():
        assert d["quads"] <= d["expected"], d["name"]
        assert d["quads"] <= 72, d["quads"]
    os.makedirs(OUT, exist_ok=True)

    # 1. side by side with the reference frame
    main_im = render(data["final"], cam, "gold", "True Flame rune, charged (tick 18 of 18, placeholder body)")
    ref = Image.open(REFERENCE).convert("RGB").resize((W, H)) if os.path.exists(REFERENCE) else Image.new("RGB", (W, H), (30, 30, 30))
    d = ImageDraw.Draw(ref)
    d.rectangle([0, 0, W, 15], fill=(0, 0, 0))
    d.text((4, 2), "owner's reference frame", fill=(235, 235, 255))
    sheet([ref, main_im], "rune_glyph_rings.png", "Glyph rings: the owner's reference (left) and the new rune (right), same framing", 2)

    # 2. the charge sequence
    strip = []
    small = Cam(cam_pos, centre + np.array([0.0, -0.1, 0.0]), 78, W, H)
    for name, lab in (("c1", "tick 1"), ("c3", "tick 3: hand ring"), ("c5", "tick 5"), ("c7", "tick 7: middle ring"), ("c9", "tick 9"), ("c11", "tick 11: outer ring"), ("c13", "tick 13: floating, arcs"),
                      ("c15", "tick 15"), ("c18", "tick 18: charged"), ("rel", "release: outer ring flares")):
        im = render(data[name], small, "gold", lab)
        strip.append(im.resize((W // 2, H // 2), Image.LANCZOS))
    sheet(strip, "rune_glyph_charge.png", "Charge sequence (18 ticks): glyphs light up one after the other, hand ring first; rings counter-rotate; the book and the arcs come in last", 5)

    # 3. levels of detail and elements
    lod = [render(data["lod%.2f" % det], small, "gold", "detail %.2f -> level %d" % (det, data["lod%.2f" % det]["level"])).resize((W // 2, H // 2), Image.LANCZOS) for det in (1.0, 0.75, 0.5, 0.2)]
    els = [render(data["el" + el], small, TINT_OF[el] if el != "LIGHTNING" else "gold", el.lower()).resize((W // 2, H // 2), Image.LANCZOS) for el in ("FIRE", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT")]
    sheet(lod + els, "rune_glyph_lod.png", "Levels of detail (top row: full, medium, low, minimal) and the element tints (single modifier)", 5)
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
