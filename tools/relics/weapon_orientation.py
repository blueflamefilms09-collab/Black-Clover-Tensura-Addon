"""WeaponOrientation (Python reference of client/relics/item/WeaponOrientation.java): the one orientation convention of every weapon mesh,
the measurements that tell which way a mesh points, and the detector that flags a mesh that breaks the convention.

THE CONVENTION (docs/weapon_scale_orientation.md)
  A mesh lives in "mesh space": y runs along the weapon from the butt (grip end, low y) to the tip / head (high y), x across the weapon in the
  item sprite's plane, z through it (thickness). WeaponRenderer lays mesh y on the item sprite's up-right diagonal, so
    * the TIP / HEAD points up-forward, away from the hand, never down and never back at the wielder;
    * mesh -x is FORWARD (the sprite's up-left side, the side the cutting edge of a vanilla axe is on): the cutting edge, the convex side of a
      crescent, the hook of a hook sword, the blade of an axe all face -x, away from the wielder;
    * the flat / back / poll / spine faces +x, toward the wielder;
    * a curved blade bends like its class does: a katana's tip bends toward its edge (-x), a sabre's, scimitar's and dadao's toward its spine (+x).
  Symmetric weapons (a straight sword, a staff, a double-bladed axe, a hammer) have no handedness: only "tip up" applies to them.

THE MEASURES (all in mesh units unless it says blocks; blocks = mesh units * the place scale)
  quads of the BODY layer (layer 0) are cut into BINS slices along y; per slice the area, the area-weighted x, the x and z extents are kept.
  axis_x      the haft's x: area-weighted mean x of the lowest GRIP_BINS slices (where the hand is)
  imbalance   area-weighted mean (x - axis_x) over the slices above HEAD_FROM of the length: the side the heavy part hangs on (blocks);
              < 0 mass toward -x (forward, as it should be for an axe), > 0 toward +x
  centroid    area-weighted y of the mesh as a fraction of its length (0 = butt, 1 = tip): a head-heavy weapon sits above 0.5
  widest_at   where the widest slice lies (0 = butt, 1 = tip): a guard or a blade sits low, an axe head high
  curve       x of the top slice's centre minus axis_x (blocks): the sideways displacement of the tip (a curved blade)

THE VERDICTS (flags): see detect(). A flag is a reason to LOOK (and, for a weapon whose class has a defined handedness, to flip); the
audit sheets (tools/relics/audit_weapon_scale.py) show every flagged mesh.
"""
import math
import os

BINS = 32
GRIP_BINS = 5
TIP_BINS = 2
HEAD_FROM = 0.30                       # slices above this share of the length count as the head / blade region
IMBALANCE_MIN = 0.012                  # blocks: below this a mesh has no side
CURVE_MIN = 0.05                       # blocks: below this a blade is straight
POINTED_WIDEST_MAX = 0.72              # a pointed weapon whose widest part (guard / blade) lies above this share of its length is held by the point
HEAD_HEAVY_MAX = 0.30                  # a head-heavy weapon whose centroid is below this has its head at the butt

FLAG_TIP_DOWN = "tip_down"
FLAG_HEAD_TOWARD_PLAYER = "head_toward_player"
FLAG_MIRRORED = "mirrored"                  # informational: a curved blade bends the wrong way for its class (the edge side of a blade cannot be told from geometry alone)
FLAG_TIP_SUSPECT = "tip_suspect"            # informational: the widest part of a pointed weapon sits near the tip (an ornament, or a blade held by its point)
ACTIONABLE = (FLAG_TIP_DOWN, FLAG_HEAD_TOWARD_PLAYER)     # the flags a fix / the runtime normaliser acts on

# class traits (class name -> set of traits). `handed`: the edge / head is on one side (must be -x); `pointed`: has a point at the tip end;
# `head_heavy`: the head is the heavy end; `curve_edge`: tip bends toward the edge (-x); `curve_spine`: tip bends toward the spine (+x)
TRAITS = {
    "dagger": {"pointed"}, "shortsword": {"pointed"}, "longsword": {"pointed"}, "greatsword": {"pointed"}, "rapier": {"pointed"},
    "katana": {"pointed", "curve_edge"}, "sabre": {"pointed", "curve_spine"}, "dadao": {"pointed", "curve_spine"},
    "handaxe": {"handed", "head_heavy"}, "battleaxe": {"handed", "head_heavy"}, "greataxe": {"handed", "head_heavy"}, "crescent": {"handed", "head_heavy"},
    "scythe": {"handed", "head_heavy"}, "cleaver": {"handed"}, "hook": {"handed"},
    "spear": {"head_heavy"}, "glaive": {"handed", "head_heavy"}, "trident": {"head_heavy"},
    "staff": {"head_heavy"}, "mace": {"handed", "head_heavy"}, "hammer": {"handed", "head_heavy"}, "club": {"head_heavy"}, "wand": set(),
    "whip": set(), "coilwhip": set(), "crossbow": set(), "shield": set(), "item": set(),
}


