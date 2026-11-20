"""wave2_kit: the restrained palette and the varied helpers of the style wave 2 (docs/heroweapon/style/wave2.md). NEW file, it edits nothing that exists.

Why: the Seven Star base weapons, the vassal books and staff forms and the light metal shield were all cut from one recipe (gold + navy + a glowing round gem, even
bands, no wear). This module is the guide's section 8 applied to the builders that are not in the other workers' files:
  * materials through forms_kit.surface() (edge wear, grime, mottle): a weapon gets 3 base materials + ONE named accent, never gold + navy + gem on all;
  * band() takes widths / gaps / taper (never an even row), positions() gives unequal stations;
  * jewel() sets one stone with a mount and a size chosen per weapon (no shared literal);
  * `Palette` names the accent and counts the materials, so a builder cannot slide back to six metals.
Users: seven_star_w2.py, forms_staff_w2.py, forms_book_w2.py, forms_shield_w2.py.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bl_lib as bl  # noqa: E402
import forms_kit as K  # noqa: E402
from bl_lib import rgb  # noqa: E402,F401
from mathutils import Vector  # noqa: E402

V = Vector
PI = math.pi


def _lighter(c, k=1.8, a=0.08):
    return tuple(min(1.0, x * k + a) for x in c[:3]) + (1.0,)


def _darker(c, k=0.5):
    return tuple(x * k for x in c[:3]) + (1.0,)


# ================================================================ materials (all through forms_kit.surface: wear, grime, mottle are baked into the skin)
def iron(name, col, seed=1, wear=0.62, rough=0.42, **kw):
    """Forged iron / dark steel: worn bright at the edges, grime in the crevices, one big forging-colour mottle. Not polished."""
    d = dict(wear=wear, wear_color=_lighter(col, 2.2, 0.10), dirt=0.55, dirt_color=(0.012, 0.010, 0.009, 1), scratches=0.22, bump="hammer", bump_scale=0.7, bump_strength=0.12,
             mottle=0.65, mottle_color=_darker(col, 0.55), fine=0.25, seed=seed, bevel=0.14, ao_dist=2.0)
    d.update(kw)
    return K.surface(name, col, 1.0, rough, **d)


def steel(name, col, seed=1, rough=0.26, **kw):
    """Cold bright steel (a blade): brushed, scratched, the bevel catches the light."""
    d = dict(wear=0.5, wear_color=_lighter(col, 1.25, 0.12), dirt=0.35, scratches=0.45, bump="brushed", bump_strength=0.09, mottle=0.5, mottle_color=_darker(col, 0.62), bevel=0.07)
    d.update(kw)
    return iron(name, col, seed, rough=rough, **d)


def wood(name, col, seed=1, **kw):
    d = dict(bump="grain", bump_scale=1.1, bump_strength=0.45, groove=_darker(col, 0.4), groove_amount=0.8, wear=0.45, wear_color=_lighter(col, 1.35, 0.05), wear_metal=0.0,
             dirt=0.55, mottle=0.9, mottle_color=_darker(col, 0.62), fine=0.4, seed=seed, bevel=0.1)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.58, **d)


def hide(name, col, seed=1, **kw):
    """Leather / cloth: cross-grain bump, worn pale at the edges, dark in the gaps."""
    d = dict(bump="leather", bump_scale=1.2, bump_strength=0.6, groove=_darker(col, 0.3), groove_amount=0.8, wear=0.65, wear_color=_lighter(col, 1.45, 0.05), wear_metal=0.0,
             dirt=0.7, mottle=0.8, mottle_color=_darker(col, 0.55), seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.64, **d)


def cord(name, col, seed=1, **kw):
    d = dict(bump="cross", bump_scale=1.0, bump_strength=0.9, groove=_darker(col, 0.25), groove_amount=0.9, wear=0.5, wear_color=_lighter(col, 1.4, 0.05), wear_metal=0.0,
             dirt=0.7, mottle=0.8, mottle_color=_darker(col, 0.5), fine=0.5, seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.62, **d)


def paper(name, col, seed=1, **kw):
    d = dict(bump="leather", bump_scale=2.0, bump_strength=0.18, wear=0.5, wear_color=_lighter(col, 1.15, 0.03), wear_metal=0.0, dirt=0.9, dirt_color=(0.09, 0.06, 0.03, 1),
             mottle=1.0, mottle_color=_darker(col, 0.7), seed=seed, bevel=0.0, ao_dist=1.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.8, **d)


def accent_gem(name, col, glow, strength=0.55, seed=1, **kw):
    """THE accent: a stone that glows, as bright as nothing else on the weapon."""
    d = dict(emit=glow, emit_strength=strength, emit_flicker=0.35, mottle=0.7, mottle_color=_lighter(col, 1.3, 0.15), seed=seed, bevel=0.05)
    d.update(kw)
    return K.surface(name, col, 0.3, 0.08, **d)


def accent_glow(name, glow, strength=1.5, seed=1, **kw):
    """Bake-only glowing line / mark / edge."""
    d = dict(emit=glow, emit_strength=strength, emit_flicker=0.45, mottle=0.8, mottle_color=_lighter(glow, 1.3, 0.2), seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, _darker(glow, 0.25), 0.0, 0.4, **d)


def scratch(name, col=(0.008, 0.008, 0.010, 1.0)):
    """A dark engraved line / pit: bake-only inlay, no bevel (a thin plate)."""
    return bl.mat(name, col, metallic=0.4, rough=0.5, bevel=0.0)


class Palette(dict):
    """3 base materials + ONE named accent + at most 2 utility materials (the guide's section 5). `accent` names the accent; check() fails past 6 materials."""

    def __init__(self, accent, base, utility=()):
        super().__init__()
        self.accent, self.base, self.utility = accent, tuple(base), tuple(utility)

    def check(self, limit=6):
        names = set(self.base) | set(self.utility) | {self.accent}
        if len(names) > limit:
            raise SystemExit("palette has %d materials (limit %d): %s" % (len(names), limit, sorted(names)))
        return self


# ================================================================ varied hardware
def positions(y0, y1, gaps):
    """Unequal station positions between y0 and y1 from a gap pattern, e.g. (0.9, 1.6, 0.7, 2.1): the first station at y0, the last exactly at y1 (never an even row)."""
    tot = float(sum(gaps))
    out, acc = [y0], 0.0
    for g in gaps:
        acc += g
        out.append(y0 + (y1 - y0) * acc / tot)
    return out


def band(name, y0, y1, r_in, r_out, material, part, sides=10, squash=1.0, lip=0.35, taper=0.0):
    """A ring round the y axis. taper pulls the outer radius in toward y1 (a forged collar, thick at one end). Replaces the even lip=0.6 fittings."""
    r1 = r_out * (1.0 - taper)
    lip = min(lip, (y1 - y0) * 0.4)
    return bl.lathe(name, [(r_in, y0), (r_out, y0 + lip), (r1, y1 - lip), (r_in * (1.0 - taper * 0.4), y1)], material, sides=sides, part=part, squash=squash, caps=(False, False))


def bands(prefix, spans, r_in, r_out, material, part, sides=10, squash=1.0, taper=0.0):
    """A group of bands of DIFFERENT widths and heights: spans = [(y0, y1, r_out scale)], at most 3."""
    return [band("%s%d" % (prefix, i), a, b, r_in, r_out * s, material, part, sides, squash, taper=taper) for i, (a, b, s) in enumerate(spans[:3])]


def jewel(name, center, r, gem_mat, metal_mat, mount="bezel", axis="z", part="gem", sides=8):
    """ONE stone. mount: 'bezel' (a plain rolled collar), 'sunk' (set flush, no collar), 'claws' (four small claws). r is chosen by the weapon, never a shared literal."""
    cx, cy, cz = center
    objs = [bl.gem(name, r, gem_mat, center=center, depth=0.35, crown=0.2, sides=sides, axis=axis, part=part)]
    if mount == "bezel":
        objs.append(bl.lathe(name + "_bezel", [(r * 1.06, -0.2), (r * 1.14, 0.7), (r * 0.98, 1.5), (r * 0.82, 1.4), (r * 0.8, 0.3)], metal_mat, sides=sides * 2, origin=(cx, cy, cz), axis=axis, caps=(False, False), part=part))
    elif mount == "claws":
        for k in range(4):
            a = PI / 4 + PI / 2 * k
            u, v = math.cos(a) * r * 1.02, math.sin(a) * r * 1.02
            p = {"z": (cx + u, cy + v, cz), "y": (cx + u, cy, cz + v), "x": (cx, cy + u, cz + v)}[axis]
            q = {"z": (cx + u * 0.7, cy + v * 0.7, cz + r * 0.5), "y": (cx + u * 0.7, cy + r * 0.5, cz + v * 0.7), "x": (cx + r * 0.5, cy + u * 0.7, cz + v * 0.7)}[axis]
            objs.append(bl.tube("%s_claw%d" % (name, k), [p, q], None, metal_mat, sides=4, sub=1, part=part, taper=(r * 0.16, r * 0.07)))
    return objs


def asym(obs, seed, jitter=0.06, rot=2.5):
    """forms_kit.asym_mirror wired in: the mirrored twin of a pair is slightly scaled / turned (the guide's section 4)."""
    return K.asym_mirror(obs, K.rng(seed), jitter, rot)


def flat_plate(name, outline, z, material, thick=0.06):
    """A bake-only plate lying on the face at height z (z < 0: the -z face)."""
    lo, hi = (z - 0.02, z + thick) if z >= 0 else (z - thick, z + 0.02)
    return bl.plate(name, outline, lo, hi, material, detail=True)
