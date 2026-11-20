"""Sameness report: how alike are the Blender-made hero weapons / bows, and how helper-pattern-heavy is each one?

Read-only. Reads the shipped assets (weapon_meshes/<id>.txt + textures/entity/weapon/<id>[_glow].png) and the Blender builders' source.

    python tools/heroweapon/style/sameness_report.py                       # table of all Blender-made weapons, worst first
    python tools/heroweapon/style/sameness_report.py --top 25 --md out.md  # also write a markdown report
    python tools/heroweapon/style/sameness_report.py --sheet sheet.png     # contact sheet of the 10 worst (uses tools/relics/preview_mesh.py)
    python tools/heroweapon/style/sameness_report.py --selftest

Per weapon it measures (all from the files, nothing from a game):
  sil_iou   best silhouette IoU (profile mask, same canvas) against another weapon of its family (swords vs swords ...)
  pal_dist  smallest palette distance (total variation of a 4x4x4 RGB histogram, area weighted) to ANY other weapon; low = same recipe
  gold/navy/gem  share of surface area that is gold / navy / saturated-bright "gem" colour (the navy+gold+glowing-gem recipe)
  pattern   share of quads in helper-pattern parts (bands/rings, gems, studs/spikes, wrapped grip, stars, wing blades, rune inlays)
  repeat    share of quads in rows of >=3 identical islands (same part, same size) and how evenly they are spaced (gap CV)
  mirror    best mirror-symmetry score of the vertices (x or y); 1.0 = perfectly symmetric, no asymmetry
  glow      share of the surface that also carries a glow-layer copy
  hf        high-frequency skin noise (mean |pixel - blur| on used texels), a proxy for 'noise instead of painted breaks'
The composite 'sameness' (0..100) is a weighted mix of rank-normalised parts; higher = more generic. Weights are in WEIGHTS.
"""
import argparse
import colorsys
import glob
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
BLENDER_DIR = os.path.join(ROOT, "tools", "blender")

WEIGHTS = {"sil": 0.20, "pal": 0.20, "pattern": 0.25, "repeat": 0.15, "glow": 0.08, "hf": 0.05, "mirror": 0.07}

# part-name classes = the helper patterns (names come from the builders' part= arguments, written into the mesh as "# part <name> <start> <count>")
CLASSES = {
    "band": re.compile(r"band|ring|collar|sleeve|cuff|hoop|wheel"),
    "gem": re.compile(r"jewel|gem|crystal|stone|orb|cabochon"),
    "stud": re.compile(r"stud|rivet|spike|bolt|thorn|nail|tine|claw|horn|barb"),
    "grip": re.compile(r"bind|wrap|cord|hand_|lace"),
    "star": re.compile(r"star|constell|sparkle"),
    "wing": re.compile(r"wing|feather|pinion|fin|limb_blade|leaf"),
    "rune": re.compile(r"rune|glyph|inlay|text|script"),
}


# ------------------------------------------------------------------ loading
def read_mesh(path):
    """-> dict(layers, P (n,4,3), UV (n,4,2), parts [(name, start, count)], header)"""
    layers, P, UV, parts, header = [], [], [], [], ""
    for line in open(path):
        if line.startswith("# part "):
            _, _, name, s, c = line.split()[:5]
            parts.append((name, int(s), int(c)))
            continue
        if line.startswith("# made") or line.startswith("# generated"):
            header = line.strip()
        if not line.startswith("qn "):
            continue
        v = line.split()
        layers.append(int(v[1]))
        p, uv = [], []
        for k in range(4):
            o = 2 + k * 8
            p.append((float(v[o]), float(v[o + 1]), float(v[o + 2])))
            uv.append((float(v[o + 3]), float(v[o + 4])))
        P.append(p)
        UV.append(uv)
    return {"layers": np.array(layers, int), "P": np.array(P, float).reshape(-1, 4, 3), "UV": np.array(UV, float).reshape(-1, 4, 2),
            "parts": parts, "header": header}


def is_blender(path):
    with open(path) as f:
        return "made in Blender" in f.readline()


def family(item_id):
    base = item_id.split("__")[0]
    return re.sub(r"_curse$", "", base)


