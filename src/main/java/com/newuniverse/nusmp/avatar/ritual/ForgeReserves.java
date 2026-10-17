package com.newuniverse.nusmp.avatar.ritual;

import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.item.GenesisCoreItem;
import com.newuniverse.nusmp.avatar.luck.LuckPoller;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the owner's rule is that the server MUST re-read the creator's reserves itself before it applies any cost, so a hacked client cannot bypass the tax.
 * This is the one place that does the reading: the creator's total luck, current and maximum magicule, maximum EP, the permanent tax their living Avatars
 * already cost them, the Genesis Cores in their inventory and whether they are in creative mode, all straight from the game on the server, packed into the pure
 * {@link ForgeContext} the validator judges. Every Tensura read is defensive (an unknown pool counts as zero, never an exception), so a missing storage can
 * only make a request fail its own check, not crash the handler.
 */
public final class ForgeReserves {
    private ForgeReserves() {}

    /**
     * @param rolledAptitude the aptitude the server rolled for this ritual ("" for none) and {@code rolledToken} its token (0 for none): server-held, never from the client
     * @param raceMagiculeCost the race's own creation price, so the bill the validator checks is the whole bill
     */
    public static ForgeContext read(ServerPlayer p, String rolledAptitude, long rolledToken, double raceMagiculeCost) {
        return new ForgeContext(LuckPoller.total(p), TensuraBridge.magicule(p), maxMagicule(p), TensuraBridge.maxEp(p),
                AvatarManager.get(p.getServer()).totalTax(p.getUUID()), GenesisCoreItem.countCores(p), p.isCreative(), rolledAptitude, rolledToken, raceMagiculeCost);
    }

    /** The creator's maximum magicule (0 when Tensura does not know it). */
    public static double maxMagicule(ServerPlayer p) {
        try { return Math.max(0.0, EnergyHelper.getMaxMagicule(p)); } catch (Throwable t) { return 0.0; }
    }
}
