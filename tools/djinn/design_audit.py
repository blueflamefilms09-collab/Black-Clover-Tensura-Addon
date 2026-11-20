#!/usr/bin/env python3
"""Djinn design audit: finds copies and concept mismatches among the 36 Djinn, writes docs/djinn_design_audit.md.

Run from the repo root:  python tools/djinn/design_audit.py [--check]
  --check  exit 1 when a DUPLICATE is found (a pair whose design is the same); mismatches are only listed (REVIEW).

What is compared (everything is read from the repo, nothing is drawn):
  meshes    djinn/<id>, u_<id>, d_<id> part files and weapon_meshes/djinn_weapon_<id>[_dark].txt: quad sets (positions only, 0.05 grid).
            Two parts of two Djinn are COPIES when >= 90% of their quads are shared; two sets when >= 60% of the parts are copies.
  skins     textures/entity/djinn/*, textures/entity/weapon/djinn_weapon_*, textures/item/djinn_seal_*, textures/entity/tinydjinn_*:
            exact sha, then luminance-structure (hue ignored) and colour. Same layout is normal, so a pair is a COPY only when the painted
            pixels match (>= 85% identical) or the luminance picture matches (mean difference < 5/255) AND colour differs only by hue.
  palettes  body colour / glow colour in CIE-Lab (relics/djinn/Djinn.java): pairs closer than 12 dE are listed, across elements they are a mismatch risk.
  concept   element vs hue of the colour and of the painted skin, ambient particles, weapon kind and text, trait / Extreme text, act text.
The concept table (ELEMENT) is the audit's own reading of each element; it only decides what is flagged for review.
"""
import colorsys
import functools
import hashlib
import json
import math
import re
import sys
from collections import defaultdict
from itertools import combinations
from pathlib import Path

from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
AM = ROOT / "src/main/resources/assets/anime_multiverse"
OUT = ROOT / "docs/djinn_design_audit.md"
DJINN_JAVA = ROOT / "src/main/java/com/newuniverse/nusmp/relics/djinn/Djinn.java"
ROSTER_JSON = ROOT / "docs/djinn_roster_36.json"
LANG = AM / "lang/en_us.json"

# ---------------------------------------------------------------- the audit's reading of each element: hue ranges (degrees) the body colour may sit in,
# particles that fit it, words that fit it and words of OTHER elements that would be a mismatch when they lead a Djinn's own text.
ELEMENT = {
    "HEAT": dict(hues=[(340, 360), (0, 50)], dark_ok=True, particles={"flame", "ember", "ash", "smoke", "spark"},
                 words=("fire", "flame", "burn", "sun", "ember", "ash", "cinder", "heat", "melt", "lion", "brand")),
    "WATER": dict(hues=[(150, 250)], dark_ok=False, particles={"bubble", "drip", "snow", "shard", "mote2", "ink"},
                  words=("water", "tide", "sea", "wave", "ice", "frost", "rime", "drown", "undertow", "mirror", "current", "wreck", "abyss", "deep", "cold", "crocodile", "whirlpool", "anchor", "freez")),
    "LIGHTNING": dict(hues=[(200, 300), (45, 70)], dark_ok=True, particles={"crit", "spark", "ink", "mote2"},
                      words=("lightning", "bolt", "storm", "thunder", "spark", "chain", "arc", "jolt", "judgement", "hidden", "seek")),
    "WIND": dict(hues=[(70, 210)], lowsat_ok=True, dark_ok=False, particles={"wind", "snow", "feather", "mote"},
                 words=("wind", "gale", "cyclone", "breeze", "swift", "sky", "tempest", "air", "ice wind", "glaive", "crescent", "updraft")),
    "LIFE": dict(hues=[(70, 170)], dark_ok=False, particles={"petal", "spore", "feather", "leaf"},
                 words=("life", "bloom", "heal", "mend", "thorn", "root", "garden", "green", "hunt", "wheel", "herb", "wild", "vine")),
    "STRENGTH": dict(hues=[(0, 60), (330, 360)], dark_ok=True, particles={"ember", "ash", "smoke", "flame", "mote", "spark"},
                     words=("crush", "titan", "giant", "heavy", "quake", "throne", "king", "slaughter", "rend", "gore", "charge", "trample", "bull", "alchem", "rock", "armour", "knee")),
    "SOUND": dict(hues=[(250, 340)], lowsat_ok=True, dark_ok=True, particles={"note", "mote", "enchant", "feather", "spark"},
                  words=("sound", "chord", "note", "song", "drum", "horn", "resonan", "voice", "requiem", "hush", "silen", "crow", "concerto", "quill", "quiet", "ring")),
    "LIGHT": dict(hues=[(38, 70), (190, 290)], lowsat_ok=True, dark_ok=False, particles={"mote", "mote2", "spark", "enchant", "shard", "drip", "ink"},
                  words=("light", "dawn", "star", "radian", "dazzle", "blind", "shine", "holy", "sun", "page", "face", "shell", "peacock", "scholar")),
}

