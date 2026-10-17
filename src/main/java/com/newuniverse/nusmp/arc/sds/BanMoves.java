package com.newuniverse.nusmp.arc.sds;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Why: the strikes of Ban's moves (the three-hit staff combo, the sweep, the extended thrust, the Fox Step, Hunter Fest, the Fountain's geysers and the Fox's
 * Hunt) as plain static functions on a {@link ZogratisBoss}, so that the boss class holds only his rules (the Fountain of Youth, the Snatch ledger, the
 * Concentration) and a reader can see in one place what every blow does and what dodges it. A task that waits some ticks (the combo's later cuts, the hunt's later dashes) asks the caller whether he has fallen in the meantime. Courechouse is steel, so its blows are the boss's plain physical
 * blow (a raised shield helps); the heart he takes and the water of the Fountain are typed Tensura damage. Every blow goes through the boss's damage law,
 * every zone is bounded in time and in the foes it looks at, a step that throws only ends its own task, and nothing changes a block.
 */
public final class BanMoves {
    private BanMoves() {}

    /** Colours (0xRRGGBB): the Fountain's green, its pale light, and the red of a warning. */
    static final int GREEN = 0x7BE0A4, PALE = 0xD9E8A0, WARN = 0xFF4A4A;
    private static final ResourceKey<DamageType> HEART = TensuraDamageTypes.HEART_EAT, WATER = TensuraDamageTypes.WATER_ELEMENTAL;

    // ================================================================ the staff
    /** The combo: three short cuts of the staff, six ticks apart, along the cone the wind-up showed. Returns nothing: each cut is its own blow. */
    static void combo(ZogratisBoss b, ServerLevel sl, Vec3 facing, double reach, double halfAngle, float damage, BooleanSupplier halted) {
        for (int k = 0; k < 3; k++) {
            final int cut = k;
            b.later(k * 6, SdsaStrikes.safe("staff combo", () -> {
                if (halted.getAsBoolean()) return;                       // he fell between two cuts
                Vec3 apex = b.position(), chest = apex.add(0, b.getBbHeight() * 0.6, 0);
                SdsaStrikes.fx(sl, VfxShape.SLASH_WAVE, chest, chest.add(facing.scale(reach)), GREEN, 8, 1.8f, true);
                SdsaStrikes.sound(sl, apex, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.1f + 0.1f * cut);
                for (LivingEntity e : SdsaStrikes.foesInCone(b, apex, facing, reach, halfAngle)) {
                    if (SdsaStrikes.covered(sl, chest, e)) continue;
                    if (b.strike(e, damage, false)) SdsaStrikes.push(e, apex, 0.25, 0.04);
                }
            }));
        }
    }

