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
 * Relics of Many Worlds, second set, wave 18: the seven Bountiful Bags (Holding, Devouring, Colding, Golding, Molding, Scolding, Unfolding) and ten
 * other charms (Old Master's Spellbook, Tiny Orbit, The Lover's Divide, Coin of Fortune, Defender's Dial, Elephant of The Room, Bag of Kittens, The
 * Pocket Campsite, Mirror of Spell Reflection, Dragon Satchel). All are Curios charms; none needs attunement (no card asks for it). {@link #defs} lists
 * them for the catalog (the core registers the items); {@link #hooks} registers the game-bus listeners once. The long card texts, with every tabletop
 * rule turned into a game rule, are in the lang fragment relics_wave18.lang.json and docs/doc_wave18.md.
 */
public final class Wave18 {
    private Wave18() {}

    private static final String LUCK = "minecraft:generic.luck", MAX_MAGICULE = "tensura:max_magicule";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Bag of Holding (uncommon)
        out.accept(RelicDef.of(Wave18Tune.ID_HOLDING, "Bag of Holding", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x7A4B2A, 0x2FA39A, 0xE05A4A).look(Look.CHARM)
                .desc("A satchel far bigger on the inside: a chest of fifty-four slots that goes with you. No bag fits inside another.")
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.HOLDING)).cooldown(10)));

        // ---------------------------------------------------------------- Bag of Devouring (very rare)
        out.accept(RelicDef.of(Wave18Tune.ID_DEVOURING, "Bag of Devouring", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0x4A3A30, 0x8B2A2A, 0xFF4A4A).look(Look.CHARM)
                .desc("The mouth of a vast creature. It eats food and plants put in it (and feeds you magicules for them), spits the rest out each day, and can gulp a small foe after three telegraphed seconds.")
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.DEVOURING)).cooldown(10))
                .act(Act.of("gulp", "Reach In", c -> Wave18Bags.stuff(c, Wave18Pure.Bag.DEVOURING)).cooldown(Wave18Tune.GULP_COOLDOWN).epPercent(Wave18Tune.GULP_EP)));

        // ---------------------------------------------------------------- Bag of Colding (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_COLDING, "Bag of Colding", Grade.RARE, RelicSlot.CHARM)
                .palette(0x5A5F70, 0x8FD8FF, 0xBFEFFF).look(Look.CHARM)
                .desc("A cold larder for food, and a snack from it on the move. Stuffed with a foe, it chills it to the bone. Frost bites you less while you carry it.")
                .passive(Passive.resist("minecraft:is_freezing", 0.3))
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.COLDING)).cooldown(10))
                .act(Act.of("stuff", "Stuff the Bag", c -> Wave18Bags.stuff(c, Wave18Pure.Bag.COLDING)).cooldown(Wave18Tune.STUFF_COOLDOWN).mana(Wave18Tune.STUFF_MANA))
                .act(Act.of("snack", "Cold Snack", Wave18Bags::snack).cooldown(40)));

        // ---------------------------------------------------------------- Bag of Golding (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_GOLDING, "Bag of Golding", Grade.RARE, RelicSlot.CHARM)
                .palette(0x7A5A2A, 0xE6B94A, 0xFFE08A).look(Look.CHARM)
                .desc("A tiny pouch for metal bars, nuggets and coins. Iron nuggets and copper left a day inside come out gilded as gold nuggets. A little luck clings to it.")
                .passive(Passive.attr(LUCK, 0.5, Operation.ADD_VALUE))
                .act(Act.of("open", "Open the Pouch", c -> Wave18Bags.open(c, Wave18Pure.Bag.GOLDING)).cooldown(10)));

        // ---------------------------------------------------------------- Bag of Molding (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_MOLDING, "Bag of Molding", Grade.RARE, RelicSlot.CHARM)
                .palette(0x4C5A34, 0x8AA64A, 0xB8E060).look(Look.CHARM)
                .desc("A damp bag for plant matter that rots it down to bone meal in a day. Stuffed with a foe, it poisons it. You are used to the stench: poison resistance while carried.")
                .passive(Passive.skill(Wave18Skills.POISON_RESISTANCE))
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.MOLDING)).cooldown(10))
                .act(Act.of("stuff", "Stuff the Bag", c -> Wave18Bags.stuff(c, Wave18Pure.Bag.MOLDING)).cooldown(Wave18Tune.STUFF_COOLDOWN).mana(Wave18Tune.STUFF_MANA)));

        // ---------------------------------------------------------------- Bag of Scolding (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_SCOLDING, "Bag of Scolding", Grade.RARE, RelicSlot.CHARM)
                .palette(0x6A3A3A, 0xE0A040, 0xFFC070).look(Look.CHARM)
                .desc("A bag that cannot stand mess: it sorts what is in it every time it opens. Stuffed with a foe, it lectures it: it takes damage, fear and a weak arm.")
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.SCOLDING)).cooldown(10))
                .act(Act.of("stuff", "Stuff the Bag", c -> Wave18Bags.stuff(c, Wave18Pure.Bag.SCOLDING)).cooldown(Wave18Tune.STUFF_COOLDOWN).mana(Wave18Tune.STUFF_MANA)));

        // ---------------------------------------------------------------- Bag of Unfolding (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_UNFOLDING, "Bag of Unfolding", Grade.RARE, RelicSlot.CHARM)
                .palette(0x5A4A7A, 0xC8C0E0, 0xD8B8FF).look(Look.CHARM)
                .desc("A bag that unties things. It shakes webs and locks off you and unlatches the iron door, trapdoor or gate you point it at, eight times a day.")
                .act(Act.of("open", "Open the Bag", c -> Wave18Bags.open(c, Wave18Pure.Bag.UNFOLDING)).cooldown(10))
                .act(Act.of("unlatch", "Unfold", Wave18Bags::unlatch).cooldown(20).usesPerDay(Wave18Tune.UNLATCH_PER_DAY)));

        // ---------------------------------------------------------------- Old Master's Spellbook (very rare)
        out.accept(RelicDef.of(Wave18Tune.ID_SPELLBOOK, "Old Master's Spellbook", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0x6B4220, 0xD2A24A, 0xFF6A3A).look(Look.CHARM)
                .desc("Centuries of margins and smudges: spells come cheaper and abilities are learned faster, elemental damage glances off, and twice a day you find the shortcut in the old notes.")
                .passive(Passive.attr(MAX_MAGICULE, 0.08, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("tensura:ability_learning_gain", 0.5, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("tensura:ability_mastery_gain", 0.5, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("tensura:magic_cost_multiplier", -0.1, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.resist("minecraft:is_fire", 0.25))
                .passive(Passive.resist("minecraft:is_freezing", 0.25))
                .passive(Passive.resist("minecraft:is_lightning", 0.25))
                .passive(Passive.resist("minecraft:is_explosion", 0.15))
                .passive(Passive.skill(Wave18Skills.MAGIC_SENSE))
                .act(Act.of("shortcut_to_mastery", "Shortcut to Mastery", Wave18Acts::study).cooldown(Wave18Tune.STUDY_COOLDOWN).usesPerDay(Wave18Tune.STUDY_PER_DAY)));

        // ---------------------------------------------------------------- Tiny Orbit (uncommon)
        out.accept(RelicDef.of(Wave18Tune.ID_ORBIT, "Tiny Orbit", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xC8CCD4, 0xEFEFEA, 0xDAD8C8).look(Look.CIRCLET)
                .desc("A thin silver chain circlet with a small illusory moon that orbits your head, and lights the dark a little for you.")
                .passive(Passive.flag(Wave18Tune.F_ORBIT)));

        // ---------------------------------------------------------------- The Lover's Divide
        out.accept(RelicDef.of(Wave18Tune.ID_LOVERS, "The Lover's Divide", Grade.RARE, RelicSlot.CHARM)
                .palette(0xE8B83A, 0xE0663A, 0xFFD27A).look(Look.CHARM)
                .desc("Bangles that join two hearts: for five minutes every blow either of you takes is split between you. The friend must be sneaking to agree, or be your own pet.")
                .passive(Passive.flag(Wave18Tune.F_LOVERS))
                .act(Act.of("link", "Join Hearts", Wave18Acts::link).cooldown(Wave18Tune.LINK_COOLDOWN))
                .act(Act.of("sever", "Sever the Chains", Wave18Acts::sever).cooldown(20)));

        // ---------------------------------------------------------------- Coin of Fortune (uncommon)
        out.accept(RelicDef.of(Wave18Tune.ID_COIN, "Coin of Fortune", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xD8A838, 0x7A3A9A, 0xFFE08A).look(Look.CHARM)
                .desc("Gold face for fortune, dark face for doom. Flip it three times a day: even is a blessing on your next blow dealt or taken and luck for five minutes, odd is the curse.")
                .act(Act.of("flip", "Flip the Coin", Wave18Acts::flip).cooldown(Wave18Tune.COIN_COOLDOWN).usesPerDay(Wave18Tune.COIN_PER_DAY)));

        // ---------------------------------------------------------------- Defender's Dial
        out.accept(RelicDef.of(Wave18Tune.ID_DIAL, "Defender's Dial", Grade.RARE, RelicSlot.CHARM)
                .palette(0x6A4A2E, 0xC0C4CC, 0xFF8A3A).look(Look.BELT)
                .desc("A dial of flame, snowflake and bolt. Turn it three times a day: each turn cuts one kind of damage by 60% for five minutes, in that order, then starts over.")
                .act(Act.of("dial", "Turn the Dial", Wave18Acts::dial).cooldown(20).usesPerDay(Wave18Tune.DIAL_PER_DAY)));

        // ---------------------------------------------------------------- Elephant of The Room (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_ELEPHANT, "Elephant of The Room", Grade.RARE, RelicSlot.CHARM)
                .palette(0x1A1A22, 0xF2F2F2, 0xBFBFFF).look(Look.CHARM)
                .desc("Say your deepest secret out loud (sneak and use it) and it swells to fill the room: foes are shoved aside, wedged against the walls and slowed for half a minute. Only once, for ever.")
                .act(Act.of("swell", "Fill the Room", Wave18Acts::elephant).needsSneak()));

        // ---------------------------------------------------------------- Bag of Kittens (very rare)
        out.accept(RelicDef.of(Wave18Tune.ID_KITTENS, "Bag of Kittens", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0xA89870, 0x5AB09A, 0xFFB0C8).look(Look.CHARM)
                .desc("A kitten paradise of a thousand kittens. Pick one to take with you (eight at most), pop one back in by sneaking, or turn the bag inside out for a flood of kittens that slows and blinds foes and cheers friends.")
                .act(Act.of("pick", "Pick a Kitten", Wave18Acts::pickKitten).cooldown(40))
                .act(Act.of("place", "Place a Kitten", Wave18Acts::placeKitten).cooldown(20))
                .act(Act.of("flood", "Feline Flood", Wave18Acts::flood).cooldown(Wave18Tune.FLOOD_COOLDOWN).epPercent(Wave18Tune.FLOOD_EP)));

        // ---------------------------------------------------------------- The Pocket Campsite
        out.accept(RelicDef.of(Wave18Tune.ID_CAMP, "The Pocket Campsite", Grade.RARE, RelicSlot.CHARM)
                .palette(0x8A4A2A, 0xD8803A, 0xFF9A3A).look(Look.CHARM)
                .desc("Point the pouch and press the rune: a large tent with a bed, two small tents and a campfire with seats appear, with a tiny hut over them. Sneak and press it again to pack it all away.")
                .act(Act.of("pitch", "Pitch the Camp", Wave18Camp::pitch).cooldown(Wave18Tune.CAMP_COOLDOWN))
                .act(Act.of("pack", "Pack Up", Wave18Camp::pack).cooldown(40).needsSneak()));

        // ---------------------------------------------------------------- Mirror of Spell Reflection (very rare)
        out.accept(RelicDef.of(Wave18Tune.ID_MIRROR, "Mirror of Spell Reflection", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0x4A3A5A, 0xC8D0E0, 0xDCE8FF).look(Look.CHARM)
                .desc("Raise the mirror (three times a day, for a little mana) and the next spell that strikes you within five seconds is thrown back at its caster.")
                .act(Act.of("reflect", "Raise the Mirror", Wave18Acts::raiseMirror).cooldown(Wave18Tune.MIRROR_COOLDOWN).mana(Wave18Tune.MIRROR_MANA).usesPerDay(Wave18Tune.MIRROR_PER_DAY)));

        // ---------------------------------------------------------------- Dragon Satchel (rare)
        out.accept(RelicDef.of(Wave18Tune.ID_DRAGON, "Dragon Satchel", Grade.RARE, RelicSlot.CHARM)
                .palette(0x7A3A2A, 0xD8A838, 0xFFC060).look(Look.CHARM)
                .desc("Fill it (sneak and use) with two handfuls of ash, sand, grass or snow; in a day it is crystal powder. Throw a handful in a cone for fire, lightning, acid or cold damage. Twice empties it.")
                .act(Act.of("breathe", "Throw Powder", Wave18Acts::breathe).cooldown(Wave18Tune.SATCHEL_COOLDOWN))
                .act(Act.of("fill", "Fill the Satchel", Wave18Acts::fill).cooldown(20).needsSneak()));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave18Events.register();
    }
}
