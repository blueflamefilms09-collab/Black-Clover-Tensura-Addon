"""Preview of a merged magic, offline: what the merge generator would make of two to four magics.

    python tools/magic/merge_preview.py flame water
    python tools/magic/merge_preview.py bronze flame water --name "Forge Tide" --png build/merge_preview.png
    python tools/magic/merge_preview.py --selftest

It reads the magics from the Java sources (MagicType.java for names, soul types and hit effects; BookPalette.java, WikiBooks.java and
GrimoireDesignGenerator for colours) and ports the pure merge code (core/magic/merge: MergedKeys, MergeNaming, MergeCheck, ColorBlend,
MergedBlend, MergedModelPlan), so what it prints is what the game derives: the id, the name, the soul type, the two hit effects, the shared
palette and weights, the colour each parent's effect is drawn in, the strands and rings of the composite effects, and the floating emblem's
pieces. --png writes a picture of the palette and a top-down sketch of the field, the burst and the emblem.

The colours are the grimoire palette (the game prefers the colour of the magic's own book where it has one: close, not identical, for the
magics whose book uses another colour). The numbers of the pure code (id key, mix, cycle, emblem plan) are checked against Java by --selftest.
"""
import argparse
import math
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")

RESTRICTED = ["ANTI_MAGIC", "KOTODAMA", "DEMON_BEAST", "CURSE", "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT", "DEMON_WATER"]
MAX_PARENTS, MIN_PARENTS, MAX_NAME, MIN_NAME = 4, 2, 32, 3
COST_FACTOR = 1.15
MAX_PIECES = 9
GEO = {"BRONZE": [("bronze", "crest"), ("bronze", "pauldron_r")], "COPPER": [("copper", "crown"), ("copper", "pauldrons")],
       "CORUNDUM": [("corundum", "crown"), ("corundum", "gem_shard")], "KEY": [("key", "mini_key")], "BEAST": [("beast", "form_head")]}


# ---------------------------------------------------------------- the game's data, read from the sources
def read(path):
    with open(os.path.join(JAVA, path), encoding="utf-8") as f:
        return f.read()


def magics():
    """name -> (display name, word, soul, hit), in enum order."""
    src = read("blackclover/MagicType.java")
    out = {}
    for m in re.finditer(r'^\s{4}([A-Z_]+)\("([^"]+)",\s*"([^"]+)",\s*"([A-Z]+)",\s*HitEffect\.([A-Z]+)', src, re.M):
        out[m.group(1)] = (m.group(2), m.group(3), m.group(4), m.group(5))
    return out


def section(src, start, end):
    a = src.index(start)
    b = src.index(end, a) if end else len(src)
    return src[a:b]


def glow_table():
    src = section(read("grimoire/BookPalette.java"), "public static int glow", "/** Emblem colour")
    return {m.group(1): int(m.group(2), 16) for m in re.finditer(r'case "([A-Z_]+)" -> 0x([0-9A-Fa-f]{6})', src)}


def wiki_table():
    src = read("book/WikiBooks.java")
    body = section(src, "public static int color(MagicType magic)", "public static MagicType magicForColor")
    return {m.group(1): int(m.group(2), 16) for m in re.finditer(r'case ([A-Z_]+) -> 0xFF([0-9A-Fa-f]{6})', body)}


M64 = (1 << 64) - 1


def _fnv(s):
    h = 0xcbf29ce484222325
    for ch in s:
        h ^= ord(ch)
        h = (h * 0x100000001b3) & M64
    h ^= h >> 29
    h = (h * 0xbf58476d1ce4e5b9) & M64
    h ^= h >> 32
    return h & 0x7FFFFFFFFFFFFFFF


def _unit(h, k):
    z = (h + k * 0x9E3779B97F4A7C15) & M64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & M64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & M64
    z ^= z >> 31
    return (z >> 11) / float(1 << 53)


def _hsl(h, s, l):
    c = (1 - abs(2 * l - 1)) * s
    x = c * (1 - abs((h * 6) % 2 - 1))
    m = l - c / 2
    sector = int(h * 6) % 6
    r, g, b = [(c, x, 0), (x, c, 0), (0, c, x), (0, x, c), (x, 0, c), (c, 0, x)][sector]
    return (round((r + m) * 255) << 16) | (round((g + m) * 255) << 8) | round((b + m) * 255)


def generated_glow(name):
    h = _fnv(name)
    hue = _unit(h, 1)
    return _hsl((hue + 0.02) % 1.0, 0.70 + _unit(h, 6) * 0.28, 0.52 + _unit(h, 7) * 0.16)


def colour_of(name, glow, wiki):
    if name in wiki:
        return 0xFF000000 | wiki[name]
    if name in glow:
        return 0xFF000000 | glow[name]
    return 0xFF000000 | generated_glow(name)


