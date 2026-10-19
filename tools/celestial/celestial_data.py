"""The shared table of Celestial Spirit Magic for the asset scripts: the keys (same order and colours as celestial/CelestialKey.java; the asset test checks them
against each other), the stylised constellation of each sign, and a few drawing helpers.

Everything is deterministic. Run python as `python3 -B`.
"""
import math
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

# id, display, rank, rgb  (the order is the ordinal order of CelestialKey)
KEYS = [
    ("aquarius", "Aquarius", "gold", 0x3FA9FF), ("taurus", "Taurus", "gold", 0xD9A441), ("leo", "Leo", "gold", 0xFFE27A),
    ("virgo", "Virgo", "gold", 0xF2A0C8), ("sagittarius", "Sagittarius", "gold", 0xC98A4B), ("gemini", "Gemini", "gold", 0x7FB5FF),
    ("cancer", "Cancer", "gold", 0xFF8A6B), ("scorpio", "Scorpio", "gold", 0xD4A257), ("capricorn", "Capricorn", "gold", 0x9A7FD6),
    ("pisces", "Pisces", "gold", 0x66E0D4), ("aries", "Aries", "gold", 0xFFC8E4), ("libra", "Libra", "gold", 0xB0B0FF),
    ("horologium", "Horologium", "silver", 0xC8B28A), ("crux", "Crux", "silver", 0xAFE8FF), ("lyra", "Lyra", "silver", 0xFFD6F5),
    ("nikora", "Nikora", "silver", 0xFFFFFF), ("spirit_king_gate", "Gate Key", "gate", 0xFFF3B0),
]
SPIRITS = [k for k in KEYS if k[2] != "gate"]
GOLD = [k for k in KEYS if k[2] == "gold"]

# the stylised constellation of each sign: stars in a 0..1 square (y down) and the lines between them (indexes)
CONSTELLATIONS = {
    "aquarius": ([(0.12, 0.38), (0.30, 0.24), (0.46, 0.42), (0.62, 0.26), (0.80, 0.44), (0.88, 0.66), (0.52, 0.72)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (2, 6)]),
    "taurus": ([(0.14, 0.14), (0.34, 0.40), (0.50, 0.55), (0.66, 0.40), (0.86, 0.14), (0.50, 0.80)], [(0, 1), (1, 2), (2, 3), (3, 4), (2, 5)]),
    "leo": ([(0.26, 0.82), (0.26, 0.56), (0.38, 0.36), (0.54, 0.26), (0.66, 0.40), (0.56, 0.52), (0.82, 0.72), (0.52, 0.66)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 7), (7, 6)]),
    "virgo": ([(0.18, 0.20), (0.34, 0.36), (0.50, 0.30), (0.66, 0.46), (0.52, 0.62), (0.40, 0.82), (0.82, 0.74)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (3, 6)]),
    "sagittarius": ([(0.14, 0.82), (0.30, 0.62), (0.50, 0.50), (0.70, 0.30), (0.86, 0.14), (0.86, 0.38), (0.62, 0.14), (0.40, 0.76)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (4, 6), (2, 7)]),
    "gemini": ([(0.26, 0.14), (0.26, 0.40), (0.24, 0.70), (0.30, 0.90), (0.72, 0.14), (0.70, 0.42), (0.74, 0.72), (0.78, 0.90)], [(0, 1), (1, 2), (2, 3), (4, 5), (5, 6), (6, 7), (1, 5), (0, 4)]),
    "cancer": ([(0.50, 0.44), (0.30, 0.28), (0.14, 0.14), (0.70, 0.28), (0.86, 0.14), (0.34, 0.70), (0.66, 0.74)], [(0, 1), (1, 2), (0, 3), (3, 4), (0, 5), (0, 6)]),
    "scorpio": ([(0.12, 0.36), (0.28, 0.40), (0.44, 0.46), (0.58, 0.56), (0.70, 0.70), (0.80, 0.84), (0.90, 0.70), (0.86, 0.52)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7)]),
    "capricorn": ([(0.14, 0.22), (0.36, 0.30), (0.56, 0.24), (0.78, 0.36), (0.84, 0.60), (0.64, 0.80), (0.40, 0.70), (0.22, 0.56)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 1)]),
    "pisces": ([(0.14, 0.20), (0.26, 0.42), (0.40, 0.58), (0.60, 0.44), (0.76, 0.58), (0.86, 0.80), (0.40, 0.84)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (2, 6)]),
    "aries": ([(0.20, 0.64), (0.40, 0.46), (0.60, 0.40), (0.80, 0.26), (0.74, 0.60)], [(0, 1), (1, 2), (2, 3), (2, 4)]),
    "libra": ([(0.18, 0.60), (0.40, 0.30), (0.62, 0.34), (0.84, 0.60), (0.50, 0.82)], [(0, 1), (1, 2), (2, 3), (0, 4), (3, 4)]),
    "horologium": ([(0.50, 0.14), (0.50, 0.50), (0.74, 0.50), (0.50, 0.86), (0.26, 0.50), (0.50, 0.50)], [(0, 1), (1, 2), (1, 4), (1, 3)]),
    "crux": ([(0.50, 0.12), (0.50, 0.88), (0.14, 0.50), (0.86, 0.46), (0.62, 0.62)], [(0, 1), (2, 3)]),
    "lyra": ([(0.50, 0.12), (0.34, 0.40), (0.66, 0.40), (0.38, 0.72), (0.62, 0.72)], [(0, 1), (0, 2), (1, 2), (1, 3), (2, 4), (3, 4)]),
    "nikora": ([(0.30, 0.46), (0.50, 0.36), (0.70, 0.46), (0.40, 0.70), (0.60, 0.70)], [(0, 1), (1, 2), (0, 3), (2, 4)]),
    "spirit_king_gate": ([(0.50, 0.10), (0.20, 0.50), (0.50, 0.90), (0.80, 0.50), (0.50, 0.50)], [(0, 1), (1, 2), (2, 3), (3, 0), (0, 4), (2, 4), (1, 3)]),
    "king": ([(0.12, 0.70), (0.26, 0.30), (0.38, 0.56), (0.50, 0.16), (0.62, 0.56), (0.74, 0.30), (0.88, 0.70)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (0, 6)]),
}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def out_path(*parts):
    p = os.path.join(ASSETS, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p
