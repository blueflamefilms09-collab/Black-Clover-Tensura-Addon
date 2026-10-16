"""Builders for the quest spec: chapters, quests, tasks, rewards and reward tables.

Why: a quest line of 350 quests written as raw SNBT dictionaries would be unreadable and easy to get subtly wrong; these small builders
keep each quest to one call, apply the shape / size / reward defaults of a quest `kind` in one place, and give every object a stable key
(chapter/quest[/task n]) that gen_quests.py turns into a stable id.
"""
import textwrap

GRID = 1.5   # distance between two neighbouring quest cells on the chapter canvas


class Task:
    def __init__(self, type_, **kw):
        self.type = type_
        self.kw = kw


class T:
    """Task constructors (FTB Quests task types: advancement, item, kill, dimension, checkmark, xp)."""
    @staticmethod
    def adv(path, criterion=""):
        return Task("advancement", advancement="nusmp:" + path if ":" not in path else path, criterion=criterion)

    @staticmethod
    def vadv(path):
        return Task("advancement", advancement=path, criterion="")

    @staticmethod
    def item(item, count=1, consume=False):
        return Task("item", item=item, count=count, consume=consume)

    @staticmethod
    def kill(entity, count=1):
        return Task("kill", entity=entity, value=count)

    @staticmethod
    def dim(dim):
        return Task("dimension", dimension=dim)

    @staticmethod
    def check(title=None):
        return Task("checkmark", title=title)

    @staticmethod
    def xp(levels):
        return Task("xp", value=levels, points=False)


class Reward:
    def __init__(self, type_, **kw):
        self.type = type_
        self.kw = kw


class R:
    """Reward constructors: xp, levels, item, loot (random table), choice (pick one from a table), toast."""
    @staticmethod
    def xp(n, auto=True):
        return Reward("xp", xp=n, auto=auto)

    @staticmethod
    def levels(n):
        return Reward("xp_levels", xp_levels=n)

    @staticmethod
    def item(item, count=1, team=False):
        return Reward("item", item=item, count=count, team=team)

    @staticmethod
    def loot(table):
        return Reward("loot", table=table)

    @staticmethod
    def choice(table):
        return Reward("choice", table=table)

    @staticmethod
    def toast(text):
        return Reward("toast", description=text)


class Quest:
    def __init__(self, chapter, key, title, at, icon, kind, sub, desc, deps, tasks, rewards, optional, shape, size, dep_any, extra):
        self.chapter = chapter
        self.key = key
        self.title = title
        self.at = at
        self.icon = icon
        self.kind = kind
        self.sub = sub
        self.desc = desc
        self.deps = deps
        self.tasks = tasks
        self.rewards = rewards
        self.optional = optional
        self.shape = shape
        self.size = size
        self.dep_any = dep_any
        self.extra = extra

    @property
    def gkey(self):
        return "%s/%s" % (self.chapter.key, self.key)


KINDS = {
    # kind: (shape, size, default optional, default rewards)
    "intro": ("rsquare", 1.25, False, lambda: [R.xp(25)]),
    "step": ("circle", 1.0, False, lambda: [R.xp(20)]),
    "mile": ("diamond", 1.25, False, lambda: [R.xp(50), R.loot("journey_cache")]),
    "boss": ("octagon", 1.5, False, lambda: [R.xp(150), R.loot("boss_trophy")]),
    "final": ("hexagon", 1.5, False, lambda: [R.xp(100), R.choice("choice_boon")]),
    "side": ("square", 1.0, True, lambda: [R.xp(15)]),
    "soon": ("octagon", 1.5, False, lambda: [R.xp(150), R.loot("boss_trophy")]),
}


def paragraphs(text):
    """A description given as one string; '\\n' separates paragraphs, which FTB shows as lines with a blank line between."""
    if isinstance(text, (list, tuple)):
        return list(text)
    out = []
    for i, p in enumerate(t for t in text.split("\n") if t.strip()):
        if i:
            out.append("")
        out.append(p.strip())
    return out


class Chapter:
    def __init__(self, key, group, title, icon, sub, order):
        self.key = key
        self.group = group
        self.title = title
        self.icon = icon
        self.sub = paragraphs(sub) if sub else []
        self.order = order
        self.quests = []
        self._keys = set()

    def q(self, key, title, at, icon, kind="step", sub="", desc="", deps=(), tasks=(), rewards=None, optional=None, shape=None,
          size=None, dep_any=False, **extra):
        """Add a quest. `at` is (column, row) in grid cells; `deps` are quest keys of this chapter or 'chapter/quest' of another."""
        if key in self._keys:
            raise ValueError("duplicate quest key %s in %s" % (key, self.key))
        self._keys.add(key)
        shape0, size0, opt0, rew0 = KINDS[kind]
        if kind == "soon":
            icon = "minecraft:barrier"
            sub = sub or "Awaiting the boss"
        quest = Quest(self, key, title, at, icon, kind, sub, paragraphs(desc) if desc else [], list(deps), list(tasks),
                      rewards if rewards is not None else rew0(), opt0 if optional is None else optional, shape or shape0,
                      size or size0, dep_any, extra)
        self.quests.append(quest)
        return quest


class Group:
    def __init__(self, key, title, icon, order):
        self.key, self.title, self.icon, self.order = key, title, icon, order


class Table:
    """A reward table: (item, count, weight) entries; `choice` tables are shown as a pick-one list."""
    def __init__(self, key, title, entries, loot_size=1, empty_weight=0.0):
        self.key, self.title, self.entries = key, title, entries
        self.loot_size, self.empty_weight = loot_size, empty_weight


def wrap(text, width=80):
    return textwrap.fill(text, width)


class Registry:
    """Everything the generator needs: groups, chapters (in display order inside their group), reward tables."""
    def __init__(self):
        self.book_title = "Multiverse of Anime"
        self.groups = []
        self.chapters = []
        self.tables = []

    def group(self, key, title, icon=None):
        self.groups.append(Group(key, title, icon, len(self.groups)))

    def chapter(self, key, group, title, icon, sub=""):
        c = Chapter(key, group, title, icon, sub, len(self.chapters))
        self.chapters.append(c)
        return c

    def table(self, key, title, entries, loot_size=1, empty_weight=0.0):
        self.tables.append(Table(key, title, entries, loot_size, empty_weight))
