package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: a devil's combat hit is drawn in the style of the anime its devil comes from (DEVIL_HIT, drawn by {@code GreedLayer} / {@code GreedDevilHits}, docs/vfx_remake/greed_devils.md). {@link DevilCombat} calls
 * this once per applied attribute effect, which is already at most once a second per host ({@link DevilRules#effectReady}): ONE effect packet through {@link VfxSpawn} (the per-tick budget, players within 96
 * blocks only). The old tinted mote spray stays (the client setting {@code devil.classicVfx} shows only that).
 */
final class DevilHitFx {
    private DevilHitFx() {}

    /** DEVIL_HIT's power for a world: the ordinal + 1 (a payload power of 0 reads as 1 on the client). */
    static float power(DevilWorld w) { return (w == null ? 0 : w.ordinal()) + 1f; }

    static void hit(ServerLevel level, LivingEntity attacker, LivingEntity target, WorldDevil d) {
        int rgb = d != null ? d.color() : DevilWorld.BLACK_CLOVER.color;
        VfxSpawn.send(level, VfxShape.DEVIL_HIT, chest(target), chest(attacker), 0xFF000000 | (rgb & 0xFFFFFF), 18, power(d != null ? d.world() : null));
    }

    private static Vec3 chest(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.6, 0.0); }

    // ------------------------------------------------------------------ CI server smoke test (prefix devil.vfx in SMOKE_EXPECT)
    private static boolean registered;

    static void registerSelfTests() {
        if (registered) return;
        registered = true;
        com.newuniverse.nusmp.labyrinth.mega.SelfTests.register("devil.vfx", DevilHitFx::selfTest);
    }

    /** Every world maps to its own DEVIL_HIT power 1..8, and one hit per world goes out through the effect budget on a dedicated server without throwing (to nobody near the spawn in CI). */
    private static com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        for (DevilWorld w : DevilWorld.values()) {
            float p = power(w);
            if (p < 1f || p > 8f || Math.round(p) - 1 != w.ordinal()) return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("the hit power of " + w + " is " + p);
            VfxSpawn.send(level, VfxShape.DEVIL_HIT, at, at.add(0, 0, 2), 0xFF000000 | w.color, 10, p);
        }
        if (DevilWorld.values().length != 8) return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("a devil world was added: give it a hit look in GreedDevilHits and GreedVfx.DEVIL_WORLDS");
        return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.ok("the 8 devil worlds each send their DEVIL_HIT (power 1..8) through VfxSpawn");
    }
}
