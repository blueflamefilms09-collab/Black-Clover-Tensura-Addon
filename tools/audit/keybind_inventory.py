#!/usr/bin/env python3
"""Lists every KeyMapping the mod declares (file:line, field, translation key, default key) and the default-key conflicts.
Run from the repo root: python tools/audit/keybind_inventory.py
Read-only. Source of docs/keybind_consolidation.md section 2. Static text scan: a key built another way (a loop, a factory) would not be found."""
import collections
import glob
import re

PAT = re.compile(r'static final KeyMapping (\w+)\s*=\s*new KeyMapping\(([^,]+),\s*InputConstants\.Type\.KEYSYM,\s*([^,]+),')
# defaults of Tensura Reincarnated 2.0.1.0 (TensuraKeybinds bytecode, GLFW codes), the keys our defaults can collide with
TENSURA = {"B": "Tensura main GUI", "N": "Tensura name", "Z": "Tensura ability slot 1", "X": "Tensura ability slot 2", "C": "Tensura ability slot 3",
           "LEFT_ALT": "Tensura next ability mode", "R": "Tensura race ability", "V": "Tensura dodge"}


def main():
    rows = []
    for f in sorted(glob.glob("src/main/java/**/*.java", recursive=True)):
        t = open(f, encoding="utf-8").read()
        for m in PAT.finditer(t):
            k = m.group(3).strip()
            for p in ("org.lwjgl.glfw.", "GLFW.", "GLFW_KEY_", "InputConstants.KEY_"):
                k = k.replace(p, "")
            k = "UNBOUND" if "UNKNOWN" in k else {"LBRACKET": "LEFT_BRACKET", "RBRACKET": "RIGHT_BRACKET"}.get(k, k)
            rows.append((f.replace("src/main/java/com/newuniverse/nusmp/", ""), t[:m.start()].count("\n") + 1, m.group(1), m.group(2).strip(), k))
    by_key = collections.defaultdict(list)
    for r in rows:
        by_key[r[4]].append(r)
        print(" | ".join(map(str, r)))
    print("total:", len(rows), " unbound:", sum(1 for r in rows if r[4] == "UNBOUND"))
    for k, v in by_key.items():
        if k != "UNBOUND" and len(v) > 1:
            print("CONFLICT (mod vs mod)", k, [x[2] + "@" + x[0].split("/")[-1] for x in v])
        if k in TENSURA:
            print("CONFLICT (mod vs " + TENSURA[k] + ")", k, [x[2] + "@" + x[0].split("/")[-1] for x in v])


if __name__ == "__main__":
    main()
