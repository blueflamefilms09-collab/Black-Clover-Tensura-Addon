package com.newuniverse.nusmp.elite.nations.contract;

import java.util.UUID;

/**
 * Why: a posted bounty ("Contract Bounties", "player bounties"): coins held in escrow that go to whoever kills the target, a PLAYER (by id) or a MOB TYPE (by entity-type id). Posted by a player (from their
 * wallet), by a nation (from its treasury) or by the war system. Pure data plus the matching rules; the escrow is already taken when a bounty exists, so a claim cannot fail for lack of money.
 */
public final class Bounty {
    public enum TargetKind { PLAYER, MOB }

    public enum Source { PLAYER, NATION, WAR }

    public long uid;
    public TargetKind kind = TargetKind.PLAYER;
    /** Player UUID string or entity type id (minecraft:zombie). */
    public String target = "";
    public String targetName = "";
    public long coins;
    public Source source = Source.PLAYER;
    public UUID poster;
    public String posterNation = "";
    public long createdAt, expires;

    public boolean expired(long now) { return expires > 0 && now >= expires; }

    /** Does a kill of this victim claim the bounty? The poster (and, for nation bounties, a member of the posting nation) cannot claim their own bounty. */
    public boolean claimableBy(UUID killer, String killerNation, boolean victimIsPlayer, String victimKey) {
        if (killer == null || victimKey == null) return false;
        if (kind == TargetKind.PLAYER) {
            if (!victimIsPlayer || !target.equals(victimKey)) return false;
        } else {
            if (victimIsPlayer || !target.equals(victimKey)) return false;
        }
        if (killer.equals(poster)) return false;
        if (kind == TargetKind.PLAYER && killer.toString().equals(target)) return false;
        return posterNation.isEmpty() || killerNation == null || !posterNation.equals(killerNation) || source == Source.WAR;
    }

    /** The fee a poster pays on top as a percent of the bounty (burned): returns the total to charge. */
    public static long totalWithFee(long coins, int feePercent) { return coins + coins * Math.max(0, feePercent) / 100; }
}
