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
 * Why: Relics of Many Worlds, part 4, wave 23: twelve relics. Three longbows (Divine Mercy, Embrasure, The Dragon's Spirit, The Reaver) and a lute that is
 * also a crossbow (Warlute), four shields (Aegis of the Abyss, Celestial Support, Hero's Shield, Thunder Shield), and three charms (Bewitching Lyre,
 * Eye of the Deep One, Horn of Leroy). Every hand-held one is a 3D model in hand; the bows and the Warlute are real bows and crossbows with the
 * draw-state meshes (&lt;id&gt;_pull1..3, &lt;id&gt;_charged).
 * <p>
 * How the abilities are wired: a bow's use button means "draw", so its abilities go on sneak + use (the second act when it has several) and on the
 * worn-relic key; a shield with no act of its own blocks like a vanilla shield while the button is held. The act bodies are in {@link Wave23Acts}, the rules
 * that need the game bus in {@link Wave23Events}, the numbers in the pure {@link Wave23Rules}. Tabletop becomes Minecraft on the earlier waves' scale:
 * 5 ft = 1 block, a round = 6 s, a short rest = a few minutes, a long rest = a Minecraft day, armour class = armour points, advantage = Luck and Tensura's
 * Inspiration.
 */
public final class Wave23 {
    private Wave23() {}

    public static final String F_MERCY = "w23_divine_mercy", F_REAVER = "w23_reaver", F_WARLUTE = "w23_warlute", F_AEGIS = "w23_aegis",
            F_CELESTIAL = "w23_celestial", F_HEROS = "w23_heros", F_THUNDER = "w23_thunder", F_EYE = "w23_eye";

    private static final String ARMOR = "minecraft:generic.armor", TOUGH = "minecraft:generic.armor_toughness", KB = "minecraft:generic.knockback_resistance";

    public static void defs(Consumer<RelicDef> out) {
        // ---------------------------------------------------------------- Divine Mercy (legendary longbow, attunement)
        out.accept(RelicDef.of("divine_mercy", "Divine Mercy", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().vessel().model3d().bow()
                .look(Look.BOW).palette(0xD89A3C, 0xC0392B, 0xFFE9A0).melee(5.0, 1.3)
                .engrave("holy_weapon", "magic_weapon", "swift")
                .desc("A longbow whose limbs are the golden wings of an angel. Every arrow lands 3 harder and burns with radiant light, twice as much on a fiend or an undead. "
                        + "Once a day, sneak and use to pass the Sentence on the next arrow: a creature of 100 health or less takes a crushing blow unless it saves.")
                .passive(Passive.skill("tensura:holy_attack_resistance"))
                .passive(Passive.flag(F_MERCY))
                .act(Act.of("divine_sentence", "Divine Sentence", Wave23Acts::divineSentence).usesPerDay(1).cooldown(Wave23Rules.SENTENCE_COOLDOWN).mana(3)));

        // ---------------------------------------------------------------- Embrasure (uncommon longbow)
        out.accept(RelicDef.of("embrasure", "Embrasure", Grade.SPECIAL, RelicSlot.MAIN_HAND).model3d().bow()
                .look(Look.BOW).palette(0x7FAE55, 0xD94A63, 0xFF8AA0).melee(3.0, 1.2)
                .engrave("growth", "sturdy", "vitality")
                .desc("A longbow of living vine with a red crystal grip. Sneak and use to say \"Protect me!\": for one round (6 seconds) a magical shield "
                        + "wraps the bow and you. Twice, then it needs a short rest of four minutes.")
                .passive(Passive.flag("w23_embrasure"))
                .act(Act.of("protect_me", "Protect me!", Wave23Acts::protectMe).cooldown(40)));

        // ---------------------------------------------------------------- The Dragon's Spirit (rare longbow, attunement)
        out.accept(RelicDef.of("the_dragons_spirit", "The Dragon's Spirit", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d().bow()
                .look(Look.BOW).palette(0x3C3438, 0x3FB8B0, 0x6FFFF0).melee(4.0, 1.3)
                .engrave("barrier_piercing", "swift", "magic_weapon")
                .desc("A slim recurve of dark wood with dragon-claw crystal tips. Its spirit gives you foresight (area and missile harm is softer). "
                        + "Sneak and use for Dragon's Claw (three half-strength arrows at three foes, twice a day); the worn-relic key calls Dragon's Sight "
                        + "(every few minutes) and Dragonstrike (once a day).")
                .passive(Passive.skill("tensura:danger_sense"))
                .passive(Passive.resist("minecraft:is_explosion", Wave23Rules.DRAGON_HAZARD_RESIST))
                .passive(Passive.resist("minecraft:is_projectile", Wave23Rules.DRAGON_HAZARD_RESIST))
                .passive(Passive.flag("w23_dragons_spirit"))
                .act(Act.of("dragons_sight", "Dragon's Sight", Wave23Acts::dragonsSight).cooldown(Wave23Rules.SIGHT_COOLDOWN))
                .act(Act.of("dragons_claw", "Dragon's Claw", Wave23Acts::dragonsClaw).usesPerDay(2).cooldown(60).mana(2))
                .act(Act.of("dragonstrike", "Dragonstrike", Wave23Acts::dragonstrike).usesPerDay(1).cooldown(100).mana(4)));

        // ---------------------------------------------------------------- The Reaver (very rare longbow, attunement)
        out.accept(RelicDef.of("the_reaver", "The Reaver", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d().bow()
                .look(Look.BOW).palette(0x16141A, 0x8E1C24, 0xFF3A2A).melee(4.0, 1.3)
                .engrave("soul_eater", "enervation", "barrier_piercing")
                .desc("A black thorned longbow forged by a demon lord. Every arrow lands 2 harder. Twice a day, sneak and use to loose a Barbed Arrow on a demonic chain: "
                        + "it bites deep and drags a Large or smaller foe onto the bow's spike.")
                .passive(Passive.flag(F_REAVER))
                .act(Act.of("barbed_arrow", "Barbed Arrow", Wave23Acts::barbedArrow).usesPerDay(Wave23Rules.BARB_USES).cooldown(Wave23Rules.BARB_COOLDOWN).mana(2)));

        // ---------------------------------------------------------------- Warlute (rare lute / crossbow, attunement)
        out.accept(RelicDef.of("warlute", "Warlute", Grade.RARE, RelicSlot.MAIN_HAND).attune().model3d().crossbow()
                .look(Look.BOW).palette(0x8A1C26, 0xC4C8D4, 0xFFD890).melee(5.0, 1.6)
                .engrave("severance", "swift", "magic_weapon")
                .desc("A crimson lute with a blade for a tail and a crossbow at the neck. Load and shoot it as a crossbow, or cut with the blade. "
                        + "Sneak and use to strike the Chord of Valor on an ally: Luck, Strength and Speed for a minute, stronger if you struck with the lute in the last round.")
                .passive(Passive.skill("tensura:sound_manipulation"))
                .passive(Passive.flag(F_WARLUTE))
                .act(Act.of("chord_of_valor", "Chord of Valor", Wave23Acts::chordOfValor).usesPerDay(Wave23Rules.CHORD_USES).cooldown(Wave23Rules.CHORD_COOLDOWN).mana(1)));

        // ---------------------------------------------------------------- Aegis of the Abyss (legendary shield, attunement)
        out.accept(RelicDef.of("aegis_of_the_abyss", "Aegis of the Abyss", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().vessel().model3d()
                .look(Look.SHIELD).palette(0x2A2A32, 0xC0C4D0, 0xA050FF).melee(4.0, 1.2)
                .engrave("magic_interference", "magicule_absorption", "magic_protection", "sturdy")
                .desc("A black shield carved as a snarling demon whose mouth holds a galaxy. +3 armour; spells hit you 25% softer and Magic Resistance is lent. "
                        + "Arcane Hunger swallows a spell by itself, twice a day, for yourself or an ally within two blocks, and feeds you absorption; one of more than 90 damage cracks it for the day.")
                .passive(Passive.attr(ARMOR, Wave23Rules.AEGIS_ARMOR, Operation.ADD_VALUE))
                .passive(Passive.attr(TOUGH, Wave23Rules.AEGIS_TOUGHNESS, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:magic_resistance"))
                .passive(Passive.flag(F_AEGIS)));

        // ---------------------------------------------------------------- Celestial Support (very rare shield, attunement)
        out.accept(RelicDef.of("celestial_support", "Celestial Support", Grade.UNIQUE, RelicSlot.MAIN_HAND).attune().model3d()
                .look(Look.SHIELD).palette(0x8A92A0, 0x3A78C8, 0xFFE070).melee(4.0, 1.2)
                .engrave("holy_coat", "sturdy", "restoration")
                .desc("A winged kite shield of speckled stone with a blue rim. +2 armour. When a heavy melee blow lands (a third of your health) you strike back at once, "
                        + "and a blow that would drop you to zero leaves you standing instead, once every five minutes.")
                .passive(Passive.attr(ARMOR, Wave23Rules.CELESTIAL_ARMOR, Operation.ADD_VALUE))
                .passive(Passive.flag(F_CELESTIAL)));

        // ---------------------------------------------------------------- Hero's Shield (legendary shield, attunement, lawful)
        out.accept(RelicDef.of("heros_shield", "Hero's Shield", Grade.LEGEND, RelicSlot.MAIN_HAND).attune().vessel().model3d()
                .look(Look.SHIELD).palette(0xB8E0EC, 0xD8DCE4, 0x6FD8FF).melee(8.0, 1.2)
                .engrave("holy_coat", "sturdy", "energy_protection", "magic_protection")
                .desc("An ice-blue kite shield with a silver maze and a glowing gem. +3 armour, steadier on your feet, and nonmagical weapons strike you 25% softer. "
                        + "Use it to hurl it at a foe (6 blocks, 18 at a disadvantage); it bashes hard in melee.")
                .passive(Passive.attr(ARMOR, Wave23Rules.HERO_ARMOR, Operation.ADD_VALUE))
                .passive(Passive.attr(TOUGH, Wave23Rules.HERO_TOUGHNESS, Operation.ADD_VALUE))
                .passive(Passive.attr(KB, Wave23Rules.HERO_KNOCKBACK, Operation.ADD_VALUE))
                .passive(Passive.flag(F_HEROS))
                .act(Act.of("shield_throw", "Shield Throw", Wave23Acts::shieldThrow).cooldown(Wave23Rules.THROW_COOLDOWN)));

        // ---------------------------------------------------------------- Thunder Shield (no rarity printed)
        out.accept(RelicDef.of("thunder_shield", "Thunder Shield", Grade.RARE, RelicSlot.MAIN_HAND).model3d()
                .look(Look.SHIELD).palette(0x2C3A52, 0x5A7AA8, 0x7FD0FF).melee(4.5, 1.3)
                .engrave("elemental_resistance", "sturdy", "energy_protection")
                .desc("A round steel-blue shield veined with live lightning. +3 armour. While it is in your hand, lightning cannot hurt you or your allies within twelve blocks.")
                .passive(Passive.attr(ARMOR, Wave23Rules.THUNDER_ARMOR, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:electricity_resistance"))
                .passive(Passive.flag(F_THUNDER)));

        // ---------------------------------------------------------------- Bewitching Lyre (rare charm, attunement by a bard)
        out.accept(RelicDef.of("bewitching_lyre", "Bewitching Lyre", Grade.RARE, RelicSlot.CHARM).attune()
                .look(Look.CHARM).palette(0xD03C60, 0xC8CCD8, 0xFF7AA0)
                .desc("A serpentine lyre of glossy pink wood with eight charges. Use to sing an Enchantment: foes within ten blocks save or lose the will to fight; sneak to spend a charge and no one saves. "
                        + "Perform for a willing audience of twelve (the second act) to win every charge back.")
                .passive(Passive.flag("w23_lyre"))
                .act(Act.of("bewitch", "Bewitching Song", Wave23Acts::bewitch).cooldown(Wave23Rules.LYRE_COOLDOWN).mana(2))
                .act(Act.of("perform", "Performance", Wave23Acts::perform).hold(Wave23Rules.PERFORM_HOLD).cooldown(Wave23Rules.PERFORM_COOLDOWN)));

        // ---------------------------------------------------------------- Eye of the Deep One (rare ring, attunement by a warlock)
        out.accept(RelicDef.of("eye_of_the_deep_one", "Eye of the Deep One", Grade.RARE, RelicSlot.CHARM).attune()
                .look(Look.CHARM).palette(0x1E4A36, 0x2F7BE0, 0x6FB8FF)
                .desc("A dark green ring of tentacles round a glowing blue eye. You see in the dark and are hard to shove or seize. After each long rest it rolls a boon for you (a d4). "
                        + "Use for an Eldritch Blast; the second act closes tentacles on a foe in reach.")
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.attr(KB, Wave23Rules.EYE_KNOCKBACK, Operation.ADD_VALUE))
                .passive(Passive.skill("tensura:darkness_attack_resistance"))
                .passive(Passive.flag(F_EYE))
                .act(Act.of("eldritch_blast", "Eldritch Blast", Wave23Acts::eldritchBlast).cooldown(Wave23Rules.BLAST_COOLDOWN).mana(1))
                .act(Act.of("grapple", "Tentacle Grasp", Wave23Acts::grapple).cooldown(Wave23Rules.GRAPPLE_COOLDOWN)));

        // ---------------------------------------------------------------- Horn of Leroy (legendary horn)
        out.accept(RelicDef.of("horn_of_leroy", "Horn of Leroy", Grade.LEGEND, RelicSlot.CHARM).vessel()
                .look(Look.CHARM).palette(0x16141A, 0xD94A63, 0xFFE9A0)
                .desc("A black hunting horn with a heart cut into the bell. Blow it and every ally within a hundred blocks charges with you for five minutes. "
                        + "Fell eight hostile creatures before the time is up and your luck grows for good; fail and you are handed a bucket of chicken. The horn crumbles and is whole again after five days.")
                .passive(Passive.flag("w23_horn"))
                .act(Act.of("alright_chums", "Alright Chums, Let's Do This", Wave23Acts::blowHorn).cooldown(Wave23Rules.HORN_COOLDOWN)));
    }

    /** Registers the damage, death, wake and tick rules of this wave on the game bus (called once from the mod constructor). */
    public static void hooks() {
        Wave23Events.register();
    }
}
