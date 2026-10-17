#!/usr/bin/env python3
"""
A first-person preview of a Mega-Dungeon template, without the game (needs numpy and Pillow): the substitute for walking into the room.

    python3 tools/labyrinth/mega_vault_view.py mega/vault/three_a out.png --cam 13,11,23.5 --yaw 180 --pitch 8
    python3 tools/labyrinth/mega_vault_view.py mega/vault/three_a outdir --views            # the standard set of views, one PNG each
    python3 tools/labyrinth/mega_vault_view.py mega/vault/three_a out.png --cam 13,40,13 --pitch 90 --cut-y 22 --fov 70   # a plan from above, roof cut off

Why: `mega_preview.py` shows the outside of a template as an isometric block picture, which cannot show what a person standing in the doorway sees, how bright
the floor is, whether the pillars frame the carpet or whether the dome is a dome. This renderer casts one ray per pixel through a grid four times finer than the
blocks, so slabs, stairs, walls, bars, chains, lightning rods, lanterns, candles, skulls, carpets and the pedestal keep their real proportions; colours come from a
keyword table, light from the same flood fill as the vault checks (the block light in front of every face, the vanilla rule), emitters glow. It is a judge of
shape, symmetry, light and clutter, not of texture. Camera yaw is Minecraft's: 0 looks south (+z), 90 west, 180 north, 270 east.
"""
import math
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_vault_kit as K  # noqa: E402
from template import load  # noqa: E402

S = 4                                                         # sub-voxels per block along each axis

COLORS = [  # (regex on the block path, rgb, emissive)
    (r"soul_lantern|soul_campfire|soul_fire|soul_wall_torch|soul_torch", (110, 205, 255), True),
    (r"lantern|campfire|torch|wall_torch|fire|shroomlight|glowstone|sea_lantern|magma_block|lava|jack_o_lantern", (255, 190, 90), True),
    (r"end_rod", (245, 245, 255), True),
    (r".*purple_stained_glass.*", (150, 80, 215), True),
    (r".*blue_stained_glass.*", (80, 110, 225), True),
    (r".*red_stained_glass.*", (210, 70, 70), True),
    (r".*candle", (235, 230, 205), True),
    (r"gold_block", (235, 190, 55), False),
    (r".*red_carpet", (150, 22, 24), False),
    (r".*_carpet", (170, 170, 170), False),
    (r".*red_wall_banner|.*red_banner", (160, 30, 30), False),
    (r".*banner", (150, 150, 160), False),
    (r"chiseled_deepslate", (92, 94, 106), False),
    (r"polished_deepslate.*", (86, 88, 100), False),
    (r"deepslate_tile.*", (58, 60, 68), False),
    (r"cracked_deepslate.*", (60, 61, 68), False),
    (r"deepslate_brick.*", (68, 70, 80), False),
    (r"cobbled_deepslate.*", (64, 66, 72), False),
    (r"deepslate", (55, 56, 62), False),
    (r".*iron_bars|iron_trapdoor", (150, 152, 162), False),
    (r"chain", (60, 62, 74), False),
    (r".*skull|.*head|bone_block", (226, 222, 200), False),
    (r"lightning_rod", (200, 118, 76), False),
    (r".*copper.*", (190, 110, 70), False),
    (r"pointed_dripstone|dripstone_block", (118, 96, 80), False),
    (r"cobweb", (225, 225, 225), False),
    (r"nusmp:relic_pedestal|relic_pedestal", (40, 38, 50), False),
    (r".*dark_oak.*|.*planks.*|.*log.*", (74, 54, 36), False),
    (r".*quartz.*|calcite|.*diorite.*|snow_block|.*white.*", (226, 224, 218), False),
    (r".*blackstone.*|.*basalt.*|obsidian", (44, 40, 52), False),
    (r".*nether_brick.*|netherrack|.*crimson.*", (110, 40, 42), False),
    (r".*stone_brick.*|stone|smooth_stone|cobblestone", (118, 118, 122), False),
    (r".*", (130, 120, 110), False),
]
_COMPILED = [(re.compile(p), c, e) for p, c, e in COLORS]


def colour_of(block):
    path = block.split(":", 1)[1] if ":" in block else block
    for rx, c, e in _COMPILED:
        if rx.fullmatch(path) or rx.fullmatch(block):
            return c, e
    return (130, 120, 110), False


# ------------------------------------------------------------------ shapes: a block as a 4x4x4 boolean mask [x][y][z]
def _empty():
    return np.zeros((S, S, S), dtype=bool)


