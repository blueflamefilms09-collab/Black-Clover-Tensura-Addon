"""Reads / writes the weapon mesh text files (assets/anime_multiverse/weapon_meshes/<id>.txt) for tools/weaponmotion.

A file is: header lines (`# comment`, place, grip, orient, glow, ...), one `q` / `qn` line per quad (layer 0 body, 1 void, 2 glow), then the
`# part <name> <first quad> <count>` comments. `bone` / `skin` lines (the swing rig, docs/vassal_forms_v2.md) are written by build_swing.py and
dropped again when a file is read, so the tool is idempotent."""
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MESH_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "weapon_meshes")


class Quad:
    __slots__ = ("layer", "pts", "raw", "part", "idx")

    def __init__(self, layer, pts, raw, part, idx):
        self.layer, self.pts, self.raw, self.part, self.idx = layer, pts, raw, part, idx


class MeshFile:
    def __init__(self, mesh_id, path=None):
        self.id = mesh_id
        self.path = path or os.path.join(MESH_DIR, mesh_id + ".txt")
        self.header, self.quads = [], []
        parts = []
        with open(self.path, encoding="utf-8") as fh:
            lines = fh.read().split("\n")
        for ln in lines:
            if not ln.strip():
                continue
            t = ln.split()
            if t[0] in ("qn", "q"):
                layer = int(t[1])
                if t[0] == "qn":
                    pts = [tuple(float(x) for x in t[2 + k * 8: 5 + k * 8]) for k in range(4)]
                else:
                    pts = [tuple(float(x) for x in t[2 + k * 5: 5 + k * 5]) for k in range(4)]
                self.quads.append(Quad(layer, pts, ln, None, len(self.quads)))
            elif t[0] == "#" and len(t) >= 5 and t[1] == "part":
                parts.append((t[2], int(t[3]), int(t[4])))
            elif t[0] in ("bone", "skin"):
                continue
            else:
                self.header.append(ln)
        for name, first, cnt in parts:
            for q in self.quads[first:first + cnt]:
                q.part = name
        self.part_names = []
        for name, _f, _c in parts:
            if name not in self.part_names:
                self.part_names.append(name)

    def write(self, order, bones=(), skin=None, path=None):
        """order: the quads in their new file order; skin: {layer: [code per vertex of the animated tail]} (the tail = the last `len(codes)/4` quads of that layer)."""
        out = list(self.header)
        out += [q.raw for q in order]
        runs = []
        for i, q in enumerate(order):
            if runs and runs[-1][0] == q.part and runs[-1][1] + runs[-1][2] == i:
                runs[-1][2] += 1
            else:
                runs.append([q.part, i, 1])
        for name, first, cnt in runs:
            if name:
                out.append("# part %s %d %d" % (name, first, cnt))
        out += list(bones)
        for layer in sorted(skin or {}):
            codes = skin[layer]
            if not codes:
                continue
            n_layer = sum(1 for q in order if q.layer == layer)
            out.append("skin %d %d %d %s" % (layer, n_layer * 4 - len(codes), len(codes), " ".join(str(c) for c in codes)))
        with open(path or self.path, "w", newline="\n") as fh:
            fh.write("\n".join(out) + "\n")


def centroid(q):
    n = len(q.pts)
    return tuple(sum(p[i] for p in q.pts) / n for i in range(3))
