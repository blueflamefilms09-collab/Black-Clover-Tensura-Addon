"""The cover emblems of the Canon Grimoires: 30 motifs, one per book, drawn as 40x40 black-and-white masks (supersampled, then thresholded so the edge is crisp pixel art).

Each motif is a function drawing on a 100 x 100 logical canvas (fill 255 draws, fill 0 erases) inside a circle of radius about 42 around (50, 50), which is what fits the dark disc of
the medallion. MOTIF_OF says which motif belongs to which book and why (the character's magic, role or animal), so a book keeps an identity of its own even where its colours are
the Builder's default grey.
"""
import math

from PIL import Image, ImageDraw

SS = 4                      # supersampling
OUT = 40                    # mask size in pixels
CAN = OUT * SS              # canvas size in pixels
K = CAN / 100.0             # logical -> canvas


class Pen:
    """A tiny drawing helper over a PIL 'L' image: logical 0..100 coordinates, fill 255 to draw and 0 to erase."""

    def __init__(self):
        self.im = Image.new("L", (CAN, CAN), 0)
        self.d = ImageDraw.Draw(self.im)

    @staticmethod
    def p(pt):
        return (pt[0] * K, pt[1] * K)

    def poly(self, pts, fill=255):
        self.d.polygon([self.p(q) for q in pts], fill=fill)

    def circ(self, cx, cy, r, fill=255):
        self.d.ellipse((self.p((cx - r, cy - r)), self.p((cx + r, cy + r))), fill=fill)

    def ring(self, cx, cy, r, w):
        self.circ(cx, cy, r, 255)
        self.circ(cx, cy, r - w, 0)

    def ell(self, x0, y0, x1, y1, fill=255):
        self.d.ellipse((self.p((x0, y0)), self.p((x1, y1))), fill=fill)

    def rect(self, x0, y0, x1, y1, fill=255, r=0):
        if r:
            self.d.rounded_rectangle((self.p((x0, y0)), self.p((x1, y1))), radius=r * K, fill=fill)
        else:
            self.d.rectangle((self.p((x0, y0)), self.p((x1, y1))), fill=fill)

    def line(self, a, b, w, fill=255):
        self.d.line((self.p(a), self.p(b)), fill=fill, width=max(1, int(round(w * K))))
        for q in (a, b):
            self.circ(q[0], q[1], w / 2.0, fill)

    def path(self, pts, w, fill=255):
        for a, b in zip(pts, pts[1:]):
            self.line(a, b, w, fill)

    def arc(self, cx, cy, r, a0, a1, w, fill=255):
        n = 40
        pts = [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]
        self.path(pts, w, fill)

    def stamp(self, fn, angle=0.0, dx=0.0, dy=0.0):
        """Draw fn on a fresh canvas, rotate it about the centre, and add it (max) at an offset."""
        sub = Pen()
        fn(sub)
        im = sub.im.rotate(angle, resample=Image.BICUBIC, center=(CAN / 2, CAN / 2))
        if dx or dy:
            im = im.transform(im.size, Image.AFFINE, (1, 0, -dx * K, 0, 1, -dy * K), resample=Image.BICUBIC)
        self.im.paste(255, (0, 0), im.point(lambda v: 255 if v > 128 else 0))

    def mask(self):
        im = self.im.resize((OUT, OUT), Image.LANCZOS)
        return im.point(lambda v: 255 if v >= 128 else 0)


def polar(cx, cy, r, deg):
    return (cx + r * math.cos(math.radians(deg)), cy + r * math.sin(math.radians(deg)))


# ----------------------------------------------------------------------------------------------------------------------------------------------- the motifs
def well(p):
    """Gravity well: three rings pulled into a point, four gaps where the pull enters."""
    p.ring(50, 50, 41, 7)
    p.ring(50, 50, 26, 7)
    p.circ(50, 50, 9)
    for a in (0, 90, 180, 270):
        c = polar(50, 50, 41, a)
        p.circ(c[0], c[1], 6, 0)
    for a in (45, 135, 225, 315):
        c = polar(50, 50, 26, a)
        p.circ(c[0], c[1], 5, 0)


