package com.newuniverse.nusmp.antimagic;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.94.0: how Anti-Magic pays. Every ability spends Tensura Aura through {@link EnergyBridge#aura} (a fraction of the max Aura with a
 * floor; magicules when the Aura is short; free in creative), the same helper the Slash, Light, Body and Earth families use. The prices
 * are in {@link AntiMagicCosts}. The Anti-Magic Power bar is no longer spent or required anywhere.
 */
public final class AntiMagicAura {
    private AntiMagicAura() {}

    /** The last failure message sent to each player (read by the self-test; harmless otherwise). */
    private static final Map<UUID, String> LAST = new ConcurrentHashMap<>();

    public static String lastMessage(UUID player) { return LAST.get(player); }

    public static void forget(UUID player) { LAST.remove(player); }

    /** Pays {@code cost}; on a shortfall tells the player "Not enough aura to {what}." and returns false. */
    public static boolean pay(ServerPlayer p, AntiMagicCosts.Cost cost, String what) {
        if (EnergyBridge.aura(p, cost.frac(), cost.floor())) return true;
        String msg = AntiMagicCosts.notEnough(what);
        LAST.put(p.getUUID(), msg);
        try {
            GrimoireBook.fail(p, msg);
        } catch (RuntimeException noConnection) {
            // a stand-in player has no connection to send the fizzle animation to; the message is kept in LAST
        }
        return false;
    }

    /** Pays one second of a sustained state. Same as {@link #pay} but the caller words the end of the state itself. */
    public static boolean drain(ServerPlayer p, AntiMagicCosts.Cost perSecond) {
        return EnergyBridge.aura(p, perSecond.frac(), perSecond.floor());
    }

    /** The two pools before a payment, to give it back if the page then does nothing; {@code null} when there is nothing to restore. */
    public static double[] snapshot(ServerPlayer p) {
        if (p.isCreative()) return null;
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null ? null : new double[]{ex.getAura(), ex.getMagicule()};
    }

    /** Gives back what {@link #pay} took after {@link #snapshot} (a page that returned false spends nothing). */
    public static void refund(ServerPlayer p, double[] before) {
        if (before == null) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        ex.setAura(Math.max(ex.getAura(), before[0]));
        ex.setMagicule(Math.max(ex.getMagicule(), before[1]));
        ex.markDirty();
    }

    /** The Aura pool as a share of its max, 0..1; 1 for creative or a player without Tensura data. */
    public static double share(ServerPlayer p) {
        if (p.isCreative()) return 1;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return 1;
        return AntiMagicCosts.auraShare(ex.getAura(), EnergyHelper.getMaxAura(p));
    }
}
