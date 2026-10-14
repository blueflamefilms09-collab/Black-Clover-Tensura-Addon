package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, second card set, wave 9: ten hand-held weapons, every one a 3D model in hand. The Hyperborean Crossbow (a real crossbow),
 * the Druidic Blade, the Axe of the Exile, Astralaria, the Null Blade, the Viper Whip, Caesar's Gladius, the Twin Swords of Deflection, Hunger
 * and Thornblight.
 * <p>
 * How the abilities are wired: the first act of a weapon runs on use (on sneak + use for the crossbow), the second on sneak + use. The acts are in
 * {@link Wave9Acts}, the rules that need the game bus (bonuses on a blow, curses, patches of frost, rebelling gladiators) in {@link Wave9Events}, the
 * numbers in the pure {@link Wave9Rules}. Tabletop becomes Minecraft: a short rest is uses per day, a saving throw is a boss's halved effect, a
 * die is a flat bonus sized to the grade.
 */
public final class Wave9 {
    private Wave9() {}

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Hyperborean Crossbow
        out.accept(RelicDef.of("hyperborean_crossbow", "Hyperborean Crossbow", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().crossbow().look(Look.BOW).palette(0x2B2F3A, 0x9AA3B0, 0x5FB8FF)
                .desc("A slim crossbow of the far north with a bowstring of blue light. Its bolts hit one point harder and slow what they strike. "
                        + "Sneak + use arms the next bolt to freeze the ground where it lands.")
                .passive(Passive.skill("tensura:cold_resistance"))
                .passive(Passive.skill("tensura:freeze"))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.flag("w9_hyperborean"))
                .act(Act.of("frost_shot", "Flash Freeze: Surface", Wave9Acts::frostShot).mana(3).cooldown(100))
                .engrave("elemental_boost", "magic_weapon", "lethargy"));

        // ---------------------------------------------------------------- Druidic Blade
        out.accept(RelicDef.of("druidic_blade", "Druidic Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x2F8FA0, 0xE8F0F8, 0x42F5E6)
                .desc("A leaf-shaped blade handed down by the elven druids. Three times a day it calls grass and vines up round a point you look at "
                        + "and holds everything there in place.")
                .passive(Passive.skill("tensura:poison_resistance"))
                .passive(Passive.flag("w9_druidic"))
                .act(Act.of("entangle", "Entangle", Wave9Acts::entangle).usesPerDay(3).cooldown(100))
                .melee(5.5, 1.8)
                .engrave("growth", "vitality", "swift"));

        // ---------------------------------------------------------------- Axe of the Exile
        out.accept(RelicDef.of("axe_of_the_exile", "Axe of the Exile", Grade.SPECIAL, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0x6B3A2A, 0xD8D2C0, 0xB07040)
                .desc("A bone-hafted handaxe that you throw and that comes back to your hand. It carries the Exile's curse: near your bed or the world spawn "
                        + "it wears you out; far from home it lightens your step.")
                .passive(Passive.flag("w9_exile"))
                .act(Act.of("exile_throw", "Throw the Axe", ctx -> Wave9Acts.throwAxe(ctx.player(), 6f, 20)).cooldown(50))
                .melee(5.0, 1.9)
                .engrave("sturdy", "swift"));

        // ---------------------------------------------------------------- Astralaria
        out.accept(RelicDef.of("astralaria", "Astralaria", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.AXE).palette(0x2A2D6B, 0xC8963A, 0x8E6BFF)
                .desc("A brass battleaxe filled with the night sky. Every blow adds radiant damage, you see through every illusion, and a cosmic trail "
                        + "follows the blade. Sneak + use scatters stars under your steps.")
                .passive(Passive.skill("tensura:eye_of_truth"))
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.flag("w9_astralaria"))
                .act(Act.of("astral_sweep", "Astral Sweep", Wave9Acts::astralSweep).mana(6).cooldown(240))
                .act(Act.of("footprints", "Cosmic Footprints", Wave9Acts::footprints).needsSneak().toggle().cooldown(10))
                .melee(11.0, 1.0)
                .engrave("holy_weapon", "severance", "magic_interference"));

        // ---------------------------------------------------------------- Null Blade
        out.accept(RelicDef.of("null_blade", "Null Blade", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.SWORD).palette(0x14121A, 0xB8C4D0, 0xC02030)
                .desc("A black greatsword with an eye in its cross. It frightens the weak, deals cold, eats maximum health and gives it to you as warding. "
                        + "After twenty lives it awakens. Anyone but its owner drops it.")
                .passive(Passive.flag("w9_null_blade"))
                .act(Act.of("blind_deafen", "Blindness / Deafness", Wave9Acts::blindDeafen).cooldown(100))
                .act(Act.of("agathys", "Armor of Agathys", Wave9Acts::agathys).needsSneak().usesPerDay(1).cooldown(40))
                .melee(11.0, 1.1)
                .engrave("enervation", "soul_eater", "stagnation"));

