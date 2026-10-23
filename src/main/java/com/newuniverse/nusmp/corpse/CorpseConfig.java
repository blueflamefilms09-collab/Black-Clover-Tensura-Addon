package com.newuniverse.nusmp.corpse;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Soul Corpse Magic raises bodies, revives players and lasts for minutes, so every number that sets how much power it hands out
 * must be a server switch an admin can turn down without a rebuild. Own spec file ({@code nusmp-corpse-server.toml}, SERVER); the lead
 * registers it with {@code modContainer.registerConfig(ModConfig.Type.SERVER, CorpseConfig.SPEC, "nusmp-corpse-server.toml")}. Plain
 * {@code define} / {@code defineInRange} only. Reads fall back to the defaults before the config is loaded (a spec value throws until
 * then), so the rules hold from the first tick.
 */
public final class CorpseConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue ZOMBIE_CAP, ZOMBIE_GLOBAL_CAP, NAMED_GLOBAL_CAP;
    public static final ModConfigSpec.BooleanValue ZOMBIES_INSTEAD_OF_PUPPETS;
    public static final ModConfigSpec.BooleanValue NUMBERED_FOR_ALL;
    public static final ModConfigSpec.IntValue LEDGER_MAX, RAISE_MAX, MAX_ACTIVE, LIFE_SECONDS, NAMED_LIFE_SECONDS, TRUE_COOLDOWN_SECONDS,
            PACT_MINUTES, REVIVE_DECAY_SECONDS, PET_WINDOW_SECONDS, PACT_RANGE, LEDGER_RADIUS, REVIVE_MANA_PERCENT;
    public static final ModConfigSpec.DoubleValue DAMAGE_MULT, REVIVE_HEALTH, REVIVE_MAX_HEALTH_CUT;
    public static final ModConfigSpec.BooleanValue ENABLED, REVIVE_PLAYERS, CONJURE, CONJURE_TENSURA;
    public static final ModConfigSpec.DoubleValue CONJURE_MANA_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("corpse");
        ENABLED = b.comment("Master switch of Rades Spirito's Soul Corpse pages (Reanimation, Jimmy, Alfredo, Carl, True Soul Corpse). false = the pages do nothing.")
                .define("enabled", true);
        LEDGER_MAX = b.comment("Most dead creatures one caster's ledger remembers (the oldest is forgotten first). Entries also expire after 60 seconds.")
                .defineInRange("ledgerMax", 24, 1, 256);
        LEDGER_RADIUS = b.comment("A creature that dies within this many blocks of a Soul Corpse mage is written into that mage's ledger.")
                .defineInRange("ledgerRadius", 32, 4, 128);
        RAISE_MAX = b.comment("Most bodies one Reanimation raises at once.")
                .defineInRange("raiseMax", 2, 1, 8);
        MAX_ACTIVE = b.comment("Most reanimated bodies one caster owns at once; raising another crumbles the oldest.")
                .defineInRange("maxActive", 4, 1, 16);
        LIFE_SECONDS = b.comment("How long a reanimated body lasts before it crumbles. Its health only goes down meanwhile.")
                .defineInRange("lifeSeconds", 60, 10, 600);
        DAMAGE_MULT = b.comment("Damage of a reanimated body compared with the living creature.")
                .defineInRange("damageMult", 1.30, 0.5, 3.0);
        NAMED_LIFE_SECONDS = b.comment("How long Jimmy, Alfredo or Carl stay once summoned (they have their own health and do not bleed).")
                .defineInRange("namedLifeSeconds", 180, 20, 1800);
        TRUE_COOLDOWN_SECONDS = b.comment("Cooldown of True Soul Corpse Magic in seconds, starting when the pact is armed.")
                .defineInRange("trueCooldown", 900, 30, 86400);
        PACT_MINUTES = b.comment("How long an armed pact lasts, in minutes.")
                .defineInRange("pactMinutes", 10, 1, 120);
        REVIVE_PLAYERS = b.comment("false = the pact only re-raises fallen pets and never cancels a player's death.")
                .define("revivePlayers", true);
        REVIVE_HEALTH = b.comment("Share of maximum health a revived person comes back with.")
                .defineInRange("reviveHealth", 0.40, 0.05, 1.0);
        REVIVE_DECAY_SECONDS = b.comment("How long a revived person carries the Soul Corpse decay (and the maximum health cut) before they are themselves again.")
                .defineInRange("reviveDecay", 90, 10, 1200);
        REVIVE_MAX_HEALTH_CUT = b.comment("Maximum health a revived person loses while they are a Soul Corpse (0.25 = minus 25 percent).")
                .defineInRange("reviveMaxHealthCut", 0.25, 0.0, 0.9);
        REVIVE_MANA_PERCENT = b.comment("Percent of the pact caster's maximum magicule paid each time the pact saves someone (on top of the cost of arming it). 0 = free.")
                .defineInRange("reviveManaPercent", 20, 0, 100);
        PACT_RANGE = b.comment("A tapped ally is only saved while the caster is online in the same dimension within this many blocks (the caster's own death has no range).")
                .defineInRange("pactRange", 96, 8, 1024);
        PET_WINDOW_SECONDS = b.comment("Seconds either side of the cast within which a fallen pet or summon of the caster is re-raised by True Soul Corpse Magic.")
                .defineInRange("petWindow", 15, 1, 120);
        CONJURE = b.comment("true (1.46.0): when no one has died near you in the last minute, Reanimation conjures new Soul Corpses out of the ground (undead that fit the",
                "biome or dimension; 1 to 3 by mastery). false = Reanimation only raises the recently dead, as in 1.44.0.").define("conjure", true);
        CONJURE_TENSURA = b.comment("Also let the ground give up the weak Tensura undead (Ghoul, Wight) when those entity types exist. Vanilla undead are always used.")
                .define("conjureTensura", true);
        CONJURE_MANA_PERCENT = b.comment("Mana a conjured corpse costs, percent of your maximum magicule per body, on top of the page's own cost. 0 = free.")
                .defineInRange("conjureManaPercent", 3.0, 0.0, 100.0);
        ZOMBIES_INSTEAD_OF_PUPPETS = b.comment("true: Soul Corpse Magic summons real zombies (owned by the caster, no sun burn, no drops) in the Marionette, Necropolis and Legion pages, in Reanimation and",
                "in True Soul Corpse Magic's re-raising of fallen pets. false = the old behaviour: stitched puppet props, and Reanimation raising the dead creature's own kind.")
                .define("zombiesInsteadOfPuppets", true);
        NUMBERED_FOR_ALL = b.comment("true: Rades's numbered undead (No.4 Jimmy, Alfredo, Carl, No.3 David, No.0 Michael Caesal and True Soul Corpse Magic) can be opened by every holder of a Soul Corpse grimoire (by mastery, with their",
                "caps and cooldowns unchanged). false = only Rades Spirito's own grimoire may open them (the 1.44.0 exclusive lock).")
                .define("numberedForAll", true);
        ZOMBIE_CAP = b.comment("Most summoned zombies one caster owns at once, over every page; summoning more dismisses the oldest.")
                .defineInRange("zombieCap", 6, 1, 16);
        ZOMBIE_GLOBAL_CAP = b.comment("Most summoned zombies on the whole server at once, all casters together (a new summon is cut to what is left).")
                .defineInRange("zombieGlobalCap", SoulZombieRules.GLOBAL_CAP_DEFAULT, 4, 512);
        NAMED_GLOBAL_CAP = b.comment("Most of Rades's numbered undead (Jimmy, David, Alfredo, Carl, Michael Caesal) on the whole server at once.")
                .defineInRange("namedGlobalCap", SoulZombieRules.NAMED_GLOBAL_CAP_DEFAULT, 3, 256);
        b.pop();
        SPEC = b.build();
    }

    private CorpseConfig() {}

    private static int geti(ModConfigSpec.IntValue v, int fallback) {
        try { return v.get(); } catch (IllegalStateException unloaded) { return fallback; }
    }

    private static double getd(ModConfigSpec.DoubleValue v, double fallback) {
        try { return v.get(); } catch (IllegalStateException unloaded) { return fallback; }
    }

    private static boolean getb(ModConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (IllegalStateException unloaded) { return fallback; }
    }

    public static boolean numberedForAll() { return getb(NUMBERED_FOR_ALL, true); }
    public static boolean zombiesInsteadOfPuppets() { return getb(ZOMBIES_INSTEAD_OF_PUPPETS, true); }
    public static int zombieGlobalCap() { return geti(ZOMBIE_GLOBAL_CAP, SoulZombieRules.GLOBAL_CAP_DEFAULT); }
    public static int namedGlobalCap() { return geti(NAMED_GLOBAL_CAP, SoulZombieRules.NAMED_GLOBAL_CAP_DEFAULT); }
    public static int zombieCap() { return geti(ZOMBIE_CAP, SoulZombieRules.CAP_DEFAULT); }
    public static boolean conjure() { return getb(CONJURE, true); }
    public static boolean conjureTensura() { return getb(CONJURE_TENSURA, true); }
    public static double conjureManaPercent() { return getd(CONJURE_MANA_PERCENT, 3.0); }
    public static boolean enabled() { return getb(ENABLED, true); }
    public static int ledgerMax() { return geti(LEDGER_MAX, 24); }
    public static int ledgerRadius() { return geti(LEDGER_RADIUS, 32); }
    public static int raiseMax() { return geti(RAISE_MAX, 2); }
    public static int maxActive() { return geti(MAX_ACTIVE, 4); }
    public static int lifeTicks() { return geti(LIFE_SECONDS, 60) * 20; }
    public static double damageMult() { return getd(DAMAGE_MULT, CorpseDecay.DAMAGE_MULT); }
    public static int namedLifeTicks() { return geti(NAMED_LIFE_SECONDS, 180) * 20; }
    public static int trueCooldownSeconds() { return geti(TRUE_COOLDOWN_SECONDS, 900); }
    public static int pactTicks() { return geti(PACT_MINUTES, 10) * 20 * 60; }
    public static boolean revivePlayers() { return getb(REVIVE_PLAYERS, true); }
    public static double reviveHealth() { return getd(REVIVE_HEALTH, 0.40); }
    public static int reviveDecayTicks() { return geti(REVIVE_DECAY_SECONDS, 90) * 20; }
    public static double reviveMaxHealthCut() { return getd(REVIVE_MAX_HEALTH_CUT, 0.25); }
    public static int reviveManaPercent() { return geti(REVIVE_MANA_PERCENT, 20); }
    public static int pactRange() { return geti(PACT_RANGE, 96); }
    public static int petWindowTicks() { return geti(PET_WINDOW_SECONDS, 15) * 20; }
}
