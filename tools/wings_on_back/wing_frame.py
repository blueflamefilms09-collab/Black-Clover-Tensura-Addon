"""Python twin of client/geo/WingFrame.java (docs/wings_on_back.md): the wing convention, the forward-wing check and the reflection fix, on the raw geo json.

    python tools/wings_on_back/wing_frame.py [name-regex]        # table of every worn wing group: dz / tipDz before and after the fix

Frame: pixels, y up, front = -z, back = +z, the torso's back face at z = 2. The drawing's own arithmetic (vanilla y-down space, Rz(-rz) Ry(ry) Rx(-rx)) is copied from tools/bc_playermodels/pmrender, so
numbers here match the Java test (WingsOnBackTest) to rounding. Used by preview_wings.py for the before/after mock-ups. Pure numpy, no game, no network.
"""
import copy
import json
import math
import os
import re
import sys

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
GEO = os.path.join(ROOT, 'src/main/resources/assets/anime_multiverse/geo/entity')
BACK_PLANE_Z, MIN_PIVOT_Z, FORWARD_DZ, FORWARD_TIP_DZ = 2.0, 1.0, -1.5, -2.0
WORN = re.compile(r'pmx_.*|(dark|flame|lightning|water)_arm_wing_[lr]|spiritdive_arm_.*wing.*|sdj_.*_arm_wing_[lr]|(nvl|ndg|nbk)_arm_wings2?|bwf_.*_arm_wings'
                  r'|wing_back|corundum_wings|paladin_wings|avatar_wing_\d|soul_armour')
WING_FILE = re.compile(r'.*_wings?\d*(?:bud)?(?:_[lr])?$')


def _rx(a): c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
def _ry(a): c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
def _rz(a): c, s = math.cos(a), math.sin(a); return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def rot(r):
    x, y, z = [v * math.pi / 180 for v in r]
    if x == 0 and y == 0 and z == 0:
        return np.eye(3)
    return _rz(-z) @ _ry(y) @ _rx(-x)


def V(p): return np.array([p[0], -p[1], p[2]], float)


def load(name):
    with open(os.path.join(GEO, name + '.geo.json')) as f:
        return json.load(f)['minecraft:geometry'][0]['bones']


def is_wing_bone(n): return re.search('wing', n, re.I) is not None


def is_anchor(b):
    return not b.get('cubes') and not any(b.get('pivot', [0, 0, 0])) and not any(b.get('rotation', [0, 0, 0])) and not is_wing_bone(b['name'])


def _children(bones):
    by = {b['name']: b for b in bones}
    kids = {}
    for b in bones:
        p = b.get('parent')
        if p and p in by and p != b['name']:
            kids.setdefault(p, []).append(b['name'])
    return by, kids


def corners(bones):
    """name -> list of cubes, each a list of 8 corners (x, y DOWN, z) in the model frame, plus the chain function."""
    by, _ = _children(bones)
    cache = {}

    def chain(name):
        if name in cache:
            return cache[name]
        b = by[name]
        P = V(b.get('pivot', [0, 0, 0]))
        R = rot(b.get('rotation', [0, 0, 0]))
        par = b.get('parent')
        if par and par in by and par != name:
            Mp, tp = chain(par)
            M, t = Mp @ R, tp + Mp @ (P - V(by[par].get('pivot', [0, 0, 0])))
        else:
            M, t = R, P
        cache[name] = (M, t)
        return cache[name]

    out = {}
    for b in bones:
        M, t = chain(b['name'])
        P = V(b.get('pivot', [0, 0, 0]))
        L = []
        for c in b.get('cubes', []):
            o, s = c['origin'], c['size']
            g = c.get('inflate', b.get('inflate', 0))
            mn = np.array([o[0] - g, -(o[1] + s[1]) - g, o[2] - g]) - P
            mx = np.array([o[0] + s[0] + g, -o[1] + g, o[2] + s[2] + g]) - P
            pts = [np.array([(mn if i & 1 == 0 else mx)[0], (mn if i & 2 == 0 else mx)[1], (mn if i & 4 == 0 else mx)[2]]) for i in range(8)]
            if 'rotation' in c and any(c['rotation']):
                cp = V(c.get('pivot', b.get('pivot', [0, 0, 0]))) - P
                Rc = rot(c['rotation'])
                pts = [cp + Rc @ (p - cp) for p in pts]
            L.append([t + M @ p for p in pts])
        out[b['name']] = L
    return out, chain