def quad_area(P):
    a = np.cross(P[:, 1] - P[:, 0], P[:, 2] - P[:, 0])
    b = np.cross(P[:, 2] - P[:, 0], P[:, 3] - P[:, 0])
    return 0.5 * (np.linalg.norm(a, axis=1) + np.linalg.norm(b, axis=1))


# ------------------------------------------------------------------ measures
def silhouette(P, layers):
    """binary profile mask (x across, y along the weapon), scaled to fit a fixed canvas, centred on x, bottom-aligned."""
    pts = P[layers == 0] if (layers == 0).any() else P
    lo, hi = pts.reshape(-1, 3).min(0), pts.reshape(-1, 3).max(0)
    w, h = max(hi[0] - lo[0], 1e-6), max(hi[1] - lo[1], 1e-6)
    tall = h >= 1.5 * w
    cw, ch = (128, 512) if tall else (256, 256)
    s = min((cw - 4) / w, (ch - 4) / h)
    im = Image.new("1", (cw, ch), 0)
    d = ImageDraw.Draw(im)
    cx = (lo[0] + hi[0]) / 2
    for q in pts:
        d.polygon([(cw / 2 + (p[0] - cx) * s, ch - 2 - (p[1] - lo[1]) * s) for p in q], fill=1)
    return np.asarray(im, bool), tall


def iou(a, b):
    u = (a | b).sum()
    return float((a & b).sum() / u) if u else 0.0


def busy(mask):
    """edge pixels per sqrt(area): high = spiky / micro-detailed outline, low = a few big shapes."""
    im = Image.fromarray((mask * 255).astype(np.uint8))
    er = np.asarray(im.filter(ImageFilter.MinFilter(3))) > 0
    edge = (mask & ~er).sum()
    return float(edge / math.sqrt(max(mask.sum(), 1)))


