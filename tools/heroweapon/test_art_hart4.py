"""Why: the art of the five Vassal weapons of hart4 is generated, so the things worth testing are the ones that go wrong silently: the files the game needs are all
there and in budget, a rebuild gives the same bytes (so a changed number in the script cannot hide behind stale pictures), the committed files are what the script writes now,
the size classes fit the meshes, and the small maths of the placement and the curves is right.

    python3 -B tools/heroweapon/test_art_hart4.py          (about two minutes: it builds the five weapons twice into temporary folders)
"""
import contextlib
import hashlib
import io
import math
import os
import shutil
import sys
import tempfile
import unittest
from unittest import mock

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import art_hart4 as a                    # noqa: E402
import art_hart4_kit as k                # noqa: E402


def digest(root, rels):
    out = {}
    for rel in rels:
        with open(os.path.join(root, rel), "rb") as f:
            out[rel] = hashlib.sha256(f.read()).hexdigest()
    return out


def all_files():
    return [rel for i in a.IDS for rel in a.files_of(i)]


def build_into(root):
    k.set_output_root(root)
    try:
        with contextlib.redirect_stdout(io.StringIO()):
            for i in a.IDS:
                a.make(i)
    finally:
        k.set_output_root(None)
    return root


class FilesTest(unittest.TestCase):
    def test_every_file_is_there_and_in_budget(self):
        with contextlib.redirect_stdout(io.StringIO()):
            problems = a.check(a.IDS)
        self.assertEqual(problems, [])

    def test_every_weapon_has_a_class_that_exists(self):
        import weapon_classes as wc
        for i in a.IDS:
            self.assertIn(a.CLASSES[i], wc.load()["classes"], i)
        self.assertEqual(sorted(a.CLASSES), sorted(a.IDS))

    def test_the_class_fragment_is_what_the_script_writes(self):
        import json
        with open(os.path.join(HERE, "classes_hart4.json"), encoding="utf-8") as f:
            self.assertEqual(json.load(f), {i: a.CLASSES[i] for i in a.IDS})

    def test_every_gem_glows_in_the_colour_of_its_weapon(self):
        for i in a.IDS:
            self.assertIsNone(a.gem_glow_problem(i), i)

    def test_the_gem_check_catches_a_wrong_colour_and_a_part_that_does_not_glow(self):
        with mock.patch.dict(a.GEMS, {"vassal_mirror": ("head_gem", (255, 40, 40))}):
            self.assertIn("hue", a.gem_glow_problem("vassal_mirror"))
        with mock.patch.dict(a.GEMS, {"vassal_mirror": ("head_rim", (127, 242, 255))}):
            self.assertIn("no glow quads", a.gem_glow_problem("vassal_mirror"))

    def test_the_sprites_are_16_by_16_and_not_blank(self):
        for i in a.IDS:
            im = Image.open(os.path.join(k.ICON_DIR, i + ".png")).convert("RGBA")
            self.assertEqual(im.size, (16, 16), i)
            self.assertGreater(int((np.asarray(im)[..., 3] > 0).sum()), 40, i)


class BuildTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.first = build_into(tempfile.mkdtemp(prefix="hart4_a_"))
        cls.second = build_into(tempfile.mkdtemp(prefix="hart4_b_"))

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.first, ignore_errors=True)
        shutil.rmtree(cls.second, ignore_errors=True)

    def test_two_builds_write_the_same_bytes(self):
        self.assertEqual(digest(self.first, all_files()), digest(self.second, all_files()))

    def test_the_committed_files_are_what_the_script_writes_now(self):
        """If this fails the script was changed (or a file hand-edited) without running python3 -B tools/heroweapon/art_hart4.py."""
        self.assertEqual(digest(self.first, all_files()), digest(k.ml.ASSETS, all_files()))


