"""Where the list of lines comes from, for the asset generators (gen_book_index.py).

Two sources, merged by line id:
  1. the route files the game loads: src/main/resources/data/nusmp/skill_trees/*.json (the 8 hand-built routes, and every generated line once gen_lines.py has written it);
  2. the rows the content authors write: tools/lines/rows/<franchise>.py through gen_lines.load_rows() (so the assets can be made BEFORE gen_lines.py has written the json,
     and while rows/ is still empty or half filled: whatever exists is used, nothing is required).
The file wins over the row when both exist (the file is what the game reads). Order: by id, so the output is deterministic.
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ROUTES = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "skill_trees")
sys.path.insert(0, HERE)

# the hand-built routes have no background theme in their json: this table gives them one (franchise / element of the route)
HAND_THEMES = {"oath_of_the_heavy_armor": "iron", "basic_swordsmanship": "ruin", "lady_luck": "city", "smoldering_fang_of_insanity": "magma"}


def _hex(c):
    if isinstance(c, int):
        return "#%06X" % (c & 0xFFFFFF)
    c = str(c).strip()
    if re.fullmatch(r"#[0-9a-fA-F]{6}", c):
        return c.upper()
    if re.fullmatch(r"0x[0-9a-fA-F]{6}", c):
        return "#" + c[2:].upper()
    return "#FFFFFF"


def load_rows(rows_dir=None):
    """gen_lines.load_rows(), optionally from another directory (tests and previews use a temporary rows folder that is never committed)."""
    if rows_dir is None:
        import gen_lines
        return gen_lines.load_rows()
    import importlib.util
    out = []
    for f in sorted(os.listdir(rows_dir)):
        if not f.endswith(".py") or f.startswith("_"):
            continue
        spec = importlib.util.spec_from_file_location("rows_tmp_" + f[:-3], os.path.join(rows_dir, f))
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        out.extend((f[:-3], L) for L in mod.LINES)
    return out


def is_art(n):
    """The nodes that become the modes of the unique skill, in file order: an art or stance of a generated line, or a classic node that grants a skill."""
    return n.get("type") == "active_ability" or (not n.get("kind") and bool(n.get("skill")))


def is_passive(n):
    """The nodes that are applied while the skill is owned and switched on: passives and stat bonuses."""
    return n.get("type") in ("passive_toggle", "attribute_modifier") or (not n.get("kind") and not n.get("skill") and bool(n.get("passives")))


def collect(rows_dir=None, routes_dir=None, with_rows=True):
    """-> dict id -> {id, name, book, book_name, color '#RRGGBB', franchise, theme, source}."""
    out = {}
    for path in sorted(glob.glob(os.path.join(routes_dir or ROUTES, "*.json"))):
        rid = os.path.basename(path)[:-5]
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        out[rid] = {"id": rid, "name": d.get("name", rid), "book": d.get("book", "nusmp:%s_book" % rid), "book_name": d.get("book_name", d.get("name", rid)),
                    "color": _hex(d.get("color", "#FFFFFF")), "franchise": d.get("franchise") or d.get("magic") or "",
                    "theme": d.get("background_theme") or HAND_THEMES.get(rid, ""), "source": "json",
                    "desc": d.get("desc", ""), "book_lore": d.get("book_lore", ""),
                    "arts": [n.get("name", n.get("id", "")) for n in d.get("nodes", []) if is_art(n)],
                    "passives": [n.get("name", n.get("id", "")) for n in d.get("nodes", []) if is_passive(n)]}
    if with_rows:
        for _fname, L in load_rows(rows_dir):
            if L.id in out:
                continue
            out[L.id] = {"id": L.id, "name": L.name, "book": "nusmp:%s_book" % L.id, "book_name": L.book_name, "color": _hex(L.color), "franchise": L.franchise,
                         "theme": L.background, "source": "row"}
    return dict(sorted(out.items()))
