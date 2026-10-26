#!/usr/bin/env python3
"""The rarity of every Skill Book (release 3.26.0, docs/skillbooks_3_26_rarity.md).

gen_book_index.py writes the result into assets/nusmp/skill_books.json as the "rarity" field of each line (absent = common for the game). The five rarities, in rank order:
common, uncommon, rare, epic, mythic (Java: skilltree/rarity/BookRarity).

HOW a line gets its rarity (in this order):
  1. NEW_SKILLS: the ten Evolution skills of 3.26.0 are fixed by their design doc (the lead fills this dict; ids that are not in the table yet are ignored, so this file works
     before and after they exist).
  2. EXPLICIT: a hand-written override, for the lines whose lore decides it: the monarch / god / sovereign / ultimate-tier lines are MYTHIC, a few story-level lines are EPIC, and the
     four hand-built classics are placed by hand.
  3. COMPUTED: from the strongest art / finisher of the line's route file. score(route) is described at the function; the score is cut at the fixed THRESHOLDS below into
     common / uncommon / rare / epic. The thresholds are constants (not quantiles of the current set) so that adding a line later never moves the rarity of an existing one.
     The computed fallback never gives mythic: that tier is only ever written by hand.

The target spread over all lines is about 30% common, 28% uncommon, 24% rare, 12% epic, 6% mythic (docs/skillbooks_3_26.md section 5); the thresholds were calibrated on the 254 lines
of 3.25 (see `python3 tools/lines/rarity_table.py --report`).

usage: python3 tools/lines/rarity_table.py [--report] [--routes-dir DIR]
"""
import binascii
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ROUTES = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "skill_trees")

RARITIES = ["common", "uncommon", "rare", "epic", "mythic"]

# The ten Evolution skills (3.26.0). The lead adds {"<line id>": "<rarity>"} here once the design doc fixes them; ids that do not exist yet are ignored.
NEW_SKILLS = {}

# Hand-written rarities. MYTHIC = the monarch / god / sovereign / ultimate-tier lines (about 6 percent of all lines); EPIC = story-level lines; the four hand-built classics.
EXPLICIT = {
    # --- the four hand-built classics of the Exiled Heavy Knight (5 or 6 nodes, no finisher: the computed score cannot judge them)
    "basic_swordsmanship": "common",
    "lady_luck": "uncommon",
    "oath_of_the_heavy_armor": "rare",
    "smoldering_fang_of_insanity": "mythic",        # the Black Market's flagship, "the target of the whole shop"
    # --- MYTHIC: monarch / god / sovereign / ultimate-tier lines
    "shadow_extraction": "mythic",                  # Solo Leveling: Arise, the Shadow Monarch's army
    "monarchs_domain": "mythic",                    # Solo Leveling: the Monarchs' absolute domain
    "white_flame_sovereign": "mythic",              # Solo Leveling: Sovereign of White Flames
    "storm_dragon_veldora": "mythic",               # Tensura: one of the four True Dragons
    "dragon_nova_milim": "mythic",                  # Tensura: Milim, the Destroyer (a Demon Lord)
    "wizard_king_sovereign": "mythic",              # Black Clover: the Wizard King
    "devil_union_vessel": "mythic",                 # Black Clover: Devil Union, a devil's full power
    "solomon_wisdom_sage": "mythic",                # Magi: King Solomon's wisdom
    "goh_zeus_charyeok": "mythic",                  # God of High School: the king of the gods
    "goh_monkey_king": "mythic",                    # God of High School: the Great Sage Equalling Heaven
    "tog_zahard_decree": "mythic",                  # Tower of God: the King of the Tower
    "asura_ascension": "mythic",                    # TBATE: an Asura's ascension
    "freya_charm": "mythic",                        # DanMachi: a goddess's absolute charm
    "omniscient_viewpoint": "mythic",               # Omniscient Reader's Viewpoint: the title skill
    "constellation_patronage": "mythic",            # Omniscient Reader's Viewpoint: a constellation's patronage
    # --- EPIC: story-level lines
    "dragon_will": "epic",
    "demon_king_of_salvation": "epic",
    "primordial_red_destruction": "epic",
    "primordial_black_diablo": "epic",
    "crimson_sovereign": "epic",
    "tog_jahad_royal": "epic",
    "goh_charyeok_vessel": "epic",
    "multi_vessel_warlord": "epic",
    "frost_monarch": "epic",
    "beast_monarch": "epic",
    "plague_monarch": "epic",
    "iron_body_monarch": "epic",
    "kamish_wrath": "epic",
    "elven_rite_bearer": "epic",
    "extreme_magic_adept": "epic",
    "breaking_the_sky_sword": "epic",
}

# Computed score -> rarity. Constants, calibrated once (see --report); a score at or above the first number it reaches wins, below the last is common.
THRESHOLDS = [("epic", 2.023), ("rare", 1.79), ("uncommon", 1.61)]

# How strong a finisher is by its kind (damage-dealing area and ray kinds count most; utility kinds least).
KIND_POWER = {"meteor_drop": 1.30, "ray_shot": 1.22, "ring_burst": 1.12, "ground_wave": 1.10, "execute": 1.12, "cone_strike": 1.04, "leap_slam": 1.02, "dash_strike": 1.00,
              "whirlwind": 1.00, "barrage": 1.00, "chain_hit": 0.95, "drain_bite": 1.00, "stun_ring": 1.05, "pull": 0.90, "shield": 0.92, "heal_pulse": 0.90, "buff_aura": 0.95,
              "buff_self": 0.85, "heal_self": 0.85, "ward_reflect": 0.95, "debuff_cone": 0.90, "smoke": 0.70, "blink": 0.75, "mark": 0.70, "taunt": 0.70}
