"""Why: every custom skill (the Uniques, their Ultimates and the three god-tier standalones) used to show a vanilla item sprite
(enchanted_book, golden_sword ...) in the skill list. This script draws one distinct 32x32 icon per skill id, stylised to the skill
(bolt, bell, wings, eye, card, book, boot-flame ...), in the same framed, glowing style as the grimoire icons of tools/gen_skill_icons.py.

    python tools/skills/gen_skill_icons.py            # writes src/main/resources/assets/nusmp/textures/skill/<id>.png
    python tools/skills/gen_skill_icons.py --sheet    # also writes build/skill_icons/skills_sheet.png (labelled preview, not shipped)

Frame by skill type: Extra = steel, Unique = gold, Ultimate = black iron lit in the skill's colour, so the type reads at a glance.
Everything is drawn at 8x (256 px) with Pillow and reduced to 32 px. Deterministic: the same inputs always give the same PNGs.
The script refuses to finish if two icons come out identical (the owner's rule: no two skills share an icon).
"""
import hashlib
import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill")
PREVIEW = os.path.join(ROOT, "build", "skill_icons")
N = 32
S = 256
C = S // 2


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lighten(c, t=0.4):
    return mix(c, (255, 255, 255), t)


def darken(c, t=0.5):
    return mix(c, (0, 0, 0), t)


# --------------------------------------------------------------------------- drawing helpers (all on a 256 canvas)
def poly(d, pts, fill, outline=None, w=0):
    d.polygon(pts, fill=fill + (255,) if len(fill) == 3 else fill)
    if outline:
        d.line(list(pts) + [pts[0]], fill=outline + (255,), width=w, joint="curve")


def ell(d, cx, cy, rx, ry, fill=None, outline=None, w=0):
    d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=(fill + (255,)) if fill else None,
              outline=(outline + (255,)) if outline else None, width=w)


def ln(d, pts, col, w):
    d.line(pts, fill=col + (255,), width=w, joint="curve")
    r = w // 2
    for p in (pts[0], pts[-1]):
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=col + (255,))


def arc(d, cx, cy, rx, ry, a0, a1, col, w):
    d.arc((cx - rx, cy - ry, cx + rx, cy + ry), a0, a1, fill=col + (255,), width=w)


def star(cx, cy, R, r, n=5, rot=-90.0):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        rad = R if i % 2 == 0 else r
        pts.append((cx + math.cos(a) * rad, cy + math.sin(a) * rad))
    return pts


def ngon(cx, cy, R, n, rot=-90.0):
    return [(cx + math.cos(math.radians(rot + i * 360.0 / n)) * R, cy + math.sin(math.radians(rot + i * 360.0 / n)) * R)
            for i in range(n)]


def sparkle(d, cx, cy, R, col):
    poly(d, [(cx, cy - R), (cx + R * 0.22, cy - R * 0.22), (cx + R, cy), (cx + R * 0.22, cy + R * 0.22),
             (cx, cy + R), (cx - R * 0.22, cy + R * 0.22), (cx - R, cy), (cx - R * 0.22, cy - R * 0.22)], col)


def rays(d, cx, cy, r0, r1, n, col, w, rot=0.0):
    for i in range(n):
        a = math.radians(rot + i * 360.0 / n)
        ln(d, [(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * r1, cy + math.sin(a) * r1)], col, w)


def bezier(p0, p1, p2, steps=24):
    out = []
    for i in range(steps + 1):
        t = i / steps
        out.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
                    (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]))
    return out


def flame(d, cx, base, h, w, col, core):
    """A teardrop flame standing on y=base, height h, width w."""
    pts = bezier((cx - w / 2, base), (cx - w * 0.6, base - h * 0.45), (cx - w * 0.05, base - h * 0.6))
    pts += bezier((cx - w * 0.05, base - h * 0.6), (cx + w * 0.05, base - h * 0.85), (cx + w * 0.02, base - h))[1:]
    pts += bezier((cx + w * 0.02, base - h), (cx + w * 0.55, base - h * 0.6), (cx + w / 2, base))[1:]
    pts += bezier((cx + w / 2, base), (cx, base + h * 0.1), (cx - w / 2, base))[1:]
    poly(d, pts, col)
    s = 0.55
    ip = [(cx + (x - cx) * s, base - (base - y) * s * 0.85) for x, y in pts]
    poly(d, ip, core)


def bolt(d, cx, cy, sz, col, edge=None):
    pts = [(cx + sz * 0.18, cy - sz), (cx - sz * 0.5, cy + sz * 0.05), (cx - sz * 0.02, cy + sz * 0.05),
           (cx - sz * 0.2, cy + sz), (cx + sz * 0.55, cy - sz * 0.15), (cx + sz * 0.05, cy - sz * 0.15)]
    poly(d, pts, col, edge, 5 if edge else 0)


def card(d, cx, cy, w, h, rot, fill, edge, inner=None):
    pts = []
    for sx, sy in [(-1, -1), (1, -1), (1, 1), (-1, 1)]:
        x, y = sx * w / 2, sy * h / 2
        a = math.radians(rot)
        pts.append((cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)))
    poly(d, pts, fill, edge, 6)
    if inner:
        s = 0.72
        ip = [(cx + (x - cx) * s, cy + (y - cy) * s) for x, y in pts]
        d.line(ip + [ip[0]], fill=inner + (255,), width=4, joint="curve")


def bell(d, cx, cy, sz, fill, light, dark):
    top = cy - sz * 0.95
    left = bezier((cx - sz * 0.22, top + sz * 0.1), (cx - sz * 0.32, cy - sz * 0.1), (cx - sz * 0.85, cy + sz * 0.55))
    right = bezier((cx + sz * 0.85, cy + sz * 0.55), (cx + sz * 0.32, cy - sz * 0.1), (cx + sz * 0.22, top + sz * 0.1))
    pts = left + [(cx - sz * 0.95, cy + sz * 0.7), (cx + sz * 0.95, cy + sz * 0.7)] + right
    ell(d, cx, top + sz * 0.08, sz * 0.2, sz * 0.2, fill)
    poly(d, pts, fill)
    poly(d, [(cx - sz * 0.95, cy + sz * 0.7), (cx + sz * 0.95, cy + sz * 0.7), (cx + sz * 0.85, cy + sz * 0.55),
             (cx - sz * 0.85, cy + sz * 0.55)], dark)
    poly(d, [(cx - sz * 0.45, cy - sz * 0.25), (cx - sz * 0.3, cy - sz * 0.25), (cx - sz * 0.5, cy + sz * 0.45),
             (cx - sz * 0.62, cy + sz * 0.45)], light)
    ell(d, cx, cy + sz * 0.88, sz * 0.2, sz * 0.2, dark)


