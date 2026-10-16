package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.menu.AvatarBuilderMenu;
import com.newuniverse.nusmp.avatar.menu.BuilderState;
import com.newuniverse.nusmp.avatar.net.C2S_FinalizeAvatarPayload;
import com.newuniverse.nusmp.avatar.net.C2S_RequestStatRollPayload;
import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

/**
 * Why: the creation screen of the Genesis Core, in the mod's glass look. Four pages (stats, race and personality, appearance, confirm) behind a menu
 * the server opened; the screen only collects CHOICES and shows what the server answered (the roll, the races, a refusal): it never decides a stat, a
 * luck value or whether a race is allowed. The rules for which button is lit are the pure {@link BuilderState}; the look is clamped by
 * {@link DynamicAppearanceEngine} (the server clamps again). A live preview of the Avatar turns on the right.
 */
public class AvatarBuilderScreen extends AbstractContainerScreen<AvatarBuilderMenu> {
    private static final int W = 384, H = 232, PREVIEW_X = 268, PREVIEW_W = 108;
    private static final String[] STATS = {"str", "dex", "con", "int", "wis", "cha"};
    private static final int ROLL_ANIM = 26;

    private final BuilderState state = new BuilderState();
    private final Random rng = new Random();
    private AppearanceData look;
    private final long seed = System.nanoTime();

    private int seenRaces = -1, seenRoll = -1, seenResult = -1;
    private int rollAnim;                 // ticks of dice animation left
    private int raceScroll, raceFlash;
    private EditBox nameBox;
    private Component refusal = Component.empty();
    private boolean refusalOk;
    private float spin, age;

    public AvatarBuilderScreen(AvatarBuilderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        look = DynamicAppearanceEngine.defaultsFor("", 1.0f, seed);
    }

    // ================================================================ data
    private RaceEntry race() {
        for (RaceEntry r : AvatarClientData.races) if (r.id().equals(state.race())) return r;
        return null;
    }

    private float raceScale() { RaceEntry r = race(); return r == null ? 1.0f : r.scale(); }

    /** Takes what the server sent since last time; true when anything changed (the page is rebuilt then). */
    private boolean pollServer() {
        boolean changed = seenRaces != AvatarClientData.racesVersion || seenRoll != AvatarClientData.rollVersion || seenResult != AvatarClientData.resultVersion;
        if (seenRaces != AvatarClientData.racesVersion) {
            seenRaces = AvatarClientData.racesVersion;
            RaceEntry r = race();
            if (r != null && !r.available()) state.chooseRace("", false);
        }
        if (seenRoll != AvatarClientData.rollVersion) {
            seenRoll = AvatarClientData.rollVersion;
            if (AvatarClientData.roll != null) { state.onRolled(); rollAnim = ROLL_ANIM; }
            state.setBusy(false);
        }
        if (seenResult != AvatarClientData.resultVersion) {
            seenResult = AvatarClientData.resultVersion;
            var res = AvatarClientData.result;
            if (res != null) {
                refusal = Component.translatableWithFallback(res.message(), res.message());
                refusalOk = res.ok();
                state.setBusy(false);
            }
        }
        return changed;
    }

    // ================================================================ widgets
    @Override
    protected void init() {
        super.init();
        pollServer();
        buildPage();
    }