# ---------------------------------------------------------------- ports of the pure merge code
def i32(v):
    v &= 0xFFFFFFFF
    return v - (1 << 32) if v & 0x80000000 else v


def enum_key(s):
    return (s or "").strip().upper().replace(" ", "_").replace("-", "_")


def canonical_parents(raw):
    return sorted({enum_key(r) for r in raw if enum_key(r)})


def merged_id(parents):
    return "merged:" + "+".join(p.lower() for p in parents)


def key_hash(mid):
    h = i32(0x811C9DC5)
    for ch in mid.strip().lower():
        h = i32(h ^ ord(ch))
        h = i32(h * 0x01000193)
    return 1 if h == 0 else h


def prettify(n):
    return " ".join(w.capitalize() for w in n.split("_"))


def auto_name(parents, names):
    def part(p, first):
        w = names.get(p, prettify(p))
        if w.endswith(" Magic"):
            w = w[:-6]
        return w.split()[0] if first else w
    full = "-".join(part(p, False) for p in parents)
    if len(full) + 6 > 40:
        full = "-".join(part(p, True) for p in parents)
    return (full or "Merged") + " Magic"


def clean_name(raw):
    if raw is None:
        return None
    out, space = [], False
    for ch in raw:
        if ch in "|,+:§" or ord(ch) < 32:
            continue
        if ch.isspace():
            space = len(out) > 0
            continue
        if not (ch.isalnum() or ch in "-'."):
            return None
        if space:
            out.append(" ")
            space = False
        out.append(ch)
    s = "".join(out)
    return None if len(s) < MIN_NAME or len(s) > MAX_NAME else s


def weights(n):
    return {1: [1.0], 2: [0.55, 0.45], 3: [0.40, 0.33, 0.27], 4: [0.34, 0.26, 0.22, 0.18]}.get(n, [1.0 / max(1, n)] * max(1, n))


def f32(x):
    import struct
    return struct.unpack("f", struct.pack("f", x))[0]


def lerp_c(a, b, t):
    k = min(max(t, 0.0), 1.0)
    ch = lambda c, s: (c >> s) & 255
    p = lambda av, bv: int(math.floor(f32(av + (bv - av) * f32(k)) + 0.5))
    cl = lambda v: min(max(v, 0), 255)
    return (cl(p(ch(a, 24), ch(b, 24))) << 24) | (cl(p(ch(a, 16), ch(b, 16))) << 16) | (cl(p(ch(a, 8), ch(b, 8))) << 8) | cl(p(ch(a, 0), ch(b, 0)))


def mix(colors, w):
    r = g = b = s = 0.0
    for i, c in enumerate(colors):
        wi = max(0.0, w[i]) if i < len(w) else 1.0
        r += ((c >> 16) & 255) * wi
        g += ((c >> 8) & 255) * wi
        b += (c & 255) * wi
        s += wi
    r, g, b = r / s, g / s, b / s
    if len(colors) > 1:
        luma = 0.299 * r + 0.587 * g + 0.114 * b
        lift = 1.18
        r, g, b = luma + (r - luma) * lift, luma + (g - luma) * lift, luma + (b - luma) * lift
    cl = lambda v: min(max(int(math.floor(v + 0.5)), 0), 255)
    return 0xFF000000 | (cl(r) << 16) | (cl(g) << 8) | cl(b)


def cycle(colors, phase):
    if len(colors) == 1:
        return colors[0] | 0xFF000000
    p = phase - math.floor(phase)
    x = p * len(colors)
    i = min(int(x), len(colors) - 1)
    return lerp_c(colors[i], colors[(i + 1) % len(colors)], x - i) | 0xFF000000


def size_for(weight, n):
    return 0.75 + 0.5 * min(1.0, weight * max(1, n) / 2.0)


def jitter(key, i):
    h = i32(key * i32(0x9E3779B1) + i * i32(0x85EBCA6B))
    h = i32(h ^ ((h & 0xFFFFFFFF) >> 15))
    h = i32(h * 0x2C1B3C6D)
    h = i32(h ^ ((h & 0xFFFFFFFF) >> 12))
    return ((h & 0xFFFF) / 32768.0) - 1.0