def eye(d, cx, cy, rx, ry, iris, pupil, lid, glow_col=None):
    top = bezier((cx - rx, cy), (cx, cy - ry * 2.0), (cx + rx, cy))
    bot = bezier((cx + rx, cy), (cx, cy + ry * 2.0), (cx - rx, cy))
    poly(d, top + bot[1:], (240, 244, 255))
    ell(d, cx, cy, ry * 0.95, ry * 0.95, iris)
    ell(d, cx, cy, ry * 0.5, ry * 0.5, pupil)
    ell(d, cx - ry * 0.3, cy - ry * 0.3, ry * 0.17, ry * 0.17, (255, 255, 255))
    ln(d, top, lid, 8)
    ln(d, bot, lid, 8)


def skull(d, cx, cy, sz, bone, dark, glow):
    ell(d, cx, cy - sz * 0.1, sz * 0.8, sz * 0.75, bone)
    poly(d, [(cx - sz * 0.45, cy + sz * 0.45), (cx + sz * 0.45, cy + sz * 0.45), (cx + sz * 0.38, cy + sz * 0.95),
             (cx - sz * 0.38, cy + sz * 0.95)], bone)
    for sx in (-1, 1):
        ell(d, cx + sx * sz * 0.33, cy - sz * 0.05, sz * 0.22, sz * 0.26, dark)
        ell(d, cx + sx * sz * 0.33, cy - sz * 0.05, sz * 0.09, sz * 0.11, glow)
    poly(d, [(cx, cy + sz * 0.15), (cx - sz * 0.1, cy + sz * 0.38), (cx + sz * 0.1, cy + sz * 0.38)], dark)
    for i in range(-2, 3):
        ln(d, [(cx + i * sz * 0.17, cy + sz * 0.62), (cx + i * sz * 0.17, cy + sz * 0.93)], dark, 6)


def boot(d, cx, cy, sz, fill, light, dark):
    """A sneaker-boot profile facing right."""
    pts = [(cx - sz * 0.5, cy - sz * 0.9), (cx + sz * 0.1, cy - sz * 0.9), (cx + sz * 0.15, cy - sz * 0.1),
           (cx + sz * 0.75, cy + sz * 0.2), (cx + sz * 0.95, cy + sz * 0.55), (cx + sz * 0.9, cy + sz * 0.75),
           (cx - sz * 0.55, cy + sz * 0.75)]
    poly(d, pts, fill)
    poly(d, [(cx - sz * 0.6, cy + sz * 0.55), (cx + sz * 0.95, cy + sz * 0.55), (cx + sz * 0.92, cy + sz * 0.85),
             (cx - sz * 0.6, cy + sz * 0.85)], dark)
    poly(d, [(cx - sz * 0.5, cy - sz * 0.9), (cx + sz * 0.1, cy - sz * 0.9), (cx + sz * 0.1, cy - sz * 0.7),
             (cx - sz * 0.5, cy - sz * 0.7)], light)
    ln(d, [(cx + sz * 0.05, cy - sz * 0.05), (cx + sz * 0.5, cy + sz * 0.3)], light, 6)


def wing(d, cx, cy, sz, flip, fill, light, dark, edge=None):
    """A feathered wing rising from (cx,cy); flip=-1 mirrors it. `edge` outlines each feather (for dark wings on a dark ground)."""
    f = flip
    feathers = [(0.0, -0.1, 1.00, -1.00), (0.12, 0.05, 1.15, -0.72), (0.2, 0.2, 1.22, -0.42), (0.28, 0.35, 1.2, -0.12),
                (0.36, 0.5, 1.05, 0.15)]
    for i, (sx, sy, ex, ey) in enumerate(feathers):
        base = (cx + f * sx * sz * 0.5, cy + sy * sz * 0.6)
        tip = (cx + f * ex * sz, cy + ey * sz)
        w = sz * 0.32
        mid1 = (base[0] + (tip[0] - base[0]) * 0.55, base[1] + (tip[1] - base[1]) * 0.55 - w * 0.5)
        mid2 = (base[0] + (tip[0] - base[0]) * 0.55, base[1] + (tip[1] - base[1]) * 0.55 + w * 0.5)
        poly(d, [base, mid1, tip, mid2], mix(fill, dark, i / 6.0), edge, 4 if edge else 0)
    ln(d, [(cx, cy + sz * 0.5), (cx + f * sz * 0.6, cy - sz * 0.5), (cx + f * sz * 1.0, cy - sz * 1.0)], light, 8)


def book(d, cx, cy, sz, cover, page, dark, open_=True):
    if open_:
        poly(d, [(cx - sz, cy - sz * 0.55), (cx, cy - sz * 0.35), (cx, cy + sz * 0.75), (cx - sz, cy + sz * 0.55)], page)
        poly(d, [(cx + sz, cy - sz * 0.55), (cx, cy - sz * 0.35), (cx, cy + sz * 0.75), (cx + sz, cy + sz * 0.55)], mix(page, dark, 0.15))
        poly(d, [(cx - sz * 1.08, cy - sz * 0.5), (cx - sz, cy - sz * 0.55), (cx - sz, cy + sz * 0.55), (cx - sz * 1.08, cy + sz * 0.62)], cover)
        poly(d, [(cx + sz * 1.08, cy - sz * 0.5), (cx + sz, cy - sz * 0.55), (cx + sz, cy + sz * 0.55), (cx + sz * 1.08, cy + sz * 0.62)], cover)
        for i in range(3):
            y = cy - sz * 0.15 + i * sz * 0.22
            ln(d, [(cx - sz * 0.8, y - sz * 0.05), (cx - sz * 0.15, y + sz * 0.05)], dark, 4)
            ln(d, [(cx + sz * 0.15, y + sz * 0.05), (cx + sz * 0.8, y - sz * 0.05)], dark, 4)
    else:
        poly(d, [(cx - sz * 0.75, cy - sz), (cx + sz * 0.75, cy - sz), (cx + sz * 0.75, cy + sz), (cx - sz * 0.75, cy + sz)], cover)
        poly(d, [(cx - sz * 0.6, cy - sz * 0.85), (cx + sz * 0.7, cy - sz * 0.85), (cx + sz * 0.7, cy + sz * 0.85), (cx - sz * 0.6, cy + sz * 0.85)], page)