class MathTest(unittest.TestCase):
    def diagonal_sprite(self):
        """A one-pixel line from the lower left to the upper right of a 16 x 16 sprite, in a folder that is removed after the test."""
        tmp = tempfile.mkdtemp(prefix="hart4_s_")
        self.addCleanup(shutil.rmtree, tmp, True)
        icon = os.path.join(tmp, "s.png")
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for n in range(1, 15):
            im.putpixel((n, 15 - n), (255, 255, 255, 255))
        im.save(icon)
        return icon

    def test_the_hand_holds_the_chosen_point_of_the_mesh(self):
        """place_on_diagonal puts the mesh point at share `grip` of its length where the hand holds a sprite (a quarter of the way along its diagonal)."""
        icon = self.diagonal_sprite()
        for grip in (0.1, 0.25, 0.6):
            m = k.ml.Mesh("t")
            p = k.ml.Part("shaft", material="wood")
            k.ml.lathe(p, "y", (0, 0, 0), [(0.0, 1.0), (50.0, 1.0)], sides=6)
            m.add(p)
            k.place_on_diagonal(m, icon, 1.5, grip)
            ox, oy, ang, sc, ymin = m.place_cfg
            origin, a0, length = k.diag_axis(icon)
            length_blocks = 50.0 * sc
            d = np.array([math.cos(math.radians(ang)), math.sin(math.radians(ang))])
            held = np.array([ox, oy]) + d * grip * length_blocks
            want = origin + d * k.HAND_POINT * length
            self.assertTrue(np.allclose(held, want, atol=1e-6), (grip, held, want))
            self.assertAlmostEqual(length_blocks * k.HAND_SCALE, 1.5, places=6)

    def test_a_quarter_grip_is_what_mesh_lib_does(self):
        """For grip 0.25 the placement is exactly mesh_lib.place_on_sprite's growth about the hand, so the meshes of the other builders and these agree.
        The sprite here is a one-pixel diagonal, whose principal axis is the 45 degree line the kit fixes, so all five numbers of the place line must match."""
        icon = self.diagonal_sprite()
        m1, m2 = k.ml.Mesh("a"), k.ml.Mesh("b")
        for m in (m1, m2):
            p = k.ml.Part("shaft", material="wood")
            k.ml.lathe(p, "y", (0, 0, 0), [(0.0, 1.0), (40.0, 1.0)], sides=6)
            m.add(p)
        k.place_on_diagonal(m1, icon, 1.2, 0.25)
        m2.place_on_sprite(icon, real_m=1.2, grip=0.25)
        self.assertEqual(len(m1.place_cfg), 5)
        self.assertTrue(np.allclose(m1.place_cfg, m2.place_cfg, atol=1e-6), (m1.place_cfg, m2.place_cfg))

    def test_the_spline_goes_through_its_control_points(self):
        pts = [(0, 0, 0), (3, 1, 0), (5, 4, 1), (9, 4, 2)]
        dense = k.catmull(pts, per=8)
        for p in pts:
            self.assertTrue(min(np.linalg.norm(np.array(q) - np.array(p)) for q in dense) < 1e-9, p)

    def test_arc_points_are_evenly_spaced(self):
        dense = k.catmull([(0, 0, 0), (10, 5, 0), (20, 0, 0), (30, 8, 0)], per=12)
        pts = k.arc_points(dense, 11)
        gaps = [np.linalg.norm(np.array(pts[i + 1]) - np.array(pts[i])) for i in range(10)]
        self.assertLess(max(gaps) - min(gaps), 0.6)

    def test_a_chain_link_is_a_closed_surface(self):
        p = k.ml.Part("link", material="iron")
        k.chain_link(p, (0, 0, 0), (0, 1, 0), (1, 0, 0), 12.4, 7.4, 1.6, sides=5, n_arc=3)
        self.assertTrue(p.is_closed(), "the seam of the loop must be welded (no crack where the sweep ends)")
        lo, hi = p.bbox()
        length, width = float(hi[1] - lo[1]), float(hi[0] - lo[0])
        tip_short = 7.4 / 2.0 * (1.0 - math.cos(math.pi / 6.0))     # the three chords of an end arc fall this far short of the true arc at the tip
        self.assertGreater(length, 12.4 - 2.0 * tip_short)           # the centre line of the polygon ...
        self.assertLess(length, 12.4 + 3.2 + 1e-6)                   # ... never longer than the true stadium plus the thickness of the wire
        self.assertGreater(width, 7.4)
        self.assertLess(width, 7.4 + 3.2 + 1e-6)

    def test_a_swept_loop_closes_where_a_plain_sweep_leaves_a_crack(self):
        """The reason sweep_loop exists: ml.sweep of the same ring with its first point repeated has two end rings with different tangents."""
        pts = [(5.0 * math.cos(a), 5.0 * math.sin(a), 0.0) for a in np.linspace(0.0, 2.0 * math.pi, 8, endpoint=False)]
        good = k.ml.Part("good", material="iron")
        k.sweep_loop(good, pts, k.ml.circle(0.6, 4), up=(0, 0, 1))
        bad = k.ml.Part("bad", material="iron")
        k.ml.sweep(bad, pts + [pts[0]], k.ml.circle(0.6, 4), caps=(False, False), up=(0, 0, 1))
        self.assertTrue(good.is_closed())
        self.assertFalse(bad.is_closed())

    def test_the_outline_of_the_mirror_is_counter_clockwise_and_egg_shaped(self):
        pts = k.oval_outline(10.0, 15.0, n=64, egg=0.1)
        area = 0.5 * sum(pts[i][0] * pts[(i + 1) % 64][1] - pts[(i + 1) % 64][0] * pts[i][1] for i in range(64))
        self.assertGreater(area, 0.0)
        top = max(p[1] for p in pts)
        wide_top = max(abs(p[0]) for p in pts if p[1] > 0.0)
        wide_bottom = max(abs(p[0]) for p in pts if p[1] < 0.0)
        self.assertGreater(top, 14.9)
        self.assertGreater(wide_top, wide_bottom)


if __name__ == "__main__":
    unittest.main(verbosity=2)
