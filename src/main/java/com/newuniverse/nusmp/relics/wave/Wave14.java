package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Breath;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 14: nine relics for the head (three dragon circlets, a duck crown, two unicorn crowns, the Phoenix circlet, the
 * Conjurer's crown, a diadem) and eight for the feet (planestriding boots, cursed boots, mud boots, high-ground boots, wind greaves, water boots,
 * sandals, dragon fire boots). The long card text of each, as game rules, is in the lang keys {@code item.nusmp.<id>.card}; the numbers and small
 * state machines are in {@link Wave14Rules} (pure), the abilities in {@link Wave14Head} and {@link Wave14Feet}, what stays in the world in
 * {@link Wave14Zones}, the handlers in {@link Wave14Events}.
 * <p>
 * Tabletop became Minecraft: a long rest is a Minecraft day (charges come back at dawn, or midnight for the Dark Unicorn Crown), a saving throw is a
 * chance or an existence (Tensura EP) check, a bonus action is an act with a short cooldown, 5 ft is 1.5 blocks, the Plane Shift spell is a
 * heel-clicking walk between places you know, and spells with no Minecraft form (fly, gust, fire breath) are real mechanics or lent Tensura skills.
 */
public final class Wave14 {
    private Wave14() {}

    private static final Passive ANY = Passive.flag(Wave14Rules.F_ANY);
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ------------------------------------------------------------ the dragon circlets
        out.accept(RelicDef.of("gold_circlet", "Gold Circlet", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().look(Look.CIRCLET).palette(0xE0B33A, 0xA6741A, 0xFF7A1C).armor(3, 1.5)
                .desc("A dragon's circlet of fire. Breathe a nine-block cone of flame that burns what it strikes and lights what can burn; fire you deal keeps burning. Carry another dragon circlet and the breath grows.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_GOLD)).passive(Passive.resist("minecraft:is_fire", 0.3)).passive(Passive.skill("tensura:flame_attack_resistance"))
                .act(Act.of("fire_breath", "Fire Breath", c -> Wave14Head.breath(c, Breath.FIRE)).cooldown(Wave14Rules.BREATH_COOLDOWN).epPercent(3.0)));

        out.accept(RelicDef.of("silver_circlet", "Silver Circlet", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().look(Look.CIRCLET).palette(0xD5DCE8, 0x8FA3BC, 0x9FE4FF).armor(3, 1.5)
                .desc("A dragon's circlet of ice. Breathe a freezing cone that slows and chills; at half health or less every melee blow carries an extra d8 of cold. Carry another dragon circlet and the breath grows.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_SILVER)).passive(Passive.resist("minecraft:is_freezing", 0.3)).passive(Passive.skill("tensura:cold_resistance"))
                .act(Act.of("ice_breath", "Freezing Breath", c -> Wave14Head.breath(c, Breath.COLD)).cooldown(Wave14Rules.BREATH_COOLDOWN).epPercent(3.0)));

        out.accept(RelicDef.of("steel_circlet", "Steel Circlet", Grade.LEGEND, RelicSlot.HEAD)
                .attune().vessel().look(Look.CIRCLET).palette(0x8A93A6, 0x4E5668, 0xE8E27A).armor(3, 1.5)
                .desc("A dragon's circlet of thunder. Breathe a percussive cone that hurls and dazes; once a day grow to a Large creature for three minutes. Carry another dragon circlet and the breath grows.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_STEEL)).passive(Passive.resist("minecraft:is_lightning", 0.3)).passive(Passive.skill("tensura:electricity_resistance"))
                .act(Act.of("thunder_breath", "Thunder Breath", c -> Wave14Head.breath(c, Breath.THUNDER)).cooldown(Wave14Rules.BREATH_COOLDOWN).epPercent(3.0))
                .act(Act.of("siege_monster", "Siege Monster", Wave14Head::siege).cooldown(100).mana(5).usesPerDay(1)));

        // ------------------------------------------------------------ the other crowns
        out.accept(RelicDef.of("tiny_duck_crown", "Tiny Duck Crown", Grade.SPECIAL, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0xE6B33A, 0x9A6A24, 0xFF6A2A).armor(1, 0.0)
                .desc("A tiny gold crown with a bill. At dawn there is an even chance that the ducks of the countryside come and follow you until midnight; say the word and five of them help you with luck and a quick hand.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_DUCK)).passive(Passive.attr("minecraft:generic.luck", 1.0, Operation.ADD_VALUE))
                .act(Act.of("king_of_ducks", "King of Ducks", Wave14Head::kingOfDucks).cooldown(100)));

        out.accept(RelicDef.of("unicorn_crown", "Unicorn Crown", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0xEDEEF6, 0xF0C85A, 0xFF8AD0).armor(2, 0.5)
                .desc("Blessed by wise unicorns: magic glances off you. Three charges of Healing Touch (they return at dawn), Detect Evil, Druidcraft and Pass without Trace at will, and your hair is any colour you like. An evil deed ends the attunement.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_UNICORN)).passive(Passive.resist("minecraft:magic", 0.25)).passive(Passive.skill("tensura:magic_resistance"))
                .act(Act.of("healing_touch", "Healing Touch", Wave14Head::healingTouch).cooldown(40))
                .act(Act.of("detect_evil", "Detect Evil and Good", Wave14Head::detectEvil).cooldown(200))
                .act(Act.of("druidcraft", "Druidcraft", Wave14Head::druidcraft).cooldown(40))
                .act(Act.of("pass_without_trace", "Pass without Trace", Wave14Head::passWithoutTrace).cooldown(600))
                .act(Act.of("hair_colour", "Hair Colour", Wave14Head::hair).cooldown(10)));

        out.accept(RelicDef.of("dark_unicorn_crown", "Dark Unicorn Crown", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0x1E1822, 0x8E1B2E, 0xE0244A).armor(2, 1.0)
                .desc("Hexed by dark unicorns: magic glances off you. Three charges of Void Beam (they return at midnight), Detect Evil, Thaumaturgy and Pass without Trace at will. Feeding a horse is aiding a unicorn, and ends the attunement.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_DARK)).passive(Passive.resist("minecraft:magic", 0.25)).passive(Passive.skill("tensura:magic_resistance"))
                .act(Act.of("void_beam", "Void Beam", Wave14Head::voidBeam).cooldown(60).mana(2))
                .act(Act.of("detect_evil", "Detect Evil and Good", Wave14Head::detectEvil).cooldown(200))
                .act(Act.of("thaumaturgy", "Thaumaturgy", Wave14Head::thaumaturgy).cooldown(200))
                .act(Act.of("pass_without_trace", "Pass without Trace", Wave14Head::passWithoutTrace).cooldown(600)));

        out.accept(RelicDef.of("circlet_of_the_phoenix", "Circlet of the Phoenix", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.CIRCLET).palette(0xE0A93A, 0xE5532A, 0xFF9A3C).armor(2, 0.5)
                .desc("Feathered gold from the Fire Plane. Fire hurts you half as much, your magicules run deeper, and no fall hurts you. It holds a secret for the day you would die.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_PHOENIX)).passive(Passive.resist("minecraft:is_fire", 0.5)).passive(Passive.skill("tensura:flame_attack_resistance"))
                .passive(Passive.attr("tensura:max_magicule", 0.10, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("feather_fall", "Feather Fall", Wave14Head::featherFall).cooldown(100)));

        out.accept(RelicDef.of("crown_of_the_conjurer", "Crown of the Conjurer", Grade.UNIQUE, RelicSlot.HEAD)
                .attune().look(Look.CROWN).palette(0xC9AE6E, 0x3E3326, 0x6FE08A).armor(2, 0.5)
                .desc("A crown of antler tines with six charges (1d4+2 return at dawn). Conjure Fey costs one charge for a small fey creature; each extra charge makes it stronger, and two more skip the circle. Your mind is steady: you shrug off knockback.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_CONJURER)).passive(Passive.attr("minecraft:generic.knockback_resistance", 0.2, Operation.ADD_VALUE))
                .act(Act.of("conjure_fey", "Conjure Fey", Wave14Head::conjure).cooldown(40))
                .act(Act.of("conjure_strength", "Set Strength", Wave14Head::conjureStrength).cooldown(5))
                .act(Act.of("conjure_swift", "Swift Casting", Wave14Head::conjureSwift).cooldown(5)));

        out.accept(RelicDef.of("diadem_of_fabulous_truthiness", "Diadem of Fabulous Truthiness", Grade.RARE, RelicSlot.HEAD)
                .look(Look.CIRCLET).palette(0xC9D2DE, 0x4FD08A, 0x7CFFB0).armor(1, 0.0)
                .desc("A silver diadem set with green gems. Once a day it casts a Zone of Truth on one creature: it glows, its disguises fall away, and you read its health and existence and see truly for two minutes.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_DIADEM))
                .act(Act.of("zone_of_truth", "Zone of Truth", Wave14Head::truth).cooldown(20)));

        // ------------------------------------------------------------ footwear
        out.accept(RelicDef.of("ruby_boots_of_planestriding", "Ruby Boots of Planestriding", Grade.UNIQUE, RelicSlot.FEET)
                .attune().look(Look.BOOTS).palette(0xC21E3E, 0x1A1216, 0xFF5470).armor(2, 1.0)
                .desc("Click the heels of the ruby boots three times and step to the next place they know: first home (your bed), then up to four places you taught them. Three charges, 1d3 back at dawn. The Dungeon cannot be strode to or from.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_RUBY))
                .act(Act.of("planestride", "Planestride", Wave14Feet::planestride).cooldown(100))
                .act(Act.of("learn_place", "Learn this Place", Wave14Feet::learnPlace).cooldown(40)));

        out.accept(RelicDef.of("boots_of_the_indomitable", "Boots of the Indomitable", Grade.RARE, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x7A5230, 0x8A8E96, 0xC0C4CC).armor(3, 1.0).engrave("minecraft:binding_curse")
                .desc("Cursed: they will not come off while you live (the Curse of Binding; only death, creative mode or a command frees you), and you sink when you swim. But nothing can move you, and once a day a fall of six blocks or more does you no harm: it lands on the creature below.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_INDOM)).passive(Passive.attr("minecraft:generic.knockback_resistance", 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr("neoforge:swim_speed", -0.4, Operation.ADD_MULTIPLIED_BASE)).passive(Passive.attr("minecraft:player.sneaking_speed", -0.15, Operation.ADD_VALUE)));

        out.accept(RelicDef.of("mudwalkers", "Mudwalkers", Grade.SPECIAL, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x6F4E32, 0x8A3E2A, 0xA86A3A).armor(1, 0.0)
                .desc("Mud-caked boots. Soul sand, mud and shallow marsh water barely slow you. Three times a day slide six blocks the way you look, leaving the ground slick with mud for a minute.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_MUD)).passive(Passive.attr("minecraft:generic.movement_efficiency", 1.0, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.water_movement_efficiency", 0.3, Operation.ADD_VALUE))
                .act(Act.of("mud_slide", "Mud Slide", Wave14Feet::mudSlide).cooldown(20).usesPerDay(3)));

        out.accept(RelicDef.of("boots_of_the_high_ground", "Boots of The High Ground", Grade.UNIQUE, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x2A2A30, 0x6A6A74, 0xFFD27A).armor(2, 0.5)
                .desc("You jump higher and fall softer. Once a day shout that you have the high ground: for a minute your melee blows hit 30% harder and anyone who comes within three blocks is struck for you. Step down and it ends.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_HIGH)).passive(Passive.attr("minecraft:generic.jump_strength", 0.12, Operation.ADD_VALUE))
                .passive(Passive.attr("minecraft:generic.safe_fall_distance", 3.0, Operation.ADD_VALUE))
                .act(Act.of("high_ground", "I Have the High Ground", Wave14Feet::highGround).cooldown(100).usesPerDay(1)));

        out.accept(RelicDef.of("greaves_of_the_wind_walker", "Greaves of the Wind Walker", Grade.GOD, RelicSlot.FEET)
                .attune().vessel().look(Look.GREAVES).palette(0x1E2038, 0xC9A45A, 0x7A6CFF).armor(3, 2.0).engrave("minecraft:binding_curse")
                .desc("Cursed: the greave will not come off. It makes you quicker and wind-proof, and holds five charges (1d4+3 return at dawn) for eight wind spells: Gust, Feather Fall, Dust Devil, Gust of Wind, Warding Wind, Wind Wall, Control Winds, Wind Walk.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_WIND)).passive(Passive.attr("minecraft:generic.movement_speed", 0.1, Operation.ADD_MULTIPLIED_BASE))
                .passive(Passive.skill("tensura:wind_attack_resistance"))
                .act(Act.of("gust", "Gust", Wave14Feet::gust).cooldown(30))
                .act(Act.of("feather_fall", "Feather Fall", Wave14Feet::featherFallSpell).cooldown(100))
                .act(Act.of("dust_devil", "Dust Devil", Wave14Feet::dustDevil).cooldown(100))
                .act(Act.of("gust_of_wind", "Gust of Wind", Wave14Feet::gustOfWind).cooldown(100))
                .act(Act.of("warding_wind", "Warding Wind", Wave14Feet::wardingWind).cooldown(200))
                .act(Act.of("wind_wall", "Wind Wall", Wave14Feet::windWall).cooldown(200))
                .act(Act.of("control_winds", "Control Winds", Wave14Feet::controlWinds).cooldown(200))
                .act(Act.of("wind_walk", "Wind Walk", Wave14Feet::windWalk).cooldown(2400).epPercent(3.0)));

        out.accept(RelicDef.of("waterwalkers", "Waterwalkers", Grade.SPECIAL, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x7A5230, 0x3AA4F0, 0x7AD8FF).armor(1, 0.0)
                .desc("Brown boots with a water-drop gem. Switch Liquid to Solid on and the water under your feet freezes to thin ice that melts behind you (sneak to sink); beneath the surface the boots drive you on.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_WATER)).passive(Passive.attr("neoforge:swim_speed", 0.33, Operation.ADD_MULTIPLIED_BASE))
                .act(Act.of("liquid_to_solid", "Liquid to Solid", c -> { }).cooldown(10).toggle()));

        out.accept(RelicDef.of("sandals_of_sneaking", "Sandals of Sneaking", Grade.SPECIAL, RelicSlot.FEET)
                .look(Look.SANDALS).palette(0xA9AFBE, 0x4E5668, 0x4FA8FF).armor(1, 0.0)
                .desc("Silver gladiator sandals laced with blue light. Your footfalls make no sound and no sculk hears them, you move faster sneaking, creatures far off lose your trail, and your presence is hard to feel.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_SNEAK)).passive(Passive.attr("minecraft:player.sneaking_speed", 0.15, Operation.ADD_VALUE))
                .passive(Passive.effect("tensura:presence_concealment", 0)));

        out.accept(RelicDef.of("dragon_fire_boots", "Dragon Fire Boots", Grade.RARE, RelicSlot.FEET)
                .look(Look.BOOTS).palette(0x6E6E76, 0xD9D4C4, 0xFF8A1C).armor(2, 0.5)
                .desc("Scaled boots with dragon-head toes. Speak the word and jets of fire lift you for half a minute of flight. Once a day cast a fireball from the soles; in the air it may throw you off course and crash you.")
                .passive(ANY).passive(Passive.flag(Wave14Rules.F_FIRE)).passive(Passive.resist("minecraft:is_fire", 0.4)).passive(Passive.skill("tensura:flame_attack_resistance"))
                .act(Act.of("fire_jets", "Fire Jets", Wave14Feet::jets).cooldown(600).mana(2))
                .act(Act.of("fireball", "Fireball", Wave14Feet::fireball).cooldown(100).usesPerDay(1)));
    }

    /** Registers the game-bus listeners of this wave; called once from the mod constructor. */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave14Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onFall);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onInteract);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onGameEvent);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onSound);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onStepSound);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onLogin);
        NeoForge.EVENT_BUS.addListener(Wave14Events::onStopping);
    }
}