def crown(d, cx, cy, sz, fill, light, dark):
    poly(d, [(cx - sz, cy + sz * 0.5), (cx - sz, cy - sz * 0.4), (cx - sz * 0.5, cy + sz * 0.05), (cx, cy - sz * 0.7),
             (cx + sz * 0.5, cy + sz * 0.05), (cx + sz, cy - sz * 0.4), (cx + sz, cy + sz * 0.5)], fill)
    poly(d, [(cx - sz, cy + sz * 0.5), (cx + sz, cy + sz * 0.5), (cx + sz, cy + sz * 0.8), (cx - sz, cy + sz * 0.8)], dark)
    for x in (-sz, 0.0, sz):
        ell(d, cx + x, cy - sz * (0.4 if x else 0.7), sz * 0.14, sz * 0.14, light)


def sword(d, cx, cy, sz, blade, hilt, edge):
    poly(d, [(cx - sz * 0.1, cy - sz), (cx + sz * 0.1, cy - sz), (cx + sz * 0.12, cy + sz * 0.45), (cx - sz * 0.12, cy + sz * 0.45)], blade)
    poly(d, [(cx - sz * 0.1, cy - sz), (cx, cy - sz * 1.2), (cx + sz * 0.1, cy - sz)], blade)
    poly(d, [(cx - sz * 0.5, cy + sz * 0.45), (cx + sz * 0.5, cy + sz * 0.45), (cx + sz * 0.5, cy + sz * 0.58), (cx - sz * 0.5, cy + sz * 0.58)], hilt)
    poly(d, [(cx - sz * 0.07, cy + sz * 0.58), (cx + sz * 0.07, cy + sz * 0.58), (cx + sz * 0.07, cy + sz * 0.95), (cx - sz * 0.07, cy + sz * 0.95)], edge)


# --------------------------------------------------------------------------- the skills
# id -> (type, inner background, outer background, main colour, accent colour)
PAL = {
    "summoner": ("unique", 0x5B3FA8, 0x120A2A, 0xB9A2FF, 0xFFE27A),
    "faith": ("unique", 0xFFE9A0, 0x4A3A10, 0xFFF2B8, 0xFFFFFF),
    "firebolt": ("extra", 0xFF8A2A, 0x2A0C04, 0xFFD84A, 0xFF4A1A),
    "argonaut": ("unique", 0xFFD35A, 0x3A2808, 0xFFD23A, 0xFFFFFF),
    "liaris_freese": ("unique", 0xB784FF, 0x1A0C34, 0xE8D4FF, 0x7CE0FF),
    "airiel": ("unique", 0x6DE0C2, 0x082A2A, 0xBFFFF0, 0x6DB8FF),
    "hestia": ("unique", 0xFF9A3A, 0x2E1206, 0xFFB347, 0xFFF1C2),
    "freya": ("unique", 0xFF7FB8, 0x35082A, 0xFFA6D2, 0xFFFFFF),
    "hephaestus": ("unique", 0xD0663A, 0x2A1008, 0xB8BEC8, 0xFF8A3A),
    "loki": ("unique", 0x6A3FC0, 0x0E0824, 0x3FD8A8, 0xE6D8FF),
    "wynn_fimbulvetr": ("extra", 0x4DB8FF, 0x061C34, 0xD6F4FF, 0x6FD0FF),
    "rea_laevateinn": ("extra", 0xFF5A1F, 0x300A04, 0xFFB040, 0xFFE38A),
    "arcs_ray": ("extra", 0xFFFFD2, 0x2A2A18, 0xFFFFFF, 0xFFE27A),
    "futsunomitama": ("extra", 0x6A3FA8, 0x080414, 0x4A2A80, 0xD6A8FF),
    "uchide_no_kozuchi": ("extra", 0xFFC93A, 0x30200A, 0xFFD24A, 0xE5532B),
    "hell_walker": ("unique", 0xC8281E, 0x1C0404, 0xE6E0D4, 0xFF7A2A),
    "hawk_eye": ("extra", 0xE5B23A, 0x2A1E08, 0xFFD060, 0x7A4A1A),
    "spirit_card": ("extra", 0x7CE0D0, 0x082A2A, 0xBFFFF4, 0xFFFFFF),
    "unbreakable_will": ("unique", 0xB8C0D0, 0x14182A, 0x8896AE, 0xFFD24A),
    "hati": ("unique", 0x8CA0C0, 0x0C1220, 0xC8D2E6, 0xFFD030),
    "devils_footprints": ("unique", 0xFF4A12, 0x2A0802, 0xE8E0D6, 0xFF9A2A),
    "book_maker": ("unique", 0xA8A8B8, 0x14141C, 0x6A6A7C, 0xC8C8D8),
    "six_eyes": ("unique", 0x4A8CFF, 0x08102E, 0x7FD0FF, 0xFFFFFF),
    "gold_experience": ("unique", 0xFFD030, 0x2E2208, 0xFFD84A, 0x57D26A),
    "vector_manipulation": ("unique", 0xC8E0FF, 0x0C1A2E, 0xE8F4FF, 0xFF5A5A),
    "lord_of_summons": ("ultimate", 0x7A3FD8, 0x0A0418, 0xC8B0FF, 0xFFD84A),
    "goddess_of_faith": ("ultimate", 0xFFE08A, 0x2A1E06, 0xFFFFFF, 0xFFD24A),
    "heros_argonaut": ("ultimate", 0xFFD35A, 0x241606, 0xFFE070, 0xFFFFFF),
    "hell_conqueror": ("ultimate", 0xFF2A1E, 0x140202, 0xE6DED0, 0xFF8A2A),
    "immortal_will": ("ultimate", 0xFF7A2A, 0x1C0804, 0xFFD060, 0xFFF1C2),
    "adolla_burst": ("ultimate", 0x4AB8FF, 0x06102A, 0xEAF6FF, 0x6AD8FF),
    "all_fiction": ("ultimate", 0x8A3FB8, 0x0C0414, 0xE8D8F8, 0xFF4AA0),
    "limitless": ("ultimate", 0x6A5AFF, 0x060418, 0xEAF0FF, 0xB06AFF),
    "gold_experience_requiem": ("ultimate", 0xFFC83A, 0x1E1404, 0xFFE070, 0xFFFFFF),
    "black_wings": ("ultimate", 0x8A9CC8, 0x060A18, 0x1C2230, 0xE8F0FF),
    "instant_death": ("ultimate", 0x8A0A1E, 0x080204, 0xE8E0D8, 0xFF2A3A),
    "the_almighty": ("ultimate", 0xB81E2A, 0x0A0204, 0xFFD0D0, 0xFF3A3A),
    "the_visionary": ("ultimate", 0xFF7AE0, 0x1A0830, 0xFFE0FA, 0x7AD8FF),
}


