package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentEffects;
import com.newuniverse.nusmp.entity.veldanava.skills.TemporalDebt;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Move;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.56.0: what the active modes of the Dragon Ball skills DO, one method per {@link DbRules.Prim}. Every press is bounded (rule 11): at most {@link DbRules#MAX_TARGETS} creatures hit (the shared
 * target helpers cap at 24), at most {@link DbRules#MAX_PROJECTILES} projectiles removed, one entity query per area, one beam or ring of effect. Damage goes through {@link SkillDamage} (the Balance Law and
 * the boss cap); control goes through {@link BalanceLaw#controlTicks}. Server thread only. Terrain is never touched.
 */
final class DbMoves {
    private DbMoves() {}

    /** Runs the move of a mode; false when it did nothing (the caller gives the cost back and starts no cooldown). */
    static boolean cast(Spec spec, boolean ult, Mode mode, ServerPlayer p, double mastery) {
        Move mv = mode.move();
        double dmg = DbRules.damage(spec, mv, ult);
        ServerLevel level = p.serverLevel();
        boolean done = switch (mv.prim()) {
            case BEAM -> beam(level, p, mv, dmg, mastery);
            case BLAST -> blast(level, p, mv, dmg, mastery);
            case STOP -> stop(level, p, mv, dmg, mastery);
            case RUSH -> rush(level, p, mv, dmg, mastery);
            case GUARD -> guard(level, p, mv, ult);
            case BARRAGE -> barrage(level, p, mv, dmg, mastery);
            case DISC -> disc(level, p, mv, dmg, mastery);
            case SEAL -> seal(level, p, mv);
            case VEIL -> veil(level, p, mv, dmg, mastery);
            case SCAN -> scan(level, p, mv);
            case CAPSULE -> capsule(level, p, mv, ult);
            case PSYCHIC -> psychic(level, p, mv, dmg, mastery);
            case DETONATE -> detonate(level, p, mv, dmg, mastery);
            default -> false;
        };
        if (done && mv.has(DbRules.SELF_COST) && mv.prim() != DbRules.Prim.DETONATE) p.setHealth(Math.max(2f, p.getHealth() - p.getMaxHealth() * 0.25f));
        if (done) SkillFx.sound(level, p.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.7f, 1.4f);
        return done;
    }

    // ---------------------------------------------------------------- the hit and what it leaves behind
    /** One blow plus the status the move's flags ask for. Returns whether it landed. */
    private static boolean hit(ServerPlayer p, LivingEntity t, double raw, double mastery, Move mv) {
        if (mv.has(DbRules.IGNORE_FRAMES)) t.invulnerableTime = 0;
        float dealt = raw > 0 ? SkillDamage.hit(p, t, raw, mastery, mv.has(DbRules.BYPASS) ? SkillDamage.Kind.MAGIC : SkillDamage.Kind.ATTACK) : 0f;
        status(p, t, mv);
        if (mv.has(DbRules.DEBT_BLAST) && t.isAlive()) {
            double extra = TemporalDebt.detonate(t, 1.0, 0.0, Math.max(raw, 1.0) * 2.0);
            if (extra > 0) SkillDamage.hit(p, t, extra, mastery, SkillDamage.Kind.MAGIC);
        }
        return dealt > 0f || raw <= 0;
    }

    private static void status(ServerPlayer p, LivingEntity t, Move mv) {
        if (!t.isAlive()) return;
        if (mv.debt() > 0) TemporalDebt.add(t, mv.debt());
        int lock = Math.max(20, DbRules.lock(mv));
        if (mv.has(DbRules.STRIP)) strip(t, 6);
        if (mv.has(DbRules.PARALYZE)) t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, lock), 0, false, true, true));
        if (mv.has(DbRules.STUN)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, lock), 2, false, true, true));
            t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(t, lock), 0, false, true, true));
        }
        if (mv.has(DbRules.REJECT)) t.addEffect(new MobEffectInstance(JudgmentEffects.FUTURE_REJECTION, BalanceLaw.controlTicks(t, lock + 20), 0, false, true, true));
        if (mv.has(DbRules.STUTTER)) t.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, BalanceLaw.controlTicks(t, 40), 0, false, true, true));
        if (mv.has(DbRules.BLIND)) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 60), 0, false, true, true));
        if (mv.has(DbRules.SILENCE)) t.addEffect(new MobEffectInstance(JudgmentEffects.EVENT_SILENCE, BalanceLaw.controlTicks(t, lock), 0, false, true, true));
        if (mv.has(DbRules.BLEED)) t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true, true));
        if (mv.has(DbRules.ARMOR_BREAK)) t.addEffect(new MobEffectInstance(JudgmentEffects.ARMOR_BREAK, 200, 0, false, true, true));
        if (mv.has(DbRules.BURY)) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, BalanceLaw.controlTicks(t, lock), 0, false, true, true));
        if (mv.has(DbRules.SNAP)) knock(p, t, 1.1);
    }

    private static void knock(ServerPlayer p, LivingEntity t, double power) {
        Vec3 away = t.position().subtract(p.position());
        if (away.lengthSqr() < 1.0E-4) return;
        Vec3 d = away.normalize().scale(power).add(0, 0.25, 0);
        t.setDeltaMovement(t.getDeltaMovement().add(d));
        t.hurtMarked = true;
    }

    private static void strip(LivingEntity t, int max) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
            if (n >= max) break;
            if (e.getEffect().value().isBeneficial() && t.removeEffect(e.getEffect())) n++;
        }
    }

    /** Removes the projectiles (not the caster's own) inside the box, at most {@link DbRules#MAX_PROJECTILES}; falling blocks are stopped. */
    private static void clearProjectiles(ServerLevel level, ServerPlayer p, AABB box) {
        int n = 0;
        for (Entity e : level.getEntities(p, box, x -> x instanceof Projectile || x instanceof FallingBlockEntity)) {
            if (n++ >= DbRules.MAX_PROJECTILES) break;
            if (e instanceof Projectile pr) { if (pr.getOwner() != p) pr.discard(); }
            else e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static List<LivingEntity> cap(List<LivingEntity> l, int n) {
        int m = Math.min(Math.max(1, n), DbRules.MAX_TARGETS);
        return l.size() > m ? new ArrayList<>(l.subList(0, m)) : l;
    }

    // ---------------------------------------------------------------- the primitives
    private static boolean beam(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, mv.range());
        SkillFx.beam(level, from, to, false);
        SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, from, to, mv.rgb(), 20, (float) Math.min(1.6, 0.7 + mv.radius() * 0.2));
        if (mv.has(DbRules.FREEZE_PROJ)) clearProjectiles(level, p, new AABB(from, to).inflate(mv.radius() + 2));
        for (LivingEntity t : cap(SkillTargets.foesAlong(p, from, to, mv.radius()), mv.n())) hit(p, t, dmg, mastery, mv);
        return true;
    }

    private static boolean blast(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        Vec3 c = mv.range() > 0 ? DMUtil.lookPoint(p, mv.range()) : p.position().add(0, 0.2, 0);
        SkillFx.ring(level, c.add(0, 0.2, 0), mv.radius(), mv.rgb() & 0xFFFFFF);
        SkillFx.burst(level, c.add(0, 1, 0), false, 18, Math.min(2.5, mv.radius() * 0.2));
        if (mv.has(DbRules.FREEZE_PROJ)) clearProjectiles(level, p, new AABB(c, c).inflate(mv.radius()));
        for (LivingEntity t : cap(SkillTargets.foes(p, c, mv.radius()), mv.n())) hit(p, t, dmg, mastery, mv);
        return true;
    }

    private static boolean stop(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, mv.range());
        SkillFx.beam(level, from, to, false);
        SkillFx.ring(level, to, mv.radius(), mv.rgb() & 0xFFFFFF);
        clearProjectiles(level, p, new AABB(from, to).inflate(mv.radius() + 1));
        for (LivingEntity t : cap(SkillTargets.foesAlong(p, from, to, mv.radius()), mv.n())) hit(p, t, dmg, mastery, mv);
        return true;
    }

    private static boolean rush(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        LivingEntity t = SkillTargets.lookFoe(p, mv.range());
        if (t == null) {
            List<LivingEntity> near = SkillTargets.foes(p, p.position(), Math.min(mv.range(), 12));
            if (!near.isEmpty()) t = near.get(0);
        }
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("db.no_target", "No target in reach.")); return false; }
        Vec3 dir = t.position().subtract(p.position());
        Vec3 stand = dir.lengthSqr() < 4 ? p.position() : t.position().subtract(dir.normalize().scale(1.6));
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 8, 0.3);
        if (stand != p.position()) p.teleportTo(stand.x, t.getY(), stand.z);
        int hits = Math.max(1, mv.n());
        for (int k = 0; k < hits && t.isAlive(); k++) {
            t.invulnerableTime = 0;
            SkillDamage.hit(p, t, dmg, mastery, mv.has(DbRules.BYPASS) ? SkillDamage.Kind.MAGIC : SkillDamage.Kind.ATTACK);
        }
        status(p, t, mv);
        if (mv.has(DbRules.DEBT_BLAST) && t.isAlive()) { double extra = TemporalDebt.detonate(t, 1.0, 0.0, dmg * 3.0); if (extra > 0) SkillDamage.hit(p, t, extra, mastery, SkillDamage.Kind.MAGIC); }
        SkillFx.burst(level, t.position().add(0, t.getBbHeight() * 0.5, 0), false, 14, 0.5);
        return true;
    }

    private static boolean guard(ServerLevel level, ServerPlayer p, Move mv, boolean ult) {
        int ticks = Math.max(40, mv.lock());
        int amp = ult ? 4 : 2;
        List<LivingEntity> who = new ArrayList<>(List.of(p));
        if (mv.radius() > 0) for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), mv.radius())) if (a != p && who.size() < DbRules.MAX_TARGETS) who.add(a);
        for (LivingEntity a : who) {
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, amp, false, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, ult ? 2 : 1, false, true, true));
            if (mv.has(DbRules.HEAL)) a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, false, true, true));
        }
        p.getPersistentData().putLong(DbPassives.GUARD_UNTIL, level.getGameTime() + ticks);
        p.getPersistentData().putBoolean(DbPassives.GUARD_PROJ, mv.has(DbRules.FREEZE_PROJ));
        if (mv.has(DbRules.FREEZE_PROJ)) clearProjectiles(level, p, p.getBoundingBox().inflate(mv.radius() + 3));
        SkillFx.aura(level, p, false);
        SkillFx.ring(level, p.position().add(0, 0.2, 0), 2.5, mv.rgb() & 0xFFFFFF);
        return true;
    }

    private static boolean barrage(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        LivingEntity t = SkillTargets.lookFoe(p, mv.range());
        if (t == null) { List<LivingEntity> near = SkillTargets.foes(p, p.position(), Math.min(mv.range(), 10)); if (!near.isEmpty()) t = near.get(0); }
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("db.no_target", "No target in reach.")); return false; }
        t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, Math.max(20, DbRules.lock(mv))), 0, false, true, true));
        for (int k = 0; k < Math.max(1, mv.n()) && t.isAlive(); k++) {
            t.invulnerableTime = 0;
            SkillDamage.hit(p, t, dmg, mastery, SkillDamage.Kind.ATTACK);
        }
        status(p, t, mv);
        SkillFx.beam(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), false);
        SkillFx.burst(level, t.position().add(0, t.getBbHeight() * 0.5, 0), false, 18, 0.6);
        return true;
    }

    private static boolean disc(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        LivingEntity t = mv.has(DbRules.HOMING) ? nearestFoe(p, mv.range()) : SkillTargets.lookFoe(p, mv.range());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("db.no_target", "No target in reach.")); return false; }
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        SkillFx.beam(level, p.getEyePosition(), at, false);
        for (int k = 0; k < Math.max(1, mv.n()) && t.isAlive(); k++) {
            t.invulnerableTime = 0;
            SkillDamage.hit(p, t, dmg, mastery, SkillDamage.Kind.MAGIC);
        }
        status(p, t, mv);
        SkillFx.burst(level, at, false, 14, 0.5);
        return true;
    }

    private static LivingEntity nearestFoe(ServerPlayer p, double range) {
        LivingEntity look = SkillTargets.lookFoe(p, range);
        if (look != null) return look;
        List<LivingEntity> near = SkillTargets.foes(p, p.position(), Math.min(range, 32));
        return near.isEmpty() ? null : near.get(0);
    }

    private static boolean seal(ServerLevel level, ServerPlayer p, Move mv) {
        LivingEntity t = SkillTargets.lookFoe(p, mv.range());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("db.no_target", "No target in reach.")); return false; }
        t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, Math.max(20, DbRules.lock(mv))), 0, false, true, true));
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 60), 0, false, true, true));
        status(p, t, mv);
        p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 6));
        SkillFx.beam(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), false);
        SkillFx.ring(level, t.position(), 1.6, mv.rgb() & 0xFFFFFF);
        return true;
    }

    private static boolean veil(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        long until = level.getGameTime() + 80;
        p.getPersistentData().putLong(DbPassives.VEIL_UNTIL, until);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, true, true));
        p.getPersistentData().putDouble(DbPassives.STORED, Math.min(DbRules.RAGE_CAP, dmg));
        for (LivingEntity t : cap(SkillTargets.foes(p, p.position(), mv.range()), 12)) status(p, t, new Move(mv.prim(), 0, 0, 0, 0, mv.debt(), 40, DbRules.BLIND | DbRules.STUTTER, mv.rgb()));
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 20, 0.8);
        return true;
    }

    private static boolean scan(ServerLevel level, ServerPlayer p, Move mv) {
        int count = 0, debt = 0;
        for (LivingEntity t : cap(SkillTargets.foes(p, p.position(), mv.radius()), mv.n())) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
            debt += TemporalDebt.get(t);
            status(p, t, mv);
            count++;
        }
        SkillFx.ring(level, p.position().add(0, 0.2, 0), Math.min(mv.radius(), 24), mv.rgb() & 0xFFFFFF);
        SkillUtil.actionbar(p, SkillMsg.t("db.scan", "Scan: %s foes marked, %s Temporal Debt.", count, debt).withStyle(ChatFormatting.AQUA));
        return true;
    }

    private static boolean capsule(ServerLevel level, ServerPlayer p, Move mv, boolean ult) {
        List<LivingEntity> who = new ArrayList<>(List.of(p));
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), mv.radius())) if (a != p && who.size() < DbRules.MAX_TARGETS) who.add(a);
        int roll = p.getRandom().nextInt(5);
        String what;
        for (LivingEntity a : who) {
            switch (roll) {
                case 0 -> a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, ult ? 4 : 2));
                case 1 -> a.heal(a.getMaxHealth() * (ult ? 0.5f : 0.25f));
                case 2 -> a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2));
                case 3 -> { a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2)); a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 2)); }
                default -> a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 2));
            }
        }
        what = new String[] {"a Shield Capsule", "a Medical Capsule", "a Regeneration Capsule", "a Speed Capsule", "a Barrier Capsule"}[roll];
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 16, 0.8);
        SkillUtil.actionbar(p, SkillMsg.t("db.capsule", "Capsule Toss: %s (%s helped).", what, who.size()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static boolean psychic(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        LivingEntity t = SkillTargets.lookFoe(p, mv.range());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("db.no_target", "No target in reach.")); return false; }
        SkillFx.beam(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), false);
        if (dmg <= 0) {
            t.setDeltaMovement(t.getDeltaMovement().multiply(0, 0, 0).add(0, 0.15, 0));
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, Math.max(20, DbRules.lock(mv))), 0, false, true, true));
            t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(t, Math.max(20, DbRules.lock(mv))), 0, false, true, true));
            status(p, t, mv);
            return true;
        }
        for (int k = 0; k < Math.max(1, mv.n()) && t.isAlive(); k++) { t.invulnerableTime = 0; SkillDamage.hit(p, t, dmg, mastery, SkillDamage.Kind.ATTACK); }
        status(p, t, mv);
        SkillFx.burst(level, t.position().add(0, 1, 0), false, 20, 0.7);
        return true;
    }

    /** Farewell, Mr. Tien: the user's own health becomes the blast (never past one heart, so no suicide) and leaves them weakened. */
    private static boolean detonate(ServerLevel level, ServerPlayer p, Move mv, double dmg, double mastery) {
        double raw = dmg + Math.max(0f, p.getHealth() - 1f) * 2.0;
        p.setHealth(1f);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 2, false, true, true));
        SkillFx.ring(level, p.position().add(0, 0.2, 0), mv.radius(), mv.rgb() & 0xFFFFFF);
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 30, 1.2);
        for (LivingEntity t : cap(SkillTargets.foes(p, p.position(), mv.radius()), mv.n())) hit(p, t, raw, mastery, mv);
        return true;
    }
}
