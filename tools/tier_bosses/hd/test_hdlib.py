"""Tests of the HD outfit tooling (python3 -B tools/tier_bosses/hd/test_hdlib.py), 3.38.0. See docs/bosses/hd_outfits.md.

Pure checks, no game: the size rule of every shipped Tier List skin and glow map (the Java twin is TierBossesTest), the Canvas (sizes, seams, binary overlay alpha, opaque base, premultiplied glow),
the 256 -> 128 / 64 downsample, the generic bridge on a synthetic blueprint, the tuple contract of every tools/tier_bosses/hd/<id>.py, and the --size plumbing of gen_tier_looks.py.
"""
import json
import os
import sys
import unittest

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TB = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TB, "..", ".."))
for p in (HERE, TB, os.path.join(ROOT, "tools"), os.path.join(ROOT, "tools", "playermodel"), os.path.join(ROOT, "tools", "arc")):
    if p not in sys.path:
        sys.path.insert(0, p)
sys.dont_write_bytecode = True

import hd_core as C  # noqa: E402
import hdlib as H  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
SKINS = os.path.join(ASSETS, "textures", "entity", "playermodel")
LOOKS_TIER = os.path.join(ASSETS, "playermodel", "looks_tier.json")
HD_IDS = sorted(f[:-3] for f in os.listdir(HERE) if f.endswith(".py") and f[:-3] not in ("hdlib", "hd_core", "hd_detail", "hd_bridge", "test_hdlib") and not f.startswith("_"))


def spec_masks(cid):
    import gen_hair
    from arc_kit import spec as S
    from tier_specs import SHEETS
    gen_hair.build()
    masks = {sid: getattr(st, "mask", None) for sid, st in gen_hair.STYLES.items()}
    raw = dict(SHEETS[cid])
    v2 = raw.pop("_v2", None)
    spec = S.normalize(raw, cid)
    if v2:
        spec["_v2"] = v2
    return spec, masks


def hd_module(cid):
    import importlib.util
    s = importlib.util.spec_from_file_location("hd_test_" + cid, H.hd_module_path(cid))
    m = importlib.util.module_from_spec(s)
    s.loader.exec_module(m)
    return m


def base_islands(cv):
    return [cv.face(part, nm, False) for part in C.PARTS for nm in C.FACES]