def S_summoner(d, p):
    card(d, C - 14, C + 6, 100, 140, -14, lighten(p[3], 0.1), p[4], darken(p[3], 0.3))
    ell(d, C - 8, C + 4, 30, 30, None, darken(p[3], 0.45), 6)
    poly(d, star(C - 8, C + 4, 26, 10, 5, -90), darken(p[3], 0.45))
    card(d, C + 38, C - 22, 80, 112, 20, p[3], p[4], darken(p[3], 0.3))
    sparkle(d, C + 62, C - 70, 24, p[4])


def S_faith(d, p):
    rays(d, C, C + 6, 62, 112, 14, p[3], 9)
    ell(d, C, C - 46, 34, 12, None, p[4], 8)
    poly(d, [(C - 12, C - 12), (C + 12, C - 12), (C + 12, C + 20), (C + 40, C + 20), (C + 40, C + 44), (C + 12, C + 44),
             (C + 12, C + 90), (C - 12, C + 90), (C - 12, C + 44), (C - 40, C + 44), (C - 40, C + 20), (C - 12, C + 20)], (255, 255, 255))
    ell(d, C, C + 8, 16, 16, p[4])


def S_firebolt(d, p):
    flame(d, C + 4, C + 84, 130, 120, p[4], p[3])
    bolt(d, C - 6, C - 4, 96, (255, 255, 235), p[3])


def S_argonaut(d, p):
    bell(d, C, C - 4, 78, p[3], lighten(p[3], 0.6), darken(p[3], 0.45))
    for i, r in enumerate((98, 116)):
        arc(d, C, C + 40, r, r - 40, 200 + i * 10, 340 - i * 10, p[4], 7)


def S_liaris(d, p):
    xs = [C - 72, C - 36, C, C + 36]
    for i, x in enumerate(xs):
        h = 40 + i * 34
        poly(d, [(x - 15, C + 84), (x + 15, C + 84), (x + 15, C + 84 - h), (x - 15, C + 84 - h)], mix(p[3], p[4], i / 3.0))
    poly(d, star(C + 66, C - 62, 34, 15, 5), (255, 240, 130))
    ln(d, [(C - 80, C + 10), (C + 20, C - 40), (C + 40, C - 40)], p[4], 9)


def S_airiel(d, p):
    for i, (y, r) in enumerate(((-52, 40), (0, 54), (52, 40))):
        pts = [(C - 92 + t * 11, C + y + math.sin(t * 0.55) * (10 if i != 1 else 18)) for t in range(0, 17)]
        ln(d, pts, p[3] if i != 1 else (255, 255, 255), 14)
        ex = pts[-1]
        arc(d, ex[0], ex[1] - r * 0.4, r * 0.5, r * 0.5, 270, 90 + 360 if False else 450, p[4] if i != 1 else p[3], 11)
    poly(d, [(C + 56, C - 80), (C + 90, C - 108), (C + 78, C - 62)], lighten(p[3], 0.5))


def S_hestia(d, p):
    ell(d, C, C + 62, 74, 24, darken(p[3], 0.35))
    poly(d, [(C - 74, C + 52), (C + 74, C + 52), (C + 56, C + 88), (C - 56, C + 88)], darken(p[3], 0.1))
    flame(d, C, C + 56, 132, 100, p[3], p[4])
    ell(d, C, C + 52, 74, 18, None, p[4], 6)


def S_freya(d, p):
    pts = bezier((C, C + 84), (C - 120, C - 10), (C - 62, C - 62))
    pts += bezier((C - 62, C - 62), (C - 24, C - 90), (C, C - 36))[1:]
    pts += bezier((C, C - 36), (C + 24, C - 90), (C + 62, C - 62))[1:]
    pts += bezier((C + 62, C - 62), (C + 120, C - 10), (C, C + 84))[1:]
    poly(d, pts, p[3])
    poly(d, [(C - 40, C - 42), (C - 18, C - 52), (C - 10, C - 34), (C - 30, C - 12)], lighten(p[3], 0.6))
    sparkle(d, C + 74, C - 70, 26, (255, 255, 255))
    sparkle(d, C - 78, C + 54, 18, p[4])


def S_hephaestus(d, p):
    poly(d, [(C - 92, C - 24), (C + 70, C - 24), (C + 100, C - 50), (C + 100, C - 6), (C + 52, C + 10), (C + 40, C + 38),
             (C + 66, C + 62), (C + 66, C + 84), (C - 66, C + 84), (C - 66, C + 62), (C - 40, C + 38), (C - 44, C - 4), (C - 92, C - 4)], p[3])
    poly(d, [(C - 92, C - 24), (C + 70, C - 24), (C + 100, C - 50), (C + 90, C - 34), (C - 92, C - 34)], lighten(p[3], 0.5))
    ln(d, [(C + 54, C - 108), (C + 20, C - 40)], (160, 110, 60), 14)
    poly(d, [(C + 36, C - 128), (C + 88, C - 108), (C + 74, C - 80), (C + 24, C - 100)], (190, 196, 206))
    for a in (-30, 0, 30):
        rays(d, C - 70, C - 34, 14, 34, 1, p[4], 7, rot=a - 90)


