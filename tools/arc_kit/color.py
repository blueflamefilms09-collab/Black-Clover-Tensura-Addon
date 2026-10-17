"""Why: a character author should write "#1c1c20" and get a believable five-tone cloth palette, not fifteen RGB tuples. This module turns one colour into
hard-edged pixel-art ramps (dark to light, shadows pushed toward blue-violet and highlights toward warm yellow, the way the Lucius palettes are), names the
common skin tones and cloth colours, and resolves every colour spelling a spec may use.
"""
import colorsys
import re

SHADOW_TINT = (46, 38, 84)
LIGHT_TINT = (255, 236, 196)


def rgb(c):
    """A colour spelling -> (r, g, b) ints. Accepts '#rrggbb', 'rrggbb', '#rgb', an (r, g, b[, a]) tuple, or a name of NAMED."""
    if isinstance(c, str):
        s = c.strip().lower()
        if s in NAMED:
            return rgb(NAMED[s])
        s = s.lstrip("#")
        if re.fullmatch(r"[0-9a-f]{3}", s):
            s = "".join(ch * 2 for ch in s)
        if re.fullmatch(r"[0-9a-f]{6}", s):
            return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))
        raise ValueError("unknown colour %r (use '#rrggbb' or one of: %s)" % (c, ", ".join(sorted(NAMED))))
    if isinstance(c, (tuple, list)) and len(c) >= 3:
        return tuple(max(0, min(255, int(round(v)))) for v in c[:3])
    raise ValueError("unknown colour %r" % (c,))


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(round(a[k] * (1 - t) + b[k] * t)) for k in range(3))


def lighten(c, t):
    return mix(c, (255, 255, 255), t)


def darken(c, t):
    return mix(c, (0, 0, 0), t)


def hls(c):
    r, g, b = [v / 255.0 for v in rgb(c)]
    return colorsys.rgb_to_hls(r, g, b)


def from_hls(h, l, s):
    r, g, b = colorsys.hls_to_rgb(h % 1.0, max(0.0, min(1.0, l)), max(0.0, min(1.0, s)))
    return (int(round(r * 255)), int(round(g * 255)), int(round(b * 255)))


def shades(base, n=5, contrast=1.0):
    """n tones from dark to light around `base` (the middle one is base itself). Shadows go toward blue-violet, the highlights toward warm yellow; dark
    colours get a tight ramp (a black coat stays black), light colours a wide shadow side. `contrast` > 1 spreads the ramp (metal), < 1 flattens it (matte)."""
    b = rgb(base)
    h, l, s = hls(b)
    out = []
    span = (n - 1) / 2.0
    lo = (0.05 + 0.40 * min(1.0, l * 1.8)) * contrast
    hi = min((0.07 + 0.34 * min(1.0, l * 2.0)) * contrast, (1.0 - l) * 0.9 + 0.02)
    for k in range(n):
        t = (k - span) / span                              # -1 .. 1
        if k == n // 2 and n % 2 == 1:
            out.append(b)
            continue
        c = from_hls(h, l + (t * lo if t < 0 else t * hi), s)
        c = mix(c, SHADOW_TINT if t < 0 else LIGHT_TINT, abs(t) * (0.20 if t < 0 else 0.13))
        out.append(c)
    return tuple(out)


def hair_shades(base, n=4):
    """A hair palette: the same ramp as a cloth, but a dark hair gets cool blue highlights (black hair shines blue) and a very light one warm shadows."""
    cols = shades(base, n, 1.1)
    h, l, s = hls(base)
    if l < 0.28:
        return tuple(mix(c, (96, 116, 186), 0.34 * (k / float(n - 1)) ** 1.5) for k, c in enumerate(cols))
    if l > 0.7:
        return tuple(mix(c, (150, 120, 130), 0.30 * (1.0 - k / float(n - 1))) for k, c in enumerate(cols))
    return cols


