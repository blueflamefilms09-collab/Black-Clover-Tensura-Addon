package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

import java.util.function.Consumer;

/**
 * Wave 0 of the Relics of Many Worlds: the sixteen items the mod already had (Quickling's Ivy, the Magebane Longsword and Shield, the Bolt
 * Thrower, the Brooch Pin, the Club of Rot, the Crossbow of Swift Death, the Doom Hammer, the Weapon of Resurgence, the Hand of Providence,
 * the Devil Contract, the Prism Blade, the Permafrost Sabre, the Cyclone Rapier, the Bloodbound Katana and Thundercleave) remade onto the relic
 * system. Every def {@code adopt}s its item, so no item is registered: the ids, the art, the 3D models, the engravings and the old right-click
 * techniques all stay. What the card says and the item lacked is added here (a grade, attunement, passives, acts, hooks); where the card
 * supersedes the old rule, the new rule is the one written in the hooks.
 * <p>
 * Tabletop becomes Minecraft in {@link Wave0Rules}: a saving throw is a roll against the target's toughness, a long rest is a Minecraft day,
 * a short rest is a cooldown, a round is a second.
 */
public final class Wave0 {
    private Wave0() {}

    // the switches the hooks read (RelicFlags.has: true while the relic is carried and, if it needs it, attuned)
    public static final String F_IVY = "w0_ivy", F_MBS = "w0_magebane_sword", F_MBSH = "w0_magebane_shield", F_BOLT = "w0_bolt_thrower",
            F_SWIFT = "w0_swift_death", F_PRISM = "w0_prism", F_SABRE = "w0_sabre", F_RAPIER = "w0_rapier";

