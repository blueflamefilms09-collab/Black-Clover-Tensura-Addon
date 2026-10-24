package com.newuniverse.nusmp.mana.zone;

/**
 * The numbers of the Mana Zone upgrade (docs/magic/mana_zone_upgrade.md), pure (no Minecraft types, unit tested). The server config
 * ({@link ManaZoneConfig}, section {@code upgrade}) fills a {@link Rules}; {@link #sanitize} keeps every number inside a range in which the
 * mechanics stay sane and cheap, whatever a config file says. With the three switches off the zone is exactly the 1.99.0 zone.
 */
public final class ZoneUpRules {
    private ZoneUpRules() {}

    /** Where a zone-cast spell is born, relative to its target. */
    public enum OriginMode {
        /** On the far side of the target, as seen from the caster ("directly behind an opponent"). */
        BEHIND,
        /** On the zone's boundary, on the line from the zone centre through the target (the spell flies inward from the edge). */
        EDGE,
        /** Above the target. */
        ABOVE
    }

    /**
     * @param evasion          the dodge on/off
     * @param evasionChance    base chance 0..1 that a threatening projectile is dodged
     * @param evasionCooldown  ticks between two dodges of one owner
     * @param evasionCostPct   percent of the owner's maximum magicule one dodge costs (0 for a free zone / creative)
     * @param evasionHorizon   ticks ahead in which a projectile counts as about to hit
     * @param evasionLooks     projectiles one owner examines per zone scan (the scan already runs every few ticks)
     * @param evasionHitRadius blocks: how close the projectile's line must pass to the owner's body centre to count as a hit
     * @param evasionImpulse   blocks per tick of the sidestep impulse
     * @param buffs            the attack / knockback-resistance modifiers on/off
     * @param attackBonus      attack damage as a fraction of the base (0.15 = +15%)
     * @param knockbackResist  added knockback resistance 0..1
     * @param cast             zone-cast on/off
     * @param castMode         where the spell is born
     * @param castRelocate     pose the caster at the origin while the page runs (off: only the aim/target/along/vfx helpers are overridden)
     * @param castGap          ticks between two zone-casts of one caster
     * @param aimGap           minimum ticks between two aim packets of one sender (the server drops faster ones)
     * @param aimTtl           ticks an aim stays valid after it arrived
     * @param castReach        blocks the BEHIND / ABOVE origin lies from the target
     * @param castMinRun       blocks: the shortest run between origin and target that is worth moving the caster for
     * @param tolerance        blocks a target may lie outside the zone sphere (the zone follows its owner, which moves between two checks)
     */
    public record Rules(boolean evasion, double evasionChance, int evasionCooldown, double evasionCostPct, int evasionHorizon, int evasionLooks, double evasionHitRadius,
                        double evasionImpulse, boolean buffs, double attackBonus, double knockbackResist, boolean cast, OriginMode castMode, boolean castRelocate, int castGap,
                        int aimGap, int aimTtl, double castReach, double castMinRun, double tolerance) {}

    public static final Rules DEFAULT = new Rules(true, 0.6, 30, 0.3, 6, 4, 0.9, 0.6, true, 0.15, 0.5, true, OriginMode.BEHIND, true, 6, 2, 30, 3.0, 2.0, 1.5);

    /** What the 1.99.0 zone did: nothing of the upgrade. */
    public static final Rules OFF = new Rules(false, 0.0, 30, 0.0, 6, 4, 0.9, 0.6, false, 0.0, 0.0, false, OriginMode.BEHIND, false, 6, 2, 30, 3.0, 2.0, 1.5);

    public static Rules sanitize(Rules r) {
        return new Rules(r.evasion, clamp(r.evasionChance, 0.0, 0.95), (int) clamp(r.evasionCooldown, 10, 1200), clamp(r.evasionCostPct, 0.0, 5.0), (int) clamp(r.evasionHorizon, 2, 12),
                (int) clamp(r.evasionLooks, 1, 8), clamp(r.evasionHitRadius, 0.5, 2.0), clamp(r.evasionImpulse, 0.2, 1.0), r.buffs, clamp(r.attackBonus, 0.0, 0.5),
                clamp(r.knockbackResist, 0.0, 1.0), r.cast, r.castMode == null ? OriginMode.BEHIND : r.castMode, r.castRelocate, (int) clamp(r.castGap, 2, 200), (int) clamp(r.aimGap, 1, 20),
                (int) clamp(r.aimTtl, 5, 100), clamp(r.castReach, 1.0, 8.0), clamp(r.castMinRun, 1.0, 6.0), clamp(r.tolerance, 0.0, 4.0));
    }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : v < lo ? lo : Math.min(v, hi); }
}
