#!/usr/bin/env python3
"""3D preview of the Beast Will designs on a Minecraft player model.
Software rasteriser (numpy): oriented boxes, z-buffer, lambert light, minecraft-style texel noise, emissive bloom.
Usage: python3 render3d.py [will_id ...]   -> preview3d_sheet_{1,2,3}.png (or preview3d_<id>.png when ids are given)"""
import json, math, os, sys, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from render_preview import SPEC, PIECE, hx, mix, shade, lum   # reuse the feature table + colour helpers

HERE = os.path.dirname(os.path.abspath(__file__))
V = np.array

# ---------------------------------------------------------------- geometry
class B:  # oriented box
    __slots__ = ("c", "ax", "h", "col", "emi", "noise", "face")
    def __init__(s, c, ax, h, col, emi=False, noise=.14, face=False):
        s.c = V(c, float); s.ax = V(ax, float); s.h = V(h, float); s.col = V(col, float); s.emi = emi; s.noise = noise; s.face = face

I3 = np.eye(3)
def obox(c, size, col, emi=False, noise=.14, face=False): return B(c, I3, [s/2 for s in size], col, emi, noise, face)
def _norm(v): n = np.linalg.norm(v); return v/n if n > 1e-9 else v
def limb(p0, p1, w, col, emi=False, w2=None, noise=.14):
    p0 = V(p0, float); p1 = V(p1, float); d = p1-p0; L = np.linalg.norm(d); y = _norm(d)
    h = V([0, 0, 1.0]) if abs(y[2]) < .9 else V([1.0, 0, 0])
    x = _norm(np.cross(h, y)); z = np.cross(x, y)
    return B((p0+p1)/2, V([x, y, z]), [w/2, L/2, (w2 or w)/2], col, emi, noise)
def plate(c, du, dv, t, col, emi=False, noise=.1):
    du = V(du, float); dv = V(dv, float); u = _norm(du); v = _norm(dv - u*np.dot(dv, u)); n = np.cross(u, v)
    return B(c, V([u, v, n]), [np.linalg.norm(du)/2, np.linalg.norm(dv)/2, t/2], col, emi, noise)
def chain(pts, widths, col, emi_last=False, col2=None):
    out = []
    for i in range(len(pts)-1):
        out.append(limb(pts[i], pts[i+1], widths[min(i, len(widths)-1)], col if (col2 is None or i % 2 == 0) else col2, emi=(emi_last and i == len(pts)-2)))
    return out
def rot_x(a): c, s = math.cos(a), math.sin(a); return V([[1, 0, 0], [0, c, -s], [0, s, c]])
def rot_y(a): c, s = math.cos(a), math.sin(a); return V([[c, 0, s], [0, 1, 0], [-s, 0, c]])
def xform(boxes, R, pivot=(0, 12, 0)):
    p = V(pivot, float)
    for b in boxes:
        b.c = R @ (b.c - p) + p; b.ax = (R @ b.ax.T).T
    return boxes
def mirror(boxes):
    """copy of boxes mirrored in x (for pairs)."""
    out = []
    for b in boxes:
        c = b.c.copy(); c[0] = -c[0]; ax = b.ax.copy(); ax[:, 0] = -ax[:, 0]
        out.append(B(c, ax, b.h, b.col, b.emi, b.noise, b.face))
    return out

def scale_boxes(boxes, k, pivot=(0, 0, 0)):
    p = V(pivot, float)
    for b in boxes:
        b.c = (b.c-p)*k+p; b.h = b.h*k
    return boxes

# faces of a unit cube: (axis index, sign) with the two in-plane axes
FACES = [(0, 1, 1, 2), (0, -1, 1, 2), (1, 1, 0, 2), (1, -1, 0, 2), (2, 1, 0, 1), (2, -1, 0, 1)]

