package com.newuniverse.nusmp.heroweapon.weapon.staff;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Geometry;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Timed;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.newuniverse.nusmp.heroweapon.weapon.staff.StaffNumbers.*;

/**
 * Why: the bodies of the staff's ten techniques. Each is a hitscan line, a field, a ring or a buff drawn from the wielder's look direction, so there is no
 * entity to spawn, track or despawn; everything that lands later (a rune pulse, a salvo, a ring) goes through {@link Timed}, which re-checks the wielder when it
 * runs. All damage goes through {@code HeroCtx.hit} (the balance law: EP scaling, the cap on players, the boss multiplier) with the staff's mark bonus applied
 * to the RAW number first, all healing through {@code HeroCtx.heal}, nothing breaks or places a block, and every technique above 8 raw shows a ring or a sigil
 * before it lands. The numbers are {@link StaffNumbers}'; the colours are the staff's violet-blue.
 */
final class StaffSpells {
    private StaffSpells() {}

    /** The staff's colour: 0xRRGGBB for the ground rings, 0xAARRGGBB for the effects. */
    static final int RGB = 0x7B8CFF, ARGB = 0xFF7B8CFF;

    // ================================================================ marks and shared pieces
    private static boolean isMarked(HeroCtx c, LivingEntity t) { return StaffState.marked(c.player.getUUID(), t.getUUID(), c.level.getGameTime()); }

    /** The raw damage of one application on {@code t}: the base plus the mark bonus when the foe carries one of this wielder's marks. */
    static float raw(HeroCtx c, LivingEntity t, double base) { return (float) StaffNumbers.marked(base, isMarked(c, t)); }

    /** The point under the crosshair within {@code reach}, dropped to the floor beneath it (a field lies on the ground, never in the air). */
    static Vec3 groundAim(HeroCtx c, double reach) {
        Vec3 p = c.aim(reach);
        Rays.Reach down = Rays.reach(c, p.add(0.0, 0.6, 0.0), new Vec3(0.0, -1.0, 0.0), 10.0);
        return down.end().add(0.0, 0.05, 0.0);
    }

    private static void cast(HeroCtx c) { c.player.swing(InteractionHand.MAIN_HAND); }

    // ================================================================ Arcane Bolt and the prism
    static boolean arcaneBolt(HeroCtx c) {
        boolean sharp = c.form.has("sharp_focus");
        double base = ARCANE_BOLT.rawBase() + (sharp ? SHARP_BONUS : 0.0);
        cast(c);
        c.sound(SoundEvents.EVOKER_CAST_SPELL, 0.7f, 1.6f);
        bolt(c, c.eye(), c.look(), base, sharp ? BOLT_PIERCE_SHARP : BOLT_PIERCE, new HashSet<>(), true);
        return true;
    }

