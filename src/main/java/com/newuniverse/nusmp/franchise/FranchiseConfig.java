package com.newuniverse.nusmp.franchise;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the franchise engravings change gameplay (bonus damage, guards, small heals, allies), so their switches are a SERVER config ({@code nusmp-franchise-server.toml}); the
 * clients only see the names and the tooltip line. Hot code reads the volatile fields, which {@link #apply} refreshes when the file loads or changes (reading a value before the
 * file is loaded throws, so every read is guarded).
 */
public final class FranchiseConfig {
    private FranchiseConfig() {}

    public static final ModConfigSpec SERVER;
    private static final ModConfigSpec.BooleanValue ENABLED, ALLIES, AURA;
    private static final ModConfigSpec.IntValue ATTACK_CAP;

    private static volatile boolean enabled = true, allies = true, aura = true;
    private static volatile double attackCap = FranchiseRules.ATTACK_CAP;

    static {
        ModConfigSpec.Builder s = new ModConfigSpec.Builder();
        s.push("franchise");
        ENABLED = s.comment("The effects of the franchise engravings (bonus damage, guards, mana returns, small heals). Off = the engravings stay on the items as names only.").define("enabled", true);
        ALLIES = s.comment("Shadow Extraction may leave a short-lived ally after a kill.").define("allies", true);
        AURA = s.comment("Guild Bond and Hearty Party refresh a faint regeneration on other players nearby.").define("aura", true);
        ATTACK_CAP = s.comment("The most extra damage all franchise engravings together add to one hit, in percent of the hit (against a player half of it). 0 to 30.").defineInRange("attackCapPercent", 15, 0, 30);
        s.pop();
        SERVER = s.build();
    }

    public static void apply() {
        try { enabled = ENABLED.get(); } catch (IllegalStateException | NullPointerException ignored) { /* not loaded yet */ }
        try { allies = ALLIES.get(); } catch (IllegalStateException | NullPointerException ignored) { /* not loaded yet */ }
        try { aura = AURA.get(); } catch (IllegalStateException | NullPointerException ignored) { /* not loaded yet */ }
        try { attackCap = ATTACK_CAP.get() / 100.0; } catch (IllegalStateException | NullPointerException ignored) { /* not loaded yet */ }
    }

    public static boolean enabled() { return enabled; }

    public static boolean allies() { return allies; }

    public static boolean aura() { return aura; }

    public static double attackCap() { return attackCap; }
}
