package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, second card set, wave 13: nine blades from the third card sheets (Dream Glass, Axe of The Crimson Saint, Palladion,
 * Dagger of Betrayal, Humbling Longsword, Mountainguard's Greataxe, Eschaton, Contingency Plan and the Time Bender Dadao). All nine are held in the
 * main hand and drawn as 3D models.
 * <p>
 * The first act of a blade runs on use, the second on sneak + use. The rules are in {@link Wave13Rules} (pure), the acts in {@link Wave13Acts},
 * the damage and tick handlers in {@link Wave13Events}; per-blade state (charges, tiers, the stored spell, the day's uses) is kept on the stack itself.
 */
public final class Wave13 {
    private Wave13() {}

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Dream Glass
        out.accept(RelicDef.of("dream_glass", "Dream Glass", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x7FC8F0, 0xA89A3A, 0x9FEFFF)
                .desc("A greatsword of blue stained glass that fades in and out of the world. Once a day its Ethereal Form lets blows pass through worn armour for two minutes; "
                        + "sneak + use draws it from nothing for a surprise blow.")
                .passive(Passive.flag("w13_dream_glass"))
                .act(Act.of("ethereal_form", "Ethereal Form", Wave13Acts::etherealForm).usesPerDay(1).cooldown(40))
                .act(Act.of("dream_draw", "Dream Draw", Wave13Acts::dreamDraw).needsSneak().cooldown(200))
                .melee(8.5, 1.2)
                .engrave("intangibility", "magic_weapon", "sturdy"));

        // ---------------------------------------------------------------- Axe of The Crimson Saint
        out.accept(RelicDef.of("axe_of_the_crimson_saint", "Axe of The Crimson Saint", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.AXE).palette(0x9A9EA6, 0xD8A838, 0xE03030)
                .desc("A cursed battle axe: wielding it costs a heart of maximum health. Each dawn it asks how much you will sacrifice: "
                        + "use offers a heart for more damage, sneak + hold gives everything for a great blow and a one in four critical.")
                .passive(Passive.attr("minecraft:generic.max_health", -Wave13Rules.CURSE_HP, AttributeModifier.Operation.ADD_VALUE))
                .passive(Passive.flag("w13_saint"))
                .act(Act.of("offer_blood", "Offer Blood", Wave13Acts::offerBlood).cooldown(20))
                .act(Act.of("todol", "Todol: Everything", Wave13Acts::everything).needsSneak().hold(40).cooldown(20))
                .melee(9.0, 1.0)
                .engrave("severance", "crushing", "energy_steal"));

        // ---------------------------------------------------------------- Palladion
        out.accept(RelicDef.of("palladion", "Palladion", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.SWORD).palette(0xB8BECC, 0xD4A83A, 0xFFF0B0)
                .desc("An indestructible blade forged against otherworldly foes. It hits celestials, fiends, fey, elementals and the undead for extra holy force, "
                        + "senses them, and a critical blow can banish one. \"Begone!\" drives them back.")
                .passive(Passive.flag("w13_palladion"))
                .passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("begone", "Begone!", Wave13Acts::begone).mana(8).cooldown(600))
                .act(Act.of("sentinel_sense", "Extraplanar Sentinel", Wave13Acts::sentinelSense).needsSneak().cooldown(200))
                .melee(10.5, 1.6)
                .engrave("holy_weapon", "barrier_piercing", "sturdy", "transcendence"));

        // ---------------------------------------------------------------- Dagger of Betrayal
        out.accept(RelicDef.of("dagger_of_betrayal", "Dagger of Betrayal", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x8A4A9E, 0xC9A24A, 0xFF3A4A)
                .desc("A spiralling tri-bladed dagger with three red crystals. It bites harder when the victim looks you in the eye, "
                        + "and far harder against one who trusts you, at the price of a sour conscience.")
                .passive(Passive.flag("w13_betrayal"))
                .melee(5.0, 2.2)
                .engrave("soul_eater", "enervation", "swift"));

        // ---------------------------------------------------------------- Humbling Longsword
        out.accept(RelicDef.of("humbling_longsword", "Humbling Longsword", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xA9AEB8, 0x2A86D8, 0xCFE8FF)
                .desc("A plain steel longsword that humbles its foes: a creature it has beaten low gives up the fight. Held in two hands it cuts deeper. "
                        + "Use demands the surrender of every wounded foe near you.")
                .passive(Passive.flag("w13_humbling"))
                .act(Act.of("demand_surrender", "Demand Surrender", Wave13Acts::demandSurrender).mana(3).cooldown(300))
                .melee(6.5, 1.6)
                .engrave("sturdy", "enervation"));

        // ---------------------------------------------------------------- Mountainguard's Greataxe
        out.accept(RelicDef.of("mountainguards_greataxe", "Mountainguard's Greataxe", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.AXE).palette(0x9AA0A8, 0xC98A2A, 0xFFD25A)
                .desc("A dwarven-forged double-bladed greataxe that stands its ground. Once a day, slamming its handle sends out a wave of gold: "
                        + "for a minute allies near you cannot be frightened and keep a shield.")
                .passive(Passive.flag("w13_mountain"))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.3, AttributeModifier.Operation.ADD_VALUE))
                .act(Act.of("last_line", "Last Line of Defense", Wave13Acts::lastLine).usesPerDay(1).cooldown(100))
                .melee(9.0, 0.9)
                .engrave("crushing", "sturdy", "vigor"));

        // ---------------------------------------------------------------- Eschaton
        out.accept(RelicDef.of("eschaton", "Eschaton", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.RAPIER).palette(0xDDE6EE, 0x3E9A4A, 0x6FFFE0)
                .desc("A rapier of arcane-conductive metal that charges with the magicules you spend on spells and lets them go in five gifts: "
                        + "arcane edge, whip reach, a second strike, force damage and a cantrip. The charge fades at dawn.")
                .passive(Passive.flag("w13_eschaton"))
                .act(Act.of("read_charge", "Read the Charge", Wave13Acts::readCharge).cooldown(10))
                .act(Act.of("discharge", "Discharge", Wave13Acts::discharge).needsSneak().mana(4).cooldown(400))
                .melee(7.5, 1.8)
                .engrave("magic_weapon", "magic_interference", "elemental_boost", "magic_capacity", "swift"));

        // ---------------------------------------------------------------- Contingency Plan
        out.accept(RelicDef.of("contingency_plan", "Contingency Plan", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x8A3A3A, 0xB06A9A, 0x8FE8FF)
                .desc("A winged dagger that holds one potion's spell in reserve. Use picks the circumstance, sneak + use with a potion in the off-hand stores it; "
                        + "when the circumstance comes it takes effect on you by itself, and the dagger rests until dawn.")
                .passive(Passive.flag("w13_contingency"))
                .act(Act.of("set_circumstance", "Set the Circumstance", Wave13Acts::setCircumstance).cooldown(6))
                .act(Act.of("cast_into_blade", "Cast into the Blade", Wave13Acts::castIntoBlade).needsSneak().cooldown(10))
                .melee(5.5, 2.2)
                .engrave("magic_weapon", "restoration", "swift"));

        // ---------------------------------------------------------------- Time Bender Dadao
        out.accept(RelicDef.of("time_bender_dadao", "Time Bender Dadao", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x15171C, 0xE8E8EE, 0x3FD0FF)
                .desc("A black dadao with a vein of cyan light, bound to you alone. Its tiers open with the time you carry it in hand: "
                        + "one day for Slow Time, three days for the Dimension Toss (sneak + use).")
                .passive(Passive.flag("w13_dadao"))
                .act(Act.of("slow_time", "Slow Time", Wave13Acts::slowTime).cooldown(100))
                .act(Act.of("dimension_toss", "Dimension Toss", Wave13Acts::dimensionToss).needsSneak().cooldown(100))
                .melee(8.5, 1.1)
                .engrave("stagnation", "lethargy", "severance"));
    }

    /** Registers the damage, critical-hit and tick rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        Wave13Events.register();
    }
}
