#!/usr/bin/env python3
"""NUSMP magic rune generator.
Makes a rune ring (512x512) and rune band (512x32, tileable) texture for every magic type.
Output goes to assets/nusmp/textures/particle/ names: <id>_rune_ring.png / <id>_rune_band.png
Usage: python3 rune_gen.py [out_dir] [--only id,id]   (needs Pillow)
Edit MAGICS below to add types or recolor. Same id -> same runes every time (seeded).
"""
import math, random, sys, os, zlib
from PIL import Image, ImageDraw, ImageFilter

# id: (primary, secondary, style, glyph_stroke_mode, symbol)
# style = ring decoration, mode = how glyph strokes are drawn (angular / curved / mixed)
MAGICS = {
 "gel":            ((120,255,170),(40,200,120),  "drops",   "curved",  "blob"),
 "ice":            ((170,235,255),(240,255,255), "frost",   "angular", "snow"),
 "demon_ice":      ((120,190,255),(210,60,120),  "frost",   "angular", "horn"),
 "ice_wedge":      ((150,220,255),(255,255,255), "frost",   "angular", "wedge"),
 "bubble":         ((150,230,255),(255,190,255), "drops",   "curved",  "bubble"),
 "glass":          ((210,240,255),(255,255,255), "facets",  "angular", "shard"),
 "crystal":        ((190,150,255),(120,255,240), "facets",  "angular", "gem"),
 "demon_fire":     ((255,110,40),(255,40,60),    "flames",  "angular", "horn"),
 "demon_water":    ((60,140,255),(30,60,170),    "drops",   "curved",  "horn"),
 "demon_light":    ((255,245,170),(255,200,90),  "rays",    "angular", "horn"),
 "demon_beast":    ((255,170,60),(160,60,30),    "claws",   "mixed",   "paw"),
 "curse":          ((170,60,230),(40,10,60),     "thorns",  "angular", "skull"),
 "curse_warding":  ((200,150,255),(255,255,255), "facets",  "mixed",   "ward"),
 "barrier":        ((120,220,255),(255,255,255), "hex",     "angular", "hex"),
 "key":            ((255,215,90),(255,255,220),  "facets",  "mixed",   "key"),
 "chain":          ((190,195,210),(90,95,110),   "links",   "mixed",   "link"),
 "butoh":          ((255,70,120),(255,200,220),  "petals",  "curved",  "mask"),
 "cherry_blossom": ((255,170,210),(255,255,255), "petals",  "curved",  "blossom"),
 "corundum":       ((255,60,90),(70,110,255),    "facets",  "angular", "gem"),
 "bronze":         ((205,140,70),(250,200,120),  "gear",    "mixed",   "gear"),
 "copper":         ((230,120,70),(80,220,180),   "gear",    "mixed",   "coil"),
 "iron":           ((170,180,195),(90,100,115),  "gear",    "angular", "anvil"),
 "mercury":        ((215,225,240),(130,150,200), "drops",   "curved",  "drop"),
 "food":           ((255,190,90),(150,230,90),   "petals",  "curved",  "leaf"),
 "fungus":         ((190,230,90),(150,90,200),   "spores",  "curved",  "cap"),
 "black_oil":      ((90,60,120),(10,10,20),      "drops",   "curved",  "drop"),
 "briar":          ((90,220,110),(40,120,60),    "thorns",  "curved",  "vine"),
 "eye":            ((255,230,120),(255,120,60),  "rays",    "mixed",   "eye"),
 "eyeball":        ((255,110,110),(255,255,255), "rays",    "curved",  "eye"),
 "body":           ((255,150,110),(255,230,200), "claws",   "mixed",   "fist"),
 "legion":         ((220,70,70),(255,200,90),    "blades",  "angular", "banner"),
 "sealing":        ((120,255,230),(255,255,255), "links",   "angular", "seal"),
 "dream":          ((200,170,255),(255,200,240), "stars",   "curved",  "moon"),
 "dark":           ((170,60,255),(20,0,40),      "thorns",  "angular", "void"),
 "time":           ((255,220,120),(255,255,255), "ticks",   "angular", "clock"),
 "spatial":        ((130,200,255),(255,255,255), "hex",     "angular", "portal"),
 "kotodama":       ((255,255,255),(255,120,120), "stars",   "mixed",   "brush"),
 "world_tree":     ((120,230,120),(255,230,150), "petals",  "curved",  "tree"),
 "star":           ((255,236,145),(132,157,255),  "stars",   "angular", "star"),
 "bone":           ((245,239,218),(173,162,188),  "claws",   "angular", "skull"),
 "blood":          ((226,28,48),(255,138,180),    "drops",   "curved",  "drop"),
 "sand":           ((220,177,105),(255,226,157),  "blades",  "mixed",   "dune"),
 "mist":           ((194,213,230),(230,246,255),  "drops",   "curved",  "moon"),
 "recombination":  ((206,139,83),(248,212,142),   "gear",    "angular", "gear"),
 "slash":          ((115,255,150),(232,255,237),  "blades",  "angular", "blade"),
}

