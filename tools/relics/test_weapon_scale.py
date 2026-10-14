"""Tests of the weapon size / orientation tooling (python tools/relics/test_weapon_scale.py).

Pure maths on synthetic meshes (the same cases as src/test/.../WeaponOrientationTest.java), the reversible fix tool on a temp mesh, the three copies of the
class table (json, game copy, Java fallback) and the state of the shipped meshes: after tools/relics/weapon_fixes.json no weapon is below its class
minimum, no head below its class minimum, no actionable orientation flag is left (informational ones are listed in the audit).
"""
import json
import math
import os
import re
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import apply_weapon_fixes as af      # noqa: E402
import audit_weapon_scale as aud     # noqa: E402
import weapon_classes as wc          # noqa: E402
import weapon_orientation as wo      # noqa: E402

ROOT = wc.ROOT
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "relics", "item", "WeaponClasses.java")
SC = 0.02


def box(x0, x1, y0, y1, z0, z1):
    c = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 4, 7, 3), (1, 2, 6, 5), (0, 1, 5, 4), (3, 7, 6, 2)]
    n = [(0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0), (0, -1, 0), (0, 1, 0)]
    return [(0, [c[i] for i in q], [n[k]] * 4) for k, q in enumerate(f)]


def axe(hx0, hx1):
    return box(-1, 1, 0, 60, -1, 1) + box(hx0, hx1, 40, 58, -0.5, 0.5)


def mesh_text(quads, place="place 0.10000 0.10000 45.000 %.6f 0.000" % SC):
    lines = ["# synthetic", place]
    for layer, P, N in quads:
        lines.append("q %d %s %.3f %.3f %.3f" % (layer, " ".join("%.3f %.3f %.3f 1.00 1.00" % p for p in P), *N[0]))
    return "\n".join(lines) + "\n"


class OrientationTest(unittest.TestCase):
    def test_a_head_on_minus_x_is_forward(self):
        m = wo.measure(axe(-14, 0), SC)
        self.assertLess(m.imbalance, -0.05)
        self.assertGreater(m.reach_neg, 0.25)
        self.assertEqual(wo.detect(m, "battleaxe"), [])

    def test_a_head_on_plus_x_faces_the_player(self):
        m = wo.measure(axe(0, 14), SC)
        self.assertGreater(m.imbalance, 0.05)
        self.assertEqual(wo.detect(m, "handaxe"), [wo.FLAG_HEAD_TOWARD_PLAYER])

    def test_a_double_bladed_head_has_no_side(self):
        m = wo.measure(axe(-12, 12), SC)
        self.assertAlmostEqual(m.imbalance, 0.0, delta=0.012)
        self.assertEqual(wo.detect(m, "greataxe"), [])

    def test_mirroring_fixes_the_flag_and_is_an_involution(self):
        q = axe(0, 14)
        m = wo.measure(q, SC)
        mq = wo.mirror_quads(q)
        self.assertEqual(wo.detect(wo.measure(mq, SC), "handaxe"), [])
        back = wo.mirror_quads(mq)
        for (l0, P0, N0), (l1, P1, N1) in zip(q, back):
            for a, b in zip(P0, P1):
                self.assertTrue(all(abs(x - y) < 1e-9 for x, y in zip(a, b)))

    def test_a_head_at_the_butt_is_tip_down_and_turning_fixes_it(self):
        q = box(-1, 1, 0, 60, -1, 1) + box(-20, 0, 2, 22, -2, 2)
        m = wo.measure(q, SC)
        self.assertLess(m.centroid, wo.HEAD_HEAVY_MAX)
        self.assertIn(wo.FLAG_TIP_DOWN, wo.detect(m, "battleaxe"))
        r = wo.measure(wo.rotate_quads_z(q, 180, (m.y0 + m.y1) / 2), SC)
        self.assertGreater(r.centroid, 0.6)
        self.assertNotIn(wo.FLAG_TIP_DOWN, wo.detect(r, "battleaxe"))

    def test_a_place_angle_pointing_down_is_tip_down_but_not_for_items(self):
        m = wo.measure(axe(-14, 0), SC)
        self.assertIn(wo.FLAG_TIP_DOWN, wo.detect(m, "longsword", -135))
        self.assertNotIn(wo.FLAG_TIP_DOWN, wo.detect(m, "item", -32))
        self.assertEqual(wo.detect(m, None), [])

    def test_a_curved_blade_bends_the_way_of_its_class(self):
        q = box(-1, 1, 0, 30, -0.5, 0.5) + box(4, 6, 30, 60, -0.5, 0.5)
        m = wo.measure(q, SC)
        self.assertGreater(m.curve, 0.05)
        self.assertIn(wo.FLAG_MIRRORED, wo.detect(m, "katana"))
        self.assertNotIn(wo.FLAG_MIRRORED, wo.detect(m, "sabre"))

    def test_the_real_sorrows_edge_of_the_generator_faces_the_player_and_the_fixed_one_does_not(self):
        pristine = aud.pristine_copy(os.path.join(tempfile.mkdtemp(), "m"))
        d = wo.read_mesh_file(os.path.join(pristine, "sorrows_edge.txt"))
        m = wo.measure(d["quads"], d["place"][3])
        self.assertIn(wo.FLAG_HEAD_TOWARD_PLAYER, wo.detect(m, "crescent"), "imbalance %.3f" % m.imbalance)
        fixed = wo.read_mesh_file(os.path.join(aud.MESHES, "sorrows_edge.txt"))
        mf = wo.measure(fixed["quads"], fixed["place"][3])
        self.assertNotIn(wo.FLAG_HEAD_TOWARD_PLAYER, wo.detect(mf, "crescent"), "imbalance %.3f" % mf.imbalance)
        self.assertLess(mf.imbalance, -0.05)


