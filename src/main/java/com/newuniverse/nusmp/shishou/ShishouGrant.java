package com.newuniverse.nusmp.shishou;

import java.util.UUID;

/**
 * Why: when does a player hold the Tensura Ultimate skill Shishou? One pure rule, decided on the server and keyed by the player's UUID: the skill belongs to THE wielder of record (the owner in the world's
 * {@link ShishouCap}), while Shishou is enabled and the server config {@code skill.grantUltimateSkill} is on. Claiming the sword grants it, the sword going to somebody else (or a reset of the world's record)
 * takes it away. Nothing here touches a game class; {@code ShishouGrantService} applies the verdict to the skill system.
 */
public final class ShishouGrant {
    private ShishouGrant() {}

    /** What to do to this player's skill list. */
    public enum Verdict { KEEP, GRANT, REVOKE }

    /** How often (in ticks) a player's skill is re-checked from the player tick: a few seconds, one map lookup. The claim and the login check at once. */
    public static final int CHECK_PERIOD = 100;

    /** Should this player have the skill? */
    public static boolean shouldHave(boolean enabled, boolean grantOn, UUID owner, UUID player) { return enabled && grantOn && owner != null && owner.equals(player); }

    /** The change that makes the player's skill list right. */
    public static Verdict verdict(boolean shouldHave, boolean has) { return shouldHave == has ? Verdict.KEEP : shouldHave ? Verdict.GRANT : Verdict.REVOKE; }

    /** Is it this player's turn for a periodic check? Spread by id so a server full of players does not check in one tick. */
    public static boolean due(long tickCount, int entityId) { return Math.floorMod(tickCount + entityId, (long) CHECK_PERIOD) == 0L; }
}
