"""wave3_kit: the shared bits of style wave 3 (docs/heroweapon/style/wave3.md). NEW file, it edits nothing that exists.

Wave 3 restyles ten weapons (spears, katanas, carriages, the Yata mirror, the Phoenix sword) the same way wave 2 did: the palette of wave2_kit (3 base materials + ONE named accent,
all through forms_kit.surface(): wear, grime and mottle painted into the skin), unequal spacing, one hero feature, no gold unless the lore says gold.
What this module adds:
  * `header(mesh_id)`: the previous mesh of every wave-3 weapon is kept in weapon_meshes/classic_style3/; the rebuilt mesh takes its `place` line from that copy, so the weapon sits
    in the hand exactly where it did (same hand, same length class);
  * `use_classic_headers()`: makes forms_kit.finish_form take that header (the vassal and exemplar drivers call finish_form with the family base, whose place line is not the form's);
  * `mark_solid(mesh_id)`: the spear meshes carry the `# blender-solid` line of 4.55.0 (HeroSolidTest); a rebuilt spear keeps it.
Users: forms_spear_w3.py, forms_katana_w3.py, forms_carriage_w3.py, forms_mirror_w3.py, forms_phoenix_w3.py.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402,F401
from wave2_kit import iron, steel, wood, hide, cord, paper, accent_gem, accent_glow, scratch, Palette  # noqa: E402,F401
from bl_lib import rgb  # noqa: E402,F401

CLASSIC = os.path.join(K.bl.MESH_DIR, "classic_style3")
MARK = "# blender-solid 4.55.0 (docs/heroweapon/flat_parts_blender.md)"
MANIFEST = os.path.join(HERE, "forms_manifest_style_wave3.json")

IDS = ["legendary_spear__dragon_minute_hand", "legendary_spear", "legendary_spear_curse", "legendary_spear__portal_spear", "vassal_katana__siderite_katana", "vassal_katana__white_tiger_katana",
       "vassal_carriage__war_carriage", "vassal_carriage__queens_chariot", "vassal_mirror__yata_mirror", "legendary_sword__phoenix_sword"]


def header(mesh_id):
    """The copy of the previous mesh (its place line): where the hand holds the weapon."""
    p = os.path.join(CLASSIC, mesh_id + ".txt")
    if not os.path.exists(p):
        raise SystemExit("no classic_style3 copy of %s: copy the old mesh there before rebuilding it" % mesh_id)
    return p


_real_finish_form = K.finish_form


def use_classic_headers():
    """forms_kit.finish_form(item, base, ...) with header_from = the classic_style3 copy of `item` (idempotent)."""
    def patched(item_id, base_id, **kw):
        kw["header_from"] = header(item_id)
        return _real_finish_form(item_id, base_id, **kw)
    K.finish_form = patched
    px = int(os.environ.get("W3_SKIN", "0") or 0)                       # W3_SKIN=768: a smaller skin so skin + glow stay under the 350 KB budget
    for mod in ("forms_vassal_common", "forms_vassal_common_b"):
        if px and mod in sys.modules:
            sys.modules[mod].SKIN_PX[0] = px


def mark_solid(mesh_id, trial=None):
    """Adds the 4.55.0 `# blender-solid` line (line 2) to a finished spear mesh when it has none."""
    p = os.path.join(os.path.join(trial, "weapon_meshes") if trial else K.bl.MESH_DIR, mesh_id + ".txt")
    lines = open(p, encoding="utf-8").read().split("\n")
    if any(ln.startswith("# blender-solid") for ln in lines[:4]):
        return
    lines.insert(1 if lines[0].startswith("#") else 0, MARK)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))
