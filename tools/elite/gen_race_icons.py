#!/usr/bin/env python3
"""Draws the five 32x32 race icons of the elite races (Tensura shows a race's icon in its evolution screen and race list).

Why a script: the icons are generated, not hand-painted, so they can be regenerated if a tier changes colour, and the output is deterministic (same pixels every run;
`--check` fails when a committed file differs). Pure standard library (zlib + struct), 4x supersampled for soft edges. The Saiyan icons are a dark badge with a crown of
hair spikes whose number and colour climb with the tier; the Primordial icon is a mana orb in a ring with four motes. No Time Magic asset is read or touched.

Usage: python tools/elite/gen_race_icons.py [--check]
"""
import math
import struct
import sys
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "assets" / "nusmp" / "textures" / "race"
SIZE, SS = 32, 4


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def png(w, h, rgba):
    raw = b"".join(b"\x00" + bytes(rgba[y * w * 4:(y + 1) * w * 4]) for y in range(h))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


class Canvas:
    """A supersampled RGBA canvas; shapes are painted in order over it."""

    def __init__(self):
        n = SIZE * SS
        self.n = n
        self.px = [[(0.0, 0.0, 0.0, 0.0)] * n for _ in range(n)]

    def paint(self, test, colour):
        """colour is (r, g, b) or a function (x, y) -> (r, g, b); test(x, y) in 0..SIZE space says whether the sample is covered."""
        for j in range(self.n):
            y = (j + 0.5) / SS
            row = self.px[j]
            for i in range(self.n):
                x = (i + 0.5) / SS
                if test(x, y):
                    r, g, b = colour(x, y) if callable(colour) else colour
                    row[i] = (r, g, b, 1.0)

    def to_rgba(self):
        out = bytearray()
        for y in range(SIZE):
            for x in range(SIZE):
                r = g = b = a = 0.0
                for dj in range(SS):
                    for di in range(SS):
                        pr, pg, pb, pa = self.px[y * SS + dj][x * SS + di]
                        r += pr * pa; g += pg * pa; b += pb * pa; a += pa
                if a <= 0:
                    out += bytes((0, 0, 0, 0))
                else:
                    out += bytes((round(r / a), round(g / a), round(b / a), round(255 * a / (SS * SS))))
        return bytes(out)


def disc(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def ring(cx, cy, r0, r1):
    return lambda x, y: r0 * r0 <= (x - cx) ** 2 + (y - cy) ** 2 <= r1 * r1


def polygon(pts):
    def inside(x, y):
        c = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]; xj, yj = pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                c = not c
            j = i
        return c
    return inside


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def saiyan(tier, hair, glow):
    c = Canvas()
    c.paint(disc(16, 16, 15.2), hexc("#1d1626"))
    c.paint(ring(16, 16, 13.6, 15.2), lerp(hexc(hair), (0, 0, 0), 0.35))
    if glow:
        c.paint(ring(16, 16, 12.2, 13.6), hexc(glow))
    # a crown of hair spikes: tier + 2 of them, the middle ones tallest
    n = tier + 2
    pts = [(7.0, 24.0)]
    base_y = 21.0
    for i in range(n):
        x0 = 7.0 + 18.0 * i / n
        x1 = 7.0 + 18.0 * (i + 1) / n
        mid = (x0 + x1) / 2
        centre = 1.0 - abs((i + 0.5) / n - 0.5) * 1.1
        pts += [(x0, base_y), (mid, base_y - 4.0 - 11.0 * centre)]
    pts += [(25.0, base_y), (25.0, 24.0)]
    c.paint(polygon(pts), lambda x, y: lerp(hexc(hair), (255, 255, 255), max(0.0, min(0.55, (24.0 - y) / 30.0))))
    # a face band under the hair so the badge reads as a head
    c.paint(polygon([(8.5, 24.0), (23.5, 24.0), (21.5, 27.5), (10.5, 27.5)]), lerp(hexc(hair), (0, 0, 0), 0.45))
    return c


def primordial():
    c = Canvas()
    c.paint(disc(16, 16, 15.2), hexc("#0e1830"))
    c.paint(ring(16, 16, 13.6, 15.2), hexc("#6fd8ff"))
    c.paint(disc(16, 16, 9.0), lambda x, y: lerp(hexc("#e9fbff"), hexc("#8a6cff"), min(1.0, math.hypot(x - 14.0, y - 13.5) / 11.0)))
    c.paint(ring(16, 16, 10.2, 11.0), hexc("#b8a6ff"))
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        c.paint(disc(16 + 12.4 * math.cos(a), 16 + 12.4 * math.sin(a), 1.5), hexc("#ffffff"))
    return c


ICONS = {
    "lesser_saiyan": lambda: saiyan(1, "#b98a3a", None),
    "medium_saiyan": lambda: saiyan(2, "#e0b030", None),
    "high_saiyan": lambda: saiyan(3, "#ffd84a", "#8a6a14"),
    "divine_saiyan": lambda: saiyan(4, "#fff2a8", "#ffffff"),
    "primordial": primordial,
}


def main():
    check = "--check" in sys.argv
    OUT.mkdir(parents=True, exist_ok=True)
    bad = 0
    for name, make in ICONS.items():
        data = png(SIZE, SIZE, make().to_rgba())
        path = OUT / (name + ".png")
        if check:
            if not path.exists() or path.read_bytes() != data:
                print("DIFFERS", path)
                bad += 1
        else:
            path.write_bytes(data)
            print("wrote", path)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
