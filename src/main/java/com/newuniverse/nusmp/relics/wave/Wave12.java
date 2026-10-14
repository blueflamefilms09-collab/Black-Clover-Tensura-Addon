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
 * Relics of Many Worlds, second set, wave 12: ten hand-held weapons, each a real 3D model in hand.
 * <ul>
 *   <li>Giant Jawblade (RARE): a bone greatsword you charge up to three times before the blow.</li>
 *   <li>The Lumberjack (RARE): a battleaxe that fells a whole natural tree and drops it on whoever stands in the way.</li>
 *   <li>White Whip of Welting (RARE): a towel with reach; wet, it humiliates.</li>
 *   <li>Whirling Blades (RARE): paired swords, one cold and one hot, with a spin attack.</li>
 *   <li>Warmage's Longsword (UNIQUE, attuned): five arcane missiles a day.</li>
 *   <li>Sword Crystal (RARE): a crystalline arm blade, four charges, moonlit recharge.</li>
 *   <li>The Stabshroom (RARE): a poisonous fungal dagger with a natural 1 and a natural 20.</li>
 *   <li>Twin Hook Swords (NORMAL): they sweep a leg and hold a climber to the wall.</li>
 *   <li>Bottle Blade (RARE): a bottle whose water shoots out and freezes.</li>
 *   <li>Disintegration Dagger (RARE): what it kills blows apart.</li>
 * </ul>
 * The long card texts are in the lang fragment; the tabletop-to-game translation is in doc_wave12.md.
 */
public final class Wave12 {
    private Wave12() {}

    public static void defs(Consumer<RelicDef> out) {
        out.accept(jawblade());
        out.accept(lumberjack());
        out.accept(whip());
        out.accept(blades());
        out.accept(warmage());
        out.accept(crystal());
        out.accept(stabshroom());
        out.accept(hookSwords());
        out.accept(bottle());
        out.accept(dagger());
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor, through the catalog). */
    public static void hooks() {
        Wave12Hooks.register();
        com.newuniverse.nusmp.relics.pair.PairEffects.onOffHand(Wave12Light.BLADES, Wave12Light::bladeOffHand);          // 1.29.0: the off-hand blade's own bite
        com.newuniverse.nusmp.relics.pair.PairEffects.onOffHand(Wave12Light.HOOKS, Wave12Light::hookOffHand);
    }

    // ---------------------------------------------------------------- the weapons
    private static RelicDef jawblade() {
        return RelicDef.of("giant_jawblade", "Giant Jawblade", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xD9B26B, 0x4A4038, 0xFFE0A0)
                .desc("A slab of jawbone, too heavy to swing quickly. Right-click to charge it up to three times (you move slowly while you do): the next blow hits 33, 67 or 100 percent harder and shoves the foe back.")
                .passive(Passive.flag(Wave12Heavy.JAW_FLAG))
                .act(Act.of("charge", "Charge the Jawblade", Wave12Heavy::charge).cooldown(20))
                .melee(9.0, 0.9)
                .engrave("crushing", "severance");
    }

    private static RelicDef lumberjack() {
        return RelicDef.of("the_lumberjack", "The Lumberjack", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0xA9AFB4, 0x7A5230, 0xFFFFFF)
                .desc("\"I'm a lumberjack and I'm okay.\" Right-click a natural tree to fell it the way you face; foes in the line of the falling trunk are crushed, harder the further from the stump.")
                .passive(Passive.flag(Wave12Heavy.AXE_FLAG))
                .act(Act.of("fell_tree", "Fell the Tree", Wave12Heavy::fell).cooldown(100))
                .melee(8.0, 1.0)
                .engrave("severance", "sturdy");
    }

