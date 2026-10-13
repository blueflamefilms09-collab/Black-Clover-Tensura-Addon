package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 3: five relics for the head (a circlet, two crowns, a cowl, a tricorn), five for the hands, a belt, and two
 * consumables. The long card text of each, as game rules, is in the lang keys {@code item.nusmp.<id>.card}; the numbers and small state machines are
 * in {@link Wave3Rules} (pure), the abilities in {@link Wave3Head}, {@link Wave3Hands}, {@link Wave3Belt} and {@link Wave3Goods}, what stays in the
 * world in {@link Wave3Field}, the handlers in {@link Wave3Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day (uses per day come back at dawn), a saving throw is the target's existence (Tensura EP)
 * against the caster's, a bonus action is an act with a short cooldown, "+5 to all rolls" is attributes and lent Tensura skills, gold is a stash of
 * Magic Stones and emeralds. Where a card needed a skill the game does not have, a Tensura skill is lent (and merged) while it is worn or active.
 */
public final class Wave3 {
    private Wave3() {}

    private static final Passive ANY = Passive.flag(Wave3Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ head
        out.accept(RelicDef.of("arcanists_circlet", "Arcanist's Circlet", Grade.SPECIAL, RelicSlot.HEAD)
                .attune().look(Look.CIRCLET).palette(0xC9D0DC, 0x8E7CC8, 0x9A5CFF).armor(1, 0.0)
                .desc("Three arcane motes circle your head. Fire one at a creature you can see (36 blocks): 3 force damage that never misses. When all three are spent they gather again after a minute.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_MOTES))
                .act(Act.of("magic_missile", "Magic Missile", Wave3Head::missile).cooldown(10)));

        out.accept(RelicDef.of("prides_crown", "Pride's Crown", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0xF0B63A, 0xC27A1E, 0xFFB347).armor(2, 0.0)
                .desc("A crown of golden flame. Hostile creatures within six blocks whose existence is below yours slow down in your presence. Once a day a charm or fright aimed at you is refused and turned back on its sender.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_PRIDE)).passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE)));

        out.accept(RelicDef.of("dreamwalker_cowl", "Dreamwalker Cowl", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.HOOD).palette(0x3E8A4A, 0x8E56C8, 0xC9A0F2).armor(1, 0.0)
                .desc("A hooded scarf of stitched dreams. You see in the dark and sense magic, and magical sleep cannot take you. Three dream charges buy Sleep, Calm Emotions and Hallucinatory Terrain; a night's sleep refills them.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_DREAMER)).passive(Passive.effect("minecraft:night_vision", 0)).passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("dream_sleep", "Sleep", Wave3Head::sleepSpell).cooldown(200))
                .act(Act.of("dream_calm", "Calm Emotions", Wave3Head::calm).cooldown(200))
                .act(Act.of("dream_terrain", "Hallucinatory Terrain", Wave3Head::terrain).cooldown(600))
                .act(Act.of("shared_imagery", "Shared Imagery", Wave3Head::imagery).cooldown(600))
                .act(Act.of("dream_rest", "Master Dreamer", Wave3Head::rest).cooldown(1200)));

        out.accept(RelicDef.of("captains_tricorn", "Captain's Tricorn", Grade.RARE, RelicSlot.HEAD)
                .attune().look(Look.HAT).palette(0x2B2B34, 0xA9AFBE, 0xFF6E9A).armor(2, 0.0)
                .desc("Eight charges, two back at every dawn (1d6+2 if you sleep aboard a boat). Command halts a creature for a moment; the Captain's Order, two charges a target, rallies your crew and talks foes out of the fight.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_TRICORN)).passive(Passive.attr("neoforge:swim_speed", 0.1, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("command", "Command", Wave3Head::command).cooldown(100))
                .act(Act.of("captains_order", "Captain's Order", Wave3Head::order).cooldown(600)));

        out.accept(RelicDef.of("godking_crown", "Godking Crown", Grade.GOD, RelicSlot.HEAD)
                .attune().vessel().look(Look.CROWN).palette(0x3C4A44, 0x61766C, 0x5CF0B4).armor(3, 2.0)
                .desc("Put on its power: ten minutes of flight, +5 on everything and three Tensura skills merged into you. Then the crown takes its price: a radiant blast on you and everything within six blocks. Take the crown off, or dismiss it, to end it in peace.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_GODKING)).passive(Passive.attr("minecraft:generic.max_health", 6.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("ascend", "Ascend", Wave3Head::ascend).cooldown(36000).epPercent(5.0))
                .act(Act.of("dismiss_ascension", "Dismiss the Ascension", Wave3Head::dismiss).cooldown(20)));

        // ------------------------------------------------------------ hands
        out.accept(RelicDef.of("claws_of_greed", "Claws of Greed", Grade.SPECIAL, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0xE2CFA0, 0x4A4440, 0xFFC83C)
                .desc("Black claws curl round your hands. You sneak faster and your presence is hard to feel. Once a day a blow that lands snatches whatever the creature holds into your own hand.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_CLAWS)).passive(Passive.attr("minecraft:player.sneaking_speed", 0.3, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:presence_concealment")));

        out.accept(RelicDef.of("gloves_of_instant_cleanliness", "Gloves of Instant Cleanliness", Grade.SPECIAL, RelicSlot.HANDS)
                .look(Look.GLOVES).palette(0x7C8DB0, 0xE6ECFA, 0xFFD866)
                .desc("Snap your fingers and everything within three blocks is spotless: you and your friends shed nausea, hunger, blindness and poison, and cobwebs and flames are swept away. Ten snaps a day. Curses stay.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_CLEAN))
                .act(Act.of("snap", "Snap Clean", Wave3Hands::snap).cooldown(20).usesPerDay(Wave3Rules.CLEAN_USES)));

        out.accept(RelicDef.of("elemental_hand_wraps", "Elemental Hand Wraps", Grade.UNIQUE, RelicSlot.HANDS)
                .attune().look(Look.GLOVES).palette(0xC93A34, 0xE8C34A, 0x9DE8F0)
                .desc("Ribbons that bind your fists. Turn them to fire, cold, lightning, thunder or acid: every bare-fisted blow then deals 3 extra damage of that element with its own sting. Turn them once more to quiet them.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_WRAPS))
                .act(Act.of("elemental_infusion", "Elemental Infusion", Wave3Hands::infusion).cooldown(10)));

        out.accept(RelicDef.of("thermo_gloves", "Thermo-Gloves", Grade.SPECIAL, RelicSlot.HANDS)
                .look(Look.GLOVES).palette(0xB02A2E, 0x7FC4E4, 0xFF9A3C)
                .desc("One red glove, one blue. Bare fists carry +2 fire or +2 cold as you choose; the red touch lights flammable things, the blue freezes water; twice a day clap them for a cloud of scalding steam. You shrug off a sixth of fire and frost.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_THERMO)).passive(Passive.resist("minecraft:is_fire", 0.15)).passive(Passive.resist("minecraft:is_freezing", 0.15))
                .act(Act.of("thermo_mode", "Red or Blue", Wave3Hands::thermoMode).cooldown(10))
                .act(Act.of("red_touch", "Ignite", Wave3Hands::ignite).cooldown(20))
                .act(Act.of("blue_touch", "Freeze Water", Wave3Hands::freeze).cooldown(40))
                .act(Act.of("steam_cloud", "Steam Cloud", Wave3Hands::steam).cooldown(100).usesPerDay(Wave3Rules.STEAM_USES)));

        out.accept(RelicDef.of("gauntlets_of_eldritch_ferocity", "Gauntlets of Eldritch Ferocity", Grade.RARE, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0x5E4636, 0x8A8E96, 0x52F2E0).passive(ANY).passive(Passive.flag(Wave3Rules.F_GAUNTLETS))
                .desc("Stone-plated gauntlets carved with runes. Eldritch Blast: a force beam (2 to 4, never under 2). Eldritch Ferocity: two beams (up to 3 each); on a 1 or 2 of a d20 the second turns on you and lowers your maximum health until you sleep.")
                .passive(Passive.attr("minecraft:generic.armor", 2.0, Operation.ADD_VALUE))
                .act(Act.of("eldritch_blast", "Eldritch Blast", Wave3Hands::blast).cooldown(24).mana(2))
                .act(Act.of("eldritch_ferocity", "Eldritch Ferocity", Wave3Hands::twin).cooldown(50).mana(4)));

        // ------------------------------------------------------------ belt
        out.accept(RelicDef.of("alchemists_belt", "Alchemist's Belt", Grade.UNIQUE, RelicSlot.BELT)
                .attune().look(Look.BELT).palette(0x8A5A34, 0xB8C0CC, 0x3CC85A)
                .desc("Six vials hang from this belt, each good once a day: red heals, blue shields, green regenerates (best in the sun), yellow arms your friends, purple charms a creature, black lets you shrug off one killing blow.")
                .passive(ANY).passive(Passive.flag(Wave3Rules.F_ALCHEMIST)).passive(Passive.attr("minecraft:generic.max_health", 2.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("vial_red", "Healing Potion", Wave3Belt::red).cooldown(20).usesPerDay(1))
                .act(Act.of("vial_blue", "Shield Potion", Wave3Belt::blue).cooldown(20).usesPerDay(1))
                .act(Act.of("vial_green", "Regeneration Potion", Wave3Belt::green).cooldown(20).usesPerDay(1))
                .act(Act.of("vial_yellow", "Enlightening Potion", Wave3Belt::yellow).cooldown(20).usesPerDay(1))
                .act(Act.of("vial_purple", "Love Potion", Wave3Belt::purple).cooldown(20).usesPerDay(1))
                .act(Act.of("vial_black", "Undeath Potion", Wave3Belt::black).cooldown(20).usesPerDay(1)));

        // ------------------------------------------------------------ consumables
        out.accept(RelicDef.of("genie_gin", "Genie Gin", Grade.UNIQUE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xC48AA8, 0x8B5A3A, 0x8AF0DC)
                .desc("Open it and a minor djinni grants one wish for a day: Power, Fortune or Fame. Sneak while you drink to turn the bottle to the next wish without using it up.")
                .act(Act.of("make_a_wish", "Make a Wish", Wave3Goods::wish)));

        out.accept(RelicDef.of("mad_marbles", "Mad Marbles", Grade.SPECIAL, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0xA8704A, 0xD8EEF0, 0xE250D8)
                .desc("A sack of glass marbles with lightning in them. Pour them on the ground in front of you: the first creature to step there falls and the marbles go off in sparks that blind it; the others only slip.")
                .act(Act.of("pour_marbles", "Pour the Marbles", Wave3Goods::pour).cooldown(20)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave3Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onApplicable);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onWake);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave3Events::onStopping);
    }
}