def drop(p):
    """A drop of blood (the curse-warding devil)."""
    p.circ(50, 64, 27)
    p.poly([(50, 6), (27, 55), (73, 55)])
    p.ell(34, 56, 44, 76, 0)
    p.line((62, 54), (70, 66), 4, 0)


def rift(p):
    """A rift in space: a diamond with a crack of light through it."""
    p.poly([(50, 6), (82, 50), (50, 94), (18, 50)])
    p.poly([(50, 20), (68, 50), (50, 80), (32, 50)], 0)
    p.path([(55, 22), (45, 40), (55, 52), (45, 64), (51, 78)], 5)


def hourglass(p):
    p.poly([(26, 14), (74, 14), (54, 50), (74, 86), (26, 86), (46, 50)])
    p.rect(18, 6, 82, 15)
    p.rect(18, 85, 82, 94)
    p.poly([(34, 20), (66, 20), (51, 42)], 0)
    p.poly([(51, 60), (34, 80), (66, 80)], 0)
    p.poly([(40, 80), (60, 80), (50, 66)])
    p.line((50, 44), (50, 62), 3)


def flame(p):
    p.poly([(50, 4), (62, 26), (78, 46), (82, 68), (70, 88), (50, 96), (30, 88), (18, 68), (24, 46), (36, 34), (40, 48), (46, 26)])
    p.poly([(50, 50), (62, 68), (60, 82), (50, 88), (40, 82), (38, 68)], 0)
    p.poly([(50, 64), (56, 76), (50, 82), (44, 76)])


def fist(p):
    for i in range(4):
        x = 24 + i * 13.5
        p.rect(x, 12, x + 12.5, 48, 255, r=6)
    p.rect(22, 30, 78, 76, 255, r=9)
    for i in range(1, 4):
        p.line((24 + i * 13.5 - 0.5, 28), (24 + i * 13.5 - 0.5, 44), 2.2, 0)
    p.rect(8, 44, 34, 62, 255, r=8)
    p.rect(30, 74, 72, 94)
    p.line((26, 50), (36, 50), 2.2, 0)


def vortex(p):
    pts = []
    for i in range(0, 121):
        t = i / 120.0
        a = 200 + 800 * t
        r = 4 + 38 * t
        pts.append(polar(50, 50, r, a))
    p.path(pts, 8)
    p.circ(50, 50, 6)


def serpent(p):
    """A twin-headed serpent: a fat S, a head with an eye at each end."""
    pts = [(50 + 27 * math.sin(i / 30.0 * math.pi * 2 - 0.2), 24 + i * 52 / 30.0) for i in range(31)]
    p.path(pts, 15)
    for q in (pts[0], pts[-1]):
        p.circ(q[0], q[1], 14)
    p.circ(pts[0][0] - 5, pts[0][1] - 3, 3.6, 0)
    p.circ(pts[0][0] + 5, pts[0][1] - 3, 3.6, 0)
    p.circ(pts[-1][0] - 5, pts[-1][1] + 3, 3.6, 0)
    p.circ(pts[-1][0] + 5, pts[-1][1] + 3, 3.6, 0)
    p.poly([(pts[0][0] - 4, pts[0][1] - 12), (pts[0][0], pts[0][1] - 22), (pts[0][0] + 4, pts[0][1] - 12)])
    p.poly([(pts[-1][0] - 4, pts[-1][1] + 12), (pts[-1][0], pts[-1][1] + 22), (pts[-1][0] + 4, pts[-1][1] + 12)])


def fox(p):
    p.poly([(10, 6), (38, 30), (62, 30), (90, 6), (90, 54), (50, 94), (10, 54)])
    p.poly([(20, 22), (32, 32), (20, 44)], 0)
    p.poly([(80, 22), (68, 32), (80, 44)], 0)
    p.poly([(24, 52), (42, 60), (39, 66), (22, 58)], 0)
    p.poly([(76, 52), (58, 60), (61, 66), (78, 58)], 0)
    p.poly([(44, 82), (56, 82), (50, 90)], 0)