S = 1024  # supersample canvas; downscaled to 512
C = S // 2

def rgba(c, a=255): return (c[0], c[1], c[2], a)

def pol(cx, cy, r, ang):  return (cx + r * math.cos(ang), cy + r * math.sin(ang))

def glyph(d, rng, cx, cy, size, ang, mode, col, w):
    """one random rune stroke-cluster, rotated to face outward"""
    pts = [(-1, -1), (0, -1), (1, -1), (-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1)]
    chosen = rng.sample(pts, rng.randint(3, 5))
    chosen.sort(key=lambda p: (p[1], p[0]) if rng.random() < .5 else (p[0], p[1]))
    ca, sa = math.cos(ang + math.pi / 2), math.sin(ang + math.pi / 2)
    def tr(p):
        x, y = p[0] * size * .5, p[1] * size * .5
        return (cx + x * ca - y * sa, cy + x * sa + y * ca)
    tp = [tr(p) for p in chosen]
    for a, b in zip(tp, tp[1:]):
        m = mode if mode != "mixed" else rng.choice(["angular", "curved"])
        if m == "angular":
            d.line([a, b], fill=col, width=w)
        else:
            mx, my = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
            nx, ny = -(b[1] - a[1]), (b[0] - a[0])
            k = rng.uniform(.25, .5)
            ctrl = (mx + nx * k, my + ny * k)
            prev = a
            for i in range(1, 13):
                t = i / 12
                x = (1 - t) ** 2 * a[0] + 2 * (1 - t) * t * ctrl[0] + t * t * b[0]
                y = (1 - t) ** 2 * a[1] + 2 * (1 - t) * t * ctrl[1] + t * t * b[1]
                d.line([prev, (x, y)], fill=col, width=w); prev = (x, y)
    for p in (tp[0], tp[-1]):  # little end dots
        d.ellipse([p[0] - w, p[1] - w, p[0] + w, p[1] + w], fill=col)

def ring_decor(d, style, rng, r, col, col2, w):
    n = {"thorns": 24, "flames": 20, "frost": 18, "petals": 12, "rays": 32, "claws": 8,
         "drops": 16, "facets": 12, "hex": 6, "links": 20, "gear": 16, "spores": 18,
         "stars": 14, "ticks": 60, "blades": 10}.get(style, 12)
    for i in range(n):
        a = i / n * math.tau
        x0, y0 = pol(C, C, r, a)
        if style in ("thorns", "claws", "blades"):
            h = 70 if style != "thorns" else 55
            tip = pol(C, C, r + h, a); l = pol(C, C, r, a - .05); rr = pol(C, C, r, a + .05)
            d.polygon([l, tip, rr], fill=rgba(col, 230))
        elif style == "flames":
            for k, hh in ((0, 75), (1, 45)):
                tip = pol(C, C, r + hh, a + k * .04)
                d.polygon([pol(C, C, r, a - .06), tip, pol(C, C, r, a + .06)], fill=rgba(col if k == 0 else col2, 220))
        elif style == "frost":
            for off in (-.5, 0, .5):
                tip = pol(C, C, r + 65 - abs(off) * 30, a + off * .05)
                d.line([(x0, y0), tip], fill=rgba(col), width=w)
            d.line([pol(C, C, r + 25, a - .04), pol(C, C, r + 25, a + .04)], fill=rgba(col2), width=w)
        elif style == "drops":
            pr = 16 if i % 2 else 26
            p = pol(C, C, r + 28, a)
            d.ellipse([p[0] - pr, p[1] - pr, p[0] + pr, p[1] + pr], outline=rgba(col), width=w)
            d.ellipse([p[0] - pr * .3 - pr * .3, p[1] - pr * .5, p[0], p[1] - pr * .2], fill=rgba(col2, 200))
        elif style == "petals":
            pts = []
            for t in range(0, 21):
                tt = t / 20 * math.pi
                rr = r + math.sin(tt) * 62
                pts.append(pol(C, C, rr, a + (t / 20 - .5) * .22))
            d.polygon(pts, outline=rgba(col), fill=rgba(col, 60), width=w)
        elif style == "rays":
            d.line([pol(C, C, r, a), pol(C, C, r + (60 if i % 2 else 30), a)], fill=rgba(col if i % 2 else col2), width=w)
        elif style in ("facets", "hex"):
            p = pol(C, C, r + 30, a); q = 28
            d.regular_polygon((p[0], p[1], q), 6 if style == "hex" else 4, rotation=math.degrees(a), outline=rgba(col), width=w)
        elif style == "links":
            p = pol(C, C, r + 22, a)
            d.ellipse([p[0] - 26, p[1] - 14, p[0] + 26, p[1] + 14], outline=rgba(col), width=w) if False else None
            lk = Image.new("RGBA", (80, 50), (0, 0, 0, 0)); ld = ImageDraw.Draw(lk)
            ld.rounded_rectangle([4, 8, 76, 42], radius=17, outline=rgba(col if i % 2 else col2), width=w)
            lk = lk.rotate(-math.degrees(a) + 90 + (90 if i % 2 else 0), expand=True, resample=Image.BICUBIC)
            d._image.alpha_composite(lk, (int(p[0] - lk.width / 2), int(p[1] - lk.height / 2)))
        elif style == "gear":
            tip = pol(C, C, r + 40, a)
            d.polygon([pol(C, C, r, a - .06), pol(C, C, r + 40, a - .035), pol(C, C, r + 40, a + .035), pol(C, C, r, a + .06)], fill=rgba(col, 220))
        elif style == "spores":
            for k in range(3):
                p = pol(C, C, r + 20 + k * 22, a + rng.uniform(-.05, .05)); q = 6 + k * 5
                d.ellipse([p[0] - q, p[1] - q, p[0] + q, p[1] + q], fill=rgba(col if k % 2 == 0 else col2, 210))
        elif style == "stars":
            p = pol(C, C, r + 30, a)
            d.regular_polygon((p[0], p[1], 22), 4, rotation=math.degrees(a) + 45, fill=rgba(col, 230))
            d.regular_polygon((p[0], p[1], 22), 4, rotation=math.degrees(a), fill=rgba(col2, 180))
        elif style == "ticks":
            L = 36 if i % 5 == 0 else 16
            d.line([pol(C, C, r, a), pol(C, C, r + L, a)], fill=rgba(col), width=w)

