"""Why: the 21 skills of the EliteTensuraAddon job (docs/elite/skills.md) each need their own 32x32 icon in the skill list, in the same framed, glowing style as the 38 rebuilt skills, and no two
icons may be alike (the owner's rule), also not alike any older skill icon. This script draws them with the drawing helpers of tools/skills/gen_skill_icons.py (imported, not copied: that module only
draws when run as a script) and the same pipeline (a radial background, the symbol at 8x, a soft glow in the accent colour, a dark rim, the frame, a LANCZOS reduction to 32 px).

    python tools/elite/gen_skill_icons.py            # writes src/main/resources/assets/nusmp/textures/skill/<id>.png (21 files)
    python tools/elite/gen_skill_icons.py --sheet    # also writes build/elite_skill_icons/sheet.png (labelled preview, not shipped)

Frames: Aspectual magic = blue steel, Extra = steel, Intrinsic = bronze, Unique = gold, Ultimate = black iron lit in the skill's colour. The script refuses to finish if two of the new icons are identical,
or if one equals an icon already in the skill texture folder. Deterministic: the same inputs always give the same PNGs. It never touches the existing icons.
"""
import hashlib
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "skills"))
import gen_skill_icons as base  # noqa: E402  (the helpers; importing it draws nothing)
from PIL import Image, ImageDraw, ImageFilter  # noqa: E402

C, S, N = base.C, base.S, base.N
poly, ell, ln, arc, star, ngon, sparkle, rays, bezier = base.poly, base.ell, base.ln, base.arc, base.star, base.ngon, base.sparkle, base.rays, base.bezier
mix, lighten, darken, rgb = base.mix, base.lighten, base.darken, base.rgb

ROOT = base.ROOT
OUT = base.OUT
PREVIEW = os.path.join(ROOT, "build", "elite_skill_icons")

# id -> (frame kind, inner background, outer background, main colour, accent colour)
PAL = {
    "gravity_well": ("magic", 0x6A4AD8, 0x0A0620, 0xB8A4FF, 0x7AE0FF),
    "kamehameha": ("magic", 0x4AB8FF, 0x061430, 0xE8F8FF, 0x7FD6FF),
    "static_chain": ("magic", 0xFFE04A, 0x241E04, 0xFFF4A0, 0xA8C8FF),
    "ancient_soul": ("extra", 0xB8A0E8, 0x140C28, 0xE8DCFF, 0xFFE9A0),
    "iron_resolve": ("extra", 0x8A98B0, 0x0E121C, 0xC8D2E6, 0xE0B040),
    "meditation": ("extra", 0x6AD8C8, 0x06201C, 0xE8FFF8, 0xFFE0A0),
    "runesmiths_insight": ("extra", 0xE0A83A, 0x241606, 0xFFE9A0, 0x7AD8FF),
    "scavenger": ("extra", 0xA88858, 0x1E1608, 0xD8C090, 0xFFD84A),
    "kaioken": ("intrinsic", 0xE02818, 0x2A0402, 0xFF7A3A, 0xFFE070),
    "mechanical_eye": ("intrinsic", 0x6A7A90, 0x080C14, 0xC0CCDC, 0x7AD8FF),
    "mechanical_hands": ("intrinsic", 0x8890A0, 0x10141C, 0xC8CEDA, 0xFF9A3A),
    "crocodile_skin": ("unique", 0x5E8A3A, 0x0A1606, 0x8AB84A, 0xE8D860),
    "hecate": ("unique", 0x6A5AD8, 0x0A0820, 0xE8F0FF, 0xFFD8F0),
    "void_edge": ("unique", 0x6A3FC0, 0x06040E, 0xC8C0E8, 0x9A5AFF),
    "basilisk_skin": ("ultimate", 0x3A9A4A, 0x041408, 0x9AD86A, 0xF0F060),
    "beelzebuth": ("ultimate", 0x8A1A4A, 0x10040A, 0xE8B0C8, 0xFF4A6A),
    "gaia": ("ultimate", 0x4AAA3A, 0x08180A, 0xA8E070, 0xFFD060),
    "hephaestus_ultimate": ("ultimate", 0xFF7A2A, 0x1C0802, 0xC0C8D4, 0xFFC040),
    "ouroboros": ("ultimate", 0xA06AFF, 0x0C0620, 0xD8C0FF, 0xFFE070),
    "rapheal": ("ultimate", 0x4AB8FF, 0x040A20, 0xD8F4FF, 0xFFE9A0),
    "void_sovereign": ("ultimate", 0x4A2A9A, 0x020208, 0x7A5AD8, 0xE8D8FF),
}