def S_loki(d, p):
    poly(d, [(C - 60, C - 70), (C - 90, C - 112), (C - 30, C - 96)], p[4])
    poly(d, [(C + 60, C - 70), (C + 90, C - 112), (C + 30, C - 96)], p[4])
    pts = bezier((C - 74, C - 62), (C - 82, C + 70), (C, C + 92)) + bezier((C, C + 92), (C + 82, C + 70), (C + 74, C - 62))[1:]
    poly(d, pts, p[3])
    for sx in (-1, 1):
        poly(d, [(C + sx * 14, C - 12), (C + sx * 62, C - 40), (C + sx * 56, C - 6), (C + sx * 24, C + 2)], (20, 12, 40))
    arc(d, C, C + 14, 46, 38, 20, 160, (20, 12, 40), 10)
    for i in range(-2, 3):
        ln(d, [(C + i * 18, C + 40), (C + i * 18, C + 56)], (20, 12, 40), 5)


def S_wynn(d, p):
    for i in range(6):
        a = math.radians(i * 60 - 90)
        ex, ey = C + math.cos(a) * 100, C + math.sin(a) * 100
        ln(d, [(C, C), (ex, ey)], p[3], 15)
        for t in (0.55, 0.78):
            bx, by = C + math.cos(a) * 100 * t, C + math.sin(a) * 100 * t
            for sgn in (-1, 1):
                b = a + sgn * math.radians(52)
                ln(d, [(bx, by), (bx + math.cos(b) * 30, by + math.sin(b) * 30)], p[4], 10)
    poly(d, ngon(C, C, 22, 6, 0), (255, 255, 255))


def S_rea(d, p):
    for dx, h, w in ((-62, 100, 62), (62, 100, 62), (0, 142, 82)):
        flame(d, C + dx, C + 80, h, w, p[3] if dx else p[4], p[4] if dx else (255, 250, 220))
    ell(d, C, C + 82, 100, 16, None, p[4], 8)


def S_arcs(d, p):
    poly(d, [(C - 108, C + 78), (C - 92, C + 100), (C + 62, C - 52), (C + 40, C - 70)], p[3])
    ln(d, [(C - 100, C + 90), (C + 56, C - 62)], p[4], 8)
    poly(d, [(C + 34, C - 96), (C + 100, C - 96), (C + 100, C - 30)], p[3])
    sparkle(d, C + 84, C - 82, 44, (255, 255, 255))
    sparkle(d, C - 40, C - 40, 20, p[4])
    sparkle(d, C + 16, C + 52, 16, p[4])


def S_futsu(d, p):
    for i, (rx, ry) in enumerate(((104, 36), (84, 28), (62, 20))):
        arc(d, C, C, rx, ry, 0, 360, mix((214, 168, 255), (120, 70, 200), i / 2.0), 9)
    ell(d, C, C, 38, 38, (12, 6, 26))
    ell(d, C, C, 38, 38, None, (214, 168, 255), 7)
    for sx in (-70, 70):
        poly(d, [(C + sx - 14, C - 96), (C + sx + 14, C - 96), (C + sx + 14, C - 62), (C + sx + 28, C - 62), (C + sx, C - 34), (C + sx - 28, C - 62),
                 (C + sx - 14, C - 62)], p[4])
    poly(d, [(C - 14, C + 40), (C + 14, C + 40), (C + 14, C + 74), (C + 28, C + 74), (C, C + 102), (C - 28, C + 74), (C - 14, C + 74)], p[4])


def S_uchide(d, p):
    ln(d, [(C - 70, C + 96), (C + 14, C - 18)], (150, 100, 56), 20)
    poly(d, [(C - 22, C - 78), (C + 76, C - 100), (C + 100, C - 22), (C + 2, C + 6)], p[3])
    poly(d, [(C - 22, C - 78), (C + 76, C - 100), (C + 66, C - 80), (C - 14, C - 56)], lighten(p[3], 0.6))
    poly(d, [(C + 62, C - 56), (C + 72, C - 76), (C + 82, C - 56), (C + 76, C - 56), (C + 76, C - 28), (C + 68, C - 28), (C + 68, C - 56)], p[4])
    sparkle(d, C - 70, C - 56, 24, (255, 255, 255))
    sparkle(d, C + 70, C + 62, 20, p[3])


def S_hellwalker(d, p):
    for dx, h, w in ((-70, 80, 56), (70, 80, 56), (0, 120, 80)):
        flame(d, C + dx, C + 96, h, w, p[4], (255, 210, 90))
    skull(d, C, C - 8, 56, p[3], (40, 8, 6), (255, 120, 40))
    poly(d, [(C - 50, C - 56), (C - 76, C - 104), (C - 28, C - 74)], p[4])
    poly(d, [(C + 50, C - 56), (C + 76, C - 104), (C + 28, C - 74)], p[4])


def S_hawkeye(d, p):
    eye(d, C, C - 6, 100, 40, (240, 180, 40), (20, 10, 4), (60, 36, 12))
    poly(d, [(C + 10, C - 100), (C + 66, C - 70), (C + 100, C - 24), (C + 54, C - 52), (C + 30, C - 52)], p[4])
    for i in range(3):
        ln(d, [(C - 80 + i * 14, C + 70 + i * 8), (C + 40 + i * 14, C + 64 + i * 8)], p[3], 8)


def S_spirit(d, p):
    card(d, C, C, 128, 172, 0, (22, 60, 66), p[3], (60, 140, 146))
    pts = bezier((C - 34, C + 52), (C - 40, C - 70), (C, C - 62)) + bezier((C, C - 62), (C + 40, C - 70), (C + 34, C + 52))[1:]
    pts += [(C + 16, C + 36), (C, C + 56), (C - 16, C + 36)]
    poly(d, pts, (235, 255, 252))
    for sx in (-1, 1):
        ell(d, C + sx * 14, C - 22, 7, 10, (22, 60, 66))
    sparkle(d, C + 50, C - 86, 16, p[4])


