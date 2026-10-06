package com.newuniverse.nusmp.client.multiverse;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Full Multiverse status screen, in the look of the Tensura menus (dark panels, thin borders, gold headings). Read-only: every
 * value comes from the server's last status message (rank, stars, squad, social class, race, grimoire, spirit lord, anti-magic,
 * ceremony). {@link #drawCompact} draws the short version inside the Tensura menu's former "Coming Soon" area.
 */
public class MultiverseStatusScreen extends Screen {
    static final int BG = 0xE0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, TEXT = 0xFFE6E6EE, DIM = 0xFF9A9AAE, RED = 0xFFE05050;

    private final Screen parent;

    public MultiverseStatusScreen(Screen parent) {
        super(Component.literal("Multiverse Status"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 - 40, height - 26, 80, 18).build());
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    record Block(String title, List<String> lines, boolean warn) {}

    static List<Block> blocks(CompoundTag s) {
        List<Block> out = new ArrayList<>();
        out.add(new Block("Magic Knight Rank", List.of(s.getString("Rank").isEmpty() ? "5th Class Junior Magic Knight" : s.getString("Rank")), false));
        out.add(new Block("Stars", List.of("Gold " + s.getInt("Gold") + "   Black " + s.getInt("Black"), "Net " + s.getInt("Net")), false));
        CompoundTag sq = s.getCompound("Squad");
        if (sq.contains("Name")) out.add(new Block("Squad", List.of(sq.getString("Name"), sq.getString("Role") + "  " + sq.getString("Standing"), "Score " + sq.getInt("Score")), false));
        else out.add(new Block("Squad", List.of(sq.getString("State").isEmpty() ? "No squad" : sq.getString("State")), sq.getString("State").startsWith("Requires")));
        out.add(new Block("Social Class", List.of(s.getString("Social").isEmpty() ? "Commoner" : s.getString("Social")), false));
        out.add(new Block("Race", List.of(s.getString("Race").isEmpty() ? "Human" : s.getString("Race")), false));
        CompoundTag g = s.getCompound("Grimoire");
        if (g.contains("Cover")) {
            List<String> l = new ArrayList<>();
            l.add(g.getString("Cover"));
            l.add(g.getString("Magic"));
            if (g.contains("Pages")) l.add("Pages " + g.getInt("Unlocked") + "/" + g.getInt("Pages") + "  slots " + g.getInt("Slots") + "  mastery " + g.getInt("Mastery") + "%");
            out.add(new Block("Grimoire", l, false));
        } else out.add(new Block("Grimoire", List.of("None yet"), false));
        CompoundTag sp = s.getCompound("Spirit");
        if (sp.contains("Type")) out.add(new Block("Spirit Lord", List.of(sp.getString("Type") + "  trust " + sp.getInt("Trust") + "/100",
                "Energy " + sp.getInt("Energy") + "%", sp.getBoolean("Incarnated") ? "Incarnated: " + sp.getString("Name") : "Not incarnated"), false));
        else out.add(new Block("Spirit Lord", List.of("No spirit bond"), false));
        CompoundTag a = s.getCompound("Anti");
        if (a.contains("Mode")) out.add(new Block("Anti-Magic", List.of(a.getString("Mode"), "AMP " + a.getInt("AMP")), false));
        else out.add(new Block("Anti-Magic", List.of("-"), false));
        CompoundTag c = s.getCompound("Ceremony");
        List<String> cl = new ArrayList<>();
        cl.add(c.getBoolean("Accepted") ? "Accepted" + (c.getString("Where").isEmpty() ? "" : " at " + c.getString("Where")) : "Not accepted");
        if (!c.getBoolean("Accepted")) cl.add(c.getBoolean("Eligible") ? "Eligible: go to a Grimoire Tower" : "Not eligible");
        cl.add(c.getBoolean("Active") ? "Ceremony in progress" : "Ceremony closed");
        out.add(new Block("Ceremony", cl, false));
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, "MULTIVERSE STATUS", width / 2, 10, GOLD);
        List<Block> blocks = blocks(MultiverseStatusClient.status());
        int cols = 3, gap = 6;
        int pw = Math.min(160, (width - 20 - gap * (cols - 1)) / cols), ph = 52;
        int x0 = (width - (pw * cols + gap * (cols - 1))) / 2, y0 = 26;
        for (int i = 0; i < blocks.size(); i++) {
            int x = x0 + (i % cols) * (pw + gap), y = y0 + (i / cols) * (ph + gap);
            panel(g, font, x, y, pw, ph, blocks.get(i));
        }
    }

    static void panel(GuiGraphics g, Font font, int x, int y, int w, int h, Block b) {
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, BORDER);
        g.drawString(font, b.title(), x + 5, y + 4, GOLD, false);
        g.hLine(x + 4, x + w - 5, y + 14, BORDER);
        int ly = y + 18;
        for (String line : b.lines()) {
            if (ly + 9 > y + h) break;
            g.drawString(font, font.plainSubstrByWidth(line, w - 10), x + 5, ly, b.warn() ? RED : TEXT, false);
            ly += 10;
        }
    }

    /** The short panel inside the Tensura menu: the most important lines, scaled to fit the area. Click opens the full screen. */
    public static void drawCompact(GuiGraphics g, Font font, int x, int y, int w, int h, CompoundTag s, boolean hover) {
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, hover ? GOLD : BORDER);
        List<Block> blocks = blocks(s);
        List<String[]> rows = new ArrayList<>();
        for (Block b : blocks) {
            if (b.title().equals("Social Class") || b.title().equals("Anti-Magic") && b.lines().get(0).equals("-")) continue;
            rows.add(new String[]{b.title(), b.lines().get(0)});
        }
        int lineH = 10, needed = 14 + rows.size() * lineH;
        float scale = Math.min(1f, (h - 4) / (float) needed);
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 3, 0);
        g.pose().scale(scale, scale, 1f);
        int inner = (int) ((w - 8) / scale);
        g.drawString(font, "MULTIVERSE", 0, 0, GOLD, false);
        String more = hover ? "click for more" : "";
        g.drawString(font, more, inner - font.width(more), 0, DIM, false);
        int ly = 14;
        int labelW = 0;
        for (String[] r : rows) labelW = Math.max(labelW, font.width(r[0]));
        labelW = Math.min(labelW, inner / 2);
        for (String[] r : rows) {
            g.drawString(font, font.plainSubstrByWidth(r[0], labelW), 0, ly, DIM, false);
            g.drawString(font, font.plainSubstrByWidth(r[1], inner - labelW - 6), labelW + 6, ly, TEXT, false);
            ly += lineH;
        }
        g.pose().popPose();
    }
}
