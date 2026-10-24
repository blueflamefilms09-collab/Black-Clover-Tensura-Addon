"""
Software preview of the rune-writing look of the Mana Method rune (the owner's reference frame: rings of readable rune symbols, a bright yellow core with an orange-red edge, a tight ring
round the hands, a bigger tilted outer ring, loose glyphs above, yellow lightning arcs, an open glowing grimoire) and the owner's clarification: ACTUAL rune symbols WRITTEN in the air.

The quads come from the REAL classes (src/main/java/.../mana/method/RuneAlphabet.java and RuneGlyphLayout.java, compiled with RuneGeneratorUtil and dumped by DumpGlyphRings.java): the
alphabet's strokes, the pen that walks them, the three rings, the lightning and the book. Every stroke segment is a strip quad in the caster-facing plane, drawn here with a stroke texture
(yellow core, orange-red edge, soft round ends) warped to its projected quad and added to the frame (additive, like the game's glow blend). The body is a flat placeholder for the framing.

    python tools/mana_method/preview_glyph_rings.py   # writes docs/previews/mana_method/rune_glyph_rings.png, rune_glyph_writing.png, rune_glyph_alphabet.png, rune_glyph_charge.png, rune_glyph_lod.png

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
BUILD = os.environ.get("MM_PREVIEW_BUILD") or os.path.join(ROOT, "build", "mana_method_preview")
JOML = os.environ.get("JOML") or os.path.join(ROOT, "tools", "vfx_preview", "lib", "joml-1.10.5.jar")
REFERENCE = os.environ.get("MM_REFERENCE") or os.path.join(HERE, "reference_frame.png")

SOURCES = [os.path.join(JAVA, "mana", "method", f) for f in ("RuneElement.java", "RuneMods.java", "RuneSpec.java", "RuneRules.java", "RuneGeneratorUtil.java", "RuneAlphabet.java", "RuneGlyphLayout.java")] + [
    os.path.join(JAVA, "perf", "BoundedCache.java"), os.path.join(JAVA, "perf", "PerfMath.java"), os.path.join(HERE, "DumpGlyphRings.java")]
W, H = 576, 347

# tint -> (core colour, edge colour, arc colour): the element looks of the stroke texture
TINTS = {
    "gold": ((255, 238, 110), (232, 84, 22), (255, 225, 60)),
    "flame": ((255, 214, 96), (222, 52, 12), (255, 190, 60)),
    "water": ((190, 240, 255), (28, 108, 226), (90, 190, 255)),
    "wind": ((228, 255, 238), (52, 176, 134), (150, 255, 210)),
    "earth": ((255, 232, 170), (150, 98, 44), (230, 190, 110)),
    "lightning": ((255, 255, 190), (240, 168, 20), (255, 235, 70)),
    "plant": ((220, 255, 170), (66, 170, 52), (150, 240, 90)),
}
TINT_OF = {"FIRE": "flame", "WATER": "water", "WIND": "wind", "EARTH": "earth", "LIGHTNING": "lightning", "PLANT": "plant"}


def dump(lines):
    os.makedirs(BUILD, exist_ok=True)
    subprocess.run(["javac", "-cp", JOML, "-d", BUILD] + SOURCES, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""), capture_output=True)
    res = subprocess.run(["java", "-cp", BUILD + os.pathsep + JOML, "DumpGlyphRings"], input="\n".join(lines), capture_output=True, text=True, check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    return [json.loads(l) for l in res.stdout.strip().splitlines() if l.startswith("{")]


def line(name, el, mods, true, age, charge, look, ctr, cam, det, single=None):
    s = "%s %s %d %s %.3f %d %s %s %s %.4f" % (name, el, mods, str(true).lower(), age, charge, " ".join("%.5f" % x for x in look), " ".join("%.5f" % x for x in ctr), " ".join("%.5f" % x for x in cam), det)
    if single:
        s += " %d %.4f %.4f" % single
    return s


# ------------------------------------------------------------------------------------------------ textures
_tex = {}


def stroke_tex(tint):
    """The stroke strip: along the stroke (x, 40 px) and across it (y, 32 px): a bright core, an orange-red edge, soft round ends. Premultiplied RGBA float."""
    if ("s", tint) not in _tex:
        core, edge, _ = TINTS[tint]
        L, A = 40, 32
        t = np.linspace(-1, 1, A)[:, None]
        u = np.linspace(0, 1, L)[None, :]
        coreK = np.exp(-(t / 0.36) ** 2)
        alpha = np.clip((1.0 - np.abs(t)) / 0.45, 0, 1) ** 1.2 * np.ones((A, L))
        end = np.clip(np.minimum(u, 1 - u) / 0.14, 0, 1) ** 0.8
        alpha = alpha * end
        rgb = (np.array(edge)[None, None, :] / 255.0) * (1 - coreK[..., None]) + (np.array(core)[None, None, :] / 255.0) * coreK[..., None]
        _tex[("s", tint)] = np.dstack([rgb * alpha[..., None], alpha])
    return _tex[("s", tint)]


def pen_tex():
    if "pen" not in _tex:
        n = 48
        y, x = np.mgrid[-1:1:n * 1j, -1:1:n * 1j]
        r = np.sqrt(x * x + y * y)
        a = np.clip(1 - r, 0, 1) ** 2.0
        col = np.array([1.0, 0.95, 0.65])[None, None, :] * np.clip(1 - r * 1.2, 0, 1)[..., None] + np.array([1.0, 0.55, 0.2])[None, None, :] * np.clip(r * 1.2, 0, 1)[..., None]
        _tex["pen"] = np.dstack([col * a[..., None], a])
    return _tex["pen"]


def page_tex():
    if "page" not in _tex:
        t = Image.new("RGBA", (96, 96), (255, 214, 96, 255))
        d = ImageDraw.Draw(t)
        d.rectangle([0, 0, 95, 95], outline=(150, 70, 20, 255), width=5)
        d.rectangle([12, 12, 83, 83], outline=(255, 244, 170, 255), width=3)
        for k in range(18, 80, 14):
            d.polygon([(k, 48), (k + 7, 41), (k + 14, 48), (k + 7, 55)], fill=(190, 90, 25, 255))
            d.line([(k, 22), (k + 7, 30)], fill=(190, 90, 25, 255), width=2)
            d.line([(k, 74), (k + 7, 66)], fill=(190, 90, 25, 255), width=2)
        _tex["page"] = np.asarray(t).astype(np.float32) / 255.0
    return _tex["page"]


def glow_tex():
    if "glow" not in _tex:
        n = 64
        y, x = np.mgrid[-1:1:n * 1j, -1:1:n * 1j]
        a = np.clip(1 - np.sqrt(x * x + y * y), 0, 1) ** 2.2
        _tex["glow"] = np.dstack([np.stack([a, a * 0.82, a * 0.27], axis=2), a])
    return _tex["glow"]


def coeffs(dst, src):
    a, b = [], []
    for (x, y), (u, v) in zip(dst, src):
        a.append([x, y, 1, 0, 0, 0, -u * x, -u * y])
        a.append([0, 0, 0, x, y, 1, -v * x, -v * y])
        b += [u, v]
    return np.linalg.solve(np.array(a, float), np.array(b, float))


def blit(acc, tex, pts, gain, premult=True):
    """Warps a float RGBA texture (premultiplied) onto the projected quad (corners in the texture's top-left, bottom-left, bottom-right, top-right order) and adds it into acc."""
    h, w = tex.shape[:2]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    x0, y0 = max(0, int(math.floor(min(xs))) - 1), max(0, int(math.floor(min(ys))) - 1)
    x1, y1 = min(acc.shape[1], int(math.ceil(max(xs))) + 2), min(acc.shape[0], int(math.ceil(max(ys))) + 2)
    if x1 <= x0 or y1 <= y0 or (x1 - x0) * (y1 - y0) > 400000:
        return
    src = [(0, 0), (0, h), (w, h), (w, 0)]
    dst = [(p[0] - x0, p[1] - y0) for p in pts]
    try:
        c = coeffs(dst, src)
    except np.linalg.LinAlgError:
        return
    chans = []
    for k in range(4):
        im = Image.fromarray(tex[..., k].astype(np.float32), "F")
        chans.append(np.asarray(im.transform((x1 - x0, y1 - y0), Image.PERSPECTIVE, tuple(c), Image.BILINEAR)))
    acc[y0:y1, x0:x1] += np.dstack(chans[:3]) * gain


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


def background(w, h, plain=False, body_scale=1.0):
    y = np.linspace(0, 1, h)[:, None, None]
    top, bot = (np.array([8, 10, 22], np.float32), np.array([30, 22, 44], np.float32)) if plain else (np.array([34, 52, 46], np.float32), np.array([20, 24, 30], np.float32))
    im = Image.fromarray((top * (1 - y) + bot * y).astype(np.uint8) * np.ones((h, w, 3), np.uint8))
    if not plain:
        d = ImageDraw.Draw(im)
        cx = w * 0.5
        d.polygon([(cx - 270, 0), (cx - 190, h), (cx + 200, h), (cx + 280, 0)], fill=(104, 82, 98))
        d.polygon([(cx - 110, 0), (cx - 80, h), (cx + 90, h), (cx + 120, 0)], fill=(38, 34, 44))
        d.ellipse([cx - 62, 150, cx - 12, 190], fill=(210, 190, 176))
        d.ellipse([cx + 8, 150, cx + 58, 190], fill=(210, 190, 176))
    return np.asarray(im).astype(np.float32) / 255.0


def render(scene, cam, tint, label=None, w=W, h=H, plain=False, stats=True):
    img = background(w, h, plain)
    acc = np.zeros((h, w, 3), np.float32)
    # the book behind everything
    if scene["book"] is not None:
        q = scene["book"]
        g = [cam.project(q[24 + k * 3:24 + k * 3 + 3]) for k in range(4)]
        if all(g):
            blit(acc, glow_tex(), [g[3], g[0], g[1], g[2]], 0.9 * scene["bookLight"])
        for side in range(2):
            pts = [cam.project(q[side * 12 + k * 3:side * 12 + k * 3 + 3]) for k in range(4)]
            if all(pts):
                dst = [pts[1], pts[0], pts[3], pts[2]] if side == 1 else [pts[2], pts[3], pts[0], pts[1]]
                blit(acc, page_tex(), dst, 0.55 + 0.45 * scene["bookLight"])
    # the written strokes, far to near
    tex = stroke_tex(tint)
    strips = scene["strips"]
    def depth(s):
        q = np.array(s["q"]).reshape(4, 3).mean(axis=0)
        return -np.linalg.norm(q - cam.pos)
    for s in sorted(strips, key=depth):
        pts = [cam.project(s["q"][k * 3:k * 3 + 3]) for k in range(4)]
        if all(pts):
            # strip corners: start-left, start-right, end-right, end-left; the texture runs along x (start to end) and across y (left to right)
            blit(acc, tex, [pts[0], pts[1], pts[2], pts[3]], min(2.0, s["light"]))
    for pn in scene["pens"]:
        pts = [cam.project(pn["q"][k * 3:k * 3 + 3]) for k in range(4)]
        if all(pts):
            blit(acc, pen_tex(), pts, 0.9 * pn["light"])
    # the lightning arcs: a wide soft strip and a thin bright core
    arc_col = TINTS[tint][2]
    layer = Image.new("RGB", (w, h), (0, 0, 0))
    core = Image.new("RGB", (w, h), (0, 0, 0))
    dl, dc = ImageDraw.Draw(layer), ImageDraw.Draw(core)
    for a in scene["arcs"]:
        pts = [cam.project(a["q"][k * 3:k * 3 + 3]) for k in range(4)]
        if not all(pts):
            continue
        m1 = ((pts[0][0] + pts[1][0]) / 2, (pts[0][1] + pts[1][1]) / 2)
        m2 = ((pts[2][0] + pts[3][0]) / 2, (pts[2][1] + pts[3][1]) / 2)
        dl.line([m1, m2], fill=arc_col, width=max(3, int(w / 80)))
        dc.line([m1, m2], fill=(255, 255, 220), width=max(1, int(w / 280)))
    soft = np.asarray(layer.filter(ImageFilter.GaussianBlur(max(1.5, w / 190)))).astype(np.float32) / 255.0
    sharp = np.asarray(layer).astype(np.float32) / 255.0 * 0.35 + np.asarray(core).astype(np.float32) / 255.0
    out = img + acc + soft * 0.9 + sharp
    im = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))
    if label is not None:
        d = ImageDraw.Draw(im)
        d.rectangle([0, 0, w, 15], fill=(0, 0, 0))
        d.text((4, 2), label, fill=(235, 235, 255))
        if stats:
            d.text((w - 235, 2), "%d quads (strips %d, pens %d, arcs %d)" % (scene["quads"], len(strips), len(scene["pens"]), len(scene["arcs"])), fill=(255, 230, 150))
    return im