    private void buildPage() {
        clearWidgets();
        nameBox = null;
        int x = leftPos, y = topPos;
        int pg = state.page();
        // tabs
        String[] tabs = {"stats", "race", "look", "confirm"};
        for (int i = 0; i < tabs.length; i++) {
            final int target = i;
            addRenderableWidget(new ThemedButton(x + 10 + i * 62, y + 8, 60, 14, Component.translatable("nusmp.avatar.gui.tab." + tabs[i]), b -> { if (state.goTo(target)) buildPage(); }));
        }
        // page controls
        switch (pg) {
            case BuilderState.PAGE_STATS -> {
                addRenderableWidget(new ThemedButton(x + 10, y + H - 24, 96, 16, Component.translatable(state.rolled() ? "nusmp.avatar.gui.reroll" : "nusmp.avatar.gui.roll"), b -> requestRoll()));
            }
            case BuilderState.PAGE_RACE -> buildRacePage(x, y);
            case BuilderState.PAGE_LOOK -> buildLookPage(x, y);
            default -> buildConfirmPage(x, y);
        }
        // back / next / create
        if (pg > 0) addRenderableWidget(new ThemedButton(x + W - 168, y + H - 24, 50, 16, Component.translatable("nusmp.avatar.gui.back"), b -> { if (state.back()) buildPage(); }));
        if (pg < BuilderState.PAGE_CONFIRM) {
            ThemedButton next = new ThemedButton(x + W - 112, y + H - 24, 100, 16, Component.translatable("nusmp.avatar.gui.next"), b -> { if (state.next()) buildPage(); });
            next.active = state.canNext();
            addRenderableWidget(next);
        } else {
            ThemedButton create = new ThemedButton(x + W - 112, y + H - 24, 100, 16, Component.translatable("nusmp.avatar.gui.create"), b -> finalizeAvatar());
            create.active = state.canCreate();
            addRenderableWidget(create);
        }
    }

    private void requestRoll() {
        if (state.busy()) return;
        state.setBusy(true);
        refusal = Component.empty();
        PacketDistributor.sendToServer(new C2S_RequestStatRollPayload());
    }

    private void finalizeAvatar() {
        if (!state.canCreate()) return;
        state.setBusy(true);
        refusal = Component.empty();
        RaceEntry r = race();
        AppearanceData sane = DynamicAppearanceEngine.sanitize(look, r == null ? 1.0f : r.scale());
        String sec = state.secondary() == null ? "" : state.secondary().name();
        PacketDistributor.sendToServer(new C2S_FinalizeAvatarPayload(state.race(), state.primary().name(), sec, sane.toTag(), state.name().strip(), AvatarClientData.roll == null ? 0L : AvatarClientData.roll.token()));
        buildPage();
    }

    // ---- page 2: race and personality
    private void buildRacePage(int x, int y) {
        // personality cyclers
        AvatarPersonality[] all = AvatarPersonality.values();
        int py = y + 150;
        addRenderableWidget(new ThemedButton(x + 10, py, 14, 14, Component.literal("<"), b -> { state.choosePrimary(step(all, state.primary(), -1, false)); buildPage(); }));
        addRenderableWidget(new ThemedButton(x + 126, py, 14, 14, Component.literal(">"), b -> { state.choosePrimary(step(all, state.primary(), 1, false)); buildPage(); }));
        addRenderableWidget(new ThemedButton(x + 10, py + 22, 14, 14, Component.literal("<"), b -> { state.chooseSecondary(step(all, state.secondary(), -1, true)); buildPage(); }));
        addRenderableWidget(new ThemedButton(x + 126, py + 22, 14, 14, Component.literal(">"), b -> { state.chooseSecondary(step(all, state.secondary(), 1, true)); buildPage(); }));
    }

    /** The next personality in a list; with 'optional' the step passes through "none" (null). */
    private AvatarPersonality step(AvatarPersonality[] all, AvatarPersonality cur, int d, boolean optional) {
        int n = all.length + (optional ? 1 : 0);
        int i = cur == null ? (optional ? n - 1 : -1) : cur.ordinal();
        i = Math.floorMod(i + d, n);
        if (optional && i == n - 1) return null;
        return all[i];
    }