class ShippedSkinSizes(unittest.TestCase):
    def test_every_tier_skin_and_glow_map_is_square_a_multiple_of_64_and_the_glow_matches(self):
        looks = json.load(open(LOOKS_TIER))["looks"]
        self.assertGreater(len(looks), 0)
        for cid, look in looks.items():
            path = os.path.join(SKINS, look["skin"] + ".png")
            self.assertTrue(os.path.isfile(path), path)
            w, h = Image.open(path).size
            self.assertEqual(w, h, cid + " must be square")
            self.assertEqual(w % 64, 0, cid + " must be a multiple of 64")
            self.assertIn(w // 64, (1, 2, 4, 8, 16), cid + " must be a power-of-two multiple of 64")
            if look.get("glow"):
                gp = os.path.join(SKINS, look["skin"] + "_glow.png")
                self.assertTrue(os.path.isfile(gp), "glow map of " + cid)
                self.assertEqual(Image.open(gp).size, (w, h), "the glow map of " + cid + " must be as big as its skin")

    def test_every_hd_module_has_its_hd_png(self):
        for cid in HD_IDS:
            self.assertEqual(Image.open(os.path.join(SKINS, cid + ".png")).size, (256, 256), cid)


class CanvasRules(unittest.TestCase):
    def test_sizes(self):
        for n in (64, 128, 256):
            skin, glow = C.Canvas(n).finish()
            self.assertEqual(skin.size, (n, n))
            self.assertIsNone(glow)
        for bad in (0, 32, 96, 192, 300):
            with self.assertRaises(ValueError):
                C.Canvas(bad)

    def test_a_shape_far_bigger_than_its_face_never_crosses_a_seam(self):
        cv = C.Canvas(256)
        f = cv.face("head", "front")
        f.ellipse(4, 4, 60, 60, "#ff0000")
        f.poly([(-30, -30), (50, -30), (50, 50), (-30, 50)], "#00ff00", a=0.5)
        f.line((-20, 4), (30, 4), "#0000ff", 6.0)
        px = cv.px.copy()
        inside = np.zeros(px.shape[:2], bool)
        inside[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W] = True
        self.assertEqual(float(px[~inside][..., 3].max()), 0.0, "paint leaked out of the island")
        self.assertGreater(float(px[inside][..., 3].min()), 0.99)

    def test_overlay_is_binary_base_is_opaque_glow_is_premultiplied(self):
        cv = C.Canvas(256)
        for f in base_islands(cv):
            f.fill(f.m_all(), "#c08060")
        o = cv.face("body", "front", True)
        o.ellipse(4, 6, 2.2, 2.0, "#ffffff", a=0.6, soft=3.0)                    # a soft, half transparent shape
        o.ellipse(4, 6, 1.0, 1.0, "#ffd070", glow=True)
        skin, glow = cv.finish()
        a = np.asarray(skin)[..., 3]
        ov = a[o.y0:o.y0 + o.H, o.x0:o.x0 + o.W]
        self.assertTrue(set(np.unique(ov).tolist()) <= {0, 255}, "the overlay layer is a cutout: alpha must be 0 or 255")
        self.assertGreater(int((ov == 255).sum()), 0)
        for f in base_islands(cv):
            self.assertTrue((a[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W] == 255).all(), repr(f))
        self.assertIsNotNone(glow)
        g = np.asarray(glow).astype(int)
        self.assertTrue((g[..., :3] <= g[..., 3:4] + 1).all(), "the glow map is premultiplied: rgb <= alpha")

    def test_no_nan_leaves_the_canvas(self):
        cv = C.Canvas(256)
        f = cv.face("body", "front")
        f.fill(f.m_all(), "#808080")
        H.sheen(f, f.m_all(), [1.0, 3.0], width=0.2, strength=0.5)
        skin, _ = cv.finish()
        self.assertEqual(skin.size, (256, 256))

    def test_slim_arm_is_three_wide(self):
        self.assertEqual(C.Canvas(256, slim=True).face("arm_r", "front").w, 3)
        self.assertEqual(C.Canvas(256, slim=False).face("arm_r", "front").w, 4)


class Downsample(unittest.TestCase):
    def test_exact_blocks_and_binary_alpha(self):
        cv = C.Canvas(256)
        for f in base_islands(cv):
            f.fill(f.m_all(), "#406080")
        o = cv.face("head", "front", True)
        o.ellipse(4, 4, 2.5, 2.5, "#ffffff")
        skin, glow = cv.finish()
        for n in (128, 64):
            d = C.downsample(skin, n)
            self.assertEqual(d.size, (n, n))
            self.assertTrue(set(np.unique(np.asarray(d)[..., 3]).tolist()) <= {0, 255})
        self.assertIs(C.downsample(skin, 256), skin)
        with self.assertRaises(ValueError):
            C.downsample(skin, 100)


class Bridge(unittest.TestCase):
    def blueprint(self):
        bp = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        px = bp.load()
        cv = C.Canvas(64)
        for f in base_islands(cv):
            for y in range(f.H):
                for x in range(f.W):
                    px[f.x0 + x, f.y0 + y] = (200, 150, 130, 255) if (x + y) % 5 else (120, 90, 70, 255)
        return bp

    def test_hd_paint_gives_a_256_skin_with_every_base_island_opaque(self):
        skin, glow = H.hd_paint("synthetic", self.blueprint())
        self.assertEqual(skin.size, (256, 256))
        self.assertIsNone(glow)
        a = np.asarray(skin)[..., 3]
        cv = C.Canvas(256)
        for f in base_islands(cv):
            self.assertTrue((a[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W] == 255).all(), repr(f))
        for f in (cv.face(p, n, True) for p in C.PARTS for n in C.FACES):
            self.assertEqual(int(a[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W].max()), 0, "an empty overlay stays empty: " + repr(f))

    def test_glow_blueprint_is_carried(self):
        bp = self.blueprint()
        gl = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        f = C.Canvas(64).face("head", "front")
        gl.putpixel((f.x0 + 3, f.y0 + 3), (255, 200, 80, 255))
        skin, glow = H.hd_paint("synthetic", bp, gl)
        self.assertIsNotNone(glow)
        self.assertEqual(glow.size, skin.size)

    def test_hd_paint_128(self):
        skin, _ = H.hd_paint("synthetic", self.blueprint(), size=128)
        self.assertEqual(skin.size, (128, 128))


class HdModules(unittest.TestCase):
    def test_modules_exist(self):
        self.assertIn("charlos", HD_IDS)
        self.assertIn("mr_2_bon_clay", HD_IDS)

    def test_each_module_keeps_the_remake_tuple_contract_at_256(self):
        for cid in HD_IDS:
            spec, masks = spec_masks(cid)
            skin, glow, hair, hrgb, slim = hd_module(cid).paint(spec, cid, masks)
            self.assertEqual(skin.size, (256, 256), cid)
            self.assertEqual(skin.mode, "RGBA")
            if glow is not None:
                self.assertEqual(glow.size, skin.size, cid + ": the glow map must be as big as the skin")
            self.assertIsInstance(hair, str)
            self.assertEqual(len(hrgb), 3)
            self.assertIsInstance(slim, bool)
            a = np.asarray(skin)[..., 3]
            cv = C.Canvas(256, slim)
            for f in base_islands(cv):
                self.assertTrue((a[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W] == 255).all(), cid + " base hole in " + repr(f))
            for f in (cv.face(p, n, True) for p in C.PARTS for n in C.FACES):
                self.assertTrue(set(np.unique(a[f.y0:f.y0 + f.H, f.x0:f.x0 + f.W]).tolist()) <= {0, 255}, cid + " soft overlay alpha in " + repr(f))

    def test_hd_is_richer_than_the_64_blueprint(self):
        for cid in HD_IDS:
            spec, masks = spec_masks(cid)
            skin, *_ = hd_module(cid).paint(spec, cid, masks)
            bp, *_ = H.blueprint(cid, spec, masks)
            hd_colours = len({tuple(c) for c in np.asarray(skin).reshape(-1, 4).tolist()})
            bp_colours = len({tuple(c) for c in np.asarray(bp).reshape(-1, 4).tolist()})
            self.assertGreater(hd_colours, 3 * bp_colours, cid + ": the HD skin should carry far more tones than its 64x64 blueprint")


class GenTierLooks(unittest.TestCase):
    def test_size_plumbing(self):
        import gen_tier_looks as G
        self.assertEqual(G.SIZES, (64, 128, 256))
        self.assertIsNone(G.load_hd("no_such_character"))
        self.assertTrue(callable(G.load_hd("charlos")))
        img = Image.new("RGBA", (256, 256), (10, 20, 30, 255))
        glow = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        for n in (64, 128, 256):
            i2, g2 = G.to_size(img, glow, n)
            self.assertEqual(i2.size, (n, n))
            self.assertEqual(g2.size, (n, n))
        i3, g3 = G.to_size(img, None, 64)
        self.assertIsNone(g3)

    def test_the_64_remake_modules_are_still_there(self):
        for cid in HD_IDS:
            self.assertTrue(os.path.isfile(os.path.join(TB, "remake", cid + ".py")), "the 64x64 remake of " + cid + " must stay (it is the HD blueprint and the --size 64 version)")


if __name__ == "__main__":
    unittest.main(verbosity=2)