    /** The sweep: the staff goes all the way round him. */
    static void sweep(ZogratisBoss b, ServerLevel sl, double radius, float damage) {
        Vec3 c = b.position();
        SdsaStrikes.fx(sl, VfxShape.SLASH_SCYTHE, c.add(0, 1, 0), c.add(0, 1, 1), GREEN, 12, (float) radius, true);
        SdsaStrikes.sound(sl, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, c, radius)) {
            if (b.strike(e, damage, false)) SdsaStrikes.push(e, c, 0.9, 0.15);
        }
    }

    /** Courechouse: Extend. The four sections run out into one long thin thrust. */
    static void lash(ZogratisBoss b, ServerLevel sl, Vec3 dir, double length, double halfWidth, float damage) {
        Vec3 from = b.position(), chest = from.add(0, b.getBbHeight() * 0.6, 0);
        Vec3 end = SdsaStrikes.clipEnd(sl, b, chest, chest.add(dir.scale(length)));
        double reach = Math.max(1.0, SdsaStrikes.along(from, dir, end));
        SdsaStrikes.fx(sl, VfxShape.DARK_THRUST, chest, chest.add(dir.scale(reach)), GREEN, 10, 1.4f, true);
        SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.3f, 1.2f);
        for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, reach, halfWidth)) {
            if (b.strike(e, damage, false)) SdsaStrikes.push(e, from, 0.7, 0.05);
        }
    }

    // ================================================================ the fox
    /**
     * The Fox Step: through the strip the wind-up showed and out the far side, two blocks past the end (or as far as a wall allows), cutting everything on the way.
     * The one place a fox differs from Meliodas's lunge is that he does not stop short: he ends behind the one he passed.
     */
    static void foxStep(ZogratisBoss b, ServerLevel sl, Vec3 dir, double length, double halfWidth, float damage) {
        Vec3 from = b.position(), eye = from.add(0, 1.0, 0);
        Vec3 want = SdsaStrikes.clipEnd(sl, b, eye, eye.add(dir.scale(length + 2.0)));
        double reach = Math.max(0.0, SdsaStrikes.along(from, dir, want));
        Vec3 stop = ZogratisBoss.groundOrNull(sl, from.add(dir.scale(reach)));
        SdsaStrikes.fx(sl, VfxShape.DARK_IAI, eye, from.add(dir.scale(reach)).add(0, 1.0, 0), GREEN, 8, 1.8f, true);
        SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.4f, 1.3f);
        for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, reach + 0.5, halfWidth)) {
            if (b.strike(e, damage, false)) SdsaStrikes.push(e, from, 0.6, 0.1);
        }
        if (stop != null && reach >= 1.8) SdsaStrikes.dashTo(b, stop);
    }

    /** Hunter Fest: he lands on the mark and takes the heart of whoever is in the ring (typed heart-eating damage). */
    static void hunterFest(ZogratisBoss b, ServerLevel sl, Vec3 mark, double radius, float damage, Consumer<LivingEntity> onHit) {
        Vec3 from = b.position();
        SdsaStrikes.fx(sl, VfxShape.SPATIAL_RIFT, from.add(0, 1, 0), from.add(0, 1.5, 0), GREEN, 10, 0.7f, true);
        boolean moved = SdsaStrikes.dashTo(b, mark);
        Vec3 at = moved ? mark : b.position();
        SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, at, at.add(0, 1, 0), GREEN, 16, (float) radius * 1.3f, true);
        SdsaStrikes.sound(sl, at, SoundEvents.ANVIL_LAND, 1.4f, 1.1f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, at, radius)) {
            if (SdsaStrikes.hit(b, e, damage, HEART)) {
                SdsaStrikes.push(e, at, 0.5, 0.15);
                if (onHit != null) onHit.accept(e);
            }
        }
    }

    // ================================================================ the Fountain
    /**
     * The geysers: the water of the Fountain breaks out of the ground on every mark the wind-up showed and throws whoever stands in the ring a couple of blocks up
     * (no fall damage). A foe is hurt once, however many geysers overlap; a shield or anti-magic cuts the blow.
     */
    static void geysers(ZogratisBoss b, ServerLevel sl, List<Vec3> marks, double radius, float damage) {
        Set<Integer> hit = new HashSet<>();
        SdsaStrikes.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 0.7f);
        for (Vec3 m : marks) {
            SdsaStrikes.fx(sl, VfxShape.TREE_ROOTS, m, m.add(0, 1, 0), GREEN, 30, (float) radius * 1.5f, true);
            SdsaStrikes.fx(sl, VfxShape.WATER_BURST, m, m.add(0, 1, 0), PALE, 24, (float) radius, true);
            for (LivingEntity e : SdsaStrikes.foesInDisc(b, m, radius)) {
                if (!hit.add(e.getId())) continue;
                if (SdsaStrikes.hit(b, e, damage * SdsaStrikes.guard(e), WATER)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.55, 0));
                    e.hurtMarked = true;
                    e.fallDistance = 0;
                }
            }
        }
    }

    // ================================================================ the ultimate
    /**
     * The Fox's Hunt: dashes through the marks the wind-up showed, in order, {@code dashes} of them, 11 ticks apart (the path is closed: after the last mark he
     * runs to the first again). Each leg cuts the strip it travels along, and each foe it cuts is offered to {@code onHit} (the Snatch).
     */
    static void hunt(ZogratisBoss b, ServerLevel sl, List<Vec3> marks, int dashes, double halfWidth, float damage, Consumer<LivingEntity> onHit, BooleanSupplier halted) {
        if (marks.isEmpty()) return;
        for (int k = 0; k < dashes; k++) {
            final Vec3 to = marks.get(k % marks.size());
            b.later(k * 11, SdsaStrikes.safe("fox hunt dash", () -> {
                if (halted.getAsBoolean()) return;                       // he fell in the middle of the circuit
                Vec3 from = b.position();
                Vec3 dir = SdsaStrikes.unitFlat(to.subtract(from), new Vec3(1, 0, 0));
                double len = Math.min(28.0, SdsaStrikes.along(from, dir, to));
                if (len < 0.8) return;
                Vec3 eye = from.add(0, 1, 0);
                SdsaStrikes.fx(sl, VfxShape.DARK_IAI, eye, from.add(dir.scale(len)).add(0, 1, 0), GREEN, 8, 1.8f, true);
                Tells.burst(sl, from.add(0, 1, 0), PALE, 8);
                SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.3f, 1.3f);
                for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, len + 0.5, halfWidth)) {
                    if (b.strike(e, damage, false)) {
                        SdsaStrikes.push(e, from, 0.6, 0.1);
                        if (onHit != null) onHit.accept(e);
                    }
                }
                Vec3 stop = ZogratisBoss.groundOrNull(sl, from.add(dir.scale(Math.max(0.0, len - 1.0))));
                if (stop != null && len >= 1.8) SdsaStrikes.dashTo(b, stop);
            }));
        }
    }
}
