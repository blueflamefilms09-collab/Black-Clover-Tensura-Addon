#!/usr/bin/env python3
"""Generates the 4.40.0 look of the Devil Contract body: the Weg markings (v2), the horns (v2) and the wings (v2). Deterministic, idempotent, PIL only.

    python3 tools/gen_devil_textures_v2.py

Why: the first Weg markings (tools/gen_devil_textures.py) were scattered two-pixel blocks that read as chicken pox. The v2 markings are CONTINUOUS FLOW LINES painted on
"unrolled strips" (right side, front, left side, back side by side, as in the skin file), so a line wraps around the limb and carries on across the faces; they grow out of a collar
at the neck and run down the arms, the ribs, the spine and the legs, branch in the higher tiers and end in a tapering tip. Tiers differ by how far the lines reach and how many there are,
not by noise: low = a collar, one line down each arm and the tear lines of the face; mid = ribs, spine, one line per leg; high = lattice on the body, rings, two lines per limb, cheek lines;
supreme = every line branches into a filigree and the face carries it to the temples.

Writes to src/main/resources/assets/anime_multiverse/textures/entity/devil/ (the old files stay; the client option devil.classicLook = true draws them):
  weg2_<tier>[_slim].png and weg2_<tier>[_slim]_glow.png  (64x64, base layer only: never on hat / jacket / sleeves / pants)
  horns2.png (32x32, four segments, ridged bone with a glowing tip), wings2.png (64x64: left half bone, right half veined membrane), wings2_glow.png
and the previews docs/previews/devil/weg2_sheet.png, parts2_sheet.png and compare_old_new.png (not part of the mod). Self-checks at the end.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/anime_multiverse/textures/entity/devil"
PREV = ROOT / "docs/previews/devil"

TIERS = ["low", "mid", "high", "supreme"]
CORE = {"low": (0xB8, 0x86, 0xFF), "mid": (0xFF, 0x4A, 0x52), "high": (0xFF, 0xC2, 0x3A), "supreme": (0xFF, 0xF6, 0xCC)}
INK = (0x0C, 0x07, 0x14, 240)
OUTER = [(32, 0, 64, 16), (16, 32, 40, 48), (40, 32, 56, 48), (48, 48, 64, 64), (0, 32, 16, 48), (0, 48, 16, 64)]


def boxes(slim):
    aw = 3 if slim else 4
    # name: (u, v, w, h, d)
    return {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, aw, 12, 4), "rleg": (0, 16, 4, 12, 4),
            "larm": (32, 48, aw, 12, 4), "lleg": (16, 48, 4, 12, 4)}


class Strip:
    """The four side faces of a box laid side by side: x runs round the limb (wrapping), y runs down it."""

    def __init__(self, box):
        self.u, self.v, self.w, self.h, self.d = box
        self.W = 2 * (self.w + self.d)
        self.ink = set()
        self.core = set()

    def front_x(self, off=0.0):
        return self.d + self.w / 2 + off

    def back_x(self, off=0.0):
        return 2 * self.d + self.w + self.w / 2 + off

    def dot(self, x, y, core=False):
        x = int(round(x)) % self.W
        y = int(round(y))
        if 0 <= y < self.h:
            self.ink.add((x, y))
            if core:
                self.core.add((x, y))

    def flow(self, x, y, length, amp=1.2, freq=0.7, phase=0.0, drift=0.0, wide=False, up=False, branches=0, rng=None, depth=0):
        """A wavy line that runs down (or up); a wide one is two columns with a glowing edge; a branch leaves at a slant and tapers."""
        step = -1 if up else 1
        px = x
        for i in range(length):
            py = y + step * i
            px = x + amp * math.sin(phase + i * freq) + drift * i
            self.dot(px, py)
            if wide and i < length - 2:
                self.dot(px + 1, py, core=True)
            elif i >= 1 and i % 2 == 0:
                self.dot(px, py, core=True) if depth == 0 and not wide else None
        if branches and rng is not None and length > 4:
            for b in range(branches):
                at = 2 + (b * (length - 3)) // max(1, branches)
                side = -1 if (b + depth) % 2 == 0 else 1
                bx = x + amp * math.sin(phase + at * freq) + drift * at
                by = y + step * at
                bl = max(3, (length - at) * 2 // 3)
                self.flow(bx, by, bl, amp=0.0, drift=0.55 * side, up=up, depth=depth + 1, rng=rng)

    def ring(self, y, gap=0, core=False):
        for x in range(self.W):
            if gap and x % (gap + 3) >= 3:
                continue
            self.dot(x, y, core=core)


def paint(slim, tier):
    """One skin: returns (ink image, glow image)."""
    t = TIERS.index(tier)
    rng = random.Random(4400 + t)
    parts = {}
    for name, box in boxes(slim).items():
        parts[name] = Strip(box)
    head, body = parts["head"], parts["body"]
    # ---- head: tear lines under the eyes (low and up), a brow line (mid and up), the cheeks to the temples (high), the filigree over the sides and the back (supreme)
    for ex in (head.front_x(-2.5), head.front_x(+1.5)):
        head.flow(ex + 0.5, 6, 3, amp=0.0, drift=0.0)
    if t >= 1:
        head.flow(head.front_x(-0.5), 0, 3, amp=0.0)
        head.flow(head.front_x(0.5), 0, 3, amp=0.0)
    if t >= 2:
        for sx in (-3.5, 2.5):
            head.flow(head.front_x(sx), 5, 3, amp=0.6, freq=1.2, drift=-0.5 if sx < 0 else 0.5)
        head.ring(7, gap=2)
    if t >= 3:
        for k in range(3):
            head.flow(head.back_x(-2.5 + k * 2.5), 0, 6, amp=0.8, freq=0.8, phase=k, branches=1, rng=rng)
        head.flow(2.5, 1, 5, amp=0.5)
        head.flow(head.d + head.w + 2.5, 1, 5, amp=0.5)
    # ---- body: collar from the low tier; ribs and spine from mid; lattice from high; filigree at supreme
    body.ring(0, core=True)
    body.flow(body.front_x(-0.5), 1, 5, amp=0.0, wide=True)
    if t >= 1:
        for s in (-1, 1):
            body.flow(body.front_x(s * 1.5), 1, 11, amp=1.6, freq=0.55, phase=0.0 if s < 0 else math.pi, drift=0.35 * s, wide=(t >= 2), branches=1 if t >= 3 else 0, rng=rng)
        body.flow(body.back_x(-0.5), 1, 11, amp=0.9, freq=0.8, wide=True, branches=(2 if t >= 2 else 1), rng=rng)
    if t >= 2:
        body.ring(11, core=False)
        for s in (-2.5, 2.5):
            body.flow(body.back_x(s), 2, 8, amp=1.0, freq=0.6, phase=s, branches=0, rng=rng)
    if t >= 3:
        for s in (-3.0, 3.0):
            body.flow(body.front_x(s), 10, 5, amp=0.4, freq=1.0, up=True, branches=1, rng=rng)
        body.ring(6, gap=1)
    # ---- the arms and legs: lines that run down and turn a little round the limb, ending in a point
    for name in ("rarm", "larm"):
        a = parts[name]
        n = (1, 2, 2, 4)[t]
        for k in range(n):
            a.flow(a.front_x(-1.2) + k * a.W / n, 0, 11 if k % 2 == 0 else 8, amp=0.9, freq=0.6, phase=k * 1.7, drift=0.18 + 0.05 * k, wide=(k == 0 and t >= 1), branches=(1 if t >= 3 else 0) if k == 0 else 0, rng=rng)
        if t >= 3:
            a.ring(5, gap=1)
        if t >= 1:
            a.ring(11, core=True)
    for name in ("rleg", "lleg"):
        l = parts[name]
        n = (0, 1, 2, 3)[t]
        for k in range(n):
            l.flow(l.front_x(-1.0) + k * l.W / max(1, n), 0, 11, amp=0.8, freq=0.5, phase=k * 2.1, drift=0.12, wide=(k == 0), branches=(1 if t >= 3 else 0), rng=rng)
        if t >= 3:
            l.ring(6, gap=1)
        if t >= 3:
            l.ring(11, core=True)
    ink = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ci = CORE[tier]
    for name, s in parts.items():
        for (x, y) in s.ink:
            ink.putpixel((s.u + x, s.v + s.d + y), INK)
            glow.putpixel((s.u + x, s.v + s.d + y), ci + ((255 if (x, y) in s.core else 150),))
    return ink, glow


# ---------------------------------------------------------------------------------------------------- horns v2
def make_horns():
    """32x32. Four segments at texOffs (0, 0), (0, 8), (0, 16), (0, 24): each a 2x3x2 box, footprint 8 x 5. Ridged bone: dark root to pale tip, a ring every other row, an ember at the tip."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for seg in range(4):
        v0 = 8 * seg
        t = seg / 3.0
        base = (int(46 + 150 * t), int(36 + 130 * t), int(44 + 100 * t))
        for y in range(5):
            for x in range(8):
                ring = (y + seg) % 2 == 0
                shade = 0.78 if ring else 1.0
                lit = 1.0 + 0.12 * (1 if x in (1, 5) else 0)
                c = tuple(max(0, min(255, int(ch * shade * lit))) for ch in base)
                px[x, v0 + y] = c + (255,)
        if seg == 3:
            for y in range(5):
                for x in range(8):
                    px[x, v0 + y] = (255, 120 + 18 * y, 60, 255) if y < 2 else (240, 200 - 10 * y, 150, 255)
    return img


