package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.function.Consumer;

/**
 * Why: the catalog needs one list of these relics ({@link #defs}, the core registers the items) and one place that registers the game-bus listeners once ({@link #hooks}). The act bodies
 * are in {@link Wave22Acts}, the listeners in {@link Wave22Events}, the charges in {@link Wave22Charges}, the numbers in {@link Wave22Rules}. The long card
 * texts, with every tabletop rule turned into a game rule, are in the lang fragment relics_wave22.lang.json and in the wave's doc.
 * <p>
 * Relics of Many Worlds, part 4, wave 22: twelve staves and wands (Dreamcore Woven Wand, Lich Wand, Scepter of the Double Moon, Serpant Head Staff,
 * Staff of Blood Magic, Staff of Ember Visions, Staff of the Apprentice, Superlunary Staff, The Bifrost, Wand-o-Wisp, Wand of Spider Summoning, Windwheel Staff).
 */
public final class Wave22 {
    private Wave22() {}

    // flags, set by the core while the relic is carried (and attuned, where it needs that)
    public static final String F_MOON = "w22_double_moon", F_LICH = "w22_lich_wand", F_SERPENT = "w22_serpant_staff", F_BIFROST = "w22_bifrost", F_SUPER = "w22_superlunary", F_WIND = "w22_windwheel";

