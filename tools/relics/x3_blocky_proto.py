"""PROTOTYPE of the 'x3 blocky' relic style (Saint Valkyrie Dress look): stacked 1-px-grid plates, gold bevelled frames, pale panels, cyan emissive inlay, diamond gems.
Writes worn-model .txt files (same format as tools/relics/gen_worn_lib.write_model) into OUT and renders before / after sheets with tools/relics/preview_worn.py."""
import os
import sys
import math

import numpy as np
from PIL import Image, ImageDraw

ROOT = "/home/user/Black-Clover-Tensura-Addon"
sys.path.insert(0, ROOT + "/tools/relics")
import preview_worn as P  # noqa: E402
import gen_worn_lib as L  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/proto"
os.makedirs(OUT, exist_ok=True)
MAIN, TRIM, GLOW, DARK, ACC = L.MAIN, L.TRIM, L.GLOW, L.DARK, L.ACCENT
EMIT = 1
GOLD = "gold_fixed"


class M:
    def __init__(self, name, anchor):
        self.name, self.anchor, self.q = name, anchor, []

    # a box, outward faces, UV from the tile cell (tileable tiles repeat by density)
    def box(self, x0, y0, z0, x1, y1, z1, slot=MAIN, tile="plate", flags=0, skip=(), dens=1.0):
        x0, x1 = sorted((x0, x1)); y0, y1 = sorted((y0, y1)); z0, z1 = sorted((z0, z1))
        faces = [((x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (0, 0, -1)), ((x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1), (0, 0, 1)),
                 ((x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (-1, 0, 0)), ((x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0), (1, 0, 0)),
                 ((x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0), (0, -1, 0)), ((x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (0, 1, 0))]
        ti = L.TILE[tile]
        cx0, cy0 = (ti % L.COLS) * L.CELL, (ti // L.COLS) * L.CELL
        for fi, f in enumerate(faces):
            if fi in skip:
                continue
            pts = f[:4]
            n = f[4]
            ax = int(np.argmax(np.abs(n)))
            ab = [((p[2], p[1]) if ax == 0 else ((p[0], p[2]) if ax == 1 else (p[0], p[1]))) for p in pts]
            lo = np.min(ab, axis=0); hi = np.max(ab, axis=0)
            c = (lo + hi) / 2
            span = np.minimum((hi - lo) * dens, L.PERIOD - 1)
            fr = (c * dens) % L.PERIOD
            half = span / 2
            fr = np.clip(fr, half + 0.5, L.PERIOD - half - 0.5)
            rec = []
            for k, p in enumerate(pts):
                a = np.array(ab[k])
                off = (a - c) * dens
                off = np.clip(off, -half, half)
                u, v = fr + off
                rec.append((p[0], p[1], p[2], (cx0 + L.GUT + u) / L.ATLAS_W, (cy0 + L.GUT + v) / L.ATLAS_H, n[0], n[1], n[2]))
            self.q.append((0, slot, flags, rec))

    def plate(self, x0, y0, x1, y1, z, d=1.0, inlay=True, face=-1, frame=0.7):
        """A Saint plate on the front (face=-1) or back (+1): gold frame, pale panel, cyan inlay line."""
        s = face
        zf0, zf1 = (z, z + s * d)
        self.box(x0, y0, min(zf0, zf1), x1, y1, max(zf0, zf1), TRIM, GOLD, dens=1.0)
        f = frame
        zp = z + s * (d + 0.35)
        self.box(x0 + f, y0 + f, min(z + s * d, zp), x1 - f, y1 - f, max(z + s * d, zp), MAIN, "plate", dens=1.2)
        if inlay and (x1 - x0) > 3.2 and (y1 - y0) > 3.2:
            zi = zp + s * 0.2
            g = f + 0.6
            self.box(x0 + g, y0 + g, min(zp, zi), x1 - g, y0 + g + 0.5, max(zp, zi), GLOW, "white", EMIT)
            self.box(x0 + g, y1 - g - 0.5, min(zp, zi), x1 - g, y1 - g, max(zp, zi), GLOW, "white", EMIT)

    def diamond(self, cx, cy, z, r=3, face=-1):
        """A blocky diamond gem in a gold diamond frame: rows of stacked boxes."""
        s = face
        for fr, slot, tile, fl, zo in ((r + 1, TRIM, GOLD, 0, 0.0), (r, GLOW, "gem", EMIT, 0.45)):
            for row in range(-fr, fr + 1):
                w = fr - abs(row)
                z0 = z + s * zo
                z1 = z + s * (zo + 0.9)
                self.box(cx - w - 0.5, cy + row - 0.5, min(z0, z1), cx + w + 0.5, cy + row + 0.5, max(z0, z1), slot, tile, fl)

    def write(self):
        lines = [f"# x3 blocky prototype {self.name}: {len(self.q)} quads", f"model {self.name}", f"anchor {self.anchor}"]
        for (g, slot, flags, rec) in self.q:
            lines.append("q %d %d %d %s" % (g, slot, flags, " ".join("%.3f %.3f %.3f %.4f %.4f %.3f %.3f %.3f" % r for r in rec)))
        path = os.path.join(OUT, self.name + ".txt")
        open(path, "w").write("\n".join(lines) + "\n")
        return path, len(self.q)


def ring(m, y0, y1, ho, hi, slot, tile, flags=0, dens=1.0, hz=None):
    """A rectangular ring (4 boxes) around x +-ho, z +-hz, wall thickness ho-hi."""
    hz = hz or ho
    t = ho - hi
    m.box(-ho, y0, -hz, ho, y1, -hz + t, slot, tile, flags, dens=dens)
    m.box(-ho, y0, hz - t, ho, y1, hz, slot, tile, flags, dens=dens)
    m.box(-ho, y0, -hz + t, -ho + t, y1, hz - t, slot, tile, flags, dens=dens)
    m.box(ho - t, y0, -hz + t, ho, y1, hz - t, slot, tile, flags, dens=dens)


# ----------------------------------------------------------------------------- crown (head)
def crown():
    m = M("x3_crown", "head")
    ring(m, -9.4, -7.8, 4.7, 4.1, TRIM, GOLD)                        # the gold band (head box is -4..4 x -8..0)
    ring(m, -9.4, -9.0, 4.7, 4.1, MAIN, "plate")
    # front plate with the diamond
    m.plate(-2.2, -11.4, 2.2, -7.8, -4.7, 0.9)
    m.diamond(0, -10.0, -5.7, 2)
    # tall stepped blades (stairs of plates), centre tallest, then 2 each side
    for i, (x, h) in enumerate(((0, 8), (-3.0, 6), (3.0, 6), (-4.9, 4), (4.9, 4))):
        w = 1.1 if i == 0 else 0.9
        top = -9.4 - h
        m.box(x - w - 0.5, top, -4.2, x + w + 0.5, -9.4, -3.3, TRIM, GOLD)
        m.box(x - w, top + 0.5, -4.6, x + w, -9.4, -3.9, MAIN, "plate", dens=1.2)
        m.box(x - 0.3, top + 1.0, -4.8, x + 0.3, -9.9 + 0.6, -4.5, GLOW, "white", EMIT)
        m.box(x - w * 0.5, top - 0.9, -4.0, x + w * 0.5, top, -3.5, GLOW, "gem", EMIT)
    # back blades
    for x in (-2.4, 0, 2.4):
        m.box(x - 0.8, -14.0, 3.3, x + 0.8, -9.4, 4.2, TRIM, GOLD)
        m.box(x - 0.45, -13.4, 4.2, x + 0.45, -9.8, 4.5, MAIN, "plate")
    # halo: a ring of gold cubes with cyan flame cubes on top, held by two arms
    R = 7.0
    for k in range(16):
        a = k * math.tau / 16
        x, z = R * math.cos(a), R * math.sin(a)
        m.box(x - 0.9, -19.2, z - 0.9, x + 0.9, -17.8, z + 0.9, TRIM, GOLD)
        if k % 2 == 0:
            m.box(x - 0.5, -21.6, z - 0.5, x + 0.5, -19.2, z + 0.5, GLOW, "flame", EMIT)
    for sx in (-1, 1):
        for st in range(6):
            m.box(sx * (4.3 + st * 0.45) - 0.5, -9.4 - st * 1.5 - 1.2, 3.2 - st * 0.1 - 0.5, sx * (4.3 + st * 0.45) + 0.5, -9.4 - st * 1.5, 3.2 + 0.5, TRIM, GOLD)
    return m


# ----------------------------------------------------------------------------- cuirass (body)
def cuirass():
    m = M("x3_cuirass", "body")
    # the front: two chest plates, a gorget, the diamond, abs plates, belt, tassets
    m.plate(-4.6, 0.2, -0.2, 5.6, -2.0, 1.0, face=-1)
    m.plate(0.2, 0.2, 4.6, 5.6, -2.0, 1.0, face=-1)
    m.plate(-3.8, 5.8, 3.8, 8.2, -2.0, 0.9)
    m.box(-4.8, 8.2, -2.9, 4.8, 9.6, 2.9, TRIM, GOLD)                      # belt
    m.diamond(0, 3.2, -3.0, 3)
    for i, x in enumerate((-3.8, -1.3, 1.3)):
        m.plate(x, 9.6, x + 2.5, 14.4, -2.2, 0.9, face=-1)                   # tassets
    # collar / gorget
    ring(m, -1.2, 0.8, 4.5, 3.7, TRIM, GOLD, hz=2.8)
    m.box(-3.6, -2.0, -2.6, 3.6, -1.2, -2.0, MAIN, "plate")
    # the back plate and spine
    m.plate(-3.8, 0.2, 3.8, 8.0, 2.0, 0.9, face=1)
    m.box(-0.5, 0.4, 3.3, 0.5, 9.0, 3.8, GLOW, "white", EMIT)
    # side plates
    for sx in (-1, 1):
        m.box(sx * 4.0 - (0.3 if sx < 0 else -0.3), 0.8, -1.6, sx * 4.9 - (0.3 if sx < 0 else -0.3), 8.0, 1.6, TRIM, GOLD)
        m.box(sx * 4.6, 1.4, -1.2, sx * 5.2, 7.4, 1.2, MAIN, "plate")
        m.box(sx * 5.2 - 0.2, 3.0, -0.35, sx * 5.2 + 0.2 * sx, 6.0, 0.35, GLOW, "white", EMIT)
    return m


def pauldron():
    m = M("x3_pauldron", "rarm")                                              # right arm local: x -3..1, y -2..10 (pivot at the shoulder)
    for i, (w, h) in enumerate(((4.6, 1.5), (4.0, 1.4), (3.4, 1.3))):         # stacked shoulder plates, each smaller (lames)
        y0 = -2.6 - i * 1.4
        m.box(-1.0 - w + 1.0 + 0.2, y0 - h * 0.0, -3.0 + i * 0.3, 1.8 + 0.4, y0 + 1.5, 3.0 - i * 0.3, TRIM, GOLD)
        m.box(-1.0 - w + 1.6, y0 - 0.25, -2.5 + i * 0.3, 1.2 + 0.4, y0 + 1.5 - 0.0, 2.5 - i * 0.3, MAIN, "plate", dens=1.2)
        m.box(-1.0 - w + 2.0, y0 - 0.35, -0.3, 0.8 + 0.4, y0 - 0.05, 0.3, GLOW, "white", EMIT)
    m.box(-3.2, -7.4, -1.0, -2.0, -2.8, 1.0, TRIM, GOLD)                    # a spike
    m.box(-2.9, -8.4, -0.5, -2.3, -7.4, 0.5, GLOW, "gem", EMIT)
    for k, y in enumerate((1.5, 5.0)):                                       # forearm bracer plates
        m.box(-3.4, y, -2.5, 1.4, y + 2.6, 2.5, TRIM, GOLD)
        m.box(-3.75, y + 0.5, -2.0, 1.75, y + 2.1, 2.0, MAIN, "plate", dens=1.2)
        m.box(-3.95, y + 1.1, -1.4, 1.95, y + 1.5, 1.4, GLOW, "white", EMIT)
    return m


# ----------------------------------------------------------------------------- boots (leg)
def boots():
    m = M("x3_boots", "rleg")                                                 # leg local: x -2..2, y 0..12, z -2..2
    ring(m, 7.4, 8.8, 2.7, 2.0, TRIM, GOLD)                                 # cuff
    m.box(-2.6, 8.8, -2.6, 2.6, 9.4, 2.6, MAIN, "plate")
    m.box(-2.7, 9.4, -2.7, 2.7, 12.4, 2.7, MAIN, "plate", dens=1.2)           # shell
    m.box(-2.9, 11.4, -2.9, 2.9, 12.4, 2.9, TRIM, GOLD)                     # sole
    m.box(-2.4, 10.6, -4.6, 2.4, 12.4, -2.6, TRIM, GOLD)                    # toe cap
    m.box(-1.9, 10.8, -5.0, 1.9, 11.8, -4.6, MAIN, "plate")
    m.box(-0.35, 8.9, -3.0, 0.35, 10.6, -2.7, GLOW, "white", EMIT)            # inlay line
    m.plate(-2.0, 1.0, 2.0, 7.4, -2.0, 0.8)                                  # shin plate
    m.diamond(0, 4.2, -3.0, 1)
    for k in range(4):                                                        # pixel fins out of the side
        for sx in (-1, 1):
            y = 6.0 - k * 1.3
            m.box(sx * 2.6, y - 1.0, -0.6, sx * (3.6 + k * 1.1), y + 0.3, 0.6, MAIN if k % 2 == 0 else GLOW, "plate" if k % 2 == 0 else "white", 0 if k % 2 == 0 else EMIT)
    return m


# ----------------------------------------------------------------------------- amulet (neck, body anchor)
def amulet():
    m = M("x3_amulet", "body")
    pts = []
    for i in range(9):                                                        # a V of gold links
        t = i / 8
        x = (1 - t) * 3.8
        y = -0.4 + 3.2 * math.sin(t * math.pi / 2) ** 1.0
        for sx in (-1, 1):
            m.box(sx * x - 0.45, y - 0.45, -2.9, sx * x + 0.45, y + 0.45, -2.2, TRIM, GOLD)
    m.plate(-2.2, 3.4, 2.2, 8.0, -3.0, 0.9)
    m.diamond(0, 5.6, -3.9, 1)
    m.box(-1.6, 8.0, -3.4, 1.6, 8.6, -2.4, TRIM, GOLD)
    m.box(-0.5, 8.6, -3.4, 0.5, 10.6, -2.6, GLOW, "gem", EMIT)
    return m


# ----------------------------------------------------------------------------- cape (back)
def cape():
    m = M("x3_cape", "body")
    z0 = 2.4
    for i in range(6):                                                        # stepped hanging panels, each longer and wider
        y0 = 0.4 + i * 3.6
        w = 4.6 + i * 0.35
        m.box(-w, y0, z0 + i * 0.35, w, y0 + 3.9, z0 + 0.9 + i * 0.35, TRIM, GOLD)
        m.box(-w + 0.7, y0 + 0.4, z0 + 0.9 + i * 0.35, w - 0.7, y0 + 3.4, z0 + 1.3 + i * 0.35, MAIN, "plate", dens=1.2)
        m.box(-w + 1.6, y0 + 1.6, z0 + 1.3 + i * 0.35, w - 1.6, y0 + 2.0, z0 + 1.55 + i * 0.35, GLOW, "white", EMIT)
    for sx in (-1, 1):                                                        # shoulder clasps
        m.box(sx * 3.0 - 0.9, -0.6, -2.6, sx * 3.0 + 0.9, 0.6, 2.8, TRIM, GOLD)
        m.box(sx * 3.0 - 0.45, -1.2, -0.5, sx * 3.0 + 0.45, -0.6, 0.5, GLOW, "gem", EMIT)
    return m


# ----------------------------------------------------------------------------- rendering
SAINT = (0xFFFFFF, 0xFFF3C8, 0x5FE3FF)         # panel, gold, cyan
ATLAS = np.asarray(Image.open(P.ATLAS).convert("RGBA"), np.float32) / 255


def shot(pieces, palette, zoom, views=("front", "three", "side", "back"), panel=(330, 380), fig=True):
    crop = P.ZOOMS[zoom]
    w, h = crop[1] - crop[0], crop[3] - crop[2]
    scale = min((panel[0] - 8) / w, (panel[1] - 20) / h)
    tiles = []
    for v in views:
        yaw, pitch = P.VIEWS[v]
        ss = 2
        im = P.render(pieces, palette, yaw, pitch, crop, scale * ss, (panel[0] * ss, panel[1] * ss), ATLAS, fig)
        tiles.append(im.reshape(panel[1], ss, panel[0], ss, 3).mean(axis=(1, 3)))
    return Image.fromarray((np.concatenate(tiles, axis=1) * 255).astype(np.uint8))


def row(label, img):
    out = Image.new("RGB", (img.width, img.height + 18), (24, 26, 34))
    out.paste(img, (0, 18))
    ImageDraw.Draw(out).text((8, 3), label, fill=(255, 255, 255))
    return out


def stack(rows):
    W = max(r.width for r in rows)
    out = Image.new("RGB", (W, sum(r.height for r in rows)), (24, 26, 34))
    y = 0
    for r in rows:
        out.paste(r, (0, y)); y += r.height
    return out


def main():
    paths = {}
    for f in (crown, cuirass, pauldron, boots, amulet, cape):
        mm = f()
        p, n = mm.write()
        paths[mm.name] = p
        print(mm.name, n, "quads")
    W = P.WORN
    old = lambda name, anchor, mirror=False: (os.path.join(W, name + ".txt"), anchor, mirror)
    new = lambda name, anchor, mirror=False: (paths[name], anchor, mirror)
    jobs = [
        ("Crown: BEFORE (crown_godking) / AFTER (x3 blocky)", "head", [old("crown_godking", "head")], [new("x3_crown", "head")], (0x8C8C9C, 0xD8C070, 0xFFE9A0)),
        ("Chest: BEFORE (chest_stone) / AFTER (x3 blocky cuirass + pauldrons)", "body", [old("chest_stone", "body"), old("pauldron_dragon", "rarm"), old("pauldron_dragon", "larm", True)],
         [new("x3_cuirass", "body"), new("x3_pauldron", "rarm"), new("x3_pauldron", "larm", True)], (0x8C8C9C, 0xD8C070, 0xFFE9A0)),
        ("Boots: BEFORE (boots_war) / AFTER (x3 blocky)", "leg", [old("boots_war", "rleg"), old("boots_war", "lleg", True)], [new("x3_boots", "rleg"), new("x3_boots", "lleg", True)], (0x8C8C9C, 0xD8C070, 0xFFE9A0)),
        ("Amulet + cape: BEFORE (amulet_crest + cape_short) / AFTER (x3 blocky)", "body", [old("amulet_crest", "body"), old("cape_short", "body")], [new("x3_amulet", "body"), new("x3_cape", "body")], (0x8C8C9C, 0xD8C070, 0xFFE9A0)),
    ]
    sheets = []
    for (label, zoom, before, after, oldpal) in jobs:
        zm = {"leg": "leg"}.get(zoom, zoom)
        a = row("BEFORE: " + label.split("/")[0].split(":")[1].strip(), shot(before, oldpal, zm))
        b = row("AFTER (palette: pale panel / gold / cyan): " + label.split("/")[1].strip(), shot(after, SAINT, zm))
        s = stack([a, b])
        s.save(os.path.join(OUT, "before_after_" + label.split(":")[0].split()[0].lower() + ".png"))
        sheets.append(s)
    # all together on one full-body figure
    allp = [new("x3_crown", "head"), new("x3_cuirass", "body"), new("x3_pauldron", "rarm"), new("x3_pauldron", "larm", True), new("x3_boots", "rleg"), new("x3_boots", "lleg", True), new("x3_amulet", "body"), new("x3_cape", "body")]
    shot(allp, SAINT, "all", panel=(380, 640)).save(os.path.join(OUT, "full_set_x3_blocky.png"))
    print("done")


if __name__ == "__main__":
    main()
