"""Shared helpers for the animated Ultimate skill icons (Sandalphon, Israfil, Jibril, Solomon, Hercules).

An icon script supplies ONE function  draw(t, S) -> PIL.Image (RGBA, S x S)  that paints the icon at time t in [0, 1)
(t = 0 and t = 1 must look the same: the loop is seamless). The helpers render N frames, write the vertical strips
and the .mcmeta files the game reads, a still hero frame, and preview GIFs / contact sheets.

Files written for an icon <id> (all under assets/nusmp/textures/skill/veldanava/):
  <id>_anim_hd.png   128 x (128*N)  the HD strip, drawn on the Will of the Creator card screen
  <id>_anim.png      32  x (32*N)   the 32 px strip, served in place of <id>.png in every ManasCore skill list
  <id>.png           32 x 32        the hero (still) frame; what the game shows with animatedUltimateIcons = false
  <id>_anim.png.mcmeta / <id>_anim_hd.png.mcmeta  {"animation": {"frametime": 2}}  (read by AnimatedIconTexture)
The pre-3.17.0 art stays as <id>_classic.png (never deleted).

Usage (from an icon script):
    from icon_anim_lib import build
    build("jibril", draw, hero_t=0.25)
Run:  python tools/gen_<id>_icon_anim.py        (previews go to /tmp/claude-0/icons/<id>/ unless --preview-dir is given)
"""
import argparse
import json
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "veldanava")
FRAMES = 32
FRAMETIME = 2          # game ticks per frame -> 32 * 2 = 64 ticks = 3.2 s loop
HD = 128
SD = 32
SUPER = 2              # draw at HD*SUPER and downscale for anti-aliasing


def clean_transparent(img):
    """Zero the RGB of fully transparent pixels (no dark fringes when the game filters the texture)."""
    a = np.array(img.convert("RGBA"))
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def to_hd(big):
    return clean_transparent(big.resize((HD, HD), Image.LANCZOS)) if big.size != (HD, HD) else clean_transparent(big)


def to_sd(hd):
    """128 -> 32: box-ish LANCZOS downscale on premultiplied colour, then a light unsharp so the small icon stays crisp."""
    a = np.array(hd.convert("RGBA")).astype(np.float64)
    al = a[..., 3:4] / 255.0
    pm = np.concatenate([a[..., :3] * al, a[..., 3:4]], axis=-1)
    im = Image.fromarray(np.clip(pm, 0, 255).astype(np.uint8), "RGBA").resize((SD, SD), Image.LANCZOS)
    im = im.filter(ImageFilter.UnsharpMask(radius=0.6, percent=60, threshold=2))
    b = np.array(im).astype(np.float64)
    ba = b[..., 3:4] / 255.0
    rgb = np.where(ba > 0.004, b[..., :3] / np.maximum(ba, 0.004), 0)
    out = np.concatenate([np.clip(rgb, 0, 255), b[..., 3:4]], axis=-1).astype(np.uint8)
    return clean_transparent(Image.fromarray(out, "RGBA"))


def strip(frames):
    w, h = frames[0].size
    s = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        s.paste(f, (0, i * h))
    return s


def seam_error(frames):
    """Mean absolute difference between the last frame -> first frame step and the average step (1.0 = as smooth as any step)."""
    def d(a, b):
        return np.abs(np.array(a, np.float64) - np.array(b, np.float64)).mean()
    steps = [d(frames[i], frames[i + 1]) for i in range(len(frames) - 1)]
    wrap = d(frames[-1], frames[0])
    return wrap / max(1e-6, float(np.mean(steps)))


