"""The effect vocabulary of the Unique Skill Lines (1.82.0).

WHY: 275+ lines of 15 to 30 nodes each cannot be 5,000 hand-written Java classes. Every node of a generated line names one KIND from this catalogue and gives
numbers for it; the engine (skilltree/line/ in Java) has ONE executor per kind and the generator (gen_lines.py) writes the per-level tables. So a line is data,
a new line costs no code, and what makes lines different is the kinds they combine, their numbers, their elements and their names. The Java side is pinned to this file by a test:
every kind here must have an executor and every executor must be listed here (see docs/skillbooks_unique.md).

Param spec:  name: (base, min, max, grow)   numeric; level n value = base * (1 + grow * (n-1)), clamped to [min, max], rounded sensibly.
Enum spec:   name: [allowed strings]        string; the same at every level (goes to the node's "params" map).
"""

VFX = ["WIND_SLASH", "WIND_RING", "SPIRIT_AURA", "FLAME_EXPLOSION", "FIRE_BURST", "WATER_BURST", "LIGHTNING_SPEAR", "EARTH_SPIKES", "SPATIAL_RIFT", "MIRROR_PANE",
       "DEVIL_CIRCLE", "MAGIC_CIRCLE", "ELF_CIRCLE", "SHADOW_POOL", "POISON_BREATH", "SEAL_CHAINS", "THREAD_LINE", "WEAPON_CONSTRUCTS", "FIRE_SPIRAL", "LIGHTNING_FIEND",
       "WATER_RING", "ASH_FORMATION", "FLAME_TRAIL", "STONE_SPIKES", "WIND_TORNADO"]

EFFECTS = ["speed", "slowness", "haste", "mining_fatigue", "strength", "weakness", "resistance", "regeneration", "fire_resistance", "water_breathing", "invisibility",
           "night_vision", "jump_boost", "absorption", "luck", "poison", "wither", "blindness", "hunger", "glowing", "slow_falling", "nausea", "levitation", "darkness"]

CONDITIONS = ["low_health", "full_health", "sprinting", "sneaking", "night", "day", "in_water", "on_fire", "airborne", "in_combat", "holding_sword", "unarmored", "underground", "raining"]

CATEGORIES = ["undead", "arthropod", "illager", "burning", "boss", "monster", "animal", "player"]

DAMAGE_KINDS = ["fire", "magic", "explosion", "projectile", "fall", "freeze", "lightning", "wither"]

ATTRIBUTES = ["max_health", "armor", "armor_toughness", "attack_damage", "attack_speed", "movement_speed", "knockback_resistance", "luck", "step_height", "jump_strength",
              "safe_fall_distance", "block_interaction_range", "entity_interaction_range"]

TARGETS = ["allies", "enemies"]
OPS = ["add", "mult"]

