#!/usr/bin/env python3
"""
The static checker of the Mega-Dungeon data (1.54.0): the substitute for "load it in the game and see". Run it before you hand anything over:

    python3 tools/labyrinth/mega_check.py            # every template under structure/mega, every pool under template_pool/mega
    python3 tools/labyrinth/mega_check.py --quiet    # errors only
    python3 tools/labyrinth/mega_check.py mega/safe/hearth_hall   # one template (name relative to structure/)

Why each rule exists (see docs/labyrinth/mega.md "Connector contract"):
  * size: x and z are whole 9-block modules and y is 48 for every piece (door caps: 3 wide), so pieces tile and floors are level;
  * jigsaws: on the OUTER wall layer, in the first row above the floor, at a module centre, pointing out of the face they sit on, named with
    one of the four allowed names; an unused socket becomes its `final_state`, so a socket that finishes as air must have a clear 3x4x2 hole;
  * pools: every pool a jigsaw names exists and every template a pool names exists (a missing one crashes the structure at world load);
  * blocks: vanilla only (the Vanilla Block Primacy Rule), except the few custom blocks listed in mega_lib.CUSTOM;
  * markers: a data marker must parse with the grammar `nusmp:<kind>[;key=value]*` and name a known kind;
  * processors: a processor list may only name processor types that some Java class registers (an unknown one fails the datapack load).
Exit status 1 when any ERROR was found (warnings never fail).
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from template import load  # noqa: E402

DATA = os.path.join(L.RES, "data", "nusmp")
STRUCT = os.path.join(DATA, "structure")
POOLS = os.path.join(DATA, "worldgen", "template_pool", "mega")
PROCS = os.path.join(DATA, "worldgen", "processor_list")
LOOT = os.path.join(DATA, "loot_table")
JAVA = os.path.join(L.ROOT, "src", "main", "java")
ALLOWED_NAMES = {L.DOOR, L.CHAIN, L.VAULT, L.ONWARD}
MARKER_RE = re.compile(r"^nusmp:[a-z_]+(;[a-z_]+(=[^;]*)?)*$")
VANILLA_PROCESSORS = {"minecraft:rule", "minecraft:block_ignore", "minecraft:block_rot", "minecraft:gravity", "minecraft:protected_blocks",
                      "minecraft:capped", "minecraft:nop", "minecraft:block_age", "minecraft:blackstone_replace", "minecraft:lava_submerged_block"}
#: architecture-like vanilla blocks outside the palette: a theme swap would not reach them, so a template that leans on them stays grey in every sector
ARCH_NON_PALETTE = re.compile(r"minecraft:(stone_bricks|stone|cobblestone|bricks|nether_bricks|red_nether_bricks|sandstone|smooth_stone|andesite|"
                              r"polished_andesite|granite|diorite|blackstone|polished_blackstone|polished_blackstone_bricks|end_stone_bricks|purpur_block|"
                              r"quartz_block|mud_bricks|tuff_bricks|prismarine_bricks|mossy_stone_bricks)(_stairs|_slab|_wall)?")


def vanilla_blocks():
    with open(os.path.join(HERE, "vanilla_blocks_1_21_1.txt")) as f:
        return {("minecraft:" + ln.strip()) for ln in f if ln.strip() and not ln.startswith("#")}


def registered_nusmp_blocks():
    out = set()
    pat = re.compile(r'BLOCKS\.register\("([a-z0-9_]+)"')
    for path in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        try:
            for m in pat.finditer(open(path, encoding="utf-8", errors="replace").read()):
                out.add("nusmp:" + m.group(1))
        except OSError:
            pass
    return out


#: processor types the contract promises (registered by the theme and safe builders); a missing registration is only a warning here and an error at integration
CONTRACT_PROCESSORS = {"nusmp:region_theme", "nusmp:mod_compat"}


def registered_processor_types():
    out = set(VANILLA_PROCESSORS) | CONTRACT_PROCESSORS
    pat = re.compile(r'PROCESSORS\.register\("([a-z0-9_]+)"')
    for path in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        try:
            for m in pat.finditer(open(path, encoding="utf-8", errors="replace").read()):
                out.add("nusmp:" + m.group(1))
        except OSError:
            pass
    return out


def template_paths(only=None):
    base = os.path.join(STRUCT, "mega")
    paths = sorted(glob.glob(os.path.join(base, "**", "*.nbt"), recursive=True))
    if only:
        paths = [p for p in paths if os.path.relpath(p, STRUCT).replace(os.sep, "/")[:-4] in only]
    return paths


def name_of(path):
    return os.path.relpath(path, STRUCT).replace(os.sep, "/")[:-4]


def check_template(path, known_blocks, errors, warns, pools):
    name = name_of(path)
    try:
        t = load(path, name)
    except Exception as e:  # noqa: BLE001
        errors.append("%s: cannot be read: %s" % (name, e))
        return None
    sx, sy, sz = t.size
    parts = name.split("/")
    folder = parts[1] if len(parts) > 2 else ""
    is_cap = folder == "cap"
    if is_cap:
        if sx != 3 or not (1 <= sz <= 8) or sy != 6:
            errors.append("%s: a door cap is 3 wide, 6 high and 1..8 deep (floor row, 4 open rows, roof row); got %dx%dx%d" % (name, sx, sy, sz))
    else:
        if sx % L.MODULE or sz % L.MODULE:
            errors.append("%s: x and z must be whole modules of %d (got %d x %d)" % (name, L.MODULE, sx, sz))
        if sy != L.HEIGHT:
            errors.append("%s: height must be %d (got %d): the floor row must be the same in every piece" % (name, L.HEIGHT, sy))
    # blocks
    bad = {}
    non_palette = {}
    for pos, c in t.cells.items():
        b = c.block
        if b.startswith("minecraft:"):
            if b not in known_blocks["vanilla"]:
                bad[b] = bad.get(b, 0) + 1
            elif ARCH_NON_PALETTE.fullmatch(b):
                non_palette[b] = non_palette.get(b, 0) + 1
        elif b.startswith("nusmp:"):
            if b not in L.CUSTOM:
                errors.append("%s: custom block %s is not allowed in a Mega template (Vanilla Block Primacy Rule; see mega_lib.CUSTOM)" % (name, b))
            elif b not in known_blocks["nusmp"]:
                errors.append("%s: custom block %s is not registered by any BLOCKS.register(\"...\") in the Java sources" % (name, b))
        else:
            errors.append("%s: block %s is neither minecraft: nor nusmp:" % (name, b))
        err = L.check_state(b, c.props)
        if err:
            errors.append("%s: (%d,%d,%d) %s" % (name, pos[0], pos[1], pos[2], err))
        if b == "minecraft:chest" and c.nbt is not None and "LootTable" in c.nbt:
            lt = str(c.nbt["LootTable"])
            if lt.startswith("nusmp:") and not os.path.exists(os.path.join(LOOT, lt[6:] + ".json")):
                errors.append("%s: loot table %s does not exist" % (name, lt))
    for b, n in sorted(bad.items()):
        errors.append("%s: %d x %s is not a vanilla 1.21.1 block id" % (name, n, b))
    for b, n in sorted(non_palette.items()):
        if n >= 30:
            warns.append("%s: %d x %s is architecture outside the palette: the Region Theme will not restyle it (use the mega_lib palette)" % (name, n, b))
    # markers
    for pos, c in t.cells.items():
        if c.block == "minecraft:structure_block":
            meta = str(c.nbt.get("metadata", "")) if c.nbt is not None else ""
            mode = str(c.nbt.get("mode", "")) if c.nbt is not None else ""
            if mode != "DATA":
                errors.append("%s: structure block at %s is not in DATA mode" % (name, pos))
            elif not MARKER_RE.match(meta) or meta.split(";")[0][6:] not in L.MARKER_KINDS:
                errors.append("%s: marker %r at %s is not `nusmp:<known kind>[;key=value]*`" % (name, meta, pos))
    # jigsaws
    for (x, y, z), o, data in t.jigsaws():
        where = "%s: jigsaw at (%d,%d,%d)" % (name, x, y, z)
        nm, tg, pool = str(data["name"]), str(data["target"]), str(data["pool"])
        fin = str(data["final_state"])
        if is_cap:
            if (x, y, z) != (1, 1, 0) or o != "north_up":
                errors.append("%s: a cap's jigsaw must sit at (1,1,0) facing north_up (got %s %s)" % (where, (x, y, z), o))
            if nm != L.DOOR or tg != L.DOOR:
                errors.append("%s: a cap's jigsaw name and target must be %s" % (where, L.DOOR))
            continue
        face = o.split("_")[0]
        dist = {"north": z, "south": sz - 1 - z, "west": x, "east": sx - 1 - x}.get(face)
        lateral = {"north": x, "south": x, "west": z, "east": z}.get(face)
        edge = {"north": sx, "south": sx, "west": sz, "east": sz}.get(face)
        if dist is None:
            errors.append("%s: orientation %s is not a horizontal facing" % (where, o))
            continue
        if dist != 0:
            errors.append("%s: must sit on the OUTER wall layer of the %s face (it is %d cells in)" % (where, face, dist))
        if y != L.DOOR_ROW:
            errors.append("%s: must be in row %d (first row above the floor), not %d" % (where, L.DOOR_ROW, y))
        if lateral % L.MODULE != L.CENTER:
            errors.append("%s: lateral position %d is not a module centre (4 mod 9)" % (where, lateral))
        if nm not in ALLOWED_NAMES or tg not in ALLOWED_NAMES:
            errors.append("%s: name %s / target %s must be one of %s" % (where, nm, tg, ", ".join(sorted(ALLOWED_NAMES))))
        if pool != L.P_EMPTY and pool not in pools:
            errors.append("%s: pool %s does not exist (template_pool/mega/*.json)" % (where, pool))
        if fin == L.AIR:
            filled = []
            for depth in range(L.WALL):
                for lat in range(lateral - 1, lateral + 2):
                    for yy in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                        cx, cy, cz = {"north": (lat, yy, depth), "south": (lat, yy, sz - 1 - depth), "west": (depth, yy, lat), "east": (sx - 1 - depth, yy, lat)}[face]
                        c = t.get(cx, cy, cz)
                        if c is not None and c.block != L.AIR and (cx, cy, cz) != (x, y, z):
                            filled.append((cx, cy, cz))
            if filled:
                errors.append("%s: ends as air (an open niche) but %d hole cells are filled, e.g. %s" % (where, len(filled), filled[0]))
    return t


def check_pools(errors, warns, only=None):
    pools = {}
    for path in sorted(glob.glob(os.path.join(POOLS, "*.json"))):
        pid = "nusmp:mega/" + os.path.basename(path)[:-5]
        try:
            pools[pid] = json.load(open(path))
        except Exception as e:  # noqa: BLE001
            errors.append("%s: invalid JSON: %s" % (pid, e))
    return pools


def check_pool_contents(pools, templates_present, errors, warns, proc_lists):
    referenced = set()
    for pid, p in pools.items():
        els = p.get("elements", [])
        if not els:
            errors.append("%s: no elements" % pid)
        fb = p.get("fallback", "minecraft:empty")
        if fb != L.P_EMPTY and fb not in pools:
            errors.append("%s: fallback pool %s does not exist" % (pid, fb))
        for e in els:
            el = e.get("element", {})
            et = el.get("element_type")
            if et == "minecraft:empty_pool_element":
                continue
            loc = el.get("location", "")
            referenced.add(loc)
            if loc.startswith("nusmp:") and loc[6:] not in templates_present:
                errors.append("%s: template %s does not exist (structure/%s.nbt)" % (pid, loc, loc[6:]))
            proc = el.get("processors", "minecraft:empty")
            if isinstance(proc, str) and proc != "minecraft:empty" and proc not in proc_lists:
                errors.append("%s: processor list %s does not exist" % (pid, proc))
            if el.get("projection", "rigid") != "rigid":
                errors.append("%s: %s must use projection rigid (a void dimension has no terrain)" % (pid, loc))
            if int(e.get("weight", 1)) <= 0 and et != "minecraft:empty_pool_element":
                warns.append("%s: %s has weight %s (never chosen)" % (pid, loc, e.get("weight")))
    return referenced


THEME_DIR = os.path.join(DATA, "mega", "template_themes")
REGION_THEME_JAVA = os.path.join(JAVA, "com", "newuniverse", "nusmp", "labyrinth", "mega", "RegionTheme.java")


def region_theme_ids():
    """The ids of the RegionTheme enum, read from its source (the names of the entries that have a constructor call, lower-cased): a template_themes file must name one."""
    text = open(REGION_THEME_JAVA, encoding="utf-8", errors="replace").read()
    body = text[text.index("public enum RegionTheme") :]
    end = re.search(r"^\s*[A-Z][A-Z0-9_]*\([^;\n]*\)\s*;", body, re.M)               # the last constant ends the list with ");"
    body = body[: end.end()] if end else body
    return {m.group(1).lower() for m in re.finditer(r"^\s*([A-Z][A-Z0-9_]*)\(", body, re.M)}


def check_template_themes(all_names, errors, warns):
    """1.58.0 (docs/labyrinth/mega_v2.md section 5.1): every data/nusmp/mega/template_themes/*.json must be valid JSON of the documented shape, name a theme that is
    a RegionTheme id (a typo would make its rooms unreachable: no sector ever wears that theme), list prefixes that match at least one template (a wrong prefix leaves
    the rooms free for every theme), and give `relative` weights only to prefixes it also claims. Returns the number of files read."""
    themes = region_theme_ids()
    n = 0
    for path in sorted(glob.glob(os.path.join(THEME_DIR, "*.json"))):
        n += 1
        fname = os.path.basename(path)
        try:
            doc = json.load(open(path))
        except Exception as e:  # noqa: BLE001
            errors.append("template_themes/%s: invalid JSON: %s" % (fname, e))
            continue
        if not isinstance(doc, dict):
            errors.append("template_themes/%s: not a JSON object" % fname)
            continue
        for k in doc:
            if k not in ("theme", "templates", "relative") and not k.startswith("_"):
                errors.append("template_themes/%s: unknown key %r" % (fname, k))
        theme = str(doc.get("theme", "")).strip().lower()
        if theme not in themes:
            errors.append("template_themes/%s: theme %r is not an id of the RegionTheme enum (%s)" % (fname, theme, ", ".join(sorted(themes))))
        prefixes = doc.get("templates")
        if not isinstance(prefixes, list) or not prefixes:
            errors.append("template_themes/%s: needs a non-empty \"templates\" list of template id prefixes" % fname)
            prefixes = []
        claimed = []
        for pre in prefixes:
            if not isinstance(pre, str) or not pre.strip():
                errors.append("template_themes/%s: a prefix is not a non-empty text" % fname)
                continue
            full = pre.strip() if ":" in pre else "nusmp:" + pre.strip()
            claimed.append(full[len("nusmp:"):] if full.startswith("nusmp:") else full)
            if not any(("nusmp:" + n).startswith(full) for n in all_names):
                errors.append("template_themes/%s: the prefix %r matches no template (a wrong prefix would leave the rooms free for every theme)" % (fname, pre))
        rel = doc.get("relative", {})
        if not isinstance(rel, dict):
            errors.append("template_themes/%s: \"relative\" must be an object of prefix -> weight" % fname)
            rel = {}
        for pre, w in rel.items():
            if not isinstance(w, int) or isinstance(w, bool) or not (1 <= w <= 150):
                errors.append("template_themes/%s: weight of %r must be an integer 1..150 (the vanilla pool limit), got %r" % (fname, pre, w))
            if not any(pre.startswith(c) or c.startswith(pre) for c in claimed):
                warns.append("template_themes/%s: the weight prefix %r is not under any claimed template prefix" % (fname, pre))
    return n


def main(argv):
    quiet = "--quiet" in argv
    only = [a for a in argv if not a.startswith("--")]
    errors, warns = [], []
    known = {"vanilla": vanilla_blocks(), "nusmp": registered_nusmp_blocks()}
    pools = check_pools(errors, warns)
    paths = template_paths(only or None)
    all_names = {name_of(p) for p in template_paths()}
    seen = 0
    for p in paths:
        check_template(p, known, errors, warns, pools)
        seen += 1
    proc_lists = {"nusmp:" + os.path.basename(p)[:-5] for p in glob.glob(os.path.join(PROCS, "*.json"))}
    types = registered_processor_types()
    for p in glob.glob(os.path.join(PROCS, "*.json")):
        try:
            for proc in json.load(open(p)).get("processors", []):
                pt = proc.get("processor_type", "")
                if pt not in types:
                    errors.append("%s: processor type %s is not registered by any PROCESSORS.register(\"...\") (an unknown type fails the datapack load)" % (os.path.basename(p), pt))
        except Exception as e:  # noqa: BLE001
            errors.append("%s: invalid JSON: %s" % (os.path.basename(p), e))
    referenced = check_pool_contents(pools, all_names, errors, warns, proc_lists)
    if not only:
        check_template_themes(all_names, errors, warns)
    if not only:
        for n in sorted(all_names):
            if "nusmp:" + n not in referenced and not n.startswith("mega/link/"):
                warns.append("%s: not referenced by any pool (orphan)" % n)
    for e in errors:
        print("ERROR  " + e)
    if not quiet:
        for w in warns:
            print("warn   " + w)
    print("%d templates, %d pools, %d errors, %d warnings" % (seen, len(pools), len(errors), len(warns)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