# --------------------------------------------------------------------------- the symbols (all on a 256 canvas)
def S_gravity_well(d, p):
    for i, r in enumerate((104, 82, 60, 38)):
        ell(d, C, C, r, r * 0.62, None, mix(p[3], p[4], i / 3.0), 9 - i)
    ell(d, C, C, 20, 13, (6, 4, 18))
    for k in range(6):
        a = math.radians(k * 60 + 15)
        x0, y0 = C + math.cos(a) * 112, C + math.sin(a) * 70
        x1, y1 = C + math.cos(a) * 54, C + math.sin(a) * 34
        ln(d, [(x0, y0), (x1, y1)], p[4], 7)
        poly(d, [(x1, y1), (x1 + math.cos(a + 2.5) * 16, y1 + math.sin(a + 2.5) * 10), (x1 + math.cos(a - 2.5) * 16, y1 + math.sin(a - 2.5) * 10)], p[4])


def S_kamehameha(d, p):
    poly(d, [(C - 10, C - 18), (C + 112, C - 40), (C + 112, C + 40), (C - 10, C + 18)], mix(p[4], (255, 255, 255), 0.2))
    poly(d, [(C - 10, C - 9), (C + 112, C - 20), (C + 112, C + 20), (C - 10, C + 9)], (255, 255, 255))
    ell(d, C - 54, C, 46, 46, p[4])
    ell(d, C - 54, C, 33, 33, p[3])
    ell(d, C - 54, C, 18, 18, (255, 255, 255))
    for ang in (35, 110, 190, 270, 330):
        a = math.radians(ang)
        ln(d, [(C - 54 + math.cos(a) * 52, C + math.sin(a) * 52), (C - 54 + math.cos(a) * 74, C + math.sin(a) * 74)], p[4], 7)
    sparkle(d, C + 92, C - 62, 18, (255, 255, 255))
    sparkle(d, C + 70, C + 60, 14, p[4])


def link(d, cx, cy, w, h, rot, col, edge):
    pts = []
    for i in range(24):
        a = i * math.pi / 12
        # a rounded rectangle outline from a superellipse
        x = math.copysign(abs(math.cos(a)) ** 0.5, math.cos(a)) * w
        y = math.copysign(abs(math.sin(a)) ** 0.5, math.sin(a)) * h
        r = math.radians(rot)
        pts.append((cx + x * math.cos(r) - y * math.sin(r), cy + x * math.sin(r) + y * math.cos(r)))
    d.line(pts + [pts[0]], fill=edge + (255,), width=16, joint="curve")
    d.line(pts + [pts[0]], fill=col + (255,), width=9, joint="curve")


def S_static_chain(d, p):
    link(d, C - 56, C - 52, 38, 22, -40, p[4], darken(p[4], 0.5))
    link(d, C - 12, C - 14, 38, 22, 50, p[4], darken(p[4], 0.5))
    link(d, C + 32, C + 24, 38, 22, -40, p[4], darken(p[4], 0.5))
    link(d, C + 72, C + 62, 36, 20, 50, p[4], darken(p[4], 0.5))
    base.bolt(d, C + 6, C - 6, 98, p[3], (255, 255, 255))


def S_ancient_soul(d, p):
    base.flame(d, C, C + 76, 168, 124, mix(p[3], p[4], 0.2), (255, 255, 255))
    ell(d, C - 18, C + 6, 8, 12, darken(p[3], 0.6))
    ell(d, C + 18, C + 6, 8, 12, darken(p[3], 0.6))
    for i in range(8):
        a = math.radians(i * 45 + 20)
        ell(d, C + math.cos(a) * 100, C + 6 + math.sin(a) * 84, 7, 7, p[4])
    arc(d, C, C + 6, 96, 82, 20, 340, p[3], 5)


