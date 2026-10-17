"""Why: the Soul Forge's circular aptitude icons, its category icons and its block textures are all small pictures drawn from code (no hand-painted files to
lose, byte-identical on every run). This module is the drawing kit they share: a Canvas that works in a 32 x 32 design space but renders at 8x and
shrinks with a box filter (smooth, deterministic), smooth splines for organic shapes, and one function per glyph. Pillow only.

    Canvas(size=32, ss=8)   .poly .circle .ring .line .arc .spline .erase_circle .shadowed()   .image()  -> RGBA PIL image at size x size
    GLYPHS[name](canvas, light, mid, dark)                                                       draws one glyph inside the 32 x 32 disc

Every glyph is drawn inside a circle of radius 10 around (16, 16) so the medallion's rim never touches it.
"""
import math

from PIL import Image, ImageChops, ImageDraw, ImageFilter


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


class Canvas:
    def __init__(self, size=32, ss=8):
        self.size, self.ss = size, ss
        self.n = size * ss
        self.img = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def P(self, x, y):
        k = self.ss * self.size / 32.0
        return (x * k, y * k)

    def W(self, w):
        return max(1, int(round(w * self.ss * self.size / 32.0)))

    def col(self, c, a=255):
        return (c[0], c[1], c[2], a)

    # ---- primitives (coordinates in the 32 design space)
    def poly(self, pts, fill):
        self.d.polygon([self.P(*p) for p in pts], fill=self.col(fill) if len(fill) == 3 else fill)

    def circle(self, cx, cy, r, fill):
        (x0, y0), (x1, y1) = self.P(cx - r, cy - r), self.P(cx + r, cy + r)
        self.d.ellipse([x0, y0, x1, y1], fill=self.col(fill) if len(fill) == 3 else fill)

    def ellipse(self, cx, cy, rx, ry, fill):
        (x0, y0), (x1, y1) = self.P(cx - rx, cy - ry), self.P(cx + rx, cy + ry)
        self.d.ellipse([x0, y0, x1, y1], fill=self.col(fill) if len(fill) == 3 else fill)

    def ring(self, cx, cy, r, w, fill):
        (x0, y0), (x1, y1) = self.P(cx - r, cy - r), self.P(cx + r, cy + r)
        self.d.ellipse([x0, y0, x1, y1], outline=self.col(fill) if len(fill) == 3 else fill, width=self.W(w))

    def line(self, pts, w, fill, round_ends=True):
        c = self.col(fill) if len(fill) == 3 else fill
        P = [self.P(*p) for p in pts]
        self.d.line(P, fill=c, width=self.W(w), joint="curve")
        if round_ends:
            r = self.W(w) / 2.0
            for (x, y) in (P[0], P[-1]):
                self.d.ellipse([x - r, y - r, x + r, y + r], fill=c)

    def arc(self, cx, cy, r, a0, a1, w, fill):
        pts = [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / 40.0)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / 40.0))) for i in range(41)]
        self.line(pts, w, fill)

    def spline(self, pts, closed=True, n=10):
        out = []
        m = len(pts)
        for i in range(m if closed else m - 1):
            p0, p1, p2, p3 = (pts[(i - 1) % m], pts[i % m], pts[(i + 1) % m], pts[(i + 2) % m]) if closed else (
                pts[max(i - 1, 0)], pts[i], pts[i + 1], pts[min(i + 2, m - 1)])
            for k in range(n):
                t = k / float(n)
                t2, t3 = t * t, t * t * t
                out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2 + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
        if not closed:
            out.append(pts[-1])
        return out

    def erase_circle(self, cx, cy, r):
        (x0, y0), (x1, y1) = self.P(cx - r, cy - r), self.P(cx + r, cy + r)
        self.d.ellipse([x0, y0, x1, y1], fill=(0, 0, 0, 0))

    def erase_poly(self, pts):
        self.d.polygon([self.P(*p) for p in pts], fill=(0, 0, 0, 0))

    def image(self):
        return self.img.resize((self.size, self.size), Image.BOX)


# ====================================================================================================== glyphs
# Each glyph: (canvas, light, mid, dark) with colours as RGB tuples. They draw a body in `mid`, highlights in `light`, shading/outline in `dark`.

