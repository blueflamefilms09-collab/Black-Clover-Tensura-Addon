package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: for a Saiyan Ki REPLACES the magicule (the owner's decision, 3.63.0): Ki is not a second bar next to the magicule but the player's own Tensura magicule pool, shown as "Ki". So everything that
 * spends magicule (this mod's skills and pages, Tensura's skills) already spends Ki, Tensura saves and regenerates it, and the race only adds to it (a capacity modifier in {@link SaiyanAttributes} and a
 * small extra regeneration here). This class is the one place the Saiyan code reads and pays that pool, modelled on the mod's other energy bridges: costs are a percentage of the maximum with the pack's cost
 * multiplier ({@link BalanceLaw#cost}), creative players pay nothing, and a player with no Tensura data has no Ki. Server thread only; it names no client class.
 */
public final class SaiyanKi {
    private SaiyanKi() {}

    /** The maximum Ki (the maximum magicule). */
    public static double max(ServerPlayer p) {
        try { return Math.max(0.0, EnergyHelper.getMaxMagicule(p)); } catch (RuntimeException | LinkageError e) { return 0.0; }
    }

    /** The Ki the player has now (the current magicule). */
    public static double current(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null ? 0.0 : Math.max(0.0, ex.getMagicule());
    }

    /** The cost of {@code percent} of the maximum Ki, with the pack's cost multiplier. */
    public static double cost(ServerPlayer p, double percent) { return BalanceLaw.cost(p, percent, 0.0); }

    public static boolean canPay(ServerPlayer p, double percent) {
        if (p.isCreative() || percent <= 0.0) return true;
        return current(p) >= cost(p, percent);
    }

    /** Pays {@code percent} of the maximum Ki; false (and nothing taken) if the player can not afford it. */
    public static boolean pay(ServerPlayer p, double percent) {
        if (p.isCreative() || percent <= 0.0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return false;
        double c = cost(p, percent);
        if (ex.getMagicule() < c) return false;
        ex.setMagicule(ex.getMagicule() - c);
        ex.markDirty();
        return true;
    }

    /** Gives back {@code percent} of the maximum Ki, never past the maximum. */
    public static void regen(ServerPlayer p, double percent) {
        if (percent <= 0.0) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double max = max(p);
        if (max <= 0.0 || ex.getMagicule() >= max) return;
        ex.setMagicule(Math.min(max, ex.getMagicule() + max * percent / 100.0));
        ex.markDirty();
    }
}
