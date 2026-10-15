#!/usr/bin/env python3
"""
A structure-template DSL for the Labyrinth: build a piece in Python, save it as the gzip NBT Minecraft 1.21.1 reads
(`data/nusmp/structure/<name>.nbt`), load one back, preview it as ASCII layers, and `--check` every template in the mod.

Why a DSL and no Blockbench / structure blocks: the Labyrinth is dozens of Jigsaw pieces that must agree on exact jigsaw positions,
sizes and block ids across builders. Generated templates are deterministic (same script -> same bytes), reviewable in a diff
(`python tools/labyrinth/template.py --preview labyrinth/base`), regenerated with one command when the palette or a block id changes,
and validated in CI (`--check`: round-trips every .nbt and checks every jigsaw's orientation/name/target/pool).

    t = Template("gate/ruin_a", 13, 14, 13)                 # name (may contain a slash), size x y z
    t.fill((0, 0, 0), (12, 0, 12), "minecraft:stone_bricks")
    t.set(6, 1, 6, "nusmp:labyrinth_pillar", {"axis": "y"})
    t.jigsaw(6, 13, 6, "up_north", "nusmp:up", "nusmp:down", "nusmp:labyrinth/ascent_1", joint="rollable")
    t.chest(3, 1, 3, "nusmp:chests/labyrinth_trial", facing="south")
    t.be(2, 1, 2, "nusmp:sealed_archway", {"id": "nusmp:sealed_archway", "radius": 9})
    t.save("src/main/resources")                           # -> src/main/resources/data/nusmp/structure/gate/ruin_a.nbt
    print(t.ascii_layers())
    load("src/main/resources/data/nusmp/structure/gate/ruin_a.nbt")

Cells that are never set are OMITTED from the NBT: the world keeps whatever is there (in the void dimension that is air anyway);
set "minecraft:air" explicitly where a piece must carve the world out. Coordinates are template-local, 0-based, inclusive ranges.
"""
import argparse
import glob
import gzip
import os
import re
import sys

try:
    from . import nbt
except ImportError:  # run as a script: tools/labyrinth is sys.path[0]
    import nbt

DATA_VERSION = 3955  # Minecraft 1.21.1
MODID = "nusmp"

#: The twelve jigsaw orientations: `<facing>_<top>`; `north_up` faces north with its top up (horizontal), `up_north` faces up (vertical).
ORIENTATIONS = ("down_east", "down_north", "down_south", "down_west", "up_east", "up_north", "up_south", "up_west",
                "west_up", "east_up", "north_up", "south_up")
