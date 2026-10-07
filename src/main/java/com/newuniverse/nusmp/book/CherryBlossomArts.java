package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Cherry Blossom Magic: the casts of the grimoire that are not a shared shape. Petals are razor thin: they blind, they cut a little again and
 * again (magicule bleed), they hide the caster and his allies, and the same petals close wounds. The Dance of 100 Million Cherry Blossoms and the
 * Clones of the Beautiful Me are the wiki's two spells; the Magic Cherry Blossom Blizzard is its third page.
 */
final class CherryBlossomArts {
    private CherryBlossomArts() {}

    // ---------------------------------------------------------------- shared
    /** Creative and spectator players are never touched. */
    static boolean valid(LivingEntity t) { return !(t instanceof Player pl && (pl.isCreative() || pl.isSpectator())); }

    /** Blindness; a boss keeps its sight for all but a moment. */
    static void blind(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(t) ? Math.min(ticks, 30) : ticks, 0));
    }

    static void slow(LivingEntity t, int ticks, int amp) {
        boolean boss = BalanceLaw.isBoss(t);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? Math.min(ticks, 40) : ticks, boss ? Math.min(amp, 1) : amp));
    }

    /** What a petal hit leaves behind: sight clouded by petals and a thin cut in the magicule pool. */
    static ElementBook.Rider cut() { return (t, p) -> { if (valid(t)) { blind(t, 50); Nullification.bleed(t, 0.01); } }; }

    private static void send(ServerPlayer p, GrimoireBook b, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, b.color, ticks, power);
    }

    private static boolean friend(ServerPlayer p, LivingEntity o) { return o == p || o.isAlliedTo(p); }

    /** Players (and the caster) near c that count as friends of the caster. */
    private static List<ServerPlayer> friends(ServerPlayer p, Vec3 c, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer o : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(r),
                o -> o.isAlive() && !o.isSpectator() && friend(p, o) && o.distanceToSqr(c) <= r * r)) {
            out.add(o);
            if (out.size() >= 12) break;
        }
        return out;
    }

    /** Takes away one harmful effect. */
    private static void cleanse(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects()))
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) { t.removeEffect(e.getEffect()); return; }
    }

    // ---------------------------------------------------------------- starters / mid
    /** Petal Fan: one broad sweep of the fan; what it brushes is cut, blinded and pushed back. */
    static boolean fan(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 9 * GrimoireBook.size(i, p);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (!valid(t) || t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.35) continue;
            if (n++ >= 20) break;
            b.hurt(i, p, t, mode, 8f);
            cut().apply(t, p);
            Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01) { d = d.normalize(); t.knockback(0.6, -d.x, -d.z); }
        }
        b.castCircle(p, 0.7f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX1, eye, eye.add(look.scale(r)), 14, 1.2f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, eye.add(look.scale(r * 0.5)), eye.add(look.scale(r)), 22, 1f);
        return true;
    }

    /** Blossom Snare: petals wind around one foe so thickly it cannot move or see, and every half second they cut it again. */
    static boolean snare(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No one to wrap in petals."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        blind(t, ticks);
        b.hurt(i, p, t, mode, 4f);
        b.castCircle(p, 0.8f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX2, t.position(), t.position(), ticks, Math.max(1.2f, t.getBbWidth() * 1.3f));
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!t.isAlive() || age == 0) return;
            b.hurt(i, p, t, mode, 1.8f);
            Nullification.bleed(t, 0.005);
            send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, t.getBoundingBox().getCenter(), t.position(), 14, 0.5f);
        });
        return true;
    }

    /** Petal Veil: a dense swarm hides you for 10 s. You turn invisible, hunters lose you, shots are shredded and anyone close goes blind. */
    static boolean veil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        float r = 4.5f * GrimoireBook.size(i, p);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CHERRY_BLOSSOM_FX2, p, p.position(), b.color, ticks, r);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 24, 1f);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            if (!p.isAlive()) return;
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r)))
                if (pr.getOwner() != p) {
                    send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, pr.position(), pr.position(), 12, 0.5f);
                    pr.discard();
                }
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                if (!valid(t)) continue;
                if (n++ >= 16) break;
                blind(t, 60);
            }
        });
        return true;
    }

    /** Petal Step: you ride a gust of petals, cutting through everything on your line, and keep the speed for a few seconds. */
    static boolean step(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!ElementBook.dash(6f, 9, false, VfxShape.CHERRY_BLOSSOM_FX1, cut()).cast(b, i, p, mode)) return false;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, p.position().add(0, 1, 0), p.position().add(0, 1.5, 0), 22, 0.9f);
        return true;
    }

    /** Beautifying Bloom: petals settle on you and your allies; wounds close, one curse is washed away and a veil of petals thickens the skin. */
    static boolean bloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 8 * GrimoireBook.size(i, p);
        float frac = 0.22f;
        for (ServerPlayer o : friends(p, p.position(), r)) {
            BalanceLaw.heal(o, o.getMaxHealth() * frac);
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            cleanse(o);
            send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, o.position().add(0, 1, 0), o.position().add(0, 2, 0), 26, 0.8f);
        }
        b.castCircle(p, 1f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX2, p.position(), p.position(), 50, (float) r);
        return true;
    }

    /**
     * Clones of the Beautiful Me: three clones of petals in your likeness circle you for 12 s. They fly, they draw the hunters off you, and every
     * half second each throws a handful of petals at the nearest foe. (The clones are petal swarms drawn on the effect, not real mobs: the
     * mechanical body is the lost aggro and the thrown petals.)
     */
    static boolean clones(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 240;
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 28, 1.2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
            List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, p.position(), 12 * s));
            foes.removeIf(t -> !valid(t));
            for (int k = 0; k < 3; k++) {
                double a = age * 0.12 + k * Math.PI * 2 / 3;
                Vec3 at = p.position().add(Math.cos(a) * 2.6, 1.2 + Math.sin(age * 0.2 + k) * 0.5, Math.sin(a) * 2.6);
                if (age % 30 == 0) send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, at, at.add(0, 1, 0), 18, 0.5f);
                LivingEntity near = null;
                for (LivingEntity t : foes) if (near == null || t.distanceToSqr(at) < near.distanceToSqr(at)) near = t;
                if (near == null) continue;
                b.hurt(i, p, near, mode, 2.5f);
                cut().apply(near, p);
                send(p, b, VfxShape.CHERRY_BLOSSOM_FX1, at, near.getBoundingBox().getCenter(), 8, 0.7f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- zone
    /** Magic Cherry Blossom Blizzard: a storm of petals at the aim point for 8 s; everyone inside is cut, blinded, slowed, and swept toward its eye. */
    static boolean blizzard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float r = 7f * GrimoireBook.size(i, p);
        int ticks = 160;
        b.castCircle(p, 1f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX2, c, c, ticks, r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (n++ >= 24) break;
                b.hurt(i, p, t, mode, 2.5f);
                blind(t, 50);
                slow(t, 40, 1);
                Nullification.bleed(t, 0.005);
                Vec3 pull = c.subtract(t.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 1) t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(0.12)));
                t.hurtMarked = true;
            }
            for (int k = 0; k < 2; k++) {
                double a = age * 0.9 + k * 3.1, d = r * (0.3 + 0.3 * k);
                Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, at, at.add(0, 1, 0), 16, 0.6f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- signature
    /**
     * Dance of 100 Million Cherry Blossoms: a great fan of petals opens in front of you and sweeps for 4 s. Every fifth of a second, twenty times, it
     * looses five streams of razor petals along the way you face, swinging left and right; each stream pierces everything on its line, cuts,
     * blinds and bleeds. Small openings are no defence: the damage reaches through iframes.
     */
    static boolean dance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 80;
        b.castCircle(p, 1.6f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CHERRY_BLOSSOM_FX2, p, p.position(), b.color, ticks, 3.5f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, p.position().add(0, 1.4, 0), p.position().add(p.getViewVector(1f).scale(3)).add(0, 1.4, 0), 36, 1.6f);
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!p.isAlive()) return;
            Vec3 start = p.getEyePosition().add(0, -0.3, 0), look = p.getViewVector(1f);
            double yaw = Math.atan2(look.z, look.x) + Math.sin(age * 0.2) * 0.45;
            for (int k = 0; k < 5; k++) {
                double a = yaw + (k - 2) * 0.16;
                Vec3 dir = new Vec3(Math.cos(a) * (1 - Math.abs(look.y) * 0.4), look.y, Math.sin(a) * (1 - Math.abs(look.y) * 0.4)).normalize();
                if (k % 2 == 0) send(p, b, VfxShape.CHERRY_BLOSSOM_FX1, start, start.add(dir.scale(20)), 12, 0.8f);
                SpellRuntime.bolt(p, start, dir.scale(1.5), 0.5, 14, true, null, (bolt, t) -> {
                    if (!valid(t)) return;
                    t.invulnerableTime = 0;
                    b.hurt(i, p, t, mode, 3.5f);
                    cut().apply(t, p);
                }, (bolt, at) -> {});
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- daily
    /**
     * Everlasting Sakura Grove: a grove of cherry trees blooms at the aim point for 15 s and the petals never stop falling. Foes inside are cut,
     * blinded, weakened and bled each second; you and your allies are mended and regenerate. Four trees stand at its edge (temporary blocks).
     */
    static boolean grove(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 10f * GrimoireBook.size(i, p);
        int ticks = 300;
        b.castCircle(p, 1.6f);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX2, c, c, ticks, r);
        send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 36, 1.8f);
        BlockState log = Blocks.CHERRY_LOG.defaultBlockState();
        BlockState leaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.6;
            Vec3 base = c.add(Math.cos(a) * r * 0.8, 0, Math.sin(a) * r * 0.8);
            for (int h = 0; h < 4; h++) {
                BlockPos pos = BlockPos.containing(base.add(0, h, 0));
                if (sl.isLoaded(pos)) SpellRuntime.tempBlock(sl, pos, log, ticks);
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 3; dy <= 4; dy++) {
                if (dx == 0 && dz == 0 && dy == 3) continue;
                BlockPos pos = BlockPos.containing(base.add(dx, dy, dz));
                if (sl.isLoaded(pos)) SpellRuntime.tempBlock(sl, pos, leaves, ticks);
            }
        }
        SpellRuntime.zone(sl, ticks, 20, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (n++ >= 32) break;
                b.hurt(i, p, t, mode, 3f);
                blind(t, 60);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                Nullification.bleed(t, 0.01);
            }
            for (ServerPlayer o : friends(p, c, r)) {
                BalanceLaw.heal(o, o.getMaxHealth() * 0.04f);
                o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            }
            if (age % 60 == 0) send(p, b, VfxShape.CHERRY_BLOSSOM_FX3, c, c.add(0, 1, 0), 22, 1.8f);
        });
        return true;
    }
}
