package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Shadow Panther's stealth, with its detection and reveal rules written down (3.45.0). While a player is VEILED (the shadow_veil ability, at most 10 s): they are invisible (the vanilla effect, no
 * particles from it), creatures that are not already hunting them do not pick them as a target beyond {@link #NOTICE_RANGE} blocks, and a faint shadow trail stays visible to everybody (the counterplay:
 * other players can follow it). The veil is REVEALED, and cannot be re-cast until its cooldown ends, by: attacking or using any ability, taking any damage, a Glowing effect, and its own timer. Bosses and
 * creatures already targeting the player are never fooled. The walking-quietness of the Acquire phase cancels the step, swim and flap game events of the wearer (what the Warden and the sculk sensors
 * hear), which is all "quiet" means here. The whole thing is off with the server setting {@code stealth = false}. State is one number per veiled player, kept in memory only and dropped by
 * {@link #forget} at logout, death and the end of the form.
 */
public final class BeastWillStealth {
    private BeastWillStealth() {}

    /** Creatures nearer than this always notice the veiled player. */
    public static final double NOTICE_RANGE = 3.0;

    private static final Map<UUID, Long> VEILED_UNTIL = new ConcurrentHashMap<>();

    public static void veil(ServerPlayer p, int ticks) {
        if (!BeastWillConfig.stealth()) return;
        VEILED_UNTIL.put(p.getUUID(), p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, true, false, false));
    }

    public static boolean veiled(ServerPlayer p) {
        Long until = VEILED_UNTIL.get(p.getUUID());
        return until != null && p.level().getGameTime() < until && !p.hasEffect(MobEffects.GLOWING);
    }

    /** Ends the veil now (an attack, an ability, a hit taken). */
    public static void reveal(ServerPlayer p) {
        if (VEILED_UNTIL.remove(p.getUUID()) != null) p.removeEffect(MobEffects.INVISIBILITY);
    }

    public static void forget(ServerPlayer p) { reveal(p); }

    public static void forget(UUID id) { VEILED_UNTIL.remove(id); }

    /** Called every tick for a player with state: expires the veil and lays the visible trail every ten ticks. */
    public static void tick(ServerPlayer p) {
        Long until = VEILED_UNTIL.get(p.getUUID());
        if (until == null) return;
        long now = p.level().getGameTime();
        if (now >= until || p.hasEffect(MobEffects.GLOWING)) {
            reveal(p);
            return;
        }
        if (now % 10 == 0) {
            int n = PerfBudgets.particles(now, 2);
            if (n > 0) p.serverLevel().sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.2, p.getZ(), n, 0.15, 0.05, 0.15, 0.0);
        }
    }

    // ------------------------------------------------------------------ the rules, as game events
    /** A creature is about to pick a target: a veiled player is not picked from afar by a creature that was not already hunting them, and a boss is never fooled. */
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer sp) || !BeastWillConfig.stealth() || !veiled(sp)) return;
        LivingEntity hunter = e.getEntity();
        if (hunter == null || BossIdentity.isBoss(hunter)) return;
        if (hunter instanceof Mob m && m.getTarget() == sp) return;                            // already hunting: not fooled
        if (hunter.distanceToSqr(sp) <= NOTICE_RANGE * NOTICE_RANGE) return;
        e.setNewAboutToBeSetTarget(null);
    }

    /** Quiet feet: the wearer's step, swim and flap events are cancelled while a will with a veil is bound and its form (or the passive) is on. */
    public static void onGameEvent(VanillaGameEvent e) {
        if (!(e.getCause() instanceof ServerPlayer sp) || !BeastWillConfig.stealth()) return;
        var h = e.getVanillaEvent();
        if (!(h.is(GameEvent.STEP) || h.is(GameEvent.SWIM) || h.is(GameEvent.FLAP) || h.is(GameEvent.HIT_GROUND))) return;
        BeastWillState s = BeastWillStore.peek(sp);
        if (s == null || s.active.isEmpty() || s.phase == BeastWillRules.OFF) return;
        if (quiet(BeastWillCatalog.get(s.active))) e.setCanceled(true);
    }

    /** Does this will walk quietly? (Data driven: it has a veil in its first phase.) */
    public static boolean quiet(BeastWillDef d) {
        if (d == null) return false;
        for (BeastWillDef.Ability a : d.acquire().abilities()) if ("shadow_veil".equals(a.kind())) return true;
        return false;
    }
}