def rasterise(boxes, yaw, pitch, W, H, scale, cy, light=(-0.45, 0.75, 0.55)):
    R = rot_x(pitch) @ rot_y(yaw)
    L = _norm(V(light, float))
    zb = np.full((H, W), -1e9); img = np.zeros((H, W, 4), np.float32); glow = np.zeros((H, W, 3), np.float32)
    for bi, b in enumerate(boxes):
        ax = (R @ b.ax.T).T; c = R @ b.c
        for fi, (a, sg, u_i, v_i) in enumerate(FACES):
            n = ax[a]*sg
            if n[2] <= 0.02: continue                      # back-face cull (viewer looks down -z, so front faces have n.z>0)
            fc = c + n*b.h[a]
            eu = ax[u_i]*b.h[u_i]; ev = ax[v_i]*b.h[v_i]
            P = [fc-eu-ev, fc+eu-ev, fc+eu+ev, fc-eu+ev]
            UV = [(0, 0), (1, 0), (1, 1), (0, 1)]
            sizeu, sizev = 2*b.h[u_i], 2*b.h[v_i]
            shd = 1.0 if b.emi else 0.5+0.5*max(0.0, float(np.dot(n, L)))
            for tri in ((0, 1, 2), (0, 2, 3)):
                pts = [(W/2+P[i][0]*scale, H/2-(P[i][1]-cy)*scale, P[i][2]) for i in tri]
                x0 = int(max(0, math.floor(min(p[0] for p in pts)))); x1 = int(min(W-1, math.ceil(max(p[0] for p in pts))))
                y0 = int(max(0, math.floor(min(p[1] for p in pts)))); y1 = int(min(H-1, math.ceil(max(p[1] for p in pts))))
                if x1 < x0 or y1 < y0: continue
                (ax_, ay_, az_), (bx_, by_, bz_), (cx_, cy_, cz_) = pts
                den = (by_-cy_)*(ax_-cx_)+(cx_-bx_)*(ay_-cy_)
                if abs(den) < 1e-9: continue
                xs, ys = np.meshgrid(np.arange(x0, x1+1)+.5, np.arange(y0, y1+1)+.5)
                w0 = ((by_-cy_)*(xs-cx_)+(cx_-bx_)*(ys-cy_))/den
                w1 = ((cy_-ay_)*(xs-cx_)+(ax_-cx_)*(ys-cy_))/den
                w2 = 1-w0-w1
                m = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
                if not m.any(): continue
                z = w0*az_+w1*bz_+w2*cz_
                sub = zb[y0:y1+1, x0:x1+1]
                m &= z > sub
                if not m.any(): continue
                uvs = [UV[i] for i in tri]
                u = (w0*uvs[0][0]+w1*uvs[1][0]+w2*uvs[2][0])*sizeu; v = (w0*uvs[0][1]+w1*uvs[1][1]+w2*uvs[2][1])*sizev
                tu = np.floor(u*1.0+1e-6).astype(np.int64); tv = np.floor(v*1.0+1e-6).astype(np.int64)
                hsh = ((tu*73856093) ^ (tv*19349663) ^ (fi*83492791) ^ ((bi+7)*2654435761)) & 0xFFFF
                nz = (hsh/65535.0-0.5)*2*b.noise
                col = b.col[None, None, :]*(1+nz[..., None])
                if b.face and fi == 4:                     # the front of a head: eyes + mouth texels on an 8x8 grid
                    col = np.broadcast_to(col, (*m.shape, 3)).copy()
                    f8u = np.clip((u/sizeu*8).astype(int), 0, 7); f8v = np.clip((v/sizev*8).astype(int), 0, 7)
                    # v grows downward in face space for fi==4 because (0,1) are y-axis 'up'; so v index 7 = top
                    row = 7-f8v
                    eye = ((row == 3) | (row == 4)) & ((f8u == 1) | (f8u == 2) | (f8u == 5) | (f8u == 6))
                    col[eye] = V([240, 240, 245])
                    pup = (row == 3) & ((f8u == 1) | (f8u == 6)) | ((row == 4) & ((f8u == 2) | (f8u == 5)))
                    col[(row == 3) & ((f8u == 2) | (f8u == 5))] = V([60, 60, 90])
                    col[(row == 6)] = col[(row == 6)]*0.55
                    mouth = (row == 1) & ((f8u >= 3) & (f8u <= 4))
                    col[mouth] = col[mouth]*0.6
                shade_arr = shd
                out = np.clip(col*shade_arr, 0, 255)
                ysl = slice(y0, y1+1); xsl = slice(x0, x1+1)
                im = img[ysl, xsl]; im[m, :3] = out[m] if out.ndim == 3 else out; im[m, 3] = 255
                sub[m] = z[m]
                if b.emi:
                    g = glow[ysl, xsl]; g[m] = b.col
                else:
                    g = glow[ysl, xsl]; g[m] = 0
    return img, glow

def finish(img, glow, W, H, bloom_col=None, bloom=1.0):
    from PIL import Image as I
    g = I.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGB")
    g1 = g.filter(ImageFilter.GaussianBlur(5)); g2 = g.filter(ImageFilter.GaussianBlur(14))
    gl = (np.asarray(g1, np.float32)*0.9+np.asarray(g2, np.float32)*1.1)*bloom
    base = img[..., :3].copy()
    a = img[..., 3:4]/255.0
    rgb = base+gl*(1.0)                                    # additive bloom
    alpha = np.clip(a[..., 0]+np.clip(gl.max(axis=2)/160, 0, 1), 0, 1)
    out = np.dstack([np.clip(rgb, 0, 255), alpha*255]).astype(np.uint8)
    return I.fromarray(out, "RGBA")

# ---------------------------------------------------------------- the player and the will parts
SKIN = (214, 163, 134); SHIRT = (74, 82, 98); PANTS = (44, 50, 68); HAIR = (60, 44, 34)

def player(pose="stand", skin=SKIN, shirt=SHIRT, pants=PANTS, hair=HAIR, lean=0.0, swing=0.0):
    """a stock minecraft player: head 8, body 8x12x4, arms 4x12x4, legs 4x12x4 (units = texels). pose 'run' strides and swings."""
    out = []
    head = obox((0, 28, 0), (8, 8, 8), skin, face=True); out.append(head)
    out.append(obox((0, 31.6, -0.3), (8.4, 1.6, 8.6), hair)); out.append(obox((0, 28.6, -3.7), (8.4, 6.6, 1.2), hair))
    out.append(obox((0, 18, 0), (8, 12, 4), shirt))
    for sx in (-1, 1):
        s = swing*sx
        sh = (6.0*sx, 22.0, 0); hand = (6.0*sx, 22.0-11*math.cos(s), 11*math.sin(s)*-1)
        out.append(limb(sh, hand, 4, shirt if False else skin));
        sl = swing*-sx
        hip = (2.0*sx, 12.0, 0); foot = (2.0*sx, 12.0-12*math.cos(sl), 12*math.sin(sl))
        out.append(limb(hip, foot, 4, pants))
    return out

