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
 * Why: this class only lists the twelve cards for the catalog and registers the game-bus handlers once; the numbers are in the pure
 * {@link Wave28Rules}, the abilities in {@link Wave28Hands}, {@link Wave28Neck}, {@link Wave28Potions} and {@link Wave28Belt}, the world-facing
 * handlers in {@link Wave28Events}. Tabletop became Minecraft: a long rest is a Minecraft day (uses per day come back at dawn), a saving throw is
 * Tensura existence or a share of health, a die is a flat or small random number, Strength is attack damage, Constitution is maximum health.
 * <p>
 * Relics of Many Worlds, part 4, wave 28: two gauntlets and a pair of gloves (the Hellfire Gauntlet, the Proficiency Gloves, the Gauntlet of
 * Gamelon), six necklaces (Chromatic Collar, Collar of the Penitent Dragon, Eye of the Aboleth, Fenrir's Collar, Sunheart Medallion, Witcher's
 * Medallion), the Buffoon's Pantaloons and two potions (Executioner's Bane, Perculiar Puking Poison). None is a weapon, so none has a 3D hand model:
 * the hands and necks are Curios items, the pantaloons a belt, the potions are drunk.
 */
public final class Wave28 {
    private Wave28() {}

    private static final Passive ANY = Passive.flag(Wave28Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Hellfire Gauntlet (legendary)
        out.accept(RelicDef.of("hellfire_gauntlet", "Hellfire Gauntlet", Grade.LEGEND, RelicSlot.HANDS)
                .attune().vessel().look(Look.GAUNTLETS).palette(0x1C181A, 0x7A2A18, 0xFF6A1A)
                .desc("A black spiked gauntlet with ever-burning fire to the elbow. +2 attack damage, +2d4 fire on every blow, claws in a bare fist, firebolt on use. "
                        + "After sixty burning blows it wakes: three charges a day buy fireball, a hell-quake and an aura of fire. Needs a hardy body.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_HELL))
                .passive(Passive.attr("minecraft:generic.attack_damage", 2.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:flame_attack_resistance"))
                .passive(Passive.resist("minecraft:is_fire", 0.5))
                .act(Act.of("firebolt", "Firebolt", Wave28Hands::firebolt).mana(1).cooldown(30))
                .act(Act.of("fireball", "Fireball", Wave28Hands::fireball).cooldown(60))
                .act(Act.of("hell_quake", "Hell-quake", Wave28Hands::quake).cooldown(100))
                .act(Act.of("fire_aura", "Aura of Fire", Wave28Hands::aura).cooldown(100)));

        // ---------------------------------------------------------------- Proficiency Gloves
        out.accept(RelicDef.of("proficiency_gloves", "Proficiency Gloves", Grade.SPECIAL, RelicSlot.HANDS)
                .look(Look.GLOVES).palette(0x6A4020, 0xE8B830, 0xFFD860)
                .desc("Brown leather gloves with gold plates. You take on the proficiency of whatever you pick up: blades hit harder, axes too and quicker, "
                        + "tools dig faster, bows aim truer, shields hold firmer, anything else a little of both.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_PROF)));

        // ---------------------------------------------------------------- The Gauntlet of Gamelon
        out.accept(RelicDef.of("the_gauntlet_of_gamelon", "The Gauntlet of Gamelon", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x4A2E6A, 0xD8B450, 0x9A7CFF)
                .desc("An ancient Hylian gauntlet with four gems: earth, fire, water, air. Three spells a gem. Use casts the turned gem's spell, sneak and use turns to the next. "
                        + "It sharpens your sorcery: spells cost a tenth less.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_GAMELON))
                .passive(Passive.attr("tensura:magic_cost_multiplier", -0.1, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.attr("tensura:max_magicule", 0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("cast", "Cast the Gem", Wave28Hands::gamelonCast).mana(2).cooldown(40))
                .act(Act.of("turn", "Turn the Gem", Wave28Hands::gamelonTurn).cooldown(6)));

        // ---------------------------------------------------------------- Chromatic Collar
        out.accept(RelicDef.of("chromatic_collar", "Chromatic Collar", Grade.RARE, RelicSlot.NECK)
                .look(Look.AMULET).palette(0x1E2024, 0x5AB048, 0xE04A3A)
                .desc("A black scaled collar set with five gems. Once a day roll a d6 for a dragon's resistance (acid, poison, fire, lightning or cold) that lasts until the next dawn; "
                        + "a 6 gives +1 armour and a resistance of your choice (sneak and use to choose).")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_CHROMA))
                .act(Act.of("draw", "Dragon's Resistance", Wave28Neck::chromaDraw).usesPerDay(1).cooldown(20))
                .act(Act.of("choose", "Choose the Resistance", Wave28Neck::chromaChoose).cooldown(10)));

        // ---------------------------------------------------------------- Collar of the Penitent Dragon
        out.accept(RelicDef.of("collar_of_the_penitent_dragon", "Collar of the Penitent Dragon", Grade.SPECIAL, RelicSlot.NECK)
                .attune().look(Look.AMULET).palette(0x8A8E98, 0xC8CCD8, 0xFFF2B0)
                .desc("A silver collar with a dragon's head. Bahamut's blessing: use it to breathe a cone of radiant light, and dragons will not hunt you unless you wrong them. "
                        + "Holy damage is easier to bear.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_PENITENT))
                .passive(Passive.skill("tensura:holy_attack_resistance"))
                .act(Act.of("radiant_breath", "Radiant Breath", Wave28Neck::breath).mana(3).cooldown(200)));

        // ---------------------------------------------------------------- Eye of the Aboleth
        out.accept(RelicDef.of("eye_of_the_aboleth", "Eye of the Aboleth", Grade.SPECIAL, RelicSlot.NECK)
                .look(Look.PENDANT).palette(0xB8BCC8, 0x7A7E90, 0xE8E040)
                .desc("A silver loop around a yellow slit-pupil eye. Use it on a creature within ten blocks: unless its will is far above yours it is charmed for a minute "
                        + "and will not strike you. A player is only dazed.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_ABOLETH))
                .act(Act.of("charm", "Charm", Wave28Neck::charm).mana(2).cooldown(300)));

        // ---------------------------------------------------------------- Fenrir's Collar
        out.accept(RelicDef.of("fenrirs_collar", "Fenrir's Collar", Grade.UNIQUE, RelicSlot.NECK)
                .attune().look(Look.AMULET).palette(0x2A2C34, 0xC8B070, 0xD02030)
                .desc("A crescent collar with a ruby. Use it to take the hybrid wolf form: stronger, faster, hardier, with a wolf's senses. At the full moon (or after a vigil under it) "
                        + "you keep your mind for five minutes; otherwise the wolf slips in ninety seconds and the curse takes its price.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_FENRIR))
                .act(Act.of("metamorphose", "Metamorphose", Wave28Neck::metamorphose).cooldown(100))
                .act(Act.of("moon_vigil", "Moon Vigil", Wave28Neck::vigil).needsSneak().usesPerDay(1).cooldown(100))
                .act(Act.of("shed_form", "Shed the Form", Wave28Neck::shed).cooldown(40)));

        // ---------------------------------------------------------------- Sunheart Medallion (legendary)
        out.accept(RelicDef.of("sunheart_medallion", "Sunheart Medallion", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.AMULET).palette(0xE0A030, 0xFFD870, 0xFFE070)
                .desc("A golden sun on a fine chain with ten charges, refilled by greeting dawn's first light. Sacred Flame on use; Vitality, Fire Shield, Mass Cure, Restoration, "
                        + "Heal and Regenerate spend charges; True Resurrection (once) shatters it. Disease cannot take you; the evil burn.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_SUN))
                .passive(Passive.skill("tensura:poison_resistance"))
                .act(Act.of("sacred_flame", "Sacred Flame", Wave28Neck::sacredFlame).cooldown(40))
                .act(Act.of("greet_dawn", "Greet the Dawn", Wave28Neck::greetDawn).cooldown(20))
                .act(Act.of("vitality", "Aura of Vitality", Wave28Neck::vitality).cooldown(100))
                .act(Act.of("fire_shield", "Fire Shield", Wave28Neck::fireShield).cooldown(200))
                .act(Act.of("mass_cure", "Mass Cure Wounds", Wave28Neck::massCure).cooldown(200))
                .act(Act.of("restoration", "Greater Restoration", Wave28Neck::restoration).cooldown(200))
                .act(Act.of("heal", "Heal", Wave28Neck::heal).cooldown(300))
                .act(Act.of("regenerate", "Regenerate", Wave28Neck::regenerate).cooldown(300))
                .act(Act.of("true_resurrection", "True Resurrection", Wave28Neck::resurrection).needsSneak().cooldown(100)));

        // ---------------------------------------------------------------- Witcher's Medallion (legendary)
        out.accept(RelicDef.of("witchers_medallion", "Witcher's Medallion", Grade.LEGEND, RelicSlot.NECK)
                .vessel().look(Look.PENDANT).palette(0x383C44, 0x9AA0AC, 0xE02020)
                .desc("A steel wolf's head with red eyes. It hums when monsters come within twenty blocks and tells you how many, how far and which way; "
                        + "it trembles before a storm and hums hard at a great presence. Use it for witcher senses: monsters glow for fifteen seconds.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_WITCHER))
                .passive(Passive.skill("tensura:danger_sense"))
                .passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("senses", "Witcher Senses", Wave28Neck::senses).mana(2).cooldown(Wave28Rules.SENSE_COOLDOWN)));

        // ---------------------------------------------------------------- Buffoon's Pantaloons
        out.accept(RelicDef.of("buffoons_pantaloons", "Buffoon's Pantaloons", Grade.RARE, RelicSlot.BELT)
                .look(Look.BELT).palette(0x8A2AC8, 0xFF5AB0, 0x40D8E8)
                .desc("Gaudy baggy pantaloons: +1 armour and a spring in your step (higher jump, a little faster, softer landings). But a jump or a blow can come up a natural 1 "
                        + "and the pantaloons fall to your ankles: slow and weak until you spend an action pulling them up.")
                .passive(ANY).passive(Passive.flag(Wave28Rules.F_PANTS))
                .passive(Passive.attr("minecraft:generic.armor", 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.jump_strength", 0.1, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.03, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 1.0, Operation.ADD_VALUE))
                .act(Act.of("pull_up", "Pull Up the Pantaloons", Wave28Belt::pullUp).cooldown(20)));

        // ---------------------------------------------------------------- Executioner's Bane (very rare)
        out.accept(RelicDef.of("executioners_bane", "Executioner's Bane", Grade.UNIQUE, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x16161C, 0xC8903A, 0x8030C0)
                .desc("A black crystal vial on a gold chain. Drink it and for three hours of the day you survive being beheaded: a killing blow leaves you on one hit point (once every ten seconds). "
                        + "You are slow and weak and cannot eat or drink; sleep in a bed to put your head back, or the body gives out.")
                .act(Act.of("behead", "Drink Executioner's Bane", Wave28Potions::behead)));

        // ---------------------------------------------------------------- Perculiar Puking Poison
        out.accept(RelicDef.of("perculiar_puking_poison", "Perculiar Puking Poison", Grade.SPECIAL, RelicSlot.CONSUMABLE)
                .look(Look.POTION).palette(0x5A4A6A, 0xA8A0B0, 0x70F0A0)
                .desc("A glass orb of swirling rainbow slime in a spiked claw. Drink it and make a Constitution save or be poisoned: six times, about every ten minutes of the hour, "
                        + "you puke up something strange (a broom, a chair, a coffin, a copy of your own head...).")
                .act(Act.of("drink", "Drink the Poison", Wave28Potions::drink)));
    }

    /** Registers the damage, target, jump, use, sleep, respawn and tick rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave28Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onDamagePre);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onJump);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onUseStart);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onWake);
        NeoForge.EVENT_BUS.addListener(Wave28Events::onRespawn);
    }
}
