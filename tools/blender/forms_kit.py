"""forms_kit: the QUALITY KIT for the weapon-form models (hero / vassal forms). Built ON TOP of bl_lib.py (which it never edits).

Why it exists: the first batch of forms was made of primitives (cones, spheres, flat plates), uniform, symmetric, clean, flat colour. The bar is the Genesis
Demon-Slayer: chipped asymmetric silhouette, stepped notches, dozens of rivets and plate seams, layered fittings, cloth wraps with hanging tails, secondary
detail, wear. Everything here makes DENSE, ASYMMETRIC, SEEDED (deterministic) geometry and WORN, baked materials. See docs/heroweapon/forms_kit.md.

Import it from a form script:
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import forms_kit as K
    K.begin(samples=24)                              # empty scene, studio bake light
    M = K.palette(...)  /  K.surface(...)            # materials with baked wear
    ... K.blade(...), K.guard_plates(...), K.feather(...), K.shard_cloud(...) ...
    K.beauty(path)                                   # a Cycles still of the model, studio light (look at it!)
    K.finish_form("legendary_sword__phoenix_sword", "legendary_sword", out_dir=..., trial=...)   # bake + export + self-check report

Coordinates are the mod's: y = the weapon's length (cm), x its width, z its thickness (+z faces the viewer). Objects never keep a transform.
Budget (rule 11): at most 5000 quad lines per mesh INCLUDING the glow copies (a quad whose skin area holds glow gets a second, additive line), skin 512 px,
skin + glow PNG <= 250 KB after tools/shrink_pngs.py. `quad_estimate()` tells you while building, `report()` after baking.
"""
import json
import math
import os
import random
import shutil
import sys
import time

import bpy  # noqa: I001  (bpy first)
import bmesh
import numpy as np
from mathutils import Matrix, Vector
from mathutils import noise as mnoise

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402

ROOT = bl.ROOT
MAX_QUADS = 5000
MAX_PNG_KB = 250
V = Vector
TAU = 2 * math.pi
FONT_RUNIC = "/usr/share/fonts/truetype/freefont/FreeMono.ttf"
RUNES = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ"
_T0 = [time.time()]


# ================================================================ scene
def begin(samples=24, bounces=6):
    """Empty scene with the studio bake light (bl_lib.reset) and Cycles settings for baking (transmission needs bounces)."""
    _T0[0] = time.time()
    sc = bl.reset(samples)
    sc.cycles.max_bounces = bounces
    sc.cycles.transmission_bounces = max(bounces, 8)
    sc.cycles.diffuse_bounces = 3
    sc.cycles.glossy_bounces = 4
    sc.cycles.sample_clamp_indirect = 8.0
    return sc


def rng(seed):
    return random.Random(seed)


def noise3(p, scale=1.0, seed=0, octaves=1):
    """Perlin-like noise -1..1 at point p (deterministic in seed)."""
    q = V(p) * scale + V((seed * 17.13, seed * 3.71, seed * 9.37))
    if octaves <= 1:
        return mnoise.noise(q)
    return mnoise.fractal(q, 0.5, 2.0, octaves)


def lerp(a, b, t):
    return a + (b - a) * t


def smoothstep(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a if b != a else 1e-9)))
    return t * t * (3 - 2 * t)


def interp(table, x):
    """Piecewise-linear lookup in [(x, value)] sorted by x."""
    if x <= table[0][0]:
        return table[0][1]
    for (x0, v0), (x1, v1) in zip(table, table[1:]):
        if x <= x1:
            return v0 + (v1 - v0) * (x - x0) / ((x1 - x0) or 1e-9)
    return table[-1][1]


# ================================================================ materials: baked wear
def _n(nt, kind, **kw):
    n = nt.nodes.new(kind)
    for k, v in kw.items():
        setattr(n, k, v)
    return n


def _map_range(nt, val, lo, hi, out_lo=0.0, out_hi=1.0, clamp=True):
    n = _n(nt, "ShaderNodeMapRange")
    n.clamp = clamp
    n.inputs["From Min"].default_value = lo
    n.inputs["From Max"].default_value = hi
    n.inputs["To Min"].default_value = out_lo
    n.inputs["To Max"].default_value = out_hi
    nt.links.new(val, n.inputs["Value"])
    return n.outputs["Result"]


def _noise(nt, co, scale, detail=5.0, rough=0.55, aniso=(1, 1, 1), seed=0):
    vec = co
    if aniso != (1, 1, 1) or seed:
        mp = _n(nt, "ShaderNodeMapping")
        mp.inputs["Scale"].default_value = aniso
        mp.inputs["Location"].default_value = (seed * 7.3, seed * 3.1, seed * 5.9)
        nt.links.new(co, mp.inputs["Vector"])
        vec = mp.outputs[0]
    n = _n(nt, "ShaderNodeTexNoise")
    n.inputs["Scale"].default_value = scale
    n.inputs["Detail"].default_value = detail
    n.inputs["Roughness"].default_value = rough
    nt.links.new(vec, n.inputs["Vector"])
    return n.outputs["Fac"]


def _scratch(nt, co, scale, width, aniso, seed):
    """Thin scratch lines: the distance to the nearest cell wall of a stretched voronoi, thresholded."""
    mp = _n(nt, "ShaderNodeMapping")
    mp.inputs["Scale"].default_value = aniso
    mp.inputs["Location"].default_value = (seed * 4.1, seed * 2.3, seed * 6.7)
    nt.links.new(co, mp.inputs["Vector"])
    v = _n(nt, "ShaderNodeTexVoronoi", feature="DISTANCE_TO_EDGE")
    v.inputs["Scale"].default_value = scale
    nt.links.new(mp.outputs[0], v.inputs["Vector"])
    return _map_range(nt, v.outputs["Distance"], 0.0, width, 1.0, 0.0)


