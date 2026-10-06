"""Preview the grimoire item look headlessly: compiles the real pure look/geometry classes, renders each book like the inventory
(gui) view and a front view, with the real textures, tints, simple face shading and held glow.

    python tools/grimoire_preview/preview.py      ->  build/grimoire_preview/books.png  (closed books, inventory + held views)
                                                      build/grimoire_preview/open.png   (the summoned book opening into the V,
                                                      seen by onlookers and by its owner, for the six Blender-preset books)
"""
import json, math, os, shutil, subprocess, sys
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book")
OUT = os.path.join(ROOT, "build", "grimoire_preview")

LOOKS = [
    ("Three-Leaf Fire", "THREE_LEAF", "FLAME", "", 0), ("Four-Leaf Wind", "FOUR_LEAF", "WIND", "", 0),
    ("Five-Leaf Dark", "FIVE_LEAF", "DARK", "", 0), ("Spade Steel", "SPADE", "STEEL", "", 0),
    ("Heart Water", "HEART", "WATER", "", 0), ("Diamond Earth", "DIAMOND", "EARTH", "", 0),
    ("Fuegoleon", "", "", "fuegoleon", 0), ("Yuno", "", "", "yuno", 0), ("Asta", "", "", "asta", 0),
    ("Noelle", "", "", "noelle", 0), ("Yami", "", "", "yami", 0), ("Julius", "", "", "julius", 0),
    ("Karna", "", "", "karna", 0), ("Kirsch", "", "", "kirsch", 0), ("Zenon", "", "", "zenon", 0),
    ("Vanica", "", "", "vanica", 0), ("Floga", "", "", "floga", 0), ("Mars", "", "", "mars", 0),
    ("Black Magic", "BLACK_MAGIC", "DARK", "", 0), ("God-Tier", "GOD_TIER", "LIGHT", "", 0),
    ("Triple Spade", "TRIPLE_SPADE", "GRAVITY", "", 0), ("Two-Heart", "TWO_HEART", "WATER", "", 0),
]
LIGHT = np.array([0.35, 0.8, 0.5]) / np.linalg.norm([0.35, 0.8, 0.5])
OPEN_BOOKS = [("Fuegoleon", "fuegoleon"), ("Yuno", "yuno"), ("Asta", "asta"), ("Noelle", "noelle"), ("Yami", "yami"), ("Julius", "julius")]
W = H = 220


def build():
    gen = os.path.join(OUT, "src")
    shutil.rmtree(gen, ignore_errors=True)
    os.makedirs(gen)
    files = [os.path.join(SRC, "grimoire", f + ".java") for f in ("BookMotif", "BookPalette", "CanonBook", "BookLook", "GrimoireBookPlan")]
    files += [os.path.join(SRC, "blackclover", f + ".java") for f in ("GrimoireCover", "Kingdom")]
    files.append(os.path.join(HERE, "GrimoirePreview.java"))
    cls = os.path.join(OUT, "classes")
    shutil.rmtree(cls, ignore_errors=True)
    subprocess.run(["javac", "-nowarn", "-d", cls] + files, check=True)
    return cls


def rot(rx, ry):
    a, b = math.radians(rx), math.radians(ry)
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    return Rx @ Ry


_tex = {}


def tex(name):
    if name not in _tex:
        _tex[name] = np.asarray(Image.open(os.path.join(TEX, name + ".png")).convert("RGBA"), dtype=np.float32) / 255
    return _tex[name]