        // ---------------------------------------------------------------- Viper Whip
        out.accept(RelicDef.of("viper_whip", "Viper Whip", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x4E5A2E, 0x2B2A22, 0x6BFF7A)
                .desc("A whip of snakeskin that ends in a viper's head. It reaches further and poisons. Once a day the viper bites: "
                        + "for ten minutes you feel which way the bitten one lies.")
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.5, AttributeModifier.Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:poison_resistance"))
                .passive(Passive.flag("w9_viper"))
                .act(Act.of("viper_bite", "Viper's Bite", Wave9Acts::viperBite).usesPerDay(1).cooldown(40))
                .melee(4.5, 1.8)
                .engrave("swift", "enervation"));

        // ---------------------------------------------------------------- Caesar's Gladius
        out.accept(RelicDef.of("caesars_gladius", "Caesar's Gladius", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xB8BCC4, 0x7A3A34, 0xFFE9A0)
                .desc("A marbled gladius with a feathered guard. Once every five days it calls three gladiators for five minutes. "
                        + "Once in ten calls they remember the Republic, and turn on you.")
                .passive(Passive.flag("w9_gladius"))
                .act(Act.of("conjure_gladiators", "Call the Gladiators", Wave9Acts::conjure).mana(5).cooldown(Wave9Rules.GLADIUS_COOLDOWN))
                .melee(5.5, 1.8)
                .engrave("sturdy", "severance"));

        // ---------------------------------------------------------------- Twin Swords of Deflection
        out.accept(RelicDef.of("twin_swords_of_deflection", "Twin Swords of Deflection", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xDCE0EC, 0x6A5CA8, 0xB8A8FF)
                .desc("A matched pair of hooked blades, drawn as one. Both land with each blow, a tenth of blows are critical, and the wielder is quicker. "
                        + "Use to parry: missiles turn back on their sender.")
                .passive(Passive.attr("minecraft:generic.attack_speed", 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.flag("w9_twin_swords"))
                .act(Act.of("deflect", "Deflect", Wave9Acts::deflect).mana(2).cooldown(120))
                .melee(5.0, 2.0)
                .engrave("swift", "magic_weapon", "severance_protection"));

        // ---------------------------------------------------------------- Hunger
        out.accept(RelicDef.of("hunger", "Hunger", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .vessel().model3d().look(Look.AXE).palette(0xC9CBD0, 0x6B4A32, 0xD03A3A)
                .desc("A sentient greataxe with a mouth for a blade. It adds acid to every blow. Three times a day it devours a hit and gives you the blood as "
                        + "temporary health. If it goes hungry for three minutes it gnaws at you.")
                .passive(Passive.skill("tensura:danger_sense"))
                .passive(Passive.flag("w9_hunger"))
                .act(Act.of("devour", "Devouring", Wave9Acts::devour).usesPerDay(3).cooldown(20))
                .melee(12.0, 0.9)
                .engrave("energy_steal", "soul_eater", "crushing", "ruination"));

        // ---------------------------------------------------------------- Thornblight
        out.accept(RelicDef.of("thornblight", "Thornblight", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0x1F2128, 0x6B4A2E, 0x35E08A)
                .desc("A thorn-hafted handaxe you can throw. Four times a day, thorns grow in the wound of the foe you struck last and fester. "
                        + "Its curse: every blow that lands costs you a point of health.")
                .passive(Passive.flag("w9_thornblight"))
                .act(Act.of("thorn_throw", "Throw the Axe", ctx -> {
                    var t = Wave9Acts.throwAxe(ctx.player(), 7f, 18);
                    if (t != null) Wave9Events.price(ctx.player(), t);
                }).cooldown(50))
                .act(Act.of("spiteful_thorns", "Spiteful Thorns", Wave9Acts::spitefulThorns).needsSneak().usesPerDay(4).cooldown(60))
                .melee(8.0, 1.5)
                .engrave("crushing", "growth", "ruination"));
    }

    /** Registers the damage, projectile, death, tick and join rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        NeoForge.EVENT_BUS.addListener(Wave9Events::onJoin);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onImpact);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave9Events::onStopping);
    }
}
