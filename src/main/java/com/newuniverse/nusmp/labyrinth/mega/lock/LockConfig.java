package com.newuniverse.nusmp.labyrinth.mega.lock;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: every number of the escape rules is a taste the owner will want to tune after the first play (how long a party must wait for the release rune, how long a fight may last
 * before the seal gives way, what {@code /dungeon unstuck} costs), so none is in code: {@code world/serverconfig/nusmp-labyrinth-mega-lock-server.toml}, section {@code [lock]}.
 * Its own file, registered by the class itself from {@link LockRegistry#init} through the mod container that is active while the mod is being constructed (the pattern of
 * {@code CyclicSettings}), so the mod class gains no line. The values feed the two pure rule sets ({@link EscapeRules.Config}, {@link UnstuckRules.Config}), which clamp them,
 * so no value written in the file can switch the guarantee off by accident: the only switch is {@code enabled}, and with it false the dungeon behaves as it did before this
 * update (the command still tells the player the rules are off). Plain {@code define} / {@code defineInRange}, never {@code defineInList}; every reader answers its default
 * while the spec is not loaded yet.
 */
public final class LockConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, RUNES, TRIAL_RULES, MECHANIC_RULES;
    public static final ModConfigSpec.IntValue ABSENT_SECONDS, RUNE_AFTER_SECONDS, IDLE_RUNE_SECONDS, RESCUE_MINUTES, MAX_SEAL_SECONDS, BOSS_MAX_SECONDS, UNSOLVABLE_SECONDS,
            UNSTUCK_COOLDOWN_SECONDS, UNSTUCK_PARTY_COOLDOWN_SECONDS, UNSTUCK_XP_LEVELS, UNSTUCK_HOSTILE_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("lock");
        ENABLED = b.comment("The escape rules: every place that can lock players in (trial chambers, sealed and caged rooms, boss cages) opens by itself after the times below, shows a release rune, "
                        + "and /dungeon unstuck works. false = the dungeon is as it was before 1.94.0 (seals stay until the puzzle or fight is won).")
                .define("enabled", true);
        RUNES = b.comment("Show the release rune (a labelled glowing marker you right-click) when a seal may be opened. false = only the timers and /dungeon release.")
                .define("runes", true);
        TRIAL_RULES = b.comment("Apply the escape rules to trial chambers (the Sealed Archway).").define("trials", true);
        MECHANIC_RULES = b.comment("Apply the escape rules to mechanics that bar the doorways or cage the party.").define("mechanics", true);
        ABSENT_SECONDS = b.comment("A seal with nobody inside (all dead, gone or logged out) opens after this many seconds.")
                .defineInRange("absentSeconds", 10, 3, 600);
        RUNE_AFTER_SECONDS = b.comment("Seconds after a seal closed before its release rune appears, whatever is going on inside.")
                .defineInRange("runeAfterSeconds", 60, 5, 3600);
        IDLE_RUNE_SECONDS = b.comment("Seconds before the rune appears when the room is idle (no hostile creature of the fight is alive) or the seal cannot be beaten by the party present.")
                .defineInRange("idleRuneSeconds", 15, 1, 3600);
        RESCUE_MINUTES = b.comment("Minutes a party may sit in a sealed room with no hostile alive before the System opens the seal by itself (the automatic rescue).")
                .defineInRange("rescueMinutes", 5, 1, 120);
        MAX_SEAL_SECONDS = b.comment("The longest any seal (trial chamber or mechanic) may stay closed, in seconds. Then it opens whatever is happening.")
                .defineInRange("maxSealSeconds", 600, 30, 7200);
        BOSS_MAX_SECONDS = b.comment("The longest a boss arena may stay closed, in seconds. It also opens the moment the boss dies or nobody is left inside. The exit rune is there from the start.")
                .defineInRange("bossMaxSeconds", 900, 60, 7200);
        UNSOLVABLE_SECONDS = b.comment("A seal the party present cannot beat (it has no counter, or a plate puzzle for more players than are inside) opens after this many seconds.")
                .defineInRange("unsolvableSeconds", 30, 5, 3600);
        UNSTUCK_COOLDOWN_SECONDS = b.comment("Seconds between two uses of /dungeon unstuck by one player. Not applied while the player is sealed in.")
                .defineInRange("unstuckCooldownSeconds", 300, 0, 86400);
        UNSTUCK_PARTY_COOLDOWN_SECONDS = b.comment("Seconds between two uses of /dungeon unstuck party by one player.")
                .defineInRange("unstuckPartyCooldownSeconds", 600, 0, 86400);
        UNSTUCK_XP_LEVELS = b.comment("Experience levels /dungeon unstuck costs (not charged to a player who is sealed in, nor in creative mode). 0 = free.")
                .defineInRange("unstuckXpLevels", 3, 0, 100);
        UNSTUCK_HOSTILE_RADIUS = b.comment("/dungeon unstuck refuses while a hostile dungeon creature is within this many blocks (a free escape from a fight would be an exploit). 0 = no test.")
                .defineInRange("unstuckHostileRadius", 8, 0, 64);
        b.pop();
        SPEC = b.build();
    }

    private LockConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static EscapeRules.Config escape() {
        return new EscapeRules.Config(get(ABSENT_SECONDS), get(RUNE_AFTER_SECONDS), get(IDLE_RUNE_SECONDS), get(RESCUE_MINUTES), get(MAX_SEAL_SECONDS), get(BOSS_MAX_SECONDS),
                get(UNSOLVABLE_SECONDS)).clamped();
    }

    public static UnstuckRules.Config unstuck() {
        return new UnstuckRules.Config(get(UNSTUCK_COOLDOWN_SECONDS), get(UNSTUCK_XP_LEVELS), get(UNSTUCK_HOSTILE_RADIUS), get(UNSTUCK_PARTY_COOLDOWN_SECONDS)).clamped();
    }

    /** Registers the spec; called once from {@link LockRegistry#init} while the mod is being constructed. Never throws: a failure leaves the defaults in force. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-lock-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The escape rules config could not be registered ({}); the defaults apply", e.toString());
        }
    }
}
