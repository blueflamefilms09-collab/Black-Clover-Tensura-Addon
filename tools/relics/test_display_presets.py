"""Tests of the oversized-weapon rule (python tools/relics/test_display_presets.py). Pure maths and a temp-dir run; touches no repo file."""
import json
import math
import os
import re
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import display_presets as dp  # noqa: E402

ROT_R = dp.BASE_ROTATION["firstperson_righthand"]


def mesh(length_blocks, angle=45.0, sc=0.02, ox=0.03, oy=0.03, width=20.0, depth=3.0):
    """A synthetic mesh of the given length: butt at ymin = 0, box width x depth."""
    ymax = length_blocks / sc
    return dp.MeshInfo(ox, oy, angle, sc, 0.0, (-width / 2, width / 2, 0.0, ymax, -depth / 2, depth / 2))


def approx(a, b, tol=1e-6):
    return all(abs(x - y) <= tol for x, y in zip(a, b))


class MathsTest(unittest.TestCase):
    def test_rotation_matches_the_vanilla_pose(self):
        r = dp.rot_matrix(*ROT_R)
        v = dp.mat_vec(r, (1, 0, 0))                       # Rz(25) first, then Ry(-90): +x ends up toward the viewer (+z)
        self.assertAlmostEqual(v[2], math.cos(math.radians(25)), places=6)
        self.assertAlmostEqual(v[1], math.sin(math.radians(25)), places=6)
        self.assertAlmostEqual(v[0], 0.0, places=6)

    def test_the_anchor_is_where_a_sprite_swords_hilt_sits(self):
        a = dp.anchor(ROT_R)
        self.assertTrue(approx(a, (0.6306, -0.5459, -0.7316), 1e-3), a)

    def test_the_length_is_the_y_extent_times_the_scale(self):
        m = mesh(2.0)
        self.assertAlmostEqual(m.length, 2.0, places=6)
        tip = m.tip()
        self.assertAlmostEqual(math.hypot(tip[0] - m.ox, tip[1] - m.oy), 2.0, places=6, msg="the tip is one length from the butt along the axis")

    def test_the_translation_puts_the_grip_on_the_anchor_at_every_scale(self):
        m = mesh(2.0)
        r = dp.rot_matrix(*ROT_R)
        for s in (0.3, 0.45, 0.68):
            t = dp.scale(dp.translation_for(m, ROT_R, s), 1 / 16.0)
            grip_cam = dp.add(dp.add(dp.HAND, t), dp.mat_vec(r, dp.scale(dp.sub(m.grip(), (0.5, 0.5, 0.5)), s)))
            self.assertTrue(approx(grip_cam, dp.anchor(ROT_R), 1e-9), (s, grip_cam))

    def test_a_short_weapon_keeps_the_full_handheld_scale(self):
        self.assertAlmostEqual(dp.max_scale(mesh(1.2), ROT_R), dp.BASE_SCALE, places=6)

    def test_the_scale_shrinks_as_the_weapon_grows_and_the_tip_stays_in_view(self):
        last = 1.0
        for length in (1.5, 1.8, 2.2, 2.6, 3.0):
            m = mesh(length)
            s, t, ok = dp.preset(m, ROT_R)
            self.assertLessEqual(s, last + 1e-9, "longer never gets a bigger scale")
            self.assertLessEqual(s * length, dp.APPARENT_MAX_BLOCKS + 1e-6, "apparent length is capped")
            self.assertTrue(ok or s <= dp.SCALE_FLOOR + 1e-9, "the tip is in view unless the floor was hit: %s %s" % (length, s))
            last = s

    def test_every_corner_is_in_front_of_the_camera(self):
        m = mesh(2.4, width=40.0)
        s, t, ok = dp.preset(m, ROT_R)
        r = dp.rot_matrix(*ROT_R)
        for q in m.corners():
            c = dp.add(dp.anchor(ROT_R), dp.mat_vec(r, dp.scale(dp.sub(q, m.grip()), s)))
            self.assertLessEqual(c[2], -dp.Z_MARGIN + 1e-6, (q, c))

    def test_the_rounding_goes_down_never_up(self):
        m = mesh(2.0)
        exact = dp.max_scale(m, ROT_R)
        s, _, _ = dp.preset(m, ROT_R)
        self.assertLessEqual(s, exact + 1e-12)
        self.assertGreater(s, exact - 0.001 - 1e-12)

    def test_the_java_guard_repeats_the_same_numbers(self):
        java = os.path.join(dp.ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "relics", "item", "OversizeGuard.java")
        with open(java, encoding="utf-8") as f:
            text = f.read()
        self.assertEqual(float(re.search(r"APPARENT_MAX = ([0-9.]+)f", text).group(1)), dp.APPARENT_MAX_BLOCKS)
        self.assertEqual(float(re.search(r"GRIP_FRACTION = ([0-9.]+)f", text).group(1)), dp.GRIP_FRACTION)


