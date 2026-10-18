package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.MapView;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Why: the screen of the Dungeon Map item: the layout of the floor the player stood on when they used it, drawn from the compact room list the server sent ({@link MapView}). Every room is a
 * rectangle at its true size and place, coloured by its kind (landing and safe rooms green and blue, halls tan, trials and traps orange and red, treasure gold, the Guardian's arena crimson,
 * corridors grey), every door the generator made is a line between the rooms it joins, and the FOG OF WAR is the brightness: a room the player has stood in is drawn solid, one they have not is
 * drawn dim with an outline, so the map shows the whole layout yet tells at a glance what has been explored. The arrival stair is marked S, the stair that leads on is marked E, and the
 * player is a gold square with a line showing where they face, moving live. The view fits the floor at first; drag with the mouse to move it, wheel to zoom about the pointer. It draws
 * nothing about what is IN a room (the item reveals the layout, not the monsters), and it is not a pause screen: the game goes on behind it.
 */
public class DungeonMapScreen extends Screen {
    private final MapView view;
    private final String note;
    private double zoom = 1.0, centerX, centerZ;

    public DungeonMapScreen(HubNet.MapSync p) {
        super(Component.translatableWithFallback("item.nusmp.dungeon_map", "Dungeon Map"));
        this.view = p.view();
        this.note = p.note();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int[] b = view.bounds();
        centerX = (b[0] + b[2]) / 2.0;
        centerZ = (b[1] + b[3]) / 2.0;
        double w = Math.max(16, b[2] - b[0] + 1), h = Math.max(16, b[3] - b[1] + 1);
        zoom = Math.max(0.2, Math.min(12.0, Math.min((width - 40) / w, (height - 80) / h)));
    }

    private int sx(double x) { return (int) Math.round(width / 2.0 + (x - centerX) * zoom); }

    private int sz(double z) { return (int) Math.round(height / 2.0 + 6 + (z - centerZ) * zoom); }

    /** The colour of a room kind (ARGB). */
    static int colour(int kind) {
        RoomKind[] kinds = RoomKind.values();
        RoomKind k = kind >= 0 && kind < kinds.length ? kinds[kind] : RoomKind.OTHER;
        return switch (k) {
            case LANDING -> 0xFF5CC878;
            case SAFE, STAIRWELL -> 0xFF4CA8D8;
            case CORRIDOR, JUNCTION, LINK, ONWARD -> 0xFF8C8A94;
            case HALL, OTHER, CAP -> 0xFFC8A870;
            case SET_PIECE -> 0xFF9A6CD0;
            case CAVERN -> 0xFF8A6A48;
            case TRIAL -> 0xFFE8903C;
            case TRAP -> 0xFFD84A4A;
            case PUZZLE -> 0xFFE8D84C;
            case TREASURE -> 0xFFF0C030;
            case ARENA -> 0xFFB02050;
            case ANTECHAMBER, VAULT -> 0xFFD070B0;
            default -> 0xFFC8A870;                                  // a kind appended to the enum later reads as a hall
        };
    }

    private static int dim(int argb) {
        int r = (argb >> 16 & 255) * 2 / 5, gr = (argb >> 8 & 255) * 2 / 5, b = (argb & 255) * 2 / 5;
        return 0xFF000000 | (r + 12) << 16 | (gr + 12) << 8 | (b + 14);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF00C0A10);
        // doors first, so the rooms sit over the part of each line that runs inside them
        for (int[] e : view.edges()) {
            MapView.Room a = byId(e[0]), b = byId(e[1]);
            if (a == null || b == null) continue;
            line(g, sx(a.centerX() + 0.5), sz(a.centerZ() + 0.5), sx(b.centerX() + 0.5), sz(b.centerZ() + 0.5), 0xFFB8B09A);
        }
        for (MapView.Room r : view.rooms()) {
            int x0 = sx(r.minX()), z0 = sz(r.minZ()), x1 = Math.max(x0 + 2, sx(r.maxX() + 1)), z1 = Math.max(z0 + 2, sz(r.maxZ() + 1));
            int c = colour(r.kind());
            g.fill(x0, z0, x1, z1, r.visited() ? c : dim(c));
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
        // header and footer
        String road = view.road().isEmpty() ? "" : " (" + Character.toUpperCase(view.road().charAt(0)) + view.road().substring(1) + ")";
        g.drawString(font, "Floor " + (view.floor() > 0 ? "+" : "") + view.floor() + road, 10, 8, HubGui.GOLD, true);
        g.drawString(font, view.rooms().size() + " rooms, " + view.edges().size() + " doors, " + view.visitedCount() + " explored", 10, 20, HubGui.TEXT, true);
        if (!note.isEmpty()) g.drawString(font, note, 10, 32, HubGui.MUTED, true);
        String hint = "Drag to move, wheel to zoom, Esc to close";
        g.drawString(font, hint, width - 10 - font.width(hint), 8, HubGui.MUTED, true);
        legend(g);
        if (view.rooms().isEmpty()) g.drawCenteredString(font, "This floor has no rooms to show.", width / 2, height / 2, HubGui.MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void legend(GuiGraphics g) {
        String[] names = {"Landing", "Safe / stairs", "Hall", "Trial / trap", "Treasure", "Arena", "Corridor"};
        int[] kinds = {RoomKind.LANDING.ordinal(), RoomKind.SAFE.ordinal(), RoomKind.HALL.ordinal(), RoomKind.TRIAL.ordinal(), RoomKind.TREASURE.ordinal(), RoomKind.ARENA.ordinal(), RoomKind.CORRIDOR.ordinal()};
        int x = 10, y = height - 14;
        for (int i = 0; i < names.length; i++) {
            g.fill(x, y + 1, x + 8, y + 9, colour(kinds[i]));
            g.drawString(font, names[i], x + 11, y + 1, HubGui.MUTED, false);
            x += 11 + font.width(names[i]) + 10;
        }
        g.fill(x, y + 1, x + 8, y + 9, dim(0xFFC8A870));
        g.drawString(font, "Unexplored", x + 11, y + 1, HubGui.MUTED, false);
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

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 || button == 2) {
            centerX -= dx / zoom;
            centerZ -= dy / zoom;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        double wx = centerX + (mx - width / 2.0) / zoom, wz = centerZ + (my - height / 2.0 - 6) / zoom;
        zoom = Math.max(0.2, Math.min(12.0, zoom * Math.pow(1.15, scrollY)));
        centerX = wx - (mx - width / 2.0) / zoom;
        centerZ = wz - (my - height / 2.0 - 6) / zoom;
        return true;
    }
}
