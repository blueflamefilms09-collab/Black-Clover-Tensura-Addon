package com.newuniverse.nusmp.elite.records.rule;

import com.newuniverse.nusmp.elite.records.rule.Bonuses.Attr;
import com.newuniverse.nusmp.elite.records.rule.Bonuses.Effect;
import com.newuniverse.nusmp.elite.records.rule.Bonuses.Op;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the synergies that ship in the code. A synergy is a reward for a PLAYSTYLE, not for a single title: three titles of one road (explorer, slayer, smith ...), the two crowns, or "any five explorer
 * titles" together give a little more than the sum of their own bonuses. The numbers are small on purpose (a few percent, half a heart), because they stack with an active title and with other
 * synergies; the server's {@code bonusScale} setting scales all of them. Eighteen are built in here (some by named titles, four by tag counts); more are data ({@code data/nusmp/elite/titles/synergies}),
 * and a data synergy with the same id replaces the built-in one, so a pack can retune any of them. The original addon's synergy list is not available; these are this mod's own designs.
 */
public final class BuiltinSynergies {
    private BuiltinSynergies() {}

    // attribute ids (1.21.1 names)
    public static final String SPEED = "minecraft:generic.movement_speed", LUCK = "minecraft:generic.luck", HEALTH = "minecraft:generic.max_health", ATTACK = "minecraft:generic.attack_damage",
            ARMOR = "minecraft:generic.armor", TOUGHNESS = "minecraft:generic.armor_toughness", KNOCKBACK_RES = "minecraft:generic.knockback_resistance", ATTACK_SPEED = "minecraft:generic.attack_speed",
            ABSORPTION = "minecraft:generic.max_absorption", BLOCK_REACH = "minecraft:player.block_interaction_range", ENTITY_REACH = "minecraft:player.entity_interaction_range",
            SAFE_FALL = "minecraft:generic.safe_fall_distance", MINING = "minecraft:player.mining_efficiency",
            MAX_MAGICULE = "tensura:max_magicule", MAX_AURA = "tensura:max_aura", MAGICULE_REGEN = "tensura:magicule_regeneration_multiplier", AURA_REGEN = "tensura:aura_regeneration_multiplier",
            MASTERY_GAIN = "tensura:ability_mastery_gain", LEARNING_GAIN = "tensura:ability_learning_gain";

    /** Small fluent builder for bonus sets, used here and by the tests. */
    public static final class B {
        private final List<Attr> attrs = new ArrayList<>();
        private final List<Effect> effects = new ArrayList<>();
        private final Map<String, Double> stats = new LinkedHashMap<>();

        public B add(String attr, double v) { attrs.add(new Attr(attr, v, Op.ADD_VALUE)); return this; }

        public B base(String attr, double v) { attrs.add(new Attr(attr, v, Op.ADD_MULTIPLIED_BASE)); return this; }

        public B total(String attr, double v) { attrs.add(new Attr(attr, v, Op.ADD_MULTIPLIED_TOTAL)); return this; }

        public B effect(String id, int amp) { effects.add(new Effect(id, amp)); return this; }

        public B stat(String name, double v) { stats.merge(name, v, Double::sum); return this; }

        public Bonuses build() { return new Bonuses(attrs, effects, stats); }
    }

    public static B b() { return new B(); }

    private static SynergyDef named(String id, String name, String desc, Bonuses bonus, String... titles) {
        return new SynergyDef(id, name, desc, List.of(titles), "", 0, bonus);
    }

    private static SynergyDef tagged(String id, String name, String desc, String tag, int count, Bonuses bonus) {
        return new SynergyDef(id, name, desc, List.of(), tag, count, bonus);
    }