def render(book, R, held_bg):
    img = np.ones((H, W, 3), np.float32) * np.array(held_bg)
    zbuf = np.full((H, W), -1e9, np.float32)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    tints = book["tints"]
    s = W / 22.0
    for q in book["quads"]:
        p = np.array(q["p"]).reshape(4, 3) - 8.0
        p = p @ R.T
        sx = W / 2 + p[:, 0] * s
        sy = H / 2 - p[:, 1] * s
        zz = p[:, 2]
        uv = np.array(q["uv"]).reshape(4, 2) / 16.0
        t = tex(q["t"])
        th, tw = t.shape[:2]
        col = np.ones(3, np.float32)
        if q["tint"] >= 0:
            c = tints[q["tint"]]
            col = np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], np.float32) / 255
        n = np.array(q.get("n", [0, 0, 1])) @ R.T
        shade = 1.0 if q["e"] else 0.45 + 0.55 * max(0.0, float(n @ LIGHT))
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]; y0, y1, y2 = sy[list(tri)]
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if den >= -1e-9:         # screen y points down, so front faces have den < 0; cull back faces like items do
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
            U = a * uv[tri[0], 0] + b * uv[tri[1], 0] + c * uv[tri[2], 0]
            V = a * uv[tri[0], 1] + b * uv[tri[1], 1] + c * uv[tri[2], 1]
            px = t[(np.clip(V, 0, .9999) * th).astype(int), (np.clip(U, 0, .9999) * tw).astype(int)]
            vis = m & (px[..., 3] > 0.1) & (Z > zbuf[miny:maxy, minx:maxx] - 1e-3)
            rgb = px[..., :3] * col * shade
            reg = img[miny:maxy, minx:maxx]
            al = (px[..., 3] * vis)[..., None]
            reg[:] = reg * (1 - al) + rgb * al
            zb = zbuf[miny:maxy, minx:maxx]
            zb[vis] = Z[vis]
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    cls = build()
    out_json = os.path.join(OUT, "books.json")
    entries = []
    for label, cover, magic, canon, seed in LOOKS:
        entries.append(f"{label}|{cover}|{magic}|{canon}|{seed}|false")
        entries.append(f"{label} (held)|{cover}|{magic}|{canon}|{seed}|true")
    subprocess.run(["java", "-cp", cls, "GrimoirePreview", out_json] + entries, check=True)
    books = json.load(open(out_json))
    cols = 6
    tiles = []
    for i in range(0, len(books), 2):
        gui = render(books[i], rot(20, -35), [0.55, 0.55, 0.6])       # inventory-style view
        front = render(books[i + 1], rot(0, 0), [0.12, 0.12, 0.16])   # front, held (glowing pages/emblem)
        tile = Image.new("RGB", (W * 2, H + 18), (30, 30, 36))
        tile.paste(Image.fromarray((np.clip(gui, 0, 1) * 255).astype(np.uint8)), (0, 18))
        tile.paste(Image.fromarray((np.clip(front, 0, 1) * 255).astype(np.uint8)), (W, 18))
        ImageDraw.Draw(tile).text((6, 3), books[i]["label"], fill=(240, 220, 160))
        tiles.append(tile)
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * W * 2 // 2, rows * (H + 18)), (20, 20, 24))
    sheet = Image.new("RGB", (cols * W * 2, rows * (H + 18)), (20, 20, 24))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * W * 2, (i // cols) * (H + 18)))
    path = os.path.join(OUT, "books.png")
    sheet.save(path)
    print("wrote", path)

    # the summoned book: opening steps 1..3 seen by onlookers (spine and covers towards them), and the full V seen by its owner
    entries = []
    for label, canon in OPEN_BOOKS:
        for step in (1, 2, 3):
            entries.append(f"{label} step {step}|||{canon}|0|true|{step}")
    subprocess.run(["java", "-cp", cls, "GrimoirePreview", out_json] + entries, check=True)
    books = json.load(open(out_json))
    views = [("opening", rot(8, -20), 0), ("opening", rot(8, -20), 1), ("open, onlookers", rot(8, -20), 2), ("open, owner", rot(8, 160), 2)]
    sheet = Image.new("RGB", (len(views) * W, len(OPEN_BOOKS) * (H + 18)), (20, 20, 24))
    for r, (label, _) in enumerate(OPEN_BOOKS):
        for c, (what, R, k) in enumerate(views):
            img = render(books[r * 3 + k], R, [0.12, 0.12, 0.16])
            tile = Image.new("RGB", (W, H + 18), (30, 30, 36))
            tile.paste(Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)), (0, 18))
            ImageDraw.Draw(tile).text((6, 3), f"{label}: {what}" + (f" {k + 1}/3" if c < 2 else ""), fill=(240, 220, 160))
            sheet.paste(tile, (c * W, r * (H + 18)))
    path = os.path.join(OUT, "open.png")
    sheet.save(path)
    print("wrote", path)


if __name__ == "__main__":
    main()