def _box(m, x1, y1, z1, x2, y2, z2):
    m[x1:x2, y1:y2, z1:z2] = True


_FACE_VEC = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


def shape_of(block, props):
    """The sub-voxel mask of a block state, or None for 'a full cube' (the common case, no allocation) or False for 'draw nothing'."""
    p = block.split(":", 1)[1] if ":" in block else block
    if p in ("air", "cave_air", "void_air", "structure_block", "jigsaw", "light", "barrier"):
        return False
    m = _empty()
    if p.endswith("_slab"):
        t = props.get("type", "bottom")
        if t == "double":
            return None
        _box(m, 0, 0 if t == "bottom" else 2, 0, S, 2 if t == "bottom" else S, S)
        return m
    if p.endswith("_stairs"):
        half, facing = props.get("half", "bottom"), props.get("facing", "north")
        lo = (0, 2) if half == "bottom" else (2, 4)
        hi = (2, 4) if half == "bottom" else (0, 2)
        _box(m, 0, lo[0], 0, S, lo[1], S)
        fx, fz = _FACE_VEC.get(facing, (0, -1))
        xs = (2, 4) if fx > 0 else (0, 2) if fx < 0 else (0, 4)
        zs = (2, 4) if fz > 0 else (0, 2) if fz < 0 else (0, 4)
        _box(m, xs[0], hi[0], zs[0], xs[1], hi[1], zs[1])
        return m
    if p.endswith("_wall") and not p.endswith("_wall_banner") and not p.endswith("_wall_skull") and not p.endswith("_wall_head") and not p.endswith("_wall_sign"):
        if props.get("up", "true") == "true":
            _box(m, 1, 0, 1, 3, 4, 3)
        for d, (dx, dz) in _FACE_VEC.items():
            v = props.get(d, "none")
            if v in ("low", "tall"):
                h = 3 if v == "low" else 4
                xs = (3, 4) if dx > 0 else (0, 1) if dx < 0 else (1, 3)
                zs = (3, 4) if dz > 0 else (0, 1) if dz < 0 else (1, 3)
                _box(m, xs[0], 0, zs[0], xs[1], h, zs[1])
        if not m.any():
            _box(m, 1, 0, 1, 3, 4, 3)
        return m
    if p.endswith("_fence") or p.endswith("iron_bars") or p.endswith("glass_pane"):
        _box(m, 1, 0, 1, 3, 4, 3) if p.endswith("_fence") else _box(m, 1, 0, 1, 3, 4, 3)
        for d, (dx, dz) in _FACE_VEC.items():
            if props.get(d) == "true":
                xs = (3, 4) if dx > 0 else (0, 1) if dx < 0 else (1, 3)
                zs = (3, 4) if dz > 0 else (0, 1) if dz < 0 else (1, 3)
                _box(m, xs[0], 0, zs[0], xs[1], 4, zs[1])
        return m
    if p == "chain":
        ax = props.get("axis", "y")
        if ax == "y":
            _box(m, 1, 0, 1, 3, 4, 3) if False else _box(m, 1, 0, 1, 2, 4, 2)
        elif ax == "x":
            _box(m, 0, 1, 1, 4, 2, 2)
        else:
            _box(m, 1, 1, 0, 2, 2, 4)
        return m
    if p == "lightning_rod":
        f = props.get("facing", "up")
        if f in ("up", "down"):
            _box(m, 1, 0, 1, 2, 4, 2)
        elif f in ("north", "south"):
            _box(m, 1, 1, 0, 2, 2, 4)
        else:
            _box(m, 0, 1, 1, 4, 2, 2)
        return m
    if p in ("lantern", "soul_lantern"):
        if props.get("hanging") == "true":
            _box(m, 1, 1, 1, 3, 3, 3)
            _box(m, 1, 3, 1, 2, 4, 2)
        else:
            _box(m, 1, 0, 1, 3, 2, 3)
            _box(m, 1, 2, 1, 2, 3, 2)
        return m
    if p.endswith("candle"):
        n = int(props.get("candles", "1"))
        for i, (cx, cz) in enumerate(((1, 1), (2, 2), (2, 1), (1, 2))[:n]):
            _box(m, cx, 0, cz, cx + 1, 2, cz + 1)
        return m
    if p in ("torch", "soul_torch"):
        _box(m, 1, 0, 1, 2, 3, 2)
        return m
    if p in ("wall_torch", "soul_wall_torch"):
        fx, fz = _FACE_VEC.get(props.get("facing", "north"), (0, -1))
        cx, cz = (1 + fx * -1, 1 + fz * -1) if False else (1 - fx if fx else 1, 1 - fz if fz else 1)
        _box(m, cx, 1, cz, cx + 1, 3, cz + 1)
        return m
    if p.endswith("_carpet"):
        _box(m, 0, 0, 0, S, 1, S)
        return m
    if p.endswith("_skull") and not p.endswith("_wall_skull"):
        _box(m, 1, 0, 1, 3, 2, 3)
        return m
    if p.endswith("_wall_skull"):
        fx, fz = _FACE_VEC.get(props.get("facing", "north"), (0, -1))
        cx, cz = (1 - fx if fx else 1, 1 - fz if fz else 1)
        _box(m, cx, 1, cz, cx + 2 if fx == 0 else cx + 1, 3, cz + 2 if fz == 0 else cz + 1)
        return m
    if p.endswith("_wall_banner") or p.endswith("_banner"):
        if p.endswith("_wall_banner"):
            fx, fz = _FACE_VEC.get(props.get("facing", "north"), (0, -1))
            xs = (3, 4) if fx < 0 else (0, 1) if fx > 0 else (0, 4)
            zs = (3, 4) if fz < 0 else (0, 1) if fz > 0 else (0, 4)
            _box(m, xs[0], 0, zs[0], xs[1], 4, zs[1])
        else:
            _box(m, 1, 0, 1, 3, 4, 2)
        return m
    if p == "pointed_dripstone":
        th = props.get("thickness", "tip")
        w = {"base": 4, "middle": 3, "frustum": 2, "tip": 1, "tip_merge": 1}.get(th, 2)
        o = (S - w) // 2
        _box(m, o, 0, o, o + w, 4, o + w)
        return m
    if p.endswith("_trapdoor"):
        _box(m, 0, 0, 0, S, 1, S) if props.get("half", "bottom") == "bottom" and props.get("open", "false") == "false" else _box(m, 0, 0, 0, S, 1, S)
        return m
    if p == "relic_pedestal":
        _box(m, 0, 0, 0, S, 1, S)
        _box(m, 1, 1, 1, 3, 3, 3)
        _box(m, 0, 3, 0, S, 4, S)
        return m
    if p in ("campfire", "soul_campfire"):
        _box(m, 0, 0, 0, S, 2, S)
        return m
    if p == "cauldron" or p.endswith("_cauldron"):
        _box(m, 0, 0, 0, S, 3, S)
        return m
    if p.endswith("cobweb"):
        _box(m, 1, 1, 1, 3, 3, 3)
        return m
    return None


