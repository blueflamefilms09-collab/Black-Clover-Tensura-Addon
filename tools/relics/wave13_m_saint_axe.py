"""Axe of The Crimson Saint (phone screenshot of a card, "Battle Axe - Very Rare", requires attunement): a long dark wooden haft with a cream leather wrap and a
trailing cord at the butt, gold bands streaked with red and a gold inlay strip, a grey steel socket with three gold studs and a gold emblem, and a grey steel head: a
broad blade scored with glowing red blood cracks and a line of runic text along its upper edge, and a small curved poll on the other side.

Reference: the screenshot shown at 900 x 2000 (the file is 1080 x 2400). The axe runs diagonally: the butt's cord end at (738, 1310), the poll at (470, 380): 984 px.
Measured on that picture: s = px from the butt along the haft, o = px out from the haft toward the blade (the blade is on the left of the card).
Axis: y up the haft from the butt to the head; the blade faces -x; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.05                                       # a battle axe in hand
S_TOP = 1040.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TOP)               # cm per px (0.101)
R_HAFT = 13.0 * K * 1.05                             # the haft is about 26 px thick
S_WRAP = (26.0, 190.0)                               # the cream leather wrap with its three bands at the butt (px 26 to 190)
S_BANDS = (692.0, 782.0)                             # the gold bands with the red streaks under the socket
S_INLAY = (560.0, 690.0)                             # the gold inlay strip along the haft
S_SOCKET = (782.0, 975.0)                            # the grey socket, 36 px wide, with three gold studs and the emblem
R_SOCKET = 19.0 * K
HEAD = [(714.0, 121.0), (722.0, 112.0), (745.0, 18.0)]  # the blade's lower (beard) corner: (s, o) of the slanted bottom
O_IN = 18.0                                          # the blade's inner edge sits behind the socket
EDGE = [(714.0, 121.0), (760.0, 150.0), (820.0, 190.0), (900.0, 232.0), (973.0, 259.0)]   # the cutting edge from the beard to the toe: (s, o) px
TOP = [(973.0, 259.0), (985.0, 200.0), (990.0, 100.0), (975.0, 20.0)]                      # the upper edge back to the socket
BLADE_T = 2.4                                        # thick at the socket, ground to an edge


def y(s):
    return s * K


def edge_out(s):
    return float(np.interp(s, [p[0] for p in EDGE], [p[1] for p in EDGE]))


def inner(s):
    return float(np.interp(s, [714.0, 745.0, 1000.0], [121.0, O_IN, O_IN]))


def top_s(o):
    return float(np.interp(o, [20.0, 100.0, 200.0, 259.0], [975.0, 990.0, 985.0, 973.0]))


def build():
    m = ml.Mesh("axe_of_the_crimson_saint")
    # ---- 1 the haft: dark wood with a slight swell in the middle and a flared butt
    hf = w.add(m, "haft", "wood", base=(70, 44, 26), uv_mode="loft")
    ml.loft(hf, "y", (0, 0, 0), [(y(0), 0.8, 0.8), (y(40), 1.25, 1.25), (y(300), 1.0, 1.0), (y(600), 0.92, 0.92), (y(800), 1.0, 1.0), (y(S_SOCKET[0]) + 0.5, 1.1, 1.1)], ml.circle(R_HAFT, 12))
    # ---- 2 the cream leather wrap at the butt, 3 its raised cream bands, 4 the trailing cord
    wr = w.add(m, "haft_wrap", "leather", base=(226, 214, 176), uv_mode="loft")
    ml.loft(wr, "y", (0, 0, 0), [(y(S_WRAP[0]), 1.2, 1.2), (y(120), 1.28, 1.28), (y(S_WRAP[1]), 1.2, 1.2)], ml.circle(R_HAFT * 1.04, 12))
    wb = w.add(m, "wrap_bands", "cloth", base=(244, 236, 210), wear=0.3, smooth=60)
    for s in (130.0, 152.0, 174.0):
        ml.collar(wb, y(s), R_HAFT * 1.5, 0.9, lip=0.25, sides=12)
    cd = w.add(m, "cord", "cloth", base=(236, 228, 200), wear=0.2, smooth=60)
    ml.sweep(cd, [(0.0, y(S_WRAP[0]) + 0.5, 0.0), (0.4, y(14), 0.2), (0.9, y(0), 0.5), (1.0, y(-30), 0.4)], ml.circle(1.0, 6), scales=lambda t: (0.22 * (1.0 - 0.5 * t), 0.22 * (1.0 - 0.5 * t)))
    # ---- 5 the gold bands streaked with red, 6 the gold inlay strip
    gb = w.add(m, "gold_bands", "gold", base=(214, 164, 58), wear=0.4, smooth=55)
    for i in range(4):
        ml.collar(gb, y(S_BANDS[0] + 4 + i * 22.0), R_HAFT * 1.22, 1.2, lip=0.3, sides=12)
    rs = w.add(m, "red_streaks", "cloth", base=(190, 30, 30), smooth=0)
    for i in range(7):
        a = 2 * math.pi * i / 7
        ml.bar(rs, (R_HAFT * 1.25 * math.cos(a), y(S_BANDS[0] + 2), R_HAFT * 1.25 * math.sin(a)), (R_HAFT * 1.25 * math.cos(a + 0.2), y(S_BANDS[1] - 4), R_HAFT * 1.25 * math.sin(a + 0.2)), 0.14, 0.06,
               up=(math.cos(a), 0, math.sin(a)), taper=0.8)
    gi = w.add(m, "gold_inlay", "gold", base=(200, 156, 56), smooth=40)
    ml.bar(gi, (0.0, y(S_INLAY[0]), R_HAFT * 0.95), (0.0, y(S_INLAY[1]), R_HAFT * 0.95), 0.9, 0.18, up=(0, 0, 1), taper=3.0)
    # ---- 7 the steel socket (a tapering sleeve, wider under the head), 8 its gold studs and emblem
    sk = w.add(m, "socket", "steel", base=(150, 154, 164), smooth=45)
    ml.loft(sk, "y", (0, 0, 0), [(y(S_SOCKET[0]), R_SOCKET * 0.8, R_SOCKET * 0.8), (y(S_SOCKET[0] + 18), 1.0 * R_SOCKET, 1.0 * R_SOCKET), (y(S_SOCKET[1]), 1.05 * R_SOCKET, 1.05 * R_SOCKET), (y(S_SOCKET[1] + 14), 0.7 * R_SOCKET, 0.7 * R_SOCKET)],
            ml.octagon(1.0, 1.0, 0.25))
    sd = w.add(m, "socket_gold", "gold", base=(222, 172, 60), smooth=50)
    for i, s in enumerate((905.0, 930.0, 955.0)):
        ml.stud(sd, (0.0, y(s), R_SOCKET * 1.0), 0.42, 0.35, (0, 0, 1), sides=8)
    ml.bar(sd, (0.0, y(820), R_SOCKET * 1.05), (0.0, y(868), R_SOCKET * 1.05), 1.1, 0.2, up=(0, 0, 1), taper=1.5)       # the emblem plate
    # ---- 9 the blade: a planform lofted along the haft, edge at -x, thick at the socket and ground away to the cutting edge
    bl = w.add(m, "blade", "steel", base=(156, 160, 170), smooth=40, uv_mode="loft")
    ss = [714.0, 722.0, 730.0, 745.0, 790.0, 840.0, 900.0, 945.0, 970.0, 982.0, 990.0]
    stations = []
    for s in ss:
        lo, hi = inner(s), edge_out(s) if s <= 973 else float(np.interp(s, [973.0, 990.0], [259.0, 40.0]))
        if s > 973:
            hi = float(np.interp(s, [973.0, 982.0, 990.0], [259.0, 180.0, 60.0]))
        wd = max(0.0, (hi - lo) * K)
        stations.append((y(s), wd if wd > 0.2 else 0.0, 1.0 if wd > 0.2 else 0.0, -(lo + hi) / 2 * K, 0.0))
    ml.loft(bl, "y", (0, 0, 0), stations, _edge_section(), caps=(True, True))
    # ---- 10 blood cracks: raised thin red veins wandering over the blade, 11 a line of runes along the upper edge
    bv = w.add(m, "blood_cracks", "crystal", base=(210, 30, 36), glow=True, smooth=0)
    rng = np.random.default_rng(13)
    for seed_o, seed_s in ((60, 840), (110, 800), (150, 880), (90, 930), (190, 860), (130, 760), (170, 950)):
        o, s = float(seed_o), float(seed_s)
        for _ in range(5):
            o2 = min(edge_out(s) - 14, max(inner(s) + 6, o + rng.uniform(-30, 34)))
            s2 = s + rng.uniform(-34, 20)
            if inner(s2) < o2 < edge_out(max(714.0, min(973.0, s2))) - 10 and 730 < s2 < 970:
                ml.bar(bv, (-o * K, y(s), BLADE_T * 0.40), (-o2 * K, y(s2), BLADE_T * 0.40), 0.22, 0.07, up=(0, 0, 1), taper=0.3)
                o, s = o2, s2
    rn = w.add(m, "rune_line", "bronze", base=(120, 90, 60), smooth=0)
    for i in range(9):                                                                               # nine rune glyphs (a stroke and a cross-stroke each) along the upper edge, card (130..330, 505..470)
        o = 70.0 + i * 19.0
        sv = float(np.interp(o, [20.0, 100.0, 200.0, 259.0], [958.0, 972.0, 968.0, 958.0]))
        ml.bar(rn, (-o * K, y(sv - 5), BLADE_T * 0.40), (-o * K, y(sv + 5), BLADE_T * 0.40), 0.16, 0.05, up=(0, 0, 1))
        ml.bar(rn, (-(o - 3.5) * K, y(sv + (i % 3) - 1), BLADE_T * 0.40), (-(o + 3.5) * K, y(sv + (i % 3) - 1), BLADE_T * 0.40), 0.14, 0.05, up=(0, 0, 1))
    # ---- 12 the poll: a small curved horn on the haft's other side
    pl = w.add(m, "poll", "steel", base=(140, 144, 156), smooth=45)
    ml.sweep(pl, [(R_SOCKET * 0.6, y(940.0), 0.0), (R_SOCKET * 2.3, y(975.0), 0.0), (R_SOCKET * 3.6, y(1015.0), 0.0), (R_SOCKET * 3.9, y(S_TOP), 0.0)], ml.octagon(1.0, 1.0, 0.3),
             scales=lambda t: (1.1 * (1.0 - 0.7 * t) if t < 0.97 else 0.05, 1.5 * (1.0 - 0.6 * t)), up=(0, 0, 1))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blood_cracks"), lambda c: (w.tint(c.img, (255, 70, 60), 0.6), w.set_glow(c, (255, 50, 40), 1.0, core=0.4)))
    w.paint_part(atlas, m.part("red_streaks"), lambda c: w.set_glow(c, (255, 60, 50), 0.6, core=0.3))
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (255, 40, 40), 0.03, core=0.0))
    ref = w.reference("p2/Relics of the world part 2/Screenshot_20261013_223222_Opera.jpg", (100, 340, 1000, 1700), "axe_of_the_crimson_saint")
    w.finish("axe_of_the_crimson_saint", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.14, 0.5, 1.0), ref=ref)


def _edge_section():
    """The blade section (counter-clockwise): the cutting edge at -x, the thick back at +x against the socket."""
    return [(-0.5, 0.0), (-0.12, -0.5 * 0.55 * BLADE_T / 1.0 * 0.35), (0.5, -0.5 * BLADE_T * 0.6), (0.5, 0.5 * BLADE_T * 0.6), (-0.12, 0.5 * 0.55 * BLADE_T / 1.0 * 0.35)]