def g_flame(c, L, M, D):
    out = c.spline([(16, 5), (19.5, 10.5), (22.5, 15), (21.5, 20), (18.5, 23.8), (15.5, 24.6), (12, 23.4), (9.8, 19.6), (10.6, 15), (13, 12), (13.4, 8.6)])
    c.poly(out, M)
    c.poly(c.spline([(16.5, 12), (19, 16), (19, 20), (16.2, 23), (13.2, 21.4), (13.2, 17.4), (15.2, 15)]), L)
    c.poly(c.spline([(16.4, 17.5), (17.8, 20), (16.4, 22.4), (14.8, 20.6)]), (255, 250, 220))


def g_drop(c, L, M, D):
    c.poly(c.spline([(16, 5.5), (19.5, 11.5), (22.2, 16.5), (21.2, 21), (16, 24.6), (10.8, 21), (9.8, 16.5), (12.5, 11.5)]), M)
    c.poly(c.spline([(13.2, 15), (14.6, 12.6), (14.9, 16.6), (13.8, 20.2), (12.2, 18.4)]), L)
    c.circle(13.6, 19.6, 1.1, (255, 255, 255))


def g_swirl(c, L, M, D):
    c.line([(6.5, 12.0), (17.0, 12.0)], 1.8, M)
    c.arc(17.0, 9.6, 2.4, 90, -190, 1.8, M)
    c.line([(4.5, 16.8), (21.5, 16.8)], 1.8, L)
    c.arc(21.5, 19.3, 2.5, -90, 180, 1.8, L)
    c.line([(8.5, 22.2), (15.0, 22.2)], 1.8, M)
    c.arc(15.0, 20.2, 2.0, 90, -190, 1.8, M)


def g_mountain(c, L, M, D):
    c.poly([(5.5, 23.5), (13.5, 8.5), (21.5, 23.5)], M)
    c.poly([(13.0, 23.5), (20.5, 12.5), (27.0, 23.5)], mix(M, D, 0.35))
    c.poly([(13.5, 8.5), (10.9, 13.4), (12.6, 12.2), (13.8, 14.2), (15.6, 12.0), (16.1, 12.8)], (245, 248, 255))
    c.poly([(20.5, 12.5), (18.7, 15.9), (20.2, 15.2), (21.2, 16.6), (22.6, 15.0)], L)


def g_leaf(c, L, M, D):
    c.poly(c.spline([(8.5, 23.5), (9.5, 15.5), (14.5, 9.0), (23.5, 7.0), (22.5, 16.0), (17.5, 22.0)]), M)
    c.poly(c.spline([(10.5, 21), (12.5, 15.5), (16.5, 11.5), (21.0, 9.2), (18.0, 15.5), (14.0, 19.5)]), mix(M, L, 0.55))
    c.line([(8.0, 24.5), (13.0, 19.0), (20.5, 10.0)], 1.0, D)
    c.line([(12.2, 20.0), (11.0, 16.6)], 0.8, D)
    c.line([(15.4, 16.4), (18.8, 17.8)], 0.8, D)


def g_snow(c, L, M, D):
    for k in range(6):
        a = math.radians(k * 60 + 90)
        ex, ey = 16 + 9.4 * math.cos(a), 16 - 9.4 * math.sin(a)
        c.line([(16, 16), (ex, ey)], 1.5, M)
        for t in (0.55, 0.78):
            bx, by = 16 + 9.4 * t * math.cos(a), 16 - 9.4 * t * math.sin(a)
            for s in (-1, 1):
                b = a + s * math.radians(50)
                c.line([(bx, by), (bx + 2.6 * math.cos(b), by - 2.6 * math.sin(b))], 1.1, L)
    c.circle(16, 16, 1.8, (255, 255, 255))


def g_bolt(c, L, M, D):
    pts = [(18.5, 5.0), (10.0, 17.0), (15.0, 17.0), (12.5, 27.0), (22.5, 13.0), (17.0, 13.0), (20.5, 5.0)]
    c.poly(pts, M)
    c.poly([(18.5, 5.0), (12.2, 15.0), (15.6, 15.0), (14.4, 21.0), (19.0, 12.0), (16.0, 12.0), (18.6, 6.5)], L)


def g_sun(c, L, M, D):
    for k in range(8):
        a = math.radians(k * 45)
        tip = (16 + 10.2 * math.cos(a), 16 + 10.2 * math.sin(a))
        l, r = (16 + 6.6 * math.cos(a - 0.26), 16 + 6.6 * math.sin(a - 0.26)), (16 + 6.6 * math.cos(a + 0.26), 16 + 6.6 * math.sin(a + 0.26))
        c.poly([l, tip, r], M)
    c.circle(16, 16, 5.6, M)
    c.circle(16, 16, 4.0, L)
    c.circle(14.6, 14.6, 1.5, (255, 255, 255))