def S_iron_resolve(d, p):
    pts = [(C - 78, C - 78), (C + 78, C - 78), (C + 78, C + 4), (C + 44, C + 62), (C, C + 98), (C - 44, C + 62), (C - 78, C + 4)]
    poly(d, pts, darken(p[3], 0.25), p[3], 8)
    inner = [(C + (x - C) * 0.78, C - 6 + (y - C + 6) * 0.78) for x, y in pts]
    poly(d, inner, p[3])
    poly(d, [(C - 9, C - 66), (C + 9, C - 66), (C + 9, C + 80), (C - 9, C + 80)], darken(p[3], 0.35))
    poly(d, [(C - 62, C - 22), (C + 62, C - 22), (C + 62, C - 5), (C - 62, C - 5)], darken(p[3], 0.35))
    for x, y in ((C - 54, C - 60), (C + 54, C - 60), (C - 36, C + 30), (C + 36, C + 30)):
        ell(d, x, y, 7, 7, p[4])
    ell(d, C, C - 14, 17, 17, p[4])


def S_meditation(d, p):
    for i, r in enumerate((98, 76, 54)):
        ell(d, C, C - 4, r, r, None, mix(p[3], p[4], i / 2.0), 5)
    ell(d, C, C - 52, 20, 20, p[3])
    poly(d, [(C - 20, C - 28), (C + 20, C - 28), (C + 44, C + 30), (C - 44, C + 30)], p[3])
    poly(d, [(C - 74, C + 40), (C - 40, C + 14), (C - 10, C + 40), (C + 10, C + 40), (C + 40, C + 14), (C + 74, C + 40), (C + 40, C + 62), (C - 40, C + 62)], mix(p[3], (0, 0, 0), 0.2))
    ell(d, C, C - 4, 12, 12, p[4])
    sparkle(d, C - 82, C - 70, 14, p[4])
    sparkle(d, C + 84, C - 64, 12, (255, 255, 255))


def S_runesmith(d, p):
    poly(d, [(C, C - 100), (C + 74, C - 20), (C + 52, C + 92), (C - 52, C + 92), (C - 74, C - 20)], darken(p[3], 0.35), p[3], 8)
    base.eye(d, C, C - 4, 40, 17, p[4], (10, 10, 20), darken(p[3], 0.5))
    for pts in (((C - 40, C - 60), (C - 14, C - 74), (C - 14, C - 52)), ((C + 40, C - 60), (C + 14, C - 74), (C + 14, C - 52)),
                ((C - 36, C + 46), (C - 36, C + 76), (C - 16, C + 60)), ((C + 36, C + 46), (C + 36, C + 76), (C + 16, C + 60))):
        ln(d, list(pts), p[4], 7)


def S_scavenger(d, p):
    poly(d, bezier((C - 80, C + 84), (C - 120, C - 10), (C - 40, C - 40)) + bezier((C - 40, C - 40), (C, C - 24), (C + 40, C - 40))[1:] +
         bezier((C + 40, C - 40), (C + 120, C - 10), (C + 80, C + 84))[1:], p[3])
    poly(d, [(C - 44, C - 44), (C + 44, C - 44), (C + 30, C - 82), (C - 30, C - 82)], darken(p[3], 0.3))
    ln(d, [(C - 40, C - 50), (C + 40, C - 50)], p[4], 9)
    # three finds shining at the mouth of the sack
    base.sparkle(d, C - 28, C - 94, 20, p[4])
    ell(d, C + 18, C - 90, 12, 12, p[4])
    poly(d, [(C + 40, C - 100), (C + 62, C - 84), (C + 42, C - 70)], (255, 255, 255))
    ln(d, [(C - 40, C + 30), (C + 40, C + 30)], darken(p[3], 0.45), 5)
    ln(d, [(C - 50, C + 56), (C + 50, C + 56)], darken(p[3], 0.45), 5)


def S_kaioken(d, p):
    base.flame(d, C, C + 92, 196, 176, p[3], p[4])
    for i, y in enumerate((C + 30, C - 8, C - 46)):
        w = 56 - i * 10
        ln(d, [(C - w, y + 24), (C, y - 6), (C + w, y + 24)], (255, 255, 255), 14)
    rays(d, C, C + 20, 112, 126, 12, p[4], 6, 8)


