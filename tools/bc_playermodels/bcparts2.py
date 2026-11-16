"""More part generators (bone and metal armour, belts, pouch): the second half of bcparts.py. Same conventions: a `Build`, a layer ("main" / "fine" / "fx")."""
import bcparts as P
from bcparts import HEAD, elf_ears  # noqa: F401


def ribcage(b, color="#e6e0d0", dark="#8a8070", layer="main", y=22.6, rows=4, z=-2.55, spread=3.2):
    """A ribcage over the chest: a sternum and `rows` rib pairs (each an angled box that wraps toward the sides)."""
    geo = b.g(layer)
    bone = b.t_horn(color, tip=dark, tw=8, th=16, seed=21, ridges=3)
    geo.cube("body", (-0.55, y - rows * 2.0 - 0.4, z - 0.15), (1.1, rows * 2.0 + 0.6, 0.9), bone)
    for i in range(rows):
        yy = y - i * 2.0
        wdt = spread * (1.0 - 0.1 * i)
        for s in (-1, 1):
            geo.cube("body", (s * (0.4 + wdt / 2.0) - wdt / 2.0, yy - 1.0, z - 0.1 * (1 if i % 2 else 0)), (wdt, 0.95, 0.95), bone, rot=(0.0, 0.0, s * 9.0), pivot=(s * 0.5, yy - 0.5, z))
            geo.cube("body", (s * (wdt + 0.4) - 0.5, yy - 1.1, z + 0.35), (1.0, 0.95, 2.6), bone)


def pauldrons(b, color="#e6e0d0", dark="#8a8070", spikes=3, layer="main", size=5.6, spike_len=4.0, sides=("r", "l")):
    """A shoulder plate with `spikes` spikes on each arm (an arm-anchored box with bone cards on top)."""
    geo = b.g(layer)
    bone = b.t_horn(color, tip=dark, tw=8, th=16, seed=22, ridges=3)
    sp = b.t_spike(color, tip="#fff8e8", seed=23)
    for sd in sides:
        s = -1 if sd == "r" else 1
        cx = s * 6.0
        geo.cube("arm_" + sd, (cx - size / 2, 22.0, -size / 2 + 0.1), (size, 2.6, size - 0.2), bone)
        for k in range(spikes):
            z = -1.9 + k * (3.8 / max(1, spikes - 1)) if spikes > 1 else 0.0
            x = cx + s * 0.9
            piv = (x, 24.4, z)
            geo.cube("arm_" + sd, (x - 0.9, 24.4, z), (1.8, spike_len, 0.0), sp, faces=("north",), rot=(0.0, 0.0, -s * 28.0), pivot=piv)
            geo.cube("arm_" + sd, (x, 24.4, z - 0.9), (0.0, spike_len, 1.8), sp, faces=("east",), rot=(0.0, 0.0, -s * 28.0), pivot=piv)


def bone_helmet(b, color="#e6e0d0", dark="#8a8070", side=1, layer="main"):
    """A bone plate over half of the face (Zenon's devil form): side +1 = the model's left."""
    geo = b.g(layer)
    bone = b.t_horn(color, tip=dark, tw=8, th=16, seed=24, ridges=2)
    x0 = 0.15 if side > 0 else -4.65
    geo.cube(HEAD, (x0, 25.6, -4.75), (4.5, 5.4, 0.8), bone)
    geo.cube(HEAD, (x0, 30.4, -4.5), (4.5, 1.6, 8.6), bone)


def back_spikes(b, color="#e6e0d0", n=4, layer="main", y=22.0, length=9.0, spread=3.0, tip="#fff8e8", lean=28.0):
    """Bony appendages growing from the back: tall flat spikes (crossed cards) fanned out."""
    geo = b.g(layer)
    sp = b.t_spike(color, tw=16, th=32, tip=tip, seed=25)
    for i in range(n):
        t = (i + 0.5) / n * 2 - 1
        x = t * spread
        ln = length * (1.0 - 0.18 * abs(t))
        rot = (-18.0 - 6.0 * abs(t), 0.0, -t * lean)
        piv = (x, y - 1.0 - 0.8 * abs(t), 2.6)
        geo.cube("body", (x - 1.6, piv[1], 2.6), (3.2, ln, 0.0), sp, faces=("north",), rot=rot, pivot=piv)
        geo.cube("body", (x, piv[1], 1.0), (0.0, ln, 3.2), sp, faces=("east",), rot=rot, pivot=piv)


def pouch(b, color="#5a3a24", gold="#e8b830", layer="main", y=13.0, x=-2.5):
    """A grimoire pouch hanging off the belt at the back (Vanica)."""
    geo = b.g(layer)
    geo.cube("body", (x - 1.9, y - 3.6, 2.2), (3.8, 3.6, 1.4), b.t_cloth(color, 16, 16, seed=26, trim=gold, trim_h=2))
    geo.cube("body", (x - 2.0, y - 0.2, 2.1), (4.0, 1.0, 1.6), b.t_metal(gold, 16, 8))


def belts_x(b, color="#1a1214", gold="#e8b830", layer="main", y0=22.4, buckle=True):
    """Two belts crossing the chest in an X, a gold buckle where they meet."""
    geo = b.g(layer)
    tile = b.t_cloth(color, 8, 16, seed=27)
    for s in (-1, 1):
        geo.cube("body", (-0.8, y0 - 9.0, -2.58), (1.6, 10.2, 0.0), tile, faces=("north",), rot=(0.0, 0.0, s * 38.0), pivot=(0.0, y0 - 4.0, -2.58))
    if buckle:
        geo.cube("body", (-1.0, y0 - 5.2, -2.7), (2.0, 2.0, 0.5), b.t_metal(gold, 16, 16))


def shoulder_fur(b, color="#f2f0f6", layer="main"):
    P.mantle(b, color, color, y=21.9, thick=2.2, width=16.2, depth=6.6, drop=2.2, layer=layer)
