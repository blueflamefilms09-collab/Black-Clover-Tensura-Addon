"""Turntable sheet of the Wind remake's real 3D pieces (sword, bow, arrow, spear, orbiting blade, feather, crown, plate, cube, panel): each mesh from four angles, solid (the baked face shade and the
atlas) and with the glow shell added. A small painter's-algorithm software render of the mesh files (no game, no bloom).

    python3 -B tools/preview_sdw3_models.py [out.png]      -> docs/previews/spirit_dive_wind3/weapons_turntable.png
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
MESH = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdw3_meshes")
SKIN = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "aura", "sdw3_skin.png")
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind3", "weapons_turntable.png")
L = (0.30, 0.35, 0.89)
ORDER = ["sword", "bow", "arrow", "spear", "blade", "feather", "crown", "plate", "cube", "panel"]
SS = 3


def load(name):
    quads = []
    for line in open(os.path.join(MESH, name + ".txt")):
        if not line.startswith("q "):
            continue
        v = line.split()
        layer = int(v[1])
        f = [float(x) for x in v[2:]]
        pts = [tuple(f[k * 5:k * 5 + 3]) for k in range(4)]
        uvs = [tuple(f[k * 5 + 3:k * 5 + 5]) for k in range(4)]
        n = tuple(f[20:23])
        rgb = tuple(int(float(x)) for x in f[23:26])
        quads.append((layer, pts, uvs, n, rgb))
    return quads


def render(quads, yaw, size, scale, cy, glow):
    """Orthographic view from yaw about the y axis (pitch 12 degrees down); returns an RGB image."""
    w, h = size
    img = Image.new("RGB", (w * SS, h * SS), (22, 24, 34))
    gl = Image.new("RGB", (w * SS, h * SS), (0, 0, 0))
    cyaw, syaw = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(12)), math.sin(math.radians(12))
    skin = np.asarray(Image.open(SKIN).convert("RGBA"), np.float32) / 255.0

    def proj(p):
        x = p[0] * cyaw + p[2] * syaw
        z = -p[0] * syaw + p[2] * cyaw
        y = p[1] * cp - z * sp
        zz = p[1] * sp + z * cp
        return (w * SS / 2 + x * scale * SS, h * SS / 2 - (y - cy) * scale * SS, zz)

    draw = ImageDraw.Draw(img)
    gd = ImageDraw.Draw(gl)
    items = []
    for layer, pts, uvs, n, rgb in quads:
        pp = [proj(p) for p in pts]
        items.append((sum(q[2] for q in pp) / 4, layer, pp, uvs, n, rgb))
    items.sort(key=lambda t: t[0])
    for _, layer, pp, uvs, n, rgb in items:
        xy = [(q[0], q[1]) for q in pp]
        if layer == 1:
            if glow:
                gd.polygon(xy, fill=(int(rgb[0] * 0.30), int(rgb[1] * 0.30), int(rgb[2] * 0.30)))
            continue
        d = n[0] * L[0] + n[1] * L[1] + n[2] * L[2]
        shade = 0.84 + 0.16 * max(0.0, d) + 0.06 * max(0.0, -d)
        u = sum(q[0] for q in uvs) / 4
        v = sum(q[1] for q in uvs) / 4
        t = skin[min(63, int(v * 64)), min(63, int(u * 64))]
        col = tuple(min(255, int(rgb[i] * shade * t[i])) for i in range(3))
        draw.polygon(xy, fill=col)
    img = img.resize((w, h), Image.LANCZOS)
    gl = gl.resize((w, h), Image.LANCZOS)
    return ImageChops.add(img, gl) if glow else img


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else OUT
    os.makedirs(os.path.dirname(base), exist_ok=True)
    font = ImageFont.load_default()
    outs = []
    for part, names in (("a", ORDER[:5]), ("b", ORDER[5:])):
        sheet = Image.new("RGB", (8 * 170, len(names) * 250), (12, 13, 20))
        d = ImageDraw.Draw(sheet)
        for r, name in enumerate(names):
            q = load(name)
            ys = [p[1] for _, pts, *_ in q for p in pts]
            xs = [max(abs(p[0]), abs(p[2])) for _, pts, *_ in q for p in pts]
            hgt, wid = max(ys) - min(ys), 2 * max(xs)
            scale = min(220.0 / max(hgt, 1e-3), 160.0 / max(wid, 1e-3), 14.0)
            cy = (max(ys) + min(ys)) / 2
            for g, glow in enumerate((False, True)):
                for c, yaw in enumerate((0, 90, 180, 270)):
                    sheet.paste(render(q, yaw, (170, 250), scale, cy, glow), ((g * 4 + c) * 170, r * 250))
            d.text((6, r * 250 + 4), name + " (solid, 4 angles)  |  + glow shell (4 angles)", fill=(230, 240, 235), font=font)
        out = base.replace(".png", "_%s.png" % part)
        sheet.save(out)
        outs.append(out)
        print(out, sheet.size)


if __name__ == "__main__":
    main()
