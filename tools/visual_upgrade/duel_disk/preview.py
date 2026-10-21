"""preview: before / after contact sheets of the deck relics worn, from the software rasteriser of the worn tools (tools/visual_upgrade/charms_genesis_worn/vupreview.py).

    python3 -B tools/visual_upgrade/duel_disk/preview.py            writes docs/previews/duel_disk/<id>.png

The game draws a relic piece at 0.85 (WornPlacer.BODY_SCALE) about its anchor; the previewer does not, so the files are scaled by 0.85 into a scratch folder first.
A software preview: orthographic, no light map, no bloom. It is not a screenshot of the game and nothing here has been seen in a game.
"""
import os
import shutil
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import disk as D  # noqa: E402
import metrics as MT  # noqa: E402
import vupreview as VP  # noqa: E402

OUT = os.path.join(MT.ROOT, "docs", "previews", "duel_disk")
K = D.BODY_SCALE
VIEWS = (("front", (0, 0)), ("side", (90, 0)), ("back", (180, 0)), ("three-quarter", (35, 14)))
ZOOM = (-8, 24, -4, 20)
PAL = {"deck_of_many_things": (0x1A1A22, 0xE6B94A, 0xFFD27A), "pact_of_the_deck": (0xF2F0E6, 0xC9A94B, 0xFFF3C0)}


def scaled(src, dst, anchor=None):
    out = []
    for ln in open(src, encoding="utf8").read().splitlines():
        v = ln.split()
        if v and v[0] == "grp":
            v[4], v[5], v[6] = ["%.3f" % (float(x) * K) for x in v[4:7]]
            if v[3] == "bob":
                v[7] = "%.4f" % (float(v[7]) * K)
            ln = " ".join(v)
        elif v and v[0] == "q":
            nums = v[4:]
            for c in range(4):
                for j in range(3):
                    nums[c * 8 + j] = "%.3f" % (float(nums[c * 8 + j]) * K)
            ln = " ".join(v[:4] + nums)
        elif v and v[0] == "anchor" and anchor:
            ln = "anchor " + anchor
        out.append(ln)
    open(dst, "w", newline="\n").write("\n".join(out) + "\n")


def rows(pieces, pal, tmp, label):
    res = []
    for t in (0.0, 45.0):
        row = []
        for name, view in VIEWS:
            row.append(("%s %s t=%d" % (label, name, t), VP.tile(pieces, pal, view, ZOOM, (300, 340), t=t, figure=True)))
        res.append(row)
    return res


def main():
    tmp = tempfile.mkdtemp(prefix="dd_prev_")
    try:
        M = MT.VU_MODELS
        for f in os.listdir(M):
            if f.startswith("duel_disk_") or f.startswith("deck_of_many_things"):
                scaled(os.path.join(M, f), os.path.join(tmp, f))
        before = {"deck_of_many_things": [(os.path.join(tmp, "deck_of_many_things_worn.txt"), None, False)],
                  "pact_of_the_deck": None}
        after = {"deck_of_many_things": [(os.path.join(tmp, "duel_disk_deck_of_many_things_worn.txt"), None, False), (os.path.join(tmp, "deck_of_many_things_cards_worn.txt"), None, False)],
                 "pact_of_the_deck": [(os.path.join(tmp, "duel_disk_pact_of_the_deck_worn.txt"), None, False)]}
        os.makedirs(OUT, exist_ok=True)
        for rid in after:
            r = []
            if before[rid]:
                r += rows(before[rid], PAL[rid], tmp, "before")
            r += rows(after[rid], PAL[rid], tmp, "after")
            VP.sheet(r, os.path.join(OUT, rid + "_before_after.png" if before[rid] else rid + "_after.png"), panel=(300, 340), title=rid)
        # the held forms
        held = {"deck_of_many_things": ([(os.path.join(M, "deck_of_many_things.txt"), "body", False)], [(os.path.join(M, "duel_disk_deck_of_many_things_hand.txt"), "body", False),
                                                                                                   (os.path.join(M, "deck_of_many_things_cards_hand.txt"), "body", False)]),
                "pact_of_the_deck": (None, [(os.path.join(M, "duel_disk_pact_of_the_deck_hand.txt"), "body", False)])}
        for rid, (b, a) in held.items():
            r = []
            for label, pcs in (("before", b), ("after", a)):
                if not pcs:
                    continue
                row = []
                for name, view in (("front", (0, 0)), ("side", (90, 0)), ("three-quarter", (35, 14)), ("top", (0, 70))):
                    row.append(("%s held %s" % (label, name), VP.tile(pcs, PAL[rid], view, (-16, 16, -16, 16), (300, 300), t=20.0, figure=False)))
                r.append(row)
            VP.sheet(r, os.path.join(OUT, rid + "_held.png"), title=rid + " held")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("previews in", OUT)


if __name__ == "__main__":
    main()
