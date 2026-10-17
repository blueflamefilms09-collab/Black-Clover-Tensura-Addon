package com.newuniverse.nusmp.avatar.party;

/**
 * Why: an Avatar that may name wild monsters could fill the world with a private army, and could ruin itself doing it, so the rules of the NPC party are
 * numbers the owner can tune and a test can pin: when it may name (the system and the setting on, not betrayed, room in the party, the cooldown over, enough
 * spare magicules), which monster is a fit (neither a mouse nor a monster it could never command), what the naming costs the Avatar and what each member costs
 * it for as long as the member lives (the permanent magicule tax, the same bargain the creator made when it named the Avatar). Pure; the game side is
 * {@link AutonomousNamingGoal} and {@link AvatarPartyManager}.
 */
public final class PartyRules {
    private PartyRules() {}

    /** What the goal knows about the Avatar. {@code magiculeFraction} is its current magicule over its maximum (0..1). */
    public record Avatar(boolean systemOn, boolean settingOn, boolean betrayed, int members, int cap, long now, long lastNamedAt, long cooldownTicks,
                         double magiculeFraction, double minMagiculeFraction) {}

    /** May the Avatar name a monster now? The cooldown only counts after a first naming ({@code lastNamedAt} 0 means never). */
    public static boolean mayName(Avatar a) {
        if (!a.systemOn() || !a.settingOn() || a.betrayed()) return false;
        if (a.cap() <= 0 || a.members() >= a.cap()) return false;
        if (a.lastNamedAt() > 0 && a.now() - a.lastNamedAt() < Math.max(0, a.cooldownTicks())) return false;
        return a.magiculeFraction() >= a.minMagiculeFraction();
    }

    /** The share of the Avatar's EP a monster's EP must lie within: at least {@code minRatio}, at most {@code maxRatio} of it. */
    public static final double DEFAULT_MIN_RATIO = 0.10, DEFAULT_MAX_RATIO = 1.20;

    /** Is the monster a fit: strong enough to be of use, weak enough to be commanded? An unknown EP (0 or less) is never a fit. */
    public static boolean suitable(double avatarEp, double mobEp, double minRatio, double maxRatio) {
        if (avatarEp <= 0.0 || mobEp <= 0.0) return false;
        double ratio = mobEp / avatarEp;
        return ratio >= minRatio && ratio <= maxRatio;
    }

    /** Magicules the naming costs: a share of the Avatar's maximum, never less than 1. */
    public static double nameCost(double maxMagicule, double fraction) {
        return Math.max(1.0, Math.max(0.0, maxMagicule) * Math.max(0.0, Math.min(1.0, fraction)));
    }

    /** The permanent share of its maximum magicule the Avatar gives up for its party: {@code percentPerMember} for each member, at most half of the pool. */
    public static double tax(int members, double percentPerMember) {
        if (members <= 0 || percentPerMember <= 0.0) return 0.0;
        return Math.min(0.5, members * percentPerMember / 100.0);
    }

    /** The members a pass has to forget: those whose entity is gone for good or whose bond was cut. */
    public static boolean shouldForget(boolean alive, boolean sameOwner) { return !alive || !sameOwner; }
}
