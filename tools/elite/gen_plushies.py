"""elite/items: builds the mob plushies' 3D models, palette textures, blockstates, item models and loot tables.

    python3 -B tools/elite/gen_plushies.py [ids ...] [--sheet] [--check]

The list of plushies is the Java list (src/main/java/.../elite/items/plush/PlushKinds.java: k("Allay", 1), ...), read from the source so the two cannot drift; the look of each is a function in
tools/elite/plush_mobs.py. For every plushie this writes
  assets/nusmp/models/block/plush/<id>.json       the cube model (big head, small body, stubby limbs, the mob's features; <= 64x64 texture)
  assets/nusmp/textures/block/plush/<id>.png      the 64x64 palette-style texture (a few KB)
  assets/nusmp/blockstates/plush_<id>.json        the four facings (the model's front is north)
  assets/nusmp/models/item/plush_<id>.json        the item model (parent: the block model, with its own display transforms)
  data/nusmp/loot_table/blocks/plush_<id>.json    the block drops itself
--sheet writes build/elite_preview/plush_sheet.png (every plushie, front three-quarter and back three-quarter); --check exits 1 when a file would change. Deterministic.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from art_lib import ASSETS, BUILD, DATA, ROOT, ensure, write_text  # noqa: E402
import plush_lib  # noqa: E402
from plush_mobs import MOBS  # noqa: E402

JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "elite", "items", "plush", "PlushKinds.java")


def kinds():
    """[(id, name, size)] in the order of PlushKinds.ALL."""
    src = open(JAVA, encoding="utf-8").read()
    out = []
    for name, size in re.findall(r'k\("([^"]+)",\s*(\d)\)', src):
        out.append((name.lower().replace(" ", "_"), name, int(size)))
    return out


def jdump(obj):
    return json.dumps(obj, indent=2) + "\n"


def files_for(pid, plush):
    """{relative path: bytes or str} for one plushie."""
    out = {}
    model = plush.model_json()
    out["assets/nusmp/models/block/plush/%s.json" % pid] = jdump(model)
    out["assets/nusmp/models/item/plush_%s.json" % pid] = jdump({"parent": "nusmp:block/plush/" + pid})
    variants = {"facing=north": {"model": "nusmp:block/plush/" + pid}, "facing=east": {"model": "nusmp:block/plush/" + pid, "y": 90},
                "facing=south": {"model": "nusmp:block/plush/" + pid, "y": 180}, "facing=west": {"model": "nusmp:block/plush/" + pid, "y": 270}}
    out["assets/nusmp/blockstates/plush_%s.json" % pid] = jdump({"variants": variants})
    out["data/nusmp/loot_table/blocks/plush_%s.json" % pid] = jdump({
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": "nusmp:plush_" + pid}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": "nusmp:blocks/plush_" + pid})
    return out


def write_all(only, check):
    ks = kinds()
    ids = [k[0] for k in ks]
    missing = [i for i in ids if i not in MOBS]
    extra = [i for i in MOBS if i not in ids]
    if missing or extra:
        print("MISMATCH between PlushKinds.java and plush_mobs.py: missing", missing, "extra", extra)
        sys.exit(2)
    dirty = 0
    plushes = {}
    for pid, name, size in ks:
        if only and pid not in only:
            continue
        P = MOBS[pid]()
        plushes[pid] = P
        lo, hi = P.bounds()
        if min(lo[0], lo[2]) < -0.5 or max(hi[0], hi[2]) > 16.5 or lo[1] < -0.5 or hi[1] > 16.5:
            print("note: %s leaves the block: x %.1f..%.1f y %.1f..%.1f z %.1f..%.1f" % (pid, lo[0], hi[0], lo[1], hi[1], lo[2], hi[2]))
        for rel, data in files_for(pid, P).items():
            path = os.path.join(ROOT, "src", "main", "resources", *rel.split("/"))
            if check:
                if not os.path.exists(path) or open(path, encoding="utf-8").read() != data:
                    dirty += 1
                continue
            write_text(path, data)
        tpath = os.path.join(ASSETS, "textures", "block", "plush", pid + ".png")
        if check:
            continue
        ensure(os.path.dirname(tpath))
        P.tex.im.save(tpath, optimize=True)
        print("%-15s %2d cubes, texture %4d px used, %d bytes" % (pid, len(P.cubes), P.tex.used(), os.path.getsize(tpath)))
    if check:
        print("dirty files:", dirty)
        sys.exit(1 if dirty else 0)
    return plushes


def sheet(plushes):
    from PIL import Image
    ids = list(plushes)
    cols = 9
    cell = 160
    rows = (len(ids) + cols - 1) // cols
    out = Image.new("RGBA", (cols * cell, rows * cell * 2), (70, 76, 96, 255))
    for n, pid in enumerate(ids):
        for v, yaw in enumerate((-32.0, 148.0)):
            im = plush_lib.render(plushes[pid], yaw=yaw, size=cell)
            out.alpha_composite(im, ((n % cols) * cell, ((n // cols) * 2 + v) * cell))
    ensure(BUILD)
    path = os.path.join(BUILD, "plush_sheet.png")
    out.save(path)
    print("sheet:", path)


def main(argv):
    only = [a for a in argv if not a.startswith("--")]
    plushes = write_all(only, "--check" in argv)
    if "--sheet" in argv:
        sheet(plushes)


if __name__ == "__main__":
    main(sys.argv[1:])
