package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Why: the parts of King's moves that touch the world and need nothing of the boss's own state (the spear and its burst, a falling spear, the dive's landing, the
 * gust, the gardens of a field and its pulse, the Sunflower's petals, Fossilization, the Guardian's slam), kept apart from {@link KingBoss} so that class can stay about
 * the fight's rules (the hover, the shell, the fields). Every effect goes through the balance law: damage through {@code boss.strike} (the Zogratis damage law with its
 * per-hit cap), control only through {@code BalanceLaw.beginControl / controlTicks} (Fossilization is the one hold, two seconds before the law's cap, never on creative
 * players, spectators or a holder of anti-magic). The colour code of this fight is the opposite of Gowther's: here a GREEN-GOLD ring is where it is SAFE (a garden of
 * pollen), and the violet field around it is where it is not. Nothing changes a block or spawns an entity.
 */
final class KingMoves {
    private KingMoves() {}

    static final int RGB = KingSheet.RGB, ACCENT = KingSheet.ACCENT;
    /** The pollen of a garden (safe), the death of a field (not), a petal's light, the stone of Fossilization, a warning. */
    static final int POLLEN = 0xFFE680, DEATH = 0x9A4FD0, PETAL = 0xFFF0A0, STONE = 0xA8A8A0, WARN = 0xFF5A7A;
    private static final int GREEN = 0xFF000000 | RGB, GOLD = 0xFF000000 | ACCENT;
    private static final String WHO = "King";

    static IntConsumer safe(String what, IntConsumer step) { return FourSinsKit.safe(WHO, what, step); }

    static Runnable safe(String what, Runnable task) { return FourSinsKit.safe(WHO, what, task); }

    private static boolean holdsAntiMagic(LivingEntity e) { return e instanceof Player p && DanteMoves.antiMagicHeld(p); }

    // ================================================================ Chastiefol: the spear
    /** Where a spear thrown from {@code from} along {@code dir} ends: the first wall, or the full reach. */
    static Vec3 spearEnd(KingBoss b, Vec3 from, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return from.add(dir.scale(KingTable.THROW_REACH));
        return FourSinsKit.clipWall(sl, b, from, from.add(dir.scale(KingTable.THROW_REACH)));
    }

