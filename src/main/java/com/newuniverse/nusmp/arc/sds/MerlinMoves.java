package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.DomainCube;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Why: the parts of Merlin's moves that touch the world and need nothing of the boss's own state (a fan of bolts, the orb's blow, a falling star, the seal, the
 * Chrono Coffin's freeze and collapse, the Mass Transfer), kept apart from {@link MerlinBoss} so that class can stay about the fight's rules (the stance, the
 * cube, the fields, the stagger). Every effect goes through the balance law: damage through {@code boss.strike} (the Zogratis damage law with its per-hit cap),
 * control through {@code BalanceLaw.beginControl / controlTicks} (three seconds on a player at the most, less on a repeat, never creative, spectators or a holder
 * of anti-magic). Colours are Merlin's violet and gold; nothing changes a block.
 */
final class MerlinMoves {
    private MerlinMoves() {}

    private static final int RGB = MerlinSheet.RGB, ACCENT = MerlinSheet.ACCENT, WARN = 0xFF5A7A;
    private static final int VIOLET = 0xFF000000 | RGB, GOLD = 0xFF000000 | ACCENT;
    private static final String WHO = "Merlin";

    static IntConsumer safe(String what, IntConsumer step) { return FourSinsKit.safe(WHO, what, step); }

    static Runnable safe(String what, Runnable task) { return FourSinsKit.safe(WHO, what, task); }

    // ================================================================ Arcane Volley
    /** {@code count} bolts of arcane power in a fan, two ticks apart, each re-aimed as it leaves; each bends gently towards the target and hurts the first foe it touches. */
    static void volley(MerlinBoss b, LivingEntity target, int count, float damage) {
        if (!(b.level() instanceof ServerLevel sl) || target == null) return;
        int n = Math.max(1, count);
        double[] fan = FourSinsMath.fan(n, Math.min(1.1, 0.16 * n));
        Vec3[] last = {null};
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            Runnable launch = () -> {
                Vec3 origin = b.position().add(0, b.getBbHeight() * 0.75, 0);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(origin) : last[0];
                if (aim == null || aim.lengthSqr() < 1e-4) aim = b.getViewVector(1f);
                last[0] = aim;
                Vec3 dir = FourSinsKit.rotateY(aim.normalize(), offset);
                new FourSinsKit.Bolt(b, origin.add(dir.scale(0.8)), dir, 1.1, 30, 0.9, 0.05, target, ACCENT, e -> {
                    if (b.strike(e, damage, true)) e.knockback(0.3, b.getX() - e.getX(), b.getZ() - e.getZ());
                }).launch(sl);
                FourSinsKit.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            };
            if (i == 0) launch.run();
            else b.later(i * 2, safe("volley bolt", launch));
        }
    }

    // ================================================================ Aldan Strike
    /** The orb comes down on a marked spot: everyone inside the ring is struck (a blow, so a raised shield takes it) and thrown outwards. */
    static void aldanImpact(MerlinBoss b, Vec3 at, double radius, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at.add(0, 0.05, 0), at.add(0, 1, 0), GOLD, 16, (float) (radius / 1.4));
        ZogratisVfx.gravityImpact(sl, at, radius, ACCENT);
        FourSinsKit.sound(sl, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
        Tells.burst(sl, at.add(0, 1, 0), ACCENT, 36);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, radius, 2, 4)) {
            if (b.strike(e, damage, false)) {
                e.knockback(0.9, at.x - e.getX(), at.z - e.getZ());
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.25, 0));
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ Starfall
    /** One falling star: a streak from the sky, a pillar where it lands, and a blow to everyone in the ring who has not been struck by an earlier star of this fall. */
    static void starImpact(MerlinBoss b, Vec3 at, Set<Integer> struck) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.STAR_MAGIC, at.add(0, 14, 0), at, GOLD, 8, 1.2f);
        ZogratisVfx.pillar(sl, at, 1.6, 12, ACCENT, false);
        FourSinsKit.sound(sl, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.7f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, MerlinTable.STAR_RADIUS, 2, 5)) {
            if (!struck.add(e.getId())) continue;
            b.strike(e, MerlinTable.STAR, true);
        }
    }

    // ================================================================ Magic Seal
    /** The seal closes on a marked spot: whoever stands in it has their grimoire silenced (under the balance law). Returns how many were sealed. */
    static int seal(MerlinBoss b, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        VfxSpawn.send(sl, VfxShape.SEAL_CHAINS, at.add(0, 1, 0), at, VIOLET, 40, 1.4f);
        FourSinsKit.sound(sl, at, SoundEvents.CHAIN_PLACE, 1.6f, 0.6f);
        List<LivingEntity> in = FourSinsKit.inDisc(b, at, MerlinTable.SEAL_RADIUS, 2, 4);
        if (in.isEmpty()) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        int sealed = 0;
        for (LivingEntity e : in) {
            if (allowed && FourSinsKit.silence(e, 50) > 0) {
                sealed++;
                FourSinsKit.note(e, "Magic Seal: your grimoire is silenced.", ChatFormatting.LIGHT_PURPLE);
            }
            b.strike(e, 4f, true);
        }
        return sealed;
    }

    // ================================================================ Mass Transfer
    /**
     * Every foe still standing in its mark is moved to a standable spot 7 to 12 blocks from Merlin, through a gate at each end, and a snap shot follows it 12
     * ticks later. Only players are moved (a summon is struck where it stands instead). If the shared control cool-down is running nobody is moved: the rings
     * strike instead. Returns how many were moved.
     */
    static int massTransfer(MerlinBoss b, List<LivingEntity> foes, List<Vec3> marks) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        List<Vec3> taken = new ArrayList<>();
        int moved = 0;
        for (int i = 0; i < foes.size() && i < marks.size(); i++) {
            LivingEntity e = foes.get(i);
            Vec3 m = marks.get(i);
            if (!e.isAlive() || Math.hypot(e.getX() - m.x, e.getZ() - m.z) > MerlinTable.MASS_RADIUS + e.getBbWidth() / 2) continue;
            Vec3 dest = allowed && e instanceof ServerPlayer && FourSinsKit.fair(e) ? destination(b, e, taken) : null;
            if (dest == null) {
                b.strike(e, MerlinTable.ALDAN * 0.6f, true);
                continue;
            }
            taken.add(dest);
            Vec3 face = FourSinsKit.flatDir(e.position(), b.position(), b.getViewVector(1f));
            ZogratisVfx.gate(sl, e.position().add(0, 1, 0), face, 1.2, 14, RGB);
            e.teleportTo(dest.x, dest.y, dest.z);
            ZogratisVfx.gate(sl, dest.add(0, 1, 0), face, 1.2, 14, RGB);
            FourSinsKit.note(e, "Mass Transfer: Merlin has moved you.", ChatFormatting.LIGHT_PURPLE);
            moved++;
            b.later(12, safe("transfer snap", () -> {
                if (e.isAlive()) volley(b, e, 1, MerlinTable.MASS_SNAP);
            }));
        }
        FourSinsKit.sound(sl, b.position(), SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
        return moved;
    }

    /** A spot 7 to 12 blocks from the boss (a dozen tries round the circle), standable, 3 blocks from the spots already taken. */
    private static Vec3 destination(MerlinBoss b, LivingEntity e, List<Vec3> taken) {
        double phase = b.getRandom().nextDouble() * Math.PI * 2;
        for (int k = 0; k < 12; k++) {
            double a = phase + k * 0.5236, r = 7 + b.getRandom().nextInt(6);
            Vec3 want = b.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            Vec3 s = FourSinsKit.standable(b.level(), e, want);
            if (s == null) continue;
            boolean crowded = false;
            for (Vec3 t : taken) if (t.distanceTo(s) < 3) crowded = true;
            if (!crowded) return s;
        }
        return null;
    }

    // ================================================================ Chrono Coffin
    /** A cube of stopped time: who is inside it, held, under the balance law. Returns how many were held. */
    static int coffinFreeze(MerlinBoss b, DomainCube cube) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        int held = 0;
        for (LivingEntity e : b.foesAround(new Vec3(cube.cx(), cube.cy(), cube.cz()), cube.half() * 1.8 + 3)) {
            if (!cube.contains(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ())) continue;
            if (!allowed || !FourSinsKit.fair(e) || (e instanceof net.minecraft.world.entity.player.Player p && DanteMoves.antiMagicHeld(p))
                    || TimeStop.isImmune(e) || TimeStop.isFrozen(e)) {
                FourSinsKit.note(e, "Time cannot hold you.", ChatFormatting.GRAY);
                continue;
            }
            int t = Math.min(MerlinTable.COFFIN_FREEZE, BalanceLaw.controlTicks(e, MerlinTable.COFFIN_FREEZE));
            TimeStop.freeze(e, t);
            float radius = Math.max(e.getBbHeight(), e.getBbWidth()) * 0.6f + 0.25f;
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            VfxSpawn.send(sl, new VfxPayload(VfxShape.TIME_STASIS.ordinal(), c, c, 0, t, radius / 0.6f, e.getId(), sl.random.nextLong()));
            FourSinsKit.note(e, "Chrono Coffin: time stops for you. Hold on.", ChatFormatting.GOLD);
            held++;
        }
        FourSinsKit.sound(sl, new Vec3(cube.cx(), cube.cy(), cube.cz()), SoundEvents.BELL_RESONATE, 2.5f, 0.5f);
        return held;
    }

    /** Time resumes with a crushing blow: everyone still inside the cube is struck once. */
    static void coffinCollapse(MerlinBoss b, DomainCube cube) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 mid = new Vec3(cube.cx(), cube.cy(), cube.cz());
        ZogratisVfx.spaceShatter(sl, mid, cube.half(), RGB);
        FourSinsKit.sound(sl, mid, SoundEvents.GLASS_BREAK, 3f, 0.5f);
        Tells.burst(sl, mid, ACCENT, 90);
        Set<Integer> struck = new HashSet<>();
        for (LivingEntity e : b.foesAround(mid, cube.half() * 1.8 + 3)) {
            if (!cube.contains(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ()) || !struck.add(e.getId())) continue;
            b.strike(e, MerlinTable.COFFIN_COLLAPSE, true);
        }
    }

    /** The cube's edges in dust, three of the twelve per call (so each is redrawn every twelve calls and the load stays small). */
    static void cubeEdges(ServerLevel sl, DomainCube cube, int call, int rgb) {
        int first = (call * 3) % DomainCube.EDGES;
        for (int k = 0; k < 3; k++)
            for (double[] p : cube.edgeSamples((first + k) % DomainCube.EDGES, 1.8)) Tells.gather(sl, new Vec3(p[0], p[1], p[2]), 0.02, rgb, 1);
    }

    /** The colour of a warning in Merlin's palette (for the callers that want one). */
    static int warn() { return WARN; }
}