def head_feats(cx, cy, cz, k, sp, M, A, A2, sc=1.0, main=True):
    out = []; D = shade(M, .55)
    f = lambda *v: tuple(x*sc for x in v)
    def at(dx, dy, dz): return (cx+dx*sc, cy+dy*sc, cz+dz*sc)
    sn = {"wolf": (3.2, 2.8, 4.6), "dragon": (3.6, 3.0, 5.4), "horse": (3.4, 5.5, 5.0), "croc": (3.8, 2.2, 8.0), "cat": (3.2, 2.2, 2.2),
          "boar": (5.0, 3.2, 3.0), "bird": (1.8, 2.4, 4.2), "lion": (3.8, 3.2, 3.0), "bull": (5.0, 3.2, 2.6), "ape": (3.6, 2.6, 1.8),
          "snake": (3.4, 2.6, 4.6), "cobra": (3.4, 2.6, 3.6), "fox": (2.6, 2.2, 4.4), "whale": (6.0, 3.4, 3.4), "plant": (0, 0, 0),
          "human": (0, 0, 0), "squid": (0, 0, 0)}[k]
    if sn[0]:
        out.append(obox(at(0, -1.6, 4+sn[2]*.65/2*1.0), f(sn[0]*1.25, sn[1]*1.25, sn[2]*1.3), mix(M, (255, 255, 255), .30)))
        out.append(obox(at(0, -1.6+sn[1]/2, 4+sn[2]), f(sn[0]*.45, sn[1]*.4, .6), (28, 26, 30)))   # nose
    if k in ("wolf", "dragon", "croc", "cat", "snake", "cobra", "fox", "lion"):
        for sx in (-1, 1): out.append(obox(at(sx*sn[0]*.32, -3.0, 4+sn[2]*.7), f(.55, 1.4, .55), (245, 245, 235)))   # fangs
    if k == "boar":
        for sx in (-1, 1):
            out.append(limb(at(sx*2.3, -2.4, 6), at(sx*3.4, -0.4, 6.6), 0.9*sc, (240, 235, 215))); out.append(limb(at(sx*3.4, -0.4, 6.6), at(sx*3.2, 1.6, 6.0), 0.8*sc, (240, 235, 215)))
    if k == "bird": out[-1].col = V(A2)
    if k == "lion" or (sp.get("mane") and k in ("wolf", "dragon", "horse", "human", "croc")):
        mc = A2 if k == "lion" else mix(M, (255, 255, 255), .25)
        out += [obox(at(0, 0, -2.0), f(13.5, 13.5, 3.5), shade(M, .5)), obox(at(0, -2.5, -2.4), f(10, 14, 3.0), mc), obox(at(0, 4, -2.2), f(11, 6, 2.8), mc)]
        for sx in (-1, 1): out.append(obox(at(sx*5.6, -1, 0), f(2.4, 9, 7), shade(M, .5)))
    if sp.get("nemes"):
        out += [obox(at(0, 4.4, 0), f(9, 1.4, 9), A2), obox(at(0, 1.2, -4.3), f(9, 9, 1.6), A2)]
        for sx in (-1, 1): out += [obox(at(sx*4.6, -1, 1), f(1.6, 9, 4), A2), obox(at(sx*4.6, -6, 2.6), f(1.6, 4, 2.4), (30, 30, 80))]
    if k == "cobra":
        out += [obox(at(0, -1, -3.2), f(15, 13, 1.6), shade(M, .6)), obox(at(0, -3, -3.3), f(10, 6, 1.7), mix(M, A, .35))]
    if k == "squid":
        out += [obox(at(0, 6.4, -1.4), f(8.8, 12, 8.8), shade(M, .85)), obox(at(0, 12, -2.0), f(6, 6, 6), shade(M, .75)), obox(at(0, 7.2, 2.6), f(3, 8.6, .8), A2)]
    if k == "whale": out += [obox(at(0, 1.6, 4.8), f(5.2, 2.6, 1.2), mix(M, (255, 255, 255), .45)), obox(at(0, 4.4, 1), f(1.2, 1, 2.4), (20, 40, 50))]
    if k == "plant":
        for i, sx in enumerate((-1, 1)):
            for j in range(3): out.append(obox(at(sx*(1.6+j*1.6), 4.4+j*1.0, 3.9), f(1.4, 2.6, .8), A2))
    # ears
    e = sp["ears"]
    for sx in (-1, 1):
        if e == "pointed": out.append(limb(at(sx*3, 4, -.5), at(sx*3.8, 8.6, -1.2), 2.4*sc, D)); out.append(limb(at(sx*3, 4.6, -.1), at(sx*3.6, 8, -.5), 1.0*sc, A2))
        if e == "round": out.append(obox(at(sx*3.8, 4.6, -.6), f(2.4, 2.4, 1.4), D))
        if e == "fox": out.append(limb(at(sx*2.8, 4, -.5), at(sx*4.4, 10.6, -1.4), 2.6*sc, D)); out.append(limb(at(sx*2.8, 4.5, -.1), at(sx*4.2, 9.8, -.8), 1.2*sc, A))
        if e == "long": out.append(limb(at(sx*3.8, 3.4, -.5), at(sx*5.6, -2.8, -.9), 2.2*sc, D))
    # horns
    h = sp["horns"]
    for sx in (-1, 1):
        if h == "swept": out += chain([at(sx*2, 4, -1), at(sx*3.4, 7, -3), at(sx*5.2, 8.6, -6), at(sx*7, 8.4, -8.6)], [1.6*sc, 1.3*sc, 1.0*sc], A2)
        if h == "bull":  out += chain([at(sx*4, 3, .5), at(sx*7, 3.6, .5), at(sx*8.8, 6.4, 1.2), at(sx*8.6, 9.6, 1.6)], [1.8*sc, 1.5*sc, 1.1*sc], A2)
        if h == "ram":   out += chain([at(sx*4, 3, -.5), at(sx*7, 3.4, -.5), at(sx*8.4, 1, 1), at(sx*7, -1.4, 3)], [2.2*sc, 1.9*sc, 1.5*sc], A2)
        if h == "branch": out += chain([at(sx*2, 4, -1), at(sx*3, 8, -1), at(sx*2, 12.4, -1.2)], [1.0*sc, .9*sc], A2) + chain([at(sx*3, 7.4, -1), at(sx*6.4, 10.4, -1.2)], [.8*sc], A2) + chain([at(sx*2.6, 10.4, -1.2), at(sx*5.4, 13.6, -1.4)], [.7*sc], A2)
    if h == "crest":
        for i in range(3): out.append(limb(at(0, 4.2, 1.6-i*2.2), at(0, 7.6-i*.8, .4-i*2.6), 1.2*sc, A2))
    # eyes (emissive)
    for sx in (-1, 1): out.append(obox(at(sx*2.0, 0.6, 4.06), f(1.8, .9, .3), A, emi=True))
    return out

