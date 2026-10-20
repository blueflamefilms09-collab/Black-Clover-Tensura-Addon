package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.requip.BarrageMath;
import com.newuniverse.nusmp.requip.RequipMagicConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Why: Requip: Barrage, Requip: Strike and the finishers are not one hit but a short animation: swords appear, hang for a beat, fly; a ring closes; petals open. Each is a SCRIPT: a list of steps, each a
 * tick offset from the cast and an action, run by one server tick listener. A script holds the caster, the damage already dealt to each target (so a boss is capped per cast, see
 * {@link BarrageMath#volleyCap}) and nothing else, so it can be tested by running all its steps at once. A script whose caster is gone or dead is dropped; the one thing that must never be left
 * half-done, a thrown weapon, is saved with the player ({@code requip.RequipArmoury.Data#limbo}) and comes home through {@link ErzaRequipMagic#safetyNet}.
 */
final class RequipScripts {
    private RequipScripts() {}

    /** At most this many scripts run at once, and at most this many per caster. */
    static final int MAX_SCRIPTS = 96, MAX_PER_CASTER = 4;

    /** One animation. {@link #at} adds a step; steps with the same tick run in the order added. */
    static final class Script {
        final ServerPlayer caster;
        final String kind;
        final List<int[]> order = new ArrayList<>();            // {tick, index into actions}
        final List<Consumer<ServerPlayer>> actions = new ArrayList<>();
        final Map<UUID, Double> dealt = new HashMap<>();
        long start = -1;
        int next;
        double mastery;
        /** For the tests and the messages: how many swords or hits the script made. */
        int launched, hits;

        Script(ServerPlayer caster, String kind, double mastery) { this.caster = caster; this.kind = kind; this.mastery = mastery; }

        Script at(int tick, Consumer<ServerPlayer> action) {
            int idx = actions.size();
            actions.add(action);
            int pos = order.size();
            while (pos > 0 && order.get(pos - 1)[0] > tick) pos--;
            order.add(pos, new int[]{tick, idx});
            return this;
        }

        int lastTick() { return order.isEmpty() ? 0 : order.get(order.size() - 1)[0]; }

        boolean finished() { return next >= order.size(); }

        /** Runs every step due at or before {@code elapsed} ticks after the cast. */
        void runTo(int elapsed) {
            while (next < order.size() && order.get(next)[0] <= elapsed) {
                Consumer<ServerPlayer> a = actions.get(order.get(next)[1]);
                next++;
                try {
                    a.accept(caster);
                } catch (RuntimeException e) {
                    // a failing step must not stop the animation or the server; the step is skipped
                }
            }
        }

        /** Runs the whole script at once (the self-test). */
        void runAll() { runTo(Integer.MAX_VALUE); }

        // ---------------------------------------------------------------- damage
        /**
         * One hit on one target: the balance law scales the raw damage with the caster's EP and mastery, {@code bypass} (0..1) ignores that share of the target's armour (capped at x3), the whole
         * script may take at most the boss share of a boss's health, and a hit refused (an ally, a creative player) deals nothing. Returns the damage dealt.
         */
        float hit(LivingEntity t, double raw, double bypass) {
            if (t == null || !t.isAlive() || raw <= 0 || !SkillTargets.isAimable(caster, t)) return 0f;
            float dmg = BalanceLaw.damage(caster, t, (float) (raw * NUConfig.DM_DAMAGE_MULT.get()), mastery);
            DamageSource src = SkillDamage.source(caster, SkillDamage.Kind.ATTACK);
            if (bypass > 0) dmg *= Math.min((float) BarrageMath.MAX_BYPASS_FACTOR, EnergyBridge.armourBypass(t, src, dmg, (float) bypass));
            double cap = BarrageMath.volleyCap(t.getMaxHealth(), BalanceLaw.isBoss(t), RequipMagicConfig.bossVolleyShare());
            double so = dealt.getOrDefault(t.getUUID(), 0.0);
            double allowed = BarrageMath.allowed(dmg, so, cap);
            if (allowed <= 0.0) return 0f;
            t.invulnerableTime = 0;                                    // every sword and every pass lands
            float before = t.getHealth();
            hitting = true;                                            // the Weapon Affinity bonus of a melee hit must not be applied again to a hit that already has it
            try {
                t.hurt(src, (float) allowed);
            } finally {
                hitting = false;
            }
            float done = Math.max(0f, before - t.getHealth());
            dealt.merge(t.getUUID(), (double) done, Double::sum);
            hits++;
            return done;
        }
    }

    private static final List<Script> ACTIVE = new ArrayList<>();

    /** True while a script is dealing a hit (read by the skill's damage hook). */
    static boolean hitting;

    /** Starts a script (its first steps run at once). Returns false when there are too many already. */
    static boolean start(MinecraftServer server, Script s) {
        if (ACTIVE.size() >= MAX_SCRIPTS) return false;
        long per = ACTIVE.stream().filter(o -> o.caster == s.caster).count();
        if (per >= MAX_PER_CASTER) return false;
        s.start = server.getTickCount();
        ACTIVE.add(s);
        s.runTo(0);
        return true;
    }

    static int running() { return ACTIVE.size(); }

    static boolean running(ServerPlayer p, String kind) {
        for (Script s : ACTIVE) if (s.caster == p && s.kind.equals(kind)) return true;
        return false;
    }

    static void cancel(ServerPlayer p) { ACTIVE.removeIf(s -> s.caster == p); }

    /** Cancels one kind of script of a caster (pressing Strike again recalls the weapon without stopping a barrage that is running). */
    static void cancel(ServerPlayer p, String kind) { ACTIVE.removeIf(s -> s.caster == p && s.kind.equals(kind)); }

    static void onServerTick(ServerTickEvent.Post e) {
        if (ACTIVE.isEmpty()) return;
        long now = e.getServer().getTickCount();
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Script s = ACTIVE.get(i);
            ServerPlayer c = s.caster;
            if (c.isRemoved() || !c.isAlive() || c.hasDisconnected()) { ACTIVE.remove(i); continue; }
            s.runTo((int) Math.min(Integer.MAX_VALUE, now - s.start));
            if (s.finished()) ACTIVE.remove(i);
        }
    }
}
