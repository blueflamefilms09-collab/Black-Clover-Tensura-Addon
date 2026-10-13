package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 2: five charms (the pouch of sand, the tome, the holy chains, the pact deck, the phantom boots) and seven
 * cloaks, capes and mantles worn on the back. The long card text of each, as game rules, is in the lang keys {@code item.nusmp.<id>.card};
 * the rules live in Wave2Rules / Wave2Deck / Wave2Tome / Wave2Lives (pure), the abilities in Wave2Acts and the handlers in Wave2Events.
 * <p>
 * Tabletop became Minecraft: one block is three feet, a long rest is a Minecraft day, a saving throw is a chance read off the target's size,
 * "advantage" is the better of two rolls (see Wave2Rules).
 */
public final class Wave2 {
    private Wave2() {}

    private static final Passive ANY = Passive.flag(Wave2Events.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ charms
        out.accept(RelicDef.of("sand_of_the_pocket", "Sand of the Pocket", Grade.RARE, RelicSlot.CHARM)
                .look(Look.CHARM).palette(0xC9B073, 0x7A6840, 0xFFE8A0)
                .desc("A pouch that is always full of sand. Throw a handful into the eyes of someone close: they are blinded and weakened for a round. It is also ideal for sandcastles.")
                .passive(ANY)
                .act(Act.of("pocket_sand", "Pocket Sand", Wave2Acts::pocketSand).cooldown(300))
                .act(Act.of("sandcastle", "Sandcastle", Wave2Acts::sandcastle).cooldown(3600)));

        out.accept(RelicDef.of("the_tome_of_wait_thats_illegal", "The Tome of \"Wait, That's Illegal\"", Grade.RARE, RelicSlot.CHARM)
                .attune().look(Look.TOME).palette(0x2F5A55, 0x8B5A2B, 0x4DE8E8)
                .desc("File an appeal against a harmful effect that hit you or someone near you (it costs 3% of your magicules). A d20 decides: denied (and fined by a paper imp), pending, re-rolled, granted or expedited. Beware Clause 66B.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_TOME)).passive(Passive.resist("minecraft:is_fire", 0.15))
                .act(Act.of("petition", "Petition for Retroactive Nullification", Wave2Acts::petition).cooldown(600).usesPerDay(2).mana(3)));

        out.accept(RelicDef.of("holy_chains", "Holy Chains", Grade.NORMAL, RelicSlot.CHARM)
                .look(Look.CHARM).palette(0xD4A82A, 0x8A6A12, 0xFFF1A0)
                .desc("Blessed gold chains. Every second they burn each evil creature (undead, daemons, devils) within three blocks of you with radiant damage.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_HOLY)));

        out.accept(RelicDef.of("pact_of_the_deck", "Pact of the Deck", Grade.SPECIAL, RelicSlot.CHARM)
                .look(Look.CHARM).palette(0xF2F0E6, 0xC9A94B, 0xFFF3C0)
                .desc("A 54-card pact deck, thrown as cards: the suit is the damage type, the value the damage; aces expose, jacks slow, queens throw again, kings knock down, jokers surge. Drawing the pact turns cards into magicules. It reshuffles at dawn.")
                .passive(ANY).passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("throw_card", "Throw a Card", Wave2Acts::throwCard).cooldown(20))
                .act(Act.of("pact_draw", "Draw the Pact", Wave2Acts::pactDraw).cooldown(1200))
                .act(Act.of("shuffle_deck", "Shuffle the Deck", Wave2Acts::shuffleDeck).cooldown(6000)));

        out.accept(RelicDef.of("phantom_steps", "Phantom Steps", Grade.RARE, RelicSlot.CHARM)
                .attune().look(Look.CHARM).palette(0x3A2A5E, 0x9AA0B4, 0x8C7CFF)
                .desc("Your steps are half in the ethereal plane: silent, unheard by the sculk, no sting from magma or thorns, a Danger Sense for traps and a hidden presence. Click your heels to slip forward through walls and creatures.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_PHANTOM)).passive(Passive.skill("tensura:danger_sense"))
                .act(Act.of("click_heels", "Click the Heels", Wave2Acts::clickHeels).cooldown(3600).usesPerDay(3)));

        // ------------------------------------------------------------ the back
        out.accept(RelicDef.of("cloak_of_the_depths", "Cloak of the Depths", Grade.RARE, RelicSlot.BACK)
                .attune().look(Look.CLOAK).palette(0x4B7F78, 0x1F2F5C, 0x8FD6E8)
                .desc("Breathe and swim like a squid (water breathing and hydraulic propulsion), reach further, and call on the tentacles: a cloud of ink to vanish in, lashing tentacles, and a grasp that roots and pulls one foe.")
                .passive(ANY).passive(Passive.effect("minecraft:water_breathing", 0)).passive(Passive.effect("minecraft:dolphins_grace", 0))
                .passive(Passive.skill("tensura:water_breathing")).passive(Passive.skill("tensura:hydraulic_propulsion"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 0.75, Operation.ADD_VALUE))
                .act(Act.of("ink_cloud", "Ink Cloud", Wave2Acts::inkCloud).cooldown(1200))
                .act(Act.of("writhing_touch", "Writhing Touch", Wave2Acts::writhingTouch).cooldown(100).mana(6))
                .act(Act.of("depths_grasp", "Depths Grasp", Wave2Acts::depthsGrasp).cooldown(1800).mana(10).usesPerDay(2)));

        out.accept(RelicDef.of("cloak_of_the_moth_rider", "Cloak of the Moth Rider", Grade.RARE, RelicSlot.BACK)
                .attune().look(Look.CLOAK).palette(0x8B4A2A, 0xF1E6D2, 0xFF9A3C)
                .desc("Turns into a giant moth that fights beside you for five minutes, throws a terrifying face to frighten the foes around you, and, with Coercion, makes one monster in five unwilling to pick you as its first target.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_MOTH)).passive(Passive.skill("tensura:coercion"))
                .act(Act.of("giant_moth", "Flight of the Giant Moth", Wave2Acts::giantMoth).cooldown(600).usesPerDay(1))
                .act(Act.of("dismiss_moth", "Dismiss the Moth", Wave2Acts::dismissMoth).cooldown(40))
                .act(Act.of("fearsome_visage", "Fearsome Visage", Wave2Acts::fearsomeVisage).cooldown(1200).usesPerDay(2)));

        out.accept(RelicDef.of("nightstalkers_mantle", "Nightstalker's Mantle", Grade.UNIQUE, RelicSlot.BACK)
                .look(Look.MANTLE).palette(0x1C1418, 0x6B4328, 0xB06A4A)
                .desc("Echolocation (ultrasonic waves) shows hostile creatures through walls, the wings open for sixty seconds of flight with a dashing wing beat, and a thunderous shriek stuns and deafens. It hides you in the dark.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_ECHO)).passive(Passive.flag(Wave2Events.F_MANTLE))
                .passive(Passive.skill("tensura:ultrasonic_waves")).passive(Passive.skill("tensura:sense_soundwave"))
                .act(Act.of("night_patrol", "Night Patrol", Wave2Acts::nightPatrol).cooldown(6000).epPercent(1.0))
                .act(Act.of("wing_dash", "Wing Dash", Wave2Acts::wingDash).cooldown(120))
                .act(Act.of("shriek", "Shriek", Wave2Acts::shriek).cooldown(3600).epPercent(1.5)));

        out.accept(RelicDef.of("cape_of_nine_lives", "Cape of Nine Lives", Grade.LEGEND, RelicSlot.BACK)
                .attune().vessel().look(Look.CAPE).palette(0x33323A, 0xC0C4CC, 0xFF4C9A)
                .desc("A cat spirit wards you from death nine times: a lethal blow leaves you on one heart instead, with regeneration to follow. Each rescue makes you more of a cat; the ninth sends the spirit away until a Nether Star calls it back.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_NINE))
                .act(Act.of("count_lives", "Count the Lives", Wave2Acts::countLives).cooldown(20))
                .act(Act.of("cat_offering", "Make a Wish", Wave2Acts::catOffering).cooldown(600)));

        out.accept(RelicDef.of("cloak_of_comfort", "Cloak of Comfort", Grade.SPECIAL, RelicSlot.BACK)
                .look(Look.CLOAK).palette(0xF4F1EA, 0xD8B24A, 0xB58AE0)
                .desc("Cool in the heat and warm in the cold: thermal-fluctuation, cold and heat resistance, and freezing and magma do you almost no harm. It also makes camp anywhere: rest, heal and set your spawn point.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_COMFORT))
                .passive(Passive.resist("minecraft:is_freezing", 0.9)).passive(Passive.resist("minecraft:hot_floor", 0.9))
                .passive(Passive.resist("minecraft:is_fire", 0.15))
                .passive(Passive.skill("tensura:thermal_fluctuation_resistance")).passive(Passive.skill("tensura:cold_resistance")).passive(Passive.skill("tensura:heat_resistance"))
                .act(Act.of("make_camp", "Make Camp", Wave2Acts::makeCamp).cooldown(6000)));

        out.accept(RelicDef.of("cloak_of_dichotomy", "Cloak of Dichotomy", Grade.RARE, RelicSlot.BACK)
                .attune().look(Look.CLOAK).palette(0xF0F0F0, 0x151518, 0xFFFFFF)
                .desc("Fades from white to black. A blow struck by hand makes your next spell 20% harder to resist; a spell makes your next blow strike with up to 4.8 extra radiant damage. Each is spent once.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_DICH))
                .act(Act.of("balance", "Read the Balance", Wave2Acts::balance).cooldown(20)));

        out.accept(RelicDef.of("cloak_mimic_tame", "Cloak Mimic, tame", Grade.UNIQUE, RelicSlot.BACK)
                .look(Look.CLOAK).palette(0x1E3FA0, 0xC01818, 0xF0C020)
                .desc("A living cloak: +2 armor, corrosion and acid resistance, night eyes and a Danger Sense that warns of ambushes, and it clings to you. It feeds on you at every dawn, on a caster's magicules first.")
                .passive(ANY).passive(Passive.flag(Wave2Events.F_MIMIC)).passive(Passive.flag(Wave2Events.F_ALERT))
                .passive(Passive.attr("minecraft:generic.armor", 2.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.2, Operation.ADD_VALUE))
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.skill("tensura:corrosion_resistance")).passive(Passive.skill("tensura:danger_sense"))
                .act(Act.of("reshape", "Shapechanger", Wave2Acts::reshape).cooldown(40)));
    }

    /** Registers the game-bus handlers; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave2Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, Wave2Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onUseFinish);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onEffectAdded);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onApplicable);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onGameEvent);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onSound);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onStepSound);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onLogin);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onRespawn);
        NeoForge.EVENT_BUS.addListener(Wave2Events::onServerStopping);
    }
}