def groups(name, bones):
    """[(root, pivot(x,y UP,z), centroid, tip)] of every wing group, like WingFrame.groups."""
    by, kids = _children(bones)
    cs, chain = corners(bones)
    wing_file = WING_FILE.match(name) is not None
    out = []

    def sub(n):
        r = [n]
        for c in kids.get(n, []):
            r += sub(c)
        return r

    def walk(n, anchored, inside):
        b = by[n]
        starts = not inside and (is_wing_bone(n) or (wing_file and anchored and not is_anchor(b) and not n.startswith('x_')))
        if starts:
            pts = np.array([p for m in sub(n) for cube in cs[m] for p in cube])
            _, t = chain(n)
            pv = np.array([t[0], -t[1], t[2]])
            if len(pts):
                cen = pts.mean(0) * np.array([1, -1, 1])
                far = pts[np.argmax(np.linalg.norm(pts - t, axis=1))] * np.array([1, -1, 1])
            else:
                cen = far = pv
            out.append(dict(root=n, pivot=pv, centroid=cen, tip=far, dz=cen[2] - pv[2], tipdz=far[2] - pv[2]))
        for c in kids.get(n, []):
            walk(c, anchored and is_anchor(b), inside or starts)

    for b in bones:
        p = b.get('parent')
        if not p or p not in by or p == b['name']:
            walk(b['name'], True, False)
    return out


def forward(g): return g['pivot'][2] >= MIN_PIVOT_Z and (g['dz'] < FORWARD_DZ or g['tipdz'] < FORWARD_TIP_DZ)


def _reflect(bones, by, kids, n, pz):
    b = by[n]
    if 'pivot' in b:
        b['pivot'][2] = 2 * pz - b['pivot'][2]
    if 'rotation' in b:
        b['rotation'][0] = -b['rotation'][0]
        b['rotation'][1] = -b['rotation'][1]
    for c in b.get('cubes', []):
        c['origin'][2] = 2 * pz - c['origin'][2] - c['size'][2]
        if 'rotation' in c and any(c['rotation']):
            if 'pivot' in c:
                c['pivot'][2] = 2 * pz - c['pivot'][2]
            c['rotation'][0] = -c['rotation'][0]
            c['rotation'][1] = -c['rotation'][1]
    for k in kids.get(n, []):
        _reflect(bones, by, kids, k, pz)


def fix(name, bones):
    """(bones turned to the back, [roots turned]); a deep copy. Same reflection as WingFrame.fix."""
    bad = [g['root'] for g in groups(name, bones) if forward(g)]
    c = copy.deepcopy(bones)
    by, kids = _children(c)
    for r in bad:
        pv = by[r].get('pivot', [0, 0, 0])
        _reflect(c, by, kids, r, pv[2])
    return c, bad


def main():
    pat = re.compile(sys.argv[1] if len(sys.argv) > 1 else '.')
    print('%-44s %-10s %7s %7s %7s %7s  %s' % ('file', 'wing', 'dz', 'tipDz', 'dz+', 'tipDz+', ''))
    for f in sorted(os.listdir(GEO)):
        n = f[:-len('.geo.json')]
        if not f.endswith('.geo.json') or not WORN.match(n) or not pat.search(n):
            continue
        bones = load(n)
        before = groups(n, bones)
        fixed, bad = fix(n, bones)
        after = {g['root']: g for g in groups(n, fixed)}
        for g in before:
            a = after[g['root']]
            print('%-44s %-10s %7.2f %7.2f %7.2f %7.2f  %s' % (n, g['root'], g['dz'], g['tipdz'], a['dz'], a['tipdz'], 'FIXED' if g['root'] in bad else ''))


if __name__ == '__main__':
    main()
