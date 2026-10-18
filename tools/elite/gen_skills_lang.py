"""Why: the elite skills declare every name, description, mode name and message in Java with an English fallback (SkillText.of, SkillMsg.t, EliteMode.of), exactly like the rebuilt skills. The shared lang
file src/main/resources/assets/nusmp/lang/en_us.json is edited by another worker and is NOT touched by this job, so the keys are written to the flat fragment tools/elite/skills_lang.json for the lead to
merge. This script reads the keys STRAIGHT FROM THE SOURCE (elite/skills/**.java), so the fragment can never drift from the code.

    python tools/elite/gen_skills_lang.py            # (re)writes tools/elite/skills_lang.json
    python tools/elite/gen_skills_lang.py --check    # exits 1 if the fragment is stale (CI-friendly)

Key shapes (the same as tools/skills/gen_lang_fragment.py, so both fragments merge into one file without a clash):
  SkillText.of("id", "Name", "Description")  ->  nusmp.skill.<id>, nusmp.skill.<id>.description
  SkillMsg.t("key", "Fallback")              ->  nusmp.skill.msg.<key>
  EliteMode.of("id", "Name", cost, cooldown) ->  nusmp.skill.mode.<id>
It also prints how many keys already exist in en_us.json (a key that exists with another text is reported; the merge should keep the older text for the shared messages "voice", "evolution.learned",
"no_magicules", which this job reuses unchanged).
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "elite", "skills")
FRAGMENT = os.path.join(ROOT, "tools", "elite", "skills_lang.json")
EN_US = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json")

S = r'"((?:[^"\\]|\\.)*)"'          # a Java string literal (group = its raw content)
RX_TEXT = re.compile(r'SkillText\.of\(\s*' + S + r'\s*,\s*' + S + r'\s*,\s*' + S + r'\s*\)')
RX_MSG = re.compile(r'SkillMsg\.t\(\s*' + S + r'\s*,\s*' + S)
RX_MODE = re.compile(r'EliteMode\.of\(\s*' + S + r'\s*,\s*' + S)


def unescape(s):
    return s.replace('\\"', '"').replace("\\'", "'").replace("\\n", "\n").replace("\\\\", "\\")


def java_files():
    for dirpath, _, names in os.walk(SRC):
        for n in sorted(names):
            if n.endswith(".java"):
                yield os.path.join(dirpath, n)


def collect():
    keys = {}

    def put(key, value, where):
        value = unescape(value)
        if key in keys and keys[key][0] != value:
            raise SystemExit("key %s has two different fallbacks:\n  %r\n  %r (%s)" % (key, keys[key][0], value, where))
        keys[key] = (value, where)

    for path in sorted(java_files()):
        name = os.path.basename(path)
        text = open(path, encoding="utf-8").read()
        for m in RX_TEXT.finditer(text):
            put("nusmp.skill." + m.group(1), m.group(2), name)
            put("nusmp.skill." + m.group(1) + ".description", m.group(3), name)
        for m in RX_MSG.finditer(text):
            put("nusmp.skill.msg." + m.group(1), m.group(2), name)
        for m in RX_MODE.finditer(text):
            put("nusmp.skill.mode." + m.group(1), m.group(2), name)
    return {k: v[0] for k, v in sorted(keys.items())}


def main():
    keys = collect()
    out = json.dumps(keys, indent=2, ensure_ascii=False) + "\n"
    if "--check" in sys.argv:
        current = open(FRAGMENT, encoding="utf-8").read() if os.path.exists(FRAGMENT) else ""
        if current != out:
            print("tools/elite/skills_lang.json is stale: run python tools/elite/gen_skills_lang.py")
            sys.exit(1)
        print("elite skills lang fragment is up to date (%d keys)" % len(keys))
        return
    with open(FRAGMENT, "w", encoding="utf-8") as f:
        f.write(out)
    print("wrote %d keys to %s" % (len(keys), FRAGMENT))
    if os.path.exists(EN_US):
        existing = json.load(open(EN_US, encoding="utf-8"))
        same = sum(1 for k, v in keys.items() if existing.get(k) == v)
        changed = [k for k, v in keys.items() if k in existing and existing[k] != v]
        new = [k for k in keys if k not in existing]
        print("  %d identical to en_us.json, %d new, %d exist with another text:" % (same, len(new), len(changed)))
        for k in changed:
            print("    differs: " + k)


if __name__ == "__main__":
    main()
