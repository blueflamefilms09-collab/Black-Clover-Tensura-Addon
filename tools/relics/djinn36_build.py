#!/usr/bin/env python3
"""djinn36_build: generator of the art of the 24 new Djinn (Bael ... Murmur). Idempotent; the first twelve's files are never touched.

    python3 -B tools/relics/djinn36_build.py                           everything for all 24
    python3 -B tools/relics/djinn36_build.py --djinn bael,agares       some Djinn
    python3 -B tools/relics/djinn36_build.py --only armor|base|skins|weapons|seals|lang|ids   one stage
    python3 -B tools/relics/djinn36_build.py --preview OUT_DIR         mannequin sheets (half, full, back, weapon; light and dark)

Writes under src/main/resources/assets/nusmp (the same names as the first twelve, so the client's name-keyed loaders find them with no code):
    djinn/u_<id>/*.txt, djinn/d_<id>/*.txt                   the unique and the Dark Djinn armour pieces
    djinn/<id>/*.txt                                         the shared-rig fallback (512 px skin)
    textures/entity/djinn/<id>_{u,d}_armor[_glow].png, <id>_{,u_,d_}rune.png, <id>_d_art.png, <id>_{,d_}bust.png, <id>_armor[_glow].png
    weapon_meshes/djinn_weapon_<id>[_dark].txt, textures/entity/weapon/..., models/item/..., textures/item/... (via djinn36_weapons)
    textures/item/djinn_seal_<id>.png, models/item/djinn_seal_<id>.json
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn36_lib as L                                             # noqa: E402
from djinn36_lib import ml, du, dua, duh, dub                       # noqa: E402
import djinn36_roster as R                                          # noqa: E402
from djinn36_reg import SETS                                        # noqa: E402
import gen_djinn_art as ga                                          # noqa: E402
import gen_djinn_unique as gdu                                      # noqa: E402
import djinn_dark as dd                                             # noqa: E402

for _f in sorted(os.listdir(HERE)):
    if _f.startswith("djinn36_set") and _f.endswith(".py"):
        __import__(_f[:-3])

ASSETS = ga.ASSETS
FAM_VAMB = gdu.FAM_VAMB
FAM_GREAVE = gdu.FAM_GREAVE


def dj_of(name):
    return R.djinn_dict(name)


# ================================================================ the armour mesh
def build_mesh(dj, variant):
    name, did = dj["name"], dj["id"]
    s = SETS[name]
    du.register_materials(did, name, dj["color"], dj["glow"], variant)
    m = ml.Mesh(f"{did}_{variant}_armor")
    k = du.UKit(m, did, dj, variant)
    fam = L.FAMILY36[name]
    k.st = dict(helm="open", pauldron="dome", vambrace=FAM_VAMB[fam], greave=FAM_GREAVE[fam])
    s["head"](k)
    s["chest"](k)
    s.get("belt", dua.belt_light)(k)
    s["pauldron"](k)
    dua.vambrace_light(k)
    dua.forearm_extra(k, name)
    dua.gauntlet_light(k)
    dua.greave_light(k)
    dua.greave_extra(k, name)
    for sgn in (1, -1):
        s["wings"](k, sgn)
    if "tail" in s and name not in R.TAIL_NONE:
        s["tail"](k)
    if k.dark:
        L.halo_dark36(k, R.DARK_SPIKES[name])
        duh.build_eye(k)
        dd.dark_extras(k)
    elif "halo" in s:
        s["halo"](k)
    s["back"](k)
    if "mantle" in s:
        s["mantle"](k)
    s["weapon"](k)
    m.parts = [p for p in m.parts if p.F]
    m.set_glow("FFFFFF", 0.12, 0.7, 1.0)
    return m


gdu.build_mesh = build_mesh           # the first generation's preview scenes (mannequin_scene / weapon_scene) now build the new Djinn


def make_armor(dj, variant, atlas_size=1024):
    mesh = build_mesh(dj, variant)
    atlas = gdu.finish_atlas(mesh, dj, atlas_size, variant)
    counts = gdu.export_pieces(mesh, atlas, dj["id"], variant, gdu.sub_dir(dj["id"], variant))
    gdu.save_atlas(atlas, dj["id"], variant)
    return mesh, atlas, counts


def make_base_rig(dj, atlas_size=512):
    """The first twelve's shared rig for a new Djinn (the fallback set the client uses only when a unique set is missing), plus its rune and bust."""
    import djinn_icons as di
    mesh, atlas, counts = ga.make_armor(dj, atlas_size)
    di.make_skin(dj, ASSETS)
    di.make_bust(dj, ASSETS)
    return counts


def selected(arg):
    if not arg:
        return list(R.NAMES)
    want = {a.strip().upper() for a in arg.split(",")}
    out = [n for n in R.NAMES if n in want]
    if not out:
        raise SystemExit("no such Djinn: " + arg)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--djinn")
    ap.add_argument("--variant", choices=["u", "d"])
    ap.add_argument("--only", choices=["armor", "base", "skins", "weapons", "seals", "lang", "ids", "doc"])
    ap.add_argument("--preview")
    ap.add_argument("--icons", help="write the icon contact sheet (seals and weapon icons) into this directory")
    ap.add_argument("--atlas", type=int, default=1024)
    a = ap.parse_args(argv)
    if a.icons:
        import djinn36_preview as pv
        print("icons", pv.icon_sheet(a.icons))
        return 0
    names = selected(a.djinn)
    for name in names:
        dj = dj_of(name)
        if a.preview:
            import djinn36_preview as pv
            print("sheet", pv.sheet(dj, a.preview))
            continue
        if a.only in (None, "armor"):
            for var in ([a.variant] if a.variant else ("u", "d")):
                mesh, atlas, counts = make_armor(dj, var, a.atlas)
                frame = sum(counts.values()) + 4 * counts.get("tail_seg", 0)
                print(dj["id"], var, "quads", sum(counts.values()), "frame", frame, "pieces", len(counts), "ppu %.2f" % atlas.ppu, "max", max(counts, key=counts.get), max(counts.values()))
        if a.only in (None, "base"):
            c = make_base_rig(dj)
            print(dj["id"], "base rig", sum(c.values()), "quads")
        if a.only in (None, "skins"):
            import djinn36_art2d as a2
            for var in ([a.variant] if a.variant else ("u", "d")):
                a2.make_skins(dj, var, ASSETS)
        if a.only in (None, "seals"):
            import djinn36_art2d as a2
            a2.make_seal(dj, ASSETS)
        if a.only in (None, "weapons"):
            import djinn36_weapons as w36
            w36.make_weapons(dj)
    if a.only in (None, "lang", "ids", "doc") and not a.preview:
        import djinn36_doc as doc
        doc.write_lang()
        doc.write_ids()
        doc.write_doc()
    return 0


if __name__ == "__main__":
    sys.exit(main())
