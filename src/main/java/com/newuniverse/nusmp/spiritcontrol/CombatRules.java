package com.newuniverse.nusmp.spiritcontrol;

import java.util.List;

/**
 * Why: the decisions of combat mode as pure functions (no Minecraft types, unit tested): who may be a target, which spell a spirit casts, what it refuses and why, how strong the cast is, how much damage may
 * leave it, and on which tick a player's decision falls. The game side ({@code SpiritCombat}) gathers the facts (one target query, the two pools) and asks these.
 */
public final class CombatRules {
    private CombatRules() {}

    /** Why a decision ended without a cast. */
    public enum Refusal { NONE, NO_SPELL_READY, NO_SPIRIT_MANA, NO_USER_MANA, OUT_OF_RANGE }

    /** The chosen spell's index in the list, or -1 with the reason. */
    public record Pick(int index, Refusal why) {
        public boolean cast() { return index >= 0; }
    }

    /**
     * Is this living thing a legal target of a spirit? Never the owner, a dead thing, a spectator, an ally, or anything of the owner's side (a pet, a summon, a teammate's). A player is a target only when the
     * server's own rules already count them an enemy of the owner ({@code playerEnemy}: PvP is allowed between them and they are not on the owner's FTB team). A mob must be hostile (a monster of the game's
     * kinds) or be hunting the owner right now.
     */
    public static boolean validTarget(boolean self, boolean alive, boolean spectator, boolean allied, boolean ownerSide, boolean isPlayer, boolean playerEnemy, boolean hostileMob, boolean huntingOwner) {
        if (self || !alive || spectator || allied || ownerSide) return false;
        if (isPlayer) return playerEnemy;
        return hostileMob || huntingOwner;
    }

    /** True when this player's decision falls on this tick: one tick in {@code period}, spread over the players by {@code hash} so they do not all decide together. */
    public static boolean decisionTick(long tickCount, int hash, int period) {
        int p = Math.max(1, period);
        return Math.floorMod(tickCount + hash, (long) p) == 0L;
    }

    /**
     * Picks the strongest spell (highest base damage) that is off cooldown, in range and affordable in both pools. When none can be cast, says which wall was hit first for the best candidate: all on cooldown,
     * all out of range, the spirit's pool too low, or the user's pool too low.
     *
     * @param readyAt     per spell, the game tick it may be cast again
     * @param userCost    per spell, the magicules the user's share costs
     */
    public static Pick pick(List<SpiritSpellDef> spells, long[] readyAt, long now, double distance, double spiritMana, double share, double[] userCost, double userMana) {
        int best = -1;
        boolean anyReady = false, anyInRange = false, anySpirit = false;
        for (int i = 0; i < spells.size(); i++) {
            SpiritSpellDef s = spells.get(i);
            if (now < readyAt[i]) continue;
            anyReady = true;
            if (distance > s.range()) continue;
            anyInRange = true;
            if (ManaSplit.spiritUnits(s.units(), share) > spiritMana + 1e-9) continue;
            anySpirit = true;
            if (userCost[i] > userMana + 1e-9) continue;
            if (best < 0 || s.raw() > spells.get(best).raw()) best = i;
        }
        if (best >= 0) return new Pick(best, Refusal.NONE);
        if (!anyReady) return new Pick(-1, Refusal.NO_SPELL_READY);
        if (!anyInRange) return new Pick(-1, Refusal.OUT_OF_RANGE);
        return new Pick(-1, anySpirit ? Refusal.NO_USER_MANA : Refusal.NO_SPIRIT_MANA);
    }

    /**
     * How strong the cast is: the bond (trust 0..100) lends 0.9 to 1.25, the two pools lend 0.85 (both empty) to 1.15 (both full) by their average fill; the product is held to 0.6..1.6.
     */
    public static double powerMult(int trust, double spiritFill, double userFill) {
        double bond = 0.9 + 0.35 * SpiritMana.clampTrust(trust) / 100.0;
        double avg = (clamp01(spiritFill) + clamp01(userFill)) / 2.0;
        double pools = 0.85 + 0.30 * avg;
        return Math.max(0.6, Math.min(1.6, bond * pools));
    }

    /** The hard ceiling on one cast's damage (a bad number reads as zero). */
    public static float clampDamage(float damage, float cap) {
        if (Float.isNaN(damage) || damage <= 0f) return 0f;
        return Math.min(damage, Math.max(0f, cap));
    }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }

    /** True when the player's last fight is recent enough to look for a target now (the idle scan runs on its own slower clock). */
    public static boolean inFight(long now, long lastHurtMob, long lastHurtBy, int window) {
        return now - lastHurtMob <= window || now - lastHurtBy <= window;
    }
}
