package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Cam;
import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Frame;
import com.newuniverse.nusmp.client.ui.GuiScale;
import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.MapView;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Why: the screen of the Dungeon Map item: the layout of the floor the player stood on when they used it, drawn from the compact room list the server sent ({@link MapView}). Every room is a
 * rectangle at its true size and place, coloured by its kind (landing and safe rooms green and blue, halls tan, trials and traps orange and red, treasure gold, the Guardian's arena crimson,
 * corridors grey), every door the generator made is a line between the rooms it joins, and the FOG OF WAR is the brightness: a room the player has stood in is drawn solid, one they have not is
 * drawn dim with an outline, so the map shows the whole layout yet tells at a glance what has been explored. The arrival stair is marked S, the stair that leads on is marked E, and the
 * player is a gold square with a line showing where they face, moving live. The view fits the floor at first; drag with the mouse to move it, wheel to zoom about the pointer. It draws
 * nothing about what is IN a room (the item reveals the layout, not the monsters), and it is not a pause screen: the game goes on behind it.
 *
 * <p>1.105.0 (map-gui): the screen used to draw the whole map in {@code render} and call {@code super.render} LAST, and vanilla's {@code Screen.render} starts with {@code renderBackground}, which BLURS the
 * frame: the finished map, its text and legend were blurred (docs/dungeon_map_gui_fix.md). Now {@code renderBackground} is a flat opaque backdrop (no blur, and wide enough to cover the window
 * when the GUI size option scales the screen down) and the map is drawn after {@code super.render}. There is a title bar with a Close button, a clipped map frame, zoom and pan limits, the keys
 * C (centre on me), F (fit the floor), + and -, and a legend with colour swatches that wraps instead of running off the window. The numbers are in {@link DungeonMapMath}.
 */
public class DungeonMapScreen extends Screen {
    private static final int BACKDROP = 0xFA0C0A10, BAR = 0xFF14111A, BAR_EDGE = 0xFFB08A3A;

    private final MapView view;
    private final String note;
    private final int[] bounds;
    private Cam cam = new Cam(0, 0, 1);
    private Frame frame = new Frame(0, DungeonMapMath.TITLE_H, 16, DungeonMapMath.TITLE_H + 16);
    private boolean fitted, dragging;

    public DungeonMapScreen(HubNet.MapSync p) {
        super(Component.translatableWithFallback("item.nusmp.dungeon_map", "Dungeon Map"));
        this.view = p.view();
        this.note = p.note();
        this.bounds = view.bounds();
    }

    @Override public boolean isPauseScreen() { return false; }

    private static String tr(String key, String english, Object... args) {
        return Component.translatableWithFallback("gui.nusmp.dungeon_map." + key, english, args).getString();
    }

    // ---- layout ----------------------------------------------------------------------------------------------------------------------------------

    private record LegendEntry(String text, int colour) {}

    private static LegendEntry[] legendEntries() {
        return new LegendEntry[] {
                new LegendEntry(tr("landing", "Landing"), DungeonMapMath.colour(RoomKind.LANDING.ordinal())),
                new LegendEntry(tr("safe", "Safe / stairs"), DungeonMapMath.colour(RoomKind.SAFE.ordinal())),
                new LegendEntry(tr("hall", "Hall"), DungeonMapMath.colour(RoomKind.HALL.ordinal())),
                new LegendEntry(tr("trial", "Trial"), DungeonMapMath.colour(RoomKind.TRIAL.ordinal())),
                new LegendEntry(tr("trap", "Trap"), DungeonMapMath.colour(RoomKind.TRAP.ordinal())),
                new LegendEntry(tr("treasure", "Treasure"), DungeonMapMath.colour(RoomKind.TREASURE.ordinal())),
                new LegendEntry(tr("arena", "Arena"), DungeonMapMath.colour(RoomKind.ARENA.ordinal())),
                new LegendEntry(tr("corridor", "Corridor"), DungeonMapMath.colour(RoomKind.CORRIDOR.ordinal())),
                new LegendEntry(tr("unexplored", "Unexplored"), DungeonMapMath.dim(DungeonMapMath.colour(RoomKind.HALL.ordinal()))),
                new LegendEntry(tr("start", "S arrival"), 0xFF5CC878),
                new LegendEntry(tr("exit", "E way on"), 0xFFE8C060)};
    }

