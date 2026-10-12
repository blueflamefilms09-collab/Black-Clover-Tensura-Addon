"""1.5.0: the Magic Stone's 16x16 inventory icon (a dark violet jewel with a bright core)."""
import math
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "magic_stone.png")
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
px = im.load()
cx, cy = 7.5, 7.5
for y in range(16):
    for x in range(16):
        d = abs(x - cx) * 0.9 + abs(y - cy) * 0.75                       # a diamond-cut gem
        if d > 7.0:
            continue
        t = d / 7.0
        shade = 1.0 - 0.55 * t
        facet = 0.18 * math.sin((x - y) * 0.9)
        r, g, b = 90 + 80 * (1 - t), 20 + 40 * (1 - t), 150 + 90 * (1 - t)
        k = shade + facet
        px[x, y] = (int(min(255, r * k)), int(min(255, g * k)), int(min(255, b * k)), 255)
for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8), (6, 6), (9, 9)):
    px[x, y] = (236, 210, 255, 255)
for x in range(16):
    for y in range(16):
        if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = (24, 8, 40, 255)
im.save(OUT)
print("wrote", OUT)