def S_mech_eye(d, p):
    ell(d, C, C, 98, 98, None, p[3], 8)
    rays(d, C, C, 84, 108, 16, p[3], 12, 0)
    ell(d, C, C, 74, 74, darken(p[3], 0.5))
    base.eye(d, C, C, 62, 28, p[4], (6, 8, 14), darken(p[3], 0.3))
    for a in (0, 90, 180, 270):
        r = math.radians(a)
        ln(d, [(C + math.cos(r) * 76, C + math.sin(r) * 76), (C + math.cos(r) * 100, C + math.sin(r) * 100)], p[4], 6)


def gear(d, cx, cy, r, teeth, col, hole):
    pts = []
    for i in range(teeth * 4):
        a = math.radians(i * 360.0 / (teeth * 4))
        rad = r * (1.25 if (i % 4) in (0, 1) else 1.0)
        pts.append((cx + math.cos(a) * rad, cy + math.sin(a) * rad))
    poly(d, pts, col)
    ell(d, cx, cy, r * 0.45, r * 0.45, hole)


def S_mech_hands(d, p):
    poly(d, [(C - 56, C + 12), (C + 56, C + 12), (C + 56, C + 74), (C - 56, C + 74)], p[3], darken(p[3], 0.5), 6)
    for i in range(4):
        x = C - 48 + i * 32
        poly(d, [(x - 12, C + 12), (x + 12, C + 12), (x + 12, C - 78 + abs(i - 1.5) * 12), (x - 12, C - 78 + abs(i - 1.5) * 12)], lighten(p[3], 0.1), darken(p[3], 0.5), 5)
    poly(d, [(C - 86, C + 22), (C - 56, C + 12), (C - 56, C + 48), (C - 80, C + 30)], p[3], darken(p[3], 0.5), 5)
    gear(d, C, C + 100, 28, 8, p[4], (20, 20, 28))
    gear(d, C + 70, C - 78, 16, 6, p[4], (20, 20, 28))
    base.sparkle(d, C - 76, C - 70, 16, (255, 255, 255))


def S_crocodile(d, p):
    for row in range(5):
        for col in range(6):
            x = C - 100 + col * 40 + (20 if row % 2 else 0)
            y = C - 92 + row * 38
            poly(d, [(x, y - 20), (x + 20, y), (x, y + 20), (x - 20, y)], mix(p[3], (0, 0, 0), 0.18 + 0.1 * ((row + col) % 3)), darken(p[3], 0.6), 4)
    base.eye(d, C + 10, C + 2, 68, 34, p[4], (4, 10, 4), darken(p[3], 0.7))
    poly(d, [(C + 10 - 4, C - 18), (C + 10 + 4, C - 18), (C + 10 + 4, C + 22), (C + 10 - 4, C + 22)], (4, 10, 4))


def S_hecate(d, p):
    for sign in (-1, 1):  # the triple moon: a crescent on each side of the full moon, convex edges outward
        cx = C + sign * 82
        d.ellipse((cx - 48, C - 70, cx + 48, C + 34), fill=p[3] + (255,))
        d.ellipse((cx - sign * 30 - 46, C - 64, cx - sign * 30 + 46, C + 28), fill=(0, 0, 0, 0))
    ell(d, C, C - 18, 48, 48, (255, 255, 255))
    ell(d, C, C - 18, 40, 40, p[3])
    # the crossroads: three roads meeting under the moons
    for ang in (-90, 30, 150):
        r = math.radians(ang)
        ln(d, [(C, C + 72), (C + math.cos(r) * 62, C + 72 + math.sin(r) * 62)], p[4], 9)
    ell(d, C, C + 72, 10, 10, (255, 255, 255))
    sparkle(d, C, C - 88, 14, p[4])


def S_void_edge(d, p):
    poly(d, [(C + 96, C - 100), (C + 108, C - 88), (C - 56, C + 78), (C - 74, C + 60)], p[3], p[4], 5)
    ln(d, [(C + 92, C - 92), (C - 62, C + 66)], p[4], 6)
    poly(d, [(C - 82, C + 52), (C - 60, C + 74), (C - 92, C + 84)], darken(p[3], 0.2))
    poly(d, [(C - 98, C + 30), (C - 38, C + 92), (C - 46, C + 104), (C - 108, C + 42)], (14, 10, 24), p[4], 4)   # the guard
    # the void crack the blade leaves
    ln(d, [(C - 100, C - 70), (C - 40, C - 20), (C - 70, C + 10), (C - 8, C + 52)], (4, 2, 10), 14)
    ln(d, [(C - 100, C - 70), (C - 40, C - 20), (C - 70, C + 10), (C - 8, C + 52)], p[4], 4)
    for pt in ((C - 84, C - 30), (C + 8, C - 66), (C + 56, C + 20)):
        sparkle(d, pt[0], pt[1], 12, p[4])