def g_moon(c, L, M, D):
    c.circle(15.5, 16, 9.0, M)
    c.erase_circle(20.2, 13.4, 7.6)
    c.circle(21.5, 9.5, 1.0, L)
    c.circle(23.6, 15.0, 0.8, L)
    c.circle(19.5, 22.0, 0.9, L)


def g_gear(c, L, M, D):
    for k in range(8):
        a = math.radians(k * 45)
        ca, sa = math.cos(a), math.sin(a)
        px, py = -sa, ca
        pts = [(16 + 6.5 * ca + px * 1.8, 16 + 6.5 * sa + py * 1.8), (16 + 10.0 * ca + px * 1.4, 16 + 10.0 * sa + py * 1.4),
               (16 + 10.0 * ca - px * 1.4, 16 + 10.0 * sa - py * 1.4), (16 + 6.5 * ca - px * 1.8, 16 + 6.5 * sa - py * 1.8)]
        c.poly(pts, M)
    c.circle(16, 16, 7.6, M)
    c.circle(16, 16, 4.6, D)
    c.circle(16, 16, 2.4, L)
    c.arc(16, 16, 6.0, 200, 290, 0.9, L)


def g_skull(c, L, M, D):
    c.poly(c.spline([(8.8, 14), (9.8, 8.8), (16, 6.2), (22.2, 8.8), (23.2, 14), (21.4, 17.6), (20.2, 20.2), (11.8, 20.2), (10.6, 17.6)]), M)
    c.poly([(12.0, 19.5), (20.0, 19.5), (19.6, 24.0), (12.4, 24.0)], M)
    c.erase_circle(12.8, 14.0, 2.4)
    c.erase_circle(19.2, 14.0, 2.4)
    c.erase_poly([(16, 16.6), (14.7, 19.2), (17.3, 19.2)])
    for x in (14.3, 16.0, 17.7):
        c.line([(x, 20.8), (x, 23.4)], 0.7, D, False)
    c.circle(11.4, 10.6, 1.0, L)


def _shrink(pts, k=0.8):
    return [(16 + (x - 16) * k, 16 + (y - 16) * k) for x, y in pts]


def g_blade(c, L, M, D):
    c.poly(_shrink([(23.8, 5.6), (25.8, 7.6), (13.6, 19.8), (11.6, 17.8)]), M)
    c.poly(_shrink([(23.8, 5.6), (24.6, 6.4), (12.6, 18.4), (11.8, 17.9)]), L)
    c.poly(_shrink([(9.0, 16.2), (15.2, 22.4), (13.8, 23.8), (7.6, 17.6)]), mix(M, D, 0.4))
    c.line(_shrink([(10.6, 21.4), (6.6, 25.4)]), 1.8, mix(M, D, 0.6))
    c.arc(12.0, 21.0, 7.4, 100, 175, 0.9, L)


def g_ripple(c, L, M, D):
    for r, w, col in ((9.6, 1.4, M), (6.6, 1.5, L), (3.6, 1.6, M)):
        c.ring(16, 16, r, w, col)
    c.circle(16, 16, 1.8, (255, 255, 255))
    c.circle(22.8, 9.4, 1.2, L)


def g_pane(c, L, M, D):
    pts = [(10.5, 7.0), (21.5, 7.0), (23.5, 24.5), (8.5, 24.5)]
    c.poly(pts, mix(M, D, 0.45))
    c.poly([(11.8, 8.4), (20.2, 8.4), (21.8, 23.0), (10.2, 23.0)], M)
    c.poly([(12.6, 9.4), (15.0, 9.4), (11.6, 22.2), (10.8, 22.2)], L)
    c.poly([(17.0, 9.4), (18.4, 9.4), (15.0, 22.2), (13.6, 22.2)], mix(L, (255, 255, 255), 0.5))


def g_lock(c, L, M, D):
    c.arc(16, 12.5, 5.0, 180, 360, 2.2, L)
    c.line([(11.0, 12.5), (11.0, 14.5)], 2.2, L, False)
    c.line([(21.0, 12.5), (21.0, 14.5)], 2.2, L, False)
    c.poly([(9.0, 14.0), (23.0, 14.0), (23.0, 24.5), (9.0, 24.5)], M)
    c.circle(16, 18.4, 1.9, D)
    c.line([(16, 19), (16, 22.2)], 1.6, D, False)
    c.line([(10.2, 15.0), (22.0, 15.0)], 0.8, L, False)


