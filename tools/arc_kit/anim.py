"""Why: the same clip names (idle walk run attack cast cast_big hurt transform death, and optionally chest_gem_reveal and spawn) must exist on every model and must not clip
a limb through the body, and nobody should key forty bones by hand per character. This module is the clip machinery: tracks sampled on a fixed grid (so everything can
be post-processed), inverse kinematics for the arms (a swing is written as where the hand goes and where the blade points; the shoulder, elbow and wrist angles are solved,
and the second hand of a two-handed weapon is solved onto the shaft), follow-through for hair, cloth, tails and wings (a low-pass lag of the body's motion), the foot
grounding (the lowest foot always touches the floor) and the clearance fixes (an arm never enters the torso, a cape never enters a leg). clips.py holds the recipes.

Signs of this frame: positive x swings a hanging limb forward (an arm raises forward), negative x folds a knee and leans the body forward; positive z swings a hanging
limb towards +x (the model's left), so a RIGHT limb abducts with negative z; positive y turns a part towards the model's right.
"""
import math

import numpy as np

from .core import TAU, catmull
from .rig import rot_matrix, rot_file

GRID = 24                      # samples per second of every track before simplification
SIN, COS = math.sin, math.cos


class Clip:
    """One clip being composed: tracks of rotation (degrees), position (pixels) and scale per bone, each an array of GRID samples per second (plus the end)."""

    def __init__(self, name, length, loop):
        self.name, self.length, self.loop = name, float(length), bool(loop)
        self.n = max(2, int(round(length * GRID)))
        self.ts = np.arange(self.n + 1) / float(GRID) * (length / (self.n / float(GRID)))
        self.rot, self.pos, self.scale = {}, {}, {}

    def _store(self, kind):
        return {"rot": self.rot, "pos": self.pos, "scale": self.scale}[kind]

    def get(self, bone, kind):
        st = self._store(kind)
        if bone not in st:
            st[bone] = np.tile(np.array([1.0, 1.0, 1.0] if kind == "scale" else [0.0, 0.0, 0.0]), (self.n + 1, 1))
        return st[bone]

    def add(self, bone, kind, arr):
        a = self.get(bone, kind)
        if kind == "scale":
            a *= np.asarray(arr, float)
        else:
            a += np.asarray(arr, float)

    def put(self, bone, kind, arr):
        self.get(bone, kind)
        self._store(kind)[bone] = np.array(arr, float).reshape(self.n + 1, 3).copy()

    def theta(self, n=1):
        """The phase of a loop (0 .. n turns) at every sample."""
        return TAU * n * self.ts / self.length

    def fn(self, bone, kind, f, add=True):
        """f(t seconds, theta) -> (x, y, z) sampled at every grid time."""
        vals = np.array([f(float(t), TAU * float(t) / self.length) for t in self.ts], float)
        (self.add if add else self.put)(bone, kind, vals)

    def keys(self, bone, kind, pts, add=True, smooth=True):
        """Keyframes [(t, (x, y, z))] sampled with a Catmull-Rom spline (held before the first and after the last)."""
        pts = sorted((float(t), tuple(float(x) for x in v)) for t, v in pts)
        if len(pts) == 1:
            pts = [(0.0, pts[0][1]), (self.length, pts[0][1])]
        if smooth and len(pts) > 2:
            vals = np.array([catmull(pts, float(t)) for t in self.ts], float)
        else:
            vals = np.array([_lin(pts, float(t)) for t in self.ts], float)
        if kind == "scale":
            vals = np.maximum(vals, 0.01)
        (self.add if add else self.put)(bone, kind, vals)

    def has(self, bone, kind="rot"):
        return bone in self._store(kind)

    def slice(self, t0, t1, name=None):
        """The part of this clip from t0 to t1 seconds (both on the sample grid) as a clip of its own (a one-shot): the two halves of a transformation that swaps models."""
        i0, i1 = int(round(t0 * GRID)), int(round(t1 * GRID))
        out = Clip(name or self.name, (i1 - i0) / float(GRID), False)
        for kind in ("rot", "pos", "scale"):
            for bone, arr in self._store(kind).items():
                out._store(kind)[bone] = arr[i0:i1 + 1].copy()
        return out


