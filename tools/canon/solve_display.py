#!/usr/bin/env python3
"""Solves the display transforms of the Canon Grimoire from what we WANT to see (docs/canon/canon_grimoire.md section 3).

The game puts a model vertex v (0..16 units) at   Pose * T(t/16) * Rx(rx) Ry(ry) Rz(rz) * S(s) * T(-0.5) * (v/16)   (ItemTransform.apply, then the model's own centring), so
  * the book's frame in the world is  L = Lpose * Rx Ry Rz  (Lpose = the linear part of what the game puts around the item), applied to the model axes
    front = +Z, up = +Y, east = +X (the fore-edge; the spine is -X);
  * the point q of the book that should sit at a wanted place P (the palm, or the spot of the screen) fixes the translation:  t/16 = P - Lpose * R * S * (q/16 - 0.5).
So: pick the world direction the cover should face and the world direction its top should point, search the Euler angles (5 degree grid, then refined) for the best fit, and solve t.

usage: python3 tools/canon/solve_display.py            # prints the chosen numbers for every context and the directions they give
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "lines"))
import preview_book as pb  # noqa: E402

CENTRE = np.array([8.2, 8.0, 8.0])        # the middle of the book's box in model units (canon_book.py)
SPINE_GRIP = np.array([2.9, 5.2, 8.0])    # the lower part of the spine: where a hand holds the book
PENALTY = 3e-5                            # radians of error per degree of rotation: among equally good solutions prefer the small, readable angles
FRONT, UP = np.array([0.0, 0.0, 1.0]), np.array([0.0, 1.0, 0.0])


def unit(v):
    v = np.array(v, float)
    return v / np.linalg.norm(v)


def orth(f, u):
    u = unit(u)
    f = unit(np.array(f, float) - np.dot(f, u) * u)
    return f, u


def euler(rx, ry, rz):
    return (pb.Rx(rx) @ pb.Ry(ry) @ pb.Rz(rz))[:3, :3]


def best_euler(L0, f_t, u_t, step=5):
    """The (rx, ry, rz) in degrees whose frame L0 @ R sends FRONT to f_t and UP to u_t as closely as possible."""
    f_t, u_t = orth(f_t, u_t)
    best = (9, None)
    rng = range(-180, 181, step)
    for rx in rng:
        Rx = pb.Rx(rx)[:3, :3]
        for ry in rng:
            Ry = pb.Ry(ry)[:3, :3]
            for rz in rng:
                R = L0 @ Rx @ Ry @ pb.Rz(rz)[:3, :3]
                e = math.acos(max(-1, min(1, R @ FRONT @ f_t))) + math.acos(max(-1, min(1, R @ UP @ u_t))) + PENALTY * (abs(rx) + abs(ry) + abs(rz))
                if e < best[0]:
                    best = (e, (rx, ry, rz))
    # refine
    (rx, ry, rz) = best[1]
    for st in (2, 1):
        improved = True
        while improved:
            improved = False
            for dx in (-st, 0, st):
                for dy in (-st, 0, st):
                    for dz in (-st, 0, st):
                        c = (rx + dx, ry + dy, rz + dz)
                        R = L0 @ euler(*c)
                        e = math.acos(max(-1, min(1, R @ FRONT @ f_t))) + math.acos(max(-1, min(1, R @ UP @ u_t))) + PENALTY * (abs(c[0]) + abs(c[1]) + abs(c[2]))
                        if e < best[0] - 1e-9:
                            best, rx, ry, rz, improved = (e, c), c[0], c[1], c[2], True
    return best[1], best[0]


def translation_for(L0, rot, scale, q, P):
    """t (in 1/16 block, display frame) that puts model point q (units) at the wanted place P (blocks, in the frame the translation lives in)."""
    R = euler(*rot)
    return 16.0 * (np.array(P, float) - (R @ (scale * (q / 16.0 - 0.5))))


def report(name, L0, rot, t, scale, extra=""):
    R = L0 @ euler(*rot)
    f, u, e = R @ FRONT, R @ UP, R @ np.array([1.0, 0, 0])
    print("%-24s rot %-18s t %-22s s %.2f   front %s  top %s  spine %s %s" % (name, list(rot), [round(float(v), 2) for v in t], scale, np.round(f, 2), np.round(u, 2), np.round(-e, 2), extra))


def main():
    # ---- first person: Pose = translate(0.56, -0.52, -0.72) in the camera frame: no rotation, x right, y up, z toward the eye
    L0 = np.eye(3)
    f_t, u_t = (-0.18, 0.40, 0.90), (-0.16, 0.86, -0.49)
    rot, err = best_euler(L0, f_t, u_t)
    scale = 0.45
    # the book's centre sits at (0.42, -0.30, -0.78) of the camera frame; the translation lives in the same frame, relative to the anchor (0.56, -0.52, -0.72)
    t = translation_for(L0, rot, scale, CENTRE, (0.42 - 0.56, -0.30 + 0.52, -0.78 + 0.72))
    report("firstperson_righthand", L0, rot, t, scale, "err %.3f rad" % err)


if __name__ == "__main__":
    main()
