#!/usr/bin/env python3
"""
PNG alpha audit for the square bug (docs/vfx_square_bug.md). Same rules as vfx/TextureAlpha.java.

  python3 -B tools/vfx_square_bug/png_audit.py            # every sprite the aura painters ask for (client/aura, client/prop) + the ring / circle / glow sprites of textures/particle
  python3 -B tools/vfx_square_bug/png_audit.py --all      # every png under textures/particle, textures/aura, textures/prop

Kinds:  COLOUR_UNDER_ALPHA  transparent texels carry a colour: vanilla's additive eyes type draws the whole quad (a solid square); the fix gives them the alpha-weighted blend
        BLACK_UNDER_ALPHA   transparent texels are black: fine with any blend
        OPAQUE_BRIGHT_EDGE  no transparent texel and bright corners: a flat sprite that lost its alpha (always a square)
        OPAQUE / EMPTY      skins on black / nothing
Exit code 1 when a square sprite (ring, circle, glow ...) has an opaque corner or a sprite an aura asks for does not exist.
"""
import glob, os, re, sys
import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
RES = os.path.join(ROOT, 'src/main/resources/assets/nusmp/textures')
SRC = os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp')
NAME = re.compile(r'(ring|circle|halo|glow|ripple|sigil|haze|puff|bloom|flare|mote|spark|glint)')


def classify(a):
    h, w = a.shape[:2]
    transparent = a[..., 3] <= 8
    n = int(transparent.sum())
    if n == w * h:
        return 'EMPTY'
    k = max(1, min(w, h) // 16)
    corners = [a[:k, :k], a[:k, -k:], a[-k:, :k], a[-k:, -k:]]
    if n == 0:
        return 'OPAQUE_BRIGHT_EDGE' if all(int(c[..., :3].max()) > 96 for c in corners) else 'OPAQUE'
    coloured = int((a[..., :3].max(axis=2)[transparent] > 24).sum())
    return 'COLOUR_UNDER_ALPHA' if coloured > n * 0.05 else 'BLACK_UNDER_ALPHA'


def corners_transparent(a, max_alpha=16):
    h, w = a.shape[:2]
    k = max(1, min(w, h) // 16)
    return all(int(c[..., 3].max()) <= max_alpha for c in (a[:k, :k], a[:k, -k:], a[-k:, :k], a[-k:, -k:]))


def asked():
    out = set()
    for d in ('client/aura', 'client/prop'):
        for f in glob.glob(os.path.join(SRC, d, '**', '*.java'), recursive=True):
            out.update(re.findall(r'\btex\("([a-z0-9_/]+)"\)', open(f).read()))
    return sorted(out)


def main():
    show_all = '--all' in sys.argv
    paths = []
    if show_all:
        for d in ('particle', 'aura', 'prop'):
            paths += [os.path.relpath(p, RES)[:-4] for p in glob.glob(os.path.join(RES, d, '**', '*.png'), recursive=True)]
    else:
        paths = asked()
        for p in glob.glob(os.path.join(RES, 'particle', '*.png')):
            n = os.path.basename(p)[:-4]
            if NAME.search(n) and not n.startswith('wiki_shell_'):
                paths.append('particle/' + n)
    bad, counts = 0, {}
    for rel in sorted(set(paths)):
        f = os.path.join(RES, rel + '.png')
        if not os.path.exists(f):
            print('MISSING  %s' % rel)
            bad += 1
            continue
        a = np.array(Image.open(f).convert('RGBA'))
        kind = classify(a)
        counts[kind] = counts.get(kind, 0) + 1
        square = a.shape[0] == a.shape[1] and NAME.search(os.path.basename(rel)) and rel.startswith('particle/')
        flag = ''
        if square and not corners_transparent(a):
            flag = '  <-- opaque corner on a square sprite'
            bad += 1
        print('%-20s %-52s %dx%d%s' % (kind, rel, a.shape[1], a.shape[0], flag))
    print('\n' + ', '.join('%s: %d' % kv for kv in sorted(counts.items())))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
