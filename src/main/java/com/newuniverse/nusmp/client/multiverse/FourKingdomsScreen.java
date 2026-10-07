package com.newuniverse.nusmp.client.multiverse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.46: the Four Kingdoms menu, opened from the slot it takes in Tensura's magic screen (see {@link FourKingdomsSlot}) or with
 * the Multiverse key. The Four Kingdoms Tensura logo heads it; the grimoire section shows the player's unlocked grimoire pages
 * as an open book, one page each: its name, cost and cooldown, and what it does (the page's description). Turning the page plays
 * a page-flip: the leaf lifts off at the spine, foreshortens as it turns (a 2D projection of the turn), shows its back, and lays
 * down on the other side, shading as it goes, with the page-turn sound.
 * <p>
 * The data is the server's status summary (MultiverseSync: Grimoire.PageList), so the menu shows what the server says is
 * unlocked; it never decides anything itself.
 */
public class FourKingdomsScreen extends Screen {
    public static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/four_kingdoms_logo.png");
    static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/grimoire_page.png");
    static final int PANEL_W = 340, PANEL_H = 236, PAGE_W = 138, PAGE_H = 160;
    static final long FLIP_NS = 450_000_000L;
    static final int INK = 0xFF3A2A1A, TITLE = 0xFF6A3A10, FAINT = 0xFF8A7A60, GOLD = 0xFFE8C060;

    private final Screen parent;
    private final List<CompoundTag> pages = new ArrayList<>();
    private int spread;                  // the spread shown: pages 2*spread (left) and 2*spread+1 (right)
    private int flipDir;                 // +1 forward, -1 back, 0 none
    private long flipStart;
    private int left, top, bookX, bookY;

    public FourKingdomsScreen(Screen parent) {
        super(Component.literal("Four Kingdoms"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pages.clear();
        CompoundTag g = MultiverseStatusClient.status().getCompound("Grimoire");
        ListTag list = g.getList("PageList", Tag.TAG_COMPOUND);
        for (int k = 0; k < list.size(); k++) pages.add(list.getCompound(k));
        spread = Mth.clamp(spread, 0, spreads() - 1);
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        bookX = left + PANEL_W / 2;                                         // the spine
        bookY = top + 62;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(bookX - PAGE_W - 2, bookY + PAGE_H + 4, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(bookX + PAGE_W - 18, bookY + PAGE_H + 4, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal("Status"), b -> minecraft.setScreen(new MultiverseStatusScreen(this)))
                .bounds(left + PANEL_W - 64, top + 8, 56, 16).build());
    }

    int spreads() { return Math.max(1, (pages.size() + 1) / 2); }

    void turn(int dir) {
        if (flipDir != 0) return;
        int to = spread + dir;
        if (to < 0 || to >= spreads()) return;
        flipDir = dir;
        flipStart = System.nanoTime();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1f));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) { turn(1); return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) { turn(-1); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy != 0) turn(dy < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (y >= bookY && y < bookY + PAGE_H) {                              // click a page to turn it
            if (x >= bookX && x < bookX + PAGE_W) { turn(1); return true; }
            if (x >= bookX - PAGE_W && x < bookX) { turn(-1); return true; }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);                            // background + frame (renderBackground), buttons
        Font font = this.font;
        // the header: logo, title, the grimoire it is
        g.blit(LOGO, left + 8, top + 6, 48, 48, 0, 0, 256, 256, 256, 256);
        g.drawString(font, Component.literal("Four Kingdoms").withStyle(s -> s.withBold(true)), left + 62, top + 12, GOLD, true);
        g.drawString(font, "Tensura", left + 62, top + 24, 0xFFA0B8FF, true);
        CompoundTag gr = MultiverseStatusClient.status().getCompound("Grimoire");
        String line = gr.contains("Magic") ? gr.getString("Magic") + "  ·  " + gr.getString("Cover") + "  ·  " + gr.getInt("Unlocked") + "/" + gr.getInt("Pages")
                + " pages  ·  mastery " + gr.getInt("Mastery") + "%" : "No grimoire yet";
        g.drawString(font, line, left + 62, top + 38, 0xFFD0D0D8, false);
        // the book
        g.fill(bookX - PAGE_W - 4, bookY - 3, bookX + PAGE_W + 4, bookY + PAGE_H + 3, 0xFF5A2E1A);          // the cover behind the pages
        float p = flipDir == 0 ? 0 : Mth.clamp((System.nanoTime() - flipStart) / (float) FLIP_NS, 0, 1);
        if (flipDir != 0 && p >= 1) { spread += flipDir; flipDir = 0; p = 0; }
        if (flipDir == 0) {
            page(g, font, 2 * spread, false, 1f, 0f);
            page(g, font, 2 * spread + 1, true, 1f, 0f);
        } else {
            float e = p * p * (3 - 2 * p);                                    // smoothstep
            float theta = e * Mth.PI, c = Mth.cos(theta);
            int cur = spread, next = spread + flipDir;
            if (flipDir > 0) {
                page(g, font, 2 * cur, false, 1f, 0f);                        // stays until covered
                page(g, font, 2 * next + 1, true, 1f, 0.35f * (1 - e));       // revealed under the lifting leaf
                if (c > 0) page(g, font, 2 * cur + 1, true, c, 1 - c);        // the leaf, front side, foreshortening
                else page(g, font, 2 * next, false, -c, 1 + c);               // its back: the next left page laying down
            } else {
                page(g, font, 2 * next, false, 1f, 0.35f * (1 - e));
                page(g, font, 2 * cur + 1, true, 1f, 0f);
                if (c > 0) page(g, font, 2 * cur, false, c, 1 - c);
                else page(g, font, 2 * next + 1, true, -c, 1 + c);
            }
        }
        // the spine
        g.fillGradient(bookX - 2, bookY - 2, bookX + 2, bookY + PAGE_H + 2, 0xFF2A1408, 0xFF4A2410);
        g.drawCenteredString(font, (spread + 1) + " / " + spreads(), bookX, bookY + PAGE_H + 7, 0xFFC8B890);
    }

    /**
     * Draws page 'index' on the right (or left) of the spine, scaled horizontally by 'sx' from the spine (the turn's
     * foreshortening) and darkened by 'shade' (0..1, the leaf turning away from the light).
     */
    void page(GuiGraphics g, Font font, int index, boolean right, float sx, float shade) {
        if (sx <= 0.01f) return;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(bookX, bookY, flipDir != 0 && shade > 0 && sx < 1 ? 50 : 0);
        pose.scale(sx, 1, 1);
        int x0 = right ? 0 : -PAGE_W;
        g.blit(PAGE, x0, 0, PAGE_W, PAGE_H, 0, 0, 128, 160, 128, 160);
        // the gutter shadow by the spine
        if (right) g.fillGradient(x0, 0, x0 + 8, PAGE_H, 0x30000000, 0x30000000);
        else g.fillGradient(x0 + PAGE_W - 8, 0, x0 + PAGE_W, PAGE_H, 0x30000000, 0x30000000);
        content(g, font, index, x0 + 8, 8, PAGE_W - 16);
        if (shade > 0.01f) g.fill(x0, 0, x0 + PAGE_W, PAGE_H, ((int) (Mth.clamp(shade, 0, 1) * 150) << 24));
        pose.popPose();
    }

    /** One grimoire page: its name, mode / cost / cooldown, a rule, then what it does. */
    void content(GuiGraphics g, Font font, int index, int x, int y, int w) {
        if (pages.isEmpty() && index == 0) {
            g.drawString(font, "No pages yet", x, y + 4, TITLE, false);
            int yy = y + 20;
            for (FormattedCharSequence l : font.split(Component.literal("Your grimoire's pages open as your mastery grows. "
                    + "Each one you unlock appears here with what it does."), w)) { g.drawString(font, l, x, yy, INK, false); yy += 10; }
            return;
        }
        if (index >= pages.size()) {
            g.drawCenteredString(font, Component.literal("~"), x + w / 2, y + PAGE_H / 2 - 12, FAINT);
            return;
        }
        CompoundTag e = pages.get(index);
        int yy = y + 2;
        for (FormattedCharSequence l : font.split(Component.literal(e.getString("Name")).withStyle(s -> s.withBold(true)), w)) {
            g.drawString(font, l, x, yy, TITLE, false);
            yy += 10;
        }
        String meta = "Page " + (e.getInt("Mode") + 1) + "  ·  " + e.getInt("Cost") + "% MP" + (e.getInt("Cooldown") > 0 ? "  ·  " + e.getInt("Cooldown") + "s" : "");
        g.drawString(font, meta, x, yy + 1, FAINT, false);
        yy += 12;
        g.fill(x, yy, x + w, yy + 1, 0x60402010);
        yy += 4;
        Component desc = Component.translatable(e.getString("Key"));
        String text = desc.getString();
        int dash = text.indexOf(" - ");                                    // descriptions start "Name - what it does"
        if (dash > 0 && dash < 60) text = text.substring(dash + 3);
        for (FormattedCharSequence l : font.split(Component.literal(text), w)) {
            if (yy > y + PAGE_H - 26) { g.drawString(font, "...", x, yy, FAINT, false); break; }
            g.drawString(font, l, x, yy, INK, false);
            yy += 9;
        }
        g.drawCenteredString(font, String.valueOf(index + 1), x + w / 2, y + PAGE_H - 18, FAINT);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFFB08A3A);                 // the frame
        g.fillGradient(left, top, left + PANEL_W, top + PANEL_H, 0xF0141824, 0xF0222838);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
