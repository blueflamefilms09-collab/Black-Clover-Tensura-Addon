"""Preview the Engravement Codex screen without the game (nothing here is the game's own renderer, so it shows the LAYOUT, not the exact pixels).

    python3 -B tools/engraving_codex/preview_screen.py        ->  build/engraving_codex/screen_*.png and docs/previews/engraving_codex/screen_*.png

ScreenPreview.java builds the real book from the shipped data with the real pure classes (CodexEntries, CodexFacts, CodexLayout, CodexFilter), measures text with the glyph widths of
Minecraft's default font, filters and paginates, and dumps the pages as JSON. This script draws them with the same geometry and colours as client/engraving/EngravingCodexScreen
(a dark glass frame in the relic tooltip's palette, the contents strip with the search well, two pages), using Pillow's default font for the glyphs. It also draws one frame of the page turn.
Needs gson on the class path: set GSON_JAR, or have gson in ~/.gradle or ~/.m2.
"""
import glob, json, os, shutil, subprocess, sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "build", "engraving_codex")
DOCS = os.path.join(ROOT, "docs", "previews", "engraving_codex")
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
S = 2                                           # pixels per GUI pixel in the preview
SW = 132                                        # the contents strip's width (STRIP_W of the screen)

CYAN, GOLD, TEXT, DIM, FAINT, WHITE = 0x5CD8FF, 0xE8C468, 0xE6F2FF, 0x8FA3BF, 0x5C6E88, 0xFFFFFF
SLATE, FOOT, GREEN = 0x8899AA, 0x777788, 0x7CE08A


def gson():
    if os.environ.get("GSON_JAR"):
        return os.environ["GSON_JAR"]
    for pat in ("~/.gradle/**/gson-*.jar", "~/.m2/**/gson-*.jar"):
        found = sorted(glob.glob(os.path.expanduser(pat), recursive=True))
        if found:
            return found[0]
    sys.exit("gson jar not found: set GSON_JAR")


def build_and_run():
    cls = os.path.join(OUT, "screen_classes")
    shutil.rmtree(cls, ignore_errors=True)
    os.makedirs(cls)
    g = gson()
    files = [os.path.join(SRC, "engraving", "codex", n + ".java") for n in ("CodexEntries", "CodexFacts", "CodexFilter", "CodexLayout", "CodexNames", "TensuraNotes")]
    files += [os.path.join(SRC, "franchise", n + ".java") for n in ("FranchiseData", "FranchiseMap", "FranchiseRules")]
    files.append(os.path.join(HERE, "ScreenPreview.java"))
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    subprocess.run(["javac", "-nowarn", "-cp", g, "-d", cls] + files, check=True, env=env)
    out = os.path.join(OUT, "screen.json")
    subprocess.run(["java", "-cp", cls + os.pathsep + g + os.pathsep + os.path.join(ROOT, "src", "main", "resources"), "ScreenPreview", out], check=True, env=env, cwd=ROOT)
    return json.load(open(out))


def cw(c):
    if c == " ":
        return 4
    if c in "i!.,:;|":
        return 2
    if c == "'":
        return 3
    if c == "l":
        return 3
    if c in "I[]t":
        return 4
    if c == '"':
        return 4
    if c in "(){}fk<>*":
        return 5
    if c in "@~":
        return 7
    return 6


FONT = ImageFont.load_default(size=int(8 * S * 1.12))


def rgb(v, a=255):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, a)


class Canvas:
    def __init__(self, w, h, transparent=False):
        self.im = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0 if transparent else 255))
        self.d = ImageDraw.Draw(self.im, "RGBA")

    def fill(self, x0, y0, x1, y1, col, a=255):
        self.d.rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=rgb(col, a))

    def grad(self, x0, y0, x1, y1, c0, a0, c1, a1):
        for y in range(y0 * S, y1 * S):
            t = (y - y0 * S) / max(1, (y1 - y0) * S - 1)
            col = tuple(int(rgb(c0)[i] * (1 - t) + rgb(c1)[i] * t) for i in range(3)) + (int(a0 * (1 - t) + a1 * t),)
            self.d.line([(x0 * S, y), (x1 * S - 1, y)], fill=col)

    def outline(self, x, y, w, h, col, a=255):
        self.d.rectangle([x * S, y * S, (x + w) * S - 1, (y + h) * S - 1], outline=rgb(col, a), width=S)

    def text(self, x, y, s, col):
        px = x * S
        for ch in s:
            self.d.text((px, y * S - S), ch, font=FONT, fill=rgb(col))
            px += cw(ch) * S


