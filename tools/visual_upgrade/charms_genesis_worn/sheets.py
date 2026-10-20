"""sheets: contact sheets of the items of the slice, for checking while building and for the before / after previews of the release.

    python3 -B tools/visual_upgrade/charms_genesis_worn/sheets.py quick <id> [<id> ...] [--out file.png]      one row per item: front, three-quarter, side (after only)
    python3 -B tools/visual_upgrade/charms_genesis_worn/sheets.py release                                     docs/previews/visual_upgrade/charms_genesis_worn/<id>.png for every item:
                                                                       before and after next to each other, held view, side view, close-up, glow pass, worn view
"""
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import metrics as MT  # noqa: E402
import roster as R  # noqa: E402
import vupreview as VP  # noqa: E402

OUT = os.path.join(MT.ROOT, "docs", "previews", "visual_upgrade", "charms_genesis_worn")


def extent(pieces):
    """Bounding box (x0, x1, y0, y1) of a model list in the preview frame (layer space, y down), with the anchor offsets."""
    lo = np.array([1e9] * 3)
    hi = np.array([-1e9] * 3)
    for path, anchor, mirror in pieces:
        a, head, groups, quads = VP.parse_model(path)
        off = np.array(VP.PW.ANCHORS[anchor or a], float)
        for q in quads:
            P = q[3].copy()
            if mirror:
                P[:, 0] *= -1
            P = P + off
            lo = np.minimum(lo, P.min(axis=0))
            hi = np.maximum(hi, P.max(axis=0))
    return lo, hi


def crop_for(pieces, pad=1.12, square=True):
    lo, hi = extent(pieces)
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    w, h = (hi[0] - lo[0]) * pad, (hi[1] - lo[1]) * pad
    if square:
        w = h = max(w, h)
    return (cx - w / 2, cx + w / 2, cy - h / 2, cy + h / 2)


def pieces_of(rid, upgrade, view):
    cats = MT.Catalogs(upgrade=upgrade)
    slot = R.BY_ID[rid][3]
    names = cats.hand_pieces(rid, slot) if view == "hand" else cats.worn_pieces(rid, slot)
    out = []
    for n in names:
        mirror = n.endswith("~")
        n2 = n.rstrip("~")
        name, _, anchor = n2.partition("@")
        path = MT.model_path(name)
        if path:
            out.append((path, anchor or None, mirror))
    return out


def quick(ids, out):
    rows = []
    for rid in ids:
        pal = R.BY_ID[rid][4]
        pcs = pieces_of(rid, True, "hand")
        crop = crop_for(pcs)
        row = []
        for label, v in (("front", (0, 0)), ("three", (38, 14)), ("side", (90, 0))):
            row.append((rid[:26] + " " + label, VP.tile(pcs, pal, v, crop, (270, 270), t=24.0, figure=False)))
        rows.append(row)
    return VP.sheet(rows, out, panel=(270, 270))


def release_sheet(rid):
    info = R.BY_ID[rid]
    pal = info[4]
    rows = []
    # row 1: held object, before | after, front and three-quarter
    b_h, a_h = pieces_of(rid, False, "hand"), pieces_of(rid, True, "hand")
    cb, ca = crop_for(b_h), crop_for(a_h)
    rows.append([("BEFORE held front", VP.tile(b_h, pal, (0, 0), cb, (300, 300), t=24.0, figure=False)), ("BEFORE held three-quarter", VP.tile(b_h, pal, (38, 14), cb, (300, 300), t=24.0, figure=False)),
                 ("AFTER held front", VP.tile(a_h, pal, (0, 0), ca, (300, 300), t=24.0, figure=False)), ("AFTER held three-quarter", VP.tile(a_h, pal, (38, 14), ca, (300, 300), t=24.0, figure=False))])
    # row 2: after side view, close-up of the detail, the glow passes only, a later frame of the idle animation
    sel = lambda lo, hi: (lo, hi)
    lo, hi = extent(a_h)
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 0.42
    close = (cx - span / 2, cx + span / 2, cy - span / 2, cy + span / 2)
    rows.append([("AFTER side", VP.tile(a_h, pal, (90, 0), ca, (300, 300), t=24.0, figure=False)), ("AFTER close-up of the detail", VP.tile(a_h, pal, (22, 10), close, (300, 300), t=24.0, figure=False)),
                 ("AFTER glow pass only", VP.tile(a_h, pal, (0, 0), ca, (300, 300), t=24.0, figure=False, passes=("glow", "star", "halo"))),
                 ("AFTER idle, 40 ticks later", VP.tile(a_h, pal, (38, 14), ca, (300, 300), t=64.0, figure=False))])
    # row 3: worn, before | after (on the figure)
    b_w, a_w = pieces_of(rid, False, "worn"), pieces_of(rid, True, "worn")
    slot = info[3]
    zoom = {"HEAD": (-11, 11, -17, 6), "BACK": (-13, 13, -4, 24), "HANDS": (-14, 6, -2, 20), "CHARM": (-13, 13, -4, 24)}[slot]
    views = ((0, 0), (40, 12)) if slot != "BACK" else ((180, 0), (140, 12))
    rows.append([("BEFORE worn", VP.tile(b_w, pal, views[0], zoom, (300, 300), t=24.0, figure=True)), ("BEFORE worn 3/4", VP.tile(b_w, pal, views[1], zoom, (300, 300), t=24.0, figure=True)),
                 ("AFTER worn", VP.tile(a_w, pal, views[0], zoom, (300, 300), t=24.0, figure=True)), ("AFTER worn 3/4", VP.tile(a_w, pal, views[1], zoom, (300, 300), t=24.0, figure=True))])
    return VP.sheet(rows, os.path.join(OUT, rid + ".png"), panel=(300, 300), title="%s (%s %s): before | after" % (info[1], info[2], info[3]))


def main(argv):
    if argv and argv[0] == "quick":
        ids = [a for a in argv[1:] if not a.startswith("--")]
        out = argv[argv.index("--out") + 1] if "--out" in argv else os.path.join(MT.ROOT, "build", "vu_preview", "quick.png")
        ids = [i for i in ids if i != out]
        print(quick(ids, out))
        return 0
    if argv and argv[0] == "release":
        only = [a for a in argv[1:] if not a.startswith("--")]
        for it in R.ITEMS:
            if only and it[0] not in only:
                continue
            if not os.path.exists(os.path.join(MT.VU_MODELS, it[0] + ".txt")):
                continue
            print(release_sheet(it[0]))
        return 0
    print(__doc__)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