def S_basilisk(d, p):
    poly(d, [(C, C - 106), (C + 98, C + 18), (C + 56, C + 94), (C - 56, C + 94), (C - 98, C + 18)], darken(p[3], 0.45), p[3], 7)
    for row in range(4):
        for col in range(5):
            x = C - 64 + col * 32 + (16 if row % 2 else 0)
            y = C + 6 + row * 22
            arc(d, x, y, 18, 14, 0, 180, mix(p[3], (0, 0, 0), 0.25), 5)
    for sx in (-1, 1):
        base.eye(d, C + sx * 40, C - 28, 28, 15, p[4], (4, 8, 4), darken(p[3], 0.6))
        poly(d, [(C + sx * 40 - 3, C - 42), (C + sx * 40 + 3, C - 42), (C + sx * 40 + 3, C - 14), (C + sx * 40 - 3, C - 14)], (4, 8, 4))
    poly(d, [(C - 22, C + 52), (C - 10, C + 52), (C - 16, C + 96)], (250, 250, 240))
    poly(d, [(C + 10, C + 52), (C + 22, C + 52), (C + 16, C + 96)], (250, 250, 240))
    sparkle(d, C + 96, C - 70, 14, p[4])


def S_beelzebuth(d, p):
    base.wing(d, C - 22, C - 40, 96, -1, mix(p[3], (255, 255, 255), 0.2), p[3], darken(p[3], 0.4), darken(p[3], 0.6))
    base.wing(d, C + 22, C - 40, 96, 1, mix(p[3], (255, 255, 255), 0.2), p[3], darken(p[3], 0.4), darken(p[3], 0.6))
    ell(d, C, C + 24, 62, 70, (20, 6, 14), p[4], 7)
    for i in range(10):
        a = math.radians(200 + i * 14.4)
        x0, y0 = C + math.cos(a) * 56, C + 24 + math.sin(a) * 62
        poly(d, [(x0 - 7, y0), (x0 + 7, y0), (x0 + math.cos(a + math.pi) * 2, y0 - math.sin(a) * 20 + 10)], (250, 240, 240))
    for i in range(10):
        a = math.radians(20 + i * 14.4)
        x0, y0 = C + math.cos(a) * 56, C + 24 + math.sin(a) * 62
        poly(d, [(x0 - 7, y0), (x0 + 7, y0), (x0, y0 - 20)], (250, 240, 240))
    ell(d, C, C + 24, 20, 26, p[4])
    for sx in (-1, 1):
        ell(d, C + sx * 40, C - 62, 12, 12, p[4])


def S_gaia(d, p):
    poly(d, [(C - 14, C + 30), (C + 14, C + 30), (C + 22, C + 100), (C - 22, C + 100)], (96, 64, 32))
    for pts in (((C - 8, C + 98), (C - 60, C + 104), (C - 90, C + 90)), ((C + 8, C + 98), (C + 60, C + 104), (C + 90, C + 90)), ((C, C + 100), (C, C + 112), (C - 10, C + 118))):
        ln(d, list(pts), (96, 64, 32), 10)
    for cx, cy, r in ((C - 52, C - 10, 52), (C + 52, C - 10, 52), (C, C - 56, 58), (C - 26, C + 6, 44), (C + 26, C + 6, 44)):
        ell(d, cx, cy, r, r, mix(p[3], (0, 0, 0), 0.15))
    ell(d, C, C - 56, 38, 38, p[3])
    for x, y in ((C - 70, C + 112), (C + 74, C + 110), (C - 6, C + 120)):
        poly(d, [(x, y - 14), (x + 11, y), (x, y + 12), (x - 11, y)], p[4], darken(p[4], 0.5), 3)
    sparkle(d, C, C - 60, 18, (255, 255, 255))


