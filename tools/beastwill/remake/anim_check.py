#!/usr/bin/env python3
"""Poses a will at clip time u (0..1) with the bone animations of forms_gen, to check that nothing tears apart. Usage: python3 anim_check.py <will_id> [u ...]"""
import sys, math, os
import numpy as np
sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_gen import build, HERE
from forms_cube import Box
from forms_preview import render, player_cards, sheet_of
from firecast_geo_lib import rot_matrix


def pose(sc, u):
    T = {}
    def tf(bn):
        if bn in T: return T[bn]
        d = sc.bonedefs.get(bn)
        if not d: T[bn] = (np.eye(3), np.zeros(3)); return T[bn]
        r = np.zeros(3); sc_ = np.ones(3); pos = np.zeros(3)
        for kind, ax, amp, cyc, ph in d["anim"]:
            v = amp * math.sin(math.tau * (cyc * u + ph))
            if kind == "rot": r[ax] += v
            elif kind == "scale": sc_[ax] += v
            else: pos[ax] += v
        R = rot_matrix(*r) @ np.diag(sc_); p = np.array(d["pivot"])
        t = p - R @ p + pos
        if d["parent"]:
            Rp, tp = tf(d["parent"]); R, t = Rp @ R, Rp @ t + tp
        T[bn] = (R, t); return T[bn]
    out = []
    for b in sc.boxes:
        R, t = tf(b.bone)
        nb = Box(b.part, b.bone, R @ b.c + t, R @ b.R, b.s, b.mat, b.dt, b.faces)
        out.append(nb)
    sc2 = type(sc)(sc.sh); sc2.boxes = out
    return sc2


if __name__ == "__main__":
    wid = sys.argv[1]; us = [float(x) for x in sys.argv[2:]] or [0.0, .25]
    sc = build(wid); pl = player_cards(head=False); ims, labels = [], []
    for u in us:
        cards = pose(sc, u).cards()
        ims.append(render(pl + cards, sc.sh, yaw=158, pitch=8, W=330, H=450, scale=10.0)); labels.append("%s u=%.2f back" % (wid, u))
        ims.append(render(pl + cards, sc.sh, yaw=22, pitch=8, W=330, H=450, scale=10.0)); labels.append("front")
    sheet_of(ims, labels, os.path.join(HERE, "prev_anim.png"), cols=4, cw=330, ch=450)
