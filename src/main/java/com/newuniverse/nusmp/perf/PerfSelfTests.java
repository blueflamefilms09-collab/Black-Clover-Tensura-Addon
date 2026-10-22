package com.newuniverse.nusmp.perf;

import com.mojang.brigadier.tree.CommandNode;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Why: the real game proves what the unit tests cannot (they run without Minecraft): {@code perf.core}, run by the CI server smoke test, checks that the performance helpers behave inside the
 * booted mod. The budgets grant and refuse as the maths say, the probe records while on and records nothing while off (and the state the run found is put back), the diff-send and the
 * throttle say what they should, the resource-location cache hands out the same object and exactly the location the expression it replaced would have built, the defaults of every
 * guard are the pre-1.99 behaviour (so a changed default has to change this test on purpose), and the command {@code /multiverse perf} is registered under the {@code multiverse} root next to
 * the commands of the other builders. Touches no client class, so it runs on the dedicated server.
 */
public final class PerfSelfTests {
    private PerfSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("perf.core", PerfSelfTests::core);
        LagAudit1SelfTests.register();                                    // 1.112.0: perf.lag_audit_1 (docs/perf/lag_audit_1.md)
    }

    private static SelfTests.Result core(MinecraftServer server) {
        // ---- budgets
        PerfBudget b = new PerfBudget("selftest", 10);
        if (b.take(1, 6) != 6 || b.take(1, 6) != 4 || b.take(1, 1) != 0) return SelfTests.Result.fail("budget grants within a tick are wrong");
        if (b.take(2, 3) != 3) return SelfTests.Result.fail("budget did not refill on a new tick");
        if (b.tryTake(2, 8)) return SelfTests.Result.fail("budget tryTake granted more than is left");
        if (!b.tryTake(2, 7)) return SelfTests.Result.fail("budget tryTake refused what fits");
        b.setCap(0);
        if (b.take(3, 1000) != 1000) return SelfTests.Result.fail("a cap of 0 must mean unlimited");

        // ---- probe: records while on, nothing while off, and the state it found is put back
        boolean was = PerfProbe.enabled();
        try {
            PerfProbe.Section s = PerfProbe.section("s.selftest.probe");
            PerfProbe.setEnabled(false);
            long off = PerfProbe.begin();
            PerfProbe.end(s, off);
            if (off != 0L || s.row().calls() != 0) return SelfTests.Result.fail("the probe measured while off");
            PerfProbe.setEnabled(true);
            long t0 = PerfProbe.begin();
            PerfProbe.end(s, t0);
            if (t0 == 0L || s.row().calls() != 1) return SelfTests.Result.fail("the probe did not measure while on");
        } finally {
            PerfProbe.setEnabled(was);
        }

        // ---- diff send and throttle
        DiffSend d = new DiffSend();
        if (!d.shouldSend(1, 5, 100, 200) || d.shouldSend(1, 5, 101, 200) || !d.shouldSend(1, 6, 102, 200) || !d.shouldSend(1, 6, 302, 200)) return SelfTests.Result.fail("diff send decisions are wrong");
        if (!PerfMath.dueFor(40, 20, 0) || PerfMath.dueFor(41, 20, 0) || !PerfMath.dueFor(41, 20, 1)) return SelfTests.Result.fail("stagger decisions are wrong");

        // ---- the location cache builds exactly what the expression it replaces built, once
        ResourceLocation a = RlCache.modTexture("particle/glow");
        if (a != RlCache.modTexture("particle/glow") || !a.equals(ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png"))) return SelfTests.Result.fail("the texture location cache is wrong");
        if (!RlCache.mod("x/y").equals(ResourceLocation.fromNamespaceAndPath("nusmp", "x/y"))) return SelfTests.Result.fail("the mod location cache is wrong");

        // ---- the defaults are the old behaviour (a changed default must change this on purpose)
        if (VfxQuality.HIGH.maxActive != 64 || VfxQuality.HIGH.detail != 1.0f || PerfSettings.DEFAULT_RELIC_SCAN_INTERVAL != 10 || PerfSettings.DEFAULT_ENTITY_SCAN_INTERVAL != 5)
            return SelfTests.Result.fail("a default no longer matches the pre-1.99 behaviour");
        if (VfxQuality.HIGH.detailAt(96.0) != 1.0f || VfxQuality.HIGH.culled(255.0) || !VfxQuality.HIGH.culled(257.0)) return SelfTests.Result.fail("the high quality changes the look near the camera");

        // ---- the command is under the multiverse root
        CommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot().getChild("multiverse");
        if (root == null || root.getChild("perf") == null) return SelfTests.Result.fail("/multiverse perf is not registered");
        return SelfTests.Result.ok("budgets, probe on/off, diff send, stagger, location cache, defaults and /multiverse perf");
    }
}
