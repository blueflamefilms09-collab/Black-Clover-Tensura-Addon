#!/usr/bin/env python3
"""Generator of the UNIQUE and DARK Djinn armour (relics/djinn, client/relics/djinn). Idempotent. The old shared rig (gen_djinn_art.py) is untouched.

    python3 tools/relics/gen_djinn_unique.py                          everything: meshes + skins for all twelve, unique and dark
    python3 tools/relics/gen_djinn_unique.py --djinn baal --variant d   one Djinn, one variant (u = the Djinn's own look, d = the Dark Djinn)
    python3 tools/relics/gen_djinn_unique.py --preview OUT_DIR           render previews (full, half, dark full, weapon) beside the reference crops
    python3 tools/relics/gen_djinn_unique.py --only art|skins            only the meshes + atlas, or only the 2D skins (rune, art, bust)

Writes under src/main/resources/assets/nusmp:
    djinn/u_<id>/<piece>.txt, djinn/d_<id>/<piece>.txt         the pieces (qn quads, UV space 128; WEAPON is item space, the rest model space)
    textures/entity/djinn/<id>_u_armor.png (+_glow), <id>_d_armor.png (+_glow)        the painted skins + additive glow
    textures/entity/djinn/<id>_u_rune.png, <id>_d_rune.png                            the body-art overlays (additive)
    textures/entity/djinn/<id>_d_art.png                                              the Dark Djinn's black veins on the skin (alpha blended)
    textures/entity/djinn/<id>_d_bust.png                                             the Dark Lesser Summon's bust
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                      # noqa: E402
import djinn_parts as dp                   # noqa: E402
import gen_djinn_art as ga                 # noqa: E402
import djinn_unique as du                  # noqa: E402
import djinn_unique_heads as duh           # noqa: E402
import djinn_unique_back as dub            # noqa: E402
import djinn_unique_arms as dua            # noqa: E402

ASSETS = ga.ASSETS
VARIANTS = ("u", "d")
FAM_VAMB = {"bolt": "fin", "flame": "claw", "wave": "plain", "feather": "fin", "ray": "plain", "thorn": "plain", "plate": "claw", "bell": "plain"}
FAM_GREAVE = {"bolt": "fin", "flame": "spike", "wave": "plain", "feather": "fin", "ray": "plain", "thorn": "plain", "plate": "spike", "bell": "plain"}
DARK_SPIKES = {"BAAL": 9, "AMON": 10, "VINEA": 8, "FOCALOR": 8, "ZAGAN": 8, "ZEPAR": 7, "FURFUR": 10, "MARCHOSIAS": 9, "SITRI": 11, "BIFRON": 9, "PAIMON": 9, "VASSAGO": 8}


# ================================================================ build
def build_mesh(dj, variant):
    did, name = dj["id"], dj["name"]
    du.register_materials(did, name, dj["color"], dj["glow"], variant)
    m = ml.Mesh(f"{did}_{variant}_armor")
    k = du.UKit(m, did, dj, variant)
    fam = dua.FAMILY[name]
    k.st = dict(helm="open", pauldron="dome", vambrace=FAM_VAMB[fam], greave=FAM_GREAVE[fam])
    duh.HEADS[name](k)
    dua.build_chest(k, name)
    dua.build_belt(k, name)
    dua.pauldron_for(k, name)
    dua.vambrace_light(k)
    dua.forearm_extra(k, name)
    dua.gauntlet_light(k)
    dua.greave_light(k)
    dua.greave_extra(k, name)
    for sgn in (1, -1):
        dub.WINGS[name](k, sgn)
    if name in dub.TAILS:
        dub.TAILS[name](k)
    if k.dark:
        dub.halo_dark(k, DARK_SPIKES[name])
        duh.build_eye(k)
        import djinn_dark as dd
        dd.dark_extras(k)
    elif name in dub.HALOS:
        dub.HALOS[name](k)
    dub.BACKS[name](k)
    if name in dub.MANTLES:
        dub.MANTLES[name](k)
    dua.WEAPONS[name](k)
    m.parts = [p for p in m.parts if p.F]
    m.set_glow("FFFFFF", 0.12, 0.7, 1.0)
    return m


def glow_rgb(dj, variant):
    """The colour of the additive glow skin: the Djinn's pale glow; for the Dark Djinn its saturated colour with only 30% of the pale glow (the
    pale glow washes out against black metal). The Java side mirrors this in DjinnLook.darkGlow."""
    if variant == "d":
        return du.mix(ga.rgb(dj["color"]), ga.rgb(dj["glow"]), 0.3)
    return ga.rgb(dj["glow"])


def finish_atlas(mesh, dj, size=1024, variant="u"):
    atlas = ml.Atlas(size)
    atlas.layout(mesh)
    atlas.paint(mesh)
    ga.paint_glow(atlas, mesh, glow_rgb(dj, variant))
    return atlas


def sub_dir(did, variant):
    return os.path.join(ASSETS, "djinn", f"{variant}_{did}")


def export_pieces(mesh, atlas, did, variant, out_dir):
    """One file per piece. The WEAPON is item space (as authored), every other piece is turned to model space ((x, y, z) -> (x, -y, -z))."""
    mesh.finish()
    by = {}
    for layer, P, UV, N, part in mesh.iter_quads("vertex", atlas):
        by.setdefault(part.name.split("__", 1)[0], []).append((layer, P, UV, N))
    os.makedirs(out_dir, exist_ok=True)
    for f in os.listdir(out_dir):
        if f.endswith(".txt"):
            os.remove(os.path.join(out_dir, f))              # a piece this build no longer has must not linger
    flip_v = np.array([1.0, -1.0, -1.0])
    counts = {}
    for piece in du.PIECES:
        quads = by.get(piece, [])
        if not quads:
            continue
        flip = np.ones(3) if piece == "weapon" else flip_v
        lines = [f"# {did} {variant} {piece}: generated by tools/relics/gen_djinn_unique.py (mesh_lib); " +
                 ("item space (grip at the origin, blade along +y)" if piece == "weapon" else "model space") + "; UV space 128"]
        for layer, P, UV, N in quads:
            if layer == 1:
                continue
            lines.append("qn %d %s" % (layer, " ".join("%.2f %.2f %.2f %.2f %.2f %.3f %.3f %.3f" % (*(P[i] * flip), *UV[i], *(N[i] * flip)) for i in range(4))))
        with open(os.path.join(out_dir, piece + ".txt"), "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        counts[piece] = len(lines) - 1
    return counts


def save_atlas(atlas, did, variant):
    d = os.path.join(ASSETS, "textures", "entity", "djinn")
    os.makedirs(d, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, f"{did}_{variant}_armor.png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, f"{did}_{variant}_armor_glow.png"), optimize=True)


def make_armor(dj, variant, atlas_size=1024):
    mesh = build_mesh(dj, variant)
    atlas = finish_atlas(mesh, dj, atlas_size, variant)
    counts = export_pieces(mesh, atlas, dj["id"], variant, sub_dir(dj["id"], variant))
    save_atlas(atlas, dj["id"], variant)
    return mesh, atlas, counts


# ================================================================ the mannequin preview
ANCHOR = dict(dp.ANCHOR)
HALF_SET = ("crest", "pauldron_r", "vambrace_r", "gauntlet_r", "weapon", "eye")


def mannequin_scene(dj, variant, half=False, wings_open=True, tail_segs=3):
    base = build_mesh(dj, variant)
    scene = ml.Mesh(dj["id"] + "_scene")
    for p in base.parts:
        piece = p.name.split("__", 1)[0]
        if half and piece not in HALF_SET:
            continue
        sgn = 1 if piece.endswith("_l") else -1
        if piece in ANCHOR:
            q = p.translated(*ANCHOR[piece], name=p.name)
        elif piece in ("wing_in_l", "wing_in_r"):
            q = p.rotated("z", sgn * 38).translated(sgn * 1.8, -3.0, -2.9, name=p.name) if wings_open else p.translated(sgn * 1.8, -3.0, -2.9, name=p.name)
        elif piece in ("wing_out_l", "wing_out_r"):
            q = p.rotated("z", sgn * 13).translated(sgn * (1.8 + 11 * 0.788), -3.0 + 11 * 0.616, -3.2, name=p.name)
        elif piece == "tail_seg":
            for i in range(tail_segs):
                s = 1 - 0.1 * i
                M = np.diag([s, s, s])
                scene.add(p.transformed(M, (0, -11.0 - 0.4 * i, -2.8 - 6.0 * i * s), name=p.name + f"#{i}"))
            continue
        elif piece == "tail_tip":
            q = p.translated(0, -11.0 - 0.4 * tail_segs, -2.8 - 6.0 * tail_segs * 0.85, name=p.name)
        elif piece == "halo":
            q = p.translated(0, 12.5, 0, name=p.name)
        elif piece == "backdrop":
            q = p.translated(0, 2.0, -5.5, name=p.name)
        elif piece == "weapon":
            q = p.rotated("x", 90).translated(-6.0, -11.0, 1.0, name=p.name)
        else:
            q = p.translated(0, 0, 0, name=p.name)
        scene.add(q)

    def box(name, p0, p1, bc=(150, 118, 96)):
        part = ml.Part("mq__" + name, material="stone", base=bc, smooth=0)
        ml.box(part, p0, p1)
        scene.add(part)
    skin = (150, 118, 96)
    box("head", (-4, 0, -4), (4, 8, 4), skin)
    box("body", (-4, -12, -2), (4, 0, 2), (90, 90, 110))
    box("arm_r", (-8, -12, -2), (-4, 0, 2), skin)
    box("arm_l", (4, -12, -2), (8, 0, 2), skin)
    box("leg_r", (-4, -24, -2), (0, -12, 2), (70, 70, 90))
    box("leg_l", (0, -24, -2), (4, -12, 2), (70, 70, 90))
    scene.set_glow("FFFFFF", 0.12, 0.9, 1.0)
    return scene


def weapon_scene(dj, variant):
    base = build_mesh(dj, variant)
    scene = ml.Mesh(dj["id"] + "_weapon")
    for p in base.parts:
        if p.name.startswith("weapon__"):
            scene.add(p.translated(0, 0, 0, name=p.name))
    scene.set_glow("FFFFFF", 0.12, 0.9, 1.0)
    return scene


def render(scene, dj, out, views, panel, size=2048, variant="u"):
    atlas = finish_atlas(scene, dj, size, variant)
    ml.preview(scene, out_png=out, atlas=atlas, views=tuple(views), panel=panel, normals="vertex", label=False)
    return out


def crop_ref(ref_png, box, size):
    im = Image.open(ref_png).convert("RGB")
    im = im.crop(box)
    sc = min(size[0] / im.width, size[1] / im.height)
    im = im.resize((max(1, int(im.width * sc)), max(1, int(im.height * sc))), Image.LANCZOS)
    t = Image.new("RGB", size, (84, 89, 107))
    t.paste(im, ((size[0] - im.width) // 2, (size[1] - im.height) // 2))
    return t


REF = {}   # filled by main: djinn id -> list of (path, crop box)


def refs_for(did, dark, refdir):
    c59 = os.path.join(refdir, "59.png")
    tiles = {"baal": 1, "amon": 2, "vinea": 3, "focalor": 4, "furfur": 5, "bifron": 5, "zepar": 6, "vassago": 7, "sitri": 8, "paimon": 9, "marchosias": 2, "zagan": 6}
    tn = tiles[did] - 1
    box59 = ((tn % 3) * 360, (tn // 3) * 539, (tn % 3) * 360 + 360, (tn // 3) * 539 + 539)
    if dark:
        return [(os.path.join(refdir, "60.png"), (0, 0, 652, 1024)), (os.path.join(refdir, "61.png"), (0, 0, 524, 672)), (os.path.join(refdir, "62.png"), (200, 0, 800, 563))]
    out = [(c59, box59)]
    if did == "sitri":
        out.insert(0, (os.path.join(refdir, "61.png"), (0, 0, 524, 672)))      # the gold-armoured Djinn with the halo and the extra arms
    return out


def make_sheet(dj, outdir, refdir, panel=(300, 480)):
    """One image per Djinn: [reference crop(s)] [Half Equip 3/4] [Full unique 3/4 + back] [weapon] [Dark Full 3/4 + back] [Dark half]."""
    os.makedirs(outdir, exist_ok=True)
    tmp = os.path.join(outdir, "_t")
    os.makedirs(tmp, exist_ok=True)
    did = dj["id"]
    tiles = []
    for p, bx in refs_for(did, False, refdir)[:1]:
        tiles.append(crop_ref(p, bx, panel))
    jobs = [("half_u", dj, "u", True, ["three_quarter"]), ("full_u", dj, "u", False, ["three_quarter", "back"])]
    for name, d_, var, half, views in jobs:
        sc = mannequin_scene(d_, var, half)
        out = render(sc, d_, os.path.join(tmp, f"{did}_{name}.png"), views, panel, variant=var)
        tiles.append(Image.open(out).convert("RGB"))
    wsc = weapon_scene(dj, "u")
    tiles.append(Image.open(render(wsc, dj, os.path.join(tmp, f"{did}_weapon.png"), ["profile"], (200, panel[1]), 1024)).convert("RGB"))
    for p, bx in refs_for(did, True, refdir)[:1]:
        tiles.append(crop_ref(p, bx, panel))
    for name, half, views in (("full_d", False, ["three_quarter", "back"]), ("half_d", True, ["three_quarter"])):
        sc = mannequin_scene(dj, "d", half)
        out = render(sc, dj, os.path.join(tmp, f"{did}_{name}.png"), views, panel, variant="d")
        tiles.append(Image.open(out).convert("RGB"))
    W = sum(t.width for t in tiles)
    H = max(t.height for t in tiles)
    sheet = Image.new("RGB", (W, H), (84, 89, 107))
    x = 0
    for t in tiles:
        sheet.paste(t, (x, 0))
        x += t.width
    path = os.path.join(outdir, f"{did}_sheet.png")
    sheet.save(path)
    return path


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--djinn")
    ap.add_argument("--variant", choices=VARIANTS)
    ap.add_argument("--only", choices=["art", "skins"])
    ap.add_argument("--preview")
    ap.add_argument("--refs", default=os.environ.get("DJINN_REFS", ""))
    ap.add_argument("--atlas", type=int, default=1024)
    a = ap.parse_args(argv)
    djs = [d for d in ga.roster() if not a.djinn or d["id"] == a.djinn.lower()]
    if not djs:
        raise SystemExit("no such Djinn")
    for dj in djs:
        if a.preview:
            print("sheet", make_sheet(dj, a.preview, a.refs))
            continue
        for var in ([a.variant] if a.variant else VARIANTS):
            if a.only in (None, "art"):
                mesh, atlas, counts = make_armor(dj, var, a.atlas)
                print(dj["id"], var, "quads", sum(counts.values()), "in", len(counts), "pieces; ppu %.2f" % atlas.ppu, " heaviest", max(counts, key=counts.get), max(counts.values()))
            if a.only in (None, "skins"):
                import djinn_dark as dd
                dd.make_skins(dj, var, ASSETS)


if __name__ == "__main__":
    main(sys.argv[1:])
