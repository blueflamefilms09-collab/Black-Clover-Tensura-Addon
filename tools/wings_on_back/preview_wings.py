"""Software mock-ups (NOT the game) of the wings before / after docs/wings_on_back.md: flat side and top silhouettes of the wing cubes next to a torso outline.

    python tools/wings_on_back/preview_wings.py            # writes docs/previews/wings_on_back/*.png (one per fixed form, the devil hero, overview.png)
    python tools/wings_on_back/preview_wings.py pmx_dv_lucifero dark_arm_wing

Each cube of a wing is drawn as the outline of its 8 corners seen from the side (the model's left, front to the RIGHT) and from above (front up), red = the file as drawn before, green = after the
fix (WingFrame / wing_frame.py: the same wing mirrored in the plane of its hinge). Grey = a 8x12x4 px torso, head, arms and legs for scale, the dot = the hinge. This is a flat drawing of the cube
corners: no textures, no shading, no light; it shows WHERE the wings are, not how they look. Pure numpy + PIL.
"""
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wing_frame as W  # noqa: E402

OUT = os.path.join(W.ROOT, 'docs/previews/wings_on_back')
RED, GREEN, GREY, INK = (214, 70, 62), (52, 160, 84), (176, 176, 184), (40, 40, 48)
CELL_W, CELL_H = 300, 300
SIDE_S, TOP_S = 6.0, 3.1                    # px per model pixel
Y_LO, Y_HI = -2.0, 46.0


def hull(pts):
    pts = sorted(set((round(float(x), 2), round(float(y), 2)) for x, y in pts))
    if len(pts) < 3:
        return pts

    def cross(o, a, b): return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lo, up = [], []
    for p in pts:
        while len(lo) >= 2 and cross(lo[-2], lo[-1], p) <= 0:
            lo.pop()
        lo.append(p)
    for p in reversed(pts):
        while len(up) >= 2 and cross(up[-2], up[-1], p) <= 0:
            up.pop()
        up.append(p)
    return lo[:-1] + up[:-1]


def wing_cubes(files, fixed):
    """Every cube of every wing group of the files: list of 8x3 arrays (x, y UP, z)."""
    out, hinges = [], []
    for n in files:
        bones = W.load(n)
        if fixed:
            bones, _ = W.fix(n, bones)
        by, kids = W._children(bones)
        cs, _ = W.corners(bones)
        for g in W.groups(n, bones):
            sub, stack = [], [g['root']]
            while stack:
                x = stack.pop()
                sub.append(x)
                stack += kids.get(x, [])
            for m in sub:
                for cube in cs[m]:
                    out.append(np.array(cube) * np.array([1, -1, 1]))
            hinges.append(g['pivot'])
    return out, hinges


BODY = [  # (x0, x1, y0, y1, z0, z1): torso, head, arms, legs of a player model, front = -z
    (-4, 4, 12, 24, -2, 2), (-4, 4, 24, 32, -4, 4), (4, 8, 12, 24, -2, 2), (-8, -4, 12, 24, -2, 2), (0, 4, 0, 12, -2, 2), (-4, 0, 0, 12, -2, 2)]


