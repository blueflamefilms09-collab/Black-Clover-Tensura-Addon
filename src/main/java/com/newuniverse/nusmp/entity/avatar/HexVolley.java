package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.VanicaMoves;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One volley of hex bolts: small, fast bolts of the Avatar's colour that leave the Avatar two ticks apart, bend a little towards the foe
 * they were thrown at, stop at the first wall and hurt the first foe they touch for {@link AvatarTable#BOLT} (magic: armour does not help).
 * A bolt is a point, not an entity (the way Vanica's lances and Dante's rock swords are): one guarded zone step a tick for the whole volley,
 * ended by the bolts' own life. One victim takes at most {@link AvatarMath#BOLT_MAX_HITS} bolts of a volley (the rest fly on), and when the
 * volley is over every victim it landed on is left with a Curse of Decay as deep as the bolts that landed ({@link AvatarMath#hexLevel}),
 * branded by Megicula's own sigil ({@link VanicaMoves#weakeningCurse}): leave the sigil, cleanse it or fight with anti-magic and it breaks.
 */
final class HexVolley {
    /** One bolt in flight. */
    private static final class Bolt {
        final LivingEntity aim;
        Vec3 pos, vel;
        int life = AvatarMath.BOLT_LIFE;
        boolean done;

        Bolt(LivingEntity aim, Vec3 pos, Vec3 vel) {
            this.aim = aim;
            this.pos = pos;
            this.vel = vel;
        }
    }

    private final DevilAvatarBoss boss;
    private final List<LivingEntity> aims;
    private final Bolt[] bolts;
    private final VolleyBudget budget = new VolleyBudget(AvatarMath.BOLT_MAX_HITS);
    private final Map<Integer, Integer> landed = new LinkedHashMap<>();
    private final Map<Integer, LivingEntity> victims = new LinkedHashMap<>();
    private final boolean brand;
    private boolean finished;

    /** A volley with one bolt for each entry of 'aims' (the same foe may be listed more than once). 'brand': leave the curse on whoever the bolts land on. */
    HexVolley(DevilAvatarBoss boss, List<LivingEntity> aims, boolean brand) {
        this.boss = boss;
        this.aims = new ArrayList<>(aims);
        this.bolts = new Bolt[this.aims.size()];
        this.brand = brand;
    }

    /** Throws the volley: one guarded zone step a tick for as long as its last bolt can live. */
    void launch(ServerLevel sl) {
        if (aims.isEmpty()) return;
        AvatarFx.sound(sl, boss.position(), SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.7f);
        SpellRuntime.zone(sl, AvatarMath.volleyTicks(aims.size()), 1, AvatarFx.safe("hex volley", age -> step(sl, age)));
    }

    private void step(ServerLevel sl, int age) {
        if (finished) return;
        if (!boss.isAlive() || boss.isRemoved()) {                               // the boss fell: its bolts go out with it
            finished = true;
            return;
        }
        boolean allDone = age >= (bolts.length - 1) * AvatarMath.BOLT_STAGGER;
        for (int i = 0; i < bolts.length; i++) {
            if (bolts[i] == null && age >= i * AvatarMath.BOLT_STAGGER) bolts[i] = leave(i);
            if (bolts[i] == null) allDone = false;
            else if (!bolts[i].done) {
                fly(sl, bolts[i]);
                if (!bolts[i].done) allDone = false;
            }
        }
        if (allDone) finish();
    }

    /** Bolt 'i' leaves the Avatar: from its raised hands, fanned a little by its place in the volley, aimed at its foe. */
    private Bolt leave(int i) {
        int n = bolts.length;
        double fan = (i - (n - 1) / 2.0);
        Vec3 face = AvatarFx.facing(boss);
        Vec3 side = new Vec3(-face.z, 0, face.x);
        Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.78 + 0.25 * (i % 2), 0).add(side.scale(fan * 0.35)).add(face.scale(0.5));
        LivingEntity aim = aims.get(i);
        Vec3 want = aim != null && aim.isAlive() ? aim.getBoundingBox().getCenter().subtract(from) : face;
        if (want.lengthSqr() < 1e-4) want = face;
        Vec3 dir = AvatarFx.rotateY(want.normalize(), fan * 0.07);
        return new Bolt(aim, from, dir.scale(AvatarMath.BOLT_SPEED));
    }

    private void fly(ServerLevel sl, Bolt b) {
        if (b.aim != null && b.aim.isAlive()) {                                  // it bends towards its foe, a little more each tick
            Vec3 want = b.aim.getBoundingBox().getCenter().subtract(b.pos);
            if (want.lengthSqr() > 1e-4) {
                Vec3 v = b.vel.scale(1 - AvatarMath.BOLT_TURN).add(want.normalize().scale(AvatarMath.BOLT_SPEED * AvatarMath.BOLT_TURN));
                double l = v.length();
                if (l > 1e-4) b.vel = v.scale(AvatarMath.BOLT_SPEED / l);
            }
        }
        Vec3 next = b.pos.add(b.vel);
        BlockHitResult wall = sl.clip(new ClipContext(b.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        if (blocked) next = wall.getLocation();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, next).inflate(AvatarMath.BOLT_REACH), boss::isFoe)) {
            if (!budget.tryHit(e.getId())) continue;                             // this victim has had its share of the volley: the bolt flies on
            if (boss.strike(e, AvatarTable.BOLT, true)) {
                landed.merge(e.getId(), 1, Integer::sum);
                victims.put(e.getId(), e);
                e.knockback(0.15, -b.vel.x, -b.vel.z);
                e.hurtMarked = true;
            }
            burst(sl, next);
            b.done = true;
            return;
        }
        b.pos = next;
        trail(sl, b);
        if (blocked || --b.life <= 0) {
            burst(sl, b.pos);
            b.done = true;
        }
    }

    private void trail(ServerLevel sl, Bolt b) {
        Vec3 d = b.vel.normalize();
        Vec3 tail = b.pos.subtract(d.scale(0.7));
        sl.sendParticles(AvatarFx.dust(AvatarFx.accent(boss), 1.0f), b.pos.x, b.pos.y, b.pos.z, 2, 0.04, 0.04, 0.04, 0.0);
        sl.sendParticles(AvatarFx.dust(AvatarFx.rgb(boss), 1.3f), tail.x, tail.y, tail.z, 2, 0.06, 0.06, 0.06, 0.0);
        if (b.life % 3 == 0) sl.sendParticles(ParticleTypes.SQUID_INK, tail.x, tail.y, tail.z, 1, 0.05, 0.05, 0.05, 0.01);
    }

    private void burst(ServerLevel sl, Vec3 at) {
        sl.sendParticles(AvatarFx.dust(AvatarFx.accent(boss), 1.5f), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.05);
        AvatarFx.sound(sl, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 0.7f);
    }

    /** The volley is over: whoever the bolts landed on is left with a Curse of Decay as deep as the bolts that landed (one sigil for a cluster of victims). */
    private void finish() {
        if (finished) return;
        finished = true;
        if (!brand || !boss.isAlive() || boss.isRemoved()) return;
        List<Vec3> sigils = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : landed.entrySet()) {
            LivingEntity v = victims.get(e.getKey());
            int level = AvatarMath.hexLevel(e.getValue(), boss.power());
            if (v == null || !v.isAlive() || level <= 0) continue;
            boolean covered = false;
            for (Vec3 c : sigils) if (c.distanceToSqr(v.position()) < 9.0) covered = true;
            if (covered) continue;
            sigils.add(v.position());
            VanicaMoves.weakeningCurse(boss, v, AvatarMath.HEX_BRAND_TICKS, level);
        }
    }
}