    private static RelicDef whip() {
        return RelicDef.of("white_whip_of_welting", "White Whip of Welting", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xE4E4E0, 0xC83C3C, 0x6FA0FF)
                .desc("A tightly wound towel with a long reach. Right-click snaps it at a foe; sneak and use it near water to soak it, and a wet towel stings, humiliates and weakens.")
                .passive(Passive.flag(Wave12Light.WHIP_FLAG))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.5, Operation.ADD_VALUE))
                .act(Act.of("snap", "Snap the Towel", Wave12Light::snap).cooldown(20))
                .act(Act.of("soak", "Soak the Towel", Wave12Light::soak).cooldown(40).needsSneak())
                .melee(4.0, 2.0)
                .engrave("swift", "breathing_support");
    }

    private static RelicDef blades() {
        return RelicDef.of("whirling_blades", "Whirling Blade (Right)", Grade.RARE, RelicSlot.MAIN_HAND)
                .pair("Whirling Blade (Left)")
                .model3d().look(Look.SWORD).palette(0x4A4A52, 0x3FE0D0, 0xFF6A4A)
                .desc("One of two curved swords that come as a pair, this one hot (its twin is cold): a blow carries a flame, and with the other in your off hand a second blow follows with its chill. Right-click joins their ends in a spin attack that hits everything around you.")
                .passive(Passive.flag(Wave12Light.BLADES_FLAG))
                .act(Act.of("spin", "Spin Attack", Wave12Light::spin).cooldown(80))
                .melee(5.0, 2.0)
                .engrave("severance", "swift");
    }

    private static RelicDef warmage() {
        return RelicDef.of("warmages_longsword", "Warmage's Longsword", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xC9CED8, 0xD4A838, 0xB070FF)
                .desc("A silver longsword set with five violet gems. +2 force on every blow, a keener sense of magic, and five Magic Missiles a day: right-click fires one, sneak and use fires every missile left.")
                .passive(Passive.flag(Wave12Magic.WARMAGE_FLAG))
                .passive(Passive.skill(Wave5Skills.MAGIC_SENSE))
                .passive(Passive.attr("tensura:max_magicule", 0.05, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("magic_missile", "Magic Missile", c -> Wave12Magic.missiles(c, false)).cooldown(10))
                .act(Act.of("arcane_barrage", "Arcane Barrage", c -> Wave12Magic.missiles(c, true)).cooldown(40).needsSneak())
                .melee(9.0, 1.6)
                .engrave("magic_weapon", "magic_interference", "barrier_piercing");
    }

    private static RelicDef crystal() {
        return RelicDef.of("sword_crystal", "Sword Crystal", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x5EC8F0, 0xE8C8A8, 0xBFF4FF)
                .desc("\"Stay cool, Bird Boy.\" Right-click pricks your finger and encases the arm in crystal for a minute: 273 HP of icy blade that chills what it cuts. Four charges, one back each moonlit night. When it breaks you take 2d4 cold.")
                .passive(Passive.flag(Wave12Magic.CRYSTAL_FLAG))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .act(Act.of("prick", "Prick the Crystal", Wave12Magic::prick).cooldown(40))
                .act(Act.of("melt", "Let It Melt", Wave12Magic::melt).cooldown(20).needsSneak())
                .melee(5.0, 1.8)
                .engrave("elemental_boost", "stagnation");
    }

    private static RelicDef stabshroom() {
        return RelicDef.of("the_stabshroom", "The Stabshroom", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xB8221E, 0x9CFF3C, 0x6CFF3C)
                .desc("A fungal dagger that glows and poisons. Every blow adds poison; a natural 20 stabs twice as deep and leaves the foe poisoned and weakened, a natural 1 bursts the spores in everyone's face. Its wielder resists poison.")
                .passive(Passive.flag(Wave12Magic.SHROOM_FLAG))
                .passive(Passive.skill(Wave5Skills.POISON_RESIST))
                .melee(5.0, 2.4)
                .engrave("ruination", "vitality");
    }

    private static RelicDef hookSwords() {
        return RelicDef.of("twin_hook_swords", "Twin Hook Sword (Right)", Grade.NORMAL, RelicSlot.MAIN_HAND)
                .pair("Twin Hook Sword (Left)")
                .model3d().look(Look.SWORD).palette(0xB4BCC6, 0x1F6E5E, 0xCFE8E0)
                .desc("One of a pair of hook swords. Right-click sweeps the leg of a two-legged or winged foe and drags it off its feet; with both swords in your hands a second blow follows each strike, and pushed against a wall the hooks hold you and let you climb a short way (sneak to hang still).")
                .passive(Passive.flag(Wave12Light.HOOKS_FLAG))
                .act(Act.of("sweep_leg", "Sweep the Leg", Wave12Light::sweepLeg).cooldown(80))
                .melee(3.8, 2.4)
                .engrave("swift", "sturdy");
    }

    private static RelicDef bottle() {
        return RelicDef.of("bottle_blade", "Bottle Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x2F8F6E, 0xB8C0C8, 0x6FF0FF)
                .desc("A green bottle with a blade of water. Right-click shakes it open: a long stream shoots out and freezes the foes in its line. Sneak and use it at water to refill, or, while full, to Control Water and throw a wave round you. An empty bottle is only a bottle.")
                .passive(Passive.flag(Wave12Magic.BOTTLE_FLAG))
                .act(Act.of("stream", "Shake and Open", Wave12Magic::stream).cooldown(60))
                .act(Act.of("control_water", "Control Water", Wave12Magic::control).cooldown(40).needsSneak())
                .melee(5.0, 1.8)
                .engrave("elemental_boost", "breathing_support");
    }

    private static RelicDef dagger() {
        return RelicDef.of("disintegration_dagger", "Disintegration Dagger", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x2A2220, 0x5A3A2C, 0x58B4FF)
                .desc("A jagged black dagger. What it kills crumbles to ash, and a moment later the body blows apart, scorching the foes around it.")
                .passive(Passive.flag(Wave12Magic.DAGGER_FLAG))
                .melee(5.0, 2.4)
                .engrave("ruination", "severance");
    }
}
