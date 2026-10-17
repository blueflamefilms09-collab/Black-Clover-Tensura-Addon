package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules;
import com.newuniverse.nusmp.relics.Grade;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Region Themes and the Crucible of Choice, {@code world/serverconfig/nusmp-labyrinth-mega-theme-server.toml}, section
 * {@code [theme]}. Its own spec (the pattern of {@code DndConfig} and {@code MegaConfig}) so this builder's switches never edit another builder's
 * file, and so an owner who finds the catastrophes too harsh or too soft can retune them without a mod update: nothing here could be balanced in a
 * game before it shipped, so every number a player feels (flood length and strength, collapse damage, spirit strength and patience) is a key. Plain
 * {@code define} / {@code defineInRange} only (never {@code defineInList}: a list-validated value throws while the server is still loading) and every
 * reader goes through {@link #get}, which answers the default while the spec is not loaded yet (a processor can run before SERVER configs are read).
 * The lead registers it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, ThemeConfig.SPEC, "nusmp-labyrinth-mega-theme-server.toml")}.
 */
public final class ThemeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CRUCIBLE_ENABLED, AMBIENT_ENABLED, SHOW_ROADS;
    public static final ModConfigSpec.IntValue OFFER_COUNT, FLOOD_SECONDS, SPIRIT_MINUTES, COLLAPSE_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> MIN_GRADE;
    public static final ModConfigSpec.DoubleValue FLOOD_AMPLIFIER_PER_THREAT, COLLAPSE_DAMAGE, COLLAPSE_DAMAGE_PER_THREAT, SPIRIT_HEALTH_PER_THREAT, SPIRIT_DAMAGE_PER_THREAT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("theme");
        CRUCIBLE_ENABLED = b.comment("The Crucible of Choice: the first survival player in a Crucible Vault makes its relics appear, and taking one forfeits the rest and starts a catastrophe.",
                        "false = the vault ticker does nothing and the pedestals keep their old behaviour (they are empty in a new vault: nothing can be taken there).")
                .define("crucibleEnabled", true);
        OFFER_COUNT = b.comment("How many relics each Crucible Vault puts on its pedestals (one may be taken, the others crumble). The real count is the smaller of this and the number of pedestals the sector's vault has.")
                .defineInRange("offerCount", 3, 1, 3);
        MIN_GRADE = b.comment("The lowest relic grade a Crucible offers: Normal, Special, Rare, Unique, Legend, God (also written Mythical) or Genesis. Every vault draws from this grade up to Genesis, weighted by the grade's loot weight, so Genesis stays very rare.",
                        "A value that is not a grade is read as Unique.")
                .define("minGrade", "UNIQUE", o -> o instanceof String s && CrucibleRules.gradeOrdinal(s) >= 0);   // not defineInList(List.of(..)): a list-validated value throws while the file does not exist yet
        SHOW_ROADS = b.comment("When a vault's relics appear, tell the people inside which road each one leads to (the theme of the next ten floors), and tell them again when one is taken.",
                        "false = they choose blind and only learn the road from what the next region looks like.")
                .define("showRoads", true);
        FLOOD_SECONDS = b.comment("How long the toxic magicule flood lasts (it ends sooner when nobody is left in the vault).")
                .defineInRange("floodSeconds", 30, 5, 300);
        FLOOD_AMPLIFIER_PER_THREAT = b.comment("Magicule sickness level of the flood = round((threat - 1) * this), at most 4. 0 = always level 0 (the flood only slows).")
                .defineInRange("floodAmplifierPerThreat", 0.34, 0.0, 2.0);
        COLLAPSE_SECONDS = b.comment("How long the collapse lasts: a two second warning, then every 1.5 seconds a stone is aimed at each person in the vault; it is marked on the floor for 0.7 seconds before it lands.")
                .defineInRange("collapseSeconds", 8, 3, 60);
        COLLAPSE_DAMAGE = b.comment("Damage of the first collapse stone that lands on a person (a stone never takes more than 35% of their maximum health, and the whole collapse never more than 60%).")
                .defineInRange("collapseDamage", 2.0, 0.0, 50.0);
        COLLAPSE_DAMAGE_PER_THREAT = b.comment("Extra collapse pulse damage per threat level above 1.")
                .defineInRange("collapseDamagePerThreat", 0.6, 0.0, 10.0);
        SPIRIT_MINUTES = b.comment("The Cursed Spirit is let go this many minutes after the last player left its sector.")
                .defineInRange("spiritMinutes", 10, 1, 120);
        SPIRIT_HEALTH_PER_THREAT = b.comment("The Cursed Spirit's maximum health is multiplied by 1 + this * (threat - 1) (on top of its roster entry's healthScale and the Labyrinth's health buffer).")
                .defineInRange("spiritHealthPerThreat", 0.25, 0.0, 5.0);
        SPIRIT_DAMAGE_PER_THREAT = b.comment("The Cursed Spirit's attack damage is multiplied by 1 + this * (threat - 1), at most 20.")
                .defineInRange("spiritDamagePerThreat", 0.08, 0.0, 2.0);
        AMBIENT_ENABLED = b.comment("A few ambient particles of the sector's theme around the players inside it (every two seconds, a handful per player).")
                .define("ambientEnabled", true);
        b.pop();
        SPEC = b.build();
    }

    private ThemeConfig() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The lowest grade a vault offers: the config value, or UNIQUE while the config is not loaded or holds something that is not a grade. */
    public static Grade minGrade() {
        int g = CrucibleRules.minGrade(get(MIN_GRADE));
        Grade[] all = Grade.values();
        return all[Math.max(0, Math.min(all.length - 1, g))];
    }
}