    // ---- page 3: appearance
    private void buildLookPage(int x, int y) {
        RaceEntry re = race();
        float base = raceScale();
        int[] pal = DynamicAppearanceEngine.SKIN_PALETTE;
        int row = y + 32;
        cycler(x + 10, row, d -> {
            int idx = 0;
            for (int i = 0; i < pal.length; i++) if (pal[i] == look.skinTone()) idx = i;
            look = look.withSkin(pal[Math.floorMod(idx + d, pal.length)]);
        });
        cycler(x + 10, row + 20, d -> look = look.withHair(Math.floorMod(look.hairId() + d, AppearanceData.HAIR_COUNT)));
        cycler(x + 10, row + 40, d -> look = look.withOutfit(Math.floorMod(look.outfitId() + d, AppearanceData.OUTFIT_COUNT)));
        cycler(x + 10, row + 60, d -> look = look.withHorn(Math.floorMod(look.hornId() + 1 + d, AppearanceData.HORN_COUNT + 1) - 1));
        cycler(x + 10, row + 80, d -> look = look.withWing(Math.floorMod(look.wingId() + 1 + d, AppearanceData.WING_COUNT + 1) - 1));
        float lo = DynamicAppearanceEngine.minScale(base), hi = DynamicAppearanceEngine.maxScale(base);
        addRenderableWidget(new ValueSlider(x + 10, row + 102, 118, 14, hi > lo ? (look.scale() - lo) / (hi - lo) : 0.5, UiTheme.GREEN,
                v -> Component.translatable("nusmp.avatar.gui.look.scale", String.format(java.util.Locale.ROOT, "%.2f", lo + v * (hi - lo))),
                v -> look = look.withScale((float) (lo + v * (hi - lo)))));
        addRenderableWidget(new ThemedButton(x + 10, row + 122, 118, 14, Component.translatable("nusmp.avatar.gui.look.random"),
                b -> { look = DynamicAppearanceEngine.defaultsFor(state.race(), base, rng.nextLong()); buildPage(); }));
        // colours: aura and eyes as three sliders each
        int cx = x + 138;
        addColour(cx, row, "aura", look.auraRgb(), c -> look = look.withAura(c));
        addColour(cx, row + 62, "eyes", look.eyeRgb(), c -> look = look.withEye(c));
        // skin shade
        addRenderableWidget(new ValueSlider(cx, row + 124, 120, 12, shadeOf(look.skinTone()), UiTheme.GOLD,
                v -> Component.translatable("nusmp.avatar.gui.look.shade"), v -> look = look.withSkin(shaded(look.skinTone(), v))));
    }

    private static double shadeOf(int rgb) {
        int mx = Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
        return Math.min(1.0, mx / 255.0);
    }

