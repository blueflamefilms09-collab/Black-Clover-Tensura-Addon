"""bow_size: measure every bow of the mod, check the new size against the hold audit, and draw the before / after mock-ups (docs/heroweapon/bow_size.md).

    python3 -B tools/relics/bow_size.py --table            # markdown table: bow, tier, mesh length, third / first person apparent length before and after, the factor
    python3 -B tools/relics/bow_size.py --hold             # the hold audit (tools/weapon_grip/hold_audit.py) of every bow, before and after the factor: which flags the size adds
    python3 -B tools/relics/bow_size.py --preview          # software mock-ups: docs/previews/bows_bigger/*.png (before / after, third and first person)

The sizes are read from visual/sizes/BowSize.java itself (the tiers, the table, the constants), so this tool cannot drift from the game. Nothing here needs a game and nothing here was seen in one:
the mock-ups are the software rasteriser of tools/item_preview (the same one the grip sheets use), with the display block of the item model and the grip shift of the game's own rule.
"""
import argparse
import json
import math
import os
import re
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
sys.path.insert(0, os.path.join(ROOT, "tools", "weapon_grip"))
sys.path.insert(0, os.path.join(ROOT, "tools", "item_preview"))
sys.path.insert(0, HERE)

JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "visual", "sizes", "BowSize.java")
STAGE = re.compile(r"_pull[123]$")


def read_java():
    """(tiers {NAME: (target, min, max)}, table {id: NAME}, constants) of BowSize.java."""
    src = open(JAVA, encoding="utf-8").read()
    tiers = {m.group(1): tuple(float(x) for x in m.groups()[1:]) for m in re.finditer(r"^\s*(HERO|FORM|RELIC)\(([\d.]+)f, ([\d.]+)f, ([\d.]+)f\)", src, re.M)}
    table = {m.group(1): m.group(2) for m in re.finditer(r'Map\.entry\("([a-z0-9_]+)", Tier\.(\w+)\)', src)}
    const = {k: float(re.search(k + r"\s*=\s*([\d.]+)f", src).group(1)) for k in ("TP_SCALE", "FP_SCALE", "FP_MAX_APPARENT", "MAX_FACTOR")}
    return tiers, table, const


TIERS, TABLE, C = read_java()


def mesh_length(path):
    """Tip to tip in blocks as WeaponRenderer.bowFactor measures it: the extent along y of every quad of layers 0 to 2 times the place scale (a few meshes reach past the place line's ymin)."""
    ys, place = [], None
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if not v:
            continue
        if v[0] == "place":
            place = [float(x) for x in v[1:6]]
        elif v[0] in ("q", "qn") and int(v[1]) <= 2:
            st = 5 if v[0] == "q" else 8
            if len(v) >= 2 + 4 * st:
                ys += [float(v[2 + k * st + 1]) for k in range(4)]
    return (max(ys) - min(ys)) * place[3]


def variants(rest):
    """The mesh ids of a bow: the rest mesh and its draw stages (top-level weapon_meshes only; the classic copies are other looks of the same ids)."""
    out = [rest]
    for n in (1, 2, 3):
        if os.path.exists(os.path.join(ASSETS, "weapon_meshes", "%s_pull%d.txt" % (rest, n))):
            out.append("%s_pull%d" % (rest, n))
    return out


def lengths():
    return {m: mesh_length(os.path.join(ASSETS, "weapon_meshes", m + ".txt")) for rest in TABLE for m in variants(rest)}


def base_factor(rest, rest_len):
    t = TIERS[TABLE[rest]][0]
    return min(C["MAX_FACTOR"], max(1.0, t / (rest_len * C["TP_SCALE"])))


def factor(mesh_id, fp, lens, on=True, scale=1.0):
    """What WeaponRenderer.bowFactor returns."""
    rest = STAGE.sub("", mesh_id)
    if not on or rest not in TABLE:
        return 1.0
    k = base_factor(rest, lens[rest]) * scale
    if fp:
        k = min(k, max(1.0, C["FP_MAX_APPARENT"] / (lens[mesh_id] * C["FP_SCALE"])))
    return k