def g_web(c, L, M, D):
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        c.line([(16, 16), (16 + 10 * math.cos(a), 16 + 10 * math.sin(a))], 0.9, M)
    for r in (3.4, 6.2, 9.0):
        pts = [(16 + r * math.cos(math.radians(k * 45 + 22.5)), 16 + r * math.sin(math.radians(k * 45 + 22.5))) for k in range(8)]
        pts.append(pts[0])
        c.line(pts, 0.9, L, False)
    c.circle(16, 16, 1.6, (255, 255, 255))


def g_bloodcell(c, L, M, D):
    c.poly(c.spline([(15, 6.0), (19.0, 12.0), (21.4, 17.0), (20.0, 21.6), (15, 25.0), (10.0, 21.6), (8.6, 17.0), (11.0, 12.0)]), M)
    c.poly(c.spline([(11.4, 15.2), (12.8, 12.6), (13.6, 16.4), (12.6, 20.0), (10.8, 18.6)]), L)
    c.circle(12.4, 21.2, 1.0, (255, 235, 235))
    c.poly(c.spline([(23.2, 12.5), (24.9, 15.4), (25.4, 17.4), (23.2, 19.4), (21.0, 17.4), (21.6, 15.4)]), mix(M, L, 0.35))


def g_orbit(c, L, M, D):
    c.circle(16, 16, 5.0, M)
    c.circle(14.6, 14.6, 1.8, L)
    pts = [(16 + 10.0 * math.cos(math.radians(t)) * math.cos(math.radians(-24)) - 3.4 * math.sin(math.radians(t)) * math.sin(math.radians(-24)),
            16 + 10.0 * math.cos(math.radians(t)) * math.sin(math.radians(-24)) + 3.4 * math.sin(math.radians(t)) * math.cos(math.radians(-24))) for t in range(0, 361, 8)]
    c.line(pts, 1.1, L, False)
    c.circle(pts[10][0], pts[10][1], 1.5, (255, 255, 255))


def g_portal(c, L, M, D):
    c.ellipse(16, 16, 6.4, 10.0, M)
    c.ellipse(16, 16, 4.4, 8.0, D)
    c.arc(16, 16, 3.4, 30, 330, 1.0, L)
    c.arc(16, 16, 1.5, 120, 460, 0.9, (255, 255, 255))
    c.line([(7.6, 9.0), (9.6, 11.6)], 1.0, L)
    c.line([(24.4, 23.0), (22.4, 20.4)], 1.0, L)
    c.circle(8.2, 21.0, 0.9, L)
    c.circle(23.8, 11.0, 0.9, L)


def g_wisp(c, L, M, D):
    c.poly(c.spline([(21, 6.5), (22.6, 12.5), (21.2, 18.5), (16.5, 23.4), (11.6, 22.0), (9.8, 17.6), (12.4, 13.4), (17.0, 11.5), (19.6, 8.8)]), M)
    c.poly(c.spline([(19.0, 12.5), (19.6, 16.4), (17.0, 20.2), (13.8, 19.4), (14.6, 16.4), (17.0, 14.6)]), L)
    c.circle(12.2, 9.4, 1.1, L)
    c.circle(8.8, 12.4, 0.8, L)


def g_eye(c, L, M, D):
    c.poly(c.spline([(5.5, 16), (10.5, 10.4), (16, 8.6), (21.5, 10.4), (26.5, 16), (21.5, 21.6), (16, 23.4), (10.5, 21.6)]), M)
    c.circle(16, 16, 5.0, L)
    c.circle(16, 16, 2.6, D)
    c.circle(14.6, 14.6, 1.1, (255, 255, 255))
    c.arc(16, 16, 9.6, 215, 325, 0.8, L)


def _star4(cx, cy, r, w=0.28, rot=0.0):
    pts = []
    for k in range(8):
        rr = r if k % 2 == 0 else r * w
        a = math.radians(k * 45 + rot - 90)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def g_spark(c, L, M, D):
    c.poly(_star4(16, 16, 10.0, 0.22), M)
    c.poly(_star4(16, 16, 6.6, 0.28, 45), L)
    c.circle(16, 16, 1.8, (255, 255, 255))
    c.circle(22.6, 9.4, 1.1, L)
    c.circle(9.6, 22.4, 0.9, L)


def g_stars(c, L, M, D):
    c.poly(_star4(13.0, 14.0, 7.2, 0.26), M)
    c.poly(_star4(22.0, 21.0, 4.4, 0.28), L)
    c.poly(_star4(21.4, 8.6, 3.2, 0.3), L)
    c.circle(8.6, 22.6, 1.0, M)
    c.circle(13.0, 14.0, 1.5, (255, 255, 255))


