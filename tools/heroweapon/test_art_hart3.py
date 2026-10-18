"""test_art_hart3: the self-check of the art of the Vassal Carriage, Fan, Scythe, Katana and Book (python3 -B tools/heroweapon/test_art_hart3.py).

Why: nothing can be seen in the game while the art is made, so the things that would show up as a purple-black cube, a weapon held by its blade, a skin
that does not fit its mesh or a mesh over the owner's budget are checked from the written files: the six files of each weapon that HeroAssetsTest asks for
exist and are well formed, every quad of the mesh is finite with unit normals and UVs inside the 128 space, the budget of the method holds (8 to 14 named
parts, at most 2800 quads with the glow layer counted), the sprites are 16 x 16 and the skins 512 px, the item models name files that exist, and the
size and orientation audit of the oversized-weapon rule (tools/relics/audit_weapon_scale.py) finds nothing once the class fragment is merged in memory.
With HART3_DETERMINISM=1 the whole script is run twice in separate processes and every file it writes must be byte for byte the same ("idempotent").
"""
import hashlib
import json
import math
import os
import subprocess
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import audit_weapon_scale as aud           # noqa: E402
import weapon_classes as wc                # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
IDS = ["vassal_scythe", "vassal_katana", "vassal_fan", "vassal_carriage", "vassal_book"]
MAX_QUADS = 2800
PARTS = (8, 14)
FRAGMENT = os.path.join(HERE, "classes_hart3.json")


def files_of(item_id):
    return [os.path.join(ASSETS, p) for p in (
        "models/item/%s.json" % item_id, "models/item/%s_icon.json" % item_id, "textures/item/%s.png" % item_id,
        "weapon_meshes/%s.txt" % item_id, "textures/entity/weapon/%s.png" % item_id, "textures/entity/weapon/%s_glow.png" % item_id)]


def read_mesh(item_id):
    """(header lines, quads, parts) of a mesh file; a quad is (layer, kind, tokens), a part is (name, first quad line, quad count)."""
    heads, quads, parts = [], [], []
    with open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt"), encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v:
                continue
            if v[0] == "#":
                if len(v) >= 5 and v[1] == "part":
                    parts.append((v[2], int(v[3]), int(v[4])))
                continue
            if v[0] in ("q", "qn"):
                quads.append((int(v[1]), v[0], [float(x) for x in v[2:]]))
            else:
                heads.append(v)
    return heads, quads, parts


class FilesTest(unittest.TestCase):
    def test_the_six_files_of_every_weapon_exist(self):
        missing = [os.path.relpath(p, ASSETS) for i in IDS for p in files_of(i) if not os.path.isfile(p)]
        self.assertEqual(missing, [])

    def test_the_sprites_are_16_by_16_and_the_skins_512(self):
        from PIL import Image
        for i in IDS:
            with Image.open(os.path.join(ASSETS, "textures", "item", i + ".png")) as sp:
                self.assertEqual(sp.size, (16, 16), i)
                opaque = sum(1 for p in sp.convert("RGBA").getdata() if p[3] > 40)
            self.assertGreater(opaque, 50, i + " sprite is nearly empty")
            for suffix in ("", "_glow"):
                with Image.open(os.path.join(ASSETS, "textures", "entity", "weapon", i + suffix + ".png")) as sk:
                    self.assertEqual(sk.size, (512, 512), i + suffix)
                    self.assertEqual(sk.mode, "RGBA", i + suffix)
            with Image.open(os.path.join(ASSETS, "textures", "entity", "weapon", i + "_glow.png")) as g:
                glow = g.convert("RGBA")
            self.assertGreater(glow.getchannel("A").getextrema()[1], 100, i + " has no glow")

    def test_the_item_models_name_files_that_exist(self):
        for i in IDS:
            with open(os.path.join(ASSETS, "models", "item", i + ".json"), encoding="utf-8") as f:
                model = json.load(f)
            self.assertEqual(model["parent"], "minecraft:builtin/entity", i)
            self.assertEqual(model["textures"]["particle"], "nusmp:item/" + i)
            for k in ("thirdperson_righthand", "firstperson_righthand", "ground", "head", "fixed", "gui"):
                self.assertIn(k, model["display"], i + " " + k)
                d = model["display"][k]
                self.assertEqual(len(d["rotation"]), 3)
                self.assertEqual(len(d["translation"]), 3)
                self.assertTrue(all(s > 0 for s in d["scale"]), i + " " + k)
            with open(os.path.join(ASSETS, "models", "item", i + "_icon.json"), encoding="utf-8") as f:
                icon = json.load(f)
            self.assertIn(icon["parent"], ("minecraft:item/handheld", "minecraft:item/generated"))
            self.assertEqual(icon["textures"]["layer0"], "nusmp:item/" + i)