# ------------------------------------------------------------------ the scene
class Scene:
    def __init__(self, t, cut_y=None):
        self.t = t
        sx, sy, sz = t.size
        self.size = (sx, sy, sz)
        occ = np.zeros((sx * S, sy * S, sz * S), dtype=bool)
        col = np.zeros((sx * S, sy * S, sz * S, 3), dtype=np.uint8)
        emi = np.zeros((sx * S, sy * S, sz * S), dtype=bool)
        for (x, y, z), c in t.cells.items():
            if cut_y is not None and y > cut_y:
                continue
            m = shape_of(c.block, c.props)
            if m is False:
                continue
            rgb, e = colour_of(c.block)
            sl = (slice(x * S, x * S + S), slice(y * S, y * S + S), slice(z * S, z * S + S))
            if m is None:
                occ[sl] = True
                col[sl] = rgb
                emi[sl] = e
            else:
                occ[sl] |= m
                sub = col[sl]
                sub[m] = rgb
                col[sl] = sub
                esub = emi[sl]
                esub[m] = e
                emi[sl] = esub
        self.occ, self.col, self.emi = occ, col, emi
        # entities: armor stands as little figures
        for e in t.entities:
            try:
                nbt_ = e.get("nbt", {})
                if str(nbt_.get("id", "")) != "minecraft:armor_stand":
                    continue
                px, py, pz = [float(v) for v in e["pos"]]
                ix, iy, iz = int(px * S), int(py * S), int(pz * S)
                for dy in range(0, 7):
                    for dx in range(-1, 1 if dy not in (3, 4, 5) else 2):
                        for dz in (-1, 0):
                            if 0 <= ix + dx < occ.shape[0] and 0 <= iy + dy < occ.shape[1] and 0 <= iz + dz < occ.shape[2]:
                                occ[ix + dx, iy + dy, iz + dz] = True
                                col[ix + dx, iy + dy, iz + dz] = (120, 120, 130) if dy > 1 else (90, 90, 100)
            except Exception:  # noqa: BLE001
                continue
        self.light = K.light_map(t)

    def light_at(self, cell):
        return self.light.get(cell, 0)


