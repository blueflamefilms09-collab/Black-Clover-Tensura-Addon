#!/usr/bin/env python3
"""Why: the headless render preview of this repo draws GeoLite models, not vanilla "elements" block models, and the Soul Forge is one of those. This is a
small software rasteriser for them so the model can be LOOKED at before it ever reaches the game: it reads a block model JSON (elements with from / to,
single-axis rotations, per-face uv and texture references, the element-level "neoforge_data" light flag), resolves #texture names to the PNGs of this
repo (a vanilla texture falls back to a flat dark colour), and draws it from the same three-quarter angle as the inventory (x 30, y 225) with a z-buffer
and the classic face shading (top 1.0, north and south 0.8, east and west 0.6, bottom 0.5; emissive elements are not shaded).

    python3 -B tools/avatar/preview_block_model.py <model.json> <out.png> [pixels-per-block=300] [yaw=225] [pitch=30]

It is a preview, not the game's renderer: it does not know ambient occlusion, tint indices, cullface or item display transforms.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets")
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
NORMAL = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
FALLBACK = (22, 16, 34)


def load_texture(ref, textures):
    while ref.startswith("#"):
        ref = textures.get(ref[1:], "")
    if ":" in ref:
        ns, path = ref.split(":", 1)
    else:
        ns, path = "minecraft", ref
    f = os.path.join(ASSETS, ns, "textures", path + ".png")
    if os.path.isfile(f):
        return Image.open(f).convert("RGBA")
    return None


def face_corners(frm, to, face):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face == "up":
        return [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)]
    if face == "down":
        return [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)]
    if face == "north":
        return [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)]
    if face == "south":
        return [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)]
    if face == "west":
        return [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)]
    return [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)]          # east


def default_uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face in ("up", "down"):
        return [x0, z0, x1, z1]
    if face in ("north", "south"):
        return [x0, 16 - y1, x1, 16 - y0]
    return [z0, 16 - y1, z1, 16 - y0]


def rotate(p, rot):
    if not rot:
        return p
    o = rot["origin"]
    a = math.radians(rot["angle"])
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    elif rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def view(p, yaw, pitch, scale, cx, cy):
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
    a = math.radians(yaw)
    x, z = x * math.cos(a) - z * math.sin(a), x * math.sin(a) + z * math.cos(a)
    b = math.radians(pitch)
    y, z = y * math.cos(b) - z * math.sin(b), y * math.sin(b) + z * math.cos(b)
    return (cx + x * scale / 16.0, cy - y * scale / 16.0, z)


def render(model, scale=300.0, yaw=225.0, pitch=30.0, size=None):
    w = h = int((size or 1.7) * scale)
    img = Image.new("RGBA", (w, h), (24, 30, 52, 255))
    zbuf = [[-1e9] * w for _ in range(h)]
    textures = dict(model.get("textures", {}))
    cache = {}
    px = img.load()
    for el in model.get("elements", []):
        emissive = (el.get("neoforge_data", {}).get("block_light", 0) or 0) >= 12
        rot = el.get("rotation")
        for face, fd in el.get("faces", {}).items():
            tex = fd["texture"]
            if tex not in cache:
                cache[tex] = load_texture(tex, textures)
            im = cache[tex]
            uv = fd.get("uv") or default_uv(face, el["from"], el["to"])
            corners = [rotate(p, rot) for p in face_corners(el["from"], el["to"], face)]
            n = NORMAL[face]
            # skip back faces: the camera looks along -z of the view frame after the rotation
            vp = [view(p, yaw, pitch, scale, w / 2, h / 2 + scale * 0.06) for p in corners]
            ax, ay = vp[1][0] - vp[0][0], vp[1][1] - vp[0][1]
            bx, by = vp[2][0] - vp[0][0], vp[2][1] - vp[0][1]
            if ax * by - ay * bx > 0:
                continue
            u0, v0, u1, v1 = uv
            uvs = [(u0, v0), (u0, v1), (u1, v1), (u1, v0)]
            shade = 1.0 if emissive else SHADE[face]
            for tri in ((0, 1, 2), (0, 2, 3)):
                (xa, ya, za), (xb, yb, zb), (xc, yc, zc) = vp[tri[0]], vp[tri[1]], vp[tri[2]]
                den = (yb - yc) * (xa - xc) + (xc - xb) * (ya - yc)
                if abs(den) < 1e-9:
                    continue
                for y in range(max(0, int(min(ya, yb, yc))), min(h, int(max(ya, yb, yc)) + 1)):
                    for x in range(max(0, int(min(xa, xb, xc))), min(w, int(max(xa, xb, xc)) + 1)):
                        l1 = ((yb - yc) * (x + 0.5 - xc) + (xc - xb) * (y + 0.5 - yc)) / den
                        l2 = ((yc - ya) * (x + 0.5 - xc) + (xa - xc) * (y + 0.5 - yc)) / den
                        l3 = 1 - l1 - l2
                        if l1 < -0.001 or l2 < -0.001 or l3 < -0.001:
                            continue
                        z = l1 * za + l2 * zb + l3 * zc
                        if z <= zbuf[y][x]:
                            continue
                        u = l1 * uvs[tri[0]][0] + l2 * uvs[tri[1]][0] + l3 * uvs[tri[2]][0]
                        v = l1 * uvs[tri[0]][1] + l2 * uvs[tri[1]][1] + l3 * uvs[tri[2]][1]
                        if im is None:
                            c = FALLBACK + (255,)
                        else:
                            c = im.getpixel((min(im.size[0] - 1, max(0, int(u * im.size[0] / 16.0))), min(im.size[1] - 1, max(0, int(v * im.size[1] / 16.0)))))
                        if c[3] < 10:
                            continue
                        zbuf[y][x] = z
                        px[x, y] = (min(255, int(c[0] * shade)), min(255, int(c[1] * shade)), min(255, int(c[2] * shade)), 255)
    return img


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    model = json.load(open(sys.argv[1]))
    scale = float(sys.argv[3]) if len(sys.argv) > 3 else 300.0
    yaw = float(sys.argv[4]) if len(sys.argv) > 4 else 225.0
    pitch = float(sys.argv[5]) if len(sys.argv) > 5 else 30.0
    render(model, scale, yaw, pitch).save(sys.argv[2])
    print("wrote", sys.argv[2])
    return 0


if __name__ == "__main__":
    sys.exit(main())
