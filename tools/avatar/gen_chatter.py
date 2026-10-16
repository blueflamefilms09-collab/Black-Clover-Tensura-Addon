#!/usr/bin/env python3
"""Why: the Avatar chatter dictionary is hand-written, one file per personality under tools/avatar/chatter_data/, and this script only checks the
rules (every event has at least 14 distinct lines, every line under 160 characters, only known placeholders) and writes the shipped JSON
files data/nusmp/avatar_chatter/<personality>.json. Run it after editing a data file; the JUnit schema test enforces the same rules on the JSON."""
import importlib.util, json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "nusmp", "avatar_chatter")
REQUIRED = ["COMBAT_START", "CREATOR_LOW_HP", "SKILL_CAST", "NAMING", "BETRAYAL", "FRENZY", "IDLE"]
EXTRA = {"JEALOUS_WARN"}
PLACEHOLDERS = {"name", "creator", "target", "skill", "grimoire", "dungeon", "relic", "magicule"}
MIN_LINES, MAX_LEN = 14, 159

def norm(s):
    return re.sub(r"[^a-z0-9{}]+", " ", s.lower()).strip()

def main():
    os.makedirs(OUT, exist_ok=True)
    data_dir = os.path.join(HERE, "chatter_data")
    bad = 0
    total = 0
    for fn in sorted(os.listdir(data_dir)):
        if not fn.endswith(".py"):
            continue
        spec = importlib.util.spec_from_file_location(fn[:-3], os.path.join(data_dir, fn))
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        for ev in REQUIRED:
            if len(mod.LINES.get(ev, [])) < MIN_LINES:
                print(f"{fn}: {ev} has {len(mod.LINES.get(ev, []))} lines (< {MIN_LINES})"); bad += 1
        for ev, lines in mod.LINES.items():
            if ev not in REQUIRED and ev not in EXTRA:
                print(f"{fn}: unknown event {ev}"); bad += 1
            seen = set()
            for l in lines:
                n = norm(l)
                if not l.strip() or len(l) > MAX_LEN:
                    print(f"{fn}: {ev}: empty or too long: {l!r}"); bad += 1
                if n in seen:
                    print(f"{fn}: {ev}: duplicate (ignoring punctuation): {l!r}"); bad += 1
                seen.add(n)
                for ph in re.findall(r"\{([a-zA-Z_]+)\}", l):
                    if ph not in PLACEHOLDERS:
                        print(f"{fn}: {ev}: unknown placeholder {ph}"); bad += 1
                total += 1
        with open(os.path.join(OUT, mod.PERSONALITY.lower() + ".json"), "w", encoding="utf-8") as f:
            json.dump({"personality": mod.PERSONALITY, "events": mod.LINES}, f, indent=1, ensure_ascii=False)
            f.write("\n")
    print(f"{total} lines, {bad} problems")
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main())
