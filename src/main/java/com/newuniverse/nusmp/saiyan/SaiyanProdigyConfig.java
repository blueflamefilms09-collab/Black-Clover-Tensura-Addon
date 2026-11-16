package com.newuniverse.nusmp.saiyan;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Saiyan Prodigy skill and the Saiyan-blooded evolution (docs/saiyan_prodigy.md), in a file of their own ({@code nusmp-saiyan-prodigy-server.toml}) so the older Saiyan configs are
 * not touched. Every value changes gameplay, so every value is a SERVER setting (rule 9). Every getter reads the file defensively (a setting not loaded yet answers its default) and clamps again, so a hand-edited
 * file can not pass a bound. {@code prodigyHub = false} restores the old behaviour at once: the separate Saiyan keys and the old Dragon Ball skills work as before.
 */
public final class SaiyanProdigyConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue HUB, MIGRATE, BLOOD, DB_ARTS, PERKS, AWAKENS, GRANT_ON_SAIYAN;
    private static final ModConfigSpec.DoubleValue BLOOD_EP, BP_SCALE, PERK_HEALTH, PERK_ATTACK, PERK_KI;
    private static final ModConfigSpec.IntValue BLOOD_KILLS, BLOOD_NEAR, ART_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("prodigy");
        HUB = b.comment("The Saiyan Prodigy hub: one intrinsic skill in one slot holds every Saiyan ability as a mode (Ki Blast, Kamehameha, forms, ...). true = a Saiyan gets it and the old Dragon Ball skills forward to it; the old Saiyan keys keep working. false = exactly the old behaviour (nothing is granted, the old skills act as before).").define("prodigyHub", true);
        GRANT_ON_SAIYAN = b.comment("Grant the Saiyan Prodigy to every Saiyan once (when the race is taken, and at login for a Saiyan who has none). false = it is only given by command (/saiyan prodigy grant).").define("grantToSaiyans", true);
        MIGRATE = b.comment("Migration at login: a player who owns one of the old Saiyan or Dragon Ball skills gets the Saiyan Prodigy once (the old skills stay owned, nothing is deleted). false = no migration.").define("prodigyMigrate", true);
        DB_ARTS = b.comment("The Dragon Ball characters' moves (the old Dragon Ball skills' modes) as arts of the Prodigy's Art mode, opened by Battle Power or by owning the old skill. false = only the Saiyan arts.").define("dbArts", true);
        BP_SCALE = b.comment("Multiplier on the Battle Power a Dragon Ball character's arts open at (1000 for the weakest, about 1.4 million for Goku, 2 million for Gohan).").defineInRange("dbArtsBpScale", 1.0, 0.0, 100.0);
        ART_COOLDOWN = b.comment("Seconds between two arts of the Art mode (a floor on top of each art's own cooldown).").defineInRange("artCooldownSeconds", 2, 0, 60);
        AWAKENS = b.comment("Becoming a Saiyan by the evolution awakens the Ki at once (the sit-still awakening is skipped). false = the Saiyan awakens as before.").define("evolutionAwakens", true);
        b.pop();

        b.push("blood");
        BLOOD = b.comment("The Saiyan-blooded evolution: a Human can evolve into a Saiyan in Tensura's evolution screen once the deeds below are done. false = the Saiyan is only made by command or by the race menu, as before.").define("bloodEvolution", true);
        BLOOD_EP = b.comment("Existence points the Human needs (Tensura's Enlightened Human asks for 100000). 0 = this deed is off.").defineInRange("bloodEp", 20000.0, 0.0, 1.0e12);
        BLOOD_KILLS = b.comment("Worthy hostile creatures (at least 10 health) the Human must have beaten. 0 = this deed is off.").defineInRange("bloodKills", 25, 0, 1000);
        BLOOD_NEAR = b.comment("Times the Human survived at or below a fifth of their health (counted once per ten minutes). 0 = this deed is off.").defineInRange("bloodNearDeath", 3, 0, 100);
        b.pop();

        b.push("perks");
        PERKS = b.comment("The small permanent gifts of the Saiyan-blooded (a little health, a little strength, a little faster Ki). No hunger effect of any kind. false = none.").define("enabled", true);
        PERK_HEALTH = b.comment("Extra maximum health, flat (4 = two hearts).").defineInRange("health", 4.0, 0.0, 40.0);
        PERK_ATTACK = b.comment("Extra attack damage, as a share (0.03 = 3 percent).").defineInRange("attack", 0.03, 0.0, 0.5);
        PERK_KI = b.comment("Extra Ki regeneration for an awakened Saiyan, percent of the maximum per second.").defineInRange("kiRegenPercentPerSecond", 0.5, 0.0, 5.0);
        b.pop();
        SPEC = b.build();
    }

    private SaiyanProdigyConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return (int) SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean hub() { return on(HUB, true); }
    public static boolean grantToSaiyans() { return on(GRANT_ON_SAIYAN, true); }
    public static boolean migrate() { return on(MIGRATE, true); }
    public static boolean dbArts() { return on(DB_ARTS, true); }
    public static double dbArtsBpScale() { return num(BP_SCALE, 1.0, 0.0, 100.0); }
    public static int artCooldownSeconds() { return whole(ART_COOLDOWN, 2, 0, 60); }
    public static boolean evolutionAwakens() { return on(AWAKENS, true); }

    public static boolean bloodEvolution() { return on(BLOOD, true); }
    public static double bloodEp() { return num(BLOOD_EP, 20000.0, 0.0, 1.0e12); }
    public static int bloodKills() { return whole(BLOOD_KILLS, 25, 0, 1000); }
    public static int bloodNearDeath() { return whole(BLOOD_NEAR, 3, 0, 100); }

    public static boolean perks() { return on(PERKS, true); }
    public static double perkHealth() { return num(PERK_HEALTH, 4.0, 0.0, 40.0); }
    public static double perkAttack() { return num(PERK_ATTACK, 0.03, 0.0, 0.5); }
    public static double perkKiRegen() { return num(PERK_KI, 0.5, 0.0, 5.0); }
}
