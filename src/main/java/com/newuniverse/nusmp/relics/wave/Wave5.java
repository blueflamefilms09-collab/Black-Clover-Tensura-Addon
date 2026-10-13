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
 * Relics of Many Worlds, wave 5: seven necklaces and amulets.
 * <ul>
 *   <li>Amulet of the Warmage (RARE): turns raw magic aside, deepens the magicule well, and holds four cantrip charges.</li>
 *   <li>Hemlock's Amulet (LEGEND): the Mask of the Living hides you from the dead; it grows Awakened, then Exalted (the Reliquary).</li>
 *   <li>Amulet of Safeguarding (UNIQUE): a full-moon record of the wearer, and a rescue from one killing blow.</li>
 *   <li>Amulet of the Huntress (RARE): a predator's stealth and a bonus with cutting weapons, doubled for claws and teeth.</li>
 *   <li>The Amulet of Lost Souls (LEGEND): eight souls, ten charges, three spells each; the last charge may free a soul.</li>
 *   <li>Lovelorn Locket (SPECIAL): fair dealing with villagers and a bed for the night from an old soldier.</li>
 *   <li>Amulet of Dragon Rage (LEGEND): two minutes as an Adult Silver Dragon, once a day.</li>
 * </ul>
 * All are Curios necklaces that need attunement. The long card texts are in the lang fragment; the tabletop-to-game translation is in doc_wave5.md.
 */
public final class Wave5 {
    private Wave5() {}

    public static void defs(Consumer<RelicDef> out) {
        out.accept(warmage());
        out.accept(hemlock());
        out.accept(safeguarding());
        out.accept(huntress());
        out.accept(lostSouls());
        out.accept(locket());
        out.accept(dragonRage());
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor, through the catalog). */
    public static void hooks() { Wave5Hooks.register(); }

