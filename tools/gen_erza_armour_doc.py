"""Writes the numbers of the Erza armour remake into the docs (1.99.0).

    python3 -B tools/gen_erza_armour_doc.py

  * docs/erza_requip.md: the block "## Pieces: anchor part, delay, fly-in side" is rewritten from requip/ArmourSets.java (the pieces, anchors, delays, fly-in sides and flags of every armour);
  * docs/erza_armour_remake.md: the measured tables (the Djinn bar, the 1.96.0 gap from docs/erza_armour_before_1_96.json, the new sets) between the markers <!-- numbers --> ... <!-- /numbers -->.
"""
import json
import os
import re
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
import erza_armour_audit as AU  # noqa: E402

JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "requip", "ArmourSets.java")
ARMOUR = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "requip", "Armour.java")
REQUIP_MD = os.path.join(ROOT, "docs", "erza_requip.md")
REMAKE_MD = os.path.join(ROOT, "docs", "erza_armour_remake.md")
WEIGHT = {"belt": 1, "gorget": 1, "ribbon": 1, "gauntlet": 2, "vambrace": 2, "sabaton": 2, "greave": 3, "cuisse": 3, "skirt": 3, "helm": 3, "pauldron": 4, "chest": 5, "backplate": 5, "cape": 5, "tail": 5,
          "wing": 6, "halo": 6, "backdrop": 7}


def parse_pieces():
    src = open(JAVA, encoding="utf-8").read()
    fac = {}
    for m in re.finditer(r"static Piece (\w+)\(\) \{ return p\(\"(\w+)\", Anchor\.(\w+), ([\d.\-]+), ([\-\d.]+), ([\-\d.]+), ([\-\d.]+)\)([^;]*);", src):
        name, pname, anchor, delay, dx, dy, dz, rest = m.groups()
        flags = []
        if "withGlass" in rest:
            flags.append("translucent")
        if "withDetail" in rest:
            flags.append("detail (24 blocks)")
        if "withHead" in rest:
            flags.append("hidden in first person")
        if "withClip" in rest:
            flags.append("animated")
        if "withFlap" in rest:
            flags.append("flaps faster in the air")
        fac[name] = dict(name=pname, anchor=anchor.lower(), delay=float(delay), d=(float(dx), float(dy), float(dz)), flags=flags)
    base = re.search(r"List<Piece> l = new ArrayList<>\(List\.of\((.*?)\)\);", src, re.S).group(1)
    base_names = re.findall(r"(\w+)\(\)", base)
    sets = {}
    for m in re.finditer(r"SETS\.put\(Armour\.(\w+), full\((.*?)\)\);", src):
        extras = []
        for call in re.findall(r"(\w+)\(\)(\.withGlass\(\))?", m.group(2)):
            extras.append((call[0], bool(call[1])))
        sets[m.group(1)] = extras
    return fac, base_names, sets


def armours():
    src = open(ARMOUR, encoding="utf-8").read()
    out = []
    for m in re.finditer(r"^    ([A-Z_]+)\(\"(\w+)\", \"([^\"]+)\"", src, re.M):
        out.append((m.group(1), m.group(2), m.group(3)))
    return out


def side(d):
    x, y, z = d
    m = max(abs(x), abs(y), abs(z))
    if abs(x) == m:
        w = "right" if x < 0 else "left"
    elif abs(y) == m:
        w = "above" if y < 0 else "below"
    else:
        w = "behind" if z > 0 else "front"
    return w + " (%g,%g,%g)" % (x, y, z)


def piece_block():
    fac, base, sets = parse_pieces()
    L = ["## Pieces: anchor part, delay, fly-in side", "",
         "The direction is the side the piece flies in from, in model pixels in the anchor part's frame (the Djinn's own numbers are about 2 to 14); y is down in that frame, so \"above\" is the head's own. The table is",
         "`requip/ArmourSets.java`; the models are `assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json` with a texture, a `_glow` map and, for animated pieces, an animation clip. Since 1.99.0 (the",
         "Djinn-quality remake, `docs/erza_armour_remake.md`) every armour is a **full set of eighteen pieces** (belt, gorget, two gauntlets, two vambraces, two sabatons, two greaves, two cuisses, skirt, helm, two",
         "pauldrons, chest, backplate) plus its own signature pieces, and the forging order follows the weight of a piece: the light underlayers first (belt 0, gorget 1), then the hands and feet, the legs, the",
         "helm and skirt, the pauldrons, the cuirass and the back, the wings and the halo, the backdrop last (delay 12); `ArmourSets.weightOf` is the rule, `ErzaArmourPiecesTest` pins it.", ""]
    for key, aid, title in armours():
        L += ["**%s** (`erza_%s`)" % (title, aid), "", "| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |", "|---|---|---|---|---|---|"]
        names = list(base) + [e[0] for e in sets[key]]
        glass = {e[0] for e in sets[key] if e[1]}
        for n in names:
            f = fac[n]
            flags = list(f["flags"])
            if n in glass and "translucent" not in flags:
                flags.insert(0, "translucent")
            stem = re.sub(r"_[rl]$", "", f["name"])
            L.append("| %s | %s | %g | %d | %s | %s |" % (f["name"], f["anchor"], f["delay"], WEIGHT.get(stem, 3), side(f["d"]), ", ".join(flags)))
        L.append("")
    return "\n".join(L) + "\n"