def anvil(p):
    p.poly([(4, 26), (96, 26), (96, 40), (72, 48), (64, 56), (66, 70), (82, 78), (82, 92), (18, 92), (18, 78), (34, 70), (36, 56), (28, 48), (4, 38)])
    p.line((30, 34), (70, 34), 3, 0)


def hammer(p):
    p.line((18, 92), (60, 46), 11)
    p.stamp(lambda q: q.rect(16, 18, 84, 58, 255, r=5) or q.line((26, 38), (74, 38), 3, 0), 45, 18, -6)


def sun(p):
    p.circ(50, 50, 21)
    for i in range(12):
        a = i * 30.0
        r1 = 46 if i % 2 == 0 else 38
        p.poly([polar(50, 50, 28, a - 9), polar(50, 50, r1, a), polar(50, 50, 28, a + 9)])
    p.circ(50, 50, 12, 0)
    p.circ(50, 50, 6)


def eye(p):
    top = [(x, 50 - 32 * (1 - ((x - 50) / 46.0) ** 2)) for x in range(4, 97, 4)]
    bot = [(x, 50 + 32 * (1 - ((x - 50) / 46.0) ** 2)) for x in range(96, 3, -4)]
    p.poly(top + bot)
    p.circ(50, 50, 21, 0)
    p.circ(50, 50, 13)
    p.circ(50, 50, 5, 0)
    for a in (-70, -90, -110):
        p.line(polar(50, 50, 38, a), polar(50, 50, 47, a), 4)


def ram(p):
    """A goat: horns sweeping out and curling down round a narrow face."""
    p.poly([(34, 38), (66, 38), (62, 78), (50, 94), (38, 78)])
    pts = [(36, 38), (22, 26), (10, 30), (6, 46), (14, 58), (26, 56)]
    p.path(pts, 10)
    p.path([(100 - x, y) for x, y in pts], 10)
    p.circ(42, 54, 4, 0)
    p.circ(58, 54, 4, 0)
    p.line((46, 80), (54, 80), 3, 0)


def snowflake(p):
    for i in range(6):
        a = i * 60.0 - 90
        p.line((50, 50), polar(50, 50, 43, a), 6)
        for r, ln in ((24, 14), (34, 10)):
            c = polar(50, 50, r, a)
            p.line(c, polar(c[0], c[1], ln, a + 45), 4.5)
            p.line(c, polar(c[0], c[1], ln, a - 45), 4.5)
    p.circ(50, 50, 9)


def wings(p):
    wing = [(54, 74), (56, 44), (66, 20), (96, 8), (90, 30), (98, 38), (88, 52), (94, 62), (80, 68), (84, 80), (64, 86)]
    p.poly(wing)
    p.poly([(100 - x, y) for x, y in wing])
    p.poly([(50, 20), (60, 52), (50, 90), (40, 52)])
    for y in (30, 46, 62):
        p.line((60, y + 4), (84, y - 12), 2.2, 0)
        p.line((40, y + 4), (16, y - 12), 2.2, 0)


def cloud(p):
    for cx, cy, r in ((28, 62, 18), (47, 46, 22), (70, 58, 20), (50, 66, 20)):
        p.circ(cx, cy, r)
    p.rect(20, 62, 82, 84, 255, r=10)
    p.arc(50, 50, 8, 200, 470, 3, 0)
    p.arc(70, 66, 6, 180, 440, 3, 0)


def crown(p):
    p.poly([(14, 70), (14, 22), (34, 46), (50, 12), (66, 46), (86, 22), (86, 70)])
    p.rect(14, 64, 86, 84)
    for x in (14, 50, 86):
        p.circ(x, 16 if x == 50 else 18, 6)
    for x in (30, 50, 70):
        p.poly([(x, 68), (x + 6, 74), (x, 80), (x - 6, 74)], 0)