def brackets(c, x, y, w, h, ln, col, a):
    for (ax, ay, bx_, by_) in ((x, y, x + ln, y + 2), (x, y, x + 2, y + ln), (x + w - ln, y, x + w, y + 2), (x + w - 2, y, x + w, y + ln),
                               (x, y + h - 2, x + ln, y + h), (x, y + h - ln, x + 2, y + h), (x + w - ln, y + h - 2, x + w, y + h), (x + w - 2, y + h - ln, x + w, y + h)):
        c.fill(ax, ay, bx_, by_, col, a)


def draw_entry(c, e, x, y, pw):
    col = e["colour"]
    c.fill(x, y, x + 2, y + e["h"], col)
    tx, ty = x + 6, y
    c.text(tx, ty, e["name"], GOLD)
    ty += 11
    c.text(tx, ty, e["group"], col)
    if e["scopeInline"]:
        c.text(tx + e["scopeX"], ty, e["scopeInline"], DIM)
    ty += 10
    for s in e["scope"]:
        c.text(tx, ty, s, DIM)
        ty += 10
    ty += 4
    for s in e["text"]:
        c.text(tx, ty, s, TEXT)
        ty += 10
    if e["facts"]:
        ty += 3
        for s, cap in zip(e["facts"], e["cap"]):
            c.text(tx, ty, s, GREEN if cap else SLATE)
            ty += 10
    if e["found"]:
        ty += 3
        for s in e["found"]:
            c.text(tx, ty, s, FOOT)
            ty += 10


def draw_half(c, sc, page_index, which, squeeze=1.0):
    top = sc["by"] + 24
    x0 = sc["pagesX"] - 4 if which == 0 else sc["spineX"]
    x1 = sc["spineX"] if which == 0 else sc["pagesX"] + 2 * sc["pw"] + 18
    sub = Canvas(x1 - x0, sc["pageH"] + 8)
    sub.grad(0, 0, x1 - x0, sc["pageH"] + 8, 0x131E38, 199, 0x090E1C, 209)
    sub.grad(0, 0, x1 - x0, 30, 0xFFFFFF, 13, 0xFFFFFF, 0)
    if sc["shown"] == 0:
        if which == 0:
            sub.text(4, 10, "No engraving matches your search.", DIM)
    elif page_index < len(sc["pages"]):
        ts = sc["ts"]                                      # the entries are drawn in a space 1 / ts times bigger, then scaled (the screen's pose scale)
        ent = Canvas(int((x1 - x0) / ts) + 2, int((sc["pageH"] + 8) / ts) + 2, transparent=True)
        y = 0
        for e in sc["pages"][page_index]:
            draw_entry(ent, e, 0, y, sc["pw"])
            y += e["h"] + sc["vGap"]
        scaled = ent.im.resize((max(1, int(ent.im.width * ts)), max(1, int(ent.im.height * ts))), Image.LANCZOS)
        sub.im.alpha_composite(scaled, ((4 if which == 0 else 11) * S, 4 * S))
    im = sub.im
    if squeeze != 1.0:
        w = max(2, int(im.width * squeeze))
        im = im.resize((w, im.height), Image.BILINEAR)
        px = x0 * S if which == 1 else int(sc["spineX"] * S - w)
        c.im.alpha_composite(im, (px if which == 1 else px, (top - 4) * S))
        return
    c.im.alpha_composite(im, (x0 * S, (top - 4) * S))


