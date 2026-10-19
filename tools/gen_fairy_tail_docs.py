"""Fairy Tail skills: writes the lang fragment (tools/lang/fairy_tail_skills.json) and the documentation (docs/fairy_tail_skills.md) FROM the spell tables in
src/main/java/com/newuniverse/nusmp/fairytail/FtLine.java and FtForms.java, so the text can never drift from the code:

    python3 -B tools/gen_fairy_tail_docs.py

The lang fragment is flat (key -> English); the owner's merge step folds it into en_us.json.
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "fairytail")


def read(name):
    with open(os.path.join(SRC, name), encoding="utf-8") as f:
        return f.read()


def strings(blob):
    return "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', blob)).replace('\\"', '"')


def parse_lines():
    src = read("FtLine.java")
    lines = []
    for m in re.finditer(r'(?m)^    ([A-Z_]+)\("([a-z_]+)", "([^"]+)", (true|false), FtElement\.([A-Z]+), "([a-z]*)",\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)\)[,;]', src):
        lines.append(dict(const=m.group(1), id=m.group(2), name=m.group(3), ultimate=m.group(4) == "true", element=m.group(5), eat=m.group(6), desc=strings(m.group(7))))
    funcs = {}
    for fm in re.finditer(r'private static List<FtMove> (\w+)\((boolean king)?\) \{(.*?)\n    \}\n', src, re.S):
        moves = []
        for mm in re.finditer(r'l\.add\(m\("([a-z_]+)", "([^"]+)", ([A-Z_]+), (\d+), ([\d.]+), (\d+), ([\d.]+), ([\d.]+), ([\d.]+), (\d+), (\d+)\)(?:\.withForm\(FtForms\.([A-Z_]+)\))?\);', fm.group(3)):
            moves.append(dict(id=mm.group(1), name=mm.group(2), kind=mm.group(3), unlock=int(mm.group(4)), cost=float(mm.group(5)), cd=int(mm.group(6)),
                              dmg=float(mm.group(7)), rng=float(mm.group(8)), rad=float(mm.group(9)), ticks=int(mm.group(10)), count=int(mm.group(11)), form=mm.group(12),
                              king_only=False))
        funcs[fm.group(1)] = (fm.group(2) is not None, fm.group(3), moves)
    return lines, funcs


def moves_for(line_id, funcs):
    name = {"fire_dragon_slayer": "fire", "fire_dragon_king": "fire", "ice_make": "ice", "sky_dragon_slayer": "sky", "lightning_dragon_slayer": "lightning",
            "iron_dragon_slayer": "iron", "crash": "crash", "water_magic": "water"}[line_id]
    has_king, body, moves = funcs[name]
    if has_king and line_id == "fire_dragon_slayer":
        # the three forms sit inside "if (king) { ... }"
        k = body.index("if (king)")
        base_moves = [m for m in moves if m["kind"] != "FORM"]
        return base_moves
    return moves


def parse_forms():
    src = read("FtForms.java")
    out = []
    for m in re.finditer(r'new FtForms\("([a-z_]+)", "([^"]+)", FtElement\.([A-Z]+), ([\d.]+), (\d+), (\d+), (\d+), (\d+), (\d+), ([\d.]+), "([a-z]*)"\)', src):
        out.append(dict(key=m.group(1), name=m.group(2), element=m.group(3), mul=float(m.group(4)), speed=int(m.group(5)), strength=int(m.group(6)), resist=int(m.group(7)),
                        absorb=int(m.group(8)), regen=int(m.group(9)), cut=float(m.group(10)), immune=m.group(11)))
    return out


KIND_TEXT = {
    "BREATH": "a lane in front of you", "CONE": "a close blow in a cone", "DASH": "a rush that hits everything on the way", "RING": "a burst round you",
    "TARGET_AREA": "a burst where you look", "RAIN": "pulses on the point you look at", "BARRAGE": "a volley, one lane after another",
    "COMBO": "a rush to the nearest foe, a chain of blows and a last explosion", "BUFF": "a buff on you", "ENCHANT": "an enchantment on the party",
    "HEAL": "heals and cleanses the party", "SHIELD": "absorption and resistance", "EAT": "eats the element to heal and charge your spells",
    "FORM": "a timed form with armour", "LIFT": "lifts the foes round you and slams them down", "BOMB": "marks the foe you aim at, it explodes a moment later",
    "DISPEL": "strips the good effects from the foes in front of you", "LOCK": "traps the foe you aim at",
}


def main():
    lines, funcs = parse_lines()
    forms = parse_forms()
    lang = {}
    for L in lines:
        lang["nusmp.skill." + L["id"]] = L["name"]
        lang["nusmp.skill." + L["id"] + ".description"] = L["desc"]
        mv = moves_for(L["id"], funcs)
        if L["id"] == "fire_dragon_king":
            mv = moves_for("fire_dragon_slayer", funcs) + [m for m in funcs["fire"][2] if m["kind"] == "FORM"]
        for m in mv:
            lang["nusmp.skill.mode.%s.%s" % (L["id"], m["id"])] = m["name"]
    msgs = {
        "fairytail.no_energy": "Not enough Aura or magicules (need %s).",
        "fairytail.no_target": "No target in reach.", "fairytail.aim": "Aim at a target.",
        "fairytail.no_iron": "You have no iron to eat.", "fairytail.no_flame": "There is no flame to eat.",
        "fairytail.control_cooldown": "Binding spells are still recovering (%s s).",
        "fairytail.locked": "%s is not open yet: master %s more.", "fairytail.cooldown": "%s is recovering.",
        "fairytail.recovering": "This magic is recovering from a fault.",
    }
    for k, v in msgs.items():
        lang["nusmp.skill.msg." + k] = v
    lang["item.nusmp.fairy_tail_skill_book"] = "Fairy Tail Skill Book"
    lang["item.nusmp.fairy_tail_skill_book.of"] = "Skill Book: %s"
    lang["nusmp.fairytail.learned"] = "You learned %s."
    with open(os.path.join(HERE, "lang", "fairy_tail_skills.json"), "w", encoding="utf-8") as f:
        json.dump(dict(sorted(lang.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")

    md = ["# Fairy Tail skills", "",
          "Eight Tensura skills (seven Uniques and the Fire Dragon King Ultimate) built from the Fairy Tail anime and manga, obtained from skill books. They do not use the Skill Tree. "
          "This page is generated by `python3 -B tools/gen_fairy_tail_docs.py` from `fairytail/FtLine.java` and `fairytail/FtForms.java`.", "",
          "## How they work", "",
          "- Each skill is a Tensura **Unique** skill (the Fire Dragon King an **Ultimate**); the spells are its **modes**. Cycle modes with the skill key, cast with the skill key.",
          "- **Mastery** opens the later spells: a spell with `open at 45%` needs the skill to be 45 percent mastered. Casting teaches the skill; a spell that hits something teaches a little more.",
          "- **Cost**: a share of your maximum Aura (at least 12), paid from the Aura first and from the magicules when the Aura is short. A mastered Unique pays 10 percent less, an Ultimate 10 percent more.",
          "- **Damage** goes through the balance law: the base number of the table is multiplied by your EP scale (x1 at 1,000 EP up to x12), by mastery, by the tier (x1.3 mastered Unique, x1.85 Ultimate), by the active form and by the charge of eaten flame or iron (up to +50 percent). Players take at most the PvP hit cap per hit.",
          "- **Cooldown**: mastery shortens it by up to a quarter and a form by another tenth.",
          "- **Forms** (Dragon Force, Lightning Flame Dragon Mode, Fire Dragon King's Mode, Ice Devil's Rage, Sky Dragon's Armor, Lightning Body, Iron Dragon Scales, Shadow Iron Dragon, Water Body, Rain Woman) put a temporary armour on you "
          "(seven geo pieces that forge in and shatter out like the Djinn full equip), give effects and raise the damage of the matching element. Casting the same form again ends it.",
          "- **Passives**: fire cannot hurt a Fire Dragon Slayer (it is eaten: it heals and charges your spells), cold cannot hurt Ice-Make, lightning cannot hurt a Lightning Dragon Slayer; Water Body and Iron Dragon Scales turn aside part of every blow.",
          "- **Evolution**: a mastered Fire Dragon Slayer becomes the **Fire Dragon King** (Ultimate) once your EP reaches the pack's evolution EP (same rule and Voice of the World line as the other evolutions). The King keeps every Slayer spell in the same mode order and appends Dragon Force, Lightning Flame Dragon Mode and Fire Dragon King's Mode.",
          "- **Getting them**: the `fairy_tail_skill_book` item (use it to learn the skill); a chance in the dungeon relic vault; `/multiverse fairytail give <skill> [player]`, `learn` and `list` for operators.", ""]
    for L in lines:
        md += ["## %s (`nusmp:%s`, %s, %s)" % (L["name"], L["id"], "Ultimate" if L["ultimate"] else "Unique", L["element"].title()), "", L["desc"], ""]
        md += ["| # | Spell | Shape | Opens at | Cost (of max Aura) | Cooldown | Base damage | Reach / width |", "|---|---|---|---|---|---|---|---|"]
        mv = moves_for(L["id"], funcs)
        if L["id"] == "fire_dragon_king":
            mv = moves_for("fire_dragon_slayer", funcs) + [m for m in funcs["fire"][2] if m["kind"] == "FORM"]
        for i, m in enumerate(mv):
            reach = ("%g / %g" % (m["rng"], m["rad"])) if m["rng"] or m["rad"] else "-"
            md.append("| %d | %s | %s | %d%% | %g%% | %gs | %s | %s |" % (i, m["name"], KIND_TEXT[m["kind"]], m["unlock"], m["cost"] * 100, m["cd"] / 20.0,
                                                                          ("%g" % m["dmg"]) if m["dmg"] else "-", reach))
        md.append("")
    md += ["## Forms", "", "| Form | Element | Damage x | Speed | Strength | Resistance | Absorption | Regeneration | Blows turned aside | Immune to |", "|---|---|---|---|---|---|---|---|---|---|"]
    for f in forms:
        md.append("| %s | %s | %g | %d | %d | %d | %d | %d | %d%% | %s |" % (f["name"], f["element"].title(), f["mul"], f["speed"], f["strength"], f["resist"], f["absorb"], f["regen"],
                                                                            round(f["cut"] * 100), f["immune"] or "-"))
    md += ["", "## Canon notes", "",
           "Spell names follow the Fairy Tail anime and manga. Where the story gives no exact English name the nearest canon name is used: Gray's Ice-Make Saint is built as a rain of ice sabers on the point you look at, "
           "Wendy's Armor and Vernier as the party enchantments they are in the story, Laxus' Raging Bolt as a long thunder lane, Gildarts' Crash as a touch that also breaks armour, magic and the ground (Magic Crash strips good effects from the foes in front of you; no blocks are destroyed). "
           "Natsu's Lightning Flame Dragon Mode and Fire Dragon King's Mode follow his Dragon Force line of the story.", "",
           "## What is built and what is not", "",
           "Built: the eight skills, the book item, the admin commands, the forms with armour sets (70 geo pieces and ten body shells), the effects (the mod's existing cast / zone / burst trios per element), the self-test `skill.fairytail`. "
           "Not built: summoned constructs as entities (Ice-Make shapes are drawn with the ice effects, not spawned), Time-Magic-grade bespoke effect layers per spell, and real-game verification of any of it.", ""]
    with open(os.path.join(ROOT, "docs", "fairy_tail_skills.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print("lang keys:", len(lang), " lines:", len(lines), " moves:", sum(len(moves_for(L['id'], funcs)) for L in lines))


if __name__ == "__main__":
    main()
