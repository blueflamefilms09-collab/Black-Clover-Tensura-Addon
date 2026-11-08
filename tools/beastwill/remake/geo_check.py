#!/usr/bin/env python3
"""Reads the exported geo pieces back (the way GeoModelData does) and renders them, to prove the files draw what the preview shows.  python3 geo_check.py <will_id>"""
import os, sys, json
import numpy as np
from PIL import Image
sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE); sys.path.insert(0, os.path.join(HERE, "..", ".."))
import firecast_geo_lib as G
from forms_lib import Card, V
from forms_export import stem
from forms_preview import render, player_cards, sheet_of

FACES = {"east": (0, 1), "west": (0, -1), "up": (1, 1), "down": (1, -1), "north": (2, -1), "south": (2, 1)}     # Bedrock names in the FILE: file east = model -x -> Model 'west'... read below
# in the file: "east" is the model's -x side, "west" is +x (GeoModelData)
FILE_FACE = {"east": (0, -1), "west": (0, 1), "up": (1, 1), "down": (1, -1), "north": (2, -1), "south": (2, 1)}


class TexSheet:
    def __init__(s, key):
        base = os.path.join(G.ROOT, "textures", "entity", key)
        s.c = np.asarray(Image.open(base + ".png").convert("RGBA"), float); s.g = np.asarray(Image.open(base + "_glow.png").convert("RGBA"), float)
        s.H, s.W = s.c.shape[:2]

    def sample(s, region, u, v, win=None):
        x, y, w, h = region
        px = np.clip((x + u * w).astype(int), 0, s.W - 1); py = np.clip((y + v * h).astype(int), 0, s.H - 1)
        return s.c[py, px], s.g[py, px]


def cards_of(st):
    key = "bwf_" + st; cards = []
    for f in sorted(os.listdir(os.path.join(G.ROOT, "geo", "entity"))):
        if not f.startswith(key + "_arm_"): continue
        g = json.load(open(os.path.join(G.ROOT, "geo", "entity", f)))["minecraft:geometry"][0]
        for b in g["bones"]:
            for c in b["cubes"]:
                o, s_ = V(*c["origin"]), V(*c["size"])
                ctr = o + s_ / 2
                R = G.rot_matrix(*c["rotation"]) if "rotation" in c else np.eye(3)
                piv = V(*c["pivot"]) if "pivot" in c else ctr
                ctr = R @ (ctr - piv) + piv
                for name, uvd in c["uv"].items():
                    ai, sg = FILE_FACE[name]
                    ax = R[:, ai] * sg
                    fw, fh = ((s_[2], s_[1]), (s_[0], s_[2]), (s_[0], s_[1]))[ai]
                    ra, ua = {0: (R[:, 2], R[:, 1]), 1: (R[:, 0], R[:, 2]), 2: (R[:, 0], R[:, 1])}[ai]
                    x, y = uvd["uv"]; w, h = uvd["uv_size"]
                    cards.append(Card(ctr + ax * (s_[ai] / 2) * 1.0 + 0 * ax, ra, ua, fw, fh, (x, y, w, h), None, "x", b["name"]))
    return cards


if __name__ == "__main__":
    st = stem(sys.argv[1]); sh = TexSheet("bwf_" + st); cards = cards_of(st); pl = player_cards(head=False)
    ims = [render(pl + cards, sh, yaw=y, pitch=8, W=360, H=470, scale=10.5) for y in (0, 35, 90, 150)]
    out = os.path.join(HERE, "prev_geo_%s.png" % st); sheet_of(ims, ["front", "3/4", "side", "back"], out, cw=360, ch=470); print(out, len(cards), "faces")
