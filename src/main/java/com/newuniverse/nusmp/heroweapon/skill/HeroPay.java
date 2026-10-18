package com.newuniverse.nusmp.heroweapon.skill;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the dispatcher pays a technique's cost BEFORE it casts (so a player who cannot pay never starts a wind-up), but the owner's rule says a technique that
 * then refuses or fails costs nothing. {@code HeroCtx.pay} spends magicules and then Aura and has no way back, so this class reads the pool before and after
 * the payment, keeps the difference in a {@link Receipt}, and gives exactly that back on a refund (never above the pool's maximum, never taking away what
 * regenerated meanwhile). A player without a Tensura record, and a creative player, pay nothing and so have nothing to refund.
 */
final class HeroPay {
    private HeroPay() {}

    /** What one payment took: the magicules and the Aura, and the Aura the player had before (so a refund cannot raise it above that). */
    record Receipt(double magicule, double aura, double auraBefore) {
        static final Receipt NOTHING = new Receipt(0.0, 0.0, 0.0);

        boolean isNothing() { return magicule <= 0.0 && aura <= 0.0; }
    }

    /** Pays {@code share} of the maximum magicules through {@code HeroCtx.pay}; null when the player cannot pay (nothing was spent). */
    static Receipt pay(HeroCtx c, double share) {
        if (share <= 0.0 || c.player.isCreative()) return Receipt.NOTHING;
        var ex = TensuraStorages.getExistenceFrom(c.player);
        if (ex == null) return Receipt.NOTHING;
        double mp = ex.getMagicule(), ap = ex.getAura();
        if (!c.pay(share)) return null;
        return new Receipt(Math.max(0.0, mp - ex.getMagicule()), Math.max(0.0, ap - ex.getAura()), ap);
    }

    /** Gives a payment back (a technique refused or failed, or its wind-up was cancelled). Safe with a null or empty receipt. */
    static void refund(ServerPlayer p, Receipt r) {
        if (r == null || r.isNothing()) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double mp = ex.getMagicule(), ap = ex.getAura();
        double maxMp = EnergyHelper.getMaxMagicule(p);
        ex.setMagicule(Math.min(Math.max(maxMp, mp), mp + r.magicule()));
        if (r.aura() > 0.0) ex.setAura(Math.min(Math.max(r.auraBefore(), ap), ap + r.aura()));
        ex.markDirty();
    }
}
