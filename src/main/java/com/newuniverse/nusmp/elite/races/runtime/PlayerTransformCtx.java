package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.state.TransformContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the real {@link TransformContext} of the state machine, built from a server player and the tier of their race each time it is needed (it holds nothing, so it can never
 * go stale). Night is the vanilla night of a dimension that has a sky; the full moon is moon phase 0 of that night (or any night when the server relaxes it with
 * {@code greatApeAnyNight}); the sky is "visible" when nothing solid is above the player's head; aura is the share left in Tensura's aura pool.
 */
public final class PlayerTransformCtx implements TransformContext {
    private final ServerPlayer p;
    private final int tier;

    public PlayerTransformCtx(ServerPlayer p, int tier) {
        this.p = p;
        this.tier = tier;
    }

    @Override public int tier() { return tier; }

    @Override public double ep() { return TensuraBridge.maxEp(p); }

    @Override public double hpFraction() { return p.getMaxHealth() <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, p.getHealth() / p.getMaxHealth())); }

    @Override public boolean night() {
        ServerLevel l = p.serverLevel();
        if (!l.dimensionType().hasSkyLight() || l.dimensionType().hasFixedTime()) return false;
        long t = l.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    @Override public boolean fullMoon() {
        if (!night()) return false;
        return EliteRacesConfig.greatApeAnyNight() || p.serverLevel().getMoonPhase() == 0;
    }

    @Override public boolean skyVisible() { return p.serverLevel().canSeeSky(p.blockPosition().above()); }

    @Override public int itemCount(String itemId) { return PlayerRaceCtx.count(p, itemId); }

    @Override public double auraFraction() { return RaceEnergy.auraFraction(p); }

    @Override public long now() { return p.serverLevel().getGameTime(); }

    @Override public double epScale() { return EliteRacesConfig.epGateScale(); }
}