def build_acquire(w, sp, M, A, A2):
    run = player(swing=0.8)
    boxes = run
    # the one equipped piece
    where = PIECE[w]; sh = (-6, 22, 0)
    if where == "arm":
        hand = (-6, 22-11*math.cos(.8*-1), 11*math.sin(.8*-1)*-1)       # right arm (x -6) end point, same formula as player()
        s = -.8
        hand = (-6.0, 22.0-11*math.cos(s), -11*math.sin(s)*-1)
        p0 = (sh[0], sh[1]-11*0.45*math.cos(s)+0, 0);
        q0 = (-6.0, 22.0-11*math.cos(s)*.35, 11*math.sin(s)*.35*-1*-1)
        boxes.append(limb((-6.0, 22.0-6.4, -(-6.4*math.tan(0)) ), (-6.0, 22.0-11, 0), 5.2, M))  # fallback gauntlet along the arm line
    elif where == "leg":
        boxes.append(obox((2.0, 1.5, 0.5), (4.8, 3, 4.8), M)); boxes.append(obox((2.0, 1.5, 2.9), (3, 1, .6), A2, emi=True))
    elif where == "hip":
        boxes += chain([(0, 12.5, -2.2), (-1.5, 11, -5), (-3.5, 9.4, -8), (-6.5, 8.6, -10.5)], [1.8, 1.5, 1.2], M); boxes.append(obox((-6.5, 8.6, -10.8), (1.6, 1.6, 1.6), A, emi=True))
    elif where == "head":
        boxes.append(obox((0, 30.2, 0), (8.8, 1.4, 8.8), M)); boxes.append(obox((0, 30.2, 4.5), (2.2, 1.4, .6), A, emi=True))
    elif where == "shoulder":
        boxes.append(obox((-6, 24.3, 0), (5.8, 2.4, 5.8), M)); boxes.append(limb((-6, 25.4, 0), (-6.6, 28.6, 0), 1.6, A))
    elif where == "neck":
        boxes.append(obox((0, 24.6, 0), (9.2, 1.8, 5.2), M)); boxes.append(obox((0, 24.6, 2.8), (2.6, 1.2, .5), A, emi=True))
    for b in boxes[:0]: pass
    boxes += [obox((x, 28.6, 4.07), (1.4, .8, .3), A, emi=True) for x in (-2, 2)]
    return boxes

def gauntlet_for_run(M, A):
    """gauntlet along the forearm of the running pose's right arm (player() uses swing 0.8 -> right arm swings -0.8)."""
    s = -0.8; sh = V([-6.0, 22.0, 0]); hand = V([-6.0, 22.0-11*math.cos(s), -11*math.sin(s)])
    p0 = sh+(hand-sh)*.45; p1 = hand
    return [limb(p0, p1, 5.4, M), limb(p0+(p1-p0)*.2, p0+(p1-p0)*.8, 5.7, A, emi=True)]

