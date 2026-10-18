package com.newuniverse.nusmp.compat.maps;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: the in-game self-test {@code compat.maps} ({@code /multiverse mega selftest compat.maps}, also run by the CI smoke script): checks that need no player and no map mod.
 * Pure checks of the waypoint format (every dimension of the mod, every kind, a name with a colon), the limiter, the dimension audit of the real dimension JSONs, the FTB Chunks
 * reader and the minimap geometry; plus one registry sweep that every block of the mod has a map colour (the runtime twin of the source-scanning unit test, so a block added by a
 * merged branch is caught in the game as well).
 */
public final class MapsSelfTests {
    private MapsSelfTests() {}

    public static void register() { SelfTests.register("compat.maps", MapsSelfTests::run); }

    /** The result of the registry sweep: how many blocks of the mod, and the ids that have no map colour. */
    public record Colours(int total, List<String> missing) {}

    public static Colours blockColours() {
        int total = 0;
        List<String> missing = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (!"nusmp".equals(id.getNamespace())) continue;
            total++;
            Block b = BuiltInRegistries.BLOCK.get(id);
            if (b.defaultMapColor() == MapColor.NONE) missing.add(id.getPath());
        }
        return new Colours(total, missing);
    }

    static SelfTests.Result run(MinecraftServer server) {
        // 1. the waypoint format, for every dimension of the mod and every kind
        int strings = 0;
        for (String n : DimensionAudit.NAMES) {
            for (WaypointShare.Kind k : WaypointShare.Kind.values()) {
                String dim = "nusmp:" + n;
                String s = WaypointShare.xaero("Floor 3: " + n, -12, 70, 4_000_123, k, dim);
                Optional<WaypointShare.Waypoint> w = WaypointShare.parse(s);
                if (w.isEmpty()) return SelfTests.Result.fail("the waypoint string does not parse back: " + s);
                WaypointShare.Waypoint p = w.get();
                if (p.x() != -12 || p.y() != 70 || p.z() != 4_000_123 || p.colour() != k.colour || !p.name().equals("Floor 3: " + n))
                    return SelfTests.Result.fail("the waypoint string changed a field on the way: " + s);
                if (!WaypointShare.setMatches(p.set(), dim)) return SelfTests.Result.fail("the dimension key does not match for " + dim + ": " + p.set());
                if (s.split(":", -1).length != 10) return SelfTests.Result.fail("a colon leaked into the waypoint string: " + s);
                strings++;
            }
        }
        // 2. the limiter
        ShareLimiter l = new ShareLimiter(2, 100);
        UUID who = UUID.nameUUIDFromBytes("nusmp-maps-selftest".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!l.tryAcquire(who, 0) || !l.tryAcquire(who, 1) || l.tryAcquire(who, 2) || !l.tryAcquire(who, 100)) return SelfTests.Result.fail("the share limiter does not limit as documented");
        // 3. the dimensions
        List<DimensionAudit.Report> dims = DimensionAudit.auditAll(MapsSelfTests.class.getClassLoader());
        boolean visible = false;                                                                  // the mod's own JSONs may not be visible to this class loader on every launcher: then only the unit test judges them
        for (DimensionAudit.Report r : dims) if (!r.problems().contains("no dimension type JSON")) visible = true;
        if (visible) for (DimensionAudit.Report r : dims) if (!r.ok()) return SelfTests.Result.fail("dimension " + r.id() + ": " + r.problems());
        // 4. the FTB Chunks reader and the minimap geometry
        FtbChunksCheck.Result f = FtbChunksCheck.analyse("claim_dimension_blacklist: [ \"nusmp:*\" ]", FtbChunksGuard.ourDimensions(), "selftest");
        if (f.status() != FtbChunksCheck.Status.COVERED) return SelfTests.Result.fail("the FTB Chunks reader did not accept a nusmp:* blacklist");
        MinimapRect.Rect mini = MinimapRect.topRight(480, 270, 140);
        if (MinimapRect.avoidX(400, 20, 60, 20, mini) >= mini.x()) return SelfTests.Result.fail("the minimap helper did not move a box out of the minimap");
        // 4b. the chat line carries the readable text, and the raw Xaero's string unless the owner switched that off
        String shown = MapShare.line(new MapShare.Place(WaypointShare.Kind.GATE, "Self-test: Gate", "nusmp:sanctuary", 1, 2, 3)).getString();
        if (!shown.contains("1, 2, 3")) return SelfTests.Result.fail("the waypoint chat line lost its coordinates: " + shown);
        if (MapsConfig.get(MapsConfig.RAW_XAERO_STRING) && !shown.contains("xaero-waypoint:Self-test^col^ Gate:S:1:2:3:")) return SelfTests.Result.fail("the waypoint chat line lacks the raw Xaero's string: " + shown);
        // 5. the block colours
        Colours c = blockColours();
        if (!c.missing().isEmpty()) return SelfTests.Result.fail("blocks without a map colour: " + c.missing());
        return SelfTests.Result.ok(strings + " waypoint strings, " + dims.size() + " dimensions, " + c.total() + " blocks coloured");
    }
}