def g_flameeye(c, L, M, D):
    c.poly(c.spline([(9.0, 24.5), (8.6, 14), (12.2, 7.6), (16, 6.4), (19.8, 7.6), (23.4, 14), (23.0, 24.5), (20.6, 22.4), (18.2, 24.5), (16, 22.4), (13.8, 24.5), (11.4, 22.4)], n=6), M)
    c.ellipse(12.8, 14.0, 1.8, 2.6, D)
    c.ellipse(19.2, 14.0, 1.8, 2.6, D)
    c.circle(12.4, 13.2, 0.7, L)
    c.circle(18.8, 13.2, 0.7, L)
    c.ellipse(16, 19.0, 1.3, 1.9, D)


def g_scale(c, L, M, D):
    for (x0, y0, x1, y1, col) in ((7.6, 10.5, 13.4, 24.0, M), (12.4, 7.0, 18.8, 25.5, L), (17.8, 9.5, 23.8, 23.0, M)):
        dx, dy = x1 - x0, y1 - y0
        bulge = 3.0
        out = [(x0, y0), (x0 + dx * 0.34 + bulge, y0 + dy * 0.32), (x0 + dx * 0.72 + bulge * 1.1, y0 + dy * 0.7), (x1, y1),
               (x0 + dx * 0.66 + bulge * 0.2, y0 + dy * 0.7), (x0 + dx * 0.3 + bulge * 0.15, y0 + dy * 0.34)]
        c.poly(c.spline(out, n=8), col)
    c.circle(20.5, 7.5, 1.1, L)
    c.circle(9.5, 24.5, 0.9, L)


def g_crown(c, L, M, D):
    c.poly([(7.0, 22.5), (8.0, 11.0), (12.4, 15.5), (16, 8.0), (19.6, 15.5), (24.0, 11.0), (25.0, 22.5)], M)
    c.poly([(7.0, 22.5), (25.0, 22.5), (25.0, 25.0), (7.0, 25.0)], mix(M, D, 0.35))
    for (x, y) in ((8.0, 10.2), (16, 7.0), (24.0, 10.2)):
        c.circle(x, y, 1.5, L)
    c.circle(16, 19.0, 1.7, (255, 255, 255))
    c.line([(10.2, 18.0), (11.6, 21.2)], 0.9, L, False)
    c.line([(21.8, 18.0), (20.4, 21.2)], 0.9, L, False)


def g_d6(c, L, M, D):
    c.poly(c.spline([(8.5, 9.5), (9.5, 8.5), (22.5, 8.5), (23.5, 9.5), (23.5, 22.5), (22.5, 23.5), (9.5, 23.5), (8.5, 22.5)], n=3), M)
    c.poly([(9.5, 9.4), (22.5, 9.4), (22.5, 10.6), (9.5, 10.6)], L)
    for (x, y) in ((12.2, 12.2), (19.8, 12.2), (16, 16), (12.2, 19.8), (19.8, 19.8)):
        c.circle(x, y, 1.5, D)


def g_tree(c, L, M, D):
    c.poly([(14.6, 15.0), (17.4, 15.0), (18.2, 25.0), (13.8, 25.0)], mix(M, D, 0.55))
    for (x, y, r) in ((16, 10.0, 5.2), (11.4, 13.2, 4.2), (20.6, 13.2, 4.2), (16, 15.0, 4.4)):
        c.circle(x, y, r, M)
    for (x, y, r) in ((14.4, 8.8, 2.0), (19.4, 12.0, 1.4), (10.4, 12.4, 1.3)):
        c.circle(x, y, r, L)
    c.line([(14.0, 25.0), (10.6, 26.2)], 1.0, mix(M, D, 0.55))
    c.line([(18.0, 25.0), (21.4, 26.2)], 1.0, mix(M, D, 0.55))


def g_clock(c, L, M, D):
    c.circle(16, 16, 9.6, M)
    c.circle(16, 16, 8.0, D)
    for k in range(12):
        a = math.radians(k * 30)
        r0 = 6.2 if k % 3 == 0 else 6.9
        c.line([(16 + r0 * math.sin(a), 16 - r0 * math.cos(a)), (16 + 7.6 * math.sin(a), 16 - 7.6 * math.cos(a))], 0.9 if k % 3 == 0 else 0.6, L, False)
    c.line([(16, 16), (16, 10.4)], 1.4, (255, 255, 255))
    c.line([(16, 16), (20.4, 18.6)], 1.2, L)
    c.circle(16, 16, 1.3, (255, 255, 255))


