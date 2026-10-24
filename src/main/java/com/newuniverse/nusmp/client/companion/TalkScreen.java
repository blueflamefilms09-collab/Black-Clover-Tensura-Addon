package com.newuniverse.nusmp.client.companion;

import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.talk.TalkView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the talk screen, the same for every companion: her name and a line under it, her words, four tabs (Talk, Interact, Bond, Pouch) with the choices of each, a rapport bar and her stance on the Bond and Pouch tabs, and
 * the nine pouch slots. It DECIDES NOTHING: every click is an ask (the id of a choice on the screen the server sent) and the server answers with a whole new screen or closes it. The drawing is {@link TalkPainter} over
 * {@link TalkLayout}; this class only turns the server's {@link TalkView} into a {@link TalkFrame} (each text resolved once, when the view arrives, never per frame), draws the pouch's item icons and tooltips, and
 * forwards clicks and the wheel. Closing it (Escape, the Close button, another screen opening) tells the server once.
 */
public final class TalkScreen extends Screen {
    private static final String[] TIER_KEYS = {"stranger", "acquaintance", "friend", "close"};
    private static final String[] STANCE_KEYS = {"guard", "aggressive", "passive"};

    private final int entityId;
    private TalkView view;
    private int tab, scroll;
    private boolean closedByServer, toldServer;
    private TalkLayout layout = TalkLayout.compute(320, 240, 0);
    private final GfxCanvas canvas = new GfxCanvas();

    // resolved when the view arrives
    private String title = "", sub = "", text = "", tierName = "", statusLine = "", closeLabel = "";
    private final String[] tabNames = new String[TalkLayout.TABS];
    private final List<TalkView.Option> rows = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<TalkFrame.Row> frameRows = new ArrayList<>();
    private List<String> slots = List.of();

    public TalkScreen(int entityId, TalkView view) {
        super(Component.empty());
        this.entityId = entityId;
        for (int i = 0; i < TalkLayout.TABS; i++) tabNames[i] = Component.translatable("nusmp.companion.tab." + new String[]{"talk", "interact", "bond", "pouch"}[i]).getString();
        closeLabel = Component.translatable("nusmp.companion.close").getString();
        update(view);
    }

    public int entityId() { return entityId; }

    /** A new screen from the server (the answer to a pick). */
    public void update(TalkView v) {
        this.view = v;
        title = v.name.getString();
        sub = v.sub.getString();
        text = v.text.getString();
        tierName = v.rapport >= 0 ? Component.translatable("nusmp.companion.tier." + TIER_KEYS[tierOf(v.rapport)]).getString() : "";
        if (v.stance >= 0 && v.stance < STANCE_KEYS.length) statusLine = Component.translatable("nusmp.companion.bond.stance_now", Component.translatable("nusmp.companion.stance." + STANCE_KEYS[v.stance])).getString();
        else statusLine = "";
        if (v.useIt >= 0) statusLine = (statusLine.isEmpty() ? "" : statusLine + "   ") + Component.translatable(v.useIt == 1 ? "nusmp.companion.bond.uses_on" : "nusmp.companion.bond.uses_off").getString();
        rebuildRows();
    }

    private static int tierOf(int rapport) { return rapport >= 25 ? 3 : rapport >= 10 ? 2 : rapport >= 3 ? 1 : 0; }

    private void rebuildRows() {
        rows.clear();
        labels.clear();
        frameRows.clear();
        for (TalkView.Option o : view.options) {
            if (o.tab() != tab) continue;
            rows.add(o);
            labels.add(o.label().getString());
            frameRows.add(new TalkFrame.Row(labels.get(labels.size() - 1), o.blocked()));
        }
        List<String> sl = new ArrayList<>(TalkLayout.SLOTS);
        if (tab == 3) for (int i = 0; i < TalkLayout.SLOTS; i++) sl.add(i < view.pouch.size() && !view.pouch.get(i).isEmpty() ? "x" : "");
        slots = sl;
        scroll = Math.max(0, Math.min(scroll, layout.maxScroll(rows.size())));
    }

    /** The server closed the conversation. */
    public void closeFromServer() {
        closedByServer = true;
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        layout = TalkLayout.compute(width, height, tab);
        scroll = Math.max(0, Math.min(scroll, layout.maxScroll(rows.size())));
        Font font = Minecraft.getInstance().font;
        canvas.set(g, font);
        int hoverRow = layout.rowAt(mx, my, scroll, rows.size());
        int hoverSlot = layout.slotAt(mx, my);
        String hint = rows.size() > layout.visibleRows ? (scroll + 1) + "-" + Math.min(rows.size(), scroll + layout.visibleRows) + " / " + rows.size() : "";
        TalkFrame frame = new TalkFrame(title, sub, view.color, text, tabNames, tab, frameRows, scroll, hoverRow, layout.tabAt(mx, my), layout.closeAt(mx, my),
                view.rapport, tierName, statusLine, view.stance, view.useIt, slots, hoverSlot, closeLabel, hint);
        TalkPainter.paint(canvas, layout, frame);
        if (tab == 3) {
            for (int i = 0; i < Math.min(TalkLayout.SLOTS, view.pouch.size()); i++) {
                ItemStack s = view.pouch.get(i);
                if (s.isEmpty()) continue;
                int[] r = layout.slotRect(i);
                g.renderItem(s, r[0] + 1, r[1] + 1);
                g.renderItemDecorations(font, s, r[0] + 1, r[1] + 1);
            }
            if (hoverSlot >= 0 && hoverSlot < view.pouch.size() && !view.pouch.get(hoverSlot).isEmpty()) g.renderTooltip(font, view.pouch.get(hoverSlot), mx, my);
        }
        if (hoverRow >= 0 && rows.get(hoverRow).blocked() && !rows.get(hoverRow).blockedReason().getString().isEmpty()) g.renderTooltip(font, rows.get(hoverRow).blockedReason(), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int x = (int) mx, y = (int) my;
        int t = layout.tabAt(x, y);
        if (t >= 0) {
            if (t != tab) {
                tab = t;
                scroll = 0;
                rebuildRows();
                click();
            }
            return true;
        }
        if (layout.closeAt(x, y)) {
            click();
            onClose();
            return true;
        }
        int row = layout.rowAt(x, y, scroll, rows.size());
        if (row >= 0) {
            if (!rows.get(row).blocked()) {
                click();
                send(rows.get(row).id());
            }
            return true;
        }
        int slot = layout.slotAt(x, y);
        if (slot >= 0) {
            String id = "k:take:" + slot;
            if (view.ids().contains(id)) {
                click();
                send(id);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (rows.size() > layout.visibleRows) {
            scroll = Math.max(0, Math.min(layout.maxScroll(rows.size()), scroll - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void send(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) PacketDistributor.sendToServer(new CompanionNet.Pick(entityId, id));
    }

    @Override
    public void removed() {
        if (!closedByServer && !toldServer) {
            toldServer = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new CompanionNet.Close(entityId));
        }
        super.removed();
    }

    /** The game's {@link TalkCanvas}: fills and text go straight to a GuiGraphics. One instance is reused and re-pointed every frame. */
    private static final class GfxCanvas implements TalkCanvas {
        private GuiGraphics g;
        private Font font;

        void set(GuiGraphics graphics, Font f) {
            g = graphics;
            font = f;
        }

        @Override public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }

        @Override public void text(String s, int x, int y, int argb) { g.drawString(font, s, x, y, argb, true); }

        @Override public int textWidth(String s) { return font.width(s); }
    }
}
