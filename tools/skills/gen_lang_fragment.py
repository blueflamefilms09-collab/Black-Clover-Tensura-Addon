"""Why: the rebuilt skills declare every name, description, mode name, message and chant line in Java with an English fallback
(SkillText.of / SkillMsg.t / SkillMsg.mode / SkillMsg.chant). The shared lang file src/main/resources/assets/nusmp/lang/en_us.json is
edited by another worker, so the skill keys are written to the flat fragment tools/skills/lang_fragment.json instead, and the lead merges
it. This script reads the keys STRAIGHT FROM THE SOURCE so the fragment can never drift from the code.

    python tools/skills/gen_lang_fragment.py            # (re)writes tools/skills/lang_fragment.json
    python tools/skills/gen_lang_fragment.py --check    # exits 1 if the fragment is stale (CI-friendly)

Also prints which keys already exist in en_us.json with different text (the merge should take the fragment's text for those: the
rebuild changed what those skills do, so their descriptions changed).
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "skill")
FRAGMENT = os.path.join(ROOT, "tools", "skills", "lang_fragment.json")
EN_US = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json")

S = r'"((?:[^"\\]|\\.)*)"'          # a Java string literal (group = its raw content)
RX_TEXT = re.compile(r'SkillText\.of\(\s*' + S + r'\s*,\s*' + S + r'\s*,\s*' + S + r'\s*\)')
RX_MSG = re.compile(r'SkillMsg\.t\(\s*' + S + r'\s*,\s*' + S)
RX_MODE = re.compile(r'SkillMsg\.mode\(\s*' + S + r'\s*,\s*' + S)
RX_CHANT = re.compile(r'SkillMsg\.chant\(\s*' + S + r'\s*,\s*(\d+)\s*,\s*' + S)


def unescape(s):
    return (s.replace('\\"', '"').replace("\\'", "'").replace("\\n", "\n").replace("\\\\", "\\"))


def collect():
    keys = {}

    def put(key, value, where):
        value = unescape(value)
        if key in keys and keys[key][0] != value:
            raise SystemExit("key %s has two different fallbacks:\n  %r\n  %r (%s)" % (key, keys[key][0], value, where))
        keys[key] = (value, where)

    for name in sorted(os.listdir(SRC)):
        if not name.endswith(".java"):
            continue
        text = open(os.path.join(SRC, name), encoding="utf-8").read()
        for m in RX_TEXT.finditer(text):
            put("nusmp.skill." + m.group(1), m.group(2), name)
            put("nusmp.skill." + m.group(1) + ".description", m.group(3), name)
        for m in RX_MSG.finditer(text):
            put("nusmp.skill.msg." + m.group(1), m.group(2), name)
        for m in RX_MODE.finditer(text):
            put("nusmp.skill.mode." + m.group(1), m.group(2), name)
        for m in RX_CHANT.finditer(text):
            put("nusmp.skill.chant.%s.%s" % (m.group(1), m.group(2)), m.group(3), name)
    return {k: v[0] for k, v in sorted(keys.items())}


def main():
    keys = collect()
    out = json.dumps(keys, indent=2, ensure_ascii=False) + "\n"
    if "--check" in sys.argv:
        current = open(FRAGMENT, encoding="utf-8").read() if os.path.exists(FRAGMENT) else ""
        if current != out:
            print("tools/skills/lang_fragment.json is stale: run python tools/skills/gen_lang_fragment.py")
            sys.exit(1)
        print("lang fragment is up to date (%d keys)" % len(keys))
        return
    with open(FRAGMENT, "w", encoding="utf-8") as f:
        f.write(out)
    print("wrote %d keys to %s" % (len(keys), FRAGMENT))
    if os.path.exists(EN_US):
        existing = json.load(open(EN_US, encoding="utf-8"))
        same = sum(1 for k, v in keys.items() if existing.get(k) == v)
        changed = [k for k, v in keys.items() if k in existing and existing[k] != v]
        new = [k for k in keys if k not in existing]
        print("  %d identical to en_us.json, %d new, %d changed text (take the fragment's):" % (same, len(new), len(changed)))
        for k in changed:
            print("    changed: " + k)


if __name__ == "__main__":
    main()
