package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.menu.BuilderState;
import com.newuniverse.nusmp.avatar.menu.DiceSpin;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.menu.ForgeMotion;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.avatar.menu.SoulForgeMenu;
import com.newuniverse.nusmp.avatar.net.C2S_AptitudeRollPayload;
import com.newuniverse.nusmp.avatar.net.C2S_BarkRequestPayload;
import com.newuniverse.nusmp.avatar.net.C2S_FinalizeSoulForgePayload;
import com.newuniverse.nusmp.avatar.net.C2S_RequestStatRollPayload;
import com.newuniverse.nusmp.avatar.net.ForgeAptitudeRow;
import com.newuniverse.nusmp.avatar.net.ForgeSkillRow;
import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: this is the owner's "character creator" for the Avatar, opened by the Soul Forge ritual: a full-screen scene in the spirit of a life-simulation
 * character screen. Five round category buttons across the top (Identity, Appearance, Traits, Skills, Aptitudes), the module of the open category sliding
 * in from the left with an eased motion, the Avatar turning in the middle and reacting live to every choice (its look changes at once, a ring of the choice's
 * colour opens round it, the aura leans toward what the pointer is on, the gifts it was given circle its feet), the price panel on the right, and the
 * Back / Next / Finalize bar at the bottom. All the RULES are pure and tested ({@link ForgeState}, {@link ForgeLayout}, {@link ForgeMotion}, {@code DiceSpin},
 * {@code SlotReel}); this class only draws, routes the mouse and keyboard, and talks to the network. The screen decides nothing: the server rolls the stats
 * and the aptitude, re-reads the creator's reserves and validates every choice on Finalize, and the slide of the birth begins only when it says yes.
 * The 3D Avatar is drawn by the repo's own GeoLite preview ({@code AvatarPreview}).
 */
public class SoulForgeScreen extends AbstractContainerScreen<SoulForgeMenu> implements ForgeHost {
    private static final int[] PAGE_IDS = {ForgeState.PAGE_IDENTITY, ForgeState.PAGE_APPEARANCE, ForgeState.PAGE_TRAITS, ForgeState.PAGE_SKILLS, ForgeState.PAGE_APTITUDES};

    private final ForgeState state = new ForgeState();
    private final ForgeMotion.ModuleSlide slide = new ForgeMotion.ModuleSlide(ForgeState.PAGE_IDENTITY);
    private final ForgeMotion.Hovers hovers = new ForgeMotion.Hovers();
    private final long seed = System.nanoTime();
    private final DiceSpin dice = new DiceSpin(seed);
    private AppearanceData look;
    private ForgeLayout.Layout lay = ForgeLayout.compute(320, 240);
    /** The whole-screen zoom (a whole number): the scene is laid out on a canvas of width/ui by height/ui and drawn ui times as big. */
    private int ui = 1, canvasW = 320, canvasH = 240;
    private final ForgePage[] pages = new ForgePage[ForgeState.PAGES];
    private EditBox nameBox;

    // the clock
    private float lastReal = -1f, dt, age;
    private boolean wasSettled = true;

    // what the server told us
    private int seenState = -1, seenStatRoll = -1, seenResult = -1, seenBark = -1, seenRaces = -1;
    private int busyTicks;
    private Component sayText = Component.empty();
    private boolean sayGood;
    private int sayTicks;
    private long lastBarkMs;

    // the cost panel and what stands in the way, rebuilt when a choice changes
    private List<ForgeState.Problem> problems = List.of();
    private ForgeState.Ledger ledger;
    private boolean dirty = true;

    // the stage
    private final float[] tint = {0.4f, 0.8f, 1f};
    private int leanRgb = -1;
    private float pulseAge = 99f;
    private int pulseRgb = 0x9A7CFF;
    private float userSpin;
    private boolean draggingStage;
    private String barkText = "", barkEvent = "";
    private float barkAge = 999f;
    private List<Component> tip;
    private int hoveredCategory = -1;

    public SoulForgeScreen(SoulForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.look = DynamicAppearanceEngine.defaultsFor("", 1.0f, seed);
        int[] a = splitRgb(look.auraRgb());
        for (int i = 0; i < 3; i++) tint[i] = a[i] / 255f;
        pages[0] = new IdentityPage(this);
        pages[1] = new AppearancePage(this);
        pages[2] = new TraitsPage(this);
        pages[3] = new SkillsPage(this);
        pages[4] = new AptitudesPage(this);
    }

