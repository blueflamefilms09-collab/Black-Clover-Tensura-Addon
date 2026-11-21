package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * Canon leaves (docs/grimoire_canon_leaves.md), the rules. One master setting {@code canonLeaves} (SERVER, default true): off = exactly the 4.84.2 behaviour everywhere.
 * The functions with plain arguments are pure (tested without a game); the ones that read a setting or a player are the thin server hooks around them.
 * Cost: a few multiplications per hit and per cost check; one long per player who has paid the devil (server thread only, cleared on logout).
 */
public final class CanonLeaves {
    private CanonLeaves() {}

    public static final double DEFAULT_LUCK_CHANCE = 0.06, DEFAULT_LUCK_MULT = 1.5, DEFAULT_DEVIL_SHARE = 0.10;
    /** Below this share of max mana a cast makes the devil take health; the share of max health it takes; the least ticks between two payments. */
    public static final double LOW_MANA = 0.15, HEALTH_SHARE = 0.02;
    public static final long PAY_GAP_TICKS = 20L;

    // ---------------------------------------------------------------- settings (SERVER; the defaults while the config is not loaded yet)
    public static boolean on() {
        try { return NUConfig.CANON_LEAVES.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
    private static double setting(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue v, double def) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return def; }
    }
    public static double luckChance() { return clamp(setting(NUConfig.CANON_LUCK_CHANCE, DEFAULT_LUCK_CHANCE), 0.0, 1.0, DEFAULT_LUCK_CHANCE); }
    public static double luckMult() { return clamp(setting(NUConfig.CANON_LUCK_MULT, DEFAULT_LUCK_MULT), 1.0, 10.0, DEFAULT_LUCK_MULT); }
    public static double devilShare() { return clamp(setting(NUConfig.CANON_DEVIL_SHARE, DEFAULT_DEVIL_SHARE), 0.0, 0.5, DEFAULT_DEVIL_SHARE); }

