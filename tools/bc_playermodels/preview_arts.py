#!/usr/bin/env python3
"""Plan-view stills of the five new boss arts (docs/boss_redesign_goku_sinbad.md, docs/boss_showcase/README.md).

HONEST LABEL: these are RECIPE PREVIEWS, not game screenshots. The Java effects need the game's classes, so this draws, from above, the shapes the code builds: the sizes are READ FROM THE JAVA
SOURCE (GokuArts.java, SinbadMoves.java) and the geometry is a port of arc/BossShapes.java (ring, orbit, sector, discDistance, pillarAngle). What it shows is where the telegraph is drawn,
where it hurts and where it is safe; the particle look in game is not shown.

    python3 tools/bc_playermodels/preview_arts.py [out_dir]       (default docs/boss_showcase)
Needs Pillow only.
"""
import math
import os
import re
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "arc")


def java_consts(path):
    """name -> number for every `static final ... NAME = 1.5` or `NAME = 3` in a source file (also the comma lists)."""
    s = open(path).read()
    out = {}
    for m in re.finditer(r"\b([A-Z][A-Z0-9_]{2,})\s*=\s*(-?\d+(?:\.\d+)?)\b", s):
        out.setdefault(m.group(1), float(m.group(2)))
    return out


G = java_consts(os.path.join(SRC, "goku", "GokuArts.java"))
S = java_consts(os.path.join(SRC, "magi", "SinbadMoves.java"))

BG, GRID, WHITE = (14, 16, 24), (30, 34, 48), (240, 240, 245)
W = H = 720
FONT = None
for f in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf"):
    if os.path.exists(f):
        FONT = f


def font(sz):
    return ImageFont.truetype(FONT, sz) if FONT else ImageFont.load_default()


class Plan:
    def __init__(self, title, sub, span, accent):
        self.im = Image.new("RGB", (W, H), BG)      # RGB so that the RGBA draw mode blends
        self.d = ImageDraw.Draw(self.im, "RGBA")
        self.span = span
        self.k = (H - 270) / (2.0 * span)
        self.cx, self.cy = W / 2, 100 + (H - 270) / 2
        self.accent = accent
        self.d.text((24, 16), title, fill=WHITE, font=font(30))
        words, line, y = sub.split(" "), "", 52
        for w in words:
            if len(line) + len(w) > 78:
                self.d.text((24, y), line, fill=(190, 196, 215), font=font(15))
                line, y = "", y + 20
            line += w + " "
        self.d.text((24, y), line, fill=(190, 196, 215), font=font(15))
        for g in range(-int(span), int(span) + 1, 5):
            c = GRID if g else (60, 66, 90)
            x, y = self.cx + g * self.k, self.cy + g * self.k
            self.d.line([(x, self.cy - span * self.k), (x, self.cy + span * self.k)], fill=c)
            self.d.line([(self.cx - span * self.k, y), (self.cx + span * self.k, y)], fill=c)
        self.d.text((24, H - 36), "RECIPE PREVIEW from the Java constants - not a game screenshot", fill=(255, 190, 90), font=font(14))

    def p(self, x, z):
        return self.cx + x * self.k, self.cy + z * self.k

    def circle(self, x, z, r, fill=None, outline=None, w=2):
        a, b = self.p(x - r, z - r), self.p(x + r, z + r)
        self.d.ellipse([a, b], fill=fill, outline=outline, width=w)

    def dot(self, x, z, r, col):
        self.circle(x, z, r / self.k, fill=col)

    def line(self, pts, col, w=2):
        self.d.line([self.p(*q) for q in pts], fill=col, width=w)

    def label(self, x, z, text, col=WHITE, sz=14):
        px, py = self.p(x, z)
        self.d.text((px, py), text, fill=col, font=font(sz))

    def legend(self, rows):
        y = H - 150
        for col, text in rows:
            self.d.rectangle([24, y + 3, 40, y + 15], fill=col)
            self.d.text((48, y), text, fill=WHITE, font=font(14))
            y += 20

    def save(self, path):
        self.im.convert("RGB").save(path)


