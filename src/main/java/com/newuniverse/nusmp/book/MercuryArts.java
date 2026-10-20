package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild: the spells that MercuryBook's pages call (ids and numbers in {@link MercuryRules}). After the wiki (.github/wiki/mercury_magic.txt and the
 * spell pages): liquid metal, dense and versatile; wounds of it are toxic; compressed and solidified it is smooth enough to reflect light; weak against fire. The spells added by the
 * rebuild: Silver Bullet (small spheres), Silver Spear (one to three spears), Silver Shining Regal Spear (a beam from the palm that follows it), Silver Star of Execution (a cage whose bars
 * switch between spears and shields); the four old pages (Silver Blade, Silver Guardian, Silver Rain, Silver Eagle) are re-implemented in MercuryBook on the same helpers.
 * Everything scales with {@link EnergyBridge#power} (the aggressive curve, 0.8 to 2.4) and goes through {@code GrimoireBook.hurt}; bosses are held for a quarter by the control law.
 */
public final class MercuryArts {
    private MercuryArts() {}

    public static final int SILVER = 0xFFC8E8FF;

    static float pw(ServerPlayer p) { return EnergyBridge.power(p); }

    /** A flying body of mercury (param 0 = blade, 1 = spear) from 'from' toward 'to': the prop moves itself along its yaw and pitch (see MercuryProps). */
    public static void body(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 to, int kind, int life) {
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1e-4) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        MagicPropEntity e = MagicProps.spawn(level, PropKind.MERCURY_1, from, yaw, kind == 0 ? 1.0f : 0.9f, life, kind, owner);
        if (e != null) e.setXRot(pitch);
    }

    /** 1.99.0: does nothing. It was the short armour a signature cast put on; only the Silver Guardian page puts the Mercury armour on now (book.ArmourPolicy). */
    static void shortArmour(ServerPlayer p) { }

    /** Silver Spear's helper and the old pages': one spear 'from' to the target point, a liquid tail, pierces; the visual body is a prop. */
    static void lance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 from, Vec3 to, float raw, LivingEntity homing, int bodyKind, float power) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = to.subtract(from).normalize();
        VfxSpawn.send(level, VfxShape.MERCURY_FX1, from, to, SILVER, 14, power);
        if (bodyKind >= 0) body(level, p, from, to, bodyKind, 12);
        SpellRuntime.bolt(p, from, dir.scale(2.2), 0.6, 14, true, homing, (bolt, t) -> b.hurt(i, p, t, mode, raw * pw(p)), (bolt, at) -> VfxSpawn.send(level, VfxShape.MERCURY_FX3, at, at, SILVER, 14, 0.35f));
    }

    // ------------------------------------------------------------------ the new pages
    /** Silver Bullet: small spheres, condensed and thrown in a spread. */
    public static boolean silverBullet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        LivingEntity tgt = GrimoireBook.target(p, 26);
        Vec3 aim = tgt != null ? tgt.getBoundingBox().getCenter() : eye.add(look.scale(24));
        int n = MercuryRules.bulletCount(pw(p));
        Vec3 side = new Vec3(-look.z, 0, look.x);
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        b.castCircle(p, 0.5f);
        for (int k = 0; k < n; k++) {
            Vec3 from = eye.add(side.scale((k - (n - 1) / 2.0) * 0.18)).add(0, -0.3, 0);
            Vec3 dir = aim.add(side.scale((p.getRandom().nextDouble() - 0.5) * 1.6)).add(0, (p.getRandom().nextDouble() - 0.5) * 1.0, 0).subtract(from).normalize();
            SpellRuntime.later(level, k, () -> {
                if (!p.isAlive()) return;
                VfxSpawn.send(level, VfxShape.MERCURY_FX1, from, from.add(dir.scale(22)), SILVER, 10, 0.3f);
                SpellRuntime.bolt(p, from, dir.scale(2.6), 0.35, 12, false, null, (bolt, t) -> b.hurt(i, p, t, mode, 3f * pw(p)),
                        (bolt, at) -> VfxSpawn.send(level, VfxShape.MERCURY_FX3, at, at, SILVER, 10, 0.2f));
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    /** Silver Spear: one to three spears of mercury launched at a target (the middle one homes). */
    public static boolean silverSpear(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 30);
        Vec3 look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x);
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 aim = t != null ? t.getBoundingBox().getCenter() : p.getEyePosition().add(look.scale(26));
        int n = MercuryRules.spearCount(pw(p));
        b.castCircle(p, 0.7f);
        for (int k = 0; k < n; k++) {
            Vec3 from = p.getEyePosition().add(side.scale((k - (n - 1) / 2.0) * 0.8)).add(0, -0.1, 0);
            int kk = k;
            SpellRuntime.later(level, 2 + 2 * k, () -> { if (p.isAlive()) lance(b, i, p, mode, from, aim, 7f, kk == n / 2 ? t : null, 1, 0.8f); });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1f, 1.4f);
        return true;
    }

    /** Silver Shining Regal Spear: the palm held out, a beam of shining mercury follows it for two seconds. */
    public static boolean regalSpear(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float power = pw(p);
        double reach = MercuryRules.regalReach(power);
        b.castCircle(p, 1.0f);
        Vec3 c0 = p.getEyePosition().add(p.getViewVector(1f).scale(reach * 0.5));
        Nullification.shatterBarriers(level, c0, reach * 0.5, p);
        Set<UUID> struck = new HashSet<>();
        SpellRuntime.zone(level, MercuryRules.REGAL_TICKS, 2, age -> {
            if (!p.isAlive()) return;
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), hand = eye.add(look.scale(1.0)).add(0, -0.35, 0), end = eye.add(look.scale(reach));
            if (age % 2 == 0) VfxSpawn.send(level, VfxShape.MERCURY_FX1, hand, end, SILVER, 6, 1.6f);
            for (LivingEntity t : GrimoireBook.along(p, hand, end, 0.9)) {
                b.hurt(i, p, t, mode, 1.6f * power);
                if (struck.add(t.getUUID())) VfxSpawn.send(level, VfxShape.MERCURY_FX3, t.position(), t.position().add(0, 1, 0), SILVER, 14, 0.5f);
            }
        });
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
        shortArmour(p);
        return true;
    }

    /** Silver Star of Execution: strands of mercury spread over the area and close into a giant cage whose bars strike as spears and block as shields. */
    public static boolean starOfExecution(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float power = pw(p);
        float r = MercuryRules.starRadius(power);
        LivingEntity t = GrimoireBook.target(p, 26);
        Vec3 c = TransmutationArts.groundAt(level, t != null ? t.position() : GrimoireBook.aim(p, 24));
        boolean hold = BalanceLaw.beginControl(p);
        b.castCircle(p, 1.4f);
        MagicPropEntity cage = MagicProps.spawn(level, PropKind.MERCURY_3, c, 0f, r / 1.5f, MercuryRules.STAR_TICKS, 0, p);
        if (cage == null) { GrimoireBook.fail(p, "The mercury would not close."); return false; }
        VfxSpawn.send(level, VfxShape.MERCURY_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), SILVER, MercuryRules.STAR_TICKS, r / 1.5f);
        VfxSpawn.send(level, VfxShape.MERCURY_FX3, c, c.add(0, 1, 0), SILVER, 30, 1.6f);
        SpellRuntime.zone(level, MercuryRules.STAR_TICKS, 5, age -> {
            if (!p.isAlive()) return;
            List<LivingEntity> inside = new ArrayList<>();
            for (LivingEntity f : GrimoireBook.around(p, c, r + 3.0)) {
                double dx = f.getX() - c.x, dz = f.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d <= r + 0.4) inside.add(f);
                else if (hold && d < r + 3.0 && age > 4 && !BalanceLaw.isBoss(f)) {                    // the bars close the way out: whoever tries to leave is turned back
                    f.setDeltaMovement(f.getDeltaMovement().add(-dx / d * 0.35, 0.05, -dz / d * 0.35));
                    f.hurtMarked = true;
                }
            }
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(r, 4, r))) {            // the shield plates block what is thrown back in
                if (pr.position().distanceToSqr(c) < r * r) { VfxSpawn.send(level, VfxShape.MERCURY_FX3, pr.position(), pr.position(), SILVER, 10, 0.3f); pr.discard(); }
            }
            if (age % 4 == 0 && !inside.isEmpty()) {                                                    // every 20 ticks the bars strike
                int n = Math.min(inside.size(), MercuryRules.starVolley(power));
                for (int k = 0; k < n; k++) {
                    LivingEntity f = inside.get(k);
                    double a = Mth.TWO_PI * (k + age) / 8.0;
                    Vec3 from = c.add(Math.cos(a) * r * 0.97, 3.0, Math.sin(a) * r * 0.97);
                    lance(b, i, p, mode, from, f.getBoundingBox().getCenter(), 5f, f, 1, 0.6f);
                }
            }
        });
        shortArmour(p);
        return true;
    }
}
