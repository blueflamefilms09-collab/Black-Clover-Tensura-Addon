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
 * Relics of Many Worlds, second set, wave 11: ten blades and a crossbow (Wavebreaker, Encircled Blade, Windcutter, Tripolar Sword, the Dagger of
 * Many Daggers, Flamecutter, Heart of the Flame, Bianca, Sorrow's Edge, the Dragon's Truth). Every one is held in the main hand and drawn as a
 * 3D model. The first act runs on use, the second on sneak + use (for the crossbow the only act is the reload on sneak + use). The act bodies are in
 * {@link Wave11Acts}, the damage, tick and arrow rules in {@link Wave11Events}, the dice in {@link Wave11Pure}.
 */
public final class Wave11 {
    private Wave11() {}

    static final String F_WAVE = "w11_wavebreaker", F_WIND = "w11_windcutter", F_TRIPOLAR = "w11_tripolar", F_FLAME = "w11_flamecutter",
            F_HEART = "w11_heart_of_the_flame", F_BIANCA = "w11_bianca", F_SORROW = "w11_sorrows_edge";

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Wavebreaker (very rare longsword, attunement)
        out.accept(RelicDef.of("wavebreaker", "Wavebreaker", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x3E7F93, 0x2A2A55, 0x7FE6FF)
                .desc("A long scaled blade forged in the deep sea. You breathe and swim as fast as you walk under water, and Shape Water freezes a patch of still water. "
                        + "Mystic Reflection raises a watery double for a minute that turns a blow aside each second and adds cold to your strikes (once a day).")
                .passive(Passive.effect("minecraft:water_breathing", 0))
                .passive(Passive.skill("tensura:water_breathing"))
                .passive(Passive.flag(F_WAVE))
                .act(Act.of("mystic_reflection", "Mystic Reflection", Wave11Acts::reflection).usesPerDay(1).cooldown(100).mana(6))
                .act(Act.of("shape_water", "Shape Water", Wave11Acts::shapeWater).needsSneak().cooldown(40))
                .melee(9.0, 1.6)
                .engrave("elemental_boost", "swift", "sturdy"));