def center_sigil(d, symbol, col, col2, w, r):
    """central emblem, tuned loosely to the magic"""
    def poly(n, rad, rot=0, fill=None, outline=None, width=w):
        d.regular_polygon((C, C, rad), n, rotation=rot, fill=fill, outline=outline, width=width)
    if symbol in ("snow", "wedge", "shard", "gem", "hex", "portal", "ward", "seal", "star"):
        n = {"snow": 6, "wedge": 3, "shard": 4, "gem": 6, "hex": 6, "portal": 8, "ward": 5, "seal": 5, "star": 8}[symbol]
        poly(n, r, 0, outline=rgba(col)); poly(n, r * .72, 180 / n, outline=rgba(col2))
        for i in range(n):
            a = i / n * math.tau
            d.line([pol(C, C, r * .15, a), pol(C, C, r, a)], fill=rgba(col, 200), width=w)
    elif symbol == "dune":
        for y in (-.45, 0, .45):
            points = [(C + x * r, C + (y + .18 * math.sin(x * math.pi * 2)) * r)
                      for x in (-1, -.75, -.5, -.25, 0, .25, .5, .75, 1)]
            d.line(points, fill=rgba(col if y else col2), width=w + 2, joint="curve")
    elif symbol == "blade":
        d.polygon([(C - r * .7, C + r * .7), (C - r * .35, C - r * .1), (C + r * .75, C - r * .75),
                   (C + r * .1, C + r * .35)], outline=rgba(col), fill=rgba(col, 65))
        d.line([(C - r * .7, C + r * .7), (C + r * .75, C - r * .75)], fill=rgba(col2), width=w + 2)
    elif symbol in ("blob", "bubble", "drop", "moon", "blossom", "leaf", "cap", "vine"):
        for k in range(5 if symbol in ("blossom",) else 3):
            a = k / (5 if symbol == "blossom" else 3) * math.tau
            p = pol(C, C, r * .5, a); q = r * .42
            d.ellipse([p[0] - q, p[1] - q, p[0] + q, p[1] + q], outline=rgba(col if k % 2 == 0 else col2), width=w)
        d.ellipse([C - r * .2, C - r * .2, C + r * .2, C + r * .2], fill=rgba(col, 220))
        if symbol == "moon":
            d.ellipse([C - r * .55, C - r * .55, C + r * .55, C + r * .55], outline=rgba(col2), width=w)
    elif symbol in ("horn", "paw", "skull", "mask", "fist", "banner", "void"):
        n = 5
        poly(n, r, -90 + 36, outline=rgba(col)); poly(n, r, -90, outline=rgba(col2))
        d.line([(C - r * .5, C - r * .3), (C, C + r * .6), (C + r * .5, C - r * .3)], fill=rgba(col), width=w + 2)
        d.ellipse([C - r * .12, C - r * .12, C + r * .12, C + r * .12], fill=rgba(col2))
    elif symbol in ("gear", "coil", "anvil", "link", "key", "brush", "tree", "clock", "eye"):
        for rad, c in ((r, col), (r * .62, col2)):
            d.ellipse([C - rad, C - rad, C + rad, C + rad], outline=rgba(c), width=w)
        if symbol in ("clock", "gear", "coil"):
            for i in range(12):
                a = i / 12 * math.tau
                d.line([pol(C, C, r * .85, a), pol(C, C, r, a)], fill=rgba(col), width=w)
            d.line([(C, C), pol(C, C, r * .5, -1.0)], fill=rgba(col2), width=w + 2)
            d.line([(C, C), pol(C, C, r * .75, .7)], fill=rgba(col), width=w)
        elif symbol == "eye":
            pts = [pol(C, C + 0, r * 1.05, 0)]
            d.arc([C - r, C - r * .5, C + r, C + r * .5], 180, 360, fill=rgba(col), width=w + 1)
            d.arc([C - r, C - r * .5, C + r, C + r * .5], 0, 180, fill=rgba(col), width=w + 1)
            d.ellipse([C - r * .28, C - r * .28, C + r * .28, C + r * .28], fill=rgba(col2, 230))
        else:
            d.line([(C - r * .6, C), (C + r * .6, C)], fill=rgba(col), width=w + 2)
            d.line([(C, C - r * .6), (C, C + r * .6)], fill=rgba(col2), width=w + 2)