def crystal(p):
    p.poly([(50, 4), (76, 26), (76, 72), (50, 96), (24, 72), (24, 26)])
    p.path([(24, 26), (50, 44), (76, 26)], 3.5, 0)
    p.line((50, 44), (50, 96), 3.5, 0)
    p.line((24, 72), (50, 44), 3.5, 0)


def sword(p):
    p.poly([(44, 4), (56, 4), (59, 64), (50, 72), (41, 64)])
    p.rect(26, 62, 74, 71, 255, r=3)
    p.rect(45, 71, 55, 88)
    p.circ(50, 91, 6)
    p.line((50, 12), (50, 56), 2.5, 0)


def orbit(p):
    p.stamp(lambda q: (q.ell(6, 30, 94, 70), q.ell(13, 36, 87, 64, 0)), -28)
    p.poly([(50, 12), (58, 42), (88, 50), (58, 58), (50, 88), (42, 58), (12, 50), (42, 42)])
    p.circ(50, 50, 5, 0)
    p.circ(82, 28, 7)


def horns(p):
    """A bull: wide horns up and in over a broad muzzle."""
    p.poly([(30, 46), (70, 46), (66, 92), (34, 92)])
    p.circ(42, 80, 3.5, 0)
    p.circ(58, 80, 3.5, 0)
    p.circ(40, 58, 4, 0)
    p.circ(60, 58, 4, 0)
    pts = [(32, 52), (14, 54), (6, 40), (10, 20)]
    p.path(pts, 11)
    p.path([(100 - x, y) for x, y in pts], 11)
    p.poly([(2, 24), (10, 4), (18, 24)])
    p.poly([(98, 24), (90, 4), (82, 24)])


def circle_magic(p):
    p.ring(50, 50, 43, 5)
    p.ring(50, 50, 33, 3)
    for rot in (-90, 90):
        pts = [polar(50, 50, 30, rot + k * 120) for k in range(3)]
        p.path(pts + [pts[0]], 4.5)
    p.circ(50, 50, 4, 0)


def star(p):
    pts = []
    for i in range(16):
        pts.append(polar(50, 50, 46 if i % 2 == 0 else 17, i * 22.5 - 90))
    p.poly(pts)
    p.circ(50, 50, 7, 0)


def wisp(p):
    p.circ(50, 38, 27)
    p.poly([(23, 38), (23, 90), (33, 80), (42, 92), (50, 80), (58, 92), (67, 80), (77, 90), (77, 38)])
    p.circ(40, 40, 7, 0)
    p.circ(60, 40, 7, 0)
    p.ell(44, 54, 56, 70, 0)


def chain(p):
    def ring_face(q):
        q.ell(26, 8, 74, 92)
        q.ell(37, 20, 63, 80, 0)

    def ring_edge(q):
        q.rect(43, 4, 57, 96, 255, r=7)
    p.stamp(ring_face, -45, -24, 24)
    p.stamp(ring_edge, -45, 0, 0)
    p.stamp(ring_face, -45, 24, -24)


def mirror(p):
    p.ring(34, 50, 28, 6)
    p.ring(66, 50, 28, 6)
    lens = []
    for i in range(0, 25):
        a = -60 + 120 * i / 24.0
        lens.append(polar(34, 50, 28, a))
    for i in range(0, 25):
        a = 120 + 120 * i / 24.0
        lens.append(polar(66, 50, 28, a))
    p.poly(lens)


def skull(p):
    p.circ(50, 42, 35)
    p.rect(30, 62, 70, 90, 255, r=6)
    p.circ(35, 44, 10, 0)
    p.circ(65, 44, 10, 0)
    p.poly([(50, 54), (43, 68), (57, 68)], 0)
    for x in (40, 50, 60):
        p.line((x, 76), (x, 92), 3, 0)


def claw(p):
    for i in range(3):
        a = 8 + i * 25
        p.poly([(a + 10, 4), (a + 26, 10), (a - 4, 96)])


def rune(p):
    p.line((50, 8), (50, 92), 9)
    p.line((50, 50), (20, 14), 9)
    p.line((50, 50), (80, 14), 9)
    p.poly([(50, 62), (62, 74), (50, 86), (38, 74)], 0)
    p.circ(50, 8, 6)


