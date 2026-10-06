"""Third-person check of the summoned grimoire: replays GrimoireFloatClient's world transform (pose -> hand transform -> model)
on the real open-book geometry and draws it next to a block-figure player, from the third-person back camera, the front camera
and from the side.

    python tools/grimoire_preview/third_person.py [right forward up yaw pitch roll scale]
        -> build/grimoire_preview/third_person.png
"""
import json, math, os, subprocess, sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import preview  # noqa: E402

W, H = 360, 300


def rx(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def book_world(book, pose):
    """Model quads (0..16 units) to world (player feet at the origin, facing +z, right is -x), as the game does."""
    right, up, fwd, yaw, pitch, roll, scale = pose
    M = ry(yaw) @ rx(pitch) @ rz(roll) * scale
    at = np.array([-right, up, fwd])
    out = []
    for q in book["quads"]:
        p = np.array(q["p"]).reshape(4, 3) / 16.0
        p = (p - 0.5) * 0.7 + np.array([0, 3 / 16, 1.5 / 16])      # handTransform
        p = p @ M.T + at
        n = np.array(q["n"]) @ (ry(yaw) @ rx(pitch) @ rz(roll)).T
        out.append((p, q, n))
    return out


def player_boxes():
    """Steve-sized boxes: (min, max, colour)."""
    return [((-0.25, 0.75, -0.125), (0.25, 1.5, 0.125), (0.25, 0.55, 0.65)),    # body
            ((-0.25, 1.5, -0.25), (0.25, 2.0, 0.25), (0.75, 0.6, 0.45)),        # head
            ((-0.25, 0.0, -0.125), (0.0, 0.75, 0.125), (0.2, 0.25, 0.55)),      # legs
            ((0.0, 0.0, -0.125), (0.25, 0.75, 0.125), (0.2, 0.25, 0.5)),
            ((-0.5, 0.75, -0.125), (-0.25, 1.5, 0.125), (0.75, 0.6, 0.45)),     # right arm (-x)
            ((0.25, 0.75, -0.125), (0.5, 1.5, 0.125), (0.75, 0.6, 0.45))]


def box_quads(mn, mx, col):
    x0, y0, z0 = mn; x1, y1, z1 = mx
    v = lambda a, b, c: np.array([a, b, c], float)
    faces = [([v(x0, y0, z0), v(x1, y0, z0), v(x1, y1, z0), v(x0, y1, z0)], (0, 0, -1)),
             ([v(x0, y0, z1), v(x0, y1, z1), v(x1, y1, z1), v(x1, y0, z1)], (0, 0, 1)),
             ([v(x0, y0, z0), v(x0, y1, z0), v(x0, y1, z1), v(x0, y0, z1)], (-1, 0, 0)),
             ([v(x1, y0, z0), v(x1, y0, z1), v(x1, y1, z1), v(x1, y1, z0)], (1, 0, 0)),
             ([v(x0, y1, z0), v(x1, y1, z0), v(x1, y1, z1), v(x0, y1, z1)], (0, 1, 0)),
             ([v(x0, y0, z0), v(x0, y0, z1), v(x1, y0, z1), v(x1, y0, z0)], (0, -1, 0))]
    return [(np.array(p), col, np.array(n, float)) for p, n in faces]


def render(book, pose, eye, target):
    img = np.ones((H, W, 3), np.float32) * np.array([0.5, 0.62, 0.8])
    zbuf = np.full((H, W), 1e9, np.float32)
    f = np.array(target, float) - eye; f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    foc = W / (2 * math.tan(math.radians(35)))
    light = np.array([0.3, 0.9, 0.4]); light /= np.linalg.norm(light)

    def proj(p):
        d = p - eye
        z = d @ f
        return W / 2 + (d @ r) * foc / z, H / 2 - (d @ u) * foc / z, z

    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    tris = []
    for p, col, n in [b for bx in player_boxes() for b in box_quads(*bx)]:
        tris.append((p, None, col, n))
    for p, q, n in book_world(book, pose):
        tris.append((p, q, None, n))
    for p, q, col, n in tris:
        sx, sy, zz = proj(p)
        if (zz < 0.05).any():
            continue
        if q is not None:
            uv = np.array(q["uv"]).reshape(4, 2) / 16.0
            t = preview.tex(q["t"])
            th, tw = t.shape[:2]
            tint = np.ones(3, np.float32)
            if q["tint"] >= 0:
                c = book["tints"][q["tint"]]
                tint = np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], np.float32) / 255
            shade = 1.0 if q["e"] else 0.45 + 0.55 * max(0.0, float(n @ light))
        else:
            shade = 0.5 + 0.5 * max(0.0, float(n @ light))
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]; y0, y1, y2 = sy[list(tri)]
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            if q is not None and den >= 0:      # items cull back faces (the open book carries its own back faces)
                continue
            minx, maxx = int(max(0, min(x0, x1, x2))), int(min(W - 1, max(x0, x1, x2))) + 1
            miny, maxy = int(max(0, min(y0, y1, y2))), int(min(H - 1, max(y0, y1, y2))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            X = xx[miny:maxy, minx:maxx] + .5; Y = yy[miny:maxy, minx:maxx] + .5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            m = (a >= 0) & (b >= 0) & (c >= 0)
            Z = a * zz[tri[0]] + b * zz[tri[1]] + c * zz[tri[2]]
            if q is not None:
                U = a * uv[tri[0], 0] + b * uv[tri[1], 0] + c * uv[tri[2], 0]
                V = a * uv[tri[0], 1] + b * uv[tri[1], 1] + c * uv[tri[2], 1]
                px = t[(np.clip(V, 0, .9999) * th).astype(int), (np.clip(U, 0, .9999) * tw).astype(int)]
                al = px[..., 3] > 0.1
                rgb = px[..., :3] * tint * shade
            else:
                al = np.ones_like(m)
                rgb = np.broadcast_to(np.array(col, np.float32) * shade, m.shape + (3,))
            vis = m & al & (Z < zbuf[miny:maxy, minx:maxx])
            reg = img[miny:maxy, minx:maxx]
            reg[vis] = rgb[vis]
            zb = zbuf[miny:maxy, minx:maxx]
            zb[vis] = Z[vis]
    return img


def main():
    os.makedirs(preview.OUT, exist_ok=True)
    pose = [float(v) for v in sys.argv[1:8]] if len(sys.argv) >= 8 else [0.78, 0.5, 0.82, 28, 55, -10, 1.7]
    pose = [pose[0], pose[2], pose[1]] + pose[3:]          # args: right forward up -> (right, up, forward)
    cls = preview.build()
    out_json = os.path.join(preview.OUT, "tp.json")
    subprocess.run(["java", "-cp", cls, "GrimoirePreview", out_json, "Fuegoleon|||fuegoleon|0|true|3"], check=True)
    book = json.load(open(out_json))[0]
    eye = np.array([0, 1.62, 0])
    views = [("third person (back)", eye + np.array([0, 1.4, -3.8]), eye + np.array([0, 0, 1.0])),
             ("third person (front)", eye + np.array([0, 1.4, 3.8]), eye + np.array([0, 0, -1.0])),
             ("side (right)", np.array([-4.0, 1.4, 0.4]), np.array([0, 1.0, 0.4])),
             ("onlooker 3/4", np.array([-2.6, 1.8, 3.2]), np.array([-0.3, 1.0, 0.3]))]
    sheet = Image.new("RGB", (W * 2, (H + 16) * 2), (20, 20, 24))
    for i, (label, at, look) in enumerate(views):
        img = render(book, pose, at, look)
        tile = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        ImageDraw.Draw(tile).text((6, 4), label, fill=(20, 20, 20))
        sheet.paste(tile, ((i % 2) * W, (i // 2) * (H + 16)))
    path = os.path.join(preview.OUT, "third_person.png")
    sheet.save(path)
    print("wrote", path)


if __name__ == "__main__":
    main()
