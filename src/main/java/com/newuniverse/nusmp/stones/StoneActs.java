package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the twelve slot abilities (two per stone) of the Infinity Stone slot, one small body each. They run only after {@link StoneKit#refuse} has checked the slot, the gate and the setting, and after the
 * relic engine has paid the cost and set the cooldown. Every one is bounded: at most 24 targets (the {@code Fx} cap), at most 12 moved, one zone per bearer, control times through the
 * {@link BalanceLaw} (1 s boss, 3 s player, 5 s mob, shared 12 s control cooldown), healing under half a bar per cast, damage through the BalanceLaw as a GOD-class hit (players capped at 30%), nothing is
 * spawned. NO MIND CONTROL anywhere (owner decision): the Mind Stone senses, strikes and shields; it never turns a will. Numbers are design values (docs/infinity_stones_phase_a.md).
 */
final class StoneActs {
    private StoneActs() {}

    private static final int MOVE_CAP = 12;

    private static double scale(ActCtx c) { return StoneSlot.scale(c.player()); }

    /** The nearest foe inside a narrow cone of the player's look (the foes list is nearest first and capped at 24). */
    private static LivingEntity aim(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : Fx.foesAround(p, range)) {
            Vec3 to = e.getEyePosition().subtract(eye);
            if (to.lengthSqr() < 1.0e-4) continue;
            if (look.dot(to.normalize()) > 0.9) return e;
        }
        return null;
    }

    /** A stone hit: through the balance law as a GOD-class hit (total scale capped at x36, not the unclassified x72), a player never loses more than 30% of their health, never a creative or spectator one. */
    private static void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == null || t == p || !t.isAlive() || t.isAlliedTo(p)) return;
        if (t instanceof Player o && (o.isCreative() || o.isSpectator())) return;
        float dmg = BalanceLaw.damage(p, t, raw, 0.6, UltimateClass.GOD);
        if (t instanceof Player) dmg = (float) Math.min(dmg, t.getMaxHealth() * 0.30);
        t.hurt(p.damageSources().indirectMagic(p, p), Math.min(dmg, 400f));
    }

    private static void flash(ServerPlayer p, InfinityStone s, Vec3 at, double radius) {
        ServerLevel l = p.serverLevel();
        Fx.ring(l, at, radius, s.rgb());
        Fx.burst(l, at, s.rgb());
    }

    // ================================================================ Space (blue)
    /** Portal Step: a blink along the look (the Fx blink: at most 32 blocks, loaded chunks, a safe place to land). */
    static void portalStep(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 from = p.position();
        if (!Fx.teleportLook(p, 32.0)) {
            Fx.say(p, "No safe place to step to in sight.");
            return;
        }
        flash(p, InfinityStone.SPACE, from, 1.5);
        flash(p, InfinityStone.SPACE, p.position(), 1.5);
    }

    /** Spatial Fold: foes within sixteen blocks are folded in to a point in front of you; sneak and they are pushed away. Never players, never bosses, at most twelve. */
    static void spatialFold(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 ahead = p.position().add((flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize()).scale(3.0));
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 16.0)) {
            if (n >= MOVE_CAP) break;
            if (t instanceof Player || BalanceLaw.isBoss(t)) continue;
            Vec3 d = c.sneaking() ? t.position().subtract(p.position()) : ahead.subtract(t.position());
            if (d.lengthSqr() < 1.0e-4) continue;
            t.setDeltaMovement(d.normalize().scale(Math.min(1.8, 0.6 + d.length() * 0.12)).add(0, 0.25, 0));
            t.hurtMarked = true;
            n++;
        }
        flash(p, InfinityStone.SPACE, p.position(), 6.0);
        Fx.say(p, n == 0 ? "Space folds around nothing." : "Space folds around " + n + (n == 1 ? " foe." : " foes."));
    }

    // ================================================================ Mind (yellow): senses and strikes, never controls
    /** Psionic Lance (replaces the removed Dominion): a bolt of thought at the foe in sight; it hurts through the balance law and leaves the target dazed for a moment. No control, no ally. */
    static void psionicLance(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = aim(p, 24.0);
        if (t == null) {
            Fx.say(p, "The Lance finds no mind in your sight.");
            return;
        }
        hit(p, t, (float) (14.0 * scale(c)));
        if (BalanceLaw.beginControl(p)) Fx.addEffect(t, "minecraft:nausea", BalanceLaw.controlTicks(t, 60), 0);
        Fx.addEffect(t, "minecraft:glowing", 100, 0);
        flash(p, InfinityStone.MIND, t.position(), 2.0);
    }

    /** Psionic Reach: read the minds within forty-eight blocks (health and EP of the ten nearest) and show them for ten seconds. Telepathy only: nothing is moved, held or ordered. */
    static void psionicReach(ActCtx c) {
        ServerPlayer p = c.player();
        List<LivingEntity> near = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(48.0),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isInvisible()));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int shown = 0;
        for (LivingEntity e : near) {
            if (shown >= 10) break;
            Fx.addEffect(e, "minecraft:glowing", 200, 0);
            if (shown < 6) {
                p.sendSystemMessage(Component.literal(e.getName().getString() + "  health " + Math.round(e.getHealth()) + "/" + Math.round(e.getMaxHealth())
                        + "  EP " + short_(TensuraBridge.maxEp(e)) + "  " + Math.round(Math.sqrt(e.distanceToSqr(p))) + " m"));
            }
            shown++;
        }
        flash(p, InfinityStone.MIND, p.position(), 4.0);
        Fx.say(p, shown == 0 ? "No mind answers within forty-eight blocks." : "You hear " + shown + (shown == 1 ? " mind." : " minds."));
    }

    static String short_(double v) {
        if (v >= 1.0e12) return String.format("%.1fT", v / 1.0e12);
        if (v >= 1.0e9) return String.format("%.1fB", v / 1.0e9);
        if (v >= 1.0e6) return String.format("%.1fM", v / 1.0e6);
        if (v >= 1.0e3) return String.format("%.1fK", v / 1.0e3);
        return String.valueOf(Math.round(v));
    }

    // ================================================================ Reality (red)
    /** Rewrite: the attacks of a mob in sight are rewritten to nothing for eight seconds (a player three); never a boss. Non-lethal and temporary. */
    static void rewrite(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = aim(p, 16.0);
        if (t == null || BalanceLaw.isBoss(t)) {
            Fx.say(p, t == null ? "There is nothing in sight to rewrite." : "A boss cannot be rewritten.");
            return;
        }
        int ticks = t instanceof Player ? BalanceLaw.controlTicks(t, 60) : 160;
        Fx.addEffect(t, "minecraft:weakness", ticks, 9);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
        flash(p, InfinityStone.REALITY, t.position(), 2.0);
        Fx.say(p, t.getName().getString() + " is rewritten: its blows do nothing for a moment.");
    }

    /** False World: you turn unseen for twenty seconds and every mob within twenty-four blocks that hunted you loses you. (The decoy clone is Phase B.) */
    static void falseWorld(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.addEffect(p, "minecraft:invisibility", 400, 0);
        int lost = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), m -> m.getTarget() == p)) {
            if (lost >= 24) break;
            m.setTarget(null);
            lost++;
        }
        flash(p, InfinityStone.REALITY, p.position(), 3.0);
        Fx.say(p, "The world forgets you for twenty seconds" + (lost > 0 ? "; " + lost + " hunters lose you." : "."));
    }

    // ================================================================ Power (purple)
    /** Power Surge: a purple wave round you; foes within eight blocks are hurt and thrown back (never a player or a boss thrown), and you take a recoil that can never kill you. */
    static void powerSurge(ActCtx c) {
        ServerPlayer p = c.player();
        float raw = (float) (18.0 * scale(c));
        for (LivingEntity t : Fx.foesAround(p, 8.0)) {
            hit(p, t, raw);
            if (t instanceof Player || BalanceLaw.isBoss(t)) continue;
            Vec3 d = t.position().subtract(p.position());
            if (d.lengthSqr() < 1.0e-4) continue;
            t.setDeltaMovement(d.normalize().scale(1.6).add(0, 0.4, 0));
            t.hurtMarked = true;
        }
        float recoil = StoneRules.recoil(p.getMaxHealth(), p.getHealth(), 0.05);
        if (recoil > 0f) p.setHealth(p.getHealth() - recoil);
        flash(p, InfinityStone.POWER, p.position(), 8.0);
    }

    /** Infinite Might: thirty seconds of Strength III and Resistance II. */
    static void infiniteMight(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.addEffect(p, "minecraft:strength", 600, 2);
        Fx.addEffect(p, "minecraft:resistance", 600, 1);
        flash(p, InfinityStone.POWER, p.position(), 3.0);
        Fx.say(p, "Power fills you for thirty seconds.");
    }

    // ================================================================ Time (green)
    /** Rewind: back to where you stood about five seconds ago, with the health you had then (healing under the balance law). Same dimension, loaded chunks only. */
    static void rewind(ActCtx c) {
        ServerPlayer p = c.player();
        StoneState.Sample s = StoneState.rewindTarget(p.getUUID());
        ServerLevel l = p.serverLevel();
        if (s == null || !s.dim().equals(l.dimension().location().toString()) || !l.isLoaded(BlockPos.containing(s.x(), s.y(), s.z()))) {
            Fx.say(p, "Time has nothing to give back yet.");
            return;
        }
        flash(p, InfinityStone.TIME, p.position(), 2.0);
        p.teleportTo(s.x(), s.y(), s.z());
        p.fallDistance = 0;
        if (s.health() > p.getHealth()) Fx.heal(p, Math.min(s.health() - p.getHealth(), p.getMaxHealth() * 0.5f));
        flash(p, InfinityStone.TIME, p.position(), 2.0);
        Fx.say(p, "Five seconds are given back.");
    }

    /** Time Lock: foes within twelve blocks are held almost still (a frozen look, not a world freeze): a boss for 1 s, a player 3 s, a mob 5 s, as the balance law has it. */
    static void timeLock(ActCtx c) {
        ServerPlayer p = c.player();
        if (!BalanceLaw.beginControl(p)) {
            Fx.say(p, "Time cannot be held again so soon.");
            return;
        }
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 12.0)) {
            int ticks = BalanceLaw.controlTicks(t, StoneRules.lockTicks(BalanceLaw.isBoss(t), t instanceof Player));
            Fx.addEffect(t, "minecraft:slowness", ticks, 6);
            Fx.addEffect(t, "minecraft:weakness", ticks, 2);
            n++;
        }
        flash(p, InfinityStone.TIME, p.position(), 12.0);
        Fx.say(p, n == 0 ? "Time holds nothing." : "Time holds " + n + (n == 1 ? " foe." : " foes."));
    }

    // ================================================================ Soul (orange)
    /** Soul Grasp: the soul of the foe in sight is held for three seconds (it is slowed to a stop and weakened); you read its health and EP. Nothing is killed, nothing is taken: no instant kill, on anyone. */
    static void soulGrasp(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = aim(p, 16.0);
        if (t == null) {
            Fx.say(p, "No soul in sight to grasp.");
            return;
        }
        if (!BalanceLaw.beginControl(p)) {
            Fx.say(p, "Souls cannot be held again so soon.");
            return;
        }
        int ticks = BalanceLaw.controlTicks(t, 60);
        Fx.addEffect(t, "minecraft:slowness", ticks, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks, 4);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
        flash(p, InfinityStone.SOUL, t.position(), 2.0);
        Fx.say(p, t.getName().getString() + ": health " + Math.round(t.getHealth()) + "/" + Math.round(t.getMaxHealth()) + ", EP " + short_(TensuraBridge.maxEp(t)) + ".");
    }

    /** Soulworld: for fifteen seconds you and your allies within eight blocks of this place heal a few percent of their health every second (at most 45% in all, under the half-a-bar law). */
    static void soulworld(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        boolean ok = StoneState.openZone(p.getUUID(), new StoneState.Zone(l.dimension().location().toString(), p.getX(), p.getY(), p.getZ(), l.getGameTime() + 300));
        if (!ok) {
            Fx.say(p, "The Soul Stone is holding too many worlds open on this server; wait a moment.");
            return;
        }
        flash(p, InfinityStone.SOUL, p.position(), 8.0);
        Fx.say(p, "A pocket of soul opens for fifteen seconds.");
    }
}