def with_alpha(c, a):
    return (c[0], c[1], c[2], a)


def cage(out):
    R, HIT, SLAM, N = G["CAGE_RADIUS"], G["CAGE_HIT"], G["SLAM_RADIUS"], int(G["CAGE_PILLARS"])
    gold = (255, 190, 60)
    pl = Plan("Goku - Zanzoken Cage", "40 tick wind-up: four afterimage pillars grow round you, he streaks through all four, then slams the middle", 10, gold)
    for e in (0.25, 0.6, 1.0):
        pl.circle(0, 0, R * e, outline=with_alpha(gold, int(90 + 120 * e)), w=2)
    pts = [(math.cos(2 * math.pi * i / N) * R, math.sin(2 * math.pi * i / N) * R) for i in range(N)]
    order = [0, 2, 1, 3, 0]
    pl.line([pts[i] for i in order], with_alpha((255, 240, 200), 160), 3)
    for i, (x, z) in enumerate(pts):
        pl.circle(x, z, HIT, fill=with_alpha((255, 120, 40), 55), outline=with_alpha((255, 120, 40), 200))
        pl.dot(x, z, 9, (255, 236, 170))
        pl.label(x + 0.3, z - 1.1, "blink %d" % (i + 1), (255, 236, 170))
    pl.circle(0, 0, SLAM, fill=with_alpha((255, 60, 40), 70), outline=(255, 90, 60))
    pl.dot(0, 0, 8, WHITE)
    pl.label(-1.2, 0.5, "you", WHITE)
    pl.label(-SLAM * 0.7, SLAM + 0.3, "slam r=%.1f" % SLAM, (255, 150, 120))
    pl.legend([(with_alpha((255, 120, 40), 160), "streak hit r=%.1f at each pillar (blink gap %d ticks)" % (HIT, G["CAGE_GAP"])),
               ((255, 90, 60), "final slam r=%.1f at the centre it locked at the start" % SLAM),
               (with_alpha(gold, 200), "telegraph ring r=%.1f, grows 25%% -> 100%% over the wind-up (a point cap of 32)" % R),
               ((255, 255, 255), "counterplay: step out of the cage before the first blink")])
    pl.save(os.path.join(out, "art_goku_zanzoken_cage.png"))


def flare(out):
    reach = G["FLARE_REACH"]
    pl = Plan("Goku - Solar Flare", "30 tick wind-up: hands to the brow; only players who FACE him are blinded", 30, (255, 240, 150))
    for i in range(3):
        pl.circle(0, 0, 4 + i * 4, outline=with_alpha((255, 240, 150), 200 - i * 50))
    pl.circle(0, 0, reach, outline=with_alpha((255, 240, 150), 90))
    ang = math.radians(-90)
    for deg in range(-62, 63, 4):
        a = ang + math.radians(deg)
        pl.line([(0, 0), (math.cos(a) * reach, math.sin(a) * reach)], with_alpha((255, 240, 150), 28), 6)
    pl.dot(0, 0, 10, (255, 150, 40))
    pl.label(0.6, 0.6, "Goku", WHITE)
    fx = [(-8, -16, True), (9, -12, True), (-14, 8, False), (15, 12, False)]
    for x, z, sees in fx:
        pl.dot(x, z, 9, (120, 200, 255) if sees else (150, 150, 160))
        pl.line([(x, z), (x * 0.75, z * 0.75)] if sees else [(x, z), (x * 1.18, z * 1.18)], (120, 200, 255) if sees else (150, 150, 160), 2)
        pl.label(x + 0.8, z + 0.8, "blinded" if sees else "faces away: safe", (120, 200, 255) if sees else (170, 170, 180), 13)
    pl.legend([(with_alpha((255, 240, 150), 200), "three warning rings (4, 8, 12 blocks), 32 points each, budgeted"),
               ((120, 200, 255), "in reach %d blocks and looking at him: blind + slow" % reach),
               ((150, 150, 160), "looking away (view cosine under the limit): untouched"),
               ((255, 255, 255), "counterplay: turn your back, or break the line of sight")])
    pl.save(os.path.join(out, "art_goku_solar_flare.png"))