    /**
     * One hitscan bolt from {@code from} along {@code dir}: it stops at a wall, strikes up to {@code pierce} foes not in {@code already} (they are added to it) and
     * ends at the last of them. When it passes through the wielder's prism, the prism splits it.
     */
    static List<LivingEntity> bolt(HeroCtx c, Vec3 from, Vec3 dir, double base, int pierce, Set<UUID> already, boolean throughPrism) {
        Rays.Reach reach = Rays.reach(c, from, dir, BOLT_RANGE);
        List<LivingEntity> line = Foes.along(c, from, reach.end(), BOLT_WIDTH, Budget.MAX_TARGETS);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity t : line) {
            if (struck.size() >= pierce) break;
            if (already.contains(t.getUUID())) continue;
            if (c.hit(t, raw(c, t, base))) {
                struck.add(t);
                already.add(t.getUUID());
            }
        }
        Vec3 tip = struck.size() >= pierce ? struck.get(struck.size() - 1).getBoundingBox().getCenter() : reach.end();
        c.fx(VfxShape.LIGHTNING_SPEAR, from, tip, ARGB, 8, 0.7f);
        if (throughPrism) {
            StaffState.Prism prism = StaffState.prism(c.player.getUUID(), c.level, c.level.getGameTime());
            if (prism != null && Geometry.distToSegmentSq(from.x, from.y, from.z, tip.x, tip.y, tip.z, prism.pos.x, prism.pos.y, prism.pos.z) <= PRISM_RADIUS * PRISM_RADIUS) {
                prism.charges--;
                split(c, prism, base, already);
            }
        }
        return struck;
    }

    /** The prism splits a bolt: up to three (four with Wide Prism) copies, each at a different foe near the prism that it can see, at 60 percent of the raw damage. */
    private static void split(HeroCtx c, StaffState.Prism prism, double base, Set<UUID> already) {
        int copies = c.form.has("wide_prism") ? PRISM_COPIES_WIDE : PRISM_COPIES;
        int made = 0;
        for (LivingEntity t : Foes.around(c, prism.pos, PRISM_COPY_RANGE, Budget.MAX_TARGETS)) {
            if (made >= copies) break;
            if (already.contains(t.getUUID())) continue;
            Vec3 aim = t.getBoundingBox().getCenter();
            if (Rays.reach(c, prism.pos, aim.subtract(prism.pos), prism.pos.distanceTo(aim)).wall()) continue;
            if (c.hit(t, raw(c, t, base * PRISM_COPY_SHARE))) {
                already.add(t.getUUID());
                made++;
                c.fx(VfxShape.LIGHTNING_SPEAR, prism.pos, aim, ARGB, 8, 0.5f);
            }
        }
        c.fx(VfxShape.LIGHT_FLARE, prism.pos, prism.pos, ARGB, 10, 1.0f);
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.8f);
    }

    static boolean magicPrism(HeroCtx c) {
        Vec3 eye = c.eye();
        double d = Geometry.clamp(eye.distanceTo(c.aim(PRISM_REACH)) - 0.4, 3.0, PRISM_REACH);
        Vec3 pos = eye.add(c.look().scale(d));
        int life = PRISM_TICKS + (c.form.has("wide_prism") ? PRISM_WIDE_TICKS : 0);
        long now = c.level.getGameTime();
        StaffState.setPrism(c.player.getUUID(), new StaffState.Prism(c.level, pos, now + life, PRISM_CHARGES));
        cast(c);
        c.fx(VfxShape.SEAL_ORBIT, pos, pos, ARGB, life, (float) PRISM_RADIUS);
        c.fx(VfxShape.LIGHT_FLARE, pos, pos, ARGB, 12, 0.8f);
        c.sound(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.9f);
        return true;
    }

    // ================================================================ Chain Spark
    static boolean chainSpark(HeroCtx c) {
        LivingEntity first = Foes.aimed(c, CHAIN_RANGE, CHAIN_WIDTH);
        if (first == null) {
            c.say("Chain Spark needs a foe in your line of sight");
            return false;
        }
        Vec3 start = first.getBoundingBox().getCenter();
        List<LivingEntity> pool = new ArrayList<>(Foes.around(c, start, CHAIN_POOL, Budget.MAX_TARGETS));
        pool.remove(first);
        double[][] points = new double[pool.size()][];
        for (int i = 0; i < points.length; i++) {
            Vec3 p = pool.get(i).getBoundingBox().getCenter();
            points[i] = new double[]{p.x, p.y, p.z};
        }
        List<LivingEntity> order = new ArrayList<>();
        order.add(first);
        for (int index : Geometry.chain(start.x, start.y, start.z, points, CHAIN_JUMPS, CHAIN_JUMP)) order.add(pool.get(index));
        cast(c);
        Vec3 from = c.eye();
        for (LivingEntity t : order) {
            Vec3 to = t.getBoundingBox().getCenter();
            c.hit(t, raw(c, t, CHAIN_SPARK.rawBase()));
            c.fx(VfxShape.FX_LIGHTNING_ARC, from, to, ARGB, 10, 0.8f);
            from = to;
        }
        c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.35f, 2.0f);
        return true;
    }

    // ================================================================ Rune Field
    static boolean runeField(HeroCtx c) {
        Vec3 center = groundAim(c, RUNE_REACH);
        cast(c);
        Tells.ring(c.level, center, RUNE_RADIUS, RGB);
        c.fx(VfxShape.SEAL_BASIC, center, center.add(0.0, 1.0, 0.0), ARGB, RUNE_TELL + (RUNE_PULSES - 1) * RUNE_INTERVAL + 12, (float) RUNE_RADIUS);
        c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        Timed.later(c, RUNE_TELL, live -> Timed.zone(live, (RUNE_PULSES - 1) * RUNE_INTERVAL, RUNE_INTERVAL, (cc, age) -> runePulse(cc, center)));
        return true;
    }

    private static void runePulse(HeroCtx c, Vec3 center) {
        for (LivingEntity t : Foes.around(c, center, RUNE_RADIUS, Budget.MAX_TARGETS)) {
            if (c.hit(t, raw(c, t, RUNE_FIELD.rawBase()))) Status.slow(t, RUNE_SLOW_TICKS, 1);
        }
        c.fx(VfxShape.MAGIC_CIRCLE_EXPLOSION, center, center.add(0.0, 1.0, 0.0), ARGB, 14, (float) (RUNE_RADIUS * 0.5));
        c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.3f);
    }

    // ================================================================ Arcane Burst
    static boolean arcaneBurst(HeroCtx c) {
        Vec3 at = c.player.position();
        cast(c);
        Tells.ring(c.level, at, BURST_RADIUS, RGB);
        c.fx(VfxShape.WIND_RING, at.add(0.0, 0.2, 0.0), at.add(0.0, 1.0, 0.0), ARGB, 20, (float) BURST_RADIUS);
        c.sound(SoundEvents.EVOKER_CAST_SPELL, 0.9f, 0.8f);
        Timed.later(c, BURST_DELAY, live -> {
            Vec3 here = live.player.position();
            for (LivingEntity t : Foes.around(live, here, BURST_RADIUS, Budget.MAX_TARGETS)) {
                if (live.hit(t, raw(live, t, ARCANE_BURST.rawBase()))) live.shove(t, here, BURST_PUSH);
            }
            live.fx(VfxShape.MAGIC_CIRCLE_EXPLOSION, here.add(0.0, 0.1, 0.0), here.add(0.0, 1.0, 0.0), ARGB, 16, (float) (BURST_RADIUS * 0.6));
            live.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
        });
        return true;
    }

    // ================================================================ Float Mirror
    static boolean floatMirror(HeroCtx c) {
        boolean deep = c.form.has("deep_mirror");
        int life = MIRROR_TICKS + (deep ? MIRROR_DEEP_TICKS : 0);
        long now = c.level.getGameTime();
        StaffState.setMirror(c.player.getUUID(), new StaffState.Mirror(c.level, now + life, deep ? MIRROR_DEEP_NEGATE : MIRROR_NEGATE, MIRROR_RETURNS));
        cast(c);
        c.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
        Timed.zone(c, life, MIRROR_INTERVAL, (cc, age) -> mirrorStep(cc, age));
        return true;
    }

    private static void mirrorStep(HeroCtx c, int age) {
        StaffState.Mirror m = StaffState.mirror(c.player.getUUID(), c.level, c.level.getGameTime());
        if (m == null) return;
        Vec3 look = c.look();
        Vec3 center = c.eye().add(look.scale(MIRROR_REACH));
        if (age % 10 == 0) c.fx(VfxShape.MIRROR_PANE, center, center.add(look), ARGB, 12, (float) (MIRROR_SIZE * 0.5));
        if (m.negated >= m.negateMax) return;
        AABB box = new AABB(center, center).inflate(MIRROR_SIZE);
        for (Projectile shot : c.level.getEntitiesOfClass(Projectile.class, box, p -> incoming(c, p))) {
            if (m.negated >= m.negateMax) break;
            Entity owner = shot.getOwner();
            shot.discard();
            m.negated++;
            c.fx(VfxShape.MIRROR_PANE, shot.position(), center, ARGB, 8, 0.6f);
            if (m.returned < m.returnMax && owner instanceof LivingEntity shooter && Foes.isFoe(c.player, shooter)
                    && shooter.distanceToSqr(center) <= MIRROR_RETURN_RANGE * MIRROR_RETURN_RANGE) {
                m.returned++;
                if (c.hit(shooter, (float) FLOAT_MIRROR.rawBase())) c.fx(VfxShape.MIRROR_RAY, center, shooter.getBoundingBox().getCenter(), ARGB, 10, 0.6f);
            }
            c.sound(SoundEvents.SHIELD_BLOCK, 0.7f, 1.7f);
        }
    }

    /** A shot that flies towards the wielder and was not fired by the wielder or an ally. */
    private static boolean incoming(HeroCtx c, Projectile shot) {
        if (!shot.isAlive()) return false;
        Entity owner = shot.getOwner();
        if (owner == c.player || (owner instanceof LivingEntity le && Foes.isAlly(c.player, le))) return false;
        Vec3 v = shot.getDeltaMovement();
        return v.lengthSqr() > 0.01 && v.dot(c.eye().subtract(shot.position())) > 0.0;
    }

    // ================================================================ Fenrir Rod
    static boolean fenrirRod(HeroCtx c) {
        long now = c.level.getGameTime();
        StaffState.rhythm(c.player.getUUID()).haste(now + HASTE_TICKS);
        cast(c);
        c.fx(VfxShape.MANA_CHARGE, c.player.position().add(0.0, 1.0, 0.0), c.player.position().add(0.0, 1.0, 0.0), ARGB, 30, 1.0f);
        VfxSpawn.sendFollowing(c.level, VfxShape.WIND_ZEPHYR, c.player, c.player.position(), ARGB, HASTE_TICKS, 0.8f);
        c.sound(SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.8f);
        c.say("Fenrir Rod: your chant quickens");
        return true;
    }

    // ================================================================ Analyze Magic
    static boolean analyze(HeroCtx c) {
        List<LivingEntity> seen = Foes.cone(c, ANALYZE_RANGE, ANALYZE_HALF_ANGLE, ANALYZE.maxTargets());
        if (seen.isEmpty()) {
            c.say("Analyze Magic finds nothing in front of you");
            return false;
        }
        long now = c.level.getGameTime();
        int life = MARK_TICKS + (c.form.has("star_mark") ? MARK_EXTRA_TICKS : 0);
        cast(c);
        for (LivingEntity t : seen) {
            StaffState.mark(c.player.getUUID(), t.getUUID(), now + life, now);
            Vec3 at = t.position().add(0.0, t.getBbHeight() * 0.5, 0.0);
            c.fx(VfxShape.COMPASS_LOCK, at, at, ARGB, 24, 0.9f);
        }
        c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.2f);
        return true;
    }

    // ================================================================ Healing Light
    static boolean healingLight(HeroCtx c) {
        Vec3 at = c.player.position();
        cast(c);
        c.fx(VfxShape.ELF_CIRCLE, at.add(0.0, 0.1, 0.0), at.add(0.0, 1.0, 0.0), ARGB, 40, (float) (HEAL_RADIUS * 0.5));
        for (LivingEntity t : Foes.alliesAround(c, at, HEAL_RADIUS, HEALING_LIGHT.maxTargets())) {
            c.heal(t, (float) (t.getMaxHealth() * HEALING_LIGHT.heal()));
            Status.cleanse(t);
            if (t != c.player) c.fx(VfxShape.SEAL_WOUND, c.eye(), t.getBoundingBox().getCenter(), ARGB, 20, 0.6f);
        }
        c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.6f);
        return true;
    }

    // ================================================================ Perpetual Bombardment (ultimate)
    static boolean bombardment(HeroCtx c) {
        Vec3 center = groundAim(c, BOMB_REACH);
        cast(c);
        Tells.ring(c.level, center, BOMB_RADIUS, RGB);
        Tells.column(c.level, center, 6.0, RGB);
        c.fx(VfxShape.DIVINE_SIGIL, center, center.add(0.0, 1.0, 0.0), ARGB, BOMB_TELL + (BOMB_SALVOS - 1) * BOMB_INTERVAL + 10, (float) BOMB_RADIUS);
        c.sound(SoundEvents.BEACON_ACTIVATE, 1.2f, 0.6f);
        Timed.later(c, BOMB_TELL, live -> Timed.zone(live, (BOMB_SALVOS - 1) * BOMB_INTERVAL, BOMB_INTERVAL, (cc, age) -> salvo(cc, center)));
        return true;
    }

    private static void salvo(HeroCtx c, Vec3 center) {
        for (LivingEntity t : Foes.around(c, center, BOMB_RADIUS, Budget.MAX_TARGETS)) c.hit(t, raw(c, t, BOMBARDMENT.rawBase()));
        for (int i = 0; i < 3; i++) {
            double a = c.level.random.nextDouble() * Math.PI * 2.0, r = Math.sqrt(c.level.random.nextDouble()) * BOMB_RADIUS;
            Vec3 land = center.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
            c.fx(VfxShape.LIGHTNING_SPEAR, land.add(0.0, 14.0, 0.0), land, ARGB, 8, 1.0f);
            c.fx(VfxShape.LIGHT_FLARE, land, land, ARGB, 8, 0.6f);
        }
        c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
    }
}
