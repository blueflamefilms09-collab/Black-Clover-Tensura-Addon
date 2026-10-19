"""djinn36_art2d: the 2D art of the 24 new Djinn: the body-art overlay of each look (a pattern of its own on every face of the skin), the dark veins and bust
(via djinn_dark), and the Seal sprites (a glyph of the Djinn's own on the usual round seal). Pure PIL / numpy, deterministic."""
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn_dark as dd                                    # noqa: E402
import djinn_icons as di                                   # noqa: E402
import djinn36_roster as R                                 # noqa: E402


def _grid(w, h):
    return np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))


def pattern36(name, w, h):
    """A 0..1 mask for a face of w x h px: the Djinn's own body-art."""
    X, Y = _grid(w, h)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    ang = np.arctan2(Y - cy, X - cx)
    r = np.hypot(X - cx, Y - cy)
    m = np.zeros((h, w), np.float32)
    if name == "BAEL":                                   # gilded chevrons
        m = (((Y + np.abs(X - cx)) % 5) < 1.0).astype(np.float32)
    elif name == "AGARES":                               # the studs of a crocodile's hide
        m = (((X % 4 == 1) & (Y % 4 == 1)) | ((X % 4 == 3) & (Y % 4 == 3))).astype(np.float32)
    elif name == "VALEFOR":                              # forked lightning with a gap between
        m = ((np.abs(((X + Y) % 7) - 3) < 0.6) & ((Y // 3) % 2 == 0)).astype(np.float32)
    elif name == "BARBATOS":                             # leaf veins
        m = (np.abs(((Y % 6) - 3) - np.abs(X - cx)) < 0.6).astype(np.float32)
    elif name == "GUSION":                               # the cracks of a mirror
        m = (((X - Y) % 8 == 0) | ((X + Y) % 11 == 0)).astype(np.float32)
    elif name == "VEPAR":                                # shells: arcs from the foot of the face
        m = (np.abs(((np.hypot(X - cx, Y - h) % 3.6) - 1.6)) < 0.5).astype(np.float32)
    elif name == "SHAX":                                 # overlapping feathers
        gx, gy = (X % 4) - 1.5, (Y % 3) - 1.0 + 1.5 * ((X // 4) % 2)
        m = ((np.hypot(gx, gy) > 1.3) & (np.hypot(gx, gy) < 1.9)).astype(np.float32)
    elif name == "BUER":                                 # five spokes
        m = ((np.abs(np.sin(ang * 2.5)) > 0.95) & (r > 1.2)).astype(np.float32)
    elif name == "ANDREALPHUS":                          # the eyes of a peacock's feather
        gx, gy = (X % 6) - 2.5, (Y % 6) - 2.5
        m = ((np.abs(np.hypot(gx, gy) - 2.0) < 0.55) | (np.hypot(gx, gy) < 0.6)).astype(np.float32)
    elif name == "KIMARIS":                              # the stripes of a barded steed
        m = ((X % 5 < 1) | ((Y % 9 < 1) & (X % 2 == 0))).astype(np.float32)
    elif name == "AMDUSIAS":                             # a stave of five lines with notes
        m = (((Y % 3 == 0) & (Y < h * 0.9)) | ((X % 7 == 3) & (Y % 3 == 1) & (Y % 6 < 3))).astype(np.float32)
    elif name == "BELIAL":                               # only the frame: an outline, a cross
        m = ((X == 1) | (X == w - 2) | (Y == 1) | (Y == h - 2) | (np.abs(X - cx) < 0.5) | (np.abs(Y - cy) < 0.5)).astype(np.float32)
    elif name == "DECARABIA":                            # small crosses, stars of the deep
        m = (((X % 6 == 2) & (Y % 6 > 0) & (Y % 6 < 5)) | ((Y % 6 == 2) & (X % 6 > 0) & (X % 6 < 5))).astype(np.float32)
    elif name == "SEERE":                                # streaks of wind
        m = ((Y % 3 == 0) & (((X + 2 * (Y // 3)) % 7) < 4)).astype(np.float32)
    elif name == "DANTALION":                            # lines of script
        m = ((Y % 3 == 1) & (((X * 7 + Y * 3) % 5) < 4)).astype(np.float32)
    elif name == "ANDROMALIUS":                          # watching eyes in a grid
        u = (X % 8) - 3.5
        v = (Y % 8) - 3.5
        m = ((np.abs(np.abs(v) - 2.0 * (1 - (u / 4.0) ** 2)) < 0.55) & (np.abs(u) < 4)).astype(np.float32)
    elif name == "GLASYA_LABOLAS":                       # claw marks, three together
        m = ((np.abs(((X - 0.5 * Y) % 9) - 1.5) < 0.6) & ((Y // 8) % 2 == 0)).astype(np.float32)
    elif name == "HAAGENTI":                             # cogs
        gx, gy = (X % 8) - 3.5, (Y % 8) - 3.5
        m = ((np.abs(np.hypot(gx, gy) - 2.5) < 0.6) | (np.hypot(gx, gy) < 0.7)).astype(np.float32)
    elif name == "CROCELL":                              # snow-crystals
        gx, gy = (X % 9) - 4, (Y % 9) - 4
        a2 = np.arctan2(gy, gx)
        m = ((np.abs(np.sin(a2 * 3.0)) > 0.96) & (np.hypot(gx, gy) < 3.6)).astype(np.float32)
    elif name == "FURCAS":                               # a frost cross and its corners
        m = ((np.abs(X - cx) < 0.6) | (np.abs(Y - cy) < 0.6) | ((X % 5 == 0) & (Y % 5 == 0))).astype(np.float32)
    elif name == "BALAM":                                # three wavering flames
        m = np.zeros((h, w), np.float32)
        for q in range(3):
            m = np.maximum(m, (np.abs(X - (w * (q + 0.5) / 3.0) - 1.2 * np.sin(Y * 0.9 + q)) < 0.6).astype(np.float32))
    elif name == "ALLOCES":                              # the rays of a mane
        m = ((np.abs(np.sin(ang * 6.0)) > 0.95) & (r > 1.2)).astype(np.float32)
    elif name == "CAMIO":                                # barbs of a feather
        m = ((np.abs(((X + Y) % 6) - 3) < 0.5) & (X % 2 == 0)).astype(np.float32)
    elif name == "MURMUR":                               # the rings of a drum
        m = ((r % 3.0) < 0.75).astype(np.float32)
    return m


_orig_pattern = dd.pattern


def _pattern(name, w, h, face, box):
    if name in R.NAMES:
        return pattern36(name, w, h)
    return _orig_pattern(name, w, h, face, box)


dd.pattern = _pattern


def make_skins(dj, variant, assets):
    """The rune overlay of the look (and, for the dark one, the veins and the bust)."""
    dd.make_skins(dj, variant, assets)


# ================================================================ seals
GLYPH36 = {
    "BAEL": ["#.#.#.#", "#######", "#######", ".#####.", ".#####.", "#######", "......."],
    "AGARES": ["#######", ".#.#.#.", ".......", ".#.#.#.", "#######", ".......", "......."],
    "VALEFOR": ["..###..", ".#...#.", ".#...#.", "..###..", "...#...", "...##..", "...#.#."],
    "BARBATOS": ["#.....#", "##...##", "#.###.#", "..###..", "...#...", "...#...", "...#..."],
    "GUSION": [".#####.", "#.....#", "#.###.#", "#.###.#", "#.....#", ".#####.", "...#..."],
    "VEPAR": ["...#...", "..###..", "...#...", "#######", "...#...", "#..#..#", ".#####."],
    "SHAX": [".......", "##...##", ".##.##.", "..###..", "...#...", "..###..", "......."],
    "BUER": ["..#.#..", ".#.#.#.", "#..#..#", ".#####.", "#..#..#", ".#.#.#.", "..#.#.."],
    "ANDREALPHUS": ["..###..", ".#####.", ".##.##.", ".#####.", "..###..", "...#...", "...#..."],
    "KIMARIS": [".#####.", "##...##", "#.....#", "#.....#", "##...##", ".#...#.", "##...##"],
    "AMDUSIAS": ["......#", "....###", "...##..", "..##...", ".##....", "##.....", "#......"],
    "BELIAL": [".#####.", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", ".#####."],
    "DECARABIA": ["...#...", "...#...", "#######", ".#####.", "..###..", ".##.##.", "##...##"],
    "SEERE": ["......#", ".....##", "...####", "..####.", ".###...", "##.....", "#......"],
    "DANTALION": [".#####.", "#######", "##.#.##", "#######", "#.###.#", "#.....#", ".#####."],
    "ANDROMALIUS": [".......", ".#####.", "#..#..#", "#.###.#", "#..#..#", ".#####.", "......."],
    "GLASYA_LABOLAS": ["#..#..#", "#..#..#", ".#.#.#.", ".#.#.#.", "..###..", "..###..", "...#..."],
    "HAAGENTI": ["#.###.#", ".#####.", "###.###", "###.###", "###.###", ".#####.", "#.###.#"],
    "CROCELL": ["#..#..#", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "#..#..#"],
    "FURCAS": ["#######", "#######", "#######", "#######", ".#####.", "..###..", "...#..."],
    "BALAM": ["#..#..#", "#..#..#", "##.#.##", "##.#.##", "#######", "#######", ".#####."],
    "ALLOCES": ["#.#.#.#", ".#####.", "#######", ".#####.", "#######", ".#####.", "#.#.#.#"],
    "CAMIO": ["..#....", ".###...", "#####..", "..####.", "...####", "....###", "...#..#"],
    "MURMUR": ["#.....#", ".#####.", "#######", "#######", "#######", ".#####.", "#.....#"],
}
for _n, _g in GLYPH36.items():
    di.GLYPH[_n] = _g


def make_seal(dj, assets):
    """The Seal sprite (16 x 16) and its item model: the round seal of the first twelve with a glyph of the Djinn's own."""
    return di.make_icon(dict(dj, affinity=dj["name"]), assets)
