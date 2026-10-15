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
 * Why: wave 20 of the Relics of Many Worlds, part 4: twelve weapons from the fourth card sheets (Demon Shovel, Dragoon's Halberd, Efreeti's Kusarigama,
 * Ilmare Gin The Starlight Silver, Katana of Carnage, Katana of the Crimson Shogun, Lightworker's Scepter, Mjolnir, Overprotective Halberd, Phoenix Flame,
 * Primal Chaos and Raven's Wing). All twelve are held in the main hand and drawn as 3D models.
 * <p>
 * The first act of a weapon runs on use, the second on sneak + use, further acts on the worn-relic key. The rules are in {@link Wave20Rules} (pure), the
 * acts in {@link Wave20Acts}, the damage, death and tick handlers in {@link Wave20Events}. Time scale as in the earlier waves: a long rest is a Minecraft day,
 * a short rest a couple of minutes, a round six seconds, five feet a block.
 */
public final class Wave20 {
    private Wave20() {}

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Demon Shovel
        out.accept(RelicDef.of("demon_shovel", "Demon Shovel", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.AXE).palette(0xC070EE, 0x2A1038, 0xE0A8FF)
                .desc("A fabulously lavender shovel made by a demon for unknown, probably evil, purposes. It hits with necrotic force, digs fast, "
                        + "coaxes the garden to grow (evil gardening) and opens a pit of shadow under your foes (sneak + use).")
                .passive(Passive.flag("w20_shovel"))
                .passive(Passive.attr("minecraft:player.block_break_speed", 0.5, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("evil_gardening", "Evil Gardening", Wave20Acts::evilGardening).cooldown(400))
                .act(Act.of("evil_hole", "Evil Hole Digging", Wave20Acts::evilHole).needsSneak().mana(2).cooldown(300))
                .melee(5.5, 1.2)
                .engrave("soul_eater", "ruination", "crushing"));

        // ---------------------------------------------------------------- Dragoon's Halberd
        out.accept(RelicDef.of("dragoons_halberd", "Dragoon's Halberd", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.STAFF).palette(0x1E2028, 0xDCB440, 0x4A78D8)
                .desc("The halberd of the Dragoons, who train to fight dragons. It hits draconic creatures harder, and a blow struck while falling "
                        + "bursts in two blocks with an element of your choice (use to change it); sneak + use leaps.")
                .passive(Passive.flag("w20_dragoon"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 0.75, Operation.ADD_VALUE))
                .act(Act.of("choose_element", "Choose the Element", Wave20Acts::chooseElement).cooldown(10))
                .act(Act.of("dragoon_leap", "Dragoon's Leap", Wave20Acts::dragoonLeap).needsSneak().cooldown(160))
                .melee(8.0, 1.0)
                .engrave("severance", "elemental_boost", "sturdy"));

        // ---------------------------------------------------------------- Efreeti's Kusarigama
        out.accept(RelicDef.of("efreetis_kusarigama", "Efreeti's Kusarigama", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.AXE).palette(0x6A2020, 0xFF8C1E, 0xFFB040)
                .desc("A flame sickle on a long chain with an ember at its end. It reaches further, burns what it cuts, and the Grip of the Fire Genie "
                        + "wraps the chain round a foe, pulls it in and scorches it for six seconds.")
                .passive(Passive.flag("w20_kusarigama"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.5, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_fire", 0.25))
                .act(Act.of("grip_of_the_fire_genie", "Grip of the Fire Genie", Wave20Acts::grip).cooldown(300))
                .melee(6.0, 1.3)
                .engrave("elemental_boost", "magic_weapon", "vigor"));

        // ---------------------------------------------------------------- Ilmare Gin, The Starlight Silver
        out.accept(RelicDef.of("ilmare_gin_the_starlight_silver", "Ilmare Gin, The Starlight Silver", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0x16205A, 0xDCEBFF, 0xBFE0FF)
                .desc("A slim midnight-blue katana flecked with starlight. Use throws it along your sight line (twenty blocks); within five seconds "
                        + "sneak + use steps you along the starlight to where it struck and into your hand.")
                .passive(Passive.flag("w20_gin"))
                .act(Act.of("starlight_throw", "Starlight Throw", Wave20Acts::starlightThrow).cooldown(100))
                .act(Act.of("star_step", "Star Step", Wave20Acts::starStep).needsSneak().cooldown(40))
                .melee(6.0, 1.7)
                .engrave("magic_weapon", "swift", "barrier_piercing"));

        // ---------------------------------------------------------------- Katana of Carnage
        out.accept(RelicDef.of("katana_of_carnage", "Katana of Carnage", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xB0B6C4, 0xB4142C, 0xFF2840)
                .desc("A +2 katana of sharpness with a blood-stained charm: one blow in twenty severs. Every kill within eight seconds of the last "
                        + "feeds Carnage (up to five stacks of added damage); use spends the stacks on speed, haste and a little blood back.")
                .passive(Passive.flag("w20_carnage"))
                .act(Act.of("bloodlust", "Bloodlust", Wave20Acts::bloodlust).cooldown(200))
                .melee(7.5, 1.6)
                .engrave("severance", "soul_eater", "swift"));

        // ---------------------------------------------------------------- Katana of the Crimson Shogun
        out.accept(RelicDef.of("katana_of_the_crimson_shogun", "Katana of the Crimson Shogun", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.SWORD).palette(0xB4BAC8, 0xD8AA3C, 0xFFE8B0)
                .desc("A +2 folded-steel katana. It may turn a melee blow aside and answer it (one in two, every three seconds), and once a day "
                        + "Steel Wind Strike flickers you between up to five foes within eight blocks.")
                .passive(Passive.flag("w20_shogun"))
                .act(Act.of("steel_wind_strike", "Steel Wind Strike", Wave20Acts::steelWindStrike).mana(5).usesPerDay(1).cooldown(200))
                .melee(9.5, 1.6)
                .engrave("severance", "sturdy", "magic_weapon"));

        // ---------------------------------------------------------------- Lightworker's Scepter
        out.accept(RelicDef.of("lightworkers_scepter", "Lightworker's Scepter", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0xE8DCA0, 0x8CD2F0, 0xCDF1FF)
                .desc("A feather-winged scepter with a kind voice. Rechannel Energy mends you and your friends (five a day), Soothing Presence calms a ring "
                        + "of six blocks (sneak + use), and the worn-relic key calls Mighty Ethereal Light. Once a day it pulls you out of a killing blow.")
                .passive(Passive.flag("w20_lightworker"))
                .passive(Passive.attr("tensura:max_magicule", 0.10, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.attr("tensura:magic_resistance", 0.15, Operation.ADD_VALUE))
                .passive(Passive.attr("tensura:darkness_resistance", 0.15, Operation.ADD_VALUE))
                .act(Act.of("rechannel_energy", "Rechannel Energy", Wave20Acts::rechannel).usesPerDay(5).cooldown(40))
                .act(Act.of("soothing_presence", "Soothing Presence", Wave20Acts::soothingPresence).needsSneak().usesPerDay(3).cooldown(100))
                .act(Act.of("mighty_ethereal_light", "Mighty Ethereal Light", Wave20Acts::etherealLight).mana(15).usesPerDay(1).cooldown(400))
                .melee(6.0, 1.2)
                .engrave("holy_weapon", "holy_coat", "restoration", "magic_capacity"));

        // ---------------------------------------------------------------- Mjolnir
        out.accept(RelicDef.of("mjolnir", "Mjolnir", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.HAMMER).palette(0x8C98AA, 0x74402E, 0xAAC8FF)
                .desc("The hammer of knotted steel. Use throws it down your sight line (it is back in your hand at once); sneak + use calls a Lightning Bolt "
                        + "once a day. It shrugs off lightning and shocks wet foes.")
                .passive(Passive.flag("w20_mjolnir"))
                .passive(Passive.resist("minecraft:lightning_bolt", 0.5))
                .act(Act.of("hammer_throw", "Hammer Throw", Wave20Acts::hammerThrow).cooldown(80))
                .act(Act.of("lightning_bolt", "Lightning Bolt", Wave20Acts::lightningBolt).needsSneak().usesPerDay(1).cooldown(100))
                .melee(9.0, 1.0)
                .engrave("crushing", "elemental_boost", "sturdy"));

        // ---------------------------------------------------------------- Overprotective Halberd
        out.accept(RelicDef.of("overprotective_halberd", "Overprotective Halberd", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0x9CA2B0, 0xC82830, 0xDDE8FF)
                .desc("A Vasteel halberd trailing red ribbons. Use sends it hovering round you for three minutes: it strikes back at whoever hits you in melee, "
                        + "once every two seconds, even when it is not in your hand. Use again to call it back.")
                .passive(Passive.flag("w20_overprotective"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 0.75, Operation.ADD_VALUE))
                .act(Act.of("hover", "Hover", Wave20Acts::hover).mana(4).cooldown(40))
                .melee(9.5, 1.0)
                .engrave("sturdy", "magic_weapon", "spiritual_protection"));

        // ---------------------------------------------------------------- Phoenix Flame
        out.accept(RelicDef.of("phoenix_flame", "Phoenix Flame", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0xC85A2C, 0x8C46A0, 0xFFA030)
                .desc("A halberd whose blade is a swept-wing phoenix. Use speaks the command word: flames erupt, light the dark around you and add fire damage to every hit. "
                        + "They burn until you speak again, drop the halberd or stow it.")
                .passive(Passive.flag("w20_phoenix"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_fire", 0.3))
                .act(Act.of("command_word", "Command Word", Wave20Acts::commandWord).cooldown(20))
                .melee(8.5, 1.0)
                .engrave("elemental_boost", "severance", "vigor"));

        // ---------------------------------------------------------------- Primal Chaos
        out.accept(RelicDef.of("primal_chaos", "Primal Chaos", Grade.RARE, RelicSlot.MAIN_HAND)
                .model3d().look(Look.RAPIER).palette(0x78AAFF, 0xFFFFFF, 0x8CC0FF)
                .desc("A rapier of light forged by the gods to destroy the creatures of the void, though it is pointy and works on others too. It adds radiant damage, "
                        + "far more against void creatures, and Void Rend lunges through a line of foes.")
                .passive(Passive.flag("w20_chaos"))
                .act(Act.of("void_rend", "Void Rend", Wave20Acts::voidRend).cooldown(160))
                .melee(5.0, 2.0)
                .engrave("holy_weapon", "barrier_piercing", "swift"));

        // ---------------------------------------------------------------- Raven's Wing
        out.accept(RelicDef.of("ravens_wing", "Raven's Wing", Grade.RARE, RelicSlot.MAIN_HAND)
                .attune().model3d().look(Look.STAFF).palette(0x58648C, 0x14141A, 0x9CC0FF)
                .desc("A +2 glaive with a raven's wing for a blade. Use tolls the dead at a creature in sight, sneak + use spares a dying friend, and the worn-relic key "
                        + "sends ravens to scout (three a day).")
                .passive(Passive.flag("w20_raven"))
                .passive(Passive.attr("minecraft:player.entity_interaction_range", 1.0, Operation.ADD_VALUE))
                .act(Act.of("toll_the_dead", "Toll the Dead", Wave20Acts::tollTheDead).cooldown(2400))
                .act(Act.of("spare_the_dying", "Spare the Dying", Wave20Acts::spareTheDying).needsSneak().cooldown(2400))
                .act(Act.of("raven_sense", "Raven Sense", Wave20Acts::ravenSense).usesPerDay(3).cooldown(200))
                .melee(8.0, 1.1)
                .engrave("soul_eater", "enervation", "severance"));
    }

    /** Registers the damage, death and tick rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        Wave20Events.register();
    }
}
