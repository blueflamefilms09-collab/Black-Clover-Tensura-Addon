package com.newuniverse.nusmp.client.saiyan;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.saiyan.SaiyanNet;
import com.newuniverse.nusmp.saiyan.SaiyanRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the client half of the Saiyan system (3.63.0): two rebindable keys that only ASK the server (Ki Blast, Rapid Ki Barrage), the hook that caches what the server says ({@code SaiyanNet.Sync}, at most
 * four a second, only on change), and the HUD panel of a Saiyan: rank and Battle Power, the Ki bar and the stamina bar, the awakening progress, the two attacks with their cooldowns (counted down here
 * between packets). Ki is the player's Tensura magicule, so while the local player is a Saiyan, Tensura's own magicule bar is hidden (the owner's decision: Ki replaces the magicule bar). The HUD is a render
 * layer of the GUI Move / GUI Size framework (id {@code saiyan_hud}). Nothing here decides anything: every number on screen is the server's. Cost: one panel of about 30 fills and 6 strings per frame, only
 * while the local player is a Saiyan; the text is rebuilt twice a second, not per frame; the layer filter is one namespace compare per HUD layer per frame.
 */
public final class SaiyanClient {
    private SaiyanClient() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping BLAST = new KeyMapping("key.nusmp.saiyan_blast", InputConstants.Type.KEYSYM, InputConstants.KEY_COMMA, CATEGORY);
    public static final KeyMapping BARRAGE = new KeyMapping("key.nusmp.saiyan_barrage", InputConstants.Type.KEYSYM, InputConstants.KEY_MINUS, CATEGORY);

    static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_hud");

    private static volatile SaiyanNet.Sync sync;
    private static long syncAt, lastAsk = -100;
    private static volatile Lines lines;
    /** Tensura layer names already written to the log (so the owner can see the real ids if the bar is not hidden). */
    private static final Set<String> SEEN = new HashSet<>();

    /** Called once by the client setup with the mod bus. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(BLAST);
            e.register(BARRAGE);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(LAYER, SaiyanClient::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SaiyanClient::onLayer);
        SaiyanNet.clientSync = SaiyanClient::onSync;
    }

    private static void clear() {
        sync = null;
        lines = null;
    }

    private static void onSync(SaiyanNet.Sync s) {
        Minecraft mc = Minecraft.getInstance();
        syncAt = mc.level == null ? 0 : mc.level.getGameTime();
        sync = s.saiyan() ? s : null;
        lines = null;
    }

    /** True while the local player is a Saiyan (the server said so). */
    public static boolean active() { return sync != null; }

    // ---------------------------------------------------------------- Ki replaces the magicule bar
    /** Hides Tensura's magicule bar while the local player is a Saiyan: the Ki bar of the Saiyan HUD takes its place. Best effort by layer name; the Tensura layer names are logged once. */
    private static void onLayer(RenderGuiLayerEvent.Pre e) {
        ResourceLocation n = e.getName();
        if (sync == null || !"tensura".equals(n.getNamespace())) return;
        if (SEEN.add(n.getPath())) LOG.info("[nusmp] Saiyan: Tensura HUD layer '{}' {}", n, n.getPath().toLowerCase(Locale.ROOT).contains("magicule") ? "hidden (Ki replaces the magicule bar)" : "kept");
        if (n.getPath().toLowerCase(Locale.ROOT).contains("magicule")) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- keys
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean can = mc.player != null && mc.getConnection() != null && mc.screen == null && sync != null;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        press(BLAST, can, now, SaiyanNet.OP_BLAST);
        press(BARRAGE, can, now, SaiyanNet.OP_BARRAGE);
    }

    /** Every queued click is drained (so none is kept for later), and at most one ask goes out every four ticks. */
    private static void press(KeyMapping key, boolean can, long now, int op) {
        while (key.consumeClick()) {
            if (!can || (now >= lastAsk && now - lastAsk < SaiyanNet.MIN_GAP_TICKS)) continue;
            lastAsk = now;
            PacketDistributor.sendToServer(new SaiyanNet.Action(op));
        }
    }

    // ---------------------------------------------------------------- text
    static String t(String key, Object... args) { return Component.translatable("nusmp.saiyan.hud." + key, args).getString(); }

    private static String num(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    /** The text of the panel, rebuilt when a packet lands and twice a second. */
    record Lines(long builtAt, String rank, String power, String ki, String stamina, String awaken, String blast, String barrage, boolean blastReady, boolean barrageReady) {}

    private static int left(int cdAtSync, long now) { return (int) Math.max(0, cdAtSync - Math.max(0, now - syncAt)); }

    static Lines build(SaiyanNet.Sync s, long now) {
        int b = left(s.blastCd(), now), r = left(s.barrageCd(), now);
        return new Lines(now, Component.translatable(SaiyanRules.Rank.byOrdinal(s.rank()).key()).getString(), t("bp", num(s.battlePower())), t("ki", num(s.ki()), num(s.kiMax())),
                t("stamina", num(s.stamina()), num(s.staminaMax())), s.awakened() ? "" : t("meditate", s.meditate()),
                attack(BLAST, "blast", b), attack(BARRAGE, "barrage", r), b <= 0, r <= 0);
    }

    private static String attack(KeyMapping key, String id, int cdTicks) {
        String k = key.getTranslatedKeyMessage().getString();
        return cdTicks > 0 ? t("cd", k, t(id), (cdTicks + 19) / 20) : t("ready", k, t(id));
    }

    // ---------------------------------------------------------------- the HUD
    private static final int W = 150;

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SaiyanNet.Sync s = sync;
        if (s == null || mc.options.hideGui || mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        Lines l = lines;
        if (l == null || now - l.builtAt() >= 10 || now < l.builtAt()) lines = l = build(s, now);
        int h = height(l);
        int x = g.guiWidth() - W - 6, y = g.guiHeight() - h - 6;
        GuiMove.report("saiyan_hud", x, y, W, h);                                       // GUI Move / GUI Size: where the panel is drawn (the layer hook moves it)
        paint(g, mc.font, x, y, l, s);
    }

    static int height(Lines l) {
        return 8 + 11 + 2 * 15 + (l.awaken().isEmpty() ? 0 : 15) + 2 * 10 + 4;
    }

    private static void paint(GuiGraphics g, Font font, int x, int y, Lines l, SaiyanNet.Sync s) {
        int h = height(l);
        float time = UiTheme.now();
        UiPanel.panel(g, x, y, W, h, UiTheme.GOLD, 0.3f, 0.92f);
        int cx = x + 7, cy = y + 6, iw = W - 14;
        g.drawString(font, font.plainSubstrByWidth(l.rank(), iw - 60), cx, cy, UiTheme.GOLD, true);
        g.drawString(font, l.power(), x + W - 7 - font.width(l.power()), cy, UiTheme.TEXT, false);
        cy += 12;
        cy = bar(g, font, l.ki(), s.kiMax() <= 0f ? 0f : s.ki() / s.kiMax(), UiTheme.CYAN, cx, cy, iw, time);
        cy = bar(g, font, l.stamina(), s.staminaMax() <= 0f ? 0f : s.stamina() / s.staminaMax(), UiTheme.GREEN, cx, cy, iw, time);
        if (!l.awaken().isEmpty()) cy = bar(g, font, l.awaken(), s.meditate() / 100f, UiTheme.VIOLET, cx, cy, iw, time);
        g.drawString(font, font.plainSubstrByWidth(l.blast(), iw), cx, cy, l.blastReady() ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
        cy += 10;
        g.drawString(font, font.plainSubstrByWidth(l.barrage(), iw), cx, cy, l.barrageReady() ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
    }

    private static int bar(GuiGraphics g, Font font, String label, float frac, int rgb, int x, int y, int w, float time) {
        g.drawString(font, font.plainSubstrByWidth(label, w), x, y, UiTheme.TEXT, false);
        UiBar.draw(g, x, y + 10, w, 3, UiMath.clamp01(frac), rgb, time, 1f);
        return y + 15;
    }
}
