import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_gen import *
from forms_preview import render, player_cards, sheet_of
ids = sys.argv[1:]
pl = player_cards(head=False); ims = []; lb = []
for wid in ids:
    sc = build(wid); cards = sc.cards()
    for yaw in (0, 40, 90):
        ims.append(render(pl + cards, sc.sh, yaw=yaw, pitch=6, W=400, H=400, scale=27, target=(0, 27, -3))); lb.append(wid + " %d" % yaw)
out = os.path.join(HERE, "prev_close.png"); sheet_of(ims, lb, out, cols=3, cw=400, ch=400); print(out)
