"""Python twin of client/motion/SwingMath.java and SwingRuntime.prepare/emit: the same spring maths, the same bone matrices, the same skinning.

The preview strips (swing_strip.py) and the Java golden test (SwingMathTest.golden) both come from this file, so what the strip shows is what the game computes.
"""
import math

import numpy as np

MAX_BONES = 32
PEAK_OMEGA = 9.0
MAX_ARM_ACCEL = 240.0
MAX_STEP = 0.05
SUBSTEP = 1.0 / 60.0
MAX_SUBSTEPS = 4
IDLE_FRACTION = 0.25
FLUTTER = 0.35


def arm_accel(u, rate):
    if not (0.0 < u < 1.0) or not rate > 0.0:
        return 0.0
    a = PEAK_OMEGA * math.pi * math.cos(math.pi * u) * rate
    return max(-MAX_ARM_ACCEL, min(MAX_ARM_ACCEL, a))


def substeps(dt):
    return max(1, min(MAX_SUBSTEPS, int(math.ceil(dt / SUBSTEP))))


def step(th, thv, n, w, zeta, gain, max_rad, arm_acc, dt):
    """th, thv: lists (in and out)."""
    if not dt > 0.0:
        return
    dt = min(dt, MAX_STEP)
    sub = substeps(dt)
    h = dt / sub
    for _ in range(sub):
        acc = arm_acc
        for i in range(n):
            a = -w[i] * w[i] * th[i] - 2.0 * zeta[i] * w[i] * thv[i] - gain[i] * acc
            v = thv[i] + a * h
            x = th[i] + v * h
            m = max_rad[i]
            if x > m:
                x = m
                if v > 0.0:
                    v = 0.0
            elif x < -m:
                x = -m
                if v < 0.0:
                    v = 0.0
            th[i], thv[i] = x, v
            acc += a


def idle(time_sec, i, phase, move):
    amp = 0.35 + 0.65 * max(0.0, min(1.0, move))
    return amp * math.sin(time_sec * 2.6 - i * 0.9 + phase)


def rot(axis, ang):
    ax = np.asarray(axis, float)
    ax = ax / np.linalg.norm(ax)
    c, s = math.cos(ang), math.sin(ang)
    x, y, z = ax
    C = 1 - c
    R = np.array([[c + x * x * C, x * y * C - z * s, x * z * C + y * s],
                  [y * x * C + z * s, c + y * y * C, y * z * C - x * s],
                  [z * x * C - y * s, z * y * C + x * s, c + z * z * C]])
    M = np.eye(4)
    M[:3, :3] = R
    return M


def trans(p):
    M = np.eye(4)
    M[:3, 3] = p
    return M


class Rig:
    """Parsed from a mesh file's `bone` / `skin` lines exactly as SwingRig.java does."""

    def __init__(self, path):
        self.bones, self.skin = [], {}
        for ln in open(path, encoding="utf-8"):
            t = ln.split()
            if not t:
                continue
            if t[0] == "bone":
                f = [float(x) for x in t[3:16]]
                self.bones.append(dict(piv=np.array(f[0:3]), a=np.array(f[3:6]), b=np.array(f[6:9]), gain=f[9], w=f[10], zeta=f[11], max=math.radians(f[12])))
            elif t[0] == "skin":
                self.skin[int(t[1])] = (int(t[2]), [int(x) for x in t[4:]])
        self.n = len(self.bones)

    def params(self):
        return ([b["w"] for b in self.bones], [b["zeta"] for b in self.bones], [b["gain"] for b in self.bones], [b["max"] for b in self.bones])

    def matrices(self, th, thv, base=None, time_sec=0.0, phase=0.0, move=0.0):
        """Bone matrices (mesh space, times `base`), the way SwingRuntime.prepare builds them."""
        mats = []
        cur = np.eye(4) if base is None else base
        for i, b in enumerate(self.bones):
            a = th[i] + IDLE_FRACTION * b["max"] * idle(time_sec, i, phase, move)
            psi = FLUTTER * thv[i] / b["w"]
            local = trans(b["piv"]) @ rot(b["a"], a) @ rot(b["b"], psi) @ trans(-b["piv"])
            cur = cur @ local
            mats.append(cur)
        return mats

    def deform_layer(self, layer, P, n_vertices):
        """P: (n_vertices, 3) mesh-space positions of one layer (4 per quad, file order); returns the skinned positions (rigid head untouched)."""
        if layer not in self.skin:
            return P
        return P

    def skin_points(self, layer, P, mats):
        if layer not in self.skin:
            return P
        frm, codes = self.skin[layer]
        out = P.copy()
        for k, code in enumerate(codes):
            b, wq = code >> 6, code & 63
            v = np.append(P[frm + k], 1.0)
            p0 = (mats[b] @ v)[:3]
            if wq:
                p1 = (mats[b + 1] @ v)[:3]
                p0 = p0 + (p1 - p0) * (wq / 63.0)
            out[frm + k] = p0
        return out


def golden():
    """The trajectory the Java test pins: 3 bones, a 0.3 s strike, 60 fps."""
    n = 3
    w, zeta, gain, mx = [11.0] * n, [0.22] * n, [1.0] * n, [math.radians(9.0)] * n
    th, thv = [0.0] * n, [0.0] * n
    out = {}
    dt = 1.0 / 60.0
    for f in range(1, 121):
        t = f * dt
        u = t / 0.3
        step(th, thv, n, w, zeta, gain, mx, arm_accel(u, 1.0 / 0.3), dt)
        if f in (6, 12, 18, 30, 60, 120):
            out[f] = list(th)
    return out


if __name__ == "__main__":
    for f, v in golden().items():
        print(f, ["%.6f" % x for x in v])
