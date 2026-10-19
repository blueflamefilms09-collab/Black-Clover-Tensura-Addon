"""Writes everything TEXT and DATA about the 35 weapons of 1.97.0 (wave 71) from tools/relics/new197_roster.py:

    python3 -B tools/relics/new197_gen.py            (idempotent)

  src/main/java/com/newuniverse/nusmp/relics/wave/Wave71.java          the relic definitions (GENERATED: edit the roster, not the Java)
  src/main/java/com/newuniverse/nusmp/relics/wave/Wave71Passives.java  the passive rules table
  tools/lang/new_weapons_197.json                                       name, tooltip and card text of every weapon (flat fragment; the lead merges it into en_us.json)
  src/main/resources/assets/nusmp/weapon_classes.json + tools/relics/weapon_classes.json   a class row for every weapon (appended)
  src/main/resources/data/nusmp/mega/relic_themes.json                  the dungeon road each weapon leads to (appended)

The meshes, skins, icons and item models are built by new197_build.py (new197_legend.py / new197_god.py / new197_genesis.py).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from new197_roster import ROSTER                                    # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave")
RES = os.path.join(ROOT, "src", "main", "resources")

RANGES = {"LEGEND": (130, 175), "GOD": (190, 240), "GENESIS": (260, 350)}
# the same table as relics/GradeDamage.java (the stand-in and the full class agree on the owner's ranges; a test compares the generated numbers with the Java)
FACTORS = {"dagger": 0.00, "knife": 0.00, "wand": 0.05, "rapier": 0.10, "coilwhip": 0.15, "whip": 0.20, "shield": 0.20, "shortsword": 0.30, "sabre": 0.35, "cleaver": 0.40,
           "handaxe": 0.40, "katana": 0.45, "staff": 0.45, "bow": 0.50, "sword": 0.50, "longsword": 0.50, "crossbow": 0.55, "club": 0.55, "spear": 0.55, "trident": 0.55,
           "lance": 0.55, "mace": 0.65, "axe": 0.65, "battleaxe": 0.70, "crescent": 0.70, "hammer": 0.75, "glaive": 0.80, "halberd": 0.85, "dadao": 0.85, "scythe": 0.90,
           "greataxe": 0.90, "greathammer": 1.00, "greatsword": 1.00, "item": 0.50, "handitem": 0.50}

COND_TEXT = {
    "ALWAYS": "on every blow", "LOW_HP_FOE": "against a foe under 30% of its health", "UNDEAD_FIEND": "against the undead, demons and other fiends",
    "WYRM": "against dragons, wyrms, serpents and hydras", "FLYING_FOE": "against a foe in the air", "WET_FOE": "against a foe in water or rain",
    "FIRE_FOE": "against a creature of fire", "DAYLIGHT": "in open daylight", "NIGHT": "at night", "FULL_HP_SELF": "while you are at full health",
    "LOW_HP_SELF": "while you are under 40% of your health", "BOSS": "against a boss", "SNEAKING": "while you sneak", "FALLING": "while you are falling",
    "BIG_FOE": "against a foe with 100 health or more",
}
KIND_TEXT = {"longsword": "longsword", "shortsword": "short sword", "sabre": "sabre", "greatsword": "greatsword", "spear": "spear", "bow": "bow", "whip": "whip", "dagger": "dagger",
             "handaxe": "throwing ring", "greataxe": "great axe", "scythe": "scythe", "staff": "staff", "hammer": "hammer", "katana": "katana", "trident": "trident", "rapier": "rapier",
             "wand": "wand", "shield": "shield", "battleaxe": "battle axe", "crossbow": "crossbow", "glaive": "halberd", "club": "pair of gauntlets"}
GRADE_NAME = {"LEGEND": "Legend", "GOD": "God", "GENESIS": "Genesis"}


def dtype(e):
    return e.get("dtype", e["cls"])


def damage(e):
    lo, hi = RANGES[e["grade"]]
    d = lo + FACTORS.get(dtype(e), 0.5) * (hi - lo)
    return max(lo, min(hi, int(d * 2.0 + 0.5) / 2.0))


def kind_label(e):
    if e["id"] in ("fairy_heart_gauntlets", "fairy_glitter_gauntlets"):
        return "pair of gauntlets"
    if e["id"] == "wavebreaker_chakram" or e["id"] == "cardinal_halo_chakram":
        return "chakram"
    if e["id"].endswith("fangs"):
        return "pair of fang-daggers"
    if e["id"] in ("magic_emperors_black_halberd",):
        return "halberd"
    if e["id"] in ("crimson_lions_flame_lance", "gae_bolg_heartseeker", "gungnir_the_unerring"):
        return "spear"
    if e["id"] in ("minotaur_kings_labrys",):
        return "double axe"
    return KIND_TEXT.get(e["cls"], e["cls"])


def jstr(s):
    return json.dumps(s, ensure_ascii=True)


def const(e):
    return "DMG_" + e["id"].upper()


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


def pct(x):
    return ("%d" % round(x * 100))


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


OPS = {"ADD_VALUE": "ADD_VALUE", "ADD_MULTIPLIED_BASE": "ADD_MULTIPLIED_BASE", "ADD_MULTIPLIED_TOTAL": "ADD_MULTIPLIED_TOTAL"}
ATTR_NAME = {"minecraft:generic.attack_speed": "attack speed", "minecraft:generic.luck": "luck", "minecraft:generic.armor": "armour", "minecraft:generic.armor_toughness": "armour toughness",
             "minecraft:generic.knockback_resistance": "knockback resistance", "minecraft:generic.max_health": "maximum health", "minecraft:generic.movement_speed": "movement speed",
             "minecraft:generic.safe_fall_distance": "safe fall distance", "minecraft:player.entity_interaction_range": "reach", "neoforge:swim_speed": "swim speed",
             "tensura:max_magicule": "maximum magicules", "minecraft:generic.attack_damage": "attack damage", "tensura:magic_cost_multiplier": "magic cost"}


def attr_text(a):
    name, amount, op = a
    nm = ATTR_NAME.get(name, name)
    if op == "ADD_VALUE":
        return "%+g %s" % (amount, nm)
    return "%+d%% %s" % (round(amount * 100), nm)


def standing_text(e):
    bits = [attr_text(a) for a in e["attrs"]]
    bits += ["%d%% less %s damage" % (round(f * 100), t.split(":")[1].replace("is_", "").replace("_", " ")) for t, f in e["resists"]]
    bits += ["the lent skill %s" % s.split(":")[1].replace("_", " ") for s in e["skills"]]
    return ", ".join(bits)


def act_trigger(e, i):
    if e["kind"] != "melee":
        return "sneak + use" if i == 0 else "the worn-relic key"
    return ["use", "sneak + use", "the worn-relic key"][i]


def card_text(e):
    d = damage(e)
    lines = ["%s Grade %s (%s damage, %s attacks a second). Needs attunement." % (GRADE_NAME[e["grade"]], kind_label(e), ("%g" % d), ("%g" % e["speed"])) ]
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


def span(sec):
    return ("%d s" % sec) if sec < 120 else ("%g min" % (sec / 60.0))


def short_desc(e):
    a = e["acts"]
    s = e["lore"] + " " + passive_text(e)[0].upper() + passive_text(e)[1:] + ". "
    s += "Use: " + a[0]["name"] + (", " if len(a) > 2 else " and ") + a[1]["name"]
    if len(a) > 2:
        s += " and " + a[2]["name"]
    s += "."
    return s


# ---------------------------------------------------------------- Java
def act_java(e, i, a):
    D = lambda k: "(float) (%s * %s)" % (const(e), repr(float(k)))
    expr = a["expr"](D)
    body = expr if expr.startswith("ctx ->") else expr
    mana = a["mana"]
    s = '.act(Act.of(%s, %s, %s).cooldown(%d).mana(%d)' % (jstr(a["key"]), jstr(a["name"]), body, a["cd"] * 20, mana)
    if a["sneak"] and e["kind"] == "melee":
        s += ".needsSneak()"
    if a["day"]:
        s += ".usesPerDay(%d)" % a["day"]
    return s + ")"


def def_java(e):
    g = e["grade"]
    L = []
    L.append('        // ---------------------------------------------------------------- %s (%s, %s)' % (e["name"], GRADE_NAME[g], e["src"]))
    head = 'out.accept(RelicDef.of(%s, %s, Grade.%s, RelicSlot.MAIN_HAND).attune().vessel().model3d()' % (jstr(e["id"]), jstr(e["name"]), g)
    if e["kind"] == "bow":
        head += ".bow()"
    elif e["kind"] == "crossbow":
        head += ".crossbow()"
    L.append("        " + head)
    L.append("                .look(Look.%s).palette(0x%06X, 0x%06X, 0x%06X)" % (e["look"], *e["pal"]))
    L.append("                .desc(%s)" % jstr(short_desc(e)))
    L.append('                .passive(Passive.flag("w71_%s"))' % e["id"])
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


def write_wave():
    out = []
    out.append("""package com.newuniverse.nusmp.relics.wave;

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
 * Relics of Many Worlds, wave 71 (1.97.0): 35 weapons of the three highest grades, 13 Legend, 12 God and 10 Genesis, over many weapon types and many worlds (Dungeons and
 * Dragons, Black Clover, Tensura, Fairy Tail, The Rising of the Shield Hero, The Seven Deadly Sins, DanMachi, old myths). GENERATED by tools/relics/new197_gen.py from
 * tools/relics/new197_roster.py: edit the table, not this file. Every weapon is held in the main hand, drawn as its own 3D model (tools/relics/new197_*.py), carries Tensura
 * engravings, a passive ({@link Wave71Passives}, applied by {@link Wave71Events}) and two or three active abilities (the shapes of {@link Wave71Acts}). The base damage of each
 * weapon is {@link GradeDamage#damage}: the owner's table by grade, placed inside the range by the weapon type.
 */
public final class Wave71 {
    private Wave71() {}
""")
    for e in ROSTER:
        out.append('    static final double %s = GradeDamage.damage("%s", "%s");' % (const(e), e["grade"].lower(), dtype(e)))
    out.append("")
    out.append("    /** The ids of this wave, in order (the self-test and the tests read them). */")
    out.append("    public static final String[] IDS = {")
    for i in range(0, len(ROSTER), 3):
        out.append("            " + ", ".join(jstr(e["id"]) for e in ROSTER[i:i + 3]) + ",")
    out.append("    };\n")
    out.append("    public static void defs(Consumer<RelicDef> out) {")
    for e in ROSTER:
        out.append(def_java(e))
        out.append("")
    out.append("    }\n")
    out.append("""    /** Registers the damage rule of the passives and the self-test of this wave (called once from the mod constructor). */
    public static void hooks() {
        Wave71Events.register();
        Wave71SelfTest.register();
    }
}""")
    open(os.path.join(JAVA, "Wave71.java"), "w", newline="\n").write("\n".join(out) + "\n")


def write_passives():
    out = ["""package com.newuniverse.nusmp.relics.wave;

import java.util.Map;

/**
 * The passive rule of each weapon of wave 71. GENERATED by tools/relics/new197_gen.py from tools/relics/new197_roster.py: edit the table, not this file.
 * The rules and their arithmetic are {@link Wave71Rules}; {@link Wave71Events} applies them.
 */
final class Wave71Passives {
    private Wave71Passives() {}

    static final Map<String, Wave71Rules.Rule> RULES = Map.ofEntries("""]
    rows = []
    for e in ROSTER:
        rows.append("            Map.entry(%s, %s)" % (jstr(e["id"]), rule_java(e["rule"])))
    out.append(",\n".join(rows))
    out.append("    );\n}")
    open(os.path.join(JAVA, "Wave71Passives.java"), "w", newline="\n").write("\n".join(out) + "\n")


def write_lang():
    d = {}
    for e in ROSTER:
        d["item.nusmp.%s" % e["id"]] = e["name"]
        d["item.nusmp.%s.desc" % e["id"]] = short_desc(e)
        d["item.nusmp.%s.card" % e["id"]] = card_text(e)
    os.makedirs(os.path.join(ROOT, "tools", "lang"), exist_ok=True)
    with open(os.path.join(ROOT, "tools", "lang", "new_weapons_197.json"), "w", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=True)
        f.write("\n")


def write_classes():
    pa = os.path.join(RES, "assets", "nusmp", "weapon_classes.json")
    pt = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")
    a = json.load(open(pa))
    t = json.load(open(pt))
    for e in ROSTER:
        a["weapons"].setdefault(e["id"], e["cls"])
        t["weapons"].setdefault(e["id"], {"class": e["cls"]})
    with open(pa, "w", newline="\n") as f:
        f.write(json.dumps(a, indent=1, ensure_ascii=True) + "\n")
    with open(pt, "w", newline="\n") as f:
        f.write(json.dumps(t, indent=1, ensure_ascii=True))


def write_themes():
    p = os.path.join(RES, "data", "nusmp", "mega", "relic_themes.json")
    raw = open(p).read()
    d = json.loads(raw)
    for e in ROSTER:
        d["explicit"].setdefault(e["id"], e["theme"])
    indent = 2
    text = json.dumps(d, indent=indent, ensure_ascii=False)
    if raw.endswith("\n"):
        text += "\n"
    open(p, "w", newline="\n").write(text)


def main():
    write_wave()
    write_passives()
    write_lang()
    write_classes()
    write_themes()
    print("wrote Wave71.java, Wave71Passives.java, tools/lang/new_weapons_197.json, the class rows and the theme rows for %d weapons" % len(ROSTER))


if __name__ == "__main__":
    main()
