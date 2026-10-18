"""The row format of the generated relic waves (1.70.0 and later) and its vocabulary.

A relic is one `R(...)` row:  R(name, slot, look, grade, (main, trim, glow), icon, desc, passives, acts, att=False, id=None)
  slot    NECK BACK BELT HANDS CHARM HEAD CHEST FEET (a weapon or a consumable has its own pipeline and is not made here)
  look    a Look the worn-model catalog knows for that slot (see LOOKS below)
  grade   NORMAL SPECIAL RARE UNIQUE LEGEND GOD GENESIS
  icon    a motif of relic_icon_lib (ring, pendant, orb, key, ...)
  passives  one string, tokens separated by spaces:
      a:<attr>:<op><amount>   attr luck speed hp armor tough kb atk aspd mp reach sneak fall jump swim; op + (add) * (base multiplier) ** (total multiplier)
      r:<kind>:<fraction>     resist kind fire cold magic proj expl bolt fall wither curse dark holy breath
      e:<effect>:<amp>        a standing effect (night_vision water_breathing fire_resistance slow_falling dolphins_grace glowing haste luck)
      s:<skill>               a Tensura skill the relic lends while worn (danger_sense, magic_sense, ...: SKILLS below)
  acts    a tuple of (name, cooldown_seconds, mana_percent, spec, flavour[, uses_per_day[, needs_sneak]]); the first runs on use, the second on sneak + use, the rest on the key
      spec = one or more calls joined by '+':
        heal:N  regen:SEC:AMP  buff:EFFECT:SEC:AMP  shield:ABSORB:SEC  aoe:RADIUS:DAMAGE:TYPE:EFFECT:SEC:AMP  bolt:RANGE:DAMAGE:TYPE:EFFECT:SEC:AMP  dash:N  blink:N  lend:SKILL:SEC
        rift:crystal|key|portal|mark|sense|pocket  (the dimensional rifts and the waypoint: RiftActs)
        cleanse  allies:RADIUS:EFFECT:SEC:AMP  mark:RADIUS:SEC  second:FRACTION:SEC  drain:RADIUS:DAMAGE:TYPE  gale:RADIUS:STRENGTH  pull:RADIUS:STRENGTH  ignite:RADIUS:SEC
        (TYPE: magic fire cold bolt expl wither thorns hit sonic; EFFECT '-' = none)
Why a vocabulary: see labyrinth-free Wave31Acts.java: every ability is one of these shapes on the relic core's own helpers."""
import re

LOOKS = {
    "NECK": {"AMULET", "PENDANT", "LOCKET"}, "BACK": {"CLOAK", "CAPE", "MANTLE"}, "BELT": {"BELT"}, "HANDS": {"GAUNTLETS", "GLOVES", "CHARM"},
    "CHARM": {"CHARM", "AMULET", "POTION", "TOME", "ORB", "PENDANT", "CIRCLET", "BELT"}, "HEAD": {"CROWN", "CIRCLET", "HAT", "HOOD", "PLATE"},
    "CHEST": {"PLATE", "COAT", "SUIT", "LEATHER", "ROBE"}, "FEET": {"BOOTS", "GREAVES", "SANDALS"},
}
GRADES = {"NORMAL", "SPECIAL", "RARE", "UNIQUE", "LEGEND", "GOD", "GENESIS"}
ATTR = {"luck": "minecraft:generic.luck", "speed": "minecraft:generic.movement_speed", "hp": "minecraft:generic.max_health", "armor": "minecraft:generic.armor",
        "tough": "minecraft:generic.armor_toughness", "kb": "minecraft:generic.knockback_resistance", "atk": "minecraft:generic.attack_damage", "aspd": "minecraft:generic.attack_speed",
        "mp": "tensura:max_magicule", "reach": "minecraft:player.entity_interaction_range", "sneak": "minecraft:player.sneaking_speed", "fall": "minecraft:generic.safe_fall_distance",
        "jump": "minecraft:generic.jump_strength", "swim": "neoforge:swim_speed"}