    private static int[] splitRgb(int rgb) { return new int[]{(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255}; }

    // ================================================================ ForgeHost
    @Override public ForgeState state() { return state; }
    @Override public net.minecraft.client.gui.Font font() { return font; }
    @Override public float now() { return UiTheme.now(); }
    @Override public boolean motion() { return UiTheme.motion(); }
    @Override public ForgeRules rules() { return SoulForgeClientData.rules; }
    @Override public ForgeContext reserves() { return SoulForgeClientData.reserves; }
    @Override public int rollsLeft() { return SoulForgeClientData.rollsLeft; }
    @Override public AppearanceData look() { return look; }
    @Override public void setLook(AppearanceData d) { look = d; }
    @Override public int iconSize() { return lay.icon(); }
    @Override public int slotSize() { return lay.slot(); }
    @Override public DiceSpin dice() { return dice; }
    @Override public EditBox nameBox() { return nameBox; }

    @Override
    public RaceEntry raceEntry() {
        for (RaceEntry r : AvatarClientData.races) if (r.id().equals(state.race())) return r;
        return null;
    }

    @Override public float hover(Object key, boolean hovered) { return hovers.step(key, hovered, UiTheme.motion() ? dt : 1000f, 0.3f); }

    @Override public void tooltip(List<Component> lines) { tip = wrapTip(lines); }

    /** A vanilla tooltip never wraps a long line by itself, so the lines are cut to a readable width here, each piece keeping the colour of its line. */
    private List<Component> wrapTip(List<Component> lines) {
        int max = Math.max(110, Math.min(230, width - 40));
        List<Component> out = new ArrayList<>();
        for (Component c : lines) {
            String s = c.getString();
            if (font.width(s) <= max) { out.add(c); continue; }
            for (String piece : ForgeUi.wrapLines(font, s, max, 8)) out.add(Component.literal(piece).withStyle(c.getStyle()));
        }
        return out;
    }

    @Override
    public void scissor(GuiGraphics g, Rect r) {
        int off = offX();
        g.enableScissor((r.x() + off) * ui, r.y() * ui, (r.right() + off) * ui, r.bottom() * ui);
    }

    @Override public void endScissor(GuiGraphics g) { g.disableScissor(); }

    @Override
    public void click() { play(SoundEvents.UI_BUTTON_CLICK, 1.0f); }

    @Override
    public void tickSound(float pitch) { play(SoundEvents.UI_BUTTON_CLICK, Math.max(0.5f, Math.min(2.0f, pitch))); }

    private void play(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> ev, float pitch) {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch));
        } catch (RuntimeException noSound) {
            // a sound that cannot play never stops the screen
        }
    }

    @Override public void changed() { dirty = true; }

    @Override
    public void pulse(int rgb) {
        pulseRgb = rgb & 0xFFFFFF;
        pulseAge = 0f;
    }

    @Override public void leanTo(int rgb) { leanRgb = rgb & 0xFFFFFF; }

    @Override
    public void say(Component message, boolean good) {
        sayText = message == null ? Component.empty() : message;
        sayGood = good;
        sayTicks = 140;
    }

    @Override
    public void requestStatRoll() { PacketDistributor.sendToServer(new C2S_RequestStatRollPayload()); }

    @Override
    public void requestAptitudeRoll() { PacketDistributor.sendToServer(new C2S_AptitudeRollPayload(state.picks())); }

    @Override
    public void requestBark() {
        AvatarPersonality p = state.primary();
        if (p == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBarkMs < 450L) return;
        lastBarkMs = now;
        AvatarPersonality s = state.secondary();
        PacketDistributor.sendToServer(new C2S_BarkRequestPayload(p.name(), s == null ? "" : s.name()));
    }

    // ================================================================ set-up
    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        ui = ForgeLayout.uiScale(width, height);
        canvasW = Math.max(1, (width + ui - 1) / ui);           // rounded up: the canvas covers the whole window (the last pixel row or column may be cut)
        canvasH = Math.max(1, (height + ui - 1) / ui);
        lay = ForgeLayout.compute(canvasW, canvasH);
        nameBox = new EditBox(font, 0, 0, 100, 10, Component.translatable("nusmp.avatar.gui.confirm.name"));
        nameBox.setBordered(false);
        nameBox.setMaxLength(BuilderState.NAME_MAX);
        nameBox.setValue(state.name());
        nameBox.setHint(ForgeText.t("nusmp.avatar.forge.name_hint", "Type a name"));
        nameBox.setResponder(s -> { state.setName(s); dirty = true; });
        addWidget(nameBox);
        for (ForgePage p : pages) p.layout(lay.leftBody());
        pollAll();
        dirty = true;
    }

    // ================================================================ server data
    private void pollAll() {
        if (seenRaces != AvatarClientData.racesVersion) {
            seenRaces = AvatarClientData.racesVersion;
            RaceEntry r = raceEntry();
            if (r != null && !r.available()) state.chooseRace("", false);
            dirty = true;
        }
        if (seenState != SoulForgeClientData.stateVersion) {
            seenState = SoulForgeClientData.stateVersion;
            state.syncRoll(SoulForgeClientData.reserves.rolledAptitude(), SoulForgeClientData.reserves.rolledToken());
            for (ForgePage p : pages) p.layout(lay.leftBody());
            dirty = true;
        }
        if (seenStatRoll != AvatarClientData.rollVersion) {
            seenStatRoll = AvatarClientData.rollVersion;
            S2C_RollPayload r = AvatarClientData.roll;
            if (r != null) {
                state.onRolled();
                int best = 1;
                for (int v : r.stats()) best = Math.max(best, v);
                dice.roll(Math.max(1, Math.min(20, best)), UiTheme.motion() ? 36 : 8);
                ((IdentityPage) pages[ForgeState.PAGE_IDENTITY]).rollArrived();
                dirty = true;
            }
        }
        if (seenResult != AvatarClientData.resultVersion) {
            seenResult = AvatarClientData.resultVersion;
            S2C_CreationResultPayload res = AvatarClientData.result;
            if (res != null) onResult(res);
        }
        if (seenBark != SoulForgeClientData.barkVersion) {
            seenBark = SoulForgeClientData.barkVersion;
            barkText = SoulForgeClientData.barkLine;
            barkEvent = SoulForgeClientData.barkEvent;
            barkAge = 0f;
        }
    }

    private void onResult(S2C_CreationResultPayload res) {
        Component msg = Component.translatableWithFallback(res.message(), res.message());
        switch (res.kind()) {
            case "roll" -> {
                ((IdentityPage) pages[ForgeState.PAGE_IDENTITY]).rollRefused();
                dice.roll(dice.value(), 16);
                if (!res.ok()) say(msg, false);
            }
            case "apt_roll" -> { state.rollFailed(); if (!res.ok()) say(msg, false); }
            case "forge" -> { state.setBusy(false); say(msg, res.ok()); }
            default -> { }
        }
    }

    private void recompute() {
        dirty = false;
        RaceEntry re = raceEntry();
        double raceCost = re == null ? 0.0 : re.magicule();
        problems = state.problems(rules(), reserves(), raceCost, SoulForgeClientData::aptitude, SoulForgeClientData::skill);
        ledger = state.ledger(rules(), reserves(), raceCost, SoulForgeClientData::aptitude, SoulForgeClientData::skill);
    }

    // ================================================================ ticks
    @Override
    protected void containerTick() {
        super.containerTick();
        pollAll();
        for (ForgePage p : pages) p.tickAlways();
        pages[slide.shown()].tick();
        if (sayTicks > 0) sayTicks--;
        if (state.busy() && ++busyTicks > 100) { state.setBusy(false); say(ForgeText.t("nusmp.avatar.forge.noanswer", "The Soul Forge did not answer."), false); }
        if (!state.busy()) busyTicks = 0;
        dirty = true;                                  // the reserves can change under us: the panel is cheap to rebuild
    }

    // ================================================================ pages
    private void requestPage(int page) {
        if (page < 0 || page >= ForgeState.PAGES || page == slide.wanted() || state.busy()) return;
        pages[slide.shown()].hidden();
        nameBox.setFocused(false);
        state.goTo(page);
        slide.request(page);
        click();
        dirty = true;
    }

    private int offX() { return Math.round(slide.offset() * (lay.left().right() + 8)); }

    // ================================================================ drawing
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) { /* the screen draws everything itself */ }

    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) { /* see render() */ }

    private void frame() {
        float real = UiTheme.realNow();
        dt = lastReal < 0f ? 0f : Math.max(0f, Math.min(4f, real - lastReal));
        lastReal = real;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        frame();
        boolean motion = UiTheme.motion();
        slide.tick(motion ? dt : 1000f);
        dice.advance(motion ? dt : 1000f);
        age += dt;
        if (pulseAge < 99f) pulseAge += dt;
        if (barkAge < 999f) barkAge += dt;
        boolean settled = slide.settled();
        if (settled && !wasSettled) pages[slide.shown()].shown();
        wasSettled = settled;
        leanRgb = -1;
        tip = null;
        if (dirty) recompute();

        int lmx = Math.floorDiv(mx, ui), lmy = Math.floorDiv(my, ui);
        drawBackdrop(g);
        g.pose().pushPose();
        g.pose().scale((float) ui, (float) ui, 1f);
        drawTopBar(g, lmx, lmy);
        drawStage(g, lmx, lmy, partial);
        drawModule(g, lmx, lmy, partial);
        drawLedger(g, lmx, lmy);
        drawBottomBar(g, lmx, lmy);
        g.pose().popPose();
        easeTint();
        hovers.endFrame();
        if (tip != null && !tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
    }

    private void easeTint() {
        int target = leanRgb >= 0 ? leanRgb : look.auraRgb() & 0xFFFFFF;
        int[] t = splitRgb(target);
        float k = UiMath.approachFactor(0.16f, UiTheme.motion() ? dt : 1000f);
        for (int i = 0; i < 3; i++) tint[i] += (t[i] / 255f - tint[i]) * k;
    }

    private int tintRgb() {
        return (Math.round(tint[0] * 255) << 16) | (Math.round(tint[1] * 255) << 8) | Math.round(tint[2] * 255);
    }

    private void drawBackdrop(GuiGraphics g) {
        g.fillGradient(0, 0, width, height, 0xF40B1226, 0xF8050812);
        // a few motes drifting upward: cheap, deterministic, and gone when motion is off
        if (UiTheme.motion()) {
            for (int i = 0; i < 26; i++) {
                float fx = ((i * 7919) % 1000) / 1000f, fy = ((i * 104729) % 1000) / 1000f, sp = 0.12f + ((i * 31) % 10) * 0.03f;
                float y = height - ((age * sp * 3f + fy * height) % height);
                float a = 0.10f + 0.10f * (float) Math.sin(age * 0.05f + i);
                int x = (int) (fx * width + Math.sin(age * 0.02f + i) * 6f);
                g.fill(x, (int) y, x + 2, (int) y + 2, ForgeUi.argb(i % 3 == 0 ? 0x9A7CFF : 0x5CD8FF, a));
            }
        }
        // the top and bottom are a little darker so the bars read
        g.fillGradient(0, 0, width, (lay.top().h() + 6) * ui, ForgeUi.argb(0x000000, 0.35f), 0x00000000);
        g.fillGradient(0, height - (lay.bottom().h() + 6) * ui, width, height, 0x00000000, ForgeUi.argb(0x000000, 0.35f));
    }

    // ---------------------------------------------------------------- the top bar
    private void drawTopBar(GuiGraphics g, int mx, int my) {
        Rect t = lay.top();
        g.fillGradient(t.x(), t.y(), t.right(), t.bottom(), ForgeUi.argb(0x0E1830, 0.95f), ForgeUi.argb(0x070C1A, 0.95f));
        g.fill(t.x(), t.bottom() - 1, t.right(), t.bottom(), ForgeUi.argb(0x5CD8FF, 0.35f));
        if (canvasW >= 380) {
            ForgeUi.textShadow(g, font, ForgeText.s("nusmp.avatar.forge.title", "SOUL FORGE"), lay.margin() + 4, t.cy() - font.lineHeight / 2, 0xE8C468);
            String cores = ForgeText.s("nusmp.avatar.forge.cores", "Genesis Cores: %s", reserves().creative() ? "inf" : String.valueOf(reserves().cores()));
            ForgeUi.text(g, font, cores, canvasW - lay.margin() - 4 - font.width(cores), t.cy() - font.lineHeight / 2, 0x9A7CFF);
        }
        hoveredCategory = -1;
        int current = slide.wanted();
        for (int i = 0; i < ForgeState.PAGES; i++) {
            Rect c = lay.categories()[i];
            boolean sel = current == i, over = c.contains(mx, my);
            if (over) hoveredCategory = i;
            float hv = hover("cat" + i, over);
            if (sel) ForgeUi.glow(g, c.cx(), c.cy(), c.w() * 0.9f, 0xE8C468, 0.30f + 0.08f * (float) Math.sin(age * 0.1f));
            else if (hv > 0.02f) ForgeUi.glow(g, c.cx(), c.cy(), c.w() * 0.8f, pages[i].accent(), 0.2f * hv);
            ForgeUi.disc(g, c, 0x05080F, 0.95f);
            int pad = Math.max(3, Math.round(c.w() * 0.2f));
            ForgeUi.sprite(g, ForgeUi.category(i), c.x() + pad, c.y() + pad, c.w() - 2 * pad, c.h() - 2 * pad, 32, sel ? 0xFFFFFF : 0xB8C8E0, sel ? 1f : 0.8f + 0.2f * hv);
            ForgeUi.ring(g, c, sel ? 0xE8C468 : pages[i].accent(), sel ? 1f : 0.55f + 0.4f * hv);
            int dot = Math.max(5, c.w() / 4);
            if (ForgeState.pageNeedsAttention(i, problems)) ForgeUi.disc(g, new Rect(c.right() - dot, c.y() - 1, dot, dot), 0xFF6A6A, 1f);
            else if (state.pageDone(i)) ForgeUi.disc(g, new Rect(c.right() - dot, c.y() - 1, dot, dot), 0x7CF0B0, 1f);
            if (sel && t.h() >= c.h() + 18) {
                String s = ForgeUi.fit(font, pages[i].title(), 150);
                ForgeUi.centered(g, font, s, c.cx(), c.bottom() + 3, 0xE8C468);
            }
        }
        if (hoveredCategory >= 0) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(pages[hoveredCategory].title()).withStyle(net.minecraft.ChatFormatting.GOLD));
            for (ForgeState.Problem p : problems) if (p.page() == hoveredCategory) lines.add(Component.literal(p.text()).withStyle(p.soft() ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.RED));
            if (lines.size() == 1) lines.add(categoryHint(hoveredCategory).withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip(lines);
        }
    }

    private static net.minecraft.network.chat.MutableComponent categoryHint(int page) {
        return switch (page) {
            case 0 -> ForgeText.t("nusmp.avatar.forge.hint.identity", "Roll your Avatar's six scores and give it a name.");
            case 1 -> ForgeText.t("nusmp.avatar.forge.hint.appearance", "Choose its race and shape how it looks.");
            case 2 -> ForgeText.t("nusmp.avatar.forge.hint.traits", "Choose its personality and hear how it speaks.");
            case 3 -> ForgeText.t("nusmp.avatar.forge.hint.skills", "Bind one Unique and one Ultimate skill, at a price.");
            default -> ForgeText.t("nusmp.avatar.forge.hint.aptitudes", "Pick a magic aptitude by hand, or gamble for one for free.");
        };
    }

    // ---------------------------------------------------------------- the module
    private void drawModule(GuiGraphics g, int mx, int my, float partial) {
        Rect l = lay.left();
        ForgePage p = pages[slide.shown()];
        int off = offX();
        boolean settled = slide.settled();
        g.pose().pushPose();
        g.pose().translate(off, 0f, 0f);
        UiPanel.panel(g, l.x(), l.y(), l.w(), l.h(), p.accent(), 0.8f, 1f);
        UiPanel.heading(g, font, ForgeUi.fit(font, p.title(), l.w() - 12), l.x() + 6, l.y() + 4, l.w() - 12, p.accent());
        p.render(g, settled ? mx : -9999, settled ? my : -9999, partial);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- the stage
    private void drawStage(GuiGraphics g, int mx, int my, float partial) {
        Rect st = lay.stage();
        g.fill(st.x(), st.y(), st.right(), st.bottom(), ForgeUi.argb(0x050914, 0.5f));
        g.renderOutline(st.x(), st.y(), st.w(), st.h(), ForgeUi.argb(0x5CD8FF, 0.22f));
        int cx = st.cx();
        int plateH = font.lineHeight * 2 + 6;
        int feet = st.bottom() - plateH - 6;
        float avatarPx = Math.max(24f, (st.h() - plateH - 18) * (bubbleShown() ? (st.h() < 130 ? 0.5f : 0.62f) : 0.86f));
        float ref = Math.max(1f, look.scale());
        float pxPerBlock = avatarPx / (1.95f * ref);
        float bodyH = pxPerBlock * look.scale() * 1.8f;
        float torso = feet - bodyH * 0.55f;
        float radius = Math.max(26f, Math.min(st.w() * 0.48f, Math.max(avatarPx * 0.62f, bodyH * 0.7f)));
        int tintRgb = tintRgb();
        float time = now();
        float pulseK = pulseAge < 18f ? pulseAge / 18f : 1f;
        float pop = pulseAge < 12f ? 1f + 0.045f * (float) Math.sin(Math.PI * pulseAge / 12f) : 1f;

        g.enableScissor(st.x() * ui, st.y() * ui, st.right() * ui, st.bottom() * ui);
        MagicCircle.draw(g, cx, torso, radius, time, 0xFF000000 | tintRgb, 0.6f, MagicCircle.CALM, 1f);
        // the glow on the floor, flattened into an ellipse
        g.pose().pushPose();
        g.pose().translate(cx, feet, 0f);
        g.pose().scale(1f, 0.28f, 1f);
        ForgeUi.glow(g, 0f, 0f, radius * 0.85f, tintRgb, 0.28f);
        g.pose().popPose();
        // the ring that opens when a choice is made (behind the Avatar)
        if (pulseK < 1f) {
            float rr = radius * (0.5f + 0.9f * ForgeMotion.easeOutCubic(pulseK));
            int ri = Math.round(rr);
            ForgeUi.sprite(g, ForgeUi.RING, Math.round(cx) - ri, Math.round(torso) - ri, ri * 2, ri * 2, 64, pulseRgb, 0.85f * (1f - pulseK));
        }
        List<GiftIcon> gifts = gifts();
        drawGifts(g, gifts, cx, feet, radius, true);
        float spin = (UiTheme.motion() ? age * 1.4f : 20f) + userSpin;
        AvatarPreview.draw(g, cx, feet, pxPerBlock * pop, look, state.race(), spin, age);
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 200f);
        drawGifts(g, gifts, cx, feet, radius, false);
        // the dice of the Identity page
        if (slide.shown() == ForgeState.PAGE_IDENTITY) drawDice(g, st, partial, 1f + Math.max(-1f, slide.offset()));
        // the speech bubble of the Traits page
        if (bubbleShown()) drawBubble(g, st, 1f + Math.max(-1f, slide.offset()));
        g.pose().popPose();
        g.disableScissor();
        drawPlate(g, st, plateH);
    }

    private record GiftIcon(net.minecraft.resources.ResourceLocation tex, String name, int tint, int frame) {}

    private List<GiftIcon> gifts() {
        List<GiftIcon> out = new ArrayList<>();
        for (String id : new String[]{state.unique(), state.ultimate()}) {
            if (id.isEmpty()) continue;
            ForgeSkillRow r = SoulForgeClientData.skillRow(id);
            if (r != null) out.add(new GiftIcon(ForgeIcons.skillTexture(r), r.name(), r.tint(), r.ultimate() ? 0xE8C468 : 0x5CD8FF));
        }
        List<String> aptIds = new ArrayList<>(state.picks());
        if (state.useRoll() && !state.rolledId().isEmpty()) aptIds.add(state.rolledId());
        for (String id : aptIds) {
            ForgeAptitudeRow r = SoulForgeClientData.aptitudeRow(id);
            if (r != null) out.add(new GiftIcon(ForgeIcons.aptitudeTexture(r), r.name(), r.tint(), 0xFFFFFF));
        }
        return out;
    }

    /** The gifts the Avatar has been given circle its feet; the half behind it is drawn before the Avatar, the half in front after. */
    private void drawGifts(GuiGraphics g, List<GiftIcon> gifts, int cx, int feet, float radius, boolean back) {
        int n = gifts.size();
        if (n == 0) return;
        int size = Math.max(14, Math.min(24, Math.round(radius * 0.28f)));
        for (int i = 0; i < n; i++) {
            double a = (UiTheme.motion() ? age * 0.035 : 0.0) + i * (Math.PI * 2.0 / n);
            boolean isBack = Math.sin(a) < 0;
            if (isBack != back) continue;
            int x = (int) Math.round(cx + Math.cos(a) * radius * 0.92) - size / 2;
            int y = (int) Math.round(feet - radius * 0.1 + Math.sin(a) * radius * 0.2) - size / 2;
            GiftIcon gi = gifts.get(i);
            ForgeUi.medallion(g, font, new Rect(x, y, size, size), gi.tex(), gi.name(), gi.tint(), gi.frame(), 0f, true, false);
        }
    }

    private void drawPlate(GuiGraphics g, Rect st, int plateH) {
        String name = state.name().isBlank() ? ForgeText.s("nusmp.avatar.forge.unnamed", "Your Avatar") : state.name().strip();
        RaceEntry re = raceEntry();
        String race = re == null ? ForgeText.s("nusmp.avatar.forge.norace", "no race yet") : Component.translatableWithFallback(re.name(), re.name()).getString();
        String trait = state.primary() == null ? "" : state.primary().displayName + (state.secondary() == null ? "" : " / " + state.secondary().displayName);
        int y = st.bottom() - plateH + 1;
        g.fillGradient(st.x() + 1, y - 3, st.right() - 1, st.bottom() - 1, 0x00000000, ForgeUi.argb(0x000000, 0.45f));
        ForgeUi.centered(g, font, ForgeUi.fit(font, name, st.w() - 8), st.cx(), y, 0xE8C468);
        ForgeUi.centered(g, font, ForgeUi.fit(font, trait.isEmpty() ? race : race + "  -  " + trait, st.w() - 8), st.cx(), y + font.lineHeight + 1, 0x8FA3BF);
    }

    private void drawDice(GuiGraphics g, Rect st, float partial, float alpha) {
        float r = Math.max(14f, Math.min(34f, st.w() * 0.11f));
        if (st.w() < r * 3.2f || alpha <= 0.02f) return;
        float dx = st.right() - r * 1.7f, dy = st.y() + r * 1.9f;
        S2C_RollPayload roll = AvatarClientData.roll;
        int colour = roll != null && roll.fiveDice() ? 0xE8C468 : 0x9A7CFF;
        ForgeUi.glow(g, dx, dy, r * 1.8f, colour, 0.22f * alpha);
        DiceSpin.View v = dice.view(partial, dx, dy, r * dice.pop(partial));
        ForgeDice3D.draw(g, font, v, colour, alpha, 0.95f * alpha, r);
        if (roll != null && !dice.rolling()) {
            int best = 0;
            for (int s : roll.stats()) best = Math.max(best, s);
            ForgeUi.centered(g, font, ForgeText.s("nusmp.avatar.forge.dice.best", "Best score %s", best), Math.round(dx), Math.round(dy + r * 1.35f), 0x8FA3BF);
        }
    }

    private boolean bubbleShown() { return slide.shown() == ForgeState.PAGE_TRAITS && !barkText.isEmpty(); }

    private void drawBubble(GuiGraphics g, Rect st, float alpha) {
        if (alpha <= 0.05f) return;
        int bw = Math.min(st.w() - 16, 190);
        int shown = UiMath.typed(UiTheme.motion() ? barkAge : 999f, 1.6f, barkText.length());
        List<String> lines = ForgeUi.wrapLines(font, barkText.substring(0, shown), bw - 10, 5);
        List<String> full = ForgeUi.wrapLines(font, barkText, bw - 10, 5);
        int lh = font.lineHeight + 1;
        int bh = (full.size() + 1) * lh + 8;
        int bx = st.cx() - bw / 2, by = st.y() + 6;
        g.fill(bx, by, bx + bw, by + bh, ForgeUi.argb(0x0A1226, 0.94f * alpha));
        g.renderOutline(bx, by, bw, bh, ForgeUi.argb(0xE8C468, 0.8f * alpha));
        g.fill(st.cx() - 4, by + bh, st.cx() + 4, by + bh + 2, ForgeUi.argb(0xE8C468, 0.8f * alpha));
        g.fill(st.cx() - 2, by + bh + 2, st.cx() + 2, by + bh + 5, ForgeUi.argb(0xE8C468, 0.8f * alpha));
        ForgeUi.text(g, font, ForgeUi.fit(font, barkTag(barkEvent), bw - 10), bx + 5, by + 4, 0x8FA3BF);
        int y = by + 4 + lh;
        for (String line : lines) { ForgeUi.text(g, font, line, bx + 5, y, 0xE6F2FF); y += lh; }
    }

    private static String barkTag(String event) {
        return switch (event == null ? "" : event) {
            case "COMBAT_START" -> ForgeText.s("nusmp.avatar.forge.bark.combat_start", "Entering a fight");
            case "SKILL_CAST" -> ForgeText.s("nusmp.avatar.forge.bark.skill_cast", "Casting a skill");
            case "CREATOR_LOW_HP" -> ForgeText.s("nusmp.avatar.forge.bark.creator_low_hp", "Seeing you hurt");
            case "IDLE" -> ForgeText.s("nusmp.avatar.forge.bark.idle", "Between fights");
            default -> ForgeText.s("nusmp.avatar.forge.bark.other", "It says");
        };
    }

    // ---------------------------------------------------------------- the cost panel
    private static String n(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    private static String pct(double fraction) {
        double p = fraction * 100.0;
        return p == Math.rint(p) ? String.valueOf((long) p) + "%" : String.format(Locale.ROOT, "%.1f%%", p);
    }

    /**
     * One entry of the cost panel: the label on the left and the value on the right when both fit on one line, else the label on one line and the value
     * (right-aligned) on the next. Returns the y below it, or the same y when there is no room left (nothing is drawn then).
     */
    private int entry(GuiGraphics g, String label, String value, int x, int y, int w, int labelRgb, int valueRgb, int bottom) {
        int lh = font.lineHeight + 3;
        boolean one = font.width(label) + 8 + font.width(value) <= w;
        if (y + (one ? font.lineHeight : lh + font.lineHeight) > bottom) return y;
        ForgeUi.text(g, font, ForgeUi.fit(font, label, w), x, y, labelRgb);
        if (one) {
            ForgeUi.text(g, font, value, x + w - font.width(value), y, valueRgb);
            return y + lh;
        }
        String v = ForgeUi.fit(font, value, w);
        ForgeUi.text(g, font, v, x + w - font.width(v), y + lh - 1, valueRgb);
        return y + 2 * lh - 1;
    }

    private void drawLedger(GuiGraphics g, int mx, int my) {
        Rect r = lay.ledger();
        if (r.isEmpty() || ledger == null) return;
        boolean beside = lay.ledgerBeside();
        if (beside) UiPanel.panel(g, r.x(), r.y(), r.w(), r.h(), 0xE8C468, 0.7f, 1f);
        else { g.fill(r.x(), r.y(), r.right(), r.bottom(), ForgeUi.argb(0x050914, 0.7f)); g.renderOutline(r.x(), r.y(), r.w(), r.h(), ForgeUi.argb(0xE8C468, 0.35f)); }
        int x = r.x() + 6, w = r.w() - 12, y = r.y() + (beside ? 5 : 3), bottom = r.bottom() - 3;
        if (beside) y = UiPanel.heading(g, font, ForgeText.s("nusmp.avatar.forge.price", "The Price"), x, y, w, 0xE8C468);
        ForgeState.Ledger l = ledger;
        int dim = 0x8FA3BF, bright = 0xE6F2FF;
        y = entry(g, ForgeText.s("nusmp.avatar.forge.price.tax", "Permanent tax"), "-" + pct(l.quote().permanentTax()), x, y, w, bright,
                l.taxOver() ? 0xFF6A6A : l.quote().taxed() ? 0xE8C468 : 0x7CF0B0, bottom);
        if (beside && y + 14 <= bottom) {
            float cap = (float) l.taxCap();
            ForgeUi.gauge(g, new Rect(x, y + 1, w, 4), cap <= 0 ? 0f : (float) Math.min(1.0, l.totalTax() / cap), 1f, l.taxOver() ? 0xFF6A6A : 0xE8C468);
            y += 9;
            if (l.quote().taxed() && y + font.lineHeight <= bottom) {
                String parts = ForgeText.s("nusmp.avatar.forge.price.parts", "aptitudes %s, skills %s, awakening %s", pct(l.quote().aptitudeTax()), pct(l.quote().skillTax()), pct(l.quote().awakeningTax()));
                y = ForgeUi.wrapped(g, font, parts, x, y, w, dim, 2) + 1;
            }
            if (y + font.lineHeight <= bottom) y = ForgeUi.wrapped(g, font, ForgeText.s("nusmp.avatar.forge.price.cap", "Limit %s, %s used already", pct(l.taxCap()), pct(l.existingTax())), x, y, w, dim, 2) + 2;
        }
        y = entry(g, ForgeText.s("nusmp.avatar.forge.price.magicule", "Magicule now"), n(l.magiculeNeeded()) + " / " + n(l.magiculeHeld()), x, y, w, bright, l.magiculeShort() ? 0xFF6A6A : bright, bottom);
        y = entry(g, ForgeText.s("nusmp.avatar.forge.price.cores", "Genesis Cores"), l.coresNeeded() + " of " + (l.creative() ? "inf" : String.valueOf(l.coresHeld())), x, y, w, bright, l.coresShort() ? 0xFF6A6A : bright, bottom);
        boolean sure = l.awakeningChance() >= 0.999;
        y = entry(g, ForgeText.s("nusmp.avatar.forge.price.awaken", "Awakening"), sure ? ForgeText.s("nusmp.avatar.forge.price.awaken_sure", "guaranteed") : ForgeText.s("nusmp.avatar.forge.price.awaken_free", "%s free", pct(l.awakeningChance())),
                x, y, w, bright, sure ? 0xE8C468 : 0x9A7CFF, bottom);
        if (beside) {
            for (ForgeState.Problem p : problems) {
                if (y + font.lineHeight > bottom) break;
                y = ForgeUi.wrapped(g, font, p.text(), x, y + 2, w, p.soft() ? 0xE8C468 : 0xFF6A6A, 3) + 1;
            }
        }
        if (r.contains(mx, my)) tooltip(List.of(ForgeText.t("nusmp.avatar.forge.price.tip1", "The tax is a share of your maximum magicule, taken for good while this Avatar lives.").withStyle(net.minecraft.ChatFormatting.GRAY),
                ForgeText.t("nusmp.avatar.forge.price.tip2", "The magicule is also paid once, now, out of what you hold. The server checks all of this again.").withStyle(net.minecraft.ChatFormatting.GRAY)));
    }

    // ---------------------------------------------------------------- the bottom bar
    private void drawBottomBar(GuiGraphics g, int mx, int my) {
        Rect b = lay.bottom();
        g.fillGradient(b.x(), b.y(), b.right(), b.bottom(), ForgeUi.argb(0x070C1A, 0.95f), ForgeUi.argb(0x0E1830, 0.95f));
        g.fill(b.x(), b.y(), b.right(), b.y() + 1, ForgeUi.argb(0x5CD8FF, 0.35f));
        int page = slide.wanted();
        Rect back = lay.back(), next = lay.next(), fin = lay.finalizeButton();
        boolean canBack = page > 0 && !state.busy(), canNext = page < ForgeState.PAGES - 1 && !state.busy();
        ForgeUi.button(g, font, back, Component.translatable("nusmp.avatar.gui.back").getString(), canBack, hover("back", back.contains(mx, my) && canBack), 0x5CD8FF);
        ForgeUi.button(g, font, next, Component.translatable("nusmp.avatar.gui.next").getString(), canNext, hover("next", next.contains(mx, my) && canNext), 0x5CD8FF);
        boolean can = state.canFinalize(problems);
        boolean over = fin.contains(mx, my);
        float hv = hover("fin", over && can);
        if (can) ForgeUi.glow(g, fin.cx(), fin.cy(), fin.w() * 0.55f, 0x7CF0B0, 0.10f + 0.06f * (float) Math.sin(age * 0.12f) + 0.1f * hv);
        String label = state.busy() ? ForgeText.s("nusmp.avatar.forge.finalizing", "Binding...") : ForgeText.s("nusmp.avatar.forge.finalize", "Finalize");
        ForgeUi.button(g, font, fin, label, can, hv, 0x7CF0B0);
        // the one line between the buttons
        Rect m = lay.message();
        if (m.w() > 30) {
            Component text = null;
            int rgb = 0xFF6A6A;
            if (sayTicks > 0 && !sayText.getString().isEmpty()) { text = sayText; rgb = sayGood ? 0x7CF0B0 : 0xFF6A6A; }
            else {
                for (ForgeState.Problem p : problems) if (!p.soft()) { text = Component.literal(p.text()); break; }
                if (text == null) for (ForgeState.Problem p : problems) { text = Component.literal(p.text()); rgb = 0xE8C468; break; }
                if (text == null) { text = ForgeText.t("nusmp.avatar.forge.ready", "Ready. Press Finalize."); rgb = 0x7CF0B0; }
            }
            String s = ForgeUi.fit(font, text.getString(), m.w());
            ForgeUi.text(g, font, s, m.x(), m.cy() - font.lineHeight / 2, rgb);
        }
        if (over && !can) {
            List<Component> lines = new ArrayList<>();
            lines.add(ForgeText.t("nusmp.avatar.forge.finalize.blocked", "Not yet:").withStyle(net.minecraft.ChatFormatting.RED));
            for (ForgeState.Problem p : problems) if (!p.soft()) lines.add(Component.literal("- " + p.text()).withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip(lines);
        }
    }

    // ================================================================ finalizing
    private void finalizeNow() {
        if (!state.canFinalize(problems)) {
            for (ForgeState.Problem p : problems) if (!p.soft()) { say(Component.literal(p.text()), false); if (p.page() >= 0) requestPage(p.page()); return; }
            return;
        }
        RaceEntry re = raceEntry();
        AppearanceData sane = DynamicAppearanceEngine.sanitize(look, re == null ? 1.0f : re.scale());
        AvatarPersonality sec = state.secondary();
        ForgeChoices ch = state.choices();
        state.setBusy(true);
        busyTicks = 0;
        say(Component.empty(), true);
        sayTicks = 0;
        click();
        PacketDistributor.sendToServer(new C2S_FinalizeSoulForgePayload(state.race(), state.primary().name(), sec == null ? "" : sec.name(), sane.toTag(), state.name().strip(),
                AvatarClientData.roll == null ? 0L : AvatarClientData.roll.token(), ch.unique(), ch.ultimate(), ch.aptitudePicks(), ch.rollToken(), ch.paidAwakening()));
    }

    // ================================================================ input
    private boolean typing() { return nameBox != null && nameBox.isFocused() && slide.shown() == ForgeState.PAGE_IDENTITY; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
            nameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;                       // typing an 'e' must not close the screen as the inventory key would
        }
        if (slide.settled() && pages[slide.shown()].keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_LEFT) { requestPage(slide.wanted() - 1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { requestPage(slide.wanted() + 1); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (typing()) return nameBox.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        double mx = rawX / ui, my = rawY / ui;
        boolean onName = slide.shown() == ForgeState.PAGE_IDENTITY && slide.settled() && lay.left().contains(mx, my);
        if (!onName && nameBox != null) nameBox.setFocused(false);
        if (button == 0) {
            for (int i = 0; i < ForgeState.PAGES; i++) if (lay.categories()[i].contains(mx, my)) { requestPage(i); return true; }
            if (lay.back().contains(mx, my)) { if (slide.wanted() > 0) requestPage(slide.wanted() - 1); return true; }
            if (lay.next().contains(mx, my)) { if (slide.wanted() < ForgeState.PAGES - 1) requestPage(slide.wanted() + 1); return true; }
            if (lay.finalizeButton().contains(mx, my)) { finalizeNow(); return true; }
        }
        if (slide.settled() && lay.left().contains(mx, my)) {
            pages[slide.shown()].mouseClicked(mx, my, button);
            return true;
        }
        if (button == 0 && lay.stage().contains(mx, my)) { draggingStage = true; return true; }
        return lay.top().contains(mx, my) || lay.bottom().contains(mx, my) || lay.ledger().contains(mx, my);
    }

    @Override
    public boolean mouseDragged(double rawX, double rawY, int button, double dx, double dy) {
        if (draggingStage) { userSpin += (float) dx * 0.9f; return true; }
        if (slide.settled() && pages[slide.shown()].mouseDragged(rawX / ui, rawY / ui, button)) return true;
        return false;
    }

    @Override
    public boolean mouseReleased(double rawX, double rawY, int button) {
        draggingStage = false;
        pages[slide.shown()].mouseReleased(rawX / ui, rawY / ui, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double sx, double sy) {
        double mx = rawX / ui, my = rawY / ui;
        if (slide.settled() && lay.left().contains(mx, my) && pages[slide.shown()].mouseScrolled(mx, my, sy)) return true;
        return false;
    }

    @Override
    public void onClose() {
        for (ForgePage p : pages) p.hidden();
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