def render(sc, spread=0, flip=None):
    w, h = sc["w"], sc["h"]
    c = Canvas(w, h)
    c.grad(0, 0, w, h, 0x2A3140, 255, 0x12151C, 255)            # a stand-in for the dimmed world
    c.grad(0, 0, w, h, 0x050812, 158, 0x020409, 209)            # the scrim
    bx, by, bw, bh = sc["bx"], sc["by"], sc["bw"], sc["bh"]
    c.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x000000, 85)
    c.grad(bx, by, bx + bw, by + bh, 0x101A30, 242, 0x070B16, 242)
    c.outline(bx, by, bw, bh, CYAN, 180)
    c.outline(bx + 3, by + 3, bw - 6, bh - 6, GOLD, 115)
    brackets(c, bx, by, bw, bh, 10, GOLD, 242)
    sx = bx + 8
    c.text(sx, by + 9, "Engravement Codex", GOLD)
    c.fill(sx, by + 20, sx + SW, by + 21, GOLD, 90)
    c.fill(sx, by + 22, sx + SW, by + 38, 0x060912, 217)
    c.outline(sx, by + 22, SW, 16, CYAN if sc["query"] else FAINT, 97)
    c.text(sx + 4, by + 26, sc["query"] if sc["query"] else "Search...", TEXT if sc["query"] else 0x555555)
    top = by + 42
    vis = (by + bh - 8 - top) // 12
    for i, r in enumerate(sc["rows"][:vis]):
        y = top + i * 12
        active = r["id"] == sc["group"]
        col = r["colour"]
        c.grad(sx, y, sx + SW - 5, y + 11, col, 80 if active else 30, col, 40 if active else 12)
        c.outline(sx, y, SW - 5, 11, col, 200 if active else 90)
        c.fill(sx + 3, y + 3, sx + 6, y + 8, col)
        label = r["label"]
        while sum(cw(ch) for ch in label) > SW - 24 - sum(cw(ch) for ch in str(r["count"])) and len(label) > 1:
            label = label[:-1]
        c.text(sx + 9, y + 2, label, WHITE if active else TEXT)
        cnt = str(r["count"])
        c.text(sx + SW - 8 - sum(cw(ch) for ch in cnt), y + 2, cnt, DIM)
    h_list = vis * 12
    bar = max(10, h_list * vis // len(sc["rows"]))
    c.fill(sx + SW - 3, top, sx + SW, top + h_list, CYAN, 46)
    c.fill(sx + SW - 3, top, sx + SW, top + bar, CYAN, 217)
    if flip is None:
        draw_half(c, sc, spread * 2, 0)
        draw_half(c, sc, spread * 2 + 1, 1)
    else:
        # a forward turn, as the screen does it: the left half still shows the OLD spread, the right half already the NEW one, and the leaf between them is the old right page
        # squeezing toward the spine (first half of the turn), then the new left page widening from it (second half)
        draw_half(c, sc, flip["old"] * 2, 0)
        draw_half(c, sc, spread * 2 + 1, 1)
        s = 1 - flip["t"] * 2 if flip["t"] < 0.5 else flip["t"] * 2 - 1
        s = s * s * (3 - 2 * s)
        if flip["t"] < 0.5:
            draw_half(c, sc, flip["old"] * 2 + 1, 1, squeeze=max(0.05, s))
        else:
            draw_half(c, sc, spread * 2, 0, squeeze=max(0.05, s))
    fy = by + bh - 14
    count = ("%d engravings" % sc["total"]) if sc["shown"] == sc["total"] else ("%d of %d shown" % (sc["shown"], sc["total"]))
    c.text(sc["pagesX"] + 4, fy, count, DIM)
    spreads = max(1, (sc["pageCount"] + 1) // 2)
    num = "%d / %d" % (spread + 1, spreads)
    c.text(bx + bw - 24 - sum(cw(ch) for ch in num), fy, num, DIM)
    if spread < spreads - 1:
        c.text(bx + bw - 18, fy, ">", CYAN)
    hint_x = sc["pagesX"] + 8 + sum(cw(ch) for ch in count) + 12
    hint = "Wheel, arrows or PageUp / PageDown turn the page."
    room = bx + bw - 24 - sum(cw(ch) for ch in num) - 12 - hint_x                # the screen cuts the hint to the room it has, and drops it under 60 px
    while sum(cw(ch) for ch in hint) > room and len(hint) > 1:
        hint = hint[:-1]
    if room > 60:
        c.text(hint_x, fy, hint, FAINT)
    # the buttons under the book
    for (x0, label, wd) in ((bx + 4, "<", 22), (bx + 30, ">", 22), (bx + bw - 64, "Close", 60)):
        c.fill(x0, by + bh + 4, x0 + wd, by + bh + 20, 0x111C34, 200)
        c.outline(x0, by + bh + 4, wd, 16, DIM, 200)
        c.text(x0 + wd // 2 - sum(cw(ch) for ch in label) // 2, by + bh + 8, label, TEXT)
    return c.im.convert("RGB")


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(DOCS, exist_ok=True)
    scenarios = build_and_run()
    names = []
    keep_in_docs = {0, 1, 2, 3, 4, 5, 6, 7, 9, 11}               # the others are written to build/ only (the docs folder stays small)
    for i, sc in enumerate(scenarios):
        im = render(sc)
        tag = "screen_%02d_%dx%d.png" % (i, sc["w"], sc["h"])
        for d in ((OUT, DOCS) if i in keep_in_docs else (OUT,)):
            im.save(os.path.join(d, tag))
        names.append(tag)
        print("wrote", tag, "-", sc["title"], "-", sc["shown"], "shown,", sc["pageCount"], "pages")
    sc = scenarios[0]
    for k, (label, spread, flip) in enumerate((("turn_mid", 1, {"old": 0, "t": 0.25}), ("second_spread", 1, None))):
        im = render(sc, spread=spread, flip=flip)
        tag = "screen_%s_%dx%d.png" % (label, sc["w"], sc["h"])
        for d in (OUT, DOCS):
            im.save(os.path.join(d, tag))
        print("wrote", tag)


if __name__ == "__main__":
    main()