def panel(cubes, hinges, color, view, title):
    im = Image.new('RGBA', (CELL_W, CELL_H), (250, 250, 252, 255))
    d = ImageDraw.Draw(im, 'RGBA')
    if view == 'side':
        def P(x, y, z): return (CELL_W / 2 - z * SIDE_S, CELL_H - 18 - (y - Y_LO) * SIDE_S * 0.98)
        for x0, x1, y0, y1, z0, z1 in BODY[:2] + BODY[2:3] + BODY[4:5]:
            d.rectangle([P(0, y1, z1)[0], P(0, y1, z1)[1], P(0, y0, z0)[0], P(0, y0, z0)[1]], fill=GREY + (150,), outline=INK + (255,))
        proj = lambda c: [P(x, y, z) for x, y, z in c]
        d.text((6, 18), 'back', fill=INK)
        d.text((CELL_W - 34, 18), 'front', fill=INK)
    else:
        def P(x, y, z): return (CELL_W / 2 + x * TOP_S, CELL_H / 2 + 8 + z * TOP_S)
        for x0, x1, y0, y1, z0, z1 in BODY[:2]:
            d.rectangle([P(x0, 0, z0)[0], P(x0, 0, z0)[1], P(x1, 0, z1)[0], P(x1, 0, z1)[1]], fill=GREY + (150,), outline=INK + (255,))
        for x0, x1, y0, y1, z0, z1 in BODY[2:4]:
            d.rectangle([P(x0, 0, z0)[0], P(x0, 0, z0)[1], P(x1, 0, z1)[0], P(x1, 0, z1)[1]], fill=GREY + (110,), outline=INK + (200,))
        proj = lambda c: [P(x, y, z) for x, y, z in c]
        d.text((6, 18), 'front (up)', fill=INK)
    for c in cubes:
        pts = hull(proj(c))
        if len(pts) >= 3:
            d.polygon(pts, fill=color + (70,), outline=color + (255,))
        else:
            d.line(pts, fill=color + (255,))
    for h in hinges:
        x, y = P(h[0], h[1], h[2])
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=INK + (255,))
    d.text((6, 4), title, fill=color + (255,))
    return im.convert('RGB')


def render(label, files, out):
    cb, hb = wing_cubes(files, False)
    ca, ha = wing_cubes(files, True)
    sheet = Image.new('RGB', (CELL_W * 4, CELL_H + 22), (255, 255, 255))
    sheet.paste(panel(cb, hb, RED, 'side', 'BEFORE side'), (0, 22))
    sheet.paste(panel(ca, ha, GREEN, 'side', 'AFTER side'), (CELL_W, 22))
    sheet.paste(panel(cb, hb, RED, 'top', 'BEFORE top'), (CELL_W * 2, 22))
    sheet.paste(panel(ca, ha, GREEN, 'top', 'AFTER top'), (CELL_W * 3, 22))
    ImageDraw.Draw(sheet).text((6, 5), '%s   (software mock-up of the cube corners, not the game)' % label, fill=INK)
    sheet.save(out, optimize=True)
    return sheet


def forms():
    """label -> files; one entry per fixed form (an _l / _r pair is one form)."""
    allf = sorted(f[:-len('.geo.json')] for f in os.listdir(W.GEO) if f.endswith('.geo.json'))
    out = {}
    for n in allf:
        if not W.WORN.match(n):
            continue
        bones = W.load(n)
        if not any(W.forward(g) for g in W.groups(n, bones)):
            continue
        key = re.sub(r'_[lr]$', '', n)
        out.setdefault(key, []).append(n)
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    only = sys.argv[1:]
    fs = forms()
    rows = []
    for key, files in fs.items():
        if only and not any(o in key for o in only):
            continue
        img = render(key, files, os.path.join(OUT, key + '.png'))
        rows.append((key, img))
    ref = ('corundum_wings (already correct, unchanged: the reference)', ['corundum_wings'])
    if not only:
        render(ref[0], ref[1], os.path.join(OUT, 'reference_corundum_wings.png'))
        fam = [k for k in ('pmx_dv_lucifero', 'pmx_dv_beelzebub', 'pmx_bc_lucifero', 'dark_arm_wing', 'flame_arm_wing', 'lightning_arm_wing', 'water_arm_wing',
                           'spiritdive_arm_fire_wing', 'spiritdive_arm_saint_wing', 'nvl_arm_wings', 'nvl_arm_wings2') if k in fs]
        imgs = [r[1] for r in rows if r[0] in fam] + [Image.open(os.path.join(OUT, 'reference_corundum_wings.png'))]
        sheet = Image.new('RGB', (imgs[0].width, sum(i.height for i in imgs)), (255, 255, 255))
        y = 0
        for i in imgs:
            sheet.paste(i, (0, y))
            y += i.height
        sheet.save(os.path.join(OUT, 'overview.png'), optimize=True)
    print('wrote %d form previews to %s' % (len(rows), OUT))


if __name__ == '__main__':
    main()
