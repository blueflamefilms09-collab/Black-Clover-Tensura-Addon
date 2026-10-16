package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: Shadow Travel costs more the further it carries you, and the page cost alone can not know the distance. This spends that extra through the
 * same magicule pool the grimoire pages use (magicules first, then Aura for what they can not cover, like the Kotodama words), scaled by the mod's
 * cost multiplier. Creative players and players with no Tensura data pay nothing.
 */
public final class ShadowMana {
    private ShadowMana() {}

    /** The magicule cost of {@code percent} of the caster's maximum, with the mod's cost multiplier. */
    public static double cost(ServerPlayer p, double percent) {
        return BalanceLaw.cost(p, percent, 0.0);
    }

    /** Can the caster pay it? (No change made.) */
    public static boolean canPay(ServerPlayer p, double cost) {
        if (p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null || ex.getMagicule() + ex.getAura() >= cost;
    }

    /** Pays it; false (and nothing taken) if the caster can not afford it. */
    public static boolean pay(ServerPlayer p, double cost) {
        if (cost <= 0 || p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        if (ex.getMagicule() + ex.getAura() < cost) return false;
        double fromMp = Math.min(cost, ex.getMagicule());
        ex.setMagicule(ex.getMagicule() - fromMp);
        if (cost > fromMp) ex.setAura(Math.max(0, ex.getAura() - (cost - fromMp)));
        ex.markDirty();
        return true;
    }
}
