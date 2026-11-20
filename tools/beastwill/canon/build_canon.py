"""Builds the canon rework of the 27 Beast Wills (docs/beastwill_canon_rework.md).

Reads   src/main/resources/data/anime_multiverse/beast_will/*.json        (the remade wills, never changed)
Writes  src/main/resources/data/anime_multiverse/beast_will_canon/*.json  (the canon wills, the new default)
        tools/lang/beastwill_canon.json                                    (new lang keys only; merged by the lang merge)
        docs/beastwill_canon_rework.md                                     (the doc, from the same table)
Run:    python3 tools/beastwill/canon/build_canon.py
It also checks every file against the same bounds as BeastWillDef.problem(), the rank progression and the signature rule, and stops with a message on the first failure.
"""
import copy
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import canon_data as C  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
BASE = os.path.join(ROOT, "src/main/resources/data/anime_multiverse/beast_will")
OUT = os.path.join(ROOT, "src/main/resources/data/anime_multiverse/beast_will_canon")
LANG_OUT = os.path.join(ROOT, "tools/lang/beastwill_canon.json")
DOC_OUT = os.path.join(ROOT, "docs/beastwill_canon_rework.md")
EN_US = os.path.join(ROOT, "src/main/resources/assets/anime_multiverse/lang/en_us.json")

ATTR = {"spd": "minecraft:generic.movement_speed", "atk": "minecraft:generic.attack_damage", "hp": "minecraft:generic.max_health", "arm": "minecraft:generic.armor",
        "tough": "minecraft:generic.armor_toughness", "kb": "minecraft:generic.knockback_resistance", "aspd": "minecraft:generic.attack_speed",
        "step": "minecraft:generic.step_height", "jump": "minecraft:generic.jump_strength", "fall": "minecraft:generic.safe_fall_distance"}
CAPS = {"minecraft:generic.movement_speed": (0.02, 0.40), "minecraft:generic.attack_damage": (8.0, 0.50), "minecraft:generic.max_health": (12.0, 0.30),
        "minecraft:generic.armor": (10.0, 0.0), "minecraft:generic.armor_toughness": (5.0, 0.0), "minecraft:generic.knockback_resistance": (0.9, 0.0),
        "minecraft:generic.attack_speed": (0.0, 0.30), "minecraft:generic.step_height": (0.6, 0.0), "minecraft:generic.jump_strength": (0.0, 0.40),
        "minecraft:generic.safe_fall_distance": (6.0, 0.0)}
# kind -> (maxDuration, maxRadius, maxTargets, minCooldown), the same table as BeastWillRules.KINDS
KINDS = {"dragon_sight": (200, 24, 8, 200), "draconic_pulse": (100, 8, 6, 60), "time_exclusion": (100, 10, 6, 600), "shadow_veil": (200, 0, 1, 200), "shadow_dash": (0, 0, 1, 60),
         "ambush": (0, 0, 1, 200), "vine_lash": (60, 10, 3, 40), "verdant_mend": (200, 0, 1, 400), "restraining_vines": (100, 8, 6, 400), "bark_ward": (200, 0, 1, 400),
         "mane_surge": (200, 0, 1, 300), "world_howl": (60, 12, 10, 400), "venom_spit": (120, 10, 6, 100), "maelstrom": (100, 10, 6, 240), "dread_gaze": (160, 16, 6, 300),
         "tail_sweep": (0, 6, 6, 120), "thunder_chain": (0, 6, 5, 100), "devour": (0, 0, 1, 200)}
NEW_KINDS = ("tail_sweep", "thunder_chain", "devour")


def fail(msg):
    sys.exit("build_canon: " + msg)


def short_of(attr):
    for k, v in ATTR.items():
        if v == attr:
            return k
    return attr


def set_stat(stats, short, op, amount):
    for s in stats:
        if s["attribute"] == ATTR[short] and s["op"] == op:
            s["amount"] = amount
            return
    stats.append({"attribute": ATTR[short], "op": op, "amount": amount})


