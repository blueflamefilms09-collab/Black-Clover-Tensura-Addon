package com.newuniverse.nusmp.boss.god;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Why: the god-tier Lucius answers what the players do: a massive channel (the Checkmate counter), a launched devastating attack (the
 * spatial swap) and an Ultimate skill (the reversal). Each answer has a cooldown that must hold per target, and "massive channel" has to
 * be a precise, cheap definition, or the boss would punish every ordinary cast. This pure class holds the definitions and the cooldown
 * book; the glue only reports facts (how long a key has been held, which skill category, which tick it is). Unit tested.
 * <ul>
 *   <li><b>Massive grimoire cast</b>: a page whose cost is {@value #HEAVY_COST_PERCENT}% of the mana or more, or a zone or signature
 *       page, held for at least 75% of its cast time (and at least 10 ticks).</li>
 *   <li><b>Mana Zone</b>: switching it on is a massive channel at once.</li>
 *   <li><b>Held skill</b>: any Tensura skill (not a resistance or an intrinsic) held down for {@value #SKILL_CHANNEL_TICKS} ticks or more.</li>
 * </ul>
 */
public final class CounterRules {
    private CounterRules() {}

    public static final int HEAVY_COST_PERCENT = 25, SKILL_CHANNEL_TICKS = 30, MIN_HOLD = 10;
    /** Cooldowns in ticks: the Checkmate counter per target (8 s), the reversal (10 s), the swap (12 s). */
    public static final int CHECKMATE_COOLDOWN = 160, REVERSAL_COOLDOWN = 200, SWAP_COOLDOWN = 240;

    /** Is a held grimoire page a massive channel right now? */
    public static boolean massiveGrimoire(double costPercent, boolean zoneOrSignature, int heldTicks, int castTicks, boolean castable) {
        if (!castable) return false;
        boolean heavy = zoneOrSignature || costPercent >= HEAVY_COST_PERCENT;
        if (!heavy) return false;
        int need = Math.max(MIN_HOLD, (int) Math.ceil(castTicks * 0.75));
        return heldTicks >= need;
    }

    /** Is a Tensura skill held for long enough to be a massive channel? 'category' is the lower-case type name (unique, ultimate, extra, common ...). */
    public static boolean massiveSkill(String category, int heldTicks) {
        if (category == null) return false;
        if (category.equals("resistance") || category.equals("intrinsic")) return false;
        return heldTicks >= SKILL_CHANNEL_TICKS;
    }

    /** Is this skill category the one the reversal answers? */
    public static boolean reversible(String category) { return "ultimate".equals(category); }

    /**
     * The true damage the reversal mirrors back at the caster: 'pct' percent of the caster's maximum health, never more than 40% of it
     * (the single-hit cap of the god tier), 0 for a non-positive percent. The damage the skill would have done is unknowable (the
     * activation is cancelled before it is worked out), so this is a share of the caster's health, honestly named.
     */
    public static float mirrorDamage(float casterMax, double pct) {
        if (!(casterMax > 0f) || !(pct > 0.0)) return 0f;
        return (float) Math.min(casterMax * pct / 100.0, casterMax * TrueDamage.SINGLE_HIT_CAP);
    }

    /** The damage a reflected blow returns to its owner: half of the blow, never more than 40% of the owner's maximum. */
    public static float swapMirror(float blow, float ownerMax) {
        if (!(blow > 0f) || !(ownerMax > 0f)) return 0f;
        return Math.min(blow * 0.5f, (float) (ownerMax * TrueDamage.SINGLE_HIT_CAP));
    }

    /** A book of "ready again at" ticks by key (a player, a target, a boss): one use sets the cooldown. */
    public static final class Cooldowns<K> {
        private final Map<K, Long> readyAt = new HashMap<>();

        public boolean ready(K key, long now) { return now >= readyAt.getOrDefault(key, Long.MIN_VALUE); }

        /** Uses it now: false (and no change) if it is not ready. */
        public boolean use(K key, long now, int cooldownTicks) {
            if (!ready(key, now)) return false;
            readyAt.put(key, now + Math.max(0, cooldownTicks));
            if (readyAt.size() > 128) prune(now);
            return true;
        }

        public long readyAt(K key) { return readyAt.getOrDefault(key, Long.MIN_VALUE); }

        private void prune(long now) {
            Iterator<Map.Entry<K, Long>> it = readyAt.entrySet().iterator();
            while (it.hasNext()) if (it.next().getValue() <= now) it.remove();
        }

        public void clear() { readyAt.clear(); }

        public int size() { return readyAt.size(); }
    }

    /** One player's held-skill watch: set when a skill is activated, cleared when it is released; reports when the hold is long enough. */
    public static final class ChannelWatch {
        private long since = -1;
        private String category = "";

        public void begin(long now, String category) {
            since = now;
            this.category = category == null ? "" : category;
        }

        public void end() { since = -1; }

        public boolean held() { return since >= 0; }

        /** Has the held skill been a massive channel since it began? */
        public boolean massive(long now) { return since >= 0 && massiveSkill(category, (int) Math.min(Integer.MAX_VALUE, now - since)); }
    }
}
