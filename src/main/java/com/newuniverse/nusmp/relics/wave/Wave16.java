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
 * Relics of Many Worlds, second card set, wave 16: three necklace pieces (Sigil of the Faint Flame, True Heart Clasp, Brooch of Po'eur) and fourteen
 * charms (Holy Handgrenade, Snugglable Doll, Paralyzing Perfume, Satchel of Unmet Needs, Drow Sunscreen, Portable God, Jar of Bats, the two Runechalks,
 * Dryad's Wedding Band, Weaversong, Coin of Draconic Luck, Strange Idol, Wallet of the Slayer). {@link #defs} lists them for the catalog (the core
 * registers the items), {@link #hooks} registers the game-bus listeners once. The long card texts, with every tabletop rule turned into a game rule,
 * are in the lang fragment relics_wave16.lang.json and docs/doc_wave16.md. None is a weapon: no 3D mesh and no engravings.
 */
public final class Wave16 {
    private Wave16() {}

    private static boolean hooked;
    private static final String LUCK = "minecraft:generic.luck", MAX_HEALTH = "minecraft:generic.max_health", MAX_MAGICULE = "tensura:max_magicule";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Sigil of the Faint Flame (very rare, attunement)
        out.accept(RelicDef.of(Wave16Tune.ID_SIGIL, "Sigil of the Faint Flame", Grade.UNIQUE, RelicSlot.NECK).attune()
                .palette(0xD9A628, 0x2D5FB5, 0x58E8F0).look(Look.AMULET)
                .desc("A gold sigil set with sapphires that stays warm in the coldest shadow. Your fire spells hit harder; a spell aimed at you is halved by a charge, and a fireball goes back at the caster.")
                .passive(Passive.flag(Wave16Tune.F_FLAME))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.resist("minecraft:is_fire", 0.25))
                .passive(Passive.skill("tensura:flame_attack_resistance"))
                .act(Act.of("faint_flame", "Faint Flame", Wave16Acts::sigilFlame).cooldown(300).mana(3))
                .act(Act.of("sunlit_warmth", "Sunlit Warmth", Wave16Acts::sigilWarmth).cooldown(600)));

        // ---------------------------------------------------------------- True Heart Clasp (no rarity printed)
        out.accept(RelicDef.of(Wave16Tune.ID_CLASP, "True Heart Clasp", Grade.RARE, RelicSlot.NECK)
                .palette(0xD94A6B, 0xB07A3A, 0xFF8FA8).look(Look.AMULET)
                .desc("A clip-on heart of pink-red gem. It lets you feel what people truly intend: you are a little luckier, and a burst of insight shows who means you harm.")
                .passive(Passive.attr(LUCK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:danger_sense"))
                .act(Act.of("true_intentions", "True Intentions", Wave16Acts::clasp).cooldown(400).mana(2)));

        // ---------------------------------------------------------------- Brooch of Po'eur (one-of-a-kind)
        out.accept(RelicDef.of(Wave16Tune.ID_POEUR, "Brooch of Po'eur", Grade.LEGEND, RelicSlot.NECK).vessel()
                .palette(0xCFE8F5, 0x9BC4E6, 0xBFEFFF).look(Look.AMULET)
                .desc("A snowflake brooch of cut gems, close enough to diamonds. Sometimes a costly spell costs you nothing at all; the bigger the spell, the harder the check. It can also burst in a starburst of frost.")
                .passive(Passive.flag(Wave16Tune.F_POEUR))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.attr(MAX_MAGICULE, 0.08, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("starburst", "Starburst", Wave16Acts::poeurStarburst).cooldown(900).mana(6)));

        // ---------------------------------------------------------------- Holy Handgrenade (legendary)
        out.accept(RelicDef.of(Wave16Tune.ID_GRENADE, "Holy Handgrenade", Grade.LEGEND, RelicSlot.CHARM).vessel()
                .palette(0x6B2230, 0xD8A0B8, 0xFFE0F0).look(Look.ORB)
                .desc("Count to three, no more, no less. Hold to count, then it is lobbed: the evil are smitten, the good are mended. Roll badly and it fizzles, stays in your hand, or goes off in it.")
                .passive(Passive.skill("tensura:holy_attack_resistance"))
                .act(Act.of("count_to_three", "Count to Three", Wave16Acts::grenade).hold(Wave16Tune.GRENADE_HOLD).cooldown(6000).epPercent(5.0)));

        // ---------------------------------------------------------------- Snugglable Doll (rare, attunement)
        out.accept(RelicDef.of(Wave16Tune.ID_DOLL, "Snugglable Doll", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0x4A7FD0, 0x7FE8F0, 0xB0C4D8).look(Look.CHARM)
                .desc("A stitched blue cat with button eyes and a ghost grinning behind it. It brings Snuggles to life, a hardier cat that fights at your side, and lends you Telepathy to speak with it.")
                .passive(Passive.skill("tensura:telepathy"))
                .act(Act.of("come_alive", "Come Alive", Wave16Acts::dollAlive).cooldown(2400).mana(3))
                .act(Act.of("cuddle", "Cuddle", Wave16Acts::dollCuddle).cooldown(600)));

        // ---------------------------------------------------------------- Paralyzing Perfume (rare)
        out.accept(RelicDef.of(Wave16Tune.ID_PERFUME, "Paralyzing Perfume", Grade.RARE, RelicSlot.CHARM)
                .palette(0x8E93A0, 0xD4B24C, 0xC080FF).look(Look.POTION)
                .desc("Spray it in a foe's face to hold them still for a few seconds unless they shake it off. Ten sprays in the bottle; one comes back at every dawn.")
                .act(Act.of("spray", "Spray", Wave16Acts::perfume).cooldown(60)));

        // ---------------------------------------------------------------- Satchel of Unmet Needs (uncommon)
        out.accept(RelicDef.of(Wave16Tune.ID_SATCHEL, "Satchel of Unmet Needs", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x8B4A2B, 0xE8B83C, 0xFFD060).look(Look.CHARM)
                .desc("A satchel that cannot be stocked, only emptied: choose an object, reach in and pull it out. It vanishes after a day outside the bag, and is then missing for three dawns.")
                .passive(Passive.flag(Wave16Tune.F_SATCHEL))
                .act(Act.of("reach_in", "Reach In", Wave16Satchel::reach).cooldown(40))
                .act(Act.of("choose_object", "Choose Object", Wave16Satchel::next).cooldown(5))
                .act(Act.of("put_back", "Put Back", Wave16Satchel::putBack).cooldown(10)));

        // ---------------------------------------------------------------- Drow Sunscreen (uncommon)
        out.accept(RelicDef.of(Wave16Tune.ID_SUNSCREEN, "Drow Sunscreen", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xF08C1E, 0xF2D030, 0x8040A0).look(Look.POTION)
                .desc("Protect your sensitive Underdark skin from the sun. Three hours after applying, the sun's burn is halved and flashes and gloom cannot blind you. Don't forget to reapply!")
                .passive(Passive.flag(Wave16Tune.F_SUN))
                .act(Act.of("apply_sunscreen", "Apply Sunscreen", Wave16Acts::sunscreen).cooldown(100).usesPerDay(3)));

        // ---------------------------------------------------------------- Portable God (rare, attunement: worship)
        out.accept(RelicDef.of(Wave16Tune.ID_GOD, "Portable God", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0x7B7F8C, 0xB8BCC8, 0xFF3030).look(Look.CHARM)
                .desc("A little figure of your god with twelve charges (three return at dawn): Punishment, Rescue, Holy Spirit, Revenge, Deus Ex and Wonder, each at its own price.")
                .passive(Passive.flag(Wave16Tune.F_GOD))
                .act(Act.of("punishment", "Punishment", Wave16God::punish).cooldown(20))
                .act(Act.of("rescue", "Rescue", Wave16God::rescue).cooldown(20))
                .act(Act.of("holy_spirit", "Holy Spirit", Wave16God::spirit).cooldown(20))
                .act(Act.of("revenge", "Revenge", Wave16God::revenge).cooldown(20))
                .act(Act.of("deus_ex", "Deus Ex", Wave16God::avatar).cooldown(100))
                .act(Act.of("wonder", "Wonder", Wave16God::wonder).cooldown(100)));

        // ---------------------------------------------------------------- Jar of Bats (no rarity printed)
        out.accept(RelicDef.of(Wave16Tune.ID_BATS, "Jar of Bats", Grade.RARE, RelicSlot.CHARM)
                .palette(0x2F9E8A, 0xC03030, 0x7CFF4A).look(Look.POTION)
                .desc("Open the jar and six bats swarm out, answering you: they fall on the nearest foe for a minute, then fly back in.")
                .act(Act.of("release_swarm", "Release the Swarm", Wave16Swarm::releaseAct).cooldown(3600).mana(2))
                .act(Act.of("recall_bats", "Recall the Bats", Wave16Swarm::recallAct).cooldown(20)));

        // ---------------------------------------------------------------- Simple Blue Runechalk (common)
        out.accept(RelicDef.of(Wave16Tune.ID_BLUE, "Simple Blue Runechalk", Grade.NORMAL, RelicSlot.CHARM)
                .palette(0x3FB6C8, 0xA8F0FF, 0x70E8FF).look(Look.CHARM)
                .desc("Twenty chalk symbols (two return at dawn): Square sets an alarm, Circle purifies food and drink, Triangle mends what is in your hand, Cross locks a door or chest.")
                .act(Act.of("square_alarm", "Square: Alarm", Wave16Chalk::blueSquare).cooldown(40))
                .act(Act.of("circle_purify", "Circle: Purify", Wave16Chalk::blueCircle).cooldown(40))
                .act(Act.of("triangle_mending", "Triangle: Mending", Wave16Chalk::blueTriangle).cooldown(40))
                .act(Act.of("cross_lock", "Cross: Lock", Wave16Chalk::blueCross).cooldown(40)));

        // ---------------------------------------------------------------- Dryad's Wedding Band (very rare, cursed)
        out.accept(RelicDef.of(Wave16Tune.ID_DRYAD, "Dryad's Wedding Band", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0x9AA0A8, 0xB8863A, 0x80D860).look(Look.CHARM)
                .desc("A cursed ring of twined vines that cannot be taken off until you die. Plants grow on you; the sun feeds you, fire burns you worse, and once a day you can speak with plants and they obey.")
                .passive(Passive.flag(Wave16Tune.F_DRYAD))
                .passive(Passive.attr(MAX_HEALTH, 2.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:poison_resistance"))
                .act(Act.of("speak_with_plants", "Speak with Plants", Wave16Acts::dryadSpeak).cooldown(200).usesPerDay(1)));

        // ---------------------------------------------------------------- Weaversong (rare, attunement)
        out.accept(RelicDef.of(Wave16Tune.ID_WEAVER, "Weaversong", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0x15182B, 0xE8E0F5, 0xF0C8F8).look(Look.CHARM)
                .desc("A silken charm with a farewell song in it. Three Weaverlings walk at your side and fight for you; one that falls comes back two seconds later, and strays are called to your feet.")
                .passive(Passive.attr(LUCK, 0.5, Operation.ADD_VALUE))
                .act(Act.of("call_weaverlings", "Call the Weaverlings", Wave16Swarm::weaverCall).cooldown(600)));

        // ---------------------------------------------------------------- Coin of Draconic Luck (uncommon)
        out.accept(RelicDef.of(Wave16Tune.ID_COIN, "Coin of Draconic Luck", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xD8A82A, 0x8A5A18, 0xFFD870).look(Look.CHARM)
                .desc("Flip the dragon's coin three times a day: heads brings luck and strength for half a minute, tails the opposite. Three tails in a row can crack the coin.")
                .act(Act.of("dragons_bargain", "Dragon's Bargain", Wave16Acts::coin).cooldown(40).usesPerDay(3)));

        // ---------------------------------------------------------------- Alabaster White Runechalk (rare)
        out.accept(RelicDef.of(Wave16Tune.ID_WHITE, "Alabaster White Runechalk", Grade.RARE, RelicSlot.CHARM)
                .palette(0xF0F0F0, 0xBFD8F0, 0xA0D8FF).look(Look.CHARM)
                .desc("Twenty chalk symbols (two return at dawn): Square pulls a friend back from death's door, Circle blesses those who stand in it, Triangle burns the undead, Cross wipes a book or map clean.")
                .act(Act.of("square_stabilize", "Square: Stabilize", Wave16Chalk::whiteSquare).cooldown(40))
                .act(Act.of("circle_luck", "Circle: Luck", Wave16Chalk::whiteCircle).cooldown(40))
                .act(Act.of("triangle_glyph", "Triangle: Sacred Glyph", Wave16Chalk::whiteTriangle).cooldown(40))
                .act(Act.of("cross_erase", "Cross: Erase", Wave16Chalk::whiteCross).cooldown(40)));

        // ---------------------------------------------------------------- Strange Idol (no rarity printed)
        out.accept(RelicDef.of(Wave16Tune.ID_IDOL, "Strange Idol", Grade.RARE, RelicSlot.CHARM)
                .palette(0xD8B040, 0x58D868, 0x70FF80).look(Look.CHARM)
                .desc("A golden cat with a stitched green scar. Once a day it re-rolls fate in your favour, keeping the better of two rolls, and someone near you rolls with disadvantage.")
                .passive(Passive.attr(LUCK, 0.5, Operation.ADD_VALUE))
                .act(Act.of("reroll_fate", "Re-roll Fate", Wave16Acts::idol).cooldown(100).usesPerDay(1)));

        // ---------------------------------------------------------------- Wallet of the Slayer (legendary)
        out.accept(RelicDef.of(Wave16Tune.ID_WALLET, "Wallet of the Slayer", Grade.LEGEND, RelicSlot.CHARM).vessel()
                .palette(0xA77B4A, 0xD8B040, 0x606068).look(Look.CHARM)
                .desc("A singed wallet that pays coin for every life your own hand takes: its challenge times 2d6 gold. Count the coin out, or tithe it for healing.")
                .passive(Passive.flag(Wave16Tune.F_WALLET))
                .act(Act.of("count_coin", "Count Coin", Wave16Wallet::withdraw).cooldown(20))
                .act(Act.of("blood_tithe", "Blood Tithe", Wave16Wallet::tithe).cooldown(1200)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        Wave16Events.register();
    }

    /** The grimoire cast code calls this after a spell is cast (the Brooch of Po'eur may refund it): see the report's integration edits. */
    public static void onSpellCast(net.minecraft.server.level.ServerPlayer p, double costPercent) { Wave16Events.onSpellCast(p, costPercent); }
}