OPS = {"+": "ADD_VALUE", "*": "ADD_MULTIPLIED_BASE", "**": "ADD_MULTIPLIED_TOTAL"}
RESIST = {"fire": "minecraft:is_fire", "cold": "minecraft:is_freezing", "magic": "minecraft:magic", "proj": "minecraft:is_projectile", "expl": "minecraft:is_explosion",
          "bolt": "minecraft:is_lightning", "fall": "minecraft:is_fall", "wither": "minecraft:wither", "curse": "tensura:curse", "dark": "tensura:darkness_elemental",
          "holy": "tensura:holy_damage", "breath": "minecraft:dragon_breath"}
DAMAGE = {"magic": "minecraft:magic", "fire": "minecraft:in_fire", "cold": "minecraft:freeze", "bolt": "minecraft:lightning_bolt", "expl": "minecraft:explosion",
          "wither": "minecraft:wither", "thorns": "minecraft:thorns", "hit": "minecraft:player_attack", "sonic": "minecraft:sonic_boom"}
EFFECT = {n: "minecraft:" + n for n in ("speed", "slowness", "strength", "resistance", "haste", "regeneration", "invisibility", "night_vision", "fire_resistance", "water_breathing",
                                         "absorption", "luck", "slow_falling", "weakness", "wither", "poison", "blindness", "nausea", "glowing", "levitation", "hunger", "mining_fatigue",
                                         "dolphins_grace", "health_boost", "saturation", "jump_boost", "hero_of_the_village")}
EFFECT.update({"regen": "minecraft:regeneration", "jump": "minecraft:jump_boost", "nv": "minecraft:night_vision", "fire_res": "minecraft:fire_resistance", "water": "minecraft:water_breathing",
               "absorb": "minecraft:absorption", "slow_fall": "minecraft:slow_falling", "glow": "minecraft:glowing", "slow": "minecraft:slowness"})
SKILLS = {n: "tensura:" + n for n in ("abnormal_condition_resistance", "coercion", "cold_resistance", "danger_sense", "darkness_attack_resistance", "dragon_ear", "electricity_resistance",
                                       "eye_of_truth", "farsight", "flame_attack_nullification", "flame_attack_resistance", "flight", "freeze", "heat_resistance", "holy_attack_resistance",
                                       "hydraulic_propulsion", "light_attack_resistance", "magic_resistance", "magic_sense", "poison_nullification", "poison_resistance", "presence_concealment",
                                       "self_regeneration", "sense_soundwave", "shadow_motion", "sound_manipulation", "spatial_attack_resistance", "strengthen_body", "telepathy",
                                       "thought_acceleration", "thought_communication", "water_attack_resistance", "water_breathing", "wind_attack_resistance")}
SERIES_TAG = {}


def slug(name):
    s = name.lower().replace("'", "").replace("’", "").replace("‘", "")
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_")


def R(name, slot, look, grade, pal, icon, desc, passives="", acts=(), att=False, id=None, series=""):
    if slot not in LOOKS or look not in LOOKS[slot]:
        raise ValueError("%s: look %s is not drawn for slot %s (%s)" % (name, look, slot, sorted(LOOKS.get(slot, []))))
    if grade not in GRADES:
        raise ValueError("%s: unknown grade %s" % (name, grade))
    return {"name": name, "id": id or slug(name), "slot": slot, "look": look, "grade": grade, "pal": pal, "icon": icon, "desc": desc, "passives": passives, "acts": list(acts), "att": att, "series": series}


def passive_java(tok):
    p = tok.split(":")
    if p[0] == "a":
        m = re.match(r"^(\+|\*\*|\*)?(-?[0-9.]+)$", p[2])
        if p[1] not in ATTR or not m:
            raise ValueError("bad attribute token " + tok)
        return 'Passive.attr("%s", %s, Operation.%s)' % (ATTR[p[1]], float(m.group(2)), OPS[m.group(1) or "+"])
    if p[0] == "r":
        if p[1] not in RESIST:
            raise ValueError("bad resist token " + tok)
        return 'Passive.resist("%s", %s)' % (RESIST[p[1]], float(p[2]))
    if p[0] == "e":
        if p[1] not in EFFECT:
            raise ValueError("bad effect token " + tok)
        return 'Passive.effect("%s", %d)' % (EFFECT[p[1]], int(p[2]) if len(p) > 2 else 0)
    if p[0] == "s":
        if p[1] not in SKILLS:
            raise ValueError("bad skill token " + tok)
        return 'Passive.skill("%s")' % SKILLS[p[1]]
    raise ValueError("bad passive token " + tok)


