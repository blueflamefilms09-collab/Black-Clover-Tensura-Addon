"""The frame of an old weapon mesh: where its axis runs, how thick it is along y, where the handle ends and the head begins. The new parts are placed relative to this frame, so a
motif (a collar, a ring of thorns, a ribbon) sits on the real weapon whatever its shape, instead of on an assumed one. Pure numpy; units are the mesh's own (about 1 unit = 1/18 block)."""
import numpy as np

BIN = 1.5


class Frame:
    def __init__(self, quads, kind):
        pts = np.concatenate([q["P"] for q in quads if q["layer"] == 0])
        self.pts = pts
        self.kind = kind
        self.y0, self.y1 = float(pts[:, 1].min()), float(pts[:, 1].max())
        self.x0, self.x1 = float(pts[:, 0].min()), float(pts[:, 0].max())
        self.z0, self.z1 = float(pts[:, 2].min()), float(pts[:, 2].max())
        nb = max(4, int(np.ceil((self.y1 - self.y0) / BIN)))
        self.ys = np.linspace(self.y0, self.y1, nb + 1)
        cx = np.zeros(nb)
        cz = np.zeros(nb)
        hx = np.zeros(nb)
        hz = np.zeros(nb)
        rad = np.zeros(nb)
        have = np.zeros(nb, bool)
        for i in range(nb):
            sel = pts[(pts[:, 1] >= self.ys[i] - 0.4) & (pts[:, 1] <= self.ys[i + 1] + 0.4)]
            if len(sel) < 3:
                continue
            have[i] = True
            cx[i] = (sel[:, 0].min() + sel[:, 0].max()) / 2
            cz[i] = (sel[:, 2].min() + sel[:, 2].max()) / 2
            hx[i] = (sel[:, 0].max() - sel[:, 0].min()) / 2
            hz[i] = (sel[:, 2].max() - sel[:, 2].min()) / 2
        idx = np.arange(nb)
        for arr in (cx, cz, hx, hz):
            arr[~have] = np.interp(idx[~have], idx[have], arr[have]) if have.any() else 0.0
        self.cx, self.cz, self.hx, self.hz = cx, cz, hx, hz
        self.n = nb
        span = self.y1 - self.y0
        thin = np.sort(np.maximum(hx, hz)[: max(3, nb // 3)])
        self.shaft_r = float(max(0.5, np.median(thin)))
        self.length = span

    def at(self, y):
        """(cx, cz, hx, hz) at height y (linear, clamped)."""
        t = (np.clip(y, self.y0, self.y1) - self.y0) / BIN - 0.5
        i = int(np.clip(np.floor(t), 0, self.n - 1))
        j = min(self.n - 1, i + 1)
        f = float(np.clip(t - i, 0, 1))
        return tuple(float(a[i] * (1 - f) + a[j] * f) for a in (self.cx, self.cz, self.hx, self.hz))

    def axis(self, y):
        c = self.at(y)
        return np.array([c[0], y, c[1]])

    def radius(self, y):
        c = self.at(y)
        return max(c[2], c[3])

    def y_at(self, t):
        return self.y0 + (self.y1 - self.y0) * t

    def head_start(self):
        """The lowest height above the handle where the weapon becomes wide: first bin whose half width exceeds 2.6 x the shaft radius and stays so (bows: 15% in from the end)."""
        wide = np.maximum(self.hx, self.hz) > 2.6 * self.shaft_r
        for i in range(self.n // 4, self.n):
            if wide[i] and wide[i:i + 3].all():
                return float(self.ys[i])
        return self.y1 - 0.3 * self.length

    def widest(self):
        i = int(np.argmax(self.hx))
        return float((self.ys[i] + self.ys[i + 1]) / 2), float(self.hx[i])