class FixToolTest(unittest.TestCase):
    def setUp(self):
        self.text = mesh_text(axe(0, 14))
        self.cls = wc.load()

    def apply(self, text, fix):
        return af.apply_to_text(text, "x_hand_axe", fix, self.cls)

    def test_flip_and_scale_and_head_are_applied_and_reported(self):
        new, rep = self.apply(self.text, {"flip_x": True, "apparent": 0.8, "head": {"k": 1.6}})
        self.assertTrue(rep["changed"])
        self.assertAlmostEqual(rep["after"], 0.8, places=2)
        d = wo.read_mesh_file_text(new) if hasattr(wo, "read_mesh_file_text") else None
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
            f.write(new)
        try:
            q = wo.read_mesh_file(f.name)
        finally:
            os.unlink(f.name)
        m = wo.measure(q["quads"], q["place"][3])
        self.assertLess(m.imbalance, -0.03, "the head is on the forward side now")
        self.assertGreater(m.max_width * 0.85, 0.30 * 0.9, "and bold")

    def test_the_same_request_twice_changes_nothing(self):
        fix = {"flip_x": True, "apparent": 0.8, "head": {"k": 1.4}}
        a, _ = self.apply(self.text, fix)
        b, rep = self.apply(a, fix)
        self.assertEqual(a, b)
        self.assertFalse(rep["changed"])

    def test_a_changed_request_undoes_the_old_one_first(self):
        a, _ = self.apply(self.text, {"flip_x": True, "apparent": 0.8})
        b, rep = self.apply(a, {"apparent": 1.0})
        self.assertAlmostEqual(rep["before"], 60 * SC * 0.85, delta=0.02, msg="the old fix was undone before the new one")
        c, _ = self.apply(self.text, {"apparent": 1.0})
        pb = [l for l in b.split("\n") if l.startswith("place")][0].split()
        pc = [l for l in c.split("\n") if l.startswith("place")][0].split()
        self.assertEqual(pb, pc, "undo + apply equals apply on the pristine mesh")
        self.assertEqual([l for l in b.split("\n") if l.startswith("q ")][:3], [l for l in c.split("\n") if l.startswith("q ")][:3])

    def test_an_empty_request_restores_the_mesh_the_generator_wrote(self):
        a, _ = self.apply(self.text, {"flip_x": True, "apparent": 1.2, "head": {"k": 1.7}, "rotate_z": 180})
        back, rep = self.apply(a, {})
        self.assertNotIn(af.MARK, back)
        qa = [l.split() for l in self.text.split("\n") if l.startswith("q ")]
        qb = [l.split() for l in back.split("\n") if l.startswith("q ")]
        self.assertEqual(len(qa), len(qb))
        for x, y in zip(qa, qb):
            for u, v in zip(x[2:22], y[2:22]):
                self.assertAlmostEqual(float(u), float(v), delta=0.01)
        self.assertEqual([l for l in back.split("\n") if l.startswith("place")], [l for l in self.text.split("\n") if l.startswith("place")])

    def test_scaling_keeps_the_grip_where_it_was(self):
        new, _ = self.apply(self.text, {"scale": 1.5})
        old_p = [float(t) for t in [l for l in self.text.split("\n") if l.startswith("place")][0].split()[1:6]]
        new_p = [float(t) for t in [l for l in new.split("\n") if l.startswith("place")][0].split()[1:6]]
        grip = wc.table(self.cls)["handaxe"]["grip"]
        a = math.radians(old_p[2])
        L = 60 * old_p[3]
        g0 = (old_p[0] + math.cos(a) * grip * L, old_p[1] + math.sin(a) * grip * L)
        g1 = (new_p[0] + math.cos(a) * grip * L * 1.5, new_p[1] + math.sin(a) * grip * L * 1.5)
        self.assertAlmostEqual(g0[0], g1[0], places=4)
        self.assertAlmostEqual(g0[1], g1[1], places=4)
        self.assertAlmostEqual(new_p[3], old_p[3] * 1.5, places=6)

    def test_orient_manual_is_written_and_removed(self):
        a, _ = self.apply(self.text, {"orient_manual": True})
        self.assertIn("orient manual", a.split("\n"))
        b, _ = self.apply(a, {})
        self.assertNotIn("orient manual", b.split("\n"))

    def test_the_head_operation_is_exactly_invertible(self):
        h = {"k": 1.8, "ky": 1.3, "kz": 1.2, "cut": 30.0, "px": 0.0, "band_mu": 4.0}
        _l, q = af.parse_lines(self.text)
        before = {i: list(t) for i, t in q.items()}
        af.op_head(q, h)
        self.assertNotEqual(before, q)
        af.op_head(q, h, inverse=True)
        for i, t in q.items():
            for u, v in zip(before[i][2:22], t[2:22]):
                self.assertAlmostEqual(float(u), float(v), delta=0.005)


