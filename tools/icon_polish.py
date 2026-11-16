"""3.86.0 skill icon polish. Reads the original 32x32 icon (the classic copy under textures/skill/classic/ if it exists, else the file itself,
which is then saved as the classic copy first, so nothing is ever lost) and writes the polished icon over the live path.

    python tools/icon_polish.py [--sheet out.png] [--only substring]

Polish (all at 8x working size, reduced with Lanczos): local-contrast (S-curve) + saturation lift, a soft top-left light and bottom-right shade
over the interior, a bright inner lip and a dark 1px rim just inside the frame, a dark outline round any glyph that touches the frame-free
interior is NOT added (the frames are kept as drawn), an unsharp pass tuned for 32 px, and a 1 px dark outer rim for contrast on the slot.
Deterministic and idempotent (always starts from the classic copy). Skips: layers (sym_, bg_, mod_), sheets, animation strips (_anim, _hd), db_ icons
(replaced by the Dragon Ball rework), the classic folder itself.
"""
import os, sys
import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "skill")
CLASSIC = os.path.join(TEX, "classic")
SKIP_PREFIX = ("sym_", "bg_", "mod_", "db_", "sheet")
SKIP_PART = ("_anim", "_hd", "_strip")
SKIP_DIR = ("veldanava" + os.sep,)          # the animated hero icons: their stills must stay frames of the strip (AnimatedIconsTest)


def skip(rel):
    base = os.path.basename(rel)
    return (rel.startswith(SKIP_DIR) or os.path.exists(os.path.join(TEX, "hd", rel)) or rel.startswith("hd" + os.sep) or rel.startswith("classic" + os.sep) or base.startswith(SKIP_PREFIX) or any(p in base for p in SKIP_PART))


def polish(im):
    im = im.convert("RGBA")
    W = im.width
    S = 8
    big = im.resize((W * S, W * S), Image.BICUBIC)
    a = np.asarray(big).astype(np.float32) / 255.0
    rgb, al = a[..., :3], a[..., 3:4]
    # saturation lift + gentle S-curve on luminance
    lum = (rgb * np.array([0.299, 0.587, 0.114], np.float32)).sum(-1, keepdims=True)
    rgb = np.clip(lum + (rgb - lum) * 1.22, 0, 1)
    lum2 = (rgb * np.array([0.299, 0.587, 0.114], np.float32)).sum(-1, keepdims=True)
    curve = lum2 + 0.95 * (lum2 - 0.5) * lum2 * (1 - lum2) * 2.0
    rgb = np.clip(rgb * (curve / np.maximum(lum2, 1e-4)), 0, 1)
    # light from the top-left, shade to the bottom-right
    n = W * S
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32) / n
    light = (1.0 - (xx + yy)) * 0.5            # +0.5 top-left .. -0.5 bottom-right
    rgb = np.clip(rgb * (1.0 + light[..., None] * 0.22), 0, 1)
    # vignette toward the edges keeps the glyph in the middle
    r = np.sqrt((xx - 0.5) ** 2 + (yy - 0.5) ** 2) / 0.7071
    rgb = np.clip(rgb * (1.0 - 0.14 * r[..., None] ** 2), 0, 1)
    out = np.concatenate([rgb, al], -1)
    img = Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA").resize((W, W), Image.LANCZOS)
    img = img.filter(ImageFilter.UnsharpMask(radius=1.6, percent=55, threshold=2)).filter(ImageFilter.UnsharpMask(radius=0.6, percent=60, threshold=1))
    arr = np.asarray(img).copy()
    arr[arr[..., 3] == 0, :3] = 0                # no colour under a transparent pixel
    # 1 px dark outer rim (only where the icon is opaque and borders transparency or the image edge)
    opaque = arr[..., 3] > 128
    pad = np.pad(opaque, 1, constant_values=False)
    edge = opaque & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    arr[edge, :3] = (arr[edge, :3] * 0.55).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def files(only=None):
    for d, _, fs in os.walk(TEX):
        for f in sorted(fs):
            rel = os.path.relpath(os.path.join(d, f), TEX)
            if f.endswith(".png") and not skip(rel) and (only is None or only in rel):
                if Image.open(os.path.join(d, f)).size == (32, 32):
                    yield rel


def main():
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    done = []
    for rel in files(only):
        live = os.path.join(TEX, rel)
        old = os.path.join(CLASSIC, rel)
        if not os.path.exists(old):
            os.makedirs(os.path.dirname(old), exist_ok=True)
            Image.open(live).save(old)
        polish(Image.open(old)).save(live)
        done.append(rel)
    print("polished", len(done), "icons")
    if "--sheet" in sys.argv:
        out = sys.argv[sys.argv.index("--sheet") + 1]
        Z, cols = 4, 16
        rows = (len(done) + cols - 1) // cols
        sh = Image.new("RGBA", (cols * 32 * Z * 2 // 1, rows * 32 * Z), (30, 30, 30, 255))
        for i, rel in enumerate(done[: cols * rows]):
            for k, p in enumerate((os.path.join(CLASSIC, rel), os.path.join(TEX, rel))):
                t = Image.open(p).convert("RGBA").resize((32 * Z, 32 * Z), Image.NEAREST)
                sh.alpha_composite(t, ((i % (cols // 2)) * 32 * Z * 2 + k * 32 * Z, (i // (cols // 2)) * 32 * Z))
        sh.save(out)


if __name__ == "__main__":
    main()
