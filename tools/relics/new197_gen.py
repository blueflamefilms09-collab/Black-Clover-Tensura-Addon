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
    if e["id"] == "tidebreaker_chakram" or e["id"] == "cardinal_halo_chakram":
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


def write_doc():
    L = []
    L.append("# New weapons of 1.97.0: 13 Legend, 12 God and 10 Genesis grade weapons (wave 71)\n")
    L.append("Owner request: \"add more weapons to the legend, God, and Genesis grade\". 35 weapons were added over many weapon types and many worlds (Dungeons and Dragons artifacts, "
             "Black Clover, Tensura, Fairy Tail, The Rising of the Shield Hero, The Seven Deadly Sins, DanMachi, old myths), each a full relic of the Relics of Many Worlds "
             "(`docs/relic_format.md`). **Everything here is NEW: no existing weapon, model, skill, act or id was removed or changed** (CLAUDE.md rules 2 and 5; section 7 lists the "
             "additions to shared files). The table and the generators are the source: edit `tools/relics/new197_roster.py` and rerun the tools, not the generated files.\n")
    L.append("## 1. The weapons\n")
    L.append("Damage is the owner's table (`relics/GradeDamage`: normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, genesis 260-350), the place "
             "inside the range chosen by the weapon type. Speed is attacks a second. A weapon's acts are the first (use, or sneak + use for a bow), the second (sneak + use; "
             "the worn-relic key for a bow) and the third (the worn-relic key). Cost is a share of the magicules (then aura, as every relic pays).\n")
    for g in ("LEGEND", "GOD", "GENESIS"):
        rows = [e for e in ROSTER if e["grade"] == g]
        lo, hi = RANGES[g]
        L.append("### %s grade (%d weapons, damage %d-%d)\n" % (GRADE_NAME[g], len(rows), lo, hi))
        L.append("| id | name | type (class) | damage / speed | passive | acts | Tensura engravings | road |")
        L.append("|---|---|---|---|---|---|---|---|")
        for e in rows:
            acts = "; ".join("%s (%s, %s, %d%%%s)" % (a["name"], act_trigger(e, i), span(a["cd"]), a["mana"], (", %d/day" % a["day"]) if a["day"] else "") for i, a in enumerate(e["acts"]))
            L.append("| `%s` | %s (%s) | %s (%s) | %g / %g | %s | %s | %s | %s |" % (e["id"], e["name"], e["src"], kind_label(e), e["cls"], damage(e), e["speed"], passive_text(e), acts,
                                                                           ", ".join(x.replace("@", " L") for x in e["engr"]), e["theme"]))
        L.append("")
    L.append("## 2. What each weapon looks like (3D model in hand, built by `tools/relics/new197_*.py`)\n")
    for e in ROSTER:
        L.append("* **%s** (`%s`): %s" % (e["name"], e["id"], e["doc"]))
    L.append("")
    L.append("Every weapon is a real baked mesh (8 to 17 named parts, 700 to 3200 quads) with a painted skin and a second additive glow skin; the Genesis weapons that carry a void part "
             "(Blackrazor, the Void Origin Katana, the Magic Emperor's Black Halberd, the Starsplitter Hammer) draw the dimensional void of the Genesis Demon-Slayer through the metal. "
             "The inventory icon is a new 16 x 16 sprite rendered from the finished mesh (outline, half-strength glow). The two bows and the crossbow have their draw states "
             "(`_pull1..3`, `_charged`); the bows lie on vanilla's diagonal with vanilla's display (`bow_vanilla.py`); the shield is placed and held like the Aegis of the Abyss. "
             "Previews: `docs/previews/new197/` (`sprites.png`, `models_LEGEND.png`, `models_GOD.png`, `models_GENESIS.png`, `held_view_sample.png`).\n")
    L.append("## 3. How the abilities work\n")
    L.append("Each act is one SHAPE of `Wave71Acts` (a cone cleave, a piercing line, a nova, a blink strike, an execution, a chain, a ward, a rally, a drain, a pull, a volley, a rush, a "
             "storm, a brand, a self buff). Every body runs inside `SafeCast.tick` (a fault is logged once, never thrown into the server tick), takes its foes only from `Fx.foesAround` "
             "and `Wave20Util` (hostile creatures, never allies, pets, summons, creative or spectator players), deals every blow through `Fx.damage` / `Fx.damageHeavy` "
             "(`BalanceLaw`: EP scaling, the cap on players, the shortened effect on bosses), clamps every radius, count and duration, and draws its effect with one of the mod's "
             "`VfxShape`s plus Rukh particles. An act's damage is a multiple of the weapon's own base damage (`DMG_<ID> * k`), so it follows the owner's table. Costs and "
             "cooldowns are the engine's (`Act.cooldown`, `.mana`, `.usesPerDay`, tiers and attunement as for every relic).\n")
    L.append("The passive of each weapon is one `Wave71Rules.Rule` (`Wave71Passives`, applied by `Wave71Events` on the blow being dealt, melee for a melee weapon and the loosed arrow or "
             "bolt for a bow or crossbow): a damage bonus under a condition (a foe low on health, an undead or fiend, a dragon, a flier, a wet foe, daylight, night, a full-health or "
             "wounded wielder, a boss, sneaking, falling, a big foe), an optional chance, a status on the foe, life stolen (capped) and a burn. The standing passives (attributes, "
             "resistances, lent Tensura skills) are the ordinary `Passive` kinds.\n")
    L.append("## 4. Registration, loot and Better Combat\n")
    L.append("* Registered by `relics/wave/Wave71` (found by `WaveRegistry`, which scans waves 8 to 80): the item, the creative tab entry, `/multiverse relic give <id>`, the tooltip, the "
             "Curios and attunement rules, the Djinn vessel flag, the forge and the sockets are the core's, as for every relic.\n"
             "* Loot is by grade through the existing relic pools, so the new weapons enter every source that rolls their grade: the `nusmp:relic` entries of the vault tables "
             "(`chests/dungeon_relic_vault`, `chests/labyrinth_pedestal`: Legend to God at weight 3 / 15, God to Genesis at 1 / 4), the Crucible of Choice of the Mega-Dungeon "
             "(unique to genesis offers) and the God of Order's ledger (one of each in a world). Each weapon is also placed on a dungeon road in `data/nusmp/mega/relic_themes.json` "
             "(the theme column), so taking it in a Crucible leads to a fitting next region.\n"
             "* Better Combat: a file in `data/nusmp/weapon_attributes/<id>.json` for every melee weapon (31; the two bows, the crossbow and the shield have none, as every ranged "
             "weapon and shield), made by `python3 -B tools/bettercombat/inventory.py` and `gen_attributes.py`; the gauntlets are class `dagger` (their size and swing) with the damage "
             "of a club.\n"
             "* Size classes: a row for every id in `assets/nusmp/weapon_classes.json` and `tools/relics/weapon_classes.json`; the first-person display of the long weapons is the "
             "oversize rule (`new197_presets.py`).\n")
    L.append("## 5. Tests\n")
    L.append("JUnit (all pure, run without the game): `relics/wave/Wave71DefsTest` (counts, unique ids, 3D, engravings from the Tensura list, 2 or 3 acts, damage inside the grade's range, "
             "ranged marks, the lang fragment), `Wave71RulesTest` (the rule arithmetic and the table of 35 rules), `Wave71AssetsTest` (mesh, skins, 16 x 16 sprite, models, draw states, "
             "class rows, Better Combat files, dungeon roads); the existing `WeaponMeshesTest`, `BowDisplayGuardTest`, `WeaponClassesTest`, `WeaponAttributesTest`, "
             "`RelicHandCoverageTest`, `RelicRenderCoverageTest` and `RelicThemesDataTest` pass with the new weapons. The in-game self-test `relic.new197` (registered by "
             "`Wave71.hooks`, run by the CI server smoke test) checks the registry (grades, 3D, engravings, acts, passive rules, damage range, the files in the jar).\n")
    L.append("## 6. Tools\n")
    L.append("```\npython3 -B tools/relics/new197_gen.py        # Wave71.java, Wave71Passives.java, tools/lang/new_weapons_197.json, class rows, dungeon roads, this page's table\n"
             "python3 -B tools/relics/new197_build.py      # all 35 meshes, skins, sprites, item models, then bow_vanilla.py and new197_presets.py\n"
             "python3 -B tools/relics/new197_audit.py      # the size / orientation audit of the 35\n"
             "python3 -B tools/relics/new197_sheet.py out.png   # the sprites; --meshes out.png [id | grade:GOD]  the models\n"
             "python3 -B tools/bettercombat/inventory.py && python3 -B tools/bettercombat/gen_attributes.py\n"
             "python3 -B tools/item_preview/preview_weapons.py vorpal_sword blackrazor   # the held view, build/item_preview/weapons.png\n```\n")
    L.append("## 7. Edits to files that already existed (all additions)\n")
    L.append("* `assets/nusmp/weapon_classes.json`, `tools/relics/weapon_classes.json`: 35 class rows appended.\n"
             "* `data/nusmp/mega/relic_themes.json`: 35 dungeon-road rows appended.\n"
             "* `tools/bettercombat/manifest.json`: the 31 melee weapons added (regenerated by the tool).\n"
             "* `tools/relics/display_presets_manifest.json`: the rows of the 26 long weapons added.\n"
             "* New: `relics/GradeDamage.java` (a STAND-IN with exactly `range(String)` and `damage(String, String)`; replace it with the full damage-table class when that is merged), "
             "`relics/wave/Wave71*.java`, three test classes, `tools/lang/new_weapons_197.json`, 31 Better Combat files, the assets, `tools/relics/new197_*.py`, this page and the previews.\n"
             "* Not touched, as asked: the version, the changelog, the README, `en_us.json` (the lead merges `tools/lang/new_weapons_197.json`), the Time Magic VFX.\n")
    L.append("## 8. What could not be verified\n")
    L.append("Nothing can be run in a game here: the models, skins, sprites, glow and void layers, the draw states, the held pose and size of each weapon, the particle and VFX shapes of "
             "the acts, the act numbers against a real boss (the balance law scales them), the passive hooks and the loot rolls are all unseen in the game. The previews are a "
             "software rasteriser, a re-implementation of the renderer (the void is shown as a crimson star field). The Java was type-checked against the API stubs (no real "
             "compile) and the JUnit tests ran in a shim without Minecraft. The Better Combat files use the presets of the class (the swings are the parent's own). "
             "The two bows and the crossbow follow the proven pipeline but their draw poses are unseen.\n")
    with open(os.path.join(ROOT, "docs", "new_weapons_197.md"), "w", newline="\n") as f:
        f.write("\n".join(L))


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
    write_doc()
    print("wrote Wave71.java, Wave71Passives.java, tools/lang/new_weapons_197.json, the class rows and the theme rows for %d weapons" % len(ROSTER))


if __name__ == "__main__":
    main()