MOTIFS = {fn.__name__: fn for fn in (well, drop, rift, hourglass, flame, fist, vortex, serpent, fox, anvil, hammer, sun, eye, ram, snowflake, wings, cloud, crown, crystal,
                                      sword, orbit, horns, circle_magic, star, wisp, chain, mirror, skull, claw, rune)}

# book id -> (motif, why)
MOTIF_OF = {
    "dante": ("well", "the Gravity Devil: weight that pulls everything in"),
    "vanica": ("drop", "the Curse-Warding Devil: blood and curses"),
    "zenon": ("rift", "the Spatial Devil: doors where there are no doors"),
    "lucius": ("hourglass", "the Time Devil's vessel: nothing stays as it was"),
    "devil_avatar": ("flame", "hellfire borrowed for as long as the book is held"),
    "goliath": ("fist", "the Pit's giant: stone, weight and a very large fist"),
    "charybdis_spawn": ("vortex", "the Basin's whirlpool"),
    "amphisbaena": ("serpent", "the Twin-Headed Water Dragon"),
    "ban": ("fox", "the Fox's Sin of Greed"),
    "black_goliath": ("anvil", "the Dungeon's Wrath: a black giant of the forge floors"),
    "diane": ("hammer", "the Serpent's Sin of Envy: the giant and her hammer Gideon"),
    "escanor": ("sun", "the Lion's Sin of Pride: The One, the sun at noon"),
    "fana": ("eye", "the Hatred of the Third Eye"),
    "gowther": ("ram", "the Goat's Sin of Lust"),
    "heath_grice": ("snowflake", "ice magic, Eye of the Midnight Sun"),
    "king": ("wings", "the Grizzly's Sin of Sloth: the fairy king"),
    "lotus_whomalt": ("cloud", "smoke magic, the Sideline General"),
    "lucifero": ("crown", "King of Devils"),
    "mars": ("crystal", "the Crystal Weapon"),
    "meliodas": ("sword", "the Dragon's Sin of Wrath: Lostvayne"),
    "merlin": ("orbit", "the Boar's Sin of Gluttony: spatial magic, a mind in orbit"),
    "minotaur": ("horns", "the Bull of the Labyrinth"),
    "morris_libardirt": ("circle_magic", "the Magic Scholar: modification magic circles"),
    "patolli": ("star", "Leader of the Eye of the Midnight Sun: light magic"),
    "rades_spirito": ("wisp", "the Soul Corpse Mage"),
    "revchi_salik": ("chain", "the Chain Thief"),
    "rhya": ("mirror", "copy magic: two circles that share a lens"),
    "udaeus": ("skull", "the Skeleton Giant of the White Palace"),
    "vetto": ("claw", "the Beast of the Third Eye"),
    "zagred": ("rune", "the Word Soul Devil: kotodama, the written word"),
}

# the four Seven Deadly Sins books that fall back on the Builder's grey (8A8A9A / C8C8D8) get an emblem glow colour of their own, so they do not read as four copies
GLOW_OVERRIDE = {"diane": 0x6FD08C, "gowther": 0xF27BC0, "king": 0xB8F070, "merlin": 0xF0C060}


def mask_of(motif):
    p = Pen()
    MOTIFS[motif](p)
    return p.mask()


if __name__ == "__main__":
    import sys
    cols = 6
    names = list(MOTIF_OF)
    sheet = Image.new("RGB", (cols * 130, ((len(names) + cols - 1) // cols) * 150), (30, 30, 36))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        m = mask_of(MOTIF_OF[n][0]).resize((120, 120), Image.NEAREST)
        sheet.paste((255, 220, 120), (130 * (i % cols) + 5, 150 * (i // cols) + 4), m)
        d.text((130 * (i % cols) + 5, 150 * (i // cols) + 128), n, fill=(255, 255, 255))
    sheet.save(sys.argv[1] if len(sys.argv) > 1 else "motifs.png")