def disc(out):
    reach, hit, outt = G["DISC_REACH"], G["DISC_HIT"], int(G["DISC_OUT"])
    pl = Plan("Goku - Kienzan Boomerang", "24 tick wind-up: a spinning disc flies out %d blocks in %d ticks and comes back along the same line" % (reach, outt), 14, (130, 220, 255))
    pl.span = 14
    # side-on strip: x runs along the throw
    y0 = 0
    def to(dist):
        return (dist - reach / 2.0) * 0.62, y0
    tcol = (130, 220, 255)
    for t in range(0, 2 * outt + 1):
        k = t / float(outt) if t <= outt else (2 * outt - t) / float(outt)
        dist = reach * (1 - (1 - k) ** 2) if 0 < t < 2 * outt else 0
        x, z = to(dist)
        yy = -2.2 - (t % 2) * 0.0
        col = with_alpha(tcol, 255 if t % 7 == 0 else 120)
        pl.circle(x, (-3.0 if t <= outt else 3.0), hit * 0.24, fill=col)
    pl.line([to(0), to(reach)], with_alpha(tcol, 80), 2)
    x0, _ = to(0)
    x1, _ = to(reach)
    pl.dot(x0, 0, 10, (255, 150, 40))
    pl.label(x0 - 1.2, 0.8, "Goku", WHITE)
    pl.dot(x1, 0, 5, tcol)
    pl.label(x1 - 2.5, 1.0, "far end", tcol, 13)
    pl.label(-9.5, -5.4, "out: fast, slowing at the far end", tcol, 13)
    pl.label(-9.5, 5.2, "back: the same eased path in reverse", tcol, 13)
    pl.circle((x0 + x1) / 2, 0, hit, outline=with_alpha((255, 120, 60), 220))
    pl.label((x0 + x1) / 2 - 3.2, 4.4, "hit width r=%.1f, one hit per pass" % hit, (255, 150, 110), 13)
    pl.legend([(tcol, "disc positions per tick (28 steps, at most 12 points each)"),
               ((255, 120, 60), "hit radius %.1f; a foe is hit once out and once back" % hit),
               ((255, 255, 255), "counterplay: step sideways off the line, the disc never homes")])
    pl.save(os.path.join(out, "art_goku_ki_disc.png"))


def orrery(out):
    shots, gap = int(S["ORRERY_SHOTS"]), int(S["ORRERY_GAP"])
    cols = [(91, 184, 255), (168, 230, 255), (215, 166, 255), (155, 240, 192), (192, 200, 216), (255, 242, 168), (140, 80, 190)]
    pl = Plan("Sinbad - Rukh Orrery", "44 tick wind-up: seven djinn lights orbit him, then land one by one (%d ticks apart) on seven locked spots round you" % gap, 12, (232, 217, 168))
    pl.circle(0, -7, 2.4, outline=with_alpha((232, 217, 168), 180))
    pl.dot(0, -7, 9, (232, 217, 168))
    pl.label(0.8, -6.2, "Sinbad", WHITE)
    for i in range(shots):
        a = i * 2 * math.pi / shots
        ox, oz = math.cos(a) * 2.4, -7 + math.sin(a) * 2.4
        pl.dot(ox, oz, 6, cols[i])
    pl.dot(0, 4, 10, WHITE)
    pl.label(0.8, 4.6, "you", WHITE)
    for i in range(shots):
        a = i * 2 * math.pi / shots + 0.4
        sx, sz = math.cos(a) * 3.2, 4 + math.sin(a) * 3.2
        pl.line([(math.cos(i * 2 * math.pi / shots) * 2.4, -7 + math.sin(i * 2 * math.pi / shots) * 2.4), (sx, sz)], with_alpha(cols[i], 70), 2)
        pl.circle(sx, sz, 0.9, outline=with_alpha(cols[i], 230), fill=with_alpha(cols[i], 50))
        pl.label(sx + 0.4, sz - 0.2, str(i + 1), cols[i], 13)
    pl.legend([((232, 217, 168), "orbit ring r=2.4 above his head, 7 lights + a slow ring of 14 points"),
               ((255, 255, 255), "spots: spread of radius 3.2 round the foe, each marked by a 1.5 block ring"),
               ((155, 240, 192), "each light lands on its own tick with its djinn's colour and rider effect"),
               ((255, 255, 255), "counterplay: keep moving, the spots are fixed when the cast starts")])
    pl.save(os.path.join(out, "art_sinbad_rukh_orrery.png"))