def _lin(pts, t):
    if t <= pts[0][0]:
        return pts[0][1]
    if t >= pts[-1][0]:
        return pts[-1][1]
    for k in range(len(pts) - 1):
        if pts[k][0] <= t <= pts[k + 1][0]:
            u = (t - pts[k][0]) / max(1e-9, pts[k + 1][0] - pts[k][0])
            return tuple(pts[k][1][i] + (pts[k + 1][1][i] - pts[k][1][i]) * u for i in range(3))
    return pts[-1][1]


def lowpass(arr, tau, loop):
    """First-order low-pass of a sampled signal (the lag of a hanging thing behind its anchor); a loop is filtered over three turns so it comes out seamless."""
    a = np.asarray(arr, float)
    n = a.shape[0] - 1 if loop else a.shape[0]
    src = np.concatenate([a[:n]] * 3) if loop else a
    k = 1.0 - math.exp(-1.0 / (GRID * max(1e-3, tau)))
    out = np.empty_like(src)
    y = src[0].copy()
    for i in range(src.shape[0]):
        y = y + (src[i] - y) * k
        out[i] = y
    if loop:
        mid = out[n:2 * n]
        return np.concatenate([mid, mid[:1]])
    return out


# ------------------------------------------------------------------------------------------------------------------------ the arm: forward and inverse kinematics
def euler_from(M):
    """The (rx, ry, rz) degrees with rot_matrix(rx, ry, rz) == M (its inverse, for |ry| < 90)."""
    ry = math.degrees(-math.asin(max(-1.0, min(1.0, float(M[2][0])))))
    rx = -math.degrees(math.atan2(-float(M[2][1]), float(M[2][2])))
    rz = -math.degrees(math.atan2(-float(M[1][0]), float(M[0][0])))
    return rx, ry, rz


def turn_about(axis, angle):
    """The matrix that turns vectors by `angle` radians about the unit vector `axis` (Rodrigues)."""
    x, y, z = axis
    K = np.array(((0.0, -z, y), (z, 0.0, -x), (-y, x, 0.0)))
    return np.eye(3) + math.sin(angle) * K + (1.0 - math.cos(angle)) * (K @ K)


def turn_between(a, b):
    """The smallest turn that takes the unit vector a onto the unit vector b."""
    c = np.cross(a, b)
    n = float(np.linalg.norm(c))
    dot = float(a @ b)
    if n < 1e-9:
        if dot > 0:
            return np.eye(3)
        helper = np.array([1.0, 0.0, 0.0]) if abs(a[0]) < 0.9 else np.array([0.0, 1.0, 0.0])
        ax = np.cross(a, helper)
        return turn_about(ax / np.linalg.norm(ax), math.pi)
    return turn_about(c / n, math.atan2(n, dot))