    /** The same hue as rgb with its brightest channel set to v (0.25 .. 1.0 of full). */
    private static int shaded(int rgb, double v) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255, mx = Math.max(1, Math.max(r, Math.max(g, b)));
        double k = (0.25 + 0.75 * v) * 255.0 / mx;
        return (Math.min(255, (int) (r * k)) << 16) | (Math.min(255, (int) (g * k)) << 8) | Math.min(255, (int) (b * k));
    }

    private void addColour(int x, int y, String key, int rgb, java.util.function.IntConsumer set) {
        int[] c = {(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255};
        int[] tint = {0xFFFF6A6A, 0xFF7CF0B0, 0xFF5CD8FF};
        String[] ch = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            addRenderableWidget(new ValueSlider(x, y + 14 + i * 15, 120, 12, c[i] / 255.0, tint[i],
                    v -> Component.literal(ch[k] + " " + (int) Math.round(v * 255)),
                    v -> { c[k] = (int) Math.round(v * 255); set.accept((c[0] << 16) | (c[1] << 8) | c[2]); }));
        }
    }

    /** A "<" and ">" pair around the current value (drawn by drawLook); the callback changes the look by -1 or +1. */
    private void cycler(int x, int y, java.util.function.IntConsumer change) {
        addRenderableWidget(new ThemedButton(x + 44, y, 14, 14, Component.literal("<"), b -> change.accept(-1)));
        addRenderableWidget(new ThemedButton(x + 114, y, 14, 14, Component.literal(">"), b -> change.accept(1)));
    }

    // ---- page 4: confirm
    private void buildConfirmPage(int x, int y) {
        nameBox = new EditBox(font, x + 10, y + 150, 150, 14, Component.translatable("nusmp.avatar.gui.confirm.name"));
        nameBox.setMaxLength(BuilderState.NAME_MAX);
        nameBox.setValue(state.name());
        nameBox.setHint(Component.translatable("nusmp.avatar.gui.confirm.name_hint"));
        nameBox.setResponder(s -> { state.setName(s); updateCreateButton(); });
        addRenderableWidget(nameBox);
    }

    private void updateCreateButton() {
        for (var w : children()) if (w instanceof ThemedButton tb && tb.getMessage().equals(Component.translatable("nusmp.avatar.gui.create"))) tb.active = state.canCreate();
    }

    // ================================================================ input
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
            nameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;                       // typing an 'e' must not close the screen as the inventory key would
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (state.page() == BuilderState.PAGE_RACE && button == 0) {
            int x = leftPos + 10, y = topPos + 30;
            List<RaceEntry> rs = AvatarClientData.races;
            if (mx >= x && mx < x + 116 && my >= y && my < y + 8 * 14) {
                int i = raceScroll + (int) ((my - y) / 14);
                if (i >= 0 && i < rs.size()) {
                    RaceEntry r = rs.get(i);
                    if (r.available()) {
                        boolean changed = !r.id().equals(state.race());
                        state.chooseRace(r.id(), true);
                        if (changed) look = DynamicAppearanceEngine.sanitize(look.withScale(r.scale()), r.scale());
                        buildPage();
                    } else raceFlash = 20;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (state.page() == BuilderState.PAGE_RACE) {
            raceScroll = Math.max(0, Math.min(Math.max(0, AvatarClientData.races.size() - 8), raceScroll - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        age++;
        if (rollAnim > 0) rollAnim--;
        if (raceFlash > 0) raceFlash--;
        if (pollServer()) buildPage();
    }

    // ================================================================ drawing
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { /* the panel draws its own texts */ }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        float now = UiTheme.now();
        int x = leftPos, y = topPos;
        UiPanel.panelScan(g, x, y, W, H, UiTheme.CYAN, 0.8f, 1f, now);
        // preview panel
        UiPanel.well(g, x + PREVIEW_X, y + 28, PREVIEW_W, H - 56, UiTheme.VIOLET, 0.9f);
        g.enableScissor(x + PREVIEW_X, y + 28, x + PREVIEW_X + PREVIEW_W, y + H - 28);
        MagicCircle.draw(g, x + PREVIEW_X + PREVIEW_W / 2f, y + 150, 48f, now, look.auraRgb() & 0xFFFFFF | 0xFF000000, 0.55f, MagicCircle.CALM, 1f);
        g.disableScissor();
        spin = (age + partial) * 3f;                  // a function of the clock, not of the frame rate
        AvatarPreview.draw(g, x + PREVIEW_X + PREVIEW_W / 2, y + H - 52, 52f, look, state.race(), spin, age + partial);
        String pn = state.primary() == null ? "?" : state.primary().displayName;
        g.drawCenteredString(font, Component.translatable("nusmp.avatar.gui.preview"), x + PREVIEW_X + PREVIEW_W / 2, y + 32, UiTheme.TEXT_DIM);
        switch (state.page()) {
            case BuilderState.PAGE_STATS -> drawStats(g, x, y, now);
            case BuilderState.PAGE_RACE -> drawRace(g, x, y, mx, my);
            case BuilderState.PAGE_LOOK -> drawLook(g, x, y);
            default -> drawConfirm(g, x, y);
        }
        if (!refusal.getString().isEmpty())
            g.drawString(font, refusal, x + 10, y + H - 40, refusalOk ? UiTheme.GREEN : UiTheme.RED);
    }

    private void drawStats(GuiGraphics g, int x, int y, float now) {
        S2C_RollPayload r = AvatarClientData.roll;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.stats.title"), x + 10, y + 30, UiTheme.GOLD);
        for (int i = 0; i < 6; i++) {
            int bx = x + 10 + (i % 3) * 82, by = y + 46 + (i / 3) * 56;
            UiPanel.well(g, bx, by, 76, 48, UiTheme.CYAN, 0.9f);
            g.drawCenteredString(font, Component.translatable("nusmp.avatar.gui.stat." + STATS[i]), bx + 38, by + 5, UiTheme.TEXT_DIM);
            String num;
            int col = UiTheme.TEXT;
            if (r == null) { num = "--"; col = UiTheme.TEXT_FAINT; }
            else {
                int settle = ROLL_ANIM - 4 * i - 6;          // the stats settle one after another
                boolean rolling = rollAnim > settle && settle > 0;
                num = rolling ? String.valueOf(3 + (int) ((age * 7 + i * 5 + rollAnim * 13) % 16)) : String.valueOf(r.stats()[i]);
                if (!rolling) col = r.stats()[i] >= 16 ? UiTheme.GOLD : r.stats()[i] <= 8 ? UiTheme.RED : UiTheme.TEXT;
            }
            g.pose().pushPose();
            g.pose().translate(bx + 38, by + 18, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, num, 0, 0, col);
            g.pose().popPose();
            if (r != null && rollAnim <= 0) {
                int mod = Math.floorDiv(r.stats()[i] - 10, 2);
                g.drawCenteredString(font, (mod >= 0 ? "+" : "") + mod, bx + 38, by + 37, mod >= 0 ? UiTheme.GREEN : UiTheme.RED);
            }
        }
        int ty = y + 164;
        if (r == null) {
            g.drawString(font, Component.translatable("nusmp.avatar.gui.stats.hint"), x + 10, ty, UiTheme.TEXT_DIM);
        } else {
            g.drawString(font, Component.translatable(r.fiveDice() ? "nusmp.avatar.gui.dice.five" : "nusmp.avatar.gui.dice.four"), x + 10, ty, r.fiveDice() ? UiTheme.GOLD : UiTheme.TEXT);
            g.drawString(font, Component.translatable("nusmp.avatar.gui.luck", String.format(java.util.Locale.ROOT, "%.1f", r.luck())), x + 10, ty + 12, UiTheme.VIOLET);
            if (r.rerollCost() > 0) g.drawString(font, Component.translatable("nusmp.avatar.gui.reroll_cost", r.rerollCost()), x + 10, ty + 24, UiTheme.TEXT_DIM);
        }
    }

    private void drawRace(GuiGraphics g, int x, int y, int mx, int my) {
        List<RaceEntry> rs = AvatarClientData.races;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.race.title"), x + 10, y + 26 - 0, UiTheme.GOLD);
        UiPanel.well(g, x + 10, y + 36 - 6 + 0, 116, 8 * 14 + 2, UiTheme.CYAN, 0.9f);
        RaceEntry hover = null;
        for (int i = 0; i < 8; i++) {
            int idx = raceScroll + i;
            if (idx >= rs.size()) break;
            RaceEntry r = rs.get(idx);
            int ry = y + 30 + i * 14;
            boolean sel = r.id().equals(state.race()), over = mx >= x + 10 && mx < x + 126 && my >= ry && my < ry + 14;
            if (over) hover = r;
            if (sel) UiPanel.hover(g, x + 11, ry, 114, 14, 1f, UiTheme.CYAN);
            else if (over) UiPanel.hover(g, x + 11, ry, 114, 14, 0.5f, r.available() ? UiTheme.CYAN : UiTheme.RED);
            int col = !r.available() ? UiTheme.TEXT_FAINT : sel ? UiTheme.WHITE : UiTheme.TEXT;
            g.drawString(font, font.plainSubstrByWidth(Component.translatableWithFallback(r.name(), r.name()).getString(), 108), x + 14, ry + 3, col);
        }
        if (rs.isEmpty()) g.drawString(font, Component.translatable("nusmp.avatar.gui.race.none"), x + 14, y + 36, UiTheme.TEXT_DIM);
        // details of the selected (or hovered) race
        RaceEntry d = hover != null ? hover : race();
        int dx = x + 134;
        if (d != null) {
            g.drawString(font, Component.translatableWithFallback(d.name(), d.name()), dx, y + 30, d.available() ? UiTheme.GOLD : UiTheme.TEXT_DIM);
            g.drawString(font, Component.translatable("nusmp.avatar.gui.race.scale", String.format(java.util.Locale.ROOT, "%.2f", d.scale())), dx, y + 42, UiTheme.TEXT);
            g.drawString(font, Component.translatable("nusmp.avatar.gui.race.ep", (long) d.ep()), dx, y + 53, UiTheme.TEXT);
            g.drawString(font, Component.translatable("nusmp.avatar.gui.race.magicule", (long) d.magicule()), dx, y + 64, UiTheme.TEXT);
            int ly = y + 75;
            if (!d.statMults().isEmpty()) g.drawString(font, font.plainSubstrByWidth(String.join(" ", d.statMults()).toUpperCase(java.util.Locale.ROOT), 124), dx, ly, UiTheme.TEXT_DIM);
            ly += 11;
            if (!d.evolvesTo().isEmpty()) { g.drawString(font, Component.translatable("nusmp.avatar.gui.race.evolves", d.evolvesTo()), dx, ly, UiTheme.VIOLET); ly += 11; }
            if (d.requiresDnd()) { g.drawString(font, Component.translatable("nusmp.avatar.gui.race.dnd"), dx, ly, UiTheme.GOLD); ly += 11; }
            if (!d.available()) {
                g.drawString(font, Component.translatable("nusmp.avatar.gui.race.locked"), dx, ly, (raceFlash > 0 && raceFlash % 6 < 3) ? UiTheme.WHITE : UiTheme.RED); ly += 11;
                for (String f : d.failed()) { g.drawString(font, font.plainSubstrByWidth("- " + Component.translatableWithFallback("nusmp.avatar.cond." + f.replace(':', '.'), f).getString(), 124), dx, ly, UiTheme.TEXT_FAINT); ly += 10; }
            }
        }
        // personalities
        int py = y + 150;
        AvatarPersonality p = state.primary(), s = state.secondary();
        g.drawString(font, Component.translatable("nusmp.avatar.gui.primary"), x + 10, py - 11, UiTheme.GOLD);
        g.drawCenteredString(font, p == null ? Component.translatable("nusmp.avatar.gui.choose") : Component.literal(p.displayName), x + 75, py + 3, p == null ? UiTheme.TEXT_DIM : UiTheme.TEXT);
        g.drawString(font, Component.translatable("nusmp.avatar.gui.secondary"), x + 10, py + 11, UiTheme.GOLD);
        g.drawCenteredString(font, s == null ? Component.translatable("nusmp.avatar.gui.look.none") : Component.literal(s.displayName), x + 75, py + 25, s == null ? UiTheme.TEXT_DIM : UiTheme.TEXT);
        AvatarPersonality show = p;
        if (show != null) g.drawString(font, font.plainSubstrByWidth(Component.translatable("nusmp.avatar.persona." + show.name().toLowerCase(java.util.Locale.ROOT)).getString(), 112), x + 148, py + 3, UiTheme.TEXT_DIM);
        if (p != null && s != null)
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("nusmp.avatar.gui.hybrid", p.displayName, s.displayName).getString(), 112), x + 148, py + 25, UiTheme.VIOLET);
        else if (s != null)
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("nusmp.avatar.persona." + s.name().toLowerCase(java.util.Locale.ROOT)).getString(), 112), x + 148, py + 25, UiTheme.TEXT_DIM);
    }

    private void drawLook(GuiGraphics g, int x, int y) {
        g.drawString(font, Component.translatable("nusmp.avatar.gui.look.title"), x + 10, y + 26, UiTheme.GOLD);
        int row = y + 32;
        String[] labels = {"skin", "hair", "outfit", "horn", "wing"};
        for (int i = 0; i < 5; i++) {
            int ry = row + i * 20;
            g.drawString(font, Component.translatable("nusmp.avatar.gui.look." + labels[i]), x + 10, ry + 3, UiTheme.TEXT_DIM);
        }
        // current values between the arrows
        g.fill(x + 60, row + 1, x + 70, row + 13, 0xFF000000 | look.skinTone());
        g.renderOutline(x + 60, row + 1, 10, 12, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("nusmp.avatar.hair." + look.hairId()), x + 93, row + 23, UiTheme.TEXT);
        g.drawCenteredString(font, Component.translatable("nusmp.avatar.outfit." + look.outfitId()), x + 93, row + 43, UiTheme.TEXT);
        g.drawCenteredString(font, look.hornId() < 0 ? Component.translatable("nusmp.avatar.gui.look.none") : Component.translatable("nusmp.avatar.horn." + look.hornId()), x + 93, row + 63, UiTheme.TEXT);
        g.drawCenteredString(font, look.wingId() < 0 ? Component.translatable("nusmp.avatar.gui.look.none") : Component.translatable("nusmp.avatar.wing." + look.wingId()), x + 93, row + 83, UiTheme.TEXT);
        int cx = x + 138;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.look.aura"), cx, row + 2, UiTheme.TEXT_DIM);
        g.fill(cx + 100, row + 1, cx + 120, row + 11, 0xFF000000 | look.auraRgb());
        g.drawString(font, Component.translatable("nusmp.avatar.gui.look.eyes"), cx, row + 64, UiTheme.TEXT_DIM);
        g.fill(cx + 100, row + 63, cx + 120, row + 73, 0xFF000000 | look.eyeRgb());
    }

    private void drawConfirm(GuiGraphics g, int x, int y) {
        S2C_RollPayload r = AvatarClientData.roll;
        RaceEntry re = race();
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.title"), x + 10, y + 26, UiTheme.GOLD);
        int ly = y + 40;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.race", re == null ? "?" : Component.translatableWithFallback(re.name(), re.name()).getString()), x + 10, ly, UiTheme.TEXT); ly += 12;
        AvatarPersonality p = state.primary(), s = state.secondary();
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.persona", p == null ? "?" : p.displayName, s == null ? "-" : s.displayName), x + 10, ly, UiTheme.TEXT); ly += 12;
        if (r != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(STATS[i].toUpperCase(java.util.Locale.ROOT)).append(' ').append(r.stats()[i]).append(i < 5 ? "  " : "");
            g.drawString(font, sb.toString(), x + 10, ly, UiTheme.TEXT); ly += 12;
            g.drawString(font, Component.translatable("nusmp.avatar.gui.luck", String.format(java.util.Locale.ROOT, "%.1f", r.luck())), x + 10, ly, UiTheme.VIOLET); ly += 12;
        }
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.look", look.hairId() + 1, look.outfitId() + 1, String.format(java.util.Locale.ROOT, "%.2f", look.scale())), x + 10, ly, UiTheme.TEXT_DIM); ly += 16;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.warn"), x + 10, ly, UiTheme.TEXT_DIM); ly += 11;
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.warn2"), x + 10, ly, UiTheme.TEXT_DIM);
        g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.name"), x + 10, y + 139, UiTheme.GOLD);
        if (!BuilderState.nameOk(state.name())) g.drawString(font, Component.translatable("nusmp.avatar.gui.confirm.name_bad"), x + 166, y + 153, UiTheme.RED);
    }
}
