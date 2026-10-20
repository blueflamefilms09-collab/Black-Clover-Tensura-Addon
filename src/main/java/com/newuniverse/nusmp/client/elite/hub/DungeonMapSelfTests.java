package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Cam;
import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Frame;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.elite.hub.MapView;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Dungeon Map screen cannot be opened by the CI server (no window), but everything that decides what it shows is plain maths ({@link DungeonMapMath}) and a plain value ({@link MapView}), and
 * both are checked here in the real game's class loader: a synthetic floor goes through {@link MapView#build}, the camera fits it, pans, zooms about a point and centres on a player, all inside the
 * limits; every room kind has an opaque colour; the backdrop covers the window at every GUI size the option allows. It touches no client class, so it runs on the dedicated server. Registered from the mod
 * constructor as {@code ui.dungeon_map}.
 */
public final class DungeonMapSelfTests {
    private DungeonMapSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("ui.dungeon_map", server -> {
            List<MapView.Source> src = new ArrayList<>();
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < 31; i++) {
                src.add(new MapView.Source(i, RoomKind.values()[i % RoomKind.values().length].ordinal(), 3, (i % 8) * 24, (i / 8) * 20, (i % 8) * 24 + 18, (i / 8) * 20 + 14));
                if (i > 0) edges.add(new int[] {i - 1, i});
            }
            MapView v = MapView.build(src, edges, 3, 3, "holy", 100, id -> id < 5, 4, 4, true, 180, 70);
            if (v.rooms().size() != 31 || v.edges().size() != 30) return SelfTests.Result.fail("the synthetic floor lost rooms or doors: " + v.rooms().size() + "/" + v.edges().size());
            int[] b = v.bounds();
            for (int[] size : new int[][] {{427, 240}, {854, 480}, {1920, 1080}, {200, 120}}) {
                Frame f = DungeonMapMath.frame(size[0], size[1], 2);
                Cam fit = DungeonMapMath.fit(b, f);
                if (!(fit.zoom() >= DungeonMapMath.ABS_MIN_ZOOM && fit.zoom() <= DungeonMapMath.MAX_ZOOM)) return SelfTests.Result.fail("fit zoom out of limits at " + size[0] + "x" + size[1] + ": " + fit.zoom());
                Cam far = DungeonMapMath.pan(fit, 1e8, -1e8, b, f);
                if (far.cx() < b[0] - DungeonMapMath.PAN_PAD - 1e-9 || far.cz() > b[3] + 1 + DungeonMapMath.PAN_PAD + 1e-9) return SelfTests.Result.fail("pan left the floor at " + size[0] + "x" + size[1]);
                Cam in = fit, out = fit;
                for (int i = 0; i < 100; i++) {
                    in = DungeonMapMath.zoomAbout(in, f.midX() + 30, f.midZ() - 10, 1, b, f);
                    out = DungeonMapMath.zoomAbout(out, f.midX() + 30, f.midZ() - 10, -1, b, f);
                }
                if (in.zoom() > DungeonMapMath.MAX_ZOOM + 1e-9 || out.zoom() < DungeonMapMath.minZoom(DungeonMapMath.fitZoom(b, f)) - 1e-9) return SelfTests.Result.fail("zoom limits broken at " + size[0] + "x" + size[1]);
                Cam me = DungeonMapMath.centreOn(fit, 60, 40, 2.0, b, f);
                if (Math.abs(DungeonMapMath.screenX(me, f, 60) - f.midX()) > 1e-6) return SelfTests.Result.fail("centre on me is off-centre at " + size[0] + "x" + size[1]);
            }
            for (RoomKind k : RoomKind.values())
                if (DungeonMapMath.colour(k.ordinal()) >>> 24 != 0xFF) return SelfTests.Result.fail("room kind " + k + " has no opaque colour");
            for (double s = GuiScaleMath.MIN; s <= GuiScaleMath.MAX + 1e-9; s += GuiScaleMath.STEP) {
                int[] r = DungeonMapMath.backdrop(854, 480, s);
                if (GuiScaleMath.toScreen(r[0], 427, s) > 0 || GuiScaleMath.toScreen(r[2], 427, s) < 854 || GuiScaleMath.toScreen(r[1], 240, s) > 0 || GuiScaleMath.toScreen(r[3], 240, s) < 480)
                    return SelfTests.Result.fail("the backdrop leaves the window uncovered at GUI size " + s);
            }
            return SelfTests.Result.ok("31 rooms fit, pan, zoom and centre inside the limits at 4 window sizes; backdrop covers the window at every GUI size; every room kind has a colour");
        });
    }
}