def rows():
    lens = lengths()
    out = []
    for rest in sorted(TABLE, key=lambda r: (TIERS[TABLE[r]][0] * -1, r)):
        L = lens[rest]
        stages = [lens[m] for m in variants(rest) if m != rest]
        out.append(dict(id=rest, tier=TABLE[rest], mesh=L, stage_min=min(stages) if stages else L, stage_max=max(stages) if stages else L,
                        tp0=L * C["TP_SCALE"], tp1=L * factor(rest, False, lens) * C["TP_SCALE"], fp0=L * C["FP_SCALE"], fp1=L * factor(rest, True, lens) * C["FP_SCALE"],
                        k_tp=factor(rest, False, lens), k_fp=factor(rest, True, lens)))
    return out


def print_table():
    print("| bow | tier | mesh (blocks) | 3rd person before | 3rd person after | 1st person before | 1st person after | factor 3rd / 1st | draw stages (mesh) |")
    print("|---|---|---|---|---|---|---|---|---|")
    for r in rows():
        print("| `%s` | %s | %.3f | %.2f | **%.2f** | %.2f | **%.2f** | x%.2f / x%.2f | %.3f to %.3f |" % (r["id"], r["tier"].lower(), r["mesh"], r["tp0"], r["tp1"], r["fp0"], r["fp1"], r["k_tp"], r["k_fp"], r["stage_min"], r["stage_max"]))


# ---------------------------------------------------------------- hold audit before / after
def hold():
    import hold_audit as ha
    lens = lengths()
    ids = sorted(lens)
    ns = argparse.Namespace(ids=",".join(ids), all=True, old=False)
    ws = ha.weapons(ns)
    ws = [w for w in ws if w.id in lens]
    print("%d bow meshes audited (rest and draw stages; the audit judges the rest meshes of hero weapons only for BODY / ARM, as it always did)" % len(ws))
    before, after = {}, {}
    ha.BOW_K = None
    for w in ws:
        before[w.id] = {r["ctx"]: r.get("flag", "") for r in ha.measure(w)}
    ha.BOW_K = lambda mid, fp: factor(mid, fp, lens)
    for w in ws:
        after[w.id] = {r["ctx"]: r.get("flag", "") for r in ha.measure(w)}
    worse, changed = [], 0
    for mid in sorted(before):
        if before[mid] != after[mid]:
            changed += 1
            worse.append("%-44s before %s | after %s" % (mid, {k: v for k, v in before[mid].items() if v}, {k: v for k, v in after[mid].items() if v}))
    print("%d meshes with a different flag set after the size:" % changed)
    for l in worse:
        print("  " + l)
    nb = sum(1 for m in before.values() for v in m.values() if v)
    na = sum(1 for m in after.values() for v in m.values() if v)
    print("flagged contexts before %d, after %d" % (nb, na))
    return 0