def build_integrate(w, sp, M, A, A2):
    out = []; D = shade(M, .6); body = shade(M, .78); L = mix(M, (255, 255, 255), .45)
    # torso, arms, digitigrade legs
    out.append(obox((0, 18, 0), (8, 12, 4), body, noise=.18))
    for sx in (-1, 1):
        out.append(limb((6*sx, 22, 0), (6.4*sx, 11.5, 1.2), 4, body))
        out.append(limb((6.4*sx, 11.6, 1.2), (6.6*sx, 9.4, 2.0), 4.4, D))                                   # forearm bulk
        for j in (-1, 0, 1): out.append(limb((6.6*sx+j*1.2, 9.2, 2.6), (6.8*sx+j*1.5, 6.2, 4.0), .8, (240, 236, 224)))     # claws
        if not sp.get("waistheads") and sp["head"] != "squid":
            out.append(limb((2*sx, 13, 0), (2.6*sx, 8, 3.0), 4.4, body))                                    # thigh forward
            out.append(limb((2.6*sx, 8, 3.0), (2.2*sx, 2.8, -1.6), 3.4, D))                                  # shin back (digitigrade)
            out.append(obox((2.2*sx, 1.0, 1.2), (3.6, 2, 6), D))
            for j in (-1, 0, 1): out.append(limb((2.2*sx+j*1.1, 1.0, 3.8), (2.2*sx+j*1.2, .6, 6.2), .8, (240, 236, 224)))
    if sp.get("waistheads") or sp["head"] == "squid":
        out.append(obox((0, 6, 0), (11, 12, 8), shade(M, .6)))
    # armour: plates, pauldrons, spikes
    if sp.get("plates"):
        for i in range(3): out.append(obox((0, 22-i*3.2, 2.35), (6.6-i*.4, 2.6, .9), mix(A2, M, .25)))
    for sx in (-1, 1):
        out.append(obox((6.4*sx, 24.4, 0), (5.8, 2.4, 5.8), M)); out.append(limb((6.4*sx, 25.4, 0), (7.4*sx, 29, -.6), 1.8, A2))
    if sp.get("spikes"):
        for i in range(4): out.append(limb((0, 23-i*3.2, -2.1), (0, 24.6-i*3.2, -5), 1.6, A2))
    # head
    hx_ = obox((0, 28.9, 0), (9.6, 9.6, 9.6), body, face=True); out.append(hx_)
    out += head_feats(0, 28.9, 0, sp["head"], sp, M, A, A2, sc=1.2)
    if sp["head"] in ("wolf", "lion", "horse", "dragon", "cat", "fox", "boar", "bull") or sp.get("mane"):      # fur ruff / neck frill so the head sits on a beast neck
        out += [obox((0, 24.9, 0), (11, 2.6, 7.2), mix(M, (255, 255, 255), .22)), obox((0, 24.6, -2.4), (11.6, 3.4, 3), shade(M, .9))]
    # side heads on necks (hydra / typhon / cerberus / yamata)
    n = sp.get("sideheads", 0)
    for i in range(n):
        sx = -1 if i % 2 == 0 else 1; k = i//2
        if n <= 2: pos = (sx*7.5, 29.5, 1.0)
        else: pos = (sx*(6.5+k*3.2), 33.5+k*2.4, 0.6-k*1.6)
        out += chain([(sx*2.5, 24, 0), (sx*(4+k*2), 27, 0), pos], [2.6, 2.4], body)
        sub = dict(sp); sub.update(horns="none", mane=0, nemes=0)
        out.append(obox(pos, (4.6, 4.6, 4.6), body, face=True))
        out += head_feats(pos[0], pos[1], pos[2], sp["head"] if sp["head"] != "dragon" or n < 3 else "snake", sub, M, A, A2, sc=.55, main=False)
    # tail
    t = sp["tail"]; base = V([0, 12.5, -2.2])
    def tailpts(L, droop=0.0, curl=0.0, side=0.0, n_=7):
        pts = []
        for i in range(n_):
            u = i/(n_-1)
            pts.append((side*u*10, 12.5-droop*u*9+curl*math.sin(u*math.pi)*6, -2.2-u*L))
        return pts
    if t in ("whip", "thick", "spiked", "bushy", "tuft", "curl", "flame", "feather", "serpent", "serpent2", "fluke"):
        wmax = {"whip": 2.6, "thick": 5.0, "spiked": 4.6, "bushy": 4.8, "tuft": 2.6, "curl": 3.0, "flame": 4.0, "feather": 3.2, "serpent": 3.4, "serpent2": 3.0, "fluke": 4.6}[t]
        for sd in ((0,) if t != "serpent2" else (-5, 5)):
            pts = tailpts(15 if t != "serpent2" else 18, droop=.25 if t in ("thick", "spiked") else -.15, curl=0.6 if t in ("whip", "curl", "serpent") else .2, side=(sd/10 if t == "serpent2" else 0.0))
            ws = [wmax*(1-i*.12) for i in range(6)]
            out += chain(pts, ws, D)
            end = V(pts[-1])
            if t == "spiked":
                for p in pts[1:-1]: out.append(limb(p+V([0, 1.5, 0]), p+V([0, 4.2, -.6]), 1.2, A2))
            if t == "tuft": out.append(obox(end, (3.4, 3.4, 4.4), A2))
            if t == "bushy": out.append(obox(end+V([0, .6, -1]), (4.4, 4.4, 5.6), L))
            if t == "flame": out.append(obox(end, (4, 4, 5), A2, emi=True)); out.append(obox(end+V([0, 2.6, -1]), (2.4, 3.6, 2.4), (255, 220, 120), emi=True))
            if t == "fluke": out.append(plate(end+V([0, .6, -1.4]), (11, 0, 0), (0, 0, 5), .9, M))
            if t in ("serpent", "serpent2"):
                out.append(obox(end+V([0, 0, -1.2]), (3.4, 3, 4), shade(M, .9), face=False)); out.append(obox(end+V([0, .5, .6]), (2.4, .7, .4), A, emi=True))
            if t == "feather":
                for j in (-2, -1, 0, 1, 2): out.append(plate(end+V([j*1.6, 0, 0]), (1.3, 0, 0), (0, 0, 7-abs(j)*.9), .5, mix(M, (255, 255, 255), .3)))
    if t == "fox":
        for j in range(5):
            a = -.95+j*.475
            pts = [(0, 12.5, -2.2), (math.sin(a)*3, 13.5, -5.4), (math.sin(a)*7, 15.5, -9.0), (math.sin(a)*10, 19.0+j*.4, -11.6)]
            out += chain(pts, [3.0, 3.6, 3.2], mix(M, (255, 255, 255), .08*j)); out.append(obox(pts[-1], (3.4, 4.2, 3.4), A2, emi=(j % 2 == 0)))
    # tentacles (kraken)
    for i in range(sp.get("tentacles", 0)):
        sx = -1 if i % 2 == 0 else 1; k = i//2
        pts = [(sx*3, 19, -2.2), (sx*(7+k*2), 17, -5), (sx*(11+k*2.4), 10, -7-k*1.4), (sx*(12+k*3), 3, -5-k*3), (sx*(9+k*4), .8, -1-k*2.2)]
        out += chain(pts, [3.2, 2.8, 2.4, 2.0], shade(M, .85), col2=shade(M, .7))
        for p in pts[2:]: out.append(obox((p[0], p[1], p[2]+1.4), (1.2, 1.2, .5), A2))
    # wings
    wing_start = len(out)
    wg = sp["wings"]
    for sx in (-1, 1):
        if wg in ("membrane", "tattered"):
            sh_ = (sx*2.5, 23.5, -2.4); tip = (sx*21, 33, -9); mid = (sx*16, 22.5, -8)
            col = shade(M, .55) if wg == "membrane" else shade(M, .35)
            out.append(limb(sh_, (sx*13, 31, -6.4), 1.6, A2)); out.append(limb((sx*13, 31, -6.4), tip, 1.3, A2))
            out.append(limb((sx*13, 31, -6.4), mid, .9, A2)); out.append(limb(sh_, mid, .9, A2))
            for q in range(4):
                out.append(plate((sx*(6.5+q*4.4), 27.4-q*2.1, -5.6-q*1.7), (sx*4.8, 0, -.8), (0, 14-q*2.6, 0), .5, col))
            if wg == "tattered": out.append(obox((sx*14, 25, -7.2), (2.6, 3, .8), (14, 14, 18)))
        if wg == "feather":
            for j in range(6):
                a = j*.2
                out.append(plate((sx*(7.5+j*1.9), 30-j*2.6, -3.4-j*.9), (sx*(14-j*1.1), 0, 0), (0, 0, 5), .5, mix(M, (255, 255, 255), .15+.08*j)))
        if wg == "small":
            out.append(plate((sx*7, 26.4, -3.4), (sx*10, 4, 0), (0, 0, 6), .5, shade(M, .7)))
    scale_boxes(out[wing_start:], 1.2, (0, 24, -2.4))
    if sp.get("fins"):
        for sx in (-1, 1): out.append(plate((sx*8.2, 14.4, 2), (0, 0, 7), (0, 8, 0), .4, A))
    if sp.get("island"):
        out.append(obox((0, 26.8, -3.6), (9.6, 3.4, 5.6), (96, 140, 76)))
        for i in range(3): out.append(obox((-2.6+i*2.6, 29.6+(i % 2), -3.6), (1.2, 3.2, 1.2), (92, 62, 36))); out.append(obox((-2.6+i*2.6, 32+(i % 2), -3.6), (3.4, 2.4, 3.4), (56, 118, 60)))
    # waist heads (scylla)
    for i in range(sp.get("waistheads", 0)):
        a = math.radians(-70+i*28)
        pos = (math.sin(a)*8.6, 11+(i % 2)*2.6, math.cos(a)*6.0)
        out += chain([(math.sin(a)*3, 13, math.cos(a)*1.4), pos], [2.2], D)
        sub = dict(sp); sub.update(head="wolf", horns="none", ears="pointed", mane=0, nemes=0)
        out.append(obox(pos, (4, 4, 4), (150, 150, 160), face=True))
        out += head_feats(pos[0], pos[1], pos[2], "wolf", sub, (150, 150, 160), (230, 60, 60), A, sc=.5)
    # weapon in the right hand (x = -6.6)
    wp = sp["weapon"]; hand = V([-6.8, 7.8, 3.6])
    if wp in ("fang", "sword"): out.append(limb(hand+V([0, -1, 0]), hand+V([0, 15, 5.4]), 1.5, L, w2=.5)); out.append(limb(hand, hand+V([0, 1, 0]), 3, A2))
    elif wp == "twin":
        for sx in (-1, 1): out.append(limb(V([6.8*sx, 7.8, 3.6]), V([6.8*sx, 17, 8.4]), 1.3, A2, w2=.5))
    elif wp == "lance": out.append(limb(hand+V([0, -5, -3]), hand+V([0, 20, 9]), 1.1, A2)); out.append(limb(hand+V([0, 20, 9]), hand+V([0, 25, 11]), 2.2, L, w2=.4))
    elif wp == "axe": out.append(limb(hand+V([0, -6, -2]), hand+V([0, 17, 3]), 1.2, A2)); out.append(plate(hand+V([0, 16, 3.4]), (0, 0, 7.4), (0, 7, 0), 1.0, L))
    elif wp == "staff": out.append(limb(hand+V([0, -6, -2]), hand+V([0, 20, 4]), 1.0, A2)); out.append(obox(hand+V([0, 22, 4.6]), (3.6, 3.6, 3.6), L, emi=True))
    elif wp == "fan":
        for j in range(5): out.append(plate(V([-8+j*0, 16, 4])+V([-1.2*j+2, j*.8, 0]), (0, 8, 0), (-2.4*j+4.6, 0, 1), .4, mix(A2, M, .15*j)))
    elif wp == "tusk":
        for sx in (-1, 1): out.append(limb(V([4.4*sx, 12.4, 2.8]), V([5.8*sx, 14, 6.4]), 1.5, (240, 235, 215)))
    elif wp == "jaws": out.append(obox(hand+V([-.4, 1.4, 1.8]), (3.8, 3.2, 4.6), A2)); [out.append(obox(hand+V([-.4+j*1.0, 0.2, 4.2]), (.5, 1.2, .5), (250, 250, 240))) for j in (-1, 0, 1)]
    # fx
    rnd = random.Random(sum(map(ord, w))); fx = sp["fx"]
    if fx == "lightning":
        for i in range(3):
            x, y, z = rnd.uniform(-8, 8), 36.0, rnd.uniform(-3, 3); pts = [(x, y, z)]
            for j in range(5): x += rnd.uniform(-2.4, 2.4); y -= 6.5; z += rnd.uniform(-1.5, 1.5); pts.append((x, y, z))
            out += chain(pts, [.7], A, emi_last=False)
            for b in out[-5:]: b.emi = True
    if fx == "vines":
        for sx in (-1, 1): out += chain([(sx*3.5, 13, 3), (sx*7, 8, 5), (sx*5, 3.4, 4), (sx*9, .8, 5)], [1.1], A2)
    if fx in ("water", "whirl"):
        for i in range(3):
            r = 11+i*3; y = 1.0+i*1.6
            for j in range(14):
                a = j*2*math.pi/14+i*.4; out.append(obox((math.cos(a)*r, y+(j % 3)*.4, math.sin(a)*r), (2.2, .7, 2.2), A2, emi=(j % 2 == 0)))
    if fx == "flame":
        for j in range(8): out.append(obox((rnd.uniform(-9, 9), rnd.uniform(14, 34), rnd.uniform(-5, 5)), (1.4, 1.8, 1.4), A2, emi=True))
    if fx == "chain":
        for j in range(6): out.append(obox((-8.4, 10.5-j*1.5, 2+j*.3), (1.8, 1.2, 1.2), (200, 210, 225)))
    if fx == "venom":
        for j in range(9): out.append(obox((rnd.uniform(-10, 10), rnd.uniform(1, 16), rnd.uniform(-5, 6)), (1.1, 1.1, 1.1), A2, emi=True))
    if fx == "roots":
        for j in range(7): a = j*.9; out += chain([(math.cos(a)*3, 1.4, math.sin(a)*3), (math.cos(a)*8, 2.2, math.sin(a)*8), (math.cos(a)*12, .4, math.sin(a)*12)], [1.0], (80, 60, 30))
    if fx == "ragged":
        for j in range(18):
            out.append(limb((rnd.uniform(-5, 5), rnd.uniform(2, 30), rnd.uniform(-3, 3)), (rnd.uniform(-9, 9), rnd.uniform(0, 34), rnd.uniform(-6, 5)), 1.0, shade(M, .6)))
    if fx in ("wind", "dust"):
        for j in range(10): out.append(limb((rnd.uniform(-10, 10), rnd.uniform(4, 30), rnd.uniform(-14, -8)), (rnd.uniform(-10, 10), rnd.uniform(4, 30), rnd.uniform(-26, -16)), .6, L, emi=True))
    # glowing eyes are in head_feats; add a chest core gem and rune stripes
    out.append(obox((0, 20.4, 2.5), (1.8, 1.8, .5), A, emi=True))
    return scale_boxes(out, 1.12, (0, 0, 0))