# ================================================================ mesh files
def read_mesh_file(path):
    """-> dict(place=(ox, oy, angle, scale, ymin) | None, quads=[(layer, [(x, y, z) * 4], [(nx, ny, nz) * 4])], directives={name: [tokens]}, lines=[raw])."""
    place, quads, directives, lines = None, [], {}, []
    with open(path, encoding="utf-8") as f:
        for raw in f:
            lines.append(raw.rstrip("\n"))
            v = raw.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "place":
                place = tuple(float(t) for t in v[1:6])
            elif v[0] == "q":
                nx, ny, nz = float(v[22]), float(v[23]), float(v[24])
                P = [(float(v[2 + k * 5]), float(v[3 + k * 5]), float(v[4 + k * 5])) for k in range(4)]
                quads.append((int(v[1]), P, [(nx, ny, nz)] * 4))
            elif v[0] == "qn":
                P = [(float(v[2 + k * 8]), float(v[3 + k * 8]), float(v[4 + k * 8])) for k in range(4)]
                N = [(float(v[7 + k * 8]), float(v[8 + k * 8]), float(v[9 + k * 8])) for k in range(4)]
                quads.append((int(v[1]), P, N))
            elif v[0] in ("orient", "glow"):
                directives[v[0]] = v[1:]
    return {"place": place, "quads": quads, "directives": directives, "lines": lines}


def body_quads(quads):
    """The layer-0 quads (the glow layer repeats the same geometry; the void layer is not the weapon)."""
    b = [q for q in quads if q[0] == 0]
    return b or [q for q in quads if q[0] != 1] or quads


# ================================================================ measures
def _poly_area(P):
    """Area of a (planar or slightly warped) 3D polygon: half the norm of the summed cross products."""
    cx = cy = cz = 0.0
    n = len(P)
    for k in range(n):
        a, b = P[k], P[(k + 1) % n]
        cx += a[1] * b[2] - a[2] * b[1]
        cy += a[2] * b[0] - a[0] * b[2]
        cz += a[0] * b[1] - a[1] * b[0]
    return 0.5 * math.sqrt(cx * cx + cy * cy + cz * cz)


def _clip_y(P, lo, hi):
    """The polygon P clipped to the slab lo <= y <= hi (Sutherland-Hodgman, linear in 3D)."""
    for keep_ge, bound in ((True, lo), (False, hi)):
        out = []
        n = len(P)
        for k in range(n):
            a, b = P[k], P[(k + 1) % n]
            ina = a[1] >= bound if keep_ge else a[1] <= bound
            inb = b[1] >= bound if keep_ge else b[1] <= bound
            if ina:
                out.append(a)
            if ina != inb and abs(b[1] - a[1]) > 1e-12:
                t = (bound - a[1]) / (b[1] - a[1])
                out.append((a[0] + t * (b[0] - a[0]), bound, a[2] + t * (b[2] - a[2])))
        P = out
        if not P:
            break
    return P


class Measures:
    """What the detector and the audit read of a mesh: see the module doc. All lengths in blocks unless named *_mu (mesh units)."""
    pass


