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
 * Why: Relics of Many Worlds, part 4, wave 25: nine charms (the Rings of Rock Eating, the Game, the Mimic, the Forgotten Ring, the Witch's Ring, the
 * Rose Petal, the Silver Tongued Lyre, the Soup Spoon of Improbability, the Spirit Lantern) and three helmets (the Bucket of Disguise, the Corpse Mask,
 * the Death Knight's Helmet). {@link #defs} lists them for the catalog (the core registers the items); {@link #hooks} registers the game-bus
 * listeners once. The long card texts, with every tabletop rule turned into a game rule, are in the lang fragment relics_wave25.lang.json and
 * relics4/doc_wave25.md. None is a weapon, so none carries engravings or a 3D model (the Rose Petal's halberd is a worn charm that blooms in the hand's
 * reach, as the lead set its slot to CHARM).
 */
public final class Wave25 {
    private Wave25() {}

    private static final String ARMOR = "minecraft:generic.armor", KNOCKBACK = "minecraft:generic.knockback_resistance",
            MAGIC_COST = "tensura:magic_cost_multiplier";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Ring of Rock Eating (rare, attunement)
        out.accept(RelicDef.of(Wave25Tune.ID_ROCK, "Ring of Rock Eating", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0xC87850, 0x7C8C3A, 0xA6D44A).look(Look.CHARM)
                .desc("Your belly learns to digest stone: eat cobblestone, gravel and flint to fill your hunger, and a hungry belly grinds a rock or two by itself. The Mines' own metal hardens you a little.")
                .passive(Passive.flag(Wave25Tune.F_ROCK))
                .passive(Passive.attr(ARMOR, 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr(KNOCKBACK, 0.1, Operation.ADD_VALUE))
                .act(Act.of("eat_rocks", "Eat Rocks", Wave25Acts::eatRocks).cooldown(Wave25Tune.ROCK_COOLDOWN)));

        // ---------------------------------------------------------------- Ring of the Game (rare, cursed)
        out.accept(RelicDef.of(Wave25Tune.ID_GAME, "Ring of the Game", Grade.RARE, RelicSlot.CHARM)
                .palette(0x16161C, 0xE8E8F0, 0xFFFFFF).look(Look.CHARM)
                .desc("Every few minutes you think of The Game and lose: a stab of psychic pain and maybe a loud curse that draws monsters. In return you can pass The Game on to the foes around you.")
                .passive(Passive.flag(Wave25Tune.F_GAME))
                .act(Act.of("pass_the_game", "Pass the Game", Wave25Acts::passTheGame).cooldown(Wave25Tune.PASS_COOLDOWN)));

        // ---------------------------------------------------------------- Ring of the Mimic (uncommon -> SPECIAL, attunement)
        out.accept(RelicDef.of(Wave25Tune.ID_MIMIC, "Ring of the Mimic", Grade.SPECIAL, RelicSlot.CHARM).attune()
                .palette(0x9A8A3A, 0x3AAE5A, 0xD6E84A).look(Look.CHARM)
                .desc("It trembles near mimics within ten blocks and lights them up. Its Guidance brings luck for a minute, but on a bad roll it spins, devours your finger and breaks its bond.")
                .passive(Passive.flag(Wave25Tune.F_MIMIC))
                .act(Act.of("guidance", "Guidance", Wave25Acts::guidance).cooldown(Wave25Tune.GUIDANCE_COOLDOWN)));

        // ---------------------------------------------------------------- Rose Petal (artifact -> GOD, attunement, vessel)
        out.accept(RelicDef.of(Wave25Tune.ID_ROSE, "Rose Petal", Grade.GOD, RelicSlot.CHARM).attune().vessel()
                .palette(0x5E7C93, 0x2FBF8A, 0xE8F6F0).look(Look.CHARM)
                .desc("A ghost-white rose that blooms into a thorned halberd: more damage and reach, thorns for those who strike you. With its twelve charges (2d6+2 back at dawn) it ensnares a foe, drags crowds in with vines, or makes you a Great Tree or a Primal Beast.")
                .passive(Passive.flag(Wave25Tune.F_ROSE))
                .act(Act.of("bloom", "Bloom", Wave25Acts::bloom).cooldown(20).toggle())
                .act(Act.of("ensnaring_strike", "Ensnaring Strike", Wave25Acts::ensnare).cooldown(20))
                .act(Act.of("grasping_vine", "Grasping Vine", Wave25Acts::grasp).cooldown(60))
                .act(Act.of("guardian_of_nature", "Guardian of Nature", Wave25Acts::guardian).cooldown(100)));

        // ---------------------------------------------------------------- Silver Tongued Lyre (rare, attunement)
        out.accept(RelicDef.of(Wave25Tune.ID_LYRE, "Silver Tongued Lyre", Grade.RARE, RelicSlot.CHARM).attune()
                .palette(0xB8A27E, 0xA8B0BC, 0xC03A3A).look(Look.CHARM)
                .desc("A devil's lyre: spells cost a little less, you can cast Hideous Laughter at a foe eighteen blocks away, and once a day a Hellish Dissonance rips necrotic damage through every humanoid and celestial that can hear it.")
                .passive(Passive.attr(MAGIC_COST, -0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("hideous_laughter", "Hideous Laughter", Wave25Acts::laughter).cooldown(Wave25Tune.LAUGH_COOLDOWN).mana(Wave25Tune.LAUGH_MANA))
                .act(Act.of("hellish_dissonance", "Hellish Dissonance", Wave25Acts::dissonance).cooldown(Wave25Tune.DISSONANCE_COOLDOWN).mana(Wave25Tune.DISSONANCE_MANA).usesPerDay(1)));

        // ---------------------------------------------------------------- Soup Spoon of Improbability (unknown -> SPECIAL)
        out.accept(RelicDef.of(Wave25Tune.ID_SPOON, "Soup Spoon of Improbability", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0xB8BCC4, 0x6A6A70, 0xFFE070).look(Look.CHARM)
                .desc("Merely having it emboldens you: a little more damage and a quicker arm. Once a day try a steady sip: it holds off weakness, slowness and the like for a while, but a spill costs you the boldness until dawn.")
                .passive(Passive.flag(Wave25Tune.F_SPOON))
                .act(Act.of("steady_sip", "Steady Sip", Wave25Acts::sip).cooldown(40).usesPerDay(1)));

        // ---------------------------------------------------------------- Spirit Lantern (uncommon -> SPECIAL)
        out.accept(RelicDef.of(Wave25Tune.ID_LANTERN, "Spirit Lantern", Grade.SPECIAL, RelicSlot.CHARM)
                .palette(0x2A3040, 0x3AA0B0, 0x7FE8FF).look(Look.CHARM)
                .desc("It sheds a cold blue light that lets you see in the dark and picks out undead near you. Hold its light on fresh remains and the ghost of the dead rises to tell you how it died.")
                .passive(Passive.flag(Wave25Tune.F_LANTERN))
                .act(Act.of("shine", "Shine on the Dead", Wave25Acts::shine).hold(Wave25Tune.SHINE_HOLD).cooldown(Wave25Tune.SHINE_COOLDOWN)));

        // ---------------------------------------------------------------- The Forgotten Ring (legendary, attunement, vessel)
        out.accept(RelicDef.of(Wave25Tune.ID_FORGOTTEN, "The Forgotten Ring", Grade.LEGEND, RelicSlot.CHARM).attune().vessel()
                .palette(0xBFC3C8, 0xEDE0BF, 0xE04040).look(Look.CHARM)
                .desc("An elven ring of malice. Vanish at will; mobs lose you beyond six blocks. You stay hidden until you strike, cast, use it again or take it off, and your first blow from hiding hits half again as hard.")
                .passive(Passive.flag(Wave25Tune.F_FORGOTTEN))
                .act(Act.of("vanish", "Vanish", Wave25Acts::vanish).cooldown(Wave25Tune.VANISH_COOLDOWN).mana(Wave25Tune.VANISH_MANA)));

        // ---------------------------------------------------------------- Witch's Ring (uncommon -> SPECIAL, attunement)
        out.accept(RelicDef.of(Wave25Tune.ID_WITCH, "Witch's Ring", Grade.SPECIAL, RelicSlot.CHARM).attune()
                .palette(0x2A262E, 0xB04AD0, 0xFF4AB8).look(Look.CHARM)
                .desc("Turn into a small beast (cat, frog, bat, owl, rat, raven, spider or goat) for two card-hours a day. Sneak and use it to change the beast, once a night.")
                .passive(Passive.flag(Wave25Tune.F_WITCH))
                .act(Act.of("shapechange", "Shapechange", Wave25Acts::shapechange).cooldown(Wave25Tune.FORM_COOLDOWN))
                .act(Act.of("change_beast", "Change the Beast", Wave25Acts::changeForm).usesPerDay(1)));

        // ---------------------------------------------------------------- Bucket of Disguise (unknown -> SPECIAL, attunement implied)
        out.accept(RelicDef.of(Wave25Tune.ID_BUCKET, "Bucket of Disguise", Grade.SPECIAL, RelicSlot.HEAD).attune()
                .palette(0xC08A4A, 0x20243A, 0xE0B070).look(Look.HAT).armor(1, 0.0)
                .desc("You truly believe you are invisible, and the thoughts of everyone near you drift into your head. Whatever has you in its sights thinks about it out loud.")
                .passive(Passive.flag(Wave25Tune.F_BUCKET))
                .passive(Passive.skill("tensura:thought_communication")));

        // ---------------------------------------------------------------- Corpse Mask (rare)
        out.accept(RelicDef.of(Wave25Tune.ID_MASK, "Corpse Mask", Grade.RARE, RelicSlot.HEAD)
                .palette(0x8E8A92, 0x7A4AA8, 0xA060D0).look(Look.HOOD).armor(1, 0.0)
                .desc("It drinks in the faces of humanoids that die near you (five at most). Twice a day wear one for five minutes: its own kind will not pick you as a target. Sneak and use it to choose the face.")
                .passive(Passive.flag(Wave25Tune.F_MASK))
                .act(Act.of("face_of_the_dead", "Face of the Dead", Wave25Acts::wearFace).cooldown(40).usesPerDay(Wave25Tune.MASK_PER_DAY))
                .act(Act.of("next_face", "Next Face", Wave25Acts::cycleFace).cooldown(10)));

        // ---------------------------------------------------------------- Death Knight's Helmet (very rare -> UNIQUE, attunement)
        out.accept(RelicDef.of(Wave25Tune.ID_DKH, "Death Knight's Helmet", Grade.UNIQUE, RelicSlot.HEAD).attune()
                .palette(0x4A6A90, 0x9A3AA8, 0xC050E0).look(Look.PLATE).armor(3, 2.0)
                .desc("Necrotic and poison blows glance off, fear cannot take you, and every day worn adds a heart of stamina, to four. But each dawn Death's Call tests you: fail and it burns your mind and takes the stamina back, harder every time.")
                .passive(Passive.flag(Wave25Tune.F_DKH))
                .passive(Passive.skill("tensura:poison_resistance"))
                .passive(Passive.resist("minecraft:wither", Wave25Tune.DKH_RESIST_WITHER)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave25Events.register();
    }
}
