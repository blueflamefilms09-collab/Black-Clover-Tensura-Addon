"""Tests of the HD arc tooling (python3 -B tools/arc_hd/test_arc_hd.py), 3.38. See docs/arc/hd_arc.md.

Pure checks, no game: the hair table (every id has a style or a reason, every style exists), the roster, the fragments against looks.json (what merge_looks.py --check says), the size rule of every shipped
HD skin and its glow map (the Java twin is ArcHdSkinsTest), the guard of gen_skins.py (an HD id is not regenerated at 64x64), the contract of every hand-painted module in tools/arc_hd/chars (the tuple, the
binary overlay alpha, the opaque base layer, the second layer only where the 64x64 blueprint has cloth) and that `--size 64` reproduces the old sheet.
"""
import os
import sys
import unittest

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import arc_hdlib as A  # noqa: E402
import merge_looks as ML  # noqa: E402

H = A.H


class RosterAndHair(unittest.TestCase):
    def test_every_roster_id_has_a_look_and_a_hair_reason(self):
        table = A.job_table()
        names = A.expand(A.RO.BASE_IDS)
        self.assertGreaterEqual(len(names), 70)
        for n in names:
            self.assertIn(n, table, n)
            hit = A.HM.lookup(n)
            self.assertIsNotNone(hit, "%s is not in hair_map.py" % n)
            self.assertTrue(hit[1], "%s has no hair reason" % n)

    def test_hair_table_only_names_real_styles(self):
        self.assertEqual(A.HM.check(), [])


class Fragments(unittest.TestCase):
    def test_fragments_match_looks_json(self):
        import json
        text = open(A.LOOKS_JSON).read()
        doc = json.loads(text)
        self.assertEqual(ML.dump(doc), text, "looks.json is not in the writer's format")
        new, replaced, problems = ML.merge(doc, A.read_fragments())
        self.assertEqual(problems, [])
        self.assertEqual(ML.dump(new), text, "looks.json differs from the merge of the fragments: run tools/arc_hd/merge_looks.py")
        self.assertEqual(ML.fragment_problems(A.read_fragments()), [])

    def test_shipped_skins_are_hd_squares_with_matching_glow(self):
        frags = A.read_fragments()
        self.assertGreaterEqual(len(frags), 70)
        for name, fr in frags.items():
            skin = os.path.join(A.SKINS, fr["look"]["skin"] + ".png")
            glow = os.path.join(A.SKINS, fr["look"]["skin"] + "_glow.png")
            self.assertEqual(A.png_problems(skin, glow if fr["look"].get("glow") else None, fr["size"]), [], name)
            with Image.open(skin) as im:
                self.assertEqual(im.width, 256, name)
            self.assertEqual(os.path.isfile(glow), bool(fr["look"].get("glow")), name)

    def test_gen_skins_leaves_an_hd_id_alone(self):
        import gen_skins as GS
        owned = GS.hd_fragments()
        self.assertIn("meliodas", owned)
        self.assertGreaterEqual(len(owned), 70)


class HandPainted(unittest.TestCase):
    def test_every_hand_module_keeps_the_contract(self):
        masks = A.hair_masks()
        mods = sorted(f[:-3] for f in os.listdir(A.CHARS_DIR) if f.endswith(".py") and not f.startswith("_"))
        self.assertIn("meliodas", mods)
        for name in mods:
            spec = A.spec_of(name)
            img, glow, hair, hrgb, slim = A.load_hand(name)(spec, name, masks)
            self.assertEqual(img.size, (256, 256), name)
            if glow is not None:
                self.assertEqual(glow.size, img.size, name)
            self.assertIn(hair, A.HM.known_styles(), name)
            self.assertEqual(bool(slim), bool(A.GS.is_slim(spec)), name)
            a = np.asarray(img.convert("RGBA"))
            cv = H.Canvas(256, slim)
            cv.px[...] = a.astype(np.float32) / 255.0
            base_ok = ov_ok = True
            for part in H.PARTS:
                for fname in H.FACES:
                    fb = cv.face(part, fname, False)
                    fo = cv.face(part, fname, True)
                    if not (np.isin(np.unique(fo.rgba[..., 3]), (0.0, 1.0)).all()):
                        ov_ok = False
                    if fb.rgba[..., 3].min() < 1.0 and fb.rgba[..., 3].max() > 0.0 and fb.rgba[..., 3].mean() != 1.0:
                        base_ok = False
            self.assertTrue(ov_ok, name + ": the second layer must be a cutout (alpha 0 or 255)")
            self.assertTrue(base_ok, name + ": a base island is half painted")

    def test_the_pilot_second_layer_stays_inside_the_blueprint(self):
        masks = A.hair_masks()
        spec = A.spec_of("meliodas")
        img, _, _, _, slim = A.load_hand("meliodas")(spec, "meliodas", masks)
        a = np.asarray(img.convert("RGBA"), np.float32) / 255.0
        cv = H.Canvas(256, slim)
        cv.px[...] = a
        spec2 = A.spec_of("meliodas")
        spec2["hair"]["color"] = "#f6d044"
        img64, _, _, _, _ = A.blueprint("meliodas", spec2, masks, hair="anime_spike_swept")
        bp = H.Canvas(256, slim)
        A.bridge(bp, img64, None, spec2, (246, 208, 68))
        for part in ("body", "arm_r", "arm_l", "leg_r", "leg_l"):
            for fname in ("front", "back", "west", "east"):
                got = cv.face(part, fname, True).rgba[..., 3] > 0.5
                allowed = bp.face(part, fname, True).rgba[..., 3] > 0.5
                self.assertFalse((got & ~allowed).any(), "%s %s: the second layer has cloth where the sheet has none" % (part, fname))


class SixtyFourMode(unittest.TestCase):
    def test_size_64_is_the_old_generator(self):
        import gen_arc_hd as G
        masks = A.hair_masks()
        spec = A.spec_of("asta")
        img, glow, hs, hrgb, slim, src = G.paint_one("asta", 64, False, masks)
        self.assertEqual(src, "classic64")
        self.assertEqual(img.size, (64, 64))
        self.assertEqual(hs, A.classic_hair("asta", spec))


if __name__ == "__main__":
    unittest.main(verbosity=1)