    public static List<SynergyDef> all() {
        List<SynergyDef> l = new ArrayList<>();
        l.add(named("pilgrims_road", "Pilgrim's Road", "A walker who also reads the land and its ruins.",
                b().base(SPEED, 0.03).add(LUCK, 0.5).build(), "wanderer", "naturalist", "ruin_delver"));
        l.add(named("worldmakers_eye", "The World-Maker's Eye", "Cartographer, deep diver and voyager of the End see more of every map.",
                b().add(LUCK, 1.0).stat(StatNames.XP_BONUS_PERCENT, 5).build(), "cartographer", "deep_diver", "end_voyager"));
        l.add(named("blade_and_bone", "Blade and Bone", "A brawler's habit, a slayer's count and a heavy hitter's weight.",
                b().add(ATTACK, 1.0).base(ATTACK_SPEED, 0.03).build(), "brawler", "slayer", "heavy_hitter"));
        l.add(named("dragonfall", "Dragonfall", "The Dragon, the Wither and the Warden all fell to you.",
                b().add(HEALTH, 4.0).base(ATTACK, 0.05).stat(StatNames.BOSS_DAMAGE_PERCENT, 8).build(), "dragon_slayer", "wither_bane", "warden_slayer"));
        l.add(named("phoenix_rite", "Phoenix Rite", "You have died enough times to stop fearing it.",
                b().add(HEALTH, 4.0).add(KNOCKBACK_RES, 0.1).effect("minecraft:regeneration", 0).build(), "survivor", "undying", "phoenix"));
        l.add(named("two_crowns", "The Two Crowns", "Demon Lord and True Hero at once: two opposed destinies in one body.",
                b().base(MAX_MAGICULE, 0.05).base(MAX_AURA, 0.05).add(HEALTH, 4.0).build(), "demon_lord", "true_hero"));
        l.add(named("sages_archive", "Sage's Archive", "Adept, collector and sage: what you learn, you learn faster.",
                b().base(MASTERY_GAIN, 0.08).base(LEARNING_GAIN, 0.08).build(), "adept", "skill_collector", "sage"));
        l.add(named("master_hands", "Master Hands", "Three ranks of the forge, one steady hand.",
                b().stat(StatNames.FORGE_QUALITY_BONUS, 5).add(MINING, 0.5).build(), "journeyman_smith", "master_smith", "grand_forgemaster"));
        l.add(named("landed_gentry", "Landed Gentry", "Land, manor and territory under one name.",
                b().stat(StatNames.COIN_BONUS_PERCENT, 5).add(ABSORPTION, 2.0).build(), "landowner", "lord_of_the_manor", "territorial_lord"));
        l.add(named("banner_of_war", "Banner of War", "Soldier, warrior and warlord: the rank and file follow you.",
                b().add(ARMOR, 1.0).stat(StatNames.WAR_DAMAGE_PERCENT, 5).build(), "soldier", "warrior", "warlord"));
        l.add(named("seat_at_the_table", "A Seat at the Table", "Envoy, regular and veteran of the Walpurgis council.",
                b().stat(StatNames.COUNCIL_VOTE_WEIGHT, 1).add(LUCK, 0.5).build(), "envoy", "council_regular", "walpurgis_veteran"));
        l.add(named("disaster_breakers", "Disaster Breakers", "You hunt calamities and you end them.",
                b().base(ATTACK, 0.04).stat(StatNames.CALAMITY_DAMAGE_PERCENT, 8).build(), "calamity_hunter", "calamity_bane"));
        l.add(named("beyond_mortal", "Beyond Mortal", "Higher being, divine ascendant, apex of EP: little of you is still mortal.",
                b().base(MAX_MAGICULE, 0.06).base(MAX_AURA, 0.06).base(MAGICULE_REGEN, 0.05).build(), "higher_being", "divine_ascendant", "apex_being"));
        l.add(named("unique_ultimate", "Unique, then Ultimate", "You hold both a Unique and an Ultimate skill.",
                b().base(MASTERY_GAIN, 0.05).stat(StatNames.EP_GAIN_PERCENT, 3).build(), "unique_bearer", "ultimate_wielder"));
        l.add(tagged("explorers_spirit", "Explorer's Spirit", "Five explorer titles: the road is home.", "explorer", 5,
                b().base(SPEED, 0.04).add(LUCK, 1.0).add(SAFE_FALL, 2.0).build()));
        l.add(tagged("war_hardened", "War Hardened", "Four titles of war: you have seen it all.", "war", 4,
                b().add(ARMOR, 1.0).add(TOUGHNESS, 1.0).stat(StatNames.WAR_DAMAGE_PERCENT, 4).build()));
        l.add(tagged("power_resonance", "Power Resonance", "Four titles of power resonate in your magicules.", "power", 4,
                b().base(MAX_MAGICULE, 0.04).base(MAX_AURA, 0.04).build()));
        l.add(tagged("many_hats", "Many Hats", "Twelve titles of any kind.", "*", 12,
                b().add(LUCK, 1.0).stat(StatNames.XP_BONUS_PERCENT, 3).build()));
        return l;
    }
}
