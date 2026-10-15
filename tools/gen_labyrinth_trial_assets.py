#!/usr/bin/env python3
"""
Assets of the Labyrinth's trial chambers (docs/labyrinth/e1_trial.md): the Sealed Archway keystone (its top and its three eye faces:
idle, active, cleared), the Magic Barrier (translucent, animated), the Ward Rune and the Spawn Glyph floor tiles, with their mcmeta,
block models, blockstates and item models. Deterministic and idempotent: running it again rewrites the same bytes.

    python3 tools/gen_labyrinth_trial_assets.py          # write
    python3 tools/gen_labyrinth_trial_assets.py --check  # regenerate in memory and list every repo file that differs (exit 1 if any)
"""
import io
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
N = 16

CYAN, CYAN_DIM, CYAN_HALO = (150, 245, 255), (40, 95, 110), (50, 150, 175)
VIOLET, VIOLET_DIM = (200, 95, 255), (90, 40, 120)


def stone(seed, base=(44, 42, 56), spread=14, rim=10):
    """Dressed dark stone: soft noise, a one-pixel lighter rim so a block reads as a cut block, not a smear."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (N, N))
    px = img.load()
    for y in range(N):
        for x in range(N):
            v = rng.randint(-spread, spread)
            edge = rim if (x == 0 or y == 0) else (-rim // 2 if (x == N - 1 or y == N - 1) else 0)
            px[x, y] = tuple(max(0, min(255, c + v + edge)) for c in base) + (255,)
    return img


def tile(seed, base, line):
    """A floor tile: stone with a dark grout line along two edges and a faint lighter line along the other two (bevelled)."""
    img = stone(seed, base, 10, 0)
    px = img.load()
    for i in range(N):
        px[i, N - 1] = line + (255,)
        px[N - 1, i] = line + (255,)
        px[i, 0] = tuple(min(255, c + 12) for c in base) + (255,)
        px[0, i] = tuple(min(255, c + 12) for c in base) + (255,)
    return img


def blend(px, x, y, col, a):
    if 0 <= x < N and 0 <= y < N:
        r, g, b, _ = px[x, y]
        px[x, y] = (int(r + (col[0] - r) * a), int(g + (col[1] - g) * a), int(b + (col[2] - b) * a), 255)


def eye(seed, mode):
    """The keystone's side face: an almond eye carved into dark stone. idle: dim teal iris. active: burning cyan with a halo. cleared: grey, cracked."""
    img = stone(seed)
    px = img.load()
    cx, cy = 7.5, 7.5
    iris = {"idle": CYAN_DIM, "active": (110, 245, 255), "cleared": (120, 118, 130)}[mode]
    for y in range(N):
        for x in range(N):
            dx, dy = (x - cx) / 6.5, (y - cy) / 3.4
            d = math.sqrt(dx * dx + dy * dy)
            if mode == "active" and d < 1.9:
                blend(px, x, y, CYAN_HALO, max(0.0, 0.55 * (1.9 - d) / 1.9))
            if d <= 1.0:
                px[x, y] = (18, 16, 26, 255)                                   # the sclera: darker than the stone
            if d < 1.15 and d > 0.95:
                px[x, y] = (96, 94, 112, 255)                                  # the carved rim
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - cx, y - cy)
            if r <= 2.4:
                px[x, y] = iris + (255,)
            if mode != "cleared" and abs(x - cx) < 0.6 and abs(y - cy) <= 2.0:
                px[x, y] = (8, 8, 16, 255)                                     # the slit pupil
    if mode == "active":
        px[6, 6] = (255, 255, 255, 255)
        px[9, 9] = (210, 255, 255, 255)
    if mode == "idle":
        px[6, 6] = (90, 150, 160, 255)
    if mode == "cleared":
        for i in range(-5, 6):                                                 # a crack from top-left to bottom-right through the iris
            x, y = 7 + i, 7 + i + (1 if i > 1 else 0)
            blend(px, x, y, (14, 12, 20), 1.0)
            blend(px, x + 1, y, (70, 68, 84), 0.6)
    return img


def keystone_top(seed):
    img = stone(seed)
    px = img.load()
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - 7.5, y - 7.5)
            if 4.6 <= r <= 5.6:
                blend(px, x, y, (84, 82, 100), 0.8)
            if r <= 1.2:
                blend(px, x, y, CYAN_DIM, 0.9)
    return img


def barrier_frames(frames=4):
    """A translucent cyan sheet with a hexagon-and-diamond lattice (period 8, so cells tile) and a shimmer that drifts across it."""
    out = Image.new("RGBA", (N, N * frames))
    hexagon = [(4, 0), (8, 2), (8, 6), (4, 8), (0, 6), (0, 2)]
    for f in range(frames):
        img = Image.new("RGBA", (N, N), (70, 215, 250, 88))
        d = ImageDraw.Draw(img)
        for ox in (-8, 0, 8):
            for oy in (-8, 0, 8):
                pts = [(x + ox, y + oy) for x, y in hexagon]
                d.line(pts + [pts[0]], fill=(175, 250, 255, 205), width=1)
        px = img.load()
        for y in range(N):
            for x in range(N):
                band = (x + y - f * 8) % 32
                if band < 3:
                    r, g, b, a = px[x, y]
                    px[x, y] = (min(255, r + 70), min(255, g + 30), 255, min(255, a + 55))
        for ox in (0, 8):
            for oy in (0, 8):
                for vx, vy in hexagon:
                    x, y = (vx + ox) % N, (vy + oy) % N
                    px[x, y] = (235, 255, 255, 230)                            # the nodes of the lattice
        out.paste(img, (0, f * N))
    return out