class ArmIK:
    """One arm as a chain shoulder (3 angles) -> elbow (1 angle) -> wrist, in the unposed body frame. `sg` = +1 for the left arm (x > 0), -1 for the right.

    It is solved in closed form, not iterated: the elbow angle follows from the distance to the wanted wrist point, and the elbow then sits somewhere on a circle around
    the shoulder-wrist line (the swivel). A whole track of targets is solved at once: every sample gets a table of swivel candidates (the natural elbow, clear of the
    torso, inside the joint limits, each with a cost) and a dynamic programme picks one candidate per sample so that the elbow follows the natural position and never flips
    from one side to the other between two samples. An iterative solver started from the previous sample drifts into other poses (a different elbow, a twisted wrist) and
    never comes back; this one gives the same arm for the same target whenever the natural elbow is free.
    """

    MARGIN = 0.0                       # how far outside the torso box the arm is kept, pixels (an arm hanging at the side touches the torso by construction, so no margin)
    PSI = np.arange(-104.0, 105.0, 8.0)          # the swivel candidates, degrees from the natural elbow (27 of them: an elbow never flips to the far side of the arm)
    PREFER = 0.5                       # cost of one swivel step away from the natural elbow (an arm leaves its natural elbow only to keep out of the torso or inside its joint limits)
    SINK = 8.0                         # cost of one pixel of torso penetration
    KEEP = 1.0                         # cost of twisting the shoulder by 8 degrees more than the moving target needs between two samples (grows with the square)

    def __init__(self, R, sg):
        d = R.d
        self.R, self.sg, self.d = R, sg, d
        self.Pa = np.array([sg * d.arm_x, d.sh_piv, 0.0])
        self.Pe = np.array([sg * d.arm_x, d.elbow_y, 0.0])
        self.Pw = np.array([sg * d.arm_x, d.wrist_y, 0.0])
        self.L1, self.L2 = d.upper, d.fore
        self.U = d.upper + d.fore
        self.torso_half = (d.tw / 2.0, d.td / 2.0)
        self.lim = [(-75.0, 178.0), (-85.0, 85.0), (-14.0, 105.0) if sg > 0 else (-105.0, 14.0), (0.0, 150.0)]
        side = "l" if sg > 0 else "r"
        self.up_pts = self._points("arm_" + side, cap=d.sh_piv - 1.0)
        self.fo_pts = self._points("forearm_" + side)
        self.hand_pts = np.array([self.Pw + np.array([0.0, -d.hand * 0.5, 0.0]), self.Pw + np.array([0.0, -d.hand, 0.0])])
        self.hand_grow = 0.4 * max(d.aw, d.ad)
        self.box = (self.torso_half[0] - 0.05, self.torso_half[1] - 0.05, d.hip_y + 0.3, d.sh_y - 0.1)
        pole = np.array([sg * 0.25, -0.45, 0.85])                    # the natural elbow: behind the line from the shoulder to the hand and a little below and out (a bent arm hangs elbow back)
        self.pole = pole / np.linalg.norm(pole)
        cos_min = math.cos(math.radians(self.lim[3][1]))
        self.rho_min = math.sqrt(max(1e-6, self.L1 ** 2 + self.L2 ** 2 + 2.0 * self.L1 * self.L2 * cos_min))
        self.rho_max = 0.995 * self.U
        ps = np.radians(self.PSI)
        self._cos, self._sin = np.cos(ps), np.sin(ps)
        self.last_miss = np.zeros(3)                                  # target minus the wrist point the last single solve reached
        self.last_costs = np.zeros(1)                                 # the cost of the pose chosen for each sample of the last track (see solve_track)
        self.pins = []                                                # (target, swivel index) of rest poses every clip must start and end on (see pin)

    def _points(self, bone, cap=None):
        """The 8 corners of the first cube of a bone plus the ring half way along its length (a long box can cross the torso with all corners outside it)."""
        cubes = self.R.m.bones[bone]["cubes"] if self.R.has(bone) else []
        if not cubes:
            return np.zeros((0, 3))
        o, s = cubes[0]["origin"], cubes[0]["size"]
        pts = []
        for ky in (0.0, 0.5, 1.0):
            for kx in (0, 1):
                for kz in (0, 1):
                    y = o[1] + s[1] * ky
                    if cap is not None and y > cap:
                        continue
                    pts.append((o[0] + s[0] * kx, y, o[2] + s[2] * kz))
        return np.array(pts, float).reshape(-1, 3)

    def fk(self, q):
        Ra = rot_matrix(q[0], q[1], q[2])
        Rf = rot_matrix(q[3], 0.0, 0.0)
        E = self.Pa + Ra @ (self.Pe - self.Pa)
        W = self.Pa + Ra @ ((self.Pe - self.Pa) + Rf @ (self.Pw - self.Pe))
        return W, E, Ra @ Rf

    def target(self, elev, azim, reach, bias=(0.0, 0.0, 0.0)):
        """A wrist point from angles: elevation above the horizontal (degrees, -90 = straight down), azimuth from forward toward outward (0 forward, 90 sideways,
        180 backward) and the fraction of the arm's length."""
        e, a = math.radians(elev), math.radians(azim)
        out, y, fwd = COS(e) * SIN(a), SIN(e), COS(e) * COS(a)
        v = np.array([self.sg * out, y, -fwd]) * self.U * min(0.99, reach)
        return self.Pa + v + np.array(bias)

    # ---- how far the arm's boxes reach into the torso
    def _depth_b(self, P, grow=0.0):
        """For a stack of point sets P (J x N x 3, body frame): per set, the depth (pixels) of the deepest point inside the torso box grown by the margin and `grow`."""
        hx, hz, y0, y1 = self.box
        m = self.MARGIN + grow
        dep = np.minimum(hx + m - np.abs(P[..., 0]), hz + m - np.abs(P[..., 2]))
        ok = (P[..., 1] > y0 - m) & (P[..., 1] < y1 + m) & (dep > 0.0)
        return np.where(ok, dep, 0.0).max(axis=1)

    def _depths(self, Ra, fx):
        """The torso penetration of the arm for a stack of shoulder rotations Ra (J x 3 x 3) and one elbow angle fx."""
        Rf = rot_matrix(fx, 0.0, 0.0)
        out = np.zeros(len(Ra))
        if len(self.up_pts):
            out = np.maximum(out, self._depth_b(self.Pa + np.einsum("nk,jlk->jnl", self.up_pts - self.Pa, Ra)))
        if len(self.fo_pts):
            base = (self.Pe - self.Pa) + (self.fo_pts - self.Pe) @ Rf.T
            out = np.maximum(out, self._depth_b(self.Pa + np.einsum("nk,jlk->jnl", base, Ra)))
        base = (self.Pe - self.Pa) + (self.hand_pts - self.Pe) @ Rf.T
        return np.maximum(out, self._depth_b(self.Pa + np.einsum("nk,jlk->jnl", base, Ra), self.hand_grow))

    def depth(self, q):
        """How far the arm's boxes at the joint angles q reach into the torso (pixels, 0 = clear): what the solver keeps at zero and the selftest measures."""
        return float(self._depths(rot_matrix(q[0], q[1], q[2])[None], q[3])[0])

    # ---- the candidates of one sample
    def _table(self, W, rho, rhat):
        """For the wrist at W (distance rho from the shoulder along rhat): the shoulder angles of every swivel candidate (J x 3, degrees), the elbow angle, the cost of each
        candidate, its torso penetration (pixels) and its shoulder rotation matrix (J x 9)."""
        L1, L2 = self.L1, self.L2
        a = (L1 * L1 - L2 * L2 + rho * rho) / (2.0 * rho)
        b = math.sqrt(max(0.0, L1 * L1 - a * a))
        u = self.pole - (self.pole @ rhat) * rhat
        nu = float(np.linalg.norm(u))
        if nu < 1e-6:
            u = np.cross(rhat, np.array([1.0, 0.0, 0.0]))
            nu = float(np.linalg.norm(u))
        u = u / nu
        v = np.cross(rhat, u)
        fx = math.degrees(math.acos(max(-1.0, min(1.0, (rho * rho - L1 * L1 - L2 * L2) / (2.0 * L1 * L2)))))
        cfx, sfx = math.cos(math.radians(fx)), math.sin(math.radians(fx))
        E = self.Pa + a * rhat + b * (self._cos[:, None] * u + self._sin[:, None] * v)
        e_up = (E - self.Pa) / L1
        f = (W - E) / L2
        Y = -e_up
        Z = (cfx * e_up - f) / max(sfx, 1e-3)
        Z = Z / np.linalg.norm(Z, axis=1)[:, None]
        X = np.cross(Y, Z)
        Ra = np.stack([X, Y, Z], 2)
        ang = np.stack([-np.degrees(np.arctan2(-Ra[:, 2, 1], Ra[:, 2, 2])), -np.degrees(np.arcsin(np.clip(Ra[:, 2, 0], -1.0, 1.0))),
                        -np.degrees(np.arctan2(-Ra[:, 1, 0], Ra[:, 0, 0]))], 1)
        viol = np.zeros(len(ang))
        for k in range(3):
            lo, hi = self.lim[k]
            viol += np.maximum(0.0, np.maximum(lo - ang[:, k], ang[:, k] - hi))
        dep = self._depths(Ra, fx)
        cost = self.SINK * dep + 0.06 * viol + self.PREFER * np.abs(self.PSI) / 8.0
        return ang, fx, cost, dep, Ra.reshape(len(Ra), 9)

    def solve_track(self, T):
        """Joint angles (n x 4: ax, ay, az, fx; degrees) that put the wrist at each of the n targets T and keep the arm out of the torso, and the miss (n x 3: target minus
        the wrist point reached). A target out of reach is brought to the nearest point that is reachable; a target the arm can only reach by entering the body is pushed out
        of the body along its nearest face. In both cases the hand misses and the miss says by how much. A dynamic programme over the swivel candidates of all samples picks
        the path with the least cost, so the elbow follows the natural position and never flips from one side to the other between two samples."""
        T0 = np.asarray(T, float).reshape(-1, 3)
        n = len(T0)
        tabs, Ws, rhats = [], np.zeros((n, 3)), []
        d = self.d
        tt = 0.5 * max(d.aw, d.ad) + 0.05                              # a wrist closer to the torso than the arm is thick cannot be reached by an arm that stays outside it
        sx, sz = self.box[0] + tt, self.box[1] + tt
        hx, hz = self.box[0] + 0.3, self.box[1] + 0.3                  # a hand pushed out of the body steps 0.3 px beyond its surface
        for i in range(n):
            t = T0[i].copy()
            if d.hip_y - 0.5 < t[1] < d.sh_y + 1.5 and abs(t[0]) < sx - 1e-6 and abs(t[2]) < sz - 1e-6:
                if sx - abs(t[0]) < sz - abs(t[2]):
                    t[0] = (1.0 if t[0] > 0 else -1.0 if t[0] < 0 else self.sg) * sx
                else:
                    t[2] = (1.0 if t[2] > 0 else -1.0) * sz if t[2] != 0 else -sz
            for k in range(5):
                r = t - self.Pa
                rho = float(np.linalg.norm(r))
                if rho < 1e-6:
                    r, rho = np.array([0.0, -1.0, -0.3]), 1.0
                rho_c = min(self.rho_max, max(self.rho_min, rho))
                rhat = r / max(rho, 1e-9)
                W = self.Pa + rhat * rho_c
                tab = self._table(W, rho_c, rhat)
                j = int(np.argmin(tab[2]))
                if tab[3][j] <= 0.45 or k == 4:
                    break
                push = float(tab[3][j]) + 0.4                           # the hand cannot stand here without the arm entering the body: step out through the nearer face
                if hz - abs(W[2]) < hx - abs(W[0]):
                    t[2] += (1.0 if W[2] >= 0 else -1.0) * push
                else:
                    t[0] += (1.0 if W[0] >= 0 else -1.0) * push
            tabs.append(tab)
            Ws[i] = W
            rhats.append(rhat)
            self._last_table_cost = tab[2]
        J = len(self.PSI)
        U = np.stack([tb[2] for tb in tabs])
        for i in range(n):                                              # a target that is (nearly) a pinned rest pose takes the pinned elbow: the same pose in every clip
            for pt, jp in self.pins:
                if float(np.linalg.norm(T0[i] - pt)) < 1.5:
                    U[i] += 3.0 * np.abs(np.arange(J) - jp)
                    break
        cost = U[0].copy()
        back = np.zeros((n, J), int)
        for i in range(1, n):
            # changing from candidate j to candidate k costs by how far the shoulder turns beyond what the moving target needs: following a swing is free, twisting the
            # arm about its own axis (a flip of the elbow from one side to the other) is not
            cosang = np.clip((tabs[i - 1][4] @ tabs[i][4].T - 1.0) * 0.5, -1.0, 1.0)
            need = math.degrees(math.acos(max(-1.0, min(1.0, float(rhats[i - 1] @ rhats[i])))))
            excess = np.maximum(0.0, np.degrees(np.arccos(cosang)) - need - 8.0) / 8.0
            tot = cost[:, None] + self.KEEP * excess * excess
            back[i] = np.argmin(tot, axis=0)
            cost = U[i] + tot[back[i], np.arange(J)]
        path = np.zeros(n, int)
        path[-1] = int(np.argmin(cost))
        for i in range(n - 1, 0, -1):
            path[i - 1] = back[i][path[i]]
        q = np.zeros((n, 4))
        for i in range(n):
            ang, fx = tabs[i][0], tabs[i][1]
            q[i] = (ang[path[i]][0], ang[path[i]][1], ang[path[i]][2], fx)
        for k in (0, 2):                                                      # the angles that wrap around: keep them continuous along the track
            q[:, k] = np.degrees(np.unwrap(np.radians(q[:, k])))
        self.last_costs = np.array([U[i][path[i]] for i in range(n)])        # how comfortable each chosen pose is (0 = the natural elbow, clear of everything)
        return q, T0 - Ws

    def pin(self, target):
        """Remember the elbow this arm takes for `target` (a rest pose: the hold of a prop, the hanging arm) so that any later track that passes through, starts or ends at
        that target takes the same elbow instead of a near-tie of it."""
        t = np.asarray(target, float)
        saved, self.pins = self.pins, []
        try:
            self.solve_track(t.reshape(1, 3))
        finally:
            self.pins = saved
        j = int(np.argmin(self._last_table_cost)) if hasattr(self, "_last_table_cost") else len(self.PSI) // 2
        self.pins.append((t.copy(), j))

    def solve(self, T, q0=None, rest=None, **_):
        """One target: the joint angles (ax, ay, az, fx); `last_miss` is the miss. q0 and rest are accepted for callers written against an iterative solver."""
        q, miss = self.solve_track(np.asarray(T, float).reshape(1, 3))
        self.last_miss = miss[0]
        return q[0]

    # ---- the hand
    def wrist_track(self, q, dirs, grip, axis=(0.0, 1.0, 0.0), sides=None):
        """The hand rotation (n x 3, degrees) that turns the prop's long axis `axis` (in the prop's own frame, `grip` = the prop bone's rotation) along each direction of
        `dirs` (body frame) for the arm angles q (n x 4). sides = a list of (local vector, wanted direction) fixes the roll about the axis as well (the string of a bow towards
        the archer, the edge of a blade up). The Euler angles are chosen of the two equivalent triples so that they stay continuous along the track."""
        n = len(q)
        out = np.zeros((n, 3))
        Rg = rot_matrix(*grip)
        a0 = np.asarray(axis, float)
        prev = None
        for i in range(n):
            _, _, Rt = self.fk(q[i])
            want = np.asarray(dirs[i], float)
            want = want / (np.linalg.norm(want) + 1e-9)
            cur = Rt @ Rg @ a0
            Rarc = turn_between(cur / np.linalg.norm(cur), want)
            Rr = np.eye(3)
            if sides is not None and sides[i] is not None:
                s_cur = Rarc @ Rt @ Rg @ np.asarray(sides[i][0], float)
                s_want = np.asarray(sides[i][1], float)
                ps = s_cur - (s_cur @ want) * want
                pt = s_want - (s_want @ want) * want
                if np.linalg.norm(ps) > 1e-6 and np.linalg.norm(pt) > 1e-6:
                    Rr = turn_about(want, math.atan2(float(want @ np.cross(ps, pt)), float(ps @ pt)))
            rx, ry, rz = euler_from(Rt.T @ Rr @ Rarc @ Rt)
            cands = ((rx, ry, rz), (rx + 180.0, 180.0 - ry, rz + 180.0))
            ref = prev if prev is not None else (0.0, 0.0, 0.0)
            best = None
            for c in cands:
                c = tuple(c[k] - 360.0 * round((c[k] - ref[k]) / 360.0) for k in range(3))        # the equivalent angles nearest the previous sample
                dist = sum(abs(c[k] - ref[k]) for k in range(3))
                if best is None or dist < best[0]:
                    best = (dist, c)
            prev = best[1]
            out[i] = prev
        return out

    def wrist_for(self, q, axis_dir, grip, axis=(0.0, 1.0, 0.0), prev=None, side=None, **_):
        """One sample of wrist_track (see there); `prev` is accepted for callers written against an iterative solver."""
        return self.wrist_track(np.asarray(q, float).reshape(1, 4), [axis_dir], grip, axis, None if side is None else [side])[0]