def surface(name, base, metal=0.0, rough=0.5, *, wear=0.0, wear_color=None, wear_metal=None, dirt=0.0, dirt_color=(0.012, 0.010, 0.009, 1.0),
            scratches=0.0, mottle=0.0, mottle_color=None, bump=None, bump_scale=1.0, bump_strength=0.3, fine=0.0, bevel=0.16, ao_dist=1.5,
            scale=1.0, emit=None, emit_strength=0.0, emit_ramp=None, emit_flicker=0.0, emit_cracks=0.0, transmission=0.0, ior=1.25, sss=0.0,
            sss_color=None, rough_wear=0.25, seed=0, groove=None, groove_amount=0.7, veins=None, vein_color=None, vein_strength=0.0,
            feather=None, tramp=None, temit=None, cramp=None, rim=None, wear_radius=0.55):
    """A worn, baked-detail material. All the extras are SHADER nodes, so they cost nothing in game: they are rendered into the 512 px skin.
      wear  0..1  edge wear: the Bevel node's normal against the true normal finds the edges, noise breaks it up; worn edges show `wear_color`
                  (bright bare metal) and become metallic (`wear_metal`), glossier. Paint over metal = base metal 0 + wear_metal 1.
      dirt  0..1  grime in crevices: the AO node (distance `ao_dist`) darkens toward `dirt_color`, broken up by noise.
      scratches 0..1  fine scratch lines (two stretched voronoi layers) that lighten the colour and roughen it.
      mottle 0..1  a large-scale colour variation toward `mottle_color` (stain, patina, forging colour).
      bump  a bl_lib bump kind ('hammer', 'brushed', 'leather', 'grain', 'wrap', 'cross', 'scales', 'rings') + `fine` extra micro-noise height.
      groove  colour the bump's low parts darken to.
      emit  emission colour; emit_strength; emit_ramp=(axis 'x'|'y'|'z', lo, hi, [(pos 0..1, (r,g,b,a))...]) paints the emission along an axis (a flame
            from root to tip); emit_flicker 0..1 breaks it up with noise; emit_cracks 0..1 makes only the wear / crack mask glow (an ember in the edge).
      veins  a noise-vein pattern (cracks, circuit lines) that mixes `vein_color` into the colour and emits it (vein_strength).
      transmission / sss  glassy or gelatinous look (the bake sees through it: baked translucency, see slime shield).
      feather  (freq, slant, strength, rachis 0..1): painted BARBS and a rachis, read from the strip's own (t along, v across) attribute `fuv`, so the pattern follows
               every feather's own direction. tramp [(pos 0..1, colour)]: base colour painted root -> tip along t; temit [(pos, colour)]: emission painted along t.
      cramp  (axis, lo, hi, stops): base colour painted along a world axis (a heat gradient up a blade).
      rim    (colour, strength): a fresnel-like rim light baked into the colour (the glassy edge of a gel).
    Returns the material. Colours are linear RGBA (use rgb(r, g, b))."""
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    tc = _n(nt, "ShaderNodeTexCoord")
    co = tc.outputs["Object"]
    mp0 = _n(nt, "ShaderNodeMapping")
    mp0.inputs["Scale"].default_value = (scale, scale, scale)
    L.new(co, mp0.inputs["Vector"])
    co = mp0.outputs[0]

    # ---- shading normal: bevel, then bump
    normal = None
    edge = None
    if bevel > 0:
        bv = _n(nt, "ShaderNodeBevel")
        bv.samples = 6
        bv.inputs["Radius"].default_value = bevel
        normal = bv.outputs["Normal"]
        ng = _n(nt, "ShaderNodeNewGeometry")
        dot = _n(nt, "ShaderNodeVectorMath", operation="DOT_PRODUCT")
        L.new(normal, dot.inputs[0])
        L.new(ng.outputs["True Normal"], dot.inputs[1])
        edge = _map_range(nt, dot.outputs["Value"], 1.0, 0.82, 0.0, 1.0)       # 0 flat .. 1 on a rounded edge
    if wear > 0:                                                               # a wider bevel just for the wear mask: worn edges a few texels wide
        bw = _n(nt, "ShaderNodeBevel")
        bw.samples = 8
        bw.inputs["Radius"].default_value = wear_radius
        dw = _n(nt, "ShaderNodeVectorMath", operation="DOT_PRODUCT")
        L.new(bw.outputs["Normal"], dw.inputs[0])
        L.new(_n(nt, "ShaderNodeNewGeometry").outputs["True Normal"], dw.inputs[1])
        edge = _map_range(nt, dw.outputs["Value"], 1.0, 0.90, 0.0, 1.0)
    height = None
    if bump or fine > 0:
        hs = []
        if bump:
            hs.append(bl._height(nt, bump, co, bump_scale))
        if fine > 0:
            hs.append(_math(nt, "MULTIPLY", _noise(nt, co, 1.6, 4.0, 0.6, seed=seed + 3), fine))
        height = hs[0] if len(hs) == 1 else _math(nt, "ADD", hs[0], hs[1])
        bp = _n(nt, "ShaderNodeBump")
        bp.inputs["Strength"].default_value = bump_strength
        bp.inputs["Distance"].default_value = 0.35
        L.new(height, bp.inputs["Height"])
        if normal is not None:
            L.new(normal, bp.inputs["Normal"])
        normal = bp.outputs["Normal"]
    if normal is not None:
        L.new(normal, bs.inputs["Normal"])

    # ---- painted per-strip attribute (t along, v across)
    tt = vv = barb = rach = None
    if feather or tramp or temit:
        at = _n(nt, "ShaderNodeAttribute")
        at.attribute_name = "fuv"
        at.attribute_type = "GEOMETRY"
        sp = _n(nt, "ShaderNodeSeparateXYZ")
        L.new(at.outputs["Vector"], sp.inputs[0])
        tt, vv = sp.outputs[0], sp.outputs[1]
        if feather:
            fr, sl, st_, ra = (list(feather) + [26.0, 6.0, 0.5, 0.6][len(feather):])[:4]
            av = _math(nt, "ABSOLUTE", vv)
            ph = _math(nt, "SUBTRACT", _math(nt, "MULTIPLY", tt, fr), _math(nt, "MULTIPLY", av, sl))
            barb = _math(nt, "POWER", _math(nt, "MULTIPLY_ADD", _math(nt, "SINE", _math(nt, "MULTIPLY", ph, TAU)), 0.5, 0.5), 1.6)
            rach = _map_range(nt, av, 0.0, 0.10, 1.0, 0.0)
            hh = _math(nt, "ADD", _math(nt, "MULTIPLY", barb, 0.6), _math(nt, "MULTIPLY", rach, 1.2))
            bp2 = _n(nt, "ShaderNodeBump")
            bp2.inputs["Strength"].default_value = 0.8
            bp2.inputs["Distance"].default_value = 0.25
            L.new(hh, bp2.inputs["Height"])
            if normal is not None:
                L.new(normal, bp2.inputs["Normal"])
            normal = bp2.outputs["Normal"]
            L.new(normal, bs.inputs["Normal"])

    # ---- colour
    col = _n(nt, "ShaderNodeRGB")
    col.outputs[0].default_value = base
    color = col.outputs[0]
    if tramp:
        color = _ramp(nt, tt, tramp)
    if cramp:
        ax, lo_, hi_, stops_ = cramp
        sp2 = _n(nt, "ShaderNodeSeparateXYZ")
        L.new(tc.outputs["Object"], sp2.inputs[0])
        color = _ramp(nt, _map_range(nt, sp2.outputs["XYZ".index(ax.upper())], lo_, hi_), stops_)
    if feather:
        bc = _n(nt, "ShaderNodeRGB")
        bc.outputs[0].default_value = tuple(c * 0.42 for c in base[:3]) + (1.0,)
        color = _mix(nt, color, bc.outputs[0], _math(nt, "MULTIPLY", barb, feather[2] if len(feather) > 2 else 0.5))
        rc = _n(nt, "ShaderNodeRGB")
        rc.outputs[0].default_value = tuple(min(1.0, c * 1.6 + 0.12) for c in base[:3]) + (1.0,)
        color = _mix(nt, color, rc.outputs[0], _math(nt, "MULTIPLY", rach, feather[3] if len(feather) > 3 else 0.6))
    if mottle > 0:
        mc = _n(nt, "ShaderNodeRGB")
        mc.outputs[0].default_value = mottle_color or tuple(min(1.0, c * 0.55) for c in base[:3]) + (1.0,)
        fac = _map_range(nt, _noise(nt, co, 0.11, 5.0, 0.55, seed=seed + 11), 0.30, 0.72, 0.0, mottle)
        color = _mix(nt, color, mc.outputs[0], fac)
    if groove is not None and height is not None:
        gc = _n(nt, "ShaderNodeRGB")
        gc.outputs[0].default_value = groove
        gf = _map_range(nt, height, 0.0, 0.8, groove_amount, 0.0)
        color = _mix(nt, color, gc.outputs[0], gf)
    if veins is not None:
        vm = _scratch_cells(nt, co, veins, seed)
        vc = _n(nt, "ShaderNodeRGB")
        vc.outputs[0].default_value = vein_color or base
        color = _mix(nt, color, vc.outputs[0], vm)
    roughness = _n(nt, "ShaderNodeValue")
    roughness.outputs[0].default_value = rough
    rough_out = roughness.outputs[0]
    metal_out = _n(nt, "ShaderNodeValue")
    metal_out.outputs[0].default_value = metal
    metal_out = metal_out.outputs[0]

    wmask = None
    if wear > 0 and edge is not None:
        wn = _noise(nt, co, 0.32, 6.0, 0.62, seed=seed + 21)
        wn2 = _noise(nt, co, 1.3, 3.0, 0.5, seed=seed + 22)
        v = _math(nt, "ADD", _math(nt, "MULTIPLY", edge, 1.0 + 1.3 * wear), _math(nt, "MULTIPLY", _math(nt, "ADD", wn, wn2), 0.30 * wear))
        wmask = _map_range(nt, v, 0.72 - 0.35 * wear, 1.0 - 0.25 * wear)
        wc = _n(nt, "ShaderNodeRGB")
        wc.outputs[0].default_value = wear_color or tuple(min(1.0, c * 1.9 + 0.07) for c in base[:3]) + (1.0,)
        color = _mix(nt, color, wc.outputs[0], wmask)
        rough_out = _math(nt, "SUBTRACT", rough_out, _math(nt, "MULTIPLY", wmask, rough_wear))
        wm = 1.0 if wear_metal is None else wear_metal
        metal_out = _mix_f(nt, metal_out, wm, wmask)
    if scratches > 0:
        s1 = _scratch(nt, co, 0.9, 0.035, (1.0, 0.12, 0.6), seed + 31)
        s2 = _scratch(nt, co, 0.7, 0.03, (0.35, 0.9, 0.35), seed + 32)
        patch = _map_range(nt, _noise(nt, co, 0.09, 3.0, 0.5, seed=seed + 33), 0.4, 0.62, 0.0, 1.0)
        sm = _math(nt, "MULTIPLY", _math(nt, "MULTIPLY", _math(nt, "MAXIMUM", s1, s2), patch), scratches)
        lc = _n(nt, "ShaderNodeRGB")
        lc.outputs[0].default_value = tuple(min(1.0, c * 1.5 + 0.05) for c in base[:3]) + (1.0,)
        color = _mix(nt, color, lc.outputs[0], sm)
        rough_out = _math(nt, "ADD", rough_out, _math(nt, "MULTIPLY", sm, 0.2))
    if dirt > 0:
        ao = _n(nt, "ShaderNodeAmbientOcclusion")
        ao.inputs["Distance"].default_value = ao_dist
        ao.samples = 8
        dn = _map_range(nt, _noise(nt, co, 0.35, 5.0, 0.6, seed=seed + 41), 0.25, 0.8, 0.45, 1.0)
        dm = _math(nt, "MULTIPLY", _math(nt, "POWER", _math(nt, "SUBTRACT", 1.0, ao.outputs["AO"]), 1.25), _math(nt, "MULTIPLY", dn, dirt * 1.6))
        dc = _n(nt, "ShaderNodeRGB")
        dc.outputs[0].default_value = dirt_color
        color = _mix(nt, color, dc.outputs[0], _clamp01(nt, dm))
        rough_out = _math(nt, "ADD", rough_out, _math(nt, "MULTIPLY", dm, 0.25))
        if wmask is not None:                                              # grime sits in the crevice, not on the bare worn edge
            pass
    if rim:
        lw = _n(nt, "ShaderNodeLayerWeight")
        lw.inputs["Blend"].default_value = 0.35
        rcol = _n(nt, "ShaderNodeRGB")
        rcol.outputs[0].default_value = rim[0]
        color = _mix(nt, color, rcol.outputs[0], _math(nt, "MULTIPLY", lw.outputs["Facing"], 0.0 + rim[1]) if False else _math(nt, "MULTIPLY", _math(nt, "SUBTRACT", 1.0, lw.outputs["Facing"]), rim[1]))
    L.new(color, bs.inputs["Base Color"])
    L.new(_clamp01(nt, rough_out), bs.inputs["Roughness"])
    L.new(_clamp01(nt, metal_out), bs.inputs["Metallic"])

    # ---- glass / gel
    if transmission > 0:
        bs.inputs["Transmission Weight"].default_value = transmission
        bs.inputs["IOR"].default_value = ior
    if sss > 0:
        bs.inputs["Subsurface Weight"].default_value = sss
        bs.inputs["Subsurface Radius"].default_value = (1.0, 0.6, 0.4)
        bs.inputs["Subsurface Scale"].default_value = 0.8
    # ---- emission
    if emit is not None and emit_strength > 0:
        ecol = _n(nt, "ShaderNodeRGB")
        ecol.outputs[0].default_value = emit
        e = ecol.outputs[0]
        if temit:
            e = _ramp(nt, tt, temit)
        if emit_ramp:
            axis, lo, hi, stops = emit_ramp
            sep = _n(nt, "ShaderNodeSeparateXYZ")
            L.new(tc.outputs["Object"], sep.inputs[0])
            t = _map_range(nt, sep.outputs["XYZ".index(axis.upper())], lo, hi)
            cr = _n(nt, "ShaderNodeValToRGB")
            cr.color_ramp.elements[0].position, cr.color_ramp.elements[0].color = stops[0][0], stops[0][1]
            cr.color_ramp.elements[1].position, cr.color_ramp.elements[1].color = stops[-1][0], stops[-1][1]
            for pos, c in stops[1:-1]:
                el = cr.color_ramp.elements.new(pos)
                el.color = c
            L.new(t, cr.inputs["Fac"])
            e = cr.outputs["Color"]
        strength = _n(nt, "ShaderNodeValue")
        strength.outputs[0].default_value = emit_strength
        s_out = strength.outputs[0]
        if emit_flicker > 0:
            fl = _map_range(nt, _noise(nt, co, 0.16, 6.0, 0.6, aniso=(1.0, 0.45, 1.0), seed=seed + 51), 0.28, 0.72, 1.0 - emit_flicker, 1.0 + 0.4 * emit_flicker)
            s_out = _math(nt, "MULTIPLY", s_out, fl)
        if emit_cracks > 0 and wmask is not None:
            s_out = _math(nt, "MULTIPLY", s_out, _math(nt, "MULTIPLY", wmask, emit_cracks))
        L.new(e, bs.inputs["Emission Color"])
        L.new(s_out, bs.inputs["Emission Strength"])
    if veins is not None and vein_strength > 0:
        vm = _scratch_cells(nt, co, veins, seed)
        vc2 = _n(nt, "ShaderNodeRGB")
        vc2.outputs[0].default_value = vein_color or base
        L.new(vc2.outputs[0], bs.inputs["Emission Color"])
        L.new(_math(nt, "MULTIPLY", vm, vein_strength), bs.inputs["Emission Strength"])
    m["glow"] = bool(emit is not None and emit_strength > 0) or bool(veins is not None and vein_strength > 0)
    return m


