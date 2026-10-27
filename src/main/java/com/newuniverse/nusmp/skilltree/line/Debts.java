package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why (3.26.0, the Evolution skills): "Debt" is the shared mechanic of the ten Evolution skills, and this is its game-facing half. The passive {@code debt_store} puts a share of the damage the player deals on the
 * foe that took it, the passive's beat ticks that Debt down (a small, capped bleed), and the art {@code debt_burst} later takes all the Debt on the foes around the player and turns it into one blow each. The
 * numbers and the bounds (at most 20 foes per player, a cap of a share of the foe's maximum health, every entry expiring, a tick never more than a quarter of what is owed) are in the pure {@link DebtLedger},
 * which is also where the memory lives: ONE ledger keyed by the owner's UUID and then the foe's UUID, never a static "current player", in memory only, forgotten when the foe dies, when the owner logs out, dies
 * or changes dimension, and when the server stops ({@link LineEvents} calls the cleanup methods here). The ledger is server-thread only. When the server setting {@code evolutionSkills} is off nothing is stored
 * and a Debt art is a plain blow. Worst case per player per half-second beat: {@link #MAX_TICKS_PER_BEAT} Debt hits (each at most 5 % of the foe's maximum health) and at most {@link #MAX_FX_PER_BEAT} effect
 * packets, through the global effect budget; a burst hits at most 16 foes once.
 */
public final class Debts {
    private Debts() {}

    private static final PerfProbe.Section PROBE = PerfProbe.section("s.line.debt");
    private static final DebtLedger LEDGER = new DebtLedger();
    /** At most this many Debt ticks are dealt for one player in one beat (half a second). */
    public static final int MAX_TICKS_PER_BEAT = 4;
    /** At most this many effect shapes are drawn for the ticks of one player in one beat. */
    public static final int MAX_FX_PER_BEAT = 2;
    /** A burst reaches at most this many foes (the same bound as every art). */
    public static final int MAX_TARGETS = 16;
    /** A foe farther than this from its owner stops carrying Debt when its tick falls due (the tick is not dealt). */
    public static final double TICK_RANGE = 48.0;

    /** The ledger, for the self-test and the report. Server thread only. */
    public static DebtLedger ledger() { return LEDGER; }

    // ================================================================== storing (the passive debt_store)
    /** A blow of the player landed for {@code dealt} damage on {@code foe}: a share of it is stored as Debt on the foe. */
    public static void store(PassiveCtx c, LivingEntity foe, float dealt) {
        if (!EvolutionConfig.enabled() || !(dealt > 0f) || foe == null || foe == c.p || !foe.isAlive() || foe.isAlliedTo(c.p)) return;
        if (foe instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return;               // never on somebody's pet
        long t0 = PerfProbe.begin();
        try {
            double maxPct = Math.min(1.0, c.num("max_pct"));
            long hold = Math.max(4, c.whole("hold_seconds")) * 20L;
            int tick = (int) Math.round(Math.max(0.0, c.num("tick_seconds")) * 20.0);
            LEDGER.store(c.p.getUUID(), foe.getUUID(), dealt, c.num("store_pct"), maxPct, foe.getMaxHealth(), c.now, hold, tick);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    // ================================================================== ticking (the beat of debt_store)
    /** The beat of an active {@code debt_store}: deals the Debt ticks that are due, at most {@link #MAX_TICKS_PER_BEAT}. */
    public static void tickFor(PassiveCtx c) {
        if (!EvolutionConfig.enabled()) return;
        UUID owner = c.p.getUUID();
        if (!LEDGER.hasOwner(owner)) return;
        long t0 = PerfProbe.begin();
        try {
            List<DebtLedger.Tick> due = LEDGER.due(owner, c.now, MAX_TICKS_PER_BEAT);
            int fx = 0;
            for (DebtLedger.Tick t : due) {
                Entity e = c.p.serverLevel().getEntity(t.foe());
                if (!(e instanceof LivingEntity foe) || !foe.isAlive() || foe.distanceToSqr(c.p) > TICK_RANGE * TICK_RANGE) {
                    LEDGER.forget(owner, t.foe());
                    continue;
                }
                float amount = (float) t.amount();
                if (foe instanceof Player) amount = BalanceLaw.cap(foe, amount);
                if (LineEvents.debtHurt(c.p, foe, amount) && fx++ < MAX_FX_PER_BEAT) {
                    Vec3 at = ActiveCtx.centre(foe);
                    LineFx.shape(c.p.serverLevel(), LineLookups.shape(c.str("vfx", ""), com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE), c.route, at, at, 10, 0.7f);
                }
            }
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    // ================================================================== the burst (the art debt_burst)
    /** True when at least one foe is within the art's radius (a cast with nobody around is refused before it costs anything). */
    public static boolean canBurst(ActiveCtx c) { return !AbilityKit.around(c.p, c.p.position(), c.num("radius")).isEmpty(); }

    /**
     * Consumes the Debt on up to {@code targets} foes within {@code radius} (those with Debt first, then the nearest) and hits each ONCE for {@code damage} through the balance law plus
     * {@code mult} times what it owed (never more than the foe's maximum health, a quarter of it for a boss; a player target is held to the player hit cap). The Debt is taken only when the blow landed.
     */
    public static void burst(ActiveCtx c) {
        long t0 = PerfProbe.begin();
        try {
            ServerPlayer p = c.p;
            UUID owner = p.getUUID();
            long now = c.world().getGameTime();
            boolean on = EvolutionConfig.enabled();
            Vec3 centre = p.position();
            double radius = c.num("radius");
            List<LivingEntity> sorted = LineGeo.nearest(AbilityKit.around(p, centre, radius), centre, MAX_TARGETS * 4);
            List<LivingEntity> picked = DebtLedger.prefer(sorted, e -> on && LEDGER.has(owner, e.getUUID(), now), Math.min(c.whole("targets"), MAX_TARGETS));
            c.fx(centre.add(0, 0.2, 0), centre.add(0, 0.2, 0), 20, (float) radius);   // 3.29.0: 20 ticks, the length of the unique Ultimate effects
            int fx = 3;
            for (LivingEntity t : picked) {
                double owed = on ? LEDGER.debtOf(owner, t.getUUID(), now) : 0.0;
                float total = (float) (BalanceLaw.damage(p, t, (float) c.num("damage"), c.mastery) + DebtLedger.burstExtra(owed, c.num("mult"), t.getMaxHealth(), BalanceLaw.isBoss(t)));
                if (t instanceof Player) total = BalanceLaw.cap(t, total);
                if (!LineEvents.debtHurt(p, t, total)) continue;
                if (owed > 0) LEDGER.consume(owner, t.getUUID(), now);
                if (fx-- > 0) c.rukh(ActiveCtx.centre(t), 5);
            }
            c.rukh(p.position().add(0, p.getBbHeight() * 0.6, 0), 8);
            AbilityKit.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.3f);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    // ================================================================== cleanup
    /** Drops this player's expired Debt (and the entry of the player when none is left). One map lookup when the player has none. */
    public static void prune(ServerPlayer p) {
        if (LEDGER.hasOwner(p.getUUID())) LEDGER.prune(p.getUUID(), p.serverLevel().getGameTime());
    }

    /** A creature died: nobody owes through it any more, and if it was a player they are forgotten as an owner too. */
    public static void onDeath(LivingEntity dead) {
        if (LEDGER.isEmpty()) return;
        if (dead instanceof ServerPlayer sp) LEDGER.forgetOwner(sp.getUUID());
        LEDGER.forgetFoe(dead.getUUID());
    }

    /** The player logged out or changed dimension. */
    public static void forgetOwner(ServerPlayer p) { LEDGER.forgetOwner(p.getUUID()); }

    /** The server is stopping. */
    public static void clear() { LEDGER.clear(); }
}
