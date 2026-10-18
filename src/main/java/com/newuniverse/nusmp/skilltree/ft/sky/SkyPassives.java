package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: two nodes of the route are not buttons but things that are simply true of the player while they own them, and a passive has to be written where the game decides the thing it
 * changes, not in a skill. The Updraft changes how much a fall hurts, and the game decides that in one event ({@code LivingFallEvent}, which carries a damage multiplier made for exactly
 * this), so the passive scales that multiplier by what the node level leaves and never touches health itself; the Armor Aura needs a heartbeat, and the game bus's per-player tick event
 * is the cheapest one (the handler leaves at once unless this player's tick count is on its beat, every two seconds, and only then looks the node up). Both are guarded by
 * {@code SkillTreeApi.isNodeActive}, the tree's single question "does this player own the route and the node", which is what makes the passives obey the same gate as the
 * techniques: refund the node and the passive is gone on the next beat, with nothing to clean up. The aura refreshes a five-second resistance on every ally in its circle each two
 * seconds, so the effect never lapses while friends stand near and ends within five seconds of leaving; it is ambient and without particles. The listeners are registered once from the route's
 * register class; the work is in two small package methods so the self-test can run exactly what the events run.
 */
public final class SkyPassives {
    private SkyPassives() {}

    /** The beat of the aura: it pulses when the player's tick count is 7 modulo this (so the players of a server do not all pulse on the same tick), and each pulse lasts a little more than two beats. */
    public static final int BEAT = 40, AURA_TICKS = 100;

    private static boolean wired;

    /** Registers the passives' listeners on the game bus (once). */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener(SkyPassives::onFall);
        NeoForge.EVENT_BUS.addListener(SkyPassives::onPlayerTick);
    }

    private static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) cushion(p, e);
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % BEAT == 7 && p.isAlive()) aura(p);
    }

    /** The Updraft: multiplies the damage of the fall in {@code e} by what the node level leaves of it. Returns the factor applied (1 when the player has no Updraft). */
    static float cushion(ServerPlayer p, LivingFallEvent e) {
        if (!SkillTreeApi.isNodeActive(p, SkyRoute.ID, SkyRoute.CUSHION)) return 1f;
        int level = SkillTreeApi.nodeLevel(p, SkyRoute.ID, SkyRoute.CUSHION);
        float factor = SkyNumbers.fallMultiplier(RouteRegistry.value(SkyRoute.ID, SkyRoute.CUSHION, "fall_reduction", level, SkyNumbers.fallback(SkyRoute.CUSHION, "fall_reduction")));
        e.setDamageMultiplier(e.getDamageMultiplier() * factor);
        return factor;
    }

    /** One pulse of the Armor Aura: gives Resistance to the player and every ally in the circle. Returns how many creatures it covered (0 when the player has no aura). */
    static int aura(ServerPlayer p) {
        if (!SkillTreeApi.isNodeActive(p, SkyRoute.ID, SkyRoute.AURA)) return 0;
        int level = SkillTreeApi.nodeLevel(p, SkyRoute.ID, SkyRoute.AURA);
        double radius = RouteRegistry.value(SkyRoute.ID, SkyRoute.AURA, "radius", level, SkyNumbers.fallback(SkyRoute.AURA, "radius"));
        int covered = 0;
        for (LivingEntity ally : SkyAllies.around(p, p.position(), radius)) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, AURA_TICKS, 0, true, false, true));
            covered++;
        }
        return covered;
    }
}
