package com.newuniverse.nusmp.perf;

import com.newuniverse.nusmp.compass3d.CompassLod;
import com.newuniverse.nusmp.compass3d.CompassMesh;
import com.newuniverse.nusmp.compass3d.CompassRules;
import com.newuniverse.nusmp.franchise.FranchiseEngravings;
import com.newuniverse.nusmp.franchise.FranchiseMap;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Why: the CI self-test {@code perf.lag_audit_1} (docs/perf/lag_audit_1.md), pure (no world, no player, no packet; runs on the dedicated server). It checks the guards the lag audit put on the
 * 1.111.0 work and on the 3D compass, in the booted game: the compass levels of detail get cheaper as they get coarser and are chosen by distance; the frame budget of the compass never lets
 * a crowd of compasses pass its cap (only the ones next to the camera may exceed it, at the cheapest level); the server-wide caps are above the per-player ones and the aim-only word
 * throttle holds; the franchise item cache answers exactly what the map answers and hands out the very same tooltip line again; and a few micro-timings (reported in the result, with a very
 * loose ceiling so a slow CI machine does not fail it, but an accidental scan of a whole table per call would).
 */
public final class LagAudit1SelfTests {
    private LagAudit1SelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("perf.lag_audit_1", LagAudit1SelfTests::run);
    }

    static SelfTests.Result run(MinecraftServer server) {
        // ---- 1. the compass meshes get cheaper with the level of detail
        int full = CompassMesh.compassQuads(0, true, true), mid = CompassMesh.compassQuads(1, true, true), low = CompassMesh.compassQuads(2, true, true);
        if (!(full > mid && mid > low && low > 0)) return SelfTests.Result.fail("compass quads by level are not decreasing: " + full + " / " + mid + " / " + low);
        if (full < 700) return SelfTests.Result.fail("the full compass lost quads: " + full);
        if (CompassMesh.arrowQuads(0) <= CompassMesh.arrowQuads(1) || CompassMesh.arrowQuads(1) <= 0 || CompassMesh.arrowQuads(2) != 0) return SelfTests.Result.fail("arrow quads by level are wrong");
        // ---- 2. levels by distance
        float[] details = {1.0f, 0.75f, 0.5f};
        for (float d : details) {
            int last = -1;
            for (double dist = 0; dist < 120; dist += 1.0) {
                int l = CompassLod.level(dist * dist, d, CompassLod.COMPASS);
                if (l < last) return SelfTests.Result.fail("a level went back to a finer one with distance (detail " + d + ", " + dist + " blocks)");
                last = l;
            }
            if (last != CompassLod.CULLED) return SelfTests.Result.fail("a compass 120 blocks away is still drawn (detail " + d + ")");
        }
        if (CompassLod.level(4.0 * 4.0, 1.0f, CompassLod.COMPASS) != CompassLod.FULL || CompassLod.level(30.0 * 30.0, 1.0f, CompassLod.COMPASS) != CompassLod.MEDIUM) return SelfTests.Result.fail("the near levels moved");
        if (!CompassLod.behind(-10.0) || CompassLod.behind(0.0) || CompassLod.behind(-1.0)) return SelfTests.Result.fail("the behind-the-camera test is wrong");
        // ---- 3. the frame budget: 40 compasses in a ring at 8..47 blocks never pass the cap (those within the keep radius aside)
        int[] cost = CompassLod.compassCost(true, true);
        if (!(cost[0] > cost[1] && cost[1] > cost[2])) return SelfTests.Result.fail("compass cost by level is not decreasing");
        for (VfxQuality q : VfxQuality.values()) {
            PerfBudget b = new PerfBudget("lag_audit_1", CompassLod.frameQuads(q));
            long spent = 0;
            int drawn = 0, culled = 0;
            for (int i = 0; i < 40; i++) {
                double dist = 8 + i;
                int want = CompassLod.level(dist * dist, q.detail, CompassLod.COMPASS);
                int got = CompassLod.admit(b, 1L, want, dist * dist, cost);
                if (got < want) return SelfTests.Result.fail("the budget made a compass finer than it was asked");
                if (got >= CompassLod.CULLED) culled++;
                else {
                    drawn++;
                    spent += cost[got];
                }
            }
            if (b.used() > CompassLod.frameQuads(q)) return SelfTests.Result.fail("the frame budget was passed on " + q + ": " + b.used());
            if (drawn == 0) return SelfTests.Result.fail("nothing was drawn on " + q);
            // a compass at 3 blocks is never dropped, even with the budget gone
            if (CompassLod.admit(b, 1L, CompassLod.FULL, 9.0, cost) > CompassLod.LOW) return SelfTests.Result.fail("a compass next to the camera was dropped");
            if (spent > CompassLod.frameQuads(q) + 1) return SelfTests.Result.fail("spent more than the cap on " + q + ": " + spent);
        }
        // ---- 4. server side: caps and the word throttle
        for (int v = CompassRules.V_NEEDLE; v <= CompassRules.V_ROUTE; v++) {
            int per = switch (v) {
                case CompassRules.V_NEEDLE -> CompassRules.CAP_NEEDLES;
                case CompassRules.V_SEAL -> CompassRules.CAP_SEALS;
                case CompassRules.V_TURN -> CompassRules.CAP_TURNS;
                default -> CompassRules.CAP_ROUTES;
            };
            if (CompassRules.globalCap(v) < per || CompassRules.globalCap(v) > 20 * per) return SelfTests.Result.fail("the server-wide cap of variant " + v + " is out of line: " + CompassRules.globalCap(v));
        }
        int w0 = CompassRules.packMain(10f, 0f, CompassRules.MODE_IDLE, 0), w1 = CompassRules.packMain(20f, 0f, CompassRules.MODE_IDLE, 0), w2 = CompassRules.packMain(20f, 0f, CompassRules.MODE_ATTACK, 0);
        if (CompassRules.shouldSetWord(w0, w0, 100, 0) || CompassRules.shouldSetWord(w0, w1, 101, 100) || !CompassRules.shouldSetWord(w0, w1, 103, 100) || !CompassRules.shouldSetWord(w0, w2, 101, 100))
            return SelfTests.Result.fail("the compass word throttle decides wrongly");
        // ---- 5. the franchise caches answer what the uncached code answered
        int checked = 0;
        for (Map.Entry<String, String> m : FranchiseMap.map().items().entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", m.getKey())).orElse(null);
            if (item == null) continue;
            ItemStack s = new ItemStack(item);
            String first = FranchiseEngravings.franchiseOf(s), again = FranchiseEngravings.franchiseOf(s);
            if (!m.getValue().equals(first) || first != again) return SelfTests.Result.fail("the franchise cache differs from the map for " + m.getKey() + ": " + first + " / " + m.getValue());
            Component line = FranchiseEngravings.tooltipLine(s);
            if (line == null || line != FranchiseEngravings.tooltipLine(s)) return SelfTests.Result.fail("the tooltip line of " + m.getKey() + " is not kept");
            if (++checked >= 400) break;
        }
        if (checked < 20) return SelfTests.Result.fail("only " + checked + " mapped items could be checked");
        if (FranchiseEngravings.franchiseOf(new ItemStack(net.minecraft.world.item.Items.APPLE)) != null || FranchiseEngravings.tooltipLine(new ItemStack(net.minecraft.world.item.Items.APPLE)) != null)
            return SelfTests.Result.fail("an apple has a franchise");
        // ---- 6. micro timings (informational; the ceiling only catches a scan of a whole table per call)
        ItemStack probe = new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", FranchiseMap.map().items().keySet().iterator().next())).orElse(net.minecraft.world.item.Items.APPLE));
        int n = 200_000;
        long t0 = System.nanoTime();
        int sink = 0;
        for (int i = 0; i < n; i++) sink += FranchiseEngravings.franchiseOf(probe) == null ? 0 : 1;
        double franchiseNs = (System.nanoTime() - t0) / (double) n;
        PerfBudget b = new PerfBudget("lag_audit_1_t", 4000);
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) sink += CompassLod.admit(b, i >> 3, CompassLod.level((i % 60) * (double) (i % 60), 1.0f, CompassLod.COMPASS), (i % 60) * (double) (i % 60), cost);
        double admitNs = (System.nanoTime() - t0) / (double) n;
        if (sink == Integer.MIN_VALUE || franchiseNs > 20_000.0 || admitNs > 20_000.0) return SelfTests.Result.fail("a hot path got slow: franchiseOf " + franchiseNs + " ns, admit " + admitNs + " ns");
        return SelfTests.Result.ok("compass quads " + full + "/" + mid + "/" + low + " by level, frame budgets held on " + VfxQuality.values().length + " qualities, caps and throttle ok, " + checked
                + " franchise items cached; franchiseOf " + Math.round(franchiseNs) + " ns, compass admit " + Math.round(admitNs) + " ns per call");
    }
}
