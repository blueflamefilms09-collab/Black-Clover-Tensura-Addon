package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.saiyan.SaiyanForms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the five attacks of the Goku boss as the framework's {@link Move}s, so they get its telegraph, cooldown, range, pacing and exposed-after-an-ultimate rules for free. Each one is thin: the
 * work (the raycast, the teleport, the blast) is in {@link GokuBoss} where it is bounded. Worst cases per use: the combo schedules 4 timed blows; the Kamehameha does 1 raycast, 1 foe query and at
 * most 24 particle packets; Instant Transmission one safe-spot search and one blink; the Spirit Bomb 1 foe query and at most 24 + 24 particle packets; the Kaio-ken burst nothing but two
 * modifiers. No move spawns an entity.
 */
public final class GokuMoves {
    private GokuMoves() {}

    public static final String COMBO = "meteor_combo", BEAM = "kamehameha", TRANSMIT = "instant_transmission", BOMB = "spirit_bomb", KAIO = "kaioken_burst";

    /** Kamehameha: length and half-width of the beam, the blue of the Saiyan chart. */
    static final double BEAM_REACH = 40.0, BEAM_RADIUS = 1.6;
    /** Spirit Bomb: blast radius. */
    static final double BOMB_RADIUS = 10.0;

    private static GokuBoss goku(ZogratisBoss b) { return (GokuBoss) b; }

    /** The moves, in the order the scheduler considers them. Ids are unique and never renamed. */
    public static List<Move> build() {
        List<Move> out = new ArrayList<>();
        out.add(Move.builder(COMBO, "Meteor Combination").minPower(0).windUp(10).cooldown(70).range(0, 4.5).weight(6).recovery(10).run((b, t) -> combo(goku(b), t)));
        out.add(Move.builder(BEAM, "Kamehameha").minPower(0).windUp(36).cooldown(200).range(6, 40).weight(4).recovery(12)
                .tell((b, t, left, total) -> beamTell(goku(b), t, left, total)).run((b, t) -> beam(goku(b), t)));
        out.add(Move.builder(TRANSMIT, "Instant Transmission").minPower(0).windUp(8).cooldown(150).range(8, 48).weight(3).recovery(8).run((b, t) -> goku(b).transmit(t)));
        out.add(Move.builder(KAIO, "Kaio-ken").minPower(0).windUp(18).cooldown(500).range(0, 48).weight(2).recovery(4).run((b, t) -> goku(b).startKaio(100)));
        out.add(Move.builder(BOMB, "Spirit Bomb").minPower(0).windUp(120).cooldown(900).range(0, 48).weight(3).ultimate().recovery(70)
                .tell((b, t, left, total) -> bombTell(goku(b), t, left, total)).run((b, t) -> bomb(goku(b), t)));
        out.addAll(GokuArts.build());       // appended: the three arts made for him (docs/boss_redesign_goku_sinbad.md)
        return out;
    }

    /** Four blows 5 ticks apart; the last is stronger and shoves. Each one re-checks that the target is still in reach. */
    private static void combo(GokuBoss g, LivingEntity target) {
        for (int i = 0; i < 4; i++) {
            final int n = i;
            g.later(n * 5, () -> {
                if (!target.isAlive() || g.distanceTo(target) > 5.5) return;
                g.swing(n % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                g.getLookControl().setLookAt(target, 60f, 60f);
                boolean landed = g.strike(target, g.base() * (n == 3 ? 0.8f : 0.45f), false);
                Vec3 away = target.position().subtract(g.position()).multiply(1, 0, 1);
                if (landed && away.lengthSqr() > 1e-4) target.knockback(n == 3 ? 1.0 : 0.25, -away.x, -away.z);
            });
        }
    }

    private static void beam(GokuBoss g, LivingEntity target) {
        g.fireBeam(target, BEAM_REACH, BEAM_RADIUS, 2.4f, SaiyanForms.KI_KAME & 0xFFFFFF);
    }

    /** The telegraph: motes gather in his hands and a line shows where he aims. */
    private static void beamTell(GokuBoss g, LivingEntity target, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 hands = g.position().add(g.getLookAngle().scale(0.8)).add(0, g.getBbHeight() * 0.65, 0);
        GokuFx.gather(sl, hands, 0.5 + (total - left) * 0.02, 0x5EC8FF, 6);
        if (left % 10 == 0) GokuFx.kiCharge(sl, g, hands, 0x5EC8FF, (total - left) / (float) Math.max(1, total));
        if (left % 9 == 0) GokuFx.beam(sl, g.getEyePosition(), target.position().add(0, target.getBbHeight() * 0.5, 0), 0x9FD8FF);
    }

    /** The Spirit Bomb: a growing sphere over his head and a ring where it will land; the spot follows the target until the last 1.8 s, then it is fixed, so a player who keeps moving escapes it. */
    private static void bombTell(GokuBoss g, LivingEntity target, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        double grow = 0.6 + 2.4 * (total - left) / (double) Math.max(1, total);
        GokuFx.gather(sl, g.position().add(0, g.getBbHeight() + 2.5, 0), grow, 0x8CD8FF, 8);
        if (left % 20 == 0) GokuFx.kiSphere(sl, g, g.position().add(0, g.getBbHeight() + 2.5, 0), 0x8CD8FF, (float) grow);
        Vec3 spot = g.aimBomb(target, left);
        if (left % 12 == 0) GokuFx.ring(sl, spot, BOMB_RADIUS, 0x8CD8FF, 16);
    }

    private static void bomb(GokuBoss g, LivingEntity target) {
        g.dropBomb(g.aimBomb(target, 0), BOMB_RADIUS, 5.0f, 0x8CD8FF);
    }
}