# The mean last-level damage of each damaging kind (measured over the finishers of the 254 lines of 3.25): a finisher above its kind's mean scores higher.
KIND_DAMAGE = {"ray_shot": 46.9, "ring_burst": 37.7, "barrage": 9.4, "ground_wave": 40.2, "whirlwind": 9.9, "dash_strike": 37.8, "meteor_drop": 48.9, "leap_slam": 43.0,
               "cone_strike": 44.1, "chain_hit": 31.7, "execute": 45.4, "pull": 30.5, "drain_bite": 45.3}
FINISHER_COOLDOWN = 900            # an art whose last-level cooldown is at least this many ticks is a finisher (the generator enforces it)
# Words in the line's or its finishers' names that mark a legend-tier theme.
KEYWORDS = {"sovereign": 0.35, "monarch": 0.35, "ultimate": 0.30, "king": 0.25, "god": 0.25, "absolute": 0.20, "divine": 0.20, "apocalypse": 0.20, "heaven": 0.15, "dragon": 0.15,
            "annihilat": 0.15, "cataclysm": 0.15, "primordial": 0.15, "judgment": 0.10, "judgement": 0.10, "eclipse": 0.10, "world": 0.10, "zero": 0.10}
KEYWORD_CAP = 0.45


def _last(values, key):
    v = values.get(key)
    return v[-1] if isinstance(v, list) and v else None


def finisher_powers(route):
    """The power of every finisher (art with a cooldown of 900+ ticks at its last level) of a route dict, highest first."""
    out = []
    for n in route.get("nodes", []):
        if n.get("type") != "active_ability" or n.get("kind") == "stance":
            continue
        vals = n.get("values", {})
        cd = _last(vals, "cooldown")
        if cd is None or cd < FINISHER_COOLDOWN:
            continue
        kind = n.get("kind", "")
        rel = 1.0
        dmg = _last(vals, "damage")
        if dmg is not None and kind in KIND_DAMAGE:
            rel = max(0.4, min(1.6, dmg / KIND_DAMAGE[kind]))
        out.append(KIND_POWER.get(kind, 0.9) * (0.5 + 0.5 * rel))
    return sorted(out, reverse=True)


def keyword_bonus(route):
    text = [route.get("name", "")]
    for n in route.get("nodes", []):
        if n.get("type") == "active_ability" and n.get("kind") != "stance":
            cd = _last(n.get("values", {}), "cooldown")
            if cd is not None and cd >= FINISHER_COOLDOWN:
                text.append(n.get("name", ""))
    low = " ".join(text).lower()
    return min(KEYWORD_CAP, sum(w for k, w in KEYWORDS.items() if k in low))


def jitter(lid):
    """A stable tie-breaker in [-0.04, 0.04) from the id, so lines with the same strength do not all land on one side of a threshold."""
    return ((binascii.crc32(lid.encode("utf-8")) % 1000) / 1000.0 - 0.5) * 0.08


def score(lid, route):
    """The strength of a line: its strongest finisher, a half of the second, a little for a deep tree, a legend-tier name, and the tie-breaker."""
    p = finisher_powers(route)
    s = (p[0] if p else 0.0) + (0.5 * p[1] if len(p) > 1 else 0.0)
    s += min(0.30, max(0, len(route.get("nodes", [])) - 21) * 0.05)
    s += keyword_bonus(route)
    return s + jitter(lid)


def from_score(s):
    for name, at in THRESHOLDS:
        if s >= at:
            return name
    return "common"


def rarity_of(lid, route=None):
    """The rarity id of one line: the new skills' table, then the explicit table, then the computed fallback (common when there is no route data)."""
    if lid in NEW_SKILLS:
        return NEW_SKILLS[lid]
    if lid in EXPLICIT:
        return EXPLICIT[lid]
    return from_score(score(lid, route)) if route else "common"


def load_route(lid, routes_dir=None):
    path = os.path.join(routes_dir or ROUTES, lid + ".json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def table(ids, routes_dir=None):
    """-> dict id -> rarity id for these line ids. Ids of NEW_SKILLS / EXPLICIT that are not in `ids` are ignored (they do not exist yet)."""
    out = {}
    for lid in ids:
        out[lid] = rarity_of(lid, load_route(lid, routes_dir))
    bad = [(k, v) for k, v in out.items() if v not in RARITIES]
    if bad:
        raise ValueError("unknown rarity: %s" % bad)
    return out


def _report(routes_dir=None):
    ids = sorted(os.path.basename(p)[:-5] for p in glob.glob(os.path.join(routes_dir or ROUTES, "*.json")))
    t = table(ids, routes_dir)
    n = len(t)
    print("%d lines" % n)
    for r in RARITIES:
        members = [k for k, v in t.items() if v == r]
        print("%-9s %3d  %4.1f%%" % (r, len(members), 100.0 * len(members) / max(1, n)))
    scores = sorted((score(i, load_route(i, routes_dir)), i) for i in ids if i not in EXPLICIT and i not in NEW_SKILLS)
    print("computed lines: %d, score min %.2f max %.2f" % (len(scores), scores[0][0], scores[-1][0]))
    for q in (0.30, 0.58, 0.82):
        print("  quantile %.2f -> %.3f" % (q, scores[int(q * len(scores))][0]))
    for r in reversed(RARITIES):
        print("\n== %s ==" % r)
        print(", ".join(sorted(k for k, v in t.items() if v == r)))
    unknown = sorted(set(EXPLICIT) - set(ids))
    if unknown:
        print("\nEXPLICIT ids with no route file (ignored): %s" % ", ".join(unknown))


if __name__ == "__main__":
    args = sys.argv[1:]
    rd = args[args.index("--routes-dir") + 1] if "--routes-dir" in args else None
    _report(rd)
