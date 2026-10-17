#!/usr/bin/env python3
"""
A first-person preview of a Mega-Dungeon template, without the game (needs Pillow and numpy): what a player standing in the room would see.

    python3 tools/labyrinth/mega_walk_preview.py mega/corridor/straight_1x3_a out.png --cam 4,10.6,2.5 --look 4,10.6,20
    python3 tools/labyrinth/mega_walk_preview.py mega/junction/plaza_2x2_a out.png --cam 3.5,10.6,3.5 --look 9,12,9 --fov 85 --size 960x540
    python3 tools/labyrinth/mega_walk_preview.py mega/junction/plaza_2x2_a --views out_prefix      # four standard views from the middle of the room
    python3 tools/labyrinth/mega_walk_preview.py mega/cap/alcove_lantern out.png --cam 1.5,2.6,-1.0 --look 1.5,2.0,5 --lightless

Why: an isometric picture shows the shape of a piece, but not whether it is atmospheric: how the lanterns pool light on the walls, whether a
doorway reads from across the room, whether the vault looks like a vault from the floor. The template is voxelised at a quarter block (stairs,
slabs, trapdoors, panes, chains, torches and candles keep their real thickness, to within that grid), every ray is marched through it, and each
hit is shaded by the block light of the cell it faces (the same flood-fill as `mega_shape_preview.light_field`), so a dark corner looks dark and
a lantern looks like one. Glass, water and cobwebs are see-through; lanterns, torches, fire and glowing blocks are drawn bright. It is a
sketch of the look (flat colours, a block-grid line on every face), not a render of the game's textures.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402
import mega_lib as L  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
from template import load  # noqa: E402

EMISSIVE = {"lantern", "soul_lantern", "torch", "wall_torch", "soul_torch", "soul_wall_torch", "campfire", "soul_campfire", "fire", "soul_fire", "lava",
            "glowstone", "sea_lantern", "shroomlight", "ochre_froglight", "verdant_froglight", "pearlescent_froglight", "jack_o_lantern", "end_rod",
            "magma_block", "crying_obsidian", "beacon", "conduit", "redstone_lamp"}


class Scene:
    """The voxel grid (res voxels per block), a palette and the light field of one template."""

    def __init__(self, t, res=4, light=True):
        self.t = t
        self.res = res
        sx, sy, sz = t.size
        self.dim = (sx * res, sy * res, sz * res)
        self.vox = np.zeros(self.dim, dtype=np.uint16)       # 0 empty, else palette index + 1
        self.palette = [(0, 0, 0, 0, 0)]                      # (r, g, b, alpha, emissive)
        self._index = {}
        self.lf = SP.light_field(t) if light else None
        self.light_on = light
        for (x, y, z), c in t.cells.items():
            n = SP.path_of(c.block)
            if n in ("air", "cave_air", "void_air", "barrier", "structure_void", "light", "structure_block", "jigsaw"):
                continue
            col = SP.color_of(c.block)
            em = 1 if (n in EMISSIVE or (n.endswith("candle") and c.props.get("lit") == "true") or (n in ("furnace", "blast_furnace", "smoker") and c.props.get("lit") == "true")) else 0
            key = (col, em)
            if key not in self._index:
                self.palette.append((col[0], col[1], col[2], col[3], em))
                self._index[key] = len(self.palette) - 1
            idx = self._index[key]
            for b in SP.boxes_for(c.block, c.props):
                self._fill(x, y, z, b, idx)
        for ent in t.entities:
            try:
                ex, ey, ez = [float(v) for v in ent["pos"]]
            except Exception:  # noqa: BLE001
                continue
            for b in SP.entity_boxes(ent):
                col = b[6]
                key = (col, 0)
                if key not in self._index:
                    self.palette.append((col[0], col[1], col[2], col[3], 0))
                    self._index[key] = len(self.palette) - 1
                self._fill_abs(b[:6], self._index[key])
        self.pal = np.array(self.palette, dtype=np.float32)

    def _fill(self, x, y, z, b, idx):
        self._fill_abs((x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]), idx)

    def _fill_abs(self, b, idx):
        r = self.res
        lo = []
        hi = []
        for a0, a1 in ((b[0], b[3]), (b[1], b[4]), (b[2], b[5])):
            i0 = int(math.floor(a0 * r + 1e-6))
            i1 = int(math.floor(a1 * r - 1e-6)) + 1
            if i1 - i0 < 1:
                i1 = i0 + 1
            if i1 - i0 == 1 and (a1 - a0) < 1.0 / r:   # a thin box: centre it on its own middle voxel
                m = int(math.floor((a0 + a1) * 0.5 * r))
                i0, i1 = m, m + 1
            lo.append(max(0, i0))
            hi.append(min(self.dim[len(lo) - 1], i1))
        if all(h > l for l, h in zip(lo, hi)):
            self.vox[lo[0]:hi[0], lo[1]:hi[1], lo[2]:hi[2]] = idx

    def light_at(self, cell):
        if not self.light_on:
            return 15
        return self.lf.get(cell, 0)


def render(scene, cam, look, fov=80.0, size=(640, 360), up=(0, 1, 0), max_steps=900, gamma=1.5, floor_light=0.07, ambient=0.0):
    """Ray-marches `scene` from `cam` toward `look` and returns a PIL image."""
    W, H = size
    res = scene.res
    dim = np.array(scene.dim)
    cam = np.array(cam, dtype=np.float64)
    f = np.array(look, dtype=np.float64) - cam
    f /= np.linalg.norm(f)
    r = np.cross(f, np.array(up, dtype=np.float64))
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    tanh = math.tan(math.radians(fov) / 2.0)
    aspect = W / float(H)
    xs = (2.0 * (np.arange(W) + 0.5) / W - 1.0) * tanh * aspect
    ys = (1.0 - 2.0 * (np.arange(H) + 0.5) / H) * tanh
    gx, gy = np.meshgrid(xs, ys)
    dirs = f[None, None, :] + gx[..., None] * r[None, None, :] + gy[..., None] * u[None, None, :]
    dirs = dirs.reshape(-1, 3)
    dirs /= np.linalg.norm(dirs, axis=1)[:, None]
    n = dirs.shape[0]
    origin = cam * res                                   # voxel units
    pos = np.tile(origin, (n, 1))
    ix = np.floor(pos).astype(np.int64)
    with np.errstate(divide="ignore", invalid="ignore"):
        inv = 1.0 / dirs
        step = np.where(dirs >= 0, 1, -1).astype(np.int64)
        tdelta = np.abs(inv)
        nxt = np.where(dirs >= 0, ix + 1, ix).astype(np.float64)
        tmax = (nxt - origin[None, :]) * inv
    tmax = np.where(np.isfinite(tmax), tmax, np.inf)
    tdelta = np.where(np.isfinite(tdelta), tdelta, np.inf)
    colour = np.zeros((n, 3), dtype=np.float64)
    trans = np.ones(n, dtype=np.float64)
    active = np.arange(n)
    pal = scene.pal
    vox = scene.vox
    for _ in range(max_steps):
        if active.size == 0:
            break
        tm = tmax[active]
        axis = np.argmin(tm, axis=1)
        tcur = tm[np.arange(active.size), axis]
        ix_a = ix[active]
        st_a = step[active]
        ar = np.arange(active.size)
        ix_a[ar, axis] += st_a[ar, axis]
        ix[active] = ix_a
        tmax[active, axis] += tdelta[active, axis]
        inb = np.all((ix_a >= 0) & (ix_a < dim[None, :]), axis=1)
        gone = ~inb
        # rays that left the grid: add the background (black) and drop them
        idxs = np.zeros(active.size, dtype=np.uint16)
        if inb.any():
            sel = ix_a[inb]
            idxs[inb] = vox[sel[:, 0], sel[:, 1], sel[:, 2]]
        hit = idxs > 0
        if hit.any():
            h = np.nonzero(hit)[0]
            rays = active[h]
            p = pal[idxs[h].astype(np.int64)]
            alpha = p[:, 3] / 255.0
            emis = p[:, 4]
            ax_h = axis[h]
            nrm_sign = -st_a[h, ax_h]
            hp = (origin[None, :] + dirs[rays] * tcur[h, None]) / res   # hit point in block units
            cell = np.floor(ix_a[h] / res).astype(np.int64)
            # the cell the ray came from (one voxel back along the stepped axis), for the light
            back = ix_a[h].copy()
            back[np.arange(h.size), ax_h] -= st_a[h, ax_h]
            bcell = np.floor(back / res).astype(np.int64)
            lv = np.empty(h.size)
            for i in range(h.size):
                lv[i] = scene.light_at((int(bcell[i, 0]), int(bcell[i, 1]), int(bcell[i, 2])))
            bright = floor_light + (1.0 - floor_light) * (lv / 15.0) ** gamma + ambient
            bright = np.where(emis > 0, 1.0, bright)
            shade = np.where(ax_h == 1, np.where(nrm_sign > 0, 1.0, 0.55), np.where(ax_h == 0, 0.8, 0.9))
            # block-grid line: darken near the edges of the block face that was hit
            frac = hp - np.floor(hp + 1e-9)
            edge = np.zeros(h.size, dtype=bool)
            for a in (0, 1, 2):
                m = ax_h != a
                fa = np.minimum(frac[:, a], 1.0 - frac[:, a])
                edge |= m & (fa < 0.045)
            grid = np.where(edge, 0.80, 1.0)
            hashv = ((cell[:, 0] * 73856093) ^ (cell[:, 1] * 19349663) ^ (cell[:, 2] * 83492791)) & 255
            noise = 0.94 + 0.12 * (hashv / 255.0)
            c = p[:, :3] / 255.0 * (bright * shade * grid * noise)[:, None]
            colour[rays] += (trans[rays] * alpha)[:, None] * c
            trans[rays] *= (1.0 - alpha)
        done_mask = gone | (hit & (trans[active] < 0.02))
        if hit.any():
            opaque_hit = np.zeros(active.size, dtype=bool)
            opaque_hit[hit] = pal[idxs[hit].astype(np.int64)][:, 3] >= 255
            done_mask |= opaque_hit
        active = active[~done_mask]
    img = np.clip(colour.reshape(H, W, 3) * 255.0, 0, 255).astype(np.uint8)
    return Image.fromarray(img, "RGB")


def parse3(s):
    return tuple(float(v) for v in s.split(","))


def standard_views(t):
    """Four views from the middle of the interior toward N, E, S, W, at eye height (a rough default: the interior is the box inside the 2-thick walls)."""
    sx, sy, sz = t.size
    cx, cz = sx / 2.0, sz / 2.0
    eye = L.FLOOR_ROW + 1 + 1.6
    return [("n", (cx, eye, cz), (cx, eye, 0.0)), ("e", (cx, eye, cz), (float(sx), eye, cz)),
            ("s", (cx, eye, cz), (cx, eye, float(sz))), ("w", (cx, eye, cz), (0.0, eye, cz))]


def main(argv):
    args = list(argv)
    if not args or "-h" in args or "--help" in args:
        print(__doc__)
        return 0
    opts = {}
    for key in ("--cam", "--look", "--fov", "--size", "--res", "--root", "--views", "--gamma"):
        if key in args:
            i = args.index(key)
            opts[key] = args[i + 1]
            del args[i:i + 2]
    flags = {f for f in ("--lightless",) if f in args}
    args = [a for a in args if a not in flags]
    name = args[0]
    path = os.path.join(opts.get("--root", L.RES), "data", "nusmp", "structure", name + ".nbt")
    t = load(path, name)
    res = int(opts.get("--res", 4))
    w, h = (int(v) for v in opts.get("--size", "640x360").split("x"))
    scene = Scene(t, res=res, light="--lightless" not in flags)
    fov = float(opts.get("--fov", 80))
    gamma = float(opts.get("--gamma", 1.5))
    if "--views" in opts:
        prefix = opts["--views"]
        for tag, cam, look in standard_views(t):
            img = render(scene, cam, look, fov, (w, h), gamma=gamma)
            img.save("%s_%s.png" % (prefix, tag))
            print("wrote %s_%s.png" % (prefix, tag))
        return 0
    img = render(scene, parse3(opts["--cam"]), parse3(opts["--look"]), fov, (w, h), gamma=gamma)
    img.save(args[1])
    print("wrote", args[1])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