def measure(quads, scale):
    """Measures of the BODY layer of `quads` (output of read_mesh_file) at the place scale `scale` (blocks per mesh unit). Every quad is clipped
    into the BINS slices along y it crosses, so a blade made of a few long quads still has a profile."""
    qs = body_quads(quads)
    m = Measures()
    ys = [p[1] for _, P, _n in qs for p in P]
    if not ys:
        m.empty = True
        return m
    m.empty = False
    y0, y1 = min(ys), max(ys)
    L = max(y1 - y0, 1e-9)
    m.y0, m.y1, m.length_mu, m.length = y0, y1, L, L * scale
    area = [0.0] * BINS
    ax = [0.0] * BINS                                     # area-weighted x
    ay = [0.0] * BINS                                     # area-weighted y
    xmin = [1e30] * BINS
    xmax = [-1e30] * BINS
    zmin = [1e30] * BINS
    zmax = [-1e30] * BINS
    bh = L / BINS
    for _l, P, _n in qs:
        qy0, qy1 = min(p[1] for p in P), max(p[1] for p in P)
        b0 = min(BINS - 1, max(0, int((qy0 - y0) / bh)))
        b1 = min(BINS - 1, max(0, int((qy1 - y0) / bh)))
        for b in range(b0, b1 + 1):
            poly = P if b0 == b1 else _clip_y(list(P), y0 + b * bh, y0 + (b + 1) * bh)
            if len(poly) < 3:
                continue
            a = _poly_area(poly)
            area[b] += a
            ax[b] += a * sum(p[0] for p in poly) / len(poly)
            ay[b] += a * (sum(p[1] for p in poly) / len(poly) - y0)
            for p in poly:
                xmin[b] = min(xmin[b], p[0])
                xmax[b] = max(xmax[b], p[0])
                zmin[b] = min(zmin[b], p[2])
                zmax[b] = max(zmax[b], p[2])
    total = sum(area)
    m.area_mu2, m.area, m.xmin, m.xmax, m.zmin, m.zmax = total, area, xmin, xmax, zmin, zmax
    gx = sum(ax[:GRIP_BINS])
    ga = sum(area[:GRIP_BINS])
    if ga <= 0:                                           # an empty grip end: use every slice
        gx, ga = sum(ax), sum(area)
    m.axis_x = gx / ga if ga > 0 else 0.0
    m.centroid = (sum(ay) / total / L) if total > 0 else 0.5
    hb = int(HEAD_FROM * BINS)
    ha = sum(area[hb:])
    hx = sum(ax[hb:]) - m.axis_x * ha
    m.imbalance = (hx / ha * scale) if ha > 0 else 0.0
    width = [(xmax[b] - xmin[b]) if xmax[b] >= xmin[b] else 0.0 for b in range(BINS)]
    depth = [(zmax[b] - zmin[b]) if zmax[b] >= zmin[b] else 0.0 for b in range(BINS)]
    m.width = [w * scale for w in width]
    m.depth = [d * scale for d in depth]
    m.max_width = max(m.width) if m.width else 0.0
    # where the widest part sits along the weapon (0 = butt, 1 = tip): a guard / a blade low down, an axe head high up
    wide = [b for b in range(BINS) if m.width[b] >= 0.9 * m.max_width] if m.max_width > 0 else [BINS // 2]
    m.widest_at = (sum(wide) / len(wide) + 0.5) / BINS
    m.top_section = max(max(width[BINS - TIP_BINS:]), max(depth[BINS - TIP_BINS:])) * scale
    m.bottom_section = max(max(width[:TIP_BINS]), max(depth[:TIP_BINS])) * scale
    cs = [(xmin[b] + xmax[b]) / 2.0 for b in range(BINS - 3, BINS) if xmax[b] >= xmin[b]]
    m.curve = ((sum(cs) / len(cs) - m.axis_x) * scale) if cs else 0.0
    # the head: slices clearly wider than the shaft (the median width); its height along the weapon and how far it reaches from the haft
    srt = sorted(w for w in m.width if w > 0)
    shaft = srt[len(srt) // 2] if srt else 0.0
    m.shaft_width = shaft
    head = [b for b in range(BINS) if m.width[b] > max(2.0 * shaft, 0.08)]
    m.head_bins = head
    m.head_height = len(head) * L / BINS * scale
    reach_neg = max([(m.axis_x - xmin[b]) for b in range(BINS) if xmax[b] >= xmin[b]] or [0.0]) * scale
    reach_pos = max([(xmax[b] - m.axis_x) for b in range(BINS) if xmax[b] >= xmin[b]] or [0.0]) * scale
    m.reach_neg, m.reach_pos = reach_neg, reach_pos
    m.head_reach = max(reach_neg, reach_pos)
    return m


# ================================================================ the detector
def detect(m, cls=None, place_angle=45.0):
    """Flags of a Measures `m` for a weapon class `cls` (None / unknown: only the class-free checks). Returns a list of FLAG_* strings."""
    flags = []
    if m.empty:
        return flags
    traits = TRAITS.get(cls, set()) if cls else set()
    if cls not in (None, "item", "whip", "coilwhip") and math.sin(math.radians(place_angle)) < -0.1:
        flags.append(FLAG_TIP_DOWN)                        # the mesh's y axis points down on the sprite: the whole weapon is upside down
    elif "head_heavy" in traits and m.centroid < HEAD_HEAVY_MAX:
        flags.append(FLAG_TIP_DOWN)                        # the weight is at the butt: the head is down
    if "pointed" in traits and m.widest_at > POINTED_WIDEST_MAX:
        flags.append(FLAG_TIP_SUSPECT)
    if "handed" in traits and m.imbalance > IMBALANCE_MIN:
        flags.append(FLAG_HEAD_TOWARD_PLAYER)
    if "curve_edge" in traits and m.curve > CURVE_MIN:
        flags.append(FLAG_MIRRORED)
    if "curve_spine" in traits and m.curve < -CURVE_MIN:
        flags.append(FLAG_MIRRORED)
    return flags


# ================================================================ operations on quads (the same maths as WeaponRenderer's orient directive)
def mirror_quads(quads):
    """x -> -x, the normals' x too, and the winding of every quad reversed (v0 v3 v2 v1) so front faces stay front faces."""
    out = []
    for layer, P, N in quads:
        P2 = [(-p[0], p[1], p[2]) for p in P]
        N2 = [(-n[0], n[1], n[2]) for n in N]
        out.append((layer, [P2[0], P2[3], P2[2], P2[1]], [N2[0], N2[3], N2[2], N2[1]], ))
    return out


def rotate_quads_z(quads, deg, pivot_y):
    """Rotates the quads by `deg` about the z axis through (0, pivot_y) (180 turns a tip-down mesh tip-up); winding is kept (a proper rotation)."""
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    out = []
    for layer, P, N in quads:
        P2 = [(p[0] * c - (p[1] - pivot_y) * s, p[0] * s + (p[1] - pivot_y) * c + pivot_y, p[2]) for p in P]
        N2 = [(n[0] * c - n[1] * s, n[0] * s + n[1] * c, n[2]) for n in N]
        out.append((layer, P2, N2))
    return out


def class_defaults_path(root):
    return os.path.join(root, "tools", "relics", "weapon_classes.json")