# ---------------------------------------------------------------- rendering of one view / one card
FW, FH = 360, 440
def render_fig(boxes, yaw, pitch=-0.12, scale=6.4, cy=19.0, glowmul=0.5):
    S = 2
    img, glow = rasterise(boxes, yaw, pitch, FW*S, FH*S, scale*S, cy)
    im = finish(img, glow, FW*S, FH*S, bloom=glowmul)
    return im.resize((FW, FH), Image.LANCZOS)

def fit(boxes, yaws, pitch=-0.12, fill=0.90):
    """auto-fit: one scale + centre for all given views so the whole figure (tail, wings, weapon) fills the frame."""
    xs0=ys0=1e9; xs1=ys1=-1e9; mx=0
    for yaw in yaws:
        R = rot_x(pitch) @ rot_y(yaw)
        for b in boxes:
            ax = (R @ b.ax.T).T; c = R @ b.c
            for sx in (-1, 1):
                for sy in (-1, 1):
                    for sz in (-1, 1):
                        p = c + ax[0]*b.h[0]*sx + ax[1]*b.h[1]*sy + ax[2]*b.h[2]*sz
                        xs0=min(xs0,p[0]); xs1=max(xs1,p[0]); ys0=min(ys0,p[1]); ys1=max(ys1,p[1]); mx=max(mx,abs(p[0]))
    sc = min(FW*fill/(2*mx), FH*fill/(ys1-ys0))
    return sc, (ys0+ys1)/2