    private static final String COST = "tensura:magic_cost_multiplier", CHANT = "tensura:chant_speed", MAX_MAGICULE = "tensura:max_magicule";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Dreamcore Woven Wand (uncommon, attunement by a druid)
        out.accept(RelicDef.of("dreamcore_woven_wand", "Dreamcore Woven Wand", Grade.SPECIAL, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x6B4A3A, 0xA8324A, 0xE8A8F8).look(Look.WAND).melee(2.5, 1.8)
                .engrave("magic_weapon", "restoration", "magic_capacity")
                .desc("A twisted wand wrapped in crimson cloth, with a pink and blue dream-crystal at its tip. Use it to give a creature a sweet dream, sneak-use for a nightmare; two more keys wake a sleeper and read a creature's dream. Three dreams a day.")
                .passive(Passive.attr(COST, -0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr(CHANT, 0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("dream_sweet", "Sweet Dream", Wave22Acts::dreamSweet).cooldown(40))
                .act(Act.of("dream_nightmare", "Nightmare", Wave22Acts::dreamNightmare).cooldown(40))
                .act(Act.of("dream_wake", "Wake", Wave22Acts::dreamWake).cooldown(40))
                .act(Act.of("dream_read", "Read the Dream", Wave22Acts::dreamRead).cooldown(20)));

        // ---------------------------------------------------------------- Lich Wand (no rarity printed)
        out.accept(RelicDef.of("lich_wand", "Lich Wand", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x6A5F78, 0x1E1A22, 0x8CFF9A).look(Look.WAND).melee(3.0, 1.6)
                .engrave("soul_eater", "ruination", "stagnation")
                .desc("A bone wand ending in a skeletal claw, wreathed in green flame. You need no air, your belly never empties and the night never tires you, but healing leaves you gaunt. Each dawn it stores three random necromancy spells; use casts the next, sneak-use reads them.")
                .passive(Passive.effect("minecraft:water_breathing", 0))
                .passive(Passive.skill("tensura:darkness_attack_resistance"))
                .passive(Passive.flag(F_LICH))
                .act(Act.of("stored_spell", "Stored Spell", Wave22Acts::lichCast).cooldown(60))
                .act(Act.of("read_wand", "Read the Wand", Wave22Acts::lichRead).cooldown(20)));

        // ---------------------------------------------------------------- Scepter of the Double Moon (legendary, attunement)
        out.accept(RelicDef.of("scepter_of_the_double_moon", "Scepter of the Double Moon", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().model3d().vessel()
                .palette(0x8F9CB4, 0xC8A050, 0xC8D8FF).look(Look.STAFF).melee(6.0, 1.2)
                .engrave("elemental_boost", "magic_capacity", "magic_interference")
                .desc("A marble orb set with rubies under two brass crescents. By day your fire, radiant and thunder spells hit twice as hard and cold half as hard; by night cold, necrotic and lightning double and fire halves; where there is no day or night the cold, necrotic, radiant and thunder double. Use for a Celestial Ray of the element the sky favours.")
                .passive(Passive.attr(MAX_MAGICULE, 0.08, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr(COST, -0.10, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.skill("tensura:magic_sense"))
                .passive(Passive.flag(F_MOON))
                .act(Act.of("celestial_ray", "Celestial Ray", Wave22Acts::celestialRay).cooldown(80).mana(4))
                .act(Act.of("read_sky", "Read the Sky", Wave22Acts::readSky).cooldown(20)));

        // ---------------------------------------------------------------- Serpant Head Staff (very rare, attunement by a spellcaster)
        out.accept(RelicDef.of("serpant_head_staff", "Serpant Head Staff", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x8C929C, 0x4FD8F0, 0x5FE8FF).look(Look.STAFF).melee(8.0, 1.5)
                .engrave("stagnation", "enervation", "sturdy", "magic_weapon")
                .desc("Silver serpents twined round a staff, each head holding an icy crystal. A +2 quarterstaff with ten charges that return at dawn. Use for a Petrifying Gaze (3 charges); sneak-use toggles Venom Strike, where each hit spends a charge and a snake head bites for you.")
                .passive(Passive.attr(COST, -0.10, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.skill("tensura:poison_resistance"))
                .passive(Passive.flag(F_SERPENT))
                .act(Act.of("petrifying_gaze", "Petrifying Gaze", Wave22Acts::gaze).cooldown(200))
                .act(Act.of("venom_strike", "Venom Strike", ctx -> Wave0Util.note(ctx.player(), ctx.isOn("venom_strike") ? "Venom Strike: the snake heads wake." : "Venom Strike: the snake heads sleep.", net.minecraft.ChatFormatting.GREEN)).toggle().cooldown(10)));

        // ---------------------------------------------------------------- Staff of Blood Magic (very rare, attunement, spellcaster)
        out.accept(RelicDef.of("staff_of_blood_magic", "Staff of Blood Magic", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x8A8A90, 0x2A2A30, 0xFF3838).look(Look.STAFF).melee(6.0, 1.3)
                .engrave("soul_eater", "ruination", "magic_weapon")
                .desc("A bone-grey staff circled by broken crescents round a red orb. It casts without magicules, paying in blood instead: use for a level 1 Blood Bolt, sneak-use for a level 3 Blood Lance, the third key for a level 5 Crimson Nova. It never takes your last two health.")
                .passive(Passive.attr(COST, -0.15, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("blood_bolt", "Blood Bolt", ctx -> Wave22Acts.bloodCast(ctx, 0)).cooldown(40))
                .act(Act.of("blood_lance", "Blood Lance", ctx -> Wave22Acts.bloodCast(ctx, 1)).cooldown(100))
                .act(Act.of("crimson_nova", "Crimson Nova", ctx -> Wave22Acts.bloodCast(ctx, 2)).cooldown(260)));

        // ---------------------------------------------------------------- Staff of Ember Visions (rare, attunement)
        out.accept(RelicDef.of("staff_of_ember_visions", "Staff of Ember Visions", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x24202A, 0xB02838, 0xFF9A3C).look(Look.STAFF).melee(5.0, 1.4)
                .engrave("elemental_boost", "magic_weapon", "vigor")
                .desc("A slim black staff crowned with an ember-bud and a pale flame-bird. A +2 focus with three charges that return at dawn. Use to raise a small flame in the shape you chose for ten minutes; sneak-use reshapes it: humanoid, beast, hummingbird.")
                .passive(Passive.attr(COST, -0.10, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr(CHANT, 0.10, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.skill("tensura:flame_attack_resistance"))
                .act(Act.of("ember_visions", "Ember Visions", Wave22Acts::emberVisions).cooldown(100))
                .act(Act.of("reshape_flame", "Reshape Flame", Wave22Acts::emberReshape).cooldown(20)));

        // ---------------------------------------------------------------- Staff of the Apprentice (uncommon, attunement)
        out.accept(RelicDef.of("staff_of_the_apprentice", "Staff of the Apprentice", Grade.SPECIAL, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x6A5C4E, 0x8A9AC8, 0xA08CFF).look(Look.STAFF).melee(3.5, 1.5)
                .engrave("magic_weapon", "magic_capacity", "vitality")
                .desc("A gnarled staff of blue-violet crystals. It is bound to one school of magic (a d8) with two first-level spells of it; five charges, 1d4 back at dawn. Use for the first spell, sneak-use for the second; the third key studies a new school once a week.")
                .passive(Passive.attr(COST, -0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("first_spell", "First Spell", ctx -> Wave22Acts.apprenticeCast(ctx, 0)).cooldown(20))
                .act(Act.of("second_spell", "Second Spell", ctx -> Wave22Acts.apprenticeCast(ctx, 1)).cooldown(20))
                .act(Act.of("study", "Study", Wave22Acts::apprenticeStudy).cooldown(24000 * Wave22Rules.WEEK)));

        // ---------------------------------------------------------------- Superlunary Staff (rare, attunement)
        out.accept(RelicDef.of("superlunary_staff", "Superlunary Staff", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x7A5A38, 0xE8B848, 0xFFA848).look(Look.STAFF).melee(5.0, 1.4)
                .engrave("elemental_boost", "magic_interference", "swift")
                .desc("A bronze staff with orbiting gold rings round a small sun. Use for a Sunburst that dazzles everything near you for half again as long. You speak freely out of breath and out of silence, and see by starlight. Sneak-use projects the places the staff remembers; the third key remembers where you stand.")
                .passive(Passive.attr("minecraft:generic.oxygen_bonus", 8.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_SUPER))
                .act(Act.of("sunburst", "Sunburst", Wave22Acts::sunburst).cooldown(300).mana(2))
                .act(Act.of("project", "Project", Wave22Acts::project).cooldown(40))
                .act(Act.of("remember", "Remember", Wave22Acts::remember).cooldown(40)));

        // ---------------------------------------------------------------- The Bifrost (legendary, attunement)
        out.accept(RelicDef.of("the_bifrost", "The Bifrost", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().model3d().vessel()
                .palette(0xF2EEE0, 0xE8C860, 0xFFC8F0).look(Look.STAFF).melee(7.0, 1.6)
                .engrave("dead_end_rainbow", "swift", "transcendence")
                .desc("A white and gold quarterstaff trailing rainbow light. While you wield it your body is ringed with rainbows. Once a week use it to open a Gate: the rainbow bridge carries you and your companions home. Sneak-use toggles Rainbow Road: footprints of light and a little speed.")
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr(MAX_MAGICULE, 0.10, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.flag(F_BIFROST))
                .act(Act.of("gate", "Gate", Wave22Acts::gate).cooldown(24000 * Wave22Rules.GATE_WEEK).epPercent(2.0))
                .act(Act.of("rainbow_road", "Rainbow Road", ctx -> Wave0Util.note(ctx.player(), ctx.isOn("rainbow_road") ? "Rainbow Road: footprints of light." : "Rainbow Road ends.", net.minecraft.ChatFormatting.LIGHT_PURPLE)).toggle().cooldown(10)));

        // ---------------------------------------------------------------- Wand-o-Wisp (rare, no attunement)
        out.accept(RelicDef.of("wand_o_wisp", "Wand-o-Wisp", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0xC8D0D8, 0x48C8C0, 0xA8F0F0).look(Look.WAND).melee(3.0, 2.0)
                .engrave("magic_weapon", "swift", "restoration")
                .desc("A silver needle with a teal crystal grip and two fluttering wisps. Seven charges. Use for Faerie Fire (1 charge): foes around you glow and take 15% more damage. Sneak-use summons a will-o-wisp (3 charges) that fights for you for a minute. When it is empty it refills d8-1 at dawn; on a 0 it sulks.")
                .passive(Passive.skill("tensura:light_attack_resistance"))
                .act(Act.of("faerie_fire", "Faerie Fire", Wave22Acts::faerieFire).cooldown(100))
                .act(Act.of("summon_wisp", "Summon Wisp", Wave22Acts::summonWisp).cooldown(200)));

        // ---------------------------------------------------------------- Wand of Spider Summoning (uncommon, attunement)
        out.accept(RelicDef.of("wand_of_spider_summoning", "Wand of Spider Summoning", Grade.SPECIAL, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x2A2630, 0xE04AB8, 0xFF4AD0).look(Look.WAND).melee(5.0, 1.7)
                .engrave("enervation", "stagnation", "severance")
                .desc("A spiked black wand like a spider's leg with a magenta thorn blade. Eight charges, 1d6+1 back at dawn. Use to summon a spider (1 charge), sneak-use a swarm of cave spiders (2), the third key a great black spider (3), each for a minute.")
                .passive(Passive.skill("tensura:poison_resistance"))
                .act(Act.of("spider", "Spider", ctx -> Wave22Acts.brood(ctx, Wave22Rules.SPIDER)).cooldown(100))
                .act(Act.of("swarm", "Spider Swarm", ctx -> Wave22Acts.brood(ctx, Wave22Rules.SWARM)).cooldown(160))
                .act(Act.of("large_spider", "Large Spider", ctx -> Wave22Acts.brood(ctx, Wave22Rules.LARGE)).cooldown(300)));

        // ---------------------------------------------------------------- Windwheel Staff (no rarity printed, attunement)
        out.accept(RelicDef.of("windwheel_staff", "Windwheel Staff", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x5A6A8A, 0x7A2A22, 0xFFC84A).look(Look.STAFF).melee(5.0, 1.4)
                .engrave("elemental_boost", "magic_weapon", "swift")
                .desc("A blue-steel ring on a red-brown staff, a pinwheel crystal spinning inside. A +1 focus. Every spell you cast (magicules spent) stores wind charges equal to its level for a minute; use to discharge them as lightning, 1d6 each (a target that braces takes half).")
                .passive(Passive.attr(COST, -0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.skill("tensura:wind_attack_resistance"))
                .passive(Passive.skill("tensura:electricity_resistance"))
                .passive(Passive.flag(F_WIND))
                .act(Act.of("discharge", "Discharge", Wave22Acts::windDischarge).cooldown(60)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave22Events.register();
    }
}