def sheet(images, path, title, cols, pad=4, bg=(6, 6, 12)):
    w, h = max(i.size[0] for i in images), max(i.size[1] for i in images)
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (cols * (w + pad) + pad, rows * (h + pad) + pad + 20), bg)
    ImageDraw.Draw(out).text((8, 4), title, fill=(255, 235, 170))
    for i, im in enumerate(images):
        out.paste(im, (pad + (i % cols) * (w + pad), 20 + pad + (i // cols) * (h + pad)))
    os.makedirs(OUT, exist_ok=True)
    out.save(os.path.join(OUT, path))
    return out


def unit(v):
    v = np.array(v, float)
    return v / np.linalg.norm(v)


def main():
    look = unit((0.0, -0.05, 1.0))
    caster = np.array([0.0, 70.0, 0.0])
    centre = caster + look * 2.6                                               # RuneRules.RUNE_DISTANCE
    cam_pos = caster + np.array([0.30, 0.55, -0.35])
    target = centre + np.array([0.0, -0.1, 0.0])
    cam = Cam(cam_pos, target, 94, W, H)
    charge = 18
    lines = []
    def rune(name, age, el="FIRE", mods=3, true=True, det=1.0):
        lines.append(line(name, el, mods, true, age, charge, look, centre, cam_pos, det))
    rune("final", 18.0)
    lines.append(line("faceon", "FIRE", 3, True, 18.0, charge, look, centre, caster, 1.0))
    for age in (1, 3, 5, 7, 9, 11, 13, 15, 18):
        rune("c%d" % age, float(age))
    rune("rel", 21.0)
    for det in (1.0, 0.75, 0.5, 0.2):
        rune("lod%.2f" % det, 19.0, det=det)
    for el in ("FIRE", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT"):
        rune("el" + el, 19.0, el=el, mods=1, true=el == "LIGHTNING")
    # one glyph being written, face on (the glyph raido: a stem, a bowl and a leg), then the whole alphabet at 4x
    fl = unit((0, 0, 1))
    gc = np.array([0.0, 70.0, 0.0])
    gcam = gc - fl * 2.6
    WRITE = [0.10, 0.22, 0.34, 0.46, 0.58, 0.72, 0.86, 1.0]
    for i, wv in enumerate(WRITE):
        lines.append(line("w%d" % i, "FIRE", 3, True, 10.0, charge, fl, gc, gcam, 1.0, (4, wv, 1.0)))
    for g in range(32):
        lines.append(line("a%d" % g, "FIRE", 3, True, 10.0, charge, fl, gc, gcam, 1.0, (g, 1.0, 1.0)))
    lines.append("#alphabet")
    out = dump(lines)
    alpha = [d for d in out if "alphabet" in d][0]["alphabet"]
    data = {d["name"]: d for d in out if "name" in d}
    for d in data.values():
        if d["name"].startswith(("a", "w")) and d["name"][1:].isdigit():
            continue
        assert d["quads"] <= d["expected"] and d["quads"] <= 220, (d["name"], d["quads"], d["expected"])
    os.makedirs(OUT, exist_ok=True)

    # 1. the new rune beside the reference frame
    main_im = render(data["final"], cam, "gold", "True Flame rune, charged (tick 18 of 18, placeholder body)")
    ref = Image.open(REFERENCE).convert("RGB").resize((W, H)) if os.path.exists(REFERENCE) else Image.new("RGB", (W, H), (30, 30, 30))
    d = ImageDraw.Draw(ref)
    d.rectangle([0, 0, W, 15], fill=(0, 0, 0))
    d.text((4, 2), "owner's reference frame", fill=(235, 235, 255))
    eye_cam = Cam(caster, centre, 84, 420, 420)
    data["faceon"]["arcs"] = []
    data["faceon"]["book"] = None
    eye_im = render(data["faceon"], eye_cam, "gold", "caster's eye, face on", 420, 420, True)
    sheet([ref, main_im, eye_im], "rune_glyph_rings.png", "Written rune symbols: the owner's reference (left), the new rune at the same framing (middle) and from the caster's eye (right)", 3)

    # 2. the writing: eight frames of one glyph, the ring being written, the reference beside them
    gcam_obj = Cam(gcam, gc, 46, 260, 260)
    frames = []
    for i, wv in enumerate(WRITE):
        frames.append(render(data["w%d" % i], gcam_obj, "gold", "pen at %d%% of the glyph" % round(wv * 100), 260, 260, True, False))
    grid = Image.new("RGB", (4 * 264 + 4, 2 * 264 + 4 + 20), (6, 6, 12))
    ImageDraw.Draw(grid).text((8, 4), "One glyph (raido) being written: the pen walks the strokes, a bright head, the line cools behind it", fill=(255, 235, 170))
    for i, im in enumerate(frames):
        grid.paste(im, (4 + (i % 4) * 264, 20 + (i // 4) * 264))
    ringseq = []
    small = Cam(cam_pos, target, 94, W, H)
    for name, lab in (("c3", "tick 3"), ("c7", "tick 7"), ("c11", "tick 11"), ("c15", "tick 15"), ("c18", "tick 18: written")):
        ringseq.append(render(data[name], small, "gold", lab, stats=False).resize((W // 2, H // 2), Image.LANCZOS))
    refsmall = ref.resize((W // 2, H // 2), Image.LANCZOS)
    full = Image.new("RGB", (max(grid.width, 4 + 4 * (W // 2 + 4)), grid.height + H // 2 + 30), (6, 6, 12))
    full.paste(grid, (0, 0))
    y = grid.height + 4
    row1 = [refsmall] + ringseq[:3]
    for i, im in enumerate(row1):
        full.paste(im, (4 + i * (W // 2 + 4), y))
    y += H // 2 + 4
    ImageDraw.Draw(full).text((8, y), "the reference frame, then the ring being written glyph after glyph (ticks 3, 7, 11 and see rune_glyph_charge.png for the rest)", fill=(255, 235, 170))
    full.save(os.path.join(OUT, "rune_glyph_writing.png"))

    # 3. the alphabet at 4x
    cell = 4 * 32 + 16
    sheetim = Image.new("RGB", (8 * cell + 8, 4 * (cell + 14) + 28), (8, 8, 18))
    ImageDraw.Draw(sheetim).text((8, 6), "Mana Method rune alphabet: 32 hand-authored glyphs of 2 to 6 straight strokes (shown at 4x, the writing pen in the real class)", fill=(255, 235, 170))
    acam = Cam(gcam, gc, 46, 4 * 32, 4 * 32)
    for g in range(32):
        im = render(data["a%d" % g], acam, "gold", None, 4 * 32, 4 * 32, True, False)
        x, yy = 4 + (g % 8) * cell + 8, 24 + (g // 8) * (cell + 14)
        sheetim.paste(im, (x, yy))
        ImageDraw.Draw(sheetim).text((x + 2, yy + 4 * 32 + 1), "%d %s (%d)" % (g, alpha[g]["name"], alpha[g]["segments"]), fill=(150, 160, 200))
    sheetim.save(os.path.join(OUT, "rune_glyph_alphabet.png"))

    # 4. the charge sequence of the ring
    strip = []
    for name, lab in (("c1", "tick 1"), ("c3", "tick 3: hand ring"), ("c5", "tick 5"), ("c7", "tick 7: middle ring"), ("c9", "tick 9"), ("c11", "tick 11: outer ring"), ("c13", "tick 13: floating, arcs"),
                      ("c15", "tick 15"), ("c18", "tick 18: charged"), ("rel", "release: outer ring flares")):
        strip.append(render(data[name], small, "gold", lab, stats=False).resize((W // 2, H // 2), Image.LANCZOS))
    sheet(strip, "rune_glyph_charge.png", "Charge (18 ticks): glyph after glyph is written, each ring at once; the book and the arcs come in last", 5)

    # 5. levels of detail and elements (each element has its own words)
    lod = [render(data["lod%.2f" % det], small, "gold", "detail %.2f -> level %d" % (det, data["lod%.2f" % det]["level"])).resize((W // 2, H // 2), Image.LANCZOS) for det in (1.0, 0.75, 0.5, 0.2)]
    els = [render(data["el" + el], small, TINT_OF[el] if el != "LIGHTNING" else "gold", el.lower()).resize((W // 2, H // 2), Image.LANCZOS) for el in ("FIRE", "WATER", "WIND", "EARTH", "LIGHTNING", "PLANT")]
    sheet(lod + els, "rune_glyph_lod.png", "Levels of detail (full, medium, low, minimal) and the six elements, each writing its own words", 5)
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
