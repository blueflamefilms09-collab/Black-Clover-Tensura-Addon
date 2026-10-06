"""
Headless VFX preview for the nusmp VFX layers.

Compiles the REAL layer classes (src/main/java/.../vfx/client/layer/*) together with the real VfxPose / VfxAnim / VfxBloom /
VfxTextures / VfxShape and lightly patched copies of VfxVertexBuffer / VfxBlend (GL calls stripped, a drain() added) against
small Minecraft stubs (stubs/) and JOML, runs each scene's frames through Preview.java, and rasterises the emitted quads with the
real textures: perspective-correct UVs, vertex colours, ALPHA / WATER / NEGATIVE / ADD blending in the same order as the game.

    python tools/vfx_preview/preview.py                 # every scene in scenes.json
    python tools/vfx_preview/preview.py fire_lion water_dragon

Needs: JDK 21 (javac/java), Python 3 with numpy + Pillow. Downloads JOML 1.10.5 from Maven Central once (tools/vfx_preview/lib).
Output: build/vfx_preview/<scene>.png (frames side by side) and a vertex-budget report.
Not a Minecraft renderer: no world, lighting or depth against blocks; it shows shape, colour, motion and budget.
"""
import json
import math
import os
import re
import shutil
import subprocess
import sys
import urllib.request

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java")
VFX = os.path.join(SRC, "com", "newuniverse", "nusmp", "vfx")
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
BUILD = os.path.join(ROOT, "build", "vfx_preview")
JOML = os.path.join(HERE, "lib", "joml-1.10.5.jar")
JOML_URL = "https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar"

W, H, FOV = 480, 360, 70.0


# ------------------------------------------------------------------------------------------------ build
def patched_vertex_buffer(src):
    src = re.sub(r"import com\.mojang[^\n]*\n|import net\.minecraft\.client[^\n]*\n", "", src)
    a = src.index("    // ------------------------------------------------------------------ flush")
    b = src.index("    // ------------------------------------------------------------------ color helpers")
    drain = '''    // preview: hand the collected quads out instead of drawing them
    public List<Object[]> drain() {
        List<Object[]> out = new ArrayList<>();
        for (Map.Entry<Key, Batch> e : batches.entrySet()) {
            Batch bt = e.getValue();
            for (int i = 0; i + 24 <= bt.size; i += 24) {
                out.add(new Object[]{e.getKey().texture().getPath(), e.getKey().blend().name(), e.getKey().blend().order, Arrays.copyOfRange(bt.data, i, i + 24)});
            }
        }
        batches.clear();
        return out;
    }

'''
    return src[:a] + drain + src[b:]


def patched_blend(src):
    src = re.sub(r"import com\.mojang[^\n]*\n", "", src)
    a = src.index("    public void apply() {")
    b = src.index("\n    }\n", a) + len("\n    }\n")
    return src[:a] + src[b:]


def build(layers):
    if not os.path.isfile(JOML):
        os.makedirs(os.path.dirname(JOML), exist_ok=True)
        print("downloading JOML ...")
        urllib.request.urlretrieve(JOML_URL, JOML)
    gen = os.path.join(BUILD, "src")
    shutil.rmtree(gen, ignore_errors=True)
    shutil.copytree(os.path.join(HERE, "stubs"), gen)
    pkg = os.path.join(gen, "com", "newuniverse", "nusmp", "vfx")
    os.makedirs(os.path.join(pkg, "client", "layer"), exist_ok=True)
    shutil.copy(os.path.join(VFX, "VfxShape.java"), pkg)
    for name in ("AbstractVfxLayer", "VfxAnim", "VfxPose", "VfxBloom", "VfxTextures"):
        shutil.copy(os.path.join(VFX, "client", name + ".java"), os.path.join(pkg, "client"))
    with open(os.path.join(VFX, "client", "VfxVertexBuffer.java"), encoding="utf-8") as f:
        vb = patched_vertex_buffer(f.read())
    with open(os.path.join(pkg, "client", "VfxVertexBuffer.java"), "w", encoding="utf-8") as f:
        f.write(vb)
    with open(os.path.join(VFX, "client", "VfxBlend.java"), encoding="utf-8") as f:
        bl = patched_blend(f.read())
    with open(os.path.join(pkg, "client", "VfxBlend.java"), "w", encoding="utf-8") as f:
        f.write(bl)
    for layer in sorted(set(layers) | {"ElementFx"} | ({"ArcaneSpellLayer"} if {"ArcaneSpellLayer2", "DreamPaintLayer"} & set(layers) else set())):     # shared helpers used by the element layers
        shutil.copy(os.path.join(VFX, "client", "layer", layer.split(".")[-1] + ".java"), os.path.join(pkg, "client", "layer"))
    shutil.copy(os.path.join(HERE, "Preview.java"), gen)
    classes = os.path.join(BUILD, "classes")
    shutil.rmtree(classes, ignore_errors=True)
    files = [os.path.join(dp, fn) for dp, _, fns in os.walk(gen) for fn in fns if fn.endswith(".java")]
    subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", JOML, "-d", classes] + files, check=True)
    return classes


