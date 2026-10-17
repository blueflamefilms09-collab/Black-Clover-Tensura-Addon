package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.logging.LogUtils;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Why: "mana" in this mod is Tensura's magicule (with aura as the second pool that pays for what magicule cannot), held in the player's
 * existence storage; the grimoire books charge and refund it through {@code TensuraStorages.getExistenceFrom(p)} and
 * {@code EnergyHelper.getMaxMagicule(p)} (see {@code KotodamaWords.pay} and {@code avatar.TensuraBridge.refundMagicule}). A rest must give
 * mana back through exactly the same calls or it would heal a number Tensura does not read. They are wrapped here, defensively, because a
 * player Tensura does not track (no storage yet, a mod mismatch) must cost the rest its mana only, never throw out of the tick handler. The
 * refill is a plain share of the MAXIMUM, set directly rather than through {@code EnergyHelper.gainMagicule} on purpose: that path applies
 * Tensura's gain multipliers (a +10 % gain skill would make a "40 % rest" give 44 %), and a rest is a rule, not a gain.
 */
final class RestMana {
    private RestMana() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    /** Gives back {@code share} (0..1) of the player's maximum magicule, and of the maximum aura when {@code aura} is true. Never throws. */
    static void restore(ServerPlayer p, double share, boolean aura) {
        if (!(share > 0.0)) return;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double maxMagicule = EnergyHelper.getMaxMagicule(p);
            if (maxMagicule > 0.0) ex.setMagicule(Math.min(maxMagicule, ex.getMagicule() + maxMagicule * share));
            if (aura) {
                double maxAura = EnergyHelper.getMaxAura(p);
                if (maxAura > 0.0) ex.setAura(Math.min(maxAura, ex.getAura() + maxAura * share));
            }
            ex.markDirty();
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] A rest could not restore mana (Tensura storage unavailable); resting still heals. {}", t.toString());
            }
        }
    }

    /** Sets the player's current magicule to {@code share} (0..1) of the maximum. Only for the self-tests; false when Tensura does not track the player. */
    static boolean setMagiculeShare(ServerPlayer p, double share) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return false;
            double max = EnergyHelper.getMaxMagicule(p);
            if (max <= 0.0) return false;
            ex.setMagicule(max * Math.max(0.0, Math.min(1.0, share)));
            ex.markDirty();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** The player's current magicule as a share of the maximum (0..1), or -1 when Tensura does not know the player (for the admin command). */
    static double magiculeShare(ServerPlayer p) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return -1.0;
            double max = EnergyHelper.getMaxMagicule(p);
            return max <= 0.0 ? -1.0 : Math.max(0.0, Math.min(1.0, ex.getMagicule() / max));
        } catch (Throwable t) {
            return -1.0;
        }
    }
}
