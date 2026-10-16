package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the abilities that make each Devil Unite mode feel like its devil (Canis: the pack and the shadow spikes; Felis: night eyes and the
 * loner's stealth; Gallus: the stunning call and the single root) are kept here, away from the stat table and from the event handlers, so each can
 * be read, tuned and switched off on its own. Every control effect goes through the balance law: the shared hard-control cooldown
 * ({@code BalanceLaw.beginControl}), then the clamps and repeat decay of {@code controlTicks} (3 s players, 1 s bosses).
 */
public final class UniteAbilities {
    private UniteAbilities() {}

    private static final int SHADOW = 0xFF3A3A55;
    /** Spikes: every 2 seconds, up to 4 enemies within 6 blocks, 10 ticks after the warning. */
    private static final int SPIKE_EVERY = 40, SPIKE_DELAY = 10, SPIKE_MAX = 4;
    private static final double SPIKE_RADIUS = 6.0;
    private static final String CALL_FOR = "nusmp_unite_call_for", CALL_N = "nusmp_unite_call_n";

    /** A mode has just begun: its opening ability. {@code oldPage}: it was started by one of the old quick pages. */
    public static void onStart(ServerPlayer p, UniteKind kind, boolean oldPage) {
        if (oldPage && !ShadowConfig.extrasOnOldPages()) return;
        if (kind == UniteKind.CANIS) {
            int n = ShadowPackProps.countFor(BalanceLaw.epScale(p), ShadowConfig.packMax());
            if (n > 0) ShadowPackProps.summon(p, n, UniteKind.TICKS);
        }
    }

    /** Called every tick for a wearer; does the periodic part of the mode. */
    public static void tick(ServerPlayer p, UniteKind kind, long now, boolean oldPage) {
        if (oldPage && !ShadowConfig.extrasOnOldPages()) return;
        switch (kind) {
            case CANIS -> { if (now % SPIKE_EVERY == 0) spikes(p); }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Canis: shadow spikes
    /** Shadow spikes rise under the nearest enemies: a warning pool first, then the hit. */
    private static void spikes(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, p.position(), SPIKE_RADIUS));
        foes.removeIf(x -> x instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator()));
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : foes) {
            if (n++ >= SPIKE_MAX) break;
            Vec3 at = t.position();
            VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, 22, 0.8f);
            SpellRuntime.later(sl, SPIKE_DELAY, () -> {
                if (!p.isAlive() || !t.isAlive() || UniteState.active(p) == null || t.distanceToSqr(p) > (SPIKE_RADIUS + 2) * (SPIKE_RADIUS + 2)) return;
                ShadowDamage.hit(p, t, 4f);
                Vec3 up = t.position().add(0, t.getBbHeight() * 0.5, 0);
                VfxSpawn.send(sl, VfxShape.SHADOW_POOL, up, up.add(0, 1, 0), SHADOW, 12, 0.4f);
            });
        }
    }

    // ---------------------------------------------------------------- Gallus: the call
    /**
     * The first call in a Gallus mode stuns every enemy within 8 blocks (1.5 to 2.5 s by distance); the second roots the enemy you look at for 4 s.
     * Returns false (and spends nothing) if there is nobody to affect, the hard-control lock is not ready, or both calls are used up.
     */
    public static boolean gallusCall(GrimoireBook b, ServerPlayer p) {
        UniteSlot slot = UniteState.slot(p);
        long now = p.level().getGameTime();
        if (slot.running(now) != UniteKind.GALLUS) { GrimoireBook.fail(p, "You must be united with Gallus to call."); return false; }
        var d = p.getPersistentData();
        int used = d.getLong(CALL_FOR) == slot.until() ? d.getInt(CALL_N) : 0;
        if (used >= 2) { GrimoireBook.fail(p, "Walgner has called twice; the call is spent until you unite again."); return false; }
        ServerLevel sl = p.serverLevel();
        if (used == 0) {
            List<LivingEntity> foes = GrimoireBook.around(p, p.position(), CallMath.CALL_RADIUS);
            if (foes.isEmpty()) { GrimoireBook.fail(p, "No one is within earshot."); return false; }
            if (!GrimoireBook.control(p)) return false;
            b.castCircle(p, 1.4f);
            VfxSpawn.sendFollowing(sl, VfxShape.SHADOW_UNITE, p, p.position().add(0, 1.4, 0), 0, 24, 1.6f);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.9f, 1.7f);
            for (LivingEntity t : foes) {
                int ticks = BalanceLaw.controlTicks(t, CallMath.stunTicks(t.distanceTo(p)));
                stun(t, ticks, true);
                Vec3 at = t.position();
                VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, Math.max(10, ticks), 0.7f);
            }
        } else {
            LivingEntity t = GrimoireBook.target(p, 18);
            if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one in your sight to hold."); return false; }
            if (!GrimoireBook.control(p)) return false;
            b.castCircle(p, 1f);
            int ticks = BalanceLaw.controlTicks(t, CallMath.ROOT_TICKS);
            stun(t, ticks, false);
            Vec3 at = t.position();
            VfxSpawn.send(sl, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), SHADOW, Math.max(10, ticks), 1.1f);
            sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.9f);
        }
        d.putLong(CALL_FOR, slot.until());
        d.putInt(CALL_N, used + 1);
        return true;
    }

    /** Holds a target still: Slowness X (cannot walk), and for a stun also Weakness so it can not strike back hard. Mobs drop their path. */
    private static void stun(LivingEntity t, int ticks, boolean weaken) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        if (weaken) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        if (t instanceof Mob m) m.getNavigation().stop();
    }

    // ---------------------------------------------------------------- Felis: stealth
    /** Mobs further than the loner range (8 blocks) that hunt the wearer lose them. Bosses keep their target. Call about every 10 ticks. */
    public static void loner(ServerPlayer p, UniteKind kind) {
        if (kind.lonerRange <= 0) return;
        double far = (double) kind.lonerRange;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), x -> x.getTarget() == p)) {
            if (BalanceLaw.isBoss(m)) continue;
            if (kind.mobLosesTarget(m.distanceTo(p)) && m.distanceTo(p) > far) m.setTarget(null);
        }
    }

    /** Night vision for the wearer, topped up while the mode runs. */
    public static void nightEyes(ServerPlayer p) {
        MobEffectInstance have = p.getEffect(MobEffects.NIGHT_VISION);
        if (have == null || have.getDuration() < 240) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false));
    }

    /** Takes the night eyes off at the end if only the mode's own short refill is left. */
    public static void endNightEyes(ServerPlayer p) {
        MobEffectInstance have = p.getEffect(MobEffects.NIGHT_VISION);
        if (have != null && have.getDuration() <= 330 && have.isAmbient()) p.removeEffect(MobEffects.NIGHT_VISION);
    }
}
