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
            if aud.is_excluded(i, data) or i.startswith(aud.SKIP_PREFIXES):
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

    def test_the_audit_never_shrinks_the_big_ones(self):
        """1.34.0: with the larger table nothing is `large` any more; the biggest of the boss weapons stay as they are (no `apparent` / `scale` entry)."""
        fixes = af.load_fixes()
        by = {r["id"]: r for r in self.rows}
        for i in ("severing_greatsword", "demon_destroyer_sword", "licht_destroyer_sword", "demon_dweller_sword", "licht_dweller_sword"):
            self.assertGreaterEqual(by[i]["apparent"], 2.4, i)
            self.assertNotIn("apparent", fixes.get(i, {}), i)
            self.assertNotIn("scale", fixes.get(i, {}), i)
        for i in {r["id"] for r in self.rows if "large" in r["status"]}:
            self.assertNotIn("apparent", fixes.get(i, {}), i)

    # ---------------------------------------------------------------- 1.34.0: the size rule
    def test_every_class_reaches_the_owners_ranges(self):
        t = wc.table(self.data)
        rng = {"dagger": (0.45, 0.70), "shortsword": (1.0, 1.3), "rapier": (1.0, 1.3), "sabre": (1.0, 1.3), "longsword": (1.4, 1.8), "katana": (1.4, 1.8),
               "greatsword": (2.2, 3.0), "handaxe": (1.1, 1.4), "battleaxe": (1.6, 2.0), "greataxe": (2.4, 3.0), "hammer": (1.3, 1.8), "mace": (1.3, 1.8),
               "greathammer": (2.2, 2.8), "spear": (2.8, 3.6), "glaive": (2.8, 3.6), "staff": (2.0, 2.6), "whip": (1.6, 2.4), "crossbow": (1.0, 1.2),
               "bow": (1.3, 1.6), "shield": (0.9, 1.2), "handitem": (0.5, 0.9)}
        for k, (lo, hi) in rng.items():
            self.assertEqual((t[k]["min"], t[k]["max"]), (lo, hi), k)
        self.assertEqual((t["handaxe"]["head_min"], t["battleaxe"]["head_min"], t["greataxe"]["head_min"]), (0.5, 0.75, 1.1))

    def test_no_weapon_is_below_the_new_class_minimum(self):
        for r in self.rows:
            if wc.is_item_class(r["class"]):
                self.assertGreaterEqual(r["extent"], 0.5 * 0.97, r["id"])
            else:
                self.assertGreaterEqual(r["apparent"], r["min"] * 0.97, r["id"])
            if r["head_min"]:
                self.assertGreaterEqual(r["head"], r["head_min"] * 0.97, r["id"])

    def test_great_things_are_massive_and_daggers_stay_small(self):
        by = {r["id"]: r for r in self.rows}
        for i, r in by.items():
            if r["class"] == "greatsword":
                self.assertGreaterEqual(r["apparent"], 2.2, i)
        for i in ("the_stabshroom", "unicorn_dagger", "dagger_of_betrayal"):
            self.assertLessEqual(by[i]["apparent"], 0.70, i)
        self.assertGreaterEqual(by["magic_tool_spear"]["apparent"], 2.8)
        self.assertGreaterEqual(by["elsdocia"]["apparent"], 2.2, "Elsdocia is a greatsword-class blade now")

    def test_every_state_of_one_weapon_has_the_same_scale(self):
        """A bow / crossbow / pair half in several states grows by one factor (a `scale` entry), so the draw stages do not jump in size."""
        import glob
        data = self.data
        groups = {}
        for p in glob.glob(os.path.join(aud.MESHES, "*.txt")):
            i = os.path.basename(p)[:-4]
            if i.startswith("djinn_weapon_"):
                continue
            groups.setdefault(wc.base_id(i), []).append(i)
        for base, ids in groups.items():
            if len(ids) < 2:
                continue
            fx = af.load_fixes().get(base, {})
            self.assertNotIn("apparent", fx, base + ": several meshes follow one entry, use `scale`")
            self.assertNotIn("extent", fx, base)

    # ---------------------------------------------------------------- 1.34.0: the grip end
    def test_every_mesh_is_held_by_its_hilt(self):
        bad = [(r["id"], r["grip_at"]) for r in self.rows if r["grip_at"] != "low" or wo.FLAG_GRIP_FAR_END in r["flags"]]
        self.assertEqual(bad, [])

    def test_the_four_reversed_swords_are_found_in_what_the_generators_wrote(self):
        """The Windcutter (the owner's screenshot 68), Tripolar Sword, Flamecutter and Encircled Blade were built point first."""
        pristine = aud.pristine_copy(os.path.join(tempfile.mkdtemp(), "m"))
        rows = {r["id"]: r for r in aud.audit_all(self.data, pristine)}
        far = sorted(i for i, r in rows.items() if wo.FLAG_GRIP_FAR_END in r["flags"])
        self.assertEqual(far, ["encircled_blade", "flamecutter", "tripolar_sword", "windcutter"])
        fixes = af.load_fixes()
        for i in far:
            self.assertTrue(fixes[i].get("flip_y"), i)
            text = open(os.path.join(aud.MESHES, i + ".txt"), encoding="utf-8").read()
            self.assertIn("\ngrip 0.120\n", text, i)

    def test_flipping_along_the_length_keeps_the_edge_side_and_is_an_involution(self):
        q = box(-1, 1, 0, 60, -1, 1) + box(-14, 0, 40, 58, -0.5, 0.5)            # a haft with a head on -x, high up
        p = (0 + 58) / 2.0
        f = wo.flip_quads_y(q, p)
        m0, m1 = wo.measure(q, SC), wo.measure(f, SC)
        for (_l, P0, _n0), (_l1, P1, _n1) in zip(q, f):
            self.assertEqual([p[0] for p in P0], [p[0] for p in P1], "no x moves: the edge keeps its side")
        back = wo.flip_quads_y(f, p)
        for (l0, P0, N0), (l1, P1, N1) in zip(q, back):
            for a, b in zip(P0, P1):
                self.assertTrue(all(abs(x - y) < 1e-9 for x, y in zip(a, b)))
        for (l0, P0, N0), (l1, P1, N1) in zip(f, wo.flip_quads_y(q, p)):
            self.assertEqual(P0, P1)

    def test_a_reversed_synthetic_sword_is_flagged_and_the_flip_fixes_it(self):
        # a blade 0..44 tapering to a point at y = 0 (wide at 30), the guard at 44 (wide), a thin grip 46..58, a pommel at 60: point first = reversed
        blade = box(-3, 3, 4, 44, -0.4, 0.4) + box(-0.5, 0.5, 0, 4, -0.2, 0.2)
        guard = box(-9, 9, 40, 52, -3, 3)                                      # an ornate guard: the heavy part of a reversed sword is its guard
        grip = box(-0.8, 0.8, 52, 58, -0.8, 0.8) + box(-1.2, 1.2, 58, 60, -1.2, 1.2)
        q = blade + guard + grip
        m = wo.measure(q, 0.025)
        self.assertIn(wo.FLAG_GRIP_FAR_END, wo.detect(m, "longsword"))
        f = wo.measure(wo.flip_quads_y(q, 30.0), 0.025)
        self.assertNotIn(wo.FLAG_GRIP_FAR_END, wo.detect(f, "longsword"))
        # the same mesh with the right way up is untouched, and an axe (not a pointed class) is never judged by this rule
        self.assertNotIn(wo.FLAG_GRIP_FAR_END, wo.detect(wo.measure(axe(-14, 0), SC), "battleaxe"))

    def test_the_grip_directive_beats_the_geometry(self):
        blade = box(-3, 3, 4, 44, -0.4, 0.4) + box(-0.5, 0.5, 0, 4, -0.2, 0.2)
        q = blade + box(-9, 9, 40, 52, -3, 3) + box(-0.8, 0.8, 52, 60, -0.8, 0.8)
        m = wo.measure(q, 0.025)
        self.assertIn(wo.FLAG_GRIP_FAR_END, wo.detect(m, "longsword"), "without the directive the geometry flags it")
        self.assertNotIn(wo.FLAG_GRIP_FAR_END, wo.detect(m, "longsword", 45.0, 0.12), "a stated low grip is trusted")
        self.assertIn(wo.FLAG_GRIP_FAR_END, wo.detect(wo.measure(axe(-14, 0), SC), "item", 45.0, 0.9), "a stated high grip is acted on for any class")

    def test_the_part_names_give_the_grip_fraction(self):
        import mesh_lib as ml
        up = [("pommel", 2.0, 10), ("grip", 8.0, 10), ("guard", 20.0, 6), ("blade", 60.0, 40), ("tip", 95.0, 4)]
        down = [("pommel", 98.0, 10), ("grip", 90.0, 10), ("guard", 80.0, 6), ("blade", 40.0, 40), ("tip", 2.0, 4)]
        self.assertLess(ml.Mesh.grip_fraction(up), 0.2)
        self.assertGreater(ml.Mesh.grip_fraction(down), 0.8)
        self.assertIsNone(ml.Mesh.grip_fraction([("fur", 1.0, 3), ("cloth", 9.0, 4)]), "no grip / blade names: undecided")
        self.assertIsNone(ml.Mesh.grip_fraction([("grip", 5.0, 2), ("blade", 5.2, 2), ("pommel", 4.9, 1), ("fur", 0.0, 1), ("cloth", 20.0, 1)]), "grip and blade at the same height")

    def test_the_fix_tool_writes_and_restores_the_grip_line(self):
        txt = mesh_text(box(-3, 3, 4, 44, -0.4, 0.4) + box(-0.5, 0.5, 0, 4, -0.2, 0.2) + box(-9, 9, 44, 46, -1, 1) + box(-0.8, 0.8, 46, 60, -0.8, 0.8))
        txt = txt.replace("place", "grip 0.870\nplace", 1) if False else txt.replace("\nq ", "\ngrip 0.870\nq ", 1)
        new, rep = af.apply_to_text(txt, "x_longsword", {"flip_y": True}, wc.load())
        self.assertIn("grip 0.120", new.split("\n"))
        self.assertNotIn("grip 0.870", new.split("\n"), "the generator's own line is replaced, not repeated")
        again, rep2 = af.apply_to_text(new, "x_longsword", {"flip_y": True}, wc.load())
        self.assertEqual(new, again)
        back, _ = af.apply_to_text(new, "x_longsword", {}, wc.load())
        self.assertIn("grip 0.870", back.split("\n"))
        self.assertNotIn("grip 0.120", back.split("\n"))
        self.assertEqual(sorted(l for l in back.split("\n") if l.startswith("q ")), sorted(l for l in txt.split("\n") if l.startswith("q ")))


if __name__ == "__main__":
    unittest.main(verbosity=2)