def plan(parents, key):
    n = len(parents)
    pool = []
    for rnd in range(3):
        for p in range(n):
            geo = GEO.get(parents[p], [])
            if rnd < len(geo) and rnd < 2:
                pool.append(("GEO", p, geo[rnd][0], geo[rnd][1]))
            else:
                pool.append(("CRYSTAL", p, None, None))
    count = min(MAX_PIECES - 1, len(pool))
    out = []
    for k in range(count):
        kind, p, gk, gn = pool[k]
        base = 360.0 * k / count
        geo = kind == "GEO"
        ring = 0.62 + 0.10 * (k % 2) + 0.05 * jitter(key, k)
        height = 1.05 + 0.55 * (((k * 5) % count) / float(max(1, count - 1))) + 0.06 * jitter(key, k + 31)
        scale = 0.34 + 0.06 * jitter(key, k + 7) if geo else 0.34 + 0.10 * (1 + jitter(key, k + 11))
        spin = (1 if k % 2 == 0 else -1) * (24 + 14 * (1 + jitter(key, k + 3)))
        lean = 14 + 10 * (1 + jitter(key, k + 19))
        out.append((kind, p, gk, gn, base + 14 * jitter(key, k + 5), ring, height, scale, spin, lean))
    out.append(("GEM", -1, None, None, 0.0, 0.36, 1.30, 0.55, 40.0, 0.0))
    return out


# ---------------------------------------------------------------- the merge
def merge(raw, custom, table, glow, wiki):
    if len(raw) < MIN_PARENTS:
        return None, "needs at least two magics"
    if len(raw) > MAX_PARENTS:
        return None, "at most four magics"
    canon = canonical_parents(raw)
    if len(canon) != len(raw):
        return None, "the same magic twice"
    for p in canon:
        if p not in table:
            return None, "unknown magic: " + p
        if p in RESTRICTED:
            return None, "restricted magic (devil, command or admin only): " + p
    names = {p: table[p][0] for p in canon}
    name = auto_name(canon, names) if not custom else clean_name(custom)
    if name is None:
        return None, "bad name (3-32 letters, digits, spaces, - ' .)"
    mid = merged_id(canon)
    colours = [colour_of(p, glow, wiki) for p in canon]
    w = weights(len(canon))
    hit1 = table[canon[0]][3]
    hit2, hit2_parent = None, -1
    for i in range(1, len(canon)):
        if table[canon[i]][3] != hit1:
            hit2, hit2_parent = table[canon[i]][3], i
            break
    return {"id": mid, "name": name, "parents": canon, "key": key_hash(mid), "soul": table[canon[0]][2], "hit1": hit1, "hit2": hit2,
            "hit2_parent": hit2_parent, "colours": colours, "weights": w, "primary": mix(colours, w)}, None


def tinted(m, i):
    return lerp_c(m["colours"][i % len(m["colours"])], m["primary"], 0.35) | 0xFF000000


def hexc(c):
    return "#%06X" % (c & 0xFFFFFF)


def report(m, table):
    n = len(m["parents"])
    print("%s  (%s)" % (m["name"], m["id"]))
    print("  key        : %d" % m["key"])
    print("  parents    : %s   (dominant: %s)" % (", ".join(prettify(p) for p in m["parents"]), prettify(m["parents"][0])))
    print("  soul type  : %s (the dominant parent's)" % m["soul"])
    print("  hit effects: %s%s" % (m["hit1"], (" + " + m["hit2"] + " (rides along, from " + prettify(m["parents"][m["hit2_parent"]]) + ")") if m["hit2"] else " (all parents hit the same way)"))
    print("  palette    : " + "  ".join("%s %s w=%.2f size x%.2f" % (prettify(p), hexc(c), w, size_for(w, n)) for p, c, w in zip(m["parents"], m["colours"], m["weights"])))
    print("  blended    : %s   effect tints (35%% toward the blend): %s" % (hexc(m["primary"]), "  ".join(hexc(tinted(m, i)) for i in range(n))))
    print("  cycle      : " + " ".join(hexc(cycle(m["colours"], k / 8.0)) for k in range(8)))
    print("  composite  : cast = %d strands (one per parent) round a blended core; field = %d counter-turning rings + blended circle;" % (n, n))
    print("               burst = a ring changing colour parent to parent + %d sparks; aura = %d stacked circles" % (5 * n, n))
    print("  cost       : the dearest parent's page of the same rank x %.2f, cooldown and mastery need alike; %d pages per parent" % (COST_FACTOR, {2: 4, 3: 3, 4: 2}[n]))
    print("  emblem     :")
    for kind, p, gk, gn, ang, rad, hgt, sc, spin, lean in plan(m["parents"], m["key"]):
        who = "fusion" if p < 0 else prettify(m["parents"][p])
        what = "%s_%s" % (gk, gn) if kind == "GEO" else kind.lower()
        print("    %-9s %-8s %-20s angle %6.1f  r %.2f  h %.2f  scale %.2f  spin %6.1f" % (kind, who, what, ang, rad, hgt, sc, spin))


