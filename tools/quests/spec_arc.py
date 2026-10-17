"""The advancements of the characters of the arcs: one per boss ("defeated"), one per friend ("met"), one per saga ("cleared"), and one for the arenas.

Why: the roster of the characters is Java (arc/ArcRoster, filled by the character builders), and Python cannot read Java, but the owner wants EVERY character
to have an advancement the day it is merged. So the bridge is a plain file, tools/quests/arc_roster.txt, one line `id|name|saga|role` per character, which the
in-game command `/multiverse arc dump` writes from the live roster (arc/world/RosterDump.java, the writer, and this reader are pinned together by a test).
Refresh the file after a merge, run tools/quests/gen_arc_adv.py (or gen_advancements.py), and the advancements, their lang and the quest hooks follow. An
advancement id never changes (it is `arc/defeated_<id>`, `arc/met_<id>`, `arc/saga_<saga>`), so a regenerated pack keeps every player's progress.

The event keys are the core's (docs/arc/core.md): `arc.defeated` and `arc.met` (the argument is the character's id), plus `arc.saga_clear` (the saga's id) and
`arc.arena` (the boss's id) which arc/world fires; all four are in the contract list of AdvancementDataTest.EVENT_KEYS. The folder is `arc` (the older `arcs`
folder is the story-arc milestones of the first tree and stays as it is). Only vanilla item icons are used, so the generator does not depend on lang keys that
have not been merged yet.

Hook: the last lines of spec_adv.py import this module and call `build(S)`.
"""
import os
import re

from spec_lib import ev, ev_any

HERE = os.path.dirname(os.path.abspath(__file__))
ROSTER_FILE = os.environ.get("ARC_ROSTER") or os.path.join(HERE, "arc_roster.txt")

# The sagas as arc/ArcSaga.java names them: id -> (title, order). An id the roster file names that is not here is shown by its prettified id, after these.
SAGAS = {
    "elf_reincarnation": ("Elf Reincarnation Saga", 1),
    "spade_raid": ("Spade Kingdom Raid Saga", 2),
    "wizard_king": ("Ultimate Wizard King Arc", 3),
    "seven_deadly_sins": ("The Seven Deadly Sins", 4),
    "danmachi": ("DanMachi", 5),
    "allies": ("Allies", 6),
}
SAGA_ICON = {
    "elf_reincarnation": "minecraft:written_book", "spade_raid": "minecraft:black_banner", "wizard_king": "minecraft:nether_star",
    "seven_deadly_sins": "minecraft:iron_sword", "danmachi": "minecraft:lantern", "allies": "minecraft:white_banner",
}
ID_RE = re.compile(r"^[a-z0-9_]+$")


def prettify(ident):
    return " ".join(w.capitalize() for w in ident.split("_"))


def saga_title(saga):
    return SAGAS[saga][0] if saga in SAGAS else prettify(saga)


def read_roster(path=None):
    """The characters of the roster file as a list of dicts {id, name, saga, role}, in file order. Blank lines and lines that start with # are skipped; a bad line
    raises ValueError with its line number (a silent skip would hide a character)."""
    path = path or ROSTER_FILE
    out, seen = [], set()
    if not os.path.exists(path):
        return out
    with open(path, encoding="utf-8") as fh:
        for n, raw in enumerate(fh, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = [p.strip() for p in line.split("|")]
            if len(parts) != 4:
                raise ValueError("%s:%d: expected id|name|saga|role, got %r" % (os.path.basename(path), n, line))
            ident, name, saga, role = parts
            if not ID_RE.match(ident):
                raise ValueError("%s:%d: bad id %r (lower case letters, digits and _ only)" % (os.path.basename(path), n, ident))
            if not ID_RE.match(saga):
                raise ValueError("%s:%d: bad saga %r" % (os.path.basename(path), n, saga))
            if role not in ("boss", "npc"):
                raise ValueError("%s:%d: the role must be boss or npc, got %r" % (os.path.basename(path), n, role))
            if not name:
                raise ValueError("%s:%d: no name" % (os.path.basename(path), n))
            if ident in seen:
                raise ValueError("%s:%d: %s is listed twice" % (os.path.basename(path), n, ident))
            seen.add(ident)
            out.append({"id": ident, "name": name, "saga": saga, "role": role})
    return out


def build(spec, roster=None):
    """Adds the advancements of the roster to the spec (the hook at the end of spec_adv.py). Returns how many were added."""
    chars = read_roster() if roster is None else roster
    add = spec.add
    start = len(spec.advs)
    add("arc/root", "root", "The Chronicle of the Arcs",
        "Meet a friend or defeat a boss of the arcs: every character of the stories you walk through is written down here.",
        "minecraft:written_book", ev_any(("arc.met",), ("arc.defeated",)), frame="task", xp=10)
    add("arc/arena", "arc/root", "Into the Arena", "Be carried into a story arena by the Chronicle Sigil.", "minecraft:lectern", ev("arc.arena"), frame="goal", xp=40)
    sagas = []
    for c in chars:
        if c["saga"] not in sagas:
            sagas.append(c["saga"])
    sagas.sort(key=lambda s: (SAGAS[s][1] if s in SAGAS else 99, s))
    for s in sagas:
        bosses = [c for c in chars if c["saga"] == s and c["role"] == "boss"]
        if bosses:
            add("arc/saga_" + s, "arc/root", saga_title(s) + ": Cleared",
                ("Defeat all %d bosses of: %s." % (len(bosses), saga_title(s))) if len(bosses) > 1 else "Defeat the boss of: %s." % saga_title(s),
                SAGA_ICON.get(s, "minecraft:written_book"), ev("arc.saga_clear", 1, s), frame="challenge", xp=300)
    for c in chars:
        parent = "arc/saga_" + c["saga"] if any(b["saga"] == c["saga"] and b["role"] == "boss" for b in chars) else "arc/root"
        if c["role"] == "boss":
            add("arc/defeated_" + c["id"], parent, "Defeated: " + c["name"], "Defeat %s. Saga: %s." % (c["name"], saga_title(c["saga"])),
                "minecraft:iron_sword", ev("arc.defeated", 1, c["id"]), frame="goal", xp=50)
        else:
            add("arc/met_" + c["id"], parent, "Met: " + c["name"], "Speak with %s. Saga: %s." % (c["name"], saga_title(c["saga"])),
                "minecraft:book", ev("arc.met", 1, c["id"]), frame="task", xp=10)
    return len(spec.advs) - start

