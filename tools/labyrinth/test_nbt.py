#!/usr/bin/env python3
"""Tests of the NBT writer/reader and the template DSL (pure Python, no Minecraft): python3 tools/labyrinth/test_nbt.py"""
import os
import struct
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402
import template  # noqa: E402


class NbtRoundTrip(unittest.TestCase):
    def every_tag(self):
        return nbt.Compound({
            "end_list": nbt.List([]),
            "byte": nbt.Byte(-7), "byte_max": nbt.Byte(127), "bool": True,
            "short": nbt.Short(-30000), "int": nbt.Int(2 ** 31 - 1), "plain_int": 42, "long": nbt.Long(-(2 ** 63)),
            "float": nbt.Float(1.5), "double": nbt.Double(-2.25), "plain_float": 0.125,
            "bytes": nbt.ByteArray(b"\x00\x01\xff\x7f"), "plain_bytes": b"ab",
            "string": "nusmp:labyrinth/base", "unicode": "ümlaut é 中 \U0001F600 nul\x00end",
            "ints": nbt.IntArray([1, -2, 3]), "longs": nbt.LongArray([2 ** 40, -1]),
            "list_int": nbt.List([nbt.Int(1), nbt.Int(2)]), "list_str": ["a", "b"],
            "list_compound": nbt.List([nbt.Compound({"a": nbt.Int(1)}), nbt.Compound({"b": "x"})]),
            "typed_empty": nbt.List([], tag_type=nbt.TAG_COMPOUND),
            "nested": nbt.Compound({"inner": nbt.Compound({"deep": nbt.List([nbt.List([nbt.Byte(1)])])})}),
        })

    def test_round_trip_every_tag(self):
        data = self.every_tag()
        raw = nbt.dumps(data)
        name, back = nbt.loads(raw)
        self.assertEqual(name, "")
        self.assertEqual(nbt.dumps(back), raw)
        self.assertIsInstance(back["byte"], nbt.Byte)
        self.assertEqual(back["byte"], -7)
        self.assertIsInstance(back["bool"], nbt.Byte)
        self.assertEqual(back["bool"], 1)
        self.assertIsInstance(back["short"], nbt.Short)
        self.assertIsInstance(back["int"], nbt.Int)
        self.assertIsInstance(back["plain_int"], nbt.Int)
        self.assertIsInstance(back["long"], nbt.Long)
        self.assertEqual(back["long"], -(2 ** 63))
        self.assertIsInstance(back["float"], nbt.Float)
        self.assertIsInstance(back["double"], nbt.Double)
        self.assertIsInstance(back["plain_float"], nbt.Double)
        self.assertEqual(back["bytes"], b"\x00\x01\xff\x7f")
        self.assertEqual(back["unicode"], data["unicode"])
        self.assertEqual(list(back["ints"]), [1, -2, 3])
        self.assertEqual(list(back["longs"]), [2 ** 40, -1])
        self.assertEqual(back["end_list"].tag_type, nbt.TAG_END)
        self.assertEqual(back["typed_empty"].tag_type, nbt.TAG_COMPOUND)
        self.assertEqual(back["list_compound"][1]["b"], "x")
        self.assertEqual(back["nested"]["inner"]["deep"][0][0], 1)

    def test_gzip_and_file(self):
        data = self.every_tag()
        gz = nbt.dumps(data, compress=True)
        self.assertEqual(gz[:2], b"\x1f\x8b")
        self.assertEqual(nbt.loads(gz)[1], data)
        with tempfile.TemporaryDirectory() as d:
            p = os.path.join(d, "x.nbt")
            nbt.dump(p, data, name="root")
            name, back = nbt.load(p)
            self.assertEqual(name, "root")
            self.assertEqual(nbt.dumps(back), nbt.dumps(data))
            nbt.dump(p, data, compress=False)
            self.assertEqual(nbt.load(p)[1], data)

    def test_byte_layout_matches_the_java_format(self):
        raw = nbt.dumps(nbt.Compound({"a": nbt.Short(1), "s": "hi"}), name="n")
        expect = b"\x0a" + b"\x00\x01n" + b"\x02" + b"\x00\x01a" + b"\x00\x01" + b"\x08" + b"\x00\x01s" + b"\x00\x02hi" + b"\x00"
        self.assertEqual(raw, expect)

    def test_mixed_list_rejected(self):
        with self.assertRaises(ValueError):
            nbt.dumps(nbt.Compound({"l": [nbt.Int(1), "x"]}))

    def test_wraps_like_java_casts(self):
        _, back = nbt.loads(nbt.dumps(nbt.Compound({"b": nbt.Byte(255), "i": nbt.Int(2 ** 32 + 5)})))
        self.assertEqual(back["b"], -1)
        self.assertEqual(back["i"], 5)

    def test_truncated_and_trailing(self):
        raw = nbt.dumps(self.every_tag())
        with self.assertRaises(ValueError):
            nbt.loads(raw[:-3])
        with self.assertRaises(ValueError):
            nbt.loads(raw + b"\x00")

    def test_modified_utf8_nul(self):
        raw = nbt.dumps(nbt.Compound({"s": "a\x00b"}))
        self.assertIn(b"\xc0\x80", raw)
        self.assertNotIn(b"a\x00b", raw)