def build_ability(old_phase, spec, slot, phase):
    """One ability dict: copied from the remade will when kept, otherwise from the kind's defaults."""
    if spec["keep"]:
        old = next((a for a in old_phase["abilities"] if a["id"] == spec["id"]), None)
        if old is None:
            fail("kept ability %s is not in the remade will" % spec["id"])
        a = copy.deepcopy(old)
    else:
        key = (spec["kind"], phase)
        if key not in C.DEFAULTS:
            fail("no defaults for %s in phase %d" % key)
        a = {"id": spec["id"], "kind": spec["kind"], "slot": slot}
        for f, v in zip(C.FIELDS, C.DEFAULTS[key]):
            a[f] = v
        a["name"] = spec["name"]
        a["effect"] = spec["effect"]
    a["slot"] = slot
    for k, v in spec["ov"].items():
        if k not in C.FIELDS:
            fail("unknown override %s" % k)
        a[k] = v
    return a


def validate(d):
    """The checks of BeastWillDef.problem() for the parts the canon rework touches, plus the rank progression."""
    i = d["id"]
    for where, stats in (("passive", d["passive"]), ("acquire", d["acquire"]["stats"]), ("integrate", d["integrate"]["stats"])):
        for s in stats:
            cap = CAPS.get(s["attribute"])
            if cap is None:
                fail("%s %s: attribute not allowed" % (i, where))
            mx = cap[0] if s["op"] == "add" else cap[1]
            if not (0 < s["amount"] <= mx + 1e-9):
                fail("%s %s: %s %s %s over the cap %s" % (i, where, s["attribute"], s["op"], s["amount"], mx))
    for ph in ("acquire", "integrate"):
        p = d[ph]
        sums = {}
        for s in d["passive"] + p["stats"]:
            k = (s["attribute"], s["op"])
            sums[k] = sums.get(k, 0) + s["amount"]
        for (attr, op), v in sums.items():
            mx = CAPS[attr][0] if op == "add" else CAPS[attr][1]
            if v > mx + 1e-9:
                fail("%s %s: passive plus form %s %s = %s passes the cap %s" % (i, ph, attr, op, v, mx))
        if not (0 <= p["drainPct"] <= 3) or not (0 <= p["strainPerSecond"] <= 2.5) or not (0 <= p["regen"] <= 1.0):
            fail("%s %s: drain, strain or regen out of bounds" % (i, ph))
        abil = p["abilities"]
        if not (1 <= len(abil) <= 2):
            fail("%s %s: needs 1 or 2 abilities" % (i, ph))
        slots = set()
        kinds = set()
        for a in abil:
            k = KINDS.get(a["kind"])
            if k is None:
                fail("%s: unknown kind %s" % (i, a["kind"]))
            if a["slot"] in slots or a["slot"] not in (1, 2):
                fail("%s %s: bad slot" % (i, ph))
            slots.add(a["slot"])
            if a["kind"] in kinds:
                fail("%s %s: both abilities are of kind %s" % (i, ph, a["kind"]))
            kinds.add(a["kind"])
            at = "%s %s/%s" % (i, ph, a["id"])
            if not (0 <= a["costPct"] <= 25) or not (0 <= a["strain"] <= 40) or not (0 <= a["range"] <= 24) or not (0 <= a["power"] <= 8) or not (0 <= a["bossFactor"] <= 1):
                fail(at + ": cost, strain, range, power or bossFactor out of bounds")
            if not (k[3] <= a["cooldownTicks"] <= 12000) or a["radius"] > k[1] or a["durationTicks"] > k[0] or not (1 <= a["maxTargets"] <= k[2]):
                fail(at + ": cooldown, radius, duration or targets out of the bounds of " + a["kind"])
    if d["acquire"]["unlockAssimilation"] > 0.5 or d["integrate"]["unlockAssimilation"] < 0.9 or d["integrate"]["unlockMastery"] < 0.3:
        fail(i + ": phase unlock out of bounds")
    rank = d["minRank"]
    if abs(d["acquire"]["unlockAssimilation"] - C.ACQUIRE_UNLOCK[rank]) > 1e-9 or d["integrate"]["unlockMastery"] + 1e-9 < C.INTEGRATE_MASTERY[rank]:
        fail(i + ": the progression does not follow the rank " + rank)
    if not (1000 <= d["requireEp"] <= 1e9):
        fail(i + ": requireEp out of bounds")


