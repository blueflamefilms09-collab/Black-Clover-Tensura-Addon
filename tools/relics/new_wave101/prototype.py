"""Bakes one or more items of wave 101 and renders the preview sheet, for checking while building.

    python3 -B tools/relics/new_wave101/prototype.py <id> [<id> ...] [--detail] [--nobake]

The sheet (two clock times x front, three-quarter, side) goes to $W101_PREVIEW (default: the scratch folder of the session); with --detail the skin and glow passes only, large, from the
front and the three-quarter view, to judge the models without the additive layers.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import common as C  # noqa: E402
import metrics as MT  # noqa: E402
import vupreview as VP  # noqa: E402
import sheets as SH  # noqa: E402

OUT = os.environ.get("W101_PREVIEW", os.path.join(MT.ROOT, "build", "w101_preview"))


def all_items():
    items = []
    for mod in ("items_a", "items_b", "items_c", "items_d", "items_e", "items_f"):
        try:
            m = __import__(mod)
        except ImportError:
            continue
        items += m.ITEMS
    return items


def _pieces(item):
    path = os.path.join(MT.VU_MODELS, item.rid + ".txt")
    anchor = item.anchor if item.worn == "pieces" else "body"
    return [(path, anchor, False)]


def preview(item, out=OUT):
    pal = C.R.BY_ID[item.rid][4]
    pieces = _pieces(item)
    lo, hi = SH.extent(pieces)
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    half = max(hi[0] - lo[0], hi[1] - lo[1]) / 2 * 1.1 + 1.0
    zoom = (cx - half, cx + half, cy - half, cy + half)
    rows = []
    for t in (0.0, 40.0):
        row = []
        for label, view in (("front", (0, 0)), ("three", (35, 14)), ("side", (90, 0))):
            row.append(("%s t=%d" % (label, t), VP.tile(pieces, pal, view, zoom, (320, 320), t, figure=False)))
        rows.append(row)
    os.makedirs(out, exist_ok=True)
    return VP.sheet(rows, os.path.join(out, item.rid + ".png"), panel=(320, 320), title=item.rid)


def detail(item, out=OUT):
    pal = C.R.BY_ID[item.rid][4]
    pieces = _pieces(item)
    lo, hi = SH.extent(pieces)
    # the solid part only: a box about the middle half of the model
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    half = max(hi[0] - lo[0], hi[1] - lo[1]) / 2 * 0.62 + 1.0
    zoom = (cx - half, cx + half, cy - half, cy + half)
    row = []
    for label, view in (("front", (0, 0)), ("three", (35, 14)), ("other", (-40, -10))):
        row.append((label, VP.tile(pieces, pal, view, zoom, (440, 440), 0.0, figure=False, passes=("skin", "glow"))))
    os.makedirs(out, exist_ok=True)
    return VP.sheet([row], os.path.join(out, item.rid + "_detail.png"), panel=(440, 440), title=item.rid + " (skin and glow)")


def main(argv):
    ids = [a for a in argv if not a.startswith("--")]
    for it in all_items():
        if ids and it.rid not in ids:
            continue
        if "--nobake" not in argv:
            C.SP.bake(it)
        print("  preview", preview(it))
        if "--detail" in argv:
            print("  detail", detail(it))


if __name__ == "__main__":
    main(sys.argv[1:])