def render(scene, cam, yaw, pitch, fov, width, height, fog=0.012, max_steps=900, exposure=1.0, ambient=0.16):
    occ, col, emi = scene.occ, scene.col, scene.emi
    shape = np.array(occ.shape)
    aspect = width / height
    xs = (np.arange(width) + 0.5) / width * 2 - 1
    ys = 1 - (np.arange(height) + 0.5) / height * 2
    tanf = math.tan(math.radians(fov) / 2)
    px, py = np.meshgrid(xs * tanf * aspect, ys * tanf)
    yawr, pitr = math.radians(yaw), math.radians(pitch)
    # camera basis (Minecraft: yaw 0 looks +z, positive yaw turns to -x; pitch positive looks down)
    fwd = np.array([-math.sin(yawr) * math.cos(pitr), -math.sin(pitr), math.cos(yawr) * math.cos(pitr)])
    right = np.array([-math.cos(yawr), 0.0, -math.sin(yawr)])
    up = np.cross(right, fwd)
    up = up / np.linalg.norm(up)
    d = fwd[None, None, :] + px[..., None] * right[None, None, :] + py[..., None] * up[None, None, :]
    d = d / np.linalg.norm(d, axis=2, keepdims=True)
    d = d.reshape(-1, 3)
    n = d.shape[0]
    o = np.array(cam, dtype=float) * S
    d = np.where(np.abs(d) < 1e-9, 1e-9, d)
    cell = np.floor(o).astype(np.int64)[None, :].repeat(n, 0)
    step = np.sign(d).astype(np.int64)
    tdelta = np.abs(1.0 / d)
    tmax = np.where(d > 0, (cell + 1 - o) / d, (cell - o) / d)
    img = np.zeros((n, 3), dtype=float)
    active = np.arange(n)
    hit_axis = np.zeros(n, dtype=np.int64)
    hit = np.zeros(n, dtype=bool)
    hit_cell = np.zeros((n, 3), dtype=np.int64)
    prev_cell = np.zeros((n, 3), dtype=np.int64)
    tdist = np.zeros(n)
    for _ in range(max_steps):
        if active.size == 0:
            break
        tm = tmax[active]
        axis = np.argmin(tm, axis=1)
        idx = np.arange(active.size)
        t_now = tm[idx, axis]
        prev_cell[active] = cell[active]
        cell[active, axis] += step[active, axis]
        tmax[active, axis] += tdelta[active, axis]
        c = cell[active]
        inside = np.all((c >= 0) & (c < shape[None, :]), axis=1)
        occupied = np.zeros(active.size, dtype=bool)
        ci = c[inside]
        occupied[inside] = occ[ci[:, 0], ci[:, 1], ci[:, 2]]
        newly = active[occupied]
        hit[newly] = True
        hit_axis[newly] = axis[occupied]
        hit_cell[newly] = c[occupied]
        tdist[newly] = t_now[occupied]
        keep = inside & ~occupied
        active = active[keep]
    # shading
    hs = np.where(hit)[0]
    out = np.zeros((n, 3))
    sky = np.array([14.0, 14.0, 20.0])
    out[:] = sky
    if hs.size:
        hc = hit_cell[hs]
        base = col[hc[:, 0], hc[:, 1], hc[:, 2]].astype(float)
        glow = emi[hc[:, 0], hc[:, 1], hc[:, 2]]
        ax = hit_axis[hs]
        pc = prev_cell[hs]
        # the block the ray came from (the air in front of the face) and its light
        bx, by, bz = pc[:, 0] // S, pc[:, 1] // S, pc[:, 2] // S
        lv = np.zeros(hs.size)
        light = scene.light
        cache = {}
        for i in range(hs.size):
            k = (int(bx[i]), int(by[i]), int(bz[i]))
            v = cache.get(k)
            if v is None:
                v = light.get(k, 0)
                cache[k] = v
            lv[i] = v
        bright = ambient + (1.0 - ambient) * (lv / 15.0) ** 1.35
        side = np.where(ax == 1, np.where(step[hs, 1] < 0, 1.0, 0.55), np.where(ax == 0, 0.82, 0.70))
        h = ((hc[:, 0] * 73856093) ^ (hc[:, 1] * 19349663) ^ (hc[:, 2] * 83492791)) & 0xFF
        noise = 0.93 + 0.14 * (h / 255.0)
        shade = (bright * side * noise * exposure)[:, None]
        lit = base * shade
        lit = np.where(glow[:, None], np.minimum(255.0, base * 1.15 + 25.0), lit)
        fogf = np.exp(-tdist[hs] / S * fog)[:, None]
        out[hs] = lit * fogf + sky * (1 - fogf)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8).reshape(height, width, 3), "RGB")


