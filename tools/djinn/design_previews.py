#!/usr/bin/env python3
"""Before / after previews of the reworked Djinn designs into docs/djinn_design_previews/ (no game needed).

    python tools/djinn/design_previews.py

busts:  bust_light_before / after (the 7 light busts that were copies), bust_dark_before / after (all 36)
tiny:   tiny_before_after.png (Vinea | Vepar old | Vepar v2, Zagan | Barbatos old | Barbatos v2: front, side, cast) and tiny_dark_v2.png
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OUT = ROOT / "docs/djinn_design_previews"
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(ROOT / "tools/tiny_djinn"))
sys.path.insert(0, str(ROOT / "tools"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    import bust2
    sys.argv = ["bust2.py", "--sheet", str(OUT)]
    bust2.main()
    import preview as pv
    pv.OUT = str(OUT)
    views = [("idle", 1.5, 0.0), ("idle", 1.5, 90.0), ("cast", 0.4, 28.0)]
    pv.sheet(["vinea", "vepar", "vepar_v2", "zagan", "barbatos", "barbatos_v2"], views, 2, "tiny_before_after.png")
    pv.sheet(["vepar_v2", "barbatos_v2"], [("idle", 1.5, 20.0)], 2, "tiny_dark_v2.png", dark=True)


if __name__ == "__main__":
    main()
