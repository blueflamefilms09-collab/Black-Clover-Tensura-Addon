"""Spirit Dive MERGE (4.x): the light meshes of the spirits that merge with the diver, built from the mod's own spirit geo models (Sylph, Undine, Salamander, Gnome).

    python3 -B tools/gen_sdm_models.py            # writes the meshes, the textures and docs/previews/spirit_dive_merge/models.txt
    python3 -B tools/gen_sdm_models.py --view out.png   # also a contact sheet of the meshes (debug, software rasteriser)

Why a generator and not the geo models themselves: the Sylph fairy with wings is 492 quads, the Undine mermaid 762 (SpiritDiveAnimeAssetTest); the merge ghost has a budget of 700 vertices (rule 11), so the
spirit is rebuilt from its LARGEST cubes (volume), in the pose of the form (rest pose for the standard tier; the character tier: the Sylph fairy with spread wings, the Undine mermaid, the Salamander coiled
with its neck raised). Nothing of the geo files changes.

Writes
  src/main/resources/assets/nusmp/sdm_meshes/<element>_<kind>.txt     the meshes (the `q` quad line format of SpiritDiveWind3Mesh, every quad on layer 1 = the additive layer)
  src/main/resources/assets/nusmp/textures/aura/sdm_skin.png         16 x 16 plain light (the meshes are mapped on its middle)
  src/main/resources/assets/nusmp/textures/aura/sdm_trail.png        64 x 16 the light strip (soft across, bright head, fading tail)
  docs/previews/spirit_dive_merge/models.txt                         cubes, quads, vertices and size per mesh

Mesh space: model pixels (1/16 block), origin = the centre of the spirit's bounding box, +y points DOWN (the layer space of the player renderer), the front of the spirit is -z. The mesh is normalised so that
its longest side is NORM px; the painter scales it. Kinds: `std` (standard tier), `char` (character tier), `core` (the faint inner silhouette).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
GEO = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "geo", "entity")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdm_meshes")
TEXDIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "aura")
DOC = os.path.join(ROOT, "docs", "previews", "spirit_dive_merge", "models.txt")

NORM = 32.0                                    # the longest side of every mesh, px
MAX_CUBES = 29                                 # 29 cubes x 24 vertices = 696 vertices: the ghost budget (~700)
CORE_CUBES = 12                                # 12 x 24 = 288 vertices: the inner silhouette (<= ~300)
UV = (0.30, 0.30, 0.70, 0.70)                  # the middle of sdm_skin.png: plain light

# colour of the spirit (the element's dive colour), base RGB
COLOR = {"wind": (140, 255, 196), "water": (100, 184, 255), "fire": (255, 112, 34), "earth": (255, 168, 56)}

# (element, kind) -> (geo files, bone pose additions {bone: (rx, ry, rz)}, bones to leave out, max cubes)
SPEC = {
    ("wind", "std"): (["spirit_sylph"], {}, [], MAX_CUBES),
    ("wind", "char"): (["spirit_sylph_fairy", "spirit_sylph_fairy_wings"], {}, ["sc", "sb", "sa"], MAX_CUBES),
    ("wind", "core"): (["spirit_sylph"], {}, [], CORE_CUBES),
    ("water", "std"): (["spirit_undine_mermaid"], {}, ["L", "ear"], MAX_CUBES),
    ("water", "char"): (["spirit_undine_mermaid"], {}, [], MAX_CUBES),
    ("water", "core"): (["spirit_undine_mermaid"], {}, ["L", "ear", "fluke", "hipfin"], CORE_CUBES),
    ("fire", "std"): (["spirit_salamander"], {}, [], MAX_CUBES),
    ("fire", "char"): (["spirit_salamander"], {"neck1": (-25, 0, 0), "neck2": (-30, 0, 0), "head": (-10, 0, 0), "tail1": (0, 38, 0), "tail2": (0, 46, 0), "tail3": (0, 52, 0),
                                              "tail4": (0, 56, 0), "tailflame": (0, 40, 0)}, [], MAX_CUBES),
    ("fire", "core"): (["spirit_salamander"], {}, [], CORE_CUBES),
    ("earth", "std"): (["spirit_gnome"], {}, [], MAX_CUBES),
    ("earth", "core"): (["spirit_gnome"], {}, [], CORE_CUBES),
}
HAIR_LIMIT = 5                                 # the mermaid's hair has dozens of small strands: only the first few


def rot(rx, ry, rz):
    """File space (y up): R = Rz Ry Rx (the same turn as GeoDraw's rotationZYX(-rz, ry, -rx) in the y-down vanilla space)."""
    a, b, c = math.radians(rx), math.radians(ry), math.radians(rz)
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Rz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def affine(R, pivot):
    """x -> pivot + R (x - pivot) as (R, t)."""
    pivot = np.array(pivot, float)
    return R, pivot - R @ pivot


def compose(parent, child):
    Rp, tp = parent
    Rc, tc = child
    return Rp @ Rc, Rp @ tc + tp


def cubes_of(geo, pose, skip):
    """The cubes of one geo file in the posed state: dicts with centre, half sizes, rotation matrix (columns = the cube's axes), bone name."""
    d = json.load(open(os.path.join(GEO, geo + ".geo.json")))
    bones = {b["name"]: b for b in d["minecraft:geometry"][0]["bones"]}
    world = {}

    def wt(name):
        if name in world:
            return world[name]
        b = bones[name]
        pv = b.get("pivot", [0, 0, 0])
        r = b.get("rotation", [0, 0, 0])
        add = pose.get(name, (0, 0, 0))
        local = affine(rot(r[0] + add[0], r[1] + add[1], r[2] + add[2]), pv)
        parent = b.get("parent")
        world[name] = compose(wt(parent), local) if parent else local
        return world[name]

    out = []
    for name, b in bones.items():
        if any(name.startswith(s) for s in skip):
            continue
        R, t = wt(name)
        for c in b.get("cubes", []):
            o, s = np.array(c["origin"], float), np.array(c["size"], float)
            if s.min() < 0.3 and s.max() < 2.0:
                continue
            infl = float(c.get("inflate", b.get("inflate", 0) or 0))
            o, s = o - infl, s + 2 * infl
            ctr = o + s / 2
            Rc = np.eye(3)
            if "rotation" in c:
                cr = c["rotation"]
                cp = c.get("pivot", b.get("pivot", [0, 0, 0]))
                Rm, tm = affine(rot(*cr), cp)
                ctr = Rm @ ctr + tm
                Rc = Rm
            ctr = R @ ctr + t
            out.append({"c": ctr, "h": s / 2, "R": R @ Rc, "bone": name, "vol": float(np.prod(s))})
    return out


def pick(cubes, n):
    hair = 0
    keep = []
    for cb in sorted(cubes, key=lambda q: -q["vol"]):
        if cb["bone"][0] == "L" and cb["bone"][1:2].isdigit():
            if hair >= HAIR_LIMIT:
                continue
            hair += 1
        keep.append(cb)
        if len(keep) >= n:
            break
    return keep


FACES = [(0, 4, 6, 2), (1, 0, 2, 3), (5, 1, 3, 7), (4, 5, 7, 6), (5, 4, 0, 1), (3, 2, 6, 7)]


def corners(cb):
    pts = []
    for i in range(8):
        sx, sy, sz = (1 if i & 1 else -1), (1 if i & 2 else -1), (1 if i & 4 else -1)
        pts.append(cb["c"] + cb["R"] @ (cb["h"] * np.array([sx, sy, sz])))
    return pts


def build(element, kind):
    files, pose, skip, n = SPEC[(element, kind)]
    cubes = []
    for f in files:
        cubes += cubes_of(f, pose, skip)
    cubes = pick(cubes, n)
    # bounding box of the corners (file space), normalised, then to the layer frame (y down)
    allp = np.array([p for cb in cubes for p in corners(cb)])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    mid, k = (lo + hi) / 2, NORM / float((hi - lo).max())
    base = COLOR[element]
    ymin, ymax = -(hi[1] - mid[1]) * k, -(lo[1] - mid[1]) * k
    quads = []
    for cb in cubes:
        pts = [np.array([(p[0] - mid[0]) * k, -(p[1] - mid[1]) * k, (p[2] - mid[2]) * k]) for p in corners(cb)]
        ctr = sum(pts) / 8
        for f in FACES:
            q = [pts[i] for i in f]
            n = np.cross(q[2] - q[0], q[3] - q[1])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
            n = n / ln
            if np.dot(n, sum(q) / 4 - ctr) < 0:
                q = [q[0], q[3], q[2], q[1]]
                n = -n
            # the light comes from the front (-z) and above (-y in the layer frame): faces turned to it are bright
            lit = max(0.0, np.dot(n, norm_v((0.25, -0.45, -0.85))))
            shade = 0.55 + 0.45 * lit
            tall = (sum(v[1] for v in q) / 4 - ymin) / max(1e-6, ymax - ymin)       # 0 at the top, 1 at the bottom
            g = shade * (1.0 - 0.28 * tall) * (0.5 if cb["bone"].startswith("wing") else 1.0)     # the wing plates are big flat sheets: dimmer, so the spirit itself reads
            rgb = tuple(int(min(255, round(c * g + (255 - c) * 0.07 * lit))) for c in base)
            quads.append((q, n, rgb))
    return quads, len(cubes), (hi - lo) * k


def norm_v(v):
    a = np.array(v, float)
    return a / np.linalg.norm(a)


def text(name, quads):
    u0, v0, u1, v1 = UV
    uvs = [(u0, v1), (u0, v0), (u1, v0), (u1, v1)]
    lines = ["# generated by tools/gen_sdm_models.py (%s): %d quads" % (name, len(quads))]
    for q, n, rgb in quads:
        body = " ".join("%.3f %.3f %.3f %.4f %.4f" % (p[0], p[1], p[2], uvs[i][0], uvs[i][1]) for i, p in enumerate(q))
        lines.append("q 1 %s %.3f %.3f %.3f %d %d %d" % (body, n[0], n[1], n[2], rgb[0], rgb[1], rgb[2]))
    return "\n".join(lines) + "\n"


def textures():
    os.makedirs(TEXDIR, exist_ok=True)
    skin = np.zeros((16, 16, 4), np.uint8)
    skin[:, :, :3] = 255
    skin[:, :, 3] = 255
    Image.fromarray(skin, "RGBA").save(os.path.join(TEXDIR, "sdm_skin.png"))
    w, h = 64, 16
    tr = np.zeros((h, w, 4), np.uint8)
    tr[:, :, :3] = 255
    for y in range(h):
        across = max(0.0, 1.0 - abs((y + 0.5) / h * 2 - 1.0)) ** 1.6                    # v: soft across, bright core
        for x in range(w):
            along = (x + 0.5) / w                                                         # u: 0 at the tail, 1 at the head
            a = across * (along ** 1.4) * (1.0 if along < 0.92 else (1.0 - (along - 0.92) / 0.08 * 0.6))
            tr[y, x, 3] = int(round(255 * min(1.0, a)))
    Image.fromarray(tr, "RGBA").save(os.path.join(TEXDIR, "sdm_trail.png"))


def view(meshes, path):
    """A tiny software rasteriser (painter's algorithm, flat colour, additive) of three views per mesh: debugging only."""
    from PIL import ImageDraw
    cols = 3
    cell = 220
    names = list(meshes)
    sheet = Image.new("RGB", (cell * cols, cell * len(names)), (14, 16, 24))
    dr = ImageDraw.Draw(sheet)
    for r, name in enumerate(names):
        quads = meshes[name]
        for c, yaw in enumerate((0, 50, 140)):
            a = math.radians(yaw)
            ca, sa = math.cos(a), math.sin(a)
            proj = []
            for q, n, rgb in quads:
                pts = [(p[0] * ca + p[2] * sa, p[1], -p[0] * sa + p[2] * ca) for p in q]
                nz = n[0] * -sa + n[2] * ca
                if nz > 0:                                                         # back face (the camera looks along +z in this frame: faces with nz < 0 face it)
                    continue
                proj.append((sum(p[2] for p in pts) / 4, pts, rgb))
            for _, pts, rgb in sorted(proj, key=lambda t: -t[0]):
                poly = [(c * cell + cell / 2 + p[0] * 3.2, r * cell + cell / 2 + p[1] * 3.2) for p in pts]
                dr.polygon(poly, fill=tuple(int(v * 0.8) for v in rgb), outline=(255, 255, 255))
        dr.text((6, r * cell + 4), name, fill=(255, 235, 150))
    sheet.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.dirname(DOC), exist_ok=True)
    meshes, rows = {}, []
    for (element, kind) in SPEC:
        quads, n, size = build(element, kind)
        name = "%s_%s" % (element, kind)
        with open(os.path.join(OUT, name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text(name, quads))
        meshes[name] = quads
        rows.append("%-14s cubes %2d  quads %3d  vertices %3d  size %5.1f x %5.1f x %5.1f px" % (name, n, len(quads), len(quads) * 4, size[0], size[1], size[2]))
    textures()
    with open(DOC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("Spirit Dive MERGE: meshes of tools/gen_sdm_models.py (cubes of the spirit geo files, largest first; layer 1 only = additive)\n" + "\n".join(rows) + "\n")
    print("\n".join(rows))
    if "--view" in sys.argv:
        view(meshes, sys.argv[sys.argv.index("--view") + 1])


if __name__ == "__main__":
    main()