def ward_rune_top(seed):
    img = tile(seed, (46, 48, 62), (26, 28, 38))
    px = img.load()
    glyph = [(7, 3), (7, 4), (7, 5), (7, 6), (7, 7), (7, 8), (7, 9), (7, 10), (7, 11), (7, 12),
             (6, 6), (8, 6), (5, 7), (9, 7), (6, 8), (8, 8),                     # the diamond at the heart
             (5, 3), (6, 3), (8, 3), (9, 3), (5, 12), (9, 12), (6, 12), (8, 12)]  # the ticks
    for x, y in glyph:
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            blend(px, x + dx, y + dy, CYAN_HALO, 0.35)
    for x, y in glyph:
        px[x, y] = CYAN + (255,)
    return img


def rune_side(seed, base, line, col):
    img = tile(seed, base, line)
    px = img.load()
    for x in range(1, N - 1):
        blend(px, x, 3, col, 0.75 if x % 3 else 0.35)
    return img


def spawn_glyph_frames(seed, frames=4):
    """A violet summoning circle on a darker tile, pulsing over four frames."""
    out = Image.new("RGBA", (N, N * frames))
    for f in range(frames):
        pulse = 0.65 + 0.35 * (0.5 + 0.5 * math.cos(2 * math.pi * f / frames))
        col = tuple(int(c * pulse) for c in VIOLET)
        img = tile(seed, (40, 34, 52), (22, 18, 30))
        px = img.load()
        cx = cy = 7.5
        for y in range(N):
            for x in range(N):
                r = math.hypot(x - cx, y - cy)
                if 5.2 <= r <= 6.3:
                    blend(px, x, y, VIOLET_DIM, 0.5)
                if 5.5 <= r <= 6.0:
                    px[x, y] = col + (255,)
        tri = [(7.5 + 4.2 * math.cos(math.radians(a)), 7.5 + 4.2 * math.sin(math.radians(a))) for a in (90, 210, 330)]
        d = ImageDraw.Draw(img)
        d.line([tri[0], tri[1], tri[2], tri[0]], fill=col + (255,), width=1)
        for x, y in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
            px[x, y] = tuple(min(255, c + 40) for c in col) + (255,)
        px[7, 7] = px[8, 8] = px[7, 8] = px[8, 7] = col + (255,)
        out.paste(img, (0, f * N))
    return out


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def json_bytes(obj):
    return (json.dumps(obj, indent=2) + "\n").encode()


def build():
    files = {}
    tx = "textures/block/"
    files[tx + "sealed_archway_top.png"] = png_bytes(keystone_top(1935101))
    for mode in ("idle", "active", "cleared"):
        files[tx + "sealed_archway_eye_%s.png" % mode] = png_bytes(eye(1935102, mode))
    files[tx + "magic_barrier.png"] = png_bytes(barrier_frames())
    files[tx + "magic_barrier.png.mcmeta"] = json_bytes({"animation": {"frametime": 5}})
    files[tx + "ward_rune_top.png"] = png_bytes(ward_rune_top(1935103))
    files[tx + "ward_rune_side.png"] = png_bytes(rune_side(1935104, (46, 48, 62), (26, 28, 38), CYAN))
    files[tx + "spawn_glyph_top.png"] = png_bytes(spawn_glyph_frames(1935105))
    files[tx + "spawn_glyph_top.png.mcmeta"] = json_bytes({"animation": {"frametime": 8}})
    files[tx + "spawn_glyph_side.png"] = png_bytes(rune_side(1935106, (40, 34, 52), (22, 18, 30), VIOLET))

    def bottom_top(top, bottom, side, extra=None):
        m = {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": "nusmp:block/" + top, "bottom": "nusmp:block/" + bottom, "side": "nusmp:block/" + side}}
        if extra:
            m.update(extra)
        return m

    for mode in ("idle", "active", "cleared"):
        files["models/block/sealed_archway_%s.json" % mode] = json_bytes(bottom_top("sealed_archway_top", "sealed_archway_top", "sealed_archway_eye_" + mode))
    files["models/block/magic_barrier.json"] = json_bytes({"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": "nusmp:block/magic_barrier"}})
    files["models/block/ward_rune.json"] = json_bytes(bottom_top("ward_rune_top", "ward_rune_side", "ward_rune_side"))
    files["models/block/spawn_glyph.json"] = json_bytes(bottom_top("spawn_glyph_top", "spawn_glyph_side", "spawn_glyph_side"))

    files["blockstates/sealed_archway.json"] = json_bytes({"variants": {"phase=%s" % m: {"model": "nusmp:block/sealed_archway_%s" % m} for m in ("idle", "active", "cleared")}})
    for b in ("magic_barrier", "ward_rune", "spawn_glyph"):
        files["blockstates/%s.json" % b] = json_bytes({"variants": {"": {"model": "nusmp:block/" + b}}})

    files["models/item/sealed_archway.json"] = json_bytes({"parent": "nusmp:block/sealed_archway_idle"})
    for b in ("magic_barrier", "ward_rune", "spawn_glyph"):
        files["models/item/%s.json" % b] = json_bytes({"parent": "nusmp:block/" + b})
    return files


def main(argv):
    files = build()
    check = "--check" in argv
    bad = 0
    for rel, data in sorted(files.items()):
        path = os.path.join(ASSETS, rel)
        if check:
            try:
                same = open(path, "rb").read() == data
            except FileNotFoundError:
                same = False
            if not same:
                bad += 1
                print("DIFFERS: " + rel)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as f:
                f.write(data)
    if check:
        print("%d files checked, %d differ" % (len(files), bad))
        return 1 if bad else 0
    print("%d files written under %s" % (len(files), os.path.normpath(ASSETS)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
