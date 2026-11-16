package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place the Black Sword Greed sends its anime effects (the {@code GREED_*} shapes drawn by {@code GreedLayer}, docs/vfx_remake/greed_devils.md). Each call is ONE effect packet through
 * {@link VfxSpawn} (the per-tick effect budget, players within 96 blocks only); a form's click or Secret Technique calls it once (twice for Dimension Destruction), a kill once (the devouring), a form
 * switch or a new tier once, a reflected blow on the shield once (already at most once every half second). Never per target, never per tick. The old particle sprays stay where they were (they are what the
 * client setting {@code greed.classicVfx} shows).
 */
final class GreedFx {
    private GreedFx() {}

    /** The crimson of the blade's edge, the souls, the threads. */
    static final int CRIMSON = 0xFFD2142E;
    /** Twilight Healing's dusk. */
    static final int DUSK = 0xFFB0407A;

    static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int color, int duration, float power) {
        VfxSpawn.send(level, shape, from, to, color, duration, power);
    }

    /** An effect that follows {@code e}, starting at {@code at} (kept at the same offset from it). */
    static void follow(ServerLevel level, VfxShape shape, Entity e, Vec3 at, int color, int duration, float power) {
        VfxSpawn.send(level, new VfxPayload(shape.ordinal(), at, at, color, duration, power, e.getId(), level.random.nextLong()));
    }

    static Vec3 chest(Entity e) { return e.position().add(0.0, e.getBbHeight() * 0.6, 0.0); }

    /** Where a cast leaves the wielder: a little below the eyes, half a block forward. */
    static Vec3 hand(net.minecraft.world.entity.LivingEntity p) { return p.getEyePosition().add(0.0, -0.35, 0.0).add(p.getLookAngle().scale(0.6)); }

    /** {@code range} blocks along the wielder's look line from the eyes. */
    static Vec3 ahead(net.minecraft.world.entity.LivingEntity p, double range) { return p.getEyePosition().add(p.getLookAngle().scale(range)); }

    /** The sword exists once per world, so one wielder: one game time is enough to send at most one devouring maw every 10 ticks (a technique that kills eight at once sends one). */
    private static long lastMaw = -100L;

    static synchronized boolean mawReady(long now) {
        if (now >= lastMaw && now - lastMaw < 10L) return false;
        lastMaw = now;
        return true;
    }

    // ------------------------------------------------------------------ CI server smoke test (prefix greed.vfx in SMOKE_EXPECT)
    private static boolean registered;

    static void registerSelfTests() {
        if (registered) return;
        registered = true;
        com.newuniverse.nusmp.labyrinth.mega.SelfTests.register("greed.vfx", GreedFx::selfTest);
    }

    /** Every Greed shape goes out through the effect budget on a dedicated server without throwing (to nobody: no player is near the spawn in CI), and the shapes are the nine expected. */
    private static com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        int sent = 0;
        for (VfxShape s : VfxShape.values()) {
            if (!s.name().startsWith("GREED_")) continue;
            send(level, s, at, at.add(0, 0, 6), CRIMSON, 10, 1f);
            sent++;
        }
        if (sent != 9) return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("expected the 9 GREED_ shapes, found " + sent);
        if (VfxShape.GREED_SLASH.ordinal() != VfxShape.KI_TRANSFER.ordinal() + 1) return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("the Greed shapes moved (they must follow KI_TRANSFER)");
        return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.ok("the 9 Greed shapes sent through VfxSpawn, appended after KI_TRANSFER");
    }
}