    private int[][] legendLayout(LegendEntry[] entries) {
        int[] widths = new int[entries.length];
        for (int i = 0; i < widths.length; i++) widths[i] = 11 + font.width(entries[i].text());
        return DungeonMapMath.legendLayout(widths, 10, width - 10, 10);
    }

    private Frame computeFrame() {
        return DungeonMapMath.frame(width, height, DungeonMapMath.legendRows(legendLayout(legendEntries())));
    }

    @Override
    protected void init() {
        frame = computeFrame();
        if (!fitted) {                                                 // init() runs again on every window resize: keep the player's pan and zoom then
            cam = DungeonMapMath.fit(bounds, frame);
            fitted = true;
        } else {
            cam = DungeonMapMath.clamp(cam, bounds, frame);
        }
        addRenderableWidget(Button.builder(Component.translatableWithFallback("gui.nusmp.dungeon_map.close", "Close"), b -> onClose())
                .bounds(width - 10 - 50, 7, 50, 18).build());
    }

    private int sx(double x) { return (int) Math.round(DungeonMapMath.screenX(cam, frame, x)); }

    private int sz(double z) { return (int) Math.round(DungeonMapMath.screenZ(cam, frame, z)); }

    /** The colour of a room kind (ARGB). */
    static int colour(int kind) { return DungeonMapMath.colour(kind); }