# kind -> {"type": ..., "params": {...}, "desc": template}
ACTIVE = {
    "cone_strike":  {"params": {"damage": (6, 2, 60, .25), "range": (3.5, 2.5, 8, .08), "half_angle": (40, 20, 80, .1), "targets": (2, 1, 12, .4), "knockback": (.4, 0, 2, .1),
                                "cost": (.015, .005, .08, .08), "cooldown": (100, 60, 400, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Strike a cone in front of you."},
    "ring_burst":   {"params": {"damage": (5, 2, 60, .25), "radius": (3.5, 2, 12, .1), "targets": (4, 1, 16, .4), "knockback": (.6, 0, 2.5, .1), "cost": (.02, .005, .08, .08), "cooldown": (140, 60, 500, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Burst outward around you."},
    "dash_strike":  {"params": {"distance": (6, 3, 14, .1), "damage": (6, 2, 60, .25), "targets": (3, 1, 10, .3), "cost": (.015, .005, .08, .08), "cooldown": (120, 60, 400, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Dash forward, hitting everything in your path."},
    "leap_slam":    {"params": {"height": (.7, .4, 1.4, .08), "damage": (7, 2, 60, .25), "radius": (3, 2, 9, .1), "knockback": (.6, 0, 2.5, .1), "cost": (.02, .005, .08, .08), "cooldown": (200, 80, 600, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Leap and crash down on the foes below."},
    "ray_shot":     {"params": {"damage": (7, 2, 70, .25), "range": (14, 6, 40, .1), "pierce": (1, 1, 6, .35), "cost": (.02, .005, .08, .08), "cooldown": (120, 60, 500, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Fire a piercing beam along your line of sight."},
    "chain_hit":    {"params": {"damage": (5, 2, 50, .25), "jumps": (3, 1, 10, .4), "jump_range": (5, 3, 10, .05), "falloff": (.15, 0, .5, 0), "cost": (.02, .005, .08, .08), "cooldown": (160, 60, 500, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Strike a foe; the blow jumps from foe to foe."},
    "buff_self":    {"params": {"seconds": (12, 4, 60, .15), "amp": (0, 0, 4, .5), "amp2": (0, 0, 4, .5), "cost": (.015, .005, .08, .08), "cooldown": (400, 100, 1800, -.06)},
                     "enums": {"effect": EFFECTS, "effect2": EFFECTS + ["none"], "vfx": VFX}, "desc": "Gain an effect (or two) for a while."},
    "buff_aura":    {"params": {"radius": (7, 3, 16, .1), "seconds": (10, 4, 40, .15), "amp": (0, 0, 3, .5), "cost": (.03, .01, .1, .08), "cooldown": (600, 200, 2400, -.06)},
                     "enums": {"effect": EFFECTS, "vfx": VFX}, "desc": "Give an effect to yourself and your allies around you."},
    "heal_self":    {"params": {"amount": (6, 2, 40, .25), "regen_seconds": (5, 0, 20, .15), "cost": (.02, .005, .08, .08), "cooldown": (400, 120, 1800, -.06)},
                     "enums": {"vfx": VFX}, "desc": "Heal yourself and regenerate for a while."},
    "heal_pulse":   {"params": {"radius": (6, 3, 14, .1), "amount": (5, 2, 30, .25), "cost": (.03, .01, .1, .08), "cooldown": (500, 160, 2000, -.06)},
                     "enums": {"vfx": VFX}, "desc": "Heal yourself and the allies around you."},
    "blink":        {"params": {"distance": (8, 4, 24, .12), "cost": (.015, .005, .08, .08), "cooldown": (140, 60, 600, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Teleport a short way forward."},
    "shield":       {"params": {"absorb_hearts": (4, 2, 20, .25), "seconds": (6, 3, 20, .1), "resist_amp": (0, 0, 3, .5), "cost": (.02, .005, .08, .08), "cooldown": (400, 120, 1800, -.06)},
                     "enums": {"vfx": VFX}, "desc": "Raise a shield of absorption."},
    "taunt":        {"params": {"radius": (8, 3, 16, .1), "targets": (4, 1, 14, .4), "resist_seconds": (3, 1, 12, .2), "cost": (.02, .005, .08, .08), "cooldown": (200, 80, 800, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Draw the attention of hostile creatures to yourself."},
    "debuff_cone":  {"params": {"range": (5, 3, 10, .08), "half_angle": (40, 20, 80, .1), "seconds": (6, 2, 30, .15), "amp": (0, 0, 3, .5), "seconds2": (0, 0, 30, .15), "cost": (.02, .005, .08, .08), "cooldown": (220, 80, 900, -.08)},
                     "enums": {"effect": EFFECTS, "effect2": EFFECTS + ["none"], "vfx": VFX}, "desc": "Afflict everyone in a cone in front of you."},
    "pull":         {"params": {"radius": (7, 3, 14, .1), "strength": (.8, .3, 2.5, .1), "targets": (4, 1, 14, .4), "damage": (3, 0, 40, .25), "cost": (.02, .005, .08, .08), "cooldown": (200, 80, 800, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Drag the creatures around you toward yourself."},
    "uppercut":     {"params": {"damage": (7, 2, 60, .25), "range": (3.2, 2.5, 6, .06), "launch": (.6, .3, 1.6, .1), "cost": (.015, .005, .08, .08), "cooldown": (120, 60, 400, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Launch the foe in front of you into the air."},
    "whirlwind":    {"params": {"damage": (3, 1, 30, .25), "radius": (3.5, 2, 9, .08), "hits": (4, 2, 12, .3), "interval": (5, 3, 10, 0), "cost": (.025, .01, .1, .08), "cooldown": (260, 100, 900, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Spin, hitting everything around you several times."},
    "barrage":      {"params": {"damage": (3, 1, 30, .25), "shots": (5, 2, 16, .35), "range": (12, 6, 30, .08), "interval": (3, 2, 8, 0), "cost": (.025, .01, .1, .08), "cooldown": (240, 100, 900, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Loose a volley at the foe you look at."},
    "ground_wave":  {"params": {"damage": (6, 2, 60, .25), "length": (9, 5, 24, .1), "width": (2, 1, 6, .1), "knockback": (.5, 0, 2, .1), "cost": (.02, .005, .08, .08), "cooldown": (180, 80, 700, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Send a wave along the ground in front of you."},
    "execute":      {"params": {"damage": (8, 3, 70, .25), "range": (3.5, 2.5, 7, .06), "bonus": (.5, 0, 3, .15), "cost": (.02, .005, .08, .08), "cooldown": (200, 80, 800, -.08)},
                     "enums": {"vfx": VFX}, "desc": "A blow that hits much harder the weaker the foe is."},
    "drain_bite":   {"params": {"damage": (6, 2, 50, .25), "range": (3.2, 2.5, 6, .06), "heal_pct": (.3, .1, 1.0, .12), "cost": (.015, .005, .08, .08), "cooldown": (120, 60, 500, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Strike a foe and heal for a share of the damage."},
    "stun_ring":    {"params": {"radius": (4, 2, 10, .1), "seconds": (2, 1, 8, .2), "targets": (4, 1, 14, .4), "cost": (.025, .01, .1, .08), "cooldown": (300, 120, 1200, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Freeze the creatures around you in place for a moment."},
    "smoke":        {"params": {"radius": (5, 2, 12, .1), "seconds": (5, 2, 20, .15), "cost": (.015, .005, .08, .08), "cooldown": (300, 100, 1200, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Blind the creatures around you while you slip away."},
    "mark":         {"params": {"range": (16, 6, 40, .1), "seconds": (8, 3, 30, .15), "bonus": (.25, .1, 1.0, .2), "cost": (.015, .005, .08, .08), "cooldown": (200, 80, 900, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Mark a foe: it takes more damage for a while."},
    "ward_reflect": {"params": {"seconds": (6, 3, 20, .1), "reflect_pct": (.3, .1, 1.0, .2), "cost": (.02, .005, .08, .08), "cooldown": (400, 120, 1800, -.06)},
                     "enums": {"vfx": VFX}, "desc": "For a while, part of the damage you take is thrown back at the attacker."},
    "meteor_drop":  {"params": {"delay": (20, 10, 60, -.05), "damage": (12, 4, 90, .25), "radius": (4, 2, 10, .1), "range": (18, 8, 40, .08), "cost": (.03, .01, .1, .08), "cooldown": (360, 120, 1500, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Call a strike down on the place you look at after a short delay."},
    # 3.26.0 (Evolution skills): consumes the Debt that the player's own hits stored on foes (see debt_store) and turns it into a burst. docs/skills/evolution_ten.md
    "debt_burst":   {"params": {"damage": (8, 2, 120, .25), "mult": (1.5, .5, 5.0, .1), "radius": (5, 2, 24, .1), "targets": (5, 1, 20, .4), "cost": (.03, .01, .1, .08), "cooldown": (260, 100, 900, -.08)},
                     "enums": {"vfx": VFX}, "desc": "Detonate the Debt stored on the foes around you: a blow plus a multiple of all the Debt it consumes."},
}

PASSIVE = {
    "on_hit_effect":     {"params": {"seconds": (3, 1, 20, .15), "amp": (0, 0, 3, .5), "chance": (.2, .05, .8, .2)}, "enums": {"effect": EFFECTS}, "desc": "Your blows may afflict the foe."},
    "on_hurt_retaliate": {"params": {"seconds": (3, 1, 20, .15), "amp": (0, 0, 3, .5), "chance": (.2, .05, .8, .2), "rest": (100, 20, 600, -.1)}, "enums": {"effect": EFFECTS}, "desc": "Whoever strikes you may be afflicted."},
    "self_effect_when":  {"params": {"amp": (0, 0, 3, .5)}, "enums": {"effect": EFFECTS, "condition": CONDITIONS}, "desc": "While the condition holds you have an effect."},
    "lifesteal":         {"params": {"pct": (.05, .01, .5, .2)}, "enums": {}, "desc": "Heal for a share of the damage you deal."},
    "thorns":            {"params": {"pct": (.15, .05, 1.0, .2), "rest": (20, 5, 200, 0)}, "enums": {}, "desc": "Part of the damage you take is returned to the attacker."},
    "crit":              {"params": {"chance": (.1, .02, .6, .2), "mult": (1.5, 1.2, 3.0, .08)}, "enums": {}, "desc": "Your blows may strike critically."},
    "dmg_vs":            {"params": {"bonus": (.15, .05, 1.0, .2)}, "enums": {"category": CATEGORIES}, "desc": "You deal more damage to one kind of creature."},
    "dmg_when":          {"params": {"bonus": (.15, .05, 1.0, .2)}, "enums": {"condition": CONDITIONS}, "desc": "You deal more damage while the condition holds."},
    "reduce_when":       {"params": {"pct": (.1, .03, .6, .2)}, "enums": {"condition": CONDITIONS}, "desc": "You take less damage while the condition holds."},
    "regen_when":        {"params": {"per_second": (.5, .1, 4, .2)}, "enums": {"condition": CONDITIONS}, "desc": "You heal while the condition holds."},
    "kill_reward":       {"params": {"heal": (2, 0, 20, .25), "seconds": (4, 0, 20, .15), "amp": (0, 0, 3, .5)}, "enums": {"effect": EFFECTS + ["none"]}, "desc": "A kill heals you and may grant an effect."},
    "dodge":             {"params": {"chance": (.08, .02, .4, .2), "rest": (100, 20, 600, -.1)}, "enums": {}, "desc": "You may avoid a blow completely."},
    "resist":            {"params": {"pct": (.15, .05, .8, .2)}, "enums": {"damage_kind": DAMAGE_KINDS}, "desc": "You take less damage of one kind."},
    "xp_bonus":          {"params": {"pct": (.1, .02, 1.0, .2)}, "enums": {}, "desc": "Kills give more experience."},
    "drop_bonus":        {"params": {"chance": (.08, .02, .5, .2)}, "enums": {}, "desc": "A kill may drop an extra stack of its loot."},
    "aura":              {"params": {"radius": (6, 3, 14, .1), "amp": (0, 0, 2, .5)}, "enums": {"effect": EFFECTS, "target": TARGETS}, "desc": "An aura around you affects those near you."},
    "hunger_saver":      {"params": {"pct": (.15, .05, .7, .2)}, "enums": {}, "desc": "You tire more slowly."},
    "combo":             {"params": {"hits": (4, 2, 8, -.1), "bonus": (.5, .2, 2.0, .2)}, "enums": {}, "desc": "Every few blows in a row hit much harder."},
    "last_stand":        {"params": {"threshold": (.3, .1, .5, 0), "seconds": (5, 2, 15, .15), "rest": (1800, 600, 6000, -.1)}, "enums": {}, "desc": "When nearly dead you are briefly shielded."},
    "momentum":          {"params": {"per_stack": (.04, .01, .15, .15), "max_stacks": (5, 2, 12, .3)}, "enums": {}, "desc": "Blows in a row grow stronger until you stop fighting."},
    "sprint_bonus":      {"params": {"bonus": (.2, .05, 1.0, .2)}, "enums": {}, "desc": "You deal more damage while sprinting."},
    "fall_guard":        {"params": {"pct": (.3, .1, 1.0, .2)}, "enums": {}, "desc": "You take less fall damage."},
    "knockback_on_hit":  {"params": {"strength": (.4, .1, 1.5, .15), "chance": (.3, .05, 1.0, .15)}, "enums": {}, "desc": "Your blows may shove the foe back."},
    "execute_threshold": {"params": {"threshold": (.2, .1, .4, .1), "bonus": (.5, .2, 3.0, .2)}, "enums": {}, "desc": "You deal much more damage to nearly dead foes."},
    "mending":           {"params": {"seconds": (20, 5, 60, -.1), "amount": (1, 1, 6, .25)}, "enums": {}, "desc": "Your wounds close over time, even out of combat."},
    # 3.26.0 (Evolution skills): the shared "Debt" mechanic. Hits store a share of their damage on the foe; it ticks later and is consumed by debt_burst arts.
    "debt_store":        {"params": {"store_pct": (.25, .1, .8, .2), "tick_seconds": (3, 0, 10, 0), "max_pct": (.5, .1, 1.0, 0), "hold_seconds": (12, 4, 30, 0)}, "enums": {"vfx": VFX},
                          "desc": "Your blows store delayed damage on the foe (Debt). It ticks while it waits, and your Debt arts consume it for a burst."},
}

STANCE = {"stance": {"params": {"damage_pct": (0, -.5, 1.0, .2), "armor": (0, -10, 20, .25), "speed_pct": (0, -.5, .6, .2), "knockback_res": (0, 0, 1.0, .2), "regen": (0, 0, 3, .2),
                                "crit_chance": (0, 0, .5, .2), "cost": (.0, 0, .05, 0), "cooldown": (40, 20, 200, 0)}, "enums": {"vfx": VFX},
                    "desc": "Take a fighting stance: a set of bonuses (and sometimes a price) while it lasts. Only one stance at a time."}}

ATTR = {"attr": {"params": {"amount": (1, -20, 40, .25)}, "enums": {"attribute": ATTRIBUTES, "op": OPS}, "desc": "A permanent bonus to one attribute."}}

BACKGROUNDS = ["magma", "void", "stellar", "forest", "ocean", "storm", "ruin", "blood", "holy", "frost", "tower", "city", "dream", "sand", "iron"]

# a finisher's cooldown is minutes, not seconds: widen every cooldown range (the generator still enforces >= 60 ticks for an art and >= 900 for a finisher at its last level)
for _group in (ACTIVE, STANCE):
    for _kind in _group.values():
        _b, _lo, _hi, _g = _kind["params"]["cooldown"]
        _kind["params"]["cooldown"] = (_b, _lo, 7200, _g)

# 3.26.0 (Evolution skills): a line may carry {"evolution": {"requirement": <kind>, "count": N, "text": "...", "ultimate_name": "..."}} and ONE finisher flagged "ultimate".
# The kind names what the engine counts for the player while the skill is owned and switched on; "damage_taken" may carry a damage_kind filter. Counters are capped at count.
EVOLUTION = {
    "kills":           {"desc": "Kills made while the skill is on."},
    "elite_kills":     {"desc": "Kills of elite or boss creatures (a boss tag, or at least 100 maximum health)."},
    "boss_kills":      {"desc": "Kills of boss creatures."},
    "damage_dealt":    {"desc": "Damage dealt to hostile creatures while the skill is on."},
    "damage_taken":    {"desc": "Damage taken (optionally only of one kind: params.damage_kind).", "enums": {"damage_kind": DAMAGE_KINDS + ["any"]}},
    "lethal_survived": {"desc": "Blows that would have left the player at 2 health or less, survived (at most one every 5 seconds)."},
    "arts_cast":       {"desc": "Arts of this skill cast."},
    "ally_buffs":      {"desc": "Allies reached by a buffing or healing art of this skill."},
}
EVOLUTION_MAX_COUNT = 1000000

# the Ultimate Evolution art has a cooldown of minutes: widen it for the one kind that carries it
ACTIVE["debt_burst"]["params"]["cooldown"] = (260, 100, 18000, -.08)