def main():
    old = {}
    for f in sorted(glob.glob(os.path.join(BASE, "*.json"))):
        with open(f, encoding="utf-8") as fh:
            d = json.load(fh)
        old[d["id"]] = d
    if set(old) != set(C.WILLS):
        fail("the table and the remade wills differ: %s" % sorted(set(old) ^ set(C.WILLS)))
    with open(EN_US, encoding="utf-8") as fh:
        en = json.load(fh)

    os.makedirs(OUT, exist_ok=True)
    built = {}
    for wid, w in C.WILLS.items():
        o = old[wid]
        d = copy.deepcopy(o)
        rank = w.get("rank", o["minRank"])
        d["minRank"] = rank
        d["affinity"] = w.get("affinity", o["affinity"])
        d["legendary"] = w.get("legendary", o["legendary"])
        d.update(w.get("TOP", {}))
        for short, op, amount in w.get("passive", []):
            set_stat(d["passive"], short, op, amount)
        d["acquire"]["unlockAssimilation"] = C.ACQUIRE_UNLOCK[rank]
        d["acquire"]["unlockMastery"] = 0.0
        d["integrate"]["unlockMastery"] = max(o["integrate"]["unlockMastery"], C.INTEGRATE_MASTERY[rank])
        d["acquire"].update(w.get("PH_A", {}))
        d["integrate"].update(w.get("PH_I", {}))
        for short, op, amount in w.get("A_stats", []):
            set_stat(d["acquire"]["stats"], short, op, amount)
        for short, op, amount in w.get("I_stats", []):
            set_stat(d["integrate"]["stats"], short, op, amount)
        if "req_ep" in w:
            d["requireEp"] = w["req_ep"]
        for ph, key, phase in (("acquire", "A", 1), ("integrate", "I", 2)):
            d[ph]["abilities"] = [build_ability(o[ph], spec, n + 1, phase) for n, spec in enumerate(w[key])]
        d["lore"] = w["lore"]
        d["designTag"] = w["tag"]
        d["canon"] = {"source": w["src"], "language": "en", "facts": w["facts"], "notes": w.get("notes", [])}
        validate(d)
        built[wid] = d

    # the signature rule: no two wills share the pair of kinds of a phase, no id twice
    seen = {1: {}, 2: {}}
    ids = {}
    for wid, d in built.items():
        for phase, ph in ((1, "acquire"), (2, "integrate")):
            pair = "+".join(sorted(a["kind"] for a in d[ph]["abilities"]))
            if pair in seen[phase]:
                fail("%s and %s share the %s signature %s" % (wid, seen[phase][pair], ph, pair))
            seen[phase][pair] = wid
            for a in d[ph]["abilities"]:
                if a["id"] in ids and ids[a["id"]] != wid:
                    fail("ability id %s is used by %s and %s" % (a["id"], ids[a["id"]], wid))
                ids[a["id"]] = wid
    for kind in NEW_KINDS:
        if not any(a["kind"] == kind for d in built.values() for ph in ("acquire", "integrate") for a in d[ph]["abilities"]):
            fail("the new kind %s is used by no will" % kind)

    for wid, d in built.items():
        with open(os.path.join(OUT, wid + ".json"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")

    # ------------------------------------------------------------------ the lang fragment: new keys only
    lang = {}
    pre = "anime_multiverse.beastwill."
    for wid, w in C.WILLS.items():
        lang[pre + wid + ".canon_lore"] = w["lore"]
        lang[pre + wid + ".canon_source"] = "Source: " + w["short"] + (". The elder itself is the mod's own design." if w["tag"] == "TBATE-KIN" else "")
        for key in ("A", "I"):
            for spec in w[key]:
                if not spec["keep"]:
                    lang[pre + "ability." + spec["id"]] = spec["name"]
    lang[pre + "msg.milestone"] = "%s: %s"
    for n, t in C.MILESTONE_TEXT.items():
        lang[pre + "milestone.%d" % n] = t
    for k in lang:
        if k in en and en[k] != lang[k]:
            fail("the lang key %s already exists in en_us with another text (a fragment cannot override it)" % k)
    with open(LANG_OUT, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(lang, fh, indent=2, ensure_ascii=False)
        fh.write("\n")

    write_doc(old, built)
    print("wrote %d wills, %d lang keys, the doc" % (len(built), len(lang)))


def phase_rows(o_ph, n_ph):
    """(status, text) rows of one phase: KEPT, or NEW with the REPLACED ability's id."""
    olds = [a for a in o_ph["abilities"]]
    news = [a for a in n_ph["abilities"]]
    gone = [a for a in olds if a["id"] not in [n["id"] for n in news]]
    rows = []
    for a in news:
        if any(a["id"] == x["id"] for x in olds):
            rows.append((a, "KEPT", ""))
        else:
            rep = gone.pop(0) if gone else None
            rows.append((a, "NEW", ("replaces " + rep["id"] + " (" + rep["kind"] + ")") if rep else ""))
    return rows


def stat_text(stats):
    return ", ".join("%s %s%g" % (short_of(s["attribute"]), "+" if s["op"] == "add" else "x", s["amount"]) for s in stats) or "none"


def write_doc(old, built):
    L = []
    w = L.append
    w("# Beast Will canon rework")
    w("")
    w("Every one of the 27 Beast Wills now follows its source: the four TBATE wills follow the TBATE wiki, the others follow their myth or folklore, and the three that have no wiki beast are marked as relatives of a TBATE beast or as design. This page is the record of what each will took from its source, what changed, what it costs a server and what has not been checked.")
    w("")
    w("## How it is switched")
    w("")
    w("- Server config `nusmp-beastwill-server.toml`, group `rules`, key `canonRework` (full path `beastwill.rules.canonRework`), default `true`.")
    w("- `true`: the game reads `data/anime_multiverse/beast_will_canon` (the canon wills), the milestone cooldowns and the fiery-only ignition.")
    w("- `false`: the game reads `data/anime_multiverse/beast_will` exactly as before (the remade wills), with the old ignition rule and no milestone cooldowns. Nothing in that folder was changed.")
    w("- `classicWills = true` still wins over both (it reads `beast_will_classic`).")
    w("- Ids, save data, keywords, drops, EP needs (one exception below), affinities, names and tints are unchanged, so old worlds, cores and progress load as they were. Wills the player holds keep their progress; only the abilities and numbers that the form gives change.")
    w("")
    w("## What is new in the rules")
    w("")
    w("| Rule | What it does | Where |")
    w("|---|---|---|")
    w("| Three new ability kinds | `tail_sweep` (a hit and a push of up to 6 foes round you), `thunder_chain` (a bolt that hops foe to foe, up to 5, each hop keeps 80 %), `devour` (a heavy bite at one foe that heals you 40 % of what it dealt, at most 6 health) | `BeastWillRules.KINDS`, `BeastWillAbilities`, `BeastWillCanon` |")
    w("| Rank progression | the first form unlocks at 10 % assimilation for A, 15 % for S, 20 % for SS beasts; the second form asks 50 % / 60 % / 70 % mastery | `BeastWillCanon.acquireUnlock`, `integrateMastery` |")
    w("| Mastery milestones | at a quarter, a half, three quarters and full mastery a line tells the holder; from half mastery all ability cooldowns are 5 %, 8 % and 10 % shorter (never below the kind's own minimum) | `BeastWillCanon.milestone`, `cooldownFactor`, `BeastWillManager` |")
    w("| Fiery-only ignition | a draconic pulse sets foes alight only for fire wills (affinity with fire, flame, heat, magma, ember, inferno or blaze); earth, wind and stone beasts no longer burn their targets | `BeastWillCanon.ignites` |")
    w("| Distinct identities | no two wills share the pair of ability kinds of a form; a will never has two abilities of one kind in a form (they would share one cooldown); no ability id twice | `BeastWillCanon.signatureProblems` |")
    w("| Readable text | every will has a `canon_lore` and a `canon_source` line (tooltip of its core) | `tools/lang/beastwill_canon.json` |")
    w("")
    w("Source tags in the table: CANON = the wiki of the series says it; TBATE-KIN = a relative of a TBATE beast that the wiki names; MYTH = a myth or folklore page; DESIGN = the mod's own. Any detail that is not in the fetched text is written as design in the will's notes.")
    w("")
    w("## The 27 wills")
    w("")
    for wid, d in built.items():
        o = old[wid]
        wl = C.WILLS[wid]
        w("### %s (`%s`, rank %s, %s)" % (d["name"], wid, d["minRank"], wl["tag"]))
        w("")
        w("- Source: %s" % wl["src"])
        for f in wl["facts"]:
            w("- Canon fact: " + f)
        for n in wl.get("notes", []):
            w("- Note: " + n)
        for x in wl.get("extra", []):
            w("- " + x)
        w("- First form unlocks at %d %% assimilation (was %d %%); second form asks %d %% mastery (was %d %%)." % (round(d["acquire"]["unlockAssimilation"] * 100), round(o["acquire"]["unlockAssimilation"] * 100), round(d["integrate"]["unlockMastery"] * 100), round(o["integrate"]["unlockMastery"] * 100)))
        changes = []
        if d["passive"] != o["passive"]:
            changes.append("passive: %s (was %s)" % (stat_text(d["passive"]), stat_text(o["passive"])))
        for ph in ("acquire", "integrate"):
            if d[ph]["stats"] != o[ph]["stats"]:
                changes.append("%s stats: %s (was %s)" % (ph, stat_text(d[ph]["stats"]), stat_text(o[ph]["stats"])))
            for k in ("drainPct", "strainPerSecond", "regen"):
                if d[ph][k] != o[ph][k]:
                    changes.append("%s %s %g (was %g)" % (ph, k, d[ph][k], o[ph][k]))
        for k in ("minRank", "affinity", "legendary", "assimilationDayCap", "masteryDayCap"):
            if d[k] != o[k]:
                changes.append("%s %s (was %s)" % (k, d[k], o[k]))
        if d["requireEp"] != o["requireEp"]:
            changes.append("requireEp %g (was %g), a design value for the AA class" % (d["requireEp"], o["requireEp"]))
        for c in changes:
            w("- Changed: " + c)
        w("")
        w("| Form | Slot | Ability | Kind | Status | Numbers |")
        w("|---|---|---|---|---|---|")
        for ph, label in (("acquire", "Acquire"), ("integrate", "Integrate")):
            for a, status, what in phase_rows(o[ph], d[ph]):
                nums = "cost %g %%, cd %d t, strain %g, targets %d" % (a["costPct"], a["cooldownTicks"], a["strain"], a["maxTargets"])
                w("| %s | %d | %s (`%s`) | %s | %s %s | %s |" % (label, a["slot"], a["name"], a["id"], a["kind"], status, what, nums))
        w("")
        # the worst case of this will
        uses = 0.0
        pics = 0.0
        tmax = 0
        for ph in ("acquire", "integrate"):
            for a in d[ph]["abilities"]:
                tmax = max(tmax, a["maxTargets"])
        for a in d["integrate"]["abilities"]:
            r = 20.0 / (a["cooldownTicks"] * 0.9)
            uses += r
            pics += r * (2 if a["kind"] in NEW_KINDS else 1)
        w("- Worst case: no entity is spawned; at most %d foes touched per use; both Integrate abilities used on cooldown at full mastery: %.2f uses per second, at most %.2f pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets." % (tmax, uses, pics))
        w("- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.")
        w("")
    w("## Server cost, all together")
    w("")
    w("- No ability spawns an entity, a lightning bolt or a block; the three new kinds only hit entities that are already there, through the same `around`, `cone`, `hit` and `BalanceLaw` helpers as the old kinds.")
    w("- `tail_sweep`: one world query round the caster, at most 6 targets, one picture and one particle burst. `thunder_chain`: one cone query and at most 4 small queries, at most 5 targets, one picture, one burst and short particle lines (4 each). `devour`: one cone query, one target, one picture, one burst, one heal that goes through `BalanceLaw`.")
    w("- Cooldowns are per kind and per will progress as before; the milestone factor only shortens them and never below half of the written value (`BeastWillRules.cooldown`) and never below the kind's own minimum in the data.")
    w("- The milestone line is one chat line per milestone, rate-limited by `BeastWillFeedback`; the first time it is only recorded. Per-holder state is one small map entry per will, dropped with the holder's run state.")
    w("- Loading: 27 small files at data reload, as before.")
    w("")
    w("## Sources and what could not be fetched")
    w("")
    w("- English first (rule 10): TBATE Fandom wiki through its MediaWiki `api.php` (Beast Will, Beast Tamers, Mana Beasts, Elderwood Guardian, Curtis Glayder/Abilities, Arthur Leywin/Abilities, Sylvia, Sylvie Leywin and others) and English Wikipedia through its `api.php` (introductory extracts of the mythology pages named per will).")
    w("- The Japanese wiki was not needed: every page above came back with enough text.")
    w("- Not used: the Hippocampus page was fetched but no will uses it. No other series wiki was queried for this task, because the 27 wills come only from TBATE, myth and folklore.")
    w("- Fenrir comes from the owner's own design document, not from a wiki: no wiki was fetched for it (rule 10), and nothing in its file claims a wiki source.")
    w("- Details that the fetched text does not give are written as design: the kraken's ink, the minotaur's axe quake, the thunder of the chimera, the Gleipnir snare, the sphinx's stilled time, and the three elder beasts (flame serpent, ridge wolf, gale skimmer) that are relatives of TBATE beasts and not wiki creatures.")
    w("")
    w("## Not built, not checked")
    w("")
    w("- Not built: a third phase for the dragon (Unleashed Realmheart) and for Fenrir (Unbound Assimilation), forged vs legacy insight, the Elderwood Guardian's mist illusions, the black-fanged wolf's fire-resistant fur (no allowed attribute), new models or textures (the looks are unchanged), and the separate `BeastSkillRules` skills.")
    w("- Not checked in a game: the three new kinds, the milestone line and the new tooltips were written and covered by unit tests, source-scan tests and the CI self-test `beastcanon`, but not seen on a screen. The build was not compiled in the session that wrote it (no Gradle there); CI is the first compile.")
    w("")
    w("## Tests")
    w("")
    w("- `BeastWillCanonTest` (pure rules), `BeastWillCanonDataTest` (the 27 canon files: validator, signatures, progression, caps, lang), `BeastWillCanonWiringTest` (source scan of the loader, config, abilities, manager and boot), `BeastWillRulesTest` (the kind table).")
    w("- Self-test prefix `beastcanon` (`BeastCanonSelfTests`): `catalog.load`, `rules.bounds`, `abilities.nospawn`.")
    w("- Regenerate everything with `python3 tools/beastwill/canon/build_canon.py`.")
    w("")
    with open(DOC_OUT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(L))


if __name__ == "__main__":
    main()