def stage(im, col, bgcol=(14, 16, 22)):
    """put a figure on a dark stage with a soft floor glow ring in the will colour."""
    bg = Image.new("RGBA", im.size, bgcol+(255,)); d = ImageDraw.Draw(bg)
    g = Image.new("RGBA", im.size, (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    gd.ellipse([60, FH-92, FW-60, FH-34], fill=col+(120,)); g = g.filter(ImageFilter.GaussianBlur(14))
    bg = Image.alpha_composite(bg, g); d = ImageDraw.Draw(bg)
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0)); sd = ImageDraw.Draw(sh); sd.ellipse([95, FH-78, FW-95, FH-52], fill=(0, 0, 0, 150)); sh = sh.filter(ImageFilter.GaussianBlur(6))
    bg = Image.alpha_composite(bg, sh)
    return Image.alpha_composite(bg, im)

def card(w, data, font, fs, W=1140, H=560):
    sp = SPEC[w]
    cols = [hx(c) for c in data["spirit"]["colors"]]
    if lum(cols[0]) < 70: cols = [mix(cols[0], (255, 255, 255), .30), cols[1] if lum(cols[1]) > 60 else mix(cols[1], (255, 255, 255), .4), cols[2]]
    M, A, A2 = cols
    # acquire: running player + afterimages + the piece
    acq = player(swing=.8); acq += gauntlet_for_run(M, A) if PIECE[w] == "arm" else build_acquire(w, sp, M, A, A2)[len(acq):]
    acq += [obox((x, 28.6, 4.07), (1.4, .8, .3), A, emi=True) for x in (-2, 2)] if PIECE[w] == "arm" else []
    for xx in (-1, 1):  # speed streaks behind the runner
        pass
    streaks = []
    rnd = random.Random(sum(map(ord, w)))
    for j in range(7): streaks.append(limb((rnd.uniform(-8, 8), rnd.uniform(4, 30), -9+rnd.uniform(-2, 0)), (rnd.uniform(-8, 8), rnd.uniform(4, 30), -20-rnd.uniform(0, 8)), .35, M, emi=True))
    ghosts = []
    for k, al in ((3, .16), (2, .26), (1, .40)):
        gp = player(swing=.8, skin=mix(M, (255, 255, 255), .35), shirt=mix(M, (255, 255, 255), .35), pants=mix(M, (255, 255, 255), .35), hair=mix(M, (255, 255, 255), .35))
        for b in gp: b.c = b.c+V([0, 0, -k*6.0]); b.noise = 0.04
        ghosts.append((gp, al))
    yaw_a = math.radians(-148+180)   # front-3/4 of a figure that runs toward the viewer's right
    def view_acq():
        base = None
        L = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
        sc0, cy0 = fit(acq+streaks+ghosts[0][0], (math.radians(38),), fill=.86)
        for gp, al in ghosts:
            gi = render_fig(gp, math.radians(38), scale=sc0, cy=cy0)
            a = gi.split()[3].point(lambda v: int(v*al)); gi.putalpha(a); L = Image.alpha_composite(L, gi)
        main = render_fig(acq+streaks, math.radians(38), scale=sc0, cy=cy0)
        return Image.alpha_composite(L, main)
    a_im = view_acq()
    ib = build_integrate(w, sp, M, A, A2)
    sc_, cy_ = fit(ib, (math.radians(35), math.radians(-150)))
    i1 = render_fig(ib, math.radians(35), scale=sc_, cy=cy_)
    i2 = render_fig(ib, math.radians(-150), scale=sc_, cy=cy_)
    base = Image.new("RGBA", (W, H), (10, 12, 18, 255)); dd = ImageDraw.Draw(base)
    tint = Image.new("RGBA", (W, H), M+(24,)); base = Image.alpha_composite(base, tint); dd = ImageDraw.Draw(base)
    cw = W//3
    for n, im in enumerate((a_im, i1, i2)):
        st = stage(im, M); st = st.resize((cw-6, int((cw-6)*FH/FW)), Image.LANCZOS)
        base.alpha_composite(st, (n*cw+3, 66))
    dd.text((14, 8), data["name"].upper().replace(" WILL", ""), fill=(255, 255, 255), font=font)
    tag = f"{data['minRank']}{' LEGENDARY' if data.get('legendary') else ''}  |  {data['tag']}"
    dd.text((W-12, 12), tag, fill=M, font=fs, anchor="ra")
    dd.text((14, 36), data["beast"].split("(")[0].strip()[:60], fill=(190, 196, 210), font=fs)
    for n, t in enumerate(("ACQUIRE: human + 1 piece", "INTEGRATE: front-left", "INTEGRATE: back")):
        dd.text((n*cw+12, H-34), t, fill=(205, 210, 225), font=fs)
    dd.text((W-12, 36), f"Integrate drain {data['integrate']['drainPct']}%  strain {data['integrate']['strainPerSecond']}/s  |  Acquire strain {data['acquire']['strainPerSecond']}/s", fill=(150, 156, 170), font=fs, anchor="ra")
    for k, c in enumerate(cols): dd.rectangle([14+k*36, H-14, 46+k*36, H-8], fill=c)
    dd.rectangle([0, 0, W-1, H-1], outline=M, width=2)
    return base

def main():
    ws = json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))
    try: font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 21); fs = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    except Exception: font = fs = ImageFont.load_default()
    ids = sys.argv[1:]
    if ids:
        for i in ids:
            d = next(w for w in ws if w["id"] == i); out = os.path.join(HERE, f"preview3d_v2_{i}.png"); card(i, d, font, fs).convert("RGB").save(out); print(out)
        return
    W, H = 1140, 560
    for gi, g in enumerate((ws[0:6], ws[6:12], ws[12:18], ws[18:24], ws[24:27]), 1):
        sheet = Image.new("RGBA", (W*2, H*((len(g)+1)//2)), (6, 7, 10, 255))
        for n, d in enumerate(g):
            sheet.alpha_composite(card(d["id"], d, font, fs, W, H), ((n % 2)*W, (n//2)*H)); print(d["id"], flush=True)
        out = os.path.join(HERE, f"preview3d_v2_sheet_{gi}.png"); sheet.convert("RGB").save(out); print(out)
if __name__ == "__main__": main()
