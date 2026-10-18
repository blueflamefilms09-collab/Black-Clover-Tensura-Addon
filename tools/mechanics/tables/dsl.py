"""Why: the small vocabulary the hand-authored base tables are written in, so a base mechanic reads like a design card and not like JSON. A base is a card (name, source,
scope, tiers, trigger, effects, counter, and the ELEMENTS / TRIGGERS / COUNTERS it can be varied with); gen_mechanics.py expands each card into its base mechanic plus variants
that change something the player notices. A number written as (a, b) is SCALED by depth (a + b*s, s = 0 at floor 1 .. 5 at floor 1000)."""

from dataclasses import dataclass, field

THEMES = ["gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral", "radiant", "abyss", "feywild_grove", "dwarven_forgehold", "crystal_geode",
          "sunken_temple", "holy_church", "celestial_court", "sky_citadel", "mountain_monastery", "cursed_archive", "necropolis", "vampire_castle", "haunted_manor",
          "fungal_underdark", "infernal_foundry", "drowned_crypt", "spider_warrens"]
# the 28th theme of the dungeon is the Mega-Dungeon default; the engine treats "any" as no preference, the tables name only the 27 flavoured ones above plus gravity as the baseline

CATEGORIES = ["floor", "puzzle", "hazard", "combat", "risk", "boss", "travel", "party"]


def E(op, delay=0, chance=1.0, times=1, every=20, cond=None, **args):
    """One primitive call. Reserved keys: delay (ticks after the trigger), chance, times + every (repeat)."""
    d = {"op": op}
    if "frm" in args: args["from"] = args.pop("frm")      # `from` is a Python keyword
    d.update(args)
    if delay: d["delay"] = delay
    if chance != 1.0: d["chance"] = chance
    if times != 1: d["times"] = times
    if every != 20 and times != 1: d["every"] = every
    if cond: d["if"] = cond
    return d


def T(kind, **args):
    d = {"kind": kind}
    d.update(args)
    return d


def C(kind, **args):
    d = {"kind": kind}
    d.update(args)
    return d


@dataclass
class El:
    """An ELEMENT variant: a different flavour of the same design (other primitive arguments, an extra effect, a swapped primitive). `set` patches the arguments of effect i,
    `swap` replaces effect i by another primitive, `add` appends effects, `drop` removes effect indices. `label` goes into the name, `clause` into the description."""
    key: str
    label: str
    clause: str
    set: dict = field(default_factory=dict)
    swap: dict = field(default_factory=dict)
    add: list = field(default_factory=list)
    drop: list = field(default_factory=list)
    tiers: tuple = None
    road: str = None
    themes: list = None
    cost: int = 0
    flavor: str = None
    trig: dict = None
    ctr: dict = None


@dataclass
class Base:
    key: str
    name: str
    src: str
    desc: str
    flavor: str
    scope: str
    tiers: tuple
    road: str
    themes: list
    cost: int
    weight: int
    trig: dict
    fx: list
    ctr: dict
    reward: str = None
    elems: list = field(default_factory=list)
    trigs: list = field(default_factory=list)
    ctrs: list = field(default_factory=list)
    cat: str = ""


def base(key, name, src, desc, flavor, scope, trig, fx, ctr=None, tiers=(1, 5), road="both", themes=None, cost=3, weight=10, reward=None, elems=(), trigs=(), ctrs=()):
    return Base(key, name, src, desc, flavor, scope, tuple(tiers), road, list(themes or []), cost, weight, trig, list(fx), ctr or C("none"), reward, list(elems), list(trigs), list(ctrs))
