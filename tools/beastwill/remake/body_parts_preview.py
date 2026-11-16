#!/usr/bin/env python3
"""Before / after previews of the body-parts pass (docs/body_parts_pass.md): for each will, the form as it was (BWF_OLD_HEADS=1) next to the form now, from the front and from the side, on the same
plain player proxy the other Beast Will previews use. Output: docs/body_parts_previews/<will>_before_after.png

  python3 tools/beastwill/remake/body_parts_preview.py fenrir_will hydra_will
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", ".."))
from forms_gen import build                                   # noqa: E402
from forms_preview import render, player_cards, sheet_of      # noqa: E402

OUT = os.path.abspath(os.path.join(HERE, "..", "..", "..", "docs", "body_parts_previews"))


def one(wid):
    pl = player_cards(head=False)
    ims, labels = [], []
    for yaw, nm in ((22, "front"), (112, "side")):
        for old in (True, False):
            if old:
                os.environ["BWF_OLD_HEADS"] = "1"
            else:
                os.environ.pop("BWF_OLD_HEADS", None)
            sc = build(wid)
            ims.append(render(pl + sc.cards(), sc.sh, yaw=yaw, pitch=8, W=420, H=520, scale=8.0, target=(0, 22, 0)))
            labels.append("%s %s (%s, %d cubes)" % (wid, "BEFORE" if old else "AFTER", nm, len(sc.boxes)))
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, wid.replace("_will", "") + "_before_after.png")
    sheet_of(ims, labels, out, cols=4, cw=420, ch=520)
    return out


if __name__ == "__main__":
    for w in sys.argv[1:]:
        print(one(w))
