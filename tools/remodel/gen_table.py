"""Why: the renderers of the remodelled mobs (src/main/java/com/newuniverse/nusmp/client/remodel) need the numbers of every model (its height, the ground one cycle of its walk and run covers, the
shadow, the box that keeps it from popping at the screen edge): the art kit measures them and writes them to tools/arc/manifest.json ("arc_look"). This script copies them into one generated Java
file, RemodelLooks.java, so the game never reads a JSON file for them and a model rebuilt with another height changes the Java in the same commit. RemodelLooksTest compares the file with the
manifest, so a model rebuilt without running this script fails the tests.

    python3 -B tools/remodel/gen_table.py            # rewrites src/main/java/com/newuniverse/nusmp/client/remodel/RemodelLooks.java
    python3 -B tools/remodel/gen_table.py --check    # fails when the file differs from what the manifest says (what the test does too)

Only the models of the remodelled characters (those whose tools/arc/char_<id>.py declares ENTITIES) are listed, with their second forms.
"""
import json
import os
import re
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(REPO, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "remodel", "RemodelLooks.java")
MANIFEST = os.path.join(REPO, "tools", "arc", "manifest.json")
ARC = os.path.join(REPO, "tools", "arc")


def remodelled_ids():
    """The ids of every character file that declares ENTITIES (read as text: the module is not imported, so this needs no kit)."""
    ids = []
    for fn in sorted(os.listdir(ARC)):
        if fn.startswith("char_") and fn.endswith(".py"):
            text = open(os.path.join(ARC, fn)).read()
            if re.search(r"^ENTITIES\s*=", text, re.M):
                ids.append(fn[len("char_"):-3])
    return ids


def render():
    models = json.load(open(MANIFEST))["models"]
    rows = []
    for cid in remodelled_ids():
        for mid in (cid, cid + "_form2"):
            m = models.get(mid)
            if not m:
                continue
            a = m["arc_look"]
            rows.append('            Map.entry("%s", new Look(%sf, %sf, %sf, %sf, %sf, %sf))' % (mid, a["height"], a["walkCycle"], a["runCycle"], a["shadow"], a["growXz"], a["growUp"]))
    body = ",\n".join(rows)
    return """package com.newuniverse.nusmp.client.remodel;

import java.util.Map;

/**
 * Why: the numbers of every remodelled model, copied from tools/arc/manifest.json ("arc_look") by tools/remodel/gen_table.py so the renderers need no file to read: the height the model was
 * built to, the blocks of ground one cycle of its walk and its run covers (the feet must not slide), the shadow, and the box that keeps a wing or a horn from popping at the screen edge.
 * GENERATED: run python3 -B tools/remodel/gen_table.py after rebuilding a character, never edit by hand (RemodelLooksTest fails when it drifts from the manifest).
 */
final class RemodelLooks {
    private RemodelLooks() {}

    /** The facts of one model (blocks). */
    record Look(float height, float walkCycle, float runCycle, float shadow, float growXz, float growUp) {}

    /** Used when a model is not listed (never for a remodelled one: the test checks them all). */
    static final Look FALLBACK = new Look(2.0f, 1.4f, 2.4f, 0.45f, 1.2f, 1.0f);

    static final Map<String, Look> BY_MODEL = Map.ofEntries(
""" + body + """
    );

    /** The facts of a model name such as {@code dante_zogratis} or {@code dante_zogratis_form2}. */
    static Look of(String model) {
        Look l = BY_MODEL.get(model);
        return l == null ? FALLBACK : l;
    }
}
"""


def main(argv):
    text = render()
    if "--check" in argv:
        have = open(OUT).read() if os.path.isfile(OUT) else ""
        if have != text:
            print("DIFFERS", os.path.relpath(OUT, REPO), "(run python3 -B tools/remodel/gen_table.py)")
            return 1
        print("RemodelLooks.java matches the manifest")
        return 0
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        f.write(text)
    print("wrote", os.path.relpath(OUT, REPO), "-", text.count("Map.entry("), "models")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