    static double clamp(double v, double lo, double hi, double fallback) {
        if (!Double.isFinite(v)) v = fallback;
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------------------------------------------------------------- 3. the plain ceremony roll never gives the five-leaf
    /**
     * The five ceremony tier weights {common, uncommon, rare (five-leaf), black, god}. With canon on the five-leaf weight moves to the four-leaf (the sum stays equal); black and god are kept. Off = the weights as given.
     */
    public static double[] tierWeights(double common, double uncommon, double rare, double black, double god, boolean canon) {
        return canon ? new double[]{common, uncommon + rare, 0.0, black, god} : new double[]{common, uncommon, rare, black, god};
    }

    // ---------------------------------------------------------------- 2. four-leaf = luck cover
    /** The luck covers (the four-leaf and the five-sided diamond; the same ones that give the +5% page chance). */
    public static boolean luckCover(GrimoireCover c) { return c != null && c.isRare(); }

    /** The damage factor of one hit: {@code mult} (clamped 1..10) when the cover is a luck cover and the injected random in [0,1) is under {@code chance} (clamped 0..1), else 1. Chance 0 never, chance 1 always. */
    public static double luckFactor(GrimoireCover c, boolean canon, double chance, double mult, DoubleSupplier random) {
        if (!canon || !luckCover(c)) return 1.0;
        double p = clamp(chance, 0.0, 1.0, 0.0);
        if (p <= 0.0) return 1.0;
        double m = clamp(mult, 1.0, 10.0, 1.0);
        return random.getAsDouble() < p ? m : 1.0;
    }

    /** What a spell hit of this cover is multiplied by, on top of the leaf power: the luck crit and the mastery bonus (1 when canon is off). Server side, one hit. */
    public static double hitFactor(GrimoireCover c, LivingEntity caster) {
        if (!on() || c == null) return 1.0;
        double luck = luckFactor(c, true, luckChance(), luckMult(), caster.getRandom()::nextDouble);
        double mastery = GrimoireMastery.damageFactor(c, TensuraBridge.maxEp(caster), clamp(setting(NUConfig.CANON_MASTERY, GrimoireMastery.DEFAULT_MAX_BONUS), 0.0, 1.0, GrimoireMastery.DEFAULT_MAX_BONUS));
        return luck * mastery;
    }

    // ---------------------------------------------------------------- 3. the devil's price
    /** Five-leaf and above (the five-leaf, the triple spade, the black magic), not the god-tier. */
    public static boolean paysDevil(GrimoireCover c) { return c != null && c.isForbidden(); }

    /**
     * The devil holds {@code share} (0..0.5) of the wielder's max mana. The mod has no simple max-mana hook (the pool is Tensura's), so it is an extra cost per spell: a spell takes the same part of the
     * smaller pool, 1 / (1 - share).
     */
    public static double devilCostFactor(GrimoireCover c, double share) {
        if (!paysDevil(c)) return 1.0;
        return 1.0 / (1.0 - clamp(share, 0.0, 0.5, 0.0));
    }

    /** The mana cost factor of a cover for a wielder (the level overreach and the devil's share); 1 when canon is off. Server side, a cost check. */
    public static double costFactor(GrimoireCover c, LivingEntity wielder) {
        if (!on() || c == null) return 1.0;
        double over = GrimoireMastery.costFactor(c, TensuraBridge.maxEp(wielder), clamp(setting(NUConfig.CANON_OVERREACH, GrimoireMastery.DEFAULT_MAX_COST), 1.0, 3.0, GrimoireMastery.DEFAULT_MAX_COST));
        return over * devilCostFactor(c, devilShare());
    }

    /**
     * The health the devil takes after a cast that left {@code manaAfter} of {@code maxMana}: 0 unless the mana is under 15% of the max and at least {@link #PAY_GAP_TICKS} passed since the last payment;
     * else 2% of max health, never more than leaves the wielder one full heart (2 hp), so never lethal.
     */
    public static double healthPrice(double hp, double maxHp, double manaAfter, double maxMana, long now, long lastPaid) {
        if (!(maxMana > 0.0) || !Double.isFinite(manaAfter) || !Double.isFinite(maxMana) || manaAfter >= LOW_MANA * maxMana) return 0.0;
        if (!Double.isFinite(hp) || !Double.isFinite(maxHp) || maxHp <= 0.0 || (now >= lastPaid && now - lastPaid < PAY_GAP_TICKS)) return 0.0;
        return Math.max(0.0, Math.min(HEALTH_SHARE * maxHp, hp - 2.0));
    }

    /** Server thread only; one long per player who paid; removed on logout. */
    private static final Map<UUID, Long> LAST_PAID = new HashMap<>();

    /** After a successful cast of a grimoire of this cover: the devil takes its health when the mana is low (never for creative, spectator or an invulnerable player; never lethal; once per second at most). */
    public static void afterCast(ServerPlayer p, GrimoireCover c) {
        if (!on() || !paysDevil(c) || devilShare() <= 0.0 || protectedState(p)) return;
        long now = p.level().getGameTime();
        double price = healthPrice(p.getHealth(), p.getMaxHealth(), TensuraBridge.magicule(p), TensuraBridge.maxMagicule(p), now, LAST_PAID.getOrDefault(p.getUUID(), Long.MIN_VALUE / 2));
        if (price <= 0.0) return;
        LAST_PAID.put(p.getUUID(), now);
        p.setHealth((float) (p.getHealth() - price));
    }

    static boolean protectedState(ServerPlayer p) { return p.isCreative() || p.isSpectator() || p.isInvulnerable() || p.getAbilities().invulnerable || !p.isAlive(); }

    public static void forget(UUID id) { LAST_PAID.remove(id); }
    public static int tracked() { return LAST_PAID.size(); }

    public static void register(IEventBus bus) {
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> forget(e.getEntity().getUUID()));
    }

    // ---------------------------------------------------------------- the admin / tooltip read-out
    /** One line for /anime_multiverse grimoire cover: cover, meaning, leaf power, cost, luck and the devil's share. */
    public static String describe(ServerPlayer p, GrimoireCover c) {
        StringBuilder s = new StringBuilder(p.getName().getString()).append(": ").append(c.displayName()).append(" | ").append(GrimoireMeaning.text(c));
        s.append(String.format(java.util.Locale.ROOT, " | leaf power x%.2f, leaf cost x%.2f", GrimoireLeafPower.factor(c), GrimoireLeafPower.costFactor(c)));
        s.append(String.format(java.util.Locale.ROOT, " | cost factor x%.3f (level + devil)", costFactor(c, p)));
        s.append(String.format(java.util.Locale.ROOT, " | luck %s", on() && luckCover(c) ? (int) Math.round(luckChance() * 100) + "% of x" + luckMult() : "none"));
        s.append(String.format(java.util.Locale.ROOT, " | devil share %s", on() && paysDevil(c) ? (int) Math.round(devilShare() * 100) + "% of max mana" : "none"));
        return s.toString();
    }
}