class MeshTest(unittest.TestCase):
    def test_every_quad_is_well_formed(self):
        for i in IDS:
            heads, quads, parts = read_mesh(i)
            self.assertTrue(any(h[0] == "place" and len(h) == 6 for h in heads), i + " has no place line")
            self.assertTrue(quads, i)
            for layer, kind, t in quads:
                self.assertIn(layer, (0, 1, 2), i)
                self.assertEqual(kind, "qn", i + ": the art writes smoothed per-vertex normals")
                self.assertEqual(len(t), 32, i)
                for k in range(4):
                    x, y, z, u, v, nx, ny, nz = t[k * 8:k * 8 + 8]
                    self.assertTrue(all(math.isfinite(c) for c in (x, y, z, u, v, nx, ny, nz)), i)
                    self.assertAlmostEqual(math.sqrt(nx * nx + ny * ny + nz * nz), 1.0, delta=0.02, msg=i + " normal")
                    self.assertTrue(-0.01 <= u <= 128.01 and -0.01 <= v <= 128.01, "%s: uv %.2f %.2f outside the 128 space" % (i, u, v))

    def test_the_budget_of_the_method_holds(self):
        for i in IDS:
            heads, quads, parts = read_mesh(i)
            names = sorted({p[0] for p in parts})
            self.assertTrue(PARTS[0] <= len(names) <= PARTS[1], "%s has %d named parts: %s" % (i, len(names), names))
            self.assertLessEqual(len(quads), MAX_QUADS, "%s has %d quads with the glow layer counted" % (i, len(quads)))

    def test_a_glow_header_is_there_when_a_glow_layer_is(self):
        for i in IDS:
            heads, quads, parts = read_mesh(i)
            has_glow_quads = any(q[0] == 2 for q in quads)
            self.assertEqual(has_glow_quads, any(h[0] == "glow" for h in heads), i)

    def test_the_hand_end_is_the_low_end(self):
        for i in IDS:
            heads, quads, parts = read_mesh(i)
            for h in heads:
                if h[0] == "grip":
                    self.assertLessEqual(float(h[1]), 0.5, i + " would be turned over by the runtime normaliser")

    def test_the_carriage_has_the_parts_the_entity_needs(self):
        """docs/heroweapon/weapon_carriage.md of the logic builder: the named parts, each wheel on its own so a renderer can turn it."""
        heads, quads, parts = read_mesh("vassal_carriage")
        names = {p[0] for p in parts}
        self.assertEqual(names, {"wheel_left", "wheel_right", "axle", "chassis", "cabin", "canopy", "posts", "bench_front", "bench_rear", "pole", "yoke", "lantern", "gem", "plumes"})

    def test_the_carriage_meets_the_entity_proportions(self):
        """The entity renderer maps mesh -x to up, +y to the nose and z to the width, puts the wheels' lowest point (x = 0) on the ground and scales by 3.2 / (ymax - ymin);
        the logic page gives the proportions as shares of that length L and the seat points the riders sit on."""
        import numpy as np
        heads, quads, parts = read_mesh("vassal_carriage")
        P = {}
        for name, first, count in parts:
            P[name] = np.array([[t[k * 8 + j] for j in range(3)] for (_l, _k, t) in quads[first:first + count] for k in range(4)])
        allp = np.concatenate([v for v in P.values()])
        lo, hi = allp.min(axis=0), allp.max(axis=0)
        length = hi[1] - lo[1]
        self.assertAlmostEqual(hi[0], 0.0, delta=0.05, msg="the wheels touch the ground at x = 0")
        self.assertAlmostEqual((hi[2] - lo[2]) / length, 0.60, delta=0.01, msg="width over the wheels")
        self.assertAlmostEqual(-P["canopy"][:, 0].min() / length, 0.58, delta=0.01, msg="height to the top of the canopy")
        wl = P["wheel_left"]
        self.assertAlmostEqual((wl[:, 0].max() - wl[:, 0].min()) / length, 0.34, delta=0.01, msg="wheel diameter")
        body = P["chassis"]
        self.assertAlmostEqual((body[:, 1].max() - body[:, 1].min()) / length, 0.70, delta=0.01, msg="body length")
        self.assertAlmostEqual((hi[1] - body[:, 1].max()) / length, 0.30, delta=0.012, msg="pole and yoke ahead of the body")
        mid = (body[:, 1].max() + body[:, 1].min()) / 2
        seat = 0.17 * length
        fb, rb = P["bench_front"], P["bench_rear"]
        for b, nm, off in ((fb, "front", 0.17), (rb, "rear", -0.19)):
            top = b[np.abs(-b[:, 0] - seat) < 0.4]
            self.assertGreater(len(top), 4, nm + " bench: a seat surface 0.17 L above the ground")
            self.assertAlmostEqual(top[:, 1].mean(), mid + off * length, delta=0.012 * length, msg=nm + " bench: the seat is where the rider sits")
        self.assertLess(abs(P["wheel_left"][:, 2].mean() + P["wheel_right"][:, 2].mean()), 0.01, "the carriage is symmetric about z = 0")
        self.assertLess(P["wheel_left"][:, 2].max(), 0.0)
        self.assertGreater(P["wheel_right"][:, 2].min(), 0.0)
        axle_line = (P["wheel_left"][:, 0].min() + P["wheel_left"][:, 0].max()) / 2
        self.assertAlmostEqual(axle_line, -0.17 * length, delta=0.05, msg="the axle line is one wheel radius above the ground")

    def test_no_model_names_in_the_files_of_this_job(self):
        """The owner's repo rule: no AI or model names in any file."""
        words = ("claude", "anthropic", "gpt", "sonnet", "opus", "haiku", "openai", "gemini", "llama", "copilot", "chatgpt")
        files = [os.path.join(HERE, n) for n in ("art_hart3.py", "art_hart3_lib.py", "art_hart3_sprites.py", "classes_hart3.json")]       # (this file lists the words, so it is not scanned)
        files += [os.path.join(ROOT, "docs", "heroweapon", "art_hart3.md")]
        for i in IDS:
            files.append(os.path.join(ASSETS, "weapon_meshes", i + ".txt"))
            files += [os.path.join(ASSETS, "models", "item", i + sfx + ".json") for sfx in ("", "_icon")]
        for path in files:
            if not os.path.exists(path):
                continue
            with open(path, encoding="utf-8", errors="replace") as f:
                text = f.read().lower()
            hits = [wd for wd in words if wd in text]
            self.assertEqual(hits, [], "%s mentions %s" % (os.path.relpath(path, ROOT), hits))


