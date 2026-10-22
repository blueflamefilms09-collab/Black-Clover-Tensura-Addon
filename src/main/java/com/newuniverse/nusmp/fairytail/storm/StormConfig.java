package com.newuniverse.nusmp.fairytail.storm;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of Storm Magic, {@code nusmp-storm-server.toml}, section {@code [storm]}. Everything here changes the game (rules, numbers, who is hurt), so it is a SERVER
 * config, decided by the server and the same for every player; nothing about looks is here. Every reader goes through the static methods below, which answer the default while the
 * spec is not loaded yet (a unit test, the first ticks of a world) and never throw.
 */
public final class StormConfig {
    private StormConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue STAFF_ONLY, HURT_PLAYERS;
    private static final ModConfigSpec.IntValue PER_PLAYER, GLOBAL, MAX_DURATION;
    private static final ModConfigSpec.DoubleValue DAMAGE_SCALE, MAX_RADIUS;

    /** The defaults, also what is answered while the spec is not loaded. */
    public static final boolean DEFAULT_STAFF_ONLY = true, DEFAULT_HURT_PLAYERS = true;
    public static final int DEFAULT_PER_PLAYER = 1, DEFAULT_GLOBAL = 12, DEFAULT_MAX_DURATION = 400;
    public static final double DEFAULT_DAMAGE_SCALE = 1.0, DEFAULT_MAX_RADIUS = 16.0;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("storm");
        STAFF_ONLY = b.comment("Storm Magic is a Fairy Tail magic of the Stormcaller Staff, not a grimoire magic. true = the old Storm grimoire (book_storm) is never rolled for a soul, a starter pool or the "
                        + "wizard tower; it stays registered, creative-tab available and every old grimoire keeps working. false = the old grimoire behaviour: Storm Magic is rolled like any other Wind-soul magic.")
                .define("staffOnly", DEFAULT_STAFF_ONLY);
        PER_PLAYER = b.comment("The most storms one player may have alive at once (default 1: casting another ends their old one).")
                .defineInRange("maxStormsPerPlayer", DEFAULT_PER_PLAYER, StormCaps.MIN_PER_PLAYER, StormCaps.MAX_PER_PLAYER);
        GLOBAL = b.comment("The most storms alive on the whole server (default 12); a full sky refuses a new storm. The work of all storms together is also bounded per tick (StormBudget), whatever this number is.")
                .defineInRange("maxStormsGlobal", DEFAULT_GLOBAL, StormCaps.MIN_GLOBAL, StormCaps.MAX_GLOBAL);
        DAMAGE_SCALE = b.comment("Multiplies the base damage of every storm spell before the balance law (EP scaling, PvP cap) is applied. 0 = storms do no damage.")
                .defineInRange("damageScale", DEFAULT_DAMAGE_SCALE, 0.0, 3.0);
        MAX_RADIUS = b.comment("The widest a storm cell may be, whatever the spell asks.")
                .defineInRange("maxRadius", DEFAULT_MAX_RADIUS, 3.0, 16.0);
        MAX_DURATION = b.comment("The longest a storm may last, in ticks (20 = one second), whatever the spell asks.")
                .defineInRange("maxDurationTicks", DEFAULT_MAX_DURATION, 40, 600);
        HURT_PLAYERS = b.comment("Storms may hurt other players who are not on the caster's team and may be harmed by them (the server's own PvP rules still apply). false = storms only hurt mobs.")
                .define("hurtPlayers", DEFAULT_HURT_PLAYERS);
        b.pop();
        SPEC = b.build();
    }

    public static boolean staffOnly() { try { return SPEC.isLoaded() ? STAFF_ONLY.get() : DEFAULT_STAFF_ONLY; } catch (RuntimeException e) { return DEFAULT_STAFF_ONLY; } }

    public static int perPlayer() { try { return SPEC.isLoaded() ? PER_PLAYER.get() : DEFAULT_PER_PLAYER; } catch (RuntimeException e) { return DEFAULT_PER_PLAYER; } }

    public static int global() { try { return SPEC.isLoaded() ? GLOBAL.get() : DEFAULT_GLOBAL; } catch (RuntimeException e) { return DEFAULT_GLOBAL; } }

    public static double damageScale() { try { return SPEC.isLoaded() ? DAMAGE_SCALE.get() : DEFAULT_DAMAGE_SCALE; } catch (RuntimeException e) { return DEFAULT_DAMAGE_SCALE; } }

    public static double maxRadius() { try { return SPEC.isLoaded() ? MAX_RADIUS.get() : DEFAULT_MAX_RADIUS; } catch (RuntimeException e) { return DEFAULT_MAX_RADIUS; } }

    public static int maxDuration() { try { return SPEC.isLoaded() ? MAX_DURATION.get() : DEFAULT_MAX_DURATION; } catch (RuntimeException e) { return DEFAULT_MAX_DURATION; } }

    public static boolean hurtPlayers() { try { return SPEC.isLoaded() ? HURT_PLAYERS.get() : DEFAULT_HURT_PLAYERS; } catch (RuntimeException e) { return DEFAULT_HURT_PLAYERS; } }
}