def png(m, path):
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow is not installed: no picture")
        return
    W, H = 900, 330
    img = Image.new("RGB", (W, H), (12, 14, 28))
    d = ImageDraw.Draw(img)
    cols = m["colours"]
    n = len(cols)
    rgb = lambda c: ((c >> 16) & 255, (c >> 8) & 255, c & 255)
    for k in range(W - 20):                                              # the palette walked round once, and the blend under it
        d.line([(10 + k, 10), (10 + k, 40)], fill=rgb(cycle(cols, k / float(W - 20))))
    d.rectangle([10, 46, 10 + (W - 20), 62], fill=rgb(m["primary"]))
    cx, cy, R = 150, 200, 100                                            # field: one ring per parent
    d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=rgb(m["primary"]), width=3)
    for i in range(n):
        r = R * (0.45 + (0.5 * i / (n - 1) if n > 1 else 0.5)) * size_for(m["weights"][i], n) * 0.9
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=rgb(cols[i]), width=4)
    cx = 450                                                             # burst: sparks alternating the palette
    d.ellipse([cx - 30, cy - 30, cx + 30, cy + 30], outline=rgb(cycle(cols, 0.4)), width=3)
    for i in range(5 * n):
        a = 2 * math.pi * i / (5 * n)
        r = 55 + 45 * ((i * 7) % 5) / 4.0
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=rgb(cols[i % n]))
    cx = 750                                                             # emblem: top-down ring of the pieces
    d.ellipse([cx - 62, cy - 62, cx + 62, cy + 62], outline=(60, 60, 90))
    for kind, p, gk, gn, ang, rad, hgt, sc, spin, lean in plan(m["parents"], m["key"]):
        a = math.radians(ang)
        x, y = cx + math.sin(a) * rad * 100, cy + math.cos(a) * rad * 100
        col = rgb(m["primary"]) if p < 0 else rgb(cols[p])
        s = 6 + sc * 22
        if kind == "GEO":
            d.rectangle([x - s, y - s, x + s, y + s], outline=col, width=3)
        else:
            d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], fill=col)
    d.text((10, 300), "%s   %s   soul %s   hit %s%s" % (m["name"], m["id"], m["soul"], m["hit1"], "+" + m["hit2"] if m["hit2"] else ""), fill=(220, 220, 240))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    img.save(path)
    print("wrote", path)


def selftest():
    """Numbers computed by the Java code (MergeCoreTest / the golden run in docs/magic_merge.md)."""
    m, err = merge(["water", "flame", "bronze"], None, magics(), glow_table(), wiki_table())
    assert err is None, err
    assert m["id"] == "merged:bronze+flame+water", m["id"]
    assert m["key"] == 1726084477, m["key"]
    assert m["name"] == "Bronze-Flame-Water Magic", m["name"]
    c = [0xFFFF4A1F, 0xFF3F8CFF, 0xFFB09A4A]
    assert mix(c, weights(3)) == 0xFFB17372, hex(mix(c, weights(3)))
    assert mix(c[:2], weights(2)) == 0xFFB06485, hex(mix(c[:2], weights(2)))
    for p, want in ((0.0, 0xFFFF4A1F), (0.1, 0xFFC55E62), (0.37, 0xFF4B8EEB), (0.5, 0xFF7893A5), (0.9, 0xFFE7622C)):
        assert cycle(c, p) == want, (p, hex(cycle(c, p)), hex(want))
    pl = plan(m["parents"], m["key"])
    assert len(pl) == 9 and pl[0][0] == "GEO" and pl[0][2:4] == ("bronze", "crest"), pl[0]
    assert abs(pl[0][4] - 9.525879) < 1e-3 and abs(pl[1][5] - 0.6760547) < 1e-4 and abs(pl[3][6] - 1.5758318) < 1e-4, (pl[0][4], pl[1][5], pl[3][6])
    assert clean_name("  Storm   Born ") == "Storm Born" and clean_name("no") is None
    assert merged_id(canonical_parents(["Water", "flame"])) == "merged:flame+water"
    bad, why = merge(["flame", "anti_magic"], None, magics(), glow_table(), wiki_table())
    assert bad is None and "restricted" in why, why
    print("selftest ok")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("magics", nargs="*", help="two to four magic names (enum names, any case)")
    ap.add_argument("--name", help="a custom name for the merged magic")
    ap.add_argument("--png", help="write a picture here")
    ap.add_argument("--selftest", action="store_true", help="check the port against the numbers of the Java code")
    a = ap.parse_args()
    if a.selftest:
        selftest()
        return 0
    table, glow, wiki = magics(), glow_table(), wiki_table()
    if not a.magics:
        ap.print_usage()
        return 2
    m, err = merge(a.magics, a.name, table, glow, wiki)
    if err:
        print("refused:", err)
        return 1
    report(m, table)
    if a.png:
        png(m, a.png)
    return 0


if __name__ == "__main__":
    sys.exit(main())