def S_hephaestus(d, p):
    base.flame(d, C - 40, C + 100, 150, 120, p[4], (255, 240, 180))
    poly(d, [(C - 98, C - 20), (C + 74, C - 20), (C + 98, C + 8), (C + 40, C + 8), (C + 30, C + 36), (C + 50, C + 36), (C + 50, C + 64), (C - 80, C + 64), (C - 80, C + 36), (C - 40, C + 36), (C - 50, C + 8), (C - 98, C + 8)],
         p[3], darken(p[3], 0.5), 6)
    poly(d, [(C - 98, C - 20), (C + 74, C - 20), (C + 90, C - 8), (C - 98, C - 8)], lighten(p[3], 0.4))
    ln(d, [(C + 20, C - 90), (C + 84, C - 26)], (120, 80, 40), 14)
    poly(d, [(C + 50, C - 118), (C + 108, C - 60), (C + 78, C - 34), (C + 26, C - 88)], darken(p[3], 0.2), (255, 255, 255), 4)
    sparkle(d, C - 70, C - 50, 16, (255, 240, 180))
    sparkle(d, C + 100, C + 20, 12, p[4])


def S_ouroboros(d, p):
    ell(d, C, C, 88, 88, None, p[3], 30)
    ell(d, C, C, 88, 88, None, mix(p[3], p[4], 0.5), 14)
    for i in range(14):
        a = math.radians(i * 360 / 14)
        ell(d, C + math.cos(a) * 88, C + math.sin(a) * 88, 7, 7, darken(p[3], 0.5))
    # the head (top left) closing on the tail (top right)
    poly(d, [(C - 22, C - 78), (C + 30, C - 112), (C + 34, C - 62)], p[3], darken(p[3], 0.5), 5)
    ell(d, C - 2, C - 92, 7, 7, p[4])
    poly(d, [(C + 28, C - 96), (C + 62, C - 88), (C + 40, C - 78)], (14, 8, 28))
    sparkle(d, C, C, 22, p[4])
    arc(d, C, C, 56, 56, 200, 330, p[4], 5)


def S_rapheal(d, p):
    base.book(d, C, C + 52, 92, darken(p[3], 0.5), (250, 246, 232), (40, 40, 60), True)
    for i in range(7):
        a = math.radians(-160 + i * 22)
        ln(d, [(C + math.cos(a) * 80, C - 50 + math.sin(a) * 52), (C + math.cos(a) * 112, C - 50 + math.sin(a) * 72)], p[4], 7)
    base.eye(d, C, C - 50, 64, 26, p[3], (6, 12, 30), p[4])
    ell(d, C, C - 50, 70, 34, None, p[4], 4)
    sparkle(d, C - 92, C - 92, 12, (255, 255, 255))
    sparkle(d, C + 96, C - 84, 14, p[4])


def S_void_sovereign(d, p):
    base.crown(d, C, C - 66, 68, p[3], p[4], darken(p[3], 0.5))
    for i, (rx, ry) in enumerate(((98, 30), (80, 24), (62, 18))):
        ell(d, C, C + 56, rx, ry, None, mix(p[3], p[4], i / 2.0), 7)
    ell(d, C, C + 56, 46, 14, (0, 0, 4))
    for ang in (30, 100, 170, 250, 320):
        a = math.radians(ang)
        ln(d, [(C + math.cos(a) * 112, C + 56 + math.sin(a) * 34), (C + math.cos(a) * 64, C + 56 + math.sin(a) * 19)], p[4], 5)
    sparkle(d, C + 94, C - 74, 14, (255, 255, 255))
    sparkle(d, C - 98, C - 20, 12, p[4])


SYMBOLS = {
    "gravity_well": S_gravity_well, "kamehameha": S_kamehameha, "static_chain": S_static_chain, "ancient_soul": S_ancient_soul,
    "iron_resolve": S_iron_resolve, "meditation": S_meditation, "runesmiths_insight": S_runesmith, "scavenger": S_scavenger,
    "kaioken": S_kaioken, "mechanical_eye": S_mech_eye, "mechanical_hands": S_mech_hands, "crocodile_skin": S_crocodile,
    "hecate": S_hecate, "void_edge": S_void_edge, "basilisk_skin": S_basilisk, "beelzebuth": S_beelzebuth, "gaia": S_gaia,
    "hephaestus_ultimate": S_hephaestus, "ouroboros": S_ouroboros, "rapheal": S_rapheal, "void_sovereign": S_void_sovereign,
}