def rewrite_requip():
    s = open(REQUIP_MD, encoding="utf-8").read()
    a = s.index("## Pieces: anchor part, delay, fly-in side")
    b = s.index("## Weapons and engravings")
    s = s[:a] + piece_block() + "\n" + s[b:]
    s = s.replace("| Assets | `tools/gen_erza_requip_geo.py` (176 piece models), `tools/gen_erza_weapons.py` (six weapons, the book, the skill icon), the preview scenes `tools/render_preview/scenes.d/erza_requip.json` |",
                  "| Assets | `tools/gen_erza_armour.py` (the 1.99.0 Djinn-quality remake: 288 piece models, see `docs/erza_armour_remake.md`; the 1.96.0 `tools/gen_erza_requip_geo.py` is kept behind `--legacy`), `tools/gen_erza_weapons.py` (six weapons, the book, the skill icon), the preview scenes `tools/render_preview/scenes.d/erza_requip.json` and `erza_armour.json` |")
    s = s.replace("(`ArmourTableTest`, `RequipAnimationTest`, `ArmourAssetsTest` (`src/test/java/.../requip`)", "(`ArmourTableTest`, `RequipAnimationTest`, `ArmourAssetsTest`, `ErzaArmourAuditTest`, `ErzaArmourPiecesTest` (`src/test/java/.../requip`)")
    open(REQUIP_MD, "w", encoding="utf-8").write(s)


def numbers():
    djinn = AU.djinn_all()
    erza = AU.erza_all()
    before = json.load(open(os.path.join(ROOT, "docs", "erza_armour_before_1_96.json")))["erza"]
    bar = AU.bar_of(djinn)
    L = ["### The Djinn standard, measured", "", AU.tables(djinn, []), "", "The bar the Erza armours must reach (the minimum over the 36 unique Djinn sets) and that `ErzaArmourAuditTest` and the in-game self-test `skill.erza_requip2` hold them to:", ""]
    L += ["| Measure | Djinn minimum |", "|---|---|"]
    names = {"pieces_min": "pieces in a set", "faces_min": "drawn faces (quads)", "glow_faces_min": "glow faces", "painted_texels_min": "painted skin texels", "glow_painted_texels_min": "painted glow texels",
             "palette_min": "palette (5-bit colours holding 0.1 percent of the paint)", "glow_palette_min": "glow palette", "density_min": "texels per model pixel (median)", "texture_side_min": "skin atlas side (texels; the whole set shares one atlas)"}
    for k, label in names.items():
        L.append("| %s | %s |" % (label, bar[k]))
    L += ["", "### The 1.96.0 armours against the bar (before)", "", "| Armour | Pieces | Cubes | Drawn faces | Largest piece texture | Painted texels | Density |", "|---|---|---|---|---|---|---|"]
    for e in before:
        L.append("| %s | %d | %d | %d | %d | %d | %s |" % (e["id"], e["pieces"], e["cubes"], e["faces"], e["texture_max"], e["painted_texels"], e["density_median"]))
    bmin = lambda k: min(e[k] for e in before)
    bmax = lambda k: max(e[k] for e in before)
    L += ["", "The old sets had %d to %d pieces (bar 19), %d to %d faces (bar 2856), pieces textured at 128 or 256 px at %.1f texels per model pixel (bar 5.45) and %d to %d painted texels (bar 664603)." % (
        bmin("pieces"), bmax("pieces"), bmin("faces"), bmax("faces"), bmin("density_median"), bmin("painted_texels"), bmax("painted_texels")), ""]
    L += ["### The 1.99.0 armours (after)", "", AU.tables([], erza).split("### Erza sets")[1].strip("\n").lstrip("\n"), ""]
    low = lambda k: min(e[k] for e in erza)
    L += ["The new sets have %d to %d pieces, %d to %d drawn faces, %.1f to %.1f texels per model pixel, %d to %d painted texels (every one at or above the Djinn minimum)." % (
        low("pieces"), max(e["pieces"] for e in erza), low("faces"), max(e["faces"] for e in erza), low("density_median"), max(e["density_median"] for e in erza),
        low("painted_texels"), max(e["painted_texels"] for e in erza)), ""]
    return "\n".join(L)


def rewrite_remake():
    s = open(REMAKE_MD, encoding="utf-8").read()
    a = s.index("<!-- numbers -->") + len("<!-- numbers -->")
    b = s.index("<!-- /numbers -->")
    s = s[:a] + "\n\n" + numbers() + "\n" + s[b:]
    open(REMAKE_MD, "w", encoding="utf-8").write(s)


if __name__ == "__main__":
    rewrite_requip()
    print("rewrote the pieces block of docs/erza_requip.md")
    if os.path.isfile(REMAKE_MD):
        rewrite_remake()
        print("rewrote the numbers of docs/erza_armour_remake.md")