# ---------------------------------------------------------------- mock-ups
def preview(out_dir, size=300):
    import preview_grip as pg
    import preview_weapons as pw
    import grip_lib as gl
    from PIL import Image, ImageDraw
    import hold_audit as ha
    lens = lengths()
    pw.W = pw.H = size
    pw.STARS = np.zeros((size, size, 3), np.float32) + [0.05, 0, 0.02]
    classes = gl.classes()
    table = json.load(open(os.path.join(ASSETS, "weapon_grips.json")))

    def tile(mesh_id, fp, on):
        path, kind = pg.mesh_paths(mesh_id)
        place, glow, skin_name, quads = pg.read_quads(path)
        pl, directive, xyz, body = gl.load_mesh(path)
        box = gl.body_box(body)
        base = gl.STATE_SUFFIX.sub("", mesh_id)
        cls = gl.grip_class(mesh_id, classes, table)
        cg = classes["classes"].get(cls, {"grip": 0.5})["grip"]
        ov = (table.get("fractions") or {}).get(mesh_id, (table.get("fractions") or {}).get(base))
        gx, gy, gz, frac, src = gl.grip_point(body, cls, cg, override=ov, directive=directive, explicit=xyz)
        model = ha.base_json(mesh_id, True)
        skin_dir = os.path.join(ASSETS, "textures", "entity", "weapon_vu" if kind == "vu" else "weapon")
        wdir = os.path.join(ASSETS, "textures", "entity", "weapon")
        skin, glowtex = None, None
        for d_, n_ in ((skin_dir, mesh_id), (wdir, mesh_id), (wdir, base)):
            skin = pw.tex(os.path.join(d_, n_ + ".png"))
            if skin is not None:
                glowtex = pw.tex(os.path.join(d_, n_ + "_glow.png"))
                break
        k = skin.shape[1] / 128.0
        quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * k], axis=1), n) for (ly, pts, n) in quads]
        disp = gl.read_display(model, "firstperson_righthand" if fp else "thirdperson_righthand")
        tgt = gl.fp_target(disp, False) if fp else gl.display_for(model, "thirdperson_righthand")
        gpt = (gx, gy, gz)
        shift = np.array(gl.shift_for(place, gpt, tgt))
        if fp:
            shift = shift * gl.fp_share(place, box, tuple(shift), disp, False)
        kk = factor(mesh_id, fp, lens, on=on)
        land = np.array(gl.cube_point(place, *gpt)) + shift                                  # where the grip lies: the pivot
        rot, tr, sc = disp
        R = np.array(gl.rot_matrix(*rot))
        t = np.array(tr) / 16.0 + (np.array(gl.HAND_FP) if fp else 0)
        qh = []
        for (ly, pts, n) in quads:
            p = pts.copy()
            for j in range(4):
                c = np.array(gl.cube_point(place, *pts[j, :3])) + shift
                c = land + kk * (c - land)
                p[j, :3] = t + R @ (sc * (c - 0.5))
            qh.append((ly, p, R @ (pg.rot_to_cube(place) @ n)))
        origin = t + R @ (sc * (np.array(tgt) - 0.5))
        if fp:
            cam = (np.zeros(3), np.array([0, 0, -1.0]), np.array([1.0, 0, 0]), np.array([0, 1.0, 0]))
            pw.project = pg.fp_project(size)
        else:
            cam = pg.view_at(origin, 90, 0, 2.2)
            pw.project = pg.project_default(size)
        hp = pw.project(np.array([origin]), cam)[0]
        img = pw.render(qh, skin, glowtex, glow, cam)
        im = Image.fromarray((img * 255).astype(np.uint8))
        d = ImageDraw.Draw(im)
        if not fp:
            r = 0.13 * ((size / 2) / math.tan(math.radians(25))) / max(hp[2], 0.05)
            d.ellipse([hp[0] - r, hp[1] - r, hp[0] + r, hp[1] + r], outline=(255, 60, 60), width=2)
        L = lens[mesh_id] * kk * (C["FP_SCALE"] if fp else C["TP_SCALE"])
        d.text((4, 3), ("%s %s %.2f blocks" % ("first" if fp else "third", "after" if on else "before", L)), fill=(255, 255, 255))
        return np.asarray(im, np.float32) / 255

    os.makedirs(out_dir, exist_ok=True)
    groups = {"hero_and_forms": [r for r in TABLE if TABLE[r] in ("HERO", "FORM")], "relic_bows": [r for r in TABLE if TABLE[r] == "RELIC"]}
    for name, ids in groups.items():
        lines = []
        for rest in sorted(ids):
            cols = [tile(rest, False, False), tile(rest, False, True), tile(rest, True, False), tile(rest, True, True)]
            row = np.concatenate(cols, 1)
            im = Image.fromarray((row * 255).astype(np.uint8))
            ImageDraw.Draw(im).text((size * 4 - 8 * len(rest) - 6, size - 14), rest, fill=(255, 220, 90))
            lines.append(np.asarray(im, np.float32) / 255)
        per = 2
        while len(lines) % per:
            lines.append(np.zeros_like(lines[0]))
        sheet = np.concatenate([np.concatenate(lines[i:i + per], 1) for i in range(0, len(lines), per)], 0)
        p = os.path.join(out_dir, name + ".png")
        Image.fromarray((sheet * 255).astype(np.uint8)).save(p, optimize=True)
        print("wrote", p)
    for rest in ("legendary_bow", "legendary_bow__scouts_bow"):                       # a close pair: the hero bow, the 3 draw stages after
        cols = [tile(rest, False, False), tile(rest, False, True)] + [tile(m, False, True) for m in variants(rest)[1:]]
        p = os.path.join(out_dir, rest + "_stages.png")
        Image.fromarray((np.concatenate(cols, 1) * 255).astype(np.uint8)).save(p, optimize=True)
        print("wrote", p)
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--table", action="store_true")
    ap.add_argument("--hold", action="store_true")
    ap.add_argument("--preview", action="store_true")
    ap.add_argument("--out", default=os.path.join(ROOT, "docs", "previews", "bows_bigger"))
    a = ap.parse_args(argv)
    if a.table:
        print_table()
    if a.hold:
        hold()
    if a.preview:
        preview(a.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
