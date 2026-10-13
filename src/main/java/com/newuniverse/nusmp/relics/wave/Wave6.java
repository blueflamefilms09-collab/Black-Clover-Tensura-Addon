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
 * Relics of Many Worlds, wave 6: ten chest armours and robes (Skycrystal Armor, Slime Armour, Armor of the Defiant Dawn, Suit of the Astral Overseer,
 * Shadow Robes, Mistwalker Robe, Neket's Practical Mage Robe, Armor of Plot, Z'ar Sancuis, Skitch's Smuggling Coat). {@link #defs} lists them for the
 * catalog (the core registers the items), {@link #hooks} registers the game-bus listeners once. The long card texts, with every tabletop rule
 * turned into a game rule, are in the lang fragment relics_wave6.lang.json and docs/doc_wave6.md.
 */
public final class Wave6 {
    private Wave6() {}

    private static final String MAX_HEALTH = "minecraft:generic.max_health", SPEED = "minecraft:generic.movement_speed", KNOCKBACK = "minecraft:generic.knockback_resistance",
            MAX_MAGICULE = "tensura:max_magicule";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Skycrystal Armor (legendary, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_SKY, "Skycrystal Armor", Grade.LEGEND, RelicSlot.CHEST).attune().vessel()
                .palette(0x1B2338, 0xB8D8F0, 0x8FD8FF).look(Look.PLATE).armor(10, 3.5)
                .desc("Giant-forged crystal plate with ten charges that regrow at dawn. It drinks spells aimed at you, casts Absorb Elements, Shield, Globe of Invulnerability and Invulnerability, and holds time at bay.")
                .passive(Passive.attr(MAX_HEALTH, 4.0, Operation.ADD_VALUE))
                .passive(Passive.attr(KNOCKBACK, 0.1, Operation.ADD_VALUE))
                .passive(Passive.attr(SPEED, -0.04, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.flag(Wave6Tune.F_SKY))
                .passive(Passive.skill(Wave6Skills.MAGIC_RESISTANCE))
                .passive(Passive.skill(Wave6Skills.ABNORMAL_CONDITION_RESISTANCE))
                .passive(Passive.skill(Wave6Skills.SELF_REGENERATION))
                .act(Act.of("absorb_elements", "Absorb Elements", Wave6Acts::skyAbsorbElements).cooldown(200))
                .act(Act.of("shield", "Shield", Wave6Acts::skyShield).cooldown(100))
                .act(Act.of("globe", "Globe of Invulnerability", Wave6Acts::skyGlobe).cooldown(1200).epPercent(1.0))
                .act(Act.of("invulnerability", "Invulnerability", Wave6Acts::skyInvulnerability).cooldown(6000).epPercent(3.0)));

        // ---------------------------------------------------------------- Slime Armour (very rare)
        out.accept(RelicDef.of(Wave6Tune.ID_SLIME, "Slime Armour", Grade.UNIQUE, RelicSlot.CHEST)
                .palette(0x3A4A44, 0x9FE8C8, 0x6BFFB5).look(Look.PLATE).armor(7, 1.0)
                .desc("A cuirass that constantly seeps slime: blades slide off it, hammers do not, and no grip or web holds you. Three times a day it melts you into a small, soft slime for a minute.")
                .passive(Passive.flag(Wave6Tune.F_SLIME))
                .passive(Passive.skill(Wave6Skills.PARALYSIS_RESISTANCE))
                .act(Act.of("slime_form", "Slime Form", Wave6Acts::slimeForm).cooldown(100)));

        // ---------------------------------------------------------------- Armor of the Defiant Dawn (very rare, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_DAWN, "Armor of the Defiant Dawn", Grade.UNIQUE, RelicSlot.CHEST).attune()
                .palette(0x2B211D, 0xE6B94A, 0xFFE9A0).look(Look.PLATE).armor(8, 2.0)
                .desc("Armour infused with the Eternal Light. Dawnlight shows what hides and lights the dark; when a hard blow lands, one of its three charges flashes a blinding, burning light around you.")
                .passive(Passive.flag(Wave6Tune.F_DAWN))
                .act(Act.of("dawnlight", "Dawnlight", Wave6Acts::dawnlight).cooldown(20))
                .act(Act.of("luminous_retribution", "Luminous Retribution", Wave6Acts::dawnFlashAct).cooldown(200).mana(5)));

        // ---------------------------------------------------------------- Suit of the Astral Overseer (legendary, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_ASTRAL, "Suit of the Astral Overseer", Grade.LEGEND, RelicSlot.CHEST).attune().vessel()
                .palette(0x5B2E91, 0xE6E8F5, 0xB070FF).look(Look.SUIT).armor(6, 2.0)
                .desc("A light suit woven from the space between worlds: you fly, you never fall, the first two spells of the day pass through you, and crossing between worlds leaves no mark.")
                .passive(Passive.effect("minecraft:slow_falling", 0))
                .passive(Passive.flag(Wave6Tune.F_ASTRAL))
                .passive(Passive.skill(Wave6Skills.GRAVITY_ATTACK_RESISTANCE))
                .passive(Passive.skill(Wave6Skills.SPATIAL_ATTACK_RESISTANCE))
                .act(Act.of("zero_gravity", "Zero Gravity", Wave6Acts::zeroGravity).cooldown(40)));

        // ---------------------------------------------------------------- Shadow Robes (very rare, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_SHADOW, "Shadow Robes", Grade.UNIQUE, RelicSlot.CHEST).attune()
                .palette(0x0E0E14, 0x8D8D9C, 0x6A5CFF).look(Look.ROBE).armor(4, 1.0)
                .desc("Robes that carry a supernatural chill: cold barely hurts you, sneaking in the dark hides you, and twice a day you step through the shadows, leaving a cloud of necrotic energy behind.")
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.flag(Wave6Tune.F_SHADOW))
                .passive(Passive.skill(Wave6Skills.COLD_RESISTANCE))
                .passive(Passive.skill(Wave6Skills.DARKNESS_ATTACK_RESISTANCE))
                .act(Act.of("shadow_step", "Shadow Step", Wave6Acts::shadowStep).cooldown(100)));

        // ---------------------------------------------------------------- Mistwalker Robe (rare, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_MIST, "Mistwalker Robe", Grade.RARE, RelicSlot.CHEST).attune()
                .palette(0x5A3A7A, 0xC9A94F, 0xA8C8D8).look(Look.ROBE).armor(3, 0.5)
                .desc("A hooded robe that exudes mist. Five charges (1d4+1 back at dawn) buy Misty Step, Levitate, Gaseous Form, Far Step and Wind Walk. Spending the last one may throw you off course.")
                .act(Act.of("misty_step", "Misty Step", Wave6Acts::mistyStep).cooldown(40))
                .act(Act.of("levitate", "Levitate", Wave6Acts::levitate).cooldown(100))
                .act(Act.of("gaseous_form", "Gaseous Form", Wave6Acts::gaseousForm).cooldown(200))
                .act(Act.of("far_step", "Far Step", Wave6Acts::farStep).cooldown(200))
                .act(Act.of("wind_walk", "Wind Walk", Wave6Acts::windWalk).cooldown(1200)));

        // ---------------------------------------------------------------- Neket's Practical Mage Robe (rare, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_NEKET, "Neket's Practical Mage Robe", Grade.RARE, RelicSlot.CHEST).attune()
                .palette(0x4C4F8C, 0xC9C6EC, 0x9AA4FF).look(Look.ROBE).armor(3, 0.5)
                .desc("A tidy robe with two bottomless side pockets, straps that return what you drop in a fight, and up to five stored Mage Armors that it casts on you by itself when you are attacked.")
                .passive(Passive.flag(Wave6Tune.F_NEKET))
                .passive(Passive.skill(Wave6Skills.DANGER_SENSE))
                .passive(Passive.attr(MAX_MAGICULE, 0.05, Operation.ADD_MULTIPLIED_TOTAL))
                .act(Act.of("left_pocket", "Left Pocket", Wave6Acts::leftPocket).cooldown(10))
                .act(Act.of("right_pocket", "Right Pocket", Wave6Acts::rightPocket).cooldown(10))
                .act(Act.of("mage_armor", "Mage Armor", Wave6Acts::mageArmor).cooldown(40))
                .act(Act.of("store_spell", "Store Spell", Wave6Acts::storeSpell).cooldown(100).mana(Wave6Tune.MAGE_STORE_MANA)));

        // ---------------------------------------------------------------- Armor of Plot (legendary, attunement or being likeable)
        out.accept(RelicDef.of(Wave6Tune.ID_PLOT, "Armor of Plot", Grade.LEGEND, RelicSlot.CHEST).attune().vessel()
                .palette(0x6E7280, 0xC9A24A, 0x7FB4FF).look(Look.PLATE).armor(9, 4.0)
                .desc("Overcome all odds. Blows have a way of missing you at the last second, more so the closer you are to the end, and three plot points a story keep you alive past a fatal blow. At zero, the story runs out.")
                .passive(Passive.resist("minecraft:is_fire", 0.2))
                .passive(Passive.resist("minecraft:is_explosion", 0.2))
                .passive(Passive.resist("minecraft:is_projectile", 0.2))
                .passive(Passive.resist("minecraft:is_lightning", 0.2))
                .passive(Passive.flag(Wave6Tune.F_PLOT))
                .passive(Passive.skill(Wave6Skills.ABNORMAL_CONDITION_RESISTANCE)));

        // ---------------------------------------------------------------- Z'ar Sancuis, Plate of the Red Prophet (legendary, attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_ZAR, "Z'ar Sancuis - Plate of the Red Prophet", Grade.LEGEND, RelicSlot.CHEST).attune().vessel()
                .palette(0x3B3E46, 0xC9A24A, 0x9E1F2A).look(Look.PLATE).armor(10, 4.0)
                .desc("Heavy plate of a red prophet. Twice a day it shrugs off an effect that would have held you, or an ally's; each blow on a hostile foe heals you; allies fighting beside you hit harder.")
                .passive(Passive.attr(KNOCKBACK, 0.2, Operation.ADD_VALUE))
                .passive(Passive.attr(SPEED, -0.03, Operation.ADD_MULTIPLIED_TOTAL))
                .passive(Passive.flag(Wave6Tune.F_ZAR))
                .passive(Passive.skill(Wave6Skills.ABNORMAL_CONDITION_RESISTANCE))
                .act(Act.of("legendary_resistance", "Legendary Resistance (ally)", Wave6Acts::zarAbsolve).cooldown(100)));

        // ---------------------------------------------------------------- Skitch's Smuggling Coat (very rare, optional attunement)
        out.accept(RelicDef.of(Wave6Tune.ID_COAT, "Skitch's Smuggling Coat", Grade.UNIQUE, RelicSlot.CHEST)
                .palette(0x6B4A2E, 0x9A7248, 0xC8A060).look(Look.COAT).armor(5, 0.5)
                .desc("A patchwork coat with fifty pockets in unlikely seams: a deep pocket of six rows and a seam pocket of three, both bigger on the inside, open at any time and wearable without attunement.")
                .act(Act.of("deep_pockets", "Deep Pockets", Wave6Acts::deepPockets).cooldown(10))
                .act(Act.of("seam_pockets", "Seam Pockets", Wave6Acts::seamPockets).cooldown(10)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        Wave6Events.register();
        Wave6LostFound.register();
    }
}
