package com.newuniverse.nusmp.scalecast;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing visual can be played by the author, so the CI server smoke test runs this in the real game (key {@code magic.scale_casting}, server safe: no client class, no packet): the pure
 * rules hold their caps and floors, the mesh is well formed, and on a real level a stand-in caster gets exactly ONE Scale however often it is asked, the aura presses at most
 * {@link ScaleCastRules#MAX_FOES} of twelve foes by no more than the cap, the damage it takes never goes under the floor, and everything is gone after {@link ScaleCastKit#forget}.
 */
public final class ScaleCastSelfTests {
    private ScaleCastSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.scale_casting", ScaleCastSelfTests::run);
    }

    static Result run(MinecraftServer server) {
        // 1. the pure rules: caps, floors, monotonic curve, the prop word
        for (double d = -4.0; d <= 4.0; d += 0.125) {
            double r = ScaleCastRules.reduction(d, false, ScaleCastRules.DAMAGE_CAP), rb = ScaleCastRules.reduction(d, true, ScaleCastRules.DAMAGE_CAP);
            if (r < 0 || r > ScaleCastRules.DAMAGE_CAP + 1e-9) return Result.fail("reduction " + r + " outside the cap at " + d + " decades");
            if (rb > r + 1e-9) return Result.fail("a boss is pressed more than a normal foe at " + d);
            double m = ScaleCastRules.magicReduction(r);
            if (m < 0 || m > ScaleCastRules.MAGIC_CAP + 1e-9) return Result.fail("magic reduction " + m + " outside its cap");
            for (float amount : new float[]{0f, 0.5f, 1f, 2f, 7f, 400f, 1e9f}) {
                float out = ScaleCastRules.apply(amount, r * 10.0);                              // a reduction far over the cap is held to the cap
                if (out > amount || out < Math.min(amount, ScaleCastRules.DAMAGE_FLOOR) - 1e-6f || out < amount * (1f - ScaleCastRules.DAMAGE_CAP) - 1e-3f * amount) return Result.fail("apply(" + amount + ") = " + out);
            }
        }
        int word = ScaleCastRules.pack(131, 200, true, 35, 12.0, true, 5, true);
        if (ScaleCastRules.casterQ(word) != 131 || ScaleCastRules.foeQ(word) != 200 || !ScaleCastRules.hasFoe(word) || ScaleCastRules.percent(word) != 35 || ScaleCastRules.radius(word) != 12
                || !ScaleCastRules.aura(word) || ScaleCastRules.pulse(word) != 5 || !ScaleCastRules.fading(word)) return Result.fail("prop word round trip");
        // 2. the mesh: every part has quads, unit normals, uv in 0..1
        for (String part : ScaleMesh.PART_NAMES) {
            float[] q = ScaleMesh.raw(part);
            if (q.length == 0 || q.length % ScaleMesh.STRIDE != 0) return Result.fail("mesh part " + part + " has " + q.length + " floats");
            for (int o = 0; o < q.length; o += ScaleMesh.STRIDE) {
                float l = (float) Math.sqrt(q[o + 20] * q[o + 20] + q[o + 21] * q[o + 21] + q[o + 22] * q[o + 22]);
                if (Math.abs(l - 1f) > 1e-3f) return Result.fail("mesh part " + part + " has a non unit normal");
                for (int k = 0; k < 4; k++) {
                    float u = q[o + k * 5 + 3], v = q[o + k * 5 + 4];
                    if (u < -1e-4f || u > 1.0001f || v < -1e-4f || v > 1.0001f) return Result.fail("mesh part " + part + " has a uv outside 0..1");
                }
            }
        }
        // 3. the live behaviour: one Scale per caster, the aura caps, the clean-up
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        ServerPlayer p = SelfTests.standIn(level, "scale_caster", at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5);
        List<Zombie> foes = new ArrayList<>();
        try {
            MagicPropEntity a = ScaleCastKit.show(p, 100), b = ScaleCastKit.show(p, 100);
            if (!ScaleCastConfig.enabled()) return Result.ok("magic.scaleCasting is off on this server: the old execution (no Scale, no aura), pure checks passed");
            if (a == null) return Result.fail("no Scale was spawned");
            if (a != b) return Result.fail("a second show made a second Scale");
            if (!ScaleCastKit.hasScale(p) || ScaleCastKit.liveScales() != 1) return Result.fail("hasScale / liveScales after show: " + ScaleCastKit.liveScales());
            for (int i = 0; i < 12; i++) {
                Zombie z = EntityType.ZOMBIE.create(level);
                if (z == null) return Result.fail("could not create a zombie");
                z.moveTo(at.getX() + 0.5 + 2 + i * 0.5, at.getY() + 1, at.getZ() + 0.5 + (i % 3), 0f, 0f);
                z.setNoAi(true);
                z.setSilent(true);
                level.addFreshEntity(z);
                foes.add(z);
            }
            ScaleCastKit.scanNow(p);
            int n = ScaleCastKit.foesOf(p);
            if (n > ScaleCastRules.MAX_FOES) return Result.fail("the aura presses " + n + " foes, the cap is " + ScaleCastRules.MAX_FOES);
            if (n != ScaleCastRules.MAX_FOES) return Result.fail("twelve zombies in reach but the aura presses " + n);
            long now = level.getGameTime();
            int pressed = 0;
            for (Zombie z : foes) {
                float red = ScaleCastKit.reductionOf(z, now);
                if (red < 0f || red > ScaleCastRules.DAMAGE_CAP + 1e-6f) return Result.fail("a zombie is pressed by " + red + ", over the cap");
                if (red > 0f) pressed++;
            }
            if (pressed > ScaleCastRules.MAX_FOES) return Result.fail("more than " + ScaleCastRules.MAX_FOES + " foes carry a reduction: " + pressed);
            ScaleCastKit.forget(p.getUUID());
            if (ScaleCastKit.hasScale(p) || ScaleCastKit.liveScales() != 0 || ScaleCastKit.liveEntries() != 0) return Result.fail("state is still there after forget: " + ScaleCastKit.liveScales() + " scales, " + ScaleCastKit.liveEntries() + " entries");
            return Result.ok("rules capped, mesh ok, one Scale per caster, the aura pressed " + n + " of 12 foes at most " + ScaleCastRules.DAMAGE_CAP + ", clean after forget");
        } finally {
            for (Zombie z : foes) z.discard();
            ScaleCastKit.forget(p.getUUID());
        }
    }
}
