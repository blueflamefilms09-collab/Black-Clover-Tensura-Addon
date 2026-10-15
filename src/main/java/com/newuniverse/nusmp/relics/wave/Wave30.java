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
 * Why: the wave has twelve relics that interact (the pair, the set, the Stones in the Gauntlet), so the defs are listed together to keep ids, flags and
 * costs consistent.
 * <p>
 * Relics of Many Worlds, part 4, wave 30 (the special wave): the Hand of Vecna and the Eye of Vecna (a bonded pair with a set bonus), the Deck of Many
 * Things (a 22-card draw), the Infinity Gauntlet with the six Infinity Stones (a set), the staff Paradox and Warren's Key. The core registers the items;
 * {@link #defs} lists them and {@link #hooks} registers the game-bus listeners once. The code is split over {@link Wave30Vecna}, {@link Wave30Deck},
 * {@link Wave30Stones}, {@link Wave30Paradox}, {@link Wave30Key}, {@link Wave30Events} and {@link Wave30Bond}; the pure rules are {@link Wave30Rules} and
 * {@link Wave30DeckMath}. The long card texts, with every tabletop rule turned into a game rule, are in the lang fragment relics_wave30.lang.json and
 * in docs/doc_wave30.md.
 */
public final class Wave30 {
    private Wave30() {}

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Hand of Vecna (artifact, GOD, hands, attunement, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_HAND, "Hand of Vecna", Grade.GOD, RelicSlot.HANDS).attune().vessel()
                .palette(0x8E8A78, 0xC9A24A, 0xE0245E).look(Look.GAUNTLETS)
                .desc("A withered grey hand in an ornate gold gauntlet. Attuning grafts it to your wrist for as long as you live: strength of a giant, cold in every blow, and a whisper that wants you to do evil. Worn with the Eye, it is more.")
                .passive(Passive.attr(Wave30Tune.MC_ATTACK, Wave30Tune.HAND_ATTACK, Operation.ADD_VALUE))
                .passive(Passive.attr(Wave30Tune.MC_ARMOR, Wave30Tune.HAND_ARMOR, Operation.ADD_VALUE))
                .passive(Passive.attr(Wave30Tune.MC_TOUGHNESS, Wave30Tune.HAND_TOUGHNESS, Operation.ADD_VALUE))
                .passive(Passive.flag(Wave30Tune.F_HAND))
                .act(Act.of("touch_of_death", "Touch of Death", Wave30Vecna::touch).cooldown(Wave30Tune.TOUCH_COOLDOWN).epPercent(Wave30Tune.TOUCH_EP))
                .act(Act.of("rimed_grasp", "Rimed Grasp", Wave30Vecna::grasp).cooldown(Wave30Tune.GRASP_COOLDOWN).mana((int) Wave30Tune.GRASP_MANA))
                .act(Act.of("skeleton_to_jelly", "Skeleton to Jelly", Wave30Vecna::jelly).cooldown(Wave30Tune.JELLY_COOLDOWN).epPercent(Wave30Tune.JELLY_EP))
                .act(Act.of("choose_wish", "Choose the Wish", Wave30Vecna::wishPick).cooldown(10))
                .act(Act.of("wish", "Make the Wish", Wave30Vecna::wish).cooldown(40)));

        // ---------------------------------------------------------------- Eye of Vecna (artifact, GOD, head, attunement, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_EYE, "Eye of Vecna", Grade.GOD, RelicSlot.HEAD).attune().vessel()
                .palette(0xC9A24A, 0xE0245E, 0xFFD84A).look(Look.CIRCLET).armor(2, 2.0)
                .desc("A pink-irised eye on a red cord. Attuning presses it into your socket for as long as you live: it sees through walls and in the dark, casts five terrible spells, and sometimes, one use in twenty, hungers for you. Worn with the Hand, it is more.")
                .passive(Passive.skill(Wave30Tune.SK_EYE_OF_TRUTH))
                .passive(Passive.skill(Wave30Tune.SK_FARSIGHT))
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.flag(Wave30Tune.F_EYE))
                .act(Act.of("clairvoyance", "Clairvoyance", Wave30Vecna::clairvoyance).cooldown(Wave30Tune.CLAIR_COOLDOWN).mana(4))
                .act(Act.of("crown_of_madness", "Crown of Madness", Wave30Vecna::crown).cooldown(Wave30Tune.CROWN_COOLDOWN).mana(3))
                .act(Act.of("disintegrate", "Disintegrate", Wave30Vecna::disintegrate).cooldown(Wave30Tune.DISINTEGRATE_COOLDOWN).epPercent(Wave30Tune.DISINTEGRATE_EP))
                .act(Act.of("dominate_monster", "Dominate Monster", Wave30Vecna::dominate).cooldown(Wave30Tune.DOMINATE_COOLDOWN).mana((int) Wave30Tune.DOMINATE_MANA))
                .act(Act.of("eyebite", "Eyebite", Wave30Vecna::eyebite).cooldown(Wave30Tune.EYEBITE_COOLDOWN).mana(3)));

        // ---------------------------------------------------------------- Deck of Many Things (artifact, GOD, charm, vessel; no attunement)
        out.accept(RelicDef.of(Wave30Tune.ID_DECK, "Deck of Many Things", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0x1A1A22, 0xE6B94A, 0xFFD27A).look(Look.CHARM)
                .desc("Twenty-two cards in a gold-edged case. Choose how many to draw (one, two or three a day) and you draw exactly that many: wonders and ruin, nothing that kills and nothing that takes a relic. After the twenty-second the deck crumbles to dust.")
                .act(Act.of("draw_one", "Draw One Card", c -> Wave30Deck.draw(c, 1)).cooldown(Wave30Tune.DECK_ACT_COOLDOWN))
                .act(Act.of("draw_two", "Draw Two Cards", c -> Wave30Deck.draw(c, 2)).cooldown(Wave30Tune.DECK_ACT_COOLDOWN))
                .act(Act.of("draw_three", "Draw Three Cards", c -> Wave30Deck.draw(c, 3)).cooldown(Wave30Tune.DECK_ACT_COOLDOWN))
                .act(Act.of("look", "Look at the Deck", Wave30Deck::peek).cooldown(10)));

        // ---------------------------------------------------------------- The Infinity Gauntlet (artifact, GENESIS, hands, attunement, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_GAUNTLET, "The Infinity Gauntlet", Grade.GENESIS, RelicSlot.HANDS).attune().vessel()
                .palette(0x20202A, 0xC9A24A, 0xFFD27A).look(Look.GAUNTLETS)
                .desc("A black gauntlet with a ribcage palm and six sockets. Every Infinity Stone you carry is set in it and answers through it, stronger for each; with all six it makes the Greater Wish and the Snap, once a day each.")
                .passive(Passive.flag(Wave30Tune.F_GAUNTLET))
                .act(Act.of("greater_wish", "Greater Wish", Wave30Stones::greatWish).cooldown(100))
                .act(Act.of("choose_wish", "Choose the Wish", Wave30Stones::greatWishPick).cooldown(10))
                .act(Act.of("stone_space", "Space Stone", c -> Wave30Stones.through(c, 0)).cooldown(Wave30Tune.SPACE_COOLDOWN).mana((int) Wave30Tune.SPACE_MANA))
                .act(Act.of("stone_mind", "Mind Stone", c -> Wave30Stones.through(c, 1)).cooldown(Wave30Tune.MIND_COOLDOWN))
                .act(Act.of("stone_power", "Power Stone", c -> Wave30Stones.through(c, 2)).cooldown(Wave30Tune.POWER_COOLDOWN).epPercent(Wave30Tune.POWER_EP))
                .act(Act.of("stone_time", "Time Stone", c -> Wave30Stones.through(c, 3)).cooldown(2400))
                .act(Act.of("stone_reality", "Reality Stone", c -> Wave30Stones.through(c, 4)).cooldown(Wave30Tune.REALITY_COOLDOWN).mana(3))
                .act(Act.of("stone_soul", "Soul Stone", c -> Wave30Stones.through(c, 5)).cooldown(20))
                .act(Act.of("snap", "The Snap", Wave30Stones::snap).cooldown(200).epPercent(Wave30Tune.SNAP_EP)));

        // ---------------------------------------------------------------- the six Infinity Stones (artifact, GOD, charm, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_SPACE, "The Space Stone, or the Tesseract", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0x2A4A9A, 0x8FD0FF, 0x3A8BFF).look(Look.ORB)
                .desc("A small blue cube of folded space. Mark a place (sneak and use), then step to it with up to nine willing companions. It lends you Spatial Motion.")
                .passive(Passive.skill(Wave30Tune.SK_SPATIAL_MOTION))
                .act(Act.of("tesseract", "Tesseract", Wave30Stones::space).cooldown(Wave30Tune.SPACE_COOLDOWN).mana((int) Wave30Tune.SPACE_MANA))
                .act(Act.of("mark", "Mark the Place", Wave30Stones::spaceMark).cooldown(20)));

        out.accept(RelicDef.of(Wave30Tune.ID_MIND, "The Mind Stone", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0xB89A20, 0xFFF0A0, 0xFFD93A).look(Look.ORB)
                .desc("A small yellow gem that hears. Use it to sense every thinking creature within two hundred blocks; sneak and use to read the mind in front of you. It lends you Thought Communication and Analytical Appraisal.")
                .passive(Passive.skill(Wave30Tune.SK_THOUGHT_COMM))
                .passive(Passive.skill(Wave30Tune.SK_ANALYTICAL))
                .act(Act.of("sense_minds", "Sense Minds", Wave30Stones::mindSense).cooldown(Wave30Tune.MIND_COOLDOWN))
                .act(Act.of("read_mind", "Read Mind", Wave30Stones::mindRead).cooldown(100)));

        out.accept(RelicDef.of(Wave30Tune.ID_POWER, "The Power Stone", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0x6A2AB0, 0xD0A0FF, 0xA04AF0).look(Look.ORB)
                .desc("A small purple gem of raw power. Your magicule and aura pools swell by half and refill twice as fast; use it for a shock of power that throws back every foe round you.")
                .passive(Passive.attr(Wave30Tune.AT_MAX_MAGICULE, 0.5, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr(Wave30Tune.AT_MAX_AURA, 0.5, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr(Wave30Tune.AT_MAGICULE_REGEN, 1.0, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.flag(Wave30Tune.F_POWER))
                .act(Act.of("power_burst", "Power Burst", Wave30Stones::power).cooldown(Wave30Tune.POWER_COOLDOWN).epPercent(Wave30Tune.POWER_EP)));

        out.accept(RelicDef.of(Wave30Tune.ID_TIME, "The Time Stone, or the Eye of Agamotto", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0x2A8A4A, 0xA0FFC0, 0x3AE07A).look(Look.ORB)
                .desc("A small green gem that sees what is to come. Hold use to cast Foresight as a ritual: for twelve thousand ticks you cannot be surprised, luck is with you and the future is in sight. It lends you Danger Sense.")
                .passive(Passive.skill(Wave30Tune.SK_DANGER_SENSE))
                .act(Act.of("foresight", "Foresight", Wave30Stones::foresight).hold(Wave30Tune.FORESIGHT_HOLD).cooldown(2400)));

        out.accept(RelicDef.of(Wave30Tune.ID_REALITY, "The Reality Stone", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0xA02A2A, 0xFF9A9A, 0xE03A3A).look(Look.ORB)
                .desc("A small red gem that rewrites. Polymorph shrinks and weakens a mob in sight; sneak and use for Seeming (unseen for three minutes); the Mirage makes nearby foes lose you.")
                .act(Act.of("polymorph", "Polymorph", Wave30Stones::polymorph).cooldown(Wave30Tune.REALITY_COOLDOWN).mana(3))
                .act(Act.of("seeming", "Seeming", Wave30Stones::seeming).cooldown(1200).mana(3))
                .act(Act.of("mirage", "Mirage Arcane", Wave30Stones::mirage).cooldown(Wave30Tune.REALITY_COOLDOWN).mana(3)));

        out.accept(RelicDef.of(Wave30Tune.ID_SOUL, "The Soul Stone", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0xB05A10, 0xFFC080, 0xFF8A2A).look(Look.ORB)
                .desc("A small orange gem that keeps the dead. You see truly at any distance; the souls of those who die within sixty-four blocks are stored in it, and once a day one of them brings you back from a killing blow.")
                .passive(Passive.skill(Wave30Tune.SK_EYE_OF_TRUTH))
                .passive(Passive.flag(Wave30Tune.F_SOUL))
                .act(Act.of("souls", "Count the Souls", Wave30Stones::soulLedger).cooldown(20)));

        // ---------------------------------------------------------------- Paradox (artifact staff, GOD, main hand, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_PARADOX, "Paradox", Grade.GOD, RelicSlot.MAIN_HAND).attune().vessel().model3d()
                .palette(0x5A2A8A, 0x8CD8A0, 0xC8A0FF).look(Look.STAFF).melee(8.0, 1.4)
                .engrave("stagnation", "magic_interference", "sealing")
                .desc("The staff of the God of Time: a twisted thorn of violet crystal. Its blows carry extra weight and a critical on 19 and 20; twenty charges, refilled by 2d8+4 each dawn, buy slows, holds, haste, a stopped moment, a turned clock and missing years. Forcing it may cast you out of your own timeline.")
                .passive(Passive.flag(Wave30Tune.F_PARADOX))
                .act(Act.of("slow", "Slow", Wave30Paradox::slow).cooldown(40))
                .act(Act.of("phase_lock", "Phase Lock", Wave30Paradox::phaseLock).cooldown(40))
                .act(Act.of("phase_stasis", "Phase Stasis", Wave30Paradox::phaseStasis).cooldown(40))
                .act(Act.of("temporal_movement", "Temporal Movement", Wave30Paradox::temporalMovement).cooldown(40))
                .act(Act.of("time_stop", "Time Stop", Wave30Paradox::timeStop).cooldown(200))
                .act(Act.of("alternate_reality", "Alternate Reality", Wave30Paradox::alternateReality).cooldown(100))
                .act(Act.of("turn_the_clock", "Turn the Clock", Wave30Paradox::turnTheClock).cooldown(40))
                .act(Act.of("missing_time", "Missing Time", Wave30Paradox::missingTime).cooldown(200)));

        // ---------------------------------------------------------------- Warren's Key (artifact, GOD, charm, vessel)
        out.accept(RelicDef.of(Wave30Tune.ID_KEY, "Warren's Key", Grade.GOD, RelicSlot.CHARM).vessel()
                .palette(0x8A5A2A, 0xD8B060, 0xE8D8A0).look(Look.CHARM)
                .desc("A long bronze skeleton key with a labyrinth for a bit. Five a day it knocks or arcane-locks a door, gate, trapdoor or chest; five a day it joins the door in front of you to a door it remembers, as a portal that stays open an hour. It does not work inside the Dungeon.")
                .act(Act.of("door_to_door", "Door to Door", Wave30Key::doorToDoor).cooldown(40))
                .act(Act.of("choose_door", "Choose the Door", Wave30Key::chooseDoor).cooldown(10))
                .act(Act.of("knock", "Knock", Wave30Key::knock).cooldown(20))
                .act(Act.of("arcane_lock", "Arcane Lock", Wave30Key::arcaneLock).cooldown(20)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave30Events.register();
        Wave30Bond.register();
    }
}