def glow(layer, strength=18, boost=1.0):
    blur = layer.filter(ImageFilter.GaussianBlur(strength))
    blur = Image.eval(blur, lambda v: v) if boost == 1 else blur
    out = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    out.alpha_composite(blur); out.alpha_composite(blur); out.alpha_composite(layer)
    return out

def make_ring(mid, spec):
    col, col2, style, mode, symbol = spec
    rng = random.Random(zlib.crc32(mid.encode()))
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    w = 6
    R_out, R_mid, R_in = 415, 315, 240
    for r, ww in ((R_out, w + 2), (R_out - 18, 3), (R_mid, w), (R_mid - 14, 3), (R_in, w)):
        d.ellipse([C - r, C - r, C + r, C + r], outline=rgba(col), width=ww)
    ring_decor(d, style, rng, R_out, col, col2, w)
    n_gl = rng.choice([12, 14, 16, 18])
    for i in range(n_gl):
        a = i / n_gl * math.tau
        x, y = pol(C, C, (R_out - 18 + R_mid) / 2, a)
        glyph(d, rng, x, y, 62, a, mode, rgba(col2 if i % 2 else col), w - 1)
    n_in = rng.choice([6, 8, 10])
    for i in range(n_in):
        a = i / n_in * math.tau + .1
        x, y = pol(C, C, (R_mid + R_in) / 2, a)
        glyph(d, rng, x, y, 50, a, mode, rgba(col), w - 2)
    center_sigil(d, symbol, col, col2, w, R_in - 40)
    img = glow(img, 14)
    return img.resize((512, 512), Image.LANCZOS)

def make_band(mid, spec):
    col, col2, style, mode, symbol = spec
    rng = random.Random(zlib.crc32((mid + "band").encode()))
    W, H = 2048, 128
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.line([(0, 6), (W, 6)], fill=rgba(col), width=6); d.line([(0, H - 7), (W, H - 7)], fill=rgba(col), width=6)
    n = 16
    for i in range(n):
        x = (i + .5) * W / n
        glyph(d, rng, x, H / 2, 70, -math.pi / 2 - math.pi / 2, mode, rgba(col2 if i % 2 else col), 5)
        if i % 4 == 0:
            d.regular_polygon((x + W / n / 2, H / 2, 10), 4, fill=rgba(col2))
    img = glow(img, 5)
    return img.resize((512, 32), Image.LANCZOS)

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    only = None
    for a in sys.argv[1:]:
        if a.startswith("--only"): only = a.split("=", 1)[1].split(",") if "=" in a else None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1].split(",")
    default_out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                               "assets", "nusmp", "textures", "particle", "magic_runes")
    out = args[0] if args and (not only or args[0] != ",".join(only)) else default_out
    os.makedirs(out, exist_ok=True)
    for mid, spec in MAGICS.items():
        if only and mid not in only: continue
        make_ring(mid, spec).save(f"{out}/{mid}_rune_ring.png")
        make_band(mid, spec).save(f"{out}/{mid}_rune_band.png")
        print("made", mid)

if __name__ == "__main__":
    main()