def S_unbreakable(d, p):
    pts = [(C - 80, C - 84), (C + 80, C - 84), (C + 80, C + 6)] + bezier((C + 80, C + 6), (C + 70, C + 62), (C, C + 104))[1:] + \
          bezier((C, C + 104), (C - 70, C + 62), (C - 80, C + 6))[1:]
    poly(d, pts, p[3])
    poly(d, [(C - 62, C - 68), (C + 62, C - 68), (C + 62, C + 2), (C, C + 80), (C - 62, C + 2)], lighten(p[3], 0.2))
    ln(d, [(C + 6, C - 82), (C - 14, C - 30), (C + 16, C - 8), (C - 6, C + 50), (C + 4, C + 88)], p[4], 11)
    poly(d, star(C, C - 8, 22, 9, 4), (255, 245, 200))


def S_hati(d, p):
    head = [(C - 84, C - 6), (C - 78, C - 98), (C - 40, C - 52), (C + 40, C - 52), (C + 78, C - 98), (C + 84, C - 6),
            (C + 40, C + 40), (C + 16, C + 100), (C - 16, C + 100), (C - 40, C + 40)]
    poly(d, head, p[3])
    poly(d, [(C - 70, C - 14), (C - 66, C - 78), (C - 44, C - 46), (C - 40, C - 14)], mix(p[3], (60, 70, 100), 0.5))
    poly(d, [(C + 70, C - 14), (C + 66, C - 78), (C + 44, C - 46), (C + 40, C - 14)], mix(p[3], (60, 70, 100), 0.5))
    for sx in (-1, 1):
        poly(d, [(C + sx * 18, C - 14), (C + sx * 56, C - 24), (C + sx * 50, C + 4), (C + sx * 24, C + 6)], p[4])
    poly(d, [(C - 16, C + 70), (C + 16, C + 70), (C, C + 100)], (20, 24, 40))
    bolt(d, C + 60, C + 62, 36, p[4], (120, 80, 0))


def S_devils(d, p):
    flame(d, C - 24, C + 98, 86, 74, p[4], (255, 230, 120))
    flame(d, C + 46, C + 98, 60, 54, p[4], (255, 230, 120))
    boot(d, C - 6, C - 18, 76, p[3], (255, 255, 255), (70, 40, 30))


def S_bookmaker(d, p):
    book(d, C, C + 14, 90, (70, 70, 86), (226, 226, 236), (60, 60, 76))
    poly(d, [(C - 12, C - 104), (C + 12, C - 104), (C + 12, C - 52), (C - 12, C - 52)], p[4])
    poly(d, [(C - 30, C - 112), (C + 30, C - 112), (C + 30, C - 98), (C - 30, C - 98)], p[4])
    for i in range(5):
        y = C - 50 + i * 20
        poly(d, [(C - 12, y), (C + 12, y + 8), (C + 12, y + 18), (C - 12, y + 10)], mix(p[4], (90, 90, 108), 0.35 + 0.1 * (i % 2)))
    poly(d, [(C - 12, C + 50), (C, C + 78), (C + 12, C + 50)], p[4])


def S_sixeyes(d, p):
    eye(d, C, C, 108, 42, (70, 170, 255), (8, 20, 60), (200, 230, 255))
    ell(d, C, C, 14, 14, (255, 255, 255))
    for a in (-60, -30, 0, 30, 60):
        rays(d, C, C - 46, 30, 48, 1, p[3], 6, rot=a - 90)
    sparkle(d, C + 84, C - 70, 18, (255, 255, 255))


def S_goldexp(d, p):
    ell(d, C, C + 14, 70, 76, p[3])
    ln(d, [(C, C - 58), (C, C + 90)], (80, 60, 10), 8)
    for dx, dy in ((-30, -10), (30, -10), (-36, 36), (36, 36), (0, 62)):
        ell(d, C + dx, C + dy, 11, 11, (80, 60, 10))
    ell(d, C, C - 78, 34, 30, (80, 60, 10))
    ln(d, [(C - 20, C - 100), (C - 36, C - 126)], (80, 60, 10), 6)
    ln(d, [(C + 20, C - 100), (C + 36, C - 126)], (80, 60, 10), 6)
    poly(d, [(C + 56, C - 92), (C + 98, C - 100), (C + 86, C - 58)], p[4])
    ln(d, [(C + 56, C - 92), (C + 80, C - 66)], (30, 110, 40), 5)
    sparkle(d, C - 82, C + 64, 20, (255, 255, 255))


def S_vector(d, p):
    ell(d, C, C, 38, 38, p[3])
    for i in range(4):
        a = math.radians(i * 90 + 45)
        ex, ey = C + math.cos(a) * 96, C + math.sin(a) * 96
        ln(d, [(C + math.cos(a) * 44, C + math.sin(a) * 44), (ex, ey)], p[3], 12)
        b0, b1 = a + math.radians(150), a - math.radians(150)
        poly(d, [(ex + math.cos(a) * 22, ey + math.sin(a) * 22), (ex + math.cos(b0) * 26, ey + math.sin(b0) * 26),
                 (ex + math.cos(b1) * 26, ey + math.sin(b1) * 26)], p[4] if i % 2 else p[3])
    arc(d, C, C, 62, 62, 200, 340, p[4], 10)
    poly(d, [(C + 51, C - 22), (C + 74, C - 26), (C + 60, C - 6)], p[4])


def S_lord(d, p):
    crown(d, C, C - 66, 54, p[4], (255, 255, 255), darken(p[4], 0.4))
    card(d, C - 36, C + 40, 78, 108, -12, p[3], (255, 255, 255), darken(p[3], 0.3))
    card(d, C + 38, C + 38, 78, 108, 12, lighten(p[3], 0.15), (255, 255, 255), darken(p[3], 0.3))
    poly(d, star(C, C + 46, 24, 9, 5), p[4])