JOINTS = ("rollable", "aligned")
_DIRS = {"down": (0, -1, 0), "up": (0, 1, 0), "north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}

_RL = re.compile(r"^(?:[a-z0-9_.\-]+:)?[a-z0-9_./\-]+$")
_STATE = re.compile(r"^[a-z0-9_.\-]+:[a-z0-9_./\-]+(?:\[[a-z0-9_]+=[a-z0-9_.\-]+(?:,[a-z0-9_]+=[a-z0-9_.\-]+)*\])?$")


def facing(orientation):
    """The unit vector a jigsaw with this orientation faces (its `front`)."""
    if orientation not in ORIENTATIONS:
        raise ValueError("bad jigsaw orientation %r (one of %s)" % (orientation, ", ".join(ORIENTATIONS)))
    return _DIRS[orientation.split("_")[0]]


def is_resource_location(s):
    return isinstance(s, str) and bool(_RL.match(s))


def is_block_state_string(s):
    return isinstance(s, str) and bool(_STATE.match(s))


def _norm_props(props):
    if not props:
        return {}
    out = {}
    for k, v in props.items():
        if isinstance(v, bool):
            v = "true" if v else "false"
        out[str(k)] = str(v)
    return out


class Cell:
    __slots__ = ("block", "props", "nbt")

    def __init__(self, block, props, nbt_):
        self.block = block
        self.props = props
        self.nbt = nbt_

    def key(self):
        return (self.block, tuple(sorted(self.props.items())))

    def __eq__(self, other):
        return isinstance(other, Cell) and self.key() == other.key() and _nbt_eq(self.nbt, other.nbt)

    def __repr__(self):
        return "Cell(%s%s%s)" % (self.block, self.props or "", (" " + nbt.snbt(self.nbt)) if self.nbt else "")


def _nbt_eq(a, b):
    if a is None or b is None:
        return a is b
    return nbt.dumps(nbt.Compound(a)) == nbt.dumps(nbt.Compound(b))


class Template:
    def __init__(self, name, sx, sy, sz):
        if not re.match(r"^[a-z0-9_./\-]+$", name) or name.startswith("/") or name.endswith("/"):
            raise ValueError("template name %r must be a resource path (lower-case, slashes allowed)" % name)
        for v in (sx, sy, sz):
            if not isinstance(v, int) or v <= 0:
                raise ValueError("template size must be positive ints, got %r" % ((sx, sy, sz),))
        self.name = name
        self.size = (sx, sy, sz)
        self.cells = {}
        self.entities = []  # raw entity compounds, kept only so a loaded file round-trips

    # ---- building ----

    def _check(self, x, y, z):
        sx, sy, sz = self.size
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
            raise ValueError("%s: (%d,%d,%d) is outside the template size %s" % (self.name, x, y, z, self.size))

    def set(self, x, y, z, block_id, props=None, nbt=None):
        """Place a block state at a local position; nbt is the block-entity compound (without x/y/z)."""
        self._check(x, y, z)
        if not is_resource_location(block_id):
            raise ValueError("bad block id %r" % block_id)
        if ":" not in block_id:
            block_id = "minecraft:" + block_id
        self.cells[(x, y, z)] = Cell(block_id, _norm_props(props), nbt_compound(nbt) if nbt is not None else None)
        return self

    def get(self, x, y, z):
        return self.cells.get((x, y, z))

    def remove(self, x, y, z):
        """Forget a cell: the world keeps its block there."""
        self.cells.pop((x, y, z), None)
        return self

    def fill(self, a, b, block_id, props=None):
        """Fill the inclusive box between corners a and b (any corner order)."""
        (x1, y1, z1), (x2, y2, z2) = _box(a, b)
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                for x in range(x1, x2 + 1):
                    self.set(x, y, z, block_id, props)
        return self

    def hollow_box(self, a, b, block_id, props=None):
        """Only the six faces of the inclusive box (the inside is left as it is)."""
        (x1, y1, z1), (x2, y2, z2) = _box(a, b)
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                for x in range(x1, x2 + 1):
                    if x in (x1, x2) or y in (y1, y2) or z in (z1, z2):
                        self.set(x, y, z, block_id, props)
        return self

    def column(self, x, z, y1, y2, block_id, props=None):
        return self.fill((x, y1, z), (x, y2, z), block_id, props)

    def stamp(self, other, dx, dy, dz):
        """Copy every cell of another template in at an offset (cells outside this template raise)."""
        for (x, y, z), c in other.cells.items():
            self.set(x + dx, y + dy, z + dz, c.block, dict(c.props), nbt.Compound(c.nbt) if c.nbt is not None else None)
        return self

    def be(self, x, y, z, block_id, nbt, props=None):
        """A block with a block entity; nbt must carry the BE `id`."""
        if "id" not in nbt:
            raise ValueError("block entity nbt at (%d,%d,%d) needs an 'id'" % (x, y, z))
        return self.set(x, y, z, block_id, props, nbt)

    def chest(self, x, y, z, loot_table, facing="north"):
        """A loot chest: the table rolls on first open (LootTableSeed 0 = per-world random)."""
        if not is_resource_location(loot_table) or ":" not in loot_table:
            raise ValueError("bad loot table id %r" % loot_table)
        if facing not in ("north", "south", "west", "east"):
            raise ValueError("chest facing must be a horizontal direction, got %r" % facing)
        return self.be(x, y, z, "minecraft:chest",
                       nbt.Compound({"id": "minecraft:chest", "LootTable": loot_table, "LootTableSeed": nbt.Long(0)}),
                       {"facing": facing, "type": "single", "waterlogged": "false"})

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state="minecraft:air", joint="aligned",
               selection_priority=0, placement_priority=0):
        """A jigsaw block. `name` is this jigsaw's own name, `target` the name it looks for in the next piece, `pool` the pool it draws
        from; `final_state` is what the jigsaw becomes once placed (the masonry block when it sits in a wall)."""
        facing(orientation)
        for label, v in (("name", name), ("target", target), ("pool", pool)):
            if not is_resource_location(v) or ":" not in v:
                raise ValueError("jigsaw %s %r must be a namespaced id" % (label, v))
        if not is_block_state_string(final_state):
            raise ValueError("jigsaw final_state %r must be a block state string like minecraft:air or nusmp:x[axis=y]" % final_state)
        if joint not in JOINTS:
            raise ValueError("jigsaw joint must be rollable or aligned, got %r" % joint)
        data = nbt.Compound({"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final_state,
                             "joint": joint, "placement_priority": nbt.Int(placement_priority),
                             "selection_priority": nbt.Int(selection_priority)})
        return self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, data)

    def jigsaws(self):
        """[(pos, orientation, nbt)] of every jigsaw block, sorted by position."""
        out = []
        for pos in sorted(self.cells, key=lambda p: (p[1], p[2], p[0])):
            c = self.cells[pos]
            if c.block == "minecraft:jigsaw":
                out.append((pos, c.props.get("orientation", ""), c.nbt))
        return out

    # ---- NBT ----

    def to_nbt(self):
        palette = []
        index = {}
        blocks = nbt.List(tag_type=nbt.TAG_COMPOUND)
        for pos in sorted(self.cells, key=lambda p: (p[1], p[2], p[0])):
            c = self.cells[pos]
            k = c.key()
            if k not in index:
                index[k] = len(palette)
                entry = nbt.Compound({"Name": c.block})
                if c.props:
                    entry["Properties"] = nbt.Compound({pk: pv for pk, pv in sorted(c.props.items())})
                palette.append(entry)
            b = nbt.Compound({"pos": nbt.List([nbt.Int(v) for v in pos]), "state": nbt.Int(index[k])})
            if c.nbt is not None:
                b["nbt"] = nbt.Compound(c.nbt)
            blocks.append(b)
        return nbt.Compound({
            "DataVersion": nbt.Int(DATA_VERSION),
            "size": nbt.List([nbt.Int(v) for v in self.size]),
            "palette": nbt.List(palette, tag_type=nbt.TAG_COMPOUND),
            "blocks": blocks,
            "entities": nbt.List(list(self.entities), tag_type=nbt.TAG_COMPOUND),
        })

    @classmethod
    def from_nbt(cls, name, data):
        size = [int(v) for v in data["size"]]
        t = cls(name, *size)
        palette = data.get("palette")
        if palette is None:  # vanilla can store several random palettes; the first one is the template
            palette = data["palettes"][0]
        states = [(str(p["Name"]), {str(k): str(v) for k, v in dict(p.get("Properties", {})).items()}) for p in palette]
        for b in data["blocks"]:
            x, y, z = [int(v) for v in b["pos"]]
            block, props = states[int(b["state"])]
            t.set(x, y, z, block, props, b.get("nbt"))
        t.entities = list(data.get("entities", []))
        return t

    def path(self, root):
        return os.path.join(root, "data", MODID, "structure", *self.name.split("/")) + ".nbt"

    def save(self, root):
        """Write `root/data/nusmp/structure/<name>.nbt` (gzip, DataVersion 3955). Returns the path."""
        p = self.path(root)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        nbt.dump(p, self.to_nbt())
        return p

    # ---- preview ----

    def ascii_layers(self):
        """One grid per y layer (rows = z, columns = x), '.' for unset cells, with a legend."""
        fixed = {"minecraft:jigsaw": "J", "minecraft:chest": "C", "minecraft:air": "~", "minecraft:structure_void": "_"}
        letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIKLMNOPQRSTUVWXYZ0123456789"
        legend = {}
        n = 0
        for pos in sorted(self.cells, key=lambda p: (p[1], p[2], p[0])):
            b = self.cells[pos].block
            if b in fixed or b in legend:
                continue
            legend[b] = letters[n] if n < len(letters) else "#"
            n += 1
        sx, sy, sz = self.size
        lines = ["%s  size %dx%dx%d  %d blocks, %d jigsaws" % (self.name, sx, sy, sz, len(self.cells), len(self.jigsaws()))]
        for y in range(sy):
            lines.append("y=%d" % y)
            for z in range(sz):
                row = []
                for x in range(sx):
                    c = self.cells.get((x, y, z))
                    row.append("." if c is None else fixed.get(c.block) or legend[c.block])
                lines.append("".join(row))
        lines.append("legend: " + "  ".join("%s=%s" % (v, k) for k, v in sorted(fixed.items(), key=lambda kv: kv[1]))
                     + "  " + "  ".join("%s=%s" % (v, k) for k, v in legend.items()))
        for pos, o, data in self.jigsaws():
            lines.append("jigsaw %s %s name=%s target=%s pool=%s joint=%s final=%s" % (
                pos, o, data["name"], data["target"], data["pool"], data["joint"], data["final_state"]))
        return "\n".join(lines)

    def __eq__(self, other):
        return isinstance(other, Template) and self.size == other.size and self.cells == other.cells