    // ---- drawing ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * The flat backdrop, with NO vanilla blur (vanilla's {@code renderBackground} blurs everything drawn so far, see ScreenBlurGuardTest). It covers the whole window even when the GUI size option has
     * scaled this screen down round the window's middle.
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int[] r = DungeonMapMath.backdrop(width, height, GuiScale.screenScale(this));
        g.fill(r[0], r[1], r[2], r[3], BACKDROP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);                  // the backdrop (renderBackground, once) and the Close button; the map goes over the backdrop, never under a blur
        frame = computeFrame();
        drawMap(g);
        drawBars(g);
    }

    private void drawMap(GuiGraphics g) {
        g.fill(frame.left(), frame.top(), frame.right(), frame.bottom(), 0xFF08070C);
        g.enableScissor(frame.left(), frame.top(), frame.right(), frame.bottom());
        try {
            // doors first, so the rooms sit over the part of each line that runs inside them
            for (int[] e : view.edges()) {
                MapView.Room a = byId(e[0]), b = byId(e[1]);
                if (a == null || b == null) continue;
                line(g, sx(a.centerX() + 0.5), sz(a.centerZ() + 0.5), sx(b.centerX() + 0.5), sz(b.centerZ() + 0.5), 0xFFB8B09A);
            }
            for (MapView.Room r : view.rooms()) {
                int x0 = sx(r.minX()), z0 = sz(r.minZ()), x1 = Math.max(x0 + 2, sx(r.maxX() + 1)), z1 = Math.max(z0 + 2, sz(r.maxZ() + 1));
                if (!DungeonMapMath.visible(frame, x0, z0, x1, z1)) continue;
                int c = DungeonMapMath.colour(r.kind());
                g.fill(x0, z0, x1, z1, r.visited() ? c : DungeonMapMath.dim(c));
                int edge = r.visited() ? 0xFFFFFFFF : 0xFF5A5668;
                g.fill(x0, z0, x1, z0 + 1, edge);
                g.fill(x0, z1 - 1, x1, z1, edge);
                g.fill(x0, z0, x0 + 1, z1, edge);
                g.fill(x1 - 1, z0, x1, z1, edge);
            }
            marker(g, view.arrivalX(), view.arrivalZ(), "S", 0xFF5CC878);
            if (view.hasExit()) marker(g, view.exitX(), view.exitZ(), "E", 0xFFE8C060);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                int px = sx(mc.player.getX()), pz = sz(mc.player.getZ());
                double yaw = Math.toRadians(mc.player.getYRot());
                g.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFFFFFFFF);
                g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFFE8C060);
                line(g, px, pz, px + (int) Math.round(-Math.sin(yaw) * 9), pz + (int) Math.round(Math.cos(yaw) * 9), 0xFFFFFFFF);
            }
        } finally {
            g.disableScissor();
        }
        if (view.rooms().isEmpty()) g.drawCenteredString(font, tr("empty", "This floor has no rooms to show."), width / 2, (int) frame.midZ(), HubGui.MUTED);
    }

    private void drawBars(GuiGraphics g) {
        // title bar
        g.fill(frame.left(), 0, frame.right(), DungeonMapMath.TITLE_H, BAR);
        g.fill(frame.left(), DungeonMapMath.TITLE_H - 1, frame.right(), DungeonMapMath.TITLE_H, BAR_EDGE);
        String road = view.road().isEmpty() ? "" : " (" + Character.toUpperCase(view.road().charAt(0)) + view.road().substring(1) + ")";
        g.drawString(font, tr("floor", "Floor %s", (view.floor() > 0 ? "+" : "") + view.floor()) + road, 10, 6, HubGui.GOLD, true);
        String counts = tr("counts", "%s rooms, %s doors, %s explored", view.rooms().size(), view.edges().size(), view.visitedCount());
        g.drawString(font, counts, 10, 19, HubGui.TEXT, true);
        if (!note.isEmpty()) {
            int nx = 10 + font.width(counts) + 14;
            if (nx + font.width(note) < width - 70) g.drawString(font, note, nx, 19, HubGui.MUTED, true);
        }
        // bottom bar: legend rows, then the hint
        LegendEntry[] entries = legendEntries();
        int[][] lay = legendLayout(entries);
        int rows = DungeonMapMath.legendRows(lay);
        int top = Math.max(frame.bottom(), height - DungeonMapMath.bottomBarHeight(rows));
        g.fill(frame.left(), top, frame.right(), height, BAR);
        g.fill(frame.left(), top, frame.right(), top + 1, BAR_EDGE);
        for (int i = 0; i < entries.length; i++) {
            int x = lay[i][0], y = top + DungeonMapMath.BAR_PAD + lay[i][1] * DungeonMapMath.ROW_H;
            g.fill(x - 1, y, x + 9, y + 10, 0xFFE8E4DC);
            g.fill(x, y + 1, x + 8, y + 9, entries[i].colour());
            g.drawString(font, entries[i].text(), x + 11, y + 1, HubGui.TEXT, false);
        }
        String hint = tr("hint", "Drag: move   Wheel / + -: zoom   C: centre on me   F: fit floor   Esc: close");
        g.drawString(font, hint, 10, top + DungeonMapMath.BAR_PAD + rows * DungeonMapMath.ROW_H + 1, HubGui.MUTED, false);
    }

    private MapView.Room byId(int id) {
        for (MapView.Room r : view.rooms()) if (r.id() == id) return r;
        return null;
    }

    private void marker(GuiGraphics g, int worldX, int worldZ, String label, int colour) {
        int x = sx(worldX + 0.5), z = sz(worldZ + 0.5);
        g.fill(x - 5, z - 5, x + 5, z + 5, 0xFF000000);
        g.fill(x - 4, z - 4, x + 4, z + 4, colour);
        g.drawString(font, label, x - font.width(label) / 2, z - 4, 0xFF101010, false);
    }

    /** A straight line, drawn as an L (horizontal, then vertical) of 1-pixel strips: doors in a maze of axis-aligned rooms read best that way. */
    private static void line(GuiGraphics g, int x0, int z0, int x1, int z1, int colour) {
        g.fill(Math.min(x0, x1), z0, Math.max(x0, x1) + 1, z0 + 1, colour);
        g.fill(x1, Math.min(z0, z1), x1 + 1, Math.max(z0, z1) + 1, colour);
    }

    // ---- input (GuiScale has already turned the mouse into this screen's own coordinates) ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;             // the Close button
        dragging = (button == 0 || button == 2) && frame.contains(mx, my);
        return dragging;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && (button == 0 || button == 2)) {
            cam = DungeonMapMath.pan(cam, dx, dy, bounds, frame);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!frame.contains(mx, my)) return super.mouseScrolled(mx, my, scrollX, scrollY);
        cam = DungeonMapMath.zoomAbout(cam, mx, my, scrollY, bounds, frame);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_C -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) cam = DungeonMapMath.centreOn(cam, mc.player.getX(), mc.player.getZ(), 2.0, bounds, frame);
                return true;
            }
            case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_HOME -> {
                cam = DungeonMapMath.fit(bounds, frame);
                return true;
            }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                cam = DungeonMapMath.zoomCentre(cam, DungeonMapMath.WHEEL_STEP, bounds, frame);
                return true;
            }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                cam = DungeonMapMath.zoomCentre(cam, 1.0 / DungeonMapMath.WHEEL_STEP, bounds, frame);
                return true;
            }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }
}
