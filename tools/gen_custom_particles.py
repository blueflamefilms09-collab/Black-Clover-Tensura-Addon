"""Generates the custom particle library of the mod (docs/custom_particles.md).

For every particle of tools/particles/lib_*.py it writes
  * src/main/resources/assets/nusmp/textures/particle/<id>_<n>.png   (the animation frames, 16x16 or 32x32)
  * src/main/resources/assets/nusmp/particles/<id>.json              (the sprite list the particle engine reads)
and, once for all, the catalog the game reads for the physics profiles:
  * src/main/resources/data/nusmp/particle_catalog.json
plus the preview sheet docs/previews/custom_particles/sheet.png (every particle's frames on a dark and on a light background).

Deterministic (seeded per id and frame), numpy + Pillow only.

    python3 -B tools/gen_custom_particles.py               # everything
    python3 -B tools/gen_custom_particles.py --only ember  # one particle (also rewrites the sheet)
    python3 -B tools/gen_custom_particles.py --sheet-only  # only the preview sheet, from the textures on disk
    python3 -B tools/gen_custom_particles.py --out /tmp/x  # write textures/jsons/sheet under another root (scratch preview)
"""
import argparse
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "particles"))

import registry  # noqa: E402
import lib_fire, lib_glow, lib_pulse, lib_nature, lib_misc  # noqa: E402,F401
from artkit import Canvas, to_uint8  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
RES = os.path.join(ROOT, "src", "main", "resources")


def render(spec, frame):
    n = spec["px"]
    c = Canvas(n)
    img = spec["fn"](c, frame, spec["frames"], registry.rng_for(spec["id"], frame))
    assert img.shape == (n, n, 4), (spec["id"], img.shape)
    return to_uint8(img)


def write_all(root_res, only=None):
    tex = os.path.join(root_res, "assets", "nusmp", "textures", "particle")
    pjs = os.path.join(root_res, "assets", "nusmp", "particles")
    dat = os.path.join(root_res, "data", "nusmp")
    for d in (tex, pjs, dat):
        os.makedirs(d, exist_ok=True)
    catalog = []
    for pid in registry.ORDER:
        spec = registry.SPECS[pid]
        if only is None or pid == only:
            for f in range(spec["frames"]):
                Image.fromarray(render(spec, f), "RGBA").save(os.path.join(tex, "%s_%d.png" % (pid, f)))
            with open(os.path.join(pjs, pid + ".json"), "w", encoding="utf-8", newline="\n") as fh:
                json.dump({"textures": ["nusmp:%s_%d" % (pid, f) for f in range(spec["frames"])]}, fh, indent=2)
                fh.write("\n")
        catalog.append({"id": pid, "family": spec["family"], "group": spec["group"], "frames": spec["frames"], "px": spec["px"], "tint": spec["tint"],
                        "behaviour": spec["behaviour"], "profile": spec["profile"]})
    with open(os.path.join(dat, "particle_catalog.json"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"version": 1, "particles": catalog}, fh, indent=1)
        fh.write("\n")
    return tex


def sheet(tex_dir, out_png):
    scale = 4
    pad = 6
    label_w = 150
    rows = []
    for pid in registry.ORDER:
        spec = registry.SPECS[pid]
        frames = [Image.open(os.path.join(tex_dir, "%s_%d.png" % (pid, f))).convert("RGBA") for f in range(spec["frames"])]
        rows.append((pid, spec, frames))
    maxw = max(sum(f.width * scale + pad for f in fr) for _, _, fr in rows)
    cell_h = [max(f.height for f in fr) * scale + pad for _, _, fr in rows]
    W = label_w + maxw + pad
    H = sum(cell_h) + pad
    out = Image.new("RGB", (W * 2, H), (0, 0, 0))
    dark = Image.new("RGB", (W, H), (20, 18, 28))
    light = Image.new("RGB", (W, H), (216, 212, 204))
    y = pad
    for (pid, spec, frames), ch in zip(rows, cell_h):
        for bg, ox in ((dark, 0), (light, W)):
            d = ImageDraw.Draw(bg)
            d.text((pad, y + 4), "%s (%s)" % (pid, spec["family"]), fill=(235, 235, 235) if bg is dark else (20, 20, 20))
        x = label_w
        for fr in frames:
            big = fr.resize((fr.width * scale, fr.height * scale), Image.NEAREST)
            dark.paste(big, (x, y), big)
            light.paste(big, (x, y), big)
            x += big.width + pad
        y += ch
    out.paste(dark, (0, 0))
    out.paste(light, (W, 0))
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    out.save(out_png)
    return out_png


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only")
    ap.add_argument("--sheet-only", action="store_true")
    ap.add_argument("--out", help="root directory to write under instead of the repository (scratch preview)")
    a = ap.parse_args()
    res = os.path.join(a.out, "src", "main", "resources") if a.out else RES
    docs = os.path.join(a.out, "docs") if a.out else os.path.join(ROOT, "docs")
    if a.sheet_only:
        tex = os.path.join(res, "assets", "nusmp", "textures", "particle")
    else:
        tex = write_all(res, a.only)
    p = sheet(tex, os.path.join(docs, "previews", "custom_particles", "sheet.png"))
    print("particles:", len(registry.ORDER), "textures:", sum(s["frames"] for s in registry.SPECS.values()), "sheet:", p)


if __name__ == "__main__":
    main()