def nbt_compound(d):
    return d if isinstance(d, nbt.Compound) else nbt.Compound(d)


def _box(a, b):
    (x1, y1, z1), (x2, y2, z2) = a, b
    return (min(x1, x2), min(y1, y2), min(z1, z2)), (max(x1, x2), max(y1, y2), max(z1, z2))


def load(path, name=None):
    """Read a template file back. The name is taken from the path after `structure/` unless given."""
    if name is None:
        norm = path.replace("\\", "/")
        m = re.search(r"/structure/(.+)\.nbt$", norm)
        name = m.group(1) if m else os.path.splitext(os.path.basename(norm))[0]
    _, data = nbt.load(path)
    return Template.from_nbt(name, data)


# ---- --check ----

def validate(t):
    """Problems with a template (empty list = fine): jigsaw strings, positions, blocks ids."""
    problems = []
    sx, sy, sz = t.size
    for (x, y, z), c in t.cells.items():
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
            problems.append("block at (%d,%d,%d) outside size %s" % (x, y, z, t.size))
        if not is_resource_location(c.block) or ":" not in c.block:
            problems.append("bad block id %r at (%d,%d,%d)" % (c.block, x, y, z))
        if c.block == "minecraft:chest" and c.nbt is not None and "LootTable" in c.nbt and not is_resource_location(str(c.nbt["LootTable"])):
            problems.append("bad LootTable %r at (%d,%d,%d)" % (c.nbt["LootTable"], x, y, z))
    for (x, y, z), o, data in t.jigsaws():
        where = "jigsaw at (%d,%d,%d)" % (x, y, z)
        if o not in ORIENTATIONS:
            problems.append("%s: bad orientation %r" % (where, o))
        if data is None or str(data.get("id", "")) != "minecraft:jigsaw":
            problems.append("%s: block entity id is not minecraft:jigsaw" % where)
            continue
        for key in ("name", "target", "pool"):
            v = data.get(key)
            if not isinstance(v, str) or not is_resource_location(v) or ":" not in v:
                problems.append("%s: %s %r is not a namespaced id" % (where, key, v))
        if not is_block_state_string(str(data.get("final_state", ""))):
            problems.append("%s: bad final_state %r" % (where, data.get("final_state")))
        if str(data.get("joint", "")) not in JOINTS:
            problems.append("%s: joint %r is not rollable/aligned" % (where, data.get("joint")))
        fx, fy, fz = facing(o) if o in ORIENTATIONS else (0, 0, 0)
        nx, ny, nz = x + fx, y + fy, z + fz
        if (0 <= nx < sx and 0 <= ny < sy and 0 <= nz < sz) and (nx, ny, nz) in t.cells and t.cells[(nx, ny, nz)].block not in ("minecraft:air", "minecraft:structure_void"):
            problems.append("%s faces %s into a solid cell of its own template (%s): the next piece cannot attach there" % (where, o, t.cells[(nx, ny, nz)].block))
    return problems


