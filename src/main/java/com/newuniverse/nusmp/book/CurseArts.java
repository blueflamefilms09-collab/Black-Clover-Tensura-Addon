package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Curse Magic: the spells of CurseBook that need their own logic. A curse is a lasting wrong laid on a body: stacking Hex Marks
 * (each one makes the victim take more damage), poison and wasting over time, a shrunken life pool, and a curse that jumps to
 * whoever stands near when a marked body falls.
 */
public final class CurseArts {
    private CurseArts() {}

    private static final int COLOR = CurseBook.COLOR;
    private static final int MAX_MARKS = 5;
    private static final String MARKS = "nusmp_curse_marks", MARK_UNTIL = "nusmp_curse_until", BY = "nusmp_curse_by";
    private static final String AEGIS = "nusmp_curse_aegis", LASH = "nusmp_curse_lash", HP_UNTIL = "nusmp_curse_hp_until";
    private static final ResourceLocation HP_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "curse_hex_hp");
    private static boolean hooked;

    /** Registers the damage and death hooks (Hex Mark amplification, Hex Aegis, the curse that spreads on death). Called once from CurseProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity v = e.getEntity();
            var d = v.getPersistentData();
            long now = v.level().getGameTime();
            if (d.getLong(MARK_UNTIL) > now) {
                int n = d.getInt(MARKS);
                if (n > 0) e.setAmount(e.getAmount() * (1f + (BalanceLaw.isBoss(v) ? 0.03f : 0.06f) * n));
            }
            if (d.getLong(AEGIS) > now) {
                e.setAmount(e.getAmount() * 0.8f);
                if (e.getSource().getEntity() instanceof LivingEntity att && att != v) lash(att, v);
            }
        });
        AttributeEvents.death(e -> {
            LivingEntity v = e.getEntity();
            if (!(v.level() instanceof ServerLevel sl)) return;
            var d = v.getPersistentData();
            int n = d.getInt(MARKS);
            if (n < 2 || d.getLong(MARK_UNTIL) <= sl.getGameTime()) return;
            UUID by = d.hasUUID(BY) ? d.getUUID(BY) : null;
            Entity owner = by == null ? null : sl.getEntity(by);
            int spread = 0;
            for (LivingEntity o : sl.getEntitiesOfClass(LivingEntity.class, v.getBoundingBox().inflate(6),
                    x -> x != v && x.isAlive() && !x.isSpectator() && !(x instanceof Player pl && pl.isCreative())
                            && x != owner && !(owner != null && x.isAlliedTo(owner)))) {
                if (spread++ >= 6) break;
                setMarks(o, by, n - 1);
                o.addEffect(new MobEffectInstance(MobEffects.WITHER, dur(o, 100), 0));
            }
            if (spread > 0) VfxSpawn.send(sl, VfxShape.CURSE_FX3, v.position(), v.position().add(0, 1, 0), COLOR, 24, 0.9f);
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float scale(ManasSkillInstance i, ServerPlayer p) { return GrimoireBook.size(i, p) * EnergyBridge.scale(p); }

    /** Control-style effects run a third as long on bosses. */
    private static int dur(LivingEntity t, int ticks) { return BalanceLaw.isBoss(t) ? Math.max(20, ticks / 3) : ticks; }

    /** Foes within r of c, nearest first, at most max (creative players are left alone). */
    static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double r, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : GrimoireBook.around(p, c, r)) {
            if (e instanceof Player pl && pl.isCreative()) continue;
            out.add(e);
        }
        out.sort(Comparator.<LivingEntity>comparingDouble(e -> e.distanceToSqr(c)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    /** The caster and its friends within r of c (at most 12). */
    static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? new ArrayList<>(out.subList(0, 12)) : out;
    }

    private static Vec3 flat(Vec3 v) { return v.multiply(1, 0, 1).normalize(); }

    private static Vec3 sideOf(Vec3 dir) {
        Vec3 s = new Vec3(-dir.z, 0, dir.x);
        return s.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : s.normalize();
    }

    // ------------------------------------------------------------------ Hex Marks
    /** Active Hex Marks on t (0 when they ran out). */
    static int marks(LivingEntity t) {
        var d = t.getPersistentData();
        return d.getLong(MARK_UNTIL) > t.level().getGameTime() ? d.getInt(MARKS) : 0;
    }

    /** Adds Hex Marks (at most 5; each lasts 20 s from the last one laid) and remembers who laid them. Returns the new count. */
    static int mark(LivingEntity t, UUID by, int add) {
        return setMarks(t, by, Math.min(MAX_MARKS, marks(t) + add));
    }

    /** Raises the marks on t to at least n (never lowers). */
    static int setMarks(LivingEntity t, UUID by, int n) {
        var d = t.getPersistentData();
        int now = Math.min(MAX_MARKS, Math.max(n, marks(t)));
        d.putInt(MARKS, now);
        d.putLong(MARK_UNTIL, t.level().getGameTime() + 400);
        if (by != null) d.putUUID(BY, by);
        return now;
    }

    private static boolean markedBy(LivingEntity t, UUID by) {
        var d = t.getPersistentData();
        return marks(t) > 0 && d.hasUUID(BY) && d.getUUID(BY).equals(by);
    }

    /** Curse damage plus Hex Marks. */
    private static void curseHit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, LivingEntity t, float raw, int marks) {
        b.hurt(i, p, t, mode, raw);
        if (marks > 0) mark(t, p.getUUID(), marks);
    }

    /** Hex Aegis: whoever strikes the warded gets one mark and a sapped arm (at most once a second). */
    private static void lash(LivingEntity att, LivingEntity ward) {
        long now = att.level().getGameTime();
        var d = att.getPersistentData();
        if (d.getLong(LASH) > now) return;
        d.putLong(LASH, now + 20);
        mark(att, ward.getUUID(), 1);
        att.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(att, 80), 0));
        att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(att, 40), 0));
    }

    /** Shrinks the maximum life of t by frac (stacking up to half, a sixth on bosses) for ticks; it comes back when the curse runs out. */
    static void hexHp(LivingEntity t, ServerLevel level, double frac, int ticks) {
        AttributeInstance a = t.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        boolean boss = BalanceLaw.isBoss(t);
        AttributeModifier old = a.getModifier(HP_ID);
        double cur = old == null ? 0 : -old.amount();
        double next = Math.min(boss ? 0.15 : 0.5, cur + (boss ? frac / 3 : frac));
        a.removeModifier(HP_ID);
        a.addTransientModifier(new AttributeModifier(HP_ID, -next, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (t.getHealth() > t.getMaxHealth()) t.setHealth(t.getMaxHealth());
        t.getPersistentData().putLong(HP_UNTIL, level.getGameTime() + ticks);
        SpellRuntime.later(level, ticks, () -> {
            if (t.getPersistentData().getLong(HP_UNTIL) <= level.getGameTime()) unhexHp(t);
        });
    }

    private static void unhexHp(LivingEntity t) {
        AttributeInstance a = t.getAttribute(Attributes.MAX_HEALTH);
        if (a != null) a.removeModifier(HP_ID);
    }

    /** Lifts the curses from a friend: poison, wither, sapped strength, marks and the shrunken life. */
    private static void cleanse(LivingEntity f) {
        f.removeEffect(MobEffects.POISON);
        f.removeEffect(MobEffects.WITHER);
        f.removeEffect(MobEffects.WEAKNESS);
        f.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        f.getPersistentData().putLong(MARK_UNTIL, 0);
        f.getPersistentData().putLong(HP_UNTIL, 0);
        unhexHp(f);
    }

    // ------------------------------------------------------------------ Aufwachen Dachs: poison badgers
    static boolean dachs(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        Vec3 side = sideOf(dir);
        List<LivingEntity> foes = foes(p, p.position(), 18, 6);
        int n = s > 1.3f ? 5 : 4;
        b.castCircle(p, 0.9f);
        for (int k = 0; k < n; k++) {
            double off = (k - (n - 1) / 2.0) * 0.5;
            Vec3 from = start.add(side.scale(off)).add(0, -0.3, 0);
            LivingEntity home = foes.isEmpty() ? null : foes.get(k % foes.size());
            Vec3 d = dir.add(side.scale(off * 0.15)).normalize();
            b.vfx(p, VfxShape.CURSE_FX1, from, home != null ? home.getBoundingBox().getCenter() : from.add(d.scale(16)), 16, 0.7f);
            SpellRuntime.bolt(p, from, d.scale(0.9), 0.6, 26, false, home, (bolt, t) -> {
                curseHit(b, i, p, mode, t, 3.5f * s, 1);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, dur(t, 100), 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(t, 40), 0));
                Nullification.bleed(t, 0.02);
            }, (bolt, at) -> b.vfx(p, VfxShape.CURSE_FX3, at, at, 14, 0.5f));
        }
        return true;
    }

    // ------------------------------------------------------------------ Withering Hex: spiritual damage and a shrunken life pool
    static boolean witheringHex(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No one to hex."); return false; }
        float s = scale(i, p);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.CURSE_FX1, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), 14, 1.0f);
        b.vfx(p, VfxShape.CURSE_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 22, 0.9f);
        curseHit(b, i, p, mode, t, 5f * s, 2);
        EnergyBridge.spirit(t, BalanceLaw.isBoss(t) ? 1 : 3);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(t, 200), 0));
        hexHp(t, p.serverLevel(), 0.15, 600);
        return true;
    }

    // ------------------------------------------------------------------ Sekke Poison Lizard: a bronze lizard that hunts one foe
    static boolean lizard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 22);
        if (t == null) { GrimoireBook.fail(p, "No one for the lizard to hunt."); return false; }
        float s = scale(i, p);
        Vec3[] at = { p.position().add(flat(p.getViewVector(1f)).scale(1.2)) };
        boolean[] done = { false };
        b.castCircle(p, 0.9f);
        SpellRuntime.zone(p.serverLevel(), 50, 1, age -> {
            if (done[0]) return;
            if (!t.isAlive()) { done[0] = true; return; }
            Vec3 goal = t.position(), d = goal.subtract(at[0]);
            if (d.length() < 1.5) {
                done[0] = true;
                curseHit(b, i, p, mode, t, 9f * s, 2);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(t, 160), 1));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(t, 100), 1));
                t.addEffect(new MobEffectInstance(MobEffects.POISON, dur(t, 120), 1));
                b.vfx(p, VfxShape.CURSE_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 20, 0.9f);
                SpellRuntime.zone(p.serverLevel(), 60, 20, a2 -> { if (t.isAlive()) b.hurt(i, p, t, mode, 1.5f * s); });
                return;
            }
            Vec3 next = at[0].add(d.normalize().scale(0.8));
            if (age % 3 == 0) b.vfx(p, VfxShape.CURSE_FX1, at[0].add(0, 0.4, 0), next.add(0, 0.4, 0), 8, 0.6f);
            at[0] = next;
        });
        return true;
    }

    // ------------------------------------------------------------------ Ash Absorbing Formation: streams that drain mana
    static boolean ashFormation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        double reach = 12 * s;
        if (foes(p, p.position(), reach, 4).isEmpty()) { GrimoireBook.fail(p, "No one for the ash to reach."); return false; }
        b.castCircle(p, 1f);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : foes(p, p.position(), reach, 4)) {
                b.hurt(i, p, t, mode, 1.6f * s);
                double got = EnergyBridge.drain(t, p, 0.02);
                if (got > 0) BalanceLaw.heal(p, 0.6f);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(t, 30), 0));
                if (age % 30 == 0) mark(t, p.getUUID(), 1);
                if (age % 20 == 0) b.vfx(p, VfxShape.CURSE_FX1, p.getEyePosition().add(0, -0.5, 0), t.getBoundingBox().getCenter(), 12, 0.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Joyful Destructive Ash: a cloud, then a second blast
    static boolean joyfulAsh(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        double r = 4.2 * s;
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.CURSE_FX1, start, start.add(dir.scale(1.2 * 18)), 18, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.2), 0.6, 18, false, null, (bolt, t) -> {}, (bolt, at) -> {
            b.vfx(p, VfxShape.CURSE_FX2, at, at.add(0, 1, 0), 30, (float) r);
            b.vfx(p, VfxShape.CURSE_FX3, at, at, 20, 1.0f);
            for (LivingEntity t : foes(p, at, r, 10)) {
                curseHit(b, i, p, mode, t, 5f * s, 1);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur(t, 50), 0));
            }
            SpellRuntime.later(p.serverLevel(), 24, () -> {
                b.vfx(p, VfxShape.CURSE_FX3, at, at.add(0, 1, 0), 26, 1.6f);
                for (LivingEntity t : foes(p, at, r, 10)) {
                    curseHit(b, i, p, mode, t, 11f * s, 1);
                    t.addEffect(new MobEffectInstance(MobEffects.WITHER, dur(t, 60), 1));
                }
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Curse-Worker's Neighbor: a tether that siphons a cursed foe
    static boolean neighbor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No cursed one in sight."); return false; }
        float s = scale(i, p);
        if (marks(t) == 0) mark(t, p.getUUID(), 1);
        b.castCircle(p, 0.9f);
        SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
            if (!t.isAlive() || !p.isAlive() || t.distanceTo(p) > 26) return;
            int m = marks(t);
            double got = EnergyBridge.drain(t, p, 0.02 * (1 + 0.4 * m));
            if (got > 0) {
                for (ServerPlayer ally : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8), a -> a != p && a.isAlliedTo(p)))
                    WikiSpells.giveMana(ally, got * 0.4);
            }
            b.hurt(i, p, t, mode, 1.5f * s);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, dur(t, 40), 0));
            if (age % 40 == 0) mark(t, p.getUUID(), 1);
            if (age % 20 == 0) b.vfx(p, VfxShape.CURSE_FX1, t.getBoundingBox().getCenter(), p.getEyePosition().add(0, -0.4, 0), 14, 0.6f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Hex Aegis: a ward that curses whoever strikes it
    static boolean aegis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        int ticks = 240;
        long until = p.level().getGameTime() + ticks;
        for (LivingEntity f : friends(p, p.position(), 6 * s)) {
            f.getPersistentData().putLong(AEGIS, until);
            cleanse(f);
        }
        b.castCircle(p, 1.1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CURSE_FX2, p, p.position(), COLOR, ticks, 2.5f);
        b.vfx(p, VfxShape.CURSE_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 24, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Cursed Step: a dash that leaves a cursed trail
    static boolean cursedStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        Vec3 from = p.position(), dir = flat(p.getViewVector(1f)), dest = null;
        if (dir.lengthSqr() < 1e-4) { GrimoireBook.fail(p, "Look ahead to step."); return false; }
        for (double d = 9; d >= 2; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to step."); return false; }
        Vec3 end = dest;
        b.castCircle(p, 0.7f);
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), end.add(0, 1, 0), 1.4)) {
            if (t instanceof Player pl && pl.isCreative()) continue;
            curseHit(b, i, p, mode, t, 4f * s, 1);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(t, 60), 1));
        }
        p.teleportTo(end.x, end.y, end.z);
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        b.vfx(p, VfxShape.CURSE_FX1, from.add(0, 1, 0), end.add(0, 1, 0), 14, 1.0f);
        b.vfx(p, VfxShape.CURSE_FX2, from, from.add(0, 1, 0), 80, 2.2f);
        b.vfx(p, VfxShape.CURSE_FX2, end, end.add(0, 1, 0), 80, 2.2f);
        SpellRuntime.zone(p.serverLevel(), 80, 10, age -> {
            for (LivingEntity t : GrimoireBook.along(p, from, end, 1.8)) {
                if (t instanceof Player pl && pl.isCreative()) continue;
                b.hurt(i, p, t, mode, 1.2f * s);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(t, 30), 1));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(t, 30), 0));
                if (age % 30 == 0) mark(t, p.getUUID(), 1);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Dwelling of the Poison Cloud: a cursed formation zone
    static boolean poisonCloud(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        double r = 6 * s;
        Vec3 c = GrimoireBook.aim(p, 16);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.CURSE_FX3, c, c.add(0, 1, 0), 24, 1.2f);
        b.vfx(p, VfxShape.CURSE_FX2, c, c.add(0, 1, 0), 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 10, age -> {
            for (LivingEntity t : foes(p, c, r, 12)) {
                b.hurt(i, p, t, mode, 1.6f * s);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, dur(t, 40), 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(t, 40), 0));
                if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                if (age % 40 == 0) mark(t, p.getUUID(), 1);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Ultra Giant Bull: a charging construct, then its cannons
    static boolean bull(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        Vec3 dir = flat(p.getViewVector(1f));
        if (dir.lengthSqr() < 1e-4) { GrimoireBook.fail(p, "Look ahead to loose the bull."); return false; }
        Vec3 start = p.position().add(dir.scale(2.5));
        Vec3[] at = { start };
        Set<Entity> hit = new HashSet<>();
        boolean[] done = { false };
        for (LivingEntity f : friends(p, p.position(), 5 * s)) f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.CURSE_FX3, start, start.add(dir), 24, 1.6f);
        Runnable fire = () -> {
            if (done[0]) return;
            done[0] = true;
            Vec3 top = at[0].add(0, 3, 0);
            b.vfx(p, VfxShape.CURSE_FX3, at[0], at[0].add(0, 1, 0), 28, 2.2f);
            for (LivingEntity t : foes(p, at[0], 14 * s, 6)) {
                b.hurt(i, p, t, mode, 8f * s);
                EnergyBridge.drain(t, p, 0.04);
                mark(t, p.getUUID(), 1);
                b.vfx(p, VfxShape.CURSE_FX1, top, t.getBoundingBox().getCenter(), 14, 0.9f);
            }
        };
        int steps = 28;
        SpellRuntime.zone(p.serverLevel(), steps + 1, 1, age -> {
            if (done[0]) return;
            if (age >= steps) { fire.run(); return; }
            Vec3 next = at[0].add(dir.scale(0.7));
            if (!free(p, next)) {
                Vec3 up = next.add(0, 1, 0);
                if (free(p, up)) next = up; else { fire.run(); return; }
            } else if (free(p, next.add(0, -1, 0))) {
                next = next.add(0, -1, 0);
            }
            for (LivingEntity t : GrimoireBook.along(p, at[0].add(0, 1, 0), next.add(0, 1, 0), 1.6)) {
                if (t instanceof Player pl && pl.isCreative()) continue;
                if (!hit.add(t)) continue;
                curseHit(b, i, p, mode, t, 18f * s, 2);
                t.setDeltaMovement(dir.scale(BalanceLaw.isBoss(t) ? 0.3 : 1.1).add(0, 0.4, 0));
                t.hurtMarked = true;
            }
            if (age % 4 == 0) b.vfx(p, VfxShape.CURSE_FX3, next, next.add(0, 1, 0), 14, 1.0f);
            at[0] = next;
        });
        return true;
    }

    private static boolean free(ServerPlayer p, Vec3 c) {
        return p.level().noCollision(p, new AABB(c.x - 1.2, c.y, c.z - 1.2, c.x + 1.2, c.y + 2.0, c.z + 1.2));
    }

    // ------------------------------------------------------------------ Curse Epidemic: the daily curse that lies in wait
    static boolean epidemic(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = scale(i, p);
        double r = 16 * s;
        List<LivingEntity> foes = foes(p, p.position(), r, 12);
        if (foes.isEmpty()) { GrimoireBook.fail(p, "No one to curse."); return false; }
        Vec3 c = p.position();
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.CURSE_FX2, c, c.add(0, 1, 0), 300, (float) r);
        b.vfx(p, VfxShape.CURSE_FX3, c.add(0, 1, 0), c.add(0, 1, 0), 30, 2.0f);
        for (LivingEntity t : foes) {
            b.hurt(i, p, t, mode, 6f * s);
            mark(t, p.getUUID(), 3);
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, dur(t, 200), 0));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(t, 100), 0));
            hexHp(t, p.serverLevel(), 0.1, 600);
            b.vfx(p, VfxShape.CURSE_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.8f);
        }
        UUID me = p.getUUID();
        SpellRuntime.zone(p.serverLevel(), 300, 20, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : foes(p, c, r + 4, 16)) {
                if (!markedBy(t, me)) continue;
                b.hurt(i, p, t, mode, 1.8f * s);
                if (age % 40 == 0) b.vfx(p, VfxShape.CURSE_FX3, t.position(), t.position().add(0, 1, 0), 14, 0.4f);
            }
        });
        return true;
    }
}