# ---------------------------------------------------------------------------------------------------- wings v2
def make_wings():
    """64x64. Left half (x 0 to 31): bone, boxes at texOffs (0,0) humerus, (0,6) forearm, (0,12/16/20) fingers. Right half (x 32 to 63): membrane wedges at (32 + 16*(k % 2), 20 * step).
    The membrane is a deep wine with a lit centre, fine veins fanning from the shoulder edge toward the tip, and a bright rim along the free edge."""
    rng = random.Random(4401)
    ink = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pi, pg = ink.load(), glow.load()
    for y in range(64):                                             # bone: ivory with a long-axis grain, darker toward the joints (the ends of each face), a pale ridge line along the top of each box
        for x in range(32):
            end = min(x % 14, 13 - x % 14) / 6.5                    # 0 at the ends of a 14 px span, 1 in the middle
            shade = 0.62 + 0.38 * min(1.0, end)
            grain = 1.0 - 0.07 * ((y + x // 7) % 3)
            c = (int(222 * shade * grain), int(206 * shade * grain), int(190 * shade * grain))
            pi[x, y] = c + (255,)
            if y % 4 == 0 and x % 14 == 0:
                pg[x, y] = (255, 90, 70, 150)
    for k in range(2):
        for st in range(3):
            x0, y0 = 32 + 16 * k, 20 * st
            hgt = (5, 10, 16)[st] if st < 3 else 16
            for y in range(20):
                for x in range(16):
                    gx = x / 15.0
                    gy = y / 19.0
                    lit = 0.55 + 0.45 * math.sin(math.pi * min(1.0, gx * 1.1)) * (0.6 + 0.4 * (1 - abs(gy - 0.5) * 2))
                    c = (int(80 + 110 * lit * 0.9), int(10 + 22 * lit), int(26 + 30 * lit))
                    pi[x0 + x, y0 + y] = c + (255,)
            for v in range(5):                                      # veins fanning from the shoulder side (x small, near the middle) to the free edge (spread over the whole height)
                vy, ve = 9 + (v - 2), 1 + v * 4 + rng.randint(0, 1)
                for x in range(16):
                    yy = int(round(vy + (x / 15.0) * (ve - vy)))
                    if 0 <= yy < 20:
                        pi[x0 + x, y0 + yy] = (36, 5, 16, 255)
                        pg[x0 + x, y0 + yy] = (255, 70 + 20 * v, 80, 200)
            for y in range(20):                                     # the free edge: a bright rim
                pi[x0 + 15, y0 + y] = (255, 120, 90, 255)
                pg[x0 + 15, y0 + y] = (255, 170, 110, 255)
    return ink, glow


# ---------------------------------------------------------------------------------------------------- previews
FRONT = {"head": (8, 8), "body": (20, 20), "rarm": (44, 20), "larm": (36, 52), "rleg": (4, 20), "lleg": (20, 52)}
BACK = {"head": (24, 8), "body": (32, 20), "rarm": (52, 20), "larm": (44, 52), "rleg": (12, 20), "lleg": (28, 52)}
SIDE = {"head": (16, 8), "body": (28, 20), "rarm": (48, 20), "rleg": (8, 20)}
BASEFLESH = (0xC9, 0x9A, 0x78)
BASECLOTH = (0x4A, 0x4D, 0x58)
BASELEG = (0x2A, 0x2C, 0x3C)
SIZES = {"head": (8, 8), "body": (8, 12), "rarm": (4, 12), "larm": (4, 12), "rleg": (4, 12), "lleg": (4, 12)}


def view(layout, skin, glow, scale, slim=False):
    """A flat figure from the front, back or side: the marks over a flat skin, the glow at 70 %."""
    w = 16 * scale
    img = Image.new("RGBA", (w, 32 * scale), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    place = {"head": (4, 0), "body": (4, 8), "rarm": (0, 8), "larm": (12, 8), "rleg": (4, 20), "lleg": (8, 20)}
    if slim:
        place["rarm"], place["larm"] = (0.75, 8), (12, 8)
    for name, (sx, sy) in layout.items():
        pw, ph = SIZES[name]
        if slim and name in ("rarm", "larm"):
            pw = 3
        px, py = place[name]
        col = BASEFLESH if name == "head" else BASELEG if "leg" in name else BASECLOTH
        for y in range(ph):
            for x in range(pw):
                c = col
                m = skin.getpixel((sx + x, sy + y))
                g = glow.getpixel((sx + x, sy + y))
                out = c + (255,)
                if m[3]:
                    a = m[3] / 255.0
                    out = tuple(int(c[i] * (1 - a) + m[i] * a) for i in range(3)) + (255,)
                if g[3]:
                    a = 0.45 * g[3] / 255.0
                    out = tuple(min(255, int(out[i] + g[i] * a)) for i in range(3)) + (255,)
                d.rectangle([(px + x) * scale, (py + y) * scale, (px + x + 1) * scale - 1, (py + y + 1) * scale - 1], fill=out)
    return img


def sheet(images):
    """images: {tier: (skin, glow)}; one row per tier: front, back, side."""
    scale = 8
    cell_w, cell_h = 16 * scale + 24, 32 * scale + 56
    sh = Image.new("RGB", (cell_w * 3 + 24, cell_h * 4 + 12), (0x2A, 0x2D, 0x38))
    d = ImageDraw.Draw(sh)
    for r, tier in enumerate(TIERS):
        skin, glow = images[tier]
        d.text((14, r * cell_h + 8), "%s  (front, back, side; the glow at 45 percent)" % tier, fill=(0xE8, 0xE8, 0xEE))
        for c, lay in enumerate((FRONT, BACK, SIDE)):
            sh.paste(view(lay, skin, glow, scale), (12 + c * cell_w, r * cell_h + 30), view(lay, skin, glow, scale))
    return sh


def parts_sheet(horns, wings, wglow):
    sh = Image.new("RGB", (1000, 460), (0x80, 0x80, 0x80))
    d = ImageDraw.Draw(sh)
    for i, (name, im, sc) in enumerate((("horns2.png 32x32", horns, 6), ("wings2.png 64x64 (left bone, right membrane)", wings, 6), ("wings2_glow.png 64x64", wglow, 6))):
        big = im.resize((im.width * sc, im.height * sc), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (0x14, 0x14, 0x18, 255))
        bg.alpha_composite(big)
        x = 10 + (0 if i == 0 else 230 + (i - 1) * 395) if i < 3 else 0
        sh.paste(bg.convert("RGB"), (x, 40))
        d.text((x, 14), name, fill=(255, 255, 255))
    return sh


def compare(new_images):
    """Old files next to the new ones, tier by tier (the front view)."""
    scale = 6
    rows = []
    sh = Image.new("RGB", (4 * (16 * scale + 20) + 20, 2 * (32 * scale + 50) + 10), (0x2A, 0x2D, 0x38))
    d = ImageDraw.Draw(sh)
    for r, tag in enumerate(("old (classicLook)", "new (4.40.0)")):
        d.text((10, r * (32 * scale + 50) + 6), tag, fill=(255, 255, 255))
        for c, tier in enumerate(TIERS):
            if r == 0:
                skin = Image.open(OUT / ("weg_%s.png" % tier)).convert("RGBA")
                glow = Image.open(OUT / ("weg_%s_glow.png" % tier)).convert("RGBA")
            else:
                skin, glow = new_images[tier]
            v = view(FRONT, skin, glow, scale)
            sh.paste(v, (10 + c * (16 * scale + 20), r * (32 * scale + 50) + 30), v)
    return sh


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    PREV.mkdir(parents=True, exist_ok=True)
    wide = {}
    marks = {}
    for tier in TIERS:
        for slim in (False, True):
            ink, glow = paint(slim, tier)
            suffix = "_slim" if slim else ""
            ink.save(OUT / ("weg2_%s%s.png" % (tier, suffix)))
            glow.save(OUT / ("weg2_%s%s_glow.png" % (tier, suffix)))
            if not slim:
                wide[tier] = (ink, glow)
                marks[tier] = sum(1 for p in ink.get_flattened_data() if p[3])
    horns = make_horns()
    horns.save(OUT / "horns2.png")
    wings, wglow = make_wings()
    wings.save(OUT / "wings2.png")
    wglow.save(OUT / "wings2_glow.png")
    sheet(wide).save(PREV / "weg2_sheet.png")
    parts_sheet(horns, wings, wglow).save(PREV / "parts2_sheet.png")
    compare(wide).save(PREV / "compare_old_new.png")
    # ---- self-checks: marks never touch the outer layer, the tiers only grow, the glow lies on the marks
    for tier in TIERS:
        for slim in (False, True):
            suffix = "_slim" if slim else ""
            ink = Image.open(OUT / ("weg2_%s%s.png" % (tier, suffix))).convert("RGBA")
            glow = Image.open(OUT / ("weg2_%s%s_glow.png" % (tier, suffix))).convert("RGBA")
            for (x0, y0, x1, y1) in OUTER:
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        assert ink.getpixel((x, y))[3] == 0 and glow.getpixel((x, y))[3] == 0, "mark on the outer layer %s %s" % (tier, (x, y))
            for y in range(64):
                for x in range(64):
                    if glow.getpixel((x, y))[3]:
                        assert ink.getpixel((x, y))[3], "glow without a mark at %s" % ((x, y),)
    counts = [marks[t] for t in TIERS]
    assert counts == sorted(counts) and len(set(counts)) == 4, "the tiers must grow: %s" % counts
    assert counts[0] >= 25 and counts[3] <= 700, counts
    print("weg2 marked pixels per tier:", dict(zip(TIERS, counts)))
    print("wrote weg2_*, horns2, wings2, wings2_glow and the previews in", PREV)


if __name__ == "__main__":
    main()