def write_mesh(path, length_blocks, sc=0.02):
    ymax = length_blocks / sc
    quad = "q 0 -5 0 0 0 0  5 0 0 0 0  5 %f 0 0 0  -5 %f 0 0 0  0 0 1\n" % (ymax, ymax)
    with open(path, "w") as f:
        f.write("place 0.03 0.03 45 %s 0\n" % sc + quad)


def model(first_scale=0.68, third_scale=1.5, parent="minecraft:builtin/entity"):
    disp = {}
    for side, (r1, r2) in (("righthand", ((0, -90, 55), (0, -90, 25))), ("lefthand", ((0, 90, -55), (0, 90, -25)))):
        disp["thirdperson_" + side] = {"rotation": list(r1), "translation": [0, 4, 0.5], "scale": [third_scale] * 3}
        disp["firstperson_" + side] = {"rotation": list(r2), "translation": [1.13, 3.2, 1.13], "scale": [first_scale] * 3}
    disp["gui"] = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}
    return {"parent": parent, "textures": {"particle": "nusmp:item/x"}, "display": disp}


class FilesTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.meshes, self.models = os.path.join(self.tmp, "m"), os.path.join(self.tmp, "j")
        os.makedirs(self.meshes)
        os.makedirs(self.models)

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def put(self, name, length, mdl=None):
        write_mesh(os.path.join(self.meshes, name + ".txt"), length)
        with open(os.path.join(self.models, name + ".json"), "w") as f:
            f.write(json.dumps(mdl or model(), indent=2))

    def load(self, name):
        with open(os.path.join(self.models, name + ".json")) as f:
            return json.load(f)

    def read(self, name):
        with open(os.path.join(self.models, name + ".json")) as f:
            return f.read()

    def run_tool(self, apply=True):
        return dp.run(apply=apply, models_dir=self.models, meshes_dir=self.meshes)

    def test_only_the_first_person_entries_change(self):
        self.put("long", 2.4)
        before = self.load("long")
        entries, changed = self.run_tool()
        after = self.load("long")
        self.assertEqual(changed, ["long"])
        for k in before["display"]:
            if k.startswith("firstperson"):
                self.assertLess(after["display"][k]["scale"][0], 0.68)
                self.assertEqual(after["display"][k]["rotation"], before["display"][k]["rotation"], "the rotation is kept")
            else:
                self.assertEqual(after["display"][k], before["display"][k], k + " is untouched (third person stays massive)")
        self.assertEqual(after["display"]["firstperson_lefthand"]["scale"], after["display"]["firstperson_righthand"]["scale"])
        self.assertIn(dp.MARKER, after)

    def test_it_is_idempotent_and_ignores_what_it_wrote_last_time(self):
        self.put("long", 2.4)
        self.run_tool()
        first = self.read("long")
        entries, changed = self.run_tool()
        self.assertEqual(changed, [])
        self.assertEqual(self.read("long"), first)
        # a json with a hand-made odd scale ends in the same place
        self.put("odd", 2.4, model(first_scale=0.9))
        self.put("plain", 2.4)
        self.run_tool()
        self.assertEqual(self.load("odd")["display"]["firstperson_righthand"], self.load("plain")["display"]["firstperson_righthand"])

    def test_a_short_weapon_is_left_alone_and_a_shrunken_one_gets_the_handheld_back(self):
        self.put("short", 1.2)
        before = self.read("short")
        self.run_tool()
        self.assertEqual(self.read("short"), before)
        self.put("grows", 2.4)
        self.run_tool()
        write_mesh(os.path.join(self.meshes, "grows.txt"), 1.0)                  # the mesh was rebuilt shorter
        entries, changed = self.run_tool()
        self.assertEqual(changed, ["grows"])
        back = self.load("grows")
        self.assertNotIn(dp.MARKER, back)
        self.assertEqual(back["display"]["firstperson_righthand"]["scale"], [0.68] * 3)
        self.assertEqual(back["display"]["firstperson_righthand"]["translation"], list(dp.BASE_TRANSLATION))

    def test_non_3d_models_are_skipped(self):
        self.put("sprite", 2.4, model(parent="minecraft:item/handheld"))
        entries, changed = self.run_tool()
        self.assertEqual(changed, [])
        self.assertEqual(entries[0]["action"], "skip_not_3d")

    def test_a_manual_display_is_never_touched(self):
        m = model()
        m[dp.MARKER] = "manual"
        self.put("hand_tuned", 2.4, m)
        before = self.read("hand_tuned")
        entries, changed = self.run_tool()
        self.assertEqual(changed, [])
        self.assertEqual(self.read("hand_tuned"), before)
        self.assertEqual(entries[0]["action"], "skip_manual" if entries else None)

    def test_a_dry_run_writes_nothing(self):
        self.put("long", 2.4)
        before = self.read("long")
        entries, changed = self.run_tool(apply=False)
        self.assertEqual(changed, ["long"])
        self.assertEqual(self.read("long"), before)


if __name__ == "__main__":
    unittest.main(verbosity=2)