    private static final String ATK = "minecraft:generic.attack_damage", SPEED = "minecraft:generic.attack_speed", ARMOR = "minecraft:generic.armor";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- weapons
        out.accept(RelicDef.of("quicklings_ivy", "Quickling's Ivy", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.QUICKLINGS_IVY).attune()
                .palette(0x3FD35A, 0xE8A93A, 0x9CFF7A).look(Look.RAPIER)
                .desc("A +1 finesse rapier whose every hit bites for 1d4 more poison. Sprout Ivy readies the next hit to restrain its target (a Strength save each second); a critical hit sprouts it free.")
                .engrave("swift", "magic_weapon", "enervation")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr(SPEED, 0.2, Operation.ADD_VALUE))
                .passive(Passive.flag(F_IVY))
                .act(Act.of("sprout_ivy", "Sprout Ivy", Wave0Acts::sproutIvy).cooldown(300).usesPerDay(6)));

        out.accept(RelicDef.of("magebane_longsword", "Magebane Longsword", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.MAGEBANE_LONGSWORD).attune()
                .palette(0x2FD6C8, 0x2B2F36, 0x9BFFF3).look(Look.SWORD)
                .desc("A +2 blade that breaks the concentration of the casters it cuts, and strikes at any caster who begins a spell next to you. With the Magebane Shield it can Dispel Magic once a day. Lends Magic Sense.")
                .engrave("magic_interference", "barrier_piercing", "magicule_absorption")
                .passive(Passive.attr(ATK, 2.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_MBS))
                .passive(Passive.skill("tensura:magic_sense"))
                .act(Act.of("dispel_magic", "Dispel Magic", Wave0Acts::dispelMagic).cooldown(40).mana(4)));

        out.accept(RelicDef.of("magebane_shield", "Magebane Shield", Grade.UNIQUE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.MAGEBANE_SHIELD).attune()
                .palette(0x2FD6C8, 0xC8B070, 0xB8FFF4).look(Look.SHIELD)
                .desc("+1 armour while held; spells and afflictions from casters within five blocks are shrugged off more often; the first elemental blow of the day is absorbed and returned. With the Magebane Longsword it can cover you from a blast. Lends Magic Resistance.")
                .engrave("magic_interference", "magicule_absorption", "magic_protection", "sturdy")
                .passive(Passive.attr(ARMOR, 1.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_MBSH))
                .passive(Passive.skill("tensura:magic_resistance")));

        out.accept(RelicDef.of("bolt_thrower", "Bolt Thrower", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.BOLT_THROWER).attune()
                .palette(0x2A2D3A, 0x7FE6FF, 0xBFF4FF).look(Look.BOW)
                .desc("A crossbow of force: +2 on every bolt (and on a blow of the frame), 1d8 more against constructs, and it never needs loading. Lends Farsight.")
                .engrave("barrier_piercing", "severance", "elemental_boost", "magic_weapon")
                .passive(Passive.attr(ATK, 2.0, Operation.ADD_VALUE))
                .passive(Passive.flag(F_BOLT))
                .passive(Passive.skill("tensura:farsight")));

        out.accept(RelicDef.of("brooch_pin", "Brooch Pin", Grade.SPECIAL, RelicSlot.MAIN_HAND)
                .adopt(NUItems.BROOCH_PIN)
                .palette(0xFFD66E, 0xFF3358, 0xFF7A8C).look(Look.CHARM)
                .desc("A dagger that folds into a ruby brooch: drawn in the hand it is a +1 dagger; worn in a Curios slot it passes for jewellery, and creatures see you a quarter less well.")
                .engrave("severance", "barrier_piercing")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE)));

        out.accept(RelicDef.of("club_of_rot", "Club of Rot", Grade.SPECIAL, RelicSlot.MAIN_HAND)
                .adopt(NUItems.CLUB_OF_ROT)
                .palette(0xE8E0C0, 0x7BD148, 0xA6E05A).look(Look.HAMMER)
                .desc("A critical hit adds 2d6 necrotic damage (not to the dead or to constructs) and sickens the target unless it saves against Constitution each second. Every hit still rots. Lends Poison Resistance.")
                .engrave("enervation", "soul_eater", "crushing", "energy_steal")
                .passive(Passive.skill("tensura:poison_resistance")));

        out.accept(RelicDef.of("crossbow_of_swift_death", "Crossbow of Swift Death", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.CROSSBOW_OF_SWIFT_DEATH).attune()
                .palette(0x2B211C, 0xD0183A, 0xFF7A8C).look(Look.BOW)
                .desc("A bolt that finds a surprised creature (asleep, struck from behind, or hunting someone else) lands as a critical hit and may terrify it. A frame found in the world carries 3d6 notches. Lends Coercion.")
                .engrave("severance", "barrier_piercing", "enervation", "soul_eater")
                .passive(Passive.flag(F_SWIFT))
                .passive(Passive.skill("tensura:coercion")));

        out.accept(RelicDef.of("doom_hammer", "Doom Hammer", Grade.SPECIAL, RelicSlot.MAIN_HAND)
                .adopt(NUItems.DOOM_HAMMER)
                .palette(0x1A1C22, 0xFF6A30, 0xFF9A50).look(Look.HAMMER)
                .desc("A blow that leaves a living creature under a third of its health marks it doomed (it mends half as much); under a quarter, the hammer adds 5d6 necrotic damage on top.")
                .engrave("crushing", "severance", "sturdy", "barrier_piercing"));

        out.accept(RelicDef.of("weapon_of_resurgence", "Weapon of Resurgence", Grade.SPECIAL, RelicSlot.MAIN_HAND)
                .adopt(NUItems.WEAPON_OF_RESURGENCE).attune()
                .palette(0x36B5A0, 0xFFD86B, 0xFFF2B0).look(Look.SWORD)
                .desc("A +1 paladin's blade: every hit refills a point of its Lay on Hands pool, and Lay on Hands spends the pool to mend an ally or cleanse an affliction. Lends Self Regeneration.")
                .engrave("holy_weapon", "restoration", "vitality", "sturdy")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:self_regeneration"))
                .act(Act.of("lay_on_hands", "Lay on Hands", Wave0Acts::layOnHands).cooldown(60)));

        out.accept(RelicDef.of("hand_of_providence", "The Hand of Providence", Grade.UNIQUE, RelicSlot.HANDS)
                .adopt(NUItems.HAND_OF_PROVIDENCE).attune()
                .palette(0xF0A830, 0x4FA3FF, 0xCFEAFF).look(Look.GAUNTLETS)
                .desc("A gauntlet with a blue cross on the back. Its Mage Hand fetches dropped items and experience from 12 blocks, and lays healing hands on an ally up to 18 blocks away. Lends Holy Resistance.")
                .engrave("holy_weapon", "restoration", "magic_protection", "sturdy", "crushing")
                .passive(Passive.skill("tensura:holy_attack_resistance"))
                .act(Act.of("mage_hand", "Mage Hand", Wave0Acts::mageHand).cooldown(80))
                .act(Act.of("mage_hand_lay", "Mage Hand: Lay on Hands", Wave0Acts::mageHandLay).cooldown(240).mana(3)));

        out.accept(RelicDef.of("devil_contract", "Infernal Contract", Grade.LEGEND, RelicSlot.CONSUMABLE)
                .adopt(NUItems.DEVIL_CONTRACT).vessel()
                .palette(0xC9A872, 0xB01820, 0xFF3030).look(Look.TOME)
                .desc("Signed in blood, forever binding, one at a time: the old bargain with a devil, or a Contract of Blood, Riches, Knowledge or Power. Whoever signs is Damned: death takes the soul into a crystal.")
                .act(Act.of("sign_blood", "Sign: Contract of Blood", c -> Wave0Acts.sign(c, Wave0Contract.Kind.BLOOD)))
                .act(Act.of("sign_riches", "Sign: Contract of Riches", c -> Wave0Acts.sign(c, Wave0Contract.Kind.RICHES)))
                .act(Act.of("sign_knowledge", "Sign: Contract of Knowledge", c -> Wave0Acts.sign(c, Wave0Contract.Kind.KNOWLEDGE)))
                .act(Act.of("sign_power", "Sign: Contract of Power", c -> Wave0Acts.sign(c, Wave0Contract.Kind.POWER))));

        out.accept(RelicDef.of("prism_blade", "Prism Blade", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.PRISM_BLADE).attune()
                .palette(0x9AE8F8, 0xE0C060, 0xFFFFFF).look(Look.SWORD)
                .desc("Every hit adds 1d8 radiant damage. Blinding Radiance blinds all within 9 blocks (once a day); Chromatic Strike readies a hit for 4d8 more and a fear or charm. Lends Light Resistance.")
                .engrave("holy_weapon", "elemental_boost", "barrier_piercing")
                .passive(Passive.flag(F_PRISM))
                .passive(Passive.skill("tensura:light_attack_resistance"))
                .act(Act.of("blinding_radiance", "Blinding Radiance", Wave0Acts::blindingRadiance).cooldown(100).usesPerDay(1).mana(3))
                .act(Act.of("chromatic_strike", "Chromatic Strike", Wave0Acts::chromaticStrike).cooldown(600).usesPerDay(3)));

        out.accept(RelicDef.of("permafrost_sabre", "Permafrost Sabre", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.PERMAFROST_SABRE).attune()
                .palette(0x4FD0B0, 0xEFE4C0, 0xAEEBFF).look(Look.SWORD)
                .desc("A +1 sabre that halves cold damage while held and adds 1d6 cold to every hit. With the Cyclone Rapier in the other hand: 2d6, and the Boreal Fusion whirlwind. Lends Cold Resistance.")
                .engrave("elemental_boost", "swift", "sturdy")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.resist("minecraft:is_freezing", 0.5))
                .passive(Passive.resist("tensura:ice_elemental", 0.5))
                .passive(Passive.flag(F_SABRE))
                .passive(Passive.skill("tensura:cold_resistance"))
                .act(Act.of("boreal_fusion", "Boreal Fusion", Wave0Acts::borealFusion).cooldown(60).mana(5)));

        out.accept(RelicDef.of("cyclone_rapier", "Cyclone Rapier", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.CYCLONE_RAPIER).attune()
                .palette(0x2A2F6A, 0xD8B060, 0xB890FF).look(Look.RAPIER)
                .desc("A +1 light rapier, quick in the hand, that adds 1d6 force to every hit. With the Permafrost Sabre in the other hand: 2d6, and the Boreal Fusion whirlwind.")
                .engrave("swift", "elemental_boost", "magic_weapon")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr(SPEED, 0.3, Operation.ADD_VALUE))
                .passive(Passive.flag(F_RAPIER))
                .act(Act.of("boreal_fusion", "Boreal Fusion", Wave0Acts::borealFusion).cooldown(60).mana(5)));

        out.accept(RelicDef.of("bloodbound_katana", "Bloodbound Katana", Grade.LEGEND, RelicSlot.MAIN_HAND)
                .adopt(NUItems.BLOODBOUND_KATANA).vessel()
                .palette(0x14161A, 0xD0183A, 0xFF3A55).look(Look.SWORD)
                .desc("Hits on the living store Blood Charges (up to 10) that bite on every blow; the dead and constructs give none. Blood Sip trades your blood for charges; Bloodflare spends three for 1d8 more.")
                .engrave("energy_steal", "severance", "soul_eater")
                .act(Act.of("blood_sip", "Blood Sip", Wave0Acts::bloodSip).cooldown(10).toggle())
                .act(Act.of("bloodflare", "Bloodflare", Wave0Acts::bloodflare).cooldown(10).toggle()));

        out.accept(RelicDef.of("thundercleave", "Thundercleave", Grade.RARE, RelicSlot.MAIN_HAND)
                .adopt(NUItems.THUNDERCLEAVE).attune()
                .palette(0xC0C8D0, 0xB0A040, 0x7FE6FF).look(Look.SWORD)
                .desc("A +1 greatsword with two charges: Thunder Sweep spends one to cut a wide arc that strikes three creatures for 2d6 thunder, hurls them back and deafens them. Dawn, or a thunderstorm overhead, restores the charges. Lends Electricity Resistance.")
                .engrave("elemental_boost", "crushing", "severance")
                .passive(Passive.attr(ATK, 1.0, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:electricity_resistance"))
                .act(Act.of("thunder_sweep", "Thunder Sweep", Wave0Acts::thunderSweep).cooldown(40)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() { Wave0Hooks.register(); }
}
