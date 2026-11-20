package com.newuniverse.nusmp.devil.dual;

/**
 * Why: the lost eye of a devil's price used to cover a small part of the right eye and nothing more. Now the price creeps (docs/devil_double_magic.md): one number per player, the corruption, 0 to
 * {@link #MAX} (permille of the screen), that grows while the player does not kill and while the devil is overused (assimilating, the union), shrinks with every kill, halves at a death and falls away when there
 * is no price to pay. Pure, no Minecraft class: the server steps it once a second per contract holder with a sacrifice ({@code DevilContracts.second}); the client only draws what the server sent.
 * It can never pass the cap (default 85 percent of the screen) and the picture always keeps a clear window ({@link #clearRadius}), so the screen never becomes unplayable.
 */
public final class CorruptionRules {
    private CorruptionRules() {}

    public static final float MAX = 1000f;
    /** Hard ceiling of the cap setting: 90 percent. */
    public static final float CAP_MAX = 900f;
    /** Permille a second lost when the system is off or the player has no price: the corruption fades in a minute or so. */
    public static final float FADE_PER_SECOND = 20f;

    /** All numbers are per minute, in permille of the screen (1000 = all of it). */
    public record Params(boolean enabled, float idlePerMin, float assimPerMin, float unionPerMin, float killRelief, float deathKeep, float cap, int quietSeconds) {
        public static final Params DEFAULT = new Params(true, 20f, 40f, 80f, 40f, 0.5f, 850f, 60);

        public Params clamped() {
            return new Params(enabled, c(idlePerMin, 0, 600), c(assimPerMin, 0, 600), c(unionPerMin, 0, 1200), c(killRelief, 0, 1000), c(deathKeep, 0, 1), c(cap, 0, CAP_MAX), (int) c(quietSeconds, 0, 3600));
        }
    }

    private static float c(float v, float lo, float hi) { return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    /**
     * The corruption one second later. {@code hasPrice}: the contract has a sacrifice. {@code ticksSinceKill}: game ticks since the player's last kill. While the last kill is younger than the quiet time
     * the idle growth stops (killing keeps the devil fed); overuse grows it regardless. Never above the cap, never below 0; a level above a lowered cap is pulled down to it.
     */
    public static float step(float level, boolean hasPrice, boolean assimilating, boolean union, long ticksSinceKill, Params raw) {
        Params p = raw.clamped();
        float l = c(level, 0, MAX);
        if (!p.enabled || !hasPrice) return Math.max(0f, l - FADE_PER_SECOND);
        float gain = (ticksSinceKill >= p.quietSeconds * 20L ? p.idlePerMin : 0f) + (assimilating ? p.assimPerMin : 0f) + (union ? p.unionPerMin : 0f);
        return Math.min(p.cap, Math.min(l, p.cap) + gain / 60f);
    }

    /** A kill eases the corruption. */
    public static float onKill(float level, Params raw) {
        Params p = raw.clamped();
        return p.enabled ? Math.max(0f, c(level, 0, MAX) - p.killRelief) : c(level, 0, MAX);
    }

    /** A death keeps this share of the corruption. */
    public static float onDeath(float level, Params raw) {
        Params p = raw.clamped();
        return c(level, 0, MAX) * p.deathKeep;
    }

    /** The level as the percent (0 to 100 of the full scale) the packet carries. */
    public static int percent(float level) { return Math.round(c(level, 0, MAX) / 10f); }

    /** The share of the screen the creeping dark covers, 0 to 0.9: the level times the player's own strength setting (0 to 1), never above 0.85 unless the server's cap says so. */
    public static float cover(int percent, float strength) {
        float s = c(strength, 0, 1);
        return Math.min(CAP_MAX / MAX, c(percent, 0, 100) / 100f * s);
    }

    /** Radius of the clear window around the crosshair as a share of the half-screen: 1 with no dark, never under {@link #MIN_CLEAR} however much of the screen is covered. */
    public static final float MIN_CLEAR = 0.18f;

    /** Opacity (0..1) of the ring {@code t01} of the way out (0 = next to the clear window, 1 = the screen edge) at this cover: smooth, and never more than 0.97 so the world can always be made out. */
    public static float ringAlpha(float t01, float cover) {
        float t = c(t01, 0, 1), s = t * t * (3f - 2f * t);
        return Math.min(0.97f, c(cover, 0, 1) * 1.15f) * s;
    }

    public static float clearRadius(float cover) { return Math.max(MIN_CLEAR, 1f - 0.95f * c(cover, 0, 1)); }
}
