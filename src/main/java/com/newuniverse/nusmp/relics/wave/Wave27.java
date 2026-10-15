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
 * Why: Relics of Many Worlds, part 4, wave 27: four headpieces (Soul Searchers, the Mad King's Crown, the Orb of Ancient Sight, the Trickster Mask)
 * and eight gloves and gauntlets (Arcane Trickster's Glove, Bear's Grip, Dragon's Fire Gauntlet, Gauntlet Mimic, Gauntlet of the Stormcaller,
 * Glove of Appreciation, Gloves of Opportunity, Goristro Charger). The long card text of each, as game rules, is in the lang keys
 * {@code item.nusmp.<id>.card}; the numbers are in {@link Wave27Rules} (pure), the abilities in {@link Wave27Head} and {@link Wave27Hands}, the
 * handlers in {@link Wave27Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day (uses and charges come back at dawn), five feet are one block, a saving throw is the
 * target's existence (Tensura EP) against the wearer's, advantage is a Luck or movement bonus, a spell slot is more maximum magicules.
 */
public final class Wave27 {
    private Wave27() {}

    private static final Passive ANY = Passive.flag(Wave27Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ head
        out.accept(RelicDef.of("soul_searchers", "Soul Searchers", Grade.RARE, RelicSlot.HEAD)
                .look(Look.CIRCLET).palette(0xA8803A, 0xD040B8, 0x8A3CD0).armor(1, 0.0)
                .desc("Stained glass sunglasses that read souls. Once every seven days, look at a creature and learn its true alignment; never twice on the same one, and a player knows when they were read. Tensura's magic sense is yours while you wear them.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_SOUL)).passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("sense_alignment", "Sense Alignment", Wave27Head::soulSearch).cooldown(40)));

        out.accept(RelicDef.of("the_mad_kings_crown", "The Mad King's Crown", Grade.SPECIAL, RelicSlot.HEAD)
                .look(Look.CROWN).palette(0x5A3A82, 0xE23A3A, 0xFF5050).armor(2, 0.0)
                .desc("A spiked helm-crown that swells your magicules by a fifth: one more spell slot, in the mod's terms. Wear it for two days straight and the nightmares of power, ruling and blood begin, and the weakness with them; take it off for a day to be free of it.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_CROWN))
                .passive(Passive.attr("tensura:max_magicule", Wave27Rules.CROWN_MAGICULE, Operation.ADD_MULTIPLIED_BASE)));

        out.accept(RelicDef.of("the_orb_of_ancient_sight", "The Orb of Ancient Sight", Grade.SPECIAL, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0xE08A30, 0xE83A3A, 0xFF8A8A).armor(1, 0.0)
                .desc("Once a day, the orb opens its eye: every creature carrying a magical effect within twenty-four blocks glows for twenty seconds, you read which effects each carries, and Tensura's magic sense is yours for the same time.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_ORB))
                .act(Act.of("ancient_sight", "Ancient Sight", Wave27Head::ancientSight).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("trickster_mask", "Trickster Mask", Grade.SPECIAL, RelicSlot.HEAD)
                .look(Look.HOOD).palette(0x3AA8A8, 0xF080A0, 0xD02A2A).armor(1, 0.0)
                .desc("A teal skull mask. Put it on and, for three hours (3000 ticks), everyone who looks at you sees a full skeleton: the undead do not pick you out and your presence is concealed. Once it ends or comes off it is dead until the next dawn.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_MASK))
                .act(Act.of("skeleton_guise", "Skeleton Guise", Wave27Head::guise).cooldown(40)));

        // ------------------------------------------------------------ hands
        out.accept(RelicDef.of("arcane_tricksters_glove", "Arcane Trickster's Glove", Grade.SPECIAL, RelicSlot.HANDS)
                .look(Look.GLOVES).palette(0x8A2CB8, 0xE8C040, 0xB860E0)
                .desc("Your Mage Hand turns invisible. Reach up to eight blocks to lift something from a creature's hands or a villager's pockets, or fetch a dropped item from twelve blocks away. The frail are easy to rob; a hand that is caught turns the creature on you.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_TRICKSTER))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("mage_hand", "Invisible Mage Hand", Wave27Hands::mageHand).cooldown(100)));

        out.accept(RelicDef.of("bears_grip", "Bear's Grip", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x7A4A28, 0xB8B0B0, 0xE8DCC8)
                .desc("Clawed leather gloves. You climb, leap and swim like a bear (higher jumps, easier swimming, a firmer stand against knockback), and Bear Grapple seizes the creature in reach and holds it still for four seconds.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_BEAR))
                .passive(Passive.attr("minecraft:generic.jump_strength", 0.1, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.water_movement_efficiency", 0.25, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.15, Operation.ADD_VALUE))
                .act(Act.of("bear_grapple", "Bear Grapple", Wave27Hands::bearGrapple).cooldown(200)));

        out.accept(RelicDef.of("dragons_fire_gauntlet", "Dragon's Fire Gauntlet", Grade.LEGEND, RelicSlot.HANDS)
                .attune().vessel().look(Look.GAUNTLETS).palette(0x4A3A3E, 0xF07A1A, 0xFFD040)
                .desc("Immune to fire, and Tensura's dragon ear lets you follow Draconic. Once a day, Burning Hands: a cone of fire five blocks long. Three charges (1 to 3 back at dawn) let the gauntlet turn aside an explosion, a shot or a lightning bolt that would hit you.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_DRAGON))
                .passive(Passive.effect("minecraft:fire_resistance", 0))
                .passive(Passive.resist("minecraft:is_fire", 0.9))
                .passive(Passive.attr("tensura:flame_resistance", 0.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:dragon_ear"))
                .act(Act.of("burning_hands", "Burning Hands", Wave27Hands::burningHands).cooldown(100).usesPerDay(1).epPercent(1.0)));

        out.accept(RelicDef.of("gauntlet_mimic", "Gauntlet Mimic", Grade.RARE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0x8A8E98, 0xE0B848, 0xE04C98)
                .desc("A living gauntlet. Bare-handed blows strike 3 harder and the mimic cycles between bludgeoning, piercing and slashing. It sticks to what it hits and holds it for three seconds, and each time it does it drinks two of your health (never below two hearts).")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_MIMIC))
                .act(Act.of("shapechanger", "Shapechanger", Wave27Hands::mimicShape).cooldown(10)));

        out.accept(RelicDef.of("gauntlet_of_the_stormcaller", "Gauntlet of the Stormcaller", Grade.RARE, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0x2A8A82, 0x58C8FF, 0xE8F8FF)
                .desc("Nine gems, nine charges (2 to 5 back at dawn). Thunderwave (1 charge) throws foes back; Call Lightning (3) drops a bolt where you look; Shocking Grasp is free and works even while stunned, when the gauntlet also shocks the nearest foe by itself.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_STORM))
                .passive(Passive.skill("tensura:electricity_resistance"))
                .act(Act.of("thunderwave", "Thunderwave", Wave27Hands::thunderwave).cooldown(60))
                .act(Act.of("call_lightning", "Call Lightning", Wave27Hands::callLightning).cooldown(100))
                .act(Act.of("shocking_grasp", "Shocking Grasp", Wave27Hands::shockingGrasp).cooldown(30)));

        out.accept(RelicDef.of("glove_of_appreciation", "Glove of Appreciation", Grade.UNIQUE, RelicSlot.HANDS)
                .look(Look.GLOVES).palette(0x8A5430, 0xE23030, 0xFFE070)
                .desc("A thumbs-up glove that grows warm with kindness: each compliment in chat that names you (or an animal you win over) gives it a charge, up to five. Spend one on a High-Five: a friend is mended, a foe that fails its save is high-fived into a stagger.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_GLOVE))
                .act(Act.of("high_five", "High-Five", Wave27Hands::highFive).cooldown(60)));

        out.accept(RelicDef.of("gloves_of_opportunity", "Gloves of Opportunity", Grade.UNIQUE, RelicSlot.HANDS)
                .look(Look.GAUNTLETS).palette(0xE8E8EC, 0x58D8E8, 0xFFFFFF)
                .desc("When a creature that holds something swings at you and fumbles (one blow in twenty goes wide), the gloves take what it holds. Lift does the same on command to the creature in front of you. Bosses and players cannot be robbed.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_OPPORTUNITY))
                .passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("lift", "Lift", Wave27Hands::lift).cooldown(600)));

        out.accept(RelicDef.of("goristro_charger", "Goristro Charger", Grade.RARE, RelicSlot.HANDS)
                .attune().look(Look.GAUNTLETS).palette(0xF0EEE8, 0xA87030, 0xC89040)
                .desc("Gauntlets of riveted goristro bone. Siege power: +2 attack damage and heavy knockback. Bare-handed blows strike 4 harder. Three times a day, Demonic Charge: rush a target within seven blocks and strike it for 16 more.")
                .passive(ANY).passive(Passive.flag(Wave27Rules.F_GORISTRO))
                .passive(Passive.attr("minecraft:generic.attack_damage", 2.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.attack_knockback", 0.5, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:strengthen_body"))
                .act(Act.of("demonic_charge", "Demonic Charge", Wave27Hands::demonicCharge).cooldown(100).usesPerDay(3).epPercent(1.0)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave27Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave27Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave27Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave27Events::onWake);
        NeoForge.EVENT_BUS.addListener(Wave27Events::onChat);
        NeoForge.EVENT_BUS.addListener(Wave27Events::onTame);
    }
}