def _eff(e):
    if e == "-":
        return "null"
    if e not in EFFECT:
        raise ValueError("unknown effect " + e)
    return '"%s"' % EFFECT[e]


def call_java(spec):
    p = spec.split(":")
    k = p[0]
    f = lambda i: float(p[i])
    n = lambda i: int(float(p[i]))
    if k == "heal": return "Wave31Acts.heal(ctx, %sf)" % f(1)
    if k == "regen": return "Wave31Acts.regen(ctx, %d, %d)" % (n(1), n(2))
    if k == "buff": return 'Wave31Acts.buff(ctx, %s, %d, %d)' % (_eff(p[1]), n(2), n(3))
    if k == "shield": return "Wave31Acts.shield(ctx, %sf, %d)" % (f(1), n(2))
    if k == "aoe": return 'Wave31Acts.aoe(ctx, %s, %sf, "%s", %s, %d, %d)' % (f(1), f(2), DAMAGE[p[3]], _eff(p[4]), n(5), n(6))
    if k == "bolt": return 'Wave31Acts.bolt(ctx, %s, %sf, "%s", %s, %d, %d)' % (f(1), f(2), DAMAGE[p[3]], _eff(p[4]), n(5), n(6))
    if k == "dash": return "Wave31Acts.dash(ctx, %s)" % f(1)
    if k == "blink": return "Wave31Acts.blink(ctx, %s)" % f(1)
    if k == "lend":
        if p[1] not in SKILLS: raise ValueError("unknown skill " + p[1])
        return 'Wave31Acts.lend(ctx, "%s", %d)' % (SKILLS[p[1]], n(2))
    if k == "cleanse": return "Wave31Acts.cleanse(ctx)"
    if k == "allies": return 'Wave31Acts.allies(ctx, %s, %s, %d, %d)' % (f(1), _eff(p[2]), n(3), n(4))
    if k == "mark": return "Wave31Acts.mark(ctx, %s, %d)" % (f(1), n(2))
    if k == "second": return "Wave31Acts.secondWind(ctx, %s, %d)" % (f(1), n(2))
    if k == "drain": return 'Wave31Acts.drain(ctx, %s, %sf, "%s")' % (f(1), f(2), DAMAGE[p[3]])
    if k == "gale": return "Wave31Acts.gale(ctx, %s, %s)" % (f(1), f(2))
    if k == "pull": return "Wave31Acts.pull(ctx, %s, %s)" % (f(1), f(2))
    if k == "ignite": return "Wave31Acts.ignite(ctx, %s, %d)" % (f(1), n(2))
    if k == "rift":                                         # 1.72.0: rift:crystal | key | portal | mark | sense | pocket  (RiftActs, the dimensional rifts and the waypoint)
        if p[1] not in ("crystal", "key", "portal", "mark", "sense", "pocket"): raise ValueError("unknown rift call " + spec)
        return "RiftActs.%s(ctx)" % p[1]
    raise ValueError("unknown act spec " + spec)


def act_java(a, idx):
    name, cd, mana, spec = a[0], a[1], a[2], a[3]
    flavour = a[4] if len(a) > 4 else ""
    per_day = a[5] if len(a) > 5 else 0
    sneak = a[6] if len(a) > 6 else False
    calls = "; ".join(call_java(s) for s in spec.split("+"))
    key = slug(name)
    say = '; Wave31Acts.say(ctx, "%s")' % jesc(flavour) if flavour else ""
    out = 'Act.of("%s", "%s", ctx -> { %s%s; }).cooldown(%d)' % (key, jesc(name), calls, say, int(cd * 20))
    if mana: out += ".mana(%d)" % mana
    if per_day: out += ".usesPerDay(%d)" % per_day
    if sneak: out += ".needsSneak()"
    return out


def jesc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"')
