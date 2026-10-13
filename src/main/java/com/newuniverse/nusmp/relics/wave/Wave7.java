package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 7: the ten chest armours (Stone Sentinel, Starstudded Leathers, Coat of the Duelist,
 * Suit of the Golden Shadow, Armor of the Lionheart, Armor of the Beheaded, False Dragon Armor, Soul Armor,
 * Arcane Conduction Armor, Delphyne's Cursed Armor). The core registers the items; this class describes them and
 * {@link Wave7Events} plays their abilities. Every ability reads a flag (one per armour) so the core's attunement
 * rules decide whether it is on.
 */
public final class Wave7 {
    private Wave7() {}

    // one flag per armour: set by the core while the armour is worn (and attuned if it needs it)
    public static final String F_STONE = "w7_stone_sentinel", F_STAR = "w7_starstudded", F_DUEL = "w7_duelist",
            F_GOLD = "w7_golden_shadow", F_LION = "w7_lionheart", F_HEAD = "w7_beheaded", F_DRAGON = "w7_false_dragon",
            F_SOUL = "w7_soul_armor", F_ARCANE = "w7_arcane_conduction", F_DELPH = "w7_delphyne";

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static boolean hooked;

    public static void defs(Consumer<RelicDef> out) {
        // ---- Armour of the Stone Sentinel (+1 breastplate, rare, cursed)
        out.accept(RelicDef.of("armour_of_the_stone_sentinel", "Armour of the Stone Sentinel", Grade.RARE, RelicSlot.CHEST)
                .attune().armor(7, 1.0).palette(0x9A9EA3, 0x1F4C55, 0x2F8F94).look(Look.PLATE)
                .desc("Cracked grey stone plate: half the food, half the phantoms, and a stony skin (Physical Attack Resistance) that slows you. Stand still for 8 seconds and you fade into the stone (Magic Sense, mobs barely notice you); after ten minutes you are a statue.")
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.15, ADD))
                .passive(Passive.attr("minecraft:generic.movement_speed", -0.08, MUL))
                .passive(Passive.skill(Wave7Skills.PHYSICAL_RESIST))
                .passive(Passive.flag(F_STONE)));

        // ---- Starstudded Leathers (studded leather, very rare)
        out.accept(RelicDef.of("starstudded_leathers", "Starstudded Leathers", Grade.UNIQUE, RelicSlot.CHEST)
                .attune().armor(7, 2.0).palette(0x1D2F7A, 0xC9D3EE, 0x8FB8FF).look(Look.LEATHER)
                .desc("Star-studded night leather: stealthier and quicker when sneaking. Three times a day it unfolds a starscape that scorches whoever hits you, outlines the invisible and lends Magic Sense and Thought Communication; inside it you can throw bolts of starlight.")
                .passive(Passive.attr("minecraft:player.sneaking_speed", 0.20, ADD))
                .passive(Passive.flag(F_STAR))
                .act(Act.of("starscape", "Emanate Starscape", Wave7Events::starscape).cooldown(200).usesPerDay(3).mana(10))
                .act(Act.of("star_bolt", "Star Bolt", Wave7Events::starBolt).cooldown(60).mana(3)));

        // ---- Coat of the Duelist (one-of-a-kind)
        out.accept(RelicDef.of("coat_of_the_duelist", "Coat of the Duelist", Grade.LEGEND, RelicSlot.CHEST)
                .armor(6, 2.0).palette(0x16161C, 0xB8BCC8, 0x8A1426).look(Look.COAT).vessel()
                .desc("A black tailcoat for a fencer: Danger Sense, far less melee damage with the off hand empty, and every hit you land sets you running. En Garde parries the next blow (with Thought Acceleration); Lunge carries you through a foe.")
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.08, MUL))
                .passive(Passive.attr("minecraft:generic.attack_speed", 0.10, MUL))
                .passive(Passive.skill(Wave7Skills.DANGER_SENSE))
                .passive(Passive.flag(F_DUEL))
                .act(Act.of("en_garde", "En Garde", Wave7Events::enGarde).cooldown(600).mana(5).epPercent(0.5))
                .act(Act.of("lunge", "Lunge", Wave7Events::lunge).cooldown(120).mana(3)));

        // ---- Suit of the Golden Shadow (legendary light armour, rogue)
        out.accept(RelicDef.of("suit_of_the_golden_shadow", "Suit of the Golden Shadow", Grade.LEGEND, RelicSlot.CHEST)
                .attune().armor(7, 2.0).palette(0x2B2D1C, 0xD4A537, 0xFFA040).look(Look.SUIT).vessel()
                .desc("Black-gold assassin's suit for a trained Rogue (the bigger your aura pool, the deeper the shadow): Magic Sense spots the hidden, kills cloak you for 5 seconds and unaware targets take sneak-attack damage. Unseen makes you invisible, with Shadow Motion, twice a day.")
                .passive(Passive.attr("minecraft:player.sneaking_speed", 0.30, ADD))
                .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, MUL))
                .passive(Passive.skill(Wave7Skills.MAGIC_SENSE))
                .passive(Passive.flag(F_GOLD))
                .act(Act.of("unseen", "Unseen", Wave7Events::unseen).cooldown(200).usesPerDay(2).mana(8)));

        // ---- Armor of the Lionheart (epic, not standard)
        out.accept(RelicDef.of("armor_of_the_lionheart", "Armor of the Lionheart", Grade.LEGEND, RelicSlot.CHEST)
                .attune().armor(10, 3.0).palette(0xC4C8D0, 0xD9A441, 0xD9622A).look(Look.PLATE).vessel()
                .desc("Silver plate under a lion's mane: Abnormal Condition Resistance, and you and allies within 10 blocks cannot be frightened. Once a day a Hero Haki battle cry strengthens every ally; twice a day Lion's Resolve moves your guard onto an ally.")
                .passive(Passive.attr("minecraft:generic.max_health", 4.0, ADD))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.2, ADD))
                .passive(Passive.skill(Wave7Skills.ABNORMAL_RESIST))
                .passive(Passive.flag(F_LION))
                .act(Act.of("roar", "Bolstering Roar", Wave7Events::roar).cooldown(600).usesPerDay(1).epPercent(1.0))
                .act(Act.of("resolve", "Lion's Resolve", Wave7Events::resolve).cooldown(100).usesPerDay(2).mana(5)));

        // ---- Armor of the Beheaded (very rare, cursed)
        out.accept(RelicDef.of("armor_of_the_beheaded", "Armor of the Beheaded", Grade.UNIQUE, RelicSlot.CHEST)
                .attune().armor(9, 2.0).palette(0x8D949C, 0x2B2B30, 0x9FD8FF).look(Look.PLATE)
                .desc("Headless plate with a chilling aura. Cursed: it takes a heart of your life, and in return no axe finds your head. Detach your head for 45 seconds for magical senses (night sight, Magic Sense, no drowning or blinding), but you cannot eat or drink; Dispel Magic strips magic from your foes and darkens those senses for a minute.")
                .passive(Passive.attr("minecraft:generic.max_health", -2.0, ADD))
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.1, ADD))
                .passive(Passive.flag(F_HEAD))
                .act(Act.of("detach_head", "Detach Head", Wave7Events::detachHead).cooldown(400).mana(4))
                .act(Act.of("dispel_magic", "Dispel Magic", Wave7Events::dispelMagic).cooldown(600).mana(6)));

        // ---- False Dragon Armor (+2 plate)
        out.accept(RelicDef.of("false_dragon_armor", "False Dragon Armor", Grade.UNIQUE, RelicSlot.CHEST)
                .attune().armor(9, 3.0).palette(0x8A1A1A, 0x9B9A3A, 0xFFD23A).look(Look.PLATE)
                .desc("Red steel plate with a dragon's eye in the chest: fire barely hurts you (Flame Attack and Heat Resistance). Once a day it burns out fiery wings that let you fly for 2 minutes.")
                .passive(Passive.resist("minecraft:is_fire", 0.6))
                .passive(Passive.skill(Wave7Skills.FLAME_RESIST))
                .passive(Passive.skill(Wave7Skills.HEAT_RESIST))
                .passive(Passive.flag(F_DRAGON))
                .act(Act.of("fiery_wings", "Fiery Wings", Wave7Events::fieryWings).cooldown(200).usesPerDay(1).mana(12)));

        // ---- Soul Armor (rare)
        out.accept(RelicDef.of("soul_armor", "Soul Armor", Grade.RARE, RelicSlot.CHEST)
                .attune().armor(7, 1.5).palette(0xC8D0DA, 0x2A2C33, 0x3FB4FF).look(Look.PLATE)
                .desc("Headless white plate that drinks the life of what you kill: each kill adds 1 to 4 temporary hit points (up to 4 hearts, fading after a day, a rest or when the armour comes off) and returns some of your magicules and aura. Heavy plate is noisy.")
                .passive(Passive.flag(F_SOUL)));

        // ---- Arcane Conduction Armor (rare light armour, spellcaster)
        out.accept(RelicDef.of("arcane_conduction_armor", "Arcane Conduction Armor", Grade.RARE, RelicSlot.CHEST)
                .attune().armor(5, 1.0).palette(0x1B1D26, 0x5A7FC0, 0x58A8FF).look(Look.LEATHER)
                .desc("A circuit-lined suit for a spellcaster (the bigger your magicule pool, the better it protects) that reconfigures into four modes: Base (resists force and magic), Fortified (tougher, slower), Stealth (hard to see, but your spells are sealed) and Burst (stronger spells, more fragile). Only Base works without a caster's pool.")
                .passive(Passive.flag(F_ARCANE))
                .act(Act.of("reconfigure", "Reconfigure", Wave7Events::reconfigure).cooldown(40)));

        // ---- Delphyne's Cursed Armor (legendary)
        out.accept(RelicDef.of("delphynes_cursed_armor", "Delphyne's Cursed Armor", Grade.LEGEND, RelicSlot.CHEST)
                .attune().armor(11, 4.0).palette(0x2C3036, 0x6A7078, 0x8FB4C8).look(Look.PLATE).vessel()
                .desc("Dark dragon-scale plate (Dragon Skin): dragons hit you far less, breath is mostly shrugged off, nothing can frighten you, and you understand every tongue. It is heavy and noisy and drags a wearer under 3,000 EP; the tail sweeps foes away.")
                .passive(Passive.attr("minecraft:generic.knockback_resistance", 0.25, ADD))
                .passive(Passive.resist("minecraft:dragon_breath", 0.7))
                .passive(Passive.effect("minecraft:hero_of_the_village", 0))
                .passive(Passive.skill(Wave7Skills.DRAGON_SKIN))
                .passive(Passive.skill(Wave7Skills.ABNORMAL_RESIST))
                .passive(Passive.skill(Wave7Skills.THOUGHT_COMMUNICATION))
                .passive(Passive.flag(F_DELPH))
                .act(Act.of("tail_sweep", "Tail Sweep", Wave7Events::tailSweep).cooldown(160).mana(5)));
    }

    /** Registers the wave's game-bus listeners (called once from the mod constructor). */
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave7Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onVisibility);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onUseStart);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onSpawn);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onWake);
        NeoForge.EVENT_BUS.addListener(Wave7Events::onLogout);
    }
}