def resources_root():
    return os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources"))


def check(root, verbose=True):
    files = sorted(glob.glob(os.path.join(root, "data", MODID, "structure", "**", "*.nbt"), recursive=True))
    bad = 0
    for p in files:
        rel = os.path.relpath(p, root).replace("\\", "/")
        try:
            with open(p, "rb") as f:
                raw = f.read()
            _, data = nbt.loads(raw)
            t = Template.from_nbt(rel, data)
            again = Template.from_nbt(rel, nbt.loads(nbt.dumps(t.to_nbt(), compress=True))[1])
            problems = validate(t)
            if again != t:
                problems.append("template does not round-trip through the writer")
            if int(data.get("DataVersion", 0)) != DATA_VERSION:
                problems.append("DataVersion %s, expected %d" % (data.get("DataVersion"), DATA_VERSION))
            plain = gzip.decompress(raw) if raw[:2] == b"\x1f\x8b" else raw
            if nbt.dumps(data) != plain:
                problems.append("nbt reader/writer disagree: the re-serialised bytes differ from the file")
        except Exception as e:  # a corrupt file is a failure, not a crash
            problems = ["cannot read: %s" % e]
            t = None
        if problems:
            bad += 1
            print("FAIL %s" % rel)
            for m in problems:
                print("   - %s" % m)
        elif verbose:
            print("ok   %-48s %dx%dx%d %4d blocks %2d jigsaws" % (rel, t.size[0], t.size[1], t.size[2], len(t.cells), len(t.jigsaws())))
    print("%d template(s), %d failed" % (len(files), bad))
    return bad == 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--root", default=resources_root(), help="resources root (default: src/main/resources of this repo)")
    ap.add_argument("--check", action="store_true", help="round-trip and validate every template under data/nusmp/structure")
    ap.add_argument("--preview", metavar="NAME", help="print the ASCII layers of a template (name like gate/ruin_a, or a path)")
    ap.add_argument("--list", action="store_true", help="list the templates")
    a = ap.parse_args(argv)
    if a.preview:
        p = a.preview if a.preview.endswith(".nbt") else os.path.join(a.root, "data", MODID, "structure", a.preview + ".nbt")
        print(load(p).ascii_layers())
        return 0
    if a.list:
        for p in sorted(glob.glob(os.path.join(a.root, "data", MODID, "structure", "**", "*.nbt"), recursive=True)):
            print(os.path.relpath(p, a.root))
        return 0
    if a.check:
        return 0 if check(a.root) else 1
    ap.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
