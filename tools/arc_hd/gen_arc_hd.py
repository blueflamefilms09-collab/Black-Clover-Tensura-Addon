#!/usr/bin/env python3
"""HD skins for the Seven Deadly Sins and Black Clover characters of the player-model kit (3.40.0, docs/arc/hd_arc.md).

    python3 -B tools/arc_hd/gen_arc_hd.py <look name or base id ...> [--size 64|128|256] [--baseline] [--exact] [--all]
                                           [--out-dir DIR] [--frag-dir DIR] [--list]

For each look name (a base id also does its `_form2`; `--exact` does only the names given; `--all` is every id of tools/arc_hd/roster.py):
  1. tools/arc_hd/chars/<name>.py `paint(spec, cid, masks) -> (skin 256, glow 256 or None, hair style id, hair rgb, slim)` when it exists (a character painted by hand; `--baseline` ignores it),
  2. else the BASELINE: the 64x64 generator's result (tools/playermodel/gen_skins.paint_character, scalp painted for the hair of hair_map.py) through the generic HD bridge of
     tools/tier_bosses/hd (smooth upscale, cloth gradients, outlines, metal bevel) with materials named from the character's sheet.
  `--size 128` averages the 256 result down by 2; `--size 64` writes the OLD 64x64 skin and its classic hair (the generator keeps the old look reproducible, CLAUDE.md rule 2).
It writes assets/nusmp/textures/entity/playermodel/<name>.png (+ _glow.png, the same size) and the look fragment tools/arc_hd/looks/<name>.json (the look entry of looks.json for this name:
skin, slim, scale, holds, glow, hair, hair_color, extras). It never touches looks.json or any other character: run tools/arc_hd/merge_looks.py afterwards.
Needs numpy and Pillow. About 1.5 s per character.
"""
import os
import sys
import time

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import arc_hdlib as A  # noqa: E402


def parse(argv):
    opts = {"--size": "256", "--out-dir": None, "--frag-dir": None}
    flags = {"--baseline", "--exact", "--all", "--list", "--quiet"}
    ids, i = [], 0
    got = set()
    while i < len(argv):
        a = argv[i]
        if a in opts:
            if i + 1 >= len(argv):
                raise SystemExit(a + " needs a value")
            opts[a] = argv[i + 1]
            i += 2
            continue
        if a in flags:
            got.add(a)
        elif a.startswith("--"):
            raise SystemExit("unknown option " + a)
        else:
            ids.append(a)
        i += 1
    size = int(opts["--size"])
    if size not in A.SIZES:
        raise SystemExit("--size must be one of %s, not %s" % (A.SIZES, size))
    return ids, size, got, opts


def classic64(name, spec, masks):
    """The old 64x64 look: the generator's own painter with its classic hair."""
    return A.blueprint(name, spec, masks, hair=None)


def paint_one(name, size, baseline_only, masks):
    """-> (skin, glow, hair id, hair rgb, slim, source)."""
    spec = A.spec_of(name)
    if size == 64:
        img, glow, hs, hrgb, slim = classic64(name, spec, masks)
        return img, glow, hs, hrgb, slim, "classic64"
    hand = None if baseline_only else A.load_hand(name)
    if hand is not None:
        img, glow, hs, hrgb, slim = hand(spec, name, masks)
        img, glow = A.to_size(img, glow, size)
        return img, glow, hs, hrgb, slim, "hand"
    img, glow, hs, hrgb, slim = A.baseline(name, spec, masks, size=size)
    return img, glow, hs, hrgb, slim, "baseline"


def check_result(name, spec, img, glow, hs, slim, size):
    """The contract of a character module and of the baseline; a violation stops the run for this name."""
    from arc_hdlib import GS
    if img.size != (size, size):
        raise ValueError("%s: skin is %s, expected %dx%d" % (name, img.size, size, size))
    if glow is not None and glow.size != img.size:
        raise ValueError("%s: glow map %s differs from its skin %s" % (name, glow.size, img.size))
    if bool(slim) != bool(GS.is_slim(spec)):
        raise ValueError("%s: slim=%s but the sheet says %s" % (name, slim, GS.is_slim(spec)))
    if hs and hs not in A.HM.known_styles():
        raise ValueError("%s: hair style %r is not in hair_styles.json" % (name, hs))


def main(argv):
    ids, size, got, opts = parse(argv)
    table = A.job_table()
    if "--list" in got:
        for n in A.expand(A.RO.BASE_IDS):
            print(n, "(hand)" if os.path.isfile(A.hand_module_path(n)) else "(baseline)")
        return 0
    if "--all" in got:
        ids = ids + A.RO.BASE_IDS
    if not ids:
        raise SystemExit(__doc__)
    names = A.expand(ids, exact="--exact" in got)
    out_dir = opts["--out-dir"] or A.SKINS
    frag_dir = opts["--frag-dir"]
    os.makedirs(out_dir, exist_ok=True)
    masks = A.hair_masks()
    looks = A.read_looks()
    t0 = time.time()
    failed = []
    for name in names:
        try:
            spec = A.spec_of(name)
            img, glow, hs, hrgb, slim, source = paint_one(name, size, "--baseline" in got, masks)
            check_result(name, spec, img, glow, hs, slim, size)
            existing = A.existing_entry(name, frag_dir, looks)
            look = A.entry_for(existing, slim, glow is not None, hs, hrgb)
            img.save(os.path.join(out_dir, name + ".png"), optimize=True)
            gpath = os.path.join(out_dir, name + "_glow.png")
            if glow is not None:
                glow.save(gpath, optimize=True)
            elif os.path.isfile(gpath):
                os.remove(gpath)                                  # the old skin had a glow map, this one has none: no stale map of the wrong size
            A.write_fragment(name, look, source, size, A.HM.reason_for(name) if source != "hand" else "painted by hand: tools/arc_hd/chars/%s.py" % name, frag_dir)
            if "--quiet" not in got:
                print("%-30s %-9s %3d  hair=%-18s glow=%s" % (name, source, size, hs, "yes" if glow is not None else "no"))
        except Exception as ex:                                    # one bad character must not stop the others
            import traceback
            traceback.print_exc()
            failed.append(name)
            print("FAILED", name, repr(ex))
    print("painted %d of %d in %.1f s%s" % (len(names) - len(failed), len(names), time.time() - t0, (", FAILED: " + ", ".join(failed)) if failed else ""))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
