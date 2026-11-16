package com.newuniverse.nusmp.client.attune;

import com.newuniverse.nusmp.attune.AttuneNet;
import com.newuniverse.nusmp.attune.AttuneRules;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the meditation screen. Top: the slots (click one to take it out). Below: every power the server says the player holds (at most {@link AttuneRules#MAX_OWNED}); click one to put it in a slot
 * (3.95.0: two of one system may share the slots; with the slots full the last one is replaced). Colours come from the same {@link AttuneRules} table the server enforces: red = rejects a power in the
 * slots (or a Devil beside a Spirit, never together), gold = resonates with one, white = neutral. Confirm sends the ids once; the server decides (owned, slots, cooldown, clashes, that the player stood still). While it is open the player gets a
 * calm visual only: a slow breathing tint and two enchant motes every eight ticks around them, both client side. Colours and texts are worked out on each click, never per frame. A small modal window,
 * so it is a fixed screen of the GUI Move framework.
 */
public final class MeditationScreen extends Screen {
    private static final int W = 300, H = 248, ROW = 13, ROWS = 12;

    private final AttuneNet.Open data;
    private final List<String> chosen = new ArrayList<>();
    private final int slots;
    private int[] rowColor = new int[0];
    private int[] slotColor = new int[0];
    private String footer = "";
    private int footerColor = UiTheme.TEXT;
    private boolean clash;
    private int ticks;
    private ThemedButton confirm;
    private String noneText = "", cooldownText = "";

    public MeditationScreen(AttuneNet.Open data) {
        super(Component.translatableWithFallback("attune.screen.title", "Meditation"));
        this.data = data;
        this.slots = AttuneRules.slots(data.slots());
        for (String id : data.attuned()) if (chosen.size() < slots && find(id) != null) chosen.add(id);
    }

    @Override public boolean isPauseScreen() { return false; }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    protected void init() {
        int x = left(), y = top();
        confirm = new ThemedButton(x + 14, y + H - 26, 130, 18, Component.translatableWithFallback("attune.screen.confirm", "Attune"), b -> {
            AttuneClient.confirm(chosen);
            onClose();
        });
        addRenderableWidget(confirm);
        addRenderableWidget(new ThemedButton(x + W - 14 - 130, y + H - 26, 130, 18, Component.translatableWithFallback("attune.screen.cancel", "Leave"), b -> onClose()));
        noneText = Component.translatableWithFallback("attune.screen.none", "You hold no Beast Will, Djinn, Devil or Spirit yet.").getString();
        updateCooldownText();
        recompute();
    }

    private AttuneNet.Entry find(String id) {
        for (AttuneNet.Entry e : data.owned()) if (e.id().equals(id)) return e;
        return null;
    }

    private static AttuneRules.Power power(AttuneNet.Entry e) { return new AttuneRules.Power(e.id(), e.system(), e.elements(), e.rank()); }

    private void recompute() {
        List<AttuneRules.Power> in = new ArrayList<>();
        for (String id : chosen) { AttuneNet.Entry e = find(id); if (e != null) in.add(power(e)); }
        int n = Math.min(ROWS * 2, data.owned().size());
        rowColor = new int[n];
        for (int i = 0; i < n; i++) {
            AttuneNet.Entry e = data.owned().get(i);
            int c = chosen.contains(e.id()) ? UiTheme.WHITE : UiTheme.TEXT_DIM;
            for (AttuneRules.Power p : in) {
                if (p.id().equals(e.id())) continue;
                if (AttuneRules.devilSpirit(p.system(), e.system())) { c = UiTheme.RED; break; }
                AttuneRules.Pair pr = AttuneRules.pair(p.elements(), e.elements());
                if (pr == AttuneRules.Pair.REJECT) { c = UiTheme.RED; break; }
                if (pr == AttuneRules.Pair.RESONATE) c = UiTheme.GOLD;
            }
            rowColor[i] = c;
        }
        slotColor = new int[in.size()];
        clash = false;
        boolean devilSpirit = false;
        for (int i = 0; i < in.size(); i++) {
            int c = UiTheme.CYAN;
            for (int j = 0; j < in.size(); j++) {
                if (i == j) continue;
                if (AttuneRules.devilSpirit(in.get(i).system(), in.get(j).system())) { c = UiTheme.RED; clash = true; devilSpirit = true; break; }
                AttuneRules.Pair pr = AttuneRules.pair(in.get(i).elements(), in.get(j).elements());
                if (pr == AttuneRules.Pair.REJECT) { c = UiTheme.RED; clash = true; break; }
                if (pr == AttuneRules.Pair.RESONATE) c = UiTheme.GOLD;
            }
            slotColor[i] = c;
        }
        int pairs = AttuneRules.resonatingPairs(in);
        int bonus = (int) Math.round((AttuneRules.resonance(pairs, data.resonancePercent() / 100.0) - 1.0) * 100);
        if (devilSpirit) { footer = Component.translatableWithFallback("attune.screen.devil_spirit", "A Devil and a Spirit cannot be held together.").getString(); footerColor = UiTheme.RED; }
        else if (clash) { footer = Component.translatableWithFallback("attune.screen.clash", "Clashing elements: these powers reject each other.").getString(); footerColor = UiTheme.RED; }
        else { footer = Component.translatableWithFallback("attune.screen.resonance", "Resonance +%s%%", bonus).getString(); footerColor = bonus > 0 ? UiTheme.GOLD : UiTheme.TEXT_DIM; }
        if (confirm != null) confirm.active = !clash && cooldownLeft() <= 0;
    }

    private int cooldownLeft() { return Math.max(0, data.cooldownTicks() - ticks); }

    private void updateCooldownText() {
        int cd = cooldownLeft();
        cooldownText = cd > 0 ? Component.translatableWithFallback("attune.screen.cooldown", "Settling: %s s", (cd + 19) / 20).getString() : "";
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks % 20 == 0) updateCooldownText();
        if (confirm != null) confirm.active = !clash && cooldownLeft() <= 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && ticks % 8 == 0) {                       // the calm visual: two motes, client only, bounded
            for (int i = 0; i < 2; i++) {
                double a = (ticks * 0.21 + i * Math.PI) % (Math.PI * 2);
                mc.level.addParticle(ParticleTypes.ENCHANT, mc.player.getX() + Math.cos(a) * 1.2, mc.player.getY() + 0.4 + i * 0.6, mc.player.getZ() + Math.sin(a) * 1.2, 0, 0.02, 0);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = left(), y = top();
        int sw = (W - 28) / Math.max(1, slots);
        for (int i = 0; i < chosen.size(); i++) {
            int sx = x + 14 + i * sw;
            if (mx >= sx && mx < sx + sw - 4 && my >= y + 28 && my < y + 48) { chosen.remove(i); recompute(); return true; }
        }
        int n = Math.min(ROWS * 2, data.owned().size()), cw = (W - 28) / 2;
        for (int i = 0; i < n; i++) {
            int rx = x + 14 + (i / ROWS) * cw, ry = y + 62 + (i % ROWS) * ROW;
            if (mx >= rx && mx < rx + cw - 4 && my >= ry && my < ry + ROW) { toggle(data.owned().get(i)); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void toggle(AttuneNet.Entry e) {
        if (chosen.remove(e.id())) { recompute(); return; }
        if (chosen.size() < slots) chosen.add(e.id());
        else if (!chosen.isEmpty()) chosen.set(chosen.size() - 1, e.id());
        recompute();
    }

    /** Everything is drawn here, after the flat dim and never the blurring vanilla background (ScreenBlurGuardTest); vanilla's render then draws the two buttons over it. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        float breath = (Mth.sin((ticks + partial) * 0.06f) + 1f) * 0.5f;                     // about one breath in five seconds
        int tint = ((int) (18 + 30 * breath) << 24) | 0x3A6CFF;
        g.fillGradient(0, 0, width, height / 3, tint, 0x003A6CFF);
        g.fillGradient(0, height - height / 3, width, height, 0x003A6CFF, tint);
        int x = left(), y = top();
        UiPanel.panel(g, x, y, W, H, clash ? UiTheme.RED : UiTheme.CYAN, 0.5f + 0.4f * breath, 1f);
        g.drawCenteredString(font, getTitle(), x + W / 2, y + 10, UiTheme.GOLD);
        int sw = (W - 28) / Math.max(1, slots);
        for (int i = 0; i < slots; i++) {
            int sx = x + 14 + i * sw;
            boolean used = i < chosen.size();
            int c = used && i < slotColor.length ? slotColor[i] : UiTheme.TEXT_FAINT;
            g.fill(sx, y + 28, sx + sw - 4, y + 48, 0x66000000);
            g.renderOutline(sx, y + 28, sw - 4, 20, c);
            if (used) {
                AttuneNet.Entry e = find(chosen.get(i));
                String t = e == null ? chosen.get(i) : e.label();
                g.drawString(font, font.plainSubstrByWidth(t, sw - 10), sx + 3, y + 34, c, false);
            } else g.drawString(font, "-", sx + (sw - 4) / 2 - 2, y + 34, UiTheme.TEXT_FAINT, false);
        }
        int n = Math.min(Math.min(ROWS * 2, data.owned().size()), rowColor.length), cw = (W - 28) / 2;
        if (n == 0) g.drawString(font, font.plainSubstrByWidth(noneText, W - 28), x + 14, y + 66, UiTheme.TEXT_DIM, false);
        for (int i = 0; i < n; i++) {
            AttuneNet.Entry e = data.owned().get(i);
            int rx = x + 14 + (i / ROWS) * cw, ry = y + 62 + (i % ROWS) * ROW;
            if (mouseX >= rx && mouseX < rx + cw - 4 && mouseY >= ry && mouseY < ry + ROW) g.fill(rx, ry, rx + cw - 4, ry + ROW, 0x22FFFFFF);
            g.drawString(font, font.plainSubstrByWidth(SYSTEM_TAG[Math.max(0, Math.min(3, e.system()))] + e.label(), cw - 8), rx + 2, ry + 2, rowColor[i], false);
        }
        g.drawString(font, footer, x + 14, y + H - 42, footerColor, false);
        if (!cooldownText.isEmpty()) g.drawString(font, cooldownText, x + W - 14 - font.width(cooldownText), y + H - 42, UiTheme.TEXT_DIM, false);
    }

    private static final String[] SYSTEM_TAG = {"[Beast] ", "[Djinn] ", "[Devil] ", "[Spirit] "};
}
