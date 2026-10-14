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
 * Relics of Many Worlds, second card set, wave 17: seventeen small wondrous items worn in the charm slot (Fast Travel Beads, the five Ancient Notebooks,
 * Madama Cupcake, Bags of Transferrence, Life's Emergency, Bag of Realistic Capacity, Plastic Sheriff Badge, Urn of Black Pudding, Crystal Dragon, Shared
 * Pouches of Transfer, Martel's Magical Bag of Candy, The Devil's Due, Fury of the Fallen). {@link #defs} lists them for the catalog (the core registers
 * the items), {@link #hooks} registers the game-bus listeners once. The long card texts, with every tabletop rule turned into a game rule, are in the lang
 * fragment relics_wave17.lang.json and docs/doc_wave17.md.
 */
public final class Wave17 {
    private Wave17() {}

    private static final String MAX_HEALTH = "minecraft:generic.max_health";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Fast Travel Beads (rare)
        out.accept(RelicDef.of("fast_travel_beads", "Fast Travel Beads", Grade.RARE, RelicSlot.CHARM)
                .palette(0x6A2A5E, 0x241A28, 0xC06BFF).look(Look.AMULET)
                .desc("A cord of starry beads, three to thirty of them. Break one to carry yourself and up to six crouching companions to a place of this world you have saved, your bed, or where you last fell.")
                .act(Act.of("bead_jump", "Break a Bead", Wave17Acts::beadJump).cooldown(100))
                .act(Act.of("choose_destination", "Choose Destination", Wave17Acts::chooseDestination).cooldown(5))
                .act(Act.of("remember_place", "Remember This Place", Wave17Acts::rememberPlace).cooldown(20)));

        // ---------------------------------------------------------------- the five Ancient Notebooks (common)
        notebook(out, "arcanists_notebook", "Arcanist's Notebook", "Arcana", 0x8A5A2B, 0xC4C8D4, 0x58D68D,
                "Notes of a long-dead arcanist. Consult it (hold use) and its pages of 0 to 3 steady your sense of magic for two to five minutes.");
        notebook(out, "botanists_notebook", "Botanist's Notebook", "Nature", 0x4F6B34, 0xB8C97A, 0x7CE36B,
                "Notes of a long-dead botanist. Consult it (hold use) and you know which plants help and which kill: Luck and poison resistance for two to five minutes.");
        notebook(out, "historians_notebook", "Historian's Notebook", "History", 0x7A5B2A, 0xD8C58A, 0xFFD36B,
                "Notes of a long-dead historian. Consult it (hold use) for a little experience and a sharper eye for what things are.");
        notebook(out, "physicians_notebook", "Physician's Notebook", "Medicine", 0x7A3A3A, 0xE8E4DC, 0xFF6B6B,
                "Notes of a long-dead physician. Consult it (hold use) to dress your wounds: a quick mend, regeneration and no poison.");
        notebook(out, "theologians_notebook", "Theologian's Notebook", "Religion", 0x6A5A8A, 0xF0D890, 0xFFF0B0,
                "Notes of a long-dead theologian. Consult it (hold use) for a remembered rite: a ward of absorption and resistance to holy harm.");

        // ---------------------------------------------------------------- Madama Cupcake (rare, consumable)
        out.accept(RelicDef.of("madama_cupcake", "Madama Cupcake", Grade.RARE, RelicSlot.CHARM)
                .palette(0x8B5A3C, 0xF4EAF8, 0xC9A0FF).look(Look.CHARM)
                .desc("A cupcake swirled with lavender frosting. Eat it and you will not go hungry for three days, yet you never feel bloated and can still eat. It is used up.")
                .act(Act.of("eat_cupcake", "Eat the Cupcake", Wave17Acts::eatCupcake).cooldown(40)));

        // ---------------------------------------------------------------- Bags of Transferrence (one of a kind: legend)
        out.accept(RelicDef.of("bags_of_transferrence", "Bags of Transferrence", Grade.LEGEND, RelicSlot.CHARM).vessel()
                .palette(0xC8A66A, 0x4A3320, 0xFFD27F).look(Look.CHARM)
                .desc("Two burlap bags that are one: a near bag and a far bag of two rows each. Speak the words and what is in one trades places with what is in the other, at any distance; link a friend and they reach the far bag with /pouch.")
                .act(Act.of("near_bag", "Open the Near Bag", Wave17Acts::openNearBag).cooldown(10))
                .act(Act.of("transfer", "Speak the Command Words", Wave17Acts::transfer).cooldown(100))
                .act(Act.of("far_bag", "Open the Far Bag", Wave17Acts::openFarBag).cooldown(10))
                .act(Act.of("link_partner", "Link a Companion", Wave17Acts::linkTransfer).cooldown(40)));

        // ---------------------------------------------------------------- Life's Emergency (unique, attunement)
        out.accept(RelicDef.of("lifes_emergency", "Life's Emergency", Grade.UNIQUE, RelicSlot.CHARM).attune()
                .palette(0x2A2018, 0xD8B04A, 0xE0364A).look(Look.CHARM)
                .desc("A cuff that draws life from the air: ten points each dawn, up to fifty. Worn, you feel hardier; fall at death's door and the whole store floods back into you.")
                .passive(Passive.attr(MAX_HEALTH, 2.0, Operation.ADD_VALUE))
                .act(Act.of("read_cuff", "Read the Cuff", Wave17Acts::readCuff).cooldown(20)));

        // ---------------------------------------------------------------- Bag of Realistic Capacity (rare)
        out.accept(RelicDef.of("bag_of_realistic_capacity", "Bag of Realistic Capacity", Grade.RARE, RelicSlot.CHARM)
                .palette(0xC9A89A, 0x8A6A5A, 0xFFE0D0).look(Look.CHARM)
                .desc("A fist-sized pouch that holds exactly as much as it looks like it can: two rows. Sneak and use to pull in the loose items around you.")
                .act(Act.of("open_pouch", "Open the Pouch", Wave17Acts::openRealistic).cooldown(10))
                .act(Act.of("gather_items", "Gather Loose Items", Wave17Acts::gather).cooldown(100)));

        // ---------------------------------------------------------------- Plastic Sheriff Badge (rare)
        out.accept(RelicDef.of("plastic_sheriff_badge", "Plastic Sheriff Badge", Grade.RARE, RelicSlot.CHARM)
                .palette(0xB8BCC4, 0x6A6E78, 0xFFFFFF).look(Look.CHARM)
                .desc("A toy star that says SHERIFF. Raiders sometimes believe it and look away; call a halt and every foe near you tests your bluff.")
                .passive(Passive.flag(Wave17Events.F_BADGE))
                .act(Act.of("halt", "Halt! In the Name of the Law", Wave17Acts::halt).cooldown(600)));

        // ---------------------------------------------------------------- Urn of Black Pudding (unique, thrown, used up)
        out.accept(RelicDef.of("urn_of_black_pudding", "Urn of Black Pudding", Grade.UNIQUE, RelicSlot.CHARM)
                .palette(0x2C7A6E, 0xC9983A, 0xFF9A3A).look(Look.POTION)
                .desc("A two-handled urn with a small black pudding inside. Throw it: acid for the target, its plain armour eaten away, and a small pudding fights for you. It shatters.")
                .act(Act.of("throw_urn", "Throw the Urn", Wave17Acts::throwUrn).cooldown(40)));

        // ---------------------------------------------------------------- Crystal Dragon (rare, used up)
        out.accept(RelicDef.of("crystal_dragon", "Crystal Dragon", Grade.RARE, RelicSlot.CHARM)
                .palette(0x3CC9C0, 0x9FF0E8, 0x7FFFF0).look(Look.CHARM)
                .desc("A statue of a crystal dragon. Once, on your command, it unfolds into a huge shadowy dragon that follows your gaze and breathes shadow for eight seconds.")
                .act(Act.of("wake_dragon", "Wake the Dragon", Wave17Acts::wakeDragon).cooldown(200).mana(8)));

        // ---------------------------------------------------------------- Shared Pouches of Transfer (rare)
        out.accept(RelicDef.of("shared_pouches_of_transfer", "Shared Pouches of Transfer", Grade.RARE, RelicSlot.CHARM)
                .palette(0x5E9E5A, 0x3A8F8A, 0xCFFFB0).look(Look.CHARM)
                .desc("A pair of sleepy green pouches on one pocket dimension, three rows deep. Link a friend (you both crouch) and you each reach the same items, from anywhere.")
                .act(Act.of("open_pouches", "Open the Pouches", Wave17Acts::openShared).cooldown(10))
                .act(Act.of("link_companion", "Link a Companion", Wave17Acts::linkShared).cooldown(40)));

        // ---------------------------------------------------------------- Martel's Magical Bag of Candy (rare)
        out.accept(RelicDef.of("martels_magical_bag_of_candy", "Martel's Magical Bag of Candy", Grade.RARE, RelicSlot.CHARM)
                .palette(0xE8E8F0, 0xE8782A, 0xFF9AD0).look(Look.POTION)
                .desc("Twenty enchanted sweets in a twine-tied bag. Eat one and roll on the wild magic table: most are kind, some are not.")
                .act(Act.of("eat_sweet", "Eat a Sweet", Wave17Acts::eatSweet).cooldown(60)));

        // ---------------------------------------------------------------- The Devil's Due (rare, used up)
        out.accept(RelicDef.of("the_devils_due", "The Devil's Due", Grade.RARE, RelicSlot.CHARM)
                .palette(0x9A5A36, 0xC8794A, 0xFF5A1A).look(Look.CHARM)
                .desc("A really bad penny, rusted and warm. Pay it with a thought: hellfire, and a horned devil fights at your side for five minutes. The coin is destroyed.")
                .act(Act.of("pay_the_devil", "Pay the Devil", Wave17Acts::payTheDevil).cooldown(100)));

        // ---------------------------------------------------------------- Fury of the Fallen (rare, attunement)
        out.accept(RelicDef.of("fury_of_the_fallen", "Fury of the Fallen", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0x201626, 0x6A3C7E, 0xFF3A5A).look(Look.CHARM)
                .desc("A dark badge with two slit eyes. When you fall under a fifth of your health you glow red, and every weapon blow you land does double damage.")
                .passive(Passive.flag(Wave17Events.F_FURY)));
    }

    /** One of the five Ancient Notebooks: a common charm, consulted by holding use for three seconds. */
    private static void notebook(Consumer<RelicDef> out, String id, String name, String check, int main, int trim, int glow, String desc) {
        out.accept(RelicDef.of(id, name, Grade.NORMAL, RelicSlot.CHARM)
                .palette(main, trim, glow).look(Look.TOME)
                .desc(desc)
                .act(Act.of("consult", "Consult the Notebook (" + check + ")", Wave17Acts::consult).hold(Wave17Rules.STUDY_HOLD).cooldown(Wave17Rules.STUDY_COOLDOWN)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave17Events.register();
    }
}
