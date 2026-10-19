package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: the spells of ModificationBook (ids and numbers in {@link ModificationRules}). Canon: Modification Magic changes the structure of
 * physical and magical things; in the Spade raid Morris sent clawed tendrils to tear the Black Bulls' base apart, threw needles, took a creature apart and put it back together, cancelled
 * Vanessa's Fate Manipulation and went through Rouge's protection (Grey and Gauche turned his tendrils into flowers; Asta's anti-magic he could not edit). The spells are built as the edits
 * of structure they are: needles, tendrils, a field that unravels barriers and armour, a shock of rewritten air, a body that re-forms against the kind of blow that has been hurting it
 * (the ledger in {@link ModificationRules.Ledger}), a thread that draws a specimen in, a spell that is rewritten into needles, test subjects, a theatre of tendrils with doors.
 * The Black Bulls beat his edits with force he could not edit at once: the Re-Structure here likewise punishes relying on one kind of defence (every other kind finds a hole, x1.1).
 * <p>Spells go through {@code GrimoireBook.hurt} (the balance law, EP scaling); bosses are held for a third and never dragged as hard; every volley is capped. The temporary armour is
 * the Dissector's Frame (Aura.MODIFICATION).</p>
 */
public final class ModificationArts {
    private ModificationArts() {}

    public static final int ORANGE = 0xFFFF9B58;
    private static final String ADAPT_UNTIL = "nusmp_mod_adapt_until", ADAPT_CUT = "nusmp_mod_adapt_cut", FRAME_UNTIL = "nusmp_mod_frame_until", FRAME_CUT = "nusmp_mod_frame_cut";
    private static final Map<UUID, ModificationRules.Ledger> LEDGERS = new HashMap<>();
    private static boolean hooked;

    /** Registers the hooks (Re-Structure's ledger, the Frame's needle upkeep). Called once from ModificationProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var data = victim.getPersistentData();
            long now = victim.level().getGameTime();
            if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            if (data.getLong(FRAME_UNTIL) > now) e.setAmount(e.getAmount() * (1f - data.getFloat(FRAME_CUT)));
            if (data.getLong(ADAPT_UNTIL) <= now) return;
            ModificationRules.Ledger ledger = LEDGERS.computeIfAbsent(victim.getUUID(), k -> new ModificationRules.Ledger());
            ModificationRules.Kind kind = kindOf(e.getSource());
            float raw = e.getAmount();
            e.setAmount(raw * ledger.multiplier(kind, now, data.getFloat(ADAPT_CUT)));
            ledger.note(kind, raw, now);
        });
        AttributeEvents.playerTick(p -> {
            var data = p.getPersistentData();
            long now = p.level().getGameTime();
            if (data.getLong(FRAME_UNTIL) <= now) return;
            int interval = ModificationRules.autoNeedleInterval(EnergyBridge.modifier(p));
            if (p.tickCount % interval == 0) autoNeedle(p);
        });
        AttributeEvents.logout(p -> LEDGERS.remove(p.getUUID()));
    }

    /** The kind of a blow: a projectile is RANGED, magic (a spell, Tensura's elemental and curse types) is MAGIC, the rest is MELEE. */
    static ModificationRules.Kind kindOf(DamageSource s) {
        if (s.is(DamageTypeTags.IS_PROJECTILE) || s.getDirectEntity() instanceof Projectile) return ModificationRules.Kind.RANGED;
        if (AntiMagic.isMagic(s)) return ModificationRules.Kind.MAGIC;
        return ModificationRules.Kind.MELEE;
    }

    // ------------------------------------------------------------------ helpers
    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96), x -> x.kind() == kind && p.getUUID().equals(x.ownerId()))) e.expire();
    }

    private static void armour(ServerPlayer p, int ticks, int style) {
        PlayerAuras.set(p, Aura.MODIFICATION, ticks, style);
    }

    private static void shortArmour(ServerPlayer p) {
        if (!PlayerAuras.active(p, Aura.MODIFICATION)) armour(p, ModificationRules.SHORT_ARMOUR_TICKS, 1);
    }

    /** A needle flying 'from' to 'to': the model prop with its direction, and the effect. */
    private static void needleVisual(ServerLevel level, Vec3 from, Vec3 to, ServerPlayer owner, float power) {
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1e-4) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        MagicPropEntity n = MagicProps.spawn(level, PropKind.MODIFICATION_3, from, yaw, 0.9f, 16, 0, owner);
        if (n != null) n.setXRot(pitch);
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX1, from, to, ORANGE, 14, power);
    }

    private static List<LivingEntity> foesByDistance(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> l = new ArrayList<>(GrimoireBook.around(p, c, r));
        l.sort((a, b) -> Double.compare(a.distanceToSqr(c), b.distanceToSqr(c)));
        return l;
    }

    // ------------------------------------------------------------------ the five generic pages, re-implemented (ids kept)
    /** attribute_bolt: Dissection Needles. */
    public static boolean needles(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity tgt = GrimoireBook.target(p, 30);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x);
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 aim = tgt != null ? tgt.getBoundingBox().getCenter() : eye.add(look.scale(28));
        int n = ModificationRules.needleCount(EnergyBridge.modifier(p));
        for (int k = 0; k < n; k++) {
            Vec3 from = eye.add(side.scale((k - (n - 1) / 2.0) * 0.45)).add(0, -0.25, 0);
            Vec3 dir = aim.subtract(from).normalize();
            SpellRuntime.later(level, 2 * k, () -> {
                if (!p.isAlive()) return;
                needleVisual(level, from, from.add(dir.scale(26)), p, 0.8f);
                SpellRuntime.bolt(p, from, dir.scale(1.8), 0.45, 18, false, null, (bolt, t) -> {
                    b.hurt(i, p, t, mode, 6f);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                }, (bolt, at) -> VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, at, at, ORANGE, 14, 0.3f));
            });
        }
        shortCircle(p, 0.6f);
        return true;
    }

    private static void shortCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MODIFICATION_FX3, p, p.position().add(0, 1, 0), ORANGE, 16, power);
    }

    /** attribute_cut: Clawed Tendrils. */
    public static boolean tendrils(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> foes = foesByDistance(p, p.position(), 16);
        LivingEntity looked = GrimoireBook.target(p, 24);
        if (looked != null) { foes.remove(looked); foes.add(0, looked); }
        if (foes.isEmpty()) { GrimoireBook.fail(p, "No one in reach."); return false; }
        int n = Math.min(foes.size(), ModificationRules.tendrilCount(EnergyBridge.modifier(p)));
        boolean drag = BalanceLaw.beginControl(p);
        for (int k = 0; k < n; k++) {
            LivingEntity t = foes.get(k);
            Vec3 at = TransmutationArts.groundAt(level, t.position());
            MagicProps.spawn(level, PropKind.MODIFICATION_1, at, (float) Math.toDegrees(Math.atan2(-(p.getX() - at.x), p.getZ() - at.z)), 1.0f, 44, 0, p);
            SpellRuntime.later(level, 14, () -> {
                if (!t.isAlive() || !p.isAlive()) return;
                b.hurt(i, p, t, mode, 8f);
                if (drag) {
                    Vec3 pull = p.position().subtract(t.position()).multiply(1, 0, 1);
                    if (pull.lengthSqr() > 1e-4) {
                        t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(BalanceLaw.isBoss(t) ? 0.15 : 0.5)).add(0, 0.1, 0));
                        t.hurtMarked = true;
                    }
                }
                VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, t.position(), t.position().add(0, 1, 0), ORANGE, 16, 0.4f);
            });
        }
        shortCircle(p, 0.7f);
        return true;
    }

    /** attribute_field: Structural Unravel. */
    public static boolean unravel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 4.5f * GrimoireBook.size(i, p);
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), ORANGE, 80, r / 1.5f);
        Nullification.shatterBarriers(level, c, r, p);
        SpellRuntime.zone(level, 80, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
            }
        });
        shortCircle(p, 0.7f);
        return true;
    }

    /** attribute_domain: Structure Shock. */
    public static boolean shock(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float r = 6.5f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, c, c.add(0, 1, 0), ORANGE, 30, r / 2.4f);
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX2, c.add(0, 0.05, 0), c, ORANGE, 24, r / 1.5f * 0.6f);
        int n = 0;
        for (LivingEntity t : foesByDistance(p, c, r)) {
            if (n++ >= 12) break;
            b.hurt(i, p, t, mode, 14f);
            Vec3 out = t.position().subtract(c).multiply(1, 0, 1);
            if (out.lengthSqr() > 1e-4) {
                t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(BalanceLaw.isBoss(t) ? 0.4 : 1.2)).add(0, 0.25, 0));
                t.hurtMarked = true;
            }
        }
        shortArmour(p);
        return true;
    }

    /** attribute_guard: Re-Structure. */
    public static boolean restructure(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var data = p.getPersistentData();
        data.putLong(ADAPT_UNTIL, p.level().getGameTime() + ModificationRules.GUARD_TICKS);
        data.putFloat(ADAPT_CUT, ModificationRules.adaptCut(BalanceLaw.epScale(p)));
        LEDGERS.remove(p.getUUID());
        armour(p, ModificationRules.GUARD_TICKS, 1);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ModificationRules.GUARD_TICKS, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MODIFICATION_FX3, p, p.position().add(0, 1, 0), ORANGE, 34, 1.1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MODIFICATION_FX2, p.position().add(0, 0.05, 0), p.position(), ORANGE, 40, 2.2f / 1.5f);
        return true;
    }

    // ------------------------------------------------------------------ the five new pages
    /** specimen_thread: Specimen Mark. */
    public static boolean specimenThread(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "No specimen in sight."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel level = p.serverLevel();
        boolean boss = BalanceLaw.isBoss(t);
        int life = ModificationRules.THREAD_TICKS;
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX2, t.position().add(0, 0.05, 0), t.position(), ORANGE, life, 2.0f / 1.5f);
        SpellRuntime.zone(level, life, 2, age -> {
            if (!t.isAlive() || !p.isAlive() || t.distanceToSqr(p) > 26 * 26 || age >= life) return;
            int tick = age * 2;
            Vec3 pull = p.position().subtract(t.position()).multiply(1, 0, 1);
            double dist = pull.length();
            if (dist > 1e-4) {
                double k = ModificationRules.threadPull(tick, life) * (boss ? 1.0 / 3 : 1.0);
                t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(k)));
                t.hurtMarked = true;
            }
            if (tick % 8 == 0) VfxSpawn.send(level, VfxShape.MODIFICATION_FX1, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), ORANGE, 10, 0.35f);
            if (tick % 20 == 0) {
                EnergyBridge.drain(t, p, 0.01);
                b.hurt(i, p, t, mode, 2f);
            }
            if (dist < 2.6 && tick > 10) {                                                  // reached: the dissection
                b.hurt(i, p, t, mode, 12f);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, t.position(), t.position().add(0, 1, 0), ORANGE, 22, 0.8f);
                p.getPersistentData().putLong("nusmp_mod_thread_end", level.getGameTime());
            }
        });
        return true;
    }

    /** spell_rewrite. */
    public static boolean spellRewrite(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        VfxSpawn.sendFollowing(level, VfxShape.MODIFICATION_FX2, p, p.position(), ORANGE, ModificationRules.REWRITE_TICKS, 4.5f / 1.5f);
        int[] returned = {0};
        SpellRuntime.zone(level, ModificationRules.REWRITE_TICKS, 2, age -> {
            if (!p.isAlive() || returned[0] >= 6) return;
            Vec3 c = p.position().add(0, 1, 0);
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(4.5))) {
                if (returned[0]++ >= 6) break;
                Vec3 from = pr.position();
                Vec3 to = pr.getOwner() instanceof LivingEntity o && o.isAlive() ? o.getBoundingBox().getCenter() : from.add(pr.getDeltaMovement().normalize().scale(-20));
                Vec3 dir = to.subtract(from).normalize();
                pr.discard();
                needleVisual(level, from, from.add(dir.scale(24)), p, 0.7f);
                SpellRuntime.bolt(p, from, dir.scale(1.8), 0.5, 16, false, null, (bolt, t) -> b.hurt(i, p, t, mode, 5f), null);
            }
        });
        shortCircle(p, 0.8f);
        return true;
    }

    /** test_subjects. */
    public static boolean testSubjects(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        dismiss(p, PropKind.MODIFICATION_2);
        double ep = BalanceLaw.epScale(p);
        int n = ModificationRules.subjectCount(EnergyBridge.modifier(p));
        int ticks = TransmutationRules.summonTicks(ModificationRules.SUBJECT_TICKS, ep);
        Vec3 look = flat(p.getViewVector(1f)), side = new Vec3(-look.z, 0, look.x);
        for (int k = 0; k < n; k++) {
            Vec3 at = TransmutationArts.groundAt(level, p.position().add(look.scale(2.4)).add(side.scale((k - (n - 1) / 2.0) * 1.8)));
            MagicPropEntity e = MagicProps.spawn(level, PropKind.MODIFICATION_2, at, p.getYRot() + 180f, 0.95f + 0.02f * (float) Math.min(8.0, ep - 1), ticks, 0, p);
            if (e != null) e.getPersistentData().putFloat("nusmp_subject_dmg", 7f * (float) GrimoireBook.cover(i).damage);
            VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, at, at.add(0, 1, 0), ORANGE, 24, 0.6f);
        }
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX2, p.position().add(look.scale(2.4)).add(0, 0.05, 0), p.position(), ORANGE, 50, 3.0f / 1.5f);
        return true;
    }

    /** dissection_theatre. */
    public static boolean dissectionTheatre(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 24);
        Vec3 c = t != null ? t.position() : GrimoireBook.aim(p, 22);
        int life = ModificationRules.THEATRE_TICKS;
        final Vec3 centre = TransmutationArts.groundAt(level, c);
        for (int k = 0; k < 6; k++) {                                                       // six tendrils on the ring; the two at 0 and 180 degrees are missing: the doors
            if (k == 0 || k == 3) continue;
            double a = Math.PI / 3 * k;
            Vec3 at = TransmutationArts.groundAt(level, centre.add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
            MagicPropEntity e = MagicProps.spawn(level, PropKind.MODIFICATION_1, at, 0f, 1.3f, life + 10, 0, p);
            if (e != null) {
                var d = e.getPersistentData();
                d.putDouble("tc_x", centre.x); d.putDouble("tc_y", centre.y); d.putDouble("tc_z", centre.z);
                d.putDouble("tc_a", a); d.putInt("tc_life", life);
            }
        }
        VfxSpawn.send(level, VfxShape.MODIFICATION_FX2, centre.add(0, 0.05, 0), centre.add(0, 1, 0), ORANGE, life + 10, 8f / 1.5f);
        int[] hitAt = {-100};
        SpellRuntime.zone(level, life, 5, age -> {
            if (!p.isAlive()) return;
            double r = ModificationRules.theatreRadius(age * 5, life);
            for (LivingEntity f : GrimoireBook.around(p, centre, 10)) {
                double dx = f.getX() - centre.x, dz = f.getZ() - centre.z, d = Math.sqrt(dx * dx + dz * dz);
                double ang = Math.atan2(dz, dx);
                boolean door = Math.abs(Math.sin(ang)) < 0.5;           // within 30 degrees of the doors at 0 and 180 degrees
                if (Math.abs(d - r) < 1.3 && !door && age * 5 - hitAt[0] >= 15) {
                    b.hurt(i, p, f, mode, 10f);
                    Vec3 in = centre.subtract(f.position()).multiply(1, 0, 1);
                    if (in.lengthSqr() > 1e-4) { f.setDeltaMovement(f.getDeltaMovement().add(in.normalize().scale(0.5))); f.hurtMarked = true; }
                }
            }
            if (age * 5 - hitAt[0] >= 15) hitAt[0] = age * 5;
        });
        SpellRuntime.later(level, life, () -> {
            if (!p.isAlive()) return;
            VfxSpawn.send(level, VfxShape.MODIFICATION_FX3, centre, centre.add(0, 1, 0), ORANGE, 30, 2.0f);
            for (LivingEntity f : GrimoireBook.around(p, centre, 3.5)) {
                b.hurt(i, p, f, mode, 16f);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(f) ? 20 : 50, 2));
            }
        });
        shortArmour(p);
        return true;
    }

    /** dissector_frame. */
    public static boolean dissectorFrame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var data = p.getPersistentData();
        data.putLong(FRAME_UNTIL, p.level().getGameTime() + ModificationRules.FRAME_TICKS);
        data.putFloat(FRAME_CUT, Math.min(0.3f, ModificationRules.adaptCut(BalanceLaw.epScale(p)) * 0.8f));
        armour(p, ModificationRules.FRAME_TICKS, 0);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ModificationRules.FRAME_TICKS, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MODIFICATION_FX3, p, p.position().add(0, 1, 0), ORANGE, 36, 1.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MODIFICATION_FX2, p.position().add(0, 0.05, 0), p.position(), ORANGE, 50, 2.6f / 1.5f);
        return true;
    }

    /** One needle of the Frame's crown, fired at the nearest foe within 12 blocks. */
    static void autoNeedle(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        LivingEntity best = null;
        double bd = 144;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 12)) {
            double d = t.distanceToSqr(p);
            if (d < bd) { bd = d; best = t; }
        }
        if (best == null) return;
        Vec3 from = p.getEyePosition().add(0, 0.6, 0), to = best.getBoundingBox().getCenter();
        needleVisual(level, from, to, p, 0.6f);
        best.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, best, 4f, 0.5));
    }

    public static boolean frameOn(LivingEntity e) { return e.getPersistentData().getLong(FRAME_UNTIL) > e.level().getGameTime(); }
}