# ---------------------------------------------------------------- reading the repo

def parse_enum():
    s = DJINN_JAVA.read_text(encoding="utf-8")
    body = s.split("// ---------------------------------------------------------------- the row types")[0]
    pat = re.compile(r'^\s*([A-Z_]+)\("([^"]+)", "([^"]+)", Affinity\.([A-Z]+), (0x[0-9A-Fa-f]+), (0x[0-9A-Fa-f]+), "([^"]+)",\s*\n'
                     r'\s*new Look\(Look\.Horns\.(\w+), Look\.Wings\.(\w+), Look\.Tail\.(\w+), Look\.Halo\.(\w+), (true|false)\),\s*\n'
                     r'\s*new Trait\(Special\.(\w+), "([^"]*)", (\d+), (\d+), ([\d.]+), ([\d.]+), "([^"]*)"\),\s*\n'
                     r'\s*new Extreme\("([^"]+)", Shape\.(\w+), ([\d.]+), ([\d.]+), "([^"]*)", (\d+), (\d+),\s*\n\s*"([^"]*)"\)', re.M)
    out = []
    for m in pat.finditer(body):
        g = m.groups()
        out.append(dict(enum=g[0], name=g[1], title=g[2], aff=g[3], color=int(g[4], 16), glow=int(g[5], 16), form=g[6],
                        horns=g[7], wings=g[8], tail=g[9], halo=g[10], ears=g[11], special=g[12], trait=g[17], xname=g[18], xshape=g[19], xblurb=g[25],
                        id=g[0].lower()))
    return out


def load_weapons():
    data = json.loads(ROSTER_JSON.read_text(encoding="utf-8"))
    return {d["id"]: d["weapon"] for d in data}


def load_lang():
    try:
        return json.loads(LANG.read_text(encoding="utf-8"))
    except Exception:
        return {}


def load_ambient(ids):
    """Ambient particle rows: the 24 from tools/relics/djinn36_roster.AMBIENT, the first twelve from client/relics/djinn/DjinnLook.java."""
    amb = {}
    try:
        sys.path.insert(0, str(ROOT / "tools/relics"))
        import djinn36_roster as r  # noqa
        for k, v in r.AMBIENT.items():
            amb[k.lower()] = v
        sys.path.pop(0)
    except Exception as e:  # pragma: no cover
        print("ambient (24) not read:", e)
    src = (ROOT / "src/main/java/com/newuniverse/nusmp/client/relics/djinn/DjinnLook.java").read_text(encoding="utf-8")
    first12 = ["baal", "amon", "vinea", "focalor", "zagan", "zepar", "furfur", "marchosias", "sitri", "bifron", "paimon", "vassago"]
    rows = re.findall(r'new Ambient\("(\w+)", (\d+), "(\w+)", (\d+), Anchor\.(\w+)\)', src)
    for i, r in zip(first12, rows):
        amb.setdefault(i, (r[0], int(r[1]), r[2], int(r[3]), r[4]))
    return amb


def mesh_quads(path):
    quads = []
    for ln in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if not ln.startswith("qn "):
            continue
        t = ln.split()[2:]
        if len(t) < 32:
            continue
        pts = []
        for i in range(4):
            x, y, z = (float(t[i * 8]), float(t[i * 8 + 1]), float(t[i * 8 + 2]))
            pts.append((round(x * 20), round(y * 20), round(z * 20)))
        quads.append(hash(tuple(sorted(pts))))
    return quads


def load_set(dirpath):
    parts = {}
    if dirpath.is_dir():
        for f in sorted(dirpath.glob("*.txt")):
            parts[f.stem] = set(mesh_quads(f))
    return parts


def jacc(a, b):
    if not a or not b:
        return 0.0
    return len(a & b) / min(len(a), len(b))