# --------------------------------------------------------------------------- frame and render
def frame(img, kind, accent):
    """The base frames for unique / extra / ultimate; two more for aspectual magic (blue steel) and intrinsic (bronze)."""
    if kind in ("unique", "extra", "ultimate"):
        return base.frame(img, kind, accent)
    d = ImageDraw.Draw(img)
    if kind == "magic":
        edge, hi, lo = (90, 128, 224), (200, 224, 255), (16, 28, 80)
    else:
        edge, hi, lo = (176, 112, 56), (255, 214, 150), (70, 36, 12)
    w = 22
    d.rectangle((0, 0, S - 1, S - 1), outline=lo + (255,), width=6)
    d.rectangle((6, 6, S - 7, S - 7), outline=edge + (255,), width=w - 6)
    d.rectangle((w - 2, w - 2, S - w + 1, S - w + 1), outline=hi + (255,), width=4)
    d.rectangle((w + 2, w + 2, S - w - 3, S - w - 3), outline=lo + (255,), width=3)
    for cx, cy in ((w - 2, w - 2), (S - w + 1, w - 2), (w - 2, S - w + 1), (S - w + 1, S - w + 1)):
        if kind == "magic":
            poly(d, star(cx, cy, 18, 7, 4, -45), hi)         # a four-point star: magic
        else:
            poly(d, ngon(cx, cy, 15, 6), hi)                  # a hexagonal bolt: the body's own machinery
    return img


def render(name):
    kind, inner, outer, main, accent = PAL[name]
    pal = (kind, rgb(inner), rgb(outer), rgb(main), rgb(accent))
    bg = base.background(kind, rgb(inner), rgb(outer))
    sym = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    SYMBOLS[name](ImageDraw.Draw(sym), pal)
    alpha = sym.split()[3]
    glow = Image.new("RGBA", (S, S), rgb(accent) + (0,))
    glow.putalpha(alpha.filter(ImageFilter.GaussianBlur(16)).point(lambda v: min(255, int(v * 0.9))))
    rim = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rim.putalpha(alpha.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(1.2)).point(lambda v: min(255, int(v * 1.0))))
    img = Image.alpha_composite(bg, glow)
    img = Image.alpha_composite(img, rim)
    img = Image.alpha_composite(img, sym)
    img = frame(img, kind, rgb(accent))
    return img.resize((N, N), Image.LANCZOS)


def existing_hashes():
    out = {}
    if os.path.isdir(OUT):
        for f in os.listdir(OUT):
            if f.endswith(".png") and f[:-4] not in PAL:
                try:
                    out[hashlib.sha1(Image.open(os.path.join(OUT, f)).convert("RGBA").tobytes()).hexdigest()] = f
                except OSError:
                    pass
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    older = existing_hashes()
    seen, icons = {}, {}
    for name in PAL:
        if name not in SYMBOLS:
            raise SystemExit("no symbol drawn for " + name)
        im = render(name)
        h = hashlib.sha1(im.tobytes()).hexdigest()
        if h in seen:
            raise SystemExit("icons %s and %s are identical" % (name, seen[h]))
        if h in older:
            raise SystemExit("icon %s is identical to the older icon %s" % (name, older[h]))
        seen[h] = name
        icons[name] = im
        im.save(os.path.join(OUT, name + ".png"))
    print("wrote %d icons to %s" % (len(icons), OUT))
    if "--sheet" in sys.argv:
        os.makedirs(PREVIEW, exist_ok=True)
        cols, cell = 7, 150
        rows = (len(icons) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * cell, rows * cell), (24, 24, 28, 255))
        d = ImageDraw.Draw(sheet)
        for i, (name, im) in enumerate(icons.items()):
            x, y = (i % cols) * cell + 11, (i // cols) * cell + 4
            sheet.paste(im.resize((128, 128), Image.NEAREST), (x, y))
            d.text((x, y + 130), name[:22], fill=(220, 220, 220, 255))
        sheet.save(os.path.join(PREVIEW, "sheet.png"))
        print("sheet:", os.path.join(PREVIEW, "sheet.png"))


if __name__ == "__main__":
    main()