def skin_stats(m, item_id):
    """area-weighted palette hist, gold/navy/gem shares, hue count and skin noise (hf)."""
    path = os.path.join(SKIN_DIR, item_id + ".png")
    if not os.path.exists(path):
        return None
    im = Image.open(path).convert("RGBA")
    sk = np.asarray(im, np.uint8)
    H, W = sk.shape[:2]
    l0 = m["layers"] == 0
    P, UV = m["P"][l0], m["UV"][l0]
    area = quad_area(P)
    c = UV.mean(1) * (W / 128.0)
    u = np.clip(c[:, 0].astype(int), 0, W - 1)
    v = np.clip(c[:, 1].astype(int), 0, H - 1)
    rgba = sk[v, u]
    keep = rgba[:, 3] > 128
    rgb = rgba[keep, :3].astype(float) / 255.0
    wt = area[keep]
    if wt.sum() <= 0:
        return None
    wt = wt / wt.sum()
    q = np.minimum((rgb * 4).astype(int), 3)
    idx = q[:, 0] * 16 + q[:, 1] * 4 + q[:, 2]
    hist = np.bincount(idx, weights=wt, minlength=64)
    hsv = np.array([colorsys.rgb_to_hsv(*c3) for c3 in rgb])
    h_, s_, v_ = hsv[:, 0] * 360, hsv[:, 1], hsv[:, 2]
    gold = (h_ >= 30) & (h_ <= 62) & (s_ > 0.35) & (v_ > 0.35)
    navy = (h_ >= 190) & (h_ <= 270) & (v_ < 0.55) & (s_ > 0.2) & (v_ > 0.12)
    gem = (s_ > 0.6) & (v_ > 0.75) & ~gold
    sat = s_ > 0.25
    hue_bins = np.bincount((h_[sat] // 30).astype(int) % 12, weights=wt[sat], minlength=12) if sat.any() else np.zeros(12)
    # skin noise on texels the mesh actually uses
    used = Image.new("1", (W, H), 0)
    d = ImageDraw.Draw(used)
    for q4 in UV[:: max(1, len(UV) // 1500)]:
        d.polygon([(p[0] * W / 128.0, p[1] * H / 128.0) for p in q4], fill=1)
    um = np.asarray(used, bool) & (sk[..., 3] > 128)
    g = Image.fromarray(sk[..., :3]).convert("L")
    gb = g.filter(ImageFilter.GaussianBlur(2))
    diff = np.abs(np.asarray(g, float) - np.asarray(gb, float))
    hf = float(diff[um].mean()) if um.any() else 0.0
    return {"hist": hist, "gold": float(wt[gold].sum()), "navy": float(wt[navy].sum()), "gem": float(wt[gem].sum()),
            "hues": int((hue_bins > 0.05).sum()), "hf": hf}


def pattern_shares(m):
    """quad share per helper-pattern class, from the part names."""
    n = len(m["layers"])
    out = {k: 0 for k in CLASSES}
    for name, s, c in m["parts"]:
        for k, rx in CLASSES.items():
            if rx.search(name.lower()):
                out[k] += c
                break
    tot = max(n, 1)
    return {k: v / tot for k, v in out.items()}


def components(m):
    """connected islands per part (quads sharing a vertex position) -> list of (part, centre, dims, nquads)"""
    res = []
    P = m["P"]
    for name, s, c in m["parts"]:
        if c <= 0:
            continue
        sub = P[s:s + c]
        parent = list(range(c))

        def find(a):
            while parent[a] != a:
                parent[a] = parent[parent[a]]
                a = parent[a]
            return a
        seen = {}
        for i in range(c):
            for p in sub[i]:
                key = (round(p[0] * 20), round(p[1] * 20), round(p[2] * 20))
                if key in seen:
                    ra, rb = find(seen[key]), find(i)
                    if ra != rb:
                        parent[ra] = rb
                else:
                    seen[key] = i
        groups = {}
        for i in range(c):
            groups.setdefault(find(i), []).append(i)
        for g in groups.values():
            pts = sub[g].reshape(-1, 3)
            lo, hi = pts.min(0), pts.max(0)
            res.append((name, (lo + hi) / 2, np.sort(hi - lo), len(g)))
    return res


def repeat_rows(m):
    """-> (quad share in rows of >=3 identical islands, mean gap CV of those rows (None if none), number of rows)"""
    comps = components(m)
    total = max(len(m["layers"]), 1)
    byp = {}
    for name, ctr, dims, nq in comps:
        byp.setdefault(name, []).append((ctr, dims, nq))
    rep, cvs, rows = 0, [], 0
    for name, lst in byp.items():
        buckets = {}
        for ctr, dims, nq in lst:
            key = (nq // 2, tuple(np.round(dims / max(dims.max(), 1e-6) * 5).astype(int)))   # same size class, same proportions
            buckets.setdefault(key, []).append((ctr, nq))
        for key, b in buckets.items():
            if len(b) < 3 or key[0] == 0:
                continue
            rep += sum(nq for _, nq in b)
            rows += 1
            cs = np.array([c for c, _ in b])
            ax = np.argmax(cs.max(0) - cs.min(0))
            mid = (m["P"].reshape(-1, 3)[:, ax].min() + m["P"].reshape(-1, 3)[:, ax].max()) / 2
            pos = np.sort(np.abs(cs[:, ax] - mid))                       # fold about the middle: both limbs / both faces of a bow count once
            gaps = np.diff(pos)
            gaps = gaps[gaps > 0.5]
            if len(gaps) >= 2:
                cvs.append(float(gaps.std() / gaps.mean()))
    return rep / total, (float(np.mean(cvs)) if cvs else None), rows


def mirror_score(P, layers):
    pts = (P[layers == 0] if (layers == 0).any() else P).reshape(-1, 3)
    if len(pts) > 6000:
        pts = pts[:: len(pts) // 6000 + 1]
    best = 0.0
    for ax in (0, 1):
        c = (pts[:, ax].min() + pts[:, ax].max()) / 2
        q = np.round(pts / 0.3).astype(int)
        keys = set(map(tuple, q))
        mq = q.copy()
        mq[:, ax] = np.round((2 * c - pts[:, ax]) / 0.3).astype(int)
        hit = 0
        for r in mq:
            t = tuple(r)
            if t in keys or (t[0] + 1, t[1], t[2]) in keys or (t[0] - 1, t[1], t[2]) in keys or (t[0], t[1] + 1, t[2]) in keys or (t[0], t[1] - 1, t[2]) in keys:
                hit += 1
        best = max(best, hit / len(mq))
    return best


def glow_share(m):
    a = quad_area(m["P"])
    l0 = a[m["layers"] == 0].sum()
    return float(a[m["layers"] == 2].sum() / l0) if l0 else 0.0


# ------------------------------------------------------------------ the run
def analyse(ids):
    W = []
    for i in ids:
        path = os.path.join(MESH_DIR, i + ".txt")
        m = read_mesh(path)
        if len(m["layers"]) == 0:
            continue
        sk = skin_stats(m, i)
        if sk is None:
            continue
        mask, tall = silhouette(m["P"], m["layers"])
        sh = pattern_shares(m)
        rep, cv, rows = repeat_rows(m)
        W.append({"id": i, "family": family(i), "quads": len(m["layers"]), "mask": mask, "tall": tall, "busy": busy(mask), "sk": sk, "shares": sh,
                  "pattern": sum(sh.values()), "classes": sum(1 for v in sh.values() if v > 0.01), "repeat": rep, "cv": cv, "rows": rows,
                  "mirror": mirror_score(m["P"], m["layers"]), "glow": glow_share(m), "parts": len(m["parts"])})
    return W


def ranknorm(x):
    x = np.asarray(x, float)
    order = x.argsort().argsort()
    return order / max(len(x) - 1, 1)


def score(W):
    n = len(W)
    for w in W:
        sims = [iou(w["mask"], o["mask"]) for o in W if o is not w and o["family"] == w["family"] and o["tall"] == w["tall"]]
        w["sil_iou"] = max(sims) if sims else 0.0
        w["sil_nn"] = max(((iou(w["mask"], o["mask"]), o["id"]) for o in W if o is not w and o["family"] == w["family"] and o["tall"] == w["tall"]), default=(0.0, "-"))[1]
        d = [(0.5 * np.abs(w["sk"]["hist"] - o["sk"]["hist"]).sum(), o["id"]) for o in W if o is not w]
        w["pal_dist"], w["pal_nn"] = min(d) if d else (1.0, "-")
        w["recipe"] = w["sk"]["gold"] > 0.12 and w["sk"]["navy"] > 0.06
    sil = np.array([w["sil_iou"] for w in W])
    pal = 1 - np.array([w["pal_dist"] for w in W])
    pat = ranknorm([w["pattern"] + 0.1 * w["classes"] for w in W])
    rep = ranknorm([w["repeat"] * (1.0 - min(w["cv"] if w["cv"] is not None else 0.6, 0.6)) for w in W])
    glow = ranknorm([w["glow"] for w in W])
    hf = ranknorm([w["sk"]["hf"] for w in W])
    mir = np.array([w["mirror"] for w in W])
    palr = ranknorm(pal)
    for k, w in enumerate(W):
        w["sameness"] = 100 * (WEIGHTS["sil"] * sil[k] + WEIGHTS["pal"] * palr[k] + WEIGHTS["pattern"] * pat[k] + WEIGHTS["repeat"] * rep[k]
                               + WEIGHTS["glow"] * glow[k] + WEIGHTS["hf"] * hf[k] + WEIGHTS["mirror"] * mir[k])
    return sorted(W, key=lambda w: -w["sameness"])


# ------------------------------------------------------------------ builder source counts
HELPERS = {
    "band(": r"\bband\(", "jewel(": r"\bjewel\(", "hand_binding(": r"\bhand_binding\(", "star_plate(": r"\bstar_plate\(", "noisy(": r"\bnoisy\(",
    "gem(/gem_setting(": r"\bgem(?:_setting)?\(", "spike(": r"\bspike\(", "rivets(": r"\brivets?\(", "grip_wrapped(/helix_wrap(": r"\b(?:grip_wrapped|helix_wrap)\(",
    "wing/feather fn(": r"\b(?:wings?|fan_feathers|feather|fan_blade)\(", "text(/runes(": r"\b(?:text|runes)\(", "stud/spike loops": r"for\s+\w+(?:,\s*\w+)*\s+in\s+[^\n:]*:\s*\n\s*(?:spike|stud|rivet|ob\w*\s*=\s*spike)",
}


def source_counts():
    rows = {}
    for f in sorted(glob.glob(os.path.join(BLENDER_DIR, "*.py"))):
        txt = open(f, encoding="utf-8", errors="replace").read()
        # drop def lines so a definition does not count as a use
        body = re.sub(r"^\s*def .*$", "", txt, flags=re.M)
        for k, rx in HELPERS.items():
            n = len(re.findall(rx, body))
            if n:
                rows.setdefault(k, {})[os.path.basename(f)] = n
    return rows


# ------------------------------------------------------------------ output
def fmt(W, top):
    L = []
    L.append("| # | id | sameness | sil IoU (nearest) | pal dist (nearest) | gold/navy/gem | pattern | repeat (CV) | mirror | glow | hf | parts |")
    L.append("|---|----|---------:|------------------|------------------|--------------|--------:|-------------|-------:|-----:|---:|------:|")
    for k, w in enumerate(W[:top], 1):
        cv = "-" if w["cv"] is None else "%.2f" % w["cv"]
        L.append("| %d | %s | %.1f | %.2f (%s) | %.2f (%s) | %.2f/%.2f/%.2f | %.2f | %.2f (%s, %d rows) | %.2f | %.2f | %.1f | %d |" % (
            k, w["id"], w["sameness"], w["sil_iou"], w["sil_nn"], w["pal_dist"], w["pal_nn"], w["sk"]["gold"], w["sk"]["navy"], w["sk"]["gem"],
            w["pattern"], w["repeat"], cv, w["rows"], w["mirror"], w["glow"], w["sk"]["hf"], w["parts"]))
    return "\n".join(L)


def summary(W):
    n = len(W)
    L = ["Weapons measured: %d (Blender-made, pull stages skipped)" % n]
    L.append("  gold-dominant (gold share > 0.12): %d   navy+gold recipe: %d   mean gold/navy/gem: %.2f / %.2f / %.2f" % (
        sum(w["sk"]["gold"] > 0.12 for w in W), sum(w["recipe"] for w in W), np.mean([w["sk"]["gold"] for w in W]), np.mean([w["sk"]["navy"] for w in W]),
        np.mean([w["sk"]["gem"] for w in W])))
    for k in CLASSES:
        c = sum(w["shares"][k] > 0.01 for w in W)
        L.append("  part class %-5s present in %3d weapons (%.0f%%), mean quad share %.3f" % (k, c, 100 * c / n, np.mean([w["shares"][k] for w in W])))
    ev = [w for w in W if w["cv"] is not None and w["cv"] < 0.2]
    L.append("  weapons with an evenly spaced identical-island row (gap CV < 0.20): %d" % len(ev))
    L.append("  weapons with a row of >=3 identical islands: %d" % sum(w["rows"] > 0 for w in W))
    L.append("  perfectly mirrored (mirror >= 0.90): %d" % sum(w["mirror"] >= 0.9 for w in W))
    L.append("  glow share > 0.25 of the surface: %d   mean glow %.2f" % (sum(w["glow"] > 0.25 for w in W), np.mean([w["glow"] for w in W])))
    fams = {}
    for w in W:
        fams.setdefault(w["family"], []).append(w)
    L.append("  family silhouette sameness (mean best IoU / mean palette NN distance):")
    for f, ws in sorted(fams.items(), key=lambda kv: -np.mean([x["sil_iou"] for x in kv[1]])):
        if len(ws) > 1:
            L.append("    %-22s n=%2d  IoU %.2f  pal-nn %.2f  gold %.2f" % (f, len(ws), np.mean([x["sil_iou"] for x in ws]), np.mean([x["pal_dist"] for x in ws]),
                                                                       np.mean([x["sk"]["gold"] for x in ws])))
    return "\n".join(L)


def pairs(W, k=12):
    out = []
    for a in range(len(W)):
        for b in range(a + 1, len(W)):
            x, y = W[a], W[b]
            if x["family"] != y["family"] or x["tall"] != y["tall"]:
                continue
            i = iou(x["mask"], y["mask"])
            d = 0.5 * np.abs(x["sk"]["hist"] - y["sk"]["hist"]).sum()
            out.append((i - d, i, d, x["id"], y["id"]))
    out.sort(reverse=True)
    return out[:k]


def sheet(W, out, n=10):
    sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
    import preview_mesh as pm
    tiles = []
    for k, w in enumerate(W[:n], 1):
        tmp = os.path.join(os.path.dirname(os.path.abspath(out)), ".tile_%d.png" % k)
        pm.render_preview(w["id"], out_png=tmp, views=("profile", "three_quarter"), panel=(230, 460) if w["tall"] else (330, 330), label=False)
        t = Image.open(tmp).convert("RGB")
        os.remove(tmp)
        flags = [f for f, ok in (("gold+navy", w["recipe"]), ("bands/rings", w["shares"]["band"] > 0.03), ("gem", w["shares"]["gem"] > 0.03),
                                 ("studs", w["shares"]["stud"] > 0.03), ("wrap grip", w["shares"]["grip"] > 0.03), ("stars", w["shares"]["star"] > 0.03),
                                 ("wing blades", w["shares"]["wing"] > 0.05), ("runes", w["shares"]["rune"] > 0.03),
                                 ("even rows", w["cv"] is not None and w["cv"] < 0.2), ("glow", w["glow"] > 0.25)) if ok]
        tiles.append((k, w, t, flags))
    TW, TH = 5, 2
    cw = max(t.width for _, _, t, _ in tiles) + 8
    chh = max(t.height for _, _, t, _ in tiles) + 62
    S = Image.new("RGB", (cw * TW, chh * TH), (18, 18, 26))
    d = ImageDraw.Draw(S)
    for idx, (k, w, t, flags) in enumerate(tiles):
        x, y = (idx % TW) * cw, (idx // TW) * chh
        S.paste(t, (x + 4, y + 58))
        d.text((x + 8, y + 6), "#%d  %s" % (k, w["id"]), fill=(255, 230, 140))
        d.text((x + 8, y + 22), "sameness %.1f | sil %.2f | pal %.2f | pattern %.2f" % (w["sameness"], w["sil_iou"], w["pal_dist"], w["pattern"]), fill=(210, 210, 220))
        d.text((x + 8, y + 38), ", ".join(flags)[:60], fill=(150, 200, 255))
    S.save(out)
    return out


def selftest():
    a = np.zeros((10, 10), bool)
    a[2:6, 2:6] = True
    b = a.copy()
    assert iou(a, b) == 1.0 and iou(a, np.zeros_like(a)) == 0.0
    assert family("legendary_bow__sagittarius_pull3") == "legendary_bow" and family("legendary_sword_curse") == "legendary_sword"
    r = ranknorm([3, 1, 2])
    assert list(r) == [1.0, 0.0, 0.5]
    print("selftest ok")


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--md")
    ap.add_argument("--sheet")
    ap.add_argument("--all", action="store_true", help="include every mesh, not only the Blender-made ones")
    ap.add_argument("--only", default=r"^(legendary|vassal)_", help="regex: only these ids are LISTED / ranked on the sheet (all are still measured, so palette distances see every weapon)")
    ap.add_argument("--exclude", default=r"_curse$", help="regex: ids dropped from the listing (curse looks share their base weapon's builder)")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args(argv)
    if a.selftest:
        selftest()
        return 0
    ids = []
    for p in sorted(glob.glob(os.path.join(MESH_DIR, "*.txt"))):
        i = os.path.basename(p)[:-4]
        if re.search(r"_pull\d$", i):
            continue
        if a.all or is_blender(p):
            ids.append(i)
    W = score(analyse(ids))
    V = [w for w in W if re.search(a.only, w["id"]) and not (a.exclude and re.search(a.exclude, w["id"]))]
    out = ["# Sameness report", "", "```", summary(W), "```", "", "## Ranked (most generic first; listing filter --only %s --exclude %s)" % (a.only, a.exclude), "", fmt(V, a.top), "", "## Most alike pairs inside a family (silhouette IoU - palette distance)", ""]
    for _, i, d, x, y in pairs(V):
        out.append("- %s <-> %s : sil IoU %.2f, palette dist %.2f" % (x, y, i, d))
    out += ["", "## Helper call counts in tools/blender/*.py (definitions excluded)", ""]
    for k, files in source_counts().items():
        out.append("- `%s` total %d : %s" % (k, sum(files.values()), ", ".join("%s %d" % kv for kv in sorted(files.items(), key=lambda kv: -kv[1]))))
    txt = "\n".join(out)
    print(txt)
    if a.md:
        open(a.md, "w").write(txt + "\n")
    if a.sheet:
        print("sheet:", sheet(V, a.sheet))
    return 0


if __name__ == "__main__":
    sys.exit(main())