# ------------------------------------------------------------------------------------------------ raster
_tex_cache = {}


def texture(path):
    if path not in _tex_cache:
        full = os.path.join(TEXTURES, path)
        _tex_cache[path] = np.asarray(Image.open(full).convert("RGBA"), dtype=np.float32) / 255.0
    return _tex_cache[path]


def basis(cam, target):
    f = np.array(target, float) - np.array(cam, float)
    f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return f, r, u


def background(scene, cam, target):
    f, r, u = basis(cam, target)
    k = (H / 2) / math.tan(math.radians(FOV / 2))
    top, bottom = np.array(scene.get("sky", [0.30, 0.34, 0.42])), np.array(scene.get("horizon", [0.62, 0.62, 0.60]))
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    dirs = (f[None, None, :] * k + r[None, None, :] * (xx[..., None] - W / 2) - u[None, None, :] * (yy[..., None] - H / 2))
    dirs /= np.linalg.norm(dirs, axis=2, keepdims=True)
    t = np.clip(dirs[..., 1] * 1.5 + 0.5, 0, 1)[..., None]
    img = bottom * (1 - t) + top * t
    gy = scene.get("ground", 0.0) - cam[1]
    hit = dirs[..., 1] < -1e-3
    dist = np.where(hit, gy / np.where(hit, dirs[..., 1], -1), 0)
    gx, gz = cam[0] + dirs[..., 0] * dist, cam[2] + dirs[..., 2] * dist
    checker = ((np.floor(gx) + np.floor(gz)) % 2 == 0).astype(np.float32)
    ground = np.array(scene.get("floor", [0.36, 0.33, 0.28]))
    gcol = ground[None, None, :] * (0.85 + 0.15 * checker[..., None])
    fog = np.clip(dist / 60.0, 0, 1)[..., None]
    gcol = gcol * (1 - fog) + bottom * fog
    img = np.where(hit[..., None], gcol, img)
    return img.astype(np.float32)


def project(p, f, r, u):
    k = (H / 2) / math.tan(math.radians(FOV / 2))
    z = p @ f
    return W / 2 + (p @ r) / z * k, H / 2 - (p @ u) / z * k, z


def draw_figures(img, scene, cam, target):
    """Dark capsules standing in for players / mobs, to judge scale."""
    f, r, u = basis(cam, target)
    pil = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(pil)
    for x, y, z, h in scene.get("figures", []):
        foot = np.array([x, y, z]) - cam
        head = foot + [0, h, 0]
        fx, fy, fz = project(foot, f, r, u)
        hx, hy, hz = project(head, f, r, u)
        if fz < 0.1 or hz < 0.1:
            continue
        k = (H / 2) / math.tan(math.radians(FOV / 2))
        w = 0.6 / fz * k / 2
        d.rounded_rectangle([fx - w, hy, fx + w, fy], radius=w, fill=(40, 38, 46))
    return np.asarray(pil, dtype=np.float32) / 255.0