def S_goddess(d, p):
    wing(d, C - 16, C + 14, 100, -1, (255, 252, 240), (255, 255, 255), (214, 190, 120))
    wing(d, C + 16, C + 14, 100, 1, (255, 252, 240), (255, 255, 255), (214, 190, 120))
    ell(d, C, C - 76, 40, 13, None, p[4], 9)
    poly(d, [(C - 12, C - 30), (C + 12, C - 30), (C + 12, C + 4), (C + 36, C + 4), (C + 36, C + 24), (C + 12, C + 24),
             (C + 12, C + 80), (C - 12, C + 80), (C - 12, C + 24), (C - 36, C + 24), (C - 36, C + 4), (C - 12, C + 4)], p[4])


def S_heros(d, p):
    rays(d, C, C, 70, 118, 16, p[4], 8)
    sword(d, C, C - 8, 100, (255, 255, 255), p[3], p[3])
    bell(d, C, C + 28, 46, p[3], (255, 255, 255), darken(p[3], 0.4))
    sparkle(d, C - 84, C - 74, 20, (255, 255, 255))


def S_hellconq(d, p):
    for dx, h, w in ((-78, 100, 62), (78, 100, 62), (0, 136, 86)):
        flame(d, C + dx, C + 100, h, w, p[4], (255, 220, 100))
    skull(d, C, C + 4, 52, p[3], (40, 8, 6), (255, 140, 40))
    crown(d, C, C - 82, 46, (255, 214, 70), (255, 255, 255), (150, 100, 20))


def S_immortal(d, p):
    wing(d, C - 12, C + 8, 96, -1, p[3], (255, 240, 180), (200, 90, 30))
    wing(d, C + 12, C + 8, 96, 1, p[3], (255, 240, 180), (200, 90, 30))
    ell(d, C, C - 40, 28, 28, None, p[4], 12)
    poly(d, [(C - 12, C - 14), (C + 12, C - 14), (C + 12, C + 4), (C + 40, C + 4), (C + 40, C + 24), (C + 12, C + 24), (C + 12, C + 92),
             (C - 12, C + 92), (C - 12, C + 24), (C - 40, C + 24), (C - 40, C + 4), (C - 12, C + 4)], p[4])
    flame(d, C, C + 100, 70, 60, (255, 120, 40), (255, 230, 130))


def S_adolla(d, p):
    rays(d, C, C - 6, 80, 120, 12, p[3], 7, rot=15)
    flame(d, C - 20, C + 100, 100, 80, p[4], (255, 255, 255))
    flame(d, C + 50, C + 100, 70, 56, p[4], (255, 255, 255))
    boot(d, C - 6, C - 22, 72, (240, 248, 255), (255, 255, 255), (60, 100, 150))
    bolt(d, C + 70, C - 66, 34, (255, 255, 255), p[4])


def S_allfiction(d, p):
    book(d, C, C + 14, 88, (60, 24, 84), (240, 232, 250), (60, 40, 80))
    ln(d, [(C - 84, C - 66), (C + 84, C + 80)], p[4], 16)
    ln(d, [(C + 84, C - 66), (C - 84, C + 80)], p[4], 16)
    for i in range(7):
        x = C - 70 + i * 24
        poly(d, [(x, C - 96 - (i % 3) * 8), (x + 10, C - 108 - (i % 3) * 8), (x + 8, C - 86 - (i % 3) * 8)], (240, 232, 250))


def S_limitless(d, p):
    pts = []
    for t in range(0, 361, 6):
        a = math.radians(t)
        den = 1 + math.sin(a) ** 2
        pts.append((C + 96 * math.cos(a) / den, C + 96 * math.sin(a) * math.cos(a) / den))
    ln(d, pts, p[3], 18)
    ell(d, C - 62, C, 28, 28, (70, 120, 255))
    ell(d, C - 62, C, 12, 12, (200, 225, 255))
    ell(d, C + 62, C, 28, 28, (255, 60, 70))
    ell(d, C + 62, C, 12, 12, (255, 200, 200))
    ell(d, C, C, 24, 24, p[4])
    ell(d, C, C, 10, 10, (255, 255, 255))


def S_requiem(d, p):
    ln(d, [(C - 92, C + 92), (C + 40, C - 40)], (150, 100, 20), 16)
    poly(d, [(C + 20, C - 104), (C + 104, C - 90), (C + 90, C - 6), (C + 62, C - 34), (C + 28, C - 68)], p[3])
    for i in range(3):
        poly(d, [(C - 90 + i * 16, C + 94 - i * 16), (C - 72 + i * 16, C + 112 - i * 16), (C - 62 + i * 16, C + 90 - i * 16)], p[3])
    arc(d, C - 34, C + 42, 44, 44, 0, 360, (255, 255, 255), 9)
    ell(d, C - 34, C + 42, 16, 22, None, p[4], 7)
    sparkle(d, C + 74, C + 56, 22, (255, 255, 255))


def S_blackwings(d, p):
    wing(d, C - 10, C + 4, 110, -1, (44, 50, 76), (150, 170, 215), (14, 16, 30), edge=(170, 190, 235))
    wing(d, C + 10, C + 4, 110, 1, (44, 50, 76), (150, 170, 215), (14, 16, 30), edge=(170, 190, 235))
    poly(d, [(C - 18, C - 40), (C + 18, C - 40), (C + 12, C + 70), (C, C + 92), (C - 12, C + 70)], (232, 240, 255))
    poly(d, [(C - 4, C - 22), (C + 4, C - 22), (C + 4, C + 50), (C - 4, C + 50)], (60, 80, 140))


def S_instdeath(d, p):
    skull(d, C, C - 14, 70, p[3], (30, 4, 8), (255, 40, 60))
    ln(d, [(C - 92, C + 96), (C + 92, C + 40)], p[4], 12)
    ln(d, [(C + 92, C + 96), (C - 92, C + 40)], p[4], 12)
    ell(d, C, C - 14, 100, 100, None, (255, 40, 60), 6)


def S_almighty(d, p):
    poly(d, ngon(C, C, 104, 8, -90), (30, 6, 10), p[4], 8)
    eye(d, C, C, 86, 36, (210, 20, 30), (8, 0, 2), (255, 160, 160))
    rays(d, C, C, 52, 92, 8, p[4], 7, rot=22.5)
    crown(d, C, C - 82, 34, p[3], (255, 255, 255), darken(p[3], 0.5))