    // ---------------------------------------------------------------- the relics
    private static RelicDef warmage() {
        return RelicDef.of("amulet_of_the_warmage", "Amulet of the Warmage", Grade.RARE, RelicSlot.NECK)
                .attune().look(Look.PENDANT).palette(0x4A4D5C, 0xC4C8D6, 0x8A7DFF)
                .desc("A war-mage's amulet: it turns raw magic aside and deepens your magicule well. It holds four cantrip charges (Fire Bolt, Ray of Frost, Shocking Grasp, Acid Splash); 1d4 come back each dawn.")
                .passive(Passive.resist("minecraft:magic", 0.5))
                .passive(Passive.resist("minecraft:indirect_magic", 0.5))
                .passive(Passive.resist("minecraft:sonic_boom", 0.5))
                .passive(Passive.attr("tensura:max_magicule", 0.08, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.skill(Wave5Skills.MAGIC_SENSE))
                .passive(Passive.flag(Wave5Warmage.FLAG))
                .act(Act.of("fire_bolt", "Fire Bolt", c -> Wave5Warmage.cast(c, Wave5Warmage.FIRE_BOLT)).cooldown(20))
                .act(Act.of("ray_of_frost", "Ray of Frost", c -> Wave5Warmage.cast(c, Wave5Warmage.RAY_OF_FROST)).cooldown(20))
                .act(Act.of("shocking_grasp", "Shocking Grasp", c -> Wave5Warmage.cast(c, Wave5Warmage.SHOCKING_GRASP)).cooldown(20))
                .act(Act.of("acid_splash", "Acid Splash", c -> Wave5Warmage.cast(c, Wave5Warmage.ACID_SPLASH)).cooldown(20));
    }

    private static RelicDef hemlock() {
        return RelicDef.of("hemlocks_amulet", "Hemlock's Amulet", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.PENDANT).palette(0x3E8F96, 0x7B2D8E, 0xB44FD8)
                .desc("It hides your true form behind a living man's face: the dead cannot find you. It drinks the dead's energy and grows Awakened, then Exalted: the Reliquary of your soul.")
                .passive(Passive.resist("minecraft:wither", 0.5))
                .passive(Passive.flag(Wave5Hemlock.FLAG))
                .act(Act.of("wear_mask", "Wear the Mask of the Living", Wave5Hemlock::don).cooldown(200).mana(3))
                .act(Act.of("drop_mask", "Let the Mask Fall", Wave5Hemlock::drop).cooldown(20));
    }

    private static RelicDef safeguarding() {
        return RelicDef.of("amulet_of_safeguarding", "Amulet of Safeguarding", Grade.UNIQUE, RelicSlot.NECK)
                .attune().look(Look.AMULET).palette(0xA9ACB4, 0x6B6E76, 0xFF7FCB)
                .desc("Under the full moon, sit still for thirty seconds and the amulet records you. If a killing blow lands, it breaks and puts you back as you were.")
                .passive(Passive.flag(Wave5Safeguard.FLAG))
                .act(Act.of("meditate", "Meditate on the Record", Wave5Safeguard::meditate).cooldown(200));
    }

    private static RelicDef huntress() {
        return RelicDef.of("amulet_of_the_huntress", "Amulet of the Huntress", Grade.RARE, RelicSlot.NECK)
                .attune().look(Look.AMULET).palette(0xD9A62E, 0x8A6410, 0xF0F0A8)
                .desc("A predator's stealth and a hunter's edge: you creep faster and are not noticed beyond eight blocks while sneaking. +1 damage with blades, axes, tridents and arrows, doubled for claws and teeth.")
                .passive(Passive.flag(Wave5Huntress.FLAG));
    }

    private static RelicDef lostSouls() {
        return RelicDef.of("the_amulet_of_lost_souls", "The Amulet of Lost Souls", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.PENDANT).palette(0xA67C3E, 0x4B2A70, 0xB05CFF)
                .desc("Eight souls are bound in it, each with three spells. Call on one at a time; ten charges, 1d10 back each dawn. Spend the last charge and the soul in use may break free for good.")
                .passive(Passive.flag(Wave5Souls.FLAG))
                .act(Act.of("soul_spell_1", "Soul Spell I (1 charge)", c -> Wave5Souls.cast(c, 1)).cooldown(20))
                .act(Act.of("soul_spell_2", "Soul Spell II (2 charges)", c -> Wave5Souls.cast(c, 2)).cooldown(40))
                .act(Act.of("soul_spell_3", "Soul Spell III (3 charges)", c -> Wave5Souls.cast(c, 3)).cooldown(100))
                .act(Act.of("commune", "Commune with the Souls", Wave5Souls::commune).cooldown(100));
    }

    private static RelicDef locket() {
        return RelicDef.of("lovelorn_locket", "Lovelorn Locket", Grade.SPECIAL, RelicSlot.NECK)
                .attune().look(Look.LOCKET).palette(0xB08A4A, 0x8A8F99, 0xE02A3A)
                .desc("Villagers deal fairly with its wearer. Once a day, beside an old soldier (a weaponsmith, armourer, toolsmith or iron golem), you are billeted for the night: fed, healed, and your return spot is set.")
                .passive(Passive.effect("minecraft:hero_of_the_village", 0))
                .act(Act.of("billet", "Billet for the Night", Wave5Locket::billet).cooldown(40));
    }

    private static RelicDef dragonRage() {
        return RelicDef.of("amulet_of_dragon_rage", "Amulet Of Dragon Rage", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.AMULET).palette(0x2B2630, 0x9A1F2B, 0x39B6FF)
                .desc("Speak the command word and become an Adult Silver Dragon for two minutes, once a day: wings, scales, a bite, a pool of dragon hit points and a breath of frost. Break the pool and the form breaks.")
                .passive(Passive.resist("minecraft:is_freezing", 0.3))
                .passive(Passive.flag(Wave5Dragon.FLAG))
                .act(Act.of("command_word", "Speak the Command Word", Wave5Dragon::transform).cooldown(24000).epPercent(3))
                .act(Act.of("frost_breath", "Silver Frost Breath", Wave5Dragon::breath).cooldown(160))
                .act(Act.of("back_to_flesh", "Speak the Word Again", Wave5Dragon::speak).cooldown(20));
    }
}