def sigil(out):
    R, N = S["SIGIL_RADIUS"], int(S["SIGIL_SECTORS"])
    gold, dark = (255, 214, 120), (20, 22, 40)
    pl = Plan("Sinbad - Kingdom Sigil", "56 tick wind-up: a gold circle of %d sectors; the lit ones erupt, the dark ones are safe" % N, 14, gold)
    phase, pattern = math.radians(20), 0
    for s in range(N):
        a0 = phase + s * 2 * math.pi / N
        a1 = a0 + 2 * math.pi / N
        lit = ((s + pattern) & 1) == 0
        pts = [pl.p(0, 0)] + [pl.p(math.cos(a0 + (a1 - a0) * t / 12.0) * R, math.sin(a0 + (a1 - a0) * t / 12.0) * R) for t in range(13)]
        pl.d.polygon(pts, fill=with_alpha(gold, 95) if lit else with_alpha((60, 70, 120), 40), outline=with_alpha(gold, 220))
        am = (a0 + a1) / 2
        pl.label(math.cos(am) * R * 0.6 - 0.6, math.sin(am) * R * 0.6 - 0.4, "HIT" if lit else "safe", gold if lit else (150, 170, 230), 14)
    pl.circle(0, 0, R, outline=gold, w=3)
    pl.dot(0, 0, 9, (232, 217, 168))
    pl.label(0.6, 0.6, "Sinbad", WHITE)
    pl.legend([(with_alpha(gold, 200), "lit sectors (every second one): 3 eruptions of 24 points + a column"),
               ((100, 110, 170), "dark sectors: nothing, stand here"),
               ((255, 255, 255), "radius %.0f; which half is lit (pattern) and the turn (phase) are locked at the start" % R),
               ((255, 255, 255), "counterplay: read the pattern during the 56 ticks, then step into the dark")])
    pl.save(os.path.join(out, "art_sinbad_kingdom_sigil.png"))


def sheet(out, names):
    ims = [Image.open(os.path.join(out, n)).convert("RGB") for n in names]
    w, h = ims[0].size
    cols = 3
    rows = (len(ims) + cols - 1) // cols
    sh = Image.new("RGB", (w * cols // 2, h * rows // 2), BG)
    for i, im in enumerate(ims):
        sh.paste(im.resize((w // 2, h // 2), Image.LANCZOS), ((i % cols) * w // 2, (i // cols) * h // 2))
    sh.save(os.path.join(out, "arts_overview.png"))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "boss_showcase")
    os.makedirs(out, exist_ok=True)
    cage(out)
    flare(out)
    disc(out)
    orrery(out)
    sigil(out)
    names = ["art_goku_zanzoken_cage.png", "art_goku_solar_flare.png", "art_goku_ki_disc.png", "art_sinbad_rukh_orrery.png", "art_sinbad_kingdom_sigil.png"]
    sheet(out, names)
    print("wrote", len(names) + 1, "images to", out)


if __name__ == "__main__":
    main()