class AuditTest(unittest.TestCase):
    def test_the_class_fragment_is_valid(self):
        with open(FRAGMENT, encoding="utf-8") as f:
            frag = json.load(f)["weapons"]
        self.assertEqual(sorted(frag), sorted(IDS))
        table = wc.table(wc.load())
        for i, v in frag.items():
            self.assertIn(v["class"], table, i)

    def test_size_and_orientation_pass_once_the_fragment_is_merged(self):
        data = wc.load()
        with open(FRAGMENT, encoding="utf-8") as f:
            frag = json.load(f)["weapons"]
        for k, v in frag.items():
            data["weapons"][k] = {"class": v["class"]}
        for i in IDS:
            row = aud.audit_one(i, data)
            self.assertIsNotNone(row, i)
            self.assertEqual(row["status"], [], "%s: %s" % (i, row))
            self.assertEqual(row["flags"], [], "%s: %s" % (i, row))


class DeterminismTest(unittest.TestCase):
    @unittest.skipUnless(os.environ.get("HART3_DETERMINISM") == "1", "set HART3_DETERMINISM=1 to run the script twice (about a minute)")
    def test_a_second_run_writes_the_same_bytes(self):
        def snapshot():
            out = {}
            for i in IDS:
                for p in files_of(i):
                    with open(p, "rb") as f:
                        out[os.path.relpath(p, ROOT)] = hashlib.sha256(f.read()).hexdigest()
            with open(FRAGMENT, "rb") as f:
                out["classes"] = hashlib.sha256(f.read()).hexdigest()
            return out
        script = os.path.join(HERE, "art_hart3.py")
        subprocess.run([sys.executable, "-B", script], check=True, cwd=ROOT, stdout=subprocess.DEVNULL)
        first = snapshot()
        subprocess.run([sys.executable, "-B", script], check=True, cwd=ROOT, stdout=subprocess.DEVNULL)
        second = snapshot()
        self.assertEqual([k for k in first if first[k] != second[k]], [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