        // ---------------------------------------------------------------- Encircled Blade
        out.accept(RelicDef.of("encircled_blade", "Encircled Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x2FC4C0, 0x2450C8, 0x7CFFF0)
                .desc("A slim turquoise blade with a black sphere in its guard. Twice a day it shuts a sphere of shimmering force round the creature in sight: a foe is held fast, a friend is sheltered.")
                .passive(Passive.flag("w11_encircled_blade"))
                .act(Act.of("encircle", "Encircle", Wave11Acts::encircle).usesPerDay(2).cooldown(100).mana(5))
                .melee(6.0, 1.8)
                .engrave("magic_weapon", "swift"));

        // ---------------------------------------------------------------- Windcutter (legendary longsword, attunement)
        out.accept(RelicDef.of("windcutter", "Windcutter", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.SWORD).palette(0xD8DCEA, 0x2FBBB0, 0x9FFFF0)
                .desc("A feather-light silver blade with winged guards, a gift of an air elemental lord. Its bearer can fly and hover, and falls softly. "
                        + "Control Winds, once a day, is a gust that throws foes and missiles back, or, sneaking, an updraft that lifts you and your allies.")
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 10.0, ADD))
                .passive(Passive.attr("minecraft:generic.attack_speed", 0.2, MUL))
                .passive(Passive.skill("tensura:flight"))
                .passive(Passive.skill("tensura:wind_attack_resistance"))
                .passive(Passive.flag(F_WIND))
                .act(Act.of("control_winds", "Control Winds", Wave11Acts::controlWinds).usesPerDay(1).cooldown(200).mana(8))
                .melee(11.0, 2.0)
                .engrave("swift", "magic_weapon", "sturdy", "elemental_boost"));

        // ---------------------------------------------------------------- Tripolar Sword (legendary, cursed)
        out.accept(RelicDef.of("tripolar_sword", "Tripolar Sword", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .vessel().model3d().look(Look.SWORD).palette(0x6E7078, 0xB8BCC8, 0x9AA8D8)
                .desc("A marbled blade with a four-pointed star guard, abandoned by its owner and bitter for it. Each dawn it rolls a die: it lashes out at you and is clumsy all day, "
                        + "behaves as a plain sword, or strikes far harder.")
                .passive(Passive.flag(F_TRIPOLAR))
                .melee(10.0, 1.6)
                .engrave("severance", "ruination", "magic_weapon"));

        // ---------------------------------------------------------------- The Dagger of Many Daggers (very rare, attunement)
        out.accept(RelicDef.of("the_dagger_of_many_daggers", "The Dagger of Many Daggers", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xC9D0DA, 0xE0A830, 0x4A5C88)
                .desc("A leaf-bladed dagger with a gold ring that pulls copies of itself out of the throws that might have been. "
                        + "Temporal Loop sends two to eight copies at the foe in sight (a fourth of them fall short).")
                .passive(Passive.flag("w11_many_daggers"))
                .act(Act.of("temporal_loop", "Temporal Loop", Wave11Acts::temporalLoop).cooldown(600).mana(4))
                .melee(5.0, 2.4)
                .engrave("swift", "magic_weapon", "intangibility"));

        // ---------------------------------------------------------------- Flamecutter (legendary longsword, attunement)
        out.accept(RelicDef.of("flamecutter", "Flamecutter", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.SWORD).palette(0xB83A1C, 0xE0A030, 0xFF8A2A)
                .desc("A flame-shaped blade with a fire lord in it, warm to the touch. Its bearer is immune to fire, walks on lava, and every blow burns. "
                        + "Wall of Fire, once a day, stands a burning wall before you.")
                .passive(Passive.effect("minecraft:fire_resistance", 0))
                .passive(Passive.resist("minecraft:is_fire", 0.9))
                .passive(Passive.skill("tensura:flame_attack_nullification"))
                .passive(Passive.flag(F_FLAME))
                .act(Act.of("wall_of_fire", "Wall of Fire", Wave11Acts::wallOfFire).usesPerDay(1).cooldown(400).mana(10))
                .melee(11.0, 1.8)
                .engrave("elemental_boost", "magic_weapon", "sturdy", "severance"));

        // ---------------------------------------------------------------- Heart of the Flame (greatsword, attunement)
        out.accept(RelicDef.of("heart_of_the_flame", "Heart of the Flame", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xF5A623, 0x4A4E58, 0xFFB030)
                .desc("A huge greatsword whose blade is a row of floating golden plates. Use speaks the command word: the blade heats and every hit burns, until you sheathe it or speak again.")
                .passive(Passive.attr("minecraft:generic.movement_speed", -0.04, MUL))
                .passive(Passive.flag(F_HEART))
                .act(Act.of("burn", "Burn", Wave11Acts::burn).cooldown(20).mana(2))
                .melee(11.0, 0.9)
                .engrave("crushing", "elemental_boost", "sturdy"));

        // ---------------------------------------------------------------- Bianca (heavy repeater crossbow)
        out.accept(RelicDef.of("bianca", "Bianca", Grade.RARE, RelicSlot.MAIN_HAND)
                .crossbow().model3d().look(Look.BOW).palette(0x7A4A28, 0xC89A3C, 0xB8BCC4)
                .desc("A dwarven heavy repeater: six cylinders of three bolts feed a volley of three in every shot, and a bayonet is fixed to the front. Sneak + use reloads all cylinders from the arrows in your pack.")
                .passive(Passive.flag(F_BIANCA))
                .act(Act.of("reload_cylinders", "Reload Cylinders", Wave11Acts::reload).needsSneak().cooldown(20))
                .melee(5.0, 1.4)
                .engrave("sturdy", "magic_weapon"));

        // ---------------------------------------------------------------- Sorrow's Edge (cursed axe)
        out.accept(RelicDef.of("sorrows_edge", "Sorrow's Edge", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0x1A1A22, 0xC8A850, 0x6A8CC8)
                .desc("A crescent starry axe whose chaotic spirit speaks to its wielder. Each blow adds necrotic harm. Each day it rolls: at 50 or above you keep your sanity, below it the spirit makes a demand (blood or a fast).")
                .passive(Passive.flag(F_SORROW))
                .melee(10.0, 0.9)
                .engrave("crushing", "enervation", "soul_eater"));

        // ---------------------------------------------------------------- The Dragon's Truth (eight dragon-tooth daggers in a belt of returning)
        out.accept(RelicDef.of("the_dragons_truth", "The Dragons Truth", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xEDE6D0, 0x7A4A28, 0xFFD080)
                .desc("Eight dragon-tooth daggers in a belt of returning. Use throws one at the foe in sight and a d8 draconic breath blasts through it; once a day, sneak + use makes the creature in front of you answer.")
                .passive(Passive.flag("w11_dragons_truth"))
                .act(Act.of("throw_tooth", "Throw a Tooth", Wave11Acts::throwTooth).cooldown(30))
                .act(Act.of("extraction", "Extraction", Wave11Acts::extraction).needsSneak().usesPerDay(1).cooldown(200).mana(6))
                .melee(4.5, 2.2)
                .engrave("swift", "magic_weapon", "elemental_boost"));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave11Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave11Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave11Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave11Events::onUseFinish);
        NeoForge.EVENT_BUS.addListener(Wave11Events::onJoin);
        NeoForge.EVENT_BUS.addListener(Wave11Events::onLogout);
    }
}