def g_slash(c, L, M, D):
    c.ring(16, 16, 8.6, 2.0, M)
    c.erase_poly([(12.5, 5.0), (27.0, 5.0), (27.0, 18.5), (24.5, 19.5), (19.5, 12.5), (13.5, 9.5)])
    c.erase_poly([(18.0, 23.0), (21.0, 27.0), (10.0, 27.0), (5.0, 21.0), (7.5, 19.5), (12.5, 22.5)])
    c.poly([(26.5, 5.5), (10.2, 24.8), (9.2, 24.2), (25.8, 5.0)], L)
    c.poly([(26.5, 5.5), (27.6, 6.6), (11.0, 26.0), (9.6, 25.4)], (255, 255, 255))
    c.circle(16, 16, 1.4, M)


GLYPHS = {
    "flame": g_flame, "drop": g_drop, "swirl": g_swirl, "mountain": g_mountain, "leaf": g_leaf, "snow": g_snow, "bolt": g_bolt, "sun": g_sun,
    "moon": g_moon, "gear": g_gear, "skull": g_skull, "blade": g_blade, "ripple": g_ripple, "pane": g_pane, "lock": g_lock, "web": g_web,
    "bloodcell": g_bloodcell, "orbit": g_orbit, "portal": g_portal, "wisp": g_wisp, "eye": g_eye, "spark": g_spark, "stars": g_stars,
    "flameeye": g_flameeye, "scale": g_scale, "crown": g_crown, "d6": g_d6, "tree": g_tree, "clock": g_clock, "slash": g_slash,
}


def medallion(glyph, tint, size=32):
    """One circular aptitude icon: a dark disc with a thin rim in the aptitude's colour and the glyph on a soft glow. Returns an RGBA PIL image."""
    base = Canvas(size)
    t = rgb(tint)
    dark = mix(t, (6, 8, 16), 0.86)
    mid_dark = mix(t, (6, 8, 16), 0.72)
    # disc: a vertical gradient built from stacked circles clipped to the disc
    base.circle(16, 16, 15.6, mix(t, (0, 0, 0), 0.55) + (255,))
    base.circle(16, 16, 14.8, dark + (255,))
    grad = Image.new("RGBA", base.img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    n = base.n
    for y in range(n):
        f = y / float(n - 1)
        col = mix(mid_dark, dark, f)
        gd.line([(0, y), (n, y)], fill=col + (255,))
    mask = Image.new("L", base.img.size, 0)
    md = ImageDraw.Draw(mask)
    (x0, y0), (x1, y1) = base.P(1.4, 1.4), base.P(30.6, 30.6)
    md.ellipse([x0, y0, x1, y1], fill=255)
    base.img = Image.composite(grad, base.img, mask)
    base.d = ImageDraw.Draw(base.img)
    base.ring(16, 16, 14.9, 1.1, mix(t, (255, 255, 255), 0.15))
    base.ring(16, 16, 13.4, 0.5, mix(t, (0, 0, 0), 0.35))
    # glow under the glyph
    glow = Image.new("RGBA", base.img.size, (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    (gx0, gy0), (gx1, gy1) = base.P(6.5, 6.5), base.P(25.5, 25.5)
    gdraw.ellipse([gx0, gy0, gx1, gy1], fill=mix(t, (0, 0, 0), 0.25) + (120,))
    glow = glow.filter(ImageFilter.GaussianBlur(base.ss * 2.4))
    base.img = Image.alpha_composite(base.img, glow)
    # glyph with a dark shadow underneath
    layer = Canvas(size)
    L, M, D = mix(t, (255, 255, 255), 0.62), mix(t, (255, 255, 255), 0.18), mix(t, (0, 0, 0), 0.55)
    GLYPHS[glyph](layer, L, M, D)
    shadow = Image.new("RGBA", layer.img.size, (0, 0, 0, 0))
    shadow.putalpha(layer.img.getchannel("A").point(lambda v: int(v * 0.65)))
    shadow = ImageChops.offset(shadow, base.ss // 2, base.ss // 2)
    base.img = Image.alpha_composite(base.img, shadow)
    base.img = Image.alpha_composite(base.img, layer.img)
    # keep everything inside the disc
    base.img.putalpha(ImageChops.multiply(base.img.getchannel("A"), Image.new("L", base.img.size, 255)))
    return base.image()
