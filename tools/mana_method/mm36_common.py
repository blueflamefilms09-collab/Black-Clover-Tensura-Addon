"""
Shared helpers of the 3.6.0 Mana Method previews (preview_lightning.py, preview_body.py, preview_brush.py): the headless VFX preview of tools/vfx_preview (the game's own layers compiled against the
Minecraft stubs, rasterised with the real textures), the numbers of the real pure classes (DumpMm36.java: LightningCharge, RuneCost, BrushRules, BodyRuneRules) and the sheet / label drawing.

Needs JDK 21, Python 3 with numpy + Pillow, JOML (tools/vfx_preview/lib/joml-1.10.5.jar, or $JOML). The Time Magic effect is not touched.
"""
import importlib.util
import json
import os
import subprocess

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
MM_BUILD = os.environ.get("MM_PREVIEW_BUILD") or os.path.join(ROOT, "build", "mana_method_preview")
JOML = os.environ.get("JOML") or os.path.join(ROOT, "tools", "vfx_preview", "lib", "joml-1.10.5.jar")

_spec = importlib.util.spec_from_file_location("vfxpreview", os.path.join(ROOT, "tools", "vfx_preview", "preview.py"))
pv = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(pv)
pv.W, pv.H = 576, 347
pv.FOV = 94.0
if os.environ.get("JOML"):
    pv.JOML = os.environ["JOML"]

DUMP_SOURCES = [os.path.join(JAVA, "mana", "method", f) for f in (
    "RuneElement.java", "RuneMods.java", "RuneSpec.java", "RuneRules.java", "RuneGeneratorUtil.java", "RuneCharge.java", "RuneCost.java", "LightningCharge.java", "BrushRules.java",
    "BodyRuneRules.java")] + [os.path.join(JAVA, "perf", "BoundedCache.java"), os.path.join(JAVA, "perf", "PerfMath.java"), os.path.join(HERE, "DumpMm36.java")]


def numbers():
    """The decisions of the real pure classes for a tap (4 ticks), a half hold (24) and a full hold (40): {"stages": [...], "ownRadius": r}."""
    os.makedirs(MM_BUILD, exist_ok=True)
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    subprocess.run(["javac", "-nowarn", "-encoding", "UTF-8", "-cp", pv.JOML, "-d", MM_BUILD] + DUMP_SOURCES, check=True, env=env, capture_output=True)
    res = subprocess.run(["java", "-cp", MM_BUILD + os.pathsep + pv.JOML, "DumpMm36"], check=True, env=env, capture_output=True, text=True)
    return json.loads(res.stdout.strip().splitlines()[-1])


def frames(classes, name, sc):
    """The frames of one scene as (PIL image, frame dict)."""
    data = pv.run_java(sc, classes, os.path.join(pv.BUILD, name + ".json"))
    out = []
    for fr in data["frames"]:
        img = pv.background(sc, sc["cam"], sc["target"])
        img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
        img = pv.raster(img, fr["quads"], sc["cam"], sc["target"])
        out.append((Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)), fr))
    return out


def merged(classes, name, scenes):
    """Several scenes of the same camera drawn into the same frames (each is one effect with its own budget, as in the game): returns (PIL image, summed vertices) per frame."""
    datas = [pv.run_java(sc, classes, os.path.join(pv.BUILD, "%s_%d.json" % (name, k))) for k, sc in enumerate(scenes)]
    sc0 = scenes[0]
    out = []
    for idx in range(len(datas[0]["frames"])):
        quads, verts = [], 0
        for d in datas:
            quads += d["frames"][idx]["quads"]
            verts += d["frames"][idx]["vertices"]
        img = pv.background(sc0, sc0["cam"], sc0["target"])
        img = pv.draw_figures(img, sc0, sc0["cam"], sc0["target"])
        img = pv.raster(img, quads, sc0["cam"], sc0["target"])
        out.append((Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)), verts, datas[0]["frames"][idx]["age"]))
    return out


def label(im, text, right=None):
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, im.width, 15], fill=(0, 0, 0))
    d.text((4, 2), text, fill=(235, 235, 255))
    if right:
        d.text((im.width - 100, 2), right, fill=(255, 230, 150))
    return im


def half(im):
    return im.resize((pv.W // 2, pv.H // 2), Image.LANCZOS)


def sheet(images, path, title, cols, pad=4):
    w, h = max(i.size[0] for i in images), max(i.size[1] for i in images)
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (cols * (w + pad) + pad, rows * (h + pad) + pad + 20), (6, 6, 12))
    ImageDraw.Draw(out).text((8, 4), title, fill=(255, 235, 170))
    for i, im in enumerate(images):
        out.paste(im, (pad + (i % cols) * (w + pad), 20 + pad + (i // cols) * (h + pad)))
    os.makedirs(OUT, exist_ok=True)
    out.save(os.path.join(OUT, path))