def S_visionary(d, p):
    poly(d, star(C, C, 108, 36, 8, -90), p[3])
    poly(d, star(C, C, 70, 26, 8, -67.5), p[4])
    ell(d, C, C, 34, 34, (255, 255, 255))
    for ang, r, s in ((30, 94, 16), (150, 100, 14), (250, 96, 18), (330, 88, 12)):
        a = math.radians(ang)
        sparkle(d, C + math.cos(a) * r, C + math.sin(a) * r, s, (255, 255, 255))


SYMBOLS = {
    "summoner": S_summoner, "faith": S_faith, "firebolt": S_firebolt, "argonaut": S_argonaut, "liaris_freese": S_liaris,
    "airiel": S_airiel, "hestia": S_hestia, "freya": S_freya, "hephaestus": S_hephaestus, "loki": S_loki,
    "wynn_fimbulvetr": S_wynn, "rea_laevateinn": S_rea, "arcs_ray": S_arcs, "futsunomitama": S_futsu,
    "uchide_no_kozuchi": S_uchide, "hell_walker": S_hellwalker, "hawk_eye": S_hawkeye, "spirit_card": S_spirit,
    "unbreakable_will": S_unbreakable, "hati": S_hati, "devils_footprints": S_devils, "book_maker": S_bookmaker,
    "six_eyes": S_sixeyes, "gold_experience": S_goldexp, "vector_manipulation": S_vector, "lord_of_summons": S_lord,
    "goddess_of_faith": S_goddess, "heros_argonaut": S_heros, "hell_conqueror": S_hellconq, "immortal_will": S_immortal,
    "adolla_burst": S_adolla, "all_fiction": S_allfiction, "limitless": S_limitless, "gold_experience_requiem": S_requiem,
    "black_wings": S_blackwings, "instant_death": S_instdeath, "the_almighty": S_almighty, "the_visionary": S_visionary,
}


# --------------------------------------------------------------------------- frame, background, glow
def background(kind, inner, outer):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    px = img.load()
    for y in range(S):
        for x in range(S):
            r = math.hypot(x - C, y - C) / (S * 0.62)
            t = min(1.0, r)
            col = mix(inner, outer, t ** 0.8)
            col = mix(col, (0, 0, 0), 0.25 * t)
            px[x, y] = col + (255,)
    return img


def frame(img, kind, accent):
    d = ImageDraw.Draw(img)
    if kind == "unique":
        base, hi, lo = (232, 188, 58), (255, 240, 150), (120, 80, 12)
    elif kind == "extra":
        base, hi, lo = (150, 162, 182), (226, 234, 248), (60, 70, 90)
    else:
        base, hi, lo = darken(accent, 0.55), lighten(accent, 0.35), (10, 8, 16)
    w = 22
    d.rectangle((0, 0, S - 1, S - 1), outline=lo + (255,), width=6)
    d.rectangle((6, 6, S - 7, S - 7), outline=base + (255,), width=w - 6)
    d.rectangle((w - 2, w - 2, S - w + 1, S - w + 1), outline=hi + (255,), width=4)
    d.rectangle((w + 2, w + 2, S - w - 3, S - w - 3), outline=lo + (255,), width=3)
    for cx, cy in ((w - 2, w - 2), (S - w + 1, w - 2), (w - 2, S - w + 1), (S - w + 1, S - w + 1)):
        if kind == "ultimate":
            poly(d, star(cx, cy, 20, 8, 4, -90), hi)
        else:
            poly(d, [(cx, cy - 17), (cx + 17, cy + 13), (cx - 17, cy + 13)], hi)
    return img


def render(name):
    kind, inner, outer, main, accent = PAL[name]
    pal = (kind, rgb(inner), rgb(outer), rgb(main), rgb(accent))
    bg = background(kind, rgb(inner), rgb(outer))
    sym = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    SYMBOLS[name](ImageDraw.Draw(sym), pal)
    # soft glow of the symbol in the skill's colour, then a dark rim so it reads at 32 px
    alpha = sym.split()[3]
    glow = Image.new("RGBA", (S, S), rgb(accent) + (0,))
    glow.putalpha(alpha.filter(ImageFilter.GaussianBlur(16)).point(lambda v: min(255, int(v * 0.9))))
    rim = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rim.putalpha(alpha.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(1.2)).point(lambda v: min(255, int(v * 1.0))))
    img = Image.alpha_composite(bg, glow)
    img = Image.alpha_composite(img, rim)
    img = Image.alpha_composite(img, sym)
    img = frame(img, kind, rgb(accent))
    small = img.resize((N, N), Image.LANCZOS)
    return small


def main():
    os.makedirs(OUT, exist_ok=True)
    seen = {}
    icons = {}
    for name in PAL:
        if name not in SYMBOLS:
            raise SystemExit("no symbol drawn for " + name)
        im = render(name)
        h = hashlib.sha1(im.tobytes()).hexdigest()
        if h in seen:
            raise SystemExit("icons %s and %s are identical" % (name, seen[h]))
        seen[h] = name
        icons[name] = im
        im.save(os.path.join(OUT, name + ".png"))
    print("wrote %d icons to %s" % (len(icons), OUT))
    if "--sheet" in sys.argv:
        os.makedirs(PREVIEW, exist_ok=True)
        cols = 8
        rows = (len(icons) + cols - 1) // cols
        cell = 150
        sheet = Image.new("RGBA", (cols * cell, rows * cell), (24, 24, 28, 255))
        d = ImageDraw.Draw(sheet)
        for i, (name, im) in enumerate(icons.items()):
            x, y = (i % cols) * cell + 11, (i // cols) * cell + 4
            sheet.paste(im.resize((128, 128), Image.NEAREST), (x, y))
            d.text((x, y + 130), name[:22], fill=(220, 220, 220, 255))
        sheet.save(os.path.join(PREVIEW, "skills_sheet.png"))
        print("sheet:", os.path.join(PREVIEW, "skills_sheet.png"))


if __name__ == "__main__":
    main()
