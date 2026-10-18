package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: Ur's Lesson is not a button but a fact about the mage's body, and in the story an Ice-Make mage is not hurt by cold (Gray walks through blizzards in his shirt). A passive has to be written where
 * the game decides the thing it changes, not in a skill. FREEZING damage is decided in the game's incoming-damage event, which can be cancelled before the health is touched, so the passive cancels
 * damage of the freeze type for the owner there (and every second wipes the frost that powder snow builds up on the body, so even the slow and the blue screen go); the second half, from level 2, is
 * the thorns of cold: whoever strikes the owner in melee is chilled for a moment, decided in the same event because that is where the attacker is known, rate-limited by a rest of a few
 * seconds kept in the owner's own saved data (no static state) so a swarm cannot turn it into a permanent freeze. Both halves are guarded by {@code SkillTreeApi.isNodeActive}, the tree's single question "does this player
 * own the route and the node", which is what makes the passive obey the same gate as the techniques: refund the node and it is gone at once, with nothing to clean up. The listeners are registered once from the
 * route's register class; the work is in three small package methods so the self-test can run exactly what the events run.
 */
public final class IcePassives {
    private IcePassives() {}

    /** The key under the owner's persistent data where the game tick until which the thorns rest is kept. */
    static final String REST_UNTIL = "nusmp_ft_ice_rest";

    private static boolean wired;

    /** Registers the passive's listeners on the game bus (once). */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener(IcePassives::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(IcePassives::onPlayerTick);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (cancelsFreeze(p, e.getSource())) {
            e.setCanceled(true);
            return;
        }
        Entity attacker = e.getSource().getEntity();
        if (attacker instanceof LivingEntity foe && attacker == e.getSource().getDirectEntity()) thorns(p, foe);
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 3 && p.getTicksFrozen() > 0 && SkillTreeApi.isNodeActive(p, IceRoute.ID, IceRoute.FROST_BODY)) p.setTicksFrozen(0);
    }

    /** Does Ur's Lesson cancel this damage for this player (freezing damage, for a player who owns the node)? */
    static boolean cancelsFreeze(ServerPlayer p, DamageSource source) {
        return source.is(DamageTypes.FREEZE) && SkillTreeApi.isNodeActive(p, IceRoute.ID, IceRoute.FROST_BODY);
    }

    /** The thorns of cold: chills {@code attacker}, who struck {@code p} in melee. True when it did (level 2 or more, and the rest is over). */
    static boolean thorns(ServerPlayer p, LivingEntity attacker) {
        if (attacker == p || !SkillTreeApi.isNodeActive(p, IceRoute.ID, IceRoute.FROST_BODY)) return false;
        int level = SkillTreeApi.nodeLevel(p, IceRoute.ID, IceRoute.FROST_BODY);
        double seconds = value("chill_seconds", level);
        if (level < 2 || seconds <= 0) return false;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(REST_UNTIL)) return false;
        p.getPersistentData().putLong(REST_UNTIL, now + (long) value("rest", level));
        IceConstructs.chill(attacker, (int) Math.round(seconds * 20), 0, (int) value("chill_ticks", level));
        return true;
    }

    private static double value(String key, int level) { return RouteRegistry.value(IceRoute.ID, IceRoute.FROST_BODY, key, level, IceNumbers.fallback(IceRoute.FROST_BODY, key)); }
}
