package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Why: the Shield's promises outlive the call that makes them. Protect says "for the next eight seconds, part of what happens to your friends happens to me", the
 * Second Shield says "the next blow on you is mine", and the guard remembers its Resolve and Counter from one blow to the next. Those promises need a memory (the
 * per-wielder {@link ShieldState} and the shared {@link CoverBook}) and one door where blows to OTHER people are seen (a game event, because the weapon's own hooks
 * only run for the wielder). This class is that memory and that door, plus the two scheduling helpers every technique uses. Rules it keeps: server thread only; the
 * void and /kill (the damage that bypasses invulnerability) are never rewritten; a redirected blow never takes the wielder under a quarter of their health and
 * never loops; all state is dropped when the wielder leaves, puts the weapon away, or the server stops (a cache, not a save); an exception in the event is caught
 * and logged once, never thrown into the damage code.
 */
final class ShieldRuntime {
    private ShieldRuntime() {}

    private static final Map<UUID, ShieldState> STATES = new HashMap<>();
    private static final CoverBook BOOK = new CoverBook();
    private static boolean registered;
    private static boolean redirecting;

    /** Hooks the events (once; the logic's constructor calls it on both sides of the game). */
    static synchronized void register(IEventBus game) {
        if (registered) return;
        registered = true;
        game.addListener(ShieldRuntime::onIncomingDamage);
        game.addListener(ShieldRuntime::onServerStopped);
        game.addListener(ShieldRuntime::onLogout);
    }

    static CoverBook book() { return BOOK; }

    /** The state of a wielder, made on first use. */
    static ShieldState state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new ShieldState()); }

    /** The state of a wielder if there is one (no allocation). */
    static ShieldState peek(UUID id) { return STATES.get(id); }

    /** Forgets everything of one wielder. */
    static void forget(UUID id) {
        ShieldState s = STATES.remove(id);
        if (s != null) s.endFloating();
        BOOK.forget(id);
    }

    static void clearAll() {
        for (ShieldState s : STATES.values()) s.endFloating();
        STATES.clear();
        BOOK.clear();
        redirecting = false;
    }

    static void onServerStopped(ServerStoppedEvent e) { clearAll(); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forget(e.getEntity().getUUID()); }

    // ------------------------------------------------------------------ scheduling
    /** True while the wielder is still here, in the level the technique began in, and still bound to the Shield: a delayed effect checks it before it acts. */
    static boolean live(HeroCtx c) {
        ServerPlayer p = c.player;
        return !p.isRemoved() && p.isAlive() && p.serverLevel() == c.level && HeroBonds.kindOf(p) == HeroKind.SHIELD;
    }

    /** Runs {@code effect} after {@code ticks} (at once for 0), unless the wielder is gone by then. A throwing effect is logged once and ends, never thrown. */
    static void after(HeroCtx c, int ticks, Runnable effect) {
        if (ticks <= 0) {
            effect.run();
            return;
        }
        SpellRuntime.later(c.level, ticks, () -> {
            if (live(c)) effect.run();
        });
    }

    /** A repeating task of {@code ticks} ticks, one step every {@code interval} ticks, that ends by itself when the wielder is gone. {@code step} gets the step number and returns nothing. */
    static void every(HeroCtx c, int ticks, int interval, IntConsumer step) {
        SpellRuntime.zone(c.level, Math.min(ticks, ShieldNumbers.MAX_TASK_TICKS), Math.max(1, interval), age -> {
            if (live(c)) step.accept(age);
        });
    }

    // ------------------------------------------------------------------ the door where blows to other people are seen
    /**
     * A living thing is about to take damage. If it carries a ward, the ward soaks what it can; if it is covered by a Shield wielder, part of what is left goes to
     * the wielder. The blow is changed in place ({@code setAmount}); it is never cancelled.
     */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            LivingEntity victim = e.getEntity();
            if (redirecting || BOOK.wardCount() + BOOK.coverCount() == 0 || !(victim.level() instanceof ServerLevel level)) return;
            float amount = e.getAmount();
            DamageSource src = e.getSource();
            if (!(amount > 0f) || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            long now = level.getGameTime();
            UUID id = victim.getUUID();

            double soaked = BOOK.soak(id, now, amount);
            if (soaked > 0) {
                amount -= (float) soaked;
                e.setAmount(Math.max(0f, amount));
                ShieldFx.shatter(level, victim.getBoundingBox().getCenter(), victim.position(), 0.6f, ShieldFx.JEWEL);
                ShieldFx.sound(level, victim.position(), SoundEvents.SHIELD_BLOCK, 0.9f, 1.4f);
                if (!(amount > 0f)) return;
            }

            CoverBook.Cover cover = BOOK.coverOf(id, now);
            if (cover == null || victim.getServer() == null) return;
            ServerPlayer w = victim.getServer().getPlayerList().getPlayer(cover.wielder());
            if (w == null || w == victim || !w.isAlive() || w.level() != level || w.distanceToSqr(victim) > ShieldNumbers.COVER_KEEP_RADIUS * ShieldNumbers.COVER_KEEP_RADIUS) return;
            if (HeroBonds.kindOf(w) != HeroKind.SHIELD || !HeroBonds.of(w).summoned()) return;
            if (w.invulnerableTime > 10) return;                                   // the wielder was just hurt: their own frames of grace stop a second redirect
            double take = CoverBook.redirect(amount, cover.share(), w.getHealth(), w.getMaxHealth(), ShieldNumbers.COVER_FLOOR);
            if (take < 0.5) return;
            boolean hurt;
            redirecting = true;
            try {
                hurt = w.hurt(src, (float) take);
            } finally {
                redirecting = false;
            }
            if (!hurt) return;
            e.setAmount(Math.max(0f, amount - (float) take));
            state(w).guard.addCounter(now, CounterMath.gain(take, w.getMaxHealth(), ShieldNumbers.COUNTER_RATE, false));
            Vec3 mid = victim.getBoundingBox().getCenter();
            ShieldFx.tellLine(level, mid, w.getBoundingBox().getCenter(), ShieldFx.TELL_JEWEL);
        } catch (RuntimeException | LinkageError ex) {
            redirecting = false;
            SafeCast.fault("hero.shield.incoming", ex);
        }
    }
}
