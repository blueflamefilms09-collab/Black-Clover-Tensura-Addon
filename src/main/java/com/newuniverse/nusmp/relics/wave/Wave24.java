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
 * Why: Relics of Many Worlds part 4, wave 24: twelve small wondrous items and rings, all worn as Charms (Instant Cooking Pot, Lucky Egg, Lumpy,
 * Monster Compass, Official-Looking Badge, Orb of Plagues, Orb of the Dryad, Pocket Time, Ring of Endless Rage, Ring of Memories, Ring of Power,
 * Ring of Regeneration). {@link #defs} lists them for the catalog (the core registers the items), {@link #hooks} registers the game-bus
 * listeners once. The act bodies are in {@link Wave24Acts}, the listeners in {@link Wave24Events}, the numbers in {@link Wave24Rules}. The long
 * card texts, with every tabletop rule turned into a game rule, are in the lang fragment relics_wave24.lang.json and the wave's doc.
 */
public final class Wave24 {
    private Wave24() {}

    // one flag per relic that has handlers, set by the core while it is worn (and attuned, if it needs that)
    public static final String F_EGG = "w24_lucky_egg", F_LUMPY = "w24_lumpy", F_COMPASS = "w24_monster_compass", F_DRYAD = "w24_orb_of_the_dryad",
            F_RAGE = "w24_endless_rage", F_REGEN = "w24_ring_of_regeneration", F_MEMORIES = "w24_ring_of_memories", F_POWER = "w24_ring_of_power",
            F_BADGE = "w24_official_looking_badge";

    private static final String ATK = "minecraft:generic.attack_damage", LUCK = "minecraft:generic.luck";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Instant Cooking Pot (no rarity printed: Special)
        out.accept(RelicDef.of("instant_cooking_pot", "Instant Cooking Pot", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x8A8E98, 0x2A2A30, 0xFF8A2A).look(Look.CHARM)
                .desc("A scorched grey saucepan that works like a microwave. Use it: the raw food in your hands or pack is cooked after a d4 of five-second steps.")
                .act(Act.of("cook", "Cook", Wave24Acts::cook).cooldown(40)));

        // ---------------------------------------------------------------- Lucky Egg
        out.accept(RelicDef.of("lucky_egg", "Lucky Egg", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x16161C, 0xE8C860, 0xF4F8FF).look(Look.PENDANT)
                .desc("A black-and-silver egg on a gold chain. Twice a day kiss it: the next heavy blow against you is rolled again, never worse, and Luck rises for half a minute.")
                .passive(Passive.attr(LUCK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_EGG))
                .act(Act.of("kiss", "Kiss the Egg", Wave24Acts::kissEgg).cooldown(40).usesPerDay(Wave24Rules.EGG_USES_PER_DAY)));

        // ---------------------------------------------------------------- Lumpy
        out.accept(RelicDef.of("lumpy", "Lumpy", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x4A78D8, 0xF08A2A, 0xBFE0FF).look(Look.CHARM)
                .desc("A sentient felt hand puppet in a blue wizard hat who talks only to you, and mostly to insult you sweetly. Mage Hand pulls items and presses levers, Message calls other players, Detect Thoughts reads the minds around you.")
                .passive(Passive.skill("tensura:telepathy"))
                .passive(Passive.flag(F_LUMPY))
                .act(Act.of("mage_hand", "Mage Hand", Wave24Acts::mageHand).cooldown(60))
                .act(Act.of("detect_thoughts", "Detect Thoughts", Wave24Acts::detectThoughts).cooldown(600).mana(1))
                .act(Act.of("message", "Message", Wave24Acts::message).cooldown(100)));

        // ---------------------------------------------------------------- Monster Compass
        out.accept(RelicDef.of("monster_compass", "Monster Compass", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xB02A22, 0xC8C8D0, 0xE8F030).look(Look.CHARM)
                .desc("A red lacquered case with a slit-pupil eye. Open the lid and the needle points to the nearest hostile monster; the darker the eye, the stronger the monster.")
                .passive(Passive.flag(F_COMPASS))
                .act(Act.of("lid", "Open the Lid", Wave24Acts::compassLid).toggle().cooldown(10)));

        // ---------------------------------------------------------------- Official-Looking Badge
        out.accept(RelicDef.of("official_looking_badge", "Official-Looking Badge", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xE8C23A, 0x9A9AA8, 0xFFE27A).look(Look.PENDANT)
                .desc("A gold badge of two crossed hammers that nobody can place, and everyone respects. Villagers trade kindly with you and luck is on your side; show it to make weak monsters back off.")
                .passive(Passive.effect("minecraft:hero_of_the_village", 0))
                .passive(Passive.attr(LUCK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_BADGE))
                .act(Act.of("show_badge", "Show the Badge", Wave24Acts::showBadge).cooldown(600)));

        // ---------------------------------------------------------------- Orb of Plagues (very rare, attunement)
        out.accept(RelicDef.of("orb_of_plagues", "Orb of Plagues", Grade.UNIQUE, RelicSlot.CHARM).attune()
                .palette(0x8A8A2A, 0x1C1814, 0x6CC83A).look(Look.ORB)
                .desc("A murky olive orb in a cage of black thorns, ten charges, 1d6+3 back at dawn. Rays of sickness and enfeeblement, a stinking cloud, blight and contagion; or smash it for a plague cloud that ruins the orb.")
                .passive(Passive.skill("tensura:poison_resistance"))
                .act(Act.of("ray_sickness", "Ray of Sickness", Wave24Acts::raySickness).cooldown(40))
                .act(Act.of("ray_enfeeblement", "Ray of Enfeeblement", Wave24Acts::rayEnfeeblement).cooldown(60))
                .act(Act.of("stinking_cloud", "Stinking Cloud", Wave24Acts::stinkingCloud).cooldown(100))
                .act(Act.of("blight", "Blight", Wave24Acts::blight).cooldown(160))
                .act(Act.of("contagion", "Contagion", Wave24Acts::contagion).cooldown(200))
                .act(Act.of("plague_cloud", "Plague Cloud", Wave24Acts::plagueCloud).cooldown(40).needsSneak())
                .act(Act.of("detect_poison", "Detect Poison and Disease", Wave24Acts::detectPoison).cooldown(200)));

        // ---------------------------------------------------------------- Orb of the Dryad (uncommon, attunement by a druid or monk)
        out.accept(RelicDef.of("orb_of_the_dryad", "Orb of the Dryad", Grade.SPECIAL, RelicSlot.CHARM).attune()
                .palette(0x9A6A2A, 0x5C3A18, 0x6CFF7A).look(Look.ORB)
                .desc("A carved golden-brown orb wreathed in green mist. It floats beside you and sheds light; once a day it flies at a creature, heals all it passes through, and hovers over its target healing for a few rounds.")
                .passive(Passive.flag(F_DRYAD))
                .act(Act.of("light", "Nature's Light", Wave24Acts::dryadLight).toggle().cooldown(10))
                .act(Act.of("healing_flight", "Healing Flight", Wave24Acts::healingFlight).cooldown(100).usesPerDay(1)));

        // ---------------------------------------------------------------- Pocket Time
        out.accept(RelicDef.of("pocket_time", "Pocket Time", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xE8C23A, 0xB88A1E, 0xFFF0A0).look(Look.PENDANT)
                .desc("A round golden pocket watch with Roman numerals. Once a day it rolls a d12 and the day jumps forward to the next time the clock shows that hour.")
                .act(Act.of("jump", "Pocket Time", Wave24Acts::pocketTime).cooldown(1200).usesPerDay(1)));

        // ---------------------------------------------------------------- Ring of Endless Rage (uncommon, a barbarian's ring)
        out.accept(RelicDef.of("ring_of_endless_rage", "Ring of Endless Rage", Grade.SPECIAL, RelicSlot.CHARM).attune()
                .palette(0xB8B8C4, 0xC8182C, 0xFF3A4A).look(Look.CHARM)
                .desc("A silver band with a ruby between two crescents. The blow that would kill you crushes the ruby: for 30 seconds you cannot die and cannot be healed, then three death saves decide how you wake. The ring is gone.")
                .passive(Passive.flag(F_RAGE)));

        // ---------------------------------------------------------------- Ring of Memories (rare, attunement)
        out.accept(RelicDef.of("ring_of_memories", "Ring of Memories", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0x4A4C56, 0x9AA0AC, 0x7CF0F0).look(Look.CHARM)
                .desc("A dark band wound with wire round a pale cyan crystal. It records where you have walked. Remember stores the journey, Relive plays it back in a ten-second trance once a day, Erase clears it.")
                .passive(Passive.flag(F_MEMORIES))
                .act(Act.of("remember", "Remember", Wave24Acts::remember).cooldown(100))
                .act(Act.of("relive", "Relive", Wave24Acts::relive).cooldown(200).usesPerDay(1))
                .act(Act.of("erase", "Erase", Wave24Acts::erase).cooldown(40).needsSneak()));

        // ---------------------------------------------------------------- Ring of Power (legendary, attunement)
        out.accept(RelicDef.of("ring_of_power", "Ring of Power", Grade.LEGEND, RelicSlot.CHARM).attune().vessel()
                .palette(0x9A6E5A, 0x8A8A98, 0xB060FF).look(Look.CHARM)
                .desc("An infinity stone of power and destruction in a clawed rose-bronze ring. More strength, extra force with every weapon blow, and once a day Disintegrate.")
                .passive(Passive.attr(ATK, 2.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_POWER))
                .act(Act.of("disintegrate", "Disintegrate", Wave24Acts::disintegrate).cooldown(600).usesPerDay(1).epPercent(3.0)));

        // ---------------------------------------------------------------- Ring of Regeneration (very rare, attunement)
        out.accept(RelicDef.of("ring_of_regeneration", "Ring of Regeneration", Grade.UNIQUE, RelicSlot.CHARM).attune()
                .palette(0x6A4426, 0x3FC83A, 0x7CFF6A).look(Look.CHARM)
                .desc("A wooden band carved with vines and green leaves. Every twenty seconds you regain 1d6 health while you are hurt, and crippling effects, poisons and sicknesses wither away twice as fast.")
                .passive(Passive.skill("tensura:self_regeneration"))
                .passive(Passive.flag(F_REGEN)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave24Events.register();
    }
}
