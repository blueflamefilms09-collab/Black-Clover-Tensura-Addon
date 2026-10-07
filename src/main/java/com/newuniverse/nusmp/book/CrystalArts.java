package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Crystal Magic: bolts and beams of faceted light, shards that shatter, crystal shields and a far-seeing scope. */
final class CrystalArts {
    private CrystalArts() {}

    private static final VfxShape CAST = VfxShape.CRYSTAL_FX1, ZONE = VfxShape.CRYSTAL_FX2, BURST = VfxShape.CRYSTAL_FX3;

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.35, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    private static void effect(LivingEntity t, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(e, ticks, amp));
    }

    /** Where a beam from a along dir ends: its full length, or the first wall. */
    private static Vec3 beamEnd(ServerPlayer p, Vec3 a, Vec3 dir, double len) {
        Vec3 end = a.add(dir.scale(len));
        HitResult wall = p.level().clip(new ClipContext(a, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return wall.getType() != HitResult.Type.MISS ? wall.getLocation() : end;
    }

    /** The first free spot above the ground near a column of the zone (for the temporary crystals). */
    private static BlockPos groundNear(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y + 2, z);
        for (int k = 0; k < 5; k++) {
            if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) return pos;
            pos = pos.below();
        }
        return null;
    }

    // ------------------------------------------------------------------ prism beam
    /** Prism Beam: a lance of light that blinds what it pierces, then refracts off the first foe into the ones around it. */
    static boolean prism(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = hand(p);
        Vec3 end = beamEnd(p, a, p.getViewVector(1f), 26 * GrimoireBook.size(i, p));
        List<LivingEntity> hits = new ArrayList<>(GrimoireBook.along(p, a, end, 0.5));
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(a)));
        b.castCircle(p, 0.8f);
        b.vfx(p, CAST, a, end, 12, 1.1f);
        int n = 0;
        for (LivingEntity t : hits) {
            if (n++ >= 4) break;
            b.hurt(i, p, t, mode, 9f);
            effect(t, MobEffects.BLINDNESS, 50, 0);
        }
        if (!hits.isEmpty()) {
            LivingEntity first = hits.get(0);
            Vec3 at = mid(first);
            b.vfx(p, BURST, at, at, 16, 0.8f);
            int split = 0;
            for (LivingEntity t : GrimoireBook.around(p, at, 9)) {
                if (hits.contains(t) || split >= 3) continue;
                split++;
                b.vfx(p, CAST, at, mid(t), 8, 0.6f);
                b.hurt(i, p, t, mode, 5f);
                effect(t, MobEffects.BLINDNESS, 30, 0);
            }
        } else {
            b.vfx(p, BURST, end, end, 14, 0.6f);
        }
        return true;
    }

    // ------------------------------------------------------------------ shatter burst
    /** Shatter Burst: the target's skin crystallises and bursts; the splinters wound and weaken everyone near, and break barriers. */
    static boolean shatter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "Nothing to shatter."); return false; }
        Vec3 at = mid(t);
        boolean marked = t.hasEffect(MobEffects.GLOWING);                      // a target seen through the scope breaks harder
        b.castCircle(p, 0.8f);
        b.vfx(p, CAST, hand(p), at, 8, 0.8f);
        SpellRuntime.later(p.serverLevel(), 6, () -> {
            if (!t.isAlive()) return;
            Vec3 c = mid(t);
            b.hurt(i, p, t, mode, marked ? 18f : 12f);
            Nullification.bleed(t, 0.02);
            for (LivingEntity o : GrimoireBook.around(p, c, 4.5)) {
                if (o == t) continue;
                b.hurt(i, p, o, mode, 6f);
                effect(o, MobEffects.WEAKNESS, 80, 0);
                Nullification.bleed(o, 0.01);
            }
            Nullification.shatterBarriers(p.serverLevel(), c, 3, p);
            b.vfx(p, BURST, c, c, 22, 1.1f);
        });
        return true;
    }

    // ------------------------------------------------------------------ crystal cage
    /** Crystal Cage: tinted crystal grows round one target and pins it (hard control). */
    static boolean cage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "Nothing to encase."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 70);
        effect(t, MobEffects.MOVEMENT_SLOWDOWN, ticks, 9);
        effect(t, MobEffects.WEAKNESS, ticks, 1);
        b.hurt(i, p, t, mode, 6f);
        ServerLevel level = p.serverLevel();
        BlockPos base = t.blockPosition();
        int h = Math.min(3, (int) Math.ceil(t.getBbHeight()));
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] s : sides) for (int y = 0; y < h; y++) {
            BlockPos pos = base.offset(s[0], y, s[1]);
            if (level.isLoaded(pos)) SpellRuntime.tempBlock(level, pos, Blocks.TINTED_GLASS.defaultBlockState(), ticks);
        }
        BlockPos top = base.above(h);
        if (level.isLoaded(top)) SpellRuntime.tempBlock(level, top, Blocks.TINTED_GLASS.defaultBlockState(), ticks);
        b.castCircle(p, 0.9f);
        b.vfx(p, CAST, hand(p), mid(t), 10, 1f);
        b.vfx(p, ZONE, t.position(), t.position(), Math.min(ticks, 80), 1.6f);
        return true;
    }

    // ------------------------------------------------------------------ crystal scope
    /** Crystal Magic: Crystal Scope. A helm of twin telescopes: you see in the dark, every foe within 48 blocks shows up, allies share the sight. */
    static boolean scope(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double range = 48 * GrimoireBook.size(i, p);
        int ticks = 400;
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), BURST, p, p.position().add(0, 1.6, 0), b.color, 30, 0.7f);
        List<ServerPlayer> team = p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(32),
                m -> m == p || m.isAlliedTo(p));
        for (ServerPlayer a : team) effect(a, MobEffects.NIGHT_VISION, ticks + 100, 0);
        int[] pulse = {0};
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (p.isRemoved() || !p.isAlive()) return;
            List<LivingEntity> seen = GrimoireBook.around(p, p.position(), range);
            seen.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
            int n = 0;
            for (LivingEntity t : seen) {
                if (n++ >= 16) break;
                effect(t, MobEffects.GLOWING, 50, 0);
            }
            if (pulse[0]++ % 2 == 0 && !seen.isEmpty()) {
                LivingEntity near = seen.get(0);
                float frac = near.getHealth() / Math.max(1f, near.getMaxHealth());
                String cond = frac > 0.75f ? "unhurt" : frac > 0.4f ? "wounded" : "near death";
                p.displayClientMessage(Component.literal("Scope: " + near.getName().getString() + " - " + Math.round(near.getHealth())
                        + "/" + Math.round(near.getMaxHealth()) + " HP, " + cond + ", " + Math.round(near.distanceTo(p)) + " m"), true);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ crystal garden
    /** Crystal Garden: crystals thrust up out of the ground in a ring; each pulse cuts and slows whatever stands in it. */
    static boolean garden(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * GrimoireBook.size(i, p);
        int ticks = 100;
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.2f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        SpellRuntime.zone(level, ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, 4f);
                effect(t, MobEffects.MOVEMENT_SLOWDOWN, 50, 1);
                Nullification.bleed(t, 0.005);
            }
            for (int k = 0; k < 5; k++) {
                double ang = p.getRandom().nextDouble() * Math.PI * 2, d = p.getRandom().nextDouble() * r;
                BlockPos pos = groundNear(level, c.x + Math.cos(ang) * d, c.y, c.z + Math.sin(ang) * d);
                if (pos != null && level.isLoaded(pos)) SpellRuntime.tempBlock(level, pos, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 50);
            }
            b.vfx(p, BURST, c, c, 16, 0.7f);
        });
        return true;
    }

    // ------------------------------------------------------------------ prism dome
    /** Prism Dome: a faceted dome around where you stand turns every shot back on its shooter and dazzles foes inside its rim. */
    static boolean dome(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        double r = 6 * GrimoireBook.size(i, p);
        int ticks = 140;
        b.castCircle(p, 1.2f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        int[] step = {0};
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    e -> e.position().distanceToSqr(c) <= r * r)) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1));
                pr.setOwner(p);
                pr.hurtMarked = true;
                b.vfx(p, BURST, pr.position(), pr.position(), 10, 0.5f);
            }
            if (step[0]++ % 5 == 0) {
                for (LivingEntity t : GrimoireBook.around(p, c, r)) effect(t, MobEffects.BLINDNESS, 40, 0);
                for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                        x -> x == p || x.isAlliedTo(p))) effect(a, MobEffects.DAMAGE_RESISTANCE, 40, 0);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ grand prism cannon
    /** Grand Prism Cannon: a held breath of gathering light, then one huge beam that wrecks, dazzles, jams magic and breaks barriers. */
    static boolean cannon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, p.position(), p.position(), 24, 2.2f);
        b.vfx(p, BURST, hand(p), hand(p), 22, 0.9f);
        SpellRuntime.later(p.serverLevel(), 20, () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            Vec3 a = hand(p);
            Vec3 end = beamEnd(p, a, p.getViewVector(1f), 36 * s);
            b.vfx(p, CAST, a, end, 18, 2.2f);
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, a, end, 1.8)) {
                b.hurt(i, p, t, mode, 26f);
                effect(t, MobEffects.BLINDNESS, 100, 0);
                effect(t, MobEffects.WEAKNESS, 100, 1);
                EnergyBridge.effect(t, "silence", BalanceLaw.isBoss(t) ? 20 : 60, 0);
                Nullification.bleed(t, 0.03);
                if (n++ < 6) b.vfx(p, BURST, mid(t), mid(t), 18, 1f);
            }
            Nullification.shatterBarriers(p.serverLevel(), end, 4, p);
            b.vfx(p, BURST, end, end, 28, 1.8f);
        });
        return true;
    }

    // ------------------------------------------------------------------ crystal resonance
    /** Crystal Resonance (daily): the whole area hums on one note, three pulses; allies are mended and shielded, foes shaken apart. */
    static boolean resonance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 10 * GrimoireBook.size(i, p);
        int[] pulse = {0};
        b.castCircle(p, 1.6f);
        b.vfx(p, ZONE, p.position(), p.position(), 50, (float) r);
        SpellRuntime.zone(p.serverLevel(), 45, 15, age -> {
            if (p.isRemoved() || !p.isAlive()) return;
            int k = pulse[0]++;
            Vec3 c = p.position();
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, 6f);
                effect(t, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                if (k == 2) effect(t, MobEffects.WEAKNESS, 100, 1);
            }
            if (k == 0) {
                for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r), x -> x == p || x.isAlliedTo(p))) {
                    BalanceLaw.heal(a, a.getMaxHealth() * 0.3f);
                    effect(a, MobEffects.ABSORPTION, 400, 1);
                    effect(a, MobEffects.REGENERATION, 100, 1);
                }
            }
            b.vfx(p, BURST, c, c, 20, (float) (r / 6) + 0.4f * k);
        });
        return true;
    }
}
