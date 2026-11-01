package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.devil.DevilNet;
import com.newuniverse.nusmp.devil.DevilRules;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.Sacrifice;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the client half of the Devil Contract system (3.48.0). It only asks and draws; every number is the server's.
 * <ul>
 * <li>The key (default {@code N}, rebindable, category "Multiverse") sends one empty {@link DevilNet.Toggle} ask, at most one every 4 ticks; every queued click is drained.</li>
 * <li>{@link DevilNet.Sync} (the owner's state) feeds the HUD panel (tier, assimilation bar, the next milestone, the union and cooldown timers, which count down here between packets, and the sacrifices)
 * and the eye shade; {@link DevilNet.Visual} (what a devil looks like) fills a small map entity id to {@link Vis} that {@link WegLayer} reads. The map is emptied on logout, capped at {@link #MAX_TRACKED}
 * entries, and swept every five seconds for entities that are gone.</li>
 * <li>A lost eye darkens that side of the screen (16 vertical strips, drawn under the other HUD layers) and narrows the field of view; both scale with the client setting {@code eyeOverlayStrength}.</li>
 * </ul>
 * Cost: one panel of about 30 fills and 8 strings per frame while the player holds a contract (the text lines are rebuilt once a second), up to 32 strips for the eye shade, one map lookup per player drawn.
 * Not seen in a running game by its author.
 */
public final class DevilClient {
    private DevilClient() {}

    public static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping ASSIMILATE = new KeyMapping("key.nusmp.devil_assimilate", InputConstants.Type.KEYSYM, InputConstants.KEY_N, CATEGORY);

    /** Other players the layer draws in one frame (the owner's own body is always drawn). */
    static final int MAX_PER_FRAME = 16;
    static final int MAX_TRACKED = 256;
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath("nusmp", "devil_hud"), EYES = ResourceLocation.fromNamespaceAndPath("nusmp", "devil_eyes");
    private static final int W = 164;

    // ---------------------------------------------------------------- what other bodies look like
    /** The look of one player, as the server last said it, plus the smoothed values the layer draws. Render thread only. */
    static final class Vis {
        int tier, level;
        boolean union;
        float shown, spread = 0.4f, lastT = -1f;

        /** Moves the drawn level toward the real one (12 percent a second) and the wings toward open or folded. */
        void step(float now, boolean air) {
            float target = union || level >= DevilRules.UNION_AT || air ? 1f : 0.4f;
            float dt = lastT < 0f || now < lastT || now - lastT > 40f ? -1f : now - lastT;               // ticks since the last frame, -1 = jump
            lastT = now;
            if (dt < 0f) {
                shown = level;
                spread = target;
                return;
            }
            float by = 12f * dt / 20f;
            shown += Mth.clamp(level - shown, -by, by);
            spread += Mth.clamp(target - spread, -0.1f * dt, 0.1f * dt);
        }
    }

    private static final Map<Integer, Vis> VIS = new HashMap<>();
    private static int drawn;

    static Vis vis(int entityId) { return VIS.isEmpty() ? null : VIS.get(entityId); }

    /** True while this frame may still draw another player's body. */
    static boolean takeSlot() { return drawn++ < MAX_PER_FRAME; }

    // ---------------------------------------------------------------- the owner's state
    private static volatile DevilNet.Sync own;
    private static long syncAt;
    private static long lastAsk = -100;
    private static volatile Lines lines;

    /** Called once by the client setup with the mod bus. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DevilClientConfig.SPEC, "nusmp-devil-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ASSIMILATE));
        modBus.addListener((RegisterGuiLayersEvent e) -> {
            e.registerBelowAll(EYES, DevilClient::renderEyes);
            e.registerAboveAll(HUD, DevilClient::renderHud);
        });
        modBus.addListener(DevilModels::register);
        modBus.addListener(WegLayer::add);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov e) -> fov(e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> { if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) drawn = 0; });
        DevilNet.clientSync = DevilClient::onSync;
        DevilNet.clientVisual = DevilClient::onVisual;
    }

    private static void clear() {
        own = null;
        lines = null;
        VIS.clear();
        drawn = 0;
    }

    private static void onSync(DevilNet.Sync s) {
        Minecraft mc = Minecraft.getInstance();
        syncAt = mc.level == null ? 0 : mc.level.getGameTime();
        own = s.tier() == 0 && s.sacMask() == 0 ? null : s;
        lines = null;
    }

    private static void onVisual(DevilNet.Visual v) {
        if (v.level() <= 0 && !v.union()) {
            VIS.remove(v.entity());
            return;
        }
        Vis x = VIS.get(v.entity());
        if (x == null) {
            if (VIS.size() >= MAX_TRACKED) return;
            x = new Vis();
            x.shown = v.level();
            VIS.put(v.entity(), x);
        }
        x.tier = v.tier();
        x.level = v.level();
        x.union = v.union();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        DevilNet.Sync s = own;
        boolean can = mc.player != null && mc.getConnection() != null && mc.screen == null && s != null && s.tier() > 0;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        while (ASSIMILATE.consumeClick()) {                                              // every queued click is drained, none is kept for later
            if (!can || !DevilRules.askAllowed(lastAsk, now)) continue;
            lastAsk = now;
            PacketDistributor.sendToServer(new DevilNet.Toggle());
        }
        if (mc.level != null && now % 100 == 0 && !VIS.isEmpty()) VIS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    // ---------------------------------------------------------------- a lost eye
    private static void fov(ViewportEvent.ComputeFov e) {
        DevilNet.Sync s = own;
        if (s == null) return;
        int eyes = Integer.bitCount(s.sacMask() & Sacrifice.EYES);
        if (eyes > 0) e.setFOV(e.getFOV() * DevilRules.fovFactor(eyes, DevilClientConfig.eyeStrength()));
    }

    private static void renderEyes(GuiGraphics g, DeltaTracker delta) {
        DevilNet.Sync s = own;
        if (s == null || (s.sacMask() & Sacrifice.EYES) == 0) return;
        float st = (float) DevilClientConfig.eyeStrength();
        if (st <= 0.01f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        if ((s.sacMask() & Sacrifice.LOST_EYE_LEFT.bit()) != 0) strips(g, w, h, true, st);
        if ((s.sacMask() & Sacrifice.LOST_EYE_RIGHT.bit()) != 0) strips(g, w, h, false, st);
    }

    /** A dark band down one edge of the screen, black at the edge and clear a fifth of the way in: 16 strips, so no texture and no allocation. */
    private static void strips(GuiGraphics g, int w, int h, boolean left, float strength) {
        int n = 16, sw = Math.max(1, w / 5 / n);
        for (int i = 0; i < n; i++) {
            float f = 1f - i / (float) n;
            int a = (int) (235f * f * f * strength);
            if (a <= 0) continue;
            int x0 = left ? i * sw : w - (i + 1) * sw;
            g.fill(x0, 0, x0 + sw, h, a << 24);
        }
    }

    // ---------------------------------------------------------------- the HUD
    private record Lines(long second, String title, String attr, String level, String stage, String union, int unionColour, String hint, String warn, String sacrifices) {}

    static String t(String key, String fallback, Object... args) { return Component.translatableWithFallback("nusmp.devil.hud." + key, fallback, args).getString(); }

    private static int left(int secAtSync, long sinceTicks) { return (int) Math.max(0, (secAtSync * 20L - sinceTicks + 19) / 20); }

    private static String clock(int seconds) { return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60); }

    private static int accent(int tier) {
        return switch (tier) { case 1 -> UiTheme.VIOLET; case 2 -> UiTheme.RED; case 3 -> UiTheme.GOLD; default -> 0xFFFFF2B8; };
    }

    private static Lines build(DevilNet.Sync s, long nowTicks) {
        long since = Math.max(0, nowTicks - syncAt);
        int unionLeft = left(s.unionSec(), since), coolLeft = left(s.coolSec(), since);
        DevilTier tier = DevilTier.byOrdinal(s.tier());
        String name = Component.translatableWithFallback("nusmp.devil.tier." + tier.key(), tier.key().substring(0, 1).toUpperCase() + tier.key().substring(1) + " Devil").getString();
        String attr = Component.translatableWithFallback("nusmp.devil.attr." + com.newuniverse.nusmp.devil.DevilAttribute.byOrdinal(s.attribute()).key(),
                com.newuniverse.nusmp.devil.DevilAttribute.byOrdinal(s.attribute()).key()).getString();
        boolean on = (s.flags() & DevilNet.F_ASSIMILATING) != 0;
        String level = t("level", "Assimilation %s%%", s.level()) + (on ? " " + t("rising", "(rising)") : s.level() > 0 ? " " + t("fading", "(fading)") : "");
        String stage = switch (DevilRules.stage(s.level())) {
            case 0 -> t("stage0", "Dormant. Horns at %s%%", (int) DevilRules.HORNS_AT);
            case 1 -> t("stage1", "Horns. Wings and flight at %s%%", (int) DevilRules.WINGS_AT);
            case 2 -> t("stage2", "Wings and flight. Union at %s%%", (int) DevilRules.UNION_AT);
            default -> t("stage3", "Devil Union");
        };
        String union;
        int unionColour;
        if ((s.flags() & DevilNet.F_UNION) != 0 && unionLeft > 0) { union = t("union_on", "Devil Union: %s left", clock(unionLeft)); unionColour = UiTheme.RED; }
        else if (coolLeft > 0) { union = t("union_cool", "Union sealed: %s", clock(coolLeft)); unionColour = UiTheme.TEXT_DIM; }
        else { union = t("union_ready", "The union opens at %s%%", (int) DevilRules.UNION_AT); unionColour = UiTheme.GOLD; }
        String hint = t("hint", "[%s] let the devil in / push it back", ASSIMILATE.getTranslatedKeyMessage().getString());
        String warn = (s.flags() & DevilNet.F_BLOCKED) != 0 ? t("blocked", "The Anti-Magic union is on. Only one devil at a time.") : "";
        List<String> sac = new ArrayList<>(3);
        if ((s.sacMask() & Sacrifice.HALF_HEALTH.bit()) != 0) sac.add(t("sac_half", "half health"));
        if ((s.sacMask() & Sacrifice.LOST_EYE_LEFT.bit()) != 0) sac.add(t("sac_eye_left", "left eye"));
        if ((s.sacMask() & Sacrifice.LOST_EYE_RIGHT.bit()) != 0) sac.add(t("sac_eye_right", "right eye"));
        String sacrifices = sac.isEmpty() ? "" : t("sacrifices", "Paid: %s", String.join(", ", sac));
        return new Lines(nowTicks / 20, name, attr, level, stage, union, unionColour, hint, warn, sacrifices);
    }

    private static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        DevilNet.Sync s = own;
        if (s == null || s.tier() == 0 || mc.options.hideGui || mc.player == null || mc.level == null || !DevilClientConfig.hud()) return;
        long now = mc.level.getGameTime();
        Lines l = lines;
        if (l == null || now / 20 != l.second() || now < syncAt) lines = l = build(s, now);                  // the text is rebuilt once a second, not per frame
        Font font = mc.font;
        int accent = accent(s.tier());
        int h = 8 + 11 + 11 + 8 + 11 + 11 + 11 + (l.warn().isEmpty() ? 0 : 10) + (l.sacrifices().isEmpty() ? 0 : 10);
        int x = Math.max(4, g.guiWidth() - W - 6), y = Math.max(4, (g.guiHeight() - h) / 2);
        GuiMove.report("devil_hud", x, y, W, h);                                                         // GUI Move / GUI Size: where the panel is drawn
        float time = UiTheme.now();
        boolean union = (s.flags() & DevilNet.F_UNION) != 0;
        UiPanel.panel(g, x, y, W, h, accent, union ? 0.55f + 0.45f * UiMath.pulse(time, 8f, 0f) : 0.25f, 0.92f);

        int cx = x + 7, cy = y + 6, iw = W - 14;
        g.drawString(font, font.plainSubstrByWidth(l.title(), iw - 44), cx, cy, accent, true);
        g.drawString(font, l.attr(), x + W - 7 - font.width(l.attr()), cy, UiTheme.TEXT_DIM, false);
        cy += 11;
        g.drawString(font, font.plainSubstrByWidth(l.level(), iw), cx, cy, UiTheme.TEXT, false);
        cy += 11;
        UiBar.draw(g, cx, cy, iw, 4, s.level() / 100f, accent, time, 1f);
        cy += 8;
        g.drawString(font, font.plainSubstrByWidth(l.stage(), iw), cx, cy, UiTheme.TEXT_DIM, false);
        cy += 11;
        g.drawString(font, font.plainSubstrByWidth(l.union(), iw), cx, cy, l.unionColour(), false);
        cy += 11;
        g.drawString(font, font.plainSubstrByWidth(l.hint(), iw), cx, cy, UiTheme.TEXT_FAINT, false);
        cy += 11;
        if (!l.warn().isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(l.warn(), iw), cx, cy, UiTheme.RED, false);
            cy += 10;
        }
        if (!l.sacrifices().isEmpty()) g.drawString(font, font.plainSubstrByWidth(l.sacrifices(), iw), cx, cy, UiTheme.GOLD, false);
    }
}