def _ramp(nt, fac, stops):
    """A colour ramp node on `fac` (0..1) through [(pos, colour)] stops."""
    cr = _n(nt, "ShaderNodeValToRGB")
    cr.color_ramp.elements[0].position, cr.color_ramp.elements[0].color = stops[0][0], stops[0][1]
    cr.color_ramp.elements[1].position, cr.color_ramp.elements[1].color = stops[-1][0], stops[-1][1]
    for pos, c in stops[1:-1]:
        el = cr.color_ramp.elements.new(pos)
        el.color = c
    nt.links.new(fac, cr.inputs["Fac"])
    return cr.outputs["Color"]


def _scratch_cells(nt, co, scale, seed):
    """A vein / crack network: thin lines at the walls of a voronoi (warped by noise)."""
    wn = _noise(nt, co, scale * 0.3, 3.0, 0.5, seed=seed + 61)
    mp = _n(nt, "ShaderNodeMapping")
    mp.inputs["Location"].default_value = (seed * 1.7, seed * 2.9, seed * 0.7)
    nt.links.new(co, mp.inputs["Vector"])
    add = _n(nt, "ShaderNodeVectorMath", operation="ADD")
    nt.links.new(mp.outputs[0], add.inputs[0])
    sc = _n(nt, "ShaderNodeVectorMath", operation="SCALE")
    sc.inputs["Scale"].default_value = 0.9
    sep_v = _n(nt, "ShaderNodeCombineXYZ")
    nt.links.new(wn, sep_v.inputs[0])
    nt.links.new(wn, sep_v.inputs[1])
    nt.links.new(wn, sep_v.inputs[2])
    nt.links.new(sep_v.outputs[0], sc.inputs[0])
    nt.links.new(sc.outputs[0], add.inputs[1])
    v = _n(nt, "ShaderNodeTexVoronoi", feature="DISTANCE_TO_EDGE")
    v.inputs["Scale"].default_value = scale
    nt.links.new(add.outputs[0], v.inputs["Vector"])
    return _map_range(nt, v.outputs["Distance"], 0.0, 0.07, 1.0, 0.0)


def _math(nt, op, a, b=None, c=None):
    n = nt.nodes.new("ShaderNodeMath")
    n.operation = op
    for i, v in enumerate((a, b, c)):
        if v is None:
            continue
        if isinstance(v, (int, float)):
            n.inputs[i].default_value = v
        else:
            nt.links.new(v, n.inputs[i])
    return n.outputs[0]


def _clamp01(nt, a):
    n = nt.nodes.new("ShaderNodeClamp")
    nt.links.new(a, n.inputs[0])
    return n.outputs[0]


def _mix(nt, a, b, fac):
    n = nt.nodes.new("ShaderNodeMix")
    n.data_type = "RGBA"
    nt.links.new(fac, n.inputs["Factor"])
    nt.links.new(a, n.inputs["A"])
    nt.links.new(b, n.inputs["B"])
    return n.outputs["Result"]


def _mix_f(nt, a, b, fac):
    n = nt.nodes.new("ShaderNodeMix")
    n.data_type = "FLOAT"
    nt.links.new(fac, n.inputs["Factor"])
    if isinstance(a, (int, float)):
        n.inputs["A"].default_value = a
    else:
        nt.links.new(a, n.inputs["A"])
    if isinstance(b, (int, float)):
        n.inputs["B"].default_value = b
    else:
        nt.links.new(b, n.inputs["B"])
    return n.outputs["Result"]


# ================================================================ objects: bookkeeping
def orient_solid(ob):
    """Makes a closed solid point outward by the exact signed volume (the divergence theorem). bmesh's recalc_face_normals gets thin solids (a feather, a
    blade tip) wrong; an inside-out piece bakes black. Open surfaces are left alone."""
    me = ob.data
    n = len(me.polygons)
    if n == 0:
        return ob
    ei = np.zeros(len(me.loops), np.int32)
    me.loops.foreach_get("edge_index", ei)
    if (np.bincount(ei, minlength=len(me.edges)) % 2).any():
        return ob
    co = np.empty(len(me.vertices) * 3, np.float64)
    me.vertices.foreach_get("co", co)
    co = co.reshape(-1, 3)
    vol = 0.0
    for p in me.polygons:
        vs = co[list(p.vertices)]
        c = vs.mean(axis=0)
        for k in range(len(vs)):
            a, b = vs[k], vs[(k + 1) % len(vs)]
            vol += np.dot(c, np.cross(a - c, b - c)) / 6.0 * 1.0
    if vol < 0:
        me.flip_normals()
        me.update()
    return ob


def _finish_obj(ob, part, smooth=True, angle=42.0):
    orient_solid(ob)
    if smooth:
        ob.data.shade_smooth()
        ob.data.set_sharp_from_angle(angle=math.radians(angle))
    return ob


def mk(name, verts, faces, material, part=None, detail=False, smooth=True, angle=42.0):
    """A mesh object (bl_lib.obj) with smooth shading and sharp edges above `angle`, so the baked high-poly shading is not faceted."""
    ob = bl.obj(name, verts, faces, material, part or name, detail)
    return _finish_obj(ob, part, smooth, angle)


def mk_bm(name, bm, material, part=None, detail=False, smooth=True, angle=42.0, outward=True):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    if material is not None:
        me.materials.append(material)
    ob["part"] = part or name
    if detail:
        ob["detail"] = True
    if outward:
        bl.outward(ob)
    return _finish_obj(ob, part, smooth, angle)


def xf(ob, M):
    """Transforms an object's points by a 4x4 matrix (keeps outward faces when the matrix mirrors)."""
    ob.data.transform(M)
    if M.determinant() < 0:
        ob.data.flip_normals()
    ob.data.update()
    return ob


def xf_all(obs, M):
    for o in obs:
        xf(o, M)
    return obs


def frame(origin, zdir, up=(0, 1, 0)):
    """A matrix that takes a part built at the origin facing +z (its y 'up') to `origin`, facing `zdir`, up hint `up`."""
    z = V(zdir).normalized()
    u = V(up)
    if abs(z.dot(u.normalized())) > 0.98:
        u = V((1, 0, 0))
    x = u.cross(z).normalized()
    y = z.cross(x).normalized()
    M = Matrix(((x.x, y.x, z.x, origin[0]), (x.y, y.y, z.y, origin[1]), (x.z, y.z, z.z, origin[2]), (0, 0, 0, 1)))
    return M


def mirror_x(obs, name_suffix="_m"):
    """Mirrored copies of one or many objects (same part names)."""
    obs = obs if isinstance(obs, (list, tuple)) else [obs]
    return [bl.mirror(o, "x", o.name + name_suffix) for o in obs]


def asym_mirror(obs, rng_, jitter=0.08, rot=3.0):
    """A mirrored twin that is NOT an exact twin: slightly scaled and rotated (so a symmetric guard still reads hand-made)."""
    outs = []
    for o in (obs if isinstance(obs, (list, tuple)) else [obs]):
        c = bl.mirror(o, "x", o.name + "_m")
        s = 1.0 + rng_.uniform(-jitter, jitter)
        bl.move(c, (rng_.uniform(-0.3, 0.3), rng_.uniform(-0.4, 0.4), 0), (0, 0, rng_.uniform(-rot, rot)), (s, 1.0 + rng_.uniform(-jitter, jitter), 1.0))
        outs.append(c)
    return outs


def displace(ob, amp, scale, seed=0, octaves=3, along="normal", mask=None, vector=None):
    """Noise displacement of every vertex (organic wobble, hammered dents, chips): the silhouette stops being a perfect shape.
    along 'normal' pushes along the vertex normal; or pass vector=(x, y, z) for a fixed direction; mask(co) -> 0..1 scales it."""
    me = ob.data
    me.update()
    for v in me.vertices:
        k = noise3(v.co, scale, seed, octaves) * amp
        if mask:
            k *= mask(v.co)
        d = V(vector) if vector is not None else v.normal
        v.co = v.co + d * k
    me.update()
    return ob


def smooth_shade(ob, angle=42.0):
    return _finish_obj(ob, None, True, angle)


def bevel_edges(ob, w=0.18, seg=1, angle=35.0):
    return bl.bevel(ob, w, seg, angle)


def quad_estimate(count_detail=False):
    """Quad lines the scene will export (body only; n-gons counted as the quads they become). The glow copies (quads whose skin area holds glow) come on
    top after the bake: `report()` has the true number."""
    q = 0
    for o in bpy.context.scene.collection.objects:
        if o.type != "MESH" or (o.get("detail") and not count_detail):
            continue
        for p in o.data.polygons:
            k = len(p.vertices)
            q += 1 if k <= 4 else (k - 2 + 1) // 2
    return q


