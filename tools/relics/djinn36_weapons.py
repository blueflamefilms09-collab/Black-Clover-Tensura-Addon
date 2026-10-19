"""djinn36_weapons: writes the in-hand 3D weapons of the new Djinn (mesh, skins, model json, inventory icon) from the weapon pieces of their armour sets, with
the same code as the first twelve (tools/relics/djinn_weapons.py, whose tables now list all 36)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn_weapons as dw                      # noqa: E402


def make_weapons(dj, _unused=None):
    d = dj["id"]
    out = []
    for dark in (False, True):
        files, b = dw.outputs(d, dark)
        for rel, data in files.items():
            path = os.path.join(dw.ASSETS, *rel.split("/"))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as f:
                f.write(data if isinstance(data, bytes) else data.encode("utf-8"))
        out.append("%s apparent %.2f blocks" % (dw.item_id(d, dark), (b["box"][3] - b["ymin"]) * b["scale"] * dw.THIRD_PERSON_SCALE))
    print("; ".join(out))
