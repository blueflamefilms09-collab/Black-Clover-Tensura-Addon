"""Writes everything TEXT and DATA of Wave102 (release 1.111.0, key `god_special_new`) from tools/relics/new_wave102/roster102.py:

    python3 -B tools/relics/new_wave102/gen102.py            (idempotent)

  src/main/java/com/newuniverse/nusmp/relics/wave/Wave102.java          the 29 relic definitions (GENERATED: edit the roster, not the Java)
  src/main/java/com/newuniverse/nusmp/relics/wave/Wave102Passives.java  the passive rules of the 9 weapons
  tools/lang/god_special_new.json                                       name, short text and card text of every relic (flat fragment; the lead merges it into en_us.json)
  assets/nusmp/weapon_classes.json + tools/relics/weapon_classes.json   a class row for every weapon (appended)
  data/nusmp/mega/relic_themes.json                                     the dungeon road of each God relic (appended)
  docs/relics/god_special_new.md                                        the table of every new relic

The meshes, skins, sprites and item models: build102.py; the worn models: worn102.py (called by gen_worn_all.py); the effect tables of the God gear: fx102.py.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import relic_rows_lib as RL  # noqa: E402
from roster102 import WEAPONS, GEAR, GEAR_META, WORN_GOD  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave")
RES = os.path.join(ROOT, "src", "main", "resources")

RANGES = {"SPECIAL": (35, 55), "GOD": (190, 240)}
FACTORS = {"dagger": 0.00, "knife": 0.00, "wand": 0.05, "rapier": 0.10, "coilwhip": 0.15, "whip": 0.20, "shield": 0.20, "shortsword": 0.30, "sabre": 0.35, "cleaver": 0.40,
           "handaxe": 0.40, "katana": 0.45, "staff": 0.45, "bow": 0.50, "sword": 0.50, "longsword": 0.50, "crossbow": 0.55, "club": 0.55, "spear": 0.55, "trident": 0.55,
           "lance": 0.55, "mace": 0.65, "axe": 0.65, "battleaxe": 0.70, "crescent": 0.70, "hammer": 0.75, "glaive": 0.80, "halberd": 0.85, "dadao": 0.85, "scythe": 0.90,
           "greataxe": 0.90, "greathammer": 1.00, "greatsword": 1.00}
COND_TEXT = {
    "ALWAYS": "on every blow", "LOW_HP_FOE": "against a foe under 30% of its health", "UNDEAD_FIEND": "against the undead, demons and other fiends",
    "WYRM": "against dragons, wyrms, serpents and hydras", "FLYING_FOE": "against a foe in the air", "WET_FOE": "against a foe in water or rain",
    "FIRE_FOE": "against a creature of fire", "DAYLIGHT": "in open daylight", "NIGHT": "at night", "FULL_HP_SELF": "while you are at full health",
    "LOW_HP_SELF": "while you are under 40% of your health", "BOSS": "against a boss", "SNEAKING": "while you sneak", "FALLING": "while you are falling",
    "BIG_FOE": "against a foe with 100 health or more",
}
KIND_TEXT = {"greatsword": "greatsword", "staff": "staff", "dagger": "pair of blades", "bow": "bow", "shield": "shield", "club": "morning star", "shortsword": "short sword"}
GRADE_NAME = {"GOD": "God", "SPECIAL": "Special"}
SLOT_NAME = {"NECK": "necklace", "BACK": "cloak", "BELT": "belt", "HANDS": "gloves", "CHARM": "charm", "HEAD": "headgear", "CHEST": "chest armour", "FEET": "boots"}
ATTR_SHORT = {"luck": "luck", "speed": "movement speed", "hp": "maximum health", "armor": "armour", "tough": "armour toughness", "kb": "knockback resistance", "atk": "attack damage",
              "aspd": "attack speed", "mp": "maximum magicules", "reach": "reach", "sneak": "sneaking speed", "fall": "safe fall distance", "jump": "jump height", "swim": "swim speed"}
RESIST_SHORT = {"fire": "fire", "cold": "cold", "magic": "magic", "proj": "projectile", "expl": "explosion", "bolt": "lightning", "fall": "fall", "wither": "wither", "curse": "curse",
                "dark": "darkness", "holy": "holy", "breath": "dragon breath"}
ATTR_NAME = {"minecraft:generic.attack_speed": "attack speed", "minecraft:generic.luck": "luck", "minecraft:generic.armor": "armour", "minecraft:generic.armor_toughness": "armour toughness",
             "minecraft:generic.knockback_resistance": "knockback resistance", "minecraft:generic.max_health": "maximum health", "minecraft:generic.movement_speed": "movement speed",
             "minecraft:generic.safe_fall_distance": "safe fall distance", "minecraft:player.entity_interaction_range": "reach", "tensura:max_magicule": "maximum magicules",
             "minecraft:generic.attack_damage": "attack damage"}
OPS = {"ADD_VALUE": "ADD_VALUE", "ADD_MULTIPLIED_BASE": "ADD_MULTIPLIED_BASE", "ADD_MULTIPLIED_TOTAL": "ADD_MULTIPLIED_TOTAL"}


def jstr(s):
    return json.dumps(s, ensure_ascii=True)


def pct(x):
    return "%d" % round(x * 100)


def span(sec):
    return ("%d min" % (sec // 60)) if (sec >= 120 and sec % 60 == 0) else ("%d s" % sec)


def dtype(e):
    return e.get("dtype", e["cls"])


def damage(e):
    lo, hi = RANGES[e["grade"]]
    d = lo + FACTORS.get(dtype(e), 0.5) * (hi - lo)
    return max(lo, min(hi, int(d * 2.0 + 0.5) / 2.0))


def const(e):
    return "DMG_" + e["id"].upper()


def kind_label(e):
    return KIND_TEXT.get(e["cls"], e["cls"])


# ================================================================ weapon text
def rule_java(r):
    s = "Wave71Rules.Rule.of(Wave71Rules.Cond.%s, %s)" % (r["cond"], repr(float(r["bonus"])))
    if r.get("chance", 1.0) != 1.0:
        s += ".chance(%s)" % repr(float(r["chance"]))
    if r.get("status"):
        s += ".status(%s, %d, %d)" % (jstr(r["status"][0]), r["status"][1], r["status"][2])
    if r.get("steal"):
        s += ".steal(%s)" % repr(float(r["steal"]))
    if r.get("burn"):
        s += ".burn(%d)" % r["burn"]
    return s


def passive_text(e):
    r = e["rule"]
    parts = []
    if r["bonus"] > 0:
        ch = r.get("chance", 1.0)
        t = "%s%% more damage %s" % (pct(r["bonus"]), COND_TEXT[r["cond"]])
        if ch < 1.0:
            t += " (a %s%% chance on each blow)" % pct(ch)
        parts.append(t)
    if r.get("status"):
        eff = r["status"][0].split(":")[1].replace("_", " ")
        parts.append("%s for %d seconds on the foe%s" % (eff, r["status"][1], "" if r["cond"] == "ALWAYS" else " " + COND_TEXT[r["cond"]]))
    if r.get("burn"):
        parts.append("sets the foe alight for %d seconds" % r["burn"])
    if r.get("steal"):
        parts.append("%s%% of the damage comes back to you as life" % pct(r["steal"]))
    return "; ".join(parts)


def attr_text(a):
    name, amount, op = a
    nm = ATTR_NAME.get(name, name)
    return ("%+g %s" % (amount, nm)) if op == "ADD_VALUE" else ("%+d%% %s" % (round(amount * 100), nm))


def standing_text(e):
    bits = [attr_text(a) for a in e["attrs"]]
    bits += ["%d%% less %s damage" % (round(f * 100), t.split(":")[1].replace("is_", "").replace("_", " ")) for t, f in e["resists"]]
    bits += ["the lent skill %s" % s.split(":")[1].replace("_", " ") for s in e["skills"]]
    return ", ".join(bits)


def act_trigger(e, i):
    if e["kind"] == "bow":
        return "sneak + use" if i == 0 else "the worn-relic key"
    return ["use", "sneak + use", "the worn-relic key"][i]


def weapon_card(e):
    d = damage(e)
    att = " Needs attunement." if e["grade"] == "GOD" else ""
    lines = ["%s Grade %s (%g damage, %g attacks a second).%s" % (GRADE_NAME[e["grade"]], kind_label(e), d, e["speed"], att)]
    lines.append("Passive: " + passive_text(e) + ".")
    st = standing_text(e)
    if st:
        lines.append("While held: " + st + ".")
    for i, a in enumerate(e["acts"]):
        bits = ["cooldown %s" % span(a["cd"]), "%d%% magicules" % a["mana"]]
        if a["day"]:
            bits.append("%d a day" % a["day"])
        lines.append("%s (%s; %s): %s" % (a["name"], act_trigger(e, i), ", ".join(bits), a["text"]))
    lines.append("Tensura engravings: " + ", ".join(x.split("@")[0].replace("_", " ") for x in e["engr"]) + ".")
    return "\n".join(lines)


def weapon_desc(e):
    a = e["acts"]
    p = passive_text(e)
    s = e["lore"] + " " + p[0].upper() + p[1:] + ". Use: " + a[0]["name"] + (", " if len(a) > 2 else " and ") + a[1]["name"] if len(a) > 1 else e["lore"]
    if len(a) > 2:
        s += " and " + a[2]["name"]
    return s + "."


# ================================================================ gear text
def passive_text_gear(tok):
    p = tok.split(":")
    if p[0] == "a":
        import re
        m = re.match(r"^(\+|\*\*|\*)?(-?[0-9.]+)$", p[2])
        op, amt = (m.group(1) or "+"), float(m.group(2))
        nm = ATTR_SHORT[p[1]]
        return ("%+g %s" % (amt, nm)) if op == "+" else ("%+d%% %s" % (round(amt * 100), nm))
    if p[0] == "r":
        return "%d%% less %s damage" % (round(float(p[2]) * 100), RESIST_SHORT[p[1]])
    if p[0] == "e":
        return "the standing effect %s" % p[1].replace("_", " ")
    if p[0] == "s":
        return "the lent skill %s" % p[1].replace("_", " ")
    raise ValueError(tok)


def eff_text(e):
    return e.replace("_", " ")


def spec_text(spec):
    out = []
    for s in spec.split("+"):
        p = s.split(":")
        k = p[0]
        if k == "heal": out.append("you are healed %g" % float(p[1]))
        elif k == "regen": out.append("Regeneration %d for %s s" % (int(p[2]) + 1, p[1]))
        elif k == "buff": out.append("%s %s for %s s" % (eff_text(p[1]).title(), ("level %d" % (int(p[3]) + 1)) if int(p[3]) > 0 else "I", p[2]))
        elif k == "shield": out.append("a shield of %g absorption for %s s" % (float(p[1]), p[2]))
        elif k == "aoe": out.append("%g %s damage to every foe within %g blocks%s" % (float(p[2]), p[3] if p[3] != "hit" else "melee", float(p[1]),
                                                                                    "" if p[4] == "-" else " and %s for %s s" % (eff_text(p[4]), p[5])))
        elif k == "bolt": out.append("a bolt of %g %s damage at the foe in front within %g blocks%s" % (float(p[2]), p[3] if p[3] != "hit" else "melee", float(p[1]),
                                                                                                        "" if p[4] == "-" else " and %s for %s s" % (eff_text(p[4]), p[5])))
        elif k == "dash": out.append("a dash of %g blocks" % float(p[1]))
        elif k == "blink": out.append("a blink of up to %g blocks" % float(p[1]))
        elif k == "cleanse": out.append("every harmful effect is taken off you")
        elif k == "allies": out.append("you and allies within %g blocks gain %s for %s s" % (float(p[1]), eff_text(p[2]).title(), p[3]))
        elif k == "mark": out.append("every foe within %g blocks glows for %s s" % (float(p[1]), p[2]))
        elif k == "second": out.append("you recover %d%% of the health you are missing and resist for %s s" % (round(float(p[1]) * 100), p[2]))
        elif k == "gale": out.append("foes within %g blocks are thrown back" % float(p[1]))
        elif k == "pull": out.append("foes within %g blocks are pulled in" % float(p[1]))
        else: raise ValueError(spec)
    t = ", ".join(out)
    return t[0].upper() + t[1:] + "."


def gear_trigger(i):
    return ["use", "sneak + use", "the worn-relic key"][min(i, 2)] if True else ""


def gear_card(r):
    grade = GRADE_NAME[r["grade"]]
    att = " Needs attunement." if r["att"] else ""
    lines = ["%s Grade %s.%s" % (grade, SLOT_NAME[r["slot"]], att), r["desc"]]
    toks = r["passives"].split()
    if toks:
        lines.append("While worn: " + ", ".join(passive_text_gear(t) for t in toks) + ".")
    for i, a in enumerate(r["acts"]):
        bits = ["cooldown %s" % span(a[1]), "%d%% magicules" % a[2]]
        if len(a) > 5 and a[5]:
            bits.append("%d a day" % a[5])
        lines.append("%s (%s; %s): %s" % (a[0], ["use", "sneak + use", "the worn-relic key"][min(i, 2)], ", ".join(bits), spec_text(a[3])))
    return "\n".join(lines)


def gear_desc(r):
    names = [a[0] for a in r["acts"]]
    tail = "Use: " + (", ".join(names[:-1]) + " and " + names[-1] if len(names) > 1 else names[0]) + "."
    return r["desc"] + " " + tail


# ================================================================ Java
def act_java(e, i, a):
    D = lambda k: "(float) (%s * %s)" % (const(e), repr(float(k)))
    s = '.act(Act.of(%s, %s, %s).cooldown(%d).mana(%d)' % (jstr(a["key"]), jstr(a["name"]), a["expr"](D), a["cd"] * 20, a["mana"])
    if a["sneak"] and e["kind"] != "bow":
        s += ".needsSneak()"
    if a["day"]:
        s += ".usesPerDay(%d)" % a["day"]
    return s + ")"


def weapon_java(e):
    g = e["grade"]
    L = ["        // ---------------------------------------------------------------- %s (%s, %s)" % (e["name"], GRADE_NAME[g], e["src"])]
    head = "out.accept(RelicDef.of(%s, %s, Grade.%s, RelicSlot.MAIN_HAND)" % (jstr(e["id"]), jstr(e["name"]), g)
    if g == "GOD":
        head += ".attune().vessel()"
    head += ".model3d()"
    if e["kind"] == "bow":
        head += ".bow()"
    L.append("        " + head)
    L.append("                .look(Look.%s).palette(0x%06X, 0x%06X, 0x%06X)" % (e["look"], *e["pal"]))
    L.append("                .desc(%s)" % jstr(weapon_desc(e)))
    L.append('                .passive(Passive.flag("w102_%s"))' % e["id"])
    for n, amt, op in e["attrs"]:
        L.append("                .passive(Passive.attr(%s, %s, Operation.%s))" % (jstr(n), repr(float(amt)), OPS[op]))
    for t, f in e["resists"]:
        L.append("                .passive(Passive.resist(%s, %s))" % (jstr(t), repr(float(f))))
    for s in e["skills"]:
        L.append("                .passive(Passive.skill(%s))" % jstr(s))
    for i, a in enumerate(e["acts"]):
        L.append("                " + act_java(e, i, a))
    L.append("                .melee(%s, %s)" % (const(e), repr(float(e["speed"]))))
    L.append("                .engrave(%s));" % ", ".join(jstr(x) for x in e["engr"]))
    return "\n".join(L)


def hexc(c):
    return "0x" + c.lstrip("#").upper()


def gear_java(r):
    src = GEAR_META[r["id"]][0]
    chain = 'out.accept(RelicDef.of("%s", "%s", Grade.%s, RelicSlot.%s)' % (r["id"], RL.jesc(r["name"]), r["grade"], r["slot"])
    if r["att"]:
        chain += ".attune()"
    chain += "\n                .palette(%s, %s, %s).look(Look.%s)" % (hexc(r["pal"][0]), hexc(r["pal"][1]), hexc(r["pal"][2]), r["look"])
    chain += '\n                .desc("%s")' % RL.jesc(gear_desc(r))
    for tok in r["passives"].split():
        chain += "\n                .passive(%s)" % RL.passive_java(tok)
    for i, a in enumerate(r["acts"]):
        chain += "\n                .act(%s)" % RL.act_java(a, i)
    return "        // ---------------------------------------------------------------- %s (%s, %s)\n        %s);" % (r["name"], GRADE_NAME[r["grade"]], src, chain)


def all_ids():
    return [e["id"] for e in WEAPONS] + [r["id"] for r in GEAR]


def write_wave():
    out = ["""package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.GradeDamage;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 102 (1.111.0, key god_special_new): 29 NEW relics to balance the grade pyramid, 9 of God grade (five weapons: a greatsword, a staff, a pair of blades, a bow and a
 * shield, and four pieces of gear: chest armour, a hat, two charms) and 20 of Special grade (four weapons and sixteen pieces of gear over every slot), from many worlds (Bleach, Dragon Ball, Attack
 * on Titan, Fate, The Legend of Zelda, Naruto, One Piece, Hunter x Hunter, JoJo and for Special Black Clover, Dr. Stone, Demon Slayer, Re:Zero, KonoSuba, Sword Art Online and more).
 * GENERATED by tools/relics/new_wave102/gen102.py from tools/relics/new_wave102/roster102.py: edit the table, not this file. The weapons are held in the main hand as their own 3D models, carry Tensura
 * engravings, a passive ({@link Wave102Passives}, applied by {@link Wave102Events}) and their acts are the shapes of {@link Wave71Acts}; the gear uses the shapes of {@link Wave31Acts}.
 * The base damage of each weapon is {@link GradeDamage#damage}: the owner's table by grade (Special 35-55, God 190-240), placed inside the range by the weapon type.
 */
public final class Wave102 {
    private Wave102() {}
"""]
    for e in WEAPONS:
        out.append('    static final double %s = GradeDamage.damage("%s", "%s");' % (const(e), e["grade"].lower(), dtype(e)))
    out.append("")
    out.append("    /** The ids of this wave: the nine God relics, then the twenty Special relics (the self-test and the tests read them). */")
    ids = [e["id"] for e in WEAPONS if e["grade"] == "GOD"] + [r["id"] for r in GEAR if r["grade"] == "GOD"] + [e["id"] for e in WEAPONS if e["grade"] == "SPECIAL"] + [r["id"] for r in GEAR if r["grade"] == "SPECIAL"]
    out.append("    public static final String[] IDS = {")
    for i in range(0, len(ids), 3):
        out.append("            " + ", ".join(jstr(x) for x in ids[i:i + 3]) + ",")
    out.append("    };\n")
    out.append("    public static void defs(Consumer<RelicDef> out) {")
    for e in WEAPONS:
        out.append(weapon_java(e))
        out.append("")
    for r in GEAR:
        out.append(gear_java(r))
        out.append("")
    out.append("    }\n")
    out.append("""    /** Registers the damage rule of the weapons' passives and the self-test of this wave (called once from RelicCatalog.hooks). */
    public static void hooks() {
        Wave102Events.register();
        Wave102SelfTest.register();
    }
}""")
    open(os.path.join(JAVA, "Wave102.java"), "w", newline="\n").write("\n".join(out) + "\n")
    return ids


def write_passives():
    out = ["""package com.newuniverse.nusmp.relics.wave;

import java.util.Map;

/**
 * The passive rule of each weapon of wave 102. GENERATED by tools/relics/new_wave102/gen102.py from tools/relics/new_wave102/roster102.py: edit the table, not this file.
 * The rules and their arithmetic are {@link Wave71Rules} (shared with wave 71); {@link Wave102Events} applies them.
 */
final class Wave102Passives {
    private Wave102Passives() {}

    static final Map<String, Wave71Rules.Rule> RULES = Map.ofEntries("""]
    out.append(",\n".join("            Map.entry(%s, %s)" % (jstr(e["id"]), rule_java(e["rule"])) for e in WEAPONS))
    out.append("    );\n}")
    open(os.path.join(JAVA, "Wave102Passives.java"), "w", newline="\n").write("\n".join(out) + "\n")


# ================================================================ data
def write_lang():
    d = {}
    for e in WEAPONS:
        d["item.nusmp.%s" % e["id"]] = e["name"]
        d["item.nusmp.%s.desc" % e["id"]] = weapon_desc(e)
        d["item.nusmp.%s.card" % e["id"]] = weapon_card(e)
    for r in GEAR:
        d["item.nusmp.%s" % r["id"]] = r["name"]
        d["item.nusmp.%s.desc" % r["id"]] = gear_desc(r)
        d["item.nusmp.%s.card" % r["id"]] = gear_card(r)
    os.makedirs(os.path.join(ROOT, "tools", "lang"), exist_ok=True)
    with open(os.path.join(ROOT, "tools", "lang", "god_special_new.json"), "w", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=True)
        f.write("\n")


def patch_json(path, mutate, **dump):
    raw = open(path, encoding="utf-8").read()
    d = json.loads(raw)
    mutate(d)
    text = json.dumps(d, **dump)
    if raw.endswith("\n"):
        text += "\n"
    if text != raw:
        open(path, "w", newline="\n", encoding="utf-8").write(text)


def write_classes():
    pa = os.path.join(RES, "assets", "nusmp", "weapon_classes.json")
    pt = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")

    def a(d):
        for e in WEAPONS:
            d["weapons"].setdefault(e["id"], e["cls"])

    def t(d):
        for e in WEAPONS:
            d["weapons"].setdefault(e["id"], {"class": e["cls"]})
    patch_json(pa, a, indent=1, ensure_ascii=True)
    patch_json(pt, t, indent=2, ensure_ascii=True)


def write_themes():
    p = os.path.join(RES, "data", "nusmp", "mega", "relic_themes.json")

    def m(d):
        for e in WEAPONS:
            if e["grade"] == "GOD":
                d["explicit"].setdefault(e["id"], e["theme"])
        for r in GEAR:
            th = GEAR_META[r["id"]][1]
            if r["grade"] == "GOD" and th:
                d["explicit"].setdefault(r["id"], th)
    patch_json(p, m, indent=2, ensure_ascii=False)


def acts_summary_weapon(e):
    return "; ".join("%s (%s, %s, %d%%%s)" % (a["name"], act_trigger(e, i), span(a["cd"]), a["mana"], (", %d/day" % a["day"]) if a["day"] else "") for i, a in enumerate(e["acts"]))


def acts_summary_gear(r):
    return "; ".join("%s (%s, %s, %d%%%s)" % (a[0], ["use", "sneak + use", "the worn-relic key"][min(i, 2)], span(a[1]), a[2], (", %d/day" % a[5]) if len(a) > 5 and a[5] else "") for i, a in enumerate(r["acts"]))


def write_doc():
    L = []
    L.append("# God and Special grade relics, wave 102 (release 1.111.0): 9 God + 20 Special, all NEW\n")
    L.append("Owner request: \"JUST ADD MORE TO balance IT OUT\": the relics of the mod were top-heavy (a God total of 51 against a Normal total of 9), so this wave ADDS 9 God grade and 20 Special "
             "grade relics next to everything that exists. **Every relic here is NEW: nothing existing was regraded, replaced, moved or removed** (CLAUDE.md rules 2 and 5; the table of "
             "shared-file edits is at the end). The roster `tools/relics/new_wave102/roster102.py` is the source: edit it and rerun the tools, not the generated files.\n")
    L.append("## NEW / REPLACED / KEPT\n")
    L.append("| | |\n|---|---|\n| NEW | all 29 relics below (9 God, 20 Special), their items, 3D / worn models, sprites, lang fragment, tests, tools |\n| REPLACED | nothing |\n| KEPT | every existing relic, grade, id, model and table; relics of the existing waves that look misgraded are only listed in section 6, not moved |\n")
    for g in ("GOD", "SPECIAL"):
        lo, hi = RANGES[g]
        L.append("## %s grade (%d relics; weapon damage %d-%d)\n" % (GRADE_NAME[g], len([e for e in WEAPONS if e["grade"] == g]) + len([r for r in GEAR if r["grade"] == g]), lo, hi))
        L.append("### Weapons\n")
        L.append("| id | name | franchise | type (class) | slot | damage / speed | passive | acts | Tensura engravings |")
        L.append("|---|---|---|---|---|---|---|---|---|")
        for e in [x for x in WEAPONS if x["grade"] == g]:
            L.append("| `%s` | %s | %s | %s (%s) | main hand | %g / %g | %s | %s | %s |" % (e["id"], e["name"], e["src"], kind_label(e), e["cls"], damage(e), e["speed"], passive_text(e), acts_summary_weapon(e),
                                                                                           ", ".join(x.replace("@", " L") for x in e["engr"])))
        L.append("")
        L.append("### Gear\n")
        L.append("| id | name | franchise | slot (look) | passives | acts |")
        L.append("|---|---|---|---|---|---|")
        for r in [x for x in GEAR if x["grade"] == g]:
            L.append("| `%s` | %s | %s | %s (%s) | %s | %s |" % (r["id"], r["name"], GEAR_META[r["id"]][0], r["slot"], r["look"], ", ".join(passive_text_gear(t) for t in r["passives"].split()), acts_summary_gear(r)))
        L.append("")
    L.append("## 3. What each looks like\n")
    L.append("**Weapons** (`tools/relics/new_wave102/weapons102.py`, the 1.97.0 weapon kit `new197_*.py`): each is a real baked mesh with a painted skin and a second additive glow skin, a 16 x 16 sprite rendered "
             "from the mesh for the inventory, and the item json that hands the drawing to the weapon renderer (the bow has its draw states, the shield is framed like vanilla's).\n")
    for e in WEAPONS:
        L.append("* **%s** (`%s`): %s" % (e["name"], e["id"], e["doc"]))
    L.append("")
    L.append("**God gear** (`worn102.py`, the worn relic library): each has its own multi-part signature model with emissive and pulsing glow parts and animated groups, a table of nine ambient particle "
             "layers and a visual for each act (`assets/nusmp/visual_upgrade/<id>.json`, `fx102.py`; every particle is one of the mod's own).\n")
    for rid, txt in (("perfect_susanoo_plate", "a violet plated cuirass with a glowing ribcage over chest and back, a spiked gorget with a sigil, tassets, horned layered pauldrons and chakra motes circling (3 pieces, 2600 vertices)."),
                     ("joyboys_straw_hat", "a woven wide-brim straw hat with a red band, knot and cord, and behind it a sun halo with twelve turning rays and sparks drifting round the head."),
                     ("greed_island_binder", "a book on a chain at the hip: covers with gilt corners, a latch and a glowing emblem, six cards circling it."),
                     ("requiem_ladybug_brooch", "a gold ladybug on the chest: split shell, six black spots, head and antennae, a glowing eye, an arrow-shaped pin, a turning rune ring and circling motes.")):
        L.append("* **%s** (`%s`): %s" % (next(r["name"] for r in GEAR if r["id"] == rid), rid, txt))
    L.append("")
    L.append("**Special gear** (16): the look of its slot (the shared worn model for the slot and look) plus its own small ornament `uq_<id>` of the generated waves (`gen_worn_unique.py`: two or three motifs "
             "picked and sized by the id), its own palette and its own sprite, so no two look alike. Held, the relic is shown as its worn model (the same route the generated waves 31 to 79 use).\n")
    L.append("## 4. How they work\n")
    L.append("* Weapons: the acts are the shapes of `Wave71Acts` (cone, line, nova, rush, blink strike, execution, ward, rally, pull, volley, storm, bolster), run inside `SafeCast.tick`, take foes only from "
             "`Fx.foesAround`, deal every blow through `Fx.damage` / `Fx.damageHeavy` (the balance law) and are multiples of the weapon's own base damage (`DMG_<ID> * k`). Each weapon's passive is a "
             "`Wave71Rules.Rule` in `Wave102Passives`, applied by `Wave102Events` on the blow being dealt (the arrow for the bow), only while the weapon is in the main hand and its flag is on "
             "(attuned for God). The shield raises like vanilla's (`ShieldSet`, `<id>_blocking.json`).\n"
             "* Gear: attribute / resistance / lent-skill passives and acts from the vocabulary of `Wave31Acts` (heal, regeneration, buff, shield, area damage, bolt, dash, blink, cleanse, allies, mark, second "
             "wind, gale, pull). God gear needs attunement; Special never does (`AttuneRules`). Nothing here touches hunger or exhaustion.\n"
             "* Loot: by grade through the existing relic pools (vault, pedestals, the Crucible, the God of Order's ledger), so the new relics enter every source that rolls God and Special. Each God relic is also "
             "placed on a dungeon road in `data/nusmp/mega/relic_themes.json`.\n"
             "* Better Combat: a file in `data/nusmp/weapon_attributes/<id>.json` for each melee weapon (the bow and the shield have none).\n"
             "* Everything is server-side logic (rule 9): the client only draws; no new packet; per-player state is the engine's own; effects go through the existing particle and effect budgets.\n")
    L.append("## 5. Tests and tools\n")
    L.append("JUnit `relics/wave/Wave102DefsTest` (exactly 9 God and 20 Special, none of the ids in another wave, every weapon's damage inside its grade's range, engravings from the Tensura list, acts, the "
             "lang fragment, the files of every relic, the tables of the God gear) and the existing `RelicRenderCoverageTest`, `RelicHandCoverageTest`, `WornCoverageTest`, `GradeTest`, "
             "`RelicDefTest`, `NoHungerDrainTest`, `ShieldClassesTest`, `WeaponMeshesTest` ... pass with the new relics. In-game self-test `relic.god_special_new` (registered by `Wave102.hooks`).\n")
    L.append("```\npython3 -B tools/relics/new_wave102/gen102.py          # Wave102.java, Wave102Passives.java, tools/lang/god_special_new.json, class rows, theme rows, this page\n"
             "python3 -B tools/relics/new_wave102/build102.py all    # the 9 weapon meshes / skins / sprites / item models, the 20 gear sprites / item models\n"
             "python3 -B tools/relics/new_wave102/fx102.py           # the four effect tables of the God gear\n"
             "python3 -B tools/relics/gen_worn_all.py                # the worn models + catalog (calls worn102.py through two hook lines)\n"
             "python3 -B tools/relics/new_wave102/presets102.py      # the first-person display of the long weapons\n"
             "python3 -B tools/relics/bow_vanilla.py && python3 -B tools/gen_shield_models.py && python3 -B tools/bettercombat/inventory.py && python3 -B tools/bettercombat/gen_attributes.py && python3 -B tools/relics/gen_curios_tags.py\n```\n")
    L.append("## 6. Existing relics that look misgraded (reported, NOT moved)\n")
    L.append("None were moved or regraded (the owner's rule: do not move relics around unless they clearly belong in another rank). A quick pass over the 653 existing relics found only two that the "
             "owner may want to look at; both are opinions, neither was touched: `the_mad_kings_crown` (Special: a ruler's crown whose lore is God-flavoured, with the effects of its grade) and "
             "`mind_stone` (Legend: one of the six cosmic stones, the other stones of that set are graded by their own power).\n")
    L.append("Previews (a software rasteriser, not the game): `docs/previews/god_special_new/sprites.png` (the 29 sprites), `models/<id>.png` (the nine weapon meshes), `worn_*.png` (the four God gear models "
             "beside a plain figure).\n")
    L.append("## 7. Edits to files that already existed (all additions)\n")
    L.append("* `relics/RelicCatalog.java`: one `guard(\"Wave102\", ...)` line in `init()` and one in `hooks()` (plus the import).\n"
             "* `visual/shields/ShieldSet.java` (the id of the new shield appended), `tools/gen_shield_models.py` (the same id).\n"
             "* `assets/nusmp/weapon_classes.json`, `tools/relics/weapon_classes.json`: 9 class rows appended. `data/nusmp/mega/relic_themes.json`: 9 God rows appended.\n"
             "* `tools/relics/gen_worn_all.py`: two append-only hook lines (`worn102.register`, `worn102.catalog`); `assets/nusmp/relic_worn/catalog.txt`: 24 lines appended by that generator.\n"
             "* `data/curios/tags/item/{necklace,back,belt,hands,charm}.json`: the new Curios relics appended by `gen_curios_tags.py`.\n"
             "* `tools/bettercombat/manifest.json` and the Better Combat attribute files, `tools/relics/display_presets_manifest.json`: rows for the new melee weapons.\n"
             "* Not touched, as asked: the version, the changelog, the README, LIBRARIES, `en_us.json` (the lead merges `tools/lang/god_special_new.json`), `.github`, the Time Magic VFX.\n")
    L.append("## 8. What could not be verified\n")
    L.append("Nothing can be run in a game here. Unseen in the game: every model, skin, glow map and sprite (the previews are a software rasteriser), the held pose and size of each weapon, the bow's draw "
             "pose, the shield's raised pose, the worn models on a real body (position, clipping, the ensemble of the Susanoo plate sits exactly on its 2600-vertex cap), the particle layers and act visuals, "
             "the act numbers against a real boss, the passive hooks, the loot rolls and the dungeon roads. The Java was type-checked against API stubs (no real compile) and the JUnit tests ran in a shim without "
             "Minecraft. The Special gear is held as its worn model (no separate hand-only model). The shared worn atlas was not regenerated (this machine's image libraries paint it slightly differently), the "
             "committed atlas is used as it is.\n")
    os.makedirs(os.path.join(ROOT, "docs", "relics"), exist_ok=True)
    with open(os.path.join(ROOT, "docs", "relics", "god_special_new.md"), "w", newline="\n") as f:
        f.write("\n".join(L))


def main():
    ids = write_wave()
    write_passives()
    write_lang()
    write_classes()
    write_themes()
    write_doc()
    print("wrote Wave102.java (%d relics), Wave102Passives.java, the lang fragment, class rows, theme rows and the doc" % len(ids))


if __name__ == "__main__":
    main()