    /**
     * The spear flies (it is too fast to be a point: the lane is the telegraph) and strikes the first foe along it, or the wall; where it lands it bursts, and everyone
     * within the burst's radius who was not struck by the spear itself takes the lesser blow.
     */
    static void throwSpear(KingBoss b, Vec3 from, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 wall = spearEnd(b, from, dir);
        LivingEntity hit = null;
        double best = 2.0;
        Vec3 mid = from.add(wall).scale(0.5);
        for (LivingEntity e : b.foesAround(mid, from.distanceTo(wall) / 2 + 4)) {
            Vec3 c = e.getBoundingBox().getCenter();
            double d = FourSinsMath.distToSegment3(c.x, c.y, c.z, from.x, from.y, from.z, wall.x, wall.y, wall.z);
            if (d > 0.8 + e.getBbWidth() / 2) continue;
            double t = FourSinsMath.along3(c.x, c.y, c.z, from.x, from.y, from.z, wall.x, wall.y, wall.z);
            if (t < best) {
                best = t;
                hit = e;
            }
        }
        Vec3 end = hit != null ? hit.getBoundingBox().getCenter() : wall;
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, from, end, GREEN, 10, 1.1f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, end, end, GOLD, 12, 1.0f);
        FourSinsKit.sound(sl, end, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 1.5f);
        Set<Integer> struck = new HashSet<>();
        if (hit != null && struck.add(hit.getId())) b.strike(hit, KingTable.THROW, false);
        for (LivingEntity e : FourSinsKit.inDisc(b, end, KingTable.THROW_BURST_RADIUS, 2.5, 2.5)) {
            if (!struck.add(e.getId())) continue;
            if (b.strike(e, KingTable.THROW_BURST, true)) e.knockback(0.6, end.x - e.getX(), end.z - e.getZ());
        }
    }

    // ================================================================ Increase
    /** One falling spear: a streak from the sky, a flare where it lands, and a blow to everyone in the ring who has not been struck by an earlier spear of this rain. */
    static void rainSpear(KingBoss b, Vec3 at, Set<Integer> struck) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, at.add(2.5, 13, 1.5), at, GREEN, 8, 0.9f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, at.add(0, 0.4, 0), at.add(0, 0.4, 0), GOLD, 8, 0.6f);
        FourSinsKit.sound(sl, at, SoundEvents.TRIDENT_HIT_GROUND, 1.2f, 1.3f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, KingTable.RAIN_RADIUS, 2, 5)) {
            if (!struck.add(e.getId())) continue;
            b.strike(e, KingTable.RAIN, false);
        }
    }

    // ================================================================ the dive
    /** He lands: everyone within the radius is struck and thrown outwards, roots burst from the ground. */
    static void diveImpact(KingBoss b, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.TREE_ROOTS, at, at.add(0, 1, 0), GREEN, 36, (float) KingTable.DIVE_RADIUS);
        ZogratisVfx.gravityImpact(sl, at, KingTable.DIVE_RADIUS, ACCENT);
        FourSinsKit.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.8f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, KingTable.DIVE_RADIUS, 2, 4)) {
            if (b.strike(e, KingTable.DIVE, false)) {
                e.knockback(0.9, at.x - e.getX(), at.z - e.getZ());
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.25, 0));
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ Fairy Gust
    /** A gust of the wings: everyone in the cone is struck, thrown back, and slowed for a second and a half. Returns how many were hit. */
    static int gust(KingBoss b, Vec3 tip, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        VfxSpawn.send(sl, VfxShape.WIND_GALE, tip.add(0, 1.0, 0), tip.add(dir.x * KingTable.GUST_RANGE, 1.0, dir.z * KingTable.GUST_RANGE), GREEN, 14, 1.2f);
        FourSinsKit.sound(sl, tip, SoundEvents.PHANTOM_FLAP, 1.6f, 0.7f);
        int n = 0;
        for (LivingEntity e : FourSinsKit.inCone(b, tip, dir, KingTable.GUST_RANGE, KingTable.GUST_HALF)) {
            if (!b.strike(e, KingTable.GUST, true)) continue;
            n++;
            e.knockback(1.1, -dir.x, -dir.z);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
        }
        return n;
    }

    // ================================================================ Fossilization
    /**
     * The two-headed spear cuts: everyone in the cone is struck and stone creeps up their legs. For a player that is a hold of two seconds at the most (the balance law
     * caps and halves it), never on creative players, spectators or a holder of anti-magic; if the shared control cool-down is running they are only struck. Returns how
     * many were held.
     */
    static int fossilize(KingBoss b, Vec3 tip, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        FourSinsKit.sound(sl, tip, SoundEvents.STONE_BREAK, 1.6f, 0.6f);
        Tells.burst(sl, tip.add(dir.scale(2.5)).add(0, 1.2, 0), STONE, 22);
        List<LivingEntity> in = FourSinsKit.inCone(b, tip, dir, KingTable.FOSSIL_RANGE, 0.7);
        if (in.isEmpty()) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        int held = 0;
        for (LivingEntity e : in) {
            b.strike(e, KingTable.FOSSIL, false);
            if (!allowed || !(e instanceof ServerPlayer) || !FourSinsKit.fair(e) || holdsAntiMagic(e)) continue;
            int ticks = BalanceLaw.controlTicks(e, KingTable.ROOT_TICKS);
            if (ticks <= 0) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
            Tells.burst(sl, e.position().add(0, 0.8, 0), STONE, 18);
            FourSinsKit.note(e, "Fossilization: stone creeps up your legs. Hold on.", ChatFormatting.GRAY);
            held++;
        }
        return held;
    }

    // ================================================================ the Guardian's four arms
    /** The Guardian brings its arms down on a marked ring: everyone inside is struck and thrown outwards. */
    static void slam(KingBoss b, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.TREE_ROOTS, at, at.add(0, 1, 0), GREEN, 40, (float) KingTable.SLAM_RADIUS);
        ZogratisVfx.gravityImpact(sl, at, KingTable.SLAM_RADIUS, RGB);
        FourSinsKit.sound(sl, at, SoundEvents.RAVAGER_STEP, 2.0f, 0.5f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, KingTable.SLAM_RADIUS, 2, 4)) {
            if (b.strike(e, KingTable.SLAM, false)) {
                e.knockback(1.0, at.x - e.getX(), at.z - e.getZ());
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ the Sunflower
    /** The petal lanes of a telegraph: two lines along each lane from the centre, and a small ring at the centre itself (which is never safe). */
    static void drawPetals(ServerLevel sl, Vec3 centre, int petals, double phase, int rgb) {
        Vec3 base = centre.add(0, 0.1, 0);
        for (double a : SunflowerMath.angles(petals, phase)) FourSinsKit.laneTell(sl, base, new Vec3(Math.cos(a), 0, Math.sin(a)), KingTable.PETAL_LENGTH, KingTable.PETAL_HALF, rgb);
        Tells.ring(sl, base, KingTable.PETAL_HALF + 0.3, WARN, 1.2f, 14);
    }

    /** The flower fires: a beam down every petal, and everyone inside a petal (and not behind a wall from the centre) is struck once. Returns how many were struck. */
    static int fireSunflower(KingBoss b, Vec3 centre, int petals, double phase) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Vec3 core = centre.add(0, 1.6, 0);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, core, core, GOLD, 14, 1.4f);
        FourSinsKit.sound(sl, centre, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.4f);
        for (double a : SunflowerMath.angles(petals, phase)) {
            Vec3 end = FourSinsKit.clipWall(sl, b, core, core.add(Math.cos(a) * KingTable.PETAL_LENGTH, 0, Math.sin(a) * KingTable.PETAL_LENGTH));
            VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, core, end, GOLD, 10, 0.9f);
        }
        int n = 0;
        for (LivingEntity e : b.foesAround(centre, KingTable.PETAL_LENGTH + 3)) {
            if (Math.abs(e.getY() - centre.y) > 4) continue;
            if (!SunflowerMath.inPetal(e.getX() - centre.x, e.getZ() - centre.z, petals, phase, KingTable.PETAL_HALF + e.getBbWidth() / 2, KingTable.PETAL_LENGTH)) continue;
            Vec3 chest = e.position().add(0, e.getBbHeight() * 0.6, 0);
            if (FourSinsKit.clipWall(sl, b, core, chest).distanceToSqr(chest) > 0.5) continue;
            if (b.strike(e, KingTable.PETAL, true)) n++;
        }
        return n;
    }

    // ================================================================ the fields
    /** The gardens of a beat in a telegraph: pollen gold, steady, with a column of pollen and a ring that closes as the pulse nears. */
    static void drawGardens(ServerLevel sl, List<Vec3> gardens, int left, int total) {
        for (Vec3 g : gardens) FourSinsKit.markTell(sl, g, KingTable.GARDEN_RADIUS, left, total, POLLEN, RGB);
    }

    /**
     * The field pulses. Everyone inside the field (within {@code radius} on the ground plane and six blocks of its height) who is not inside a garden is struck;
     * the safe are told so once. A ring of violet races out from the centre. Returns how many were struck.
     */
    static int pulse(KingBoss b, Vec3 centre, double radius, List<Vec3> gardens, float damage, Set<Integer> told) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        List<SafeGardens.Garden> gs = gardens.stream().map(v -> new SafeGardens.Garden(v.x, v.z)).toList();
        int hit = 0;
        for (LivingEntity e : b.foesAround(centre, radius + 4)) {
            if (Math.abs(e.getY() - centre.y) > 6 || Math.hypot(e.getX() - centre.x, e.getZ() - centre.z) > radius + e.getBbWidth() / 2) continue;
            if (SafeGardens.isSafe(gs, e.getX(), e.getZ(), KingTable.GARDEN_RADIUS, e.getBbWidth())) {
                Tells.gather(sl, e.position().add(0, 1.0, 0), 0.5, POLLEN, 5);
                if (told != null && told.add(e.getId())) FourSinsKit.note(e, "The pollen eases the Disaster's touch.", ChatFormatting.GREEN);
                continue;
            }
            Tells.burst(sl, e.position().add(0, 1.0, 0), DEATH, 12);
            if (b.strike(e, damage, true)) hit++;
        }
        for (Vec3 g : gardens) Tells.burst(sl, g.add(0, 0.5, 0), POLLEN, 8);
        FourSinsKit.sound(sl, centre, SoundEvents.WITHER_SHOOT, 1.4f, 0.6f);
        ringOut(sl, centre, radius);
        return hit;
    }

    /** A ring of violet dust that races from the centre to the rim in six ticks. */
    private static void ringOut(ServerLevel sl, Vec3 centre, double radius) {
        for (int i = 1; i <= 6; i++) {
            final double r = radius * i / 6.0;
            SpellRuntime.later(sl, i, safe("pulse ring", () -> Tells.ring(sl, centre.add(0, 0.15, 0), r, DEATH, 1.3f, (int) Math.min(80, 14 + r * 5))));
        }
    }
}