def checker(size, cell=8):
    c = Image.new("RGBA", (size, size), (28, 28, 34, 255))
    px = c.load()
    for y in range(size):
        for x in range(size):
            if ((x // cell) + (y // cell)) % 2:
                px[x, y] = (40, 40, 48, 255)
    return c


def on_bg(img, scale=1, bg=None):
    bgi = (bg or checker(img.size[0] * scale)).copy()
    big = img.resize((img.size[0] * scale, img.size[1] * scale), Image.NEAREST if scale > 1 else Image.LANCZOS)
    bgi.alpha_composite(big)
    return bgi


def previews(idn, hd_frames, sd_frames, pdir):
    os.makedirs(pdir, exist_ok=True)
    # GIF of the HD loop
    gif = [on_bg(f).convert("P", palette=Image.ADAPTIVE) for f in hd_frames]
    gif[0].save(os.path.join(pdir, f"{idn}_hd.gif"), save_all=True, append_images=gif[1:], duration=FRAMETIME * 50, loop=0, disposal=2)
    # GIF of the 32 px loop at 6x nearest (what the skill lists show)
    g32 = [on_bg(f, 6).convert("P", palette=Image.ADAPTIVE) for f in sd_frames]
    g32[0].save(os.path.join(pdir, f"{idn}_32px_x6.gif"), save_all=True, append_images=g32[1:], duration=FRAMETIME * 50, loop=0, disposal=2)
    # contact sheet: 8 evenly spaced HD frames in two rows, plus the 32 px frames below at 3x
    pick = [hd_frames[i * (len(hd_frames) // 8)] for i in range(8)]
    sheet = Image.new("RGBA", (800, HD * 2 + SD * 3 + 8), (20, 20, 26, 255))
    for i, f in enumerate(pick):
        sheet.alpha_composite(on_bg(f), (144 + (i % 4) * 128 + (i % 4) * 4, (i // 4) * HD))
    for i in range(8):
        sheet.alpha_composite(on_bg(sd_frames[i * (len(sd_frames) // 8)], 3), ((i % 8) * 100 + 2, HD * 2 + 8))
    sheet = sheet.convert("RGB")
    sheet.save(os.path.join(pdir, f"{idn}_sheet.png"))
    # one big still (the hero frame) for a quick look
    on_bg(hd_frames[0], 3).convert("RGB").save(os.path.join(pdir, f"{idn}_frame0_x3.png"))


def build(idn, draw, hero_t=0.0, frames=FRAMES, out=None, preview_dir=None):
    """Render the icon and write every file. draw(t, S) -> RGBA image (S x S). Returns (hd_frames, sd_frames)."""
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview-dir", default=preview_dir or os.path.join("/tmp/claude-0/icons", idn))
    ap.add_argument("--no-write", action="store_true", help="only write the previews, not the game files")
    ns, _ = ap.parse_known_args()
    out = out or OUT
    S = HD * SUPER
    hd_frames, sd_frames = [], []
    for i in range(frames):
        big = draw(i / frames, S)
        assert big.size == (S, S), "draw() must return an S x S image"
        hd = to_hd(big.convert("RGBA"))
        hd_frames.append(hd)
        sd_frames.append(to_sd(hd))
    hero = int(round(hero_t * frames)) % frames
    err = seam_error(hd_frames)
    print(f"{idn}: {frames} frames, hero frame {hero}, loop seam ratio {err:.2f} (<= 1.5 is seamless)")
    if not ns.no_write:
        os.makedirs(out, exist_ok=True)
        strip(hd_frames).save(os.path.join(out, f"{idn}_anim_hd.png"), optimize=True)
        strip(sd_frames).save(os.path.join(out, f"{idn}_anim.png"), optimize=True)
        sd_frames[hero].save(os.path.join(out, f"{idn}.png"), optimize=True)
        meta = json.dumps({"animation": {"frametime": FRAMETIME}}, indent=2) + "\n"
        for n in (f"{idn}_anim.png.mcmeta", f"{idn}_anim_hd.png.mcmeta"):
            with open(os.path.join(out, n), "w", newline="\n") as fh:
                fh.write(meta)
        print("wrote", os.path.join(out, f"{idn}_anim_hd.png"), "+ _anim.png + .png + 2 mcmeta")
    previews(idn, hd_frames, sd_frames, ns.preview_dir)
    print("previews in", ns.preview_dir)
    return hd_frames, sd_frames


# ---- small drawing helpers the icon scripts may share -------------------------------------------------------------

def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(len(a)))


def canvas(S):
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def glow(img, radius, strength=1.0):
    """Additive-looking bloom: blur the image, scale alpha, and composite it under the original."""
    g = img.filter(ImageFilter.GaussianBlur(radius))
    if strength != 1.0:
        a = np.array(g).astype(np.float64)
        a[..., 3] = np.clip(a[..., 3] * strength, 0, 255)
        g = Image.fromarray(a.astype(np.uint8), "RGBA")
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.alpha_composite(g)
    out.alpha_composite(img)
    return out


def disc_mask(S, r_frac=0.47):
    """Soft-edged round mask: icons live inside a disc so they read at 32 px like the existing ones."""
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.hypot(xx - (S - 1) / 2, yy - (S - 1) / 2) / (S * r_frac)
    m = np.clip((1.0 - d) * S * r_frac / 1.5, 0, 1)
    return Image.fromarray((m * 255).astype(np.uint8), "L")


if __name__ == "__main__":
    sys.exit("icon_anim_lib is a library; run tools/gen_<id>_icon_anim.py")
