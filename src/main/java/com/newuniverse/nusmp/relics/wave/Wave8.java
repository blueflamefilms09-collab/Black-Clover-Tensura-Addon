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
 * Relics of Many Worlds, second card set, wave 8: ten blades and axes (Spotlight, Duality Blade, Frostfang, Soul Shatter Blade, Buster Sword,
 * Dragon Tooth Blade [Red], Corpse Eater, Random Axe of Charity, Leviathan Axe, Shadow Burn). The core registers the items; this class describes
 * them, {@link Wave8Acts} plays their active abilities and {@link Wave8Events} their passive ones. The rules live in {@link Wave8Rules}.
 * <p>
 * Tabletop rules became: attack bonuses and extra damage dice, flat damage added to the blow (capped against players); saving throws, a roll
 * against a percentage that falls with the target's toughness (mobs resist more than players, bosses always resist at least three times in four);
 * "advantage", a guaranteed stronger next blow; short rests, a cooldown; long rests, uses per Minecraft day; reach in feet, blocks; conditions
 * (paralysed, frightened), Tensura's paralysis effect plus a complete slow, with the balance law's duration caps.
 */
public final class Wave8 {
    private Wave8() {}

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ---- Spotlight: +1 shortsword; the card's curse is the need to be seen
        out.accept(RelicDef.of("spotlight", "Spotlight", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xF2E27A, 0x1B1B20, 0xFFF2A0)
                .desc("A shortsword whose blade is a beam of stage light. You cannot help but be the centre of attention: it keeps you glowing. Once a day a beam finds you, you see in the dark and your next blow lands true.")
                .passive(Passive.effect("minecraft:glowing", 0))
                .passive(Passive.flag(Wave8Events.F_SPOT))
                .act(Act.of("spotlight_beam", "Spotlight", Wave8Acts::spotlightBeam).usesPerDay(1).cooldown(40))
                .melee(5.5, 1.9)
                .engrave("holy_weapon", "swift", "magic_weapon"));

        // ---- Duality Blade: necrotic and radiant
        out.accept(RelicDef.of("duality_blade", "Duality Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x17151D, 0xB9A8F0, 0xE6DAFF)
                .desc("A matte black shortsword that hides in the dark. In dim light it strikes with unseen, withering force; switch on its streamers of light and it shines, sets the foe aglow and burns the undead.")
                .passive(Passive.flag(Wave8Events.F_DUAL))
                .act(Act.of("streamers", "Streamers of Light", Wave8Acts::streamers).toggle().cooldown(20))
                .melee(6.0, 2.0)
                .engrave("holy_weapon", "soul_eater", "swift"));

        // ---- Frostfang: legendary battleaxe
        out.accept(RelicDef.of("frostfang", "Frostfang", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.AXE).palette(0x86CDE8, 0x2C6F8F, 0xC8F4FF)
                .desc("A battleaxe with a horned, frost-rimed skull for a head. Every blow adds cold and chills the foe. Right-click casts a Cone of Cold (it costs existence points and needs a rest); sneak-click toggles Footfalls of Ice, which freeze water under your steps.")
                .passive(Passive.skill("tensura:cold_resistance"))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.flag(Wave8Events.F_FROST))
                .act(Act.of("cone_of_cold", "Cone of Cold", Wave8Acts::coneOfCold).cooldown(2400).epPercent(1.0))
                .act(Act.of("footfalls", "Footfalls of Ice", Wave8Acts::footfalls).toggle().cooldown(20))
                .melee(11.0, 1.0)
                .engrave("elemental_boost", "crushing", "sturdy"));