VIEWS = {
    # name: (cam x,y,z, yaw, pitch, fov, share): the camera is made for a template 27 deep; `share` of the extra depth of a longer one is added to its z
    # (1.0 for a view from the door end, 0.5 for one from the middle, 0 for one at the far end)
    "entry": ((13.5, 10.6, 23.5), 180, 4, 85, 1.0),
    "entry_low": ((13.5, 10.6, 24.8), 180, -2, 95, 1.0),
    "center": ((13.5, 10.6, 14.5), 180, 6, 95, 0.5),
    "apse": ((13.5, 10.6, 12.5), 180, 14, 100, 0.0),
    "west": ((13.5, 10.6, 13.5), 90, 6, 100, 0.5),
    "dome": ((13.5, 10.6, 20.5), 180, -34, 95, 1.0),
    "back": ((13.5, 10.6, 7.5), 0, 4, 90, 0.0),
}

# the nave (`*_b`) is a long hall with a raised chancel at the far end, so its views are placed from the depth: (x, y, z as a function of the depth d)
NAVE_VIEWS = {
    "entry": ((13.5, 10.6, lambda d: d - 2.5), 180, 2, 85),
    "mid": ((13.5, 10.6, lambda d: d * 0.55), 180, -4, 90),
    "chancel": ((13.5, 12.5, lambda d: 10.5), 180, 2, 90),
    "from_chancel": ((13.5, 12.6, lambda d: 6.5), 0, 3, 95),
    "aisle": ((4.5, 10.6, lambda d: d - 6.0), 180, 2, 90),
    "west_wall": ((14.5, 10.6, lambda d: d * 0.5), 90, 4, 100),
    "east_wall": ((12.5, 10.6, lambda d: d * 0.5), 270, 4, 100),
    "dome": ((13.5, 10.6, lambda d: d - 3.5), 180, -34, 95),
    "pedestals": ((13.5, 12.6, lambda d: 9.0), 180, -2, 110),
}


def main(argv):
    args = list(argv)

    def opt(name, default=None):
        if name in args:
            i = args.index(name)
            v = args[i + 1]
            del args[i:i + 2]
            return v
        return default

    cam = opt("--cam")
    yaw = float(opt("--yaw", "180"))
    pitch = float(opt("--pitch", "0"))
    fov = float(opt("--fov", "90"))
    size = opt("--size", "800x500")
    cut = opt("--cut-y")
    views = "--views" in args
    if views:
        args.remove("--views")
    name, out = args[0], args[1]
    width, height = [int(v) for v in size.split("x")]
    t = load(os.path.join(L.RES, "data", "nusmp", "structure", name + ".nbt"), name)
    scene = Scene(t, cut_y=int(cut) if cut else None)
    if views:
        os.makedirs(out, exist_ok=True)
        base = name.split("/")[-1]
        if base.endswith("_b"):
            for vn, ((cx, cy, cz), y, p, f) in NAVE_VIEWS.items():
                img = render(scene, (cx, cy, cz(t.size[2])), y, p, f, width, height)
                img.save(os.path.join(out, "%s_%s.png" % (base, vn)))
                print("wrote", os.path.join(out, "%s_%s.png" % (base, vn)))
        else:
            extra = t.size[2] - 27
            for vn, (c, y, p, f, share) in VIEWS.items():
                img = render(scene, (c[0], c[1], c[2] + share * extra), y, p, f, width, height)
                img.save(os.path.join(out, "%s_%s.png" % (base, vn)))
                print("wrote", os.path.join(out, "%s_%s.png" % (base, vn)))
        return 0
    c = [float(v) for v in cam.split(",")] if cam else [13.5, 10.6, 23.5]
    render(scene, c, yaw, pitch, fov, width, height).save(out)
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
