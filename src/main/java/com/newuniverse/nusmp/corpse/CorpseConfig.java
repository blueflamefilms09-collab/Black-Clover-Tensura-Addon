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
    public static final ModConfigSpec.IntValue LEDGER_MAX, RAISE_MAX, MAX_ACTIVE, LIFE_SECONDS, NAMED_LIFE_SECONDS, TRUE_COOLDOWN_SECONDS,
            PACT_MINUTES, REVIVE_DECAY_SECONDS, PET_WINDOW_SECONDS, PACT_RANGE, LEDGER_RADIUS, REVIVE_MANA_PERCENT;
    public static final ModConfigSpec.DoubleValue DAMAGE_MULT, REVIVE_HEALTH, REVIVE_MAX_HEALTH_CUT;
    public static final ModConfigSpec.BooleanValue ENABLED, REVIVE_PLAYERS;

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