def raster(img, quads, cam, target):
    f, r, u = basis(cam, target)
    k = (H / 2) / math.tan(math.radians(FOV / 2))
    order = sorted(range(len(quads)), key=lambda i: quads[i]["o"])          # stable: insertion order within a blend
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    for qi in order:
        q = quads[qi]
        tex = texture(q["t"])
        th, tw = tex.shape[:2]
        v = np.array(q["v"], dtype=np.float64)
        pts = v[:, :3]
        uv = v[:, 3:5]
        col = np.array([[((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255] for c in v[:, 5].astype(np.int64)])
        z = pts @ f
        if np.any(z < 0.05):
            continue
        sx = W / 2 + (pts @ r) / z * k
        sy = H / 2 - (pts @ u) / z * k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, min(x0, x1, x2))), int(min(W - 1, max(x0, x1, x2))) + 1
            miny, maxy = int(max(0, min(y0, y1, y2))), int(min(H - 1, max(y0, y1, y2))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = xx[miny:maxy, minx:maxx] + 0.5
            Y = yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            m = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not m.any():
                continue
            iz = np.array([1 / z[i] for i in tri])
            w_ = a * iz[0] + b * iz[1] + c * iz[2]
            def interp(vals):
                return (a * vals[0] * iz[0] + b * vals[1] * iz[1] + c * vals[2] * iz[2]) / w_
            U = interp([uv[i, 0] for i in tri])
            V = interp([uv[i, 1] for i in tri])
            ui = (np.mod(U, 1.0) * tw).astype(int).clip(0, tw - 1)
            vi = (np.mod(V, 1.0) * th).astype(int).clip(0, th - 1)
            vc = np.stack([interp([col[i, ch] for i in tri]) for ch in range(4)], axis=-1)
            s = tex[vi, ui] * vc
            al = (s[..., 3] * m)[..., None]
            region = img[miny:maxy, minx:maxx]
            blend = q["b"]
            if blend in ("ALPHA", "WATER"):
                region[:] = region * (1 - al) + s[..., :3] * al
            elif blend == "NEGATIVE":
                src = s[..., :3] * m[..., None]
                region[:] = src * (1 - region) + region * (1 - src)
            else:
                region += s[..., :3] * al
    return img


def run_scene(name, scene, classes):
    out_json = os.path.join(BUILD, name + ".json")
    args = ["java", "-cp", classes + os.pathsep + JOML, "Preview",
            "--layer", "com.newuniverse.nusmp.vfx.client.layer." + scene["layer"], "--shape", scene["shape"],
            "--from", ",".join(map(str, scene.get("from", [0, 0, 0]))), "--to", ",".join(map(str, scene.get("to", [0, 1, 0]))),
            "--power", str(scene.get("power", 1)), "--duration", str(scene.get("duration", 0)), "--color", scene.get("color", "0"),
            "--seed", str(scene.get("seed", 12345)), "--cam", ",".join(map(str, scene["cam"])),
            "--target", ",".join(map(str, scene["target"])), "--ages", ",".join(map(str, scene["ages"])),
            "--detail", str(scene.get("detail", 1)), "--out", out_json]
    subprocess.run(args, check=True)
    with open(out_json) as f:
        data = json.load(f)
    tiles = []
    for fr in data["frames"]:
        img = background(scene, scene["cam"], scene["target"])
        img = draw_figures(img, scene, scene["cam"], scene["target"])
        img = raster(img, fr["quads"], scene["cam"], scene["target"])
        tile = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        ImageDraw.Draw(tile).text((6, 4), f"{name}  t={fr['age']:.0f}/{data['duration']}  {fr['vertices']} verts", fill=(255, 255, 255))
        tiles.append(tile)
    sheet = Image.new("RGB", (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    path = os.path.join(BUILD, name + ".png")
    sheet.save(path)
    worst = max(fr["vertices"] for fr in data["frames"])
    print(f"{name:24s} max {worst:3d}/400 vertices -> {path}")
    return worst


def main():
    with open(os.path.join(HERE, "scenes.json")) as f:
        scenes = {k: v for k, v in json.load(f).items() if not k.startswith("_")}
    wanted = sys.argv[1:] or list(scenes)
    os.makedirs(BUILD, exist_ok=True)
    classes = build([scenes[n]["layer"] for n in wanted])
    over = [n for n in wanted if run_scene(n, scenes[n], classes) > 400]
    if over:
        print("OVER BUDGET:", over)
        sys.exit(1)


if __name__ == "__main__":
    main()