def ramp(base, n=5, contrast=1.0):
    return shades(base, n, contrast)


NAMED = {
    "black": "#1a1a20", "ink": "#101016", "charcoal": "#34343c", "grey": "#808088", "gray": "#808088", "silver": "#b4bccc", "white": "#ecebea",
    "cream": "#e8dcc0", "ivory": "#e0d8c4", "bone": "#d8d0b4", "beige": "#c8b48c", "tan": "#b08c5c", "brown": "#6c4a30", "dark_brown": "#3c281c",
    "leather": "#5a3c28", "red": "#b02424", "crimson": "#8a1426", "maroon": "#5a1820", "orange": "#d0701c", "gold": "#d8a830", "yellow": "#e8d040",
    "olive": "#6c7034", "green": "#2e7a3c", "forest": "#1e4a2c", "teal": "#2a8a86", "cyan": "#38b8d0", "ice": "#9ed8f0", "blue": "#2c56b4",
    "navy": "#1c2a5a", "indigo": "#38306c", "purple": "#6c2c94", "violet": "#8a54c8", "magenta": "#b02c88", "pink": "#e890b4", "rose": "#c84868",
    "steel": "#8c98a8", "iron": "#5c6270", "bronze": "#9a6a34", "copper": "#b4643c", "rust": "#8a4a2c",
}

# Skin tones: four colours dark -> light (the same kind of palette as the Lucius SKIN)
SKIN_TONES = {
    "pale": ((186, 146, 152), (226, 190, 186), (247, 220, 212), (255, 238, 230)),
    "fair": ((190, 144, 128), (228, 184, 160), (244, 208, 184), (252, 228, 208)),
    "light": ((184, 132, 108), (222, 170, 138), (240, 198, 164), (250, 220, 188)),
    "tan": ((152, 100, 70), (190, 132, 92), (216, 160, 114), (232, 184, 136)),
    "brown": ((98, 62, 44), (136, 88, 62), (166, 114, 82), (190, 138, 100)),
    "dark": ((58, 38, 32), (86, 56, 44), (112, 76, 58), (136, 96, 74)),
    "olive": ((128, 106, 70), (166, 140, 94), (194, 168, 118), (214, 192, 140)),
    "ashen": ((120, 122, 134), (156, 158, 170), (186, 188, 198), (212, 214, 222)),
    "grey": ((98, 100, 110), (132, 134, 146), (160, 162, 174), (186, 188, 200)),
    "red": ((120, 36, 40), (166, 54, 54), (200, 80, 70), (224, 112, 94)),
    "blue": ((56, 82, 138), (84, 118, 178), (118, 152, 208), (156, 188, 230)),
    "green": ((52, 92, 52), (78, 128, 70), (108, 160, 92), (140, 190, 118)),
    "purple": ((86, 52, 112), (120, 78, 150), (152, 108, 182), (184, 142, 208)),
    "undead": ((100, 112, 104), (140, 152, 140), (172, 182, 166), (200, 208, 190)),
}


def skin_palette(spec):
    """A skin spec -> 4 colours dark to light: a name of SKIN_TONES, a '#hex' (ramped), or an explicit list of 4 colours."""
    if isinstance(spec, (list, tuple)) and len(spec) == 4 and all(isinstance(v, (list, tuple, str)) for v in spec):
        return tuple(rgb(v) for v in spec)
    if isinstance(spec, str) and spec.lower() in SKIN_TONES:
        return SKIN_TONES[spec.lower()]
    return shades(rgb(spec), 4, 0.8)


def cloth_palette(spec, n=5, contrast=1.0):
    """A material colour spec -> n colours dark to light: '#hex' / a name (ramped) or an explicit list (used as is)."""
    if isinstance(spec, (list, tuple)) and len(spec) >= 3 and isinstance(spec[0], (list, tuple, str)):
        return tuple(rgb(v) for v in spec)
    return shades(spec, n, contrast)