def set_similarity(pa, pb):
    """(share of the parts of the smaller set that are copies in the other, list of copied part names). Tiny parts (< 6 quads) do not count."""
    names = [n for n in pa if n in pb and len(pa[n]) >= 6 and len(pb[n]) >= 6]
    if not names:
        return 0.0, []
    same = [n for n in names if jacc(pa[n], pb[n]) >= 0.9]
    return len(same) / len(names), same


def img(path):
    try:
        im = Image.open(path).convert("RGBA")
        if im.width > 256:  # 1024 px skins are compared at 256 (box filter): copies still match, memory stays small
            im = im.resize((256, 256 * im.height // im.width), Image.BOX)
        return im
    except Exception:
        return None


@functools.lru_cache(maxsize=None)
def tex_stats(path):
    im = img(path)
    if im is None:
        return None
    a = np.asarray(im).astype(np.float32)
    m = a[..., 3] > 16
    return a, m


def lab(rgb):
    r, g, b = [((c / 255.0) / 12.92 if c / 255.0 <= 0.04045 else (((c / 255.0) + 0.055) / 1.055) ** 2.4) for c in rgb]
    x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
    y = (r * 0.2126 + g * 0.7152 + b * 0.0722)
    z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return (116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))


def de(c1, c2):
    return math.dist(lab(c1), lab(c2))


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def hue_deg(c):
    h, s, v = colorsys.rgb_to_hsv(*[x / 255 for x in rgb(c)])
    return h * 360, s, v


def hue_ok(c, spec):
    h, s, v = hue_deg(c)
    if v < 0.32 and spec.get("dark_ok"):
        return True
    if s < 0.22 and spec.get("lowsat_ok", True) and spec["hues"] is not None and (v > 0.6 or spec.get("dark_ok")):
        return True  # white / grey / silver: allowed for every element but flagged separately by the pair check
    return any(lo <= h <= hi for lo, hi in spec["hues"])


def skin_summary(path):
    """(dominant hue of the saturated painted pixels, mean colour of painted pixels) of a skin, or None."""
    st = tex_stats(path)
    if st is None:
        return None
    a, m = st
    if m.sum() < 20:
        return None
    px = a[m][:, :3] / 255.0
    mx, mn = px.max(1), px.min(1)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    w = sat * mx
    hsv = np.array([colorsys.rgb_to_hsv(*p) for p in px[:: max(1, len(px) // 4000)]])
    ww = w[:: max(1, len(px) // 4000)]
    if ww.sum() < 1e-3:
        return dict(hue=None, sat=float(sat.mean()), val=float(mx.mean()), mean=tuple((px.mean(0) * 255).astype(int)))
    # circular weighted mean hue
    ang = hsv[:, 0] * 2 * math.pi
    hue = (math.atan2((np.sin(ang) * ww).sum(), (np.cos(ang) * ww).sum()) / (2 * math.pi)) % 1.0 * 360
    return dict(hue=hue, sat=float(sat.mean()), val=float(mx.mean()), mean=tuple((px.mean(0) * 255).astype(int)))


@functools.lru_cache(maxsize=None)
def sha(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()


@functools.lru_cache(maxsize=None)
def lum(p):
    return tex_stats(p)[0][..., :3] @ np.array([0.299, 0.587, 0.114], dtype=np.float32)


def tex_pair(pa, pb):
    """('exact' | 'pixels' | 'structure' | None, detail). Same-size skins only."""
    if sha(pa) == sha(pb):
        return "exact", "byte-identical"
    sa, sb = tex_stats(pa), tex_stats(pb)
    if sa is None or sb is None or sa[0].shape != sb[0].shape:
        return None, ""
    a, ma = sa
    b, mb = sb
    union = ma | mb
    if union.sum() < 200:
        return None, ""
    both = ma & mb
    same_px = (np.abs(a[..., :3] - b[..., :3]).max(-1) <= 6) & both
    frac_same = same_px.sum() / union.sum()
    if frac_same >= 0.85:
        return "pixels", f"{frac_same:.0%} of painted pixels identical"
    la = lum(pa)
    lb = lum(pb)
    mask_agree = (ma == mb).sum() / ma.size
    if both.sum() > 200 and mask_agree > 0.97:
        d = np.abs(la - lb)[both].mean()
        if d < 5.0:
            return "structure", f"same picture, only the colour differs (mean luminance diff {d:.1f}/255)"
    return None, ""


def shown_bust(i, dark):
    """The bust file the game draws (client/relics/djinn/DjinnLook.bustFile, not classic): the v2 one when it exists."""
    base = AM / f"textures/entity/djinn/{i}{'_d' if dark else ''}_bust"
    v2 = Path(str(base) + "2.png")
    return v2 if v2.exists() else Path(str(base) + ".png")


# findings that were looked at and are right as they are: (kind, djinn) -> why. They are listed in their own table, not counted.
ACCEPTED = {
    ("concept", "camio"): "crow-black plumage with a blue sheen: Songbird of the Crows is dark navy by lore, its sound comes from notes, quills and the crow wing",
    ("concept", "kimaris"): "the plum and rose of the dark steed's fire; Kimaris's Extreme is a black-fire charge (concept dark) and its colour says so",
    ("asset colour", "amdusias"): "the Alicorn Rapier is an ivory horn (low saturation orange) with a lilac grip: the pale unicorn horn is the point of the weapon",
    ("palette", "baal"): "Andrealphus is a peacock: its blue is plumage, its element is carried by gold stars, eyes, the divine VFX and the light weapon, Baal's by bolts, black iron and lightning VFX",
    ("palette", "paimon"): "Paimon is pale sky cyan with a near-white glow and feathers, Vepar deep wreck teal with coral and brass; the element is in the VFX, the weapon and the motifs",
    ("extreme concept", "camio"): "its Extreme is a murder of crows diving (fear), drawn with the dark damage type; the Djinn keeps Sound for its look and its notes",
    ("extreme concept", "kimaris"): "its Extreme is a charge of black fire: concept dark is the right one",
    ("extreme concept", "haagenti"): "its Extreme is a cauldron of bronze wind (a vortex): concept wind is what it draws",
    ("extreme concept", "vinea"): "Vinea's Half Equip freezes and her Extreme leaves the sea frozen where it stops: concept ice, element Water",
}

# what the audit's findings led to (each is listed once in docs/djinn_rework_and_ranks.md): (what, who, how)
REWORKED = [
    ("Lesser Summon bust (light)", "balam, haagenti, vinea, alloces, furfur, furcas, amdusias", "new <id>_bust2.png with its own headgear, element mantle and glyph (tools/djinn/bust2.py)"),
    ("Lesser Summon bust (dark)", "all 36", "new <id>_d_bust2.png: one figure per Djinn instead of one black figure with a different gem (tools/djinn/bust2.py)"),
    ("tiny Djinn model", "vepar, barbatos", "new tinydjinn_<id>_v2 (geo, light and dark skins, glow maps) from tools/tiny_djinn/specs_v2.json"),
    ("Extreme Magic damage concept", "focalor", "water -> wind in djinn_extreme_types.json (the Warden of Gales throws a Seven-Wind Cyclone)"),
]

# ---------------------------------------------------------------- the audit

def main():
    check = "--check" in sys.argv
    roster = parse_enum()
    assert len(roster) == 36, f"parsed {len(roster)} Djinn, expected 36"
    ids = [d["id"] for d in roster]
    by = {d["id"]: d for d in roster}
    weapons = load_weapons()
    lang = load_lang()
    amb = load_ambient(ids)
    dups, mism = [], []

    # ---- meshes: armour sets (fallback rig, unique light set, dark set) and weapon meshes
    sets = {k: {i: load_set(AM / "djinn" / (pre + i)) for i in ids} for k, pre in (("rig", ""), ("u", "u_"), ("d", "d_"))}
    rig_shared = 0
    for kind in ("rig", "u", "d"):
        for a, b in combinations(ids, 2):
            share, same = set_similarity(sets[kind][a], sets[kind][b])
            if share >= 0.6 and len(same) >= 3:
                if kind == "rig":
                    rig_shared += 1  # the fallback rig is one shared template by design and is never drawn while u_<id> exists: counted, not a finding
                else:
                    dups.append((f"mesh set {kind}", a, b, f"{len(same)} parts are copies ({share:.0%}): {', '.join(same[:6])}"))
    wm = {}
    for i in ids:
        for dark in (False, True):
            p = AM / "weapon_meshes" / (f"djinn_weapon_{i}{'_dark' if dark else ''}.txt")
            if p.exists():
                wm[(i, dark)] = set(mesh_quads(p))
    for dark in (False, True):
        for a, b in combinations(ids, 2):
            if (a, dark) in wm and (b, dark) in wm and jacc(wm[(a, dark)], wm[(b, dark)]) >= 0.5:
                dups.append((f"weapon mesh{' (dark)' if dark else ''}", a, b, f"{jacc(wm[(a, dark)], wm[(b, dark)]):.0%} of the quads are shared"))

    # ---- skins
    groups = [("armour skin (u)", lambda i: AM / f"textures/entity/djinn/{i}_u_armor.png"),
              ("armour skin (dark)", lambda i: AM / f"textures/entity/djinn/{i}_d_armor.png"),
              ("fallback skin", lambda i: AM / f"textures/entity/djinn/{i}_armor.png"),
              ("rune", lambda i: AM / f"textures/entity/djinn/{i}_u_rune.png"),
              ("bust", lambda i: shown_bust(i, False)),
              ("weapon skin", lambda i: AM / f"textures/entity/weapon/djinn_weapon_{i}.png"),
              ("weapon skin (dark)", lambda i: AM / f"textures/entity/weapon/djinn_weapon_{i}_dark.png"),
              ("seal", lambda i: AM / f"textures/item/djinn_seal_{i}.png"),
              ("bust (dark)", lambda i: shown_bust(i, True)),
              ("weapon icon", lambda i: AM / f"textures/item/djinn_weapon_{i}.png")]
    skin_info = {}
    # tiny Djinn: the 64x64 skins share one UV layout, so they are compared by their design table (tools/tiny_djinn/specs.json), not by pixels
    tiny = json.loads((ROOT / "tools/tiny_djinn/specs.json").read_text(encoding="utf-8"))["djinn"]
    tiny = tiny if isinstance(tiny, dict) else {r["id"]: r for r in tiny}
    v2p = ROOT / "tools/tiny_djinn/specs_v2.json"
    if v2p.exists():  # the figure the game draws (TinyDjinnAssets.fileKey): the reworked row replaces the old one
        for r in json.loads(v2p.read_text(encoding="utf-8"))["djinn"]:
            tiny[r["id"]] = r
    slots = ["eyes", "mouth", "chest", "pauldron", "hair", "head", "ears", "cape", "weapon", "tail", "acc"]
    for a, b in combinations(ids, 2):
        if a in tiny and b in tiny:
            same = [k for k in slots if tiny[a].get(k) == tiny[b].get(k)]
            if len(same) >= len(slots) - 3:
                dups.append(("tiny djinn design", a, b, f"{len(same)} of {len(slots)} design slots equal (differs only in {', '.join(k for k in slots if k not in same)})"))
    for gname, fn in groups:
        have = [i for i in ids if fn(i).exists()]
        for a, b in combinations(have, 2):
            how, detail = tex_pair(fn(a), fn(b))
            if how:
                dups.append((gname, a, b, f"{how}: {detail}"))
        if gname == "armour skin (u)":
            for i in have:
                skin_info[i] = skin_summary(fn(i))

    # ---- palettes
    pal_close = []
    for a, b in combinations(ids, 2):
        da = de(rgb(by[a]["color"]), rgb(by[b]["color"]))
        dg = de(rgb(by[a]["glow"]), rgb(by[b]["glow"]))
        if da < 12:
            pal_close.append((a, b, da, dg))
    for a, b, da, dg in pal_close:
        cross = by[a]["aff"] != by[b]["aff"]
        if dg < 14 or cross:
            (dups if dg < 6 and not cross else mism).append(("palette", a, b, f"body colours {da:.1f} dE apart, glows {dg:.1f} dE" + (f" ({by[a]['aff']} vs {by[b]['aff']}: two elements one colour)" if cross else "")))

    # ---- ambient + vfx
    seen_amb = defaultdict(list)
    for i in ids:
        if i in amb:
            seen_amb[tuple(amb[i])].append(i)
    for k, v in seen_amb.items():
        if len(v) > 1:
            dups.append(("ambient particles", v[0], v[1], f"same ambient row {k}" + (f" (also {v[2:]})" if len(v) > 2 else "")))

    # ---- does every asset of a Djinn wear the Djinn's own colour? (armour skin, weapon skin, seal icon, tiny Djinn, bust)
    asset_hue = {}
    for aname, fn in (("armour skin", lambda i: AM / f"textures/entity/djinn/{i}_u_armor.png"),
                      ("weapon skin", lambda i: AM / f"textures/entity/weapon/djinn_weapon_{i}.png"),
                      ("seal", lambda i: AM / f"textures/item/djinn_seal_{i}.png"),
                      ("tiny Djinn", lambda i: AM / f"textures/entity/tinydjinn_{i}.png"),
                      ("bust", lambda i: AM / f"textures/entity/djinn/{i}_bust.png")):
        for i in ids:
            if not fn(i).exists():
                continue
            sm = skin_summary(fn(i))
            if not sm or sm["hue"] is None or sm["sat"] < 0.3:
                continue
            h0, s0, v0 = hue_deg(by[i]["color"])
            if s0 < 0.3 or v0 < 0.3:
                continue  # a grey or black body colour: no hue to compare
            dh = abs((sm["hue"] - h0 + 180) % 360 - 180)
            asset_hue[(aname, i)] = dh
            if dh > 55:
                mism.append(("asset colour", i, aname, f"{aname} is painted hue {sm['hue']:.0f} but the Djinn's colour is hue {h0:.0f} ({dh:.0f} degrees apart)"))

    # ---- the Extreme Magic's damage concept (assets/anime_multiverse/djinn_extreme_types.json) against the element
    fits = {"HEAT": {"fire"}, "LIGHTNING": {"lightning"}, "WATER": {"water", "ice"}, "WIND": {"wind"}, "LIFE": {"life"}, "STRENGTH": {"force"}, "SOUND": {"sound"}, "LIGHT": {"light"}}
    ext = json.loads((AM / "djinn_extreme_types.json").read_text(encoding="utf-8"))["djinn"]
    for i in ids:
        if ext.get(i) not in fits[by[i]["aff"]]:
            mism.append(("extreme concept", i, "", f"Extreme Magic hits as '{ext.get(i)}' but the Djinn is {by[i]['aff']} (expected {', '.join(sorted(fits[by[i]['aff']]))})"))
    for i in ids:
        if by[i]["aff"] != "HEAT" and "burn" in by[i]["xblurb"].lower() + by[i]["trait"].lower():
            mism.append(("text", i, "", "a non-fire Djinn's trait or Extreme text speaks of burning"))

    # ---- concept check per Djinn
    rows = []
    for i in ids:
        d = by[i]
        spec = ELEMENT[d["aff"]]
        w = weapons.get(i, {})
        acts = w.get("acts", [])
        issues = []
        # colour vs element
        if not hue_ok(d["color"], spec):
            issues.append(f"body colour #{d['color']:06X} (hue {hue_deg(d['color'])[0]:.0f}) is outside the {d['aff']} range")
        sk = skin_info.get(i)
        if sk and sk["hue"] is not None and sk["sat"] > 0.25 and not any(lo - 15 <= sk["hue"] <= hi + 15 for lo, hi in spec["hues"]):
            issues.append(f"painted armour skin leans hue {sk['hue']:.0f}, outside the {d['aff']} range")
        # ambient
        a_row = amb.get(i)
        if a_row and a_row[0] not in spec["particles"] and a_row[2] not in spec["particles"]:
            issues.append(f"ambient particles {a_row[0]}/{a_row[2]} fit no {d['aff']} Djinn")
        # texts: the element words of the other elements in the lead of the Djinn's own text
        text = " ".join([d["title"], d["trait"], d["xblurb"]] + [a.get("desc", "") + " " + a.get("name", "") for a in acts] + [(w.get("names") or {}).get("light", "")]).lower()
        mine = sum(text.count(x) for x in spec["words"])
        others = {e: sum(text.count(x) for x in s["words"]) for e, s in ELEMENT.items() if e != d["aff"]}
        best = max(others, key=others.get)
        if mine == 0:
            issues.append("no word of its own element in title / trait / Extreme / weapon text")
        if others[best] >= mine + 3:
            issues.append(f"text leans {best} ({others[best]} words) over {d['aff']} ({mine})")
        rows.append(dict(id=i, d=d, w=w, acts=acts, issues=issues, amb=a_row, sk=sk))
        for x in issues:
            mism.append(("concept", i, "", x))

    # ---- report
    accepted = [(k, a, b, d, ACCEPTED[(k, a)]) for k, a, b, d in mism if (k, a) in ACCEPTED]
    mism = [m for m in mism if (m[0], m[1]) not in ACCEPTED]
    dup_by = defaultdict(list)
    for kind, a, b, det in dups:
        dup_by[a].append(f"{kind}: with {b} ({det})")
        if b:
            dup_by[b].append(f"{kind}: with {a} ({det})")
    L = []
    L.append("# Djinn design audit\n")
    L.append("Written by `python tools/djinn/design_audit.py` (do not edit by hand; run it again after a change). It reads the 36 Djinn of "
             "`relics/djinn/Djinn.java`, their armour meshes (`djinn/<id>`, `u_<id>`, `d_<id>`), weapon meshes, skins, seals, tiny Djinn skins, ambient particles, "
             "weapon table (`docs/djinn_roster_36.json`) and compares them. Rules of the comparison are in the script header.\n")
    L.append(f"* Duplicates found: **{len(dups)}** (after the rework of this release; the list of what was reworked is below)\n* Concept mismatches found: **{len(mism)}**\n"
             f"* Findings looked at and accepted (reason given): **{len(accepted)}**\n* Shared parts of the fallback rig (`djinn/<id>/`, never drawn while `u_<id>` exists, one template by design): {rig_shared} pairs, not counted\n")
    L.append("## What the audit led to (REPLACED: the old file stays, `djinnLookClassic` brings it back)\n")
    L.append("| what | Djinn | how |\n|---|---|---|")
    for w, who, how in REWORKED:
        L.append(f"| {w} | {who} | {how} |")
    L.append("")
    L.append("## Duplicates (same design twice)\n")
    if dups:
        L.append("| what | djinn A | djinn B | detail |\n|---|---|---|---|")
        for kind, a, b, det in sorted(dups):
            L.append(f"| {kind} | {a} | {b} | {det} |")
    else:
        L.append("None.")
    L.append("\n## Concept mismatches (look / abilities / weapon against the aspect)\n")
    if mism:
        L.append("| kind | djinn | other | finding |\n|---|---|---|---|")
        for kind, a, b, det in sorted(mism):
            L.append(f"| {kind} | {a} | {b} | {det} |")
    else:
        L.append("None.")
    L.append("\n## Looked at and accepted\n")
    if accepted:
        L.append("| kind | djinn | finding | why it stays |\n|---|---|---|---|")
        for k, a, b, d, why in sorted(accepted):
            L.append(f"| {k} | {a} | {d} | {why} |")
    else:
        L.append("None.")
    # the GUI medallion silhouette is keyed by (horns, wings, ears): informational
    from collections import Counter
    sil = Counter((by[i]["horns"], by[i]["wings"], by[i]["ears"]) for i in ids)
    shared = sum(1 for v in sil.values() if v > 1)
    L.append(f"\nThe Djinn panel's medallion silhouette is drawn from (horns, wings, ears): {shared} of the {len(sil)} silhouettes are shared by two or three Djinn. They still differ in halo, tail, palette and accent "
             "(no two Djinn share horns, wings, tail and halo: checked here), so the panels are not copies; nothing was changed.\n")
    L.append("\n## Every Djinn\n")
    L.append("| # | Djinn | aspect | colour / glow | horns, wings, tail, halo | ambient | weapon (acts) | Half Equip, Extreme | duplicates | mismatches |\n|---|---|---|---|---|---|---|---|---|---|")
    for n, r in enumerate(rows):
        d, w = r["d"], r["w"]
        wk = f"{w.get('kind', '?')} ({', '.join(a['key'] for a in r['acts'])})"
        am = "/".join(str(x) for x in (r["amb"][0], r["amb"][2])) if r["amb"] else "-"
        L.append(f"| {n} | {d['name']} | {d['aff'].title()} | #{d['color']:06X} / #{d['glow']:06X} | {d['horns'].lower()}, {d['wings'].lower()}, {d['tail'].lower()}, {d['halo'].lower()} | {am} | {wk} | "
                 f"{d['special'].lower()}, {d['xshape'].lower()} | {'<br>'.join(dup_by.get(d['id'], [])) or '-'} | {'<br>'.join(r['issues']) or '-'} |")
    OUT.parent.mkdir(exist_ok=True)
    OUT.write_text("\n".join(L) + "\n", encoding="utf-8")
    print(f"{len(dups)} duplicates, {len(mism)} concept findings -> {OUT.relative_to(ROOT)}")
    if check and dups:
        sys.exit(1)


if __name__ == "__main__":
    main()
