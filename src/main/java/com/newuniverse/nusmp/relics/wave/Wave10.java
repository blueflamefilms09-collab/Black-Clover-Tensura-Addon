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
 * Relics of Many Worlds, second card set, wave 10: ten weapons (Krakeblade, Unicorn Dagger, Rune Splitter, Junglethorn Whip, Needler, Sidras'
 * swearing sword, Stegosaurus Greatsword, Jade Xiphos, Returning Hand Axe, Dead Blade). {@link #defs} lists them for the catalog (the core registers
 * the items), {@link #hooks} registers the game-bus listeners once. The act bodies are in {@link Wave10Acts}, the listeners in {@link Wave10Events},
 * the numbers in {@link Wave10Rules}. The long card texts, with every tabletop rule turned into a game rule, are in the lang fragment
 * relics_wave10.lang.json and docs/doc_wave10.md.
 */
public final class Wave10 {
    private Wave10() {}

    // one flag per weapon, set by the core while it is carried in the main hand (and attuned, if it needs that)
    public static final String F_KRAKE = "w10_krakeblade", F_RUNE = "w10_rune_splitter", F_JUNGLE = "w10_junglethorn", F_NEEDLER = "w10_needler",
            F_SIDRA = "w10_sidras", F_STEGO = "w10_stegosaurus", F_JADE = "w10_jade_xiphos", F_AXE = "w10_returning_axe", F_DEAD = "w10_dead_blade",
            F_UNICORN = "w10_unicorn_dagger";

    private static final String ATK = "minecraft:generic.attack_damage", REACH = "minecraft:player.entity_interaction_range";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Krakeblade (very rare, attunement)
        out.accept(RelicDef.of("krakeblade", "Krakeblade", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x4C6A5A, 0x6B4A32, 0x3FE0C8).look(Look.SWORD).melee(11.0, 0.9)
                .engrave("elemental_boost", "breathing_support", "crushing", "sturdy")
                .desc("A cleaver-slab of barnacle and coral with a tentacle round its guard. Near water it glows blue-green and bites 20% harder; you swim like a dolphin and breathe underwater. Once a day, a Wall of Water.")
                .passive(Passive.effect("minecraft:water_breathing", 0))
                .passive(Passive.effect("minecraft:dolphins_grace", 0))
                .passive(Passive.attr("minecraft:generic.oxygen_bonus", 4.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.water_movement_efficiency", 0.6, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:water_breathing"))
                .passive(Passive.flag(F_KRAKE))
                .act(Act.of("wall_of_water", "Wall of Water", Wave10Acts::wallOfWater).cooldown(400).usesPerDay(1).mana(3)));

        // ---------------------------------------------------------------- Unicorn Dagger (a rare, no attunement)
        out.accept(RelicDef.of("unicorn_dagger", "Unicorn Dagger", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0xF0D0F4, 0xE8B84A, 0xFFB8F0).look(Look.SWORD).melee(4.5, 2.4)
                .engrave("restoration", "vitality", "swift")
                .desc("A dagger cut from a unicorn's horn. Poke an ally three times a day: 1 harmless damage and a random d8 buff. A plain dagger against enemies.")
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_UNICORN))
                .act(Act.of("poke", "Poke", Wave10Acts::poke).cooldown(40).usesPerDay(3)));

        // ---------------------------------------------------------------- Rune Splitter (very rare, attunement)
        out.accept(RelicDef.of("rune_splitter", "Rune Splitter", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x7A1E22, 0xE8B83A, 0xFFE27A).look(Look.AXE).melee(10.0, 1.0)
                .engrave("holy_weapon", "magic_interference", "barrier_piercing", "magic_weapon")
                .desc("A dwarven cleric's sun axe, strapped with light. Use it to shine (night sight for you, a glow on hostile creatures near you; the colour changes each time); sneak-use once a day to Dispel Magic.")
                .passive(Passive.skill("tensura:light_attack_resistance"))
                .passive(Passive.flag(F_RUNE))
                .act(Act.of("light", "Light", Wave10Acts::light).toggle().cooldown(10))
                .act(Act.of("dispel_magic", "Dispel Magic", Wave10Acts::dispelMagic).cooldown(100).usesPerDay(1).mana(4)));

        // ---------------------------------------------------------------- Junglethorn Whip (very rare, attunement)
        out.accept(RelicDef.of("junglethorn_whip", "Junglethorn Whip", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0x6B7A32, 0xC8502A, 0x9CFF6A).look(Look.SWORD).melee(6.0, 1.6)
                .engrave("growth", "vitality", "severance", "swift")
                .desc("A thorny jungle vine. Climb trees at walking speed, swing from tree to tree, tear weapons from mobs' hands, lash a creature towards you; wild animals leave you be.")
                .passive(Passive.attr(REACH, 1.5, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 4.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_JUNGLE))
                .act(Act.of("vine_swing", "Vine Swing", Wave10Acts::vineSwing).cooldown(50))
                .act(Act.of("thorny_disarm", "Thorny Disarm", Wave10Acts::thornyDisarm).cooldown(300))
                .act(Act.of("thorn_whip", "Thorn Whip", Wave10Acts::thornWhip).cooldown(100).mana(1)));

        // ---------------------------------------------------------------- Needler (one-of-a-kind crossbow)
        out.accept(RelicDef.of("needler", "Needler", Grade.LEGEND, RelicSlot.MAIN_HAND).crossbow().model3d().vessel()
                .palette(0x4A1F7A, 0xB8BCC8, 0xB070FF).look(Look.BOW).melee(5.0, 1.5)
                .engrave("barrier_piercing", "magic_weapon", "swift", "enervation")
                .desc("A deep purple crossbow that ignores loading. Sneak-use for a Needle Volley: four needles in a fan, no strength added. Every bolt adds a little magic that finds the gap.")
                .passive(Passive.skill("tensura:farsight"))
                .passive(Passive.flag(F_NEEDLER))
                .act(Act.of("needle_volley", "Needle Volley", Wave10Acts::needleVolley).cooldown(120).epPercent(1.0)));

        // ---------------------------------------------------------------- Sidras' swearing sword (rare, attunement)
        out.accept(RelicDef.of("sidras_swearing_sword", "Sidras' swearing sword", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d()
                .palette(0xE4D8EE, 0x7A7A86, 0xD8B8FF).look(Look.SWORD).melee(5.5, 2.0)
                .engrave("enervation", "magic_interference", "swift")
                .desc("A sentient crystal shortsword that cannot stop swearing. After a hit the target saves or is insulted: 1d4 psychic damage and a moment of weakness. Barrage of Insults does it to everyone nearby.")
                .passive(Passive.flag(F_SIDRA))
                .act(Act.of("barrage", "Barrage of Insults", Wave10Acts::barrage).cooldown(400).mana(2)));

        // ---------------------------------------------------------------- Stegosaurus Greatsword (no rarity printed)
        out.accept(RelicDef.of("stegosaurus_greatsword", "Stegosaurus Greatsword", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0xE8E4D0, 0x3A6A6A, 0xD84A5A).look(Look.SWORD).melee(9.0, 1.0)
                .engrave("crushing", "sturdy", "vitality")
                .desc("A +2 greatsword made of a stegosaurus's spine; speak the word and it clatters into a whip. It will not fight to the front: ahead it hits weakly, but it lashes back by itself at whatever stands behind you.")
                .passive(Passive.flag(F_STEGO))
                .act(Act.of("command_word", "Command Word", Wave10Acts::commandWord).toggle().cooldown(20)));

        // ---------------------------------------------------------------- Jade Xiphos (uncommon)
        out.accept(RelicDef.of("jade_xiphos", "Jade Xiphos", Grade.SPECIAL, RelicSlot.MAIN_HAND).model3d()
                .palette(0x3FC878, 0xE8B84A, 0x7CFFB0).look(Look.SWORD).melee(5.0, 1.9)
                .engrave("severance", "swift")
                .desc("A leaf-shaped blade of green glass, finely balanced. A heavy blast of force or thunder shatters it, and the shards fly into every foe beside you. Sing it to pieces on purpose by sneaking and holding use.")
                .passive(Passive.flag(F_JADE))
                .act(Act.of("ringing_note", "Ringing Note", Wave10Acts::ringingNote).cooldown(200))
                .act(Act.of("shatter", "Shatter", Wave10Acts::shatter).hold(60)));

        // ---------------------------------------------------------------- Returning Hand Axe
        out.accept(RelicDef.of("returning_hand_axe", "Returning Hand Axe", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x1C1C24, 0xB8864A, 0x4FD8E8).look(Look.AXE).melee(6.0, 1.3)
                .engrave("elemental_boost", "swift", "severance")
                .desc("A throwing axe that turns to water. Use it to hurl it at the first creature in sight; sneak-use within eight seconds to bring it back in a burst that cuts everything on the way home.")
                .passive(Passive.skill("tensura:water_attack_resistance"))
                .passive(Passive.flag(F_AXE))
                .act(Act.of("hurl", "Hurl", Wave10Acts::hurl).cooldown(40))
                .act(Act.of("recall", "Recall", Wave10Acts::recall).cooldown(60)));

        // ---------------------------------------------------------------- Dead Blade
        out.accept(RelicDef.of("dead_blade", "Dead Blade", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .palette(0x3A2E48, 0xFF3FD0, 0xFF5AE0).look(Look.STAFF).melee(9.0, 1.0)
                .engrave("soul_eater", "ruination", "enervation", "severance")
                .desc("A glaive whose huge blade is a heap of glowing skulls. Long reach. Once a day, hold use and release for Finger of Death: a drain of 14 plus 12% of the target's health (a save halves it), and a humanoid it kills rises as your servant.")
                .passive(Passive.attr(REACH, 1.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:darkness_attack_resistance"))
                .passive(Passive.flag(F_DEAD))
                .act(Act.of("finger_of_death", "Finger of Death", Wave10Acts::fingerOfDeath).hold(25).cooldown(200).usesPerDay(1).epPercent(2.0)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave10Events.register();
    }
}
