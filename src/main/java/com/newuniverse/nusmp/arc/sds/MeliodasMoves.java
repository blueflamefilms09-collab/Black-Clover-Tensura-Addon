package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the strikes of Meliodas's moves (the cuts, the dash, the crescent shots, the black-flame field and the eruptions of the Purgatory, the images and the
 * rush, the nova), as plain static functions on a {@link ZogratisBoss}, so that the boss class holds only his rules (Full Counter, the Demon Mark, the Wrath's
 * feeding) and a reader can see in one place what every blow does and what dodges it. Every blow goes through the boss's damage law
 * ({@link SdsaStrikes#hit}), every zone is bounded in time and in the number of foes it looks at, a step that throws only ends its own zone, and nothing
 * changes a block. The dodge of each one is a rule the telegraph shows: leave the cone, the strip or the ring, jump the low wave, or guard.
 */
public final class MeliodasMoves {
    private MeliodasMoves() {}

    /** Colours (0xRRGGBB): the black flame is drawn dark crimson, because a black that is really black cannot be seen. */
    static final int HELL = 0xB01838, EMBER = 0xFF5E6E, WARN = 0xFF4A4A;
    private static final ResourceKey<DamageType> SWORD = TensuraDamageTypes.AURA_SLASH, FLAME = TensuraDamageTypes.BLACK_FLAME;

    // ================================================================ the sword
    /** A cut in a cone in front of him: Lostvayne Slash and Kamichigiri. Returns how many it hit. */
    static int cone(ZogratisBoss b, ServerLevel sl, Vec3 facing, double reach, double halfAngle, float damage, double knock, VfxShape shape, float shapePower) {
        Vec3 apex = b.position(), chest = apex.add(0, b.getBbHeight() * 0.6, 0);
        SdsaStrikes.fx(sl, shape, chest, chest.add(facing.scale(reach)), HELL, 10, shapePower, true);
        SdsaStrikes.sound(sl, apex, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.65f);
        int n = 0;
        for (LivingEntity e : SdsaStrikes.foesInCone(b, apex, facing, reach, halfAngle)) {
            if (SdsaStrikes.covered(sl, chest, e)) continue;
            if (SdsaStrikes.hit(b, e, damage, SWORD)) {
                SdsaStrikes.push(e, apex, knock, 0.08);
                n++;
            }
        }
        return n;
    }

    /**
     * The dash of the lunge: along {@code dir} for up to {@code length} (shorter if a wall or a gap is in the way), cutting everything in the strip. He stops a
     * body's width short of the end, so he never ends up inside the one he hit.
     */
    static void dash(ZogratisBoss b, ServerLevel sl, Vec3 dir, double length, double halfWidth, float damage) {
        Vec3 from = b.position(), eye = from.add(0, 1.0, 0);
        Vec3 want = SdsaStrikes.clipEnd(sl, b, eye, eye.add(dir.scale(length)));
        double reach = Math.max(0.0, SdsaStrikes.along(from, dir, want));
        Vec3 stop = ZogratisBoss.groundOrNull(sl, from.add(dir.scale(Math.max(0.0, reach - 1.0))));
        SdsaStrikes.fx(sl, VfxShape.DARK_IAI, eye, from.add(dir.scale(reach)).add(0, 1.0, 0), EMBER, 8, 1.8f, true);
        SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.8f);
        for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, reach + 0.5, halfWidth)) {
            if (SdsaStrikes.hit(b, e, damage, SWORD)) SdsaStrikes.push(e, from, 0.6, 0.1);
        }
        if (stop != null && reach >= 1.8) SdsaStrikes.dashTo(b, stop);
    }

    /** One crescent of Lostvayne Wave: a low shot along the floor that hurts the first foe it touches. */
    static void crescent(ZogratisBoss b, ServerLevel sl, Vec3 start, Vec3 dir, float damage) {
        SdsaStrikes.fx(sl, VfxShape.SLASH_WAVE, start, start.add(dir.scale(12)), HELL, 14, 2.0f, true);
        SdsaStrikes.shoot(b, sl, start, dir, 1.5, 24, 0.7, damage, SWORD,
                (e, at) -> {
                    Tells.burst(sl, at, EMBER, 12);
                    SdsaStrikes.push(e, start, 0.5, 0.05);
                },
                p -> Tells.gather(sl, p, 0.12, HELL, 2));
    }

    // ================================================================ the black flame
    /**
     * A Hellblaze field: a disc of black flame on the floor that burns whoever stands in it every half second and keeps burning them a while after they
     * leave. The flame is not put out by water; walking out of it is the only way, and it fades with distance in time, not in space.
     */
    static void field(ZogratisBoss b, ServerLevel sl, Vec3 centre, double radius, int ticks, float burn, int linger) {
        final Map<Integer, Long> burning = new HashMap<>();
        SdsaStrikes.fx(sl, VfxShape.FIRE_PILLAR, centre, centre.add(0, 1, 0), HELL, 28, (float) radius, true);
        SdsaStrikes.sound(sl, centre, SoundEvents.BLAZE_SHOOT, 1.6f, 0.5f);
        SpellRuntime.zone(sl, ticks, 5, SdsaStrikes.safe("hellblaze field", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            long now = sl.getGameTime();
            if (age % 10 == 0) {
                Tells.ring(sl, centre, radius, HELL, 1.5f, (int) Math.min(80, 14 + radius * 5));
                Tells.gather(sl, centre.add(0, 0.5, 0), radius * 0.6, EMBER, 6);
                if (age % 40 == 0) SdsaStrikes.fx(sl, VfxShape.FIRE_WILD, centre, centre.add(0, 1, 0), HELL, 30, (float) radius, true);
            }
            if (age % 10 == 0) {
                Set<Integer> inside = new HashSet<>();
                for (LivingEntity e : SdsaStrikes.foesInDisc(b, centre, radius)) {
                    inside.add(e.getId());
                    burning.put(e.getId(), now + linger);
                    SdsaStrikes.hit(b, e, burn, FLAME);
                }
                if (age % 20 == 0) {
                    for (LivingEntity e : b.foes(60)) {
                        Long until = burning.get(e.getId());
                        if (until != null && until > now && !inside.contains(e.getId())) SdsaStrikes.hit(b, e, burn * 0.5f, FLAME);
                    }
                }
            }
        }));
    }

    /** Hellblaze Burst: a dome of black flame round him that shoves everything inside outward. */
    static void burst(ZogratisBoss b, ServerLevel sl, double radius, float damage) {
        Vec3 c = b.position();
        SdsaStrikes.fx(sl, VfxShape.FIRE_BURST, c, c.add(0, 1, 0), HELL, 22, (float) radius, true);
        SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, c, c.add(0, 1, 0), EMBER, 18, (float) radius * 0.8f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.FIRECHARGE_USE, 1.8f, 0.5f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, c, radius)) {
            if (SdsaStrikes.hit(b, e, damage * (SdsaStrikes.covered(sl, c.add(0, 1, 0), e) ? 0.4f : 1f), FLAME)) SdsaStrikes.push(e, c, 1.1, 0.25);
        }
    }

    // ================================================================ the steps
    /** Demon Step: gone from here and cutting in a ring where the mark is. */
    static void step(ZogratisBoss b, ServerLevel sl, Vec3 mark, double radius, float damage) {
        Vec3 from = b.position();
        SdsaStrikes.fx(sl, VfxShape.SPATIAL_RIFT, from.add(0, 1, 0), from.add(0, 1.5, 0), HELL, 12, 0.8f, true);
        boolean moved = SdsaStrikes.dashTo(b, mark);
        Vec3 now = moved ? mark : b.position();
        SdsaStrikes.fx(sl, VfxShape.SPATIAL_RIFT, now.add(0, 1, 0), now.add(0, 1.5, 0), HELL, 12, 0.8f, false);
        SdsaStrikes.fx(sl, VfxShape.SLASH_SCYTHE, now.add(0, 1, 0), now.add(0, 1, 1), EMBER, 12, (float) radius, true);
        SdsaStrikes.sound(sl, now, SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.7f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, now, radius)) {
            if (SdsaStrikes.hit(b, e, damage, SWORD)) SdsaStrikes.push(e, now, 0.8, 0.12);
        }
    }

    /**
     * The Lostvayne images: each stands on its spot, then one after the other (every {@link DemonKingMath#IMAGE_GAP} ticks) cuts a strip across the whole
     * ring through its middle. The strips were shown for the whole wind-up, so the safe places are between them and far enough from the centre.
     */
    static void images(ZogratisBoss b, ServerLevel sl, List<Vec3> spots, Vec3 centre, double halfWidth, float damage) {
        for (int i = 0; i < spots.size(); i++) {
            final Vec3 from = spots.get(i);
            SdsaStrikes.fx(sl, VfxShape.BUTOH_AFTERIMAGE, from, from.add(0, 1, 0), HELL, 20 + i * DemonKingMath.IMAGE_GAP, 1.6f, true);
            b.later(i * DemonKingMath.IMAGE_GAP, SdsaStrikes.safe("image cut", () -> {
                Vec3 dir = SdsaStrikes.unitFlat(centre.subtract(from), new Vec3(1, 0, 0));
                double len = from.distanceTo(centre) * 2.0;
                SdsaStrikes.fx(sl, VfxShape.DARK_IAI, from.add(0, 1, 0), from.add(dir.scale(len)).add(0, 1, 0), EMBER, 8, 1.6f, true);
                SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.9f);
                for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, len, halfWidth)) {
                    if (SdsaStrikes.hit(b, e, damage, SWORD)) SdsaStrikes.push(e, from, 0.5, 0.1);
                }
            }));
        }
    }

    /** The Assault Rush: dashes, 12 ticks apart, to the spots the wind-up showed; each cuts the strip it travels along and stops a body short of the spot. */
    static void rush(ZogratisBoss b, ServerLevel sl, List<Vec3> spots, double halfWidth, float damage) {
        for (int k = 0; k < spots.size(); k++) {
            final Vec3 to = spots.get(k);
            b.later(k * 12, SdsaStrikes.safe("rush dash", () -> {
                Vec3 from = b.position();
                Vec3 dir = SdsaStrikes.unitFlat(to.subtract(from), new Vec3(1, 0, 0));
                double len = Math.min(26.0, SdsaStrikes.along(from, dir, to));
                if (len < 0.8) return;
                Vec3 eye = from.add(0, 1, 0);
                SdsaStrikes.fx(sl, VfxShape.DARK_IAI, eye, from.add(dir.scale(len)).add(0, 1, 0), EMBER, 8, 1.8f, true);
                SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.3f, 1.0f);
                for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, len + 0.5, halfWidth)) {
                    if (SdsaStrikes.hit(b, e, damage, SWORD)) SdsaStrikes.push(e, from, 0.6, 0.1);
                }
                Vec3 stop = ZogratisBoss.groundOrNull(sl, from.add(dir.scale(Math.max(0.0, len - 1.0))));
                if (stop != null && len >= 1.8) SdsaStrikes.dashTo(b, stop);
            }));
        }
    }

    // ================================================================ the ultimates
    /**
     * Hellblaze: Purgatory. A column of black flame rises on every mark the wind-up showed (a shield or anti-magic cuts the blow, a jump does not help),
     * and each leaves a short field behind. A foe is hurt once, however many columns overlap.
     */
    static void purgatory(ZogratisBoss b, ServerLevel sl, List<Vec3> marks, double radius, float damage, float burn, int linger) {
        Set<Integer> hit = new HashSet<>();
        SdsaStrikes.sound(sl, b.position(), SoundEvents.WARDEN_SONIC_BOOM, 2.5f, 0.5f);
        for (Vec3 m : marks) {
            SdsaStrikes.fx(sl, VfxShape.FIRE_PILLAR, m, m.add(0, 1, 0), HELL, 36, (float) radius * 1.3f, true);
            SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, m, m.add(0, 1, 0), EMBER, 20, (float) radius, true);
            for (LivingEntity e : SdsaStrikes.foesInDisc(b, m, radius)) {
                if (!hit.add(e.getId())) continue;
                if (SdsaStrikes.hit(b, e, damage * SdsaStrikes.guard(e), FLAME)) SdsaStrikes.push(e, m, 0.7, 0.3);
            }
            field(b, sl, m, radius * 0.8, 60, burn, linger);
        }
    }

    /** The Demon King's Wrath, the detonation: everything inside the ring takes the blow (a shield or anti-magic cuts it), and is thrown out. */
    static void nova(ZogratisBoss b, ServerLevel sl, Vec3 centre, double radius, float damage) {
        SdsaStrikes.fx(sl, VfxShape.FIRE_WILD, centre, centre.add(0, 1, 0), HELL, 40, (float) radius, true);
        SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, centre, centre.add(0, 1, 0), EMBER, 24, (float) radius, true);
        SdsaStrikes.fx(sl, VfxShape.SPACE_SHATTER, centre.add(0, 1, 0), centre.add(0, 2, 0), HELL, 20, (float) radius * 0.6f, true);
        SdsaStrikes.sound(sl, centre, SoundEvents.ENDER_DRAGON_GROWL, 3.0f, 0.5f);
        SdsaStrikes.sound(sl, centre, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.4f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, centre, radius)) {
            if (SdsaStrikes.hit(b, e, damage * SdsaStrikes.guard(e), FLAME)) SdsaStrikes.push(e, centre, 1.4, 0.4);
        }
    }
}
