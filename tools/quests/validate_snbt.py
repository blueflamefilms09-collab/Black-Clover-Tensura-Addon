#!/usr/bin/env python3
"""Validate the generated FTB Quests pack against what FTB Quests 2101.1.x actually reads.

Why: nothing here can be loaded by FTB Quests in this environment, so the format is proven the only other way available: the validator
parses the pack with a re-implementation of FTB Library's SNBT parser (SNBTParser.java), then checks every key of every object against the
keys the FTB Quests source reads in that object's readData (derived from the Java source by regex; cached in ftbq_keys.json so the check
also runs where the source is not checked out), plus the structural invariants (unique 63-bit hex ids, dependencies exist and are acyclic,
reward tables and chapter groups exist, advancement tasks point at advancement files, lang keys point at objects).

Usage:
    python tools/quests/validate_snbt.py                      validate src/main/resources/quest_pack
    python tools/quests/validate_snbt.py --src PATH           re-derive the key lists from a FTB Quests checkout (writes ftbq_keys.json)
    python tools/quests/validate_snbt.py --pack DIR           validate another pack folder
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
KEYS_JSON = os.path.join(HERE, "ftbq_keys.json")
DEFAULT_PACK = os.path.join(ROOT, "src", "main", "resources", "quest_pack")
ADV_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "advancement")


# ---------------------------------------------------------------------------------------------------- SNBT parser
class Tag:
    """A parsed value with its NBT tag type: byte short int long float double string list compound intarray longarray bytearray bool."""
    __slots__ = ("t", "v")

    def __init__(self, t, v):
        self.t, self.v = t, v

    def __repr__(self):
        return "%s(%r)" % (self.t, self.v)


class SnbtError(Exception):
    pass


_SIMPLE = re.compile(r"[A-Za-z0-9_.+\-∞]")


class Parser:
    """Same grammar as dev.ftb.mods.ftblibrary.snbt.SNBTParser: comment lines (// or #), unquoted keys and words, , or newline separators."""
    def __init__(self, text):
        lines = []
        for line in text.split("\n"):
            t = line.strip()
            lines.append("" if (t.startswith("//") or t.startswith("#")) else line)
        self.s = "\n".join(lines) + "\n"
        self.p = 0
        if len(self.s) < 2:
            raise SnbtError("file has to have at least two characters")

    def next(self):
        if self.p >= len(self.s):
            raise SnbtError("EOF")
        c = self.s[self.p]
        self.p += 1
        return c

    def nextns(self):
        while True:
            c = self.next()
            if c > " ":
                return c

    def pos(self):
        row = self.s.count("\n", 0, self.p) + 1
        return "%d" % row

    def read(self):
        v = self.tag(self.nextns())
        return v

    def tag(self, first):
        if first == "{":
            return self.compound()
        if first == "[":
            return self.collection()
        if first in "\"'":
            return Tag("string", self.quoted(first))
        w = self.word(first)
        if w == "true":
            return Tag("bool", True)
        if w == "false":
            return Tag("bool", False)
        if w in ("null", "end", "END"):
            return Tag("end", None)
        return self.number_or_string(w)

    @staticmethod
    def number_or_string(w):
        if not w:
            return Tag("string", w)
        last = w[-1].lower()

        def isint(x):
            return re.fullmatch(r"[+-]?\d+", x) is not None

        if last.isdigit() and isint(w) and -2 ** 31 <= int(w) < 2 ** 31:
            return Tag("int", int(w))
        start = w[:-1]
        if last == "b" and isint(start):
            return Tag("byte", int(start))
        if last == "s" and isint(start):
            return Tag("short", int(start))
        if last == "l" and isint(start):
            return Tag("long", int(start))
        if last == "f" and _isfloat(start):
            return Tag("float", float(start))
        if last == "d" and _isfloat(start):
            return Tag("double", float(start))
        if _isfloat(w):
            return Tag("double", float(w))
        return Tag("string", w)

    def word(self, first):
        out = [first]
        while True:
            c = self.next()
            if _SIMPLE.match(c):
                out.append(c)
            else:
                self.p -= 1
                return "".join(out)

    def quoted(self, stop):
        out = []
        esc = False
        esc_map = {'"': '"', "\\": "\\", "t": "\t", "b": "\b", "n": "\n", "r": "\r", "f": "\f"}
        while True:
            c = self.next()
            if c == "\n":
                raise SnbtError("new line inside a string @ line %s" % self.pos())
            if esc:
                esc = False
                if c in esc_map:
                    out.append(esc_map[c])
            elif c == "\\":
                esc = True
            elif c == stop:
                return "".join(out)
            else:
                out.append(c)

    def compound(self):
        d = {}
        while True:
            c = self.nextns()
            if c == "}":
                return Tag("compound", d)
            if c in ",\n":
                continue
            key = self.quoted(c) if c in "\"'" else self.word(c)
            n = self.nextns()
            if n not in ":=":
                raise SnbtError("expected ':' got %r @ line %s" % (n, self.pos()))
            d[key] = self.tag(self.nextns())

    def collection(self):
        prev = self.p
        n1 = self.nextns()
        n2 = self.nextns()
        if n2 == ";" and n1 in "IiLlBb":
            return self.array(n1.lower())
        self.p = prev
        return self.lst()

    def lst(self):
        items = []
        while True:
            c = self.nextns()
            if c == "]":
                return Tag("list", items)
            if c == ",":
                continue
            t = self.tag(c)
            if items and items[0].t != t.t and not (items[0].t in ("byte", "bool") and t.t in ("byte", "bool")):
                raise SnbtError("mixed tag types in a list (%s and %s) @ line %s" % (items[0].t, t.t, self.pos()))
            items.append(t)

    def array(self, kind):
        vals = []
        while True:
            c = self.nextns()
            if c == "]":
                return Tag({"i": "intarray", "l": "longarray", "b": "bytearray"}[kind], vals)
            if c == ",":
                continue
            t = self.tag(c)
            if t.t not in ("byte", "short", "int", "long"):
                raise SnbtError("array needs numbers @ line %s" % self.pos())
            vals.append(t.v)


def _isfloat(x):
    try:
        float(x)
        return re.fullmatch(r"[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?", x) is not None
    except ValueError:
        return False


def parse_file(path):
    return Parser(open(path, encoding="utf-8").read()).read()


def native(t):
    if t.t == "compound":
        return {k: native(v) for k, v in t.v.items()}
    if t.t == "list":
        return [native(v) for v in t.v]
    return t.v


# ------------------------------------------------------------------------------------ key lists from the Java source
def method_body(text, signature_re):
    m = re.search(signature_re, text)
    if not m:
        return ""
    i = text.index("{", m.end() - 1) if text[m.end() - 1] != "{" else m.end() - 1
    depth, j = 0, i
    while j < len(text):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return text[i:j + 1]
        j += 1
    return text[i:]


KEY_RES = [
    re.compile(r'\b(?:nbt|tag|data|compound|n)\.(?:get\w*|contains|has)\(\s*"([^"]+)"'),
    re.compile(r'\b\w+\.read\(\s*(?:nbt|tag)\s*,\s*"([^"]+)"'),
    re.compile(r'\bread\(\s*(?:nbt|tag)\s*,\s*"([^"]+)"'),
]
TYPE_RES = [
    (re.compile(r'contains\(\s*"([^"]+)"\s*,\s*(?:Tag|SNBTCompoundTag)\.TAG_(\w+)\s*\)'), "contains"),
    (re.compile(r'getList\(\s*"([^"]+)"\s*,\s*Tag\.TAG_(\w+)\s*\)'), "list"),
]


def read_keys(path, extra_methods=("readData",)):
    text = open(path, encoding="utf-8").read()
    keys, types = set(), {}
    for sig in extra_methods:
        body = method_body(text, r"(?:public|protected|private)?\s*(?:final\s+)?void\s+%s\s*\(\s*CompoundTag" % sig)
        for r in KEY_RES:
            keys.update(r.findall(body))
        for r, kind in TYPE_RES:
            for k, t in r.findall(body):
                types.setdefault(k, set()).add((kind, t))
    return keys, {k: sorted(v) for k, v in types.items()}


def derive(src):
    """Walk the FTB Quests source and write ftbq_keys.json."""
    q = os.path.join(src, "common", "src", "main", "java", "dev", "ftb", "mods", "ftbquests", "quest")
    out = {"source": "FTB Quests 1.21.1/dev (2101.1.x)", "objects": {}, "types": {}}

    def k(rel):
        return read_keys(os.path.join(q, rel))

    base_k, base_t = k("QuestObjectBase.java")
    out["objects"]["base"] = sorted(base_k)
    qo_k, _ = k("QuestObject.java") if os.path.exists(os.path.join(q, "QuestObject.java")) else (set(), {})
    ex = {}
    for name, rel in [("file", "BaseQuestFile.java"), ("chapter_group", "ChapterGroup.java"), ("chapter", "Chapter.java"),
                      ("quest", "Quest.java"), ("reward_table", "loot/RewardTable.java"), ("task", "task/Task.java"),
                      ("reward", "reward/Reward.java")]:
        ks, ts = k(rel)
        out["objects"][name] = sorted(ks)
        ex.update(ts)
    out["types"]["_value_types"] = {a: b for a, b in ex.items()}
    tt = open(os.path.join(q, "task", "TaskTypes.java"), encoding="utf-8").read()
    out["tasks"] = {}
    for m in re.finditer(r'register\(FTBQuestsAPI\.rl\("(\w+)"\),\s*(\w+)::new', tt):
        ks, ts = k("task/%s.java" % m.group(2))
        out["tasks"][m.group(1)] = sorted(ks)
        out["types"].update({m.group(1) + "." + a: b for a, b in ts.items()})
    rt = open(os.path.join(q, "reward", "RewardTypes.java"), encoding="utf-8").read()
    out["rewards"] = {}
    for m in re.finditer(r'register\(FTBQuestsAPI\.rl\("(\w+)"\),\s*(\w+)::new', rt):
        ks, ts = k("reward/%s.java" % m.group(2))
        if m.group(2) in ("ChoiceReward", "LootReward", "AllTableReward"):
            rk, _ = k("reward/RandomReward.java")
            ks = ks | rk
        out["rewards"][m.group(1)] = sorted(ks)
        out["types"].update({"reward." + m.group(1) + "." + a: b for a, b in ts.items()})
    # classes that extend another reward/task read through their parent, keep the union with their own
    json.dump(out, open(KEYS_JSON, "w", encoding="utf-8", newline="\n"), indent=1, sort_keys=True)
    return out


# ------------------------------------------------------------------------------------------------------- validation
HEX = re.compile(r"^[0-9A-F]{16}$")
STRUCT = {
    "file": {"version"},
    "chapter_group": {"id"},
    "chapter": {"id", "group", "order_index", "quests", "quest_links", "images", "title", "subtitle"},
    "quest": {"id", "tasks", "rewards", "title", "subtitle", "description"},
    "task": {"id", "type", "title"},
    "reward": {"id", "type", "title", "weight"},
    "reward_table": {"id", "order_index", "title"},
}
LANG_KEY = re.compile(r"^(file|chapter_group|chapter|quest|task|reward|reward_table)\.([0-9A-F]{16})\.(title|quest_subtitle|quest_desc|chapter_subtitle)$")
LIST_KEYS = {"quest_desc", "chapter_subtitle"}
INV = {"file": {"title"}}


def validate(pack, keysdb):
    errors, warns = [], []
    objs = {}          # id -> (kind, where)
    stats = {"chapters": 0, "quests": 0, "tasks": 0, "rewards": 0, "tables": 0, "groups": 0, "deps": 0, "lang": 0, "adv_tasks": 0}

    def err(m):
        errors.append(m)

    def check_keys(kind, d, where, extra=()):
        allowed = set(keysdb["objects"]["base"]) | set(keysdb["objects"].get(kind.split(":")[0], [])) | set(extra)
        if kind.startswith("task:"):
            allowed |= set(keysdb["tasks"].get(kind[5:], [])) | set(keysdb["objects"]["task"]) | STRUCT["task"]
        if kind.startswith("reward:"):
            allowed |= set(keysdb["rewards"].get(kind[7:], [])) | set(keysdb["objects"]["reward"]) | STRUCT["reward"]
        base = kind.split(":")[0]
        allowed |= STRUCT.get(base, set())
        for key in d:
            if key not in allowed:
                err("%s: key %r is not read by FTB Quests for a %s" % (where, key, kind))

    def reg(i, kind, where):
        if not isinstance(i, str) or not HEX.match(i):
            err("%s: id %r is not 16 upper-case hex digits" % (where, i))
            return
        if int(i, 16) >= 2 ** 63:
            err("%s: id %s does not fit Long.parseLong(hex, 16)" % (where, i))
        if int(i, 16) in (0, 1):
            err("%s: reserved id" % where)
        if i in objs:
            err("%s: duplicate id %s (also %s)" % (where, i, objs[i][1]))
        objs[i] = (kind, where)

    # ---- data.snbt
    p = os.path.join(pack, "data.snbt")
    data = parse_file(p)
    d = data.v
    check_keys("file", d, "data.snbt", ("version",))
    if d.get("version", Tag("int", 0)).v != keysdb.get("file_version", 13):
        warns.append("data.snbt version is %s (FTB Quests 2101.1.x writes 13); a different value only makes it rewrite the file" % d.get("version", Tag("int", 0)).v)
    reg("0000000000000001", "file", "data.snbt") if False else None

    # ---- groups
    gp = os.path.join(pack, "chapter_groups.snbt")
    groups = {}
    if os.path.exists(gp):
        g = parse_file(gp).v
        for gt in g.get("chapter_groups", Tag("list", [])).v:
            gd = gt.v
            check_keys("chapter_group", gd, "chapter_groups.snbt")
            reg(gd["id"].v, "group", "group " + gd["id"].v)
            groups[gd["id"].v] = True
            stats["groups"] += 1

    # ---- reward tables
    tables = {}
    tdir = os.path.join(pack, "reward_tables")
    for f in sorted(os.listdir(tdir)) if os.path.isdir(tdir) else []:
        if not f.endswith(".snbt"):
            continue
        t = parse_file(os.path.join(tdir, f)).v
        where = "reward_tables/" + f
        check_keys("reward_table", t, where)
        tid = t["id"].v
        reg(tid, "table", where)
        tables[tid] = f
        stats["tables"] += 1
        if "rewards" not in t or not t["rewards"].v:
            err(where + ": a reward table without rewards")
        for rt in t.get("rewards", Tag("list", [])).v:
            rd = rt.v
            reg(rd.get("id", Tag("string", "")).v, "reward", where + " entry")
            rtype = rd["type"].v if "type" in rd else "item"
            check_keys("reward:" + rtype, rd, where + " entry")
            if rtype == "item":
                _check_item(rd.get("item"), where + " entry", err)

    # ---- chapters
    chapters = {}
    questmap = {}
    cdir = os.path.join(pack, "chapters")
    chapter_files = sorted(f for f in os.listdir(cdir) if f.endswith(".snbt"))
    order_seen = {}
    for f in chapter_files:
        where = "chapters/" + f
        c = parse_file(os.path.join(cdir, f)).v
        check_keys("chapter", c, where)
        cid = c["id"].v
        reg(cid, "chapter", where)
        chapters[cid] = f
        stats["chapters"] += 1
        if c.get("filename") and c["filename"].v != f[:-5]:
            err(where + ": filename %r differs from the file name" % c["filename"].v)
        grp = c.get("group", Tag("string", "")).v
        if grp and grp not in groups:
            err(where + ": group %s does not exist" % grp)
        key = (grp, c["order_index"].v)
        if key in order_seen:
            err(where + ": order_index %s repeated in group (also %s)" % (c["order_index"].v, order_seen[key]))
        order_seen[key] = f
        if f != f.lower():
            err(where + ": FTB Quests lower-cases chapter file names")
        for qt in c.get("quests", Tag("list", [])).v:
            q = qt.v
            qw = "%s quest %s" % (where, q.get("id", Tag("string", "?")).v)
            check_keys("quest", q, qw)
            qid = q["id"].v
            reg(qid, "quest", qw)
            questmap[qid] = (q, f)
            stats["quests"] += 1
            for need in ("x", "y"):
                if need not in q or q[need].t != "double":
                    err(qw + ": %s must be a double tag" % need)
            if "icon_scale" in q and q["icon_scale"].t != "double":
                err(qw + ": icon_scale must be a double")
            if "size" in q and q["size"].t != "double":
                err(qw + ": size must be a double (it is read with getDouble)")
            if "icon" in q:
                _check_item(q["icon"], qw + " icon", err, single=True)
            if "dependency_requirement" in q and q["dependency_requirement"].v not in ("all_completed", "one_completed", "all_started", "one_started"):
                err(qw + ": bad dependency_requirement")
            if not q.get("tasks") or not q["tasks"].v:
                err(qw + ": a quest needs at least one task")
            for tt in q.get("tasks", Tag("list", [])).v:
                td = tt.v
                ttype = td["type"].v
                tw = qw + " task " + td.get("id", Tag("string", "?")).v
                if ttype not in keysdb["tasks"]:
                    err(tw + ": unknown task type " + ttype)
                    continue
                check_keys("task:" + ttype, td, tw)
                reg(td["id"].v, "task", tw)
                stats["tasks"] += 1
                _check_task(ttype, td, tw, err, stats)
            for rr in q.get("rewards", Tag("list", [])).v:
                rd = rr.v
                rtype = rd["type"].v
                rw = qw + " reward " + rd.get("id", Tag("string", "?")).v
                if rtype not in keysdb["rewards"]:
                    err(rw + ": unknown reward type " + rtype)
                    continue
                check_keys("reward:" + rtype, rd, rw)
                reg(rd["id"].v, "reward", rw)
                stats["rewards"] += 1
                _check_reward(rtype, rd, rw, err, tables)
        for key_ in ("quest_links", "images"):
            if key_ in c and c[key_].v:
                warns.append(where + ": %s present (not generated by this pack)" % key_)

    # ---- dependencies, cycles
    graph = {}
    for qid, (q, f) in questmap.items():
        deps = [x.v for x in q.get("dependencies", Tag("list", [])).v]
        graph[qid] = deps
        for dep in deps:
            stats["deps"] += 1
            if dep not in questmap:
                err("chapters/%s quest %s: dependency %s is not a quest of the pack" % (f, qid, dep))
            if dep == qid:
                err("quest %s depends on itself" % qid)
    state = {}

    def dfs(n, stack):
        if state.get(n) == 2:
            return
        if state.get(n) == 1:
            err("dependency cycle: " + " > ".join(stack + [n]))
            return
        state[n] = 1
        for m in graph.get(n, []):
            if m in graph:
                dfs(m, stack + [n])
        state[n] = 2

    for n in graph:
        dfs(n, [])

    # ---- lang
    lp = os.path.join(pack, "lang", "en_us.snbt")
    if os.path.exists(lp):
        lang = parse_file(lp).v
        kinds_of = {"quest": "quest", "chapter": "chapter", "task": "task", "reward": "reward", "reward_table": "table",
                    "chapter_group": "group", "file": "file"}
        for k, v in lang.items():
            m = LANG_KEY.match(k)
            stats["lang"] += 1
            if not m:
                err("lang: malformed key %r" % k)
                continue
            kind, oid, sub = m.groups()
            if kind != "file" and (oid not in objs or objs[oid][0] != kinds_of[kind]):
                err("lang: %s refers to a %s that does not exist" % (k, kind))
            if kind == "file" and oid != "0000000000000001":
                err("lang: the book itself is object 0000000000000001")
            if sub in LIST_KEYS and v.t != "list":
                err("lang: %s must be a list" % k)
            if sub not in LIST_KEYS and v.t != "string":
                err("lang: %s must be a string" % k)
            if v.t == "string" and not v.v.strip():
                err("lang: empty text at %s" % k)
        for qid in questmap:
            if "quest.%s.title" % qid not in lang:
                err("quest %s has no title in the lang file (it would show as an anonymous quest)" % qid)
        for cid in chapters:
            if "chapter.%s.title" % cid not in lang:
                err("chapter %s has no title" % cid)
    else:
        err("lang/en_us.snbt is missing")

    return errors, warns, stats


def _check_item(tag, where, err, single=False):
    if tag is None:
        err(where + ": missing item")
        return
    if tag.t == "string":
        iid = tag.v
    elif tag.t == "compound" and "id" in tag.v:
        iid = tag.v["id"].v
    else:
        err(where + ": item must be {id: ...}")
        return
    if not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", iid):
        err(where + ": bad item id %r" % iid)


def adv_exists(rl):
    ns, path = rl.split(":", 1)
    if ns == "nusmp":
        return os.path.exists(os.path.join(ADV_DIR, *path.split("/")) + ".json")
    return rl in ("minecraft:story/root",)


def _check_task(ttype, td, where, err, stats):
    if ttype == "advancement":
        stats["adv_tasks"] += 1
        a = td.get("advancement")
        if a is None or ":" not in a.v:
            err(where + ": advancement id missing")
        elif not adv_exists(a.v):
            err(where + ": advancement %s has no JSON file" % a.v)
    elif ttype == "item":
        _check_item(td.get("item"), where, err)
        if "count" in td and td["count"].t not in ("long", "int"):
            err(where + ": count must be numeric")
    elif ttype == "kill":
        e = td.get("entity")
        if e is None or ":" not in e.v:
            err(where + ": entity id missing")
        if "value" not in td or td["value"].v < 1:
            err(where + ": kill value must be >= 1")
    elif ttype == "dimension":
        d = td.get("dimension")
        if d is None or ":" not in d.v:
            err(where + ": dimension id missing")


def _check_reward(rtype, rd, where, err, tables):
    if rtype in ("item",):
        _check_item(rd.get("item"), where, err)
    if rtype in ("loot", "choice", "random", "all_table"):
        t = rd.get("table_id")
        if t is None or t.t != "long":
            err(where + ": table_id must be a long tag")
        else:
            hexid = "%016X" % t.v
            if hexid not in tables:
                err(where + ": reward table %s does not exist" % hexid)
    if rtype == "xp" and ("xp" not in rd or rd["xp"].v <= 0):
        err(where + ": xp must be positive")


def main(argv):
    pack = DEFAULT_PACK
    src = None
    if "--pack" in argv:
        pack = argv[argv.index("--pack") + 1]
    if "--src" in argv:
        src = argv[argv.index("--src") + 1]
    if src:
        keysdb = derive(src)
        print("derived key lists from", src, "->", os.path.relpath(KEYS_JSON, ROOT))
    else:
        keysdb = json.load(open(KEYS_JSON, encoding="utf-8"))
    try:
        errors, warns, stats = validate(pack, keysdb)
    except SnbtError as e:
        print("SNBT syntax error:", e)
        return 1
    for w in warns:
        print("warning:", w)
    for e in errors[:100]:
        print("error:", e)
    print("quest pack %s: %d chapters, %d groups, %d quests, %d tasks (%d advancement tasks), %d rewards, %d reward tables, "
          "%d dependency edges, %d lang entries; %d errors, %d warnings"
          % (os.path.relpath(pack, ROOT), stats["chapters"], stats["groups"], stats["quests"], stats["tasks"], stats["adv_tasks"],
             stats["rewards"], stats["tables"], stats["deps"], stats["lang"], len(errors), len(warns)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