def side_dir(mode, d1):
    """The direction (body frame) that the marked side of a prop should point to when its long axis points along d1: 'edge' (the cutting edge up, or to the side when the
    blade is upright), 'lateral' (to the model's left), 'forward', 'back', 'up'. It is made perpendicular to d1."""
    up = np.array([0.0, 1.0, 0.0])
    lat = np.array([1.0, 0.0, 0.0])
    fwd = np.array([0.0, 0.0, -1.0])
    d1 = np.asarray(d1, float)

    def perp(v):
        return v - (v @ d1) * d1
    if mode == "edge":
        u = perp(up)
        k = 1.0 - min(1.0, np.linalg.norm(u) / 0.7)
        v = u + perp(lat) * k * 1.2
    elif mode == "lateral":
        v = perp(lat)
    elif mode == "forward":
        v = perp(fwd) + perp(up) * 0.01
    elif mode == "back":
        v = perp(-fwd) + perp(up) * 0.01
    else:
        v = perp(up)
    n = np.linalg.norm(v)
    if n < 1e-3:
        v = perp(lat if mode != "lateral" else up)
        n = np.linalg.norm(v) + 1e-9
    return v / n


def dir_from(phi, tilt=0.0):
    """A body-frame direction from the angle `phi` above the horizontal in the forward plane (0 = forward, 90 = up, -90 = down, 180 = backward) and a lateral tilt
    toward the model's left (+) or right (-), in degrees."""
    p, t = math.radians(phi), math.radians(tilt)
    return np.array([COS(p) * SIN(t), SIN(p), -COS(p) * COS(t)])
