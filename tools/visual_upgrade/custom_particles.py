"""Custom particles for the effect tables of the upgraded weapons and relics (release 1.110.0).

Every generator under tools/visual_upgrade writes tables (assets/nusmp/visual_upgrade/<id>.json, fx/<id>.json) whose layers name a particle. Those names used to be vanilla ones
("minecraft:flame", "end_rod", "dust"), and the game turned them into vanilla particles: white square puffs, a flame sprite that filled the first-person view, cyan dust. The owner asked
for the mod's own particles. The game now resolves every token to a custom particle (particle/fx/UpgradeParticleAlias.java), and this module makes the SHIPPED tables say so as well, so a
re-run of any generator keeps the fix:

    "dust"                      -> "fx"                      (the hue-themed custom mote, dyed with the layer colour)
    "minecraft:flame" / "flame" -> "nusmp:flame_tongue"      (the nearest custom particle, from particle_alias.json)
    "nusmp:..." / "fx"          -> unchanged

`convert(obj)` rewrites a table in place (every "particle" string, at any depth); each generator calls it right before it writes its json. Run as a script it converts every table of the
assets tree: `python3 -B tools/visual_upgrade/custom_particles.py` (add `--check` to fail when a table still holds a vanilla token). The alias table is particle_alias.json, which the unit
test UpgradeParticleAliasTest keeps identical to the Java table. The swing key "particles" (a descriptive name of the trail, not read by the game) and the integer "particles" of the acts
are not touched.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
TABLES = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "visual_upgrade")
CATALOG = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "particle_catalog.json")

with open(os.path.join(HERE, "particle_alias.json"), encoding="utf-8") as _f:
    ALIAS = json.load(_f)["alias"]
with open(CATALOG, encoding="utf-8") as _f:
    IDS = {p["id"] for p in json.load(_f)["particles"]}

FALLBACK = "mote"
THEMED = ("", "fx", "dust")


def token(t):
    """The custom token that replaces a table token (a catalog id is written as nusmp:<id>); already-custom tokens are returned unchanged."""
    if not isinstance(t, str):
        return t
    path = t.split(":", 1)[-1].strip().lower()
    if path in THEMED:
        return "fx"
    if t.startswith("nusmp:") and path in IDS:
        return t
    if path in ALIAS:
        return "nusmp:" + ALIAS[path]
    if path in IDS:
        return "nusmp:" + path
    return "nusmp:" + FALLBACK


def convert(obj):
    """Rewrites every "particle" string of a table (any depth) in place; returns the number of tokens changed."""
    n = 0
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == "particle" and isinstance(v, str):
                t = token(v)
                if t != v:
                    obj[k] = t
                    n += 1
            else:
                n += convert(v)
    elif isinstance(obj, list):
        for v in obj:
            n += convert(v)
    return n


_PAT = re.compile(r'("particle"\s*:\s*)"([^"]*)"')


def convert_text(raw):
    """The same rewrite on the text of a table, touching nothing but the token strings (the generator's own layout is kept). Returns (new text, tokens changed)."""
    count = [0]

    def sub(m):
        t = token(m.group(2))
        if t != m.group(2):
            count[0] += 1
        return m.group(1) + '"' + t + '"'

    return _PAT.sub(sub, raw), count[0]


def convert_tree(check=False):
    """Converts every table of the assets tree; with check=True it only counts. Returns (files changed, tokens changed)."""
    files = tokens = 0
    for dp, _dn, fn in os.walk(TABLES):
        for name in sorted(fn):
            if not name.endswith(".json"):
                continue
            path = os.path.join(dp, name)
            with open(path, encoding="utf-8", newline="") as f:
                raw = f.read()
            out, n = convert_text(raw)
            if n:
                files += 1
                tokens += n
                if not check:
                    with open(path, "w", encoding="utf-8", newline="") as f:
                        f.write(out)
    return files, tokens


if __name__ == "__main__":
    chk = "--check" in sys.argv
    nf, nt = convert_tree(check=chk)
    print(("would change" if chk else "changed"), nt, "particle tokens in", nf, "tables")
    sys.exit(1 if (chk and nt) else 0)