class TableTest(unittest.TestCase):
    def test_the_game_copy_matches_the_tool_table(self):
        self.assertTrue(aud.asset_copy_matches(wc.load()), "run python tools/relics/audit_weapon_scale.py --write")

    def test_the_json_classes_equal_the_python_defaults(self):
        data = wc.load()
        for k, v in wc.CLASSES.items():
            c = data["classes"][k]
            self.assertEqual((c["min"], c["max"], c["head_min"], c["grip"]), tuple(v), k)

    def test_the_java_fallback_table_and_keywords_equal_the_python_ones(self):
        src = open(JAVA, encoding="utf-8").read()
        java = {m.group(1): (float(m.group(2)), float(m.group(3)), float(m.group(4)), float(m.group(5)))
                for m in re.finditer(r'c\("(\w+)", ([\d.]+)f, ([\d.]+)f, ([\d.]+)f, ([\d.]+)f\);', src)}
        self.assertEqual(set(java), set(wc.CLASSES))
        for k, v in wc.CLASSES.items():
            self.assertEqual(java[k], tuple(v), k)
        kws = re.findall(r'\{"(\w+)", "(\w+)"\}', src[src.index("KEYWORDS"):src.index("STATE_SUFFIX")])
        self.assertEqual(kws, wc.KEYWORDS)

    def test_the_keyword_guess(self):
        self.assertEqual(wc.guess_class("returning_hand_axe"), "handaxe")
        self.assertEqual(wc.guess_class("mountainguards_greataxe"), "greataxe")
        self.assertEqual(wc.guess_class("fortune_die"), "item")
        self.assertEqual(wc.base_id("bianca_pull2"), "bianca")
        self.assertEqual(wc.base_id("dragon_tooth_blade_red"), "dragon_tooth_blade_red")

    def test_every_mesh_has_a_reviewed_class(self):
        data = wc.load()
        import glob
        for p in glob.glob(os.path.join(aud.MESHES, "*.txt")):
            i = os.path.basename(p)[:-4]
            if aud.is_excluded(i, data):
                continue
            self.assertIn(wc.base_id(i), data["weapons"], i)


class ShippedMeshesTest(unittest.TestCase):
    """The state of the repository: the fixes leave nothing below its class and nothing facing the wrong way."""

    @classmethod
    def setUpClass(cls):
        cls.data = wc.load()
        cls.rows = aud.audit_all(cls.data)

    def test_no_weapon_is_below_its_class_minimum_or_has_a_small_head(self):
        bad = [(r["id"], r["status"]) for r in self.rows if "small" in r["status"] or "head_small" in r["status"]]
        self.assertEqual(bad, [])

    def test_no_actionable_orientation_flag_is_left(self):
        bad = [(r["id"], r["flags"]) for r in self.rows if set(r["flags"]) & set(wo.ACTIONABLE)]
        self.assertEqual(bad, [])

    def test_the_fixes_file_is_up_to_date_with_the_meshes(self):
        rows, changed = af.run(apply=False)
        self.assertEqual(changed, [], "run python tools/relics/apply_weapon_fixes.py --apply")

    def test_the_audit_sees_the_known_large_ones_and_does_not_shrink_them(self):
        large = {r["id"] for r in self.rows if "large" in r["status"]}
        self.assertIn("severing_greatsword", large)
        fixes = af.load_fixes()
        for i in large:
            self.assertNotIn("apparent", fixes.get(i, {}), i)


if __name__ == "__main__":
    unittest.main(verbosity=2)
