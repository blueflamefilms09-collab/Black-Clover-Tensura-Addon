package com.newuniverse.nusmp.compass3d;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: nothing visual can be played by the author, so the CI server smoke test runs this in the real game (key {@code magic.compass_3d}, server safe: no client class, no packet): the mesh is
 * well formed, the prop word round-trips, and on a real level a stand-in caster gets exactly ONE compass however often it is asked for, and the arrow-needles, seal discs, turn discs and routes
 * never pass their caps however many are asked for, and everything is gone after {@link CompassKit#forget}.
 */
public final class CompassSelfTests {
    private CompassSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.compass_3d", CompassSelfTests::run);
    }

    static Result run(MinecraftServer server) {
        // 1. the mesh: every part has quads, unit normals, uv in 0..1
        for (String part : new String[]{"rim", "face", "star", "needle", "glass", "arrow", "bar", "chevron"}) {
            float[] q = CompassMesh.raw(part);
            if (q.length == 0 || q.length % CompassMesh.STRIDE != 0) return Result.fail("mesh part " + part + " has " + q.length + " floats");
            for (int o = 0; o < q.length; o += CompassMesh.STRIDE) {
                float l = (float) Math.sqrt(q[o + 20] * q[o + 20] + q[o + 21] * q[o + 21] + q[o + 22] * q[o + 22]);
                if (Math.abs(l - 1f) > 1e-3f) return Result.fail("mesh part " + part + " has a non unit normal");
                for (int k = 0; k < 4; k++) {
                    float u = q[o + k * 5 + 3], v = q[o + k * 5 + 4];
                    if (u < -1e-4f || u > 1.0001f || v < -1e-4f || v > 1.0001f) return Result.fail("mesh part " + part + " has a uv outside 0..1");
                }
            }
        }
        // 2. the prop word
        int w = CompassRules.packMain(123f, -20f, CompassRules.MODE_CHART, 9);
        if (CompassRules.mode(w) != CompassRules.MODE_CHART || CompassRules.fadeLeft(w) != 9 || Math.abs(CompassRules.aimYaw(w) - 123f) > 1.5f) return Result.fail("main prop word round trip");
        // 3. the live behaviour
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        ServerPlayer p = SelfTests.standIn(level, "compass3d_caster", at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5);
        ServerPlayer foe = SelfTests.standIn(level, "compass3d_foe", at.getX() + 6.5, at.getY() + 2, at.getZ() + 0.5);
        try {
            MagicPropEntity a = CompassKit.ensure(p, 100, CompassRules.MODE_ATTACK, foe), b = CompassKit.ensure(p, 100, -1, null);
            if (a == null) return Result.fail("no compass was spawned");
            if (a != b) return Result.fail("a second ensure made a second compass");
            if (!CompassKit.hasMain(p)) return Result.fail("hasMain is false right after ensure");
            Vec3 c = CompassKit.centre(p);
            if (c.distanceTo(p.position()) > 3.0) return Result.fail("the compass is far from its caster: " + c.distanceTo(p.position()));
            for (int i = 0; i < 40; i++)
                CompassKit.bolt(p, c, new Vec3(0.5, 0, 0), 0.3, 5, false, null, (bolt, t) -> { }, null, i);
            if (CompassKit.live(p, CompassRules.V_NEEDLE) > CompassRules.CAP_NEEDLES) return Result.fail("needle cap passed: " + CompassKit.live(p, CompassRules.V_NEEDLE));
            for (int i = 0; i < 30; i++) CompassKit.seal(p, foe, 40, i);
            if (CompassKit.live(p, CompassRules.V_SEAL) > CompassRules.CAP_SEALS) return Result.fail("seal cap passed: " + CompassKit.live(p, CompassRules.V_SEAL));
            for (int i = 0; i < 10; i++) CompassKit.turnDisc(p, c, foe);
            if (CompassKit.live(p, CompassRules.V_TURN) > CompassRules.CAP_TURNS) return Result.fail("turn disc cap passed");
            for (int i = 0; i < 6; i++) CompassKit.route(p, foe, 100);
            if (CompassKit.live(p, CompassRules.V_ROUTE) > CompassRules.CAP_ROUTES) return Result.fail("route cap passed: " + CompassKit.live(p, CompassRules.V_ROUTE));
            int needles = CompassKit.live(p, CompassRules.V_NEEDLE), seals = CompassKit.live(p, CompassRules.V_SEAL);
            CompassKit.forget(p.getUUID());
            if (CompassKit.hasMain(p) || CompassKit.live(p, CompassRules.V_NEEDLE) != 0 || CompassKit.live(p, CompassRules.V_SEAL) != 0) return Result.fail("props are still there after forget");
            return Result.ok("mesh ok, one compass per caster, caps held (needles " + needles + "/" + CompassRules.CAP_NEEDLES + ", seals " + seals + "/" + CompassRules.CAP_SEALS + "), clean after forget");
        } finally {
            CompassKit.forget(p.getUUID());
        }
    }
}