class TemplateTest(unittest.TestCase):
    def build(self):
        t = template.Template("test/piece", 5, 4, 5)
        t.fill((0, 0, 0), (4, 0, 4), "nusmp:labyrinth_tiles")
        t.hollow_box((0, 1, 0), (4, 3, 4), "nusmp:labyrinth_bricks")
        t.set(2, 1, 2, "nusmp:labyrinth_pillar", {"axis": "y"})
        t.set(2, 2, 2, "labyrinth_glyph")  # namespace defaults to minecraft like a command would
        t.chest(1, 1, 1, "nusmp:chests/labyrinth_trial", facing="east")
        t.jigsaw(2, 3, 2, "up_north", "nusmp:up", "nusmp:down", "nusmp:labyrinth/ascent_1", joint="rollable", selection_priority=2)
        t.jigsaw(4, 1, 2, "east_up", "nusmp:side_out", "nusmp:side_in", "nusmp:labyrinth/side",
                 final_state="nusmp:labyrinth_bricks", placement_priority=1)
        t.be(3, 1, 3, "nusmp:sealed_archway", {"id": "nusmp:sealed_archway", "radius": nbt.Int(9),
                                                 "mobs": [nbt.Compound({"id": "minecraft:zombie", "count": nbt.Int(3)})]})
        return t

    def test_nbt_shape(self):
        t = self.build()
        data = t.to_nbt()
        self.assertEqual(data["DataVersion"], 3955)
        self.assertEqual(list(data["size"]), [5, 4, 5])
        names = [p["Name"] for p in data["palette"]]
        self.assertEqual(len(names), len(set(tuple(sorted(dict(p.get("Properties", {})).items())) + (p["Name"],) for p in data["palette"])))
        self.assertIn("minecraft:jigsaw", names)
        self.assertEqual(names.count("minecraft:jigsaw"), 2)  # two orientations = two palette states
        self.assertIn("minecraft:labyrinth_glyph", names)
        self.assertEqual(len(data["blocks"]), len(t.cells))
        self.assertEqual(data["entities"].tag_type, nbt.TAG_COMPOUND)
        chest = [b for b in data["blocks"] if "nbt" in b and b["nbt"]["id"] == "minecraft:chest"][0]
        self.assertEqual(chest["nbt"]["LootTable"], "nusmp:chests/labyrinth_trial")
        self.assertIsInstance(chest["nbt"]["LootTableSeed"], nbt.Long)
        self.assertEqual(list(chest["pos"]), [1, 1, 1])
        jig = [b for b in data["blocks"] if "nbt" in b and b["nbt"]["id"] == "minecraft:jigsaw" and list(b["pos"]) == [2, 3, 2]][0]
        self.assertEqual(data["palette"][jig["state"]]["Properties"]["orientation"], "up_north")
        self.assertEqual(jig["nbt"]["joint"], "rollable")
        self.assertEqual(jig["nbt"]["selection_priority"], 2)
        self.assertEqual(jig["nbt"]["final_state"], "minecraft:air")
        # unset cells are omitted: the inside of the hollow box minus what we placed
        self.assertNotIn((1, 2, 1), t.cells)

    def test_save_load_round_trip_and_check(self):
        t = self.build()
        with tempfile.TemporaryDirectory() as d:
            p = t.save(d)
            self.assertEqual(p, os.path.join(d, "data", "nusmp", "structure", "test", "piece.nbt"))
            back = template.load(p)
            self.assertEqual(back.name, "test/piece")
            self.assertEqual(back, t)
            self.assertEqual(nbt.dumps(back.to_nbt()), nbt.dumps(t.to_nbt()))
            self.assertEqual(template.validate(back), [])
            self.assertTrue(template.check(d, verbose=False))
            # a second save is byte-identical (deterministic, reviewable in a diff)
            first = open(p, "rb").read()
            t.save(d)
            self.assertEqual(open(p, "rb").read(), first)

    def test_validation(self):
        t = template.Template("t", 3, 3, 3)
        with self.assertRaises(ValueError):
            t.set(3, 0, 0, "minecraft:stone")
        with self.assertRaises(ValueError):
            t.jigsaw(1, 1, 1, "sideways", "nusmp:a", "nusmp:b", "nusmp:p")
        with self.assertRaises(ValueError):
            t.jigsaw(1, 1, 1, "north_up", "nusmp:a", "b", "nusmp:p")
        with self.assertRaises(ValueError):
            t.jigsaw(1, 1, 1, "north_up", "nusmp:a", "nusmp:b", "nusmp:p", joint="loose")
        with self.assertRaises(ValueError):
            t.jigsaw(1, 1, 1, "north_up", "nusmp:a", "nusmp:b", "nusmp:p", final_state="air block")
        with self.assertRaises(ValueError):
            t.chest(0, 0, 0, "chests/x")
        t.jigsaw(1, 1, 1, "north_up", "nusmp:a", "nusmp:b", "nusmp:p")
        t.set(1, 1, 0, "minecraft:stone")  # the cell the jigsaw faces into
        self.assertTrue(any("faces" in m for m in template.validate(t)))
        self.assertEqual(template.facing("north_up"), (0, 0, -1))
        self.assertEqual(template.facing("up_east"), (0, 1, 0))
        self.assertEqual(template.facing("down_west"), (0, -1, 0))

    def test_ascii_preview(self):
        text = self.build().ascii_layers()
        self.assertIn("y=0", text)
        self.assertIn("J", text)
        self.assertIn("C", text)
        self.assertIn("legend:", text)
        self.assertIn("pool=nusmp:labyrinth/side", text)


if __name__ == "__main__":
    unittest.main()