def quad_table(top=12):
    """Quads per part name (body objects only), biggest first: where the budget goes."""
    t = {}
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail"):
            k = o.get("part", o.name)
            for p in o.data.polygons:
                n = len(p.vertices)
                t[k] = t.get(k, 0) + (1 if n <= 4 else (n - 2 + 1) // 2)
    rows = sorted(t.items(), key=lambda kv: -kv[1])
    return rows[:top]


# ================================================================ blades, plates, strips
def merge_rows(L, R, axis=1):
    """Pairs two edge polylines (3D points) into rows (l, r): walks both along `axis`, advancing the one whose next point is lower. The two edges can
    have different breakpoints, different tips (a slanted break) - this is what makes chips, notches and asymmetric silhouettes possible."""
    i = j = 0
    rows = [(V(L[0]), V(R[0]))]
    while i < len(L) - 1 or j < len(R) - 1:
        if i >= len(L) - 1:
            j += 1
        elif j >= len(R) - 1:
            i += 1
        elif L[i + 1][axis] <= R[j + 1][axis]:
            i += 1
        else:
            j += 1
        rows.append((V(L[i]), V(R[j])))
    return rows


def strip(name, rows, material, vs=(-1.0, -0.8, 0.8, 1.0), hs=(0.0, 1.0, 1.0, 0.0), thick=1.0, part=None, up=None, detail=False, caps=(True, True),
          smooth=True, angle=42.0, wobble=None):
    """A solid ribbon between pairs of edge points. For each row the surface lies between left and right; `vs` are positions across (-1 left .. +1 right) and
    `hs` the height factor there (0 at a sharp edge), `thick` a number or function(t of the row 0..1) -> half thickness. Both faces are built (a closed
    solid ring per row), so the strip is never see-through. `up` forces the facing direction (default: across x along). wobble(p, t) -> offset adds noise."""
    nrows = len(rows)
    rings = []
    fuv = []
    for k, (l, r) in enumerate(rows):
        t = k / max(nrows - 1, 1)
        c = (l + r) * 0.5
        c0, c1 = (rows[max(k - 1, 0)][0] + rows[max(k - 1, 0)][1]) * 0.5, (rows[min(k + 1, nrows - 1)][0] + rows[min(k + 1, nrows - 1)][1]) * 0.5
        along = c1 - c0
        across = r - l
        if up is not None:
            n = V(up(t) if callable(up) else up).normalized()
        else:
            n = across.cross(along)
            if n.length < 1e-6:
                n = V((0, 0, 1))
            n.normalize()
        th = thick(t) if callable(thick) else thick
        m = len(vs)
        top = []
        bot = []
        for vi, h in zip(vs, hs):
            p = l + across * ((vi + 1.0) * 0.5)
            top.append(p + n * (th * h))
            bot.append(p - n * (th * h))
        ring = top + bot[-2:0:-1]
        if wobble:
            ring = [q + V(wobble(q, t)) for q in ring]
        rings.append(ring)
        fuv += [(t, vi, 1.0) for vi in vs] + [(t, vi, -1.0) for vi in vs[-2:0:-1]]
    ob = _loft(name, rings, material, caps, part, detail, smooth, angle)
    a = ob.data.attributes.new("fuv", "FLOAT_VECTOR", "POINT")
    a.data.foreach_set("vector", np.asarray(fuv, np.float32).ravel())
    return ob


def _loft(name, rings, material, caps=(True, True), part=None, detail=False, smooth=True, angle=42.0):
    ob = bl.loft(name, rings, material, caps, part, detail=detail)
    return _finish_obj(ob, part, smooth, angle)


def blade(name, material, edgeL, edgeR, *, thick=1.3, fuller=0.0, fuller_depth=0.45, fuller_off=0.0, bevel_w=0.22, part="blade", wob=0.0, seed=0, detail=False,
          section=None):
    """A blade, from two edge polylines (use `blade_edge()` for each). Section: an edge bevel on both sides, optionally a fuller (a groove, `fuller` = its
    half width as a share of the half blade, `fuller_off` moves it off centre), a slightly warped surface (`wob`). `thick`: half thickness, number or
    function(t of the blade 0..1). Because the edges are independent polylines the blade can have a broken slanted tip, chips and stepped notches."""
    rows = merge_rows(edgeL, edgeR)
    if section is None:
        if fuller > 0:
            f0, f1 = fuller_off - fuller, fuller_off + fuller
            vs = (-1.0, -(1 - bevel_w), f0, f1, 1 - bevel_w, 1.0)
            hs = (0.0, 1.0, 1.0 - fuller_depth, 1.0 - fuller_depth, 1.0, 0.0)
        else:
            vs, hs = (-1.0, -(1 - bevel_w), 1 - bevel_w, 1.0), (0.0, 1.0, 1.0, 0.0)
    else:
        vs, hs = section
    wobble = (lambda p, t: (0, 0, wob * noise3(p, 0.35, seed, 3))) if wob else None
    return strip(name, rows, material, vs, hs, thick, part=part, up=(0, 0, 1), wobble=wobble, detail=detail)


def blade_edge(side, stations, y0, y1, *, notches=(), jitter=0.0, res=3.0, seed=0, tip=None, step_style=False):
    """One edge of a blade as a 3D polyline (x, y, 0). side: -1 left, +1 right. stations [(y, half width)] give the base outline. notches: [(y, depth, width, kind)]
    cut into this edge: kind 'v' (a triangular nick), 'chip' (a rounded gouge), 'step' (a stepped notch: abrupt step in, then a slow slant back out like a
    chopped edge), 'tooth' (a sawtooth). jitter: random-walk roughness of the edge (cm). tip: (x, y) end point (a slanted break = different tips on the two
    edges); default the last station."""
    r = random.Random(seed)
    ys = sorted({y0, y1, *[s[0] for s in stations if y0 <= s[0] <= y1]})
    k = y0
    while k < y1:
        ys.append(k)
        k += res
    for (ny, nd, nw, kind) in notches:
        for d in ((-0.5, 0.0, 0.5) if kind in ("v", "tooth") else (-0.5, -0.2, 0.2, 0.5) if kind == "chip" else (-0.5, -0.49, 0.0, 0.5)):
            ys.append(ny + d * nw)
    ys = sorted({round(min(max(y, y0), y1), 3) for y in ys})
    pts = []
    walk = 0.0
    for y in ys:
        w = interp(stations, y)
        cut = 0.0
        for (ny, nd, nw, kind) in notches:
            u = (y - ny) / nw
            if abs(u) > 0.5:
                continue
            if kind == "v":
                cut = max(cut, nd * (1 - abs(u) * 2))
            elif kind == "chip":
                cut = max(cut, nd * (1.0 if abs(u) <= 0.2 else (0.5 - abs(u)) / 0.3))
            elif kind == "step":
                cut = max(cut, nd if u < -0.49 else nd * (1 - (u + 0.49) / 0.99) if u < 0.5 else 0)
            elif kind == "tooth":
                cut = max(cut, nd * (u + 0.5))
        walk = walk * 0.7 + r.uniform(-jitter, jitter)
        pts.append(V((side * max(w - cut + walk, 0.05), y, 0.0)))
    if tip is not None:
        pts[-1] = V((tip[0], tip[1], 0.0))
    return pts


def plate(name, outline, z0, z1, material, part=None, detail=False, bevel_w=0.0, smooth=True):
    """bl.plate (an extruded 2D outline) + an optional bevel; smooth shaded."""
    ob = bl.plate(name, outline, z0, z1, material, part, detail)
    if bevel_w > 0:
        bl.bevel(ob, bevel_w, 1, 30)
    return _finish_obj(ob, part, smooth, 30.0)


def inset_poly(pts, d):
    """The polygon offset inward by d (positive) / outward (negative), vertex-normal miter (fine for the convex-ish outlines of guards and plates)."""
    n = len(pts)
    area = sum(pts[i][0] * pts[(i + 1) % n][1] - pts[(i + 1) % n][0] * pts[i][1] for i in range(n)) * 0.5
    sgn = 1.0 if area > 0 else -1.0
    out = []
    for i in range(n):
        p0, p1, p2 = V((*pts[i - 1][:2], 0)), V((*pts[i][:2], 0)), V((*pts[(i + 1) % n][:2], 0))
        e1, e2 = (p1 - p0).normalized(), (p2 - p1).normalized()
        n1, n2 = V((-e1.y, e1.x, 0)) * sgn, V((-e2.y, e2.x, 0)) * sgn
        b = (n1 + n2)
        if b.length < 1e-6:
            b = n1
        b.normalize()
        k = d / max(0.35, b.dot(n1))
        q = p1 + b * k
        out.append((q.x, q.y))
    return out


def stepped_plate(name, outline, z0, material, steps=3, step_h=0.35, inset=0.7, part=None, bevel_w=0.12, detail=False, up=1):
    """A plate built as stacked, shrinking layers (a stepped, armour-plate edge like the Demon-Slayer's guard), each bevelled. Returns the layers.
    up = +1 builds toward +z from z0, -1 toward -z."""
    obs = []
    pts = list(outline)
    z = z0
    for i in range(steps):
        a, b = (z, z + up * step_h) if up > 0 else (z - step_h, z)
        obs.append(plate("%s_s%d" % (name, i), pts, a, b, material, part, detail, bevel_w))
        z += up * step_h
        pts = inset_poly(pts, inset)
    return obs


def rivets(name, points, material, r=0.35, part=None, detail=True, sides=6, height=0.55, rng_=None, jitter=0.0):
    """Domed rivets. points: [(position, normal)]; detail=True bakes them into the skin (free), False exports real geometry (sides*2 quads each). jitter: random size variance."""
    bm = bmesh.new()
    rr = rng_ or random.Random(1)
    for (p, nrm) in points:
        M = frame(p, nrm)
        k = r * (1.0 + rr.uniform(-jitter, jitter))
        ring0 = [bm.verts.new(M @ V((k * math.cos(TAU * i / sides), k * math.sin(TAU * i / sides), 0.0))) for i in range(sides)]
        ring1 = [bm.verts.new(M @ V((k * 0.82 * math.cos(TAU * i / sides), k * 0.82 * math.sin(TAU * i / sides), k * height * 0.8))) for i in range(sides)]
        top = bm.verts.new(M @ V((0, 0, k * height)))
        for i in range(sides):
            j = (i + 1) % sides
            bm.faces.new((ring0[i], ring0[j], ring1[j], ring1[i]))
            bm.faces.new((ring1[i], ring1[j], top))
    return mk_bm(name, bm, material, part or name, detail, True, 60.0)


def rivet_line(p0, p1, normal, spacing, wob=0.0, rng_=None):
    """Evenly spaced (position, normal) points along a line (a seam of rivets)."""
    a, b = V(p0), V(p1)
    n = max(1, int(round((b - a).length / spacing)))
    rr = rng_ or random.Random(2)
    return [(a + (b - a) * (i / n) + V((rr.uniform(-wob, wob), rr.uniform(-wob, wob), 0)), normal) for i in range(n + 1)]


def seam(name, pts, material, w=0.12, depth=0.1, z=0.0, side=1, part=None, detail=True):
    """A plate seam / engraved line: a thin raised (or, with a dark material, scored) strip along 2D points (x, y), lying on the z surface (side +1/-1). Bake-only by default."""
    path = [(x, y, z * side) for (x, y) in pts]
    return bl.sweep(name, path, [(-w, -0.01), (w, -0.01), (w * 0.8, depth), (-w * 0.8, depth)], material, up=(0, 0, side), part=part or name, detail=detail)


def bar(name, p0, p1, w, t, material, part=None, taper=1.0, bevel_w=0.15, up=(0, 0, 1), detail=False):
    """A flat or chunky bar between two points (section w x t half sizes), optionally tapered toward p1 (taper = end scale), bevelled."""
    ob = bl.sweep(name, [V(p0), V(p1)], bl.rrect(w, t, min(w, t) * 0.35, 2), material, scale=lambda s: (1.0 + (taper - 1.0) * s, 1.0 + (taper - 1.0) * s * 0.6), up=up, part=part or name, detail=detail)
    if bevel_w > 0:
        bl.bevel(ob, bevel_w, 1, 35)
    return _finish_obj(ob, part, True, 40.0)


def tube(name, pts, radius, material, sides=8, sub=3, part=None, taper=None, detail=False, bumps=0.0, seed=0):
    """bl.tube with smooth shading and optional organic radius bumps (bone, root, vine)."""
    if bumps:
        base = (lambda t: taper[0] + (taper[1] - taper[0]) * t) if taper else (lambda t: radius)
        radius_f = lambda t: base(t) * (1.0 + bumps * noise3((t * 7.0, 0, 0), 1.0, seed))     # noqa: E731
        ob = bl.tube(name, pts, radius_f, material, sides, sub, part, detail=detail)
    else:
        ob = bl.tube(name, pts, radius, material, sides, sub, part, taper, detail)
    return _finish_obj(ob, part, True, 55.0)


def tapered(name, pts, r0, r1, material, sides=6, sub=2, part=None, detail=False, bulge=None):
    """A thorn / claw / horn through points (smoothed): radius r0 at the root, r1 at the tip, optional bulge(t) multiplier."""
    f = lambda t: (r0 + (r1 - r0) * t) * (bulge(t) if bulge else 1.0)      # noqa: E731
    ob = bl.tube(name, pts, f, material, sides, sub, part, detail=detail)
    return _finish_obj(ob, part, True, 55.0)


# ================================================================ cloth, leather, wraps
def ribbon(name, path, width, material, *, facing=(0, 0, 1), thick=0.08, part=None, fray=0.0, rng_=None, edge_wave=0.0, notch=0.0, detail=False):
    """A cloth / leather ribbon along a 3D path. width: number or function(t) -> full width. The ribbon's flat side faces `facing` (a vector or function(t)).
    fray: a ragged torn end (the last rows get random lengths); notch: a swallow-tail V cut at the end; edge_wave: wavy edges. Hanging tails, sashes, banners."""
    rr = rng_ or random.Random(3)
    P = [V(p) for p in path]
    n = len(P)
    L, R = [], []
    for i, p in enumerate(P):
        t = i / max(n - 1, 1)
        tan = (P[min(i + 1, n - 1)] - P[max(i - 1, 0)]).normalized()
        f = V(facing(t) if callable(facing) else facing)
        ac = tan.cross(f)
        if ac.length < 1e-6:
            ac = V((1, 0, 0))
        ac.normalize()
        w = (width(t) if callable(width) else width) * 0.5
        if edge_wave:
            w *= 1.0 + edge_wave * math.sin(t * 17.0 + rr.uniform(0, 0.4))
        endcut = 1.0
        if notch and t > 0.82:
            endcut = 1.0
        L.append(p - ac * w)
        R.append(p + ac * w)
    if notch:                                                               # swallow-tail: the centre of the last rows lags behind the edges
        for i in range(max(1, n // 6)):
            k = n - 1 - i
            tt = 1.0 - i / max(1, n // 6)
            tan = (P[min(k + 1, n - 1)] - P[max(k - 1, 0)]).normalized()
            L[k] = L[k] + tan * notch * tt
            R[k] = R[k] + tan * notch * tt * 0.6
    if fray:
        for i in range(max(2, n // 6)):
            k = n - 1 - i
            tan = (P[min(k + 1, n - 1)] - P[max(k - 1, 0)]).normalized()
            L[k] = L[k] - tan * rr.uniform(0, fray)
            R[k] = R[k] - tan * rr.uniform(0, fray)
    rows = list(zip(L, R))
    return strip(name, rows, material, (-1.0, 0.0, 1.0), (0.45, 1.0, 0.45), thick, part=part, up=(lambda t: facing(t)) if callable(facing) else facing, detail=detail, angle=60)


def helix_wrap(name, y0, y1, r, material, *, turns=8.0, cord=0.32, pitch_wobble=0.05, axis_off=(0, 0), squash=1.0, part=None, seed=0, rng_=None, sub=6, sides=6):
    """A cord / leather strip wound round a vertical grip in a spiral (real geometry, not a texture): `turns` turns from y0 to y1 at radius r, cord = half width.
    Each turn is slightly irregular (a hand-wound wrap)."""
    rr = rng_ or random.Random(seed + 5)
    n = int(turns * sub)
    path = []
    for i in range(n + 1):
        t = i / n
        a = TAU * turns * t
        rad = r + 0.04 * math.sin(a * 3.0) + 0.03 * rr.uniform(-1, 1)
        y = y0 + (y1 - y0) * t + pitch_wobble * math.sin(a * 2.0) * (y1 - y0) / turns
        path.append(V((axis_off[0] + rad * math.cos(a), y, axis_off[1] + rad * math.sin(a) * squash)))
    sec = [(-0.06, -cord), (0.1, -cord * 0.9), (0.16, 0.0), (0.1, cord * 0.9), (-0.06, cord)]
    ob = bl.sweep(name, path, sec, material, up=(0, 1, 0), part=part or name, caps=(True, True))
    return _finish_obj(ob, part, True, 55.0)


def grip_wrapped(name, y0, y1, r, core_mat, wrap_mat, *, turns=9.0, cord=0.34, bands=(), band_mat=None, swell=0.15, part="grip", seed=0, sides=10, squash=0.92):
    """A full grip: a swelling core (dark leather) with a wrap spiral on top, and metal bands (`bands` = [y, ...]). Returns the objects."""
    obs = []
    n = 9
    prof = [(r * (1 + swell * math.sin(math.pi * i / (n - 1))) * 0.9, y0 + (y1 - y0) * i / (n - 1)) for i in range(n)]
    core = bl.lathe(name + "_core", prof, core_mat, sides=sides, squash=squash, part=part)
    obs.append(_finish_obj(core, part))
    obs.append(helix_wrap(name + "_wrap", y0 + 0.4, y1 - 0.4, r * (1 + swell * 0.7), wrap_mat, turns=turns, cord=cord, part=part, seed=seed, axis_off=(0, 0), squash=squash))
    for i, y in enumerate(bands):
        rr = r * (1 + swell * math.sin(math.pi * (y - y0) / (y1 - y0))) * 1.12
        b = bl.lathe("%s_band%d" % (name, i), [(rr - 0.15, y - 0.75), (rr + 0.12, y - 0.55), (rr + 0.2, y), (rr + 0.12, y + 0.55), (rr - 0.15, y + 0.75)], band_mat or core_mat, sides=sides, squash=squash, part=part + "_rings")
        obs.append(_finish_obj(b, part + "_rings"))
    return obs


# ================================================================ gems, filigree, runes
def gem_setting(name, center, r, gem_mat, metal_mat, *, normal=(0, 0, 1), claws=6, sides=8, crown=0.32, depth=0.55, bezel=True, part="gem", rng_=None, up=(0, 1, 0), claw_r=0.34):
    """A cut stone in a bezel ring with real claws (tapered, bent over the stone), built facing `normal` at `center`. Returns objects (stone, bezel, claws)."""
    rr = rng_ or random.Random(4)
    obs = []
    M = frame(center, normal, up)
    g = bl.gem(name, r, gem_mat, center=(0, 0, 0), depth=depth, crown=crown, sides=sides, axis="z", part=part)
    xf(g, M)
    obs.append(_finish_obj(g, part, True, 25.0))
    if bezel:
        prof = [(r * 1.28, -r * 0.2), (r * 1.34, 0.0), (r * 1.22, r * 0.34), (r * 1.02, r * 0.5), (r * 1.02, r * 0.12)]
        bz = bl.lathe(name + "_bezel", prof, metal_mat, sides=max(10, sides * 2), axis="z", caps=(False, False), part=part + "_setting")
        xf(bz, M)
        obs.append(_finish_obj(bz, part + "_setting", True, 50.0))
    for k in range(claws):
        a = TAU * (k + rr.uniform(-0.12, 0.12)) / claws
        p0 = V((r * 1.15 * math.cos(a), r * 1.15 * math.sin(a), 0.0))
        p1 = V((r * 1.16 * math.cos(a), r * 1.16 * math.sin(a), r * 0.55))
        p2 = V((r * 0.78 * math.cos(a), r * 0.78 * math.sin(a), r * 0.84))
        c = tapered("%s_claw%d" % (name, k), [M @ p0, M @ p1, M @ p2], claw_r * r / 2.0 + 0.1, 0.1, metal_mat, sides=4, sub=1, part=part + "_setting")
        obs.append(c)
    return obs


def filigree(name, pts, material, w=0.5, h=0.25, z=0.0, side=1, sub=4, taper=(1.0, 0.35), part=None, detail=False, round_profile=True):
    """A thin scroll / vine of metal lying on a z surface (side +1/-1): a rounded bead swept along 2D points (x, y), tapering to its end."""
    path = [V((p[0], p[1], z * side)) for p in bl.smooth_path([V((p[0], p[1], 0)) for p in pts], sub)]
    sec = [(-1.0, 0.0), (-0.75, 0.55), (0.0, 1.0), (0.75, 0.55), (1.0, 0.0)] if round_profile else [(-1.0, 0.0), (-0.8, 1.0), (0.8, 1.0), (1.0, 0.0)]
    sec = [(u, v * h) for u, v in sec]
    ob = bl.sweep(name, path, sec, material, up=(0, 0, side), scale=lambda t: (w * (taper[0] + (taper[1] - taper[0]) * t), 1.0), part=part or name, detail=detail, caps=(True, True))
    return _finish_obj(ob, part, True, 55.0)


def spiral(center, r0, r1, turns, start=0.0, n=14, flip=1, squash=1.0, drift=(0.0, 0.0)):
    """2D points of a spiral from radius r0 to r1 (a curl: scroll ends, vine tendrils, wave crests)."""
    pts = []
    for i in range(n + 1):
        t = i / n
        a = start + flip * TAU * turns * t
        r = r0 + (r1 - r0) * t
        pts.append((center[0] + r * math.cos(a) + drift[0] * t, center[1] + r * math.sin(a) * squash + drift[1] * t))
    return pts


def runes(name, text, size, material, loc, rot=(0, 0, 0), extrude=0.05, spacing=1.3, detail=True, font=None, part=None):
    """Extruded rune / letter geometry (a real text mesh). detail=True: a bake-only relief a hair above its surface (free); False: exported geometry."""
    return bl.text(name, text, size, material, loc, rot, extrude=extrude, font=font or FONT_RUNIC, spacing=spacing, detail=detail, part=part)


def engrave_lines(name, lines, material, side=1, z=0.0, w=0.07, part=None, detail=True):
    """A set of seams (polylines of 2D points) at once: engraved ornament, plate edges, hammered pattern lines."""
    return [seam("%s%d" % (name, i), l, material, w=w, z=z, side=side, part=part or name, detail=detail) for i, l in enumerate(lines)]


# ================================================================ arrays: feathers, petals, scales, spikes
def leaf_rows(base, direction, length, width, *, bend=(0, 0, 0), curl=0.0, profile=None, rows=11, serrate=0.0, tip_pull=0.0, facing=None, seed=0, droop=0.0):
    """Edge polylines (L, R) of a feather / leaf / petal / flame tongue: from `base` along `direction` for `length`, `width` wide at its widest, pushed sideways by `bend`
    (a 3D vector at the tip, quadratic) and curling at the tip by `curl` (cm, along `facing`). profile(t) -> 0..1 width factor (default a fat leaf).
    serrate: barb notches in the edge (alternate in/out by that fraction): a real feather vane. Returns (L, R, centre path)."""
    r = random.Random(seed)
    d = V(direction).normalized()
    f = V(facing) if facing is not None else V((0, 0, 1))
    ac = d.cross(f)
    if ac.length < 1e-6:
        ac = V((1, 0, 0))
    ac.normalize()
    fz = ac.cross(d).normalized()
    prof = profile or (lambda t: math.sin(math.pi * min(1.0, t ** 0.75)) ** 0.7)
    L, R, C = [], [], []
    for i in range(rows + 1):
        t = i / rows
        c = V(base) + d * (length * t) + V(bend) * (t * t) + fz * (curl * t ** 3 + droop * t * t)
        C.append(c)
    for i, c in enumerate(C):
        t = i / rows
        tan = (C[min(i + 1, rows)] - C[max(i - 1, 0)]).normalized()
        a = tan.cross(fz)
        if a.length < 1e-6:
            a = ac
        a.normalize()
        w = width * 0.5 * prof(t)
        s = 1.0 - (serrate if (i % 2 == 1 and 0.1 < t < 0.96) else 0.0) * r.uniform(0.7, 1.3)
        s2 = 1.0 - (serrate if (i % 2 == 0 and 0.1 < t < 0.96) else 0.0) * r.uniform(0.7, 1.3)
        pull = tan * (tip_pull * t * w)
        L.append(c - a * w * s + pull * 0.0)
        R.append(c + a * w * s2)
    return L, R, C


def feather(name, base, direction, length, width, material, *, shaft_material=None, bend=(0, 0, 0), curl=0.0, rows=10, serrate=0.22, facing=(0, 0, 1), thick=0.16, part=None,
            seed=0, profile=None, droop=0.0, ridge=1.6, detail=False, compact=True):
    """One feather: a serrated vane (barb notches, a raised central rib, a slight camber) along a curved quill. ~rows * 6 quads. Returns the object."""
    L, R, C = leaf_rows(base, direction, length, width, bend=bend, curl=curl, rows=rows, serrate=serrate, facing=facing, seed=seed, profile=profile, droop=droop)
    rows_ = list(zip(L, R))
    vs, hs = ((-1.0, 0.0, 1.0), (0.0, 1.0, 0.0)) if compact else ((-1.0, -0.12, 0.12, 1.0), (0.0, 0.9, 0.9, 0.0))
    ob = strip(name, rows_, material, vs, hs, lambda t: thick * (1.0 + (ridge - 1.0) * (1 - t) * 0.6) * (0.4 + 0.6 * (1 - t)),
               part=part or name, up=facing, detail=detail, angle=70)
    return ob


def fan_feathers(prefix, center, count, a0, a1, length, width, material, rng_, *, len_jit=0.12, ang_jit=3.0, wid_jit=0.15, layer_dz=0.28, shaft_dir=(1, 0, 0),
                 serrate=0.2, rows=9, curl=2.0, bend=0.0, part=None, plane_normal=(0, 0, 1), thick=0.14, cascade=0.0, mat2=None, droop=0.0, compact=True):
    """A fan of feathers (a wing, a tail, a crest) laid in layers: angles from a0 to a1 degrees in the xy plane (0 = +x), each with seeded variation in length,
    angle, width and a layered z offset so they overlap like real plumage. cascade grows the length toward the a1 end. mat2 alternates the material."""
    out = []
    for i in range(count):
        u = i / max(count - 1, 1)
        a = math.radians(lerp(a0, a1, u) + rng_.uniform(-ang_jit, ang_jit))
        ln = length * (1.0 + cascade * u) * (1.0 + rng_.uniform(-len_jit, len_jit))
        wd = width * (1.0 + rng_.uniform(-wid_jit, wid_jit))
        d = V((math.cos(a), math.sin(a), 0))
        side = V((-d.y, d.x, 0))
        z = (i % 2) * layer_dz + i * layer_dz * 0.2
        out.append(feather("%s%d" % (prefix, i), V(center) + V((0, 0, z)) * V(plane_normal), d, ln, wd, (mat2 if (mat2 and i % 2) else material), bend=side * (bend * rng_.uniform(0.5, 1.5)),
                           curl=curl * rng_.uniform(0.5, 1.5), serrate=serrate, rows=rows, part=part or prefix, seed=rng_.randint(0, 9999), thick=thick, droop=droop, compact=compact))
    return out


def scale_plate(name, center, normal, size, material, *, up=(0, 1, 0), thick=0.14, rows=4, part=None, aspect=1.15, tilt=14.0, shape="round", detail=False):
    """One overlapping scale / armour tile (round, pointed or shield-shaped), its lower edge lifted by `tilt` degrees so rows of them overlap like a dragon hide."""
    M = frame(center, normal, up)
    pts = []
    for i in range(rows * 2 + 1):
        t = i / (rows * 2)
        x = size * (t * 2 - 1)
        pts.append(x)
    L, R = [], []
    for i in range(rows + 1):
        t = i / rows
        yy = -size * aspect * t
        if shape == "round":
            w = size * math.sqrt(max(0.02, 1 - (1.0 - 2.0 * t * 0.5 - 0.0) ** 2 * 0.0)) * (1 - t ** 2.2) ** 0.5
        else:
            w = size * (1 - t) ** 0.9
        z = math.sin(math.radians(tilt)) * size * aspect * t
        L.append(M @ V((-w, yy, z)))
        R.append(M @ V((w, yy, z)))
    rows_ = list(zip(L, R))
    ob = strip(name, rows_, material, (-1.0, -0.4, 0.4, 1.0), (0.0, 1.0, 1.0, 0.0), thick, part=part or name, up=M.to_3x3() @ V((0, 0, 1)), detail=detail, angle=60)
    return ob


def spike_array(prefix, base_pts, directions, lengths, r, material, rng_, *, bend=0.0, sides=4, part=None, len_jit=0.25, curve=0.0):
    """Spikes / thorns / teeth at the given base points: each tapered, bent (seeded), with varied length: a row of teeth is never 8 identical cones."""
    out = []
    for i, (b, d) in enumerate(zip(base_pts, directions)):
        ln = lengths[i] if isinstance(lengths, (list, tuple)) else lengths
        ln *= 1.0 + rng_.uniform(-len_jit, len_jit)
        dv = V(d).normalized()
        sidev = dv.cross(V((0, 0, 1)))
        mid = V(b) + dv * ln * 0.5 + sidev * rng_.uniform(-bend, bend) + V((0, 0, rng_.uniform(-bend, bend)) ) * 0.3 + dv.cross(sidev) * curve * ln
        tip = V(b) + dv * ln + sidev * rng_.uniform(-bend, bend) * 1.4 + dv.cross(sidev) * curve * ln * 2.2
        out.append(tapered("%s%d" % (prefix, i), [V(b), mid, tip], r * rng_.uniform(0.8, 1.2), 0.04, material, sides=sides, part=part or prefix))
    return out


# ================================================================ organic: blobs, drips, bone, wood, vines
def blob(name, center, radii, material, *, seed=0, lump=0.18, lump_scale=0.07, rings=10, segs=14, part=None, detail=False, squash=(1, 1, 1), subsurf=0, cut=None):
    """An organic lump: a UV sphere stretched to `radii`, displaced by layered noise (a gel, an egg, a heart, a stone). subsurf: extra smoothing levels.
    cut(co) -> False discards faces fully outside (a dome from a sphere)."""
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=segs, v_segments=rings, radius=1.0)
    for v in bm.verts:
        v.co = V((v.co.x * radii[0], v.co.y * radii[1], v.co.z * radii[2]))
    if cut:
        bmesh.ops.delete(bm, geom=[f for f in bm.faces if all(not cut(v.co) for v in f.verts)], context="FACES")
        bmesh.ops.delete(bm, geom=[v for v in bm.verts if not v.link_faces], context="VERTS")
    ob = mk_bm(name, bm, material, part or name, detail, True, 70.0, outward=cut is None)
    if lump:
        displace(ob, lump * max(radii), lump_scale / max(1e-6, max(radii) * 0.1), seed, 3)
    if subsurf:
        bl.subsurf(ob, subsurf)
    xf(ob, Matrix.Translation(V(center)))
    return _finish_obj(ob, part, True, 80.0)


def drip(name, top, length, r, material, *, seed=0, sides=8, part=None, neck=0.45, bulb=1.25, bend=(0, 0, 0)):
    """A hanging teardrop of gel from `top` down (-y by default): a thin neck swelling into a bulb, wobbled."""
    rr = random.Random(seed)
    prof = [(r * neck * 1.9, 0.0), (r * neck, length * 0.18), (r * neck * 0.85, length * 0.45), (r * 0.9, length * 0.72), (r * bulb, length * 0.9), (r * 0.8, length * 1.0), (0.02, length * 1.04)]
    rings = []
    for (rad, h) in prof:
        t = h / length
        ctr = V(top) + V((0, -h, 0)) + V(bend) * (t * t)
        rings.append([ctr + V((rad * math.cos(TAU * k / sides + rr.uniform(-0.08, 0.08)), 0, rad * math.sin(TAU * k / sides)) ) * V((1 + rr.uniform(-0.05, 0.05), 1, 1)) for k in range(sides)])
    return _loft(name, rings, material, (True, True), part, False, True, 70.0)


def bone(name, p0, p1, r, material, *, knob=1.7, sub=5, sides=8, part=None, bow=(0, 0, 0), seed=0, ridge=0.08, detail=False):
    """A long bone: a shaft with two knobbed ends, a slight bow, a little knobbly irregularity."""
    a, b = V(p0), V(p1)
    mid = (a + b) * 0.5 + V(bow)
    path = bl.smooth_path([a, (a + mid) * 0.5 + V(bow) * 0.2, mid, (mid + b) * 0.5 + V(bow) * 0.2, b], sub)

    def rad(t):
        knobs = knob * (math.exp(-((t) / 0.07) ** 2) + math.exp(-((1 - t) / 0.07) ** 2))
        return r * (0.82 + knobs * 0.55 + ridge * noise3((t * 9, 0, 0), 1.0, seed))
    ob = bl.sweep(name, path, bl.circle(1.0, sides), material, scale=rad, up=(0, 0, 1), part=part or name, detail=detail)
    return _finish_obj(ob, part, True, 60.0)


def wood_branch(name, pts, r0, r1, material, *, seed=0, sides=9, knots=2, part=None, bump=0.12, sub=4, detail=False):
    """A bark-textured branch / root / staff: smoothly tapering, with irregular radius, knots and a slight twist."""
    rr = random.Random(seed)
    kn = [rr.uniform(0.15, 0.85) for _ in range(knots)]

    def rad(t):
        base = r0 + (r1 - r0) * t
        k = sum(0.5 * math.exp(-((t - c) / 0.04) ** 2) for c in kn)
        return base * (1.0 + bump * noise3((t * 6, 1.3, 0), 1.0, seed) + k)
    path = bl.smooth_path([V(p) for p in pts], sub)
    ob = bl.sweep(name, path, bl.circle(1.0, sides), material, scale=rad, up=(0, 0, 1), twist=lambda t: t * 0.8, part=part or name, detail=detail)
    return _finish_obj(ob, part, True, 60.0)


def vine(name, pts, r, material, leaves=0, leaf_mat=None, rng_=None, leaf_len=3.0, part=None, sides=6, seed=0):
    """A twisting vine with seeded leaves: returns [stem, leaf...]."""
    rr = rng_ or random.Random(seed)
    path = bl.smooth_path([V(p) for p in pts], 4)
    stem = tube(name, [tuple(p) for p in path], r, material, sides=sides, sub=1, part=part or name, taper=(r, r * 0.35))
    out = [stem]
    for i in range(leaves):
        k = rr.randint(2, len(path) - 3)
        p = path[k]
        tan = (path[k + 1] - path[k - 1]).normalized()
        side = tan.cross(V((0, 0, 1))) * (1 if i % 2 else -1)
        d = (side * 0.8 + tan * 0.35 + V((0, 0, rr.uniform(-0.4, 0.4)))).normalized()
        L, R, C = leaf_rows(p, d, leaf_len * rr.uniform(0.7, 1.3), leaf_len * 0.55, rows=6, facing=(0, 0, 1), seed=i, curl=rr.uniform(-0.6, 0.6))
        out.append(strip("%s_leaf%d" % (name, i), list(zip(L, R)), leaf_mat or material, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.07, part=(part or name) + "_leaf", up=(0, 0, 1)))
    return out


# ================================================================ mechanical: chains, gears, springs, links
def link(name, center, tangent, up, length, width, wire, material, *, sides=5, segs=10, part=None):
    """One chain link (an oval torus), long axis along `tangent`, flat to `up`."""
    bm = bmesh.new()
    t = V(tangent).normalized()
    u = V(up).normalized()
    s = t.cross(u).normalized()
    rings = []
    for i in range(segs):
        a = TAU * i / segs
        cx, cy = math.cos(a) * (length * 0.5 - wire), math.sin(a) * (width * 0.5 - wire)
        c = V(center) + t * cx + u * cy
        # centre line normal in the (t, u) plane
        nx, ny = math.cos(a) / max(length * 0.5 - wire, 1e-3), math.sin(a) / max(width * 0.5 - wire, 1e-3)
        nrm = (t * nx + u * ny).normalized()
        rings.append([bm.verts.new(c + nrm * (wire * math.cos(TAU * k / sides)) + s * (wire * math.sin(TAU * k / sides))) for k in range(sides)])
    for i in range(segs):
        j = (i + 1) % segs
        for k in range(sides):
            bm.faces.new((rings[i][k], rings[i][(k + 1) % sides], rings[j][(k + 1) % sides], rings[j][k]))
    return mk_bm(name, bm, material, part or name, False, True, 80.0)


def chain(name, path, link_len, link_w, wire, material, *, max_links=14, part=None, up=(0, 0, 1), sides=5, segs=8):
    """A chain along a 3D path: alternate links turned 90 degrees, evenly spaced along the path's arc length, count capped (`max_links`)."""
    P = [V(p) for p in bl.smooth_path(path, 6)]
    total = sum((P[i + 1] - P[i]).length for i in range(len(P) - 1))
    n = min(max_links, max(2, int(total / (link_len * 0.75))))
    out = []
    for i in range(n):
        d = total * (i + 0.5) / n
        acc = 0.0
        for k in range(len(P) - 1):
            seg = (P[k + 1] - P[k]).length
            if acc + seg >= d:
                tt = (d - acc) / max(seg, 1e-9)
                p = P[k].lerp(P[k + 1], tt)
                tan = (P[k + 1] - P[k]).normalized()
                break
            acc += seg
        u = V(up)
        if abs(tan.dot(u.normalized())) > 0.95:
            u = V((1, 0, 0))
        side = tan.cross(u).normalized()
        upv = side.cross(tan).normalized() if i % 2 == 0 else side
        out.append(link("%s%d" % (name, i), p, tan, upv, link_len, link_w, wire, material, sides=sides, segs=segs, part=part or name))
    return out


def gear(name, center, r_out, r_in, teeth, thick, material, *, hole=0.0, normal=(0, 0, 1), part=None, rng_=None, spokes=0, bevel_w=0.12, wear=0.0):
    """A toothed gear (extruded outline, bevelled), tooth tops slightly irregular when rng_ is given; hole radius > 0 punches an axle hole (as a ring)."""
    rr = rng_
    pts = []
    for i in range(teeth):
        a0 = TAU * i / teeth
        step = TAU / teeth
        j = (1.0 + rr.uniform(-wear, wear)) if rr else 1.0
        for da, rad in ((0.0, r_in), (0.18, r_in), (0.30, r_out * j), (0.62, r_out * j), (0.74, r_in)):
            pts.append((r_in * 0 + rad * math.cos(a0 + step * da), rad * math.sin(a0 + step * da)))
    ob = bl.plate(name, pts, -thick, thick, material, part or name)
    if bevel_w:
        bl.bevel(ob, bevel_w, 1, 35)
    xf(ob, frame(center, normal))
    return _finish_obj(ob, part, True, 40.0)


def spring(name, p0, p1, r, wire, coils, material, *, sides=5, part=None, sub=7):
    """A coil spring between two points (real helical geometry)."""
    a, b = V(p0), V(p1)
    ax = b - a
    ln = ax.length
    d = ax.normalized()
    u = d.cross(V((0, 0, 1)))
    if u.length < 0.1:
        u = d.cross(V((1, 0, 0)))
    u.normalize()
    w = d.cross(u)
    n = int(coils * sub)
    path = [a + d * (ln * i / n) + (u * math.cos(TAU * coils * i / n) + w * math.sin(TAU * coils * i / n)) * r for i in range(n + 1)]
    ob = bl.tube(name, [tuple(p) for p in path], wire, material, sides=sides, sub=1, part=part or name)
    return _finish_obj(ob, part, True, 60.0)


# ================================================================ clouds: shards, embers, fragments
def shard(name, center, size, material, rng_, *, part="shards", sides=5, stretch=2.6, tilt=None, dir_=None, facets=True):
    """One irregular crystal / fragment: a jittered prism with a skewed point (a splinter, an ember crystal, a rock chip). ~sides*2 quads."""
    bm = bmesh.new()
    d = V(dir_) if dir_ is not None else V((rng_.uniform(-1, 1), rng_.uniform(-1, 1), rng_.uniform(-0.5, 0.5)))
    if d.length < 1e-3:
        d = V((0, 1, 0))
    M = frame(center, d.normalized(), (0, 1, 0))
    rings = []
    for lvl, (zz, rad) in enumerate(((-0.35, 0.62), (0.0, 1.0), (0.5, 0.78))):
        rings.append([bm.verts.new(M @ V(((rad + rng_.uniform(-0.2, 0.2)) * size * math.cos(TAU * k / sides + rng_.uniform(-0.2, 0.2)),
                                          (rad + rng_.uniform(-0.2, 0.2)) * size * math.sin(TAU * k / sides) * 0.7, zz * size * stretch))) for k in range(sides)])
    tip = bm.verts.new(M @ V((rng_.uniform(-0.2, 0.2) * size, rng_.uniform(-0.2, 0.2) * size, size * stretch * rng_.uniform(0.9, 1.15))))
    base = bm.verts.new(M @ V((rng_.uniform(-0.1, 0.1) * size, 0, -size * stretch * rng_.uniform(0.45, 0.6))))
    for lv in range(2):
        for k in range(sides):
            j = (k + 1) % sides
            bm.faces.new((rings[lv][k], rings[lv][j], rings[lv + 1][j], rings[lv + 1][k]))
    for k in range(sides):
        j = (k + 1) % sides
        bm.faces.new((rings[2][k], rings[2][j], tip))
        bm.faces.new((rings[0][j], rings[0][k], base))
    return mk_bm(name, bm, material, part, False, not facets, 30.0)


def ember(name, center, size, material, rng_, part="embers"):
    """A tiny flat ember / ash flake / spark: a skewed diamond with a little thickness (6 quad lines)."""
    return shard(name, center, size, material, rng_, part=part, sides=3, stretch=1.2)


def shard_cloud(prefix, centre, extent, count, size, material, rng_, *, avoid=None, part="shards", min_dist=None, shell=0.0, stretch=2.4, flow=None, kinds=1, mat2=None, max_count=40, sides=4):
    """A bounded cloud of floating fragments round the weapon (static geometry: no tick cost). extent = (ex, ey, ez) half sizes of the region; avoid(p) -> True
    where the weapon itself is (placement is retried so shards never sit in the blade); size (lo, hi) random; flow(p) -> direction the shard points (a
    swirl, a spray away from the hit). The count is capped at max_count (rule 11), and no two shards overlap closer than min_dist."""
    count = min(count, max_count)
    out, pts = [], []
    tries = 0
    while len(out) < count and tries < count * 60:
        tries += 1
        p = V((rng_.uniform(-1, 1) * extent[0], rng_.uniform(-1, 1) * extent[1], rng_.uniform(-1, 1) * extent[2])) + V(centre)
        if avoid and avoid(p):
            continue
        if shell:
            u = V((p.x - centre[0]) / extent[0], (p.y - centre[1]) / extent[1], (p.z - centre[2]) / max(extent[2], 1e-3))
            if u.length < shell:
                continue
        if min_dist and any((p - q).length < min_dist for q in pts):
            continue
        pts.append(p)
        s = rng_.uniform(*size)
        d = V(flow(p)) if flow else None
        out.append(shard("%s%d" % (prefix, len(out)), p, s, (mat2 if (mat2 and len(out) % kinds == 1) else material), rng_, part=part, dir_=d, stretch=stretch, sides=sides))
    return out


# ================================================================ check, beauty, finish
def beauty(path, *, view="three_quarter", size=(700, 1000), samples=28, focus=None, dist=None, bg=(0.06, 0.07, 0.10), key=(1.0, 0.93, 0.84), rim=(0.5, 0.6, 1.0), lens=70.0, details=True):
    """A Cycles still of the model as it is NOW (before finish), under a studio rig: key light, cold rim light, a dark gradient background. This is what a
    pro would look at. view: 'front', 'three_quarter', 'side', 'back'. focus (x, y, z, width) frames a part. Written to `path` (PNG). Cleans up after itself
    (camera, lights, world are restored so the bake is untouched)."""
    sc = bpy.context.scene
    old_world, old_cam = sc.world, sc.camera
    old_res = (sc.render.resolution_x, sc.render.resolution_y, sc.render.film_transparent)
    old_samples, old_den = sc.cycles.samples, sc.cycles.use_denoising
    obs = [o for o in sc.collection.objects if o.type == "MESH" and (details or not o.get("detail"))]
    allp = np.array([tuple(o.matrix_world @ v.co) for o in obs for v in o.data.vertices])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    ctr = V(((lo + hi) / 2).tolist())
    ext = V((hi - lo).tolist())
    # world: dark vertical gradient with a soft overhead
    w = bpy.data.worlds.new("beauty")
    w.use_nodes = True
    nt = w.node_tree
    bgn = nt.nodes["Background"]
    bgn.inputs["Color"].default_value = (*bg, 1.0)
    bgn.inputs["Strength"].default_value = 0.8
    sc.world = w
    az = {"front": (0, 0), "three_quarter": (-32, 14), "side": (-90, 0), "back": (180, 0)}[view]
    cam_d = (focus[3] * 1.15 if focus else max(ext.y, ext.x) * 1.12) / (2 * math.tan(math.atan(18.0 / lens)))
    cam_d = dist or cam_d * 1.0
    target = V(focus[:3]) if focus else ctr
    yaw, pit = math.radians(az[0]), math.radians(az[1])
    cpos = target + V((math.sin(yaw) * math.cos(pit), math.sin(pit), math.cos(yaw) * math.cos(pit))) * cam_d
    cd = bpy.data.cameras.new("cam")
    cd.lens = lens
    cd.sensor_width = 36.0
    cam = bpy.data.objects.new("cam", cd)
    sc.collection.objects.link(cam)
    cam.location = cpos
    cam.matrix_world = frame(cpos, cpos - target, (0, 1, 0))
    sc.camera = cam
    lights = []

    def area(loc, energy, color, size_):
        ld = bpy.data.lights.new("l", "AREA")
        ld.energy, ld.color, ld.size = energy, color, size_
        lo_ = bpy.data.objects.new("l", ld)
        sc.collection.objects.link(lo_)
        lo_.location = loc
        lo_.rotation_euler = (target - V(loc)).to_track_quat("-Z", "Y").to_euler()
        lights.append(lo_)
    sc_ = max(ext.y, 60.0)
    area(target + V((-0.6, 0.7, 1.0)) * sc_, 9.0e5 * (sc_ / 150.0) ** 2, key, sc_ * 0.8)
    area(target + V((0.9, 0.2, -0.5)) * sc_, 5.0e5 * (sc_ / 150.0) ** 2, rim, sc_ * 0.6)
    area(target + V((0.5, -0.4, 0.9)) * sc_, 2.0e5 * (sc_ / 150.0) ** 2, (1, 1, 1), sc_ * 0.8)
    sc.render.resolution_x, sc.render.resolution_y = size
    sc.render.film_transparent = False
    sc.cycles.samples = samples
    sc.cycles.use_denoising = True
    try:
        sc.cycles.denoiser = "OPENIMAGEDENOISE"
    except Exception:
        pass
    sc.view_settings.view_transform = "Filmic" if "Filmic" in [e.identifier for e in sc.view_settings.bl_rna.properties["view_transform"].enum_items] else "AgX"
    sc.render.filepath = path
    sc.render.image_settings.file_format = "PNG"
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    t = time.time()
    bpy.ops.render.render(write_still=True)
    dt = time.time() - t
    for o in lights + [cam]:
        bpy.data.objects.remove(o)
    sc.world, sc.camera = old_world, old_cam
    sc.render.resolution_x, sc.render.resolution_y, sc.render.film_transparent = old_res
    sc.cycles.samples, sc.cycles.use_denoising = old_samples, old_den
    return dt


def _ensure_view_transform():
    sc = bpy.context.scene
    sc.view_settings.view_transform = "Standard"


def size_class(base_id, mesh_path):
    """(class, apparent length in blocks, class min, class max, status list) of a written mesh, measured like tools/relics/audit_weapon_scale.py with the
    third-person display scale of the base weapon (the form keeps the base's class)."""
    sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
    import audit_weapon_scale as aud
    import weapon_classes as wc
    data = wc.load()
    tmp = os.path.join(os.path.dirname(os.path.abspath(mesh_path)), "_audit_tmp")
    os.makedirs(tmp, exist_ok=True)
    shutil.copy(mesh_path, os.path.join(tmp, base_id + ".txt"))
    try:
        r = aud.audit_one(base_id, data, tmp)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    return r


def report(item_id, base_id, mesh_dir, skin_dir, mesh=None, atlas=None, seconds=None):
    """The self-check of a built mesh: quad lines (body + glow copies) against the 5000 budget, the size class and apparent length against the class range,
    the PNG sizes against 250 KB. Prints and returns a dict."""
    path = os.path.join(mesh_dir, item_id + ".txt")
    body = glow = 0
    for line in open(path):
        if line.startswith("qn ") or line.startswith("q "):
            if line.split()[1] == "2":
                glow += 1
            else:
                body += 1
    sk, gl = os.path.join(skin_dir, item_id + ".png"), os.path.join(skin_dir, item_id + "_glow.png")
    kb = (os.path.getsize(sk) + os.path.getsize(gl)) / 1024.0 if os.path.exists(sk) and os.path.exists(gl) else -1
    r = size_class(base_id, path) or {}
    ok_q = body + glow <= MAX_QUADS
    ok_png = 0 < kb <= MAX_PNG_KB
    ok_size = bool(r) and not r.get("status")
    out = {"id": item_id, "body_quads": body, "glow_quads": glow, "lines": body + glow, "png_kb": round(kb, 1), "class": r.get("class"), "apparent": r.get("apparent"),
           "range": (r.get("min"), r.get("max")), "status": r.get("status"), "ok_quads": ok_q, "ok_png": ok_png, "ok_size": ok_size, "seconds": round(seconds, 1) if seconds else None}
    print("[report] %s: %d body + %d glow = %d quad lines (budget %d: %s)  PNG skin+glow %.1f KB (<=%d: %s)  class %s apparent %s in %s (%s)  %s" % (
        item_id, body, glow, body + glow, MAX_QUADS, "ok" if ok_q else "OVER", kb, MAX_PNG_KB, "ok" if ok_png else "check shrink_pngs.py", out["class"], out["apparent"], out["range"],
        "in class" if ok_size else "OUT OF CLASS " + str(out["status"]), "built in %.0f s" % seconds if seconds else ""))
    return out


def finish_form(item_id, base_id, *, out_dir=None, trial=None, glow=("FFFFFF", 0.12, 0.45, 1.0), bake=1024, skin=512, header_from=None, cage=0.5, preview=True):
    """Bakes and writes the form: joins the scene, unwraps, bakes the skin (Cycles, `bake` px, boxed down to `skin` = 512) and the glow skin, writes
    <out>/weapon_meshes/<id>.txt and <out>/textures/entity/weapon/<id>(_glow).png (trial=DIR writes there instead of the mod's assets), runs the PNG
    shrinker on the skin folder and the self-check `report()`. header_from defaults to the base weapon's mesh (its place line: same hand, same size class)."""
    t0 = _T0[0]
    if trial:
        mesh_dir, skin_dir = os.path.join(trial, "weapon_meshes"), os.path.join(trial, "textures", "entity", "weapon")
    elif out_dir:
        mesh_dir, skin_dir = os.path.join(out_dir, "weapon_meshes"), os.path.join(out_dir, "textures", "entity", "weapon")
    else:
        mesh_dir, skin_dir = bl.MESH_DIR, bl.SKIN_DIR
    os.makedirs(mesh_dir, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    hdr = header_from or os.path.join(bl.MESH_DIR, base_id + ".txt")
    prev = os.path.join(trial or out_dir or "/tmp", "preview_" + item_id + ".png") if preview else None
    mesh, atlas, stats = bl.finish(item_id, glow=glow, header_from=hdr, bake=bake, skin=skin, out_mesh=mesh_dir, out_skin=skin_dir, preview=None, write=True, budget=10 ** 6, cage=cage)
    bake_s = time.time() - t0
    shrink_pngs(skin_dir, item_id)
    return report(item_id, base_id, mesh_dir, skin_dir, mesh, atlas, bake_s)


def pack_png(path, colors=256, denoise=0.6):
    """Makes a baked skin small without losing its detail: a light median pass takes the bake's sampling speckle out (blended `denoise` with the
    original, so rivets and seams survive), then a 256-colour palette (the cut-out alpha of a skin stays binary; a glow skin keeps its soft alpha through
    Pillow's octree). Writes a palette PNG (tools/shrink_pngs.py leaves those alone). Returns (kb before, kb after, psnr dB)."""
    from PIL import Image, ImageFilter
    before = os.path.getsize(path) / 1024.0
    im = Image.open(path).convert("RGBA")
    a = np.asarray(im).astype(np.float32)
    alpha = a[..., 3]
    rgb_ = Image.fromarray(a[..., :3].astype(np.uint8))
    if denoise > 0:
        med = np.asarray(rgb_.filter(ImageFilter.MedianFilter(3))).astype(np.float32)
        rgb_ = Image.fromarray((a[..., :3] * (1 - denoise) + med * denoise).astype(np.uint8))
    if path.endswith("_glow.png"):
        q = Image.fromarray(np.concatenate([np.asarray(rgb_), alpha[..., None].astype(np.uint8)], axis=2), "RGBA").quantize(colors=colors, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
        out = q
    else:
        q = rgb_.quantize(colors=colors - 1, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        idx = np.asarray(q).copy()
        idx[alpha < 128] = colors - 1
        pal = q.getpalette()[: (colors - 1) * 3]
        pal += [0] * ((colors - 1) * 3 - len(pal)) + [0, 0, 0]
        out = Image.fromarray(idx, "P")
        out.putpalette(pal)
        out.info["transparency"] = bytes([255] * (colors - 1) + [0])
    out.save(path, optimize=True, transparency=out.info.get("transparency")) if not path.endswith("_glow.png") else out.save(path, optimize=True)
    r = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    w = a[..., 3:4] / 255.0
    mse = float((((a[..., :3] - r[..., :3]) ** 2) * w).sum() / max(1.0, float(w.sum()) * 3.0))
    return before, os.path.getsize(path) / 1024.0, (99.0 if mse < 1e-9 else 10.0 * math.log10(255.0 ** 2 / mse))


def shrink_pngs(folder, only=None):
    """Packs every skin of a folder with pack_png() (denoise + palette); the repo's tools/shrink_pngs.py then has nothing left to do (it skips palette PNGs)."""
    for f in sorted(os.listdir(folder)):
        if f.endswith(".png") and "preview" not in f and (only is None or f in (only + ".png", only + "_glow.png")):
            b, a_, ps = pack_png(os.path.join(folder, f))
            print("[pack] %s %.0f KB -> %.0f KB (psnr %.1f dB)" % (f, b, a_, ps))


def write_manifest(path, entries):
    """tools/blender/forms_manifest_<family>.json: {meshid: {weapon, form, class}} (merged with what is already there)."""
    data = {}
    if os.path.exists(path):
        data = json.load(open(path))
    data.update(entries)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=1, sort_keys=True)
        fh.write("\n")