        // ---- Soul Shatter Blade: cursed, soul fragments
        out.accept(RelicDef.of("soul_shatter_blade", "Soul Shatter Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x2A1014, 0x8A1E1E, 0xFF5A2A)
                .desc("A cursed lich's greatsword that drinks the dying: a kill heals you by the harm you dealt (the surplus becomes temporary hearts) and adds a soul fragment that stays with you. Fragments wake Inflict Wounds (5), a dark aura (10), a third charge (15), Animate Dead (20), the lich's voice (25) and its call (30). Golden apples cleanse.")
                .passive(Passive.flag(Wave8Events.F_SSB))
                .act(Act.of("inflict_wounds", "Inflict Wounds", Wave8Acts::inflictWounds).cooldown(30))
                .act(Act.of("animate_dead", "Animate Dead", Wave8Acts::animateDead).cooldown(60))
                .melee(8.0, 1.5)
                .engrave("soul_eater", "energy_steal", "enervation"));

        // ---- Buster Sword: the plain slab
        out.accept(RelicDef.of("buster_sword", "Buster Sword", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0x9CA2AA, 0x7A1F1F, 0xD8E2F0)
                .desc("A huge slab of steel with a red-wrapped grip and two materia holes. Hold it with both hands (an empty off hand): a full-strength blow is heavy. Braver leaps and lands in a shockwave; sneak-click for a Cross-Slash.")
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.2, ADD))
                .passive(Passive.flag(Wave8Events.F_BUSTER))
                .act(Act.of("braver", "Braver", Wave8Acts::braver).cooldown(240).mana(4))
                .act(Act.of("cross_slash", "Cross-Slash", Wave8Acts::crossSlash).cooldown(120).mana(3))
                .melee(11.5, 1.0)
                .engrave("crushing", "sturdy", "slotting"));

        // ---- Dragon Tooth Blade [Red]
        out.accept(RelicDef.of("dragon_tooth_blade_red", "Dragon Tooth Blade [Red]", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.SWORD).palette(0xE4E0D6, 0x9E1B1B, 0xFF4A2A)
                .desc("A long, thin, strongly curved fang of a blade. Two blows in a row on the same foe make it burst into flame. Right-click lights the fang for ten seconds: every blow sets the foe alight. Fire hurts you less.")
                .passive(Passive.resist("minecraft:is_fire", 0.25))
                .passive(Passive.skill("tensura:flame_attack_resistance"))
                .passive(Passive.flag(Wave8Events.F_FANG))
                .act(Act.of("searing_fang", "Searing Fang", Wave8Acts::searingFang).cooldown(600))
                .melee(7.5, 1.6)
                .engrave("elemental_boost", "severance", "swift"));

        // ---- Corpse Eater: frenzy form
        out.accept(RelicDef.of("corpse_eater", "Corpse Eater", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x1A1316, 0xC8A24A, 0xFF3A3A)
                .desc("A katana whose edge has split open around a staring eye. Frenzy form: two blocks more reach and a necrotic edge. The eye can hold those who see it (they may resist), a Rending Bite tears the foe and feeds you, and every death within thirty-six blocks sends sword and wielder into a Feeding Frenzy.")
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 2.0, ADD))
                .passive(Passive.flag(Wave8Events.F_CORPSE))
                .act(Act.of("eye_gaze", "The Eye Opens", Wave8Acts::eyeGaze).cooldown(1200).epPercent(1.0))
                .act(Act.of("rending_bite", "Rending Bite", Wave8Acts::rendingBite).cooldown(160).mana(3))
                .melee(9.0, 1.5)
                .engrave("soul_eater", "severance", "energy_steal"));

        // ---- Random Axe of Charity
        out.accept(RelicDef.of("random_axe_of_charity", "Random Axe of Charity", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0x8D9399, 0x6B4A2B, 0xFFB347)
                .desc("A plain handaxe with a blood-red stain. Each creature it fells drops a few copper ingots. It brings a little luck, and you can throw it at a foe in sight without losing it.")
                .passive(Passive.attr("minecraft:generic.luck", 1.0, ADD))
                .passive(Passive.flag(Wave8Events.F_CHARITY))
                .act(Act.of("throw_handaxe", "Throw Handaxe", Wave8Acts::throwHandaxe).cooldown(80))
                .melee(5.5, 1.8)
                .engrave("sturdy", "swift", "growth"));

        // ---- Leviathan Axe: the card is cut off at its header; the kit is invented around the name
        out.accept(RelicDef.of("leviathan_axe", "Leviathan Axe", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .attune().vessel().model3d().look(Look.AXE).palette(0xA9D9F2, 0x3A5F7A, 0x6FE0FF)
                .desc("A rune-carved frost axe. Right-click throws it down a line of twenty blocks, chilling everything it passes, and it flies back to your hand, dragging foes with it. Sneak-click awakens its runes: a freezing fog and colder blows for twelve seconds.")
                .passive(Passive.skill("tensura:cold_resistance"))
                .passive(Passive.resist("minecraft:is_freezing", 0.6))
                .passive(Passive.flag(Wave8Events.F_LEV))
                .act(Act.of("leviathan_throw", "Leviathan Throw", Wave8Acts::leviathanThrow).cooldown(160).mana(3))
                .act(Act.of("frost_awakening", "Frost Awakening", Wave8Acts::frostAwakening).cooldown(900).epPercent(1.0))
                .melee(9.5, 1.4)
                .engrave("elemental_boost", "crushing", "sturdy"));

        // ---- Shadow Burn: unique greatsword
        out.accept(RelicDef.of("shadow_burn", "Shadow Burn", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x2B2E3C, 0xC9A24A, 0xFF7A33)
                .desc("A heavy greatsword pulled from the heart of shade to serve the light, wrapped in shadow wisps. Foes standing in shadow burn. Burning Hold drives it into a foe's shadow (fire, and paralysis unless they resist); sneak-click for Shadow Stalk, twice a day: rise behind a foe within twenty blocks.")
                .passive(Passive.skill("tensura:shadow_motion"))
                .passive(Passive.flag(Wave8Events.F_BURN))
                .act(Act.of("burning_hold", "Burning Hold", Wave8Acts::burningHold).cooldown(240).mana(5))
                .act(Act.of("shadow_stalk", "Shadow Stalk", Wave8Acts::shadowStalk).usesPerDay(2).cooldown(60))
                .melee(10.5, 1.1)
                .engrave("elemental_boost", "severance", "sturdy"));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave8Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave8Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave8Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave8Events::onUseFinish);
        NeoForge.EVENT_BUS.addListener(Wave8Events::onPlayerTick);
    }
}
